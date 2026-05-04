package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Druid class features (L1-L5) + Circle of the Land subclass.
 *
 * <p>5e RAW: Druids pick a Circle at L2. Spellcasting is handled by the
 * Spellbook (full caster).
 */
public final class DruidFeatures {

    public static final String DRUIDIC_ID = "druidic";
    public static final String WILD_SHAPE_ID = "wild_shape";
    public static final String DRUID_CIRCLE_ID = "druid_circle";
    public static final String LAND_BONUS_CANTRIP_ID = "land_bonus_cantrip";
    public static final String NATURAL_RECOVERY_ID = "natural_recovery";

    private DruidFeatures() {}

    public enum DruidCircle {
        LAND("Circle of the Land", "Spellcasting focus; recover spell slots on a short rest."),
        MOON("Circle of the Moon", "Combat shapeshifter focus (placeholder).");

        private final String displayName;
        private final String description;

        DruidCircle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static List<ClassFeature> createFeaturesForLevel(int level, DruidCircle circle) {
        List<ClassFeature> features = new ArrayList<>();

        if (level >= 1) {
            features.add(createDruidic());
        }

        if (level >= 2) {
            features.add(createWildShape(level));
            if (circle != null) {
                features.add(createDruidCircleMarker(circle));
                if (circle == DruidCircle.LAND) {
                    features.add(createLandBonusCantrip());
                    features.add(createNaturalRecovery(level));
                }
            }
        }

        return features;
    }

    public static ClassFeature createDruidic() {
        return new PassiveFeature(
            DRUIDIC_ID,
            "Druidic",
            "You know Druidic, the secret language of druids. You can speak the language and "
                + "use it to leave hidden messages.",
            1
        ) {};
    }

    public static WildShape createWildShape(int druidLevel) {
        return new WildShape(druidLevel);
    }

    public static ClassFeature createDruidCircleMarker(DruidCircle circle) {
        return new PassiveFeature(
            DRUID_CIRCLE_ID,
            "Druid Circle: " + circle.getDisplayName(),
            circle.getDescription(),
            2
        ) {};
    }

    public static ClassFeature createLandBonusCantrip() {
        return new PassiveFeature(
            LAND_BONUS_CANTRIP_ID,
            "Bonus Cantrip (Land)",
            "Circle of the Land druids learn one additional druid cantrip of their choice at "
                + "2nd level.",
            2
        ) {};
    }

    public static NaturalRecovery createNaturalRecovery(int druidLevel) {
        return new NaturalRecovery(druidLevel);
    }

    /**
     * Wild Shape (L2+) — bonus action to transform into a beast. Two uses
     * per short rest. CR cap and form restrictions scale with level
     * (L2: CR 1/4, no swim/fly; L4: CR 1/2, no fly; L8: CR 1, full).
     * For v1 we expose use tracking + a CR cap getter; the shape mechanics
     * themselves are post-pitch.
     */
    public static class WildShape extends ActivatedFeature {
        private int druidLevel;

        public WildShape(int druidLevel) {
            super(
                WILD_SHAPE_ID,
                "Wild Shape",
                "As an action, magically assume the shape of a beast you have seen before. "
                    + "Two uses per short rest.",
                2,
                2,
                ResetType.SHORT_REST,
                true,
                true
            );
            this.druidLevel = druidLevel;
        }

        public double getMaxChallengeRating() {
            if (druidLevel >= 8) return 1.0;
            if (druidLevel >= 4) return 0.5;
            return 0.25;
        }

        public boolean canTakeFlyingForms() {
            return druidLevel >= 8;
        }

        public boolean canTakeSwimmingForms() {
            return druidLevel >= 4;
        }

        public void setDruidLevel(int level) {
            this.druidLevel = level;
        }

        public int getDruidLevel() {
            return druidLevel;
        }

        @Override
        protected String activate(Character user) {
            return String.format(
                "Wild Shape ready (CR %s max). Uses: %d/%d.",
                getMaxChallengeRating() == 1.0 ? "1" :
                    getMaxChallengeRating() == 0.5 ? "1/2" : "1/4",
                getCurrentUses(), getMaxUses());
        }
    }

    /**
     * Natural Recovery (Circle of the Land, L2+) — once per long rest, on a
     * short rest you may recover spell slots whose combined level is up to
     * half your druid level (rounded up). 6th-level slots and higher are
     * excluded.
     */
    public static class NaturalRecovery extends ActivatedFeature {
        private int druidLevel;

        public NaturalRecovery(int druidLevel) {
            super(
                NATURAL_RECOVERY_ID,
                "Natural Recovery",
                "Once per day during a short rest, recover spell slots whose combined level "
                    + "is no greater than half your druid level (rounded up). 6th-level slots "
                    + "and higher are excluded.",
                2,
                1,
                ResetType.LONG_REST,
                false,
                true
            );
            this.druidLevel = druidLevel;
        }

        public int getRecoveryBudget() {
            return (druidLevel + 1) / 2;
        }

        public void setDruidLevel(int level) {
            this.druidLevel = level;
        }

        public int getDruidLevel() {
            return druidLevel;
        }

        @Override
        protected String activate(Character user) {
            return String.format(
                "Natural Recovery readied. Combined slot levels recoverable: %d.",
                getRecoveryBudget());
        }
    }
}
