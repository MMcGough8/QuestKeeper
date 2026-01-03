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

    private static final String FLAG_VISITED = "visited";
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

    public Set<String> getExits() {
        return Collections.unmodifiableSet(exits.keySet());
    }

    public String getExit (String direction){
        if (direction == null) {
            return null;
        }
        return exits.get(direction.toLowerCase());
    }
    
    public boolean hasExit(String direction) {
        return direction != null && exits.containsKey(direction.toLowerCase());
    }

    public void addExit(String direction, String locationId) {
        if (direction != null && !direction.trim().isEmpty() &&
            locationId != null && !locationId.trim().isEmpty()) {
            exits.put(direction.toLowerCase(), locationId);
        }
    }

    public void removeExit(String direction) {
        if (direction != null) {
            exits.remove(direction.toLowerCase());
        }
    }

     public int getExitCount() {
        return exits.size();
    }

    public List<String> getNpcs() {
        return Collections.unmodifiableList(npcs);
    }

    public boolean hasNpc(String npcId) {
        return npcId != null && npcs.contains(npcId);
    }

    public void addNpc(String npcId) {
        if (npcId != null && !npcId.trim().isEmpty() && !npcs.contains(npcId)) {
            npcs.add(npcId);
        }
    }

    public boolean removeNpc(String npcId) {
        return npcId != null && npcs.remove(npcId);
    }

    public int getNpcCount() {
        return npcs.size();
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean hasItem(String itemId) {
        return itemId != null && items.contains(itemId);
    }

    public void addItem(String itemId) {
        if (itemId != null && !itemId.trim().isEmpty()) {
            items.add(itemId);  // Allow duplicates (multiple of same item)
        }
    }

    public boolean removeItem(String itemId) {
        return itemId != null && items.remove(itemId);
    }

    public int getItemCount() {
        return items.size();
    }

    public boolean hasBeenVisited() {
        return flags.contains(FLAG_VISITED);
    }

    public void markVisited() {
        flags.add(FLAG_VISITED);
    }

    public boolean isUnlocked() {
        return flags.contains(FLAG_UNLOCKED);
    }

    public void unlock() {
        flags.add(FLAG_UNLOCKED);
    }

    public void lock() {
        flags.remove(FLAG_UNLOCKED);
    }

    public boolean hasFlag(String flag) {
        return flag != null && flags.contains(flag.toLowerCase());
    }

    public void setFlag(String flag) {
        if (flag != null && !flag.trim().isEmpty()) {
            flags.add(flag.toLowerCase());
        }
    }

    public void removeFlag(String flag) {
        if (flag != null) {
            flags.remove(flag.toLowerCase());
        }
    }

    public Set<String> getFlags() {
        return Collections.unmodifiableSet(flags);
    }
}