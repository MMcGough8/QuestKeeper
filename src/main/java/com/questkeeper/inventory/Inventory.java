package com.questkeeper.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages a collection of items with weight limits and equipment slots.
 * 
 * Handles inventory operations like adding, removing, and equipping items.
 * Tracks carrying capacity based on character strength.
 * 
 * @author Marc McGough
 * @version 1.1
 */
public class Inventory {

    /** Default carrying capacity multiplier (STR × this = capacity in lbs) */
    private static final double CARRY_CAPACITY_MULTIPLIER = 15.0;
    
    /** Default maximum inventory slots (0 = unlimited) */
    private static final int DEFAULT_MAX_SLOTS = 0;

    private final List<ItemStack> items;
    private final Map<EquipmentSlot, Item> equipped;
    
    private int maxSlots;           // Maximum number of item stacks (0 = unlimited)
    private double maxWeight;       // Maximum carrying capacity in pounds
    private int gold;               // Currency

    /**
     * Equipment slots for wearable/holdable items following D&D 5e conventions.
     * Each slot can hold one item at a time. Two-handed weapons occupy both
     * MAIN_HAND and OFF_HAND slots. Rings can be worn on either hand.
     */
    public enum EquipmentSlot {
        /** Primary weapon hand */
        MAIN_HAND("Main Hand"),
        /** Shield or secondary weapon hand */
        OFF_HAND("Off Hand"),
        /** Body armor slot */
        ARMOR("Armor"),
        /** Helmet or headgear slot */
        HEAD("Head"),
        /** Amulet or necklace slot */
        NECK("Neck"),
        /** Left hand ring slot */
        RING_LEFT("Left Ring"),
        /** Right hand ring slot */
        RING_RIGHT("Right Ring");

        private final String displayName;

        EquipmentSlot(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    /**
     * Result of attempting to add an item to inventory.
     * Provides detailed feedback about why an add operation succeeded or failed.
     * Use {@link #isSuccess()} to check if the operation succeeded, and
     * {@link #getMessage()} to get a user-friendly explanation.
     */
    public enum AddItemResult {
        SUCCESS("Item added successfully."),
        ITEM_NULL("Cannot add null item."),
        INVALID_QUANTITY("Quantity must be positive."),
        WEIGHT_EXCEEDED("Cannot carry that much weight."),
        SLOTS_FULL("Inventory is full.");

        private final String message;

        AddItemResult(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }

        public boolean isSuccess() { return this == SUCCESS; }
    }

    /**
     * Creates a new empty inventory with default settings.
     */
    public Inventory() {
        this.items = new ArrayList<>();
        this.equipped = new HashMap<>();
        this.maxSlots = DEFAULT_MAX_SLOTS;
        this.maxWeight = 0; // 0 = no limit
        this.gold = 0;
    }

    public Inventory(int strengthScore) {
        this();
        setCarryingCapacityFromStrength(strengthScore);
    }

    public boolean addItem(Item item) {
        return addItem(item, 1);
    }

    public boolean addItem(Item item, int quantity) {
        return tryAddItem(item, quantity).isSuccess();
    }

    /**
     * Attempts to add items with detailed result.
     * Use this when you need to know why an add failed.
     */
    public AddItemResult tryAddItem(Item item, int quantity) {
        if (item == null) {
            return AddItemResult.ITEM_NULL;
        }
        if (quantity <= 0) {
            return AddItemResult.INVALID_QUANTITY;
        }

        // Check weight limit
        double addedWeight = item.getWeight() * quantity;
        if (maxWeight > 0 && getCurrentWeight() + addedWeight > maxWeight) {
            return AddItemResult.WEIGHT_EXCEEDED;
        }

        // Try to stack with existing items
        int remaining = quantity;
        if (item.isStackable()) {
            for (ItemStack stack : items) {
                if (stack.canStackWith(item)) {
                    int added = stack.add(remaining);
                    if (added == remaining) {
                        return AddItemResult.SUCCESS;
                    }
                    remaining -= added;
                }
            }
        }

        // Add remaining as new stacks
        while (remaining > 0) {
            // Check slot limit
            if (maxSlots > 0 && items.size() >= maxSlots) {
                return AddItemResult.SLOTS_FULL;
            }

            int stackSize = item.isStackable() ?
                    Math.min(remaining, item.getMaxStackSize()) : 1;
            items.add(new ItemStack(item.copy(), stackSize));
            remaining -= stackSize;
        }

        return AddItemResult.SUCCESS;
    }

    public boolean removeItem(Item item) {
        return removeItem(item, 1);
    }

    public boolean removeItem(Item item, int quantity) {
        if (item == null || quantity <= 0) {
            return false;
        }
        
        // Check if we have enough
        if (getItemCount(item) < quantity) {
            return false;
        }
        
        // Remove from stacks
        int remaining = quantity;
        List<ItemStack> toRemove = new ArrayList<>();
        
        for (ItemStack stack : items) {
            if (stack.getItem().getId().equals(item.getId())) {
                int removed = stack.remove(remaining);
                remaining -= removed;
                
                if (stack.isEmpty()) {
                    toRemove.add(stack);
                }
                
                if (remaining <= 0) {
                    break;
                }
            }
        }

        items.removeAll(toRemove);
        return remaining <= 0;
    }

    public Item removeItemById(String itemId) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.getItem().getId().equals(itemId)) {
                Item removed = stack.getItem();
                if (stack.getQuantity() == 1) {
                    items.remove(i);
                } else {
                    stack.remove(1);
                }
                return removed;
            }
        }
        return null;
    }

    public boolean hasItem(Item item) {
        return getItemCount(item) > 0;
    }

    public boolean hasItemById(String itemId) {
        return items.stream()
                .anyMatch(stack -> stack.getItem().getId().equals(itemId));
    }

    public int getItemCount(Item item) {
        return items.stream()
                .filter(stack -> stack.getItem().getId().equals(item.getId()))
                .mapToInt(ItemStack::getQuantity)
                .sum();
    }

    public Optional<Item> findItemById(String itemId) {
        return items.stream()
                .filter(stack -> stack.getItem().getId().equals(itemId))
                .map(ItemStack::getItem)
                .findFirst();
    }

    public List<Item> findItemsByName(String name) {
        String lowerName = name.toLowerCase();
        return items.stream()
                .filter(stack -> stack.getItem().getName().toLowerCase().contains(lowerName))
                .map(ItemStack::getItem)
                .collect(Collectors.toList());
    }

    public List<Item> getItemsByType(Item.ItemType type) {
        return items.stream()
                .filter(stack -> stack.getItem().getType() == type)
                .map(ItemStack::getItem)
                .collect(Collectors.toList());
    }

    public List<ItemStack> getAllItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Equips an item to the appropriate slot.
     */
    public Item equip(Item item) {
        if (item == null || !item.isEquippable()) {
            return null;
        }
        
        // Must have item in inventory
        if (!hasItem(item)) {
            return null;
        }
        
        EquipmentSlot slot = getSlotForItem(item);
        if (slot == null) {
            return null;
        }
        
        return equipToSlot(item, slot);
    }

    public Item equipToSlot(Item item, EquipmentSlot slot) {
        if (item == null || slot == null) {
            return null;
        }
        
        if (!hasItem(item)) {
            return null;
        }
        
        // Handle two-handed weapons
        if (item instanceof Weapon weapon && weapon.isTwoHanded()) {
            Item offHand = unequip(EquipmentSlot.OFF_HAND);
            if (offHand != null) {
                addItem(offHand);
            }
        }
        
        Item previous = equipped.get(slot);
        if (previous != null) {
            addItem(previous);
        }
        
        removeItem(item);
        equipped.put(slot, item);
        
        return previous;
    }

    public Item unequip(EquipmentSlot slot) {
        Item item = equipped.remove(slot);
        if (item != null) {
            addItem(item);
        }
        return item;
    }

    public Item getEquipped(EquipmentSlot slot) {
        return equipped.get(slot);
    }

    public Weapon getEquippedWeapon() {
        Item item = equipped.get(EquipmentSlot.MAIN_HAND);
        return item instanceof Weapon ? (Weapon) item : null;
    }

    public Armor getEquippedArmor() {
        Item item = equipped.get(EquipmentSlot.ARMOR);
        return item instanceof Armor ? (Armor) item : null;
    }

    public Armor getEquippedShield() {
        Item item = equipped.get(EquipmentSlot.OFF_HAND);
        return (item instanceof Armor armor && armor.isShield()) ? armor : null;
    }

    public boolean isSlotEmpty(EquipmentSlot slot) {
        return !equipped.containsKey(slot);
    }

    public Map<EquipmentSlot, Item> getEquippedItems() {
        return Collections.unmodifiableMap(equipped);
    }

    private EquipmentSlot getSlotForItem(Item item) {
        if (item instanceof Weapon) {
            return EquipmentSlot.MAIN_HAND;
        } else if (item instanceof Armor armor) {
            if (armor.isShield()) {
                return EquipmentSlot.OFF_HAND;
            }
            return EquipmentSlot.ARMOR;
        }
        return null;
    }

    public double getCurrentWeight() {
        double inventoryWeight = items.stream()
                .mapToDouble(stack -> stack.getItem().getWeight() * stack.getQuantity())
                .sum();
        
        double equippedWeight = equipped.values().stream()
                .mapToDouble(Item::getWeight)
                .sum();
        
        return inventoryWeight + equippedWeight;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight) {
        this.maxWeight = Math.max(0, maxWeight);
    }

    public void setCarryingCapacityFromStrength(int strengthScore) {
        this.maxWeight = strengthScore * CARRY_CAPACITY_MULTIPLIER;
    }

    public boolean isOverEncumbered() {
        return maxWeight > 0 && getCurrentWeight() > maxWeight;
    }

    public double getRemainingCapacity() {
        if (maxWeight <= 0) {
            return -1;
        }
        return maxWeight - getCurrentWeight();
    }

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        if (amount > 0) {
            gold += amount;
        }
    }

    public boolean removeGold(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (gold >= amount) {
            gold -= amount;
            return true;
        }
        return false;
    }

    public boolean hasGold(int amount) {
        return gold >= amount;
    }

    public int getUsedSlots() {
        return items.size();
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = Math.max(0, maxSlots);
    }

    public boolean isFull() {
        return maxSlots > 0 && items.size() >= maxSlots;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }

    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(ItemStack::getQuantity)
                .sum();
    }

    public int getTotalValue() {
        int inventoryValue = items.stream()
                .mapToInt(stack -> stack.getItem().getGoldValue() * stack.getQuantity())
                .sum();
        
        int equippedValue = equipped.values().stream()
                .mapToInt(Item::getGoldValue)
                .sum();
        
        return inventoryValue + equippedValue + gold;
    }

    @Override
    public String toString() {
        return String.format("Inventory[%d items, %.1f/%.1f lbs, %d gp]",
                getTotalItemCount(), getCurrentWeight(), maxWeight, gold);
    }

    public static class ItemStack {
        private final Item item;
        private int quantity;
        
        public ItemStack(Item item, int quantity) {
            this.item = item;
            this.quantity = Math.max(1, quantity);
        }
        
        public Item getItem() {
            return item;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public boolean canStackWith(Item other) {
            return item.isStackable() && 
                   item.getId().equals(other.getId()) &&
                   quantity < item.getMaxStackSize();
        }
        
        public int add(int amount) {
            int canAdd = item.getMaxStackSize() - quantity;
            int toAdd = Math.min(amount, canAdd);
            quantity += toAdd;
            return toAdd;
        }
        
        public int remove(int amount) {
            int toRemove = Math.min(amount, quantity);
            quantity -= toRemove;
            return toRemove;
        }
        
        public boolean isEmpty() {
            return quantity <= 0;
        }
        
        @Override
        public String toString() {
            if (quantity == 1) {
                return item.getName();
            }
            return item.getName() + " x" + quantity;
        }
    }
}