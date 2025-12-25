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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ContactFragment", "onResume called - loading friends");
        // Load friends when fragment becomes visible (critical for show/hide pattern)
        loadFriends();
    }

    private void initViews(View view) {
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        
        // Find and set click listener for Friend Invitations row
        View friendRequestsRow = view.findViewById(R.id.clickable_friend_requests);
        if (friendRequestsRow != null) {
            friendRequestsRow.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.doan_zaloclone.ui.contact.FriendRequestActivity.class);
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
                    friendsAdapter.updateFriends(friends);
                }
            } else if (resource.isError()) {
                Log.e("ContactFragment", "Error loading friends: " + resource.getMessage());
            }
        });
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
