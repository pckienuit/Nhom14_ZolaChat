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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
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
    
    private ContactViewModel contactViewModel;
    private FirebaseAuth firebaseAuth;
    private FirestoreManager firestoreManager; // Still needed for some legacy operations

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
        
        initViews(view);
        setupRecyclerViews();
        setupSearchListener();
        observeViewModel();
        loadFriendRequests();
        
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
        friendsRecyclerView.setAdapter(friendsAdapter);
        
        // Setup search results RecyclerView
        userAdapter = new UserAdapter(new ArrayList<>(), this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setHasFixedSize(true);
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
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);
    }
    
    private void observeViewModel() {
        // Observe search results
        contactViewModel.getSearchResults().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || resource.getData() == null) {
                searchResultsRecyclerView.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                return;
            }
            
            if (resource.isSuccess()) {
                List<User> users = resource.getData();
                String currentUserId = firebaseAuth.getCurrentUser() != null 
                    ? firebaseAuth.getCurrentUser().getUid() 
                    : "";
                
                // Filter out current user
                List<User> filteredUsers = new ArrayList<>();
                for (User user : users) {
                    if (!currentUserId.equals(user.getId())) {
                        filteredUsers.add(user);
                    }
                }
                
                if (!filteredUsers.isEmpty()) {
                    userAdapter.updateUsers(filteredUsers);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);
                    
                    // Check friend request status for each user
                    for (User user : filteredUsers) {
                        checkFriendRequestStatus(currentUserId, user.getId());
                    }
                }
            } else if (resource.isError()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
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
        
        // Observe friend requests
        contactViewModel.getFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess()) {
                List<FriendRequest> requests = resource.getData();
                if (requests != null) {
                    Log.d("ContactFragment", "Friend requests loaded: " + requests.size());
                    friendRequestAdapter.updateRequests(requests);
                    friendRequestsRecyclerView.requestLayout();
                }
            } else if (resource.isError()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading friend requests: " + resource.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        
        // Use ViewModel to search
        contactViewModel.searchUsers(query);
        
        // Reset button state after a delay (search is asynchronous)
        searchButton.postDelayed(() -> {
            searchButton.setEnabled(true);
            searchButton.setText("Search");
        }, 1000);
    }

    private void checkFriendRequestStatus(String fromUserId, String toUserId) {
        contactViewModel.checkFriendRequestStatus(fromUserId, toUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                userAdapter.updateFriendRequestStatus(toUserId, resource.getData());
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
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        // Fetch current user's name from Firestore
        firestoreManager.getFirestore().collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String currentUserName = "User";
                
                if (documentSnapshot.exists()) {
                    User currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null && currentUser.getName() != null) {
                        currentUserName = currentUser.getName();
                    }
                }
                
                // Use ViewModel to send friend request
                String finalUserName = currentUserName;
                contactViewModel.sendFriendRequest(currentUserId, user.getId(), finalUserName)
                    .observe(this, resource -> {
                        if (resource == null) return;
                        
                        if (resource.isSuccess()) {
                            if (getActivity() != null) {
                                Toast.makeText(getContext(), "Friend request sent to " + user.getName(), 
                                    Toast.LENGTH_SHORT).show();
                                userAdapter.updateFriendRequestStatus(user.getId(), "PENDING");
                            }
                        } else if (resource.isError()) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error: " + resource.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error fetching user info", Toast.LENGTH_SHORT).show();
            });
    }

    private void acceptFriendRequest(FriendRequest request) {
        contactViewModel.acceptFriendRequest(request).observe(this, resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess()) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Accepted friend request from " + request.getFromUserName(), 
                        Toast.LENGTH_SHORT).show();
                    // Reload friends list
                    loadFriends();
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
        searchEmailEditText.setText("");
        contactViewModel.clearSearchResults();
    }

    private void loadFriends() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d("ContactFragment", "Loading friends for user: " + currentUserId);
        
        // Friends are automatically loaded via ViewModel observer
        // Just refresh if needed
        contactViewModel.getFriends(currentUserId);
    }

    @Override
    public void onMessageClick(User friend) {
        // Open conversation with friend
        onUserClick(friend);
    }

    private void loadFriendRequests() {
        // Friend requests are automatically loaded via ViewModel observer
        // The observer in observeViewModel() handles the real-time updates
    }
}
