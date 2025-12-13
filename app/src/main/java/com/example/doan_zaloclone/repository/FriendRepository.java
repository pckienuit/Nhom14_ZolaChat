package com.example.doan_zaloclone.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Repository for friend-related operations
 * Handles friend requests, friend list, and user search
 */
public class FriendRepository {
    
    private final FirestoreManager firestoreManager;
    private ListenerRegistration friendRequestsListener;
    private ListenerRegistration friendsListener;  // Track friends listener
    
    public FriendRepository() {
        this.firestoreManager = FirestoreManager.getInstance();
    }
    
    /**
     * Search users by name or email
     * @param query Search query (partial match supported)
     * @return LiveData containing Resource with list of users
     */
    public LiveData<Resource<List<User>>> searchUsers(@NonNull String query) {
        MutableLiveData<Resource<List<User>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.searchUsers(query, new FirestoreManager.OnUserSearchListener() {
            @Override
            public void onSuccess(List<User> users) {
                result.setValue(Resource.success(users));
            }
            
            @Override
            public void onFailure(Exception e) {
                String errorMessage = e.getMessage() != null 
                        ? e.getMessage() 
                        : "Failed to search users";
                result.setValue(Resource.error(errorMessage));
            }
        });
        
        return result;
    }
    
    /**
     * Send a friend request
     * @param fromUserId ID of user sending the request
     * @param toUserId ID of user receiving the request
     * @param fromUserName Name of user sending the request
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> sendFriendRequest(@NonNull String fromUserId,
                                                          @NonNull String toUserId,
                                                          @NonNull String fromUserName) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // FirestoreManager requires email too, get it from Firestore or use empty
        String fromUserEmail = ""; // Email will be empty for now
        
        firestoreManager.sendFriendRequest(fromUserId, fromUserName, fromUserEmail, toUserId, 
            new FirestoreManager.OnFriendRequestListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to send friend request";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Accept a friend request
     * @param request The friend request to accept
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> acceptFriendRequest(@NonNull FriendRequest request) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.acceptFriendRequest(request.getId(), 
            new FirestoreManager.OnFriendRequestListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to accept friend request";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Reject a friend request
     * @param request The friend request to reject
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> rejectFriendRequest(@NonNull FriendRequest request) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.rejectFriendRequest(request.getId(), 
            new FirestoreManager.OnFriendRequestListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to reject friend request";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Get list of friends with real-time updates  
     * @param userId ID of the user
     * @return LiveData containing Resource with list of friends
     */
    public LiveData<Resource<List<User>>> getFriends(@NonNull String userId) {
        MutableLiveData<Resource<List<User>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Remove previous listener if exists
        if (friendsListener != null) {
            friendsListener.remove();
        }
        
        // Set up real-time listener for friends
        friendsListener = firestoreManager.listenToFriends(userId, 
            new FirestoreManager.OnFriendsChangedListener() {
                @Override
                public void onFriendsChanged(List<User> friends) {
                    result.setValue(Resource.success(friends));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to load friends";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Get friend requests for a user with real-time updates
     * @param userId ID of the user
     * @return LiveData containing Resource with list of friend requests
     */
    public LiveData<Resource<List<FriendRequest>>> getFriendRequests(@NonNull String userId) {
        MutableLiveData<Resource<List<FriendRequest>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Remove previous listener if exists
        if (friendRequestsListener != null) {
            friendRequestsListener.remove();
        }
        
        // Set up real-time listener
        friendRequestsListener = firestoreManager.listenToFriendRequests(userId, 
            new FirestoreManager.OnFriendRequestsChangedListener() {
                @Override
                public void onFriendRequestsChanged(List<FriendRequest> requests) {
                    result.setValue(Resource.success(requests));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to load friend requests";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Check if two users are friends
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return LiveData containing Resource with friendship status
     */
    public LiveData<Resource<Boolean>> checkFriendship(@NonNull String userId1, 
                                                        @NonNull String userId2) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.checkFriendship(userId1, userId2, 
            new FirestoreManager.OnFriendshipCheckListener() {
                @Override
                public void onResult(boolean areFriends) {
                    result.setValue(Resource.success(areFriends));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to check friendship";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Check friend request status between two users
     * @param fromUserId ID of sender
     * @param toUserId ID of receiver
     * @return LiveData containing Resource with status string
     */
    public LiveData<Resource<String>> checkFriendRequestStatus(@NonNull String fromUserId,
                                                                @NonNull String toUserId) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.checkFriendRequestStatus(fromUserId, toUserId, 
            new FirestoreManager.OnFriendRequestStatusListener() {
                @Override
                public void onStatus(String status) {
                    result.setValue(Resource.success(status));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to check request status";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Remove a friend (unfriend)
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> removeFriend(@NonNull String userId1,
                                                     @NonNull String userId2) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.removeFriend(userId1, userId2, 
            new FirestoreManager.OnFriendRemovedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to remove friend";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Clean up listeners when repository is no longer needed
     */
    public void cleanup() {
        if (friendRequestsListener != null) {
            friendRequestsListener.remove();
            friendRequestsListener = null;
        }
        if (friendsListener != null) {
            friendsListener.remove();
            friendsListener = null;
        }
    }
}
