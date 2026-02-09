package com.questkeeper.core.command;

/**
 * Result of executing a command.
 *
 * Contains success/failure status and optional messages.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class CommandResult {

    /**
     * Result type indicating what happened.
     */
    public enum ResultType {
        /** Command executed successfully */
        SUCCESS,
        /** Command failed due to invalid input or state */
        FAILURE,
        /** Command requires more input from user */
        NEEDS_INPUT,
        /** Command resulted in combat starting */
        COMBAT_STARTED,
        /** Command resulted in combat ending */
        COMBAT_ENDED,
        /** Command resulted in dialogue starting */
        DIALOGUE_STARTED,
        /** Command resulted in dialogue ending */
        DIALOGUE_ENDED,
        /** Command triggered game quit */
        QUIT_GAME,
        /** Player moved to a new location (triggers post-move checks) */
        PLAYER_MOVED
    }

    private final ResultType type;
    private final String message;
    private final boolean displayLocation;

    private CommandResult(ResultType type, String message, boolean displayLocation) {
        this.type = type;
        this.message = message;
        this.displayLocation = displayLocation;
    }

    // ==========================================
    // Factory Methods
    // ==========================================

    /**
     * Creates a successful result with no message.
     */
    public static CommandResult success() {
        return new CommandResult(ResultType.SUCCESS, null, false);
    }

    /**
     * Creates a successful result with a message.
     */
    public static CommandResult success(String message) {
        return new CommandResult(ResultType.SUCCESS, message, false);
    }

    /**
     * Creates a successful result that should refresh the location display.
     */
    public static CommandResult successWithLocationRefresh() {
        return new CommandResult(ResultType.SUCCESS, null, true);
    }

    /**
     * Creates a successful result with message and location refresh.
     */
    public static CommandResult successWithLocationRefresh(String message) {
        return new CommandResult(ResultType.SUCCESS, message, true);
    }

    /**
     * Creates a failure result with an error message.
     */
    public static CommandResult failure(String message) {
        return new CommandResult(ResultType.FAILURE, message, false);
    }

    /**
     * Creates a result indicating the command needs more input.
     */
    public static CommandResult needsInput(String prompt) {
        return new CommandResult(ResultType.NEEDS_INPUT, prompt, false);
    }

    /**
     * Creates a result indicating combat has started.
     */
    public static CommandResult combatStarted() {
        return new CommandResult(ResultType.COMBAT_STARTED, null, false);
    }

    /**
     * Creates a result indicating combat has ended.
     */
    public static CommandResult combatEnded(String message) {
        return new CommandResult(ResultType.COMBAT_ENDED, message, true);
    }

    /**
     * Creates a result indicating dialogue has started.
     */
    public static CommandResult dialogueStarted() {
        return new CommandResult(ResultType.DIALOGUE_STARTED, null, false);
    }

    /**
     * Creates a result indicating dialogue has ended.
     */
    public static CommandResult dialogueEnded() {
        return new CommandResult(ResultType.DIALOGUE_ENDED, null, false);
    }

    /**
     * Creates a result indicating the game should quit.
     */
    public static CommandResult quit() {
        return new CommandResult(ResultType.QUIT_GAME, null, false);
    }

    /**
     * Creates a result indicating the player moved to a new location.
     * This triggers post-move checks (trials, random encounters).
     */
    public static CommandResult playerMoved() {
        return new CommandResult(ResultType.PLAYER_MOVED, null, true);
    }

    // ==========================================
    // Accessors
    // ==========================================

    public ResultType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasMessage() {
        return message != null && !message.isEmpty();
    }

    public boolean shouldDisplayLocation() {
        return displayLocation;
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }

    public boolean isFailure() {
        return type == ResultType.FAILURE;
    }

    public boolean shouldQuit() {
        return type == ResultType.QUIT_GAME;
    }

    public boolean hasPlayerMoved() {
        return type == ResultType.PLAYER_MOVED;
    }
}
