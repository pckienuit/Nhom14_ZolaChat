package com.example.doan_zaloclone.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirestoreManager - Quản lý các thao tác với Firestore Database
 * Chiến lược: Cloud-First (đồng bộ đa thiết bị)
 */
public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_CONVERSATIONS = "conversations";
    private static final String COLLECTION_FRIEND_REQUESTS = "friendRequests";

    private final FirebaseFirestore db;
    private static FirestoreManager instance;

    // Singleton pattern
    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    /**
     * Tạo User mới trong Firestore
     * Được gọi ngay sau khi đăng ký Auth thành công
     *
     * @param userId   ID của user (từ Firebase Authentication)
     * @param email    Email của user
     * @param name     Tên hiển thị của user
     * @param listener Callback để xử lý kết quả
     */
    public void createNewUser(@NonNull String userId,
                              @NonNull String email,
                              @NonNull String name,
                              @NonNull OnUserCreatedListener listener) {
        createNewUser(userId, email, name, null, listener);
    }

    /**
     * Tạo User mới trong Firestore với device token
     *
     * @param userId      ID của user (từ Firebase Authentication)
     * @param email       Email của user
     * @param name        Tên hiển thị của user
     * @param deviceToken Token thiết bị hiện tại (có thể null)
     * @param listener    Callback để xử lý kết quả
     */
    public void createNewUser(@NonNull String userId,
                              @NonNull String email,
                              @NonNull String name,
                              String deviceToken,
                              @NonNull OnUserCreatedListener listener) {
        // Normalize email to lowercase for consistent searching
        String normalizedEmail = email.trim().toLowerCase();
        
        // Create User object
        User user = new User(userId, name, normalizedEmail, ""); // avatarUrl empty initially

        // Add device token if available
        if (deviceToken != null && !deviceToken.isEmpty()) {
            user.addDevice(deviceToken);
        }

        // Save to Firestore
        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User created successfully: " + userId + " with email: " + normalizedEmail);
                        listener.onSuccess(user);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating user: " + userId, e);
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Thêm device token cho user hiện tại
     *
     * @param userId      ID của user
     * @param deviceToken Token thiết bị cần thêm
     * @param listener    Callback để xử lý kết quả
     */
    public void addDeviceToken(@NonNull String userId,
                               @NonNull String deviceToken,
                               @NonNull OnDeviceTokenUpdatedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("devices." + deviceToken, true);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Device token added successfully for user: " + userId);
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error adding device token for user: " + userId, e);
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Xóa device token của user
     *
     * @param userId      ID của user
     * @param deviceToken Token thiết bị cần xóa
     * @param listener    Callback để xử lý kết quả
     */
    public void removeDeviceToken(@NonNull String userId,
                                  @NonNull String deviceToken,
                                  @NonNull OnDeviceTokenUpdatedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("devices." + deviceToken, com.google.firebase.firestore.FieldValue.delete());

        db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Device token removed successfully for user: " + userId);
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error removing device token for user: " + userId, e);
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Lấy reference đến Firestore instance
     */
    public FirebaseFirestore getFirestore() {
        return db;
    }

    /**
     * Search users by name or email (partial match)
     * Uses client-side filtering for flexibility
     *
     * @param query    Search query (name or email fragment)
     * @param listener Callback để xử lý kết quả
     */
    public void searchUsers(@NonNull String query,
                           @NonNull OnUserSearchListener listener) {
        String searchQuery = query.trim().toLowerCase();
        Log.d(TAG, "Searching users with query: " + searchQuery);
        
        if (searchQuery.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }
        
        // Fetch all users and filter client-side
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        Log.d(TAG, "Fetched " + querySnapshot.size() + " total users");
                        List<User> allUsers = new ArrayList<>();
                        
                        // Parse all users
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setId(document.getId());  // Set document ID
                                allUsers.add(user);
                            }
                        }
                        
                        // Filter by query (name or email contains)
                        List<User> matchedUsers = new ArrayList<>();
                        for (User user : allUsers) {
                            String userName = user.getName() != null ? user.getName().toLowerCase() : "";
                            String userEmail = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                            
                            if (userName.contains(searchQuery) || userEmail.contains(searchQuery)) {
                                matchedUsers.add(user);
                            }
                        }
                        
                        // Sort: exact matches first, then partial matches
                        matchedUsers.sort((u1, u2) -> {
                            String name1 = u1.getName() != null ? u1.getName().toLowerCase() : "";
                            String email1 = u1.getEmail() != null ? u1.getEmail().toLowerCase() : "";
                            String name2 = u2.getName() != null ? u2.getName().toLowerCase() : "";
                            String email2 = u2.getEmail() != null ? u2.getEmail().toLowerCase() : "";
                            
                            boolean exact1 = name1.equals(searchQuery) || email1.equals(searchQuery);
                            boolean exact2 = name2.equals(searchQuery) || email2.equals(searchQuery);
                            
                            if (exact1 && !exact2) return -1;
                            if (!exact1 && exact2) return 1;
                            return name1.compareTo(name2);
                        });
                        
                        Log.d(TAG, "Found " + matchedUsers.size() + " matching users");
                        listener.onSuccess(matchedUsers);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error searching users: " + searchQuery, e);
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Kiểm tra xem đã có conversation giữa 2 users chưa
     *
     * @param currentUserId ID của user hiện tại
     * @param otherUserId   ID của user kia
     * @param listener      Callback để xử lý kết quả
     */
    public void findExistingConversation(@NonNull String currentUserId,
                                        @NonNull String otherUserId,
                                        @NonNull OnConversationFoundListener listener) {
        // Tìm conversations có chứa cả 2 user IDs
        db.collection(COLLECTION_CONVERSATIONS)
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        // Lọc thêm để tìm conversation có cả 2 users
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Conversation conversation = document.toObject(Conversation.class);
                            if (conversation != null && 
                                conversation.getMemberIds() != null &&
                                conversation.getMemberIds().contains(otherUserId)) {
                                // Tìm thấy conversation
                                Log.d(TAG, "Found existing conversation: " + conversation.getId());
                                listener.onFound(conversation);
                                return;
                            }
                        }
                        // Không tìm thấy
                        Log.d(TAG, "No existing conversation found");
                        listener.onNotFound();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error finding conversation", e);
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Tạo conversation mới giữa 2 users
     *
     * @param currentUserId   ID của user hiện tại
     * @param currentUserName Tên của user hiện tại
     * @param otherUserId     ID của user kia
     * @param otherUserName   Tên của user kia
     * @param listener        Callback để xử lý kết quả
     */
    public void createConversation(@NonNull String currentUserId,
                                  @NonNull String currentUserName,
                                  @NonNull String otherUserId,
                                  @NonNull String otherUserName,
                                  @NonNull OnConversationCreatedListener listener) {
        // Tạo conversation mới
        DocumentReference newConversationRef = db.collection(COLLECTION_CONVERSATIONS).document();
        String conversationId = newConversationRef.getId();
        
        List<String> memberIds = Arrays.asList(currentUserId, otherUserId);
        
        // Create conversation data with memberNames
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("id", conversationId);
        conversationData.put("memberIds", memberIds);
        
        // Store member names for easier access
        Map<String, String> memberNames = new HashMap<>();
        memberNames.put(currentUserId, currentUserName);
        memberNames.put(otherUserId, otherUserName);
        conversationData.put("memberNames", memberNames);
        
        // For 1-1 chats, name is left empty
        conversationData.put("name", "");
        conversationData.put("lastMessage", "");
        conversationData.put("timestamp", System.currentTimeMillis());

        newConversationRef.set(conversationData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Conversation created successfully: " + conversationId + 
                             " between " + currentUserName + " and " + otherUserName);
                        
                        // Create conversation object for callback
                        Conversation conversation = new Conversation(
                                conversationId,
                                "",
                                "",
                                System.currentTimeMillis(),
                                memberIds
                        );
                        conversation.setMemberNames(memberNames);
                        
                        listener.onSuccess(conversation);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating conversation", e);
                        listener.onFailure(e);
                    }
                });
    }

    // Callback Interfaces

    /**
     * Listener cho việc tạo user mới
     */
    public interface OnUserCreatedListener {
        void onSuccess(User user);

        void onFailure(Exception e);
    }

    /**
     * Listener cho việc cập nhật device token
     */
    public interface OnDeviceTokenUpdatedListener {
        void onSuccess();

        void onFailure(Exception e);
    }

    /**
     * Listener cho việc tìm kiếm user
     */
    public interface OnUserSearchListener {
        void onSuccess(List<User> users);

        void onFailure(Exception e);
    }

    /**
     * Listener cho việc tìm conversation
     */
    public interface OnConversationFoundListener {
        void onFound(Conversation conversation);

        void onNotFound();

        void onFailure(Exception e);
    }

    /**
     * Listener cho việc tạo conversation
     */
    public interface OnConversationCreatedListener {
        void onSuccess(Conversation conversation);

        void onFailure(Exception e);
    }

    /**
     * Listener cho việc lắng nghe conversations realtime
     */
    public interface OnConversationsChangedListener {
        void onConversationsChanged(List<Conversation> conversations);

        void onFailure(Exception e);
    }

    /**
     * Lắng nghe conversations của user realtime
     *
     * @param userId   ID của user
     * @param listener Callback để xử lý kết quả
     * @return ListenerRegistration để cleanup
     */
    public com.google.firebase.firestore.ListenerRegistration listenToConversations(
            @NonNull String userId,
            @NonNull OnConversationsChangedListener listener) {
        return db.collection(COLLECTION_CONVERSATIONS)
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot querySnapshot,
                                        com.google.firebase.firestore.FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "Error listening to conversations", e);
                            listener.onFailure(e);
                            return;
                        }

                        if (querySnapshot != null) {
                            List<Conversation> conversations = new ArrayList<>();
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Conversation conversation = document.toObject(Conversation.class);
                                if (conversation != null) {
                                    conversation.setId(document.getId());  // Set document ID
                                    conversations.add(conversation);
                                }
                            }
                            // Sort by timestamp descending (newest first)
                            conversations.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                            Log.d(TAG, "Loaded " + conversations.size() + " conversations for user: " + userId);
                            listener.onConversationsChanged(conversations);
                        }
                    }
                });
    }

    // ===================== FRIEND REQUEST METHODS =====================

    /**
     * Send friend request to another user
     */
    public void sendFriendRequest(@NonNull String fromUserId,
                                  @NonNull String fromUserName,
                                  @NonNull String fromUserEmail,
                                  @NonNull String toUserId,
                                  @NonNull OnFriendRequestListener listener) {
        // Check if request already exists
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereIn("status", Arrays.asList("PENDING", "ACCEPTED"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        listener.onFailure(new Exception("Friend request already exists"));
                        return;
                    }

                    // Create new friend request
                    DocumentReference requestRef = db.collection(COLLECTION_FRIEND_REQUESTS).document();
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("id", requestRef.getId());
                    requestData.put("fromUserId", fromUserId);
                    requestData.put("toUserId", toUserId);
                    requestData.put("fromUserName", fromUserName);
                    requestData.put("fromUserEmail", fromUserEmail);
                    requestData.put("status", "PENDING");
                    requestData.put("timestamp", System.currentTimeMillis());

                    requestRef.set(requestData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Friend request sent: " + requestRef.getId());
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sending friend request", e);
                                listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing request", e);
                    listener.onFailure(e);
                });
    }

    /**
     * Accept friend request and auto-create conversation
     */
    public void acceptFriendRequest(@NonNull String requestId,
                                    @NonNull OnFriendRequestListener listener) {
        // First get the request details
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .document(requestId)
                .get()
                .addOnSuccessListener(requestDoc -> {
                    if (!requestDoc.exists()) {
                        listener.onFailure(new Exception("Request not found"));
                        return;
                    }
                    
                    String fromUserId = requestDoc.getString("fromUserId");
                    String toUserId = requestDoc.getString("toUserId");
                    String fromUserName = requestDoc.getString("fromUserName");
                    
                    // Update status to ACCEPTED
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "ACCEPTED");
                    
                    db.collection(COLLECTION_FRIEND_REQUESTS)
                            .document(requestId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Friend request accepted: " + requestId);
                                
                                // Create conversation
                                createFriendConversation(fromUserId, toUserId, fromUserName, listener);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error accepting friend request", e);
                                listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching request", e);
                    listener.onFailure(e);
                });
    }

    /**
     * Create conversation when friends accept request
     * Fetches both users' details to properly set conversation info
     */
    private void createFriendConversation(String user1Id, String user2Id, 
                                         String user1Name, OnFriendRequestListener listener) {
        // Check if conversation already exists
        findExistingConversation(user1Id, user2Id, new OnConversationFoundListener() {
            @Override
            public void onFound(Conversation conversation) {
                // Conversation exists, just notify success
                Log.d(TAG, "Conversation already exists: " + conversation.getId());
                listener.onSuccess();
            }

            @Override
            public void onNotFound() {
                // Fetch both users' full details
                db.collection(COLLECTION_USERS).document(user1Id).get()
                    .addOnSuccessListener(user1Doc -> {
                        db.collection(COLLECTION_USERS).document(user2Id).get()
                            .addOnSuccessListener(user2Doc -> {
                                User user1 = user1Doc.toObject(User.class);
                                User user2 = user2Doc.toObject(User.class);
                                
                                if (user1 == null || user2 == null) {
                                    Log.e(TAG, "Failed to fetch user details for conversation");
                                    listener.onFailure(new Exception("User details not found"));
                                    return;
                                }
                                
                                // Create new conversation with proper user info
                                DocumentReference convRef = db.collection(COLLECTION_CONVERSATIONS).document();
                                
                                // For a 1-1 conversation, we store it as a single conversation object
                                // The app will display the OTHER user's name based on who's viewing
                                Map<String, Object> conversationData = new HashMap<>();
                                conversationData.put("id", convRef.getId());
                                conversationData.put("memberIds", Arrays.asList(user1Id, user2Id));
                                
                                // Store member names for easier access
                                Map<String, String> memberNames = new HashMap<>();
                                memberNames.put(user1Id, user1.getName());
                                memberNames.put(user2Id, user2.getName());
                                conversationData.put("memberNames", memberNames);
                                
                                // For 1-1 chats, name is typically left empty or set as the conversation ID
                                // The UI will display the other user's name
                                conversationData.put("name", "");
                                
                                // Set a friendly first message
                                conversationData.put("lastMessage", "Các bạn đã là bạn bè. Hãy bắt đầu trò chuyện!");
                                conversationData.put("timestamp", System.currentTimeMillis());
                                
                                convRef.set(conversationData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Friend conversation created: " + convRef.getId() + 
                                                 " between " + user1.getName() + " and " + user2.getName());
                                            listener.onSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Failed to create conversation, but friendship accepted", e);
                                            listener.onSuccess();  // Still success since friendship is accepted
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fetch user2 details", e);
                                listener.onFailure(e);
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user1 details", e);
                        listener.onFailure(e);
                    });
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Error checking existing conversation", e);
                listener.onSuccess();  // Still success
            }
        });
    }

    /**
     * Reject friend request
     */
    public void rejectFriendRequest(@NonNull String requestId,
                                    @NonNull OnFriendRequestListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "REJECTED");

        db.collection(COLLECTION_FRIEND_REQUESTS)
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Friend request rejected: " + requestId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error rejecting friend request", e);
                    listener.onFailure(e);
                });
    }

    /**
     * Listen to incoming friend requests realtime
     */
    public com.google.firebase.firestore.ListenerRegistration listenToFriendRequests(
            @NonNull String userId,
            @NonNull OnFriendRequestsChangedListener listener) {
        return db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to friend requests", e);
                        listener.onFailure(e);
                        return;
                    }

                    if (querySnapshot != null) {
                        List<com.example.doan_zaloclone.models.FriendRequest> requests = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            com.example.doan_zaloclone.models.FriendRequest request = 
                                document.toObject(com.example.doan_zaloclone.models.FriendRequest.class);
                            if (request != null) {
                                request.setId(document.getId());  // Set document ID for DiffUtil comparison
                                requests.add(request);
                            }
                        }
                        Log.d(TAG, "Loaded " + requests.size() + " pending friend requests");
                        listener.onFriendRequestsChanged(requests);
                    }
                });
    }

    /**
     * Check friend request status between two users
     */
    public void checkFriendRequestStatus(@NonNull String fromUserId,
                                        @NonNull String toUserId,
                                        @NonNull OnFriendRequestStatusListener listener) {
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onStatus("NONE");
                    } else {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String status = doc.getString("status");
                        listener.onStatus(status != null ? status : "NONE");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking friend request status", e);
                    listener.onFailure(e);
                });
    }

    /**
     * Get list of friends for a user
     * Queries accepted friend requests and fetches user details
     */
    public void getFriends(@NonNull String userId,
                          @NonNull OnFriendsLoadedListener listener) {
        Log.d(TAG, "Loading friends for user: " + userId);
        
        // Query friend requests where user is involved and status is ACCEPTED
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Use Set to avoid duplicates (in case of multiple requests between same users)
                    java.util.Set<String> friendIdsSet = new java.util.HashSet<>();
                    
                    // Extract friend IDs
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String fromUserId = doc.getString("fromUserId");
                        String toUserId = doc.getString("toUserId");
                        
                        if (userId.equals(fromUserId)) {
                            friendIdsSet.add(toUserId);
                        } else if (userId.equals(toUserId)) {
                            friendIdsSet.add(fromUserId);
                        }
                    }
                    
                    List<String> friendIds = new ArrayList<>(friendIdsSet);
                    
                    if (friendIds.isEmpty()) {
                        Log.d(TAG, "No friends found");
                        listener.onFriendsLoaded(new ArrayList<>());
                        return;
                    }
                    
                    Log.d(TAG, "Found " + friendIds.size() + " unique friend(s)");
                    
                    // Fetch user details for each friend
                    List<User> friends = new ArrayList<>();
                    int[] fetchedCount = {0};
                    
                    for (String friendId : friendIds) {
                        db.collection(COLLECTION_USERS)
                                .document(friendId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    User friend = userDoc.toObject(User.class);
                                    if (friend != null) {
                                        friend.setId(userDoc.getId());  // Set document ID
                                        friends.add(friend);
                                    }
                                    
                                    fetchedCount[0]++;
                                    if (fetchedCount[0] == friendIds.size()) {
                                        // All friends fetched
                                        friends.sort((f1, f2) -> {
                                            String name1 = f1.getName() != null ? f1.getName() : "";
                                            String name2 = f2.getName() != null ? f2.getName() : "";
                                            return name1.compareTo(name2);
                                        });
                                        Log.d(TAG, "Loaded " + friends.size() + " friends");
                                        listener.onFriendsLoaded(friends);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to fetch friend: " + friendId, e);
                                    fetchedCount[0]++;
                                    if (fetchedCount[0] == friendIds.size()) {
                                        listener.onFriendsLoaded(friends);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading friends", e);
                    listener.onFailure(e);
                });
    }

    // Callback Interfaces for Friend Requests

    public interface OnFriendRequestListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnFriendRequestsChangedListener {
        void onFriendRequestsChanged(List<com.example.doan_zaloclone.models.FriendRequest> requests);
        void onFailure(Exception e);
    }

    public interface OnFriendRequestStatusListener {
        void onStatus(String status);
        void onFailure(Exception e);
    }
    
    /**
     * Check if two users are friends
     */
    public void checkFriendship(@NonNull String user1Id,
                               @NonNull String user2Id,
                               @NonNull OnFriendshipCheckListener listener) {
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean areFriends = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String from = doc.getString("fromUserId");
                        String to = doc.getString("toUserId");
                        
                        if ((user1Id.equals(from) && user2Id.equals(to)) ||
                            (user1Id.equals(to) && user2Id.equals(from))) {
                            areFriends = true;
                            break;
                        }
                    }
                    Log.d(TAG, "Friendship check: " + user1Id + " and " + user2Id + " = " + areFriends);
                    listener.onResult(areFriends);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking friendship", e);
                    listener.onFailure(e);
                });
    }
    
    public interface OnFriendsLoadedListener {
        void onFriendsLoaded(List<User> friends);
        void onFailure(Exception e);
    }
    
    /**
     * Listener for realtime friend list updates
     */
    public interface OnFriendsChangedListener {
        void onFriendsChanged(List<User> friends);
        void onFailure(Exception e);
    }
    
    public interface OnFriendshipCheckListener {
        void onResult(boolean areFriends);
        void onFailure(Exception e);
    }
    
    public interface OnFriendRemovedListener {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    /**
     * Remove a friend (unfriend)
     * Updates the friend request status from ACCEPTED to REMOVED
     * Conversation is preserved to maintain chat history
     * 
     * @param user1Id First user ID
     * @param user2Id Second user ID
     * @param listener Callback for result
     */
    public void removeFriend(@NonNull String user1Id,
                            @NonNull String user2Id,
                            @NonNull OnFriendRemovedListener listener) {
        Log.d(TAG, "Removing friend relationship between: " + user1Id + " and " + user2Id);
        
        // Find the accepted friend request between the two users
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean found = false;
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String fromUserId = doc.getString("fromUserId");
                        String toUserId = doc.getString("toUserId");
                        
                        // Check if this is the friend request between the two users
                        if ((user1Id.equals(fromUserId) && user2Id.equals(toUserId)) ||
                            (user1Id.equals(toUserId) && user2Id.equals(fromUserId))) {
                            
                            // Update status to REMOVED (preserve for history)
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "REMOVED");
                            updates.put("removedAt", System.currentTimeMillis());
                            
                            db.collection(COLLECTION_FRIEND_REQUESTS)
                                    .document(doc.getId())
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Friend removed successfully: " + doc.getId());
                                        listener.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error removing friend", e);
                                        listener.onFailure(e);
                                    });
                            
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        Log.w(TAG, "No friend relationship found between users");
                        listener.onFailure(new Exception("Không tìm thấy mối quan hệ bạn bè"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding friend request", e);
                    listener.onFailure(e);
                });
    }
    
    /**
     * Listen to friends list with realtime updates
     * Sets up a snapshot listener on friend requests with ACCEPTED status
     */
    public com.google.firebase.firestore.ListenerRegistration listenToFriends(
            @NonNull String userId,
            @NonNull OnFriendsChangedListener listener) {
        return db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to friends", e);
                        listener.onFailure(e);
                        return;
                    }

                    if (querySnapshot != null) {
                        // Use Set to avoid duplicates
                        java.util.Set<String> friendIdsSet = new java.util.HashSet<>();
                        
                        // Extract friend IDs
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String fromUserId = doc.getString("fromUserId");
                            String toUserId = doc.getString("toUserId");
                            
                            if (userId.equals(fromUserId)) {
                                friendIdsSet.add(toUserId);
                            } else if (userId.equals(toUserId)) {
                                friendIdsSet.add(fromUserId);
                            }
                        }
                        
                        List<String> friendIds = new ArrayList<>(friendIdsSet);
                        
                        if (friendIds.isEmpty()) {
                            Log.d(TAG, "No friends found (realtime)");
                            listener.onFriendsChanged(new ArrayList<>());
                            return;
                        }
                        
                        Log.d(TAG, "Found " + friendIds.size() + " unique friend(s) (realtime)");
                        
                        // Fetch user details for each friend
                        List<User> friends = new ArrayList<>();
                        int[] fetchedCount = {0};
                        
                        for (String friendId : friendIds) {
                            db.collection(COLLECTION_USERS)
                                    .document(friendId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        User friend = userDoc.toObject(User.class);
                                        if (friend != null) {
                                            friend.setId(userDoc.getId());
                                            friends.add(friend);
                                        }
                                        
                                        fetchedCount[0]++;
                                        if (fetchedCount[0] == friendIds.size()) {
                                            // All friends fetched, sort and notify
                                            friends.sort((f1, f2) -> {
                                                String name1 = f1.getName() != null ? f1.getName() : "";
                                                String name2 = f2.getName() != null ? f2.getName() : "";
                                                return name1.compareTo(name2);
                                            });
                                            Log.d(TAG, "Loaded " + friends.size() + " friends (realtime)");
                                            listener.onFriendsChanged(friends);
                                        }
                                    })
                                    .addOnFailureListener(error -> {
                                        Log.w(TAG, "Failed to fetch friend: " + friendId, error);
                                        fetchedCount[0]++;
                                        if (fetchedCount[0] == friendIds.size()) {
                                            listener.onFriendsChanged(friends);
                                        }
                                    });
                        }
                    }
                });
    }
}
