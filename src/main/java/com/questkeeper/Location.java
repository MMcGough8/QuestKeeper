package com.questkeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a location in the game world.
 * 
 * Locations are connected via exits and can contain NPCs and items.
 * Uses ID references for YAML serialization compatibility.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class Location {

    private static final String FLAG_Visited = "visited";
    private static final String FLAG_UNLOCKED = "unlocked";

    private final String id;
    private String name;
    private String description;
    private String readAloudText;

    private final Map<String, String> exits;
    private final List<String> npcs;
    private final List<String> items; 
    private final Set<String> flags;

    public Location(String id, String name) {
        this(id, name, "", "");
    }

    public Location(String id, String name, String description, String readAloudText) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be null or empty");
        }

        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.readAloudText = readAloudText != null ? readAloudText : "";

        this.exits = new HashMap<>();
        this.npcs = new ArrayList<>();
        this.items = new ArrayList<>();
        this.flags = new HashSet<>();

        this.flags.add(FLAG_UNLOCKED);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getReadAloudText() {
        return readAloudText;
    }

    public void setReadAloudText(String readAloudText) {
        this.readAloudText = readAloudText != null ? readAloudText : "";
    }

    public String getDisplayDescription() {
        if (!hasBeenVisited() && !readAloudText.isEmpty()) {
            return readAloudText;
        }
        return description;
    }
    }