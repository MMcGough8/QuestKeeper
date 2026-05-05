package com.questkeeper.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        
        @Test
        @DisplayName("parse returns empty command for empty string")
        void parseEmptyStringReturnsEmpty() {
            Command cmd = CommandParser.parse("");
            assertTrue(cmd.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("parse returns empty command for whitespace input")
        void parseWhitespaceReturnsEmpty(String input) {
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
            "head, go",
            "run, go",
            "trek, go",
            "journey, go",
            "march, go",
            "dash, go",
            "sprint, go",
            "jog, go",
            "stride, go",
            "hike, go",
            "climb, go",
            "ascend, go",
            "descend, go",
            "swim, go",
            "dive, go",
            "leap, go",
            "wade, go",
            "crawl, go",
            "sneak, go",
            "submerge, go"
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
            "view, look",
            "observe, look",
            "search, look",
            "l, look",
            "peek, look",
            "peer, look",
            "glance, look",
            "scrutinize, look",
            "survey, look",
            "scan, look",
            "eye, look",
            "watch, look",
            "study, look",
            "ponder, look",
            "gaze, look",
            "regard, look",
            "decipher, look",
            "decode, look"
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
            "pickup, take",
            "collect, take",
            "seize, take",
            "snatch, take",
            "lift, take",
            "gather, take",
            "loot, take",
            "claim, take",
            "nab, take",
            "swipe, take",
            "retrieve, take",
            "fetch, take",
            "scoop, take",
            "pluck, take"
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
            "converse, talk",
            "greet, talk",
            "hail, talk",
            "address, talk",
            "approach, talk",
            "parley, talk",
            "confer, talk",
            "whisper, talk"
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
            "question, ask",
            "query, ask",
            "interrogate, ask",
            "demand, ask",
            "request, ask",
            "petition, ask",
            "beseech, ask"
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
            "kill, attack",
            "slay, attack",
            "smite, attack",
            "slash, attack",
            "stab, attack",
            "swing, attack",
            "parry, attack",
            "lunge, attack",
            "charge, attack",
            "pummel, attack",
            "bash, attack",
            "cleave, attack",
            "pierce, attack",
            "slice, attack",
            "rend, attack",
            "hack, attack",
            "dispatch, attack",
            "assault, attack",
            "maul, attack",
            "club, attack",
            "batter, attack",
            "thrust, attack"
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
            "bag, inventory",
            "pack, inventory",
            "pouch, inventory",
            "satchel, inventory",
            "kit, inventory"
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
            "commands, help",
            "hint, help",
            "h, help"
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
            "wield, equip",
            "hold, equip",
            "don, equip",
            "strap, equip",
            "brandish, equip",
            "draw, equip"
        })
        @DisplayName("Equip synonyms map to 'equip'")
        void equipSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " armor");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "cast, cast",
            "spell, cast",
            "invoke, cast",
            "channel, cast",
            "evoke, cast",
            "conjure, cast",
            "summon, cast",
            "weave, cast",
            "hex, cast",
            "banish, cast",
            "enchant, cast",
            "intone, cast",
            "incant, cast",
            "manifest, cast",
            "unleash, cast"
        })
        @DisplayName("Cast synonyms map to 'cast'")
        void castSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " spell");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "drop, drop",
            "discard, drop",
            "throw, drop",
            "toss, drop",
            "release, drop",
            "dump, drop",
            "ditch, drop",
            "abandon, drop",
            "jettison, drop",
            "scatter, drop"
        })
        @DisplayName("Drop synonyms map to 'drop'")
        void dropSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " sword");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "use, use",
            "activate, use",
            "apply, use",
            "operate, use",
            "manipulate, use",
            "push, use",
            "pull, use",
            "press, use",
            "turn, use",
            "twist, use",
            "rotate, use",
            "spin, use",
            "wind, use",
            "crank, use",
            "toggle, use",
            "flip, use",
            "adjust, use",
            "align, use",
            "calibrate, use",
            "redirect, use",
            "reset, use",
            "trigger, use",
            "resonate, use"
        })
        @DisplayName("Use synonyms map to 'use'")
        void useSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " gear");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "rest, rest",
            "sleep, rest",
            "camp, rest",
            "nap, rest",
            "meditate, rest",
            "recover, rest",
            "recuperate, rest",
            "snooze, rest"
        })
        @DisplayName("Rest synonyms map to 'rest'")
        void restSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "unequip, unequip",
            "remove, unequip",
            "doff, unequip",
            "stow, unequip",
            "sheathe, unequip",
            "holster, unequip"
        })
        @DisplayName("Unequip synonyms map to 'unequip'")
        void unequipSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " armor");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "read, read",
            "peruse, read",
            "browse, read"
        })
        @DisplayName("Read synonyms map to 'read'")
        void readSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " tome");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "bye, bye",
            "farewell, bye",
            "goodbye, bye",
            "later, bye",
            "adieu, bye",
            "ciao, bye"
        })
        @DisplayName("Bye synonyms map to 'bye'")
        void byeSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "trial, trial",
            "trials, trial",
            "challenge, trial",
            "puzzle, trial",
            "mission, trial",
            "ordeal, trial",
            "gauntlet, trial"
        })
        @DisplayName("Trial synonyms map to 'trial'")
        void trialSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "attempt, attempt",
            "try, attempt",
            "solve, attempt",
            "do, attempt",
            "tackle, attempt",
            "perform, attempt",
            "execute, attempt"
        })
        @DisplayName("Attempt synonyms map to 'attempt'")
        void attemptSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " athletics");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "stats, stats",
            "status, stats",
            "character, stats",
            "char, stats",
            "me, stats",
            "sheet, stats",
            "info, stats",
            "abilities, stats",
            "attributes, stats"
        })
        @DisplayName("Stats synonyms map to 'stats'")
        void statsSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "equipment, equipment",
            "equipped, equipment",
            "gear, equipment",
            "worn, equipment",
            "loadout, equipment",
            "getup, equipment",
            "outfit, equipment"
        })
        @DisplayName("Equipment synonyms map to 'equipment'")
        void equipmentSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "close, close",
            "shut, close",
            "seal, close"
        })
        @DisplayName("Close synonyms map to 'close'")
        void closeSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " door");

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "load, load",
            "restore, load",
            "reload, load"
        })
        @DisplayName("Load synonyms map to 'load'")
        void loadSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input);

            assertEquals(expected, cmd.getVerb());
        }

        @ParameterizedTest
        @CsvSource({
            "smite, attack",
            "slash, attack",
            "stab, attack",
            "cleave, attack",
            "parry, attack",
            "lunge, attack",
            "pummel, attack",
            "channel, cast",
            "invoke, cast",
            "evoke, cast",
            "conjure, cast",
            "summon, cast",
            "hex, cast",
            "banish, cast",
            "dive, go",
            "swim, go",
            "descend, go",
            "leap, go",
            "climb, go",
            "submerge, go",
            "wind, use",
            "align, use",
            "manipulate, use",
            "redirect, use",
            "crank, use",
            "resonate, use",
            "peruse, read",
            "decipher, look",
            "scrutinize, look",
            "greet, talk",
            "hail, talk",
            "parley, talk"
        })
        @DisplayName("Key D&D phrases resolve to expected canonical verbs")
        void keyDndPhrasesResolveCorrectly(String input, String expected) {
            Command cmd = CommandParser.parse(input + " target");

            assertEquals(expected, cmd.getVerb(),
                "Expected '" + input + "' to map to canonical '" + expected + "'");
        }

        @ParameterizedTest
        @CsvSource({
            "wind, use",
            "spin, use",
            "rotate, use",
            "crank, use",
            "manipulate, use",
            "reset, use",
            "trigger, use",
            "decipher, look",
            "resonate, use",
            "align, use",
            "redirect, use",
            "channel, cast",
            "leap, go",
            "ascend, go",
            "dive, go",
            "descend, go",
            "submerge, go",
            "swim, go",
            "wade, go",
            "seal, close",
            "whisper, talk"
        })
        @DisplayName("Campaign-flavor verbs map to expected canonical actions")
        void campaignFlavorSynonyms(String input, String expected) {
            Command cmd = CommandParser.parse(input + " target");

            assertEquals(expected, cmd.getVerb(),
                "Campaign verb '" + input + "' should map to canonical '" + expected + "'");
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

    @Nested
    @DisplayName("Phrasal verb handling")
    class PhrasalVerbTests {

        @Test
        @DisplayName("'pick up X' parses as take X")
        void pickUpParsesAsTake() {
            CommandParser.Command cmd = CommandParser.parse("pick up the mask");
            assertEquals("take", cmd.getVerb());
            assertEquals("mask", cmd.getNoun());
        }

        @Test
        @DisplayName("'set down X' parses as drop X")
        void setDownParsesAsDrop() {
            CommandParser.Command cmd = CommandParser.parse("set down the sword");
            assertEquals("drop", cmd.getVerb());
            assertEquals("sword", cmd.getNoun());
        }

        @Test
        @DisplayName("'put down X' parses as drop X")
        void putDownParsesAsDrop() {
            CommandParser.Command cmd = CommandParser.parse("put down lantern");
            assertEquals("drop", cmd.getVerb());
            assertEquals("lantern", cmd.getNoun());
        }

        @Test
        @DisplayName("'pickup X' (no space) also parses as take X")
        void pickupNoSpaceParsesAsTake() {
            CommandParser.Command cmd = CommandParser.parse("pickup mask");
            assertEquals("take", cmd.getVerb());
            assertEquals("mask", cmd.getNoun());
        }

        @Test
        @DisplayName("'pick' alone still parses as take with single-word noun")
        void plainPickStillWorks() {
            CommandParser.Command cmd = CommandParser.parse("pick mask");
            assertEquals("take", cmd.getVerb());
            assertEquals("mask", cmd.getNoun());
        }
    }
}