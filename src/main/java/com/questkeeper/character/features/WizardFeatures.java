package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard class features (L1-L5) + School of Evocation subclass.
 *
 * <p>5e RAW: Wizards pick an Arcane Tradition at L2. Spellcasting itself is
 * handled by the Spellbook; this file only covers class abilities outside
 * the slot table.
 */
public final class WizardFeatures {

    public static final String ARCANE_RECOVERY_ID = "arcane_recovery";
    public static final String ARCANE_TRADITION_ID = "arcane_tradition";
    public static final String EVOCATION_SAVANT_ID = "evocation_savant";
    public static final String SCULPT_SPELLS_ID = "sculpt_spells";

    private WizardFeatures() {}

    public enum ArcaneTradition {
        EVOCATION("School of Evocation", "Damage spell focus; sculpt allies out of AoEs."),
        ABJURATION("School of Abjuration", "Defensive ward focus (placeholder)."),
        DIVINATION("School of Divination", "Prediction/foresight focus (placeholder)."),
        ENCHANTMENT("School of Enchantment", "Charm focus (placeholder)."),
        ILLUSION("School of Illusion", "Illusion/deception focus (placeholder).");

        private final String displayName;
        private final String description;

        ArcaneTradition(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static List<ClassFeature> createFeaturesForLevel(int level, ArcaneTradition tradition) {
        List<ClassFeature> features = new ArrayList<>();

        if (level >= 1) {
            features.add(createArcaneRecovery(level));
        }

        if (level >= 2 && tradition != null) {
            features.add(createArcaneTraditionMarker(tradition));
            if (tradition == ArcaneTradition.EVOCATION) {
                features.add(createEvocationSavant());
                features.add(createSculptSpells());
            }
        }

        return features;
    }

    public static ArcaneRecovery createArcaneRecovery(int wizardLevel) {
        return new ArcaneRecovery(wizardLevel);
    }

    public static ClassFeature createArcaneTraditionMarker(ArcaneTradition tradition) {
        return new PassiveFeature(
            ARCANE_TRADITION_ID,
            "Arcane Tradition: " + tradition.getDisplayName(),
            tradition.getDescription(),
            2
        ) {};
    }

    public static ClassFeature createEvocationSavant() {
        return new PassiveFeature(
            EVOCATION_SAVANT_ID,
            "Evocation Savant",
            "The gold and time you must spend to copy an evocation spell into your spellbook is "
                + "halved.",
            2
        ) {};
    }

    public static ClassFeature createSculptSpells() {
        return new PassiveFeature(
            SCULPT_SPELLS_ID,
            "Sculpt Spells",
            "When you cast an evocation spell that affects other creatures you can see, you can "
                + "choose a number of them equal to 1 + the spell's level. The chosen creatures "
                + "automatically succeed on their saves and take no damage if the spell would "
                + "otherwise deal half damage on a successful save.",
            2
        ) {};
    }

    /**
     * Arcane Recovery (L1+) — once per day on a short rest, recover spell
     * slots whose combined level total ≤ ceil(wizardLevel / 2). For v1
     * we expose the formula and let the player drive recovery manually.
     */
    public static class ArcaneRecovery extends ActivatedFeature {
        private int wizardLevel;

        public ArcaneRecovery(int wizardLevel) {
            super(
                ARCANE_RECOVERY_ID,
                "Arcane Recovery",
                "Once per day when you finish a short rest, you can recover expended spell "
                    + "slots whose combined level is no greater than half your wizard level "
                    + "(rounded up). Slots recovered cannot include any of 6th level or higher.",
                1,  // Level required
                1,  // Max uses per day
                ResetType.LONG_REST,
                false,
                true
            );
            this.wizardLevel = wizardLevel;
        }

        public int getRecoveryBudget() {
            return (wizardLevel + 1) / 2;
        }

        public void setWizardLevel(int level) {
            this.wizardLevel = level;
        }

        public int getWizardLevel() {
            return wizardLevel;
        }

        @Override
        protected String activate(Character user) {
            return String.format(
                "Arcane Recovery readied. Combined slot levels recoverable: %d "
                    + "(no 6th-level or higher).",
                getRecoveryBudget());
        }
    }
}
