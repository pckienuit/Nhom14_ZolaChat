package com.example.doan_zaloclone.models;

public class Message {
    // Message types
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";

    private String id;
    private String senderId;
    private String content;
    private String type; // TEXT hoặc IMAGE
    private long timestamp;

    // Empty constructor bắt buộc cho Firestore serialization/deserialization
    public Message() {
        this.type = TYPE_TEXT; // Default type
    }

    // Constructor cũ (backward compatible) - mặc định type là TEXT
    public Message(String id, String senderId, String content, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.type = TYPE_TEXT;
    }

    // Constructor đầy đủ
    public Message(String id, String senderId, String content, String type, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.type = type != null ? type : TYPE_TEXT;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters (cần cho Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Helper methods
    public boolean isTextMessage() {
        return TYPE_TEXT.equals(this.type);
    }

    public boolean isImageMessage() {
        return TYPE_IMAGE.equals(this.type);
    }
}
