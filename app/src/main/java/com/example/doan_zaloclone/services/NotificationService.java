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
    
    // Call listener
    private com.example.doan_zaloclone.repository.CallRepository callRepository;
    private com.google.firebase.firestore.ListenerRegistration incomingCallListener;
    private String lastHandledCallId = null;
    
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
        
        // Setup incoming call listener
        setupIncomingCallListener();
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
    
    /**
     * Setup listener for incoming calls
     * This allows the service to receive calls even when app is in background
     */
    private void setupIncomingCallListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot setup call listener - no user logged in");
            return;
        }
        
        final String currentUserId = currentUser.getUid();
        Log.d(TAG, "Setting up incoming call listener for user: " + currentUserId);
        
        callRepository = new com.example.doan_zaloclone.repository.CallRepository();
        incomingCallListener = callRepository.listenToIncomingCalls(
                currentUserId,
                new com.example.doan_zaloclone.repository.CallRepository.OnIncomingCallListener() {
                    @Override
                    public void onIncomingCall(com.example.doan_zaloclone.models.Call call) {
                        Log.d(TAG, "Incoming call detected: " + call.getId());
                        
                        // Prevent duplicate calls
                        if (call.getId().equals(lastHandledCallId)) {
                            Log.d(TAG, "Skipping duplicate call: " + call.getId());
                            return;
                        }
                        
                        // Reject old calls (> 60 seconds)
                        if (call.getStartTime() > 0) {
                            long callAge = System.currentTimeMillis() - call.getStartTime();
                            if (callAge > 60000) {
                                Log.w(TAG, "Rejecting old call: " + call.getId() + ", age: " + callAge + "ms");
                                return;
                            }
                        }
                        
                        lastHandledCallId = call.getId();
                        Log.d(TAG, "Processing incoming call: " + call.getId());
                        
                        // Fetch caller name and launch IncomingCallService
                        fetchCallerNameAndLaunchService(call, currentUserId);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error listening for incoming calls: " + error);
                    }
                }
        );
        
        Log.d(TAG, "Incoming call listener setup complete");
    }
    
    /**
     * Fetch caller name from Firestore and launch IncomingCallService
     */
    private void fetchCallerNameAndLaunchService(com.example.doan_zaloclone.models.Call call, String currentUserId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(call.getCallerId())
                .get()
                .addOnSuccessListener(doc -> {
                    String callerName = "Unknown";
                    if (doc.exists() && doc.contains("name")) {
                        callerName = doc.getString("name");
                    }
                    
                    launchIncomingCallService(call, currentUserId, callerName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch caller name", e);
                    launchIncomingCallService(call, currentUserId, "Unknown");
                });
    }
    
    /**
     * Launch IncomingCallService with call details
     */
    private void launchIncomingCallService(com.example.doan_zaloclone.models.Call call, 
                                            String receiverId, String callerName) {
        Intent serviceIntent = new Intent(this, IncomingCallService.class);
        serviceIntent.putExtra("call_id", call.getId());
        serviceIntent.putExtra("caller_id", call.getCallerId());
        serviceIntent.putExtra("caller_name", callerName);
        serviceIntent.putExtra("receiver_id", receiverId);
        serviceIntent.putExtra("conversation_id", call.getConversationId());
        serviceIntent.putExtra("is_video", com.example.doan_zaloclone.models.Call.TYPE_VIDEO.equals(call.getType()));
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d(TAG, "Launched IncomingCallService for call: " + call.getId() + 
                ", caller: " + callerName + ", isVideo: " + 
                com.example.doan_zaloclone.models.Call.TYPE_VIDEO.equals(call.getType()));
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
        
        // Cleanup incoming call listener
        if (incomingCallListener != null) {
            incomingCallListener.remove();
            incomingCallListener = null;
            Log.d(TAG, "Incoming call listener removed");
        }
        
        // Note: We don't disconnect WebSocket here because MainActivity
        // might still be running. MainActivity will handle disconnect on its own destroy.
    }
}
