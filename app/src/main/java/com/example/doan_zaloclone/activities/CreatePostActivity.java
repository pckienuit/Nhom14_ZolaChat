package com.example.doan_zaloclone.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FireBasePost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class CreatePostActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1;
    
    private EditText etContent;
    private Button btnDang;
    private ImageView imgPreview;
    private Button btnAddImage;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    private Uri selectedImageUri;

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
        
        // Thêm ImageView để xem trước ảnh (cần thêm vào layout XML)
        imgPreview = findViewById(R.id.imgPreview); // ID này cần thêm vào XML
        // Thêm nút chọn ảnh (cần thêm vào layout XML)
        btnAddImage = findViewById(R.id.btnAddImage); // ID này cần thêm vào XML

        // 3. Xử lý sự kiện chọn ảnh
        if (btnAddImage != null) {
            btnAddImage.setOnClickListener(v -> openImagePicker());
        }

        // 4. Xử lý sự kiện bấm nút Đăng
        btnDang.setOnClickListener(v -> handlePost());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (imgPreview != null) {
                imgPreview.setVisibility(View.VISIBLE);
                imgPreview.setImageURI(selectedImageUri);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void handlePost() {
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(content) && selectedImageUri == null) {
            Toast.makeText(this, "Bạn chưa nhập nội dung hoặc chọn ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDang.setEnabled(false);
        btnDang.setText("Đang đăng...");

        if (selectedImageUri != null) {
            uploadImageAndPost(content);
        } else {
            savePostToFirestore(content, null);
        }
    }
    
    private void uploadImageAndPost(String content) {
        // Sử dụng Cloudinary để upload ảnh
        // Lưu ý: Cần đảm bảo MediaManager đã được init trong ZaloApplication
        MediaManager.get().upload(selectedImageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        savePostToFirestore(content, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            Toast.makeText(CreatePostActivity.this, "Upload ảnh thất bại: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            btnDang.setEnabled(true);
                            btnDang.setText("Đăng bài");
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                })
                .dispatch();
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
                imageUrl,
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
