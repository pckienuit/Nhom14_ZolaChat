package com.example.doan_zaloclone.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<Conversation> conversations;
    private OnConversationClickListener listener;
    private String currentUserId;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener, String currentUserId) {
        this.conversations = conversations;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation, listener, currentUserId);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateConversations(List<Conversation> newConversations) {
        this.conversations = newConversations;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView lastMessageTextView;
        private TextView timestampTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Conversation conversation, OnConversationClickListener listener, String currentUserId) {
            // For 1-on-1 conversations, display the other user's name
            String displayName;
            if (conversation.getName() == null || conversation.getName().isEmpty()) {
                // Try to get from memberNames
                displayName = conversation.getOtherUserName(currentUserId);
                if (displayName.isEmpty()) {
                    displayName = "Đang tải..."; // Fallback if name not available
                }
            } else {
                // Use the conversation name (for group chats)
                displayName = conversation.getName();
            }
            
            nameTextView.setText(displayName);
            lastMessageTextView.setText(conversation.getLastMessage());
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timestampTextView.setText(sdf.format(new Date(conversation.getTimestamp())));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation);
                }
            });
        }
    }
}
