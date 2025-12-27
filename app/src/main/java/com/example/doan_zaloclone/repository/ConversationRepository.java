package com.example.doan_zaloclone.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.api.ApiService;
import com.example.doan_zaloclone.api.RetrofitClient;
import com.example.doan_zaloclone.api.models.ApiResponse;
import com.example.doan_zaloclone.api.models.ConversationListResponse;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for conversation-related operations
 * Handles conversation fetching, creation, and real-time updates via REST API + WebSocket
 */
public class ConversationRepository {

    private static final String TAG = "ConversationRepo";
    private static final long REFRESH_DEBOUNCE_MS = 1500; // Debounce time for refresh events

    // Singleton instance
    private static ConversationRepository instance;

    private final ApiService apiService;
    private final SocketManager socketManager;
    private final Handler mainHandler;
    private final java.util.concurrent.ExecutorService backgroundExecutor;

    // Cache conversations locally
    private final List<Conversation> cachedConversations = new ArrayList<>();

    // LiveData for real-time events
    private final MutableLiveData<String> groupLeftEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> conversationRefreshNeeded = new MutableLiveData<>();
    
    // Debounce for refresh events
    private long lastRefreshTriggerTime = 0;
    private Runnable pendingRefreshRunnable = null;

    // Legacy Firestore (keep for now for features not migrated yet)
    private final FirestoreManager firestoreManager;
    private ListenerRegistration conversationsListener;

    /**
     * Private constructor for singleton
     */
    private ConversationRepository() {
        this.apiService = RetrofitClient.getApiService();
        this.socketManager = SocketManager.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.backgroundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        this.firestoreManager = FirestoreManager.getInstance();

        setupSocketListeners();

        // Connect SocketManager to receive real-time conversation events
        socketManager.connect();
    }

    /**
     * Get singleton instance
     */
    public static synchronized ConversationRepository getInstance() {
        if (instance == null) {
            instance = new ConversationRepository();
        }
        return instance;
    }

    /**
     * Setup WebSocket listeners for real-time events
     */
    private void setupSocketListeners() {
        // Listen for group events (when user leaves a group)
        socketManager.setGroupEventListener(new SocketManager.OnGroupEventListener() {
            @Override
            public void onGroupLeft(String conversationId) {
                Log.d(TAG, "🚪 Received group_left event for conversation: " + conversationId);

                // Remove from cache
                synchronized (cachedConversations) {
                    cachedConversations.removeIf(c -> c.getId().equals(conversationId));
                }

                // Broadcast event to UI
                mainHandler.post(() -> groupLeftEvent.setValue(conversationId));
            }

            @Override
            public void onMemberLeft(String conversationId, String userId, String userName) {
                Log.d(TAG, "👥 Member " + userName + " left conversation: " + conversationId);
                // Trigger refresh to update member count
                triggerRefreshDebounced();
            }

            @Override
            public void onConversationCreated(String conversationId) {
                Log.d(TAG, "➕ New conversation created: " + conversationId);
                // Clear cache and trigger refresh
                synchronized (cachedConversations) {
                    cachedConversations.clear();
                }
                triggerRefreshDebounced();
            }

            @Override
            public void onConversationUpdated(String conversationId) {
                Log.d(TAG, "🔄 Conversation updated: " + conversationId);
                // Trigger refresh to get latest data
                triggerRefreshDebounced();
            }

            @Override
            public void onConversationDeleted(String conversationId) {
                Log.d(TAG, "🗑️ Conversation deleted: " + conversationId);

                // Remove from cache
                synchronized (cachedConversations) {
                    cachedConversations.removeIf(c -> c.getId().equals(conversationId));
                }

                // Broadcast event to UI (same as group_left - removes from list)
                mainHandler.post(() -> groupLeftEvent.setValue(conversationId));
            }

            @Override
            public void onMemberAdded(String conversationId, String userId, String addedBy) {
                Log.d(TAG, "➕ Member " + userId + " added to conversation: " + conversationId);
                // Trigger refresh to update member list
                triggerRefreshDebounced();
            }

            @Override
            public void onMemberRemoved(String conversationId, String userId, String removedBy) {
                Log.d(TAG, "➖ Member " + userId + " removed from conversation: " + conversationId);
                // Trigger refresh to update member list
                triggerRefreshDebounced();
            }

            @Override
            public void onAdminUpdated(String conversationId, String userId, String action, String updatedBy) {
                Log.d(TAG, "👑 Admin " + action + " for user " + userId + " in conversation: " + conversationId);
                // Trigger refresh to update admin status
                triggerRefreshDebounced();
            }
        });

        // Listen for new messages to update conversation preview
        socketManager.addMessageListener(new SocketManager.OnMessageListener() {
            @Override
            public void onMessageReceived(org.json.JSONObject messageData) {
                Log.d(TAG, "📨 ConversationRepo received new message, triggering refresh");
                // Use debounced refresh to prevent multiple rapid refreshes
                triggerRefreshDebounced();
            }

            @Override
            public void onMessageUpdated(org.json.JSONObject messageData) {
                 triggerRefreshDebounced();
            }

            @Override
            public void onMessageDeleted(org.json.JSONObject messageData) {
                 triggerRefreshDebounced();
            }
        });
    }
    
    /**
     * Trigger refresh with debounce to prevent multiple rapid refreshes
     * This coalesces multiple events within REFRESH_DEBOUNCE_MS into a single refresh
     */
    private void triggerRefreshDebounced() {
        long now = System.currentTimeMillis();
        
        // Cancel any pending refresh
        if (pendingRefreshRunnable != null) {
            mainHandler.removeCallbacks(pendingRefreshRunnable);
        }
        
        // Schedule new refresh after debounce period
        pendingRefreshRunnable = () -> {
            Log.d(TAG, "🔄 Triggering debounced refresh");
            conversationRefreshNeeded.setValue(true);
            lastRefreshTriggerTime = System.currentTimeMillis();
        };
        
        // If last refresh was recent, delay more; otherwise refresh sooner
        long delay = (now - lastRefreshTriggerTime < REFRESH_DEBOUNCE_MS) ? REFRESH_DEBOUNCE_MS : 500;
        mainHandler.postDelayed(pendingRefreshRunnable, delay);
    }

    /**
     * Get LiveData for group_left events
     * UI can observe this to remove conversations from the list
     */
    public LiveData<String> getGroupLeftEvent() {
        return groupLeftEvent;
    }

    /**
     * Get LiveData for conversation refresh events
     * UI should observe this to reload conversations list
     */
    public LiveData<Boolean> getConversationRefreshNeeded() {
        return conversationRefreshNeeded;
    }

    /**
     * Get conversations for a user via REST API
     *
     * @param userId ID of the user (for future use, currently server gets from auth token)
     * @return LiveData containing Resource with list of conversations
     */
    public LiveData<Resource<List<Conversation>>> getConversations(@NonNull String userId) {
        MutableLiveData<Resource<List<Conversation>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        Log.d(TAG, "Fetching conversations from API...");

        // Fetch conversations from API
        Call<ConversationListResponse> call = apiService.getConversations(50);

        call.enqueue(new Callback<ConversationListResponse>() {
            @Override
            public void onResponse(Call<ConversationListResponse> call, Response<ConversationListResponse> response) {
                Log.d(TAG, "API Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ConversationListResponse responseBody = response.body();
                    List<Conversation> conversations = responseBody.getConversations();

                    Log.d(TAG, "Response body: " + (responseBody != null ? "exists" : "null"));
                    Log.d(TAG, "Conversations list: " + (conversations != null ? "size=" + conversations.size() : "null"));

                    // Update cache
                    synchronized (cachedConversations) {
                        cachedConversations.clear();
                        if (conversations != null) {
                            cachedConversations.addAll(conversations);
                        }
                    }

                    Log.d(TAG, "✅ Fetched " + (conversations != null ? conversations.size() : 0) + " conversations");
                    result.setValue(Resource.success(conversations != null ? conversations : new ArrayList<>()));
                } else {
                    String error = "HTTP " + response.code();
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "no error body";
                        Log.e(TAG, "Failed to fetch conversations: " + error + ", body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to fetch conversations: " + error);
                    }
                    result.setValue(Resource.error(error));
                }
            }

            @Override
            public void onFailure(Call<ConversationListResponse> call, Throwable t) {
                String error = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "❌ Network error fetching conversations: " + error, t);
                result.setValue(Resource.error(error));
            }
        });

        return result;
    }

    /**
     * Find existing conversation between two users
     *
     * @param currentUserId ID of current user
     * @param otherUserId   ID of other user
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
     * Create a new conversation (1-on-1 or group chat) via REST API
     *
     * @param participants List of user IDs
     * @param isGroup      true for group chat, false for 1-on-1
     * @param groupName    Group name (for group chats only)
     * @return LiveData containing Resource with success status and conversationId
     */
    public LiveData<Resource<String>> createConversation(@NonNull List<String> participants,
                                                         boolean isGroup,
                                                         String groupName) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                // Prepare request body
                Map<String, Object> conversationData = new HashMap<>();
                conversationData.put("participants", participants);
                conversationData.put("isGroup", isGroup);
                if (isGroup && groupName != null) {
                    conversationData.put("groupName", groupName);
                }

                // Call API
                Call<Map<String, Object>> call = apiService.createConversation(conversationData);
                Response<Map<String, Object>> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    String conversationId = (String) responseBody.get("conversationId");

                    Log.d(TAG, "Created conversation: " + conversationId);
                    mainHandler.post(() -> result.setValue(Resource.success(conversationId)));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to create conversation: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error creating conversation", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
            }
        });

        return result;
    }

    /**
     * Legacy: Create a new conversation between two users via Firestore
     * DEPRECATED: Use createConversation(List, boolean, String) instead
     */
    @Deprecated
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
     *
     * @param conversationId ID of the conversation
     * @param userId         ID of the user pinning the conversation
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
     *
     * @param conversationId ID of the conversation
     * @param userId         ID of the user unpinning the conversation
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
     * Update tags for a conversation
     *
     * @param conversationId ID of the conversation
     * @param userId         ID of the user
     * @param tags           List of tags to set
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> updateTags(@NonNull String conversationId,
                                               @NonNull String userId,
                                               @NonNull List<String> tags) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.updateConversationTags(conversationId, userId, tags,
                new FirestoreManager.OnGroupUpdatedListener() {
                    @Override
                    public void onSuccess() {
                        result.setValue(Resource.success(null));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        String errorMessage = e.getMessage() != null
                                ? e.getMessage()
                                : "Failed to update tags";
                        result.setValue(Resource.error(errorMessage));
                    }
                });

        return result;
    }

    /**
     * Update group conversation (rename, change avatar, etc.) via REST API
     *
     * @param conversationId ID of the conversation
     * @param updates        Map of fields to update (name, groupAvatar, etc.)
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> updateGroupInfo(@NonNull String conversationId,
                                                    @NonNull Map<String, Object> updates) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Updating group " + conversationId + " - fields: " + updates.keySet());

                Call<Map<String, Object>> call = apiService.updateConversation(conversationId, updates);
                Response<Map<String, Object>> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Updated group info");
                    mainHandler.post(() -> result.setValue(Resource.success(null)));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to update group: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error updating group", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
            }
        });

        return result;
    }

    /**
     * Add member to group via REST API
     *
     * @param conversationId ID of the group conversation
     * @param userId         ID of the user to add
     * @param userName       Name of the user to add
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> addGroupMember(@NonNull String conversationId,
                                                   @NonNull String userId,
                                                   @NonNull String userName) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Adding member " + userId + " to group " + conversationId);

                Map<String, String> memberData = new HashMap<>();
                memberData.put("userId", userId);
                memberData.put("userName", userName);

                Call<ApiResponse<Void>> call = apiService.addGroupMember(conversationId, memberData);
                Response<ApiResponse<Void>> response = call.execute();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Added member to group");
                    mainHandler.post(() -> result.setValue(Resource.success(null)));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to add member: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error adding member", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
            }
        });

        return result;
    }

    /**
     * Remove member from group via REST API
     *
     * @param conversationId ID of the group conversation
     * @param userId         ID of the user to remove
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> removeGroupMember(@NonNull String conversationId,
                                                      @NonNull String userId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Removing member " + userId + " from group " + conversationId);

                Call<ApiResponse<Void>> call = apiService.removeGroupMember(conversationId, userId);
                Response<ApiResponse<Void>> response = call.execute();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Removed member from group");
                    mainHandler.post(() -> result.setValue(Resource.success(null)));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to remove member: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error removing member", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
            }
        });

        return result;
    }

    /**
     * Leave group conversation via REST API
     *
     * @param conversationId ID of the group conversation
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> leaveGroup(@NonNull String conversationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Leaving group " + conversationId);

                Call<Map<String, Object>> call = apiService.leaveGroup(conversationId);
                Response<Map<String, Object>> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Left group");
                    mainHandler.post(() -> result.setValue(Resource.success(null)));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to leave group: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error leaving group", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
            }
        });

        return result;
    }

    /**
     * Delete/Dissolve group conversation via REST API
     *
     * @param conversationId ID of the group conversation
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Void>> deleteConversation(@NonNull String conversationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Deleting conversation " + conversationId);

                Call<Map<String, Object>> call = apiService.deleteConversation(conversationId);
                Response<Map<String, Object>> response = call.execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Deleted conversation");

                    // Remove from cache
                    synchronized (cachedConversations) {
                        cachedConversations.removeIf(c -> c.getId().equals(conversationId));
                    }

                    mainHandler.post(() -> result.setValue(Resource.success(null)));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to delete conversation: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error deleting conversation", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
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
