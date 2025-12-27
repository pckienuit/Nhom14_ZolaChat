package com.example.doan_zaloclone.websocket;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * WebSocket Manager for Real-time Messaging
 * Singleton pattern - manages Socket.IO connection to backend server
 */
public class SocketManager {
    private static final String TAG = "SocketManager";

    // Server URL from BuildConfig (set by product flavor)
    private static final String SOCKET_URL = com.example.doan_zaloclone.config.ServerConfig.SOCKET_URL;

    private static SocketManager instance;
    private Socket socket;
    private boolean isConnected = false;
    private boolean isConnecting = false; // Track connection in progress
    
    // Track current conversation room for auto-rejoin on reconnect
    private String currentConversationRoom = null;

    // Listeners
    private OnMessageListener messageListener;
    private OnTypingListener typingListener;
    private OnConnectionListener connectionListener;
    private OnReactionListener reactionListener;
    private OnSeenListener seenListener;
    private OnGroupEventListener groupEventListener;
    private final java.util.List<OnFriendEventListener> friendEventListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private OnNotificationListener notificationListener;

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
        // Prevent duplicate connections
        if (isConnected) {
            Log.d(TAG, "Already connected");
            return;
        }
        
        if (isConnecting) {
            Log.d(TAG, "Connection already in progress, skipping");
            return;
        }
        
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Socket already connected");
            isConnected = true;
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot connect - user not logged in");
            return;
        }
        
        isConnecting = true; // Mark as connecting

        // Get Firebase ID token for authentication
        user.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();

            try {
                // Disconnect existing socket if any
                if (socket != null) {
                    socket.disconnect();
                    socket.off(); // Remove all listeners
                    socket = null;
                }
                
                IO.Options options = new IO.Options();
                options.auth = new java.util.HashMap<>();
                options.auth.put("token", token);
                
                // Force WebSocket transport and secure connection
                options.transports = new String[]{"websocket"};
                options.secure = SOCKET_URL.startsWith("https://");
                
                options.reconnection = true;
                options.reconnectionAttempts = 10;
                options.reconnectionDelay = 2000;
                options.timeout = 20000;

                socket = IO.socket(SOCKET_URL, options);

                // Setup event listeners
                setupEventListeners();

                // Connect
                socket.connect();
                Log.d(TAG, "Connecting to " + SOCKET_URL);

            } catch (URISyntaxException e) {
                Log.e(TAG, "Invalid socket URL", e);
                isConnecting = false;
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get Firebase token", e);
            isConnecting = false;
        });
    }

    /**
     * Setup Socket.IO event listeners
     */
    private void setupEventListeners() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            isConnected = true;
            isConnecting = false; // Connection complete
            Log.d(TAG, "✅ WebSocket connected");
            
            // Join any pending conversation rooms
            onSocketConnected();
            
            if (connectionListener != null) {
                connectionListener.onConnected();
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            isConnected = false;
            isConnecting = false; // Reset on disconnect
            Log.d(TAG, "❌ WebSocket disconnected");
            if (connectionListener != null) {
                connectionListener.onDisconnected();
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            isConnecting = false; // Reset on error
            Log.e(TAG, "Connection error: " + (args.length > 0 ? args[0] : "Unknown"));
            if (connectionListener != null) {
                connectionListener.onError(args.length > 0 ? args[0].toString() : "Connection error");
            }
        });

        // Room joined confirmation
        socket.on("room_joined", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String convId = data.optString("conversationId");
                    boolean success = data.optBoolean("success", false);
                    Log.d(TAG, "✅ Room joined confirmed: " + convId + ", success=" + success);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing room_joined", e);
                }
            }
        });

        // Room left confirmation
        socket.on("room_left", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String convId = data.optString("conversationId");
                    Log.d(TAG, "📤 Room left confirmed: " + convId);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing room_left", e);
                }
            }
        });

        // New message event
        socket.on("new_message", args -> {
            if (args.length > 0) {
                try {
                    JSONObject messageData = (JSONObject) args[0];
                    Log.d(TAG, "📨 New message received: " + messageData.toString());
                    
                    // Notify all listeners
                    for (OnMessageListener listener : messageListeners) {
                        try {
                            listener.onMessageReceived(messageData);
                        } catch (Exception e) {
                            Log.e(TAG, "Error in listener", e);
                        }
                    }
                    
                    // Trigger notification if listener is set
                    if (notificationListener != null) {
                        notificationListener.onNewMessage(messageData);
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

                    for (OnMessageListener listener : messageListeners) {
                         try {
                             listener.onMessageUpdated(messageData);
                         } catch (Exception e) {
                             Log.e(TAG, "Error in listener", e);
                         }
                    }
                    
                    // Trigger notification if message is recalled
                    if (notificationListener != null) {
                        boolean isRecalled = messageData.optBoolean("isRecalled", false);
                        if (isRecalled) {
                            notificationListener.onMessageRecalled(messageData);
                        }
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
            Log.d(TAG, "📩 reaction_updated event received! Args length: " + args.length);
            if (args.length > 0) {
                try {
                    JSONObject reactionData = (JSONObject) args[0];
                    Log.d(TAG, "❤️ Reaction updated: " + reactionData.toString());

                    if (reactionListener != null) {
                        Log.d(TAG, "✅ reactionListener is set, processing...");
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

                        Log.d(TAG, "📤 Calling reactionListener.onReactionUpdated()");
                        reactionListener.onReactionUpdated(conversationId, messageId, userId, reactionType, reactions, reactionCounts);
                    } else {
                        Log.w(TAG, "⚠️ reactionListener is NULL! Event ignored.");
                    }
                    
                    // Trigger notification for reactions (optional)
                    if (notificationListener != null && reactionData.has("reactionType")) {
                        String reactionType = reactionData.optString("reactionType");
                        if (reactionType != null && !reactionType.isEmpty()) {
                            notificationListener.onMessageReaction(reactionData);
                        }
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

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendRequestReceived(senderId, senderName);
                    }
                    
                    // Trigger notification
                    if (notificationListener != null) {
                        notificationListener.onFriendRequestReceived(senderId, senderName);
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

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendRequestAccepted(userId);
                    }
                    
                    // Trigger notification
                    if (notificationListener != null) {
                        notificationListener.onFriendRequestAccepted(userId);
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
                    Log.e(TAG, "friendEventListeners count: " + friendEventListeners.size());

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendRequestRejected(userId);
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

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendAdded(userId);
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

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendRemoved(userId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_removed event", e);
                }
            }
        });
        
        // Friend request cancelled event (sender cancelled their request)
        socket.on("friend_request_cancelled", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = data.getString("senderId");

                    Log.d(TAG, "🔔 Friend request cancelled by sender: " + senderId);

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendRequestCancelled(senderId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_request_cancelled event", e);
                }
            }
        });
        
        // Friend online/offline status change event
        socket.on("friend_status_changed", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String friendId = data.getString("friendId");
                    boolean isOnline = data.getBoolean("isOnline");

                    Log.d(TAG, "🟢 Friend status changed: " + friendId + " isOnline: " + isOnline);

                    for (OnFriendEventListener listener : friendEventListeners) {
                        listener.onFriendStatusChanged(friendId, isOnline);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing friend_status_changed event", e);
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
        Log.d(TAG, "🚪 joinConversation called: " + conversationId + ", socket=" + (socket != null) + ", isConnected=" + isConnected);
        
        if (socket == null) {
            Log.w(TAG, "Cannot join room - socket is null");
            return;
        }

        if (!isConnected) {
            Log.w(TAG, "Socket not connected yet, will join room when connected: " + conversationId);
            // Store pending room to join after connection
            pendingConversationJoin = conversationId;
            return;
        }

        socket.emit("join_conversation", conversationId);
        currentConversationRoom = conversationId; // Track for auto-rejoin on reconnect
        Log.d(TAG, "✅ Emitted join_conversation for room: " + conversationId);
        pendingConversationJoin = null;
    }

    // Pending conversation to join after socket connects
    private String pendingConversationJoin = null;

    /**
     * Called when socket connects - join any pending rooms or rejoin current room
     */
    private void onSocketConnected() {
        if (pendingConversationJoin != null) {
            Log.d(TAG, "🔄 Joining pending conversation after connect: " + pendingConversationJoin);
            socket.emit("join_conversation", pendingConversationJoin);
            currentConversationRoom = pendingConversationJoin;
            pendingConversationJoin = null;
        } else if (currentConversationRoom != null) {
            // Auto-rejoin current room on reconnect
            Log.d(TAG, "🔄 Auto-rejoining conversation after reconnect: " + currentConversationRoom);
            socket.emit("join_conversation", currentConversationRoom);
        }
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
        // Clear tracking if leaving current room
        if (conversationId != null && conversationId.equals(currentConversationRoom)) {
            currentConversationRoom = null;
        }
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

    // List of listeners support
    private final java.util.List<OnMessageListener> messageListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addMessageListener(OnMessageListener listener) {
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    public void removeMessageListener(OnMessageListener listener) {
        messageListeners.remove(listener);
    }

    /**
     * @deprecated Use addMessageListener instead
     */
    @Deprecated
    public void setMessageListener(OnMessageListener listener) {
        // For Backward Compatibility: Clear others and add this one
        // Warning: This behavior might affect other components!
        // Better to migrate to add/remove pattern.
        // But for now, let's treat it as "add" to avoid breaking existing code
        addMessageListener(listener); 
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
    
    public void setNotificationListener(OnNotificationListener listener) {
        this.notificationListener = listener;
    }

    public void addFriendEventListener(OnFriendEventListener listener) {
        if (listener != null && !friendEventListeners.contains(listener)) {
            friendEventListeners.add(listener);
        }
    }
    
    public void removeFriendEventListener(OnFriendEventListener listener) {
        friendEventListeners.remove(listener);
    }
    
    /**
     * @deprecated Use addFriendEventListener instead
     */
    @Deprecated
    public void setFriendEventListener(OnFriendEventListener listener) {
        // For backward compatibility, add the listener
        if (listener != null) {
            addFriendEventListener(listener);
        }
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
        
        void onFriendRequestCancelled(String senderId);

        void onFriendAdded(String userId);

        void onFriendRemoved(String userId);
        
        void onFriendStatusChanged(String friendId, boolean isOnline);
    }
    
    /**
     * Listener for notification events
     * Used to trigger system notifications when events occur
     */
    public interface OnNotificationListener {
        void onNewMessage(org.json.JSONObject messageData);
        
        void onMessageRecalled(org.json.JSONObject messageData);
        
        void onMessageReaction(org.json.JSONObject reactionData);
        
        void onFriendRequestReceived(String senderId, String senderName);
        
        void onFriendRequestAccepted(String userId);
    }
}
