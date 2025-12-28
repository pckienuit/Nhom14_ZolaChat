package com.example.doan_zaloclone.ui.newsfeed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.BuildConfig;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.VideoItem;
import com.example.doan_zaloclone.viewmodel.VideoShortsViewModel;

import java.util.ArrayList;

public class VideoShortsFragment extends Fragment {
    private RecyclerView rvVideoShorts;
    private VideoAdapter adapter;
    private VideoShortsViewModel viewModel;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_shorts, container, false);

        rvVideoShorts = view.findViewById(R.id.rvVideoShorts);
        progressBar = view.findViewById(R.id.progressBar);

        setupRecyclerView();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(VideoShortsViewModel.class);

        observeViewModel();
        searchForShorts();
    }

    private void setupRecyclerView() {
        rvVideoShorts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new VideoAdapter(new ArrayList<>());
        rvVideoShorts.setAdapter(adapter);
    }

    private void observeViewModel() {
        // The search query is now passed here, and the API key is handled in the repository
        viewModel.searchShorts("vietnam travel").observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    rvVideoShorts.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    rvVideoShorts.setVisibility(View.VISIBLE);
                    if (resource.getData() != null && !resource.getData().isEmpty()) {
                        adapter.updateVideos(resource.getData());
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy video nào", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void searchForShorts() {
        // The observer in observeViewModel will be triggered automatically when the fragment is created.
        // You can add a SwipeRefreshLayout to allow the user to trigger a new search.
    }
}
