package com.example.doan_zaloclone.ui.qr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.personal.ProfileCardActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * QRScanActivity - Scan QR codes using ZXing library
 * Supports scanning user profile QR codes and other links
 */
public class QRScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private DecoratedBarcodeView barcodeView;
    private CaptureManager capture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        barcodeView = findViewById(R.id.barcode_scanner);

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initializeScanner(savedInstanceState);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void initializeScanner(Bundle savedInstanceState) {
        capture = new CaptureManager(this, barcodeView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        // Set custom callback for scanning result
        barcodeView.decodeContinuous(result -> {
            if (result != null && result.getText() != null) {
                handleScanResult(result.getText());
            }
        });
        
        // Close button
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
    }

    private void handleScanResult(String scannedData) {
        // Pause scanning
        capture.onPause();

        // Detect QR type and show appropriate dialog
        if (scannedData.startsWith("zalo://user/")) {
            // Zalo user profile QR
            String userId = scannedData.replace("zalo://user/", "");
            showOpenAppDialog(
                "Zalo Profile",
                "Mở trang cá nhân người dùng?",
                R.drawable.ic_avatar,
                () -> openUserProfile(userId)
            );
        } else if (scannedData.startsWith("http://") || scannedData.startsWith("https://")) {
            // Web link - detect specific apps
            String appName = detectAppFromUrl(scannedData);
            int iconRes = getIconForApp(appName);
            
            showOpenAppDialog(
                appName,
                "Mở link trong " + appName + "?\n\n" + scannedData,
                iconRes,
                () -> openWebLink(scannedData)
            );
        } else if (scannedData.startsWith("tel:")) {
            // Phone number
            showOpenAppDialog(
                "Điện thoại",
                "Gọi số: " + scannedData.replace("tel:", ""),
                R.drawable.ic_phone_outline,
                () -> openPhoneDialer(scannedData)
            );
        } else {
            // Generic QR code
            showQRContentDialog(scannedData);
        }
    }
    
    private String detectAppFromUrl(String url) {
        if (url.contains("facebook.com") || url.contains("fb.com")) return "Facebook";
        if (url.contains("instagram.com")) return "Instagram";
        if (url.contains("youtube.com") || url.contains("youtu.be")) return "YouTube";
        if (url.contains("zalo.me")) return "Zalo";
        if (url.contains("twitter.com") || url.contains("x.com")) return "X (Twitter)";
        if (url.contains("tiktok.com")) return "TikTok";
        return "Trình duyệt";
    }
    
    private int getIconForApp(String appName) {
        switch (appName) {
            case "Zalo": return R.drawable.ic_avatar;
            case "Facebook": 
            case "Instagram": 
            case "YouTube":
            case "TikTok":
            case "X (Twitter)":
                return R.drawable.ic_link; // Use link icon for social media
            default:
                return R.drawable.ic_link; // Generic link icon
        }
    }
    
    private void showOpenAppDialog(String appName, String message, int iconRes, Runnable onConfirm) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(appName)
            .setMessage(message)
            .setIcon(iconRes)
            .setPositiveButton("Mở", (dialog, which) -> {
                onConfirm.run();
                finish();
            })
            .setNegativeButton("Hủy", (dialog, which) -> finish())
            .setOnCancelListener(dialog -> finish())
            .show();
    }
    
    private void showQRContentDialog(String content) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nội dung QR")
            .setMessage(content)
            .setPositiveButton("Sao chép", (dialog, which) -> {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("QR Content", content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Đã sao chép", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("Đóng", (dialog, which) -> finish())
            .setOnCancelListener(dialog -> finish())
            .show();
    }

    private void openUserProfile(String userId) {
        Intent intent = new Intent(this, ProfileCardActivity.class);
        intent.putExtra(ProfileCardActivity.EXTRA_USER_ID, userId);
        intent.putExtra(ProfileCardActivity.EXTRA_IS_EDITABLE, false);
        startActivity(intent);
    }
    
    private void openWebLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(url));
        startActivity(intent);
    }
    
    private void openPhoneDialer(String phoneUri) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse(phoneUri));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeScanner(null);
            } else {
                Toast.makeText(this, "Cần quyền truy cập camera để quét QR", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }
}
