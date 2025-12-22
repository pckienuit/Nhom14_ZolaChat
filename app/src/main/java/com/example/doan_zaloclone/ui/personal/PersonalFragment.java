package com.example.doan_zaloclone.ui.personal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;

/**
 * PersonalFragment - Main fragment for Personal tab
 * Displays user profile card and menu items
 */
public class PersonalFragment extends Fragment {
    
    private PersonalViewModel viewModel;
    
    // Views
    private ImageView coverImage;
    private ImageView avatarImage;
    private TextView txtDisplayName;
    private TextView txtBio;
    private LinearLayout nameContainer;
    private LinearLayout bioContainer;
    
    // Menu items
    private View menuQrWallet;
    private View menuSecurity;
    private View menuPrivacy;
    private View menuCloud;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);
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
        setupMenuItems();
        observeViewModel();
    }
    
    private void initializeViews(View view) {
        // User card views
        coverImage = view.findViewById(R.id.coverImage);
        avatarImage = view.findViewById(R.id.avatarImage);
        txtDisplayName = view.findViewById(R.id.txtDisplayName);
        txtBio = view.findViewById(R.id.txtBio);
        nameContainer = view.findViewById(R.id.nameContainer);
        bioContainer = view.findViewById(R.id.bioContainer);
        
        // Menu views
        menuQrWallet = view.findViewById(R.id.menuQrWallet);
        menuSecurity = view.findViewById(R.id.menuSecurity);
        menuPrivacy = view.findViewById(R.id.menuPrivacy);
        menuCloud = view.findViewById(R.id.menuCloud);
        
        // TODO: Phase 5 - Setup edit buttons for avatar and cover
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
        
        // TODO: Phase 5 - Observe update and upload states
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
