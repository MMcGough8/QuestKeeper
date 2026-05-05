package com.questkeeper.state;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.character.Character;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.StandardEquipment;
import com.questkeeper.save.SaveState;
import com.questkeeper.world.Location;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Manages the runtime game state for an active play session.
 *
 * GameState is the central hub connecting the player's Character,
 * the loaded Campaign data, current Location, and all progress tracking.
 * Unlike SaveState (which handles persistence), GameState holds live
 * object references for efficient gameplay operations.
 *
 * Key responsibilities:
 * - Track current location and handle movement
 * - Manage game flags for story/quest progress
 * - Track completed trials and active quests
 * - Record visited locations
 * - Track play time
 * - Convert to/from SaveState for save/load operations
 *
 * @author Marc McGough
 * @version 1.0
 */
public class GameState {

    private final String stateId;
    private final Character character;
    private final Campaign campaign;

    private Location currentLocation;
    private final Set<String> visitedLocations;
    private final Set<String> unlockedLocations;
    private final Set<String> flags;
    private final Set<String> completedTrials;
    private final Set<String> startedTrials;
    private final List<String> activeQuests;

    private final Map<String, Integer> counters;
    private final Map<String, String> variables;

    private Instant sessionStartTime;
    private long previousPlayTimeSeconds;

    /**
     * Warnings produced during the most recent {@link #fromSaveState} call —
     * e.g., equipped items that couldn't be located in the current campaign,
     * or slot-name mismatches. Empty for fresh games. {@link com.questkeeper.core.GameEngine#loadGame}
     * surfaces these to the player so a partial load isn't silent.
     */
    private final List<String> loadWarnings = new ArrayList<>();

    /**
     * Creates a new GameState for a fresh game.
     */
    public GameState(Character character, Campaign campaign) {
        this.stateId = UUID.randomUUID().toString();
        this.character = Objects.requireNonNull(character, "Character cannot be null");
        this.campaign = Objects.requireNonNull(campaign, "Campaign cannot be null");

        this.visitedLocations = new HashSet<>();
        this.unlockedLocations = new HashSet<>();
        this.flags = new HashSet<>();
        this.completedTrials = new HashSet<>();
        this.startedTrials = new HashSet<>();
        this.activeQuests = new ArrayList<>();

        // Initialize unlocked locations from campaign (locations without 'locked' flag are unlocked by default)
        initializeUnlockedLocations();
        this.counters = new HashMap<>();
        this.variables = new HashMap<>();

        this.sessionStartTime = Instant.now();
        this.previousPlayTimeSeconds = 0;

        // Set starting location
        Location startingLocation = campaign.getStartingLocation();
        if (startingLocation != null) {
            moveToLocation(startingLocation);
        }
    }

    /**
     * Creates a GameState from a SaveState (for loading saved games).
     */
    public static GameState fromSaveState(SaveState saveState, Campaign campaign) {
        Character character = saveState.restoreCharacter();
        GameState state = new GameState(character, campaign);

        // Restore location
        String locationId = saveState.getCurrentLocationId();
        if (locationId != null) {
            Location loc = campaign.getLocation(locationId);
            if (loc != null) {
                state.currentLocation = loc;
                state.visitedLocations.add(locationId);
            } else {
                // Saved location no longer exists in this campaign (renamed/removed).
                // Player will fall back to starting_location set by the constructor.
                System.err.println("Warning: saved location '" + locationId +
                    "' not found in campaign '" + campaign.getId() +
                    "'. Falling back to starting location.");
            }
        }

        // Restore visited locations (no need to mark on Location objects - we track in GameState)
        for (String visitedId : saveState.getVisitedLocations()) {
            state.visitedLocations.add(visitedId);
        }

        // Restore flags (lowercase to match setFlag's normalization, so
        // hand-edited saves with mixed-case keys still resolve via hasFlag).
        for (Map.Entry<String, Boolean> entry : saveState.getStateFlags().entrySet()) {
            if (entry.getValue()) {
                state.flags.add(entry.getKey().toLowerCase());
            }
        }

        // Restore location unlocks based on progression flags (data-driven)
        restoreLocationUnlocks(state.unlockedLocations, campaign, state.flags);

        // Restore trial states from flags. Filter against the campaign's
        // actual trial IDs so non-trial "started_X"/"completed_X" flags
        // (e.g., quest flags) don't pollute the trial sets.
        for (String flag : state.flags) {
            if (flag.startsWith("started_")) {
                String trialId = flag.substring("started_".length());
                if (campaign.getTrials().containsKey(trialId)) {
                    state.startedTrials.add(trialId);
                }
            } else if (flag.startsWith("completed_minigame_")) {
                // No-op: the flag is the single source of truth.
                // TrialCommandHandler reads completion via the flag, so we no
                // longer mutate the shared Campaign-held MiniGame instance
                // here — that was leaking state across re-loads in the same
                // JVM.
            } else if (flag.startsWith("completed_")) {
                String trialId = flag.substring("completed_".length());
                if (campaign.getTrials().containsKey(trialId)) {
                    state.completedTrials.add(trialId);
                }
            }
        }

        // Restore counters
        state.counters.putAll(saveState.getStateCounters());

        // Restore variables
        state.variables.putAll(saveState.getStateStrings());

        // Restore inventory items from campaign or StandardEquipment.
        // Track lookup failures (often cross-campaign load) and weight-cap
        // overflows so the load isn't silently lossy.
        StandardEquipment stdEquip = StandardEquipment.getInstance();
        for (String itemId : saveState.getInventoryItems()) {
            Item item = findItem(itemId, campaign, stdEquip);
            if (item == null) {
                String msg = "Backpack item '" + itemId
                    + "' is not part of this campaign; dropped on load.";
                System.err.println("WARN: " + msg);
                state.loadWarnings.add(msg);
                continue;
            }
            if (!character.getInventory().addItem(item)) {
                String msg = "Could not pack '" + item.getName()
                    + "' on load (likely over carrying capacity).";
                System.err.println("WARN: " + msg);
                state.loadWarnings.add(msg);
            }
        }

        // Restore equipped items (use slot info if available, fallback to legacy).
        // Warnings collected here so we can show the player exactly what
        // didn't restore — silent drops mask save corruption + cross-campaign
        // loads.
        java.util.List<String> equipWarnings = new java.util.ArrayList<>();
        if (saveState.hasEquippedSlots()) {
            // New format: restore to exact slots
            for (var entry : saveState.getEquippedSlots().entrySet()) {
                String slotName = entry.getKey();
                String itemId = entry.getValue();
                Item item = findItem(itemId, campaign, stdEquip);
                if (item == null) {
                    equipWarnings.add("Could not find equipped item '" + itemId
                        + "' (slot " + slotName + ") in this campaign.");
                    continue;
                }
                character.getInventory().addItem(item);
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(slotName);
                    character.getInventory().equipToSlot(item, slot);
                } catch (IllegalArgumentException e) {
                    EquipmentSlot fallback = determineSlotForItem(item);
                    if (fallback != null) {
                        character.getInventory().equipToSlot(item, fallback);
                    } else {
                        equipWarnings.add("Could not restore '" + item.getName()
                            + "' to slot '" + slotName
                            + "'; left in backpack.");
                    }
                }
            }
        } else {
            // Legacy format: use default slots
            for (String itemId : saveState.getEquippedItems()) {
                Item item = findItem(itemId, campaign, stdEquip);
                if (item == null) {
                    equipWarnings.add("Could not find equipped item '" + itemId
                        + "' in this campaign.");
                    continue;
                }
                character.getInventory().addItem(item);
                EquipmentSlot slot = determineSlotForItem(item);
                if (slot != null) {
                    character.getInventory().equipToSlot(item, slot);
                } else {
                    equipWarnings.add("Could not infer slot for '"
                        + item.getName() + "'; left in backpack.");
                }
            }
        }
        if (!equipWarnings.isEmpty()) {
            for (String w : equipWarnings) {
                System.err.println("WARN: " + w);
            }
            state.loadWarnings.addAll(equipWarnings);
        }

        // Restore attunement state on magic items in the inventory and
        // equipped slots. attune() is idempotent on a freshly-loaded
        // (un-attuned) item and just flips the boolean.
        java.util.Set<String> attuned = saveState.getAttunedItemIds();
        if (attuned != null && !attuned.isEmpty()) {
            for (var stack : character.getInventory().getAllItems()) {
                if (stack.getItem() instanceof com.questkeeper.inventory.items.MagicItem mi
                        && attuned.contains(mi.getId())) {
                    try {
                        mi.attune(character);
                    } catch (RuntimeException e) {
                        state.loadWarnings.add("Could not re-attune '"
                            + mi.getName() + "': " + e.getMessage());
                    }
                }
            }
            for (var item : character.getInventory().getEquippedItems().values()) {
                if (item instanceof com.questkeeper.inventory.items.MagicItem mi
                        && attuned.contains(mi.getId()) && !mi.isAttuned()) {
                    try {
                        mi.attune(character);
                    } catch (RuntimeException e) {
                        state.loadWarnings.add("Could not re-attune '"
                            + mi.getName() + "': " + e.getMessage());
                    }
                }
            }
        }

        // Restore gold
        character.getInventory().addGold(saveState.getGold());

        // Restore play time
        state.previousPlayTimeSeconds = saveState.getTotalPlayTimeSeconds();
        state.sessionStartTime = Instant.now();

        return state;
    }

    /**
     * Converts this GameState to a SaveState for persistence.
     */
    public SaveState toSaveState() {
        SaveState save = new SaveState(character, campaign.getId());

        // Location
        if (currentLocation != null) {
            save.setCurrentLocation(currentLocation.getId());
        }

        // Copy visited locations
        for (String locationId : visitedLocations) {
            save.addVisitedLocation(locationId);
        }

        // Flags
        for (String flag : flags) {
            save.setFlag(flag, true);
        }

        // Counters
        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            save.setCounter(entry.getKey(), entry.getValue());
        }

        // Variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            save.setString(entry.getKey(), entry.getValue());
        }

        // Inventory items (backpack). Also captures attunement state on
        // any MagicItem so a Wand of X / Ring of Y stays attuned across
        // save/load. (Per-effect charge state is deferred — round-trip
        // for stacked charged items is nontrivial.)
        for (var stack : character.getInventory().getAllItems()) {
            String itemId = stack.getItem().getId();
            for (int i = 0; i < stack.getQuantity(); i++) {
                save.addItem(itemId);
            }
            if (stack.getItem() instanceof com.questkeeper.inventory.items.MagicItem mi
                    && mi.isAttuned()) {
                save.setItemAttuned(itemId, true);
            }
        }

        // Equipped items (save with slot information)
        for (var entry : character.getInventory().getEquippedItems().entrySet()) {
            EquipmentSlot slot = entry.getKey();
            var item = entry.getValue();
            String itemId = item.getId();
            save.equipItemToSlot(slot.name(), itemId);
            if (item instanceof com.questkeeper.inventory.items.MagicItem mi
                    && mi.isAttuned()) {
                save.setItemAttuned(itemId, true);
            }
        }

        // Gold
        save.addGold(character.getInventory().getGold());

        // Play time
        save.addPlayTime(getTotalPlayTimeSeconds());

        return save;
    }

    // ==========================================
    // Location Management
    // ==========================================

    /**
     * Initializes the set of unlocked locations based on campaign data.
     * Locations without the 'locked' flag are unlocked by default.
     */
    private void initializeUnlockedLocations() {
        for (Location location : campaign.getLocations().values()) {
            // A location is initially unlocked if it doesn't have the 'locked' flag
            if (!location.hasFlag("locked")) {
                unlockedLocations.add(location.getId());
            }
        }
    }

    /**
     * Moves the player to a new location.
     * Note: Does NOT mark the location as visited - that should happen
     * after displaying the location, so read-aloud text can be shown.
     */
    public boolean moveToLocation(Location location) {
        if (location == null) {
            return false;
        }

        String locationId = location.getId();
        if (locationId == null || locationId.isEmpty()) {
            return false;
        }

        // Check unlock state in GameState, not on Location object
        if (!isLocationUnlocked(locationId)) {
            return false;
        }

        this.currentLocation = location;
        this.visitedLocations.add(locationId);

        return true;
    }

    /**
     * Checks if a location is unlocked.
     * @param locationId the location ID to check
     * @return true if the location is unlocked
     */
    public boolean isLocationUnlocked(String locationId) {
        return locationId != null && unlockedLocations.contains(locationId);
    }

    /**
     * Unlocks a location, making it accessible to the player.
     * @param locationId the location ID to unlock
     */
    public void unlockLocation(String locationId) {
        if (locationId != null && !locationId.isEmpty()) {
            unlockedLocations.add(locationId);
        }
    }

    /**
     * Gets all unlocked location IDs.
     * @return unmodifiable set of unlocked location IDs
     */
    public Set<String> getUnlockedLocations() {
        return Collections.unmodifiableSet(unlockedLocations);
    }

    /**
     * Attempts to move in a direction from the current location.
     */
    public boolean move(String direction) {
        if (currentLocation == null || direction == null) {
            return false;
        }

        String targetId = currentLocation.getExit(direction.toLowerCase());
        if (targetId == null) {
            return false;
        }

        Location target = campaign.getLocation(targetId);
        if (target == null) {
            return false;
        }
        return moveToLocation(target);
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public String getCurrentLocationId() {
        return currentLocation != null ? currentLocation.getId() : null;
    }

    public boolean hasVisited(String locationId) {
        return visitedLocations.contains(locationId);
    }

    public Set<String> getVisitedLocations() {
        return Collections.unmodifiableSet(visitedLocations);
    }

    // ==========================================
    // Flag Management
    // ==========================================

    /**
     * Sets a game flag.
     */
    public void setFlag(String flag) {
        if (flag != null && !flag.isEmpty()) {
            flags.add(flag.toLowerCase());
        }
    }

    /**
     * Sets or clears a game flag based on the value.
     */
    public void setFlag(String flag, boolean value) {
        if (flag == null || flag.isEmpty()) {
            return;
        }
        if (value) {
            flags.add(flag.toLowerCase());
        } else {
            flags.remove(flag.toLowerCase());
        }
    }

    public void clearFlag(String flag) {
        if (flag != null) {
            flags.remove(flag.toLowerCase());
        }
    }

    public boolean hasFlag(String flag) {
        return flag != null && flags.contains(flag.toLowerCase());
    }

    public Set<String> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    /**
     * Warnings accumulated during the most recent load. Empty for fresh games.
     * Caller (GameEngine.loadGame) should surface these to the player so
     * partial loads (cross-campaign, missing items, slot mismatches) aren't silent.
     */
    public List<String> getLoadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
    }

    // ==========================================
    // Counter Management
    // ==========================================

    /**
     * Sets a counter value.
     */
    public void setCounter(String key, int value) {
        if (key != null) {
            counters.put(key.toLowerCase(), value);
        }
    }

    /**
     * Gets a counter value.
     */
    public int getCounter(String key) {
        return key != null ? counters.getOrDefault(key.toLowerCase(), 0) : 0;
    }

    /**
     * Increments a counter.
     */
    public void incrementCounter(String key) {
        if (key != null) {
            counters.merge(key.toLowerCase(), 1, Integer::sum);
        }
    }

    /**
     * Decrements a counter.
     */
    public void decrementCounter(String key) {
        if (key != null) {
            counters.merge(key.toLowerCase(), -1, Integer::sum);
        }
    }

    // ==========================================
    // Variable Management
    // ==========================================

    public void setVariable(String key, String value) {
        if (key != null) {
            if (value != null) {
                variables.put(key.toLowerCase(), value);
            } else {
                variables.remove(key.toLowerCase());
            }
        }
    }


    public String getVariable(String key) {
        return key != null ? variables.get(key.toLowerCase()) : null;
    }

    // ==========================================
    // Trial Management
    // ==========================================

    /**
     * Marks a trial as started.
     * @param trialId the trial ID to mark as started
     */
    public void startTrial(String trialId) {
        if (trialId != null && !trialId.isEmpty()) {
            startedTrials.add(trialId);
            setFlag("started_" + trialId);
        }
    }

    /**
     * Checks if a trial has been started.
     * @param trialId the trial ID to check
     * @return true if the trial has been started
     */
    public boolean hasStartedTrial(String trialId) {
        return trialId != null && startedTrials.contains(trialId);
    }

    /**
     * Gets all started trial IDs.
     * @return unmodifiable set of started trial IDs
     */
    public Set<String> getStartedTrials() {
        return Collections.unmodifiableSet(startedTrials);
    }

    /**
     * Marks a trial as completed.
     * @param trialId the trial ID to mark as completed
     */
    public void completeTrial(String trialId) {
        if (trialId != null && !trialId.isEmpty()) {
            completedTrials.add(trialId);
            setFlag("completed_" + trialId);
        }
    }

    /**
     * Checks if a trial has been completed.
     * @param trialId the trial ID to check
     * @return true if the trial has been completed
     */
    public boolean hasCompletedTrial(String trialId) {
        return trialId != null && completedTrials.contains(trialId);
    }

    /**
     * Gets all completed trial IDs.
     * @return unmodifiable set of completed trial IDs
     */
    public Set<String> getCompletedTrials() {
        return Collections.unmodifiableSet(completedTrials);
    }

    // ==========================================
    // Quest Management
    // ==========================================

    /**
     * Starts a quest.
     */
    public void startQuest(String questId) {
        if (questId != null && !questId.isEmpty() && !activeQuests.contains(questId)) {
            activeQuests.add(questId);
            setFlag("started_" + questId);
        }
    }

    /**
     * Completes a quest.
     */
    public void completeQuest(String questId) {
        if (questId != null) {
            activeQuests.remove(questId);
            setFlag("completed_" + questId);
        }
    }

    /**
     * Checks if a quest is active.
     */
    public boolean isQuestActive(String questId) {
        return questId != null && activeQuests.contains(questId);
    }

    /**
     * Gets all active quest IDs.
     */
    public List<String> getActiveQuests() {
        return Collections.unmodifiableList(activeQuests);
    }

    // ==========================================
    // Play Time Tracking
    // ==========================================

    public long getTotalPlayTimeSeconds() {
        long currentSessionSeconds = Duration.between(sessionStartTime, Instant.now()).getSeconds();
        return previousPlayTimeSeconds + currentSessionSeconds;
    }

    /**
     * Gets formatted play time string.
     */
    public String getFormattedPlayTime() {
        long totalSeconds = getTotalPlayTimeSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dm", minutes);
    }

    // ==========================================
    // Accessors
    // ==========================================

    public String getStateId() {
        return stateId;
    }

    public Character getCharacter() {
        return character;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getCampaignId() {
        return campaign.getId();
    }

    public String getCampaignName() {
        return campaign.getName();
    }

    /**
     * Restores location unlocks based on game progress flags.
     * Uses a data-driven approach: any flag ending in "_unlocked" will unlock
     * the corresponding location (e.g., "clocktower_hill_unlocked" unlocks "clocktower_hill").
     *
     * @param unlockedLocations the set to add unlocked location IDs to
     * @param campaign the campaign containing the locations
     * @param flags the set of game progress flags
     */
    private static void restoreLocationUnlocks(Set<String> unlockedLocations, Campaign campaign, Set<String> flags) {
        for (String flag : flags) {
            if (flag.endsWith("_unlocked")) {
                // Extract location ID from flag (e.g., "clocktower_hill_unlocked" -> "clocktower_hill")
                String locationId = flag.substring(0, flag.length() - "_unlocked".length());
                Location location = campaign.getLocation(locationId);
                if (location != null) {
                    unlockedLocations.add(locationId);
                }
            }
        }
    }

    /**
     * Finds an item by ID, checking campaign items first, then StandardEquipment.
     */
    private static Item findItem(String itemId, Campaign campaign, StandardEquipment stdEquip) {
        // Try campaign first
        Item item = campaign.getItem(itemId);
        if (item != null) {
            return item;
        }

        // Try StandardEquipment weapons
        var weapon = stdEquip.getWeapon(itemId);
        if (weapon != null) {
            return weapon;
        }

        // Try StandardEquipment armor
        var armor = stdEquip.getArmor(itemId);
        if (armor != null) {
            return armor;
        }

        return null;
    }

    /**
     * Determines the appropriate equipment slot for an item.
     */
    private static EquipmentSlot determineSlotForItem(Item item) {
        return switch (item.getType()) {
            case WEAPON -> EquipmentSlot.MAIN_HAND;
            case ARMOR -> EquipmentSlot.ARMOR;
            case SHIELD -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return String.format("GameState[%s in %s @ %s, flags=%d, played=%s]",
                character.getName(),
                campaign.getId(),
                currentLocation != null ? currentLocation.getName() : "nowhere",
                flags.size(),
                getFormattedPlayTime());
    }
}
