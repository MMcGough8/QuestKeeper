package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for Paladin class features.
 *
 * Paladin features by level:
 * - Level 1: Divine Sense, Lay on Hands
 * - Level 2: Fighting Style, Spellcasting (spell slots), Divine Smite
 * - Level 3: Divine Health, Sacred Oath (Devotion: Channel Divinity)
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class PaladinFeatures {

    // Feature IDs
    public static final String DIVINE_SENSE_ID = "divine_sense";
    public static final String LAY_ON_HANDS_ID = "lay_on_hands";
    public static final String PALADIN_FIGHTING_STYLE_ID = "paladin_fighting_style";
    public static final String DIVINE_SMITE_ID = "divine_smite";
    public static final String DIVINE_HEALTH_ID = "divine_health";
    public static final String CHANNEL_DIVINITY_ID = "channel_divinity";
    public static final String SACRED_WEAPON_ID = "sacred_weapon";
    public static final String TURN_THE_UNHOLY_ID = "turn_the_unholy";

    private PaladinFeatures() {
        // Utility class
    }

    /**
     * Creates all Paladin features appropriate for the given level.
     *
     * @param level the character's Paladin level
     * @param fightingStyle the chosen fighting style (null if not yet chosen)
     * @return list of features available at this level
     */
    public static List<ClassFeature> createFeaturesForLevel(int level, FightingStyle fightingStyle) {
        List<ClassFeature> features = new ArrayList<>();

        // Level 1: Divine Sense, Lay on Hands
        if (level >= 1) {
            features.add(createDivineSense());
            features.add(createLayOnHands(level));
        }

        // Level 2: Fighting Style, Divine Smite (requires spell slots)
        if (level >= 2) {
            if (fightingStyle != null) {
                features.add(createFightingStyleFeature(fightingStyle));
            }
            features.add(createDivineSmite(level));
        }

        // Level 3: Divine Health, Sacred Oath (Channel Divinity)
        if (level >= 3) {
            features.add(createDivineHealth());
            features.add(createChannelDivinity());
            features.add(createSacredWeapon());
            features.add(createTurnTheUnholy());
        }

        return features;
    }

    /**
     * Gets Paladin spell slots by level.
     * Returns array where index 0 = 1st level slots, index 1 = 2nd level, etc.
     */
    public static int[] getSpellSlots(int paladinLevel) {
        return switch (paladinLevel) {
            case 2 -> new int[]{2, 0, 0, 0, 0};
            case 3, 4 -> new int[]{3, 0, 0, 0, 0};
            case 5, 6 -> new int[]{4, 2, 0, 0, 0};
            case 7, 8 -> new int[]{4, 3, 0, 0, 0};
            case 9, 10 -> new int[]{4, 3, 2, 0, 0};
            case 11, 12 -> new int[]{4, 3, 3, 0, 0};
            case 13, 14 -> new int[]{4, 3, 3, 1, 0};
            case 15, 16 -> new int[]{4, 3, 3, 2, 0};
            case 17, 18 -> new int[]{4, 3, 3, 3, 1};
            case 19, 20 -> new int[]{4, 3, 3, 3, 2};
            default -> new int[]{0, 0, 0, 0, 0};  // Level 1 or invalid
        };
    }

    /**
     * Gets the Lay on Hands pool size (Paladin level × 5).
     */
    public static int getLayOnHandsPool(int paladinLevel) {
        return paladinLevel * 5;
    }

    /**
     * Gets the number of Divine Sense uses (1 + CHA modifier).
     */
    public static int getDivineSenseUses(Character paladin) {
        return 1 + Math.max(0, paladin.getAbilityModifier(Ability.CHARISMA));
    }

    // ==========================================
    // Factory Methods
    // ==========================================

    public static DivineSense createDivineSense() {
        return new DivineSense();
    }

    public static LayOnHands createLayOnHands(int paladinLevel) {
        return new LayOnHands(paladinLevel);
    }

    public static FightingStyleFeature createFightingStyleFeature(FightingStyle style) {
        return new FightingStyleFeature(style, PALADIN_FIGHTING_STYLE_ID);
    }

    public static DivineSmite createDivineSmite(int paladinLevel) {
        return new DivineSmite(paladinLevel);
    }

    public static DivineHealth createDivineHealth() {
        return new DivineHealth();
    }

    public static ChannelDivinity createChannelDivinity() {
        return new ChannelDivinity();
    }

    public static SacredWeapon createSacredWeapon() {
        return new SacredWeapon();
    }

    public static TurnTheUnholy createTurnTheUnholy() {
        return new TurnTheUnholy();
    }

    // ==========================================
    // Concrete Feature Classes
    // ==========================================

    /**
     * Divine Sense - Detect celestials, fiends, and undead.
     * Uses = 1 + CHA modifier per long rest.
     */
    public static class DivineSense extends ActivatedFeature {

        public DivineSense() {
            super(
                DIVINE_SENSE_ID,
                "Divine Sense",
                "As an action, you can open your awareness to detect such forces. Until the end " +
                    "of your next turn, you know the location of any celestial, fiend, or undead " +
                    "within 60 feet of you that is not behind total cover. You know the type of any " +
                    "being whose presence you sense, but not its identity.\n" +
                    "You can use this feature 1 + your Charisma modifier times per long rest.",
                1,  // Level required
                1,  // Default max uses (updated when used based on CHA)
                ResetType.LONG_REST,
                true,   // Available in combat
                true    // Available outside combat
            );
        }

        @Override
        protected String activate(Character user) {
            return String.format(
                "%s opens their awareness to the divine...\n" +
                "You sense the presence of celestials, fiends, and undead within 60 feet.",
                user.getName());
        }

        /**
         * Updates max uses based on character's CHA modifier.
         */
        public void updateMaxUses(Character paladin) {
            int uses = getDivineSenseUses(paladin);
            setMaxUses(uses);
        }
    }

    /**
     * Lay on Hands - Pool of healing power.
     * Pool = Paladin level × 5. Can heal HP or cure disease/poison (5 points each).
     */
    public static class LayOnHands extends ActivatedFeature {

        private int paladinLevel;
        private int poolRemaining;

        public LayOnHands(int paladinLevel) {
            super(
                LAY_ON_HANDS_ID,
                "Lay on Hands",
                "You have a pool of healing power that replenishes on a long rest. With that pool, " +
                    "you can restore a total number of hit points equal to your paladin level × 5.\n" +
                    "As an action, you can touch a creature and draw power from the pool to restore HP.\n" +
                    "You can also expend 5 HP from the pool to cure one disease or neutralize one poison.",
                1,  // Level required
                1,  // Max uses (not really used - we track pool instead)
                ResetType.LONG_REST,
                true,   // Available in combat
                true    // Available outside combat
            );
            this.paladinLevel = paladinLevel;
            this.poolRemaining = getLayOnHandsPool(paladinLevel);
        }

        @Override
        protected String activate(Character user) {
            // Base activation - actual healing amount determined by heal() method
            return String.format("Lay on Hands pool: %d/%d HP remaining",
                poolRemaining, getMaxPool());
        }

        @Override
        public boolean canUse() {
            return poolRemaining > 0;
        }

        /**
         * Heals a target for the specified amount.
         * @param target the character to heal
         * @param amount HP to restore (capped by pool remaining)
         * @return actual amount healed
         */
        public int heal(Character target, int amount) {
            int actualHeal = Math.min(amount, poolRemaining);
            if (actualHeal <= 0) {
                return 0;
            }

            poolRemaining -= actualHeal;
            target.heal(actualHeal);
            return actualHeal;
        }

        /**
         * Cures a disease or poison (costs 5 HP from pool).
         * @return true if successful, false if not enough pool
         */
        public boolean cureAffliction() {
            if (poolRemaining < 5) {
                return false;
            }
            poolRemaining -= 5;
            return true;
        }

        /**
         * Gets remaining pool HP.
         */
        public int getPoolRemaining() {
            return poolRemaining;
        }

        /**
         * Gets maximum pool HP.
         */
        public int getMaxPool() {
            return getLayOnHandsPool(paladinLevel);
        }

        /**
         * Updates Paladin level and pool size.
         */
        public void setPaladinLevel(int level) {
            int oldMax = getMaxPool();
            this.paladinLevel = level;
            int newMax = getMaxPool();
            // Add the difference to current pool
            poolRemaining += (newMax - oldMax);
        }

        @Override
        public void resetOnLongRest() {
            super.resetOnLongRest();
            poolRemaining = getMaxPool();
        }

        @Override
        public String getUsageStatus() {
            return String.format("Lay on Hands: %d/%d HP (long rest)",
                poolRemaining, getMaxPool());
        }

        public int getPaladinLevel() {
            return paladinLevel;
        }
    }

    /**
     * Fighting Style Feature for Paladin.
     * Paladins can choose: Defense, Dueling, Great Weapon Fighting, Protection
     */
    public static class FightingStyleFeature extends PassiveFeature {

        private final FightingStyle style;

        public FightingStyleFeature(FightingStyle style, String id) {
            super(
                id,
                "Fighting Style: " + style.getDisplayName(),
                style.getDescription(),
                2  // Level required for Paladin
            );
            this.style = style;
        }

        public FightingStyle getStyle() {
            return style;
        }
    }

    /**
     * Divine Smite - Expend spell slots for extra radiant damage.
     * 2d8 for 1st level slot, +1d8 per level above 1st (max 5d8).
     * +1d8 extra vs undead or fiends.
     */
    public static class DivineSmite extends PassiveFeature {

        private int paladinLevel;
        private int[] currentSlots;  // Current available spell slots

        public DivineSmite(int paladinLevel) {
            super(
                DIVINE_SMITE_ID,
                "Divine Smite",
                "When you hit a creature with a melee weapon attack, you can expend one spell slot " +
                    "to deal radiant damage to the target, in addition to the weapon's damage. " +
                    "The extra damage is 2d8 for a 1st-level slot, plus 1d8 for each spell level " +
                    "higher than 1st, to a maximum of 5d8. The damage increases by 1d8 if the target " +
                    "is an undead or a fiend.",
                2  // Level required
            );
            this.paladinLevel = paladinLevel;
            this.currentSlots = getSpellSlots(paladinLevel).clone();
        }

        /**
         * Gets the smite damage dice for a given slot level.
         * @param slotLevel 1-5
         * @param vsUndeadOrFiend true for +1d8 bonus
         * @return number of d8s to roll
         */
        public int getSmiteDice(int slotLevel, boolean vsUndeadOrFiend) {
            // Base: 2d8 for 1st level, +1d8 per level above, max 5d8
            int dice = Math.min(1 + slotLevel, 5);
            if (vsUndeadOrFiend) {
                dice++;
            }
            return dice;
        }

        /**
         * Gets smite damage notation (e.g., "3d8").
         */
        public String getSmiteDamageNotation(int slotLevel, boolean vsUndeadOrFiend) {
            return getSmiteDice(slotLevel, vsUndeadOrFiend) + "d8";
        }

        /**
         * Checks if a spell slot of the given level is available.
         */
        public boolean hasSlot(int slotLevel) {
            if (slotLevel < 1 || slotLevel > 5) return false;
            return currentSlots[slotLevel - 1] > 0;
        }

        /**
         * Gets the highest available spell slot level.
         * @return slot level (1-5) or 0 if no slots
         */
        public int getHighestAvailableSlot() {
            for (int i = 4; i >= 0; i--) {
                if (currentSlots[i] > 0) {
                    return i + 1;
                }
            }
            return 0;
        }

        /**
         * Gets the lowest available spell slot level.
         * @return slot level (1-5) or 0 if no slots
         */
        public int getLowestAvailableSlot() {
            for (int i = 0; i < 5; i++) {
                if (currentSlots[i] > 0) {
                    return i + 1;
                }
            }
            return 0;
        }

        /**
         * Expends a spell slot for Divine Smite.
         * @param slotLevel the slot level to expend (1-5)
         * @return true if successful, false if no slot available
         */
        public boolean expendSlot(int slotLevel) {
            if (!hasSlot(slotLevel)) {
                return false;
            }
            currentSlots[slotLevel - 1]--;
            return true;
        }

        /**
         * Gets current spell slots.
         * @return array of current slots (index 0 = 1st level, etc.)
         */
        public int[] getCurrentSlots() {
            return currentSlots.clone();
        }

        /**
         * Gets max spell slots for current level.
         */
        public int[] getMaxSlots() {
            return getSpellSlots(paladinLevel);
        }

        /**
         * Gets a formatted string of current spell slots.
         */
        public String getSlotsStatus() {
            int[] max = getMaxSlots();
            StringBuilder sb = new StringBuilder("Spell Slots: ");
            boolean first = true;
            for (int i = 0; i < 5; i++) {
                if (max[i] > 0) {
                    if (!first) sb.append(", ");
                    sb.append("L").append(i + 1).append(": ").append(currentSlots[i]).append("/").append(max[i]);
                    first = false;
                }
            }
            return sb.toString();
        }

        /**
         * Restores all spell slots (on long rest).
         */
        public void restoreAllSlots() {
            currentSlots = getSpellSlots(paladinLevel).clone();
        }

        /**
         * Updates Paladin level and spell slots.
         */
        public void setPaladinLevel(int level) {
            this.paladinLevel = level;
            // Keep track of used slots vs new max
            int[] newMax = getSpellSlots(level);
            for (int i = 0; i < 5; i++) {
                // Add any new slots from leveling
                if (newMax[i] > getSpellSlots(paladinLevel)[i]) {
                    currentSlots[i] += (newMax[i] - getSpellSlots(paladinLevel)[i]);
                }
                // Cap at new max
                currentSlots[i] = Math.min(currentSlots[i], newMax[i]);
            }
        }

        public int getPaladinLevel() {
            return paladinLevel;
        }
    }

    /**
     * Divine Health - Immunity to disease.
     */
    public static class DivineHealth extends PassiveFeature {

        public DivineHealth() {
            super(
                DIVINE_HEALTH_ID,
                "Divine Health",
                "The divine magic flowing through you makes you immune to disease.",
                3  // Level required
            );
        }
    }

    /**
     * Channel Divinity - Sacred Oath power, 1 use per short rest.
     */
    public static class ChannelDivinity extends ActivatedFeature {

        public ChannelDivinity() {
            super(
                CHANNEL_DIVINITY_ID,
                "Channel Divinity",
                "You gain the ability to channel divine energy directly from your deity, using that " +
                    "energy to fuel magical effects. You start with two such effects: Sacred Weapon " +
                    "and Turn the Unholy. You can use your Channel Divinity once per short or long rest.",
                3,  // Level required
                1,  // Max uses
                ResetType.SHORT_REST,
                true,   // Available in combat
                true    // Available outside combat
            );
        }

        @Override
        protected String activate(Character user) {
            return "Channel Divinity is ready. Choose: Sacred Weapon or Turn the Unholy.";
        }
    }

    /**
     * Sacred Weapon (Channel Divinity) - Add CHA to attack rolls for 1 minute.
     */
    public static class SacredWeapon extends PassiveFeature {

        private boolean active = false;
        private int roundsRemaining = 0;

        public SacredWeapon() {
            super(
                SACRED_WEAPON_ID,
                "Sacred Weapon",
                "As an action, you can imbue one weapon you are holding with positive energy. " +
                    "For 1 minute, you add your Charisma modifier to attack rolls made with that weapon. " +
                    "The weapon also emits bright light in a 20-foot radius. " +
                    "If the weapon is not already magical, it becomes magical for the duration.",
                3  // Level required
            );
        }

        /**
         * Activates Sacred Weapon.
         * @param chaMod the Charisma modifier to add to attacks
         * @return activation message
         */
        public String activate(int chaMod) {
            active = true;
            roundsRemaining = 10;  // 1 minute = 10 rounds
            return String.format(
                "Your weapon glows with divine radiance!\n" +
                "+%d to attack rolls for 10 rounds.\n" +
                "The weapon emits bright light in a 20-foot radius.",
                Math.max(0, chaMod));
        }

        public boolean isActive() {
            return active;
        }

        public void deactivate() {
            active = false;
            roundsRemaining = 0;
        }

        public boolean processTurnEnd() {
            if (!active) return false;
            roundsRemaining--;
            if (roundsRemaining <= 0) {
                deactivate();
                return false;
            }
            return true;
        }

        public int getRoundsRemaining() {
            return roundsRemaining;
        }
    }

    /**
     * Turn the Unholy (Channel Divinity) - Frighten fiends and undead.
     */
    public static class TurnTheUnholy extends PassiveFeature {

        public TurnTheUnholy() {
            super(
                TURN_THE_UNHOLY_ID,
                "Turn the Unholy",
                "As an action, you present your holy symbol and speak a prayer censuring fiends and undead. " +
                    "Each fiend or undead that can see or hear you within 30 feet must make a Wisdom saving throw. " +
                    "If the creature fails, it is turned for 1 minute or until it takes damage.\n" +
                    "A turned creature must spend its turns trying to move as far away from you as it can.",
                3  // Level required
            );
        }

        /**
         * Gets the Turn save DC (8 + proficiency + CHA).
         */
        public int getSaveDC(Character paladin) {
            return 8 + paladin.getProficiencyBonus() + paladin.getAbilityModifier(Ability.CHARISMA);
        }
    }
}
