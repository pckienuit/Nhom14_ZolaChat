package com.example.doan_zaloclone.api;

import com.example.doan_zaloclone.api.models.*;
import com.example.doan_zaloclone.models.*;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit API Service Interface
 * Defines all API endpoints for ZaloClone backend
 */
public interface ApiService {
    
    // ========== Authentication ==========
    
    @GET("auth/verify")
    Call<Map<String, Object>> verifyToken();
    
    @POST("auth/set-admin")
    Call<ApiResponse<Void>> setAdmin(@Body Map<String, String> body);
    
    // ========== Users ==========
    
    @GET("users/{userId}")
    Call<User> getUser(@Path("userId") String userId);
    
    @PUT("users/{userId}")
    Call<ApiResponse<Void>> updateUser(
        @Path("userId") String userId,
        @Body Map<String, Object> updates
    );
    
    @POST("users/{userId}/status")
    Call<ApiResponse<Void>> updateUserStatus(
        @Path("userId") String userId,
        @Body Map<String, Boolean> status
    );
    
    @GET("users")
    Call<Map<String, Object>> listUsers(
        @Query("limit") int limit,
        @Query("search") String search,
        @Query("status") String status
    );
    
    @POST("users/{userId}/ban")
    Call<ApiResponse<Void>> banUser(
        @Path("userId") String userId,
        @Body Map<String, Boolean> banData
    );
    
    @POST("users/search")
    Call<Map<String, Object>> searchUsers(@Body Map<String, String> searchQuery);
    
    @POST("users/batch")
    Call<Map<String, Object>> getUsersBatch(@Body Map<String, java.util.List<String>> userIds);
    
    // ========== Messages ==========

    
    @GET("chats/{conversationId}/messages")
    Call<MessageListResponse> getMessages(
        @Path("conversationId") String conversationId,
        @Query("limit") int limit,
        @Query("before") Long before
    );
    
    @POST("chats/{conversationId}/messages")
    Call<ApiResponse<Message>> sendMessage(
        @Path("conversationId") String conversationId,
        @Body SendMessageRequest messageRequest
    );
    
    @POST("chats/{conversationId}/messages/{messageId}/recall")
    Call<ApiResponse<Message>> recallMessage(
        @Path("conversationId") String conversationId,
        @Path("messageId") String messageId
    );
    
    @PUT("chats/{conversationId}/messages/{messageId}")
    Call<ApiResponse<Message>> updateMessage(
        @Path("conversationId") String conversationId,
        @Path("messageId") String messageId,
        @Body Map<String, Object> updates
    );
    
    @DELETE("chats/{conversationId}/messages/{messageId}")
    Call<ApiResponse<Void>> deleteMessage(
        @Path("conversationId") String conversationId,
        @Path("messageId") String messageId
    );
    
    // React to message
    @POST("chats/{conversationId}/messages/{messageId}/react")
    Call<ApiResponse<Message>> addReaction(
        @Path("conversationId") String conversationId,
        @Path("messageId") String messageId,
        @Body Map<String, String> reaction
    );
    
    // ========== Messages API (Phase 3C-1) ==========
    
    // Send message (new API)
    @POST("messages")
    Call<Map<String, Object>> sendMessageV2(@Body Map<String, Object> message);
    
    // Delete message (new API)
    @DELETE("messages/{messageId}")
    Call<Map<String, Object>> deleteMessageV2(
        @Path("messageId") String messageId,
        @Query("conversationId") String conversationId
    );
    
    // Update message - edit or recall (new API)
    @PUT("messages/{messageId}")
    Call<Map<String, Object>> updateMessageV2(
        @Path("messageId") String messageId,
        @Body Map<String, Object> updates
    );

    
    // Remove reaction
    @HTTP(method = "DELETE", path = "chats/{conversationId}/messages/{messageId}/react", hasBody = false)
    Call<ApiResponse<Message>> removeReaction(
        @Path("conversationId") String conversationId,
        @Path("messageId") String messageId,
        @Query("userId") String userId
    );
    
    // Mark conversation as seen/read
    @POST("chats/{conversationId}/seen")
    Call<ApiResponse<Map<String, Object>>> markConversationAsSeen(
        @Path("conversationId") String conversationId,
        @Body Map<String, String> seenData
    );
    
    @PUT("chats/messages/{messageId}")
    Call<ApiResponse<Void>> updateMessageOld(
        @Path("messageId") String messageId,
        @Body Map<String, Object> updates
    );
    
    @POST("chats/messages/{messageId}/reactions")
    Call<ApiResponse<Void>> addReactionOld(
        @Path("messageId") String messageId,
        @Body Map<String, String> reactionData
    );
    
    @DELETE("chats/messages/{messageId}")
    Call<ApiResponse<Void>> deleteMessageOld(
        @Path("messageId") String messageId,
        @Query("conversationId") String conversationId
    );
    
    // ========== Conversations ==========
    
    @GET("conversations")
    Call<ConversationListResponse> getConversations(
        @Query("limit") int limit
    );
    
    @GET("conversations/{conversationId}")
    Call<Conversation> getConversation(@Path("conversationId") String conversationId);
    
    @POST("conversations")
    Call<Map<String, Object>> createConversation(@Body Map<String, Object> conversationData);
    
    @PUT("conversations/{conversationId}")
    Call<ApiResponse<Void>> updateConversation(
        @Path("conversationId") String conversationId,
        @Body Map<String, Object> updates
    );
    
    @DELETE("conversations/{conversationId}")
    Call<ApiResponse<Void>> deleteConversation(@Path("conversationId") String conversationId);
    
    // Group member management
    @POST("conversations/{conversationId}/members")
    Call<ApiResponse<Void>> addGroupMember(
        @Path("conversationId") String conversationId,
        @Body Map<String, String> memberData  // {userId, userName}
    );
    
    @HTTP(method = "DELETE", path = "conversations/{conversationId}/members/{userId}", hasBody = false)
    Call<ApiResponse<Void>> removeGroupMember(
        @Path("conversationId") String conversationId,
        @Path("userId") String userId
    );
    
    @POST("conversations/{conversationId}/leave")
    Call<ApiResponse<Void>> leaveGroup(@Path("conversationId") String conversationId);
    
    // ========== Calls ==========
    
    @GET("calls")
    Call<Map<String, Object>> getCalls(@Query("limit") int limit);
    
    @POST("calls")
    Call<Map<String, Object>> createCall(@Body Map<String, Object> callData);
    
    @PUT("calls/{callId}")
    Call<ApiResponse<Void>> updateCall(
        @Path("callId") String callId,
        @Body Map<String, Object> updates
    );
    
    // ========== Friends ==========
    
    @GET("friends")
    Call<Map<String, Object>> getFriends();
    
    @GET("friends/requests")
    Call<Map<String, Object>> getFriendRequests();
    
    @POST("friends/requests")
    Call<ApiResponse<Void>> sendFriendRequest(@Body Map<String, String> requestData);
    
    @PUT("friends/requests/{requestId}")
    Call<ApiResponse<Void>> respondToFriendRequest(
        @Path("requestId") String requestId,
        @Body Map<String, String> response
    );
    
    @DELETE("friends/{friendId}")
    Call<ApiResponse<Void>> unfriend(@Path("friendId") String friendId);
    
    // ========== Stickers ==========
    
    @GET("stickers/packs")
    Call<Map<String, Object>> getStickerPacks();
    
    @GET("stickers/packs/{packId}")
    Call<Map<String, Object>> getStickerPack(@Path("packId") String packId);
    
    @POST("stickers/packs")
    Call<ApiResponse<Void>> createStickerPack(@Body Map<String, Object> packData);
}
