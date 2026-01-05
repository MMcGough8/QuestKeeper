package com.questkeeper.ui;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.core.Dice;

import java.util.*;
import static com.questkeeper.ui.Display.*;

/**
 * Enhanced D&D Character Creator with multiple ability score methods.
 * Work in progress!
 */
public class CharacterCreator {
    
    private static final Scanner scanner = new Scanner(System.in);
    private static final int POINT_BUY_TOTAL = 27;
    private static final int[] POINT_BUY_COSTS = {0, 1, 2, 3, 4, 5, 7, 9}; // Cost to reach score 8 through 15
}

public static Character createCharacter() {
        Display.init();
        clearScreen();

        printBox("QUESTKEEPER", 70, YELLOW);
        printBox("A D&D 5th Edition Adventure", 70, WHITE);
        println();
        println(bold(colorize("Welcome, adventurer. Your story begins now...", CYAN)));
        println();
        pressEnterToContinue();

        Character character = new Character();

        // Step 1: Name
        clearScreen();
        printBox("STEP 1: NAME YOUR HERO", 70, MAGENTA);
        String name = promptForString("What is your character's name?", "Aelar");
        character.setName(name.isEmpty() ? "Aelar" : name);
        println(colorize("Welcome to the world, " + bold(character.getName()) + "!", GREEN));
        pressEnterToContinue();

        // Step 2: Race
        clearScreen();
        printBox("STEP 2: CHOOSE YOUR RACE", 70, MAGENTA);
        println(bold("Your ancestry shapes your abilities and place in the world.\n"));
        for (int i = 0; i < Race.values().length; i++) {
            Race race = Race.values()[i];
            String bonus = "+" + race.getAbilityBonusAmount() + " " + race.getAbilityBonus().getAbbreviation();
            if (race.getSecondaryBonus() != null) {
                bonus += " and +" + race.getSecondaryBonusAmount() + " " + race.getSecondaryBonus().getAbbreviation();
            }
            println(String.format("%s%d) %s%s — %s, %d ft speed",
                    colorize(String.valueOf(i + 1), YELLOW),
                    colorize(") ", WHITE),
                    bold(race.getDisplayName()),
                    colorize(" (" + race.getSubraceDisplay() + ")", MAGENTA),
                    bonus,
                    race.getSpeed()));
        }
        println();
        Race race = promptForEnum(Race.values(), "Select your race (number): ");
        character.setRace(race);
        printBox("You are a proud " + race.getDisplayName() + "!", 70, GREEN);
        pressEnterToContinue();

        // Step 3: Class
        clearScreen();
        printBox("STEP 3: CHOOSE YOUR CLASS", 70, MAGENTA);
        println(bold("Your class defines your path and power.\n"));
        for (int i = 0; i < CharacterClass.values().length; i++) {
            CharacterClass cc = CharacterClass.values()[i];
            println(String.format("%s%d) %s%s — Hit Die: d%d",
                    colorize(String.valueOf(i + 1), YELLOW),
                    colorize(") ", WHITE),
                    bold(cc.getDisplayName()),
                    colorize(" (Level 1)", MAGENTA),
                    cc.getHitDie()));
        }
        println();
        CharacterClass characterClass = promptForEnum(CharacterClass.values(), "Select your class (number): ");
        character.setCharacterClass(characterClass);
        character.setLevel(1);
        printBox("You are now a Level 1 " + characterClass.getDisplayName() + "!", 70, GREEN);
        pressEnterToContinue();

        // Step 4: Ability Score Method Selection
        clearScreen();
        printBox("STEP 4: DETERMINE ABILITY SCORES", 70, MAGENTA);
        println(bold("Choose how to generate your six ability scores:\n"));
        println("1) " + bold("Point Buy") + "        — Spend 27 points (official 5e method)");
        println("2) " + bold("Standard Array") + "  — Assign 15, 14, 13, 12, 10, 8");
        println("3) " + bold("Roll for Stats") + "  — 4d6 drop lowest ×6 (classic randomness)");
        println("4) " + bold("Random Everything") + " — Let fate decide race, class, and stats too!");
        println();

        int method = promptForInt("Choose method (1-4): ", 1, 4);

        if (method == 4) {
            randomFullCharacter(character);
        } else {
            Map<Ability, Integer> baseScores = new EnumMap<>(Ability.class);
            for (Ability a : Ability.values()) {
                baseScores.put(a, method == 1 ? 8 : 0); // Point buy starts at 8
            }

            switch (method) {
                case 1 -> pointBuySystem(character, baseScores);
                case 2 -> standardArrayAssignment(character, baseScores);
                case 3 -> rollForStats(character, baseScores);
            }

            // Apply final base scores
            for (Map.Entry<Ability, Integer> entry : baseScores.entrySet()) {
                character.setRawAbilityScore(entry.getKey(), entry.getValue());
            }
        }

        printBox("Ability scores locked in!", 70, GREEN);
        pressEnterToContinue();

        // Final Character Sheet
        showFinalCharacterSheet(character);

        Display.shutdown();
        return character;
    }


    private static void pointBuySystem(Character character, Map<Ability, Integer> scores) {
        clearScreen();
        printBox("POINT BUY — 27 points available", 70, YELLOW);
        println("Costs: 8→9 (1pt), 13→14 (2pt), 14→15 (2pt)\n");

        int pointsLeft = POINT_BUY_TOTAL;

        while (pointsLeft > 0) {
            clearScreen();
            printBox("POINT BUY — " + pointsLeft + " points remaining", 70, YELLOW);
            println(bold("Current Scores (before racial bonuses):\n"));

            for (Ability a : Ability.values()) {
                int score = scores.get(a);
                int costTo15 = getCumulativeCost(15) - getCumulativeCost(score);
                String racial = character.getAbilityModifierFromRace(a) > 0
                        ? colorize(" +" + character.getAbilityModifierFromRace(a), GREEN) : "";
                println(String.format("  %s%-3s%s %s%-12s%s: %2d%s %s",
                        colorize(a.getAbbreviation() + ":", CYAN),
                        colorize("", WHITE),
                        colorize("", WHITE),
                        colorize("", WHITE),
                        a.getFullName(),
                        colorize("", WHITE),
                        score,
                        racial,
                        costTo15 <= pointsLeft ? colorize("(→15: " + costTo15 + "pts)", MAGENTA) : ""));
            }
            println();

            Ability ability = promptForEnum(Ability.values(), "Increase which ability? ");
            int current = scores.get(ability);

            if (current >= 15) {
                println(colorize("Cannot exceed 15 before racial bonuses!", RED));
                pressEnterToContinue();
                continue;
            }

            int cost = POINT_BUY_COSTS[current - 7]; // index 0 = cost to go from 8→9
            if (cost > pointsLeft) {
                println(colorize("Not enough points!", RED));
                pressEnterToContinue();
                continue;
            }

            scores.put(ability, current + 1);
            pointsLeft -= cost;
            println(colorize("✓ " + ability.getFullName() + " → " + (current + 1) + " (-" + cost + " pts)", GREEN));
            pressEnterToContinue();
        }
    }

    private static void standardArrayAssignment(Character character, Map<Ability, Integer> scores) {
        clearScreen();
        printBox("STANDARD ARRAY", 70, YELLOW);
        println("Assign the values: " + bold("15, 14, 13, 12, 10, 8") + "\n");

        int[] array = {15, 14, 13, 12, 10, 8};

        for (int value : array) {
            clearScreen();
            printBox("Assign " + bold(String.valueOf(value)) + " to which ability?", 70, CYAN);
            println("Current assignments:\n");

            for (Ability a : Ability.values()) {
                int curr = scores.getOrDefault(a, 0);
                String racial = character.getAbilityModifierFromRace(a) > 0
                        ? colorize(" +" + character.getAbilityModifierFromRace(a), GREEN) : "";
                println(String.format("  %s%-12s%s: %2d%s", colorize(a.getAbbreviation() + ":", WHITE),
                        a.getFullName(), colorize("", WHITE), curr, racial));
            }
            println();

            Ability ability;
            do {
                ability = promptForEnum(Ability.values(), "Choose ability: ");
            } while (scores.containsKey(ability));

            scores.put(ability, value);
            println(colorize("✓ Assigned " + value + " to " + ability.getFullName(), GREEN));
            pressEnterToContinue();
        }
    }

