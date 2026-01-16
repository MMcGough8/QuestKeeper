package com.questkeeper.state;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.character.Character;
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
    private final Set<String> flags;
    private final Set<String> completedTrials;
    private final List<String> activeQuests;

    private final Map<String, Integer> counters;
    private final Map<String, String> variables;

    private Instant sessionStartTime;
    private long previousPlayTimeSeconds;

    /**
     * Creates a new GameState for a fresh game.
     */
    public GameState(Character character, Campaign campaign) {
        this.stateId = UUID.randomUUID().toString();
        this.character = Objects.requireNonNull(character, "Character cannot be null");
        this.campaign = Objects.requireNonNull(campaign, "Campaign cannot be null");

        this.visitedLocations = new HashSet<>();
        this.flags = new HashSet<>();
        this.completedTrials = new HashSet<>();
        this.activeQuests = new ArrayList<>();
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
            }
        }

        // Restore visited locations and mark them on Location objects
        for (String visitedId : saveState.getVisitedLocations()) {
            state.visitedLocations.add(visitedId);
            Location visitedLoc = campaign.getLocation(visitedId);
            if (visitedLoc != null) {
                visitedLoc.markVisited();
            }
        }

        // Restore flags
        for (Map.Entry<String, Boolean> entry : saveState.getStateFlags().entrySet()) {
            if (entry.getValue()) {
                state.flags.add(entry.getKey());
            }
        }

        // Restore location unlocks based on progression flags
        restoreLocationUnlocks(campaign, state.flags);

        // Restore counters
        state.counters.putAll(saveState.getStateCounters());

        // Restore variables
        state.variables.putAll(saveState.getStateStrings());

        // Restore inventory items from campaign
        for (String itemId : saveState.getInventoryItems()) {
            var item = campaign.getItem(itemId);
            if (item != null) {
                character.getInventory().addItem(item);
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

        // Inventory items
        for (var stack : character.getInventory().getAllItems()) {
            String itemId = stack.getItem().getId();
            for (int i = 0; i < stack.getQuantity(); i++) {
                save.addItem(itemId);
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
     * Moves the player to a new location.
     * Note: Does NOT mark the location as visited - that should happen
     * after displaying the location, so read-aloud text can be shown.
     */
    public boolean moveToLocation(Location location) {
        if (location == null) {
            return false;
        }

        if (!location.isUnlocked()) {
            return false;
        }

        this.currentLocation = location;
        this.visitedLocations.add(location.getId());
        // Don't call location.markVisited() here - let GameEngine do it after display

        return true;
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
     * Marks a trial as completed.
     */
    public void completeTrial(String trialId) {
        if (trialId != null && !trialId.isEmpty()) {
            completedTrials.add(trialId);
            setFlag("completed_" + trialId);
        }
    }

    /**
     * Checks if a trial has been completed.
     */
    public boolean hasCompletedTrial(String trialId) {
        return trialId != null && completedTrials.contains(trialId);
    }

    /**
     * Gets all completed trial IDs.
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

    /**
     * Resets session start time (call when resuming from pause).
     */
    public void resumeSession() {
        this.sessionStartTime = Instant.now();
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
     * Called when loading a saved game to ensure locations are accessible.
     */
    private static void restoreLocationUnlocks(Campaign campaign, Set<String> flags) {
        // Unlock clocktower if Trial #1 was completed
        if (flags.contains("clocktower_unlocked") || flags.contains("trial_01_complete")) {
            Location clocktower = campaign.getLocation("clocktower_hill");
            if (clocktower != null) {
                clocktower.unlock();
            }
        }

        // Unlock Harlequin's Lair if Trial #2 was completed
        if (flags.contains("harlequin_lair_unlocked") || flags.contains("trial_02_complete")) {
            Location lair = campaign.getLocation("harlequin_lair");
            if (lair != null) {
                lair.unlock();
            }
        }

        // Unlock Elara's back room if player has discovered it
        if (flags.contains("back_room_unlocked")) {
            Location backRoom = campaign.getLocation("back_room");
            if (backRoom != null) {
                backRoom.unlock();
            }
        }
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
