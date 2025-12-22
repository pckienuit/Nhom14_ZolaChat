package com.example.doan_zaloclone.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.LiveLocation;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LocationSharingService extends Service {
    private static final String TAG = "LocationSharingService";
    private static final String CHANNEL_ID = "channel_location_sharing";
    private static final int NOTIFICATION_ID = 123456;
    
    public static final String ACTION_START_SHARING = "action_start_sharing";
    public static final String ACTION_STOP_SHARING = "action_stop_sharing";
    
    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_DURATION = "extra_duration";
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ChatRepository chatRepository;
    private Handler stopHandler;
    private String sessionId;
    private String currentUserId;
    private long endTime;
    
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        chatRepository = new ChatRepository();
        stopHandler = new Handler(Looper.getMainLooper());
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SHARING.equals(action)) {
                sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
                long duration = intent.getLongExtra(EXTRA_DURATION, 15 * 60 * 1000); // Default 15 mins
                
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    currentUserId = user.getUid();
                }
                
                if (sessionId != null && currentUserId != null) {
                    startLocationUpdates();
                    
                    // Schedule auto-stop
                    endTime = System.currentTimeMillis() + duration;
                    stopHandler.postDelayed(this::stopSharing, duration);
                    
                    // Update initial status
                    startForeground(NOTIFICATION_ID, createNotification());
                }
            } else if (ACTION_STOP_SHARING.equals(action)) {
                stopSharing();
            }
        }
        return START_STICKY;
    }
    
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
                
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationToFirestore(location);
                }
            }
        };
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
    
    private void updateLocationToFirestore(Location location) {
        if (sessionId == null || currentUserId == null) return;
        
        LiveLocation liveLocation = new LiveLocation(
            sessionId,
            currentUserId,
            location.getLatitude(),
            location.getLongitude(),
            System.currentTimeMillis(),
            endTime
        );
        
        chatRepository.updateLiveLocation(liveLocation);
    }
    
    private void stopSharing() {
        if (sessionId != null) {
            chatRepository.stopLiveLocation(sessionId);
        }
        stopLocationUpdates();
        stopForeground(true);
        stopSelf();
    }
    
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (stopHandler != null) {
            stopHandler.removeCallbacksAndMessages(null);
        }
    }
    
    private Notification createNotification() {
        Intent stopIntent = new Intent(this, LocationSharingService.class);
        stopIntent.setAction(ACTION_STOP_SHARING);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đang chia sẻ vị trí trực tiếp")
                .setContentText("Vị trí của bạn đang được cập nhật...")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dừng chia sẻ", stopPendingIntent);
                
        return builder.build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Live Location Sharing",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }
}
