package com.example.doan_zaloclone.ui.group;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.viewmodel.GroupViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for creating a new group conversation
 * - Enter group name
 * - Multi-select friends
 * - Create group and navigate to chat
 */
public class CreateGroupActivity extends AppCompatActivity implements SelectableFriendsAdapter.OnSelectionChangedListener {

    private static final String TAG = "CreateGroupActivity";

    private MaterialToolbar toolbar;
    private EditText groupNameEditText;
    private RecyclerView friendsRecyclerView;
    private TextView selectedCountTextView;
    private Button createGroupButton;
    private ProgressBar progressBar;

    private SelectableFriendsAdapter friendsAdapter;
    private ContactViewModel contactViewModel;
    private GroupViewModel groupViewModel;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        firebaseAuth = FirebaseAuth.getInstance();
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadFriends(); // Load friends first to initialize LiveData
        observeViewModel(); // Then observe the LiveData
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        groupNameEditText = findViewById(R.id.groupNameEditText);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        selectedCountTextView = findViewById(R.id.selectedCountTextView);
        createGroupButton = findViewById(R.id.createGroupButton);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        friendsAdapter = new SelectableFriendsAdapter(new ArrayList<>(), this);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(friendsAdapter);
    }

    private void setupListeners() {
        // Enable create button only when group name is filled and at least 1 friend selected
        groupNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCreateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        createGroupButton.setOnClickListener(v -> createGroup());
    }

    private void observeViewModel() {
        // Observe friends list
        contactViewModel.getFriendsLiveData().observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        Log.d(TAG, "Loading friends...");
                        break;

                    case SUCCESS:
                        List<User> friends = resource.getData();
                        Log.d(TAG, "Loaded " + (friends != null ? friends.size() : 0) + " friends");
                        if (friends != null && !friends.isEmpty()) {
                            friendsAdapter.updateFriends(friends);
                        } else {
                            Toast.makeText(this, "Bạn chưa có bạn bè nào", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case ERROR:
                        Log.e(TAG, "Error loading friends: " + resource.getMessage());
                        Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        // Observe group creation result
        groupViewModel.getCreateGroupResult().observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        Log.d(TAG, "Creating group...");
                        createGroupButton.setEnabled(false);
                        createGroupButton.setText("Đang tạo...");
                        break;

                    case SUCCESS:
                        Conversation conversation = resource.getData();
                        Log.d(TAG, "Group created successfully: " + conversation.getId());
                        Toast.makeText(this, "Tạo nhóm thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to chat room
                        Intent intent = new Intent(this, RoomActivity.class);
                        intent.putExtra("conversationId", conversation.getId());
                        intent.putExtra("conversationName", conversation.getName());
                        startActivity(intent);
                        finish();
                        break;

                    case ERROR:
                        Log.e(TAG, "Error creating group: " + resource.getMessage());
                        Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                        createGroupButton.setEnabled(true);
                        createGroupButton.setText("Tạo nhóm");
                        break;
                }
            }
        });
    }

    private void loadFriends() {
        String currentUserId = firebaseAuth.getCurrentUser() != null 
                ? firebaseAuth.getCurrentUser().getUid() 
                : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        contactViewModel.loadFriends(currentUserId);
    }

    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        List<String> selectedFriendIds = friendsAdapter.getSelectedFriendIds();

        if (groupName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFriendIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 thành viên", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser() != null 
                ? firebaseAuth.getCurrentUser().getUid() 
                : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Add current user to member list
        List<String> memberIds = new ArrayList<>(selectedFriendIds);
        if (!memberIds.contains(currentUserId)) {
            memberIds.add(currentUserId);
        }

        Log.d(TAG, "Creating group: " + groupName + " with " + memberIds.size() + " members");
        groupViewModel.createGroup(currentUserId, groupName, memberIds);
    }

    private void updateCreateButtonState() {
        String groupName = groupNameEditText.getText().toString().trim();
        int selectedCount = friendsAdapter.getSelectedFriendIds().size();
        
        boolean isValid = !groupName.isEmpty() && selectedCount > 0;
        createGroupButton.setEnabled(isValid);
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        selectedCountTextView.setText(selectedCount + " đã chọn");
        updateCreateButtonState();
    }
}
