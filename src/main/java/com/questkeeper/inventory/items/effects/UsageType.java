package com.questkeeper.inventory.items.effects;

/**
 * Defines how often an item effect can be used.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public enum UsageType {
    
    /**
     * Can be used once per day, resets at dawn.
     */
    DAILY("Daily", "Resets at dawn"),
    
    /**
     * Can be used once per long rest.
     */
    LONG_REST("Long Rest", "Resets after a long rest"),
    
    /**
     * Has a limited number of charges that may or may not recharge.
     */
    CHARGES("Charges", "Limited charges"),
    
    /**
     * Single use, consumed when used.
     */
    CONSUMABLE("Consumable", "Destroyed when used"),
    
    /**
     * Can be used unlimited times with no restrictions.
     */
    UNLIMITED("Unlimited", "No usage limit"),
    
    /**
     * Passive effect, always active when equipped.
     */
    PASSIVE("Passive", "Always active");
    
    private final String displayName;
    private final String description;
    
    UsageType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}