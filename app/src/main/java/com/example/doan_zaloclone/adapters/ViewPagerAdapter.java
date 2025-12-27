package com.example.doan_zaloclone.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.doan_zaloclone.fragments.PostsFragment;
import com.example.doan_zaloclone.ui.newsfeed.VideoShortsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PostsFragment();
            case 1:
                return new VideoShortsFragment();
            default:
                return new PostsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Chúng ta có 2 tab
    }
}
