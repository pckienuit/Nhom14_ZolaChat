package com.example.doan_zaloclone.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.doan_zaloclone.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Observes the application lifecycle to track user online/offline status.
 * Uses ProcessLifecycleOwner to detect when the entire app goes to foreground/background.
 */
public class AppLifecycleObserver implements DefaultLifecycleObserver {
    
    private static final String TAG = "AppLifecycleObserver";
    private static final long HEARTBEAT_INTERVAL_MS = 2 * 60 * 1000; // 2 minutes
    
    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private android.os.Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private boolean isForeground = false;
    
    public AppLifecycleObserver() {
        this.userRepository = new UserRepository();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.heartbeatHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        // Heartbeat task: Update lastSeen periodically
        this.heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isForeground) {
                    updateLastSeen();
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
                }
            }
        };
    }
    
    /**
     * Called when the app comes to the foreground.
     * Sets the user's online status to true and starts heartbeat.
     */
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App entered foreground");
        isForeground = true;
        updateOnlineStatus(true);
        
        // Start heartbeat immediately
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        heartbeatHandler.post(heartbeatRunnable);
    }
    
    /**
     * Called when the app goes to the background.
     * Sets the user's online status to false and stops heartbeat.
     */
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App entered background");
        isForeground = false;
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        updateOnlineStatus(false);
    }
    
    /**
     * Updates only the lastSeen timestamp (heartbeat)
     */
    private void updateLastSeen() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        // Just update timestamp, keep isOnline=true
        userRepository.updateUserStatus(userId, true, new UserRepository.OnUpdateListener() {
            @Override
            public void onSuccess() {
                Log.v(TAG, "Heartbeat sent for user " + userId);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Heartbeat failed: " + error);
            }
        });
    }
    
    /**
     * Updates the user's online status in Firestore.
     * @param isOnline true if user is online, false otherwise
     */
    private void updateOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping status update");
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d(TAG, "Updating status for user " + userId + " to " + (isOnline ? "ONLINE" : "OFFLINE"));
        
        userRepository.updateUserStatus(userId, isOnline, new UserRepository.OnUpdateListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Status updated successfully: " + (isOnline ? "ONLINE" : "OFFLINE"));
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to update status: " + error);
            }
        });
    }
}
