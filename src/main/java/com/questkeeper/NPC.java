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

    public void removeDialogue(String topic) {
        if (topic != null) {
            dialogues.remove(topic.toLowerCase());
        }
    }

    public List<String> getAvailableTopics() {
        List<String> topics = new ArrayList<>(dialogues.keySet());
        Collections.sort(topics);
        return Collections.unmodifiableList(topics);
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

     /**
     * Creates Norrin the Bard (tavern, rumors, comedic relief).
     */
    public static NPC createNorrin() {
        NPC norrin = new NPC(
            "norrin_bard",
            "Norrin",
            "bard",
            "sing-song, theatrical",
            "friendly, dramatic, never gives full truth directly"
        );
        
        norrin.setDescription("A flamboyant half-elf with a lute that's seen better days " +
                "and a smile that's seen better taverns. His fingers never stop moving, " +
                "always tapping out some rhythm only he can hear.");
        norrin.setLocationId("drunken_dragon_inn");
        
        norrin.setGreeting("Ah! New faces—new verses! Come, sit, let old Norrin " +
                "spin you a tale... or perhaps you have one for me?");
        norrin.setReturnGreeting("My favorite audience returns! What news from " +
                "the world outside these warm walls?");
        
        norrin.addDialogue("mayor", "The mayor? Oh, that's a song still being written, " +
                "I'm afraid. Verse one: he was here. Verse two: he wasn't. " +
                "The chorus? Nobody knows it yet.");
        norrin.addDialogue("town", "Muddlebrook! Where the mud is deep and the secrets deeper. " +
                "A fine place to lose yourself... or find trouble.");
        norrin.addDialogue("clocktower", "That old tower? It stopped ticking when I was a boy. " +
                "Some say it's waiting. For what? *strums dramatically* That's the mystery.");
        norrin.addDialogue("machinist", "Shh! *looks around* We don't sing that name here. " +
                "But if I were to hum a tune about masked figures and clockwork... " +
                "well, that's just a song, isn't it?");
        norrin.addDialogue("rumors", "Rumors? I only write embarrassing songs sometimes. " +
                "But I've heard the town hall has been... peculiar lately. " +
                "Strange sounds. Stranger visitors.");
        
        norrin.addSampleLine("Ah! New faces—new verses!");
        norrin.addSampleLine("I only write embarrassing songs sometimes.");
        norrin.addSampleLine("*strums a chord* That reminds me of a ballad...");
        
        return norrin;
    }

    /**
     * Creates Mara Ember (bartender, practical info, keeps pace moving).
     */
    public static NPC createMara() {
        NPC mara = new NPC(
            "mara_bartender",
            "Mara Ember",
            "bartender",
            "confident, no-nonsense",
            "warm but firm, reads the room, hates drama"
        );
        
        mara.setDescription("A sturdy human woman with calloused hands and sharp eyes " +
                "that miss nothing. Her apron is spotless despite the tavern's chaos, " +
                "and she polishes the same glass whether it needs it or not.");
        mara.setLocationId("drunken_dragon_inn");
        
        mara.setGreeting("If you're not buying, make it quick. If you are buying, " +
                "take a seat and I'll be with you.");
        mara.setReturnGreeting("Back again? The usual, or something stronger?");
        
        mara.addDialogue("mayor", "Alderwick's a good man. Was. Is. I don't know anymore. " +
                "He came in three nights before he vanished. Looked scared. " +
                "Wouldn't say of what.");
        mara.addDialogue("drinks", "Ale's a copper. Wine's three. Anything fancy, " +
                "you're in the wrong tavern.");
        mara.addDialogue("rumors", "Rumors are cheap. Facts cost. But I'll tell you this for free: " +
                "something's wrong in this town. People are nervous. And nervous people drink more, " +
                "so I shouldn't complain, but...");
        mara.addDialogue("norrin", "The bard? Harmless. Mostly. His songs are better than his secrets, " +
                "but he hears things. Just don't believe everything he sings.");
        mara.addDialogue("work", "Looking for work? Talk to the guard captain. Or check the notice board " +
                "in the square. Just... be careful what you sign up for these days.");
        
        mara.addSampleLine("If you're not buying, make it quick.");
        mara.addSampleLine("Rumors are cheap. Facts cost.");
        mara.addSampleLine("*wipes glass* I see everything. Remember that.");
        
        return mara;
    }

    /**
     * Creates Darius (dark recluse, mentor, warnings).
     */
    public static NPC createDarius() {
        NPC darius = new NPC(
            "darius_recluse",
            "Darius",
            "recluse",
            "low, careful, intense",
            "speaks like every word has weight, hates the Machinist, respects cleverness"
        );
        
        darius.setDescription("A gaunt man in a hooded cloak, sitting alone in the darkest corner. " +
                "Scars trace his hands like a map of past mistakes. His eyes are old, " +
                "older than his face suggests.");
        darius.setLocationId("drunken_dragon_inn");
        
        darius.setGreeting("You have the look of someone who asks questions. " +
                "I have the look of someone who knows answers. " +
                "The question is: are you ready for them?");
        darius.setReturnGreeting("Still alive. Good. That means you're either lucky or clever. " +
                "I hope it's the latter.");
        
        darius.addDialogue("machinist", "I've seen his work before. Years ago. Different town, " +
                "same games. He doesn't want to kill you—that's too easy. " +
                "He wants to *make* you. Into what, I never learned.");
        darius.addDialogue("trials", "His trials can be beaten. I've seen it done. " +
                "The trick is: never panic. That's what he wants. Stay clever. Stay calm. " +
                "And never, ever trust the obvious solution.");
        darius.addDialogue("past", "*long pause* I played his games once. I survived. " +
                "Not everyone did. That's all you need to know.");
        darius.addDialogue("help", "I can point you in the right direction. Elara at Clockwork Curios— " +
                "she knows things about mechanisms. And there's a back room in her shop... " +
                "but you didn't hear that from me.");
        darius.addDialogue("mayor", "Alderwick found something he shouldn't have. In the clocktower. " +
                "He tried to stop it. Now he's gone. Draw your own conclusions.");
        
        darius.addSampleLine("*stares intensely* Choose your next words carefully.");
        darius.addSampleLine("I've seen this before. It doesn't end well for the unprepared.");
        darius.addSampleLine("*nods slowly* Clever. You might survive after all.");
        
        return darius;
    }

    /**
     * Creates Elara (Clockwork Curios shopkeeper, upgrades, back-room secrets).
     */
    public static NPC createElara() {
        NPC elara = new NPC(
            "elara_shopkeeper",
            "Elara",
            "shopkeeper",
            "sharp, amused, confident",
            "tests customers, bargains hard, hints at hidden stock"
        );
        
        elara.setDescription("A wiry gnome woman with magnifying goggles pushed up on her forehead " +
                "and grease stains that might be intentional. Her fingers are nimble, " +
                "always fiddling with some tiny mechanism.");
        elara.setLocationId("clockwork_curios");
        elara.setShopkeeper(true);
        
        elara.setGreeting("Browser or buyer? I don't mind either way, but touch anything " +
                "with more than three gears and you own it. Shop rules.");
        elara.setReturnGreeting("Ah, you're back. Broke something already, or looking to upgrade?");
        
        elara.addDialogue("shop", "Everything here works. Mostly. Some things work in ways " +
                "you wouldn't expect. That's the fun of it.");
        elara.addDialogue("buy", "Looking to spend coin? Smart. My devices have saved more lives " +
                "than any sword. Probably. I don't keep count.");
        elara.addDialogue("clocktower", "*pauses* That old thing? Complicated mechanism. Very old. " +
                "If someone were to... examine it closely... they'd need to know what they're doing. " +
                "Lucky for you, I sell expertise.");
        elara.addDialogue("machinist", "Heard of him. Professional interest, you understand. " +
                "His work is... impressive. Terrible, but impressive. " +
                "Like a beautiful trap that takes your fingers off.");
        elara.addDialogue("backroom", "*glances at curtain* Back room? That's just storage. " +
                "Unless you're the sort who can appreciate... specialized inventory. " +
                "Are you that sort?");
        
        elara.addSampleLine("Browser or buyer?");
        elara.addSampleLine("*adjusts goggles* Interesting. Very interesting.");
        elara.addSampleLine("You break it, you buy it. You buy it, you break it—that's your problem.");
        
        return elara;
    }
}
