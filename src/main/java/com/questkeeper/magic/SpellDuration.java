package com.questkeeper.magic;

/**
 * Spell durations.
 *
 * @author Marc McGough
 * @version 1.0
 */
public enum SpellDuration {
    INSTANTANEOUS("Instantaneous", 0),
    ONE_ROUND("1 round", 1),
    ONE_MINUTE("1 minute", 10),
    TEN_MINUTES("10 minutes", 100),
    ONE_HOUR("1 hour", 600),
    EIGHT_HOURS("8 hours", 4800),
    TWENTY_FOUR_HOURS("24 hours", 14400),
    UNTIL_DISPELLED("Until dispelled", -1),
    SPECIAL("Special", -2);

    private final String displayName;
    private final int rounds;  // -1 for unlimited, -2 for special

    SpellDuration(String displayName, int rounds) {
        this.displayName = displayName;
        this.rounds = rounds;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the duration in combat rounds.
     * Returns -1 for until dispelled, -2 for special.
     */
    public int getRounds() {
        return rounds;
    }

    /**
     * Returns true if this duration has a definite end in combat.
     */
    public boolean isTimeLimited() {
        return rounds > 0;
    }
}
