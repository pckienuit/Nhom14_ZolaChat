package com.example.doan_zaloclone.ui.sticker;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.StickerPack;
import com.example.doan_zaloclone.repository.StickerRepository;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Activity for browsing and downloading sticker packs
 */
public class StickerStoreActivity extends AppCompatActivity {
    private static final String TAG = "StickerStoreActivity";

    private RecyclerView featuredRecyclerView;
    private RecyclerView trendingRecyclerView;
    private RecyclerView newRecyclerView;
    private ProgressBar loadingProgress;

    private StickerPackStoreAdapter featuredAdapter;
    private StickerPackStoreAdapter trendingAdapter;
    private StickerPackStoreAdapter newAdapter;

    private StickerRepository stickerRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_store);

        initViews();
        initData();
        loadStores();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        featuredRecyclerView = findViewById(R.id.featuredRecyclerView);
        trendingRecyclerView = findViewById(R.id.trendingRecyclerView);
        newRecyclerView = findViewById(R.id.newRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);

        // Setup RecyclerViews
        setupRecyclerView(featuredRecyclerView, true); // Horizontal
        setupRecyclerView(trendingRecyclerView, false); // Grid
        setupRecyclerView(newRecyclerView, false); // Grid
    }

    private void setupRecyclerView(RecyclerView recyclerView, boolean horizontal) {
        if (horizontal) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        }
    }

    private void initData() {
        stickerRepository = StickerRepository.getInstance();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        // Create adapters
        StickerPackStoreAdapter.OnPackActionListener listener = new StickerPackStoreAdapter.OnPackActionListener() {
            @Override
            public void onDownloadPack(StickerPack pack) {
                downloadPack(pack);
            }

            @Override
            public void onPackClick(StickerPack pack) {
                // TODO: Open pack detail view
                Toast.makeText(StickerStoreActivity.this,
                        "Xem gói: " + pack.getName(), Toast.LENGTH_SHORT).show();
            }
        };

        featuredAdapter = new StickerPackStoreAdapter(listener);
        trendingAdapter = new StickerPackStoreAdapter(listener);
        newAdapter = new StickerPackStoreAdapter(listener);

        featuredRecyclerView.setAdapter(featuredAdapter);
        trendingRecyclerView.setAdapter(trendingAdapter);
        newRecyclerView.setAdapter(newAdapter);
    }

    private void loadStores() {
        showLoading(true);

        // Load featured packs
        stickerRepository.getFeaturedPacks().observe(this, new Observer<Resource<List<StickerPack>>>() {
            @Override
            public void onChanged(Resource<List<StickerPack>> resource) {
                if (resource.isSuccess()) {
                    featuredAdapter.setPacks(resource.getData());
                } else if (resource.isError()) {
                    Toast.makeText(StickerStoreActivity.this,
                            "Lỗi tải featured: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load trending packs
        stickerRepository.getTrendingPacks().observe(this, new Observer<Resource<List<StickerPack>>>() {
            @Override
            public void onChanged(Resource<List<StickerPack>> resource) {
                if (resource.isSuccess()) {
                    trendingAdapter.setPacks(resource.getData());
                } else if (resource.isError()) {
                    Toast.makeText(StickerStoreActivity.this,
                            "Lỗi tải trending: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load new packs
        stickerRepository.getNewPacks().observe(this, new Observer<Resource<List<StickerPack>>>() {
            @Override
            public void onChanged(Resource<List<StickerPack>> resource) {
                showLoading(false);
                if (resource.isSuccess()) {
                    newAdapter.setPacks(resource.getData());
                } else if (resource.isError()) {
                    Toast.makeText(StickerStoreActivity.this,
                            "Lỗi tải new packs: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void downloadPack(StickerPack pack) {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang tải gói " + pack.getName() + "...", Toast.LENGTH_SHORT).show();

        stickerRepository.addPackToUser(currentUserId, pack.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Đã thêm gói sticker!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
