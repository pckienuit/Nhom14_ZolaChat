package com.example.doan_zaloclone;

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
import com.example.doan_zaloclone.ui.personal.PersonalFragment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_main);

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

        // TEMPORARY: API Test Button - Only in DEBUG builds
        // addApiTestButton();
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
                    android.util.Log.d("MainActivity", "New friend request from: " + senderName);
                    Toast.makeText(MainActivity.this, senderName + " sent you a friend request!", Toast.LENGTH_SHORT).show();
                    // Notify fragment to reload friend requests if active
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("REQUEST_RECEIVED", senderId);
                    }
                });
            }

            @Override
            public void onFriendRequestAccepted(String userId) {
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "Friend request accepted by: " + userId);
                    Toast.makeText(MainActivity.this, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                    // Notify fragment to reload if active
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("ACCEPTED", userId);
                    }
                });
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "Friend request rejected by: " + userId);
                    Toast.makeText(MainActivity.this, "Friend request was rejected", Toast.LENGTH_SHORT).show();
                    // Notify fragment to update search results
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("REJECTED", userId);
                    }
                });
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "Friend request cancelled by sender: " + senderId);
                    // Notify fragment if active
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("CANCELLED", senderId);
                    }
                });
            }

            @Override
            public void onFriendAdded(String userId) {
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "New friend added: " + userId);
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("ADDED", userId);
                    }
                });
            }

            @Override
            public void onFriendRemoved(String userId) {
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "Friend removed: " + userId);
                    Toast.makeText(MainActivity.this, "Friend removed", Toast.LENGTH_SHORT).show();
                    if (contactFragment != null && contactFragment.isAdded()) {
                        contactFragment.onFriendEventReceived("REMOVED", userId);
                    }
                });
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
        // Clean up incoming call listener
        if (incomingCallListener != null) {
            incomingCallListener.remove();
        }
        // Clean up force logout listener
        if (forceLogoutListener != null) {
            forceLogoutListener.remove();
        }
    }
}