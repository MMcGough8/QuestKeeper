package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaladinFeatures")
class PaladinFeaturesTest {

    @Nested
    @DisplayName("Feature Initialization")
    class FeatureInitializationTests {

        @Test
        @DisplayName("Level 1 Paladin has Divine Sense")
        void level1PaladinHasDivineSense() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);

            assertTrue(paladin.getFeature(PaladinFeatures.DIVINE_SENSE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Paladin has Lay on Hands")
        void level1PaladinHasLayOnHands() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);

            assertTrue(paladin.getFeature(PaladinFeatures.LAY_ON_HANDS_ID).isPresent());
        }

        @Test
        @DisplayName("Level 1 Paladin does not have Divine Smite")
        void level1PaladinDoesNotHaveDivineSmite() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);

            assertTrue(paladin.getFeature(PaladinFeatures.DIVINE_SMITE_ID).isEmpty());
        }

        @Test
        @DisplayName("Level 2 Paladin has Divine Smite")
        void level2PaladinHasDivineSmite() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);
            paladin.setLevel(2);

            assertTrue(paladin.getFeature(PaladinFeatures.DIVINE_SMITE_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Paladin has Divine Health")
        void level3PaladinHasDivineHealth() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);
            paladin.setLevel(3);

            assertTrue(paladin.getFeature(PaladinFeatures.DIVINE_HEALTH_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Paladin has Channel Divinity")
        void level3PaladinHasChannelDivinity() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);
            paladin.setLevel(3);

            assertTrue(paladin.getFeature(PaladinFeatures.CHANNEL_DIVINITY_ID).isPresent());
        }

        @Test
        @DisplayName("Level 3 Paladin has Sacred Weapon")
        void level3PaladinHasSacredWeapon() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);
            paladin.setLevel(3);

            assertTrue(paladin.getFeature(PaladinFeatures.SACRED_WEAPON_ID).isPresent());
        }

        @Test
        @DisplayName("Non-Paladin class has no Paladin features")
        void nonPaladinHasNoPaladinFeatures() {
            Character fighter = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(fighter.getFeature(PaladinFeatures.DIVINE_SENSE_ID).isEmpty());
            assertTrue(fighter.getFeature(PaladinFeatures.LAY_ON_HANDS_ID).isEmpty());
        }
    }

    @Nested
    @DisplayName("Lay on Hands")
    class LayOnHandsTests {

        @Test
        @DisplayName("Lay on Hands pool equals level times 5")
        void layOnHandsPoolEqualsLevelTimes5() {
            assertEquals(5, PaladinFeatures.getLayOnHandsPool(1));
            assertEquals(15, PaladinFeatures.getLayOnHandsPool(3));
            assertEquals(50, PaladinFeatures.getLayOnHandsPool(10));
            assertEquals(100, PaladinFeatures.getLayOnHandsPool(20));
        }

        @Test
        @DisplayName("Lay on Hands can heal")
        void layOnHandsCanHeal() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN,
                14, 10, 14, 10, 10, 14);
            paladin.takeDamage(10);
            int startHp = paladin.getCurrentHitPoints();

            PaladinFeatures.LayOnHands loh = PaladinFeatures.createLayOnHands(1);
            int healed = loh.heal(paladin, 5);

            assertEquals(5, healed);
            assertEquals(startHp + 5, paladin.getCurrentHitPoints());
            assertEquals(0, loh.getPoolRemaining());
        }

        @Test
        @DisplayName("Lay on Hands cannot heal more than pool")
        void layOnHandsCannotHealMoreThanPool() {
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);
            paladin.takeDamage(20);

            PaladinFeatures.LayOnHands loh = PaladinFeatures.createLayOnHands(1);  // 5 HP pool
            int healed = loh.heal(paladin, 10);

            assertEquals(5, healed);  // Only healed 5 (pool max)
            assertEquals(0, loh.getPoolRemaining());
        }

        @Test
        @DisplayName("Lay on Hands pool restores on long rest")
        void layOnHandsPoolRestoresOnLongRest() {
            PaladinFeatures.LayOnHands loh = PaladinFeatures.createLayOnHands(2);  // 10 HP pool
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);
            paladin.takeDamage(10);
            loh.heal(paladin, 10);
            assertEquals(0, loh.getPoolRemaining());

            loh.resetOnLongRest();
            assertEquals(10, loh.getPoolRemaining());
        }

        @Test
        @DisplayName("Lay on Hands pool scales with level")
        void layOnHandsPoolScalesWithLevel() {
            PaladinFeatures.LayOnHands loh = PaladinFeatures.createLayOnHands(1);
            assertEquals(5, loh.getMaxPool());

            loh.setPaladinLevel(3);
            assertEquals(15, loh.getMaxPool());
        }
    }

    @Nested
    @DisplayName("Divine Sense")
    class DivineSenseTests {

        @Test
        @DisplayName("Divine Sense uses equal 1 plus CHA modifier")
        void divineSenseUsesEqual1PlusCHA() {
            // CHA 14 (+2): 1 + 2 = 3 uses
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN,
                10, 10, 10, 10, 10, 14);

            assertEquals(3, PaladinFeatures.getDivineSenseUses(paladin));
        }

        @Test
        @DisplayName("Divine Sense minimum 1 use with negative CHA")
        void divineSenseMinimum1Use() {
            // CHA 8 (-1): 1 + max(0, -1) = 1 use
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN,
                10, 10, 10, 10, 10, 8);

            assertEquals(1, PaladinFeatures.getDivineSenseUses(paladin));
        }
    }

    @Nested
    @DisplayName("Divine Smite")
    class DivineSmiteTests {

        @Test
        @DisplayName("Level 2 Paladin has 2 first-level spell slots")
        void level2Has2FirstLevelSlots() {
            int[] slots = PaladinFeatures.getSpellSlots(2);
            assertEquals(2, slots[0]);  // 1st level
            assertEquals(0, slots[1]);  // 2nd level
        }

        @Test
        @DisplayName("Level 3 Paladin has 3 first-level spell slots")
        void level3Has3FirstLevelSlots() {
            int[] slots = PaladinFeatures.getSpellSlots(3);
            assertEquals(3, slots[0]);  // 1st level
        }

        @Test
        @DisplayName("Level 5 Paladin has 4 first-level and 2 second-level slots")
        void level5Has4And2Slots() {
            int[] slots = PaladinFeatures.getSpellSlots(5);
            assertEquals(4, slots[0]);  // 1st level
            assertEquals(2, slots[1]);  // 2nd level
        }

        @Test
        @DisplayName("Divine Smite damage scales with slot level")
        void divineSmiteDamageScalesWithSlotLevel() {
            PaladinFeatures.DivineSmite smite = PaladinFeatures.createDivineSmite(5);

            // 1st level: 2d8
            assertEquals(2, smite.getSmiteDice(1, false));
            assertEquals("2d8", smite.getSmiteDamageNotation(1, false));

            // 2nd level: 3d8
            assertEquals(3, smite.getSmiteDice(2, false));
            assertEquals("3d8", smite.getSmiteDamageNotation(2, false));

            // 3rd level: 4d8
            assertEquals(4, smite.getSmiteDice(3, false));

            // 4th level: 5d8 (max)
            assertEquals(5, smite.getSmiteDice(4, false));

            // 5th level: still 5d8 (max)
            assertEquals(5, smite.getSmiteDice(5, false));
        }

        @Test
        @DisplayName("Divine Smite adds 1d8 vs undead or fiend")
        void divineSmiteExtraDamageVsUndeadFiend() {
            PaladinFeatures.DivineSmite smite = PaladinFeatures.createDivineSmite(5);

            // 1st level vs undead: 3d8 (2d8 + 1d8)
            assertEquals(3, smite.getSmiteDice(1, true));
            assertEquals("3d8", smite.getSmiteDamageNotation(1, true));

            // 2nd level vs undead: 4d8 (3d8 + 1d8)
            assertEquals(4, smite.getSmiteDice(2, true));
        }

        @Test
        @DisplayName("Divine Smite can expend spell slots")
        void divineSmiteCanExpendSlots() {
            PaladinFeatures.DivineSmite smite = PaladinFeatures.createDivineSmite(2);  // 2 first-level slots

            assertTrue(smite.hasSlot(1));
            assertEquals(1, smite.getLowestAvailableSlot());

            assertTrue(smite.expendSlot(1));
            assertTrue(smite.hasSlot(1));  // Still has 1 left

            assertTrue(smite.expendSlot(1));
            assertFalse(smite.hasSlot(1));  // No more slots
            assertEquals(0, smite.getLowestAvailableSlot());
        }

        @Test
        @DisplayName("Divine Smite spell slots restore on long rest")
        void divineSmiteSlotsRestoreOnLongRest() {
            PaladinFeatures.DivineSmite smite = PaladinFeatures.createDivineSmite(2);
            smite.expendSlot(1);
            smite.expendSlot(1);
            assertFalse(smite.hasSlot(1));

            smite.restoreAllSlots();
            assertTrue(smite.hasSlot(1));
            assertEquals(2, smite.getCurrentSlots()[0]);
        }
    }

    @Nested
    @DisplayName("Channel Divinity")
    class ChannelDivinityTests {

        @Test
        @DisplayName("Channel Divinity has 1 use per short rest")
        void channelDivinityHas1UsePerShortRest() {
            PaladinFeatures.ChannelDivinity cd = PaladinFeatures.createChannelDivinity();

            assertTrue(cd.canUse());
            assertEquals(1, cd.getMaxUses());
            assertEquals(ResetType.SHORT_REST, cd.getResetType());
        }

        @Test
        @DisplayName("Channel Divinity restores on short rest")
        void channelDivinityRestoresOnShortRest() {
            PaladinFeatures.ChannelDivinity cd = PaladinFeatures.createChannelDivinity();
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN);

            cd.use(paladin);
            assertFalse(cd.canUse());

            cd.resetOnShortRest();
            assertTrue(cd.canUse());
        }
    }

    @Nested
    @DisplayName("Sacred Weapon")
    class SacredWeaponTests {

        @Test
        @DisplayName("Sacred Weapon can be activated")
        void sacredWeaponCanBeActivated() {
            PaladinFeatures.SacredWeapon sw = PaladinFeatures.createSacredWeapon();

            assertFalse(sw.isActive());

            String result = sw.activate(3);  // +3 CHA mod
            assertTrue(sw.isActive());
            assertEquals(10, sw.getRoundsRemaining());
            assertTrue(result.contains("+3"));
        }

        @Test
        @DisplayName("Sacred Weapon duration decrements each turn")
        void sacredWeaponDurationDecrements() {
            PaladinFeatures.SacredWeapon sw = PaladinFeatures.createSacredWeapon();
            sw.activate(2);

            assertEquals(10, sw.getRoundsRemaining());

            assertTrue(sw.processTurnEnd());
            assertEquals(9, sw.getRoundsRemaining());

            // Simulate several turns
            for (int i = 0; i < 8; i++) {
                assertTrue(sw.processTurnEnd());
            }
            assertEquals(1, sw.getRoundsRemaining());

            // Last turn
            assertFalse(sw.processTurnEnd());  // Returns false when ends
            assertFalse(sw.isActive());
        }
    }

    @Nested
    @DisplayName("Turn the Unholy")
    class TurnTheUnholyTests {

        @Test
        @DisplayName("Turn save DC calculated correctly")
        void turnSaveDCCalculatedCorrectly() {
            // Level 3, CHA 16 (+3): DC = 8 + 2 (prof) + 3 (CHA) = 13
            Character paladin = new Character("Test", Race.HUMAN, CharacterClass.PALADIN,
                14, 10, 14, 10, 10, 16);
            paladin.setLevel(3);

            PaladinFeatures.TurnTheUnholy turn = PaladinFeatures.createTurnTheUnholy();
            assertEquals(13, turn.getSaveDC(paladin));
        }
    }

    @Nested
    @DisplayName("createFeaturesForLevel")
    class CreateFeaturesForLevelTests {

        @Test
        @DisplayName("Level 1 returns Divine Sense and Lay on Hands")
        void level1ReturnsBasicFeatures() {
            List<ClassFeature> features = PaladinFeatures.createFeaturesForLevel(1, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.DIVINE_SENSE_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.LAY_ON_HANDS_ID)));
            assertEquals(2, features.size());
        }

        @Test
        @DisplayName("Level 2 includes Divine Smite")
        void level2IncludesDivineSmite() {
            List<ClassFeature> features = PaladinFeatures.createFeaturesForLevel(2, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.DIVINE_SMITE_ID)));
        }

        @Test
        @DisplayName("Level 2 with fighting style includes Fighting Style")
        void level2WithFightingStyleIncludesIt() {
            List<ClassFeature> features = PaladinFeatures.createFeaturesForLevel(2, FightingStyle.DEFENSE);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.PALADIN_FIGHTING_STYLE_ID)));
        }

        @Test
        @DisplayName("Level 3 includes Sacred Oath features")
        void level3IncludesSacredOath() {
            List<ClassFeature> features = PaladinFeatures.createFeaturesForLevel(3, null);

            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.DIVINE_HEALTH_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.CHANNEL_DIVINITY_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.SACRED_WEAPON_ID)));
            assertTrue(features.stream().anyMatch(f -> f.getId().equals(PaladinFeatures.TURN_THE_UNHOLY_ID)));
        }
    }
}
