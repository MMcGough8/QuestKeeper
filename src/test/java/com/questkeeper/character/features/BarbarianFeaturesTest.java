package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BarbarianFeatures")
class BarbarianFeaturesTest {

    @Nested
    @DisplayName("Feature Initialization")
    class FeatureInitializationTests {

        @Test
        @DisplayName("Level 1 Barbarian has Rage")
        void level1BarbarianHasRage() {
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);

            assertTrue(barbarian.getFeature(BarbarianFeatures.RAGE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Barbarian has Unarmored Defense")
        void level1BarbarianHasUnarmoredDefense() {
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);

            assertTrue(barbarian.getFeature(BarbarianFeatures.UNARMORED_DEFENSE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Barbarian does not have Reckless Attack")
        void level1BarbarianDoesNotHaveRecklessAttack() {
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);

            assertTrue(barbarian.getFeature(BarbarianFeatures.RECKLESS_ATTACK_ID).isEmpty());
        }

        @Test
        @DisplayName("Level 2 Barbarian has Reckless Attack")
        void level2BarbarianHasRecklessAttack() {
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            barbarian.setLevel(2);

            assertTrue(barbarian.getFeature(BarbarianFeatures.RECKLESS_ATTACK_ID).isPresent());
        }

        @Test
        @DisplayName("Level 2 Barbarian has Danger Sense")
        void level2BarbarianHasDangerSense() {
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            barbarian.setLevel(2);

            assertTrue(barbarian.getFeature(BarbarianFeatures.DANGER_SENSE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Barbarian has Frenzy")
        void level3BarbarianHasFrenzy() {
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            barbarian.setLevel(3);

            assertTrue(barbarian.getFeature(BarbarianFeatures.FRENZY_ID).isPresent());
        }

        @Test
        @DisplayName("Non-Barbarian class has no Barbarian features")
        void nonBarbarianHasNoBarbarianFeatures() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(BarbarianFeatures.RAGE_ID).isEmpty());
            assertTrue(fighter.getFeature(BarbarianFeatures.UNARMORED_DEFENSE_ID).isEmpty());
        }
    }

    @Nested
    @DisplayName("Rage")
    class RageTests {

        @Test
        @DisplayName("Level 1-2 Barbarian has 2 rages per long rest")
        void level1Has2Rages() {
            assertEquals(2, BarbarianFeatures.getRagesPerLongRest(1));
            assertEquals(2, BarbarianFeatures.getRagesPerLongRest(2));
        }

        @Test
        @DisplayName("Level 3-5 Barbarian has 3 rages per long rest")
        void level3Has3Rages() {
            assertEquals(3, BarbarianFeatures.getRagesPerLongRest(3));
            assertEquals(3, BarbarianFeatures.getRagesPerLongRest(5));
        }

        @Test
        @DisplayName("Level 6-11 Barbarian has 4 rages per long rest")
        void level6Has4Rages() {
            assertEquals(4, BarbarianFeatures.getRagesPerLongRest(6));
            assertEquals(4, BarbarianFeatures.getRagesPerLongRest(11));
        }

        @Test
        @DisplayName("Level 1-8 Barbarian has +2 rage damage")
        void level1Has2RageDamage() {
            assertEquals(2, BarbarianFeatures.getRageDamageBonus(1));
            assertEquals(2, BarbarianFeatures.getRageDamageBonus(8));
        }

        @Test
        @DisplayName("Level 9-15 Barbarian has +3 rage damage")
        void level9Has3RageDamage() {
            assertEquals(3, BarbarianFeatures.getRageDamageBonus(9));
            assertEquals(3, BarbarianFeatures.getRageDamageBonus(15));
        }

        @Test
        @DisplayName("Level 16+ Barbarian has +4 rage damage")
        void level16Has4RageDamage() {
            assertEquals(4, BarbarianFeatures.getRageDamageBonus(16));
            assertEquals(4, BarbarianFeatures.getRageDamageBonus(20));
        }

        @Test
        @DisplayName("Rage can be activated and deactivated")
        void rageCanBeActivatedAndDeactivated() {
            BarbarianFeatures.Rage rage = BarbarianFeatures.createRage(1);

            assertFalse(rage.isActive());

            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            rage.use(barbarian);

            assertTrue(rage.isActive());
            assertEquals(10, rage.getRoundsRemaining());

            rage.endRage();
            assertFalse(rage.isActive());
            assertEquals(0, rage.getRoundsRemaining());
        }

        @Test
        @DisplayName("Rage duration decrements each turn")
        void rageDurationDecrements() {
            BarbarianFeatures.Rage rage = BarbarianFeatures.createRage(1);
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            rage.use(barbarian);

            assertEquals(10, rage.getRoundsRemaining());

            assertTrue(rage.processTurnEnd());
            assertEquals(9, rage.getRoundsRemaining());

            // Simulate several turns
            for (int i = 0; i < 8; i++) {
                assertTrue(rage.processTurnEnd());
            }
            assertEquals(1, rage.getRoundsRemaining());

            // Last turn
            assertFalse(rage.processTurnEnd());  // Returns false when rage ends
            assertEquals(0, rage.getRoundsRemaining());
            assertFalse(rage.isActive());
        }

        @Test
        @DisplayName("Rage damage bonus scales with level")
        void rageDamageBonusScalesWithLevel() {
            BarbarianFeatures.Rage rage1 = BarbarianFeatures.createRage(1);
            assertEquals(2, rage1.getDamageBonus());

            BarbarianFeatures.Rage rage9 = BarbarianFeatures.createRage(9);
            assertEquals(3, rage9.getDamageBonus());

            BarbarianFeatures.Rage rage16 = BarbarianFeatures.createRage(16);
            assertEquals(4, rage16.getDamageBonus());
        }
    }

    @Nested
    @DisplayName("Unarmored Defense")
    class UnarmoredDefenseTests {

        @Test
        @DisplayName("Unarmored Defense calculates AC correctly")
        void unarmoredDefenseCalculatesAC() {
            // DEX 14 (+2), CON 16 (+3) = 10 + 2 + 3 = 15
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN,
                14, 14, 16, 10, 10, 10);

            BarbarianFeatures.UnarmoredDefense unarmoredDefense =
                BarbarianFeatures.createUnarmoredDefense();

            assertEquals(15, unarmoredDefense.calculateAC(barbarian));
        }

        @Test
        @DisplayName("Barbarian uses Unarmored Defense when not wearing armor")
        void barbarianUsesUnarmoredDefenseWhenUnarmored() {
            // DEX 14 (+2), CON 16 (+3) = 10 + 2 + 3 = 15
            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN,
                14, 14, 16, 10, 10, 10);

            assertEquals(15, barbarian.getArmorClass());
        }
    }

    @Nested
    @DisplayName("Reckless Attack")
    class RecklessAttackTests {

        @Test
        @DisplayName("Reckless Attack can be activated once per turn")
        void recklessAttackCanBeActivatedOncePerTurn() {
            BarbarianFeatures.RecklessAttack reckless = BarbarianFeatures.createRecklessAttack();

            assertTrue(reckless.canUse());
            assertFalse(reckless.isActiveThisTurn());

            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            reckless.use(barbarian);

            assertTrue(reckless.isActiveThisTurn());
            assertFalse(reckless.canUse());
        }

        @Test
        @DisplayName("Reckless Attack resets each turn")
        void recklessAttackResetsEachTurn() {
            BarbarianFeatures.RecklessAttack reckless = BarbarianFeatures.createRecklessAttack();

            Character barbarian = new Character("Test", Race.HUMAN, CharacterClass.BARBARIAN);
            reckless.use(barbarian);

            assertTrue(reckless.isActiveThisTurn());

            reckless.resetTurn();

            assertFalse(reckless.isActiveThisTurn());
            assertTrue(reckless.canUse());
        }
    }

    @Nested
    @DisplayName("createFeaturesForLevel")
    class CreateFeaturesForLevelTests {

        @Test
        @DisplayName("Level 1 returns Rage and Unarmored Defense")
        void level1ReturnsBasicFeatures() {
            List<ClassFeature> features = BarbarianFeatures.createFeaturesForLevel(1);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(BarbarianFeatures.RAGE_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(BarbarianFeatures.UNARMORED_DEFENSE_ID)));
            assertEquals(2, features.size());
        }

        @Test
        @DisplayName("Level 2 includes Reckless Attack and Danger Sense")
        void level2IncludesRecklessAndDangerSense() {
            List<ClassFeature> features = BarbarianFeatures.createFeaturesForLevel(2);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(BarbarianFeatures.RECKLESS_ATTACK_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(BarbarianFeatures.DANGER_SENSE_ID)));
            assertEquals(4, features.size());
        }

        @Test
        @DisplayName("Level 3 includes Frenzy")
        void level3IncludesFrenzy() {
            List<ClassFeature> features = BarbarianFeatures.createFeaturesForLevel(3);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(BarbarianFeatures.FRENZY_ID)));
            assertEquals(5, features.size());
        }
    }
}
