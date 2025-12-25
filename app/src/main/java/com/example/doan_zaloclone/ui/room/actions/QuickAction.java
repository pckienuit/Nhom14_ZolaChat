package com.example.doan_zaloclone.ui.room.actions;

import android.content.Context;

/**
 * Interface for quick actions in the action menu
 * Extensible design allows adding new actions easily
 */
public interface QuickAction {

    /**
     * Get the icon resource ID for this action
     *
     * @return Drawable resource ID
     */
    int getIconResId();

    /**
     * Get the label text for this action
     *
     * @return Label string
     */
    String getLabel();

    /**
     * Execute this action
     *
     * @param context  Android context
     * @param callback Callback for action results
     */
    void execute(Context context, QuickActionCallback callback);
}
