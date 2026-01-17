package com.questkeeper;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.combat.CombatResult;
import com.questkeeper.combat.CombatSystem;
import com.questkeeper.combat.Monster;
import com.questkeeper.core.CommandParser;
import com.questkeeper.core.Dice;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.world.Location;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and boundary condition tests across all systems.
 * These tests verify correct behavior at system boundaries and
 * with unexpected inputs.
 */
@DisplayName("Edge Case Tests")
class EdgeCaseTest {

    // ========================================================================
    // CHARACTER EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Character HP Edge Cases")
    class CharacterHpEdgeCases {

        private Character character;

        @BeforeEach
        void setUp() {
            character = new Character("EdgeCase", Race.HUMAN, CharacterClass.FIGHTER);
            character.setAbilityScore(Ability.CONSTITUTION, 14); // +2 modifier
        }

        @Test
        @DisplayName("Character at exactly 0 HP is not alive")
        void characterAtZeroHpIsNotAlive() {
            int maxHp = character.getMaxHitPoints();
            character.takeDamage(maxHp);

            assertEquals(0, character.getCurrentHitPoints());
            assertFalse(character.isAlive());
        }

        @Test
        @DisplayName("Character at 1 HP is still alive")
        void characterAtOneHpIsAlive() {
            int maxHp = character.getMaxHitPoints();
            character.takeDamage(maxHp - 1);

            assertEquals(1, character.getCurrentHitPoints());
            assertTrue(character.isAlive());
        }

        @Test
        @DisplayName("Negative damage is treated as 0")
        void negativeDamageIsIgnored() {
            int initialHp = character.getCurrentHitPoints();
            character.takeDamage(-10);

            assertEquals(initialHp, character.getCurrentHitPoints());
        }

        @Test
        @DisplayName("Overkill damage stops at 0 HP")
        void overkillDamageStopsAtZero() {
            character.takeDamage(1000);

            assertEquals(0, character.getCurrentHitPoints());
            assertTrue(character.getCurrentHitPoints() >= 0);
        }

        @Test
        @DisplayName("Healing at full HP does nothing")
        void healingAtFullHpDoesNothing() {
            int maxHp = character.getMaxHitPoints();
            character.heal(100);

            assertEquals(maxHp, character.getCurrentHitPoints());
        }

        @Test
        @DisplayName("Healing cannot exceed max HP")
        void healingCannotExceedMaxHp() {
            character.takeDamage(5);
            int maxHp = character.getMaxHitPoints();
            character.heal(1000);

            assertEquals(maxHp, character.getCurrentHitPoints());
        }

        @Test
        @DisplayName("Negative healing is treated as 0")
        void negativeHealingIsIgnored() {
            character.takeDamage(5);
            int currentHp = character.getCurrentHitPoints();
            character.heal(-10);

            assertEquals(currentHp, character.getCurrentHitPoints());
        }

        @Test
        @DisplayName("Zero damage does nothing")
        void zeroDamageDoesNothing() {
            int initialHp = character.getCurrentHitPoints();
            character.takeDamage(0);

            assertEquals(initialHp, character.getCurrentHitPoints());
        }

        @Test
        @DisplayName("Zero healing does nothing")
        void zeroHealingDoesNothing() {
            character.takeDamage(5);
            int currentHp = character.getCurrentHitPoints();
            character.heal(0);

            assertEquals(currentHp, character.getCurrentHitPoints());
        }
    }

    @Nested
    @DisplayName("Character Ability Score Edge Cases")
    class CharacterAbilityEdgeCases {

        private Character character;

        @BeforeEach
        void setUp() {
            character = new Character("EdgeCase", Race.HUMAN, CharacterClass.FIGHTER);
        }

        @Test
        @DisplayName("Minimum ability score of 1 gives -4 modifier (with human +1 bonus)")
        void minimumAbilityScoreModifier() {
            // Human race gives +1 to all abilities, so score 1 becomes 2
            // Modifier = (2 - 10) / 2 = -4
            character.setAbilityScore(Ability.STRENGTH, 1);
            assertEquals(-4, character.getAbilityModifier(Ability.STRENGTH));
        }

        @Test
        @DisplayName("Ability score of 10 gives 0 modifier")
        void baseAbilityScoreModifier() {
            character.setAbilityScore(Ability.STRENGTH, 10);
            assertEquals(0, character.getAbilityModifier(Ability.STRENGTH));
        }

        @Test
        @DisplayName("Maximum ability score of 20 gives +5 modifier")
        void maximumAbilityScoreModifier() {
            character.setAbilityScore(Ability.STRENGTH, 20);
            assertEquals(5, character.getAbilityModifier(Ability.STRENGTH));
        }

        @Test
        @DisplayName("Ability score of 11 gives +1 modifier (with human +1 bonus)")
        void oddAbilityScoreRoundsDown() {
            // Human race gives +1 to all abilities, so score 11 becomes 12
            // Modifier = (12 - 10) / 2 = 1
            character.setAbilityScore(Ability.STRENGTH, 11);
            assertEquals(1, character.getAbilityModifier(Ability.STRENGTH));
        }

        @Test
        @DisplayName("Setting ability score below 1 is clamped to 1")
        void abilityScoreBelowMinimumIsClamped() {
            character.setAbilityScore(Ability.STRENGTH, 0);
            assertTrue(character.getAbilityScore(Ability.STRENGTH) >= 1);
        }

        @Test
        @DisplayName("Setting ability score above 20 is clamped to 20")
        void abilityScoreAboveMaximumIsClamped() {
            character.setAbilityScore(Ability.STRENGTH, 25);
            assertTrue(character.getAbilityScore(Ability.STRENGTH) <= 20);
        }
    }

    @Nested
    @DisplayName("Character Level Edge Cases")
    class CharacterLevelEdgeCases {

        private Character character;

        @BeforeEach
        void setUp() {
            character = new Character("EdgeCase", Race.HUMAN, CharacterClass.FIGHTER);
        }

        @Test
        @DisplayName("New character starts at level 1")
        void newCharacterStartsAtLevel1() {
            assertEquals(1, character.getLevel());
        }

        @Test
        @DisplayName("Character starts with 0 XP")
        void characterStartsWithZeroXp() {
            assertEquals(0, character.getExperiencePoints());
        }

        @Test
        @DisplayName("Adding 0 XP does not level up")
        void addingZeroXpDoesNotLevelUp() {
            character.addExperience(0);
            assertEquals(1, character.getLevel());
        }

        @Test
        @DisplayName("Adding negative XP is ignored")
        void addingNegativeXpIsIgnored() {
            character.addExperience(100);
            int currentXp = character.getExperiencePoints();
            character.addExperience(-50);
            assertEquals(currentXp, character.getExperiencePoints());
        }

        @Test
        @DisplayName("Exactly 300 XP reaches level 2")
        void exactly300XpReachesLevel2() {
            character.addExperience(300);
            assertEquals(2, character.getLevel());
        }

        @Test
        @DisplayName("299 XP stays at level 1")
        void justUnderThresholdStaysAtLevel() {
            character.addExperience(299);
            assertEquals(1, character.getLevel());
        }

        @Test
        @DisplayName("Large XP gain can skip levels")
        void largeXpGainCanSkipLevels() {
            character.addExperience(6500); // Enough for level 5
            assertTrue(character.getLevel() >= 5);
        }
    }

    // ========================================================================
    // INVENTORY EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Empty Inventory Edge Cases")
    class EmptyInventoryEdgeCases {

        private Inventory inventory;

        @BeforeEach
        void setUp() {
            inventory = new Inventory(10); // STR 10 = 150 lbs capacity
        }

        @Test
        @DisplayName("Empty inventory has no items")
        void emptyInventoryHasNoItems() {
            assertTrue(inventory.isEmpty());
            assertEquals(0, inventory.getTotalItemCount());
        }

        @Test
        @DisplayName("Empty inventory has zero weight")
        void emptyInventoryHasZeroWeight() {
            assertEquals(0.0, inventory.getCurrentWeight(), 0.001);
        }

        @Test
        @DisplayName("Removing from empty inventory returns false")
        void removingFromEmptyInventoryReturnsFalse() {
            Item item = new Item("Test", Item.ItemType.MISCELLANEOUS, "Test item", 1.0, 0);
            assertFalse(inventory.removeItem(item));
        }

        @Test
        @DisplayName("Finding item in empty inventory returns empty")
        void findingItemInEmptyInventoryReturnsEmpty() {
            assertTrue(inventory.findItemById("nonexistent").isEmpty());
        }

        @Test
        @DisplayName("Empty inventory getAllItems returns empty list")
        void emptyInventoryGetAllItemsReturnsEmptyList() {
            assertTrue(inventory.getAllItems().isEmpty());
        }

        @Test
        @DisplayName("Empty inventory has zero gold by default")
        void emptyInventoryHasZeroGold() {
            assertEquals(0, inventory.getGold());
        }

        @Test
        @DisplayName("Cannot remove gold from empty inventory")
        void cannotRemoveGoldFromEmpty() {
            assertFalse(inventory.removeGold(10));
        }
    }

    @Nested
    @DisplayName("Inventory Weight Edge Cases")
    class InventoryWeightEdgeCases {

        private Inventory inventory;

        @BeforeEach
        void setUp() {
            inventory = new Inventory(10); // 150 lbs capacity
        }

        @Test
        @DisplayName("Adding item at exactly weight limit succeeds")
        void addingItemAtExactlyWeightLimitSucceeds() {
            Item heavyItem = new Item("Heavy Item", Item.ItemType.MISCELLANEOUS, "Test", 150.0, 0);

            assertTrue(inventory.addItem(heavyItem));
        }

        @Test
        @DisplayName("Adding item over weight limit fails")
        void addingItemOverWeightLimitFails() {
            Item heavyItem = new Item("Heavy Item", Item.ItemType.MISCELLANEOUS, "Test", 151.0, 0);

            assertFalse(inventory.addItem(heavyItem));
        }

        @Test
        @DisplayName("Adding zero weight item always succeeds")
        void addingZeroWeightItemSucceeds() {
            // Fill inventory to weight limit
            Item filler = new Item("Filler", Item.ItemType.MISCELLANEOUS, "Test", 150.0, 0);
            inventory.addItem(filler);

            // Add weightless item
            Item weightless = new Item("Weightless", Item.ItemType.MISCELLANEOUS, "Test", 0.0, 0);

            assertTrue(inventory.addItem(weightless));
        }
    }

    @Nested
    @DisplayName("Inventory Equipment Edge Cases")
    class InventoryEquipmentEdgeCases {

        private Inventory inventory;

        @BeforeEach
        void setUp() {
            inventory = new Inventory(14);
        }

        @Test
        @DisplayName("Equipping null item returns null")
        void equippingNullReturnsNull() {
            assertNull(inventory.equip(null));
        }

        @Test
        @DisplayName("Equipping non-equippable item returns null")
        void equippingNonEquippableReturnsNull() {
            Item potion = new Item("Potion", Item.ItemType.CONSUMABLE, "Healing", 0.5, 50);
            inventory.addItem(potion);

            assertNull(inventory.equip(potion));
        }

        @Test
        @DisplayName("Unequipping empty slot returns null")
        void unequippingEmptySlotReturnsNull() {
            assertNull(inventory.unequip(Inventory.EquipmentSlot.MAIN_HAND));
        }

        @Test
        @DisplayName("All equipment slots start empty")
        void allSlotsStartEmpty() {
            for (Inventory.EquipmentSlot slot : Inventory.EquipmentSlot.values()) {
                assertTrue(inventory.isSlotEmpty(slot));
            }
        }
    }

    // ========================================================================
    // COMBAT EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Combat Edge Cases")
    class CombatEdgeCases {

        private CombatSystem combatSystem;
        private Character character;
        private Monster monster;

        @BeforeEach
        void setUp() {
            combatSystem = new CombatSystem();
            character = new Character("Fighter", Race.HUMAN, CharacterClass.FIGHTER);
            character.setAbilityScore(Ability.STRENGTH, 16);
            character.setAbilityScore(Ability.DEXTERITY, 14);
            character.setAbilityScore(Ability.CONSTITUTION, 14);

            monster = new Monster("goblin", "Goblin", 13, 7);
        }

        @Test
        @DisplayName("Combat system starts not in combat")
        void combatSystemStartsNotInCombat() {
            assertFalse(combatSystem.isInCombat());
        }

        @Test
        @DisplayName("Starting combat with empty enemy list returns error")
        void startingCombatWithEmptyEnemiesReturnsError() {
            // Create a minimal GameState for testing
            var campaign = com.questkeeper.campaign.Campaign.loadFromYaml(
                java.nio.file.Path.of("src/main/resources/campaigns/muddlebrook"));
            var state = new com.questkeeper.state.GameState(character, campaign);

            CombatResult result = combatSystem.startCombat(state, List.of());
            assertTrue(result.isError());
        }

        @Test
        @DisplayName("Player turn when not in combat returns error")
        void playerTurnWhenNotInCombatReturnsError() {
            CombatResult result = combatSystem.playerTurn("attack", "goblin");
            assertTrue(result.isError());
        }

        @Test
        @DisplayName("Monster with 0 HP is not alive")
        void monsterAtZeroHpIsNotAlive() {
            monster.takeDamage(monster.getMaxHitPoints());
            assertFalse(monster.isAlive());
        }

        @Test
        @DisplayName("Monster HP cannot go below 0")
        void monsterHpCannotGoBelowZero() {
            monster.takeDamage(1000);
            assertTrue(monster.getCurrentHitPoints() >= 0);
        }
    }

    // ========================================================================
    // COMMAND PARSER EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Command Parser Edge Cases")
    class CommandParserEdgeCases {

        @Test
        @DisplayName("Empty input returns invalid command")
        void emptyInputReturnsInvalid() {
            CommandParser.Command cmd = CommandParser.parse("");
            assertFalse(cmd.isValid());
        }

        @Test
        @DisplayName("Whitespace only input returns invalid command")
        void whitespaceOnlyReturnsInvalid() {
            CommandParser.Command cmd = CommandParser.parse("   ");
            assertFalse(cmd.isValid());
        }

        @Test
        @DisplayName("Null input returns invalid command")
        void nullInputHandled() {
            // Parser gracefully handles null by returning invalid command
            CommandParser.Command cmd = CommandParser.parse(null);
            assertFalse(cmd.isValid());
        }

        @Test
        @DisplayName("Very long input is handled")
        void veryLongInputIsHandled() {
            String longInput = "go " + "north ".repeat(1000);
            CommandParser.Command cmd = CommandParser.parse(longInput);
            // Should either parse or return invalid, but not crash
            assertNotNull(cmd);
        }

        @Test
        @DisplayName("Special characters in input are handled")
        void specialCharactersHandled() {
            CommandParser.Command cmd = CommandParser.parse("look @#$%");
            assertNotNull(cmd);
            assertEquals("look", cmd.getVerb());
        }

        @Test
        @DisplayName("Mixed case commands are normalized")
        void mixedCaseIsNormalized() {
            CommandParser.Command cmd1 = CommandParser.parse("LOOK");
            CommandParser.Command cmd2 = CommandParser.parse("Look");
            CommandParser.Command cmd3 = CommandParser.parse("look");

            assertEquals(cmd1.getVerb(), cmd2.getVerb());
            assertEquals(cmd2.getVerb(), cmd3.getVerb());
        }

        @Test
        @DisplayName("Gibberish returns invalid command")
        void gibberishReturnsInvalid() {
            CommandParser.Command cmd = CommandParser.parse("xyzzy plugh");
            assertFalse(cmd.isValid());
        }
    }

    // ========================================================================
    // LOCATION EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Location Edge Cases")
    class LocationEdgeCases {

        @Test
        @DisplayName("Location with null ID throws exception")
        void nullIdThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> new Location(null, "Test"));
        }

        @Test
        @DisplayName("Location with empty ID throws exception")
        void emptyIdThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> new Location("", "Test"));
        }

        @Test
        @DisplayName("Location with null name throws exception")
        void nullNameThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> new Location("test", null));
        }

        @Test
        @DisplayName("Location with no exits returns empty set")
        void noExitsReturnsEmptySet() {
            Location loc = new Location("test", "Test");
            assertTrue(loc.getExits().isEmpty());
        }

        @Test
        @DisplayName("Getting nonexistent exit returns null")
        void getNonexistentExitReturnsNull() {
            Location loc = new Location("test", "Test");
            assertNull(loc.getExit("north"));
        }

        @Test
        @DisplayName("Location starts as not visited")
        void locationStartsNotVisited() {
            Location loc = new Location("test", "Test");
            assertFalse(loc.hasBeenVisited());
        }

        @Test
        @DisplayName("Marking visited works")
        void markingVisitedWorks() {
            Location loc = new Location("test", "Test");
            loc.markVisited();
            assertTrue(loc.hasBeenVisited());
        }

        @Test
        @DisplayName("Location starts unlocked by default")
        void locationStartsUnlocked() {
            Location loc = new Location("test", "Test");
            assertTrue(loc.isUnlocked());
        }

        @Test
        @DisplayName("Exit direction is case-insensitive")
        void exitDirectionCaseInsensitive() {
            Location loc = new Location("test", "Test");
            loc.addExit("NORTH", "other");

            assertEquals("other", loc.getExit("north"));
            assertEquals("other", loc.getExit("NORTH"));
            assertEquals("other", loc.getExit("North"));
        }
    }

    // ========================================================================
    // DICE EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Dice Edge Cases")
    class DiceEdgeCases {

        @BeforeEach
        void setUp() {
            Dice.clearRollHistory();
        }

        @Test
        @DisplayName("Rolling d1 always returns 1")
        void rollingD1AlwaysReturns1() {
            for (int i = 0; i < 100; i++) {
                assertEquals(1, Dice.roll(1));
            }
        }

        @Test
        @DisplayName("Rolling 0 dice throws IllegalArgumentException")
        void rollingZeroDiceThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.rollMultiple(0, 6));
        }

        @Test
        @DisplayName("Rolling negative dice throws IllegalArgumentException")
        void rollingNegativeDiceThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.rollMultiple(-5, 6));
        }

        @Test
        @DisplayName("Very large modifier is applied correctly")
        void veryLargeModifierApplied() {
            int result = Dice.rollWithModifier(6, 1000);
            assertTrue(result >= 1001 && result <= 1006);
        }

        @Test
        @DisplayName("Very negative modifier is applied correctly")
        void veryNegativeModifierApplied() {
            int result = Dice.rollWithModifier(6, -1000);
            assertTrue(result >= -999 && result <= -994);
        }

        @Test
        @DisplayName("Parse handles invalid notation gracefully")
        void parseHandlesInvalidNotation() {
            // Should not crash on invalid input
            assertThrows(Exception.class, () -> Dice.parse("invalid"));
        }

        @Test
        @DisplayName("Parse handles empty string")
        void parseHandlesEmptyString() {
            assertThrows(Exception.class, () -> Dice.parse(""));
        }
    }

    // ========================================================================
    // MONSTER EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Monster Edge Cases")
    class MonsterEdgeCases {

        @Test
        @DisplayName("Monster with 1 HP dies from 1 damage")
        void monsterWithOneHpDiesFromOneDamage() {
            Monster weak = new Monster("weak", "Weak Monster", 10, 1);
            weak.takeDamage(1);

            assertFalse(weak.isAlive());
        }

        @Test
        @DisplayName("Monster HP can be restored manually")
        void monsterHpCanBeRestored() {
            Monster monster = new Monster("test", "Test", 15, 20);
            monster.takeDamage(15);
            assertEquals(5, monster.getCurrentHitPoints());

            // Heal back to full
            monster.heal(15);
            assertEquals(20, monster.getCurrentHitPoints());
        }

        @Test
        @DisplayName("Monster with negative AC is allowed")
        void monsterWithNegativeAcIsAllowed() {
            Monster monster = new Monster("slow", "Slow Monster", -5, 10);
            assertEquals(-5, monster.getArmorClass());
        }

        @Test
        @DisplayName("Monster XP value calculation works")
        void monsterXpValueWorks() {
            Monster monster = new Monster("goblin", "Goblin", 13, 7);
            monster.setChallengeRating(0.25);

            assertTrue(monster.getExperienceValue() > 0);
        }
    }
}
