package com.questkeeper.magic;

/**
 * Tracks spell slots for a character.
 *
 * Supports different caster types:
 * - Full casters (Wizard, Cleric, etc.): Standard slot progression
 * - Half casters (Paladin, Ranger): Half slot progression, starts at level 2
 * - Warlock: Pact Magic (different progression, short rest recovery)
 *
 * @author Marc McGough
 * @version 1.0
 */
public class SpellSlots {

    /**
     * Type of spellcasting which determines slot progression.
     */
    public enum CasterType {
        FULL,       // Wizard, Cleric, Druid, Bard, Sorcerer
        HALF,       // Paladin, Ranger
        THIRD,      // Eldritch Knight, Arcane Trickster (not implemented yet)
        WARLOCK     // Pact Magic
    }

    private final CasterType casterType;
    private int casterLevel;
    private int[] currentSlots;
    private int[] maxSlots;

    /**
     * Creates spell slots for a given caster type and level.
     */
    public SpellSlots(CasterType casterType, int casterLevel) {
        this.casterType = casterType;
        this.casterLevel = casterLevel;
        this.maxSlots = calculateMaxSlots(casterType, casterLevel);
        this.currentSlots = maxSlots.clone();
    }

    /**
     * Calculates max spell slots based on caster type and level.
     */
    private static int[] calculateMaxSlots(CasterType type, int level) {
        return switch (type) {
            case FULL -> getFullCasterSlots(level);
            case HALF -> getHalfCasterSlots(level);
            case WARLOCK -> getWarlockSlots(level);
            case THIRD -> getThirdCasterSlots(level);
        };
    }

    /**
     * Full caster spell slot progression (Wizard, Cleric, etc.)
     */
    private static int[] getFullCasterSlots(int level) {
        return switch (level) {
            case 1 -> new int[]{2, 0, 0, 0, 0, 0, 0, 0, 0};
            case 2 -> new int[]{3, 0, 0, 0, 0, 0, 0, 0, 0};
            case 3 -> new int[]{4, 2, 0, 0, 0, 0, 0, 0, 0};
            case 4 -> new int[]{4, 3, 0, 0, 0, 0, 0, 0, 0};
            case 5 -> new int[]{4, 3, 2, 0, 0, 0, 0, 0, 0};
            case 6 -> new int[]{4, 3, 3, 0, 0, 0, 0, 0, 0};
            case 7 -> new int[]{4, 3, 3, 1, 0, 0, 0, 0, 0};
            case 8 -> new int[]{4, 3, 3, 2, 0, 0, 0, 0, 0};
            case 9 -> new int[]{4, 3, 3, 3, 1, 0, 0, 0, 0};
            case 10 -> new int[]{4, 3, 3, 3, 2, 0, 0, 0, 0};
            case 11, 12 -> new int[]{4, 3, 3, 3, 2, 1, 0, 0, 0};
            case 13, 14 -> new int[]{4, 3, 3, 3, 2, 1, 1, 0, 0};
            case 15, 16 -> new int[]{4, 3, 3, 3, 2, 1, 1, 1, 0};
            case 17 -> new int[]{4, 3, 3, 3, 2, 1, 1, 1, 1};
            case 18 -> new int[]{4, 3, 3, 3, 3, 1, 1, 1, 1};
            case 19 -> new int[]{4, 3, 3, 3, 3, 2, 1, 1, 1};
            case 20 -> new int[]{4, 3, 3, 3, 3, 2, 2, 1, 1};
            default -> new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        };
    }

    /**
     * Half caster spell slot progression (Paladin, Ranger).
     * Starts at level 2, uses half the full caster progression.
     */
    private static int[] getHalfCasterSlots(int level) {
        return switch (level) {
            case 2 -> new int[]{2, 0, 0, 0, 0, 0, 0, 0, 0};
            case 3, 4 -> new int[]{3, 0, 0, 0, 0, 0, 0, 0, 0};
            case 5, 6 -> new int[]{4, 2, 0, 0, 0, 0, 0, 0, 0};
            case 7, 8 -> new int[]{4, 3, 0, 0, 0, 0, 0, 0, 0};
            case 9, 10 -> new int[]{4, 3, 2, 0, 0, 0, 0, 0, 0};
            case 11, 12 -> new int[]{4, 3, 3, 0, 0, 0, 0, 0, 0};
            case 13, 14 -> new int[]{4, 3, 3, 1, 0, 0, 0, 0, 0};
            case 15, 16 -> new int[]{4, 3, 3, 2, 0, 0, 0, 0, 0};
            case 17, 18 -> new int[]{4, 3, 3, 3, 1, 0, 0, 0, 0};
            case 19, 20 -> new int[]{4, 3, 3, 3, 2, 0, 0, 0, 0};
            default -> new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};  // Level 1 or invalid
        };
    }

    /**
     * Warlock Pact Magic progression.
     * Fewer slots, but all at highest available level, recovered on short rest.
     */
    private static int[] getWarlockSlots(int level) {
        // Warlock slots are all the same level, stored at that level's index
        // Level 1: 1 slot at 1st level
        // Level 2: 2 slots at 1st level
        // Level 3-4: 2 slots at 2nd level
        // Level 5-6: 2 slots at 3rd level
        // Level 7-8: 2 slots at 4th level
        // Level 9-10: 2 slots at 5th level
        // Level 11+: 3 slots at 5th level (+ Mystic Arcanum)
        int slotCount = level >= 11 ? 3 : (level >= 2 ? 2 : 1);
        if (level >= 17) slotCount = 4;

        int slotLevel;
        if (level >= 9) slotLevel = 5;
        else if (level >= 7) slotLevel = 4;
        else if (level >= 5) slotLevel = 3;
        else if (level >= 3) slotLevel = 2;
        else slotLevel = 1;

        int[] slots = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        slots[slotLevel - 1] = slotCount;
        return slots;
    }

    /**
     * Third caster spell slot progression (Eldritch Knight, Arcane Trickster).
     */
    private static int[] getThirdCasterSlots(int level) {
        // Third casters get slots at 1/3 rate, starting at level 3
        return switch (level) {
            case 3 -> new int[]{2, 0, 0, 0, 0, 0, 0, 0, 0};
            case 4, 5, 6 -> new int[]{3, 0, 0, 0, 0, 0, 0, 0, 0};
            case 7, 8, 9 -> new int[]{4, 2, 0, 0, 0, 0, 0, 0, 0};
            case 10, 11, 12 -> new int[]{4, 3, 0, 0, 0, 0, 0, 0, 0};
            case 13, 14, 15 -> new int[]{4, 3, 2, 0, 0, 0, 0, 0, 0};
            case 16, 17, 18 -> new int[]{4, 3, 3, 0, 0, 0, 0, 0, 0};
            case 19, 20 -> new int[]{4, 3, 3, 1, 0, 0, 0, 0, 0};
            default -> new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        };
    }

    /**
     * Checks if a slot of the given level is available.
     * @param slotLevel 1-9
     */
    public boolean hasSlot(int slotLevel) {
        if (slotLevel < 1 || slotLevel > 9) return false;
        return currentSlots[slotLevel - 1] > 0;
    }

    /**
     * Gets the number of slots remaining at a given level.
     */
    public int getSlotsRemaining(int slotLevel) {
        if (slotLevel < 1 || slotLevel > 9) return 0;
        return currentSlots[slotLevel - 1];
    }

    /**
     * Gets the maximum slots at a given level.
     */
    public int getMaxSlots(int slotLevel) {
        if (slotLevel < 1 || slotLevel > 9) return 0;
        return maxSlots[slotLevel - 1];
    }

    /**
     * Expends a spell slot of the given level.
     * @return true if successful, false if no slot available
     */
    public boolean expendSlot(int slotLevel) {
        if (!hasSlot(slotLevel)) return false;
        currentSlots[slotLevel - 1]--;
        return true;
    }

    /**
     * Gets the highest level slot available.
     * @return slot level (1-9) or 0 if none available
     */
    public int getHighestAvailableSlot() {
        for (int i = 8; i >= 0; i--) {
            if (currentSlots[i] > 0) return i + 1;
        }
        return 0;
    }

    /**
     * Gets the lowest level slot available.
     * @return slot level (1-9) or 0 if none available
     */
    public int getLowestAvailableSlot() {
        for (int i = 0; i < 9; i++) {
            if (currentSlots[i] > 0) return i + 1;
        }
        return 0;
    }

    /**
     * Gets the highest spell level this caster can cast.
     */
    public int getMaxSpellLevel() {
        for (int i = 8; i >= 0; i--) {
            if (maxSlots[i] > 0) return i + 1;
        }
        return 0;
    }

    /**
     * Gets all current slots as an array (index 0 = 1st level, etc.)
     */
    public int[] getCurrentSlots() {
        return currentSlots.clone();
    }

    /**
     * Gets all max slots as an array.
     */
    public int[] getMaxSlotsArray() {
        return maxSlots.clone();
    }

    /**
     * Restores all spell slots to maximum.
     * Called on long rest (or short rest for Warlock).
     */
    public void restoreAll() {
        currentSlots = maxSlots.clone();
    }

    /**
     * Restores slots for short rest (only affects Warlock Pact Magic).
     */
    public void restoreOnShortRest() {
        if (casterType == CasterType.WARLOCK) {
            restoreAll();
        }
    }

    /**
     * Restores slots for long rest (affects all caster types).
     */
    public void restoreOnLongRest() {
        restoreAll();
    }

    /**
     * Updates caster level and recalculates max slots.
     */
    public void setCasterLevel(int level) {
        this.casterLevel = level;
        this.maxSlots = calculateMaxSlots(casterType, level);
        // Cap current slots at new max
        for (int i = 0; i < 9; i++) {
            currentSlots[i] = Math.min(currentSlots[i], maxSlots[i]);
        }
    }

    public int getCasterLevel() {
        return casterLevel;
    }

    public CasterType getCasterType() {
        return casterType;
    }

    /**
     * Gets a formatted status string showing all slots.
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder("Spell Slots: ");
        boolean first = true;
        for (int i = 0; i < 9; i++) {
            if (maxSlots[i] > 0) {
                if (!first) sb.append(", ");
                sb.append(i + 1).append("st: ").append(currentSlots[i]).append("/").append(maxSlots[i]);
                first = false;
            }
        }
        if (first) {
            return "Spell Slots: None";
        }
        return sb.toString();
    }

    /**
     * Returns true if any spell slots are available.
     */
    public boolean hasAnySlots() {
        for (int i = 0; i < 9; i++) {
            if (currentSlots[i] > 0) return true;
        }
        return false;
    }

    /**
     * Returns true if this caster has any spell slot capacity at all.
     */
    public boolean canCastSpells() {
        for (int i = 0; i < 9; i++) {
            if (maxSlots[i] > 0) return true;
        }
        return false;
    }
}
