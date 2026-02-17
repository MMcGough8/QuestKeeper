package com.questkeeper.magic;

/**
 * Spell casting times.
 *
 * @author Marc McGough
 * @version 1.0
 */
public enum CastingTime {
    ACTION("1 action", true),
    BONUS_ACTION("1 bonus action", true),
    REACTION("1 reaction", true),
    ONE_MINUTE("1 minute", false),
    TEN_MINUTES("10 minutes", false),
    ONE_HOUR("1 hour", false),
    EIGHT_HOURS("8 hours", false),
    TWELVE_HOURS("12 hours", false),
    TWENTY_FOUR_HOURS("24 hours", false);

    private final String displayName;
    private final boolean usableInCombat;

    CastingTime(String displayName, boolean usableInCombat) {
        this.displayName = displayName;
        this.usableInCombat = usableInCombat;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this casting time allows the spell to be cast in combat.
     */
    public boolean isUsableInCombat() {
        return usableInCombat;
    }
}
