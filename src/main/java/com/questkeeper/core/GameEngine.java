package com.questkeeper.core;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.campaign.CampaignInfo;
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
import com.questkeeper.core.RestSystem.RestResult;
import com.questkeeper.dialogue.DialogueResult;
import com.questkeeper.dialogue.DialogueSystem;
import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Inventory.ItemStack;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.inventory.items.MagicItem;
import com.questkeeper.save.SaveState;
import com.questkeeper.state.GameState;
import com.questkeeper.ui.CharacterCreator;
import com.questkeeper.ui.Display;
import com.questkeeper.world.Location;

import java.io.IOException;
import java.nio.file.Files;
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
    private Trial activeTrial;
    private boolean running;

    public GameEngine() {
        this.scanner = new Scanner(System.in);
        this.random = new Random();
        this.dialogueSystem = new DialogueSystem();
        this.combatSystem = new CombatSystem();
        this.restSystem = new RestSystem();
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

        switch (verb) {
            case "look" -> handleLook(noun);
            case "go", "north", "south", "east", "west", "n", "s", "e", "w",
                 "northeast", "northwest", "southeast", "southwest", "ne", "nw", "se", "sw",
                 "up", "down", "u", "d" -> {
                // If verb is a direction, use it; otherwise use noun
                String direction = isDirection(verb) ? verb : noun;
                handleGo(direction);
            }
            case "leave", "exit" -> handleLeave();
            case "talk" -> handleTalk(noun);
            case "ask" -> handleAsk(input);
            case "buy" -> handleBuy(noun);
            case "bye" -> handleBye();
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
            case "use", "activate" -> handleUse(noun);
            case "rest" -> handleRest(noun);
            case "help" -> handleHelp();
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

        // Expand shorthand directions
        direction = expandDirection(direction);

        Location currentLocation = gameState.getCurrentLocation();
        if (!currentLocation.hasExit(direction)) {
            Display.showError("You can't go " + direction + " from here.");
            Display.println(buildExitsDisplay(currentLocation));
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
            if (target != null && !gameState.isLocationUnlocked(target.getId())) {
                // Provide context-specific locked messages
                String lockedMessage = getLockedLocationMessage(target.getId());
                Display.showError(lockedMessage);
            } else {
                Display.showError("You can't go that way.");
            }
        }
    }

    private String getLockedLocationMessage(String locationId) {
        // Data-driven: get locked message from the Location itself
        Location location = campaign.getLocation(locationId);
        if (location != null) {
            return location.getLockedMessage();
        }
        return "That way is currently blocked or locked.";
    }

    private void handleLeave() {
        Location currentLocation = gameState.getCurrentLocation();
        Set<String> exits = currentLocation.getExits();

        if (exits.isEmpty()) {
            Display.showError("There's no way out of here!");
            return;
        }

        // Priority order for exit directions
        String[] exitPriority = {"out", "outside", "exit", "door", "south", "north", "east", "west"};

        String chosenExit = null;
        for (String preferred : exitPriority) {
            if (exits.contains(preferred)) {
                chosenExit = preferred;
                break;
            }
        }

        // If no preferred exit found, use the first available
        if (chosenExit == null) {
            chosenExit = exits.iterator().next();
        }

        // Use handleGo to do the actual movement
        handleGo(chosenExit);
    }

    private String expandDirection(String dir) {
        if (dir == null) return dir;
        return switch (dir.toLowerCase()) {
            case "n" -> "north";
            case "s" -> "south";
            case "e" -> "east";
            case "w" -> "west";
            case "ne" -> "northeast";
            case "nw" -> "northwest";
            case "se" -> "southeast";
            case "sw" -> "southwest";
            case "u" -> "up";
            case "d" -> "down";
            default -> dir;
        };
    }

    private boolean isDirection(String word) {
        if (word == null) return false;
        return switch (word.toLowerCase()) {
            case "north", "south", "east", "west", "n", "s", "e", "w",
                 "northeast", "northwest", "southeast", "southwest", "ne", "nw", "se", "sw",
                 "up", "down", "u", "d" -> true;
            default -> false;
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

    private void handleBye() {
        if (!dialogueSystem.isInConversation()) {
            Display.println("You wave goodbye to no one in particular.");
            return;
        }

        NPC npc = dialogueSystem.getCurrentNpc();
        dialogueSystem.endDialogue();

        Display.println();
        Display.println(Display.colorize("You end your conversation with " + npc.getName() + ".", CYAN));
        Display.println();
    }

    private void handleBuy(String itemName) {
        // Must be in conversation with a shopkeeper
        if (!dialogueSystem.isInConversation()) {
            Display.showError("You need to talk to a shopkeeper first.");
            return;
        }

        NPC npc = dialogueSystem.getCurrentNpc();
        if (!npc.isShopkeeper()) {
            Display.showError(npc.getName() + " doesn't have anything for sale.");
            return;
        }

        // Show shop menu if no item specified
        if (itemName == null || itemName.trim().isEmpty()) {
            showShopMenu(npc);
            return;
        }

        // Try to find the item
        String searchTerm = itemName.toLowerCase().trim();
        Map<String, Integer> shopItems = npc.getShopItems();

        String foundItem = null;
        int price = 0;

        // Try exact match first, then partial match
        for (Map.Entry<String, Integer> entry : shopItems.entrySet()) {
            if (entry.getKey().equals(searchTerm)) {
                foundItem = entry.getKey();
                price = entry.getValue();
                break;
            }
        }

        if (foundItem == null) {
            for (Map.Entry<String, Integer> entry : shopItems.entrySet()) {
                if (entry.getKey().contains(searchTerm) || searchTerm.contains(entry.getKey())) {
                    foundItem = entry.getKey();
                    price = entry.getValue();
                    break;
                }
            }
        }

        if (foundItem == null) {
            Display.showError(npc.getName() + " doesn't sell '" + itemName + "'.");
            showShopMenu(npc);
            return;
        }

        // Check if player has enough gold
        Character character = gameState.getCharacter();
        Inventory inventory = character.getInventory();

        if (!inventory.hasGold(price)) {
            Display.println();
            Display.println(Display.colorize(npc.getName() + " shakes their head.", CYAN));
            Display.println("\"That'll be " + formatPrice(price) + ", and you only have " +
                formatPrice(inventory.getGold()) + ".\"");
            Display.println();
            return;
        }

        // Make the purchase
        inventory.removeGold(price);

        // Add item to inventory (as a consumable for drinks/food)
        Item purchasedItem = campaign.getItem(foundItem);
        if (purchasedItem != null) {
            inventory.addItem(purchasedItem);
        }

        // Display purchase
        Display.println();
        Display.println(Display.colorize(npc.getName() + " takes your coin and hands you the " + foundItem + ".", CYAN));
        Display.println(Display.colorize("(-" + formatPrice(price) + ")", YELLOW));
        Display.println();
    }

    private void showShopMenu(NPC npc) {
        Map<String, Integer> shopItems = npc.getShopItems();

        Display.println();
        Display.println(Display.colorize(npc.getName() + "'s Wares:", YELLOW));
        Display.println();

        for (Map.Entry<String, Integer> entry : shopItems.entrySet()) {
            String itemName = entry.getKey();
            int price = entry.getValue();
            // Capitalize first letter of each word
            String displayName = capitalizeWords(itemName);
            Display.println("  " + Display.colorize(displayName, WHITE) +
                " - " + Display.colorize(formatPrice(price), YELLOW));
        }

        Display.println();
        Display.println(Display.colorize("Your gold: " + formatPrice(gameState.getCharacter().getInventory().getGold()), CYAN));
        Display.println(Display.colorize("Type 'buy <item>' to purchase.", DEFAULT));
        Display.println();
    }

    private String formatPrice(int copper) {
        if (copper >= 100) {
            int gold = copper / 100;
            int remaining = copper % 100;
            if (remaining > 0) {
                return gold + " gp " + remaining + " cp";
            }
            return gold + " gp";
        } else if (copper >= 10) {
            int silver = copper / 10;
            int remaining = copper % 10;
            if (remaining > 0) {
                return silver + " sp " + remaining + " cp";
            }
            return silver + " sp";
        }
        return copper + " cp";
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(java.lang.Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        return result.toString();
    }

    private void handleInventory() {
        Character character = gameState.getCharacter();
        Inventory inventory = character.getInventory();

        Display.println();
        Display.printBox("INVENTORY", 50, YELLOW);
        Display.println();

        // Show key equipped items at a glance
        Map<EquipmentSlot, Item> equipped = inventory.getEquippedItems();
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

        List<ItemStack> items = inventory.getAllItems();
        if (items.isEmpty()) {
            Display.println("Your pack is empty.");
        } else {
            Display.println(Display.colorize("Backpack:", WHITE));
            for (ItemStack stack : items) {
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
            Display.showError("Equip what? Try 'equip <item name>' or 'equip <item> to offhand'");
            return;
        }

        // Parse "equip X to Y" or "equip X Y" syntax for explicit slot targeting
        String itemName = target;
        EquipmentSlot targetSlot = null;

        if (target.toLowerCase().contains(" to ")) {
            // "equip shortsword to offhand" syntax
            String[] parts = target.split("(?i) to ");
            itemName = parts[0].trim();
            if (parts.length > 1) {
                targetSlot = parseSlotName(parts[1].trim().toLowerCase());
            }
        } else {
            // Try "equip shortsword offhand" syntax (no "to")
            String[] words = target.split("\\s+");
            if (words.length >= 2) {
                String lastWord = words[words.length - 1].toLowerCase();
                EquipmentSlot possibleSlot = parseSlotName(lastWord);
                if (possibleSlot != null) {
                    targetSlot = possibleSlot;
                    // Remove the slot name from the item name
                    itemName = target.substring(0, target.toLowerCase().lastIndexOf(lastWord)).trim();
                }
            }
        }

        var inventory = gameState.getCharacter().getInventory();
        List<Item> matches = inventory.findItemsByName(itemName);

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

        // Equip the item (use explicit slot if provided)
        Item previousItem;
        String slotName;
        EquipmentSlot actualSlot;

        if (targetSlot != null) {
            previousItem = inventory.equipToSlot(toEquip, targetSlot);
            slotName = targetSlot.getDisplayName();
            actualSlot = targetSlot;
        } else {
            previousItem = inventory.equip(toEquip);
            slotName = getSlotNameForItem(toEquip);
            actualSlot = getSlotForItem(toEquip);
        }

        // Verify the equip actually succeeded by checking if the item is in the slot
        if (actualSlot == null || inventory.getEquipped(actualSlot) != toEquip) {
            Display.showError("Failed to equip " + toEquip.getName() + " to " + slotName + ".");
            return;
        }

        Display.println();
        Display.println(Display.colorize("Equipped: ", GREEN) + toEquip.getName() +
                Display.colorize(" (" + slotName + ")", WHITE));

        if (previousItem != null) {
            Display.println(Display.colorize("Unequipped: ", YELLOW) + previousItem.getName() +
                    " (returned to inventory)");
        }
        Display.println();
    }

    private EquipmentSlot getSlotForItem(Item item) {
        if (item instanceof Weapon) {
            return EquipmentSlot.MAIN_HAND;
        } else if (item instanceof Armor armor) {
            return armor.isShield() ? EquipmentSlot.OFF_HAND : EquipmentSlot.ARMOR;
        }
        return null;
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

    private void handleUse(String target) {
        if (target == null || target.isEmpty()) {
            Display.showError("Use what? Try 'use <item name>'.");
            return;
        }

        var inventory = gameState.getCharacter().getInventory();
        var items = inventory.findItemsByName(target);

        if (items.isEmpty()) {
            // Also check equipped items
            var equipped = inventory.getEquippedItems();
            for (Item item : equipped.values()) {
                if (item.getName().toLowerCase().contains(target.toLowerCase())) {
                    items.add(item);
                    break;
                }
            }
        }

        if (items.isEmpty()) {
            Display.showError("You don't have anything called '" + target + "'.");
            return;
        }

        Item item = items.get(0);

        // Check if it's a MagicItem with usable effects
        if (item instanceof MagicItem magicItem) {
            Character player = gameState.getCharacter();

            if (!magicItem.canUse(player)) {
                Display.showError(magicItem.getCannotUseReason(player));
                return;
            }

            var usableEffects = magicItem.getUsableEffects();
            if (usableEffects.isEmpty()) {
                Display.println();
                Display.println(Display.colorize(magicItem.getName(), CYAN));
                Display.println(magicItem.getDescription());
                Display.println();
                Display.println(Display.colorize("This item has no activatable effects right now.", YELLOW));
                Display.println();
                return;
            }

            Display.println();
            String result = magicItem.use(player);
            Display.println(Display.colorize("You use the " + magicItem.getName() + "!", GREEN));
            Display.println();
            Display.println(result);
            Display.println();

            // Check if consumable and fully used
            if (magicItem.isFullyConsumed()) {
                inventory.removeItem(magicItem);
                Display.println(Display.colorize("The " + magicItem.getName() + " is consumed.", YELLOW));
                Display.println();
            }
            return;
        }

        // For regular items, show the description (flavor text for non-magic items)
        Display.println();
        Display.println(Display.colorize(item.getName(), CYAN));
        if (!item.getDescription().isEmpty()) {
            Display.println(item.getDescription());
        } else {
            Display.println("You examine the " + item.getName() + ".");
        }
        Display.println();

        // Check if item type suggests it might be usable
        if (item.getType() == Item.ItemType.CONSUMABLE) {
            Display.println(Display.colorize("This looks like it could be consumed, but you're not sure how.", YELLOW));
            Display.println();
        }
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

    private void handleHelp() {
        // If in conversation, show dialogue-specific help with topics
        if (dialogueSystem.isInConversation()) {
            Display.println();
            NPC currentNpc = dialogueSystem.getCurrentNpc();
            List<String> topics = dialogueSystem.getAvailableTopics();

            Display.println(Display.colorize("Talking to " + currentNpc.getName(), CYAN));
            if (topics != null && !topics.isEmpty()) {
                Display.println(Display.colorize("Topics: ", WHITE) + String.join(", ", topics));
            }

            // Show buy command if NPC is a shopkeeper
            if (currentNpc.isShopkeeper()) {
                Display.println(Display.colorize("Commands: ", WHITE) + "ask about <topic>, buy <item>, bye");
            } else {
                Display.println(Display.colorize("Commands: ", WHITE) + "ask about <topic>, bye");
            }
            Display.println();
        } else {
            Display.showHelp();
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

    private void handleTrial() {
        String locationId = gameState.getCurrentLocation().getId();
        Trial trial = campaign.getTrialAtLocation(locationId);

        if (trial == null) {
            Display.showError("There's no trial at this location.");
            listAvailableTrials();
            return;
        }

        if (gameState.hasCompletedTrial(trial.getId())) {
            Display.println("You've already completed this trial.");
            return;
        }

        if (!checkTrialPrerequisites(trial)) {
            Display.showError("You haven't completed the prerequisites for this trial.");
            return;
        }

        // Start or continue the trial
        if (!gameState.hasStartedTrial(trial.getId())) {
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
                String status = gameState.hasCompletedTrial(t.getId()) ? "[COMPLETE]" :
                               gameState.hasStartedTrial(t.getId()) ? "[IN PROGRESS]" : "[NOT STARTED]";
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

        // Mark the trial as started in GameState
        gameState.startTrial(trial.getId());

        // Show quest started notification
        Display.showQuestStarted(trial.getName());

        // Show entry narrative
        String narrative = trial.getEntryNarrative();
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

        if (trial == null || !gameState.hasStartedTrial(trial.getId())) {
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

            // Handle rewards and save progress
            if (result.success()) {
                // Save mini-game completion to flags for persistence
                gameState.setFlag("completed_minigame_" + game.getId());

                if (result.hasReward()) {
                    grantMiniGameReward(game);
                }
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

        // Mark trial as completed in GameState
        gameState.completeTrial(trial.getId());

        // Show clue gained notification
        Display.showClueGained("You've learned more about the mystery!");

        // Grant reward
        String rewardId = trial.getCompletionReward();
        if (rewardId != null && !rewardId.isEmpty()) {
            var item = campaign.getItem(rewardId);
            if (item != null) {
                gameState.getCharacter().getInventory().addItem(item);
                Display.showItemGained(item.getName(), item.getDescription());
            } else {
                Display.println(Display.colorize("You received: " + rewardId, YELLOW));
            }
        }

        // Show stinger (villain message)
        String stinger = trial.getStinger();
        if (stinger != null && !stinger.isEmpty()) {
            Display.println();
            Display.showVillainMessage(stinger);
        }

        // Set completion flags
        setTrialCompletionFlags(trial);

        Display.println("Press Enter to continue...");
        scanner.nextLine();

        activeTrial = null;
    }

    private void setTrialCompletionFlags(Trial trial) {
        // Data-driven flag setting using Trial's completionFlags list
        for (String flag : trial.getCompletionFlags()) {
            gameState.setFlag(flag, true);

            // Check if this flag indicates a location unlock
            if (flag.endsWith("_unlocked")) {
                String locationId = flag.replace("_unlocked", "");
                Location location = campaign.getLocation(locationId);

                if (location != null) {
                    unlockLocation(locationId);
                    Display.println();
                    Display.println(Display.colorize(
                        "New area unlocked: " + location.getName() + "!", CYAN));
                }
            }

            // Check for campaign completion
            if (flag.equals("campaign_complete")) {
                Display.println();
                Display.printBox("CAMPAIGN COMPLETE!", 60, YELLOW);
                Display.println();
                Display.println(Display.colorize(
                    "Congratulations! You've completed the " + campaign.getName() + "!", GREEN));
                Display.println();
            }
        }
    }

    /**
     * Unlocks a location by ID, making it accessible to the player.
     */
    private void unlockLocation(String locationId) {
        if (locationId != null && !locationId.isEmpty()) {
            gameState.unlockLocation(locationId);
        }
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
