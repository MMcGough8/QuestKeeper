package com.questkeeper.character;

import com.questkeeper.character.features.BarbarianFeatures;
import com.questkeeper.character.features.BardFeatures;
import com.questkeeper.character.features.ClassFeature;
import com.questkeeper.character.features.ClericFeatures;
import com.questkeeper.character.features.DruidFeatures;
import com.questkeeper.character.features.FighterFeatures;
import com.questkeeper.character.features.MonkFeatures;
import com.questkeeper.character.features.PaladinFeatures;
import com.questkeeper.character.features.RangerFeatures;
import com.questkeeper.character.features.RogueFeatures;
import com.questkeeper.character.features.SorcererFeatures;
import com.questkeeper.character.features.WizardFeatures;
import com.questkeeper.character.features.WarlockFeatures;

import java.util.List;

/**
 * Encapsulates the per-class branching that turns a character's level +
 * subclass choices into a list of {@link ClassFeature} instances on the
 * character. Extracted from {@link Character#initializeClassFeatures()} so
 * the Character class stays focused on identity + state, and so this
 * dispatch table is reviewable in isolation.
 *
 * <p>Package-private — call only from inside {@code Character}.
 */
final class ClassFeatureWiring {

    private ClassFeatureWiring() {}

    static void wire(Character c) {
        Character.CharacterClass cc = c.getCharacterClass();
        int level = c.getLevel();

        if (cc == Character.CharacterClass.FIGHTER) {
            wireFighter(c, level);
        } else if (cc == Character.CharacterClass.ROGUE) {
            wireRogue(c, level);
        } else if (cc == Character.CharacterClass.BARBARIAN) {
            wireBarbarian(c, level);
        } else if (cc == Character.CharacterClass.MONK) {
            wireMonk(c, level);
        } else if (cc == Character.CharacterClass.PALADIN) {
            wirePaladin(c, level);
        } else if (cc == Character.CharacterClass.RANGER) {
            wireRanger(c, level);
        } else if (cc == Character.CharacterClass.CLERIC) {
            wireCleric(c, level);
        } else if (cc == Character.CharacterClass.WIZARD) {
            wireWizard(c, level);
        } else if (cc == Character.CharacterClass.SORCERER) {
            wireSorcerer(c, level);
        } else if (cc == Character.CharacterClass.BARD) {
            wireBard(c, level);
        } else if (cc == Character.CharacterClass.DRUID) {
            wireDruid(c, level);
        } else if (cc == Character.CharacterClass.WARLOCK) {
            wireWarlock(c, level);
        }
    }

    private static void wireFighter(Character c, int level) {
        for (ClassFeature feature : FighterFeatures.createFeaturesForLevel(level, c.getFightingStyle())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            }
        }
        // Action Surge gains a second use at Fighter L17.
        c.getFeature(FighterFeatures.ACTION_SURGE_ID)
            .filter(f -> f instanceof FighterFeatures.ActionSurge)
            .map(f -> (FighterFeatures.ActionSurge) f)
            .ifPresent(as -> as.setFighterLevel(level));
    }

    private static void wireRogue(Character c, int level) {
        for (ClassFeature feature : RogueFeatures.createFeaturesForLevel(level, c.getExpertiseSkills())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(RogueFeatures.SNEAK_ATTACK_ID)) {
                c.getFeature(RogueFeatures.SNEAK_ATTACK_ID)
                    .filter(f -> f instanceof RogueFeatures.SneakAttack)
                    .map(f -> (RogueFeatures.SneakAttack) f)
                    .ifPresent(sa -> sa.setRogueLevel(level));
            }
        }
    }

    private static void wireBarbarian(Character c, int level) {
        for (ClassFeature feature : BarbarianFeatures.createFeaturesForLevel(level)) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(BarbarianFeatures.RAGE_ID)) {
                c.getFeature(BarbarianFeatures.RAGE_ID)
                    .filter(f -> f instanceof BarbarianFeatures.Rage)
                    .map(f -> (BarbarianFeatures.Rage) f)
                    .ifPresent(rage -> rage.setBarbarianLevel(level));
            } else if (feature.getId().equals(BarbarianFeatures.BRUTAL_CRITICAL_ID)) {
                c.getFeature(BarbarianFeatures.BRUTAL_CRITICAL_ID)
                    .filter(f -> f instanceof BarbarianFeatures.BrutalCritical)
                    .map(f -> (BarbarianFeatures.BrutalCritical) f)
                    .ifPresent(bc -> bc.setBarbarianLevel(level));
            }
        }
    }

    private static void wireMonk(Character c, int level) {
        for (ClassFeature feature : MonkFeatures.createFeaturesForLevel(level)) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(MonkFeatures.MARTIAL_ARTS_ID)) {
                c.getFeature(MonkFeatures.MARTIAL_ARTS_ID)
                    .filter(f -> f instanceof MonkFeatures.MartialArts)
                    .map(f -> (MonkFeatures.MartialArts) f)
                    .ifPresent(ma -> ma.setMonkLevel(level));
            } else if (feature.getId().equals(MonkFeatures.KI_ID)) {
                c.getFeature(MonkFeatures.KI_ID)
                    .filter(f -> f instanceof MonkFeatures.Ki)
                    .map(f -> (MonkFeatures.Ki) f)
                    .ifPresent(ki -> ki.setMonkLevel(level));
            } else if (feature.getId().equals(MonkFeatures.DEFLECT_MISSILES_ID)) {
                c.getFeature(MonkFeatures.DEFLECT_MISSILES_ID)
                    .filter(f -> f instanceof MonkFeatures.DeflectMissiles)
                    .map(f -> (MonkFeatures.DeflectMissiles) f)
                    .ifPresent(dm -> dm.setMonkLevel(level));
            }
        }
    }

    private static void wirePaladin(Character c, int level) {
        List<ClassFeature> paladinFeatures =
            PaladinFeatures.createFeaturesForLevel(level, c.getFightingStyle());

        for (ClassFeature feature : paladinFeatures) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
                if (feature.getId().equals(PaladinFeatures.DIVINE_SMITE_ID)
                        && feature instanceof PaladinFeatures.DivineSmite ds) {
                    ds.bindSpellSlots(c.getSpellbook().getSpellSlots());
                }
            } else if (feature.getId().equals(PaladinFeatures.LAY_ON_HANDS_ID)) {
                c.getFeature(PaladinFeatures.LAY_ON_HANDS_ID)
                    .filter(f -> f instanceof PaladinFeatures.LayOnHands)
                    .map(f -> (PaladinFeatures.LayOnHands) f)
                    .ifPresent(loh -> loh.setPaladinLevel(level));
            } else if (feature.getId().equals(PaladinFeatures.DIVINE_SMITE_ID)) {
                c.getFeature(PaladinFeatures.DIVINE_SMITE_ID)
                    .filter(f -> f instanceof PaladinFeatures.DivineSmite)
                    .map(f -> (PaladinFeatures.DivineSmite) f)
                    .ifPresent(ds -> ds.setPaladinLevel(level));
            }
        }

        c.getFeature(PaladinFeatures.DIVINE_SENSE_ID)
            .filter(f -> f instanceof PaladinFeatures.DivineSense)
            .map(f -> (PaladinFeatures.DivineSense) f)
            .ifPresent(ds -> ds.updateMaxUses(c));
    }

    private static void wireRanger(Character c, int level) {
        List<ClassFeature> rangerFeatures = RangerFeatures.createFeaturesForLevel(
            level, null, null, c.getFightingStyle(), null);
        for (ClassFeature feature : rangerFeatures) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(RangerFeatures.RANGER_SPELLCASTING_ID)) {
                c.getFeature(RangerFeatures.RANGER_SPELLCASTING_ID)
                    .filter(f -> f instanceof RangerFeatures.RangerSpellcasting)
                    .map(f -> (RangerFeatures.RangerSpellcasting) f)
                    .ifPresent(rs -> rs.setRangerLevel(level));
            }
        }
    }

    private static void wireCleric(Character c, int level) {
        for (ClassFeature feature : ClericFeatures.createFeaturesForLevel(level, c.getDivineDomain())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            }
        }
        // Channel Divinity scales 1 -> 2 at L6 -> 3 at L18; resize on each level-up.
        c.getFeature(ClericFeatures.CLERIC_CHANNEL_DIVINITY_ID)
            .filter(f -> f instanceof ClericFeatures.ChannelDivinity)
            .map(f -> (ClericFeatures.ChannelDivinity) f)
            .ifPresent(cd -> cd.setClericLevel(level));
    }

    private static void wireWizard(Character c, int level) {
        for (ClassFeature feature : WizardFeatures.createFeaturesForLevel(level, c.getArcaneTradition())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(WizardFeatures.ARCANE_RECOVERY_ID)) {
                c.getFeature(WizardFeatures.ARCANE_RECOVERY_ID)
                    .filter(f -> f instanceof WizardFeatures.ArcaneRecovery)
                    .map(f -> (WizardFeatures.ArcaneRecovery) f)
                    .ifPresent(ar -> ar.setWizardLevel(level));
            }
        }
    }

    private static void wireSorcerer(Character c, int level) {
        for (ClassFeature feature : SorcererFeatures.createFeaturesForLevel(level, c.getSorcerousOrigin())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(SorcererFeatures.FONT_OF_MAGIC_ID)) {
                c.getFeature(SorcererFeatures.FONT_OF_MAGIC_ID)
                    .filter(f -> f instanceof SorcererFeatures.FontOfMagic)
                    .map(f -> (SorcererFeatures.FontOfMagic) f)
                    .ifPresent(fom -> fom.setSorcererLevel(level));
            }
        }
    }

    private static void wireBard(Character c, int level) {
        for (ClassFeature feature : BardFeatures.createFeaturesForLevel(level, c.getBardCollege())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(BardFeatures.BARDIC_INSPIRATION_ID)) {
                c.getFeature(BardFeatures.BARDIC_INSPIRATION_ID)
                    .filter(f -> f instanceof BardFeatures.BardicInspiration)
                    .map(f -> (BardFeatures.BardicInspiration) f)
                    .ifPresent(bi -> bi.setBardLevel(level));
            }
        }
    }

    private static void wireDruid(Character c, int level) {
        for (ClassFeature feature : DruidFeatures.createFeaturesForLevel(level, c.getDruidCircle())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            } else if (feature.getId().equals(DruidFeatures.WILD_SHAPE_ID)) {
                c.getFeature(DruidFeatures.WILD_SHAPE_ID)
                    .filter(f -> f instanceof DruidFeatures.WildShape)
                    .map(f -> (DruidFeatures.WildShape) f)
                    .ifPresent(ws -> ws.setDruidLevel(level));
            } else if (feature.getId().equals(DruidFeatures.NATURAL_RECOVERY_ID)) {
                c.getFeature(DruidFeatures.NATURAL_RECOVERY_ID)
                    .filter(f -> f instanceof DruidFeatures.NaturalRecovery)
                    .map(f -> (DruidFeatures.NaturalRecovery) f)
                    .ifPresent(nr -> nr.setDruidLevel(level));
            }
        }
    }

    private static void wireWarlock(Character c, int level) {
        for (ClassFeature feature : WarlockFeatures.createFeaturesForLevel(
                level, c.getWarlockPatron(), c.getWarlockPactBoon())) {
            if (c.getFeature(feature.getId()).isEmpty()) {
                c.addClassFeatureInternal(feature);
            }
        }
    }
}
