package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that modifies movement capabilities.
 * 
 * Examples: Boots of Speed, Winged Boots, Boots of Elvenkind,
 * Ring of Water Walking, Cloak of the Manta Ray
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class MovementEffect extends AbstractItemEffect {
    
    /**
     * Types of movement modifications.
     */
    public enum MovementType {
        SPEED_BONUS("Speed Bonus"),
        SPEED_SET("Speed Set"),
        SPEED_DOUBLE("Double Speed"),
        FLYING("Flying"),
        SWIMMING("Swimming"),
        CLIMBING("Climbing"),
        BURROWING("Burrowing"),
        WATER_WALKING("Water Walking"),
        SPIDER_CLIMB("Spider Climb"),
        JUMP_BONUS("Jump Bonus"),
        IGNORE_DIFFICULT_TERRAIN("Ignore Difficult Terrain");
        
        private final String displayName;
        
        MovementType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private MovementType movementType;
    private int speedValue;
    private int duration;               // Duration in minutes (0 = permanent)
    private boolean requiresAttunement;
    private boolean hovering;           // For flying: can hover
    private String activationAction;    // "bonus action", "action", or "none"
    
    public MovementEffect(String id, String name, MovementType type, int speedValue) {
        super(id, name, createDescription(type, speedValue), UsageType.PASSIVE, -1);
        this.movementType = type;
        this.speedValue = Math.max(0, speedValue);
        this.duration = 0;
        this.requiresAttunement = false;
        this.hovering = false;
        this.activationAction = "none";
    }

    public MovementEffect(String id, String name, MovementType type, int speedValue,
                          UsageType usageType, int maxCharges, int durationMinutes) {
        super(id, name, createDescription(type, speedValue), usageType, maxCharges);
        this.movementType = type;
        this.speedValue = Math.max(0, speedValue);
        this.duration = durationMinutes;
        this.requiresAttunement = false;
        this.hovering = false;
        this.activationAction = "bonus action";
    }
    
    private static String createDescription(MovementType type, int value) {
        return switch (type) {
            case SPEED_BONUS -> String.format("Your walking speed increases by %d feet.", value);
            case SPEED_SET -> String.format("Your walking speed is %d feet.", value);
            case SPEED_DOUBLE -> "Your walking speed is doubled.";
            case FLYING -> String.format("You gain a flying speed of %d feet.", value);
            case SWIMMING -> String.format("You gain a swimming speed of %d feet.", value);
            case CLIMBING -> String.format("You gain a climbing speed of %d feet.", value);
            case BURROWING -> String.format("You gain a burrowing speed of %d feet.", value);
            case WATER_WALKING -> "You can walk on water as if it were solid ground.";
            case SPIDER_CLIMB -> "You can climb difficult surfaces, including ceilings.";
            case JUMP_BONUS -> String.format("Your jump distance increases by %d feet.", value);
            case IGNORE_DIFFICULT_TERRAIN -> "Moving through difficult terrain costs no extra movement.";
        };
    }
    
    @Override
    protected String applyEffect(Character user) {
        String result = switch (movementType) {
            case SPEED_BONUS -> String.format("%s moves faster! (+%d ft)", 
                    user.getName(), speedValue);
            case SPEED_DOUBLE -> String.format("%s's speed doubles!", user.getName());
            case FLYING -> String.format("%s takes to the air!", user.getName());
            case SWIMMING -> String.format("%s can swim with ease!", user.getName());
            case WATER_WALKING -> String.format("%s walks on water!", user.getName());
            case SPIDER_CLIMB -> String.format("%s clings to the walls!", user.getName());
            default -> String.format("%s's movement is enhanced!", user.getName());
        };
        
        if (duration > 0) {
            result += String.format(" (Duration: %d minutes)", duration);
        }
        
        return result;
    }
    
    public int calculateModifiedSpeed(int baseSpeed) {
        return switch (movementType) {
            case SPEED_BONUS -> baseSpeed + speedValue;
            case SPEED_SET -> speedValue;
            case SPEED_DOUBLE -> baseSpeed * 2;
            default -> baseSpeed;
        };
    }

    public int getSpecialMovementSpeed() {
        return switch (movementType) {
            case FLYING, SWIMMING, CLIMBING, BURROWING -> speedValue;
            default -> 0;
        };
    }

    public boolean grantsMovementMode(MovementType type) {
        return this.movementType == type;
    }

    public MovementType getMovementType() {
        return movementType;
    }
    
    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }
    
    public int getSpeedValue() {
        return speedValue;
    }
    
    public void setSpeedValue(int speedValue) {
        this.speedValue = Math.max(0, speedValue);
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = Math.max(0, duration);
    }
    
    public boolean requiresAttunement() {
        return requiresAttunement;
    }
    
    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }
    
    public boolean canHover() {
        return hovering;
    }
    
    public void setHovering(boolean hovering) {
        this.hovering = hovering;
    }
    
    public String getActivationAction() {
        return activationAction;
    }
    
    public void setActivationAction(String activationAction) {
        this.activationAction = activationAction;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append(movementType.getDisplayName());
        
        if (speedValue > 0 && movementType != MovementType.SPEED_DOUBLE) {
            sb.append(": ").append(speedValue).append(" ft");
        }
        sb.append("\n");
        
        if (hovering && movementType == MovementType.FLYING) {
            sb.append("Can hover\n");
        }
        
        if (duration > 0) {
            sb.append("Duration: ").append(duration).append(" minutes\n");
        }
        
        if (!"none".equals(activationAction)) {
            sb.append("Activation: ").append(activationAction).append("\n");
        }
        
        if (requiresAttunement) {
            sb.append("Requires attunement\n");
        }
        
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }
 
    /**
     * Creates Boots of Speed (double speed for 10 min).
     */
    public static MovementEffect createBootsOfSpeed() {
        MovementEffect effect = new MovementEffect("boots_speed_effect",
                "Speed", MovementType.SPEED_DOUBLE, 0,
                UsageType.DAILY, 1, 10);
        effect.setRequiresAttunement(true);
        effect.setActivationAction("bonus action");
        effect.setDescription("Double your walking speed for 10 minutes.");
        return effect;
    }
    
    /**
     * Creates Winged Boots (flying 4 hours/day).
     */
    public static MovementEffect createWingedBoots() {
        MovementEffect effect = new MovementEffect("winged_boots_effect",
                "Flying", MovementType.FLYING, 30,
                UsageType.DAILY, 4, 60); // 4 charges of 1 hour each
        effect.setRequiresAttunement(true);
        effect.setHovering(false);
        effect.setDescription("Gain a flying speed equal to your walking speed for up to 4 hours.");
        return effect;
    }
    
    /**
     * Creates Broom of Flying.
     */
    public static MovementEffect createBroomOfFlying() {
        MovementEffect effect = new MovementEffect("broom_flying_effect",
                "Flying Broom", MovementType.FLYING, 50);
        effect.setHovering(true);
        effect.setDescription("The broom flies at 50 feet and can hover. It can carry up to 400 pounds.");
        return effect;
    }
    
    /**
     * Creates Ring of Water Walking.
     */
    public static MovementEffect createRingOfWaterWalking() {
        return new MovementEffect("ring_water_walking_effect",
                "Water Walking", MovementType.WATER_WALKING, 0);
    }
    
    /**
     * Creates Slippers of Spider Climbing.
     */
    public static MovementEffect createSlippersOfSpiderClimbing() {
        MovementEffect effect = new MovementEffect("slippers_spider_effect",
                "Spider Climb", MovementType.SPIDER_CLIMB, 0);
        effect.setRequiresAttunement(true);
        effect.setDescription("You can move on vertical surfaces and ceilings while leaving your hands free.");
        return effect;
    }
    
    /**
     * Creates Boots of Striding and Springing.
     */
    public static MovementEffect createBootsOfStridingAndSpringing() {
        MovementEffect effect = new MovementEffect("boots_striding_effect",
                "Striding", MovementType.SPEED_SET, 30);
        effect.setRequiresAttunement(true);
        effect.setDescription("Your walking speed is 30 feet (unless higher). Your jump distance is tripled.");
        return effect;
    }
    
    /**
     * Creates Hopper's Jump Band from Muddlebrook.
     */
    public static MovementEffect createHoppersJumpBand() {
        MovementEffect effect = new MovementEffect("hoppers_jump_effect",
                "Hopper's Jump", MovementType.JUMP_BONUS, 10,
                UsageType.LONG_REST, 1, 0);
        effect.setDescription("Your jump distance is increased by 10 feet for your next jump.");
        return effect;
    }
    
    /**
     * Creates Cloak of the Manta Ray (swimming + water breathing).
     */
    public static MovementEffect createCloakOfMantaRay() {
        MovementEffect effect = new MovementEffect("cloak_manta_effect",
                "Manta Ray", MovementType.SWIMMING, 60);
        effect.setDescription("You gain a swimming speed of 60 feet and can breathe underwater.");
        return effect;
    }
}