package com.example.doan_zaloclone.models;

public class FriendRequest {
    private String id;
    private String fromUserId;
    private String toUserId;
    private String fromUserName;
    private String fromUserEmail;
    private String status;
    private long timestamp;

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    // Empty constructor for Firestore
    public FriendRequest() {
    }

    // Full constructor
    public FriendRequest(String id, String fromUserId, String toUserId, 
                        String fromUserName, String fromUserEmail, 
                        String status, long timestamp) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.fromUserName = fromUserName;
        this.fromUserEmail = fromUserEmail;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public String getFromUserEmail() {
        return fromUserEmail;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters (needed for Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public void setFromUserEmail(String fromUserEmail) {
        this.fromUserEmail = fromUserEmail;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Helper methods for backward compatibility
    public String getName() {
        return fromUserName;
    }

    public String getEmail() {
        return fromUserEmail;
    }
}
