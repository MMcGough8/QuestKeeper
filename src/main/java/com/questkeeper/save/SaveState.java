package com.questkeeper.save;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;

import org.w3c.dom.CharacterData;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Manages game state persistence for QuestKeeper.
 * 
 * Saves and loads all game progress including character data,
 * current location, inventory, quest flags, and campaign state.
 * Uses YAML format for human-readable save files.
 * 
 * Design principles:
 * - Campaign-agnostic: works with any campaign
 * - Forward-compatible: ignores unknown fields when loading
 * - Human-readable: YAML format for easy debugging
 * - Atomic saves: writes to temp file then renames
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class SaveState {

    private String saveVersion;
    private Instant timestamp;
    private String campaignId;
    private String saveName;

    private CharacterData character;

    private String currentLocationId;
    private Set<String> visitedLocations;

    private Map<String, Boolean> stateFlags;
    private Map<String, Integer> stateCounters;
    private Map<String, String> stateStrings;

    private List<String> inventoryItems;
    private List<String> equippedItems;
    private int gold;

    private long totalPlayTimeSeconds;
    private int saveCount;

    private static final String CURRENT_VERSION = "1.0";
    private static final String DEFAULT_SAVE_DIR = "saves";

    /**
     * Creates a new empty SaveState.
     */
    public SaveState() {
        this.saveVersion = CURRENT_VERSION;
        this.timestamp = Instant.now();
        this.campaignId = "unknown";
        this.saveName = "Unnamed Save";
        
        this.visitedLocations = new HashSet<>();
        this.stateFlags = new HashMap<>();
        this.stateCounters = new HashMap<>();
        this.stateStrings = new HashMap<>();
        this.inventoryItems = new ArrayList<>();
        this.equippedItems = new ArrayList<>();
        this.gold = 0;
        
        this.totalPlayTimeSeconds = 0;
        this.saveCount = 0;
    }

    /**
     * Creates a SaveState from an existing Character.
     */
    public SaveState(Character character, String campaignId) {
        this();
        this.campaignId = campaignId;
        this.saveName = character.getName() + " - " + campaignId;
        this.character = CharacterData.fromCharacter(character);
    }

    /**
     * Saves the game state to a YAML file.
     * Uses atomic write (temp file + rename) for safety.
     */
    public void save(Path filepath) throws IOException {
        this.timestamp = Instant.now();
        this.saveCount++;
        
        // Ensure parent directory exists
        Path parent = filepath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        
        // Convert to Map for YAML serialization
        Map<String, Object> data = toMap();
        
        // Configure YAML output
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);
        
        // Atomic write: temp file then rename
        Path tempFile = filepath.resolveSibling(filepath.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            yaml.dump(data, writer);
        }
        Files.move(tempFile, filepath, 
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

     /**
     * Loads a game state from a YAML file.
     */
    public static SaveState load(Path filepath) throws IOException {
        Yaml yaml = new Yaml();
        
        try (Reader reader = Files.newBufferedReader(filepath)) {
            Map<String, Object> data = yaml.load(reader);
            return fromMap(data);
        }
    }

    /**
     * Quick save to default location.
     */
    public void quickSave() throws IOException {
        String filename = sanitizeFilename(saveName) + ".yaml";
        Path savePath = Path.of(DEFAULT_SAVE_DIR, filename);
        save(savePath);
    }

    /**
     * Lists all save files in the default save directory.
     */
    public static List<SaveInfo> listSaves() throws IOException {
        Path saveDir = Path.of(DEFAULT_SAVE_DIR);
        if (!Files.exists(saveDir)) {
            return Collections.emptyList();
        }
        
        List<SaveInfo> saves = new ArrayList<>();
        try (var stream = Files.list(saveDir)) {
            stream.filter(p -> p.toString().endsWith(".yaml"))
                  .forEach(p -> {
                      try {
                          saves.add(SaveInfo.fromFile(p));
                      } catch (IOException e) {
                          // Skip corrupted saves
                      }
                  });
        }
        
        // Sort by timestamp, newest first
        saves.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return saves;
    }

    private Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        
        map.put("save_version", saveVersion);
        map.put("timestamp", timestamp.toString());
        map.put("campaign_id", campaignId);
        map.put("save_name", saveName);
        
        if (character != null) {
            map.put("character", character.toMap());
        }
        
        map.put("current_location", currentLocationId);
        map.put("visited_locations", new ArrayList<>(visitedLocations));
        
        map.put("flags", stateFlags);
        map.put("counters", stateCounters);
        map.put("strings", stateStrings);
        
        map.put("inventory", inventoryItems);
        map.put("equipped", equippedItems);
        map.put("gold", gold);
        
        map.put("play_time_seconds", totalPlayTimeSeconds);
        map.put("save_count", saveCount);
        
        return map;
    }

     @SuppressWarnings("unchecked")
    private static SaveState fromMap(Map<String, Object> data) {
        SaveState state = new SaveState();
        
        // Metadata
        state.saveVersion = getString(data, "save_version", CURRENT_VERSION);
        state.timestamp = Instant.parse(getString(data, "timestamp", Instant.now().toString()));
        state.campaignId = getString(data, "campaign_id", "unknown");
        state.saveName = getString(data, "save_name", "Unnamed Save");
        
        // Character
        Map<String, Object> charData = (Map<String, Object>) data.get("character");
        if (charData != null) {
            state.character = CharacterData.fromMap(charData);
        }
        
        // World state
        state.currentLocationId = getString(data, "current_location", null);
        List<String> visited = (List<String>) data.get("visited_locations");
        if (visited != null) {
            state.visitedLocations = new HashSet<>(visited);
        }
        
        // Campaign progress
        Map<String, Boolean> flags = (Map<String, Boolean>) data.get("flags");
        if (flags != null) {
            state.stateFlags = new HashMap<>(flags);
        }
        
        Map<String, Integer> counters = (Map<String, Integer>) data.get("counters");
        if (counters != null) {
            state.stateCounters = new HashMap<>(counters);
        }
        
        Map<String, String> strings = (Map<String, String>) data.get("strings");
        if (strings != null) {
            state.stateStrings = new HashMap<>(strings);
        }
        
        // Inventory
        List<String> inventory = (List<String>) data.get("inventory");
        if (inventory != null) {
            state.inventoryItems = new ArrayList<>(inventory);
        }
        
        List<String> equipped = (List<String>) data.get("equipped");
        if (equipped != null) {
            state.equippedItems = new ArrayList<>(equipped);
        }
        
        state.gold = getInt(data, "gold", 0);
        
        // Stats
        state.totalPlayTimeSeconds = getLong(data, "play_time_seconds", 0);
        state.saveCount = getInt(data, "save_count", 0);
        
        return state;
    }
}