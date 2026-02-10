package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RogueFeatures")
class RogueFeaturesTest {

    @Nested
    @DisplayName("Feature Initialization")
    class FeatureInitializationTests {

        @Test
        @DisplayName("Level 1 Rogue has Sneak Attack")
        void level1RogueHasSneakAttack() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);

            assertTrue(rogue.getFeature(RogueFeatures.SNEAK_ATTACK_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Rogue has Thieves' Cant")
        void level1RogueHasThievesCant() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);

            assertTrue(rogue.getFeature(RogueFeatures.THIEVES_CANT_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Rogue does not have Cunning Action")
        void level1RogueDoesNotHaveCunningAction() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);

            assertTrue(rogue.getFeature(RogueFeatures.CUNNING_ACTION_ID).isEmpty());
        }

        @Test
        @DisplayName("Level 2 Rogue has Cunning Action")
        void level2RogueHasCunningAction() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);
            rogue.setLevel(2);

            assertTrue(rogue.getFeature(RogueFeatures.CUNNING_ACTION_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Rogue has Fast Hands")
        void level3RogueHasFastHands() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);
            rogue.setLevel(3);

            assertTrue(rogue.getFeature(RogueFeatures.FAST_HANDS_ID).isPresent());
        }

        @Test
        @DisplayName("Non-Rogue class has no Rogue features")
        void nonRogueHasNoRogueFeatures() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(RogueFeatures.SNEAK_ATTACK_ID).isEmpty());
            assertTrue(fighter.getFeature(RogueFeatures.CUNNING_ACTION_ID).isEmpty());
        }
    }

    @Nested
    @DisplayName("Sneak Attack")
    class SneakAttackTests {

        @Test
        @DisplayName("Level 1 Rogue has 1d6 Sneak Attack")
        void level1SneakAttack() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);

            var feature = rogue.getFeature(RogueFeatures.SNEAK_ATTACK_ID);
            assertTrue(feature.isPresent());

            RogueFeatures.SneakAttack sneakAttack = (RogueFeatures.SneakAttack) feature.get();
            assertEquals(1, sneakAttack.getSneakAttackDice());
            assertEquals("1d6", sneakAttack.getSneakAttackDamage());
        }

        @Test
        @DisplayName("Level 3 Rogue has 2d6 Sneak Attack")
        void level3SneakAttack() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);
            rogue.setLevel(3);

            var feature = rogue.getFeature(RogueFeatures.SNEAK_ATTACK_ID);
            assertTrue(feature.isPresent());

            RogueFeatures.SneakAttack sneakAttack = (RogueFeatures.SneakAttack) feature.get();
            assertEquals(2, sneakAttack.getSneakAttackDice());
            assertEquals("2d6", sneakAttack.getSneakAttackDamage());
        }

        @Test
        @DisplayName("Level 5 Rogue has 3d6 Sneak Attack")
        void level5SneakAttack() {
            Character rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE);
            rogue.setLevel(5);

            var feature = rogue.getFeature(RogueFeatures.SNEAK_ATTACK_ID);
            assertTrue(feature.isPresent());

            RogueFeatures.SneakAttack sneakAttack = (RogueFeatures.SneakAttack) feature.get();
            assertEquals(3, sneakAttack.getSneakAttackDice());
        }
    }

    @Nested
    @DisplayName("Expertise")
    class ExpertiseTests {

        private Character rogue;

        @BeforeEach
        void setUp() {
            rogue = new Character("Test", Race.HUMAN, CharacterClass.ROGUE,
                10, 16, 12, 10, 14, 10);
            // Add some skill proficiencies
            rogue.addSkillProficiency(Skill.STEALTH);
            rogue.addSkillProficiency(Skill.PERCEPTION);
            rogue.addSkillProficiency(Skill.SLEIGHT_OF_HAND);
        }

        @Test
        @DisplayName("Can set expertise skills")
        void canSetExpertiseSkills() {
            Set<Skill> expertiseSkills = EnumSet.of(Skill.STEALTH, Skill.PERCEPTION);
            rogue.setExpertiseSkills(expertiseSkills);

            assertTrue(rogue.hasExpertise(Skill.STEALTH));
            assertTrue(rogue.hasExpertise(Skill.PERCEPTION));
            assertFalse(rogue.hasExpertise(Skill.SLEIGHT_OF_HAND));
        }

        @Test
        @DisplayName("Expertise doubles proficiency bonus")
        void expertiseDoublesBonus() {
            // Level 1 proficiency bonus is +2
            int normalMod = rogue.getSkillModifier(Skill.STEALTH);  // DEX (+3) + prof (+2) = +5

            rogue.setExpertiseSkills(EnumSet.of(Skill.STEALTH));

            int expertiseMod = rogue.getSkillModifier(Skill.STEALTH);  // DEX (+3) + prof*2 (+4) = +7

            assertEquals(normalMod + 2, expertiseMod);  // Expertise adds another +2 at level 1
        }

        @Test
        @DisplayName("Cannot set expertise on non-proficient skill")
        void cannotSetExpertiseOnNonProficient() {
            assertThrows(IllegalArgumentException.class, () ->
                rogue.setExpertiseSkills(EnumSet.of(Skill.ARCANA)));
        }

        @Test
        @DisplayName("Cannot set expertise on non-Rogue")
        void cannotSetExpertiseOnNonRogue() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);
            fighter.addSkillProficiency(Skill.ATHLETICS);

            assertThrows(IllegalStateException.class, () ->
                fighter.setExpertiseSkills(EnumSet.of(Skill.ATHLETICS)));
        }

        @Test
        @DisplayName("Expertise feature is added to class features")
        void expertiseFeatureAdded() {
            rogue.setExpertiseSkills(EnumSet.of(Skill.STEALTH));

            assertTrue(rogue.getFeature(RogueFeatures.EXPERTISE_ID).isPresent());
        }
    }

    @Nested
    @DisplayName("createFeaturesForLevel")
    class CreateFeaturesForLevelTests {

        @Test
        @DisplayName("Level 1 returns Sneak Attack and Thieves' Cant")
        void level1ReturnsBasicFeatures() {
            List<ClassFeature> features = RogueFeatures.createFeaturesForLevel(1, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RogueFeatures.SNEAK_ATTACK_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RogueFeatures.THIEVES_CANT_ID)));
        }

        @Test
        @DisplayName("Level 2 includes Cunning Action")
        void level2IncludesCunningAction() {
            List<ClassFeature> features = RogueFeatures.createFeaturesForLevel(2, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RogueFeatures.CUNNING_ACTION_ID)));
        }

        @Test
        @DisplayName("Level 3 includes Thief features")
        void level3IncludesThiefFeatures() {
            List<ClassFeature> features = RogueFeatures.createFeaturesForLevel(3, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RogueFeatures.FAST_HANDS_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RogueFeatures.SECOND_STORY_WORK_ID)));
        }
    }
}
