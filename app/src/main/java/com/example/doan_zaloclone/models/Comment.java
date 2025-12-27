package com.example.doan_zaloclone.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {
    private String id;
    private String postId; // Thêm ID của bài đăng
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Date timestamp; // Sử dụng Date cho @ServerTimestamp

    public Comment() { }

    // Cập nhật constructor để bao gồm postId
    public Comment(String id, String postId, String userId, String userName, String userAvatar, String content) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
    }

    public String getId() { return id; }
    public String getPostId() { return postId; } // Thêm getter cho postId
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getContent() { return content; }

    @ServerTimestamp // Để Firestore tự động điền thời gian
    public Date getTimestamp() { return timestamp; }

    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
