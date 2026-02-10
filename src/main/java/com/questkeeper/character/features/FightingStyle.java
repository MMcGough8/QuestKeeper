package com.questkeeper.character.features;

/**
 * Fighter Fighting Styles available at level 1.
 *
 * Each style provides a distinct combat bonus based on playstyle.
 *
 * @author Marc McGough
 * @version 1.0
 */
public enum FightingStyle {
    /**
     * +1 bonus to AC while wearing armor.
     */
    DEFENSE("Defense",
        "+1 bonus to AC while you are wearing armor.",
        1, 0, 0, false),

    /**
     * +2 bonus to damage with one-handed melee weapons when no off-hand weapon.
     */
    DUELING("Dueling",
        "When you are wielding a melee weapon in one hand and no other weapons, " +
        "you gain a +2 bonus to damage rolls with that weapon.",
        0, 2, 0, false),

    /**
     * Reroll 1s and 2s on damage dice with two-handed weapons.
     */
    GREAT_WEAPON("Great Weapon Fighting",
        "When you roll a 1 or 2 on a damage die for an attack you make with a melee weapon " +
        "that you are wielding with two hands, you can reroll the die.",
        0, 0, 0, true),

    /**
     * +2 bonus to attack rolls with ranged weapons.
     */
    ARCHERY("Archery",
        "+2 bonus to attack rolls you make with ranged weapons.",
        0, 0, 2, false),

    /**
     * Add ability modifier to off-hand damage.
     */
    TWO_WEAPON("Two-Weapon Fighting",
        "When you engage in two-weapon fighting, you can add your ability modifier " +
        "to the damage of the second attack.",
        0, 0, 0, false);

    private final String displayName;
    private final String description;
    private final int acBonus;
    private final int damageBonus;
    private final int rangedAttackBonus;
    private final boolean rerollLowDamage;

    FightingStyle(String displayName, String description,
                  int acBonus, int damageBonus, int rangedAttackBonus, boolean rerollLowDamage) {
        this.displayName = displayName;
        this.description = description;
        this.acBonus = acBonus;
        this.damageBonus = damageBonus;
        this.rangedAttackBonus = rangedAttackBonus;
        this.rerollLowDamage = rerollLowDamage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the AC bonus from this fighting style.
     * Only DEFENSE provides an AC bonus.
     */
    public int getAcBonus() {
        return acBonus;
    }

    /**
     * Gets the damage bonus from this fighting style.
     * Only DUELING provides a flat damage bonus.
     */
    public int getDamageBonus() {
        return damageBonus;
    }

    /**
     * Gets the ranged attack roll bonus.
     * Only ARCHERY provides a ranged attack bonus.
     */
    public int getRangedAttackBonus() {
        return rangedAttackBonus;
    }

    /**
     * Returns true if this style allows rerolling low damage dice.
     * Only GREAT_WEAPON allows rerolling 1s and 2s.
     */
    public boolean allowsRerollLowDamage() {
        return rerollLowDamage;
    }
}
