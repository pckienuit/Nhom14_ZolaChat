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
        
        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "do0mdssvu");
        config.put("api_key", "248235972111755");
        config.put("api_secret", "9hnWcqIA7en-XduIpo1f3C1CdIg");
        
        MediaManager.init(this, config);
        
        // Register lifecycle observer for real-time presence tracking
        ProcessLifecycleOwner.get().getLifecycle()
                .addObserver(new AppLifecycleObserver());
    }
}
