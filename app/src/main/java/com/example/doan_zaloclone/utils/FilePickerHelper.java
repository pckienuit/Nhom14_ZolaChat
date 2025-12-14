package com.example.doan_zaloclone.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;

/**
 * Helper class for picking files using Android's Storage Access Framework (SAF)
 * 
 * Usage:
 * 1. Register launcher in Activity/Fragment onCreate:
 *    ActivityResultLauncher<String> launcher = registerForActivityResult(
 *        new ActivityResultContracts.GetContent(),
 *        uri -> { ... handle selected file ... }
 *    );
 * 
 * 2. Launch picker:
 *    FilePickerHelper.launchFilePicker(launcher);
 */
public class FilePickerHelper {
    
    /**
     * Launch file picker for any file type
     * @param launcher ActivityResultLauncher for GetContent contract
     */
    public static void launchFilePicker(ActivityResultLauncher<String> launcher) {
        launcher.launch("*/*");
    }
    
    /**
     * Launch file picker for specific MIME type
     * @param launcher ActivityResultLauncher for GetContent contract
     * @param mimeType MIME type filter (e.g., "image/*", "application/pdf")
     */
    public static void launchFilePickerWithType(ActivityResultLauncher<String> launcher, String mimeType) {
        launcher.launch(mimeType);
    }
    
    /**
     * Launch file picker for multiple file types
     * Note: This requires using GetMultipleContents contract
     * @param launcher ActivityResultLauncher for GetMultipleContents contract
     */
    public static void launchMultipleFilePicker(ActivityResultLauncher<String> launcher) {
        launcher.launch("*/*");
    }
    
    /**
     * Create intent for file picker (alternative approach without ActivityResultLauncher)
     * Use this with startActivityForResult if needed
     * @param mimeType MIME type filter
     * @return Intent for file picker
     */
    public static Intent createFilePickerIntent(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }
    
    /**
     * Create intent for multiple file selection
     * @param mimeType MIME type filter
     * @return Intent for multiple file picker
     */
    public static Intent createMultipleFilePickerIntent(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }
    
    /**
     * Validate if URI is accessible
     * @param context Android context
     * @param uri File URI
     * @return true if URI is valid and accessible
     */
    public static boolean isValidUri(Context context, Uri uri) {
        if (uri == null) return false;
        
        try {
            context.getContentResolver().openInputStream(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
