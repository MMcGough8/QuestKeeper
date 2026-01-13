package com.questkeeper.campaign;

import com.questkeeper.character.NPC;
import com.questkeeper.combat.Monster;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.world.Location;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads campaign content from YAML files.
 * 
 * Supports loading monsters, NPCs, items, and campaign metadata from
 * structured YAML files, enabling campaign-agnostic game content.
 * 
 * Directory structure expected:
 * campaigns/
 *   campaign_id/
 *     campaign.yaml      - Campaign metadata
 *     monsters.yaml      - Monster templates
 *     npcs.yaml          - NPC definitions
 *     items.yaml         - Item definitions (weapons, armor, misc)
 *     locations.yaml     - Location definitions with exits, NPCs, and items
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class CampaignLoader {

    private static final String CAMPAIGN_FILE = "campaign.yaml";
    private static final String MONSTERS_FILE = "monsters.yaml";
    private static final String NPCS_FILE = "npcs.yaml";
    private static final String ITEMS_FILE = "items.yaml";
    private static final String LOCATIONS_FILE = "locations.yaml";

    private final Yaml yaml;
    private final Path campaignRoot;

    // Campaign metadata
    private String campaignId;
    private String campaignName;
    private String campaignDescription;
    private String campaignAuthor;
    private String campaignVersion;
    private String startingLocationId;

    // Loaded content (templates)
    private final Map<String, Monster> monsterTemplates;
    private final Map<String, NPC> npcs;
    private final Map<String, Item> items;
    private final Map<String, Weapon> weapons;
    private final Map<String, Armor> armors;
    private final Map<String, Location> locations;

    // Loading state
    private boolean loaded;
    private final List<String> loadErrors;


    public CampaignLoader(Path campaignPath) {
        this.yaml = new Yaml();
        this.campaignRoot = campaignPath;
        
        this.monsterTemplates = new HashMap<>();
        this.npcs = new HashMap<>();
        this.items = new HashMap<>();
        this.weapons = new HashMap<>();
        this.armors = new HashMap<>();
        this.locations = new HashMap<>();

        this.loaded = false;
        this.loadErrors = new ArrayList<>();
    }

    public CampaignLoader(String campaignId) {
        this(Path.of("campaigns", campaignId));
    }

    public boolean load() {
        loadErrors.clear();
        loaded = false;

        if (!Files.isDirectory(campaignRoot)) {
            loadErrors.add("Campaign directory not found: " + campaignRoot);
            return false;
        }

        if (!loadCampaignMetadata()) {
            return false;
        }

        loadMonsters();
        loadNPCs();
        loadItems();
        loadLocations();
        validateExitReferences();

        loaded = true;
        return true; 
    }

    @SuppressWarnings("unchecked")
    private boolean loadCampaignMetadata() {
        Path campaignFile = campaignRoot.resolve(CAMPAIGN_FILE);
        
        if (!Files.exists(campaignFile)) {
            loadErrors.add("Missing required file: " + CAMPAIGN_FILE);
            return false;
        }

        try (InputStream is = Files.newInputStream(campaignFile)) {
            Map<String, Object> data = yaml.load(is);
            
            if (data == null) {
                loadErrors.add("Empty or invalid campaign.yaml");
                return false;
            }

            campaignId = getString(data, "id", "unknown");
            campaignName = getString(data, "name", "Unnamed Campaign");
            campaignDescription = getString(data, "description", "");
            campaignAuthor = getString(data, "author", "Unknown");
            campaignVersion = getString(data, "version", "1.0");
            startingLocationId = getString(data, "starting_location", null);

            return true;
        } catch (IOException e) {
            loadErrors.add("Error reading campaign.yaml: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMonsters() {
        Path monstersFile = campaignRoot.resolve(MONSTERS_FILE);
        
        if (!Files.exists(monstersFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(monstersFile)) {
            Map<String, Object> data = yaml.load(is);
            
            if (data == null || !data.containsKey("monsters")) {
                return;
            }

            List<Map<String, Object>> monsterList = (List<Map<String, Object>>) data.get("monsters");
            
            for (Map<String, Object> monsterData : monsterList) {
                try {
                    Monster monster = parseMonster(monsterData);
                    monsterTemplates.put(monster.getId(), monster);
                } catch (Exception e) {
                    loadErrors.add("Error parsing monster: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading monsters.yaml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Monster parseMonster(Map<String, Object> data) {
        String id = getString(data, "id", "unknown_monster");
        String name = getString(data, "name", "Unknown Monster");
        int ac = getInt(data, "armor_class", 10);
        int hp = getInt(data, "hit_points", 10);

        Monster monster = new Monster(id, name, ac, hp);

        String sizeStr = getString(data, "size", "MEDIUM");
        try {
            monster.setSize(Monster.Size.valueOf(sizeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid size '" + sizeStr + "' for monster " + id);
        }

        String typeStr = getString(data, "type", "HUMANOID");
        try {
            monster.setType(Monster.MonsterType.valueOf(typeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid type '" + typeStr + "' for monster " + id);
        }

        monster.setAlignment(getString(data, "alignment", "unaligned"));
        monster.setSpeed(getInt(data, "speed", 30));
        monster.setChallengeRating(getDouble(data, "challenge_rating", 0));
        monster.setExperienceValue(getInt(data, "xp", 10));
        monster.setAttackBonus(getInt(data, "attack_bonus", 2));
        monster.setDamageDice(getString(data, "damage_dice", "1d4"));
        monster.setDescription(getString(data, "description", ""));

        if (data.containsKey("abilities")) {
            Map<String, Object> abilities = (Map<String, Object>) data.get("abilities");
            monster.setAbilityModifiers(
                getInt(abilities, "str", 0),
                getInt(abilities, "dex", 0),
                getInt(abilities, "con", 0),
                getInt(abilities, "int", 0),
                getInt(abilities, "wis", 0),
                getInt(abilities, "cha", 0)
            );
        }

        return monster;
    }

    @SuppressWarnings("unchecked")
    private void loadNPCs() {
        Path npcsFile = campaignRoot.resolve(NPCS_FILE);
        
        if (!Files.exists(npcsFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(npcsFile)) {
            Map<String, Object> data = yaml.load(is);
            
            if (data == null || !data.containsKey("npcs")) {
                return;
            }

            List<Map<String, Object>> npcList = (List<Map<String, Object>>) data.get("npcs");
            
            for (Map<String, Object> npcData : npcList) {
                try {
                    NPC npc = parseNPC(npcData);
                    npcs.put(npc.getId(), npc);
                } catch (Exception e) {
                    loadErrors.add("Error parsing NPC: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading npcs.yaml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private NPC parseNPC(Map<String, Object> data) {
        String id = getString(data, "id", "unknown_npc");
        String name = getString(data, "name", "Unknown NPC");
        String role = getString(data, "role", "");
        String voice = getString(data, "voice", "");
        String personality = getString(data, "personality", "");

        NPC npc = new NPC(id, name, role, voice, personality);

        npc.setDescription(getString(data, "description", ""));
        npc.setLocationId(getString(data, "location_id", null));
        npc.setShopkeeper(getBoolean(data, "shopkeeper", false));
        npc.setGreeting(getString(data, "greeting", ""));
        npc.setReturnGreeting(getString(data, "return_greeting", ""));

        if (data.containsKey("dialogues")) {
            Map<String, String> dialogueMap = (Map<String, String>) data.get("dialogues");
            for (Map.Entry<String, String> entry : dialogueMap.entrySet()) {
                npc.addDialogue(entry.getKey(), entry.getValue());
            }
        }

        if (data.containsKey("sample_lines")) {
            List<String> lines = (List<String>) data.get("sample_lines");
            for (String line : lines) {
                npc.addSampleLine(line);
            }
        }

        return npc;
    }

    @SuppressWarnings("unchecked")
    private void loadItems() {
        Path itemsFile = campaignRoot.resolve(ITEMS_FILE);
        
        if (!Files.exists(itemsFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(itemsFile)) {
            Map<String, Object> data = yaml.load(is);
            
            if (data == null) {
                return;
            }

            if (data.containsKey("weapons")) {
                List<Map<String, Object>> weaponList = (List<Map<String, Object>>) data.get("weapons");
                for (Map<String, Object> weaponData : weaponList) {
                    try {
                        String yamlId = getString(weaponData, "id", null);
                        Weapon weapon = parseWeapon(weaponData);
                        // Use YAML id as key if provided, otherwise use generated id
                        String key = yamlId != null ? yamlId : weapon.getId();
                        weapons.put(key, weapon);
                    } catch (Exception e) {
                        loadErrors.add("Error parsing weapon: " + e.getMessage());
                    }
                }
            }

            if (data.containsKey("armor")) {
                List<Map<String, Object>> armorList = (List<Map<String, Object>>) data.get("armor");
                for (Map<String, Object> armorData : armorList) {
                    try {
                        String yamlId = getString(armorData, "id", null);
                        Armor armor = parseArmor(armorData);
                        String key = yamlId != null ? yamlId : armor.getId();
                        armors.put(key, armor);
                    } catch (Exception e) {
                        loadErrors.add("Error parsing armor: " + e.getMessage());
                    }
                }
            }

            if (data.containsKey("items")) {
                List<Map<String, Object>> itemList = (List<Map<String, Object>>) data.get("items");
                for (Map<String, Object> itemData : itemList) {
                    try {
                        String yamlId = getString(itemData, "id", null);
                        Item item = parseItem(itemData);
                        String key = yamlId != null ? yamlId : item.getId();
                        items.put(key, item);
                    } catch (Exception e) {
                        loadErrors.add("Error parsing item: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading items.yaml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLocations() {
        Path locationsFile = campaignRoot.resolve(LOCATIONS_FILE);

        if (!Files.exists(locationsFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(locationsFile)) {
            Map<String, Object> data = yaml.load(is);

            if (data == null || !data.containsKey("locations")) {
                return;
            }

            List<Map<String, Object>> locationList = (List<Map<String, Object>>) data.get("locations");

            for (Map<String, Object> locationData : locationList) {
                try {
                    Location location = parseLocation(locationData);
                    locations.put(location.getId(), location);
                } catch (Exception e) {
                    loadErrors.add("Error parsing location: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading locations.yaml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Location parseLocation(Map<String, Object> data) {
        String id = getString(data, "id", "unknown_location");
        String name = getString(data, "name", "Unknown Location");
        String description = getString(data, "description", "");
        String readAloudText = getString(data, "read_aloud_text", "");

        Location location = new Location(id, name, description, readAloudText);

        // Parse exits
        if (data.containsKey("exits")) {
            Map<String, String> exits = (Map<String, String>) data.get("exits");
            for (Map.Entry<String, String> exit : exits.entrySet()) {
                location.addExit(exit.getKey(), exit.getValue());
            }
        }

        // Parse NPCs at this location
        if (data.containsKey("npcs")) {
            List<String> npcIds = (List<String>) data.get("npcs");
            for (String npcId : npcIds) {
                location.addNpc(npcId);
            }
        }

        // Parse items at this location
        if (data.containsKey("items")) {
            List<String> itemIds = (List<String>) data.get("items");
            for (String itemId : itemIds) {
                location.addItem(itemId);
            }
        }

        // Parse flags
        if (data.containsKey("flags")) {
            List<String> flags = (List<String>) data.get("flags");
            for (String flag : flags) {
                location.setFlag(flag);
            }
        }

        // Handle locked flag specially
        if (data.containsKey("flags")) {
            List<String> flags = (List<String>) data.get("flags");
            if (flags.contains("locked")) {
                location.lock();
            }
        }

        return location;
    }

    /**
     * Validates that all exit references point to existing locations.
     * Adds warnings for any invalid exit references found.
     */
    private void validateExitReferences() {
        for (Location location : locations.values()) {
            for (String direction : location.getExits()) {
                String targetId = location.getExit(direction);
                if (!locations.containsKey(targetId)) {
                    loadErrors.add(String.format(
                        "Invalid exit reference: '%s' exit '%s' points to unknown location '%s'",
                        location.getId(), direction, targetId));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Weapon parseWeapon(Map<String, Object> data) {
        String name = getString(data, "name", "Unknown Weapon");
        int diceCount = getInt(data, "damage_dice_count", 1);
        int dieSize = getInt(data, "damage_die_size", 4);
        
        String damageTypeStr = getString(data, "damage_type", "SLASHING");
        Weapon.DamageType damageType = Weapon.DamageType.valueOf(damageTypeStr.toUpperCase());
        
        String categoryStr = getString(data, "category", "SIMPLE_MELEE");
        Weapon.WeaponCategory category = Weapon.WeaponCategory.valueOf(categoryStr.toUpperCase());
        
        double weight = getDouble(data, "weight", 1.0);
        int value = getInt(data, "value", 1);
        int normalRange = getInt(data, "normal_range", 0);
        int longRange = getInt(data, "long_range", 0);

        Weapon weapon;
        if (normalRange > 0) {
            weapon = new Weapon(name, diceCount, dieSize, damageType, category, 
                              normalRange, longRange, weight, value);
        } else {
            weapon = new Weapon(name, diceCount, dieSize, damageType, category, weight, value);
        }

        if (data.containsKey("properties")) {
            List<String> props = (List<String>) data.get("properties");
            for (String prop : props) {
                try {
                    weapon.addProperty(Weapon.WeaponProperty.valueOf(prop.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    loadErrors.add("Invalid weapon property: " + prop);
                }
            }
        }

        if (data.containsKey("versatile_die_size")) {
            weapon.setVersatileDieSize(getInt(data, "versatile_die_size", 0));
        }

        if (data.containsKey("attack_bonus")) {
            weapon.setAttackBonus(getInt(data, "attack_bonus", 0));
        }
        if (data.containsKey("damage_bonus")) {
            weapon.setDamageBonus(getInt(data, "damage_bonus", 0));
        }

        String rarityStr = getString(data, "rarity", "COMMON");
        try {
            weapon.setRarity(Item.Rarity.valueOf(rarityStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid rarity: " + rarityStr);
        }

        weapon.setDescription(getString(data, "description", ""));

        return weapon;
    }

    @SuppressWarnings("unchecked")
    private Armor parseArmor(Map<String, Object> data) {
        String name = getString(data, "name", "Unknown Armor");
        int baseAC = getInt(data, "base_ac", 10);
        
        String categoryStr = getString(data, "category", "LIGHT");
        Armor.ArmorCategory category = Armor.ArmorCategory.valueOf(categoryStr.toUpperCase());
        
        double weight = getDouble(data, "weight", 1.0);
        int value = getInt(data, "value", 1);
        int strengthReq = getInt(data, "strength_requirement", 0);
        boolean stealthDisadv = getBoolean(data, "stealth_disadvantage", false);

        Armor armor;
        if (strengthReq > 0 || stealthDisadv) {
            armor = new Armor(name, category, baseAC, strengthReq, stealthDisadv, weight, value);
        } else {
            armor = new Armor(name, category, baseAC, weight, value);
        }

        if (data.containsKey("magic_bonus")) {
            armor.setMagicBonus(getInt(data, "magic_bonus", 0));
        }

        String rarityStr = getString(data, "rarity", "COMMON");
        try {
            armor.setRarity(Item.Rarity.valueOf(rarityStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid rarity: " + rarityStr);
        }

        armor.setDescription(getString(data, "description", ""));

        return armor;
    }

    private Item parseItem(Map<String, Object> data) {
        String name = getString(data, "name", "Unknown Item");
        
        String typeStr = getString(data, "type", "MISCELLANEOUS");
        Item.ItemType type = Item.ItemType.valueOf(typeStr.toUpperCase());
        
        String description = getString(data, "description", "");
        double weight = getDouble(data, "weight", 0);
        int value = getInt(data, "value", 0);

        Item item = new Item(name, type, description, weight, value);

        String rarityStr = getString(data, "rarity", "COMMON");
        try {
            item.setRarity(Item.Rarity.valueOf(rarityStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid rarity: " + rarityStr);
        }

        item.setStackable(getBoolean(data, "stackable", false));
        item.setQuestItem(getBoolean(data, "quest_item", false));

        return item;
    }

    public Optional<Monster> createMonster(String templateId, String instanceId) {
        Monster template = monsterTemplates.get(templateId);
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(template.copy(instanceId));
    }

    public Optional<Monster> createMonster(String templateId) {
        return createMonster(templateId, templateId + "_" + System.currentTimeMillis());
    }

    public Optional<NPC> getNPC(String id) {
        return Optional.ofNullable(npcs.get(id));
    }

    public List<NPC> getNPCsAtLocation(String locationId) {
        List<NPC> result = new ArrayList<>();
        for (NPC npc : npcs.values()) {
            if (locationId.equals(npc.getLocationId())) {
                result.add(npc);
            }
        }
        return result;
    }

    public Optional<Weapon> getWeapon(String id) {
        return Optional.ofNullable(weapons.get(id));
    }

    public Optional<Armor> getArmor(String id) {
        return Optional.ofNullable(armors.get(id));
    }

    public Optional<Item> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Optional<Item> getAnyItem(String id) {
        if (weapons.containsKey(id)) {
            return Optional.of(weapons.get(id));
        }
        if (armors.containsKey(id)) {
            return Optional.of(armors.get(id));
        }
        return Optional.ofNullable(items.get(id));
    }

    public Map<String, Monster> getMonsterTemplates() {
        return Collections.unmodifiableMap(monsterTemplates);
    }

    public Map<String, NPC> getAllNPCs() {
        return Collections.unmodifiableMap(npcs);
    }

    public Map<String, Weapon> getAllWeapons() {
        return Collections.unmodifiableMap(weapons);
    }

    public Map<String, Armor> getAllArmor() {
        return Collections.unmodifiableMap(armors);
    }

    public Map<String, Item> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    public Optional<Location> getLocation(String id) {
        return Optional.ofNullable(locations.get(id));
    }

    public Map<String, Location> getAllLocations() {
        return Collections.unmodifiableMap(locations);
    }

    public Optional<Location> getStartingLocation() {
        if (startingLocationId == null) {
            return Optional.empty();
        }
        return getLocation(startingLocationId);
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public String getCampaignDescription() {
        return campaignDescription;
    }

    public String getCampaignAuthor() {
        return campaignAuthor;
    }

    public String getCampaignVersion() {
        return campaignVersion;
    }

    public String getStartingLocationId() {
        return startingLocationId;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<String> getLoadErrors() {
        return Collections.unmodifiableList(loadErrors);
    }

    public boolean hasErrors() {
        return !loadErrors.isEmpty();
    }

    public String getLoadSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Campaign: %s (v%s) by %s%n", campaignName, campaignVersion, campaignAuthor));
        sb.append(String.format("Locations: %d | Monsters: %d | NPCs: %d%n",
                locations.size(), monsterTemplates.size(), npcs.size()));
        sb.append(String.format("Weapons: %d | Armor: %d | Items: %d%n",
                weapons.size(), armors.size(), items.size()));
        if (!loadErrors.isEmpty()) {
            sb.append(String.format("Warnings: %d%n", loadErrors.size()));
        }
        return sb.toString();
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}