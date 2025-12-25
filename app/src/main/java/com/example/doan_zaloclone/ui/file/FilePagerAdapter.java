package com.example.doan_zaloclone.ui.file;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.doan_zaloclone.models.FileCategory;

/**
 * ViewPager adapter for file management tabs
 */
public class FilePagerAdapter extends FragmentStateAdapter {

    private final String conversationId;

    public FilePagerAdapter(@NonNull FragmentActivity fragmentActivity, String conversationId) {
        super(fragmentActivity);
        this.conversationId = conversationId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        FileCategory category = FileCategory.fromPosition(position);
        return FileListFragment.newInstance(category, conversationId);
    }

    @Override
    public int getItemCount() {
        return FileCategory.values().length;
    }
}
