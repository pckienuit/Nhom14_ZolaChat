package com.example.doan_zaloclone.ui.room;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.example.doan_zaloclone.utils.MediaStoreHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

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
    
    // Image picker UI
    private ImageButton attachImageButton;
    private FrameLayout imagePickerContainer;
    private LinearLayout normalInputLayout;
    private LinearLayout imageInputLayout;
    private ImageButton backButton;
    private Spinner qualitySpinner;
    private TextView selectedCountTextView;
    private ImageButton sendImagesButton;
    private ImagePickerAdapter imagePickerAdapter;
    private List<Uri> selectedPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        conversationId = getIntent().getStringExtra("conversationId");
        conversationName = getIntent().getStringExtra("conversationName");
        
        chatRepository = new ChatRepository();
        firebaseAuth = FirebaseAuth.getInstance();

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
        
        // Image picker views
        imagePickerContainer = findViewById(R.id.imagePickerContainer);
        normalInputLayout = findViewById(R.id.normalInputLayout);
        imageInputLayout = findViewById(R.id.imageInputLayout);
        backButton = findViewById(R.id.backButton);
        qualitySpinner = findViewById(R.id.qualitySpinner);
        selectedCountTextView = findViewById(R.id.selectedCountTextView);
        sendImagesButton = findViewById(R.id.sendImagesButton);
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
        attachImageButton.setOnClickListener(v -> showImagePicker());
        backButton.setOnClickListener(v -> hideImagePicker());
        sendImagesButton.setOnClickListener(v -> handleSendImages());
    }
    
    private void showImagePicker() {
        // Load photos from device
        List<Uri> photos = MediaStoreHelper.loadPhotos(this);
        
        // Setup adapter
        imagePickerAdapter = new ImagePickerAdapter(photos, new ImagePickerAdapter.OnItemClickListener() {
            @Override
            public void onCameraClick() {
                Toast.makeText(RoomActivity.this, "Camera feature coming soon", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPhotoClick(Uri photoUri, int position) {
                updateImageModeUI();
            }
        });
        
        // Inflate bottom sheet layout
        View bottomSheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_image_picker, imagePickerContainer, false);
        RecyclerView photosRecyclerView = bottomSheetView.findViewById(R.id.photosRecyclerView);
        photosRecyclerView.setAdapter(imagePickerAdapter);
        
        // Add to container and show
        imagePickerContainer.removeAllViews();
        imagePickerContainer.addView(bottomSheetView);
        imagePickerContainer.setVisibility(View.VISIBLE);
        
        // Switch to image mode
        switchToImageMode();
    }
    
    private void hideImagePicker() {
        imagePickerContainer.setVisibility(View.GONE);
        imagePickerContainer.removeAllViews();
        if (imagePickerAdapter != null) {
            imagePickerAdapter.clearSelection();
        }
        switchToNormalMode();
    }
    
    private void switchToImageMode() {
        normalInputLayout.setVisibility(View.GONE);
        imageInputLayout.setVisibility(View.VISIBLE);
        updateImageModeUI();
    }
    
    private void switchToNormalMode() {
        imageInputLayout.setVisibility(View.GONE);
        normalInputLayout.setVisibility(View.VISIBLE);
    }
    
    private void updateImageModeUI() {
        if (imagePickerAdapter != null) {
            selectedPhotos = imagePickerAdapter.getSelectedPhotos();
            int count = selectedPhotos.size();
            selectedCountTextView.setText(count + " ảnh đã chọn");
        }
    }
    
    private void handleSendImages() {
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get quality setting
        int qualityIndex = qualitySpinner.getSelectedItemPosition();
        
        Toast.makeText(this, "Đang gửi " + selectedPhotos.size() + " ảnh...", Toast.LENGTH_SHORT).show();
        
        // Upload each image
        for (Uri photoUri : selectedPhotos) {
            uploadSingleImage(photoUri, qualityIndex);
        }
        
        // Hide picker
        hideImagePicker();
    }
    
    private void uploadSingleImage(Uri imageUri, int qualityIndex) {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        try {
            Uri finalImageUri = imageUri;
            
            // Compress based on quality setting
            if (qualityIndex == 0) {
                // HD - no compression
            } else if (qualityIndex == 1) {
                // Standard - 1080p, 85%
                finalImageUri = ImageUtils.compressImage(this, imageUri, 85);
            } else if (qualityIndex == 2) {
                // Low - 720p, 75%
                finalImageUri = compressImageLowQuality(imageUri);
            }
            
            Uri uploadUri = finalImageUri;
            chatRepository.uploadImageAndSendMessage(conversationId, uploadUri, currentUserId,
                    new ChatRepository.SendMessageCallback() {
                        @Override
                        public void onSuccess() {
                            // Silent success for batch upload
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(RoomActivity.this, "Lỗi gửi ảnh: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private Uri compressImageLowQuality(Uri imageUri) throws Exception {
        // For low quality, we could further resize to 720px
        // For now, use same method with lower quality
        return ImageUtils.compressImage(this, imageUri, 75);
    }
    
    private void openImagePicker() {
        // Old method - no longer used
        showImagePicker();
    }
    
    private void handleImageSelected(Uri imageUri) {
        // Old method - no longer used
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
