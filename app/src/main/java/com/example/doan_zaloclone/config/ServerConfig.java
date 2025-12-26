package com.example.doan_zaloclone.config;

import com.example.doan_zaloclone.BuildConfig;

/**
 * Centralized Server Configuration
 * Uses BuildConfig values based on selected product flavor (development/production)
 */
public final class ServerConfig {
    
    // API Base URL
    public static final String API_BASE_URL = BuildConfig.API_BASE_URL;
    
    // WebSocket URL
    public static final String SOCKET_URL = BuildConfig.SOCKET_URL;
    
    // VPS URL for sticker uploads
    public static final String VPS_BASE_URL = BuildConfig.VPS_BASE_URL;
    
    // Derived URLs
    public static final String VPS_UPLOAD_URL = VPS_BASE_URL + "/api/stickers/upload";
    public static final String VPS_CREATE_PACK_URL = VPS_BASE_URL + "/api/stickers/packs";
    
    /**
     * Check if running in development mode
     */
    public static boolean isDevelopment() {
        return BuildConfig.DEBUG && API_BASE_URL.contains("10.0.2.2");
    }
    
    private ServerConfig() {
        // Prevent instantiation
    }
}
