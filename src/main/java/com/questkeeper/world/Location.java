package com.questkeeper.world;

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

    /**
     * Returns a formatted string of available exits for display.
     */
    public String getExitsDisplay() {
        if (exits.isEmpty()) {
            return "There are no obvious exits.";
        }
        
        StringBuilder sb = new StringBuilder("Exits: ");
        List<String> exitList = new ArrayList<>(exits.keySet());
        Collections.sort(exitList);
        
        for (int i = 0; i < exitList.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(exitList.get(i));
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Location[id=%s, name=%s, exits=%d, npcs=%d, items=%d]",
                id, name, exits.size(), npcs.size(), items.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Location other = (Location) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // ==================== Factory Methods for Muddlebrook ====================

    /**
     * Creates the Drunken Dragon Inn (main hub).
     */
    public static Location createDrunkenDragonInn() {
        Location inn = new Location(
            "drunken_dragon_inn",
            "The Drunken Dragon Inn",
            "A warm, bustling tavern with creaky wooden floors and the smell of hearty stew.",
            "You push open the heavy oak door and warmth washes over you. The Drunken Dragon Inn " +
            "is alive with the clink of mugs and murmur of conversation. A bard strums lazily in " +
            "the corner, and the bartender polishes glasses with practiced ease. Lanterns cast " +
            "dancing shadows across worn wooden tables where locals nurse their drinks."
        );
        
        inn.addExit("north", "town_square");
        inn.addExit("door", "town_square");
        inn.addNpc("norrin_bard");
        inn.addNpc("mara_bartender");
        
        return inn;
    }

    /**
     * Creates the Town Square (central hub).
     */
    public static Location createTownSquare() {
        Location square = new Location(
            "town_square",
            "Muddlebrook Town Square",
            "The muddy center of town, surrounded by shops and the looming Town Hall.",
            "The town square squelches underfoot—Muddlebrook earns its name. A weathered " +
            "notice board stands at the center, plastered with papers. To the north, the " +
            "imposing Town Hall watches over the square. Shops line the east side, and the " +
            "warm glow of the Drunken Dragon Inn beckons from the south."
        );
        
        square.addExit("south", "drunken_dragon_inn");
        square.addExit("north", "town_hall");
        square.addExit("east", "market_row");
        square.addExit("west", "clocktower_hill");
        
        return square;
    }

    /**
     * Creates Town Hall (Trial #1 location).
     */
    public static Location createTownHall() {
        Location hall = new Location(
            "town_hall",
            "Muddlebrook Town Hall",
            "An official building with creaky floors and the faint smell of old paper.",
            "The Town Hall's double doors open with a groan. Inside, dust motes dance in " +
            "shafts of light from high windows. Portraits of past mayors line the walls, " +
            "their painted eyes seeming to follow you. A grand staircase leads up to the " +
            "mayor's office. Something feels... off. Too quiet."
        );
        
        hall.addExit("south", "town_square");
        hall.addExit("upstairs", "mayors_office");
        
        return hall;
    }

    /**
     * Creates the Mayor's Office (Trial #1 puzzle room).
     */
    public static Location createMayorsOffice() {
        Location office = new Location(
            "mayors_office",
            "Mayor's Office",
            "A ransacked office with scattered papers and strange clockwork devices.",
            "You reach the top of the stairs and freeze. The mayor's office has been " +
            "transformed into something between a crime scene and a carnival. Papers are " +
            "scattered everywhere. A clock on the wall ticks backwards. A mechanical frog " +
            "sits on the desk, watching you with glass eyes. And pinned to the mayor's " +
            "empty chair is a note with a laughing mask drawn on it."
        );
        
        office.addExit("downstairs", "town_hall");
        office.setFlag("trial_location");
        
        return office;
    }

    /**
     * Creates Old Market Row (shopping district).
     */
    public static Location createMarketRow() {
        Location market = new Location(
            "market_row",
            "Old Market Row",
            "A narrow street lined with shops, stalls, and the occasional suspicious alley.",
            "Market Row buzzes with haggling voices and the clatter of commerce. Colorful " +
            "awnings shade stalls selling everything from fresh bread to questionable potions. " +
            "One shop catches your eye—'Clockwork Curios' reads the sign, with gears turning " +
            "lazily in the window display."
        );
        
        market.addExit("west", "town_square");
        market.addExit("shop", "clockwork_curios");
        market.addNpc("street_vendor");
        
        return market;
    }

    /**
     * Creates Clockwork Curios shop.
     */
    public static Location createClockworkCurios() {
        Location shop = new Location(
            "clockwork_curios",
            "Clockwork Curios",
            "A cramped shop filled with ticking, whirring, and occasionally sparking devices.",
            "A bell chimes as you enter, though you don't see one. The shop is a maze of " +
            "shelves crammed with mechanical oddities—music boxes that play themselves, " +
            "compasses that point to 'interesting', pocket watches with too many hands. " +
            "Behind the counter, a sharp-eyed woman looks up from a disassembled device. " +
            "\"Browser or buyer?\" she asks."
        );
        
        shop.addExit("out", "market_row");
        shop.addNpc("elara_shopkeeper");
        
        return shop;
    }

    /**
     * Creates Clocktower Hill.
     */
    public static Location createClocktowerHill() {
        Location hill = new Location(
            "clocktower_hill",
            "Clocktower Hill",
            "A steep hill crowned by an abandoned clocktower that hasn't told time in years.",
            "The path up Clocktower Hill is overgrown but well-worn by curious feet. The " +
            "clocktower looms above, its face frozen at an impossible time—all four hands " +
            "pointing in different directions. Locals say it stopped the night the old " +
            "mayor vanished, twenty years ago. The new mayor's disappearance has people " +
            "whispering about curses."
        );
        
        hill.addExit("east", "town_square");
        hill.addExit("up", "clocktower_base");
        hill.lock();  // Unlocked after Trial #1
        
        return hill;
    }
}