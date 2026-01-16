package com.questkeeper.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for dice rolling operations.
 * 
 * Supports all standard D&D dice (d4, d6, d8, d10, d12, d20, d100)
 * and common rolling mechanics like advantage, disadvantage, and modifiers.
 * Includes roll history tracking for debugging and gameplay display.
 * 
 * @author Marc McGough
 * @version 1.1.0
 */
public final class Dice {
    
    private static final Random random = new Random();

    private static final Pattern DICE_PATTERN = Pattern.compile(
            "^(\\d*)d(\\d+)([+-]\\d+)?$", Pattern.CASE_INSENSITIVE);

    // Thread-safe list for roll history
    private static final List<String> rollHistory = Collections.synchronizedList(new ArrayList<>());

    private static final int MAX_HISTORY_SIZE = 1000;

    private Dice() {
        throw new AssertionError("Dice is a utility class and cannot be instantiated");
    }

    public static List<String> getRollHistory() {
        synchronized (rollHistory) {
            return Collections.unmodifiableList(new ArrayList<>(rollHistory));
        }
    }

    public static String getLastRoll() {
        synchronized (rollHistory) {
            if (rollHistory.isEmpty()) {
                return null;
            }
            return rollHistory.get(rollHistory.size() - 1);
        }
    }

    public static List<String> getRecentRolls(int count) {
        if (count <= 0 || rollHistory.isEmpty()) {
            return Collections.emptyList();
        }
        synchronized (rollHistory) {
            int startIndex = Math.max(0, rollHistory.size() - count);
            return Collections.unmodifiableList(
                    new ArrayList<>(rollHistory.subList(startIndex, rollHistory.size())));
        }
    }
    
    public static void clearRollHistory() {
        rollHistory.clear();
    }

    public static int getHistorySize() {
        return rollHistory.size();
    }

    private static void addToHistory(String entry) {
        synchronized (rollHistory) {
            // Trim oldest entries if we're at capacity
            if (rollHistory.size() >= MAX_HISTORY_SIZE) {
                rollHistory.remove(0);
            }
            rollHistory.add(entry);
        }
    }

    public static int roll(int sides) {
        if (sides < 1) {
            throw new IllegalArgumentException("Die must have at least 1 side, got: " + sides);
        }
        int result = random.nextInt(sides) + 1;
        addToHistory(String.format("d%d: %d", sides, result));
        return result;
    }

    public static int rollD20() {
        return roll(20);
    }

    public static int rollD6() {
        return roll(6);
    }

    public static int rollD4() {
        return roll(4);
    }

    public static int rollD8() {
        return roll(8);
    }

    public static int rollD10() {
        return roll(10);
    }
    
    public static int rollD12() {
        return roll(12);
    }

    public static int rollD100() {
        return roll(100);
    }

    public static int rollMultiple(int count, int sides) {
        if (count < 1) {
            throw new IllegalArgumentException("Must roll at least 1 die, got: " + count);
        }
        if (sides < 1) {
            throw new IllegalArgumentException("Die must have at least 1 side, got: " + sides);
        }
        
        int[] rolls = new int[count];
        int total = 0;
        
        for (int i = 0; i < count; i++) {
            rolls[i] = random.nextInt(sides) + 1;
            total += rolls[i];
        }

        StringBuilder historyEntry = new StringBuilder();
        historyEntry.append(String.format("%dd%d: [", count, sides));
        for (int i = 0; i < rolls.length; i++) {
            if (i > 0) historyEntry.append(", ");
            historyEntry.append(rolls[i]);
        }
        historyEntry.append(String.format("] = %d", total));
        addToHistory(historyEntry.toString());
        
        return total;
    }

    public static int rollWithModifier(int sides, int modifier) {
        if (sides < 1) {
            throw new IllegalArgumentException("Die must have at least 1 side, got: " + sides);
        }
        
        int baseRoll = random.nextInt(sides) + 1;
        int total = baseRoll + modifier;

        String modStr = modifier >= 0 ? String.format(" + %d", modifier) 
                                      : String.format(" - %d", Math.abs(modifier));
        addToHistory(String.format("d%d: %d%s = %d", sides, baseRoll, modStr, total));
        
        return total;
    }

    public static int rollMultipleWithModifier(int count, int sides, int modifier) {
        if (count < 1) {
            throw new IllegalArgumentException("Must roll at least 1 die, got: " + count);
        }
        if (sides < 1) {
            throw new IllegalArgumentException("Die must have at least 1 side, got: " + sides);
        }
        
        int[] rolls = new int[count];
        int subtotal = 0;
        
        for (int i = 0; i < count; i++) {
            rolls[i] = random.nextInt(sides) + 1;
            subtotal += rolls[i];
        }
        
        int total = subtotal + modifier;

        StringBuilder historyEntry = new StringBuilder();
        historyEntry.append(String.format("%dd%d: [", count, sides));
        for (int i = 0; i < rolls.length; i++) {
            if (i > 0) historyEntry.append(", ");
            historyEntry.append(rolls[i]);
        }
        historyEntry.append(String.format("] = %d", subtotal));

        String modStr = modifier >= 0 ? String.format(" + %d", modifier) 
                                      : String.format(" - %d", Math.abs(modifier));
        historyEntry.append(String.format("%s = %d", modStr, total));
        addToHistory(historyEntry.toString());
        
        return total;
    }

    public static boolean checkAgainstDC(int modifier, int dc) {
        int baseRoll = random.nextInt(20) + 1;
        int total = baseRoll + modifier;
        boolean success = total >= dc;
        
        String modStr = modifier >= 0 ? String.format(" + %d", modifier) 
                                      : String.format(" - %d", Math.abs(modifier));
        String critNote = "";
        if (baseRoll == 20) {
            critNote = " (NAT 20!)";
        } else if (baseRoll == 1) {
            critNote = " (NAT 1)";
        }
        
        addToHistory(String.format("d20: %d%s = %d vs DC %d - %s%s", 
                baseRoll, modStr, total, dc, 
                success ? "SUCCESS" : "FAILURE", critNote));
        
        return success;
    }

    public static int rollWithAdvantage() {
        int roll1 = random.nextInt(20) + 1;
        int roll2 = random.nextInt(20) + 1;
        int result = Math.max(roll1, roll2);
        
        addToHistory(String.format("d20 (Advantage): [%d, %d] = %d", roll1, roll2, result));
        
        return result;
    }

    public static int rollWithDisadvantage() {
        int roll1 = random.nextInt(20) + 1;
        int roll2 = random.nextInt(20) + 1;
        int result = Math.min(roll1, roll2);
        
        addToHistory(String.format("d20 (Disadvantage): [%d, %d] = %d", roll1, roll2, result));
        
        return result;
    }

    public static int rollWithAdvantage(int modifier) {
        int roll1 = random.nextInt(20) + 1;
        int roll2 = random.nextInt(20) + 1;
        int baseResult = Math.max(roll1, roll2);
        int total = baseResult + modifier;
        
        String modStr = modifier >= 0 ? String.format(" + %d", modifier) 
                                      : String.format(" - %d", Math.abs(modifier));
        addToHistory(String.format("d20 (Advantage): [%d, %d] = %d%s = %d", 
                roll1, roll2, baseResult, modStr, total));
        
        return total;
    }

    public static int rollWithDisadvantage(int modifier) {
        int roll1 = random.nextInt(20) + 1;
        int roll2 = random.nextInt(20) + 1;
        int baseResult = Math.min(roll1, roll2);
        int total = baseResult + modifier;
        
        String modStr = modifier >= 0 ? String.format(" + %d", modifier) 
                                      : String.format(" - %d", Math.abs(modifier));
        addToHistory(String.format("d20 (Disadvantage): [%d, %d] = %d%s = %d", 
                roll1, roll2, baseResult, modStr, total));
        
        return total;
    }

    public static boolean wasNatural20() {
        String lastRoll = getLastRoll();
        return lastRoll != null && lastRoll.contains("NAT 20");
    }

    public static boolean wasNatural1() {
        String lastRoll = getLastRoll();
        return lastRoll != null && lastRoll.contains("NAT 1") && !lastRoll.contains("NAT 20");
    }

    public static int parse(String notation) {
        if (notation == null || notation.trim().isEmpty()) {
            throw new IllegalArgumentException("Dice notation cannot be null or empty");
        }
        
        Matcher matcher = DICE_PATTERN.matcher(notation.trim());
        
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid dice notation: " + notation);
        }

        String countStr = matcher.group(1);
        int count = (countStr == null || countStr.isEmpty()) ? 1 : Integer.parseInt(countStr);

        int sides = Integer.parseInt(matcher.group(2));

        String modifierStr = matcher.group(3);
        int modifier = (modifierStr == null) ? 0 : Integer.parseInt(modifierStr);

        if (modifier == 0) {
            return rollMultiple(count, sides);
        } else {
            return rollMultipleWithModifier(count, sides, modifier);
        }
    }

    public static RollResult parseDetailed(String notation) {
        int historyIndexBefore = rollHistory.size();
        int result = parse(notation);
        String historyEntry = rollHistory.get(historyIndexBefore);
        return new RollResult(result, historyEntry, notation);
    }

    public static class RollResult {
        private final int total;
        private final String description;
        private final String notation;
        
        public RollResult(int total, String description, String notation) {
            this.total = total;
            this.description = description;
            this.notation = notation;
        }
        
        public int getTotal() { return total; }
        public String getDescription() { return description; }
        public String getNotation() { return notation; }
        
        @Override
        public String toString() {
            return String.format("[ROLL] %s: %s", notation, description);
        }
    }
}