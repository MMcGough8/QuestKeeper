package com.questkeeper.magic;

import com.questkeeper.magic.spells.*;

import java.util.*;

/**
 * Registry of all available spells in the game.
 * Provides lookup by ID and filtering by school, level, etc.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class SpellRegistry {

    private static final Map<String, Spell> SPELLS = new HashMap<>();

    static {
        // Register all spells
        register(CureWounds.create());
        register(MagicMissile.create());
        register(Shield.create());
        register(FireBolt.create());
        register(SacredFlame.create());
    }

    private static void register(Spell spell) {
        SPELLS.put(spell.getId(), spell);
    }

    /**
     * Gets a spell by its ID.
     * @return the spell, or null if not found
     */
    public static Spell getSpell(String id) {
        return SPELLS.get(id);
    }

    /**
     * Gets a spell by name (case-insensitive).
     * @return the spell, or null if not found
     */
    public static Spell getSpellByName(String name) {
        String lowerName = name.toLowerCase();
        for (Spell spell : SPELLS.values()) {
            if (spell.getName().toLowerCase().equals(lowerName)) {
                return spell;
            }
        }
        // Try partial match
        for (Spell spell : SPELLS.values()) {
            if (spell.getName().toLowerCase().contains(lowerName) ||
                lowerName.contains(spell.getName().toLowerCase())) {
                return spell;
            }
        }
        return null;
    }

    /**
     * Gets all registered spells.
     */
    public static Collection<Spell> getAllSpells() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }

    /**
     * Gets all spells of a given school.
     */
    public static List<Spell> getSpellsBySchool(SpellSchool school) {
        return SPELLS.values().stream()
            .filter(s -> s.getSchool() == school)
            .sorted(Comparator.comparingInt(Spell::getLevel).thenComparing(Spell::getName))
            .toList();
    }

    /**
     * Gets all spells of a given level.
     * @param level 0 for cantrips, 1-9 for leveled spells
     */
    public static List<Spell> getSpellsByLevel(int level) {
        return SPELLS.values().stream()
            .filter(s -> s.getLevel() == level)
            .sorted(Comparator.comparing(Spell::getName))
            .toList();
    }

    /**
     * Gets all cantrips (level 0 spells).
     */
    public static List<Spell> getCantrips() {
        return getSpellsByLevel(0);
    }

    /**
     * Gets all spells up to a given level (for spell lists).
     */
    public static List<Spell> getSpellsUpToLevel(int maxLevel) {
        return SPELLS.values().stream()
            .filter(s -> s.getLevel() <= maxLevel)
            .sorted(Comparator.comparingInt(Spell::getLevel).thenComparing(Spell::getName))
            .toList();
    }

    /**
     * Checks if a spell ID exists in the registry.
     */
    public static boolean exists(String id) {
        return SPELLS.containsKey(id);
    }
}
