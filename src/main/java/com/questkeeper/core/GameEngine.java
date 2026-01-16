package com.questkeeper.core;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.campaign.MiniGame;
import com.questkeeper.campaign.Trial;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.character.NPC;
import com.questkeeper.combat.CombatResult;
import com.questkeeper.combat.CombatSystem;
import com.questkeeper.combat.Combatant;
import com.questkeeper.combat.Monster;
import com.questkeeper.core.CommandParser.Command;
import com.questkeeper.dialogue.DialogueResult;
import com.questkeeper.dialogue.DialogueSystem;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Item;
import com.questkeeper.save.SaveState;
import com.questkeeper.state.GameState;
import com.questkeeper.ui.CharacterCreator;
import com.questkeeper.ui.Display;
import com.questkeeper.world.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

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
public class GameEngine {

    private static final String DEFAULT_CAMPAIGN = "muddlebrook";
    private static final double RANDOM_ENCOUNTER_CHANCE = 0.15; // 15% chance when moving

    private final Scanner scanner;
    private final Random random;
    private Campaign campaign;
    private GameState gameState;
    private DialogueSystem dialogueSystem;
    private CombatSystem combatSystem;
    private Trial activeTrial;
    private boolean running;

    public GameEngine() {
        this.scanner = new Scanner(System.in);
        this.random = new Random();
        this.dialogueSystem = new DialogueSystem();
        this.combatSystem = new CombatSystem();
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
            Display.shutdown();
        }
    }

    private void showTitleScreen() {
        Display.clearScreen();
        Display.println();
        Display.printBox("QUESTKEEPER", 60, YELLOW);
        Display.printBox("A D&D 5e Auto DM Adventure", 60, WHITE);
        Display.println();
        Display.println(Display.colorize("Welcome, adventurer!", CYAN));
        Display.println();
        Display.println("Press Enter to begin your journey...");
        scanner.nextLine();
    }

    private void loadCampaign() {
        Display.clearScreen();
        Display.println(Display.colorize("Loading campaign...", CYAN));

        // Try to load from classpath resources first (packaged JAR)
        Path resourcePath = findCampaignPath(DEFAULT_CAMPAIGN);
        campaign = Campaign.loadFromYaml(resourcePath);
        Display.println(Display.colorize("Loaded: " + campaign.getName(), GREEN));
        Display.println();

        if (campaign.hasValidationErrors()) {
            Display.showWarning("Campaign has " + campaign.getValidationErrors().size() + " validation warnings.");
        }

        Display.println(campaign.getSummary());
        Display.println();
        Display.println("Press Enter to continue...");
        scanner.nextLine();
    }

    private Path findCampaignPath(String campaignId) {
        // Try multiple possible locations
        String[] possiblePaths = {
            "src/main/resources/campaigns/" + campaignId,
            "campaigns/" + campaignId,
            "../resources/campaigns/" + campaignId
        };

        for (String pathStr : possiblePaths) {
            Path path = Path.of(pathStr);
            if (Files.exists(path) && Files.isDirectory(path)) {
                return path;
            }
        }

        // Default fallback
        return Path.of("src/main/resources/campaigns/" + campaignId);
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
        Display.println(Display.colorize("Game state initialized.", GREEN));
    }

    private void runGameLoop() {
        // Show initial location
        displayCurrentLocation();

        while (running) {
            Display.showPrompt();
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        Display.println();
        Display.println(Display.colorize("Thanks for playing QuestKeeper!", YELLOW));
        Display.println("Your adventure lasted " + gameState.getFormattedPlayTime() + ".");
    }

    private void processCommand(String input) {
        Command command = CommandParser.parse(input);

        if (!command.isValid()) {
            Display.showError("I don't understand '" + input + "'. Type 'help' for commands.");
            return;
        }

        String verb = command.getVerb();
        String noun = command.getNoun();

        switch (verb) {
            case "look" -> handleLook(noun);
            case "go" -> handleGo(noun);
            case "talk" -> handleTalk(noun);
            case "ask" -> handleAsk(input);
            case "attack" -> handleAttack(noun);
            case "trial" -> handleTrial();
            case "attempt", "solve", "try" -> handleAttempt(noun);
            case "inventory", "i" -> handleInventory();
            case "equipment", "equipped", "gear" -> showEquippedItems();
            case "stats" -> handleStats();
            case "take", "get", "pickup" -> handleTake(noun);
            case "drop" -> handleDrop(noun);
            case "equip", "wear", "wield" -> handleEquip(noun);
            case "unequip", "remove" -> handleUnequip(noun);
            case "help" -> Display.showHelp();
            case "save" -> handleSave();
            case "load" -> handleLoad();
            case "quit" -> handleQuit();
            default -> Display.showError("Command '" + verb + "' is not yet implemented.");
        }
    }

    // ==========================================
    // Command Handlers
    // ==========================================

    private void handleLook(String target) {
        if (target == null || target.isEmpty()) {
            displayCurrentLocation();
            return;
        }

        // Look at specific thing
        Location location = gameState.getCurrentLocation();

        // Check if looking at an NPC
        for (String npcId : location.getNpcs()) {
            NPC npc = campaign.getNPC(npcId);
            if (npc != null && matchesTarget(npc.getName(), target)) {
                Display.println();
                Display.println(Display.colorize(npc.getName(), CYAN));
                Display.println(npc.getDescription());
                Display.println();
                return;
            }
        }

        // Check if looking at an item in the location
        for (String itemId : location.getItems()) {
            if (matchesTarget(itemId, target)) {
                var item = campaign.getItem(itemId);
                if (item != null) {
                    Display.println();
                    Display.println(Display.colorize(item.getName(), YELLOW));
                    Display.println(item.getDescription());
                    Display.println();
                    return;
                }
            }
        }

        Display.showError("You don't see '" + target + "' here.");
    }

    private void handleGo(String direction) {
        if (direction == null || direction.isEmpty()) {
            Display.showError("Go where? Specify a direction (north, south, east, west, etc.)");
            return;
        }

        Location currentLocation = gameState.getCurrentLocation();
        if (!currentLocation.hasExit(direction)) {
            Display.showError("You can't go " + direction + " from here.");
            Display.println(currentLocation.getExitsDisplay());
            return;
        }

        boolean moved = gameState.move(direction);
        if (moved) {
            // End any active conversation when moving
            if (dialogueSystem.isInConversation()) {
                dialogueSystem.endDialogue();
            }
            displayCurrentLocation();

            // Check for trial at new location
            checkForTrialAtLocation();

            // Random encounters only when not at a trial location
            if (campaign.getTrialAtLocation(gameState.getCurrentLocation().getId()) == null) {
                checkForRandomEncounter();
            }
        } else {
            Location target = campaign.getLocation(currentLocation.getExit(direction));
            if (target != null && !target.isUnlocked()) {
                // Provide context-specific locked messages
                String lockedMessage = getLockedLocationMessage(target.getId());
                Display.showError(lockedMessage);
            } else {
                Display.showError("You can't go that way.");
            }
        }
    }

    private String getLockedLocationMessage(String locationId) {
        return switch (locationId) {
            case "clocktower_hill" -> "The path to Clocktower Hill is blocked. Perhaps investigating " +
                    "the Town Hall and completing the trial there will reveal a way forward.";
            case "harlequin_lair" -> "A hidden passage, but it's sealed tight. You'll need to discover " +
                    "the Clocktower's secrets first.";
            case "back_room" -> "Elara's back room is off limits... unless you can earn her trust.";
            default -> "That way is currently blocked or locked.";
        };
    }

    private void handleTalk(String target) {
        if (target == null || target.isEmpty()) {
            // List available NPCs
            List<NPC> npcs = dialogueSystem.getNpcsAtCurrentLocation(gameState);
            if (npcs.isEmpty()) {
                Display.println("There's no one here to talk to.");
            } else {
                Display.println("You can talk to:");
                for (NPC npc : npcs) {
                    Display.println("  - " + Display.colorize(npc.getName(), CYAN));
                }
            }
            return;
        }

        DialogueResult result = dialogueSystem.startDialogue(gameState, target);
        displayDialogueResult(result);
    }

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

    private void handleInventory() {
        Character character = gameState.getCharacter();
        var inventory = character.getInventory();

        Display.println();
        Display.printBox("INVENTORY", 50, YELLOW);
        Display.println();

        // Show key equipped items at a glance
        var equipped = inventory.getEquippedItems();
        Display.println(Display.colorize("Equipped:", WHITE));
        Item weapon = equipped.get(EquipmentSlot.MAIN_HAND);
        Item armor = equipped.get(EquipmentSlot.ARMOR);
        Item offhand = equipped.get(EquipmentSlot.OFF_HAND);
        Display.println("  Weapon: " + (weapon != null ? weapon.getName() : "(none)"));
        Display.println("  Armor:  " + (armor != null ? armor.getName() : "(none)"));
        if (offhand != null) {
            Display.println("  Off-hand: " + offhand.getName());
        }
        Display.println(Display.colorize("  (type 'equipment' for full list)", DEFAULT));
        Display.println();

        Display.println(Display.colorize("Gold: ", WHITE) +
            Display.colorize(String.valueOf(inventory.getGold()) + " gp", YELLOW));
        Display.println(Display.colorize("Weight: ", WHITE) +
            String.format("%.1f / %.1f lbs", inventory.getCurrentWeight(), inventory.getMaxWeight()));
        Display.println();

        var items = inventory.getAllItems();
        if (items.isEmpty()) {
            Display.println("Your pack is empty.");
        } else {
            Display.println(Display.colorize("Backpack:", WHITE));
            for (var stack : items) {
                String countStr = stack.getQuantity() > 1 ? " (x" + stack.getQuantity() + ")" : "";
                Display.println("  - " + stack.getItem().getName() + countStr);
            }
        }

        Display.println();
    }

    private void handleStats() {
        Character character = gameState.getCharacter();

        Display.println();
        Display.printBox(character.getName(), 60, GREEN);
        Display.println();

        Display.println(String.format("Level %d %s %s",
            character.getLevel(),
            character.getRace().getDisplayName(),
            character.getCharacterClass().getDisplayName()));
        Display.println();

        Display.printHealthBar(character.getCurrentHitPoints(), character.getMaxHitPoints());
        Display.println(Display.colorize("AC: ", WHITE) + character.getArmorClass() +
            "  " + Display.colorize("Speed: ", WHITE) + character.getRace().getSpeed() + " ft");
        Display.println();

        Display.println(Display.colorize("Ability Scores:", WHITE));
        Display.println(character.getAbilityScoresString());
        Display.println();

        Display.println(Display.colorize("Proficiency Bonus: ", WHITE) + "+" + character.getProficiencyBonus());
        Display.println();
    }

    private void handleTake(String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Take what?");
            return;
        }

        Location location = gameState.getCurrentLocation();
        List<String> items = location.getItems();

        for (String itemId : items) {
            if (matchesTarget(itemId, target)) {
                var item = campaign.getItem(itemId);
                if (item != null) {
                    // Add to inventory and remove from location
                    gameState.getCharacter().getInventory().addItem(item);
                    location.removeItem(itemId);
                    Display.showItemGained(item.getName(), item.getDescription());
                    return;
                }
            }
        }

        Display.showError("You don't see '" + target + "' here to take.");
    }

    private void handleDrop(String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Drop what?");
            return;
        }

        var inventory = gameState.getCharacter().getInventory();
        var items = inventory.getAllItems();

        for (var stack : items) {
            if (matchesTarget(stack.getItem().getName(), target) ||
                matchesTarget(stack.getItem().getId(), target)) {
                var item = stack.getItem();
                inventory.removeItem(item);
                gameState.getCurrentLocation().addItem(item.getId());
                Display.println("You drop the " + item.getName() + ".");
                return;
            }
        }

        Display.showError("You don't have '" + target + "' in your inventory.");
    }

    private void handleEquip(String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Equip what? Try 'equip <item name>'");
            return;
        }

        var inventory = gameState.getCharacter().getInventory();
        List<Item> matches = inventory.findItemsByName(target);

        if (matches.isEmpty()) {
            Display.showError("You don't have anything called '" + target + "' in your inventory.");
            return;
        }

        // Filter to only equippable items
        List<Item> equippable = matches.stream()
                .filter(Item::isEquippable)
                .toList();

        if (equippable.isEmpty()) {
            Display.showError("'" + matches.get(0).getName() + "' cannot be equipped.");
            return;
        }

        Item toEquip;
        if (equippable.size() == 1) {
            toEquip = equippable.get(0);
        } else {
            // Multiple matches - let player choose
            Display.println();
            Display.println(Display.colorize("Multiple items match. Which one?", CYAN));
            for (int i = 0; i < equippable.size(); i++) {
                Display.println(String.format("  %d) %s", i + 1, equippable.get(i).getName()));
            }
            Display.showPrompt("Choice (1-" + equippable.size() + ")> ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice < 1 || choice > equippable.size()) {
                    Display.showError("Invalid choice.");
                    return;
                }
                toEquip = equippable.get(choice - 1);
            } catch (NumberFormatException e) {
                Display.showError("Please enter a number.");
                return;
            }
        }

        // Equip the item
        Item previousItem = inventory.equip(toEquip);

        // Determine what slot it went to for display
        String slotName = getSlotNameForItem(toEquip);

        Display.println();
        Display.println(Display.colorize("Equipped: ", GREEN) + toEquip.getName() +
                Display.colorize(" (" + slotName + ")", WHITE));

        if (previousItem != null) {
            Display.println(Display.colorize("Unequipped: ", YELLOW) + previousItem.getName() +
                    " (returned to inventory)");
        }
        Display.println();
    }

    private String getSlotNameForItem(Item item) {
        if (item instanceof com.questkeeper.inventory.Weapon) {
            return "Main Hand";
        } else if (item instanceof com.questkeeper.inventory.Armor armor) {
            return armor.isShield() ? "Off Hand" : "Armor";
        }
        return "Equipment";
    }

    private void handleUnequip(String target) {
        if (target == null || target.isEmpty()) {
            showEquippedItems();
            Display.println("Use 'unequip <slot>' or 'unequip <item name>' to unequip.");
            Display.println("Slots: weapon, armor, shield, offhand, head, neck, ring");
            Display.println();
            return;
        }

        var inventory = gameState.getCharacter().getInventory();
        String lowerTarget = target.toLowerCase();

        // Try to match by slot name first
        EquipmentSlot slot = parseSlotName(lowerTarget);

        if (slot != null) {
            Item unequipped = inventory.unequip(slot);
            if (unequipped != null) {
                Display.println();
                Display.println(Display.colorize("Unequipped: ", GREEN) + unequipped.getName() +
                        " (returned to inventory)");
                Display.println();
            } else {
                Display.showError("Nothing equipped in " + slot.getDisplayName() + " slot.");
            }
            return;
        }

        // Try to find equipped item by name
        var equippedItems = inventory.getEquippedItems();
        for (var entry : equippedItems.entrySet()) {
            if (entry.getValue().getName().toLowerCase().contains(lowerTarget)) {
                Item unequipped = inventory.unequip(entry.getKey());
                Display.println();
                Display.println(Display.colorize("Unequipped: ", GREEN) + unequipped.getName() +
                        " (returned to inventory)");
                Display.println();
                return;
            }
        }

        Display.showError("'" + target + "' is not equipped. Use 'unequip' to see equipped items.");
    }

    private EquipmentSlot parseSlotName(String name) {
        return switch (name) {
            case "weapon", "main hand", "mainhand", "main" -> EquipmentSlot.MAIN_HAND;
            case "offhand", "off hand", "off", "shield" -> EquipmentSlot.OFF_HAND;
            case "armor", "body", "chest" -> EquipmentSlot.ARMOR;
            case "head", "helmet", "helm", "hat" -> EquipmentSlot.HEAD;
            case "neck", "necklace", "amulet" -> EquipmentSlot.NECK;
            case "ring", "ring left", "left ring", "ringleft" -> EquipmentSlot.RING_LEFT;
            case "ring right", "right ring", "ringright" -> EquipmentSlot.RING_RIGHT;
            default -> null;
        };
    }

    private void showEquippedItems() {
        var inventory = gameState.getCharacter().getInventory();
        var equipped = inventory.getEquippedItems();

        Display.println();
        Display.printBox("EQUIPPED ITEMS", 50, MAGENTA);
        Display.println();

        if (equipped.isEmpty()) {
            Display.println("Nothing equipped.");
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                Item item = equipped.get(slot);
                String slotDisplay = String.format("%-12s", slot.getDisplayName() + ":");
                if (item != null) {
                    Display.println(Display.colorize(slotDisplay, WHITE) + item.getName());
                } else {
                    Display.println(Display.colorize(slotDisplay, WHITE) +
                            Display.colorize("(empty)", DEFAULT));
                }
            }
        }
        Display.println();
    }

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

    private void handleQuit() {
        Display.println();
        Display.println("Are you sure you want to quit? (y/n)");
        Display.showPrompt();
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            running = false;
        }
    }

    private void handleAttack(String target) {
        // If already in combat, process attack
        if (combatSystem.isInCombat()) {
            CombatResult result = combatSystem.playerTurn("attack", target);
            displayCombatResult(result);

            if (result.isCombatOver()) {
                handleCombatEnd(result);
                return;
            }

            // Process enemy turns
            processEnemyTurns();
            return;
        }

        // Not in combat - start combat with a monster
        if (target == null || target.isEmpty()) {
            Display.showError("Attack what? Use 'attack <monster>' to start combat.");
            listAvailableMonsters();
            return;
        }

        // Find monster to fight
        Monster monster = findMonsterByName(target);
        if (monster == null) {
            Display.showError("There's no '" + target + "' here to fight.");
            listAvailableMonsters();
            return;
        }

        // Start combat
        startCombat(List.of(monster));
    }

    private void listAvailableMonsters() {
        var monsters = campaign.getMonsterTemplates();
        if (!monsters.isEmpty()) {
            Display.println("Available monsters in this campaign:");
            for (String id : monsters.keySet()) {
                Monster m = monsters.get(id);
                Display.println("  - " + Display.colorize(m.getName(), RED) +
                    " (CR " + m.getChallengeRating() + ")");
            }
        }
    }

    private Monster findMonsterByName(String name) {
        String searchTerm = name.trim().toLowerCase();
        var templates = campaign.getMonsterTemplates();

        for (String id : templates.keySet()) {
            Monster template = templates.get(id);
            if (template.getName().toLowerCase().contains(searchTerm) ||
                id.toLowerCase().contains(searchTerm)) {
                // Create a new instance from the template
                return campaign.createMonster(id);
            }
        }
        return null;
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

    private void processEnemyTurns() {
        while (combatSystem.isInCombat()) {
            Combatant current = combatSystem.getCurrentCombatant();
            if (current == null || current instanceof Character) {
                // Back to player's turn
                if (combatSystem.isInCombat()) {
                    displayCombatStatus();
                }
                return;
            }

            // Enemy turn
            CombatResult result = combatSystem.executeTurn();
            displayCombatResult(result);

            if (result.isCombatOver()) {
                handleCombatEnd(result);
                return;
            }

            // Small pause for readability
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

    private void checkForTrialAtLocation() {
        String locationId = gameState.getCurrentLocation().getId();
        Trial trial = campaign.getTrialAtLocation(locationId);

        if (trial == null || trial.isCompleted()) {
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
        // Check if location has prerequisites flag
        // For now, check if previous trials are complete based on trial ID
        String trialId = trial.getId();

        if (trialId.equals("trial_02")) {
            return gameState.hasFlag("trial_01_complete");
        } else if (trialId.equals("trial_03")) {
            return gameState.hasFlag("trial_02_complete");
        }

        return true; // No prerequisites for trial_01
    }

    private void handleTrial() {
        String locationId = gameState.getCurrentLocation().getId();
        Trial trial = campaign.getTrialAtLocation(locationId);

        if (trial == null) {
            Display.showError("There's no trial at this location.");
            listAvailableTrials();
            return;
        }

        if (trial.isCompleted()) {
            Display.println("You've already completed this trial.");
            return;
        }

        if (!checkTrialPrerequisites(trial)) {
            Display.showError("You haven't completed the prerequisites for this trial.");
            return;
        }

        // Start or continue the trial
        if (!trial.isStarted()) {
            startTrial(trial);
        } else {
            displayTrialStatus(trial);
        }
    }

    private void listAvailableTrials() {
        var trials = campaign.getTrials();
        if (!trials.isEmpty()) {
            Display.println("Trials in this campaign:");
            for (Trial t : trials.values()) {
                String status = t.isCompleted() ? "[COMPLETE]" :
                               t.isStarted() ? "[IN PROGRESS]" : "[NOT STARTED]";
                Display.println("  - " + Display.colorize(t.getName(), MAGENTA) +
                    " at " + t.getLocation() + " " + status);
            }
        }
    }

    private void startTrial(Trial trial) {
        activeTrial = trial;

        Display.println();
        Display.printDivider('=', 60, MAGENTA);
        Display.showTrialHeader(trial.getName(), getTrialNumber(trial));
        Display.printDivider('=', 60, MAGENTA);
        Display.println();

        // Show entry narrative
        String narrative = trial.start();
        Display.showNarrative(narrative);
        Display.println();

        Display.println("Press Enter to continue...");
        scanner.nextLine();

        displayTrialStatus(trial);
    }

    private int getTrialNumber(Trial trial) {
        String id = trial.getId();
        if (id.contains("01")) return 1;
        if (id.contains("02")) return 2;
        if (id.contains("03")) return 3;
        return 0;
    }

    private void displayTrialStatus(Trial trial) {
        Display.println();
        Display.printBox(trial.getName(), 60, MAGENTA);
        Display.println();

        Display.println(Display.colorize("Challenges:", WHITE));
        Display.println();

        int i = 1;
        for (MiniGame game : trial.getMiniGames()) {
            String status = game.isCompleted() ?
                Display.colorize("[COMPLETE]", GREEN) :
                Display.colorize("[INCOMPLETE]", YELLOW);

            Display.println(String.format("  %d. %s %s", i++, game.getName(), status));

            if (!game.isCompleted()) {
                Display.println("     " + Display.colorize(game.getDescription().split("\n")[0], WHITE));
                Display.println("     Skills: " + Display.colorize(getSkillOptions(game), CYAN));
                Display.println("     DC: " + game.getDc());
            }
            Display.println();
        }

        Display.println("Use 'attempt <challenge name>' or 'attempt <number>' to try a challenge.");
        Display.println("Use 'attempt <challenge> with <skill>' to use a specific skill.");
        Display.println();
    }

    private String getSkillOptions(MiniGame game) {
        StringBuilder sb = new StringBuilder();
        if (game.getRequiredSkill() != null) {
            sb.append(game.getRequiredSkill().getDisplayName());
        }
        if (game.getAlternateSkill() != null) {
            sb.append(" or ").append(game.getAlternateSkill().getDisplayName());
        }
        return sb.toString();
    }

    private void handleAttempt(String target) {
        // Check if we're in a trial
        String locationId = gameState.getCurrentLocation().getId();
        Trial trial = campaign.getTrialAtLocation(locationId);

        if (trial == null || !trial.isStarted()) {
            Display.showError("You're not in an active trial. Use 'trial' to start one.");
            return;
        }

        if (target == null || target.isEmpty()) {
            Display.showError("Attempt what? Use 'attempt <challenge>' or 'attempt <number>'.");
            displayTrialStatus(trial);
            return;
        }

        // Parse "attempt X with Y" syntax
        String challengeName = target;
        String skillName = null;

        if (target.toLowerCase().contains(" with ")) {
            String[] parts = target.toLowerCase().split(" with ");
            challengeName = parts[0].trim();
            skillName = parts.length > 1 ? parts[1].trim() : null;
        }

        // Find the mini-game
        MiniGame game = findMiniGame(trial, challengeName);
        if (game == null) {
            Display.showError("Couldn't find challenge '" + challengeName + "'.");
            displayTrialStatus(trial);
            return;
        }

        if (game.isCompleted()) {
            Display.println("You've already completed '" + game.getName() + "'.");
            return;
        }

        // Determine which skill to use
        Skill skill = determineSkill(game, skillName);
        if (skill == null) {
            Display.showError("Invalid skill. Use: " + getSkillOptions(game));
            return;
        }

        // Attempt the challenge!
        attemptMiniGame(trial, game, skill);
    }

    private MiniGame findMiniGame(Trial trial, String search) {
        List<MiniGame> games = trial.getMiniGames();

        // Try as number first
        try {
            int index = Integer.parseInt(search.trim()) - 1;
            if (index >= 0 && index < games.size()) {
                return games.get(index);
            }
        } catch (NumberFormatException ignored) {
            // Not a number, try name matching
        }

        // Search by name
        String searchLower = search.toLowerCase();
        for (MiniGame game : games) {
            if (game.getName().toLowerCase().contains(searchLower) ||
                game.getId().toLowerCase().contains(searchLower)) {
                return game;
            }
        }

        return null;
    }

    private Skill determineSkill(MiniGame game, String skillName) {
        if (skillName != null && !skillName.isEmpty()) {
            // Player specified a skill
            try {
                Skill skill = Skill.valueOf(skillName.toUpperCase().replace(" ", "_"));
                if (game.isValidApproach(skill)) {
                    return skill;
                }
                return null; // Invalid skill for this challenge
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // Default to required skill
        return game.getRequiredSkill();
    }

    private void attemptMiniGame(Trial trial, MiniGame game, Skill skill) {
        Display.println();
        Display.printBox(game.getName(), 60, CYAN);
        Display.println();

        // Show description
        Display.showNarrative(game.getDescription());
        Display.println();

        // Show hint
        if (!game.getHint().isEmpty()) {
            Display.println(Display.colorize("Hint: ", YELLOW) + game.getHint());
            Display.println();
        }

        Display.println(String.format("Attempting with %s (DC %d)...",
            Display.colorize(skill.getDisplayName(), CYAN), game.getDc()));
        Display.println();
        Display.println("Press Enter to roll...");
        scanner.nextLine();

        // Perform the skill check
        try {
            MiniGame.EvaluationResult result = game.evaluate(gameState.getCharacter(), skill.name());
            displayMiniGameResult(result);

            // Handle rewards
            if (result.success() && result.hasReward()) {
                grantMiniGameReward(game);
            }

            // Handle consequences
            if (!result.success() && result.hasConsequence()) {
                applyMiniGameConsequence(game, result);
            }

            // Check if trial is complete
            if (trial.checkComplete()) {
                completeTrial(trial);
            }

        } catch (IllegalArgumentException e) {
            Display.showError(e.getMessage());
        }
    }

    private void displayMiniGameResult(MiniGame.EvaluationResult result) {
        Display.println();
        Display.printDivider('-', 60, WHITE);

        // Show roll
        Display.showSkillCheck(
            result.rollDescription().split("\\+")[1].split("\\(")[0].trim(), // Extract skill name
            result.naturalRoll(),
            result.totalRoll() - result.naturalRoll(),
            extractDC(result.rollDescription()),
            result.success()
        );

        // Natural 20 or 1
        if (result.wasNatural20()) {
            Display.println(Display.colorize("*** NATURAL 20! ***", YELLOW));
        } else if (result.wasNatural1()) {
            Display.println(Display.colorize("*** NATURAL 1! ***", RED));
        }

        Display.println();

        // Show result message
        if (result.success()) {
            Display.println(Display.colorize("SUCCESS!", GREEN));
            Display.println();
            Display.showNarrative(result.message());
        } else {
            Display.println(Display.colorize("FAILURE!", RED));
            Display.println();
            Display.showNarrative(result.message());
        }

        Display.printDivider('-', 60, WHITE);
        Display.println();
    }

    private int extractDC(String rollDescription) {
        // Extract DC from "d20(X) + Skill(+Y) = Z vs DC N"
        try {
            String[] parts = rollDescription.split("DC ");
            if (parts.length < 2) {
                return 10; // Default DC if format doesn't match
            }
            return Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 10; // Default DC on parse failure
        }
    }

    private void grantMiniGameReward(MiniGame game) {
        String rewardId = game.getReward();
        if (rewardId == null || rewardId.isEmpty()) {
            return;
        }

        // Try to find the item in the campaign
        var item = campaign.getItem(rewardId);
        if (item != null) {
            gameState.getCharacter().getInventory().addItem(item);
            Display.showItemGained(item.getName(), item.getDescription());
        } else {
            // Generic reward text
            Display.println(Display.colorize("Reward: " + rewardId, YELLOW));
        }
    }

    private void applyMiniGameConsequence(MiniGame game, MiniGame.EvaluationResult result) {
        String consequence = result.consequence();
        Display.println(Display.colorize("Consequence: " + consequence, RED));

        // Apply damage if specified
        String failDamage = game.getFailConsequence();
        if (failDamage != null && failDamage.contains("damage")) {
            // Simple damage parsing - extract number
            int damage = 1; // Default
            if (failDamage.contains("1d4")) {
                damage = Dice.parse("1d4");
            } else if (failDamage.contains("1d6")) {
                damage = Dice.parse("1d6");
            } else {
                // Try to extract single number from the consequence text
                damage = extractDamageNumber(failDamage);
            }

            gameState.getCharacter().takeDamage(damage);
            Display.println(Display.colorize("You take " + damage + " damage!", RED));
            Display.printHealthBar(
                gameState.getCharacter().getCurrentHitPoints(),
                gameState.getCharacter().getMaxHitPoints()
            );
        }
    }

    private int extractDamageNumber(String text) {
        // Try to extract a single number from the text
        String[] parts = text.split(" ");
        for (String part : parts) {
            try {
                return Integer.parseInt(part);
            } catch (NumberFormatException ignored) {
                // Not a number, continue
            }
        }
        return 1; // Default damage
    }

    private void completeTrial(Trial trial) {
        Display.println();
        Display.printDivider('=', 60, GREEN);
        Display.println(Display.colorize("  TRIAL COMPLETE!", GREEN));
        Display.printDivider('=', 60, GREEN);
        Display.println();

        Trial.CompletionResult result = trial.complete();

        // Grant reward
        if (result.hasReward()) {
            String rewardId = trial.getCompletionReward();
            var item = campaign.getItem(rewardId);
            if (item != null) {
                gameState.getCharacter().getInventory().addItem(item);
                Display.showItemGained(item.getName(), item.getDescription());
            } else {
                Display.println(Display.colorize("You received: " + result.reward(), YELLOW));
            }
        }

        // Show stinger (villain message)
        if (result.hasStinger()) {
            Display.println();
            Display.showVillainMessage(result.stinger());
        }

        // Set completion flags
        setTrialCompletionFlags(trial);

        Display.println("Press Enter to continue...");
        scanner.nextLine();

        activeTrial = null;
    }

    private void setTrialCompletionFlags(Trial trial) {
        String trialId = trial.getId();

        // Set standard completion flag
        gameState.setFlag(trialId + "_complete", true);

        // Set specific flags and unlock locations based on trial
        if (trialId.equals("trial_01")) {
            gameState.setFlag("trial_01_complete", true);
            gameState.setFlag("met_machinist", true);
            gameState.setFlag("clocktower_unlocked", true);

            // Unlock the clocktower area
            unlockLocation("clocktower_hill");
            Display.println();
            Display.println(Display.colorize("The path to Clocktower Hill is now accessible!", CYAN));
        } else if (trialId.equals("trial_02")) {
            gameState.setFlag("trial_02_complete", true);
            gameState.setFlag("workshop_discovered", true);
            gameState.setFlag("harlequin_lair_unlocked", true);

            // Unlock the Harlequin's Lair
            unlockLocation("harlequin_lair");
            Display.println();
            Display.println(Display.colorize("A hidden passage to the Harlequin's Stage has been revealed!", CYAN));
        } else if (trialId.equals("trial_03")) {
            gameState.setFlag("trial_03_complete", true);
            gameState.setFlag("mayor_rescued", true);
            gameState.setFlag("machinist_revealed", true);
            gameState.setFlag("campaign_complete", true);

            // Campaign complete!
            Display.println();
            Display.printBox("CAMPAIGN COMPLETE!", 60, YELLOW);
            Display.println();
            Display.println(Display.colorize("Congratulations! You've completed the Muddlebrook campaign!", GREEN));
            Display.println();
        }
    }

    /**
     * Unlocks a location by ID, making it accessible to the player.
     */
    private void unlockLocation(String locationId) {
        Location location = campaign.getLocation(locationId);
        if (location != null) {
            location.unlock();
        }
    }

    // ==========================================
    // Display Helpers
    // ==========================================

    private void displayCurrentLocation() {
        Location location = gameState.getCurrentLocation();
        if (location == null) {
            Display.showError("You are nowhere... this shouldn't happen!");
            return;
        }

        Display.println();
        Display.printBox(location.getName(), 60, CYAN);
        Display.println();

        // Show description (read-aloud for first visit, regular description otherwise)
        // Get the description BEFORE marking as visited so read-aloud text shows
        String description = location.getDisplayDescription();

        // Now mark as visited so subsequent looks show the short description
        location.markVisited();

        Display.printWrapped(description, 60);
        Display.println();

        // Show NPCs
        List<String> npcIds = location.getNpcs();
        if (!npcIds.isEmpty()) {
            Display.println(Display.colorize("You see:", WHITE));
            for (String npcId : npcIds) {
                NPC npc = campaign.getNPC(npcId);
                if (npc != null) {
                    Display.println("  - " + Display.colorize(npc.getName(), CYAN));
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

        // Show exits
        Display.println(Display.colorize(location.getExitsDisplay(), WHITE));
        Display.println();
    }

    private void displayDialogueResult(DialogueResult result) {
        Display.println();

        switch (result.getType()) {
            case SUCCESS -> {
                NPC npc = result.getNpc();
                Display.showDialogue(npc.getName(), result.getMessage());

                List<String> topics = result.getAvailableTopics();
                if (topics != null && !topics.isEmpty()) {
                    Display.println(Display.colorize("Topics: ", WHITE) +
                        String.join(", ", topics));
                    Display.println(Display.colorize("(Use 'ask about <topic>' or 'bye' to end conversation)", WHITE));
                }
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

    private boolean matchesTarget(String actual, String search) {
        if (actual == null || search == null) {
            return false;
        }
        String actualLower = actual.toLowerCase().replace("_", " ");
        String searchLower = search.toLowerCase();
        return actualLower.equals(searchLower) ||
               actualLower.contains(searchLower) ||
               searchLower.contains(actualLower);
    }
}
