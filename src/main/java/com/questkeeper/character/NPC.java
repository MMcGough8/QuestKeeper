package com.questkeeper.character;

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
    private final Map<String, String> dialogueRequiredFlags;  // topic -> required flag
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
        this.dialogueRequiredFlags = new HashMap<>();
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

    public String getDialogue(String topic) {
        if (topic == null) {
            return null;
        }
        return dialogues.get(topic.toLowerCase());
    }

    public boolean hasDialogue(String topic) {
        return topic != null && dialogues.containsKey(topic.toLowerCase());
    }

    public void addDialogue(String topic, String response) {
        if (topic != null && !topic.trim().isEmpty() && response != null) {
            dialogues.put(topic.toLowerCase(), response);
        }
    }

    /**
     * Adds a dialogue that requires a specific flag to be available.
     */
    public void addDialogue(String topic, String response, String requiredFlag) {
        addDialogue(topic, response);
        if (requiredFlag != null && !requiredFlag.trim().isEmpty()) {
            dialogueRequiredFlags.put(topic.toLowerCase(), requiredFlag.toLowerCase());
        }
    }

    /**
     * Sets a required flag for an existing dialogue topic.
     */
    public void setDialogueRequiredFlag(String topic, String requiredFlag) {
        if (topic != null && requiredFlag != null && !requiredFlag.trim().isEmpty()) {
            dialogueRequiredFlags.put(topic.toLowerCase(), requiredFlag.toLowerCase());
        }
    }

    /**
     * Gets the required flag for a dialogue topic, or null if none.
     */
    public String getDialogueRequiredFlag(String topic) {
        return topic != null ? dialogueRequiredFlags.get(topic.toLowerCase()) : null;
    }

    public void removeDialogue(String topic) {
        if (topic != null) {
            dialogues.remove(topic.toLowerCase());
            dialogueRequiredFlags.remove(topic.toLowerCase());
        }
    }

    /**
     * Gets all topics (regardless of flag requirements).
     */
    public List<String> getAvailableTopics() {
        List<String> topics = new ArrayList<>(dialogues.keySet());
        Collections.sort(topics);
        return Collections.unmodifiableList(topics);
    }

    /**
     * Gets topics available based on active game flags.
     * Topics with no required flag are always available.
     * Topics with a required flag are only available if that flag is in activeFlags.
     */
    public List<String> getAvailableTopics(Set<String> activeFlags) {
        Set<String> normalizedFlags = new HashSet<>();
        if (activeFlags != null) {
            for (String flag : activeFlags) {
                normalizedFlags.add(flag.toLowerCase());
            }
        }

        List<String> topics = new ArrayList<>();
        for (String topic : dialogues.keySet()) {
            String requiredFlag = dialogueRequiredFlags.get(topic);
            if (requiredFlag == null || normalizedFlags.contains(requiredFlag)) {
                topics.add(topic);
            }
        }
        Collections.sort(topics);
        return Collections.unmodifiableList(topics);
    }

    /**
     * Checks if a dialogue topic is available based on active flags.
     */
    public boolean isDialogueAvailable(String topic, Set<String> activeFlags) {
        if (topic == null || !dialogues.containsKey(topic.toLowerCase())) {
            return false;
        }

        String requiredFlag = dialogueRequiredFlags.get(topic.toLowerCase());
        if (requiredFlag == null) {
            return true; // No flag required
        }

        if (activeFlags == null) {
            return false;
        }

        for (String flag : activeFlags) {
            if (flag.equalsIgnoreCase(requiredFlag)) {
                return true;
            }
        }
        return false;
    }

    public int getDialogueCount() {
        return dialogues.size();
    }

    public List<String> getSampleLines() {
        return Collections.unmodifiableList(sampleLines);
    }

    public void addSampleLine(String line) {
        if (line != null && !line.trim().isEmpty()) {
            sampleLines.add(line);
        }
    }

    public String getRandomSampleLine() {
        if (sampleLines.isEmpty()) {
            return "";
        }
        return sampleLines.get(random.nextInt(sampleLines.size()));
    }

    public int getSampleLineCount() {
        return sampleLines.size();
    }

    public boolean hasMetPlayer() {
        return flags.contains(FLAG_MET_PLAYER);
    }

    public void markAsMet() {
        flags.add(FLAG_MET_PLAYER);
    }

    public void resetMet() {
        flags.remove(FLAG_MET_PLAYER);
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

    public String getRoleplayPrompt() {
        StringBuilder sb = new StringBuilder();
        if (!role.isEmpty()) {
            sb.append(role);
        }
        if (!voice.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(voice).append(" voice");
        }
        if (!personality.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(personality);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("NPC[id=%s, name=%s, role=%s, topics=%d]",
                id, name, role, dialogues.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NPC other = (NPC) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
