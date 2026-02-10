package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for Monk class features.
 *
 * Monk features by level:
 * - Level 1: Unarmored Defense, Martial Arts
 * - Level 2: Ki, Flurry of Blows, Patient Defense, Step of the Wind
 * - Level 3: Monastic Tradition (Way of the Open Hand), Deflect Missiles
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class MonkFeatures {

    // Feature IDs
    public static final String UNARMORED_DEFENSE_ID = "monk_unarmored_defense";
    public static final String MARTIAL_ARTS_ID = "martial_arts";
    public static final String KI_ID = "ki";
    public static final String FLURRY_OF_BLOWS_ID = "flurry_of_blows";
    public static final String PATIENT_DEFENSE_ID = "patient_defense";
    public static final String STEP_OF_THE_WIND_ID = "step_of_the_wind";
    public static final String DEFLECT_MISSILES_ID = "deflect_missiles";
    public static final String OPEN_HAND_TECHNIQUE_ID = "open_hand_technique";

    private MonkFeatures() {
        // Utility class
    }

    /**
     * Creates all Monk features appropriate for the given level.
     *
     * @param level the character's Monk level
     * @return list of features available at this level
     */
    public static List<ClassFeature> createFeaturesForLevel(int level) {
        List<ClassFeature> features = new ArrayList<>();

        // Level 1: Unarmored Defense, Martial Arts
        if (level >= 1) {
            features.add(createUnarmoredDefense());
            features.add(createMartialArts(level));
        }

        // Level 2: Ki, Flurry of Blows, Patient Defense, Step of the Wind
        if (level >= 2) {
            features.add(createKi(level));
            features.add(createFlurryOfBlows());
            features.add(createPatientDefense());
            features.add(createStepOfTheWind());
        }

        // Level 3: Monastic Tradition (Open Hand), Deflect Missiles
        if (level >= 3) {
            features.add(createDeflectMissiles(level));
            features.add(createOpenHandTechnique());
        }

        return features;
    }

    /**
     * Gets the Martial Arts die size based on Monk level.
     * Level 1-4: d4, Level 5-10: d6, Level 11-16: d8, Level 17+: d10
     */
    public static int getMartialArtsDie(int level) {
        if (level >= 17) return 10;
        if (level >= 11) return 8;
        if (level >= 5) return 6;
        return 4;
    }

    /**
     * Gets the number of Ki points based on Monk level.
     * Ki points = Monk level
     */
    public static int calculateKiPoints(int level) {
        return level;
    }

    /**
     * Gets the Ki save DC based on Monk stats.
     * DC = 8 + proficiency + WIS modifier
     */
    public static int getKiSaveDC(Character monk) {
        return 8 + monk.getProficiencyBonus() + monk.getAbilityModifier(Ability.WISDOM);
    }

    // ==========================================
    // Factory Methods
    // ==========================================

    public static UnarmoredDefense createUnarmoredDefense() {
        return new UnarmoredDefense();
    }

    public static MartialArts createMartialArts(int monkLevel) {
        return new MartialArts(monkLevel);
    }

    public static Ki createKi(int monkLevel) {
        return new Ki(monkLevel);
    }

    public static FlurryOfBlows createFlurryOfBlows() {
        return new FlurryOfBlows();
    }

    public static PatientDefense createPatientDefense() {
        return new PatientDefense();
    }

    public static StepOfTheWind createStepOfTheWind() {
        return new StepOfTheWind();
    }

    public static DeflectMissiles createDeflectMissiles(int monkLevel) {
        return new DeflectMissiles(monkLevel);
    }

    public static OpenHandTechnique createOpenHandTechnique() {
        return new OpenHandTechnique();
    }

    // ==========================================
    // Concrete Feature Classes
    // ==========================================

    /**
     * Unarmored Defense (Monk) - AC = 10 + DEX + WIS when not wearing armor.
     * Unlike Barbarian, Monks cannot use a shield with Unarmored Defense.
     */
    public static class UnarmoredDefense extends PassiveFeature {

        public UnarmoredDefense() {
            super(
                UNARMORED_DEFENSE_ID,
                "Unarmored Defense",
                "While you are wearing no armor and not wielding a shield, your AC equals " +
                    "10 + your Dexterity modifier + your Wisdom modifier.",
                1  // Level required
            );
        }

        /**
         * Calculates unarmored AC for the given character.
         */
        public int calculateAC(Character character) {
            int dexMod = character.getAbilityModifier(Ability.DEXTERITY);
            int wisMod = character.getAbilityModifier(Ability.WISDOM);
            return 10 + dexMod + wisMod;
        }
    }

    /**
     * Martial Arts - Enhanced unarmed combat abilities.
     * - Use DEX instead of STR for unarmed/monk weapons
     * - Unarmed damage scales with level
     * - Bonus action unarmed strike after Attack action
     */
    public static class MartialArts extends PassiveFeature {

        private int monkLevel;

        public MartialArts(int monkLevel) {
            super(
                MARTIAL_ARTS_ID,
                "Martial Arts",
                "Your practice of martial arts gives you mastery of combat styles that use " +
                    "unarmed strikes and monk weapons. You gain the following benefits:\n" +
                    "- You can use Dexterity instead of Strength for attack and damage rolls\n" +
                    "- You can roll your Martial Arts die in place of normal damage\n" +
                    "- When you use the Attack action, you can make one unarmed strike as a bonus action",
                1  // Level required
            );
            this.monkLevel = monkLevel;
        }

        /**
         * Gets the Martial Arts damage die size.
         */
        public int getDamageDie() {
            return getMartialArtsDie(monkLevel);
        }

        /**
         * Gets the Martial Arts damage notation (e.g., "1d6").
         */
        public String getDamageNotation() {
            return "1d" + getDamageDie();
        }

        /**
         * Updates the Monk level (called when leveling up).
         */
        public void setMonkLevel(int level) {
            this.monkLevel = level;
        }

        public int getMonkLevel() {
            return monkLevel;
        }
    }

    /**
     * Ki - Pool of mystical energy for special abilities.
     * Ki points = Monk level, all restored on short or long rest.
     */
    public static class Ki extends ActivatedFeature {

        private int monkLevel;

        public Ki(int monkLevel) {
            super(
                KI_ID,
                "Ki",
                "Your training allows you to harness the mystic energy of ki. Your access to " +
                    "this energy is represented by a number of ki points equal to your monk level.\n" +
                    "You regain all expended ki points when you finish a short or long rest.",
                2,  // Level required
                calculateKiPoints(monkLevel),  // Max uses = monk level
                ResetType.SHORT_REST,
                true,   // Available in combat
                true    // Available outside combat (for Step of the Wind movement)
            );
            this.monkLevel = monkLevel;
        }

        @Override
        protected String activate(Character user) {
            // Ki itself isn't activated - individual ki abilities are
            return "You focus your ki energy. (" + getCurrentUses() + " ki points remaining)";
        }

        /**
         * Spends ki points for an ability.
         * @param cost the number of ki points to spend
         * @return true if successfully spent, false if not enough ki
         */
        public boolean spendKi(int cost) {
            return spendUses(cost);
        }

        /**
         * Gets remaining ki points.
         */
        public int getKiPoints() {
            return getCurrentUses();
        }

        /**
         * Gets max ki points.
         */
        public int getMaxKiPoints() {
            return getMaxUses();
        }

        /**
         * Updates the Monk level (for scaling ki points).
         */
        public void setMonkLevel(int level) {
            this.monkLevel = level;
            setMaxUses(calculateKiPoints(level));
        }

        public int getMonkLevel() {
            return monkLevel;
        }
    }

    /**
     * Flurry of Blows - Spend 1 ki for two bonus action unarmed strikes.
     */
    public static class FlurryOfBlows extends PassiveFeature {

        public FlurryOfBlows() {
            super(
                FLURRY_OF_BLOWS_ID,
                "Flurry of Blows",
                "Immediately after you take the Attack action on your turn, you can spend " +
                    "1 ki point to make two unarmed strikes as a bonus action.",
                2  // Level required
            );
        }
    }

    /**
     * Patient Defense - Spend 1 ki to Dodge as bonus action.
     */
    public static class PatientDefense extends PassiveFeature {

        public PatientDefense() {
            super(
                PATIENT_DEFENSE_ID,
                "Patient Defense",
                "You can spend 1 ki point to take the Dodge action as a bonus action on your turn. " +
                    "Until the start of your next turn, any attack roll made against you has " +
                    "disadvantage if you can see the attacker.",
                2  // Level required
            );
        }
    }

    /**
     * Step of the Wind - Spend 1 ki to Dash or Disengage as bonus action.
     */
    public static class StepOfTheWind extends PassiveFeature {

        public StepOfTheWind() {
            super(
                STEP_OF_THE_WIND_ID,
                "Step of the Wind",
                "You can spend 1 ki point to take the Disengage or Dash action as a bonus action " +
                    "on your turn, and your jump distance is doubled for the turn.",
                2  // Level required
            );
        }
    }

    /**
     * Deflect Missiles - Reduce ranged weapon attack damage, possibly catch and throw.
     */
    public static class DeflectMissiles extends PassiveFeature {

        private int monkLevel;

        public DeflectMissiles(int monkLevel) {
            super(
                DEFLECT_MISSILES_ID,
                "Deflect Missiles",
                "You can use your reaction to deflect or catch the missile when you are hit by a " +
                    "ranged weapon attack. The damage is reduced by 1d10 + DEX modifier + monk level.\n" +
                    "If you reduce the damage to 0, you can spend 1 ki point to make a ranged attack " +
                    "with the caught missile as part of the same reaction.",
                3  // Level required
            );
            this.monkLevel = monkLevel;
        }

        /**
         * Calculates the damage reduction for Deflect Missiles.
         * @param character the monk
         * @param roll the 1d10 roll result
         * @return total damage reduction
         */
        public int calculateReduction(Character character, int roll) {
            return roll + character.getAbilityModifier(Ability.DEXTERITY) + monkLevel;
        }

        /**
         * Gets the damage reduction formula string.
         */
        public String getReductionFormula(Character character) {
            int dexMod = character.getAbilityModifier(Ability.DEXTERITY);
            String dexStr = dexMod >= 0 ? "+" + dexMod : String.valueOf(dexMod);
            return "1d10" + dexStr + "+" + monkLevel;
        }

        public void setMonkLevel(int level) {
            this.monkLevel = level;
        }

        public int getMonkLevel() {
            return monkLevel;
        }
    }

    /**
     * Open Hand Technique (Way of the Open Hand) - Enhanced Flurry of Blows.
     */
    public static class OpenHandTechnique extends PassiveFeature {

        public OpenHandTechnique() {
            super(
                OPEN_HAND_TECHNIQUE_ID,
                "Open Hand Technique",
                "Whenever you hit a creature with one of the attacks granted by your Flurry of Blows, " +
                    "you can impose one of the following effects on that target:\n" +
                    "- It must succeed on a DEX save or be knocked prone\n" +
                    "- It must make a STR save or be pushed up to 15 feet away\n" +
                    "- It can't take reactions until the end of your next turn",
                3  // Level required
            );
        }
    }
}
