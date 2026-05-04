package com.questkeeper.combat;

import com.questkeeper.core.Dice;

/**
 * Represents an enemy creature in combat.
 * 
 * Monsters have simplified stats compared to Characters, following
 * D&D 5e monster stat block conventions. They implement Combatant
 * for use in the combat system.
 * 
 * @author Marc McGough
 * @version 1.0
 */

public class Monster implements Combatant {

    public enum Size {
        TINY("Tiny", 2.5),
        SMALL("Small", 5),
        MEDIUM("Medium", 5),
        LARGE("Large", 10),
        HUGE("Huge", 15),
        GARGANTUAN("Gargantuan", 20);

        private final String displayName;
        private final double space; // feet

        Size(String displayName, double space) {
            this.displayName = displayName;
            this.space = space;
        }

        public String getDisplayName() {
            return displayName;
        }
        public double getSpace() {
            return space;
        }
    }

    public enum MonsterType {
        ABERRATION("Aberration"),
        BEAST("Beast"),
        CELESTIAL("Celestial"),
        CONSTRUCT("Construct"),
        DRAGON("Dragon"),
        ELEMENTAL("Elemental"),
        FEY("Fey"),
        FIEND("Fiend"),
        GIANT("GIANT"),
        HUMANOID("Humanoid"),
        MONSTROSITY("Monstrosity"),
        OOZE("Ooze"),
        PLANT("Plant"),
        UNDEAD("Undead");

        private final String displayName;

        MonsterType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Defines AI behavior patterns for combat.
     */
    public enum Behavior {
        AGGRESSIVE("Aggressive"),   // Always attacks, never flees
        COWARDLY("Cowardly"),       // Flees when bloodied
        TACTICAL("Tactical"),       // Uses special abilities, targets last attacker
        DEFENSIVE("Defensive");     // May flee when below 25% HP

        private final String displayName;

        Behavior(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String id;
    private String name;
    private Size size;
    private MonsterType type;
    private String alignment;

    private int armorClass;
    private int maxHitPoints;
    private int currentHitPoints;
    private int speed;

    private int strengthMod;
    private int dexterityMod;
    private int constitutionMod;
    private int intelligenceMod;
    private int wisdomMod;
    private int charismaMod;

    private double challengeRating;
    private int experienceValue;
    private int attackBonus;
    private String damageDice;

    private String description;
    private String specialAbility;
    private Behavior behavior;
    private boolean rangedAttack;  // true => primary attack is ranged (bow, crossbow, javelin, etc.)
    private boolean boss;          // true => excluded from random-encounter pools

    public Monster(String id, String name, int armorClass, int maxHitPoints) {
        this.id  = id;
        this.name = name;
        this.armorClass = armorClass;
        this.maxHitPoints = maxHitPoints;
        this.currentHitPoints = maxHitPoints;

        this.size = Size.MEDIUM;
        this.type = MonsterType.HUMANOID;
        this.alignment = "unaligned";
        this.speed = 30;
        this.challengeRating = 0;
        this.experienceValue = 10;
        this.attackBonus = 2;
        this.damageDice = "1d4";
        this.description = "";
        this.specialAbility = null;
        this.behavior = Behavior.AGGRESSIVE;  // Default behavior

        this.strengthMod = 0;
        this.dexterityMod = 0;
        this.constitutionMod = 0;
        this.intelligenceMod = 0;
        this.wisdomMod = 0;
        this.charismaMod = 0;
    }
    
    public Monster(String id, String name, Size size, MonsterType type, 
                   int armorClass, int maxHitPoints, int speed, 
                   double challengeRating, int experienceValue) {
        this(id, name, armorClass, maxHitPoints);
        this.size = size;
        this.type = type;
        this.speed = speed;
        this.challengeRating = challengeRating;
        this. experienceValue = experienceValue;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getCurrentHitPoints() {
        return currentHitPoints;
    }
    
    @Override
    public int getMaxHitPoints() {
        return maxHitPoints;
    }

    @Override
    public int getArmorClass() {
        return armorClass;
    }

    @Override
    public int takeDamage(int amount) {
        if (amount <= 0) return 0;

        int actualDamage = Math.min(amount, currentHitPoints);
        currentHitPoints -= actualDamage;
        return actualDamage;
    }

    @Override
    public int heal(int amount) {
        if (amount <= 0) return 0;
    
        int oldHp = currentHitPoints;
        currentHitPoints = Math.min(currentHitPoints + amount, maxHitPoints);
        return currentHitPoints - oldHp;
    }

    @Override
    public int getInitiativeModifier() {
        return dexterityMod;
    }
    
    @Override
    public int rollInitiative() {
        return Dice.rollWithModifier(20, getInitiativeModifier());
    }

    public int rollAttack() {
        return Dice.rollWithModifier(20, attackBonus);
    }

    public int rollDamage() {
        return Dice.parse(damageDice);
    }

    public boolean attackHits(int targetAC) {
        int roll = rollAttack();
        return roll >= targetAC;
    }

    public void resetHitPoints() {
        this.currentHitPoints = this.maxHitPoints;
    }

    /**
     * Checks if the monster is bloodied (at or below 50% HP).
     */
    public boolean isBloodied() {
        return currentHitPoints <= (maxHitPoints / 2);
    }

    /**
     * Gets the monster's current HP as a percentage of max HP.
     */
    public int getHpPercentage() {
        if (maxHitPoints == 0) return 0;
        return (currentHitPoints * 100) / maxHitPoints;
    }

    public Monster copy(String newId) {
        Monster copy = new Monster(newId, name, size, type, 
                armorClass, maxHitPoints, speed, challengeRating,experienceValue);
        copy.alignment = this.alignment;
        copy.strengthMod = this.strengthMod;
        copy.dexterityMod = this.dexterityMod;
        copy.constitutionMod = this.constitutionMod;
        copy.intelligenceMod = this.intelligenceMod;
        copy.wisdomMod = this.wisdomMod;
        copy.charismaMod = this.charismaMod;
        copy.attackBonus = this.attackBonus;
        copy.damageDice = this.damageDice;
        copy.description = this.description;
        copy.specialAbility = this.specialAbility;
        copy.behavior = this.behavior;
        return copy;
    }
    
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public MonsterType getType() {
        return type;
    }

    public void setType(MonsterType type) {
        this.type = type;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setArmorClass(int armorClass) {
        this.armorClass = armorClass;
    }

    public void setMaxHitPoints(int maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
        if (currentHitPoints > maxHitPoints) {
            currentHitPoints = maxHitPoints;
        }
    }

    public double getChallengeRating() {
        return challengeRating;
    }
    
    public void setChallengeRating(double challengeRating) {
        this.challengeRating = challengeRating;
    }
    
    public int getExperienceValue() {
        return experienceValue;
    }
    
    public void setExperienceValue(int experienceValue) {
        this.experienceValue = experienceValue;
    }
    
    public int getAttackBonus() {
        return attackBonus;
    }
    
    public void setAttackBonus(int attackBonus) {
        this.attackBonus = attackBonus;
    }
    
    public String getDamageDice() {
        return damageDice;
    }

    public void setDamageDice(String damageDice) {
        this.damageDice = damageDice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecialAbility() {
        return specialAbility;
    }

    public void setSpecialAbility(String specialAbility) {
        this.specialAbility = specialAbility;
    }

    public boolean hasSpecialAbility() {
        return specialAbility != null && !specialAbility.isEmpty();
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior != null ? behavior : Behavior.AGGRESSIVE;
    }

    public boolean isRangedAttack() {
        return rangedAttack;
    }

    public void setRangedAttack(boolean rangedAttack) {
        this.rangedAttack = rangedAttack;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public void setAbilityModifiers(int str, int dex, int con, int intel, int wis, int cha) {
        this.strengthMod = str;
        this.dexterityMod = dex;
        this.constitutionMod = con;
        this.intelligenceMod = intel;
        this.wisdomMod = wis;
        this.charismaMod = cha;
    }

    public int getStrengthMod() { 
        return strengthMod; 
    }

    public int getDexterityMod() { 
        return dexterityMod; 
    }

    public int getConstitutionMod() { 
        return constitutionMod; 
    }

    public int getIntelligenceMod() { 
        return intelligenceMod; 
    }

    public int getWisdomMod() { 
        return wisdomMod; 
    }

    public int getCharismaMod() { 
        return charismaMod; 
    }
    
    public void setStrengthMod(int mod) { 
        this.strengthMod = mod; 
    }

    public void setDexterityMod(int mod) { 
        this.dexterityMod = mod; 
    }

    public void setConstitutionMod(int mod) { 
        this.constitutionMod = mod;
    }

    public void setIntelligenceMod(int mod) { 
        this.intelligenceMod = mod; 
    }

    public void setWisdomMod(int mod) { 
        this.wisdomMod = mod; 
    }

    public void setCharismaMod(int mod) { 
        this.charismaMod = mod; 
    }
private String formatCR() {
    if (challengeRating == 0) return "0";
    if (challengeRating == 0.125) return "1/8";
    if (challengeRating == 0.25) return "1/4";
    if (challengeRating == 0.5) return "1/2";
    return String.format("%.0f", challengeRating);  // Whole numbers for CR 1+
}
    public String getStatBlock() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%n", name));
        sb.append(String.format("%s %s, %s%n", size.getDisplayName(), type.getDisplayName(), alignment));
        sb.append(String.format("───────────────────────────%n"));
        sb.append(String.format("AC: %d | HP: %d/%d | Speed: %d ft%n", 
                armorClass, currentHitPoints, maxHitPoints, speed));
        sb.append(String.format("───────────────────────────%n"));
        sb.append(String.format("STR %+d | DEX %+d | CON %+d%n", strengthMod, dexterityMod, constitutionMod));
        sb.append(String.format("INT %+d | WIS %+d | CHA %+d%n", intelligenceMod, wisdomMod, charismaMod));
        sb.append(String.format("───────────────────────────%n"));
        sb.append(String.format("Attack: +%d to hit, %s damage%n", attackBonus, damageDice));
        if (hasSpecialAbility()) {
            sb.append(String.format("Special: %s%n", specialAbility));
        }
        sb.append(String.format("CR: %s (%d XP)", formatCR(), experienceValue));

        if (!description.isEmpty()) {
            sb.append(String.format("%n%n%s", description));
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%s [%s %s, CR %s] (HP: %d/%d, AC: %d)",
        name, size.getDisplayName(), type.getDisplayName(),
        formatCR(), currentHitPoints, maxHitPoints, armorClass);
    }
}
