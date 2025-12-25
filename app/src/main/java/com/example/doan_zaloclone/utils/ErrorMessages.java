package com.example.doan_zaloclone.utils;

/**
 * User-friendly error messages for common API errors
 * Provides consistent, localized error messages across the app
 */
public class ErrorMessages {

    // Network Errors
    public static final String NETWORK_ERROR = "Không có kết nối mạng. Vui lòng kiểm tra và thử lại.";
    public static final String TIMEOUT_ERROR = "Kết nối quá chậm. Vui lòng thử lại.";
    public static final String SERVER_ERROR = "Lỗi máy chủ. Vui lòng thử lại sau.";
    
    // Authentication Errors
    public static final String AUTH_ERROR = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    public static final String PERMISSION_DENIED = "Bạn không có quyền thực hiện thao tác này.";
    
    // Data Errors
    public static final String NOT_FOUND = "Không tìm thấy dữ liệu.";
    public static final String INVALID_DATA = "Dữ liệu không hợp lệ.";
    public static final String DUPLICATE_DATA = "Dữ liệu đã tồn tại.";
    
    // Operation Errors
    public static final String UPLOAD_FAILED = "Tải lên thất bại. Vui lòng thử lại.";
    public static final String DOWNLOAD_FAILED = "Tải xuống thất bại. Vui lòng thử lại.";
    public static final String SEND_FAILED = "Gửi tin nhắn thất bại. Vui lòng thử lại.";
    public static final String DELETE_FAILED = "Xóa thất bại. Vui lòng thử lại.";
    public static final String UPDATE_FAILED = "Cập nhật thất bại. Vui lòng thử lại.";
    
    // Generic
    public static final String UNKNOWN_ERROR = "Đã xảy ra lỗi. Vui lòng thử lại.";
    public static final String TRY_AGAIN = "Vui lòng thử lại sau.";

    /**
     * Get user-friendly error message based on HTTP status code
     */
    public static String fromHttpCode(int code) {
        switch (code) {
            case 400:
                return INVALID_DATA;
            case 401:
            case 403:
                return AUTH_ERROR;
            case 404:
                return NOT_FOUND;
            case 408:
                return TIMEOUT_ERROR;
            case 409:
                return DUPLICATE_DATA;
            case 500:
            case 502:
            case 503:
            case 504:
                return SERVER_ERROR;
            default:
                return UNKNOWN_ERROR;
        }
    }

    /**
     * Get user-friendly error message from exception
     */
    public static String fromException(Throwable t) {
        if (t == null) return UNKNOWN_ERROR;
        
        String message = t.getMessage();
        if (message == null) return UNKNOWN_ERROR;
        
        // Network-related errors
        if (message.contains("Unable to resolve host") || 
            message.contains("No address associated") ||
            message.contains("failed to connect")) {
            return NETWORK_ERROR;
        }
        
        if (message.contains("timeout") || message.contains("timed out")) {
            return TIMEOUT_ERROR;
        }
        
        // Firestore-related errors
        if (message.contains("PERMISSION_DENIED")) {
            return PERMISSION_DENIED;
        }
        
        if (message.contains("NOT_FOUND")) {
            return NOT_FOUND;
        }
        
        // Default to server error for unhandled cases
        return SERVER_ERROR;
    }

    /**
     * Check if error is retryable
     */
    public static boolean isRetryable(Throwable t) {
        if (t == null) return false;
        
        String message = t.getMessage();
        if (message == null) return false;
        
        // Retry on network and timeout errors
        return message.contains("timeout") || 
               message.contains("failed to connect") ||
               message.contains("Unable to resolve host");
    }

    /**
     * Check if HTTP error is retryable
     */
    public static boolean isRetryableHttpError(int code) {
        // Retry on server errors and timeout, but not on client errors
        return code == 408 || code == 429 || (code >= 500 && code < 600);
    }
}
