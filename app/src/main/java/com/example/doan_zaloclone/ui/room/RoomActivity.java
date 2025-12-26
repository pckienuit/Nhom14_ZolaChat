package com.example.doan_zaloclone.ui.room;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.ui.call.CallActivity;
import com.example.doan_zaloclone.utils.ImageUtils;
import com.example.doan_zaloclone.utils.MediaStoreHelper;
import com.example.doan_zaloclone.utils.PermissionHelper;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.viewmodel.RoomViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends AppCompatActivity {

    // Auto-call extras (for triggering call from business card)
    public static final String EXTRA_AUTO_START_CALL = "auto_start_call";
    public static final String EXTRA_IS_VIDEO_CALL = "is_video_call";
    private final List<FilePreviewAdapter.FileItem> selectedFilesForPreview = new ArrayList<>();
    private Toolbar toolbar;
    private TextView titleTextView;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private MessageAdapter messageAdapter;
    private String conversationId;
    private String conversationName;
    private RoomViewModel roomViewModel;
    private ContactViewModel contactViewModel;
    private ChatRepository chatRepository; // For backward compatibility with image uploads
    private com.example.doan_zaloclone.repository.ConversationRepository conversationRepository;
    private FirebaseAuth firebaseAuth;
    // Message limits for non-friends (only for 1-1 chats)
    private boolean areFriends = false;
    private String otherUserId = "";
    private String conversationType = ""; // "FRIEND" or "GROUP"
    private List<Message> messages = new ArrayList<>();
    // Image picker UI
    private ImageButton attachImageButton;
    private ImageButton voiceRecordButton;
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
    // Sticker button
    private ImageButton stickerButton;
    // Action menu UI
    private ImageButton moreActionsButton;
    private FrameLayout actionMenuContainer;
    private com.example.doan_zaloclone.ui.room.actions.QuickActionAdapter quickActionAdapter;
    private com.example.doan_zaloclone.ui.room.actions.QuickActionManager quickActionManager;
    // File picker
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String> multipleFilePickerLauncher;
    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;
    private String selectedFileMimeType;
    // File preview (for multiple files)
    private FrameLayout filePickerBottomSheet;
    private FilePreviewAdapter filePreviewAdapter;
    // Poll creation
    private ActivityResultLauncher<Intent> createPollLauncher;

    // Location picker
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    // Pinned messages UI
    private FrameLayout pinnedMessagesContainer;
    private View pinnedBarCollapsed;
    private TextView pinnedMessagePreview;
    private ImageButton expandCollapseButton;
    private FrameLayout pinnedListContainer;
    private RecyclerView pinnedMessagesRecyclerView;
    private TextView pinnedMessagesCount;
    private PinnedMessagesAdapter pinnedMessagesAdapter;
    private boolean isPinnedListExpanded = false;

    // Reply bar UI
    private View replyBarLayout;
    private TextView replyingToName;
    private TextView replyingToContent;
    private ImageButton cancelReplyButton;
    private Message replyingToMessage = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_room);

        // Set status bar to transparent to show gradient behind
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        conversationId = getIntent().getStringExtra("conversationId");
        conversationName = getIntent().getStringExtra("conversationName");

        // Initialize ViewModel
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        chatRepository = new ChatRepository(); // For legacy image upload operations
        conversationRepository = com.example.doan_zaloclone.repository.ConversationRepository.getInstance();
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

        // Register multiple file picker launcher
        multipleFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        handleMultipleFilesSelected(uris);
                    }
                }
        );

        // Register poll creation launcher
        createPollLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        com.example.doan_zaloclone.models.Poll poll =
                                (com.example.doan_zaloclone.models.Poll) result.getData()
                                        .getSerializableExtra(CreatePollActivity.EXTRA_POLL_DATA);
                        if (poll != null) {
                            sendPollMessage(poll);
                        }
                    }
                }
        );

        // Register location picker launcher
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        boolean isLiveLocation = result.getData().getBooleanExtra(
                                com.example.doan_zaloclone.ui.location.LocationPickerActivity.EXTRA_IS_LIVE_LOCATION, false);

                        if (isLiveLocation) {
                            // Handle Live Location
                            long duration = result.getData().getLongExtra(
                                    com.example.doan_zaloclone.ui.location.LocationPickerActivity.EXTRA_LIVE_DURATION, 15 * 60 * 1000);
                            String sessionId = java.util.UUID.randomUUID().toString();

                            // Start Service
                            Intent serviceIntent = new Intent(this, com.example.doan_zaloclone.services.LocationSharingService.class);
                            serviceIntent.setAction(com.example.doan_zaloclone.services.LocationSharingService.ACTION_START_SHARING);
                            serviceIntent.putExtra(com.example.doan_zaloclone.services.LocationSharingService.EXTRA_SESSION_ID, sessionId);
                            serviceIntent.putExtra(com.example.doan_zaloclone.services.LocationSharingService.EXTRA_DURATION, duration);

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent);
                            } else {
                                startService(serviceIntent);
                            }

                            // Send Live Location Message
                            sendLiveLocationMessage(sessionId);

                        } else {
                            // Handle Static Location
                            double latitude = result.getData().getDoubleExtra(
                                    com.example.doan_zaloclone.ui.location.LocationPickerActivity.EXTRA_LATITUDE, 0);
                            double longitude = result.getData().getDoubleExtra(
                                    com.example.doan_zaloclone.ui.location.LocationPickerActivity.EXTRA_LONGITUDE, 0);
                            String locationName = result.getData().getStringExtra(
                                    com.example.doan_zaloclone.ui.location.LocationPickerActivity.EXTRA_LOCATION_NAME);
                            String locationAddress = result.getData().getStringExtra(
                                    com.example.doan_zaloclone.ui.location.LocationPickerActivity.EXTRA_LOCATION_ADDRESS);

                            sendLocationMessage(latitude, longitude, locationName, locationAddress);
                        }
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

        // Check if we should auto-start a call (from business card)
        checkAutoStartCall();
    }

    /**
     * Check if activity was opened with auto-start call intent
     */
    private void checkAutoStartCall() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(EXTRA_AUTO_START_CALL, false)) {
            boolean isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, false);

            // Clear the flag to prevent re-triggering
            intent.removeExtra(EXTRA_AUTO_START_CALL);

            // Delay slightly to allow UI to initialize
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                android.util.Log.d("RoomActivity", "Auto-starting " + (isVideo ? "video" : "voice") + " call");
                startCall(isVideo);
            }, 500);
        }
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

    private LinearLayout inputContainer;

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        titleTextView = findViewById(R.id.titleTextView);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        attachImageButton = findViewById(R.id.attachImageButton);
        stickerButton = findViewById(R.id.stickerButton);

        // Input container
        inputContainer = findViewById(R.id.inputContainer);

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
        voiceRecordButton = findViewById(R.id.voiceRecordButton);
        actionMenuContainer = findViewById(R.id.actionMenuContainer);

        // Call buttons
        ImageButton videoCallButton = findViewById(R.id.videoCallButton);
        ImageButton voiceCallButton = findViewById(R.id.voiceCallButton);
        ImageButton fileManagementButton = findViewById(R.id.fileManagementButton);

        // Setup call button listeners
        if (videoCallButton != null) videoCallButton.setOnClickListener(v -> startCall(true));
        if (voiceCallButton != null) voiceCallButton.setOnClickListener(v -> startCall(false));
         // Compatibility check for hidden buttons
        if (fileManagementButton != null) fileManagementButton.setOnClickListener(v -> openFileManagement());

        // Initialize QuickActionManager
        quickActionManager = com.example.doan_zaloclone.ui.room.actions.QuickActionManager.getInstance();

        // Setup voice record button (Phase 4A)
        setupVoiceRecordButton();

        initPinnedMessagesViews();
        initReplyBarViews();
        
        setupInsets();
    }
    
    // Phase 4A & 4B: Voice Record - Permission & Recording Logic
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1001;
    private boolean isRecording = false;
    private android.media.MediaRecorder mediaRecorder;
    private String audioFilePath;
    private long recordingStartTime = 0;
    private Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable recordingTimerRunnable;
    
    private void setupVoiceRecordButton() {
        if (voiceRecordButton == null) return;
        
        // Long-press to start recording, release to stop
        voiceRecordButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // Start recording on press
                    if (!isRecording) {
                        if (checkRecordAudioPermission()) {
                            startVoiceRecording();
                        } else {
                            requestRecordAudioPermission();
                        }
                    }
                    return true;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // Stop recording on release
                    if (isRecording) {
                        stopVoiceRecording();
                    }
                    return true;
            }
            return false;
        });
    }
    
    private boolean checkRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestRecordAudioPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        }
    }
    
    private void startVoiceRecording() {
        try {
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            updateVoiceRecordButtonUI();
            
            // Create audio file in cache directory
            String fileName = "voice_" + System.currentTimeMillis() + ".m4a";
            audioFilePath = new java.io.File(getCacheDir(), fileName).getAbsolutePath();
            
            // Setup MediaRecorder
            mediaRecorder = new android.media.MediaRecorder();
            mediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(audioFilePath);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            // Start recording timer
            startRecordingTimer();
            
            Toast.makeText(this, "🎤 Recording...", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            android.util.Log.e("RoomActivity", "Error starting recording", e);
            Toast.makeText(this, "Lỗi khi ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            cleanupRecording();
        }
    }
    
    private void stopVoiceRecording() {
        if (!isRecording) return;
        
        try {
            // Stop timer
            stopRecordingTimer();
            
            // Calculate duration
            long duration = System.currentTimeMillis() - recordingStartTime;
            
            // Stop and release MediaRecorder
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                } catch (RuntimeException e) {
                    android.util.Log.e("RoomActivity", "Error stopping MediaRecorder", e);
                }
                mediaRecorder = null;
            }
            
            isRecording = false;
            updateVoiceRecordButtonUI();
            
            // Check if recording is too short (less than 1 second)
            if (duration < 1000) {
                Toast.makeText(this, "Ghi âm quá ngắn", Toast.LENGTH_SHORT).show();
                deleteAudioFile();
                return;
            }
            
            // Phase 4C: Upload and send voice message
            int durationInSeconds = (int) (duration / 1000);
            uploadVoiceMessage(audioFilePath, durationInSeconds);
            
        } catch (Exception e) {
            android.util.Log.e("RoomActivity", "Error stopping recording", e);
            Toast.makeText(this, "Lỗi khi dừng ghi âm", Toast.LENGTH_SHORT).show();
        } finally {
            cleanupRecording();
        }
    }
    
    // Phase 4C: Upload voice message to Cloudinary and send
    private void uploadVoiceMessage(String filePath, int durationSeconds) {
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy file ghi âm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Đang gửi tin nhắn thoại...", Toast.LENGTH_SHORT).show();
        
        java.io.File audioFile = new java.io.File(filePath);
        if (!audioFile.exists()) {
            Toast.makeText(this, "File ghi âm không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Upload to Cloudinary in background
        new Thread(() -> {
            try {
                // Upload audio file
                com.cloudinary.Cloudinary cloudinary = new com.cloudinary.Cloudinary(
                    com.cloudinary.utils.ObjectUtils.asMap(
                        "cloud_name", "do0mdssvu",
                        "api_key", "248235972111755",
                        "api_secret", "9hnWcqIA7en-XduIpo1f3C1CdIg"
                    )
                );
                
                java.util.Map uploadResult = cloudinary.uploader().upload(
                    audioFile,
                    com.cloudinary.utils.ObjectUtils.asMap(
                        "resource_type", "raw",
                        "folder", "zalo_voice_messages"
                    )
                );
                
                String voiceUrl = (String) uploadResult.get("secure_url");
                
                // Send voice message on main thread
                runOnUiThread(() -> {
                    sendVoiceMessage(voiceUrl, durationSeconds);
                    // Delete temp file after upload
                    audioFile.delete();
                    Toast.makeText(this, "✅ Đã gửi tin nhắn thoại", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                android.util.Log.e("RoomActivity", "Error uploading voice", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void sendVoiceMessage(String voiceUrl, int durationSeconds) {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";
        
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy user ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create voice message
        Message voiceMessage = new Message();
        voiceMessage.setId(java.util.UUID.randomUUID().toString());
        voiceMessage.setSenderId(currentUserId);
        voiceMessage.setType(Message.TYPE_VOICE);
        voiceMessage.setContent("🎤 Tin nhắn thoại (" + durationSeconds + "s)");
        voiceMessage.setVoiceUrl(voiceUrl);
        voiceMessage.setVoiceDuration(durationSeconds);
        voiceMessage.setTimestamp(System.currentTimeMillis());
        
        // Set sender name for group chats
        String displayName = firebaseAuth.getCurrentUser().getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            voiceMessage.setSenderName(displayName);
        }
        
        // Handle reply if replying to a message
        if (replyingToMessage != null) {
            voiceMessage.setReplyToId(replyingToMessage.getId());
            voiceMessage.setReplyToContent(replyingToMessage.getContent());
            voiceMessage.setReplyToSenderId(replyingToMessage.getSenderId());
            hideReplyBar();
        }
        
        // Send via ViewModel
        roomViewModel.sendMessage(conversationId, voiceMessage);
    }
    
    private void startRecordingTimer() {
        recordingTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    int seconds = (int) (elapsed / 1000);
                    
                    // Update UI - could show in a TextView if available
                    // For Phase 4B, we just log it
                    android.util.Log.d("RoomActivity", "Recording: " + seconds + "s");
                    
                    // Schedule next update
                    recordingTimerHandler.postDelayed(this, 1000);
                }
            }
        };
        recordingTimerHandler.post(recordingTimerRunnable);
    }
    
    private void stopRecordingTimer() {
        if (recordingTimerRunnable != null) {
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
            recordingTimerRunnable = null;
        }
    }
    
    private void cleanupRecording() {
        isRecording = false;
        updateVoiceRecordButtonUI();
        stopRecordingTimer();
        
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                // Ignore
            }
            mediaRecorder = null;
        }
    }
    
    private void deleteAudioFile() {
        if (audioFilePath != null) {
            java.io.File file = new java.io.File(audioFilePath);
            if (file.exists()) {
                file.delete();
            }
            audioFilePath = null;
        }
    }
    
    private void updateVoiceRecordButtonUI() {
        if (voiceRecordButton == null) return;
        
        if (isRecording) {
            // Show pause/stop icon and red tint
            voiceRecordButton.setImageResource(android.R.drawable.ic_media_pause);
            voiceRecordButton.setColorFilter(getResources().getColor(android.R.color.holo_red_dark, null));
        } else {
            // Show mic icon with normal tint
            voiceRecordButton.setImageResource(R.drawable.ic_mic);
            voiceRecordButton.setColorFilter(getResources().getColor(R.color.colorPrimary, null));
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền ghi âm đã được cấp. Nhấn giữ mic để ghi âm.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần quyền ghi âm để gửi tin nhắn thoại", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void setupInsets() {
        if (inputContainer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(inputContainer, (v, windowInsets) -> {
                int typeMask = androidx.core.view.WindowInsetsCompat.Type.systemBars() | androidx.core.view.WindowInsetsCompat.Type.ime();
                androidx.core.graphics.Insets insets = windowInsets.getInsets(typeMask);
                v.setPadding(0, 0, 0, insets.bottom);
                return androidx.core.view.WindowInsetsCompat.CONSUMED;
            });
        }
    }

    private void initReplyBarViews() {
        replyBarLayout = findViewById(R.id.replyBarLayout);
        if (replyBarLayout != null) {
            replyingToName = replyBarLayout.findViewById(R.id.replyingToName);
            replyingToContent = replyBarLayout.findViewById(R.id.replyingToContent);
            cancelReplyButton = replyBarLayout.findViewById(R.id.cancelReplyButton);

            if (cancelReplyButton != null) {
                cancelReplyButton.setOnClickListener(v -> hideReplyBar());
            }
        }
    }

    private void initPinnedMessagesViews() {
        pinnedMessagesContainer = findViewById(R.id.pinnedMessagesContainer);

        if (pinnedMessagesContainer != null) {
            // Find views within included layout
            View includedView = pinnedMessagesContainer.getChildAt(0); // layout_pinned_message_bar
            if (includedView != null) {
                pinnedBarCollapsed = includedView.findViewById(R.id.pinnedBarCollapsed);
                pinnedMessagePreview = includedView.findViewById(R.id.pinnedMessagePreview);
                expandCollapseButton = includedView.findViewById(R.id.expandCollapseButton);
                pinnedListContainer = includedView.findViewById(R.id.pinnedListContainer);

                // Find views in expanded list
                if (pinnedListContainer != null) {
                    View expandedView = pinnedListContainer.getChildAt(0);
                    if (expandedView != null) {
                        pinnedMessagesRecyclerView = expandedView.findViewById(R.id.pinnedMessagesRecyclerView);
                        pinnedMessagesCount = expandedView.findViewById(R.id.pinnedMessagesCount);
                    }
                }
            }
        }

        // Setup pinned messages RecyclerView
        setupPinnedMessagesRecyclerView();

        // Setup click listeners
        if (pinnedBarCollapsed != null) {
            pinnedBarCollapsed.setOnClickListener(v -> {
                // Clicking bar scrolls to latest pinned message
                scrollToLatestPinnedMessage();
            });
        }

        if (expandCollapseButton != null) {
            expandCollapseButton.setOnClickListener(v -> {
                togglePinnedList();
            });
        }
    }

    private void setupPinnedMessagesRecyclerView() {
        if (pinnedMessagesRecyclerView == null) return;

        pinnedMessagesAdapter = new PinnedMessagesAdapter(new PinnedMessagesAdapter.OnPinnedMessageClickListener() {
            @Override
            public void onPinnedMessageClick(Message message) {
                scrollToMessage(message.getId());
                // Optional: collapse list after clicking
                if (isPinnedListExpanded) {
                    togglePinnedList();
                }
            }

            @Override
            public void onUnpinClick(Message message) {
                // Unpin message via ViewModel
                roomViewModel.unpinMessage(conversationId, message.getId());
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        pinnedMessagesRecyclerView.setLayoutManager(layoutManager);
        pinnedMessagesRecyclerView.setAdapter(pinnedMessagesAdapter);
    }

    private void togglePinnedList() {
        if (pinnedListContainer == null || expandCollapseButton == null) return;

        isPinnedListExpanded = !isPinnedListExpanded;

        if (isPinnedListExpanded) {
            // Expand
            pinnedListContainer.setVisibility(View.VISIBLE);
            expandCollapseButton.setRotation(180); // Arrow up
        } else {
            // Collapse
            pinnedListContainer.setVisibility(View.GONE);
            expandCollapseButton.setRotation(0); // Arrow down
        }
    }

    private void scrollToMessage(String messageId) {
        if (messageAdapter == null || messageId == null) return;

        int position = messageAdapter.getPositionOfMessage(messageId);
        if (position >= 0) {
            messagesRecyclerView.smoothScrollToPosition(position);

            // Highlight the message briefly after scroll completes
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                messageAdapter.highlightMessage(messageId);
            }, 300); // Small delay to let scroll complete
        }
    }

    private void scrollToLatestPinnedMessage() {
        // Scroll to the most recent pinned message (one-time observation)
        androidx.lifecycle.LiveData<com.example.doan_zaloclone.utils.Resource<List<Message>>> liveData =
                roomViewModel.getPinnedMessages(conversationId);

        liveData.observe(this, new androidx.lifecycle.Observer<com.example.doan_zaloclone.utils.Resource<List<Message>>>() {
            @Override
            public void onChanged(com.example.doan_zaloclone.utils.Resource<List<Message>> resource) {
                // Remove observer immediately to prevent repeated calls
                liveData.removeObserver(this);

                if (resource != null && resource.isSuccess() && resource.getData() != null) {
                    List<Message> pinnedMsgs = resource.getData();
                    if (!pinnedMsgs.isEmpty()) {
                        // Messages are sorted newest first, so get first one
                        scrollToMessage(pinnedMsgs.get(0).getId());
                    }
                }
            }
        });
    }

    private void setupToolbar() {
        // Do not use setSupportActionBar to avoid default overflow menu
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set initial name
        titleTextView.setText(conversationName != null ? conversationName : "Conversation");

        // Add click listener to open group info or user profile
        titleTextView.setOnClickListener(v -> {
            if ("GROUP".equals(conversationType)) {
                openGroupInfo();
            } else if (!otherUserId.isEmpty()) {
                // Open user profile for 1-1 chat
                com.example.doan_zaloclone.utils.ProfileNavigator.openUserProfile(this, otherUserId);
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

        // Set long-click listener for pin/unpin
        messageAdapter.setOnMessageLongClickListener(new MessageAdapter.OnMessageLongClickListener() {
            @Override
            public void onPinMessage(Message message) {
                roomViewModel.pinMessage(conversationId, message.getId());
            }

            @Override
            public void onUnpinMessage(Message message) {
                roomViewModel.unpinMessage(conversationId, message.getId());
            }
        });

        // Set reply listener
        messageAdapter.setOnMessageReplyListener(new MessageAdapter.OnMessageReplyListener() {
            @Override
            public void onReplyMessage(Message message) {
                showReplyBar(message);
            }
        });

        // Set reply preview click listener - navigate to original message
        messageAdapter.setOnReplyPreviewClickListener(new MessageAdapter.OnReplyPreviewClickListener() {
            @Override
            public void onReplyPreviewClick(String replyToMessageId) {
                scrollToMessage(replyToMessageId);
            }
        });

        // Set recall listener - show confirmation dialog
        messageAdapter.setOnMessageRecallListener(new MessageAdapter.OnMessageRecallListener() {
            @Override
            public void onRecallMessage(Message message) {
                showRecallConfirmDialog(message);
            }
        });

        // Set forward listener - show forward dialog
        messageAdapter.setOnMessageForwardListener(new MessageAdapter.OnMessageForwardListener() {
            @Override
            public void onForwardMessage(Message message) {
                showForwardDialog(message);
            }
        });

        // Set reaction listener - show reaction picker on long press, toggle default reaction on click
        messageAdapter.setOnMessageReactionListener(new MessageAdapter.OnMessageReactionListener() {
            @Override
            public void onReactionClick(Message message, String currentReactionType) {
                // Add reaction
                handleReactionClick(message, currentReactionType);
            }

            @Override
            public void onReactionLongPress(Message message, View anchorView) {
                // Show reaction picker popup
                showReactionPicker(message, anchorView);
            }

            @Override
            public void onReactionStatsClick(Message message) {
                // Show reaction statistics dialog
                showReactionStatsDialog(message);
            }
        });

        // Set poll interaction listener
        messageAdapter.setOnPollInteractionListener(new MessageAdapter.OnPollInteractionListener() {
            @Override
            public void onVotePoll(Message message, String optionId) {
                handlePollVote(message, optionId);
            }

            @Override
            public void onClosePoll(Message message) {
                handleClosePoll(message);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        // Note: Not using setHasFixedSize(true) because messages have variable heights (text vs images)
        messagesRecyclerView.setAdapter(messageAdapter);

        // Disable change animation to prevent flicker when reactions update
        // Keep add/remove animations for smooth list updates
        androidx.recyclerview.widget.RecyclerView.ItemAnimator animator = messagesRecyclerView.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        // Setup swipe-to-reply gesture
        SwipeToReplyCallback swipeCallback = new SwipeToReplyCallback(this, messageAdapter, new SwipeToReplyCallback.SwipeToReplyListener() {
            @Override
            public void onSwipeToReply(int position) {
                if (position >= 0 && position < messageAdapter.getItemCount()) {
                    Message message = messageAdapter.getMessageAt(position);
                    if (message != null) {
                        showReplyBar(message);
                        // Animation is handled by SwipeToReplyCallback.clearView()
                    }
                }
            }
        });
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(messagesRecyclerView);
    }

    private void showReplyBar(Message message) {
        replyingToMessage = message;
        if (replyBarLayout != null) {
            replyBarLayout.setVisibility(View.VISIBLE);

            // Fetch sender name
            String senderId = message.getSenderId();
            String currentUserId = firebaseAuth.getCurrentUser() != null
                    ? firebaseAuth.getCurrentUser().getUid() : "";

            if (senderId.equals(currentUserId)) {
                if (replyingToName != null) {
                    replyingToName.setText("You");
                }
            } else {
                // Try to use cached sender name first
                String cachedName = message.getSenderName();
                if (cachedName != null && !cachedName.isEmpty()) {
                    if (replyingToName != null) {
                        replyingToName.setText(cachedName);
                    }
                } else {
                    // Fallback: Fetch name from Firestore for old messages
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(senderId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String senderName = doc.getString("name");
                                    if (replyingToName != null) {
                                        replyingToName.setText(senderName != null ? senderName : "User");
                                    }
                                }
                            });
                }
            }

            // Set content preview
            if (replyingToContent != null) {
                String preview = message.getContent();
                if (message.isImageMessage()) {
                    preview = "📷 Hình ảnh";
                } else if (message.isFileMessage()) {
                    preview = "📎 " + (message.getFileName() != null ? message.getFileName() : "File");
                }
                replyingToContent.setText(preview);
            }

            // Focus on input
            messageEditText.requestFocus();
        }
    }

    private void hideReplyBar() {
        replyingToMessage = null;
        if (replyBarLayout != null) {
            replyBarLayout.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        // Observe messages via ViewModel
        roomViewModel.getMessages(conversationId).observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                List<Message> newMessages = resource.getData();

                if (newMessages != null) {
                    // Check if this is a new message addition (not just an update)
                    int oldSize = messages != null ? messages.size() : 0;
                    int newSize = newMessages.size();
                    boolean isNewMessage = newSize > oldSize;

                    // Store for counting
                    messages = newMessages;

                    // Update adapter
                    messageAdapter.updateMessages(newMessages);

                    // Only auto-scroll if there's a NEW message added
                    // Don't scroll for reaction updates or other changes
                    if (isNewMessage && messageAdapter.getItemCount() > 0) {
                        messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }
            } else if (resource.isError()) {
                Toast.makeText(this, "Error loading messages: " + resource.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Observe pinned messages
        roomViewModel.getPinnedMessages(conversationId).observe(this, resource -> {
            android.util.Log.d("RoomActivity", "Pinned messages observer called, resource: " + resource);
            if (resource == null) {
                android.util.Log.d("RoomActivity", "Pinned messages resource is null");
                return;
            }

            if (resource.isLoading()) {
                android.util.Log.d("RoomActivity", "Pinned messages: LOADING");
                return;
            }

            if (resource.isSuccess() && resource.getData() != null) {
                List<Message> pinnedMessages = resource.getData();
                android.util.Log.d("RoomActivity", "Pinned messages SUCCESS: count=" + pinnedMessages.size());
                updatePinnedMessagesUI(pinnedMessages);
            } else if (resource.isError()) {
                android.util.Log.e("RoomActivity", "Error loading pinned messages: " + resource.getMessage());
            }
        });
        // Observe pin/unpin operations
        roomViewModel.getPinMessageState().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                // Reload pinned messages will happen automatically via auto-reload in ViewModel
                Toast.makeText(this, "Đã cập nhật ghim", Toast.LENGTH_SHORT).show();
                roomViewModel.resetPinMessageState();
            } else if (resource.isError()) {
                Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                roomViewModel.resetPinMessageState();
            }
        });

        // Observe reaction operations
        roomViewModel.getReactionState().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                android.util.Log.d("RoomActivity", "Reaction updated successfully - will update via WebSocket");

                // Note: UI will be updated via WebSocket reaction_updated event
                // No need to manually update here as ChatRepository handles WebSocket events

                roomViewModel.resetReactionState();
            } else if (resource.isError()) {
                Toast.makeText(this, "Lỗi cập nhật reaction: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                android.util.Log.e("RoomActivity", "Error updating reaction: " + resource.getMessage());
                roomViewModel.resetReactionState();
            }
        });

        // Observe group left/deleted events - auto-finish if this conversation is deleted
        conversationRepository.getGroupLeftEvent().observe(this, deletedConversationId -> {
            if (deletedConversationId != null && deletedConversationId.equals(conversationId)) {
                android.util.Log.d("RoomActivity", "Conversation deleted, finishing activity: " + conversationId);
                Toast.makeText(this, "Nhóm đã bị xóa hoặc bạn đã rời nhóm", Toast.LENGTH_SHORT).show();
                finish();
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

        // Sticker button
        stickerButton.setOnClickListener(v -> showStickerPicker());

        // TextWatcher to toggle Send button visibility
        messageEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    sendButton.setVisibility(View.VISIBLE);
                    moreActionsButton.setVisibility(View.GONE);
                    if (voiceRecordButton != null) voiceRecordButton.setVisibility(View.GONE);
                    attachImageButton.setVisibility(View.GONE);
                } else {
                    sendButton.setVisibility(View.GONE);
                    moreActionsButton.setVisibility(View.VISIBLE);
                    if (voiceRecordButton != null) voiceRecordButton.setVisibility(View.VISIBLE);
                    attachImageButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
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

    /**
     * Public method to trigger image picker from QuickActions
     */
    public void triggerImagePicker() {
        requestPermissionAndShowPicker();
    }

    private void loadAndShowImagePicker() {
        // Show loading indicator
        Toast.makeText(this, "Đang tải ảnh...", Toast.LENGTH_SHORT).show();

        android.util.Log.d("RoomActivity", "Starting to load image picker");

        // Load photos in background thread to prevent ANR
        new Thread(() -> {
            try {
                // Load photos from device (limited to 100 most recent)
                List<Uri> photos = MediaStoreHelper.loadPhotos(this, 100);

                android.util.Log.d("RoomActivity", "Loaded " + photos.size() + " photos");

                // Update UI on main thread
                runOnUiThread(() -> {
                    if (photos.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy ảnh nào trên thiết bị", Toast.LENGTH_SHORT).show();
                    } else {
                        setupImagePickerUI(photos);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("RoomActivity", "Error loading photos", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
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

        // Set sender name from Firebase Auth or fetch from Firestore if needed
        String displayName = firebaseAuth.getCurrentUser().getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            newMessage.setSenderName(displayName);
            // Continue with send
            sendMessageNow(newMessage);
        } else {
            // Fetch from Firestore
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                newMessage.setSenderName(name);
                            }
                        }
                        sendMessageNow(newMessage);
                    })
                    .addOnFailureListener(e -> {
                        // Send anyway without name
                        sendMessageNow(newMessage);
                    });
            // Exit early, will send after fetch
        }
    }

    private void sendMessageNow(Message newMessage) {
        // Debug: Check replyingToMessage state
        android.util.Log.d("RoomActivity", "handleSendMessage - replyingToMessage: " +
                (replyingToMessage != null ? "SET (id=" + replyingToMessage.getId() + ")" : "NULL"));

        // Add reply data if replying to a message
        if (replyingToMessage != null) {
            newMessage.setReplyToId(replyingToMessage.getId());

            // Set reply content preview
            String replyContent = replyingToMessage.getContent();
            if (replyingToMessage.isImageMessage()) {
                replyContent = "📷 Hình ảnh";
            } else if (replyingToMessage.isFileMessage()) {
                replyContent = "📎 " + (replyingToMessage.getFileName() != null ? replyingToMessage.getFileName() : "File");
            }
            newMessage.setReplyToContent(replyContent);

            newMessage.setReplyToSenderId(replyingToMessage.getSenderId());

            // Set sender name - get from UI if available
            if (replyingToName != null) {
                newMessage.setReplyToSenderName(replyingToName.getText().toString());
            }

            // Hide reply bar after setting data
            hideReplyBar();
        }

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
                // Check which action triggered this
                if (action instanceof com.example.doan_zaloclone.ui.room.actions.SendContactAction) {
                    // Show friend selection dialog for business card
                    showSendContactDialog();
                } else if (action instanceof com.example.doan_zaloclone.ui.room.actions.SendPollAction) {
                    // Launch poll creation activity
                    showCreatePollActivity();
                } else if (action instanceof com.example.doan_zaloclone.ui.room.actions.SendLocationAction) {
                    // Launch location picker activity
                    launchLocationPicker();
                } else {
                    // Launch file picker for other actions
                    launchFilePicker();
                }
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
            public void onPollCreated(com.example.doan_zaloclone.models.Poll poll) {
                // Send poll message (to be implemented in Phase 5)
                // sendPollMessage(poll);
                Toast.makeText(RoomActivity.this, "Poll created: " + poll.getQuestion(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationSelected(double latitude, double longitude, String locationName, String locationAddress) {
                // Handle location selected (Phase 3 - not yet implemented)
                Toast.makeText(RoomActivity.this, "Location selected: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
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
        // Default to multi-file picker (can still select just 1 file)
        com.example.doan_zaloclone.utils.FilePickerHelper.launchMultipleFilePicker(multipleFilePickerLauncher);
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

    private void handleMultipleFilesSelected(List<Uri> uris) {
        selectedFilesForPreview.clear();

        // Process each URI and create FileItem
        for (Uri uri : uris) {
            String name = com.example.doan_zaloclone.utils.FileUtils.getFileName(this, uri);
            long size = com.example.doan_zaloclone.utils.FileUtils.getFileSize(this, uri);
            String mimeType = com.example.doan_zaloclone.utils.FileUtils.getMimeType(this, uri);

            selectedFilesForPreview.add(new FilePreviewAdapter.FileItem(uri, name, size, mimeType));
        }

        // Show file preview bottom sheet
        showFilePreviewBottomSheet();
    }

    private void showFilePreviewBottomSheet() {
        // Inflate bottom sheet
        View bottomSheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_file_picker, actionMenuContainer, false);

        // Setup RecyclerView with FilePreviewAdapter
        RecyclerView filesRecyclerView = bottomSheetView.findViewById(R.id.filesRecyclerView);
        filesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        TextView selectedCountTextView = bottomSheetView.findViewById(R.id.selectedCountTextView);
        if (selectedCountTextView == null) {
            // If TextView doesn't exist in layout, we'll handle it differently
            android.widget.Button sendButton = bottomSheetView.findViewById(R.id.sendFileButton);
            sendButton.setText("Gửi (" + selectedFilesForPreview.size() + " files)");
        }

        filePreviewAdapter = new FilePreviewAdapter(selectedFilesForPreview, selectedCount -> {
            // Update button text when selection changes
            android.widget.Button sendButton = bottomSheetView.findViewById(R.id.sendFileButton);
            sendButton.setText("Gửi (" + selectedCount + " files)");
            sendButton.setEnabled(selectedCount > 0);
        });
        filesRecyclerView.setAdapter(filePreviewAdapter);

        // Setup buttons
        android.widget.Button cancelButton = bottomSheetView.findViewById(R.id.cancelButton);
        android.widget.Button sendButton = bottomSheetView.findViewById(R.id.sendFileButton);

        cancelButton.setOnClickListener(v -> hideFilePreviewBottomSheet());
        sendButton.setOnClickListener(v -> {
            hideFilePreviewBottomSheet();
            uploadAndSendMultipleFiles();
        });

        // Show bottom sheet
        actionMenuContainer.removeAllViews();
        actionMenuContainer.addView(bottomSheetView);
        actionMenuContainer.setVisibility(View.VISIBLE);
    }

    private void hideFilePreviewBottomSheet() {
        actionMenuContainer.setVisibility(View.GONE);
        actionMenuContainer.removeAllViews();
    }

    private void uploadAndSendMultipleFiles() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        List<FilePreviewAdapter.FileItem> filesToSend = filePreviewAdapter.getSelectedFiles();
        if (filesToSend.isEmpty()) {
            Toast.makeText(this, "Chưa chọn file nào", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Toast.makeText(this, "Đang gửi " + filesToSend.size() + " files...", Toast.LENGTH_SHORT).show();

        // Upload each file sequentially
        uploadFilesSequentially(filesToSend, currentUserId, 0, filesToSend.size());
    }

    private void uploadFilesSequentially(List<FilePreviewAdapter.FileItem> files, String userId,
                                         int currentIndex, int total) {
        if (currentIndex >= files.size()) {
            // All files uploaded
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã gửi tất cả " + total + " files!", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        FilePreviewAdapter.FileItem file = files.get(currentIndex);
        int fileNumber = currentIndex + 1;

        chatRepository.uploadFileAndSendMessage(
                conversationId,
                file.uri,
                userId,
                file.name,
                file.size,
                file.mimeType,
                new ChatRepository.SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this,
                                    "Đã gửi file " + fileNumber + "/" + total,
                                    Toast.LENGTH_SHORT).show();
                        });
                        // Upload next file
                        uploadFilesSequentially(files, userId, currentIndex + 1, total);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this,
                                    "Lỗi gửi file " + fileNumber + ": " + error,
                                    Toast.LENGTH_LONG).show();
                        });
                        // Continue with next file despite error
                        uploadFilesSequentially(files, userId, currentIndex + 1, total);
                    }
                }
        );
    }


    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.room_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_file_management) {
            openFileManagement();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFileManagement() {
        android.content.Intent intent = new android.content.Intent(this,
                com.example.doan_zaloclone.ui.file.FileManagementActivity.class);
        intent.putExtra(com.example.doan_zaloclone.ui.file.FileManagementActivity.EXTRA_CONVERSATION_ID,
                conversationId);
        intent.putExtra(com.example.doan_zaloclone.ui.file.FileManagementActivity.EXTRA_CONVERSATION_NAME,
                conversationName);
        startActivity(intent);
    }

    private void openGroupInfo() {
        android.content.Intent intent = new android.content.Intent(this,
                com.example.doan_zaloclone.ui.group.GroupInfoActivity.class);
        intent.putExtra("conversationId", conversationId);
        startActivity(intent);
    }

    /**
     * Start a call (video or voice)
     *
     * @param isVideo true for video call, false for voice call
     */
    private void startCall(boolean isVideo) {
        // Check and request permissions
        if (!PermissionHelper.checkCallPermissions(this, isVideo)) {
            PermissionHelper.requestCallPermissions(this, isVideo,
                    isVideo ? PermissionHelper.REQUEST_CODE_VIDEO_CALL : PermissionHelper.REQUEST_CODE_VOICE_CALL);
            return;
        }

        // Get current user ID
        String currentUserId = firebaseAuth.getCurrentUser() != null ?
                firebaseAuth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
            return;
        }

        // For group calls, show a message (not yet implemented)
        if ("GROUP".equals(conversationType)) {
            Toast.makeText(this, "Cuộc gọi nhóm sẽ sớm được hỗ trợ", Toast.LENGTH_SHORT).show();
            return;
        }

        // For 1-1 calls, ensure we have otherUserId
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Không thể xác định người nhận cuộc gọi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start call activity
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(CallActivity.EXTRA_CALLER_ID, currentUserId);
        intent.putExtra(CallActivity.EXTRA_RECEIVER_ID, otherUserId);
        intent.putExtra(CallActivity.EXTRA_IS_VIDEO, isVideo);
        intent.putExtra(CallActivity.EXTRA_IS_INCOMING, false);
        intent.putExtra(CallActivity.EXTRA_CALLER_NAME, conversationName);
        startActivity(intent);
    }

    private void updatePinnedMessagesUI(List<Message> pinnedMessages) {
        if (pinnedMessages == null || pinnedMessages.isEmpty()) {
            // Hide pinned bar
            if (pinnedMessagesContainer != null) {
                pinnedMessagesContainer.setVisibility(View.GONE);
            }
            // Clear pinned status in adapter
            if (messageAdapter != null) {
                messageAdapter.setPinnedMessageIds(null);
            }
            return;
        }

        // Show pinned bar
        if (pinnedMessagesContainer != null) {
            pinnedMessagesContainer.setVisibility(View.VISIBLE);
        }

        // Update preview with latest pinned message
        Message latestPinned = pinnedMessages.get(0); // Already sorted newest first
        if (pinnedMessagePreview != null) {
            String preview = getMessagePreview(latestPinned);
            pinnedMessagePreview.setText(preview);
        }

        // Update count
        if (pinnedMessagesCount != null) {
            pinnedMessagesCount.setText("(" + pinnedMessages.size() + ")");
        }

        // Update pinned messages adapter
        if (pinnedMessagesAdapter != null) {
            pinnedMessagesAdapter.setPinnedMessages(pinnedMessages);
        }

        // Update main message adapter with pinned IDs for icons and smart context menu
        if (messageAdapter != null) {
            java.util.List<String> pinnedIds = new java.util.ArrayList<>();
            for (Message msg : pinnedMessages) {
                pinnedIds.add(msg.getId());
            }
            messageAdapter.setPinnedMessageIds(pinnedIds);
        }
    }

    private String getMessagePreview(Message message) {
        if (message == null) return "";

        switch (message.getType()) {
            case Message.TYPE_IMAGE:
                return "📷 Hình ảnh";
            case Message.TYPE_FILE:
                String fileName = message.getFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    return "📎 " + fileName;
                }
                return "📎 File";
            case Message.TYPE_CALL:
                return "📞 Cuộc gọi";
            case Message.TYPE_TEXT:
            default:
                return message.getContent() != null ? message.getContent() : "";
        }
    }

    /**
     * Show confirmation dialog before recalling a message
     */
    private void showRecallConfirmDialog(Message message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Thu hồi tin nhắn")
                .setMessage("Bạn có chắc muốn thu hồi tin nhắn này?")
                .setPositiveButton("Thu hồi", (dialog, which) -> {
                    recallMessage(message);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Recall a message via ChatRepository (using REST API)
     */
    private void recallMessage(Message message) {
        if (chatRepository == null || conversationId == null) {
            Toast.makeText(this, "Lỗi: Không thể thu hồi tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the API-based recall method
        chatRepository.recallMessage(conversationId, message.getId(), new ChatRepository.SendMessageCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RoomActivity.this, "Đã thu hồi tin nhắn", Toast.LENGTH_SHORT).show();

                // Update UI immediately without waiting for WebSocket
                // Find and update the message in local list
                if (messages != null) {
                    for (int i = 0; i < messages.size(); i++) {
                        Message msg = messages.get(i);
                        if (msg.getId() != null && msg.getId().equals(message.getId())) {
                            // Create updated message with recalled state
                            msg.setRecalled(true);
                            msg.setContent("Tin nhắn đã bị thu hồi");

                            // Update adapter
                            List<Message> updatedMessages = new java.util.ArrayList<>(messages);
                            messageAdapter.updateMessages(updatedMessages);

                            android.util.Log.d("RoomActivity", "UI updated after recall - message: " + message.getId());
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RoomActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show forward message dialog
     */
    private void showForwardDialog(Message message) {
        // Inflate dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forward_message, null);

        // Find views
        EditText searchEditText = dialogView.findViewById(R.id.searchEditText);
        TextView selectedCountText = dialogView.findViewById(R.id.selectedCountText);
        RecyclerView friendsRecyclerView = dialogView.findViewById(R.id.friendsRecyclerView);
        TextView emptyStateText = dialogView.findViewById(R.id.emptyStateText);

        // Create final reference for use in callbacks
        final View sendBtn = dialogView.findViewById(R.id.sendButton);

        // Setup RecyclerView
        ForwardFriendsAdapter adapter = new ForwardFriendsAdapter(new ArrayList<>(), selectedCount -> {
            // Handle selection change - update UI
            if (selectedCount > 0) {
                selectedCountText.setVisibility(View.VISIBLE);
                selectedCountText.setText("Đã chọn " + selectedCount + " bạn");
                sendBtn.setEnabled(true);
            } else {
                selectedCountText.setVisibility(View.GONE);
                sendBtn.setEnabled(false);
            }
        });

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(adapter);

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Setup buttons
        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.sendButton).setOnClickListener(v -> {
            java.util.List<String> selectedFriendIds = adapter.getSelectedFriendIds();
            if (selectedFriendIds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một người nhận", Toast.LENGTH_SHORT).show();
                return;
            }

            // Forward message to all selected friends
            String currentUserId = firebaseAuth.getCurrentUser() != null
                    ? firebaseAuth.getCurrentUser().getUid() : "";

            // Get sender name for forwarding
            fetchSenderNameAndForward(message, selectedFriendIds, currentUserId, dialog);
        });

        // Disable send button initially (enabled via selection listener)
        dialogView.findViewById(R.id.sendButton).setEnabled(false);

        // Setup search
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        // Load friends
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid() : "";
        contactViewModel.getFriends(currentUserId).observe(this, resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                java.util.List<com.example.doan_zaloclone.models.User> friends = resource.getData();
                if (friends.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    friendsRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    friendsRecyclerView.setVisibility(View.VISIBLE);
                    adapter.updateFriends(friends);
                }
            }
        });

        dialog.show();
    }

    /**
     * Fetch sender name and forward message
     */
    private void fetchSenderNameAndForward(Message message, java.util.List<String> friendIds, String currentUserId, AlertDialog dialog) {
        // Try to use cached sender name first
        String cachedName = message.getSenderName();
        if (cachedName != null && !cachedName.isEmpty()) {
            // Use cached name directly
            forwardMessageToFriends(message, friendIds, currentUserId, cachedName, dialog);
        } else {
            // Fallback: Fetch sender name from Firestore for old messages
            String senderId = message.getSenderId();

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(senderId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String senderName = "User";
                        if (doc.exists()) {
                            senderName = doc.getString("name");
                            if (senderName == null) senderName = "User";
                        }

                        // Forward to each selected friend
                        forwardMessageToFriends(message, friendIds, currentUserId, senderName, dialog);
                    })
                    .addOnFailureListener(e -> {
                        // Use default name on error
                        forwardMessageToFriends(message, friendIds, currentUserId, "User", dialog);
                    });
        }
    }

    /**
     * Forward message to selected friends
     */
    private void forwardMessageToFriends(Message message, java.util.List<String> friendIds, String currentUserId, String originalSenderName, AlertDialog dialog) {
        final int[] successCount = {0};
        final int[] errorCount = {0};
        final int totalCount = friendIds.size();

        android.util.Log.d("RoomActivity", "forwardMessageToFriends - currentUserId: " + currentUserId + ", friendIds: " + friendIds);

        for (String friendId : friendIds) {
            android.util.Log.d("RoomActivity", "Forwarding to friendId: " + friendId);
            // Get or create conversation with friend
            chatRepository.getOrCreateConversationWithFriend(currentUserId, friendId, new ChatRepository.ConversationCallback() {
                @Override
                public void onSuccess(String targetConversationId) {
                    // Forward the message
                    chatRepository.forwardMessage(targetConversationId, message, currentUserId, originalSenderName, new ChatRepository.ForwardMessageCallback() {
                        @Override
                        public void onSuccess() {
                            successCount[0]++;
                            checkForwardCompletion(successCount[0], errorCount[0], totalCount, dialog);
                        }

                        @Override
                        public void onError(String error) {
                            errorCount[0]++;
                            checkForwardCompletion(successCount[0], errorCount[0], totalCount, dialog);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    errorCount[0]++;
                    checkForwardCompletion(successCount[0], errorCount[0], totalCount, dialog);
                }
            });
        }
    }

    /**
     * Check if all forward operations completed
     */
    private void checkForwardCompletion(int successCount, int errorCount, int totalCount, AlertDialog dialog) {
        if (successCount + errorCount == totalCount) {
            dialog.dismiss();
            if (errorCount == 0) {
                Toast.makeText(this, "Đã chuyển tiếp tin nhắn đến " + successCount + " người", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã chuyển tiếp: " + successCount + ", Lỗi: " + errorCount, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =============== BUSINESS CARD / CONTACT HANDLING ===============

    /**
     * Show dialog to select a friend to send as business card
     */
    private void showSendContactDialog() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để gửi danh thiếp", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        SendContactDialog dialog = new SendContactDialog(this, currentUserId, this);

        dialog.setOnContactSelectedListener(contactUserId -> {
            sendContactMessage(contactUserId);
        });

        dialog.show();
    }

    /**
     * Send a contact message (business card)
     *
     * @param contactUserId User ID of the contact to share
     */
    private void sendContactMessage(String contactUserId) {
        if (firebaseAuth.getCurrentUser() == null || contactUserId == null) {
            Toast.makeText(this, "Không thể gửi danh thiếp", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Create contact message
        Message contactMessage = new Message();
        contactMessage.setType(Message.TYPE_CONTACT);
        contactMessage.setContactUserId(contactUserId);
        contactMessage.setSenderId(currentUserId);
        contactMessage.setTimestamp(System.currentTimeMillis());

        // Fetch sender name
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            contactMessage.setSenderName(name);
                        }
                    }
                    // Send message via ViewModel
                    roomViewModel.sendMessage(conversationId, contactMessage);
                    Toast.makeText(this, "Đã gửi danh thiếp", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Send without senderName
                    roomViewModel.sendMessage(conversationId, contactMessage);
                    Toast.makeText(this, "Đã gửi danh thiếp", Toast.LENGTH_SHORT).show();
                });
    }

    // =============== REACTION HANDLING ===============

    /**
     * Handle reaction click - add reaction (Zalo style, does not toggle off)
     * When clicking the add button, adds heart. When clicking existing reaction, adds that type.
     */
    private void handleReactionClick(Message message, String reactionType) {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Không thể thêm reaction", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("RoomActivity", "handleReactionClick - messageId: " + message.getId()
                + ", reactionType: " + reactionType);

        // Just call API - WebSocket/Server will push update to refresh UI
        // Removed optimistic update to avoid race condition with server response
        roomViewModel.toggleReaction(conversationId, message.getId(), currentUserId, reactionType);
    }

    /**
     * Show reaction picker popup above the message
     */
    private void showReactionPicker(Message message, View anchorView) {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Không thể thêm reaction", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("RoomActivity", "showReactionPicker - messageId: " + message.getId());

        // Create and show reaction picker popup
        ReactionPickerPopup popup = new ReactionPickerPopup(this, reactionType -> {
            // User selected a reaction or remove (null)
            android.util.Log.d("RoomActivity", "Reaction selected: " + reactionType + " for message: " + message.getId());

            // Just call API - WebSocket/Server will push update to refresh UI
            // Removed optimistic update to avoid race condition with server response
            if (reactionType == null) {
                // Remove user's reaction
                roomViewModel.removeReaction(conversationId, message.getId(), currentUserId);
            } else {
                // Add/update reaction
                roomViewModel.toggleReaction(conversationId, message.getId(), currentUserId, reactionType);
            }
        });

        // Show popup above the message bubble
        popup.showAboveAnchor(anchorView);
    }

    /**
     * Show reaction statistics dialog
     */
    private void showReactionStatsDialog(Message message) {
        if (message == null || !message.hasReactions()) {
            return;
        }

        // Get reaction counts
        java.util.Map<String, Integer> reactionCounts =
                com.example.doan_zaloclone.models.MessageReaction.getReactionTypeCounts(
                        message.getReactions(), message.getReactionCounts());

        // Build dialog content
        StringBuilder statsText = new StringBuilder();
        statsText.append("Thống kê cảm xúc:\n\n");

        int totalReactions = 0;
        if (message.getReactionCounts() != null) {
            for (Integer count : message.getReactionCounts().values()) {
                totalReactions += count;
            }
        } else {
            totalReactions = message.getReactions() != null ? message.getReactions().size() : 0;
        }

        // Add each reaction type with count
        if (reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_HEART) > 0) {
            statsText.append("❤️ Thích: ")
                    .append(reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_HEART))
                    .append("\n");
        }
        if (reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_HAHA) > 0) {
            statsText.append("😂 Haha: ")
                    .append(reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_HAHA))
                    .append("\n");
        }
        if (reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_WOW) > 0) {
            statsText.append("😮 Wow: ")
                    .append(reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_WOW))
                    .append("\n");
        }
        if (reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_SAD) > 0) {
            statsText.append("😢 Buồn: ")
                    .append(reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_SAD))
                    .append("\n");
        }
        if (reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_ANGRY) > 0) {
            statsText.append("😠 Phẫn nộ: ")
                    .append(reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_ANGRY))
                    .append("\n");
        }
        if (reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_LIKE) > 0) {
            statsText.append("👍 Like: ")
                    .append(reactionCounts.get(com.example.doan_zaloclone.models.MessageReaction.REACTION_LIKE))
                    .append("\n");
        }

        statsText.append("\nTổng số lượt: ").append(totalReactions);

        // Show dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cảm xúc của tin nhắn")
                .setMessage(statsText.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    // ============== POLL METHODS ==============

    /**
     * Show poll creation activity
     */
    private void showCreatePollActivity() {
        Intent intent = new Intent(this, CreatePollActivity.class);
        intent.putExtra(CreatePollActivity.EXTRA_CONVERSATION_NAME, conversationName);
        createPollLauncher.launch(intent);
    }

    /**
     * Send poll message to conversation
     */
    private void sendPollMessage(com.example.doan_zaloclone.models.Poll poll) {
        if (poll == null || firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Lỗi: Không thể gửi poll", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        chatRepository.sendPollMessage(conversationId, currentUserId, poll,
                new ChatRepository.SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Đã tạo poll", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // ===================== LOCATION HANDLING METHODS =====================

    /**
     * Launch location picker activity
     */
    private void launchLocationPicker() {
        Intent intent = new Intent(this, com.example.doan_zaloclone.ui.location.LocationPickerActivity.class);
        locationPickerLauncher.launch(intent);
    }

    /**
     * Send location message
     */
    private void sendLocationMessage(double latitude, double longitude, String locationName, String locationAddress) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để gửi vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        // Debug log
        android.util.Log.d("RoomActivity", "sendLocationMessage called with:");
        android.util.Log.d("RoomActivity", "  Latitude: " + latitude);
        android.util.Log.d("RoomActivity", "  Longitude: " + longitude);
        android.util.Log.d("RoomActivity", "  LocationName: " + locationName);
        android.util.Log.d("RoomActivity", "  LocationAddress: " + locationAddress);

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Create location message
        Message locationMessage = new Message();
        locationMessage.setSenderId(currentUserId);
        locationMessage.setType(Message.TYPE_LOCATION);
        locationMessage.setLatitude(latitude);
        locationMessage.setLongitude(longitude);
        locationMessage.setLocationName(locationName);
        locationMessage.setLocationAddress(locationAddress);
        locationMessage.setTimestamp(System.currentTimeMillis());

        // Set sender name
        String displayName = firebaseAuth.getCurrentUser().getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            locationMessage.setSenderName(displayName);
        }

        // Debug log after setting
        android.util.Log.d("RoomActivity", "Created message with:");
        android.util.Log.d("RoomActivity", "  Message.getLatitude(): " + locationMessage.getLatitude());
        android.util.Log.d("RoomActivity", "  Message.getLongitude(): " + locationMessage.getLongitude());

        // Send via ViewModel
        roomViewModel.sendMessage(conversationId, locationMessage);

        Toast.makeText(this, "Đã gửi vị trí", Toast.LENGTH_SHORT).show();
    }

    /**
     * Send live location message
     */
    private void sendLiveLocationMessage(String sessionId) {
        if (firebaseAuth.getCurrentUser() == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        Message liveLocationMessage = new Message();
        liveLocationMessage.setSenderId(currentUserId);
        liveLocationMessage.setType(Message.TYPE_LIVE_LOCATION);
        liveLocationMessage.setLiveLocationSessionId(sessionId);
        liveLocationMessage.setContent("Đang chia sẻ vị trí trực tiếp");
        liveLocationMessage.setTimestamp(System.currentTimeMillis());

        // Set sender name
        String displayName = firebaseAuth.getCurrentUser().getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            liveLocationMessage.setSenderName(displayName);
        }

        roomViewModel.sendMessage(conversationId, liveLocationMessage);
        Toast.makeText(this, "Đang chia sẻ vị trí trực tiếp...", Toast.LENGTH_SHORT).show();
    }

    // ===================== POLL INTERACTION HANDLERS =====================

    /**
     * Handle voting for a poll option
     */
    private void handlePollVote(Message message, String optionId) {
        if (message == null || optionId == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        chatRepository.votePoll(conversationId, message.getId(), optionId, currentUserId,
                new ChatRepository.VotePollCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            // Poll will auto-update via Firestore listener
                            android.util.Log.d("RoomActivity", "Vote successful");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Handle closing a poll
     */
    private void handleClosePoll(Message message) {
        if (message == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        // TODO: Check if user is group admin (for now, pass false)
        boolean isGroupAdmin = false;

        chatRepository.closePoll(conversationId, message.getId(), currentUserId, isGroupAdmin,
                new ChatRepository.VotePollCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Đã đóng bình chọn", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(RoomActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // ========== Sticker Methods ==========

    /**
     * Show sticker picker bottom sheet
     */
    private void showStickerPicker() {
        StickerPickerFragment stickerPicker = new StickerPickerFragment();
        stickerPicker.setOnStickerSelectedListener(sticker -> {
            sendStickerMessage(sticker);
        });
        stickerPicker.show(getSupportFragmentManager(), "StickerPicker");
    }

    /**
     * Send a sticker message
     */
    private void sendStickerMessage(com.example.doan_zaloclone.models.Sticker sticker) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để gửi sticker", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate sticker data
        if (sticker == null || sticker.getId() == null || sticker.getImageUrl() == null) {
            android.util.Log.e("StickerMessage", "Invalid sticker data: " + (sticker != null ? sticker.getId() : "null"));
            Toast.makeText(this, "Sticker bị lỗi dữ liệu, vui lòng chọn sticker khác", Toast.LENGTH_SHORT).show();
            return;
        }

        // Debug log
        android.util.Log.d("StickerMessage", "sendStickerMessage - sticker.getId(): " + sticker.getId()
                + ", sticker.getImageUrl(): " + sticker.getImageUrl()
                + ", sticker.getPackId(): " + sticker.getPackId());

        String senderId = firebaseAuth.getCurrentUser().getUid();

        // Create sticker message
        Message stickerMessage = new Message();
        stickerMessage.setId(java.util.UUID.randomUUID().toString());
        stickerMessage.setSenderId(senderId);
        stickerMessage.setType(Message.TYPE_STICKER);
        stickerMessage.setTimestamp(System.currentTimeMillis());
        stickerMessage.setStickerId(sticker.getId());
        stickerMessage.setStickerPackId(sticker.getPackId());
        stickerMessage.setStickerUrl(sticker.getImageUrl());
        stickerMessage.setStickerAnimated(sticker.isAnimated());

        // Set sender name
        String displayName = firebaseAuth.getCurrentUser().getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            stickerMessage.setSenderName(displayName);
        }

        // Send via ViewModel
        roomViewModel.sendMessage(conversationId, stickerMessage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup recording resources
        cleanupRecording();
        deleteAudioFile();
        // ViewModel will automatically clean up listeners
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop recording if activity is paused
        if (isRecording) {
            stopVoiceRecording();
        }
    }
}
