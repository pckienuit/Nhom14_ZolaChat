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
    private String adminId;
    private String avatarUrl;

    public Conversation() {
        this.memberIds = new ArrayList<>();
        this.type = TYPE_FRIEND;
    }

    public Conversation(String id, String name, String lastMessage, long timestamp) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = new ArrayList<>();
        this.type = TYPE_FRIEND;
    }

    public Conversation(String id, String name, String lastMessage, long timestamp, List<String> memberIds) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
        this.type = TYPE_FRIEND;
    }
    
    public Conversation(String id, String name, String lastMessage, long timestamp, 
                       List<String> memberIds, String type, String adminId, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
        this.type = type != null ? type : TYPE_FRIEND;
        this.adminId = adminId;
        this.avatarUrl = avatarUrl;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }
    
    public String getType() {
        return type;
    }
    
    public String getAdminId() {
        return adminId;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
        return userId != null && userId.equals(adminId);
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
               java.util.Objects.equals(adminId, that.adminId) &&
               java.util.Objects.equals(avatarUrl, that.avatarUrl);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, lastMessage, timestamp, memberIds, memberNames, 
                                      type, adminId, avatarUrl);
    }
}
