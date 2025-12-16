package com.example.doan_zaloclone.models;

import com.google.firebase.firestore.Exclude;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing WebRTC signaling data
 * Used to exchange SDP offers/answers and ICE candidates between peers via Firestore
 */
public class CallSignal {
    // Signal types
    public static final String TYPE_OFFER = "OFFER";              // SDP offer from caller
    public static final String TYPE_ANSWER = "ANSWER";            // SDP answer from receiver
    public static final String TYPE_ICE_CANDIDATE = "ICE_CANDIDATE";  // ICE candidate
    
    private String id;
    private String callId;         // Reference to the call this signal belongs to
    private String senderId;       // User ID who sent this signal
    private String type;           // OFFER, ANSWER, or ICE_CANDIDATE
    private String sdp;            // Session Description Protocol - for OFFER and ANSWER
    private Map<String, Object> iceCandidate;  // ICE candidate data - for ICE_CANDIDATE type
    private long timestamp;        // When signal was created
    
    // Empty constructor required for Firestore serialization/deserialization
    public CallSignal() {
        this.timestamp = System.currentTimeMillis();
        this.iceCandidate = new HashMap<>();
    }
    
    // Constructor for SDP signals (OFFER or ANSWER)
    public CallSignal(String id, String callId, String senderId, String type, String sdp) {
        this.id = id;
        this.callId = callId;
        this.senderId = senderId;
        this.type = type;
        this.sdp = sdp;
        this.iceCandidate = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    // Constructor for ICE candidate signals
    public CallSignal(String id, String callId, String senderId, Map<String, Object> iceCandidate) {
        this.id = id;
        this.callId = callId;
        this.senderId = senderId;
        this.type = TYPE_ICE_CANDIDATE;
        this.iceCandidate = iceCandidate;
        this.sdp = null;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Full constructor
    public CallSignal(String id, String callId, String senderId, String type, 
                     String sdp, Map<String, Object> iceCandidate, long timestamp) {
        this.id = id;
        this.callId = callId;
        this.senderId = senderId;
        this.type = type;
        this.sdp = sdp;
        this.iceCandidate = iceCandidate != null ? iceCandidate : new HashMap<>();
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getCallId() {
        return callId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getType() {
        return type;
    }
    
    public String getSdp() {
        return sdp;
    }
    
    public Map<String, Object> getIceCandidate() {
        return iceCandidate;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    // Setters (required for Firestore)
    public void setId(String id) {
        this.id = id;
    }
    
    public void setCallId(String callId) {
        this.callId = callId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setSdp(String sdp) {
        this.sdp = sdp;
    }
    
    public void setIceCandidate(Map<String, Object> iceCandidate) {
        this.iceCandidate = iceCandidate;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    // Helper methods
    
    /**
     * Check if this is an OFFER signal
     */
    @Exclude
    public boolean isOfferSignal() {
        return TYPE_OFFER.equals(this.type);
    }
    /**
     * Check if this is an ANSWER signal
     */
    @Exclude
    public boolean isAnswerSignal() {
        return TYPE_ANSWER.equals(this.type);
    }
    /**
     * Check if this is an ICE candidate signal
     */
    @Exclude
    public boolean isIceCandidateSignal() {
        return TYPE_ICE_CANDIDATE.equals(this.type);
    }
    
    /**
     * Helper to create ICE candidate map from WebRTC IceCandidate
     * @param candidate The candidate string
     * @param sdpMid The SDP media stream identification
     * @param sdpMLineIndex The SDP m-line index
     * @return Map suitable for Firestore storage
     */
    public static Map<String, Object> createIceCandidateMap(String candidate, 
                                                             String sdpMid, 
                                                             int sdpMLineIndex) {
        Map<String, Object> map = new HashMap<>();
        map.put("candidate", candidate);
        map.put("sdpMid", sdpMid);
        map.put("sdpMLineIndex", sdpMLineIndex);
        return map;
    }
    
    /**
     * Extract candidate string from ICE candidate map
     */
    @Exclude
    public String getCandidateString() {
        if (iceCandidate == null) return null;
        Object candidate = iceCandidate.get("candidate");
        return candidate != null ? candidate.toString() : null;
    }
    /**
     * Extract sdpMid from ICE candidate map
     */
    @Exclude
    public String getSdpMid() {
        if (iceCandidate == null) return null;
        Object sdpMid = iceCandidate.get("sdpMid");
        return sdpMid != null ? sdpMid.toString() : null;
    }
    /**
     * Extract sdpMLineIndex from ICE candidate map
     */
    @Exclude
    public Integer getSdpMLineIndex() {
        if (iceCandidate == null) return null;
        Object index = iceCandidate.get("sdpMLineIndex");
        if (index instanceof Number) {
            return ((Number) index).intValue();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "CallSignal{" +
                "id='" + id + '\'' +
                ", callId='" + callId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", type='" + type + '\'' +
                ", hasSdp=" + (sdp != null) +
                ", hasIceCandidate=" + (iceCandidate != null && !iceCandidate.isEmpty()) +
                '}';
    }
}
