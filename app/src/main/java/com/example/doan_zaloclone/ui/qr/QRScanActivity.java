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

        // Parse the scanned data
        if (scannedData.startsWith("zalo://user/")) {
            // User profile QR code
            String userId = scannedData.replace("zalo://user/", "");
            openUserProfile(userId);
        } else if (scannedData.startsWith("http://") || scannedData.startsWith("https://")) {
            // Web link
            Toast.makeText(this, "Link: " + scannedData, Toast.LENGTH_LONG).show();
            // TODO: Open browser or handle link
            finish();
        } else {
            // Generic QR code
            Toast.makeText(this, "Quét thành công: " + scannedData, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openUserProfile(String userId) {
        Intent intent = new Intent(this, ProfileCardActivity.class);
        intent.putExtra(ProfileCardActivity.EXTRA_USER_ID, userId);
        intent.putExtra(ProfileCardActivity.EXTRA_IS_EDITABLE, false);
        startActivity(intent);
        finish();
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
