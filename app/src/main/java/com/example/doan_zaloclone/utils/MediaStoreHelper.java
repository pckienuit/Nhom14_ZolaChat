package com.example.doan_zaloclone.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class MediaStoreHelper {
    
    /**
     * Load all images from device storage
     * @param context Application context
     * @return List of image URIs sorted by date (newest first)
     */
    public static List<Uri> loadPhotos(Context context) {
        return loadPhotos(context, 100); // Default to 100 images
    }
    
    /**
     * Load images from device storage with a limit
     * @param context Application context
     * @param limit Maximum number of images to load (prevents ANR)
     * @return List of image URIs sorted by date (newest first)
     */
    public static List<Uri> loadPhotos(Context context, int limit) {
        List<Uri> photoUris = new ArrayList<>();
        
        // Define columns to query
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_MODIFIED
        };
        
        // Sort by date modified (newest first) with LIMIT
        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC LIMIT " + limit;
        
        ContentResolver contentResolver = context.getContentResolver();
        
        try (Cursor cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int count = 0;
                
                while (cursor.moveToNext() && count < limit) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id)
                    );
                    photoUris.add(contentUri);
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return photoUris;
    }
}
