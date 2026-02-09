package com.questkeeper.core.command;

import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;

/**
 * Shared utility methods for command handlers.
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class CommandUtils {

    private CommandUtils() {
        // Utility class - no instantiation
    }

    /**
     * Checks if a search string matches a target string.
     * Case-insensitive and supports partial matching.
     *
     * @param actual the actual name or ID to match against
     * @param search the search string entered by the player
     * @return true if the search matches the actual string
     */
    public static boolean matchesTarget(String actual, String search) {
        if (actual == null || search == null) return false;
        String lowerActual = actual.toLowerCase();
        String lowerSearch = search.toLowerCase();
        // Exact match or contains match
        return lowerActual.equals(lowerSearch) ||
               lowerActual.contains(lowerSearch) ||
               lowerActual.replace("_", " ").contains(lowerSearch);
    }

    /**
     * Parses a slot name string to an EquipmentSlot.
     *
     * @param name the slot name (e.g., "weapon", "armor", "offhand")
     * @return the corresponding EquipmentSlot, or null if not recognized
     */
    public static EquipmentSlot parseSlotName(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase()) {
            case "weapon", "main hand", "mainhand", "main" -> EquipmentSlot.MAIN_HAND;
            case "offhand", "off hand", "off", "shield" -> EquipmentSlot.OFF_HAND;
            case "armor", "body", "chest" -> EquipmentSlot.ARMOR;
            case "head", "helmet", "helm", "hat" -> EquipmentSlot.HEAD;
            case "neck", "necklace", "amulet" -> EquipmentSlot.NECK;
            case "ring", "ring left", "left ring", "ringleft" -> EquipmentSlot.RING_LEFT;
            case "ring right", "right ring", "ringright" -> EquipmentSlot.RING_RIGHT;
            default -> null;
        };
    }

    /**
     * Gets the default equipment slot for an item.
     *
     * @param item the item to check
     * @return the appropriate slot, or null if item isn't equippable
     */
    public static EquipmentSlot getSlotForItem(Item item) {
        if (item instanceof Weapon) {
            return EquipmentSlot.MAIN_HAND;
        } else if (item instanceof Armor armor) {
            return armor.isShield() ? EquipmentSlot.OFF_HAND : EquipmentSlot.ARMOR;
        }
        return null;
    }

    /**
     * Gets a display name for the slot an item would equip to.
     *
     * @param item the item to check
     * @return the slot display name
     */
    public static String getSlotNameForItem(Item item) {
        if (item instanceof Weapon) {
            return "Main Hand";
        } else if (item instanceof Armor armor) {
            return armor.isShield() ? "Off Hand" : "Armor";
        }
        return "Equipment";
    }

    /**
     * Expands shorthand direction to full name.
     *
     * @param dir the direction (e.g., "n", "ne")
     * @return the expanded direction (e.g., "north", "northeast")
     */
    public static String expandDirection(String dir) {
        if (dir == null) return dir;
        return switch (dir.toLowerCase()) {
            case "n" -> "north";
            case "s" -> "south";
            case "e" -> "east";
            case "w" -> "west";
            case "ne" -> "northeast";
            case "nw" -> "northwest";
            case "se" -> "southeast";
            case "sw" -> "southwest";
            case "u" -> "up";
            case "d" -> "down";
            default -> dir;
        };
    }

    /**
     * Checks if a word is a direction.
     *
     * @param word the word to check
     * @return true if it's a direction
     */
    public static boolean isDirection(String word) {
        if (word == null) return false;
        return switch (word.toLowerCase()) {
            case "north", "south", "east", "west", "n", "s", "e", "w",
                 "northeast", "northwest", "southeast", "southwest", "ne", "nw", "se", "sw",
                 "up", "down", "u", "d" -> true;
            default -> false;
        };
    }

    /**
     * Normalizes direction input by stripping common prefixes.
     * Handles inputs like "to the cemetery", "to cemetery", "the cemetery".
     *
     * @param direction the raw direction input
     * @return the normalized direction
     */
    public static String normalizeDirection(String direction) {
        if (direction == null) return null;

        String normalized = direction.toLowerCase().trim();

        // Strip common prefixes in order of specificity
        if (normalized.startsWith("to the ")) {
            normalized = normalized.substring(7);
        } else if (normalized.startsWith("to ")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("the ")) {
            normalized = normalized.substring(4);
        }

        return normalized.trim();
    }
}
