package com.questkeeper.core.command;

import java.util.Set;

/**
 * Interface for command handlers in the game engine.
 *
 * Each handler is responsible for processing one or more related commands.
 * Handlers receive the game context and command details, and return a result
 * indicating success or failure.
 *
 * @author Marc McGough
 * @version 1.0
 */
public interface CommandHandler {

    /**
     * Gets the set of verbs this handler can process.
     *
     * @return set of command verbs (lowercase)
     */
    Set<String> getHandledVerbs();

    /**
     * Checks if this handler can process the given verb.
     *
     * @param verb the command verb to check
     * @return true if this handler can process the verb
     */
    default boolean canHandle(String verb) {
        return getHandledVerbs().contains(verb.toLowerCase());
    }

    /**
     * Handles a command.
     *
     * @param context the game context
     * @param verb the command verb
     * @param noun the command noun/target (may be empty)
     * @param fullInput the full original input string
     * @return the result of handling the command
     */
    CommandResult handle(GameContext context, String verb, String noun, String fullInput);

    /**
     * Checks if this handler can be used in the current game state.
     * Override to restrict when certain commands are available.
     *
     * @param context the game context
     * @return true if commands can be processed
     */
    default boolean isAvailable(GameContext context) {
        return true;
    }

    /**
     * Gets a message explaining why the handler is unavailable.
     * Only called when isAvailable returns false.
     *
     * @param context the game context
     * @return explanation message
     */
    default String getUnavailableMessage(GameContext context) {
        return "That command is not available right now.";
    }
}
