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
import com.questkeeper.ui.LevelUpFlow;
import com.questkeeper.world.Location;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

    /**
     * Class-action verbs the combat loop must pass to CombatSystem
     * unchanged, even though the global synonym map remaps some of
     * them outside combat (e.g., 'smite' -> attack, 'turn' -> use,
     * 'dash' -> go). Inside a fight, these mean the class action.
     */
    private static final Set<String> COMBAT_CLASS_VERBS = Set.of(
        "smite", "divinesmite",
        "rage", "frenzy", "reckless",
        "turn", "layonhands", "sacredweapon",
        "flurry", "patient", "step", "stun",
        "dash", "disengage", "hide",
        "inspire", "bardic",
        "surge", "secondwind",
        "wildshape", "fontofmagic", "sorcerypoints",
        "help", "?"
    );

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

    // Locations the player has been shown the full description for in
    // this session. Subsequent visits render compact. Cleared on load.
    private final Set<String> shownLocationsThisSession = new HashSet<>();

    // Drives the "Try: ..." nudge in the game loop. True for the first
    // turn after a context shift (game start, move to new room, look,
    // load, combat end, parse failure); flips false after one render so
    // routine commands don't repeat the hint.
    private boolean showHintNextTurn = true;

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

        // Share GameEngine's Scanner so type-ahead doesn't get split across two buffers
        CharacterCreator.setScanner(scanner);
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

        int lastSeenLevel = gameState.getCharacter().getLevel();

        while (running) {
            // Drain any pending Ability Score Improvements before the player
            // gets the next prompt, so XP earned in the prior tick (combat
            // victory, trial reward) finishes its level-up bookkeeping
            // before they act.
            Character player = gameState.getCharacter();
            if (player.getPendingAbilityScoreImprovements() > 0) {
                LevelUpFlow.applyPendingAbilityScoreImprovements(player, scanner);
            }

            // Auto-save on level-up. Best-effort; failures don't block.
            // Only fire on +1 level changes (genuine level-ups via XP) — a
            // larger jump means the player loaded a higher-level save and
            // we should not stomp their existing save with a fresh auto.
            int currentLevel = player.getLevel();
            if (currentLevel == lastSeenLevel + 1) {
                var autoPath = com.questkeeper.save.SaveState.autoSave(
                    gameState, "level-" + currentLevel);
                if (autoPath != null) {
                    Display.println(Display.colorize(
                        "[Auto-saved: " + autoPath + "]", YELLOW));
                } else {
                    // Don't let a milestone auto-save failure pass without a
                    // visible cue — the player might rely on it for rollback.
                    Display.showWarning("Auto-save for level " + currentLevel
                        + " failed; see stderr. Consider 'save' manually.");
                }
            }
            lastSeenLevel = currentLevel;

            // Show action prompt with suggestions only on context shifts
            // (first turn, new room, look, load, combat end, parse error).
            if (showHintNextTurn) {
                Display.showActionPrompt(generateSuggestions());
                showHintNextTurn = false;
            }
            Display.showPrompt(buildModePrompt());
            if (!scanner.hasNextLine()) {
                // EOF (Ctrl-D / piped input exhausted). Exit cleanly instead
                // of throwing NoSuchElementException up the stack.
                Display.println();
                Display.println(Display.colorize("Input closed. Exiting.", YELLOW));
                break;
            }
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

        // While in dialogue, default to interpreting input as a topic
        // ("ask about <input>"). Otherwise natural responses like
        // "i havent" get hijacked by the synonym map (i -> inventory).
        // Allow a small set of system verbs to escape: bye/quit/help/
        // ask/save/load/look/stats/clear and the explicit ask form.
        if (dialogueSystem.isInConversation()) {
            String lower = input.trim().toLowerCase();
            String firstWord = lower.split("\\s+", 2)[0];
            java.util.Set<String> allowed = java.util.Set.of(
                "bye", "farewell", "goodbye", "leave",
                "quit", "exit", "q",
                "help", "?",
                "ask",
                "save", "load",
                "look",
                "stats", "character",
                "clear", "cls"
            );
            if (!allowed.contains(firstWord)) {
                handleAsk("ask about " + input);
                return;
            }
        }

        // Bare exit name shortcut: "market" -> "go market" when an exit
        // by that name exists at the current location. Players naturally
        // type the suggestion shown in `Try: go market, look`.
        if (!command.isValid() && gameState != null) {
            var loc = gameState.getCurrentLocation();
            if (loc != null && loc.hasExit(input.trim().toLowerCase())) {
                command = CommandParser.parse("go " + input.trim());
            }
        }

        // Manual clear: wipe screen and re-render current location.
        String trimmed = input.trim().toLowerCase();
        if (trimmed.equals("clear") || trimmed.equals("cls")) {
            Display.clearScreen();
            displayCurrentLocation(true);
            return;
        }

        if (!command.isValid()) {
            Display.showError("I don't understand '" + input + "'. Type 'help' for commands.");
            showHintNextTurn = true;
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
                    // On movement, wipe the screen so each new room is a
                    // fresh slate. `look` keeps the scrolling buffer.
                    if (result.hasPlayerMoved()) {
                        Display.clearScreen();
                    }
                    // Movement renders compact if the location was already
                    // shown this session (cleaner exploration). `look`
                    // always renders full.
                    displayCurrentLocation(!result.hasPlayerMoved());
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
        // Mid-dialogue state is runtime-only and cannot survive load; end
        // the conversation first so the saved game has a defined entry point.
        if (dialogueSystem.isInConversation()) {
            Display.showError("Can't save mid-conversation. Type 'bye' first.");
            return;
        }
        // Combat state isn't persisted, so saving mid-fight would let the
        // player load out of the encounter (effectively cheat). Block it.
        if (combatSystem != null && combatSystem.isInCombat()) {
            Display.showError("Can't save mid-combat. Finish the fight or flee first.");
            return;
        }
        // Trial state IS in flags, but the active-trial UI mode isn't —
        // similar story: end the trial run before saving.
        if (gameContext != null && gameContext.getActiveTrial() != null) {
            Display.showError("Can't save inside a trial. Complete the current attempt first.");
            return;
        }

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

            // Warn before overwriting an existing save (the slug is many-to-one,
            // so different display names can map to the same file).
            if (java.nio.file.Files.exists(savePath)) {
                Display.println();
                Display.println(Display.colorize(
                    "A save already exists at: " + savePath, YELLOW));
                Display.println(Display.colorize(
                    "Overwrite? (y/n)", YELLOW));
                Display.showPrompt("> ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (!confirm.startsWith("y")) {
                    Display.println(Display.colorize("Save cancelled.", CYAN));
                    return;
                }
            }

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

            Display.println("Enter number to load, 'd <n>' to delete, 'r <n>' to rename, or 'cancel':");
            Display.showPrompt("Load> ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("cancel") || input.isEmpty()) {
                Display.println("Load cancelled.");
                return;
            }

            // Delete: "d <n>"
            if (input.startsWith("d ") || input.startsWith("delete ")) {
                String numStr = input.replaceFirst("^(d|delete)\\s+", "");
                handleDeleteSave(saves, numStr);
                return;
            }

            // Rename: "r <n>"
            if (input.startsWith("r ") || input.startsWith("rename ")) {
                String numStr = input.replaceFirst("^(r|rename)\\s+", "");
                handleRenameSave(saves, numStr);
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

    private void handleDeleteSave(List<SaveState.SaveInfo> saves, String numStr) {
        int idx;
        try {
            idx = Integer.parseInt(numStr.trim()) - 1;
        } catch (NumberFormatException e) {
            Display.showError("Expected: d <number>");
            return;
        }
        if (idx < 0 || idx >= saves.size()) {
            Display.showError("Save number out of range.");
            return;
        }
        SaveState.SaveInfo target = saves.get(idx);
        Display.println(Display.colorize(
            "Delete '" + target.saveName() + "'? This cannot be undone. (y/n)", YELLOW));
        Display.showPrompt("> ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.startsWith("y")) {
            Display.println("Delete cancelled.");
            return;
        }
        try {
            java.nio.file.Files.delete(target.path());
            Display.println(Display.colorize("Deleted: " + target.path(), GREEN));
        } catch (IOException e) {
            Display.showError("Delete failed: " + e.getMessage());
        }
    }

    private void handleRenameSave(List<SaveState.SaveInfo> saves, String numStr) {
        int idx;
        try {
            idx = Integer.parseInt(numStr.trim()) - 1;
        } catch (NumberFormatException e) {
            Display.showError("Expected: r <number>");
            return;
        }
        if (idx < 0 || idx >= saves.size()) {
            Display.showError("Save number out of range.");
            return;
        }
        SaveState.SaveInfo target = saves.get(idx);
        Display.println("Renaming '" + target.saveName() + "'.");
        Display.println("Enter new name (or blank to cancel):");
        Display.showPrompt("New name> ");
        String newName = scanner.nextLine().trim();
        if (newName.isEmpty()) {
            Display.println("Rename cancelled.");
            return;
        }
        try {
            // Update the save's display name and rewrite to a new slug-based path.
            SaveState saveState = SaveState.load(target.path());
            saveState.setSaveName(newName);
            String newSlug = newName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase() + ".yaml";
            Path newPath = target.path().resolveSibling(newSlug);
            if (!newPath.equals(target.path()) && java.nio.file.Files.exists(newPath)) {
                Display.showError("A save already exists at: " + newPath);
                return;
            }
            saveState.save(newPath);
            if (!newPath.equals(target.path())) {
                java.nio.file.Files.deleteIfExists(target.path());
            }
            Display.println(Display.colorize(
                "Renamed to '" + newName + "' (" + newPath + ")", GREEN));
        } catch (IOException e) {
            Display.showError("Rename failed: " + e.getMessage());
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

            // Restore game state and refresh the command-handler context so
            // routed commands (stats, inventory, etc.) see the new character.
            gameState = GameState.fromSaveState(saveState, campaign);
            gameContext = new GameContext(
                campaign, gameState, dialogueSystem, combatSystem,
                restSystem, scanner, random
            );
            // Reset the compact-revisit tracker so the loaded location
            // gets a full first-impression display.
            shownLocationsThisSession.clear();

            Display.println(Display.colorize("Game loaded successfully!", GREEN));
            Display.println();
            Display.println("Welcome back, " + gameState.getCharacter().getName() + "!");
            Display.println("Play time: " + gameState.getFormattedPlayTime());

            // Surface any partial-load warnings so the player knows what
            // didn't restore (e.g., items not in this campaign, slot
            // mismatches). Stays silent on a clean load.
            for (String warn : gameState.getLoadWarnings()) {
                Display.showWarning(warn);
            }
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

                // Parse combat command. Route the verb through the main
                // synonym map so 'stab', 'slash', 'pummel' etc. all resolve
                // to 'attack' just like outside combat. Class actions get
                // a pass-through allowlist so verbs that overlap the
                // synonym map (smite -> attack outside combat, but Divine
                // Smite during it) keep their class meaning here.
                String[] parts = input.split("\\s+", 2);
                String action = COMBAT_CLASS_VERBS.contains(parts[0])
                    ? parts[0]
                    : CommandParser.mapSynonym(parts[0]);
                String target = parts.length > 1 ? parts[1] : null;

                // System commands that should work even mid-combat
                if (action.equals("quit") || action.equals("q")) {
                    Display.println();
                    Display.println(Display.colorize("Quit during combat? Unsaved progress will be lost. (y/n)", RED));
                    Display.showPrompt("> ");
                    String confirm = scanner.nextLine().trim().toLowerCase();
                    if (confirm.startsWith("y")) {
                        running = false;
                        return;
                    }
                    Display.println(Display.colorize("Returning to combat.", CYAN));
                    continue;
                }
                if (action.equals("save")) {
                    Display.showError("Cannot save during combat. Finish or flee first.");
                    continue;
                }
                if (action.equals("load")) {
                    Display.showError("Cannot load during combat. Quit first, then load.");
                    continue;
                }
                if (action.equals("rest")) {
                    Display.showError("You can't rest with enemies nearby.");
                    continue;
                }
                if (action.equals("help") || action.equals("?")) {
                    // Delegate to the in-combat help dispatcher so this
                    // text reflects the current player's available class
                    // verbs (Bardic Inspiration, Wild Shape, etc.).
                    CombatResult helpResult = combatSystem.playerTurn("help", null);
                    if (helpResult != null && helpResult.getMessage() != null) {
                        Display.println(Display.colorize(helpResult.getMessage(), YELLOW));
                    }
                    Display.println(Display.colorize(
                        "System: quit (with confirmation). save/load/rest are blocked mid-combat.",
                        YELLOW));
                    continue;
                }
                // Read-only player info commands — route through the
                // command router so the player can check their inventory,
                // equipment, or stats mid-fight without losing their turn.
                if (action.equals("inventory") || action.equals("i")
                        || action.equals("stats") || action.equals("character")
                        || action.equals("equipment") || action.equals("equipped")
                        || action.equals("gear")) {
                    commandRouter.route(gameContext, action, target, input);
                    continue;
                }

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
        Display.println(Display.colorize("COMBATANTS", RED));
        Display.println();
        Character player = gameState.getCharacter();

        // Pick a label-column width that fits the longest name, with a
        // sensible floor so short rows still align.
        int labelWidth = 4; // "You" + space
        for (Combatant enemy : combatSystem.getLivingEnemies()) {
            labelWidth = Math.max(labelWidth, enemy.getName().length());
        }

        Display.println(Display.healthBarLine("You", CYAN,
            player.getCurrentHitPoints(), player.getMaxHitPoints(), labelWidth));
        for (Combatant enemy : combatSystem.getLivingEnemies()) {
            Display.println();
            Display.println(Display.healthBarLine(enemy.getName(), RED,
                enemy.getCurrentHitPoints(), enemy.getMaxHitPoints(), labelWidth));
        }
        Display.println();
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

        // Get a random monster from the campaign, excluding bosses (they
        // are reserved for scripted trial encounters; spawning them as
        // random encounters one-shots low-level characters).
        var templates = campaign.getMonsterTemplates();
        if (templates.isEmpty()) {
            return;
        }

        String[] monsterIds = templates.entrySet().stream()
            .filter(e -> !e.getValue().isBoss())
            .map(java.util.Map.Entry::getKey)
            .toArray(String[]::new);
        if (monsterIds.length == 0) {
            return;
        }
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
        Display.printBox("~ " + campaign.getName() + " ~", 60,
            Display.themeFor(campaign.getId()).accent());
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
        displayCurrentLocation(true);
    }

    /**
     * Renders the current location.
     *
     * @param fullDescription when true, prints the description blockquote,
     *     NPC list, items, and exits in full. When false (post-move on a
     *     revisited location), prints a compact one-section banner with
     *     just exits — players type {@code look} to re-show the prose.
     */
    private void displayCurrentLocation(boolean fullDescription) {
        Location location = gameState.getCurrentLocation();
        if (location == null) {
            Display.showError("You are nowhere... this shouldn't happen!");
            return;
        }

        // Full panels re-arm the next-turn hint. Compact revisit panels
        // (this location already shown once this session and the caller
        // didn't ask for full) stay quiet.
        boolean isCompactRevisit = !fullDescription
            && shownLocationsThisSession.contains(location.getId());
        if (!isCompactRevisit) {
            showHintNextTurn = true;
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

        // Compact path: location already seen this session, render terse.
        if (!fullDescription && shownLocationsThisSession.contains(location.getId())) {
            Display.println();
            Display.println(Display.colorize(
                "» " + location.getName(), CYAN));
            Display.println(Display.colorize(buildExitsDisplay(location), WHITE));
            Display.println();
            return;
        }

        // Full path: blockquote, NPCs, items, exits.
        shownLocationsThisSession.add(location.getId());

        Display.println();
        Display.printBox(location.getName(), 60,
            Display.themeFor(campaign.getId()).primary());
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
    /**
     * Builds a context-aware prompt string. Players see at a glance
     * whether they're in dialogue, mid-trial, or just exploring.
     */
    private String buildModePrompt() {
        if (gameContext != null && gameContext.isInDialogue()) {
            var npc = gameContext.getDialogueSystem().getCurrentNpc();
            if (npc != null) {
                return Display.colorize("[Talking with " + npc.getName() + "] > ", CYAN);
            }
            return Display.colorize("[Dialogue] > ", CYAN);
        }
        if (gameContext != null && gameContext.getActiveTrial() != null) {
            return Display.colorize("[Trial] > ", MAGENTA);
        }
        return "> ";
    }

    private String[] generateSuggestions() {
        List<String> suggestions = new ArrayList<>();
        Location location = gameState.getCurrentLocation();
        Character player = gameState.getCharacter();

        // Active trial gets top billing — players forget it's there.
        if (location != null) {
            Trial trial = campaign.getTrialAtLocation(location.getId());
            if (trial != null && !gameState.hasCompletedTrial(trial.getId())) {
                suggestions.add(gameState.hasStartedTrial(trial.getId()) ? "trial" : "trial");
            }
        }

        // Items on the floor → suggest taking the first one. Use the
        // LAST token of the display name as the hint — it's almost always
        // the most specific noun (e.g., "Mayor Alderwick's Journal" -> "journal").
        if (location != null && !location.getItems().isEmpty()) {
            String firstItemId = location.getItems().get(0);
            var item = campaign.getItem(firstItemId);
            if (item != null) {
                String[] tokens = item.getName().split("\\s+");
                String shortName = tokens[tokens.length - 1].toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
                if (!shortName.isEmpty()) {
                    suggestions.add("take " + shortName);
                }
            }
        }

        // NPCs present → suggest talking.
        if (location != null && !location.getNpcs().isEmpty()) {
            String firstNpc = location.getNpcs().get(0);
            NPC npc = campaign.getNPC(firstNpc);
            if (npc != null) {
                suggestions.add("talk " + npc.getName().toLowerCase().split("\\s+")[0]);
            }
        }

        // Low HP cue: under 50% → suggest rest.
        if (player != null && player.getMaxHitPoints() > 0
            && player.getCurrentHitPoints() * 2 < player.getMaxHitPoints()) {
            suggestions.add("rest");
        }

        // Movement, but only if no higher-priority context already filled the list.
        if (suggestions.size() < 4 && location != null && !location.getExits().isEmpty()) {
            String firstExit = location.getExits().iterator().next();
            suggestions.add("go " + firstExit);
        }

        // Look is always cheap and useful.
        if (!suggestions.contains("look")) suggestions.add("look");

        // Cap to 5 to stop the prompt from getting noisy.
        if (suggestions.size() > 5) suggestions = suggestions.subList(0, 5);

        return suggestions.toArray(new String[0]);
    }
}
