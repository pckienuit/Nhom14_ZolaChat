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
import com.example.doan_zaloclone.viewmodel.PostViewModel;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etContent;
    private Button btnDang;
    private PostViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

        etContent = findViewById(R.id.etPostContent);
        btnDang = findViewById(R.id.btnPost);

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

        // Gọi ViewModel để đăng bài
        viewModel.createPost(content).observe(this, resource -> {
            if (resource.status == com.example.doan_zaloclone.utils.Status.SUCCESS) {
                Toast.makeText(CreatePostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (resource.status == com.example.doan_zaloclone.utils.Status.ERROR) {
                Toast.makeText(CreatePostActivity.this, "Lỗi: " + resource.message, Toast.LENGTH_SHORT).show();
                btnDang.setEnabled(true);
                btnDang.setText("Đăng bài");
            }
        });
    }
}
