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
        // Tạo User object
        User user = new User(userId, name, email, ""); // avatarUrl rỗng ban đầu

        // Thêm device token nếu có
        if (deviceToken != null && !deviceToken.isEmpty()) {
            user.addDevice(deviceToken);
        }

        // Lưu vào Firestore
        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User created successfully: " + userId);
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
     * Tìm kiếm user theo email
     *
     * @param email    Email cần tìm
     * @param listener Callback để xử lý kết quả
     */
    public void searchUserByEmail(@NonNull String email,
                                  @NonNull OnUserSearchListener listener) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("email", email.trim().toLowerCase())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        List<User> users = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                        Log.d(TAG, "Found " + users.size() + " users with email: " + email);
                        listener.onSuccess(users);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error searching user by email: " + email, e);
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
        
        Conversation conversation = new Conversation(
                conversationId,
                otherUserName, // Tên hiển thị là tên của user kia
                "",
                System.currentTimeMillis(),
                memberIds
        );

        newConversationRef.set(conversation)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Conversation created successfully: " + conversationId);
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
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
                                    conversations.add(conversation);
                                }
                            }
                            Log.d(TAG, "Loaded " + conversations.size() + " conversations for user: " + userId);
                            listener.onConversationsChanged(conversations);
                        }
                    }
                });
    }
}
