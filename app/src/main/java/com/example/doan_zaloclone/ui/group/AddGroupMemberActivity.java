package com.example.doan_zaloclone.ui.group;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doan_zaloclone.databinding.ActivityAddGroupMemberBinding;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.repository.AuthRepository;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.viewmodel.GroupViewModel;

import java.util.ArrayList;
import java.util.List;

public class AddGroupMemberActivity extends AppCompatActivity {

    private ActivityAddGroupMemberBinding binding;
    private SelectableFriendsAdapter friendsAdapter;
    private ContactViewModel contactViewModel;
    private GroupViewModel groupViewModel;

    private String conversationId;
    private List<String> currentMemberIds;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddGroupMemberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get data from intent
        conversationId = getIntent().getStringExtra("conversationId");
        currentMemberIds = getIntent().getStringArrayListExtra("currentMemberIds");
        AuthRepository authRepository = new AuthRepository();
        currentUserId = authRepository.getCurrentUser() != null
                ? authRepository.getCurrentUser().getUid()
                : null;

        if (conversationId == null || currentMemberIds == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nhóm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupViewModels();
        loadFriends();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        friendsAdapter = new SelectableFriendsAdapter(
                new ArrayList<>(),
                selectedCount -> binding.selectedCountTextView.setText(selectedCount + " đã chọn")
        );
        binding.friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.friendsRecyclerView.setAdapter(friendsAdapter);
    }

    private void setupViewModels() {
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        // Observe friends
        contactViewModel.getFriendsLiveData().observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        if (resource.getData() != null) {
                            // Filter out users who are already members
                            List<User> nonMembers = new ArrayList<>();
                            for (User user : resource.getData()) {
                                if (!currentMemberIds.contains(user.getId())) {
                                    nonMembers.add(user);
                                }
                            }
                            friendsAdapter.updateFriends(nonMembers);
                        }
                        break;

                    case ERROR:
                        Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        // Observe add member result
        groupViewModel.getUpdateResult().observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        Toast.makeText(this, "Đã thêm thành viên", Toast.LENGTH_SHORT).show();
                        finish();
                        break;

                    case ERROR:
                        Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.addButton.setEnabled(true);
                        break;

                    case LOADING:
                        binding.addButton.setEnabled(false);
                        break;
                }
            }
        });
    }

    private void loadFriends() {
        contactViewModel.loadFriends(currentUserId);
    }

    private void setupListeners() {
        binding.addButton.setOnClickListener(v -> {
            List<String> selectedIds = friendsAdapter.getSelectedFriendIds();
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 thành viên", Toast.LENGTH_SHORT).show();
                return;
            }

            groupViewModel.addGroupMembers(conversationId, selectedIds);
        });
    }
}
