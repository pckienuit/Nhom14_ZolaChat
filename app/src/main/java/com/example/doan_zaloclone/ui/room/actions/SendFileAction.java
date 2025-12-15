package com.example.doan_zaloclone.ui.room.actions;

import android.content.Context;
import com.example.doan_zaloclone.R;

/**
 * Quick action for sending files
 */
public class SendFileAction implements QuickAction {
    
    @Override
    public int getIconResId() {
        return R.drawable.ic_attach_file;
    }
    
    @Override
    public String getLabel() {
        return "Gửi File";
    }
    
    @Override
    public void execute(Context context, QuickActionCallback callback) {
        // Trigger file picker UI
        callback.onShowUI();
    }
}
