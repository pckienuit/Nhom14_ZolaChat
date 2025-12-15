package com.example.doan_zaloclone.ui.group;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.databinding.ActivityGroupInfoBinding;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.GroupMember;
import com.example.doan_zaloclone.repository.AuthRepository;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.viewmodel.GroupViewModel;
import com.example.doan_zaloclone.viewmodel.RoomViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupInfoActivity extends AppCompatActivity implements GroupMemberAdapter.OnMemberActionListener {

    private ActivityGroupInfoBinding binding;
    private GroupViewModel groupViewModel;
    private RoomViewModel roomViewModel;
    private ContactViewModel contactViewModel;
    private GroupMemberAdapter memberAdapter;
    private FirebaseFirestore firestore;

    private String conversationId;
    private String currentUserId;
    private Conversation conversation;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase and ViewModels
        firestore = FirebaseFirestore.getInstance();
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        // Get current user ID
        AuthRepository authRepository = new AuthRepository();
        currentUserId = authRepository.getCurrentUser() != null 
                ? authRepository.getCurrentUser().getUid() 
                : null;

        // Get conversation ID from intent
        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nhóm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        loadGroupInfo();
        setupListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Will be updated in updateUI when conversation is loaded
        boolean isAdmin = conversation != null && conversation.isAdmin(currentUserId);
        memberAdapter = new GroupMemberAdapter(currentUserId, isAdmin, this);
        binding.membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.membersRecyclerView.setAdapter(memberAdapter);
    }

    private void loadGroupInfo() {
        firestore.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        conversation = documentSnapshot.toObject(Conversation.class);
                        if (conversation != null) {
                            updateUI();
                            loadMembers();
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi khi tải thông tin nhóm", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateUI() {
        // Check if current user is admin
        isAdmin = conversation.isAdmin(currentUserId);

        // Update group name
        binding.groupNameTextView.setText(conversation.getName());

        // Update member count
        int memberCount = conversation.getMemberIds() != null ? conversation.getMemberIds().size() : 0;
        binding.memberCountTextView.setText(memberCount + " thành viên");

        // Load avatar
        if (conversation.getAvatarUrl() != null && !conversation.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(conversation.getAvatarUrl())
                    .placeholder(R.drawable.ic_group)
                    .into(binding.groupAvatarImageView);
        } else {
            binding.groupAvatarImageView.setImageResource(R.drawable.ic_group);
        }

        // Show/hide admin-only buttons
        if (isAdmin) {
            binding.editGroupNameButton.setVisibility(View.VISIBLE);
            binding.changeAvatarButton.setVisibility(View.VISIBLE);
            binding.addMemberButton.setVisibility(View.VISIBLE);
            binding.deleteGroupButton.setVisibility(View.VISIBLE);
        } else {
            binding.editGroupNameButton.setVisibility(View.GONE);
            binding.changeAvatarButton.setVisibility(View.GONE);
            binding.addMemberButton.setVisibility(View.GONE);
            binding.deleteGroupButton.setVisibility(View.GONE);
        }

        // Update adapter with admin status
        memberAdapter = new GroupMemberAdapter(currentUserId, isAdmin, this);
        binding.membersRecyclerView.setAdapter(memberAdapter);
        loadMembers();
    }

    private void loadMembers() {
        if (conversation == null || conversation.getMemberIds() == null) return;

        List<GroupMember> members = new ArrayList<>();
        int totalMembers = conversation.getMemberIds().size();
        int[] loadedCount = {0};

        for (String memberId : conversation.getMemberIds()) {
            firestore.collection("users")
                    .document(memberId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String avatarUrl = documentSnapshot.getString("avatarUrl");
                            boolean isMemberAdmin = conversation.isAdmin(memberId);

                            GroupMember member = new GroupMember(memberId, name, email, avatarUrl, isMemberAdmin);
                            members.add(member);
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalMembers) {
                            // Sort: admin first, then alphabetically
                            members.sort((m1, m2) -> {
                                if (m1.isAdmin() && !m2.isAdmin()) return -1;
                                if (!m1.isAdmin() && m2.isAdmin()) return 1;
                                return m1.getName().compareTo(m2.getName());
                            });
                            memberAdapter.updateMembers(members);
                        }
                    });
        }
    }

    private void setupListeners() {
        // Edit group name
        binding.editGroupNameButton.setOnClickListener(v -> showEditNameDialog());

        // Change avatar (TODO: implement image picker)
        binding.changeAvatarButton.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        );

        // Add member
        binding.addMemberButton.setOnClickListener(v -> showAddMemberDialog());

        // Leave group
        binding.leaveGroupButton.setOnClickListener(v -> showLeaveGroupDialog());

        // Delete group (admin only)
        binding.deleteGroupButton.setOnClickListener(v -> showDeleteGroupDialog());
    }

    private void showEditNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        TextInputEditText editText = dialogView.findViewById(R.id.editText);
        editText.setText(conversation.getName());

        new AlertDialog.Builder(this)
                .setTitle("Đổi tên nhóm")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        groupViewModel.updateGroupName(conversationId, newName);
                    } else {
                        Toast.makeText(this, "Tên nhóm không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddMemberDialog() {
        // Navigate to member selection screen
        Intent intent = new Intent(this, AddGroupMemberActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.putStringArrayListExtra("currentMemberIds", 
                new ArrayList<>(conversation.getMemberIds()));
        startActivity(intent);
    }

    private void showLeaveGroupDialog() {
        // Check if user is the last admin
        if (conversation != null && conversation.isLastAdmin(currentUserId)) {
            new AlertDialog.Builder(this)
                    .setTitle("Không thể rời nhóm")
                    .setMessage("Bạn là quản trị viên duy nhất. Vui lòng chuyển quyền quản trị cho thành viên khác trước khi rời nhóm.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Rời nhóm")
                .setMessage("Bạn có chắc muốn rời khỏi nhóm này?")
                .setPositiveButton("Rời nhóm", (dialog, which) -> 
                    groupViewModel.leaveGroup(conversationId, currentUserId)
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteGroupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nhóm")
                .setMessage("Bạn có chắc muốn xóa nhóm này? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> 
                    groupViewModel.deleteGroup(conversationId)
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onRemoveMember(GroupMember member) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thành viên")
                .setMessage("Bạn có chắc muốn xóa " + member.getName() + " khỏi nhóm?")
                .setPositiveButton("Xóa", (dialog, which) -> 
                    groupViewModel.removeGroupMember(conversationId, member.getUserId())
                )
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    public void onMemberLongClick(GroupMember member) {
        // Only admins can transfer admin rights
        if (conversation == null || !conversation.isAdmin(currentUserId)) {
            return;
        }
        
        // Cannot transfer to yourself or someone who's already admin
        if (member.getUserId().equals(currentUserId) || member.isAdmin()) {
            return;
        }
        
        // Show dialog to transfer admin
        new AlertDialog.Builder(this)
                .setTitle("Chuyển quyền quản trị")
                .setMessage("Bạn có muốn chuyển quyền quản trị cho " + member.getName() + "?")
                .setPositiveButton("Chuyển quyền", (dialog, which) -> {
                    groupViewModel.transferAdmin(conversationId, member.getUserId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void observeViewModel() {
        groupViewModel.getUpdateResult().observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                        
                        // Check if user left or group was deleted
                        if (resource.getData() != null && resource.getData()) {
                            // Reload group info to update UI
                            loadGroupInfo();
                        }
                        break;
                        
                    case ERROR:
                        Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                        
                    case LOADING:
                        // Show loading indicator if needed
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload members when returning from add member screen
        loadGroupInfo();
    }
}
