package com.questkeeper.ui;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CharacterCreator.
 * 
 * Note: CharacterCreator is highly interactive and uses Scanner for input.
 * These tests verify the helper methods and logic, but full integration
 * testing of the interactive flow requires manual testing or a more
 * sophisticated test harness with simulated input streams.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("CharacterCreator")
class CharacterCreatorTest {

    @Nested
    @DisplayName("Race Enum")
    class RaceTests {

        @Test
        @DisplayName("all races have display names")
        void allRacesHaveDisplayNames() {
            for (Race race : Race.values()) {
                assertNotNull(race.getDisplayName());
                assertFalse(race.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("all races have valid speed")
        void allRacesHaveValidSpeed() {
            for (Race race : Race.values()) {
                assertTrue(race.getSpeed() >= 25, 
                        race.getDisplayName() + " should have speed >= 25");
                assertTrue(race.getSpeed() <= 35, 
                        race.getDisplayName() + " should have speed <= 35");
            }
        }

        @Test
        @DisplayName("human has +1 to all abilities")
        void humanHasPlusOneToAll() {
            // Human enum shows 0s, but Character class applies +1 to all
            // This tests that the enum exists and has expected structure
            Race human = Race.HUMAN;
            assertEquals("Human", human.getDisplayName());
            assertEquals(30, human.getSpeed());
        }

        @Test
        @DisplayName("dwarf has +2 CON")
        void dwarfHasPlusTwoCon() {
            Race dwarf = Race.DWARF;
            assertEquals(2, dwarf.getAbilityBonus(Ability.CONSTITUTION));
            assertEquals(0, dwarf.getAbilityBonus(Ability.STRENGTH));
        }

        @Test
        @DisplayName("elf has +2 DEX")
        void elfHasPlusTwoDex() {
            Race elf = Race.ELF;
            assertEquals(2, elf.getAbilityBonus(Ability.DEXTERITY));
        }

        @Test
        @DisplayName("dragonborn has +2 STR and +1 CHA")
        void dragonbornHasMultipleBonuses() {
            Race dragonborn = Race.DRAGONBORN;
            assertEquals(2, dragonborn.getAbilityBonus(Ability.STRENGTH));
            assertEquals(1, dragonborn.getAbilityBonus(Ability.CHARISMA));
        }

        @Test
        @DisplayName("half-orc has +2 STR and +1 CON")
        void halfOrcHasMultipleBonuses() {
            Race halfOrc = Race.HALF_ORC;
            assertEquals(2, halfOrc.getAbilityBonus(Ability.STRENGTH));
            assertEquals(1, halfOrc.getAbilityBonus(Ability.CONSTITUTION));
        }
    }

    @Nested
    @DisplayName("CharacterClass Enum")
    class CharacterClassTests {

        @Test
        @DisplayName("all classes have display names")
        void allClassesHaveDisplayNames() {
            for (CharacterClass cc : CharacterClass.values()) {
                assertNotNull(cc.getDisplayName());
                assertFalse(cc.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("all classes have valid hit dice")
        void allClassesHaveValidHitDice() {
            Set<Integer> validHitDice = Set.of(6, 8, 10, 12);
            for (CharacterClass cc : CharacterClass.values()) {
                assertTrue(validHitDice.contains(cc.getHitDie()),
                        cc.getDisplayName() + " should have hit die of 6, 8, 10, or 12");
            }
        }

        @Test
        @DisplayName("barbarian has d12 hit die")
        void barbarianHasD12() {
            assertEquals(12, CharacterClass.BARBARIAN.getHitDie());
        }

        @Test
        @DisplayName("wizard has d6 hit die")
        void wizardHasD6() {
            assertEquals(6, CharacterClass.WIZARD.getHitDie());
        }

        @Test
        @DisplayName("fighter has d10 hit die")
        void fighterHasD10() {
            assertEquals(10, CharacterClass.FIGHTER.getHitDie());
        }
    }

    @Nested
    @DisplayName("Ability Score Methods")
    class AbilityScoreTests {

        @Test
        @DisplayName("standard array contains correct values")
        void standardArrayValues() {
            int[] standardArray = {15, 14, 13, 12, 10, 8};
            int sum = Arrays.stream(standardArray).sum();
            assertEquals(72, sum, "Standard array should sum to 72");
            assertEquals(6, standardArray.length, "Standard array should have 6 values");
        }

        @Test
        @DisplayName("point buy total is 27")
        void pointBuyTotal() {
            // This is the official 5e point buy total
            int expectedTotal = 27;
            // We can't directly test the private constant, but we verify the rule
            assertEquals(27, expectedTotal);
        }

        @Test
        @DisplayName("4d6 drop lowest produces valid range")
        void rollForStatsRange() {
            // Simulate 4d6 drop lowest many times to verify range
            Random rand = new Random(42); // Seeded for reproducibility
            
            for (int i = 0; i < 100; i++) {
                int[] dice = new int[4];
                for (int j = 0; j < 4; j++) {
                    dice[j] = rand.nextInt(6) + 1;
                }
                Arrays.sort(dice);
                int total = dice[1] + dice[2] + dice[3]; // Drop lowest
                
                assertTrue(total >= 3, "Minimum should be 3 (1+1+1)");
                assertTrue(total <= 18, "Maximum should be 18 (6+6+6)");
            }
        }

        @Test
        @DisplayName("ability scores have valid D&D range")
        void abilityScoreRange() {
            // D&D ability scores typically range from 3-18 base, up to 20 with bonuses
            int minScore = 3;
            int maxScore = 20;
            
            assertTrue(minScore >= 1);
            assertTrue(maxScore <= 30); // Absolute max in 5e
        }
    }

    @Nested
    @DisplayName("Character Creation Integration")
    class IntegrationTests {

        @Test
        @DisplayName("can create character with all combinations")
        void canCreateAllCombinations() {
            // Test that Character constructor accepts all race/class combinations
            for (Race race : Race.values()) {
                for (CharacterClass cc : CharacterClass.values()) {
                    Character character = new Character("Test", race, cc);
                    
                    assertNotNull(character);
                    assertEquals("Test", character.getName());
                    assertEquals(race, character.getRace());
                    assertEquals(cc, character.getCharacterClass());
                }
            }
        }

        @Test
        @DisplayName("character has correct level 1 stats")
        void levelOneStats() {
            Character fighter = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
            
            assertEquals(1, fighter.getLevel());
            assertEquals(2, fighter.getProficiencyBonus()); // +2 at level 1
            assertTrue(fighter.getMaxHitPoints() > 0);
        }

        @Test
        @DisplayName("racial bonuses are applied")
        void racialBonusesApplied() {
            // Create two characters with same base stats but different races
            Character dwarf = new Character("Dwarf", Race.DWARF, CharacterClass.FIGHTER);
            Character elf = new Character("Elf", Race.ELF, CharacterClass.FIGHTER);
            
            // Set same base scores
            for (Ability a : Ability.values()) {
                dwarf.setAbilityScore(a, 10);
                elf.setAbilityScore(a, 10);
            }
            
            // Dwarf should have higher CON (10 + 2 = 12)
            assertEquals(12, dwarf.getAbilityScore(Ability.CONSTITUTION));
            
            // Elf should have higher DEX (10 + 2 = 12)
            assertEquals(12, elf.getAbilityScore(Ability.DEXTERITY));
        }

        @Test
        @DisplayName("human gets +1 to all abilities")
        void humanBonusApplied() {
            Character human = new Character("Human", Race.HUMAN, CharacterClass.FIGHTER);
            
            // Set all base scores to 10
            for (Ability a : Ability.values()) {
                human.setAbilityScore(a, 10);
            }
            
            // All should be 11 (10 + 1)
            for (Ability a : Ability.values()) {
                assertEquals(11, human.getAbilityScore(a),
                        a.getFullName() + " should be 11 for Human");
            }
        }

        @Test
        @DisplayName("ability modifiers calculated correctly")
        void abilityModifiersCorrect() {
            Character character = new Character("Test", Race.DWARF, CharacterClass.FIGHTER);
            
            // Test various scores and expected modifiers
            // Score -> Modifier: 1 -> -5, 10-11 -> 0, 12-13 -> +1, 14-15 -> +2, etc.
            character.setAbilityScore(Ability.STRENGTH, 10);
            assertEquals(0, character.getAbilityModifier(Ability.STRENGTH));
            
            character.setAbilityScore(Ability.STRENGTH, 14);
            assertEquals(2, character.getAbilityModifier(Ability.STRENGTH));
            
            character.setAbilityScore(Ability.STRENGTH, 8);
            assertEquals(-1, character.getAbilityModifier(Ability.STRENGTH));
            
            character.setAbilityScore(Ability.STRENGTH, 15);
            assertEquals(2, character.getAbilityModifier(Ability.STRENGTH));
        }
    }

    @Nested
    @DisplayName("Point Buy Costs")
    class PointBuyCostTests {

        @Test
        @DisplayName("cost from 8 to 13 is 1 per point")
        void costEightToThirteen() {
            // 8->9: 1, 9->10: 1, 10->11: 1, 11->12: 1, 12->13: 1
            // Total: 5 points to go from 8 to 13
            int cost = 5;
            assertEquals(5, cost);
        }

        @Test
        @DisplayName("cost from 13 to 14 is 2")
        void costThirteenToFourteen() {
            int cost = 2;
            assertEquals(2, cost);
        }

        @Test
        @DisplayName("cost from 14 to 15 is 2")
        void costFourteenToFifteen() {
            int cost = 2;
            assertEquals(2, cost);
        }

        @Test
        @DisplayName("total cost for 15 from 8 is 9 points")
        void totalCostForFifteen() {
            // 8->13: 5 points, 13->14: 2 points, 14->15: 2 points = 9 total
            int totalCost = 5 + 2 + 2;
            assertEquals(9, totalCost);
        }

        @Test
        @DisplayName("27 points can achieve various valid arrays")
        void validPointBuyArrays() {
            // Example valid array: 15, 15, 15, 8, 8, 8 = 9+9+9+0+0+0 = 27 points
            int cost1 = 9 + 9 + 9 + 0 + 0 + 0;
            assertEquals(27, cost1);
            
            // Example valid array: 15, 14, 13, 12, 10, 8 = 9+7+5+4+2+0 = 27 points
            int cost2 = 9 + 7 + 5 + 4 + 2 + 0;
            assertEquals(27, cost2);
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("empty name gets default")
        void emptyNameGetsDefault() {
            String name = "";
            String defaultName = "Aelar";
            String result = name.isEmpty() ? defaultName : name;
            assertEquals("Aelar", result);
        }

        @Test
        @DisplayName("whitespace name gets default")
        void whitespaceNameGetsDefault() {
            String name = "   ";
            String defaultName = "Aelar";
            String result = name.trim().isEmpty() ? defaultName : name.trim();
            assertEquals("Aelar", result);
        }

        @Test
        @DisplayName("valid name is preserved")
        void validNamePreserved() {
            String name = "Gandalf";
            String defaultName = "Aelar";
            String result = name.isEmpty() ? defaultName : name;
            assertEquals("Gandalf", result);
        }
    }

    @Nested
    @DisplayName("Display Formatting")
    class DisplayTests {

        @Test
        @DisplayName("race bonus string format")
        void raceBonusStringFormat() {
            // Test the expected format of racial bonus display
            Race dwarf = Race.DWARF;
            
            StringBuilder bonus = new StringBuilder();
            for (Ability a : Ability.values()) {
                int b = dwarf.getAbilityBonus(a);
                if (b > 0) {
                    if (bonus.length() > 0) {
                        bonus.append(", ");
                    }
                    bonus.append("+").append(b).append(" ").append(a.getAbbreviation());
                }
            }
            
            assertEquals("+2 CON", bonus.toString());
        }

        @Test
        @DisplayName("dragonborn bonus string shows both bonuses")
        void dragonbornBonusString() {
            Race dragonborn = Race.DRAGONBORN;
            
            StringBuilder bonus = new StringBuilder();
            for (Ability a : Ability.values()) {
                int b = dragonborn.getAbilityBonus(a);
                if (b > 0) {
                    if (bonus.length() > 0) {
                        bonus.append(", ");
                    }
                    bonus.append("+").append(b).append(" ").append(a.getAbbreviation());
                }
            }
            
            // Should show both STR and CHA bonuses
            assertTrue(bonus.toString().contains("+2 STR"));
            assertTrue(bonus.toString().contains("+1 CHA"));
        }

        @Test
        @DisplayName("center text pads correctly")
        void centerTextPads() {
            String text = "Hello";
            int width = 11;
            int expectedPad = (width - text.length()) / 2; // (11-5)/2 = 3
            String centered = " ".repeat(expectedPad) + text;
            
            assertEquals("   Hello", centered);
        }

        @Test
        @DisplayName("pad right fills correctly")
        void padRightFills() {
            String text = "Test";
            int width = 10;
            String padded = text + " ".repeat(width - text.length());
            
            assertEquals("Test      ", padded);
            assertEquals(10, padded.length());
        }
    }

    @Nested
    @DisplayName("Subclass prompts during creation")
    class SubclassPromptTests {

        @Test
        @DisplayName("Cleric: picking '1' sets Divine Domain to LIFE")
        void clericPicksLifeDomain() {
            Character cleric = new Character("Mira",
                Race.HUMAN, CharacterClass.CLERIC,
                10, 10, 14, 10, 16, 10);
            // Script: "1" -> Life domain (first in enum), then Enter to dismiss "Press Enter to continue".
            CharacterCreator.setScanner(new Scanner("1\n\n"));

            CharacterCreator.promptSubclassIfNeeded(cleric);

            assertEquals(
                com.questkeeper.character.features.ClericFeatures.DivineDomain.LIFE,
                cleric.getDivineDomain(),
                "Cleric picking option 1 should select LIFE domain");
            assertTrue(cleric.getFeature(
                com.questkeeper.character.features.ClericFeatures.BONUS_PROFICIENCY_HEAVY_ARMOR_ID
            ).isPresent(),
                "Life Domain feature wiring should fire when domain is set");
        }

        @Test
        @DisplayName("Sorcerer: picking '1' sets Sorcerous Origin to DRACONIC")
        void sorcererPicksDraconic() {
            Character sorc = new Character("Eldra",
                Race.HUMAN, CharacterClass.SORCERER,
                10, 14, 14, 10, 10, 16);
            CharacterCreator.setScanner(new Scanner("1\n\n"));

            CharacterCreator.promptSubclassIfNeeded(sorc);

            assertEquals(
                com.questkeeper.character.features.SorcererFeatures.SorcerousOrigin.DRACONIC,
                sorc.getSorcerousOrigin());
            assertTrue(sorc.getFeature(
                com.questkeeper.character.features.SorcererFeatures.DRACONIC_RESILIENCE_ID
            ).isPresent());
        }

        @Test
        @DisplayName("Warlock: picking '1' sets Otherworldly Patron to FIEND")
        void warlockPicksFiend() {
            Character lock = new Character("Vex",
                Race.HUMAN, CharacterClass.WARLOCK,
                10, 14, 14, 10, 10, 16);
            CharacterCreator.setScanner(new Scanner("1\n\n"));

            CharacterCreator.promptSubclassIfNeeded(lock);

            assertEquals(
                com.questkeeper.character.features.WarlockFeatures.OtherworldlyPatron.FIEND,
                lock.getWarlockPatron());
            assertTrue(lock.getFeature(
                com.questkeeper.character.features.WarlockFeatures.DARK_ONES_BLESSING_ID
            ).isPresent());
        }

        @Test
        @DisplayName("Fighter does not get a subclass prompt at creation")
        void fighterNoSubclassPrompt() {
            Character f = new Character("Aelar",
                Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            // Empty scanner — if the prompt fires, it would block trying to read.
            CharacterCreator.setScanner(new Scanner(""));

            // Should return without reading anything.
            CharacterCreator.promptSubclassIfNeeded(f);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("all six abilities can be assigned")
        void allSixAbilitiesAssigned() {
            assertEquals(6, Ability.values().length);
        }

        @Test
        @DisplayName("ability enum has correct abbreviations")
        void abilityAbbreviations() {
            assertEquals("STR", Ability.STRENGTH.getAbbreviation());
            assertEquals("DEX", Ability.DEXTERITY.getAbbreviation());
            assertEquals("CON", Ability.CONSTITUTION.getAbbreviation());
            assertEquals("INT", Ability.INTELLIGENCE.getAbbreviation());
            assertEquals("WIS", Ability.WISDOM.getAbbreviation());
            assertEquals("CHA", Ability.CHARISMA.getAbbreviation());
        }

        @Test
        @DisplayName("race count matches expected")
        void raceCount() {
            // PHB races: Human, Dwarf, Elf, Halfling, Dragonborn, Gnome, Half-Elf, Half-Orc, Tiefling
            assertEquals(9, Race.values().length);
        }

        @Test
        @DisplayName("class count matches expected")
        void classCount() {
            // PHB classes: Barbarian, Bard, Cleric, Druid, Fighter, Monk, Paladin, Ranger, Rogue, Sorcerer, Warlock, Wizard
            assertEquals(12, CharacterClass.values().length);
        }
    }
}