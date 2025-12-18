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
        currentCallId = intent.getStringExtra("CALL_ID");
        
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
        String callerName = intent.getStringExtra("CALLER_NAME");
        boolean isVideo = intent.getBooleanExtra("IS_VIDEO", false);
        
        // Create call object for notification
        Call call = new Call();
        call.setId(currentCallId);
        call.setType(isVideo ? Call.TYPE_VIDEO : Call.TYPE_VOICE);
        
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, 
            CallNotificationHelper.createIncomingCallNotification(this, call, callerName, null));
        
        // Start ringtone
        playRingtone();
        
        // Start vibration
        startVibration();
        
        // Listen to call updates (auto-dismiss if caller cancels)
        listenToCallUpdates();
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
