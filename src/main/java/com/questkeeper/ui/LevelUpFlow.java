package com.questkeeper.ui;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import org.fusesource.jansi.Ansi;

import java.util.Scanner;

/**
 * Interactive flow that walks the player through spending pending Ability
 * Score Improvements (PHB p.15: at L4, L8, L12, L16, L19 a character gains
 * 2 ability points to spend, +2 to one OR +1/+1 to two distinct abilities).
 *
 * <p>Invoked from the main game loop whenever the player has unspent ASIs.
 * Does no I/O outside the supplied {@link Scanner} and {@link Display} so
 * tests can exercise it with a canned reader.
 */
public final class LevelUpFlow {

    private LevelUpFlow() {}

    public static void applyPendingAbilityScoreImprovements(
            Character character, Scanner scanner) {
        while (character.getPendingAbilityScoreImprovements() > 0) {
            Display.println();
            Display.printBox("LEVEL UP — Ability Score Improvement", 60, Ansi.Color.GREEN);
            Display.println(
                "You have an Ability Score Improvement to spend "
                + "(" + character.getPendingAbilityScoreImprovements()
                + " remaining).");
            Display.println("Current scores:");
            Display.println(character.getAbilityScoresString());
            Display.println();
            Display.println("  1) +2 to one ability");
            Display.println("  2) +1 to each of two distinct abilities");
            Display.println();
            Display.print("Choose 1 or 2: ");

            String choice = scanner.nextLine().trim();
            if (choice.equals("1")) {
                Ability target = readAbility(scanner, "Ability to raise by +2");
                if (target == null) continue;
                try {
                    character.applyAbilityScoreImprovement(target);
                    Display.println("Raised " + target + " by 2.");
                } catch (RuntimeException e) {
                    Display.println("Cannot apply: " + e.getMessage());
                }
            } else if (choice.equals("2")) {
                Ability first = readAbility(scanner, "First ability (+1)");
                if (first == null) continue;
                Ability second = readAbility(scanner, "Second ability (+1)");
                if (second == null) continue;
                try {
                    character.applyAbilityScoreImprovement(first, second);
                    Display.println("Raised " + first + " and " + second + " by 1 each.");
                } catch (RuntimeException e) {
                    Display.println("Cannot apply: " + e.getMessage());
                }
            } else {
                Display.println("Pick 1 or 2.");
            }
        }
    }

    private static Ability readAbility(Scanner scanner, String label) {
        Display.print(label + " (str/dex/con/int/wis/cha): ");
        String raw = scanner.nextLine().trim().toUpperCase();
        return switch (raw) {
            case "STR", "STRENGTH" -> Ability.STRENGTH;
            case "DEX", "DEXTERITY" -> Ability.DEXTERITY;
            case "CON", "CONSTITUTION" -> Ability.CONSTITUTION;
            case "INT", "INTELLIGENCE" -> Ability.INTELLIGENCE;
            case "WIS", "WISDOM" -> Ability.WISDOM;
            case "CHA", "CHARISMA" -> Ability.CHARISMA;
            default -> {
                Display.println("Unrecognized ability: " + raw);
                yield null;
            }
        };
    }
}
