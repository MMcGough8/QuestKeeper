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
 * @version 1.0
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
     * Equipment slots for wearable/holdable items.
     */
    public enum EquipmentSlot {
        MAIN_HAND("Main Hand"),
        OFF_HAND("Off Hand"),
        ARMOR("Armor"),
        HEAD("Head"),
        NECK("Neck"),
        RING_LEFT("Left Ring"),
        RING_RIGHT("Right Ring");
        
        private final String displayName;
        
        EquipmentSlot(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
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
        if (item == null || quantity <= 0) {
            return false;
        }
        
        // Check weight limit
        double addedWeight = item.getWeight() * quantity;
        if (maxWeight > 0 && getCurrentWeight() + addedWeight > maxWeight) {
            return false;
        }
        
        // Try to stack with existing items
        if (item.isStackable()) {
            for (ItemStack stack : items) {
                if (stack.canStackWith(item)) {
                    int added = stack.add(quantity);
                    if (added == quantity) {
                        return true;
                    }
                    quantity -= added;
                }
            }
        }
        // Add remaining as new stacks
        while (quantity > 0) {
            // Check slot limit
            if (maxSlots > 0 && items.size() >= maxSlots) {
                return false;
            }
            
            int stackSize = item.isStackable() ? 
                    Math.min(quantity, item.getMaxStackSize()) : 1;
            items.add(new ItemStack(item.copy(), stackSize));
            quantity -= stackSize;
        }
        
        return true;
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

    public List<Item> getItemsByType(ItemType type) {
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
            Item offHand = unequip(EquipmentSlot.OFF_HAND);             // Unequip off-hand if equipping two-handed weapon
            if (offHand != null) {
                addItem(offHand);
            }
        }
        Item previous = equipped.get(slot);                             // Unequip current item in slot
        if (previous != null) {
            addItem(previous);
        }
        removeItem(item);                                               // Remove from inventory and equip
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
        return item instanceof Weapon weapon ? weapon : null;
    }

    public Armor getEquippedArmor() {
        Item item = equipped.get(EquipmentSlot.ARMOR);
        return item instanceof Armor armor ? armor : null;
    }

    public Armor getEquippedShield() {
        Item item = equipped.get(EquipmentSlot.OFF_HAND);
        return item instanceof Armor armor && armor.isShield() ? armor : null;
    }