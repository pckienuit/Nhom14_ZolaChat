package com.example.doan_zaloclone.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository class for handling Firebase Authentication operations
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get current authenticated user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Login with email and password
     */
    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        String errorMessage = task.getException() != null 
                                ? task.getException().getMessage() 
                                : "Login failed";
                        callback.onError(errorMessage);
                    }
                });
    }

    /**
     * Register new user with email and password
     */
    public void register(String name, String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            // Save user info to Firestore
                            saveUserToFirestore(user.getUid(), name, email, callback);
                        } else {
                            callback.onError("Registration failed");
                        }
                    } else {
                        String errorMessage = task.getException() != null 
                                ? task.getException().getMessage() 
                                : "Registration failed";
                        callback.onError(errorMessage);
                    }
                });
    }

    /**
     * Save user information to Firestore
     */
    private void saveUserToFirestore(String userId, String name, String email, AuthCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        user.put("isOnline", true);
        user.put("lastSeen", System.currentTimeMillis());

        firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to save user data";
                    callback.onError(errorMessage);
                });
    }

    /**
     * Logout current user
     */
    public void logout(LogoutCallback callback) {
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            
            // Update online status before logout
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", false);
            updates.put("lastSeen", System.currentTimeMillis());
            
            // Try to update Firestore, but don't block logout if it fails
            firestore.collection("users").document(userId)
                    .update(updates)
                    .addOnCompleteListener(task -> {
                        // Sign out regardless of Firestore update result
                        firebaseAuth.signOut();
                        if (callback != null) {
                            callback.onLogoutComplete();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If Firestore fails, still logout from Firebase Auth
                        firebaseAuth.signOut();
                        if (callback != null) {
                            callback.onLogoutComplete();
                        }
                    });
        } else {
            firebaseAuth.signOut();
            if (callback != null) {
                callback.onLogoutComplete();
            }
        }
    }
    
    /**
     * Logout current user (synchronous version)
     */
    public void logout() {
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            
            // Update online status before logout
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", false);
            updates.put("lastSeen", System.currentTimeMillis());
            
            firestore.collection("users").document(userId)
                    .update(updates);
        }
        // Force immediate sign out
        firebaseAuth.signOut();
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Callback interface for authentication operations
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String error);
    }
    
    /**
     * Callback interface for logout operations
     */
    public interface LogoutCallback {
        void onLogoutComplete();
    }
}
