package com.example.doan_zaloclone.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.doan_zaloclone.api.RetrofitClient;
import com.example.doan_zaloclone.api.models.ApiResponse;
import com.example.doan_zaloclone.api.ApiService;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;

/**
 * Repository class for handling Firebase Authentication operations
 * Updated to use ExecutorService for sequential processing
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final ApiService apiService;
    private final ExecutorService backgroundExecutor;
    private final Handler mainHandler;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.apiService = RetrofitClient.getApiService();
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
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
    /**
     * Login with email and password
     */
    public void login(String email, String password, AuthCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                // 1. Authenticate with Firebase
                AuthResult authResult = Tasks.await(firebaseAuth.signInWithEmailAndPassword(email, password));
                FirebaseUser user = authResult.getUser();

                if (user != null) {
                    // 2. Check Banned Status synchronously
                    DocumentSnapshot document = Tasks.await(firestore.collection("users").document(user.getUid()).get());
                    
                    if (document.exists()) {
                        Boolean isBanned = document.getBoolean("isBanned");
                        if (isBanned != null && isBanned) {
                            // User is banned
                            firebaseAuth.signOut();
                            mainHandler.post(() -> callback.onError("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ admin."));
                            return;
                        }
                    }

                    // 3. Update Online Status
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isOnline", true);
                    updates.put("lastSeen", System.currentTimeMillis());
                    
                    // Best effort update, don't block heavily on it or fail login if it fails
                    try {
                        Tasks.await(firestore.collection("users").document(user.getUid()).update(updates));
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to update online status on login", e);
                    }

                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    mainHandler.post(() -> callback.onError("Login failed: User is null"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                // Unwrap ExecutionException if needed for cleaner messages
                String msg = e.getMessage();
                if (e.getCause() != null) {
                    msg = e.getCause().getMessage();
                }
                String finalMsg = msg != null ? msg : "Login error";
                mainHandler.post(() -> callback.onError(finalMsg));
            }
        });
    }

    /**
     * Register new user with email and password
     */
    /**
     * Register new user with email and password
     */
    public void register(String name, String email, String password, AuthCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                // 1. Create User
                AuthResult authResult = Tasks.await(firebaseAuth.createUserWithEmailAndPassword(email, password));
                FirebaseUser user = authResult.getUser();

                if (user != null) {
                    // 2. Prepare User Entity
                    String normalizedEmail = email.trim().toLowerCase();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("userId", user.getUid());
                    userData.put("name", name);
                    userData.put("email", normalizedEmail);
                    userData.put("createdAt", System.currentTimeMillis());
                    userData.put("isOnline", true);
                    userData.put("lastSeen", System.currentTimeMillis());

                    // 3. Save to Firestore
                    Tasks.await(firestore.collection("users").document(user.getUid()).set(userData));
                    
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    mainHandler.post(() -> callback.onError("Registration failed: User is null"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Registration failed", e);
                String msg = e.getMessage();
                if (e.getCause() != null) {
                    msg = e.getCause().getMessage();
                }
                String finalMsg = msg != null ? msg : "Registration error";
                mainHandler.post(() -> callback.onError(finalMsg));
            }
        });
    }

    /**
     * Logout current user
     */
    /**
     * Logout current user
     */
    public void logout(LogoutCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Update online status to false via API (which also updates Firestore)
                    Map<String, Boolean> statusUpdate = new HashMap<>();
                    statusUpdate.put("isOnline", false);
                    
                    try {
                        Call<ApiResponse<Void>> call = apiService.updateUserStatus(user.getUid(), statusUpdate);
                        call.execute(); // Synchronous call
                        Log.d(TAG, "✅ Updated user status to offline via API");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to set offline status via API on logout, trying Firestore", e);
                        
                        // Fallback to direct Firestore update
                        try {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("isOnline", false);
                            updates.put("lastSeen", System.currentTimeMillis());
                            Tasks.await(firestore.collection("users").document(user.getUid()).update(updates));
                        } catch (Exception ex) {
                            Log.w(TAG, "Failed to set offline status on logout (Firestore fallback)", ex);
                        }
                    }
                }
                
                firebaseAuth.signOut();
                if (callback != null) {
                    mainHandler.post(callback::onLogoutComplete);
                }
            } catch (Exception e) {
                Log.e(TAG, "Logout error", e);
                // Even on error, we try to ensure signout happens or callback is called
                if (callback != null) {
                    mainHandler.post(callback::onLogoutComplete);
                }
            }
        });
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
