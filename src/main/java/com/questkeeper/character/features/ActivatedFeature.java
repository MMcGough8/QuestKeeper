package com.questkeeper.character.features;

import com.questkeeper.character.Character;

/**
 * Base class for class features that must be activated and have limited uses.
 *
 * Examples: Second Wind, Action Surge, Channel Divinity
 *
 * @author Marc McGough
 * @version 1.0
 */
public abstract class ActivatedFeature implements ClassFeature {

    private final String id;
    private final String name;
    private final String description;
    private final int levelRequired;
    private final boolean availableInCombat;
    private final boolean availableOutOfCombat;
    private final ResetType resetType;

    private int maxUses;
    private int currentUses;

    protected ActivatedFeature(String id, String name, String description,
                               int levelRequired, int maxUses, ResetType resetType,
                               boolean availableInCombat, boolean availableOutOfCombat) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.levelRequired = levelRequired;
        this.maxUses = maxUses;
        this.currentUses = maxUses;
        this.resetType = resetType;
        this.availableInCombat = availableInCombat;
        this.availableOutOfCombat = availableOutOfCombat;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getLevelRequired() {
        return levelRequired;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean isAvailableInCombat() {
        return availableInCombat;
    }

    @Override
    public boolean isAvailableOutOfCombat() {
        return availableOutOfCombat;
    }

    /**
     * Gets the reset type for this feature.
     */
    public ResetType getResetType() {
        return resetType;
    }

    /**
     * Gets the maximum number of uses for this feature.
     */
    public int getMaxUses() {
        return maxUses;
    }

    /**
     * Sets the maximum number of uses (for features that scale with level).
     */
    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
        // Also restore uses when max increases
        if (currentUses < maxUses) {
            currentUses = maxUses;
        }
    }

    /**
     * Gets the current number of uses remaining.
     */
    public int getCurrentUses() {
        return currentUses;
    }

    /**
     * Checks if this feature can be used (has uses remaining).
     */
    public boolean canUse() {
        return currentUses > 0;
    }

    /**
     * Uses this feature, consuming one use.
     *
     * @param user the character using this feature
     * @return result message describing what happened
     */
    public String use(Character user) {
        if (!canUse()) {
            return String.format("You have no uses of %s remaining. Rest to recover it.", name);
        }

        currentUses--;
        return activate(user);
    }

    /**
     * Implements the actual effect of using this feature.
     * Called by use() after validating uses remain.
     *
     * @param user the character using this feature
     * @return result message describing the effect
     */
    protected abstract String activate(Character user);

    /**
     * Resets uses on a short rest.
     */
    public void resetOnShortRest() {
        if (resetType == ResetType.SHORT_REST) {
            currentUses = maxUses;
        }
    }

    /**
     * Resets uses on a long rest.
     */
    public void resetOnLongRest() {
        // Long rest resets both long rest AND short rest features
        if (resetType == ResetType.SHORT_REST || resetType == ResetType.LONG_REST) {
            currentUses = maxUses;
        }
    }

    /**
     * Gets a formatted status string showing uses remaining.
     */
    public String getUsageStatus() {
        return String.format("%s: %d/%d uses (%s)",
            name, currentUses, maxUses, resetType.getDisplayName());
    }
}
