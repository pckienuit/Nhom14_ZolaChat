package com.example.doan_zaloclone.ui.sticker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;
import com.example.doan_zaloclone.repository.StickerRepository;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Activity to display user's personal stickers
 */
public class MyStickerActivity extends AppCompatActivity {

    private static final int REQUEST_CREATE_STICKER = 101;

    private StickerRepository stickerRepository;
    private MyStickerAdapter adapter;
    private String currentUserId;

    // Views
    private ImageView btnBack;
    private ImageView btnCreate;
    private TextView txtTitle;
    private TextView txtStickerCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerStickers;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sticker);

        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        // Initialize Repository
        stickerRepository = StickerRepository.getInstance();

        // Initialize views
        initViews();

        // Setup transparent status bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // Setup RecyclerView
        setupRecyclerView();

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Load stickers
        loadMyStickers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCreate = findViewById(R.id.btnCreate);
        txtTitle = findViewById(R.id.txtTitle);
        txtStickerCount = findViewById(R.id.txtStickerCount);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerStickers = findViewById(R.id.recyclerStickers);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> openCreateSticker());
    }

    private void setupRecyclerView() {
        adapter = new MyStickerAdapter();
        adapter.setListener(new MyStickerAdapter.OnStickerClickListener() {
            @Override
            public void onStickerClick(Sticker sticker) {
                // Preview sticker
                Toast.makeText(MyStickerActivity.this, 
                    sticker.getId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStickerLongClick(Sticker sticker) {
                // Toggle edit mode
                adapter.setEditMode(!adapter.isEditMode());
            }

            @Override
            public void onDeleteClick(Sticker sticker) {
                // Confirm delete
                showDeleteConfirmDialog(sticker);
            }
        });

        recyclerStickers.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerStickers.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimaryDark
        );
        swipeRefreshLayout.setOnRefreshListener(this::loadMyStickers);
    }

    private void loadMyStickers() {
        progressBar.setVisibility(View.VISIBLE);
        
        // First ensure pack exists for current user, then load stickers
        stickerRepository.getOrCreateUserStickerPack(currentUserId, new StickerRepository.PackCallback() {
            @Override
            public void onSuccess(String packId) {
                // Now load stickers for this user's pack
                stickerRepository.getStickersInPack(packId).observe(MyStickerActivity.this, resource -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (resource.isSuccess()) {
                        List<Sticker> stickers = resource.getData();
                        if (stickers != null && !stickers.isEmpty()) {
                            adapter.updateStickers(stickers);
                            updateStickerCount(stickers.size());
                            showContent();
                        } else {
                            showEmptyState();
                        }
                    } else if (resource.isError()) {
                        Toast.makeText(MyStickerActivity.this, "Lỗi: " + resource.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MyStickerActivity.this, "Lỗi tạo pack: " + error, 
                    Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void updateStickerCount(int count) {
        txtStickerCount.setText(count + (count == 1 ? " sticker" : " sticker"));
    }

    private void showContent() {
        recyclerStickers.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerStickers.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        updateStickerCount(0);
    }

    private void openCreateSticker() {
        Intent intent = new Intent(this, CreateStickerActivity.class);
        startActivityForResult(intent, REQUEST_CREATE_STICKER);
    }

    private void showDeleteConfirmDialog(Sticker sticker) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa sticker")
                .setMessage("Bạn có chắc muốn xóa sticker này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteSticker(sticker))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSticker(Sticker sticker) {
        progressBar.setVisibility(View.VISIBLE);
        
        stickerRepository.deleteSticker(currentUserId, sticker.getId(), new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MyStickerActivity.this, 
                    "Đã xóa sticker", Toast.LENGTH_SHORT).show();
                // Exit edit mode
                adapter.setEditMode(false);
                // Reload stickers
                loadMyStickers();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_STICKER && resultCode == RESULT_OK) {
            // Reload stickers after creating new one
            loadMyStickers();
        }
    }
}
