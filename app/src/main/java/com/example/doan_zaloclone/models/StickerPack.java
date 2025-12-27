package com.example.doan_zaloclone.models;

import java.util.ArrayList;
import java.util.List;

/**
 * StickerPack model class
 * Represents a collection of stickers
 */
public class StickerPack {
    // Pack types - must match admin-web values (lowercase)
    public static final String TYPE_OFFICIAL = "official";
    public static final String TYPE_USER = "user";
    public static final String TYPE_TRENDING = "trending";

    private String id;
    private String name;
    private String description;
    private String iconUrl;         // Pack icon
    private String bannerUrl;       // For store display
    private String creatorId;       // null = official
    private String creatorName;
    private String type;            // OFFICIAL / USER / TRENDING
    private int stickerCount;
    private long downloadCount;
    private long createdAt;
    private long updatedAt;
    private boolean isPublished;    // Visible in store
    private boolean isFree;
    private double price;           // Future: paid stickers
    private List<String> sampleStickerUrls;  // Preview stickers

    // Empty constructor for Firestore
    public StickerPack() {
        this.sampleStickerUrls = new ArrayList<>();
    }

    public StickerPack(String id, String name, String description, String iconUrl,
                       String bannerUrl, String creatorId, String creatorName, String type,
                       int stickerCount, long downloadCount, long createdAt, long updatedAt,
                       boolean isPublished, boolean isFree, double price,
                       List<String> sampleStickerUrls) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.bannerUrl = bannerUrl;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.type = type;
        this.stickerCount = stickerCount;
        this.downloadCount = downloadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublished = isPublished;
        this.isFree = isFree;
        this.price = price;
        this.sampleStickerUrls = sampleStickerUrls != null ? sampleStickerUrls : new ArrayList<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    // Setters (for Firestore)
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStickerCount() {
        return stickerCount;
    }

    public void setStickerCount(int stickerCount) {
        this.stickerCount = stickerCount;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<String> getSampleStickerUrls() {
        return sampleStickerUrls;
    }

    public void setSampleStickerUrls(List<String> sampleStickerUrls) {
        this.sampleStickerUrls = sampleStickerUrls;
    }

    /**
     * Check if this is an official pack
     */
    public boolean isOfficial() {
        return TYPE_OFFICIAL.equalsIgnoreCase(type) || creatorId == null || creatorId.isEmpty();
    }

    /**
     * Increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    /**
     * Increment sticker count
     */
    public void incrementStickerCount() {
        this.stickerCount++;
    }
}
