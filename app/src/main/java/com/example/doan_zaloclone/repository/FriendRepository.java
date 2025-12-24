package com.example.doan_zaloclone.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.api.ApiService;
import com.example.doan_zaloclone.api.RetrofitClient;
import com.example.doan_zaloclone.api.models.ApiResponse;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.utils.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for friend-related operations
 * Migrated to use REST API + WebSocket for real-time updates
 */
public class FriendRepository {
    
    private static final String TAG = "FriendRepository";
    private final ApiService apiService;
    
    public FriendRepository() {
        this.apiService = RetrofitClient.getApiService();
    }
    
    /**
     * Search users by name or email via API
     * @param query Search query (partial match supported)
     * @return LiveData containing Resource with list of users
     */
    public LiveData<Resource<List<User>>> searchUsers(@NonNull String query) {
        MutableLiveData<Resource<List<User>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        if (query.length() < 2) {
            result.setValue(Resource.success(new ArrayList<>()));
            return result;
        }
        
        Map<String, String> searchQuery = new HashMap<>();
        searchQuery.put("query", query);
        
        Call<Map<String, Object>> call = apiService.searchUsers(searchQuery);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> usersData = (List<Map<String, Object>>) response.body().get("users");
                    List<User> users = parseUsersFromData(usersData);
                    
                    Log.d(TAG, "✅ Search found " + users.size() + " users");
                    result.setValue(Resource.success(users));
                } else {
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Search failed", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * Send a friend request via API
     * @param fromUserId ID of user sending the request
     * @param toUserId ID of user receiving the request
     * @param fromUserName Name of user sending (for notification)
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> sendFriendRequest(@NonNull String fromUserId,
                                                          @NonNull String toUserId,
                                                          @NonNull String fromUserName) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        Map<String, String> requestData = new HashMap<>();
        requestData.put("senderId", fromUserId);
        requestData.put("receiverId", toUserId);
        requestData.put("senderName", fromUserName);
        
        Call<ApiResponse<Void>> call = apiService.sendFriendRequest(requestData);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Friend request sent");
                    result.setValue(Resource.success(true));
                } else {
                    Log.e(TAG, "Failed: HTTP " + response.code());
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Network error", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * Accept a friend request via API
     * @param request The friend request to accept
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> acceptFriendRequest(@NonNull FriendRequest request) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        Map<String, String> response = new HashMap<>();
        response.put("action", "accept");
        
        Call<ApiResponse<Void>> call = apiService.respondToFriendRequest(request.getId(), response);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                if (resp.isSuccessful()) {
                    Log.d(TAG, "✅ Friend request accepted");
                    result.setValue(Resource.success(true));
                } else {
                    result.setValue(Resource.error("HTTP " + resp.code()));
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Accept failed", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * Reject a friend request via API
     * @param request The friend request to reject
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> rejectFriendRequest(@NonNull FriendRequest request) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        Map<String, String> response = new HashMap<>();
        response.put("action", "reject");
        
        Call<ApiResponse<Void>> call = apiService.respondToFriendRequest(request.getId(), response);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> resp) {
                if (resp.isSuccessful()) {
                    Log.d(TAG, "✅ Friend request rejected");
                    result.setValue(Resource.success(true));
                } else {
                    result.setValue(Resource.error("HTTP " + resp.code()));
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Reject failed", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * Get list of friends via API (returns full User objects)
     * @param userId ID of the user
     * @return LiveData containing Resource with list of friend User objects
     */
    public LiveData<Resource<List<User>>> getFriends(@NonNull String userId) {
        MutableLiveData<Resource<List<User>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // First get friend IDs
        Call<Map<String, Object>> call = apiService.getFriends();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> friendIds = (List<String>) response.body().get("friends");
                    if (friendIds == null || friendIds.isEmpty()) {
                        result.setValue(Resource.success(new ArrayList<>()));
                        return;
                    }
                    
                    // Batch fetch friend user data
                    Map<String, List<String>> batchRequest = new HashMap<>();
                    batchRequest.put("userIds", friendIds);
                    
                    Call<Map<String, Object>> batchCall = apiService.getUsersBatch(batchRequest);
                    batchCall.enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> batchCall, Response<Map<String, Object>> batchResponse) {
                            if (batchResponse.isSuccessful() && batchResponse.body() != null) {
                                List<Map<String, Object>> usersData = (List<Map<String, Object>>) batchResponse.body().get("users");
                                List<User> friends = parseUsersFromData(usersData);
                                
                                Log.d(TAG, "✅ Friends loaded: " + friends.size());
                                result.setValue(Resource.success(friends));
                            } else {
                                result.setValue(Resource.error("HTTP " + batchResponse.code()));
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<Map<String, Object>> batchCall, Throwable t) {
                            Log.e(TAG, "Failed to batch load friend data", t);
                            result.setValue(Resource.error(t.getMessage()));
                        }
                    });
                } else {
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Failed to load friends", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * Get friend requests via API
     * @param userId ID of the user
     * @return LiveData containing Resource with list of friend requests
     */
    public LiveData<Resource<List<FriendRequest>>> getFriendRequests(@NonNull String userId) {
        MutableLiveData<Resource<List<FriendRequest>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        Call<Map<String, Object>> call = apiService.getFriendRequests();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> requestsData = (List<Map<String, Object>>) response.body().get("requests");
                    List<FriendRequest> requests = parseFriendRequestsFromData(requestsData);
                    
                    Log.d(TAG, "✅ Friend requests loaded: " + requests.size());
                    result.setValue(Resource.success(requests));
                } else {
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Failed to load requests", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });
        
        return result;
    }
    
    /**
     * Remove a friend via API
     * @param userId1 Current user ID
     * @param userId2 Friend's user ID
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> removeFriend(@NonNull String userId1,
                                                     @NonNull String userId2) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        Call<ApiResponse<Void>> call = apiService.unfriend(userId2);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Friend removed");
                    result.setValue(Resource.success(true));
                } else {
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Failed to remove friend", t);
                result.setValue(Resource.error(t.getMessage()));
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
        
        // Get friends list and check if userId2 is in it
        LiveData<Resource<List<User>>> friendsLiveData = getFriends(userId1);
        friendsLiveData.observeForever(friendsResource -> {
            if (friendsResource != null) {
                if (friendsResource.getStatus() == Resource.Status.SUCCESS && friendsResource.getData() != null) {
                    boolean isFriend = false;
                    for (User friend : friendsResource.getData()) {
                        if (friend.getId().equals(userId2)) {
                            isFriend = true;
                            break;
                        }
                    }
                    result.setValue(Resource.success(isFriend));
                } else if (friendsResource.getStatus() == Resource.Status.ERROR) {
                    result.setValue(Resource.error(friendsResource.getMessage()));
                }
            }
        });
        
        return result;
    }
    
    /**
     * Check friend request status between two users
     * @param fromUserId Sender ID
     * @param toUserId Receiver ID
     * @return LiveData containing Resource with status string
     */
    public LiveData<Resource<String>> checkFriendRequestStatus(@NonNull String fromUserId,
                                                                @NonNull String toUserId) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Get friend requests and check status
        LiveData<Resource<List<FriendRequest>>> requestsLiveData = getFriendRequests(toUserId);
        requestsLiveData.observeForever(requestsResource -> {
            if (requestsResource != null) {
                if (requestsResource.getStatus() == Resource.Status.SUCCESS && requestsResource.getData() != null) {
                    String status = "NONE";
                    for (FriendRequest request : requestsResource.getData()) {
                        if (request.getFromUserId().equals(fromUserId) && request.getToUserId().equals(toUserId)) {
                            status = request.getStatus();
                            break;
                        }
                    }
                    result.setValue(Resource.success(status));
                } else if (requestsResource.getStatus() == Resource.Status.ERROR) {
                    result.setValue(Resource.error(requestsResource.getMessage()));
                }
            }
        });
        
        return result;
    }
    
    // ========== Helper methods ==========
    
    private List<User> parseUsersFromData(List<Map<String, Object>> usersData) {
        List<User> users = new ArrayList<>();
        if (usersData != null) {
            for (Map<String, Object> userData : usersData) {
                User user = parseUserFromMap(userData);
                if (user != null) {
                    users.add(user);
                }
            }
        }
        return users;
    }
    
    private User parseUserFromMap(Map<String, Object> userData) {
        try {
            User user = new User();
            user.setId((String) userData.get("id"));
            user.setName((String) userData.get("name"));
            user.setEmail((String) userData.get("email"));
            user.setAvatarUrl((String) userData.get("avatarUrl"));
            user.setBio((String) userData.get("bio"));
            
            // Note: User model doesn't have isOnline field in Android
            // Online status is handled separately via presence system
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing user", e);
            return null;
        }
    }
    
    private List<FriendRequest> parseFriendRequestsFromData(List<Map<String, Object>> requestsData) {
        List<FriendRequest> requests = new ArrayList<>();
        if (requestsData != null) {
            for (Map<String, Object> requestData : requestsData) {
                FriendRequest request = parseFriendRequestFromMap(requestData);
                if (request != null) {
                    requests.add(request);
                }
            }
        }
        return requests;
    }
    
    private FriendRequest parseFriendRequestFromMap(Map<String, Object> requestData) {
        try {
            FriendRequest request = new FriendRequest();
            request.setId((String) requestData.get("id"));
            request.setFromUserId((String) requestData.get("senderId"));
            request.setToUserId((String) requestData.get("receiverId"));
            request.setFromUserName((String) requestData.get("fromUserName"));
            request.setFromUserEmail((String) requestData.get("fromUserEmail"));
            request.setStatus((String) requestData.get("status"));
            
            Object timestampObj = requestData.get("createdAt");
            if (timestampObj instanceof Number) {
                request.setTimestamp(((Number) timestampObj).longValue());
            }
            
            Log.d(TAG, "Parsed friend request: " + request.getFromUserName() + " (" + request.getFromUserId() + ")");
            
            return request;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing friend request", e);
            return null;
        }
    }
    
    /**
     * Cleanup method (needed if we add WebSocket listeners in future)
     */
    public void cleanup() {
        // Future: Stop WebSocket listeners here
    }
}
