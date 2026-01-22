package com.questkeeper.campaign;

import com.questkeeper.campaign.MiniGame.EvaluationResult;
import com.questkeeper.campaign.MiniGame.Type;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the MiniGame class.
 */
@DisplayName("MiniGame")
class MiniGameTest {

    private MiniGame miniGame;
    private Character rogue;  // High DEX for Stealth
    private Character wizard; // High INT for Investigation

    @BeforeEach
    void setUp() {
        miniGame = new MiniGame("test_game", "Test Challenge", Type.SKILL_CHECK);

        // Rogue with high DEX (18) and Stealth proficiency
        rogue = new Character("Test Rogue", Race.HUMAN, CharacterClass.ROGUE,
                10, 18, 12, 10, 10, 10);
        rogue.addSkillProficiency(Skill.STEALTH);

        // Wizard with high INT (18) and Investigation proficiency
        wizard = new Character("Test Wizard", Race.HUMAN, CharacterClass.WIZARD,
                8, 12, 10, 18, 14, 10);
        wizard.addSkillProficiency(Skill.INVESTIGATION);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("creates mini-game with valid parameters")
        void createsWithValidParameters() {
            MiniGame game = new MiniGame("puzzle_01", "Ancient Puzzle", Type.DECODE);

            assertEquals("puzzle_01", game.getId());
            assertEquals("Ancient Puzzle", game.getName());
            assertEquals(Type.DECODE, game.getType());
            assertFalse(game.isCompleted());
        }

        @Test
        @DisplayName("trims whitespace from ID and name")
        void trimsWhitespace() {
            MiniGame game = new MiniGame("  puzzle_01  ", "  Ancient Puzzle  ", Type.DECODE);

            assertEquals("puzzle_01", game.getId());
            assertEquals("Ancient Puzzle", game.getName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("throws exception for null or empty ID")
        void throwsForNullOrEmptyId(String id) {
            assertThrows(IllegalArgumentException.class, () ->
                    new MiniGame(id, "Valid Name", Type.SKILL_CHECK));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("throws exception for null or empty name")
        void throwsForNullOrEmptyName(String name) {
            assertThrows(IllegalArgumentException.class, () ->
                    new MiniGame("valid_id", name, Type.SKILL_CHECK));
        }

        @Test
        @DisplayName("defaults to SKILL_CHECK type when null")
        void defaultsToSkillCheckType() {
            MiniGame game = new MiniGame("test", "Test", null);

            assertEquals(Type.SKILL_CHECK, game.getType());
        }

        @Test
        @DisplayName("sets default values correctly")
        void setsDefaultValues() {
            MiniGame game = new MiniGame("test", "Test", Type.SEARCH);

            assertEquals("", game.getDescription());
            assertEquals("", game.getHint());
            assertNull(game.getRequiredSkill());
            assertNull(game.getAlternateSkill());
            assertEquals(10, game.getDc());  // Default DC is 10 (Easy)
            assertEquals("", game.getReward());
            assertEquals("", game.getFailConsequence());
            assertEquals("Challenge completed!", game.getCompletionText());
            assertEquals("You failed the challenge.", game.getFailureText());
        }

        @Test
        @DisplayName("constructor with description sets description")
        void constructorWithDescription() {
            MiniGame game = new MiniGame("test", "Test", Type.EXAMINE, "Look carefully");

            assertEquals("Look carefully", game.getDescription());
        }
    }

    @Nested
    @DisplayName("Type Enum")
    class TypeEnumTests {

        @ParameterizedTest
        @EnumSource(Type.class)
        @DisplayName("all types have display names")
        void allTypesHaveDisplayNames(Type type) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }

        @ParameterizedTest
        @EnumSource(Type.class)
        @DisplayName("all types have descriptions")
        void allTypesHaveDescriptions(Type type) {
            assertNotNull(type.getDescription());
            assertFalse(type.getDescription().isEmpty());
        }

        @Test
        @DisplayName("contains expected types")
        void containsExpectedTypes() {
            assertEquals(10, Type.values().length);
            assertNotNull(Type.SEARCH);
            assertNotNull(Type.EXAMINE);
            assertNotNull(Type.DECODE);
            assertNotNull(Type.ALIGNMENT);
            assertNotNull(Type.TIMING);
            assertNotNull(Type.DIALOGUE);
            assertNotNull(Type.MECHANISM);
            assertNotNull(Type.CHOICE);
            assertNotNull(Type.COMBAT);
            assertNotNull(Type.SKILL_CHECK);
        }
    }

    @Nested
    @DisplayName("Setters")
    class SetterTests {

        @Test
        @DisplayName("setName updates name with valid value")
        void setNameUpdatesName() {
            miniGame.setName("New Name");
            assertEquals("New Name", miniGame.getName());
        }

        @Test
        @DisplayName("setName ignores null")
        void setNameIgnoresNull() {
            miniGame.setName(null);
            assertEquals("Test Challenge", miniGame.getName());
        }

        @Test
        @DisplayName("setName ignores empty string")
        void setNameIgnoresEmpty() {
            miniGame.setName("   ");
            assertEquals("Test Challenge", miniGame.getName());
        }

        @Test
        @DisplayName("setDescription handles null as empty string")
        void setDescriptionHandlesNull() {
            miniGame.setDescription(null);
            assertEquals("", miniGame.getDescription());
        }

        @Test
        @DisplayName("setHint handles null as empty string")
        void setHintHandlesNull() {
            miniGame.setHint(null);
            assertEquals("", miniGame.getHint());
        }

        @Test
        @DisplayName("setType defaults to SKILL_CHECK for null")
        void setTypeDefaultsForNull() {
            miniGame.setType(Type.COMBAT);
            miniGame.setType(null);
            assertEquals(Type.SKILL_CHECK, miniGame.getType());
        }

        @Test
        @DisplayName("setDc enforces minimum of 1")
        void setDcEnforcesMinimum() {
            miniGame.setDc(0);
            assertEquals(1, miniGame.getDc());

            miniGame.setDc(-5);
            assertEquals(1, miniGame.getDc());
        }

        @Test
        @DisplayName("setDc accepts valid values")
        void setDcAcceptsValidValues() {
            miniGame.setDc(15);
            assertEquals(15, miniGame.getDc());

            miniGame.setDc(25);
            assertEquals(25, miniGame.getDc());
        }

        @Test
        @DisplayName("setReward handles null as empty string")
        void setRewardHandlesNull() {
            miniGame.setReward(null);
            assertEquals("", miniGame.getReward());
        }

        @Test
        @DisplayName("setFailConsequence handles null as empty string")
        void setFailConsequenceHandlesNull() {
            miniGame.setFailConsequence(null);
            assertEquals("", miniGame.getFailConsequence());
        }

        @Test
        @DisplayName("setCompletionText provides default for null")
        void setCompletionTextDefault() {
            miniGame.setCompletionText("Custom success!");
            assertEquals("Custom success!", miniGame.getCompletionText());

            miniGame.setCompletionText(null);
            assertEquals("Challenge completed!", miniGame.getCompletionText());
        }

        @Test
        @DisplayName("setFailureText provides default for null")
        void setFailureTextDefault() {
            miniGame.setFailureText("Custom failure!");
            assertEquals("Custom failure!", miniGame.getFailureText());

            miniGame.setFailureText(null);
            assertEquals("You failed the challenge.", miniGame.getFailureText());
        }

        @Test
        @DisplayName("setRequiredSkill and setAlternateSkill work")
        void setSkills() {
            miniGame.setRequiredSkill(Skill.STEALTH);
            miniGame.setAlternateSkill(Skill.ACROBATICS);

            assertEquals(Skill.STEALTH, miniGame.getRequiredSkill());
            assertEquals(Skill.ACROBATICS, miniGame.getAlternateSkill());
        }
    }

    @Nested
    @DisplayName("Game Actions")
    class GameActionTests {

        @Test
        @DisplayName("complete() marks game as completed")
        void completeMarksCompleted() {
            assertFalse(miniGame.isCompleted());

            String result = miniGame.complete();

            assertTrue(miniGame.isCompleted());
            assertEquals(miniGame.getCompletionText(), result);
        }

        @Test
        @DisplayName("fail() keeps game not completed")
        void failKeepsNotCompleted() {
            miniGame.complete();
            assertTrue(miniGame.isCompleted());

            String result = miniGame.fail();

            assertFalse(miniGame.isCompleted());
            assertEquals(miniGame.getFailureText(), result);
        }

        @Test
        @DisplayName("reset() clears completed state")
        void resetClearsCompleted() {
            miniGame.complete();
            assertTrue(miniGame.isCompleted());

            miniGame.reset();

            assertFalse(miniGame.isCompleted());
        }
    }

    @Nested
    @DisplayName("Skill Validation")
    class SkillValidationTests {

        @BeforeEach
        void setUpSkills() {
            miniGame.setRequiredSkill(Skill.STEALTH);
            miniGame.setAlternateSkill(Skill.ACROBATICS);
        }

        @Test
        @DisplayName("isValidApproach returns true for required skill")
        void validForRequiredSkill() {
            assertTrue(miniGame.isValidApproach(Skill.STEALTH));
        }

        @Test
        @DisplayName("isValidApproach returns true for alternate skill")
        void validForAlternateSkill() {
            assertTrue(miniGame.isValidApproach(Skill.ACROBATICS));
        }

        @Test
        @DisplayName("isValidApproach returns false for other skills")
        void invalidForOtherSkills() {
            assertFalse(miniGame.isValidApproach(Skill.ATHLETICS));
            assertFalse(miniGame.isValidApproach(Skill.PERCEPTION));
        }

        @Test
        @DisplayName("isValidApproach returns false when no required skill")
        void invalidWhenNoRequiredSkill() {
            MiniGame noSkillGame = new MiniGame("test", "Test", Type.CHOICE);
            assertFalse(noSkillGame.isValidApproach(Skill.STEALTH));
        }

        @Test
        @DisplayName("hasAlternateApproach returns true when set")
        void hasAlternateWhenSet() {
            assertTrue(miniGame.hasAlternateApproach());
        }

        @Test
        @DisplayName("hasAlternateApproach returns false when not set")
        void noAlternateWhenNotSet() {
            MiniGame game = new MiniGame("test", "Test", Type.SKILL_CHECK);
            game.setRequiredSkill(Skill.STEALTH);
            // No alternate set

            assertFalse(game.hasAlternateApproach());
        }
    }

    @Nested
    @DisplayName("Evaluate")
    class EvaluateTests {

        @BeforeEach
        void setUpForEvaluation() {
            miniGame.setRequiredSkill(Skill.STEALTH);
            miniGame.setAlternateSkill(Skill.ACROBATICS);
            miniGame.setDc(10);
            miniGame.setReward("Gold Coin");
            miniGame.setFailConsequence("You triggered an alarm");
            miniGame.setCompletionText("You sneak past successfully!");
            miniGame.setFailureText("You were spotted!");
        }

        @Test
        @DisplayName("throws exception for null character")
        void throwsForNullCharacter() {
            assertThrows(IllegalArgumentException.class, () ->
                    miniGame.evaluate(null, "stealth"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("throws exception for null or empty approach")
        void throwsForNullOrEmptyApproach(String approach) {
            assertThrows(IllegalArgumentException.class, () ->
                    miniGame.evaluate(rogue, approach));
        }

        @Test
        @DisplayName("throws exception when no required skill set")
        void throwsWhenNoRequiredSkill() {
            MiniGame noSkillGame = new MiniGame("test", "Test", Type.SKILL_CHECK);

            assertThrows(IllegalStateException.class, () ->
                    noSkillGame.evaluate(rogue, "stealth"));
        }

        @Test
        @DisplayName("throws exception for invalid skill")
        void throwsForInvalidSkill() {
            assertThrows(IllegalArgumentException.class, () ->
                    miniGame.evaluate(rogue, "not_a_skill"));
        }

        @Test
        @DisplayName("throws exception for skill not allowed for this challenge")
        void throwsForDisallowedSkill() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    miniGame.evaluate(rogue, "athletics"));

            assertTrue(ex.getMessage().contains("not valid for this challenge"));
        }

        @Test
        @DisplayName("accepts required skill (case insensitive)")
        void acceptsRequiredSkillCaseInsensitive() {
            assertDoesNotThrow(() -> miniGame.evaluate(rogue, "STEALTH"));
            miniGame.reset();
            assertDoesNotThrow(() -> miniGame.evaluate(rogue, "stealth"));
            miniGame.reset();
            assertDoesNotThrow(() -> miniGame.evaluate(rogue, "Stealth"));
        }

        @Test
        @DisplayName("accepts alternate skill")
        void acceptsAlternateSkill() {
            assertDoesNotThrow(() -> miniGame.evaluate(rogue, "acrobatics"));
        }

        @RepeatedTest(20)
        @DisplayName("successful check marks game completed")
        void successMarksCompleted() {
            // DC 10, Rogue has +6 Stealth (DEX +4 + Prof +2)
            // Needs to roll 4+ on d20 (85% chance)
            miniGame.setDc(5);  // Make DC very low to ensure success

            EvaluationResult result = miniGame.evaluate(rogue, "stealth");

            if (result.success()) {
                assertTrue(miniGame.isCompleted());
            }
        }

        @Test
        @DisplayName("failed check does not mark completed")
        void failureDoesNotMarkCompleted() {
            miniGame.setDc(30);  // Impossible DC

            EvaluationResult result = miniGame.evaluate(rogue, "stealth");

            assertFalse(result.success());
            assertFalse(miniGame.isCompleted());
        }

        @Test
        @DisplayName("result contains roll description")
        void resultContainsRollDescription() {
            EvaluationResult result = miniGame.evaluate(rogue, "stealth");

            assertNotNull(result.rollDescription());
            assertTrue(result.rollDescription().contains("d20"));
            assertTrue(result.rollDescription().contains("Stealth"));
            assertTrue(result.rollDescription().contains("DC 10"));
        }

        @Test
        @DisplayName("successful result has reward and completion text")
        void successfulResultHasReward() {
            miniGame.setDc(1);  // Guaranteed success

            EvaluationResult result = miniGame.evaluate(rogue, "stealth");

            assertTrue(result.success());
            assertEquals("You sneak past successfully!", result.message());
            assertEquals("Gold Coin", result.reward());
            assertNull(result.consequence());
        }

        @Test
        @DisplayName("failed result has consequence and failure text")
        void failedResultHasConsequence() {
            miniGame.setDc(30);  // Guaranteed failure

            EvaluationResult result = miniGame.evaluate(rogue, "stealth");

            assertFalse(result.success());
            assertEquals("You were spotted!", result.message());
            assertNull(result.reward());
            assertEquals("You triggered an alarm", result.consequence());
        }

        @Test
        @DisplayName("natural roll is between 1 and 20")
        void naturalRollInRange() {
            for (int i = 0; i < 100; i++) {
                miniGame.reset();
                EvaluationResult result = miniGame.evaluate(rogue, "stealth");
                assertTrue(result.naturalRoll() >= 1 && result.naturalRoll() <= 20,
                        "Natural roll should be 1-20, was: " + result.naturalRoll());
            }
        }

        @Test
        @DisplayName("total roll includes modifier")
        void totalRollIncludesModifier() {
            // Rogue has +6 Stealth modifier
            int modifier = rogue.getSkillModifier(Skill.STEALTH);

            for (int i = 0; i < 20; i++) {
                miniGame.reset();
                EvaluationResult result = miniGame.evaluate(rogue, "stealth");
                assertEquals(result.naturalRoll() + modifier, result.totalRoll());
            }
        }

        @RepeatedTest(100)
        @DisplayName("detects natural 20")
        void detectsNatural20() {
            EvaluationResult result = miniGame.evaluate(rogue, "stealth");
            miniGame.reset();

            if (result.naturalRoll() == 20) {
                assertTrue(result.wasNatural20());
                assertFalse(result.wasNatural1());
            }
        }

        @RepeatedTest(100)
        @DisplayName("detects natural 1")
        void detectsNatural1() {
            EvaluationResult result = miniGame.evaluate(rogue, "stealth");
            miniGame.reset();

            if (result.naturalRoll() == 1) {
                assertTrue(result.wasNatural1());
                assertFalse(result.wasNatural20());
            }
        }
    }

    @Nested
    @DisplayName("EvaluationResult")
    class EvaluationResultTests {

        @Test
        @DisplayName("hasReward returns true when reward present")
        void hasRewardWhenPresent() {
            EvaluationResult result = new EvaluationResult(
                    true, "Success!", "Gold", null, "roll", 15, 20, false, false);
            assertTrue(result.hasReward());
        }

        @Test
        @DisplayName("hasReward returns false when reward null")
        void hasRewardFalseWhenNull() {
            EvaluationResult result = new EvaluationResult(
                    true, "Success!", null, null, "roll", 15, 20, false, false);
            assertFalse(result.hasReward());
        }

        @Test
        @DisplayName("hasReward returns false when reward empty")
        void hasRewardFalseWhenEmpty() {
            EvaluationResult result = new EvaluationResult(
                    true, "Success!", "", null, "roll", 15, 20, false, false);
            assertFalse(result.hasReward());
        }

        @Test
        @DisplayName("hasConsequence returns true when consequence present")
        void hasConsequenceWhenPresent() {
            EvaluationResult result = new EvaluationResult(
                    false, "Failure!", null, "Alarm triggered", "roll", 5, 10, false, false);
            assertTrue(result.hasConsequence());
        }

        @Test
        @DisplayName("hasConsequence returns false when consequence null")
        void hasConsequenceFalseWhenNull() {
            EvaluationResult result = new EvaluationResult(
                    false, "Failure!", null, null, "roll", 5, 10, false, false);
            assertFalse(result.hasConsequence());
        }

        @Test
        @DisplayName("getFormattedResult contains all information")
        void formattedResultContainsAllInfo() {
            EvaluationResult result = new EvaluationResult(
                    true, "You did it!", "Magic Sword", null,
                    "d20(18) + Stealth(+6) = 24 vs DC 15", 18, 24, false, false);

            String formatted = result.getFormattedResult();

            assertTrue(formatted.contains("SKILL CHECK"));
            assertTrue(formatted.contains("d20(18)"));
            assertTrue(formatted.contains("SUCCESS"));
            assertTrue(formatted.contains("You did it!"));
            assertTrue(formatted.contains("Magic Sword"));
        }

        @Test
        @DisplayName("getFormattedResult shows natural 20")
        void formattedResultShowsNat20() {
            EvaluationResult result = new EvaluationResult(
                    true, "Critical!", "Bonus", null, "roll", 20, 26, true, false);

            String formatted = result.getFormattedResult();

            assertTrue(formatted.contains("NATURAL 20"));
        }

        @Test
        @DisplayName("getFormattedResult shows natural 1")
        void formattedResultShowsNat1() {
            EvaluationResult result = new EvaluationResult(
                    false, "Oops!", null, "Bad stuff", "roll", 1, 7, false, true);

            String formatted = result.getFormattedResult();

            assertTrue(formatted.contains("NATURAL 1"));
        }
    }

    @Nested
    @DisplayName("Display and Utility")
    class DisplayAndUtilityTests {

        @Test
        @DisplayName("getDisplayText shows type, name, and status")
        void displayTextShowsInfo() {
            miniGame.setDescription("Test description");

            String display = miniGame.getDisplayText();

            assertTrue(display.contains("Skill Check"));
            assertTrue(display.contains("Test Challenge"));
            assertTrue(display.contains("Test description"));
            assertTrue(display.contains("Incomplete"));
        }

        @Test
        @DisplayName("getDisplayText shows completed status")
        void displayTextShowsCompleted() {
            miniGame.complete();

            String display = miniGame.getDisplayText();

            assertTrue(display.contains("COMPLETE"));
        }

        @Test
        @DisplayName("equals based on ID only")
        void equalsBasedOnId() {
            MiniGame game1 = new MiniGame("same_id", "Name 1", Type.SEARCH);
            MiniGame game2 = new MiniGame("same_id", "Name 2", Type.COMBAT);

            assertEquals(game1, game2);
        }

        @Test
        @DisplayName("not equals for different IDs")
        void notEqualsForDifferentIds() {
            MiniGame game1 = new MiniGame("id_1", "Same Name", Type.SEARCH);
            MiniGame game2 = new MiniGame("id_2", "Same Name", Type.SEARCH);

            assertNotEquals(game1, game2);
        }

        @Test
        @DisplayName("hashCode consistent with equals")
        void hashCodeConsistentWithEquals() {
            MiniGame game1 = new MiniGame("same_id", "Name 1", Type.SEARCH);
            MiniGame game2 = new MiniGame("same_id", "Name 2", Type.COMBAT);

            assertEquals(game1.hashCode(), game2.hashCode());
        }

        @Test
        @DisplayName("toString contains key information")
        void toStringContainsInfo() {
            String str = miniGame.toString();

            assertTrue(str.contains("test_game"));
            assertTrue(str.contains("Test Challenge"));
            assertTrue(str.contains("SKILL_CHECK"));
            assertTrue(str.contains("completed=false"));
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("rogue excels at stealth checks")
        void rogueExcelsAtStealth() {
            miniGame.setRequiredSkill(Skill.STEALTH);
            miniGame.setDc(12);

            // Rogue has +6 Stealth, needs 6+ on d20 (75% success)
            int successes = 0;
            for (int i = 0; i < 100; i++) {
                miniGame.reset();
                EvaluationResult result = miniGame.evaluate(rogue, "stealth");
                if (result.success()) successes++;
            }

            // Should succeed roughly 75% of the time (allow variance)
            assertTrue(successes > 50, "Rogue should succeed often with +6 modifier, got: " + successes);
        }

        @Test
        @DisplayName("wizard struggles with stealth checks")
        void wizardStrugglesWithStealth() {
            miniGame.setRequiredSkill(Skill.STEALTH);
            miniGame.setDc(12);

            // Wizard has +1 Stealth (no proficiency), needs 11+ on d20 (50% success)
            int successes = 0;
            for (int i = 0; i < 100; i++) {
                miniGame.reset();
                EvaluationResult result = miniGame.evaluate(wizard, "stealth");
                if (result.success()) successes++;
            }

            // Should succeed roughly 50% of the time
            assertTrue(successes < 75, "Wizard should struggle with +1 modifier, got: " + successes);
        }

        @Test
        @DisplayName("wizard excels at investigation checks")
        void wizardExcelsAtInvestigation() {
            miniGame.setRequiredSkill(Skill.INVESTIGATION);
            miniGame.setDc(12);

            // Wizard has +6 Investigation (INT +4 + Prof +2), needs 6+ on d20
            int successes = 0;
            for (int i = 0; i < 100; i++) {
                miniGame.reset();
                EvaluationResult result = miniGame.evaluate(wizard, "investigation");
                if (result.success()) successes++;
            }

            assertTrue(successes > 50, "Wizard should succeed often with +6 modifier, got: " + successes);
        }

        @Test
        @DisplayName("full mini-game workflow")
        void fullWorkflow() {
            // Create a mini-game
            MiniGame puzzle = new MiniGame("ancient_lock", "Ancient Lock", Type.MECHANISM);
            puzzle.setDescription("A complex mechanical lock guards the treasure.");
            puzzle.setHint("Perhaps someone skilled with tools could help...");
            puzzle.setRequiredSkill(Skill.SLEIGHT_OF_HAND);
            puzzle.setAlternateSkill(Skill.INVESTIGATION);
            puzzle.setDc(15);
            puzzle.setReward("ancient_key");
            puzzle.setFailConsequence("The lock jams and cannot be tried again for an hour.");
            puzzle.setCompletionText("The lock clicks open, revealing its secrets!");
            puzzle.setFailureText("Your attempt fails and the lock mechanism seizes up.");

            // Check initial state
            assertFalse(puzzle.isCompleted());
            assertEquals("Ancient Lock", puzzle.getName());
            assertTrue(puzzle.hasAlternateApproach());
            assertTrue(puzzle.isValidApproach(Skill.SLEIGHT_OF_HAND));
            assertTrue(puzzle.isValidApproach(Skill.INVESTIGATION));
            assertFalse(puzzle.isValidApproach(Skill.ATHLETICS));

            // Attempt the puzzle (with low DC to likely succeed)
            puzzle.setDc(5);
            EvaluationResult result = puzzle.evaluate(rogue, "sleight_of_hand");

            // Verify result structure
            assertNotNull(result.message());
            assertNotNull(result.rollDescription());
            assertTrue(result.naturalRoll() >= 1 && result.naturalRoll() <= 20);

            if (result.success()) {
                assertTrue(puzzle.isCompleted());
                assertEquals("ancient_key", result.reward());
                assertNull(result.consequence());
            } else {
                assertFalse(puzzle.isCompleted());
                assertNull(result.reward());
                assertEquals("The lock jams and cannot be tried again for an hour.", result.consequence());
            }

            // Reset and try again
            puzzle.reset();
            assertFalse(puzzle.isCompleted());
        }
    }
}
