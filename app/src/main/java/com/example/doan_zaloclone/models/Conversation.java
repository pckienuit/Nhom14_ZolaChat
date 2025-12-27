package com.example.doan_zaloclone.models;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    public static final String TYPE_FRIEND = "FRIEND";
    public static final String TYPE_GROUP = "GROUP";

    private String id;
    private String name;
    private String lastMessage;
    private long timestamp;
    private List<String> memberIds;
    private java.util.Map<String, String> memberNames;

    private String type;
    private List<String> adminIds;
    private String avatarUrl;

    // Pinned messages - list of message IDs (unlimited)
    private List<String> pinnedMessageIds;

    // Pinned conversations - map of userId to pinnedAt timestamp
    private java.util.Map<String, Long> pinnedByUsers;

    // Tags - map of userId to list of tags
    private java.util.Map<String, java.util.List<String>> userTags;

    // Displayed tag - map of userId to displayed tag (null means show latest, "ALL" means show all)
    private java.util.Map<String, String> displayedTags;

    // Unread message counts - map of userId to unread count
    // Using Object instead of Integer because Gson may deserialize as Double or Long
    private java.util.Map<String, Object> unreadCounts;

    public Conversation() {
        this.memberIds = new ArrayList<>();
        this.type = TYPE_FRIEND;
        this.pinnedMessageIds = new ArrayList<>();
        this.pinnedByUsers = new java.util.HashMap<>();
        this.userTags = new java.util.HashMap<>();
    }

    public Conversation(String id, String name, String lastMessage, long timestamp) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = new ArrayList<>();
        this.type = TYPE_FRIEND;
        this.pinnedMessageIds = new ArrayList<>();
        this.pinnedByUsers = new java.util.HashMap<>();
        this.userTags = new java.util.HashMap<>();
    }

    public Conversation(String id, String name, String lastMessage, long timestamp, List<String> memberIds) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
        this.type = TYPE_FRIEND;
        this.pinnedMessageIds = new ArrayList<>();
        this.pinnedByUsers = new java.util.HashMap<>();
        this.userTags = new java.util.HashMap<>();
    }

    public Conversation(String id, String name, String lastMessage, long timestamp,
                        List<String> memberIds, String type, String adminId, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
        this.type = type != null ? type : TYPE_FRIEND;
        // Backward compatibility: convert single adminId to list
        this.adminIds = new ArrayList<>();
        if (adminId != null && !adminId.isEmpty()) {
            this.adminIds.add(adminId);
        }
        this.avatarUrl = avatarUrl;
        this.pinnedMessageIds = new ArrayList<>();
        this.pinnedByUsers = new java.util.HashMap<>();
        this.userTags = new java.util.HashMap<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAdminIds() {
        return adminIds != null ? adminIds : new ArrayList<>();
    }

    public void setAdminIds(List<String> adminIds) {
        this.adminIds = adminIds;
    }

    // Backward compatibility: get first admin
    public String getAdminId() {
        if (adminIds != null && !adminIds.isEmpty()) {
            return adminIds.get(0);
        }
        return null;
    }

    // Backward compatibility: set single admin as list
    public void setAdminId(String adminId) {
        this.adminIds = new ArrayList<>();
        if (adminId != null && !adminId.isEmpty()) {
            this.adminIds.add(adminId);
        }
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<String> getPinnedMessageIds() {
        return pinnedMessageIds != null ? pinnedMessageIds : new ArrayList<>();
    }

    public void setPinnedMessageIds(List<String> pinnedMessageIds) {
        this.pinnedMessageIds = pinnedMessageIds;
    }

    public void addMember(String userId) {
        if (this.memberIds == null) {
            this.memberIds = new ArrayList<>();
        }
        if (!this.memberIds.contains(userId)) {
            this.memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        if (this.memberIds != null) {
            this.memberIds.remove(userId);
        }
    }

    public boolean hasMember(String userId) {
        return this.memberIds != null && this.memberIds.contains(userId);
    }

    public java.util.Map<String, String> getMemberNames() {
        return memberNames;
    }

    public void setMemberNames(java.util.Map<String, String> memberNames) {
        this.memberNames = memberNames;
    }

    public boolean isGroupChat() {
        return TYPE_GROUP.equals(type);
    }

    public boolean isFriendChat() {
        return TYPE_FRIEND.equals(type);
    }

    public boolean isAdmin(String userId) {
        return userId != null && adminIds != null && adminIds.contains(userId);
    }

    public void addAdmin(String userId) {
        if (this.adminIds == null) {
            this.adminIds = new ArrayList<>();
        }
        if (userId != null && !userId.isEmpty() && !this.adminIds.contains(userId)) {
            this.adminIds.add(userId);
        }
    }

    public void removeAdmin(String userId) {
        if (this.adminIds != null && userId != null) {
            this.adminIds.remove(userId);
        }
    }

    public int getAdminCount() {
        return adminIds != null ? adminIds.size() : 0;
    }

    public boolean isLastAdmin(String userId) {
        return isAdmin(userId) && getAdminCount() == 1;
    }

    public String getOtherUserName(String currentUserId) {
        android.util.Log.d("Conversation", "getOtherUserName called - currentUserId: " + currentUserId +
                ", memberNames: " + memberNames +
                ", memberIds: " + memberIds);

        if (memberNames != null && memberIds != null) {
            for (String memberId : memberIds) {
                android.util.Log.d("Conversation", "Checking memberId: " + memberId +
                        ", equals currentUserId: " + memberId.equals(currentUserId));
                if (!memberId.equals(currentUserId)) {
                    String name = memberNames.get(memberId);
                    android.util.Log.d("Conversation", "Found other user - memberId: " + memberId + ", name: " + name);
                    return name != null ? name : "";
                }
            }
        }
        android.util.Log.d("Conversation", "No other user found, returning empty");
        return "";
    }

    // Pinned messages helper methods

    /**
     * Add a message to pinned messages list
     *
     * @param messageId ID of message to pin
     */
    public void addPinnedMessage(String messageId) {
        if (this.pinnedMessageIds == null) {
            this.pinnedMessageIds = new ArrayList<>();
        }
        if (!this.pinnedMessageIds.contains(messageId)) {
            this.pinnedMessageIds.add(messageId);
        }
    }

    /**
     * Remove a message from pinned messages list
     *
     * @param messageId ID of message to unpin
     */
    public void removePinnedMessage(String messageId) {
        if (this.pinnedMessageIds != null) {
            this.pinnedMessageIds.remove(messageId);
        }
    }

    /**
     * Check if a message is pinned
     *
     * @param messageId ID of message to check
     * @return true if message is pinned
     */
    public boolean isPinned(String messageId) {
        return this.pinnedMessageIds != null && this.pinnedMessageIds.contains(messageId);
    }

    /**
     * Get count of pinned messages
     *
     * @return number of pinned messages
     */
    public int getPinnedMessageCount() {
        return this.pinnedMessageIds != null ? this.pinnedMessageIds.size() : 0;
    }

    // Pinned conversation helper methods

    /**
     * Get pinned by users map
     *
     * @return map of userId to pinnedAt timestamp
     */
    public java.util.Map<String, Long> getPinnedByUsers() {
        return pinnedByUsers != null ? pinnedByUsers : new java.util.HashMap<>();
    }

    /**
     * Set pinned by users map
     *
     * @param pinnedByUsers map of userId to pinnedAt timestamp
     */
    public void setPinnedByUsers(java.util.Map<String, Long> pinnedByUsers) {
        this.pinnedByUsers = pinnedByUsers;
    }

    /**
     * Check if conversation is pinned by a user
     *
     * @param userId ID of user to check
     * @return true if pinned by this user
     */
    public boolean isPinnedByUser(String userId) {
        return this.pinnedByUsers != null && this.pinnedByUsers.containsKey(userId);
    }

    /**
     * Get pinned timestamp for a user
     *
     * @param userId ID of user
     * @return timestamp when pinned, or 0 if not pinned
     */
    public long getPinnedAtTimestamp(String userId) {
        if (this.pinnedByUsers != null && this.pinnedByUsers.containsKey(userId)) {
            return this.pinnedByUsers.get(userId);
        }
        return 0;
    }

    /**
     * Pin conversation for a user
     *
     * @param userId ID of user pinning the conversation
     */
    public void pinForUser(String userId) {
        if (this.pinnedByUsers == null) {
            this.pinnedByUsers = new java.util.HashMap<>();
        }
        this.pinnedByUsers.put(userId, System.currentTimeMillis());
    }

    /**
     * Unpin conversation for a user
     *
     * @param userId ID of user unpinning the conversation
     */
    public void unpinForUser(String userId) {
        if (this.pinnedByUsers != null) {
            this.pinnedByUsers.remove(userId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return timestamp == that.timestamp &&
                java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(name, that.name) &&
                java.util.Objects.equals(lastMessage, that.lastMessage) &&
                java.util.Objects.equals(memberIds, that.memberIds) &&
                java.util.Objects.equals(memberNames, that.memberNames) &&
                java.util.Objects.equals(type, that.type) &&
                java.util.Objects.equals(adminIds, that.adminIds) &&
                java.util.Objects.equals(avatarUrl, that.avatarUrl);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, lastMessage, timestamp, memberIds, memberNames,
                type, adminIds, avatarUrl);
    }

    // Tag management helper methods

    /**
     * Get user tags map
     *
     * @return map of userId to list of tags
     */
    public java.util.Map<String, java.util.List<String>> getUserTags() {
        return userTags != null ? userTags : new java.util.HashMap<>();
    }

    /**
     * Set user tags map
     *
     * @param userTags map of userId to list of tags
     */
    public void setUserTags(java.util.Map<String, java.util.List<String>> userTags) {
        this.userTags = userTags;
    }

    /**
     * Get tags for a specific user
     *
     * @param userId ID of user
     * @return list of tags for this user (empty if none)
     */
    public java.util.List<String> getUserTagsForUser(String userId) {
        if (this.userTags == null || !this.userTags.containsKey(userId)) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(this.userTags.get(userId));
    }

    /**
     * Add a tag for a user
     *
     * @param userId ID of user
     * @param tag    Tag to add
     */
    public void addTag(String userId, String tag) {
        if (this.userTags == null) {
            this.userTags = new java.util.HashMap<>();
        }
        if (!this.userTags.containsKey(userId)) {
            this.userTags.put(userId, new java.util.ArrayList<>());
        }
        if (!this.userTags.get(userId).contains(tag)) {
            this.userTags.get(userId).add(tag);
        }
    }

    /**
     * Remove a tag for a user
     *
     * @param userId ID of user
     * @param tag    Tag to remove
     */
    public void removeTag(String userId, String tag) {
        if (this.userTags != null && this.userTags.containsKey(userId)) {
            this.userTags.get(userId).remove(tag);
            // Clean up empty lists
            if (this.userTags.get(userId).isEmpty()) {
                this.userTags.remove(userId);
            }
        }
    }

    /**
     * Check if user has a specific tag
     *
     * @param userId ID of user
     * @param tag    Tag to check
     * @return true if user has this tag
     */
    public boolean hasTag(String userId, String tag) {
        return this.userTags != null &&
                this.userTags.containsKey(userId) &&
                this.userTags.get(userId).contains(tag);
    }

    // Displayed tag management

    /**
     * Get displayedTags map
     *
     * @return map of userId to displayed tag
     */
    public java.util.Map<String, String> getDisplayedTags() {
        return displayedTags != null ? displayedTags : new java.util.HashMap<>();
    }

    /**
     * Set displayedTags map
     *
     * @param displayedTags map of userId to displayed tag
     */
    public void setDisplayedTags(java.util.Map<String, String> displayedTags) {
        this.displayedTags = displayedTags;
    }

    /**
     * Get which tags should be displayed for a user
     *
     * @param userId ID of user
     * @return List of tags to display (latest tag, specific tag, or all tags)
     */
    public java.util.List<String> getDisplayedTagsForUser(String userId) {
        java.util.List<String> allTags = getUserTagsForUser(userId);
        if (allTags == null || allTags.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // Get display preference
        String displayPref = (displayedTags != null && displayedTags.containsKey(userId))
                ? displayedTags.get(userId)
                : null;

        if ("ALL".equals(displayPref)) {
            // Show all tags
            return allTags;
        } else if (displayPref != null && allTags.contains(displayPref)) {
            // Show specific tag
            java.util.List<String> result = new java.util.ArrayList<>();
            result.add(displayPref);
            return result;
        } else {
            // Default: show latest tag only
            java.util.List<String> result = new java.util.ArrayList<>();
            result.add(allTags.get(allTags.size() - 1));
            return result;
        }
    }

    /**
     * Set which tag should be displayed for a user
     *
     * @param userId ID of user
     * @param tag    Tag to display (null for latest, "ALL" for all tags, or specific tag name)
     */
    public void setDisplayedTag(String userId, String tag) {
        if (this.displayedTags == null) {
            this.displayedTags = new java.util.HashMap<>();
        }
        if (tag == null) {
            this.displayedTags.remove(userId);
        } else {
            this.displayedTags.put(userId, tag);
        }
    }

    // Unread counts management

    /**
     * Get unread counts map
     *
     * @return map of userId to unread count
     */
    public java.util.Map<String, Object> getUnreadCounts() {
        return unreadCounts != null ? unreadCounts : new java.util.HashMap<>();
    }

    /**
     * Set unread counts map
     *
     * @param unreadCounts map of userId to unread count
     */
    public void setUnreadCounts(java.util.Map<String, Object> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }

    /**
     * Get unread count for a specific user
     *
     * @param userId ID of user
     * @return unread count for this user (0 if none)
     */
    public int getUnreadCountForUser(String userId) {
        if (this.unreadCounts == null || !this.unreadCounts.containsKey(userId)) {
            return 0;
        }
        Object countObj = this.unreadCounts.get(userId);
        if (countObj == null) {
            return 0;
        }
        // Handle Gson deserializing numbers as Double or Long
        if (countObj instanceof Number) {
            return ((Number) countObj).intValue();
        }
        return 0;
    }

    /**
     * Reset unread count to 0 for a user (mark as read)
     *
     * @param userId ID of user
     */
    public void markAsReadForUser(String userId) {
        if (this.unreadCounts == null) {
            this.unreadCounts = new java.util.HashMap<>();
        }
        this.unreadCounts.put(userId, 0);
    }
}
