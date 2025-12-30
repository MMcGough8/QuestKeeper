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