package com.example.doan_zaloclone.repository;

import com.example.doan_zaloclone.models.Message;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for handling chat/messaging operations with Firestore
 */
public class ChatRepository {

    private final FirebaseFirestore firestore;

    public ChatRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Send a message to a conversation
     * @param conversationId ID of the conversation
     * @param message Message object to send (ID will be auto-generated)
     * @param callback Callback for success/error
     */
    public void sendMessage(String conversationId, Message message, SendMessageCallback callback) {
        // Auto-generate message ID
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document();

        // Set the message ID
        message.setId(messageRef.getId());

        // Convert message to Map for Firestore
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", message.getId());
        messageData.put("senderId", message.getSenderId());
        messageData.put("content", message.getContent());
        messageData.put("type", message.getType());
        messageData.put("timestamp", message.getTimestamp());

        // Save message to Firestore
        messageRef.set(messageData)
                .addOnSuccessListener(aVoid -> {
                    // Update conversation's lastMessage
                    updateConversationLastMessage(conversationId, message.getContent(), message.getTimestamp());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to send message";
                    callback.onError(errorMessage);
                });
    }

    /**
     * Listen to messages in a conversation (real-time updates)
     * @param conversationId ID of the conversation
     * @param listener Listener for message updates
     * @return ListenerRegistration for cleanup
     */
    public ListenerRegistration listenToMessages(String conversationId, MessagesListener listener) {
        return firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage() != null ? error.getMessage() : "Failed to load messages");
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Message> messages = new ArrayList<>();
                        querySnapshot.forEach(document -> {
                            Message message = document.toObject(Message.class);
                            messages.add(message);
                        });
                        listener.onMessagesChanged(messages);
                    }
                });
    }

    /**
     * Update conversation's lastMessage and timestamp
     * @param conversationId ID of the conversation
     * @param lastMessage Content of the last message
     * @param timestamp Timestamp of the last message
     */
    private void updateConversationLastMessage(String conversationId, String lastMessage, long timestamp) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("timestamp", timestamp);

        firestore.collection("conversations")
                .document(conversationId)
                .update(updates)
                .addOnFailureListener(e -> {
                    // Log error but don't block message sending
                    // In production, you might want to handle this differently
                });
    }

    /**
     * Callback interface for send message operations
     */
    public interface SendMessageCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Listener interface for real-time message updates
     */
    public interface MessagesListener {
        void onMessagesChanged(List<Message> messages);
        void onError(String error);
    }
}
