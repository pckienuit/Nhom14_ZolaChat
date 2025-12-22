package com.example.doan_zaloclone.ui.newsfeed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideoShortsFragment extends Fragment {
    private RecyclerView rvVideoShorts;
    private VideoAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_shorts, container, false);

        rvVideoShorts = view.findViewById(R.id.rvVideoShorts);
        // Thiết lập 2 cột
        rvVideoShorts.setLayoutManager(new androidx.leanback.widget.GridLayoutManager(getContext(), 2));

        // Dữ liệu mẫu ID video Shorts
        List<String> videoIds = Arrays.asList("dQw4w9WgXcQ", "3JZ_D3ELwOQ", "jNQXAC9IVRw");

        adapter = new VideoAdapter(videoIds);
        rvVideoShorts.setAdapter(adapter);

        return view;
    }
}
