package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;
import com.questkeeper.core.Dice;
import com.questkeeper.magic.*;

/**
 * Cure Wounds - 1st level Evocation
 * A creature you touch regains hit points equal to 1d8 + spellcasting modifier.
 * At Higher Levels: +1d8 per slot level above 1st.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class CureWounds extends AbstractSpell {

    public static final String SPELL_ID = "cure_wounds";

    private CureWounds(Builder builder) {
        super(builder);
    }

    public static CureWounds create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC) {
        return castAtLevel(caster, target, spellAttackBonus, spellSaveDC, 1);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC, int slotLevel) {
        if (target == null) {
            target = caster;  // Default to self
        }

        // Can't heal undead or constructs (simplified check)
        if (!target.isAlive()) {
            return SpellResult.error("Cannot heal a dead creature.");
        }

        // Roll healing: 1d8 per slot level + spellcasting modifier
        int healingDice = slotLevel;
        int healing = 0;
        for (int i = 0; i < healingDice; i++) {
            healing += Dice.roll(8);
        }

        // Add spellcasting ability modifier (approximated from spell save DC)
        // DC = 8 + prof + mod, so mod = DC - 8 - prof
        // For simplicity, we'll estimate based on typical values
        int abilityMod = spellSaveDC - 8 - ((caster.getLevel() - 1) / 4 + 2);
        healing += Math.max(0, abilityMod);

        // Apply healing
        int actualHealing = target.heal(healing);

        String message = String.format(
            "%s casts Cure Wounds%s on %s!\n" +
            "Healed %d HP. (%s is now at %d/%d HP)",
            caster.getName(),
            slotLevel > 1 ? " at " + slotLevel + getOrdinalSuffix(slotLevel) + " level" : "",
            target.getName(),
            actualHealing,
            target.getName(),
            target.getCurrentHitPoints(),
            target.getMaxHitPoints());

        return SpellResult.healing(this, target, actualHealing);
    }

    private String getOrdinalSuffix(int n) {
        if (n >= 11 && n <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private static class Builder extends AbstractSpell.Builder<Builder> {

        public Builder() {
            id(SPELL_ID);
            name("Cure Wounds");
            description("A creature you touch regains a number of hit points equal to 1d8 + " +
                "your spellcasting ability modifier. This spell has no effect on undead or constructs.\n\n" +
                "At Higher Levels: When you cast this spell using a spell slot of 2nd level or higher, " +
                "the healing increases by 1d8 for each slot level above 1st.");
            level(1);
            school(SpellSchool.EVOCATION);
            castingTime(CastingTime.ACTION);
            rangeTouch();
            components(SpellComponent.VERBAL, SpellComponent.SOMATIC);
            duration(SpellDuration.INSTANTANEOUS);
            targetAlly();
        }

        @Override
        public CureWounds build() {
            return new CureWounds(this);
        }
    }
}
