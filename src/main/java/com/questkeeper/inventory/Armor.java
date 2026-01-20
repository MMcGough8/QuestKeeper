package com.questkeeper.inventory;

/**
 * Represents armor and shields with D&D 5e defensive properties.
 * 
 * Armor provides AC bonuses and may have DEX modifiers, strength
 * requirements, and stealth disadvantage.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class Armor extends Item {

    /**
     * Armor categories that determine how DEX modifier is applied.
     */
    public enum ArmorCategory {
        LIGHT("Light Armor", true, -1),         // Full DEX mod
        MEDIUM("Medium Armor", true, 2),        // DEX mod capped at +2
        HEAVY("Heavy Armor", false, 0),         // No DEX mod
        SHIELD("Shield", false, 0);             // No DEX mod (just flat bonus)
        
        private final String displayName;
        private final boolean allowsDex;
        private final int maxDexBonus;  // -1 = unlimited
        
        ArmorCategory(String displayName, boolean allowsDex, int maxDexBonus) {
            this.displayName = displayName;
            this.allowsDex = allowsDex;
            this.maxDexBonus = maxDexBonus;
        }
        
        public String getDisplayName() { return displayName; }
        public boolean allowsDexBonus() { return allowsDex; }
        public int getMaxDexBonus() { return maxDexBonus; }
    }

    // Core armor stats
    private int baseAC;                     // Base armor class provided
    private ArmorCategory category;
    private int strengthRequirement;        // Minimum STR to wear without penalty (0 = none)
    private boolean stealthDisadvantage;    // Imposes disadvantage on Stealth checks
    
    // Magic armor bonuses
    private int acBonus;                    // Additional AC from magic enhancement

    /**
     * Creates armor with standard properties.
     */
    public Armor(String name, ArmorCategory category, int baseAC, 
                 double weight, int goldValue) {
        super(name, category == ArmorCategory.SHIELD ? ItemType.SHIELD : ItemType.ARMOR, 
              "", weight, goldValue);
        this.category = category;
        this.baseAC = Math.max(0, baseAC);
        this.strengthRequirement = 0;
        this.stealthDisadvantage = false;
        this.acBonus = 0;
    }

    /**
     * Creates armor with strength requirement and stealth penalty.
     */
    public Armor(String name, ArmorCategory category, int baseAC,
                 int strengthRequirement, boolean stealthDisadvantage,
                 double weight, int goldValue) {
        this(name, category, baseAC, weight, goldValue);
        this.strengthRequirement = Math.max(0, strengthRequirement);
        this.stealthDisadvantage = stealthDisadvantage;
    }

    /**
     * Protected copy constructor that preserves the original ID.
     */
    protected Armor(Armor original) {
        super(original.getId(), original.getName(),
              original.getCategory() == ArmorCategory.SHIELD ? ItemType.SHIELD : ItemType.ARMOR,
              original.getDescription(), original.getWeight(), original.getGoldValue());
        this.category = original.category;
        this.baseAC = original.baseAC;
        this.strengthRequirement = original.strengthRequirement;
        this.stealthDisadvantage = original.stealthDisadvantage;
        this.acBonus = original.acBonus;
        setRarity(original.getRarity());
    }

    /**
     * Private constructor for creating armor with an explicit ID.
     */
    private Armor(String id, String name, ArmorCategory category, int baseAC,
                  double weight, int goldValue) {
        super(id, name, category == ArmorCategory.SHIELD ? ItemType.SHIELD : ItemType.ARMOR,
              "", weight, goldValue);
        this.category = category;
        this.baseAC = Math.max(0, baseAC);
        this.strengthRequirement = 0;
        this.stealthDisadvantage = false;
        this.acBonus = 0;
    }

    /**
     * Creates armor with an explicit ID (for YAML-loaded items).
     */
    public static Armor createWithId(String id, String name, ArmorCategory category, int baseAC,
                                      double weight, int goldValue) {
        return new Armor(id, name, category, baseAC, weight, goldValue);
    }

    /**
     * Calculates the total AC this armor provides.
     * Does NOT include the wearer's DEX modifier - that's handled by Character.
     */
    public int getTotalBaseAC() {
        return baseAC + acBonus;
    }

    public int calculateAC(int dexModifier) {
        int ac = baseAC + acBonus;
        
        if (category.allowsDexBonus()) {
            int maxDex = category.getMaxDexBonus();
            if (maxDex < 0) {
                // Unlimited DEX bonus (light armor)
                ac += dexModifier;
            } else {
                // Capped DEX bonus (medium armor)
                ac += Math.min(dexModifier, maxDex);
            }
        }
        // Heavy armor and shields get no DEX bonus
        
        return ac;
    }

    public int getShieldBonus() {
        if (isShield()) {
            return baseAC + acBonus;
        }
        return 0;
    }

    public boolean isShield() {
        return category == ArmorCategory.SHIELD;
    }

    public boolean isLightArmor() {
        return category == ArmorCategory.LIGHT;
    }

    public boolean isMediumArmor() {
        return category == ArmorCategory.MEDIUM;
    }

    public boolean isHeavyArmor() {
        return category == ArmorCategory.HEAVY;
    }

    public boolean isMagical() {
        return acBonus > 0;
    }

    public boolean meetsStrengthRequirement(int strengthScore) {
        return strengthScore >= strengthRequirement;
    }

    /**
     * Gets the speed penalty for not meeting strength requirement.
     */
    public int getSpeedPenalty(int strengthScore) {
        if (strengthRequirement > 0 && strengthScore < strengthRequirement) {
            return 10; // Standard D&D 5e penalty
        }
        return 0;
    }

    public int getBaseAC() {
        return baseAC;
    }

    public void setBaseAC(int baseAC) {
        this.baseAC = Math.max(0, baseAC);
    }

    public ArmorCategory getCategory() {
        return category;
    }

    public void setCategory(ArmorCategory category) {
        this.category = category;
        setType(category == ArmorCategory.SHIELD ? ItemType.SHIELD : ItemType.ARMOR);
    }

    public int getStrengthRequirement() {
        return strengthRequirement;
    }

    public void setStrengthRequirement(int strengthRequirement) {
        this.strengthRequirement = Math.max(0, strengthRequirement);
    }

    public boolean hasStealthDisadvantage() {
        return stealthDisadvantage;
    }

    public void setStealthDisadvantage(boolean stealthDisadvantage) {
        this.stealthDisadvantage = stealthDisadvantage;
    }

    public int getAcBonus() {
        return acBonus;
    }

    public void setAcBonus(int acBonus) {
        this.acBonus = acBonus;
    }

    /**
     * Sets the magic enhancement bonus (for +X armor).
     */
    public void setMagicBonus(int bonus) {
        this.acBonus = bonus;
    }

    @Override
    public boolean isEquippable() {
        return true;
    }

    @Override
    public Item copy() {
        return new Armor(this);
    }

    /**
     * Creates a copy with a new unique ID (for loot duplication).
     */
    public Armor copyWithNewId() {
        Armor copy = new Armor(getName(), category, baseAC, 
                strengthRequirement, stealthDisadvantage,
                getWeight(), getGoldValue());
        copy.setDescription(getDescription());
        copy.setRarity(getRarity());
        copy.acBonus = this.acBonus;
        return copy;
    }

    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        if (getRarity() != Rarity.COMMON) {
            sb.append(" [").append(getRarity().getDisplayName()).append("]");
        }
        if (acBonus > 0) {
            sb.append(" +").append(acBonus);
        }
        sb.append("\n");
        
        sb.append(category.getDisplayName()).append("\n");
        
        if (isShield()) {
            sb.append("AC: +").append(getTotalBaseAC()).append("\n");
        } else {
            sb.append("AC: ").append(getTotalBaseAC());
            if (category.allowsDexBonus()) {
                if (category.getMaxDexBonus() < 0) {
                    sb.append(" + DEX");
                } else {
                    sb.append(" + DEX (max ").append(category.getMaxDexBonus()).append(")");
                }
            }
            sb.append("\n");
        }
        
        if (strengthRequirement > 0) {
            sb.append("Strength Required: ").append(strengthRequirement).append("\n");
        }
        
        if (stealthDisadvantage) {
            sb.append("Stealth: Disadvantage\n");
        }
        
        if (!getDescription().isEmpty()) {
            sb.append(getDescription()).append("\n");
        }
        
        sb.append(String.format("Weight: %.1f lb | Value: %d gp", getWeight(), getGoldValue()));
        
        return sb.toString();
    }

    @Override
    public String toString() {
        String bonusStr = acBonus > 0 ? " +" + acBonus : "";
        if (isShield()) {
            return String.format("%s%s [Shield, +%d AC]", getName(), bonusStr, getTotalBaseAC());
        }
        return String.format("%s%s [%s, AC %d]", 
                getName(), bonusStr, category.getDisplayName(), getTotalBaseAC());
    }

    // Light Armor
    public static Armor createPaddedArmor() {
        Armor a = new Armor("Padded Armor", ArmorCategory.LIGHT, 11, 8.0, 5);
        a.setStealthDisadvantage(true);
        return a;
    }

    public static Armor createLeatherArmor() {
        return new Armor("Leather Armor", ArmorCategory.LIGHT, 11, 10.0, 10);
    }

    public static Armor createStuddedLeather() {
        return new Armor("Studded Leather", ArmorCategory.LIGHT, 12, 13.0, 45);
    }

    // Medium Armor
    public static Armor createHide() {
        return new Armor("Hide Armor", ArmorCategory.MEDIUM, 12, 12.0, 10);
    }

    public static Armor createChainShirt() {
        return new Armor("Chain Shirt", ArmorCategory.MEDIUM, 13, 20.0, 50);
    }

    public static Armor createScaleMail() {
        Armor a = new Armor("Scale Mail", ArmorCategory.MEDIUM, 14, 45.0, 50);
        a.setStealthDisadvantage(true);
        return a;
    }

    public static Armor createBreastplate() {
        return new Armor("Breastplate", ArmorCategory.MEDIUM, 14, 20.0, 400);
    }

    public static Armor createHalfPlate() {
        Armor a = new Armor("Half Plate", ArmorCategory.MEDIUM, 15, 40.0, 750);
        a.setStealthDisadvantage(true);
        return a;
    }

    // Heavy Armor
    public static Armor createRingMail() {
        Armor a = new Armor("Ring Mail", ArmorCategory.HEAVY, 14, 40.0, 30);
        a.setStealthDisadvantage(true);
        return a;
    }

    public static Armor createChainMail() {
        Armor a = new Armor("Chain Mail", ArmorCategory.HEAVY, 16, 13, true, 55.0, 75);
        return a;
    }

    public static Armor createSplint() {
        Armor a = new Armor("Splint Armor", ArmorCategory.HEAVY, 17, 15, true, 60.0, 200);
        return a;
    }

    public static Armor createPlate() {
        Armor a = new Armor("Plate Armor", ArmorCategory.HEAVY, 18, 15, true, 65.0, 1500);
        return a;
    }

    // Shields
    public static Armor createShield() {
        return new Armor("Shield", ArmorCategory.SHIELD, 2, 6.0, 10);
    }

    /**
     * Creates a named shield with the standard +2 AC bonus.
     */
    public static Armor createShield(String name, double weight, int goldValue) {
        return new Armor(name, ArmorCategory.SHIELD, 2, weight, goldValue);
    }
}