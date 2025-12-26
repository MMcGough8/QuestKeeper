package com.questkeeper.core;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for dice rolling operatoins.
 * 
 * Supports all standard D&D dice (d4, d6, d8, d10, d12, d20, d100)
 * and common rolling mechanics like advantage, disadvantage, and modifiers.
 * 
 * @author Marc McGough
 * @version 1.0.0
 */

public final class Dice {

    private static final Random random = new Random();

    private static final Pattern DICE_PATTERN = Pattern.compile
    ("^(\\d*)d(\\d+)([+-]\\d+)?$", Pattern.CASE_INSENSITIVE);

    private Dice() {
    throw new AssertionError("Dice is a utility class and cannot be instantiated"); 
    }

    public static int roll(int sides) {
        if (sides < 1){
            throw new IllegalArgumentException("Dice must have at least 1 side, got: " + sides);
        }
        return random.nextInt(sides) + 1;
    }

    public static int rollMultiple(int count, int sides) {
        if (count < 0) {
            throw new IllegalArgumentException("Cannot roll negative number of dice: " + count);
        }
        if (count == 0) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < count; i++) {
            total += roll(sides);
        }
        return total;
    }

    public static int rollWithModifier(int sides, int modifier) {
        return roll(sides) + modifier;
    }

    public static int rollD20() {
        return roll(20);
    }

    public static int rollD6() {
        return roll(6);
    }

    public static boolean checkAgainstDC(int modifier, int dc) {
        int roll = rollD20();
        int total = roll + modifier;
        return total >= dc;
    }

    public static int rollWithAdvantage() {
        int roll1 = rollD20();
        int roll2 = rollD20();
        return Math.max(roll1, roll2);
    }

    public static int rollWithDisadvantage() {
        int roll1 = rollD20();
        int roll2 = rollD20();
        return Math.min(roll1, roll2);
    }

    public static int parse(String notation) {
        if (notation == null || notation.trim().isEmpty()) {
            throw new IllegalArgumentException("Dice notation cannot be null or empty");
        }

        String cleaned = notation.trim().toLowerCase();
        Matcher matcher = DICE_PATTERN.matcher(cleaned);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid dice notation: " + notation);
        }

        String countStr = matcher.group(1);
        int count = (countStr == null || countStr.isEmpty()) ? 1 : Integer.parseInt(countStr);

        int sides = Integer.parseInt(matcher.group(2));

        String modifierStr = matcher.group(3);
        int modifier = (modifierStr == null) ? 0 : Integer.parseInt(modifierStr);
    
        return rollMultiple(count, sides) + modifier;
    }
}

