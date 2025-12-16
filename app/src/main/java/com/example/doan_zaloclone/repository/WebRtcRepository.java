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
    private static final String LOCAL_STREAM_ID = "local_stream";
    
    private final Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private MediaStream localMediaStream;
    
    // Video components
    private EglBase eglBase;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private CameraVideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private boolean isFrontCamera = true;
    
    private boolean isAudioEnabled = true;
    private boolean isVideoEnabled = true;
    
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
     * @param callId Call ID for logging
     * @param isVideo Whether this is a video call (for future video support)
     */
    public void initializePeerConnection(@NonNull String callId, boolean isVideo) {
        Log.d(TAG, "Initializing PeerConnection for call: " + callId + ", isVideo: " + isVideo);
        
        // Create PeerConnectionFactory
        if (peerConnectionFactory == null) {
            peerConnectionFactory = WebRtcHelper.createPeerConnectionFactory(context);
        }
        
        // Create PeerConnection
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(
                WebRtcHelper.getDefaultIceServers()
        );
        
        // Set ICE transport policy (relay for TURN-only, all for STUN+TURN)
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
        
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
        
        // Create local media stream and add track
        localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
        localMediaStream.addTrack(localAudioTrack);
        
        // Add stream to peer connection
        if (peerConnection != null) {
            peerConnection.addStream(localMediaStream);
            Log.d(TAG, "Local audio track added to PeerConnection");
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
        
        MediaConstraints sdpConstraints = WebRtcHelper.createSdpConstraints(false);  // Audio only for now
        
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
     * @param callback Callback for answer creation result
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
                MediaConstraints sdpConstraints = WebRtcHelper.createSdpConstraints(false);
                
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
     * @param candidate ICE candidate string
     * @param sdpMid SDP media stream identification
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
        // Create EglBase for video rendering
        if (eglBase == null) {
            eglBase = EglBase.create();
        }
        
        // Create camera capturer
        videoCapturer = createCameraCapturer(true);  // Start with front camera
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to create camera capturer");
            return;
        }
        
        // Create surface texture helper
        surfaceTextureHelper = SurfaceTextureHelper.create(
                "CameraThread",
                eglBase.getEglBaseContext()
        );
        
        // Create video source
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(
                surfaceTextureHelper,
                context,
                videoSource.getCapturerObserver()
        );
        
        // Start capturing
        videoCapturer.startCapture(720, 1280, 30);  // Portrait mode
        
        // Create video track
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(true);
        
        // Add video track to media stream
        if (localMediaStream != null) {
            localMediaStream.addTrack(localVideoTrack);
            Log.d(TAG, "Local video track added to MediaStream");
        }
        
        Log.d(TAG, "Local video track created and capturing started");
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
     * Attach local video to renderer
     * Call this to show local camera preview
     * 
     * @param renderer SurfaceViewRenderer for local video
     */
    public void attachLocalRenderer(SurfaceViewRenderer renderer) {
        if (eglBase == null) {
            Log.e(TAG, "EglBase not initialized");
            return;
        }
        
        renderer.init(eglBase.getEglBaseContext(), null);
        renderer.setMirror(isFrontCamera);  // Mirror for front camera
        
        if (localVideoTrack != null) {
            localVideoTrack.addSink(renderer);
            Log.d(TAG, "Local video renderer attached");
        }
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
        
        if (localMediaStream != null) {
            localMediaStream.dispose();
            localMediaStream = null;
        }
        
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }
        
        // Note: PeerConnectionFactory and EglBase should be kept alive for potential future calls
        // Only dispose when app is closing
        
        Log.d(TAG, "PeerConnection closed and resources cleaned up");
    }
    
    /**
     * Dispose PeerConnectionFactory
     * Call this when app is closing
     */
    public void dispose() {
        closePeerConnection();
        
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
        
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
            Log.d(TAG, "ICE gathering state changed: " + iceGatheringState);
        }
        
        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "ICE candidate generated: " + iceCandidate.sdp.substring(0, Math.min(50, iceCandidate.sdp.length())));
            
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
    
    // ==================== Callback Interfaces ====================
    
    public interface OfferCallback {
        void onOfferCreated(String sdp);
        void onError(String error);
    }
    
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
}
