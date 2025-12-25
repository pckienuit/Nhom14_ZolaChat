package com.example.doan_zaloclone.models;

/**
 * Enum defining the three main categories for file management
 */
public enum FileCategory {
    MEDIA("Ảnh & Video", 0),
    FILES("Tài liệu", 1),
    LINKS("Liên kết", 2);

    private final String displayName;
    private final int position;

    FileCategory(String displayName, int position) {
        this.displayName = displayName;
        this.position = position;
    }

    public static FileCategory fromPosition(int position) {
        for (FileCategory category : values()) {
            if (category.position == position) {
                return category;
            }
        }
        return MEDIA; // Default
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPosition() {
        return position;
    }
}
