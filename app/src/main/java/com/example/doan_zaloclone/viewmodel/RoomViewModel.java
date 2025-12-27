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
    private final MutableLiveData<Resource<Boolean>> sendMessageState = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> uploadImageState = new MutableLiveData<>();
    private final MutableLiveData<String> currentConversationId = new MutableLiveData<>();
    private final MutableLiveData<Long> pinnedMessagesRefreshTrigger = new MutableLiveData<>(0L);
    private final MutableLiveData<Resource<Boolean>> pinMessageState = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> reactionState = new MutableLiveData<>();
    private LiveData<Resource<List<Message>>> messages;
    private LiveData<Resource<List<Message>>> pinnedMessages;

    public RoomViewModel() {
        this.chatRepository = ChatRepository.getInstance();
        this.friendRepository = new FriendRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Load messages for a conversation
     *
     * @param conversationId ID of the conversation
     */
    public LiveData<Resource<List<Message>>> getMessages(@NonNull String conversationId) {
        // Always get fresh LiveData from repository to handle conversation changes
        // ChatRepository singleton will handle cleanup and setup internally
        messages = chatRepository.getMessagesLiveData(conversationId);
        return messages;
    }

    /**
     * Send a text message
     *
     * @param conversationId ID of the conversation
     * @param message        Message object to send
     */
    public void sendMessage(@NonNull String conversationId, @NonNull Message message) {
        LiveData<Resource<Boolean>> result = chatRepository.sendMessageLiveData(conversationId, message);
        sendMessageState.setValue(result.getValue());

        // Observe and forward the result
        result.observeForever(sendMessageState::setValue);
    }

    // ===================== PINNED MESSAGES METHODS =====================

    /**
     * Get send message state
     */
    public LiveData<Resource<Boolean>> getSendMessageState() {
        return sendMessageState;
    }

    /**
     * Upload and send an image message
     *
     * @param conversationId ID of the conversation
     * @param imageUri       URI of the image to upload
     * @param senderId       ID of the sender
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
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     */
    public LiveData<Resource<Boolean>> checkFriendship(@NonNull String userId1,
                                                       @NonNull String userId2) {
        return friendRepository.checkFriendship(userId1, userId2);
    }

    /**
     * Get username by user ID
     *
     * @param userId User ID
     */
    public LiveData<Resource<String>> getUserName(@NonNull String userId) {
        return userRepository.getUserName(userId);
    }

    /**
     * Load pinned messages for a conversation
     *
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
     *
     * @param conversationId ID of the conversation
     * @param messageId      ID of the message to pin
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
     *
     * @param conversationId ID of the conversation
     * @param messageId      ID of the message to unpin
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

    // ===================== MESSAGE REACTIONS METHODS =====================

    /**
     * Reset pin message state (call after handling the result)
     */
    public void resetPinMessageState() {
        pinMessageState.setValue(null);
    }

    /**
     * Add a reaction to a message (increment count)
     * NEW LOGIC: Each click adds one more reaction of that type for this user
     *
     * @param conversationId ID of the conversation
     * @param messageId      ID of the message to react to
     * @param userId         ID of the user adding the reaction
     * @param reactionType   Type of reaction (heart, haha, sad, angry, wow, like)
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
     * Decrement a reaction from a message
     * Removes one count of the specified reaction type for this user
     *
     * @param conversationId ID of the conversation
     * @param messageId      ID of the message
     * @param userId         ID of the user
     * @param reactionType   Type of reaction to decrement
     */
    public void decrementReaction(@NonNull String conversationId,
                                  @NonNull String messageId,
                                  @NonNull String userId,
                                  @NonNull String reactionType) {
        LiveData<Resource<Boolean>> result = chatRepository.decrementReaction(
                conversationId, messageId, userId, reactionType);
        
        // Set initial loading state
        reactionState.setValue(Resource.loading());

        // Observe and forward the result, then remove observer
        final androidx.lifecycle.Observer<Resource<Boolean>> observer = new androidx.lifecycle.Observer<Resource<Boolean>>() {
            @Override
            public void onChanged(Resource<Boolean> resource) {
                if (resource != null && !resource.isLoading()) {
                    reactionState.setValue(resource);
                    // Remove observer after getting final result
                    result.removeObserver(this);
                }
            }
        };
        result.observeForever(observer);
    }

    /**
     * Remove ALL reactions of a user from a message
     *
     * @param conversationId ID of the conversation
     * @param messageId      ID of the message
     * @param userId         ID of the user removing their reaction
     */
    public void removeReaction(@NonNull String conversationId,
                               @NonNull String messageId,
                               @NonNull String userId) {
        LiveData<Resource<Boolean>> result = chatRepository.removeReaction(
                conversationId, messageId, userId);
        
        // Set initial loading state
        reactionState.setValue(Resource.loading());

        // Observe and forward the result, then remove observer
        final androidx.lifecycle.Observer<Resource<Boolean>> observer = new androidx.lifecycle.Observer<Resource<Boolean>>() {
            @Override
            public void onChanged(Resource<Boolean> resource) {
                if (resource != null && !resource.isLoading()) {
                    reactionState.setValue(resource);
                    // Remove observer after getting final result
                    result.removeObserver(this);
                }
            }
        };
        result.observeForever(observer);
    }

    /**
     * Toggle a reaction - if user already has this reaction type, remove it; otherwise add/update it
     *
     * @param conversationId ID of the conversation
     * @param messageId      ID of the message
     * @param userId         ID of the user toggling the reaction
     * @param reactionType   Type of reaction to toggle
     */
    public void toggleReaction(@NonNull String conversationId,
                               @NonNull String messageId,
                               @NonNull String userId,
                               @NonNull String reactionType) {
        LiveData<Resource<Boolean>> result = chatRepository.toggleReaction(
                conversationId, messageId, userId, reactionType);
        
        // Set initial loading state
        reactionState.setValue(Resource.loading());

        // Observe and forward the result, then remove observer
        final androidx.lifecycle.Observer<Resource<Boolean>> observer = new androidx.lifecycle.Observer<Resource<Boolean>>() {
            @Override
            public void onChanged(Resource<Boolean> resource) {
                if (resource != null && !resource.isLoading()) {
                    reactionState.setValue(resource);
                    // Remove observer after getting final result
                    result.removeObserver(this);
                }
            }
        };
        result.observeForever(observer);
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

    /**
     * Mark conversation as read for the current user
     */
    public void markAsRead(String conversationId, String userId) {
        chatRepository.markConversationAsSeen(conversationId, userId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up repository listeners
        chatRepository.cleanup();
    }
}
