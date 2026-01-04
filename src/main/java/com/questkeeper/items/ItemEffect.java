package com.questkeeper.items;

import com.questkeeper.character.Character;
import com.questkeeper.items.effects.UsageType;

/**
 * Interface for all item effects in the game.
 * 
 * Item effects define what happens when a magic item is used or equipped.
 * Implementations handle specific effect types like teleportation, bonus damage,
 * stat increases, etc.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public interface ItemEffect {
    
    String getId();
    
    String getName();

    String getDescription();

    UsageType getUsageType();
    
    boolean isUsable();
    
    default boolean isPassive() {
        return getUsageType() == UsageType.PASSIVE;
    }
  
    String use(Character user);
 
    default String use(Character user, Character target) {
        return use(user);
    }
 
    void resetOnLongRest();

    void resetDaily();
 
    int getCurrentCharges();
    
    int getMaxCharges();
    
    default String getChargeDisplay() {
        if (getUsageType() == UsageType.UNLIMITED || getUsageType() == UsageType.PASSIVE) {
            return getUsageType().getDisplayName();
        }
        return String.format("%d/%d %s", getCurrentCharges(), getMaxCharges(), 
                getMaxCharges() == 1 ? "use" : "uses");
    }

    String getDetailedInfo();
}