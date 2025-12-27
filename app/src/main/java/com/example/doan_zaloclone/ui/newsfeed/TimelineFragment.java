package com.example.doan_zaloclone.ui.newsfeed;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.activities.CreatePostActivity;
import com.example.doan_zaloclone.adapters.PostAdapter;
import com.example.doan_zaloclone.models.FireBasePost;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TimelineFragment extends Fragment {

    private RecyclerView rvTimeline;
    private SwipeRefreshLayout swipeRefresh;
    private PostAdapter postAdapter;
    private List<FireBasePost> postList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        rvTimeline = view.findViewById(R.id.rvTimeline);
        swipeRefresh = view.findViewById(R.id.swipeRefreshTimeline);
        
        db = FirebaseFirestore.getInstance();
        postList = new ArrayList<>();

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
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            FireBasePost post = document.toObject(FireBasePost.class);
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("TimelineFragment", "Error loading posts", e);
                    Toast.makeText(getContext(), "Không thể tải bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại bài viết khi quay lại màn hình này (ví dụ sau khi đăng bài xong)
        loadPosts();
    }
}
