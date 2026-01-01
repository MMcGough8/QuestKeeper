package com.questkeeper.inventory;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all items in the game.
 * 
 * Items have a name, description, weight, and gold value.
 * Subclasses add specific functionality for weapons, armor, consumables, etc.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class Item {

    private static final int DEFAULT_MAX_STACK = 1;

    private static final int STACKABLE_MAX_STACK = 99;

    public enum ItemType {
        WEAPON("Weapon"),
        ARMOR("Armor"),
        SHIELD("Shield"),
        CONSUMABLE("Consumable"),
        MAGIC_ITEM("Magic Item"),
        TOOL("Tool"),
        TREASURE("Treasure"),
        QUEST_ITEM("Quest Item"),
        MISCELLANEOUS("Miscellaneous");
        
        private final String displayName;
        
        ItemType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }

    public enum Rarity {
        COMMON("Common", "gray"),
        UNCOMMON("Uncommon", "green"),
        RARE("Rare", "blue"),
        VERY_RARE("Very Rare", "purple"),
        LEGENDARY("Legendary", "orange"),
        ARTIFACT("Artifact", "red");
        
        private final String displayName;
        private final String color;
        
        Rarity(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    private final String id;
    private String name;
    private String description;
    private ItemType type;
    private Rarity rarity;
    private double weight;          // Weight in pounds
    private int goldValue;          // Base value in gold pieces
    private boolean stackable;
    private int maxStackSize;
    private boolean questItem;      // Quest items cannot be dropped/sold

    public Item(String name, ItemType type) {
        this(name, type, "", 0.0, 0);
    }

    public Item(String name, ItemType type, String description, double weight, int goldValue) {
        this.id = generateId(name);
        this.name = name;
        this.type = type;
        this.description = description;
        this.weight = Math.max(0, weight);
        this.goldValue = Math.max(0, goldValue);
        this.rarity = Rarity.COMMON;
        this.stackable = false;
        this.maxStackSize = DEFAULT_MAX_STACK;
        this.questItem = false;
    }

    public Item(String name, ItemType type, String description, double weight, 
                int goldValue, Rarity rarity, boolean stackable) {
        this(name, type, description, weight, goldValue);
        this.rarity = rarity;
        this.stackable = stackable;
        this.maxStackSize = stackable ? STACKABLE_MAX_STACK : DEFAULT_MAX_STACK;
    }

    protected Item(String id, String name, ItemType type, String description, 
                   double weight, int goldValue) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.weight = Math.max(0, weight);
        this.goldValue = Math.max(0, goldValue);
        this.rarity = Rarity.COMMON;
        this.stackable = false;
        this.maxStackSize = DEFAULT_MAX_STACK;
        this.questItem = false;
    }

    private static String generateId(String name) {
        String baseName = name.toLowerCase().replaceAll("\\s+", "_");
        return baseName + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ItemType getType() {
        return type;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public double getWeight() {
        return weight;
    }

    public int getGoldValue() {
        return goldValue;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }

    public void setWeight(double weight) {
        this.weight = Math.max(0, weight);
    }

    public void setGoldValue(int goldValue) {
        this.goldValue = Math.max(0, goldValue);
    }

    public boolean isStackable() {
        return stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
        if (stackable && maxStackSize == DEFAULT_MAX_STACK) {
            this.maxStackSize = STACKABLE_MAX_STACK;
        } else if (!stackable) {
            this.maxStackSize = DEFAULT_MAX_STACK;
        }
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = Math.max(1, maxStackSize);
    }

    public boolean isEquippable() {
        return type == ItemType.WEAPON || type == ItemType.ARMOR || type == ItemType.SHIELD;
    }

    public boolean isQuestItem() {
        return questItem;
    }

    public void setQuestItem(boolean questItem) {
        this.questItem = questItem;
    }

    public boolean isSellable() {
        return !questItem && goldValue > 0;
    }

    public boolean isDroppable() {
        return !questItem;
    }

    public Item copy() {
        Item copy = new Item(generateId(name), name, type, description, weight, goldValue);
        copy.rarity = this.rarity;
        copy.stackable = this.stackable;
        copy.maxStackSize = this.maxStackSize;
        copy.questItem = this.questItem;
        return copy;
    }

    public String getDisplayName() {
        if (rarity != Rarity.COMMON) {
            return String.format("%s (%s)", name, rarity.getDisplayName());
        }
        return name;
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (rarity != Rarity.COMMON) {
            sb.append(" [").append(rarity.getDisplayName()).append("]");
        }
        sb.append("\n");
        sb.append("Type: ").append(type.getDisplayName()).append("\n");
        if (!description.isEmpty()) {
            sb.append(description).append("\n");
        }
        sb.append(String.format("Weight: %.1f lb", weight));
        if (goldValue > 0) {
            sb.append(String.format(" | Value: %d gp", goldValue));
        }
        if (questItem) {
            sb.append(" | Quest Item");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%s [%s, %.1f lb, %d gp]", 
                name, type.getDisplayName(), weight, goldValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item other = (Item) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Item createConsumable(String name, String description, 
                                         double weight, int goldValue) {
        Item item = new Item(name, ItemType.CONSUMABLE, description, weight, goldValue);
        item.setStackable(true);
        return item;
    }

    public static Item createTreasure(String name, String description, 
                                       double weight, int goldValue, Rarity rarity) {
        Item item = new Item(name, ItemType.TREASURE, description, weight, goldValue);
        item.setRarity(rarity);
        return item;
    }

    public static Item createQuestItem(String name, String description) {
        Item item = new Item(name, ItemType.QUEST_ITEM, description, 0, 0);
        item.setQuestItem(true);
        return item;
    }

    public static Item createTool(String name, String description, 
                                   double weight, int goldValue) {
        return new Item(name, ItemType.TOOL, description, weight, goldValue);
    }
}