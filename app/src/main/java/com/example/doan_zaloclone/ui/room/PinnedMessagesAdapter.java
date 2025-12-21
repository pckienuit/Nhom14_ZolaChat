package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying pinned messages in RecyclerView
 */
public class PinnedMessagesAdapter extends RecyclerView.Adapter<PinnedMessagesAdapter.PinnedMessageViewHolder> {

    private List<Message> pinnedMessages = new ArrayList<>();
    private OnPinnedMessageClickListener listener;

    public interface OnPinnedMessageClickListener {
        void onPinnedMessageClick(Message message);
        void onUnpinClick(Message message);
    }

    public PinnedMessagesAdapter(OnPinnedMessageClickListener listener) {
        this.listener = listener;
    }

    public void setPinnedMessages(List<Message> messages) {
        this.pinnedMessages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PinnedMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pinned_message, parent, false);
        return new PinnedMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinnedMessageViewHolder holder, int position) {
        Message message = pinnedMessages.get(position);
        holder.bind(message, listener);
    }

    @Override
    public int getItemCount() {
        return pinnedMessages.size();
    }

    static class PinnedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView senderName;
        private TextView messageContent;
        private ImageButton unpinButton;

        public PinnedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.pinnedSenderName);
            messageContent = itemView.findViewById(R.id.pinnedMessageContent);
            unpinButton = itemView.findViewById(R.id.unpinButton);
        }

        public void bind(Message message, OnPinnedMessageClickListener listener) {
            // Try to get sender name from message first (optimized)
            String senderId = message.getSenderId();
            String cachedName = message.getSenderName();
            
            if (cachedName != null && !cachedName.isEmpty()) {
                // Use cached name from message
                senderName.setText(cachedName);
            } else {
                // Fallback: Fetch from Firestore for old messages without senderName
                senderName.setText("Loading...");
                
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                senderName.setText(name);
                            } else {
                                senderName.setText("User");
                            }
                        } else {
                            senderName.setText("User");
                        }
                    })
                    .addOnFailureListener(e -> {
                        senderName.setText("User");
                    });
            }

            // Set message content based on type
            String content = getMessagePreview(message);
            messageContent.setText(content);

            // Click listener for navigating to message
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPinnedMessageClick(message);
                }
            });

            // Unpin button click listener
            unpinButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnpinClick(message);
                }
            });
        }

        private String getMessagePreview(Message message) {
            if (message == null) return "";

            switch (message.getType()) {
                case Message.TYPE_IMAGE:
                    return "📷 Hình ảnh";
                case Message.TYPE_FILE:
                    // Extract filename if available
                    String fileName = message.getFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        return "📎 " + fileName;
                    }
                    return "📎 File";
                case Message.TYPE_CALL:
                    return "📞 Cuộc gọi";
                case Message.TYPE_TEXT:
                default:
                    return message.getContent() != null ? message.getContent() : "";
            }
        }
    }
}
