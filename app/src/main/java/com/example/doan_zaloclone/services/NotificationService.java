package com.example.doan_zaloclone.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.doan_zaloclone.utils.NotificationHelper;
import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

/**
 * Foreground service to maintain WebSocket connection and receive notifications
 * when app is in background. This ensures notifications work even when MainActivity
 * is not visible.
 * 
 * Lifecycle:
 * - Started when app launches (from MainActivity)
 * - Runs in foreground with persistent notification
 * - Stops when user logs out or app is force-killed
 */
public class NotificationService extends Service {
    private static final String TAG = "NotificationService";
    private static final int NOTIFICATION_ID = 9999;
    
    // Track active conversation to avoid notifications
    private String activeConversationId = null;
    
    // WebSocket manager
    private SocketManager socketManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService created");
        
        // Start as foreground service with persistent notification
        startForeground(NOTIFICATION_ID, 
                NotificationHelper.createServiceNotification(this));
        
        // Initialize WebSocket connection
        socketManager = SocketManager.getInstance();
        
        // Connect if not already connected
        if (!socketManager.isConnected()) {
            socketManager.connect();
        }
        
        // Setup notification listener
        setupNotificationListener();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            if ("SET_ACTIVE_CONVERSATION".equals(action)) {
                // Update active conversation ID
                activeConversationId = intent.getStringExtra("conversationId");
                Log.d(TAG, "Active conversation set to: " + activeConversationId);
            } else if ("CLEAR_ACTIVE_CONVERSATION".equals(action)) {
                // Clear active conversation ID
                activeConversationId = null;
                Log.d(TAG, "Active conversation cleared");
            }
        }
        
        // Restart service if killed
        return START_STICKY;
    }
    
    private void setupNotificationListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, stopping service");
            stopSelf();
            return;
        }
        
        final String currentUserId = currentUser.getUid();
        
        socketManager.setNotificationListener(new SocketManager.OnNotificationListener() {
            @Override
            public void onNewMessage(JSONObject messageData) {
                try {
                    String conversationId = messageData.optString("conversationId");
                    String senderId = messageData.optString("senderId");
                    String senderName = messageData.optString("senderName", "Unknown");
                    String messageText = messageData.optString("text", "");
                    String messageType = messageData.optString("type", "text");
                    
                    // Skip if from current user
                    if (senderId.equals(currentUserId)) {
                        return;
                    }
                    
                    // Skip if user is viewing this conversation
                    if (conversationId.equals(activeConversationId)) {
                        Log.d(TAG, "Skipping notification - user is in this conversation");
                        return;
                    }
                    
                    // Format message text based on type
                    String displayText = messageText;
                    if ("image".equals(messageType)) {
                        displayText = "🖼️ Hình ảnh";
                    } else if ("voice".equals(messageType)) {
                        displayText = "🎤 Tin nhắn thoại";
                    } else if ("file".equals(messageType)) {
                        displayText = "📎 Tệp tin";
                    } else if ("sticker".equals(messageType)) {
                        displayText = "😀 Nhãn dán";
                    } else if ("location".equals(messageType)) {
                        displayText = "📍 Vị trí";
                    } else if ("card".equals(messageType)) {
                        displayText = "💼 Danh thiếp";
                    }
                    
                    // Show notification
                    NotificationHelper.showMessageNotification(
                            NotificationService.this,
                            senderName,
                            displayText,
                            conversationId,
                            null
                    );
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error showing message notification", e);
                }
            }
            
            @Override
            public void onMessageRecalled(JSONObject messageData) {
                try {
                    String conversationId = messageData.optString("conversationId");
                    String senderId = messageData.optString("senderId");
                    String senderName = messageData.optString("senderName", "Unknown");
                    
                    // Skip if from current user
                    if (senderId.equals(currentUserId)) {
                        return;
                    }
                    
                    // Skip if user is viewing this conversation
                    if (conversationId.equals(activeConversationId)) {
                        return;
                    }
                    
                    NotificationHelper.showMessageRecallNotification(
                            NotificationService.this,
                            senderName,
                            conversationId
                    );
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error showing recall notification", e);
                }
            }
            
            @Override
            public void onMessageReaction(JSONObject reactionData) {
                try {
                    String conversationId = reactionData.optString("conversationId");
                    String userId = reactionData.optString("userId");
                    String reactionType = reactionData.optString("reactionType");
                    
                    // Skip if from current user
                    if (userId.equals(currentUserId)) {
                        return;
                    }
                    
                    // Skip if user is viewing this conversation
                    if (conversationId.equals(activeConversationId)) {
                        return;
                    }
                    
                    // Fetch user name and show notification
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String userName = "Unknown";
                                if (doc.exists() && doc.contains("name")) {
                                    userName = doc.getString("name");
                                }
                                
                                NotificationHelper.showMessageReactionNotification(
                                        NotificationService.this,
                                        userName,
                                        reactionType,
                                        conversationId
                                );
                            });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error showing reaction notification", e);
                }
            }
            
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                NotificationHelper.showFriendRequestNotification(
                        NotificationService.this,
                        senderName,
                        senderId,
                        null
                );
            }
            
            @Override
            public void onFriendRequestAccepted(String userId) {
                // Fetch user name and show notification
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String userName = "Unknown";
                            if (doc.exists() && doc.contains("name")) {
                                userName = doc.getString("name");
                            }
                            
                            NotificationHelper.showFriendAcceptedNotification(
                                    NotificationService.this,
                                    userName,
                                    userId
                            );
                        });
            }
        });
    }
    
    /**
     * Update the active conversation ID
     * Called from MainActivity/RoomActivity via Intent
     */
    public static void setActiveConversation(android.content.Context context, String conversationId) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction("SET_ACTIVE_CONVERSATION");
        intent.putExtra("conversationId", conversationId);
        context.startService(intent);
    }
    
    /**
     * Clear the active conversation ID
     * Called when leaving RoomActivity
     */
    public static void clearActiveConversation(android.content.Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction("CLEAR_ACTIVE_CONVERSATION");
        context.startService(intent);
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationService destroyed");
        
        // Note: We don't disconnect WebSocket here because MainActivity
        // might still be running. MainActivity will handle disconnect on its own destroy.
    }
}
