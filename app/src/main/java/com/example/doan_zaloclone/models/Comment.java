package com.example.doan_zaloclone.models;

public class Comment {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private long timestamp;

    public Comment() { }

    public Comment(String id, String userId, String userName, String userAvatar, String content, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
