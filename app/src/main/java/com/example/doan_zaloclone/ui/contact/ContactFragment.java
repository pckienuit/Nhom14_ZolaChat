package com.example.doan_zaloclone.ui.contact;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ContactFragment extends Fragment implements UserAdapter.OnUserActionListener, FriendsAdapter.OnFriendClickListener {

    private EditText searchEmailEditText;
    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView friendRequestsRecyclerView;
    private RecyclerView friendsRecyclerView;
    private View divider;

    private UserAdapter userAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    private FriendsAdapter friendsAdapter;
    
    private FirestoreManager firestoreManager;
    private FirebaseAuth firebaseAuth;
    
    private com.google.firebase.firestore.ListenerRegistration friendRequestsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        initViews(view);
        setupRecyclerViews();
        setupSearchListener();
        loadFriends();
        loadFriendRequests();
        
        return view;
    }

    private void initViews(View view) {
        searchEmailEditText = view.findViewById(R.id.searchEmailEditText);
        searchButton = view.findViewById(R.id.searchButton);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView);
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        divider = view.findViewById(R.id.divider);
    }

    private void setupRecyclerViews() {
        // Setup friends RecyclerView
        friendsAdapter = new FriendsAdapter(new ArrayList<>(), this);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsRecyclerView.setHasFixedSize(true); // Optimize performance
        friendsRecyclerView.setAdapter(friendsAdapter);
        
        // Setup search results RecyclerView
        userAdapter = new UserAdapter(new ArrayList<>(), this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setHasFixedSize(true); // Optimize performance
        searchResultsRecyclerView.setAdapter(userAdapter);

        // Setup friend requests RecyclerView
        friendRequestAdapter = new FriendRequestAdapter(new ArrayList<>(), 
            new FriendRequestAdapter.FriendRequestListener() {
                @Override
                public void onAccept(FriendRequest request) {
                    acceptFriendRequest(request);
                }

                @Override
                public void onReject(FriendRequest request) {
                    rejectFriendRequest(request);
                }
            });
        
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendRequestsRecyclerView.setHasFixedSize(true); // Optimize performance
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);
    }

    private void setupSearchListener() {
        searchButton.setOnClickListener(v -> {
            String query = searchEmailEditText.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(getContext(), "Please enter name or email", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUsers(query);
        });
    }

    private void searchUsers(String query) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        firestoreManager.searchUsers(query, new FirestoreManager.OnUserSearchListener() {
            @Override
            public void onSuccess(List<User> users) {
                if (getActivity() == null) return;
                
                searchButton.setEnabled(true);
                searchButton.setText("Search");
                
                if (users.isEmpty()) {
                    Toast.makeText(getContext(), "No users found", Toast.LENGTH_SHORT).show();
                    searchResultsRecyclerView.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                } else {
                    // Filter out current user
                    List<User> filteredUsers = new ArrayList<>();
                    for (User user : users) {
                        if (!currentUserId.equals(user.getId())) {
                            filteredUsers.add(user);
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        Toast.makeText(getContext(), "Cannot add yourself", Toast.LENGTH_SHORT).show();
                        searchResultsRecyclerView.setVisibility(View.GONE);
                        divider.setVisibility(View.GONE);
                    } else {
                        userAdapter.updateUsers(filteredUsers);
                        searchResultsRecyclerView.setVisibility(View.VISIBLE);
                        divider.setVisibility(View.VISIBLE);
                        
                        // Check friend request status for each user
                        for (User user : filteredUsers) {
                            checkFriendRequestStatus(currentUserId, user.getId());
                        }
                        
                        Toast.makeText(getContext(), "Found " + filteredUsers.size() + " user(s)", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                
                searchButton.setEnabled(true);
                searchButton.setText("Search");
                Toast.makeText(getContext(), "Error searching: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkFriendRequestStatus(String fromUserId, String toUserId) {
        firestoreManager.checkFriendRequestStatus(fromUserId, toUserId,
            new FirestoreManager.OnFriendRequestStatusListener() {
                @Override
                public void onStatus(String status) {
                    if (getActivity() != null) {
                        userAdapter.updateFriendRequestStatus(toUserId, status);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // Silently fail - will show default "Add Friend" button
                }
            });
    }

    @Override
    public void onUserClick(User user) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "Opening chat with " + user.getName() + "...", Toast.LENGTH_SHORT).show();

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String currentUserName = firebaseAuth.getCurrentUser().getDisplayName() != null 
                ? firebaseAuth.getCurrentUser().getDisplayName() 
                : "User";
        
        // Check if conversation already exists
        firestoreManager.findExistingConversation(currentUserId, user.getId(), 
            new FirestoreManager.OnConversationFoundListener() {
                @Override
                public void onFound(Conversation conversation) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Opening existing chat", Toast.LENGTH_SHORT).show();
                        openChatRoom(conversation);
                    }
                }

                @Override
                public void onNotFound() {
                    if (getActivity() != null) {
                        createNewConversation(currentUserId, currentUserName, user);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    public void onAddFriendClick(User user) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String currentUserName = firebaseAuth.getCurrentUser().getDisplayName() != null
                ? firebaseAuth.getCurrentUser().getDisplayName()
                : "User";
        String currentUserEmail = firebaseAuth.getCurrentUser().getEmail() != null
                ? firebaseAuth.getCurrentUser().getEmail()
                : "";

        firestoreManager.sendFriendRequest(
                currentUserId,
                currentUserName,
                currentUserEmail,
                user.getId(),
                new FirestoreManager.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() != null) {
                            Toast.makeText(getContext(), "Friend request sent to " + user.getName(), Toast.LENGTH_SHORT).show();
                            userAdapter.updateFriendRequestStatus(user.getId(), "PENDING");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getActivity() != null) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void acceptFriendRequest(FriendRequest request) {
        firestoreManager.acceptFriendRequest(request.getId(),
            new FirestoreManager.OnFriendRequestListener() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Accepted friend request from " + request.getFromUserName(), Toast.LENGTH_SHORT).show();
                        // Reload friends list
                        loadFriends();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Error accepting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void rejectFriendRequest(FriendRequest request) {
        firestoreManager.rejectFriendRequest(request.getId(),
            new FirestoreManager.OnFriendRequestListener() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Rejected friend request from " + request.getFromUserName(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Error rejecting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        searchEmailEditText.setText("");
        searchResultsRecyclerView.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);
    }

    private void loadFriends() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d("ContactFragment", "Loading friends for user: " + currentUserId);
        
        firestoreManager.getFriends(currentUserId, new FirestoreManager.OnFriendsLoadedListener() {
            @Override
            public void onFriendsLoaded(List<User> friends) {
                if (getActivity() != null) {
                    Log.d("ContactFragment", "Friends loaded: " + friends.size());
                    friendsAdapter.updateFriends(friends);
                    
                    if (friends.isEmpty()) {
                        Log.d("ContactFragment", "No friends found - make sure you have ACCEPTED friend requests");
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    Log.e("ContactFragment", "Error loading friends", e);
                    Toast.makeText(getContext(), "Error loading friends: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMessageClick(User friend) {
        // Open conversation with friend
        onUserClick(friend);
    }

    private void loadFriendRequests() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        // Listen to friend requests realtime
        friendRequestsListener = firestoreManager.listenToFriendRequests(currentUserId,
            new FirestoreManager.OnFriendRequestsChangedListener() {
                @Override
                public void onFriendRequestsChanged(List<FriendRequest> requests) {
                    if (getActivity() != null) {
                        friendRequestAdapter.updateRequests(requests);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Error loading friend requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listener
        if (friendRequestsListener != null) {
            friendRequestsListener.remove();
        }
    }
}
