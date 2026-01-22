package com.questkeeper.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.questkeeper.core.CommandParser.Command;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the CommandParser utility class.
 * 
 * @author Marc McGough
 */
class CommandParserTest {
    
    // ========================================================================
    // BASIC PARSING TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Basic Parsing Tests")
    class BasicParsingTests {
        
        @Test
        @DisplayName("parse returns empty command for null input")
        void parseNullReturnsEmpty() {
            Command cmd = CommandParser.parse(null);
            
            assertTrue(cmd.isEmpty());
            assertNull(cmd.getVerb());
            assertNull(cmd.getNoun());
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("parse returns empty command for blank input")
        void parseBlankReturnsEmpty(String input) {
            Command cmd = CommandParser.parse(input);
            
            assertTrue(cmd.isEmpty());
        }
        
        @Test
        @DisplayName("parse extracts verb only when no noun")
        void parseVerbOnly() {
            Command cmd = CommandParser.parse("look");
            
            assertEquals("look", cmd.getVerb());
            assertNull(cmd.getNoun());
            assertTrue(cmd.isValid());
            assertFalse(cmd.hasNoun());
        }
        
        @Test
        @DisplayName("parse extracts verb and noun")
        void parseVerbAndNoun() {
            Command cmd = CommandParser.parse("take sword");
            
            assertEquals("take", cmd.getVerb());
            assertEquals("sword", cmd.getNoun());
            assertTrue(cmd.isValid());
            assertTrue(cmd.hasNoun());
        }
        
        @Test
        @DisplayName("parse handles multi-word nouns")
        void parseMultiWordNoun() {
            Command cmd = CommandParser.parse("take rusty sword");
            
            assertEquals("take", cmd.getVerb());
            assertEquals("rusty sword", cmd.getNoun());
        }
        
        @Test
        @DisplayName("parse preserves original input")
        void parsePreservesOriginal() {
            Command cmd = CommandParser.parse("GO North");
            
            assertEquals("GO North", cmd.getOriginalInput());
        }
        
        @Test
        @DisplayName("parse is case insensitive")
        void parseCaseInsensitive() {
            Command cmd = CommandParser.parse("LOOK");
            
            assertEquals("look", cmd.getVerb());
        }
        
        @Test
        @DisplayName("parse trims whitespace")
        void parseTrimsWhitespace() {
            Command cmd = CommandParser.parse("  look  around  ");
            
            assertEquals("look", cmd.getVerb());
            assertEquals("around", cmd.getNoun());
        }
    }
    
    // ========================================================================
    // SYNONYM TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Synonym Mapping Tests")
    @SuppressWarnings("java:S4144")
    class SynonymTests {
        
        @ParameterizedTest
        @CsvSource({
            "go, go",
            "walk, go",
            "move, go",
            "travel, go",
            "run, go"
        })
        @DisplayName("Movement synonyms map to 'go'")
        void movementSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " north");
            
            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "look, look",
            "examine, look",
            "inspect, look",
            "check, look",
            "l, look"
        })
        @DisplayName("Look synonyms map to 'look'")
        void lookSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "take, take",
            "get, take",
            "grab, take",
            "pick, take",
            "pickup, take"
        })
        @DisplayName("Take synonyms map to 'take'")
        void takeSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " item");
            
            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "talk, talk",
            "speak, talk",
            "chat, talk",
            "converse, talk"
        })
        @DisplayName("Talk synonyms map to 'talk'")
        void talkSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " npc");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "ask, ask",
            "inquire, ask",
            "question, ask"
        })
        @DisplayName("Ask synonyms map to 'ask'")
        void askSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " topic");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "attack, attack",
            "hit, attack",
            "strike, attack",
            "fight, attack",
            "kill, attack"
        })
        @DisplayName("Attack synonyms map to 'attack'")
        void attackSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " goblin");
            
            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "inventory, inventory",
            "inv, inventory",
            "i, inventory",
            "items, inventory",
            "bag, inventory"
        })
        @DisplayName("Inventory synonyms map to 'inventory'")
        void inventorySynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "quit, quit",
            "q, quit"
        })
        @DisplayName("Quit synonyms map to 'quit'")
        void quitSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "leave, leave",
            "exit, leave"
        })
        @DisplayName("Leave synonyms map to 'leave'")
        void leaveSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "help, help",
            "?, help",
            "commands, help"
        })
        @DisplayName("Help synonyms map to 'help'")
        void helpSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expected, cmd.getVerb());
        }
        
        @ParameterizedTest
        @CsvSource({
            "equip, equip",
            "wear, equip",
            "wield, equip"
        })
        @DisplayName("Equip synonyms map to 'equip'")
        void equipSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " armor");
            
            assertEquals(expected, cmd.getVerb());
        }
    }
    
    // ========================================================================
    // DIRECTION TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Direction Handling Tests")
    @SuppressWarnings("java:S4144")
    class DirectionTests {
        
        @ParameterizedTest
        @CsvSource({
            "go north, go, north",
            "go south, go, south",
            "go east, go, east",
            "go west, go, west",
            "go up, go, up",
            "go down, go, down"
        })
        @DisplayName("Full direction names work with go")
        void fullDirections(String input, String expectedVerb, String expectedNoun) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expectedVerb, cmd.getVerb());
            assertEquals(expectedNoun, cmd.getNoun());
        }
        
        @ParameterizedTest
        @CsvSource({
            "go n, go, north",
            "go s, go, south",
            "go e, go, east",
            "go w, go, west",
            "go u, go, up",
            "go d, go, down"
        })
        @DisplayName("Direction shortcuts expand to full names")
        void directionShortcuts(String input, String expectedVerb, String expectedNoun) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expectedVerb, cmd.getVerb());
            assertEquals(expectedNoun, cmd.getNoun());
        }
        
        @ParameterizedTest
        @CsvSource({
            "north, go, north",
            "south, go, south",
            "east, go, east",
            "west, go, west"
        })
        @DisplayName("Bare direction becomes 'go direction'")
        void bareDirection(String input, String expectedVerb, String expectedNoun) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expectedVerb, cmd.getVerb());
            assertEquals(expectedNoun, cmd.getNoun());
        }
        
        @ParameterizedTest
        @CsvSource({
            "n, go, north",
            "s, go, south",
            "e, go, east",
            "w, go, west"
        })
        @DisplayName("Bare direction shortcut becomes 'go direction'")
        void bareDirectionShortcut(String input, String expectedVerb, String expectedNoun) {
            Command cmd = CommandParser.parse(input);
            
            assertEquals(expectedVerb, cmd.getVerb());
            assertEquals(expectedNoun, cmd.getNoun());
        }
    }
    
    // ========================================================================
    // ARTICLE REMOVAL TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Article Removal Tests")
    class ArticleRemovalTests {
        
        @Test
        @DisplayName("Removes 'the' from noun")
        void removesThe() {
            Command cmd = CommandParser.parse("take the sword");
            
            assertEquals("sword", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Removes 'a' from noun")
        void removesA() {
            Command cmd = CommandParser.parse("take a potion");
            
            assertEquals("potion", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Removes 'an' from noun")
        void removesAn() {
            Command cmd = CommandParser.parse("take an apple");
            
            assertEquals("apple", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Removes preposition 'at' from noun")
        void removesAt() {
            Command cmd = CommandParser.parse("look at painting");
            
            assertEquals("painting", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Removes preposition 'to' from noun")
        void removesTo() {
            Command cmd = CommandParser.parse("talk to bartender");
            
            assertEquals("bartender", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Preserves article in middle of noun phrase")
        void preservesMiddleArticle() {
            Command cmd = CommandParser.parse("take sword of the king");
            
            assertEquals("sword of the king", cmd.getNoun());
        }
    }
    
    // ========================================================================
    // EXTRACT METHODS TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Extract Methods Tests")
    class ExtractMethodsTests {
        
        @Test
        @DisplayName("extractVerb returns first word")
        void extractVerbReturnsFirstWord() {
            assertEquals("look", CommandParser.extractVerb("look around"));
            assertEquals("go", CommandParser.extractVerb("go north"));
        }
        
        @Test
        @DisplayName("extractVerb returns null for empty input")
        void extractVerbNullForEmpty() {
            assertNull(CommandParser.extractVerb(null));
            assertNull(CommandParser.extractVerb(""));
        }
        
        @Test
        @DisplayName("extractNoun returns everything after first word")
        void extractNounReturnsRest() {
            assertEquals("north", CommandParser.extractNoun("go north"));
            assertEquals("rusty sword", CommandParser.extractNoun("take rusty sword"));
        }
        
        @Test
        @DisplayName("extractNoun returns null for single word")
        void extractNounNullForSingleWord() {
            assertNull(CommandParser.extractNoun("look"));
        }
        
        @Test
        @DisplayName("extractNoun returns null for empty input")
        void extractNounNullForEmpty() {
            assertNull(CommandParser.extractNoun(null));
            assertNull(CommandParser.extractNoun(""));
        }
    }
    
    // ========================================================================
    // UTILITY METHODS TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("isValidVerb returns true for synonyms")
        void isValidVerbForSynonyms() {
            assertTrue(CommandParser.isValidVerb("go"));
            assertTrue(CommandParser.isValidVerb("walk"));
            assertTrue(CommandParser.isValidVerb("look"));
            assertTrue(CommandParser.isValidVerb("examine"));
        }
        
        @Test
        @DisplayName("isValidVerb returns false for unknown verbs")
        void isValidVerbFalseForUnknown() {
            assertFalse(CommandParser.isValidVerb("dance"));
            assertFalse(CommandParser.isValidVerb("fly"));
            assertFalse(CommandParser.isValidVerb(null));
        }
        
        @Test
        @DisplayName("getCanonicalVerb normalizes synonyms")
        void getCanonicalVerbNormalizes() {
            assertEquals("go", CommandParser.getCanonicalVerb("walk"));
            assertEquals("look", CommandParser.getCanonicalVerb("examine"));
            assertEquals("take", CommandParser.getCanonicalVerb("grab"));
        }
        
        @Test
        @DisplayName("getValidVerbs returns all canonical verbs")
        void getValidVerbsReturnsAll() {
            var verbs = CommandParser.getValidVerbs();
            
            assertTrue(verbs.contains("go"));
            assertTrue(verbs.contains("look"));
            assertTrue(verbs.contains("take"));
            assertTrue(verbs.contains("attack"));
            assertTrue(verbs.contains("inventory"));
            assertTrue(verbs.contains("help"));
        }
    }
    
    // ========================================================================
    // COMMAND OBJECT TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Command Object Tests")
    class CommandObjectTests {
        
        @Test
        @DisplayName("Command.empty() creates empty command")
        void emptyCommandIsEmpty() {
            Command cmd = Command.empty();
            
            assertTrue(cmd.isEmpty());
            assertFalse(cmd.isValid());
            assertFalse(cmd.hasNoun());
        }
        
        @Test
        @DisplayName("isVerb checks verb match")
        void isVerbChecksMatch() {
            Command cmd = CommandParser.parse("go north");
            
            assertTrue(cmd.isVerb("go"));
            assertFalse(cmd.isVerb("look"));
            assertFalse(cmd.isVerb(null));
        }
        
        @Test
        @DisplayName("Command toString includes all fields")
        void toStringIncludesFields() {
            Command cmd = CommandParser.parse("take sword");
            String str = cmd.toString();
            
            assertTrue(str.contains("take"));
            assertTrue(str.contains("sword"));
        }
        
        @Test
        @DisplayName("Command equals compares verb and noun")
        void equalsComparesVerbAndNoun() {
            Command cmd1 = CommandParser.parse("go north");
            Command cmd2 = CommandParser.parse("walk north");
            Command cmd3 = CommandParser.parse("go south");
            
            assertEquals(cmd1, cmd2); // Both normalize to "go north"
            assertNotEquals(cmd1, cmd3); // Different noun
        }
        
        @Test
        @DisplayName("Command hashCode is consistent with equals")
        void hashCodeConsistentWithEquals() {
            Command cmd1 = CommandParser.parse("go north");
            Command cmd2 = CommandParser.parse("walk north");
            
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
        }
    }
    
    // ========================================================================
    // MUDDLEBROOK-SPECIFIC TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Muddlebrook Campaign Tests")
    class MuddlebrookTests {
        
        @Test
        @DisplayName("Can talk to Norrin")
        void talkToNorrin() {
            Command cmd = CommandParser.parse("talk to norrin");
            
            assertEquals("talk", cmd.getVerb());
            assertEquals("norrin", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Can examine the clocktower")
        void examineClockTower() {
            Command cmd = CommandParser.parse("examine the clocktower");
            
            assertEquals("look", cmd.getVerb());
            assertEquals("clocktower", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Can take Blinkstep Spark")
        void takeBlinkstepSpark() {
            Command cmd = CommandParser.parse("take blinkstep spark");
            
            assertEquals("take", cmd.getVerb());
            assertEquals("blinkstep spark", cmd.getNoun());
        }
        
        @Test
        @DisplayName("Can attack Clockwork Critter")
        void attackClockworkCritter() {
            Command cmd = CommandParser.parse("attack clockwork critter");

            assertEquals("attack", cmd.getVerb());
            assertEquals("clockwork critter", cmd.getNoun());
        }
    }

    // ========================================================================
    // ASK ABOUT PARSING TESTS
    // ========================================================================

    @Nested
    @DisplayName("parseAskAbout Tests")
    class ParseAskAboutTests {

        @Test
        @DisplayName("parses 'ask about topic' correctly")
        void parsesAskAboutTopic() {
            String[] result = CommandParser.parseAskAbout("ask about rumors");

            assertNull(result[0]); // No target
            assertEquals("rumors", result[1]);
        }

        @Test
        @DisplayName("parses 'about topic' correctly")
        void parsesAboutTopic() {
            String[] result = CommandParser.parseAskAbout("about mayor");

            assertNull(result[0]);
            assertEquals("mayor", result[1]);
        }

        @Test
        @DisplayName("parses 'ask npc about topic' correctly")
        void parsesAskNpcAboutTopic() {
            String[] result = CommandParser.parseAskAbout("ask mara about drinks");

            assertEquals("mara", result[0]);
            assertEquals("drinks", result[1]);
        }

        @Test
        @DisplayName("parses just topic correctly")
        void parsesJustTopic() {
            String[] result = CommandParser.parseAskAbout("clocktower");

            assertNull(result[0]);
            assertEquals("clocktower", result[1]);
        }

        @Test
        @DisplayName("handles null input")
        void handlesNull() {
            String[] result = CommandParser.parseAskAbout(null);

            assertNull(result[0]);
            assertNull(result[1]);
        }

        @Test
        @DisplayName("handles empty input")
        void handlesEmpty() {
            String[] result = CommandParser.parseAskAbout("");

            assertNull(result[0]);
            assertNull(result[1]);
        }

        @Test
        @DisplayName("is case-insensitive")
        void caseInsensitive() {
            String[] result = CommandParser.parseAskAbout("ASK MARA ABOUT RUMORS");

            assertEquals("mara", result[0]);
            assertEquals("rumors", result[1]);
        }
    }
}