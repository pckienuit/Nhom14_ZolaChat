package com.example.doan_zaloclone.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for message reactions
 * Supports 6 basic reaction types: heart, haha, sad, angry, wow, like
 */
public class MessageReaction {
    // Reaction types constants
    public static final String REACTION_HEART = "heart";
    public static final String REACTION_HAHA = "haha";
    public static final String REACTION_SAD = "sad";
    public static final String REACTION_ANGRY = "angry";
    public static final String REACTION_WOW = "wow";
    public static final String REACTION_LIKE = "like";

    // Maximum count to display before showing "99+"
    public static final int MAX_DISPLAY_COUNT = 99;

    /**
     * Get total reaction count from reactions map
     *
     * @param reactions Map of userId -> reactionType
     * @return Total number of reactions
     */
    public static int getReactionCount(Map<String, String> reactions) {
        if (reactions == null) {
            return 0;
        }
        return reactions.size();
    }

    /**
     * Get formatted reaction count for display
     *
     * @param reactions Map of userId -> reactionType
     * @return Formatted string (e.g., "5", "99+")
     */
    public static String getFormattedReactionCount(Map<String, String> reactions) {
        int count = getReactionCount(reactions);
        if (count == 0) {
            return "";
        } else if (count > MAX_DISPLAY_COUNT) {
            return MAX_DISPLAY_COUNT + "+";
        } else {
            return String.valueOf(count);
        }
    }

    /**
     * Get the reaction type for a specific user
     *
     * @param reactions Map of userId -> reactionType
     * @param userId    User ID to check
     * @return Reaction type or null if user hasn't reacted
     */
    public static String getUserReaction(Map<String, String> reactions, String userId) {
        if (reactions == null || userId == null) {
            return null;
        }
        return reactions.get(userId);
    }

    /**
     * Check if a user has reacted to this message
     *
     * @param reactions Map of userId -> reactionType
     * @param userId    User ID to check
     * @return true if user has reacted
     */
    public static boolean hasUserReacted(Map<String, String> reactions, String userId) {
        return getUserReaction(reactions, userId) != null;
    }

    /**
     * Get the default reaction icon for a message
     * If user has reacted, return their reaction type
     * Otherwise return heart outline (default)
     *
     * @param reactions     Map of userId -> reactionType
     * @param currentUserId Current user's ID
     * @return Reaction type to display as default icon
     */
    public static String getDefaultReactionIcon(Map<String, String> reactions, String currentUserId) {
        String userReaction = getUserReaction(reactions, currentUserId);
        // If user has already reacted, show their reaction as default
        // Otherwise show heart outline (we'll handle this in the UI layer)
        return userReaction;
    }

    /**
     * Check if a reaction type is valid
     *
     * @param reactionType Reaction type to validate
     * @return true if valid reaction type
     */
    public static boolean isValidReactionType(String reactionType) {
        if (reactionType == null) {
            return false;
        }
        switch (reactionType) {
            case REACTION_HEART:
            case REACTION_HAHA:
            case REACTION_SAD:
            case REACTION_ANGRY:
            case REACTION_WOW:
            case REACTION_LIKE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Create a new reaction map with a user's reaction
     *
     * @param existingReactions Existing reactions map (can be null)
     * @param userId            User ID
     * @param reactionType      Reaction type
     * @return Updated reactions map
     */
    public static Map<String, String> addReaction(Map<String, String> existingReactions,
                                                  String userId, String reactionType) {
        Map<String, String> reactions = existingReactions != null
                ? new HashMap<>(existingReactions)
                : new HashMap<>();

        if (isValidReactionType(reactionType)) {
            reactions.put(userId, reactionType);
        }
        return reactions;
    }

    /**
     * Remove a user's reaction from the map
     *
     * @param existingReactions Existing reactions map
     * @param userId            User ID to remove
     * @return Updated reactions map
     */
    public static Map<String, String> removeReaction(Map<String, String> existingReactions,
                                                     String userId) {
        if (existingReactions == null) {
            return new HashMap<>();
        }

        Map<String, String> reactions = new HashMap<>(existingReactions);
        reactions.remove(userId);
        return reactions;
    }

    /**
     * Toggle a user's reaction - always add/update reaction (Zalo style)
     * Does NOT remove reaction when clicking same type
     *
     * @param existingReactions Existing reactions map
     * @param userId            User ID
     * @param reactionType      New reaction type
     * @return Updated reactions map
     */
    public static Map<String, String> toggleReaction(Map<String, String> existingReactions,
                                                     String userId, String reactionType) {
        // Always add/update the reaction (Zalo style - clicking same reaction keeps it)
        return addReaction(existingReactions, userId, reactionType);
    }

    /**
     * Get top N reactions sorted by count (descending)
     *
     * @param reactions Map of userId -> reactionType
     * @param topN      Number of top reactions to return
     * @return List of reaction types sorted by count (highest first)
     */
    public static java.util.List<String> getTopReactions(Map<String, String> reactions, int topN) {
        Map<String, Integer> counts = getReactionTypeCounts(reactions);

        // Filter out reactions with 0 count and sort by count descending
        java.util.List<java.util.Map.Entry<String, Integer>> sortedEntries = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                sortedEntries.add(entry);
            }
        }

        // Sort by count descending
        java.util.Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        // Get top N reaction types
        java.util.List<String> topReactions = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(topN, sortedEntries.size()); i++) {
            topReactions.add(sortedEntries.get(i).getKey());
        }

        return topReactions;
    }

    /**
     * Increment reaction count for a specific type
     *
     * @param existingCounts Existing reaction counts map
     * @param reactionType   Reaction type to increment
     * @return Updated reaction counts map
     */
    public static Map<String, Integer> incrementReactionCount(Map<String, Integer> existingCounts,
                                                              String reactionType) {
        Map<String, Integer> counts = existingCounts != null
                ? new HashMap<>(existingCounts)
                : new HashMap<>();

        if (isValidReactionType(reactionType)) {
            int currentCount = counts.getOrDefault(reactionType, 0);
            counts.put(reactionType, currentCount + 1);
        }

        return counts;
    }

    /**
     * Get top N reactions sorted by count from reactionCounts (descending)
     *
     * @param reactionCounts Map of reactionType -> count
     * @param topN           Number of top reactions to return
     * @return List of reaction types sorted by count (highest first)
     */
    public static java.util.List<String> getTopReactionsFromCounts(Map<String, Integer> reactionCounts, int topN) {
        if (reactionCounts == null || reactionCounts.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // Filter out reactions with 0 count and sort by count descending
        java.util.List<java.util.Map.Entry<String, Integer>> sortedEntries = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : reactionCounts.entrySet()) {
            Integer value = entry.getValue();
            if (value != null && value > 0) {
                sortedEntries.add(entry);
            }
        }

        // Sort by count descending
        java.util.Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        // Get top N reaction types
        java.util.List<String> topReactions = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(topN, sortedEntries.size()); i++) {
            topReactions.add(sortedEntries.get(i).getKey());
        }

        return topReactions;
    }

    /**
     * Get count of each reaction type
     * Uses reactionCounts if available (tracks total clicks),
     * otherwise falls back to counting unique users
     *
     * @param reactions      Map of userId -> reactionType (for user tracking)
     * @param reactionCounts Map of reactionType -> count (for total count)
     * @return Map of reactionType -> count
     */
    public static Map<String, Integer> getReactionTypeCounts(Map<String, String> reactions,
                                                             Map<String, Integer> reactionCounts) {
        // If reactionCounts is available, use it (supports multiple clicks per user)
        if (reactionCounts != null && !reactionCounts.isEmpty()) {
            Map<String, Integer> counts = new HashMap<>();
            counts.put(REACTION_HEART, reactionCounts.getOrDefault(REACTION_HEART, 0));
            counts.put(REACTION_HAHA, reactionCounts.getOrDefault(REACTION_HAHA, 0));
            counts.put(REACTION_SAD, reactionCounts.getOrDefault(REACTION_SAD, 0));
            counts.put(REACTION_ANGRY, reactionCounts.getOrDefault(REACTION_ANGRY, 0));
            counts.put(REACTION_WOW, reactionCounts.getOrDefault(REACTION_WOW, 0));
            counts.put(REACTION_LIKE, reactionCounts.getOrDefault(REACTION_LIKE, 0));
            return counts;
        }

        // Fallback: count unique users (old behavior)
        return getReactionTypeCounts(reactions);
    }

    /**
     * Get count of each reaction type (legacy - counts unique users only)
     *
     * @param reactions Map of userId -> reactionType
     * @return Map of reactionType -> count
     */
    public static Map<String, Integer> getReactionTypeCounts(Map<String, String> reactions) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put(REACTION_HEART, 0);
        counts.put(REACTION_HAHA, 0);
        counts.put(REACTION_SAD, 0);
        counts.put(REACTION_ANGRY, 0);
        counts.put(REACTION_WOW, 0);
        counts.put(REACTION_LIKE, 0);

        if (reactions == null || reactions.isEmpty()) {
            return counts;
        }

        for (String reactionType : reactions.values()) {
            if (isValidReactionType(reactionType)) {
                counts.put(reactionType, counts.get(reactionType) + 1);
            }
        }

        return counts;
    }
}
