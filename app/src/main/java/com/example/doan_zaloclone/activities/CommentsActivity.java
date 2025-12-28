package com.example.doan_zaloclone.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.adapters.CommentAdapter;
import com.example.doan_zaloclone.models.Comment;
import com.example.doan_zaloclone.utils.Resource;
import com.example.doan_zaloclone.viewmodel.PostViewModel;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";

    private RecyclerView rvComments;
    private EditText etContent;
    private ImageButton btnSend;
    
    private PostViewModel viewModel;
    private CommentAdapter adapter;
    private List<Comment> commentList;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Enable back button in ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bình luận");
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) {
            Toast.makeText(this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(PostViewModel.class);
        
        initViews();
        setupRecyclerView();
        observeComments();

        btnSend.setOnClickListener(v -> handleSendComment());
    }

    private void initViews() {
        rvComments = findViewById(R.id.rvComments);
        etContent = findViewById(R.id.etCommentContent);
        btnSend = findViewById(R.id.btnSendComment);
    }

    private void setupRecyclerView() {
        commentList = new ArrayList<>();
        adapter = new CommentAdapter(this, commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);
    }

    private void observeComments() {
        viewModel.getComments(postId).observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                commentList.clear();
                if (resource.getData() != null) {
                    commentList.addAll(resource.getData());
                }
                adapter.notifyDataSetChanged();
                
                // Scroll to bottom if new comment added
                if (!commentList.isEmpty()) {
                    rvComments.smoothScrollToPosition(commentList.size() - 1);
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Lỗi tải bình luận: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSendComment() {
        String content = etContent.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        etContent.setText(""); // Clear input
        
        viewModel.sendComment(postId, content).observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Gửi bình luận thất bại: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
