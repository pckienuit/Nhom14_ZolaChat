package com.example.doan_zaloclone.models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private Map<String, Boolean> devices; // Map<deviceToken, true> để hỗ trợ đa thiết bị

    // Empty constructor bắt buộc cho Firestore serialization/deserialization
    public User() {
        this.devices = new HashMap<>();
    }

    // Constructor đầy đủ
    public User(String id, String name, String email, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.devices = new HashMap<>();
    }

    // Constructor với devices
    public User(String id, String name, String email, String avatarUrl, Map<String, Boolean> devices) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.devices = devices != null ? devices : new HashMap<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Map<String, Boolean> getDevices() {
        return devices;
    }

    // Setters (cần cho Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setDevices(Map<String, Boolean> devices) {
        this.devices = devices;
    }

    // Helper methods
    public void addDevice(String deviceToken) {
        if (this.devices == null) {
            this.devices = new HashMap<>();
        }
        this.devices.put(deviceToken, true);
    }

    public void removeDevice(String deviceToken) {
        if (this.devices != null) {
            this.devices.remove(deviceToken);
        }
    }

    public boolean hasDevice(String deviceToken) {
        return this.devices != null && this.devices.containsKey(deviceToken);
    }
}
