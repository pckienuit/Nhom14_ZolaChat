package com.example.doan_zaloclone.models;

public class LiveLocation {
    private String sessionId;
    private String senderId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private boolean isActive;
    private long endTime; // Timestamp when sharing ends

    public LiveLocation() {
        // Required for Firestore
    }

    public LiveLocation(String sessionId, String senderId, double latitude, double longitude, long timestamp, long endTime) {
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.endTime = endTime;
        this.isActive = true;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
}
