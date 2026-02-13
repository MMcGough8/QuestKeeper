package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RangerFeatures")
class RangerFeaturesTest {

    @Nested
    @DisplayName("Feature Initialization")
    class FeatureInitializationTests {

        @Test
        @DisplayName("Level 2 Ranger has Spellcasting")
        void level2RangerHasSpellcasting() {
            Character ranger = new Character("Test", Race.HUMAN, CharacterClass.RANGER);
            ranger.setLevel(2);

            assertTrue(ranger.getFeature(RangerFeatures.RANGER_SPELLCASTING_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Ranger does not have Spellcasting")
        void level1RangerDoesNotHaveSpellcasting() {
            Character ranger = new Character("Test", Race.HUMAN, CharacterClass.RANGER);

            assertTrue(ranger.getFeature(RangerFeatures.RANGER_SPELLCASTING_ID).isEmpty());
        }

        @Test
        @DisplayName("Level 3 Ranger has Primeval Awareness")
        void level3RangerHasPrimevalAwareness() {
            Character ranger = new Character("Test", Race.HUMAN, CharacterClass.RANGER);
            ranger.setLevel(3);

            assertTrue(ranger.getFeature(RangerFeatures.PRIMEVAL_AWARENESS_ID).isPresent());
        }

        @Test
        @DisplayName("Non-Ranger class has no Ranger features")
        void nonRangerHasNoRangerFeatures() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(RangerFeatures.FAVORED_ENEMY_ID).isEmpty());
            assertTrue(fighter.getFeature(RangerFeatures.RANGER_SPELLCASTING_ID).isEmpty());
        }
    }

    @Nested
    @DisplayName("Favored Enemy")
    class FavoredEnemyTests {

        @Test
        @DisplayName("Favored Enemy stores enemy type")
        void favoredEnemyStoresEnemyType() {
            RangerFeatures.FavoredEnemy fe = RangerFeatures.createFavoredEnemy(
                RangerFeatures.FavoredEnemyType.UNDEAD);

            assertEquals(RangerFeatures.FavoredEnemyType.UNDEAD, fe.getEnemyType());
            assertTrue(fe.getName().contains("Undead"));
        }

        @Test
        @DisplayName("Favored Enemy can check creature type")
        void favoredEnemyCanCheckCreatureType() {
            RangerFeatures.FavoredEnemy fe = RangerFeatures.createFavoredEnemy(
                RangerFeatures.FavoredEnemyType.UNDEAD);

            assertTrue(fe.isFavoredEnemy("Undead"));
            assertTrue(fe.isFavoredEnemy("UNDEAD"));
            assertFalse(fe.isFavoredEnemy("Dragon"));
        }

        @Test
        @DisplayName("All favored enemy types have display names")
        void allFavoredEnemyTypesHaveDisplayNames() {
            for (RangerFeatures.FavoredEnemyType type : RangerFeatures.FavoredEnemyType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Natural Explorer")
    class NaturalExplorerTests {

        @Test
        @DisplayName("Natural Explorer stores terrain type")
        void naturalExplorerStoresTerrainType() {
            RangerFeatures.NaturalExplorer ne = RangerFeatures.createNaturalExplorer(
                RangerFeatures.FavoredTerrainType.FOREST);

            assertEquals(RangerFeatures.FavoredTerrainType.FOREST, ne.getTerrainType());
            assertTrue(ne.getName().contains("Forest"));
        }

        @Test
        @DisplayName("All terrain types have display names")
        void allTerrainTypesHaveDisplayNames() {
            for (RangerFeatures.FavoredTerrainType type : RangerFeatures.FavoredTerrainType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Ranger Spellcasting")
    class RangerSpellcastingTests {

        @Test
        @DisplayName("Level 2 Ranger has 2 first-level spell slots")
        void level2Has2FirstLevelSlots() {
            int[] slots = RangerFeatures.getSpellSlots(2);
            assertEquals(2, slots[0]);
            assertEquals(0, slots[1]);
        }

        @Test
        @DisplayName("Level 3 Ranger has 3 first-level spell slots")
        void level3Has3FirstLevelSlots() {
            int[] slots = RangerFeatures.getSpellSlots(3);
            assertEquals(3, slots[0]);
        }

        @Test
        @DisplayName("Level 5 Ranger has 4 first-level and 2 second-level slots")
        void level5Has4And2Slots() {
            int[] slots = RangerFeatures.getSpellSlots(5);
            assertEquals(4, slots[0]);
            assertEquals(2, slots[1]);
        }

        @Test
        @DisplayName("Ranger can expend spell slots")
        void rangerCanExpendSpellSlots() {
            RangerFeatures.RangerSpellcasting rs = RangerFeatures.createRangerSpellcasting(2);

            assertTrue(rs.hasSlot(1));
            assertTrue(rs.expendSlot(1));
            assertTrue(rs.hasSlot(1));  // Still has 1 left
            assertTrue(rs.expendSlot(1));
            assertFalse(rs.hasSlot(1));  // No more
        }

        @Test
        @DisplayName("Ranger spell slots restore on long rest")
        void rangerSpellSlotsRestoreOnLongRest() {
            RangerFeatures.RangerSpellcasting rs = RangerFeatures.createRangerSpellcasting(2);
            rs.expendSlot(1);
            rs.expendSlot(1);
            assertFalse(rs.hasSlot(1));

            rs.restoreAllSlots();
            assertTrue(rs.hasSlot(1));
            assertEquals(2, rs.getCurrentSlots()[0]);
        }

        @Test
        @DisplayName("Spells known scales with level")
        void spellsKnownScalesWithLevel() {
            assertEquals(0, RangerFeatures.getSpellsKnown(1));
            assertEquals(2, RangerFeatures.getSpellsKnown(2));
            assertEquals(3, RangerFeatures.getSpellsKnown(3));
            assertEquals(3, RangerFeatures.getSpellsKnown(4));
            assertEquals(4, RangerFeatures.getSpellsKnown(5));
        }
    }

    @Nested
    @DisplayName("Colossus Slayer")
    class ColossusSlayerTests {

        @Test
        @DisplayName("Colossus Slayer deals 1d8 extra damage")
        void colossusSlayerDeals1d8() {
            RangerFeatures.ColossusSlayer cs = RangerFeatures.createColossusSlayer();
            assertEquals("1d8", cs.getExtraDamage());
        }

        @Test
        @DisplayName("Colossus Slayer can only be used once per turn")
        void colossusSlayerOncePerTurn() {
            RangerFeatures.ColossusSlayer cs = RangerFeatures.createColossusSlayer();

            assertTrue(cs.canUse());
            cs.use();
            assertFalse(cs.canUse());

            cs.resetTurn();
            assertTrue(cs.canUse());
        }
    }

    @Nested
    @DisplayName("Giant Killer")
    class GiantKillerTests {

        @Test
        @DisplayName("Giant Killer reaction can only be used once per round")
        void giantKillerOncePerRound() {
            RangerFeatures.GiantKiller gk = RangerFeatures.createGiantKiller();

            assertTrue(gk.canUseReaction());
            gk.useReaction();
            assertFalse(gk.canUseReaction());

            gk.resetRound();
            assertTrue(gk.canUseReaction());
        }
    }

    @Nested
    @DisplayName("Horde Breaker")
    class HordeBreakerTests {

        @Test
        @DisplayName("Horde Breaker can only be used once per turn")
        void hordeBreakerOncePerTurn() {
            RangerFeatures.HordeBreaker hb = RangerFeatures.createHordeBreaker();

            assertTrue(hb.canUse());
            hb.use();
            assertFalse(hb.canUse());

            hb.resetTurn();
            assertTrue(hb.canUse());
        }
    }

    @Nested
    @DisplayName("Hunter's Prey Choices")
    class HuntersPreyTests {

        @Test
        @DisplayName("All Hunter's Prey options have display names")
        void allHuntersPreyOptionsHaveDisplayNames() {
            for (RangerFeatures.HuntersPrey prey : RangerFeatures.HuntersPrey.values()) {
                assertNotNull(prey.getDisplayName());
                assertFalse(prey.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("createHuntersPrey creates correct feature for each choice")
        void createHuntersPreyCreatesCorrectFeature() {
            ClassFeature colossus = RangerFeatures.createHuntersPrey(RangerFeatures.HuntersPrey.COLOSSUS_SLAYER);
            assertTrue(colossus instanceof RangerFeatures.ColossusSlayer);

            ClassFeature giant = RangerFeatures.createHuntersPrey(RangerFeatures.HuntersPrey.GIANT_KILLER);
            assertTrue(giant instanceof RangerFeatures.GiantKiller);

            ClassFeature horde = RangerFeatures.createHuntersPrey(RangerFeatures.HuntersPrey.HORDE_BREAKER);
            assertTrue(horde instanceof RangerFeatures.HordeBreaker);
        }
    }

    @Nested
    @DisplayName("createFeaturesForLevel")
    class CreateFeaturesForLevelTests {

        @Test
        @DisplayName("Level 1 with choices returns Favored Enemy and Natural Explorer")
        void level1WithChoicesReturnsBasicFeatures() {
            List<ClassFeature> features = RangerFeatures.createFeaturesForLevel(1,
                RangerFeatures.FavoredEnemyType.UNDEAD,
                RangerFeatures.FavoredTerrainType.FOREST,
                null, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RangerFeatures.FAVORED_ENEMY_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RangerFeatures.NATURAL_EXPLORER_ID)));
            assertEquals(2, features.size());
        }

        @Test
        @DisplayName("Level 1 without choices returns empty list")
        void level1WithoutChoicesReturnsEmpty() {
            List<ClassFeature> features = RangerFeatures.createFeaturesForLevel(1,
                null, null, null, null);

            assertEquals(0, features.size());
        }

        @Test
        @DisplayName("Level 2 includes Spellcasting")
        void level2IncludesSpellcasting() {
            List<ClassFeature> features = RangerFeatures.createFeaturesForLevel(2,
                null, null, null, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RangerFeatures.RANGER_SPELLCASTING_ID)));
        }

        @Test
        @DisplayName("Level 2 with fighting style includes Fighting Style")
        void level2WithFightingStyleIncludesIt() {
            List<ClassFeature> features = RangerFeatures.createFeaturesForLevel(2,
                null, null, FightingStyle.ARCHERY, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RangerFeatures.RANGER_FIGHTING_STYLE_ID)));
        }

        @Test
        @DisplayName("Level 3 includes Primeval Awareness")
        void level3IncludesPrimevalAwareness() {
            List<ClassFeature> features = RangerFeatures.createFeaturesForLevel(3,
                null, null, null, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RangerFeatures.PRIMEVAL_AWARENESS_ID)));
        }

        @Test
        @DisplayName("Level 3 with Hunter choice includes Hunter's Prey feature")
        void level3WithHunterChoiceIncludesHuntersPrey() {
            List<ClassFeature> features = RangerFeatures.createFeaturesForLevel(3,
                null, null, null, RangerFeatures.HuntersPrey.COLOSSUS_SLAYER);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(RangerFeatures.COLOSSUS_SLAYER_ID)));
        }
    }
}
