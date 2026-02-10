package com.questkeeper.character.features;

/**
 * Base class for class features that are always active.
 *
 * Examples: Fighting Style, Improved Critical, Unarmored Defense
 *
 * @author Marc McGough
 * @version 1.0
 */
public abstract class PassiveFeature implements ClassFeature {

    private final String id;
    private final String name;
    private final String description;
    private final int levelRequired;

    protected PassiveFeature(String id, String name, String description, int levelRequired) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.levelRequired = levelRequired;
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
        return true;
    }

    @Override
    public boolean isAvailableInCombat() {
        return true;  // Passive features are always "available"
    }

    @Override
    public boolean isAvailableOutOfCombat() {
        return true;  // Passive features are always "available"
    }
}
