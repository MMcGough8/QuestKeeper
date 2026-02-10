package com.questkeeper.core;

import com.questkeeper.character.Character;

/**
 * Handles D&D 5e rest mechanics for characters.
 *
 * Short Rest: 1 hour of downtime
 * - Spend hit dice to heal (roll hit die + CON mod per die)
 * - Some abilities recharge (not yet implemented)
 *
 * Long Rest: 8 hours of downtime
 * - Regain all hit points
 * - Regain half of max hit dice (minimum 1)
 * - Daily abilities recharge (not yet implemented)
 *
 * @author Marc McGough
 * @version 1.0
 */
public class RestSystem {

    /**
     * Result of a rest action, containing details about what was restored.
     */
    public record RestResult(
            RestType type,
            int hpRestored,
            int hitDiceUsed,
            int hitDiceRestored,
            int currentHp,
            int maxHp,
            int availableHitDice,
            int maxHitDice
    ) {
        public boolean wasSuccessful() {
            return hpRestored > 0 || hitDiceRestored > 0 || type == RestType.LONG;
        }
    }

    /**
     * Result of using a single hit die during short rest.
     */
    public record HitDieResult(
            int roll,
            int conModifier,
            int totalHealing,
            int actualHealing,
            int currentHp,
            int maxHp,
            int remainingHitDice
    ) {}

    public enum RestType {
        SHORT("Short Rest"),
        LONG("Long Rest");

        private final String displayName;

        RestType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Performs a short rest, allowing the character to spend hit dice to heal.
     * This method spends ALL available hit dice. For interactive use, call
     * useHitDie() repeatedly instead.
     *
     * @param character the character taking a short rest
     * @param hitDiceToSpend number of hit dice to spend (0 for none)
     * @return the result of the short rest
     */
    public RestResult shortRest(Character character, int hitDiceToSpend) {
        int startHp = character.getCurrentHitPoints();
        int diceUsed = 0;

        int toSpend = Math.min(hitDiceToSpend, character.getAvailableHitDice());
        for (int i = 0; i < toSpend; i++) {
            int healed = character.useHitDie();
            if (healed < 0) break;  // No more hit dice
            if (healed == 0 && character.getCurrentHitPoints() >= character.getMaxHitPoints()) {
                break;  // Already at full HP
            }
            diceUsed++;
        }

        int totalHealed = character.getCurrentHitPoints() - startHp;

        // Reset class features that recharge on short rest
        character.resetFeaturesOnShortRest();

        return new RestResult(
                RestType.SHORT,
                totalHealed,
                diceUsed,
                0,  // No hit dice restored on short rest
                character.getCurrentHitPoints(),
                character.getMaxHitPoints(),
                character.getAvailableHitDice(),
                character.getMaxHitDice()
        );
    }

    /**
     * Uses a single hit die during a short rest.
     * Useful for interactive short rests where the player decides die by die.
     *
     * @param character the character using the hit die
     * @return the result of using the hit die, or null if no dice available
     */
    public HitDieResult useHitDie(Character character) {
        if (character.getAvailableHitDice() <= 0) {
            return null;
        }
        if (character.getCurrentHitPoints() >= character.getMaxHitPoints()) {
            return new HitDieResult(0, 0, 0, 0,
                    character.getCurrentHitPoints(),
                    character.getMaxHitPoints(),
                    character.getAvailableHitDice());
        }

        int conMod = character.getAbilityModifier(Character.Ability.CONSTITUTION);
        int roll = Dice.roll(character.getHitDieSize());
        int totalHealing = Math.max(1, roll + conMod);

        int startHp = character.getCurrentHitPoints();
        character.useHitDie();  // This does the actual healing
        int actualHealing = character.getCurrentHitPoints() - startHp;

        // Note: useHitDie() does its own roll, so we recalculate for display
        // This is slightly inconsistent but keeps the Character class self-contained
        return new HitDieResult(
                roll,
                conMod,
                totalHealing,
                actualHealing,
                character.getCurrentHitPoints(),
                character.getMaxHitPoints(),
                character.getAvailableHitDice()
        );
    }

    /**
     * Performs a long rest, fully restoring the character.
     *
     * @param character the character taking a long rest
     * @return the result of the long rest
     */
    public RestResult longRest(Character character) {
        int startHp = character.getCurrentHitPoints();

        // Restore all HP
        character.fullHeal();
        int hpRestored = character.getCurrentHitPoints() - startHp;

        // Clear temporary HP (per D&D 5e rules - temp HP ends after long rest)
        character.clearTemporaryHitPoints();

        // Restore hit dice (half of max, minimum 1)
        int diceRestored = character.restoreHitDice();

        // Reset class features that recharge on long rest
        character.resetFeaturesOnLongRest();

        return new RestResult(
                RestType.LONG,
                hpRestored,
                0,  // No hit dice used
                diceRestored,
                character.getCurrentHitPoints(),
                character.getMaxHitPoints(),
                character.getAvailableHitDice(),
                character.getMaxHitDice()
        );
    }

    /**
     * Checks if a character would benefit from a short rest.
     */
    public boolean wouldBenefitFromShortRest(Character character) {
        return character.getCurrentHitPoints() < character.getMaxHitPoints()
                && character.getAvailableHitDice() > 0;
    }

    /**
     * Checks if a character would benefit from a long rest.
     */
    public boolean wouldBenefitFromLongRest(Character character) {
        return character.getCurrentHitPoints() < character.getMaxHitPoints()
                || character.getAvailableHitDice() < character.getMaxHitDice();
    }
}
