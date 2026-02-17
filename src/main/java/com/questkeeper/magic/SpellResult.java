package com.questkeeper.magic;

import com.questkeeper.combat.Combatant;

/**
 * Result of casting a spell.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class SpellResult {

    public enum Type {
        SUCCESS,           // Spell cast successfully
        HIT,              // Attack spell hit target
        MISS,             // Attack spell missed
        SAVE_FAILED,      // Target failed saving throw
        SAVE_SUCCESS,     // Target succeeded saving throw (may still have effect)
        HEALING,          // Healing spell
        BUFF,             // Beneficial effect applied
        CONDITION,        // Condition applied to target
        ERROR             // Spell could not be cast
    }

    private final Type type;
    private final String message;
    private final Spell spell;
    private final Combatant target;
    private final int damage;
    private final int healing;
    private final String damageType;
    private final int attackRoll;
    private final int targetAC;
    private final int saveRoll;
    private final int saveDC;
    private final boolean critical;

    private SpellResult(Builder builder) {
        this.type = builder.type;
        this.message = builder.message;
        this.spell = builder.spell;
        this.target = builder.target;
        this.damage = builder.damage;
        this.healing = builder.healing;
        this.damageType = builder.damageType;
        this.attackRoll = builder.attackRoll;
        this.targetAC = builder.targetAC;
        this.saveRoll = builder.saveRoll;
        this.saveDC = builder.saveDC;
        this.critical = builder.critical;
    }

    // Getters
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public Spell getSpell() { return spell; }
    public Combatant getTarget() { return target; }
    public int getDamage() { return damage; }
    public int getHealing() { return healing; }
    public String getDamageType() { return damageType; }
    public int getAttackRoll() { return attackRoll; }
    public int getTargetAC() { return targetAC; }
    public int getSaveRoll() { return saveRoll; }
    public int getSaveDC() { return saveDC; }
    public boolean isCritical() { return critical; }

    public boolean isSuccess() {
        return type != Type.ERROR && type != Type.MISS;
    }

    public boolean dealsDamage() {
        return damage > 0;
    }

    public boolean heals() {
        return healing > 0;
    }

    // Static factory methods for common results
    public static SpellResult hit(Spell spell, Combatant target, int attackRoll, int targetAC,
            int damage, String damageType, boolean critical) {
        String msg = String.format("%s hits %s for %d %s damage!%s",
            spell.getName(), target.getName(), damage, damageType,
            critical ? " (CRITICAL!)" : "");
        return new Builder(Type.HIT, msg)
            .spell(spell)
            .target(target)
            .attackRoll(attackRoll)
            .targetAC(targetAC)
            .damage(damage, damageType)
            .critical(critical)
            .build();
    }

    public static SpellResult miss(Spell spell, Combatant target, int attackRoll, int targetAC) {
        String msg = String.format("%s misses %s. (Rolled %d vs AC %d)",
            spell.getName(), target.getName(), attackRoll, targetAC);
        return new Builder(Type.MISS, msg)
            .spell(spell)
            .target(target)
            .attackRoll(attackRoll)
            .targetAC(targetAC)
            .build();
    }

    public static SpellResult saveFailed(Spell spell, Combatant target, int saveRoll, int saveDC,
            int damage, String damageType) {
        String msg = String.format("%s fails their save against %s! (%d vs DC %d) Takes %d %s damage.",
            target.getName(), spell.getName(), saveRoll, saveDC, damage, damageType);
        return new Builder(Type.SAVE_FAILED, msg)
            .spell(spell)
            .target(target)
            .saveRoll(saveRoll)
            .saveDC(saveDC)
            .damage(damage, damageType)
            .build();
    }

    public static SpellResult saveSuccess(Spell spell, Combatant target, int saveRoll, int saveDC,
            int damage, String damageType) {
        String msg;
        if (damage > 0) {
            msg = String.format("%s saves against %s! (%d vs DC %d) Takes %d %s damage (half).",
                target.getName(), spell.getName(), saveRoll, saveDC, damage, damageType);
        } else {
            msg = String.format("%s saves against %s! (%d vs DC %d) No effect.",
                target.getName(), spell.getName(), saveRoll, saveDC);
        }
        return new Builder(Type.SAVE_SUCCESS, msg)
            .spell(spell)
            .target(target)
            .saveRoll(saveRoll)
            .saveDC(saveDC)
            .damage(damage, damageType)
            .build();
    }

    public static SpellResult healing(Spell spell, Combatant target, int amountHealed) {
        String msg = String.format("%s heals %s for %d HP!",
            spell.getName(), target.getName(), amountHealed);
        return new Builder(Type.HEALING, msg)
            .spell(spell)
            .target(target)
            .healing(amountHealed)
            .build();
    }

    public static SpellResult buff(Spell spell, Combatant target, String effectDescription) {
        String msg = String.format("%s affects %s: %s",
            spell.getName(), target.getName(), effectDescription);
        return new Builder(Type.BUFF, msg)
            .spell(spell)
            .target(target)
            .build();
    }

    public static SpellResult success(Spell spell, String message) {
        return new Builder(Type.SUCCESS, message)
            .spell(spell)
            .build();
    }

    public static SpellResult error(String message) {
        return new Builder(Type.ERROR, message).build();
    }

    // Builder class
    public static class Builder {
        private final Type type;
        private final String message;
        private Spell spell;
        private Combatant target;
        private int damage;
        private int healing;
        private String damageType;
        private int attackRoll;
        private int targetAC;
        private int saveRoll;
        private int saveDC;
        private boolean critical;

        public Builder(Type type, String message) {
            this.type = type;
            this.message = message;
        }

        public Builder spell(Spell spell) {
            this.spell = spell;
            return this;
        }

        public Builder target(Combatant target) {
            this.target = target;
            return this;
        }

        public Builder damage(int damage, String damageType) {
            this.damage = damage;
            this.damageType = damageType;
            return this;
        }

        public Builder healing(int healing) {
            this.healing = healing;
            return this;
        }

        public Builder attackRoll(int attackRoll) {
            this.attackRoll = attackRoll;
            return this;
        }

        public Builder targetAC(int targetAC) {
            this.targetAC = targetAC;
            return this;
        }

        public Builder saveRoll(int saveRoll) {
            this.saveRoll = saveRoll;
            return this;
        }

        public Builder saveDC(int saveDC) {
            this.saveDC = saveDC;
            return this;
        }

        public Builder critical(boolean critical) {
            this.critical = critical;
            return this;
        }

        public SpellResult build() {
            return new SpellResult(this);
        }
    }
}
