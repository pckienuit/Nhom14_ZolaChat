package com.example.doan_zaloclone.repository;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.FireBasePost;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public PostRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Create a new post (text only)
     */
    public LiveData<Resource<Boolean>> createPost(@NonNull String content) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous_id";
        String userName = auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null 
                          ? auth.getCurrentUser().getDisplayName() : "Người dùng ẩn danh";
        String userAvatar = auth.getCurrentUser() != null && auth.getCurrentUser().getPhotoUrl() != null 
                            ? auth.getCurrentUser().getPhotoUrl().toString() : "";

        String postId = db.collection("posts").document().getId();
        long timestamp = System.currentTimeMillis();

        FireBasePost newPost = new FireBasePost(
                postId,
                userId,
                userName,
                userAvatar,
                content,
                null, // Image URL always null for text-only posts
                timestamp
        );

        db.collection("posts").document(postId)
                .set(newPost)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(true)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage())));

        return result;
    }

    /**
     * Get all posts ordered by timestamp descending
     */
    public LiveData<Resource<List<FireBasePost>>> getPosts() {
        MutableLiveData<Resource<List<FireBasePost>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FireBasePost> posts = new ArrayList<>();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            posts.add(document.toObject(FireBasePost.class));
                        }
                    }
                    result.setValue(Resource.success(posts));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage())));

        return result;
    }
}
