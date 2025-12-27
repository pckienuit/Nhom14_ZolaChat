package com.example.doan_zaloclone;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
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
import com.example.doan_zaloclone.viewmodel.MainViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        if (savedInstanceState == null) {
            // Show home fragment on first launch
            showFragment(0);
        }

        setupBottomNavigation();
        observeViewModel();
        
        // Start listening for incoming calls
        setupIncomingCallListener();
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

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(0);
                return true;
            } else if (itemId == R.id.nav_contact) {
                showFragment(1);
                return true;
            } else if (itemId == R.id.nav_newsfeed) {
                showFragment(2);
                return true;
            } else if (itemId == R.id.nav_personal) {
                showFragment(3);
                return true;
            }
            return false;
        });
    }

    private void showFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // Disable default animations to prevent flicker
        transaction.setCustomAnimations(0, 0);
        
        // Hide all fragments first
        if (homeFragment != null) {
            transaction.hide(homeFragment);
        }
        if (contactFragment != null) {
            transaction.hide(contactFragment);
        }
        if (newsfeedFragment != null) {
            transaction.hide(newsfeedFragment);
        }
        if (personalFragment != null) {
            transaction.hide(personalFragment);
        }
        
        // Show or create the selected fragment
        switch (position) {
            case 0: // Home
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
            case 2: // Newsfeed
                if (newsfeedFragment == null) {
                    newsfeedFragment = new NewsfeedFragment();
                    transaction.add(R.id.fragmentContainer, newsfeedFragment, "NEWSFEED");
                } else {
                    transaction.show(newsfeedFragment);
                }
                break;
            case 3: // Personal
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
    }
}