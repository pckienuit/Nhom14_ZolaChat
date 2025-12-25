package com.example.doan_zaloclone.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class for managing runtime permissions for calls
 * Handles CAMERA and RECORD_AUDIO permissions needed for voice and video calls
 */
public class PermissionHelper {
    // Permission request codes
    public static final int REQUEST_CODE_VOICE_CALL = 1001;
    public static final int REQUEST_CODE_VIDEO_CALL = 1002;
    private static final String TAG = "PermissionHelper";

    /**
     * Check if CAMERA permission is granted
     *
     * @param context Application context
     * @return true if permission is granted
     */
    public static boolean checkCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if RECORD_AUDIO permission is granted
     *
     * @param context Application context
     * @return true if permission is granted
     */
    public static boolean checkAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all required call permissions are granted
     *
     * @param context Application context
     * @param isVideo Whether this is a video call (requires CAMERA)
     * @return true if all required permissions are granted
     */
    public static boolean checkCallPermissions(Context context, boolean isVideo) {
        boolean audioGranted = checkAudioPermission(context);

        if (isVideo) {
            boolean cameraGranted = checkCameraPermission(context);
            return audioGranted && cameraGranted;
        }

        return audioGranted;
    }

    /**
     * Request call permissions
     *
     * @param activity    Activity to request permissions from
     * @param isVideo     Whether this is a video call (requires CAMERA)
     * @param requestCode Request code for permission result callback
     */
    public static void requestCallPermissions(Activity activity, boolean isVideo, int requestCode) {
        if (isVideo) {
            // Request both CAMERA and RECORD_AUDIO for video call
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    },
                    requestCode
            );
        } else {
            // Request only RECORD_AUDIO for voice call
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    requestCode
            );
        }
    }

    /**
     * Check if permission rationale should be shown
     * Called when user has previously denied permission
     *
     * @param activity Activity
     * @param isVideo  Whether this is a video call
     * @return true if should show rationale
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, boolean isVideo) {
        boolean audioRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO
        );

        if (isVideo) {
            boolean cameraRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
            );
            return audioRationale || cameraRationale;
        }

        return audioRationale;
    }

    /**
     * Handle permission request result
     *
     * @param requestCode  Request code from requestPermissions
     * @param permissions  Permissions that were requested
     * @param grantResults Grant results for each permission
     * @param callback     Callback for permission result
     */
    public static void handlePermissionResult(int requestCode,
                                              String[] permissions,
                                              int[] grantResults,
                                              PermissionCallback callback) {
        if (grantResults.length == 0) {
            callback.onPermissionDenied();
            return;
        }

        // Check if all permissions are granted
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            callback.onPermissionGranted();
        } else {
            callback.onPermissionDenied();
        }
    }

    /**
     * Get missing permissions for call
     *
     * @param context Application context
     * @param isVideo Whether this is a video call
     * @return Array of missing permissions (empty if all granted)
     */
    public static String[] getMissingPermissions(Context context, boolean isVideo) {
        java.util.List<String> missing = new java.util.ArrayList<>();

        if (!checkAudioPermission(context)) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }

        if (isVideo && !checkCameraPermission(context)) {
            missing.add(Manifest.permission.CAMERA);
        }

        return missing.toArray(new String[0]);
    }

    /**
     * Callback interface for permission results
     */
    public interface PermissionCallback {
        void onPermissionGranted();

        void onPermissionDenied();
    }
}
