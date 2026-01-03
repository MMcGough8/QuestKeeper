package com.questkeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Represents a non-player character in the game world.
 * 
 * NPCs have personality traits, dialogue options, and can be shopkeepers.
 * Uses ID references and String fields for YAML serialization compatibility.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class NPC {

    private static final String FLAG_MET_PLAYER = "met_player";
    private static final Random random = new Random();

    private final String id;
    private String name;
    private String role;                          // e.g., "bartender", "shopkeeper", "informant"
    private String voice;                         // e.g., "sing-song", "no-nonsense", "intense"
    private String personality;                   // e.g., "friendly", "suspicious", "theatrical"
    private String description;                   // Physical appearance
    private String locationId;                    // Where NPC is located (null if mobile)

    private final Map<String, String> dialogues;  // topic -> response
    private String greeting;                      // First interaction greeting
    private String returnGreeting;                // Greeting after first meeting
    private final List<String> sampleLines;       // Roleplay flavor lines
    private final Set<String> flags;              // Relationship state

    private boolean shopkeeper;                   // Can sell items

    public NPC(String id, String name) {
        this(id, name, "", "", "");
    }

    /**
     * Creates an NPC with core personality properties.
     */
    public NPC(String id, String name, String role, String voice, String personality) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("NPC ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("NPC name cannot be null or empty");
        }

        this.id = id;
        this.name = name;
        this.role = role != null ? role : "";
        this.voice = voice != null ? voice : "";
        this.personality = personality != null ? personality : "";
        this.description = "";
        this.locationId = null;

        this.dialogues = new HashMap<>();
        this.greeting = "";
        this.returnGreeting = "";
        this.sampleLines = new ArrayList<>();
        this.flags = new HashSet<>();

        this.shopkeeper = false;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role : "";
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice != null ? voice : "";
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality != null ? personality : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public boolean isShopkeeper() {
        return shopkeeper;
    }

    public void setShopkeeper(boolean shopkeeper) {
        this.shopkeeper = shopkeeper;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting != null ? greeting : "";
    }

    public String getReturnGreeting() {
        return returnGreeting;
    }

    public void setReturnGreeting(String returnGreeting) {
        this.returnGreeting = returnGreeting != null ? returnGreeting : "";
    }

    public String greet() {
        if (hasMetPlayer()) {
            return returnGreeting.isEmpty() ? greeting : returnGreeting;
        } else {
            markAsMet();
            return greeting;
        }
    }

    public String peekGreeting() {
        if (hasMetPlayer()) {
            return returnGreeting.isEmpty() ? greeting : returnGreeting;
        }
        return greeting;
    }
