package com.example.doan_zaloclone.models;

/**
 * PinnedMessage - Data class for displaying pinned messages in UI
 * Contains necessary information to show in pinned messages list
 */
public class PinnedMessage {
    private String messageId;
    private String content;
    private String senderName;
    private String senderId;
    private String type;  // Message.TYPE_TEXT, TYPE_IMAGE, TYPE_FILE, TYPE_CALL
    private long pinnedAt;
    private long originalTimestamp;
    
    // Empty constructor for Firestore
    public PinnedMessage() {
    }
    
    public PinnedMessage(String messageId, String content, String senderName, String senderId, 
                        String type, long pinnedAt, long originalTimestamp) {
        this.messageId = messageId;
        this.content = content;
        this.senderName = senderName;
        this.senderId = senderId;
        this.type = type;
        this.pinnedAt = pinnedAt;
        this.originalTimestamp = originalTimestamp;
    }
    
    // Getters
    public String getMessageId() {
        return messageId;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getType() {
        return type;
    }
    
    public long getPinnedAt() {
        return pinnedAt;
    }
    
    public long getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    // Setters
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setPinnedAt(long pinnedAt) {
        this.pinnedAt = pinnedAt;
    }
    
    public void setOriginalTimestamp(long originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }
    
    /**
     * Get display preview based on message type
     * @return Human-readable preview of the message
     */
    public String getPreview() {
        if (type == null) return content;
        
        switch (type) {
            case Message.TYPE_IMAGE:
                return "📷 Hình ảnh";
            case Message.TYPE_FILE:
                // Extract filename from content (if it's a URL or path)
                String fileName = extractFileName(content);
                return "📎 " + fileName;
            case Message.TYPE_CALL:
                return "📞 Cuộc gọi";
            case Message.TYPE_TEXT:
            default:
                return content != null ? content : "";
        }
    }
    
    private String extractFileName(String content) {
        if (content == null) return "File";
        // If content contains '/', get last part
        int lastSlash = content.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < content.length() - 1) {
            return content.substring(lastSlash + 1);
        }
        return content;
    }
}
