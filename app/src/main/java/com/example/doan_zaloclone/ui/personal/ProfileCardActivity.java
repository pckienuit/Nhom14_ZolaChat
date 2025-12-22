package com.example.doan_zaloclone.ui.personal;

import android.Manifest;
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
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;
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
    private ProgressDialog progressDialog;
    private String userId;
    private boolean isEditable;
    
    // Views
    private Toolbar toolbar;
    private ImageView coverImage;
    private ImageView avatarImage;
    private ImageButton btnEditCover;
    private ImageButton btnEditAvatar;
    private TextView txtDisplayName;
    private TextView txtBio;
    private LinearLayout nameContainer;
    private LinearLayout bioContainer;
    
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
        
        // Default to current user if no userId provided
        if (userId == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                userId = auth.getCurrentUser().getUid();
                isEditable = true; // Can edit own profile
            }
        }
        
        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);
        
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
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        coverImage = findViewById(R.id.coverImage);
        avatarImage = findViewById(R.id.avatarImage);
        btnEditCover = findViewById(R.id.btnEditCover);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        txtDisplayName = findViewById(R.id.txtDisplayName);
        txtBio = findViewById(R.id.txtBio);
        nameContainer = findViewById(R.id.nameContainer);
        bioContainer = findViewById(R.id.bioContainer);
        
        // Hide edit buttons if not editable
        if (!isEditable) {
            btnEditCover.setVisibility(View.GONE);
            btnEditAvatar.setVisibility(View.GONE);
            nameContainer.setClickable(false);
            bioContainer.setClickable(false);
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Trang cá nhân");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupClickListeners() {
        if (isEditable) {
            nameContainer.setOnClickListener(v -> showEditNameDialog());
            bioContainer.setOnClickListener(v -> showEditBioDialog());
            btnEditAvatar.setOnClickListener(v -> pickAvatarImage());
            btnEditCover.setOnClickListener(v -> pickCoverImage());
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
            avatarPicker.launch("image/*");
        } else {
            isWaitingForAvatarPick = true;
            requestStoragePermission();
        }
    }
    
    private void pickCoverImage() {
        if (checkStoragePermission()) {
            coverPicker.launch("image/*");
        } else {
            isWaitingForCoverPick = true;
            requestStoragePermission();
        }
    }
    
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestStoragePermission() {
        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
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
}
