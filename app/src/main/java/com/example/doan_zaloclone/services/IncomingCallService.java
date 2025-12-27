package com.example.doan_zaloclone.services;

import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.doan_zaloclone.models.Call;
import com.example.doan_zaloclone.repository.CallRepository;
import com.example.doan_zaloclone.ui.call.CallActivity;
import com.example.doan_zaloclone.utils.CallNotificationHelper;
import com.google.firebase.firestore.ListenerRegistration;

public class IncomingCallService extends Service {
    private static final String TAG = "IncomingCallService";
    private static final int NOTIFICATION_ID = 1001;

    private CallRepository callRepository;
    private ListenerRegistration callListener;
    private Ringtone ringtone;
    private Vibrator vibrator;
    private String currentCallId;

    @Override
    public void onCreate() {
        super.onCreate();
        callRepository = new CallRepository();

        // Create notification channel
        CallNotificationHelper.createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        currentCallId = intent.getStringExtra("call_id");

        if (action == null) {
            // Show incoming call notification
            showIncomingCallNotification(intent);
        } else if ("ACTION_ACCEPT_CALL".equals(action)) {
            acceptCall();
        } else if ("ACTION_REJECT_CALL".equals(action)) {
            rejectCall();
        } else if ("ACTION_CANCEL_CALL".equals(action)) {
            stopService();
        }

        return START_NOT_STICKY;
    }

    private void showIncomingCallNotification(Intent intent) {
        String callerName = intent.getStringExtra("caller_name");
        boolean isVideo = intent.getBooleanExtra("is_video", false);
        String callerId = intent.getStringExtra("caller_id");
        String conversationId = intent.getStringExtra("conversation_id");
        String receiverId = intent.getStringExtra("receiver_id");

        // Create call object for notification
        Call call = new Call();
        call.setId(currentCallId);
        call.setType(isVideo ? Call.TYPE_VIDEO : Call.TYPE_VOICE);

        // Start foreground with notification
        // Use SPECIAL_USE type for incoming call notification (Android 14+ compatible)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires SPECIAL_USE
            startForeground(NOTIFICATION_ID,
                    CallNotificationHelper.createIncomingCallNotification(this, call, callerName, null),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            Log.d(TAG, "Started with foreground service type: specialUse (Android 14+)");
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID,
                    CallNotificationHelper.createIncomingCallNotification(this, call, callerName, null),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
            Log.d(TAG, "Started with foreground service type: phoneCall");
        } else {
            startForeground(NOTIFICATION_ID,
                    CallNotificationHelper.createIncomingCallNotification(this, call, callerName, null));
        }

        // Start ringtone
        playRingtone();

        // Start vibration
        startVibration();
        
        // AUTO-LAUNCH CallActivity for full-screen experience
        // This ensures the call screen pops up immediately, not just notification
        launchCallActivity(callerId, callerName, conversationId, receiverId, isVideo);

        // Listen to call updates (auto-dismiss if caller cancels)
        listenToCallUpdates();
    }
    
    /**
     * Launch CallActivity to show full-screen incoming call
     */
    private void launchCallActivity(String callerId, String callerName, String conversationId, String receiverId, boolean isVideo) {
        // Wake up the device screen
        android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        android.os.PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.FULL_WAKE_LOCK |
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP |
                android.os.PowerManager.ON_AFTER_RELEASE,
                "ZaloClone:IncomingCall"
        );
        wakeLock.acquire(60 * 1000L); // 60 seconds timeout
        
        // Check if we can draw overlays (required for Android 10+)
        boolean canDrawOverlays = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            canDrawOverlays = android.provider.Settings.canDrawOverlays(this);
        }
        
        if (!canDrawOverlays) {
            Log.w(TAG, "Cannot draw overlays - will rely on notification full-screen intent");
            // Don't launch activity directly, let notification handle it
            return;
        }
        
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("call_id", currentCallId);
        callIntent.putExtra("caller_id", callerId);
        callIntent.putExtra("caller_name", callerName);
        callIntent.putExtra("conversation_id", conversationId);
        callIntent.putExtra("receiver_id", receiverId);
        callIntent.putExtra("is_incoming", true);
        callIntent.putExtra("is_video", isVideo);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP |
                           Intent.FLAG_ACTIVITY_SINGLE_TOP |
                           Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        
        // Add window flags for lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        
        try {
            startActivity(callIntent);
            Log.d(TAG, "Launched CallActivity for incoming call successfully");
            
            // Replace with silent notification since full-screen activity is showing
            // We can't cancel the notification (would stop foreground service)
            // Instead, update to a low-priority silent notification
            android.app.NotificationManager notificationManager = 
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            
            // Create a minimal silent notification to keep foreground service alive
            // Use the same channel as CallNotificationHelper
            android.app.Notification silentNotification = new android.app.Notification.Builder(this, 
                    "call_channel")
                    .setSmallIcon(com.example.doan_zaloclone.R.drawable.ic_call)
                    .setContentTitle("Cuộc gọi đang đến...")
                    .setOngoing(true)
                    .setSound(null)  // No sound
                    .setVibrate(null)  // No vibration
                    .setPriority(android.app.Notification.PRIORITY_MIN)
                    .build();
            
            notificationManager.notify(NOTIFICATION_ID, silentNotification);
            Log.d(TAG, "Notification replaced with silent version - full-screen activity is showing");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch CallActivity", e);
            // Keep original notification if activity launch failed
        } finally {
            // Release wake lock shortly after
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void playRingtone() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
            ringtone.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone", e);
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 1000}; // Wait 0ms, vibrate 1s, wait 1s
        vibrator.vibrate(pattern, 0); // Repeat
    }

    private void stopRingtoneAndVibration() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void listenToCallUpdates() {
        if (currentCallId == null) return;

        callListener = callRepository.listenToCall(currentCallId, new CallRepository.OnCallChangedListener() {
            @Override
            public void onCallChanged(Call call) {
                // If call ended/rejected/missed/cancelled, stop service
                if (call.isEnded() ||
                        Call.STATUS_MISSED.equals(call.getStatus()) ||
                        Call.STATUS_ENDED.equals(call.getStatus())) {
                    stopService();
                }
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error listening to call: " + errorMsg);
            }
        });
    }

    private void acceptCall() {
        stopRingtoneAndVibration();

        // Open CallActivity
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("CALL_ID", currentCallId);
        callIntent.putExtra("IS_INCOMING", true);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callIntent);

        stopService();
    }

    private void rejectCall() {
        if (currentCallId != null) {
            callRepository.updateCallStatus(currentCallId, Call.STATUS_REJECTED,
                    new CallRepository.OnCallUpdatedListener() {
                        @Override
                        public void onSuccess() {
                            stopService();
                        }

                        @Override
                        public void onError(String errorMsg) {
                            stopService();
                        }
                    });
        } else {
            stopService();
        }
    }

    private void stopService() {
        stopRingtoneAndVibration();

        if (callListener != null) {
            callListener.remove();
        }

        CallNotificationHelper.cancelNotification(this, NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRingtoneAndVibration();
        if (callListener != null) {
            callListener.remove();
        }
    }
}
