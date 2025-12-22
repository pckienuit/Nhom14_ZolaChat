package com.example.doan_zaloclone.ui.room.actions;

import android.content.Context;

import com.example.doan_zaloclone.R;

/**
 * Quick action for creating and sending polls
 */
public class SendPollAction implements QuickAction {
    
    @Override
    public int getIconResId() {
        return R.drawable.ic_poll;
    }
    
    @Override
    public String getLabel() {
        return "Tạo Poll";
    }
    
    @Override
    public void execute(Context context, QuickActionCallback callback) {
        // Trigger UI to show poll creation screen
        callback.onShowUI();
    }
}
