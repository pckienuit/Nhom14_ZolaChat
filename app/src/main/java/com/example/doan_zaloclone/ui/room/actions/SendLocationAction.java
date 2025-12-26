package com.example.doan_zaloclone.ui.room.actions;

import android.content.Context;

import com.example.doan_zaloclone.R;

/**
 * Quick action for sending location
 * Allows users to share their current location or a pinned location
 */
public class SendLocationAction implements QuickAction {

    @Override
    public int getIconResId() {
        return R.drawable.ic_location;
    }

    @Override
    public String getLabel() {
        return "Gửi vị trí";
    }

    @Override
    public int getBackgroundColor() {
        return android.graphics.Color.parseColor("#E91E63"); // Pink/Red
    }

    @Override
    public void execute(Context context, QuickActionCallback callback) {
        // Trigger UI to show location picker
        callback.onShowUI();
    }
}
