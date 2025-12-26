package com.questkeeper.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Dice utility.
 * 
 * Tests cover all standard D&D dice (d4, d6, d8, d10, d12, d20, d100)
 * and various rolling scenarios used in gameplay.
 */
@DisplayName("Dice Utility Tests")
class DiceTest {

    // ========================================================================
    // SINGLE DIE ROLLS
    // ========================================================================
    
    @Nested
    @DisplayName("Single Die Rolls")
    class SingleDieRolls {
        
        @RepeatedTest(100)
        @DisplayName("roll(6) returns value between 1 and 6")
        void rollD6ReturnsValidRange() {
            int result = Dice.roll(6);
            assertTrue(result >= 1 && result <= 6,
                    "d6 roll should be between 1 and 6, got: " + result);
        }
        
        @RepeatedTest(100)
        @DisplayName("roll(20) returns value between 1 and 20")
        void rollD20ReturnsValidRange() {
            int result = Dice.roll(20);
            assertTrue(result >= 1 && result <= 20,
                    "d20 roll should be between 1 and 20, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("roll(4) returns value between 1 and 4")
        void rollD4ReturnsValidRange() {
            int result = Dice.roll(4);
            assertTrue(result >= 1 && result <= 4,
                    "d4 roll should be between 1 and 4, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("roll(8) returns value between 1 and 8")
        void rollD8ReturnsValidRange() {
            int result = Dice.roll(8);
            assertTrue(result >= 1 && result <= 8,
                    "d8 roll should be between 1 and 8, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("roll(10) returns value between 1 and 10")
        void rollD10ReturnsValidRange() {
            int result = Dice.roll(10);
            assertTrue(result >= 1 && result <= 10,
                    "d10 roll should be between 1 and 10, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("roll(12) returns value between 1 and 12")
        void rollD12ReturnsValidRange() {
            int result = Dice.roll(12);
            assertTrue(result >= 1 && result <= 12,
                    "d12 roll should be between 1 and 12, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("roll(100) returns value between 1 and 100")
        void rollD100ReturnsValidRange() {
            int result = Dice.roll(100);
            assertTrue(result >= 1 && result <= 100,
                    "d100 roll should be between 1 and 100, got: " + result);
        }
    }

    // ========================================================================
    // CONVENIENCE METHODS
    // ========================================================================
    
    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {
        
        @RepeatedTest(100)
        @DisplayName("rollD20() returns value between 1 and 20")
        void rollD20MethodReturnsValidRange() {
            int result = Dice.rollD20();
            assertTrue(result >= 1 && result <= 20,
                    "rollD20() should return between 1 and 20, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("rollD6() returns value between 1 and 6")
        void rollD6MethodReturnsValidRange() {
            int result = Dice.rollD6();
            assertTrue(result >= 1 && result <= 6,
                    "rollD6() should return between 1 and 6, got: " + result);
        }
    }

    // ========================================================================
    // MULTIPLE DICE ROLLS
    // ========================================================================
    
    @Nested
    @DisplayName("Multiple Dice Rolls")
    class MultipleDiceRolls {
        
        @RepeatedTest(50)
        @DisplayName("rollMultiple(2, 6) returns sum between 2 and 12")
        void roll2d6ReturnsValidRange() {
            int result = Dice.rollMultiple(2, 6);
            assertTrue(result >= 2 && result <= 12,
                    "2d6 should return between 2 and 12, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("rollMultiple(3, 6) returns sum between 3 and 18")
        void roll3d6ReturnsValidRange() {
            int result = Dice.rollMultiple(3, 6);
            assertTrue(result >= 3 && result <= 18,
                    "3d6 should return between 3 and 18, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("rollMultiple(4, 6) returns sum between 4 and 24")
        void roll4d6ReturnsValidRange() {
            int result = Dice.rollMultiple(4, 6);
            assertTrue(result >= 4 && result <= 24,
                    "4d6 should return between 4 and 24, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("rollMultiple(1, 20) behaves same as roll(20)")
        void roll1d20SameAsRollD20() {
            int result = Dice.rollMultiple(1, 20);
            assertTrue(result >= 1 && result <= 20,
                    "1d20 should return between 1 and 20, got: " + result);
        }
        
        @Test
        @DisplayName("rollMultiple(0, 6) returns 0")
        void roll0d6ReturnsZero() {
            int result = Dice.rollMultiple(0, 6);
            assertEquals(0, result, "Rolling 0 dice should return 0");
        }
    }

    // ========================================================================
    // ROLL WITH MODIFIER
    // ========================================================================
    
    @Nested
    @DisplayName("Rolls with Modifier")
    class RollsWithModifier {
        
        @RepeatedTest(50)
        @DisplayName("rollWithModifier(20, 5) returns value between 6 and 25")
        void rollD20Plus5ReturnsValidRange() {
            int result = Dice.rollWithModifier(20, 5);
            assertTrue(result >= 6 && result <= 25,
                    "d20+5 should return between 6 and 25, got: " + result);
        }
        
        @RepeatedTest(50)
        @DisplayName("rollWithModifier(20, -2) returns value between -1 and 18")
        void rollD20Minus2ReturnsValidRange() {
            int result = Dice.rollWithModifier(20, -2);
            assertTrue(result >= -1 && result <= 18,
                    "d20-2 should return between -1 and 18, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("rollWithModifier(20, 0) same as roll(20)")
        void rollD20Plus0SameAsRollD20() {
            int result = Dice.rollWithModifier(20, 0);
            assertTrue(result >= 1 && result <= 20,
                    "d20+0 should return between 1 and 20, got: " + result);
        }
    }

    // ========================================================================
    // ABILITY CHECK (D20 + MODIFIER VS DC)
    // ========================================================================
    
    @Nested
    @DisplayName("Ability Checks")
    class AbilityChecks {
        
        @Test
        @DisplayName("checkAgainstDC with high modifier always succeeds against low DC")
        void highModifierBeatsLowDC() {
            // d20 (1-20) + 10 = 11-30, always beats DC 10
            for (int i = 0; i < 50; i++) {
                boolean result = Dice.checkAgainstDC(10, 10);
                assertTrue(result, "d20+10 should always beat DC 10");
            }
        }
        
        @Test
        @DisplayName("checkAgainstDC with very low modifier can fail high DC")
        void lowModifierCanFailHighDC() {
            // d20 (1-20) + 0 = 1-20, can fail DC 15
            boolean hadFailure = false;
            for (int i = 0; i < 100; i++) {
                if (!Dice.checkAgainstDC(0, 15)) {
                    hadFailure = true;
                    break;
                }
            }
            assertTrue(hadFailure, "d20+0 should sometimes fail DC 15");
        }
        
        @Test
        @DisplayName("checkAgainstDC returns boolean")
        void checkAgainstDCReturnsBoolean() {
            boolean result = Dice.checkAgainstDC(5, 12);
            assertNotNull(result);
        }
    }

    // ========================================================================
    // ADVANTAGE / DISADVANTAGE (D&D 5e Mechanic)
    // ========================================================================
    
    @Nested
    @DisplayName("Advantage and Disadvantage")
    class AdvantageDisadvantage {
        
        @RepeatedTest(100)
        @DisplayName("rollWithAdvantage() returns value between 1 and 20")
        void rollWithAdvantageReturnsValidRange() {
            int result = Dice.rollWithAdvantage();
            assertTrue(result >= 1 && result <= 20,
                    "Advantage roll should be between 1 and 20, got: " + result);
        }
        
        @RepeatedTest(100)
        @DisplayName("rollWithDisadvantage() returns value between 1 and 20")
        void rollWithDisadvantageReturnsValidRange() {
            int result = Dice.rollWithDisadvantage();
            assertTrue(result >= 1 && result <= 20,
                    "Disadvantage roll should be between 1 and 20, got: " + result);
        }
        
        @Test
        @DisplayName("Advantage average should be higher than normal d20 average")
        void advantageAverageShouldBeHigher() {
            long advantageSum = 0;
            long normalSum = 0;
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                advantageSum += Dice.rollWithAdvantage();
                normalSum += Dice.rollD20();
            }
            
            double advantageAvg = (double) advantageSum / iterations;
            double normalAvg = (double) normalSum / iterations;
            
            assertTrue(advantageAvg > normalAvg,
                    "Advantage average (" + advantageAvg + ") should be higher than normal (" + normalAvg + ")");
        }
        
        @Test
        @DisplayName("Disadvantage average should be lower than normal d20 average")
        void disadvantageAverageShouldBeLower() {
            long disadvantageSum = 0;
            long normalSum = 0;
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                disadvantageSum += Dice.rollWithDisadvantage();
                normalSum += Dice.rollD20();
            }
            
            double disadvantageAvg = (double) disadvantageSum / iterations;
            double normalAvg = (double) normalSum / iterations;
            
            assertTrue(disadvantageAvg < normalAvg,
                    "Disadvantage average (" + disadvantageAvg + ") should be lower than normal (" + normalAvg + ")");
        }
    }

    // ========================================================================
    // EDGE CASES AND INVALID INPUT
    // ========================================================================
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("roll(1) always returns 1")
        void rollD1AlwaysReturnsOne() {
            for (int i = 0; i < 10; i++) {
                assertEquals(1, Dice.roll(1), "d1 should always return 1");
            }
        }
        
        @Test
        @DisplayName("roll(0) throws IllegalArgumentException")
        void rollD0ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Dice.roll(0),
                    "Rolling a d0 should throw IllegalArgumentException");
        }
        
        @Test
        @DisplayName("roll(-1) throws IllegalArgumentException")
        void rollNegativeThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Dice.roll(-1),
                    "Rolling negative dice should throw IllegalArgumentException");
        }
        
        @Test
        @DisplayName("rollMultiple with negative count throws IllegalArgumentException")
        void rollMultipleNegativeCountThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Dice.rollMultiple(-1, 6),
                    "Rolling negative number of dice should throw IllegalArgumentException");
        }
    }

    // ========================================================================
    // DICE NOTATION PARSER (e.g., "2d6+3")
    // ========================================================================
    
    @Nested
    @DisplayName("Dice Notation Parser")
    class DiceNotationParser {
        
        @RepeatedTest(30)
        @DisplayName("parse('1d20') returns value between 1 and 20")
        void parse1d20ReturnsValidRange() {
            int result = Dice.parse("1d20");
            assertTrue(result >= 1 && result <= 20,
                    "1d20 should return between 1 and 20, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("parse('2d6') returns value between 2 and 12")
        void parse2d6ReturnsValidRange() {
            int result = Dice.parse("2d6");
            assertTrue(result >= 2 && result <= 12,
                    "2d6 should return between 2 and 12, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("parse('1d20+5') returns value between 6 and 25")
        void parse1d20Plus5ReturnsValidRange() {
            int result = Dice.parse("1d20+5");
            assertTrue(result >= 6 && result <= 25,
                    "1d20+5 should return between 6 and 25, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("parse('2d6+3') returns value between 5 and 15")
        void parse2d6Plus3ReturnsValidRange() {
            int result = Dice.parse("2d6+3");
            assertTrue(result >= 5 && result <= 15,
                    "2d6+3 should return between 5 and 15, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("parse('1d8-1') returns value between 0 and 7")
        void parse1d8Minus1ReturnsValidRange() {
            int result = Dice.parse("1d8-1");
            assertTrue(result >= 0 && result <= 7,
                    "1d8-1 should return between 0 and 7, got: " + result);
        }
        
        @RepeatedTest(30)
        @DisplayName("parse('d20') (shorthand) returns value between 1 and 20")
        void parseD20ShorthandReturnsValidRange() {
            int result = Dice.parse("d20");
            assertTrue(result >= 1 && result <= 20,
                    "d20 should return between 1 and 20, got: " + result);
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"1d6", "2d8", "3d4", "4d10", "1d12", "1d100"})
        @DisplayName("parse() handles various standard dice notations")
        void parseHandlesVariousNotations(String notation) {
            assertDoesNotThrow(() -> Dice.parse(notation),
                    "Should be able to parse: " + notation);
        }
        
        @Test
        @DisplayName("parse('invalid') throws IllegalArgumentException")
        void parseInvalidNotationThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Dice.parse("invalid"),
                    "Invalid notation should throw IllegalArgumentException");
        }
        
        @Test
        @DisplayName("parse('') throws IllegalArgumentException")
        void parseEmptyStringThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Dice.parse(""),
                    "Empty string should throw IllegalArgumentException");
        }
        
        @Test
        @DisplayName("parse(null) throws IllegalArgumentException")
        void parseNullThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Dice.parse(null),
                    "Null should throw IllegalArgumentException");
        }
    }

    // ========================================================================
    // DISTRIBUTION TEST (Statistical Sanity Check)
    // ========================================================================
    
    @Nested
    @DisplayName("Distribution Tests")
    class DistributionTests {
        
        @Test
        @DisplayName("d6 produces all values 1-6 over many rolls")
        void d6ProducesAllValues() {
            boolean[] seen = new boolean[7]; // index 0 unused
            
            for (int i = 0; i < 1000; i++) {
                int result = Dice.roll(6);
                seen[result] = true;
            }
            
            for (int i = 1; i <= 6; i++) {
                assertTrue(seen[i], "d6 should produce " + i + " at least once in 1000 rolls");
            }
        }
        
        @Test
        @DisplayName("d20 produces all values 1-20 over many rolls")
        void d20ProducesAllValues() {
            boolean[] seen = new boolean[21]; // index 0 unused
            
            for (int i = 0; i < 5000; i++) {
                int result = Dice.roll(20);
                seen[result] = true;
            }
            
            for (int i = 1; i <= 20; i++) {
                assertTrue(seen[i], "d20 should produce " + i + " at least once in 5000 rolls");
            }
        }
    }
}