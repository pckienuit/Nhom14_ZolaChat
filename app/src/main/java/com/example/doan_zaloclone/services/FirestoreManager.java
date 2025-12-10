package com.example.doan_zaloclone.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.doan_zaloclone.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
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
}
