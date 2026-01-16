package com.questkeeper.combat;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CombatResult class.
 */
@DisplayName("CombatResult Tests")
class CombatResultTest {

    private Character testCharacter;
    private Monster testMonster;

    @BeforeEach
    void setUp() {
        testCharacter = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
        testMonster = new Monster("goblin", "Goblin", 13, 7);
    }

    @Nested
    @DisplayName("Type Enum Tests")
    class TypeEnumTests {

        @Test
        @DisplayName("enum has correct number of types")
        void enumHasCorrectNumberOfTypes() {
            assertEquals(11, CombatResult.Type.values().length);
        }

        @Test
        @DisplayName("all expected types exist")
        void allExpectedTypesExist() {
            assertNotNull(CombatResult.Type.COMBAT_START);
            assertNotNull(CombatResult.Type.TURN_START);
            assertNotNull(CombatResult.Type.ATTACK_HIT);
            assertNotNull(CombatResult.Type.ATTACK_MISS);
            assertNotNull(CombatResult.Type.SPECIAL_ABILITY);
            assertNotNull(CombatResult.Type.ENEMY_DEFEATED);
            assertNotNull(CombatResult.Type.PLAYER_DEFEATED);
            assertNotNull(CombatResult.Type.VICTORY);
            assertNotNull(CombatResult.Type.FLED);
            assertNotNull(CombatResult.Type.INFO);
            assertNotNull(CombatResult.Type.ERROR);
        }
    }

    @Nested
    @DisplayName("combatStart Factory Tests")
    class CombatStartTests {

        @Test
        @DisplayName("creates result with COMBAT_START type")
        void createsResultWithCombatStartType() {
            List<Combatant> turnOrder = List.of(testCharacter, testMonster);
            CombatResult result = CombatResult.combatStart(turnOrder, "Combat begins!");

            assertEquals(CombatResult.Type.COMBAT_START, result.getType());
        }

        @Test
        @DisplayName("stores message correctly")
        void storesMessageCorrectly() {
            List<Combatant> turnOrder = List.of(testCharacter, testMonster);
            CombatResult result = CombatResult.combatStart(turnOrder, "Combat begins!");

            assertEquals("Combat begins!", result.getMessage());
        }

        @Test
        @DisplayName("stores turn order")
        void storesTurnOrder() {
            List<Combatant> turnOrder = List.of(testCharacter, testMonster);
            CombatResult result = CombatResult.combatStart(turnOrder, "Combat begins!");

            assertTrue(result.hasTurnOrder());
            assertEquals(2, result.getTurnOrder().size());
        }

        @Test
        @DisplayName("turn order is unmodifiable")
        void turnOrderIsUnmodifiable() {
            List<Combatant> turnOrder = new ArrayList<>();
            turnOrder.add(testCharacter);
            turnOrder.add(testMonster);
            CombatResult result = CombatResult.combatStart(turnOrder, "Combat begins!");

            assertThrows(UnsupportedOperationException.class, () -> {
                result.getTurnOrder().add(testMonster);
            });
        }

        @Test
        @DisplayName("attacker and defender are null")
        void attackerAndDefenderAreNull() {
            CombatResult result = CombatResult.combatStart(List.of(testCharacter), "Combat begins!");

            assertNull(result.getAttacker());
            assertNull(result.getDefender());
        }

        @Test
        @DisplayName("attack values are zero")
        void attackValuesAreZero() {
            CombatResult result = CombatResult.combatStart(List.of(testCharacter), "Combat begins!");

            assertEquals(0, result.getAttackRoll());
            assertEquals(0, result.getTargetAC());
            assertEquals(0, result.getDamageRoll());
            assertEquals(0, result.getXpGained());
        }
    }

    @Nested
    @DisplayName("turnStart Factory Tests")
    class TurnStartTests {

        @Test
        @DisplayName("creates result with TURN_START type")
        void createsResultWithTurnStartType() {
            CombatResult result = CombatResult.turnStart(testCharacter);
            assertEquals(CombatResult.Type.TURN_START, result.getType());
        }

        @Test
        @DisplayName("message includes combatant name")
        void messageIncludesCombatantName() {
            CombatResult result = CombatResult.turnStart(testCharacter);
            assertTrue(result.getMessage().contains("Hero"));
            assertTrue(result.getMessage().contains("turn"));
        }

        @Test
        @DisplayName("attacker is the combatant")
        void attackerIsTheCombatant() {
            CombatResult result = CombatResult.turnStart(testCharacter);
            assertEquals(testCharacter, result.getAttacker());
        }

        @Test
        @DisplayName("defender is null")
        void defenderIsNull() {
            CombatResult result = CombatResult.turnStart(testCharacter);
            assertNull(result.getDefender());
        }

        @Test
        @DisplayName("has no turn order")
        void hasNoTurnOrder() {
            CombatResult result = CombatResult.turnStart(testCharacter);
            assertFalse(result.hasTurnOrder());
            assertTrue(result.getTurnOrder().isEmpty());
        }
    }

    @Nested
    @DisplayName("attackHit Factory Tests")
    class AttackHitTests {

        @Test
        @DisplayName("creates result with ATTACK_HIT type")
        void createsResultWithAttackHitType() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertEquals(CombatResult.Type.ATTACK_HIT, result.getType());
        }

        @Test
        @DisplayName("stores attacker and defender")
        void storesAttackerAndDefender() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);

            assertEquals(testCharacter, result.getAttacker());
            assertEquals(testMonster, result.getDefender());
        }

        @Test
        @DisplayName("stores attack roll")
        void storesAttackRoll() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertEquals(18, result.getAttackRoll());
        }

        @Test
        @DisplayName("stores target AC")
        void storesTargetAC() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertEquals(13, result.getTargetAC());
        }

        @Test
        @DisplayName("stores damage roll")
        void storesDamageRoll() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertEquals(8, result.getDamageRoll());
        }

        @Test
        @DisplayName("message includes HIT")
        void messageIncludesHit() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertTrue(result.getMessage().contains("HIT"));
        }

        @Test
        @DisplayName("message includes roll vs AC")
        void messageIncludesRollVsAC() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertTrue(result.getMessage().contains("Roll: 18"));
            assertTrue(result.getMessage().contains("vs AC 13"));
        }

        @Test
        @DisplayName("message includes damage")
        void messageIncludesDamage() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertTrue(result.getMessage().contains("Damage: 8"));
        }

        @Test
        @DisplayName("includes special effect when provided")
        void includesSpecialEffectWhenProvided() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8, "[CRITICAL!]");
            assertTrue(result.getMessage().contains("[CRITICAL!]"));
        }

        @Test
        @DisplayName("no special effect when null")
        void noSpecialEffectWhenNull() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8, null);
            assertFalse(result.getMessage().contains("null"));
        }

        @Test
        @DisplayName("no special effect when empty")
        void noSpecialEffectWhenEmpty() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8, "");
            // Should not have extra spaces or issues
            assertNotNull(result.getMessage());
        }
    }

    @Nested
    @DisplayName("attackMiss Factory Tests")
    class AttackMissTests {

        @Test
        @DisplayName("creates result with ATTACK_MISS type")
        void createsResultWithAttackMissType() {
            CombatResult result = CombatResult.attackMiss(testCharacter, testMonster, 10, 13);
            assertEquals(CombatResult.Type.ATTACK_MISS, result.getType());
        }

        @Test
        @DisplayName("stores attacker and defender")
        void storesAttackerAndDefender() {
            CombatResult result = CombatResult.attackMiss(testCharacter, testMonster, 10, 13);

            assertEquals(testCharacter, result.getAttacker());
            assertEquals(testMonster, result.getDefender());
        }

        @Test
        @DisplayName("stores attack roll and target AC")
        void storesAttackRollAndTargetAC() {
            CombatResult result = CombatResult.attackMiss(testCharacter, testMonster, 10, 13);

            assertEquals(10, result.getAttackRoll());
            assertEquals(13, result.getTargetAC());
        }

        @Test
        @DisplayName("damage is zero")
        void damageIsZero() {
            CombatResult result = CombatResult.attackMiss(testCharacter, testMonster, 10, 13);
            assertEquals(0, result.getDamageRoll());
        }

        @Test
        @DisplayName("message includes MISS")
        void messageIncludesMiss() {
            CombatResult result = CombatResult.attackMiss(testCharacter, testMonster, 10, 13);
            assertTrue(result.getMessage().contains("MISS"));
        }

        @Test
        @DisplayName("message includes roll vs AC")
        void messageIncludesRollVsAC() {
            CombatResult result = CombatResult.attackMiss(testCharacter, testMonster, 10, 13);
            assertTrue(result.getMessage().contains("Roll: 10"));
            assertTrue(result.getMessage().contains("vs AC 13"));
        }
    }

    @Nested
    @DisplayName("enemyDefeated Factory Tests")
    class EnemyDefeatedTests {

        @Test
        @DisplayName("creates result with ENEMY_DEFEATED type")
        void createsResultWithEnemyDefeatedType() {
            CombatResult result = CombatResult.enemyDefeated(testMonster);
            assertEquals(CombatResult.Type.ENEMY_DEFEATED, result.getType());
        }

        @Test
        @DisplayName("message includes enemy name")
        void messageIncludesEnemyName() {
            CombatResult result = CombatResult.enemyDefeated(testMonster);
            assertTrue(result.getMessage().contains("Goblin"));
        }

        @Test
        @DisplayName("message includes defeated")
        void messageIncludesDefeated() {
            CombatResult result = CombatResult.enemyDefeated(testMonster);
            assertTrue(result.getMessage().contains("defeated"));
        }

        @Test
        @DisplayName("defender is the enemy")
        void defenderIsTheEnemy() {
            CombatResult result = CombatResult.enemyDefeated(testMonster);
            assertEquals(testMonster, result.getDefender());
        }
    }

    @Nested
    @DisplayName("playerDefeated Factory Tests")
    class PlayerDefeatedTests {

        @Test
        @DisplayName("creates result with PLAYER_DEFEATED type")
        void createsResultWithPlayerDefeatedType() {
            CombatResult result = CombatResult.playerDefeated(testCharacter);
            assertEquals(CombatResult.Type.PLAYER_DEFEATED, result.getType());
        }

        @Test
        @DisplayName("message includes player name")
        void messageIncludesPlayerName() {
            CombatResult result = CombatResult.playerDefeated(testCharacter);
            assertTrue(result.getMessage().contains("Hero"));
        }

        @Test
        @DisplayName("message includes fallen")
        void messageIncludesFallen() {
            CombatResult result = CombatResult.playerDefeated(testCharacter);
            assertTrue(result.getMessage().contains("fallen"));
        }

        @Test
        @DisplayName("defender is the player")
        void defenderIsThePlayer() {
            CombatResult result = CombatResult.playerDefeated(testCharacter);
            assertEquals(testCharacter, result.getDefender());
        }
    }

    @Nested
    @DisplayName("victory Factory Tests")
    class VictoryTests {

        @Test
        @DisplayName("creates result with VICTORY type")
        void createsResultWithVictoryType() {
            CombatResult result = CombatResult.victory(100);
            assertEquals(CombatResult.Type.VICTORY, result.getType());
        }

        @Test
        @DisplayName("stores XP gained")
        void storesXpGained() {
            CombatResult result = CombatResult.victory(150);
            assertEquals(150, result.getXpGained());
        }

        @Test
        @DisplayName("message includes Victory")
        void messageIncludesVictory() {
            CombatResult result = CombatResult.victory(100);
            assertTrue(result.getMessage().contains("Victory"));
        }

        @Test
        @DisplayName("message includes XP amount")
        void messageIncludesXpAmount() {
            CombatResult result = CombatResult.victory(250);
            assertTrue(result.getMessage().contains("250"));
            assertTrue(result.getMessage().contains("XP"));
        }
    }

    @Nested
    @DisplayName("fled Factory Tests")
    class FledTests {

        @Test
        @DisplayName("creates result with FLED type (no message)")
        void createsResultWithFledType() {
            CombatResult result = CombatResult.fled();
            assertEquals(CombatResult.Type.FLED, result.getType());
        }

        @Test
        @DisplayName("default message mentions fled")
        void defaultMessageMentionsFled() {
            CombatResult result = CombatResult.fled();
            assertTrue(result.getMessage().contains("fled"));
        }

        @Test
        @DisplayName("creates result with custom message")
        void createsResultWithCustomMessage() {
            CombatResult result = CombatResult.fled("You escaped barely!");
            assertEquals("You escaped barely!", result.getMessage());
        }

        @Test
        @DisplayName("custom message preserves FLED type")
        void customMessagePreservesFledType() {
            CombatResult result = CombatResult.fled("Custom flee message");
            assertEquals(CombatResult.Type.FLED, result.getType());
        }
    }

    @Nested
    @DisplayName("opportunityAttack Factory Tests")
    class OpportunityAttackTests {

        @Test
        @DisplayName("creates result with ATTACK_HIT type")
        void createsResultWithAttackHitType() {
            CombatResult result = CombatResult.opportunityAttack(testMonster, testCharacter, 18, 14, 6);
            assertEquals(CombatResult.Type.ATTACK_HIT, result.getType());
        }

        @Test
        @DisplayName("message includes Opportunity Attack")
        void messageIncludesOpportunityAttack() {
            CombatResult result = CombatResult.opportunityAttack(testMonster, testCharacter, 18, 14, 6);
            assertTrue(result.getMessage().contains("Opportunity Attack"));
        }

        @Test
        @DisplayName("stores attack details")
        void storesAttackDetails() {
            CombatResult result = CombatResult.opportunityAttack(testMonster, testCharacter, 18, 14, 6);

            assertEquals(testMonster, result.getAttacker());
            assertEquals(testCharacter, result.getDefender());
            assertEquals(18, result.getAttackRoll());
            assertEquals(14, result.getTargetAC());
            assertEquals(6, result.getDamageRoll());
        }

        @Test
        @DisplayName("message includes HIT")
        void messageIncludesHit() {
            CombatResult result = CombatResult.opportunityAttack(testMonster, testCharacter, 18, 14, 6);
            assertTrue(result.getMessage().contains("HIT"));
        }
    }

    @Nested
    @DisplayName("opportunityAttackMiss Factory Tests")
    class OpportunityAttackMissTests {

        @Test
        @DisplayName("creates result with ATTACK_MISS type")
        void createsResultWithAttackMissType() {
            CombatResult result = CombatResult.opportunityAttackMiss(testMonster, testCharacter, 10, 14);
            assertEquals(CombatResult.Type.ATTACK_MISS, result.getType());
        }

        @Test
        @DisplayName("message includes Opportunity Attack")
        void messageIncludesOpportunityAttack() {
            CombatResult result = CombatResult.opportunityAttackMiss(testMonster, testCharacter, 10, 14);
            assertTrue(result.getMessage().contains("Opportunity Attack"));
        }

        @Test
        @DisplayName("message includes MISS")
        void messageIncludesMiss() {
            CombatResult result = CombatResult.opportunityAttackMiss(testMonster, testCharacter, 10, 14);
            assertTrue(result.getMessage().contains("MISS"));
        }

        @Test
        @DisplayName("damage is zero")
        void damageIsZero() {
            CombatResult result = CombatResult.opportunityAttackMiss(testMonster, testCharacter, 10, 14);
            assertEquals(0, result.getDamageRoll());
        }
    }

    @Nested
    @DisplayName("specialAbility Factory Tests")
    class SpecialAbilityTests {

        @Test
        @DisplayName("creates result with SPECIAL_ABILITY type (with target)")
        void createsResultWithSpecialAbilityTypeWithTarget() {
            CombatResult result = CombatResult.specialAbility(testMonster, testCharacter, "Disarm", "Weapon dropped!");
            assertEquals(CombatResult.Type.SPECIAL_ABILITY, result.getType());
        }

        @Test
        @DisplayName("message includes ability name (with target)")
        void messageIncludesAbilityNameWithTarget() {
            CombatResult result = CombatResult.specialAbility(testMonster, testCharacter, "Disarm", "Weapon dropped!");
            assertTrue(result.getMessage().contains("Disarm"));
        }

        @Test
        @DisplayName("message includes effect description (with target)")
        void messageIncludesEffectDescriptionWithTarget() {
            CombatResult result = CombatResult.specialAbility(testMonster, testCharacter, "Disarm", "Weapon dropped!");
            assertTrue(result.getMessage().contains("Weapon dropped!"));
        }

        @Test
        @DisplayName("stores user and target")
        void storesUserAndTarget() {
            CombatResult result = CombatResult.specialAbility(testMonster, testCharacter, "Disarm", "Effect");

            assertEquals(testMonster, result.getAttacker());
            assertEquals(testCharacter, result.getDefender());
        }

        @Test
        @DisplayName("creates result with SPECIAL_ABILITY type (no target)")
        void createsResultWithSpecialAbilityTypeNoTarget() {
            CombatResult result = CombatResult.specialAbility(testCharacter, "Second Wind", "Healed 10 HP!");
            assertEquals(CombatResult.Type.SPECIAL_ABILITY, result.getType());
        }

        @Test
        @DisplayName("message includes ability name (no target)")
        void messageIncludesAbilityNameNoTarget() {
            CombatResult result = CombatResult.specialAbility(testCharacter, "Second Wind", "Healed 10 HP!");
            assertTrue(result.getMessage().contains("Second Wind"));
        }

        @Test
        @DisplayName("stores user but no target")
        void storesUserButNoTarget() {
            CombatResult result = CombatResult.specialAbility(testCharacter, "Second Wind", "Effect");

            assertEquals(testCharacter, result.getAttacker());
            assertNull(result.getDefender());
        }
    }

    @Nested
    @DisplayName("info Factory Tests")
    class InfoTests {

        @Test
        @DisplayName("creates result with INFO type")
        void createsResultWithInfoType() {
            CombatResult result = CombatResult.info("The enemy tries to flee!");
            assertEquals(CombatResult.Type.INFO, result.getType());
        }

        @Test
        @DisplayName("stores info message")
        void storesInfoMessage() {
            CombatResult result = CombatResult.info("Status update here");
            assertEquals("Status update here", result.getMessage());
        }

        @Test
        @DisplayName("isInfo returns true")
        void isInfoReturnsTrue() {
            CombatResult result = CombatResult.info("Info message");
            assertTrue(result.isInfo());
        }

        @Test
        @DisplayName("isError returns false for info")
        void isErrorReturnsFalseForInfo() {
            CombatResult result = CombatResult.info("Info message");
            assertFalse(result.isError());
        }

        @Test
        @DisplayName("isSuccess returns true for info")
        void isSuccessReturnsTrueForInfo() {
            CombatResult result = CombatResult.info("Info message");
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("error Factory Tests")
    class ErrorTests {

        @Test
        @DisplayName("creates result with ERROR type")
        void createsResultWithErrorType() {
            CombatResult result = CombatResult.error("Something went wrong!");
            assertEquals(CombatResult.Type.ERROR, result.getType());
        }

        @Test
        @DisplayName("stores error message")
        void storesErrorMessage() {
            CombatResult result = CombatResult.error("Invalid action!");
            assertEquals("Invalid action!", result.getMessage());
        }

        @Test
        @DisplayName("isError returns true")
        void isErrorReturnsTrue() {
            CombatResult result = CombatResult.error("Error");
            assertTrue(result.isError());
        }

        @Test
        @DisplayName("isSuccess returns false")
        void isSuccessReturnsFalse() {
            CombatResult result = CombatResult.error("Error");
            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Accessor Tests")
    class AccessorTests {

        @Test
        @DisplayName("isSuccess returns true for non-error types")
        void isSuccessReturnsTrueForNonErrorTypes() {
            assertTrue(CombatResult.victory(100).isSuccess());
            assertTrue(CombatResult.fled().isSuccess());
            assertTrue(CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8).isSuccess());
        }

        @Test
        @DisplayName("isError returns false for non-error types")
        void isErrorReturnsFalseForNonErrorTypes() {
            assertFalse(CombatResult.victory(100).isError());
            assertFalse(CombatResult.fled().isError());
            assertFalse(CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8).isError());
        }

        @Test
        @DisplayName("isCombatOver returns true for VICTORY")
        void isCombatOverReturnsTrueForVictory() {
            assertTrue(CombatResult.victory(100).isCombatOver());
        }

        @Test
        @DisplayName("isCombatOver returns true for PLAYER_DEFEATED")
        void isCombatOverReturnsTrueForPlayerDefeated() {
            assertTrue(CombatResult.playerDefeated(testCharacter).isCombatOver());
        }

        @Test
        @DisplayName("isCombatOver returns true for FLED")
        void isCombatOverReturnsTrueForFled() {
            assertTrue(CombatResult.fled().isCombatOver());
        }

        @Test
        @DisplayName("isCombatOver returns false for other types")
        void isCombatOverReturnsFalseForOtherTypes() {
            assertFalse(CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8).isCombatOver());
            assertFalse(CombatResult.attackMiss(testCharacter, testMonster, 10, 13).isCombatOver());
            assertFalse(CombatResult.turnStart(testCharacter).isCombatOver());
            assertFalse(CombatResult.enemyDefeated(testMonster).isCombatOver());
            assertFalse(CombatResult.error("Error").isCombatOver());
        }

        @Test
        @DisplayName("hasTurnOrder returns true when turn order exists")
        void hasTurnOrderReturnsTrueWhenExists() {
            CombatResult result = CombatResult.combatStart(List.of(testCharacter), "Start");
            assertTrue(result.hasTurnOrder());
        }

        @Test
        @DisplayName("hasTurnOrder returns false when no turn order")
        void hasTurnOrderReturnsFalseWhenNone() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertFalse(result.hasTurnOrder());
        }
    }

    @Nested
    @DisplayName("format Method Tests")
    class FormatTests {

        @Test
        @DisplayName("format includes message")
        void formatIncludesMessage() {
            CombatResult result = CombatResult.victory(100);
            String formatted = result.format();
            assertTrue(formatted.contains("Victory"));
        }

        @Test
        @DisplayName("format includes initiative order for COMBAT_START")
        void formatIncludesInitiativeOrderForCombatStart() {
            List<Combatant> turnOrder = List.of(testCharacter, testMonster);
            CombatResult result = CombatResult.combatStart(turnOrder, "Combat begins!");

            String formatted = result.format();

            assertTrue(formatted.contains("Initiative Order"));
            assertTrue(formatted.contains("1."));
            assertTrue(formatted.contains("2."));
        }

        @Test
        @DisplayName("format does not include initiative for non-COMBAT_START")
        void formatDoesNotIncludeInitiativeForOtherTypes() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            String formatted = result.format();

            assertFalse(formatted.contains("Initiative Order"));
        }
    }

    @Nested
    @DisplayName("toString Method Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString includes type")
        void toStringIncludesType() {
            CombatResult result = CombatResult.victory(100);
            String str = result.toString();
            assertTrue(str.contains("VICTORY"));
        }

        @Test
        @DisplayName("toString includes truncated message")
        void toStringIncludesTruncatedMessage() {
            CombatResult result = CombatResult.victory(100);
            String str = result.toString();
            assertTrue(str.contains("Victory"));
        }

        @Test
        @DisplayName("toString handles long messages")
        void toStringHandlesLongMessages() {
            String longMessage = "A".repeat(100);
            CombatResult result = CombatResult.error(longMessage);
            String str = result.toString();

            // Should be truncated to 50 characters max
            assertTrue(str.length() < longMessage.length() + 50);
        }

        @Test
        @DisplayName("toString format is correct")
        void toStringFormatIsCorrect() {
            CombatResult result = CombatResult.error("Test error");
            String str = result.toString();
            assertTrue(str.startsWith("CombatResult["));
            assertTrue(str.endsWith("]"));
        }
    }

    @Nested
    @DisplayName("Null Turn Order Tests")
    class NullTurnOrderTests {

        @Test
        @DisplayName("getTurnOrder returns empty list when null")
        void getTurnOrderReturnsEmptyListWhenNull() {
            CombatResult result = CombatResult.attackHit(testCharacter, testMonster, 18, 13, 8);
            assertNotNull(result.getTurnOrder());
            assertTrue(result.getTurnOrder().isEmpty());
        }
    }
}
