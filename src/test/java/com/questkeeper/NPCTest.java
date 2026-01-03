package com.questkeeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the NPC class.
 * 
 * @author Marc McGough
 * @version 1.0
 */

@DisplayName("NPC")
class NPCTest {

    private NPC npc;

    @BeforeEach
    void setUp() {
        npc = new NPC("test_npc", "Test Character", "merchant", "friendly", "helpful");
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("creates NPC with all properties")
        void createsWithAllProperties() {
            NPC n = new NPC("guard", "Town Guard", "guard", "gruff", "suspicious");
            
            assertEquals("guard", n.getId());
            assertEquals("Town Guard", n.getName());
            assertEquals("guard", n.getRole());
            assertEquals("gruff", n.getVoice());
            assertEquals("suspicious", n.getPersonality());
        }

        @Test
        @DisplayName("creates NPC with minimal properties")
        void createsWithMinimalProperties() {
            NPC n = new NPC("simple", "Simple NPC");
            
            assertEquals("simple", n.getId());
            assertEquals("Simple NPC", n.getName());
            assertEquals("", n.getRole());
            assertEquals("", n.getVoice());
            assertEquals("", n.getPersonality());
        }

        @Test
        @DisplayName("throws exception for null ID")
        void throwsForNullId() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new NPC(null, "Name"));
        }

        @Test
        @DisplayName("throws exception for empty ID")
        void throwsForEmptyId() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new NPC("", "Name"));
        }

        @Test
        @DisplayName("throws exception for null name")
        void throwsForNullName() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new NPC("id", null));
        }

        @Test
        @DisplayName("throws exception for empty name")
        void throwsForEmptyName() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new NPC("id", ""));
        }

        @Test
        @DisplayName("handles null optional properties gracefully")
        void handlesNullOptionalProperties() {
            NPC n = new NPC("id", "Name", null, null, null);
            
            assertEquals("", n.getRole());
            assertEquals("", n.getVoice());
            assertEquals("", n.getPersonality());
        }

        @Test
        @DisplayName("NPC starts as not having met player")
        void startsNotMet() {
            assertFalse(npc.hasMetPlayer());
        }

        @Test
        @DisplayName("NPC starts as non-shopkeeper")
        void startsNonShopkeeper() {
            assertFalse(npc.isShopkeeper());
        }
    }

    @Nested
    @DisplayName("Core Properties")
    class CorePropertyTests {

        @Test
        @DisplayName("sets and gets description")
        void setsDescription() {
            npc.setDescription("A tall figure in a cloak.");
            assertEquals("A tall figure in a cloak.", npc.getDescription());
        }

        @Test
        @DisplayName("sets and gets locationId")
        void setsLocationId() {
            npc.setLocationId("tavern");
            assertEquals("tavern", npc.getLocationId());
        }

        @Test
        @DisplayName("locationId can be null")
        void locationIdCanBeNull() {
            npc.setLocationId("somewhere");
            npc.setLocationId(null);
            assertNull(npc.getLocationId());
        }

        @Test
        @DisplayName("sets and gets shopkeeper status")
        void setsShopkeeper() {
            npc.setShopkeeper(true);
            assertTrue(npc.isShopkeeper());
            
            npc.setShopkeeper(false);
            assertFalse(npc.isShopkeeper());
        }

        @Test
        @DisplayName("handles null description gracefully")
        void handlesNullDescription() {
            npc.setDescription(null);
            assertEquals("", npc.getDescription());
        }
    }

    @Nested
    @DisplayName("Greeting System")
    class GreetingTests {

        @BeforeEach
        void setUpGreetings() {
            npc.setGreeting("Hello, stranger!");
            npc.setReturnGreeting("Welcome back, friend!");
        }

        @Test
        @DisplayName("returns first greeting on initial meeting")
        void returnsFirstGreeting() {
            String greeting = npc.greet();
            assertEquals("Hello, stranger!", greeting);
        }

        @Test
        @DisplayName("marks as met after first greeting")
        void marksAsMetAfterGreet() {
            assertFalse(npc.hasMetPlayer());
            npc.greet();
            assertTrue(npc.hasMetPlayer());
        }

        @Test
        @DisplayName("returns return greeting on subsequent meetings")
        void returnsReturnGreeting() {
            npc.greet(); // First meeting
            String greeting = npc.greet(); // Second meeting
            assertEquals("Welcome back, friend!", greeting);
        }

        @Test
        @DisplayName("falls back to first greeting if no return greeting set")
        void fallsBackToFirstGreeting() {
            npc.setReturnGreeting("");
            npc.greet(); // First meeting
            String greeting = npc.greet(); // Second meeting
            assertEquals("Hello, stranger!", greeting);
        }

        @Test
        @DisplayName("peekGreeting does not mark as met")
        void peekDoesNotMark() {
            String peeked = npc.peekGreeting();
            assertEquals("Hello, stranger!", peeked);
            assertFalse(npc.hasMetPlayer());
        }

        @Test
        @DisplayName("peekGreeting returns appropriate greeting based on met status")
        void peekReturnsAppropriateGreeting() {
            assertEquals("Hello, stranger!", npc.peekGreeting());
            
            npc.markAsMet();
            assertEquals("Welcome back, friend!", npc.peekGreeting());
        }

        @Test
        @DisplayName("handles null greetings gracefully")
        void handlesNullGreetings() {
            npc.setGreeting(null);
            npc.setReturnGreeting(null);
            
            assertEquals("", npc.getGreeting());
            assertEquals("", npc.getReturnGreeting());
        }
    }

    @Nested
    @DisplayName("Dialogue System")
    class DialogueTests {

        @Test
        @DisplayName("adds and retrieves dialogue")
        void addsAndRetrievesDialogue() {
            npc.addDialogue("weather", "It's a fine day.");
            
            assertEquals("It's a fine day.", npc.getDialogue("weather"));
        }

        @Test
        @DisplayName("dialogue topics are case-insensitive")
        void dialogueCaseInsensitive() {
            npc.addDialogue("WEATHER", "It's a fine day.");
            
            assertEquals("It's a fine day.", npc.getDialogue("weather"));
            assertEquals("It's a fine day.", npc.getDialogue("Weather"));
            assertEquals("It's a fine day.", npc.getDialogue("WEATHER"));
        }

        @Test
        @DisplayName("hasDialogue checks correctly")
        void hasDialogueWorks() {
            npc.addDialogue("weather", "It's a fine day.");
            
            assertTrue(npc.hasDialogue("weather"));
            assertFalse(npc.hasDialogue("politics"));
        }

        @Test
        @DisplayName("returns null for unknown topic")
        void returnsNullForUnknownTopic() {
            assertNull(npc.getDialogue("unknown"));
        }

        @Test
        @DisplayName("returns null for null topic")
        void returnsNullForNullTopic() {
            assertNull(npc.getDialogue(null));
            assertFalse(npc.hasDialogue(null));
        }

        @Test
        @DisplayName("removes dialogue")
        void removesDialogue() {
            npc.addDialogue("weather", "It's a fine day.");
            npc.removeDialogue("weather");
            
            assertFalse(npc.hasDialogue("weather"));
        }

        @Test
        @DisplayName("returns available topics sorted")
        void returnsTopicsSorted() {
            npc.addDialogue("zebras", "Stripy horses.");
            npc.addDialogue("apples", "Red fruit.");
            npc.addDialogue("mayor", "Important person.");
            
            List<String> topics = npc.getAvailableTopics();
            
            assertEquals(3, topics.size());
            assertEquals("apples", topics.get(0));
            assertEquals("mayor", topics.get(1));
            assertEquals("zebras", topics.get(2));
        }

        @Test
        @DisplayName("getDialogueCount returns correct count")
        void getDialogueCountWorks() {
            assertEquals(0, npc.getDialogueCount());
            
            npc.addDialogue("topic1", "Response 1");
            npc.addDialogue("topic2", "Response 2");
            
            assertEquals(2, npc.getDialogueCount());
        }

        @Test
        @DisplayName("ignores null or empty topic additions")
        void ignoresInvalidTopics() {
            npc.addDialogue(null, "Response");
            npc.addDialogue("", "Response");
            npc.addDialogue("   ", "Response");
            npc.addDialogue("valid", null);
            
            assertEquals(0, npc.getDialogueCount());
        }

        @Test
        @DisplayName("updates existing dialogue")
        void updatesExistingDialogue() {
            npc.addDialogue("weather", "It's sunny.");
            npc.addDialogue("weather", "It's raining now.");
            
            assertEquals("It's raining now.", npc.getDialogue("weather"));
            assertEquals(1, npc.getDialogueCount());
        }
    }

    @Nested
    @DisplayName("Sample Lines")
    class SampleLineTests {

        @Test
        @DisplayName("adds sample lines")
        void addsSampleLines() {
            npc.addSampleLine("Hello there!");
            npc.addSampleLine("Fine weather, isn't it?");
            
            assertEquals(2, npc.getSampleLineCount());
            assertTrue(npc.getSampleLines().contains("Hello there!"));
        }

        @Test
        @DisplayName("getRandomSampleLine returns a line")
        void getRandomSampleLineWorks() {
            npc.addSampleLine("Only line");
            
            assertEquals("Only line", npc.getRandomSampleLine());
        }

        @Test
        @DisplayName("getRandomSampleLine returns empty string when no lines")
        void getRandomSampleLineEmpty() {
            assertEquals("", npc.getRandomSampleLine());
        }

        @Test
        @DisplayName("ignores null or empty sample lines")
        void ignoresInvalidSampleLines() {
            npc.addSampleLine(null);
            npc.addSampleLine("");
            npc.addSampleLine("   ");
            
            assertEquals(0, npc.getSampleLineCount());
        }

        @Test
        @DisplayName("sample lines list is unmodifiable")
        void sampleLinesUnmodifiable() {
            npc.addSampleLine("Test");
            
            assertThrows(UnsupportedOperationException.class, 
                    () -> npc.getSampleLines().add("Hacked!"));
        }
    }

    @Nested
    @DisplayName("Flag Management")
    class FlagTests {

        @Test
        @DisplayName("marks as met")
        void marksAsMet() {
            npc.markAsMet();
            assertTrue(npc.hasMetPlayer());
        }

        @Test
        @DisplayName("resets met status")
        void resetsMet() {
            npc.markAsMet();
            npc.resetMet();
            assertFalse(npc.hasMetPlayer());
        }

        @Test
        @DisplayName("sets custom flag")
        void setsCustomFlag() {
            npc.setFlag("quest_given");
            assertTrue(npc.hasFlag("quest_given"));
        }

        @Test
        @DisplayName("flags are case-insensitive")
        void flagsCaseInsensitive() {
            npc.setFlag("QUEST_GIVEN");
            
            assertTrue(npc.hasFlag("quest_given"));
            assertTrue(npc.hasFlag("Quest_Given"));
        }

        @Test
        @DisplayName("removes custom flag")
        void removesFlag() {
            npc.setFlag("quest_given");
            npc.removeFlag("quest_given");
            
            assertFalse(npc.hasFlag("quest_given"));
        }

        @Test
        @DisplayName("returns all flags")
        void returnsAllFlags() {
            npc.markAsMet();
            npc.setFlag("quest_given");
            
            assertTrue(npc.getFlags().contains("met_player"));
            assertTrue(npc.getFlags().contains("quest_given"));
        }

        @Test
        @DisplayName("hasFlag returns false for null")
        void hasFlagReturnsFalseForNull() {
            assertFalse(npc.hasFlag(null));
        }

        @Test
        @DisplayName("flags set is unmodifiable")
        void flagsUnmodifiable() {
            assertThrows(UnsupportedOperationException.class, 
                    () -> npc.getFlags().add("hacked"));
        }
    }

    @Nested
    @DisplayName("Roleplay Prompt")
    class RoleplayPromptTests {

        @Test
        @DisplayName("generates full roleplay prompt")
        void generatesFullPrompt() {
            String prompt = npc.getRoleplayPrompt();
            
            assertTrue(prompt.contains("merchant"));
            assertTrue(prompt.contains("friendly voice"));
            assertTrue(prompt.contains("helpful"));
        }

        @Test
        @DisplayName("handles missing properties in prompt")
        void handlesMissingProperties() {
            NPC simple = new NPC("id", "Name");
            
            assertEquals("", simple.getRoleplayPrompt());
        }

        @Test
        @DisplayName("handles partial properties in prompt")
        void handlesPartialProperties() {
            NPC partial = new NPC("id", "Name", "guard", "", "");
            
            assertEquals("guard", partial.getRoleplayPrompt());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("NPCs with same ID are equal")
        void sameIdEquals() {
            NPC npc1 = new NPC("guard", "Guard One");
            NPC npc2 = new NPC("guard", "Guard Two");
            
            assertEquals(npc1, npc2);
            assertEquals(npc1.hashCode(), npc2.hashCode());
        }

        @Test
        @DisplayName("NPCs with different IDs are not equal")
        void differentIdNotEquals() {
            NPC npc1 = new NPC("guard1", "Guard");
            NPC npc2 = new NPC("guard2", "Guard");
            
            assertNotEquals(npc1, npc2);
        }

        @Test
        @DisplayName("NPC not equal to null")
        void notEqualToNull() {
            assertNotEquals(null, npc);
        }

        @Test
        @DisplayName("NPC not equal to other types")
        void notEqualToOtherType() {
            assertNotEquals("test_npc", npc);
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("creates Norrin correctly")
        void createsNorrin() {
            NPC norrin = NPC.createNorrin();
            
            assertEquals("norrin_bard", norrin.getId());
            assertEquals("Norrin", norrin.getName());
            assertEquals("bard", norrin.getRole());
            assertEquals("drunken_dragon_inn", norrin.getLocationId());
            assertFalse(norrin.isShopkeeper());
            
            assertTrue(norrin.hasDialogue("mayor"));
            assertTrue(norrin.hasDialogue("rumors"));
            assertTrue(norrin.getSampleLineCount() > 0);
        }

        @Test
        @DisplayName("creates Mara correctly")
        void createsMara() {
            NPC mara = NPC.createMara();
            
            assertEquals("mara_bartender", mara.getId());
            assertEquals("Mara Ember", mara.getName());
            assertEquals("bartender", mara.getRole());
            assertEquals("drunken_dragon_inn", mara.getLocationId());
            assertFalse(mara.isShopkeeper());
            
            assertTrue(mara.hasDialogue("drinks"));
            assertTrue(mara.hasDialogue("mayor"));
        }

        @Test
        @DisplayName("creates Darius correctly")
        void createsDarius() {
            NPC darius = NPC.createDarius();
            
            assertEquals("darius_recluse", darius.getId());
            assertEquals("Darius", darius.getName());
            assertEquals("recluse", darius.getRole());
            
            assertTrue(darius.hasDialogue("machinist"));
            assertTrue(darius.hasDialogue("trials"));
        }

        @Test
        @DisplayName("creates Elara correctly")
        void createsElara() {
            NPC elara = NPC.createElara();
            
            assertEquals("elara_shopkeeper", elara.getId());
            assertEquals("Elara", elara.getName());
            assertEquals("shopkeeper", elara.getRole());
            assertEquals("clockwork_curios", elara.getLocationId());
            assertTrue(elara.isShopkeeper());
            
            assertTrue(elara.hasDialogue("shop"));
            assertTrue(elara.hasDialogue("backroom"));
        }

        @Test
        @DisplayName("factory NPCs have greetings set")
        void factoryNpcsHaveGreetings() {
            NPC norrin = NPC.createNorrin();
            NPC mara = NPC.createMara();
            
            assertFalse(norrin.getGreeting().isEmpty());
            assertFalse(norrin.getReturnGreeting().isEmpty());
            assertFalse(mara.getGreeting().isEmpty());
        }

        @Test
        @DisplayName("factory NPCs have descriptions set")
        void factoryNpcsHaveDescriptions() {
            NPC norrin = NPC.createNorrin();
            NPC elara = NPC.createElara();
            
            assertFalse(norrin.getDescription().isEmpty());
            assertFalse(elara.getDescription().isEmpty());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("formats toString correctly")
        void formatsCorrectly() {
            npc.addDialogue("topic1", "Response");
            npc.addDialogue("topic2", "Response");
            
            String str = npc.toString();
            
            assertTrue(str.contains("test_npc"));
            assertTrue(str.contains("Test Character"));
            assertTrue(str.contains("merchant"));
            assertTrue(str.contains("topics=2"));
        }
    }
}