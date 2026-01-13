package com.questkeeper.campaign;

import com.questkeeper.character.NPC;
import com.questkeeper.combat.Monster;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.world.Location;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the Campaign class.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Campaign")
class CampaignTest {

    private static Campaign muddlebrook;

    @BeforeAll
    static void loadMuddlebrook() {
        // Load the actual Muddlebrook campaign for integration tests
        muddlebrook = Campaign.loadFromYaml(
            Path.of("src/main/resources/campaigns/muddlebrook"));
    }

    @Nested
    @DisplayName("loadFromYaml")
    class LoadFromYamlTests {

        @Test
        @DisplayName("loads campaign from path string")
        void loadsFromPathString() {
            Campaign campaign = Campaign.loadFromYaml(
                "src/main/resources/campaigns/muddlebrook");
            assertNotNull(campaign);
            assertEquals("muddlebrook", campaign.getId());
        }

        @Test
        @DisplayName("loads campaign from Path object")
        void loadsFromPath() {
            Campaign campaign = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
            assertNotNull(campaign);
            assertEquals("muddlebrook", campaign.getId());
        }

        @Test
        @DisplayName("throws exception for non-existent path")
        void throwsForNonExistentPath() {
            assertThrows(Campaign.CampaignLoadException.class, () ->
                Campaign.loadFromYaml("non/existent/path"));
        }

        @Test
        @DisplayName("throws exception for invalid campaign directory")
        void throwsForInvalidCampaign(@TempDir Path tempDir) {
            // Empty directory - no campaign.yaml
            assertThrows(Campaign.CampaignLoadException.class, () ->
                Campaign.loadFromYaml(tempDir));
        }
    }

    @Nested
    @DisplayName("Campaign Metadata")
    class MetadataTests {

        @Test
        @DisplayName("returns correct campaign ID")
        void returnsCampaignId() {
            assertEquals("muddlebrook", muddlebrook.getId());
        }

        @Test
        @DisplayName("returns correct campaign name")
        void returnsCampaignName() {
            assertEquals("Muddlebrook: Harlequin Trials", muddlebrook.getName());
        }

        @Test
        @DisplayName("returns campaign author")
        void returnsCampaignAuthor() {
            assertNotNull(muddlebrook.getAuthor());
            assertFalse(muddlebrook.getAuthor().isEmpty());
        }

        @Test
        @DisplayName("returns campaign version")
        void returnsCampaignVersion() {
            assertNotNull(muddlebrook.getVersion());
        }

        @Test
        @DisplayName("returns campaign description")
        void returnsCampaignDescription() {
            assertNotNull(muddlebrook.getDescription());
        }

        @Test
        @DisplayName("returns starting location ID")
        void returnsStartingLocationId() {
            assertNotNull(muddlebrook.getStartingLocationId());
            assertEquals("drunken_dragon_inn", muddlebrook.getStartingLocationId());
        }
    }

    @Nested
    @DisplayName("getLocation")
    class GetLocationTests {

        @Test
        @DisplayName("returns location by ID")
        void returnsLocationById() {
            Location location = muddlebrook.getLocation("drunken_dragon_inn");
            assertNotNull(location);
            assertEquals("drunken_dragon_inn", location.getId());
        }

        @Test
        @DisplayName("returns null for unknown location")
        void returnsNullForUnknown() {
            assertNull(muddlebrook.getLocation("nonexistent"));
        }

        @Test
        @DisplayName("returns starting location")
        void returnsStartingLocation() {
            Location starting = muddlebrook.getStartingLocation();
            assertNotNull(starting);
            assertEquals("drunken_dragon_inn", starting.getId());
        }

        @Test
        @DisplayName("returns all locations map")
        void returnsAllLocations() {
            Map<String, Location> locations = muddlebrook.getLocations();
            assertNotNull(locations);
            assertFalse(locations.isEmpty());
            assertTrue(locations.containsKey("drunken_dragon_inn"));
        }

        @Test
        @DisplayName("locations map is unmodifiable")
        void locationsMapUnmodifiable() {
            Map<String, Location> locations = muddlebrook.getLocations();
            assertThrows(UnsupportedOperationException.class, () ->
                locations.put("test", new Location("test", "Test")));
        }
    }

    @Nested
    @DisplayName("getNPC")
    class GetNPCTests {

        @Test
        @DisplayName("returns NPC by ID")
        void returnsNpcById() {
            NPC npc = muddlebrook.getNPC("norrin_bard");
            assertNotNull(npc);
            assertEquals("norrin_bard", npc.getId());
            assertEquals("Norrin", npc.getName());
        }

        @Test
        @DisplayName("returns null for unknown NPC")
        void returnsNullForUnknown() {
            assertNull(muddlebrook.getNPC("nonexistent"));
        }

        @Test
        @DisplayName("returns all NPCs map")
        void returnsAllNpcs() {
            Map<String, NPC> npcs = muddlebrook.getNPCs();
            assertNotNull(npcs);
            assertFalse(npcs.isEmpty());
            assertTrue(npcs.containsKey("norrin_bard"));
        }

        @Test
        @DisplayName("returns NPCs at location")
        void returnsNpcsAtLocation() {
            List<NPC> npcs = muddlebrook.getNPCsAtLocation("drunken_dragon_inn");
            assertNotNull(npcs);
            assertFalse(npcs.isEmpty());
            assertTrue(npcs.stream().anyMatch(npc -> "norrin_bard".equals(npc.getId())));
        }

        @Test
        @DisplayName("returns empty list for location with no NPCs")
        void returnsEmptyListForNoNpcs() {
            List<NPC> npcs = muddlebrook.getNPCsAtLocation("nonexistent_location");
            assertNotNull(npcs);
            assertTrue(npcs.isEmpty());
        }
    }

    @Nested
    @DisplayName("getTrial")
    class GetTrialTests {

        @Test
        @DisplayName("returns trial by ID")
        void returnsTrialById() {
            Trial trial = muddlebrook.getTrial("trial_01");
            assertNotNull(trial);
            assertEquals("trial_01", trial.getId());
        }

        @Test
        @DisplayName("returns null for unknown trial")
        void returnsNullForUnknown() {
            assertNull(muddlebrook.getTrial("nonexistent"));
        }

        @Test
        @DisplayName("returns all trials map")
        void returnsAllTrials() {
            Map<String, Trial> trials = muddlebrook.getTrials();
            assertNotNull(trials);
            assertFalse(trials.isEmpty());
        }

        @Test
        @DisplayName("returns trial at location")
        void returnsTrialAtLocation() {
            Trial trial = muddlebrook.getTrialAtLocation("mayors_office");
            assertNotNull(trial);
            assertEquals("trial_01", trial.getId());
        }

        @Test
        @DisplayName("returns null for location without trial")
        void returnsNullForNoTrial() {
            Trial trial = muddlebrook.getTrialAtLocation("drunken_dragon_inn");
            assertNull(trial);
        }
    }

    @Nested
    @DisplayName("getItem")
    class GetItemTests {

        @Test
        @DisplayName("returns general item by ID")
        void returnsGeneralItemById() {
            Item item = muddlebrook.getItem("gear_grease");
            assertNotNull(item);
            assertEquals("Gear Grease", item.getName());
        }

        @Test
        @DisplayName("returns weapon via getItem")
        void returnsWeaponViaGetItem() {
            Item item = muddlebrook.getItem("clockwork_dagger");
            assertNotNull(item);
            assertTrue(item instanceof Weapon);
        }

        @Test
        @DisplayName("returns armor via getItem")
        void returnsArmorViaGetItem() {
            Item item = muddlebrook.getItem("clockwork_leather");
            assertNotNull(item);
            assertTrue(item instanceof Armor);
        }

        @Test
        @DisplayName("returns null for unknown item")
        void returnsNullForUnknown() {
            assertNull(muddlebrook.getItem("nonexistent"));
        }

        @Test
        @DisplayName("returns weapon by ID")
        void returnsWeaponById() {
            Weapon weapon = muddlebrook.getWeapon("clockwork_dagger");
            assertNotNull(weapon);
            assertEquals("Clockwork Dagger", weapon.getName());
        }

        @Test
        @DisplayName("returns armor by ID")
        void returnsArmorById() {
            Armor armor = muddlebrook.getArmor("clockwork_leather");
            assertNotNull(armor);
            assertEquals("Clockwork Leather Armor", armor.getName());
        }

        @Test
        @DisplayName("returns all items maps")
        void returnsAllItemsMaps() {
            assertFalse(muddlebrook.getItems().isEmpty());
            assertFalse(muddlebrook.getWeapons().isEmpty());
            assertFalse(muddlebrook.getArmors().isEmpty());
        }
    }

    @Nested
    @DisplayName("Monster Templates")
    class MonsterTests {

        @Test
        @DisplayName("returns monster template by ID")
        void returnsMonsterTemplateById() {
            Monster template = muddlebrook.getMonsterTemplate("clockwork_critter");
            assertNotNull(template);
            assertEquals("Clockwork Critter", template.getName());
        }

        @Test
        @DisplayName("creates monster instance from template")
        void createsMonsterInstance() {
            Monster instance = muddlebrook.createMonster("clockwork_critter", "critter_001");
            assertNotNull(instance);
            assertEquals("critter_001", instance.getId());
            assertEquals("Clockwork Critter", instance.getName());
        }

        @Test
        @DisplayName("creates monster with auto-generated ID")
        void createsMonsterAutoId() {
            Monster instance = muddlebrook.createMonster("clockwork_critter");
            assertNotNull(instance);
            assertTrue(instance.getId().startsWith("clockwork_critter_"));
        }

        @Test
        @DisplayName("returns null for unknown template")
        void returnsNullForUnknown() {
            assertNull(muddlebrook.createMonster("nonexistent"));
        }

        @Test
        @DisplayName("monster instances are independent")
        void monsterInstancesIndependent() {
            Monster m1 = muddlebrook.createMonster("clockwork_critter", "critter_1");
            Monster m2 = muddlebrook.createMonster("clockwork_critter", "critter_2");

            m1.takeDamage(5);

            assertNotEquals(m1.getCurrentHitPoints(), m2.getCurrentHitPoints());
        }

        @Test
        @DisplayName("returns all monster templates")
        void returnsAllTemplates() {
            Map<String, Monster> templates = muddlebrook.getMonsterTemplates();
            assertNotNull(templates);
            assertFalse(templates.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMiniGame")
    class GetMiniGameTests {

        @Test
        @DisplayName("returns mini-game by ID")
        void returnsMiniGameById() {
            MiniGame miniGame = muddlebrook.getMiniGame("backwards_clock");
            assertNotNull(miniGame);
            assertEquals("backwards_clock", miniGame.getId());
        }

        @Test
        @DisplayName("returns null for unknown mini-game")
        void returnsNullForUnknown() {
            assertNull(muddlebrook.getMiniGame("nonexistent"));
        }

        @Test
        @DisplayName("returns all mini-games map")
        void returnsAllMiniGames() {
            Map<String, MiniGame> miniGames = muddlebrook.getMiniGames();
            assertNotNull(miniGames);
            assertFalse(miniGames.isEmpty());
        }
    }

    @Nested
    @DisplayName("Cross-Reference Validation")
    class ValidationTests {

        @Test
        @DisplayName("validates location exits")
        void validatesLocationExits() {
            // Muddlebrook should have valid exit references
            for (Location location : muddlebrook.getLocations().values()) {
                for (String direction : location.getExits()) {
                    String targetId = location.getExit(direction);
                    assertNotNull(muddlebrook.getLocation(targetId),
                        String.format("Location '%s' exit '%s' points to non-existent '%s'",
                            location.getId(), direction, targetId));
                }
            }
        }

        @Test
        @DisplayName("validates trial mini-game references")
        void validatesTrialMiniGames() {
            for (Trial trial : muddlebrook.getTrials().values()) {
                for (MiniGame miniGame : trial.getMiniGames()) {
                    assertNotNull(muddlebrook.getMiniGame(miniGame.getId()),
                        String.format("Trial '%s' has unregistered mini-game '%s'",
                            trial.getId(), miniGame.getId()));
                }
            }
        }

        @Test
        @DisplayName("validates NPC location references")
        void validatesNpcLocations() {
            for (NPC npc : muddlebrook.getNPCs().values()) {
                String locationId = npc.getLocationId();
                if (locationId != null) {
                    assertNotNull(muddlebrook.getLocation(locationId),
                        String.format("NPC '%s' references non-existent location '%s'",
                            npc.getId(), locationId));
                }
            }
        }

        @Test
        @DisplayName("validates trial location references")
        void validatesTrialLocations() {
            for (Trial trial : muddlebrook.getTrials().values()) {
                String locationId = trial.getLocation();
                if (locationId != null) {
                    assertNotNull(muddlebrook.getLocation(locationId),
                        String.format("Trial '%s' references non-existent location '%s'",
                            trial.getId(), locationId));
                }
            }
        }

        @Test
        @DisplayName("detects invalid references in test campaign")
        void detectsInvalidReferences(@TempDir Path tempDir) throws IOException {
            // Create a campaign with invalid references
            createCampaignYaml(tempDir);
            createLocationsWithBadExit(tempDir);

            Campaign campaign = Campaign.loadFromYaml(tempDir);

            assertTrue(campaign.hasValidationErrors());
            assertTrue(campaign.getValidationErrors().stream()
                .anyMatch(e -> e.contains("nonexistent_room")));
        }

        private void createCampaignYaml(Path dir) throws IOException {
            String yaml = """
                id: test_campaign
                name: Test Campaign
                author: Test
                version: 1.0
                starting_location: start_room
                """;
            Files.writeString(dir.resolve("campaign.yaml"), yaml);
        }

        private void createLocationsWithBadExit(Path dir) throws IOException {
            String yaml = """
                locations:
                  - id: start_room
                    name: Start Room
                    exits:
                      north: nonexistent_room
                """;
            Files.writeString(dir.resolve("locations.yaml"), yaml);
        }
    }

    @Nested
    @DisplayName("Summary and toString")
    class SummaryTests {

        @Test
        @DisplayName("returns campaign summary")
        void returnsSummary() {
            String summary = muddlebrook.getSummary();
            assertNotNull(summary);
            assertTrue(summary.contains("Muddlebrook"));
            assertTrue(summary.contains("Locations:"));
            assertTrue(summary.contains("NPCs:"));
        }

        @Test
        @DisplayName("toString includes campaign info")
        void toStringIncludesInfo() {
            String str = muddlebrook.toString();
            assertTrue(str.contains("muddlebrook"));
            assertTrue(str.contains("Muddlebrook"));
        }
    }

    @Nested
    @DisplayName("Muddlebrook Integration")
    class MuddlebrookIntegrationTests {

        @Test
        @DisplayName("has expected number of locations")
        void hasExpectedLocations() {
            assertTrue(muddlebrook.getLocations().size() >= 10,
                "Expected at least 10 locations");
        }

        @Test
        @DisplayName("has expected NPCs")
        void hasExpectedNpcs() {
            assertNotNull(muddlebrook.getNPC("norrin_bard"));
            assertNotNull(muddlebrook.getNPC("mara_bartender"));
            assertNotNull(muddlebrook.getNPC("darius_recluse"));
            assertNotNull(muddlebrook.getNPC("elara_shopkeeper"));
            assertNotNull(muddlebrook.getNPC("mayor_alderwick"));
        }

        @Test
        @DisplayName("has expected monsters")
        void hasExpectedMonsters() {
            assertNotNull(muddlebrook.getMonsterTemplate("clockwork_critter"));
            assertNotNull(muddlebrook.getMonsterTemplate("confetti_ooze"));
            assertNotNull(muddlebrook.getMonsterTemplate("harlequin_machinist"));
        }

        @Test
        @DisplayName("has expected trials")
        void hasExpectedTrials() {
            assertNotNull(muddlebrook.getTrial("trial_01"));
            assertNotNull(muddlebrook.getTrial("trial_02"));
            assertNotNull(muddlebrook.getTrial("trial_03"));
        }

        @Test
        @DisplayName("trial 01 has correct mini-games")
        void trial01HasMiniGames() {
            Trial trial = muddlebrook.getTrial("trial_01");
            assertNotNull(trial);
            assertTrue(trial.getMiniGames().size() >= 5,
                "Trial 01 should have at least 5 mini-games");
        }

        @Test
        @DisplayName("no validation errors in Muddlebrook")
        void noValidationErrors() {
            if (muddlebrook.hasValidationErrors()) {
                fail("Muddlebrook has validation errors: " +
                    String.join(", ", muddlebrook.getValidationErrors()));
            }
        }
    }
}
