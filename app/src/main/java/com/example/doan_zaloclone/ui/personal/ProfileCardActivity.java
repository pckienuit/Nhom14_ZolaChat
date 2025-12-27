package com.example.doan_zaloclone.ui.personal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.repository.FriendRepository;
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ProfileCardActivity - Reusable full profile card
 * Can be used for viewing own profile or others' profiles
 */
public class ProfileCardActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_IS_EDITABLE = "is_editable";

    private PersonalViewModel viewModel;
    private FriendRepository friendRepository;
    private com.example.doan_zaloclone.repository.UserRepository userRepository;
    private ProgressDialog progressDialog;
    private String userId;
    private String currentUserId;
    private boolean isEditable;
    private boolean areFriends = false;
    private boolean hasPendingRequest = false;  // True if current user has sent request to this user

    // Views
    private ImageView btnBack; 
    private ImageView coverImage;
    private ImageView avatarImage;
    private View btnEditCover; 
    private View btnEditAvatar;
    private TextView txtDisplayName;
    private TextView txtBio;
    private View nameContainer;
    private View bioContainer;
    private MaterialButton btnAddFriend;

    // Image pickers
    private ActivityResultLauncher<String> avatarPicker;
    private ActivityResultLauncher<String> coverPicker;
    private ActivityResultLauncher<String> permissionLauncher;

    private boolean isWaitingForAvatarPick = false;
    private boolean isWaitingForCoverPick = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_card);

        // Get extras
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        isEditable = getIntent().getBooleanExtra(EXTRA_IS_EDITABLE, false);

        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();

            // Default to current user if no userId provided
            if (userId == null) {
                userId = currentUserId;
                isEditable = true; // Can edit own profile
            }
        }

        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);
        friendRepository = new FriendRepository();
        userRepository = new com.example.doan_zaloclone.repository.UserRepository();

        // Load the specific user (not just current user)
        if (userId != null) {
            viewModel.loadUser(userId);

            // Check friendship status if viewing another user's profile
            if (!userId.equals(currentUserId)) {
                checkFriendshipStatus();
            }
        }

        setupImagePickers();
        initializeViews();
        setupToolbar();
        setupClickListeners();
        observeViewModel();
    }

    private void setupImagePickers() {
        avatarPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadAvatar(uri);
                    }
                }
        );

        coverPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadCover(uri);
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (isWaitingForAvatarPick) {
                            isWaitingForAvatarPick = false;
                            avatarPicker.launch("image/*");
                        } else if (isWaitingForCoverPick) {
                            isWaitingForCoverPick = false;
                            coverPicker.launch("image/*");
                        }
                    } else {
                        Toast.makeText(this, "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    // ...

    private void initializeViews() {
        // Toolbar Views
        btnBack = findViewById(R.id.btnBack);
        
        coverImage = findViewById(R.id.coverImage);
        avatarImage = findViewById(R.id.avatarImage);
        
        // These are invisible in new layout but kept for compatibility or repurposed
        btnEditCover = findViewById(R.id.btnEditCover); 
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        
        txtDisplayName = findViewById(R.id.txtDisplayName);
        txtBio = findViewById(R.id.txtBio);
        nameContainer = findViewById(R.id.nameContainer);
        bioContainer = findViewById(R.id.bioContainer);
        btnAddFriend = findViewById(R.id.btnAddFriend);
    }

    private void setupToolbar() {
         if (btnBack != null) {
             btnBack.setOnClickListener(v -> finish());
         }
         
         View btnMore = findViewById(R.id.btnMore);
         if (btnMore != null) {
             btnMore.setOnClickListener(this::showMoreMenu);
         }
    }

    private void showMoreMenu(View view) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
        
        // Add options
        if (isEditable) {
            // Self profile options
            popup.getMenu().add("Chia sẻ QR code");
            popup.getMenu().add("Cập nhật thông tin");
        } else {
            // Other user profile options
            popup.getMenu().add("Chia sẻ trang cá nhân");
            if (areFriends) {
                 popup.getMenu().add("Xóa bạn bè");
            }
             popup.getMenu().add("Chặn người này");
             popup.getMenu().add("Báo xấu");
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "Chia sẻ trang cá nhân":
                    shareProfile();
                    return true;
                case "Chia sẻ QR code":
                    shareQRCode();
                    return true;
                case "Xóa bạn bè":
                    showDeleteFriendConfirmation();
                    return true;
                case "Chặn người này":
                    showBlockConfirmation();
                    return true;
                case "Báo xấu":
                    showReportDialog();
                    return true;
                case "Cập nhật thông tin":
                    showEditNameDialog();
                    return true;
                default:
                    Toast.makeText(this, "Tính năng " + title + " đang phát triển", Toast.LENGTH_SHORT).show();
                    return false;
            }
        });
        
        popup.show();
    }

    /**
     * Share profile link or QR code
     */
    private void shareProfile() {
        // Get user from ViewModel
        viewModel.getCurrentUser().observe(this, resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                User user = resource.getData();
                String userName = user.getName() != null ? user.getName() : "Người dùng";
                
                String shareText = "Trang cá nhân: " + userName + "\n" +
                                  "ID: " + userId + "\n" +
                                  "Zalo: zalo://user/" + userId;
                
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Trang cá nhân Zalo");
                
                startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ qua"));
            }
        });
    }

    /**
     * Share QR code (for own profile)
     */
    private void shareQRCode() {
        // Open MyQRCodeActivity for full QR experience
        android.content.Intent intent = new android.content.Intent(this, 
            com.example.doan_zaloclone.ui.qr.MyQRCodeActivity.class);
        startActivity(intent);
    }

    /**
     * Show block confirmation dialog
     */
    private void showBlockConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Chặn người dùng")
            .setMessage("Bạn có chắc muốn chặn người này? Họ sẽ không thể gửi tin nhắn hoặc gọi cho bạn.")
            .setPositiveButton("Chặn", (dialog, which) -> blockUser())
            .setNegativeButton("Hủy", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    /**
     * Block the user
     */
    private void blockUser() {
        if (userId == null) return;
        
        showProgress("Đang chặn...");
        
        // Call UserRepository to block user
        userRepository.blockUser(userId).observe(this, resource -> {
            hideProgress();
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        Toast.makeText(this, "Đã chặn người dùng", Toast.LENGTH_SHORT).show();
                        finish(); // Close profile after blocking
                        break;
                    case ERROR:
                        Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    /**
     * Show report dialog with reason selection
     */
    private void showReportDialog() {
        String[] reasons = {
            "Spam/Quảng cáo",
            "Nội dung không phù hợp",
            "Giả mạo",
            "Quấy rối",
            "Lý do khác"
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Báo xấu người dùng")
            .setItems(reasons, (dialog, which) -> {
                String selectedReason = reasons[which];
                
                if (selectedReason.equals("Lý do khác")) {
                    showReportInputDialog();
                } else {
                    submitReport(selectedReason);
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    /**
     * Show custom reason input dialog
     */
    private void showReportInputDialog() {
        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nhập lý do báo xấu");
        input.setMinLines(3);
        
        new AlertDialog.Builder(this)
            .setTitle("Lý do báo xấu")
            .setView(input)
            .setPositiveButton("Gửi", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                if (!reason.isEmpty()) {
                    submitReport(reason);
                } else {
                    Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    /**
     * Submit report to server
     */
    private void submitReport(String reason) {
        if (userId == null) return;
        
        showProgress("Đang gửi báo cáo...");
        
        // Call UserRepository to report user
        userRepository.reportUser(userId, reason).observe(this, resource -> {
            hideProgress();
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        new AlertDialog.Builder(this)
                            .setTitle("Báo cáo đã gửi")
                            .setMessage("Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét và xử lý trong thời gian sớm nhất.")
                            .setPositiveButton("OK", null)
                            .show();
                        break;
                    case ERROR:
                        Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void showDeleteFriendConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bạn bè")
                .setMessage("Bạn có chắc chắn muốn xóa người này khỏi danh sách bạn bè?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (currentUserId != null && userId != null) {
                        friendRepository.removeFriend(currentUserId, userId).observe(this, resource -> {
                            if (resource.isSuccess()) {
                                Toast.makeText(this, "Đã xóa bạn bè", Toast.LENGTH_SHORT).show();
                                areFriends = false;
                                updateAddFriendButtonVisibility();
                                setResult(RESULT_OK); // Notify caller to refresh
                                
                                // Notify via WebSocket if needed, though fragment should handle it.
                                // Actually, socket event will trigger fragment refresh.
                            } else {
                                Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupClickListeners() {
        if (isEditable) {
            // Allow clicking on name/bio to edit
            txtDisplayName.setOnClickListener(v -> showEditNameDialog());
            bioContainer.setOnClickListener(v -> showEditBioDialog());
            
            // Allow clicking images to edit
            avatarImage.setOnClickListener(v -> pickAvatarImage());
            coverImage.setOnClickListener(v -> pickCoverImage());
        }

        // Add friend button click listener
        if (btnAddFriend != null) {
             btnAddFriend.setOnClickListener(v -> sendFriendRequest());
        }
    }

    private void observeViewModel() {
        // Observe current user data
        viewModel.getCurrentUser().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                User user = resource.getData();
                if (user != null) {
                    updateUI(user);
                }
            } else if (resource.isError()) {
                Toast.makeText(this, "Lỗi tải thông tin: " + resource.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Observe update profile state
        viewModel.getUpdateProfileState().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isLoading()) {
                showProgress("Đang cập nhật...");
            } else {
                hideProgress();
                if (resource.isSuccess()) {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                } else if (resource.isError()) {
                    Toast.makeText(this, "Lỗi: " + resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observe upload image state
        viewModel.getUploadImageState().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isLoading()) {
                showProgress("Đang upload ảnh...");
            } else {
                hideProgress();
                if (resource.isSuccess()) {
                    Toast.makeText(this, "Upload thành công", Toast.LENGTH_SHORT).show();
                } else if (resource.isError()) {
                    Toast.makeText(this, "Lỗi upload: " + resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showEditNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile_name, null);

        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        editName.setText(txtDisplayName.getText());
        editName.setSelection(editName.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (!newName.isEmpty()) {
                viewModel.updateName(newName);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEditBioDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_bio, null);

        TextInputEditText editBio = dialogView.findViewById(R.id.editBio);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        String currentBio = txtBio.getText().toString();
        if (!currentBio.equals("Thêm giới thiệu bản thân")) {
            editBio.setText(currentBio);
            editBio.setSelection(editBio.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newBio = editBio.getText().toString().trim();
            viewModel.updateBio(newBio);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void pickAvatarImage() {
        if (checkStoragePermission()) {
            // Already have permission, launch picker directly
            avatarPicker.launch("image/*");
        } else {
            // Need permission, show dialog then request
            isWaitingForAvatarPick = true;
            requestStoragePermission();
        }
    }

    private void pickCoverImage() {
        if (checkStoragePermission()) {
            // Already have permission, launch picker directly
            coverPicker.launch("image/*");
        } else {
            // Need permission, show dialog then request
            isWaitingForCoverPick = true;
            requestStoragePermission();
        }
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - use READ_MEDIA_IMAGES
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 and below - use READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        // Determine which permission to request based on Android version
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        // Show explanation dialog before requesting permission
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cần quyền truy cập ảnh")
                .setMessage("Ứng dụng cần quyền truy cập thư viện ảnh để bạn có thể chọn ảnh làm avatar hoặc ảnh bìa.")
                .setPositiveButton("Cho phép", (dialog, which) -> {
                    permissionLauncher.launch(permission);
                })
                .setNegativeButton("Huỷ", (dialog, which) -> {
                    Toast.makeText(this, "Bạn cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void uploadAvatar(Uri imageUri) {
        viewModel.uploadAvatar(imageUri);
    }

    private void uploadCover(Uri imageUri) {
        viewModel.uploadCover(imageUri);
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void updateUI(User user) {
        if (user.getName() != null && !user.getName().isEmpty()) {
            txtDisplayName.setText(user.getName());
        } else {
            txtDisplayName.setText("Người dùng");
        }

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            txtBio.setText(user.getBio());
            txtBio.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            txtBio.setText("Thêm giới thiệu bản thân");
            txtBio.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_avatar)
                    .into(avatarImage);
        }

        if (user.getCoverUrl() != null && !user.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getCoverUrl())
                    .into(coverImage);
        }
    }

    /**
     * Check friendship status between current user and viewed user
     */
    private void checkFriendshipStatus() {
        if (currentUserId == null || userId == null) return;

        // First check if already friends
        friendRepository.checkFriendship(currentUserId, userId).observe(this, resource -> {
            if (resource != null && resource.isSuccess()) {
                areFriends = resource.getData() != null && resource.getData();
                
                if (!areFriends) {
                    // If not friends, check if there's a pending request
                    checkPendingFriendRequest();
                } else {
                    updateAddFriendButtonVisibility();
                }
            }
        });
    }

    /**
     * Check if current user has sent a pending friend request to viewed user
     */
    private void checkPendingFriendRequest() {
        if (currentUserId == null || userId == null) return;

        friendRepository.getSentFriendRequests(currentUserId).observe(this, resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                hasPendingRequest = false;
                for (com.example.doan_zaloclone.models.FriendRequest request : resource.getData()) {
                    if (userId.equals(request.getToUserId()) && 
                        "PENDING".equals(request.getStatus())) {
                        hasPendingRequest = true;
                        break;
                    }
                }
            }
            updateAddFriendButtonVisibility();
        });
    }

    /**
     * Update add friend button visibility based on friendship status
     */
    private void updateAddFriendButtonVisibility() {
        if (btnAddFriend == null) return;
        
        // Show button only if viewing another user's profile
        if (userId != null && !userId.equals(currentUserId)) {
            if (areFriends) {
                // Already friends - hide button
                btnAddFriend.setVisibility(View.GONE);
            } else if (hasPendingRequest) {
                // Request already sent - show as "Đã gửi lời mời"
                btnAddFriend.setVisibility(View.VISIBLE);
                btnAddFriend.setText("Đã gửi lời mời");
                btnAddFriend.setEnabled(false);
                btnAddFriend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.gray)));
            } else {
                // Not friends, no pending request - show "Kết bạn"
                btnAddFriend.setVisibility(View.VISIBLE);
                btnAddFriend.setText("Kết bạn");
                btnAddFriend.setEnabled(true);
                btnAddFriend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_blue)));
            }
        } else {
            // Own profile - hide button
            btnAddFriend.setVisibility(View.GONE);
        }
    }

    /**
     * Send friend request to the viewed user
     */
    private void sendFriendRequest() {
        if (currentUserId == null || userId == null) return;

        // Get current user's name for the friend request
        viewModel.getCurrentUser().observe(this, resource -> {
            if (resource != null && resource.isSuccess()) {
                User currentUser = resource.getData();
                if (currentUser != null) {
                    String currentUserName = currentUser.getName() != null ?
                            currentUser.getName() : "Người dùng";

                    // Disable button and show loading state
                    btnAddFriend.setEnabled(false);
                    btnAddFriend.setText("Đang gửi...");

                    friendRepository.sendFriendRequest(currentUserId, userId, currentUserName)
                            .observe(this, friendResource -> {
                                if (friendResource != null) {
                                    if (friendResource.isSuccess()) {
                                        Toast.makeText(this, "Đã gửi lời mời kết bạn",
                                                Toast.LENGTH_SHORT).show();
                                        // Update state
                                        hasPendingRequest = true;
                                        btnAddFriend.setText("Đã gửi lời mời");
                                        btnAddFriend.setEnabled(false);
                                        btnAddFriend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                            ContextCompat.getColor(this, R.color.gray)));
                                    } else if (friendResource.isError()) {
                                        Toast.makeText(this, "Lỗi: " + friendResource.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        btnAddFriend.setText("Kết bạn");
                                        btnAddFriend.setEnabled(true);
                                        btnAddFriend.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                            ContextCompat.getColor(this, R.color.primary_blue)));
                                    }
                                }
                            });
                }
            }
        });
    }
}
