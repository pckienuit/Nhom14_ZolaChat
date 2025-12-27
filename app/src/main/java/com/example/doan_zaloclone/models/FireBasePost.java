package com.example.doan_zaloclone.models;

import java.util.ArrayList;
import java.util.List;

public class FireBasePost {
    private String postId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private String imageUrl;
    private long timestamp;
    private int likeCount;
    private List<String> likes; // Danh sách userId đã like

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
        this.likes = new ArrayList<>();
    }

    // Getter và Setter
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }
    public int getLikeCount() { return likeCount; }
    
    public List<String> getLikes() {
        if (likes == null) likes = new ArrayList<>();
        return likes;
    }
    
    public void setLikes(List<String> likes) {
        this.likes = likes;
        this.likeCount = (likes != null) ? likes.size() : 0;
    }
    
    // Helper để kiểm tra user đã like chưa
    public boolean isLikedBy(String userId) {
        return getLikes().contains(userId);
    }
}
