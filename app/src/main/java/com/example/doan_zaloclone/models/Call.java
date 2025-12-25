package com.example.doan_zaloclone.models;

/**
 * Model class representing a voice or video call
 * Stores call metadata and state in Firestore
 */
public class Call {
    // Call types
    public static final String TYPE_VOICE = "VOICE";
    public static final String TYPE_VIDEO = "VIDEO";

    // Call statuses
    public static final String STATUS_CALLING = "CALLING";     // Caller waiting for receiver to pick up
    public static final String STATUS_RINGING = "RINGING";     // Receiver's phone is ringing
    public static final String STATUS_CONNECTED = "CONNECTED"; // Call connected (WebRTC established)
    public static final String STATUS_ONGOING = "ONGOING";     // Call in progress
    public static final String STATUS_ENDED = "ENDED";         // Call ended normally
    public static final String STATUS_MISSED = "MISSED";       // Receiver didn't answer
    public static final String STATUS_REJECTED = "REJECTED";   // Receiver declined
    public static final String STATUS_FAILED = "FAILED";       // Connection failed

    private String id;
    private String conversationId;
    private String callerId;
    private String receiverId;
    private String type;           // VOICE or VIDEO
    private String status;         // CALLING, RINGING, ONGOING, etc.
    private long startTime;        // Timestamp when call was initiated
    private long endTime;          // Timestamp when call ended (0 if ongoing)
    private long duration;         // Duration in seconds (0 if not ended)

    // Empty constructor required for Firestore serialization/deserialization
    public Call() {
        this.type = TYPE_VOICE;
        this.status = STATUS_CALLING;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.duration = 0;
    }

    // Constructor for creating a new call
    public Call(String id, String conversationId, String callerId, String receiverId, String type) {
        this.id = id;
        this.conversationId = conversationId;
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.type = type != null ? type : TYPE_VOICE;
        this.status = STATUS_CALLING;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.duration = 0;
    }

    // Full constructor
    public Call(String id, String conversationId, String callerId, String receiverId,
                String type, String status, long startTime, long endTime, long duration) {
        this.id = id;
        this.conversationId = conversationId;
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.type = type != null ? type : TYPE_VOICE;
        this.status = status != null ? status : STATUS_CALLING;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
    }

    // Getters
    public String getId() {
        return id;
    }

    // Setters (required for Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    // Helper methods

    /**
     * Check if this is a voice call
     */
    public boolean isVoiceCall() {
        return TYPE_VOICE.equals(this.type);
    }

    /**
     * Check if this is a video call
     */
    public boolean isVideoCall() {
        return TYPE_VIDEO.equals(this.type);
    }

    /**
     * Check if call is currently active (ongoing or ringing)
     */
    public boolean isActive() {
        return STATUS_ONGOING.equals(this.status) ||
                STATUS_RINGING.equals(this.status) ||
                STATUS_CALLING.equals(this.status);
    }

    /**
     * Check if call has ended (any terminal state)
     */
    public boolean isEnded() {
        return STATUS_ENDED.equals(this.status) ||
                STATUS_MISSED.equals(this.status) ||
                STATUS_REJECTED.equals(this.status) ||
                STATUS_FAILED.equals(this.status);
    }

    /**
     * Format duration to MM:SS or HH:MM:SS
     *
     * @return Formatted duration string
     */
    public String getFormattedDuration() {
        if (duration <= 0) {
            return "00:00";
        }

        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Get user-friendly status text
     */
    public String getStatusText() {
        switch (status) {
            case STATUS_CALLING:
                return "Đang gọi...";
            case STATUS_RINGING:
                return "Đang đổ chuông...";
            case STATUS_ONGOING:
                return "Đang gọi";
            case STATUS_ENDED:
                return "Đã kết thúc";
            case STATUS_MISSED:
                return "Cuộc gọi nhỡ";
            case STATUS_REJECTED:
                return "Đã từ chối";
            case STATUS_FAILED:
                return "Cuộc gọi thất bại";
            default:
                return status;
        }
    }

    @Override
    public String toString() {
        return "Call{" +
                "id='" + id + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", callerId='" + callerId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", duration=" + duration +
                '}';
    }
}
