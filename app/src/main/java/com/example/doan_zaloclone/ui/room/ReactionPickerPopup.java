package com.example.doan_zaloclone.ui.room;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.MessageReaction;

/**
 * Popup window displaying reaction options for messages
 * Shows 6 emotions: heart, haha, sad, angry, wow, like
 */
public class ReactionPickerPopup extends PopupWindow {
    
    private OnReactionSelectedListener listener;
    private View contentView;
    
    /**
     * Callback interface for reaction selection
     */
    public interface OnReactionSelectedListener {
        void onReactionSelected(String reactionType);
    }
    
    public ReactionPickerPopup(Context context, OnReactionSelectedListener listener) {
        super(context);
        this.listener = listener;
        
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.layout_reaction_picker, null);
        
        // Configure popup
        setContentView(contentView);
        setWidth(FrameLayout.LayoutParams.WRAP_CONTENT);
        setHeight(FrameLayout.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(context.getDrawable(R.drawable.bg_reaction_picker));
        setElevation(8);
        
        // Setup click listeners for all reactions
        setupReactionListeners();
    }
    
    private void setupReactionListeners() {
        // Heart reaction
        View heartLayout = contentView.findViewById(R.id.reactionHeart);
        heartLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(MessageReaction.REACTION_HEART);
            }
            dismiss();
        });
        
        // Haha reaction
        View hahaLayout = contentView.findViewById(R.id.reactionHaha);
        hahaLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(MessageReaction.REACTION_HAHA);
            }
            dismiss();
        });
        
        // Sad reaction
        View sadLayout = contentView.findViewById(R.id.reactionSad);
        sadLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(MessageReaction.REACTION_SAD);
            }
            dismiss();
        });
        
        // Angry reaction
        View angryLayout = contentView.findViewById(R.id.reactionAngry);
        angryLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(MessageReaction.REACTION_ANGRY);
            }
            dismiss();
        });
        
        // Wow reaction
        View wowLayout = contentView.findViewById(R.id.reactionWow);
        wowLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(MessageReaction.REACTION_WOW);
            }
            dismiss();
        });
        
        // Like reaction
        View likeLayout = contentView.findViewById(R.id.reactionLike);
        likeLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(MessageReaction.REACTION_LIKE);
            }
            dismiss();
        });
        
        // Remove reaction (X button)
        View removeLayout = contentView.findViewById(R.id.reactionRemove);
        removeLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReactionSelected(null); // null = remove reaction
            }
            dismiss();
        });
    }
    
    /**
     * Show popup above the anchor view (message bubble)
     * @param anchorView The view to anchor the popup to (typically message container)
     */
    public void showAboveAnchor(View anchorView) {
        // Measure content view
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = contentView.getMeasuredWidth();
        int popupHeight = contentView.getMeasuredHeight();
        
        // Get anchor location
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        
        // Calculate position (center horizontally above the message)
        int x = location[0] + (anchorView.getWidth() / 2) - (popupWidth / 2);
        int y = location[1] - popupHeight - 16; // 16dp margin above message
        
        // Ensure popup stays within screen bounds
        int screenWidth = anchorView.getContext().getResources().getDisplayMetrics().widthPixels;
        if (x < 16) {
            x = 16; // 16dp margin from left edge
        } else if (x + popupWidth > screenWidth - 16) {
            x = screenWidth - popupWidth - 16; // 16dp margin from right edge
        }
        
        // Show popup at calculated position
        showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
        
        // Apply scale animation
        animateEntry();
    }
    
    /**
     * Apply scale-in animation when popup appears
     */
    private void animateEntry() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
            0.8f, 1.0f,  // Scale from 80% to 100% on X axis
            0.8f, 1.0f,  // Scale from 80% to 100% on Y axis
            Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot X at center
            Animation.RELATIVE_TO_SELF, 1.0f   // Pivot Y at bottom (grows upward)
        );
        scaleAnimation.setDuration(150); // 150ms animation
        scaleAnimation.setFillAfter(true);
        contentView.startAnimation(scaleAnimation);
    }
    
    /**
     * Show popup anchored to a specific view with custom positioning
     * @param anchorView The view to anchor the popup to
     * @param gravity Gravity flags for positioning (e.g., Gravity.TOP | Gravity.CENTER_HORIZONTAL)
     * @param xOffset Horizontal offset in pixels
     * @param yOffset Vertical offset in pixels
     */
    public void showAsDropDown(View anchorView, int xOffset, int yOffset, int gravity) {
        super.showAsDropDown(anchorView, xOffset, yOffset, gravity);
        animateEntry();
    }
}
