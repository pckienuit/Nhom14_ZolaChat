package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
    private static final int VIEW_TYPE_FILE_SENT = 5;
    private static final int VIEW_TYPE_FILE_RECEIVED = 6;
    private static final int VIEW_TYPE_CALL_HISTORY = 7;
    
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
    
    private OnMessageLongClickListener longClickListener;

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
        boolean isSent = message.getSenderId().equals(currentUserId);
        boolean isImage = Message.TYPE_IMAGE.equals(message.getType());
        boolean isFile = Message.TYPE_FILE.equals(message.getType());
        boolean isCall = Message.TYPE_CALL.equals(message.getType());
        
        // Call history messages are always centered
        if (isCall) {
            return VIEW_TYPE_CALL_HISTORY;
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
            ((SentMessageViewHolder) holder).bind(message, longClickListener, isPinned, isHighlighted);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, isGroupChat, longClickListener, isPinned, isHighlighted);
        } else if (holder instanceof ImageSentViewHolder) {
            ((ImageSentViewHolder) holder).bind(message, longClickListener, isPinned, isHighlighted);
        } else if (holder instanceof ImageReceivedViewHolder) {
            ((ImageReceivedViewHolder) holder).bind(message, isGroupChat, longClickListener, isPinned, isHighlighted);
        } else if (holder instanceof FileMessageSentViewHolder) {
            ((FileMessageSentViewHolder) holder).bind(message, longClickListener, isPinned, isHighlighted);
        } else if (holder instanceof FileMessageReceivedViewHolder) {
            ((FileMessageReceivedViewHolder) holder).bind(message, longClickListener, isPinned, isHighlighted);
        } else if (holder instanceof CallHistoryViewHolder) {
            ((CallHistoryViewHolder) holder).bind(message);
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
    
    private static void showMessageContextMenu(View view, Message message, OnMessageLongClickListener listener, boolean isPinned) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(view.getContext(), view);
        
        // Only show relevant option based on pin status
        if (isPinned) {
            popup.getMenu().add(0, 2, 0, "📌 Bỏ ghim tin nhắn");
        } else {
            popup.getMenu().add(0, 1, 0, "📌 Ghim tin nhắn");
        }
        
        popup.setOnMenuItemClickListener(item -> {
            if (listener == null) return false;
            
            int itemId = item.getItemId();
            if (itemId == 1) {
                listener.onPinMessage(message);
                return true;
            } else if (itemId == 2) {
                listener.onUnpinMessage(message);
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
        // Find or create pin indicator
        ImageView pinIndicator = itemView.findViewById(R.id.pinIndicator);
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

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;
        private TextView timestampTextView;
        private OnMessageLongClickListener listener;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message, OnMessageLongClickListener listener, boolean isPinned, boolean isHighlighted) {
            this.listener = listener;
            messageTextView.setText(message.getContent());
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, isPinned);
                return true;
            });
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;
        private TextView timestampTextView;
        private TextView senderNameTextView;
        private OnMessageLongClickListener listener;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
        }

        public void bind(Message message, boolean isGroupChat, OnMessageLongClickListener listener, boolean isPinned, boolean isHighlighted) {
            this.listener = listener;
            messageTextView.setText(message.getContent());
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Show sender name only in group chats
            if (senderNameTextView != null) {
                if (isGroupChat) {
                    senderNameTextView.setVisibility(View.VISIBLE);
                    // Fetch sender name from Firestore
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
                } else {
                    senderNameTextView.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, isPinned);
                return true;
            });
        }
    }
    
    static class ImageSentViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImageView;
        private TextView timestampTextView;
        private OnMessageLongClickListener listener;

        public ImageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message, OnMessageLongClickListener listener, boolean isPinned, boolean isHighlighted) {
            this.listener = listener;
            Glide.with(itemView.getContext())
                    .load(message.getContent())
                    .into(messageImageView);
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, isPinned);
                return true;
            });
        }
    }
    
    static class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImageView;
        private TextView timestampTextView;
        private TextView senderNameTextView;
        private OnMessageLongClickListener listener;

        public ImageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
        }

        public void bind(Message message, boolean isGroupChat, OnMessageLongClickListener listener, boolean isPinned, boolean isHighlighted) {
            this.listener = listener;
            Glide.with(itemView.getContext())
                    .load(message.getContent())
                    .into(messageImageView);
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Show sender name only in group chats
            if (senderNameTextView != null) {
                if (isGroupChat) {
                    senderNameTextView.setVisibility(View.VISIBLE);
                    // Fetch sender name from Firestore
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
                } else {
                    senderNameTextView.setVisibility(View.GONE);
                }
            }
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, isPinned);
                return true;
            });
        }
    }
    
    static class FileMessageSentViewHolder extends RecyclerView.ViewHolder {
        private ImageView fileIcon;
        private TextView fileName;
        private TextView fileSize;
        private TextView timestampTextView;
        private OnMessageLongClickListener listener;

        public FileMessageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message, OnMessageLongClickListener listener, boolean isPinned, boolean isHighlighted) {
            this.listener = listener;
            fileName.setText(message.getFileName());
            
            // Display "FileType - Size" format
            String fileType = com.example.doan_zaloclone.utils.FileTypeUtils.getFileTypeLabel(message.getFileMimeType());
            String sizeFormatted = message.getFormattedFileSize();
            fileSize.setText(fileType + " - " + sizeFormatted);
            
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Set appropriate icon based on file type
            int iconResId = com.example.doan_zaloclone.utils.FileUtils.getFileIcon(message.getFileMimeType());
            fileIcon.setImageResource(iconResId);
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            // Add click listener to open file
            itemView.setOnClickListener(v -> openFile(message));
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, isPinned);
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
        private OnMessageLongClickListener listener;

        public FileMessageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message, OnMessageLongClickListener listener, boolean isPinned, boolean isHighlighted) {
            this.listener = listener;
            fileName.setText(message.getFileName());
            
            // Display "FileType - Size" format
            String fileType = com.example.doan_zaloclone.utils.FileTypeUtils.getFileTypeLabel(message.getFileMimeType());
            String sizeFormatted = message.getFormattedFileSize();
            fileSize.setText(fileType + " - " + sizeFormatted);
            
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
            
            // Set appropriate icon based on file type
            int iconResId = com.example.doan_zaloclone.utils.FileUtils.getFileIcon(message.getFileMimeType());
            fileIcon.setImageResource(iconResId);
            
            applyPinAndHighlight(itemView, isPinned, isHighlighted);
            
            // Add click listener to open file
            itemView.setOnClickListener(v -> openFile(message));
            
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, listener, isPinned);
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
            
            // Compare all relevant fields
            return oldMessage.getContent().equals(newMessage.getContent()) &&
                   oldMessage.getSenderId().equals(newMessage.getSenderId()) &&
                   oldMessage.getTimestamp() == newMessage.getTimestamp() &&
                   oldMessage.getType().equals(newMessage.getType());
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
}
