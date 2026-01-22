package com.questkeeper.core;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.core.RestSystem.RestResult;
import com.questkeeper.core.RestSystem.RestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the D&D 5e rest system.
 */
@DisplayName("RestSystem")
class RestSystemTest {

    private RestSystem restSystem;
    private Character fighter;
    private Character wizard;
    private Character barbarian;

    @BeforeEach
    void setUp() {
        restSystem = new RestSystem();

        // Fighter: d10 hit die, 14 CON (+2 mod)
        fighter = new Character("Test Fighter", Race.HUMAN, CharacterClass.FIGHTER,
                14, 12, 14, 10, 10, 10);

        // Wizard: d6 hit die, 10 CON (+0 mod)
        wizard = new Character("Test Wizard", Race.ELF, CharacterClass.WIZARD,
                8, 14, 10, 16, 12, 10);

        // Barbarian: d12 hit die, 16 CON (+3 mod)
        barbarian = new Character("Test Barbarian", Race.HALF_ORC, CharacterClass.BARBARIAN,
                16, 14, 16, 8, 10, 8);
    }

    @Nested
    @DisplayName("Character Hit Dice")
    class CharacterHitDiceTests {

        @Test
        @DisplayName("should start with hit dice equal to level")
        void startsWithHitDiceEqualToLevel() {
            assertEquals(1, fighter.getAvailableHitDice());
            assertEquals(1, fighter.getMaxHitDice());
        }

        @Test
        @DisplayName("should have correct hit die size for class")
        void correctHitDieSizeForClass() {
            assertEquals(10, fighter.getHitDieSize());  // Fighter d10
            assertEquals(6, wizard.getHitDieSize());    // Wizard d6
            assertEquals(12, barbarian.getHitDieSize()); // Barbarian d12
        }

        @Test
        @DisplayName("should decrease available hit dice when used")
        void decreasesWhenUsed() {
            fighter.takeDamage(5);  // Damage fighter first
            int before = fighter.getAvailableHitDice();

            fighter.useHitDie();

            assertEquals(before - 1, fighter.getAvailableHitDice());
        }

        @Test
        @DisplayName("should not go below zero hit dice")
        void doesNotGoBelowZero() {
            fighter.takeDamage(5);
            fighter.useHitDie();  // Use the only die

            int result = fighter.useHitDie();  // Try to use another

            assertEquals(-1, result);  // Returns -1 when no dice
            assertEquals(0, fighter.getAvailableHitDice());
        }

        @Test
        @DisplayName("should heal when using hit die")
        void healsWhenUsingHitDie() {
            fighter.takeDamage(10);
            int hpBefore = fighter.getCurrentHitPoints();

            int healed = fighter.useHitDie();

            assertTrue(healed > 0);
            assertEquals(hpBefore + healed, fighter.getCurrentHitPoints());
        }

        @Test
        @DisplayName("should restore hit dice with restoreHitDice")
        void restoresHitDice() {
            // Use all hit dice
            fighter.takeDamage(5);
            fighter.useHitDie();
            assertEquals(0, fighter.getAvailableHitDice());

            // Restore
            int restored = fighter.restoreHitDice();

            assertTrue(restored > 0);
            assertTrue(fighter.getAvailableHitDice() > 0);
        }

        @Test
        @DisplayName("should gain hit dice when leveling up")
        void gainsHitDiceOnLevelUp() {
            fighter.addExperience(300);  // Level up to 2

            assertEquals(2, fighter.getLevel());
            assertEquals(2, fighter.getMaxHitDice());
            assertEquals(2, fighter.getAvailableHitDice());
        }
    }

    @Nested
    @DisplayName("Short Rest")
    class ShortRestTests {

        @Test
        @DisplayName("should heal character when spending hit dice")
        void healsWhenSpendingHitDice() {
            fighter.takeDamage(10);
            int hpBefore = fighter.getCurrentHitPoints();

            RestResult result = restSystem.shortRest(fighter, 1);

            assertEquals(RestType.SHORT, result.type());
            assertTrue(result.hpRestored() > 0);
            assertEquals(1, result.hitDiceUsed());
            assertEquals(hpBefore + result.hpRestored(), fighter.getCurrentHitPoints());
        }

        @Test
        @DisplayName("should not restore hit dice on short rest")
        void doesNotRestoreHitDice() {
            fighter.takeDamage(5);

            RestResult result = restSystem.shortRest(fighter, 1);

            assertEquals(0, result.hitDiceRestored());
        }

        @Test
        @DisplayName("should not use dice if at full health")
        void doesNotUseDiceAtFullHealth() {
            RestResult result = restSystem.shortRest(fighter, 1);

            assertEquals(0, result.hpRestored());
            assertEquals(0, result.hitDiceUsed());
        }

        @Test
        @DisplayName("should not use more dice than available")
        void doesNotUseMoreThanAvailable() {
            fighter.takeDamage(50);  // Heavy damage

            RestResult result = restSystem.shortRest(fighter, 10);  // Request 10 dice

            assertEquals(1, result.hitDiceUsed());  // Only 1 available at level 1
        }

        @Test
        @DisplayName("should stop healing at max HP")
        void stopsAtMaxHp() {
            // Level up to have more hit dice
            fighter.addExperience(900);  // Level 3
            fighter.takeDamage(1);  // Only 1 damage

            RestResult result = restSystem.shortRest(fighter, 3);

            assertEquals(fighter.getMaxHitPoints(), fighter.getCurrentHitPoints());
            // Should have used only what was needed
            assertTrue(result.hitDiceUsed() <= 3);
        }
    }

    @Nested
    @DisplayName("Long Rest")
    class LongRestTests {

        @Test
        @DisplayName("should fully restore HP")
        void fullyRestoresHp() {
            fighter.takeDamage(50);

            RestResult result = restSystem.longRest(fighter);

            assertEquals(RestType.LONG, result.type());
            assertEquals(fighter.getMaxHitPoints(), result.currentHp());
            assertEquals(fighter.getMaxHitPoints(), fighter.getCurrentHitPoints());
        }

        @Test
        @DisplayName("should restore half of max hit dice (minimum 1)")
        void restoresHalfHitDice() {
            // Level up and use all hit dice
            fighter.addExperience(2700);  // Level 4
            fighter.takeDamage(20);
            while (fighter.getAvailableHitDice() > 0) {
                fighter.useHitDie();
            }
            assertEquals(0, fighter.getAvailableHitDice());

            RestResult result = restSystem.longRest(fighter);

            // Should restore half of 4 = 2 dice
            assertEquals(2, result.hitDiceRestored());
            assertEquals(2, fighter.getAvailableHitDice());
        }

        @Test
        @DisplayName("should restore at least 1 hit die")
        void restoresMinimumOneDie() {
            // Level 1, use the only die
            fighter.takeDamage(5);
            fighter.useHitDie();
            assertEquals(0, fighter.getAvailableHitDice());

            RestResult result = restSystem.longRest(fighter);

            // Half of 1 rounded down is 0, but minimum is 1
            assertEquals(1, result.hitDiceRestored());
            assertEquals(1, fighter.getAvailableHitDice());
        }

        @Test
        @DisplayName("should clear temporary HP")
        void clearsTemporaryHp() {
            fighter.setTemporaryHitPoints(10);
            assertEquals(10, fighter.getTemporaryHitPoints());

            restSystem.longRest(fighter);

            assertEquals(0, fighter.getTemporaryHitPoints());
        }

        @Test
        @DisplayName("should not restore dice beyond maximum")
        void doesNotExceedMaxDice() {
            // Already at full dice
            assertEquals(1, fighter.getAvailableHitDice());
            assertEquals(1, fighter.getMaxHitDice());

            RestResult result = restSystem.longRest(fighter);

            assertEquals(0, result.hitDiceRestored());
            assertEquals(1, fighter.getAvailableHitDice());
        }

        @Test
        @DisplayName("should report successful even with no restoration needed")
        void successfulEvenWhenNotNeeded() {
            // Already at full health and dice
            RestResult result = restSystem.longRest(fighter);

            assertTrue(result.wasSuccessful());
        }
    }

    @Nested
    @DisplayName("Rest Benefits Check")
    class RestBenefitsTests {

        @Test
        @DisplayName("should detect when short rest would help")
        void detectsShortRestBenefit() {
            // At full health with dice - no benefit
            assertFalse(restSystem.wouldBenefitFromShortRest(fighter));

            // Damaged with dice - would benefit
            fighter.takeDamage(5);
            assertTrue(restSystem.wouldBenefitFromShortRest(fighter));

            // Damaged but no dice - no benefit
            fighter.useHitDie();
            assertFalse(restSystem.wouldBenefitFromShortRest(fighter));
        }

        @Test
        @DisplayName("should detect when long rest would help")
        void detectsLongRestBenefit() {
            // At full health and full dice - no benefit
            assertFalse(restSystem.wouldBenefitFromLongRest(fighter));

            // Damaged - would benefit
            fighter.takeDamage(5);
            assertTrue(restSystem.wouldBenefitFromLongRest(fighter));

            // Full health but missing dice - would benefit
            fighter.fullHeal();
            fighter.takeDamage(1);
            fighter.useHitDie();
            fighter.fullHeal();
            assertTrue(restSystem.wouldBenefitFromLongRest(fighter));
        }
    }

    @Nested
    @DisplayName("Different Classes")
    class DifferentClassesTests {

        @Test
        @DisplayName("barbarian heals more per hit die due to d12 and high CON")
        void barbarianHealsMore() {
            barbarian.takeDamage(20);
            wizard.takeDamage(20);

            // Run multiple trials to verify barbarian tends to heal more
            int barbarianTotal = 0;
            int wizardTotal = 0;

            // Reset and test multiple times
            for (int i = 0; i < 10; i++) {
                Character testBarb = new Character("Barb", Race.HALF_ORC, CharacterClass.BARBARIAN,
                        16, 14, 16, 8, 10, 8);
                Character testWiz = new Character("Wiz", Race.ELF, CharacterClass.WIZARD,
                        8, 14, 10, 16, 12, 10);

                testBarb.takeDamage(20);
                testWiz.takeDamage(20);

                barbarianTotal += testBarb.useHitDie();
                wizardTotal += testWiz.useHitDie();
            }

            // Barbarian (d12+3) should heal more than Wizard (d6+0) on average
            // Barb average: 6.5 + 3 = 9.5
            // Wiz average: 3.5 + 0 = 3.5
            assertTrue(barbarianTotal > wizardTotal,
                    "Barbarian should heal more on average: " + barbarianTotal + " vs " + wizardTotal);
        }
    }
}
