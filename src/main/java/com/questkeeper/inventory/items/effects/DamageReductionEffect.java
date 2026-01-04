package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that reduces incoming damage.
 * 
 * Examples: Sigil Shard, Adamantine Armor, Brooch of Shielding
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class DamageReductionEffect extends AbstractItemEffect {
    
    /**
     * Types of damage reduction.
     */
    public enum ReductionType {
        FLAT,               // Reduce by fixed amount
        PERCENTAGE,         // Reduce by percentage
        HALVE,              // Halve the damage
        NEGATE_CRIT         // Turn critical hits into normal hits
    }
    
    private ReductionType reductionType;
    private int reductionAmount;        // Flat amount or percentage
    private String damageTypeRestriction; // null = all damage, or specific type like "fire"
    private boolean isReaction;         // Requires reaction to use
    
    /**
     * Creates a flat damage reduction effect.
     */
    public DamageReductionEffect(String id, String name, int reductionAmount) {
        this(id, name, reductionAmount, UsageType.PASSIVE, -1);
    }
    
    /**
     * Creates a damage reduction effect with usage limits.
     */
    public DamageReductionEffect(String id, String name, int reductionAmount,
                                  UsageType usageType, int maxCharges) {
        super(id, name, createDescription(ReductionType.FLAT, reductionAmount, null), 
                usageType, maxCharges);
        this.reductionType = ReductionType.FLAT;
        this.reductionAmount = Math.max(0, reductionAmount);
        this.damageTypeRestriction = null;
        this.isReaction = false;
    }
    
    /**
     * Creates a damage reduction effect with a specific type.
     */
    public DamageReductionEffect(String id, String name, ReductionType reductionType,
                                  int amount, UsageType usageType, int maxCharges) {
        super(id, name, createDescription(reductionType, amount, null), usageType, maxCharges);
        this.reductionType = reductionType;
        this.reductionAmount = Math.max(0, amount);
        this.damageTypeRestriction = null;
        this.isReaction = false;
    }
    
    private static String createDescription(ReductionType type, int amount, String damageType) {
        String typeStr = damageType != null ? damageType + " " : "";
        return switch (type) {
            case FLAT -> String.format("Reduce %sdamage taken by %d.", typeStr, amount);
            case PERCENTAGE -> String.format("Reduce %sdamage taken by %d%%.", typeStr, amount);
            case HALVE -> String.format("Halve %sdamage taken.", typeStr);
            case NEGATE_CRIT -> "Critical hits against you become normal hits.";
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        return switch (reductionType) {
            case FLAT -> String.format("%s's defenses reduce damage by %d!", 
                    user.getName(), reductionAmount);
            case PERCENTAGE -> String.format("%s's defenses reduce damage by %d%%!", 
                    user.getName(), reductionAmount);
            case HALVE -> String.format("%s's defenses halve the damage!", user.getName());
            case NEGATE_CRIT -> String.format("%s's armor turns the critical into a normal hit!", 
                    user.getName());
        };
    }
    
    /**
     * Calculates the actual damage after reduction.
     */
    public int calculateReducedDamage(int incomingDamage, String damageType, boolean isCritical) {
        // Check if this effect applies to this damage type
        if (damageTypeRestriction != null && 
            !damageTypeRestriction.equalsIgnoreCase(damageType)) {
            return incomingDamage;
        }
        
        // Check if usable (for limited-use effects)
        if (!isUsable() && getUsageType() != UsageType.PASSIVE) {
            return incomingDamage;
        }
        
        int reducedDamage = switch (reductionType) {
            case FLAT -> Math.max(0, incomingDamage - reductionAmount);
            case PERCENTAGE -> Math.max(0, incomingDamage - (incomingDamage * reductionAmount / 100));
            case HALVE -> incomingDamage / 2;
            case NEGATE_CRIT -> isCritical ? incomingDamage / 2 : incomingDamage; // Crits do double, so halving returns to normal
        };
        
        return reducedDamage;
    }
    
    public int getDamageReduced(int incomingDamage, String damageType, boolean isCritical) {
        return incomingDamage - calculateReducedDamage(incomingDamage, damageType, isCritical);
    }
  
    public ReductionType getReductionType() {
        return reductionType;
    }
    
    public void setReductionType(ReductionType reductionType) {
        this.reductionType = reductionType;
    }
    
    public int getReductionAmount() {
        return reductionAmount;
    }
    
    public void setReductionAmount(int reductionAmount) {
        this.reductionAmount = Math.max(0, reductionAmount);
    }
    
    public String getDamageTypeRestriction() {
        return damageTypeRestriction;
    }
    
    public void setDamageTypeRestriction(String damageType) {
        this.damageTypeRestriction = damageType;
    }
    
    public boolean isReaction() {
        return isReaction;
    }
    
    public void setReaction(boolean isReaction) {
        this.isReaction = isReaction;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        
        switch (reductionType) {
            case FLAT -> sb.append("Reduces damage by ").append(reductionAmount).append("\n");
            case PERCENTAGE -> sb.append("Reduces damage by ").append(reductionAmount).append("%\n");
            case HALVE -> sb.append("Halves damage taken\n");
            case NEGATE_CRIT -> sb.append("Negates critical hits\n");
        }
        
        if (damageTypeRestriction != null) {
            sb.append("Only affects ").append(damageTypeRestriction).append(" damage\n");
        }
        
        if (isReaction) {
            sb.append("Requires reaction\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }

    /**
     * Creates the Sigil Shard from Muddlebrook.
     * Reduce damage by 2 once per day.
     */
    public static DamageReductionEffect createSigilShard() {
        DamageReductionEffect effect = new DamageReductionEffect("sigil_shard_effect",
                "Sigil Ward", 2, UsageType.DAILY, 1);
        effect.setReaction(true);
        effect.setDescription("As a reaction, reduce damage from one attack by 2.");
        return effect;
    }
    
    /**
     * Creates an Adamantine Armor effect (crits become normal hits).
     */
    public static DamageReductionEffect createAdamantineArmor() {
        DamageReductionEffect effect = new DamageReductionEffect("adamantine_effect",
                "Adamantine", DamageReductionEffect.ReductionType.NEGATE_CRIT, 
                0, UsageType.PASSIVE, -1);
        effect.setDescription("Critical hits against you become normal hits.");
        return effect;
    }
    
    /**
     * Creates a Brooch of Shielding (immune to magic missile, resist force).
     */
    public static DamageReductionEffect createBroochOfShielding() {
        DamageReductionEffect effect = new DamageReductionEffect("brooch_shielding_effect",
                "Force Ward", DamageReductionEffect.ReductionType.HALVE,
                0, UsageType.PASSIVE, -1);
        effect.setDamageTypeRestriction("force");
        effect.setDescription("You have resistance to force damage.");
        return effect;
    }
}