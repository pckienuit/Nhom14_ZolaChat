package com.example.doan_zaloclone.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Post;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.viewmodel.PostViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etContent;
    private Button btnDang;
    private PostViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

        etContent = findViewById(R.id.etPostContent);
        btnDang = findViewById(R.id.btnPost);

        btnDang.setOnClickListener(v -> handlePost());
    }

    @SuppressLint("SetTextI18n")
    private void handlePost() {
        String content = etContent.getText().toString().trim();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Bạn chưa nhập nội dung!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để đăng bài", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDang.setEnabled(false);
        btnDang.setText("Đang đăng...");

        // Lấy thông tin người dùng và tạo đối tượng Post
        String userId = currentUser.getUid();
        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng Zalo";
        String userAvatar = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";

        Post newPost = new Post(userId, userName, userAvatar, content);

        viewModel.createPost(newPost).observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(CreatePostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(CreatePostActivity.this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                btnDang.setEnabled(true);
                btnDang.setText("Đăng bài");
            }
        });
    }
}
