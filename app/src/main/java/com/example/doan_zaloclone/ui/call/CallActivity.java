package com.example.doan_zaloclone.ui.call;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.util.Rational;
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
import com.example.doan_zaloclone.services.OngoingCallService;
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
    
    private View outgoingCallButtons;
    private FloatingActionButton cancelButton;
    
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
    private boolean isInPipMode = false;
    
    // Duration tracking
    private Handler durationHandler;
    private Runnable durationRunnable;
    private long callStartTime = 0;
     // Call data (needed for permission callback)
    private String conversationId;
    private String callerId;
    
    // Call history logging flag
    private boolean callHistoryLogged = false;
    
    // Audio
    private AudioManager audioManager;
    
    // Proximity Sensor
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private boolean isProximityNear = false;
    private SensorEventListener proximitySensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                boolean wasNear = isProximityNear;
                isProximityNear = distance < proximitySensor.getMaximumRange();
                
                if (wasNear != isProximityNear) {
                    Log.d(TAG, "Proximity changed: " + (isProximityNear ? "NEAR" : "FAR"));
                    handleProximityChange();
                }
            }
        }
        
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed
        }
    };
    
    // OngoingCallService
    private OngoingCallService ongoingCallService;
    private boolean serviceBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OngoingCallService.LocalBinder binder = (OngoingCallService.LocalBinder) service;
            ongoingCallService = binder.getService();
            serviceBound = true;
            Log.d(TAG, "OngoingCallService bound");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            ongoingCallService = null;
            serviceBound = false;
            Log.d(TAG, "OngoingCallService unbound");
        }
    };
    
    // Audio routing
    private BroadcastReceiver audioDeviceChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Headphones unplugged
                Log.d(TAG, "Headphones unplugged");
                configureAudioRouting();
            } else if (AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                // Wired headset plugged/unplugged
                int state = intent.getIntExtra("state", -1);
                if (state == 1) {
                    Log.d(TAG, "Wired headset connected");
                } else if (state == 0) {
                    Log.d(TAG, "Wired headset disconnected");
                }
                configureAudioRouting();
            }
        }
    };
    
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
        
        // DEBUG: Log call perspective
        Log.d(TAG, "========================================");
        Log.d(TAG, "CallActivity created");
        Log.d(TAG, "isIncoming: " + isIncoming + " (" + (isIncoming ? "RECEIVER" : "CALLER") + ")");
        Log.d(TAG, "conversationId: " + conversationId);
        Log.d(TAG, "callerId: " + callerId);
        Log.d(TAG, "receiverId: " + receiverId);
        Log.d(TAG, "========================================");
        String callerNameStr = intent.getStringExtra(EXTRA_CALLER_NAME);
        
        // Initialize ViewModel BEFORE initViews()
        // initViews() calls setupVideoViews() which needs callViewModel
        callViewModel = new ViewModelProvider(this).get(CallViewModel.class);
        
        // Initialize UI
        initViews();
        
        // Setup audio manager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        
        // Configure default audio routing based on call type
        configureAudioRouting();
        
        // Register audio device change listener
        registerAudioDeviceListener();
        
        // Setup proximity sensor for voice calls
        if (!isVideo) {
            setupProximitySensor();
        }
        
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
        
        outgoingCallButtons = findViewById(R.id.outgoingCallButtons);
        cancelButton = findViewById(R.id.cancelButton);
        
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
        cancelButton.setOnClickListener(v -> endCall());
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
        setupIncomingUI();
        
        // Listen to call status changes in realtime
        if (callId != null) {
            callViewModel.startListeningToCall(callId);
        }
    }
    
    private void handleOutgoingCall(String conversationId, String from, String to) {
        Log.d(TAG, "Handling outgoing call to: " + to);
        callStatus.setText(R.string.call_calling);
        
        // Show outgoing UI
        setupOutgoingUI();
        
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
        Log.d(TAG, "conversationId: " + conversationId);
        
        if (callId != null) {
            callViewModel.rejectCall(callId);
            
            // Log call history as MISSED from receiver's perspective
            if (conversationId != null) {
                Log.d(TAG, "========== REJECT CALL - LOGGING ==========="  );
                Log.d(TAG, "Logging MISSED call history (rejected by receiver)");
                Log.d(TAG, "isIncoming (should be TRUE): " + isIncoming);
                Log.d(TAG, "conversationId: " + conversationId);
                callViewModel.logCallHistory(conversationId, "MISSED", isVideo, 0, true);
            } else {
                Log.e(TAG, "Cannot log MISSED call - conversationId is NULL!");
            }
        }
        finish();
    }
    
    private void endCall() {
        Log.d(TAG, "Ending call");
        
        callViewModel.endCall();
        
        stopDurationTimer();
        stopOngoingCallService();
        disableProximitySensor();
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
    
    /**
     * Configure audio routing based on call type and device state
     */
    private void configureAudioRouting() {
        if (audioManager == null) return;
        
        // Check if headphones/bluetooth are connected
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        boolean isBluetoothOn = audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn();
        
        Log.d(TAG, "Audio routing - Wired: " + isWiredHeadsetOn + ", Bluetooth: " + isBluetoothOn + ", Video: " + isVideo);
        
        if (isWiredHeadsetOn || isBluetoothOn) {
            // Headphones/Bluetooth connected: route to them (speaker off)
            isSpeakerOn = false;
            audioManager.setSpeakerphoneOn(false);
            Log.d(TAG, "Audio routed to headphones/bluetooth");
        } else {
            // No external audio device
            if (isVideo) {
                // Video call: default to speaker
                isSpeakerOn = true;
                audioManager.setSpeakerphoneOn(true);
                Log.d(TAG, "Video call: Audio routed to speaker");
            } else {
                // Voice call: default to earpiece
                isSpeakerOn = false;
                audioManager.setSpeakerphoneOn(false);
                Log.d(TAG, "Voice call: Audio routed to earpiece");
            }
        }
        
        updateSpeakerUI();
    }
    
    /**
     * Register listener for audio device changes
     */
    private void registerAudioDeviceListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioDeviceChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(audioDeviceChangeReceiver, filter);
        }
        
        Log.d(TAG, "Audio device listener registered");
    }
    
    /**
     * Unregister audio device listener
     */
    private void unregisterAudioDeviceListener() {
        try {
            unregisterReceiver(audioDeviceChangeReceiver);
            Log.d(TAG, "Audio device listener unregistered");
        } catch (IllegalArgumentException e) {
            // Receiver was not registered, ignore
        }
    }
    
    /**
     * Setup proximity sensor for voice calls
     */
    private void setupProximitySensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        
        if (proximitySensor != null) {
            Log.d(TAG, "Proximity sensor available, max range: " + proximitySensor.getMaximumRange());
        } else {
            Log.w(TAG, "Proximity sensor not available on this device");
        }
    }
    
    /**
     * Enable proximity sensor when call connects (voice calls only)
     */
    private void enableProximitySensor() {
        if (isVideo || proximitySensor == null) return;
        
        sensorManager.registerListener(
            proximitySensorListener,
            proximitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        );
        
        Log.d(TAG, "Proximity sensor enabled");
    }
    
    /**
     * Disable proximity sensor
     */
    private void disableProximitySensor() {
        if (proximitySensor == null) return;
        
        try {
            sensorManager.unregisterListener(proximitySensorListener);
            Log.d(TAG, "Proximity sensor disabled");
        } catch (IllegalArgumentException e) {
            // Listener was not registered, ignore
        }
    }
    
    /**
     * Handle proximity sensor state change
     */
    private void handleProximityChange() {
        if (isVideo) return; // Only for voice calls
        
        if (isProximityNear) {
            // Phone near ear: turn off screen
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            
            // Request screen off
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false);
                setTurnScreenOn(false);
            }
            
            Log.d(TAG, "Screen should turn off (phone near ear)");
        } else {
            // Phone away from ear: turn on screen
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            }
            
            Log.d(TAG, "Screen should turn on (phone away from ear)");
        }
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
                
                // Log call history before finishing
                long durationSeconds = 0;
                if (callStartTime > 0) {
                    durationSeconds = (System.currentTimeMillis() - callStartTime) / 1000;
                }
                
                Log.d(TAG, "Call ENDED - Duration: " + durationSeconds + "s, conversationId: " + conversationId);
                Log.d(TAG, "Call perspective - isIncoming: " + isIncoming);
                
                if (conversationId != null) {
                    // Prevent duplicate logging
                    if (!callHistoryLogged) {
                        String callType;
                        if (durationSeconds > 0) {
                            // Call was accepted and connected
                            callType = isIncoming ? "INCOMING" : "OUTGOING";
                        } else {
                            // Call was not answered
                            // For receiver: missed call
                            // For caller: cancelled outgoing call
                            callType = isIncoming ? "MISSED" : "OUTGOING";
                        }
                        Log.d(TAG, "========== ENDED - LOGGING ===========");
                        Log.d(TAG, "Logging call history on ENDED: type=" + callType + ", isIncoming=" + isIncoming);
                        Log.d(TAG, "conversationId: " + conversationId);
                        Log.d(TAG, "durationSeconds: " + durationSeconds);
                        callViewModel.logCallHistory(conversationId, callType, isVideo, durationSeconds, isIncoming);
                        callHistoryLogged = true;  // Mark as logged
                    } else {
                        Log.d(TAG, "Call history already logged, skipping duplicate");
                    }
                } else {
                    Log.e(TAG, "Cannot log call history - conversationId is NULL!");
                }
                
                stopDurationTimer();
                stopOngoingCallService();
                disableProximitySensor();
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
        
        // Start OngoingCallService to maintain call in background
        startOngoingCallService();
        
        // Enable proximity sensor for voice calls
        enableProximitySensor();
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
    
    private void setupIncomingUI() {
        incomingCallButtons.setVisibility(View.VISIBLE);
        outgoingCallButtons.setVisibility(View.GONE);
        callControls.setVisibility(View.GONE);
    }
    
    private void setupOutgoingUI() {
        Log.d(TAG, "Showing outgoing call UI (cancel button)");
        outgoingCallButtons.setVisibility(View.VISIBLE);
        incomingCallButtons.setVisibility(View.GONE);
        callControls.setVisibility(View.GONE);
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
                String duration = formatDuration(elapsed);
                callDuration.setText(duration);
                
                // Update OngoingCallService notification
                updateOngoingCallNotification(duration);
                
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
        
        // Stop OngoingCallService
        stopOngoingCallService();
        
        // Unregister audio device listener
        unregisterAudioDeviceListener();
        
        // Disable proximity sensor
        disableProximitySensor();
        
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
    
    /**
     * Start OngoingCallService to keep call alive in background
     */
    private void startOngoingCallService() {
        Intent serviceIntent = new Intent(this, OngoingCallService.class);
        serviceIntent.setAction(OngoingCallService.ACTION_START_SERVICE);
        serviceIntent.putExtra(OngoingCallService.EXTRA_CALL_ID, callId);
        serviceIntent.putExtra(OngoingCallService.EXTRA_CALLER_NAME, callerName.getText().toString());
        serviceIntent.putExtra(OngoingCallService.EXTRA_IS_VIDEO, isVideo);
        
        // Start foreground service
        startService(serviceIntent);
        
        // Bind to service for updates
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        Log.d(TAG, "OngoingCallService started");
    }
    
    /**
     * Stop OngoingCallService when call ends
     */
    private void stopOngoingCallService() {
        // Unbind from service
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        // Stop service
        Intent serviceIntent = new Intent(this, OngoingCallService.class);
        serviceIntent.setAction(OngoingCallService.ACTION_STOP_SERVICE);
        startService(serviceIntent);
        
        Log.d(TAG, "OngoingCallService stopped");
    }
    
    /**
     * Update OngoingCallService notification with new duration
     */
    private void updateOngoingCallNotification(String duration) {
        if (serviceBound && ongoingCallService != null) {
            ongoingCallService.updateDuration(duration);
        }
    }
    
    /**
     * Enter Picture-in-Picture mode (video calls only)
     */
    private void enterPiPMode() {
        if (!isVideo) {
            Log.d(TAG, "PiP mode only available for video calls");
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params = buildPiPParams();
            enterPictureInPictureMode(params);
            Log.d(TAG, "Entering PiP mode");
        } else {
            Log.w(TAG, "PiP mode requires Android 8.0+");
        }
    }
    
    /**
     * Build PiP parameters with aspect ratio
     */
    private PictureInPictureParams buildPiPParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Calculate aspect ratio from video view dimensions
            Rational aspectRatio = new Rational(9, 16); // Default portrait
            
            if (remoteVideoView != null && remoteVideoView.getWidth() > 0 && remoteVideoView.getHeight() > 0) {
                aspectRatio = new Rational(remoteVideoView.getWidth(), remoteVideoView.getHeight());
                Log.d(TAG, "PiP aspect ratio: " + aspectRatio);
            }
            
            return new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
        }
        return null;
    }
    
    /**
     * Auto-enter PiP when user presses Home (video calls only)
     */
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        
        // Only enter PiP for video calls
        if (isVideo && !isInPipMode) {
            enterPiPMode();
        }
    }
    
    /**
     * Handle PiP mode changes
     */
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        
        isInPipMode = isInPictureInPictureMode;
        Log.d(TAG, "PiP mode changed: " + isInPictureInPictureMode);
        
        if (isInPictureInPictureMode) {
            // Hide UI controls in PiP mode
            if (callControls != null) {
                callControls.setVisibility(View.GONE);
            }
            if (callDuration != null) {
                callDuration.setVisibility(View.GONE);
            }
            if (localVideoView != null) {
                localVideoView.setVisibility(View.GONE); // Hide small preview
            }
            Log.d(TAG, "PiP mode: UI controls hidden");
        } else {
            // Restore UI controls when exiting PiP
            if (callControls != null) {
                callControls.setVisibility(View.VISIBLE);
            }
            if (callDuration != null) {
                callDuration.setVisibility(View.VISIBLE);
            }
            if (localVideoView != null) {
                localVideoView.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "Exited PiP mode: UI controls restored");
        }
    }
    
    /**
     * Handle configuration changes (required for PiP)
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update PiP params if aspect ratio changed
        if (isInPipMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params = buildPiPParams();
            setPictureInPictureParams(params);
        }
    }
    
    @Override
    public void onBackPressed() {
        // Allow back button to minimize to background (OngoingCallService keeps call alive)
        moveTaskToBack(true);
    }
}
