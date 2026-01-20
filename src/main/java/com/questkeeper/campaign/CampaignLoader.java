package com.questkeeper.campaign;

import com.questkeeper.character.Character.Skill;
import com.questkeeper.character.NPC;
import com.questkeeper.combat.Monster;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.inventory.items.MagicItem;
import com.questkeeper.inventory.items.effects.DescriptionEffect;
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
 * Internal class that loads campaign content from YAML files.
 *
 * This is a package-private implementation detail. External code should use
 * {@link Campaign#loadFromYaml(String)} to load campaigns.
 *
 * Directory structure expected:
 * campaigns/
 *   campaign_id/
 *     campaign.yaml      - Campaign metadata
 *     monsters.yaml      - Monster templates
 *     npcs.yaml          - NPC definitions
 *     items.yaml         - Item definitions (weapons, armor, misc, magic items)
 *     locations.yaml     - Location definitions with exits, NPCs, and items
 *     trials.yaml        - Trial definitions (puzzle rooms with mini-games)
 *     minigames.yaml     - Mini-game definitions (skill checks, puzzles)
 *
 * @author Marc McGough
 * @version 1.1
 */
class CampaignLoader {

    private static final String CAMPAIGN_FILE = "campaign.yaml";
    private static final String MONSTERS_FILE = "monsters.yaml";
    private static final String NPCS_FILE = "npcs.yaml";
    private static final String ITEMS_FILE = "items.yaml";
    private static final String LOCATIONS_FILE = "locations.yaml";
    private static final String TRIALS_FILE = "trials.yaml";
    private static final String MINIGAMES_FILE = "minigames.yaml";

    private final Yaml yaml;
    private final Path campaignRoot;

    // Campaign metadata
    private String campaignId;
    private String campaignName;
    private String campaignDescription;
    private String campaignIntro;
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
    private final Map<String, Trial> trials;
    private final Map<String, MiniGame> miniGames;

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
        this.trials = new HashMap<>();
        this.miniGames = new HashMap<>();

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
        loadMiniGames();
        loadTrials();
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
            campaignIntro = getString(data, "intro", "");
            campaignAuthor = getString(data, "author", "Unknown");
            campaignVersion = getString(data, "version", "1.0");
            startingLocationId = getString(data, "starting_location", null);

            return true;
        } catch (IOException e) {
            loadErrors.add("Error reading campaign.yaml: " + e.getMessage());
            return false;
        }
    }

    private void loadMonsters() {
        Path monstersFile = campaignRoot.resolve(MONSTERS_FILE);

        if (!Files.exists(monstersFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(monstersFile)) {
            Map<String, Object> data = yaml.load(is);

            if (data == null) {
                return;
            }

            List<Map<String, Object>> monsterList = getListOfMaps(data, "monsters");

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

        // Parse ability modifiers safely
        Map<String, Object> abilities = getMap(data, "abilities");
        if (!abilities.isEmpty()) {
            monster.setAbilityModifiers(
                getInt(abilities, "str", 0),
                getInt(abilities, "dex", 0),
                getInt(abilities, "con", 0),
                getInt(abilities, "int", 0),
                getInt(abilities, "wis", 0),
                getInt(abilities, "cha", 0)
            );
        }

        // Parse special abilities - combine names into a comma-separated string
        List<Map<String, Object>> specialAbilities = getListOfMaps(data, "special_abilities");
        if (!specialAbilities.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> ability : specialAbilities) {
                String abilityName = getString(ability, "name", "");
                if (!abilityName.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(abilityName);
                }
            }
            if (sb.length() > 0) {
                monster.setSpecialAbility(sb.toString());
            }
        }

        // Parse behavior (AI type)
        String behaviorStr = getString(data, "behavior", null);
        if (behaviorStr != null) {
            try {
                monster.setBehavior(Monster.Behavior.valueOf(behaviorStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                loadErrors.add("Invalid behavior '" + behaviorStr + "' for monster " + id);
            }
        }

        return monster;
    }

    private void loadNPCs() {
        Path npcsFile = campaignRoot.resolve(NPCS_FILE);

        if (!Files.exists(npcsFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(npcsFile)) {
            Map<String, Object> data = yaml.load(is);

            if (data == null) {
                return;
            }

            List<Map<String, Object>> npcList = getListOfMaps(data, "npcs");

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

        // Parse shop items if present
        Map<String, Object> shopItemsData = getMap(data, "shop_items");
        if (shopItemsData != null) {
            for (Map.Entry<String, Object> entry : shopItemsData.entrySet()) {
                String itemName = entry.getKey();
                int price = entry.getValue() instanceof Number ? ((Number) entry.getValue()).intValue() : 0;
                npc.addShopItem(itemName.toLowerCase(), price);
            }
        }

        Map<String, String> dialogueMap = getStringMap(data, "dialogues");
        for (Map.Entry<String, String> entry : dialogueMap.entrySet()) {
            npc.addDialogue(entry.getKey(), entry.getValue());
        }

        List<String> lines = getStringList(data, "sample_lines");
        for (String line : lines) {
            npc.addSampleLine(line);
        }

        return npc;
    }

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

            List<Map<String, Object>> weaponList = getListOfMaps(data, "weapons");
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

            List<Map<String, Object>> armorList = getListOfMaps(data, "armor");
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

            List<Map<String, Object>> itemList = getListOfMaps(data, "items");
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

            // Load magic items (stored in items map for unified access)
            List<Map<String, Object>> magicItemList = getListOfMaps(data, "magic_items");
            for (Map<String, Object> magicItemData : magicItemList) {
                try {
                    String yamlId = getString(magicItemData, "id", null);
                    Item magicItem = parseMagicItem(magicItemData);
                    String key = yamlId != null ? yamlId : magicItem.getId();
                    items.put(key, magicItem);
                } catch (Exception e) {
                    loadErrors.add("Error parsing magic item: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading items.yaml: " + e.getMessage());
        }
    }

    private void loadLocations() {
        Path locationsFile = campaignRoot.resolve(LOCATIONS_FILE);

        if (!Files.exists(locationsFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(locationsFile)) {
            Map<String, Object> data = yaml.load(is);

            if (data == null) {
                return;
            }

            List<Map<String, Object>> locationList = getListOfMaps(data, "locations");

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

    private Location parseLocation(Map<String, Object> data) {
        String id = getString(data, "id", "unknown_location");
        String name = getString(data, "name", "Unknown Location");
        String description = getString(data, "description", "");
        String readAloudText = getString(data, "read_aloud_text", "");

        Location location = new Location(id, name, description, readAloudText);

        // Parse locked message (shown when player tries to enter a locked location)
        String lockedMessage = getString(data, "locked_message", null);
        if (lockedMessage != null) {
            location.setLockedMessage(lockedMessage);
        }

        // Parse exits
        Map<String, String> exits = getStringMap(data, "exits");
        for (Map.Entry<String, String> exit : exits.entrySet()) {
            location.addExit(exit.getKey(), exit.getValue());
        }

        // Parse NPCs at this location
        List<String> npcIds = getStringList(data, "npcs");
        for (String npcId : npcIds) {
            location.addNpc(npcId);
        }

        // Parse items at this location
        List<String> itemIds = getStringList(data, "items");
        for (String itemId : itemIds) {
            location.addItem(itemId);
        }

        // Parse flags and handle locked flag specially
        List<String> flags = getStringList(data, "flags");
        for (String flag : flags) {
            location.setFlag(flag);
        }
        if (flags.contains("locked")) {
            location.lock();
        }

        return location;
    }

    private void loadMiniGames() {
        Path miniGamesFile = campaignRoot.resolve(MINIGAMES_FILE);

        if (!Files.exists(miniGamesFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(miniGamesFile)) {
            Map<String, Object> data = yaml.load(is);

            if (data == null) {
                return;
            }

            List<Map<String, Object>> miniGameList = getListOfMaps(data, "minigames");

            for (Map<String, Object> miniGameData : miniGameList) {
                try {
                    MiniGame miniGame = parseMiniGame(miniGameData);
                    miniGames.put(miniGame.getId(), miniGame);
                } catch (Exception e) {
                    loadErrors.add("Error parsing minigame: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading minigames.yaml: " + e.getMessage());
        }
    }

    private MiniGame parseMiniGame(Map<String, Object> data) {
        String id = getString(data, "id", "unknown_minigame");
        String name = getString(data, "name", "Unknown Mini-Game");

        String typeStr = getString(data, "type", "SKILL_CHECK");
        MiniGame.Type type;
        try {
            type = MiniGame.Type.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid minigame type '" + typeStr + "' for " + id);
            type = MiniGame.Type.SKILL_CHECK;
        }

        MiniGame miniGame = new MiniGame(id, name, type);
        miniGame.setDescription(getString(data, "description", ""));
        miniGame.setHint(getString(data, "hint", ""));
        miniGame.setDc(getInt(data, "dc", 10));

        // Parse required skill
        String requiredSkillStr = getString(data, "required_skill", null);
        if (requiredSkillStr != null) {
            try {
                miniGame.setRequiredSkill(Skill.valueOf(requiredSkillStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                loadErrors.add("Invalid required_skill '" + requiredSkillStr + "' for minigame " + id);
            }
        }

        // Parse alternate skill
        String altSkillStr = getString(data, "alternate_skill", null);
        if (altSkillStr != null) {
            try {
                miniGame.setAlternateSkill(Skill.valueOf(altSkillStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                loadErrors.add("Invalid alternate_skill '" + altSkillStr + "' for minigame " + id);
            }
        }

        // Parse reward (can be item ID or text)
        String rewardItem = getString(data, "reward_item", null);
        String rewardText = getString(data, "reward_text", "");
        if (rewardItem != null) {
            miniGame.setReward(rewardItem);
        } else if (!rewardText.isEmpty()) {
            miniGame.setReward(rewardText);
        }

        // Parse success/fail text
        miniGame.setCompletionText(getString(data, "success_text", "Challenge completed!"));
        miniGame.setFailureText(getString(data, "fail_text", "You failed the challenge."));
        miniGame.setFailConsequence(getString(data, "fail_consequence", ""));

        return miniGame;
    }

    private void loadTrials() {
        Path trialsFile = campaignRoot.resolve(TRIALS_FILE);

        if (!Files.exists(trialsFile)) {
            return;
        }

        try (InputStream is = Files.newInputStream(trialsFile)) {
            Map<String, Object> data = yaml.load(is);

            if (data == null) {
                return;
            }

            List<Map<String, Object>> trialList = getListOfMaps(data, "trials");

            for (Map<String, Object> trialData : trialList) {
                try {
                    Trial trial = parseTrial(trialData);
                    trials.put(trial.getId(), trial);
                } catch (Exception e) {
                    loadErrors.add("Error parsing trial: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            loadErrors.add("Error reading trials.yaml: " + e.getMessage());
        }
    }

    private Trial parseTrial(Map<String, Object> data) {
        String id = getString(data, "id", "unknown_trial");
        String name = getString(data, "name", "Unknown Trial");
        String locationId = getString(data, "location_id", null);
        String entryNarrative = getString(data, "entry_narrative", "");

        Trial trial = new Trial(id, name, locationId, entryNarrative);

        trial.setCompletionReward(getString(data, "completion_reward", ""));
        trial.setStinger(getString(data, "stinger", ""));

        // Parse prerequisites (flags that must be set to start this trial)
        List<String> prerequisites = getStringList(data, "prerequisites");
        trial.setPrerequisites(prerequisites);

        // Parse completion flags (flags to set when trial is completed)
        List<String> completionFlags = getStringList(data, "completion_flags");
        trial.setCompletionFlags(completionFlags);

        // Add mini-games to the trial by looking them up from the loaded miniGames
        List<String> miniGameIds = getStringList(data, "mini_games");
        for (String miniGameId : miniGameIds) {
            MiniGame miniGame = miniGames.get(miniGameId);
            if (miniGame != null) {
                trial.addMiniGame(miniGame);
            } else {
                loadErrors.add("Trial '" + id + "' references unknown minigame: " + miniGameId);
            }
        }

        return trial;
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

        List<String> props = getStringList(data, "properties");
        for (String prop : props) {
            try {
                weapon.addProperty(Weapon.WeaponProperty.valueOf(prop.toUpperCase()));
            } catch (IllegalArgumentException e) {
                loadErrors.add("Invalid weapon property: " + prop);
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
        String id = getString(data, "id", "unknown");
        String name = getString(data, "name", "Unknown Item");

        String typeStr = getString(data, "type", "MISCELLANEOUS");
        Item.ItemType type = Item.ItemType.valueOf(typeStr.toUpperCase());

        String description = getString(data, "description", "");
        double weight = getDouble(data, "weight", 0);
        int value = getInt(data, "value", 0);

        String rarityStr = getString(data, "rarity", "COMMON");
        Item.Rarity rarity;
        try {
            rarity = Item.Rarity.valueOf(rarityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid rarity: " + rarityStr);
            rarity = Item.Rarity.COMMON;
        }

        // Check if this item has effect fields - if so, create a MagicItem
        String effectDescription = getString(data, "effect_description", null);
        if (effectDescription != null && !effectDescription.isEmpty()) {
            MagicItem magicItem = MagicItem.createWithId(id, name, description, weight, value, rarity);

            // Create an effect from the YAML fields
            String usageType = getString(data, "usage_type", "DAILY");
            int charges = getInt(data, "charges", 1);
            DescriptionEffect effect = DescriptionEffect.fromYaml(id, name, effectDescription, usageType, charges);
            magicItem.addEffect(effect);

            // Handle attunement
            if (getBoolean(data, "requires_attunement", false)) {
                magicItem.setRequiresAttunement(true);
            }

            return magicItem;
        }

        // Regular item without effects - use YAML ID
        Item item = Item.createWithId(id, name, type, description, weight, value);
        item.setRarity(rarity);
        item.setStackable(getBoolean(data, "stackable", false));
        item.setQuestItem(getBoolean(data, "quest_item", false));

        return item;
    }

    private Item parseMagicItem(Map<String, Object> data) {
        String name = getString(data, "name", "Unknown Magic Item");
        String description = getString(data, "description", "");
        double weight = getDouble(data, "weight", 0);
        int value = getInt(data, "value", 0);

        String rarityStr = getString(data, "rarity", "UNCOMMON");
        Item.Rarity rarity;
        try {
            rarity = Item.Rarity.valueOf(rarityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            loadErrors.add("Invalid rarity '" + rarityStr + "' for magic item " + name);
            rarity = Item.Rarity.UNCOMMON;
        }

        MagicItem magicItem = new MagicItem(name, description, weight, value, rarity);

        // Set attunement requirements
        if (getBoolean(data, "requires_attunement", false)) {
            magicItem.setRequiresAttunement(true);
            String attunementReq = getString(data, "attunement_requirements", null);
            if (attunementReq != null) {
                magicItem.setAttunementRequirement(attunementReq);
            }
        }

        // Mark as consumable if specified
        if (getBoolean(data, "consumable", false)) {
            magicItem.setStackable(true);
        }

        return magicItem;
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

    public Optional<Trial> getTrial(String id) {
        return Optional.ofNullable(trials.get(id));
    }

    public Map<String, Trial> getAllTrials() {
        return Collections.unmodifiableMap(trials);
    }

    public Optional<MiniGame> getMiniGame(String id) {
        return Optional.ofNullable(miniGames.get(id));
    }

    public Map<String, MiniGame> getAllMiniGames() {
        return Collections.unmodifiableMap(miniGames);
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

    public String getCampaignIntro() {
        return campaignIntro;
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
        sb.append(String.format("Trials: %d | Mini-Games: %d%n",
                trials.size(), miniGames.size()));
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

    /**
     * Safely extracts a List from YAML data with type checking.
     * Returns empty list if key doesn't exist or value is not a List.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListOfMaps(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    // Verify all keys are strings
                    boolean allStrings = map.keySet().stream().allMatch(k -> k instanceof String);
                    if (allStrings) {
                        result.add((Map<String, Object>) map);
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Safely extracts a Map from YAML data with type checking.
     * Returns empty map if key doesn't exist or value is not a Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            // Verify all keys are strings
            boolean allStrings = map.keySet().stream().allMatch(k -> k instanceof String);
            if (allStrings) {
                return (Map<String, Object>) map;
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Safely extracts a List of Strings from YAML data.
     * Returns empty list if key doesn't exist or value is not a List.
     */
    private List<String> getStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Safely extracts a Map with String keys and String values from YAML data.
     * Returns empty map if key doesn't exist or value is not a valid Map.
     */
    private Map<String, String> getStringMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String keyStr) {
                    result.put(keyStr, entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }
}