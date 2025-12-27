package com.example.doan_zaloclone.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

    public CreatePostActivity(TextView ignoredTvUserName) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 2. Ánh xạ View (Đảm bảo ID khớp với file XML activity_create_post.xml)
        etContent = findViewById(R.id.etPostContent);
        btnDang = findViewById(R.id.btnPost);
        // Nếu trong layout chưa có nút back hoặc tên user thì bạn có thể bỏ qua 2 dòng dưới
        // btnBack = findViewById(R.id.btnBack);
        // tvUserName = findViewById(R.id.tvUserName);

        // 3. Hiển thị tên người dùng hiện tại (nếu có)

        // 4. Xử lý sự kiện bấm nút Đăng
        btnDang.setOnClickListener(v -> handlePost());

        // Xử lý nút quay lại (nếu có)
    }

    @SuppressLint("SetTextI18n")
    private void handlePost() {
        String content = etContent.getText().toString().trim();

        // Kiểm tra nội dung rỗng
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Bạn chưa nhập nội dung!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tắt nút đăng để tránh bấm nhiều lần
        btnDang.setEnabled(false);
        btnDang.setText("Đang đăng...");

        // Lấy thông tin User hiện tại
        FirebaseUser user = auth.getCurrentUser();

        String userId = (user != null) ? user.getUid() : "anonymous_id";
        String userName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Người dùng ẩn danh";
        String userAvatar = (user != null && user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";

        // Tạo ID mới cho bài viết
        String postId = db.collection("posts").document().getId();
        long timestamp = System.currentTimeMillis();

        // Tạo đối tượng Post
        // Lưu ý: Constructor này phải khớp với Class FireBasePost bạn đang có
        FireBasePost newPost = new FireBasePost(
                postId,
                userId,
                userName,
                userAvatar,
                content,
                null, // Ảnh bài viết (tạm thời để null)
                timestamp
        );

        // Đẩy lên Firestore
        db.collection("posts").document(postId)
                .set(newPost)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreatePostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Đóng Activity để quay về màn hình trước
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatePostActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Mở lại nút đăng nếu lỗi
                    btnDang.setEnabled(true);
                    btnDang.setText("Đăng bài");
                });
    }
}
