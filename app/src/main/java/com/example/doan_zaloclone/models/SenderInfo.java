package com.example.doan_zaloclone.models;

/**
 * Model representing a sender with file count for filtering
 */
public class SenderInfo {
    private String senderId;
    private String senderName;
    private String senderAvatarUrl;
    private int fileCount;
    
    public SenderInfo(String senderId, String senderName, String senderAvatarUrl, int fileCount) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatarUrl = senderAvatarUrl;
        this.fileCount = fileCount;
    }
    
    // Getters
    public String getSenderId() {
        return senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }
    
    public int getFileCount() {
        return fileCount;
    }
    
    // Setters
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }
    
    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }
}
