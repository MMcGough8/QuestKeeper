package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;
import com.questkeeper.core.Dice;
import com.questkeeper.magic.AbstractSpell;
import com.questkeeper.magic.CastingTime;
import com.questkeeper.magic.Spell;
import com.questkeeper.magic.SpellComponent;
import com.questkeeper.magic.SpellDuration;
import com.questkeeper.magic.SpellResult;
import com.questkeeper.magic.SpellSchool;

/**
 * Healing Word — 1st-level Evocation. Bonus-action ranged heal.
 * 1d4 + spell mod, +1d4 per slot level above 1st.
 */
public class HealingWord extends AbstractSpell {

    public static final String SPELL_ID = "healing_word";

    private HealingWord(Builder builder) {
        super(builder);
    }

    public static HealingWord create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int attackBonus, int saveDc) {
        return castAtLevel(caster, target, attackBonus, saveDc, 1);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int attackBonus, int saveDc, int slotLevel) {
        if (target == null) target = caster;
        if (!target.isAlive()) return SpellResult.error("Healing Word can't restore a dead creature.");

        int dice = slotLevel;
        int healing = 0;
        for (int i = 0; i < dice; i++) healing += Dice.roll(4);
        int abilityMod = saveDc - 8 - ((caster.getLevel() - 1) / 4 + 2);
        healing += Math.max(0, abilityMod);

        int actual = target.heal(healing);
        String msg = String.format(
            "%s speaks a word of healing%s, restoring %d HP to %s. (%d/%d HP)",
            caster.getName(),
            slotLevel > 1 ? " at slot level " + slotLevel : "",
            actual, target.getName(),
            target.getCurrentHitPoints(), target.getMaxHitPoints());

        return new SpellResult.Builder(SpellResult.Type.HEALING, msg)
            .spell(this).target(target).healing(actual).build();
    }

    private static class Builder extends AbstractSpell.Builder<Builder> {
        Builder() {
            id(SPELL_ID);
            name("Healing Word");
            description("A creature of your choice within 60 feet regains hit points equal to "
                + "1d4 + your spellcasting modifier. Bonus action. "
                + "At higher levels: +1d4 per slot level above 1st.");
            level(1);
            school(SpellSchool.EVOCATION);
            castingTime(CastingTime.BONUS_ACTION);
            rangeFeet(60);
            components(SpellComponent.VERBAL);
            duration(SpellDuration.INSTANTANEOUS);
            targetAlly();
        }

        @Override
        public HealingWord build() {
            return new HealingWord(this);
        }
    }
}
