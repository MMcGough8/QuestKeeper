package com.questkeeper.magic;

/**
 * The eight schools of magic in D&D 5e.
 *
 * @author Marc McGough
 * @version 1.0
 */
public enum SpellSchool {
    ABJURATION("Abjuration", "Protective magic that blocks, banishes, or protects"),
    CONJURATION("Conjuration", "Magic that produces objects or creatures out of thin air"),
    DIVINATION("Divination", "Magic that reveals information"),
    ENCHANTMENT("Enchantment", "Magic that affects the minds of others"),
    EVOCATION("Evocation", "Magic that manipulates energy to produce a desired effect"),
    ILLUSION("Illusion", "Magic that deceives the senses or minds of others"),
    NECROMANCY("Necromancy", "Magic that manipulates life force"),
    TRANSMUTATION("Transmutation", "Magic that changes the properties of creatures or objects");

    private final String displayName;
    private final String description;

    SpellSchool(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
