package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.repository.ConversationRepository;
import com.example.doan_zaloclone.repository.FriendRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel for HomeFragment
 * Manages conversations list and related operations
 */
public class HomeViewModel extends BaseViewModel {
    
    private final ConversationRepository conversationRepository;
    private final FriendRepository friendRepository;
    private LiveData<Resource<List<Conversation>>> conversations;
    private MutableLiveData<String> selectedTagFilter = new MutableLiveData<>(null);
    
    public HomeViewModel() {
        this.conversationRepository = new ConversationRepository();
        this.friendRepository = new FriendRepository();
    }
    
    /**
     * Load conversations for a user with automatic sorting
     * Pinned conversations appear first (sorted by pin time), then unpinned (sorted by timestamp)
     * @param userId ID of the user
     * @return LiveData containing sorted list of conversations
     */
    public LiveData<Resource<List<Conversation>>> getConversations(@NonNull String userId) {
        if (conversations == null) {
            LiveData<Resource<List<Conversation>>> rawConversations = 
                conversationRepository.getConversations(userId);
            
            // Transform conversations to sort them: pinned first, then unpinned
            conversations = Transformations.map(rawConversations, resource -> {
                if (resource != null && resource.isSuccess() && resource.getData() != null) {
                    List<Conversation> sorted = sortConversations(resource.getData(), userId);
                    return Resource.success(sorted);
                }
                return resource;
            });
        }
        return conversations;
    }
    
    /**
     * Sort conversations: pinned first (by pin timestamp), then unpinned (by message timestamp)
     * @param conversationList List of conversations to sort
     * @param userId Current user ID
     * @return Sorted list
     */
    private List<Conversation> sortConversations(List<Conversation> conversationList, String userId) {
        java.util.List<Conversation> pinned = new java.util.ArrayList<>();
        java.util.List<Conversation> unpinned = new java.util.ArrayList<>();
        
        // Separate pinned and unpinned conversations
        for (Conversation c : conversationList) {
            if (c.isPinnedByUser(userId)) {
                pinned.add(c);
            } else {
                unpinned.add(c);
            }
        }
        
        // Sort pinned by pinnedAt timestamp (oldest pinned first - maintains position)
        pinned.sort((a, b) -> Long.compare(
            a.getPinnedAtTimestamp(userId), 
            b.getPinnedAtTimestamp(userId)));
        
        // Sort unpinned by message timestamp (newest first)
        unpinned.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        // Combine: pinned first, then unpinned
        pinned.addAll(unpinned);
        return pinned;
    }
    
    /**
     * Pin a conversation
     * @param conversationId ID of the conversation
     * @param userId ID of the user
     * @return LiveData with success/error status
     */
    public LiveData<Resource<Void>> pinConversation(@NonNull String conversationId,
                                                     @NonNull String userId) {
        return conversationRepository.pinConversation(conversationId, userId);
    }
    
    /**
     * Unpin a conversation
     * @param conversationId ID of the conversation
     * @param userId ID of the user
     * @return LiveData with success/error status
     */
    public LiveData<Resource<Void>> unpinConversation(@NonNull String conversationId,
                                                       @NonNull String userId) {
        return conversationRepository.unpinConversation(conversationId, userId);
    }
    
    /**
     * Update tags for a conversation
     * @param conversationId ID of the conversation
     * @param userId ID of the user
     * @param tags List of tags to set
     * @return LiveData with success/error status
     */
    public LiveData<Resource<Void>> updateTags(@NonNull String conversationId,
                                                @NonNull String userId,
                                                @NonNull List<String> tags) {
        return conversationRepository.updateTags(conversationId, userId, tags);
    }
    
    /**
     * Set tag filter
     * @param tag Tag to filter by (null for all)
     */
    public void setTagFilter(String tag) {
        selectedTagFilter.setValue(tag);
    }
    
    /**
     * Get current tag filter
     * @return LiveData of selected tag filter
     */
    public LiveData<String> getSelectedTagFilter() {
        return selectedTagFilter;
    }
    
    /**
     * Get filtered conversations based on selected tag
     * @param userId Current user ID
     * @return LiveData of filtered conversations
     */
    public LiveData<Resource<List<Conversation>>> getFilteredConversations(@NonNull String userId) {
        return Transformations.switchMap(selectedTagFilter, tag -> {
            if (tag == null) {
                // No filter, return all conversations
                return getConversations(userId);
            } else {
                // Filter by tag
                return Transformations.map(getConversations(userId), resource -> {
                    if (resource != null && resource.isSuccess() && resource.getData() != null) {
                        List<Conversation> filtered = resource.getData().stream()
                            .filter(conv -> conv.hasTag(userId, tag))
                            .collect(Collectors.toList());
                        return Resource.success(filtered);
                    }
                    return resource;
                });
            }
        });
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
