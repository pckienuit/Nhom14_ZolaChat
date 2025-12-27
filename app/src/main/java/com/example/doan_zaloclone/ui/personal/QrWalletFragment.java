package com.example.doan_zaloclone.ui.personal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.ui.qr.MyQRCodeActivity;
import com.example.doan_zaloclone.ui.qr.QRScanActivity;
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QrWalletFragment - QR Wallet feature
 * Displays user's personal QR code and scan option
 */
public class QrWalletFragment extends Fragment {

    private static final String QR_PREFIX = "zalo://user/";
    
    private ImageView ivMyQrCode;
    private ImageView ivUserAvatar;
    private TextView tvUserName;
    private CardView cardMyQr;
    private CardView cardScanQr;
    
    private PersonalViewModel viewModel;
    private String currentUserId;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_wallet, container, false);

        // Get current user ID
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }

        initViews(view);
        setupToolbar(view);
        setupClickListeners();
        
        viewModel = new ViewModelProvider(requireActivity()).get(PersonalViewModel.class);
        observeViewModel();
        
        // Load user data
        viewModel.loadCurrentUser();
        
        // Generate QR preview
        generateQRPreview();

        return view;
    }

    private void initViews(View view) {
        ivMyQrCode = view.findViewById(R.id.ivMyQrCode);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        cardMyQr = view.findViewById(R.id.cardMyQr);
        cardScanQr = view.findViewById(R.id.cardScanQr);
        
        // Button actions
        view.findViewById(R.id.btnShareQr).setOnClickListener(v -> openMyQRActivity());
        view.findViewById(R.id.btnSaveQr).setOnClickListener(v -> openMyQRActivity());
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupClickListeners() {
        // Open full QR screen
        cardMyQr.setOnClickListener(v -> openMyQRActivity());
        
        // Open QR scanner
        cardScanQr.setOnClickListener(v -> openQRScanner());
    }

    private void observeViewModel() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess() && resource.getData() != null) {
                currentUser = resource.getData();
                updateUI(currentUser);
            }
        });
    }

    private void updateUI(User user) {
        // Update name
        if (user.getName() != null && !user.getName().isEmpty()) {
            tvUserName.setText(user.getName());
        } else {
            tvUserName.setText("Người dùng");
        }

        // Update avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty() && isAdded()) {
            Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar)
                .circleCrop()
                .into(ivUserAvatar);
        }
    }

    private void generateQRPreview() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        String qrContent = QR_PREFIX + currentUserId;
        
        try {
            QRCodeWriter writer = new QRCodeWriter();
            int size = 256; // Smaller size for preview
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size);

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            
            int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
            int whiteColor = ContextCompat.getColor(requireContext(), R.color.white);
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? primaryColor : whiteColor);
                }
            }

            ivMyQrCode.setImageBitmap(bitmap);
            
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void openMyQRActivity() {
        Intent intent = new Intent(requireContext(), MyQRCodeActivity.class);
        startActivity(intent);
    }

    private void openQRScanner() {
        Intent intent = new Intent(requireContext(), QRScanActivity.class);
        startActivity(intent);
    }
}
