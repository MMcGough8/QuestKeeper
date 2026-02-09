package com.questkeeper.core.command;

import com.questkeeper.character.NPC;
import com.questkeeper.ui.Display;
import com.questkeeper.world.Location;

import java.util.Set;

import static com.questkeeper.core.command.CommandUtils.*;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles exploration commands: look, go (+ directions), leave, exit.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class ExplorationCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of(
        "look",
        "go", "north", "south", "east", "west", "n", "s", "e", "w",
        "northeast", "northwest", "southeast", "southwest", "ne", "nw", "se", "sw",
        "up", "down", "u", "d",
        "leave", "exit"
    );

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        String lowerVerb = verb.toLowerCase();

        if (lowerVerb.equals("look")) {
            return handleLook(context, noun);
        } else if (lowerVerb.equals("leave") || lowerVerb.equals("exit")) {
            return handleLeave(context);
        } else if (lowerVerb.equals("go")) {
            return handleGo(context, noun);
        } else if (isDirection(lowerVerb)) {
            // Direction used as verb (e.g., "north" instead of "go north")
            return handleGo(context, lowerVerb);
        }

        return CommandResult.failure("Unknown exploration command: " + verb);
    }

    private CommandResult handleLook(GameContext context, String target) {
        if (target == null || target.isEmpty()) {
            // Looking at current location - return with location refresh
            return CommandResult.successWithLocationRefresh();
        }

        Location location = context.getCurrentLocation();

        // Check if looking at an NPC
        for (String npcId : location.getNpcs()) {
            NPC npc = context.getCampaign().getNPC(npcId);
            if (npc != null && matchesTarget(npc.getName(), target)) {
                Display.println();
                Display.println(Display.colorize(npc.getName(), CYAN));
                Display.println(npc.getDescription());
                Display.println();
                return CommandResult.success();
            }
        }

        // Check if looking at an item in the location
        for (String itemId : location.getItems()) {
            if (matchesTarget(itemId, target)) {
                var item = context.getCampaign().getItem(itemId);
                if (item != null) {
                    Display.println();
                    Display.println(Display.colorize(item.getName(), YELLOW));
                    Display.println(item.getDescription());
                    Display.println();
                    return CommandResult.success();
                }
            }
        }

        Display.showError("You don't see '" + target + "' here.");
        return CommandResult.failure("Target not found");
    }

    private CommandResult handleGo(GameContext context, String direction) {
        if (direction == null || direction.isEmpty()) {
            Display.showError("Go where? Specify a direction (north, south, east, west, etc.)");
            return CommandResult.failure("No direction specified");
        }

        // Strip common prefixes like "to the", "to", "the"
        direction = CommandUtils.normalizeDirection(direction);

        // Expand shorthand directions
        direction = expandDirection(direction);

        Location currentLocation = context.getCurrentLocation();
        if (!currentLocation.hasExit(direction)) {
            Display.showError("You can't go " + direction + " from here.");
            Display.println(buildExitsDisplay(currentLocation));
            return CommandResult.failure("No exit in that direction");
        }

        boolean moved = context.getGameState().move(direction);
        if (moved) {
            // End any active conversation when moving
            if (context.isInDialogue()) {
                context.getDialogueSystem().endDialogue();
            }
            // Return PLAYER_MOVED so GameEngine can handle post-move checks
            return CommandResult.playerMoved();
        } else {
            Location target = context.getCampaign().getLocation(currentLocation.getExit(direction));
            if (target != null && !context.getGameState().isLocationUnlocked(target.getId())) {
                String lockedMessage = target.getLockedMessage();
                Display.showError(lockedMessage);
            } else {
                Display.showError("You can't go that way.");
            }
            return CommandResult.failure("Movement blocked");
        }
    }

    private CommandResult handleLeave(GameContext context) {
        Location currentLocation = context.getCurrentLocation();
        Set<String> exits = currentLocation.getExits();

        if (exits.isEmpty()) {
            Display.showError("There's no way out of here!");
            return CommandResult.failure("No exits");
        }

        // Priority order for exit directions
        String[] exitPriority = {"out", "outside", "exit", "door", "south", "north", "east", "west"};

        String chosenExit = null;
        for (String preferred : exitPriority) {
            if (exits.contains(preferred)) {
                chosenExit = preferred;
                break;
            }
        }

        // If no preferred exit found, use the first available
        if (chosenExit == null) {
            chosenExit = exits.iterator().next();
        }

        // Use handleGo to do the actual movement
        return handleGo(context, chosenExit);
    }

    private String buildExitsDisplay(Location location) {
        Set<String> exits = location.getExits();
        if (exits.isEmpty()) {
            return Display.colorize("Exits: ", WHITE) + "none";
        }
        return Display.colorize("Exits: ", WHITE) + String.join(", ", exits);
    }
}
