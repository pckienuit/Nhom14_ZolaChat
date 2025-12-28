package com.example.doan_zaloclone.models;

public class VideoItem {
    private String videoId;
    private String title;
    private String thumbnailUrl;

    public VideoItem(String videoId, String title, String thumbnailUrl) {
        this.videoId = videoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
