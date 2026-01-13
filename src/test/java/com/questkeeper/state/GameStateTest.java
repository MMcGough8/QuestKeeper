package com.questkeeper.state;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.save.SaveState;
import com.questkeeper.world.Location;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the GameState class.
 *
 * @author Marc McGough
 */
@DisplayName("GameState Tests")
class GameStateTest {

    @TempDir
    Path tempDir;

    private Path campaignDir;
    private Campaign campaign;
    private Character character;

    @BeforeEach
    void setUp() throws IOException {
        campaignDir = tempDir.resolve("test_campaign");
        Files.createDirectories(campaignDir);

        // Create minimal campaign files
        createCampaignYaml();
        createLocationsYaml();

        campaign = Campaign.loadFromYaml(campaignDir);

        character = new Character("TestHero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    private void createCampaignYaml() throws IOException {
        Files.writeString(campaignDir.resolve("campaign.yaml"), """
            id: test_campaign
            name: Test Campaign
            author: Test
            version: "1.0"
            starting_location: tavern
            """);
    }

    private void createLocationsYaml() throws IOException {
        Files.writeString(campaignDir.resolve("locations.yaml"), """
            locations:
              - id: tavern
                name: The Tavern
                description: A cozy tavern.
                exits:
                  north: town_square
                  cellar: cellar
              - id: town_square
                name: Town Square
                description: The center of town.
                exits:
                  south: tavern
                  east: shop
              - id: shop
                name: General Shop
                description: A small shop.
                exits:
                  west: town_square
              - id: cellar
                name: Dark Cellar
                description: A dark cellar.
                flags:
                  - locked
            """);
    }

    // ==========================================
    // Constructor Tests
    // ==========================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("creates GameState with character and campaign")
        void createsWithCharacterAndCampaign() {
            GameState state = new GameState(character, campaign);

            assertNotNull(state.getStateId());
            assertEquals(character, state.getCharacter());
            assertEquals(campaign, state.getCampaign());
            assertEquals("test_campaign", state.getCampaignId());
        }

        @Test
        @DisplayName("sets starting location automatically")
        void setsStartingLocation() {
            GameState state = new GameState(character, campaign);

            assertNotNull(state.getCurrentLocation());
            assertEquals("tavern", state.getCurrentLocationId());
            assertTrue(state.hasVisited("tavern"));
        }

        @Test
        @DisplayName("throws on null character")
        void throwsOnNullCharacter() {
            assertThrows(NullPointerException.class, () ->
                    new GameState(null, campaign));
        }

        @Test
        @DisplayName("throws on null campaign")
        void throwsOnNullCampaign() {
            assertThrows(NullPointerException.class, () ->
                    new GameState(character, null));
        }
    }

    // ==========================================
    // Location Tests
    // ==========================================

    @Nested
    @DisplayName("Location Management")
    class LocationTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = new GameState(character, campaign);
        }

        @Test
        @DisplayName("moves to valid location")
        void movesToValidLocation() {
            assertTrue(state.move("north"));
            assertEquals("town_square", state.getCurrentLocationId());
            assertTrue(state.hasVisited("town_square"));
        }

        @Test
        @DisplayName("fails to move in invalid direction")
        void failsInvalidDirection() {
            assertFalse(state.move("west"));
            assertEquals("tavern", state.getCurrentLocationId());
        }

        @Test
        @DisplayName("fails to move to locked location")
        void failsLockedLocation() {
            assertFalse(state.move("cellar"));
            assertEquals("tavern", state.getCurrentLocationId());
        }

        @Test
        @DisplayName("tracks visited locations")
        void tracksVisitedLocations() {
            state.move("north");
            state.move("east");

            assertTrue(state.hasVisited("tavern"));
            assertTrue(state.hasVisited("town_square"));
            assertTrue(state.hasVisited("shop"));
            assertEquals(3, state.getVisitedLocations().size());
        }

        @Test
        @DisplayName("can move back to previous location")
        void movesBackToPrevious() {
            state.move("north");
            assertEquals("town_square", state.getCurrentLocationId());

            state.move("south");
            assertEquals("tavern", state.getCurrentLocationId());
        }

        @Test
        @DisplayName("move is case-insensitive")
        void moveIsCaseInsensitive() {
            assertTrue(state.move("NORTH"));
            assertEquals("town_square", state.getCurrentLocationId());
        }
    }

    // ==========================================
    // Flag Tests
    // ==========================================

    @Nested
    @DisplayName("Flag Management")
    class FlagTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = new GameState(character, campaign);
        }

        @Test
        @DisplayName("sets and checks flags")
        void setsAndChecksFlags() {
            assertFalse(state.hasFlag("met_npc"));

            state.setFlag("met_npc");
            assertTrue(state.hasFlag("met_npc"));
        }

        @Test
        @DisplayName("clears flags")
        void clearsFlags() {
            state.setFlag("temporary_flag");
            assertTrue(state.hasFlag("temporary_flag"));

            state.clearFlag("temporary_flag");
            assertFalse(state.hasFlag("temporary_flag"));
        }

        @Test
        @DisplayName("flags are case-insensitive")
        void flagsAreCaseInsensitive() {
            state.setFlag("MET_NPC");
            assertTrue(state.hasFlag("met_npc"));
            assertTrue(state.hasFlag("MET_NPC"));
        }

        @Test
        @DisplayName("gets all flags")
        void getsAllFlags() {
            state.setFlag("flag1");
            state.setFlag("flag2");
            state.setFlag("flag3");

            assertEquals(3, state.getFlags().size());
            assertTrue(state.getFlags().contains("flag1"));
        }

        @Test
        @DisplayName("handles null flag gracefully")
        void handlesNullFlag() {
            state.setFlag(null);
            assertFalse(state.hasFlag(null));
        }
    }

    // ==========================================
    // Counter Tests
    // ==========================================

    @Nested
    @DisplayName("Counter Management")
    class CounterTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = new GameState(character, campaign);
        }

        @Test
        @DisplayName("sets and gets counters")
        void setsAndGetsCounters() {
            assertEquals(0, state.getCounter("gold"));

            state.setCounter("gold", 100);
            assertEquals(100, state.getCounter("gold"));
        }

        @Test
        @DisplayName("increments counters")
        void incrementsCounters() {
            state.incrementCounter("kills");
            state.incrementCounter("kills");
            state.incrementCounter("kills");

            assertEquals(3, state.getCounter("kills"));
        }

        @Test
        @DisplayName("decrements counters")
        void decrementsCounters() {
            state.setCounter("lives", 5);
            state.decrementCounter("lives");
            state.decrementCounter("lives");

            assertEquals(3, state.getCounter("lives"));
        }

        @Test
        @DisplayName("counters are case-insensitive")
        void countersAreCaseInsensitive() {
            state.setCounter("GOLD", 50);
            assertEquals(50, state.getCounter("gold"));
        }
    }

    // ==========================================
    // Variable Tests
    // ==========================================

    @Nested
    @DisplayName("Variable Management")
    class VariableTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = new GameState(character, campaign);
        }

        @Test
        @DisplayName("sets and gets variables")
        void setsAndGetsVariables() {
            assertNull(state.getVariable("current_quest"));

            state.setVariable("current_quest", "find_mayor");
            assertEquals("find_mayor", state.getVariable("current_quest"));
        }

        @Test
        @DisplayName("clears variable with null value")
        void clearsVariableWithNull() {
            state.setVariable("temp", "value");
            assertEquals("value", state.getVariable("temp"));

            state.setVariable("temp", null);
            assertNull(state.getVariable("temp"));
        }

        @Test
        @DisplayName("variables are case-insensitive")
        void variablesAreCaseInsensitive() {
            state.setVariable("PLAYER_CHOICE", "option_a");
            assertEquals("option_a", state.getVariable("player_choice"));
        }
    }

    // ==========================================
    // Trial Tests
    // ==========================================

    @Nested
    @DisplayName("Trial Management")
    class TrialTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = new GameState(character, campaign);
        }

        @Test
        @DisplayName("completes and tracks trials")
        void completesAndTrackTrials() {
            assertFalse(state.hasCompletedTrial("trial_01"));

            state.completeTrial("trial_01");

            assertTrue(state.hasCompletedTrial("trial_01"));
            assertTrue(state.hasFlag("completed_trial_01"));
            assertTrue(state.getCompletedTrials().contains("trial_01"));
        }

        @Test
        @DisplayName("tracks multiple completed trials")
        void tracksMultipleTrials() {
            state.completeTrial("trial_01");
            state.completeTrial("trial_02");
            state.completeTrial("trial_03");

            assertEquals(3, state.getCompletedTrials().size());
        }
    }

    // ==========================================
    // Quest Tests
    // ==========================================

    @Nested
    @DisplayName("Quest Management")
    class QuestTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = new GameState(character, campaign);
        }

        @Test
        @DisplayName("starts and tracks quests")
        void startsAndTracksQuests() {
            assertFalse(state.isQuestActive("find_mayor"));

            state.startQuest("find_mayor");

            assertTrue(state.isQuestActive("find_mayor"));
            assertTrue(state.hasFlag("started_find_mayor"));
        }

        @Test
        @DisplayName("completes quests")
        void completesQuests() {
            state.startQuest("find_mayor");
            assertTrue(state.isQuestActive("find_mayor"));

            state.completeQuest("find_mayor");

            assertFalse(state.isQuestActive("find_mayor"));
            assertTrue(state.hasFlag("completed_find_mayor"));
        }

        @Test
        @DisplayName("does not duplicate active quests")
        void noDuplicateQuests() {
            state.startQuest("find_mayor");
            state.startQuest("find_mayor");
            state.startQuest("find_mayor");

            assertEquals(1, state.getActiveQuests().size());
        }
    }

    // ==========================================
    // SaveState Conversion Tests
    // ==========================================

    @Nested
    @DisplayName("SaveState Conversion")
    class SaveStateConversionTests {

        @Test
        @DisplayName("converts to SaveState")
        void convertsToSaveState() {
            GameState state = new GameState(character, campaign);
            state.move("north");
            state.setFlag("met_npc");
            state.setCounter("gold", 100);
            state.setVariable("choice", "option_a");
            state.completeTrial("trial_01");

            SaveState save = state.toSaveState();

            assertEquals("test_campaign", save.getCampaignId());
            assertEquals("town_square", save.getCurrentLocationId());
            assertTrue(save.hasFlag("met_npc"));
            assertEquals(100, save.getCounter("gold"));
            assertEquals("option_a", save.getString("choice"));
        }

        @Test
        @DisplayName("restores from SaveState")
        void restoresFromSaveState() {
            // Create original state with progress
            GameState original = new GameState(character, campaign);
            original.move("north");
            original.setFlag("met_npc");
            original.setCounter("gold", 100);
            original.completeTrial("trial_01");

            // Convert to SaveState and back
            SaveState save = original.toSaveState();
            GameState restored = GameState.fromSaveState(save, campaign);

            // Verify restoration
            assertEquals("town_square", restored.getCurrentLocationId());
            assertTrue(restored.hasFlag("met_npc"));
            assertEquals(100, restored.getCounter("gold"));
            assertTrue(restored.hasVisited("tavern"));
            assertTrue(restored.hasVisited("town_square"));
        }

        @Test
        @DisplayName("preserves character data through save/load")
        void preservesCharacterData() {
            character.takeDamage(5);

            GameState original = new GameState(character, campaign);
            SaveState save = original.toSaveState();
            GameState restored = GameState.fromSaveState(save, campaign);

            assertEquals(character.getName(), restored.getCharacter().getName());
            assertEquals(character.getRace(), restored.getCharacter().getRace());
            assertEquals(character.getCharacterClass(), restored.getCharacter().getCharacterClass());
        }
    }

    // ==========================================
    // Play Time Tests
    // ==========================================

    @Nested
    @DisplayName("Play Time Tracking")
    class PlayTimeTests {

        @Test
        @DisplayName("tracks play time")
        void tracksPlayTime() {
            GameState state = new GameState(character, campaign);

            // Play time should be very small but positive
            long playTime = state.getTotalPlayTimeSeconds();
            assertTrue(playTime >= 0);
        }

        @Test
        @DisplayName("formats play time correctly")
        void formatsPlayTime() {
            GameState state = new GameState(character, campaign);

            String formatted = state.getFormattedPlayTime();
            assertNotNull(formatted);
            assertTrue(formatted.contains("m")); // Should have minutes
        }
    }

    // ==========================================
    // toString Tests
    // ==========================================

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("returns meaningful string representation")
        void returnsMeaningfulString() {
            GameState state = new GameState(character, campaign);
            state.setFlag("test_flag");

            String str = state.toString();

            assertTrue(str.contains("TestHero"));
            assertTrue(str.contains("test_campaign"));
            assertTrue(str.contains("The Tavern"));
        }
    }
}
