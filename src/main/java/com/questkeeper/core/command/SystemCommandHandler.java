package com.questkeeper.core.command;

import com.questkeeper.character.NPC;
import com.questkeeper.ui.Display;

import java.util.List;
import java.util.Set;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles system commands: help, quit.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class SystemCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of("help", "quit");

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        return switch (verb.toLowerCase()) {
            case "help" -> handleHelp(context);
            case "quit" -> handleQuit(context);
            default -> CommandResult.failure("Unknown system command: " + verb);
        };
    }

    private CommandResult handleHelp(GameContext context) {
        // If in conversation, show dialogue-specific help with topics
        if (context.isInDialogue()) {
            Display.println();
            NPC currentNpc = context.getDialogueSystem().getCurrentNpc();
            List<String> topics = context.getDialogueSystem().getAvailableTopics();

            Display.println(Display.colorize("Talking to " + currentNpc.getName(), CYAN));
            if (topics != null && !topics.isEmpty()) {
                Display.println(Display.colorize("Topics: ", WHITE) + String.join(", ", topics));
            }

            // Show buy command if NPC is a shopkeeper
            if (currentNpc.isShopkeeper()) {
                Display.println(Display.colorize("Commands: ", WHITE) + "ask about <topic>, buy <item>, bye");
            } else {
                Display.println(Display.colorize("Commands: ", WHITE) + "ask about <topic>, bye");
            }
            Display.println();
        } else {
            Display.showHelp();
        }

        return CommandResult.success();
    }

    private CommandResult handleQuit(GameContext context) {
        Display.println();
        Display.println("Are you sure you want to quit? (y/n)");
        Display.showPrompt();
        String confirm = context.readInput().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            context.stopGame();
            return CommandResult.quit();
        }

        return CommandResult.success("Quit cancelled.");
    }
}
