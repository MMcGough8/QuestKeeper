package com.questkeeper.core.command;

import com.questkeeper.character.Character;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.inventory.items.MagicItem;
import com.questkeeper.ui.Display;
import com.questkeeper.world.Location;

import java.util.List;
import java.util.Set;

import static com.questkeeper.core.command.CommandUtils.*;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles item-related commands: take, drop, equip, unequip, use.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class ItemCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of(
        "take", "get", "pickup",
        "drop",
        "equip", "wear", "wield",
        "unequip", "remove",
        "use", "activate"
    );

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        return switch (verb.toLowerCase()) {
            case "take", "get", "pickup" -> handleTake(context, noun);
            case "drop" -> handleDrop(context, noun);
            case "equip", "wear", "wield" -> handleEquip(context, noun);
            case "unequip", "remove" -> handleUnequip(context, noun);
            case "use", "activate" -> handleUse(context, noun);
            default -> CommandResult.failure("Unknown item command: " + verb);
        };
    }

    private CommandResult handleTake(GameContext context, String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Take what?");
            return CommandResult.failure("Take what?");
        }

        Location location = context.getCurrentLocation();
        List<String> items = location.getItems();

        for (String itemId : items) {
            if (matchesTarget(itemId, target)) {
                var item = context.getCampaign().getItem(itemId);
                if (item != null) {
                    // Add to inventory and remove from location
                    context.getCharacter().getInventory().addItem(item);
                    location.removeItem(itemId);
                    Display.showItemGained(item.getName(), item.getDescription());
                    return CommandResult.success();
                }
            }
        }

        Display.showError("You don't see '" + target + "' here to take.");
        return CommandResult.failure("Item not found");
    }

    private CommandResult handleDrop(GameContext context, String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Drop what?");
            return CommandResult.failure("Drop what?");
        }

        var inventory = context.getCharacter().getInventory();
        var items = inventory.getAllItems();

        for (var stack : items) {
            if (matchesTarget(stack.getItem().getName(), target) ||
                matchesTarget(stack.getItem().getId(), target)) {
                var item = stack.getItem();
                inventory.removeItem(item);
                context.getCurrentLocation().addItem(item.getId());
                Display.println("You drop the " + item.getName() + ".");
                return CommandResult.success();
            }
        }

        Display.showError("You don't have '" + target + "' in your inventory.");
        return CommandResult.failure("Item not in inventory");
    }

    private CommandResult handleEquip(GameContext context, String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Equip what? Try 'equip <item name>' or 'equip <item> to offhand'");
            return CommandResult.failure("Equip what?");
        }

        // Parse "equip X to Y" or "equip X Y" syntax for explicit slot targeting
        String itemName = target;
        EquipmentSlot targetSlot = null;

        if (target.toLowerCase().contains(" to ")) {
            // "equip shortsword to offhand" syntax
            String[] parts = target.split("(?i) to ");
            itemName = parts[0].trim();
            if (parts.length > 1) {
                targetSlot = parseSlotName(parts[1].trim().toLowerCase());
            }
        } else {
            // Try "equip shortsword offhand" syntax (no "to")
            String[] words = target.split("\\s+");
            if (words.length >= 2) {
                String lastWord = words[words.length - 1].toLowerCase();
                EquipmentSlot possibleSlot = parseSlotName(lastWord);
                if (possibleSlot != null) {
                    targetSlot = possibleSlot;
                    // Remove the slot name from the item name
                    itemName = target.substring(0, target.toLowerCase().lastIndexOf(lastWord)).trim();
                }
            }
        }

        var inventory = context.getCharacter().getInventory();
        List<Item> matches = inventory.findItemsByName(itemName);

        if (matches.isEmpty()) {
            Display.showError("You don't have anything called '" + target + "' in your inventory.");
            return CommandResult.failure("Item not found");
        }

        // Filter to only equippable items
        List<Item> equippable = matches.stream()
                .filter(Item::isEquippable)
                .toList();

        if (equippable.isEmpty()) {
            Display.showError("'" + matches.get(0).getName() + "' cannot be equipped.");
            return CommandResult.failure("Cannot equip item");
        }

        Item toEquip;
        if (equippable.size() == 1) {
            toEquip = equippable.get(0);
        } else {
            // Multiple matches - let player choose
            Display.println();
            Display.println(Display.colorize("Multiple items match. Which one?", CYAN));
            for (int i = 0; i < equippable.size(); i++) {
                Display.println(String.format("  %d) %s", i + 1, equippable.get(i).getName()));
            }
            Display.showPrompt("Choice (1-" + equippable.size() + ")> ");

            try {
                int choice = Integer.parseInt(context.readInput().trim());
                if (choice < 1 || choice > equippable.size()) {
                    Display.showError("Invalid choice.");
                    return CommandResult.failure("Invalid choice");
                }
                toEquip = equippable.get(choice - 1);
            } catch (NumberFormatException e) {
                Display.showError("Please enter a number.");
                return CommandResult.failure("Invalid input");
            }
        }

        // Equip the item (use explicit slot if provided)
        Item previousItem;
        String slotName;
        EquipmentSlot actualSlot;

        if (targetSlot != null) {
            previousItem = inventory.equipToSlot(toEquip, targetSlot);
            slotName = targetSlot.getDisplayName();
            actualSlot = targetSlot;
        } else {
            previousItem = inventory.equip(toEquip);
            slotName = getSlotNameForItem(toEquip);
            actualSlot = getSlotForItem(toEquip);
        }

        // Verify the equip actually succeeded by checking if the item is in the slot
        if (actualSlot == null || inventory.getEquipped(actualSlot) != toEquip) {
            Display.showError("Failed to equip " + toEquip.getName() + " to " + slotName + ".");
            return CommandResult.failure("Equip failed");
        }

        Display.println();
        Display.println(Display.colorize("Equipped: ", GREEN) + toEquip.getName() +
                Display.colorize(" (" + slotName + ")", WHITE));

        if (previousItem != null) {
            Display.println(Display.colorize("Unequipped: ", YELLOW) + previousItem.getName() +
                    " (returned to inventory)");
        }
        Display.println();
        return CommandResult.success();
    }

    private CommandResult handleUnequip(GameContext context, String target) {
        if (target == null || target.isEmpty()) {
            showEquippedItems(context);
            Display.println("Use 'unequip <slot>' or 'unequip <item name>' to unequip.");
            Display.println("Slots: weapon, armor, shield, offhand, head, neck, ring");
            Display.println();
            return CommandResult.success();
        }

        var inventory = context.getCharacter().getInventory();
        String lowerTarget = target.toLowerCase();

        // Try to match by slot name first
        EquipmentSlot slot = parseSlotName(lowerTarget);

        if (slot != null) {
            Item unequipped = inventory.unequip(slot);
            if (unequipped != null) {
                Display.println();
                Display.println(Display.colorize("Unequipped: ", GREEN) + unequipped.getName() +
                        " (returned to inventory)");
                Display.println();
                return CommandResult.success();
            } else {
                Display.showError("Nothing equipped in " + slot.getDisplayName() + " slot.");
                return CommandResult.failure("Nothing equipped");
            }
        }

        // Try to find equipped item by name
        var equippedItems = inventory.getEquippedItems();
        for (var entry : equippedItems.entrySet()) {
            if (entry.getValue().getName().toLowerCase().contains(lowerTarget)) {
                Item unequipped = inventory.unequip(entry.getKey());
                Display.println();
                Display.println(Display.colorize("Unequipped: ", GREEN) + unequipped.getName() +
                        " (returned to inventory)");
                Display.println();
                return CommandResult.success();
            }
        }

        Display.showError("'" + target + "' is not equipped. Use 'unequip' to see equipped items.");
        return CommandResult.failure("Item not equipped");
    }

    private CommandResult handleUse(GameContext context, String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Use what? Try 'use <item name>'.");
            return CommandResult.failure("Use what?");
        }

        var inventory = context.getCharacter().getInventory();
        var items = inventory.findItemsByName(target);

        if (items.isEmpty()) {
            // Also check equipped items
            var equipped = inventory.getEquippedItems();
            for (Item item : equipped.values()) {
                if (item.getName().toLowerCase().contains(target.toLowerCase())) {
                    items.add(item);
                    break;
                }
            }
        }

        if (items.isEmpty()) {
            Display.showError("You don't have anything called '" + target + "'.");
            return CommandResult.failure("Item not found");
        }

        Item item = items.get(0);

        // Check if it's a MagicItem with usable effects
        if (item instanceof MagicItem magicItem) {
            Character player = context.getCharacter();

            if (!magicItem.canUse(player)) {
                Display.showError(magicItem.getCannotUseReason(player));
                return CommandResult.failure("Cannot use item");
            }

            var usableEffects = magicItem.getUsableEffects();
            if (usableEffects.isEmpty()) {
                Display.println();
                Display.println(Display.colorize(magicItem.getName(), CYAN));
                Display.println(magicItem.getDescription());
                Display.println();
                Display.println(Display.colorize("This item has no activatable effects right now.", YELLOW));
                Display.println();
                return CommandResult.success();
            }

            Display.println();
            String result = magicItem.use(player);
            Display.println(Display.colorize("You use the " + magicItem.getName() + "!", GREEN));
            Display.println();
            Display.println(result);
            Display.println();

            // Check if consumable and fully used
            if (magicItem.isFullyConsumed()) {
                inventory.removeItem(magicItem);
                Display.println(Display.colorize("The " + magicItem.getName() + " is consumed.", YELLOW));
                Display.println();
            }
            return CommandResult.success();
        }

        // For regular items, show the description (flavor text for non-magic items)
        Display.println();
        Display.println(Display.colorize(item.getName(), CYAN));
        if (!item.getDescription().isEmpty()) {
            Display.println(item.getDescription());
        } else {
            Display.println("You examine the " + item.getName() + ".");
        }
        Display.println();

        // Check if item type suggests it might be usable
        if (item.getType() == Item.ItemType.CONSUMABLE) {
            Display.println(Display.colorize("This looks like it could be consumed, but you're not sure how.", YELLOW));
            Display.println();
        }
        return CommandResult.success();
    }

    private void showEquippedItems(GameContext context) {
        var inventory = context.getCharacter().getInventory();
        var equipped = inventory.getEquippedItems();

        Display.println();
        Display.printBox("EQUIPPED ITEMS", 50, MAGENTA);
        Display.println();

        if (equipped.isEmpty()) {
            Display.println("Nothing equipped.");
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                Item item = equipped.get(slot);
                String slotDisplay = String.format("%-12s", slot.getDisplayName() + ":");
                if (item != null) {
                    Display.println(Display.colorize(slotDisplay, WHITE) + item.getName());
                } else {
                    Display.println(Display.colorize(slotDisplay, WHITE) +
                            Display.colorize("(empty)", DEFAULT));
                }
            }
        }
        Display.println();
    }
}
