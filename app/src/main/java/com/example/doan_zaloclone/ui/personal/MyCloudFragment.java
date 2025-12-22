package com.example.doan_zaloclone.ui.personal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.doan_zaloclone.R;

/**
 * MyCloudFragment - Placeholder for Cloud storage feature
 */
public class MyCloudFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        
        // Customize placeholder
        ImageView icon = view.findViewById(R.id.placeholderIcon);
        TextView title = view.findViewById(R.id.placeholderTitle);
        TextView description = view.findViewById(R.id.placeholderDescription);
        
        icon.setImageResource(R.drawable.ic_cloud);
        title.setText("Cloud của tôi");
        description.setText("Lưu trữ các tin nhắn và file quan trọng\n\nTính năng đang được phát triển");
        
        return view;
    }
}
