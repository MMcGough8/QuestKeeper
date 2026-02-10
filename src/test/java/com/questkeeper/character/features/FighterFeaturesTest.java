package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FighterFeatures")
class FighterFeaturesTest {

    @Nested
    @DisplayName("Feature Initialization")
    class FeatureInitializationTests {

        @Test
        @DisplayName("Level 1 Fighter has Second Wind")
        void level1FighterHasSecondWind() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(FighterFeatures.SECOND_WIND_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Fighter does not have Action Surge")
        void level1FighterDoesNotHaveActionSurge() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(FighterFeatures.ACTION_SURGE_ID).isEmpty());
        }

        @Test
        @DisplayName("Level 2 Fighter has Action Surge")
        void level2FighterHasActionSurge() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);
            fighter.setLevel(2);
            // Re-initialize features by setting level which doesn't call initializeClassFeatures
            // We need to add XP to trigger levelUp
            fighter.setExperiencePoints(0);
            fighter.addExperience(300);  // Level up to 2

            assertTrue(fighter.getFeature(FighterFeatures.ACTION_SURGE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Fighter has Improved Critical")
        void level3FighterHasImprovedCritical() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);
            fighter.addExperience(900);  // Level up to 3

            assertTrue(fighter.getFeature(FighterFeatures.IMPROVED_CRITICAL_ID).isPresent());
            assertTrue(fighter.hasImprovedCritical());
            assertEquals(19, fighter.getCriticalThreshold());
        }

        @Test
        @DisplayName("Non-Fighter class has no features")
        void nonFighterHasNoFeatures() {
            Character wizard = new Character("Test", Race.HUMAN, CharacterClass.WIZARD);

            assertTrue(wizard.getClassFeatures().isEmpty());
        }
    }

    @Nested
    @DisplayName("Second Wind")
    class SecondWindTests {

        private Character fighter;

        @BeforeEach
        void setUp() {
            fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER,
                16, 14, 14, 10, 10, 10);
        }

        @Test
        @DisplayName("Can use Second Wind when available")
        void canUseSecondWind() {
            assertTrue(fighter.canUseFeature(FighterFeatures.SECOND_WIND_ID));
        }

        @Test
        @DisplayName("Second Wind heals the character")
        void secondWindHeals() {
            fighter.takeDamage(10);
            int hpBefore = fighter.getCurrentHitPoints();

            String result = fighter.useFeature(FighterFeatures.SECOND_WIND_ID);

            assertTrue(result.contains("Second Wind"));
            assertTrue(fighter.getCurrentHitPoints() >= hpBefore);
        }

        @Test
        @DisplayName("Cannot use Second Wind twice before rest")
        void cannotUseSecondWindTwice() {
            fighter.takeDamage(5);
            fighter.useFeature(FighterFeatures.SECOND_WIND_ID);

            assertFalse(fighter.canUseFeature(FighterFeatures.SECOND_WIND_ID));
        }

        @Test
        @DisplayName("Short rest restores Second Wind")
        void shortRestRestoresSecondWind() {
            fighter.useFeature(FighterFeatures.SECOND_WIND_ID);
            assertFalse(fighter.canUseFeature(FighterFeatures.SECOND_WIND_ID));

            fighter.resetFeaturesOnShortRest();

            assertTrue(fighter.canUseFeature(FighterFeatures.SECOND_WIND_ID));
        }

        @Test
        @DisplayName("Long rest restores Second Wind")
        void longRestRestoresSecondWind() {
            fighter.useFeature(FighterFeatures.SECOND_WIND_ID);
            assertFalse(fighter.canUseFeature(FighterFeatures.SECOND_WIND_ID));

            fighter.resetFeaturesOnLongRest();

            assertTrue(fighter.canUseFeature(FighterFeatures.SECOND_WIND_ID));
        }
    }

    @Nested
    @DisplayName("Fighting Style")
    class FightingStyleTests {

        @Test
        @DisplayName("Can set Fighting Style on Fighter")
        void canSetFightingStyle() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            fighter.setFightingStyle(FightingStyle.DEFENSE);

            assertEquals(FightingStyle.DEFENSE, fighter.getFightingStyle());
        }

        @Test
        @DisplayName("Fighting Style feature is added to class features")
        void fightingStyleAddedToFeatures() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            fighter.setFightingStyle(FightingStyle.ARCHERY);

            assertTrue(fighter.getFeature(FighterFeatures.FIGHTING_STYLE_ID).isPresent());
            ClassFeature feature = fighter.getFeature(FighterFeatures.FIGHTING_STYLE_ID).get();
            assertTrue(feature.getName().contains("Archery"));
        }

        @Test
        @DisplayName("Defense style adds +1 AC when wearing armor")
        void defenseStyleAddsAC() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER,
                16, 14, 14, 10, 10, 10);

            // Equip armor first
            var armor = com.questkeeper.inventory.Armor.createChainMail();
            fighter.getInventory().addItem(armor);
            fighter.getInventory().equip(armor);
            int armoredAC = fighter.getArmorClass();

            // Set Defense style
            fighter.setFightingStyle(FightingStyle.DEFENSE);

            // AC should be 1 higher with Defense style
            assertEquals(armoredAC + 1, fighter.getArmorClass());
        }

        @Test
        @DisplayName("Cannot set Fighting Style on non-Fighter")
        void cannotSetFightingStyleOnNonFighter() {
            Character wizard = new Character("Test", Race.HUMAN, CharacterClass.WIZARD);

            assertThrows(IllegalStateException.class, () ->
                wizard.setFightingStyle(FightingStyle.DEFENSE));
        }
    }

    @Nested
    @DisplayName("createFeaturesForLevel")
    class CreateFeaturesForLevelTests {

        @Test
        @DisplayName("Level 1 returns Second Wind only")
        void level1ReturnsSecondWind() {
            List<ClassFeature> features = FighterFeatures.createFeaturesForLevel(1, null);

            assertEquals(1, features.size());
            assertEquals(FighterFeatures.SECOND_WIND_ID, features.get(0).getId());
        }

        @Test
        @DisplayName("Level 1 with Fighting Style returns two features")
        void level1WithFightingStyleReturnsTwoFeatures() {
            List<ClassFeature> features = FighterFeatures.createFeaturesForLevel(1, FightingStyle.DEFENSE);

            assertEquals(2, features.size());
        }

        @Test
        @DisplayName("Level 2 includes Action Surge")
        void level2IncludesActionSurge() {
            List<ClassFeature> features = FighterFeatures.createFeaturesForLevel(2, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(FighterFeatures.ACTION_SURGE_ID)));
        }

        @Test
        @DisplayName("Level 3 includes Improved Critical")
        void level3IncludesImprovedCritical() {
            List<ClassFeature> features = FighterFeatures.createFeaturesForLevel(3, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(FighterFeatures.IMPROVED_CRITICAL_ID)));
        }
    }
}
