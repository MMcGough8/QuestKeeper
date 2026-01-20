package com.questkeeper.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parses player input into structured Command objects.
 * 
 * Handles synonym mapping (e.g., "walk" → "go", "examine" → "look")
 * and extracts verbs and nouns from natural language input.
 * 
 * @author Marc McGough
 * @version 1.0
 */

public class CommandParser {
    
    private static final String VERB_GO = "go";
    private static final String VERB_LOOK = "look";
    private static final String VERB_TAKE = "take";
    private static final String VERB_DROP = "drop";
    private static final String VERB_USE = "use";
    private static final String VERB_TALK = "talk";
    private static final String VERB_ASK = "ask";
    private static final String VERB_ATTACK = "attack";
    private static final String VERB_CAST = "cast";
    private static final String VERB_INVENTORY = "inventory";
    private static final String VERB_STATS = "stats";
    private static final String VERB_HELP = "help";
    private static final String VERB_SAVE = "save";
    private static final String VERB_LOAD = "load";
    private static final String VERB_QUIT = "quit";
    private static final String VERB_REST = "rest";
    private static final String VERB_OPEN = "open";
    private static final String VERB_CLOSE = "close";
    private static final String VERB_READ = "read";
    private static final String VERB_EQUIP = "equip";
    private static final String VERB_UNEQUIP = "unequip";
    private static final String VERB_BYE = "bye";
    private static final String VERB_TRIAL = "trial";
    private static final String VERB_ATTEMPT = "attempt";
    private static final String VERB_EQUIPMENT = "equipment";
    private static final String VERB_LEAVE = "leave";

    private static final Map<String, String> SYNONYM_MAP = new HashMap<>();

    private static final Set<String> VALID_VERBS = Set.of(
        VERB_GO, VERB_LOOK, VERB_TAKE, VERB_DROP, VERB_USE, VERB_TALK, VERB_ASK,
        VERB_ATTACK, VERB_CAST, VERB_INVENTORY, VERB_STATS, VERB_HELP, VERB_SAVE,
        VERB_LOAD, VERB_QUIT, VERB_REST, VERB_OPEN, VERB_CLOSE, VERB_READ,
        VERB_EQUIP, VERB_UNEQUIP, VERB_BYE, VERB_TRIAL, VERB_ATTEMPT, VERB_EQUIPMENT,
        VERB_LEAVE, "exit"
    );

    private static final Map<String, String> DIRECTION_ALIASES = Map.of(
        "n", "north",
        "s", "south",
        "e", "east",
        "w", "west",
        "u", "up",
        "d", "down"
    ); 

    static {
        // Movement synonyms → "go"
        SYNONYM_MAP.put("go", VERB_GO);
        SYNONYM_MAP.put("walk", VERB_GO);
        SYNONYM_MAP.put("move", VERB_GO);
        SYNONYM_MAP.put("travel", VERB_GO);
        SYNONYM_MAP.put("head", VERB_GO);
        SYNONYM_MAP.put("run", VERB_GO);
        
        // Look synonyms → "look"
        SYNONYM_MAP.put("look", VERB_LOOK);
        SYNONYM_MAP.put("examine", VERB_LOOK);
        SYNONYM_MAP.put("inspect", VERB_LOOK);
        SYNONYM_MAP.put("check", VERB_LOOK);
        SYNONYM_MAP.put("view", VERB_LOOK);
        SYNONYM_MAP.put("observe", VERB_LOOK);
        SYNONYM_MAP.put("search", VERB_LOOK);
        SYNONYM_MAP.put("l", VERB_LOOK);
        
        // Take synonyms → "take"
        SYNONYM_MAP.put("take", VERB_TAKE);
        SYNONYM_MAP.put("get", VERB_TAKE);
        SYNONYM_MAP.put("grab", VERB_TAKE);
        SYNONYM_MAP.put("pick", VERB_TAKE);
        SYNONYM_MAP.put("pickup", VERB_TAKE);
        SYNONYM_MAP.put("collect", VERB_TAKE);
        
        // Drop synonyms → "drop"
        SYNONYM_MAP.put("drop", VERB_DROP);
        SYNONYM_MAP.put("discard", VERB_DROP);
        SYNONYM_MAP.put("throw", VERB_DROP);
        SYNONYM_MAP.put("toss", VERB_DROP);
        
        // Use synonyms → "use"
        SYNONYM_MAP.put("use", VERB_USE);
        SYNONYM_MAP.put("activate", VERB_USE);
        SYNONYM_MAP.put("apply", VERB_USE);
        
        // Talk synonyms → "talk"
        SYNONYM_MAP.put("talk", VERB_TALK);
        SYNONYM_MAP.put("speak", VERB_TALK);
        SYNONYM_MAP.put("chat", VERB_TALK);
        SYNONYM_MAP.put("converse", VERB_TALK);

        // Ask synonyms → "ask"
        SYNONYM_MAP.put("ask", VERB_ASK);
        SYNONYM_MAP.put("inquire", VERB_ASK);
        SYNONYM_MAP.put("question", VERB_ASK);
        
        // Attack synonyms → "attack"
        SYNONYM_MAP.put(VERB_ATTACK, VERB_ATTACK);
        SYNONYM_MAP.put("hit", VERB_ATTACK);
        SYNONYM_MAP.put("strike", VERB_ATTACK);
        SYNONYM_MAP.put("fight", VERB_ATTACK);
        SYNONYM_MAP.put("kill", VERB_ATTACK);
        SYNONYM_MAP.put("slay", VERB_ATTACK);
        
        // Cast synonyms → "cast"
        SYNONYM_MAP.put("cast", VERB_CAST);
        SYNONYM_MAP.put("spell", VERB_CAST);
        
        // Inventory synonyms → "inventory"
        SYNONYM_MAP.put(VERB_INVENTORY, VERB_INVENTORY);
        SYNONYM_MAP.put("inv", VERB_INVENTORY);
        SYNONYM_MAP.put("i", VERB_INVENTORY);
        SYNONYM_MAP.put("items", VERB_INVENTORY);
        SYNONYM_MAP.put("bag", VERB_INVENTORY);
        
        // Stats synonyms → "stats"
        SYNONYM_MAP.put(VERB_STATS, VERB_STATS);
        SYNONYM_MAP.put("status", VERB_STATS);
        SYNONYM_MAP.put("character", VERB_STATS);
        SYNONYM_MAP.put("char", VERB_STATS);
        SYNONYM_MAP.put("me", VERB_STATS);
        
        // Help synonyms → "help"
        SYNONYM_MAP.put("help", VERB_HELP);
        SYNONYM_MAP.put("?", VERB_HELP);
        SYNONYM_MAP.put("commands", VERB_HELP);
        
        // Save/Load → direct mapping
        SYNONYM_MAP.put("save", VERB_SAVE);
        SYNONYM_MAP.put("load", VERB_LOAD);
        SYNONYM_MAP.put("restore", VERB_LOAD);
        
        // Quit synonyms → "quit"
        SYNONYM_MAP.put("quit", VERB_QUIT);
        SYNONYM_MAP.put("q", VERB_QUIT);
        // Note: "bye" and "farewell" are handled as dialogue-ending commands
        // Note: "leave" and "exit" are now separate commands for leaving locations

        // Dialogue ending → "bye"
        SYNONYM_MAP.put("bye", VERB_BYE);
        SYNONYM_MAP.put("farewell", VERB_BYE);
        SYNONYM_MAP.put("goodbye", VERB_BYE);
        SYNONYM_MAP.put("later", VERB_BYE);

        // Leave/exit location → "leave"
        SYNONYM_MAP.put("leave", VERB_LEAVE);
        SYNONYM_MAP.put("exit", VERB_LEAVE);

        // Rest synonyms → "rest"
        SYNONYM_MAP.put("rest", VERB_REST);
        SYNONYM_MAP.put("sleep", VERB_REST);
        SYNONYM_MAP.put("camp", VERB_REST);
        
        // Open/Close → direct mapping
        SYNONYM_MAP.put("open", VERB_OPEN);
        SYNONYM_MAP.put(VERB_CLOSE, VERB_CLOSE);
        SYNONYM_MAP.put("shut", VERB_CLOSE);
        
        // Read → direct mapping
        SYNONYM_MAP.put("read", VERB_READ);
        
        // Equip synonyms → "equip"
        SYNONYM_MAP.put(VERB_EQUIP, VERB_EQUIP);
        SYNONYM_MAP.put("wear", VERB_EQUIP);
        SYNONYM_MAP.put("wield", VERB_EQUIP);
        SYNONYM_MAP.put("hold", VERB_EQUIP);
        
        // Unequip synonyms → "unequip"
        SYNONYM_MAP.put(VERB_UNEQUIP, VERB_UNEQUIP);
        SYNONYM_MAP.put("remove", VERB_UNEQUIP);

        // Trial synonyms → "trial"
        SYNONYM_MAP.put(VERB_TRIAL, VERB_TRIAL);
        SYNONYM_MAP.put("trials", VERB_TRIAL);
        SYNONYM_MAP.put("challenge", VERB_TRIAL);
        SYNONYM_MAP.put("puzzle", VERB_TRIAL);

        // Attempt synonyms → "attempt"
        SYNONYM_MAP.put(VERB_ATTEMPT, VERB_ATTEMPT);
        SYNONYM_MAP.put("try", VERB_ATTEMPT);
        SYNONYM_MAP.put("solve", VERB_ATTEMPT);
        SYNONYM_MAP.put("do", VERB_ATTEMPT);

        // Equipment synonyms → "equipment"
        SYNONYM_MAP.put(VERB_EQUIPMENT, VERB_EQUIPMENT);
        SYNONYM_MAP.put("equipped", VERB_EQUIPMENT);
        SYNONYM_MAP.put("gear", VERB_EQUIPMENT);
        SYNONYM_MAP.put("worn", VERB_EQUIPMENT);
    }
    
    private CommandParser() {

    }

    public static Command parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Command.empty();
        }

        String cleaned = cleanInput(input);
        String verb = extractVerb(cleaned);
        String noun = extractNoun(cleaned);

        String normalizedVerb = normalizeVerb(verb);

        if (VERB_GO.equals(normalizedVerb) && noun != null) {
            noun = normalizeDirection(noun);
        }
        
        if (normalizedVerb == null && noun == null) {
            String possibleDirection = normalizeDirection(verb);
            if (possibleDirection != null && !possibleDirection.equals(verb)) {
                return new Command(VERB_GO, possibleDirection, input);
            }
            if (isDirection(verb)) {
                return new Command(VERB_GO, verb, input);
            }
        }

        return new Command(normalizedVerb, noun, input);
    }

    public static String extractVerb(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        String[] parts = input.split("\\s+", 2);
        return parts[0].toLowerCase();
    }

    public static String extractNoun(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }
        
        String noun = parts[1].toLowerCase();

        noun = removeLeadingArticles(noun);
        
        return noun.isEmpty() ? null : noun;
    }

    private static String cleanInput(String input) {
        return input.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeVerb(String verb) {
        if (verb == null) {
            return null;
        }
        return SYNONYM_MAP.get(verb.toLowerCase());
    }

    private static String normalizeDirection(String direction) {
        if (direction == null) {
            return null;
        }
        String normalized = DIRECTION_ALIASES.get(direction.toLowerCase());
        return normalized != null ? normalized : direction.toLowerCase();
    }

    private static boolean isDirection(String word) {
        if (word == null) {
            return false;
        }
        String lower = word.toLowerCase();
        return DIRECTION_ALIASES.containsKey(lower) ||
               Set.of("north", "south", "east", "west", "up", "down",
                      "northeast", "northwest", "southeast", "southwest",
                      "ne", "nw", "se", "sw").contains(lower);
    }

    private static String removeLeadingArticles(String noun) {
        String[] articlesToRemove = {"the", "a", "an", "at", "to", "with", "on", "in", "about"};

        String result = noun;
        for (String article : articlesToRemove) {
            if (result.startsWith(article + " ")) {
                result = result.substring(article.length() + 1);
                break;                                            // Only remove one leading article
            }
        }

        return result.trim();
    }

    /**
     * Parses "ask [target] about [topic]" pattern.
     * Returns array: [target, topic] or [null, topic] if no target.
     */
    public static String[] parseAskAbout(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[]{null, null};
        }

        String cleaned = cleanInput(input).toLowerCase();

        // Remove "ask" prefix if present
        if (cleaned.startsWith("ask ")) {
            cleaned = cleaned.substring(4).trim();
        }

        // Check for "about" keyword
        int aboutIndex = cleaned.indexOf(" about ");
        if (aboutIndex > 0) {
            // "mara about rumors" → target=mara, topic=rumors
            String target = cleaned.substring(0, aboutIndex).trim();
            String topic = cleaned.substring(aboutIndex + 7).trim();
            return new String[]{target, topic};
        }

        // "about rumors" or just "rumors"
        if (cleaned.startsWith("about ")) {
            return new String[]{null, cleaned.substring(6).trim()};
        }

        // Just the topic
        return new String[]{null, cleaned};
    }

    public static boolean isValidVerb(String verb) {
        return verb != null && SYNONYM_MAP.containsKey(verb.toLowerCase());
    }

    public static String getCanonicalVerb(String verb) {
        return normalizeVerb(verb);
    }
   
    public static Set<String> getValidVerbs() {
        return VALID_VERBS;
    }

    public static class Command {
        private final String verb;
        private final String noun;
        private final String originalInput;

        public Command(String verb, String noun, String originalInput) {
            this.verb = verb;
            this.noun = noun;
            this.originalInput = originalInput;
        }

        public static Command empty() {
            return new Command(null, null, "");
        }

        public String getVerb() {
            return verb;
        }

        public String getNoun() {
            return noun;
        }

        public String getOriginalInput() { 
        return originalInput;
    }

        public boolean isEmpty() {
        return verb == null && noun == null;
    }

        public boolean isValid() {
            return verb != null;
        }

        public boolean hasNoun() {
            return noun != null;
        }

        public boolean isVerb(String expectedVerb) {
            return expectedVerb != null && expectedVerb.equals(verb);
        }

        @Override
        public String toString() {
            return String.format("Command[verb=%s, noun=%s, original='%s']", verb, noun, originalInput);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Command other = (Command) obj;
            return java.util.Objects.equals(verb, other.verb) &&
                   java.util.Objects.equals(noun, other.noun);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(verb, noun);
        }
    }
}
