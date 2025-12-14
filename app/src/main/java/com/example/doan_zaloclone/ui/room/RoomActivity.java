package com.example.doan_zaloclone.ui.room;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.utils.ImageUtils;
import com.example.doan_zaloclone.utils.MediaStoreHelper;
import com.example.doan_zaloclone.viewmodel.RoomViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import androidx.lifecycle.ViewModelProvider;

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
    
    private RoomViewModel roomViewModel;
    private ChatRepository chatRepository; // For backward compatibility with image uploads
    private FirebaseAuth firebaseAuth;
    
    // Message limits for non-friends (only for 1-1 chats)
    private boolean areFriends = false;
    private String otherUserId = "";
    private String conversationType = ""; // "FRIEND" or "GROUP"
    private List<Message> messages = new ArrayList<>();
    
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
    private ActivityResultLauncher<String> permissionLauncher;
    
    // Action menu UI
    private ImageButton moreActionsButton;
    private FrameLayout actionMenuContainer;
    private com.example.doan_zaloclone.ui.room.actions.QuickActionAdapter quickActionAdapter;
    private com.example.doan_zaloclone.ui.room.actions.QuickActionManager quickActionManager;
    
    // File picker
    private ActivityResultLauncher<String> filePickerLauncher;
    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;
    private String selectedFileMimeType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        conversationId = getIntent().getStringExtra("conversationId");
        conversationName = getIntent().getStringExtra("conversationName");
        
        // Initialize ViewModel
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        chatRepository = new ChatRepository(); // For legacy image upload operations
        firebaseAuth = FirebaseAuth.getInstance();
        
        observeViewModel();
        
        // Register permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        loadAndShowImagePicker();
                    } else {
                        Toast.makeText(this, "Cần quyền truy cập ảnh để chọn ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        
        // Register file picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleFileSelected(uri);
                    }
                }
        );

        initViews();
        setupToolbar();
        setupRecyclerView();
        
        // Check network connectivity
        checkNetworkConnectivity();
        
        loadMessages();
        setupListeners();
    }
    
    private void checkNetworkConnectivity() {
        if (!com.example.doan_zaloclone.utils.NetworkUtils.isNetworkAvailable(this)) {
            String networkType = com.example.doan_zaloclone.utils.NetworkUtils.getNetworkType(this);
            android.util.Log.w("RoomActivity", "No network connection. Type: " + networkType);
            Toast.makeText(this, 
                "Không có kết nối mạng. Một số chức năng có thể không hoạt động.", 
                Toast.LENGTH_LONG).show();
        } else {
            android.util.Log.d("RoomActivity", "Network is available");
        }
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
        
        // Action menu views
        moreActionsButton = findViewById(R.id.moreActionsButton);
        actionMenuContainer = findViewById(R.id.actionMenuContainer);
        
        // Initialize QuickActionManager
        quickActionManager = com.example.doan_zaloclone.ui.room.actions.QuickActionManager.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Set initial name
        titleTextView.setText(conversationName != null ? conversationName : "Conversation");
        
        // Add click listener to open group info
        titleTextView.setOnClickListener(v -> {
            if ("GROUP".equals(conversationType)) {
                openGroupInfo();
            }
        });
        
        // If name is "User" (placeholder), fetch real name from conversation memberNames
        if (conversationName == null || conversationName.equals("User")) {
            fetchAndDisplayRealName();
        }
    }
    
    private void fetchAndDisplayRealName() {
        String currentUserId = firebaseAuth.getCurrentUser() != null 
                ? firebaseAuth.getCurrentUser().getUid() 
                : null;
        
        if (currentUserId == null) return;
        
        com.example.doan_zaloclone.services.FirestoreManager.getInstance()
            .getFirestore()
            .collection("conversations")
            .document(conversationId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // Try to get from memberNames first
                    java.util.Map<String, Object> memberNames = 
                        (java.util.Map<String, Object>) doc.get("memberNames");
                    List<String> memberIds = (List<String>) doc.get("memberIds");
                    
                    String realName = null;
                    
                    if (memberNames != null && memberIds != null) {
                        for (String memberId : memberIds) {
                            if (!memberId.equals(currentUserId)) {
                                realName = (String) memberNames.get(memberId);
                                break;
                            }
                        }
                    }
                    
                    // If still "User" or null, fetch from users collection
                    if (realName == null || realName.equals("User")) {
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                if (!memberId.equals(currentUserId)) {
                                    fetchUserName(memberId);
                                    break;
                                }
                            }
                        }
                    } else {
                        titleTextView.setText(realName);
                    }
                }
            });
    }
    
    private void fetchUserName(String userId) {
        com.example.doan_zaloclone.services.FirestoreManager.getInstance()
            .getFirestore()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    com.example.doan_zaloclone.models.User user = 
                        doc.toObject(com.example.doan_zaloclone.models.User.class);
                    if (user != null && user.getName() != null) {
                        titleTextView.setText(user.getName());
                    }
                }
            });
    }

    private void setupRecyclerView() {
        String currentUserId = firebaseAuth.getCurrentUser() != null 
                ? firebaseAuth.getCurrentUser().getUid() 
                : "";
        messageAdapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        // Note: Not using setHasFixedSize(true) because messages have variable heights (text vs images)
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void observeViewModel() {
        // Observe messages via ViewModel
        roomViewModel.getMessages(conversationId).observe(this, resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess()) {
                messages = resource.getData(); // Store for counting
                if (messages != null) {
                    messageAdapter.updateMessages(messages);
                    if (messageAdapter.getItemCount() > 0) {
                        messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }
            } else if (resource.isError()) {
                Toast.makeText(this, "Error loading messages: " + resource.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadMessages() {
        // Messages are automatically loaded via ViewModel observer
        // Check friendship status
        extractOtherUserIdAndCheckFriendship();
    }
    
    private void extractOtherUserIdAndCheckFriendship() {
        // Get current user
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        // Extract other user from conversation name or intent
        // For now, we need to fetch conversation to get memberIds
        com.example.doan_zaloclone.services.FirestoreManager.getInstance()
            .getFirestore()
            .collection("conversations")
            .document(conversationId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // Get conversation type
                    conversationType = doc.getString("type");
                    if (conversationType == null) {
                        conversationType = "FRIEND"; // Default for old conversations
                    }
                    
                    android.util.Log.d("RoomActivity", "Conversation type: " + conversationType);
                    
                    // Update MessageAdapter for group chat
                    boolean isGroup = "GROUP".equals(conversationType);
                    if (messageAdapter != null) {
                        messageAdapter.setGroupChat(isGroup);
                    }
                    
                    // Update toolbar for group chat
                    if (isGroup) {
                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds != null) {
                            int memberCount = memberIds.size();
                            String groupName = doc.getString("name");
                            if (groupName != null) {
                                titleTextView.setText(groupName + " (" + memberCount + ")");
                            }
                        }
                    }
                    
                    // Only check friendship for 1-1 chats
                    if ("FRIEND".equals(conversationType)) {
                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                if (!memberId.equals(currentUserId)) {
                                    otherUserId = memberId;
                                    checkFriendship();
                                    break;
                                }
                            }
                        }
                    } else {
                        // For group chats, no message limit
                        areFriends = true; // Treat as friends to bypass limit
                        android.util.Log.d("RoomActivity", "Group chat - no message limit");
                    }
                }
            });
    }
    
    private void checkFriendship() {
        if (otherUserId.isEmpty()) return;
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        roomViewModel.checkFriendship(currentUserId, otherUserId).observe(this, resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                areFriends = resource.getData();
                android.util.Log.d("RoomActivity", "Friendship status with " + otherUserId + ": " + areFriends);
            } else if (resource != null && resource.isError()) {
                android.util.Log.e("RoomActivity", "Error checking friendship: " + resource.getMessage());
                areFriends = false;
            }
        });
    }
    
    private int countMyMessages() {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        int count = 0;
        
        for (Message msg : messages) {
            if (currentUserId.equals(msg.getSenderId())) {
                count++;
            }
        }
        
        return count;
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> handleSendMessage());
        attachImageButton.setOnClickListener(v -> requestPermissionAndShowPicker());
        moreActionsButton.setOnClickListener(v -> showActionMenu());
        backButton.setOnClickListener(v -> hideImagePicker());
        sendImagesButton.setOnClickListener(v -> handleSendImages());
    }
    
    private void requestPermissionAndShowPicker() {
        // Check if permission is already granted
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAndShowImagePicker();
        } else {
            // Request permission
            permissionLauncher.launch(permission);
        }
    }
    
    private void loadAndShowImagePicker() {
        // Show loading indicator
        Toast.makeText(this, "Đang tải ảnh...", Toast.LENGTH_SHORT).show();
        
        // Load photos in background thread to prevent ANR
        new Thread(() -> {
            // Load photos from device (limited to 100 most recent)
            List<Uri> photos = MediaStoreHelper.loadPhotos(this, 100);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                setupImagePickerUI(photos);
            });
        }).start();
    }
    
    private void setupImagePickerUI(List<Uri> photos) {
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
        requestPermissionAndShowPicker();
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
        
        // CHECK MESSAGE LIMIT FOR NON-FRIENDS (ONLY FOR 1-1 CHATS)
        if ("FRIEND".equals(conversationType) && !areFriends) {
            int myMessageCount = countMyMessages();
            
            if (myMessageCount >= 3) {
                Toast.makeText(this, 
                    "Bạn cần kết bạn để tiếp tục nhắn tin", 
                    Toast.LENGTH_LONG).show();
                return; // Block sending
            }
            
            int remaining = 3 - myMessageCount;
            Toast.makeText(this, 
                "Còn " + remaining + " tin nhắn. Hãy kết bạn để tiếp tục!", 
                Toast.LENGTH_SHORT).show();
        }
        // No limit for group chats
        
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
        
        // Send message via ViewModel
        roomViewModel.sendMessage(conversationId, newMessage);
        
        // Clear input immediately for better UX
        messageEditText.setText("");
        
        // Re-enable button after a short delay
        sendButton.postDelayed(() -> sendButton.setEnabled(true), 500);
    }
    
    // ============ ACTION MENU METHODS ============
    
    private void showActionMenu() {
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = 
            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        
        // Setup adapter with actions
        quickActionAdapter = new com.example.doan_zaloclone.ui.room.actions.QuickActionAdapter(
            quickActionManager.getActions(),
            this::handleQuickAction
        );
        
        // Inflate bottom sheet
        View menuView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_action_menu, actionMenuContainer, false);
        RecyclerView actionsRecyclerView = menuView.findViewById(R.id.actionsRecyclerView);
        actionsRecyclerView.setAdapter(quickActionAdapter);
        
        // Show menu
        actionMenuContainer.removeAllViews();
        actionMenuContainer.addView(menuView);
        actionMenuContainer.setVisibility(View.VISIBLE);
    }
    
    private void hideActionMenu() {
        actionMenuContainer.setVisibility(View.GONE);
        actionMenuContainer.removeAllViews();
    }
    
    private void handleQuickAction(com.example.doan_zaloclone.ui.room.actions.QuickAction action) {
        hideActionMenu();
        
        action.execute(this, new com.example.doan_zaloclone.ui.room.actions.QuickActionCallback() {
            @Override
            public void onShowUI() {
                // Launch file picker
                launchFilePicker();
            }
            
            @Override
            public void onCancel() {
                Toast.makeText(RoomActivity.this, "Đã hủy", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onFilesSelected(List<Uri> fileUris) {
                // Handle multiple files (future enhancement)
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(RoomActivity.this, error, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onSuccess() {
                Toast.makeText(RoomActivity.this, "Thành công!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // ============ FILE HANDLING METHODS ============
    
    private void launchFilePicker() {
        com.example.doan_zaloclone.utils.FilePickerHelper.launchFilePicker(filePickerLauncher);
    }
    
    private void handleFileSelected(Uri fileUri) {
        // Extract file metadata
        selectedFileUri = fileUri;
        selectedFileName = com.example.doan_zaloclone.utils.FileUtils.getFileName(this, fileUri);
        selectedFileSize = com.example.doan_zaloclone.utils.FileUtils.getFileSize(this, fileUri);
        selectedFileMimeType = com.example.doan_zaloclone.utils.FileUtils.getMimeType(this, fileUri);
        
        // Show confirmation and upload
        String fileSizeFormatted = com.example.doan_zaloclone.utils.FileUtils.formatFileSize(selectedFileSize);
        String message = "Gửi file: " + selectedFileName + " (" + fileSizeFormatted + ")?";
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận gửi file")
                .setMessage(message)
                .setPositiveButton("Gửi", (dialog, which) -> uploadAndSendFile())
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void uploadAndSendFile() {
        if (selectedFileUri == null || firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Lỗi: Không thể gửi file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Toast.makeText(this, "Đang gửi file...", Toast.LENGTH_SHORT).show();
        
        chatRepository.uploadFileAndSendMessage(
                conversationId,
                selectedFileUri,
                currentUserId,
                selectedFileName,
                selectedFileSize,
                selectedFileMimeType,
                new ChatRepository.SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Đã gửi file!", Toast.LENGTH_SHORT).show();
                            // Clear selection
                            selectedFileUri = null;
                            selectedFileName = null;
                            selectedFileSize = 0;
                            selectedFileMimeType = null;
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Lỗi gửi file: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }
    
    private void openGroupInfo() {
        android.content.Intent intent = new android.content.Intent(this, 
                com.example.doan_zaloclone.ui.group.GroupInfoActivity.class);
        intent.putExtra("conversationId", conversationId);
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ViewModel will automatically clean up listeners
    }
}
