package com.example.doan_zaloclone.ui.contact;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.contact.tabs.FriendRequestReceivedFragment;
import com.example.doan_zaloclone.ui.contact.tabs.FriendRequestSentFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class FriendRequestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnAddFriend = findViewById(R.id.btnAddFriend);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        btnBack.setOnClickListener(v -> finish());
        
        // Navigate to Add Friend activity
        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddFriendActivity.class);
            startActivity(intent);
        });

        FriendRequestsPagerAdapter adapter = new FriendRequestsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Đã nhận");
            } else {
                tab.setText("Đã gửi");
            }
        }).attach();
    }

    private static class FriendRequestsPagerAdapter extends FragmentStateAdapter {

        public FriendRequestsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new FriendRequestReceivedFragment();
            } else {
                return new FriendRequestSentFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
