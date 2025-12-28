package com.example.doan_zaloclone.ui.room;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;

/**
 * Custom ItemTouchHelper.Callback for swipe-to-reply gesture
 * - Sent messages: swipe LEFT only
 * - Received messages: swipe RIGHT only
 */
public class SwipeToReplyCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final SwipeToReplyListener listener;
    private final MessageAdapter adapter;
    private final Drawable replyIcon;
    private final int iconSizePx;
    private final int triggerDistancePx;

    public SwipeToReplyCallback(Context context, MessageAdapter adapter, SwipeToReplyListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.adapter = adapter;
        this.listener = listener;
        this.iconSizePx = dpToPx(24);
        this.triggerDistancePx = dpToPx(60);

        // Load reply icon
        replyIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_revert);
        if (replyIcon != null) {
            replyIcon.setTint(ContextCompat.getColor(context, R.color.colorPrimary));
        }
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return 0;

        int viewType = adapter.getItemViewType(position);

        // Sent messages can only swipe LEFT
        if (viewType == MessageAdapter.VIEW_TYPE_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_IMAGE_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_FILE_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_VOICE_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_STICKER_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_LOCATION_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_LIVE_LOCATION_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_CONTACT_SENT ||
                viewType == MessageAdapter.VIEW_TYPE_POLL_SENT) {
            return ItemTouchHelper.LEFT;
        }

        // Received messages can only swipe RIGHT
        if (viewType == MessageAdapter.VIEW_TYPE_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_IMAGE_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_FILE_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_VOICE_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_STICKER_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_LOCATION_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_LIVE_LOCATION_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_CONTACT_RECEIVED ||
                viewType == MessageAdapter.VIEW_TYPE_POLL_RECEIVED) {
            return ItemTouchHelper.RIGHT;
        }

        // Call history and recalled messages - no swipe
        return 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // This won't be called because we return 1.0f in getSwipeThreshold
        // Reply is triggered in onChildDraw when distance is reached
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Set very high so onSwiped is never called - we handle reply ourselves
        return 1.0f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return Float.MAX_VALUE; // Prevent fling to complete swipe
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState,
                            boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        float absDx = Math.abs(dX);

        // Limit the visual swipe distance
        float maxSwipe = dpToPx(80);
        float clampedDX;
        if (dX > 0) {
            clampedDX = Math.min(dX, maxSwipe);
        } else {
            clampedDX = Math.max(dX, -maxSwipe);
        }

        // Draw reply icon
        if (replyIcon != null && absDx > dpToPx(10)) {
            int itemHeight = itemView.getBottom() - itemView.getTop();
            int iconTop = itemView.getTop() + (itemHeight - iconSizePx) / 2;
            int iconBottom = iconTop + iconSizePx;

            int iconLeft, iconRight;
            if (dX > 0) {
                // Swiping right
                iconLeft = itemView.getLeft() + dpToPx(16);
                iconRight = iconLeft + iconSizePx;
            } else {
                // Swiping left
                iconRight = itemView.getRight() - dpToPx(16);
                iconLeft = iconRight - iconSizePx;
            }

            replyIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            int alpha = (int) (255 * Math.min(1.0f, absDx / triggerDistancePx));
            replyIcon.setAlpha(alpha);
            replyIcon.draw(c);
        }

        // Trigger reply when user releases after reaching threshold
        if (!isCurrentlyActive && absDx >= triggerDistancePx) {
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onSwipeToReply(position);
            }
        }

        // Apply clamped translation
        getDefaultUIUtil().onDraw(c, recyclerView, itemView, clampedDX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
        // Don't call super - we handle drawing in onChildDraw
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        getDefaultUIUtil().clearView(viewHolder.itemView);
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public interface SwipeToReplyListener {
        void onSwipeToReply(int position);
    }
}
