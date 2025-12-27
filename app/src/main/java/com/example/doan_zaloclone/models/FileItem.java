package com.example.doan_zaloclone.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Wrapper model for messages containing files
 * Adds sender information for display in file management UI
 */
public class FileItem {
    // URL pattern for link extraction
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s]+)",
            Pattern.CASE_INSENSITIVE
    );
    private Message message;
    private String senderName;
    private String senderAvatarUrl;

    public FileItem(Message message) {
        this.message = message;
    }

    public FileItem(Message message, String senderName, String senderAvatarUrl) {
        this.message = message;
        this.senderName = senderName;
        this.senderAvatarUrl = senderAvatarUrl;
    }

    // Getters
    public Message getMessage() {
        return message;
    }

    // Setters
    public void setMessage(Message message) {
        this.message = message;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    /**
     * Determine the category type based on message type and MIME type
     */
    public FileCategory getCategoryType() {
        if (message == null) return FileCategory.FILES;

        String type = message.getType();
        String mimeType = message.getFileMimeType();

        // Images can be either TYPE_IMAGE or TYPE_FILE with image MIME type
        if (Message.TYPE_IMAGE.equals(type)) {
            return FileCategory.MEDIA;
        }

        // Files with image or video MIME type are MEDIA
        if (Message.TYPE_FILE.equals(type)) {
            if (mimeType != null) {
                if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                    return FileCategory.MEDIA;
                }
            }
            return FileCategory.FILES;
        }

        // Text messages with URLs are LINKS
        if (Message.TYPE_TEXT.equals(type)) {
            String content = message.getContent();
            if (content != null && URL_PATTERN.matcher(content).find()) {
                return FileCategory.LINKS;
            }
            // TEXT messages without URLs should not be categorized as FILES
            // They should have been filtered out in the repository
            // This is a fallback that shouldn't normally be reached
        }

        return FileCategory.FILES;
    }

    /**
     * Get display name for the file
     * For images/files: use fileName
     * For links: extract domain from URL
     */
    public String getDisplayName() {
        if (message == null) return "Unknown";

        FileCategory category = getCategoryType();

        if (category == FileCategory.LINKS) {
            String content = message.getContent();
            if (content != null) {
                // Extract domain from URL
                try {
                    java.util.regex.Matcher matcher = URL_PATTERN.matcher(content);
                    if (matcher.find()) {
                        String url = matcher.group(1);
                        // Simple domain extraction
                        url = url.replaceFirst("https?://", "");
                        int slashIndex = url.indexOf('/');
                        if (slashIndex > 0) {
                            url = url.substring(0, slashIndex);
                        }
                        return url;
                    }
                } catch (Exception e) {
                    return "Link";
                }
            }
            return "Link";
        }

        // For files and media
        String fileName = message.getFileName();
        if (fileName != null && !fileName.isEmpty()) {
            return fileName;
        }

        // Fallback to type
        if (message.isImageMessage()) {
            return "Image";
        } else if (message.isFileMessage()) {
            return "File";
        }

        return "Unknown";
    }

    /**
     * Get formatted date for display
     */
    public String getFormattedDate() {
        if (message == null) return "";

        long timestamp = message.getTimestamp();
        Date date = new Date(timestamp);

        // Check if today
        Date now = new Date();
        SimpleDateFormat todayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (todayFormat.format(date).equals(todayFormat.format(now))) {
            // Show time only
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(date);
        }

        // Check if this year
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        if (yearFormat.format(date).equals(yearFormat.format(now))) {
            // Show date without year
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            return dateFormat.format(date);
        }

        // Show full date
        return todayFormat.format(date);
    }

    /**
     * Get file URL for images or files
     */
    public String getFileUrl() {
        if (message == null) return null;
        return message.getContent();
    }

    /**
     * Get extracted URL for link items
     */
    public String getExtractedUrl() {
        if (message == null || !Message.TYPE_TEXT.equals(message.getType())) {
            return null;
        }

        String content = message.getContent();
        if (content != null) {
            java.util.regex.Matcher matcher = URL_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Get domain from URL (for link items)
     */
    public String getDomain() {
        String url = getExtractedUrl();
        if (url == null) return null;

        try {
            // Remove protocol
            String domain = url.replaceFirst("^https?://", "");
            // Remove www. prefix if present
            domain = domain.replaceFirst("^www\\.", "");
            // Remove path (everything after first /)
            int slashIndex = domain.indexOf('/');
            if (slashIndex > 0) {
                domain = domain.substring(0, slashIndex);
            }
            // Remove port if present
            int colonIndex = domain.indexOf(':');
            if (colonIndex > 0) {
                domain = domain.substring(0, colonIndex);
            }
            return domain.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if this is a video file
     */
    public boolean isVideo() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * Check if this is an image file
     */
    public boolean isImage() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        String type = message.getType();
        return Message.TYPE_IMAGE.equals(type) ||
                (mimeType != null && mimeType.startsWith("image/"));
    }

    /**
     * Check if this is a PDF file
     */
    public boolean isPdf() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        String fileName = message.getFileName();
        return (mimeType != null && mimeType.contains("pdf")) ||
                (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    /**
     * Check if this is a Word document
     */
    public boolean isWord() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        String fileName = message.getFileName();
        return (mimeType != null && (mimeType.contains("word") || mimeType.contains("msword") ||
                mimeType.contains("officedocument.wordprocessing"))) ||
                (fileName != null && (fileName.toLowerCase().endsWith(".doc") ||
                        fileName.toLowerCase().endsWith(".docx")));
    }

    /**
     * Check if this is an Excel spreadsheet
     */
    public boolean isExcel() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        String fileName = message.getFileName();
        return (mimeType != null && (mimeType.contains("excel") || mimeType.contains("spreadsheet") ||
                mimeType.contains("ms-excel"))) ||
                (fileName != null && (fileName.toLowerCase().endsWith(".xls") ||
                        fileName.toLowerCase().endsWith(".xlsx")));
    }

    /**
     * Check if this is a PowerPoint presentation
     */
    public boolean isPowerPoint() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        String fileName = message.getFileName();
        return (mimeType != null && (mimeType.contains("powerpoint") || mimeType.contains("presentation") ||
                mimeType.contains("ms-powerpoint"))) ||
                (fileName != null && (fileName.toLowerCase().endsWith(".ppt") ||
                        fileName.toLowerCase().endsWith(".pptx")));
    }

    /**
     * Check if this is an archive file (zip, rar, 7z, etc.)
     */
    public boolean isArchive() {
        if (message == null) return false;
        String mimeType = message.getFileMimeType();
        String fileName = message.getFileName();
        boolean isMimeArchive = mimeType != null && (mimeType.contains("zip") ||
                mimeType.contains("rar") || mimeType.contains("compress") ||
                mimeType.contains("archive"));
        boolean isFileArchive = fileName != null && (fileName.toLowerCase().endsWith(".zip") ||
                fileName.toLowerCase().endsWith(".rar") || fileName.toLowerCase().endsWith(".7z") ||
                fileName.toLowerCase().endsWith(".tar") || fileName.toLowerCase().endsWith(".gz"));
        return isMimeArchive || isFileArchive;
    }

    /**
     * Get file type label (e.g., "PDF", "DOC", "MP4")
     */
    public String getFileTypeLabel() {
        if (message == null) return "";

        String fileName = message.getFileName();
        if (fileName != null && fileName.contains(".")) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            return extension.toUpperCase(Locale.getDefault());
        }

        // Fallback to MIME type
        String mimeType = message.getFileMimeType();
        if (mimeType != null) {
            if (mimeType.contains("pdf")) return "PDF";
            if (mimeType.contains("word")) return "DOC";
            if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) return "XLSX";
            if (mimeType.contains("zip")) return "ZIP";
            if (mimeType.contains("image")) return "IMG";
            if (mimeType.contains("video")) return "VID";
        }

        return "FILE";
    }

    /**
     * Get formatted file size
     */
    public String getFormattedFileSize() {
        if (message == null) return "";
        return message.getFormattedFileSize();
    }
}
