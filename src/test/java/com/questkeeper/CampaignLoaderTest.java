package com.questkeeper;

import com.questkeeper.character.NPC;
import com.questkeeper.combat.Monster;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.campaign.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CampaignLoader class.
 * 
 * @author Marc McGough
 */
@DisplayName("CampaignLoader Tests")
class CampaignLoaderTest {

    @TempDir
    Path tempDir;

    private Path campaignDir;
    private CampaignLoader loader;

    @BeforeEach
    void setUp() throws IOException {
        campaignDir = tempDir.resolve("test_campaign");
        Files.createDirectories(campaignDir);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private void createCampaignYaml(String content) throws IOException {
        Files.writeString(campaignDir.resolve("campaign.yaml"), content);
    }

    private void createMonstersYaml(String content) throws IOException {
        Files.writeString(campaignDir.resolve("monsters.yaml"), content);
    }

    private void createNPCsYaml(String content) throws IOException {
        Files.writeString(campaignDir.resolve("npcs.yaml"), content);
    }

    private void createItemsYaml(String content) throws IOException {
        Files.writeString(campaignDir.resolve("items.yaml"), content);
    }

    private void createMinimalCampaign() throws IOException {
        createCampaignYaml("""
            id: test
            name: Test Campaign
            description: A test campaign
            author: Test Author
            version: "1.0"
            starting_location: start
            """);
    }

    // ==========================================
    // Campaign Metadata Tests
    // ==========================================

    @Nested
    @DisplayName("Campaign Metadata Loading")
    class MetadataTests {

        @Test
        @DisplayName("loads campaign metadata successfully")
        void loadsCampaignMetadata() throws IOException {
            createCampaignYaml("""
                id: muddlebrook
                name: "Muddlebrook: Harlequin Trials"
                description: A comedic mystery adventure
                author: Andrew Mariani
                version: "1.0"
                starting_location: drunken_dragon_inn
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            assertEquals("muddlebrook", loader.getCampaignId());
            assertEquals("Muddlebrook: Harlequin Trials", loader.getCampaignName());
            assertEquals("A comedic mystery adventure", loader.getCampaignDescription());
            assertEquals("Andrew Mariani", loader.getCampaignAuthor());
            assertEquals("1.0", loader.getCampaignVersion());
            assertEquals("drunken_dragon_inn", loader.getStartingLocationId());
        }

        @Test
        @DisplayName("fails when campaign.yaml is missing")
        void failsWithoutCampaignYaml() {
            loader = new CampaignLoader(campaignDir);
            assertFalse(loader.load());
            assertTrue(loader.getLoadErrors().stream()
                    .anyMatch(e -> e.contains("Missing required file")));
        }

        @Test
        @DisplayName("fails when campaign directory doesn't exist")
        void failsWithNonexistentDirectory() {
            loader = new CampaignLoader(tempDir.resolve("nonexistent"));
            assertFalse(loader.load());
            assertTrue(loader.getLoadErrors().stream()
                    .anyMatch(e -> e.contains("Campaign directory not found")));
        }

        @Test
        @DisplayName("uses default values for missing fields")
        void usesDefaultValues() throws IOException {
            createCampaignYaml("""
                id: minimal
                name: Minimal Campaign
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            assertEquals("minimal", loader.getCampaignId());
            assertEquals("Minimal Campaign", loader.getCampaignName());
            assertEquals("", loader.getCampaignDescription());
            assertEquals("Unknown", loader.getCampaignAuthor());
            assertEquals("1.0", loader.getCampaignVersion());
            assertNull(loader.getStartingLocationId());
        }
    }

    // ==========================================
    // Monster Loading Tests
    // ==========================================

    @Nested
    @DisplayName("Monster Loading")
    class MonsterTests {

        @BeforeEach
        void setUpCampaign() throws IOException {
            createMinimalCampaign();
        }

        @Test
        @DisplayName("loads monsters from YAML")
        void loadsMonsters() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: clockwork_critter
                    name: Clockwork Critter
                    size: SMALL
                    type: CONSTRUCT
                    alignment: neutral evil
                    armor_class: 13
                    hit_points: 11
                    speed: 30
                    challenge_rating: 0.25
                    xp: 50
                    attack_bonus: 4
                    damage_dice: "1d6+2"
                    abilities:
                      str: -1
                      dex: 2
                      con: 2
                      int: -4
                      wis: 0
                      cha: -4
                    description: A skittering mechanical creature.
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            assertEquals(1, loader.getMonsterTemplates().size());
            assertTrue(loader.getMonsterTemplates().containsKey("clockwork_critter"));
        }

        @Test
        @DisplayName("creates monster instances from templates")
        void createsMonsterInstances() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: goblin
                    name: Goblin
                    armor_class: 15
                    hit_points: 7
                    attack_bonus: 4
                    damage_dice: "1d6+2"
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            Optional<Monster> monster1 = loader.createMonster("goblin", "goblin_1");
            Optional<Monster> monster2 = loader.createMonster("goblin", "goblin_2");

            assertTrue(monster1.isPresent());
            assertTrue(monster2.isPresent());
            assertEquals("goblin_1", monster1.get().getId());
            assertEquals("goblin_2", monster2.get().getId());
            assertEquals("Goblin", monster1.get().getName());
            assertEquals(15, monster1.get().getArmorClass());
        }

        @Test
        @DisplayName("monster instances are independent")
        void monsterInstancesAreIndependent() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: orc
                    name: Orc
                    armor_class: 13
                    hit_points: 15
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            Monster orc1 = loader.createMonster("orc", "orc_1").orElseThrow();
            Monster orc2 = loader.createMonster("orc", "orc_2").orElseThrow();

            orc1.takeDamage(10);

            assertEquals(5, orc1.getCurrentHitPoints());
            assertEquals(15, orc2.getCurrentHitPoints()); // Unaffected
        }

        @Test
        @DisplayName("returns empty for unknown monster template")
        void returnsEmptyForUnknownMonster() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: known_monster
                    name: Known Monster
                    armor_class: 10
                    hit_points: 10
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            Optional<Monster> unknown = loader.createMonster("unknown_monster");
            assertTrue(unknown.isEmpty());
        }

        @Test
        @DisplayName("loads without monsters.yaml")
        void loadsWithoutMonstersFile() throws IOException {
            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());
            assertTrue(loader.getMonsterTemplates().isEmpty());
        }

        @Test
        @DisplayName("parses challenge rating fractions correctly")
        void parsesChallengeRating() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: weak_monster
                    name: Weak Monster
                    armor_class: 10
                    hit_points: 5
                    challenge_rating: 0.25
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            Monster monster = loader.createMonster("weak_monster").orElseThrow();
            assertEquals(0.25, monster.getChallengeRating());
        }
    }

    // ==========================================
    // NPC Loading Tests
    // ==========================================

    @Nested
    @DisplayName("NPC Loading")
    class NPCTests {

        @BeforeEach
        void setUpCampaign() throws IOException {
            createMinimalCampaign();
        }

        @Test
        @DisplayName("loads NPCs from YAML")
        void loadsNPCs() throws IOException {
            createNPCsYaml("""
                npcs:
                  - id: norrin_bard
                    name: Norrin
                    role: bard
                    voice: "sing-song, theatrical"
                    personality: "friendly, dramatic"
                    description: A flamboyant half-elf with a lute.
                    location_id: drunken_dragon_inn
                    greeting: "Ah! New faces—new verses!"
                    return_greeting: "My favorite audience returns!"
                    dialogues:
                      mayor: "The mayor? That's a song still being written."
                      town: "Muddlebrook! Where the mud is deep."
                    sample_lines:
                      - "Ah! New faces—new verses!"
                      - "*strums a chord* That reminds me of a ballad..."
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            Optional<NPC> norrin = loader.getNPC("norrin_bard");
            assertTrue(norrin.isPresent());
            assertEquals("Norrin", norrin.get().getName());
            assertEquals("bard", norrin.get().getRole());
            assertEquals("drunken_dragon_inn", norrin.get().getLocationId());
            assertTrue(norrin.get().hasDialogue("mayor"));
            assertTrue(norrin.get().hasDialogue("town"));
            assertEquals(2, norrin.get().getSampleLines().size());
        }

        @Test
        @DisplayName("finds NPCs by location")
        void findsNPCsByLocation() throws IOException {
            createNPCsYaml("""
                npcs:
                  - id: npc1
                    name: NPC One
                    location_id: tavern
                  - id: npc2
                    name: NPC Two
                    location_id: tavern
                  - id: npc3
                    name: NPC Three
                    location_id: shop
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            List<NPC> tavernNPCs = loader.getNPCsAtLocation("tavern");
            assertEquals(2, tavernNPCs.size());

            List<NPC> shopNPCs = loader.getNPCsAtLocation("shop");
            assertEquals(1, shopNPCs.size());

            List<NPC> emptyLocation = loader.getNPCsAtLocation("nowhere");
            assertTrue(emptyLocation.isEmpty());
        }

        @Test
        @DisplayName("marks shopkeeper NPCs correctly")
        void marksShopkeeperNPCs() throws IOException {
            createNPCsYaml("""
                npcs:
                  - id: merchant
                    name: Merchant
                    shopkeeper: true
                  - id: villager
                    name: Villager
                    shopkeeper: false
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            assertTrue(loader.getNPC("merchant").get().isShopkeeper());
            assertFalse(loader.getNPC("villager").get().isShopkeeper());
        }
    }

    // ==========================================
    // Item Loading Tests
    // ==========================================

    @Nested
    @DisplayName("Item Loading")
    class ItemTests {

        @BeforeEach
        void setUpCampaign() throws IOException {
            createMinimalCampaign();
        }

        @Test
        @DisplayName("loads weapons from YAML")
        void loadsWeapons() throws IOException {
            createItemsYaml("""
                weapons:
                  - id: clockwork_dagger
                    name: Clockwork Dagger
                    damage_dice_count: 1
                    damage_die_size: 4
                    damage_type: PIERCING
                    category: SIMPLE_MELEE
                    weight: 1.0
                    value: 50
                    normal_range: 20
                    long_range: 60
                    properties:
                      - FINESSE
                      - LIGHT
                      - THROWN
                    rarity: UNCOMMON
                    attack_bonus: 1
                    description: A dagger with tiny gears.
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            Optional<Weapon> weapon = loader.getWeapon("clockwork_dagger");
            assertTrue(weapon.isPresent());
            assertEquals("Clockwork Dagger", weapon.get().getName());
            assertEquals(1, weapon.get().getAttackBonus());
            assertTrue(weapon.get().isFinesse());
            assertTrue(weapon.get().isLight());
            assertTrue(weapon.get().isThrown());
            assertEquals(Item.Rarity.UNCOMMON, weapon.get().getRarity());
        }

        @Test
        @DisplayName("loads armor from YAML")
        void loadsArmor() throws IOException {
            createItemsYaml("""
                armor:
                  - id: clockwork_leather
                    name: Clockwork Leather Armor
                    base_ac: 12
                    category: LIGHT
                    weight: 12.0
                    value: 200
                    rarity: UNCOMMON
                    description: Leather armor with brass plates.
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            Optional<Armor> armor = loader.getArmor("clockwork_leather");
            assertTrue(armor.isPresent());
            assertEquals("Clockwork Leather Armor", armor.get().getName());
            assertEquals(12, armor.get().getBaseAC());
            assertEquals(Armor.ArmorCategory.LIGHT, armor.get().getCategory());
        }

        @Test
        @DisplayName("loads generic items from YAML")
        void loadsGenericItems() throws IOException {
            createItemsYaml("""
                items:
                  - id: mayors_journal
                    name: Mayor's Journal
                    type: QUEST_ITEM
                    description: A leather-bound journal.
                    weight: 0.5
                    value: 0
                    quest_item: true
                  - id: gear_grease
                    name: Gear Grease
                    type: CONSUMABLE
                    description: Specialized lubricant.
                    weight: 0.5
                    value: 25
                    stackable: true
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());

            Optional<Item> journal = loader.getItem("mayors_journal");
            assertTrue(journal.isPresent());
            assertTrue(journal.get().isQuestItem());

            Optional<Item> grease = loader.getItem("gear_grease");
            assertTrue(grease.isPresent());
            assertTrue(grease.get().isStackable());
        }

        @Test
        @DisplayName("getAnyItem searches all item types")
        void getAnyItemSearchesAllTypes() throws IOException {
            createItemsYaml("""
                weapons:
                  - id: sword
                    name: Sword
                    damage_dice_count: 1
                    damage_die_size: 8
                    damage_type: SLASHING
                    category: MARTIAL_MELEE
                    weight: 3.0
                    value: 15
                armor:
                  - id: shield
                    name: Shield
                    base_ac: 2
                    category: SHIELD
                    weight: 6.0
                    value: 10
                items:
                  - id: potion
                    name: Potion
                    type: CONSUMABLE
                    weight: 0.5
                    value: 50
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            assertTrue(loader.getAnyItem("sword").isPresent());
            assertTrue(loader.getAnyItem("shield").isPresent());
            assertTrue(loader.getAnyItem("potion").isPresent());
            assertTrue(loader.getAnyItem("nonexistent").isEmpty());
        }
    }

    // ==========================================
    // Error Handling Tests
    // ==========================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @BeforeEach
        void setUpCampaign() throws IOException {
            createMinimalCampaign();
        }

        @Test
        @DisplayName("records errors for invalid monster data")
        void recordsMonsterErrors() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: bad_monster
                    name: Bad Monster
                    armor_class: 10
                    hit_points: 10
                    size: INVALID_SIZE
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            assertTrue(loader.hasErrors());
            assertTrue(loader.getLoadErrors().stream()
                    .anyMatch(e -> e.contains("Invalid size")));
        }

        @Test
        @DisplayName("continues loading after non-fatal errors")
        void continuesAfterErrors() throws IOException {
            createMonstersYaml("""
                monsters:
                  - id: good_monster
                    name: Good Monster
                    armor_class: 10
                    hit_points: 10
                  - id: bad_monster
                    name: Bad Monster
                    armor_class: 10
                    hit_points: 10
                    type: INVALID_TYPE
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load()); // Still returns true

            // Good monster should be loaded
            assertEquals(1, loader.getMonsterTemplates().size());
            assertTrue(loader.getMonsterTemplates().containsKey("bad_monster")); // Loaded with default type
            assertTrue(loader.hasErrors());
        }

        @Test
        @DisplayName("handles empty YAML files gracefully")
        void handlesEmptyFiles() throws IOException {
            createMonstersYaml("");
            createNPCsYaml("");
            createItemsYaml("");

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());
            assertTrue(loader.getMonsterTemplates().isEmpty());
            assertTrue(loader.getAllNPCs().isEmpty());
            assertTrue(loader.getAllItems().isEmpty());
        }
    }

    // ==========================================
    // Load Summary Tests
    // ==========================================

    @Nested
    @DisplayName("Load Summary")
    class LoadSummaryTests {

        @Test
        @DisplayName("generates accurate load summary")
        void generatesLoadSummary() throws IOException {
            createCampaignYaml("""
                id: test
                name: Test Campaign
                author: Test Author
                version: "2.0"
                """);
            createMonstersYaml("""
                monsters:
                  - id: monster1
                    name: Monster One
                    armor_class: 10
                    hit_points: 10
                  - id: monster2
                    name: Monster Two
                    armor_class: 10
                    hit_points: 10
                """);
            createNPCsYaml("""
                npcs:
                  - id: npc1
                    name: NPC One
                """);

            loader = new CampaignLoader(campaignDir);
            loader.load();

            String summary = loader.getLoadSummary();
            assertTrue(summary.contains("Test Campaign"));
            assertTrue(summary.contains("v2.0"));
            assertTrue(summary.contains("Test Author"));
            assertTrue(summary.contains("Monsters: 2"));
            assertTrue(summary.contains("NPCs: 1"));
        }
    }

    // ==========================================
    // Integration Tests
    // ==========================================

    @Nested
    @DisplayName("Full Campaign Loading")
    class IntegrationTests {

        @Test
        @DisplayName("loads complete campaign with all content types")
        void loadsCompleteCampaign() throws IOException {
            createCampaignYaml("""
                id: integration_test
                name: Integration Test Campaign
                description: Tests all content loading
                author: Test
                version: "1.0"
                starting_location: start
                """);

            createMonstersYaml("""
                monsters:
                  - id: test_monster
                    name: Test Monster
                    size: MEDIUM
                    type: BEAST
                    armor_class: 12
                    hit_points: 20
                    speed: 40
                    challenge_rating: 1
                    xp: 200
                    attack_bonus: 4
                    damage_dice: "1d8+2"
                    abilities:
                      str: 2
                      dex: 1
                      con: 1
                      int: -4
                      wis: 1
                      cha: -2
                """);

            createNPCsYaml("""
                npcs:
                  - id: test_npc
                    name: Test NPC
                    role: innkeeper
                    location_id: start
                    shopkeeper: true
                    greeting: Welcome!
                    dialogues:
                      help: How can I help you?
                """);

            createItemsYaml("""
                weapons:
                  - id: test_sword
                    name: Test Sword
                    damage_dice_count: 1
                    damage_die_size: 8
                    damage_type: SLASHING
                    category: MARTIAL_MELEE
                    weight: 3.0
                    value: 15
                    properties:
                      - VERSATILE
                    versatile_die_size: 10
                armor:
                  - id: test_armor
                    name: Test Armor
                    base_ac: 14
                    category: MEDIUM
                    weight: 20.0
                    value: 50
                items:
                  - id: test_item
                    name: Test Item
                    type: CONSUMABLE
                    weight: 0.5
                    value: 10
                    stackable: true
                """);

            loader = new CampaignLoader(campaignDir);
            assertTrue(loader.load());
            assertTrue(loader.isLoaded());
            assertFalse(loader.hasErrors());

            // Verify all content loaded
            assertEquals(1, loader.getMonsterTemplates().size());
            assertEquals(1, loader.getAllNPCs().size());
            assertEquals(1, loader.getAllWeapons().size());
            assertEquals(1, loader.getAllArmor().size());
            assertEquals(1, loader.getAllItems().size());

            // Verify content details
            Monster monster = loader.createMonster("test_monster").orElseThrow();
            assertEquals("Test Monster", monster.getName());
            assertEquals(Monster.Size.MEDIUM, monster.getSize());
            assertEquals(Monster.MonsterType.BEAST, monster.getType());

            NPC npc = loader.getNPC("test_npc").orElseThrow();
            assertEquals("Test NPC", npc.getName());
            assertTrue(npc.isShopkeeper());
            assertTrue(npc.hasDialogue("help"));

            Weapon weapon = loader.getWeapon("test_sword").orElseThrow();
            assertTrue(weapon.isVersatile());

            Armor armor = loader.getArmor("test_armor").orElseThrow();
            assertEquals(Armor.ArmorCategory.MEDIUM, armor.getCategory());

            Item item = loader.getItem("test_item").orElseThrow();
            assertTrue(item.isStackable());
        }
    }
}

