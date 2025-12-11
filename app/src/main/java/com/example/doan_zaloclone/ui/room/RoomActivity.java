package com.example.doan_zaloclone.ui.room;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class RoomActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView titleTextView;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    
    private MessageAdapter messageAdapter;
    private String conversationId;
    private String conversationName;
    
    private ChatRepository chatRepository;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messagesListener;
    
    private ImageButton attachImageButton;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        conversationId = getIntent().getStringExtra("conversationId");
        conversationName = getIntent().getStringExtra("conversationName");
        
        chatRepository = new ChatRepository();
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Register image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleImageSelected
        );

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadMessages();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        titleTextView = findViewById(R.id.titleTextView);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        attachImageButton = findViewById(R.id.attachImageButton);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        titleTextView.setText(conversationName);
    }

    private void setupRecyclerView() {
        String currentUserId = firebaseAuth.getCurrentUser() != null 
                ? firebaseAuth.getCurrentUser().getUid() 
                : "";
        messageAdapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        // Listen to real-time message updates from Firestore
        messagesListener = chatRepository.listenToMessages(conversationId, new ChatRepository.MessagesListener() {
            @Override
            public void onMessagesChanged(java.util.List<Message> messages) {
                messageAdapter.updateMessages(messages);
                if (messageAdapter.getItemCount() > 0) {
                    messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RoomActivity.this, "Error loading messages: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> handleSendMessage());
        attachImageButton.setOnClickListener(v -> openImagePicker());
    }
    
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }
    
    private void handleImageSelected(Uri imageUri) {
        if (imageUri == null || firebaseAuth.getCurrentUser() == null) {
            return;
        }
        
        // Show preview dialog instead of uploading immediately
        showImagePreviewDialog(imageUri);
    }
    
    private void showImagePreviewDialog(Uri imageUri) {
        // Inflate dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_preview, null);
        
        ImageView previewImageView = dialogView.findViewById(R.id.previewImageView);
        SwitchCompat compressionSwitch = dialogView.findViewById(R.id.compressionSwitch);
        
        // Load image preview
        previewImageView.setImageURI(imageUri);
        
        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Handle cancel button
        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        
        // Handle send button
        dialogView.findViewById(R.id.sendButton).setOnClickListener(v -> {
            dialog.dismiss();
            boolean shouldCompress = compressionSwitch.isChecked();
            uploadImage(imageUri, shouldCompress);
        });
        
        dialog.show();
    }
    
    private void uploadImage(Uri imageUri, boolean shouldCompress) {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        
        try {
            // Compress if enabled
            Uri finalImageUri = imageUri;
            if (shouldCompress) {
                finalImageUri = ImageUtils.compressImage(this, imageUri);
            }
            
            Uri uploadUri = finalImageUri;
            chatRepository.uploadImageAndSendMessage(conversationId, uploadUri, currentUserId,
                    new ChatRepository.SendMessageCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(RoomActivity.this, "Image sent!", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(RoomActivity.this, "Failed to send image: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }
        
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable send button while sending
        sendButton.setEnabled(false);
        
        // Create new message
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Message newMessage = new Message(
                null, // ID will be auto-generated by ChatRepository
                currentUserId,
                messageText,
                Message.TYPE_TEXT,
                System.currentTimeMillis()
        );
        
        // Send message to Firestore
        chatRepository.sendMessage(conversationId, newMessage, new ChatRepository.SendMessageCallback() {
            @Override
            public void onSuccess() {
                // Clear input and re-enable button
                runOnUiThread(() -> {
                    messageEditText.setText("");
                    sendButton.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RoomActivity.this, "Failed to send: " + error, Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firestore listener
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}
