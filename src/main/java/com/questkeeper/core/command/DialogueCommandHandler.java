package com.questkeeper.core.command;

import com.questkeeper.character.Character;
import com.questkeeper.character.NPC;
import com.questkeeper.core.CommandParser;
import com.questkeeper.dialogue.DialogueResult;
import com.questkeeper.dialogue.DialogueSystem;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Item;
import com.questkeeper.ui.Display;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles dialogue-related commands: talk, ask, buy, bye.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class DialogueCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of(
        "talk", "ask", "buy", "bye"
    );

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public boolean isAvailable(GameContext context) {
        // Talk is always available
        // Ask, buy, bye require active conversation
        return true;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        return switch (verb.toLowerCase()) {
            case "talk" -> handleTalk(context, noun);
            case "ask" -> handleAsk(context, fullInput);
            case "buy" -> handleBuy(context, noun);
            case "bye" -> handleBye(context);
            default -> CommandResult.failure("Unknown dialogue command: " + verb);
        };
    }

    private CommandResult handleTalk(GameContext context, String target) {
        DialogueSystem dialogueSystem = context.getDialogueSystem();

        if (target == null || target.isEmpty()) {
            // List available NPCs
            List<NPC> npcs = dialogueSystem.getNpcsAtCurrentLocation(context.getGameState());
            if (npcs.isEmpty()) {
                Display.println("There's no one here to talk to.");
            } else {
                Display.println("You can talk to:");
                for (NPC npc : npcs) {
                    Display.println("  - " + Display.colorize(npc.getName(), CYAN));
                }
            }
            return CommandResult.success();
        }

        DialogueResult result = dialogueSystem.startDialogue(context.getGameState(), target);
        displayDialogueResult(result);
        return result.isSuccess() ? CommandResult.dialogueStarted() : CommandResult.failure(result.getMessage());
    }

    private CommandResult handleAsk(GameContext context, String input) {
        DialogueSystem dialogueSystem = context.getDialogueSystem();

        if (!dialogueSystem.isInConversation()) {
            Display.showError("You're not talking to anyone. Use 'talk <name>' first.");
            return CommandResult.failure("Not in conversation");
        }

        String[] parsed = CommandParser.parseAskAbout(input);
        String topic = parsed[1];

        if (topic == null || topic.isEmpty()) {
            NPC npc = dialogueSystem.getCurrentNpc();
            Display.println("What do you want to ask " + npc.getName() + " about?");
            Display.println("Available topics: " + String.join(", ",
                npc.getAvailableTopics(context.getGameState().getFlags())));
            return CommandResult.success();
        }

        DialogueResult result = dialogueSystem.askAbout(topic);
        displayDialogueResult(result);
        return CommandResult.success();
    }

    private CommandResult handleBye(GameContext context) {
        DialogueSystem dialogueSystem = context.getDialogueSystem();

        if (!dialogueSystem.isInConversation()) {
            Display.println("You wave goodbye to no one in particular.");
            return CommandResult.success();
        }

        NPC npc = dialogueSystem.getCurrentNpc();
        dialogueSystem.endDialogue();

        Display.println();
        Display.println(Display.colorize("You end your conversation with " + npc.getName() + ".", CYAN));
        Display.println();
        return CommandResult.success();
    }

    private CommandResult handleBuy(GameContext context, String itemName) {
        DialogueSystem dialogueSystem = context.getDialogueSystem();

        // Must be in conversation with a shopkeeper
        if (!dialogueSystem.isInConversation()) {
            Display.showError("You need to talk to a shopkeeper first.");
            return CommandResult.failure("Not in conversation");
        }

        NPC npc = dialogueSystem.getCurrentNpc();
        if (!npc.isShopkeeper()) {
            Display.showError(npc.getName() + " doesn't have anything for sale.");
            return CommandResult.failure("NPC is not a shopkeeper");
        }

        // Show shop menu if no item specified
        if (itemName == null || itemName.trim().isEmpty()) {
            showShopMenu(context, npc);
            return CommandResult.success();
        }

        // Try to find the item
        String searchTerm = itemName.toLowerCase().trim();
        Map<String, Integer> shopItems = npc.getShopItems();

        String foundItem = null;
        int price = 0;

        // Try exact match first, then partial match
        for (Map.Entry<String, Integer> entry : shopItems.entrySet()) {
            if (entry.getKey().equals(searchTerm)) {
                foundItem = entry.getKey();
                price = entry.getValue();
                break;
            }
        }

        if (foundItem == null) {
            for (Map.Entry<String, Integer> entry : shopItems.entrySet()) {
                if (entry.getKey().contains(searchTerm) || searchTerm.contains(entry.getKey())) {
                    foundItem = entry.getKey();
                    price = entry.getValue();
                    break;
                }
            }
        }

        if (foundItem == null) {
            Display.showError(npc.getName() + " doesn't sell '" + itemName + "'.");
            showShopMenu(context, npc);
            return CommandResult.failure("Item not found in shop");
        }

        // Check if player has enough gold
        Character character = context.getCharacter();
        Inventory inventory = character.getInventory();

        if (!inventory.hasGold(price)) {
            Display.println();
            Display.println(Display.colorize(npc.getName() + " shakes their head.", CYAN));
            Display.println("\"That'll be " + formatPrice(price) + ", and you only have " +
                formatPrice(inventory.getGold()) + ".\"");
            Display.println();
            return CommandResult.failure("Not enough gold");
        }

        // Make the purchase
        inventory.removeGold(price);

        // Add item to inventory
        Item purchasedItem = context.getCampaign().getItem(foundItem);
        if (purchasedItem != null) {
            inventory.addItem(purchasedItem);
        }

        // Display purchase
        Display.println();
        Display.println(Display.colorize(npc.getName() + " takes your coin and hands you the " + foundItem + ".", CYAN));
        Display.println(Display.colorize("(-" + formatPrice(price) + ")", YELLOW));
        Display.println();
        return CommandResult.success();
    }

    private void showShopMenu(GameContext context, NPC npc) {
        Map<String, Integer> shopItems = npc.getShopItems();

        Display.println();
        Display.println(Display.colorize(npc.getName() + "'s Wares:", YELLOW));
        Display.println();

        for (Map.Entry<String, Integer> entry : shopItems.entrySet()) {
            String itemName = entry.getKey();
            int price = entry.getValue();
            String displayName = capitalizeWords(itemName);
            Display.println("  " + Display.colorize(displayName, WHITE) +
                " - " + Display.colorize(formatPrice(price), YELLOW));
        }

        Display.println();
        Display.println(Display.colorize("Your gold: " + formatPrice(context.getCharacter().getInventory().getGold()), CYAN));
        Display.println(Display.colorize("Type 'buy <item>' to purchase.", DEFAULT));
        Display.println();
    }

    private void displayDialogueResult(DialogueResult result) {
        if (result == null) return;

        Display.println();

        if (result.getNpcName() != null) {
            Display.println(Display.colorize(result.getNpcName() + ":", CYAN));
        }

        if (result.getMessage() != null) {
            Display.println("\"" + result.getMessage() + "\"");
        }

        if (result.getAvailableTopics() != null && !result.getAvailableTopics().isEmpty()) {
            Display.println();
            Display.println(Display.colorize("Topics: ", WHITE) +
                String.join(", ", result.getAvailableTopics()));
        }

        Display.println();
    }

    private String formatPrice(int copper) {
        if (copper >= 100) {
            int gold = copper / 100;
            int remaining = copper % 100;
            if (remaining > 0) {
                return gold + " gp " + remaining + " cp";
            }
            return gold + " gp";
        } else if (copper >= 10) {
            int silver = copper / 10;
            int remaining = copper % 10;
            if (remaining > 0) {
                return silver + " sp " + remaining + " cp";
            }
            return silver + " sp";
        }
        return copper + " cp";
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(java.lang.Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        return result.toString();
    }
}
