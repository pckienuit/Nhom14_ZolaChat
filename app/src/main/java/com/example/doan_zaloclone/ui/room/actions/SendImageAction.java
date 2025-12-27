package com.example.doan_zaloclone.ui.room.actions;

import android.content.Context;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.room.RoomActivity;

/**
 * Quick action for sending images from gallery
 */
public class SendImageAction implements QuickAction {

    @Override
    public int getIconResId() {
        return R.drawable.ic_image;
    }

    @Override
    public String getLabel() {
        return "Gửi Ảnh";
    }

    @Override
    public int getBackgroundColor() {
        return android.graphics.Color.parseColor("#2196F3"); // Blue
    }

    @Override
    public void execute(Context context, QuickActionCallback callback) {
        // Trigger image picker
        if (context instanceof RoomActivity) {
            ((RoomActivity) context).triggerImagePicker();
        }
    }
}
