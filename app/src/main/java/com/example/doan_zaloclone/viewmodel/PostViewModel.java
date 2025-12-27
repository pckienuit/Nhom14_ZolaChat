package com.example.doan_zaloclone.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.doan_zaloclone.models.FireBasePost;
import com.example.doan_zaloclone.repository.PostRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

public class PostViewModel extends ViewModel {

    private final PostRepository repository;

    public PostViewModel() {
        this.repository = new PostRepository();
    }

    public LiveData<Resource<Boolean>> createPost(String content) {
        return repository.createPost(content);
    }

    public LiveData<Resource<List<FireBasePost>>> getPosts() {
        return repository.getPosts();
    }
}
