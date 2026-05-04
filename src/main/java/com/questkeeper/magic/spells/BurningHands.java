package com.questkeeper.magic.spells;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;
import com.questkeeper.combat.Monster;
import com.questkeeper.core.Dice;
import com.questkeeper.magic.AbstractSpell;
import com.questkeeper.magic.CastingTime;
import com.questkeeper.magic.Spell;
import com.questkeeper.magic.SpellComponent;
import com.questkeeper.magic.SpellDuration;
import com.questkeeper.magic.SpellResult;
import com.questkeeper.magic.SpellSchool;

/**
 * Burning Hands — 1st-level Evocation. 15-ft cone, 3d6 fire, DEX save half.
 * +1d6 per slot level above 1st.
 *
 * <p>Single-target shorthand for the CLI: applies the cone to the supplied
 * target only. Future versions can extend this to AoE once group selection
 * exists in combat.
 */
public class BurningHands extends AbstractSpell {

    public static final String SPELL_ID = "burning_hands";

    private BurningHands(Builder builder) {
        super(builder);
    }

    public static BurningHands create() {
        return new Builder().build();
    }

    @Override
    public SpellResult cast(Character caster, Combatant target, int attackBonus, int saveDc) {
        return castAtLevel(caster, target, attackBonus, saveDc, 1);
    }

    @Override
    public SpellResult castAtLevel(Character caster, Combatant target, int attackBonus, int saveDc, int slotLevel) {
        if (target == null || !target.isAlive()) {
            return SpellResult.error("Burning Hands needs a living target.");
        }
        int dice = 2 + slotLevel;  // 3d6 at L1, +1 per upcast level
        int damage = 0;
        for (int i = 0; i < dice; i++) damage += Dice.roll(6);

        int dexMod = (target instanceof Monster m) ? m.getDexterityMod() : 0;
        int saveRoll = Dice.rollWithModifier(20, dexMod);
        boolean saved = saveRoll >= saveDc;
        int finalDamage = saved ? damage / 2 : damage;
        target.takeDamage(finalDamage);

        String msg = String.format(
            "%s unleashes a cone of flame%s! [DEX save %d vs DC %d -> %s] %s takes %d fire damage. (%d/%d HP)",
            caster.getName(),
            slotLevel > 1 ? " at slot level " + slotLevel : "",
            saveRoll, saveDc, saved ? "SAVE (half)" : "FAIL",
            target.getName(), finalDamage,
            target.getCurrentHitPoints(), target.getMaxHitPoints());

        SpellResult.Type type = saved ? SpellResult.Type.SAVE_SUCCESS : SpellResult.Type.SAVE_FAILED;
        return new SpellResult.Builder(type, msg)
            .spell(this).target(target)
            .damage(finalDamage, "fire")
            .saveRoll(saveRoll).saveDC(saveDc).build();
    }

    private static class Builder extends AbstractSpell.Builder<Builder> {
        Builder() {
            id(SPELL_ID);
            name("Burning Hands");
            description("As you hold your hands with thumbs touching and fingers spread, a thin "
                + "sheet of flames shoots forth. Each creature in a 15-foot cone must make a "
                + "Dexterity saving throw, taking 3d6 fire damage on failure or half on success. "
                + "At higher levels: +1d6 per slot level above 1st.");
            level(1);
            school(SpellSchool.EVOCATION);
            castingTime(CastingTime.ACTION);
            rangeSelf();
            components(SpellComponent.VERBAL, SpellComponent.SOMATIC);
            duration(SpellDuration.INSTANTANEOUS);
            targetEnemy();
        }

        @Override
        public BurningHands build() {
            return new BurningHands(this);
        }
    }
}
