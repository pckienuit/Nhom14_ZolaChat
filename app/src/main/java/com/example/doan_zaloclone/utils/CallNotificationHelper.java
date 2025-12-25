package com.example.doan_zaloclone.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Call;
import com.example.doan_zaloclone.ui.call.CallActivity;

public class CallNotificationHelper {
    private static final String CHANNEL_ID = "call_channel";
    private static final String CHANNEL_NAME = "Cuộc gọi";
    private static final int INCOMING_CALL_NOTIFICATION_ID = 1001;
    private static final int ONGOING_CALL_NOTIFICATION_ID = 1002;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo cuộc gọi đến");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Create full-screen incoming call notification
     */
    public static Notification createIncomingCallNotification(
            Context context,
            Call call,
            String callerName,
            String callerAvatar) {

        // Full screen intent
        Intent fullScreenIntent = new Intent(context, CallActivity.class);
        fullScreenIntent.putExtra("CALL_ID", call.getId());
        fullScreenIntent.putExtra("IS_INCOMING", true);
        fullScreenIntent.putExtra("IS_VIDEO", call.isVideoCall());
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Accept action
        Intent acceptIntent = new Intent(context, com.example.doan_zaloclone.services.IncomingCallService.class);
        acceptIntent.setAction("ACTION_ACCEPT_CALL");
        acceptIntent.putExtra("CALL_ID", call.getId());
        PendingIntent acceptPendingIntent = PendingIntent.getService(
                context, 1, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Reject action
        Intent rejectIntent = new Intent(context, com.example.doan_zaloclone.services.IncomingCallService.class);
        rejectIntent.setAction("ACTION_REJECT_CALL");
        rejectIntent.putExtra("CALL_ID", call.getId());
        PendingIntent rejectPendingIntent = PendingIntent.getService(
                context, 2, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle(callerName)
                .setContentText(call.isVideoCall() ? "Cuộc gọi video đến..." : "Cuộc gọi thoại đến...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .addAction(R.drawable.ic_close, "Từ chối", rejectPendingIntent)
                .addAction(R.drawable.ic_call, "Chấp nhận", acceptPendingIntent);

        return builder.build();
    }

    /**
     * Create ongoing call notification
     */
    public static Notification createOngoingCallNotification(Context context, Call call, String userName) {
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra("CALL_ID", call.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("Cuộc gọi đang diễn ra")
                .setContentText(userName)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancel(notificationId);
    }

    public static int getIncomingCallNotificationId() {
        return INCOMING_CALL_NOTIFICATION_ID;
    }

    public static int getOngoingCallNotificationId() {
        return ONGOING_CALL_NOTIFICATION_ID;
    }
}
