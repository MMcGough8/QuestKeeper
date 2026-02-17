package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;
import com.questkeeper.core.Dice;
import com.questkeeper.magic.*;

/**
 * Magic Missile - 1st level Evocation
 * Creates darts of magical force that automatically hit.
 * Each dart deals 1d4+1 force damage.
 * At Higher Levels: +1 dart per slot level above 1st.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class MagicMissile extends AbstractSpell {

    public static final String SPELL_ID = "magic_missile";

    private MagicMissile(Builder builder) {
        super(builder);
    }

    public static MagicMissile create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC) {
        return castAtLevel(caster, target, spellAttackBonus, spellSaveDC, 1);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC, int slotLevel) {
        if (target == null) {
            return SpellResult.error("Magic Missile requires a target.");
        }

        if (!target.isAlive()) {
            return SpellResult.error("Target is already dead.");
        }

        // 3 darts at 1st level, +1 per slot level above 1st
        int dartCount = 2 + slotLevel;

        // Each dart deals 1d4+1 force damage
        int totalDamage = 0;
        StringBuilder dartDetails = new StringBuilder();
        for (int i = 0; i < dartCount; i++) {
            int dartDamage = Dice.roll(4) + 1;
            totalDamage += dartDamage;
            if (i > 0) dartDetails.append(", ");
            dartDetails.append(dartDamage);
        }

        // Apply damage
        target.takeDamage(totalDamage);

        String message = String.format(
            "%s casts Magic Missile%s at %s!%n" +
            "%d darts streak toward the target! (Damage: %s)%n" +
            "Total: %d force damage. (%s: %d/%d HP)",
            caster.getName(),
            slotLevel > 1 ? " at " + slotLevel + getOrdinalSuffix(slotLevel) + " level" : "",
            target.getName(),
            dartCount,
            dartDetails.toString(),
            totalDamage,
            target.getName(),
            target.getCurrentHitPoints(),
            target.getMaxHitPoints());

        return new SpellResult.Builder(SpellResult.Type.HIT, message)
            .spell(this)
            .target(target)
            .damage(totalDamage, "force")
            .build();
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
            name("Magic Missile");
            description("You create three glowing darts of magical force. Each dart hits a creature " +
                "of your choice that you can see within range. A dart deals 1d4+1 force damage to its target. " +
                "The darts all strike simultaneously.\n\n" +
                "At Higher Levels: When you cast this spell using a spell slot of 2nd level or higher, " +
                "the spell creates one more dart for each slot level above 1st.");
            level(1);
            school(SpellSchool.EVOCATION);
            castingTime(CastingTime.ACTION);
            rangeFeet(120);
            components(SpellComponent.VERBAL, SpellComponent.SOMATIC);
            duration(SpellDuration.INSTANTANEOUS);
            targetEnemy();
        }

        @Override
        public MagicMissile build() {
            return new MagicMissile(this);
        }
    }
}
