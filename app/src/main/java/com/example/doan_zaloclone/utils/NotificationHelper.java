package com.example.doan_zaloclone.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.contact.FriendRequestActivity;
import com.example.doan_zaloclone.ui.room.RoomActivity;

/**
 * Centralized notification helper for all app notifications
 * Manages notification channels and displays notifications for:
 * - Chat messages (new, recalled, reactions)
 * - Friend requests (received, accepted)
 * - Calls (handled separately by CallNotificationHelper)
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    // ========== Notification Channel IDs ==========
    public static final String CHANNEL_ID_MESSAGES = "messages_channel";
    public static final String CHANNEL_ID_FRIEND_REQUESTS = "friend_requests_channel";
    public static final String CHANNEL_ID_SERVICE = "notification_service_channel";

    // ========== Notification IDs ==========
    public static final int NOTIFICATION_ID_NEW_MESSAGE = 2001;
    public static final int NOTIFICATION_ID_MESSAGE_RECALLED = 2002;
    public static final int NOTIFICATION_ID_MESSAGE_REACTION = 2003;
    public static final int NOTIFICATION_ID_FRIEND_REQUEST = 3001;
    public static final int NOTIFICATION_ID_FRIEND_ACCEPTED = 3002;
    public static final int NOTIFICATION_ID_SERVICE = 9999;

    /**
     * Create all notification channels (Call for API 26+)
     * Should be called when app starts (e.g., in MainActivity.onCreate)
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Messages Channel
            NotificationChannel messagesChannel = new NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    "Tin nhắn",
                    NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Thông báo tin nhắn mới, thu hồi, và phản ứng");
            messagesChannel.enableVibration(true);
            messagesChannel.setShowBadge(true);

            // Friend Requests Channel
            NotificationChannel friendRequestsChannel = new NotificationChannel(
                    CHANNEL_ID_FRIEND_REQUESTS,
                    "Lời mời kết bạn",
                    NotificationManager.IMPORTANCE_HIGH
            );
            friendRequestsChannel.setDescription("Thông báo lời mời kết bạn");
            friendRequestsChannel.enableVibration(true);
            friendRequestsChannel.setShowBadge(true);

            // Service Channel (low priority, for foreground service)
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID_SERVICE,
                    "Dịch vụ nền",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Dịch vụ lắng nghe thông báo");
            serviceChannel.setShowBadge(false);

            // Register channels
            manager.createNotificationChannel(messagesChannel);
            manager.createNotificationChannel(friendRequestsChannel);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    // ==================== Chat Notifications ====================

    /**
     * Show notification for new message
     *
     * @param context        Context
     * @param senderName     Name of the sender
     * @param messageText    Message content
     * @param conversationId Conversation ID (to open RoomActivity)
     * @param senderAvatar   Sender avatar bitmap (optional, can be null)
     */
    public static void showMessageNotification(Context context, String senderName,
                                                String messageText, String conversationId,
                                                Bitmap senderAvatar) {
        // Intent to open RoomActivity when notification is clicked
        Intent intent = new Intent(context, RoomActivity.class);
        intent.putExtra("CONVERSATION_ID", conversationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(senderName)
                .setContentText(messageText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Add large icon if avatar is provided
        if (senderAvatar != null) {
            builder.setLargeIcon(senderAvatar);
        }

        // Show notification
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_NEW_MESSAGE, builder.build());
        }
    }

    /**
     * Show notification for recalled message
     *
     * @param context        Context
     * @param senderName     Name of the sender who recalled the message
     * @param conversationId Conversation ID
     */
    public static void showMessageRecallNotification(Context context, String senderName,
                                                      String conversationId) {
        Intent intent = new Intent(context, RoomActivity.class);
        intent.putExtra("CONVERSATION_ID", conversationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(senderName)
                .setContentText("đã thu hồi một tin nhắn")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_MESSAGE_RECALLED, builder.build());
        }
    }

    /**
     * Show notification for message reaction
     *
     * @param context        Context
     * @param senderName     Name of the person who reacted
     * @param reaction       Reaction emoji
     * @param conversationId Conversation ID
     */
    public static void showMessageReactionNotification(Context context, String senderName,
                                                        String reaction, String conversationId) {
        Intent intent = new Intent(context, RoomActivity.class);
        intent.putExtra("CONVERSATION_ID", conversationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(senderName)
                .setContentText("đã thả cảm xúc " + reaction + " vào tin nhắn của bạn")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_MESSAGE_REACTION, builder.build());
        }
    }

    // ==================== Friend Request Notifications ====================

    /**
     * Show notification for new friend request
     *
     * @param context      Context
     * @param senderName   Name of the sender
     * @param senderId     Sender user ID
     * @param senderAvatar Sender avatar bitmap (optional, can be null)
     */
    public static void showFriendRequestNotification(Context context, String senderName,
                                                      String senderId, Bitmap senderAvatar) {
        Intent intent = new Intent(context, FriendRequestActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_FRIEND_REQUESTS)
                .setSmallIcon(R.drawable.ic_person_add)
                .setContentTitle("Lời mời kết bạn")
                .setContentText(senderName + " đã gửi lời mời kết bạn cho bạn")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (senderAvatar != null) {
            builder.setLargeIcon(senderAvatar);
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_FRIEND_REQUEST, builder.build());
        }
    }

    /**
     * Show notification when friend request is accepted
     *
     * @param context Context
     * @param userName Name of the user who accepted the request
     * @param userId User ID
     */
    public static void showFriendAcceptedNotification(Context context, String userName,
                                                       String userId) {
        Intent intent = new Intent(context, FriendRequestActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_FRIEND_REQUESTS)
                .setSmallIcon(R.drawable.ic_person_add)
                .setContentTitle("Lời mời kết bạn")
                .setContentText(userName + " đã chấp nhận lời mời kết bạn của bạn")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_FRIEND_ACCEPTED, builder.build());
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Cancel a notification by ID
     *
     * @param context        Context
     * @param notificationId Notification ID to cancel
     */
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(notificationId);
        }
    }

    /**
     * Cancel all notifications
     *
     * @param context Context
     */
    public static void cancelAllNotifications(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancelAll();
        }
    }

    /**
     * Create a simple persistent notification for foreground service
     *
     * @param context Context
     * @return Notification object
     */
    public static Notification createServiceNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle("Zalo Clone")
                .setContentText("Đang lắng nghe thông báo")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }
}
