package com.example.doan_zaloclone.ui.newsfeed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.doan_zaloclone.R;
import java.util.ArrayList;

public class TimelineFragment extends Fragment {

    private RecyclerView rvTimeline;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        rvTimeline = view.findViewById(R.id.rvTimeline);
        swipeRefresh = view.findViewById(R.id.swipeRefreshTimeline);

        setupRecyclerView();

        // Xử lý sự kiện kéo để làm mới
        swipeRefresh.setOnRefreshListener(() -> {
            // Giả lập tải dữ liệu mới
            swipeRefresh.setRefreshing(false);
        });

        // Xử lý sự kiện khi nhấn vào "Hôm nay bạn thế nào?"
        view.findViewById(R.id.layoutCreatePost).setOnClickListener(v -> {
            // Mở màn hình đăng bài
        });

        return view;
    }

    private void setupRecyclerView() {
        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        // NewsfeedAdapter adapter = new NewsfeedAdapter(getData());
        // rvTimeline.setAdapter(adapter);
    }
}
