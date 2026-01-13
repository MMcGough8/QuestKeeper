package com.questkeeper.dialogue;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.NPC;
import com.questkeeper.state.GameState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DialogueSystem class.
 *
 * @author Marc McGough
 */
@DisplayName("DialogueSystem Tests")
class DialogueSystemTest {

    @TempDir
    Path tempDir;

    private Path campaignDir;
    private Campaign campaign;
    private Character character;
    private GameState state;
    private DialogueSystem dialogueSystem;

    @BeforeEach
    void setUp() throws IOException {
        campaignDir = tempDir.resolve("test_campaign");
        Files.createDirectories(campaignDir);

        createCampaignYaml();
        createLocationsYaml();
        createNpcsYaml();

        campaign = Campaign.loadFromYaml(campaignDir);
        character = new Character("TestHero", Race.HUMAN, CharacterClass.FIGHTER);
        state = new GameState(character, campaign);
        dialogueSystem = new DialogueSystem();
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
                npcs:
                  - barkeep
                  - bard
              - id: town_square
                name: Town Square
                description: The center of town.
                exits:
                  south: tavern
                npcs: []
            """);
    }

    private void createNpcsYaml() throws IOException {
        Files.writeString(campaignDir.resolve("npcs.yaml"), """
            npcs:
              - id: barkeep
                name: Mara
                role: bartender
                greeting: "Welcome to my tavern! What can I get you?"
                return_greeting: "Back again? The usual?"
                dialogues:
                  drinks: "We have ale and wine."
                  rumors: "I've heard strange things about the clocktower."
                  mayor: "The mayor went missing last week."
              - id: bard
                name: Norrin
                role: bard
                greeting: "Ah, a new face! Let me sing you a tale."
                return_greeting: "My favorite listener returns!"
                dialogues:
                  songs: "I know many ballads."
                  clocktower: "That old tower holds dark secrets."
            """);
    }

    private void addConditionalDialogue() {
        // Add a conditional dialogue that requires a flag
        NPC mara = campaign.getNPC("barkeep");
        mara.addDialogue("secret", "The clocktower hides a terrible secret.", "completed_trial_1");
    }

    // ==========================================
    // Initial State Tests
    // ==========================================

    @Nested
    @DisplayName("Initial State")
    class InitialStateTests {

        @Test
        @DisplayName("starts not in conversation")
        void startsNotInConversation() {
            assertFalse(dialogueSystem.isInConversation());
            assertNull(dialogueSystem.getCurrentNpc());
        }
    }

    // ==========================================
    // Start Dialogue Tests
    // ==========================================

    @Nested
    @DisplayName("Start Dialogue")
    class StartDialogueTests {

        @Test
        @DisplayName("starts dialogue with NPC by name")
        void startsDialogueByName() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            assertTrue(result.isSuccess());
            assertEquals("Mara", result.getNpcName());
            assertTrue(result.getMessage().contains("Welcome"));
            assertTrue(dialogueSystem.isInConversation());
        }

        @Test
        @DisplayName("starts dialogue with NPC by ID")
        void startsDialogueById() {
            DialogueResult result = dialogueSystem.startDialogue(state, "barkeep");

            assertTrue(result.isSuccess());
            assertEquals("Mara", result.getNpcName());
        }

        @Test
        @DisplayName("starts dialogue with partial name match")
        void startsDialoguePartialMatch() {
            DialogueResult result = dialogueSystem.startDialogue(state, "mar");

            assertTrue(result.isSuccess());
            assertEquals("Mara", result.getNpcName());
        }

        @Test
        @DisplayName("is case-insensitive")
        void caseInsensitive() {
            DialogueResult result = dialogueSystem.startDialogue(state, "MARA");

            assertTrue(result.isSuccess());
            assertEquals("Mara", result.getNpcName());
        }

        @Test
        @DisplayName("sets met flag in game state")
        void setsMetFlag() {
            assertFalse(state.hasFlag("met_barkeep"));

            dialogueSystem.startDialogue(state, "Mara");

            assertTrue(state.hasFlag("met_barkeep"));
        }

        @Test
        @DisplayName("shows return greeting on second meeting")
        void showsReturnGreeting() {
            // First meeting
            DialogueResult first = dialogueSystem.startDialogue(state, "Mara");
            assertTrue(first.getMessage().contains("Welcome"));
            dialogueSystem.endDialogue();

            // Second meeting
            DialogueResult second = dialogueSystem.startDialogue(state, "Mara");
            assertTrue(second.getMessage().contains("Back again"));
        }

        @Test
        @DisplayName("includes available topics")
        void includesAvailableTopics() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            assertTrue(result.hasTopics());
            List<String> topics = result.getAvailableTopics();
            assertTrue(topics.contains("drinks"));
            assertTrue(topics.contains("rumors"));
            assertTrue(topics.contains("mayor"));
        }

        @Test
        @DisplayName("fails for NPC not at location")
        void failsForNpcNotAtLocation() {
            // Move to town square (no NPCs)
            state.move("north");

            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("no one called"));
        }

        @Test
        @DisplayName("fails for unknown NPC")
        void failsForUnknownNpc() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Ghost");

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("no one called"));
        }

        @Test
        @DisplayName("fails for null state")
        void failsForNullState() {
            DialogueResult result = dialogueSystem.startDialogue(null, "Mara");

            assertTrue(result.isError());
        }

        @Test
        @DisplayName("fails for empty NPC name")
        void failsForEmptyName() {
            DialogueResult result = dialogueSystem.startDialogue(state, "");

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("Who"));
        }
    }

    // ==========================================
    // Ask About Tests
    // ==========================================

    @Nested
    @DisplayName("Ask About Topic")
    class AskAboutTests {

        @BeforeEach
        void startConversation() {
            dialogueSystem.startDialogue(state, "Mara");
        }

        @Test
        @DisplayName("returns response for valid topic")
        void returnsResponseForValidTopic() {
            DialogueResult result = dialogueSystem.askAbout("drinks");

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("ale"));
            assertEquals("drinks", result.getTopic());
        }

        @Test
        @DisplayName("is case-insensitive")
        void caseInsensitive() {
            DialogueResult result = dialogueSystem.askAbout("DRINKS");

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("ale"));
        }

        @Test
        @DisplayName("returns no topic result for unknown topic")
        void returnsNoTopicForUnknown() {
            DialogueResult result = dialogueSystem.askAbout("dragons");

            assertEquals(DialogueResult.Type.NO_TOPIC, result.getType());
            assertTrue(result.getMessage().contains("doesn't seem to know"));
            assertTrue(result.hasTopics()); // Still shows available topics
        }

        @Test
        @DisplayName("still includes available topics in response")
        void includesTopicsInResponse() {
            DialogueResult result = dialogueSystem.askAbout("drinks");

            assertTrue(result.hasTopics());
        }

        @Test
        @DisplayName("fails when not in conversation")
        void failsWhenNotInConversation() {
            dialogueSystem.endDialogue();

            DialogueResult result = dialogueSystem.askAbout("drinks");

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("not talking"));
        }

        @Test
        @DisplayName("fails for empty topic")
        void failsForEmptyTopic() {
            DialogueResult result = dialogueSystem.askAbout("");

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("What"));
        }
    }

    // ==========================================
    // End Dialogue Tests
    // ==========================================

    @Nested
    @DisplayName("End Dialogue")
    class EndDialogueTests {

        @Test
        @DisplayName("ends conversation successfully")
        void endsConversation() {
            dialogueSystem.startDialogue(state, "Mara");
            assertTrue(dialogueSystem.isInConversation());

            DialogueResult result = dialogueSystem.endDialogue();

            assertEquals(DialogueResult.Type.ENDED, result.getType());
            assertFalse(dialogueSystem.isInConversation());
            assertNull(dialogueSystem.getCurrentNpc());
        }

        @Test
        @DisplayName("includes NPC name in farewell")
        void includesNpcNameInFarewell() {
            dialogueSystem.startDialogue(state, "Mara");

            DialogueResult result = dialogueSystem.endDialogue();

            assertTrue(result.getMessage().contains("Mara"));
        }

        @Test
        @DisplayName("fails when not in conversation")
        void failsWhenNotInConversation() {
            DialogueResult result = dialogueSystem.endDialogue();

            assertTrue(result.isError());
        }
    }

    // ==========================================
    // List NPCs Tests
    // ==========================================

    @Nested
    @DisplayName("List NPCs at Location")
    class ListNpcsTests {

        @Test
        @DisplayName("returns NPCs at current location")
        void returnsNpcsAtLocation() {
            List<NPC> npcs = dialogueSystem.getNpcsAtCurrentLocation(state);

            assertEquals(2, npcs.size());
            assertTrue(npcs.stream().anyMatch(n -> n.getName().equals("Mara")));
            assertTrue(npcs.stream().anyMatch(n -> n.getName().equals("Norrin")));
        }

        @Test
        @DisplayName("returns empty list when no NPCs at location")
        void returnsEmptyWhenNoNpcs() {
            state.move("north"); // Town square has no NPCs

            List<NPC> npcs = dialogueSystem.getNpcsAtCurrentLocation(state);

            assertTrue(npcs.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null state")
        void returnsEmptyForNullState() {
            List<NPC> npcs = dialogueSystem.getNpcsAtCurrentLocation(null);

            assertTrue(npcs.isEmpty());
        }
    }

    // ==========================================
    // Multiple NPC Tests
    // ==========================================

    @Nested
    @DisplayName("Multiple NPCs")
    class MultipleNpcTests {

        @Test
        @DisplayName("can switch between NPCs")
        void canSwitchNpcs() {
            // Talk to Mara
            dialogueSystem.startDialogue(state, "Mara");
            assertEquals("Mara", dialogueSystem.getCurrentNpc().getName());

            // End and talk to Norrin
            dialogueSystem.endDialogue();
            dialogueSystem.startDialogue(state, "Norrin");
            assertEquals("Norrin", dialogueSystem.getCurrentNpc().getName());
        }

        @Test
        @DisplayName("can start new conversation without ending")
        void canStartNewWithoutEnding() {
            dialogueSystem.startDialogue(state, "Mara");

            // Start talking to someone else (implicit end)
            DialogueResult result = dialogueSystem.startDialogue(state, "Norrin");

            assertTrue(result.isSuccess());
            assertEquals("Norrin", dialogueSystem.getCurrentNpc().getName());
        }
    }

    // ==========================================
    // Format Tests
    // ==========================================

    @Nested
    @DisplayName("Result Formatting")
    class FormatTests {

        @Test
        @DisplayName("formats greeting with topics")
        void formatsGreetingWithTopics() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            String formatted = result.format();
            assertTrue(formatted.contains("Mara:"));
            assertTrue(formatted.contains("Welcome"));
            assertTrue(formatted.contains("ask about"));
        }

        @Test
        @DisplayName("formats error without NPC name")
        void formatsErrorWithoutNpcName() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Ghost");

            String formatted = result.format();
            assertFalse(formatted.contains("Ghost:"));
        }
    }

    // ==========================================
    // Conditional Dialogue Tests
    // ==========================================

    @Nested
    @DisplayName("Conditional Dialogue Topics")
    class ConditionalDialogueTests {

        @BeforeEach
        void setUpConditionalDialogue() {
            addConditionalDialogue();
        }

        @Test
        @DisplayName("hides topic when flag not set")
        void hidesTopicWithoutFlag() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            assertFalse(result.getAvailableTopics().contains("secret"));
        }

        @Test
        @DisplayName("shows topic when flag is set")
        void showsTopicWithFlag() {
            state.setFlag("completed_trial_1");

            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            assertTrue(result.getAvailableTopics().contains("secret"));
        }

        @Test
        @DisplayName("cannot ask about hidden topic")
        void cannotAskHiddenTopic() {
            dialogueSystem.startDialogue(state, "Mara");

            DialogueResult result = dialogueSystem.askAbout("secret");

            assertEquals(DialogueResult.Type.NO_TOPIC, result.getType());
        }

        @Test
        @DisplayName("can ask about topic after flag set")
        void canAskAfterFlagSet() {
            state.setFlag("completed_trial_1");
            dialogueSystem.startDialogue(state, "Mara");

            DialogueResult result = dialogueSystem.askAbout("secret");

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("terrible secret"));
        }

        @Test
        @DisplayName("unconditional topics always available")
        void unconditionalTopicsAvailable() {
            DialogueResult result = dialogueSystem.startDialogue(state, "Mara");

            assertTrue(result.getAvailableTopics().contains("drinks"));
            assertTrue(result.getAvailableTopics().contains("rumors"));
            assertTrue(result.getAvailableTopics().contains("mayor"));
        }
    }
}
