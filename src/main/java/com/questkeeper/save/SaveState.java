package com.questkeeper.save;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;

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
    private List<String> equippedItems;  // Legacy format for backwards compatibility
    private Map<String, String> equippedSlots;  // New format: slot name -> item ID
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
        this.equippedSlots = new HashMap<>();
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
     *
     * @throws IOException if the file cannot be read or is corrupted
     */
    public static SaveState load(Path filepath) throws IOException {
        if (!Files.exists(filepath)) {
            throw new IOException("Save file not found: " + filepath);
        }

        Yaml yaml = new Yaml();

        try (Reader reader = Files.newBufferedReader(filepath)) {
            Object loaded = yaml.load(reader);
            if (loaded == null) {
                throw new IOException("Save file is empty: " + filepath);
            }
            if (!(loaded instanceof Map)) {
                throw new IOException("Save file has invalid format (expected YAML map): " + filepath);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) loaded;
            return fromMap(data, filepath.toString());
        } catch (ClassCastException e) {
            throw new IOException("Save file has corrupted data structure: " + filepath + " - " + e.getMessage(), e);
        } catch (org.yaml.snakeyaml.scanner.ScannerException e) {
            throw new IOException("Save file has invalid YAML syntax: " + filepath + " - " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Save file has invalid data: " + filepath + " - " + e.getMessage(), e);
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
     * Corrupted save files are logged to stderr but still skipped.
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
                          // Log corrupted save files so user knows there's an issue
                          System.err.println("Warning: Could not read save file " +
                              p.getFileName() + ": " + e.getMessage());
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
        map.put("equipped", equippedItems);  // Legacy format
        map.put("equipped_slots", equippedSlots);  // New format with slot info
        map.put("gold", gold);
        
        map.put("play_time_seconds", totalPlayTimeSeconds);
        map.put("save_count", saveCount);
        
        return map;
    }

    private static SaveState fromMap(Map<String, Object> data, String source) {
        SaveState state = new SaveState();

        // Metadata
        state.saveVersion = getString(data, "save_version", CURRENT_VERSION);
        String timestampStr = getString(data, "timestamp", null);
        if (timestampStr != null) {
            try {
                state.timestamp = Instant.parse(timestampStr);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid timestamp format in " + source + ": " + timestampStr);
            }
        }
        state.campaignId = getString(data, "campaign_id", "unknown");
        state.saveName = getString(data, "save_name", "Unnamed Save");

        // Character - with defensive type checking
        Object charObj = data.get("character");
        if (charObj != null) {
            if (!(charObj instanceof Map)) {
                throw new IllegalArgumentException("Character data is not a valid map in " + source);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> charData = (Map<String, Object>) charObj;
            state.character = CharacterData.fromMap(charData);
        }

        // World state
        state.currentLocationId = getString(data, "current_location", null);
        state.visitedLocations = getStringSet(data, "visited_locations");

        // Campaign progress - with defensive type checking
        state.stateFlags = getBooleanMap(data, "flags");
        state.stateCounters = getIntegerMap(data, "counters");
        state.stateStrings = getStringMap(data, "strings");

        // Inventory
        state.inventoryItems = getStringList(data, "inventory");
        state.equippedItems = getStringList(data, "equipped");  // Legacy format
        state.equippedSlots = getStringMap(data, "equipped_slots");  // New format
        state.gold = getInt(data, "gold", 0);

        // Stats
        state.totalPlayTimeSeconds = getLong(data, "play_time_seconds", 0);
        state.saveCount = getInt(data, "save_count", 0);

        return state;
    }

    /**
     * Safely extracts a list of strings from YAML data.
     */
    private static List<String> getStringList(Map<String, Object> data, String key) {
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
        return new ArrayList<>();
    }

    /**
     * Safely extracts a set of strings from YAML data.
     */
    private static Set<String> getStringSet(Map<String, Object> data, String key) {
        return new HashSet<>(getStringList(data, key));
    }

    /**
     * Safely extracts a map with string keys and boolean values.
     */
    private static Map<String, Boolean> getBooleanMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Boolean> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String keyStr && entry.getValue() instanceof Boolean boolVal) {
                    result.put(keyStr, boolVal);
                }
            }
            return result;
        }
        return new HashMap<>();
    }

    /**
     * Safely extracts a map with string keys and integer values.
     */
    private static Map<String, Integer> getIntegerMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Integer> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String keyStr && entry.getValue() instanceof Number numVal) {
                    result.put(keyStr, numVal.intValue());
                }
            }
            return result;
        }
        return new HashMap<>();
    }

    /**
     * Safely extracts a map with string keys and string values.
     */
    private static Map<String, String> getStringMap(Map<String, Object> data, String key) {
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
        return new HashMap<>();
    }

    public void setFlag(String key, boolean value) {
        stateFlags.put(key, value);
    }

    public boolean getFlag(String key) {
        return stateFlags.getOrDefault(key, false);
    }

    public boolean hasFlag(String key) {
        return stateFlags.getOrDefault(key, false);
    }

    public void setCounter(String key, int value) {
        stateCounters.put(key, value);
    }

    public int getCounter(String key) {
        return stateCounters.getOrDefault(key, 0);
    }

    public void incrementCounter(String key) {
        stateCounters.merge(key, 1, Integer::sum);
    }

    public void setString(String key, String value) {
        stateStrings.put(key, value);
    }

    public String getString(String key) {
        return stateStrings.get(key);
    }

    public void setCurrentLocation(String locationId) {
        this.currentLocationId = locationId;
        if (locationId != null) {
            this.visitedLocations.add(locationId);
        }
    }

    public boolean hasVisited(String locationId) {
        return visitedLocations.contains(locationId);
    }

    public void addVisitedLocation(String locationId) {
        if (locationId != null) {
            visitedLocations.add(locationId);
        }
    }

    public Character restoreCharacter() {
        if (character == null) {
            throw new IllegalStateException("No character data in save state");
        }
        return character.toCharacter();
    }

    public void updateCharacter(Character c) {
        this.character = CharacterData.fromCharacter(c);
    }

    public void addItem(String itemId) {
        inventoryItems.add(itemId);
    }

    public void removeItem(String itemId) {
        inventoryItems.remove(itemId);
    }

    public void equipItem(String itemId) {
        if (!equippedItems.contains(itemId)) {
            equippedItems.add(itemId);
        }
    }

    /**
     * Equips an item to a specific slot (new format with slot preservation).
     */
    public void equipItemToSlot(String slotName, String itemId) {
        equippedSlots.put(slotName, itemId);
        // Also add to legacy list for backwards compatibility
        if (!equippedItems.contains(itemId)) {
            equippedItems.add(itemId);
        }
    }

    public void unequipItem(String itemId) {
        equippedItems.remove(itemId);
    }

    /**
     * Checks if we have slot-based equipment info (new format).
     */
    public boolean hasEquippedSlots() {
        return equippedSlots != null && !equippedSlots.isEmpty();
    }

    public Map<String, String> getEquippedSlots() {
        return Collections.unmodifiableMap(equippedSlots);
    }

    public void addGold(int amount) {
        this.gold += amount;
    }

    public boolean spendGold(int amount) {
        if (gold >= amount) {
            gold -= amount;
            return true;
        }
        return false;
    }

    public void addPlayTime(long seconds) {
        this.totalPlayTimeSeconds += seconds;
    }

    public String getFormattedPlayTime() {
        long hours = totalPlayTimeSeconds / 3600;
        long minutes = (totalPlayTimeSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return((Number) val).intValue();
        }
        return defaultValue;
    }

    private static long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return((Number) val).longValue();
        }
        return defaultValue;
    }

    public String getSaveVersion() { 
        return saveVersion; 
    }

    public Instant getTimestamp() { 
        return timestamp; 
    }

    public String getCampaignId() { 
        return campaignId; 
    }

    public String getSaveName() { 
        return saveName; 
    }

    public String getCurrentLocationId() { 
        return currentLocationId; 
    }

    public Set<String> getVisitedLocations() { 
        return Collections.unmodifiableSet(visitedLocations); 
    }

    public Map<String, Boolean> getStateFlags() { 
        return Collections.unmodifiableMap(stateFlags); 
    }

    public Map<String, Integer> getStateCounters() { 
        return Collections.unmodifiableMap(stateCounters); 
    }

    public Map<String, String> getStateStrings() { 
        return Collections.unmodifiableMap(stateStrings); 
    }

    public List<String> getInventoryItems() { 
        return Collections.unmodifiableList(inventoryItems); 
    }

    public List<String> getEquippedItems() { 
        return Collections.unmodifiableList(equippedItems); 
    }

    public int getGold() { 
        return gold; 
    }

    public long getTotalPlayTimeSeconds() { 
        return totalPlayTimeSeconds; 
    }

    public int getSaveCount() { 
        return saveCount; 
    }

    public void setSaveName(String saveName) { 
        this.saveName = saveName; 
    }

    public void setCampaignId(String campaignId) { 
        this.campaignId = campaignId; 
    }

    @Override
    public String toString() {
        return String.format("SaveState[%s, %s, %s, played: %s]",
            saveName, campaignId, timestamp, getFormattedPlayTime());
    }

    /**
     * Lightweight save file info for listing saves without full load.
     */
    public record SaveInfo(
        Path path,
        String saveName,
        String campaignId,
        String characterName,
        int characterLevel,
        Instant timestamp,
        String playTime
    ) {
        public static SaveInfo fromFile(Path path) throws IOException {
            Yaml yaml = new Yaml();
            try (Reader reader = Files.newBufferedReader(path)) {
                Object loaded = yaml.load(reader);
                if (loaded == null) {
                    throw new IOException("Save file is empty: " + path.getFileName());
                }
                if (!(loaded instanceof Map)) {
                    throw new IOException("Save file has invalid format: " + path.getFileName());
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) loaded;

                String saveName = getString(data, "save_name", "Unknown");
                String campaignId = getString(data, "campaign_id", "unknown");

                Instant timestamp;
                String timestampStr = getString(data, "timestamp", null);
                if (timestampStr != null) {
                    try {
                        timestamp = Instant.parse(timestampStr);
                    } catch (Exception e) {
                        timestamp = Instant.now();
                    }
                } else {
                    timestamp = Instant.now();
                }

                String characterName = "Unknown";
                int characterLevel = 1;
                Object charObj = data.get("character");
                if (charObj instanceof Map<?, ?> charMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> charData = (Map<String, Object>) charMap;
                    characterName = getString(charData, "name", "Unknown");
                    characterLevel = getInt(charData, "level", 1);
                }

                long playSeconds = getLong(data, "play_time_seconds", 0);
                long hours = playSeconds / 3600;
                long minutes = (playSeconds % 3600) / 60;
                String playTime = hours > 0 ? String.format("%dh %dm", hours, minutes) : String.format("%dm", minutes);

                return new SaveInfo(path, saveName, campaignId, characterName, characterLevel, timestamp, playTime);
            } catch (org.yaml.snakeyaml.scanner.ScannerException e) {
                throw new IOException("Save file has invalid YAML syntax: " + path.getFileName(), e);
            } catch (ClassCastException e) {
                throw new IOException("Save file has corrupted data: " + path.getFileName(), e);
            }
        }

        private static String getString(Map<String, Object> map, String key, String defaultValue) {
            Object val = map.get(key);
            return val != null ? val.toString() : defaultValue;
        }

        private static int getInt(Map<String, Object> map, String key, int defaultValue) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            return defaultValue;
        }

        private static long getLong(Map<String, Object> map, String key, long defaultValue) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
            return defaultValue;
        }
    }
}
