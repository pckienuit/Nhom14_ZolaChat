package com.example.doan_zaloclone.api.models;

import com.example.doan_zaloclone.models.Message;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MessageListResponse {
    @SerializedName("messages")
    private List<Message> messages;
    
    @SerializedName("count")
    private int count;
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public int getCount() {
        return count;
    }
}
