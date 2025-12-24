package com.example.doan_zaloclone.models;

import java.util.Map;

public class Message {
    // Message types
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_CALL = "CALL";
    public static final String TYPE_POLL = "POLL";
    public static final String TYPE_CONTACT = "CONTACT"; // Business card message
    public static final String TYPE_LOCATION = "LOCATION"; // Location message
    public static final String TYPE_LIVE_LOCATION = "LIVE_LOCATION"; // Live location message
    public static final String TYPE_STICKER = "STICKER"; // Sticker message

    private String id;
    private String senderId;
    private String senderName;      // Sender's display name (cached for performance)
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
    private String originalSenderName;  // Original sender name if forwarded
    
    // Reaction field - Map of userId to reaction type (heart, haha, sad, angry, wow, like)
    private Map<String, String> reactions;
    
    // Reaction counts - Map of reaction type to total count (allows multiple clicks per user)
    // Format: {"heart": 5, "haha": 3} - tracks total clicks, not just unique users
    private Map<String, Integer> reactionCounts;
    
    // Poll data (only used for TYPE_POLL messages)
    private Poll pollData;
    
    // Contact data (only used for TYPE_CONTACT messages)
    private String contactUserId; // User ID of the contact being shared
    
    // Location data (only used for TYPE_LOCATION messages)
    private double latitude;           // GPS latitude
    private double longitude;          // GPS longitude
    private String locationName;       // Name of the location (optional)
    private String locationAddress;    // Full address of the location (optional)
    
    // Live Location data (only used for TYPE_LIVE_LOCATION messages)
    private String liveLocationSessionId; // Session ID for live location tracking
    
    // Sticker data (only used for TYPE_STICKER messages)
    private String stickerId;           // Sticker ID
    private String stickerPackId;       // Sticker pack ID
    private String stickerUrl;          // Direct URL for display
    private boolean isStickerAnimated;  // true if animated sticker

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

    public String getSenderName() {
        return senderName;
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
    
    // Alias for Firestore deserialization (maps 'isRecalled' field to this getter)
    public boolean getIsRecalled() {
        return isRecalled;
    }
    
    public boolean isForwarded() {
        return isForwarded;
    }
    
    // Alias for Firestore deserialization (maps 'isForwarded' field to this getter)
    public boolean getIsForwarded() {
        return isForwarded;
    }
    
    public String getOriginalSenderId() {
        return originalSenderId;
    }
    
    public String getOriginalSenderName() {
        return originalSenderName;
    }
    
    public Map<String, String> getReactions() {
        return reactions;
    }

    // Setters (cần cho Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
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
    
    // Alias for Firestore deserialization
    public void setIsRecalled(boolean isRecalled) {
        this.isRecalled = isRecalled;
    }
    
    public void setForwarded(boolean forwarded) {
        isForwarded = forwarded;
    }
    
    // Alias for Firestore deserialization
    public void setIsForwarded(boolean isForwarded) {
        this.isForwarded = isForwarded;
    }
    
    public void setOriginalSenderId(String originalSenderId) {
        this.originalSenderId = originalSenderId;
    }
    
    public void setOriginalSenderName(String originalSenderName) {
        this.originalSenderName = originalSenderName;
    }
    
    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions;
    }
    
    public Map<String, Integer> getReactionCounts() {
        return reactionCounts;
    }
    
    public void setReactionCounts(Map<String, Integer> reactionCounts) {
        this.reactionCounts = reactionCounts;
    }
    
    public Poll getPollData() {
        return pollData;
    }
    
    public void setPollData(Poll pollData) {
        this.pollData = pollData;
    }
    
    public String getContactUserId() {
        return contactUserId;
    }
    
    public void setContactUserId(String contactUserId) {
        this.contactUserId = contactUserId;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getLocationName() {
        return locationName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public String getLocationAddress() {
        return locationAddress;
    }
    
    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
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
    
    public boolean isPollMessage() {
        return TYPE_POLL.equals(this.type);
    }
    
    public boolean isContactMessage() {
        return TYPE_CONTACT.equals(this.type);
    }
    
    public boolean isLocationMessage() {
        return TYPE_LOCATION.equals(this.type);
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
    
    /**
     * Check if this message has any reactions
     * @return true if message has at least one reaction
     */
    public boolean hasReactions() {
        return reactions != null && !reactions.isEmpty();
    }
    
    /**
     * Get total reaction count
     * @return Number of users who reacted
     */
    public int getReactionCount() {
        return MessageReaction.getReactionCount(reactions);
    }
    
    /**
     * Get formatted reaction count for display
     * @return Formatted string (e.g., "5", "99+")
     */
    public String getFormattedReactionCount() {
        return MessageReaction.getFormattedReactionCount(reactions);
    }
    
    /**
     * Get user's reaction type
     * @param userId User ID to check
     * @return Reaction type or null if user hasn't reacted
     */
    public String getUserReaction(String userId) {
        return MessageReaction.getUserReaction(reactions, userId);
    }
    
    /**
     * Check if a specific user has reacted
     * @param userId User ID to check
     * @return true if user has reacted
     */
    public boolean hasUserReacted(String userId) {
        return MessageReaction.hasUserReacted(reactions, userId);
    }
    
    public String getLiveLocationSessionId() {
        return liveLocationSessionId;
    }
    
    public void setLiveLocationSessionId(String liveLocationSessionId) {
        this.liveLocationSessionId = liveLocationSessionId;
    }
    
    // Sticker getters and setters
    public String getStickerId() {
        return stickerId;
    }
    
    public void setStickerId(String stickerId) {
        this.stickerId = stickerId;
    }
    
    public String getStickerPackId() {
        return stickerPackId;
    }
    
    public void setStickerPackId(String stickerPackId) {
        this.stickerPackId = stickerPackId;
    }
    
    public String getStickerUrl() {
        return stickerUrl;
    }
    
    public void setStickerUrl(String stickerUrl) {
        this.stickerUrl = stickerUrl;
    }
    
    public boolean isStickerAnimated() {
        return isStickerAnimated;
    }
    
    // Alias for Firestore
    public boolean getIsStickerAnimated() {
        return isStickerAnimated;
    }
    
    public void setStickerAnimated(boolean stickerAnimated) {
        isStickerAnimated = stickerAnimated;
    }
    
    // Alias for Firestore
    public void setIsStickerAnimated(boolean isStickerAnimated) {
        this.isStickerAnimated = isStickerAnimated;
    }
    
    public boolean isStickerMessage() {
        return TYPE_STICKER.equals(this.type);
    }
}
