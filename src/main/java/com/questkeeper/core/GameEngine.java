package com.questkeeper.core;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.campaign.CampaignInfo;
import com.questkeeper.campaign.Trial;
import com.questkeeper.character.Character;
import com.questkeeper.character.NPC;
import com.questkeeper.combat.CombatResult;
import com.questkeeper.combat.CombatSystem;
import com.questkeeper.combat.Combatant;
import com.questkeeper.combat.Monster;
import com.questkeeper.core.CommandParser.Command;
import com.questkeeper.core.RestSystem.RestResult;
import com.questkeeper.core.command.CommandResult;
import com.questkeeper.core.command.CommandRouter;
import com.questkeeper.core.command.GameContext;
import com.questkeeper.dialogue.DialogueResult;
import com.questkeeper.dialogue.DialogueSystem;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.save.SaveState;
import com.questkeeper.state.GameState;
import com.questkeeper.ui.CharacterCreator;
import com.questkeeper.ui.Display;
import com.questkeeper.world.Location;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Main game engine that orchestrates the game loop.
 *
 * Handles campaign loading, character creation, game state management,
 * and command processing for the QuestKeeper CLI game.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class GameEngine implements AutoCloseable {

    private static final double RANDOM_ENCOUNTER_CHANCE = 0.15; // 15% chance when moving

    private final Scanner scanner;
    private final Random random;
    private Campaign campaign;
    private GameState gameState;
    private DialogueSystem dialogueSystem;
    private CombatSystem combatSystem;
    private RestSystem restSystem;
    private boolean running;

    // Command handler infrastructure
    private CommandRouter commandRouter;
    private GameContext gameContext;

    public GameEngine() {
        this.scanner = new Scanner(System.in);
        this.random = new Random();
        this.dialogueSystem = new DialogueSystem();
        this.combatSystem = new CombatSystem();
        this.restSystem = new RestSystem();
        this.commandRouter = CommandRouter.createDefault();
        this.running = false;
    }

    /**
     * Starts the game - main entry point.
     */
    public void start() {
        Display.init();
        running = true;

        try {
            showTitleScreen();
            loadCampaign();
            Character character = createOrLoadCharacter();
            initializeGameState(character);
            runGameLoop();
        } catch (RuntimeException e) {
            Display.showError("Fatal error: " + e.getMessage());
            System.err.println("Stack trace for debugging:");
            e.printStackTrace(System.err);
        } finally {
            close();
        }
    }

    /**
     * Closes resources used by the game engine.
     */
    @Override
    public void close() {
        Display.shutdown();
        if (scanner != null) {
            scanner.close();
        }
    }

    private void showTitleScreen() {
        Display.clearScreen();
        Display.showTitleScreen();
        Display.println();
        Display.showGameMessage("Welcome, adventurer!");
        Display.println();
        Display.println("Press Enter to begin your journey...");
        scanner.nextLine();
    }

    private void loadCampaign() {
        Display.clearScreen();

        // Discover available campaigns
        List<CampaignInfo> availableCampaigns = Campaign.listAvailable();

        if (availableCampaigns.isEmpty()) {
            Display.showError("No campaigns found!");
            Display.println("Please ensure campaign files are in the 'campaigns/' directory.");
            throw new RuntimeException("No campaigns available");
        }

        CampaignInfo selectedCampaign;

        if (availableCampaigns.size() == 1) {
            // Only one campaign available, use it directly
            selectedCampaign = availableCampaigns.get(0);
            Display.println(Display.colorize("Loading campaign...", CYAN));
        } else {
            // Multiple campaigns available, show selection UI
            selectedCampaign = showCampaignSelection(availableCampaigns);
        }

        // Load the selected campaign
        Display.println();
        Display.println(Display.colorize("Loading: " + selectedCampaign.name() + "...", CYAN));
        campaign = Campaign.loadFromYaml(selectedCampaign.path());
        Display.println(Display.colorize("Loaded successfully!", GREEN));
        Display.println();

        if (campaign.hasValidationErrors()) {
            Display.showWarning("Campaign has " + campaign.getValidationErrors().size() + " validation warnings.");
        }

        Display.println(campaign.getSummary());
        Display.println();
        Display.println("Press Enter to continue...");
        scanner.nextLine();
    }

    private CampaignInfo showCampaignSelection(List<CampaignInfo> campaigns) {
        Display.println();
        Display.println(Display.colorize("SELECT YOUR CAMPAIGN", YELLOW));
        Display.println();

        // Sort campaigns by difficulty: Beginner, Intermediate, Advanced
        List<CampaignInfo> sortedCampaigns = sortCampaignsByDifficulty(campaigns);

        for (int i = 0; i < sortedCampaigns.size(); i++) {
            CampaignInfo info = sortedCampaigns.get(i);
            String difficulty = getCampaignDifficulty(info.id());
            String description = getCampaignDescription(info.id());

            Display.println("  " + (i + 1) + ". " + Display.colorize(info.name(), CYAN) + " (" + difficulty + ")");
            Display.println("     " + description);
            Display.println();
        }

        while (true) {
            Display.showPrompt("> ");
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= sortedCampaigns.size()) {
                    return sortedCampaigns.get(choice - 1);
                }
            } catch (NumberFormatException e) {
                // Invalid input, try again
            }

            Display.showError("Please enter a number between 1 and " + sortedCampaigns.size());
        }
    }

    private List<CampaignInfo> sortCampaignsByDifficulty(List<CampaignInfo> campaigns) {
        // Define difficulty order
        Map<String, Integer> difficultyOrder = Map.of(
            "muddlebrook", 1,
            "eberron", 2,
            "drownedgod", 3
        );

        List<CampaignInfo> sorted = new ArrayList<>(campaigns);
        sorted.sort((a, b) -> {
            int orderA = difficultyOrder.getOrDefault(a.id().toLowerCase(), 99);
            int orderB = difficultyOrder.getOrDefault(b.id().toLowerCase(), 99);
            return Integer.compare(orderA, orderB);
        });
        return sorted;
    }

    private String getCampaignDifficulty(String campaignId) {
        return switch (campaignId.toLowerCase()) {
            case "muddlebrook" -> "Beginner";
            case "eberron" -> "Intermediate";
            case "drownedgod" -> "Advanced";
            default -> "Unknown";
        };
    }

    private String getCampaignDescription(String campaignId) {
        return switch (campaignId.toLowerCase()) {
            case "muddlebrook" -> "Comedic mystery with theatrical puzzles";
            case "eberron" -> "Olympic competition with cosmic stakes";
            case "drownedgod" -> "Gothic horror with multiple endings";
            default -> "A mysterious adventure awaits";
        };
    }

    private Character createOrLoadCharacter() {
        Display.clearScreen();
        Display.printBox("CHARACTER SELECTION", 60, MAGENTA);
        Display.println();
        Display.println("1) Create a new character");
        Display.println("2) Quick start (pre-made character)");
        Display.println();
        Display.showPrompt("Choose (1-2): ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) {
            return createQuickStartCharacter();
        }

        return CharacterCreator.createCharacter();
    }

    private Character createQuickStartCharacter() {
        Display.println();
        Display.println(Display.colorize("Creating quick-start character...", CYAN));

        Character character = new Character(
            "Aldric",
            Character.Race.HUMAN,
            Character.CharacterClass.FIGHTER
        );

        // Set reasonable ability scores
        character.setAbilityScore(Character.Ability.STRENGTH, 16);
        character.setAbilityScore(Character.Ability.DEXTERITY, 14);
        character.setAbilityScore(Character.Ability.CONSTITUTION, 14);
        character.setAbilityScore(Character.Ability.INTELLIGENCE, 10);
        character.setAbilityScore(Character.Ability.WISDOM, 12);
        character.setAbilityScore(Character.Ability.CHARISMA, 10);

        // Add skill proficiencies
        character.addSkillProficiency(Character.Skill.ATHLETICS);
        character.addSkillProficiency(Character.Skill.PERCEPTION);

        // Add starting equipment (Fighter Option A equivalent)
        Inventory inventory = character.getInventory();
        com.questkeeper.inventory.StandardEquipment equip =
            com.questkeeper.inventory.StandardEquipment.getInstance();

        // Add items to inventory then equip them
        inventory.addItem(equip.getWeapon("longsword"));
        inventory.addItem(equip.getArmor("chain_mail"));
        inventory.addItem(equip.getArmor("shield"));

        // equipToSlot now properly retrieves items from inventory by ID
        inventory.equipToSlot(equip.getWeapon("longsword"), Inventory.EquipmentSlot.MAIN_HAND);
        inventory.equipToSlot(equip.getArmor("chain_mail"), Inventory.EquipmentSlot.ARMOR);
        inventory.equipToSlot(equip.getArmor("shield"), Inventory.EquipmentSlot.OFF_HAND);

        // Add ranged option (stays in backpack)
        inventory.addItem(equip.getWeapon("light_crossbow"));

        // Add starting gold
        inventory.addGold(10);

        Display.println(Display.colorize("Created: " + character.getName() + " the " +
            character.getRace().getDisplayName() + " " +
            character.getCharacterClass().getDisplayName(), GREEN));
        Display.println();
        Display.println("Press Enter to continue...");
        scanner.nextLine();

        return character;
    }

    private void initializeGameState(Character character) {
        gameState = new GameState(character, campaign);
        // Create game context for command handlers
        gameContext = new GameContext(
            campaign, gameState, dialogueSystem, combatSystem,
            restSystem, scanner, random
        );
    }

    private void runGameLoop() {
        // Clear screen from character creation
        Display.clearScreen();

        // Show campaign intro if available
        displayCampaignIntro();

        // Clear and show initial location
        Display.clearScreen();
        Display.showHeader();
        displayCurrentLocation();

        // Show tutorial tip on first play
        Display.showTutorialTip("Use 'look' to examine your surroundings, 'go <direction>' to move, and 'talk <name>' to speak with characters.");

        while (running) {
            // Show action prompt with suggestions
            Display.showActionPrompt(generateSuggestions());
            Display.showPrompt();
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        Display.println();
        Display.showHeader();
        Display.println(Display.colorize("Thanks for playing QuestKeeper!", YELLOW));
        Display.println("Your adventure lasted " + gameState.getFormattedPlayTime() + ".");
    }

    private void processCommand(String input) {
        Command command = CommandParser.parse(input);

        // If in conversation, allow typing just the topic name
        if (!command.isValid() && dialogueSystem.isInConversation()) {
            // Try to interpret as a topic
            handleAsk("ask about " + input);
            return;
        }

        if (!command.isValid()) {
            Display.showError("I don't understand '" + input + "'. Type 'help' for commands.");
            return;
        }

        String verb = command.getVerb();
        String noun = command.getNoun();

        // Try routing through command handlers first
        if (commandRouter.canHandle(verb)) {
            CommandResult result = commandRouter.route(gameContext, verb, noun, input);
            if (result != null) {
                // Handle special result types
                if (result.shouldQuit()) {
                    running = false;
                }
                if (result.shouldDisplayLocation()) {
                    displayCurrentLocation();
                }
                if (result.hasPlayerMoved()) {
                    // Post-move checks: trials and random encounters
                    checkForTrialAtLocation();
                    if (campaign.getTrialAtLocation(gameState.getCurrentLocation().getId()) == null) {
                        checkForRandomEncounter();
                    }
                }
                if (result.hasCombatStarted()) {
                    // Combat was initiated - run the combat loop
                    runCombatLoop();
                    if (running) {
                        displayCurrentLocation();
                    }
                }
                return;
            }
        }

        // Fall back to switch statement for commands not yet extracted
        switch (verb) {
            case "rest" -> handleRest(noun);
            case "save" -> handleSave();
            case "load" -> handleLoad();
            // Note: 'help' and 'quit' are now handled by SystemCommandHandler
            // Note: 'inventory', 'i', 'stats', 'equipment', 'equipped', 'gear' are handled by InventoryCommandHandler
            // Note: 'take', 'get', 'pickup', 'drop', 'equip', 'wear', 'wield', 'unequip', 'remove', 'use', 'activate' are handled by ItemCommandHandler
            // Note: 'talk', 'ask', 'buy', 'bye' are handled by DialogueCommandHandler
            // Note: 'look', 'go', directions, 'leave', 'exit' are handled by ExplorationCommandHandler
            // Note: 'attack' is handled by CombatCommandHandler
            // Note: 'trial', 'attempt', 'solve', 'try' are handled by TrialCommandHandler
            default -> Display.showError("Command '" + verb + "' is not yet implemented.");
        }
    }

    // ==========================================
    // Command Handlers
    // ==========================================

    // handleAsk is still needed for dialogue topic shortcut in processCommand
    private void handleAsk(String input) {
        if (!dialogueSystem.isInConversation()) {
            Display.showError("You're not talking to anyone. Use 'talk <name>' first.");
            return;
        }

        String[] parsed = CommandParser.parseAskAbout(input);
        String topic = parsed[1];

        if (topic == null || topic.isEmpty()) {
            NPC npc = dialogueSystem.getCurrentNpc();
            Display.println("What do you want to ask " + npc.getName() + " about?");
            Display.println("Available topics: " + String.join(", ",
                npc.getAvailableTopics(gameState.getFlags())));
            return;
        }

        DialogueResult result = dialogueSystem.askAbout(topic);
        displayDialogueResult(result);
    }

    // ==========================================
    // Save/Load System
    // ==========================================

    private void handleSave() {
        Display.println();
        Display.printBox("SAVE GAME", 50, CYAN);
        Display.println();

        // Get save name
        String defaultName = gameState.getCharacter().getName() + " - " + campaign.getId();
        Display.println("Enter save name (or press Enter for '" + defaultName + "'):");
        Display.showPrompt("Save name> ");
        String saveName = scanner.nextLine().trim();

        if (saveName.isEmpty()) {
            saveName = defaultName;
        }

        try {
            SaveState saveState = gameState.toSaveState();
            saveState.setSaveName(saveName);

            // Create filename from save name
            String filename = saveName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase() + ".yaml";
            Path savePath = Path.of("saves", filename);

            saveState.save(savePath);

            Display.println();
            Display.println(Display.colorize("Game saved successfully!", GREEN));
            Display.println("Saved to: " + savePath);
            Display.println();
        } catch (IOException e) {
            Display.showError("Failed to save game: " + e.getMessage());
        }
    }

    private void handleLoad() {
        Display.println();
        Display.printBox("LOAD GAME", 50, CYAN);
        Display.println();

        try {
            List<SaveState.SaveInfo> saves = SaveState.listSaves();

            if (saves.isEmpty()) {
                Display.println("No save files found.");
                Display.println("Save files are stored in the 'saves/' directory.");
                Display.println();
                return;
            }

            // Display available saves
            Display.println(Display.colorize("Available Saves:", WHITE));
            Display.println();

            int i = 1;
            for (SaveState.SaveInfo save : saves) {
                Display.println(String.format("  %d. %s", i++, Display.colorize(save.saveName(), YELLOW)));
                Display.println(String.format("     %s Lvl %d | %s | Played: %s",
                    save.characterName(),
                    save.characterLevel(),
                    save.campaignId(),
                    save.playTime()));
                Display.println(String.format("     Saved: %s", formatTimestamp(save.timestamp())));
                Display.println();
            }

            Display.println("Enter number to load, or 'cancel' to go back:");
            Display.showPrompt("Load> ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("cancel") || input.isEmpty()) {
                Display.println("Load cancelled.");
                return;
            }

            try {
                int choice = Integer.parseInt(input) - 1;
                if (choice >= 0 && choice < saves.size()) {
                    loadGame(saves.get(choice).path());
                } else {
                    Display.showError("Invalid selection.");
                }
            } catch (NumberFormatException e) {
                Display.showError("Please enter a number or 'cancel'.");
            }

        } catch (IOException e) {
            Display.showError("Failed to list saves: " + e.getMessage());
        }
    }

    private void loadGame(Path savePath) {
        try {
            Display.println();
            Display.println(Display.colorize("Loading save...", CYAN));

            SaveState saveState = SaveState.load(savePath);

            // Check campaign compatibility
            if (!saveState.getCampaignId().equals(campaign.getId())) {
                Display.showWarning("Save is from a different campaign (" +
                    saveState.getCampaignId() + "). Some data may not load correctly.");
            }

            // Restore game state
            gameState = GameState.fromSaveState(saveState, campaign);

            Display.println(Display.colorize("Game loaded successfully!", GREEN));
            Display.println();
            Display.println("Welcome back, " + gameState.getCharacter().getName() + "!");
            Display.println("Play time: " + gameState.getFormattedPlayTime());
            Display.println();

            // Show current location
            displayCurrentLocation();

        } catch (IOException e) {
            Display.showError("Failed to load save: " + e.getMessage());
        } catch (IllegalStateException e) {
            Display.showError("Invalid save data: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Display.showError("Error restoring game state: " + e.getMessage());
        }
    }

    private String formatTimestamp(java.time.Instant timestamp) {
        java.time.ZonedDateTime zdt = timestamp.atZone(java.time.ZoneId.systemDefault());
        return java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a").format(zdt);
    }

    // ==========================================
    // Rest System
    // ==========================================

    private void handleRest(String type) {
        Character character = gameState.getCharacter();

        // Can't rest during combat
        if (combatSystem.isInCombat()) {
            Display.showError("You cannot rest during combat!");
            return;
        }

        // Can't rest during dialogue
        if (dialogueSystem.isInConversation()) {
            Display.showError("You should end your conversation first.");
            return;
        }

        // Determine rest type
        if (type == null || type.isEmpty()) {
            // Show rest options
            showRestMenu(character);
            return;
        }

        String restType = type.toLowerCase();
        if (restType.equals("short") || restType.equals("s")) {
            performShortRest(character);
        } else if (restType.equals("long") || restType.equals("l")) {
            performLongRest(character);
        } else {
            Display.showError("Unknown rest type. Use 'rest short' or 'rest long'.");
        }
    }

    private void showRestMenu(Character character) {
        Display.println();
        Display.println(Display.colorize("=== Rest Options ===", CYAN));
        Display.println();

        // Show current status
        Display.println(String.format("HP: %d/%d  |  Hit Dice: %dd%d available",
                character.getCurrentHitPoints(),
                character.getMaxHitPoints(),
                character.getAvailableHitDice(),
                character.getHitDieSize()));
        Display.println();

        Display.println(Display.colorize("Short Rest", YELLOW) + " (1 hour)");
        Display.println("  Spend hit dice to heal. Roll 1d" + character.getHitDieSize() +
                " + " + character.getAbilityModifier(Character.Ability.CONSTITUTION) + " (CON) per die.");
        Display.println();

        Display.println(Display.colorize("Long Rest", YELLOW) + " (8 hours)");
        Display.println("  Fully restore HP. Regain half your hit dice.");
        Display.println();

        Display.showPrompt("Rest type (short/long/cancel)> ");
        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("short") || choice.equals("s")) {
            performShortRest(character);
        } else if (choice.equals("long") || choice.equals("l")) {
            performLongRest(character);
        } else {
            Display.println("Rest cancelled.");
        }
    }

    private void performShortRest(Character character) {
        Display.println();
        Display.println(Display.colorize("=== Short Rest ===", CYAN));

        if (character.getCurrentHitPoints() >= character.getMaxHitPoints()) {
            Display.println("You are already at full health.");
            Display.println();
            return;
        }

        if (character.getAvailableHitDice() <= 0) {
            Display.println("You have no hit dice remaining. Take a long rest to recover them.");
            Display.println();
            return;
        }

        Display.println();
        Display.println(String.format("You have %d hit dice (d%d) available.",
                character.getAvailableHitDice(), character.getHitDieSize()));
        Display.println("You settle down for a short rest...");
        Display.println();

        int totalHealed = 0;
        int diceUsed = 0;

        while (character.getAvailableHitDice() > 0 &&
                character.getCurrentHitPoints() < character.getMaxHitPoints()) {

            Display.showPrompt("Spend a hit die? (y/n)> ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (!choice.equals("y") && !choice.equals("yes")) {
                break;
            }

            // Roll hit die
            int roll = Dice.roll(character.getHitDieSize());
            int conMod = character.getAbilityModifier(Character.Ability.CONSTITUTION);
            int healing = Math.max(1, roll + conMod);
            int actualHealing = character.useHitDie();

            if (actualHealing < 0) {
                break;  // No more dice
            }

            diceUsed++;
            totalHealed += actualHealing;

            String modStr = conMod >= 0 ? "+" + conMod : String.valueOf(conMod);
            Display.println(String.format("  Rolled d%d: %d %s = %d HP restored (now %d/%d)",
                    character.getHitDieSize(), roll, modStr, actualHealing,
                    character.getCurrentHitPoints(), character.getMaxHitPoints()));

            if (character.getCurrentHitPoints() >= character.getMaxHitPoints()) {
                Display.println(Display.colorize("  You are now at full health!", GREEN));
                break;
            }

            Display.println(String.format("  %d hit dice remaining.", character.getAvailableHitDice()));
        }

        Display.println();
        if (diceUsed > 0) {
            Display.println(Display.colorize("Short rest complete.", GREEN) +
                    String.format(" Restored %d HP using %d hit dice.", totalHealed, diceUsed));
        } else {
            Display.println("You finish your rest without using any hit dice.");
        }
        Display.println();
    }

    private void performLongRest(Character character) {
        Display.println();
        Display.println(Display.colorize("=== Long Rest ===", CYAN));
        Display.println();
        Display.println("You settle down for a long rest...");
        Display.println();

        RestResult result = restSystem.longRest(character);

        Display.println(Display.colorize("After 8 hours of rest:", WHITE));
        Display.println();

        if (result.hpRestored() > 0) {
            Display.println(Display.colorize("  HP fully restored: ", GREEN) +
                    result.currentHp() + "/" + result.maxHp());
        } else {
            Display.println("  HP: " + result.currentHp() + "/" + result.maxHp() + " (already full)");
        }

        if (result.hitDiceRestored() > 0) {
            Display.println(Display.colorize("  Hit dice recovered: ", GREEN) +
                    "+" + result.hitDiceRestored() +
                    " (now " + result.availableHitDice() + "/" + result.maxHitDice() + ")");
        } else {
            Display.println("  Hit dice: " + result.availableHitDice() + "/" + result.maxHitDice() +
                    " (already full)");
        }

        Display.println();
        Display.println(Display.colorize("You feel refreshed and ready to continue your adventure.", CYAN));
        Display.println();
    }

    private void startCombat(List<Monster> enemies) {
        Display.println();
        Display.printDivider('=', 60, RED);
        Display.println(Display.colorize("  COMBAT ENCOUNTER!", RED));
        Display.printDivider('=', 60, RED);
        Display.println();

        CombatResult startResult = combatSystem.startCombat(gameState, enemies);
        displayCombatResult(startResult);

        if (startResult.isError()) {
            return;
        }

        // Run combat loop
        runCombatLoop();
    }

    private void runCombatLoop() {
        while (combatSystem.isInCombat() && running) {
            // Execute current turn
            CombatResult turnResult = combatSystem.executeTurn();

            // If it's the player's turn, wait for input
            if (turnResult.getType() == CombatResult.Type.TURN_START &&
                combatSystem.getCurrentCombatant() instanceof Character) {

                displayCombatStatus();
                Display.println(Display.colorize("Your turn! Actions: attack, flee", YELLOW));
                Display.showPrompt("Combat> ");

                String input = scanner.nextLine().trim().toLowerCase();
                if (input.isEmpty()) {
                    continue;
                }

                // Parse combat command
                String[] parts = input.split("\\s+", 2);
                String action = parts[0];
                String target = parts.length > 1 ? parts[1] : null;

                CombatResult actionResult = combatSystem.playerTurn(action, target);
                displayCombatResult(actionResult);

                if (actionResult.isCombatOver()) {
                    handleCombatEnd(actionResult);
                    return;
                }
            } else {
                // Enemy turn - result already processed
                displayCombatResult(turnResult);

                if (turnResult.isCombatOver()) {
                    handleCombatEnd(turnResult);
                    return;
                }

                // Small pause between enemy actions for readability
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handleCombatEnd(CombatResult result) {
        Display.println();
        Display.printDivider('=', 60, result.getType() == CombatResult.Type.VICTORY ? GREEN : RED);

        switch (result.getType()) {
            case VICTORY -> {
                Display.println(Display.colorize("  VICTORY!", GREEN));
                Display.println(Display.colorize("  You gained " + result.getXpGained() + " XP!", YELLOW));
            }
            case PLAYER_DEFEATED -> {
                Display.println(Display.colorize("  DEFEATED!", RED));
                Display.println(Display.colorize("  You have fallen in battle...", RED));
                Display.println();
                Display.println("Game Over. Press Enter to exit.");
                scanner.nextLine();
                running = false;
            }
            case FLED -> {
                Display.println(Display.colorize("  ESCAPED!", YELLOW));
                Display.println("You flee from combat!");
            }
            default -> Display.println(result.getMessage());
        }

        Display.printDivider('=', 60, WHITE);
        Display.println();

        // Show current location after combat ends (if still alive)
        if (running) {
            displayCurrentLocation();
        }
    }

    private void displayCombatStatus() {
        Display.println();
        Display.printDivider('-', 60, WHITE);

        // Show player status
        Character player = gameState.getCharacter();
        Display.print(Display.colorize("You: ", GREEN));
        Display.printHealthBar(player.getCurrentHitPoints(), player.getMaxHitPoints());

        // Show enemy status
        for (Combatant enemy : combatSystem.getLivingEnemies()) {
            Display.print(Display.colorize(enemy.getName() + ": ", RED));
            Display.printHealthBar(enemy.getCurrentHitPoints(), enemy.getMaxHitPoints());
        }

        Display.printDivider('-', 60, WHITE);
    }

    private void displayCombatResult(CombatResult result) {
        if (result == null) {
            return;
        }

        Display.println();

        switch (result.getType()) {
            case COMBAT_START -> {
                Display.println(result.getMessage());
                Display.println();
                Display.println(Display.colorize("Initiative Order:", CYAN));
                int i = 1;
                for (Combatant c : result.getTurnOrder()) {
                    Display.println(String.format("  %d. %s", i++, c.getCombatStatus()));
                }
            }
            case TURN_START -> {
                // Don't display "X's turn" for player, we handle that separately
                if (!(combatSystem.getCurrentCombatant() instanceof Character)) {
                    Display.println(Display.colorize(result.getMessage(), CYAN));
                }
            }
            case ATTACK_HIT -> {
                Display.println(Display.colorize(result.getMessage(),
                    result.getAttacker() instanceof Monster ? RED : GREEN));
            }
            case ATTACK_MISS -> {
                Display.println(Display.colorize(result.getMessage(), WHITE));
            }
            case ENEMY_DEFEATED -> {
                Display.println(Display.colorize(result.getMessage(), GREEN));
            }
            case PLAYER_DEFEATED -> {
                Display.println(Display.colorize(result.getMessage(), RED));
            }
            case ERROR -> {
                Display.println(Display.colorize(result.getMessage(), YELLOW));
            }
            case VICTORY -> {
                // Show the killing blow, handleCombatEnd() will show the victory box
                if (result.getMessage() != null && !result.getMessage().startsWith("Victory!")) {
                    Display.println(Display.colorize(result.getMessage(), GREEN));
                }
            }
            case FLED -> {
                // Handled by handleCombatEnd() - don't print here to avoid duplicate messages
            }
            default -> {
                Display.println(result.getMessage());
            }
        }
    }

    private void checkForRandomEncounter() {
        // Only trigger random encounters occasionally
        if (random.nextDouble() > RANDOM_ENCOUNTER_CHANCE) {
            return;
        }

        // Get a random monster from the campaign
        var templates = campaign.getMonsterTemplates();
        if (templates.isEmpty()) {
            return;
        }

        String[] monsterIds = templates.keySet().toArray(new String[0]);
        String randomId = monsterIds[random.nextInt(monsterIds.length)];
        Monster monster = campaign.createMonster(randomId);

        if (monster != null) {
            Display.println();
            Display.println(Display.colorize("A wild " + monster.getName() + " appears!", RED));
            Display.println();

            startCombat(List.of(monster));
        }
    }

    // ==========================================
    // Trial Handlers
    // ==========================================

    private int countCompletedTrials() {
        int count = 0;
        for (String trialId : campaign.getTrials().keySet()) {
            if (gameState.hasCompletedTrial(trialId)) {
                count++;
            }
        }
        return count;
    }

    private void checkForTrialAtLocation() {
        String locationId = gameState.getCurrentLocation().getId();
        Trial trial = campaign.getTrialAtLocation(locationId);

        if (trial == null || gameState.hasCompletedTrial(trial.getId())) {
            return;
        }

        // Check prerequisites
        if (!checkTrialPrerequisites(trial)) {
            return;
        }

        // Offer to start the trial
        Display.println();
        Display.printBox("TRIAL AVAILABLE", 60, MAGENTA);
        Display.println();
        Display.println(Display.colorize(trial.getName(), YELLOW));
        Display.println("This location contains a trial with " + trial.getMiniGameCount() + " challenges.");
        Display.println();
        Display.println("Type 'trial' to begin or continue exploring.");
        Display.println();
    }

    private boolean checkTrialPrerequisites(Trial trial) {
        // Data-driven prerequisite checking using Trial's prerequisites list
        if (!trial.hasPrerequisites()) {
            return true; // No prerequisites required
        }

        for (String prerequisite : trial.getPrerequisites()) {
            if (!gameState.hasFlag(prerequisite)) {
                return false;
            }
        }
        return true;
    }

    // ==========================================
    // Display Helpers
    // ==========================================

    private void displayCampaignIntro() {
        if (!campaign.hasIntro()) {
            return;
        }

        Display.clearScreen();
        Display.showHeader();
        Display.printBox("~ " + campaign.getName() + " ~", 60, MAGENTA);
        Display.println();

        // Display the intro text with blockquote style
        Display.showNarrationLabel("The adventure begins...");
        Display.println();

        String intro = campaign.getIntro();
        String[] paragraphs = intro.split("\n\n");

        for (String paragraph : paragraphs) {
            Display.showBlockquote(paragraph.trim());
            Display.println();
        }

        Display.println();
        Display.println(Display.colorize("Press Enter to begin your adventure...", YELLOW));
        scanner.nextLine();
        Display.clearScreen();
    }

    private void displayCurrentLocation() {
        Location location = gameState.getCurrentLocation();
        if (location == null) {
            Display.showError("You are nowhere... this shouldn't happen!");
            return;
        }

        // Show status panel at the top
        Character player = gameState.getCharacter();
        int completedTrials = countCompletedTrials();
        int totalTrials = campaign.getTrials().size();
        Display.showStatusPanel(
            player.getCurrentHitPoints(),
            player.getMaxHitPoints(),
            player.getLevel(),
            completedTrials,
            totalTrials
        );

        Display.println();
        Display.printBox(location.getName(), 60, CYAN);
        Display.println();

        // Show description (read-aloud for first visit, regular description otherwise)
        // Check visited status in GameState, not on Location object
        boolean hasReadAloud = location.getReadAloudText() != null && !location.getReadAloudText().isEmpty();
        boolean firstVisit = !gameState.hasVisited(location.getId());

        if (firstVisit && hasReadAloud) {
            // Show read-aloud in blockquote style
            Display.showNarrationLabel("As you enter...");
            Display.println();
            Display.showBlockquote(location.getReadAloudText());
        } else {
            Display.showBlockquote(location.getDescription());
        }

        Display.println();

        // Show NPCs with descriptors
        List<String> npcIds = location.getNpcs();
        if (!npcIds.isEmpty()) {
            Display.println(Display.colorize("You see:", WHITE));
            for (String npcId : npcIds) {
                NPC npc = campaign.getNPC(npcId);
                if (npc != null) {
                    String descriptor = npc.getShortDescriptor();
                    if (descriptor.isEmpty()) {
                        Display.println("  - " + Display.colorize(npc.getName(), CYAN));
                    } else {
                        Display.println("  - " + Display.colorize(npc.getName(), CYAN) +
                                       " (" + descriptor + ")");
                    }
                }
            }
            Display.println();
        }

        // Show items on the ground
        List<String> itemIds = location.getItems();
        if (!itemIds.isEmpty()) {
            Display.println(Display.colorize("Items here:", YELLOW));
            for (String itemId : itemIds) {
                var item = campaign.getItem(itemId);
                if (item != null) {
                    Display.println("  - " + item.getName());
                }
            }
            Display.println();
        }

        // Show exits with destinations
        Display.println(Display.colorize(buildExitsDisplay(location), WHITE));
        Display.println();
    }

    private String buildExitsDisplay(Location location) {
        Set<String> exitDirections = location.getExits();
        if (exitDirections.isEmpty()) {
            return "There are no obvious exits.";
        }

        StringBuilder sb = new StringBuilder("Exits: ");
        List<String> exitList = new ArrayList<>(exitDirections);
        Collections.sort(exitList);

        for (int i = 0; i < exitList.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String direction = exitList.get(i);
            String destId = location.getExit(direction);
            Location dest = campaign.getLocation(destId);

            // Show direction with destination name
            sb.append(Display.colorize(direction, CYAN));
            if (dest != null) {
                sb.append(" (").append(dest.getName()).append(")");
            }
        }

        return sb.toString();
    }

    private void displayDialogueResult(DialogueResult result) {
        Display.println();

        switch (result.getType()) {
            case SUCCESS -> {
                NPC npc = result.getNpc();
                Display.showDialogue(npc.getName(), result.getMessage());
            }
            case NO_TOPIC -> {
                Display.println(result.getNpc().getName() + " doesn't know about that.");
                List<String> topics = result.getAvailableTopics();
                if (topics != null && !topics.isEmpty()) {
                    Display.println("Try asking about: " + String.join(", ", topics));
                }
            }
            case ENDED -> {
                Display.println("You end your conversation.");
            }
            case ERROR -> {
                Display.showError(result.getMessage());
            }
        }
        Display.println();
    }

    /**
     * Generates contextual command suggestions based on current game state.
     */
    private String[] generateSuggestions() {
        List<String> suggestions = new ArrayList<>();
        Location location = gameState.getCurrentLocation();

        // Always suggest look
        suggestions.add("look");

        // Suggest talking to NPCs if present
        if (location != null && !location.getNpcs().isEmpty()) {
            String firstNpc = location.getNpcs().get(0);
            NPC npc = campaign.getNPC(firstNpc);
            if (npc != null) {
                suggestions.add("talk " + npc.getName().toLowerCase());
            }
        }

        // Suggest trial if at trial location
        if (location != null) {
            Trial trial = campaign.getTrialAtLocation(location.getId());
            if (trial != null && !gameState.hasCompletedTrial(trial.getId())) {
                suggestions.add("trial");
            }
        }

        // Suggest go if there are exits
        if (location != null && !location.getExits().isEmpty()) {
            String firstExit = location.getExits().iterator().next();
            suggestions.add("go " + firstExit);
        }

        // Suggest inventory and help
        suggestions.add("inventory");
        suggestions.add("help");

        return suggestions.toArray(new String[0]);
    }
}
