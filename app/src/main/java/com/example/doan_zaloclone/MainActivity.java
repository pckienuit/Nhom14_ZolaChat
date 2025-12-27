package com.example.doan_zaloclone;

import android.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.doan_zaloclone.models.Call;
import com.example.doan_zaloclone.repository.CallRepository;
import com.example.doan_zaloclone.ui.call.CallActivity;
import com.example.doan_zaloclone.ui.contact.ContactFragment;
import com.example.doan_zaloclone.ui.home.HomeFragment;
import com.example.doan_zaloclone.ui.login.LoginActivity;
import com.example.doan_zaloclone.ui.newsfeed.NewsfeedFragment;
import com.example.doan_zaloclone.ui.personal.PersonalFragment;
import com.example.doan_zaloclone.utils.NotificationHelper;
import com.example.doan_zaloclone.viewmodel.MainViewModel;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout navMessages, navContact, navGrid, navTimeline, navPersonal;
    private ImageView iconMessages, iconContact, iconGrid, iconTimeline, iconPersonal;
    private TextView textMessages, textContact, textGrid, textTimeline, textPersonal;
    private Toolbar toolbar;
    private MainViewModel mainViewModel;

    // Cache fragments to preserve state
    private HomeFragment homeFragment;
    private ContactFragment contactFragment;
    private NewsfeedFragment newsfeedFragment;
    private PersonalFragment personalFragment;

    // Incoming call listener
    private CallRepository callRepository;
    private ListenerRegistration incomingCallListener;
    private String lastHandledCallId = null;  // Track last handled call to avoid duplicates

    // Force logout listener
    private ListenerRegistration forceLogoutListener;
    private long loginTimestamp = 0;
    
    // Unread badge view
    private TextView badgeMessages;
    private TextView badgeContact;
    
    // Friend repository for badge updates
    private com.example.doan_zaloclone.repository.FriendRepository friendRepository;
    
    // Track active conversation to avoid notifications
    private static String activeConversationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_main);

        // Create notification channels
        NotificationHelper.createNotificationChannels(this);
        
        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Record login timestamp for force logout comparison
        loginTimestamp = System.currentTimeMillis();

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Initialize Custom Navigation
        initCustomBottomNav();

        if (savedInstanceState == null) {
            // Show home fragment on first launch
            showFragment(0);
        }

        // Setup friend events via WebSocket
        setupFriendEventListener();
        
        // Setup notification listener for messages and friend requests
        setupNotificationListener();

        // Apply window insets for Edge-to-Edge Navigation Bar
        View bottomNav = findViewById(R.id.bottom_navigation_container);
        if (bottomNav != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                // Apply bottom padding = system nav bar height
                // Keep existing horizontal padding if any (likely 0)
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
                return androidx.core.view.WindowInsetsCompat.CONSUMED;
            });
        }

        // Start listening for incoming calls
        setupIncomingCallListener();

        // Start listening for force logout / ban
        setupForceLogoutListener();

        // Setup friend events via WebSocket
        setupFriendEventListener();
        
        // Initialize FriendRepository and load friend requests badge
        setupFriendRequestsBadge();
        
        // Start NotificationService to maintain WebSocket in background
        startNotificationService();

        // TEMPORARY: API Test Button - Only in DEBUG builds
        // addApiTestButton();
    }
    
    /**
     * Start NotificationService as foreground service
     * This maintains WebSocket connection when app is in background
     */
    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, com.example.doan_zaloclone.services.NotificationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d("MainActivity", "NotificationService started");
    }
    
    /**
     * Request notification permission for Android 13+
     */
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                    1001
                );
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission");
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted");
            }
        }
        
        // Also request full-screen intent permission for calls
        requestFullScreenIntentPermission();
    }
    
    /**
     * Request full-screen intent permission for Android 14+
     * This allows incoming calls to pop up automatically
     */
    private void requestFullScreenIntentPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires explicit permission
            if (!getSystemService(android.app.NotificationManager.class).canUseFullScreenIntent()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                    Toast.makeText(this, 
                        "Vui lòng bật 'Full screen intent' để nhận cuộc gọi toàn màn hình", 
                        Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to open full screen intent settings", e);
                }
            } else {
                Log.d("MainActivity", "Full screen intent permission already granted");
            }
        }
        
        // Also request overlay permission for background activity launch
        requestOverlayPermission();
    }
    
    /**
     * Request overlay permission for displaying over other apps
     * Required for Android 10+ to show incoming call screen
     */
    private void requestOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName())
                );
                try {
                    startActivity(intent);
                    Toast.makeText(this, 
                        "Vui lòng bật 'Hiển thị trên ứng dụng khác' để nhận cuộc gọi", 
                        Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to open overlay permission settings", e);
                }
            } else {
                Log.d("MainActivity", "Overlay permission already granted");
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted");
                Toast.makeText(this, "Thông báo đã được bật", Toast.LENGTH_SHORT).show();
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied");
                Toast.makeText(this, "Vui lòng cấp quyền thông báo để nhận tin nhắn", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Setup friend requests badge - load initial count and observe changes
     */
    private void setupFriendRequestsBadge() {
        friendRepository = new com.example.doan_zaloclone.repository.FriendRepository();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        
        // Load initial friend requests count
        loadFriendRequestsCount(userId);
        
        // Observe WebSocket events for real-time badge updates
        friendRepository.getFriendRequestRefreshNeeded().observe(this, needsRefresh -> {
            if (needsRefresh != null && needsRefresh) {
                loadFriendRequestsCount(userId);
            }
        });
    }
    
    /**
     * Load friend requests count and update badge
     */
    private void loadFriendRequestsCount(String userId) {
        friendRepository.getFriendRequests(userId).observe(this, resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                int count = resource.getData().size();
                updateContactBadge(count);
            }
        });
    }

    /**
     * Setup WebSocket listener for friend events
     * This persists across fragment changes
     */
    private void setupFriendEventListener() {
        com.example.doan_zaloclone.websocket.SocketManager socketManager =
                com.example.doan_zaloclone.websocket.SocketManager.getInstance();

        // Ensure socket is connected
        if (!socketManager.isConnected()) {
            socketManager.connect();
        }

        socketManager.setFriendEventListener(new com.example.doan_zaloclone.websocket.SocketManager.OnFriendEventListener() {
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, senderName + " sent you a friend request!", Toast.LENGTH_SHORT).show();
                    // Update badge immediately
                    refreshFriendRequestsBadge();
                    // Notify fragment to reload friend requests if active
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("REQUEST_RECEIVED", senderId);
                    }
                });
            }

            @Override
            public void onFriendRequestAccepted(String userId) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                    // Update badge immediately
                    refreshFriendRequestsBadge();
                    // Notify fragment to reload if active
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("ACCEPTED", userId);
                    }
                });
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Friend request was rejected", Toast.LENGTH_SHORT).show();
                    // Update badge immediately
                    refreshFriendRequestsBadge();
                    // Notify fragment to update search results
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("REJECTED", userId);
                    }
                });
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                runOnUiThread(() -> {
                    // Update badge immediately
                    refreshFriendRequestsBadge();
                    // Notify fragment if active
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("CANCELLED", senderId);
                    }
                });
            }

            @Override
            public void onFriendAdded(String userId) {
                runOnUiThread(() -> {
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("ADDED", userId);
                    }
                });
            }

            @Override
            public void onFriendRemoved(String userId) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Friend removed", Toast.LENGTH_SHORT).show();
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("REMOVED", userId);
                    }
                });
            }
            
            @Override
            public void onFriendStatusChanged(String friendId, boolean isOnline) {
                // Online status updates are handled in ContactFragment directly
            }
        });
    }

    /**
     * TEMPORARY: Add floating API test button
     * Remove this method after API testing is complete!
     */
    private void addApiTestButton() {
        // Note: Button always shows. Remove entire method after testing!

        android.widget.Button btnApiTest = new android.widget.Button(this);
        btnApiTest.setText("🔧 API Test");
        btnApiTest.setBackgroundColor(0xFF4CAF50);
        btnApiTest.setTextColor(0xFFFFFFFF);
        btnApiTest.setPadding(32, 16, 32, 16);
        btnApiTest.setTextSize(12);
        btnApiTest.setAllCaps(false);

        btnApiTest.setOnClickListener(v -> {
            Intent intent = new Intent(this, ApiTestActivity.class);
            startActivity(intent);
        });

        // Add to root layout
        android.view.ViewGroup rootLayout = findViewById(android.R.id.content);
        if (rootLayout instanceof android.widget.FrameLayout) {
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            params.setMargins(0, 200, 32, 0);
            btnApiTest.setLayoutParams(params);
            rootLayout.addView(btnApiTest);
        }
    }

    /**
     * Listen for force logout or ban status changes
     */
    private void setupForceLogoutListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        forceLogoutListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    // Check if user is banned
                    Boolean isBanned = snapshot.getBoolean("isBanned");
                    if (isBanned != null && isBanned) {
                        performForceLogout("Tài khoản của bạn đã bị khóa.");
                        return;
                    }

                    // Check for force logout timestamp
                    Long forceLogoutAt = snapshot.getLong("forceLogoutAt");
                    if (forceLogoutAt != null && forceLogoutAt > loginTimestamp) {
                        performForceLogout("Bạn đã bị đăng xuất bởi quản trị viên.");
                    }
                });
    }

    /**
     * Force logout and redirect to login screen
     */
    private void performForceLogout(String message) {
        // Remove listener first to prevent loop
        if (forceLogoutListener != null) {
            forceLogoutListener.remove();
            forceLogoutListener = null;
        }
        if (incomingCallListener != null) {
            incomingCallListener.remove();
            incomingCallListener = null;
        }

        // Sign out
        FirebaseAuth.getInstance().signOut();
        
        // Stop NotificationService
        stopService(new Intent(this, com.example.doan_zaloclone.services.NotificationService.class));

        // Show message and redirect
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupIncomingCallListener() {
        android.util.Log.d("MainActivity", "setupIncomingCallListener() called");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            android.util.Log.e("MainActivity", "Cannot setup listener - user not logged in");
            return;
        }

        android.util.Log.d("MainActivity", "Setting up listener for user: " + currentUser.getUid());

        callRepository = new CallRepository();
        incomingCallListener = callRepository.listenToIncomingCalls(
                currentUser.getUid(),
                new CallRepository.OnIncomingCallListener() {
                    @Override
                    public void onIncomingCall(Call call) {
                        android.util.Log.d("MainActivity", "onIncomingCall triggered for call: " + call.getId());

                        // Prevent duplicate launches for the same call
                        if (call.getId().equals(lastHandledCallId)) {
                            android.util.Log.d("MainActivity", "Skipping duplicate call: " + call.getId());
                            return;
                        }

                        // Reject calls older than 60 seconds
                        if (call.getStartTime() > 0) {
                            long callAge = System.currentTimeMillis() - call.getStartTime();
                            if (callAge > 60000) { // 60 seconds
                                android.util.Log.w("MainActivity", "Rejecting old call: " + call.getId() + ", age: " + callAge + "ms");
                                return;
                            }
                        }

                        lastHandledCallId = call.getId();
                        android.util.Log.d("MainActivity", "Processing new incoming call: " + call.getId());

                        // Fetch caller name before launching CallActivity
                        fetchCallerNameAndLaunchCallActivity(call, currentUser.getUid());
                    }

                    @Override
                    public void onError(String error) {
                        // Log error but don't show to user
                        android.util.Log.e("MainActivity", "Error listening for calls: " + error);
                    }
                }
        );

        android.util.Log.d("MainActivity", "Incoming call listener setup complete");
    }

    /**
     * Fetch caller name from Firestore and launch CallActivity
     * Fixes: Bug #3 (missing caller name), Bug #1 (missing receiver_id), Bug #2 (wrong isVideo)
     */
    private void fetchCallerNameAndLaunchCallActivity(Call call, String currentUserId) {
        // Fetch caller name from Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(call.getCallerId())
                .get()
                .addOnSuccessListener(doc -> {
                    String callerName = "Unknown";
                    if (doc.exists() && doc.contains("name")) {
                        callerName = doc.getString("name");
                    }

                    launchIncomingCallActivity(call, currentUserId, callerName);
                })
                .addOnFailureListener(e -> {
                    // Launch anyway with default name
                    android.util.Log.e("MainActivity", "Failed to fetch caller name", e);
                    launchIncomingCallActivity(call, currentUserId, "Unknown");
                });
    }

    /**
     * Launch CallActivity for incoming call with all required extras
     */
    private void launchIncomingCallActivity(Call call, String currentUserId, String callerName) {
        Intent intent = new Intent(MainActivity.this, CallActivity.class);

        // Fix #1: Add call_id and receiver_id
        intent.putExtra(CallActivity.EXTRA_CALL_ID, call.getId());
        intent.putExtra(CallActivity.EXTRA_RECEIVER_ID, currentUserId);

        // Fix #2: Convert call type to boolean isVideo
        boolean isVideo = Call.TYPE_VIDEO.equals(call.getType());
        intent.putExtra(CallActivity.EXTRA_IS_VIDEO, isVideo);

        // FIX: Add conversationId so receiver can log call history
        intent.putExtra(CallActivity.EXTRA_CONVERSATION_ID, call.getConversationId());

        // Other required extras
        intent.putExtra(CallActivity.EXTRA_IS_INCOMING, true);
        intent.putExtra(CallActivity.EXTRA_CALLER_ID, call.getCallerId());

        // Fix #3: Add caller name
        intent.putExtra(CallActivity.EXTRA_CALLER_NAME, callerName);

        // Fix #4: Use SINGLE_TOP to prevent duplicate activities
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);

        android.util.Log.d("MainActivity",
                "Launched CallActivity for incoming " + (isVideo ? "video" : "voice") +
                        " call from " + callerName);
    }

    private void observeViewModel() {
        // Observe logout state
        mainViewModel.getLogoutState().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                // Logout successful, navigate to login
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("FROM_LOGOUT", true);
                startActivity(intent);
                finish();
            } else if (resource.isError()) {
                // Show error message
                Toast.makeText(this,
                        "Logout failed: " + resource.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleLogout() {
        // Trigger logout operation via ViewModel
        mainViewModel.logout();
    }

    private void initCustomBottomNav() {
        navMessages = findViewById(R.id.nav_messages);
        navContact = findViewById(R.id.nav_contact);
        navGrid = findViewById(R.id.nav_grid);
        navTimeline = findViewById(R.id.nav_timeline);
        navPersonal = findViewById(R.id.nav_personal);

        iconMessages = findViewById(R.id.icon_messages);
        iconContact = findViewById(R.id.icon_contact);
        iconGrid = findViewById(R.id.icon_grid);
        iconTimeline = findViewById(R.id.icon_timeline);
        iconPersonal = findViewById(R.id.icon_personal);

        textMessages = findViewById(R.id.text_messages);
        textContact = findViewById(R.id.text_contact);
        textGrid = findViewById(R.id.text_grid);
        textTimeline = findViewById(R.id.text_timeline);
        textPersonal = findViewById(R.id.text_personal);

        navMessages.setOnClickListener(v -> showFragment(0));
        navContact.setOnClickListener(v -> showFragment(1));
        navGrid.setOnClickListener(v -> showFragment(2));
        navTimeline.setOnClickListener(v -> showFragment(3));
        navPersonal.setOnClickListener(v -> showFragment(4));
        
        // Get badge view reference
        badgeMessages = findViewById(R.id.badge_messages);
        badgeContact = findViewById(R.id.badge_contact);
    }
    
    /**
     * Update messages badge with total unread count
     * Called from HomeFragment when conversations are loaded
     */
    public void updateMessagesBadge(int totalUnread) {
        if (badgeMessages != null) {
            if (totalUnread > 0) {
                badgeMessages.setVisibility(View.VISIBLE);
                if (totalUnread > 5) {
                    badgeMessages.setText("5+");
                } else {
                    badgeMessages.setText(String.valueOf(totalUnread));
                }
            } else {
                badgeMessages.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Update contact badge with new friend requests count
     * Called from ContactFragment
     */
    public void updateContactBadge(int count) {
        if (badgeContact != null) {
            if (count > 0) {
                badgeContact.setVisibility(View.VISIBLE);
                if (count > 5) {
                    badgeContact.setText("5+");
                } else {
                    badgeContact.setText(String.valueOf(count));
                }
            } else {
                badgeContact.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Refresh friend requests badge - reload count from server
     * Called when WebSocket events occur
     */
    private void refreshFriendRequestsBadge() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && friendRepository != null) {
            loadFriendRequestsCount(currentUser.getUid());
        }
    }

    private void updateBottomNavUI(int position) {
        // Reset all to inactive
        int inactiveColor = Color.parseColor("#757575");
        int activeColor = Color.parseColor("#0068FF");

        // Messages
        iconMessages.setColorFilter(inactiveColor);
        textMessages.setVisibility(View.GONE);

        // Contact
        iconContact.setColorFilter(inactiveColor);
        textContact.setVisibility(View.GONE);

        // Grid
        iconGrid.setColorFilter(inactiveColor);
        textGrid.setVisibility(View.GONE);

        // Timeline
        iconTimeline.setColorFilter(inactiveColor);
        textTimeline.setVisibility(View.GONE);

        // Personal
        iconPersonal.setColorFilter(inactiveColor);
        textPersonal.setVisibility(View.GONE);

        // Set active
        switch (position) {
            case 0:
                iconMessages.setColorFilter(activeColor);
                textMessages.setVisibility(View.VISIBLE);
                break;
            case 1:
                iconContact.setColorFilter(activeColor);
                textContact.setVisibility(View.VISIBLE);
                break;
            case 2:
                iconGrid.setColorFilter(activeColor);
                textGrid.setVisibility(View.VISIBLE);
                break;
            case 3:
                iconTimeline.setColorFilter(activeColor);
                textTimeline.setVisibility(View.VISIBLE);
                break;
            case 4:
                iconPersonal.setColorFilter(activeColor);
                textPersonal.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Disable default animations to prevent flicker
        transaction.setCustomAnimations(0, 0);

        // Hide all fragments first
        if (homeFragment != null) transaction.hide(homeFragment);
        if (contactFragment != null) transaction.hide(contactFragment);
        if (personalFragment != null) transaction.hide(personalFragment);
        
        // Update UI
        updateBottomNavUI(position);

        // Show or create the selected fragment
        switch (position) {
            case 0: // Home / Messages
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                    transaction.add(R.id.fragmentContainer, homeFragment, "HOME");
                } else {
                    transaction.show(homeFragment);
                }
                break;
            case 1: // Contact
                if (contactFragment == null) {
                    contactFragment = new ContactFragment();
                    transaction.add(R.id.fragmentContainer, contactFragment, "CONTACT");
                } else {
                    transaction.show(contactFragment);
                }
                break;
            case 2: // Grid (Discovery) - Placeholder
                // For now, reuse Home or show Toast
                 Toast.makeText(this, "Discovery Coming Soon", Toast.LENGTH_SHORT).show();
                 // To prevent empty screen, maybe show home? Or just nothing.
                 // Ideally create a placeholder fragment.
                break;
            case 3: // Timeline - Placeholder
                 Toast.makeText(this, "Timeline Coming Soon", Toast.LENGTH_SHORT).show();
                break;
            case 4: // Personal
                if (personalFragment == null) {
                    personalFragment = new PersonalFragment();
                    transaction.add(R.id.fragmentContainer, personalFragment, "PERSONAL");
                } else {
                    transaction.show(personalFragment);
                }
                break;
        }

        transaction.commit();
    }
    
    /**
     * Setup notification listener for WebSocket events
     * Shows notifications for messages and friend requests
     */
    private void setupNotificationListener() {
        com.example.doan_zaloclone.websocket.SocketManager socketManager =
                com.example.doan_zaloclone.websocket.SocketManager.getInstance();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        final String currentUserId = currentUser.getUid();
        
        socketManager.setNotificationListener(new com.example.doan_zaloclone.websocket.SocketManager.OnNotificationListener() {
            @Override
            public void onNewMessage(org.json.JSONObject messageData) {
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
                        Log.d("MainActivity", "Skipping notification - user is in this conversation");
                        return;
                    }
                    
                    // Format message text based on type
                    String displayText = messageText;
                    if ("image".equals(messageType)) {
                        displayText = "\ud83d\uddbc\ufe0f Hình ảnh";
                    } else if ("voice".equals(messageType)) {
                        displayText = "\ud83c\udfa4 Tin nhắn thoại";
                    } else if ("file".equals(messageType)) {
                        displayText = "\ud83d\udcce Tệp tin";
                    } else if ("sticker".equals(messageType)) {
                        displayText = "\ud83d\ude00 Nhãn dán";
                    } else if ("location".equals(messageType)) {
                        displayText = "\ud83d\udccd Vị trí";
                    } else if ("card".equals(messageType)) {
                        displayText = "\ud83d\udcbc Danh thiếp";
                    }
                    
                    // Show notification
                    NotificationHelper.showMessageNotification(
                            MainActivity.this, 
                            senderName, 
                            displayText, 
                            conversationId, 
                            null  // Avatar will be loaded async in future
                    );
                    
                } catch (Exception e) {
                    Log.e("MainActivity", "Error showing message notification", e);
                }
            }
            
            @Override
            public void onMessageRecalled(org.json.JSONObject messageData) {
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
                    
                    // Show recall notification
                    NotificationHelper.showMessageRecallNotification(
                            MainActivity.this,
                            senderName,
                            conversationId
                    );
                    
                } catch (Exception e) {
                    Log.e("MainActivity", "Error showing recall notification", e);
                }
            }
            
            @Override
            public void onMessageReaction(org.json.JSONObject reactionData) {
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
                                        MainActivity.this,
                                        userName,
                                        reactionType,
                                        conversationId
                                );
                            });
                    
                } catch (Exception e) {
                    Log.e("MainActivity", "Error showing reaction notification", e);
                }
            }
            
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                // Show friend request notification
                NotificationHelper.showFriendRequestNotification(
                        MainActivity.this,
                        senderName,
                        senderId,
                        null  // Avatar will be loaded async in future
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
                                    MainActivity.this,
                                    userName,
                                    userId
                            );
                        });
            }
        });
    }
    
    /**
     * Set the active conversation ID (called from RoomActivity)
     * Used to prevent notifications when user is viewing the conversation
     */
    public static void setActiveConversationId(String conversationId) {
        activeConversationId = conversationId;
    }
    
    /**
     * Clear the active conversation ID (called when RoomActivity is destroyed)
     */
    public static void clearActiveConversationId() {
        activeConversationId = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset the flag when returning to MainActivity
        // This allows detecting new incoming calls
        lastHandledCallId = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Disconnect WebSocket when activity is destroyed
        // Note: This won't be called if app is force-killed, but server will detect disconnect anyway
        if (isFinishing()) {
            // Only disconnect if activity is truly finishing (not just rotating)
            com.example.doan_zaloclone.websocket.SocketManager.getInstance().disconnect();
        }
        
        // Clean up incoming call listener
        if (incomingCallListener != null) {
            incomingCallListener.remove();
        }
        // Clean up force logout listener
        if (forceLogoutListener != null) {
            forceLogoutListener.remove();
        }
        
        // Stop NotificationService when app is finishing
        if (isFinishing()) {
            stopService(new Intent(this, com.example.doan_zaloclone.services.NotificationService.class));
        }
    }
}