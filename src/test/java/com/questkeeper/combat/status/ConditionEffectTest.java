package com.questkeeper.combat.status;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.combat.Monster;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ConditionEffect class.
 */
@DisplayName("ConditionEffect Tests")
class ConditionEffectTest {

    private Character testCharacter;
    private Monster testMonster;

    @BeforeEach
    void setUp() {
        testCharacter = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
        testCharacter.setAbilityScore(Ability.STRENGTH, 14);
        testCharacter.setAbilityScore(Ability.DEXTERITY, 12);
        testCharacter.setAbilityScore(Ability.CONSTITUTION, 13);

        testMonster = new Monster("test_goblin", "Test Goblin", 13, 7);
        testMonster.setAbilityModifiers(1, 2, 0, -1, 0, -1);
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("poisoned creates correct duration-based effect")
        void poisonedCreatesDurationEffect() {
            ConditionEffect effect = ConditionEffect.poisoned(3);

            assertEquals(Condition.POISONED, effect.getCondition());
            assertEquals("Poisoned", effect.getName());
            assertEquals(DurationType.ROUNDS, effect.getDurationType());
            assertEquals(3, effect.getRemainingDuration());
            assertFalse(effect.allowsSavingThrow());
        }

        @Test
        @DisplayName("poisonedWithSave creates save-based effect")
        void poisonedWithSaveCreatesSaveEffect() {
            ConditionEffect effect = ConditionEffect.poisonedWithSave(Ability.CONSTITUTION, 15);

            assertEquals(Condition.POISONED, effect.getCondition());
            assertEquals(DurationType.UNTIL_SAVE, effect.getDurationType());
            assertTrue(effect.allowsSavingThrow());
            assertEquals(Ability.CONSTITUTION, effect.getSavingThrowAbility());
            assertEquals(15, effect.getSaveDC());
        }

        @Test
        @DisplayName("restrained creates correct effect")
        void restrainedCreatesCorrectEffect() {
            ConditionEffect effect = ConditionEffect.restrained(2);

            assertEquals(Condition.RESTRAINED, effect.getCondition());
            assertTrue(effect.grantsAdvantageAgainst());
            assertTrue(effect.causesDisadvantageOnAttacks());
            assertTrue(effect.preventsMovement());
        }

        @Test
        @DisplayName("paralyzed creates effect with auto-crit")
        void paralyzedCreatesAutoCritEffect() {
            ConditionEffect effect = ConditionEffect.paralyzed(1);

            assertEquals(Condition.PARALYZED, effect.getCondition());
            assertTrue(effect.meleeCritsOnHit());
            assertTrue(effect.autoFailsStrDexSaves());
            assertTrue(effect.preventsActions());
        }

        @Test
        @DisplayName("prone creates a 1-round effect (auto-recovers next turn)")
        void proneCreates1RoundEffect() {
            ConditionEffect effect = ConditionEffect.prone();

            assertEquals(Condition.PRONE, effect.getCondition());
            assertEquals(DurationType.ROUNDS, effect.getDurationType());
            assertEquals(1, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("grappled creates indefinite effect")
        void grappledCreatesIndefiniteEffect() {
            ConditionEffect effect = ConditionEffect.grappled();

            assertEquals(Condition.GRAPPLED, effect.getCondition());
            assertEquals(DurationType.INDEFINITE, effect.getDurationType());
            assertTrue(effect.preventsMovement());
        }

        @Test
        @DisplayName("invisible grants advantage on attacks")
        void invisibleGrantsAdvantageOnAttacks() {
            ConditionEffect effect = ConditionEffect.invisible(3);

            assertEquals(Condition.INVISIBLE, effect.getCondition());
            assertTrue(effect.grantsAdvantageOnAttacks());
            assertFalse(effect.causesDisadvantageOnAttacks());
        }

        @Test
        @DisplayName("stunned prevents actions and auto-fails saves")
        void stunnedPreventsActionsAndAutoFails() {
            ConditionEffect effect = ConditionEffect.stunned(1);

            assertEquals(Condition.STUNNED, effect.getCondition());
            assertTrue(effect.preventsActions());
            assertTrue(effect.autoFailsStrDexSaves());
            assertFalse(effect.meleeCritsOnHit()); // Stunned doesn't auto-crit
        }

        @Test
        @DisplayName("unconscious has all severe effects")
        void unconsciousHasSevereEffects() {
            ConditionEffect effect = ConditionEffect.unconscious();

            assertEquals(Condition.UNCONSCIOUS, effect.getCondition());
            assertTrue(effect.preventsActions());
            assertTrue(effect.preventsMovement());
            assertTrue(effect.autoFailsStrDexSaves());
            assertTrue(effect.meleeCritsOnHit());
            assertTrue(effect.grantsAdvantageAgainst());
        }
    }

    @Nested
    @DisplayName("Duration Tests")
    class DurationTests {

        @Test
        @DisplayName("decrementDuration reduces remaining duration")
        void decrementDurationReducesDuration() {
            ConditionEffect effect = ConditionEffect.poisoned(3);

            assertEquals(3, effect.getRemainingDuration());

            effect.decrementDuration();
            assertEquals(2, effect.getRemainingDuration());

            effect.decrementDuration();
            assertEquals(1, effect.getRemainingDuration());

            effect.decrementDuration();
            assertEquals(0, effect.getRemainingDuration());
        }

        @Test
        @DisplayName("isExpired returns true when duration reaches 0")
        void isExpiredWhenDurationReachesZero() {
            ConditionEffect effect = ConditionEffect.poisoned(1);

            assertFalse(effect.isExpired());

            effect.decrementDuration();
            assertTrue(effect.isExpired());
        }

        @Test
        @DisplayName("expire marks effect as expired immediately")
        void expireMarksEffectExpired() {
            ConditionEffect effect = ConditionEffect.poisoned(5);

            assertFalse(effect.isExpired());

            effect.expire();
            assertTrue(effect.isExpired());
        }

        @Test
        @DisplayName("indefinite effects don't expire from decrement")
        void indefiniteEffectsDontExpireFromDecrement() {
            // Use grappled instead of prone since prone is now a 1-round
            // auto-recover effect (grappled is the canonical INDEFINITE).
            ConditionEffect effect = ConditionEffect.grappled();

            assertFalse(effect.isExpired());

            effect.decrementDuration();
            assertFalse(effect.isExpired());
        }
    }

    @Nested
    @DisplayName("Source Tracking Tests")
    class SourceTrackingTests {

        @Test
        @DisplayName("source is null by default")
        void sourceIsNullByDefault() {
            ConditionEffect effect = ConditionEffect.poisoned(3);
            assertNull(effect.getSource());
        }

        @Test
        @DisplayName("setSource tracks effect source")
        void setSourceTracksSource() {
            ConditionEffect effect = ConditionEffect.frightened(3);
            effect.setSource(testMonster);

            assertEquals(testMonster, effect.getSource());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString includes condition name and duration")
        void toStringIncludesNameAndDuration() {
            ConditionEffect effect = ConditionEffect.poisoned(3);
            String result = effect.toString();

            assertTrue(result.contains("Poisoned"));
            assertTrue(result.contains("3 rounds"));
        }

        @Test
        @DisplayName("toString includes save DC for save-based effects")
        void toStringIncludesSaveDC() {
            ConditionEffect effect = ConditionEffect.restrainedWithSave(Ability.STRENGTH, 14);
            String result = effect.toString();

            assertTrue(result.contains("Restrained"));
            assertTrue(result.contains("DC 14"));
            assertTrue(result.contains("STR"));
        }
    }
}
