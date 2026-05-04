package com.questkeeper.inventory.items;

import com.questkeeper.character.Character;
import com.questkeeper.inventory.items.effects.UsageType;

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

    /**
     * Returns an effect that can be safely shared with a duplicated item.
     * Default returns `this` — safe for stateless / passive effects (most
     * StatBonus / Resistance / SkillBonus / AbilitySet / Description /
     * DamageReduction / ExtraDamage / Movement variants).
     *
     * <p><strong>NOT SAFE</strong> for effects with mutable runtime state
     * such as {@link UsageType#CHARGES}, {@link UsageType#DAILY},
     * {@link UsageType#LONG_REST}, {@link UsageType#CONSUMABLE} — sharing
     * the instance across duplicated MagicItems will drain charges from
     * both. If a campaign starts placing duplicate magic items with
     * charges, the affected effect class must override this method to
     * return an independent instance with copied state.
     */
    default ItemEffect copy() {
        return this;
    }

    default String getChargeDisplay() {
        if (getUsageType() == UsageType.UNLIMITED || getUsageType() == UsageType.PASSIVE) {
            return getUsageType().getDisplayName();
        }
        return String.format("%d/%d %s", getCurrentCharges(), getMaxCharges(), 
                getMaxCharges() == 1 ? "use" : "uses");
    }

    String getDetailedInfo();
}