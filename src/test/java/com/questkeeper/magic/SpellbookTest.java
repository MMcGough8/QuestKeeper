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
}
