package com.example.doan_zaloclone.models;

public class FireBasePost {
    private String postId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private String imageUrl; // Nếu bài viết có ảnh
    private long timestamp;
    private int likeCount;

    // Bắt buộc phải có Constructor rỗng cho Firestore
    public FireBasePost() { }

    public FireBasePost(String postId, String userId, String userName, String userAvatar, String content, String imageUrl, long timestamp) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.likeCount = 0;
    }

    // Getter và Setter (Bắt buộc)
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    // ... tạo hết các getter/setter còn lại ...
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }
    public int getLikeCount() { return likeCount; }
}

