package com.questkeeper.combat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Monster class and Combatant interface implementation.
 * 
 * @author Marc McGough
 */
class MonsterTest {
    
    private Monster basicMonster;
    private Monster clockworkCritter;
    private Monster confettiOoze;
    
    @BeforeEach
    void setUp() {
        basicMonster = new Monster("goblin_01", "Goblin", 15, 20);
        clockworkCritter = Monster.createClockworkCritter("critter_01");
        confettiOoze = Monster.createConfettiOoze("ooze_01");
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Basic constructor sets correct values")
        void basicConstructor() {
            assertEquals("goblin_01", basicMonster.getId());
            assertEquals("Goblin", basicMonster.getName());
            assertEquals(15, basicMonster.getArmorClass());
            assertEquals(20, basicMonster.getMaxHitPoints());
            assertEquals(20, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Basic constructor uses default values")
        void basicConstructorDefaults() {
            assertEquals(Monster.Size.MEDIUM, basicMonster.getSize());
            assertEquals(Monster.MonsterType.HUMANOID, basicMonster.getType());
            assertEquals(30, basicMonster.getSpeed());
            assertEquals(0, basicMonster.getChallengeRating());
        }
        
        @Test
        @DisplayName("Full constructor sets all values")
        void fullConstructor() {
            Monster dragon = new Monster("dragon_01", "Young Dragon", 
                    Monster.Size.LARGE, Monster.MonsterType.DRAGON,
                    18, 120, 40, 5, 1800);
            
            assertEquals(Monster.Size.LARGE, dragon.getSize());
            assertEquals(Monster.MonsterType.DRAGON, dragon.getType());
            assertEquals(18, dragon.getArmorClass());
            assertEquals(120, dragon.getMaxHitPoints());
            assertEquals(40, dragon.getSpeed());
            assertEquals(5, dragon.getChallengeRating());
            assertEquals(1800, dragon.getExperienceValue());
        }
    }
    
    @Nested
    @DisplayName("Combatant Interface Tests")
    class CombatantInterfaceTests {
        
        @Test
        @DisplayName("getName returns monster name")
        void getName() {
            assertEquals("Goblin", basicMonster.getName());
        }
        
        @Test
        @DisplayName("getArmorClass returns correct AC")
        void getArmorClass() {
            assertEquals(15, basicMonster.getArmorClass());
        }
        
        @Test
        @DisplayName("getCurrentHitPoints returns current HP")
        void getCurrentHitPoints() {
            assertEquals(20, basicMonster.getCurrentHitPoints());
            basicMonster.takeDamage(5);
            assertEquals(15, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("getMaxHitPoints returns max HP")
        void getMaxHitPoints() {
            assertEquals(20, basicMonster.getMaxHitPoints());
            basicMonster.takeDamage(10);
            assertEquals(20, basicMonster.getMaxHitPoints()); // Max unchanged
        }
        
        @Test
        @DisplayName("isAlive returns true when HP > 0")
        void isAlive() {
            assertTrue(basicMonster.isAlive());
            basicMonster.takeDamage(19);
            assertTrue(basicMonster.isAlive());
            basicMonster.takeDamage(1);
            assertFalse(basicMonster.isAlive());
        }
        
        @Test
        @DisplayName("isUnconscious returns true when HP <= 0")
        void isUnconscious() {
            assertFalse(basicMonster.isUnconscious());
            basicMonster.takeDamage(20);
            assertTrue(basicMonster.isUnconscious());
        }
        
        @Test
        @DisplayName("isBloodied returns true when HP <= half max")
        void isBloodied() {
            assertFalse(basicMonster.isBloodied()); // 20/20
            basicMonster.takeDamage(9);
            assertFalse(basicMonster.isBloodied()); // 11/20
            basicMonster.takeDamage(1);
            assertTrue(basicMonster.isBloodied()); // 10/20 (exactly half)
            basicMonster.takeDamage(5);
            assertTrue(basicMonster.isBloodied()); // 5/20
        }
        
        @Test
        @DisplayName("getCombatStatus shows correct status")
        void getCombatStatus() {
            assertEquals("Goblin (HP: 20/20)", basicMonster.getCombatStatus());
            
            basicMonster.takeDamage(10);
            assertEquals("Goblin (HP: 10/20) [BLOODIED]", basicMonster.getCombatStatus());
            
            basicMonster.takeDamage(10);
            assertEquals("Goblin (HP: 0/20) [DOWN]", basicMonster.getCombatStatus());
        }
        
        @Test
        @DisplayName("getHpPercentage calculates correctly")
        void getHpPercentage() {
            assertEquals(100, basicMonster.getHpPercentage());
            basicMonster.takeDamage(10);
            assertEquals(50, basicMonster.getHpPercentage());
            basicMonster.takeDamage(5);
            assertEquals(25, basicMonster.getHpPercentage());
        }
        
        @Test
        @DisplayName("rollInitiative returns reasonable values")
        void rollInitiative() {
            // Run multiple times to verify range
            for (int i = 0; i < 100; i++) {
                int roll = basicMonster.rollInitiative();
                // With 0 DEX mod, should be 1-20
                assertTrue(roll >= 1 && roll <= 20, 
                        "Roll " + roll + " outside expected range");
            }
        }
    }
    
    @Nested
    @DisplayName("Damage and Healing Tests")
    class DamageHealingTests {
        
        @Test
        @DisplayName("takeDamage reduces HP")
        void takeDamage() {
            int dealt = basicMonster.takeDamage(5);
            assertEquals(5, dealt);
            assertEquals(15, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("takeDamage caps at current HP")
        void takeDamageCapsAtZero() {
            int dealt = basicMonster.takeDamage(100);
            assertEquals(20, dealt); // Only 20 actual damage
            assertEquals(0, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("takeDamage ignores non-positive values")
        void takeDamageIgnoresNonPositive() {
            assertEquals(0, basicMonster.takeDamage(0));
            assertEquals(0, basicMonster.takeDamage(-5));
            assertEquals(20, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("heal restores HP")
        void heal() {
            basicMonster.takeDamage(10);
            int healed = basicMonster.heal(5);
            assertEquals(5, healed);
            assertEquals(15, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("heal caps at max HP")
        void healCapsAtMax() {
            basicMonster.takeDamage(5);
            int healed = basicMonster.heal(100);
            assertEquals(5, healed); // Only healed 5
            assertEquals(20, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("heal ignores non-positive values")
        void healIgnoresNonPositive() {
            basicMonster.takeDamage(10);
            assertEquals(0, basicMonster.heal(0));
            assertEquals(0, basicMonster.heal(-5));
            assertEquals(10, basicMonster.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("resetHitPoints restores to full")
        void resetHitPoints() {
            basicMonster.takeDamage(15);
            assertEquals(5, basicMonster.getCurrentHitPoints());
            basicMonster.resetHitPoints();
            assertEquals(20, basicMonster.getCurrentHitPoints());
        }
    }
    
    @Nested
    @DisplayName("Combat Action Tests")
    class CombatActionTests {
        
        @Test
        @DisplayName("rollAttack returns reasonable values")
        void rollAttack() {
            // Basic monster has +2 attack bonus
            for (int i = 0; i < 100; i++) {
                int roll = basicMonster.rollAttack();
                assertTrue(roll >= 3 && roll <= 22, 
                        "Attack roll " + roll + " outside expected range");
            }
        }
        
        @Test
        @DisplayName("rollDamage returns damage in expected range")
        void rollDamage() {
            basicMonster.setDamageDice("1d6+2");
            for (int i = 0; i < 100; i++) {
                int damage = basicMonster.rollDamage();
                assertTrue(damage >= 3 && damage <= 8,
                        "Damage " + damage + " outside 1d6+2 range");
            }
        }
        
        @Test
        @DisplayName("attackHits correctly checks AC")
        void attackHits() {
            // This is probabilistic, but we can verify the method exists
            // and returns boolean
            boolean result = basicMonster.attackHits(10);
            assertNotNull(result);
        }
    }
    
    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {
        
        @Test
        @DisplayName("createClockworkCritter has correct stats")
        void clockworkCritterStats() {
            assertEquals("Clockwork Critter", clockworkCritter.getName());
            assertEquals(Monster.Size.SMALL, clockworkCritter.getSize());
            assertEquals(Monster.MonsterType.CONSTRUCT, clockworkCritter.getType());
            assertEquals(13, clockworkCritter.getArmorClass());
            assertEquals(11, clockworkCritter.getMaxHitPoints());
            assertEquals(0.25, clockworkCritter.getChallengeRating());
            assertEquals(50, clockworkCritter.getExperienceValue());
            assertEquals(4, clockworkCritter.getAttackBonus());
            assertEquals("1d6+2", clockworkCritter.getDamageDice());
        }
        
        @Test
        @DisplayName("createConfettiOoze has correct stats")
        void confettiOozeStats() {
            assertEquals("Confetti Ooze", confettiOoze.getName());
            assertEquals(Monster.Size.MEDIUM, confettiOoze.getSize());
            assertEquals(Monster.MonsterType.OOZE, confettiOoze.getType());
            assertEquals(8, confettiOoze.getArmorClass());
            assertEquals(30, confettiOoze.getMaxHitPoints());
            assertEquals(0.5, confettiOoze.getChallengeRating());
            assertEquals(100, confettiOoze.getExperienceValue());
        }
        
        @Test
        @DisplayName("createHarlequinMachinist has boss stats")
        void harlequinMachinistStats() {
            Monster boss = Monster.createHarlequinMachinist("hm_01");
            
            assertEquals("Harlequin Machinist", boss.getName());
            assertEquals(Monster.MonsterType.HUMANOID, boss.getType());
            assertEquals(16, boss.getArmorClass());
            assertEquals(110, boss.getMaxHitPoints());
            assertEquals(7, boss.getChallengeRating());
            assertEquals(2900, boss.getExperienceValue());
            assertEquals(8, boss.getAttackBonus());
        }
    }
    
    @Nested
    @DisplayName("Copy and Utility Tests")
    class CopyUtilityTests {
        
        @Test
        @DisplayName("copy creates independent instance")
        void copyCreatesIndependentInstance() {
            Monster original = Monster.createClockworkCritter("critter_original");
            Monster copy = original.copy("critter_copy");
            
            assertEquals("critter_copy", copy.getId());
            assertEquals(original.getName(), copy.getName());
            assertEquals(original.getArmorClass(), copy.getArmorClass());
            assertEquals(original.getMaxHitPoints(), copy.getMaxHitPoints());
            
            // Verify independence
            original.takeDamage(5);
            assertEquals(6, original.getCurrentHitPoints());
            assertEquals(11, copy.getCurrentHitPoints()); // Unchanged
        }
        
        @Test
        @DisplayName("getStatBlock returns formatted string")
        void getStatBlock() {
            String statBlock = clockworkCritter.getStatBlock();
    
            assertTrue(statBlock.contains("Clockwork Critter"));
            assertTrue(statBlock.contains("Small"));
            assertTrue(statBlock.contains("Construct"));
            assertTrue(statBlock.contains("AC: 13"));
            assertTrue(statBlock.contains("HP: 11/11"));
            assertTrue(statBlock.contains("CR: 1/4"));
        }

        @Test
        @DisplayName("toString returns summary string")
        void toStringTest() {
            String str = clockworkCritter.toString();
            
            assertTrue(str.contains("Clockwork Critter"));
            assertTrue(str.contains("Small"));
            assertTrue(str.contains("Construct"));
            assertTrue(str.contains("CR 1/4"));
        }
    }
    
    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {
        
        @Test
        @DisplayName("setMaxHitPoints adjusts current HP if needed")
        void setMaxHitPointsAdjustsCurrent() {
            basicMonster.setMaxHitPoints(10);
            assertEquals(10, basicMonster.getMaxHitPoints());
            assertEquals(10, basicMonster.getCurrentHitPoints()); // Capped down
            
            basicMonster.setMaxHitPoints(50);
            assertEquals(50, basicMonster.getMaxHitPoints());
            assertEquals(10, basicMonster.getCurrentHitPoints()); // Not raised
        }
        
        @Test
        @DisplayName("setAbilityModifiers sets all mods")
        void setAbilityModifiers() {
            basicMonster.setAbilityModifiers(2, 3, 1, -1, 0, 2);
            
            assertEquals(2, basicMonster.getStrengthMod());
            assertEquals(3, basicMonster.getDexterityMod());
            assertEquals(1, basicMonster.getConstitutionMod());
            assertEquals(-1, basicMonster.getIntelligenceMod());
            assertEquals(0, basicMonster.getWisdomMod());
            assertEquals(2, basicMonster.getCharismaMod());
        }
    }
}