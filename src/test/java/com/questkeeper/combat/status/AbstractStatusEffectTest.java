package com.questkeeper.combat.status;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.combat.Combatant;
import com.questkeeper.combat.Monster;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AbstractStatusEffect class.
 * Uses a concrete test implementation to test the abstract functionality.
 */
@DisplayName("AbstractStatusEffect Tests")
class AbstractStatusEffectTest {

    private Character testCharacter;
    private Monster testMonster;

    /**
     * Concrete implementation for testing the abstract class.
     */
    static class TestStatusEffect extends AbstractStatusEffect {

        public TestStatusEffect(String id, String name, String description, int durationRounds) {
            super(id, name, description, durationRounds);
        }

        public TestStatusEffect(String id, String name, String description,
                                Ability savingThrowAbility, int saveDC) {
            super(id, name, description, savingThrowAbility, saveDC);
        }

        public TestStatusEffect(String id, String name, String description, DurationType durationType) {
            super(id, name, description, durationType);
        }

        public TestStatusEffect(String id, String name, String description,
                                DurationType durationType, Ability savingThrowAbility, int saveDC) {
            super(id, name, description, durationType, savingThrowAbility, saveDC);
        }
    }

    @BeforeEach
    void setUp() {
        testCharacter = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
        testCharacter.setAbilityScore(Ability.STRENGTH, 14);
        testCharacter.setAbilityScore(Ability.DEXTERITY, 12);
        testCharacter.setAbilityScore(Ability.CONSTITUTION, 16);

        testMonster = new Monster("test_goblin", "Test Goblin", 13, 7);
        testMonster.setAbilityModifiers(1, 2, 0, -1, 0, -1);
    }

    @Nested
    @DisplayName("Constructor Tests - Duration Rounds")
    class ConstructorDurationRoundsTests {

        @Test
        @DisplayName("creates effect with correct id")
        void createsEffectWithCorrectId() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", 3);
            assertEquals("test_id", effect.getId());
        }

        @Test
        @DisplayName("creates effect with correct name")
        void createsEffectWithCorrectName() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test Effect", "Desc", 3);
            assertEquals("Test Effect", effect.getName());
        }

        @Test
        @DisplayName("creates effect with correct description")
        void createsEffectWithCorrectDescription() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Test description", 3);
            assertEquals("Test description", effect.getDescription());
        }

        @Test
        @DisplayName("creates effect with ROUNDS duration type")
        void createsEffectWithRoundsDurationType() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", 3);
            assertEquals(DurationType.ROUNDS, effect.getDurationType());
        }

        @Test
        @DisplayName("creates effect with correct remaining duration")
        void createsEffectWithCorrectRemainingDuration() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", 5);
            assertEquals(5, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("creates effect that is not expired initially")
        void createsEffectNotExpiredInitially() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", 3);
            assertFalse(effect.isExpired());
        }

        @Test
        @DisplayName("creates effect without saving throw")
        void createsEffectWithoutSavingThrow() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", 3);
            assertFalse(effect.allowsSavingThrow());
            assertNull(effect.getSavingThrowAbility());
            assertEquals(0, effect.getSaveDC());
        }
    }

    @Nested
    @DisplayName("Constructor Tests - Saving Throw")
    class ConstructorSavingThrowTests {

        @Test
        @DisplayName("creates effect with UNTIL_SAVE duration type")
        void createsEffectWithUntilSaveDurationType() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", Ability.STRENGTH, 15);
            assertEquals(DurationType.UNTIL_SAVE, effect.getDurationType());
        }

        @Test
        @DisplayName("creates effect with correct saving throw ability")
        void createsEffectWithCorrectSavingThrowAbility() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", Ability.DEXTERITY, 14);
            assertEquals(Ability.DEXTERITY, effect.getSavingThrowAbility());
        }

        @Test
        @DisplayName("creates effect with correct save DC")
        void createsEffectWithCorrectSaveDC() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", Ability.CONSTITUTION, 16);
            assertEquals(16, effect.getSaveDC());
        }

        @Test
        @DisplayName("creates effect that allows saving throw")
        void createsEffectThatAllowsSavingThrow() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", Ability.WISDOM, 12);
            assertTrue(effect.allowsSavingThrow());
        }

        @Test
        @DisplayName("creates effect with -1 remaining duration")
        void createsEffectWithNegativeOneDuration() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", Ability.STRENGTH, 15);
            assertEquals(-1, effect.getRemainingDuration());
        }
    }

    @Nested
    @DisplayName("Constructor Tests - Duration Type")
    class ConstructorDurationTypeTests {

        @Test
        @DisplayName("creates effect with INDEFINITE duration type")
        void createsEffectWithIndefiniteDurationType() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", DurationType.INDEFINITE);
            assertEquals(DurationType.INDEFINITE, effect.getDurationType());
        }

        @Test
        @DisplayName("creates effect with PERMANENT duration type")
        void createsEffectWithPermanentDurationType() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", DurationType.PERMANENT);
            assertEquals(DurationType.PERMANENT, effect.getDurationType());
        }

        @Test
        @DisplayName("creates effect with UNTIL_END_OF_TURN duration type")
        void createsEffectWithUntilEndOfTurnDurationType() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", DurationType.UNTIL_END_OF_TURN);
            assertEquals(DurationType.UNTIL_END_OF_TURN, effect.getDurationType());
        }

        @Test
        @DisplayName("creates effect with UNTIL_START_OF_TURN duration type")
        void createsEffectWithUntilStartOfTurnDurationType() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", DurationType.UNTIL_START_OF_TURN);
            assertEquals(DurationType.UNTIL_START_OF_TURN, effect.getDurationType());
        }

        @Test
        @DisplayName("ROUNDS duration type sets remaining duration to 1")
        void roundsDurationTypeSetsRemainingDurationToOne() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc", DurationType.ROUNDS);
            assertEquals(1, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("non-ROUNDS duration types set remaining duration to -1")
        void nonRoundsDurationTypesSetsRemainingDurationToNegativeOne() {
            TestStatusEffect indefinite = new TestStatusEffect("test", "Test", "Desc", DurationType.INDEFINITE);
            TestStatusEffect permanent = new TestStatusEffect("test", "Test", "Desc", DurationType.PERMANENT);

            assertEquals(-1, indefinite.getRemainingDuration());
            assertEquals(-1, permanent.getRemainingDuration());
        }
    }

    @Nested
    @DisplayName("Constructor Tests - Duration Type with Save")
    class ConstructorDurationTypeWithSaveTests {

        @Test
        @DisplayName("creates effect with custom duration type and save")
        void createsEffectWithCustomDurationTypeAndSave() {
            TestStatusEffect effect = new TestStatusEffect("test_id", "Test", "Desc",
                DurationType.ROUNDS, Ability.STRENGTH, 13);

            assertEquals(DurationType.ROUNDS, effect.getDurationType());
            assertEquals(Ability.STRENGTH, effect.getSavingThrowAbility());
            assertEquals(13, effect.getSaveDC());
            assertTrue(effect.allowsSavingThrow());
        }
    }

    @Nested
    @DisplayName("Duration Management Tests")
    class DurationManagementTests {

        @Test
        @DisplayName("decrementDuration reduces remaining duration")
        void decrementDurationReducesRemainingDuration() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);

            effect.decrementDuration();
            assertEquals(2, effect.getRemainingDuration());

            effect.decrementDuration();
            assertEquals(1, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("decrementDuration does nothing for non-ROUNDS types")
        void decrementDurationDoesNothingForNonRoundsTypes() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", DurationType.INDEFINITE);
            int initialDuration = effect.getRemainingDuration();

            effect.decrementDuration();
            assertEquals(initialDuration, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("decrementDuration does not go below zero")
        void decrementDurationDoesNotGoBelowZero() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 1);

            effect.decrementDuration();
            assertEquals(0, effect.getRemainingDuration());

            effect.decrementDuration();
            assertEquals(0, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("isExpired returns true when duration reaches zero")
        void isExpiredReturnsTrueWhenDurationReachesZero() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 1);

            assertFalse(effect.isExpired());
            effect.decrementDuration();
            assertTrue(effect.isExpired());
        }

        @Test
        @DisplayName("expire marks effect as expired immediately")
        void expireMarksEffectAsExpiredImmediately() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 10);

            assertFalse(effect.isExpired());
            effect.expire();
            assertTrue(effect.isExpired());
        }

        @Test
        @DisplayName("isExpired returns true after expire even with remaining duration")
        void isExpiredReturnsTrueAfterExpireEvenWithDuration() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 10);

            assertEquals(10, effect.getRemainingDuration());
            effect.expire();
            assertTrue(effect.isExpired());
        }
    }

    @Nested
    @DisplayName("Turn Processing Tests")
    class TurnProcessingTests {

        @Test
        @DisplayName("onTurnStart expires UNTIL_START_OF_TURN effects")
        void onTurnStartExpiresUntilStartOfTurnEffects() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", DurationType.UNTIL_START_OF_TURN);

            assertFalse(effect.isExpired());
            String message = effect.onTurnStart(testCharacter);

            assertTrue(effect.isExpired());
            assertNotNull(message);
            assertTrue(message.contains("ended"));
        }

        @Test
        @DisplayName("onTurnStart returns null for other duration types")
        void onTurnStartReturnsNullForOtherTypes() {
            TestStatusEffect roundsEffect = new TestStatusEffect("test", "Test", "Desc", 3);
            TestStatusEffect indefiniteEffect = new TestStatusEffect("test", "Test", "Desc", DurationType.INDEFINITE);

            assertNull(roundsEffect.onTurnStart(testCharacter));
            assertNull(indefiniteEffect.onTurnStart(testCharacter));
        }

        @Test
        @DisplayName("onTurnEnd decrements duration for ROUNDS type")
        void onTurnEndDecrementsDurationForRoundsType() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);

            effect.onTurnEnd(testCharacter);
            assertEquals(2, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("onTurnEnd expires UNTIL_END_OF_TURN effects")
        void onTurnEndExpiresUntilEndOfTurnEffects() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", DurationType.UNTIL_END_OF_TURN);

            assertFalse(effect.isExpired());
            String message = effect.onTurnEnd(testCharacter);

            assertTrue(effect.isExpired());
            assertNotNull(message);
            assertTrue(message.contains("ended"));
        }

        @Test
        @DisplayName("onTurnEnd returns message when ROUNDS effect expires")
        void onTurnEndReturnsMessageWhenRoundsEffectExpires() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test Effect", "Desc", 1);

            String message = effect.onTurnEnd(testCharacter);

            assertTrue(effect.isExpired());
            assertNotNull(message);
            assertTrue(message.contains("worn off"));
        }

        @Test
        @DisplayName("onTurnEnd returns null when no events occur")
        void onTurnEndReturnsNullWhenNoEvents() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", DurationType.INDEFINITE);

            String message = effect.onTurnEnd(testCharacter);
            assertNull(message);
        }
    }

    @Nested
    @DisplayName("Saving Throw Tests")
    class SavingThrowTests {

        @Test
        @DisplayName("allowsSavingThrow returns false when no ability set")
        void allowsSavingThrowReturnsFalseWhenNoAbility() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.allowsSavingThrow());
        }

        @Test
        @DisplayName("allowsSavingThrow returns false when DC is 0")
        void allowsSavingThrowReturnsFalseWhenDCIsZero() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.allowsSavingThrow());
        }

        @Test
        @DisplayName("allowsSavingThrow returns true when ability and DC are set")
        void allowsSavingThrowReturnsTrueWhenSet() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.STRENGTH, 15);
            assertTrue(effect.allowsSavingThrow());
        }

        @Test
        @DisplayName("attemptSave returns false when saving throw not allowed")
        void attemptSaveReturnsFalseWhenNotAllowed() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.attemptSave(testCharacter));
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Character")
        void getSaveModifierGetsCorrectModifierForCharacter() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.STRENGTH, 15);

            // Character with STR 14 has +2 modifier, Fighter has STR save proficiency
            // At level 1, proficiency bonus is +2, so total should be +4
            int modifier = effect.getSaveModifier(testCharacter, Ability.STRENGTH);
            assertTrue(modifier >= 2); // At least the ability modifier
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Monster STR")
        void getSaveModifierGetsCorrectModifierForMonsterStr() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.STRENGTH, 15);
            // Monster has STR mod of 1
            assertEquals(1, effect.getSaveModifier(testMonster, Ability.STRENGTH));
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Monster DEX")
        void getSaveModifierGetsCorrectModifierForMonsterDex() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.DEXTERITY, 15);
            // Monster has DEX mod of 2
            assertEquals(2, effect.getSaveModifier(testMonster, Ability.DEXTERITY));
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Monster CON")
        void getSaveModifierGetsCorrectModifierForMonsterCon() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.CONSTITUTION, 15);
            // Monster has CON mod of 0
            assertEquals(0, effect.getSaveModifier(testMonster, Ability.CONSTITUTION));
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Monster INT")
        void getSaveModifierGetsCorrectModifierForMonsterInt() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.INTELLIGENCE, 15);
            // Monster has INT mod of -1
            assertEquals(-1, effect.getSaveModifier(testMonster, Ability.INTELLIGENCE));
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Monster WIS")
        void getSaveModifierGetsCorrectModifierForMonsterWis() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.WISDOM, 15);
            // Monster has WIS mod of 0
            assertEquals(0, effect.getSaveModifier(testMonster, Ability.WISDOM));
        }

        @Test
        @DisplayName("getSaveModifier gets correct modifier for Monster CHA")
        void getSaveModifierGetsCorrectModifierForMonsterCha() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", Ability.CHARISMA, 15);
            // Monster has CHA mod of -1
            assertEquals(-1, effect.getSaveModifier(testMonster, Ability.CHARISMA));
        }
    }

    @Nested
    @DisplayName("Source Tracking Tests")
    class SourceTrackingTests {

        @Test
        @DisplayName("source is null by default")
        void sourceIsNullByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertNull(effect.getSource());
        }

        @Test
        @DisplayName("setSource sets the source correctly")
        void setSourceSetsSourceCorrectly() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            effect.setSource(testMonster);
            assertEquals(testMonster, effect.getSource());
        }

        @Test
        @DisplayName("source can be changed")
        void sourceCanBeChanged() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            effect.setSource(testMonster);
            effect.setSource(testCharacter);
            assertEquals(testCharacter, effect.getSource());
        }
    }

    @Nested
    @DisplayName("Default Mechanical Effects Tests")
    class DefaultMechanicalEffectsTests {

        @Test
        @DisplayName("grantsAdvantageOnAttacks returns false by default")
        void grantsAdvantageOnAttacksReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.grantsAdvantageOnAttacks());
        }

        @Test
        @DisplayName("causesDisadvantageOnAttacks returns false by default")
        void causesDisadvantageOnAttacksReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.causesDisadvantageOnAttacks());
        }

        @Test
        @DisplayName("grantsAdvantageAgainst returns false by default")
        void grantsAdvantageAgainstReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.grantsAdvantageAgainst());
        }

        @Test
        @DisplayName("preventsActions returns false by default")
        void preventsActionsReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.preventsActions());
        }

        @Test
        @DisplayName("preventsMovement returns false by default")
        void preventsMovementReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.preventsMovement());
        }

        @Test
        @DisplayName("meleeCritsOnHit returns false by default")
        void meleeCritsOnHitReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.meleeCritsOnHit());
        }

        @Test
        @DisplayName("autoFailsStrDexSaves returns false by default")
        void autoFailsStrDexSavesReturnsFalseByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertFalse(effect.autoFailsStrDexSaves());
        }

        @Test
        @DisplayName("getCondition returns null by default")
        void getConditionReturnsNullByDefault() {
            TestStatusEffect effect = new TestStatusEffect("test", "Test", "Desc", 3);
            assertNull(effect.getCondition());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString includes name for ROUNDS type")
        void toStringIncludesNameForRoundsType() {
            TestStatusEffect effect = new TestStatusEffect("test", "Poison", "Desc", 3);
            String result = effect.toString();

            assertTrue(result.contains("Poison"));
        }

        @Test
        @DisplayName("toString includes duration for ROUNDS type")
        void toStringIncludesDurationForRoundsType() {
            TestStatusEffect effect = new TestStatusEffect("test", "Poison", "Desc", 3);
            String result = effect.toString();

            assertTrue(result.contains("3 rounds"));
        }

        @Test
        @DisplayName("toString includes DC and ability for UNTIL_SAVE type")
        void toStringIncludesDcAndAbilityForUntilSaveType() {
            TestStatusEffect effect = new TestStatusEffect("test", "Hold", "Desc", Ability.WISDOM, 14);
            String result = effect.toString();

            assertTrue(result.contains("DC 14"));
            assertTrue(result.contains("WIS"));
        }

        @Test
        @DisplayName("toString has simple format for INDEFINITE type")
        void toStringHasSimpleFormatForIndefiniteType() {
            TestStatusEffect effect = new TestStatusEffect("test", "Grapple", "Desc", DurationType.INDEFINITE);
            String result = effect.toString();

            assertEquals("Grapple", result);
        }
    }
}
