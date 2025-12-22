package com.example.doan_zaloclone.ui.personal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;
import com.google.android.material.textfield.TextInputEditText;

/**
 * PersonalFragment - Main fragment for Personal tab
 * Displays user profile card and menu items
 */
public class PersonalFragment extends Fragment {
    
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private PersonalViewModel viewModel;
    private ProgressDialog progressDialog;
    
    // Views
    private ImageView coverImage;
    private ImageView avatarImage;
    private ImageButton btnEditCover;
    private ImageButton btnEditAvatar;
    private TextView txtDisplayName;
    private TextView txtBio;
    private LinearLayout nameContainer;
    private LinearLayout bioContainer;
    
    // Menu items
    private View menuQrWallet;
    private View menuSecurity;
    private View menuPrivacy;
    private View menuCloud;
    
    // Image pickers
    private ActivityResultLauncher<String> avatarPicker;
    private ActivityResultLauncher<String> coverPicker;
    private ActivityResultLauncher<String> permissionLauncher;
    
    private boolean isWaitingForAvatarPick = false;
    private boolean isWaitingForCoverPick = false;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);
        
        // Setup image pickers
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
        
        // Setup permission launcher
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
                    Toast.makeText(requireContext(), 
                        "Cần cấp quyền truy cập ảnh", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupClickListeners();
        setupMenuItems();
        observeViewModel();
    }
    
    private void initializeViews(View view) {
        // User card views
        coverImage = view.findViewById(R.id.coverImage);
        avatarImage = view.findViewById(R.id.avatarImage);
        btnEditCover = view.findViewById(R.id.btnEditCover);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        txtDisplayName = view.findViewById(R.id.txtDisplayName);
        txtBio = view.findViewById(R.id.txtBio);
        nameContainer = view.findViewById(R.id.nameContainer);
        bioContainer = view.findViewById(R.id.bioContainer);
        
        // Menu views
        menuQrWallet = view.findViewById(R.id.menuQrWallet);
        menuSecurity = view.findViewById(R.id.menuSecurity);
        menuPrivacy = view.findViewById(R.id.menuPrivacy);
        menuCloud = view.findViewById(R.id.menuCloud);
    }
    
    private void setupClickListeners() {
        // Edit name
        nameContainer.setOnClickListener(v -> showEditNameDialog());
        
        // Edit bio
        bioContainer.setOnClickListener(v -> showEditBioDialog());
        
        // Edit avatar
        btnEditAvatar.setOnClickListener(v -> pickAvatarImage());
        
        // Edit cover
        btnEditCover.setOnClickListener(v -> pickCoverImage());
    }
    
    private void setupMenuItems() {
        // QR Wallet
        setupMenuItem(menuQrWallet, R.drawable.ic_qr_code, 
            "Ví QR", "Lưu trữ và xuất trình các mã QR quan trọng",
            new QrWalletFragment());
        
        // Account & Security
        setupMenuItem(menuSecurity, R.drawable.ic_security, 
            "Tài khoản và bảo mật", "Quản lý thông tin tài khoản",
            new SecurityFragment());
        
        // Privacy
        setupMenuItem(menuPrivacy, R.drawable.ic_privacy, 
            "Quyền riêng tư", "Cài đặt quyền riêng tư",
            new PrivacyFragment());
        
        // My Cloud
        setupMenuItem(menuCloud, R.drawable.ic_cloud, 
            "Cloud của tôi", "Lưu trữ các tin nhắn quan trọng",
            new MyCloudFragment());
    }
    
    private void setupMenuItem(View menuView, int iconRes, String title, String description, Fragment targetFragment) {
        ImageView icon = menuView.findViewById(R.id.menuIcon);
        TextView titleView = menuView.findViewById(R.id.menuTitle);
        TextView descView = menuView.findViewById(R.id.menuDescription);
        
        icon.setImageResource(iconRes);
        titleView.setText(title);
        descView.setText(description);
        
        // Navigate to placeholder fragment
        menuView.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, targetFragment)
                .addToBackStack(null)
                .commit();
        });
    }
    
    private void observeViewModel() {
        // Observe current user data
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess()) {
                User user = resource.getData();
                if (user != null) {
                    updateUI(user);
                }
            } else if (resource.isError()) {
                Toast.makeText(requireContext(), 
                    "Lỗi tải thông tin: " + resource.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe update profile state
        viewModel.getUpdateProfileState().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            
            if (resource.isLoading()) {
                showProgress("Đang cập nhật...");
            } else {
                hideProgress();
                if (resource.isSuccess()) {
                    Toast.makeText(requireContext(), 
                        "Cập nhật thành công", 
                        Toast.LENGTH_SHORT).show();
                } else if (resource.isError()) {
                    Toast.makeText(requireContext(), 
                        "Lỗi: " + resource.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Observe upload image state
        viewModel.getUploadImageState().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            
            if (resource.isLoading()) {
                showProgress("Đang upload ảnh...");
            } else {
                hideProgress();
                if (resource.isSuccess()) {
                    Toast.makeText(requireContext(), 
                        "Upload thành công", 
                        Toast.LENGTH_SHORT).show();
                } else if (resource.isError()) {
                    Toast.makeText(requireContext(), 
                        "Lỗi upload: " + resource.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void showEditNameDialog() {
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile_name, null);
        
        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Set current name
        editName.setText(txtDisplayName.getText());
        editName.setSelection(editName.getText().length());
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (!newName.isEmpty()) {
                viewModel.updateName(newName);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), 
                    "Tên không được để trống", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void showEditBioDialog() {
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_bio, null);
        
        TextInputEditText editBio = dialogView.findViewById(R.id.editBio);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Set current bio (if not placeholder)
        String currentBio = txtBio.getText().toString();
        if (!currentBio.equals("Thêm giới thiệu bản thân")) {
            editBio.setText(currentBio);
            editBio.setSelection(editBio.getText().length());
        }
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestStoragePermission() {
        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
    }
    
    private void uploadAvatar(Uri imageUri) {
        viewModel.uploadAvatar(imageUri);
    }
    
    private void uploadCover(Uri imageUri) {
        viewModel.uploadCover(imageUri);
    }
    
    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
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
        // Update name
        if (user.getName() != null && !user.getName().isEmpty()) {
            txtDisplayName.setText(user.getName());
        } else {
            txtDisplayName.setText("Người dùng");
        }
        
        // Update bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            txtBio.setText(user.getBio());
            txtBio.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            txtBio.setText("Thêm giới thiệu bản thân");
            txtBio.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        
        // Update avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar)
                .into(avatarImage);
        }
        
        // Update cover
        if (user.getCoverUrl() != null && !user.getCoverUrl().isEmpty()) {
            Glide.with(this)
                .load(user.getCoverUrl())
                .into(coverImage);
        }
    }
}
