package com.questkeeper.save;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.character.features.ActivatedFeature;
import com.questkeeper.character.features.ClassFeature;
import com.questkeeper.character.features.FightingStyle;

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

    // Class/race choice fields (round-tripped because re-derivation isn't possible)
    private String fightingStyle;                  // FightingStyle enum name, null if unset
    private Set<String> expertiseSkills;           // Skill enum names
    private Set<String> halfElfBonusAbilities;     // Ability enum names; size 0 or 2

    // Subclass choices — enum names from each class's choice enum, null if unset.
    private String divineDomain;        // Cleric
    private String arcaneTradition;     // Wizard
    private String sorcerousOrigin;     // Sorcerer
    private String bardCollege;         // Bard
    private String druidCircle;         // Druid
    private String warlockPatron;       // Warlock
    private String warlockPactBoon;     // Warlock

    // Activated feature use counts (Lay on Hands pool, Ki, Action Surge,
    // Rage, etc.). Keyed by feature ID; values are current uses / pool.
    private Map<String, Integer> featureUses;

    // Unspent Ability Score Improvements waiting for a UI prompt.
    private int pendingAbilityScoreImprovements;

    // Spellcasting state (Wizard/Cleric/Druid/Paladin/Sorcerer/Bard/Warlock/Ranger).
    // Empty/null on non-casters and on saves predating Phase 1.3.
    private Set<String> knownCantripIds;
    private Set<String> knownSpellIds;
    /** current spell slots remaining; index 0 = 1st-level slot. Sized 9 for L20 casters. */
    private int[] spellSlotsCurrent;

    //creates empty CharacterData
    public CharacterData() {
        this.baseAbilityScores = new HashMap<>();
        this.skillProficiencies = new HashSet<>();
        this.savingThrowProficiencies = new HashSet<>();
        this.expertiseSkills = new HashSet<>();
        this.halfElfBonusAbilities = new HashSet<>();
        this.featureUses = new HashMap<>();
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

        // Class/race choice fields
        if (character.getFightingStyle() != null) {
            data.fightingStyle = character.getFightingStyle().name();
        }
        for (Skill skill : character.getExpertiseSkills()) {
            data.expertiseSkills.add(skill.name());
        }
        for (Ability ability : character.getHalfElfBonusAbilities()) {
            data.halfElfBonusAbilities.add(ability.name());
        }

        // Subclass choices (Phase 3.6 classes)
        if (character.getDivineDomain() != null) {
            data.divineDomain = character.getDivineDomain().name();
        }
        if (character.getArcaneTradition() != null) {
            data.arcaneTradition = character.getArcaneTradition().name();
        }
        if (character.getSorcerousOrigin() != null) {
            data.sorcerousOrigin = character.getSorcerousOrigin().name();
        }
        if (character.getBardCollege() != null) {
            data.bardCollege = character.getBardCollege().name();
        }
        if (character.getDruidCircle() != null) {
            data.druidCircle = character.getDruidCircle().name();
        }
        if (character.getWarlockPatron() != null) {
            data.warlockPatron = character.getWarlockPatron().name();
        }
        if (character.getWarlockPactBoon() != null) {
            data.warlockPactBoon = character.getWarlockPactBoon().name();
        }

        // Activated feature use counts (Lay on Hands pool, Ki, Action Surge,
        // Rage, Channel Divinity, Divine Sense, Second Wind...)
        for (ClassFeature feature : character.getClassFeatures()) {
            if (feature instanceof ActivatedFeature af) {
                data.featureUses.put(af.getId(), af.getCurrentUses());
            }
        }

        data.pendingAbilityScoreImprovements = character.getPendingAbilityScoreImprovements();

        // Spellbook state (skipped for non-casters where canCastSpells()
        // returns false; their data fields stay null/empty).
        com.questkeeper.magic.Spellbook sb = character.getSpellbook();
        if (sb != null && sb.canCastSpells()) {
            data.knownCantripIds = sb.getKnownCantripIds();
            data.knownSpellIds = sb.getKnownSpellIds();
            com.questkeeper.magic.SpellSlots ss = sb.getSpellSlots();
            if (ss != null) {
                data.spellSlotsCurrent = ss.getCurrentSlots();
            }
        }

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
    
    // Skill proficiencies must be restored before expertise (expertise
    // requires proficiency) and before setLevel (Rogue features rely on it).
    for (String skillName : skillProficiencies) {
        try {
            character.addSkillProficiency(Skill.valueOf(skillName));
        } catch (IllegalArgumentException e) {
            // Unknown skill; skip
        }
    }

    // Saving throw proficiencies (constructor adds class defaults; this
    // restores any non-default ones that were saved).
    for (String saveName : savingThrowProficiencies) {
        try {
            character.addSavingThrowProficiency(Ability.valueOf(saveName));
        } catch (IllegalArgumentException e) {
            // Unknown ability; skip
        }
    }

    // Class/race choice fields must be set BEFORE setLevel so that
    // initializeClassFeatures sees them when adding level-gated features.
    if (halfElfBonusAbilities != null && halfElfBonusAbilities.size() == 2) {
        Iterator<String> it = halfElfBonusAbilities.iterator();
        try {
            Ability a1 = Ability.valueOf(it.next());
            Ability a2 = Ability.valueOf(it.next());
            character.setHalfElfBonusAbilities(a1, a2);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Invalid bonus abilities (wrong race or unknown enum); skip
        }
    }
    if (fightingStyle != null) {
        try {
            character.setFightingStyle(FightingStyle.valueOf(fightingStyle));
        } catch (IllegalArgumentException e) {
            // Unknown fighting style; skip
        }
    }
    if (expertiseSkills != null && !expertiseSkills.isEmpty()) {
        Set<Skill> skills = new HashSet<>();
        for (String name : expertiseSkills) {
            try {
                skills.add(Skill.valueOf(name));
            } catch (IllegalArgumentException e) {
                // Unknown skill; skip
            }
        }
        if (!skills.isEmpty()) {
            character.setExpertiseSkills(skills);
        }
    }

    // Subclass choices must be restored BEFORE setLevel so that
    // ClassFeatureWiring sees them when adding level-gated features.
    if (divineDomain != null) {
        try {
            character.setDivineDomain(
                com.questkeeper.character.features.ClericFeatures.DivineDomain.valueOf(divineDomain));
        } catch (RuntimeException e) { /* unknown; skip */ }
    }
    if (arcaneTradition != null) {
        try {
            character.setArcaneTradition(
                com.questkeeper.character.features.WizardFeatures.ArcaneTradition.valueOf(arcaneTradition));
        } catch (RuntimeException e) { /* skip */ }
    }
    if (sorcerousOrigin != null) {
        try {
            character.setSorcerousOrigin(
                com.questkeeper.character.features.SorcererFeatures.SorcerousOrigin.valueOf(sorcerousOrigin));
        } catch (RuntimeException e) { /* skip */ }
    }
    if (bardCollege != null) {
        try {
            character.setBardCollege(
                com.questkeeper.character.features.BardFeatures.BardCollege.valueOf(bardCollege));
        } catch (RuntimeException e) { /* skip */ }
    }
    if (druidCircle != null) {
        try {
            character.setDruidCircle(
                com.questkeeper.character.features.DruidFeatures.DruidCircle.valueOf(druidCircle));
        } catch (RuntimeException e) { /* skip */ }
    }
    if (warlockPatron != null) {
        try {
            character.setWarlockPatron(
                com.questkeeper.character.features.WarlockFeatures.OtherworldlyPatron.valueOf(warlockPatron));
        } catch (RuntimeException e) { /* skip */ }
    }
    if (warlockPactBoon != null) {
        try {
            character.setWarlockPactBoon(
                com.questkeeper.character.features.WarlockFeatures.PactBoon.valueOf(warlockPactBoon));
        } catch (RuntimeException e) { /* skip */ }
    }

    // Set level (will recalculate max HP)
    character.setLevel(level);
    character.setExperiencePoints(experiencePoints);
    
    // Restore combat state
    character.setArmorBonus(armorBonus);
    character.setShieldBonus(shieldBonus);
    character.setTemporaryHitPoints(temporaryHitPoints);

    // Restore HP by healing to full, then damaging to reach saved HP
    character.setCurrentHitPoints(currentHitPoints);

    // Restore rest state
    character.setAvailableHitDice(availableHitDice);

    // Restore activated feature use counts (must come after setLevel which
    // (re)builds the class-features list).
    if (featureUses != null) {
        for (ClassFeature feature : character.getClassFeatures()) {
            if (feature instanceof ActivatedFeature af) {
                Integer saved = featureUses.get(af.getId());
                if (saved != null) {
                    af.setCurrentUses(saved);
                }
            }
        }
    }

    // setLevel re-grants ASIs as it crosses thresholds, so overwrite with
    // the saved (possibly-spent) count last.
    character.setPendingAbilityScoreImprovements(pendingAbilityScoreImprovements);

    // Spellbook restoration must happen AFTER setLevel — onLevelUp inside
    // setLevel restoreAlls slots and re-adds default spells, so saved
    // state would be clobbered if we ran first.
    com.questkeeper.magic.Spellbook sb = character.getSpellbook();
    if (sb != null) {
        if (knownCantripIds != null) {
            for (String id : knownCantripIds) sb.addCantrip(id);
        }
        if (knownSpellIds != null) {
            // addKnownSpell auto-prepares (per Spellbook fix), so prepared
            // mirrors known until a preparation UI lands.
            for (String id : knownSpellIds) sb.addKnownSpell(id);
        }
        if (spellSlotsCurrent != null && sb.getSpellSlots() != null) {
            sb.getSpellSlots().setCurrentSlots(spellSlotsCurrent);
        }
    }

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

        if (fightingStyle != null) {
            map.put("fighting_style", fightingStyle);
        }
        if (expertiseSkills != null && !expertiseSkills.isEmpty()) {
            map.put("expertise_skills", new ArrayList<>(expertiseSkills));
        }
        if (halfElfBonusAbilities != null && !halfElfBonusAbilities.isEmpty()) {
            map.put("half_elf_bonus_abilities", new ArrayList<>(halfElfBonusAbilities));
        }
        if (featureUses != null && !featureUses.isEmpty()) {
            map.put("feature_uses", new LinkedHashMap<>(featureUses));
        }
        if (pendingAbilityScoreImprovements > 0) {
            map.put("pending_ability_score_improvements", pendingAbilityScoreImprovements);
        }

        if (divineDomain != null)     map.put("divine_domain", divineDomain);
        if (arcaneTradition != null)  map.put("arcane_tradition", arcaneTradition);
        if (sorcerousOrigin != null)  map.put("sorcerous_origin", sorcerousOrigin);
        if (bardCollege != null)      map.put("bard_college", bardCollege);
        if (druidCircle != null)      map.put("druid_circle", druidCircle);
        if (warlockPatron != null)    map.put("warlock_patron", warlockPatron);
        if (warlockPactBoon != null)  map.put("warlock_pact_boon", warlockPactBoon);

        if (knownCantripIds != null && !knownCantripIds.isEmpty()) {
            map.put("known_cantrips", new ArrayList<>(knownCantripIds));
        }
        if (knownSpellIds != null && !knownSpellIds.isEmpty()) {
            map.put("known_spells", new ArrayList<>(knownSpellIds));
        }
        if (spellSlotsCurrent != null && spellSlotsCurrent.length > 0) {
            List<Integer> slots = new ArrayList<>();
            for (int s : spellSlotsCurrent) slots.add(s);
            map.put("spell_slots_current", slots);
        }

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

        // Class/race choice fields (all optional, missing on old saves)
        Object fs = data.get("fighting_style");
        if (fs instanceof String fsStr) {
            cd.fightingStyle = fsStr;
        }
        List<String> exp = (List<String>) data.get("expertise_skills");
        if (exp != null) {
            cd.expertiseSkills = new HashSet<>(exp);
        }
        List<String> heAbilities = (List<String>) data.get("half_elf_bonus_abilities");
        if (heAbilities != null) {
            cd.halfElfBonusAbilities = new HashSet<>(heAbilities);
        }
        Object featureUsesObj = data.get("feature_uses");
        if (featureUsesObj instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof Number num) {
                    cd.featureUses.put(key, num.intValue());
                }
            }
        }

        cd.pendingAbilityScoreImprovements = getInt(data, "pending_ability_score_improvements", 0);

        cd.divineDomain    = (String) data.get("divine_domain");
        cd.arcaneTradition = (String) data.get("arcane_tradition");
        cd.sorcerousOrigin = (String) data.get("sorcerous_origin");
        cd.bardCollege     = (String) data.get("bard_college");
        cd.druidCircle     = (String) data.get("druid_circle");
        cd.warlockPatron   = (String) data.get("warlock_patron");
        cd.warlockPactBoon = (String) data.get("warlock_pact_boon");

        // Spellbook state (Phase 1.3+; absent on older saves)
        List<String> cantrips = (List<String>) data.get("known_cantrips");
        if (cantrips != null) {
            cd.knownCantripIds = new LinkedHashSet<>(cantrips);
        }
        List<String> spells = (List<String>) data.get("known_spells");
        if (spells != null) {
            cd.knownSpellIds = new LinkedHashSet<>(spells);
        }
        Object slotsObj = data.get("spell_slots_current");
        if (slotsObj instanceof List<?> slotsList) {
            cd.spellSlotsCurrent = new int[slotsList.size()];
            for (int i = 0; i < slotsList.size(); i++) {
                Object v = slotsList.get(i);
                cd.spellSlotsCurrent[i] = v instanceof Number n ? n.intValue() : 0;
            }
        }

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


