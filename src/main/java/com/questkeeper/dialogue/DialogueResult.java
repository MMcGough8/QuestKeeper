package com.questkeeper.dialogue;

import com.questkeeper.character.NPC;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a dialogue interaction.
 *
 * Contains the NPC response, available topics, and status information
 * for display by the UI layer.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class DialogueResult {

    public enum Type {
        SUCCESS,      // Successfully started conversation or got response
        NO_TOPIC,     // NPC doesn't know about that topic
        ERROR,        // Something went wrong
        ENDED         // Conversation ended
    }

    private final Type type;
    private final NPC npc;
    private final String message;
    private final String topic;
    private final List<String> availableTopics;

    private DialogueResult(Type type, NPC npc, String message, String topic,
                          List<String> availableTopics) {
        this.type = type;
        this.npc = npc;
        this.message = message;
        this.topic = topic;
        this.availableTopics = availableTopics != null ?
            Collections.unmodifiableList(availableTopics) : List.of();
    }

    // ==========================================
    // Factory Methods
    // ==========================================

    /**
     * Creates a successful dialogue start result with greeting.
     */
    public static DialogueResult success(NPC npc, String greeting, List<String> topics) {
        return new DialogueResult(Type.SUCCESS, npc, greeting, null, topics);
    }

    /**
     * Creates a successful topic response result.
     */
    public static DialogueResult response(NPC npc, String topic, String response,
                                         List<String> topics) {
        return new DialogueResult(Type.SUCCESS, npc, response, topic, topics);
    }

    /**
     * Creates a result for when the NPC doesn't know about a topic.
     */
    public static DialogueResult noTopic(NPC npc, String topic, List<String> topics) {
        String message = String.format("%s doesn't seem to know about '%s'.",
            npc.getName(), topic);
        return new DialogueResult(Type.NO_TOPIC, npc, message, topic, topics);
    }

    /**
     * Creates an error result.
     */
    public static DialogueResult error(String message) {
        return new DialogueResult(Type.ERROR, null, message, null, List.of());
    }

    /**
     * Creates a conversation ended result.
     */
    public static DialogueResult ended(String npcName) {
        String message = String.format("You end your conversation with %s.", npcName);
        return new DialogueResult(Type.ENDED, null, message, null, List.of());
    }

    // ==========================================
    // Accessors
    // ==========================================

    public Type getType() {
        return type;
    }

    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }

    public boolean isError() {
        return type == Type.ERROR;
    }

    public NPC getNpc() {
        return npc;
    }

    public String getNpcName() {
        return npc != null ? npc.getName() : null;
    }

    public String getMessage() {
        return message;
    }

    public String getTopic() {
        return topic;
    }

    public List<String> getAvailableTopics() {
        return availableTopics;
    }

    public boolean hasTopics() {
        return !availableTopics.isEmpty();
    }

    // ==========================================
    // Display
    // ==========================================

    /**
     * Formats the result for display in the game UI.
     * Includes NPC name prefix, message, and available topics if applicable.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();

        if (npc != null && type != Type.ENDED) {
            sb.append(npc.getName()).append(": ");
        }

        sb.append(message);

        if (hasTopics() && (type == Type.SUCCESS || type == Type.NO_TOPIC)) {
            sb.append("\n\nYou can ask about: ");
            sb.append(String.join(", ", availableTopics));
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("DialogueResult[type=%s, npc=%s, message=%s]",
            type,
            npc != null ? npc.getName() : "null",
            message != null ? message.substring(0, Math.min(50, message.length())) : "null");
    }
}
