package com.questkeeper.dialogue;

import com.questkeeper.character.NPC;
import com.questkeeper.state.GameState;
import com.questkeeper.world.Location;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Manages NPC dialogue interactions.
 *
 * Handles finding NPCs at the player's current location, initiating
 * conversations, displaying greetings, tracking met flags, and
 * processing dialogue topic requests.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class DialogueSystem {

    private NPC currentNpc;
    private GameState currentState;
    private boolean inConversation;

    public DialogueSystem() {
        this.currentNpc = null;
        this.currentState = null;
        this.inConversation = false;
    }

    // ==========================================
    // Dialogue Actions
    // ==========================================

    /**
     * Starts a dialogue with an NPC at the current location.
     *
     * Finds the NPC by name or ID, displays their greeting (first meeting
     * or return greeting), and sets the met_[npc] flag in the game state.
     *
     * @param state the current game state
     * @param npcIdentifier the NPC name or ID to talk to
     * @return the dialogue result containing greeting and available topics
     */
    public DialogueResult startDialogue(GameState state, String npcIdentifier) {
        if (state == null) {
            return DialogueResult.error("No active game state.");
        }

        if (npcIdentifier == null || npcIdentifier.trim().isEmpty()) {
            return DialogueResult.error("Who do you want to talk to?");
        }

        Location currentLocation = state.getCurrentLocation();
        if (currentLocation == null) {
            return DialogueResult.error("You're not in a valid location.");
        }

        // Find NPC at current location
        Optional<NPC> npcOpt = findNpcAtLocation(state, npcIdentifier);
        if (npcOpt.isEmpty()) {
            return DialogueResult.error(
                String.format("There's no one called '%s' here.", npcIdentifier));
        }

        NPC npc = npcOpt.get();
        this.currentNpc = npc;
        this.currentState = state;
        this.inConversation = true;

        // Get greeting (handles first meeting vs return)
        String greeting = npc.greet();

        // Set met flag in game state
        String metFlag = "met_" + npc.getId();
        state.setFlag(metFlag);

        // Build result with greeting and available topics (filtered by game flags)
        List<String> topics = npc.getAvailableTopics(state.getFlags());

        return DialogueResult.success(npc, greeting, topics);
    }

    /**
     * Asks the current NPC about a topic.
     *
     * Uses D&D-style dialogue trees where topics may be gated by game flags.
     * If the topic isn't available, attempts partial matching before failing.
     *
     * @param topic the topic to ask about (case-insensitive)
     * @return the dialogue result with the NPC's response or NO_TOPIC if unknown
     */
    public DialogueResult askAbout(String topic) {
        if (!inConversation || currentNpc == null) {
            return DialogueResult.error("You're not talking to anyone.");
        }

        if (topic == null || topic.trim().isEmpty()) {
            return DialogueResult.error("What do you want to ask about?");
        }

        String normalizedTopic = topic.trim().toLowerCase();
        Set<String> activeFlags = currentState != null ? currentState.getFlags() : Set.of();

        // Check if NPC has this dialogue topic and it's available
        if (!currentNpc.hasDialogue(normalizedTopic) ||
            !currentNpc.isDialogueAvailable(normalizedTopic, activeFlags)) {
            // Try to find a partial match among available topics
            Optional<String> matchedTopic = findMatchingTopic(normalizedTopic, activeFlags);
            if (matchedTopic.isPresent()) {
                normalizedTopic = matchedTopic.get();
            } else {
                return DialogueResult.noTopic(currentNpc, normalizedTopic,
                    currentNpc.getAvailableTopics(activeFlags));
            }
        }

        String response = currentNpc.getDialogue(normalizedTopic);
        return DialogueResult.response(currentNpc, normalizedTopic, response,
            currentNpc.getAvailableTopics(activeFlags));
    }

    /**
     * Ends the current conversation.
     *
     * @return a farewell result indicating the conversation has ended
     */
    public DialogueResult endDialogue() {
        if (!inConversation || currentNpc == null) {
            return DialogueResult.error("You're not in a conversation.");
        }

        String npcName = currentNpc.getName();
        this.currentNpc = null;
        this.currentState = null;
        this.inConversation = false;

        return DialogueResult.ended(npcName);
    }

    // ==========================================
    // State Accessors
    // ==========================================

    /**
     * Checks if currently in a conversation.
     *
     * @return true if talking to an NPC
     */
    public boolean isInConversation() {
        return inConversation;
    }

    /**
     * Gets the NPC currently being talked to.
     *
     * @return the current NPC, or null if not in conversation
     */
    public NPC getCurrentNpc() {
        return currentNpc;
    }

    /**
     * Lists NPCs available to talk to at the current location.
     *
     * @param state the current game state
     * @return list of NPCs at the player's location
     */
    public List<NPC> getNpcsAtCurrentLocation(GameState state) {
        if (state == null || state.getCurrentLocation() == null) {
            return List.of();
        }

        Location location = state.getCurrentLocation();
        List<String> npcIds = location.getNpcs();

        return npcIds.stream()
            .map(id -> state.getCampaign().getNPC(id))
            .filter(npc -> npc != null)
            .toList();
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    /**
     * Finds an NPC at the current location by name or ID.
     */
    private Optional<NPC> findNpcAtLocation(GameState state, String identifier) {
        String searchTerm = identifier.trim().toLowerCase();
        Location location = state.getCurrentLocation();
        List<String> npcIds = location.getNpcs();

        for (String npcId : npcIds) {
            NPC npc = state.getCampaign().getNPC(npcId);
            if (npc != null) {
                // Match by ID or name (case-insensitive)
                if (npc.getId().toLowerCase().equals(searchTerm) ||
                    npc.getName().toLowerCase().equals(searchTerm) ||
                    npc.getName().toLowerCase().contains(searchTerm)) {
                    return Optional.of(npc);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Tries to find a matching topic from available topics.
     */
    private Optional<String> findMatchingTopic(String searchTerm, Set<String> activeFlags) {
        if (currentNpc == null) {
            return Optional.empty();
        }

        List<String> availableTopics = currentNpc.getAvailableTopics(activeFlags);

        // Try exact match first
        for (String topic : availableTopics) {
            if (topic.equals(searchTerm)) {
                return Optional.of(topic);
            }
        }

        // Try partial match
        for (String topic : availableTopics) {
            if (topic.contains(searchTerm) || searchTerm.contains(topic)) {
                return Optional.of(topic);
            }
        }

        return Optional.empty();
    }
}
