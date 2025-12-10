package com.example.doan_zaloclone.ui.contact;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.ui.room.RoomActivity;

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

    // Demo: Current user ID (trong thực tế sẽ lấy từ Firebase Auth)
    // String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    //String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
    
    private static final String CURRENT_USER_ID = "demo_current_user_id";
    private static final String CURRENT_USER_NAME = "Demo User";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        
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
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

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
                        if (!CURRENT_USER_ID.equals(user.getId())) {
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
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                
                searchButton.setEnabled(true);
                searchButton.setText("Search");
                Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onUserClick(User user) {
        // Hiển thị loading
        Toast.makeText(getContext(), "Opening chat with " + user.getName() + "...", Toast.LENGTH_SHORT).show();

        // Kiểm tra xem đã có conversation chưa
        firestoreManager.findExistingConversation(CURRENT_USER_ID, user.getId(),
                new FirestoreManager.OnConversationFoundListener() {
                    @Override
                    public void onFound(Conversation conversation) {
                        if (getActivity() == null) return;
                        // Đã có conversation -> Mở RoomActivity
                        openChatRoom(conversation);
                    }

                    @Override
                    public void onNotFound() {
                        if (getActivity() == null) return;
                        // Chưa có conversation -> Tạo mới
                        createNewConversation(user);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getActivity() == null) return;
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewConversation(User otherUser) {
        firestoreManager.createConversation(
                CURRENT_USER_ID,
                CURRENT_USER_NAME,
                otherUser.getId(),
                otherUser.getName(),
                new FirestoreManager.OnConversationCreatedListener() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        if (getActivity() == null) return;
                        Toast.makeText(getContext(), "New chat created!", Toast.LENGTH_SHORT).show();
                        openChatRoom(conversation);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getActivity() == null) return;
                        Toast.makeText(getContext(), "Failed to create chat: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
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
        // Demo data
        List<FriendRequest> requests = new ArrayList<>();
        requests.add(new FriendRequest("1", "User Test 1", "test1@mail.com", FriendRequest.Status.PENDING));
        requests.add(new FriendRequest("2", "User Test 2", "test2@mail.com", FriendRequest.Status.PENDING));
        
        friendRequestAdapter.updateRequests(requests);
    }
}
