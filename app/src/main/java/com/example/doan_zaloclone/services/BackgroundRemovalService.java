package com.example.doan_zaloclone.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for AI-powered background removal
 * Supports Clipdrop API (primary) with rembg VPS fallback
 */
public class BackgroundRemovalService {
    private static final String TAG = "BackgroundRemoval";

    // Clipdrop API Configuration
    private static final String CLIPDROP_API_KEY = "4a4976c484431ca4a89cb1ab2a8f3ea278228d9aef74327cb898f302fee92a332197087247ed23badd193ec852e709b2";
    private static final String CLIPDROP_API_URL = "https://clipdrop-api.co/remove-background/v1";

    // VPS rembg fallback
    private static final String VPS_REMBG_URL = "http://163.61.182.20/api/rembg";

    private final OkHttpClient client;

    public BackgroundRemovalService() {
        this.client = new OkHttpClient();
    }

    /**
     * Remove background from an image using Clipdrop API (primary method)
     * Falls back to rembg VPS if Clipdrop fails or quota exceeded
     *
     * @param imageUri URI of the image to process
     * @param context  Android context for file operations
     * @param callback Callback for results
     */
    public void removeBackground(@NonNull Uri imageUri, @NonNull Context context, @NonNull RemovalCallback callback) {
        callback.onProgress(10);

        try {
            // Convert URI to File
            File imageFile = uriToFile(imageUri, context);

            if (imageFile == null) {
                callback.onError("Không thể đọc file ảnh");
                return;
            }

            callback.onProgress(20);

            // Try Clipdrop first
            removeBackgroundWithClipdrop(imageFile, callback);

        } catch (Exception e) {
            Log.e(TAG, "Error preparing image for background removal", e);
            callback.onError("Lỗi xử lý ảnh: " + e.getMessage());
        }
    }

    /**
     * Remove background using Clipdrop API
     */
    private void removeBackgroundWithClipdrop(@NonNull File imageFile, @NonNull RemovalCallback callback) {
        callback.onProgress(30);

        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image_file", imageFile.getName(),
                            RequestBody.create(imageFile, MediaType.parse("image/*")))
                    .build();

            Request request = new Request.Builder()
                    .url(CLIPDROP_API_URL)
                    .addHeader("x-api-key", CLIPDROP_API_KEY)
                    .post(requestBody)
                    .build();

            callback.onProgress(40);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onProgress(80);

                            byte[] imageBytes = response.body().bytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                            if (bitmap != null) {
                                callback.onProgress(100);
                                callback.onSuccess(bitmap, null); // URL will be set after upload to VPS
                                Log.d(TAG, "Clipdrop background removal successful");
                            } else {
                                callback.onError("Không thể decode ảnh kết quả");
                            }

                        } else if (response.code() == 402) {
                            // Payment required - quota exceeded
                            Log.w(TAG, "Clipdrop quota exceeded, falling back to rembg");
                            callback.onProgress(50);
                            removeBackgroundWithRembg(imageFile, callback);

                        } else {
                            Log.e(TAG, "Clipdrop API error: " + response.code() + " - " + response.message());
                            // Fallback to rembg
                            callback.onProgress(50);
                            removeBackgroundWithRembg(imageFile, callback);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing Clipdrop response", e);
                        callback.onError("Lỗi xử lý kết quả: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Clipdrop API call failed, falling back to rembg", e);
                    // Fallback to rembg
                    callback.onProgress(50);
                    removeBackgroundWithRembg(imageFile, callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error calling Clipdrop API", e);
            callback.onError("Lỗi gọi API: " + e.getMessage());
        }
    }

    /**
     * Remove background using self-hosted rembg on VPS
     */
    private void removeBackgroundWithRembg(@NonNull File imageFile, @NonNull RemovalCallback callback) {
        callback.onProgress(60);

        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", imageFile.getName(),
                            RequestBody.create(imageFile, MediaType.parse("image/*")))
                    .build();

            Request request = new Request.Builder()
                    .url(VPS_REMBG_URL)
                    .post(requestBody)
                    .build();

            callback.onProgress(70);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onProgress(90);

                            byte[] imageBytes = response.body().bytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                            if (bitmap != null) {
                                callback.onProgress(100);
                                callback.onSuccess(bitmap, null);
                                Log.d(TAG, "Rembg background removal successful");
                            } else {
                                callback.onError("Không thể decode ảnh kết quả từ rembg");
                            }

                        } else {
                            Log.e(TAG, "Rembg API error: " + response.code());
                            callback.onError("Lỗi xóa nền (rembg): " + response.code());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing rembg response", e);
                        callback.onError("Lỗi xử lý kết quả rembg: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Rembg API call failed", e);
                    callback.onError("Không thể kết nối đến server xóa nền: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error calling rembg API", e);
            callback.onError("Lỗi gọi rembg API: " + e.getMessage());
        }
    }

    /**
     * Convert URI to File
     */
    private File uriToFile(Uri uri, Context context) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return tempFile;

        } catch (IOException e) {
            Log.e(TAG, "Error converting URI to File", e);
            return null;
        }
    }

    /**
     * Callback interface for background removal results
     */
    public interface RemovalCallback {
        void onSuccess(Bitmap resultBitmap, String transparentPngUrl);

        void onError(String error);

        void onProgress(int percent);
    }
}
