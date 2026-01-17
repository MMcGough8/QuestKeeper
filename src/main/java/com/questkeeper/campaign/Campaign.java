package com.questkeeper.campaign;

import com.questkeeper.character.NPC;
import com.questkeeper.combat.Monster;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.world.Location;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Represents a loaded campaign with all its content.
 *
 * Provides convenient access to all campaign entities (locations, NPCs, monsters,
 * items, trials, mini-games) and validates cross-references between them on load.
 *
 * Usage:
 * <pre>
 * Campaign campaign = Campaign.loadFromYaml("campaigns/muddlebrook");
 * Location tavern = campaign.getLocation("drunken_dragon_inn");
 * NPC norrin = campaign.getNPC("norrin_bard");
 * </pre>
 *
 * @author Marc McGough
 * @version 1.0
 */
public class Campaign {

    // Campaign metadata
    private final String id;
    private final String name;
    private final String description;
    private final String intro;
    private final String author;
    private final String version;
    private final String startingLocationId;

    // Content maps
    private final Map<String, Location> locations;
    private final Map<String, NPC> npcs;
    private final Map<String, Monster> monsterTemplates;
    private final Map<String, Item> items;
    private final Map<String, Weapon> weapons;
    private final Map<String, Armor> armors;
    private final Map<String, Trial> trials;
    private final Map<String, MiniGame> miniGames;

    // Validation state
    private final List<String> validationErrors;

    /**
     * Private constructor - use loadFromYaml() factory method.
     */
    private Campaign(CampaignLoader loader) {
        this.id = loader.getCampaignId();
        this.name = loader.getCampaignName();
        this.description = loader.getCampaignDescription();
        this.intro = loader.getCampaignIntro();
        this.author = loader.getCampaignAuthor();
        this.version = loader.getCampaignVersion();
        this.startingLocationId = loader.getStartingLocationId();

        // Copy maps from loader
        this.locations = new HashMap<>(loader.getAllLocations());
        this.npcs = new HashMap<>(loader.getAllNPCs());
        this.monsterTemplates = new HashMap<>(loader.getMonsterTemplates());
        this.items = new HashMap<>(loader.getAllItems());
        this.weapons = new HashMap<>(loader.getAllWeapons());
        this.armors = new HashMap<>(loader.getAllArmor());
        this.trials = new HashMap<>(loader.getAllTrials());
        this.miniGames = new HashMap<>(loader.getAllMiniGames());

        // Copy any load errors from loader
        this.validationErrors = new ArrayList<>(loader.getLoadErrors());
    }

    /**
     * Loads a campaign from a YAML directory.
     */
    public static Campaign loadFromYaml(String campaignPath) {
        return loadFromYaml(Path.of(campaignPath));
    }

    /**
     * Loads a campaign from a YAML directory.
     */
    public static Campaign loadFromYaml(Path campaignPath) {
        CampaignLoader loader = new CampaignLoader(campaignPath);

        if (!loader.load()) {
            throw new CampaignLoadException(
                "Failed to load campaign from: " + campaignPath,
                loader.getLoadErrors()
            );
        }

        Campaign campaign = new Campaign(loader);
        campaign.validateCrossReferences();

        return campaign;
    }

    /**
     * Discovers all available campaigns in the given campaigns directory.
     * Only reads campaign.yaml metadata without loading full campaign content.
     *
     * @param campaignsDir the directory containing campaign subdirectories
     * @return list of CampaignInfo objects for available campaigns
     */
    public static List<CampaignInfo> listAvailable(Path campaignsDir) {
        List<CampaignInfo> campaigns = new ArrayList<>();

        if (!Files.exists(campaignsDir) || !Files.isDirectory(campaignsDir)) {
            return campaigns;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(campaignsDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    CampaignInfo info = readCampaignInfo(entry);
                    if (info != null) {
                        campaigns.add(info);
                    }
                }
            }
        } catch (IOException e) {
            // Return whatever we found so far
        }

        // Sort by name for consistent display
        campaigns.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return campaigns;
    }

    /**
     * Discovers available campaigns by searching common locations.
     *
     * @return list of CampaignInfo objects for available campaigns
     */
    public static List<CampaignInfo> listAvailable() {
        // Try multiple possible campaign directory locations
        String[] possiblePaths = {
            "src/main/resources/campaigns",
            "campaigns",
            "../resources/campaigns"
        };

        for (String pathStr : possiblePaths) {
            Path path = Path.of(pathStr);
            if (Files.exists(path) && Files.isDirectory(path)) {
                List<CampaignInfo> campaigns = listAvailable(path);
                if (!campaigns.isEmpty()) {
                    return campaigns;
                }
            }
        }

        return new ArrayList<>();
    }

    /**
     * Reads just the campaign.yaml metadata without loading full content.
     */
    @SuppressWarnings("unchecked")
    private static CampaignInfo readCampaignInfo(Path campaignDir) {
        Path campaignFile = campaignDir.resolve("campaign.yaml");

        if (!Files.exists(campaignFile)) {
            return null;
        }

        try (InputStream is = Files.newInputStream(campaignFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);

            if (data == null) {
                return null;
            }

            String id = getStringOrDefault(data, "id", campaignDir.getFileName().toString());
            String name = getStringOrDefault(data, "name", "Unnamed Campaign");
            String description = getStringOrDefault(data, "description", "");
            String author = getStringOrDefault(data, "author", "Unknown");
            String version = getStringOrDefault(data, "version", "1.0");

            return new CampaignInfo(id, name, description, author, version, campaignDir);
        } catch (IOException e) {
            return null;
        }
    }

    private static String getStringOrDefault(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value instanceof String s) {
            return s;
        }
        return defaultValue;
    }

    /**
     * Validates all cross-references between campaign entities.
     * Checks that:
     * - All location exits point to valid locations
     * - All NPCs referenced in locations exist
     * - All items referenced in locations exist
     * - All trial location references are valid
     * - All mini-game reward items exist
     * - All NPC location references are valid
     * - Starting location exists
     */
    private void validateCrossReferences() {
        validateStartingLocation();
        validateLocationExits();
        validateLocationNpcs();
        validateLocationItems();
        validateNpcLocations();
        validateTrialLocations();
        validateTrialMiniGames();
        validateMiniGameRewards();
    }

    private void validateStartingLocation() {
        if (startingLocationId != null && !locations.containsKey(startingLocationId)) {
            validationErrors.add(String.format(
                "Starting location '%s' does not exist", startingLocationId));
        }
    }

    private void validateLocationExits() {
        for (Location location : locations.values()) {
            for (String direction : location.getExits()) {
                String targetId = location.getExit(direction);
                if (!locations.containsKey(targetId)) {
                    validationErrors.add(String.format(
                        "Location '%s' exit '%s' references unknown location '%s'",
                        location.getId(), direction, targetId));
                }
            }
        }
    }

    private void validateLocationNpcs() {
        for (Location location : locations.values()) {
            for (String npcId : location.getNpcs()) {
                if (!npcs.containsKey(npcId)) {
                    validationErrors.add(String.format(
                        "Location '%s' references unknown NPC '%s'",
                        location.getId(), npcId));
                }
            }
        }
    }

    private void validateLocationItems() {
        for (Location location : locations.values()) {
            for (String itemId : location.getItems()) {
                if (!itemExists(itemId)) {
                    validationErrors.add(String.format(
                        "Location '%s' references unknown item '%s'",
                        location.getId(), itemId));
                }
            }
        }
    }

    private void validateNpcLocations() {
        for (NPC npc : npcs.values()) {
            String locationId = npc.getLocationId();
            if (locationId != null && !locations.containsKey(locationId)) {
                validationErrors.add(String.format(
                    "NPC '%s' references unknown location '%s'",
                    npc.getId(), locationId));
            }
        }
    }

    private void validateTrialLocations() {
        for (Trial trial : trials.values()) {
            String locationId = trial.getLocation();
            if (locationId != null && !locations.containsKey(locationId)) {
                validationErrors.add(String.format(
                    "Trial '%s' references unknown location '%s'",
                    trial.getId(), locationId));
            }
        }
    }

    private void validateTrialMiniGames() {
        for (Trial trial : trials.values()) {
            for (MiniGame miniGame : trial.getMiniGames()) {
                if (!miniGames.containsKey(miniGame.getId())) {
                    validationErrors.add(String.format(
                        "Trial '%s' contains unregistered mini-game '%s'",
                        trial.getId(), miniGame.getId()));
                }
            }
        }
    }

    private void validateMiniGameRewards() {
        for (MiniGame miniGame : miniGames.values()) {
            String reward = miniGame.getReward();
            // Only validate if reward looks like an item ID (not descriptive text)
            if (reward != null && !reward.isEmpty() &&
                !reward.contains(" ") && reward.matches("[a-z_]+")) {
                if (!itemExists(reward)) {
                    validationErrors.add(String.format(
                        "Mini-game '%s' references unknown reward item '%s'",
                        miniGame.getId(), reward));
                }
            }
        }
    }

    /**
     * Checks if an item exists in any of the item maps (items, weapons, armors).
     */
    private boolean itemExists(String itemId) {
        return items.containsKey(itemId) ||
               weapons.containsKey(itemId) ||
               armors.containsKey(itemId);
    }

    // ==========================================
    // Getters - Locations
    // ==========================================

    /**
     * Gets a location by ID.
     */
    public Location getLocation(String id) {
        return locations.get(id);
    }

    /**
     * Gets all locations in the campaign.
     */
    public Map<String, Location> getLocations() {
        return Collections.unmodifiableMap(locations);
    }

    /**
     * Gets the starting location for the campaign.
     */
    public Location getStartingLocation() {
        return locations.get(startingLocationId);
    }

    // ==========================================
    // Getters - NPCs
    // ==========================================

    /**
     * Gets an NPC by ID.
     */
    public NPC getNPC(String id) {
        return npcs.get(id);
    }

    /**
     * Gets all NPCs in the campaign.
     */
    public Map<String, NPC> getNPCs() {
        return Collections.unmodifiableMap(npcs);
    }

    /**
     * Gets all NPCs at a specific location.
     */
    public List<NPC> getNPCsAtLocation(String locationId) {
        List<NPC> result = new ArrayList<>();
        for (NPC npc : npcs.values()) {
            if (locationId.equals(npc.getLocationId())) {
                result.add(npc);
            }
        }
        return result;
    }

    // ==========================================
    // Getters - Monsters
    // ==========================================

    /**
     * Gets a monster template by ID.
     */
    public Monster getMonsterTemplate(String id) {
        return monsterTemplates.get(id);
    }

    /**
     * Creates a new monster instance from a template.
     */
    public Monster createMonster(String templateId, String instanceId) {
        Monster template = monsterTemplates.get(templateId);
        if (template == null) {
            return null;
        }
        return template.copy(instanceId);
    }

    /**
     * Creates a new monster instance from a template with auto-generated ID.
     */
    public Monster createMonster(String templateId) {
        return createMonster(templateId, templateId + "_" + System.currentTimeMillis());
    }

    /**
     * Gets all monster templates in the campaign.
     */
    public Map<String, Monster> getMonsterTemplates() {
        return Collections.unmodifiableMap(monsterTemplates);
    }

    // ==========================================
    // Getters - Items
    // ==========================================

    /**
     * Gets an item by ID. Searches general items, weapons, and armor.
     */
    public Item getItem(String id) {
        if (weapons.containsKey(id)) {
            return weapons.get(id);
        }
        if (armors.containsKey(id)) {
            return armors.get(id);
        }
        return items.get(id);
    }

    /**
     * Gets a weapon by ID.
     */
    public Weapon getWeapon(String id) {
        return weapons.get(id);
    }

    /**
     * Gets armor by ID.
     */
    public Armor getArmor(String id) {
        return armors.get(id);
    }

    /**
     * Gets all general items (excluding weapons and armor).
     */
    public Map<String, Item> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Gets all weapons in the campaign.
     */
    public Map<String, Weapon> getWeapons() {
        return Collections.unmodifiableMap(weapons);
    }

    /**
     * Gets all armor in the campaign.
     */
    public Map<String, Armor> getArmors() {
        return Collections.unmodifiableMap(armors);
    }

    // ==========================================
    // Getters - Trials
    // ==========================================

    /**
     * Gets a trial by ID.
     */
    public Trial getTrial(String id) {
        return trials.get(id);
    }

    /**
     * Gets all trials in the campaign.
     */
    public Map<String, Trial> getTrials() {
        return Collections.unmodifiableMap(trials);
    }

    /**
     * Gets the trial associated with a location, if any.
     */
    public Trial getTrialAtLocation(String locationId) {
        for (Trial trial : trials.values()) {
            if (locationId.equals(trial.getLocation())) {
                return trial;
            }
        }
        return null;
    }

    // ==========================================
    // Getters - Mini-Games
    // ==========================================

    /**
     * Gets a mini-game by ID.
     */
    public MiniGame getMiniGame(String id) {
        return miniGames.get(id);
    }

    /**
     * Gets all mini-games in the campaign.
     */
    public Map<String, MiniGame> getMiniGames() {
        return Collections.unmodifiableMap(miniGames);
    }

    // ==========================================
    // Campaign Metadata
    // ==========================================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIntro() {
        return intro;
    }

    public boolean hasIntro() {
        return intro != null && !intro.isEmpty();
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public String getStartingLocationId() {
        return startingLocationId;
    }

    // ==========================================
    // Validation and Statistics
    // ==========================================

    /**
     * Returns true if there were any validation errors or warnings.
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * Gets the list of validation errors and warnings.
     */
    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    /**
     * Gets a summary of the campaign contents.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Campaign: %s (v%s) by %s%n", name, version, author));
        sb.append(String.format("Locations: %d%n", locations.size()));
        sb.append(String.format("NPCs: %d%n", npcs.size()));
        sb.append(String.format("Monsters: %d%n", monsterTemplates.size()));
        sb.append(String.format("Items: %d (Weapons: %d, Armor: %d, Other: %d)%n",
                weapons.size() + armors.size() + items.size(),
                weapons.size(), armors.size(), items.size()));
        sb.append(String.format("Trials: %d%n", trials.size()));
        sb.append(String.format("Mini-Games: %d%n", miniGames.size()));
        if (!validationErrors.isEmpty()) {
            sb.append(String.format("Validation Warnings: %d%n", validationErrors.size()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Campaign[%s: %s v%s]", id, name, version);
    }

    // ==========================================
    // Exception class
    // ==========================================

    /**
     * Exception thrown when a campaign fails to load.
     */
    public static class CampaignLoadException extends RuntimeException {
        private final List<String> errors;

        public CampaignLoadException(String message, List<String> errors) {
            super(message + (errors.isEmpty() ? "" : ": " + errors.get(0)));
            this.errors = new ArrayList<>(errors);
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }
    }
}
