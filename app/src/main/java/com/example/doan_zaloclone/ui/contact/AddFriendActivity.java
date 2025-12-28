package com.example.doan_zaloclone.ui.contact;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AddFriendActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnSearch;
    private FrameLayout searchResultContainer;
    private LinearLayout otherOptions;
    private TextView tvNotFound;
    
    // RecyclerView for results
    private androidx.recyclerview.widget.RecyclerView recyclerSearchResults;
    private SearchResultAdapter searchResultAdapter;
    
    private ContactViewModel viewModel;
    private SocketManager.OnFriendEventListener friendEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        setupEdgeToEdge();
        
        setContentView(R.layout.activity_add_friend);

        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        initViews();
        setupListeners();
        observeViewModel();
        setupWebSocketListener();
    }
    
    private void setupEdgeToEdge() {
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        searchResultContainer = findViewById(R.id.searchResultContainer);
        otherOptions = findViewById(R.id.otherOptions);
        tvNotFound = findViewById(R.id.tvNotFound);
        recyclerSearchResults = findViewById(R.id.recyclerSearchResults);
        
        // Setup RecyclerView with both callbacks
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        searchResultAdapter = new SearchResultAdapter(currentUserId, new SearchResultAdapter.OnUserActionListener() {
            @Override
            public void onAddFriendClick(User user) {
                sendFriendRequest(user);
            }
            
            @Override
            public void onCancelRequestClick(String userId, String requestId) {
                cancelFriendRequest(userId, requestId);
            }
        });
        recyclerSearchResults.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        recyclerSearchResults.setAdapter(searchResultAdapter);
    }
    
    /**
     * Setup WebSocket listener for friend request events
     */
    private void setupWebSocketListener() {
        friendEventListener = new SocketManager.OnFriendEventListener() {
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                // Not relevant
            }

            @Override
            public void onFriendRequestAccepted(String userId) {
                // Request was accepted - update button state
                runOnUiThread(() -> {
                    searchResultAdapter.updateButtonState(userId, "ĐÃ LÀ BẠN BÈ", null);
                });
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                // Request was rejected - reset to allow re-sending
                runOnUiThread(() -> {
                    Toast.makeText(AddFriendActivity.this, "Lời mời đã bị từ chối", Toast.LENGTH_SHORT).show();
                    searchResultAdapter.updateButtonState(userId, "KẾT BẠN", null);
                });
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                // Not relevant - we are the sender
            }

            @Override
            public void onFriendAdded(String userId) {
                // Friend added - update button state
                runOnUiThread(() -> {
                    searchResultAdapter.updateButtonState(userId, "ĐÃ LÀ BẠN BÈ", null);
                });
            }

            @Override
            public void onFriendRemoved(String userId) {
                // Friend removed - reset button
                runOnUiThread(() -> {
                    searchResultAdapter.updateButtonState(userId, "KẾT BẠN", null);
                });
            }
            
            @Override
            public void onFriendStatusChanged(String friendId, boolean isOnline) {
                // Not relevant for add friend activity
            }
        };
        
        SocketManager.getInstance().addFriendEventListener(friendEventListener);
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSearch.setEnabled(s.length() > 0);
                if (s.length() > 0) {
                    btnSearch.setBackgroundTintList(getColorStateList(R.color.colorPrimary));
                } else {
                     // Dim color
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
    }

    private void performSearch(String query) {
        // Clear kết quả cũ trước khi tìm kiếm mới
        viewModel.clearSearchResults();
        // Thực hiện tìm kiếm
        viewModel.searchUsers(query);
    }
    
    private void sendFriendRequest(User user) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        
        // Kiểm tra không cho gửi lời mời cho chính mình
        if (currentUserId.equals(user.getId())) {
            Toast.makeText(this, "Không thể gửi lời mời kết bạn cho chính mình", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentUserName == null) currentUserName = "User";
        
        viewModel.sendFriendRequest(currentUserId, user.getId(), currentUserName)
            .observe(this, resource -> {
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    
                    // Load sent requests to get the request ID
                    viewModel.getSentFriendRequests(currentUserId).observe(this, requestsResource -> {
                        if (requestsResource != null && requestsResource.getStatus() == Resource.Status.SUCCESS) {
                            List<FriendRequest> requests = requestsResource.getData();
                            if (requests != null) {
                                for (FriendRequest req : requests) {
                                    if (req.getToUserId().equals(user.getId())) {
                                        // Cập nhật button state trong adapter
                                        searchResultAdapter.updateButtonState(user.getId(), "THU HỒI", req.getId());
                                        break;
                                    }
                                }
                            }
                        }
                    });
                    
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    /**
     * Cancel the current friend request
     */
    private void cancelFriendRequest(String userId, String requestId) {
        if (requestId == null) {
            Toast.makeText(this, "Không tìm thấy lời mời để thu hồi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a temporary FriendRequest object with ID
        FriendRequest tempRequest = new FriendRequest();
        tempRequest.setId(requestId);
        
        viewModel.cancelFriendRequest(tempRequest).observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(this, "Đã thu hồi lời mời", Toast.LENGTH_SHORT).show();
                
                // Reset button state trong adapter
                searchResultAdapter.updateButtonState(userId, "KẾT BẠN", null);
                
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(this, resource -> {
            if (resource == null) return;
            
            if (resource.getStatus() == Resource.Status.LOADING) {
                // Show loading?
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<User> users = resource.getData();
                // Bỏ qua nếu data là null (từ clearSearchResults)
                if (users != null) {
                    processSearchResults(users);
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Lỗi tìm kiếm: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSearchResults(List<User> users) {
        otherOptions.setVisibility(View.GONE);
        searchResultContainer.setVisibility(View.VISIBLE);
        
        if (users != null && !users.isEmpty()) {
            // Giới hạn tối đa 10 kết quả
            List<User> limitedUsers = users;
            if (users.size() > 10) {
                limitedUsers = users.subList(0, 10);
            }
            
            // Hiển thị RecyclerView
            tvNotFound.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.VISIBLE);
            searchResultAdapter.setUsers(limitedUsers);
        } else {
            // Hiển thị thông báo không tìm thấy
            tvNotFound.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove WebSocket listener
        if (friendEventListener != null) {
            SocketManager.getInstance().removeFriendEventListener(friendEventListener);
        }
    }
}
