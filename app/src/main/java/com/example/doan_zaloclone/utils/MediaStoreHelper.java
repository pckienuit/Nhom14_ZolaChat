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
        List<Uri> photoUris = new ArrayList<>();
        
        // Define columns to query
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_MODIFIED
        };
        
        // Sort by date modified (newest first)
        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        
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
                
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id)
                    );
                    photoUris.add(contentUri);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return photoUris;
    }
}
