package com.example.doan_zaloclone.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model representing a single option in a poll
 */
public class PollOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String text;                        // Nội dung phương án
    private List<String> voterIds;              // Danh sách user đã vote
    private Map<String, String> voterNames;     // Map userId -> userName cho hiển thị nhanh
    private String addedByUserId;               // User thêm option (nếu allowAddOptions)
    
    // Empty constructor for Firestore
    public PollOption() {
        this.voterIds = new ArrayList<>();
        this.voterNames = new HashMap<>();
    }
    
    public PollOption(String id, String text) {
        this.id = id;
        this.text = text;
        this.voterIds = new ArrayList<>();
        this.voterNames = new HashMap<>();
        this.addedByUserId = null;
    }
    
    public PollOption(String id, String text, String addedByUserId) {
        this.id = id;
        this.text = text;
        this.voterIds = new ArrayList<>();
        this.voterNames = new HashMap<>();
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
    
    public Map<String, String> getVoterNames() {
        return voterNames != null ? voterNames : new HashMap<>();
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
    
    public void setVoterNames(Map<String, String> voterNames) {
        this.voterNames = voterNames;
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
     * Add a vote from a user with their name
     */
    public void addVote(String userId, String userName) {
        if (voterIds == null) {
            voterIds = new ArrayList<>();
        }
        if (voterNames == null) {
            voterNames = new HashMap<>();
        }
        if (!voterIds.contains(userId)) {
            voterIds.add(userId);
            voterNames.put(userId, userName != null ? userName : "User");
        }
    }
    
    /**
     * Add a vote from a user (legacy - without name)
     */
    public void addVote(String userId) {
        addVote(userId, "User");
    }
    
    /**
     * Remove a vote from a user
     */
    public void removeVote(String userId) {
        if (voterIds != null) {
            voterIds.remove(userId);
        }
        if (voterNames != null) {
            voterNames.remove(userId);
        }
    }
    
    /**
     * Get voter name by userId (returns stored name or fallback)
     */
    public String getVoterName(String userId) {
        if (voterNames != null && voterNames.containsKey(userId)) {
            return voterNames.get(userId);
        }
        return "User " + (userId != null ? userId.substring(0, Math.min(8, userId.length())) : "");
    }
    
    /**
     * Get all voter names as a list
     */
    public List<String> getVoterNamesList() {
        List<String> names = new ArrayList<>();
        if (voterIds != null) {
            for (String voterId : voterIds) {
                names.add(getVoterName(voterId));
            }
        }
        return names;
    }
    
    /**
     * Check if this option was added by a user (not original creator)
     */
    public boolean isUserAdded() {
        return addedByUserId != null && !addedByUserId.isEmpty();
    }
}
