package com.questkeeper.combat.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DurationType enum.
 */
@DisplayName("DurationType Enum Tests")
class DurationTypeTest {

    @Nested
    @DisplayName("usesRoundCounter Tests")
    class UsesRoundCounterTests {

        @Test
        @DisplayName("ROUNDS uses round counter")
        void roundsUsesRoundCounter() {
            assertTrue(DurationType.ROUNDS.usesRoundCounter());
        }

        @Test
        @DisplayName("UNTIL_END_OF_TURN does not use round counter")
        void untilEndOfTurnDoesNotUseRoundCounter() {
            assertFalse(DurationType.UNTIL_END_OF_TURN.usesRoundCounter());
        }

        @Test
        @DisplayName("UNTIL_START_OF_TURN does not use round counter")
        void untilStartOfTurnDoesNotUseRoundCounter() {
            assertFalse(DurationType.UNTIL_START_OF_TURN.usesRoundCounter());
        }

        @Test
        @DisplayName("UNTIL_SAVE does not use round counter")
        void untilSaveDoesNotUseRoundCounter() {
            assertFalse(DurationType.UNTIL_SAVE.usesRoundCounter());
        }

        @Test
        @DisplayName("PERMANENT does not use round counter")
        void permanentDoesNotUseRoundCounter() {
            assertFalse(DurationType.PERMANENT.usesRoundCounter());
        }

        @Test
        @DisplayName("INDEFINITE does not use round counter")
        void indefiniteDoesNotUseRoundCounter() {
            assertFalse(DurationType.INDEFINITE.usesRoundCounter());
        }
    }

    @Nested
    @DisplayName("allowsSavingThrow Tests")
    class AllowsSavingThrowTests {

        @Test
        @DisplayName("UNTIL_SAVE allows saving throw")
        void untilSaveAllowsSavingThrow() {
            assertTrue(DurationType.UNTIL_SAVE.allowsSavingThrow());
        }

        @Test
        @DisplayName("ROUNDS does not allow saving throw by itself")
        void roundsDoesNotAllowSavingThrow() {
            assertFalse(DurationType.ROUNDS.allowsSavingThrow());
        }

        @Test
        @DisplayName("PERMANENT does not allow saving throw")
        void permanentDoesNotAllowSavingThrow() {
            assertFalse(DurationType.PERMANENT.allowsSavingThrow());
        }

        @Test
        @DisplayName("INDEFINITE does not allow saving throw")
        void indefiniteDoesNotAllowSavingThrow() {
            assertFalse(DurationType.INDEFINITE.allowsSavingThrow());
        }

        @Test
        @DisplayName("UNTIL_END_OF_TURN does not allow saving throw")
        void untilEndOfTurnDoesNotAllowSavingThrow() {
            assertFalse(DurationType.UNTIL_END_OF_TURN.allowsSavingThrow());
        }

        @Test
        @DisplayName("UNTIL_START_OF_TURN does not allow saving throw")
        void untilStartOfTurnDoesNotAllowSavingThrow() {
            assertFalse(DurationType.UNTIL_START_OF_TURN.allowsSavingThrow());
        }
    }

    @Nested
    @DisplayName("checksAtTurnStart Tests")
    class ChecksAtTurnStartTests {

        @Test
        @DisplayName("UNTIL_START_OF_TURN checks at turn start")
        void untilStartOfTurnChecksAtTurnStart() {
            assertTrue(DurationType.UNTIL_START_OF_TURN.checksAtTurnStart());
        }

        @Test
        @DisplayName("ROUNDS does not check at turn start")
        void roundsDoesNotCheckAtTurnStart() {
            assertFalse(DurationType.ROUNDS.checksAtTurnStart());
        }

        @Test
        @DisplayName("UNTIL_END_OF_TURN does not check at turn start")
        void untilEndOfTurnDoesNotCheckAtTurnStart() {
            assertFalse(DurationType.UNTIL_END_OF_TURN.checksAtTurnStart());
        }

        @Test
        @DisplayName("UNTIL_SAVE does not check at turn start")
        void untilSaveDoesNotCheckAtTurnStart() {
            assertFalse(DurationType.UNTIL_SAVE.checksAtTurnStart());
        }

        @Test
        @DisplayName("PERMANENT does not check at turn start")
        void permanentDoesNotCheckAtTurnStart() {
            assertFalse(DurationType.PERMANENT.checksAtTurnStart());
        }

        @Test
        @DisplayName("INDEFINITE does not check at turn start")
        void indefiniteDoesNotCheckAtTurnStart() {
            assertFalse(DurationType.INDEFINITE.checksAtTurnStart());
        }
    }

    @Nested
    @DisplayName("checksAtTurnEnd Tests")
    class ChecksAtTurnEndTests {

        @Test
        @DisplayName("ROUNDS checks at turn end")
        void roundsChecksAtTurnEnd() {
            assertTrue(DurationType.ROUNDS.checksAtTurnEnd());
        }

        @Test
        @DisplayName("UNTIL_END_OF_TURN checks at turn end")
        void untilEndOfTurnChecksAtTurnEnd() {
            assertTrue(DurationType.UNTIL_END_OF_TURN.checksAtTurnEnd());
        }

        @Test
        @DisplayName("UNTIL_SAVE checks at turn end")
        void untilSaveChecksAtTurnEnd() {
            assertTrue(DurationType.UNTIL_SAVE.checksAtTurnEnd());
        }

        @Test
        @DisplayName("UNTIL_START_OF_TURN does not check at turn end")
        void untilStartOfTurnDoesNotCheckAtTurnEnd() {
            assertFalse(DurationType.UNTIL_START_OF_TURN.checksAtTurnEnd());
        }

        @Test
        @DisplayName("PERMANENT does not check at turn end")
        void permanentDoesNotCheckAtTurnEnd() {
            assertFalse(DurationType.PERMANENT.checksAtTurnEnd());
        }

        @Test
        @DisplayName("INDEFINITE does not check at turn end")
        void indefiniteDoesNotCheckAtTurnEnd() {
            assertFalse(DurationType.INDEFINITE.checksAtTurnEnd());
        }
    }

    @Nested
    @DisplayName("getDescription Tests")
    class GetDescriptionTests {

        @Test
        @DisplayName("ROUNDS has correct description")
        void roundsHasCorrectDescription() {
            assertEquals("Lasts for a number of rounds", DurationType.ROUNDS.getDescription());
        }

        @Test
        @DisplayName("UNTIL_END_OF_TURN has correct description")
        void untilEndOfTurnHasCorrectDescription() {
            assertEquals("Lasts until end of turn", DurationType.UNTIL_END_OF_TURN.getDescription());
        }

        @Test
        @DisplayName("UNTIL_START_OF_TURN has correct description")
        void untilStartOfTurnHasCorrectDescription() {
            assertEquals("Lasts until start of next turn", DurationType.UNTIL_START_OF_TURN.getDescription());
        }

        @Test
        @DisplayName("UNTIL_SAVE has correct description")
        void untilSaveHasCorrectDescription() {
            assertEquals("Lasts until successful saving throw", DurationType.UNTIL_SAVE.getDescription());
        }

        @Test
        @DisplayName("PERMANENT has correct description")
        void permanentHasCorrectDescription() {
            assertEquals("Permanent until dispelled", DurationType.PERMANENT.getDescription());
        }

        @Test
        @DisplayName("INDEFINITE has correct description")
        void indefiniteHasCorrectDescription() {
            assertEquals("Lasts until removed", DurationType.INDEFINITE.getDescription());
        }

        @Test
        @DisplayName("all duration types have non-empty descriptions")
        void allTypesHaveDescriptions() {
            for (DurationType type : DurationType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Enum Coverage Tests")
    class EnumCoverageTests {

        @Test
        @DisplayName("enum has exactly 6 values")
        void enumHasSixValues() {
            assertEquals(6, DurationType.values().length);
        }

        @Test
        @DisplayName("valueOf works for all types")
        void valueOfWorksForAllTypes() {
            assertEquals(DurationType.ROUNDS, DurationType.valueOf("ROUNDS"));
            assertEquals(DurationType.UNTIL_END_OF_TURN, DurationType.valueOf("UNTIL_END_OF_TURN"));
            assertEquals(DurationType.UNTIL_START_OF_TURN, DurationType.valueOf("UNTIL_START_OF_TURN"));
            assertEquals(DurationType.UNTIL_SAVE, DurationType.valueOf("UNTIL_SAVE"));
            assertEquals(DurationType.PERMANENT, DurationType.valueOf("PERMANENT"));
            assertEquals(DurationType.INDEFINITE, DurationType.valueOf("INDEFINITE"));
        }

        @Test
        @DisplayName("ordinal values are sequential")
        void ordinalValuesAreSequential() {
            DurationType[] values = DurationType.values();
            for (int i = 0; i < values.length; i++) {
                assertEquals(i, values[i].ordinal());
            }
        }
    }
}
