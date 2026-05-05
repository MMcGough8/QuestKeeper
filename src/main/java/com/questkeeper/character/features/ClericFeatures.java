package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleric class feature definitions (L1-L5 baseline + Life Domain).
 *
 * <p>Subclass picks ("Divine Domain") are chosen at L1 in 5e RAW. We model
 * this with a {@link DivineDomain} enum supplied at level-up; for v1 only
 * {@link DivineDomain#LIFE} is fully wired, others fall through to the
 * baseline class features only.
 */
public final class ClericFeatures {

    public static final String DIVINE_DOMAIN_ID = "divine_domain";
    public static final String BONUS_PROFICIENCY_HEAVY_ARMOR_ID = "cleric_heavy_armor";
    public static final String DISCIPLE_OF_LIFE_ID = "disciple_of_life";
    public static final String CLERIC_CHANNEL_DIVINITY_ID = "channel_divinity";
    public static final String TURN_UNDEAD_ID = "turn_undead";
    public static final String PRESERVE_LIFE_ID = "preserve_life";
    public static final String DESTROY_UNDEAD_ID = "destroy_undead";

    private ClericFeatures() {}

    public enum DivineDomain {
        LIFE("Life Domain", "Healing-focused; bonus heavy armor proficiency."),
        WAR("War Domain", "Combat-focused (placeholder)."),
        LIGHT("Light Domain", "Radiant damage focus (placeholder)."),
        TRICKERY("Trickery Domain", "Deception focus (placeholder)."),
        KNOWLEDGE("Knowledge Domain", "Lore focus (placeholder).");

        private final String displayName;
        private final String description;

        DivineDomain(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static List<ClassFeature> createFeaturesForLevel(int level, DivineDomain domain) {
        List<ClassFeature> features = new ArrayList<>();

        if (level >= 1 && domain != null) {
            features.add(createDivineDomainMarker(domain));
            if (domain == DivineDomain.LIFE) {
                features.add(createBonusProficiencyHeavyArmor());
                features.add(createDiscipleOfLife());
            }
        }

        if (level >= 2) {
            features.add(createChannelDivinity());
            features.add(createTurnUndead());
            if (domain == DivineDomain.LIFE) {
                features.add(createPreserveLife());
            }
        }

        if (level >= 5) {
            features.add(createDestroyUndead());
        }

        return features;
    }

    public static ClassFeature createDivineDomainMarker(DivineDomain domain) {
        return new PassiveFeature(
            DIVINE_DOMAIN_ID,
            "Divine Domain: " + domain.getDisplayName(),
            domain.getDescription(),
            1
        ) {};
    }

    public static ClassFeature createBonusProficiencyHeavyArmor() {
        return new PassiveFeature(
            BONUS_PROFICIENCY_HEAVY_ARMOR_ID,
            "Bonus Proficiency (Heavy Armor)",
            "Life Domain clerics gain proficiency with heavy armor.",
            1
        ) {};
    }

    public static ClassFeature createDiscipleOfLife() {
        return new PassiveFeature(
            DISCIPLE_OF_LIFE_ID,
            "Disciple of Life",
            "Whenever you cast a spell of 1st level or higher that restores hit points to a "
                + "creature, the creature regains additional hit points equal to 2 + the spell's "
                + "level.",
            1
        ) {};
    }

    public static ChannelDivinity createChannelDivinity() {
        return new ChannelDivinity();
    }

    public static ClassFeature createTurnUndead() {
        return new PassiveFeature(
            TURN_UNDEAD_ID,
            "Turn Undead",
            "As an action, present your holy symbol and speak a prayer. Each undead within "
                + "30 feet that can see or hear you must make a Wisdom save or be turned for "
                + "1 minute (or until it takes damage).",
            2
        ) {};
    }

    public static ClassFeature createPreserveLife() {
        return new PassiveFeature(
            PRESERVE_LIFE_ID,
            "Channel Divinity: Preserve Life",
            "As an action, present your holy symbol to restore a number of hit points equal "
                + "to five times your cleric level among creatures of your choice within 30 feet "
                + "(no creature healed beyond half its HP max).",
            2
        ) {};
    }

    public static ClassFeature createDestroyUndead() {
        return new PassiveFeature(
            DESTROY_UNDEAD_ID,
            "Destroy Undead (CR 1/2)",
            "When an undead of CR 1/2 or lower fails its save against your Turn Undead, the "
                + "creature is instantly destroyed.",
            5
        ) {};
    }

    /**
     * Cleric Channel Divinity. RAW use-per-short-rest pool: 1 at L2-5,
     * 2 at L6-17, 3 at L18+. ClassFeatureWiring should call
     * {@link #setClericLevel(int)} on level-up to keep the pool sized
     * correctly.
     */
    public static class ChannelDivinity extends ActivatedFeature {
        public ChannelDivinity() {
            super(
                CLERIC_CHANNEL_DIVINITY_ID,
                "Channel Divinity",
                "You gain the ability to channel divine energy directly from your deity. "
                    + "Choose Turn Undead or your domain's Channel Divinity option. "
                    + "Recovers on a short or long rest.",
                2,  // Level required
                1,  // Max uses at L2-5
                ResetType.SHORT_REST,
                true,
                true
            );
        }

        @Override
        protected String activate(Character user) {
            return "Channel Divinity is ready. Choose: Turn Undead"
                + " (or domain option if available).";
        }

        /**
         * Resizes the use pool per RAW: 1 at L2, 2 at L6, 3 at L18.
         * ActivatedFeature.setMaxUses grants the delta on growth and
         * clamps current on shrink, so calling this on every level-up
         * is safe and idempotent.
         */
        public void setClericLevel(int level) {
            int newMax = level >= 18 ? 3 : level >= 6 ? 2 : 1;
            setMaxUses(newMax);
        }
    }
}
