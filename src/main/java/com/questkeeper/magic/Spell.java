package com.questkeeper.magic;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;

import java.util.Set;

/**
 * Represents a spell in the D&D 5e magic system.
 *
 * @author Marc McGough
 * @version 1.0
 */
public interface Spell {

    /**
     * Gets the spell's unique identifier.
     */
    String getId();

    /**
     * Gets the spell's display name.
     */
    String getName();

    /**
     * Gets the spell's description.
     */
    String getDescription();

    /**
     * Gets the spell level (0 for cantrips, 1-9 for leveled spells).
     */
    int getLevel();

    /**
     * Gets the spell's school of magic.
     */
    SpellSchool getSchool();

    /**
     * Gets the casting time description.
     */
    CastingTime getCastingTime();

    /**
     * Gets the spell's range in feet (0 for self/touch).
     */
    int getRange();

    /**
     * Gets the spell's range description (e.g., "Self", "Touch", "60 feet").
     */
    String getRangeDescription();

    /**
     * Gets the required components for casting.
     */
    Set<SpellComponent> getComponents();

    /**
     * Gets the material component description, if any.
     */
    String getMaterialComponent();

    /**
     * Gets the spell's duration.
     */
    SpellDuration getDuration();

    /**
     * Returns true if this spell requires concentration.
     */
    boolean requiresConcentration();

    /**
     * Returns true if this is a ritual spell.
     */
    boolean isRitual();

    /**
     * Returns true if this spell can target enemies.
     */
    boolean canTargetEnemy();

    /**
     * Returns true if this spell can target allies/self.
     */
    boolean canTargetAlly();

    /**
     * Returns true if this spell requires an attack roll.
     */
    boolean requiresAttackRoll();

    /**
     * Returns true if this spell allows a saving throw.
     */
    boolean allowsSavingThrow();

    /**
     * Gets the saving throw ability, if applicable.
     */
    Character.Ability getSaveAbility();

    /**
     * Casts the spell at base level.
     *
     * @param caster the character casting the spell
     * @param target the target of the spell (may be null for self-targeting)
     * @param spellAttackBonus the caster's spell attack bonus
     * @param spellSaveDC the caster's spell save DC
     * @return the result of casting the spell
     */
    SpellResult cast(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC);

    /**
     * Casts the spell at a higher level (upcasting).
     *
     * @param caster the character casting the spell
     * @param target the target of the spell
     * @param spellAttackBonus the caster's spell attack bonus
     * @param spellSaveDC the caster's spell save DC
     * @param slotLevel the level of spell slot used (must be >= spell level)
     * @return the result of casting the spell
     */
    SpellResult castAtLevel(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC, int slotLevel);

    /**
     * Gets a formatted string showing the spell's stats.
     */
    default String getSpellCard() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" (");
        if (getLevel() == 0) {
            sb.append(getSchool().getDisplayName()).append(" cantrip");
        } else {
            sb.append("Level ").append(getLevel()).append(" ").append(getSchool().getDisplayName().toLowerCase());
        }
        sb.append(")\n");
        sb.append("Casting Time: ").append(getCastingTime().getDisplayName()).append("\n");
        sb.append("Range: ").append(getRangeDescription()).append("\n");
        sb.append("Components: ").append(getComponentsString()).append("\n");
        sb.append("Duration: ").append(getDuration().getDisplayName());
        if (requiresConcentration()) {
            sb.append(" (concentration)");
        }
        sb.append("\n\n");
        sb.append(getDescription());
        return sb.toString();
    }

    /**
     * Gets a formatted string of spell components.
     */
    default String getComponentsString() {
        StringBuilder sb = new StringBuilder();
        Set<SpellComponent> components = getComponents();
        if (components.contains(SpellComponent.VERBAL)) sb.append("V");
        if (components.contains(SpellComponent.SOMATIC)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("S");
        }
        if (components.contains(SpellComponent.MATERIAL)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("M");
            String material = getMaterialComponent();
            if (material != null && !material.isEmpty()) {
                sb.append(" (").append(material).append(")");
            }
        }
        return sb.toString();
    }
}
