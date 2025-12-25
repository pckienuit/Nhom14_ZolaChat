package com.example.doan_zaloclone.websocket;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * WebSocket Manager for Real-time Messaging
 * Singleton pattern - manages Socket.IO connection to backend server
 */
public class SocketManager {
    private static final String TAG = "SocketManager";
    
    // TODO: Change to production URL when deploying
    private static final String SOCKET_URL = "http://10.0.2.2:3000";  // Emulator localhost
    // private static final String SOCKET_URL = "https://api.zolachat.site";  // Production
    
    private static SocketManager instance;
    private Socket socket;
    private boolean isConnected = false;
    
    // Listeners
    private OnMessageListener messageListener;
    private OnTypingListener typingListener;
    private OnConnectionListener connectionListener;
    private OnReactionListener reactionListener;
    private OnSeenListener seenListener;
    private OnGroupEventListener groupEventListener;
    private OnFriendEventListener friendEventListener;
    
    private SocketManager() {
        // Private constructor for singleton
    }
    
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }
    
    /**
     * Connect to WebSocket server with Firebase authentication
     */
    public void connect() {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Already connected");
            return;
        }
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot connect - user not logged in");
            return;
        }
        
        // Get Firebase ID token for authentication
        user.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();
            
            try {
                IO.Options options = new IO.Options();
                options.auth = new java.util.HashMap<>();
                options.auth.put("token", token);
                options.reconnection = true;
                options.reconnectionAttempts = 5;
                options.reconnectionDelay = 1000;
                options.timeout = 10000;
                
                socket = IO.socket(SOCKET_URL, options);
                
                // Setup event listeners
                setupEventListeners();
                
                // Connect
                socket.connect();
                Log.d(TAG, "Connecting to " + SOCKET_URL);
                
            } catch (URISyntaxException e) {
                Log.e(TAG, "Invalid socket URL", e);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get Firebase token", e);
        });
    }
    
    /**
     * Setup Socket.IO event listeners
     */
    private void setupEventListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            isConnected = true;
            Log.d(TAG, "✅ WebSocket connected");
            if (connectionListener != null) {
                connectionListener.onConnected();
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            isConnected = false;
            Log.d(TAG, "❌ WebSocket disconnected");
            if (connectionListener != null) {
                connectionListener.onDisconnected();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "Connection error: " + (args.length > 0 ? args[0] : "Unknown"));
            if (connectionListener != null) {
                connectionListener.onError(args.length > 0 ? args[0].toString() : "Connection error");
            }
        });
        
        // New message event
        socket.on("new_message", args -> {
            if (args.length > 0) {
                try {
                    JSONObject messageData = (JSONObject) args[0];
                    Log.d(TAG, "📨 New message received: " + messageData.toString());
                    
                    if (messageListener != null) {
                        messageListener.onMessageReceived(messageData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing new message", e);
                }
            }
        });
        
        // Message updated event (for recall and edit)
        socket.on("message_updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject messageData = (JSONObject) args[0];
                    Log.d(TAG, "✏️ Message updated: " + messageData.toString());
                    
                    if (messageListener != null) {
                        messageListener.onMessageUpdated(messageData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing updated message", e);
                }
            }
        });
        
        // Message deleted event
        socket.on("message_deleted", args -> {
            if (args.length > 0) {
                try {
                    JSONObject messageData = (JSONObject) args[0];
                    Log.d(TAG, "🗑️ Message deleted: " + messageData.toString());
                    
                    if (messageListener != null) {
                        messageListener.onMessageDeleted(messageData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing deleted message", e);
                }
            }
        });
        
        // Reaction updated event
        socket.on("reaction_updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject reactionData = (JSONObject) args[0];
                    Log.d(TAG, "❤️ Reaction updated: " + reactionData.toString());
                    
                    if (reactionListener != null) {
                        String conversationId = reactionData.optString("conversationId");
                        String messageId = reactionData.optString("messageId");
                        String userId = reactionData.optString("userId");
                        String reactionType = reactionData.optString("reactionType");
                        
                        // Parse reactions map from server
                        java.util.Map<String, String> reactions = new java.util.HashMap<>();
                        JSONObject reactionsJson = reactionData.optJSONObject("reactions");
                        if (reactionsJson != null) {
                            java.util.Iterator<String> keys = reactionsJson.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                reactions.put(key, reactionsJson.optString(key));
                            }
                        }
                        
                        // Parse reactionCounts map from server
                        java.util.Map<String, Integer> reactionCounts = new java.util.HashMap<>();
                        JSONObject countsJson = reactionData.optJSONObject("reactionCounts");
                        if (countsJson != null) {
                            java.util.Iterator<String> keys = countsJson.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                reactionCounts.put(key, countsJson.optInt(key, 0));
                            }
                        }
                        
                        reactionListener.onReactionUpdated(conversationId, messageId, userId, reactionType, reactions, reactionCounts);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing reaction event", e);
                }
            }
        });
        
        // Message read/seen event
        socket.on("message_read", args -> {
            if (args.length > 0) {
                try {
                    JSONObject readData = (JSONObject) args[0];
                    Log.d(TAG, "👁️ Message read: " + readData.toString());
                    
                    if (seenListener != null) {
                        String conversationId = readData.optString("conversationId");
                        String userId = readData.optString("userId");
                        long timestamp = readData.optLong("timestamp", System.currentTimeMillis());
                        
                        seenListener.onMessageSeen(conversationId, userId, timestamp);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing message read event", e);
                }
            }
        });
        
        // Typing indicator event
        socket.on("user_typing", args -> {
            if (args.length > 0) {
                try {
                    JSONObject typingData = (JSONObject) args[0];
                    String userId = typingData.getString("userId");
                    boolean isTyping = typingData.getBoolean("isTyping");
                    
                    Log.d(TAG, "⌨️ User typing: " + userId + " = " + isTyping);
                    
                    if (typingListener != null) {
                        typingListener.onUserTyping(userId, isTyping);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing typing event", e);
                }
            }
        });
        
        // Group left event
        socket.on("group_left", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    
                    Log.d(TAG, "🚪 Group left event for conversation " + conversationId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onGroupLeft(conversationId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing group_left event", e);
                }
            }
        });
        
        // Member left event
        socket.on("member_left", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    String userId = data.getString("userId");
                    String userName = data.optString("userName", "Unknown");
                    
                    Log.d(TAG, "👥 Member " + userName + " left conversation " + conversationId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onMemberLeft(conversationId, userId, userName);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing member_left event", e);
                }
            }
        });
        
        // Conversation created event
        socket.on("conversation_created", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    
                    Log.d(TAG, "➕ New conversation created: " + conversationId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onConversationCreated(conversationId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing conversation_created event", e);
                }
            }
        });
        
        // Conversation updated event
        socket.on("conversation_updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    
                    Log.d(TAG, "🔄 Conversation updated: " + conversationId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onConversationUpdated(conversationId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing conversation_updated event", e);
                }
            }
        });
        
        // Conversation deleted event
        socket.on("conversation_deleted", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    
                    Log.d(TAG, "🗑️ Conversation deleted: " + conversationId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onConversationDeleted(conversationId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing conversation_deleted event", e);
                }
            }
        });
        
        // New friend request received event (for receiver)
        socket.on("friend_request_received", args -> {
            Log.e(TAG, "🔔 New friend request received!");
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = data.getString("senderId");
                    String senderName = data.optString("senderName", "Unknown");
                    
                    Log.e(TAG, "📩 Friend request from: " + senderName + " (" + senderId + ")");
                    
                    if (friendEventListener != null) {
                        friendEventListener.onFriendRequestReceived(senderId, senderName);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_request_received event", e);
                }
            }
        });
        
        // Friend request accepted event (for sender)
        socket.on("friend_request_accepted", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    
                    Log.d(TAG, "✅ Friend request accepted by: " + userId);
                    
                    if (friendEventListener != null) {
                        friendEventListener.onFriendRequestAccepted(userId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_request_accepted event", e);
                }
            }
        });
        
        // Friend request rejected event (for sender)
        socket.on("friend_request_rejected", args -> {
            Log.e(TAG, "🔔🔔🔔 RECEIVED friend_request_rejected event! args count: " + args.length);
            if (args.length > 0) {
                try {
                    Log.e(TAG, "Raw args[0]: " + args[0].toString());
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    
                    Log.e(TAG, "❌ Friend request rejected by: " + userId);
                    Log.e(TAG, "friendEventListener is null? " + (friendEventListener == null));
                    
                    if (friendEventListener != null) {
                        friendEventListener.onFriendRequestRejected(userId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_request_rejected event", e);
                }
            }
        });
        
        // Friend added event (for receiver after accepting)
        socket.on("friend_added", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    
                    Log.d(TAG, "👥 New friend added: " + userId);
                    
                    if (friendEventListener != null) {
                        friendEventListener.onFriendAdded(userId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_added event", e);
                }
            }
        });
        
        // Friend removed event
        socket.on("friend_removed", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    
                    Log.d(TAG, "👋 Friend removed: " + userId);
                    
                    if (friendEventListener != null) {
                        friendEventListener.onFriendRemoved(userId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_removed event", e);
                }
            }
        });
        
        // ========== Phase 4B: Group Management Events ==========
        
        // Conversation updated event (group name/avatar changed)
        socket.on("conversation_updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    
                    Log.d(TAG, "🔄 Conversation updated: " + conversationId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onConversationUpdated(conversationId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing conversation_updated event", e);
                }
            }
        });
        
        // Member added event
        socket.on("member_added", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    String userId = data.getString("userId");
                    String addedBy = data.getString("addedBy");
                    
                    Log.d(TAG, "➕ Member added to " + conversationId + ": " + userId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onMemberAdded(conversationId, userId, addedBy);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing member_added event", e);
                }
            }
        });
        
        // Member removed event
        socket.on("member_removed", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    String userId = data.getString("userId");
                    String removedBy = data.getString("removedBy");
                    
                    Log.d(TAG, "➖ Member removed from " + conversationId + ": " + userId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onMemberRemoved(conversationId, userId, removedBy);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing member_removed event", e);
                }
            }
        });
        
        // Member left event
        socket.on("member_left", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    String userId = data.getString("userId");
                    
                    Log.d(TAG, "🚪 Member left " + conversationId + ": " + userId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onMemberLeft(conversationId, userId, "");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing member_left event", e);
                }
            }
        });
        
        // Admin updated event (promote/demote)
        socket.on("admin_updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String conversationId = data.getString("conversationId");
                    String userId = data.getString("userId");
                    String action = data.getString("action"); // "add" or "remove"
                    String updatedBy = data.getString("updatedBy");
                    
                    Log.d(TAG, "👑 Admin " + action + " in " + conversationId + ": " + userId);
                    
                    if (groupEventListener != null) {
                        groupEventListener.onAdminUpdated(conversationId, userId, action, updatedBy);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing admin_updated event", e);
                }
            }
        });
    }
    
    /**
     * Join a conversation room
     */
    public void joinConversation(String conversationId) {
        if (!isConnected || socket == null) {
            Log.w(TAG, "Cannot join room - not connected");
            return;
        }
        
        socket.emit("join_conversation", conversationId);
        Log.d(TAG, "Joined conversation: " + conversationId);
    }
    
    /**
     * Leave a conversation room
     */
    public void leaveConversation(String conversationId) {
        if (!isConnected || socket == null) {
            Log.w(TAG, "Cannot leave room - not connected");
            return;
        }
        
        socket.emit("leave_conversation", conversationId);
        Log.d(TAG, "Left conversation: " + conversationId);
    }
    
    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(String conversationId, boolean isTyping) {
        if (!isConnected || socket == null) {
            Log.w(TAG, "Cannot send typing - not connected");
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("conversationId", conversationId);
            data.put("isTyping", isTyping);
            
            socket.emit("typing", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending typing indicator", e);
        }
    }
    
    /**
     * Disconnect from WebSocket
     */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
            isConnected = false;
            Log.d(TAG, "Disconnected from WebSocket");
        }
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.connected();
    }
    
    // ========== Listener Setters ==========
    
    public void setMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }
    
    public void setTypingListener(OnTypingListener listener) {
        this.typingListener = listener;
    }
    
    public void setConnectionListener(OnConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    public void setReactionListener(OnReactionListener listener) {
        this.reactionListener = listener;
    }
    
    public void setSeenListener(OnSeenListener listener) {
        this.seenListener = listener;
    }
    
    public void setGroupEventListener(OnGroupEventListener listener) {
        this.groupEventListener = listener;
    }
    
    public void setFriendEventListener(OnFriendEventListener listener) {
        this.friendEventListener = listener;
    }
    
    /**
     * Manually trigger conversation created event (for Firestore-created conversations)
     * This is a workaround until full API migration
     */
    public void triggerConversationCreated(String conversationId) {
        if (groupEventListener != null) {
            Log.d(TAG, "Manually triggering conversation_created for: " + conversationId);
            groupEventListener.onConversationCreated(conversationId);
        }
    }
    
    /**
     * Manually trigger conversation updated event
     */
    public void triggerConversationUpdated(String conversationId) {
        if (groupEventListener != null) {
            Log.d(TAG, "Manually triggering conversation_updated for: " + conversationId);
            groupEventListener.onConversationUpdated(conversationId);
        }
    }
    
    // ========== Listener Interfaces ==========
    
    public interface OnMessageListener {
        void onMessageReceived(JSONObject messageData);
        void onMessageUpdated(JSONObject messageData);
        void onMessageDeleted(JSONObject messageData);
    }
    
    public interface OnTypingListener {
        void onUserTyping(String userId, boolean isTyping);
    }
    
    public interface OnConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }
    
    public interface OnReactionListener {
        void onReactionUpdated(String conversationId, String messageId, String userId, String reactionType,
                               java.util.Map<String, String> reactions, java.util.Map<String, Integer> reactionCounts);
    }
    
    public interface OnSeenListener {
        void onMessageSeen(String conversationId, String userId, long timestamp);
    }
    
    public interface OnGroupEventListener {
        void onGroupLeft(String conversationId);
        void onMemberLeft(String conversationId, String userId, String userName);
        void onConversationCreated(String conversationId);
        void onConversationUpdated(String conversationId);
        void onConversationDeleted(String conversationId);
        
        // Phase 4B: New methods for group management
        void onMemberAdded(String conversationId, String userId, String addedBy);
        void onMemberRemoved(String conversationId, String userId, String removedBy);
        void onAdminUpdated(String conversationId, String userId, String action, String updatedBy);
    }
    
    public interface OnFriendEventListener {
        void onFriendRequestReceived(String senderId, String senderName);
        void onFriendRequestAccepted(String userId);
        void onFriendRequestRejected(String userId);
        void onFriendAdded(String userId);
        void onFriendRemoved(String userId);
    }
}
