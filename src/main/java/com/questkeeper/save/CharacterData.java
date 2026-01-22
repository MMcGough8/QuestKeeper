package com.questkeeper.save;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;

import java.util.*;

/**
 * Serializable representation of Character data for save/load operations.
 * 
 * Separates persistence concerns from the Character class itself.
 * Handles conversion between Character objects and YAML-friendly Maps.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class CharacterData {

    // Identity
    private String name;
    private String race;        
    private String characterClass;
    
    // Progression
    private int level;
    private int experiencePoints;
    
    // Ability scores (base, before racial bonuses)
    private Map<String, Integer> baseAbilityScores;
    
    // Proficiencies
    private Set<String> skillProficiencies;
    private Set<String> savingThrowProficiencies;
    
    // Combat state
    private int currentHitPoints;
    private int maxHitPoints;
    private int temporaryHitPoints;
    private int armorBonus;
    private int shieldBonus;

    // Rest state
    private int availableHitDice;


    //creates empty CharacterData
    public CharacterData() {
        this.baseAbilityScores = new HashMap<>();
        this.skillProficiencies = new HashSet<>();
        this.savingThrowProficiencies = new HashSet<>();
    }

    //creates CharacterData from existing Character
    public static CharacterData fromCharacter(Character character) {
        CharacterData data = new CharacterData();
        
        // Identity
        data.name = character.getName();
        data.race = character.getRace().name();
        data.characterClass = character.getCharacterClass().name();
        
        // Progression
        data.level = character.getLevel();
        data.experiencePoints = character.getExperiencePoints();
        
        // Ability scores (store BASE scores, not with racial bonuses)
        for (Ability ability : Ability.values()) {
            data.baseAbilityScores.put(ability.name(), character.getBaseAbilityScore(ability));
        }
        
        // Skill proficiencies
        for (Skill skill : character.getProficientSkills()) {
            data.skillProficiencies.add(skill.name());
        }
        
        // Saving throw proficiencies
        for (Ability ability : Ability.values()) {
            if (character.hasSavingThrowProficiency(ability)) {
                data.savingThrowProficiencies.add(ability.name());
            }
        }
        
        // Combat state
        data.currentHitPoints = character.getCurrentHitPoints();
        data.maxHitPoints = character.getMaxHitPoints();
        data.temporaryHitPoints = character.getTemporaryHitPoints();
        data.armorBonus = character.getArmorBonus();
        data.shieldBonus = character.getShieldBonus();

        // Rest state
        data.availableHitDice = character.getAvailableHitDice();

        return data;
    }

     /**
     * Restores a Character from CharacterData.
     */
    public Character toCharacter() {
    // Parse enums
    Race raceEnum = Race.valueOf(race);
    CharacterClass classEnum = CharacterClass.valueOf(characterClass);
    
    // Create character
    Character character = new Character(name, raceEnum, classEnum);
    
    // Restore ability scores FIRST (affects HP calculation)
    for (Map.Entry<String, Integer> entry : baseAbilityScores.entrySet()) {
        Ability ability = Ability.valueOf(entry.getKey());
        character.setAbilityScore(ability, entry.getValue());
    }
    
    // Set level (will recalculate max HP)
    character.setLevel(level);
    character.setExperiencePoints(experiencePoints);
    
    // Restore skill proficiencies
    for (String skillName : skillProficiencies) {
        try {
            Skill skill = Skill.valueOf(skillName);
            character.addSkillProficiency(skill);
        } catch (IllegalArgumentException e) {
            // Skip unknown skills (forward compatibility)
        }
    }
        
    // Restore combat state
    character.setArmorBonus(armorBonus);
    character.setShieldBonus(shieldBonus);
    character.setTemporaryHitPoints(temporaryHitPoints);

    // Restore HP by healing to full, then damaging to reach saved HP
    character.setCurrentHitPoints(currentHitPoints);

    // Restore rest state
    character.setAvailableHitDice(availableHitDice);

    return character;
}
    /**
     * Converts to Map for YAML serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("name", name);
        map.put("race", race);
        map.put("class", characterClass);
        map.put("level", level);
        map.put("experience_points", experiencePoints);

        map.put("ability_scores", new LinkedHashMap<>(baseAbilityScores));
        map.put("skill_proficiencies", new ArrayList<>(skillProficiencies));
        map.put("saving_throw_proficiencies", new ArrayList<> (savingThrowProficiencies));

        map.put("current_hp", currentHitPoints);
        map.put("max_hp", maxHitPoints);
        map.put("temp_hp", temporaryHitPoints);
        map.put("armor_bonus", armorBonus);
        map.put("shield_bonus", shieldBonus);
        map.put("available_hit_dice", availableHitDice);

        return map;

    }

    /**
     * Creates CharacterData from a Map (YAML deserialization).
     */
    @SuppressWarnings("unchecked")
    public static CharacterData fromMap(Map<String, Object> data) {
        CharacterData cd = new CharacterData();
        
        cd.name = (String) data.get("name");
        cd.race = (String) data.get("race");
        cd.characterClass = (String) data.get("class");
        cd.level = getInt(data, "level", 1);
        cd.experiencePoints = getInt(data, "experience_points", 0);
        
        // Ability scores
        Map<String, Object> scores = (Map<String, Object>) data.get("ability_scores");
        if (scores != null) {
            for (Map.Entry<String, Object> entry : scores.entrySet()) {
                cd.baseAbilityScores.put(entry.getKey(), ((Number) entry.getValue()).intValue());
            }
        }
        
        // Skills
        List<String> skills = (List<String>) data.get("skill_proficiencies");
        if (skills != null) {
            cd.skillProficiencies = new HashSet<>(skills);
        }
        
        // Saving throws
        List<String> saves = (List<String>) data.get("saving_throw_proficiencies");
        if (saves != null) {
            cd.savingThrowProficiencies = new HashSet<>(saves);
        }
        
        // Combat
        cd.currentHitPoints = getInt(data, "current_hp", 10);
        cd.maxHitPoints = getInt(data, "max_hp", 10);
        cd.temporaryHitPoints = getInt(data, "temp_hp", 0);
        cd.armorBonus = getInt(data, "armor_bonus", 0);
        cd.shieldBonus = getInt(data, "shield_bonus", 0);

        // Rest state (default to level for old saves without hit dice tracking)
        cd.availableHitDice = getInt(data, "available_hit_dice", cd.level);

        return cd;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    public String getName() { 
        return name; 
    }

    public String getRace() { 
        return race; 
    }

    public String getCharacterClass() { 
        return characterClass; 
    }

    public int getLevel() { 
        return level; 
    }

    public int getExperiencePoints() { 
        return experiencePoints; 
    }

    public int getCurrentHitPoints() { 
        return currentHitPoints; 
    }

    public int getMaxHitPoints() { 
        return maxHitPoints; 
    }

    @Override
    public String toString() {
        return String.format("CharacterData[%s, Level %d %s %s, HP: %d/%d]",
            name, level, race, characterClass, currentHitPoints, maxHitPoints);
    }
}


