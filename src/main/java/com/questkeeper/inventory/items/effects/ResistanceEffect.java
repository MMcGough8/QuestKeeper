package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that grants resistance, immunity, or vulnerability to damage types.
 * 
 * Resistance halves damage, immunity negates it, vulnerability doubles it.
 * Examples: Ring of Fire Resistance, Armor of Invulnerability, 
 * Periapt of Proof Against Poison
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class ResistanceEffect extends AbstractItemEffect {
    
    /**
     * Level of resistance.
     */
    public enum ResistanceLevel {
        VULNERABILITY("Vulnerability", 2.0),    // Double damage
        NORMAL("Normal", 1.0),                  // Normal damage
        RESISTANCE("Resistance", 0.5),          // Half damage
        IMMUNITY("Immunity", 0.0);              // No damage
        
        private final String displayName;
        private final double damageMultiplier;
        
        ResistanceLevel(String displayName, double damageMultiplier) {
            this.displayName = displayName;
            this.damageMultiplier = damageMultiplier;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public double getDamageMultiplier() {
            return damageMultiplier;
        }
    }
    
    /**
     * Common damage types in D&D.
     */
    public enum DamageType {
        // Physical
        BLUDGEONING("Bludgeoning"),
        PIERCING("Piercing"),
        SLASHING("Slashing"),
        
        // Elemental
        ACID("Acid"),
        COLD("Cold"),
        FIRE("Fire"),
        LIGHTNING("Lightning"),
        THUNDER("Thunder"),
        
        // Other
        FORCE("Force"),
        NECROTIC("Necrotic"),
        POISON("Poison"),
        PSYCHIC("Psychic"),
        RADIANT("Radiant"),
        
        // Special categories
        NONMAGICAL_PHYSICAL("Nonmagical Weapons"),
        ALL("All Damage");
        
        private final String displayName;
        
        DamageType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private ResistanceLevel resistanceLevel;
    private DamageType damageType;
    private boolean requiresAttunement;
    private boolean onlyNonmagical;     // For physical damage: only affects nonmagical
    
    
    public ResistanceEffect(String id, String name, DamageType damageType, 
                             ResistanceLevel level) {
        super(id, name, createDescription(damageType, level), UsageType.PASSIVE, -1);
        this.damageType = damageType;
        this.resistanceLevel = level;
        this.requiresAttunement = false;
        this.onlyNonmagical = false;
    }
    
    private static String createDescription(DamageType type, ResistanceLevel level) {
        return switch (level) {
            case VULNERABILITY -> String.format("You are vulnerable to %s damage (take double damage).", 
                    type.getDisplayName());
            case NORMAL -> String.format("You take normal %s damage.", type.getDisplayName());
            case RESISTANCE -> String.format("You have resistance to %s damage (take half damage).", 
                    type.getDisplayName());
            case IMMUNITY -> String.format("You are immune to %s damage.", type.getDisplayName());
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        return String.format("%s has %s to %s damage!", 
                user.getName(), resistanceLevel.getDisplayName(), damageType.getDisplayName());
    }
    
    public int calculateModifiedDamage(int incomingDamage, boolean isMagical) {
        // Check if this resistance applies
        if (onlyNonmagical && isMagical) {
            return incomingDamage;
        }
        
        return (int) (incomingDamage * resistanceLevel.getDamageMultiplier());
    }

    public boolean appliesTo(DamageType type, boolean isMagical) {
        if (damageType == DamageType.ALL) {
            return true;
        }
        
        if (damageType == DamageType.NONMAGICAL_PHYSICAL && !isMagical) {
            return type == DamageType.BLUDGEONING || 
                   type == DamageType.PIERCING || 
                   type == DamageType.SLASHING;
        }
        
        if (onlyNonmagical && isMagical) {
            return false;
        }
        
        return damageType == type;
    }

    public ResistanceLevel getResistanceLevel() {
        return resistanceLevel;
    }
    
    public void setResistanceLevel(ResistanceLevel resistanceLevel) {
        this.resistanceLevel = resistanceLevel;
        setDescription(createDescription(this.damageType, this.resistanceLevel));
    }
    
    public DamageType getDamageType() {
        return damageType;
    }
    
    public void setDamageType(DamageType damageType) {
        this.damageType = damageType;
        setDescription(createDescription(this.damageType, this.resistanceLevel));
    }
    
    public boolean requiresAttunement() {
        return requiresAttunement;
    }
    
    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }
    
    public boolean isOnlyNonmagical() {
        return onlyNonmagical;
    }
    
    public void setOnlyNonmagical(boolean onlyNonmagical) {
        this.onlyNonmagical = onlyNonmagical;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append(resistanceLevel.getDisplayName()).append(" to ");
        sb.append(damageType.getDisplayName());
        if (onlyNonmagical) {
            sb.append(" (nonmagical only)");
        }
        sb.append("\n");
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }
 
    /**
     * Creates a Ring of Fire Resistance.
     */
    public static ResistanceEffect createRingOfFireResistance() {
        ResistanceEffect effect = new ResistanceEffect("ring_fire_resist_effect",
                "Fire Resistance", DamageType.FIRE, ResistanceLevel.RESISTANCE);
        effect.setRequiresAttunement(true);
        return effect;
    }
    
    /**
     * Creates a Ring of Cold Resistance.
     */
    public static ResistanceEffect createRingOfColdResistance() {
        ResistanceEffect effect = new ResistanceEffect("ring_cold_resist_effect",
                "Cold Resistance", DamageType.COLD, ResistanceLevel.RESISTANCE);
        effect.setRequiresAttunement(true);
        return effect;
    }
    
    /**
     * Creates a Ring of Lightning Resistance.
     */
    public static ResistanceEffect createRingOfLightningResistance() {
        ResistanceEffect effect = new ResistanceEffect("ring_lightning_resist_effect",
                "Lightning Resistance", DamageType.LIGHTNING, ResistanceLevel.RESISTANCE);
        effect.setRequiresAttunement(true);
        return effect;
    }
    
    /**
     * Creates Periapt of Proof Against Poison (poison immunity).
     */
    public static ResistanceEffect createPeriaptOfProofAgainstPoison() {
        ResistanceEffect effect = new ResistanceEffect("periapt_poison_effect",
                "Poison Immunity", DamageType.POISON, ResistanceLevel.IMMUNITY);
        effect.setDescription("You are immune to poison damage and the poisoned condition.");
        return effect;
    }
    
    /**
     * Creates Armor of Invulnerability effect (immune to nonmagical damage).
     */
    public static ResistanceEffect createArmorOfInvulnerability() {
        ResistanceEffect effect = new ResistanceEffect("armor_invuln_effect",
                "Invulnerability", DamageType.NONMAGICAL_PHYSICAL, ResistanceLevel.IMMUNITY);
        effect.setRequiresAttunement(true);
        effect.setOnlyNonmagical(true);
        effect.setDescription("You are immune to nonmagical damage.");
        return effect;
    }
    
    /**
     * Creates a Brooch of Shielding (force resistance + magic missile immunity).
     */
    public static ResistanceEffect createBroochOfShieldingForce() {
        ResistanceEffect effect = new ResistanceEffect("brooch_shielding_force_effect",
                "Force Resistance", DamageType.FORCE, ResistanceLevel.RESISTANCE);
        effect.setDescription("You have resistance to force damage and are immune to Magic Missile.");
        return effect;
    }
}