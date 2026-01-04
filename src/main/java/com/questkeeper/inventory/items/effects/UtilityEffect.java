package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect for miscellaneous utility abilities that don't fit other categories.
 * 
 * Examples: Goggles of Night (darkvision), Helm of Comprehending Languages,
 * Lantern of Revealing, Bag of Holding, Decanter of Endless Water
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class UtilityEffect extends AbstractItemEffect {
    
    /**
     * Types of utility effects.
     */
    public enum UtilityType {
        DARKVISION("Darkvision"),
        TRUESIGHT("Truesight"),
        BLINDSIGHT("Blindsight"),
        TREMORSENSE("Tremorsense"),
        COMPREHEND_LANGUAGES("Comprehend Languages"),
        SPEAK_LANGUAGE("Speak Language"),
        TELEPATHY("Telepathy"),
        LIGHT("Light"),
        DETECT_MAGIC("Detect Magic"),
        DETECT_EVIL_GOOD("Detect Evil and Good"),
        SEE_INVISIBILITY("See Invisibility"),
        STORAGE("Extra Storage"),
        PRODUCE_ITEM("Produce Item"),
        PARTY_COMMUNICATION("Party Communication"),
        DISGUISE("Disguise"),
        BREATHING("Breathing"),
        SUSTENANCE("No Need for Food/Water");
        
        private final String displayName;
        
        UtilityType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private UtilityType utilityType;
    private int range;                  // Range in feet (for vision, telepathy)
    private int duration;               // Duration in minutes (0 = permanent)
    private String specificValue;       // e.g., language name, item produced
    private boolean requiresAttunement;
    
    public UtilityEffect(String id, String name, UtilityType type) {
        super(id, name, createDescription(type, 0, null), UsageType.PASSIVE, -1);
        this.utilityType = type;
        this.range = 60; // Default range
        this.duration = 0;
        this.specificValue = null;
        this.requiresAttunement = false;
    }
    
    public UtilityEffect(String id, String name, UtilityType type, int range) {
        super(id, name, createDescription(type, range, null), UsageType.PASSIVE, -1);
        this.utilityType = type;
        this.range = Math.max(0, range);
        this.duration = 0;
        this.specificValue = null;
        this.requiresAttunement = false;
    }
    
    public UtilityEffect(String id, String name, UtilityType type, int range,
                         UsageType usageType, int maxCharges) {
        super(id, name, createDescription(type, range, null), usageType, maxCharges);
        this.utilityType = type;
        this.range = Math.max(0, range);
        this.duration = 0;
        this.specificValue = null;
        this.requiresAttunement = false;
    }
    
    private static String createDescription(UtilityType type, int range, String specific) {
        return switch (type) {
            case DARKVISION -> String.format("You have darkvision out to %d feet.", range > 0 ? range : 60);
            case TRUESIGHT -> String.format("You have truesight out to %d feet.", range > 0 ? range : 30);
            case BLINDSIGHT -> String.format("You have blindsight out to %d feet.", range > 0 ? range : 30);
            case TREMORSENSE -> String.format("You have tremorsense out to %d feet.", range > 0 ? range : 30);
            case COMPREHEND_LANGUAGES -> "You understand all spoken and written languages.";
            case SPEAK_LANGUAGE -> String.format("You can speak %s.", specific != null ? specific : "an additional language");
            case TELEPATHY -> String.format("You can communicate telepathically within %d feet.", range > 0 ? range : 30);
            case LIGHT -> "You can cause the item to shed bright light.";
            case DETECT_MAGIC -> "You can sense magical auras.";
            case DETECT_EVIL_GOOD -> "You can sense celestials, fiends, and undead.";
            case SEE_INVISIBILITY -> "You can see invisible creatures and objects.";
            case STORAGE -> "The item can hold far more than its size suggests.";
            case PRODUCE_ITEM -> String.format("The item produces %s.", specific != null ? specific : "something");
            case PARTY_COMMUNICATION -> "You can communicate silently with your party.";
            case DISGUISE -> "You can alter your appearance.";
            case BREATHING -> "You can breathe in environments you normally couldn't.";
            case SUSTENANCE -> "You don't need to eat or drink.";
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        return switch (utilityType) {
            case DARKVISION -> String.format("%s can see in the dark out to %d feet!", 
                    user.getName(), range);
            case TRUESIGHT -> String.format("%s sees through all illusions!", user.getName());
            case COMPREHEND_LANGUAGES -> String.format("%s understands all languages!", user.getName());
            case TELEPATHY -> String.format("%s establishes telepathic contact!", user.getName());
            case LIGHT -> String.format("%s's item begins to glow!", user.getName());
            case DETECT_MAGIC -> String.format("%s senses magical auras!", user.getName());
            case SEE_INVISIBILITY -> String.format("%s can see the invisible!", user.getName());
            case PARTY_COMMUNICATION -> String.format("%s whispers to the party!", user.getName());
            default -> String.format("%s activates %s!", user.getName(), getName());
        };
    }
    
    public int getVisionRange() {
        return switch (utilityType) {
            case DARKVISION, TRUESIGHT, BLINDSIGHT, TREMORSENSE -> range;
            default -> 0;
        };
    }

    public boolean grantsVision(UtilityType visionType) {
        return utilityType == visionType;
    }

    public UtilityType getUtilityType() {
        return utilityType;
    }
    
    public void setUtilityType(UtilityType utilityType) {
        this.utilityType = utilityType;
    }
    
    public int getRange() {
        return range;
    }
    
    public void setRange(int range) {
        this.range = Math.max(0, range);
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = Math.max(0, duration);
    }
    
    public String getSpecificValue() {
        return specificValue;
    }
    
    public void setSpecificValue(String specificValue) {
        this.specificValue = specificValue;
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
        sb.append(utilityType.getDisplayName());
        
        if (range > 0 && (utilityType == UtilityType.DARKVISION || 
                          utilityType == UtilityType.TRUESIGHT ||
                          utilityType == UtilityType.BLINDSIGHT ||
                          utilityType == UtilityType.TELEPATHY)) {
            sb.append(": ").append(range).append(" ft");
        }
        sb.append("\n");
        
        if (specificValue != null) {
            sb.append("Specific: ").append(specificValue).append("\n");
        }
        
        if (duration > 0) {
            sb.append("Duration: ").append(duration).append(" minutes\n");
        }
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }

    /**
     * Creates Goggles of Night (darkvision 60 ft).
     */
    public static UtilityEffect createGogglesOfNight() {
        return new UtilityEffect("goggles_night_effect", "Night Vision",
                UtilityType.DARKVISION, 60);
    }
    
    /**
     * Creates Helm of Comprehending Languages.
     */
    public static UtilityEffect createHelmOfComprehendingLanguages() {
        UtilityEffect effect = new UtilityEffect("helm_languages_effect", 
                "Comprehend Languages", UtilityType.COMPREHEND_LANGUAGES);
        effect.setDescription("You understand the literal meaning of any spoken or written language.");
        return effect;
    }
    
    /**
     * Creates Gem of Seeing (truesight 120 ft, 3 charges).
     */
    public static UtilityEffect createGemOfSeeing() {
        UtilityEffect effect = new UtilityEffect("gem_seeing_effect",
                "True Seeing", UtilityType.TRUESIGHT, 120,
                UsageType.CHARGES, 3);
        effect.setRequiresAttunement(true);
        effect.setDuration(10);
        effect.setRechargeAmount(1); // 1d3 at dawn
        effect.setDescription("Gain truesight for 10 minutes. 3 charges, regains 1d3 at dawn.");
        return effect;
    }
    
    /**
     * Creates Lantern of Revealing (see invisibility in 30 ft).
     */
    public static UtilityEffect createLanternOfRevealing() {
        UtilityEffect effect = new UtilityEffect("lantern_revealing_effect",
                "Revealing Light", UtilityType.SEE_INVISIBILITY, 30);
        effect.setDescription("Invisible creatures and objects are visible in the lantern's bright light.");
        return effect;
    }
    
    /**
     * Creates Whispering Stone from Muddlebrook.
     */
    public static UtilityEffect createWhisperingStone() {
        UtilityEffect effect = new UtilityEffect("whispering_stone_effect",
                "Whisper", UtilityType.PARTY_COMMUNICATION, 100,
                UsageType.DAILY, 1);
        effect.setDescription("Send a telepathic message to all party members within 100 feet.");
        return effect;
    }
    
    /**
     * Creates Flash Powder Orb from Muddlebrook.
     */
    public static UtilityEffect createFlashPowderOrb() {
        UtilityEffect effect = new UtilityEffect("flash_powder_effect",
                "Flash", UtilityType.LIGHT, 30,
                UsageType.CONSUMABLE, 1);
        effect.setDescription("Throw to create a blinding flash. DC 12 CON save or be blinded for 1 round.");
        return effect;
    }
    
    /**
     * Creates Bag of Holding.
     */
    public static UtilityEffect createBagOfHolding() {
        UtilityEffect effect = new UtilityEffect("bag_holding_effect",
                "Holding", UtilityType.STORAGE);
        effect.setSpecificValue("500 lbs / 64 cubic feet");
        effect.setDescription("This bag can hold up to 500 pounds or 64 cubic feet of material.");
        return effect;
    }
    
    /**
     * Creates Decanter of Endless Water.
     */
    public static UtilityEffect createDecanterOfEndlessWater() {
        UtilityEffect effect = new UtilityEffect("decanter_water_effect",
                "Endless Water", UtilityType.PRODUCE_ITEM);
        effect.setSpecificValue("water");
        effect.setDescription("Produces fresh or salt water on command (1-30 gallons).");
        return effect;
    }
    
    /**
     * Creates Ring of Mind Shielding (immunity to mind reading).
     */
    public static UtilityEffect createRingOfMindShielding() {
        UtilityEffect effect = new UtilityEffect("ring_mind_shield_effect",
                "Mind Shield", UtilityType.TELEPATHY);
        effect.setRange(0);
        effect.setRequiresAttunement(true);
        effect.setDescription("You are immune to magic that reads thoughts, determines lies, or reveals alignment.");
        return effect;
    }
}