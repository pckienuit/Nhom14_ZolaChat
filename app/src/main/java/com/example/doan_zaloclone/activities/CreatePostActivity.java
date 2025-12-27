package com.example.doan_zaloclone.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FireBasePost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etContent;
    private Button btnDang;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 2. Ánh xạ View
        etContent = findViewById(R.id.etPostContent);
        btnDang = findViewById(R.id.btnPost);

        // 3. Xử lý sự kiện bấm nút Đăng
        btnDang.setOnClickListener(v -> handlePost());
    }

    @SuppressLint("SetTextI18n")
    private void handlePost() {
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Bạn chưa nhập nội dung!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDang.setEnabled(false);
        btnDang.setText("Đang đăng...");

        // Chỉ lưu bài viết dạng text, image URL là null
        savePostToFirestore(content, null);
    }

    private void savePostToFirestore(String content, String imageUrl) {
        FirebaseUser user = auth.getCurrentUser();

        String userId = (user != null) ? user.getUid() : "anonymous_id";
        String userName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Người dùng ẩn danh";
        String userAvatar = (user != null && user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";

        String postId = db.collection("posts").document().getId();
        long timestamp = System.currentTimeMillis();

        FireBasePost newPost = new FireBasePost(
                postId,
                userId,
                userName,
                userAvatar,
                content,
                imageUrl, // Sẽ luôn là null
                timestamp
        );

        db.collection("posts").document(postId)
                .set(newPost)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreatePostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatePostActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnDang.setEnabled(true);
                    btnDang.setText("Đăng bài");
                });
    }
}
