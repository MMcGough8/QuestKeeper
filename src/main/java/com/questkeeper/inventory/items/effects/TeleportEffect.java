package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * Effect that teleports the user a certain distance.
 * 
 * Examples: Blinkstep Spark, Cape of the Mountebank
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class TeleportEffect extends AbstractItemEffect {
    
    private int distance;           // Teleport distance in feet
    private boolean requiresSight;  // Must see destination
    private boolean canTakePotions; // Can bring willing creatures (future)
    
    /**
     * Creates a teleport effect with default settings.
     */
    public TeleportEffect(String id, String name, int distance) {
        this(id, name, distance, UsageType.LONG_REST, 1);
    }
    
    /**
     * Creates a teleport effect with full customization.
     */
    public TeleportEffect(String id, String name, int distance, 
                          UsageType usageType, int maxCharges) {
        super(id, name, createDescription(distance), usageType, maxCharges);
        this.distance = Math.max(5, distance);
        this.requiresSight = true;
        this.canTakePotions = false;
    }
    
    private static String createDescription(int distance) {
        return String.format("Teleport up to %d feet to an unoccupied space you can see.", distance);
    }
    
    @Override
    protected String applyEffect(Character user) {
        // In a full implementation, this would interact with the game world
        // For now, we return a description of what happens
        return String.format("%s vanishes in a flash and reappears up to %d feet away!", 
                user.getName(), distance);
    }
    
    public int getDistance() {
        return distance;
    }
    
    public void setDistance(int distance) {
        this.distance = Math.max(5, distance);
        setDescription(createDescription(this.distance));
    }
    
    public boolean requiresSight() {
        return requiresSight;
    }
    
    public void setRequiresSight(boolean requiresSight) {
        this.requiresSight = requiresSight;
    }
    
    public boolean canTakeOthers() {
        return canTakePotions;
    }
    
    public void setCanTakeOthers(boolean canTakeOthers) {
        this.canTakePotions = canTakeOthers;
    }
    
    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\n");
        sb.append("Teleport: ").append(distance).append(" feet\n");
        if (requiresSight) {
            sb.append("Requires line of sight to destination\n");
        }
        sb.append("Usage: ").append(getChargeDisplay());
        return sb.toString();
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Creates the Blinkstep Spark from Muddlebrook.
     */
    public static TeleportEffect createBlinkstepSpark() {
        return new TeleportEffect("blinkstep_spark_effect", "Blinkstep", 10, 
                UsageType.LONG_REST, 1);
    }
    
    /**
     * Creates a standard Misty Step effect (30 ft teleport).
     */
    public static TeleportEffect createMistyStep() {
        return new TeleportEffect("misty_step_effect", "Misty Step", 30, 
                UsageType.LONG_REST, 1);
    }
}