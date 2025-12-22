package com.example.doan_zaloclone.ui.newsfeed;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class NewsfeedPagerAdapter extends FragmentStateAdapter {
    public NewsfeedPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new VideoShortsFragment(); // Tab Video
        }
        return new TimelineFragment(); // Tab Bảng tin
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
