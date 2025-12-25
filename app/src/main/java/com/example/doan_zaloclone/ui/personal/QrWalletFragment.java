package com.example.doan_zaloclone.ui.personal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.doan_zaloclone.R;

/**
 * QrWalletFragment - Placeholder for QR Wallet feature
 */
public class QrWalletFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);

        // Setup toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("Ví QR");
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Customize placeholder
        ImageView icon = view.findViewById(R.id.placeholderIcon);
        TextView title = view.findViewById(R.id.placeholderTitle);
        TextView description = view.findViewById(R.id.placeholderDescription);

        icon.setImageResource(R.drawable.ic_qr_code);
        title.setText("Ví QR");
        description.setText("Lưu trữ và xuất trình các mã QR quan trọng\n\nTính năng đang được phát triển");

        return view;
    }
}
