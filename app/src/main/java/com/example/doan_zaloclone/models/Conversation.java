package com.example.doan_zaloclone.models;

public class Conversation {
    private String id;
    private String name;
    private String lastMessage;
    private long timestamp;

    public Conversation(String id, String name, String lastMessage, long timestamp) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
