package com.example.doan_zaloclone.ui.personal;

import android.content.Intent;
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
 * Displays compact user card and menu items
 */
public class PersonalFragment extends Fragment {

    private PersonalViewModel viewModel;

    // Views
    private LinearLayout compactUserCard;
    private ImageView avatarImage;
    private TextView txtDisplayName;
    private ImageView btnSettings;

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
        setupClickListeners();
        setupMenuItems();
        observeViewModel();

        // Load current user's profile
        viewModel.loadCurrentUser();
    }

    private void initializeViews(View view) {
        // Compact user card views
        compactUserCard = view.findViewById(R.id.compactUserCard);
        avatarImage = view.findViewById(R.id.avatarImage);
        txtDisplayName = view.findViewById(R.id.txtDisplayName);
        btnSettings = view.findViewById(R.id.btn_settings);

        // Menu views
        menuQrWallet = view.findViewById(R.id.menuQrWallet);
        menuSecurity = view.findViewById(R.id.menuSecurity);
        menuPrivacy = view.findViewById(R.id.menuPrivacy);
        menuCloud = view.findViewById(R.id.menuCloud);
    }

    private void setupClickListeners() {
        // Click compact user card to open full profile
        compactUserCard.setOnClickListener(v -> openProfileCard());
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                 Toast.makeText(getContext(), "Cài đặt & Quyền riêng tư", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupMenuItems() {
        // Cloud của tôi
        setupMenuItem(menuCloud, R.drawable.ic_cloud_blue,
                "Cloud của tôi", "Lưu trữ các tin nhắn quan trọng",
                new MyCloudFragment());

        // QR Wallet
        setupMenuItem(menuQrWallet, R.drawable.ic_qr_wallet_blue,
                "Ví QR", "Lưu trữ và xuất trình các mã QR quan trọng",
                new QrWalletFragment());

        // Account & Security
        setupMenuItem(menuSecurity, R.drawable.ic_security_blue,
                "Tài khoản và bảo mật", "",
                new SecurityFragment());

        // Privacy
        setupMenuItem(menuPrivacy, R.drawable.ic_privacy_blue,
                "Quyền riêng tư", "",
                new PrivacyFragment());
    }

    private void setupMenuItem(View menuView, int iconRes, String title, String description, Fragment targetFragment) {
        if (menuView == null) return;
        
        ImageView icon = menuView.findViewById(R.id.menuIcon);
        TextView titleView = menuView.findViewById(R.id.menuTitle);
        TextView descView = menuView.findViewById(R.id.menuDescription);

        icon.setImageResource(iconRes);
        titleView.setText(title);
        
        if (description != null && !description.isEmpty()) {
            descView.setText(description);
            descView.setVisibility(View.VISIBLE);
        } else {
            descView.setVisibility(View.GONE);
        }

        // Navigate to placeholder fragment
        menuView.setOnClickListener(v -> {
             if (targetFragment != null) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, targetFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                 Toast.makeText(getContext(), title + " - Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
            }
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
    }

    private void openProfileCard() {
        Intent intent = new Intent(requireContext(), ProfileCardActivity.class);
        // Don't need to pass userId, ProfileCardActivity will use current user
        intent.putExtra(ProfileCardActivity.EXTRA_IS_EDITABLE, true);
        startActivity(intent);
    }

    private void updateUI(User user) {
        // Update name
        if (user.getName() != null && !user.getName().isEmpty()) {
            txtDisplayName.setText(user.getName());
        } else {
            txtDisplayName.setText("Người dùng");
        }

        // Update avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_avatar)
                    .into(avatarImage);
        }
    }
}
