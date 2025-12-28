package com.example.doan_zaloclone.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.doan_zaloclone.models.Comment;
import com.example.doan_zaloclone.models.Post;
import com.example.doan_zaloclone.repository.PostRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

public class PostViewModel extends ViewModel {

    private final PostRepository repository;

    public PostViewModel() {
        this.repository = PostRepository.getInstance();
    }

    // Sửa lại để nhận vào một đối tượng Post
    public LiveData<Resource<Void>> createPost(Post post) {
        return repository.createPost(post);
    }

    // Lấy danh sách bài đăng
    public LiveData<Resource<List<Post>>> getPosts() {
        return repository.getPosts();
    }

    // Xử lý like
    public void toggleLike(String postId) {
        repository.toggleLike(postId);
    }

    // Gửi comment
    public LiveData<Resource<Boolean>> sendComment(String postId, String content) {
        return repository.sendComment(postId, content);
    }

    // Lấy danh sách comment
    public LiveData<Resource<List<Comment>>> getComments(String postId) {
        return repository.getComments(postId);
    }
}
