package com.example.doan_zaloclone.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    private static final int MAX_DIMENSION = 1080;
    private static final int DEFAULT_QUALITY = 85;

    /**
     * Compress image to reduce file size (OPTIMIZED)
     *
     * @param context  Application context
     * @param imageUri Original image URI
     * @param quality  JPEG quality (0-100)
     * @return Uri of compressed image
     */
    public static Uri compressImage(Context context, Uri imageUri, int quality) throws Exception {
        Bitmap originalBitmap = null;
        Bitmap rotatedBitmap = null;
        Bitmap resizedBitmap = null;
        InputStream inStream = null;
        FileOutputStream outputStream = null;

        try {
            // Get image rotation first (reads EXIF efficiently)
            int rotation = getImageRotation(context, imageUri);

            // First pass: get image dimensions without loading into memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            inStream = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inStream, null, options);
            inStream.close();

            // Calculate inSampleSize to reduce memory
            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION);
            options.inJustDecodeBounds = false;

            // Second pass: decode with sampling
            inStream = context.getContentResolver().openInputStream(imageUri);
            originalBitmap = BitmapFactory.decodeStream(inStream, null, options);
            inStream.close();
            inStream = null;

            if (originalBitmap == null) {
                throw new Exception("Failed to decode image");
            }

            // Rotate if needed
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                        originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                if (rotatedBitmap != originalBitmap) {
                    originalBitmap.recycle();
                    originalBitmap = null;
                }
            } else {
                rotatedBitmap = originalBitmap;
                originalBitmap = null;
            }

            // Calculate final dimensions
            int width = rotatedBitmap.getWidth();
            int height = rotatedBitmap.getHeight();

            float ratio = Math.min(
                    (float) MAX_DIMENSION / width,
                    (float) MAX_DIMENSION / height
            );

            // Only resize if needed
            if (ratio < 1.0f) {
                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);
                resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true);
                rotatedBitmap.recycle();
                rotatedBitmap = null;
            } else {
                resizedBitmap = rotatedBitmap;
                rotatedBitmap = null;
            }

            // Save to temp file
            File tempFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            outputStream = new FileOutputStream(tempFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();

            return Uri.fromFile(tempFile);

        } finally {
            // Ensure all resources are cleaned up
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (Exception e) { /* ignore */ }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) { /* ignore */ }
            }
            if (originalBitmap != null && !originalBitmap.isRecycled()) {
                originalBitmap.recycle();
            }
            if (rotatedBitmap != null && !rotatedBitmap.isRecycled()) {
                rotatedBitmap.recycle();
            }
            if (resizedBitmap != null && !resizedBitmap.isRecycled()) {
                resizedBitmap.recycle();
            }
        }
    }

    /**
     * Compress with default quality
     */
    public static Uri compressImage(Context context, Uri imageUri) throws Exception {
        return compressImage(context, imageUri, DEFAULT_QUALITY);
    }

    /**
     * Calculate inSampleSize for efficient bitmap loading
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Get image rotation from EXIF data
     */
    private static int getImageRotation(Context context, Uri imageUri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

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
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
}
