package com.example.doan_zaloclone.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Repository for conversation-related operations
 * Handles conversation fetching, creation, and real-time updates
 */
public class ConversationRepository {
    
    private final FirestoreManager firestoreManager;
    private ListenerRegistration conversationsListener;
    
    public ConversationRepository() {
        this.firestoreManager = FirestoreManager.getInstance();
    }
    
    /**
     * Get conversations for a user with real-time updates
     * @param userId ID of the user
     * @return LiveData containing Resource with list of conversations
     */
    public LiveData<Resource<List<Conversation>>> getConversations(@NonNull String userId) {
        MutableLiveData<Resource<List<Conversation>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Remove previous listener if exists
        if (conversationsListener != null) {
            conversationsListener.remove();
        }
        
        // Set up real-time listener
        conversationsListener = firestoreManager.listenToConversations(userId, 
            new FirestoreManager.OnConversationsChangedListener() {
                @Override
                public void onConversationsChanged(List<Conversation> conversations) {
                    result.setValue(Resource.success(conversations));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to load conversations";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Find existing conversation between two users
     * @param currentUserId ID of current user
     * @param otherUserId ID of other user
     * @return LiveData containing Resource with conversation (null if not found)
     */
    public LiveData<Resource<Conversation>> findExistingConversation(@NonNull String currentUserId,
                                                                      @NonNull String otherUserId) {
        MutableLiveData<Resource<Conversation>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.findExistingConversation(currentUserId, otherUserId, 
            new FirestoreManager.OnConversationFoundListener() {
                @Override
                public void onFound(Conversation conversation) {
                    result.setValue(Resource.success(conversation));
                }
                
                @Override
                public void onNotFound() {
                    result.setValue(Resource.success(null));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to find conversation";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Create a new conversation between two users
     * @param currentUserId ID of current user
     * @param currentUserName Name of current user
     * @param otherUserId ID of other user
     * @param otherUserName Name of other user
     * @return LiveData containing Resource with created conversation
     */
    public LiveData<Resource<Conversation>> createConversation(@NonNull String currentUserId,
                                                                @NonNull String currentUserName,
                                                                @NonNull String otherUserId,
                                                                @NonNull String otherUserName) {
        MutableLiveData<Resource<Conversation>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.createConversation(currentUserId, currentUserName, otherUserId, otherUserName, 
            new FirestoreManager.OnConversationCreatedListener() {
                @Override
                public void onSuccess(Conversation conversation) {
                    result.setValue(Resource.success(conversation));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to create conversation";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Pin conversation for a user
     * @param conversationId ID of the conversation
     * @param userId ID of the user pinning the conversation
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> pinConversation(@NonNull String conversationId,
                                                     @NonNull String userId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.pinConversation(conversationId, userId, 
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(null));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to pin conversation";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Unpin conversation for a user
     * @param conversationId ID of the conversation
     * @param userId ID of the user unpinning the conversation
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> unpinConversation(@NonNull String conversationId,
                                                       @NonNull String userId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.unpinConversation(conversationId, userId, 
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(null));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to unpin conversation";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Clean up listeners when repository is no longer needed
     */
    public void cleanup() {
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }
}
