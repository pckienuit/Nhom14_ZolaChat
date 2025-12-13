package com.example.doan_zaloclone.models;

public class GroupMember {
    private String userId;
    private String name;
    private String email;
    private String avatarUrl;
    private boolean isAdmin;

    public GroupMember() {
    }

    public GroupMember(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.isAdmin = false;
    }

    public GroupMember(String userId, String name, String email, String avatarUrl, boolean isAdmin) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.isAdmin = isAdmin;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
