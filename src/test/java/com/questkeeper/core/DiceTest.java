package com.questkeeper.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DiceTest {
    
    @BeforeEach
    void setUp() {
        // Clear roll history before each test
        Dice.clearRollHistory();
    }
    
    // ========================================================================
    // ROLL HISTORY TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Roll History Tests")
    class RollHistoryTests {
        
        @Test
        @DisplayName("History starts empty")
        void historyStartsEmpty() {
            assertEquals(0, Dice.getHistorySize());
            assertTrue(Dice.getRollHistory().isEmpty());
            assertNull(Dice.getLastRoll());
        }
        
        @Test
        @DisplayName("Single roll adds to history")
        void singleRollAddsToHistory() {
            Dice.rollD20();
            
            assertEquals(1, Dice.getHistorySize());
            assertNotNull(Dice.getLastRoll());
            assertTrue(Dice.getLastRoll().startsWith("d20:"));
        }
        
        @Test
        @DisplayName("Multiple rolls accumulate in history")
        void multipleRollsAccumulate() {
            Dice.rollD20();
            Dice.rollD6();
            Dice.rollD8();
            
            assertEquals(3, Dice.getHistorySize());
            List<String> history = Dice.getRollHistory();
            assertTrue(history.get(0).startsWith("d20:"));
            assertTrue(history.get(1).startsWith("d6:"));
            assertTrue(history.get(2).startsWith("d8:"));
        }
        
        @Test
        @DisplayName("Clear history removes all entries")
        void clearHistoryWorks() {
            Dice.rollD20();
            Dice.rollD6();
            Dice.rollD8();
            
            assertEquals(3, Dice.getHistorySize());
            
            Dice.clearRollHistory();
            
            assertEquals(0, Dice.getHistorySize());
            assertTrue(Dice.getRollHistory().isEmpty());
        }
        
        @Test
        @DisplayName("getRecentRolls returns correct subset")
        void getRecentRollsWorks() {
            Dice.rollD4();
            Dice.rollD6();
            Dice.rollD8();
            Dice.rollD10();
            Dice.rollD12();
            
            List<String> recent = Dice.getRecentRolls(3);
            assertEquals(3, recent.size());
            assertTrue(recent.get(0).startsWith("d8:"));
            assertTrue(recent.get(1).startsWith("d10:"));
            assertTrue(recent.get(2).startsWith("d12:"));
        }
        
        @Test
        @DisplayName("getRecentRolls handles count larger than history")
        void getRecentRollsHandlesLargeCount() {
            Dice.rollD6();
            Dice.rollD8();
            
            List<String> recent = Dice.getRecentRolls(10);
            assertEquals(2, recent.size());
        }
        
        @Test
        @DisplayName("getRecentRolls handles zero and negative counts")
        void getRecentRollsHandlesInvalidCounts() {
            Dice.rollD6();
            
            assertTrue(Dice.getRecentRolls(0).isEmpty());
            assertTrue(Dice.getRecentRolls(-1).isEmpty());
        }
        
        @Test
        @DisplayName("History is unmodifiable")
        void historyIsUnmodifiable() {
            Dice.rollD20();
            List<String> history = Dice.getRollHistory();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                history.add("hacked entry");
            });
        }
        
        @Test
        @DisplayName("Roll with modifier shows correct format")
        void rollWithModifierHistoryFormat() {
            Dice.rollWithModifier(20, 5);
            
            String lastRoll = Dice.getLastRoll();
            assertTrue(lastRoll.contains("d20:"));
            assertTrue(lastRoll.contains("+ 5"));
            assertTrue(lastRoll.contains("="));
        }
        
        @Test
        @DisplayName("Roll with negative modifier shows correct format")
        void rollWithNegativeModifierHistoryFormat() {
            Dice.rollWithModifier(20, -2);
            
            String lastRoll = Dice.getLastRoll();
            assertTrue(lastRoll.contains("d20:"));
            assertTrue(lastRoll.contains("- 2"));
        }
        
        @Test
        @DisplayName("Multiple dice roll shows individual values")
        void multipleDiceHistoryFormat() {
            Dice.rollMultiple(3, 6);
            
            String lastRoll = Dice.getLastRoll();
            assertTrue(lastRoll.startsWith("3d6:"));
            assertTrue(lastRoll.contains("["));
            assertTrue(lastRoll.contains("]"));
            assertTrue(lastRoll.contains(","));
        }
        
        @Test
        @DisplayName("DC check shows success/failure")
        void dcCheckHistoryFormat() {
            // Run multiple times to get both successes and failures
            boolean foundSuccess = false;
            boolean foundFailure = false;
            
            for (int i = 0; i < 100 && !(foundSuccess && foundFailure); i++) {
                Dice.clearRollHistory();
                Dice.checkAgainstDC(0, 11);
                String lastRoll = Dice.getLastRoll();
                
                if (lastRoll.contains("SUCCESS")) foundSuccess = true;
                if (lastRoll.contains("FAILURE")) foundFailure = true;
            }
            
            assertTrue(foundSuccess, "Should have recorded at least one success");
            assertTrue(foundFailure, "Should have recorded at least one failure");
        }
        
        @Test
        @DisplayName("Advantage roll shows both dice")
        void advantageHistoryFormat() {
            Dice.rollWithAdvantage();
            
            String lastRoll = Dice.getLastRoll();
            assertTrue(lastRoll.contains("Advantage"));
            assertTrue(lastRoll.contains("["));
            assertTrue(lastRoll.contains(","));
        }
        
        @Test
        @DisplayName("Disadvantage roll shows both dice")
        void disadvantageHistoryFormat() {
            Dice.rollWithDisadvantage();
            
            String lastRoll = Dice.getLastRoll();
            assertTrue(lastRoll.contains("Disadvantage"));
            assertTrue(lastRoll.contains("["));
            assertTrue(lastRoll.contains(","));
        }
    }
    
    // ========================================================================
    // SINGLE DIE ROLL TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Single Die Roll Tests")
    class SingleDieTests {
        
        @Test
        @DisplayName("roll(4) returns values between 1 and 4")
        void rollD4ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(4);
                assertTrue(result >= 1 && result <= 4, 
                    "d4 roll should be 1-4, got: " + result);
            }
        }
        
        @Test
        @DisplayName("roll(6) returns values between 1 and 6")
        void rollD6ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(6);
                assertTrue(result >= 1 && result <= 6,
                    "d6 roll should be 1-6, got: " + result);
            }
        }
        
        @Test
        @DisplayName("roll(8) returns values between 1 and 8")
        void rollD8ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(8);
                assertTrue(result >= 1 && result <= 8,
                    "d8 roll should be 1-8, got: " + result);
            }
        }
        
        @Test
        @DisplayName("roll(10) returns values between 1 and 10")
        void rollD10ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(10);
                assertTrue(result >= 1 && result <= 10,
                    "d10 roll should be 1-10, got: " + result);
            }
        }
        
        @Test
        @DisplayName("roll(12) returns values between 1 and 12")
        void rollD12ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(12);
                assertTrue(result >= 1 && result <= 12,
                    "d12 roll should be 1-12, got: " + result);
            }
        }
        
        @Test
        @DisplayName("roll(20) returns values between 1 and 20")
        void rollD20ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(20);
                assertTrue(result >= 1 && result <= 20,
                    "d20 roll should be 1-20, got: " + result);
            }
        }
        
        @Test
        @DisplayName("roll(100) returns values between 1 and 100")
        void rollD100ReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.roll(100);
                assertTrue(result >= 1 && result <= 100,
                    "d100 roll should be 1-100, got: " + result);
            }
        }
    }
    
    // ========================================================================
    // CONVENIENCE METHOD TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Convenience Method Tests")
    class ConvenienceMethodTests {
        
        @Test
        @DisplayName("rollD20() returns values between 1 and 20")
        void rollD20Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD20();
                assertTrue(result >= 1 && result <= 20);
            }
        }
        
        @Test
        @DisplayName("rollD6() returns values between 1 and 6")
        void rollD6Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD6();
                assertTrue(result >= 1 && result <= 6);
            }
        }
        
        @Test
        @DisplayName("rollD4() returns values between 1 and 4")
        void rollD4Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD4();
                assertTrue(result >= 1 && result <= 4);
            }
        }
        
        @Test
        @DisplayName("rollD8() returns values between 1 and 8")
        void rollD8Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD8();
                assertTrue(result >= 1 && result <= 8);
            }
        }
        
        @Test
        @DisplayName("rollD10() returns values between 1 and 10")
        void rollD10Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD10();
                assertTrue(result >= 1 && result <= 10);
            }
        }
        
        @Test
        @DisplayName("rollD12() returns values between 1 and 12")
        void rollD12Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD12();
                assertTrue(result >= 1 && result <= 12);
            }
        }
        
        @Test
        @DisplayName("rollD100() returns values between 1 and 100")
        void rollD100Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollD100();
                assertTrue(result >= 1 && result <= 100);
            }
        }
    }
    
    // ========================================================================
    // MULTIPLE DICE TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Multiple Dice Tests")
    class MultipleDiceTests {
        
        @Test
        @DisplayName("rollMultiple(2, 6) returns values between 2 and 12")
        void roll2d6Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollMultiple(2, 6);
                assertTrue(result >= 2 && result <= 12,
                    "2d6 should be 2-12, got: " + result);
            }
        }
        
        @Test
        @DisplayName("rollMultiple(3, 6) returns values between 3 and 18")
        void roll3d6Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollMultiple(3, 6);
                assertTrue(result >= 3 && result <= 18,
                    "3d6 should be 3-18, got: " + result);
            }
        }
        
        @Test
        @DisplayName("rollMultiple(4, 6) returns values between 4 and 24")
        void roll4d6Works() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollMultiple(4, 6);
                assertTrue(result >= 4 && result <= 24,
                    "4d6 should be 4-24, got: " + result);
            }
        }
        
        @Test
        @DisplayName("rollMultipleWithModifier works correctly")
        void rollMultipleWithModifierWorks() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollMultipleWithModifier(2, 6, 3);
                assertTrue(result >= 5 && result <= 15,
                    "2d6+3 should be 5-15, got: " + result);
            }
        }
    }
    
    // ========================================================================
    // MODIFIER TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Modifier Tests")
    class ModifierTests {
        
        @Test
        @DisplayName("rollWithModifier adds positive modifier correctly")
        void positiveModifierWorks() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollWithModifier(20, 5);
                assertTrue(result >= 6 && result <= 25,
                    "d20+5 should be 6-25, got: " + result);
            }
        }
        
        @Test
        @DisplayName("rollWithModifier adds negative modifier correctly")
        void negativeModifierWorks() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollWithModifier(20, -2);
                assertTrue(result >= -1 && result <= 18,
                    "d20-2 should be -1 to 18, got: " + result);
            }
        }
    }
    
    // ========================================================================
    // ABILITY CHECK TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Ability Check Tests")
    class AbilityCheckTests {
        
        @Test
        @DisplayName("checkAgainstDC returns true when roll meets DC")
        void checkMeetsDC() {
            // With +20 modifier, minimum roll (1+20=21) always beats DC 10
            for (int i = 0; i < 50; i++) {
                assertTrue(Dice.checkAgainstDC(20, 10));
            }
        }
        
        @Test
        @DisplayName("checkAgainstDC returns false when roll fails DC")
        void checkFailsDC() {
            // With -10 modifier, maximum roll (20-10=10) never beats DC 15
            for (int i = 0; i < 50; i++) {
                assertFalse(Dice.checkAgainstDC(-10, 15));
            }
        }
        
        @Test
        @DisplayName("checkAgainstDC can both succeed and fail")
        void checkCanSucceedOrFail() {
            boolean foundSuccess = false;
            boolean foundFailure = false;
            
            for (int i = 0; i < 1000 && !(foundSuccess && foundFailure); i++) {
                if (Dice.checkAgainstDC(0, 11)) {
                    foundSuccess = true;
                } else {
                    foundFailure = true;
                }
            }
            
            assertTrue(foundSuccess, "Should have at least one success with mod +0 vs DC 11");
            assertTrue(foundFailure, "Should have at least one failure with mod +0 vs DC 11");
        }
    }
    
    // ========================================================================
    // ADVANTAGE/DISADVANTAGE TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Advantage/Disadvantage Tests")
    class AdvantageDisadvantageTests {
        
        @Test
        @DisplayName("rollWithAdvantage returns values between 1 and 20")
        void advantageReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollWithAdvantage();
                assertTrue(result >= 1 && result <= 20);
            }
        }
        
        @Test
        @DisplayName("rollWithDisadvantage returns values between 1 and 20")
        void disadvantageReturnsValidRange() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollWithDisadvantage();
                assertTrue(result >= 1 && result <= 20);
            }
        }
        
        @Test
        @DisplayName("Advantage tends to roll higher than normal")
        void advantageRollsHigher() {
            long advantageSum = 0;
            long normalSum = 0;
            int trials = 10000;
            
            for (int i = 0; i < trials; i++) {
                advantageSum += Dice.rollWithAdvantage();
                normalSum += Dice.rollD20();
            }
            
            double advantageAvg = (double) advantageSum / trials;
            double normalAvg = (double) normalSum / trials;
            
            assertTrue(advantageAvg > normalAvg,
                "Advantage average (" + advantageAvg + ") should exceed normal (" + normalAvg + ")");
        }
        
        @Test
        @DisplayName("Disadvantage tends to roll lower than normal")
        void disadvantageRollsLower() {
            long disadvantageSum = 0;
            long normalSum = 0;
            int trials = 10000;
            
            for (int i = 0; i < trials; i++) {
                disadvantageSum += Dice.rollWithDisadvantage();
                normalSum += Dice.rollD20();
            }
            
            double disadvantageAvg = (double) disadvantageSum / trials;
            double normalAvg = (double) normalSum / trials;
            
            assertTrue(disadvantageAvg < normalAvg,
                "Disadvantage average (" + disadvantageAvg + ") should be less than normal (" + normalAvg + ")");
        }
        
        @Test
        @DisplayName("rollWithAdvantage with modifier works")
        void advantageWithModifierWorks() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollWithAdvantage(5);
                assertTrue(result >= 6 && result <= 25,
                    "Advantage+5 should be 6-25, got: " + result);
            }
        }
        
        @Test
        @DisplayName("rollWithDisadvantage with modifier works")
        void disadvantageWithModifierWorks() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.rollWithDisadvantage(-2);
                assertTrue(result >= -1 && result <= 18,
                    "Disadvantage-2 should be -1 to 18, got: " + result);
            }
        }
    }
    
    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("roll(0) throws IllegalArgumentException")
        void rollZeroThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.roll(0));
        }
        
        @Test
        @DisplayName("roll(-1) throws IllegalArgumentException")
        void rollNegativeThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.roll(-1));
        }
        
        @Test
        @DisplayName("rollMultiple with 0 count throws")
        void rollMultipleZeroCountThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.rollMultiple(0, 6));
        }
        
        @Test
        @DisplayName("rollMultiple with 0 sides throws")
        void rollMultipleZeroSidesThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.rollMultiple(2, 0));
        }
    }
    
    // ========================================================================
    // DICE NOTATION PARSER TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Dice Notation Parser Tests")
    class ParserTests {
        
        @Test
        @DisplayName("parse('2d6+3') returns valid range")
        void parse2d6Plus3() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.parse("2d6+3");
                assertTrue(result >= 5 && result <= 15,
                    "2d6+3 should be 5-15, got: " + result);
            }
        }
        
        @Test
        @DisplayName("parse('1d20') returns valid range")
        void parse1d20() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.parse("1d20");
                assertTrue(result >= 1 && result <= 20);
            }
        }
        
        @Test
        @DisplayName("parse('d20') returns valid range (implied 1)")
        void parseD20Implied1() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.parse("d20");
                assertTrue(result >= 1 && result <= 20);
            }
        }
        
        @Test
        @DisplayName("parse('3d8-2') returns valid range")
        void parse3d8Minus2() {
            for (int i = 0; i < 100; i++) {
                int result = Dice.parse("3d8-2");
                assertTrue(result >= 1 && result <= 22,
                    "3d8-2 should be 1-22, got: " + result);
            }
        }
        
        @Test
        @DisplayName("parse is case insensitive")
        void parseCaseInsensitive() {
            int upper = Dice.parse("2D6");
            int lower = Dice.parse("2d6");
            int mixed = Dice.parse("2D6+3");
    
            assertTrue(upper >= 2 && upper <= 12, "2D6 should return 2-12");
            assertTrue(lower >= 2 && lower <= 12, "2d6 should return 2-12");
            assertTrue(mixed >= 5 && mixed <= 15, "2D6+3 should return 5-15");
}   
        
        @Test
        @DisplayName("parse throws on invalid notation")
        void parseInvalidThrows() {
            assertThrows(IllegalArgumentException.class, () -> Dice.parse("invalid"));
            assertThrows(IllegalArgumentException.class, () -> Dice.parse(""));
            assertThrows(IllegalArgumentException.class, () -> Dice.parse(null));
            assertThrows(IllegalArgumentException.class, () -> Dice.parse("d"));
            assertThrows(IllegalArgumentException.class, () -> Dice.parse("2d"));
        }
        
        @Test
        @DisplayName("parseDetailed returns RollResult with correct info")
        void parseDetailedWorks() {
            Dice.RollResult result = Dice.parseDetailed("2d6+3");
            
            assertTrue(result.getTotal() >= 5 && result.getTotal() <= 15);
            assertEquals("2d6+3", result.getNotation());
            assertNotNull(result.getDescription());
            assertTrue(result.toString().contains("2d6+3"));
        }
    }
    
    // ========================================================================
    // DISTRIBUTION TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Distribution Tests")
    class DistributionTests {
        
        @Test
        @DisplayName("All d6 values (1-6) are rolled over many trials")
        void d6DistributionCoversAllValues() {
            Set<Integer> rolledValues = new HashSet<>();
            
            for (int i = 0; i < 1000 && rolledValues.size() < 6; i++) {
                rolledValues.add(Dice.rollD6());
            }
            
            assertEquals(6, rolledValues.size(),
                "All values 1-6 should be rolled. Missing: " + getMissing(rolledValues, 1, 6));
        }
        
        @Test
        @DisplayName("All d20 values (1-20) are rolled over many trials")
        void d20DistributionCoversAllValues() {
            Set<Integer> rolledValues = new HashSet<>();
            
            for (int i = 0; i < 5000 && rolledValues.size() < 20; i++) {
                rolledValues.add(Dice.rollD20());
            }
            
            assertEquals(20, rolledValues.size(),
                "All values 1-20 should be rolled. Missing: " + getMissing(rolledValues, 1, 20));
        }
        
        private String getMissing(Set<Integer> rolled, int min, int max) {
            StringBuilder missing = new StringBuilder();
            for (int i = min; i <= max; i++) {
                if (!rolled.contains(i)) {
                    if (!missing.isEmpty()) missing.append(", ");
                    missing.append(i);
                }
            }
            return missing.toString();
        }
    }
    
    // ========================================================================
    // CRITICAL HIT/MISS DETECTION TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Critical Hit/Miss Detection Tests")
    class CriticalDetectionTests {
        
        @Test
        @DisplayName("wasNatural20 detects nat 20 in DC checks")
        void detectsNatural20() {
            boolean foundNat20 = false;
            
            for (int i = 0; i < 1000 && !foundNat20; i++) {
                Dice.checkAgainstDC(0, 25); // DC 25 so only nat 20 note appears on success
                if (Dice.wasNatural20()) {
                    foundNat20 = true;
                }
            }
            
            assertTrue(foundNat20, "Should detect a natural 20 within 1000 rolls");
        }
        
        @Test
        @DisplayName("wasNatural1 detects nat 1 in DC checks")
        void detectsNatural1() {
            boolean foundNat1 = false;
            
            for (int i = 0; i < 1000 && !foundNat1; i++) {
                Dice.checkAgainstDC(0, 1); // DC 1 so nat 1 note appears
                if (Dice.wasNatural1()) {
                    foundNat1 = true;
                }
            }
            
            assertTrue(foundNat1, "Should detect a natural 1 within 1000 rolls");
        }
    }

    // ========================================================================
    // STATISTICAL DISTRIBUTION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Statistical Distribution Tests")
    class StatisticalDistributionTests {

        private static final int SAMPLE_SIZE = 5000;
        private static final double TOLERANCE = 0.20; // 20% tolerance for random variance

        @BeforeEach
        void clearHistoryBeforeStatisticalTests() {
            // Clear history to prevent memory buildup from high-volume tests
            Dice.clearRollHistory();
        }

        @Test
        @DisplayName("d6 distribution is approximately uniform")
        void d6DistributionIsUniform() {
            Dice.clearRollHistory(); // Ensure history is clear
            int[] counts = new int[6];

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                int roll = Dice.rollD6();
                counts[roll - 1]++;
            }

            double expectedCount = SAMPLE_SIZE / 6.0;
            double minExpected = expectedCount * (1 - TOLERANCE);
            double maxExpected = expectedCount * (1 + TOLERANCE);

            for (int i = 0; i < 6; i++) {
                assertTrue(counts[i] >= minExpected && counts[i] <= maxExpected,
                    String.format("Face %d count %d should be within %.0f-%.0f (expected ~%.0f)",
                        i + 1, counts[i], minExpected, maxExpected, expectedCount));
            }
        }

        @Test
        @DisplayName("d20 distribution is approximately uniform")
        void d20DistributionIsUniform() {
            int[] counts = new int[20];

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                int roll = Dice.rollD20();
                counts[roll - 1]++;
            }

            double expectedCount = SAMPLE_SIZE / 20.0;
            double minExpected = expectedCount * (1 - TOLERANCE);
            double maxExpected = expectedCount * (1 + TOLERANCE);

            for (int i = 0; i < 20; i++) {
                assertTrue(counts[i] >= minExpected && counts[i] <= maxExpected,
                    String.format("Face %d count %d should be within %.0f-%.0f (expected ~%.0f)",
                        i + 1, counts[i], minExpected, maxExpected, expectedCount));
            }
        }

        @Test
        @DisplayName("d20 mean is approximately 10.5")
        void d20MeanIsCorrect() {
            long sum = 0;

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                sum += Dice.rollD20();
            }

            double mean = (double) sum / SAMPLE_SIZE;
            double expectedMean = 10.5;
            double allowedDeviation = 0.5; // Allow 0.5 deviation from expected mean

            assertTrue(Math.abs(mean - expectedMean) < allowedDeviation,
                String.format("d20 mean %.2f should be close to expected %.1f", mean, expectedMean));
        }

        @Test
        @DisplayName("2d6 distribution follows expected bell curve")
        void twoD6DistributionIsBellCurve() {
            int[] counts = new int[13]; // indices 0-12, but 0 and 1 unused (min roll is 2)

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                int roll = Dice.rollMultiple(2, 6);
                counts[roll]++;
            }

            // In 2d6, 7 is the most common (6/36 probability = 16.67%)
            // 2 and 12 are least common (1/36 probability = 2.78%)

            // Verify 7 is most common
            int maxCount = 0;
            int maxValue = 0;
            for (int i = 2; i <= 12; i++) {
                if (counts[i] > maxCount) {
                    maxCount = counts[i];
                    maxValue = i;
                }
            }
            assertEquals(7, maxValue, "7 should be the most frequent result for 2d6");

            // Verify extremes (2, 12) are less common than middle (6, 7, 8)
            assertTrue(counts[7] > counts[2], "7 should appear more often than 2");
            assertTrue(counts[7] > counts[12], "7 should appear more often than 12");
            assertTrue(counts[6] > counts[3], "6 should appear more often than 3");
            assertTrue(counts[8] > counts[11], "8 should appear more often than 11");
        }

        @Test
        @DisplayName("advantage rolls are statistically higher than normal")
        void advantageRollsAreHigher() {
            long normalSum = 0;
            long advantageSum = 0;

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                normalSum += Dice.rollD20();
                advantageSum += Dice.rollWithAdvantage();
            }

            double normalMean = (double) normalSum / SAMPLE_SIZE;
            double advantageMean = (double) advantageSum / SAMPLE_SIZE;

            // Advantage mean should be approximately 13.825 vs normal 10.5
            assertTrue(advantageMean > normalMean,
                String.format("Advantage mean (%.2f) should be higher than normal mean (%.2f)",
                    advantageMean, normalMean));

            // Advantage should increase mean by roughly 3+ points
            assertTrue(advantageMean - normalMean > 2.5,
                String.format("Advantage should increase mean by ~3.3, actual increase: %.2f",
                    advantageMean - normalMean));
        }

        @Test
        @DisplayName("disadvantage rolls are statistically lower than normal")
        void disadvantageRollsAreLower() {
            long normalSum = 0;
            long disadvantageSum = 0;

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                normalSum += Dice.rollD20();
                disadvantageSum += Dice.rollWithDisadvantage();
            }

            double normalMean = (double) normalSum / SAMPLE_SIZE;
            double disadvantageMean = (double) disadvantageSum / SAMPLE_SIZE;

            // Disadvantage mean should be approximately 7.175 vs normal 10.5
            assertTrue(disadvantageMean < normalMean,
                String.format("Disadvantage mean (%.2f) should be lower than normal mean (%.2f)",
                    disadvantageMean, normalMean));

            // Disadvantage should decrease mean by roughly 3+ points
            assertTrue(normalMean - disadvantageMean > 2.5,
                String.format("Disadvantage should decrease mean by ~3.3, actual decrease: %.2f",
                    normalMean - disadvantageMean));
        }

        @Test
        @DisplayName("natural 20 occurs approximately 5% of the time")
        void natural20OccursAtExpectedRate() {
            int nat20Count = 0;

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                if (Dice.rollD20() == 20) {
                    nat20Count++;
                }
            }

            double expectedRate = 0.05; // 5%
            double actualRate = (double) nat20Count / SAMPLE_SIZE;

            // Allow 1.5% tolerance (3.5% - 6.5%)
            assertTrue(actualRate >= 0.035 && actualRate <= 0.065,
                String.format("Natural 20 rate %.2f%% should be close to expected 5%%",
                    actualRate * 100));
        }

        @Test
        @DisplayName("natural 1 occurs approximately 5% of the time")
        void natural1OccursAtExpectedRate() {
            int nat1Count = 0;

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                if (Dice.rollD20() == 1) {
                    nat1Count++;
                }
            }

            double expectedRate = 0.05; // 5%
            double actualRate = (double) nat1Count / SAMPLE_SIZE;

            // Allow 1.5% tolerance (3.5% - 6.5%)
            assertTrue(actualRate >= 0.035 && actualRate <= 0.065,
                String.format("Natural 1 rate %.2f%% should be close to expected 5%%",
                    actualRate * 100));
        }

        @Test
        @DisplayName("4d6 drop lowest produces expected range")
        void fourD6DropLowestProducesExpectedRange() {
            int minSeen = Integer.MAX_VALUE;
            int maxSeen = Integer.MIN_VALUE;
            long sum = 0;

            for (int i = 0; i < SAMPLE_SIZE; i++) {
                // Roll 4d6, drop lowest
                int[] rolls = new int[4];
                for (int j = 0; j < 4; j++) {
                    rolls[j] = Dice.rollD6();
                }

                // Sort and sum top 3
                java.util.Arrays.sort(rolls);
                int result = rolls[1] + rolls[2] + rolls[3]; // indices 1,2,3 are top 3 after sort

                minSeen = Math.min(minSeen, result);
                maxSeen = Math.max(maxSeen, result);
                sum += result;
            }

            double mean = (double) sum / SAMPLE_SIZE;

            // Range should be 3-18 (minimum: three 1s after dropping a 1, maximum: three 6s)
            assertTrue(minSeen >= 3, "Minimum should be at least 3");
            assertTrue(maxSeen <= 18, "Maximum should be at most 18");

            // Mean for 4d6 drop lowest is approximately 12.24
            assertTrue(mean > 11.5 && mean < 13.0,
                String.format("4d6 drop lowest mean %.2f should be around 12.24", mean));
        }
    }
}