package com.questkeeper.character.features;

import com.questkeeper.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Warlock class features (L1-L5) + Fiend Patron / Pact Boon subclass.
 *
 * <p>5e RAW: Warlocks pick an Otherworldly Patron at L1 and a Pact Boon at
 * L3. Pact Magic (the Warlock's slot table) is handled by Spellbook with
 * caster type {@code WARLOCK}. This file covers class abilities outside
 * the slot table.
 */
public final class WarlockFeatures {

    public static final String OTHERWORLDLY_PATRON_ID = "otherworldly_patron";
    public static final String DARK_ONES_BLESSING_ID = "dark_ones_blessing";
    public static final String ELDRITCH_INVOCATIONS_ID = "eldritch_invocations";
    public static final String PACT_BOON_ID = "pact_boon";

    private WarlockFeatures() {}

    public enum OtherworldlyPatron {
        FIEND("The Fiend", "Dark One's Blessing: temp HP on enemy kill."),
        ARCHFEY("The Archfey", "Fey-step focus (placeholder)."),
        GREAT_OLD_ONE("The Great Old One", "Telepathic focus (placeholder).");

        private final String displayName;
        private final String description;

        OtherworldlyPatron(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public enum PactBoon {
        TOME("Pact of the Tome", "Book of Shadows: 3 bonus cantrips."),
        BLADE("Pact of the Blade", "Pact weapon you can summon as an action."),
        CHAIN("Pact of the Chain", "Bind a pact familiar (imp, pseudodragon, etc.).");

        private final String displayName;
        private final String description;

        PactBoon(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public static List<ClassFeature> createFeaturesForLevel(
            int level, OtherworldlyPatron patron, PactBoon pactBoon) {
        List<ClassFeature> features = new ArrayList<>();

        if (level >= 1 && patron != null) {
            features.add(createPatronMarker(patron));
            if (patron == OtherworldlyPatron.FIEND) {
                features.add(createDarkOnesBlessing());
            }
        }

        if (level >= 2) {
            features.add(createEldritchInvocations(level));
        }

        if (level >= 3 && pactBoon != null) {
            features.add(createPactBoonMarker(pactBoon));
        }

        return features;
    }

    public static ClassFeature createPatronMarker(OtherworldlyPatron patron) {
        return new PassiveFeature(
            OTHERWORLDLY_PATRON_ID,
            "Otherworldly Patron: " + patron.getDisplayName(),
            patron.getDescription(),
            1
        ) {};
    }

    public static ClassFeature createDarkOnesBlessing() {
        return new PassiveFeature(
            DARK_ONES_BLESSING_ID,
            "Dark One's Blessing",
            "When you reduce a hostile creature to 0 hit points, you gain temporary hit points "
                + "equal to your Charisma modifier + your warlock level (minimum 1).",
            1
        ) {};
    }

    public static ClassFeature createEldritchInvocations(int warlockLevel) {
        return new PassiveFeature(
            ELDRITCH_INVOCATIONS_ID,
            "Eldritch Invocations",
            "Your study of occult lore has unearthed eldritch invocations. You learn "
                + getInvocationCount(warlockLevel) + " invocations at this level. "
                + "(Selection UI is post-pitch.)",
            2
        ) {};
    }

    public static int getInvocationCount(int warlockLevel) {
        if (warlockLevel >= 18) return 8;
        if (warlockLevel >= 15) return 7;
        if (warlockLevel >= 12) return 6;
        if (warlockLevel >= 9) return 5;
        if (warlockLevel >= 7) return 4;
        if (warlockLevel >= 5) return 3;
        return 2;
    }

    public static ClassFeature createPactBoonMarker(PactBoon boon) {
        return new PassiveFeature(
            PACT_BOON_ID,
            "Pact Boon: " + boon.getDisplayName(),
            boon.getDescription(),
            3
        ) {};
    }
}
