package com.questkeeper.magic;

import com.questkeeper.character.Character.CharacterClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellbookTest {

    @Nested
    @DisplayName("Default spells gated by class level")
    class DefaultSpellsTests {

        @Test
        @DisplayName("Paladin at Lvl 1 has no spells (cure_wounds gated at Lvl 2)")
        void paladinAtLvl1HasNoSpells() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.PALADIN, 1);

            assertEquals(0, book.getKnownSpells().size(),
                "Paladin at Lvl 1 should know zero spells");
        }

        @Test
        @DisplayName("Paladin built at Lvl 2 has cure_wounds")
        void paladinAtLvl2HasCureWounds() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.PALADIN, 2);

            assertTrue(book.getKnownSpells().stream()
                .anyMatch(s -> "cure_wounds".equals(s.getId())),
                "Paladin built at Lvl 2 should know cure_wounds");
        }

        @Test
        @DisplayName("Wizard at Lvl 1 has fire_bolt cantrip and 1st-level spells")
        void wizardAtLvl1HasDefaults() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.WIZARD, 1);

            assertTrue(book.getKnownCantrips().stream()
                .anyMatch(s -> "fire_bolt".equals(s.getId())),
                "Wizard at Lvl 1 should know fire_bolt");
            assertTrue(book.getKnownSpells().stream()
                .anyMatch(s -> "magic_missile".equals(s.getId())),
                "Wizard at Lvl 1 should know magic_missile");
        }
    }

    @Nested
    @DisplayName("Level-up should re-grant default spells gated by level")
    class LevelUpDefaultSpellsTests {

        @Test
        @DisplayName("Paladin leveled from 1 to 2 gains cure_wounds")
        void paladinLeveledFrom1To2GainsCureWounds() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.PALADIN, 1);
            assertEquals(0, book.getKnownSpells().size(),
                "precondition: Paladin at Lvl 1 has no spells");

            book.onLevelUp(2);

            assertTrue(book.getKnownSpells().stream()
                .anyMatch(s -> "cure_wounds".equals(s.getId())),
                "Paladin should know cure_wounds after leveling to 2");
        }

        @Test
        @DisplayName("Ranger leveled from 1 to 2 gains cure_wounds")
        void rangerLeveledFrom1To2GainsCureWounds() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.RANGER, 1);
            assertEquals(0, book.getKnownSpells().size(),
                "precondition: Ranger at Lvl 1 has no spells");

            book.onLevelUp(2);

            assertTrue(book.getKnownSpells().stream()
                .anyMatch(s -> "cure_wounds".equals(s.getId())),
                "Ranger should know cure_wounds after leveling to 2");
        }

        @Test
        @DisplayName("Repeated onLevelUp calls do not duplicate spells")
        void repeatedLevelUpDoesNotDuplicate() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.PALADIN, 2);
            int initialCount = book.getKnownSpells().size();

            book.onLevelUp(3);
            book.onLevelUp(4);
            book.onLevelUp(5);

            assertEquals(initialCount, book.getKnownSpells().size(),
                "Repeated level-up should not duplicate the cure_wounds entry");
        }

        @Test
        @DisplayName("onLevelUp on a non-caster (Fighter) does nothing")
        void onLevelUpOnNonCasterIsNoOp() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.FIGHTER, 1);

            assertDoesNotThrow(() -> book.onLevelUp(2));
            assertEquals(0, book.getKnownSpells().size());
            assertEquals(0, book.getKnownCantrips().size());
        }
    }

    @Nested
    @DisplayName("Spell slot table updates on level-up")
    class SlotTableUpdateTests {

        @Test
        @DisplayName("Half-caster leveled from 1 to 2 gains 2 first-level slots")
        void halfCasterLeveledTo2GainsSlots() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.PALADIN, 1);
            assertEquals(0, book.getSpellSlots().getSlotsRemaining(1),
                "precondition: Paladin at Lvl 1 has no slots");

            book.onLevelUp(2);

            assertEquals(2, book.getSpellSlots().getSlotsRemaining(1),
                "Paladin should have 2 first-level slots after leveling to 2");
        }

        @Test
        @DisplayName("Full-caster leveled from 2 to 3 gains a 2nd-level slot")
        void fullCasterLeveledTo3GainsSecondLevelSlot() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.WIZARD, 2);
            assertEquals(0, book.getSpellSlots().getSlotsRemaining(2),
                "precondition: Wizard at Lvl 2 has no 2nd-level slots");

            book.onLevelUp(3);

            assertEquals(2, book.getSpellSlots().getSlotsRemaining(2),
                "Wizard should have 2 second-level slots after leveling to 3");
        }

        @Test
        @DisplayName("Full-caster L3 -> L4 gains a 2nd-level slot (2 -> 3)")
        void fullCasterL3ToL4GainsSecondLevelSlot() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.WIZARD, 3);
            assertEquals(2, book.getSpellSlots().getSlotsRemaining(2),
                "precondition: Wizard L3 has 2 second-level slots");

            book.onLevelUp(4);

            assertEquals(3, book.getSpellSlots().getSlotsRemaining(2),
                "Wizard L4 should have 3 second-level slots");
        }

        @Test
        @DisplayName("Full-caster L4 -> L5 gains 2 third-level slots (0 -> 2)")
        void fullCasterL4ToL5GainsThirdLevelSlots() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.WIZARD, 4);
            assertEquals(0, book.getSpellSlots().getSlotsRemaining(3),
                "precondition: Wizard L4 has no 3rd-level slots");

            book.onLevelUp(5);

            assertEquals(2, book.getSpellSlots().getSlotsRemaining(3),
                "Wizard L5 should have 2 third-level slots");
        }

        @Test
        @DisplayName("Half-caster L4 -> L5 gains a 1st-level slot (3 -> 4) and 2 new 2nd-level slots")
        void halfCasterL4ToL5GainsSlots() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.PALADIN, 4);
            assertEquals(3, book.getSpellSlots().getSlotsRemaining(1),
                "precondition: Paladin L4 has 3 first-level slots");
            assertEquals(0, book.getSpellSlots().getSlotsRemaining(2),
                "precondition: Paladin L4 has no 2nd-level slots");

            book.onLevelUp(5);

            assertEquals(4, book.getSpellSlots().getSlotsRemaining(1),
                "Paladin L5 should have 4 first-level slots");
            assertEquals(2, book.getSpellSlots().getSlotsRemaining(2),
                "Paladin L5 should have 2 second-level slots");
        }

        @Test
        @DisplayName("Already-expended slots are not refilled on level-up")
        void levelUpDoesNotRefillUsedSlots() {
            Spellbook book = new Spellbook();
            book.initializeForClass(CharacterClass.WIZARD, 2);
            book.getSpellSlots().expendSlot(1);
            int firstLevelBefore = book.getSpellSlots().getSlotsRemaining(1);

            book.onLevelUp(3);

            int firstLevelAfter = book.getSpellSlots().getSlotsRemaining(1);
            assertEquals(firstLevelBefore + (4 - 3), firstLevelAfter,
                "Lvl 2->3 wizard 1st-level slot count should grow by (newMax - oldMax)=1, not refill the spent one");
        }
    }
}
