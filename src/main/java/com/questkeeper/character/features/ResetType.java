package com.questkeeper.character.features;

/**
 * Defines when a class feature's uses are restored.
 *
 * @author Marc McGough
 * @version 1.0
 */
public enum ResetType {
    /**
     * Uses are restored after a short rest (1 hour).
     */
    SHORT_REST("Short Rest"),

    /**
     * Uses are restored after a long rest (8 hours).
     */
    LONG_REST("Long Rest"),

    /**
     * Feature has no usage limit.
     */
    UNLIMITED("Unlimited");

    private final String displayName;

    ResetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
