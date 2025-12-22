package com.example.doan_zaloclone.models;

public class Post {
    private String userName;
    private String content;
    private String timestamp;
    private int userAvatarRes; // ID ảnh avatar
    private int postImageRes;  // ID ảnh bài đăng (nếu có)

    public Post(String userName, String content, String timestamp, int userAvatarRes, int postImageRes) {
        this.userName = userName;
        this.content = content;
        this.timestamp = timestamp;
        this.userAvatarRes = userAvatarRes;
        this.postImageRes = postImageRes;
    }

    // Getters
    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getUserAvatarRes() {
        return userAvatarRes;
    }

    public int getPostImageRes() {
        return postImageRes;
    }
}