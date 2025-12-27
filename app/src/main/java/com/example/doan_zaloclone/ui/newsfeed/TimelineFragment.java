package com.example.doan_zaloclone.ui.newsfeed;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.activities.CreatePostActivity;
import com.example.doan_zaloclone.adapters.PostAdapter;
import com.example.doan_zaloclone.models.FireBasePost;
import com.example.doan_zaloclone.viewmodel.PostViewModel;

import java.util.ArrayList;
import java.util.List;

public class TimelineFragment extends Fragment {

    private RecyclerView rvTimeline;
    private SwipeRefreshLayout swipeRefresh;
    private PostAdapter postAdapter;
    private List<FireBasePost> postList;
    private PostViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        rvTimeline = view.findViewById(R.id.rvTimeline);
        swipeRefresh = view.findViewById(R.id.swipeRefreshTimeline);
        
        postList = new ArrayList<>();
        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

        setupRecyclerView();
        loadPosts();

        // Xử lý sự kiện kéo để làm mới
        swipeRefresh.setOnRefreshListener(this::loadPosts);

        // Xử lý sự kiện khi nhấn vào "Hôm nay bạn thế nào?"
        view.findViewById(R.id.layoutCreatePost).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void setupRecyclerView() {
        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(getContext(), postList);
        rvTimeline.setAdapter(postAdapter);
    }

    private void loadPosts() {
        swipeRefresh.setRefreshing(true);
        viewModel.getPosts().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == com.example.doan_zaloclone.utils.Status.SUCCESS) {
                postList.clear();
                if (resource.data != null) {
                    postList.addAll(resource.data);
                }
                postAdapter.notifyDataSetChanged();
            } else if (resource.status == com.example.doan_zaloclone.utils.Status.ERROR) {
                Toast.makeText(getContext(), "Không thể tải bài viết: " + resource.message, Toast.LENGTH_SHORT).show();
            }
            swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại bài viết khi quay lại màn hình này
        loadPosts();
    }
}
