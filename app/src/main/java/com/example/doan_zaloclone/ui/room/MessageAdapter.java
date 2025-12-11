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
    
    // Static SimpleDateFormat to avoid recreation in bind()
    private static final SimpleDateFormat TIMESTAMP_FORMAT = 
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);
        boolean isImage = Message.TYPE_IMAGE.equals(message.getType());
        
        if (isSent) {
            return isImage ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_SENT;
        } else {
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
            default:
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ImageSentViewHolder) {
            ((ImageSentViewHolder) holder).bind(message);
        } else if (holder instanceof ImageReceivedViewHolder) {
            ((ImageReceivedViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(this.messages, newMessages));
        this.messages = newMessages;
        diffResult.dispatchUpdatesTo(this);
    }

    public List<Message> getMessages() {
        return messages;
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;
        private TextView timestampTextView;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message) {
            messageTextView.setText(message.getContent());
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;
        private TextView timestampTextView;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message) {
            messageTextView.setText(message.getContent());
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
        }
    }
    
    static class ImageSentViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImageView;
        private TextView timestampTextView;

        public ImageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message) {
            Glide.with(itemView.getContext())
                    .load(message.getContent())
                    .into(messageImageView);
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
        }
    }
    
    static class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImageView;
        private TextView timestampTextView;

        public ImageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }

        public void bind(Message message) {
            Glide.with(itemView.getContext())
                    .load(message.getContent())
                    .into(messageImageView);
            timestampTextView.setText(TIMESTAMP_FORMAT.format(new Date(message.getTimestamp())));
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
}
