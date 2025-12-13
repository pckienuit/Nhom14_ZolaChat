package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.repository.ConversationRepository;
import com.example.doan_zaloclone.repository.FriendRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

/**
 * ViewModel for ContactFragment
 * Manages friend search, friend requests, and friends list
 */
public class ContactViewModel extends BaseViewModel {
    
    private final FriendRepository friendRepository;
    private final ConversationRepository conversationRepository;
    
    private final MutableLiveData<Resource<List<User>>> searchResults = new MutableLiveData<>();
    private LiveData<Resource<List<User>>> friends;
    private LiveData<Resource<List<FriendRequest>>> friendRequests;
    
    public ContactViewModel() {
        this.friendRepository = new FriendRepository();
        this.conversationRepository = new ConversationRepository();
    }
    
    /**
     * Search users by query
     * @param query Search term (name or email)
     */
    public void searchUsers(@NonNull String query) {
        // Update search results LiveData
        LiveData<Resource<List<User>>> result = friendRepository.searchUsers(query);
        searchResults.setValue(result.getValue());
        
        // Observe and forward the result
        result.observeForever(searchResults::setValue);
    }
    
    /**
     * Get search results LiveData
     */
    public LiveData<Resource<List<User>>> getSearchResults() {
        return searchResults;
    }
    
    /**
     * Clear search results
     */
    public void clearSearchResults() {
        searchResults.setValue(Resource.success(null));
    }
    
    /**
     * Get friends list for a user
     * @param userId ID of the user
     */
    public LiveData<Resource<List<User>>> getFriends(@NonNull String userId) {
        if (friends == null) {
            friends = friendRepository.getFriends(userId);
        }
        return friends;
    }
    
    /**
     * Get friend requests for a user
     * @param userId ID of the user
     */
    public LiveData<Resource<List<FriendRequest>>> getFriendRequests(@NonNull String userId) {
        if (friendRequests == null) {
            friendRequests = friendRepository.getFriendRequests(userId);
        }
        return friendRequests;
    }
    
    /**
     * Send a friend request
     * @param fromUserId Sender user ID
     * @param toUserId Receiver user ID
     * @param fromUserName Sender user name
     */
    public LiveData<Resource<Boolean>> sendFriendRequest(@NonNull String fromUserId,
                                                          @NonNull String toUserId,
                                                          @NonNull String fromUserName) {
        return friendRepository.sendFriendRequest(fromUserId, toUserId, fromUserName);
    }
    
    /**
     * Accept a friend request
     * @param request The request to accept
     */
    public LiveData<Resource<Boolean>> acceptFriendRequest(@NonNull FriendRequest request) {
        return friendRepository.acceptFriendRequest(request);
    }
    
    /**
     * Reject a friend request
     * @param request The request to reject
     */
    public LiveData<Resource<Boolean>> rejectFriendRequest(@NonNull FriendRequest request) {
        return friendRepository.rejectFriendRequest(request);
    }
    
    /**
     * Check friend request status between two users
     * @param fromUserId Sender ID
     * @param toUserId Receiver ID
     */
    public LiveData<Resource<String>> checkFriendRequestStatus(@NonNull String fromUserId,
                                                                 @NonNull String toUserId) {
        return friendRepository.checkFriendRequestStatus(fromUserId, toUserId);
    }
    
    /**
     * Find existing conversation between two users
     * @param currentUserId Current user ID
     * @param otherUserId Other user ID
     */
    public LiveData<Resource<Conversation>> findExistingConversation(@NonNull String currentUserId,
                                                                      @NonNull String otherUserId) {
        return conversationRepository.findExistingConversation(currentUserId, otherUserId);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up repository listeners
        friendRepository.cleanup();
    }
}
