package com.example.doan_zaloclone.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {
    
    private static final int MAX_DIMENSION = 1080;
    private static final int DEFAULT_QUALITY = 85;
    
    /**
     * Compress image to reduce file size
     * @param context Application context
     * @param imageUri Original image URI
     * @param quality JPEG quality (0-100)
     * @return Uri of compressed image
     */
    public static Uri compressImage(Context context, Uri imageUri, int quality) throws Exception {
        // Read original image
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();
        
        if (originalBitmap == null) {
            throw new Exception("Failed to decode image");
        }
        
        // Get image orientation
        int rotation = getImageRotation(context, imageUri);
        
        // Rotate if needed
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, 
                    originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
        }
        
        // Calculate new dimensions while keeping aspect ratio
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        
        float ratio = Math.min(
                (float) MAX_DIMENSION / width,
                (float) MAX_DIMENSION / height
        );
        
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        // Resize bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
        originalBitmap.recycle();
        
        // Save to temp file
        File tempFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        outputStream.flush();
        outputStream.close();
        resizedBitmap.recycle();
        
        return Uri.fromFile(tempFile);
    }
    
    /**
     * Compress with default quality
     */
    public static Uri compressImage(Context context, Uri imageUri) throws Exception {
        return compressImage(context, imageUri, DEFAULT_QUALITY);
    }
    
    /**
     * Get image rotation from EXIF data
     */
    private static int getImageRotation(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (inputStream != null) inputStream.close();
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
