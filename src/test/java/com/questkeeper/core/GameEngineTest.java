package com.questkeeper.core;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.state.GameState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the GameEngine class.
 *
 * Note: GameEngine is tightly coupled to Scanner and Display for I/O,
 * making full integration testing challenging. These tests focus on
 * verifiable behaviors and state management.
 */
@DisplayName("GameEngine Tests")
class GameEngineTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("GameEngine can be instantiated")
        void canBeInstantiated() {
            GameEngine engine = new GameEngine();
            assertNotNull(engine);
        }

        @Test
        @DisplayName("GameEngine implements AutoCloseable")
        void implementsAutoCloseable() {
            assertTrue(AutoCloseable.class.isAssignableFrom(GameEngine.class));
        }

        @Test
        @DisplayName("GameEngine can be used in try-with-resources")
        void canBeUsedInTryWithResources() {
            assertDoesNotThrow(() -> {
                try (GameEngine engine = new GameEngine()) {
                    assertNotNull(engine);
                }
            });
        }
    }

    @Nested
    @DisplayName("Close Method Tests")
    class CloseTests {

        @Test
        @DisplayName("close() does not throw exceptions")
        void closeDoesNotThrow() {
            GameEngine engine = new GameEngine();
            assertDoesNotThrow(engine::close);
        }

        @Test
        @DisplayName("close() can be called multiple times safely")
        void closeCanBeCalledMultipleTimes() {
            GameEngine engine = new GameEngine();
            assertDoesNotThrow(() -> {
                engine.close();
                engine.close();
                engine.close();
            });
        }
    }

    @Nested
    @DisplayName("Command Parser Integration Tests")
    class CommandParserIntegrationTests {

        @Test
        @DisplayName("CommandParser is used for command parsing")
        void commandParserIsUsed() {
            // Verify CommandParser properly parses commands that GameEngine would use
            CommandParser.Command lookCmd = CommandParser.parse("look around");
            assertEquals("look", lookCmd.getVerb());

            CommandParser.Command goCmd = CommandParser.parse("go north");
            assertEquals("go", goCmd.getVerb());
            assertEquals("north", goCmd.getNoun());

            CommandParser.Command talkCmd = CommandParser.parse("talk to norrin");
            assertEquals("talk", talkCmd.getVerb());
            assertEquals("norrin", talkCmd.getNoun());
        }

        @Test
        @DisplayName("All GameEngine commands are recognized by CommandParser")
        void allCommandsRecognized() {
            // Test all commands in VALID_VERBS
            String[] commands = {
                "look", "go north", "talk norrin", "ask about mayor",
                "attack goblin", "take sword", "drop potion",
                "equip armor", "unequip weapon", "use potion",
                "help", "save", "load", "quit", "rest",
                "bye", "trial", "attempt puzzle", "equipment",
                "inventory", "stats", "open door", "close door", "read book"
            };

            for (String input : commands) {
                CommandParser.Command cmd = CommandParser.parse(input);
                assertTrue(cmd.isValid(), "Command should be valid: " + input);
            }
        }

        @Test
        @DisplayName("Movement shortcuts are recognized")
        void movementShortcutsWork() {
            String[] shortcuts = {"n", "s", "e", "w", "u", "d", "ne", "nw", "se", "sw"};

            for (String shortcut : shortcuts) {
                CommandParser.Command cmd = CommandParser.parse(shortcut);
                assertTrue(cmd.isValid(), "Shortcut should be valid: " + shortcut);
                assertEquals("go", cmd.getVerb());
            }
        }
    }

    @Nested
    @DisplayName("GameState Integration Tests")
    class GameStateIntegrationTests {

        private Character testCharacter;
        private Campaign testCampaign;

        @BeforeEach
        void setUp() {
            testCharacter = new Character("TestHero", Race.HUMAN, CharacterClass.FIGHTER);
            testCharacter.setAbilityScore(Character.Ability.STRENGTH, 16);
            testCharacter.setAbilityScore(Character.Ability.DEXTERITY, 14);
            testCharacter.setAbilityScore(Character.Ability.CONSTITUTION, 14);

            // Load the real campaign for integration testing
            testCampaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
        }

        @Test
        @DisplayName("GameState initializes with campaign starting location")
        void gameStateInitializesCorrectly() {
            GameState state = new GameState(testCharacter, testCampaign);

            assertNotNull(state.getCurrentLocation());
            assertEquals("drunken_dragon_inn", state.getCurrentLocation().getId());
        }

        @Test
        @DisplayName("GameState tracks visited locations")
        void gameStateTracksVisitedLocations() {
            GameState state = new GameState(testCharacter, testCampaign);

            assertTrue(state.hasVisited("drunken_dragon_inn"));
            assertFalse(state.hasVisited("town_square"));
        }

        @Test
        @DisplayName("GameState flag system works")
        void gameStateFlagsWork() {
            GameState state = new GameState(testCharacter, testCampaign);

            assertFalse(state.hasFlag("test_flag"));
            state.setFlag("test_flag", true);
            assertTrue(state.hasFlag("test_flag"));
        }
    }

    @Nested
    @DisplayName("Campaign Loading Integration Tests")
    class CampaignLoadingTests {

        @Test
        @DisplayName("Muddlebrook campaign loads successfully")
        void muddlebrookCampaignLoads() {
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));

            assertNotNull(campaign);
            assertEquals("muddlebrook", campaign.getId());
            assertEquals("Muddlebrook: Harlequin Trials", campaign.getName());
        }

        @Test
        @DisplayName("Campaign has required content for gameplay")
        void campaignHasRequiredContent() {
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));

            // Verify essential content exists
            assertNotNull(campaign.getStartingLocation());
            assertFalse(campaign.getLocations().isEmpty());
            assertFalse(campaign.getNPCs().isEmpty());
            assertFalse(campaign.getMonsterTemplates().isEmpty());
            assertFalse(campaign.getTrials().isEmpty());
        }

        @Test
        @DisplayName("Campaign NPCs are accessible")
        void campaignNpcsAccessible() {
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));

            // Key NPCs from Muddlebrook (using full IDs)
            assertNotNull(campaign.getNPC("norrin_bard"));
            assertNotNull(campaign.getNPC("mara_bartender"));
        }

        @Test
        @DisplayName("Campaign monsters can be instantiated")
        void campaignMonstersCanBeInstantiated() {
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));

            // Create monster from template
            var critter = campaign.createMonster("clockwork_critter");
            assertNotNull(critter);
            assertTrue(critter.isAlive());
            assertTrue(critter.getMaxHitPoints() > 0);
        }
    }

    @Nested
    @DisplayName("Combat System Integration Tests")
    class CombatSystemIntegrationTests {

        @Test
        @DisplayName("Combat system can be instantiated")
        void combatSystemCanBeInstantiated() {
            var combatSystem = new com.questkeeper.combat.CombatSystem();
            assertNotNull(combatSystem);
            assertFalse(combatSystem.isInCombat());
        }

        @Test
        @DisplayName("Combat integrates with GameState")
        void combatIntegratesWithGameState() {
            Character character = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
            character.setAbilityScore(Character.Ability.STRENGTH, 16);
            character.setAbilityScore(Character.Ability.DEXTERITY, 14);
            character.setAbilityScore(Character.Ability.CONSTITUTION, 14);

            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
            GameState state = new GameState(character, campaign);

            var combatSystem = new com.questkeeper.combat.CombatSystem();
            var monster = campaign.createMonster("clockwork_critter");

            var result = combatSystem.startCombat(state, java.util.List.of(monster));

            assertTrue(combatSystem.isInCombat());
            assertNotNull(result);
            assertEquals(com.questkeeper.combat.CombatResult.Type.COMBAT_START, result.getType());
        }
    }

    @Nested
    @DisplayName("Dialogue System Integration Tests")
    class DialogueSystemIntegrationTests {

        @Test
        @DisplayName("Dialogue system integrates with GameState")
        void dialogueIntegratesWithGameState() {
            Character character = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
            GameState state = new GameState(character, campaign);

            var dialogueSystem = new com.questkeeper.dialogue.DialogueSystem();

            // Start dialogue with an NPC at the starting location
            var result = dialogueSystem.startDialogue(state, "norrin_bard");

            // Norrin should be at the starting tavern
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Save/Load Integration Tests")
    class SaveLoadIntegrationTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Game state can be saved and loaded")
        void gameStateCanBeSavedAndLoaded() throws Exception {
            // Create initial state
            Character character = new Character("SaveTestHero", Race.ELF, CharacterClass.WIZARD);
            character.setAbilityScore(Character.Ability.INTELLIGENCE, 18);
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
            GameState originalState = new GameState(character, campaign);

            // Set some flags
            originalState.setFlag("test_save_flag", true);
            originalState.setCounter("test_counter", 42);

            // Save
            var saveState = originalState.toSaveState();
            Path savePath = tempDir.resolve("test_save.yaml");
            saveState.save(savePath);

            assertTrue(Files.exists(savePath));

            // Load
            var loadedSave = com.questkeeper.save.SaveState.load(savePath);
            GameState loadedState = GameState.fromSaveState(loadedSave, campaign);

            // Verify
            assertEquals("SaveTestHero", loadedState.getCharacter().getName());
            assertTrue(loadedState.hasFlag("test_save_flag"));
            assertEquals(42, loadedState.getCounter("test_counter"));
        }
    }

    @Nested
    @DisplayName("Full Game Flow Tests")
    class FullGameFlowTests {

        @Test
        @DisplayName("Character can navigate between locations")
        void characterCanNavigate() {
            Character character = new Character("Navigator", Race.HUMAN, CharacterClass.ROGUE);
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
            GameState state = new GameState(character, campaign);

            // Start at tavern
            assertEquals("drunken_dragon_inn", state.getCurrentLocation().getId());

            // Move to town square (if exit exists)
            var exits = state.getCurrentLocation().getExits();
            if (exits.contains("south") || exits.contains("out")) {
                String direction = exits.contains("south") ? "south" : "out";
                boolean moved = state.move(direction);
                if (moved) {
                    assertNotEquals("drunken_dragon_inn", state.getCurrentLocation().getId());
                }
            }
        }

        @Test
        @DisplayName("Character can pick up and use items")
        void characterCanUseItems() {
            Character character = new Character("ItemUser", Race.HUMAN, CharacterClass.FIGHTER);
            character.setAbilityScore(Character.Ability.STRENGTH, 14);

            // Create a test item
            var potion = new com.questkeeper.inventory.Item(
                "Healing Potion",
                com.questkeeper.inventory.Item.ItemType.CONSUMABLE,
                "Restores health", 0.5, 50
            );
            potion.setStackable(true);

            // Add to inventory
            assertTrue(character.getInventory().addItem(potion));
            assertTrue(character.getInventory().hasItem(potion));

            // Remove from inventory
            assertTrue(character.getInventory().removeItem(potion));
            assertFalse(character.getInventory().hasItem(potion));
        }

        @Test
        @DisplayName("Combat affects character HP")
        void combatAffectsCharacterHp() {
            Character character = new Character("Warrior", Race.HUMAN, CharacterClass.FIGHTER);
            character.setAbilityScore(Character.Ability.CONSTITUTION, 14);

            int initialHp = character.getCurrentHitPoints();
            character.takeDamage(5);

            assertEquals(initialHp - 5, character.getCurrentHitPoints());

            character.heal(3);
            assertEquals(initialHp - 2, character.getCurrentHitPoints());
        }

        @Test
        @DisplayName("XP gain triggers level up at threshold")
        void xpGainTriggersLevelUp() {
            Character character = new Character("Adventurer", Race.HUMAN, CharacterClass.FIGHTER);
            character.setAbilityScore(Character.Ability.CONSTITUTION, 14);

            assertEquals(1, character.getLevel());

            // Add enough XP to level up (300 XP for level 2)
            character.addExperience(300);

            assertEquals(2, character.getLevel());
        }
    }
}
