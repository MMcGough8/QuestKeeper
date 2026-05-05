package com.questkeeper.ui;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Display utility class for CLI presentation.
 * 
 * Handles all text output formatting for the QuestKeeper game,
 * including locations, characters, combat, dialogue, rolls, and system messages.
 * Uses Jansi for ANSI color support in terminals.
 * 
 * @author Marc McGough
 * @version 1.0
 */

@SuppressWarnings("java:S106") // System.out is intentional for CLI output
public class Display {

    /** Default width for boxes and dividers */
    private static final int DEFAULT_WIDTH = 60;
    
    /** Characters for box drawing */
    private static final char BOX_HORIZONTAL = '═';
    private static final char BOX_VERTICAL = '║';
    private static final char BOX_TOP_LEFT = '╔';
    private static final char BOX_TOP_RIGHT = '╗';
    private static final char BOX_BOTTOM_LEFT = '╚';
    private static final char BOX_BOTTOM_RIGHT = '╝';
    
    /** Characters for simple dividers */
    private static final char DIVIDER_CHAR = '─';
    private static final char DIVIDER_ACCENT = '═';

    /** Character for blockquote left border */
    private static final char BLOCKQUOTE_BORDER = '│';

    /** Message prefix constants */
    private static final String PREFIX_GAME = "[GAME]";
    private static final String PREFIX_NARRATION = "[NARRATION]";
    private static final String PREFIX_ROLL = "[ROLL]";

     /** Whether Jansi has been installed */
    private static boolean jansiInstalled = false;
    
    /** Whether colors are enabled (can be disabled for testing or plain terminals) */
    private static boolean colorsEnabled = true;

    private Display() {
    }

    /**
     * Should be called once at application startup.
     */
    public static void init() {
        if (!jansiInstalled) {
            AnsiConsole.systemInstall();
            jansiInstalled = true;
        }
    }

    /**
     * Should be called at application shutdown.
     */
    public static void shutdown() {
        if (jansiInstalled) {
            AnsiConsole.systemUninstall();
            jansiInstalled = false;
        }
    }

     /**
     * Enables or disables color output.
     * 
     * @param enabled true to enable colors, false for plain text
     */
    public static void setColorsEnabled(boolean enabled) {
        colorsEnabled = enabled;
    }
    
    /**
     * Checks if colors are currently enabled.
     * 
     * @return true if colors are enabled
     */
    public static boolean areColorsEnabled() {
        return colorsEnabled;
    }

    public static void showLocation(String name, String description, String[] exits) {
        println();
        printBox(name, DEFAULT_WIDTH, CYAN);
        println();
        printWrapped(description, DEFAULT_WIDTH);
        println();

        if (exits != null && exits.length > 0) {
            print(colorize("Exits: ", YELLOW));
            println(String.join(", ", exits));
        }
        println();
    }

    public static void showCharacter(String name, String race, String charClass, int level, int hp, int maxHp, int ac) {
        println();
        printBox(name + " - " + race + " " + charClass, DEFAULT_WIDTH, GREEN);
        println();

        printHealthBar(hp, maxHp);

        print(colorize("Level: ", WHITE));
        print(colorize(String.valueOf(level), YELLOW));
        print("  ");
        print(colorize("AC: ", WHITE));
        println(colorize(String.valueOf(ac), YELLOW));
        println();
    }

    public static void printHealthBar(int current, int max) {
        int barWidth = 20;
        int filled = (int) ((double) current / max * barWidth);
        filled = Math.max(0, Math.min(barWidth, filled));

        print(colorize("HP: ", WHITE));
        print(colorize("[", WHITE));

        double percentage = (double) current / max;

        Ansi.Color barColor;
        if (percentage > 0.5) {
            barColor = CYAN;  // More visible than green on dark backgrounds
        } else if (percentage > 0.25) {
            barColor = YELLOW;
        } else {
            barColor = RED;
        }

        // Use = for filled and - for empty (more visible than block characters)
        print(colorize("=".repeat(filled), barColor));
        print(colorize("-".repeat(barWidth - filled), DEFAULT));
        print(colorize("] ", WHITE));

        print(colorize(current + "/" + max, barColor));
        println();
    }

    public static void showCombat(String enemyName, int enemyHp, int enemyMaxHp, int playerHp, int playerMaxHp, int round) {
        println();
        printDivider(DIVIDER_ACCENT, DEFAULT_WIDTH);
        println(colorize(" COMBAT - Round " + round, RED));
        printDivider(DIVIDER_ACCENT, DEFAULT_WIDTH);
        println();

        print(colorize(enemyName + ": ", RED));
        printHealthBar(enemyHp, enemyMaxHp);
        
        print(colorize("You: ", GREEN));
        printHealthBar(playerHp, playerMaxHp);
        
        println();
        printDivider(DIVIDER_CHAR, DEFAULT_WIDTH);
        println();
    }

    public static void showDialogue(String speaker, String text) {
        println();
        print(colorize(speaker, CYAN));
        println(colorize(":", WHITE));
        showBlockquote("\"" + text + "\"");
        println();
    }

    public static void showDialogue(String speaker, String text, String voiceTag) {
        println();
        print(colorize(speaker, CYAN));
        print(colorize(" (" + voiceTag + ")", DEFAULT));
        println(colorize(":", WHITE));
        showBlockquote("\"" + text + "\"");
        println();
    }

    public static void showRoll(String formula, int result, Boolean success) {
        print(colorize("[ROLL] ", CYAN));
        print(colorize(formula + ": ", WHITE));
        print(colorize(String.valueOf(result), YELLOW));
        
        if (success != null) {
            if (success) {
                println(colorize(" - SUCCESS!", GREEN));
            } else {
                println(colorize(" - FAILURE", RED));
            }
        } else {
            println();
        }
    }

    public static void showSkillCheck (String skillName, int roll, int modifier, int dc, boolean success) {
        int total = roll + modifier;
        String modStr = modifier >= 0 ? "+" + modifier : String.valueOf(modifier);

        print(colorize(PREFIX_ROLL + " ", CYAN));
        print(colorize(skillName + " Check: ", WHITE));
        print(colorize("d20: ", WHITE));
        print(colorize(String.valueOf(roll), YELLOW));
        print(colorize(" " + modStr + " = ", WHITE));
        print(colorize(String.valueOf(total), YELLOW));
        print(" ");

        if (success) {
            println(colorize("SUCCESS!", GREEN));
        } else {
            println(colorize("FAILURE", RED));
        }

        if (roll == 20) {
            println(colorize("  ★ NATURAL 20! ★", YELLOW));
        } else if (roll == 1) {
            println(colorize("  ✗ Natural 1...", RED));
        }
    }

    public static void showDamage (String notation, int total, String damageType) {
        print(colorize("[DAMAGE] ", RED));
        print(colorize(notation + " = ", WHITE));
        print(colorize(String.valueOf(total), YELLOW));
        if (damageType != null && !damageType.isEmpty()) {
            print(colorize(" " + damageType, MAGENTA));
        }
        println();
    }

    public static void showError(String message) {
        println();
        print(colorize("[ERROR] ", RED));
        println(colorize(message, WHITE));
        println();
    }

    public static void showWarning(String message) {
        print(colorize("[WARNING] ", YELLOW));
        println(colorize(message, WHITE));
    }

    public static void showInfo(String message) {
        print(colorize("[INFO] ", CYAN));
        println(colorize(message, WHITE));
    }

    public static void showSuccess(String message) {
        println(colorize("+ " + message, GREEN));
    }

    public static void showHelp() {
        println();
        printBox("AVAILABLE COMMANDS", DEFAULT_WIDTH, YELLOW);
        println();

        println(colorize("Exploration", YELLOW));
        showHelpCommand("look / examine", "Examine your surroundings or an object");
        showHelpCommand("go <direction>", "Move (n/s/e/w/ne/nw/se/sw/up/down)");
        showHelpCommand("rest", "Short or long rest (recover HP / hit dice)");
        println();

        println(colorize("Items & Equipment", YELLOW));
        showHelpCommand("take <item>", "Pick up an item from the room");
        showHelpCommand("drop <item>", "Drop an item from your inventory");
        showHelpCommand("use <item>", "Use a consumable or magic item");
        showHelpCommand("equip <item>", "Equip a weapon, armor, or accessory");
        showHelpCommand("unequip <slot>", "Remove equipped gear");
        showHelpCommand("inventory / i", "View your inventory");
        showHelpCommand("equipment", "View what you have equipped");
        showHelpCommand("stats", "View character sheet + class features");
        println();

        println(colorize("Dialogue", YELLOW));
        showHelpCommand("talk <npc>", "Begin a conversation");
        showHelpCommand("ask about <topic>", "Ask the NPC about a topic");
        showHelpCommand("buy <item>", "Buy from a shopkeeper");
        showHelpCommand("bye", "End the conversation");
        println();

        println(colorize("Combat", YELLOW));
        showHelpCommand("attack <target>", "Make a weapon attack");
        showHelpCommand("cast <spell>", "Cast a spell (uses a slot)");
        showHelpCommand("flee", "Attempt to escape combat (DEX check)");
        println();

        println(colorize("Class Actions (use what your class has)", YELLOW));
        showHelpCommand("smite", "Paladin: prime Divine Smite for next hit");
        showHelpCommand("sacredweapon", "Paladin: Channel Divinity weapon buff");
        showHelpCommand("turn", "Paladin/Cleric: Channel Divinity vs undead/fiends");
        showHelpCommand("layonhands", "Paladin: heal from your divine pool");
        showHelpCommand("rage", "Barbarian: enter rage (bonus damage / resistance)");
        showHelpCommand("reckless", "Barbarian: advantage on attacks (and on you)");
        showHelpCommand("frenzy", "Barbarian Berserker: bonus attack while raging");
        showHelpCommand("flurry", "Monk: spend 1 ki for two bonus attacks");
        showHelpCommand("patient / step", "Monk: defensive / mobility ki actions");
        showHelpCommand("stun", "Monk L5: prime Stunning Strike on next hit");
        showHelpCommand("dash / disengage", "Rogue: bonus-action movement");
        showHelpCommand("hide", "Rogue: bonus-action stealth");
        showHelpCommand("actionsurge", "Fighter: take an extra action this turn");
        showHelpCommand("secondwind", "Fighter: bonus action self-heal");
        println();

        println(colorize("Trials", YELLOW));
        showHelpCommand("trial", "Begin a trial at this location");
        showHelpCommand("attempt <skill>", "Attempt a mini-game with a skill check");
        println();

        println(colorize("System", YELLOW));
        showHelpCommand("save", "Save your game (YAML; auto-saves on level-up + trial completion)");
        showHelpCommand("load", "Load a saved game");
        showHelpCommand("help", "Show this menu");
        showHelpCommand("quit / exit", "Exit the game");
        println();
    }

    private static void showHelpCommand(String command, String description) {
        print("  ");
        print(colorize(String.format("%-20s", command), CYAN));
        println(colorize(description, WHITE));
    }

    public static void showNarrative(String text) {
        println();
        showBlockquote(text);
        println();
    }

    public static void showPause() {
        println(colorize("...", DEFAULT));
    }

    public static void showSceneTransition(String sceneName) {
        println();
        printDivider(DIVIDER_ACCENT, DEFAULT_WIDTH);
        if (sceneName != null && !sceneName.isEmpty()) {
            int padding = (DEFAULT_WIDTH - sceneName.length() - 4) / 2;
            println(colorize(" ".repeat(Math.max(0, padding)) + "« " + sceneName + " »", MAGENTA));
        }
        printDivider(DIVIDER_ACCENT, DEFAULT_WIDTH);
        println();
    }

    public static void showChoices(String prompt, String[] choices) {
        println();
        println(colorize(prompt, WHITE));
        println();
        
        for (int i = 0; i < choices.length; i++) {
            print(colorize("  [" + (i + 1) + "] ", YELLOW));
            println(colorize(choices[i], WHITE));
        }
        println();
    }

    // ========================================================================
    // MUDDLEBROOK-SPECIFIC DISPLAYS
    // ========================================================================

    public static void showTrialHeader(String trialName, int trialNumber) {
        println();
        printBox("TRIAL #" + trialNumber + ": " + trialName, DEFAULT_WIDTH, MAGENTA);
        println();
    }

    public static void showMiniGameResult(boolean success, String reward, String consequence) {
        println();
        if (success) {
            println(colorize("═══ SUCCESS! ═══", GREEN));
            if (reward != null && !reward.isEmpty()) {
                println(colorize("✦ REWARD: " + reward, YELLOW));
            }
        } else {
            println(colorize("═══ FAILED ═══", RED));
            if (consequence != null && !consequence.isEmpty()) {
                println(colorize("✗ " + consequence, RED));
            }
        }
        println();
    }

    public static void showVillainMessage(String message) {
        println();
        printDivider('~', DEFAULT_WIDTH);
        println(colorize("  🎭 The Harlequin Machinist:", MAGENTA));
        println();
        print(colorize("  \"", RED));
        print(colorize(message, WHITE));
        println(colorize("\"", RED));
        println();
        printDivider('~', DEFAULT_WIDTH);
        println();
    }

    public static void showItemGained(String itemName, String itemDescription) {
        println();
        println(colorize("╔══════════════════════════════════════╗", YELLOW));
        println(colorize("║       ✦ NEW ITEM ACQUIRED ✦         ║", YELLOW));
        println(colorize("╚══════════════════════════════════════╝", YELLOW));
        println();
        println(colorize("  " + itemName, GREEN));
        if (itemDescription != null && !itemDescription.isEmpty()) {
            println(colorize("  " + itemDescription, WHITE));
        }
        println();
    }

    public static void printBox(String title, int width, Ansi.Color color) {
        // Top border
        print(colorize(String.valueOf(BOX_TOP_LEFT), color));
        print(colorize(String.valueOf(BOX_HORIZONTAL).repeat(width - 2), color));
        println(colorize(String.valueOf(BOX_TOP_RIGHT), color));
        
        // Title line (centered). Clamp at 0 so over-long titles don't throw.
        int padding = Math.max(0, (width - 2 - title.length()) / 2);
        int extraPadding = Math.max(0, (width - 2 - title.length()) % 2);

        print(colorize(String.valueOf(BOX_VERTICAL), color));
        print(" ".repeat(padding));
        print(colorize(title, color));
        print(" ".repeat(padding + extraPadding));
        println(colorize(String.valueOf(BOX_VERTICAL), color));
        
        // Bottom border
        print(colorize(String.valueOf(BOX_BOTTOM_LEFT), color));
        print(colorize(String.valueOf(BOX_HORIZONTAL).repeat(width - 2), color));
        println(colorize(String.valueOf(BOX_BOTTOM_RIGHT), color));
    }

    public static void printDivider(char character, int width) {
        println(String.valueOf(character).repeat(width));
    }

    public static void printDivider(char character, int width, Ansi.Color color) {
        println(colorize(String.valueOf(character).repeat(width), color));
    }

    public static void printWrapped(String text, int width) {
        if (text == null || text.isEmpty()) return;
    
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
    
        for (String word : words) {
            if (line.length() + word.length() + 1 > width) {
                println(line.toString());
                line = new StringBuilder();
            }
            if (!line.isEmpty()) {
                line.append(" ");
            }
            line.append(word);
        }
    
        if (!line.isEmpty()) {
            println(line.toString());
        }
    }

    public static String colorize(String text, Ansi.Color color) {
        if (!colorsEnabled || color == null) {
            return text;
        }
        return ansi().fg(color).a(text).reset().toString();
    }

    // ===== Semantic color helpers — use these for consistency =====
    /** Informational neutral text (e.g., counts, timestamps). */
    public static String info(String text)    { return colorize(text, CYAN); }
    /** Warnings + tips. Yellow conveys "pay attention but don't panic." */
    public static String warn(String text)    { return colorize(text, YELLOW); }
    /** Successful operations + positive outcomes (heals, hits landed). */
    public static String success(String text) { return colorize(text, GREEN); }
    /** Errors, damage taken, danger states. */
    public static String danger(String text)  { return colorize(text, RED); }
    /** Narrative voice / storyteller asides. */
    public static String narrative(String text) { return colorize(text, MAGENTA); }

    public static String bold(String text) {
        if (!colorsEnabled) {
            return text;
        }
        return ansi().bold().a(text).reset().toString();
    }

    public static String italic(String text) {
        if (!colorsEnabled) {
            return text;
        }
        return ansi().a(Ansi.Attribute.ITALIC).a(text).reset().toString();
    }

    public static void clearScreen() {
        System.out.flush();
        if (colorsEnabled) {
            System.out.print(ansi().eraseScreen().cursor(1, 1));
            System.out.flush();
        } else {
            for (int i = 0; i < 50; i++) {
                println();
            }
        }
    }

    public static void print(String text) {
        System.out.print(text);
    }

    public static void println(String text) {
        System.out.println(text);
    }

    public static void println() {
        System.out.println();
    }

    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static void showPrompt() {
        print(colorize("> ", GREEN));
    }

    public static void showPrompt(String prompt) {
        print(colorize(prompt + " ", GREEN));
    }

    // ========================================================================
    // ENHANCED UI METHODS
    // ========================================================================

    /**
     * Displays the ASCII art title screen.
     */
    public static void showTitleScreen() {
        println();
        println(colorize("    ╔═══════════════════════════════════════════════════════╗", YELLOW));
        println(colorize("    ║                                                       ║", YELLOW));
        println(colorize("    ║     ██████╗ ██╗   ██╗███████╗███████╗████████╗        ║", YELLOW));
        println(colorize("    ║    ██╔═══██╗██║   ██║██╔════╝██╔════╝╚══██╔══╝        ║", YELLOW));
        println(colorize("    ║    ██║   ██║██║   ██║█████╗  ███████╗   ██║           ║", YELLOW));
        println(colorize("    ║    ██║▄▄ ██║██║   ██║██╔══╝  ╚════██║   ██║           ║", YELLOW));
        println(colorize("    ║    ╚██████╔╝╚██████╔╝███████╗███████║   ██║           ║", YELLOW));
        println(colorize("    ║     ╚══▀▀═╝  ╚═════╝ ╚══════╝╚══════╝   ╚═╝           ║", YELLOW));
        println(colorize("    ║                                                       ║", YELLOW));
        println(colorize("    ║    ██╗  ██╗███████╗███████╗██████╗ ███████╗██████╗    ║", YELLOW));
        println(colorize("    ║    ██║ ██╔╝██╔════╝██╔════╝██╔══██╗██╔════╝██╔══██╗   ║", YELLOW));
        println(colorize("    ║    █████╔╝ █████╗  █████╗  ██████╔╝█████╗  ██████╔╝   ║", YELLOW));
        println(colorize("    ║    ██╔═██╗ ██╔══╝  ██╔══╝  ██╔═══╝ ██╔══╝  ██╔══██╗   ║", YELLOW));
        println(colorize("    ║    ██║  ██╗███████╗███████╗██║     ███████╗██║  ██║   ║", YELLOW));
        println(colorize("    ║    ╚═╝  ╚═╝╚══════╝╚══════╝╚═╝     ╚══════╝╚═╝  ╚═╝   ║", YELLOW));
        println(colorize("    ║                                                       ║", YELLOW));
        println(colorize("    ║           ⚔  A Terminal Fantasy Adventure  ⚔          ║", YELLOW));
        println(colorize("    ║                                                       ║", YELLOW));
        println(colorize("    ╚═══════════════════════════════════════════════════════╝", YELLOW));
        println();
    }

    /**
     * Displays the game header bar with session status.
     */
    public static void showHeader() {
        println();
        print(colorize("⚔ QUESTKEEPER ⚔", YELLOW));
        print("  ");
        println(colorize("● ONLINE SESSION: ACTIVE", GREEN));
        printDivider(DIVIDER_ACCENT, DEFAULT_WIDTH);
        println();
    }

    /**
     * Displays a compact status panel showing HP, Level, and Trials progress.
     */
    public static void showStatusPanel(int hp, int maxHp, int level, int completedTrials, int totalTrials) {
        int columnWidth = 18;

        // Top border - using ASCII for terminal compatibility
        print(colorize("+", CYAN));
        print(colorize("-".repeat(columnWidth), CYAN));
        print(colorize("+", CYAN));
        print(colorize("-".repeat(columnWidth), CYAN));
        print(colorize("+", CYAN));
        print(colorize("-".repeat(columnWidth), CYAN));
        println(colorize("+", CYAN));

        // Content row
        print(colorize("|", CYAN));
        String hpText = String.format(" HP: %d/%d", hp, maxHp);
        print(colorize(padRight(hpText, columnWidth), getHpColor(hp, maxHp)));
        print(colorize("|", CYAN));
        String levelText = String.format(" LEVEL: %d", level);
        print(colorize(padRight(levelText, columnWidth), YELLOW));
        print(colorize("|", CYAN));
        String trialsText = String.format(" TRIALS: %d/%d", completedTrials, totalTrials);
        print(colorize(padRight(trialsText, columnWidth), MAGENTA));
        println(colorize("|", CYAN));

        // Bottom border
        print(colorize("+", CYAN));
        print(colorize("-".repeat(columnWidth), CYAN));
        print(colorize("+", CYAN));
        print(colorize("-".repeat(columnWidth), CYAN));
        print(colorize("+", CYAN));
        print(colorize("-".repeat(columnWidth), CYAN));
        println(colorize("+", CYAN));
    }

    private static Ansi.Color getHpColor(int hp, int maxHp) {
        double percentage = (double) hp / maxHp;
        if (percentage > 0.5) return GREEN;
        if (percentage > 0.25) return YELLOW;
        return RED;
    }

    private static String padRight(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        return text + " ".repeat(width - text.length());
    }

    /**
     * Displays a system message with [GAME] prefix.
     */
    public static void showGameMessage(String message) {
        print(colorize(PREFIX_GAME + " ", CYAN));
        println(colorize(message, WHITE));
    }

    /**
     * Displays a narration label with [NARRATION] prefix.
     */
    public static void showNarrationLabel(String text) {
        print(colorize(PREFIX_NARRATION + " ", CYAN));
        println(colorize(text, WHITE));
    }

    /**
     * Displays text in a blockquote style with cyan left border and yellow italic text.
     */
    public static void showBlockquote(String text) {
        if (text == null || text.isEmpty()) return;

        String[] lines = wrapText(text, DEFAULT_WIDTH - 4);
        for (String line : lines) {
            print(colorize("  " + BLOCKQUOTE_BORDER + " ", CYAN));
            println(colorize(italic(line), YELLOW));
        }
    }

    /**
     * Displays multiple lines in blockquote style.
     */
    public static void showBlockquote(String[] lines) {
        if (lines == null) return;

        for (String line : lines) {
            if (line == null) continue;
            // Wrap each line if it's too long
            String[] wrapped = wrapText(line, DEFAULT_WIDTH - 4);
            for (String wrappedLine : wrapped) {
                print(colorize("  " + BLOCKQUOTE_BORDER + " ", CYAN));
                println(colorize(italic(wrappedLine), YELLOW));
            }
        }
    }

    /**
     * Wraps text to specified width, returning array of lines.
     */
    private static String[] wrapText(String text, int width) {
        if (text == null || text.isEmpty()) return new String[0];

        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() + 1 > width) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
            }
            if (!line.isEmpty()) {
                line.append(" ");
            }
            line.append(word);
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Displays a quest started notification.
     */
    public static void showQuestStarted(String questName) {
        println();
        println(colorize("+ QUEST STARTED: " + questName, GREEN));
        println();
    }

    /**
     * Displays a clue gained notification.
     */
    public static void showClueGained(String description) {
        println();
        println(colorize("+ CLUE GAINED: " + description, GREEN));
        println();
    }

    /**
     * Displays a tutorial tip in a bordered box.
     */
    public static void showTutorialTip(String tipText) {
        println();
        int contentWidth = DEFAULT_WIDTH - 4;

        // Top border with header - using ASCII for terminal compatibility
        print(colorize("+-", YELLOW));
        print(colorize(" TIP ", YELLOW));
        print(colorize("-".repeat(contentWidth - 5), YELLOW));
        println(colorize("-+", YELLOW));

        // Content
        String[] lines = wrapText(tipText, contentWidth - 2);
        for (String line : lines) {
            print(colorize("| ", YELLOW));
            print(colorize(line, WHITE));
            print(" ".repeat(contentWidth - line.length()));
            println(colorize(" |", YELLOW));
        }

        // Bottom border
        print(colorize("+", YELLOW));
        print(colorize("-".repeat(contentWidth + 2), YELLOW));
        println(colorize("+", YELLOW));
        println();
    }

    /**
     * Displays the action prompt with optional command suggestions and a
     * help hint. GameEngine computes context-aware suggestions every loop;
     * surface them so the player sees what's actionable right now.
     */
    public static void showActionPrompt(String[] suggestions) {
        println();
        if (suggestions != null && suggestions.length > 0) {
            println(colorize("Try: " + String.join(", ", suggestions), DEFAULT));
        }
        println(colorize("(type 'help' for commands)", DEFAULT));
    }

    /**
     * Echoes user input back for visual feedback.
     */
    public static void echoUserInput(String input) {
        println(colorize("> " + input, GREEN));
    }

    /**
     * Displays an enhanced skill check with highlighted components.
     */
    public static void showEnhancedSkillCheck(String skillName, int roll, int modifier, int total, int dc, boolean success) {
        String modStr = modifier >= 0 ? "+" + modifier : String.valueOf(modifier);

        print(colorize(PREFIX_ROLL + " ", CYAN));
        print(colorize(skillName + " Check: ", WHITE));
        print(colorize("d20: ", WHITE));
        print(colorize(String.valueOf(roll), YELLOW));
        print(colorize(" " + modStr + " = ", WHITE));
        print(colorize(String.valueOf(total), YELLOW));
        print(" ");

        if (success) {
            println(colorize("SUCCESS!", GREEN));
        } else {
            println(colorize("FAILURE", RED));
        }

        // Natural 20 or 1 callouts
        if (roll == 20) {
            println(colorize("  ★ NATURAL 20! ★", YELLOW));
        } else if (roll == 1) {
            println(colorize("  ✗ Natural 1...", RED));
        }
    }
}