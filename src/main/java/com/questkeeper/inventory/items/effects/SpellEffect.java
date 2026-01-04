package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that allows casting a spell or spell-like ability.
 * 
 * Examples: Featherfall Bookmark, Wand of Fireballs, Ring of Invisibility
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class SpellEffect extends AbstractItemEffect {
    
    private String spellName;           // Name of the spell
    private int spellLevel;             // Spell level (0 for cantrips)
    private String savingThrow;         // Required save (e.g., "DEX", "WIS") or null
    private int saveDC;                 // DC for the saving throw
    private String damageOrHealing;     // Dice expression for damage/healing or null
    private String damageType;          // Type of damage if applicable
    private int duration;               // Duration in rounds (0 = instant)
    private boolean concentration;      // Requires concentration
    private boolean selfOnly;           // Can only target self
    private int range;                  // Range in feet (0 = self, -1 = touch)
    
    /**
     * Creates a simple self-targeting spell effect.
     */
    public SpellEffect(String id, String name, String spellName) {
        this(id, name, spellName, UsageType.DAILY, 1);
    }
    
    /**
     * Creates a spell effect with usage limits.
     */
    public SpellEffect(String id, String name, String spellName,
                       UsageType usageType, int maxCharges) {
        super(id, name, "Cast " + spellName + ".", usageType, maxCharges);
        this.spellName = spellName;
        this.spellLevel = 1;
        this.savingThrow = null;
        this.saveDC = 0;
        this.damageOrHealing = null;
        this.damageType = null;
        this.duration = 0;
        this.concentration = false;
        this.selfOnly = true;
        this.range = 0;
    }
    
    @Override
    protected String applyEffect(Character user) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s casts %s!", user.getName(), spellName));
        
        if (damageOrHealing != null && !damageOrHealing.isEmpty()) {
            int value = com.questkeeper.core.Dice.parse(damageOrHealing);
            if (damageType != null) {
                result.append(String.format(" (%d %s damage)", value, damageType));
            } else {
                result.append(String.format(" (%d healing)", value));
            }
        }
        
        if (duration > 0) {
            result.append(String.format(" Duration: %d rounds.", duration));
        }
        
        return result.toString();
    }
    
    /**
     * Uses the spell on a specific target.
     */
    @Override
    public String use(Character user, Character target) {
        if (!isUsable()) {
            throw new IllegalStateException("Effect '" + getName() + "' cannot be used: no charges remaining");
        }
        
        if (selfOnly && target != user) {
            throw new IllegalArgumentException(spellName + " can only target the caster.");
        }
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s casts %s", user.getName(), spellName));
        
        if (target != user) {
            result.append(String.format(" on %s", target.getName()));
        }
        result.append("!");
        
        if (damageOrHealing != null && !damageOrHealing.isEmpty()) {
            int value = com.questkeeper.core.Dice.parse(damageOrHealing);
            if (damageType != null) {
                result.append(String.format(" (%d %s damage)", value, damageType));
                target.takeDamage(value);
            } else {
                result.append(String.format(" (%d healing)", value));
                target.heal(value);
            }
        }
        
        // Consume charge
        if (getUsageType() != UsageType.UNLIMITED && getUsageType() != UsageType.PASSIVE) {
            // Manually decrement since we're overriding the parent use()
            setCurrentCharges(getCurrentCharges() - 1);
        }
        
        return result.toString();
    }
    
    public String getSpellName() {
        return spellName;
    }
    
    public void setSpellName(String spellName) {
        this.spellName = spellName;
        setDescription("Cast " + spellName + ".");
    }
    
    public int getSpellLevel() {
        return spellLevel;
    }
    
    public void setSpellLevel(int spellLevel) {
        this.spellLevel = Math.max(0, Math.min(9, spellLevel));
    }
    
    public String getSavingThrow() {
        return savingThrow;
    }
    
    public void setSavingThrow(String savingThrow) {
        this.savingThrow = savingThrow;
    }
    
    public int getSaveDC() {
        return saveDC;
    }
    
    public void setSaveDC(int saveDC) {
        this.saveDC = Math.max(0, saveDC);
    }
    
    public String getDamageOrHealing() {
        return damageOrHealing;
    }
    
    public void setDamageOrHealing(String damageOrHealing) {
        this.damageOrHealing = damageOrHealing;
    }
    
    public String getDamageType() {
        return damageType;
    }
    
    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = Math.max(0, duration);
    }
    
    public boolean requiresConcentration() {
        return concentration;
    }
    
    public void setConcentration(boolean concentration) {
        this.concentration = concentration;
    }
    
    public boolean isSelfOnly() {
        return selfOnly;
    }
    
    public void setSelfOnly(boolean selfOnly) {
        this.selfOnly = selfOnly;
    }
    
    public int getRange() {
        return range;
    }
    
    public void setRange(int range) {
        this.range = range;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append("Spell: ").append(spellName);
        if (spellLevel > 0) {
            sb.append(" (").append(spellLevel).append(ordinalSuffix(spellLevel)).append(" level)");
        } else {
            sb.append(" (cantrip)");
        }
        sb.append("\n");
        
        if (range == 0) {
            sb.append("Range: Self\n");
        } else if (range == -1) {
            sb.append("Range: Touch\n");
        } else {
            sb.append("Range: ").append(range).append(" feet\n");
        }
        
        if (savingThrow != null) {
            sb.append("Save: DC ").append(saveDC).append(" ").append(savingThrow).append("\n");
        }
        
        if (damageOrHealing != null) {
            if (damageType != null) {
                sb.append("Damage: ").append(damageOrHealing).append(" ").append(damageType).append("\n");
            } else {
                sb.append("Healing: ").append(damageOrHealing).append("\n");
            }
        }
        
        if (duration > 0) {
            sb.append("Duration: ").append(duration).append(" rounds");
            if (concentration) {
                sb.append(" (concentration)");
            }
            sb.append("\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }
    
    private String ordinalSuffix(int n) {
        if (n >= 11 && n <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    /**
     * Creates the Featherfall Bookmark from Muddlebrook.
     */
    public static SpellEffect createFeatherfallBookmark() {
        SpellEffect effect = new SpellEffect("featherfall_bookmark_effect",
                "Featherfall", "Feather Fall", UsageType.DAILY, 1);
        effect.setSpellLevel(1);
        effect.setSelfOnly(true);
        effect.setDuration(10); // 1 minute = 10 rounds
        effect.setDescription("Cast Feather Fall on yourself, descending 60 feet per round without taking falling damage.");
        return effect;
    }
    
    /**
     * Creates a Wand of Fireballs.
     */
    public static SpellEffect createWandOfFireballs() {
        SpellEffect effect = new SpellEffect("wand_fireballs_effect",
                "Fireball", "Fireball", UsageType.CHARGES, 7);
        effect.setSpellLevel(3);
        effect.setSelfOnly(false);
        effect.setRange(150);
        effect.setSavingThrow("DEX");
        effect.setSaveDC(15);
        effect.setDamageOrHealing("8d6");
        effect.setDamageType("fire");
        effect.setRechargeAmount(1); // Regains 1d6+1 charges at dawn, simplified to 1
        effect.setDescription("Expend 1 charge to cast Fireball (8d6 fire damage, DC 15 DEX save for half).");
        return effect;
    }
    
    /**
     * Creates a Ring of Invisibility.
     */
    public static SpellEffect createRingOfInvisibility() {
        SpellEffect effect = new SpellEffect("ring_invisibility_effect",
                "Invisibility", "Invisibility", UsageType.UNLIMITED, -1);
        effect.setSpellLevel(2);
        effect.setSelfOnly(true);
        effect.setDuration(0); // Until you attack or cast a spell
        effect.setDescription("Turn invisible. The effect ends when you attack or cast a spell.");
        return effect;
    }
    
    /**
     * Creates a Potion of Healing effect.
     */
    public static SpellEffect createPotionOfHealing() {
        SpellEffect effect = new SpellEffect("potion_healing_effect",
                "Healing", "Cure Wounds", UsageType.CONSUMABLE, 1);
        effect.setSpellLevel(1);
        effect.setSelfOnly(true);
        effect.setDamageOrHealing("2d4+2");
        effect.setDescription("Drink to regain 2d4+2 hit points.");
        return effect;
    }
}