package com.example.doan_zaloclone.utils;

import android.content.Context;

import com.example.doan_zaloclone.BuildConfig;

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
     * Includes multiple TURN servers for reliable cross-network connectivity
     *
     * @return List of ICE servers
     */
    public static List<PeerConnection.IceServer> getDefaultIceServers() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        // Add STUN servers for NAT traversal
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER_1).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER_2).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER_3).createIceServer());

        // ============================================================
        // TURN SERVER - Self-hosted Coturn VPS (credentials from BuildConfig)
        // ============================================================
        String coturnUsername = BuildConfig.TURN_SERVER_USERNAME;
        String coturnCredential = BuildConfig.TURN_SERVER_PASSWORD;
        String coturnServer = BuildConfig.TURN_SERVER_HOST;

        // TURN UDP (preferred for media)
        iceServers.add(
                PeerConnection.IceServer.builder("turn:" + coturnServer + ":3478")
                        .setUsername(coturnUsername)
                        .setPassword(coturnCredential)
                        .createIceServer()
        );

        // TURN TCP (fallback)
        iceServers.add(
                PeerConnection.IceServer.builder("turn:" + coturnServer + ":3478?transport=tcp")
                        .setUsername(coturnUsername)
                        .setPassword(coturnCredential)
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
     * Controls audio capture settings for optimal voice quality
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

        // Enhanced audio quality settings
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googAudioMirroring", "false"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googExperimentalEchoCancellation", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googExperimentalAutoGainControl", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googExperimentalNoiseSuppression", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"));

        return constraints;
    }

    /**
     * Create video source constraints
     * Controls video capture settings with adaptive resolution
     * WebRTC automatically adjusts quality based on network bandwidth
     *
     * @return MediaConstraints for video source
     */
    public static MediaConstraints createVideoSourceConstraints() {
        MediaConstraints constraints = new MediaConstraints();

        // Adaptive video quality (1080p to 360p)
        // WebRTC automatically scales based on available bandwidth
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "1920"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "1080"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "640"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "360"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "30"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", "10"));

        return constraints;
    }
}
