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
        String conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID);
        String callerId = intent.getStringExtra(EXTRA_CALLER_ID);
        this.receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID);  // Store as instance variable
        String callerNameStr = intent.getStringExtra(EXTRA_CALLER_NAME);
        
        // Initialize UI
        initViews();
        
        // Setup audio manager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        
        // Initialize ViewModel
        callViewModel = new ViewModelProvider(this).get(CallViewModel.class);
        
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
        if (isIncoming) {
            handleIncomingCall();
        } else {
            handleOutgoingCall(conversationId, callerId, receiverId);
        }
    }
    
    private void initViews() {
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
            setupVideoViews();
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
        // Create EglBase for video rendering
        eglBase = EglBase.create();
        
        // Initialize local video view
        localVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setMirror(true);  // Mirror for front camera
        localVideoView.setVisibility(View.VISIBLE);
        
        // Initialize remote video view
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
        remoteVideoView.setVisibility(View.VISIBLE);
        
        // Attach local renderer
        callViewModel.getWebRtcRepository().attachLocalRenderer(localVideoView);
        
        // Setup remote stream callback
        callViewModel.getWebRtcRepository().setRemoteStreamCallback(mediaStream -> {
            Log.d(TAG, "Remote stream received");
            
            // Get video track from stream
            if (mediaStream.videoTracks.size() > 0) {
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                
                // Must run on UI thread
                runOnUiThread(() -> {
                    if (remoteVideoView != null) {
                        remoteVideoTrack.addSink(remoteVideoView);
                        Log.d(TAG, "Remote video track attached to view");
                    }
                });
            }
        });
        
        Log.d(TAG, "Video views setup complete");
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
    
    private void handleOutgoingCall(String conversationId, String callerId, String receiverId) {
        Log.d(TAG, "Initiating outgoing call");
        callStatus.setText(R.string.call_calling);
        showOutgoingCallUI();
        
        // Initiate call via ViewModel
        callViewModel.initiateCall(conversationId, callerId, receiverId, isVideo);
    }
    
    private void acceptCall() {
        Log.d(TAG, "Accepting call: " + callId);
        
        if (callId == null) {
            showError("Invalid call ID");
            finish();
            return;
        }
        
        // Use receiver ID from Intent, fallback to current user if not available
        String currentUserId = receiverId != null ? receiverId : getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            showError("Cannot determine receiver ID");
            finish();
            return;
        }
        
        // Pass isVideo flag to acceptCall so it can initialize WebRTC correctly
        callViewModel.acceptCall(callId, currentUserId, isVideo);
        
        callStatus.setText(R.string.call_connecting);
        showConnectingUI();
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
        incomingCallButtons.setVisibility(View.GONE);
        callControls.setVisibility(View.VISIBLE);
        callDuration.setVisibility(View.VISIBLE);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionHelper.REQUEST_CODE_VOICE_CALL || 
            requestCode == PermissionHelper.REQUEST_CODE_VIDEO_CALL) {
            
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // Permissions granted, retry call initiation
                recreate();
            } else {
                // Show explanation dialog
                showPermissionDeniedDialog();
            }
        }
    }
    
    /**
     * Show dialog explaining why permissions are needed
     */
    private void showPermissionDeniedDialog() {
        String message = isVideo ? 
            "Cần quyền Camera và Microphone để thực hiện cuộc gọi video" :
            "Cần quyền Microphone để thực hiện cuộc gọi thoại";
            
        new AlertDialog.Builder(this)
            .setTitle("Cần quyền truy cập")
            .setMessage(message)
            .setPositiveButton("Cài đặt", (dialog, which) -> {
                // Open app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Hủy", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
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
