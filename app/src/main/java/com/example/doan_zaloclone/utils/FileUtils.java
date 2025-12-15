package com.example.doan_zaloclone.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.example.doan_zaloclone.R;

/**
 * Utility class for file operations
 */
public class FileUtils {
    
    /**
     * Get file name from URI
     * @param context Android context
     * @param uri File URI
     * @return File name or "Unknown"
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = "Unknown";
        
        if (uri == null) return fileName;
        
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } else if (scheme != null && scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        }
        
        return fileName;
    }
    
    /**
     * Get file size from URI
     * @param context Android context
     * @param uri File URI
     * @return File size in bytes, or 0 if unknown
     */
    public static long getFileSize(Context context, Uri uri) {
        long fileSize = 0;
        
        if (uri == null) return fileSize;
        
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        
        return fileSize;
    }
    
    /**
     * Get MIME type from URI
     * @param context Android context
     * @param uri File URI
     * @return MIME type string or "application/octet-stream"
     */
    public static String getMimeType(Context context, Uri uri) {
        if (uri == null) return "application/octet-stream";
        
        String mimeType;
        
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            mimeType = contentResolver.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    /**
     * Format file size to human-readable string
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Get file icon resource based on MIME type
     * @param mimeType MIME type string
     * @return Drawable resource ID
     */
    public static int getFileIcon(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return R.drawable.ic_document;
        }
        
        // Image files
        if (mimeType.startsWith("image/")) {
            return R.drawable.ic_file_image;
        }
        
        // Video files
        if (mimeType.startsWith("video/")) {
            return R.drawable.ic_file_video;
        }
        
        // PDF files
        if (mimeType.equals("application/pdf")) {
            return R.drawable.ic_file_pdf;
        }
        
        // Word documents
        if (mimeType.equals("application/msword") || 
            mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return R.drawable.ic_file_word;
        }
        
        // Excel spreadsheets
        if (mimeType.equals("application/vnd.ms-excel") || 
            mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return R.drawable.ic_file_excel;
        }
        
        // PowerPoint presentations
        if (mimeType.equals("application/vnd.ms-powerpoint") || 
            mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
            return R.drawable.ic_file_powerpoint;
        }
        
        // Archive files (zip, rar, 7z, etc.)
        if (mimeType.equals("application/zip") || 
            mimeType.equals("application/x-zip-compressed") ||
            mimeType.equals("application/x-rar-compressed") ||
            mimeType.equals("application/x-7z-compressed") ||
            mimeType.equals("application/x-tar") ||
            mimeType.equals("application/gzip")) {
            return R.drawable.ic_file_archive;
        }
        
        // Text files
        if (mimeType.startsWith("text/")) {
            return R.drawable.ic_document;
        }
        
        // Default for other files
        return R.drawable.ic_document;
    }
    
    /**
     * Get file extension from filename
     * @param filename File name
     * @return Extension (without dot) or empty string
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        
        return "";
    }
}
