package com.questkeeper.magic;

/**
 * Spell components required for casting.
 *
 * @author Marc McGough
 * @version 1.0
 */
public enum SpellComponent {
    VERBAL("V", "Verbal", "Spoken words of power"),
    SOMATIC("S", "Somatic", "Hand gestures"),
    MATERIAL("M", "Material", "Physical components");

    private final String abbreviation;
    private final String displayName;
    private final String description;

    SpellComponent(String abbreviation, String displayName, String description) {
        this.abbreviation = abbreviation;
        this.displayName = displayName;
        this.description = description;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
