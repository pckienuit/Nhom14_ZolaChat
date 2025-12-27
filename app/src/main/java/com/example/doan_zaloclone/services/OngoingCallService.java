package com.example.doan_zaloclone.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.call.CallActivity;

/**
 * Foreground Service to maintain ongoing call connection
 * Prevents Android from killing the app during calls when in background
 */
public class OngoingCallService extends Service {
    // Intent extras
    public static final String EXTRA_CALL_ID = "call_id";
    public static final String EXTRA_CALLER_NAME = "caller_name";
    public static final String EXTRA_IS_VIDEO = "is_video";
    // Actions
    public static final String ACTION_START_SERVICE = "ACTION_START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    public static final String ACTION_UPDATE_DURATION = "ACTION_UPDATE_DURATION";
    private static final String TAG = "OngoingCallService";
    private static final int NOTIFICATION_ID = 1002;
    private static final String CHANNEL_ID = "ongoing_call_channel";
    // Binder for Activity to interact with Service
    private final IBinder binder = new LocalBinder();
    private String currentCallId;
    private String callerName;
    private boolean isVideo;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OngoingCallService created");

        // Create notification channel
        createNotificationChannel();

        // Acquire WakeLock to prevent CPU from sleeping during call
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ZaloClone:OngoingCallWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes max*/);
        Log.d(TAG, "WakeLock acquired");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_START_SERVICE.equals(action)) {
            currentCallId = intent.getStringExtra(EXTRA_CALL_ID);
            callerName = intent.getStringExtra(EXTRA_CALLER_NAME);
            isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false);

            Log.d(TAG, "Starting foreground service for call: " + currentCallId + " (video=" + isVideo + ")");

            // Start foreground with notification
            Notification notification = createOngoingCallNotification("00:00");

            // Conditionally set foreground service type based on call type
            // Voice calls only need microphone, video calls need both camera and microphone
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int serviceType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
                if (isVideo) {
                    serviceType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
                }
                startForeground(NOTIFICATION_ID, notification, serviceType);
                Log.d(TAG, "Started with foreground service type: " +
                        (isVideo ? "camera|microphone" : "microphone"));
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

        } else if (ACTION_UPDATE_DURATION.equals(action)) {
            String duration = intent.getStringExtra("duration");
            updateNotification(duration);

        } else if (ACTION_STOP_SERVICE.equals(action)) {
            Log.d(TAG, "Stopping ongoing call service");
            stopForeground(true);
            stopSelf();
        }

        // If service is killed, don't restart it
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Create notification for ongoing call
     * Hidden from notification tray but keeps service alive
     */
    private Notification createOngoingCallNotification(String duration) {
        // Intent to return to CallActivity when notification is tapped
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra(CallActivity.EXTRA_CALL_ID, currentCallId);
        callIntent.putExtra(CallActivity.EXTRA_IS_VIDEO, isVideo);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build minimal silent notification (hidden from notification tray)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Cuộc gọi")
                .setContentText("Đang kết nối...")
                .setSmallIcon(R.drawable.ic_call)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)  // Minimal priority
                .setSound(null)
                .setVibrate(null)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        return builder.build();
    }

    /**
     * Update notification with new duration
     */
    private void updateNotification(String duration) {
        if (currentCallId == null) return;

        Notification notification = createOngoingCallNotification(duration);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Create notification channel for Android O+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cuộc gọi đang diễn ra",
                    NotificationManager.IMPORTANCE_LOW  // Low importance = hidden from notification tray
            );
            channel.setDescription("Duy trì kết nối cuộc gọi");
            channel.setSound(null, null);  // No sound for this channel
            channel.enableVibration(false);
            channel.setShowBadge(false);  // No badge

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Release WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }

        Log.d(TAG, "OngoingCallService destroyed");
    }

    /**
     * Public method to update call duration from Activity
     */
    public void updateDuration(String duration) {
        updateNotification(duration);
    }

    public class LocalBinder extends Binder {
        public OngoingCallService getService() {
            return OngoingCallService.this;
        }
    }
}
