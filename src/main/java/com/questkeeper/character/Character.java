package com.questkeeper.character;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a player character in the game.
 * 
 * Handles ability scores, derived stats, class/race features,
 * and D&D 5e mechanics like proficiency bonuses and skill checks.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class Character {

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
        HUMAN( "Human", 0, 0, 0, 0, 0, 0, 30),              // +1 to all (handled separately)
        DWARF("Dwarf", 0, 0, 2, 0, 0, 0, 25),               // +2 CON
        ELF("Elf", 0, 2, 0, 0, 0, 0, 30),                   // +2 DEX
        HALFLING("Halfling", 0, 2, 0, 0, 0, 0, 25),         // +2 DEX
        DRAGONBORN("Dragonborn", 2, 0, 0, 0, 0, 1, 30),     // +2 STR, +1 CHA
        GNOME("Gnome", 0, 0, 0, 2, 0, 0, 25),               // +2 INT
        HALF_ELF("Half-Elf", 0, 0, 0, 0, 0, 0, 30),         // +2 CHA, +1 to two others
        HALF_ORC("Half-Orc", 2, 0, 1, 0, 0, 0, 30),         // +2 STR, +1 CON
        TIEFLING("Tiefling", 0, 0, 0, 1, 0, 2, 30);         // +2 CHA, +1 INT

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

        public String getDisplayName() {
            return displayName; 
        }
        public int getStrBonus() { 
            return strBonus; 
        }
        public int getDexBonus() { 
            return dexBonus; 
        }
        public int getConBonus() { 
            return conBonus; 
        }
        public int getIntBonus() { 
            return intBonus; 
        }
        public int getWisBonus() { 
            return wisBonus; 
        }
        public int getChaBonus() { 
            return chaBonus; 
        }
        public int getSpeed() { 
            return speed; 
        }

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
    
    private int armorBonus;
    private int shieldBonus;

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
        
        this.armorBonus = 0;
        this.shieldBonus = 0;
    }

    public Character(String name, Race race, CharacterClass characterClass,
                     int str, int dex, int con, int intel, int wis, int cha) {
        this(name, race, characterClass);
        setAbilityScores(str, dex, con, intel, wis, cha);
        
        // Recalculate HP with new CON
        this.maxHitPoints = calculateMaxHitPoints();
        this.currentHitPoints = maxHitPoints;
    }
}
