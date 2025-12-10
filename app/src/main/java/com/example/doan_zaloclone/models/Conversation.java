package com.example.doan_zaloclone.models;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private String id;
    private String name;
    private String lastMessage;
    private long timestamp;
    private List<String> memberIds; // List userId để query conversations của user

    // Empty constructor bắt buộc cho Firestore serialization/deserialization
    public Conversation() {
        this.memberIds = new ArrayList<>();
    }

    // Constructor cũ (backward compatible)
    public Conversation(String id, String name, String lastMessage, long timestamp) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = new ArrayList<>();
    }

    // Constructor đầy đủ
    public Conversation(String id, String name, String lastMessage, long timestamp, List<String> memberIds) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
    }

    // Getters
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

    public List<String> getMemberIds() {
        return memberIds;
    }

    // Setters (cần cho Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    // Helper methods
    public void addMember(String userId) {
        if (this.memberIds == null) {
            this.memberIds = new ArrayList<>();
        }
        if (!this.memberIds.contains(userId)) {
            this.memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        if (this.memberIds != null) {
            this.memberIds.remove(userId);
        }
    }

    public boolean hasMember(String userId) {
        return this.memberIds != null && this.memberIds.contains(userId);
    }
}
