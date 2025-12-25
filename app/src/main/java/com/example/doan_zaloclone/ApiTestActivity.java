package com.example.doan_zaloclone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doan_zaloclone.api.ApiService;
import com.example.doan_zaloclone.api.RetrofitClient;
import com.example.doan_zaloclone.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Test Activity for API Layer
 * Tests connectivity, authentication, and basic endpoints
 */
public class ApiTestActivity extends AppCompatActivity {
    private static final String TAG = "ApiTestActivity";

    private TextView tvResults;
    private ScrollView scrollView;
    private Button btnTestHealth, btnTestVerify, btnTestGetUser, btnClear;

    private ApiService apiService;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_test);

        // Initialize views
        tvResults = findViewById(R.id.tvResults);
        scrollView = findViewById(R.id.scrollView);
        btnTestHealth = findViewById(R.id.btnTestHealth);
        btnTestVerify = findViewById(R.id.btnTestVerify);
        btnTestGetUser = findViewById(R.id.btnTestGetUser);
        btnClear = findViewById(R.id.btnClear);

        // Initialize API service
        apiService = RetrofitClient.getApiService();
        firebaseAuth = FirebaseAuth.getInstance();

        // Setup click listeners
        btnTestHealth.setOnClickListener(v -> testHealthEndpoint());
        btnTestVerify.setOnClickListener(v -> testVerifyEndpoint());
        btnTestGetUser.setOnClickListener(v -> testGetUserEndpoint());
        btnClear.setOnClickListener(v -> {
            tvResults.setText("");
            appendLog("Logs cleared\n");
        });

        // WebSocket Test button
        Button btnWebSocketTest = findViewById(R.id.btnWebSocketTest);
        btnWebSocketTest.setOnClickListener(v -> {
            Intent intent = new Intent(this, WebSocketTestActivity.class);
            startActivity(intent);
        });

        // Display initial info
        displayInitialInfo();
    }

    private void displayInitialInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== API Test Activity ===\n\n");
        info.append("Base URL: ").append(getBaseUrl()).append("\n");

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            info.append("Firebase User: ").append(user.getEmail()).append("\n");
            info.append("User ID: ").append(user.getUid()).append("\n");
            info.append("✅ Authenticated\n");
        } else {
            info.append("❌ Not authenticated\n");
            info.append("Please login first!\n");
        }

        info.append("\n--- Ready to test ---\n\n");
        tvResults.setText(info.toString());
    }

    private String getBaseUrl() {
        // Extract from RetrofitClient (hardcoded for now)
        return "http://10.0.2.2:3000/api/";
    }

    private void testHealthEndpoint() {
        appendLog("Testing /health endpoint...\n");

        // Health endpoint doesn't go through /api/ prefix
        // We'll test with verify instead since health is just HTTP
        appendLog("Note: Health check doesn't require auth\n");
        appendLog("Testing with /api/auth/verify instead...\n");
        testVerifyEndpoint();
    }

    private void testVerifyEndpoint() {
        appendLog("Testing /api/auth/verify...\n");

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            appendLog("❌ Error: No Firebase user logged in\n\n");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<Map<String, Object>> call = apiService.verifyToken();

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    appendLog("✅ Success! Response:\n");
                    appendLog("Valid: " + result.get("valid") + "\n");
                    appendLog("UID: " + result.get("uid") + "\n");
                    appendLog("Response code: " + response.code() + "\n\n");
                    Toast.makeText(ApiTestActivity.this, "✅ Token verified!", Toast.LENGTH_SHORT).show();
                } else {
                    appendLog("❌ Failed: HTTP " + response.code() + "\n");
                    appendLog("Message: " + response.message() + "\n\n");
                    Toast.makeText(ApiTestActivity.this, "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                appendLog("❌ Network Error: " + t.getMessage() + "\n\n");
                Log.e(TAG, "Verify endpoint failed", t);
                Toast.makeText(ApiTestActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void testGetUserEndpoint() {
        appendLog("Testing /api/users/:userId...\n");

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            appendLog("❌ Error: No Firebase user logged in\n\n");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseUser.getUid();
        appendLog("Fetching user: " + userId + "\n");

        Call<User> call = apiService.getUser(userId);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    appendLog("✅ Success! User data:\n");
                    appendLog("ID: " + user.getId() + "\n");
                    appendLog("Name: " + user.getName() + "\n");
                    appendLog("Email: " + user.getEmail() + "\n");
                    appendLog("Phone: " + user.getPhoneNumber() + "\n");
                    appendLog("Response code: " + response.code() + "\n\n");
                    Toast.makeText(ApiTestActivity.this, "✅ User loaded!", Toast.LENGTH_SHORT).show();
                } else {
                    appendLog("❌ Failed: HTTP " + response.code() + "\n");
                    appendLog("Message: " + response.message() + "\n\n");
                    Toast.makeText(ApiTestActivity.this, "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                appendLog("❌ Network Error: " + t.getMessage() + "\n\n");
                Log.e(TAG, "Get user endpoint failed", t);
                Toast.makeText(ApiTestActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            tvResults.append(message);
            // Auto-scroll to bottom
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
        Log.d(TAG, message.trim());
    }
}
