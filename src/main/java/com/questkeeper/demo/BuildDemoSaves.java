package com.questkeeper.demo;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.campaign.CampaignInfo;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.character.features.BardFeatures;
import com.questkeeper.character.features.FightingStyle;
import com.questkeeper.character.features.WizardFeatures;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.StandardEquipment;
import com.questkeeper.save.SaveState;
import com.questkeeper.state.GameState;

import java.nio.file.Path;
import java.util.List;

/**
 * Generates 4 demo save states for the 5-minute pitch (2026-05-27).
 *
 * <p>Each save targets a specific engine capability: combat depth (Paladin),
 * caster mechanics (Wizard), narrative + trial system (Bard), and
 * higher-level scaling (Barbarian).
 *
 * <p>Run via Maven exec:
 * <pre>{@code mvn exec:java -Dexec.mainClass="com.questkeeper.demo.BuildDemoSaves" -q}</pre>
 *
 * <p>Output: {@code saves/demo/0N-<name>.yaml} files.
 */
public final class BuildDemoSaves {

    private BuildDemoSaves() {}

    public static void main(String[] args) throws Exception {
        System.out.println("Building demo saves...");

        Path p1 = build01Paladin();
        System.out.println("  ✓ " + p1);

        Path p2 = build02Wizard();
        System.out.println("  ✓ " + p2);

        Path p3 = build03BardTrial();
        System.out.println("  ✓ " + p3);

        Path p4 = build04BerserkerL9();
        System.out.println("  ✓ " + p4);

        System.out.println("Done. 4 saves written to saves/demo/.");
    }

    private static Path build01Paladin() throws Exception {
        Campaign campaign = loadCampaign("muddlebrook");

        Character paladin = new Character("Aelar Brightwood", Race.HUMAN, CharacterClass.PALADIN,
            16, 10, 14, 10, 12, 16);
        paladin.setLevel(5);
        // Spend the L4 ASI: +2 STR (Paladin loves big STR).
        if (paladin.getPendingAbilityScoreImprovements() > 0) {
            paladin.applyAbilityScoreImprovement(Ability.STRENGTH);
        }
        paladin.setFightingStyle(FightingStyle.DEFENSE);
        paladin.addSkillProficiency(Skill.ATHLETICS);
        paladin.addSkillProficiency(Skill.PERSUASION);
        paladin.addSkillProficiency(Skill.RELIGION);

        StandardEquipment eq = StandardEquipment.getInstance();
        Inventory inv = paladin.getInventory();
        inv.addItem(eq.getWeapon("longsword"));
        inv.addItem(eq.getArmor("chain_mail"));
        inv.addItem(eq.getArmor("shield"));
        inv.equipToSlot(eq.getWeapon("longsword"), Inventory.EquipmentSlot.MAIN_HAND);
        inv.equipToSlot(eq.getArmor("chain_mail"), Inventory.EquipmentSlot.ARMOR);
        inv.equipToSlot(eq.getArmor("shield"), Inventory.EquipmentSlot.OFF_HAND);
        inv.addGold(85);

        GameState state = new GameState(paladin, campaign);
        state.setFlag("met_norrin", true);
        state.setFlag("started_trial_01", true);
        state.setFlag("completed_trial_01", true);
        state.setFlag("clocktower_hill_unlocked", true);
        state.unlockLocation("clocktower_base");
        state.unlockLocation("clocktower_hill");
        state.moveToLocation(campaign.getLocation("drunken_dragon_inn"));
        state.moveToLocation(campaign.getLocation("town_square"));
        state.moveToLocation(campaign.getLocation("mayors_office"));
        state.moveToLocation(campaign.getLocation("clocktower_hill"));
        state.moveToLocation(campaign.getLocation("clocktower_base"));

        healToFull(state.getCharacter());
        SaveState save = state.toSaveState();
        save.setSaveName("Aelar (Paladin L5) - Trial 1 done, gearing up for the clocktower");
        Path path = Path.of("saves", "demo", "01-paladin-l5-combat.yaml");
        save.save(path);
        return path;
    }

    private static Path build02Wizard() throws Exception {
        Campaign campaign = loadCampaign("eberron");

        Character wizard = new Character("Mira Steelglint", Race.HALF_ELF, CharacterClass.WIZARD,
            8, 14, 14, 16, 12, 10);
        wizard.setHalfElfBonusAbilities(Ability.INTELLIGENCE, Ability.DEXTERITY);
        wizard.setLevel(5);
        // Spend the L4 ASI: +2 INT (Wizard's primary).
        if (wizard.getPendingAbilityScoreImprovements() > 0) {
            wizard.applyAbilityScoreImprovement(Ability.INTELLIGENCE);
        }
        wizard.setArcaneTradition(WizardFeatures.ArcaneTradition.EVOCATION);
        wizard.addSkillProficiency(Skill.ARCANA);
        wizard.addSkillProficiency(Skill.INVESTIGATION);
        wizard.addSkillProficiency(Skill.HISTORY);

        StandardEquipment eq = StandardEquipment.getInstance();
        Inventory inv = wizard.getInventory();
        inv.addItem(eq.getWeapon("quarterstaff"));
        inv.equipToSlot(eq.getWeapon("quarterstaff"), Inventory.EquipmentSlot.MAIN_HAND);
        inv.addGold(40);

        GameState state = new GameState(wizard, campaign);
        // Constructor already moves to starting location; no further move needed.

        healToFull(state.getCharacter());
        SaveState save = state.toSaveState();
        save.setSaveName("Mira (Wizard L5 Evocation) - Sculpt Spells, ready to cast");
        Path path = Path.of("saves", "demo", "02-wizard-l5-evocation.yaml");
        save.save(path);
        return path;
    }

    private static Path build03BardTrial() throws Exception {
        Campaign campaign = loadCampaign("muddlebrook");

        Character bard = new Character("Lirael Songweaver", Race.HALF_ELF, CharacterClass.BARD,
            8, 14, 12, 12, 10, 16);
        bard.setHalfElfBonusAbilities(Ability.DEXTERITY, Ability.INTELLIGENCE);
        bard.setLevel(3);
        bard.setBardCollege(BardFeatures.BardCollege.LORE);
        bard.addSkillProficiency(Skill.PERFORMANCE);
        bard.addSkillProficiency(Skill.PERSUASION);
        bard.addSkillProficiency(Skill.INVESTIGATION);
        bard.addSkillProficiency(Skill.SLEIGHT_OF_HAND);

        StandardEquipment eq = StandardEquipment.getInstance();
        Inventory inv = bard.getInventory();
        inv.addItem(eq.getWeapon("rapier"));
        inv.addItem(eq.getArmor("leather_armor"));
        inv.equipToSlot(eq.getWeapon("rapier"), Inventory.EquipmentSlot.MAIN_HAND);
        inv.equipToSlot(eq.getArmor("leather_armor"), Inventory.EquipmentSlot.ARMOR);
        inv.addGold(35);

        GameState state = new GameState(bard, campaign);
        state.setFlag("met_norrin", true);
        state.setFlag("met_darius", true);
        state.setFlag("started_trial_01", true);
        state.setFlag("completed_minigame_mechanical_frog", true);
        state.setFlag("completed_minigame_three_locks", true);
        state.setFlag("completed_minigame_backwards_clock", true);
        state.incrementCounter("clues_found");
        state.incrementCounter("clues_found");
        state.incrementCounter("clues_found");
        state.moveToLocation(campaign.getLocation("drunken_dragon_inn"));
        state.moveToLocation(campaign.getLocation("town_square"));
        state.moveToLocation(campaign.getLocation("town_hall"));
        state.moveToLocation(campaign.getLocation("mayors_office"));

        healToFull(state.getCharacter());
        SaveState save = state.toSaveState();
        save.setSaveName("Lirael (Bard L3 Lore) - Mayor's Office, 3 of 6 clues solved");
        Path path = Path.of("saves", "demo", "03-bard-l3-trial.yaml");
        save.save(path);
        return path;
    }

    private static Path build04BerserkerL9() throws Exception {
        Campaign campaign = loadCampaign("drownedgod");

        Character barb = new Character("Thrain Stormjaw", Race.HALF_ORC, CharacterClass.BARBARIAN,
            18, 14, 16, 8, 12, 10);
        barb.setLevel(9);
        // Pre-loaded post-ASI scores; clear pending so save round-trips cleanly.
        if (barb.getPendingAbilityScoreImprovements() > 0) {
            barb.setPendingAbilityScoreImprovements(0);
        }
        barb.addSkillProficiency(Skill.INTIMIDATION);
        barb.addSkillProficiency(Skill.ATHLETICS);
        barb.addSkillProficiency(Skill.PERCEPTION);
        barb.addSkillProficiency(Skill.SURVIVAL);

        StandardEquipment eq = StandardEquipment.getInstance();
        Inventory inv = barb.getInventory();
        inv.addItem(eq.getWeapon("greataxe"));
        inv.addItem(eq.getWeapon("javelin"));
        inv.addItem(eq.getWeapon("javelin"));
        inv.addItem(eq.getWeapon("javelin"));
        inv.equipToSlot(eq.getWeapon("greataxe"), Inventory.EquipmentSlot.MAIN_HAND);
        inv.addGold(220);

        GameState state = new GameState(barb, campaign);
        state.setFlag("survived_first_storm", true);

        healToFull(state.getCharacter());
        SaveState save = state.toSaveState();
        save.setSaveName("Thrain (Barbarian L9 Berserker) - Brutal Critical online");
        Path path = Path.of("saves", "demo", "04-barbarian-l9-brutal.yaml");
        save.save(path);
        return path;
    }

    /**
     * Tops the character to full HP. Called right before saving so a fresh
     * load drops the demo player at full strength.
     */
    private static void healToFull(Character c) {
        c.setCurrentHitPoints(c.getMaxHitPoints());
    }

    private static Campaign loadCampaign(String campaignId) throws Exception {
        List<CampaignInfo> all = Campaign.listAvailable();
        CampaignInfo info = all.stream()
            .filter(c -> c.id().equalsIgnoreCase(campaignId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Campaign not found: " + campaignId));
        return Campaign.loadFromYaml(info.path());
    }
}
