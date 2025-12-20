package com.example.doan_zaloclone.models;

public class Message {
    // Message types
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_CALL = "CALL";

    private String id;
    private String senderId;
    private String content;
    private String type; // TEXT, IMAGE, hoặc FILE
    private long timestamp;
    
    // File metadata (only used for TYPE_FILE messages)
    private String fileName;
    private long fileSize; // in bytes
    private String fileMimeType;
    
    // Reply fields
    private String replyToId;           // ID of the message being replied to
    private String replyToContent;      // Content preview of the replied message
    private String replyToSenderId;     // Sender ID of the replied message
    private String replyToSenderName;   // Sender name of the replied message
    
    // Recall field
    private boolean isRecalled;         // Message has been recalled
    
    // Forward fields
    private boolean isForwarded;        // Message is forwarded
    private String originalSenderId;    // Original sender ID if forwarded

    // Empty constructor bắt buộc cho Firestore serialization/deserialization
    public Message() {
        this.type = TYPE_TEXT; // Default type
    }

    // Constructor cũ (backward compatible) - mặc định type là TEXT
    public Message(String id, String senderId, String content, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.type = TYPE_TEXT;
    }

    // Constructor đầy đủ
    public Message(String id, String senderId, String content, String type, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.type = type != null ? type : TYPE_TEXT;
        this.timestamp = timestamp;
    }
    
    // Constructor cho file messages
    public Message(String id, String senderId, String content, String type, long timestamp,
                   String fileName, long fileSize, String fileMimeType) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.type = type != null ? type : TYPE_TEXT;
        this.timestamp = timestamp;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileMimeType = fileMimeType;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getFileMimeType() {
        return fileMimeType;
    }
    
    public String getReplyToId() {
        return replyToId;
    }
    
    public String getReplyToContent() {
        return replyToContent;
    }
    
    public String getReplyToSenderId() {
        return replyToSenderId;
    }
    
    public String getReplyToSenderName() {
        return replyToSenderName;
    }
    
    public boolean isRecalled() {
        return isRecalled;
    }
    
    public boolean isForwarded() {
        return isForwarded;
    }
    
    public String getOriginalSenderId() {
        return originalSenderId;
    }

    // Setters (cần cho Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public void setFileMimeType(String fileMimeType) {
        this.fileMimeType = fileMimeType;
    }
    
    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }
    
    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }
    
    public void setReplyToSenderId(String replyToSenderId) {
        this.replyToSenderId = replyToSenderId;
    }
    
    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }
    
    public void setRecalled(boolean recalled) {
        isRecalled = recalled;
    }
    
    public void setForwarded(boolean forwarded) {
        isForwarded = forwarded;
    }
    
    public void setOriginalSenderId(String originalSenderId) {
        this.originalSenderId = originalSenderId;
    }

    // Helper methods
    public boolean isTextMessage() {
        return TYPE_TEXT.equals(this.type);
    }

    public boolean isImageMessage() {
        return TYPE_IMAGE.equals(this.type);
    }
    
    public boolean isFileMessage() {
        return TYPE_FILE.equals(this.type);
    }
    
    public boolean isCallMessage() {
        return TYPE_CALL.equals(this.type);
    }
    
    /**
     * Check if this message is a reply to another message
     * @return true if this is a reply message
     */
    public boolean isReplyMessage() {
        return replyToId != null && !replyToId.isEmpty();
    }
    
    /**
     * Check if this message was forwarded from another user
     * @param currentUserId Current user's ID to compare
     * @return true if forwarded from someone else
     */
    public boolean isForwardedFromOther(String currentUserId) {
        return isForwarded && originalSenderId != null 
                && !originalSenderId.equals(currentUserId);
    }
    
    /**
     * Check if this message can be recalled
     * @param senderId ID of the user checking
     * @return true if message can be recalled (sent by user and within 1 hour)
     */
    public boolean canBeRecalled(String senderId) {
        if (!this.senderId.equals(senderId) || isRecalled) {
            return false;
        }
        long oneHourInMillis = 60 * 60 * 1000; // 1 hour
        return (System.currentTimeMillis() - timestamp) < oneHourInMillis;
    }
    
    /**
     * Format file size to human-readable string
     * @return Formatted string (e.g., "1.5 MB", "256 KB")
     */
    public String getFormattedFileSize() {
        if (fileSize <= 0) return "0 B";
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
