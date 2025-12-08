package com.example.doan_zaloclone.models;

public class Message {
    private String id;
    private String senderId;
    private String content;
    private long timestamp;

    public Message(String id, String senderId, String content, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
