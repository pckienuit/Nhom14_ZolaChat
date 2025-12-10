package com.example.doan_zaloclone.ui.contact;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContactFragment extends Fragment {

    private EditText searchEmailEditText;
    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView friendRequestsRecyclerView;
    private View divider;

    private UserAdapter userAdapter;
    private FriendRequestAdapter friendRequestAdapter;

    // Demo mode flag
    private static final boolean DEMO_MODE = true;

    // Demo: Current user ID
    private static final String CURRENT_USER_ID = "demo_current_user_id";
    private static final String CURRENT_USER_NAME = "Demo User";

    // Demo data: Danh sách users có sẵn
    private List<User> demoUsers;
    
    // Demo data: Danh sách conversations đã tạo
    private List<Conversation> demoConversations;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        
        initDemoData();
        initViews(view);
        setupRecyclerViews();
        setupSearchListener();
        loadFriendRequests();
        
        return view;
    }

    private void initDemoData() {
        // Khởi tạo demo users
        demoUsers = new ArrayList<>();
        demoUsers.add(new User("user1", "Alice Johnson", "alice@example.com", ""));
        demoUsers.add(new User("user2", "Bob Smith", "bob@example.com", ""));
        demoUsers.add(new User("user3", "Charlie Brown", "charlie@example.com", ""));
        demoUsers.add(new User("user4", "Diana Prince", "diana@example.com", ""));
        demoUsers.add(new User("user5", "Evan Davis", "evan@example.com", ""));
        
        // Khởi tạo demo conversations (rỗng ban đầu)
        demoConversations = new ArrayList<>();
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

        if (DEMO_MODE) {
            // Demo mode: Simulate network delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                searchUserByEmailDemo(email);
            }, 500);
        } else {
            // Real Firestore search (khi có backend)
            // firestoreManager.searchUserByEmail(email, listener);
            Toast.makeText(getContext(), "Firestore mode not implemented yet", Toast.LENGTH_SHORT).show();
            searchButton.setEnabled(true);
            searchButton.setText("Search");
        }
    }

    private void searchUserByEmailDemo(String email) {
        if (getActivity() == null) return;

        searchButton.setEnabled(true);
        searchButton.setText("Search");

        // Tìm kiếm trong demo users
        List<User> results = new ArrayList<>();
        String searchEmail = email.trim().toLowerCase();
        
        for (User user : demoUsers) {
            if (user.getEmail().toLowerCase().contains(searchEmail)) {
                results.add(user);
            }
        }

        if (results.isEmpty()) {
            Toast.makeText(getContext(), "No user found with this email", Toast.LENGTH_SHORT).show();
            searchResultsRecyclerView.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        } else {
            // Lọc bỏ user hiện tại khỏi kết quả
            List<User> filteredUsers = new ArrayList<>();
            for (User user : results) {
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
                Toast.makeText(getContext(), "Found " + filteredUsers.size() + " user(s)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onUserClick(User user) {
        Toast.makeText(getContext(), "Opening chat with " + user.getName() + "...", Toast.LENGTH_SHORT).show();

        if (DEMO_MODE) {
            // Demo mode: Simulate network delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                checkAndOpenConversationDemo(user);
            }, 300);
        } else {
            // Real Firestore check (khi có backend)
            // firestoreManager.findExistingConversation(CURRENT_USER_ID, user.getId(), listener);
            Toast.makeText(getContext(), "Firestore mode not implemented yet", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndOpenConversationDemo(User otherUser) {
        if (getActivity() == null) return;

        // Kiểm tra xem đã có conversation với user này chưa
        Conversation existingConversation = null;
        for (Conversation conv : demoConversations) {
            if (conv.getMemberIds() != null && 
                conv.getMemberIds().contains(CURRENT_USER_ID) &&
                conv.getMemberIds().contains(otherUser.getId())) {
                existingConversation = conv;
                break;
            }
        }

        if (existingConversation != null) {
            // Đã có conversation
            Toast.makeText(getContext(), "Opening existing chat", Toast.LENGTH_SHORT).show();
            openChatRoom(existingConversation);
        } else {
            // Chưa có -> Tạo mới
            createNewConversationDemo(otherUser);
        }
    }

    private void createNewConversationDemo(User otherUser) {
        if (getActivity() == null) return;

        // Tạo conversation ID mới
        String conversationId = "conv_" + System.currentTimeMillis();
        
        List<String> memberIds = Arrays.asList(CURRENT_USER_ID, otherUser.getId());
        
        Conversation newConversation = new Conversation(
                conversationId,
                otherUser.getName(), // Tên hiển thị là tên của user kia
                "",
                System.currentTimeMillis(),
                memberIds
        );

        // Lưu vào danh sách demo
        demoConversations.add(newConversation);

        Toast.makeText(getContext(), "New chat created!", Toast.LENGTH_SHORT).show();
        openChatRoom(newConversation);
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
