package com.questkeeper.core.command;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.campaign.Trial;
import com.questkeeper.character.Character;
import com.questkeeper.combat.CombatSystem;
import com.questkeeper.core.RestSystem;
import com.questkeeper.dialogue.DialogueSystem;
import com.questkeeper.state.GameState;
import com.questkeeper.world.Location;

import java.util.Random;
import java.util.Scanner;

/**
 * Holds all shared game state that command handlers need access to.
 *
 * This context object is passed to command handlers, giving them access
 * to the game systems without tight coupling to GameEngine.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class GameContext {

    private final Campaign campaign;
    private final GameState gameState;
    private final DialogueSystem dialogueSystem;
    private final CombatSystem combatSystem;
    private final RestSystem restSystem;
    private final Scanner scanner;
    private final Random random;

    private Trial activeTrial;
    private boolean running;

    public GameContext(Campaign campaign, GameState gameState,
                       DialogueSystem dialogueSystem, CombatSystem combatSystem,
                       RestSystem restSystem, Scanner scanner, Random random) {
        this.campaign = campaign;
        this.gameState = gameState;
        this.dialogueSystem = dialogueSystem;
        this.combatSystem = combatSystem;
        this.restSystem = restSystem;
        this.scanner = scanner;
        this.random = random;
        this.running = true;
    }

    // ==========================================
    // Accessors
    // ==========================================

    public Campaign getCampaign() {
        return campaign;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Character getCharacter() {
        return gameState.getCharacter();
    }

    public Location getCurrentLocation() {
        return gameState.getCurrentLocation();
    }

    public DialogueSystem getDialogueSystem() {
        return dialogueSystem;
    }

    public CombatSystem getCombatSystem() {
        return combatSystem;
    }

    public RestSystem getRestSystem() {
        return restSystem;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public Random getRandom() {
        return random;
    }

    // ==========================================
    // Trial State
    // ==========================================

    public Trial getActiveTrial() {
        return activeTrial;
    }

    public void setActiveTrial(Trial trial) {
        this.activeTrial = trial;
    }

    public boolean hasActiveTrial() {
        return activeTrial != null;
    }

    // ==========================================
    // Game Running State
    // ==========================================

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void stopGame() {
        this.running = false;
    }

    // ==========================================
    // Convenience Methods
    // ==========================================

    /**
     * Checks if the player is currently in combat.
     */
    public boolean isInCombat() {
        return combatSystem.isInCombat();
    }

    /**
     * Checks if the player is currently in a dialogue.
     */
    public boolean isInDialogue() {
        return dialogueSystem.isInConversation();
    }

    /**
     * Gets a random double between 0.0 and 1.0.
     */
    public double nextRandomDouble() {
        return random.nextDouble();
    }

    /**
     * Reads a line of input from the player.
     */
    public String readInput() {
        return scanner.nextLine();
    }
}
