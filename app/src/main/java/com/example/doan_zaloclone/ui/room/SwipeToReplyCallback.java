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
    private Drawable replyIcon;
    private final int iconSize = 24; // dp

    public interface SwipeToReplyListener {
        void onSwipeToReply(int position);
    }

    public SwipeToReplyCallback(Context context, MessageAdapter adapter, SwipeToReplyListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.adapter = adapter;
        this.listener = listener;
        
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
            viewType == MessageAdapter.VIEW_TYPE_FILE_SENT) {
            return ItemTouchHelper.LEFT;
        }
        
        // Received messages can only swipe RIGHT
        if (viewType == MessageAdapter.VIEW_TYPE_RECEIVED ||
            viewType == MessageAdapter.VIEW_TYPE_IMAGE_RECEIVED ||
            viewType == MessageAdapter.VIEW_TYPE_FILE_RECEIVED) {
            return ItemTouchHelper.RIGHT;
        }
        
        // Call history messages - no swipe
        return 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, 
                         @NonNull RecyclerView.ViewHolder viewHolder, 
                         @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't want drag & drop
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            listener.onSwipeToReply(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, 
                           @NonNull RecyclerView recyclerView, 
                           @NonNull RecyclerView.ViewHolder viewHolder, 
                           float dX, float dY, 
                           int actionState, 
                           boolean isCurrentlyActive) {
        
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            
            // Limit swipe distance
            float maxSwipeDistance = dpToPx(80);
            float limitedDX = Math.max(-maxSwipeDistance, Math.min(maxSwipeDistance, dX));
            
            // Draw reply icon
            if (replyIcon != null) {
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicHeight = dpToPx(iconSize);
                int intrinsicWidth = dpToPx(iconSize);
                
                int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int iconBottom = iconTop + intrinsicHeight;
                
                int iconLeft, iconRight;
                
                if (dX > 0) {
                    // Swiping right (received message)
                    iconLeft = itemView.getLeft() + dpToPx(16);
                    iconRight = iconLeft + intrinsicWidth;
                } else {
                    // Swiping left (sent message)
                    iconRight = itemView.getRight() - dpToPx(16);
                    iconLeft = iconRight - intrinsicWidth;
                }
                
                replyIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                
                // Fade icon based on swipe progress
                int alpha = (int) (255 * Math.min(1.0f, Math.abs(limitedDX) / maxSwipeDistance));
                replyIcon.setAlpha(alpha);
                replyIcon.draw(c);
            }
            
            // Apply translation with limit
            super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.3f; // Trigger at 30% swipe
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 2; // Make it easier to trigger
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
