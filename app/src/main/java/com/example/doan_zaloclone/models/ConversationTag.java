package com.example.doan_zaloclone.models;

import java.util.Arrays;
import java.util.List;

/**
 * ConversationTag - Model representing tags that can be applied to conversations
 * Includes both system tags (auto-assigned) and user-defined tags
 */
public class ConversationTag {
    
    // System tags (auto-assigned, cannot be removed manually)
    public static final String TAG_STRANGER = "Người lạ";
    
    // Default tags (user can choose)
    public static final String TAG_WORK = "Công việc";
    public static final String TAG_FAMILY = "Gia đình";
    public static final String TAG_FRIENDS = "Bạn bè";
    
    // List of default tags available for selection
    public static final List<String> DEFAULT_TAGS = Arrays.asList(
        TAG_WORK,
        TAG_FAMILY,
        TAG_FRIENDS
    );
    
    // List of all system tags (auto-assigned)
    public static final List<String> SYSTEM_TAGS = Arrays.asList(
        TAG_STRANGER
    );
    
    /**
     * Get color for a specific tag
     * @param tag Tag name
     * @return Color resource ID
     */
    public static int getTagColor(String tag) {
        if (tag == null) return android.R.color.darker_gray;
        
        switch (tag) {
            case TAG_STRANGER:
                return android.R.color.darker_gray;  // Gray for strangers
            case TAG_WORK:
                return android.R.color.holo_blue_dark;  // Blue for work
            case TAG_FAMILY:
                return android.R.color.holo_red_light;  // Red for family
            case TAG_FRIENDS:
                return android.R.color.holo_green_light;  // Green for friends
            default:
                return android.R.color.holo_purple;  // Purple for custom tags
        }
    }
    
    /**
     * Check if a tag is a system tag (cannot be manually removed)
     * @param tag Tag name
     * @return true if system tag
     */
    public static boolean isSystemTag(String tag) {
        return SYSTEM_TAGS.contains(tag);
    }
    
    /**
     * Check if a tag is a default tag
     * @param tag Tag name
     * @return true if default tag
     */
    public static boolean isDefaultTag(String tag) {
        return DEFAULT_TAGS.contains(tag);
    }
}
