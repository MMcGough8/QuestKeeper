package com.questkeeper.character.features;

/**
 * Represents a class feature that a character can have.
 *
 * Features are abilities granted by a character's class at specific levels.
 * They can be passive (always active) or activated (require usage).
 *
 * @author Marc McGough
 * @version 1.0
 */
public interface ClassFeature {

    /**
     * Gets the unique identifier for this feature.
     */
    String getId();

    /**
     * Gets the display name of this feature.
     */
    String getName();

    /**
     * Gets the description of what this feature does.
     */
    String getDescription();

    /**
     * Gets the minimum level required to have this feature.
     */
    int getLevelRequired();

    /**
     * Returns true if this feature is always active (passive).
     * Passive features don't need to be activated.
     */
    boolean isPassive();

    /**
     * Returns true if this feature can be used in combat.
     */
    boolean isAvailableInCombat();

    /**
     * Returns true if this feature can be used outside of combat.
     */
    boolean isAvailableOutOfCombat();
}
