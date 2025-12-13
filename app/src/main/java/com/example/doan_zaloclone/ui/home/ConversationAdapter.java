package com.example.doan_zaloclone.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    // Static SimpleDateFormat to avoid recreation in bind()
    private static final SimpleDateFormat TIMESTAMP_FORMAT = 
            new SimpleDateFormat("HH:mm", Locale.getDefault());

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
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ConversationDiffCallback(this.conversations, newConversations));
        this.conversations = newConversations;
        diffResult.dispatchUpdatesTo(this);
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
                
                android.util.Log.d("ConversationAdapter", "Conversation " + conversation.getId() + 
                                  " - memberNames: " + (conversation.getMemberNames() != null ? conversation.getMemberNames().size() : "null") +
                                  ", memberIds: " + (conversation.getMemberIds() != null ? conversation.getMemberIds().size() : "null") +
                                  ", displayName: " + displayName);
                
                // If name is "User" (placeholder), fetch real name from Firestore
                if (displayName != null && displayName.equals("User")) {
                    // Find other user ID
                    if (conversation.getMemberIds() != null && conversation.getMemberIds().size() == 2) {
                        for (String memberId : conversation.getMemberIds()) {
                            if (!memberId.equals(currentUserId)) {
                                // Fetch real name from Firestore
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(memberId)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            com.example.doan_zaloclone.models.User user = 
                                                doc.toObject(com.example.doan_zaloclone.models.User.class);
                                            if (user != null && user.getName() != null) {
                                                android.util.Log.d("ConversationAdapter", "Fetched real name: " + user.getName());
                                                nameTextView.setText(user.getName());
                                            }
                                        }
                                    });
                                break;
                            }
                        }
                    }
                }
                
                if (displayName == null || displayName.isEmpty()) {
                    // Fallback: try to get other user ID
                    if (conversation.getMemberIds() != null && conversation.getMemberIds().size() == 2) {
                        for (String memberId : conversation.getMemberIds()) {
                            if (!memberId.equals(currentUserId)) {
                                displayName = "User " + memberId.substring(0, Math.min(8, memberId.length()));
                                break;
                            }
                        }
                    } else {
                        displayName = "Conversation";
                    }
                }
            } else {
                // Use the conversation name (for group chats)
                displayName = conversation.getName();
            }
            
            nameTextView.setText(displayName);
            lastMessageTextView.setText(conversation.getLastMessage());
            
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(conversation.getTimestamp())));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation);
                }
            });
        }
    }
    
    // DiffUtil Callback for efficient list updates
    private static class ConversationDiffCallback extends DiffUtil.Callback {
        private final List<Conversation> oldList;
        private final List<Conversation> newList;
        
        public ConversationDiffCallback(List<Conversation> oldList, List<Conversation> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(
                    newList.get(newItemPosition).getId());
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Conversation oldConv = oldList.get(oldItemPosition);
            Conversation newConv = newList.get(newItemPosition);
            
            // Compare all relevant display fields
            boolean nameEquals = (oldConv.getName() == null && newConv.getName() == null) ||
                                (oldConv.getName() != null && oldConv.getName().equals(newConv.getName()));
            boolean lastMessageEquals = (oldConv.getLastMessage() == null && newConv.getLastMessage() == null) ||
                                       (oldConv.getLastMessage() != null && oldConv.getLastMessage().equals(newConv.getLastMessage()));
            
            return nameEquals && lastMessageEquals &&
                   oldConv.getTimestamp() == newConv.getTimestamp();
        }
    }
}
