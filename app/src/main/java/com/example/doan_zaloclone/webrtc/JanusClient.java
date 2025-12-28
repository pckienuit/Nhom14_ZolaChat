package com.example.doan_zaloclone.webrtc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Janus Gateway Client for Android
 * Handles WebSocket connection to Janus SFU server
 */
public class JanusClient {
    private static final String TAG = "JanusClient";
    
    private final String wsUrl;
    private final String apiSecret;
    private WebSocket webSocket;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private long sessionId = 0;
    private long handleId = 0;
    private String currentRoomId = null;
    
    // Callbacks
    private JanusCallback janusCallback;
    
    public interface JanusCallback {
        void onConnected(long sessionId);
        void onDisconnected();
        void onError(String error);
        void onJsep(JSONObject jsep);
        void onRemoteJsep(JSONObject jsep);
        void onIceCandidate(JSONObject candidate);
        void onPublisherJoined(long publisherId, String display);
        void onPublisherLeft(long publisherId);
    }
    
    public JanusClient(String wsUrl, String apiSecret) {
        this.wsUrl = wsUrl;
        this.apiSecret = apiSecret;
    }
    
    public void setCallback(JanusCallback callback) {
        this.janusCallback = callback;
    }
    
    /**
     * Connect to Janus WebSocket server
     */
    public void connect() {
        Log.d(TAG, "Connecting to Janus: " + wsUrl);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                createSession();
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleMessage(bytes.utf8());
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed");
                if (janusCallback != null) {
                    mainHandler.post(() -> janusCallback.onDisconnected());
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error: " + t.getMessage());
                if (janusCallback != null) {
                    mainHandler.post(() -> janusCallback.onError(t.getMessage()));
                }
            }
        });
    }
    
    /**
     * Create Janus session
     */
    private void createSession() {
        try {
            JSONObject message = new JSONObject();
            message.put("janus", "create");
            message.put("transaction", generateTransactionId());
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create session", e);
        }
    }
    
    /**
     * Attach to VideoRoom plugin
     */
    private void attachPlugin() {
        try {
            JSONObject message = new JSONObject();
            message.put("janus", "attach");
            message.put("plugin", "janus.plugin.videoroom");
            message.put("transaction", generateTransactionId());
            message.put("session_id", sessionId);
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to attach plugin", e);
        }
    }
    
    /**
     * Join room as publisher
     */
    public void joinRoom(String roomId, String displayName) {
        this.currentRoomId = roomId;
        
        try {
            JSONObject body = new JSONObject();
            body.put("request", "join");
            body.put("room", roomId);
            body.put("ptype", "publisher");
            body.put("display", displayName);
            
            JSONObject message = new JSONObject();
            message.put("janus", "message");
            message.put("transaction", generateTransactionId());
            message.put("session_id", sessionId);
            message.put("handle_id", handleId);
            message.put("body", body);
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to join room", e);
        }
    }
    
    /**
     * Publish local SDP
     */
    public void publish(JSONObject jsep) {
        try {
            JSONObject body = new JSONObject();
            body.put("request", "configure");
            body.put("audio", true);
            body.put("video", true);
            
            JSONObject message = new JSONObject();
            message.put("janus", "message");
            message.put("transaction", generateTransactionId());
            message.put("session_id", sessionId);
            message.put("handle_id", handleId);
            message.put("body", body);
            message.put("jsep", jsep);
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to publish", e);
        }
    }
    
    /**
     * Subscribe to remote publisher
     */
    public void subscribe(long publisherId, JSONObject offer) {
        try {
            JSONObject body = new JSONObject();
            body.put("request", "start");
            body.put("room", currentRoomId);
            
            JSONObject message = new JSONObject();
            message.put("janus", "message");
            message.put("transaction", generateTransactionId());
            message.put("session_id", sessionId);
            message.put("handle_id", handleId);
            message.put("body", body);
            if (offer != null) {
                message.put("jsep", offer);
            }
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to subscribe", e);
        }
    }
    
    /**
     * Send trickle ICE candidate
     */
    public void trickle(JSONObject candidate) {
        try {
            JSONObject message = new JSONObject();
            message.put("janus", "trickle");
            message.put("transaction", generateTransactionId());
            message.put("session_id", sessionId);
            message.put("handle_id", handleId);
            message.put("candidate", candidate);
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to send candidate", e);
        }
    }
    
    /**
     * Leave room
     */
    public void leaveRoom() {
        try {
            JSONObject body = new JSONObject();
            body.put("request", "leave");
            
            JSONObject message = new JSONObject();
            message.put("janus", "message");
            message.put("transaction", generateTransactionId());
            message.put("session_id", sessionId);
            message.put("handle_id", handleId);
            message.put("body", body);
            message.put("apisecret", apiSecret);
            
            sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to leave room", e);
        }
    }
    
    /**
     * Disconnect from Janus
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        sessionId = 0;
        handleId = 0;
    }
    
    /**
     * Handle incoming Janus messages
     */
    private void handleMessage(String text) {
        try {
            JSONObject message = new JSONObject(text);
            String janus = message.optString("janus");
            
            Log.d(TAG, "Janus message: " + janus);
            
            switch (janus) {
                case "success":
                    handleSuccess(message);
                    break;
                case "event":
                    handleEvent(message);
                    break;
                case "webrtcup":
                    Log.d(TAG, "WebRTC PeerConnection is up");
                    break;
                case "media":
                    Log.d(TAG, "Media event");
                    break;
                case "error":
                    handleError(message);
                    break;
                case "ack":
                    // Message acknowledged
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse message", e);
        }
    }
    
    private void handleSuccess(JSONObject message) throws JSONException {
        if (sessionId == 0) {
            // Session created
            sessionId = message.getJSONObject("data").getLong("id");
            Log.d(TAG, "Session created: " + sessionId);
            attachPlugin();
            
            if (janusCallback != null) {
                mainHandler.post(() -> janusCallback.onConnected(sessionId));
            }
        } else if (handleId == 0) {
            // Plugin attached
            handleId = message.getJSONObject("data").getLong("id");
            Log.d(TAG, "Plugin attached: " + handleId);
        }
    }
    
    private void handleEvent(JSONObject message) throws JSONException {
        JSONObject plugindata = message.optJSONObject("plugindata");
        if (plugindata == null) return;
        
        JSONObject data = plugindata.optJSONObject("data");
        if (data == null) return;
        
        String videoroom = data.optString("videoroom");
        
        switch (videoroom) {
            case "joined":
                Log.d(TAG, "Joined room");
                // Handle publishers already in room
                break;
            case "event":
                handleRoomEvent(data, message.optJSONObject("jsep"));
                break;
        }
        
        JSONObject jsep = message.optJSONObject("jsep");
        if (jsep != null && janusCallback != null) {
            mainHandler.post(() -> janusCallback.onRemoteJsep(jsep));
        }
    }
    
    private void handleRoomEvent(JSONObject data, JSONObject jsep) throws JSONException {
        // Handle publisher events
        String event = data.optString("leaving");
        if (!event.isEmpty()) {
            long publisherId = Long.parseLong(event);
            if (janusCallback != null) {
                mainHandler.post(() -> janusCallback.onPublisherLeft(publisherId));
            }
        }
    }
    
    private void handleError(JSONObject message) throws JSONException {
        JSONObject error = message.getJSONObject("error");
        String errorMsg = error.optString("reason", "Unknown error");
        Log.e(TAG, "Janus error: " + errorMsg);
        
        if (janusCallback != null) {
            mainHandler.post(() -> janusCallback.onError(errorMsg));
        }
    }
    
    private void sendMessage(JSONObject message) {
        if (webSocket != null) {
            webSocket.send(message.toString());
            Log.d(TAG, "Sent: " + message.toString());
        }
    }
    
    private String generateTransactionId() {
        return "tx_" + System.currentTimeMillis();
    }
    
    public long getSessionId() {
        return sessionId;
    }
    
    public long getHandleId() {
        return handleId;
    }
}
