package com.example.doan_zaloclone.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.api.ApiService;
import com.example.doan_zaloclone.api.RetrofitClient;
import com.example.doan_zaloclone.api.models.ConversationListResponse;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.websocket.SocketManager;
import com.example.doan_zaloclone.utils.Resource;
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
    
    private final ApiService apiService;
    private final SocketManager socketManager;
    private final Handler mainHandler;
    
    // Cache conversations locally
    private final List<Conversation> cachedConversations = new ArrayList<>();
    
    // Legacy Firestore (keep for now for features not migrated yet)
    private final FirestoreManager firestoreManager;
    private ListenerRegistration conversationsListener;
    
    public ConversationRepository() {
        this.apiService = RetrofitClient.getApiService();
        this.socketManager = SocketManager.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.firestoreManager = FirestoreManager.getInstance();
    }
    
    /**
     * Get conversations for a user via REST API
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
     * Create a new conversation (1-on-1 or group chat) via REST API
     * @param participants List of user IDs
     * @param isGroup true for group chat, false for 1-on-1
     * @param groupName Group name (for group chats only)
     * @return LiveData containing Resource with success status and conversationId
     */
    public LiveData<Resource<String>> createConversation(@NonNull List<String> participants,
                                                          boolean isGroup,
                                                          String groupName) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Prepare request body
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("participants", participants);
        conversationData.put("isGroup", isGroup);
        if (isGroup && groupName != null) {
            conversationData.put("groupName", groupName);
        }
        
        // Call API
        Call<Map<String, Object>> call = apiService.createConversation(conversationData);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    String conversationId = (String) responseBody.get("conversationId");
                    
                    Log.d(TAG, "Created conversation: " + conversationId);
                    result.setValue(Resource.success(conversationId));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to create conversation: " + error);
                    result.setValue(Resource.error(error));
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                String error = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "Network error creating conversation", t);
                result.setValue(Resource.error(error));
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
     * Update tags for a conversation
     * @param conversationId ID of the conversation
     * @param userId ID of the user
     * @param tags List of tags to set
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
     * Clean up listeners when repository is no longer needed
     */
    public void cleanup() {
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }
}
