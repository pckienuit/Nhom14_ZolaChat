package com.example.doan_zaloclone.ui.sticker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;
import com.example.doan_zaloclone.repository.StickerRepository;
import com.example.doan_zaloclone.services.BackgroundRemovalService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

/**
 * Activity for creating custom stickers from images
 * Features: image picker, cropping, AI background removal, upload to VPS
 */
public class CreateStickerActivity extends AppCompatActivity {
    private static final String TAG = "CreateStickerActivity";
    
    // UI Components
    private ImageView stickerPreview;
    private View checkerboardBackground;
    private View plainBackground;
    private TextView placeholderText;
    private MaterialButton btnPickImage;
    private MaterialButton btnCrop;
    private MaterialButton btnRemoveBackground;
    private MaterialButton btnSave;
    private ProgressBar loadingProgress;
    private TextView statusText;
    
    // Data
    private Uri originalImageUri;
    private Uri processedImageUri;
    private boolean backgroundRemoved = false;
    private BackgroundRemovalService bgRemovalService;
    private StickerRepository stickerRepository;
    private String currentUserId;
    
    // Activity launchers
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_sticker);
        
        initServices();
        initViews();
        setupLaunchers();
        setupListeners();
    }
    
    private void initServices() {
        bgRemovalService = new BackgroundRemovalService();
        stickerRepository = StickerRepository.getInstance();
        
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }
    }
    
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        stickerPreview = findViewById(R.id.stickerPreview);
        checkerboardBackground = findViewById(R.id.checkerboardBackground);
        plainBackground = findViewById(R.id.plainBackground);
        placeholderText = findViewById(R.id.placeholderText);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnCrop = findViewById(R.id.btnCrop);
        btnRemoveBackground = findViewById(R.id.btnRemoveBackground);
        btnSave = findViewById(R.id.btnSave);
        loadingProgress = findViewById(R.id.loadingProgress);
        statusText = findViewById(R.id.statusText);
    }
    
    private void setupLaunchers() {
        // Permission launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(this, "Cần quyền truy cập ảnh để chọn sticker", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    originalImageUri = uri;
                    processedImageUri = uri;
                    updatePreview(uri);
                    enableButtons();
                }
            }
        );
    }
    
    private void setupListeners() {
        btnPickImage.setOnClickListener(v -> requestPermissionAndPickImage());
        btnCrop.setOnClickListener(v -> cropImage());
        btnRemoveBackground.setOnClickListener(v -> removeBackground());
        btnSave.setOnClickListener(v -> saveStickerToVPS());
    }
    
    private void requestPermissionAndPickImage() {
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_IMAGES
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            permissionLauncher.launch(permission);
        }
    }
    
    private void launchImagePicker() {
        imagePickerLauncher.launch("image/*");
    }
    
    private void cropImage() {
        if (processedImageUri != null) {
            File destFile = new File(getCacheDir(), "cropped_sticker_" + System.currentTimeMillis() + ".png");
            UCrop.of(processedImageUri, Uri.fromFile(destFile))
                .withAspectRatio(1, 1) // Square crop
                .withMaxResultSize(512, 512) // Max size for stickers
                .start(this);
        } else {
            Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void removeBackground() {
        if (processedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading("Đang xóa nền...");
        
        bgRemovalService.removeBackground(processedImageUri, this, new BackgroundRemovalService.RemovalCallback() {
            @Override
            public void onSuccess(android.graphics.Bitmap resultBitmap, String transparentPngUrl) {
                runOnUiThread(() -> {
                    try {
                        // Save bitmap to cache as PNG
                        File outputFile = new File(getCacheDir(), "bg_removed_" + System.currentTimeMillis() + ".png");
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        resultBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();
                        
                        // Update processedImageUri
                        processedImageUri = Uri.fromFile(outputFile);
                        backgroundRemoved = true;
                        updatePreview(processedImageUri);
                        showCheckerboard(true);
                        setStatus("✅ Đã xóa nền thành công!");
                        hideLoading();
                    } catch (Exception e) {
                        Toast.makeText(CreateStickerActivity.this, 
                            "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        hideLoading();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CreateStickerActivity.this, 
                        "Lỗi xóa nền: " + error, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    loadingProgress.setProgress(percent);
                    setStatus("Đang xóa nền... " + percent + "%");
                });
            }
        });
    }
    
    private void saveStickerToVPS() {
        if (processedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading("Đang tải lên...");
        
        String fileName = "sticker_" + System.currentTimeMillis() + (backgroundRemoved ? ".png" : ".jpg");
        
        stickerRepository.uploadCustomSticker(processedImageUri, currentUserId, fileName, 
            new StickerRepository.UploadCallback() {
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> {
                        loadingProgress.setProgress(progress);
                        setStatus("Đang tải lên... " + progress + "%");
                    });
                }
                
                @Override
                public void onSuccess(String stickerUrl) {
                    runOnUiThread(() -> {
                        saveToFirestore(stickerUrl);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(CreateStickerActivity.this, 
                            "Lỗi upload: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
    }
    
    private void saveToFirestore(String stickerUrl) {
        // Get or create "My Stickers" pack
        stickerRepository.getOrCreateUserStickerPack(currentUserId, new StickerRepository.PackCallback() {
            @Override
            public void onSuccess(String packId) {
                // Create Sticker object
                Sticker newSticker = new Sticker();
                newSticker.setId(UUID.randomUUID().toString());
                newSticker.setPackId(packId);
                newSticker.setImageUrl(stickerUrl);
                newSticker.setCreatorId(currentUserId);
                newSticker.setFormat(backgroundRemoved ? "PNG" : "JPEG");
                newSticker.setAnimated(false);
                newSticker.setCreatedAt(System.currentTimeMillis());
                
                // Save to Firestore
                stickerRepository.addStickerToPack(packId, newSticker, () -> {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(CreateStickerActivity.this, 
                            "✅ Đã lưu sticker!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CreateStickerActivity.this, 
                        "Lỗi tạo gói sticker: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void updatePreview(Uri imageUri) {
        placeholderText.setVisibility(View.GONE);
        Glide.with(this)
            .load(imageUri)
            .fitCenter()
            .into(stickerPreview);
    }
    
    private void enableButtons() {
        btnCrop.setEnabled(true);
        btnRemoveBackground.setEnabled(true);
        btnSave.setEnabled(true);
    }
    
    private void showCheckerboard(boolean show) {
        checkerboardBackground.setVisibility(show ? View.VISIBLE : View.GONE);
        plainBackground.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showLoading(String message) {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingProgress.setProgress(0);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
        
        // Disable buttons during loading
        btnPickImage.setEnabled(false);
        btnCrop.setEnabled(false);
        btnRemoveBackground.setEnabled(false);
        btnSave.setEnabled(false);
    }
    
    private void hideLoading() {
        loadingProgress.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        
        // Re-enable buttons
        btnPickImage.setEnabled(true);
        if (processedImageUri != null) {
            btnCrop.setEnabled(true);
            btnRemoveBackground.setEnabled(true);
            btnSave.setEnabled(true);
        }
    }
    
    private void setStatus(String message) {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                Uri croppedUri = UCrop.getOutput(data);
                if (croppedUri != null) {
                    processedImageUri = croppedUri;
                    updatePreview(croppedUri);
                    setStatus("✅ Đã cắt ảnh thành công!");
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                Throwable cropError = UCrop.getError(data);
                Toast.makeText(this, "Lỗi cắt ảnh: " + (cropError != null ? cropError.getMessage() : "Unknown"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
