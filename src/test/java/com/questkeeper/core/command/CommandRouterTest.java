package com.questkeeper.core.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandRouter.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("CommandRouter")
class CommandRouterTest {

    private CommandRouter router;

    @BeforeEach
    void setUp() {
        router = new CommandRouter();
    }

    @Nested
    @DisplayName("Handler Registration")
    class RegistrationTests {

        @Test
        @DisplayName("registers handler and maps verbs")
        void registersHandlerAndMapsVerbs() {
            TestHandler handler = new TestHandler(Set.of("test", "demo"));
            router.registerHandler(handler);

            assertTrue(router.canHandle("test"));
            assertTrue(router.canHandle("demo"));
            assertFalse(router.canHandle("unknown"));
        }

        @Test
        @DisplayName("verb lookup is case insensitive")
        void verbLookupCaseInsensitive() {
            router.registerHandler(new TestHandler(Set.of("test")));

            assertTrue(router.canHandle("TEST"));
            assertTrue(router.canHandle("Test"));
            assertTrue(router.canHandle("test"));
        }

        @Test
        @DisplayName("getHandlers returns all registered handlers")
        void getHandlersReturnsAll() {
            TestHandler h1 = new TestHandler(Set.of("a"));
            TestHandler h2 = new TestHandler(Set.of("b"));
            router.registerHandler(h1);
            router.registerHandler(h2);

            assertEquals(2, router.getHandlers().size());
        }
    }

    @Nested
    @DisplayName("Routing")
    class RoutingTests {

        @Test
        @DisplayName("routes to correct handler")
        void routesToCorrectHandler() {
            TestHandler handler = new TestHandler(Set.of("test"));
            router.registerHandler(handler);

            CommandResult result = router.route(null, "test", "", "test");

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertTrue(handler.wasHandled());
        }

        @Test
        @DisplayName("returns null for unknown verb")
        void returnsNullForUnknownVerb() {
            router.registerHandler(new TestHandler(Set.of("test")));

            CommandResult result = router.route(null, "unknown", "", "unknown");

            assertNull(result);
        }

        @Test
        @DisplayName("passes correct parameters to handler")
        void passesCorrectParameters() {
            TestHandler handler = new TestHandler(Set.of("test"));
            router.registerHandler(handler);

            router.route(null, "test", "target", "test target now");

            assertEquals("test", handler.lastVerb);
            assertEquals("target", handler.lastNoun);
            assertEquals("test target now", handler.lastFullInput);
        }
    }

    @Nested
    @DisplayName("Default Router")
    class DefaultRouterTests {

        @Test
        @DisplayName("createDefault includes system handler")
        void createDefaultIncludesSystemHandler() {
            CommandRouter defaultRouter = CommandRouter.createDefault();

            assertTrue(defaultRouter.canHandle("help"));
            assertTrue(defaultRouter.canHandle("quit"));
        }

        @Test
        @DisplayName("createDefault includes inventory handler")
        void createDefaultIncludesInventoryHandler() {
            CommandRouter defaultRouter = CommandRouter.createDefault();

            assertTrue(defaultRouter.canHandle("inventory"));
            assertTrue(defaultRouter.canHandle("i"));
            assertTrue(defaultRouter.canHandle("stats"));
            assertTrue(defaultRouter.canHandle("equipment"));
        }

        @Test
        @DisplayName("createDefault includes item handler")
        void createDefaultIncludesItemHandler() {
            CommandRouter defaultRouter = CommandRouter.createDefault();

            assertTrue(defaultRouter.canHandle("take"));
            assertTrue(defaultRouter.canHandle("get"));
            assertTrue(defaultRouter.canHandle("drop"));
            assertTrue(defaultRouter.canHandle("equip"));
            assertTrue(defaultRouter.canHandle("unequip"));
            assertTrue(defaultRouter.canHandle("use"));
        }

        @Test
        @DisplayName("createDefault includes dialogue handler")
        void createDefaultIncludesDialogueHandler() {
            CommandRouter defaultRouter = CommandRouter.createDefault();

            assertTrue(defaultRouter.canHandle("talk"));
            assertTrue(defaultRouter.canHandle("ask"));
            assertTrue(defaultRouter.canHandle("buy"));
            assertTrue(defaultRouter.canHandle("bye"));
        }

        @Test
        @DisplayName("createDefault includes exploration handler")
        void createDefaultIncludesExplorationHandler() {
            CommandRouter defaultRouter = CommandRouter.createDefault();

            assertTrue(defaultRouter.canHandle("look"));
            assertTrue(defaultRouter.canHandle("go"));
            assertTrue(defaultRouter.canHandle("north"));
            assertTrue(defaultRouter.canHandle("n"));
            assertTrue(defaultRouter.canHandle("leave"));
            assertTrue(defaultRouter.canHandle("exit"));
        }
    }

    // Test helper class
    private static class TestHandler implements CommandHandler {
        private final Set<String> verbs;
        private boolean handled = false;
        String lastVerb;
        String lastNoun;
        String lastFullInput;

        TestHandler(Set<String> verbs) {
            this.verbs = verbs;
        }

        @Override
        public Set<String> getHandledVerbs() {
            return verbs;
        }

        @Override
        public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
            this.handled = true;
            this.lastVerb = verb;
            this.lastNoun = noun;
            this.lastFullInput = fullInput;
            return CommandResult.success();
        }

        boolean wasHandled() {
            return handled;
        }
    }
}
