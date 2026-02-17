package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;
import com.questkeeper.core.Dice;
import com.questkeeper.magic.*;

/**
 * Fire Bolt - Evocation cantrip
 * Ranged spell attack that deals 1d10 fire damage.
 * Damage increases at higher levels: 2d10 at 5th, 3d10 at 11th, 4d10 at 17th.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class FireBolt extends AbstractSpell {

    public static final String SPELL_ID = "fire_bolt";

    private FireBolt(Builder builder) {
        super(builder);
    }

    public static FireBolt create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC) {
        // Cantrips don't use slots, but scale with character level
        return castAtLevel(caster, target, spellAttackBonus, spellSaveDC, 0);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC, int slotLevel) {
        if (target == null) {
            return SpellResult.error("Fire Bolt requires a target.");
        }

        if (!target.isAlive()) {
            return SpellResult.error("Target is already dead.");
        }

        // Make spell attack roll
        int d20Roll = Dice.roll(20);
        boolean isCrit = d20Roll == 20;
        boolean isFumble = d20Roll == 1;
        int attackTotal = d20Roll + spellAttackBonus;
        int targetAC = target.getArmorClass();

        // Check for hit (natural 1 always misses, natural 20 always hits)
        if (isFumble || (!isCrit && attackTotal < targetAC)) {
            return SpellResult.miss(this, target, attackTotal, targetAC);
        }

        // Calculate damage dice based on caster level
        int damageDice = getDamageDice(caster.getLevel());

        // Roll damage
        int damage = 0;
        for (int i = 0; i < damageDice; i++) {
            damage += Dice.roll(10);
        }

        // Double dice on crit
        if (isCrit) {
            for (int i = 0; i < damageDice; i++) {
                damage += Dice.roll(10);
            }
        }

        // Apply damage
        target.takeDamage(damage);

        return SpellResult.hit(this, target, attackTotal, targetAC, damage, "fire", isCrit);
    }

    /**
     * Gets the number of damage dice based on caster level.
     * Cantrips scale at 5th, 11th, and 17th level.
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
            name("Fire Bolt");
            description("You hurl a mote of fire at a creature or object within range. " +
                "Make a ranged spell attack against the target. On a hit, the target takes 1d10 fire damage. " +
                "A flammable object hit by this spell ignites if it isn't being worn or carried.\n\n" +
                "The spell's damage increases by 1d10 when you reach 5th level (2d10), 11th level (3d10), " +
                "and 17th level (4d10).");
            level(0);  // Cantrip
            school(SpellSchool.EVOCATION);
            castingTime(CastingTime.ACTION);
            rangeFeet(120);
            components(SpellComponent.VERBAL, SpellComponent.SOMATIC);
            duration(SpellDuration.INSTANTANEOUS);
            targetEnemy();
            attackRoll();
        }

        @Override
        public FireBolt build() {
            return new FireBolt(this);
        }
    }
}
