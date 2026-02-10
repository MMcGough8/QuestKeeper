package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonkFeatures")
class MonkFeaturesTest {

    @Nested
    @DisplayName("Feature Initialization")
    class FeatureInitializationTests {

        @Test
        @DisplayName("Level 1 Monk has Unarmored Defense")
        void level1MonkHasUnarmoredDefense() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);

            assertTrue(monk.getFeature(MonkFeatures.UNARMORED_DEFENSE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Monk has Martial Arts")
        void level1MonkHasMartialArts() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);

            assertTrue(monk.getFeature(MonkFeatures.MARTIAL_ARTS_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Monk does not have Ki")
        void level1MonkDoesNotHaveKi() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);

            assertTrue(monk.getFeature(MonkFeatures.KI_ID).isEmpty());
        }

        @Test
        @DisplayName("Level 2 Monk has Ki")
        void level2MonkHasKi() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);
            monk.setLevel(2);

            assertTrue(monk.getFeature(MonkFeatures.KI_ID).isPresent());
        }

        @Test
        @DisplayName("Level 2 Monk has Flurry of Blows")
        void level2MonkHasFlurryOfBlows() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);
            monk.setLevel(2);

            assertTrue(monk.getFeature(MonkFeatures.FLURRY_OF_BLOWS_ID).isPresent());
        }

        @Test
        @DisplayName("Level 2 Monk has Patient Defense")
        void level2MonkHasPatientDefense() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);
            monk.setLevel(2);

            assertTrue(monk.getFeature(MonkFeatures.PATIENT_DEFENSE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 2 Monk has Step of the Wind")
        void level2MonkHasStepOfTheWind() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);
            monk.setLevel(2);

            assertTrue(monk.getFeature(MonkFeatures.STEP_OF_THE_WIND_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Monk has Deflect Missiles")
        void level3MonkHasDeflectMissiles() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);
            monk.setLevel(3);

            assertTrue(monk.getFeature(MonkFeatures.DEFLECT_MISSILES_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Monk has Open Hand Technique")
        void level3MonkHasOpenHandTechnique() {
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK);
            monk.setLevel(3);

            assertTrue(monk.getFeature(MonkFeatures.OPEN_HAND_TECHNIQUE_ID).isPresent());
        }

        @Test
        @DisplayName("Non-Monk class has no Monk features")
        void nonMonkHasNoMonkFeatures() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(MonkFeatures.UNARMORED_DEFENSE_ID).isEmpty());
            assertTrue(fighter.getFeature(MonkFeatures.MARTIAL_ARTS_ID).isEmpty());
            assertTrue(fighter.getFeature(MonkFeatures.KI_ID).isEmpty());
        }
    }

    @Nested
    @DisplayName("Martial Arts")
    class MartialArtsTests {

        @Test
        @DisplayName("Level 1-4 Monk has d4 Martial Arts die")
        void level1To4HasD4() {
            assertEquals(4, MonkFeatures.getMartialArtsDie(1));
            assertEquals(4, MonkFeatures.getMartialArtsDie(4));
        }

        @Test
        @DisplayName("Level 5-10 Monk has d6 Martial Arts die")
        void level5To10HasD6() {
            assertEquals(6, MonkFeatures.getMartialArtsDie(5));
            assertEquals(6, MonkFeatures.getMartialArtsDie(10));
        }

        @Test
        @DisplayName("Level 11-16 Monk has d8 Martial Arts die")
        void level11To16HasD8() {
            assertEquals(8, MonkFeatures.getMartialArtsDie(11));
            assertEquals(8, MonkFeatures.getMartialArtsDie(16));
        }

        @Test
        @DisplayName("Level 17+ Monk has d10 Martial Arts die")
        void level17PlusHasD10() {
            assertEquals(10, MonkFeatures.getMartialArtsDie(17));
            assertEquals(10, MonkFeatures.getMartialArtsDie(20));
        }

        @Test
        @DisplayName("Martial Arts damage notation is correct")
        void martialArtsDamageNotation() {
            MonkFeatures.MartialArts ma1 = MonkFeatures.createMartialArts(1);
            assertEquals("1d4", ma1.getDamageNotation());

            MonkFeatures.MartialArts ma5 = MonkFeatures.createMartialArts(5);
            assertEquals("1d6", ma5.getDamageNotation());

            MonkFeatures.MartialArts ma11 = MonkFeatures.createMartialArts(11);
            assertEquals("1d8", ma11.getDamageNotation());

            MonkFeatures.MartialArts ma17 = MonkFeatures.createMartialArts(17);
            assertEquals("1d10", ma17.getDamageNotation());
        }

        @Test
        @DisplayName("Martial Arts updates on level up")
        void martialArtsUpdatesOnLevelUp() {
            MonkFeatures.MartialArts ma = MonkFeatures.createMartialArts(1);
            assertEquals(4, ma.getDamageDie());

            ma.setMonkLevel(5);
            assertEquals(6, ma.getDamageDie());
        }
    }

    @Nested
    @DisplayName("Ki")
    class KiTests {

        @Test
        @DisplayName("Ki points equal Monk level")
        void kiPointsEqualMonkLevel() {
            assertEquals(2, MonkFeatures.calculateKiPoints(2));
            assertEquals(5, MonkFeatures.calculateKiPoints(5));
            assertEquals(10, MonkFeatures.calculateKiPoints(10));
            assertEquals(20, MonkFeatures.calculateKiPoints(20));
        }

        @Test
        @DisplayName("Ki can be spent and tracked")
        void kiCanBeSpentAndTracked() {
            MonkFeatures.Ki ki = MonkFeatures.createKi(3);

            assertEquals(3, ki.getKiPoints());
            assertEquals(3, ki.getMaxKiPoints());

            assertTrue(ki.spendKi(1));
            assertEquals(2, ki.getKiPoints());

            assertTrue(ki.spendKi(2));
            assertEquals(0, ki.getKiPoints());

            assertFalse(ki.spendKi(1));  // Not enough ki
            assertEquals(0, ki.getKiPoints());
        }

        @Test
        @DisplayName("Ki restores on short rest")
        void kiRestoresOnShortRest() {
            MonkFeatures.Ki ki = MonkFeatures.createKi(3);
            ki.spendKi(3);
            assertEquals(0, ki.getKiPoints());

            ki.resetOnShortRest();
            assertEquals(3, ki.getKiPoints());
        }

        @Test
        @DisplayName("Ki updates on level up")
        void kiUpdatesOnLevelUp() {
            MonkFeatures.Ki ki = MonkFeatures.createKi(2);
            assertEquals(2, ki.getMaxKiPoints());

            ki.setMonkLevel(5);
            assertEquals(5, ki.getMaxKiPoints());
        }
    }

    @Nested
    @DisplayName("Unarmored Defense")
    class UnarmoredDefenseTests {

        @Test
        @DisplayName("Unarmored Defense calculates AC correctly")
        void unarmoredDefenseCalculatesAC() {
            // DEX 16 (+3), WIS 14 (+2) = 10 + 3 + 2 = 15
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK,
                10, 16, 10, 10, 14, 10);

            MonkFeatures.UnarmoredDefense unarmoredDefense =
                MonkFeatures.createUnarmoredDefense();

            assertEquals(15, unarmoredDefense.calculateAC(monk));
        }

        @Test
        @DisplayName("Monk uses Unarmored Defense when not wearing armor")
        void monkUsesUnarmoredDefenseWhenUnarmored() {
            // DEX 16 (+3), WIS 14 (+2) = 10 + 3 + 2 = 15
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK,
                10, 16, 10, 10, 14, 10);

            assertEquals(15, monk.getArmorClass());
        }
    }

    @Nested
    @DisplayName("Ki Save DC")
    class KiSaveDCTests {

        @Test
        @DisplayName("Ki Save DC calculated correctly")
        void kiSaveDCCalculatedCorrectly() {
            // Level 1, WIS 14 (+2): DC = 8 + 2 (prof) + 2 (WIS) = 12
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK,
                10, 14, 10, 10, 14, 10);

            assertEquals(12, MonkFeatures.getKiSaveDC(monk));
        }

        @Test
        @DisplayName("Ki Save DC scales with level and WIS")
        void kiSaveDCScales() {
            // Level 5, WIS 16 (+3): DC = 8 + 3 (prof) + 3 (WIS) = 14
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK,
                10, 14, 10, 10, 16, 10);
            monk.setLevel(5);

            assertEquals(14, MonkFeatures.getKiSaveDC(monk));
        }
    }

    @Nested
    @DisplayName("Deflect Missiles")
    class DeflectMissilesTests {

        @Test
        @DisplayName("Deflect Missiles reduction formula is correct")
        void deflectMissilesReductionFormula() {
            // DEX 16 (+3), Level 3: 1d10+3+3
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK,
                10, 16, 10, 10, 14, 10);
            monk.setLevel(3);

            MonkFeatures.DeflectMissiles dm = MonkFeatures.createDeflectMissiles(3);
            assertEquals("1d10+3+3", dm.getReductionFormula(monk));
        }

        @Test
        @DisplayName("Deflect Missiles calculates reduction correctly")
        void deflectMissilesCalculatesReduction() {
            // DEX 16 (+3), Level 3, roll 5: 5 + 3 + 3 = 11
            Character monk = new Character("Test", Race.HUMAN, CharacterClass.MONK,
                10, 16, 10, 10, 14, 10);
            monk.setLevel(3);

            MonkFeatures.DeflectMissiles dm = MonkFeatures.createDeflectMissiles(3);
            assertEquals(11, dm.calculateReduction(monk, 5));
        }
    }

    @Nested
    @DisplayName("createFeaturesForLevel")
    class CreateFeaturesForLevelTests {

        @Test
        @DisplayName("Level 1 returns Unarmored Defense and Martial Arts")
        void level1ReturnsBasicFeatures() {
            List<ClassFeature> features = MonkFeatures.createFeaturesForLevel(1);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.UNARMORED_DEFENSE_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.MARTIAL_ARTS_ID)));
            assertEquals(2, features.size());
        }

        @Test
        @DisplayName("Level 2 includes Ki abilities")
        void level2IncludesKiAbilities() {
            List<ClassFeature> features = MonkFeatures.createFeaturesForLevel(2);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.KI_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.FLURRY_OF_BLOWS_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.PATIENT_DEFENSE_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.STEP_OF_THE_WIND_ID)));
            assertEquals(6, features.size());
        }

        @Test
        @DisplayName("Level 3 includes Monastic Tradition features")
        void level3IncludesMonasticTradition() {
            List<ClassFeature> features = MonkFeatures.createFeaturesForLevel(3);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.DEFLECT_MISSILES_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(MonkFeatures.OPEN_HAND_TECHNIQUE_ID)));
            assertEquals(8, features.size());
        }
    }
}
