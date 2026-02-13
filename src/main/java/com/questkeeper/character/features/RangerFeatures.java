package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for Ranger class features.
 *
 * Ranger features by level:
 * - Level 1: Favored Enemy, Natural Explorer
 * - Level 2: Fighting Style, Spellcasting
 * - Level 3: Ranger Archetype (Hunter), Primeval Awareness
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class RangerFeatures {

    // Feature IDs
    public static final String FAVORED_ENEMY_ID = "favored_enemy";
    public static final String NATURAL_EXPLORER_ID = "natural_explorer";
    public static final String RANGER_FIGHTING_STYLE_ID = "ranger_fighting_style";
    public static final String RANGER_SPELLCASTING_ID = "ranger_spellcasting";
    public static final String PRIMEVAL_AWARENESS_ID = "primeval_awareness";
    public static final String COLOSSUS_SLAYER_ID = "colossus_slayer";
    public static final String GIANT_KILLER_ID = "giant_killer";
    public static final String HORDE_BREAKER_ID = "horde_breaker";

    /**
     * Types of favored enemies a Ranger can choose.
     */
    public enum FavoredEnemyType {
        ABERRATIONS("Aberrations", "Creatures from the Far Realm"),
        BEASTS("Beasts", "Natural animals"),
        CELESTIALS("Celestials", "Divine beings"),
        CONSTRUCTS("Constructs", "Artificial creatures"),
        DRAGONS("Dragons", "Draconic creatures"),
        ELEMENTALS("Elementals", "Creatures of the elements"),
        FEY("Fey", "Creatures of the Feywild"),
        FIENDS("Fiends", "Demons and devils"),
        GIANTS("Giants", "Giant-kin"),
        MONSTROSITIES("Monstrosities", "Unnatural creatures"),
        OOZES("Oozes", "Amorphous creatures"),
        PLANTS("Plants", "Animated plant creatures"),
        UNDEAD("Undead", "Creatures of unlife"),
        HUMANOIDS_TWO("Humanoids (two races)", "Two humanoid races of choice");

        private final String displayName;
        private final String description;

        FavoredEnemyType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    /**
     * Types of favored terrain a Ranger can choose.
     */
    public enum FavoredTerrainType {
        ARCTIC("Arctic", "Frozen tundra and icy wastes"),
        COAST("Coast", "Shorelines and beaches"),
        DESERT("Desert", "Sandy wastes and badlands"),
        FOREST("Forest", "Woodlands and jungles"),
        GRASSLAND("Grassland", "Plains and savannas"),
        MOUNTAIN("Mountain", "High peaks and foothills"),
        SWAMP("Swamp", "Marshes and bogs"),
        UNDERDARK("Underdark", "Subterranean realms");

        private final String displayName;
        private final String description;

        FavoredTerrainType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private RangerFeatures() {
        // Utility class
    }

    /**
     * Creates all Ranger features appropriate for the given level.
     *
     * @param level the character's Ranger level
     * @param favoredEnemy the chosen favored enemy (null if not yet chosen)
     * @param favoredTerrain the chosen favored terrain (null if not yet chosen)
     * @param fightingStyle the chosen fighting style (null if not yet chosen)
     * @param hunterChoice the Hunter's Prey choice (null if not yet chosen or not Hunter)
     * @return list of features available at this level
     */
    public static List<ClassFeature> createFeaturesForLevel(int level,
            FavoredEnemyType favoredEnemy, FavoredTerrainType favoredTerrain,
            FightingStyle fightingStyle, HuntersPrey hunterChoice) {
        List<ClassFeature> features = new ArrayList<>();

        // Level 1: Favored Enemy, Natural Explorer
        if (level >= 1) {
            if (favoredEnemy != null) {
                features.add(createFavoredEnemy(favoredEnemy));
            }
            if (favoredTerrain != null) {
                features.add(createNaturalExplorer(favoredTerrain));
            }
        }

        // Level 2: Fighting Style, Spellcasting
        if (level >= 2) {
            if (fightingStyle != null) {
                features.add(createFightingStyleFeature(fightingStyle));
            }
            features.add(createRangerSpellcasting(level));
        }

        // Level 3: Ranger Archetype (Hunter), Primeval Awareness
        if (level >= 3) {
            features.add(createPrimevalAwareness());
            if (hunterChoice != null) {
                features.add(createHuntersPrey(hunterChoice));
            }
        }

        return features;
    }

    /**
     * Gets Ranger spell slots by level.
     * Returns array where index 0 = 1st level slots, etc.
     */
    public static int[] getSpellSlots(int rangerLevel) {
        return switch (rangerLevel) {
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
     * Gets spells known by Ranger level.
     */
    public static int getSpellsKnown(int rangerLevel) {
        if (rangerLevel < 2) return 0;
        if (rangerLevel == 2) return 2;
        if (rangerLevel <= 4) return 3;
        if (rangerLevel <= 6) return 4;
        if (rangerLevel <= 8) return 5;
        if (rangerLevel <= 10) return 6;
        if (rangerLevel <= 12) return 7;
        if (rangerLevel <= 14) return 8;
        if (rangerLevel <= 16) return 9;
        if (rangerLevel <= 18) return 10;
        return 11;  // Level 19-20
    }

    // ==========================================
    // Factory Methods
    // ==========================================

    public static FavoredEnemy createFavoredEnemy(FavoredEnemyType enemyType) {
        return new FavoredEnemy(enemyType);
    }

    public static NaturalExplorer createNaturalExplorer(FavoredTerrainType terrainType) {
        return new NaturalExplorer(terrainType);
    }

    public static FightingStyleFeature createFightingStyleFeature(FightingStyle style) {
        return new FightingStyleFeature(style);
    }

    public static RangerSpellcasting createRangerSpellcasting(int rangerLevel) {
        return new RangerSpellcasting(rangerLevel);
    }

    public static PrimevalAwareness createPrimevalAwareness() {
        return new PrimevalAwareness();
    }

    public static ClassFeature createHuntersPrey(HuntersPrey choice) {
        return switch (choice) {
            case COLOSSUS_SLAYER -> createColossusSlayer();
            case GIANT_KILLER -> createGiantKiller();
            case HORDE_BREAKER -> createHordeBreaker();
        };
    }

    public static ColossusSlayer createColossusSlayer() {
        return new ColossusSlayer();
    }

    public static GiantKiller createGiantKiller() {
        return new GiantKiller();
    }

    public static HordeBreaker createHordeBreaker() {
        return new HordeBreaker();
    }

    // ==========================================
    // Hunter's Prey Choices
    // ==========================================

    public enum HuntersPrey {
        COLOSSUS_SLAYER("Colossus Slayer", "Extra 1d8 damage to wounded targets"),
        GIANT_KILLER("Giant Killer", "Reaction attack when Large+ creature misses"),
        HORDE_BREAKER("Horde Breaker", "Extra attack against adjacent creature");

        private final String displayName;
        private final String description;

        HuntersPrey(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // ==========================================
    // Concrete Feature Classes
    // ==========================================

    /**
     * Favored Enemy - Expertise against specific enemy types.
     */
    public static class FavoredEnemy extends PassiveFeature {

        private final FavoredEnemyType enemyType;

        public FavoredEnemy(FavoredEnemyType enemyType) {
            super(
                FAVORED_ENEMY_ID,
                "Favored Enemy: " + enemyType.getDisplayName(),
                "You have significant experience studying, tracking, hunting, and even talking to " +
                    enemyType.getDisplayName().toLowerCase() + ".\n" +
                    "- Advantage on Survival checks to track your favored enemies\n" +
                    "- Advantage on Intelligence checks to recall information about them\n" +
                    "- You learn one language spoken by your favored enemies (if any)",
                1  // Level required
            );
            this.enemyType = enemyType;
        }

        public FavoredEnemyType getEnemyType() {
            return enemyType;
        }

        /**
         * Checks if a creature type matches this favored enemy.
         */
        public boolean isFavoredEnemy(String creatureType) {
            return creatureType != null &&
                creatureType.toUpperCase().contains(enemyType.name().replace("_", " "));
        }
    }

    /**
     * Natural Explorer - Expertise in favored terrain.
     */
    public static class NaturalExplorer extends PassiveFeature {

        private final FavoredTerrainType terrainType;

        public NaturalExplorer(FavoredTerrainType terrainType) {
            super(
                NATURAL_EXPLORER_ID,
                "Natural Explorer: " + terrainType.getDisplayName(),
                "You are a master of navigating " + terrainType.getDisplayName().toLowerCase() + " terrain.\n" +
                    "While traveling in your favored terrain:\n" +
                    "- Difficult terrain doesn't slow your group's travel\n" +
                    "- Your group can't become lost except by magical means\n" +
                    "- You remain alert to danger even when engaged in other activities\n" +
                    "- You can move stealthily at a normal pace when alone\n" +
                    "- You find twice as much food when foraging\n" +
                    "- You learn exact details about creatures you track",
                1  // Level required
            );
            this.terrainType = terrainType;
        }

        public FavoredTerrainType getTerrainType() {
            return terrainType;
        }
    }

    /**
     * Fighting Style Feature for Ranger.
     * Rangers can choose: Archery, Defense, Dueling, Two-Weapon Fighting
     */
    public static class FightingStyleFeature extends PassiveFeature {

        private final FightingStyle style;

        public FightingStyleFeature(FightingStyle style) {
            super(
                RANGER_FIGHTING_STYLE_ID,
                "Fighting Style: " + style.getDisplayName(),
                style.getDescription(),
                2  // Level required for Ranger
            );
            this.style = style;
        }

        public FightingStyle getStyle() {
            return style;
        }
    }

    /**
     * Ranger Spellcasting - Tracks spell slots for Rangers.
     */
    public static class RangerSpellcasting extends PassiveFeature {

        private int rangerLevel;
        private int[] currentSlots;

        public RangerSpellcasting(int rangerLevel) {
            super(
                RANGER_SPELLCASTING_ID,
                "Spellcasting",
                "You have learned to use the magical essence of nature to cast spells. " +
                    "You know a number of ranger spells and can cast them using spell slots.",
                2  // Level required
            );
            this.rangerLevel = rangerLevel;
            this.currentSlots = getSpellSlots(rangerLevel).clone();
        }

        /**
         * Checks if a spell slot of the given level is available.
         */
        public boolean hasSlot(int slotLevel) {
            if (slotLevel < 1 || slotLevel > 5) return false;
            return currentSlots[slotLevel - 1] > 0;
        }

        /**
         * Expends a spell slot.
         */
        public boolean expendSlot(int slotLevel) {
            if (!hasSlot(slotLevel)) return false;
            currentSlots[slotLevel - 1]--;
            return true;
        }

        /**
         * Gets current spell slots.
         */
        public int[] getCurrentSlots() {
            return currentSlots.clone();
        }

        /**
         * Gets max spell slots for current level.
         */
        public int[] getMaxSlots() {
            return getSpellSlots(rangerLevel);
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
            currentSlots = getSpellSlots(rangerLevel).clone();
        }

        /**
         * Updates Ranger level and spell slots.
         */
        public void setRangerLevel(int level) {
            this.rangerLevel = level;
        }

        public int getRangerLevel() {
            return rangerLevel;
        }

        public int getSpellsKnown() {
            return RangerFeatures.getSpellsKnown(rangerLevel);
        }
    }

    /**
     * Primeval Awareness - Sense specific creature types.
     */
    public static class PrimevalAwareness extends PassiveFeature {

        public PrimevalAwareness() {
            super(
                PRIMEVAL_AWARENESS_ID,
                "Primeval Awareness",
                "You can use your action and expend one ranger spell slot to focus your awareness " +
                    "on the region around you. For 1 minute per level of the spell slot expended, " +
                    "you can sense whether the following types of creatures are present within 1 mile: " +
                    "aberrations, celestials, dragons, elementals, fey, fiends, and undead.\n" +
                    "This feature doesn't reveal the creatures' location or number.",
                3  // Level required
            );
        }
    }

    /**
     * Colossus Slayer - Extra damage to wounded targets.
     */
    public static class ColossusSlayer extends PassiveFeature {

        private boolean usedThisTurn = false;

        public ColossusSlayer() {
            super(
                COLOSSUS_SLAYER_ID,
                "Colossus Slayer",
                "Your tenacity can wear down the most potent foes. When you hit a creature with a " +
                    "weapon attack, the creature takes an extra 1d8 damage if it's below its hit point " +
                    "maximum. You can deal this extra damage only once per turn.",
                3  // Level required
            );
        }

        /**
         * Checks if Colossus Slayer can be used this turn.
         */
        public boolean canUse() {
            return !usedThisTurn;
        }

        /**
         * Marks Colossus Slayer as used this turn.
         */
        public void use() {
            usedThisTurn = true;
        }

        /**
         * Resets at the start of each turn.
         */
        public void resetTurn() {
            usedThisTurn = false;
        }

        /**
         * Gets the extra damage (1d8).
         */
        public String getExtraDamage() {
            return "1d8";
        }
    }

    /**
     * Giant Killer - Reaction attack when Large+ creature misses.
     */
    public static class GiantKiller extends PassiveFeature {

        private boolean usedThisRound = false;

        public GiantKiller() {
            super(
                GIANT_KILLER_ID,
                "Giant Killer",
                "When a Large or larger creature within 5 feet of you hits or misses you with an attack, " +
                    "you can use your reaction to attack that creature immediately after its attack, " +
                    "provided that you can see the creature.",
                3  // Level required
            );
        }

        /**
         * Checks if Giant Killer reaction is available.
         */
        public boolean canUseReaction() {
            return !usedThisRound;
        }

        /**
         * Uses the Giant Killer reaction.
         */
        public void useReaction() {
            usedThisRound = true;
        }

        /**
         * Resets at the start of each round.
         */
        public void resetRound() {
            usedThisRound = false;
        }
    }

    /**
     * Horde Breaker - Extra attack against adjacent creature.
     */
    public static class HordeBreaker extends PassiveFeature {

        private boolean usedThisTurn = false;

        public HordeBreaker() {
            super(
                HORDE_BREAKER_ID,
                "Horde Breaker",
                "Once on each of your turns when you make a weapon attack, you can make another attack " +
                    "with the same weapon against a different creature that is within 5 feet of the " +
                    "original target and within range of your weapon.",
                3  // Level required
            );
        }

        /**
         * Checks if Horde Breaker can be used this turn.
         */
        public boolean canUse() {
            return !usedThisTurn;
        }

        /**
         * Marks Horde Breaker as used this turn.
         */
        public void use() {
            usedThisTurn = true;
        }

        /**
         * Resets at the start of each turn.
         */
        public void resetTurn() {
            usedThisTurn = false;
        }
    }
}
