package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;
import com.questkeeper.magic.*;

/**
 * Shield - 1st level Abjuration
 * Reaction spell that grants +5 AC until the start of your next turn.
 * Can be cast when hit by an attack or targeted by magic missile.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class Shield extends AbstractSpell {

    public static final String SPELL_ID = "shield";
    public static final int AC_BONUS = 5;

    private Shield(Builder builder) {
        super(builder);
    }

    public static Shield create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC) {
        return castAtLevel(caster, target, spellAttackBonus, spellSaveDC, 1);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int spellAttackBonus, int spellSaveDC, int slotLevel) {
        // Shield always targets self
        String message = String.format(
            "%s casts Shield!%n" +
            "An invisible barrier of magical force appears, granting +%d AC until the start of their next turn.",
            caster.getName(),
            AC_BONUS);

        return SpellResult.buff(this, caster, "+5 AC until start of next turn");
    }

    /**
     * Returns true since Shield can potentially block an incoming attack.
     * Combat system should check if attack roll + 5 would miss.
     */
    public boolean wouldBlockAttack(int attackRoll, int currentAC) {
        return attackRoll >= currentAC && attackRoll < (currentAC + AC_BONUS);
    }

    private static class Builder extends AbstractSpell.Builder<Builder> {

        public Builder() {
            id(SPELL_ID);
            name("Shield");
            description("An invisible barrier of magical force appears and protects you. " +
                "Until the start of your next turn, you have a +5 bonus to AC, including against " +
                "the triggering attack, and you take no damage from magic missile.");
            level(1);
            school(SpellSchool.ABJURATION);
            castingTime(CastingTime.REACTION);
            rangeSelf();
            components(SpellComponent.VERBAL, SpellComponent.SOMATIC);
            duration(SpellDuration.ONE_ROUND);
            targetAlly();  // Self-targeting
        }

        @Override
        public Shield build() {
            return new Shield(this);
        }
    }
}
