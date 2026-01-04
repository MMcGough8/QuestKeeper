package com.questkeeper.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.items.ItemEffect;

/**
 * Abstract base class for item effects providing common functionality.
 * 
 * Handles charge tracking, usage validation, and reset logic.
 * Subclasses implement the specific effect behavior in applyEffect().
 * 
 * @author Marc McGough
 * @version 1.0
 */
public abstract class AbstractItemEffect implements ItemEffect {
    
    private final String id;
    private String name;
    private String description;
    private UsageType usageType;
    
    private int maxCharges;
    private int currentCharges;
    private int rechargeAmount;     // How many charges restore on reset (0 = all)
    private boolean consumed;       // For consumables, tracks if used
    
    /**
     * Creates an effect with unlimited uses.
     */
    protected AbstractItemEffect(String id, String name, String description) {
        this(id, name, description, UsageType.UNLIMITED, -1);
    }
    
    /**
     * Creates an effect with a specific usage type and charges.
     */
    protected AbstractItemEffect(String id, String name, String description, 
                                  UsageType usageType, int maxCharges) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Effect ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Effect name cannot be null or empty");
        }
        
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.usageType = usageType != null ? usageType : UsageType.UNLIMITED;
        
        // Set up charges based on usage type
        if (this.usageType == UsageType.UNLIMITED || this.usageType == UsageType.PASSIVE) {
            this.maxCharges = -1;
            this.currentCharges = -1;
        } else {
            this.maxCharges = Math.max(1, maxCharges);
            this.currentCharges = this.maxCharges;
        }
        
        this.rechargeAmount = 0; // Default: full recharge
        this.consumed = false;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }
    
    @Override
    public UsageType getUsageType() {
        return usageType;
    }
    
    public void setUsageType(UsageType usageType) {
        this.usageType = usageType != null ? usageType : UsageType.UNLIMITED;
    }
    
    @Override
    public int getCurrentCharges() {
        return currentCharges;
    }
    
    @Override
    public int getMaxCharges() {
        return maxCharges;
    }
    
    public void setMaxCharges(int maxCharges) {
        this.maxCharges = maxCharges;
        if (currentCharges > maxCharges) {
            currentCharges = maxCharges;
        }
    }
    
    /**
     * Sets how many charges restore on reset.
     * 0 = full recharge, positive number = that many charges.
     */
    public void setRechargeAmount(int amount) {
        this.rechargeAmount = Math.max(0, amount);
    }
    
    public int getRechargeAmount() {
        return rechargeAmount;
    }
    
    @Override
    public boolean isUsable() {
        if (consumed) {
            return false;
        }
        
        return switch (usageType) {
            case UNLIMITED, PASSIVE -> true;
            case DAILY, LONG_REST, CHARGES, CONSUMABLE -> currentCharges > 0;
        };
    }
    
    /**
     * Checks if this consumable has been consumed.
     */
    public boolean isConsumed() {
        return consumed;
    }
    
    @Override
    public String use(Character user) {
        if (!isUsable()) {
            throw new IllegalStateException("Effect '" + name + "' cannot be used: no charges remaining");
        }
        
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Apply the effect (implemented by subclass)
        String result = applyEffect(user);
        
        // Consume charge if applicable
        if (usageType != UsageType.UNLIMITED && usageType != UsageType.PASSIVE) {
            currentCharges--;
            
            if (usageType == UsageType.CONSUMABLE && currentCharges <= 0) {
                consumed = true;
            }
        }
        
        return result;
    }
    
    /**
     * Applies the actual effect. Implemented by subclasses.
     */
    protected abstract String applyEffect(Character user);
    
    @Override
    public void resetOnLongRest() {
        if (usageType == UsageType.LONG_REST || usageType == UsageType.CHARGES) {
            recharge();
        }
    }
    
    @Override
    public void resetDaily() {
        if (usageType == UsageType.DAILY) {
            recharge();
        }
        // Long rest items also reset on daily (a new day implies rest)
        resetOnLongRest();
    }
    
    private void recharge() {
        if (consumed) {
            return; // Consumables don't recharge
        }
        
        if (rechargeAmount == 0) {
            // Full recharge
            currentCharges = maxCharges;
        } else {
            // Partial recharge
            currentCharges = Math.min(maxCharges, currentCharges + rechargeAmount);
        }
    }

    public void addCharges(int amount) {
        if (maxCharges > 0 && !consumed) {
            currentCharges = Math.min(maxCharges, currentCharges + amount);
        }
    }

    public void setCurrentCharges(int charges) {
        if (maxCharges > 0) {
            this.currentCharges = Math.max(0, Math.min(maxCharges, charges));
        }
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append(description).append("\n");
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s[id=%s, usage=%s, charges=%s]", 
                getClass().getSimpleName(), id, usageType.getDisplayName(), getChargeDisplay());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractItemEffect other = (AbstractItemEffect) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}