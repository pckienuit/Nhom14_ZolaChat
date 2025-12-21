package com.example.doan_zaloclone.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model representing a poll in a conversation
 */
public class Poll implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String question;                    // Câu hỏi bình chọn
    private List<PollOption> options;           // Danh sách phương án
    private String creatorId;                   // Người tạo poll
    private long createdAt;                     // Thời gian tạo
    private long expiresAt;                     // Thời hạn (0 = không giới hạn)
    private boolean isPinned;                   // Ghim lên đầu trò chuyện
    private boolean isAnonymous;                // Ẩn người bình chọn
    private boolean hideResultsUntilVoted;      // Ẩn kết quả khi chưa bình chọn
    private boolean allowMultipleChoice;        // Chọn nhiều phương án
    private boolean allowAddOptions;            // Có thể thêm phương án
    private boolean isClosed;                   // Poll đã đóng
    
    // Empty constructor for Firestore
    public Poll() {
        this.options = new ArrayList<>();
    }
    
    public Poll(String id, String question, String creatorId) {
        this.id = id;
        this.question = question;
        this.creatorId = creatorId;
        this.options = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = 0;
        this.isPinned = false;
        this.isAnonymous = false;
        this.hideResultsUntilVoted = false;
        this.allowMultipleChoice = false;
        this.allowAddOptions = false;
        this.isClosed = false;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public List<PollOption> getOptions() {
        return options != null ? options : new ArrayList<>();
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public boolean isPinned() {
        return isPinned;
    }
    
    public boolean isAnonymous() {
        return isAnonymous;
    }
    
    public boolean isHideResultsUntilVoted() {
        return hideResultsUntilVoted;
    }
    
    public boolean isAllowMultipleChoice() {
        return allowMultipleChoice;
    }
    
    public boolean isAllowAddOptions() {
        return allowAddOptions;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
    // Firestore compatibility getters (boolean fields)
    public boolean getIsPinned() {
        return isPinned;
    }
    
    public boolean getIsAnonymous() {
        return isAnonymous;
    }
    
    public boolean getHideResultsUntilVoted() {
        return hideResultsUntilVoted;
    }
    
    public boolean getAllowMultipleChoice() {
        return allowMultipleChoice;
    }
    
    public boolean getAllowAddOptions() {
        return allowAddOptions;
    }
    
    public boolean getIsClosed() {
        return isClosed;
    }
    
    // Setters (for Firestore)
    public void setId(String id) {
        this.id = id;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public void setOptions(List<PollOption> options) {
        this.options = options;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }
    
    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }
    
    public void setHideResultsUntilVoted(boolean hideResultsUntilVoted) {
        this.hideResultsUntilVoted = hideResultsUntilVoted;
    }
    
    public void setAllowMultipleChoice(boolean allowMultipleChoice) {
        this.allowMultipleChoice = allowMultipleChoice;
    }
    
    public void setAllowAddOptions(boolean allowAddOptions) {
        this.allowAddOptions = allowAddOptions;
    }
    
    public void setClosed(boolean closed) {
        isClosed = closed;
    }
    
    // Firestore compatibility setters
    public void setIsPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }
    
    public void setIsAnonymous(boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }
    
    public void setIsClosed(boolean isClosed) {
        this.isClosed = isClosed;
    }
    
    // Helper methods
    
    /**
     * Check if poll has expired
     */
    public boolean hasExpired() {
        if (expiresAt == 0) return false; // No expiry
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Check if user can vote (poll not closed and not expired)
     */
    public boolean canVote() {
        return !isClosed && !hasExpired();
    }
    
    /**
     * Check if a user can close this poll
     * @param userId User ID to check
     * @param isGroupAdmin Whether the user is a group admin
     */
    public boolean canClosePoll(String userId, boolean isGroupAdmin) {
        return userId.equals(creatorId) || isGroupAdmin;
    }
    
    /**
     * Get total number of unique voters
     */
    public int getTotalVoters() {
        Set<String> uniqueVoters = new HashSet<>();
        if (options != null) {
            for (PollOption option : options) {
                uniqueVoters.addAll(option.getVoterIds());
            }
        }
        return uniqueVoters.size();
    }
    
    /**
     * Get total number of votes (can be > voters if multiple choice)
     */
    public int getTotalVotes() {
        int total = 0;
        if (options != null) {
            for (PollOption option : options) {
                total += option.getVoteCount();
            }
        }
        return total;
    }
    
    /**
     * Check if a user has voted
     */
    public boolean hasUserVoted(String userId) {
        if (options == null) return false;
        for (PollOption option : options) {
            if (option.hasUserVoted(userId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get list of option indices that user has voted for
     */
    public List<Integer> getUserVotes(String userId) {
        List<Integer> votes = new ArrayList<>();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).hasUserVoted(userId)) {
                    votes.add(i);
                }
            }
        }
        return votes;
    }
    
    /**
     * Add an option to the poll
     */
    public void addOption(PollOption option) {
        if (options == null) {
            options = new ArrayList<>();
        }
        options.add(option);
    }
    
    /**
     * Get option by ID
     */
    public PollOption getOptionById(String optionId) {
        if (options == null) return null;
        for (PollOption option : options) {
            if (option.getId().equals(optionId)) {
                return option;
            }
        }
        return null;
    }
    
    /**
     * Get option by index
     */
    public PollOption getOptionByIndex(int index) {
        if (options == null || index < 0 || index >= options.size()) {
            return null;
        }
        return options.get(index);
    }
    
    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTime() {
        if (expiresAt == 0) return Long.MAX_VALUE; // No expiry
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Get formatted remaining time string
     */
    public String getFormattedRemainingTime() {
        if (expiresAt == 0) return "Không giới hạn";
        if (hasExpired()) return "Đã hết hạn";
        
        long remaining = getRemainingTime();
        long hours = remaining / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 24) {
            long days = hours / 24;
            return "Còn " + days + " ngày";
        } else if (hours > 0) {
            return "Còn " + hours + " giờ";
        } else {
            return "Còn " + minutes + " phút";
        }
    }
}
