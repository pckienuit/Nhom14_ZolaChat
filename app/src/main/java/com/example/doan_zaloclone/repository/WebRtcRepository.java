package com.example.doan_zaloclone.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.doan_zaloclone.utils.WebRtcHelper;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.List;

/**
 * Repository for managing WebRTC PeerConnection
 * Handles creating offers/answers, ICE candidates, and audio control
 */
public class WebRtcRepository {
    private static final String TAG = "WebRtcRepository";
    private static final String AUDIO_TRACK_ID = "audio_track";
    private static final String VIDEO_TRACK_ID = "video_track";
    private static final String STREAM_ID = "stream";
    // Conservative defaults to prioritize stability on mid/low devices
    private static final int VIDEO_WIDTH = 540;
    private static final int VIDEO_HEIGHT = 960;
    private static final int VIDEO_FPS = 24;
    private static final int VIDEO_FALLBACK_WIDTH = 480;
    private static final int VIDEO_FALLBACK_HEIGHT = 640;
    private static final int VIDEO_FALLBACK_FPS = 15;

    private final Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private RtpSender audioSender;
    private RtpSender videoSender;

    // Video components
    private EglBase eglBase;  // No longer created here, must be set from Activity
    private EglBase.Context eglBaseContext;  // Received from Activity
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private CameraVideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private boolean isFrontCamera = true;
    private SurfaceViewRenderer localRenderer; // Store for later attachment

    private boolean isAudioEnabled = true;
    private boolean isVideoEnabled = true;
    private boolean isVideoCall = false;  // Track if this is a video call

    // Callbacks
    private ConnectionStateCallback connectionStateCallback;
    private IceCandidateCallback iceCandidateCallback;
    private RemoteStreamCallback remoteStreamCallback;

    public WebRtcRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Initialize PeerConnection
     * Must be called before creating offers/answers
     *
     * @param callId  Call ID for logging
     * @param isVideo Whether this is a video call (for future video support)
     */
    public void initializePeerConnection(@NonNull String callId, boolean isVideo) {
        Log.d(TAG, "Initializing PeerConnection for call: " + callId + ", isVideo: " + isVideo);

        // CRITICAL: Validate EglBase context is set for video calls
        if (isVideo && eglBaseContext == null) {
            Log.e(TAG, "FATAL: Video call requested but EglBase context not set! Call setEglBaseContext() first!");
            throw new IllegalStateException("EglBase context must be set before initializing video call");
        }

        // Store isVideo flag for later use in SDP creation
        this.isVideoCall = isVideo;

        // Create PeerConnectionFactory
        if (peerConnectionFactory == null) {
            peerConnectionFactory = WebRtcHelper.createPeerConnectionFactory(context);
        }

        // Create PeerConnection
        List<PeerConnection.IceServer> iceServers = WebRtcHelper.getDefaultIceServers();

        // DEBUG: Log all ICE servers
        Log.d(TAG, "===== ICE SERVERS CONFIGURATION =====");
        Log.d(TAG, "Total ICE servers: " + iceServers.size());
        for (int i = 0; i < iceServers.size(); i++) {
            PeerConnection.IceServer server = iceServers.get(i);
            Log.d(TAG, "ICE Server #" + (i + 1) + ": " + server.urls);
            if (server.username != null && !server.username.isEmpty()) {
                Log.d(TAG, "  Username: " + server.username);
                Log.d(TAG, "  Has Password: " + (server.password != null && !server.password.isEmpty()));
            }
        }
        Log.d(TAG, "====================================");

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

        // Set ICE transport policy - ALL uses both STUN (direct) and TURN (relay)
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
        Log.d(TAG, "ICE Transport Type: ALL (STUN + TURN)");

        // CRITICAL: Explicitly set Unified Plan SDP semantics
        // This is required for addTrack() API to work properly
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        // Create PeerConnection with observer
        peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                new PeerConnectionObserver()
        );

        if (peerConnection == null) {
            Log.e(TAG, "Failed to create PeerConnection");
            return;
        }

        // Create local audio track
        createLocalAudioTrack();

        // Create local video track if video call
        if (isVideo) {
            createLocalVideoTrack();
        }

        Log.d(TAG, "PeerConnection initialized successfully");
    }

    /**
     * Create local audio track and add to peer connection
     */
    private void createLocalAudioTrack() {
        // Create audio source with constraints
        MediaConstraints audioConstraints = WebRtcHelper.createAudioSourceConstraints();
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);

        // Create audio track
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        // Add audio track to peer connection using Unified Plan API
        // Note: Unified Plan uses addTrack with stream labels
        if (peerConnection != null) {
            java.util.List<String> streamLabels = List.of(STREAM_ID);
            audioSender = peerConnection.addTrack(localAudioTrack, streamLabels);
            Log.d(TAG, "Local audio track added to PeerConnection with stream: " + STREAM_ID);
        }
    }

    /**
     * Create SDP offer
     * Call this when initiating a call
     *
     * @param callback Callback for offer creation result
     */
    public void createOffer(@NonNull OfferCallback callback) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection not initialized");
            callback.onError("PeerConnection not initialized");
            return;
        }

        MediaConstraints sdpConstraints = WebRtcHelper.createSdpConstraints(isVideoCall);
        Log.d(TAG, "Creating offer with video=" + isVideoCall);

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "Offer created successfully");

                // Set local description
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sd) {
                        // Not used here
                    }

                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully");
                        callback.onOfferCreated(sessionDescription.description);
                    }

                    @Override
                    public void onCreateFailure(String error) {
                        Log.e(TAG, "Failed to set local description: " + error);
                        callback.onError(error);
                    }

                    @Override
                    public void onSetFailure(String error) {
                        Log.e(TAG, "Failed to set local description: " + error);
                        callback.onError(error);
                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {
                // Not used here
            }

            @Override
            public void onCreateFailure(String error) {
                Log.e(TAG, "Failed to create offer: " + error);
                callback.onError(error);
            }

            @Override
            public void onSetFailure(String error) {
                // Not used here
            }
        }, sdpConstraints);
    }

    /**
     * Create SDP answer
     * Call this when receiving a call
     *
     * @param remoteSdp Remote SDP offer
     * @param callback  Callback for answer creation result
     */
    public void createAnswer(@NonNull String remoteSdp, @NonNull AnswerCallback callback) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection not initialized");
            callback.onError("PeerConnection not initialized");
            return;
        }

        // Set remote description first
        SessionDescription remoteDescription = new SessionDescription(
                SessionDescription.Type.OFFER,
                remoteSdp
        );

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Not used here
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote description set successfully");

                // Create answer
                MediaConstraints sdpConstraints = WebRtcHelper.createSdpConstraints(isVideoCall);
                Log.d(TAG, "Creating answer with video=" + isVideoCall);

                peerConnection.createAnswer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        Log.d(TAG, "Answer created successfully");

                        // Set local description
                        peerConnection.setLocalDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sd) {
                                // Not used here
                            }

                            @Override
                            public void onSetSuccess() {
                                Log.d(TAG, "Local description set successfully");
                                callback.onAnswerCreated(sessionDescription.description);
                            }

                            @Override
                            public void onCreateFailure(String error) {
                                Log.e(TAG, "Failed to set local description: " + error);
                                callback.onError(error);
                            }

                            @Override
                            public void onSetFailure(String error) {
                                Log.e(TAG, "Failed to set local description: " + error);
                                callback.onError(error);
                            }
                        }, sessionDescription);
                    }

                    @Override
                    public void onSetSuccess() {
                        // Not used here
                    }

                    @Override
                    public void onCreateFailure(String error) {
                        Log.e(TAG, "Failed to create answer: " + error);
                        callback.onError(error);
                    }

                    @Override
                    public void onSetFailure(String error) {
                        // Not used here
                    }
                }, sdpConstraints);
            }

            @Override
            public void onCreateFailure(String error) {
                // Not used here
            }

            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "Failed to set remote description: " + error);
                callback.onError(error);
            }
        }, remoteDescription);
    }

    /**
     * Set remote SDP description (when receiving answer)
     *
     * @param sdp Remote SDP answer
     */
    public void setRemoteDescription(@NonNull String sdp) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection not initialized");
            return;
        }

        SessionDescription remoteDescription = new SessionDescription(
                SessionDescription.Type.ANSWER,
                sdp
        );

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Not used
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote description (answer) set successfully");
            }

            @Override
            public void onCreateFailure(String error) {
                // Not used
            }

            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "Failed to set remote description: " + error);
            }
        }, remoteDescription);
    }

    /**
     * Add ICE candidate received from remote peer
     *
     * @param candidate     ICE candidate string
     * @param sdpMid        SDP media stream identification
     * @param sdpMLineIndex SDP m-line index
     */
    public void addIceCandidate(@NonNull String candidate, String sdpMid, int sdpMLineIndex) {
        if (peerConnection == null) {
            Log.e(TAG, "PeerConnection not initialized");
            return;
        }

        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
        peerConnection.addIceCandidate(iceCandidate);
        Log.d(TAG, "ICE candidate added: " + candidate.substring(0, Math.min(50, candidate.length())));
    }

    /**
     * Toggle microphone on/off
     *
     * @param enabled true to enable, false to mute
     */
    public void toggleMicrophone(boolean enabled) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(enabled);
            isAudioEnabled = enabled;
            Log.d(TAG, "Microphone " + (enabled ? "enabled" : "muted"));
        }
    }

    /**
     * Check if microphone is enabled
     *
     * @return true if microphone is enabled
     */
    public boolean isMicrophoneEnabled() {
        return isAudioEnabled;
    }

    /**
     * Create local video track and add to peer connection
     */
    private void createLocalVideoTrack() {
        // CRITICAL: eglBaseContext MUST be set from Activity before calling this
        if (eglBaseContext == null) {
            Log.e(TAG, "CRITICAL ERROR: eglBaseContext is null! Must call setEglBaseContext() first!");
            return;
        }

        // Create camera capturer
        videoCapturer = createCameraCapturer(true);  // Start with front camera
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to create camera capturer");
            return;
        }

        // Create surface texture helper using the SHARED EglBase context from Activity
        surfaceTextureHelper = SurfaceTextureHelper.create(
                "CameraThread",
                eglBaseContext  // Use context from Activity!
        );

        Log.d(TAG, "SurfaceTextureHelper created with shared EglBase context");

        // Create video source
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(
                surfaceTextureHelper,
                context,
                videoSource.getCapturerObserver()
        );

        // Start capturing with conservative defaults; fallback to an even lower profile on error
        try {
            videoCapturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS);
        } catch (RuntimeException captureError) {
            Log.e(TAG, "Primary capture profile failed, applying fallback", captureError);
            try {
                videoCapturer.startCapture(VIDEO_FALLBACK_WIDTH, VIDEO_FALLBACK_HEIGHT, VIDEO_FALLBACK_FPS);
            } catch (RuntimeException fallbackError) {
                Log.e(TAG, "Fallback capture profile failed", fallbackError);
            }
        }

        // Create video track
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(true);

        // Add video track to peer connection using Unified Plan API
        if (peerConnection != null) {
            java.util.List<String> streamLabels = List.of(STREAM_ID);
            videoSender = peerConnection.addTrack(localVideoTrack, streamLabels);
            Log.d(TAG, "Local video track added to PeerConnection with stream: " + STREAM_ID);
        }

        Log.d(TAG, "Local video track created and capturing started");

        // Auto-attach to local renderer if it was set up before track creation
        if (localRenderer != null) {
            Log.d(TAG, "Auto-attaching local video track to pre-initialized renderer");
            localVideoTrack.addSink(localRenderer);
            localRenderer.setMirror(isFrontCamera);
            Log.d(TAG, "Local video track attached to renderer (mirror=" + isFrontCamera + ")");
        }
    }

    /**
     * Create camera capturer for video
     *
     * @param useFrontCamera true to use front camera, false for back camera
     * @return CameraVideoCapturer or null if failed
     */
    private CameraVideoCapturer createCameraCapturer(boolean useFrontCamera) {
        CameraEnumerator enumerator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            enumerator = new Camera2Enumerator(context);
        } else {
            enumerator = new Camera1Enumerator(false);
        }

        String[] deviceNames = enumerator.getDeviceNames();

        // First try to find the requested camera
        for (String deviceName : deviceNames) {
            if (useFrontCamera == enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) {
                    Log.d(TAG, "Camera created: " + deviceName);
                    return capturer;
                }
            }
        }

        // Fallback: try any camera
        for (String deviceName : deviceNames) {
            CameraVideoCapturer capturer = enumerator.createCapturer(deviceName, null);
            if (capturer != null) {
                Log.d(TAG, "Fallback camera created: " + deviceName);
                return capturer;
            }
        }

        return null;
    }

    /**
     * Toggle camera on/off
     *
     * @param enabled true to enable, false to disable
     */
    public void toggleCamera(boolean enabled) {
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(enabled);
            isVideoEnabled = enabled;
            Log.d(TAG, "Camera " + (enabled ? "enabled" : "disabled"));
        }
    }

    /**
     * Switch between front and back camera
     */
    public void switchCamera() {
        if (videoCapturer != null) {
            videoCapturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean isFrontCamera) {
                    WebRtcRepository.this.isFrontCamera = isFrontCamera;
                    Log.d(TAG, "Camera switched to " + (isFrontCamera ? "front" : "back"));
                }

                @Override
                public void onCameraSwitchError(String errorDescription) {
                    Log.e(TAG, "Camera switch error: " + errorDescription);
                }
            });
        }
    }

    /**
     * Check if camera is enabled
     *
     * @return true if camera is enabled
     */
    public boolean isCameraEnabled() {
        return isVideoEnabled;
    }

    /**
     * Set EglBase context from Activity
     * MUST be called before initializing video call
     *
     * @param eglBaseContext EglBase context from Activity's EglBase.create()
     */
    public void setEglBaseContext(EglBase.Context eglBaseContext) {
        this.eglBaseContext = eglBaseContext;
        Log.d(TAG, "EglBase context set from Activity");
    }

    /**
     * Attach local video to renderer
     * Call this to show local camera preview
     *
     * @param renderer SurfaceViewRenderer for local video (must already be initialized)
     */
    public void attachLocalRenderer(SurfaceViewRenderer renderer) {
        Log.d(TAG, "===== ATTACHING LOCAL RENDERER =====");

        // Store renderer reference
        this.localRenderer = renderer;

        // If localVideoTrack already exists, attach immediately
        if (localVideoTrack != null) {
            renderer.setMirror(isFrontCamera);
            localVideoTrack.addSink(renderer);
            Log.d(TAG, "Local video track attached immediately (mirror=" + isFrontCamera + ")");
        } else {
            // localVideoTrack will be created later in initializePeerConnection
            // and will auto-attach to this stored renderer
            Log.d(TAG, "Renderer stored, will attach when localVideoTrack is created");
        }

        Log.d(TAG, "===== LOCAL RENDERER ATTACHED =====");
    }

    /**
     * Detach local video from renderer
     *
     * @param renderer SurfaceViewRenderer to detach
     */
    public void detachLocalRenderer(SurfaceViewRenderer renderer) {
        if (localVideoTrack != null) {
            localVideoTrack.removeSink(renderer);
        }
        renderer.release();
        Log.d(TAG, "Local video renderer detached");
    }

    /**
     * Close PeerConnection and cleanup resources
     */
    public void closePeerConnection() {
        Log.d(TAG, "Closing PeerConnection");

        // Stop video capturing
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping video capture", e);
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }

        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }

        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }

        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }

        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }

        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        // Clear RtpSender references
        audioSender = null;
        videoSender = null;

        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }

        // Note: EglBase is owned by Activity, don't dispose it here
        // PeerConnectionFactory should be kept alive for potential future calls
        // Only dispose when app is closing

        Log.d(TAG, "PeerConnection closed and resources cleaned up");
    }

    /**
     * Dispose PeerConnectionFactory
     * Call this when app is closing
     */
    public void dispose() {
        closePeerConnection();

        // Note: EglBase is owned by Activity, don't dispose it here

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
    }

    /**
     * Set connection state callback
     *
     * @param callback Callback for connection state changes
     */
    public void setConnectionStateCallback(ConnectionStateCallback callback) {
        this.connectionStateCallback = callback;
    }

    /**
     * Set ICE candidate callback
     *
     * @param callback Callback for ICE candidate generation
     */
    public void setIceCandidateCallback(IceCandidateCallback callback) {
        this.iceCandidateCallback = callback;
    }

    /**
     * Set remote stream callback
     *
     * @param callback Callback for remote stream
     */
    public void setRemoteStreamCallback(RemoteStreamCallback callback) {
        this.remoteStreamCallback = callback;
    }

    // ==================== PeerConnection Observer ====================

    public interface OfferCallback {
        void onOfferCreated(String sdp);

        void onError(String error);
    }

    // ==================== Callback Interfaces ====================

    public interface AnswerCallback {
        void onAnswerCreated(String sdp);

        void onError(String error);
    }

    public interface IceCandidateCallback {
        void onIceCandidateGenerated(String candidate, String sdpMid, int sdpMLineIndex);
    }

    public interface RemoteStreamCallback {
        void onRemoteStream(MediaStream mediaStream);
    }

    public interface ConnectionStateCallback {
        void onConnected();

        void onDisconnected();

        void onFailed();
    }

    /**
     * PeerConnection observer to handle connection events
     */
    private class PeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "Signaling state changed: " + signalingState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "ICE connection state changed: " + iceConnectionState);

            if (connectionStateCallback != null) {
                switch (iceConnectionState) {
                    case CONNECTED:
                        connectionStateCallback.onConnected();
                        break;
                    case DISCONNECTED:
                        connectionStateCallback.onDisconnected();
                        break;
                    case FAILED:
                        connectionStateCallback.onFailed();
                        break;
                }
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "ICE connection receiving change: " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "===== ICE GATHERING STATE CHANGE =====");
            Log.d(TAG, "State: " + iceGatheringState);

            switch (iceGatheringState) {
                case NEW:
                    Log.d(TAG, "ICE gathering: NEW - Not started yet");
                    break;
                case GATHERING:
                    Log.d(TAG, "ICE gathering: GATHERING - In progress, waiting for candidates");
                    break;
                case COMPLETE:
                    Log.d(TAG, "ICE gathering: COMPLETE - All candidates collected");
                    Log.d(TAG, "If you don't see RELAY or SRFLX candidates above, TURN/STUN servers failed!");
                    break;
            }
            Log.d(TAG, "======================================");
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            // Log FULL candidate to see type (relay/srflx/host)
            Log.d(TAG, "===== ICE CANDIDATE GENERATED =====");
            Log.d(TAG, "Full SDP: " + iceCandidate.sdp);

            // Parse candidate type for debugging
            String type = "unknown";
            if (iceCandidate.sdp.contains(" typ relay ")) {
                type = "RELAY (TURN server)";
            } else if (iceCandidate.sdp.contains(" typ srflx ")) {
                type = "SERVER-REFLEXIVE (STUN)";
            } else if (iceCandidate.sdp.contains(" typ host ")) {
                type = "HOST (local)";
            }
            Log.d(TAG, "Candidate TYPE: " + type);
            Log.d(TAG, "===================================");

            if (iceCandidateCallback != null) {
                iceCandidateCallback.onIceCandidateGenerated(
                        iceCandidate.sdp,
                        iceCandidate.sdpMid,
                        iceCandidate.sdpMLineIndex
                );
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "ICE candidates removed: " + iceCandidates.length);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "Remote stream added: " + mediaStream.getId());
            // Remote audio will automatically play through earpiece/speaker
            // Notify callback for remote video handling
            if (remoteStreamCallback != null) {
                remoteStreamCallback.onRemoteStream(mediaStream);
            }
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "Remote stream removed: " + mediaStream.getId());
        }

        @Override
        public void onDataChannel(org.webrtc.DataChannel dataChannel) {
            Log.d(TAG, "Data channel: " + dataChannel.label());
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "Renegotiation needed");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "Track added: " + rtpReceiver.id());
        }
    }
}
