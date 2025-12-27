package com.example.doan_zaloclone;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.cloudinary.android.MediaManager;
import com.example.doan_zaloclone.utils.AppLifecycleObserver;

import java.util.HashMap;
import java.util.Map;

public class ZaloApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary (credentials injected from local.properties)
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
        config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);

        MediaManager.init(this, config);

        // Register lifecycle observer for real-time presence tracking
        ProcessLifecycleOwner.get().getLifecycle()
                .addObserver(new AppLifecycleObserver());
    }
}
