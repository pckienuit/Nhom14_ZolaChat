package com.example.doan_zaloclone;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

/**
 * WebSocket Test Activity
 * Tests Socket.IO connection, events, and real-time communication
 */
public class WebSocketTestActivity extends AppCompatActivity {
    private static final String TAG = "WebSocketTest";
    
    private TextView tvLogs;
    private ScrollView scrollView;
    private Button btnConnect, btnDisconnect, btnJoinRoom, btnLeaveRoom, btnSendTyping, btnClear;
    private EditText etConversationId;
    
    private SocketManager socketManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_websocket_test);
        
        // Initialize views
        tvLogs = findViewById(R.id.tvLogs);
        scrollView = findViewById(R.id.scrollView);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);
        btnSendTyping = findViewById(R.id.btnSendTyping);
        btnClear = findViewById(R.id.btnClear);
        etConversationId = findViewById(R.id.etConversationId);
        
        socketManager = SocketManager.getInstance();
        
        // Setup listeners
        setupSocketListeners();
        setupClickListeners();
        
        // Display info
        displayInitialInfo();
    }
    
    private void displayInitialInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== WebSocket Test Activity ===\n\n");
        info.append("Server: http://10.0.2.2:3000\n");
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            info.append("User: ").append(user.getEmail()).append("\n");
            info.append("UID: ").append(user.getUid()).append("\n");
            info.append("✅ Authenticated\n");
        } else {
            info.append("❌ Not authenticated\n");
        }
        
        info.append("\n--- Ready to test ---\n\n");
        tvLogs.setText(info.toString());
    }
    
    private void setupSocketListeners() {
        // Connection listener
        socketManager.setConnectionListener(new SocketManager.OnConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    appendLog("✅ WebSocket CONNECTED\n");
                    Toast.makeText(WebSocketTestActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    updateButtonStates(true);
                });
            }
            
            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    appendLog("❌ WebSocket DISCONNECTED\n");
                    Toast.makeText(WebSocketTestActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                    updateButtonStates(false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    appendLog("⚠️ ERROR: " + error + "\n");
                    Toast.makeText(WebSocketTestActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
        
        // Message listener - now implements all 3 methods
        socketManager.setMessageListener(new SocketManager.OnMessageListener() {
            @Override
            public void onMessageReceived(JSONObject messageData) {
                runOnUiThread(() -> {
                    try {
                        appendLog("📨 NEW MESSAGE:\n");
                        appendLog("  Content: " + messageData.optString("content", "N/A") + "\n");
                        appendLog("  From: " + messageData.optString("senderId", "N/A") + "\n");
                        appendLog("  Type: " + messageData.optString("type", "N/A") + "\n");
                        appendLog("  Raw: " + messageData.toString() + "\n\n");
                    } catch (Exception e) {
                        appendLog("Error parsing message: " + e.getMessage() + "\n");
                    }
                });
            }
            
            @Override
            public void onMessageUpdated(JSONObject messageData) {
                runOnUiThread(() -> {
                    try {
                        appendLog("✏️ MESSAGE UPDATED:\n");
                        appendLog("  ID: " + messageData.optString("id", "N/A") + "\n");
                        appendLog("  New Content: " + messageData.optString("content", "N/A") + "\n");
                        appendLog("  Is Recalled: " + messageData.optBoolean("isRecalled", false) + "\n");
                        appendLog("  Raw: " + messageData.toString() + "\n\n");
                    } catch (Exception e) {
                        appendLog("Error parsing updated message: " + e.getMessage() + "\n");
                    }
                });
            }
            
            @Override
            public void onMessageDeleted(JSONObject messageData) {
                runOnUiThread(() -> {
                    try {
                        appendLog("🗑️ MESSAGE DELETED:\n");
                        appendLog("  ID: " + messageData.optString("messageId", "N/A") + "\n");
                        appendLog("  Raw: " + messageData.toString() + "\n\n");
                    } catch (Exception e) {
                        appendLog("Error parsing deleted message: " + e.getMessage() + "\n");
                    }
                });
            }
        });
        
        // Typing listener
        socketManager.setTypingListener((userId, isTyping) -> {
            runOnUiThread(() -> {
                appendLog("⌨️ TYPING: " + userId + " is " + (isTyping ? "typing..." : "stopped") + "\n");
            });
        });
    }
    
    private void setupClickListeners() {
        btnConnect.setOnClickListener(v -> {
            appendLog("Connecting to WebSocket...\n");
            socketManager.connect();
        });
        
        btnDisconnect.setOnClickListener(v -> {
            appendLog("Disconnecting...\n");
            socketManager.disconnect();
            updateButtonStates(false);
        });
        
        btnJoinRoom.setOnClickListener(v -> {
            String conversationId = etConversationId.getText().toString().trim();
            if (conversationId.isEmpty()) {
                Toast.makeText(this, "Enter conversation ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!socketManager.isConnected()) {
                Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            socketManager.joinConversation(conversationId);
            appendLog("Joined room: " + conversationId + "\n");
        });
        
        btnLeaveRoom.setOnClickListener(v -> {
            String conversationId = etConversationId.getText().toString().trim();
            if (conversationId.isEmpty()) {
                Toast.makeText(this, "Enter conversation ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!socketManager.isConnected()) {
                Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            socketManager.leaveConversation(conversationId);
            appendLog("Left room: " + conversationId + "\n");
        });
        
        btnSendTyping.setOnClickListener(v -> {
            String conversationId = etConversationId.getText().toString().trim();
            if (conversationId.isEmpty()) {
                Toast.makeText(this, "Enter conversation ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!socketManager.isConnected()) {
                Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            socketManager.sendTypingIndicator(conversationId, true);
            appendLog("Sent typing indicator for: " + conversationId + "\n");
            
            // Auto-stop typing after 2 seconds
            tvLogs.postDelayed(() -> {
                socketManager.sendTypingIndicator(conversationId, false);
                appendLog("Stopped typing indicator\n");
            }, 2000);
        });
        
        btnClear.setOnClickListener(v -> {
            tvLogs.setText("");
            appendLog("Logs cleared\n\n");
        });
    }
    
    private void updateButtonStates(boolean connected) {
        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        btnJoinRoom.setEnabled(connected);
        btnLeaveRoom.setEnabled(connected);
        btnSendTyping.setEnabled(connected);
    }
    
    private void appendLog(String message) {
        tvLogs.append(message);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect here - let it persist
        // socketManager.disconnect();
    }
}
