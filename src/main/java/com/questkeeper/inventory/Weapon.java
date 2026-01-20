package com.questkeeper.inventory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents a weapon item with D&D 5e combat properties.
 * 
 * Weapons have damage dice, damage types, and special properties
 * like finesse, versatile, two-handed, etc.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class Weapon extends Item {

    /**
     * Damage types for weapons.
     */
    public enum DamageType {
        BLUDGEONING("Bludgeoning"),
        PIERCING("Piercing"),
        SLASHING("Slashing"),
        FIRE("Fire"),
        COLD("Cold"),
        LIGHTNING("Lightning"),
        ACID("Acid"),
        POISON("Poison"),
        NECROTIC("Necrotic"),
        RADIANT("Radiant"),
        FORCE("Force"),
        PSYCHIC("Psychic"),
        THUNDER("Thunder");
        
        private final String displayName;
        
        DamageType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }

    /**
     * Weapon categories for proficiency.
     */
    public enum WeaponCategory {
        SIMPLE_MELEE("Simple Melee"),
        SIMPLE_RANGED("Simple Ranged"),
        MARTIAL_MELEE("Martial Melee"),
        MARTIAL_RANGED("Martial Ranged");
        
        private final String displayName;
        
        WeaponCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        public boolean isSimple() {
            return this == SIMPLE_MELEE || this == SIMPLE_RANGED;
        }
        
        public boolean isMartial() {
            return this == MARTIAL_MELEE || this == MARTIAL_RANGED;
        }
        
        public boolean isMelee() {
            return this == SIMPLE_MELEE || this == MARTIAL_MELEE;
        }
        
        public boolean isRanged() {
            return this == SIMPLE_RANGED || this == MARTIAL_RANGED;
        }
    }

    /**
     * Special weapon properties per D&D 5e rules.
     */
    public enum WeaponProperty {
        AMMUNITION("Ammunition"),       // Requires ammo to fire
        FINESSE("Finesse"),             // Can use DEX instead of STR
        HEAVY("Heavy"),                 // Small creatures have disadvantage
        LIGHT("Light"),                 // Good for two-weapon fighting
        LOADING("Loading"),             // Only one attack per action
        REACH("Reach"),                 // +5 ft reach
        THROWN("Thrown"),               // Can be thrown as ranged attack
        TWO_HANDED("Two-Handed"),       // Requires two hands
        VERSATILE("Versatile"),         // Can be used one or two-handed
        SPECIAL("Special"),             // Has unique rules
        SILVERED("Silvered"),           // Overcomes some resistances
        MAGICAL("Magical");             // Magical damage
        
        private final String displayName;
        
        WeaponProperty(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }

    // Core weapon stats
    private int damageDiceCount;        // Number of damage dice (e.g., 2 for 2d6)
    private int damageDieSize;          // Size of damage die (e.g., 6 for d6)
    private DamageType damageType;
    private WeaponCategory category;
    private final Set<WeaponProperty> properties;
    
    // Range (for thrown/ranged weapons)
    private int normalRange;            // Normal range in feet (0 for pure melee)
    private int longRange;              // Long range in feet (attacks at disadvantage)
    
    // Versatile damage (when used two-handed)
    private int versatileDieSize;       // Die size when used two-handed
    
    // Magic weapon bonuses
    private int attackBonus;            // Bonus to attack rolls
    private int damageBonus;            // Bonus to damage rolls

    /**
     * Creates a basic melee weapon.
     */
    public Weapon(String name, int damageDiceCount, int damageDieSize, 
                  DamageType damageType, WeaponCategory category,
                  double weight, int goldValue) {
        super(name, ItemType.WEAPON, "", weight, goldValue);
        this.damageDiceCount = Math.max(1, damageDiceCount);
        this.damageDieSize = Math.max(1, damageDieSize);
        this.damageType = damageType;
        this.category = category;
        this.properties = EnumSet.noneOf(WeaponProperty.class);
        this.normalRange = 0;
        this.longRange = 0;
        this.versatileDieSize = 0;
        this.attackBonus = 0;
        this.damageBonus = 0;
    }

    /**
     * Creates a weapon with range (ranged or thrown).
     */
    public Weapon(String name, int damageDiceCount, int damageDieSize,
                  DamageType damageType, WeaponCategory category,
                  int normalRange, int longRange,
                  double weight, int goldValue) {
        this(name, damageDiceCount, damageDieSize, damageType, category, weight, goldValue);
        this.normalRange = Math.max(0, normalRange);
        this.longRange = Math.max(normalRange, longRange);
    }

    /**
     * Protected copy constructor that preserves the original ID.
     */
    protected Weapon(Weapon original) {
        super(original.getId(), original.getName(), ItemType.WEAPON, 
              original.getDescription(), original.getWeight(), original.getGoldValue());
        this.damageDiceCount = original.damageDiceCount;
        this.damageDieSize = original.damageDieSize;
        this.damageType = original.damageType;
        this.category = original.category;
        this.properties = EnumSet.copyOf(original.properties);
        this.normalRange = original.normalRange;
        this.longRange = original.longRange;
        this.versatileDieSize = original.versatileDieSize;
        this.attackBonus = original.attackBonus;
        this.damageBonus = original.damageBonus;
        setRarity(original.getRarity());
    }

    public void addProperty(WeaponProperty property) {
        properties.add(property);
    }

    public void removeProperty(WeaponProperty property) {
        properties.remove(property);
    }

    public boolean hasProperty(WeaponProperty property) {
        return properties.contains(property);
    }

    public Set<WeaponProperty> getProperties() {
        return EnumSet.copyOf(properties);
    }

    public boolean isTwoHanded() {
        return hasProperty(WeaponProperty.TWO_HANDED);
    }

    public boolean isFinesse() {
        return hasProperty(WeaponProperty.FINESSE);
    }

    public boolean isLight() {
        return hasProperty(WeaponProperty.LIGHT);
    }

    public boolean isVersatile() {
        return hasProperty(WeaponProperty.VERSATILE);
    }

    public boolean isThrown() {
        return hasProperty(WeaponProperty.THROWN);
    }

    public boolean hasReach() {
        return hasProperty(WeaponProperty.REACH);
    }

    public boolean isRanged() {
        return category.isRanged();
    }

    public boolean isMelee() {
        return category.isMelee();
    }

    public boolean isMagical() {
        return hasProperty(WeaponProperty.MAGICAL) || attackBonus > 0 || damageBonus > 0;
    }

    /**
     * Gets the damage dice string (e.g., "2d6", "1d8+2").
     */
    public String getDamageDice() {
        StringBuilder sb = new StringBuilder();
        sb.append(damageDiceCount).append("d").append(damageDieSize);
        if (damageBonus > 0) {
            sb.append("+").append(damageBonus);
        } else if (damageBonus < 0) {
            sb.append(damageBonus);
        }
        return sb.toString();
    }

    public String getVersatileDamageDice() {
        if (!isVersatile() || versatileDieSize == 0) {
            return getDamageDice();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(damageDiceCount).append("d").append(versatileDieSize);
        if (damageBonus > 0) {
            sb.append("+").append(damageBonus);
        } else if (damageBonus < 0) {
            sb.append(damageBonus);
        }
        return sb.toString();
    }

    public double getAverageDamage() {
        double avgDie = (damageDieSize + 1) / 2.0;
        return (damageDiceCount * avgDie) + damageBonus;
    }

    public int getDamageDiceCount() {
        return damageDiceCount;
    }

    public void setDamageDiceCount(int damageDiceCount) {
        this.damageDiceCount = Math.max(1, damageDiceCount);
    }

    public int getDamageDieSize() {
        return damageDieSize;
    }

    public void setDamageDieSize(int damageDieSize) {
        this.damageDieSize = Math.max(1, damageDieSize);
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public void setDamageType(DamageType damageType) {
        this.damageType = damageType;
    }

    public WeaponCategory getCategory() {
        return category;
    }

    public void setCategory(WeaponCategory category) {
        this.category = category;
    }

    public int getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(int normalRange) {
        this.normalRange = Math.max(0, normalRange);
    }

    public int getLongRange() {
        return longRange;
    }

    public void setLongRange(int longRange) {
        this.longRange = Math.max(this.normalRange, longRange);
    }

    public void setRange(int normal, int longRange) {
        this.normalRange = Math.max(0, normal);
        this.longRange = Math.max(this.normalRange, longRange);
    }

    public int getVersatileDieSize() {
        return versatileDieSize;
    }

    public void setVersatileDieSize(int versatileDieSize) {
        this.versatileDieSize = Math.max(0, versatileDieSize);
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public void setAttackBonus(int attackBonus) {
        this.attackBonus = attackBonus;
    }

    public int getDamageBonus() {
        return damageBonus;
    }

    public void setDamageBonus(int damageBonus) {
        this.damageBonus = damageBonus;
    }

    public void setMagicBonus(int bonus) {
        this.attackBonus = bonus;
        this.damageBonus = bonus;
        if (bonus > 0) {
            addProperty(WeaponProperty.MAGICAL);
        }
    }

    @Override
    public boolean isEquippable() {
        return true;
    }

    @Override
    public Item copy() {
        return new Weapon(this);
    }
    
    /**
     * Creates a copy with a new unique ID (for loot duplication).
     */
    public Weapon copyWithNewId() {
        Weapon copy = new Weapon(getName(), damageDiceCount, damageDieSize,
                damageType, category, getWeight(), getGoldValue());
        copy.setDescription(getDescription());
        copy.setRarity(getRarity());
        copy.normalRange = this.normalRange;
        copy.longRange = this.longRange;
        copy.versatileDieSize = this.versatileDieSize;
        copy.attackBonus = this.attackBonus;
        copy.damageBonus = this.damageBonus;
        copy.properties.addAll(this.properties);
        return copy;
    }

    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        if (getRarity() != Rarity.COMMON) {
            sb.append(" [").append(getRarity().getDisplayName()).append("]");
        }
        if (attackBonus > 0) {
            sb.append(" +").append(attackBonus);
        }
        sb.append("\n");
        
        sb.append(category.getDisplayName()).append(" Weapon\n");
        sb.append("Damage: ").append(getDamageDice()).append(" ").append(damageType.getDisplayName());
        
        if (isVersatile() && versatileDieSize > 0) {
            sb.append(" (").append(getVersatileDamageDice()).append(" two-handed)");
        }
        sb.append("\n");
        
        if (normalRange > 0) {
            sb.append("Range: ").append(normalRange).append("/").append(longRange).append(" ft\n");
        }
        
        if (!properties.isEmpty()) {
            sb.append("Properties: ");
            sb.append(properties.stream()
                    .map(WeaponProperty::getDisplayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            sb.append("\n");
        }
        
        if (!getDescription().isEmpty()) {
            sb.append(getDescription()).append("\n");
        }
        
        sb.append(String.format("Weight: %.1f lb | Value: %d gp", getWeight(), getGoldValue()));
        
        return sb.toString();
    }

    @Override
    public String toString() {
        String rangeStr = normalRange > 0 ? String.format(" (%d/%d)", normalRange, longRange) : "";
        String bonusStr = attackBonus > 0 ? " +" + attackBonus : "";
        return String.format("%s%s [%s, %s %s%s]", 
                getName(), bonusStr, category.getDisplayName(), 
                getDamageDice(), damageType.getDisplayName(), rangeStr);
    }

    // Simple Melee Weapons
    public static Weapon createClub() {
        Weapon w = new Weapon("Club", 1, 4, DamageType.BLUDGEONING, 
                WeaponCategory.SIMPLE_MELEE, 2.0, 1);
        w.addProperty(WeaponProperty.LIGHT);
        return w;
    }

    public static Weapon createDagger() {
        Weapon w = new Weapon("Dagger", 1, 4, DamageType.PIERCING,
                WeaponCategory.SIMPLE_MELEE, 20, 60, 1.0, 2);
        w.addProperty(WeaponProperty.FINESSE);
        w.addProperty(WeaponProperty.LIGHT);
        w.addProperty(WeaponProperty.THROWN);
        return w;
    }

    public static Weapon createGreatclub() {
        Weapon w = new Weapon("Greatclub", 1, 8, DamageType.BLUDGEONING,
                WeaponCategory.SIMPLE_MELEE, 10.0, 2);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    public static Weapon createHandaxe() {
        Weapon w = new Weapon("Handaxe", 1, 6, DamageType.SLASHING,
                WeaponCategory.SIMPLE_MELEE, 20, 60, 2.0, 5);
        w.addProperty(WeaponProperty.LIGHT);
        w.addProperty(WeaponProperty.THROWN);
        return w;
    }

    public static Weapon createJavelin() {
        Weapon w = new Weapon("Javelin", 1, 6, DamageType.PIERCING,
                WeaponCategory.SIMPLE_MELEE, 30, 120, 2.0, 5);
        w.addProperty(WeaponProperty.THROWN);
        return w;
    }

    public static Weapon createLightHammer() {
        Weapon w = new Weapon("Light Hammer", 1, 4, DamageType.BLUDGEONING,
                WeaponCategory.SIMPLE_MELEE, 20, 60, 2.0, 2);
        w.addProperty(WeaponProperty.LIGHT);
        w.addProperty(WeaponProperty.THROWN);
        return w;
    }

    public static Weapon createMace() {
        return new Weapon("Mace", 1, 6, DamageType.BLUDGEONING,
                WeaponCategory.SIMPLE_MELEE, 4.0, 5);
    }

    public static Weapon createQuarterstaff() {
        Weapon w = new Weapon("Quarterstaff", 1, 6, DamageType.BLUDGEONING,
                WeaponCategory.SIMPLE_MELEE, 4.0, 2);
        w.addProperty(WeaponProperty.VERSATILE);
        w.setVersatileDieSize(8);
        return w;
    }

    public static Weapon createSickle() {
        Weapon w = new Weapon("Sickle", 1, 4, DamageType.SLASHING,
                WeaponCategory.SIMPLE_MELEE, 2.0, 1);
        w.addProperty(WeaponProperty.LIGHT);
        return w;
    }

    public static Weapon createSpear() {
        Weapon w = new Weapon("Spear", 1, 6, DamageType.PIERCING,
                WeaponCategory.SIMPLE_MELEE, 20, 60, 3.0, 1);
        w.addProperty(WeaponProperty.THROWN);
        w.addProperty(WeaponProperty.VERSATILE);
        w.setVersatileDieSize(8);
        return w;
    }

    // Simple Ranged Weapons
    public static Weapon createLightCrossbow() {
        Weapon w = new Weapon("Light Crossbow", 1, 8, DamageType.PIERCING,
                WeaponCategory.SIMPLE_RANGED, 80, 320, 5.0, 25);
        w.addProperty(WeaponProperty.AMMUNITION);
        w.addProperty(WeaponProperty.LOADING);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    public static Weapon createShortbow() {
        Weapon w = new Weapon("Shortbow", 1, 6, DamageType.PIERCING,
                WeaponCategory.SIMPLE_RANGED, 80, 320, 2.0, 25);
        w.addProperty(WeaponProperty.AMMUNITION);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    // Martial Melee Weapons
    public static Weapon createBattleaxe() {
        Weapon w = new Weapon("Battleaxe", 1, 8, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 4.0, 10);
        w.addProperty(WeaponProperty.VERSATILE);
        w.setVersatileDieSize(10);
        return w;
    }

    public static Weapon createGreatsword() {
        Weapon w = new Weapon("Greatsword", 2, 6, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 6.0, 50);
        w.addProperty(WeaponProperty.HEAVY);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    public static Weapon createGreataxe() {
        Weapon w = new Weapon("Greataxe", 1, 12, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 7.0, 30);
        w.addProperty(WeaponProperty.HEAVY);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    public static Weapon createMaul() {
        Weapon w = new Weapon("Maul", 2, 6, DamageType.BLUDGEONING,
                WeaponCategory.MARTIAL_MELEE, 10.0, 10);
        w.addProperty(WeaponProperty.HEAVY);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    public static Weapon createGlaive() {
        Weapon w = new Weapon("Glaive", 1, 10, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 6.0, 20);
        w.addProperty(WeaponProperty.HEAVY);
        w.addProperty(WeaponProperty.TWO_HANDED);
        w.addProperty(WeaponProperty.REACH);
        return w;
    }

    public static Weapon createLongsword() {
        Weapon w = new Weapon("Longsword", 1, 8, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 3.0, 15);
        w.addProperty(WeaponProperty.VERSATILE);
        w.setVersatileDieSize(10);
        return w;
    }

    public static Weapon createRapier() {
        Weapon w = new Weapon("Rapier", 1, 8, DamageType.PIERCING,
                WeaponCategory.MARTIAL_MELEE, 2.0, 25);
        w.addProperty(WeaponProperty.FINESSE);
        return w;
    }

    public static Weapon createScimitar() {
        Weapon w = new Weapon("Scimitar", 1, 6, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 3.0, 25);
        w.addProperty(WeaponProperty.FINESSE);
        w.addProperty(WeaponProperty.LIGHT);
        return w;
    }

    public static Weapon createShortsword() {
        Weapon w = new Weapon("Shortsword", 1, 6, DamageType.PIERCING,
                WeaponCategory.MARTIAL_MELEE, 2.0, 10);
        w.addProperty(WeaponProperty.FINESSE);
        w.addProperty(WeaponProperty.LIGHT);
        return w;
    }

    public static Weapon createWarhammer() {
        Weapon w = new Weapon("Warhammer", 1, 8, DamageType.BLUDGEONING,
                WeaponCategory.MARTIAL_MELEE, 2.0, 15);
        w.addProperty(WeaponProperty.VERSATILE);
        w.setVersatileDieSize(10);
        return w;
    }

    // Martial Ranged Weapons
    public static Weapon createLongbow() {
        Weapon w = new Weapon("Longbow", 1, 8, DamageType.PIERCING,
                WeaponCategory.MARTIAL_RANGED, 150, 600, 2.0, 50);
        w.addProperty(WeaponProperty.AMMUNITION);
        w.addProperty(WeaponProperty.HEAVY);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }

    public static Weapon createHandCrossbow() {
        Weapon w = new Weapon("Hand Crossbow", 1, 6, DamageType.PIERCING,
                WeaponCategory.MARTIAL_RANGED, 30, 120, 3.0, 75);
        w.addProperty(WeaponProperty.AMMUNITION);
        w.addProperty(WeaponProperty.LIGHT);
        w.addProperty(WeaponProperty.LOADING);
        return w;
    }

    public static Weapon createHeavyCrossbow() {
        Weapon w = new Weapon("Heavy Crossbow", 1, 10, DamageType.PIERCING,
                WeaponCategory.MARTIAL_RANGED, 100, 400, 18.0, 50);
        w.addProperty(WeaponProperty.AMMUNITION);
        w.addProperty(WeaponProperty.HEAVY);
        w.addProperty(WeaponProperty.LOADING);
        w.addProperty(WeaponProperty.TWO_HANDED);
        return w;
    }
}