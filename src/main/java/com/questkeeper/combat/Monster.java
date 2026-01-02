package com.questkeeper.combat;

import com.questkeeper.core.Dice;

/**
 * Represents an enemy creature in combat.
 * 
 * Monsters have simplified stats compared to Characters, following
 * D&D 5e monster stat block conventions. They implement Combatant
 * for use in the combat system.
 * 
 * @author Marc McGough
 * @version 1.0
 */

public class Monster implements Combatant {
    public enum Size {
        TINY("Tiny", 2.5),
        SMALL("Small", 5),
        MEDIUM("Medium", 5),
        LARGE("Large", 10),
        HUGE("Huge", 15),
        GARGANTUAN("Gargantuan", 20);

        private final String displayName;
        private final double space; // feet

        Size(String displayName, double space) {
            this.displayName = displayName;
            this.space = space;
        }

        public String getDisplayName() {
            return displayName;
        }
        public double getSpace() {
            return space;
        }
    }

    
    
}
