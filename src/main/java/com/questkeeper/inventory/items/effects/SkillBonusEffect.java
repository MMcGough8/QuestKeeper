package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that provides bonuses to specific skills or tool checks.
 * 
 * Examples: Gearbreaker's Kit, Eyes of the Eagle, Cloak of Elvenkind,
 * Boots of Elvenkind, Gloves of Thievery
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class SkillBonusEffect extends AbstractItemEffect {
    
    /**
     * Skills that can receive bonuses.
     */
    public enum Skill {
        // Strength
        ATHLETICS("Athletics", "STR"),
        
        // Dexterity
        ACROBATICS("Acrobatics", "DEX"),
        SLEIGHT_OF_HAND("Sleight of Hand", "DEX"),
        STEALTH("Stealth", "DEX"),
        
        // Intelligence
        ARCANA("Arcana", "INT"),
        HISTORY("History", "INT"),
        INVESTIGATION("Investigation", "INT"),
        NATURE("Nature", "INT"),
        RELIGION("Religion", "INT"),
        
        // Wisdom
        ANIMAL_HANDLING("Animal Handling", "WIS"),
        INSIGHT("Insight", "WIS"),
        MEDICINE("Medicine", "WIS"),
        PERCEPTION("Perception", "WIS"),
        SURVIVAL("Survival", "WIS"),
        
        // Charisma
        DECEPTION("Deception", "CHA"),
        INTIMIDATION("Intimidation", "CHA"),
        PERFORMANCE("Performance", "CHA"),
        PERSUASION("Persuasion", "CHA"),
        
        // Tool proficiencies (selected common ones)
        THIEVES_TOOLS("Thieves' Tools", "DEX"),
        TINKERS_TOOLS("Tinker's Tools", "INT"),
        ALCHEMISTS_SUPPLIES("Alchemist's Supplies", "INT"),
        DISGUISE_KIT("Disguise Kit", "CHA"),
        FORGERY_KIT("Forgery Kit", "DEX"),
        HERBALISM_KIT("Herbalism Kit", "WIS"),
        POISONERS_KIT("Poisoner's Kit", "INT"),
        
        // Special categories
        ALL_SKILLS("All Skills", ""),
        DISABLE_TRAPS("Disable Traps", "DEX");
        
        private final String displayName;
        private final String abilityShort;
        
        Skill(String displayName, String abilityShort) {
            this.displayName = displayName;
            this.abilityShort = abilityShort;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getAbilityShort() {
            return abilityShort;
        }
    }
    
    /**
     * Types of skill bonuses.
     */
    public enum BonusType {
        FLAT_BONUS,         // +X to checks
        ADVANTAGE,          // Advantage on checks
        PROFICIENCY,        // Grants proficiency
        EXPERTISE,          // Double proficiency bonus
        AUTO_SUCCESS        // Automatically succeed (rare)
    }
    
    private Skill skill;
    private BonusType bonusType;
    private int bonusAmount;            // For FLAT_BONUS
    private boolean requiresAttunement;
    private int charges;                // For limited-use bonuses (0 = unlimited)
    private String specialCondition;    // e.g., "only against mechanical traps"
    
    public SkillBonusEffect(String id, String name, Skill skill, int bonusAmount) {
        super(id, name, createDescription(skill, BonusType.FLAT_BONUS, bonusAmount), 
                UsageType.PASSIVE, -1);
        this.skill = skill;
        this.bonusType = BonusType.FLAT_BONUS;
        this.bonusAmount = bonusAmount;
        this.requiresAttunement = false;
        this.charges = 0;
        this.specialCondition = null;
    }
    
    public SkillBonusEffect(String id, String name, Skill skill, BonusType bonusType) {
        super(id, name, createDescription(skill, bonusType, 0), UsageType.PASSIVE, -1);
        this.skill = skill;
        this.bonusType = bonusType;
        this.bonusAmount = 0;
        this.requiresAttunement = false;
        this.charges = 0;
        this.specialCondition = null;
    }
    
    public SkillBonusEffect(String id, String name, Skill skill, int bonusAmount,
                            int charges) {
        super(id, name, createDescription(skill, BonusType.FLAT_BONUS, bonusAmount),
                UsageType.CHARGES, charges);
        this.skill = skill;
        this.bonusType = BonusType.FLAT_BONUS;
        this.bonusAmount = bonusAmount;
        this.requiresAttunement = false;
        this.charges = charges;
        this.specialCondition = null;
    }
    
    private static String createDescription(Skill skill, BonusType type, int bonus) {
        return switch (type) {
            case FLAT_BONUS -> String.format("You gain +%d to %s checks.", bonus, skill.getDisplayName());
            case ADVANTAGE -> String.format("You have advantage on %s checks.", skill.getDisplayName());
            case PROFICIENCY -> String.format("You gain proficiency in %s.", skill.getDisplayName());
            case EXPERTISE -> String.format("Your proficiency bonus is doubled for %s checks.", 
                    skill.getDisplayName());
            case AUTO_SUCCESS -> String.format("You automatically succeed on %s checks.", 
                    skill.getDisplayName());
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        return switch (bonusType) {
            case FLAT_BONUS -> String.format("%s gains +%d to %s!", 
                    user.getName(), bonusAmount, skill.getDisplayName());
            case ADVANTAGE -> String.format("%s has advantage on %s!", 
                    user.getName(), skill.getDisplayName());
            case PROFICIENCY -> String.format("%s is proficient in %s!", 
                    user.getName(), skill.getDisplayName());
            case EXPERTISE -> String.format("%s has expertise in %s!", 
                    user.getName(), skill.getDisplayName());
            case AUTO_SUCCESS -> String.format("%s automatically succeeds at %s!", 
                    user.getName(), skill.getDisplayName());
        };
    }
    

    public int calculateBonus(int proficiencyBonus, boolean hasProficiency) {
        return switch (bonusType) {
            case FLAT_BONUS -> bonusAmount;
            case PROFICIENCY -> hasProficiency ? 0 : proficiencyBonus;
            case EXPERTISE -> hasProficiency ? proficiencyBonus : proficiencyBonus * 2;
            default -> 0;
        };
    }

    public boolean grantsAdvantage() {
        return bonusType == BonusType.ADVANTAGE;
    }
    
    public boolean appliesTo(Skill targetSkill) {
        if (skill == Skill.ALL_SKILLS) {
            return true;
        }
        return skill == targetSkill;
    }

    public Skill getSkill() {
        return skill;
    }
    
    public void setSkill(Skill skill) {
        this.skill = skill;
    }
    
    public BonusType getBonusType() {
        return bonusType;
    }
    
    public void setBonusType(BonusType bonusType) {
        this.bonusType = bonusType;
    }
    
    public int getBonusAmount() {
        return bonusAmount;
    }
    
    public void setBonusAmount(int bonusAmount) {
        this.bonusAmount = bonusAmount;
    }
    
    public boolean requiresAttunement() {
        return requiresAttunement;
    }
    
    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }
    
    public String getSpecialCondition() {
        return specialCondition;
    }
    
    public void setSpecialCondition(String specialCondition) {
        this.specialCondition = specialCondition;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append(skill.getDisplayName()).append(": ");
        
        switch (bonusType) {
            case FLAT_BONUS -> sb.append("+").append(bonusAmount);
            case ADVANTAGE -> sb.append("Advantage");
            case PROFICIENCY -> sb.append("Proficiency");
            case EXPERTISE -> sb.append("Expertise");
            case AUTO_SUCCESS -> sb.append("Auto-success");
        }
        sb.append("\n");
        
        if (specialCondition != null) {
            sb.append("Condition: ").append(specialCondition).append("\n");
        }
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }
 
    /**
     * Creates Gearbreaker's Kit from Muddlebrook.
     * +2 to disable mechanical traps, 3 charges.
     */
    public static SkillBonusEffect createGearbrakersKit() {
        SkillBonusEffect effect = new SkillBonusEffect("gearbreakers_kit_effect",
                "Gearbreaker's Kit", Skill.DISABLE_TRAPS, 2, 3);
        effect.setSpecialCondition("mechanical traps only");
        effect.setDescription("+2 to checks to disable mechanical traps. 3 charges.");
        return effect;
    }
    
    /**
     * Creates Eyes of the Eagle (+5 Perception, advantage on sight).
     */
    public static SkillBonusEffect createEyesOfTheEagle() {
        SkillBonusEffect effect = new SkillBonusEffect("eyes_eagle_effect",
                "Eagle Eyes", Skill.PERCEPTION, 5);
        effect.setRequiresAttunement(true);
        effect.setDescription("+5 to Perception checks that rely on sight. Advantage on Perception checks in daylight.");
        return effect;
    }
    
    /**
     * Creates Cloak of Elvenkind (advantage on Stealth).
     */
    public static SkillBonusEffect createCloakOfElvenkind() {
        SkillBonusEffect effect = new SkillBonusEffect("cloak_elvenkind_effect",
                "Elvenkind", Skill.STEALTH, BonusType.ADVANTAGE);
        effect.setRequiresAttunement(true);
        effect.setDescription("Advantage on Stealth checks. Others have disadvantage on Perception to see you.");
        return effect;
    }
    
    /**
     * Creates Boots of Elvenkind (advantage on Stealth to move silently).
     */
    public static SkillBonusEffect createBootsOfElvenkind() {
        SkillBonusEffect effect = new SkillBonusEffect("boots_elvenkind_effect",
                "Silent Step", Skill.STEALTH, BonusType.ADVANTAGE);
        effect.setSpecialCondition("to move silently");
        effect.setDescription("Advantage on Stealth checks to move silently.");
        return effect;
    }
    
    /**
     * Creates Gloves of Thievery (+5 Sleight of Hand and lockpicking).
     */
    public static SkillBonusEffect createGlovesOfThievery() {
        SkillBonusEffect effect = new SkillBonusEffect("gloves_thievery_effect",
                "Thievery", Skill.SLEIGHT_OF_HAND, 5);
        effect.setDescription("+5 to Sleight of Hand checks and to pick locks.");
        return effect;
    }
    
    /**
     * Creates Cloak Pin of Minor Disguise from Muddlebrook.
     */
    public static SkillBonusEffect createCloakPinOfMinorDisguise() {
        SkillBonusEffect effect = new SkillBonusEffect("cloak_pin_disguise_effect",
                "Minor Disguise", Skill.DECEPTION, BonusType.ADVANTAGE);
        effect.setUsageType(UsageType.DAILY);
        effect.setMaxCharges(1);
        effect.setDescription("Change your voice or facial features subtly for 10 minutes. Advantage on Deception to maintain disguise.");
        return effect;
    }
    
    /**
     * Creates a generic tool proficiency granting effect.
     */
    public static SkillBonusEffect createToolProficiency(String toolName, Skill toolSkill) {
        return new SkillBonusEffect(toolName.toLowerCase().replace(" ", "_") + "_prof_effect",
                toolName + " Proficiency", toolSkill, BonusType.PROFICIENCY);
    }
}