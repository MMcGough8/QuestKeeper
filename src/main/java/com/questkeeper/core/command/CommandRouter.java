package com.questkeeper.core.command;

import com.questkeeper.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes commands to appropriate handlers.
 *
 * The router maintains a registry of command handlers and dispatches
 * incoming commands to the handler that can process them.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class CommandRouter {

    private final List<CommandHandler> handlers;
    private final Map<String, CommandHandler> verbToHandler;

    public CommandRouter() {
        this.handlers = new ArrayList<>();
        this.verbToHandler = new HashMap<>();
    }

    /**
     * Registers a command handler.
     *
     * @param handler the handler to register
     */
    public void registerHandler(CommandHandler handler) {
        handlers.add(handler);
        for (String verb : handler.getHandledVerbs()) {
            verbToHandler.put(verb.toLowerCase(), handler);
        }
    }

    /**
     * Checks if a verb can be handled by any registered handler.
     *
     * @param verb the command verb
     * @return true if a handler exists for this verb
     */
    public boolean canHandle(String verb) {
        return verbToHandler.containsKey(verb.toLowerCase());
    }

    /**
     * Routes a command to the appropriate handler.
     *
     * @param context the game context
     * @param verb the command verb
     * @param noun the command noun/target
     * @param fullInput the full original input
     * @return the result of handling the command, or null if no handler found
     */
    public CommandResult route(GameContext context, String verb, String noun, String fullInput) {
        CommandHandler handler = verbToHandler.get(verb.toLowerCase());

        if (handler == null) {
            return null; // No handler found - let GameEngine handle it
        }

        // Check if handler is available in current state
        if (!handler.isAvailable(context)) {
            Display.showError(handler.getUnavailableMessage(context));
            return CommandResult.failure(handler.getUnavailableMessage(context));
        }

        return handler.handle(context, verb, noun, fullInput);
    }

    /**
     * Gets all registered handlers.
     *
     * @return list of handlers
     */
    public List<CommandHandler> getHandlers() {
        return new ArrayList<>(handlers);
    }

    /**
     * Creates a router with the default set of handlers.
     *
     * @return configured command router
     */
    public static CommandRouter createDefault() {
        CommandRouter router = new CommandRouter();

        // Register handlers
        router.registerHandler(new SystemCommandHandler());
        router.registerHandler(new InventoryCommandHandler());
        router.registerHandler(new ItemCommandHandler());

        return router;
    }
}
