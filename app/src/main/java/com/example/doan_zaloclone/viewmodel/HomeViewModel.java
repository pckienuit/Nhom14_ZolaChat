package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.repository.ConversationRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

/**
 * ViewModel for HomeFragment
 * Manages conversations list and related operations
 */
public class HomeViewModel extends BaseViewModel {
    
    private final ConversationRepository conversationRepository;
    private LiveData<Resource<List<Conversation>>> conversations;
    
    public HomeViewModel() {
        this.conversationRepository = new ConversationRepository();
    }
    
    /**
     * Load conversations for a user
     * @param userId ID of the user
     * @return LiveData containing list of conversations
     */
    public LiveData<Resource<List<Conversation>>> getConversations(@NonNull String userId) {
        if (conversations == null) {
            conversations = conversationRepository.getConversations(userId);
        }
        return conversations;
    }
    
    /**
     * Refresh conversations (call this if needed to reload)
     * @param userId ID of the user
     */
    public void refreshConversations(@NonNull String userId) {
        conversations = conversationRepository.getConversations(userId);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up repository listeners
        conversationRepository.cleanup();
    }
}
