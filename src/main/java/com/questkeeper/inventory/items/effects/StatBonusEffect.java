package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that provides static bonuses to character stats.
 * 
 * Examples: +1 Armor, Ring of Protection, Cloak of Protection,
 * +2 Weapon, Ioun Stone of Fortitude
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class StatBonusEffect extends AbstractItemEffect {
    
    /**
     * Types of stats that can receive bonuses.
     */
    public enum StatType {
        ARMOR_CLASS("AC"),
        ATTACK_ROLLS("Attack"),
        DAMAGE_ROLLS("Damage"),
        SAVING_THROWS("Saving Throws"),
        SPELL_ATTACK("Spell Attack"),
        SPELL_DC("Spell Save DC"),
        INITIATIVE("Initiative"),
        
        // Specific saving throws
        STRENGTH_SAVE("Strength Saves"),
        DEXTERITY_SAVE("Dexterity Saves"),
        CONSTITUTION_SAVE("Constitution Saves"),
        INTELLIGENCE_SAVE("Intelligence Saves"),
        WISDOM_SAVE("Wisdom Saves"),
        CHARISMA_SAVE("Charisma Saves"),
        
        // Ability scores (for ability checks)
        STRENGTH("Strength"),
        DEXTERITY("Dexterity"),
        CONSTITUTION("Constitution"),
        INTELLIGENCE("Intelligence"),
        WISDOM("Wisdom"),
        CHARISMA("Charisma");
        
        private final String displayName;
        
        StatType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private StatType statType;
    private int bonusAmount;
    private boolean requiresAttunement;
    private boolean stacksWithOther;    // Can stack with similar effects
    
    /**
     * Creates a stat bonus effect.
     */
    public StatBonusEffect(String id, String name, StatType statType, int bonusAmount) {
        super(id, name, createDescription(statType, bonusAmount), UsageType.PASSIVE, -1);
        this.statType = statType;
        this.bonusAmount = bonusAmount;
        this.requiresAttunement = false;
        this.stacksWithOther = false;
    }
    
    private static String createDescription(StatType stat, int bonus) {
        String sign = bonus >= 0 ? "+" : "";
        return String.format("You gain %s%d to %s.", sign, bonus, stat.getDisplayName());
    }
    
    @Override
    protected String applyEffect(Character user) {
        // Passive effects don't have an "apply" action
        return String.format("%s benefits from %s%d to %s.", 
                user.getName(), bonusAmount >= 0 ? "+" : "", bonusAmount, 
                statType.getDisplayName());
    }
    
    /**
     * Gets the bonus amount for stat calculations.
     */
    public int getBonus() {
        return bonusAmount;
    }
    
    /**
     * Checks if this bonus applies to a specific stat type.
     */
    public boolean appliesTo(StatType type) {
        if (statType == type) {
            return true;
        }
        
        // SAVING_THROWS applies to all individual saves
        if (statType == StatType.SAVING_THROWS) {
            return switch (type) {
                case STRENGTH_SAVE, DEXTERITY_SAVE, CONSTITUTION_SAVE,
                     INTELLIGENCE_SAVE, WISDOM_SAVE, CHARISMA_SAVE -> true;
                default -> false;
            };
        }
        
        return false;
    }

    public StatType getStatType() {
        return statType;
    }
    
    public void setStatType(StatType statType) {
        this.statType = statType;
        setDescription(createDescription(this.statType, this.bonusAmount));
    }
    
    public int getBonusAmount() {
        return bonusAmount;
    }
    
    public void setBonusAmount(int bonusAmount) {
        this.bonusAmount = bonusAmount;
        setDescription(createDescription(this.statType, this.bonusAmount));
    }
    
    public boolean requiresAttunement() {
        return requiresAttunement;
    }
    
    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }
    
    public boolean stacksWithOther() {
        return stacksWithOther;
    }
    
    public void setStacksWithOther(boolean stacksWithOther) {
        this.stacksWithOther = stacksWithOther;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append(statType.getDisplayName()).append(": ");
        sb.append(bonusAmount >= 0 ? "+" : "").append(bonusAmount).append("\n");
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }

    /**
     * Creates a +1 Armor bonus effect.
     */
    public static StatBonusEffect createPlusOneArmor() {
        return new StatBonusEffect("plus_one_armor_effect", "+1 AC", 
                StatType.ARMOR_CLASS, 1);
    }
    
    /**
     * Creates a +2 Armor bonus effect.
     */
    public static StatBonusEffect createPlusTwoArmor() {
        return new StatBonusEffect("plus_two_armor_effect", "+2 AC", 
                StatType.ARMOR_CLASS, 2);
    }
    
    /**
     * Creates a +1 Weapon attack/damage bonus.
     */
    public static StatBonusEffect createPlusOneWeaponAttack() {
        return new StatBonusEffect("plus_one_attack_effect", "+1 Attack", 
                StatType.ATTACK_ROLLS, 1);
    }
    
    /**
     * Creates a +1 Weapon damage bonus (often paired with attack).
     */
    public static StatBonusEffect createPlusOneWeaponDamage() {
        return new StatBonusEffect("plus_one_damage_effect", "+1 Damage", 
                StatType.DAMAGE_ROLLS, 1);
    }
    
    /**
     * Creates a Ring of Protection effect (+1 AC and saves).
     */
    public static StatBonusEffect createRingOfProtectionAC() {
        StatBonusEffect effect = new StatBonusEffect("ring_protection_ac_effect", 
                "Protection", StatType.ARMOR_CLASS, 1);
        effect.setRequiresAttunement(true);
        return effect;
    }
    
    /**
     * Creates the save bonus portion of Ring of Protection.
     */
    public static StatBonusEffect createRingOfProtectionSaves() {
        StatBonusEffect effect = new StatBonusEffect("ring_protection_saves_effect", 
                "Protection", StatType.SAVING_THROWS, 1);
        effect.setRequiresAttunement(true);
        return effect;
    }
    
    /**
     * Creates a Cloak of Protection effect (+1 AC and saves).
     */
    public static StatBonusEffect createCloakOfProtection() {
        StatBonusEffect effect = new StatBonusEffect("cloak_protection_effect", 
                "Protection", StatType.ARMOR_CLASS, 1);
        effect.setRequiresAttunement(true);
        effect.setDescription("You gain +1 to AC and saving throws.");
        return effect;
    }
    
    /**
     * Creates a bonus to initiative.
     */
    public static StatBonusEffect createInitiativeBonus(int bonus) {
        return new StatBonusEffect("initiative_bonus_effect", "Quick Reflexes", 
                StatType.INITIATIVE, bonus);
    }
    
    /**
     * Creates a Wand of the War Mage effect (+X spell attack).
     */
    public static StatBonusEffect createWandOfWarMage(int bonus) {
        StatBonusEffect effect = new StatBonusEffect("wand_war_mage_effect", 
                "War Mage", StatType.SPELL_ATTACK, bonus);
        effect.setRequiresAttunement(true);
        effect.setDescription(String.format("You gain +%d to spell attack rolls.", bonus));
        return effect;
    }
}