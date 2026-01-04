package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that sets an ability score to a fixed value.
 * 
 * These items set your score to a specific number if it's lower,
 * but have no effect if your score is already equal or higher.
 * 
 * Examples: Gauntlets of Ogre Power (STR 19), Headband of Intellect (INT 19),
 * Amulet of Health (CON 19), Belt of Giant Strength (STR varies)
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class AbilitySetEffect extends AbstractItemEffect {
    
    /**
     * Which ability score is affected.
     */
    public enum Ability {
        STRENGTH("Strength"),
        DEXTERITY("Dexterity"),
        CONSTITUTION("Constitution"),
        INTELLIGENCE("Intelligence"),
        WISDOM("Wisdom"),
        CHARISMA("Charisma");
        
        private final String displayName;
        
        Ability(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private Ability ability;
    private int setValue;           // The value to set the ability to
    private boolean requiresAttunement;
    
    /**
     * Creates an ability set effect.
     */
    public AbilitySetEffect(String id, String name, Ability ability, int setValue) {
        super(id, name, createDescription(ability, setValue), UsageType.PASSIVE, -1);
        this.ability = ability;
        this.setValue = Math.max(1, Math.min(30, setValue)); // D&D ability scores cap at 30
        this.requiresAttunement = true; // Almost always requires attunement
    }
    
    private static String createDescription(Ability ability, int value) {
        return String.format("Your %s score is %d while wearing this item. " +
                "It has no effect if your %s is already %d or higher.",
                ability.getDisplayName(), value, ability.getDisplayName(), value);
    }
    
    @Override
    protected String applyEffect(Character user) {
        // This is a passive effect, so this describes what happens when equipped
        return String.format("%s's %s is set to %d!", 
                user.getName(), ability.getDisplayName(), setValue);
    }
 
    public int getEffectiveScore(int currentScore) {
        return Math.max(currentScore, setValue);
    }
    
    public boolean wouldImproveScore(int currentScore) {
        return currentScore < setValue;
    }
 
    public int getSetModifier() {
        return (setValue - 10) / 2;
    }
  
    public Ability getAbility() {
        return ability;
    }
    
    public void setAbility(Ability ability) {
        this.ability = ability;
        setDescription(createDescription(this.ability, this.setValue));
    }
    
    public int getSetValue() {
        return setValue;
    }
    
    public void setSetValue(int setValue) {
        this.setValue = Math.max(1, Math.min(30, setValue));
        setDescription(createDescription(this.ability, this.setValue));
    }
    
    public boolean requiresAttunement() {
        return requiresAttunement;
    }
    
    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append(ability.getDisplayName()).append(" set to: ").append(setValue);
        sb.append(" (modifier: ").append(getSetModifier() >= 0 ? "+" : "")
          .append(getSetModifier()).append(")\n");
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }

    /**
     * Creates Gauntlets of Ogre Power (STR 19).
     */
    public static AbilitySetEffect createGauntletsOfOgrePower() {
        AbilitySetEffect effect = new AbilitySetEffect("gauntlets_ogre_effect",
                "Ogre Power", Ability.STRENGTH, 19);
        return effect;
    }
    
    /**
     * Creates Headband of Intellect (INT 19).
     */
    public static AbilitySetEffect createHeadbandOfIntellect() {
        AbilitySetEffect effect = new AbilitySetEffect("headband_intellect_effect",
                "Intellect", Ability.INTELLIGENCE, 19);
        return effect;
    }
    
    /**
     * Creates Amulet of Health (CON 19).
     */
    public static AbilitySetEffect createAmuletOfHealth() {
        AbilitySetEffect effect = new AbilitySetEffect("amulet_health_effect",
                "Health", Ability.CONSTITUTION, 19);
        return effect;
    }
    
    /**
     * Creates Belt of Hill Giant Strength (STR 21).
     */
    public static AbilitySetEffect createBeltOfHillGiantStrength() {
        AbilitySetEffect effect = new AbilitySetEffect("belt_hill_giant_effect",
                "Hill Giant Strength", Ability.STRENGTH, 21);
        return effect;
    }
    
    /**
     * Creates Belt of Stone Giant Strength (STR 23).
     */
    public static AbilitySetEffect createBeltOfStoneGiantStrength() {
        AbilitySetEffect effect = new AbilitySetEffect("belt_stone_giant_effect",
                "Stone Giant Strength", Ability.STRENGTH, 23);
        return effect;
    }
    
    /**
     * Creates Belt of Fire Giant Strength (STR 25).
     */
    public static AbilitySetEffect createBeltOfFireGiantStrength() {
        AbilitySetEffect effect = new AbilitySetEffect("belt_fire_giant_effect",
                "Fire Giant Strength", Ability.STRENGTH, 25);
        return effect;
    }
    
    /**
     * Creates Belt of Cloud Giant Strength (STR 27).
     */
    public static AbilitySetEffect createBeltOfCloudGiantStrength() {
        AbilitySetEffect effect = new AbilitySetEffect("belt_cloud_giant_effect",
                "Cloud Giant Strength", Ability.STRENGTH, 27);
        return effect;
    }
    
    /**
     * Creates Belt of Storm Giant Strength (STR 29).
     */
    public static AbilitySetEffect createBeltOfStormGiantStrength() {
        AbilitySetEffect effect = new AbilitySetEffect("belt_storm_giant_effect",
                "Storm Giant Strength", Ability.STRENGTH, 29);
        return effect;
    }
}