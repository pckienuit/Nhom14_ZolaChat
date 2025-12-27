package com.example.doan_zaloclone.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.doan_zaloclone.models.Post;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostRepository {

    private static PostRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static PostRepository getInstance() {
        if (instance == null) {
            instance = new PostRepository();
        }
        return instance;
    }

    public LiveData<Resource<Void>> createPost(Post post) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // Tạo một ID mới cho bài đăng
        String postId = db.collection("posts").document().getId();
        post.setPostId(postId);

        db.collection("posts").document(postId).set(post)
            .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
