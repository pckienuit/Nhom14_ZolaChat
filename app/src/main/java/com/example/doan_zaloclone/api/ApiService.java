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
    
    // Add/change/remove reaction (new API - Phase 3C-3)
    @POST("messages/{messageId}/reactions")
    Call<Map<String, Object>> addReactionV2(
        @Path("messageId") String messageId,
        @Body Map<String, Object> reactionData
    );
    
    // Clear ALL reactions on a message (new API - Phase 3C-3)
    @DELETE("messages/{messageId}/reactions")
    Call<Map<String, Object>> clearAllReactions(
        @Path("messageId") String messageId,
        @Query("conversationId") String conversationId
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
    
    // Still using old signatures (used by addGroupMember/removeGroupMember)
    @POST("conversations/{conversationId}/members")
    Call<ApiResponse<Void>> addGroupMember(
        @Path("conversationId") String conversationId,
        @Body Map<String, String> memberData
    );
    
    @HTTP(method = "DELETE", path = "conversations/{conversationId}/members/{userId}", hasBody = false)
    Call<ApiResponse<Void>> removeGroupMember(
        @Path("conversationId") String conversationId,
        @Path("userId") String userId
    );
    
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
    
    // ========== Group Management (Phase 4A - 3D) ==========
    
    // Update group info (name, avatar)
    @PUT("conversations/{id}")
    Call<Map<String, Object>> updateConversation(
        @Path("id") String conversationId,
        @Body Map<String, Object> updates
    );
    
    // Add member to group
    @POST("conversations/{id}/members")
    Call<Map<String, Object>> addMember(
        @Path("id") String conversationId,
        @Body Map<String, Object> data
    );
    
    // Remove member from group
    @DELETE("conversations/{id}/members/{userId}")
    Call<Map<String, Object>> removeMember(
        @Path("id") String conversationId,
        @Path("userId") String userId
    );
    
    // Leave group
    @POST("conversations/{id}/leave")
    Call<Map<String, Object>> leaveGroup(@Path("id") String conversationId);
    
    // Promote/demote admin
    @PUT("conversations/{id}/admins")
    Call<Map<String, Object>> updateAdmins(
        @Path("id") String conversationId,
        @Body Map<String, Object> data
    );
    
    // ========== Conversation Settings (Phase 4A - 3E) ==========
    
    // Archive/unarchive conversation
    @PUT("conversations/{id}/archive")
    Call<Map<String, Object>> archiveConversation(
        @Path("id") String conversationId,
        @Body Map<String, Object> data
    );
    
    // Mute/unmute conversation
    @PUT("conversations/{id}/mute")
    Call<Map<String, Object>> muteConversation(
        @Path("id") String conversationId,
        @Body Map<String, Object> data
    );
    
    // Pin/unpin conversation
    @PUT("conversations/{id}/pin")
    Call<Map<String, Object>> pinConversation(
        @Path("id") String conversationId,
        @Body Map<String, Object> data
    );
    
    // Delete conversation (soft delete)
    @DELETE("conversations/{id}/user")
    Call<Map<String, Object>> deleteConversation(@Path("id") String conversationId);
    
    // Get user's conversation settings
    @GET("conversations/{id}/settings")
    Call<Map<String, Object>> getConversationSettings(@Path("id") String conversationId);
    
    // ========== Search & Filter (Phase 4A - 3F) ==========
    
    // Search conversations by name
    @GET("conversations/search")
    Call<Map<String, Object>> searchConversations(
        @Query("query") String query,
        @Query("limit") Integer limit
    );
    
    // Search messages in conversation
    @GET("conversations/{id}/messages/search")
    Call<Map<String, Object>> searchMessages(
        @Path("id") String conversationId,
        @Query("query") String query,
        @Query("limit") Integer limit,
        @Query("offset") Integer offset
    );
    
    // Filter conversations
    @GET("conversations/filter")
    Call<Map<String, Object>> filterConversations(
        @Query("type") String type,
        @Query("archived") String archived,
        @Query("muted") String muted,
        @Query("pinned") String pinned,
        @Query("limit") Integer limit
    );
}
