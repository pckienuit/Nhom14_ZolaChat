package com.example.doan_zaloclone.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.example.doan_zaloclone.R;

/**
 * Glide configuration for optimized image loading
 */
@GlideModule
public class AppGlideConfiguration extends AppGlideModule {
    
    private static final int DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final int MEMORY_CACHE_SIZE = 20 * 1024 * 1024; // 20 MB
    
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Set memory cache
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_SIZE));
        
        // Set disk cache
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE));
        
        // Use RGB_565 format to reduce memory usage (50% less than ARGB_8888)
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        );
    }
    
    @Override
    public boolean isManifestParsingEnabled() {
        // Disable manifest parsing for better performance
        return false;
    }
}
