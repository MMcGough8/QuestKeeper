package com.questkeeper.inventory;

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
    ItemType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}