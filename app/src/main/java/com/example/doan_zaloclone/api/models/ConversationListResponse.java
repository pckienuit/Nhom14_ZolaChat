package com.example.doan_zaloclone.api.models;

import com.example.doan_zaloclone.models.Conversation;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConversationListResponse {
    @SerializedName("conversations")
    private List<Conversation> conversations;

    @SerializedName("count")
    private int count;

    public List<Conversation> getConversations() {
        return conversations;
    }

    public int getCount() {
        return count;
    }
}
