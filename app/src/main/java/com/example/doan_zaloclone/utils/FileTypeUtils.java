package com.example.doan_zaloclone.utils;

/**
 * Utility for getting file type label from MIME type
 */
public class FileTypeUtils {
    
    /**
     * Get readable file type from MIME type
     * @param mimeType MIME type string
     * @return File type label (e.g., "PDF", "Word", "Image")
     */
    public static String getFileTypeLabel(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return "File";
        }
        
        // Images
        if (mimeType.startsWith("image/")) {
            if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return "JPG";
            if (mimeType.contains("png")) return "PNG";
            if (mimeType.contains("gif")) return "GIF";
            if (mimeType.contains("webp")) return "WebP";
            return "Image";
        }
        
        // Videos
        if (mimeType.startsWith("video/")) {
            if (mimeType.contains("mp4")) return "MP4";
            if (mimeType.contains("avi")) return "AVI";
            if (mimeType.contains("mkv")) return "MKV";
            return "Video";
        }
        
        // Audio
        if (mimeType.startsWith("audio/")) {
            if (mimeType.contains("mp3")) return "MP3";
            if (mimeType.contains("wav")) return "WAV";
            return "Audio";
        }
        
        // Documents
        if (mimeType.equals("application/pdf")) return "PDF";
        
        // Microsoft Office
        if (mimeType.contains("msword") || mimeType.contains("wordprocessingml")) return "Word";
        if (mimeType.contains("ms-excel") || mimeType.contains("spreadsheetml")) return "Excel";
        if (mimeType.contains("ms-powerpoint") || mimeType.contains("presentationml")) return "PowerPoint";
        
        // Text
        if (mimeType.startsWith("text/")) {
            if (mimeType.contains("plain")) return "Text";
            if (mimeType.contains("html")) return "HTML";
            if (mimeType.contains("csv")) return "CSV";
            return "Text";
        }
        
        // Archives
        if (mimeType.contains("zip")) return "ZIP";
        if (mimeType.contains("rar")) return "RAR";
        if (mimeType.contains("7z")) return "7Z";
        
        return "File";
    }
}
