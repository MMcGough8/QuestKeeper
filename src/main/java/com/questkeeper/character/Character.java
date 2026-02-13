package com.questkeeper.character;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.questkeeper.character.features.ActivatedFeature;
import com.questkeeper.character.features.BarbarianFeatures;
import com.questkeeper.character.features.ClassFeature;
import com.questkeeper.character.features.FighterFeatures;
import com.questkeeper.character.features.FightingStyle;
import com.questkeeper.character.features.MonkFeatures;
import com.questkeeper.character.features.PaladinFeatures;
import com.questkeeper.character.features.RangerFeatures;
import com.questkeeper.character.features.RogueFeatures;
import com.questkeeper.combat.Combatant;
import com.questkeeper.core.Dice;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Inventory;

/**
 * Represents a player character in the game.
 * 
 * Handles ability scores, derived stats, class/race features,
 * and D&D 5e mechanics like proficiency bonuses and skill checks.
 * Implements Combatant for use in the combat system.
 * 
 * @author Marc McGough
 * @version 1.1
 */

public class Character implements Combatant {
    /**
     * The six core ability scores.
     */
    public enum Ability {
        STRENGTH("Strength", "STR"),
        DEXTERITY("Dexterity", "DEX"),
        CONSTITUTION("Constitution", "CON"),
        INTELLIGENCE("Intelligence", "INT"),
        WISDOM("Wisdom", "WIS"),
        CHARISMA("Charisma", "CHA");
        
        private final String fullName;
        private final String abbreviation;
        
        Ability(String fullName, String abbreviation) {
            this.fullName = fullName;
            this.abbreviation = abbreviation;
        }
        
        public String getFullName() { return fullName; }
        public String getAbbreviation() { return abbreviation; }
    }
    
    /**
     * Skills and their associated abilities.
     */
    public enum Skill {
        // Strength
        ATHLETICS(Ability.STRENGTH, "Athletics"),
        
        // Dexterity
        ACROBATICS(Ability.DEXTERITY, "Acrobatics"),
        SLEIGHT_OF_HAND(Ability.DEXTERITY, "Sleight of Hand"),
        STEALTH(Ability.DEXTERITY, "Stealth"),
        
        // Intelligence
        ARCANA(Ability.INTELLIGENCE, "Arcana"),
        HISTORY(Ability.INTELLIGENCE, "History"),
        INVESTIGATION(Ability.INTELLIGENCE, "Investigation"),
        NATURE(Ability.INTELLIGENCE, "Nature"),
        RELIGION(Ability.INTELLIGENCE, "Religion"),
        
        // Wisdom
        ANIMAL_HANDLING(Ability.WISDOM, "Animal Handling"),
        INSIGHT(Ability.WISDOM, "Insight"),
        MEDICINE(Ability.WISDOM, "Medicine"),
        PERCEPTION(Ability.WISDOM, "Perception"),
        SURVIVAL(Ability.WISDOM, "Survival"),
        
        // Charisma
        DECEPTION(Ability.CHARISMA, "Deception"),
        INTIMIDATION(Ability.CHARISMA, "Intimidation"),
        PERFORMANCE(Ability.CHARISMA, "Performance"),
        PERSUASION(Ability.CHARISMA, "Persuasion");
        
        private final Ability ability;
        private final String displayName;
        
        Skill(Ability ability, String displayName) {
            this.ability = ability;
            this.displayName = displayName;
        }
        
        public Ability getAbility() { return ability; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Available character races.
     */
    public enum Race {
        HUMAN("Human", 0, 0, 0, 0, 0, 0, 30),           // +1 to all (handled separately)
        DWARF("Dwarf", 0, 0, 2, 0, 0, 0, 25),           // +2 CON
        ELF("Elf", 0, 2, 0, 0, 0, 0, 30),               // +2 DEX
        HALFLING("Halfling", 0, 2, 0, 0, 0, 0, 25),     // +2 DEX
        DRAGONBORN("Dragonborn", 2, 0, 0, 0, 0, 1, 30), // +2 STR, +1 CHA
        GNOME("Gnome", 0, 0, 0, 2, 0, 0, 25),           // +2 INT
        HALF_ELF("Half-Elf", 0, 0, 0, 0, 0, 2, 30),     // +2 CHA, +1 to two others
        HALF_ORC("Half-Orc", 2, 0, 1, 0, 0, 0, 30),     // +2 STR, +1 CON
        TIEFLING("Tiefling", 0, 0, 0, 1, 0, 2, 30);     // +2 CHA, +1 INT
        
        private final String displayName;
        private final int strBonus;
        private final int dexBonus;
        private final int conBonus;
        private final int intBonus;
        private final int wisBonus;
        private final int chaBonus;
        private final int speed;
        
        Race(String displayName, int str, int dex, int con, int intel, int wis, int cha, int speed) {
            this.displayName = displayName;
            this.strBonus = str;
            this.dexBonus = dex;
            this.conBonus = con;
            this.intBonus = intel;
            this.wisBonus = wis;
            this.chaBonus = cha;
            this.speed = speed;
        }
        
        public String getDisplayName() { return displayName; }
        public int getStrBonus() { return strBonus; }
        public int getDexBonus() { return dexBonus; }
        public int getConBonus() { return conBonus; }
        public int getIntBonus() { return intBonus; }
        public int getWisBonus() { return wisBonus; }
        public int getChaBonus() { return chaBonus; }
        public int getSpeed() { return speed; }
        
        public int getAbilityBonus(Ability ability) {
            return switch (ability) {
                case STRENGTH -> strBonus;
                case DEXTERITY -> dexBonus;
                case CONSTITUTION -> conBonus;
                case INTELLIGENCE -> intBonus;
                case WISDOM -> wisBonus;
                case CHARISMA -> chaBonus;
            };
        }
    }
    
    /**
     * Available character classes.
     */
    public enum CharacterClass {
        BARBARIAN("Barbarian", 12, Ability.STRENGTH, Ability.CONSTITUTION),
        BARD("Bard", 8, Ability.DEXTERITY, Ability.CHARISMA),
        CLERIC("Cleric", 8, Ability.WISDOM, Ability.CHARISMA),
        DRUID("Druid", 8, Ability.INTELLIGENCE, Ability.WISDOM),
        FIGHTER("Fighter", 10, Ability.STRENGTH, Ability.CONSTITUTION),
        MONK("Monk", 8, Ability.STRENGTH, Ability.DEXTERITY),
        PALADIN("Paladin", 10, Ability.WISDOM, Ability.CHARISMA),
        RANGER("Ranger", 10, Ability.STRENGTH, Ability.DEXTERITY),
        ROGUE("Rogue", 8, Ability.DEXTERITY, Ability.INTELLIGENCE),
        SORCERER("Sorcerer", 6, Ability.CONSTITUTION, Ability.CHARISMA),
        WARLOCK("Warlock", 8, Ability.WISDOM, Ability.CHARISMA),
        WIZARD("Wizard", 6, Ability.INTELLIGENCE, Ability.WISDOM);
        
        private final String displayName;
        private final int hitDie;
        private final Ability primarySave;
        private final Ability secondarySave;
        
        CharacterClass(String displayName, int hitDie, Ability primary, Ability secondary) {
            this.displayName = displayName;
            this.hitDie = hitDie;
            this.primarySave = primary;
            this.secondarySave = secondary;
        }
        
        public String getDisplayName() { return displayName; }
        public int getHitDie() { return hitDie; }
        public Ability getPrimarySave() { return primarySave; }
        public Ability getSecondarySave() { return secondarySave; }
    }
 
    private static final int MIN_ABILITY_SCORE = 1;

    private static final int MAX_ABILITY_SCORE = 20;
 
    private static final int BASE_AC = 10;
 
    private static final int STARTING_LEVEL = 1;

    private static final int[] XP_THRESHOLDS = {
        0, 300, 900, 2700, 6500, 14000, 23000, 34000, 48000, 64000,
        85000, 100000, 120000, 140000, 165000, 195000, 225000, 265000, 305000, 355000
    };

    private String name;
    private Race race;
    private CharacterClass characterClass;
    private int level;
    private int experiencePoints;
    
    private final Map<Ability, Integer> baseAbilityScores;
    private final Set<Skill> proficientSkills;
    private final Set<Ability> savingThrowProficiencies;
    
    private int currentHitPoints;
    private int maxHitPoints;
    private int temporaryHitPoints;

    private int availableHitDice;  // Hit dice available for short rest healing

    private int armorBonus;
    private int shieldBonus;

    // Half-Elf bonus abilities (+1 to two abilities of player's choice, excluding CHA)
    private Set<Ability> halfElfBonusAbilities = EnumSet.noneOf(Ability.class);

    private final Inventory inventory;

    // Class features
    private final List<ClassFeature> classFeatures = new ArrayList<>();
    private FightingStyle fightingStyle;  // Fighter-specific; null for other classes
    private Set<Skill> expertiseSkills = EnumSet.noneOf(Skill.class);  // Rogue-specific

    public Character(String name, Race race, CharacterClass characterClass) {
        this.name = name;
        this.race = race;
        this.characterClass = characterClass;
        this.level = STARTING_LEVEL;
        this.experiencePoints = 0;
        
        this.baseAbilityScores = new EnumMap<>(Ability.class);
        this.proficientSkills = EnumSet.noneOf(Skill.class);
        this.savingThrowProficiencies = EnumSet.noneOf(Ability.class);
        
        for (Ability ability : Ability.values()) {
            baseAbilityScores.put(ability, 10);
        }
        
        savingThrowProficiencies.add(characterClass.getPrimarySave());
        savingThrowProficiencies.add(characterClass.getSecondarySave());
        
        this.maxHitPoints = calculateMaxHitPoints();
        this.currentHitPoints = maxHitPoints;
        this.temporaryHitPoints = 0;
        this.availableHitDice = level;  // Start with all hit dice available

        this.armorBonus = 0;
        this.shieldBonus = 0;

        // Initialize inventory with strength-based carrying capacity
        this.inventory = new Inventory(getAbilityScore(Ability.STRENGTH));

        // Initialize class features for starting level
        initializeClassFeatures();
    }

    public Character(String name, Race race, CharacterClass characterClass,
                     int str, int dex, int con, int intel, int wis, int cha) {
        this(name, race, characterClass);
        setAbilityScores(str, dex, con, intel, wis, cha);
        this.maxHitPoints = calculateMaxHitPoints();
        this.currentHitPoints = maxHitPoints;
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
        int dexMod = getAbilityModifier(Ability.DEXTERITY);
        int ac;

        // Check for equipped armor
        Armor equippedArmor = inventory.getEquippedArmor();
        if (equippedArmor != null) {
            // Use armor's AC calculation (handles light/medium/heavy DEX limits)
            ac = equippedArmor.calculateAC(dexMod);
        } else {
            // No armor: check for Unarmored Defense
            if (characterClass == CharacterClass.BARBARIAN) {
                // Barbarian Unarmored Defense: 10 + DEX + CON
                int conMod = getAbilityModifier(Ability.CONSTITUTION);
                ac = BASE_AC + dexMod + conMod;
            } else if (characterClass == CharacterClass.MONK) {
                // Monk Unarmored Defense: 10 + DEX + WIS (no shield allowed)
                Armor equippedShield = inventory.getEquippedShield();
                if (equippedShield == null) {
                    int wisMod = getAbilityModifier(Ability.WISDOM);
                    ac = BASE_AC + dexMod + wisMod;
                } else {
                    // Monk with shield loses Unarmored Defense, uses standard 10 + DEX
                    ac = BASE_AC + dexMod;
                }
            } else {
                // Standard unarmored: 10 + DEX
                ac = BASE_AC + dexMod;
            }
        }

        // Add shield bonus if equipped (Monk already handled above)
        Armor equippedShield = inventory.getEquippedShield();
        if (equippedShield != null) {
            ac += equippedShield.getShieldBonus();
        }

        // Add any additional bonuses (magic items, class features, etc.)
        ac += armorBonus + shieldBonus;

        // Defense Fighting Style: +1 AC while wearing armor
        if (fightingStyle == FightingStyle.DEFENSE && equippedArmor != null) {
            ac += 1;
        }

        return ac;
    }
    
    @Override
    public int takeDamage(int amount) {
        if (amount <= 0) return 0;
        
        int remainingDamage = amount;
        
        if (temporaryHitPoints > 0) {
            if (temporaryHitPoints >= remainingDamage) {
                temporaryHitPoints -= remainingDamage;
                return 0;
            } else {
                remainingDamage -= temporaryHitPoints;
                temporaryHitPoints = 0;
            }
        }
    
        int actualDamage = Math.min(remainingDamage, currentHitPoints);
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

    // ==========================================
    // Hit Dice Methods (for resting)
    // ==========================================

    /**
     * Gets the number of hit dice currently available for short rest healing.
     */
    public int getAvailableHitDice() {
        return availableHitDice;
    }

    /**
     * Gets the maximum number of hit dice (equals character level).
     */
    public int getMaxHitDice() {
        return level;
    }

    /**
     * Gets the size of this character's hit die (e.g., 8 for d8).
     */
    public int getHitDieSize() {
        return characterClass.getHitDie();
    }

    /**
     * Sets the available hit dice (used when loading saved games).
     */
    public void setAvailableHitDice(int dice) {
        this.availableHitDice = Math.max(0, Math.min(dice, level));
    }

    /**
     * Uses one hit die to heal during a short rest.
     * Rolls the hit die and adds CON modifier.
     *
     * @return the amount healed, or -1 if no hit dice available
     */
    public int useHitDie() {
        if (availableHitDice <= 0) {
            return -1;
        }
        if (currentHitPoints >= maxHitPoints) {
            return 0;  // Already at full health
        }

        availableHitDice--;
        int roll = Dice.roll(characterClass.getHitDie());
        int conMod = getAbilityModifier(Ability.CONSTITUTION);
        int healing = Math.max(1, roll + conMod);  // Minimum 1 HP healed

        return heal(healing);
    }

    /**
     * Restores hit dice after a long rest.
     * Regains half of max hit dice (minimum 1).
     *
     * @return the number of hit dice restored
     */
    public int restoreHitDice() {
        int toRestore = Math.max(1, level / 2);
        int oldDice = availableHitDice;
        availableHitDice = Math.min(level, availableHitDice + toRestore);
        return availableHitDice - oldDice;
    }

    @Override
    public boolean isAlive() {
        return currentHitPoints > 0;
    }
    
    @Override
    public boolean isUnconscious() {
        return currentHitPoints <= 0;
    }
    
    @Override
    public boolean isBloodied() {
        return currentHitPoints <= maxHitPoints / 2;
    }
    
    @Override
    public int getInitiativeModifier() {
        return getAbilityModifier(Ability.DEXTERITY);
    }
    
    @Override
    public int rollInitiative() {
        return Dice.rollWithModifier(20, getInitiativeModifier());
    }

    public int getBaseAbilityScore(Ability ability) {
        return baseAbilityScores.get(ability);
    }

    public int getAbilityScore(Ability ability) {
        int base = baseAbilityScores.get(ability);
        int racialBonus = race.getAbilityBonus(ability);

        if (race == Race.HUMAN) {
            racialBonus = 1;
        }

        // Half-Elf gets +1 to two abilities of player's choice (in addition to +2 CHA)
        if (race == Race.HALF_ELF && halfElfBonusAbilities.contains(ability)) {
            racialBonus += 1;
        }

        return Math.min(base + racialBonus, MAX_ABILITY_SCORE);
    }

    public int getAbilityModifier(Ability ability) {
        return (getAbilityScore(ability) - 10) / 2;
    }
    
    public void setAbilityScore(Ability ability, int score) {
        int clampedScore = Math.max(MIN_ABILITY_SCORE, Math.min(MAX_ABILITY_SCORE, score));
        baseAbilityScores.put(ability, clampedScore);

        if (ability == Ability.CONSTITUTION) {
            int oldMax = maxHitPoints;
            maxHitPoints = calculateMaxHitPoints();
            if (oldMax > 0) {
                currentHitPoints = (int) ((double) currentHitPoints / oldMax * maxHitPoints);
            }
        } else if (ability == Ability.STRENGTH) {
            inventory.setCarryingCapacityFromStrength(getAbilityScore(Ability.STRENGTH));
        }
    }

    public void setExperiencePoints(int xp) {
        this.experiencePoints = Math.max(0, xp);
    }

    public void setCurrentHitPoints(int hp) {
        this.currentHitPoints = Math.max(0, Math.min(hp, maxHitPoints));
    }
    
    public void setAbilityScores(int str, int dex, int con, int intel, int wis, int cha) {
        baseAbilityScores.put(Ability.STRENGTH, clampAbilityScore(str));
        baseAbilityScores.put(Ability.DEXTERITY, clampAbilityScore(dex));
        baseAbilityScores.put(Ability.CONSTITUTION, clampAbilityScore(con));
        baseAbilityScores.put(Ability.INTELLIGENCE, clampAbilityScore(intel));
        baseAbilityScores.put(Ability.WISDOM, clampAbilityScore(wis));
        baseAbilityScores.put(Ability.CHARISMA, clampAbilityScore(cha));

        maxHitPoints = calculateMaxHitPoints();
        if (currentHitPoints > maxHitPoints) {
            currentHitPoints = maxHitPoints;
        }

        // Update inventory carrying capacity based on new STR
        inventory.setCarryingCapacityFromStrength(getAbilityScore(Ability.STRENGTH));
    }
    
    private int clampAbilityScore(int score) {
        return Math.max(MIN_ABILITY_SCORE, Math.min(MAX_ABILITY_SCORE, score));
    }

    public int getProficiencyBonus() {
        return (level - 1) / 4 + 2;
    }

    private int calculateMaxHitPoints() {
        int conMod = getAbilityModifier(Ability.CONSTITUTION);
        int hitDie = characterClass.getHitDie();
        
        int hp = hitDie + conMod;
        
        for (int i = 2; i <= level; i++) {
            hp += (hitDie / 2) + 1 + conMod;
        }
        
        return Math.max(1, hp);
    }
    
    public int getSpeed() {
        return race.getSpeed();
    }
    
    public int getPassivePerception() {
        return 10 + getSkillModifier(Skill.PERCEPTION);
    }

    /**
     * Gets the character's inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }

    // ==========================================
    // Class Feature Methods
    // ==========================================

    /**
     * Initializes class features based on current level.
     * Called from constructor and levelUp().
     */
    private void initializeClassFeatures() {
        if (characterClass == CharacterClass.FIGHTER) {
            List<ClassFeature> fighterFeatures = FighterFeatures.createFeaturesForLevel(level, fightingStyle);

            // Add any features we don't already have
            for (ClassFeature feature : fighterFeatures) {
                if (getFeature(feature.getId()).isEmpty()) {
                    classFeatures.add(feature);
                }
            }
        } else if (characterClass == CharacterClass.ROGUE) {
            List<ClassFeature> rogueFeatures = RogueFeatures.createFeaturesForLevel(level, expertiseSkills);

            // Add any features we don't already have
            for (ClassFeature feature : rogueFeatures) {
                if (getFeature(feature.getId()).isEmpty()) {
                    classFeatures.add(feature);
                } else if (feature.getId().equals(RogueFeatures.SNEAK_ATTACK_ID)) {
                    // Update Sneak Attack dice when leveling up
                    getFeature(RogueFeatures.SNEAK_ATTACK_ID)
                        .filter(f -> f instanceof RogueFeatures.SneakAttack)
                        .map(f -> (RogueFeatures.SneakAttack) f)
                        .ifPresent(sa -> sa.setRogueLevel(level));
                }
            }
        } else if (characterClass == CharacterClass.BARBARIAN) {
            List<ClassFeature> barbarianFeatures = BarbarianFeatures.createFeaturesForLevel(level);

            // Add any features we don't already have
            for (ClassFeature feature : barbarianFeatures) {
                if (getFeature(feature.getId()).isEmpty()) {
                    classFeatures.add(feature);
                } else if (feature.getId().equals(BarbarianFeatures.RAGE_ID)) {
                    // Update Rage uses and damage when leveling up
                    getFeature(BarbarianFeatures.RAGE_ID)
                        .filter(f -> f instanceof BarbarianFeatures.Rage)
                        .map(f -> (BarbarianFeatures.Rage) f)
                        .ifPresent(rage -> rage.setBarbarianLevel(level));
                }
            }
        } else if (characterClass == CharacterClass.MONK) {
            List<ClassFeature> monkFeatures = MonkFeatures.createFeaturesForLevel(level);

            // Add any features we don't already have
            for (ClassFeature feature : monkFeatures) {
                if (getFeature(feature.getId()).isEmpty()) {
                    classFeatures.add(feature);
                } else if (feature.getId().equals(MonkFeatures.MARTIAL_ARTS_ID)) {
                    // Update Martial Arts die when leveling up
                    getFeature(MonkFeatures.MARTIAL_ARTS_ID)
                        .filter(f -> f instanceof MonkFeatures.MartialArts)
                        .map(f -> (MonkFeatures.MartialArts) f)
                        .ifPresent(ma -> ma.setMonkLevel(level));
                } else if (feature.getId().equals(MonkFeatures.KI_ID)) {
                    // Update Ki points when leveling up
                    getFeature(MonkFeatures.KI_ID)
                        .filter(f -> f instanceof MonkFeatures.Ki)
                        .map(f -> (MonkFeatures.Ki) f)
                        .ifPresent(ki -> ki.setMonkLevel(level));
                } else if (feature.getId().equals(MonkFeatures.DEFLECT_MISSILES_ID)) {
                    // Update Deflect Missiles when leveling up
                    getFeature(MonkFeatures.DEFLECT_MISSILES_ID)
                        .filter(f -> f instanceof MonkFeatures.DeflectMissiles)
                        .map(f -> (MonkFeatures.DeflectMissiles) f)
                        .ifPresent(dm -> dm.setMonkLevel(level));
                }
            }
        } else if (characterClass == CharacterClass.PALADIN) {
            List<ClassFeature> paladinFeatures = PaladinFeatures.createFeaturesForLevel(level, fightingStyle);

            // Add any features we don't already have
            for (ClassFeature feature : paladinFeatures) {
                if (getFeature(feature.getId()).isEmpty()) {
                    classFeatures.add(feature);
                } else if (feature.getId().equals(PaladinFeatures.LAY_ON_HANDS_ID)) {
                    // Update Lay on Hands pool when leveling up
                    getFeature(PaladinFeatures.LAY_ON_HANDS_ID)
                        .filter(f -> f instanceof PaladinFeatures.LayOnHands)
                        .map(f -> (PaladinFeatures.LayOnHands) f)
                        .ifPresent(loh -> loh.setPaladinLevel(level));
                } else if (feature.getId().equals(PaladinFeatures.DIVINE_SMITE_ID)) {
                    // Update Divine Smite spell slots when leveling up
                    getFeature(PaladinFeatures.DIVINE_SMITE_ID)
                        .filter(f -> f instanceof PaladinFeatures.DivineSmite)
                        .map(f -> (PaladinFeatures.DivineSmite) f)
                        .ifPresent(ds -> ds.setPaladinLevel(level));
                }
            }

            // Update Divine Sense uses based on CHA
            getFeature(PaladinFeatures.DIVINE_SENSE_ID)
                .filter(f -> f instanceof PaladinFeatures.DivineSense)
                .map(f -> (PaladinFeatures.DivineSense) f)
                .ifPresent(ds -> ds.updateMaxUses(this));
        } else if (characterClass == CharacterClass.RANGER) {
            // Ranger features - choices default to null until set
            List<ClassFeature> rangerFeatures = RangerFeatures.createFeaturesForLevel(
                level, null, null, fightingStyle, null);

            // Add any features we don't already have
            for (ClassFeature feature : rangerFeatures) {
                if (getFeature(feature.getId()).isEmpty()) {
                    classFeatures.add(feature);
                } else if (feature.getId().equals(RangerFeatures.RANGER_SPELLCASTING_ID)) {
                    // Update spell slots when leveling up
                    getFeature(RangerFeatures.RANGER_SPELLCASTING_ID)
                        .filter(f -> f instanceof RangerFeatures.RangerSpellcasting)
                        .map(f -> (RangerFeatures.RangerSpellcasting) f)
                        .ifPresent(rs -> rs.setRangerLevel(level));
                }
            }
        }
        // Other classes will be added here as features are implemented
    }

    /**
     * Gets all class features this character has.
     */
    public List<ClassFeature> getClassFeatures() {
        return Collections.unmodifiableList(classFeatures);
    }

    /**
     * Gets a class feature by its ID.
     */
    public Optional<ClassFeature> getFeature(String id) {
        return classFeatures.stream()
            .filter(f -> f.getId().equals(id))
            .findFirst();
    }

    /**
     * Checks if a feature can be used (exists, is activated type, and has uses remaining).
     */
    public boolean canUseFeature(String id) {
        return getFeature(id)
            .filter(f -> f instanceof ActivatedFeature)
            .map(f -> ((ActivatedFeature) f).canUse())
            .orElse(false);
    }

    /**
     * Uses an activated feature by ID.
     *
     * @param id the feature ID
     * @return result message, or error message if feature not found or can't be used
     */
    public String useFeature(String id) {
        Optional<ClassFeature> feature = getFeature(id);
        if (feature.isEmpty()) {
            return "You don't have that ability.";
        }

        ClassFeature f = feature.get();
        if (!(f instanceof ActivatedFeature activated)) {
            return f.getName() + " is a passive ability that is always active.";
        }

        return activated.use(this);
    }

    /**
     * Resets all feature uses that reset on a short rest.
     */
    public void resetFeaturesOnShortRest() {
        for (ClassFeature feature : classFeatures) {
            if (feature instanceof ActivatedFeature activated) {
                activated.resetOnShortRest();
            }
        }
    }

    /**
     * Resets all feature uses that reset on a long rest.
     */
    public void resetFeaturesOnLongRest() {
        for (ClassFeature feature : classFeatures) {
            if (feature instanceof ActivatedFeature activated) {
                activated.resetOnLongRest();
            }
        }
    }

    /**
     * Gets the character's fighting style (Fighter only).
     */
    public FightingStyle getFightingStyle() {
        return fightingStyle;
    }

    /**
     * Sets the character's fighting style (Fighter only).
     * This will update the class features list.
     */
    public void setFightingStyle(FightingStyle style) {
        if (characterClass != CharacterClass.FIGHTER) {
            throw new IllegalStateException("Only Fighters can have a Fighting Style");
        }

        this.fightingStyle = style;

        // Remove any existing fighting style feature
        classFeatures.removeIf(f -> f.getId().equals(FighterFeatures.FIGHTING_STYLE_ID));

        // Add the new fighting style feature
        if (style != null) {
            classFeatures.add(FighterFeatures.createFightingStyleFeature(style));
        }
    }

    /**
     * Checks if this character has the Improved Critical feature (Champion Fighter).
     */
    public boolean hasImprovedCritical() {
        return getFeature(FighterFeatures.IMPROVED_CRITICAL_ID).isPresent();
    }

    /**
     * Gets the critical hit threshold for this character.
     * Returns 19 for Champions with Improved Critical, 20 otherwise.
     */
    public int getCriticalThreshold() {
        return hasImprovedCritical() ? 19 : 20;
    }

    /**
     * Checks if the Barbarian is currently raging.
     */
    public boolean isRaging() {
        return getFeature(BarbarianFeatures.RAGE_ID)
            .filter(f -> f instanceof BarbarianFeatures.Rage)
            .map(f -> ((BarbarianFeatures.Rage) f).isActive())
            .orElse(false);
    }

    /**
     * Gets the rage damage bonus if raging, 0 otherwise.
     */
    public int getRageDamageBonus() {
        if (!isRaging()) return 0;

        return getFeature(BarbarianFeatures.RAGE_ID)
            .filter(f -> f instanceof BarbarianFeatures.Rage)
            .map(f -> ((BarbarianFeatures.Rage) f).getDamageBonus())
            .orElse(0);
    }

    /**
     * Ends the current rage (Barbarian only).
     */
    public void endRage() {
        getFeature(BarbarianFeatures.RAGE_ID)
            .filter(f -> f instanceof BarbarianFeatures.Rage)
            .map(f -> (BarbarianFeatures.Rage) f)
            .ifPresent(BarbarianFeatures.Rage::endRage);
    }

    /**
     * Checks if Reckless Attack is active this turn (Barbarian only).
     */
    public boolean isRecklessAttackActive() {
        return getFeature(BarbarianFeatures.RECKLESS_ATTACK_ID)
            .filter(f -> f instanceof BarbarianFeatures.RecklessAttack)
            .map(f -> ((BarbarianFeatures.RecklessAttack) f).isActiveThisTurn())
            .orElse(false);
    }

    /**
     * Resets Reckless Attack at start of turn (Barbarian only).
     */
    public void resetRecklessAttack() {
        getFeature(BarbarianFeatures.RECKLESS_ATTACK_ID)
            .filter(f -> f instanceof BarbarianFeatures.RecklessAttack)
            .map(f -> (BarbarianFeatures.RecklessAttack) f)
            .ifPresent(BarbarianFeatures.RecklessAttack::resetTurn);
    }

    /**
     * Gets a list of available activated features (for display in combat).
     */
    public List<ActivatedFeature> getAvailableActivatedFeatures(boolean inCombat) {
        List<ActivatedFeature> available = new ArrayList<>();
        for (ClassFeature feature : classFeatures) {
            if (feature instanceof ActivatedFeature activated) {
                boolean canUse = inCombat ? feature.isAvailableInCombat() : feature.isAvailableOutOfCombat();
                if (canUse && activated.canUse()) {
                    available.add(activated);
                }
            }
        }
        return available;
    }

    public void addSkillProficiency(Skill skill) {
        proficientSkills.add(skill);
    }

    public void removeSkillProficiency(Skill skill) {
        proficientSkills.remove(skill);
    }

    public boolean isProficientIn(Skill skill) {
        return proficientSkills.contains(skill);
    }
    
    public int getSkillModifier(Skill skill) {
        int modifier = getAbilityModifier(skill.getAbility());
        if (proficientSkills.contains(skill)) {
            // Expertise doubles proficiency bonus
            if (expertiseSkills.contains(skill)) {
                modifier += getProficiencyBonus() * 2;
            } else {
                modifier += getProficiencyBonus();
            }
        }
        return modifier;
    }

    public Set<Skill> getProficientSkills() {
        return EnumSet.copyOf(proficientSkills);
    }

    /**
     * Checks if a skill has expertise (Rogue feature).
     */
    public boolean hasExpertise(Skill skill) {
        return expertiseSkills.contains(skill);
    }

    /**
     * Gets all skills with expertise.
     */
    public Set<Skill> getExpertiseSkills() {
        return EnumSet.copyOf(expertiseSkills);
    }

    /**
     * Sets the expertise skills (Rogue only).
     * Skills must already be proficient skills.
     *
     * @param skills the skills to grant expertise in (typically 2 at level 1, 2 more at level 6)
     * @throws IllegalStateException if character is not a Rogue
     * @throws IllegalArgumentException if any skill is not a proficient skill
     */
    public void setExpertiseSkills(Set<Skill> skills) {
        if (characterClass != CharacterClass.ROGUE) {
            throw new IllegalStateException("Only Rogues can have Expertise");
        }
        for (Skill skill : skills) {
            if (!proficientSkills.contains(skill)) {
                throw new IllegalArgumentException(
                    "Cannot have expertise in " + skill.getDisplayName() + " - not proficient");
            }
        }

        this.expertiseSkills = EnumSet.copyOf(skills);

        // Remove any existing expertise feature and re-add with new skills
        classFeatures.removeIf(f -> f.getId().equals(RogueFeatures.EXPERTISE_ID));
        if (!expertiseSkills.isEmpty()) {
            classFeatures.add(RogueFeatures.createExpertise(expertiseSkills));
        }
    }

    /**
     * Adds expertise to a skill (Rogue only).
     */
    public void addExpertise(Skill skill) {
        if (characterClass != CharacterClass.ROGUE) {
            throw new IllegalStateException("Only Rogues can have Expertise");
        }
        if (!proficientSkills.contains(skill)) {
            throw new IllegalArgumentException(
                "Cannot have expertise in " + skill.getDisplayName() + " - not proficient");
        }

        expertiseSkills.add(skill);

        // Update the expertise feature
        classFeatures.removeIf(f -> f.getId().equals(RogueFeatures.EXPERTISE_ID));
        classFeatures.add(RogueFeatures.createExpertise(expertiseSkills));
    }
    
    public boolean hasSavingThrowProficiency(Ability ability) {
        return savingThrowProficiencies.contains(ability);
    }

    public int getSavingThrowModifier(Ability ability) {
        int modifier = getAbilityModifier(ability);
        if (savingThrowProficiencies.contains(ability)) {
            modifier += getProficiencyBonus();
        }
        return modifier;
    }
    
    public int makeAbilityCheck(Ability ability) {
        return Dice.rollWithModifier(20, getAbilityModifier(ability));
    }

    public int makeSkillCheck(Skill skill) {
        return Dice.rollWithModifier(20, getSkillModifier(skill));
    }

    public int makeSavingThrow(Ability ability) {
        return Dice.rollWithModifier(20, getSavingThrowModifier(ability));
    }

    public boolean makeAbilityCheckAgainstDC(Ability ability, int dc) {
        return Dice.checkAgainstDC(getAbilityModifier(ability), dc);
    }

    public boolean makeSkillCheckAgainstDC(Skill skill, int dc) {
        return Dice.checkAgainstDC(getSkillModifier(skill), dc);
    }
 
    public boolean makeSavingThrowAgainstDC(Ability ability, int dc) {
        return Dice.checkAgainstDC(getSavingThrowModifier(ability), dc);
    }
    
    public int getTemporaryHitPoints() {
        return temporaryHitPoints;
    }
    
    public void setTemporaryHitPoints(int amount) {
        temporaryHitPoints = Math.max(temporaryHitPoints, amount);
    }

    /**
     * Clears all temporary hit points (used on long rest).
     */
    public void clearTemporaryHitPoints() {
        temporaryHitPoints = 0;
    }

    public void fullHeal() {
        currentHitPoints = maxHitPoints;
    }
 
    public int getLevel() {
        return level;
    }
 
    public int getExperiencePoints() {
        return experiencePoints;
    }

    public int getXpForNextLevel() {
        if (level >= XP_THRESHOLDS.length) {
            return -1;
        }
        return XP_THRESHOLDS[level];
    }

    public int addExperience(int xp) {
        if (xp <= 0) return 0;
        
        experiencePoints += xp;
        int levelsGained = 0;

        while (level < XP_THRESHOLDS.length && experiencePoints >= XP_THRESHOLDS[level]) {
            levelUp();
            levelsGained++;
        }
        
        return levelsGained;
    }

    private void levelUp() {
        level++;

        int oldMax = maxHitPoints;
        maxHitPoints = calculateMaxHitPoints();

        currentHitPoints += (maxHitPoints - oldMax);
        availableHitDice++;  // Gain one hit die per level

        // Update class features for new level
        initializeClassFeatures();
    }
    
    public void setLevel(int newLevel) {
        if (newLevel < 1) newLevel = 1;
        if (newLevel > 20) newLevel = 20;

        this.level = newLevel;
        this.maxHitPoints = calculateMaxHitPoints();
        this.currentHitPoints = Math.min(currentHitPoints, maxHitPoints);
        this.availableHitDice = Math.min(availableHitDice, level);  // Cap at new level

        // Update class features for new level
        initializeClassFeatures();
    }

    public void setArmorBonus(int bonus) {
        this.armorBonus = Math.max(0, bonus);
    }

    public void setShieldBonus(int bonus) {
        this.shieldBonus = Math.max(0, bonus);
    }

    public int getArmorBonus() {
        return armorBonus;
    }

    public int getShieldBonus() {
        return shieldBonus;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Race getRace() {
        return race;
    }

    /**
     * Sets the two bonus abilities for Half-Elf characters.
     * Per D&D 5e PHB, Half-Elves get +2 CHA and +1 to two other abilities of choice.
     *
     * @param ability1 first ability to receive +1 bonus (cannot be CHARISMA)
     * @param ability2 second ability to receive +1 bonus (cannot be CHARISMA)
     * @throws IllegalArgumentException if either ability is CHARISMA or if abilities are the same
     * @throws IllegalStateException if character is not a Half-Elf
     */
    public void setHalfElfBonusAbilities(Ability ability1, Ability ability2) {
        if (race != Race.HALF_ELF) {
            throw new IllegalStateException("Only Half-Elf characters can set bonus abilities");
        }
        if (ability1 == Ability.CHARISMA || ability2 == Ability.CHARISMA) {
            throw new IllegalArgumentException("Half-Elf bonus abilities cannot include Charisma (already +2)");
        }
        if (ability1 == ability2) {
            throw new IllegalArgumentException("Half-Elf bonus abilities must be different");
        }

        halfElfBonusAbilities.clear();
        halfElfBonusAbilities.add(ability1);
        halfElfBonusAbilities.add(ability2);

        // Recalculate HP if CON was affected
        if (ability1 == Ability.CONSTITUTION || ability2 == Ability.CONSTITUTION) {
            int oldMax = maxHitPoints;
            maxHitPoints = calculateMaxHitPoints();
            if (oldMax > 0) {
                currentHitPoints = (int) ((double) currentHitPoints / oldMax * maxHitPoints);
            }
        }

        // Update carrying capacity if STR was affected
        if (ability1 == Ability.STRENGTH || ability2 == Ability.STRENGTH) {
            inventory.setCarryingCapacityFromStrength(getAbilityScore(Ability.STRENGTH));
        }
    }

    /**
     * Gets the Half-Elf bonus abilities.
     *
     * @return unmodifiable set of abilities receiving +1 bonus, empty if not set or not Half-Elf
     */
    public Set<Ability> getHalfElfBonusAbilities() {
        return Collections.unmodifiableSet(halfElfBonusAbilities);
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    public String getAbilityScoresString() {
        StringBuilder sb = new StringBuilder();
        for (Ability ability : Ability.values()) {
            int score = getAbilityScore(ability);
            int mod = getAbilityModifier(ability);
            String modStr = mod >= 0 ? "+" + mod : String.valueOf(mod);
            sb.append(String.format("%s: %d (%s)  ", ability.getAbbreviation(), score, modStr));
        }
        return sb.toString().trim();
    }
    
    @Override
    public String toString() {
        return String.format("%s - Level %d %s %s (HP: %d/%d, AC: %d)",
                name, level, race.getDisplayName(), characterClass.getDisplayName(),
                currentHitPoints, maxHitPoints, getArmorClass());
    }
}