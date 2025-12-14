package com.example.doan_zaloclone.ui.room.actions;

import android.net.Uri;
import java.util.List;

/**
 * Callback interface for quick action execution results
 */
public interface QuickActionCallback {
    
    /**
     * Called when action needs to show UI (e.g., file picker)
     */
    void onShowUI();
    
    /**
     * Called when action is cancelled
     */
    void onCancel();
    
    /**
     * Called when files are selected (for file-related actions)
     * @param fileUris List of selected file URIs
     */
    void onFilesSelected(List<Uri> fileUris);
    
    /**
     * Called when action fails
     * @param error Error message
     */
    void onError(String error);
    
    /**
     * Called when action completes successfully
     */
    void onSuccess();
}
