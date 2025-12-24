package com.example.doan_zaloclone.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for user-related operations
 * Handles user data fetching and updates using Firestore
 */
public class UserRepository {
    
    private final FirebaseFirestore firestore;
    private final FirestoreManager firestoreManager;
    
    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.firestoreManager = FirestoreManager.getInstance();
    }
    
    /**
     * Get user by ID
     * @param userId ID of the user to fetch
     * @return LiveData containing Resource with User data
     */
    public LiveData<Resource<User>> getUser(@NonNull String userId) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        result.setValue(Resource.success(user));
                    } else {
                        result.setValue(Resource.error("User not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to fetch user";
                    result.setValue(Resource.error(errorMessage));
                });
        
        return result;
    }
    
    /**
     * Get username by user ID
     * @param userId ID of the user
     * @return LiveData containing Resource with username string
     */
    public LiveData<Resource<String>> getUserName(@NonNull String userId) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            result.setValue(Resource.success(name));
                        } else {
                            result.setValue(Resource.success("User"));
                        }
                    } else {
                        result.setValue(Resource.success("User"));
                    }
                })
                .addOnFailureListener(e -> {
                    // On error, return default "User" instead of failing
                    result.setValue(Resource.success("User"));
                });
        
        return result;
    }
    
    /**
     * Update user information
     * @param userId ID of the user to update
     * @param updates Map of field updates
     * @return LiveData containing Resource with update success status
     */
    public LiveData<Resource<Boolean>> updateUser(@NonNull String userId, 
                                                   @NonNull java.util.Map<String, Object> updates) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestore.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(true)))
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to update user";
                    result.setValue(Resource.error(errorMessage));
                });
        
        return result;
    }
    
    // Callback interfaces
    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(String error);
    }
    
    public interface OnUpdateListener {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Get user by ID with callback
     */
    public void getUserById(@NonNull String userId, @NonNull OnUserLoadedListener listener) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserLoaded(user);
                    } else {
                        listener.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update user name
     */
    public void updateUserName(@NonNull String userId, @NonNull String newName, 
                               @NonNull OnUpdateListener listener) {
        firestore.collection("users")
                .document(userId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update user bio
     */
    public void updateUserBio(@NonNull String userId, @NonNull String newBio, 
                              @NonNull OnUpdateListener listener) {
        firestore.collection("users")
                .document(userId)
                .update("bio", newBio)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update user avatar URL
     */
    public void updateUserAvatar(@NonNull String userId, @NonNull String avatarUrl, 
                                 @NonNull OnUpdateListener listener) {
        firestore.collection("users")
                .document(userId)
                .update("avatarUrl", avatarUrl)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update user cover URL
     */
    public void updateUserCover(@NonNull String userId, @NonNull String coverUrl, 
                                @NonNull OnUpdateListener listener) {
        firestore.collection("users")
                .document(userId)
                .update("coverUrl", coverUrl)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update user online status and lastSeen timestamp.
     * Called when app goes to foreground (online) or background (offline).
     * 
     * @param userId ID of the user
     * @param isOnline true if user is online, false if offline
     * @param listener Callback for result
     */
    public void updateUserStatus(@NonNull String userId, boolean isOnline, 
                                 @NonNull OnUpdateListener listener) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("isOnline", isOnline);
        updates.put("lastSeen", System.currentTimeMillis());
        
        firestore.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
