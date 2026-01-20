package com.questkeeper.demo;

import com.questkeeper.ui.Display;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Automated demo for Demo Day presentations.
 * Run with: mvn exec:java -Dexec.mainClass="com.questkeeper.demo.AutoDemo" -q
 *
 * This showcases all UI features without requiring interactive input.
 * Use as a backup if live demo fails.
 */
public class AutoDemo {

    private static final int PAUSE_SHORT = 1500;
    private static final int PAUSE_MEDIUM = 2500;
    private static final int PAUSE_LONG = 4000;

    public static void main(String[] args) {
        try {
            runDemo();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runDemo() throws InterruptedException {
        // =====================================================================
        // TITLE SCREEN
        // =====================================================================
        Display.clearScreen();
        Display.showTitleScreen();
        Display.println();
        Display.showGameMessage("Welcome to QuestKeeper - Demo Day Edition");
        Display.println();
        Display.println(Display.colorize("Press any key to continue... (auto-advancing in demo mode)", WHITE));
        pause(PAUSE_LONG);

        // =====================================================================
        // CAMPAIGN SELECTION
        // =====================================================================
        Display.clearScreen();
        Display.showHeader();
        Display.println();
        Display.println(Display.colorize("SELECT YOUR CAMPAIGN", YELLOW));
        Display.println();
        Display.println("  1. " + Display.colorize("Muddlebrook: Harlequin Trials", CYAN) + " (Beginner)");
        Display.println("     Comedic mystery with theatrical puzzles");
        Display.println();
        Display.println("  2. " + Display.colorize("Eberron: Shards of the Fallen Sky", CYAN) + " (Intermediate)");
        Display.println("     Olympic competition with cosmic stakes");
        Display.println();
        Display.println("  3. " + Display.colorize("Whispers of the Drowned God", CYAN) + " (Advanced)");
        Display.println("     Gothic horror with multiple endings");
        Display.println();
        pause(PAUSE_MEDIUM);
        Display.echoUserInput("1");
        Display.showGameMessage("Loading Muddlebrook campaign...");
        pause(PAUSE_SHORT);

        // =====================================================================
        // CHARACTER CREATION
        // =====================================================================
        Display.clearScreen();
        Display.showHeader();
        Display.printBox("CHARACTER CREATION", 60, YELLOW);
        Display.println();

        Display.println(Display.colorize("Enter your character's name:", WHITE));
        pause(PAUSE_SHORT);
        Display.echoUserInput("Aldric");
        Display.println();

        Display.println(Display.colorize("Choose your race:", WHITE));
        Display.println("  1. Dwarf    2. Elf    3. Human    4. Halfling");
        pause(PAUSE_SHORT);
        Display.echoUserInput("3");
        Display.showSuccess("Human selected - Versatile and adaptable");
        Display.println();

        Display.println(Display.colorize("Choose your class:", WHITE));
        Display.println("  1. Fighter    2. Wizard    3. Rogue    4. Cleric");
        pause(PAUSE_SHORT);
        Display.echoUserInput("3");
        Display.showSuccess("Rogue selected - Master of stealth and skill");
        Display.println();

        // Show character summary
        Display.printBox("ALDRIC - Human Rogue", 60, CYAN);
        Display.println();
        Display.println("  STR: 10 (+0)    DEX: 16 (+3)    CON: 12 (+1)");
        Display.println("  INT: 14 (+2)    WIS: 10 (+0)    CHA: 14 (+2)");
        Display.println();
        Display.println("  HP: 10    AC: 14    Initiative: +3");
        Display.println();
        pause(PAUSE_LONG);

        // =====================================================================
        // CAMPAIGN INTRO
        // =====================================================================
        Display.clearScreen();
        Display.showHeader();
        Display.printBox("~ Muddlebrook: Harlequin Trials ~", 60, MAGENTA);
        Display.println();
        Display.showNarrationLabel("The adventure begins...");
        Display.println();

        Display.showBlockquote("The town of Muddlebrook has always been peculiar. Built on the edge of the Whisperwood, where reality grows thin, it attracts the strange and the stranger.");
        Display.println();
        pause(PAUSE_MEDIUM);

        Display.showBlockquote("Three days ago, Mayor Alderwick vanished from his locked office. In his place: a mechanical frog, a playing card (the Joker), and a note that read simply: 'THE TRIALS BEGIN.'");
        Display.println();
        pause(PAUSE_MEDIUM);

        Display.showBlockquote("You've come seeking adventure. You've found something far more interesting.");
        Display.println();
        pause(PAUSE_LONG);

        // =====================================================================
        // FIRST LOCATION - THE INN
        // =====================================================================
        Display.clearScreen();
        Display.showStatusPanel(10, 10, 1, 0, 3);
        Display.println();
        Display.printBox("The Drunken Dragon Inn", 60, CYAN);
        Display.println();

        Display.showNarrationLabel("As you enter...");
        Display.println();
        Display.showBlockquote("You push open the heavy oak door and warmth washes over you. The Drunken Dragon Inn is alive with the clink of mugs and murmur of conversation. A bard strums lazily in the corner, and the bartender polishes glasses with practiced ease.");
        Display.println();

        Display.println(Display.colorize("You see:", WHITE));
        Display.println("  - " + Display.colorize("Norrin", CYAN) + " (a bard with knowing eyes)");
        Display.println("  - " + Display.colorize("Mara", CYAN) + " (the bartender)");
        Display.println();

        Display.println(Display.colorize("Exits: north (Town Square), door (outside)", WHITE));

        Display.showTutorialTip("Use 'look' to examine your surroundings, 'go <direction>' to move, and 'talk <name>' to speak with characters.");

        pause(PAUSE_LONG);

        // =====================================================================
        // NPC DIALOGUE
        // =====================================================================
        Display.showActionPrompt(new String[]{"look", "talk norrin", "go north", "inventory"});
        pause(PAUSE_SHORT);
        Display.echoUserInput("talk norrin");
        Display.println();

        Display.showDialogue("Norrin", "Ah, a new face! Come for the excitement, have you? The whole town's buzzing about the mayor's disappearance.", "warm, theatrical");
        Display.println();
        Display.println(Display.colorize("Topics: ", WHITE) + "harlequin, mayor, trials, muddlebrook");
        Display.println(Display.colorize("(Use 'ask about <topic>' or 'bye' to end conversation)", WHITE));
        pause(PAUSE_MEDIUM);

        Display.echoUserInput("ask about harlequin");
        Display.println();
        Display.showDialogue("Norrin", "The Harlequin Machinist, they call him. Leaves clockwork toys at crime scenes, riddles instead of demands. Some say he's mad. Others say he's making a point. I say... he's putting on quite a show.", "leaning in conspiratorially");

        Display.showClueGained("The Harlequin Machinist uses clockwork devices");
        pause(PAUSE_LONG);

        // =====================================================================
        // EXPLORATION
        // =====================================================================
        Display.echoUserInput("bye");
        Display.showGameMessage("You end the conversation with Norrin.");
        pause(PAUSE_SHORT);

        Display.echoUserInput("go north");
        Display.println();

        Display.clearScreen();
        Display.showStatusPanel(10, 10, 1, 0, 3);
        Display.println();
        Display.printBox("Muddlebrook Town Square", 60, CYAN);
        Display.println();

        Display.showBlockquote("The muddy center of town, surrounded by shops and the looming Town Hall. A weathered notice board stands at the center, plastered with papers.");
        Display.println();

        Display.println(Display.colorize("Exits: south (Inn), north (Town Hall), east (Market), west (Clocktower)", WHITE));
        pause(PAUSE_MEDIUM);

        // =====================================================================
        // ENTERING A TRIAL
        // =====================================================================
        Display.echoUserInput("go north");
        Display.println();

        Display.clearScreen();
        Display.showStatusPanel(10, 10, 1, 0, 3);
        Display.println();
        Display.printBox("Muddlebrook Town Hall", 60, CYAN);
        Display.println();

        Display.showBlockquote("The Town Hall's double doors open with a groan. Inside, dust motes dance in shafts of light from high windows. Portraits of past mayors line the walls, their painted eyes seeming to follow you.");
        Display.println();

        pause(PAUSE_SHORT);
        Display.echoUserInput("trial");
        Display.println();

        Display.printDivider('=', 60, MAGENTA);
        Display.println(Display.colorize("  TRIAL I: The Mayor's Riddle", MAGENTA));
        Display.printDivider('=', 60, MAGENTA);
        Display.println();

        Display.showQuestStarted("The Mayor's Riddle");

        Display.showBlockquote("You ascend to the mayor's office. Papers are scattered everywhere. A clock on the wall ticks backwards. A mechanical frog sits on the desk, watching you with glass eyes. And pinned to the mayor's empty chair is a note with a laughing mask drawn on it.");
        Display.println();
        pause(PAUSE_LONG);

        // =====================================================================
        // SKILL CHECK
        // =====================================================================
        Display.println(Display.colorize("A puzzle box sits on the desk, covered in strange symbols.", WHITE));
        Display.println(Display.colorize("You attempt to decipher the mechanism...", WHITE));
        Display.println();
        pause(PAUSE_SHORT);

        Display.showSkillCheck("Investigation", 17, 2, 12, true);
        Display.println();
        Display.showSuccess("You notice the symbols match constellations - and one is missing!");
        Display.println();
        pause(PAUSE_MEDIUM);

        Display.println(Display.colorize("The frog's eyes seem to follow your discovery...", WHITE));
        Display.println(Display.colorize("You reach for the box's hidden latch...", WHITE));
        Display.println();
        pause(PAUSE_SHORT);

        Display.showSkillCheck("Sleight of Hand", 14, 3, 10, true);
        Display.println();

        Display.println();
        Display.printDivider('=', 60, GREEN);
        Display.println(Display.colorize("  TRIAL COMPLETE!", GREEN));
        Display.printDivider('=', 60, GREEN);
        Display.println();

        Display.showClueGained("The Harlequin's next target is the Clocktower!");
        Display.showSuccess("Gained item: Clockwork Key");
        Display.println();
        pause(PAUSE_LONG);

        // =====================================================================
        // UPDATED STATUS
        // =====================================================================
        Display.clearScreen();
        Display.showStatusPanel(10, 10, 1, 1, 3);
        Display.println();
        Display.printBox("Town Hall - Mayor's Office", 60, CYAN);
        Display.println();

        Display.showBlockquote("The puzzle box lies open, revealing a brass key and a new note: 'Time waits for no one. Especially not at the Clocktower. - H.M.'");
        Display.println();

        Display.showGameMessage("The path to Clocktower Hill is now unlocked!");
        Display.println();
        pause(PAUSE_LONG);

        // =====================================================================
        // FINALE
        // =====================================================================
        Display.clearScreen();
        Display.showTitleScreen();
        Display.println();
        Display.printBox("DEMO COMPLETE", 60, GREEN);
        Display.println();
        Display.println(Display.colorize("What you've seen:", WHITE));
        Display.println("  - ASCII art title screen");
        Display.println("  - Campaign selection");
        Display.println("  - Character creation with D&D stats");
        Display.println("  - Narrative presentation with blockquotes");
        Display.println("  - Status panel (HP, Level, Trials)");
        Display.println("  - Mini-maps for each campaign");
        Display.println("  - NPC dialogue system");
        Display.println("  - Skill checks with dice rolls");
        Display.println("  - Quest and clue notifications");
        Display.println("  - Tutorial tips");
        Display.println();
        Display.println(Display.colorize("Technical highlights:", CYAN));
        Display.println("  - 1,900+ tests");
        Display.println("  - 3 complete campaigns");
        Display.println("  - YAML-driven content (zero code required)");
        Display.println("  - Clean architecture with design patterns");
        Display.println();
        Display.println(Display.colorize("Thank you for watching!", YELLOW));
        Display.println();
    }

    private static void pause(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
}
