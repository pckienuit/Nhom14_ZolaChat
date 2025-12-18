package com.example.doan_zaloclone.utils;

import android.content.Context;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for WebRTC operations
 * Provides utilities for creating PeerConnectionFactory, ICE servers, and media constraints
 */
public class WebRtcHelper {
    private static final String TAG = "WebRtcHelper";
    
    // Google's free STUN servers for development
    // For production, should use own STUN/TURN servers
    private static final String STUN_SERVER_1 = "stun:stun.l.google.com:19302";
    private static final String STUN_SERVER_2 = "stun:stun1.l.google.com:19302";
    private static final String STUN_SERVER_3 = "stun:stun2.l.google.com:19302";
    
    /**
     * Get default ICE servers for WebRTC connection
     * Uses Google's free STUN servers for NAT traversal
     * 
     * @return List of ICE servers
     */
    public static List<PeerConnection.IceServer> getDefaultIceServers() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        
        // Add STUN servers for NAT traversal
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER_1).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER_2).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER_3).createIceServer());
        
        // Add FREE TURN servers from Metered.ca
        // More reliable than Open Relay Project for testing
        iceServers.add(
            PeerConnection.IceServer.builder("turn:a.relay.metered.ca:80")
                .setUsername("87662b8ca63f0c36f8afb86f")
                .setPassword("W91wCu2RKr6GmGd/")
                .createIceServer()
        );
        
        iceServers.add(
            PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443")
                .setUsername("87662b8ca63f0c36f8afb86f")
                .setPassword("W91wCu2RKr6GmGd/")
                .createIceServer()
        );
        
        iceServers.add(
            PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                .setUsername("87662b8ca63f0c36f8afb86f")
                .setPassword("W91wCu2RKr6GmGd/")
                .createIceServer()
        );
        
        return iceServers;
    }
    
    /**
     * Create PeerConnectionFactory for WebRTC
     * Must be called before creating any PeerConnection
     * 
     * @param context Application context
     * @return Configured PeerConnectionFactory
     */
    public static PeerConnectionFactory createPeerConnectionFactory(Context context) {
        // Initialize PeerConnectionFactory
        PeerConnectionFactory.InitializationOptions initOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);
        
        // Create EglBase for video rendering
        EglBase rootEglBase = EglBase.create();
        
        // Build PeerConnectionFactory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                        rootEglBase.getEglBaseContext(),
                        true,  // enableIntelVp8Encoder
                        true   // enableH264HighProfile
                ))
                .createPeerConnectionFactory();
    }
    
    /**
     * Create media constraints for audio-only call
     * 
     * @return MediaConstraints for audio
     */
    public static MediaConstraints createAudioConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        
        // Mandatory constraints
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        
        // Optional constraints for better audio quality
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        
        return constraints;
    }
    
    /**
     * Create media constraints for video call
     * 
     * @return MediaConstraints for video
     */
    public static MediaConstraints createVideoConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        
        // Mandatory constraints
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        
        // Optional constraints
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        
        return constraints;
    }
    
    /**
     * Create SDP constraints (used when creating offer/answer)
     * 
     * @param isVideo Whether this is a video call
     * @return MediaConstraints for SDP
     */
    public static MediaConstraints createSdpConstraints(boolean isVideo) {
        MediaConstraints constraints = new MediaConstraints();
        
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideo ? "true" : "false"));
        
        return constraints;
    }
    
    /**
     * Create audio source constraints
     * Controls audio capture settings
     * 
     * @return MediaConstraints for audio source
     */
    public static MediaConstraints createAudioSourceConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        
        // Enable echo cancellation and noise suppression
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        
        return constraints;
    }
    
    /**
     * Create video source constraints
     * Controls video capture settings (resolution, fps)
     * 
     * @return MediaConstraints for video source
     */
    public static MediaConstraints createVideoSourceConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        
        // Set video resolution and frame rate
        // Favor stability: lower the envelope to keep render/load balanced
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "960"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "960"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "480"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "360"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "24"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", "15"));
        
        return constraints;
    }
}
