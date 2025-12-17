package com.example.doan_zaloclone.ui.call;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Call;
import com.example.doan_zaloclone.utils.PermissionHelper;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.viewmodel.CallViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/**
 * Activity for handling voice and video calls
 * Supports incoming, outgoing, and ongoing call states
 */
public class CallActivity extends AppCompatActivity {
    private static final String TAG = "CallActivity";
    
    // Intent extras
    public static final String EXTRA_CALL_ID = "call_id";
    public static final String EXTRA_IS_INCOMING = "is_incoming";
    public static final String EXTRA_IS_VIDEO = "is_video";
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CALLER_ID = "caller_id";
    public static final String EXTRA_RECEIVER_ID = "receiver_id";
    public static final String EXTRA_CALLER_NAME = "caller_name";
    
    // UI Components
    private View callerInfoContainer;  // Container holding avatar, name, status
    private ImageView callerAvatar;
    private TextView callerName;
    private TextView callStatus;
    private TextView callDuration;
    
    private View incomingCallButtons;
    private FloatingActionButton acceptButton;
    private FloatingActionButton rejectButton;
    
    private View callControls;
    private FloatingActionButton micButton;
    private FloatingActionButton speakerButton;
    private FloatingActionButton endCallButton;
    
    // Content overlay
    private View contentOverlay;
    private View rootLayout;  // Root FrameLayout
    
    // Video components
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private FloatingActionButton cameraButton;
    private FloatingActionButton switchCameraButton;
    private EglBase eglBase;
    
    // ViewModel
    private CallViewModel callViewModel;
    
    // State
    private String callId;
    private String receiverId;  // Added to store receiver ID from Intent
    private boolean isIncoming;
    private boolean isVideo;
    private boolean isMicEnabled = true;
    private boolean isSpeakerOn = false;
    
    // Duration tracking
    private Handler durationHandler;
    private Runnable durationRunnable;
    private long callStartTime = 0;
     // Call data (needed for permission callback)
    private String conversationId;
    private String callerId;
    
    // Audio
    private AudioManager audioManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        
        // Get intent extras
        Intent intent = getIntent();
        callId = intent.getStringExtra(EXTRA_CALL_ID);
        isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false);
        isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false);
        this.conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID);
        this.callerId = intent.getStringExtra(EXTRA_CALLER_ID);
        this.receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID);  // Store as instance variable
        String callerNameStr = intent.getStringExtra(EXTRA_CALLER_NAME);
        
        // Initialize ViewModel BEFORE initViews()
        // initViews() calls setupVideoViews() which needs callViewModel
        callViewModel = new ViewModelProvider(this).get(CallViewModel.class);
        
        // Initialize UI
        initViews();
        
        // Setup audio manager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        
        // Setup observers
        setupObservers();
        
        // Set caller name
        if (callerNameStr != null) {
            callerName.setText(callerNameStr);
        }
        
        // Check permissions
        if (!PermissionHelper.checkCallPermissions(this, isVideo)) {
            PermissionHelper.requestCallPermissions(this, isVideo, PermissionHelper.REQUEST_CODE_VOICE_CALL);
            return;
        }
        
        // Handle incoming vs outgoing call
        startCall();
    }
    
    /**
     * Start call after permissions are granted
     */
    private void startCall() {
        if (isIncoming) {
            handleIncomingCall();
        } else {
            handleOutgoingCall(conversationId, callerId, receiverId);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionHelper.REQUEST_CODE_VOICE_CALL) {
            if (PermissionHelper.checkCallPermissions(this, isVideo)) {
                Log.d(TAG, "Permissions granted, starting call");
                startCall();
            } else {
                Log.e(TAG, "Permissions denied");
                Toast.makeText(this, "Không thể thực hiện cuộc gọi do thiếu quyền", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void initViews() {
        // Find root layout and overlays
        rootLayout = findViewById(android.R.id.content).getRootView();
        callerInfoContainer = findViewById(R.id.callerInfoContainer);
        contentOverlay = findViewById(R.id.contentOverlay);
        callerAvatar = findViewById(R.id.callerAvatar);
        callerName = findViewById(R.id.callerName);
        callStatus = findViewById(R.id.callStatus);
        callDuration = findViewById(R.id.callDuration);
        
        incomingCallButtons = findViewById(R.id.incomingCallButtons);
        acceptButton = findViewById(R.id.acceptButton);
        rejectButton = findViewById(R.id.rejectButton);
        
        callControls = findViewById(R.id.callControls);
        micButton = findViewById(R.id.micButton);
        speakerButton = findViewById(R.id.speakerButton);
        endCallButton = findViewById(R.id.endCallButton);
        
        // Video components
        localVideoView = findViewById(R.id.localVideoView);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        cameraButton = findViewById(R.id.cameraButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);
        
        // Setup button listeners
        acceptButton.setOnClickListener(v -> acceptCall());
        rejectButton.setOnClickListener(v -> rejectCall());
        endCallButton.setOnClickListener(v -> endCall());
        micButton.setOnClickListener(v -> toggleMicrophone());
        speakerButton.setOnClickListener(v -> toggleSpeaker());
        
        // Video button listeners
        cameraButton.setOnClickListener(v -> callViewModel.toggleCamera());
        switchCameraButton.setOnClickListener(v -> callViewModel.switchCamera());
        
        // Setup video if this is a video call
        if (isVideo) {
            setupVideoViews();  // Activity uses its own EglBase
            cameraButton.setVisibility(View.VISIBLE);
            switchCameraButton.setVisibility(View.VISIBLE);
            callerAvatar.setVisibility(View.GONE);  // Hide avatar for video call
        } else {
            cameraButton.setVisibility(View.GONE);
            switchCameraButton.setVisibility(View.GONE);
        }
    }
    
    /**
     * Setup video views for video call
     */
    private void setupVideoViews() {
        Log.d(TAG, "===== SETTING UP VIDEO VIEWS =====");
        
        // Create EglBase for video rendering (Activity owns this, not Repository)
        eglBase = EglBase.create();
        Log.d(TAG, "EglBase created");
        
        // CRITICAL: Share EglBase context with WebRtcRepository IMMEDIATELY
        callViewModel.getWebRtcRepository().setEglBaseContext(eglBase.getEglBaseContext());
        Log.d(TAG, "EglBase context shared with WebRtcRepository");
        
        // Initialize and setup remote video view FIRST (background, full screen)
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
        remoteVideoView.setEnableHardwareScaler(true);  // Enable hardware acceleration
        remoteVideoView.setMirror(false);  // Don't mirror remote video
        remoteVideoView.setZOrderMediaOverlay(false);  // Render in background
        remoteVideoView.setZOrderOnTop(false);  // Ensure it's in background
        
        // CRITICAL: Set visible NOW before attaching tracks
        remoteVideoView.setVisibility(View.VISIBLE);
        remoteVideoView.requestLayout();
        Log.d(TAG, "Remote video view initialized and set to VISIBLE (width=" + remoteVideoView.getWidth() + ", height=" + remoteVideoView.getHeight() + ")");
        
        // Initialize and setup local video view (foreground, small preview)
        localVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setEnableHardwareScaler(true);  // Enable hardware acceleration
        localVideoView.setMirror(true);  // Mirror for selfie view
        localVideoView.setZOrderMediaOverlay(true);  // Render on top of remote view
        localVideoView.setZOrderOnTop(true);  // Ensure it's on top
        
        // CRITICAL: Set visible NOW before attaching tracks
        localVideoView.setVisibility(View.VISIBLE);
        localVideoView.requestLayout();
        Log.d(TAG, "Local video view initialized and set to VISIBLE (width=" + localVideoView.getWidth() + ", height=" + localVideoView.getHeight() + ")");
        
        // Force layout pass to ensure views have proper dimensions
        remoteVideoView.post(() -> {
            Log.d(TAG, "Post-layout remote view: width=" + remoteVideoView.getWidth() + ", height=" + remoteVideoView.getHeight());
        });
        localVideoView.post(() -> {
            Log.d(TAG, "Post-layout local view: width=" + localVideoView.getWidth() + ", height=" + localVideoView.getHeight());
        });
        
        // Register renderer - will attach video track when it's created in initializePeerConnection()
        callViewModel.getWebRtcRepository().attachLocalRenderer(localVideoView);
        
        // Setup remote stream callback
        callViewModel.getWebRtcRepository().setRemoteStreamCallback(mediaStream -> {
            Log.d(TAG, "===== REMOTE STREAM CALLBACK TRIGGERED =====");
            Log.d(TAG, "Remote stream ID: " + mediaStream.getId());
            Log.d(TAG, "Audio tracks: " + mediaStream.audioTracks.size());
            Log.d(TAG, "Video tracks: " + mediaStream.videoTracks.size());
            
            // Get video track from stream
            if (mediaStream.videoTracks.size() > 0) {
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                Log.d(TAG, "Remote video track ID: " + remoteVideoTrack.id());
                Log.d(TAG, "Remote video track enabled: " + remoteVideoTrack.enabled());
                Log.d(TAG, "Remote video track state: " + remoteVideoTrack.state());
                
                // Must run on UI thread
                runOnUiThread(() -> {
                    if (remoteVideoView != null) {
                        // Double-check visibility and dimensions
                        Log.d(TAG, "Before attach: remoteVideoView visibility=" + remoteVideoView.getVisibility() + 
                                  ", width=" + remoteVideoView.getWidth() + ", height=" + remoteVideoView.getHeight());
                        
                        // Ensure view is visible before attaching
                        remoteVideoView.setVisibility(View.VISIBLE);
                        remoteVideoView.bringToFront();
                        remoteVideoView.requestLayout();
                        
                        // Wait for layout to complete, then attach
                        remoteVideoView.post(() -> {
                            // Attach video track to renderer
                            remoteVideoTrack.addSink(remoteVideoView);
                            Log.d(TAG, "Remote video track attached to remoteVideoView");
                            Log.d(TAG, "After attach: width=" + remoteVideoView.getWidth() + ", height=" + remoteVideoView.getHeight());
                        });
                    } else {
                        Log.e(TAG, "ERROR: remoteVideoView is null!");
                    }
                });
            } else {
                Log.w(TAG, "WARNING: No video tracks in remote stream!");
            }
        });
        
        Log.d(TAG, "===== VIDEO VIEWS SETUP COMPLETE =====");
    }
    
    private void setupObservers() {
        // Observe current call
        callViewModel.getCurrentCall().observe(this, resource -> {
            if (resource.isLoading()) {
                Log.d(TAG, "Loading call...");
            } else if (resource.isSuccess()) {
                Call call = resource.getData();
                if (call != null) {
                    updateUIForCallStatus(call.getStatus());
                }
            } else if (resource.isError()) {
                Log.e(TAG, "Call error: " + resource.getMessage());
                showError(resource.getMessage());
                finish();
            }
        });
        
        // Observe connection state
        callViewModel.getConnectionState().observe(this, state -> {
            Log.d(TAG, "Connection state: " + state);
            if ("CONNECTED".equals(state)) {
                onCallConnected();
            } else if ("FAILED".equals(state)) {
                showError("Kết nối thất bại");
                finish();
            }
        });
        
        // Observe mic state
        callViewModel.getIsMicEnabled().observe(this, enabled -> {
            isMicEnabled = enabled;
            updateMicUI();
        });
        
        // Observe camera state
        callViewModel.getIsCameraEnabled().observe(this, enabled -> {
            if (isVideo) {
                cameraButton.setImageResource(enabled ? 
                    R.drawable.ic_videocam : R.drawable.ic_videocam_off);
            }
        });
        
        // Observe errors
        callViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
    }
    
    private void handleIncomingCall() {
        Log.d(TAG, "Handling incoming call: " + callId);
        callStatus.setText(R.string.incoming_call);
        showIncomingCallUI();
    }
    
    private void handleOutgoingCall(String conversationId, String from, String to) {
        Log.d(TAG, "Handling outgoing call to: " + to);
        callStatus.setText(R.string.call_calling);
        
        // Show outgoing UI
        showOutgoingCallUI();
        
        // Initiate call via ViewModel (this calls initializePeerConnection)
        callViewModel.initiateCall(conversationId, from, to, isVideo);
    }
    
    private void acceptCall() {
        Log.d(TAG, "===== ACCEPT BUTTON CLICKED =====");
        Log.d(TAG, "Accepting call: " + callId);
        Log.d(TAG, "receiverId: " + receiverId);
        Log.d(TAG, "isVideo: " + isVideo);
        
        if (callId == null) {
            Log.e(TAG, "ERROR: callId is null!");
            showError("Invalid call ID");
            finish();
            return;
        }
        
        // Use receiver ID from Intent, fallback to current user if not available
        String currentUserId = receiverId != null ? receiverId : getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "ERROR: currentUserId is null or empty!");
            showError("Cannot determine receiver ID");
            finish();
            return;
        }
        
        Log.d(TAG, "Calling callViewModel.acceptCall()...");
        // Pass isVideo flag to acceptCall so it can initialize WebRTC correctly
        callViewModel.acceptCall(callId, currentUserId, isVideo);
        
        Log.d(TAG, "Setting UI to connecting...");
        callStatus.setText(R.string.call_connecting);
        showConnectingUI();
        Log.d(TAG, "===== ACCEPT CALL COMPLETE =====");
    }
    
    private void rejectCall() {
        Log.d(TAG, "Rejecting call");
        
        if (callId != null) {
            callViewModel.rejectCall(callId);
        }
        finish();
    }
    
    private void endCall() {
        Log.d(TAG, "Ending call");
        callViewModel.endCall();
        
        stopDurationTimer();
        finish();
    }
    
    private void toggleMicrophone() {
        callViewModel.toggleMicrophone();
    }
    
    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        audioManager.setSpeakerphoneOn(isSpeakerOn);
        updateSpeakerUI();
        Log.d(TAG, "Speaker " + (isSpeakerOn ? "ON" : "OFF"));
    }
    
    private void updateUIForCallStatus(String status) {
        Log.d(TAG, "Updating UI for status: " + status);
        
        switch (status) {
            case Call.STATUS_CALLING:
                callStatus.setText(R.string.call_calling);
                break;
            case Call.STATUS_RINGING:
                callStatus.setText(R.string.call_ringing);
                break;
            case Call.STATUS_CONNECTED:  // THIS IS THE MISSING CASE!
            case Call.STATUS_ONGOING:
                callStatus.setText(R.string.call_ongoing);
                onCallConnected();
                break;
            case Call.STATUS_ENDED:
                callStatus.setText(R.string.call_ended);
                stopDurationTimer();
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                break;
            case Call.STATUS_REJECTED:
                callStatus.setText(R.string.call_rejected);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                break;
            case Call.STATUS_FAILED:
                callStatus.setText(R.string.call_failed);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                break;
        }
    }
    
    private void onCallConnected() {
        Log.d(TAG, "Call connected");
        showOngoingCallUI();
        startDurationTimer();
    }
    
    private void showIncomingCallUI() {
        incomingCallButtons.setVisibility(View.VISIBLE);
        callControls.setVisibility(View.GONE);
        callDuration.setVisibility(View.GONE);
    }
    
    private void showOutgoingCallUI() {
        incomingCallButtons.setVisibility(View.GONE);
        callControls.setVisibility(View.GONE);
        callDuration.setVisibility(View.GONE);
    }
    
    private void showConnectingUI() {
        incomingCallButtons.setVisibility(View.GONE);
        callControls.setVisibility(View.GONE);
        callDuration.setVisibility(View.GONE);
    }
    
    private void showOngoingCallUI() {
        Log.d(TAG, "===== showOngoingCallUI called =====");
        
        incomingCallButtons.setVisibility(View.GONE);
        callControls.setVisibility(View.VISIBLE);
        callDuration.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "Call controls visibility set to VISIBLE");
        Log.d(TAG, "Call duration visibility set to VISIBLE");
        
        // For video calls: hide overlay elements to show video
        if (isVideo) {
            Log.d(TAG, "Configuring UI for VIDEO call");
            
            // Hide caller info container to show video
            if (callerInfoContainer != null) {
                callerInfoContainer.setVisibility(View.GONE);
                Log.d(TAG, "Hidden caller info container");
            }
            
            // Verify controls are visible
            callControls.post(() -> {
                Log.d(TAG, "Controls check: visibility=" + callControls.getVisibility() + 
                      ", width=" + callControls.getWidth() + 
                      ", height=" + callControls.getHeight());
            });
            
            // Video views are already visible from setupVideoViews()
            // Just verify they're ready
            if (remoteVideoView != null) {
                Log.d(TAG, "Remote video: visibility=" + remoteVideoView.getVisibility() + 
                      ", width=" + remoteVideoView.getWidth() + 
                      ", height=" + remoteVideoView.getHeight());
            }
            
            if (localVideoView != null) {
                Log.d(TAG, "Local video: visibility=" + localVideoView.getVisibility() +
                      ", width=" + localVideoView.getWidth() +
                      ", height=" + localVideoView.getHeight());
            }
            
            Log.d(TAG, "===== Video call UI configured =====");
        }
    }
    
    private void startDurationTimer() {
        callStartTime = System.currentTimeMillis();
        durationHandler = new Handler(Looper.getMainLooper());
        durationRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = (System.currentTimeMillis() - callStartTime) / 1000;
                callDuration.setText(formatDuration(elapsed));
                durationHandler.postDelayed(this, 1000);
            }
        };
        durationHandler.post(durationRunnable);
    }
    
    private void stopDurationTimer() {
        if (durationHandler != null && durationRunnable != null) {
            durationHandler.removeCallbacks(durationRunnable);
        }
    }
    
    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    private void updateMicUI() {
        if (isMicEnabled) {
            micButton.setImageResource(R.drawable.ic_mic);
        } else {
            micButton.setImageResource(R.drawable.ic_mic_off);
        }
    }
    
    private void updateSpeakerUI() {
        // Update speaker button appearance if needed
        // For now, speaker icon stays the same
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private String getCurrentUserId() {
        // TODO: Get from SharedPreferences or AuthManager
        // For now, return from Firebase Auth
        com.google.firebase.auth.FirebaseUser currentUser = 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "";
    }
    
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDurationTimer();
        
        // Cleanup video resources
        if (isVideo && eglBase != null) {
            if (localVideoView != null) {
                callViewModel.getWebRtcRepository().detachLocalRenderer(localVideoView);
                localVideoView.release();
            }
            if (remoteVideoView != null) {
                remoteVideoView.release();
            }
            eglBase.release();
        }
        
        // Reset audio mode
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button during call
        // User must use end call button
    }
}
