package com.example.doan_zaloclone.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a single option in a poll
 */
public class PollOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String text;                        // Nội dung phương án
    private List<String> voterIds;              // Danh sách user đã vote
    private String addedByUserId;               // User thêm option (nếu allowAddOptions)
    
    // Empty constructor for Firestore
    public PollOption() {
        this.voterIds = new ArrayList<>();
    }
    
    public PollOption(String id, String text) {
        this.id = id;
        this.text = text;
        this.voterIds = new ArrayList<>();
        this.addedByUserId = null;
    }
    
    public PollOption(String id, String text, String addedByUserId) {
        this.id = id;
        this.text = text;
        this.voterIds = new ArrayList<>();
        this.addedByUserId = addedByUserId;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getText() {
        return text;
    }
    
    public List<String> getVoterIds() {
        return voterIds != null ? voterIds : new ArrayList<>();
    }
    
    public String getAddedByUserId() {
        return addedByUserId;
    }
    
    // Setters (for Firestore)
    public void setId(String id) {
        this.id = id;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public void setVoterIds(List<String> voterIds) {
        this.voterIds = voterIds;
    }
    
    public void setAddedByUserId(String addedByUserId) {
        this.addedByUserId = addedByUserId;
    }
    
    // Helper methods
    
    /**
     * Get total number of votes for this option
     */
    public int getVoteCount() {
        return voterIds != null ? voterIds.size() : 0;
    }
    
    /**
     * Check if a user has voted for this option
     */
    public boolean hasUserVoted(String userId) {
        return voterIds != null && voterIds.contains(userId);
    }
    
    /**
     * Add a vote from a user
     */
    public void addVote(String userId) {
        if (voterIds == null) {
            voterIds = new ArrayList<>();
        }
        if (!voterIds.contains(userId)) {
            voterIds.add(userId);
        }
    }
    
    /**
     * Remove a vote from a user
     */
    public void removeVote(String userId) {
        if (voterIds != null) {
            voterIds.remove(userId);
        }
    }
    
    /**
     * Check if this option was added by a user (not original creator)
     */
    public boolean isUserAdded() {
        return addedByUserId != null && !addedByUserId.isEmpty();
    }
}
