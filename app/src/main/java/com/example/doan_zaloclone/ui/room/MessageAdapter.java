package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.Intent;
import android.os.CountDownTimer;
import com.example.doan_zaloclone.models.LiveLocation;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.services.LocationSharingService;
import com.example.doan_zaloclone.ui.location.LiveLocationViewActivity;
import com.example.doan_zaloclone.ui.room.RoomActivity; // If needed, or check usages
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    public static final int VIEW_TYPE_IMAGE_SENT = 3;
    public static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
    public static final int VIEW_TYPE_FILE_SENT = 5;
    public static final int VIEW_TYPE_FILE_RECEIVED = 6;
    public static final int VIEW_TYPE_CALL_HISTORY = 7;
    public static final int VIEW_TYPE_RECALLED_SENT = 8;
    public static final int VIEW_TYPE_RECALLED_RECEIVED = 9;
    public static final int VIEW_TYPE_POLL_SENT = 10;
    public static final int VIEW_TYPE_POLL_RECEIVED = 11;
    public static final int VIEW_TYPE_CONTACT_SENT = 12;
    public static final int VIEW_TYPE_CONTACT_RECEIVED = 13;
    public static final int VIEW_TYPE_LOCATION_SENT = 14;
    public static final int VIEW_TYPE_LOCATION_RECEIVED = 15;
    public static final int VIEW_TYPE_LIVE_LOCATION_SENT = 16;
    public static final int VIEW_TYPE_LIVE_LOCATION_RECEIVED = 17;
    
    // Static SimpleDateFormat to avoid recreation in bind()
    private static final SimpleDateFormat TIMESTAMP_FORMAT = 
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private List<Message> messages;
    private String currentUserId;
    private boolean isGroupChat;
    private java.util.Set<String> pinnedMessageIds = new java.util.HashSet<>();
    private String highlightedMessageId = null;
    
    // Listener for pin/unpin actions
    public interface OnMessageLongClickListener {
        void onPinMessage(Message message);
        void onUnpinMessage(Message message);
    }
    
    // Listener for reply action
    public interface OnMessageReplyListener {
        void onReplyMessage(Message message);
    }
    
    public interface OnReplyPreviewClickListener {
        void onReplyPreviewClick(String replyToMessageId);
    }
    
    public interface OnMessageRecallListener {
        void onRecallMessage(Message message);
    }
    
    public interface OnMessageForwardListener {
        void onForwardMessage(Message message);
    }
    
    public interface OnMessageReactionListener {
        void onReactionClick(Message message, String reactionType);
        void onReactionLongPress(Message message, View anchorView);
        void onReactionStatsClick(Message message);
    }
    
    public interface OnPollInteractionListener {
        void onVotePoll(Message message, String optionId);
        void onClosePoll(Message message);
    }
    
    private OnMessageLongClickListener longClickListener;
    private OnMessageReplyListener replyListener;
    private OnReplyPreviewClickListener replyPreviewClickListener;
    private OnMessageRecallListener recallListener;
    private OnMessageForwardListener forwardListener;
    private OnMessageReactionListener reactionListener;
    private OnPollInteractionListener pollInteractionListener;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.isGroupChat = false;
    }
    
    public MessageAdapter(List<Message> messages, String currentUserId, boolean isGroupChat) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.isGroupChat = isGroupChat;
    }
    
    public void setGroupChat(boolean isGroupChat) {
        this.isGroupChat = isGroupChat;
        notifyDataSetChanged();
    }
    
    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }
    
    public void setOnMessageReplyListener(OnMessageReplyListener listener) {
        this.replyListener = listener;
    }
    
    public void setOnReplyPreviewClickListener(OnReplyPreviewClickListener listener) {
        this.replyPreviewClickListener = listener;
    }
    
    public void setOnMessageRecallListener(OnMessageRecallListener listener) {
        this.recallListener = listener;
    }
    
    public void setOnMessageForwardListener(OnMessageForwardListener listener) {
        this.forwardListener = listener;
    }
    
    public void setOnMessageReactionListener(OnMessageReactionListener listener) {
        this.reactionListener = listener;
    }
    
    public void setOnPollInteractionListener(OnPollInteractionListener listener) {
        this.pollInteractionListener = listener;
    }
    
    public void setPinnedMessageIds(java.util.List<String> pinnedIds) {
        this.pinnedMessageIds.clear();
        if (pinnedIds != null) {
            this.pinnedMessageIds.addAll(pinnedIds);
        }
        notifyDataSetChanged();
    }
    
    public boolean isMessagePinned(String messageId) {
        return pinnedMessageIds.contains(messageId);
    }
    
    public void highlightMessage(String messageId) {
        this.highlightedMessageId = messageId;
        int position = getPositionOfMessage(messageId);
        if (position >= 0) {
            notifyItemChanged(position);
            // Clear highlight after 2 seconds
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                this.highlightedMessageId = null;
                notifyItemChanged(position);
            }, 2000);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        
        // Debug log
        android.util.Log.d("MessageAdapter", "getItemViewType - position: " + position + 
            ", messageId: " + message.getId() + 
            ", isRecalled: " + message.isRecalled());
        
        boolean isSent = message.getSenderId().equals(currentUserId);
        
        // Check if message is recalled first (takes precedence)
        if (message.isRecalled()) {
            return isSent ? VIEW_TYPE_RECALLED_SENT : VIEW_TYPE_RECALLED_RECEIVED;
        }
        boolean isImage = Message.TYPE_IMAGE.equals(message.getType());
        boolean isFile = Message.TYPE_FILE.equals(message.getType());
        boolean isCall = Message.TYPE_CALL.equals(message.getType());
        boolean isPoll = Message.TYPE_POLL.equals(message.getType());
        boolean isContact = Message.TYPE_CONTACT.equals(message.getType());
        boolean isLocation = Message.TYPE_LOCATION.equals(message.getType());
        
        // Call history messages are always centered
        if (isCall) {
            return VIEW_TYPE_CALL_HISTORY;
        }
        
        // Poll messages
        if (isPoll) {
            return isSent ? VIEW_TYPE_POLL_SENT : VIEW_TYPE_POLL_RECEIVED;
        }
        
        // Contact messages (business cards)
        if (isContact) {
            return isSent ? VIEW_TYPE_CONTACT_SENT : VIEW_TYPE_CONTACT_RECEIVED;
        }
        
        // Location messages
        if (isLocation) {
            return isSent ? VIEW_TYPE_LOCATION_SENT : VIEW_TYPE_LOCATION_RECEIVED;
        }
        
        // Live Location messages
        if (Message.TYPE_LIVE_LOCATION.equals(message.getType())) {
            return isSent ? VIEW_TYPE_LIVE_LOCATION_SENT : VIEW_TYPE_LIVE_LOCATION_RECEIVED;
        }
        
        if (isSent) {
            if (isFile) return VIEW_TYPE_FILE_SENT;
            return isImage ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_SENT;
        } else {
            if (isFile) return VIEW_TYPE_FILE_RECEIVED;
            return isImage ? VIEW_TYPE_IMAGE_RECEIVED : VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(view);
            case VIEW_TYPE_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageViewHolder(view);
            case VIEW_TYPE_IMAGE_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_image_sent, parent, false);
                return new ImageSentViewHolder(view);
            case VIEW_TYPE_IMAGE_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_image_received, parent, false);
                return new ImageReceivedViewHolder(view);
            case VIEW_TYPE_FILE_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_file_sent, parent, false);
                return new FileMessageSentViewHolder(view);
            case VIEW_TYPE_FILE_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_file_received, parent, false);
                return new FileMessageReceivedViewHolder(view);
            case VIEW_TYPE_CALL_HISTORY:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_call_history, parent, false);
                return new CallHistoryViewHolder(view);
            case VIEW_TYPE_RECALLED_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_recalled_sent, parent, false);
                return new RecalledMessageViewHolder(view);
            case VIEW_TYPE_RECALLED_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_recalled_received, parent, false);
                return new RecalledMessageViewHolder(view);
            case VIEW_TYPE_POLL_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_poll_sent, parent, false);
                return new PollMessageViewHolder(view, true);
            case VIEW_TYPE_POLL_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_poll_received, parent, false);
                return new PollMessageViewHolder(view, false);
            case VIEW_TYPE_CONTACT_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_contact_sent, parent, false);
                return new ContactMessageViewHolder(view, true);
            case VIEW_TYPE_CONTACT_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_contact_received, parent, false);
                return new ContactMessageViewHolder(view, false);
            case VIEW_TYPE_LOCATION_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_location_sent, parent, false);
                return new LocationMessageViewHolder(view);
            case VIEW_TYPE_LOCATION_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_location_received, parent, false);
                return new LocationMessageViewHolder(view);
            case VIEW_TYPE_LIVE_LOCATION_SENT:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_live_location_sent, parent, false);
                return new LiveLocationMessageViewHolder(view, true);
            case VIEW_TYPE_LIVE_LOCATION_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_live_location_received, parent, false);
                return new LiveLocationMessageViewHolder(view, false);
            default:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isPinned = isMessagePinned(message.getId());
        boolean isHighlighted = message.getId() != null && message.getId().equals(highlightedMessageId);
        
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message, longClickListener, replyListener, replyPreviewClickListener, recallListener, forwardListener, currentUserId, isPinned, isHighlighted, reactionListener);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, isGroupChat, longClickListener, replyListener, replyPreviewClickListener, recallListener, forwardListener, currentUserId, isPinned, isHighlighted, reactionListener);
        } else if (holder instanceof ImageSentViewHolder) {
            ((ImageSentViewHolder) holder).bind(message, longClickListener, replyListener, replyPreviewClickListener, recallListener, forwardListener, currentUserId, isPinned, isHighlighted, reactionListener);
        } else if (holder instanceof LiveLocationMessageViewHolder) {
            ((LiveLocationMessageViewHolder) holder).bind(message, currentUserId);
        } else if (holder instanceof ImageReceivedViewHolder) {
            ((ImageReceivedViewHolder) holder).bind(message, isGroupChat, longClickListener, replyListener, replyPreviewClickListener, recallListener, forwardListener, currentUserId, isPinned, isHighlighted, reactionListener);
        } else if (holder instanceof FileMessageSentViewHolder) {
            ((FileMessageSentViewHolder) holder).bind(message, longClickListener, replyListener, replyPreviewClickListener, recallListener, forwardListener, currentUserId, isPinned, isHighlighted, reactionListener);
        } else if (holder instanceof FileMessageReceivedViewHolder) {
            ((FileMessageReceivedViewHolder) holder).bind(message, longClickListener, replyListener, replyPreviewClickListener, recallListener, forwardListener, currentUserId, isPinned, isHighlighted, reactionListener);
        } else if (holder instanceof CallHistoryViewHolder) {
            ((CallHistoryViewHolder) holder).bind(message);
        } else if (holder instanceof PollMessageViewHolder) {
            ((PollMessageViewHolder) holder).bind(message, currentUserId, isPinned, isHighlighted, pollInteractionListener);
        } else if (holder instanceof ContactMessageViewHolder) {
            ((ContactMessageViewHolder) holder).bind(message, currentUserId);
        } else if (holder instanceof LocationMessageViewHolder) {
            ((LocationMessageViewHolder) holder).bind(message);
        } else if (holder instanceof RecalledMessageViewHolder) {
            // Recalled messages just display static text, no binding needed
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        // Filter out call messages from other users
        // Each user should only see their own call history perspective
        List<Message> filteredMessages = new java.util.ArrayList<>();
        for (Message msg : newMessages) {
            if (Message.TYPE_CALL.equals(msg.getType())) {
                // Only include call messages from current user
                if (msg.getSenderId().equals(currentUserId)) {
                    filteredMessages.add(msg);
                }
            } else {
                // Include all non-call messages
                filteredMessages.add(msg);
            }
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(this.messages, filteredMessages));
        this.messages = filteredMessages;
        diffResult.dispatchUpdatesTo(this);
    }

    public List<Message> getMessages() {
        return messages;
    }
    
    private static void showMessageContextMenu(View view, Message message, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, boolean isPinned, String currentUserId) {
        // Don't show context menu for recalled messages
        if (message.isRecalled()) {
            return;
        }
        
        android.widget.PopupMenu popup = new android.widget.PopupMenu(view.getContext(), view);
        
        // Add reply option
        popup.getMenu().add(0, 3, 0, "↩️ Trả lời");
        
        // Add forward option
        popup.getMenu().add(0, 5, 0, "↪️ Chuyển tiếp");
        
        // Add pin/unpin option based on status
        if (isPinned) {
            popup.getMenu().add(0, 2, 0, "📌 Bỏ ghim tin nhắn");
        } else {
            popup.getMenu().add(0, 1, 0, "📌 Ghim tin nhắn");
        }
        
        // Add recall option (if message can be recalled)
        if (message.canBeRecalled(currentUserId)) {
            popup.getMenu().add(0, 4, 0, "🔄 Thu hồi");
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1 && listener != null) {
                listener.onPinMessage(message);
                return true;
            } else if (itemId == 2 && listener != null) {
                listener.onUnpinMessage(message);
                return true;
            } else if (itemId == 3 && replyListener != null) {
                replyListener.onReplyMessage(message);
                return true;
            } else if (itemId == 4 && recallListener != null) {
                recallListener.onRecallMessage(message);
                return true;
            } else if (itemId == 5 && forwardListener != null) {
                forwardListener.onForwardMessage(message);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    /**
     * Helper method to apply pin indicator and highlight effect to a message view
     */
    private static void applyPinAndHighlight(View itemView, boolean isPinned, boolean isHighlighted) {
        // Find pin indicator (can be ImageView or CardView)
        View pinIndicator = itemView.findViewById(R.id.pinIndicator);
        if (pinIndicator != null) {
            pinIndicator.setVisibility(isPinned ? View.VISIBLE : View.GONE);
        }
        
        // Apply highlight effect
        if (isHighlighted) {
            itemView.setBackgroundColor(0x40FFEB3B); // Semi-transparent yellow
        } else {
            itemView.setBackground(null);
        }
    }
    
    /**
     * Helper method to bind reaction indicator to a message view
     * @param itemView The message item view
     * @param message The message object
     * @param currentUserId Current user's ID
     * @param reactionListener Listener for reaction events
     */
    private static void bindReactionIndicator(View itemView, Message message, 
                                              String currentUserId, 
                                              OnMessageReactionListener reactionListener) {
        // Find new layout views
        View reactionContainer = itemView.findViewById(R.id.reactionContainer);
        View existingReactionsLayout = itemView.findViewById(R.id.existingReactionsLayout);
        ImageView addReactionButton = itemView.findViewById(R.id.addReactionButton);
        
        if (reactionContainer == null || addReactionButton == null) {
            return; // Layout doesn't have reaction views
        }
        
        // Always show reaction container and add button
        reactionContainer.setVisibility(View.VISIBLE);
        addReactionButton.setVisibility(View.VISIBLE);
        
        // Get reaction counts by type
        Map<String, String> reactions = message.getReactions();
        Map<String, Integer> reactionCounts = com.example.doan_zaloclone.models.MessageReaction.getReactionTypeCounts(
                reactions, message.getReactionCounts());
        String userReaction = message.getUserReaction(currentUserId);
        
        // Check if message has any reactions
        boolean hasReactions = message.hasReactions();
        
        // Get top 2 reactions to display (use reactionCounts if available)
        java.util.List<String> topReactions;
        if (message.getReactionCounts() != null && !message.getReactionCounts().isEmpty()) {
            topReactions = com.example.doan_zaloclone.models.MessageReaction.getTopReactionsFromCounts(
                    message.getReactionCounts(), 2);
        } else {
            topReactions = com.example.doan_zaloclone.models.MessageReaction.getTopReactions(reactions, 2);
        }
        
        // Check if there are any reactions with count > 0
        boolean hasValidReactions = false;
        for (String reactionType : topReactions) {
            Integer count = reactionCounts.get(reactionType);
            if (count != null && count > 0) {
                hasValidReactions = true;
                break;
            }
        }
        
        if (hasReactions && hasValidReactions && existingReactionsLayout != null) {
            existingReactionsLayout.setVisibility(View.VISIBLE);
            
            // Hide all reaction layouts first
            hideAllReactionLayouts(itemView);
            
            // Only show top 2 reactions with count > 0
            for (String reactionType : topReactions) {
                int layoutId = getReactionLayoutId(reactionType);
                int countTextId = getReactionCountTextId(reactionType);
                Integer count = reactionCounts.get(reactionType);
                if (layoutId != 0 && countTextId != 0 && count != null && count > 0) {
                    bindSingleReaction(itemView, layoutId, countTextId,
                            reactionType, count,
                            message, reactionListener);
                }
            }
                    
            // Click on existing reactions container to show reaction stats
            existingReactionsLayout.setOnClickListener(v -> {
                if (reactionListener != null) {
                    reactionListener.onReactionStatsClick(message);
                }
            });
        } else if (existingReactionsLayout != null) {
            existingReactionsLayout.setVisibility(View.GONE);
        }
        
        // Update add reaction button icon based on user's reaction
        if (userReaction != null) {
            // User has reacted - show their reaction icon (filled)
            int iconRes = getReactionIconResource(userReaction);
            addReactionButton.setImageResource(iconRes);
        } else {
            // No reaction - show heart outline
            addReactionButton.setImageResource(R.drawable.ic_reaction_heart_outline);
        }
        
        // Click on add reaction button - add current user's reaction (or heart if none)
        addReactionButton.setOnClickListener(v -> {
            if (reactionListener != null) {
                // Use user's current reaction if they have one, otherwise default to heart
                String reactionToAdd = userReaction != null 
                        ? userReaction 
                        : com.example.doan_zaloclone.models.MessageReaction.REACTION_HEART;
                reactionListener.onReactionClick(message, reactionToAdd);
            }
        });
        
        // Long press on add reaction button to show picker
        addReactionButton.setOnLongClickListener(v -> {
            if (reactionListener != null) {
                reactionListener.onReactionLongPress(message, v);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Bind a single reaction type layout
     * No click listener - whole counter area shows stats, only add button increments count
     */
    private static void bindSingleReaction(View itemView, int layoutId, int countTextId,
                                           String reactionType, int count,
                                           Message message, OnMessageReactionListener listener) {
        View layout = itemView.findViewById(layoutId);
        TextView countText = itemView.findViewById(countTextId);
        
        if (layout == null) return;
        
        if (count > 0) {
            layout.setVisibility(View.VISIBLE);
            if (countText != null) {
                countText.setText(String.valueOf(count));
            }
            // No onClick - let parent handle it for stats
        } else {
            layout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Get drawable resource for a reaction type
     */
    private static int getReactionIconResource(String reactionType) {
        if (reactionType == null) {
            return R.drawable.ic_reaction_heart_outline;
        }
        
        switch (reactionType) {
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_HEART:
                return R.drawable.ic_reaction_heart;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_HAHA:
                return R.drawable.ic_reaction_haha;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_SAD:
                return R.drawable.ic_reaction_sad;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_ANGRY:
                return R.drawable.ic_reaction_angry;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_WOW:
                return R.drawable.ic_reaction_wow;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_LIKE:
                return R.drawable.ic_reaction_like;
            default:
                return R.drawable.ic_reaction_heart_outline;
        }
    }

    /**
     * Get layout ID for a reaction type
     */
    private static int getReactionLayoutId(String reactionType) {
        if (reactionType == null) return 0;
        switch (reactionType) {
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_HEART:
                return R.id.heartReactionLayout;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_LIKE:
                return R.id.likeReactionLayout;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_HAHA:
                return R.id.hahaReactionLayout;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_SAD:
                return R.id.sadReactionLayout;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_ANGRY:
                return R.id.angryReactionLayout;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_WOW:
                return R.id.wowReactionLayout;
            default:
                return 0;
        }
    }

    /**
     * Get count TextView ID for a reaction type
     */
    private static int getReactionCountTextId(String reactionType) {
        if (reactionType == null) return 0;
        switch (reactionType) {
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_HEART:
                return R.id.heartReactionCount;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_LIKE:
                return R.id.likeReactionCount;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_HAHA:
                return R.id.hahaReactionCount;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_SAD:
                return R.id.sadReactionCount;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_ANGRY:
                return R.id.angryReactionCount;
            case com.example.doan_zaloclone.models.MessageReaction.REACTION_WOW:
                return R.id.wowReactionCount;
            default:
                return 0;
        }
    }

    /**
     * Hide all reaction layouts
     */
    private static void hideAllReactionLayouts(View itemView) {
        int[] layoutIds = {
            R.id.heartReactionLayout,
            R.id.likeReactionLayout,
            R.id.hahaReactionLayout,
            R.id.sadReactionLayout,
            R.id.angryReactionLayout,
            R.id.wowReactionLayout
        };
        
        for (int layoutId : layoutIds) {
            View layout = itemView.findViewById(layoutId);
            if (layout != null) {
                layout.setVisibility(View.GONE);
            }
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;
        private TextView timestampTextView;
        private TextView forwardedIndicator;
        private View replyPreviewContainer;
        private TextView replyToSenderName;
        private TextView replyToContent;
        private OnMessageLongClickListener listener;
        private OnMessageReplyListener replyListener;
        private OnMessageRecallListener recallListener;
        private String currentUserId;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            forwardedIndicator = itemView.findViewById(R.id.forwardedIndicator);
            replyPreviewContainer = itemView.findViewById(R.id.replyPreviewContainer);
            replyToSenderName = itemView.findViewById(R.id.replyToSenderName);
            replyToContent = itemView.findViewById(R.id.replyToContent);
        }

        public void bind(Message message, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnReplyPreviewClickListener previewClickListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, String currentUserId, boolean isPinned, boolean isHighlighted, OnMessageReactionListener reactionListener) {
            this.listener = listener;
            this.replyListener = replyListener;
            this.recallListener = recallListener;
            this.currentUserId = currentUserId;
            messageTextView.setText(message.getContent());
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Debug log for reply data
            android.util.Log.d("MessageAdapter", "bind() - messageId: " + message.getId() + 
                ", isReplyMessage: " + message.isReplyMessage() + 
                ", replyToId: " + message.getReplyToId() +
                ", replyToSenderName: " + message.getReplyToSenderName());
            
            // Bind reply preview if this is a reply message
            if (message.isReplyMessage() && replyPreviewContainer != null) {
                replyPreviewContainer.setVisibility(View.VISIBLE);
                if (replyToSenderName != null) {
                    replyToSenderName.setText(message.getReplyToSenderName() != null ? message.getReplyToSenderName() : "User");
                }
                if (replyToContent != null) {
                    replyToContent.setText(message.getReplyToContent() != null ? message.getReplyToContent() : "");
                }
                // Set click listener to navigate to original message
                final String replyToId = message.getReplyToId();
                replyPreviewContainer.setOnClickListener(v -> {
                    if (previewClickListener != null && replyToId != null) {
                        previewClickListener.onReplyPreviewClick(replyToId);
                    }
                });
            } else if (replyPreviewContainer != null) {
                replyPreviewContainer.setVisibility(View.GONE);
                replyPreviewContainer.setOnClickListener(null);
            }
            
            // Bind forwarded indicator
            if (forwardedIndicator != null) {
                if (message.isForwardedFromOther(currentUserId)) {
                    forwardedIndicator.setVisibility(View.VISIBLE);
                } else {
                    forwardedIndicator.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, replyListener, recallListener, forwardListener, isPinned, currentUserId);
                return true;
            });
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;
        private TextView timestampTextView;
        private TextView senderNameTextView;
        private TextView forwardedIndicator;
        private View replyPreviewContainer;
        private TextView replyToSenderName;
        private TextView replyToContent;
        private OnMessageLongClickListener listener;
        private OnMessageReplyListener replyListener;
        private OnMessageRecallListener recallListener;
        private String currentUserId;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            forwardedIndicator = itemView.findViewById(R.id.forwardedIndicator);
            replyPreviewContainer = itemView.findViewById(R.id.replyPreviewContainer);
            replyToSenderName = itemView.findViewById(R.id.replyToSenderName);
            replyToContent = itemView.findViewById(R.id.replyToContent);
        }

        public void bind(Message message, boolean isGroupChat, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnReplyPreviewClickListener previewClickListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, String currentUserId, boolean isPinned, boolean isHighlighted, OnMessageReactionListener reactionListener) {
            this.listener = listener;
            this.replyListener = replyListener;
            this.recallListener = recallListener;
            this.currentUserId = currentUserId;
            messageTextView.setText(message.getContent());
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Bind reply preview if this is a reply message
            if (message.isReplyMessage() && replyPreviewContainer != null) {
                replyPreviewContainer.setVisibility(View.VISIBLE);
                if (replyToSenderName != null) {
                    replyToSenderName.setText(message.getReplyToSenderName() != null ? message.getReplyToSenderName() : "User");
                }
                if (replyToContent != null) {
                    replyToContent.setText(message.getReplyToContent() != null ? message.getReplyToContent() : "");
                }
                // Set click listener to navigate to original message
                final String replyToId = message.getReplyToId();
                replyPreviewContainer.setOnClickListener(v -> {
                    if (previewClickListener != null && replyToId != null) {
                        previewClickListener.onReplyPreviewClick(replyToId);
                    }
                });
            } else if (replyPreviewContainer != null) {
                replyPreviewContainer.setVisibility(View.GONE);
                replyPreviewContainer.setOnClickListener(null);
            }
            
            // Show sender name only in group chats
            if (senderNameTextView != null) {
                if (isGroupChat) {
                    senderNameTextView.setVisibility(View.VISIBLE);
                    // Try to use cached sender name first
                    String cachedName = message.getSenderName();
                    if (cachedName != null && !cachedName.isEmpty()) {
                        senderNameTextView.setText(cachedName);
                    } else {
                        // Fallback: Fetch sender name from Firestore for old messages
                        String senderId = message.getSenderId();
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(senderId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String senderName = doc.getString("name");
                                    if (senderName != null) {
                                        senderNameTextView.setText(senderName);
                                    } else {
                                        senderNameTextView.setText("User");
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                senderNameTextView.setText("User");
                            });
                    }
                } else {
                    senderNameTextView.setVisibility(View.GONE);
                }
            }
            
            // Bind forwarded indicator
            if (forwardedIndicator != null) {
                if (message.isForwardedFromOther(currentUserId)) {
                    forwardedIndicator.setVisibility(View.VISIBLE);
                } else {
                    forwardedIndicator.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, replyListener, recallListener, forwardListener, isPinned, currentUserId);
                return true;
            });
        }
    }
    
    static class ImageSentViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImageView;
        private TextView timestampTextView;
        private TextView forwardedIndicator;
        private OnMessageLongClickListener listener;
        private OnMessageRecallListener recallListener;
        private String currentUserId;

        public ImageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            forwardedIndicator = itemView.findViewById(R.id.forwardedIndicator);
        }

        public void bind(Message message, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnReplyPreviewClickListener previewClickListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, String currentUserId, boolean isPinned, boolean isHighlighted, OnMessageReactionListener reactionListener) {
            this.listener = listener;
            this.recallListener = recallListener;
            this.currentUserId = currentUserId;
            Glide.with(itemView.getContext())
                    .load(message.getContent())
                    .into(messageImageView);
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Bind forwarded indicator
            if (forwardedIndicator != null) {
                if (message.isForwardedFromOther(currentUserId)) {
                    forwardedIndicator.setVisibility(View.VISIBLE);
                } else {
                    forwardedIndicator.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, replyListener, recallListener, forwardListener, isPinned, currentUserId);
                return true;
            });
        }
    }
    
    static class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImageView;
        private TextView timestampTextView;
        private TextView senderNameTextView;
        private TextView forwardedIndicator;
        private OnMessageLongClickListener listener;
        private OnMessageRecallListener recallListener;
        private String currentUserId;

        public ImageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            forwardedIndicator = itemView.findViewById(R.id.forwardedIndicator);
        }

        public void bind(Message message, boolean isGroupChat, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnReplyPreviewClickListener previewClickListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, String currentUserId, boolean isPinned, boolean isHighlighted, OnMessageReactionListener reactionListener) {
            this.listener = listener;
            this.recallListener = recallListener;
            this.currentUserId = currentUserId;
            Glide.with(itemView.getContext())
                    .load(message.getContent())
                    .into(messageImageView);
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Show sender name only in group chats
            if (senderNameTextView != null) {
                if (isGroupChat) {
                    senderNameTextView.setVisibility(View.VISIBLE);
                    // Try to use cached sender name first
                    String cachedName = message.getSenderName();
                    if (cachedName != null && !cachedName.isEmpty()) {
                        senderNameTextView.setText(cachedName);
                    } else {
                        // Fallback: Fetch sender name from Firestore for old messages
                        String senderId = message.getSenderId();
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(senderId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String senderName = doc.getString("name");
                                    if (senderName != null) {
                                        senderNameTextView.setText(senderName);
                                    } else {
                                        senderNameTextView.setText("User");
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                senderNameTextView.setText("User");
                            });
                    }
                } else {
                    senderNameTextView.setVisibility(View.GONE);
                }
            }
            
            // Bind forwarded indicator
            if (forwardedIndicator != null) {
                if (message.isForwardedFromOther(currentUserId)) {
                    forwardedIndicator.setVisibility(View.VISIBLE);
                } else {
                    forwardedIndicator.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, replyListener, recallListener, forwardListener, isPinned, currentUserId);
                return true;
            });
        }
    }
    
    static class FileMessageSentViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileIcon;
        private TextView fileName;
        private TextView fileSize;
        private TextView timestampTextView;
        private TextView forwardedIndicator;
        private OnMessageLongClickListener listener;
        private OnMessageRecallListener recallListener;
        private String currentUserId;

        public FileMessageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            forwardedIndicator = itemView.findViewById(R.id.forwardedIndicator);
        }

        public void bind(Message message, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnReplyPreviewClickListener previewClickListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, String currentUserId, boolean isPinned, boolean isHighlighted, OnMessageReactionListener reactionListener) {
            this.listener = listener;
            this.recallListener = recallListener;
            this.currentUserId = currentUserId;
            fileName.setText(message.getFileName());
            
            // Display "FileType - Size" format
            String fileType = com.example.doan_zaloclone.utils.FileTypeUtils.getFileTypeLabel(message.getFileMimeType());
            String sizeFormatted = message.getFormattedFileSize();
            fileSize.setText(fileType + " - " + sizeFormatted);
            
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Set appropriate icon based on file type
            int iconResId = com.example.doan_zaloclone.utils.FileUtils.getFileIcon(message.getFileMimeType());
            fileIcon.setImageResource(iconResId);
            
            // Bind forwarded indicator
            if (forwardedIndicator != null) {
                if (message.isForwardedFromOther(currentUserId)) {
                    forwardedIndicator.setVisibility(View.VISIBLE);
                } else {
                    forwardedIndicator.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            // Bind reaction indicator
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
            
            // Add click listener to open file
            itemView.setOnClickListener(v -> openFile(message));
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, replyListener, recallListener, forwardListener, isPinned, currentUserId);
                return true;
            });
        }
        
        private void openFile(Message message) {
            // Show loading dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(itemView.getContext());
            progressDialog.setMessage("Đang tải file...");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Download file first
            String fileUrl = message.getContent();
            String fileName = message.getFileName();
            
            com.example.doan_zaloclone.utils.FileDownloadHelper.downloadFile(
                itemView.getContext(),
                fileUrl,
                fileName,
                new com.example.doan_zaloclone.utils.FileDownloadHelper.DownloadCallback() {
                    @Override
                    public void onProgress(int progress) {
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            progressDialog.setProgress(progress);
                        });
                    }
                    
                    @Override
                    public void onSuccess(java.io.File file) {
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            progressDialog.dismiss();
                            
                            try {
                                // Get URI using FileProvider
                                android.net.Uri fileUri = com.example.doan_zaloclone.utils.FileDownloadHelper.getFileUri(
                                        itemView.getContext(), file);
                                
                                // Open file with appropriate app
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(fileUri, message.getFileMimeType());
                                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | 
                                              android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                itemView.getContext().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                android.widget.Toast.makeText(itemView.getContext(), 
                                    "Không tìm thấy ứng dụng để mở file này", 
                                    android.widget.Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                android.widget.Toast.makeText(itemView.getContext(), 
                                    "Lỗi mở file: " + e.getMessage(), 
                                    android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            progressDialog.dismiss();
                            android.widget.Toast.makeText(itemView.getContext(), 
                                "Lỗi tải file: " + error, 
                                android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }
                });
        }
    }

    static class FileMessageReceivedViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileIcon;
        private TextView fileName;
        private TextView fileSize;
        private TextView timestampTextView;
        private TextView forwardedIndicator;
        private OnMessageLongClickListener listener;
        private OnMessageRecallListener recallListener;
        private String currentUserId;

        public FileMessageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            forwardedIndicator = itemView.findViewById(R.id.forwardedIndicator);
        }

        public void bind(Message message, OnMessageLongClickListener listener, OnMessageReplyListener replyListener, OnReplyPreviewClickListener previewClickListener, OnMessageRecallListener recallListener, OnMessageForwardListener forwardListener, String currentUserId, boolean isPinned, boolean isHighlighted, OnMessageReactionListener reactionListener) {
            this.listener = listener;
            this.recallListener = recallListener;
            this.currentUserId = currentUserId;
            fileName.setText(message.getFileName());
            
            // Display "FileType - Size" format
            String fileType = com.example.doan_zaloclone.utils.FileTypeUtils.getFileTypeLabel(message.getFileMimeType());
            String sizeFormatted = message.getFormattedFileSize();
            fileSize.setText(fileType + " - " + sizeFormatted);
            
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Set appropriate icon based on file type
            int iconResId = com.example.doan_zaloclone.utils.FileUtils.getFileIcon(message.getFileMimeType());
            fileIcon.setImageResource(iconResId);
            
            // Bind forwarded indicator
            if (forwardedIndicator != null) {
                if (message.isForwardedFromOther(currentUserId)) {
                    forwardedIndicator.setVisibility(View.VISIBLE);
                } else {
                    forwardedIndicator.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            // Bind reaction indicator
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
            
            // Add click listener to open file
            itemView.setOnClickListener(v -> openFile(message));
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, replyListener, recallListener, forwardListener, isPinned, currentUserId);
                return true;
            });
        }
        
        private void openFile(Message message) {
            // Show loading dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(itemView.getContext());
            progressDialog.setMessage("Đang tải file...");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Download file first
            String fileUrl = message.getContent();
            String fileName = message.getFileName();
            
            com.example.doan_zaloclone.utils.FileDownloadHelper.downloadFile(
                itemView.getContext(),
                fileUrl,
                fileName,
                new com.example.doan_zaloclone.utils.FileDownloadHelper.DownloadCallback() {
                    @Override
                    public void onProgress(int progress) {
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            progressDialog.setProgress(progress);
                        });
                    }
                    
                    @Override
                    public void onSuccess(java.io.File file) {
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            progressDialog.dismiss();
                            
                            try {
                                // Get URI using FileProvider
                                android.net.Uri fileUri = com.example.doan_zaloclone.utils.FileDownloadHelper.getFileUri(
                                        itemView.getContext(), file);
                                
                                // Open file with appropriate app
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(fileUri, message.getFileMimeType());
                                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | 
                                              android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                itemView.getContext().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                android.widget.Toast.makeText(itemView.getContext(), 
                                    "Không tìm thấy ứng dụng để mở file này", 
                                    android.widget.Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                android.widget.Toast.makeText(itemView.getContext(), 
                                    "Lỗi mở file: " + e.getMessage(), 
                                    android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            progressDialog.dismiss();
                            android.widget.Toast.makeText(itemView.getContext(), 
                                "Lỗi tải file: " + error, 
                                android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }
                });
        }
    }

    static class CallHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView callHistoryTextView;

        public CallHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            callHistoryTextView = itemView.findViewById(R.id.callHistoryTextView);
        }

        public void bind(Message message) {
            String content = message.getContent();
            callHistoryTextView.setText(content);
            
            // Apply color based on call type
            if (content != null && content.contains("nhỡ")) {
                // Missed call - RED
                callHistoryTextView.setTextColor(0xFFE53935);
            } else if (content != null && content.contains("đi")) {
                // Outgoing call - LIGHT GRAY
                callHistoryTextView.setTextColor(0xFFB0B0B0);
            } else if (content != null && content.contains("đến")) {
                // Incoming call - STANDARD GRAY
                callHistoryTextView.setTextColor(0xFF757575);
            } else {
                // Default gray (fallback)
                callHistoryTextView.setTextColor(0xFF757575);
            }
        }
    }
    
    /**
     * ViewHolder for recalled messages
     */
    static class RecalledMessageViewHolder extends RecyclerView.ViewHolder {
        public RecalledMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // No bindings needed - static text layout
        }
    }
    
    // DiffUtil Callback for efficient list updates
    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<Message> oldList;
        private final List<Message> newList;
        
        public MessageDiffCallback(List<Message> oldList, List<Message> newList) {
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
            // Compare message IDs
            return oldList.get(oldItemPosition).getId().equals(
                    newList.get(newItemPosition).getId());
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Message oldMessage = oldList.get(oldItemPosition);
            Message newMessage = newList.get(newItemPosition);
            
            // Safe null comparison for content (poll messages may have null content)
            String oldContent = oldMessage.getContent();
            String newContent = newMessage.getContent();
            boolean contentMatch = (oldContent == null && newContent == null) ||
                    (oldContent != null && oldContent.equals(newContent));
            
            // Safe null comparison for type
            String oldType = oldMessage.getType();
            String newType = newMessage.getType();
            boolean typeMatch = (oldType == null && newType == null) ||
                    (oldType != null && oldType.equals(newType));
            
            // Compare all relevant fields including reactions
            boolean basicFieldsMatch = contentMatch &&
                   oldMessage.getSenderId().equals(newMessage.getSenderId()) &&
                   oldMessage.getTimestamp() == newMessage.getTimestamp() &&
                   typeMatch;
            
            if (!basicFieldsMatch) {
                return false;
            }
            
            // Compare reactions
            java.util.Map<String, String> oldReactions = oldMessage.getReactions();
            java.util.Map<String, String> newReactions = newMessage.getReactions();
            
            boolean reactionsMatch;
            if (oldReactions == null && newReactions == null) {
                reactionsMatch = true;
            } else if (oldReactions == null || newReactions == null) {
                reactionsMatch = false;
            } else {
                reactionsMatch = oldReactions.equals(newReactions);
            }
            
            if (!reactionsMatch) {
                return false;
            }
            
            // Compare reactionCounts (CRITICAL for realtime count updates)
            java.util.Map<String, Integer> oldCounts = oldMessage.getReactionCounts();
            java.util.Map<String, Integer> newCounts = newMessage.getReactionCounts();
            
            if (oldCounts == null && newCounts == null) {
                // Both null, continue to poll comparison
            } else if (oldCounts == null || newCounts == null) {
                return false;
            } else if (!oldCounts.equals(newCounts)) {
                return false;
            }
            
            // Compare pollData (CRITICAL for realtime vote updates)
            com.example.doan_zaloclone.models.Poll oldPoll = oldMessage.getPollData();
            com.example.doan_zaloclone.models.Poll newPoll = newMessage.getPollData();
            
            if (oldPoll == null && newPoll == null) {
                return true; // No poll data in both
            }
            if (oldPoll == null || newPoll == null) {
                return false; // One has poll, other doesn't
            }
            
            // Compare poll options (votes)
            java.util.List<com.example.doan_zaloclone.models.PollOption> oldOptions = oldPoll.getOptions();
            java.util.List<com.example.doan_zaloclone.models.PollOption> newOptions = newPoll.getOptions();
            
            if (oldOptions == null && newOptions == null) {
                return true;
            }
            if (oldOptions == null || newOptions == null) {
                return false;
            }
            if (oldOptions.size() != newOptions.size()) {
                return false;
            }
            
            // Compare each option's votes
            for (int i = 0; i < oldOptions.size(); i++) {
                com.example.doan_zaloclone.models.PollOption oldOpt = oldOptions.get(i);
                com.example.doan_zaloclone.models.PollOption newOpt = newOptions.get(i);
                
                java.util.List<String> oldVoters = oldOpt.getVoterIds();
                java.util.List<String> newVoters = newOpt.getVoterIds();
                
                if (oldVoters == null && newVoters == null) continue;
                if (oldVoters == null || newVoters == null) return false;
                if (oldVoters.size() != newVoters.size()) return false;
                if (!oldVoters.containsAll(newVoters)) return false;
            }
            
            // Also check if poll is closed
            if (oldPoll.isClosed() != newPoll.isClosed()) {
                return false;
            }
            
            return true;
        }
    }

    /**
     * Get position of message by ID
     * @param messageId Message ID to find
     * @return Position in adapter, or -1 if not found
     */
    public int getPositionOfMessage(String messageId) {
        if (messageId == null || messages == null) return -1;

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg != null && messageId.equals(msg.getId())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Get message at specific position
     * @param position Position in adapter
     * @return Message at position, or null if invalid
     */
    public Message getMessageAt(int position) {
        if (position < 0 || position >= messages.size()) {
            return null;
        }
        return messages.get(position);
    }
    
    // ============== POLL MESSAGE VIEW HOLDER ==============
    
    static class PollMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView questionTextView;
        private RecyclerView optionsRecyclerView;
        private TextView voterCountText;
        private TextView deadlineText;
        private TextView closePollButton;
        private TextView addOptionButton;
        private TextView seeMoreButton;
        private TextView timestampTextView;
        private ImageView pinIndicator;
        private boolean isSent;
        
        private PollOptionAdapter pollOptionAdapter;
        private boolean showingAllOptions = false;
        
        public PollMessageViewHolder(@NonNull View itemView, boolean isSent) {
            super(itemView);
            this.isSent = isSent;
            
            questionTextView = itemView.findViewById(R.id.questionTextView);
            optionsRecyclerView = itemView.findViewById(R.id.optionsRecyclerView);
            voterCountText = itemView.findViewById(R.id.voterCountText);
            deadlineText = itemView.findViewById(R.id.deadlineText);
            closePollButton = itemView.findViewById(R.id.closePollButton);
            addOptionButton = itemView.findViewById(R.id.addOptionButton);
            seeMoreButton = itemView.findViewById(R.id.seeMoreButton);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            pinIndicator = itemView.findViewById(R.id.pinIndicator);
            
            // Setup RecyclerView for options
            optionsRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(itemView.getContext()));
        }
        
        public void bind(Message message, String currentUserId, boolean isPinned, boolean isHighlighted, OnPollInteractionListener pollListener) {
            com.example.doan_zaloclone.models.Poll poll = message.getPollData();
            if (poll == null) return;
            
            // Set question
            questionTextView.setText(poll.getQuestion());
            
            // Determine how many options to show
            java.util.List<com.example.doan_zaloclone.models.PollOption> allOptions = poll.getOptions();
            int totalOptions = allOptions.size();
            int maxDisplay = 5;
            
            // Create limited poll for display
            java.util.List<com.example.doan_zaloclone.models.PollOption> displayOptions;
            if (!showingAllOptions && totalOptions > maxDisplay) {
                // Show only first 4 options when collapsed
                displayOptions = allOptions.subList(0, maxDisplay - 1);
            } else {
                displayOptions = allOptions;
            }
            
            // Create temporary poll with limited options for adapter
            com.example.doan_zaloclone.models.Poll displayPoll = poll;
            if (displayOptions.size() != allOptions.size()) {
                // Clone poll with limited options
                displayPoll = new com.example.doan_zaloclone.models.Poll(poll.getId(), poll.getQuestion(), poll.getCreatorId());
                displayPoll.setOptions(new java.util.ArrayList<>(displayOptions));
                displayPoll.setPinned(poll.isPinned());
                displayPoll.setAnonymous(poll.isAnonymous());
                displayPoll.setHideResultsUntilVoted(poll.isHideResultsUntilVoted());
                displayPoll.setAllowMultipleChoice(poll.isAllowMultipleChoice());
                displayPoll.setAllowAddOptions(poll.isAllowAddOptions());
                displayPoll.setClosed(poll.isClosed());
                displayPoll.setExpiresAt(poll.getExpiresAt());
                displayPoll.setCreatedAt(poll.getCreatedAt());
            }
            
            // Setup options adapter with real voting
            final com.example.doan_zaloclone.models.Poll finalPoll = poll; // Original poll for voting
            pollOptionAdapter = new PollOptionAdapter(displayPoll, currentUserId, (optionIndex, option) -> {
                // Handle vote - call listener with original poll
                if (pollListener != null) {
                    pollListener.onVotePoll(message, option.getId());
                }
            });
            optionsRecyclerView.setAdapter(pollOptionAdapter);
            
            // See more button logic
            if (seeMoreButton != null) {
                if (!showingAllOptions && totalOptions > maxDisplay) {
                    seeMoreButton.setVisibility(View.VISIBLE);
                    seeMoreButton.setText("Xem thêm (" + (totalOptions - (maxDisplay - 1)) + " lựa chọn)");
                    seeMoreButton.setOnClickListener(v -> {
                        showingAllOptions = true;
                        bind(message, currentUserId, isPinned, isHighlighted, pollListener);
                    });
                } else {
                    seeMoreButton.setVisibility(View.GONE);
                }
            }
            
            // Add option button - show only if < 5 options and allowed
            if (addOptionButton != null) {
                if (totalOptions < maxDisplay && poll.isAllowAddOptions() && poll.canVote()) {
                    addOptionButton.setVisibility(View.VISIBLE);
                    addOptionButton.setOnClickListener(v -> {
                        android.widget.Toast.makeText(itemView.getContext(), 
                                "Add option (to be implemented)", 
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
                } else {
                    addOptionButton.setVisibility(View.GONE);
                }
            }
            
            // Voter count
            int totalVotes = poll.getTotalVotes();
            int totalVoters = poll.getTotalVoters();
            if (poll.isAnonymous()) {
                voterCountText.setText(totalVotes + " lượt bình chọn");
            } else {
                voterCountText.setText(totalVoters + " người đã bình chọn");
            }
            
            // Deadline
            if (poll.getExpiresAt() > 0) {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                String deadlineStr = "Hết hạn: " + dateFormat.format(new java.util.Date(poll.getExpiresAt()));
                deadlineText.setText(deadlineStr);
                deadlineText.setVisibility(View.VISIBLE);
            } else {
                deadlineText.setVisibility(View.GONE);
            }
            
            // Close poll button (only show for creator in open polls)
            if (closePollButton != null && poll.canClosePoll(currentUserId, false) && !poll.isClosed()) {
                closePollButton.setVisibility(View.VISIBLE);
                closePollButton.setOnClickListener(v -> {
                    if (pollListener != null) {
                        // Show confirmation dialog
                        new android.app.AlertDialog.Builder(itemView.getContext())
                                .setTitle("Đóng bình chọn")
                                .setMessage("Bạn có chắc muốn đóng bình chọn này? Sau khi đóng sẽ không thể bình chọn thêm.")
                                .setPositiveButton("Đóng", (dialog, which) -> {
                                    pollListener.onClosePoll(message);
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    }
                });
            } else if (closePollButton != null) {
                closePollButton.setVisibility(View.GONE);
            }
            
            // Timestamp
            if (timestampTextView != null) {
                timestampTextView.setText(TIMESTAMP_FORMAT.format(new java.util.Date(message.getTimestamp())));
            }
            
            // Pin indicator
            if (pinIndicator != null) {
                pinIndicator.setVisibility(isPinned ? View.VISIBLE : View.GONE);
            }
            
            // Highlight effect
            if (isHighlighted) {
                itemView.setBackgroundColor(0x40FFEB3B);
            } else {
                itemView.setBackground(null);
            }
        }
    }
    
    /**
     * ViewHolder for contact/business card messages
     */
    static class ContactMessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView contactAvatar;
        private TextView contactName;
        private TextView contactPhone;
        private LinearLayout phoneContainer;
        private LinearLayout btnAddFriendFromCard; // Changed to LinearLayout
        private TextView btnAddFriendText; // Text inside button
        private View friendRequestDivider; // Divider above button
        private LinearLayout btnCallFromCard;
        private LinearLayout btnMessageFromCard;
        private boolean isSent;
        
        // Track current contact to validate async callbacks
        private String currentContactUserId;
        // Store listeners for cleanup on rebind
        private com.google.firebase.firestore.ListenerRegistration friendRequestListener;
        private com.google.firebase.firestore.ListenerRegistration friendsListener;
        
        // Friendship status cache (memory cache to reduce Firestore reads)
        private static final java.util.Map<String, FriendshipStatus> friendshipCache = 
            new java.util.concurrent.ConcurrentHashMap<>();
        private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
        
        // Retry mechanism
        private int listenerRetryCount = 0;
        private static final int MAX_RETRY_ATTEMPTS = 3;
        private android.os.Handler retryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        // Loading state
        private boolean isLoadingFriendshipStatus = false;
        
        /**
         * Cached friendship status with timestamp
         */
        private static class FriendshipStatus {
            String statusFromMe;
            String statusToMe;
            long timestamp;
            
            FriendshipStatus(String statusFromMe, String statusToMe) {
                this.statusFromMe = statusFromMe;
                this.statusToMe = statusToMe;
                this.timestamp = System.currentTimeMillis();
            }
            
            boolean isExpired() {
                return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
            }
            
            boolean areFriends() {
                return "ACCEPTED".equals(statusFromMe) || "ACCEPTED".equals(statusToMe);
            }
        }
        
        public ContactMessageViewHolder(@NonNull View itemView, boolean isSent) {
            super(itemView);
            this.isSent = isSent;
            contactAvatar = itemView.findViewById(R.id.contactAvatar);
            contactName = itemView.findViewById(R.id.contactName);
            contactPhone = itemView.findViewById(R.id.contactPhone);
            phoneContainer = itemView.findViewById(R.id.phoneContainer);
            btnAddFriendFromCard = itemView.findViewById(R.id.btnAddFriendFromCard);
            btnAddFriendText = itemView.findViewById(R.id.btnAddFriendText);
            friendRequestDivider = itemView.findViewById(R.id.friendRequestDivider);
            btnCallFromCard = itemView.findViewById(R.id.btnCallFromCard);
            btnMessageFromCard = itemView.findViewById(R.id.btnMessageFromCard);
        }
        
        public void bind(Message message, String currentUserId) {
            // CRITICAL: Reset UI state immediately to prevent ViewHolder recycling issues
            btnAddFriendFromCard.setVisibility(View.GONE);
            friendRequestDivider.setVisibility(View.GONE);
            
            // Remove old listeners if exists to prevent memory leaks
            if (friendRequestListener != null) {
                friendRequestListener.remove();
                friendRequestListener = null;
            }
            if (friendsListener != null) {
                friendsListener.remove();
                friendsListener = null;
            }
            
            // Debug logs
            android.util.Log.d("ContactMessageViewHolder", "Binding contact message");
            android.util.Log.d("ContactMessageViewHolder", "Message ID: " + message.getId());
            android.util.Log.d("ContactMessageViewHolder", "Message Type: " + message.getType());
            android.util.Log.d("ContactMessageViewHolder", "Contact User ID: " + message.getContactUserId());
            
            // Get contact user ID from message
            String contactUserId = message.getContactUserId();
            if (contactUserId == null || contactUserId.isEmpty()) {
                android.util.Log.e("ContactMessageViewHolder", "ERROR: contactUserId is null or empty!");
                contactName.setText("Không thể tải thông tin");
                return;
            }
            
            // Store current contact ID for callback validation
            this.currentContactUserId = contactUserId;
            
            android.util.Log.d("ContactMessageViewHolder", "Fetching user info for: " + contactUserId);
            
            // Fetch contact user info from Firestore
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(contactUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String avatarUrl = doc.getString("avatarUrl");
                        String phoneNumber = doc.getString("phoneNumber");
                        
                        // Set contact name
                        if (name != null && !name.isEmpty()) {
                            contactName.setText(name);
                        } else {
                            contactName.setText("Người dùng");
                        }
                        
                        // Set avatar
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(itemView.getContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_avatar)
                                .into(contactAvatar);
                        } else {
                            contactAvatar.setImageResource(R.drawable.ic_avatar);
                        }
                        
                        // Set phone number in header (if available)
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            contactPhone.setText(phoneNumber);
                            contactPhone.setVisibility(View.VISIBLE);
                        } else {
                            contactPhone.setVisibility(View.GONE);
                        }
                        
                        // Setup action buttons
                        setupActionButtons(contactUserId, currentUserId, phoneNumber);
                        
                        // Set click listener on avatar/name to view profile
                        View.OnClickListener profileClickListener = v -> {
                            android.content.Intent intent = new android.content.Intent(
                                itemView.getContext(),
                                com.example.doan_zaloclone.ui.personal.ProfileCardActivity.class
                            );
                            intent.putExtra(com.example.doan_zaloclone.ui.personal.ProfileCardActivity.EXTRA_USER_ID, 
                                contactUserId);
                            intent.putExtra(com.example.doan_zaloclone.ui.personal.ProfileCardActivity.EXTRA_IS_EDITABLE, 
                                false);
                            itemView.getContext().startActivity(intent);
                        };
                        
                        contactAvatar.setOnClickListener(profileClickListener);
                        contactName.setOnClickListener(profileClickListener);
                    } else {
                        contactName.setText("Người dùng không tồn tại");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ContactMessageViewHolder", "ERROR fetching contact user: " + e.getMessage(), e);
                    contactName.setText("Lỗi tải thông tin");
                });
        }
        
        /**
         * Setup Call and Message buttons
         */
        private void setupActionButtons(String contactUserId, String currentUserId, String phoneNumber) {
            // Setup "Gọi điện" button - always enabled, shows options dialog
            btnCallFromCard.setAlpha(1.0f);
            btnCallFromCard.setOnClickListener(v -> {
                showCallOptionsDialog(contactUserId, phoneNumber);
            });
            
            
            // Setup "Nhắn tin" button - always enabled
            btnMessageFromCard.setOnClickListener(v -> {
                // Open conversation with contact user
                com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                if (auth.getCurrentUser() == null) {
                    android.widget.Toast.makeText(itemView.getContext(), 
                        "Không thể mở cuộc trò chuyện", 
                        android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String loggedInUserId = auth.getCurrentUser().getUid();
                
                // Create or open conversation with contact user
                com.example.doan_zaloclone.repository.ChatRepository chatRepo = 
                    new com.example.doan_zaloclone.repository.ChatRepository();
                
                chatRepo.getOrCreateConversationWithFriend(loggedInUserId, contactUserId, 
                    new com.example.doan_zaloclone.repository.ChatRepository.ConversationCallback() {
                        @Override
                        public void onSuccess(String conversationId) {
                            // Navigate to conversation
                            android.content.Intent intent = new android.content.Intent(
                            itemView.getContext(),
                                com.example.doan_zaloclone.ui.room.RoomActivity.class
                            );
                            intent.putExtra("conversationId", conversationId);
                            itemView.getContext().startActivity(intent);
                        }
                        
                        @Override
                        public void onError(String error) {
                            android.widget.Toast.makeText(itemView.getContext(), 
                                "Không thể mở cuộc trò chuyện: " + error, 
                                android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
            });
            
            // Check friendship status for friend request button
            setupFriendshipListener(contactUserId, currentUserId);
        }
        
        /**
         * Setup real-time listeners for friendship and friend request status
         * With caching, loading state, and retry mechanism
         */
        private void setupFriendshipListener(String contactUserId, String currentUserId) {
            String cacheKey = currentUserId + "_" + contactUserId;
            
            // Check cache first
            FriendshipStatus cachedStatus = friendshipCache.get(cacheKey);
            if (cachedStatus != null && !cachedStatus.isExpired()) {
                android.util.Log.d("ContactMessageViewHolder", "Using cached status for: " + cacheKey);
                updateUIFromCache(cachedStatus);
                // Still setup listeners for real-time updates, but don't show loading
                setupListenersWithRetry(contactUserId, currentUserId, cacheKey, false);
                return;
            }
            
            // Show loading state
            isLoadingFriendshipStatus = true;
            showLoadingState();
            
            // Setup listeners with retry mechanism
            setupListenersWithRetry(contactUserId, currentUserId, cacheKey, true);
        }
        
        /**
         * Setup listeners with automatic retry on failure
         */
        private void setupListenersWithRetry(String contactUserId, String currentUserId, 
                                             String cacheKey, boolean isInitialLoad) {
            com.google.firebase.firestore.FirebaseFirestore db = 
                com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            final String[] requestStatusFromMe = {null};
            final String[] requestStatusToMe = {null};
            
            // Helper to update UI and cache
            Runnable updateUIAndCache = () -> {
                if (!contactUserId.equals(this.currentContactUserId)) return;
                
                // Update cache
                FriendshipStatus newStatus = new FriendshipStatus(
                    requestStatusFromMe[0], requestStatusToMe[0]
                );
                friendshipCache.put(cacheKey, newStatus);
                
                // Hide loading if this was initial load
                if (isInitialLoad && isLoadingFriendshipStatus) {
                    isLoadingFriendshipStatus = false;
                    hideLoadingState();
                }
                
                // Reset retry count on successful update
                listenerRetryCount = 0;
                
                // Update UI
                boolean areFriends = "ACCEPTED".equals(requestStatusFromMe[0]) || 
                                    "ACCEPTED".equals(requestStatusToMe[0]);
                boolean hasPendingRequest = "PENDING".equals(requestStatusFromMe[0]);
                boolean hasIncomingRequest = "PENDING".equals(requestStatusToMe[0]);
                
                if (areFriends) {
                    btnAddFriendFromCard.setVisibility(View.GONE);
                    friendRequestDivider.setVisibility(View.GONE);
                    android.util.Log.d("ContactMessageViewHolder", "Already friends - hiding button");
                } else if (hasPendingRequest) {
                    btnAddFriendFromCard.setVisibility(View.VISIBLE);
                    friendRequestDivider.setVisibility(View.VISIBLE);
                    btnAddFriendText.setText("ĐÃ GỬI LỜI MỜI");
                    btnAddFriendFromCard.setEnabled(false);
                    btnAddFriendFromCard.setAlpha(0.6f);
                    btnAddFriendFromCard.setOnClickListener(null);
                    android.util.Log.d("ContactMessageViewHolder", "Pending request - showing disabled button");
                } else if (hasIncomingRequest) {
                    btnAddFriendFromCard.setVisibility(View.VISIBLE);
                    friendRequestDivider.setVisibility(View.VISIBLE);
                    btnAddFriendText.setText("ĐANG CHỜ PHẢN HỒI");
                    btnAddFriendFromCard.setEnabled(false);
                    btnAddFriendFromCard.setAlpha(0.6f);
                    btnAddFriendFromCard.setOnClickListener(null);
                    android.util.Log.d("ContactMessageViewHolder", "Incoming request - showing waiting button");
                } else {
                    btnAddFriendFromCard.setVisibility(View.VISIBLE);
                    friendRequestDivider.setVisibility(View.VISIBLE);
                    btnAddFriendText.setText("GỬI LỜI MỜI KẾT BẠN");
                    btnAddFriendFromCard.setEnabled(true);
                    btnAddFriendFromCard.setAlpha(1.0f);
                    btnAddFriendFromCard.setOnClickListener(v -> sendFriendRequest(contactUserId, currentUserId));
                    android.util.Log.d("ContactMessageViewHolder", "No relationship - showing send button");
                }
            };
            
            // Error handler with retry logic
            java.util.function.Consumer<Exception> handleError = (error) -> {
                android.util.Log.e("ContactMessageViewHolder", "Listener error, attempt: " + (listenerRetryCount + 1), error);
                
                if (listenerRetryCount < MAX_RETRY_ATTEMPTS) {
                    listenerRetryCount++;
                    long retryDelay = (long) Math.pow(2, listenerRetryCount) * 1000; // Exponential backoff
                    
                    android.util.Log.d("ContactMessageViewHolder", "Retrying in " + retryDelay + "ms");
                    retryHandler.postDelayed(() -> {
                        if (contactUserId.equals(this.currentContactUserId)) {
                            setupListenersWithRetry(contactUserId, currentUserId, cacheKey, isInitialLoad);
                        }
                    }, retryDelay);
                } else {
                    android.util.Log.e("ContactMessageViewHolder", "Max retry attempts reached");
                    if (isLoadingFriendshipStatus) {
                        isLoadingFriendshipStatus = false;
                        hideLoadingState();
                        showErrorState();
                    }
                }
            };
            
            // Listen to outgoing requests
            try {
                friendRequestListener = db.collection("friendRequests")
                    .whereEqualTo("fromUserId", currentUserId)
                    .whereEqualTo("toUserId", contactUserId)
                    .addSnapshotListener((snapshots, error) -> {
                        if (!contactUserId.equals(this.currentContactUserId)) return;
                        
                        if (error != null) {
                            handleError.accept(error);
                            return;
                        }
                        
                        if (snapshots != null && !snapshots.isEmpty()) {
                            String bestStatus = findBestStatus(snapshots.getDocuments());
                            requestStatusFromMe[0] = bestStatus;
                            android.util.Log.d("ContactMessageViewHolder", "Outgoing status: " + bestStatus);
                        } else {
                            requestStatusFromMe[0] = null;
                        }
                        updateUIAndCache.run();
                    });
            } catch (Exception e) {
                handleError.accept(e);
                return;
            }
            
            // Listen to incoming requests
            try {
                friendsListener = db.collection("friendRequests")
                    .whereEqualTo("fromUserId", contactUserId)
                    .whereEqualTo("toUserId", currentUserId)
                    .addSnapshotListener((snapshots, error) -> {
                        if (!contactUserId.equals(this.currentContactUserId)) return;
                        
                        if (error != null) {
                            handleError.accept(error);
                            return;
                        }
                        
                        if (snapshots != null && !snapshots.isEmpty()) {
                            String bestStatus = findBestStatus(snapshots.getDocuments());
                            requestStatusToMe[0] = bestStatus;
                            android.util.Log.d("ContactMessageViewHolder", "Incoming status: " + bestStatus);
                        } else {
                            requestStatusToMe[0] = null;
                        }
                        updateUIAndCache.run();
                    });
            } catch (Exception e) {
                handleError.accept(e);
            }
        }
        
        /**
         * Find best status from documents: ACCEPTED > PENDING > others
         */
        private String findBestStatus(java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs) {
            String bestStatus = null;
            for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                String status = doc.getString("status");
                if ("ACCEPTED".equals(status)) {
                    return "ACCEPTED"; // Can't get better
                } else if ("PENDING".equals(status)) {
                    bestStatus = "PENDING";
                } else if (bestStatus == null) {
                    bestStatus = status;
                }
            }
            return bestStatus;
        }
        
        /**
         * Update UI from cached status
         */
        private void updateUIFromCache(FriendshipStatus status) {
            if (status.areFriends()) {
                btnAddFriendFromCard.setVisibility(View.GONE);
                friendRequestDivider.setVisibility(View.GONE);
            } else if ("PENDING".equals(status.statusFromMe)) {
                btnAddFriendFromCard.setVisibility(View.VISIBLE);
                friendRequestDivider.setVisibility(View.VISIBLE);
                btnAddFriendText.setText("ĐÃ GỬI LỜI MỜI");
                btnAddFriendFromCard.setEnabled(false);
                btnAddFriendFromCard.setAlpha(0.6f);
            } else if ("PENDING".equals(status.statusToMe)) {
                btnAddFriendFromCard.setVisibility(View.VISIBLE);
                friendRequestDivider.setVisibility(View.VISIBLE);
                btnAddFriendText.setText("ĐANG CHỜ PHẢN HỒI");
                btnAddFriendFromCard.setEnabled(false);
                btnAddFriendFromCard.setAlpha(0.6f);
            } else {
                btnAddFriendFromCard.setVisibility(View.VISIBLE);
                friendRequestDivider.setVisibility(View.VISIBLE);
                btnAddFriendText.setText("GỬI LỜI MỜI KẾT BẠN");
                btnAddFriendFromCard.setEnabled(true);
                btnAddFriendFromCard.setAlpha(1.0f);
            }
        }
        
        /**
         * Show loading state (simplified - just dim the button)  
         */
        private void showLoadingState() {
            btnAddFriendFromCard.setVisibility(View.VISIBLE);
            friendRequestDivider.setVisibility(View.VISIBLE);
            btnAddFriendText.setText("ĐANG TẢI...");
            btnAddFriendFromCard.setEnabled(false);
            btnAddFriendFromCard.setAlpha(0.5f);
        }
        
        /**
         * Hide loading state
         */
        private void hideLoadingState() {
            // UI will be updated by updateUIAndCache
        }
        
        /**
         * Show error state
         */
        private void showErrorState() {
            btnAddFriendFromCard.setVisibility(View.VISIBLE);
            friendRequestDivider.setVisibility(View.VISIBLE);
            btnAddFriendText.setText("LỖI KẾT NỐI");
            btnAddFriendFromCard.setEnabled(false);
            btnAddFriendFromCard.setAlpha(0.5f);
        }
        
        /**
         * Show call options bottom sheet dialog
         */
        private void showCallOptionsDialog(String contactUserId, String phoneNumber) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Chọn cách gọi");
            
            // Prepare options
            java.util.List<String> options = new java.util.ArrayList<>();
            java.util.List<Runnable> actions = new java.util.ArrayList<>();
            
            // Option 1: Phone call (if phone number available)
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                options.add("📞 Gọi qua số điện thoại");
                actions.add(() -> makePhoneCall(phoneNumber));
            }
            
            // Option 2: Video call via app (always available)
            options.add("📹 Gọi video qua ứng dụng");
            actions.add(() -> makeAppCall(contactUserId, true));
            
            // Option 3: Voice call via app (always available)
            options.add("📞 Gọi thoại qua ứng dụng");
            actions.add(() -> makeAppCall(contactUserId, false));
            
            // Convert to array
            String[] optionsArray = options.toArray(new String[0]);
            
            builder.setItems(optionsArray, (dialog, which) -> {
                if (which >= 0 && which < actions.size()) {
                    actions.get(which).run();
                }
            });
            
            builder.setNegativeButton("Hủy", null);
            builder.show();
        }
        
        /**
         * Make phone call using system dialer
         */
        private void makePhoneCall(String phoneNumber) {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
                itemView.getContext().startActivity(intent);
            } catch (Exception e) {
                android.widget.Toast.makeText(itemView.getContext(),
                    "Không thể thực hiện cuộc gọi",
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        
        /**
         * Make in-app call (video or voice)
         * Navigates to conversation and auto-triggers call
         */
        private void makeAppCall(String contactUserId, boolean isVideo) {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                android.widget.Toast.makeText(itemView.getContext(),
                    "Không thể thực hiện cuộc gọi",
                    android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            String currentUserId = auth.getCurrentUser().getUid();
            
            // Show loading
            android.widget.Toast.makeText(itemView.getContext(),
                "Đang kết nối...",
                android.widget.Toast.LENGTH_SHORT).show();
            
            // Get or create conversation first
            com.example.doan_zaloclone.repository.ChatRepository chatRepo = 
                new com.example.doan_zaloclone.repository.ChatRepository();
            
            chatRepo.getOrCreateConversationWithFriend(currentUserId, contactUserId, 
                new com.example.doan_zaloclone.repository.ChatRepository.ConversationCallback() {
                    @Override
                    public void onSuccess(String conversationId) {
                        // Navigate to RoomActivity with auto-start call flag
                        android.content.Intent intent = new android.content.Intent(
                            itemView.getContext(),
                            com.example.doan_zaloclone.ui.room.RoomActivity.class
                        );
                        intent.putExtra("conversationId", conversationId);
                        intent.putExtra(com.example.doan_zaloclone.ui.room.RoomActivity.EXTRA_AUTO_START_CALL, true);
                        intent.putExtra(com.example.doan_zaloclone.ui.room.RoomActivity.EXTRA_IS_VIDEO_CALL, isVideo);
                        itemView.getContext().startActivity(intent);
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.widget.Toast.makeText(itemView.getContext(),
                            "Không thể kết nối: " + error,
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        }
        
        /**
         * Send friend request to contact user
         */
        private void sendFriendRequest(String contactUserId, String currentUserId) {
            // Fetch current user's name
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String currentUserName = "Người dùng";
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            currentUserName = name;
                        }
                    }
                    
                    // Send friend request
                    com.example.doan_zaloclone.repository.FriendRepository friendRepo = 
                        new com.example.doan_zaloclone.repository.FriendRepository();
                    
                    String finalCurrentUserName = currentUserName;
                        friendRepo.sendFriendRequest(currentUserId, contactUserId, finalCurrentUserName)
                        .observeForever(resource -> {
                            if (resource != null) {
                                if (resource.isSuccess()) {
                                    // Invalidate cache to ensure fresh data
                                    String cacheKey = currentUserId + "_" + contactUserId;
                                    friendshipCache.remove(cacheKey);
                                    
                                    android.widget.Toast.makeText(itemView.getContext(), 
                                        "Đã gửi lời mời kết bạn", 
                                        android.widget.Toast.LENGTH_SHORT).show();
                                    // Update UI to show request sent
                                    btnAddFriendText.setText("ĐÃ GỬI LỜI MỜI");
                                    btnAddFriendFromCard.setEnabled(false);
                                    btnAddFriendFromCard.setAlpha(0.6f);
                                } else if (resource.isError()) {
                                    android.widget.Toast.makeText(itemView.getContext(), 
                                        "Lỗi: " + resource.getMessage(), 
                                        android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(itemView.getContext(), 
                        "Không thể gửi lời mời kết bạn", 
                        android.widget.Toast.LENGTH_SHORT).show();
                });
        }
    }
    
    // ================ LOCATION MESSAGE VIEW HOLDER ================
    
    static class LocationMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView locationNameText;
        private TextView locationAddressText;
        private TextView coordinatesText;
        private TextView timestampTextView;
        private View messageCard;
        
        public LocationMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            locationNameText = itemView.findViewById(R.id.locationNameText);
            locationAddressText = itemView.findViewById(R.id.locationAddressText);
            coordinatesText = itemView.findViewById(R.id.coordinatesText);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            messageCard = itemView.findViewById(R.id.messageCard);
        }
        
        public void bind(Message message) {
            // Set location name
            String locationName = message.getLocationName();
            if (locationName != null && !locationName.isEmpty()) {
                locationNameText.setText(locationName);
            } else {
                locationNameText.setText("Vị trí");
            }
            
            // Set address
            String address = message.getLocationAddress();
            if (address != null && !address.isEmpty()) {
                locationAddressText.setText(address);
                locationAddressText.setVisibility(View.VISIBLE);
            } else {
                locationAddressText.setVisibility(View.GONE);
            }
            
            // Set coordinates
            double lat = message.getLatitude();
            double lng = message.getLongitude();
            coordinatesText.setText(String.format(java.util.Locale.getDefault(), 
                    "📍 %.4f, %.4f", lat, lng));
            
            // Set timestamp
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Set click listener to open Google Maps
            messageCard.setOnClickListener(v -> {
                android.net.Uri gmmIntentUri = android.net.Uri.parse(
                        "geo:" + lat + "," + lng + "?q=" + lat + "," + lng);
                android.content.Intent mapIntent = new android.content.Intent(
                        android.content.Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                
                if (mapIntent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(mapIntent);
                } else {
                    // Fallback to browser
                    android.net.Uri browserAndriod = android.net.Uri.parse(
                            "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                    android.content.Intent browserIntent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW, browserAndriod);
                    itemView.getContext().startActivity(browserIntent);
                }
            });
            
            // Set timestamp
            if (timestampTextView != null) {
                timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            }
        }
    }
    
    // ================ LIVE LOCATION MESSAGE VIEW HOLDER ================
    
    static class LiveLocationMessageViewHolder extends RecyclerView.ViewHolder {
        private MapView mapView;
        private TextView statusText;
        private TextView timestampTextView;
        private TextView btnStopSharing;
        private View messageCard;
        private View mapOverlay;
        private ListenerRegistration liveLocationListener;
        private CountDownTimer countDownTimer;
        private Marker userMarker;
        private boolean isSender;
        
        public LiveLocationMessageViewHolder(@NonNull View itemView, boolean isSender) {
            super(itemView);
            this.isSender = isSender;
            mapView = itemView.findViewById(R.id.mapView);
            statusText = itemView.findViewById(R.id.statusText);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            btnStopSharing = itemView.findViewById(R.id.btnStopSharing); // Only present in sent layout
            messageCard = itemView.findViewById(R.id.messageCard);
            mapOverlay = itemView.findViewById(R.id.mapOverlay);
            
            // Ensure overlay is visible to block map touch and capture click
            if (mapOverlay != null) {
                mapOverlay.setVisibility(View.VISIBLE);
            }
            
            // Basic Map Setup
            setupMap();
        }
        
        private void setupMap() {
            Configuration.getInstance().setUserAgentValue(itemView.getContext().getPackageName());
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            
            // Disable interaction for preview (User wanted separate full screen view)
            mapView.setMultiTouchControls(false); 
            mapView.setClickable(false); 
            
            mapView.getController().setZoom(15.0);
            mapView.setOnTouchListener(null);
            
            // Performance settings
            mapView.setUseDataConnection(true);
        }
        
        public void bind(Message message, String currentUserId) {
            String sessionId = message.getLiveLocationSessionId();
            if (sessionId == null) return;
            
            // Cleanup previous listener
            cleanup();
            
            // Listen to Live Location updates
            liveLocationListener = FirebaseFirestore.getInstance()
                    .collection("liveLocations")
                    .document(sessionId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) {
                            statusText.setText("Phiên chia sẻ đã kết thúc");
                            if (btnStopSharing != null) btnStopSharing.setVisibility(View.GONE);
                            return;
                        }
                        
                        LiveLocation liveLocation = snapshot.toObject(LiveLocation.class);
                        if (liveLocation != null) {
                            updateUI(liveLocation, message, currentUserId);
                        }
                    });
            
            // Handle Stop Sharing button
            if (btnStopSharing != null && isSender) {
                btnStopSharing.setOnClickListener(v -> {
                    // Update Firestore
                    new ChatRepository().stopLiveLocation(sessionId);
                    
                    // Stop Background Service
                    Intent intent = new Intent(itemView.getContext(), LocationSharingService.class);
                    intent.setAction(LocationSharingService.ACTION_STOP_SHARING);
                    itemView.getContext().startService(intent);
                });
            }
             
            // Set timestamp
            if (timestampTextView != null) {
                timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            }
            
            // Handle view detachment to cleanup listener
            itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {}
                @Override
                public void onViewDetachedFromWindow(View v) {
                    cleanup();
                }
            });
        }
        
        private void updateUI(LiveLocation liveLocation, Message message, String currentUserId) {
            // Update Map Marker
            GeoPoint point = new GeoPoint(liveLocation.getLatitude(), liveLocation.getLongitude());
            if (userMarker == null) {
                userMarker = new Marker(mapView);
                userMarker.setTitle("Vị trí trực tiếp");
                mapView.getOverlays().add(userMarker);
            }
            userMarker.setPosition(point);
            mapView.getController().setCenter(point);
            mapView.invalidate();
            
            // Define click action for Map/View Button -> Open Activity
            View.OnClickListener openMapAction = v -> {
                 Intent intent = new Intent(itemView.getContext(), LiveLocationViewActivity.class);
                 intent.putExtra(LiveLocationViewActivity.EXTRA_SESSION_ID, liveLocation.getSessionId());
                 intent.putExtra(LiveLocationViewActivity.EXTRA_IS_SENDER, isSender);
                 itemView.getContext().startActivity(intent);
            };
            
            messageCard.setOnClickListener(openMapAction);
            if (mapOverlay != null) mapOverlay.setOnClickListener(openMapAction);
            
            // Check active status
            boolean isActive = liveLocation.isActive();
            long remainingTime = liveLocation.getEndTime() - System.currentTimeMillis();
            
            if (!isActive || remainingTime <= 0) {
                statusText.setText("Đã dừng chia sẻ");
                if (isSender) {
                    if (btnStopSharing != null) btnStopSharing.setVisibility(View.GONE);
                } else {
                    // Receiver: Show View Location button even if ended
                    if (btnStopSharing != null) {
                        btnStopSharing.setVisibility(View.VISIBLE);
                        btnStopSharing.setText("Xem vị trí");
                        btnStopSharing.setOnClickListener(openMapAction);
                    }
                }
                cleanupTimer();
            } else {
                if (isSender) {
                    if (btnStopSharing != null) {
                        btnStopSharing.setVisibility(View.VISIBLE);
                        btnStopSharing.setText("Dừng chia sẻ");
                        // Note: Stop action is set in bind()
                    }
                } else {
                    if (btnStopSharing != null) {
                        btnStopSharing.setVisibility(View.VISIBLE);
                        btnStopSharing.setText("Xem vị trí");
                        btnStopSharing.setOnClickListener(openMapAction);
                    }
                }
                startTimer(remainingTime);
            }
        }
        
        private void startTimer(long durationMillis) {
            cleanupTimer();
            if (durationMillis <= 0) return;
            
            countDownTimer = new CountDownTimer(durationMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long minutes = millisUntilFinished / 1000 / 60;
                    long seconds = (millisUntilFinished / 1000) % 60;
                    statusText.setText(String.format("Đang chia sẻ • Còn %02d:%02d", minutes, seconds));
                }
                @Override
                public void onFinish() {
                    statusText.setText("Đã hết thời gian chia sẻ");
                    if (btnStopSharing != null) btnStopSharing.setVisibility(View.GONE);
                }
            }.start();
        }
        
        private void cleanupTimer() {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
        }
        
        private void cleanup() {
            if (liveLocationListener != null) {
                liveLocationListener.remove();
                liveLocationListener = null;
            }
            cleanupTimer();
        }
    }
}
