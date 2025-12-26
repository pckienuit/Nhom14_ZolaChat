package com.example.doan_zaloclone.ui.contact;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ContactFragment extends Fragment implements UserAdapter.OnUserActionListener, FriendsAdapter.OnFriendClickListener {

    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;

    private ContactViewModel contactViewModel;
    private FirebaseAuth firebaseAuth;
    private FirestoreManager firestoreManager; // Still needed for some legacy operations
    private SocketManager socketManager;
    
    // WebSocket listener for friend events
    private SocketManager.OnFriendEventListener friendEventListener;
    
    // Real-time counts views
    private TextView tvFriendRequestCount;
    private TextView chipAllFriends;
    private TextView chipRecentlyActive;
    private View dotFriendRequestNew;
    
    // Track last seen request count for notification dot
    private int lastSeenRequestCount = -1;
    
    // Filter state: true = show online only, false = show all
    private boolean isOnlineFilterActive = false;
    
    // Cache all friends list for filtering
    private List<User> allFriendsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d("ContactFragment", "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        // Initialize ViewModel
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        firebaseAuth = FirebaseAuth.getInstance();
        firestoreManager = FirestoreManager.getInstance(); // For legacy operations
        socketManager = SocketManager.getInstance();

        initViews(view);
        setupRecyclerViews();
        // setupSearchListener(); // Removed in redesign
        observeViewModel();
        // loadFriendRequests(); // Removed, handled by header row now (todo)
        
        // Setup WebSocket listener for friend events
        setupFriendEventListener();

        return view;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove WebSocket listener to prevent memory leaks
        if (friendEventListener != null) {
            socketManager.removeFriendEventListener(friendEventListener);
            friendEventListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ContactFragment", "onResume called - loading friends and updating badge");
        // Load friends when fragment becomes visible (critical for show/hide pattern)
        loadFriends();
        // Also refresh friend request count to update badge
        loadFriendRequestsCount();
    }
    
    /**
     * Setup WebSocket listener for friend events (added/removed)
     * This ensures friends list updates in real-time
     */
    private long lastEventTime = 0;
    private static final long DEBOUNCE_MS = 1000; // Debounce 1 second
    
    private void setupFriendEventListener() {
        friendEventListener = new SocketManager.OnFriendEventListener() {
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                // Reload friend request count when receiving new request
                Log.d("ContactFragment", "Friend request received from: " + senderName);
                if (getActivity() != null && shouldProcessEvent()) {
                    getActivity().runOnUiThread(() -> {
                        loadFriendRequestsCount();
                    });
                }
            }

            @Override
            public void onFriendRequestAccepted(String userId) {
                // When someone accepts our request, we become friends - reload list
                Log.d("ContactFragment", "Friend request accepted by: " + userId + ", reloading friends");
                if (getActivity() != null && shouldProcessEvent()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Đã trở thành bạn bè!", Toast.LENGTH_SHORT).show();
                        loadFriends();
                        loadFriendRequestsCount();
                    });
                }
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                // Reload friend request count
                if (getActivity() != null && shouldProcessEvent()) {
                    getActivity().runOnUiThread(() -> loadFriendRequestsCount());
                }
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                // Reload friend request count
                if (getActivity() != null && shouldProcessEvent()) {
                    getActivity().runOnUiThread(() -> loadFriendRequestsCount());
                }
            }

            @Override
            public void onFriendAdded(String userId) {
                // New friend added - reload list
                Log.d("ContactFragment", "Friend added: " + userId + ", reloading friends");
                if (getActivity() != null && shouldProcessEvent()) {
                    getActivity().runOnUiThread(() -> {
                        loadFriends();
                        loadFriendRequestsCount();
                    });
                }
            }

            @Override
            public void onFriendRemoved(String userId) {
                // Friend removed - reload list
                Log.d("ContactFragment", "Friend removed: " + userId + ", reloading friends");
                if (getActivity() != null && shouldProcessEvent()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Đã xóa bạn bè", Toast.LENGTH_SHORT).show();
                        loadFriends();
                    });
                }
            }
            
            @Override
            public void onFriendStatusChanged(String friendId, boolean isOnline) {
                // Friend online/offline status changed - update UI
                Log.d("ContactFragment", "Friend " + friendId + " status changed: " + (isOnline ? "online" : "offline"));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateFriendOnlineStatus(friendId, isOnline);
                    });
                }
            }
        };
        
        socketManager.addFriendEventListener(friendEventListener);
    }
    
    /**
     * Debounce helper to prevent duplicate event processing
     */
    private boolean shouldProcessEvent() {
        long now = System.currentTimeMillis();
        if (now - lastEventTime < DEBOUNCE_MS) {
            return false;
        }
        lastEventTime = now;
        return true;
    }

    private void initViews(View view) {
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        
        // Setup inline search EditText for realtime filtering
        setupSearchEditText(view);
        
        // Find and set click listener for Friend Invitations row
        View friendRequestsRow = view.findViewById(R.id.clickable_friend_requests);
        if (friendRequestsRow != null) {
            friendRequestsRow.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.doan_zaloclone.ui.contact.FriendRequestActivity.class);
                startActivity(intent);
            });
        }
        
        // Birthdays row - open BirthdayListActivity
        View birthdaysRow = view.findViewById(R.id.clickable_birthdays);
        if (birthdaysRow != null) {
            birthdaysRow.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.doan_zaloclone.ui.contact.BirthdayListActivity.class);
                startActivity(intent);
            });
        }

        // Add Friend Button
        View btnAddFriend = view.findViewById(R.id.btn_add_friend);
        if (btnAddFriend != null) {
            btnAddFriend.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.doan_zaloclone.ui.contact.AddFriendActivity.class);
                startActivity(intent);
            });
        }
        
        // Real-time count views
        tvFriendRequestCount = view.findViewById(R.id.tvFriendRequestCount);
        chipAllFriends = view.findViewById(R.id.chipAllFriends);
        chipRecentlyActive = view.findViewById(R.id.chipRecentlyActive);
        dotFriendRequestNew = view.findViewById(R.id.dotFriendRequestNew);
        
        // Setup chip filter click listeners
        setupChipFilters();
        
        // Load friend requests count
        loadFriendRequestsCount();
    }
    
    /**
     * Setup chip filter click listeners for "Tất cả" and "Trực tuyến"
     */
    private void setupChipFilters() {
        if (chipAllFriends != null) {
            chipAllFriends.setOnClickListener(v -> {
                if (isOnlineFilterActive) {
                    isOnlineFilterActive = false;
                    updateChipStyles();
                    applyFilter();
                }
            });
        }
        
        if (chipRecentlyActive != null) {
            chipRecentlyActive.setOnClickListener(v -> {
                if (!isOnlineFilterActive) {
                    isOnlineFilterActive = true;
                    updateChipStyles();
                    applyFilter();
                }
            });
        }
    }
    
    /**
     * Update chip visual styles based on filter state
     */
    private void updateChipStyles() {
        if (chipAllFriends != null) {
            if (!isOnlineFilterActive) {
                chipAllFriends.setBackgroundResource(R.drawable.bg_chip_selected);
                chipAllFriends.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                chipAllFriends.setBackgroundResource(R.drawable.bg_chip_unselected);
                chipAllFriends.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
        
        if (chipRecentlyActive != null) {
            if (isOnlineFilterActive) {
                chipRecentlyActive.setBackgroundResource(R.drawable.bg_chip_selected);
                chipRecentlyActive.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                chipRecentlyActive.setBackgroundResource(R.drawable.bg_chip_unselected);
                chipRecentlyActive.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }
    
    /**
     * Apply the current filter to the friends list
     */
    private void applyFilter() {
        if (friendsAdapter == null) return;
        
        if (isOnlineFilterActive) {
            // Filter to show only online friends
            List<User> onlineFriends = new ArrayList<>();
            for (User friend : allFriendsList) {
                if (friend.isOnline()) {
                    onlineFriends.add(friend);
                }
            }
            friendsAdapter.updateFriends(onlineFriends);
            Log.d("ContactFragment", "Filtered to " + onlineFriends.size() + " online friends");
        } else {
            // Show all friends
            friendsAdapter.updateFriends(allFriendsList);
            Log.d("ContactFragment", "Showing all " + allFriendsList.size() + " friends");
        }
    }
    
    private void setupSearchEditText(View view) {
        android.widget.EditText searchEditText = view.findViewById(R.id.searchEditText);
        if (searchEditText == null) return;
        
        // TextWatcher for realtime filtering
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void filterFriends(String query) {
        if (friendsAdapter == null) return;
        
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        
        // Get all friends from ViewModel
        contactViewModel.getFriends(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                java.util.List<com.example.doan_zaloclone.models.User> allFriends = resource.getData();
                
                if (query == null || query.trim().isEmpty()) {
                    // Show all if search is empty
                    friendsAdapter.updateFriends(allFriends);
                } else {
                    // Filter by friend name
                    String lowerQuery = query.toLowerCase();
                    java.util.List<com.example.doan_zaloclone.models.User> filtered = new java.util.ArrayList<>();
                    
                    for (com.example.doan_zaloclone.models.User friend : allFriends) {
                        if (friend.getName() != null && 
                            friend.getName().toLowerCase().contains(lowerQuery)) {
                            filtered.add(friend);
                        }
                    }
                    
                    friendsAdapter.updateFriends(filtered);
                }
            }
        });
    }

    private void setupRecyclerViews() {
        // Setup friends RecyclerView
        friendsAdapter = new FriendsAdapter(new ArrayList<>(), this);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsRecyclerView.setAdapter(friendsAdapter);

        // Search and Friend Request RecyclerViews are removed from this layout
    }

    private void observeViewModel() {
        // Search results logic commented out as UI elements are removed
        /*
        contactViewModel.getSearchResults().observe(getViewLifecycleOwner(), resource -> {
            ...
        });
        */

        // Observe friends list
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";
        contactViewModel.getFriends(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                List<User> friends = resource.getData();
                if (friends != null) {
                    Log.d("ContactFragment", "Friends loaded: " + friends.size());
                    friendsAdapter.updateFriends(friends);
                    friendsRecyclerView.requestLayout();
                }
            } else if (resource.isError()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading friends: " + resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Friend requests logic commented out as UI elements are removed
        /*
        contactViewModel.getFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            ...
        });
        */
    }

    /*
    private void setupSearchListener() {
        ...
    }

    private void searchUsers(String query) {
        ...
    }

    private void checkFriendRequestStatus(String fromUserId, String toUserId) {
        ...
    }
    */

    @Override
    public void onUserClick(User user) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Opening chat with " + user.getName() + "...", Toast.LENGTH_SHORT).show();

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Use ViewModel to find existing conversation
        contactViewModel.findExistingConversation(currentUserId, user.getId()).observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                Conversation conversation = resource.getData();
                if (conversation != null) {
                    // Found existing conversation
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Opening existing chat", Toast.LENGTH_SHORT).show();
                        openChatRoom(conversation);
                    }
                } else {
                    // No conversation found, create new one
                    String currentUserName = firebaseAuth.getCurrentUser().getDisplayName() != null
                            ? firebaseAuth.getCurrentUser().getDisplayName()
                            : "User";
                    createNewConversation(currentUserId, currentUserName, user);
                }
            } else if (resource.isError()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onAddFriendClick(User user) {
        // TODO: Re-implement add friend logic with new UI
        /*
        if (firebaseAuth.getCurrentUser() == null) return;
        ...
        */
        Toast.makeText(getContext(), "Add Friend clicked (Not implemented in new UI)", Toast.LENGTH_SHORT).show();
    }

    private void acceptFriendRequest(FriendRequest request) {
        contactViewModel.acceptFriendRequest(request).observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Accepted friend request from " + request.getFromUserName(),
                            Toast.LENGTH_SHORT).show();
                    // Reload both lists after accepting
                    reloadFriendData();
                }
            } else if (resource.isError()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error accepting request: " + resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void rejectFriendRequest(FriendRequest request) {
        contactViewModel.rejectFriendRequest(request).observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Rejected friend request from " + request.getFromUserName(),
                            Toast.LENGTH_SHORT).show();
                    // Reload friend requests after rejecting
                    reloadFriendData();
                }
            } else if (resource.isError()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error rejecting request: " + resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createNewConversation(String currentUserId, String currentUserName, User otherUser) {
        if (getActivity() == null) return;

        firestoreManager.createConversation(
                currentUserId,
                currentUserName,
                otherUser.getId(),
                otherUser.getName(),
                new FirestoreManager.OnConversationCreatedListener() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        if (getActivity() != null) {
                            Toast.makeText(getContext(), "New chat created!", Toast.LENGTH_SHORT).show();
                            openChatRoom(conversation);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getActivity() != null) {
                            Toast.makeText(getContext(), "Error creating chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openChatRoom(Conversation conversation) {
        if (firebaseAuth.getCurrentUser() == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Intent intent = new Intent(getActivity(), RoomActivity.class);
        intent.putExtra("conversationId", conversation.getId());

        // Get proper conversation name
        String conversationName = conversation.getName();
        if (conversationName == null || conversationName.isEmpty()) {
            conversationName = conversation.getOtherUserName(currentUserId);
        }
        intent.putExtra("conversationName", conversationName);
        startActivity(intent);

        // Clear search
        // searchEmailEditText.setText(""); // View removed
        contactViewModel.clearSearchResults();
    }

    private void loadFriends() {
        if (firebaseAuth.getCurrentUser() == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d("ContactFragment", "Loading friends for user: " + currentUserId);

        // Force reload friends
        contactViewModel.loadFriends(currentUserId);

        // Re-observe the new LiveData
        contactViewModel.getFriendsLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                List<User> friends = resource.getData();
                if (friends != null) {
                    Log.d("ContactFragment", "Friends reloaded: " + friends.size());
                    
                    // Cache all friends for filtering
                    allFriendsList = new ArrayList<>(friends);
                    
                    // Apply current filter
                    applyFilter();
                    
                    // Update real-time counts
                    updateFriendCounts(friends);
                }
            } else if (resource.isError()) {
                Log.e("ContactFragment", "Error loading friends: " + resource.getMessage());
            }
        });
    }
    
    /**
     * Load and display friend request count
     */
    private void loadFriendRequestsCount() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d("ContactFragment", "Loading friend requests for user: " + currentUserId);
        
        contactViewModel.getFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                Log.d("ContactFragment", "Friend requests resource is null");
                return;
            }
            
            if (resource.isLoading()) {
                Log.d("ContactFragment", "Loading friend requests...");
            } else if (resource.isSuccess() && resource.getData() != null) {
                int count = resource.getData().size();
                Log.d("ContactFragment", "Friend requests count: " + count);
                updateFriendRequestCount(count);
            } else if (resource.isError()) {
                Log.e("ContactFragment", "Error loading friend requests: " + resource.getMessage());
            }
        });
    }
    
    /**
     * Update the friend request count display
     */
    private void updateFriendRequestCount(int count) {
        if (tvFriendRequestCount != null) {
            if (count > 0) {
                tvFriendRequestCount.setText("Lời mời kết bạn (" + count + ")");
            } else {
                tvFriendRequestCount.setText("Lời mời kết bạn");
            }
        }
        
        // Show red dot if new requests since last seen
        if (dotFriendRequestNew != null) {
            if (lastSeenRequestCount >= 0 && count > lastSeenRequestCount) {
                dotFriendRequestNew.setVisibility(View.VISIBLE);
            } else {
                dotFriendRequestNew.setVisibility(View.GONE);
            }
        }
        
        // Update badge in MainActivity
        if (getActivity() instanceof com.example.doan_zaloclone.MainActivity) {
            ((com.example.doan_zaloclone.MainActivity) getActivity()).updateContactBadge(count);
        }
        
        // Update last seen count when not in friend requests screen
        if (lastSeenRequestCount < 0) {
            lastSeenRequestCount = count;
        }
    }
    
    /**
     * Update friend counts (total and online)
     */
    private void updateFriendCounts(List<User> friends) {
        int totalFriends = friends.size();
        long onlineFriends = friends.stream()
                .filter(user -> user.isOnline())
                .count();
        
        // Debug log to check online status
        Log.d("ContactFragment", "Total friends: " + totalFriends + ", Online: " + onlineFriends);
        for (User friend : friends) {
            Log.d("ContactFragment", "Friend " + friend.getName() + " isOnline: " + friend.isOnline());
        }
        
        if (chipAllFriends != null) {
            chipAllFriends.setText("Tất cả " + totalFriends);
        }
        
        if (chipRecentlyActive != null) {
            chipRecentlyActive.setText("Trực tuyến " + onlineFriends);
        }
    }
    
    /**
     * Update a friend's online status in real-time (called from WebSocket event)
     */
    private void updateFriendOnlineStatus(String friendId, boolean isOnline) {
        if (allFriendsList == null || allFriendsList.isEmpty()) {
            Log.d("ContactFragment", "allFriendsList is empty, cannot update status");
            return;
        }
        
        boolean found = false;
        for (User friend : allFriendsList) {
            if (friend.getId() != null && friend.getId().equals(friendId)) {
                friend.setOnline(isOnline);
                found = true;
                Log.d("ContactFragment", "Updated " + friend.getName() + " isOnline to " + isOnline);
                break;
            }
        }
        
        if (found) {
            // Update chip counts
            updateFriendCounts(allFriendsList);
            
            // If filter is active, re-apply it
            if (isOnlineFilterActive) {
                applyFilter();
            } else {
                // Notify adapter of change
                if (friendsAdapter != null) {
                    friendsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void reloadFriendData() {
        if (firebaseAuth.getCurrentUser() == null) return;

        // Reload friend requests (logic commented out for UI)
        /*
        contactViewModel.getFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
             ...
        });
        */

        // Reload friends list
        loadFriends();
    }

    @Override
    public void onMessageClick(User friend) {
        onUserClick(friend);
    }

    @Override
    public void onFriendLongClick(View view, User friend) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), view);
        popup.getMenu().add("Xóa bạn bè");
        popup.getMenu().add("Chặn người này");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Xóa bạn bè")) {
                showUnfriendConfirmationDialog(friend);
                return true;
            }
            Toast.makeText(getContext(), "Tính năng " + title + " đang phát triển", Toast.LENGTH_SHORT).show();
            return false;
        });
        popup.show();
    }

    @Override
    public void onUnfriendClick(User friend) {
        showUnfriendConfirmationDialog(friend);
    }

    private void showUnfriendConfirmationDialog(User friend) {
        if (getContext() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Confirm Unfriend")
                .setMessage("Are you sure you want to remove " + friend.getName() + " from your friends?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    String currentUserId = firebaseAuth.getCurrentUser() != null
                            ? firebaseAuth.getCurrentUser().getUid()
                            : "";
                    removeFriend(currentUserId, friend.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFriend(String currentUserId, String friendId) {
        contactViewModel.removeFriend(currentUserId, friendId)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource == null) return;

                    if (resource.isSuccess()) {
                        Toast.makeText(getContext(), "Friend removed successfully",
                                Toast.LENGTH_SHORT).show();
                        // Reload friends list after removing
                        reloadFriendData();
                    } else if (resource.isError()) {
                        Toast.makeText(getContext(), "Error: " + resource.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFriendRequests() {
        // Friend requests are automatically loaded via ViewModel observer
        // The observer in observeViewModel() handles the real-time updates
    }

    /**
     * Called from MainActivity when friend events are received via WebSocket
     *
     * @param eventType ACCEPTED, REJECTED, ADDED, or REMOVED
     * @param userId    The user ID involved in the event
     */
    public void onFriendEventReceived(String eventType, String userId) {
        Log.e("ContactFragment", "🔔 onFriendEventReceived: " + eventType + " for user: " + userId);
        
        // Simplified event handling for now
        reloadFriendData();
        
        /*
        switch (eventType) {
            case "REQUEST_RECEIVED":
                ...
        }
        */
    }
}
