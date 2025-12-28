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
import com.example.doan_zaloclone.activities.CreatePostActivity;
import com.example.doan_zaloclone.activities.CommentsActivity;
import com.example.doan_zaloclone.models.Post;
import com.example.doan_zaloclone.adapters.PostAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostsFragment extends Fragment implements PostAdapter.OnPostInteractionListener {

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private LinearLayout layoutPostInput;
    private ListenerRegistration postListener;

    public PostsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng layout cũ của NewsFeedFragment, giờ là layout của PostsFragment
        return inflater.inflate(R.layout.fragment_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rvPosts = view.findViewById(R.id.rvPosts);
        layoutPostInput = view.findViewById(R.id.layoutPostInput);

        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        postList = new ArrayList<>();

        postAdapter = new PostAdapter(requireContext(), postList, this);
        rvPosts.setAdapter(postAdapter);

        if (layoutPostInput != null) {
            layoutPostInput.setOnClickListener(v -> {
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
                                Post post = doc.toObject(Post.class);
                                if (post != null) {
                                    postList.add(post);
                                }
                            } catch (Exception e) {
                                Log.e("PostsFragment", "Lỗi convert data: " + e.getMessage());
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onLikeClick(String postId) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (postId == null || currentUserId == null) return;

        db.collection("posts").document(postId).get().addOnSuccessListener(documentSnapshot -> {
            Post post = documentSnapshot.toObject(Post.class);
            if (post != null) {
                Map<String, Boolean> likes = post.getLikes();
                if (likes.containsKey(currentUserId)) {
                    db.collection("posts").document(postId).update("likes."+currentUserId, FieldValue.delete());
                } else {
                    db.collection("posts").document(postId).update("likes."+currentUserId, true);
                }
            }
        });
    }

    @Override
    public void onCommentClick(String postId) {
        Intent intent = new Intent(requireContext(), CommentsActivity.class);
        intent.putExtra(CommentsActivity.EXTRA_POST_ID, postId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) {
            postListener.remove();
        }
    }
}
