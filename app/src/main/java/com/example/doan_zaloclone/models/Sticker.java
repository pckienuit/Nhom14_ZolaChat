package com.example.doan_zaloclone.models;

/**
 * Sticker model class
 * Represents a single sticker within a sticker pack
 */
public class Sticker {
    private String id;              // Unique sticker ID
    private String packId;          // Parent pack ID
    private String imageUrl;        // URL on VPS (static/animated)
    private String thumbnailUrl;    // Thumbnail for quick loading
    private boolean isAnimated;     // true = WebP/GIF animated
    private int width;
    private int height;
    private String creatorId;       // User ID (null if official)
    private long createdAt;
    private String[] tags;          // For search
    private String format;          // webp, gif, png, apng, jpeg, lottie

    // Empty constructor for Firestore
    public Sticker() {
    }

    public Sticker(String id, String packId, String imageUrl, String thumbnailUrl,
                   boolean isAnimated, int width, int height, String creatorId,
                   long createdAt, String[] tags, String format) {
        this.id = id;
        this.packId = packId;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.isAnimated = isAnimated;
        this.width = width;
        this.height = height;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.tags = tags;
        this.format = format;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getPackId() {
        return packId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean isAnimated() {
        return isAnimated;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String[] getTags() {
        return tags;
    }

    public String getFormat() {
        return format;
    }

    // Setters (for Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setAnimated(boolean animated) {
        isAnimated = animated;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Check if this is an official sticker
     */
    public boolean isOfficial() {
        return creatorId == null || creatorId.isEmpty();
    }

    /**
     * Get display size based on format
     * Lottie scales infinitely, images have fixed size
     */
    public boolean isVectorFormat() {
        return "lottie".equalsIgnoreCase(format);
    }
}
