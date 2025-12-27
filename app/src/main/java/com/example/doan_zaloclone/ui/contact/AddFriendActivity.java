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
    
    // Result View
    private ImageView imgAvatar;
    private TextView tvName;
    private TextView tvPhone;
    private Button btnAddFriend;
    
    private ContactViewModel viewModel;
    private User foundUser;
    private String currentRequestId = null; // Track current friend request ID
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
        
        imgAvatar = findViewById(R.id.imgAvatar);
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        btnAddFriend = findViewById(R.id.btnAddFriend);
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
                // Request was accepted - reset button
                if (foundUser != null && foundUser.getId().equals(userId)) {
                    runOnUiThread(() -> {
                        btnAddFriend.setText("ĐÃ LÀ BẠN BÈ");
                        btnAddFriend.setEnabled(false);
                        currentRequestId = null;
                    });
                }
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                // Request was rejected - reset to allow re-sending
                if (foundUser != null && foundUser.getId().equals(userId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddFriendActivity.this, "Lời mời đã bị từ chối", Toast.LENGTH_SHORT).show();
                        resetButtonToAddFriend();
                    });
                }
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                // Not relevant - we are the sender
            }

            @Override
            public void onFriendAdded(String userId) {
                // Friend added
                if (foundUser != null && foundUser.getId().equals(userId)) {
                    runOnUiThread(() -> {
                        btnAddFriend.setText("ĐÃ LÀ BẠN BÈ");
                        btnAddFriend.setEnabled(false);
                        currentRequestId = null;
                    });
                }
            }

            @Override
            public void onFriendRemoved(String userId) {
                // Not relevant
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
        
        btnAddFriend.setOnClickListener(v -> {
            if (foundUser != null) {
                String buttonText = btnAddFriend.getText().toString();
                
                if (buttonText.equals("THU HỒI")) {
                    // Cancel the friend request
                    cancelFriendRequest();
                } else if (buttonText.equals("KẾT BẠN")) {
                    // Send friend request
                    sendFriendRequest(foundUser);
                }
            }
        });
    }

    private void performSearch(String query) {
        viewModel.searchUsers(query);
    }
    
    private void sendFriendRequest(User user) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
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
                                        currentRequestId = req.getId();
                                        break;
                                    }
                                }
                            }
                        }
                    });
                    
                    // Change button to "THU HỒI" (Recall)
                    btnAddFriend.setText("THU HỒI");
                    btnAddFriend.setEnabled(true);
                    btnAddFriend.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
                    
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    /**
     * Cancel the current friend request
     */
    private void cancelFriendRequest() {
        if (currentRequestId == null) {
            Toast.makeText(this, "Không tìm thấy lời mời để thu hồi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a temporary FriendRequest object with ID
        FriendRequest tempRequest = new FriendRequest();
        tempRequest.setId(currentRequestId);
        
        viewModel.cancelFriendRequest(tempRequest).observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(this, "Đã thu hồi lời mời", Toast.LENGTH_SHORT).show();
                resetButtonToAddFriend();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Reset button back to "KẾT BẠN" state
     */
    private void resetButtonToAddFriend() {
        btnAddFriend.setText("KẾT BẠN");
        btnAddFriend.setEnabled(true);
        btnAddFriend.setBackgroundTintList(getColorStateList(R.color.colorPrimary));
        currentRequestId = null;
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(this, resource -> {
            if (resource == null) return;
            
            if (resource.getStatus() == Resource.Status.LOADING) {
                // Show loading?
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<User> users = resource.getData();
                processSearchResults(users);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Lỗi tìm kiếm: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSearchResults(List<User> users) {
        otherOptions.setVisibility(View.GONE);
        searchResultContainer.setVisibility(View.VISIBLE);
        
        if (users != null && !users.isEmpty()) {
            foundUser = users.get(0);
            
            tvNotFound.setVisibility(View.GONE);
            ((View)imgAvatar.getParent().getParent()).setVisibility(View.VISIBLE);
            
            tvName.setText(foundUser.getName());
            tvPhone.setText(foundUser.getEmail());
            
            if (foundUser.getAvatarUrl() != null && !foundUser.getAvatarUrl().isEmpty()) {
                Glide.with(this).load(foundUser.getAvatarUrl()).circleCrop().into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_avatar);
            }
            
            // Reset to default "KẾT BẠN" state
            resetButtonToAddFriend();
            
        } else {
            foundUser = null;
            currentRequestId = null;
            tvNotFound.setVisibility(View.VISIBLE);
            ((View)imgAvatar.getParent().getParent()).setVisibility(View.GONE);
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
