package com.questkeeper.magic;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.combat.Combatant;

import java.util.*;

/**
 * Manages a character's known/prepared spells and spellcasting.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class Spellbook {

    private final Set<String> knownSpellIds = new HashSet<>();
    private final Set<String> preparedSpellIds = new HashSet<>();
    private final Set<String> knownCantripIds = new HashSet<>();
    private SpellSlots spellSlots;
    private Ability spellcastingAbility;
    private boolean usesPreparedSpells;  // Wizard, Cleric, Druid, Paladin prepare; Sorcerer, Bard, Ranger know

    /**
     * Creates an empty spellbook.
     */
    public Spellbook() {
        this.spellSlots = null;
        this.spellcastingAbility = Ability.INTELLIGENCE;
        this.usesPreparedSpells = false;
    }

    /**
     * Initializes spellcasting for a character class.
     */
    public void initializeForClass(CharacterClass charClass, int level) {
        SpellSlots.CasterType casterType = getCasterType(charClass);
        if (casterType != null) {
            this.spellSlots = new SpellSlots(casterType, level);
        }
        this.spellcastingAbility = getSpellcastingAbility(charClass);
        this.usesPreparedSpells = usesPreparedSpells(charClass);

        // Add default cantrips/spells based on class
        addDefaultSpells(charClass, level);
    }

    /**
     * Gets the caster type for a character class.
     */
    private SpellSlots.CasterType getCasterType(CharacterClass charClass) {
        return switch (charClass) {
            case WIZARD, SORCERER, CLERIC, DRUID, BARD -> SpellSlots.CasterType.FULL;
            case PALADIN, RANGER -> SpellSlots.CasterType.HALF;
            case WARLOCK -> SpellSlots.CasterType.WARLOCK;
            default -> null;  // Non-casters
        };
    }

    /**
     * Gets the spellcasting ability for a class.
     */
    private Ability getSpellcastingAbility(CharacterClass charClass) {
        return switch (charClass) {
            case WIZARD -> Ability.INTELLIGENCE;
            case SORCERER, BARD, WARLOCK -> Ability.CHARISMA;
            case CLERIC, DRUID, RANGER -> Ability.WISDOM;
            case PALADIN -> Ability.CHARISMA;
            default -> Ability.INTELLIGENCE;
        };
    }

    /**
     * Checks if the class prepares spells (vs knowing spells).
     */
    private boolean usesPreparedSpells(CharacterClass charClass) {
        return switch (charClass) {
            case WIZARD, CLERIC, DRUID, PALADIN -> true;
            case SORCERER, BARD, RANGER, WARLOCK -> false;
            default -> false;
        };
    }

    /**
     * Adds default starting spells for a class.
     */
    private void addDefaultSpells(CharacterClass charClass, int level) {
        switch (charClass) {
            case WIZARD -> {
                addCantrip("fire_bolt");
                if (level >= 1) {
                    addKnownSpell("magic_missile");
                    addKnownSpell("shield");
                }
            }
            case CLERIC -> {
                addCantrip("sacred_flame");
                if (level >= 1) {
                    addKnownSpell("cure_wounds");
                }
            }
            case SORCERER -> {
                addCantrip("fire_bolt");
                if (level >= 1) {
                    addKnownSpell("magic_missile");
                }
            }
            case DRUID -> {
                if (level >= 1) {
                    addKnownSpell("cure_wounds");
                }
            }
            case BARD -> {
                if (level >= 1) {
                    addKnownSpell("cure_wounds");
                }
            }
            case PALADIN -> {
                if (level >= 2) {
                    addKnownSpell("cure_wounds");
                }
            }
            case RANGER -> {
                if (level >= 2) {
                    addKnownSpell("cure_wounds");
                }
            }
            case WARLOCK -> {
                // Warlocks get different spells - will expand later
            }
            default -> {
                // Non-casters
            }
        }
    }

    /**
     * Adds a cantrip to the known cantrips.
     */
    public void addCantrip(String spellId) {
        knownCantripIds.add(spellId);
    }

    /**
     * Adds a spell to known spells.
     */
    public void addKnownSpell(String spellId) {
        knownSpellIds.add(spellId);
        // For non-prepared casters, known spells are always available
        if (!usesPreparedSpells) {
            preparedSpellIds.add(spellId);
        }
    }

    /**
     * Prepares a spell (for prepared casters).
     */
    public void prepareSpell(String spellId) {
        if (knownSpellIds.contains(spellId) || !usesPreparedSpells) {
            preparedSpellIds.add(spellId);
        }
    }

    /**
     * Unprepares a spell.
     */
    public void unprepareSpell(String spellId) {
        if (usesPreparedSpells) {
            preparedSpellIds.remove(spellId);
        }
    }

    /**
     * Checks if a spell can be cast (known/prepared and slot available).
     */
    public boolean canCast(String spellId) {
        Spell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) return false;

        // Cantrips are always castable
        if (spell.getLevel() == 0) {
            return knownCantripIds.contains(spellId);
        }

        // Check if prepared/known
        if (!preparedSpellIds.contains(spellId)) {
            return false;
        }

        // Check if we have a slot
        return spellSlots != null && spellSlots.hasSlot(spell.getLevel());
    }

    /**
     * Checks if we can cast a spell at a given level.
     */
    public boolean canCastAtLevel(String spellId, int slotLevel) {
        Spell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) return false;

        // Cantrips don't use slots
        if (spell.getLevel() == 0) {
            return knownCantripIds.contains(spellId);
        }

        // Must have the spell prepared
        if (!preparedSpellIds.contains(spellId)) {
            return false;
        }

        // Slot level must be >= spell level
        if (slotLevel < spell.getLevel()) {
            return false;
        }

        // Must have a slot at that level
        return spellSlots != null && spellSlots.hasSlot(slotLevel);
    }

    /**
     * Casts a spell, expending a slot.
     * @return the spell result
     */
    public SpellResult cast(String spellId, Character caster, Combatant target) {
        Spell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return SpellResult.error("Unknown spell: " + spellId);
        }

        return castAtLevel(spellId, caster, target, spell.getLevel());
    }

    /**
     * Casts a spell at a specific slot level.
     */
    public SpellResult castAtLevel(String spellId, Character caster, Combatant target, int slotLevel) {
        Spell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return SpellResult.error("Unknown spell: " + spellId);
        }

        // Cantrips don't expend slots
        if (spell.getLevel() == 0) {
            if (!knownCantripIds.contains(spellId)) {
                return SpellResult.error("You don't know that cantrip.");
            }
            return spell.cast(caster, target, getSpellAttackBonus(caster), getSpellSaveDC(caster));
        }

        // Check if prepared
        if (!preparedSpellIds.contains(spellId)) {
            return SpellResult.error("That spell is not prepared.");
        }

        // Check slot
        if (spellSlots == null || !spellSlots.hasSlot(slotLevel)) {
            return SpellResult.error("No spell slot available at level " + slotLevel + ".");
        }

        if (slotLevel < spell.getLevel()) {
            return SpellResult.error("Cannot cast " + spell.getName() + " with a level " + slotLevel + " slot.");
        }

        // Expend slot
        spellSlots.expendSlot(slotLevel);

        // Cast the spell
        return spell.castAtLevel(caster, target, getSpellAttackBonus(caster), getSpellSaveDC(caster), slotLevel);
    }

    /**
     * Gets the spell attack bonus.
     */
    public int getSpellAttackBonus(Character caster) {
        int profBonus = caster.getProficiencyBonus();
        int abilityMod = caster.getAbilityModifier(spellcastingAbility);
        return profBonus + abilityMod;
    }

    /**
     * Gets the spell save DC.
     */
    public int getSpellSaveDC(Character caster) {
        return 8 + getSpellAttackBonus(caster);
    }

    /**
     * Gets all known cantrips.
     */
    public List<Spell> getKnownCantrips() {
        return knownCantripIds.stream()
            .map(SpellRegistry::getSpell)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Spell::getName))
            .toList();
    }

    /**
     * Gets all known spells (not cantrips).
     */
    public List<Spell> getKnownSpells() {
        return knownSpellIds.stream()
            .map(SpellRegistry::getSpell)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(Spell::getLevel).thenComparing(Spell::getName))
            .toList();
    }

    /**
     * Gets all prepared spells.
     */
    public List<Spell> getPreparedSpells() {
        return preparedSpellIds.stream()
            .map(SpellRegistry::getSpell)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(Spell::getLevel).thenComparing(Spell::getName))
            .toList();
    }

    /**
     * Gets the spell slots tracker.
     */
    public SpellSlots getSpellSlots() {
        return spellSlots;
    }

    /**
     * Gets the spellcasting ability.
     */
    public Ability getSpellcastingAbility() {
        return spellcastingAbility;
    }

    /**
     * Checks if this spellbook has any spellcasting capability.
     */
    public boolean canCastSpells() {
        return !knownCantripIds.isEmpty() || !knownSpellIds.isEmpty();
    }

    /**
     * Gets a formatted status string showing spell slots and prepared spells.
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();

        if (spellSlots != null && spellSlots.canCastSpells()) {
            sb.append(spellSlots.getStatus()).append("\n");
        }

        if (!knownCantripIds.isEmpty()) {
            sb.append("Cantrips: ");
            sb.append(String.join(", ", getKnownCantrips().stream()
                .map(Spell::getName).toList()));
            sb.append("\n");
        }

        List<Spell> prepared = getPreparedSpells();
        if (!prepared.isEmpty()) {
            sb.append("Prepared: ");
            sb.append(String.join(", ", prepared.stream()
                .map(Spell::getName).toList()));
        }

        return sb.toString().trim();
    }

    /**
     * Restores spell slots on short rest.
     */
    public void onShortRest() {
        if (spellSlots != null) {
            spellSlots.restoreOnShortRest();
        }
    }

    /**
     * Restores spell slots on long rest.
     */
    public void onLongRest() {
        if (spellSlots != null) {
            spellSlots.restoreOnLongRest();
        }
    }

    /**
     * Updates for level up.
     */
    public void onLevelUp(int newLevel) {
        if (spellSlots != null) {
            spellSlots.setCasterLevel(newLevel);
        }
    }
}
