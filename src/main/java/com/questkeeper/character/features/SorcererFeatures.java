package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorcerer class features (L1-L5) + Draconic Origin subclass.
 *
 * <p>5e RAW: Sorcerers pick a Sorcerous Origin at L1 and gain Font of Magic
 * (sorcery points) at L2. Spellcasting itself is handled by the Spellbook;
 * this file only covers class abilities outside the slot table.
 */
public final class SorcererFeatures {

    public static final String SORCEROUS_ORIGIN_ID = "sorcerous_origin";
    public static final String DRACONIC_RESILIENCE_ID = "draconic_resilience";
    public static final String DRACONIC_ANCESTRY_ID = "draconic_ancestry";
    public static final String FONT_OF_MAGIC_ID = "font_of_magic";

    private SorcererFeatures() {}

    public enum SorcerousOrigin {
        DRACONIC("Draconic Bloodline", "Dragon ancestry; +1 HP/level, AC=13+DEX without armor."),
        WILD_MAGIC("Wild Magic", "Chaotic surges (placeholder).");

        private final String displayName;
        private final String description;

        SorcerousOrigin(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static List<ClassFeature> createFeaturesForLevel(int level, SorcerousOrigin origin) {
        List<ClassFeature> features = new ArrayList<>();

        if (level >= 1 && origin != null) {
            features.add(createSorcerousOriginMarker(origin));
            if (origin == SorcerousOrigin.DRACONIC) {
                features.add(createDraconicAncestry());
                features.add(createDraconicResilience());
            }
        }

        if (level >= 2) {
            features.add(createFontOfMagic(level));
        }

        return features;
    }

    public static ClassFeature createSorcerousOriginMarker(SorcerousOrigin origin) {
        return new PassiveFeature(
            SORCEROUS_ORIGIN_ID,
            "Sorcerous Origin: " + origin.getDisplayName(),
            origin.getDescription(),
            1
        ) {};
    }

    public static ClassFeature createDraconicAncestry() {
        return new PassiveFeature(
            DRACONIC_ANCESTRY_ID,
            "Draconic Ancestry",
            "Choose one type of dragon as your ancestor. Whenever you make a Charisma check "
                + "interacting with dragons, your proficiency bonus is doubled if it applies.",
            1
        ) {};
    }

    public static ClassFeature createDraconicResilience() {
        return new PassiveFeature(
            DRACONIC_RESILIENCE_ID,
            "Draconic Resilience",
            "Your hit point maximum increases by 1, and increases by 1 again whenever you gain "
                + "a level in this class. When you aren't wearing armor, your AC equals 13 + "
                + "your Dexterity modifier.",
            1
        ) {};
    }

    public static FontOfMagic createFontOfMagic(int sorcererLevel) {
        return new FontOfMagic(sorcererLevel);
    }

    /**
     * Font of Magic (L2+) — sorcery points equal sorcerer level (min 2).
     * Players spend points to convert spell slots or fuel metamagic.
     * Recovers on a long rest.
     */
    public static class FontOfMagic extends ActivatedFeature {
        private int sorcererLevel;

        public FontOfMagic(int sorcererLevel) {
            super(
                FONT_OF_MAGIC_ID,
                "Font of Magic",
                "Sorcery points fuel a variety of magical abilities. You have a number of "
                    + "sorcery points equal to your sorcerer level. You regain all spent sorcery "
                    + "points when you finish a long rest.",
                2,  // Level required
                Math.max(2, sorcererLevel),  // Sorcery points = sorcerer level (min 2)
                ResetType.LONG_REST,
                true,
                true
            );
            this.sorcererLevel = sorcererLevel;
        }

        public int getSorceryPoints() {
            return getCurrentUses();
        }

        public int getMaxSorceryPoints() {
            return Math.max(2, sorcererLevel);
        }

        public boolean spendPoints(int amount) {
            if (amount <= 0 || amount > getCurrentUses()) return false;
            setCurrentUses(getCurrentUses() - amount);
            return true;
        }

        public void setSorcererLevel(int level) {
            this.sorcererLevel = level;
            // ActivatedFeature.setMaxUses already grants the delta on growth
            // and clamps current to the new max on shrink. The previous code
            // applied the delta a second time, doubling sorcery-point gains
            // every level-up.
            setMaxUses(getMaxSorceryPoints());
        }

        public int getSorcererLevel() {
            return sorcererLevel;
        }

        @Override
        protected String activate(Character user) {
            return String.format("Font of Magic ready. Sorcery points: %d/%d.",
                getCurrentUses(), getMaxUses());
        }
    }
}
