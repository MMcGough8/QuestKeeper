package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.core.Dice;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for Fighter class features.
 *
 * Fighter features by level:
 * - Level 1: Fighting Style, Second Wind
 * - Level 2: Action Surge
 * - Level 3: Martial Archetype (Champion: Improved Critical)
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class FighterFeatures {

    // Feature IDs
    public static final String SECOND_WIND_ID = "second_wind";
    public static final String ACTION_SURGE_ID = "action_surge";
    public static final String FIGHTING_STYLE_ID = "fighting_style";
    public static final String IMPROVED_CRITICAL_ID = "improved_critical";

    private FighterFeatures() {
        // Utility class
    }

    /**
     * Creates all Fighter features appropriate for the given level.
     *
     * @param level the character's Fighter level
     * @param fightingStyle the chosen fighting style (null if not yet chosen)
     * @return list of features available at this level
     */
    public static List<ClassFeature> createFeaturesForLevel(int level, FightingStyle fightingStyle) {
        List<ClassFeature> features = new ArrayList<>();

        // Level 1: Fighting Style + Second Wind
        if (level >= 1) {
            if (fightingStyle != null) {
                features.add(createFightingStyleFeature(fightingStyle));
            }
            features.add(createSecondWind());
        }

        // Level 2: Action Surge
        if (level >= 2) {
            features.add(createActionSurge());
        }

        // Level 3: Martial Archetype - Champion (Improved Critical)
        if (level >= 3) {
            features.add(createImprovedCritical());
        }

        return features;
    }

    /**
     * Creates the Second Wind feature.
     * Heals 1d10 + Fighter level as a bonus action, 1/short rest.
     */
    public static SecondWind createSecondWind() {
        return new SecondWind();
    }

    /**
     * Creates the Action Surge feature.
     * Grants an additional action on your turn, 1/short rest.
     */
    public static ActionSurge createActionSurge() {
        return new ActionSurge();
    }

    /**
     * Creates a Fighting Style feature for the given style.
     */
    public static FightingStyleFeature createFightingStyleFeature(FightingStyle style) {
        return new FightingStyleFeature(style);
    }

    /**
     * Creates the Improved Critical feature (Champion archetype).
     * Critical hits on 19-20 instead of just 20.
     */
    public static ImprovedCritical createImprovedCritical() {
        return new ImprovedCritical();
    }

    // ==========================================
    // Concrete Feature Classes
    // ==========================================

    /**
     * Second Wind - Heal 1d10 + Fighter level as a bonus action.
     * Usable once per short rest.
     */
    public static class SecondWind extends ActivatedFeature {

        public SecondWind() {
            super(
                SECOND_WIND_ID,
                "Second Wind",
                "On your turn, you can use a bonus action to regain hit points equal to " +
                    "1d10 + your Fighter level. Once you use this feature, you must finish " +
                    "a short or long rest before you can use it again.",
                1,  // Level required
                1,  // Max uses
                ResetType.SHORT_REST,
                true,   // Available in combat
                true    // Available out of combat (for healing between fights)
            );
        }

        @Override
        protected String activate(Character user) {
            int roll = Dice.roll(10);
            int level = user.getLevel();
            int healing = roll + level;

            int actualHealing = user.heal(healing);

            return String.format("Second Wind! Rolled d10: %d + %d (level) = %d healing. " +
                "Restored %d HP. (Now at %d/%d HP)",
                roll, level, healing, actualHealing,
                user.getCurrentHitPoints(), user.getMaxHitPoints());
        }
    }

    /**
     * Action Surge - Take one additional action on your turn.
     * Usable once per short rest (twice at Fighter level 17).
     */
    public static class ActionSurge extends ActivatedFeature {

        private boolean active = false;

        public ActionSurge() {
            super(
                ACTION_SURGE_ID,
                "Action Surge",
                "On your turn, you can take one additional action. " +
                    "Once you use this feature, you must finish a short or long rest " +
                    "before you can use it again.",
                2,  // Level required
                1,  // Max uses (becomes 2 at level 17)
                ResetType.SHORT_REST,
                true,   // Available in combat
                false   // Not useful outside combat
            );
        }

        @Override
        protected String activate(Character user) {
            active = true;
            return "Action Surge! You can take an additional action this turn.";
        }

        /**
         * Checks if Action Surge is currently active.
         */
        public boolean isActive() {
            return active;
        }

        /**
         * Consumes the Action Surge effect (called after taking the extra action).
         */
        public void consume() {
            active = false;
        }

        /**
         * Resets the active state at end of turn.
         */
        public void endTurn() {
            active = false;
        }
    }

    /**
     * Fighting Style - Passive combat bonus.
     */
    public static class FightingStyleFeature extends PassiveFeature {

        private final FightingStyle style;

        public FightingStyleFeature(FightingStyle style) {
            super(
                FIGHTING_STYLE_ID,
                "Fighting Style: " + style.getDisplayName(),
                style.getDescription(),
                1  // Level required
            );
            this.style = style;
        }

        public FightingStyle getStyle() {
            return style;
        }
    }

    /**
     * Improved Critical (Champion) - Critical hits on 19-20.
     */
    public static class ImprovedCritical extends PassiveFeature {

        public ImprovedCritical() {
            super(
                IMPROVED_CRITICAL_ID,
                "Improved Critical",
                "Your weapon attacks score a critical hit on a roll of 19 or 20.",
                3  // Level required
            );
        }

        /**
         * Gets the minimum roll needed for a critical hit.
         * Returns 19 for Improved Critical (standard is 20).
         */
        public int getCriticalThreshold() {
            return 19;
        }
    }
}
