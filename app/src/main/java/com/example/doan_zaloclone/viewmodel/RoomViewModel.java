package com.example.doan_zaloclone.viewmodel;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.repository.FriendRepository;
import com.example.doan_zaloclone.repository.UserRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

/**
 * ViewModel for RoomActivity
 * Manages chat messages, sending operations, and room state
 */
public class RoomViewModel extends BaseViewModel {
    
    private final ChatRepository chatRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    
    private LiveData<Resource<List<Message>>> messages;
    private final MutableLiveData<Resource<Boolean>> sendMessageState = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> uploadImageState = new MutableLiveData<>();
    
    public RoomViewModel() {
        this.chatRepository = new ChatRepository();
        this.friendRepository = new FriendRepository();
        this.userRepository = new UserRepository();
    }
    
    /**
     * Load messages for a conversation
     * @param conversationId ID of the conversation
     */
    public LiveData<Resource<List<Message>>> getMessages(@NonNull String conversationId) {
        if (messages == null) {
            messages = chatRepository.getMessagesLiveData(conversationId);
        }
        return messages;
    }
    
    /**
     * Send a text message
     * @param conversationId ID of the conversation
     * @param message Message object to send
     */
    public void sendMessage(@NonNull String conversationId, @NonNull Message message) {
        LiveData<Resource<Boolean>> result = chatRepository.sendMessageLiveData(conversationId, message);
        sendMessageState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(sendMessageState::setValue);
    }
    
    /**
     * Get send message state
     */
    public LiveData<Resource<Boolean>> getSendMessageState() {
        return sendMessageState;
    }
    
    /**
     * Upload and send an image message
     * @param conversationId ID of the conversation
     * @param imageUri URI of the image to upload
     * @param senderId ID of the sender
     */
    public void uploadAndSendImage(@NonNull String conversationId, 
                                    @NonNull Uri imageUri, 
                                    @NonNull String senderId) {
        LiveData<Resource<Boolean>> result = chatRepository.uploadImageAndSendMessageLiveData(
                conversationId, imageUri, senderId);
        uploadImageState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(uploadImageState::setValue);
    }
    
    /**
     * Get upload image state
     */
    public LiveData<Resource<Boolean>> getUploadImageState() {
        return uploadImageState;
    }
    
    /**
     * Check if two users are friends
     * @param userId1 First user ID
     * @param userId2 Second user ID
     */
    public LiveData<Resource<Boolean>> checkFriendship(@NonNull String userId1, 
                                                        @NonNull String userId2) {
        return friendRepository.checkFriendship(userId1, userId2);
    }
    
    /**
     * Get username by user ID
     * @param userId User ID
     */
    public LiveData<Resource<String>> getUserName(@NonNull String userId) {
        return userRepository.getUserName(userId);
    }
    
    // ===================== PINNED MESSAGES METHODS =====================
    
    private MutableLiveData<String> currentConversationId = new MutableLiveData<>();
    private MutableLiveData<Long> pinnedMessagesRefreshTrigger = new MutableLiveData<>(0L);
    private LiveData<Resource<List<Message>>> pinnedMessages;
    private final MutableLiveData<Resource<Boolean>> pinMessageState = new MutableLiveData<>();
    
    /**
     * Load pinned messages for a conversation
     * @param conversationId ID of the conversation
     * @return LiveData containing list of pinned messages
     */
    public LiveData<Resource<List<Message>>> getPinnedMessages(@NonNull String conversationId) {
        // Store conversation ID
        currentConversationId.setValue(conversationId);
        
        // Use switchMap to automatically reload when trigger changes
        if (pinnedMessages == null) {
            pinnedMessages = Transformations.switchMap(pinnedMessagesRefreshTrigger, trigger -> {
                String convId = currentConversationId.getValue();
                if (convId != null) {
                    return chatRepository.getPinnedMessages(convId);
                }
                return new MutableLiveData<>();
            });
        }
        return pinnedMessages;
    }
    
    /**
     * Reload pinned messages (useful after pin/unpin operations)
     */
    public void reloadPinnedMessages() {
        // Trigger switchMap by changing the value
        Long current = pinnedMessagesRefreshTrigger.getValue();
        pinnedMessagesRefreshTrigger.setValue(current != null ? current + 1 : 1L);
    }
    
    /**
     * Pin a message
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to pin
     */
    public void pinMessage(@NonNull String conversationId, @NonNull String messageId) {
        LiveData<Resource<Boolean>> result = chatRepository.pinMessage(conversationId, messageId);
        pinMessageState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(resource -> {
            pinMessageState.setValue(resource);
            // Reload pinned messages after successful pin
            if (resource != null && resource.isSuccess()) {
                reloadPinnedMessages();
            }
        });
    }
    
    /**
     * Unpin a message
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to unpin
     */
    public void unpinMessage(@NonNull String conversationId, @NonNull String messageId) {
        LiveData<Resource<Boolean>> result = chatRepository.unpinMessage(conversationId, messageId);
        pinMessageState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(resource -> {
            pinMessageState.setValue(resource);
            // Reload pinned messages after successful unpin
            if (resource != null && resource.isSuccess()) {
                reloadPinnedMessages();
            }
        });
    }
    
    /**
     * Get pin/unpin operation state
     */
    public LiveData<Resource<Boolean>> getPinMessageState() {
        return pinMessageState;
    }
    
    /**
     * Reset pin message state (call after handling the result)
     */
    public void resetPinMessageState() {
        pinMessageState.setValue(null);
    }
    
    // ===================== MESSAGE REACTIONS METHODS =====================
    
    private final MutableLiveData<Resource<Boolean>> reactionState = new MutableLiveData<>();
    
    /**
     * Add or update a reaction to a message
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to react to
     * @param userId ID of the user adding the reaction
     * @param reactionType Type of reaction (heart, haha, sad, angry, wow, like)
     */
    public void addReaction(@NonNull String conversationId,
                           @NonNull String messageId,
                           @NonNull String userId,
                           @NonNull String reactionType) {
        LiveData<Resource<Boolean>> result = chatRepository.addReaction(
                conversationId, messageId, userId, reactionType);
        reactionState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(reactionState::setValue);
    }
    
    /**
     * Remove a user's reaction from a message
     * @param conversationId ID of the conversation
     * @param messageId ID of the message
     * @param userId ID of the user removing their reaction
     */
    public void removeReaction(@NonNull String conversationId,
                              @NonNull String messageId,
                              @NonNull String userId) {
        LiveData<Resource<Boolean>> result = chatRepository.removeReaction(
                conversationId, messageId, userId);
        reactionState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(reactionState::setValue);
    }
    
    /**
     * Toggle a reaction - if user already has this reaction type, remove it; otherwise add/update it
     * @param conversationId ID of the conversation
     * @param messageId ID of the message
     * @param userId ID of the user toggling the reaction
     * @param reactionType Type of reaction to toggle
     */
    public void toggleReaction(@NonNull String conversationId,
                              @NonNull String messageId,
                              @NonNull String userId,
                              @NonNull String reactionType) {
        LiveData<Resource<Boolean>> result = chatRepository.toggleReaction(
                conversationId, messageId, userId, reactionType);
        reactionState.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(reactionState::setValue);
    }
    
    /**
     * Get reaction operation state
     */
    public LiveData<Resource<Boolean>> getReactionState() {
        return reactionState;
    }
    
    /**
     * Reset reaction state (call after handling the result)
     */
    public void resetReactionState() {
        reactionState.setValue(null);
    }
    
    /**
     * Reset send message state (call after handling the result)
     */
    public void resetSendMessageState() {
        sendMessageState.setValue(null);
    }
    
    /**
     * Reset upload image state (call after handling the result)
     */
    public void resetUploadImageState() {
        uploadImageState.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up repository listeners
        chatRepository.cleanup();
    }
}
