package com.example.doan_zaloclone.ui.qr;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.viewmodel.PersonalViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * MyQRCodeActivity - Display and share user's personal QR code
 * QR code contains deep link: zalo://user/{userId}
 */
public class MyQRCodeActivity extends AppCompatActivity {

    private static final int WRITE_PERMISSION_REQUEST = 101;
    private static final String QR_PREFIX = "zalo://user/";
    
    private ImageView ivAvatar;
    private ImageView ivQrCode;
    private TextView tvUserName;
    private TextView tvBio;
    private View btnShare;
    private View btnSave;
    private View btnScan;
    
    private PersonalViewModel viewModel;
    private String currentUserId;
    private Bitmap qrCodeBitmap;
    private User currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qr_code);

        // Get current user ID
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = firebaseUser.getUid();

        initViews();
        setupToolbar();
        setupClickListeners();
        
        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);
        observeViewModel();
        
        // Load user data
        viewModel.loadCurrentUser();
        
        // Generate QR code
        generateQRCode();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvUserName = findViewById(R.id.tvUserName);
        tvBio = findViewById(R.id.tvBio);
        btnShare = findViewById(R.id.btnShare);
        btnSave = findViewById(R.id.btnSave);
        btnScan = findViewById(R.id.btnScan);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                generateQRCode();
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        btnShare.setOnClickListener(v -> shareQRCode());
        btnSave.setOnClickListener(v -> saveQRCode());
        btnScan.setOnClickListener(v -> openQRScanner());
    }

    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess() && resource.getData() != null) {
                currentUser = resource.getData();
                updateUI(currentUser);
            } else if (resource.isError()) {
                Toast.makeText(this, "Lỗi tải thông tin: " + resource.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(User user) {
        // Update name
        if (user.getName() != null && !user.getName().isEmpty()) {
            tvUserName.setText(user.getName());
        } else {
            tvUserName.setText("Người dùng");
        }

        // Update bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            tvBio.setText(user.getBio());
            tvBio.setVisibility(View.VISIBLE);
        } else {
            tvBio.setVisibility(View.GONE);
        }

        // Update avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_avatar)
                .circleCrop()
                .into(ivAvatar);
        }
    }

    private void generateQRCode() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Không thể tạo mã QR", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrContent = QR_PREFIX + currentUserId;
        
        try {
            QRCodeWriter writer = new QRCodeWriter();
            int size = 512; // QR code size in pixels
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size);

            qrCodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            int whiteColor = ContextCompat.getColor(this, R.color.white);
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? primaryColor : whiteColor);
                }
            }

            ivQrCode.setImageBitmap(qrCodeBitmap);
            
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "Vui lòng đợi tạo mã QR", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save bitmap to cache
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get URI using FileProvider
            Uri contentUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            
            String userName = currentUser != null && currentUser.getName() != null 
                ? currentUser.getName() : "Người dùng";
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Quét mã QR để xem trang cá nhân của " + userName + " trên Zalo");
            
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ QR code"));
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi chia sẻ QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "Vui lòng đợi tạo mã QR", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check permission for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_PERMISSION_REQUEST);
                return;
            }
        }

        saveQRToGallery();
    }

    private void saveQRToGallery() {
        try {
            String fileName = "ZaloQR_" + System.currentTimeMillis() + ".png";
            OutputStream outputStream;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZaloQR");
                
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    Toast.makeText(this, "Không thể lưu ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                // For Android 9 and below
                File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "ZaloQR");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
                
                // Notify gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(file));
                sendBroadcast(mediaScanIntent);
            }

            if (outputStream != null) {
                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                Toast.makeText(this, "Đã lưu QR code vào thư viện ảnh", Toast.LENGTH_SHORT).show();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openQRScanner() {
        Intent intent = new Intent(this, QRScanActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveQRToGallery();
            } else {
                Toast.makeText(this, "Cần quyền lưu trữ để lưu QR code", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
