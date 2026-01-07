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
