package com.example.doan_zaloclone.ui.contact;

import android.content.Intent;
import android.os.Bundle;
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

public class ContactFragment extends Fragment {

    private EditText searchEmailEditText;
    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView friendRequestsRecyclerView;
    private View divider;

    private UserAdapter userAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    
    private FirestoreManager firestoreManager;
    private FirebaseAuth firebaseAuth;

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
        loadFriendRequests();
        
        return view;
    }

    private void initViews(View view) {
        searchEmailEditText = view.findViewById(R.id.searchEmailEditText);
        searchButton = view.findViewById(R.id.searchButton);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView);
        divider = view.findViewById(R.id.divider);
    }

    private void setupRecyclerViews() {
        // Setup search results RecyclerView
        userAdapter = new UserAdapter(new ArrayList<>(), this::onUserClick);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setAdapter(userAdapter);

        // Setup friend requests RecyclerView
        friendRequestAdapter = new FriendRequestAdapter(new ArrayList<>(), 
            new FriendRequestAdapter.FriendRequestListener() {
                @Override
                public void onAccept(FriendRequest request) {
                    Toast.makeText(getContext(), "Accepted friend request", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                }

                @Override
                public void onReject(FriendRequest request) {
                    Toast.makeText(getContext(), "Rejected friend request", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                }
            });
        
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);
    }

    private void setupSearchListener() {
        searchButton.setOnClickListener(v -> {
            String email = searchEmailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an email", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUserByEmail(email);
        });
    }

    private void searchUserByEmail(String email) {
        // Check if user is logged in
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        // Search in Firestore
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        firestoreManager.searchUserByEmail(email, new FirestoreManager.OnUserSearchListener() {
            @Override
            public void onSuccess(List<User> users) {
                if (getActivity() == null) return;
                
                searchButton.setEnabled(true);
                searchButton.setText("Search");
                
                if (users.isEmpty()) {
                    Toast.makeText(getContext(), "No user found with this email", Toast.LENGTH_SHORT).show();
                    searchResultsRecyclerView.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                } else {
                    // Lọc bỏ user hiện tại khỏi kết quả
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



    private void onUserClick(User user) {
        // Check if user is logged in
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
                    // Create new conversation
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
        Intent intent = new Intent(getActivity(), RoomActivity.class);
        intent.putExtra("conversationId", conversation.getId());
        intent.putExtra("conversationName", conversation.getName());
        startActivity(intent);
        
        // Clear search
        searchEmailEditText.setText("");
        searchResultsRecyclerView.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);
    }

    private void loadFriendRequests() {
        // TODO: Load friend requests from Firestore when feature is implemented
        // For now, show empty list
        friendRequestAdapter.updateRequests(new ArrayList<>());
    }
}
