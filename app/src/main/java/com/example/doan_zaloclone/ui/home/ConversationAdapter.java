package com.example.doan_zaloclone.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.ConversationTag;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    // Static SimpleDateFormat to avoid recreation in bind()
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
            
    private static boolean isSameDay(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    // Cache for custom tag colors from Firestore
    private static final java.util.Map<String, Integer> customTagColorsCache = new java.util.HashMap<>();
    private final OnConversationClickListener listener;
    private final String currentUserId;
    private List<Conversation> conversations;
    private OnConversationLongClickListener longClickListener;

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener, String currentUserId) {
        this.conversations = conversations;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    /**
     * Load custom tag colors from Firestore for current user
     */
    public static void loadCustomTagColors(String userId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("customTagColors")) {
                        java.util.Map<String, Object> colorsMap =
                                (java.util.Map<String, Object>) documentSnapshot.get("customTagColors");
                        if (colorsMap != null) {
                            customTagColorsCache.clear();
                            for (java.util.Map.Entry<String, Object> entry : colorsMap.entrySet()) {
                                if (entry.getValue() instanceof Long) {
                                    customTagColorsCache.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                                } else if (entry.getValue() instanceof Integer) {
                                    customTagColorsCache.put(entry.getKey(), (Integer) entry.getValue());
                                }
                            }
                        }
                    }
                });
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

    /**
     * Remove conversation by ID (e.g., when user leaves a group)
     */
    public void removeConversationById(String conversationId) {
        int position = -1;
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getId().equals(conversationId)) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            conversations.remove(position);
            notifyItemRemoved(position);
        }
    }

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public interface OnConversationLongClickListener {
        void onConversationLongClick(Conversation conversation);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView lastMessageTextView;
        private final TextView memberCountTextView;
        private final TextView timestampTextView;
        private final TextView badgeTextView;
        private final android.widget.ImageView groupIconImageView;
        private final android.widget.ImageView pinIndicator;
        private final android.widget.ImageView muteIndicator;
        private final LinearLayout tagsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            memberCountTextView = itemView.findViewById(R.id.memberCountTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            badgeTextView = itemView.findViewById(R.id.badgeTextView);
            groupIconImageView = itemView.findViewById(R.id.groupIconImageView);
            pinIndicator = itemView.findViewById(R.id.pinIndicator);
            muteIndicator = itemView.findViewById(R.id.muteIndicator);
            tagsContainer = itemView.findViewById(R.id.tagsContainer);
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

            // --- Format Timestamp ---
            if (conversation.getTimestamp() > 0) {
                long now = System.currentTimeMillis();
                long diff = now - conversation.getTimestamp();
                long oneDay = 24 * 60 * 60 * 1000;
                
                if (diff < oneDay && isSameDay(now, conversation.getTimestamp())) {
                    // Today: show HH:mm or "User: ..." might optionally show time differently? No, just HH:mm
                    // Actually, for very recent, Zalo shows "X phút" (minutes) or "Vừa xong"
                    if (diff < 60 * 60 * 1000) {
                         long minutes = diff / (60 * 1000);
                         if (minutes <= 0) timestampTextView.setText("Vừa xong");
                         else timestampTextView.setText(minutes + " phút");
                    } else if (diff < 12 * 60 * 60 * 1000) {
                         long hours = diff / (60 * 60 * 1000);
                         timestampTextView.setText(hours + " giờ");
                    } else {
                         timestampTextView.setText(TIMESTAMP_FORMAT.format(new java.util.Date(conversation.getTimestamp())));
                    }
                } else {
                     // Older: show Date or Day
                     // For now simple date format
                     java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM", Locale.getDefault());
                     timestampTextView.setText(sdfDate.format(new java.util.Date(conversation.getTimestamp())));
                }
            } else {
                timestampTextView.setText("");
            }

            // --- Pin Indicator ---
            if (pinIndicator != null) {
                boolean isPinned = conversation.isPinnedByUser(currentUserId);
                pinIndicator.setVisibility(isPinned ? View.VISIBLE : View.GONE);
            }
            
            // --- Mute Indicator ---
            // For now, hide it as we don't have isMuted logic fully visible here
            if (muteIndicator != null) muteIndicator.setVisibility(View.GONE);

            // --- Unread Badge ---
            int unreadCount = 0; 
            // Mock logic since getUnreadCounts() is missing in Conversation model
            // If we want to test UI, we could force a value, but for now let's keep it 0 (hidden)
            // or maybe show random for demo? No, better safe.
            // TODO: Implement unread count in Conversation model
            
            if (unreadCount > 0) {
                badgeTextView.setVisibility(View.VISIBLE);
                if (unreadCount > 5) {
                    badgeTextView.setText("5+");
                    badgeTextView.setBackgroundResource(R.drawable.bg_red_badge_pill);
                } else {
                    badgeTextView.setText(String.valueOf(unreadCount));
                    badgeTextView.setBackgroundResource(R.drawable.bg_red_badge); // Circle for single digits usually
                }
            } else {
                badgeTextView.setVisibility(View.GONE);
            }

            // Render tags
            renderTags(conversation, currentUserId);

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

        /**
         * Render tags as icons in the tags container
         */
        private void renderTags(Conversation conversation, String currentUserId) {
            if (tagsContainer == null) return;

            // Clear existing tags
            tagsContainer.removeAllViews();

            // Get displayed tags (based on user preference: latest, specific, or all)
            java.util.List<String> tags = conversation.getDisplayedTagsForUser(currentUserId);
            if (tags == null || tags.isEmpty()) {
                tagsContainer.setVisibility(View.GONE);
                return;
            }

            tagsContainer.setVisibility(View.VISIBLE);

            // Create icon for each displayed tag
            for (String tag : tags) {
                android.widget.ImageView iconView = createTagIcon(tag);
                tagsContainer.addView(iconView);
            }
        }

        /**
         * Create a tag icon view
         */
        private android.widget.ImageView createTagIcon(String tag) {
            android.widget.ImageView icon = new android.widget.ImageView(itemView.getContext());

            // Set icon drawable
            icon.setImageResource(R.drawable.ic_tag);

            // Set color tint (with Firestore cache)
            int tagColor = getTagColorWithCache(tag);
            icon.setColorFilter(tagColor);

            // Set size
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (16 * itemView.getContext().getResources().getDisplayMetrics().density),
                    (int) (16 * itemView.getContext().getResources().getDisplayMetrics().density)
            );
            params.setMarginEnd((int) (2 * itemView.getContext().getResources().getDisplayMetrics().density));
            icon.setLayoutParams(params);

            return icon;
        }

        /**
         * Get tag color with Firestore cache fallback
         */
        private int getTagColorWithCache(String tag) {
            // Check if it's a custom tag in cache
            if (customTagColorsCache.containsKey(tag)) {
                return customTagColorsCache.get(tag);
            }

            // Otherwise use ConversationTag's logic
            return ConversationTag.getTagColor(itemView.getContext(), tag);
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

            // Compare userTags maps for real-time tag updates
            boolean tagsEquals = false;
            if (oldConv.getUserTags() == null && newConv.getUserTags() == null) {
                tagsEquals = true;
            } else if (oldConv.getUserTags() != null && newConv.getUserTags() != null) {
                tagsEquals = oldConv.getUserTags().equals(newConv.getUserTags());
            }

            // Compare displayedTags maps for real-time display preference updates
            boolean displayedTagsEquals = false;
            if (oldConv.getDisplayedTags() == null && newConv.getDisplayedTags() == null) {
                displayedTagsEquals = true;
            } else if (oldConv.getDisplayedTags() != null && newConv.getDisplayedTags() != null) {
                displayedTagsEquals = oldConv.getDisplayedTags().equals(newConv.getDisplayedTags());
            }

            return nameEquals && lastMessageEquals &&
                    oldConv.getTimestamp() == newConv.getTimestamp() &&
                    pinnedEquals &&
                    tagsEquals &&
                    displayedTagsEquals;
        }

    }
}
