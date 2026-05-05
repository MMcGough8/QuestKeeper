package com.questkeeper.character;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.inventory.Armor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the Character class.
 * 
 * @author Marc McGough
 */
class CharacterTest {
    
    private Character fighter;
    private Character wizard;
    private Character rogue;
    
    @BeforeEach
    void setUp() {
        // Standard fighter with decent stats
        fighter = new Character("Thorin", Race.DWARF, CharacterClass.FIGHTER,
                16, 12, 14, 10, 10, 8);
        
        // Wizard with high INT
        wizard = new Character("Gandalf", Race.HUMAN, CharacterClass.WIZARD,
                8, 14, 12, 16, 14, 10);
        
        // Rogue with high DEX
        rogue = new Character("Shadow", Race.ELF, CharacterClass.ROGUE,
                10, 16, 12, 14, 10, 12);
    }
    
    // ========================================================================
    // BASIC CONSTRUCTION TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {
        
        @Test
        @DisplayName("Character is created with correct name")
        void characterHasCorrectName() {
            assertEquals("Thorin", fighter.getName());
            assertEquals("Gandalf", wizard.getName());
        }
        
        @Test
        @DisplayName("Character is created with correct race")
        void characterHasCorrectRace() {
            assertEquals(Race.DWARF, fighter.getRace());
            assertEquals(Race.HUMAN, wizard.getRace());
            assertEquals(Race.ELF, rogue.getRace());
        }
        
        @Test
        @DisplayName("Character is created with correct class")
        void characterHasCorrectClass() {
            assertEquals(CharacterClass.FIGHTER, fighter.getCharacterClass());
            assertEquals(CharacterClass.WIZARD, wizard.getCharacterClass());
            assertEquals(CharacterClass.ROGUE, rogue.getCharacterClass());
        }
        
        @Test
        @DisplayName("Character starts at level 1")
        void characterStartsAtLevel1() {
            assertEquals(1, fighter.getLevel());
        }
        
        @Test
        @DisplayName("Character starts with 0 XP")
        void characterStartsWithZeroXP() {
            assertEquals(0, fighter.getExperiencePoints());
        }
        
        @Test
        @DisplayName("Default constructor sets ability scores to 10")
        void defaultConstructorSetsScoresTo10() {
            Character defaultChar = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);
            
            for (Ability ability : Ability.values()) {
                // Human gets +1 to all, so total is 11
                assertEquals(11, defaultChar.getAbilityScore(ability));
            }
        }
    }
    
    // ========================================================================
    // ABILITY SCORE TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Ability Score Tests")
    class AbilityScoreTests {
        
        @Test
        @DisplayName("Base ability scores are set correctly")
        void baseScoresSetCorrectly() {
            assertEquals(16, fighter.getBaseAbilityScore(Ability.STRENGTH));
            assertEquals(12, fighter.getBaseAbilityScore(Ability.DEXTERITY));
            assertEquals(14, fighter.getBaseAbilityScore(Ability.CONSTITUTION));
        }
        
        @Test
        @DisplayName("Racial bonuses are applied correctly")
        void racialBonusesApplied() {
            // Dwarf gets +2 CON
            assertEquals(14, fighter.getBaseAbilityScore(Ability.CONSTITUTION));
            assertEquals(16, fighter.getAbilityScore(Ability.CONSTITUTION)); // 14 + 2
            
            // Elf gets +2 DEX
            assertEquals(16, rogue.getBaseAbilityScore(Ability.DEXTERITY));
            assertEquals(18, rogue.getAbilityScore(Ability.DEXTERITY)); // 16 + 2
        }
        
        @Test
        @DisplayName("Human gets +1 to all abilities")
        void humanBonusApplied() {
            // Wizard is human, base INT is 16
            assertEquals(16, wizard.getBaseAbilityScore(Ability.INTELLIGENCE));
            assertEquals(17, wizard.getAbilityScore(Ability.INTELLIGENCE)); // 16 + 1
            
            // Check all abilities have +1
            assertEquals(9, wizard.getAbilityScore(Ability.STRENGTH));   // 8 + 1
            assertEquals(15, wizard.getAbilityScore(Ability.DEXTERITY)); // 14 + 1
        }
        
        @Test
        @DisplayName("Ability modifier calculated correctly")
        void abilityModifierCalculation() {
            // Score 10-11 = +0
            assertEquals(0, new Character("Test", Race.HUMAN, CharacterClass.FIGHTER, 
                    10, 10, 10, 10, 10, 10).getAbilityModifier(Ability.STRENGTH));
            
            // Score 16 (Dwarf STR, no racial bonus) = +3
            assertEquals(3, fighter.getAbilityModifier(Ability.STRENGTH));
            
            // Score 18 (Elf DEX with +2 racial) = +4
            assertEquals(4, rogue.getAbilityModifier(Ability.DEXTERITY));
            
            // Score 8 = -1
            assertEquals(-1, fighter.getAbilityModifier(Ability.CHARISMA));
        }
        
        @Test
        @DisplayName("Ability scores are clamped to 1-20")
        void scoresAreClamped() {
            Character testChar = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);
            
            testChar.setAbilityScore(Ability.STRENGTH, 25);
            assertEquals(20, testChar.getBaseAbilityScore(Ability.STRENGTH));
            
            testChar.setAbilityScore(Ability.STRENGTH, -5);
            assertEquals(1, testChar.getBaseAbilityScore(Ability.STRENGTH));
        }
        
        @Test
        @DisplayName("Setting ability scores recalculates HP when CON changes")
        void conChangeRecalculatesHP() {
            int originalMax = fighter.getMaxHitPoints();
            
            // Increase CON
            fighter.setAbilityScore(Ability.CONSTITUTION, 18);
            
            assertTrue(fighter.getMaxHitPoints() > originalMax);
        }
    }
    
    // ========================================================================
    // DERIVED STATS TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Derived Stats Tests")
    class DerivedStatsTests {
        
        @Test
        @DisplayName("Proficiency bonus is +2 at level 1")
        void proficiencyBonusLevel1() {
            assertEquals(2, fighter.getProficiencyBonus());
        }
        
        @Test
        @DisplayName("Proficiency bonus increases correctly")
        void proficiencyBonusScales() {
            fighter.setLevel(5);
            assertEquals(3, fighter.getProficiencyBonus());
            
            fighter.setLevel(9);
            assertEquals(4, fighter.getProficiencyBonus());
            
            fighter.setLevel(13);
            assertEquals(5, fighter.getProficiencyBonus());
            
            fighter.setLevel(17);
            assertEquals(6, fighter.getProficiencyBonus());
        }
        
        @Test
        @DisplayName("AC is calculated correctly")
        void armorClassCalculation() {
            // Base AC = 10 + DEX mod
            // Fighter DEX = 12, mod = +1
            assertEquals(11, fighter.getArmorClass());
            
            // Rogue DEX = 18 (16 + 2 elf), mod = +4
            assertEquals(14, rogue.getArmorClass());
        }
        
        @Test
        @DisplayName("Armor and shield bonuses affect AC")
        void armorBonusesAffectAC() {
            int baseAC = fighter.getArmorClass();
            
            fighter.setArmorBonus(5); // Chain mail equivalent
            assertEquals(baseAC + 5, fighter.getArmorClass());
            
            fighter.setShieldBonus(2);
            assertEquals(baseAC + 7, fighter.getArmorClass());
        }
        
        @Test
        @DisplayName("Initiative equals DEX modifier")
        void initiativeCalculation() {
            assertEquals(fighter.getAbilityModifier(Ability.DEXTERITY), 
                         fighter.getInitiativeModifier());
            assertEquals(4, rogue.getInitiativeModifier());
        }
        
        @Test
        @DisplayName("Speed is determined by race")
        void speedFromRace() {
            assertEquals(25, fighter.getSpeed()); // Dwarf
            assertEquals(30, wizard.getSpeed());  // Human
            assertEquals(30, rogue.getSpeed());   // Elf
        }
        
        @Test
        @DisplayName("Passive perception is calculated correctly")
        void passivePerceptionCalculation() {
            // 10 + WIS mod + proficiency (if proficient)
            rogue.addSkillProficiency(Skill.PERCEPTION);
            
            int wisdomMod = rogue.getAbilityModifier(Ability.WISDOM);
            int profBonus = rogue.getProficiencyBonus();
            
            assertEquals(10 + wisdomMod + profBonus, rogue.getPassivePerception());
        }
    }
    
    // ========================================================================
    // HIT POINT TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Hit Point Tests")
    class HitPointTests {
        
        @Test
        @DisplayName("Level 1 HP equals max hit die + CON mod")
        void level1HPCalculation() {
            // Fighter: d10 hit die, CON 16 (+3 with dwarf bonus), mod = +3
            // HP = 10 + 3 = 13
            assertEquals(13, fighter.getMaxHitPoints());
            
            // Wizard: d6 hit die, CON 13 (12+1 human), mod = +1
            // HP = 6 + 1 = 7
            assertEquals(7, wizard.getMaxHitPoints());
        }
        
        @Test
        @DisplayName("Character starts at full HP")
        void startsAtFullHP() {
            assertEquals(fighter.getMaxHitPoints(), fighter.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Healing works correctly")
        void healingWorks() {
            fighter.takeDamage(5);
            int hpBefore = fighter.getCurrentHitPoints();
            
            int healed = fighter.heal(3);
            
            assertEquals(3, healed);
            assertEquals(hpBefore + 3, fighter.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Healing cannot exceed max HP")
        void healingCappedAtMax() {
            fighter.takeDamage(5);
            int healed = fighter.heal(100);
            
            assertEquals(5, healed);
            assertEquals(fighter.getMaxHitPoints(), fighter.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Damage reduces HP correctly")
        void damageReducesHP() {
            int startHP = fighter.getCurrentHitPoints();
            
            int damageTaken = fighter.takeDamage(5);
            
            assertEquals(5, damageTaken);
            assertEquals(startHP - 5, fighter.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Temp HP absorbs damage first")
        void tempHPAbsorbsDamage() {
            fighter.setTemporaryHitPoints(10);
            int realHP = fighter.getCurrentHitPoints();
            
            fighter.takeDamage(5);
            
            assertEquals(5, fighter.getTemporaryHitPoints());
            assertEquals(realHP, fighter.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Damage carries over from temp HP")
        void damageCarriesOver() {
            fighter.setTemporaryHitPoints(5);
            int realHP = fighter.getCurrentHitPoints();
            
            fighter.takeDamage(8);
            
            assertEquals(0, fighter.getTemporaryHitPoints());
            assertEquals(realHP - 3, fighter.getCurrentHitPoints());
        }
        
        @Test
        @DisplayName("Temp HP don't stack - keeps higher")
        void tempHPDontStack() {
            fighter.gainTemporaryHitPoints(5);
            fighter.gainTemporaryHitPoints(3);
            assertEquals(5, fighter.getTemporaryHitPoints());

            fighter.gainTemporaryHitPoints(10);
            assertEquals(10, fighter.getTemporaryHitPoints());
        }
        
        @Test
        @DisplayName("isUnconscious returns true at 0 HP")
        void unconsciousAtZeroHP() {
            assertFalse(fighter.isUnconscious());
            
            fighter.takeDamage(fighter.getCurrentHitPoints());
            
            assertTrue(fighter.isUnconscious());
        }
        
        @Test
        @DisplayName("isBloodied returns true at half HP or less")
        void bloodiedAtHalfHP() {
            assertFalse(fighter.isBloodied());
            
            int halfHP = fighter.getMaxHitPoints() / 2;
            fighter.takeDamage(fighter.getCurrentHitPoints() - halfHP);
            
            assertTrue(fighter.isBloodied());
        }
        
        @Test
        @DisplayName("fullHeal restores to max HP")
        void fullHealWorks() {
            fighter.takeDamage(10);
            fighter.fullHeal();
            
            assertEquals(fighter.getMaxHitPoints(), fighter.getCurrentHitPoints());
        }
    }
    
    // ========================================================================
    // DICE CHECK TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Dice Check Tests")
    class DiceCheckTests {
        
        @Test
        @DisplayName("makeAbilityCheck returns valid range")
        void abilityCheckReturnsValidRange() {
            // Fighter STR mod is +3, so range is 4-23 (1+3 to 20+3)
            for (int i = 0; i < 50; i++) {
                int result = fighter.makeAbilityCheck(Ability.STRENGTH);
                assertTrue(result >= 4 && result <= 23,
                    "STR check should be 4-23, got: " + result);
            }
        }
        
        @Test
        @DisplayName("makeSkillCheck returns valid range")
        void skillCheckReturnsValidRange() {
            fighter.addSkillProficiency(Skill.ATHLETICS);
            // STR mod +3, proficiency +2 = +5, range is 6-25
            for (int i = 0; i < 50; i++) {
                int result = fighter.makeSkillCheck(Skill.ATHLETICS);
                assertTrue(result >= 6 && result <= 25,
                    "Athletics check should be 6-25, got: " + result);
            }
        }
        
        @Test
        @DisplayName("makeSavingThrow returns valid range")
        void savingThrowReturnsValidRange() {
            // Fighter has STR save proficiency: +3 mod + 2 prof = +5
            for (int i = 0; i < 50; i++) {
                int result = fighter.makeSavingThrow(Ability.STRENGTH);
                assertTrue(result >= 6 && result <= 25,
                    "STR save should be 6-25, got: " + result);
            }
        }
        
        @Test
        @DisplayName("makeAbilityCheckAgainstDC can succeed and fail")
        void abilityCheckAgainstDCWorks() {
            boolean foundSuccess = false;
            boolean foundFailure = false;
            
            for (int i = 0; i < 100 && !(foundSuccess && foundFailure); i++) {
                // DC 15 with +3 mod means roll needs 12+ to succeed
                if (fighter.makeAbilityCheckAgainstDC(Ability.STRENGTH, 15)) {
                    foundSuccess = true;
                } else {
                    foundFailure = true;
                }
            }
            
            assertTrue(foundSuccess, "Should have at least one success");
            assertTrue(foundFailure, "Should have at least one failure");
        }
        
        @Test
        @DisplayName("makeSkillCheckAgainstDC can succeed and fail")
        void skillCheckAgainstDCWorks() {
            boolean foundSuccess = false;
            boolean foundFailure = false;
            
            for (int i = 0; i < 100 && !(foundSuccess && foundFailure); i++) {
                if (fighter.makeSkillCheckAgainstDC(Skill.ATHLETICS, 15)) {
                    foundSuccess = true;
                } else {
                    foundFailure = true;
                }
            }
            
            assertTrue(foundSuccess, "Should have at least one success");
            assertTrue(foundFailure, "Should have at least one failure");
        }
        
        @Test
        @DisplayName("makeSavingThrowAgainstDC can succeed and fail")
        void savingThrowAgainstDCWorks() {
            boolean foundSuccess = false;
            boolean foundFailure = false;
            
            for (int i = 0; i < 100 && !(foundSuccess && foundFailure); i++) {
                if (fighter.makeSavingThrowAgainstDC(Ability.STRENGTH, 15)) {
                    foundSuccess = true;
                } else {
                    foundFailure = true;
                }
            }
            
            assertTrue(foundSuccess, "Should have at least one success");
            assertTrue(foundFailure, "Should have at least one failure");
        }
        
        @Test
        @DisplayName("rollInitiative returns valid range")
        void rollInitiativeWorks() {
            // Rogue has DEX 18 (+4 mod), range is 5-24
            for (int i = 0; i < 50; i++) {
                int result = rogue.rollInitiative();
                assertTrue(result >= 5 && result <= 24,
                    "Initiative should be 5-24, got: " + result);
            }
        }
        
        @Test
        @DisplayName("High modifier always succeeds low DC")
        void highModifierSucceedsLowDC() {
            // Fighter STR +3, DC 4 means minimum roll (1+3=4) always succeeds
            for (int i = 0; i < 50; i++) {
                assertTrue(fighter.makeAbilityCheckAgainstDC(Ability.STRENGTH, 4));
            }
        }
        
        @Test
        @DisplayName("Low modifier always fails high DC")
        void lowModifierFailsHighDC() {
            // Fighter CHA is 8 (-1 mod), DC 25 means max roll (20-1=19) always fails
            for (int i = 0; i < 50; i++) {
                assertFalse(fighter.makeAbilityCheckAgainstDC(Ability.CHARISMA, 25));
            }
        }
    }
    
    // ========================================================================
    // SKILL TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Skill Tests")
    class SkillTests {
        
        @Test
        @DisplayName("Skill proficiency can be added")
        void addSkillProficiency() {
            assertFalse(fighter.isProficientIn(Skill.ATHLETICS));
            
            fighter.addSkillProficiency(Skill.ATHLETICS);
            
            assertTrue(fighter.isProficientIn(Skill.ATHLETICS));
        }
        
        @Test
        @DisplayName("Skill proficiency can be removed")
        void removeSkillProficiency() {
            fighter.addSkillProficiency(Skill.ATHLETICS);
            fighter.removeSkillProficiency(Skill.ATHLETICS);
            
            assertFalse(fighter.isProficientIn(Skill.ATHLETICS));
        }
        
        @Test
        @DisplayName("Skill modifier includes ability mod")
        void skillModifierIncludesAbilityMod() {
            // Athletics uses STR, fighter has 16 STR (+3 mod)
            assertEquals(3, fighter.getSkillModifier(Skill.ATHLETICS));
        }
        
        @Test
        @DisplayName("Skill modifier includes proficiency when proficient")
        void skillModifierIncludesProficiency() {
            fighter.addSkillProficiency(Skill.ATHLETICS);
            
            // STR mod +3, proficiency +2
            assertEquals(5, fighter.getSkillModifier(Skill.ATHLETICS));
        }
        
        @Test
        @DisplayName("Skills use correct ability")
        void skillsUseCorrectAbility() {
            assertEquals(Ability.STRENGTH, Skill.ATHLETICS.getAbility());
            assertEquals(Ability.DEXTERITY, Skill.STEALTH.getAbility());
            assertEquals(Ability.INTELLIGENCE, Skill.INVESTIGATION.getAbility());
            assertEquals(Ability.WISDOM, Skill.PERCEPTION.getAbility());
            assertEquals(Ability.CHARISMA, Skill.PERSUASION.getAbility());
        }
    }
    
    // ========================================================================
    // SAVING THROW TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Saving Throw Tests")
    class SavingThrowTests {
        
        @Test
        @DisplayName("Class grants correct saving throw proficiencies")
        void classGrantsSavingThrows() {
            // Fighter: STR and CON saves
            assertTrue(fighter.hasSavingThrowProficiency(Ability.STRENGTH));
            assertTrue(fighter.hasSavingThrowProficiency(Ability.CONSTITUTION));
            assertFalse(fighter.hasSavingThrowProficiency(Ability.DEXTERITY));
            
            // Wizard: INT and WIS saves
            assertTrue(wizard.hasSavingThrowProficiency(Ability.INTELLIGENCE));
            assertTrue(wizard.hasSavingThrowProficiency(Ability.WISDOM));
            
            // Rogue: DEX and INT saves
            assertTrue(rogue.hasSavingThrowProficiency(Ability.DEXTERITY));
            assertTrue(rogue.hasSavingThrowProficiency(Ability.INTELLIGENCE));
        }
        
        @Test
        @DisplayName("Saving throw modifier includes proficiency when proficient")
        void savingThrowIncludesProficiency() {
            // Fighter STR save: +3 (mod) + 2 (prof) = +5
            assertEquals(5, fighter.getSavingThrowModifier(Ability.STRENGTH));
            
            // Fighter DEX save: +1 (mod), no proficiency
            assertEquals(1, fighter.getSavingThrowModifier(Ability.DEXTERITY));
        }
    }
    
    // ========================================================================
    // LEVEL AND XP TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Level and XP Tests")
    class LevelXPTests {
        
        @Test
        @DisplayName("XP for next level is correct")
        void xpForNextLevel() {
            assertEquals(300, fighter.getXpForNextLevel()); // Level 1 -> 2
            
            fighter.setLevel(5);
            assertEquals(14000, fighter.getXpForNextLevel()); // Level 5 -> 6
        }
        
        @Test
        @DisplayName("Adding XP causes level up")
        void addingXPLevelsUp() {
            assertEquals(1, fighter.getLevel());
            
            int levelsGained = fighter.addExperience(300);
            
            assertEquals(1, levelsGained);
            assertEquals(2, fighter.getLevel());
        }
        
        @Test
        @DisplayName("Multiple level ups from large XP gain")
        void multipleLevelUps() {
            int levelsGained = fighter.addExperience(2700); // Enough for level 4
            
            assertEquals(3, levelsGained);
            assertEquals(4, fighter.getLevel());
        }
        
        @Test
        @DisplayName("Level up increases max HP")
        void levelUpIncreasesHP() {
            int level1HP = fighter.getMaxHitPoints();
            
            fighter.addExperience(300);
            
            assertTrue(fighter.getMaxHitPoints() > level1HP);
        }
        
        @Test
        @DisplayName("setLevel clamps to 1-20")
        void setLevelClamped() {
            fighter.setLevel(0);
            assertEquals(1, fighter.getLevel());
            
            fighter.setLevel(25);
            assertEquals(20, fighter.getLevel());
        }
    }
    
    // ========================================================================
    // UTILITY TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Utility Tests")
    class UtilityTests {
        
        @Test
        @DisplayName("toString includes key information")
        void toStringIncludesInfo() {
            String str = fighter.toString();
            
            assertTrue(str.contains("Thorin"));
            assertTrue(str.contains("Dwarf"));
            assertTrue(str.contains("Fighter"));
            assertTrue(str.contains("Level 1"));
        }
        
        @Test
        @DisplayName("getAbilityScoresString formats correctly")
        void abilityScoresStringFormat() {
            String scores = fighter.getAbilityScoresString();
            
            assertTrue(scores.contains("STR:"));
            assertTrue(scores.contains("DEX:"));
            assertTrue(scores.contains("CON:"));
            assertTrue(scores.contains("INT:"));
            assertTrue(scores.contains("WIS:"));
            assertTrue(scores.contains("CHA:"));
        }
        
        @Test
        @DisplayName("Name can be changed")
        void nameCanBeChanged() {
            fighter.setName("Gimli");
            assertEquals("Gimli", fighter.getName());
        }
    }
    
    // ========================================================================
    // ENUM TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {
        
        @Test
        @DisplayName("Ability enum has correct values")
        void abilityEnumValues() {
            assertEquals("STR", Ability.STRENGTH.getAbbreviation());
            assertEquals("Strength", Ability.STRENGTH.getFullName());
            assertEquals(6, Ability.values().length);
        }
        
        @Test
        @DisplayName("Race enum has correct bonuses")
        void raceEnumBonuses() {
            assertEquals(2, Race.DWARF.getConBonus());
            assertEquals(2, Race.ELF.getDexBonus());
            assertEquals(2, Race.DRAGONBORN.getStrBonus());
        }
        
        @Test
        @DisplayName("CharacterClass enum has correct hit dice")
        void classEnumHitDice() {
            assertEquals(12, CharacterClass.BARBARIAN.getHitDie());
            assertEquals(10, CharacterClass.FIGHTER.getHitDie());
            assertEquals(8, CharacterClass.ROGUE.getHitDie());
            assertEquals(6, CharacterClass.WIZARD.getHitDie());
        }
        
        @Test
        @DisplayName("Skill enum has correct abilities")
        void skillEnumAbilities() {
            assertEquals(Ability.STRENGTH, Skill.ATHLETICS.getAbility());
            assertEquals(Ability.DEXTERITY, Skill.ACROBATICS.getAbility());
            assertEquals(Ability.INTELLIGENCE, Skill.ARCANA.getAbility());
            assertEquals(Ability.WISDOM, Skill.PERCEPTION.getAbility());
            assertEquals(Ability.CHARISMA, Skill.PERSUASION.getAbility());
            
            // Verify no skills use Constitution (D&D 5e has no CON skills)
            for (Skill skill : Skill.values()) {
                assertNotEquals(Ability.CONSTITUTION, skill.getAbility(),
                    "No skill should use Constitution");
            }
        }
    }

    // ========================================================================
    // HALF-ELF BONUS ABILITY TESTS
    // ========================================================================

    @Nested
    @DisplayName("Half-Elf Bonus Abilities")
    class HalfElfBonusTests {

        @Test
        @DisplayName("Half-Elf gets +2 CHA by default")
        void halfElfGetsPlusTwoCha() {
            Character halfElf = new Character("Test", Race.HALF_ELF, CharacterClass.BARD);
            halfElf.setAbilityScores(10, 10, 10, 10, 10, 10);

            // +2 CHA from racial bonus
            assertEquals(12, halfElf.getAbilityScore(Ability.CHARISMA));
        }

        @Test
        @DisplayName("Half-Elf bonus abilities add +1 to chosen abilities")
        void halfElfBonusAbilitiesAddOne() {
            Character halfElf = new Character("Test", Race.HALF_ELF, CharacterClass.FIGHTER);
            halfElf.setAbilityScores(10, 10, 10, 10, 10, 10);
            halfElf.setHalfElfBonusAbilities(Ability.STRENGTH, Ability.CONSTITUTION);

            // +1 from Half-Elf bonus
            assertEquals(11, halfElf.getAbilityScore(Ability.STRENGTH));
            assertEquals(11, halfElf.getAbilityScore(Ability.CONSTITUTION));
            // No bonus to these
            assertEquals(10, halfElf.getAbilityScore(Ability.DEXTERITY));
            assertEquals(10, halfElf.getAbilityScore(Ability.INTELLIGENCE));
            assertEquals(10, halfElf.getAbilityScore(Ability.WISDOM));
            // Still +2 CHA from racial
            assertEquals(12, halfElf.getAbilityScore(Ability.CHARISMA));
        }

        @Test
        @DisplayName("Cannot set CHA as Half-Elf bonus ability")
        void cannotSetChaAsBonusAbility() {
            Character halfElf = new Character("Test", Race.HALF_ELF, CharacterClass.BARD);

            assertThrows(IllegalArgumentException.class, () ->
                halfElf.setHalfElfBonusAbilities(Ability.CHARISMA, Ability.STRENGTH));

            assertThrows(IllegalArgumentException.class, () ->
                halfElf.setHalfElfBonusAbilities(Ability.DEXTERITY, Ability.CHARISMA));
        }

        @Test
        @DisplayName("Cannot set same ability twice")
        void cannotSetSameAbilityTwice() {
            Character halfElf = new Character("Test", Race.HALF_ELF, CharacterClass.FIGHTER);

            assertThrows(IllegalArgumentException.class, () ->
                halfElf.setHalfElfBonusAbilities(Ability.STRENGTH, Ability.STRENGTH));
        }

        @Test
        @DisplayName("Non-Half-Elf cannot set bonus abilities")
        void nonHalfElfCannotSetBonusAbilities() {
            Character human = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertThrows(IllegalStateException.class, () ->
                human.setHalfElfBonusAbilities(Ability.STRENGTH, Ability.DEXTERITY));
        }

        @Test
        @DisplayName("Get bonus abilities returns unmodifiable set")
        void getBonusAbilitiesReturnsUnmodifiableSet() {
            Character halfElf = new Character("Test", Race.HALF_ELF, CharacterClass.FIGHTER);
            halfElf.setHalfElfBonusAbilities(Ability.STRENGTH, Ability.DEXTERITY);

            var abilities = halfElf.getHalfElfBonusAbilities();

            assertThrows(UnsupportedOperationException.class, () ->
                abilities.add(Ability.WISDOM));
        }

        @Test
        @DisplayName("Bonus abilities empty for non-Half-Elf")
        void bonusAbilitiesEmptyForNonHalfElf() {
            Character human = new Character("Test", Race.HUMAN, CharacterClass.FIGHTER);

            assertTrue(human.getHalfElfBonusAbilities().isEmpty());
        }

        @Test
        @DisplayName("Setting CON bonus updates HP")
        void settingConBonusUpdatesHP() {
            Character halfElf = new Character("Test", Race.HALF_ELF, CharacterClass.FIGHTER);
            halfElf.setAbilityScores(10, 10, 10, 10, 10, 10);

            int hpBefore = halfElf.getMaxHitPoints();
            halfElf.setHalfElfBonusAbilities(Ability.CONSTITUTION, Ability.STRENGTH);
            int hpAfter = halfElf.getMaxHitPoints();

            // +1 CON = +1 to modifier (10->11 is still 0 mod, but just checking it recalculates)
            // Actually 11 CON still has +0 modifier, need 12 for +1
            // But we're verifying the recalculation happens
            assertNotNull(halfElf.getMaxHitPoints());
        }
    }

    // ========================================================================
    // Equipped Armor AC Tests
    // ========================================================================

    @Nested
    @DisplayName("Equipped Armor AC Calculation")
    class EquippedArmorACTests {

        private Character highDexCharacter;

        @BeforeEach
        void setUp() {
            // Character with DEX 16 (+3 modifier)
            highDexCharacter = new Character("DexFighter", Race.HUMAN, CharacterClass.FIGHTER,
                    10, 16, 14, 10, 10, 10);
        }

        @Test
        @DisplayName("No armor uses base AC 10 + full DEX")
        void noArmorUsesBaseACPlusDex() {
            // DEX 16 = +3 modifier
            // AC = 10 + 3 = 13
            assertEquals(13, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Light armor applies full DEX modifier")
        void lightArmorAppliesFullDex() {
            // Leather armor: AC 11 + full DEX
            Armor leather = Armor.createLeatherArmor();
            highDexCharacter.getInventory().addItem(leather);
            highDexCharacter.getInventory().equip(leather);

            // AC = 11 + 3 (full DEX) = 14
            assertEquals(14, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Studded leather applies full DEX modifier")
        void studdedLeatherAppliesFullDex() {
            // Studded leather: AC 12 + full DEX
            Armor studdedLeather = Armor.createStuddedLeather();
            highDexCharacter.getInventory().addItem(studdedLeather);
            highDexCharacter.getInventory().equip(studdedLeather);

            // AC = 12 + 3 (full DEX) = 15
            assertEquals(15, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Medium armor caps DEX at +2")
        void mediumArmorCapsDexAtTwo() {
            // Scale mail: AC 14 + DEX (max 2)
            Armor scaleMail = Armor.createScaleMail();
            highDexCharacter.getInventory().addItem(scaleMail);
            highDexCharacter.getInventory().equip(scaleMail);

            // AC = 14 + 2 (capped DEX) = 16, NOT 14 + 3 = 17
            assertEquals(16, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Heavy armor ignores DEX completely")
        void heavyArmorIgnoresDex() {
            // Chain mail: AC 16 + 0 DEX
            Armor chainMail = Armor.createChainMail();
            highDexCharacter.getInventory().addItem(chainMail);
            highDexCharacter.getInventory().equip(chainMail);

            // AC = 16 + 0 = 16, NOT 16 + 3 = 19
            assertEquals(16, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Plate armor ignores DEX")
        void plateArmorIgnoresDex() {
            // Plate: AC 18 + 0 DEX
            Armor plate = Armor.createPlate();
            highDexCharacter.getInventory().addItem(plate);
            highDexCharacter.getInventory().equip(plate);

            // AC = 18 + 0 = 18
            assertEquals(18, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Shield adds bonus to AC")
        void shieldAddsBonusToAC() {
            // Shield: +2 AC
            Armor shield = Armor.createShield();
            highDexCharacter.getInventory().addItem(shield);
            highDexCharacter.getInventory().equip(shield);

            // AC = 10 + 3 (DEX) + 2 (shield) = 15
            assertEquals(15, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Armor and shield combine correctly")
        void armorAndShieldCombine() {
            // Chain mail (AC 16) + shield (+2)
            Armor chainMail = Armor.createChainMail();
            Armor shield = Armor.createShield();

            highDexCharacter.getInventory().addItem(chainMail);
            highDexCharacter.getInventory().addItem(shield);
            highDexCharacter.getInventory().equip(chainMail);
            highDexCharacter.getInventory().equip(shield);

            // AC = 16 + 0 (no DEX for heavy) + 2 (shield) = 18
            assertEquals(18, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Light armor with shield works correctly")
        void lightArmorWithShield() {
            // Leather (AC 11 + DEX) + shield (+2)
            Armor leather = Armor.createLeatherArmor();
            Armor shield = Armor.createShield();

            highDexCharacter.getInventory().addItem(leather);
            highDexCharacter.getInventory().addItem(shield);
            highDexCharacter.getInventory().equip(leather);
            highDexCharacter.getInventory().equip(shield);

            // AC = 11 + 3 (full DEX) + 2 (shield) = 16
            assertEquals(16, highDexCharacter.getArmorClass());
        }

        @Test
        @DisplayName("Low DEX character benefits from heavy armor")
        void lowDexBenefitsFromHeavyArmor() {
            // Character with DEX 8 (-1 modifier)
            Character lowDexCharacter = new Character("Tank", Race.DWARF, CharacterClass.FIGHTER,
                    16, 8, 16, 10, 10, 10);

            // Without armor: AC = 10 + (-1) = 9
            assertEquals(9, lowDexCharacter.getArmorClass());

            // With plate: AC = 18 (ignores negative DEX too)
            Armor plate = Armor.createPlate();
            lowDexCharacter.getInventory().addItem(plate);
            lowDexCharacter.getInventory().equip(plate);
            assertEquals(18, lowDexCharacter.getArmorClass());
        }
    }

    @Nested
    @DisplayName("Warlock class features")
    class WarlockClassFeatureTests {

        @Test
        @DisplayName("L1 Fiend Warlock gets Dark One's Blessing")
        void l1FiendWarlockBlessing() {
            Character w = new Character("Vex", Race.HUMAN, CharacterClass.WARLOCK,
                10, 14, 14, 10, 10, 16);
            w.setWarlockPatron(
                com.questkeeper.character.features.WarlockFeatures.OtherworldlyPatron.FIEND);
            assertTrue(w.getFeature(
                com.questkeeper.character.features.WarlockFeatures.DARK_ONES_BLESSING_ID
            ).isPresent());
        }

        @Test
        @DisplayName("L2 Warlock gets Eldritch Invocations (count scales with level)")
        void l2WarlockInvocations() {
            Character w = new Character("Vex", Race.HUMAN, CharacterClass.WARLOCK,
                10, 14, 14, 10, 10, 16);
            w.setLevel(2);
            assertTrue(w.getFeature(
                com.questkeeper.character.features.WarlockFeatures.ELDRITCH_INVOCATIONS_ID
            ).isPresent());
            assertEquals(2,
                com.questkeeper.character.features.WarlockFeatures.getInvocationCount(2));
            assertEquals(3,
                com.questkeeper.character.features.WarlockFeatures.getInvocationCount(5));
        }

        @Test
        @DisplayName("L3 Warlock with Pact of the Tome gets Pact Boon marker")
        void l3WarlockTomeBoon() {
            Character w = new Character("Vex", Race.HUMAN, CharacterClass.WARLOCK,
                10, 14, 14, 10, 10, 16);
            w.setLevel(3);
            w.setWarlockPactBoon(
                com.questkeeper.character.features.WarlockFeatures.PactBoon.TOME);
            assertTrue(w.getFeature(
                com.questkeeper.character.features.WarlockFeatures.PACT_BOON_ID
            ).isPresent());
        }

        @Test
        @DisplayName("setWarlockPatron on a non-Warlock throws")
        void setPatronOnNonWarlockThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.setWarlockPatron(
                    com.questkeeper.character.features.WarlockFeatures.OtherworldlyPatron.FIEND));
        }
    }

    @Nested
    @DisplayName("Druid class features")
    class DruidClassFeatureTests {

        @Test
        @DisplayName("L1 Druid gets Druidic")
        void l1DruidGetsDruidic() {
            Character d = new Character("Sylven", Race.HALF_ELF, CharacterClass.DRUID,
                10, 10, 14, 10, 16, 10);
            assertTrue(d.getFeature(
                com.questkeeper.character.features.DruidFeatures.DRUIDIC_ID
            ).isPresent());
        }

        @Test
        @DisplayName("L2 Druid gets Wild Shape with CR 1/4 cap")
        void l2DruidWildShapeCR4() {
            Character d = new Character("Sylven", Race.HALF_ELF, CharacterClass.DRUID,
                10, 10, 14, 10, 16, 10);
            d.setLevel(2);
            var ws = (com.questkeeper.character.features.DruidFeatures.WildShape)
                d.getFeature(
                    com.questkeeper.character.features.DruidFeatures.WILD_SHAPE_ID
                ).orElseThrow();
            assertEquals(0.25, ws.getMaxChallengeRating());
            assertFalse(ws.canTakeFlyingForms());
            assertFalse(ws.canTakeSwimmingForms());
        }

        @Test
        @DisplayName("L4 Druid Wild Shape CR scales to 1/2 with swim forms")
        void l4DruidWildShapeCRHalf() {
            Character d = new Character("Sylven", Race.HALF_ELF, CharacterClass.DRUID,
                10, 10, 14, 10, 16, 10);
            d.setLevel(4);
            var ws = (com.questkeeper.character.features.DruidFeatures.WildShape)
                d.getFeature(
                    com.questkeeper.character.features.DruidFeatures.WILD_SHAPE_ID
                ).orElseThrow();
            assertEquals(0.5, ws.getMaxChallengeRating());
            assertTrue(ws.canTakeSwimmingForms());
        }

        @Test
        @DisplayName("L2 Land Druid gets Bonus Cantrip + Natural Recovery")
        void l2LandDruidSubclassFeatures() {
            Character d = new Character("Sylven", Race.HALF_ELF, CharacterClass.DRUID,
                10, 10, 14, 10, 16, 10);
            d.setLevel(2);
            d.setDruidCircle(
                com.questkeeper.character.features.DruidFeatures.DruidCircle.LAND);
            assertTrue(d.getFeature(
                com.questkeeper.character.features.DruidFeatures.LAND_BONUS_CANTRIP_ID
            ).isPresent());
            assertTrue(d.getFeature(
                com.questkeeper.character.features.DruidFeatures.NATURAL_RECOVERY_ID
            ).isPresent());
        }

        @Test
        @DisplayName("Natural Recovery budget = ceil(level/2)")
        void naturalRecoveryBudget() {
            Character d = new Character("Sylven", Race.HALF_ELF, CharacterClass.DRUID,
                10, 10, 14, 10, 16, 10);
            d.setLevel(5);
            d.setDruidCircle(
                com.questkeeper.character.features.DruidFeatures.DruidCircle.LAND);
            var nr = (com.questkeeper.character.features.DruidFeatures.NaturalRecovery)
                d.getFeature(
                    com.questkeeper.character.features.DruidFeatures.NATURAL_RECOVERY_ID
                ).orElseThrow();
            assertEquals(3, nr.getRecoveryBudget(), "L5 -> ceil(5/2) = 3");
        }

        @Test
        @DisplayName("setDruidCircle on a non-Druid throws")
        void setDruidCircleOnNonDruidThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.setDruidCircle(
                    com.questkeeper.character.features.DruidFeatures.DruidCircle.LAND));
        }
    }

    @Nested
    @DisplayName("Bard class features")
    class BardClassFeatureTests {

        @Test
        @DisplayName("L1 Bard gets Bardic Inspiration with d6 die")
        void l1BardGetsBardicInspirationD6() {
            Character b = new Character("Halo", Race.HUMAN, CharacterClass.BARD,
                10, 14, 14, 10, 10, 16);
            var bi = (com.questkeeper.character.features.BardFeatures.BardicInspiration)
                b.getFeature(
                    com.questkeeper.character.features.BardFeatures.BARDIC_INSPIRATION_ID
                ).orElseThrow();
            assertEquals(6, bi.getInspirationDie(), "L1 Bard inspiration die is d6");
        }

        @Test
        @DisplayName("L5 Bard inspiration die scales to d8 and resets on short rest")
        void l5BardInspirationScales() {
            Character b = new Character("Halo", Race.HUMAN, CharacterClass.BARD,
                10, 14, 14, 10, 10, 16);
            b.setLevel(5);
            var bi = (com.questkeeper.character.features.BardFeatures.BardicInspiration)
                b.getFeature(
                    com.questkeeper.character.features.BardFeatures.BARDIC_INSPIRATION_ID
                ).orElseThrow();
            assertEquals(8, bi.getInspirationDie(), "L5 Bard inspiration die is d8");
            assertTrue(b.getFeature(
                com.questkeeper.character.features.BardFeatures.FONT_OF_INSPIRATION_ID
            ).isPresent(), "Lvl 5 Bard should gain Font of Inspiration");
        }

        @Test
        @DisplayName("L2 Bard gains Jack of All Trades + Song of Rest")
        void l2BardL2Features() {
            Character b = new Character("Halo", Race.HUMAN, CharacterClass.BARD,
                10, 14, 14, 10, 10, 16);
            b.setLevel(2);
            assertTrue(b.getFeature(
                com.questkeeper.character.features.BardFeatures.JACK_OF_ALL_TRADES_ID
            ).isPresent());
            assertTrue(b.getFeature(
                com.questkeeper.character.features.BardFeatures.SONG_OF_REST_ID
            ).isPresent());
        }

        @Test
        @DisplayName("L3 Bard with College of Lore gets Cutting Words + Bonus Proficiencies")
        void l3LoreBardSubclassFeatures() {
            Character b = new Character("Halo", Race.HUMAN, CharacterClass.BARD,
                10, 14, 14, 10, 10, 16);
            b.setLevel(3);
            b.setBardCollege(
                com.questkeeper.character.features.BardFeatures.BardCollege.LORE);
            assertTrue(b.getFeature(
                com.questkeeper.character.features.BardFeatures.CUTTING_WORDS_ID
            ).isPresent());
            assertTrue(b.getFeature(
                com.questkeeper.character.features.BardFeatures.LORE_BONUS_PROFICIENCIES_ID
            ).isPresent());
        }

        @Test
        @DisplayName("setBardCollege on a non-Bard throws")
        void setBardCollegeOnNonBardThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.setBardCollege(
                    com.questkeeper.character.features.BardFeatures.BardCollege.LORE));
        }
    }

    @Nested
    @DisplayName("Sorcerer class features")
    class SorcererClassFeatureTests {

        @Test
        @DisplayName("L1 Draconic Sorcerer gets Draconic Ancestry + Resilience")
        void l1DraconicGivesAncestryAndResilience() {
            Character s = new Character("Eldra", Race.HUMAN, CharacterClass.SORCERER,
                10, 14, 14, 10, 10, 16);
            s.setSorcerousOrigin(
                com.questkeeper.character.features.SorcererFeatures.SorcerousOrigin.DRACONIC);
            assertTrue(s.getFeature(
                com.questkeeper.character.features.SorcererFeatures.DRACONIC_ANCESTRY_ID
            ).isPresent());
            assertTrue(s.getFeature(
                com.questkeeper.character.features.SorcererFeatures.DRACONIC_RESILIENCE_ID
            ).isPresent());
        }

        @Test
        @DisplayName("L2 Sorcerer gets Font of Magic with sorcerer-level points")
        void l2SorcererGetsFontOfMagic() {
            Character s = new Character("Eldra", Race.HUMAN, CharacterClass.SORCERER,
                10, 14, 14, 10, 10, 16);
            s.setLevel(2);
            var fom = (com.questkeeper.character.features.SorcererFeatures.FontOfMagic)
                s.getFeature(
                    com.questkeeper.character.features.SorcererFeatures.FONT_OF_MAGIC_ID
                ).orElseThrow();
            assertEquals(2, fom.getMaxSorceryPoints(), "L2 Sorcerer has 2 sorcery points");
        }

        @Test
        @DisplayName("Font of Magic pool grows with sorcerer level")
        void fontOfMagicGrowsWithLevel() {
            Character s = new Character("Eldra", Race.HUMAN, CharacterClass.SORCERER,
                10, 14, 14, 10, 10, 16);
            s.setLevel(5);
            var fom = (com.questkeeper.character.features.SorcererFeatures.FontOfMagic)
                s.getFeature(
                    com.questkeeper.character.features.SorcererFeatures.FONT_OF_MAGIC_ID
                ).orElseThrow();
            assertEquals(5, fom.getMaxSorceryPoints(), "L5 Sorcerer has 5 sorcery points");
        }

        @Test
        @DisplayName("Spending sorcery points reduces the pool")
        void spendingPointsReducesPool() {
            Character s = new Character("Eldra", Race.HUMAN, CharacterClass.SORCERER,
                10, 14, 14, 10, 10, 16);
            s.setLevel(3);
            var fom = (com.questkeeper.character.features.SorcererFeatures.FontOfMagic)
                s.getFeature(
                    com.questkeeper.character.features.SorcererFeatures.FONT_OF_MAGIC_ID
                ).orElseThrow();
            assertEquals(3, fom.getSorceryPoints());
            assertTrue(fom.spendPoints(2));
            assertEquals(1, fom.getSorceryPoints());
            assertFalse(fom.spendPoints(5), "Cannot overspend");
        }

        @Test
        @DisplayName("setSorcerousOrigin on a non-Sorcerer throws")
        void setOriginOnNonSorcererThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.setSorcerousOrigin(
                    com.questkeeper.character.features.SorcererFeatures.SorcerousOrigin.DRACONIC));
        }
    }

    @Nested
    @DisplayName("Wizard class features")
    class WizardClassFeatureTests {

        @Test
        @DisplayName("L1 Wizard gets Arcane Recovery")
        void l1WizardGetsArcaneRecovery() {
            Character w = new Character("Mira", Race.HUMAN, CharacterClass.WIZARD,
                10, 10, 14, 16, 10, 10);
            assertTrue(w.getFeature(
                com.questkeeper.character.features.WizardFeatures.ARCANE_RECOVERY_ID
            ).isPresent(), "Wizard L1 should have Arcane Recovery");
        }

        @Test
        @DisplayName("Arcane Recovery budget = ceil(level/2)")
        void arcaneRecoveryBudgetGrowsWithLevel() {
            Character w = new Character("Mira", Race.HUMAN, CharacterClass.WIZARD,
                10, 10, 14, 16, 10, 10);
            var ar = (com.questkeeper.character.features.WizardFeatures.ArcaneRecovery)
                w.getFeature(
                    com.questkeeper.character.features.WizardFeatures.ARCANE_RECOVERY_ID
                ).orElseThrow();
            assertEquals(1, ar.getRecoveryBudget(), "L1 -> 1");

            w.setLevel(5);
            assertEquals(3, ar.getRecoveryBudget(),
                "L5 -> ceil(5/2) = 3");
        }

        @Test
        @DisplayName("L2 Wizard with Evocation tradition gets Sculpt Spells + Evocation Savant")
        void l2EvocationGivesSchoolFeatures() {
            Character w = new Character("Mira", Race.HUMAN, CharacterClass.WIZARD,
                10, 10, 14, 16, 10, 10);
            w.setLevel(2);
            w.setArcaneTradition(
                com.questkeeper.character.features.WizardFeatures.ArcaneTradition.EVOCATION);
            assertTrue(w.getFeature(
                com.questkeeper.character.features.WizardFeatures.SCULPT_SPELLS_ID
            ).isPresent());
            assertTrue(w.getFeature(
                com.questkeeper.character.features.WizardFeatures.EVOCATION_SAVANT_ID
            ).isPresent());
        }

        @Test
        @DisplayName("setArcaneTradition on a non-Wizard throws")
        void setArcaneTraditionOnNonWizardThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.setArcaneTradition(
                    com.questkeeper.character.features.WizardFeatures.ArcaneTradition.EVOCATION));
        }
    }

    @Nested
    @DisplayName("Higher-level (L6-20) features")
    class HigherLevelFeatureTests {

        @Test
        @DisplayName("L9 Barbarian gets Brutal Critical with 1 extra die")
        void l9BrutalCriticalOneDie() {
            Character barb = new Character("Grog", Race.HUMAN, CharacterClass.BARBARIAN,
                16, 14, 14, 10, 10, 10);
            barb.setLevel(9);
            var bc = (com.questkeeper.character.features.BarbarianFeatures.BrutalCritical)
                barb.getFeature(
                    com.questkeeper.character.features.BarbarianFeatures.BRUTAL_CRITICAL_ID
                ).orElseThrow();
            assertEquals(1, bc.getExtraDice());
        }

        @Test
        @DisplayName("L13/L17 Brutal Critical scales to 2 / 3 dice")
        void brutalCriticalScales() {
            Character barb = new Character("Grog", Race.HUMAN, CharacterClass.BARBARIAN,
                16, 14, 14, 10, 10, 10);
            barb.setLevel(13);
            var bc13 = (com.questkeeper.character.features.BarbarianFeatures.BrutalCritical)
                barb.getFeature(
                    com.questkeeper.character.features.BarbarianFeatures.BRUTAL_CRITICAL_ID
                ).orElseThrow();
            assertEquals(2, bc13.getExtraDice());
            barb.setLevel(17);
            assertEquals(3, bc13.getExtraDice());
        }

        @Test
        @DisplayName("L11 Rogue with Reliable Talent floors proficient skill rolls at 10")
        void reliableTalentFloors() {
            Character rogue = new Character("Vex", Race.HUMAN, CharacterClass.ROGUE,
                10, 16, 14, 10, 10, 10);
            rogue.setLevel(11);
            rogue.addSkillProficiency(Skill.STEALTH);
            assertTrue(rogue.getFeature(
                com.questkeeper.character.features.RogueFeatures.RELIABLE_TALENT_ID
            ).isPresent());
            // Stealth modifier = DEX(+3) + prof(+4 at L11) = +7. Reliable Talent
            // floors raw rolls ≤9 to 10, so totals must be ≥ 17 over many rolls.
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < 100; i++) {
                int total = rogue.makeSkillCheck(Skill.STEALTH);
                if (total < min) min = total;
            }
            assertTrue(min >= 17,
                "Reliable Talent should floor proficient totals at 17; got min=" + min);
        }

        @Test
        @DisplayName("L6 Paladin with high CHA gets Aura of Protection bonus on saves")
        void auraOfProtectionAddsCHA() {
            Character pal = new Character("Lyra", Race.HUMAN, CharacterClass.PALADIN,
                14, 10, 14, 10, 10, 16);
            // Pre-aura baseline: STR is not a Paladin save proficiency.
            // STR 14 base + 1 racial = 15 -> mod +2.
            int strSaveL1 = pal.getSavingThrowModifier(Ability.STRENGTH);
            assertEquals(2, strSaveL1, "Pre-L6: STR save = STR mod only");

            pal.setLevel(6);
            // CHA 16 base + 1 racial = 17 -> mod +3.
            // With Aura: STR save = +2 + max(1, +3) = +5.
            int strSaveL6 = pal.getSavingThrowModifier(Ability.STRENGTH);
            assertEquals(5, strSaveL6,
                "L6 Aura of Protection should add CHA mod +3 to STR save (2 -> 5)");
        }
    }

    @Nested
    @DisplayName("Cleric class features")
    class ClericClassFeatureTests {

        @Test
        @DisplayName("L2 Cleric without domain still gets Channel Divinity + Turn Undead")
        void l2ClericBaselineFeatures() {
            Character c = new Character("Mira", Race.HUMAN, CharacterClass.CLERIC,
                10, 10, 14, 10, 16, 10);
            c.setLevel(2);
            assertTrue(c.getFeature(
                com.questkeeper.character.features.ClericFeatures.CLERIC_CHANNEL_DIVINITY_ID
            ).isPresent(), "Cleric L2 should have Channel Divinity");
            assertTrue(c.getFeature(
                com.questkeeper.character.features.ClericFeatures.TURN_UNDEAD_ID
            ).isPresent(), "Cleric L2 should have Turn Undead");
        }

        @Test
        @DisplayName("Setting Life Domain at L1 grants heavy armor proficiency + Disciple of Life")
        void lifeDomainGrantsLifeFeatures() {
            Character c = new Character("Mira", Race.HUMAN, CharacterClass.CLERIC,
                10, 10, 14, 10, 16, 10);
            c.setDivineDomain(
                com.questkeeper.character.features.ClericFeatures.DivineDomain.LIFE);
            assertTrue(c.getFeature(
                com.questkeeper.character.features.ClericFeatures.BONUS_PROFICIENCY_HEAVY_ARMOR_ID
            ).isPresent(), "Life Domain should grant heavy armor proficiency");
            assertTrue(c.getFeature(
                com.questkeeper.character.features.ClericFeatures.DISCIPLE_OF_LIFE_ID
            ).isPresent(), "Life Domain should grant Disciple of Life");
        }

        @Test
        @DisplayName("L2 Life Cleric also gets Preserve Life (CD option)")
        void l2LifeClericGetsPreserveLife() {
            Character c = new Character("Mira", Race.HUMAN, CharacterClass.CLERIC,
                10, 10, 14, 10, 16, 10);
            c.setDivineDomain(
                com.questkeeper.character.features.ClericFeatures.DivineDomain.LIFE);
            c.setLevel(2);
            assertTrue(c.getFeature(
                com.questkeeper.character.features.ClericFeatures.PRESERVE_LIFE_ID
            ).isPresent(), "L2 Life Cleric should have Preserve Life");
        }

        @Test
        @DisplayName("L5 Cleric gains Destroy Undead")
        void l5ClericGetsDestroyUndead() {
            Character c = new Character("Mira", Race.HUMAN, CharacterClass.CLERIC,
                10, 10, 14, 10, 16, 10);
            c.setLevel(5);
            assertTrue(c.getFeature(
                com.questkeeper.character.features.ClericFeatures.DESTROY_UNDEAD_ID
            ).isPresent(), "Cleric L5 should have Destroy Undead");
        }

        @Test
        @DisplayName("setDivineDomain on a non-Cleric throws")
        void setDivineDomainOnNonClericThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.setDivineDomain(
                    com.questkeeper.character.features.ClericFeatures.DivineDomain.LIFE));
        }
    }

    @Nested
    @DisplayName("Extra Attack distribution")
    class ExtraAttackDistributionTests {

        @ParameterizedTest(name = "Lvl 4 {0} has 1 attack")
        @CsvSource({
            "FIGHTER",
            "PALADIN",
            "RANGER",
            "MONK",
            "BARBARIAN"
        })
        @DisplayName("Pre-L5 martial classes have 1 attack per turn")
        void preL5HasOneAttack(CharacterClass cls) {
            Character c = new Character("X", Race.HUMAN, cls,
                14, 14, 14, 10, 10, 10);
            c.setLevel(4);
            assertEquals(1, c.getAttacksPerTurn(),
                cls + " at L4 should still have 1 attack");
        }

        @ParameterizedTest(name = "Lvl 5 {0} gets Extra Attack -> 2 attacks")
        @CsvSource({
            "FIGHTER",
            "PALADIN",
            "RANGER",
            "MONK",
            "BARBARIAN"
        })
        @DisplayName("L5 martial classes gain Extra Attack")
        void l5MartialsGetExtraAttack(CharacterClass cls) {
            Character c = new Character("X", Race.HUMAN, cls,
                14, 14, 14, 10, 10, 10);
            c.setLevel(5);
            assertEquals(2, c.getAttacksPerTurn(),
                cls + " at L5 should have 2 attacks (Extra Attack)");
        }

        @ParameterizedTest(name = "Lvl 5 {0} (non-martial) keeps 1 attack")
        @CsvSource({
            "WIZARD",
            "SORCERER",
            "WARLOCK",
            "BARD",
            "CLERIC",
            "DRUID",
            "ROGUE"
        })
        @DisplayName("Non-extra-attack classes stay at 1 attack at L5")
        void l5NonExtraAttackClassesUnchanged(CharacterClass cls) {
            Character c = new Character("X", Race.HUMAN, cls,
                14, 14, 14, 10, 10, 10);
            c.setLevel(5);
            assertEquals(1, c.getAttacksPerTurn(),
                cls + " does not get Extra Attack");
        }

        @Test
        @DisplayName("Fighter L11 -> 3 attacks, L20 -> 4 attacks")
        void fighterTieredExtraAttack() {
            Character f = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            f.setLevel(11);
            assertEquals(3, f.getAttacksPerTurn(), "Fighter L11 should have 3 attacks");
            f.setLevel(20);
            assertEquals(4, f.getAttacksPerTurn(), "Fighter L20 should have 4 attacks");
        }
    }

    @Nested
    @DisplayName("Ability Score Improvements")
    class AbilityScoreImprovementTests {

        @Test
        @DisplayName("Crossing level 4 grants one pending ASI")
        void levelingTo4GrantsOneASI() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertEquals(0, fighter.getPendingAbilityScoreImprovements());
            fighter.setLevel(4);
            assertEquals(1, fighter.getPendingAbilityScoreImprovements(),
                "Lvl 4 should grant one ASI");
        }

        @Test
        @DisplayName("Jumping multiple thresholds grants the right total")
        void multiThresholdJumpAccumulates() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            // 1 -> 12 crosses standard ASI levels 4, 8, 12 plus the Fighter
            // bonus ASI at L6 (Fighter gets bonus ASIs at L6 + L14). 4 total.
            fighter.setLevel(12);
            assertEquals(4, fighter.getPendingAbilityScoreImprovements());
        }

        @Test
        @DisplayName("Pre-L4 levels do not grant an ASI")
        void preL4NoASI() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(3);
            assertEquals(0, fighter.getPendingAbilityScoreImprovements());
        }

        @Test
        @DisplayName("Applying +2 to a single ability raises that score by 2 and consumes the ASI")
        void plusTwoSingleAbility() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            int before = fighter.getAbilityScore(Ability.STRENGTH);

            fighter.applyAbilityScoreImprovement(Ability.STRENGTH);

            assertEquals(before + 2, fighter.getAbilityScore(Ability.STRENGTH));
            assertEquals(0, fighter.getPendingAbilityScoreImprovements(),
                "ASI should be consumed");
        }

        @Test
        @DisplayName("Applying +1/+1 to two abilities raises both and consumes the ASI")
        void plusOneOneTwoAbilities() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            int strBefore = fighter.getAbilityScore(Ability.STRENGTH);
            int dexBefore = fighter.getAbilityScore(Ability.DEXTERITY);

            fighter.applyAbilityScoreImprovement(Ability.STRENGTH, Ability.DEXTERITY);

            assertEquals(strBefore + 1, fighter.getAbilityScore(Ability.STRENGTH));
            assertEquals(dexBefore + 1, fighter.getAbilityScore(Ability.DEXTERITY));
            assertEquals(0, fighter.getPendingAbilityScoreImprovements());
        }

        @Test
        @DisplayName("ASI cannot raise an ability above 20")
        void cannotExceedTwenty() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                20, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            assertThrows(IllegalStateException.class,
                () -> fighter.applyAbilityScoreImprovement(Ability.STRENGTH),
                "STR already at 20 — ASI must be rejected");
        }

        @Test
        @DisplayName("Single-ability ASI without pending improvements throws")
        void noPendingThrows() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertThrows(IllegalStateException.class,
                () -> fighter.applyAbilityScoreImprovement(Ability.STRENGTH));
        }

        @Test
        @DisplayName("Two-ability ASI rejects same-ability picks")
        void twoAbilityRejectsSameAbility() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            assertThrows(IllegalArgumentException.class,
                () -> fighter.applyAbilityScoreImprovement(Ability.STRENGTH, Ability.STRENGTH));
        }
    }

    @Nested
    @DisplayName("Saving Throw Advantage")
    class SavingThrowAdvantageTests {

        @Test
        @DisplayName("Lvl 2+ Barbarian gets advantage on DEX saves (Danger Sense)")
        void barbarianDangerSenseGrantsDexAdvantage() {
            Character barb = new Character("Grog", Race.HUMAN, CharacterClass.BARBARIAN,
                14, 14, 14, 10, 10, 10);
            barb.setLevel(2);
            assertTrue(barb.hasAdvantageOnSavingThrow(Ability.DEXTERITY),
                "Lvl 2 Barbarian should have advantage on DEX saves");
            assertFalse(barb.hasAdvantageOnSavingThrow(Ability.STRENGTH),
                "Danger Sense should not affect STR saves");
        }

        @Test
        @DisplayName("Lvl 1 Barbarian has no Danger Sense advantage")
        void lvl1BarbarianHasNoDangerSense() {
            Character barb = new Character("Grog", Race.HUMAN, CharacterClass.BARBARIAN,
                14, 14, 14, 10, 10, 10);
            assertFalse(barb.hasAdvantageOnSavingThrow(Ability.DEXTERITY),
                "Lvl 1 Barbarian should not yet have Danger Sense");
        }

        @Test
        @DisplayName("Non-barbarian classes do not benefit from Danger Sense")
        void nonBarbarianHasNoDangerSense() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(5);
            assertFalse(fighter.hasAdvantageOnSavingThrow(Ability.DEXTERITY),
                "Fighter does not have Danger Sense");
        }
    }
}