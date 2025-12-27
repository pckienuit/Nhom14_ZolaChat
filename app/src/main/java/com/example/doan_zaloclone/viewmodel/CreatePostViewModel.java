package com.example.doan_zaloclone.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.doan_zaloclone.data.repository.PostRepository;
import com.example.doan_zaloclone.models.Post;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CreatePostViewModel extends ViewModel {

    private final PostRepository postRepository;

    public CreatePostViewModel() {
        this.postRepository = PostRepository.getInstance();
    }

    public LiveData<Resource<Void>> createPost(String content) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // NOTE: In a real app, you should fetch the user's name and avatar URL from your user profile data
        String userName = currentUser != null ? currentUser.getDisplayName() : "Anonymous";
        String userAvatar = currentUser != null && currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";

        Post newPost = new Post(currentUser.getUid(), userName, userAvatar, content);
        return postRepository.createPost(newPost);
    }
}
