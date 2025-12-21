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
    private OnConversationLongClickListener longClickListener;
    private String currentUserId;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }
    
    public interface OnConversationLongClickListener {
        void onConversationLongClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener, String currentUserId) {
        this.conversations = conversations;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }
    
    public void setOnConversationLongClickListener(OnConversationLongClickListener listener) {
        this.longClickListener = listener;
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
        holder.bind(conversation, listener, longClickListener, currentUserId);
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
        private TextView memberCountTextView;
        private android.widget.ImageView groupIconImageView;
        private android.widget.ImageView pinIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            memberCountTextView = itemView.findViewById(R.id.memberCountTextView);
            groupIconImageView = itemView.findViewById(R.id.groupIconImageView);
            pinIndicator = itemView.findViewById(R.id.pinIndicator);
        }

        public void bind(Conversation conversation, OnConversationClickListener listener, 
                        OnConversationLongClickListener longClickListener, String currentUserId) {
            // Check if this is a group chat
            boolean isGroupChat = conversation.isGroupChat();
            
            // Show/hide group indicator
            if (groupIconImageView != null) {
                groupIconImageView.setVisibility(isGroupChat ? View.VISIBLE : View.GONE);
            }
            
            // Show member count for groups
            if (memberCountTextView != null) {
                if (isGroupChat && conversation.getMemberIds() != null) {
                    int memberCount = conversation.getMemberIds().size();
                    memberCountTextView.setText("(" + memberCount + ")");
                    memberCountTextView.setVisibility(View.VISIBLE);
                } else {
                    memberCountTextView.setVisibility(View.GONE);
                }
            }
            
            // Display name logic
            String displayName;
            boolean needsAsyncNameFetch = false;
            
            if (isGroupChat) {
                // For group chats, use the conversation name
                displayName = conversation.getName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = "Nhóm chat";
                }
            } else {
                // For 1-on-1 conversations, display the other user's name
                if (conversation.getName() == null || conversation.getName().isEmpty()) {
                    // Try to get from memberNames
                    displayName = conversation.getOtherUserName(currentUserId);
                    
                    android.util.Log.d("ConversationAdapter", "Conversation " + conversation.getId() + 
                                      " - memberNames: " + (conversation.getMemberNames() != null ? conversation.getMemberNames().size() : "null") +
                                      ", memberIds: " + (conversation.getMemberIds() != null ? conversation.getMemberIds().size() : "null") +
                                      ", displayName: " + displayName);
                    
                    // If name is empty or "User" (placeholder), fetch real name from Firestore
                    if (displayName == null || displayName.isEmpty() || displayName.equals("User")) {
                        // Find other user ID and fetch name
                        if (conversation.getMemberIds() != null && conversation.getMemberIds().size() == 2) {
                            for (String memberId : conversation.getMemberIds()) {
                                if (!memberId.equals(currentUserId)) {
                                    // Set temporary display name
                                    displayName = "Đang tải...";
                                    needsAsyncNameFetch = true;
                                    
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
                        
                        // If still no name and didn't trigger async fetch
                        if (!needsAsyncNameFetch && (displayName == null || displayName.isEmpty())) {
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
                    }
                } else {
                    displayName = conversation.getName();
                }
            }
            
            nameTextView.setText(displayName);
            lastMessageTextView.setText(conversation.getLastMessage());
            
            // Show/hide pin indicator
            if (pinIndicator != null) {
                boolean isPinned = conversation.isPinnedByUser(currentUserId);
                pinIndicator.setVisibility(isPinned ? View.VISIBLE : View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onConversationLongClick(conversation);
                    return true;
                }
                return false;
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
            
            // Compare pinnedByUsers maps for real-time pin indicator updates
            boolean pinnedEquals = false;
            if (oldConv.getPinnedByUsers() == null && newConv.getPinnedByUsers() == null) {
                pinnedEquals = true;
            } else if (oldConv.getPinnedByUsers() != null && newConv.getPinnedByUsers() != null) {
                pinnedEquals = oldConv.getPinnedByUsers().equals(newConv.getPinnedByUsers());
            }
            
            return nameEquals && lastMessageEquals &&
                   oldConv.getTimestamp() == newConv.getTimestamp() &&
                   pinnedEquals;
        }

    }
}
