package com.questkeeper.core.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandResult.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("CommandResult")
class CommandResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("success() creates successful result")
        void successCreatesSuccessfulResult() {
            CommandResult result = CommandResult.success();

            assertTrue(result.isSuccess());
            assertFalse(result.isFailure());
            assertFalse(result.hasMessage());
            assertFalse(result.shouldDisplayLocation());
        }

        @Test
        @DisplayName("success(message) includes message")
        void successWithMessageIncludesMessage() {
            CommandResult result = CommandResult.success("Done!");

            assertTrue(result.isSuccess());
            assertTrue(result.hasMessage());
            assertEquals("Done!", result.getMessage());
        }

        @Test
        @DisplayName("successWithLocationRefresh() sets display flag")
        void successWithLocationRefreshSetsFlag() {
            CommandResult result = CommandResult.successWithLocationRefresh();

            assertTrue(result.isSuccess());
            assertTrue(result.shouldDisplayLocation());
        }

        @Test
        @DisplayName("failure() creates failure result")
        void failureCreatesFailureResult() {
            CommandResult result = CommandResult.failure("Error occurred");

            assertTrue(result.isFailure());
            assertFalse(result.isSuccess());
            assertEquals("Error occurred", result.getMessage());
        }

        @Test
        @DisplayName("quit() creates quit result")
        void quitCreatesQuitResult() {
            CommandResult result = CommandResult.quit();

            assertTrue(result.shouldQuit());
            assertEquals(CommandResult.ResultType.QUIT_GAME, result.getType());
        }

        @Test
        @DisplayName("combatStarted() creates combat result")
        void combatStartedCreatesCombatResult() {
            CommandResult result = CommandResult.combatStarted();

            assertEquals(CommandResult.ResultType.COMBAT_STARTED, result.getType());
        }

        @Test
        @DisplayName("dialogueStarted() creates dialogue result")
        void dialogueStartedCreatesDialogueResult() {
            CommandResult result = CommandResult.dialogueStarted();

            assertEquals(CommandResult.ResultType.DIALOGUE_STARTED, result.getType());
        }
    }

    @Nested
    @DisplayName("ResultType")
    class ResultTypeTests {

        @Test
        @DisplayName("all result types are distinct")
        void allResultTypesDistinct() {
            CommandResult.ResultType[] types = CommandResult.ResultType.values();
            assertEquals(8, types.length);
        }
    }
}
