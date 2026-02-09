package com.questkeeper.core.command;

import com.questkeeper.character.Character;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Inventory.ItemStack;
import com.questkeeper.inventory.Item;
import com.questkeeper.ui.Display;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles inventory and character stat display commands.
 *
 * Commands: inventory, i, stats, equipment, equipped, gear
 *
 * @author Marc McGough
 * @version 1.0
 */
public class InventoryCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of(
        "inventory", "i", "stats", "equipment", "equipped", "gear"
    );

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        return switch (verb.toLowerCase()) {
            case "inventory", "i" -> handleInventory(context);
            case "stats" -> handleStats(context);
            case "equipment", "equipped", "gear" -> handleEquipment(context);
            default -> CommandResult.failure("Unknown inventory command: " + verb);
        };
    }

    private CommandResult handleInventory(GameContext context) {
        Character character = context.getCharacter();
        Inventory inventory = character.getInventory();

        Display.println();
        Display.printBox("INVENTORY", 50, YELLOW);
        Display.println();

        // Show key equipped items at a glance
        Map<EquipmentSlot, Item> equipped = inventory.getEquippedItems();
        Display.println(Display.colorize("Equipped:", WHITE));
        Item weapon = equipped.get(EquipmentSlot.MAIN_HAND);
        Item armor = equipped.get(EquipmentSlot.ARMOR);
        Item offhand = equipped.get(EquipmentSlot.OFF_HAND);
        Display.println("  Weapon: " + (weapon != null ? weapon.getName() : "(none)"));
        Display.println("  Armor:  " + (armor != null ? armor.getName() : "(none)"));
        if (offhand != null) {
            Display.println("  Off-hand: " + offhand.getName());
        }
        Display.println(Display.colorize("  (type 'equipment' for full list)", DEFAULT));
        Display.println();

        Display.println(Display.colorize("Gold: ", WHITE) +
            Display.colorize(String.valueOf(inventory.getGold()) + " gp", YELLOW));
        Display.println(Display.colorize("Weight: ", WHITE) +
            String.format("%.1f / %.1f lbs", inventory.getCurrentWeight(), inventory.getMaxWeight()));
        Display.println();

        List<ItemStack> items = inventory.getAllItems();
        if (items.isEmpty()) {
            Display.println("Your pack is empty.");
        } else {
            Display.println(Display.colorize("Backpack:", WHITE));
            for (ItemStack stack : items) {
                String countStr = stack.getQuantity() > 1 ? " (x" + stack.getQuantity() + ")" : "";
                Display.println("  - " + stack.getItem().getName() + countStr);
            }
        }

        Display.println();
        return CommandResult.success();
    }

    private CommandResult handleStats(GameContext context) {
        Character character = context.getCharacter();

        Display.println();
        Display.printBox(character.getName(), 60, GREEN);
        Display.println();

        Display.println(String.format("Level %d %s %s",
            character.getLevel(),
            character.getRace().getDisplayName(),
            character.getCharacterClass().getDisplayName()));
        Display.println();

        Display.printHealthBar(character.getCurrentHitPoints(), character.getMaxHitPoints());
        Display.println(Display.colorize("AC: ", WHITE) + character.getArmorClass() +
            "  " + Display.colorize("Speed: ", WHITE) + character.getRace().getSpeed() + " ft");
        Display.println();

        Display.println(Display.colorize("Ability Scores:", WHITE));
        Display.println(character.getAbilityScoresString());
        Display.println();

        Display.println(Display.colorize("Proficiency Bonus: ", WHITE) + "+" + character.getProficiencyBonus());
        Display.println();

        return CommandResult.success();
    }

    private CommandResult handleEquipment(GameContext context) {
        Character character = context.getCharacter();
        Inventory inventory = character.getInventory();
        Map<EquipmentSlot, Item> equipped = inventory.getEquippedItems();

        Display.println();
        Display.printBox("EQUIPMENT", 50, CYAN);
        Display.println();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Item item = equipped.get(slot);
            String slotName = formatSlotName(slot);
            if (item != null) {
                Display.println(Display.colorize(slotName + ": ", WHITE) + item.getName());
            } else {
                Display.println(Display.colorize(slotName + ": ", WHITE) +
                    Display.colorize("(empty)", DEFAULT));
            }
        }

        Display.println();
        Display.println(Display.colorize("Total AC: ", WHITE) + character.getArmorClass());
        Display.println();

        return CommandResult.success();
    }

    private String formatSlotName(EquipmentSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> "Main Hand";
            case OFF_HAND -> "Off Hand";
            case ARMOR -> "Armor";
            case HEAD -> "Head";
            case NECK -> "Neck";
            case RING_LEFT -> "Left Ring";
            case RING_RIGHT -> "Right Ring";
        };
    }
}
