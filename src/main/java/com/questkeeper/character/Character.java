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
}
