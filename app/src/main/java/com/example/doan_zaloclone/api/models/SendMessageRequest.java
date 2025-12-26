package com.example.doan_zaloclone.api.models;

import com.example.doan_zaloclone.models.Message;
import com.google.gson.annotations.SerializedName;

/**
 * Request body for sending a message
 * Maps Message model to API-compatible format
 */
public class SendMessageRequest {
    @SerializedName("content")
    private String content;

    @SerializedName("type")
    private String type;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("senderName")
    private String senderName;

    // File metadata (for file/image messages)
    @SerializedName("fileName")
    private String fileName;

    @SerializedName("fileSize")
    private Long fileSize;

    @SerializedName("fileMimeType")
    private String fileMimeType;

    // Reply fields
    @SerializedName("replyToId")
    private String replyToId;

    @SerializedName("replyToContent")
    private String replyToContent;

    @SerializedName("replyToSenderId")
    private String replyToSenderId;

    @SerializedName("replyToSenderName")
    private String replyToSenderName;

    // Forward fields
    @SerializedName("isForwarded")
    private Boolean isForwarded;

    @SerializedName("originalSenderId")
    private String originalSenderId;

    @SerializedName("originalSenderName")
    private String originalSenderName;

    // Contact (business card)
    @SerializedName("contactUserId")
    private String contactUserId;

    // Location fields
    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("locationName")
    private String locationName;

    @SerializedName("locationAddress")
    private String locationAddress;

    // Live location
    @SerializedName("liveLocationSessionId")
    private String liveLocationSessionId;

    // Sticker fields
    @SerializedName("stickerId")
    private String stickerId;

    @SerializedName("stickerPackId")
    private String stickerPackId;

    @SerializedName("stickerUrl")
    private String stickerUrl;

    @SerializedName("isStickerAnimated")
    private Boolean isStickerAnimated;

    // Empty constructor
    public SendMessageRequest() {
    }

    // Constructor from Message model
    public SendMessageRequest(Message message) {
        this.content = message.getContent();
        this.type = message.getType();
        this.senderId = message.getSenderId();
        this.senderName = message.getSenderName();

        // File metadata
        if (Message.TYPE_FILE.equals(message.getType()) ||
                Message.TYPE_IMAGE.equals(message.getType())) {
            this.fileName = message.getFileName();
            this.fileSize = message.getFileSize() > 0 ? message.getFileSize() : null;
            this.fileMimeType = message.getFileMimeType();
        }

        // Reply fields
        if (message.getReplyToId() != null && !message.getReplyToId().isEmpty()) {
            this.replyToId = message.getReplyToId();
            this.replyToContent = message.getReplyToContent();
            this.replyToSenderId = message.getReplyToSenderId();
            this.replyToSenderName = message.getReplyToSenderName();
        }

        // Forward fields
        if (message.isForwarded()) {
            this.isForwarded = true;
            this.originalSenderId = message.getOriginalSenderId();
            this.originalSenderName = message.getOriginalSenderName();
        }

        // Contact
        if (Message.TYPE_CONTACT.equals(message.getType())) {
            this.contactUserId = message.getContactUserId();
        }

        // Location
        if (Message.TYPE_LOCATION.equals(message.getType())) {
            this.latitude = message.getLatitude();
            this.longitude = message.getLongitude();
            this.locationName = message.getLocationName();
            this.locationAddress = message.getLocationAddress();
        }

        // Live location
        if (Message.TYPE_LIVE_LOCATION.equals(message.getType())) {
            this.liveLocationSessionId = message.getLiveLocationSessionId();
        }

        // Sticker
        if (Message.TYPE_STICKER.equals(message.getType())) {
            this.stickerId = message.getStickerId();
            this.stickerPackId = message.getStickerPackId();
            this.stickerUrl = message.getStickerUrl();
            this.isStickerAnimated = message.isStickerAnimated() ? true : null;
        }

        // Voice
        if (Message.TYPE_VOICE.equals(message.getType())) {
            this.voiceUrl = message.getVoiceUrl();
            this.voiceDuration = message.getVoiceDuration() > 0 ? message.getVoiceDuration() : null;
        }
    }

    @SerializedName("voiceUrl")
    private String voiceUrl;

    @SerializedName("voiceDuration")
    private Integer voiceDuration;

    // Getters and setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
