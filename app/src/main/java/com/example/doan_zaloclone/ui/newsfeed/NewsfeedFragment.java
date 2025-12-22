package com.example.doan_zaloclone.ui.newsfeed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.viewmodel.NewsfeedViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class NewsfeedFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private NewsfeedViewModel newsfeedViewModel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newsfeed, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutNewsfeed);
        viewPager = view.findViewById(R.id.viewPagerNewsfeed);

        NewsfeedPagerAdapter adapter = new NewsfeedPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Kết nối Tab với ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("BẢNG TIN");
            else tab.setText("VIDEO");
        }).attach();

        // Khởi tạo ViewModel
        newsfeedViewModel = new ViewModelProvider(this).get(NewsfeedViewModel.class);

        return view;
    }
}

