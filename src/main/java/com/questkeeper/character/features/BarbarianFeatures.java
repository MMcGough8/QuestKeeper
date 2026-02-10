package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for Barbarian class features.
 *
 * Barbarian features by level:
 * - Level 1: Rage, Unarmored Defense
 * - Level 2: Reckless Attack, Danger Sense
 * - Level 3: Primal Path (Berserker: Frenzy)
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class BarbarianFeatures {

    // Feature IDs
    public static final String RAGE_ID = "rage";
    public static final String UNARMORED_DEFENSE_ID = "unarmored_defense";
    public static final String RECKLESS_ATTACK_ID = "reckless_attack";
    public static final String DANGER_SENSE_ID = "danger_sense";
    public static final String FRENZY_ID = "frenzy";

    private BarbarianFeatures() {
        // Utility class
    }

    /**
     * Creates all Barbarian features appropriate for the given level.
     *
     * @param level the character's Barbarian level
     * @return list of features available at this level
     */
    public static List<ClassFeature> createFeaturesForLevel(int level) {
        List<ClassFeature> features = new ArrayList<>();

        // Level 1: Rage, Unarmored Defense
        if (level >= 1) {
            features.add(createRage(level));
            features.add(createUnarmoredDefense());
        }

        // Level 2: Reckless Attack, Danger Sense
        if (level >= 2) {
            features.add(createRecklessAttack());
            features.add(createDangerSense());
        }

        // Level 3: Primal Path - Berserker (Frenzy)
        if (level >= 3) {
            features.add(createFrenzy());
        }

        return features;
    }

    /**
     * Gets the number of rages per long rest based on Barbarian level.
     */
    public static int getRagesPerLongRest(int level) {
        if (level >= 20) return Integer.MAX_VALUE;  // Unlimited
        if (level >= 17) return 6;
        if (level >= 12) return 5;
        if (level >= 6) return 4;
        if (level >= 3) return 3;
        return 2;  // Levels 1-2
    }

    /**
     * Gets the rage damage bonus based on Barbarian level.
     */
    public static int getRageDamageBonus(int level) {
        if (level >= 16) return 4;
        if (level >= 9) return 3;
        return 2;  // Levels 1-8
    }

    /**
     * Creates the Rage feature.
     */
    public static Rage createRage(int barbarianLevel) {
        return new Rage(barbarianLevel);
    }

    /**
     * Creates the Unarmored Defense feature.
     */
    public static UnarmoredDefense createUnarmoredDefense() {
        return new UnarmoredDefense();
    }

    /**
     * Creates the Reckless Attack feature.
     */
    public static RecklessAttack createRecklessAttack() {
        return new RecklessAttack();
    }

    /**
     * Creates the Danger Sense feature.
     */
    public static DangerSense createDangerSense() {
        return new DangerSense();
    }

    /**
     * Creates the Frenzy feature (Berserker path).
     */
    public static Frenzy createFrenzy() {
        return new Frenzy();
    }

    // ==========================================
    // Concrete Feature Classes
    // ==========================================

    /**
     * Rage - Enter a battle fury for bonus damage and resistances.
     * Uses bonus action. Lasts up to 1 minute (10 rounds).
     */
    public static class Rage extends ActivatedFeature {

        private int barbarianLevel;
        private boolean active = false;
        private int roundsRemaining = 0;

        public Rage(int barbarianLevel) {
            super(
                RAGE_ID,
                "Rage",
                "As a bonus action, enter a rage for up to 1 minute. While raging:\n" +
                    "- Advantage on Strength checks and saving throws\n" +
                    "- Bonus damage on melee attacks using Strength\n" +
                    "- Resistance to bludgeoning, piercing, and slashing damage\n" +
                    "Rage ends early if knocked unconscious or if your turn ends " +
                    "without attacking or taking damage since your last turn.",
                1,  // Level required
                getRagesPerLongRest(barbarianLevel),  // Max uses
                ResetType.LONG_REST,
                true,   // Available in combat
                false   // Not useful outside combat
            );
            this.barbarianLevel = barbarianLevel;
        }

        @Override
        protected String activate(Character user) {
            active = true;
            roundsRemaining = 10;  // 1 minute = 10 rounds

            int damageBonus = getRageDamageBonus(barbarianLevel);
            return String.format(
                "RAGE! %s enters a furious battle rage!\n" +
                "- +%d bonus damage on melee STR attacks\n" +
                "- Resistance to physical damage\n" +
                "- Advantage on STR checks and saves\n" +
                "(Rage lasts up to 10 rounds)",
                user.getName(), damageBonus);
        }

        /**
         * Checks if rage is currently active.
         */
        public boolean isActive() {
            return active;
        }

        /**
         * Ends the rage early.
         */
        public void endRage() {
            active = false;
            roundsRemaining = 0;
        }

        /**
         * Called at end of each turn. Decrements duration.
         * @return true if rage is still active
         */
        public boolean processTurnEnd() {
            if (!active) return false;

            roundsRemaining--;
            if (roundsRemaining <= 0) {
                endRage();
                return false;
            }
            return true;
        }

        /**
         * Gets the rage damage bonus at current level.
         */
        public int getDamageBonus() {
            return getRageDamageBonus(barbarianLevel);
        }

        /**
         * Gets rounds remaining on current rage.
         */
        public int getRoundsRemaining() {
            return roundsRemaining;
        }

        /**
         * Updates the Barbarian level (for scaling uses and damage).
         */
        public void setBarbarianLevel(int level) {
            this.barbarianLevel = level;
            setMaxUses(getRagesPerLongRest(level));
        }

        public int getBarbarianLevel() {
            return barbarianLevel;
        }
    }

    /**
     * Unarmored Defense - AC = 10 + DEX + CON when not wearing armor.
     */
    public static class UnarmoredDefense extends PassiveFeature {

        public UnarmoredDefense() {
            super(
                UNARMORED_DEFENSE_ID,
                "Unarmored Defense",
                "While you are not wearing any armor, your Armor Class equals " +
                    "10 + your Dexterity modifier + your Constitution modifier. " +
                    "You can use a shield and still gain this benefit.",
                1  // Level required
            );
        }

        /**
         * Calculates unarmored AC for the given character.
         */
        public int calculateAC(Character character) {
            int dexMod = character.getAbilityModifier(Ability.DEXTERITY);
            int conMod = character.getAbilityModifier(Ability.CONSTITUTION);
            return 10 + dexMod + conMod;
        }
    }

    /**
     * Reckless Attack - Trade defense for offense.
     * Gain advantage on attacks, but enemies have advantage on you until next turn.
     */
    public static class RecklessAttack extends ActivatedFeature {

        private boolean activeThisTurn = false;

        public RecklessAttack() {
            super(
                RECKLESS_ATTACK_ID,
                "Reckless Attack",
                "When you make your first attack on your turn, you can decide to " +
                    "attack recklessly. Doing so gives you advantage on melee weapon " +
                    "attack rolls using Strength during this turn, but attack rolls " +
                    "against you have advantage until your next turn.",
                2,  // Level required
                1,  // Max uses (resets each turn)
                ResetType.UNLIMITED,
                true,   // Available in combat
                false   // Not useful outside combat
            );
        }

        @Override
        protected String activate(Character user) {
            activeThisTurn = true;
            return String.format(
                "%s attacks RECKLESSLY!\n" +
                "- Advantage on melee STR attacks this turn\n" +
                "- Enemies have advantage on attacks against you until your next turn",
                user.getName());
        }

        /**
         * Checks if Reckless Attack is active this turn.
         */
        public boolean isActiveThisTurn() {
            return activeThisTurn;
        }

        /**
         * Resets at the start of each turn.
         */
        public void resetTurn() {
            activeThisTurn = false;
        }

        @Override
        public boolean canUse() {
            return !activeThisTurn;  // Can only use once per turn
        }
    }

    /**
     * Danger Sense - Advantage on DEX saves against effects you can see.
     */
    public static class DangerSense extends PassiveFeature {

        public DangerSense() {
            super(
                DANGER_SENSE_ID,
                "Danger Sense",
                "You have advantage on Dexterity saving throws against effects that " +
                    "you can see, such as traps and spells. To gain this benefit, you " +
                    "can't be blinded, deafened, or incapacitated.",
                2  // Level required
            );
        }
    }

    /**
     * Frenzy (Berserker) - Bonus action attack while raging.
     * Causes exhaustion when rage ends.
     */
    public static class Frenzy extends PassiveFeature {

        public Frenzy() {
            super(
                FRENZY_ID,
                "Frenzy",
                "You can go into a frenzy when you rage. If you do so, for the " +
                    "duration of your rage you can make a single melee weapon attack " +
                    "as a bonus action on each of your turns after this one. When your " +
                    "rage ends, you suffer one level of exhaustion.",
                3  // Level required
            );
        }
    }
}
