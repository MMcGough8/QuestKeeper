package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Bard class features (L1-L5) + College of Lore subclass.
 *
 * <p>5e RAW: Bards pick a College at L3. Spellcasting is handled by the
 * Spellbook (full caster).
 */
public final class BardFeatures {

    public static final String BARDIC_INSPIRATION_ID = "bardic_inspiration";
    public static final String JACK_OF_ALL_TRADES_ID = "jack_of_all_trades";
    public static final String SONG_OF_REST_ID = "song_of_rest";
    public static final String BARD_COLLEGE_ID = "bard_college";
    public static final String LORE_BONUS_PROFICIENCIES_ID = "lore_bonus_proficiencies";
    public static final String CUTTING_WORDS_ID = "cutting_words";
    public static final String FONT_OF_INSPIRATION_ID = "font_of_inspiration";

    private BardFeatures() {}

    public enum BardCollege {
        LORE("College of Lore", "Knowledge focus; Cutting Words to disrupt enemies."),
        VALOR("College of Valor", "Combat support focus (placeholder).");

        private final String displayName;
        private final String description;

        BardCollege(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static List<ClassFeature> createFeaturesForLevel(int level, BardCollege college) {
        List<ClassFeature> features = new ArrayList<>();

        if (level >= 1) {
            features.add(createBardicInspiration(level));
        }

        if (level >= 2) {
            features.add(createJackOfAllTrades());
            features.add(createSongOfRest());
        }

        if (level >= 3 && college != null) {
            features.add(createBardCollegeMarker(college));
            if (college == BardCollege.LORE) {
                features.add(createLoreBonusProficiencies());
                features.add(createCuttingWords());
            }
        }

        if (level >= 5) {
            features.add(createFontOfInspiration());
        }

        return features;
    }

    public static BardicInspiration createBardicInspiration(int bardLevel) {
        return new BardicInspiration(bardLevel);
    }

    public static ClassFeature createJackOfAllTrades() {
        return new PassiveFeature(
            JACK_OF_ALL_TRADES_ID,
            "Jack of All Trades",
            "Starting at 2nd level, you can add half your proficiency bonus, rounded down, to "
                + "any ability check you make that doesn't already include your proficiency bonus.",
            2
        ) {};
    }

    public static ClassFeature createSongOfRest() {
        return new PassiveFeature(
            SONG_OF_REST_ID,
            "Song of Rest",
            "Beginning at 2nd level, you can use soothing music or oration to help revitalize "
                + "your wounded allies during a short rest. Each ally who spends Hit Dice during "
                + "the rest regains an extra 1d6 hit points.",
            2
        ) {};
    }

    public static ClassFeature createBardCollegeMarker(BardCollege college) {
        return new PassiveFeature(
            BARD_COLLEGE_ID,
            "Bard College: " + college.getDisplayName(),
            college.getDescription(),
            3
        ) {};
    }

    public static ClassFeature createLoreBonusProficiencies() {
        return new PassiveFeature(
            LORE_BONUS_PROFICIENCIES_ID,
            "Bonus Proficiencies (Lore)",
            "When you join the College of Lore at 3rd level, you gain proficiency with three "
                + "skills of your choice.",
            3
        ) {};
    }

    public static ClassFeature createCuttingWords() {
        return new PassiveFeature(
            CUTTING_WORDS_ID,
            "Cutting Words",
            "When a creature you can see within 60 feet of you makes an attack roll, an "
                + "ability check, or a damage roll, you can use your reaction to expend one "
                + "of your uses of Bardic Inspiration, rolling a Bardic Inspiration die and "
                + "subtracting it from the creature's roll.",
            3
        ) {};
    }

    public static ClassFeature createFontOfInspiration() {
        return new PassiveFeature(
            FONT_OF_INSPIRATION_ID,
            "Font of Inspiration",
            "Beginning when you reach 5th level, you regain all of your expended uses of "
                + "Bardic Inspiration when you finish a short or long rest.",
            5
        ) {};
    }

    /**
     * Bardic Inspiration (L1+) — bonus action: a creature within 60ft gains
     * a Bardic Inspiration die that can be added to one roll within 10 min.
     * Die scales: d6 (L1-4), d8 (L5-9), d10 (L10-14), d12 (L15+).
     * Resets long rest (L1-4) or short rest (L5+ via Font of Inspiration).
     * Max uses default to 1 here; the caller should resize based on CHA
     * modifier when constructing the Character.
     */
    public static class BardicInspiration extends ActivatedFeature {
        private int bardLevel;

        public BardicInspiration(int bardLevel) {
            super(
                BARDIC_INSPIRATION_ID,
                "Bardic Inspiration",
                "Bonus action to grant an ally within 60ft a Bardic Inspiration die.",
                1,
                1,
                bardLevel >= 5 ? ResetType.SHORT_REST : ResetType.LONG_REST,
                true,
                true
            );
            this.bardLevel = bardLevel;
        }

        public int getInspirationDie() {
            if (bardLevel >= 15) return 12;
            if (bardLevel >= 10) return 10;
            if (bardLevel >= 5) return 8;
            return 6;
        }

        public void setBardLevel(int level) {
            this.bardLevel = level;
        }

        /**
         * Scales the use pool to max(1, Cha mod) per RAW. Caller passes
         * the current CHA modifier; this also updates bardLevel for
         * inspiration die size.
         *
         * <p>Note: the LR -> SR resetType change at L5 (Font of Inspiration)
         * is not handled here because resetType is final on
         * ActivatedFeature. Pre-pitch acceptable for the L3 demo Bard;
         * post-pitch the feature should be rebuilt at the L5 boundary.
         */
        public void setBardLevel(int level, int chaMod) {
            this.bardLevel = level;
            setMaxUses(Math.max(1, chaMod));
        }

        public int getBardLevel() {
            return bardLevel;
        }

        @Override
        protected String activate(Character user) {
            return String.format(
                "Bardic Inspiration die (d%d) granted. Uses: %d/%d.",
                getInspirationDie(), getCurrentUses(), getMaxUses());
        }
    }
}
