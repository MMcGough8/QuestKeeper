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