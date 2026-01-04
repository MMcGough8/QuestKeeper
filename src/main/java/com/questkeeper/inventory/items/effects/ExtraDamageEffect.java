package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.core.Dice;

/**
 * Effect that adds extra damage to attacks.
 * 
 * Examples: Flame Tongue (+2d6 fire), Vicious Weapon (+2d6 on crit),
 * Holy Avenger (+2d10 vs fiends)
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class ExtraDamageEffect extends AbstractItemEffect {
    
    /**
     * When the extra damage applies.
     */
    public enum Trigger {
        ALWAYS,             // Every hit
        ON_CRITICAL,        // Only on critical hits
        ONCE_PER_TURN,      // First hit each turn
        CONDITIONAL         // Based on target type or other condition
    }
    
    private String damageDice;          // e.g., "2d6"
    private String damageType;          // e.g., "fire", "radiant"
    private Trigger trigger;
    private String conditionDescription; // For CONDITIONAL trigger
    private String targetRestriction;   // e.g., "fiend", "undead" or null for any
    private boolean requiresAttunement;
    
    /**
     * Creates a simple always-on extra damage effect.
     */
    public ExtraDamageEffect(String id, String name, String damageDice, String damageType) {
        super(id, name, createDescription(damageDice, damageType, Trigger.ALWAYS, null), 
                UsageType.PASSIVE, -1);
        this.damageDice = damageDice;
        this.damageType = damageType;
        this.trigger = Trigger.ALWAYS;
        this.conditionDescription = null;
        this.targetRestriction = null;
        this.requiresAttunement = false;
    }
    
    /**
     * Creates an extra damage effect with a specific trigger.
     */
    public ExtraDamageEffect(String id, String name, String damageDice, String damageType,
                              Trigger trigger) {
        super(id, name, createDescription(damageDice, damageType, trigger, null), 
                UsageType.PASSIVE, -1);
        this.damageDice = damageDice;
        this.damageType = damageType;
        this.trigger = trigger;
        this.conditionDescription = null;
        this.targetRestriction = null;
        this.requiresAttunement = false;
    }
    
    private static String createDescription(String dice, String type, Trigger trigger, String condition) {
        String base = String.format("Deal an extra %s %s damage", dice, type);
        return switch (trigger) {
            case ALWAYS -> base + " on every hit.";
            case ON_CRITICAL -> base + " on critical hits.";
            case ONCE_PER_TURN -> base + " on your first hit each turn.";
            case CONDITIONAL -> base + (condition != null ? " " + condition : " under certain conditions.") ;
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        int damage = Dice.parse(damageDice);
        return String.format("%s deals an extra %d %s damage!", 
                user.getName(), damage, damageType);
    }

    public int rollExtraDamage() {
        return Dice.parse(damageDice);
    }
    

    public boolean shouldApply(boolean isCritical, boolean isFirstHitThisTurn, String targetType) {
        // Check trigger condition
        boolean triggerMet = switch (trigger) {
            case ALWAYS -> true;
            case ON_CRITICAL -> isCritical;
            case ONCE_PER_TURN -> isFirstHitThisTurn;
            case CONDITIONAL -> true; // Checked separately
        };
        
        if (!triggerMet) {
            return false;
        }
        
        // Check target restriction
        if (targetRestriction != null && targetType != null) {
            return targetRestriction.equalsIgnoreCase(targetType);
        }
        
        return true;
    }
 
    public String getDamageDice() {
        return damageDice;
    }
    
    public void setDamageDice(String damageDice) {
        this.damageDice = damageDice;
    }
    
    public String getDamageType() {
        return damageType;
    }
    
    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }
    
    public Trigger getTrigger() {
        return trigger;
    }
    
    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }
    
    public String getConditionDescription() {
        return conditionDescription;
    }
    
    public void setConditionDescription(String conditionDescription) {
        this.conditionDescription = conditionDescription;
    }
    
    public String getTargetRestriction() {
        return targetRestriction;
    }
    
    public void setTargetRestriction(String targetRestriction) {
        this.targetRestriction = targetRestriction;
    }
    
    public boolean requiresAttunement() {
        return requiresAttunement;
    }
    
    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append("Extra Damage: ").append(damageDice).append(" ").append(damageType).append("\n");
        
        switch (trigger) {
            case ALWAYS -> sb.append("Applies on every hit\n");
            case ON_CRITICAL -> sb.append("Applies on critical hits\n");
            case ONCE_PER_TURN -> sb.append("Applies once per turn\n");
            case CONDITIONAL -> {
                if (conditionDescription != null) {
                    sb.append("Condition: ").append(conditionDescription).append("\n");
                }
            }
        }
        
        if (targetRestriction != null) {
            sb.append("Against: ").append(targetRestriction).append(" only\n");
        }
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }

    /**
     * Creates a Flame Tongue effect (+2d6 fire damage).
     */
    public static ExtraDamageEffect createFlameTongue() {
        ExtraDamageEffect effect = new ExtraDamageEffect("flame_tongue_effect",
                "Flame Tongue", "2d6", "fire");
        effect.setRequiresAttunement(true);
        effect.setDescription("While the sword is ablaze, it deals an extra 2d6 fire damage to any target it hits.");
        return effect;
    }
    
    /**
     * Creates a Vicious Weapon effect (+2d6 on crits).
     */
    public static ExtraDamageEffect createViciousWeapon() {
        ExtraDamageEffect effect = new ExtraDamageEffect("vicious_weapon_effect",
                "Vicious Strike", "2d6", "weapon", Trigger.ON_CRITICAL);
        effect.setDescription("When you roll a 20 on an attack roll, the target takes an extra 2d6 damage.");
        return effect;
    }
    
    /**
     * Creates a Sneak Attack style effect (once per turn).
     */
    public static ExtraDamageEffect createSneakAttackStyle(int diceCount) {
        ExtraDamageEffect effect = new ExtraDamageEffect("sneak_attack_effect",
                "Sneak Attack", diceCount + "d6", "weapon", Trigger.ONCE_PER_TURN);
        effect.setConditionDescription("when you have advantage or an ally is within 5 feet of the target");
        return effect;
    }
    
    /**
     * Creates a Holy Avenger effect (+2d10 vs fiends/undead).
     */
    public static ExtraDamageEffect createHolyAvenger() {
        ExtraDamageEffect effect = new ExtraDamageEffect("holy_avenger_effect",
                "Holy Avenger", "2d10", "radiant", Trigger.CONDITIONAL);
        effect.setTargetRestriction("fiend");
        effect.setConditionDescription("against fiends and undead");
        effect.setRequiresAttunement(true);
        return effect;
    }
}