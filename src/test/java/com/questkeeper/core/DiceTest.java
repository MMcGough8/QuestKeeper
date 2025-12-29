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
}