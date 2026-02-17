package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.combat.Combatant;
import com.questkeeper.core.Dice;
import com.questkeeper.magic.*;

/**
 * Sacred Flame - Evocation cantrip
 * Target makes DEX save or takes radiant damage.
 * Damage scales: 1d8, 2d8 at 5th, 3d8 at 11th, 4d8 at 17th.
 * Target gains no benefit from cover for this save.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class SacredFlame extends AbstractSpell {

    public static final String SPELL_ID = "sacred_flame";

    private SacredFlame(Builder builder) {
        super(builder);
    }

    public static SacredFlame create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC) {
        return castAtLevel(caster, target, spellAttackBonus, spellSaveDC, 0);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC, int slotLevel) {
        if (target == null) {
            return SpellResult.error("Sacred Flame requires a target.");
        }

        if (!target.isAlive()) {
            return SpellResult.error("Target is already dead.");
        }

        // Target makes DEX saving throw
        int saveRoll = Dice.roll(20) + getTargetDexMod(target);
        boolean saved = saveRoll >= spellSaveDC;

        // Calculate damage dice based on caster level
        int damageDice = getDamageDice(caster.getLevel());

        // Roll damage (0 if saved - no half damage on cantrip)
        int damage = 0;
        if (!saved) {
            for (int i = 0; i < damageDice; i++) {
                damage += Dice.roll(8);
            }
            target.takeDamage(damage);
        }

        if (saved) {
            return SpellResult.saveSuccess(this, target, saveRoll, spellSaveDC, 0, "radiant");
        } else {
            return SpellResult.saveFailed(this, target, saveRoll, spellSaveDC, damage, "radiant");
        }
    }

    /**
     * Gets the target's DEX modifier for the saving throw.
     * If target is a Character, uses their ability. Otherwise estimates from AC.
     */
    private int getTargetDexMod(Combatant target) {
        if (target instanceof Character character) {
            return character.getAbilityModifier(Ability.DEXTERITY);
        }
        // For monsters, estimate DEX mod from AC (rough approximation)
        // Typical monster AC = 10 + DEX mod + natural armor
        // We'll assume +2 DEX mod as a reasonable default
        return 2;
    }

    /**
     * Gets the number of damage dice based on caster level.
     */
    public static int getDamageDice(int casterLevel) {
        if (casterLevel >= 17) return 4;
        if (casterLevel >= 11) return 3;
        if (casterLevel >= 5) return 2;
        return 1;
    }

    private static class Builder extends AbstractSpell.Builder<Builder> {

        public Builder() {
            id(SPELL_ID);
            name("Sacred Flame");
            description("Flame-like radiance descends on a creature that you can see within range. " +
                "The target must succeed on a Dexterity saving throw or take 1d8 radiant damage. " +
                "The target gains no benefit from cover for this saving throw.\n\n" +
                "The spell's damage increases by 1d8 when you reach 5th level (2d8), 11th level (3d8), " +
                "and 17th level (4d8).");
            level(0);  // Cantrip
            school(SpellSchool.EVOCATION);
            castingTime(CastingTime.ACTION);
            rangeFeet(60);
            components(SpellComponent.VERBAL, SpellComponent.SOMATIC);
            duration(SpellDuration.INSTANTANEOUS);
            targetEnemy();
            savingThrow(Ability.DEXTERITY);
        }

        @Override
        public SacredFlame build() {
            return new SacredFlame(this);
        }
    }
}
