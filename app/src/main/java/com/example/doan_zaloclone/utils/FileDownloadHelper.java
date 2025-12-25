package com.example.doan_zaloclone.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class for downloading files from URLs to local storage
 * Used for file preview - download before opening with local apps
 */
public class FileDownloadHelper {

    private static final String TAG = "FileDownloadHelper";

    /**
     * Download file from URL to app's cache directory
     *
     * @param context  Application context
     * @param fileUrl  URL of file to download
     * @param fileName Name to save file as
     * @param callback Callback for download result
     */
    public static void downloadFile(Context context, String fileUrl, String fileName, DownloadCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting download: " + fileUrl);

                // Create connection
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                // Check response
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    callback.onError("Server returned: " + connection.getResponseCode());
                    return;
                }

                // Get file size
                int fileLength = connection.getContentLength();

                // Create cache directory if needed
                File cacheDir = new File(context.getCacheDir(), "downloaded_files");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                // Create file
                File outputFile = new File(cacheDir, fileName);

                // Download file
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(outputFile);

                byte[] buffer = new byte[4096];
                long total = 0;
                int count;

                while ((count = input.read(buffer)) != -1) {
                    total += count;

                    // Publish progress
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        callback.onProgress(progress);
                    }

                    output.write(buffer, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                connection.disconnect();

                Log.d(TAG, "Download complete: " + outputFile.getAbsolutePath());
                callback.onSuccess(outputFile);

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Get URI for file using FileProvider
     *
     * @param context Application context
     * @param file    File to get URI for
     * @return Content URI for file
     */
    public static Uri getFileUri(Context context, File file) {
        return androidx.core.content.FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }

    /**
     * Clean up old downloaded files from cache
     *
     * @param context  Application context
     * @param maxAgeMs Maximum age of files to keep (milliseconds)
     */
    public static void cleanupOldFiles(Context context, long maxAgeMs) {
        File cacheDir = new File(context.getCacheDir(), "downloaded_files");
        if (!cacheDir.exists()) return;

        long now = System.currentTimeMillis();
        File[] files = cacheDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (now - file.lastModified() > maxAgeMs) {
                    file.delete();
                    Log.d(TAG, "Cleaned up old file: " + file.getName());
                }
            }
        }
    }

    /**
     * Callback interface for download operations
     */
    public interface DownloadCallback {
        void onProgress(int progress);

        void onSuccess(File file);

        void onError(String error);
    }
}
