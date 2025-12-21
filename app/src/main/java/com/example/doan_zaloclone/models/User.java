package com.example.doan_zaloclone.models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private Map<String, Boolean> devices; // Map<deviceToken, true> để hỗ trợ đa thiết bị
    
    // Custom tags - map of tag name to boolean (for Firestore compatibility)
    private Map<String, Boolean> customTags;
    
    // Custom tag colors - map of tag name to color integer
    private Map<String, Integer> customTagColors;

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
    
    // Custom tags getters/setters
    public java.util.List<String> getCustomTags() {
        if (customTags == null) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(customTags.keySet());
    }
    
    public Map<String, Boolean> getCustomTagsMap() {
        return customTags != null ? customTags : new HashMap<>();
    }
    
    public void setCustomTags(Map<String, Boolean> customTags) {
        this.customTags = customTags;
    }
    
    public Map<String, Integer> getCustomTagColors() {
        return customTagColors != null ? customTagColors : new HashMap<>();
    }
    
    public void setCustomTagColors(Map<String, Integer> customTagColors) {
        this.customTagColors = customTagColors;
    }
    
    // Custom tags helper methods
    public void addCustomTag(String tagName, int color) {
        if (this.customTags == null) {
            this.customTags = new HashMap<>();
        }
        if (this.customTagColors == null) {
            this.customTagColors = new HashMap<>();
        }
        
        this.customTags.put(tagName, true);
        this.customTagColors.put(tagName, color);
    }
    
    public void removeCustomTag(String tagName) {
        if (this.customTags != null) {
            this.customTags.remove(tagName);
        }
        if (this.customTagColors != null) {
            this.customTagColors.remove(tagName);
        }
    }
    
    public boolean hasCustomTag(String tagName) {
        return this.customTags != null && this.customTags.containsKey(tagName);
    }
    
    public Integer getCustomTagColor(String tagName) {
        if (this.customTagColors != null && this.customTagColors.containsKey(tagName)) {
            return this.customTagColors.get(tagName);
        }
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return java.util.Objects.equals(id, user.id) &&
               java.util.Objects.equals(name, user.name) &&
               java.util.Objects.equals(email, user.email) &&
               java.util.Objects.equals(avatarUrl, user.avatarUrl);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, email, avatarUrl);
    }
}
