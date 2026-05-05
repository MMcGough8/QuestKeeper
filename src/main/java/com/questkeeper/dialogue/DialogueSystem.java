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
        if (response == null || response.isEmpty()) {
            return DialogueResult.noTopic(currentNpc, normalizedTopic,
                currentNpc.getAvailableTopics(activeFlags));
        }
        return DialogueResult.response(currentNpc, normalizedTopic, response,
            currentNpc.getAvailableTopics(activeFlags));
    }

    /**
     * Ends the current conversation.
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
     */
    public boolean isInConversation() {
        return inConversation;
    }

    /**
     * Gets the NPC currently being talked to.
     */
    public NPC getCurrentNpc() {
        return currentNpc;
    }

    /**
     * Gets available dialogue topics for the current conversation.
     */
    public List<String> getAvailableTopics() {
        if (currentNpc == null || currentState == null) {
            return List.of();
        }
        Set<String> flags = currentState.getFlags();
        return currentNpc.getAvailableTopics(flags);
    }

    /**
     * Lists NPCs available to talk to at the current location.
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
     * Finds an NPC at the current location by name, ID, role descriptor,
     * or near-miss typo. Match order:
     *   1. Exact ID / exact name / name substring
     *   2. Role descriptor (e.g., "the announcer" -> Herald Vox, "an announcer")
     *   3. Levenshtein distance <= 1 against any name token (catches typos
     *      like "vax" for "Vox" without false-positives on short inputs)
     */
    private Optional<NPC> findNpcAtLocation(GameState state, String identifier) {
        String raw = identifier.trim().toLowerCase();
        String searchTerm = stripLeadingArticles(raw);
        Location location = state.getCurrentLocation();
        List<String> npcIds = location.getNpcs();

        // Pass 1: exact / contains / descriptor.
        for (String npcId : npcIds) {
            NPC npc = state.getCampaign().getNPC(npcId);
            if (npc == null) continue;
            String name = npc.getName().toLowerCase();
            if (npc.getId().toLowerCase().equals(searchTerm)
                    || name.equals(searchTerm)
                    || name.contains(searchTerm)) {
                return Optional.of(npc);
            }
            String descriptor = stripLeadingArticles(npc.getShortDescriptor().toLowerCase());
            if (!descriptor.isEmpty()
                    && (descriptor.equals(searchTerm) || descriptor.contains(searchTerm))) {
                return Optional.of(npc);
            }
        }

        // Pass 2: typo fallback. Only fires for >=4-char single-word searches
        // so "vax" -> "vox" (Levenshtein 1) but "to" or "an" stay no-ops.
        if (searchTerm.length() >= 4 && !searchTerm.contains(" ")) {
            for (String npcId : npcIds) {
                NPC npc = state.getCampaign().getNPC(npcId);
                if (npc == null) continue;
                for (String token : npc.getName().toLowerCase().split("\\s+")) {
                    if (token.length() >= 3 && levenshtein(token, searchTerm) <= 1) {
                        return Optional.of(npc);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static String stripLeadingArticles(String s) {
        String r = s.trim();
        for (String a : new String[]{"the ", "a ", "an ", "to "}) {
            if (r.startsWith(a)) r = r.substring(a.length());
        }
        return r.trim();
    }

    /** Levenshtein edit distance (insert/delete/substitute). */
    private static int levenshtein(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                                   d[i - 1][j - 1] + cost);
            }
        }
        return d[a.length()][b.length()];
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
