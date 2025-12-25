package com.example.doan_zaloclone.models;

public class FriendRequest {
    private String id;
    private String fromUserId;
    private String toUserId;
    private String fromUserName;
    private String fromUserEmail;
    private String status;
    private long timestamp;

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

    // Setters (needed for Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getFromUserEmail() {
        return fromUserEmail;
    }

    public void setFromUserEmail(String fromUserEmail) {
        this.fromUserEmail = fromUserEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendRequest that = (FriendRequest) o;
        return timestamp == that.timestamp &&
                java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(fromUserId, that.fromUserId) &&
                java.util.Objects.equals(toUserId, that.toUserId) &&
                java.util.Objects.equals(fromUserName, that.fromUserName) &&
                java.util.Objects.equals(fromUserEmail, that.fromUserEmail) &&
                java.util.Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, fromUserId, toUserId, fromUserName,
                fromUserEmail, status, timestamp);
    }

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }
}
