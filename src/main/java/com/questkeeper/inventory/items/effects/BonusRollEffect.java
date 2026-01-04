package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.core.Dice;

/**
 * Effect that provides bonuses to dice rolls.
 * 
 * Can grant flat bonuses, bonus dice, advantage, or rerolls.
 * Examples: Jester's Lucky Coin, Harlequin's Favor, Stone of Good Luck
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class BonusRollEffect extends AbstractItemEffect {
    
    /**
     * Types of roll bonuses.
     */
    public enum BonusType {
        FLAT_BONUS,         // +1, +2, etc.
        BONUS_DICE,         // +1d4, +1d6, etc.
        ADVANTAGE,          // Roll twice, take higher
        REROLL,             // Reroll and take new result
        AUTO_SUCCESS        // Automatically succeed (Legendary Resistance style)
    }
    
    /**
     * What rolls this bonus applies to.
     */
    public enum AppliesTo {
        ANY,                // Any roll
        ATTACK,             // Attack rolls only
        SAVING_THROW,       // Saving throws only
        ABILITY_CHECK,      // Ability checks only
        DAMAGE,             // Damage rolls only
        SPECIFIC_SKILL      // Specific skill (set via skillName)
    }
    
    private BonusType bonusType;
    private AppliesTo appliesTo;
    private int flatBonus;              // For FLAT_BONUS
    private String bonusDice;           // For BONUS_DICE (e.g., "1d4")
    private String skillName;           // For SPECIFIC_SKILL
    private int selfDamage;             // Backlash damage (like Jester's Coin)
    private String selfDamageCondition; // When backlash occurs (e.g., "on tails")
    
    /**
     * Creates a simple flat bonus effect.
     */
    public BonusRollEffect(String id, String name, int flatBonus) {
        super(id, name, createDescription(BonusType.FLAT_BONUS, flatBonus, null), 
                UsageType.PASSIVE, -1);
        this.bonusType = BonusType.FLAT_BONUS;
        this.appliesTo = AppliesTo.ANY;
        this.flatBonus = flatBonus;
        this.bonusDice = null;
        this.selfDamage = 0;
    }
    
    /**
     * Creates a bonus dice effect.
     */
    public BonusRollEffect(String id, String name, String bonusDice, 
                           UsageType usageType, int maxCharges) {
        super(id, name, createDescription(BonusType.BONUS_DICE, 0, bonusDice), 
                usageType, maxCharges);
        this.bonusType = BonusType.BONUS_DICE;
        this.appliesTo = AppliesTo.ANY;
        this.flatBonus = 0;
        this.bonusDice = bonusDice;
        this.selfDamage = 0;
    }
    
    /**
     * Creates an advantage/reroll effect.
     */
    public BonusRollEffect(String id, String name, BonusType bonusType,
                           UsageType usageType, int maxCharges) {
        super(id, name, createDescription(bonusType, 0, null), usageType, maxCharges);
        this.bonusType = bonusType;
        this.appliesTo = AppliesTo.ANY;
        this.flatBonus = 0;
        this.bonusDice = null;
        this.selfDamage = 0;
    }
    
    private static String createDescription(BonusType type, int flat, String dice) {
        return switch (type) {
            case FLAT_BONUS -> String.format("Gain a +%d bonus to rolls.", flat);
            case BONUS_DICE -> String.format("Add %s to your roll.", dice);
            case ADVANTAGE -> "Roll twice and take the higher result.";
            case REROLL -> "Reroll and use the new result.";
            case AUTO_SUCCESS -> "Automatically succeed on the roll.";
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        StringBuilder result = new StringBuilder();
        
        switch (bonusType) {
            case FLAT_BONUS -> {
                result.append(String.format("%s gains +%d to their roll!", 
                        user.getName(), flatBonus));
            }
            case BONUS_DICE -> {
                int bonus = Dice.parse(bonusDice);
                result.append(String.format("%s rolls %s for an extra %d!", 
                        user.getName(), bonusDice, bonus));
                
                // Check for self-damage (like Jester's Coin)
                if (selfDamage > 0 && shouldTakeSelfDamage()) {
                    result.append(String.format(" But %s takes %d psychic damage!", 
                            user.getName(), selfDamage));
                    user.takeDamage(selfDamage);
                }
            }
            case ADVANTAGE -> {
                result.append(String.format("%s rolls with advantage!", user.getName()));
            }
            case REROLL -> {
                result.append(String.format("%s rerolls!", user.getName()));
            }
            case AUTO_SUCCESS -> {
                result.append(String.format("%s automatically succeeds!", user.getName()));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Determines if self-damage should occur (e.g., coin flip for Jester's Coin).
     */
    private boolean shouldTakeSelfDamage() {
        if (selfDamageCondition == null || selfDamageCondition.isEmpty()) {
            return false;
        }
        // Simple coin flip - 50% chance
        if (selfDamageCondition.equalsIgnoreCase("on tails") || 
            selfDamageCondition.equalsIgnoreCase("coin flip")) {
            return Dice.roll(2) == 1; // 1 = tails, 2 = heads
        }
        return false;
    }
    
    /**
     * Gets the actual bonus value for calculations.
     * For bonus dice, this rolls the dice.
     */
    public int getBonusValue() {
        return switch (bonusType) {
            case FLAT_BONUS -> flatBonus;
            case BONUS_DICE -> Dice.parse(bonusDice);
            default -> 0;
        };
    }
    
    public BonusType getBonusType() {
        return bonusType;
    }
    
    public void setBonusType(BonusType bonusType) {
        this.bonusType = bonusType;
    }
    
    public AppliesTo getAppliesTo() {
        return appliesTo;
    }
    
    public void setAppliesTo(AppliesTo appliesTo) {
        this.appliesTo = appliesTo;
    }
    
    public int getFlatBonus() {
        return flatBonus;
    }
    
    public void setFlatBonus(int flatBonus) {
        this.flatBonus = flatBonus;
    }
    
    public String getBonusDice() {
        return bonusDice;
    }
    
    public void setBonusDice(String bonusDice) {
        this.bonusDice = bonusDice;
    }
    
    public String getSkillName() {
        return skillName;
    }
    
    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
    
    public int getSelfDamage() {
        return selfDamage;
    }
    
    public void setSelfDamage(int selfDamage) {
        this.selfDamage = Math.max(0, selfDamage);
    }
    
    public String getSelfDamageCondition() {
        return selfDamageCondition;
    }
    
    public void setSelfDamageCondition(String condition) {
        this.selfDamageCondition = condition;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        
        switch (bonusType) {
            case FLAT_BONUS -> sb.append("Bonus: +").append(flatBonus).append("\n");
            case BONUS_DICE -> sb.append("Bonus: ").append(bonusDice).append("\n");
            case ADVANTAGE -> sb.append("Grants advantage\n");
            case REROLL -> sb.append("Allows reroll\n");
            case AUTO_SUCCESS -> sb.append("Automatic success\n");
        }
        
        if (appliesTo != AppliesTo.ANY) {
            sb.append("Applies to: ").append(appliesTo.name().toLowerCase().replace("_", " ")).append("\n");
        }
        
        if (selfDamage > 0) {
            sb.append("Risk: ").append(selfDamage).append(" damage ").append(selfDamageCondition).append("\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }

    /**
     * Creates the Jester's Lucky Coin from Muddlebrook.
     * +1d4 to a roll, but on tails take 1 psychic damage.
     */
    public static BonusRollEffect createJestersLuckyCoin() {
        BonusRollEffect effect = new BonusRollEffect("jesters_coin_effect", 
                "Jester's Luck", "1d4", UsageType.DAILY, 1);
        effect.setSelfDamage(1);
        effect.setSelfDamageCondition("on tails");
        effect.setDescription("Add 1d4 to any roll. Flip a coin - on tails, take 1 psychic damage.");
        return effect;
    }
    
    /**
     * Creates the Harlequin's Favor from Muddlebrook.
     * Grants advantage on one roll.
     */
    public static BonusRollEffect createHarlequinsFavor() {
        BonusRollEffect effect = new BonusRollEffect("harlequins_favor_effect",
                "Harlequin's Favor", BonusType.ADVANTAGE, UsageType.CONSUMABLE, 1);
        effect.setDescription("Gain advantage on one roll. The Machinist notices when you use this.");
        return effect;
    }
    
    /**
     * Creates a Stone of Good Luck (+1 to ability checks and saves).
     */
    public static BonusRollEffect createStoneOfGoodLuck() {
        BonusRollEffect effect = new BonusRollEffect("stone_good_luck_effect",
                "Good Luck", 1);
        effect.setAppliesTo(AppliesTo.ABILITY_CHECK);
        effect.setDescription("Gain +1 to ability checks and saving throws.");
        return effect;
    }
}