package com.example.doan_zaloclone.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.doan_zaloclone.models.Comment;
import com.example.doan_zaloclone.models.Post;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PostRepository {

    private static PostRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

    public static PostRepository getInstance() {
        if (instance == null) {
            instance = new PostRepository();
        }
        return instance;
    }

    public LiveData<Resource<Void>> createPost(Post post) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        String postId = db.collection("posts").document().getId();
        post.setPostId(postId);
        db.collection("posts").document(postId).set(post)
            .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
        return result;
    }

    public LiveData<Resource<List<Post>>> getPosts() {
        MutableLiveData<Resource<List<Post>>> data = new MutableLiveData<>();
        db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    data.setValue(Resource.error(e.getMessage(), null));
                    return;
                }
                List<Post> posts = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    posts.add(doc.toObject(Post.class));
                }
                data.setValue(Resource.success(posts));
            });
        return data;
    }

    public void toggleLike(String postId) {
        if (currentUserId == null) return;
        db.collection("posts").document(postId).get().addOnSuccessListener(documentSnapshot -> {
            Post post = documentSnapshot.toObject(Post.class);
            if (post != null && post.getLikes() != null) {
                if (post.getLikes().containsKey(currentUserId)) {
                    db.collection("posts").document(postId).update("likes." + currentUserId, FieldValue.delete());
                } else {
                    db.collection("posts").document(postId).update("likes." + currentUserId, true);
                }
            }
        });
    }

    public LiveData<Resource<List<Comment>>> getComments(String postId) {
        MutableLiveData<Resource<List<Comment>>> data = new MutableLiveData<>();
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    data.setValue(Resource.error(e.getMessage(), null));
                    return;
                }
                List<Comment> comments = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    comments.add(doc.toObject(Comment.class));
                }
                data.setValue(Resource.success(comments));
            });
        return data;
    }

    public LiveData<Resource<Boolean>> sendComment(String postId, String content) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        if (currentUserId == null) {
            result.setValue(Resource.error("User not logged in", null));
            return result;
        }

        // Lấy thông tin người dùng hiện tại từ Firestore
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            String userName = "Người dùng Zalo"; // Tên mặc định
            String userAvatar = ""; // Avatar mặc định

            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    userName = user.getName();
                    userAvatar = user.getAvatarUrl();
                }
            }

            // Tạo đối tượng Comment với thông tin người dùng thực tế
            String commentId = db.collection("posts").document(postId).collection("comments").document().getId();
            Comment comment = new Comment(commentId, postId, currentUserId, userName, userAvatar, content);
            
            // Lưu comment vào sub-collection
            db.collection("posts").document(postId).collection("comments").document(commentId).set(comment)
                .addOnSuccessListener(aVoid -> {
                    // Tăng số lượng comment trong document Post
                    db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1));
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
        }).addOnFailureListener(e -> {
            result.setValue(Resource.error("Không thể lấy thông tin người dùng: " + e.getMessage(), null));
        });

        return result;
    }
}
