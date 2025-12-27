package com.example.doan_zaloclone.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
// Import đúng đường dẫn Activity (Nếu báo đỏ xem Phần 2 bên dưới)
import com.example.doan_zaloclone.activities.CreatePostActivity;
import com.example.doan_zaloclone.models.FireBasePost;
import com.example.doan_zaloclone.adapters.PostAdapter;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NewsFeedFragment extends Fragment {

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private List<FireBasePost> postList;
    private FirebaseFirestore db;
    private LinearLayout layoutPostInput;

    private ListenerRegistration postListener;

    public NewsFeedFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rvPosts = view.findViewById(R.id.rvPosts);
        layoutPostInput = view.findViewById(R.id.layoutPostInput);

        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        postList = new ArrayList<>();

        // Khởi tạo Adapter
        postAdapter = new PostAdapter(requireContext(), postList);
        rvPosts.setAdapter(postAdapter);

        if (layoutPostInput != null) {
            layoutPostInput.setOnClickListener(v -> {
                // Đã sửa lỗi: Gọi trực tiếp class, không cần viết dài dòng package
                Intent intent = new Intent(requireContext(), CreatePostActivity.class);
                startActivity(intent);
            });
        }

        loadPostsFromFirebase();
    }

    private void loadPostsFromFirebase() {
        postListener = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        postList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                FireBasePost post = doc.toObject(FireBasePost.class);
                                if (post != null) {
                                    postList.add(post);
                                }
                            } catch (Exception e) {
                                Log.e("NewsFeed", "Lỗi convert data: " + e.getMessage());
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) {
            postListener.remove();
        }
    }
}

