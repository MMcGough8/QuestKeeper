package com.questkeeper.campaign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a trial (puzzle room/challenge) in a campaign.
 *
 * Trials are theatrical challenge rooms that test party abilities.
 * Each trial contains mini-games that must be completed, and rewards
 * the player with a completion reward (like a Favor Coin) and a
 * stinger message from the villain.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class Trial {

    private final String id;
    private String name;
    private String location;
    private String entryNarrative;

    private final Map<String, MiniGame> miniGames;
    private String completionReward;
    private String stinger;

    // Data-driven prerequisites and completion flags
    private List<String> prerequisites;
    private List<String> completionFlags;

    private boolean started;
    private boolean completed;

    /**
     * Creates a new trial with the given ID and name.
     */
    public Trial(String id, String name) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Trial ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Trial name cannot be null or empty");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.entryNarrative = "";
        this.miniGames = new HashMap<>();
        this.completionReward = "";
        this.stinger = "";
        this.prerequisites = new ArrayList<>();
        this.completionFlags = new ArrayList<>();
        this.started = false;
        this.completed = false;
    }

    /**
     * Creates a new trial with full details.
     */
    public Trial(String id, String name, String location, String entryNarrative) {
        this(id, name);
        setLocation(location);
        setEntryNarrative(entryNarrative);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getEntryNarrative() {
        return entryNarrative;
    }

    public String getCompletionReward() {
        return completionReward;
    }

    public String getStinger() {
        return stinger;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setEntryNarrative(String entryNarrative) {
        this.entryNarrative = entryNarrative != null ? entryNarrative : "";
    }

    public void setCompletionReward(String completionReward) {
        this.completionReward = completionReward != null ? completionReward : "";
    }

    public void setStinger(String stinger) {
        this.stinger = stinger != null ? stinger : "";
    }

    // ==========================================
    // Prerequisites and Completion Flags
    // ==========================================

    /**
     * Gets the list of prerequisite flags that must be set before this trial can be started.
     * @return unmodifiable list of prerequisite flag names
     */
    public List<String> getPrerequisites() {
        return Collections.unmodifiableList(prerequisites);
    }

    /**
     * Sets the list of prerequisite flags for this trial.
     * @param prerequisites list of flag names that must be true to start this trial
     */
    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites != null ? new ArrayList<>(prerequisites) : new ArrayList<>();
    }

    /**
     * Checks if this trial has any prerequisites.
     * @return true if there are prerequisites
     */
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }

    /**
     * Gets the list of flags to set when this trial is completed.
     * @return unmodifiable list of completion flag names
     */
    public List<String> getCompletionFlags() {
        return Collections.unmodifiableList(completionFlags);
    }

    /**
     * Sets the list of flags to set when this trial is completed.
     * @param completionFlags list of flag names to set on completion
     */
    public void setCompletionFlags(List<String> completionFlags) {
        this.completionFlags = completionFlags != null ? new ArrayList<>(completionFlags) : new ArrayList<>();
    }

    /**
     * Checks if this trial has completion flags defined.
     * @return true if there are completion flags
     */
    public boolean hasCompletionFlags() {
        return !completionFlags.isEmpty();
    }

    // ==========================================
    // Mini-Games
    // ==========================================

    /**
     * Adds a mini-game to this trial.
     */
    public void addMiniGame(MiniGame miniGame) {
        if (miniGame != null) {
            miniGames.put(miniGame.getId(), miniGame);
        }
    }

    /**
     * Removes a mini-game from this trial.
     */
    public MiniGame removeMiniGame(String miniGameId) {
        return miniGames.remove(miniGameId);
    }

    /**
     * Gets a mini-game by its ID.
     */
    public Optional<MiniGame> getMiniGame(String id) {
        return Optional.ofNullable(miniGames.get(id));
    }

    /**
     * Gets all mini-games in this trial.
     */
    public List<MiniGame> getMiniGames() {
        return Collections.unmodifiableList(new ArrayList<>(miniGames.values()));
    }

    /**
     * Gets the number of mini-games in this trial.
     */
    public int getMiniGameCount() {
        return miniGames.size();
    }

    /**
     * Checks if this trial has any mini-games.
     */
    public boolean hasMiniGames() {
        return !miniGames.isEmpty();
    }

    // ==========================================
    // Trial Lifecycle Methods
    // ==========================================

    /**
     * Starts the trial and returns the entry narrative.
     */
    public String start() {
        this.started = true;
        return entryNarrative;
    }

    /**
     * Checks if all mini-games in the trial are complete.
     */
    public boolean checkComplete() {
        if (miniGames.isEmpty()) {
            return true;
        }
        return miniGames.values().stream().allMatch(MiniGame::isCompleted);
    }

    /**
     * Completes the trial, returning a result containing the reward and stinger.
     */
    public CompletionResult complete() {
        if (!started) {
            throw new IllegalStateException("Cannot complete a trial that hasn't been started");
        }
        if (!checkComplete()) {
            throw new IllegalStateException("Cannot complete trial - not all mini-games are finished");
        }

        this.completed = true;
        return new CompletionResult(completionReward, stinger);
    }

    /**
     * Resets the trial to its initial state.
     */
    public void reset() {
        this.started = false;
        this.completed = false;
        miniGames.values().forEach(MiniGame::reset);
    }

    // ==========================================
    // Completion Result
    // ==========================================

    /**
     * Result of completing a trial, containing the reward and stinger.
     */
    public record CompletionResult(String reward, String stinger) {

        /**
         * Checks if there is a reward.
         */
        public boolean hasReward() {
            return reward != null && !reward.isEmpty();
        }

        /**
         * Checks if there is a stinger message.
         */
        public boolean hasStinger() {
            return stinger != null && !stinger.isEmpty();
        }

        /**
         * Gets a formatted completion message.
         */
        public String getFormattedMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== TRIAL COMPLETE ===\n\n");

            if (hasReward()) {
                sb.append("You received: ").append(reward).append("\n\n");
            }

            if (hasStinger()) {
                sb.append(stinger).append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * Gets a formatted summary of this trial suitable for display.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %s ===%n", name));

        if (!entryNarrative.isEmpty()) {
            sb.append(entryNarrative).append("\n\n");
        }

        if (!miniGames.isEmpty()) {
            sb.append("Challenges:\n");
            int i = 1;
            for (MiniGame game : miniGames.values()) {
                String status = game.isCompleted() ? "[X]" : "[ ]";
                sb.append(String.format("  %s %d. %s%n", status, i++, game.getName()));
            }
        }

        if (!completionReward.isEmpty()) {
            sb.append(String.format("%nReward: %s%n", completionReward));
        }

        return sb.toString();
    }

    // ==========================================
    // Equality and HashCode
    // ==========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trial trial = (Trial) o;
        return Objects.equals(id, trial.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Trial{id='%s', name='%s', miniGames=%d, completed=%s}",
                id, name, miniGames.size(), completed);
    }
}
