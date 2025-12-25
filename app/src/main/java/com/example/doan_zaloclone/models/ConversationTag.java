package com.example.doan_zaloclone.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // Default color palette for custom tags
    public static final int[] COLOR_PALETTE = {
            Color.parseColor("#FF6200EE"),  // Purple
            Color.parseColor("#FF3700B3"),  // Deep Purple
            Color.parseColor("#FF03DAC5"),  // Teal
            Color.parseColor("#FF018786"),  // Dark Teal
            Color.parseColor("#FFB00020"),  // Red
            Color.parseColor("#FFFF5722"),  // Deep Orange
            Color.parseColor("#FFFF9800"),  // Orange
            Color.parseColor("#FFFFC107"),  // Amber
            Color.parseColor("#FF4CAF50"),  // Green
            Color.parseColor("#FF2196F3"),  // Blue
            Color.parseColor("#FF9C27B0"),  // Pink
            Color.parseColor("#FF795548"),  // Brown
    };
    // List of default tags available for selection
    public static final List<String> DEFAULT_TAGS = Arrays.asList(
            TAG_WORK,
            TAG_FAMILY,
            TAG_FRIENDS
    );
    // List of all system tags (auto-assigned)
    public static final List<String> SYSTEM_TAGS = List.of(
            TAG_STRANGER
    );
    private static final String PREFS_NAME = "ConversationTagPrefs";
    private static final String KEY_CUSTOM_COLORS = "custom_tag_colors";
    // In-memory cache for custom colors
    private static final Map<String, Integer> customColorCache = new HashMap<>();

    /**
     * Get color for a specific tag
     *
     * @param context Context for accessing SharedPreferences
     * @param tag     Tag name
     * @return Color integer
     */
    public static int getTagColor(Context context, String tag) {
        if (tag == null) return Color.GRAY;

        // Check default tag colors first
        switch (tag) {
            case TAG_STRANGER:
                return Color.parseColor("#FF757575");  // Gray
            case TAG_WORK:
                return Color.parseColor("#FF2196F3");  // Blue
            case TAG_FAMILY:
                return Color.parseColor("#FFFF5722");  // Deep Orange
            case TAG_FRIENDS:
                return Color.parseColor("#FF4CAF50");  // Green
        }

        // Check custom colors
        if (context != null) {
            Integer customColor = getCustomTagColor(context, tag);
            if (customColor != null) {
                return customColor;
            }
        }

        // Default color for unknown tags
        return Color.parseColor("#FF9C27B0");  // Purple
    }

    /**
     * Set custom color for a tag
     *
     * @param context Context for accessing SharedPreferences
     * @param tag     Tag name
     * @param color   Color integer
     */
    public static void setCustomTagColor(Context context, String tag, int color) {
        if (context == null || tag == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CUSTOM_COLORS + "_" + tag, color);
        editor.apply();

        // Update cache
        customColorCache.put(tag, color);
    }

    /**
     * Get custom color for a tag
     *
     * @param context Context for accessing SharedPreferences
     * @param tag     Tag name
     * @return Color integer or null if not set
     */
    public static Integer getCustomTagColor(Context context, String tag) {
        if (context == null || tag == null) return null;

        // Check cache first
        if (customColorCache.containsKey(tag)) {
            return customColorCache.get(tag);
        }

        // Load from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_CUSTOM_COLORS + "_" + tag)) {
            int color = prefs.getInt(KEY_CUSTOM_COLORS + "_" + tag, Color.GRAY);
            customColorCache.put(tag, color);
            return color;
        }

        return null;
    }

    /**
     * Check if a tag is a system tag (cannot be manually removed)
     *
     * @param tag Tag name
     * @return true if system tag
     */
    public static boolean isSystemTag(String tag) {
        return SYSTEM_TAGS.contains(tag);
    }

    /**
     * Check if a tag is a default tag
     *
     * @param tag Tag name
     * @return true if default tag
     */
    public static boolean isDefaultTag(String tag) {
        return DEFAULT_TAGS.contains(tag);
    }

    /**
     * Check if a tag is a custom tag (user-defined)
     *
     * @param tag Tag name
     * @return true if custom tag
     */
    public static boolean isCustomTag(String tag) {
        return !isSystemTag(tag) && !isDefaultTag(tag);
    }
}
