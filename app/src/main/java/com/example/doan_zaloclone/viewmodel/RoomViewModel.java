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
