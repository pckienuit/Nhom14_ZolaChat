package com.example.doan_zaloclone.ui.room.actions;

import android.content.Context;

import com.example.doan_zaloclone.R;

/**
 * Quick action for sending business cards (contact cards)
 * Allows users to share friend profiles in chat
 */
public class SendContactAction implements QuickAction {
    
    @Override
    public int getIconResId() {
        return R.drawable.ic_contact_card;
    }
    
   @Override
    public String getLabel() {
        return "Danh thiếp";
    }
    
    @Override
    public void execute(Context context, QuickActionCallback callback) {
        // Trigger UI to show contact selection dialog
        callback.onShowUI();
    }
}
