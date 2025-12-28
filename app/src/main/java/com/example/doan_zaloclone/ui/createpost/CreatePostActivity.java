package com.example.doan_zaloclone.ui.createpost;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.viewmodel.CreatePostViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class CreatePostActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText edtStatus;
    private Button btnPost;
    private ImageView btnAddPhoto, btnAddVideo;
    private ProgressBar progressBar;
    private CreatePostViewModel createPostViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_create_post);

        // Initialize ViewModel
        createPostViewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        edtStatus = findViewById(R.id.edtStatus);
        btnPost = findViewById(R.id.btnPost);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        progressBar = findViewById(R.id.progressBar);

        // Handle back button click
        toolbar.setNavigationOnClickListener(v -> finish());

        // Handle post button click
        btnPost.setOnClickListener(v -> {
            String content = edtStatus.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
                return;
            }
            createPost(content);
        });

        // TODO: Implement photo/video adding logic
    }

    private void createPost(String content) {
        createPostViewModel.createPost(content).observe(this, resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    btnPost.setEnabled(false);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    btnPost.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}
