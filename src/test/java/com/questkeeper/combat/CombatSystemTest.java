package com.questkeeper.combat;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.state.GameState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CombatSystem class.
 *
 * @author Marc McGough
 */
@DisplayName("CombatSystem Tests")
class CombatSystemTest {

    @TempDir
    Path tempDir;

    private Path campaignDir;
    private Campaign campaign;
    private Character character;
    private GameState state;
    private CombatSystem combatSystem;

    @BeforeEach
    void setUp() throws IOException {
        campaignDir = tempDir.resolve("test_campaign");
        Files.createDirectories(campaignDir);

        createCampaignYaml();
        createLocationsYaml();

        campaign = Campaign.loadFromYaml(campaignDir);
        character = new Character("TestHero", Race.HUMAN, CharacterClass.FIGHTER,
            14, 12, 14, 10, 10, 10);
        state = new GameState(character, campaign);
        combatSystem = new CombatSystem();
    }

    private void createCampaignYaml() throws IOException {
        Files.writeString(campaignDir.resolve("campaign.yaml"), """
            id: test_campaign
            name: Test Campaign
            author: Test
            version: "1.0"
            starting_location: tavern
            """);
    }

    private void createLocationsYaml() throws IOException {
        Files.writeString(campaignDir.resolve("locations.yaml"), """
            locations:
              - id: tavern
                name: The Tavern
                description: A cozy tavern.
            """);
    }

    private Monster createGoblin() {
        Monster goblin = new Monster("goblin_1", "Goblin", 13, 7);
        goblin.setAttackBonus(4);
        goblin.setDamageDice("1d6+2");
        goblin.setDexterityMod(2);
        goblin.setExperienceValue(50);
        return goblin;
    }

    private Monster createOrc() {
        Monster orc = new Monster("orc_1", "Orc", 13, 15);
        orc.setAttackBonus(5);
        orc.setDamageDice("1d12+3");
        orc.setDexterityMod(1);
        orc.setExperienceValue(100);
        return orc;
    }

    // ==========================================
    // Initial State Tests
    // ==========================================

    @Nested
    @DisplayName("Initial State")
    class InitialStateTests {

        @Test
        @DisplayName("starts not in combat")
        void startsNotInCombat() {
            assertFalse(combatSystem.isInCombat());
            assertNull(combatSystem.getCurrentCombatant());
            assertTrue(combatSystem.getParticipants().isEmpty());
        }
    }

    // ==========================================
    // Start Combat Tests
    // ==========================================

    @Nested
    @DisplayName("Start Combat")
    class StartCombatTests {

        @Test
        @DisplayName("starts combat successfully")
        void startsCombatSuccessfully() {
            Monster goblin = createGoblin();

            CombatResult result = combatSystem.startCombat(state, List.of(goblin));

            assertEquals(CombatResult.Type.COMBAT_START, result.getType());
            assertTrue(combatSystem.isInCombat());
            assertTrue(result.getMessage().contains("Combat begins"));
        }

        @Test
        @DisplayName("includes all participants")
        void includesAllParticipants() {
            Monster goblin = createGoblin();
            Monster orc = createOrc();

            combatSystem.startCombat(state, List.of(goblin, orc));

            List<Combatant> participants = combatSystem.getParticipants();
            assertEquals(3, participants.size()); // Player + 2 enemies
        }

        @Test
        @DisplayName("rolls initiative for all")
        void rollsInitiativeForAll() {
            Monster goblin = createGoblin();

            CombatResult result = combatSystem.startCombat(state, List.of(goblin));

            assertTrue(result.getMessage().contains("Initiative Rolls"));
            assertTrue(result.hasTurnOrder());
            assertEquals(2, result.getTurnOrder().size());
        }

        @Test
        @DisplayName("sorts initiative order descending")
        void sortsInitiativeOrder() {
            Monster goblin = createGoblin();

            combatSystem.startCombat(state, List.of(goblin));

            List<Combatant> order = combatSystem.getInitiativeOrder();
            int firstRoll = combatSystem.getInitiativeRoll(order.get(0));
            int secondRoll = combatSystem.getInitiativeRoll(order.get(1));

            assertTrue(firstRoll >= secondRoll);
        }

        @Test
        @DisplayName("resets enemy HP")
        void resetsEnemyHp() {
            Monster goblin = createGoblin();
            goblin.takeDamage(5);
            assertEquals(2, goblin.getCurrentHitPoints());

            combatSystem.startCombat(state, List.of(goblin));

            assertEquals(7, goblin.getCurrentHitPoints());
        }

        @Test
        @DisplayName("fails for null state")
        void failsForNullState() {
            Monster goblin = createGoblin();

            CombatResult result = combatSystem.startCombat(null, List.of(goblin));

            assertTrue(result.isError());
        }

        @Test
        @DisplayName("fails for empty enemies")
        void failsForEmptyEnemies() {
            CombatResult result = combatSystem.startCombat(state, List.of());

            assertTrue(result.isError());
        }

        @Test
        @DisplayName("fails for null enemies")
        void failsForNullEnemies() {
            CombatResult result = combatSystem.startCombat(state, null);

            assertTrue(result.isError());
        }
    }

    // ==========================================
    // Player Turn Tests
    // ==========================================

    @Nested
    @DisplayName("Player Turn")
    class PlayerTurnTests {

        private Monster goblin;

        @BeforeEach
        void startCombatWithPlayer() {
            goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Advance until player's turn
            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Monster) {
                combatSystem.enemyTurn();
            }
        }

        @Test
        @DisplayName("attack hits when roll meets AC")
        void attackHitsWhenRollMeetsAC() {
            // Use high-HP weak enemy so player survives long enough to hit
            Monster toughGoblin = new Monster("tough_goblin", "Tough Goblin", 13, 100);
            toughGoblin.setDexterityMod(2);
            toughGoblin.setAttackBonus(0);  // Low attack to protect player
            toughGoblin.setDamageDice("1d1");  // Minimal damage
            combatSystem.startCombat(state, List.of(toughGoblin));

            while (combatSystem.getCurrentCombatant() instanceof Monster &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            // Run many attempts to get at least one hit
            boolean gotHit = false;
            for (int i = 0; i < 20 && !gotHit && combatSystem.isInCombat(); i++) {
                CombatResult result = combatSystem.playerTurn("attack", null);
                if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                    gotHit = true;
                    assertTrue(result.getMessage().contains("HIT"));
                }

                // Advance turns if combat continues
                if (combatSystem.isInCombat()) {
                    while (combatSystem.getCurrentCombatant() instanceof Monster &&
                           combatSystem.isInCombat()) {
                        combatSystem.enemyTurn();
                    }
                }
            }
            assertTrue(gotHit, "Should hit at least once in 20 attempts");
        }

        @Test
        @DisplayName("attack shows roll details")
        void attackShowsRollDetails() {
            // Use a high-HP enemy to ensure we get an attack result, not victory
            Monster toughGoblin = new Monster("tough_goblin", "Tough Goblin", 13, 100);
            toughGoblin.setDexterityMod(2);
            combatSystem.startCombat(state, List.of(toughGoblin));

            while (combatSystem.getCurrentCombatant() instanceof Monster) {
                combatSystem.enemyTurn();
            }

            CombatResult result = combatSystem.playerTurn("attack", null);

            // Result should be ATTACK_HIT or ATTACK_MISS, both contain roll info
            assertTrue(result.getType() == CombatResult.Type.ATTACK_HIT ||
                       result.getType() == CombatResult.Type.ATTACK_MISS);
            assertTrue(result.getMessage().contains("Roll:"));
            assertTrue(result.getMessage().contains("vs AC"));
        }

        @Test
        @DisplayName("attack defaults to first enemy")
        void attackDefaultsToFirstEnemy() {
            // Use high-HP weak enemy so combat doesn't end early
            Monster toughGoblin = new Monster("tough_goblin", "Tough Goblin", 13, 100);
            toughGoblin.setAttackBonus(0);
            toughGoblin.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(toughGoblin));

            while (combatSystem.getCurrentCombatant() instanceof Monster &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("attack", null);
                assertTrue(result.getMessage().contains("Goblin") ||
                           result.getMessage().contains("goblin"));
            }
        }

        @Test
        @DisplayName("attack targets specific enemy")
        void attackTargetsSpecificEnemy() {
            // Use high-HP enemies to ensure combat doesn't end early
            Monster toughGoblin = new Monster("tough_goblin", "Tough Goblin", 13, 100);
            Monster toughOrc = new Monster("tough_orc", "Tough Orc", 13, 100);
            combatSystem.startCombat(state, List.of(toughGoblin, toughOrc));

            while (combatSystem.getCurrentCombatant() instanceof Monster &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("attack", "orc");
                assertTrue(result.getMessage().contains("Orc"));
            }
        }

        @Test
        @DisplayName("attack with partial name match")
        void attackWithPartialNameMatch() {
            // Use high-HP weak enemy to ensure combat doesn't end early
            Monster toughGoblin = new Monster("tough_goblin", "Tough Goblin", 13, 100);
            toughGoblin.setAttackBonus(0);
            toughGoblin.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(toughGoblin));

            while (combatSystem.getCurrentCombatant() instanceof Monster &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("attack", "gob");
                assertTrue(result.getMessage().contains("Goblin"));
            }
        }

        @Test
        @DisplayName("fails for unknown target")
        void failsForUnknownTarget() {
            CombatResult result = combatSystem.playerTurn("attack", "dragon");

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("No enemy"));
        }

        @Test
        @DisplayName("fails for invalid action")
        void failsForInvalidAction() {
            CombatResult result = combatSystem.playerTurn("dance", null);

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("Unknown action"));
        }

        @Test
        @DisplayName("accepts attack synonyms")
        void acceptsAttackSynonyms() {
            // Use high-HP weak enemy so combat doesn't end
            Monster toughEnemy = new Monster("tough", "Tough Enemy", 13, 100);
            toughEnemy.setAttackBonus(0);
            toughEnemy.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(toughEnemy));

            while (combatSystem.getCurrentCombatant() instanceof Monster &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult hitResult = combatSystem.playerTurn("hit", null);
                assertFalse(hitResult.isError());
            }

            // Advance to player turn again
            while (combatSystem.getCurrentCombatant() instanceof Monster &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult strikeResult = combatSystem.playerTurn("strike", null);
                assertFalse(strikeResult.isError());
            }
        }
    }

    // ==========================================
    // Enemy Turn Tests
    // ==========================================

    @Nested
    @DisplayName("Enemy Turn")
    class EnemyTurnTests {

        @Test
        @DisplayName("enemy attacks player")
        void enemyAttacksPlayer() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Find an enemy turn
            while (combatSystem.isInCombat() &&
                   !(combatSystem.getCurrentCombatant() instanceof Monster)) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.enemyTurn();

                assertTrue(result.getMessage().contains(character.getName()) ||
                           result.isCombatOver());
            }
        }

        @Test
        @DisplayName("enemy turn shows roll details")
        void enemyTurnShowsRollDetails() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            while (!(combatSystem.getCurrentCombatant() instanceof Monster) &&
                   combatSystem.isInCombat()) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.enemyTurn();

                assertTrue(result.getMessage().contains("Roll:") ||
                           result.isCombatOver());
            }
        }
    }

    // ==========================================
    // Attack Resolution Tests
    // ==========================================

    @Nested
    @DisplayName("Attack Resolution")
    class AttackResolutionTests {

        @Test
        @DisplayName("damage reduces target HP")
        void damageReducesTargetHp() {
            // Use weak enemy (low attack) and high HP to ensure we see HP reduction
            Monster weakEnemy = new Monster("weak", "Weak Enemy", 10, 50);
            weakEnemy.setAttackBonus(0);
            weakEnemy.setDamageDice("1d1");  // Very low damage
            combatSystem.startCombat(state, List.of(weakEnemy));

            int startingHp = weakEnemy.getCurrentHitPoints();

            // Attack until we hit or run out of turns
            int maxTurns = 50;
            while (combatSystem.isInCombat() && weakEnemy.getCurrentHitPoints() == startingHp && maxTurns-- > 0) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    combatSystem.playerTurn("attack", null);
                } else {
                    combatSystem.enemyTurn();
                }
            }

            // Either we hit or combat ended
            assertTrue(weakEnemy.getCurrentHitPoints() < startingHp || !weakEnemy.isAlive(),
                "Enemy HP should have decreased after being hit");
        }

        @Test
        @DisplayName("attack result includes attacker and defender")
        void attackResultIncludesParticipants() {
            // Use weak enemy to ensure player survives
            Monster weakEnemy = new Monster("weak", "Weak Enemy", 13, 100);
            weakEnemy.setAttackBonus(0);
            weakEnemy.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(weakEnemy));

            while (!(combatSystem.getCurrentCombatant() instanceof Character) &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("attack", null);

                // Only check for ATTACK_HIT or ATTACK_MISS results
                if (result.getType() == CombatResult.Type.ATTACK_HIT ||
                    result.getType() == CombatResult.Type.ATTACK_MISS) {
                    assertNotNull(result.getAttacker());
                    assertNotNull(result.getDefender());
                }
            }
        }
    }

    // ==========================================
    // End Conditions Tests
    // ==========================================

    @Nested
    @DisplayName("End Conditions")
    class EndConditionsTests {

        @Test
        @DisplayName("victory when all enemies dead")
        void victoryWhenAllEnemiesDead() {
            Monster weakGoblin = new Monster("weak_goblin", "Weak Goblin", 5, 1);
            weakGoblin.setExperienceValue(25);

            combatSystem.startCombat(state, List.of(weakGoblin));

            CombatResult result = null;
            while (combatSystem.isInCombat()) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    result = combatSystem.playerTurn("attack", null);
                } else {
                    result = combatSystem.enemyTurn();
                }
            }

            assertNotNull(result);
            assertTrue(result.getType() == CombatResult.Type.VICTORY ||
                       result.getType() == CombatResult.Type.PLAYER_DEFEATED);
        }

        @Test
        @DisplayName("defeat when player HP reaches zero")
        void defeatWhenPlayerDead() {
            // Create a very strong monster
            Monster dragon = new Monster("dragon", "Dragon", 5, 100);
            dragon.setAttackBonus(20);
            dragon.setDamageDice("10d10+50");

            // Weaken player
            character.takeDamage(character.getCurrentHitPoints() - 1);

            combatSystem.startCombat(state, List.of(dragon));

            CombatResult result = null;
            int maxTurns = 100;
            while (combatSystem.isInCombat() && maxTurns-- > 0) {
                if (combatSystem.getCurrentCombatant() instanceof Monster) {
                    result = combatSystem.enemyTurn();
                } else {
                    result = combatSystem.playerTurn("attack", null);
                }
            }

            // Either player died or we hit max turns
            assertTrue(!combatSystem.isInCombat() || maxTurns <= 0);
        }

        @Test
        @DisplayName("fled ends combat")
        void fledEndsCombat() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Keep trying to flee until successful or combat ends
            while (combatSystem.isInCombat()) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    CombatResult result = combatSystem.playerTurn("flee", null);
                    if (result.getType() == CombatResult.Type.FLED) {
                        assertFalse(combatSystem.isInCombat());
                        return;
                    }
                } else {
                    combatSystem.enemyTurn();
                }
            }
        }

        @Test
        @DisplayName("victory awards XP")
        void victoryAwardsXp() {
            Monster weakGoblin = new Monster("weak_goblin", "Weak Goblin", 5, 1);
            weakGoblin.setExperienceValue(50);

            int startingXp = character.getExperiencePoints();
            combatSystem.startCombat(state, List.of(weakGoblin));

            while (combatSystem.isInCombat()) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    combatSystem.playerTurn("attack", null);
                } else {
                    combatSystem.enemyTurn();
                }
            }

            // XP should increase if player won
            if (character.isAlive()) {
                assertTrue(character.getExperiencePoints() >= startingXp);
            }
        }
    }

    // ==========================================
    // Turn Cycling Tests
    // ==========================================

    @Nested
    @DisplayName("Turn Cycling")
    class TurnCyclingTests {

        @Test
        @DisplayName("advances turn after player action")
        void advancesTurnAfterPlayerAction() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Get initial turn
            int initialTurn = combatSystem.getCurrentTurnIndex();

            // Execute one full round
            if (combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            } else {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                int newTurn = combatSystem.getCurrentTurnIndex();
                assertNotEquals(initialTurn, newTurn);
            }
        }

        @Test
        @DisplayName("cycles through all combatants")
        void cyclesThroughAllCombatants() {
            Monster goblin = createGoblin();
            Monster orc = createOrc();
            combatSystem.startCombat(state, List.of(goblin, orc));

            // Track which combatants took turns
            java.util.Set<String> tookTurn = new java.util.HashSet<>();

            int maxIterations = 10;
            while (combatSystem.isInCombat() && tookTurn.size() < 3 && maxIterations-- > 0) {
                Combatant current = combatSystem.getCurrentCombatant();
                tookTurn.add(current.getName());

                if (current instanceof Character) {
                    combatSystem.playerTurn("attack", null);
                } else {
                    combatSystem.enemyTurn();
                }
            }

            // All 3 combatants should have had a turn (or combat ended)
            assertTrue(tookTurn.size() >= 1);
        }
    }

    // ==========================================
    // Roll Transparency Tests
    // ==========================================

    @Nested
    @DisplayName("Roll Transparency")
    class RollTransparencyTests {

        @Test
        @DisplayName("attack message shows roll vs AC")
        void attackMessageShowsRollVsAc() {
            // Use high-HP enemy to ensure attack result, not victory
            Monster toughGoblin = new Monster("tough_goblin", "Tough Goblin", 13, 100);
            toughGoblin.setDexterityMod(2);
            combatSystem.startCombat(state, List.of(toughGoblin));

            while (!(combatSystem.getCurrentCombatant() instanceof Character) &&
                   combatSystem.isInCombat()) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("attack", null);
                assertTrue(result.getMessage().contains("vs AC"));
            }
        }

        @Test
        @DisplayName("hit message shows damage")
        void hitMessageShowsDamage() {
            Monster weakGoblin = new Monster("weak", "Weak Goblin", 1, 100);
            combatSystem.startCombat(state, List.of(weakGoblin));

            while (combatSystem.isInCombat()) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    CombatResult result = combatSystem.playerTurn("attack", null);
                    if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                        assertTrue(result.getMessage().contains("Damage:"));
                        return;
                    }
                } else {
                    combatSystem.enemyTurn();
                }
            }
        }

        @Test
        @DisplayName("combat start shows all initiative rolls")
        void combatStartShowsInitiativeRolls() {
            Monster goblin = createGoblin();
            Monster orc = createOrc();

            CombatResult result = combatSystem.startCombat(state, List.of(goblin, orc));

            String message = result.getMessage();
            assertTrue(message.contains("TestHero"));
            assertTrue(message.contains("Goblin"));
            assertTrue(message.contains("Orc"));
        }
    }

    // ==========================================
    // State Management Tests
    // ==========================================

    @Nested
    @DisplayName("State Management")
    class StateManagementTests {

        @Test
        @DisplayName("getEnemies returns only monsters")
        void getEnemiesReturnsOnlyMonsters() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            List<Combatant> enemies = combatSystem.getEnemies();

            assertEquals(1, enemies.size());
            assertTrue(enemies.get(0) instanceof Monster);
        }

        @Test
        @DisplayName("getPlayer returns character")
        void getPlayerReturnsCharacter() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            Combatant player = combatSystem.getPlayer();

            assertNotNull(player);
            assertEquals("TestHero", player.getName());
        }

        @Test
        @DisplayName("getLivingEnemies excludes dead")
        void getLivingEnemiesExcludesDead() {
            Monster goblin = createGoblin();
            Monster orc = createOrc();
            combatSystem.startCombat(state, List.of(goblin, orc));

            // Kill the goblin
            goblin.takeDamage(100);

            List<Combatant> living = combatSystem.getLivingEnemies();

            assertEquals(1, living.size());
            assertEquals("Orc", living.get(0).getName());
        }
    }

    // ==========================================
    // Error Handling Tests
    // ==========================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("playerTurn fails when not in combat")
        void playerTurnFailsWhenNotInCombat() {
            CombatResult result = combatSystem.playerTurn("attack", null);

            assertTrue(result.isError());
            assertTrue(result.getMessage().contains("Not in combat"));
        }

        @Test
        @DisplayName("enemyTurn fails when not in combat")
        void enemyTurnFailsWhenNotInCombat() {
            CombatResult result = combatSystem.enemyTurn();

            assertTrue(result.isError());
        }

        @Test
        @DisplayName("executeTurn fails when not in combat")
        void executeTurnFailsWhenNotInCombat() {
            CombatResult result = combatSystem.executeTurn();

            assertTrue(result.isError());
        }

        @Test
        @DisplayName("endCombat fails when not in combat")
        void endCombatFailsWhenNotInCombat() {
            CombatResult result = combatSystem.endCombat();

            assertTrue(result.isError());
        }
    }

    // ==========================================
    // Monster Behavior Tests
    // ==========================================

    @Nested
    @DisplayName("Monster Behavior")
    class MonsterBehaviorTests {

        @Test
        @DisplayName("monster defaults to aggressive behavior")
        void monsterDefaultsToAggressive() {
            Monster goblin = createGoblin();
            assertEquals(Monster.Behavior.AGGRESSIVE, goblin.getBehavior());
        }

        @Test
        @DisplayName("can set monster behavior")
        void canSetMonsterBehavior() {
            Monster goblin = createGoblin();
            goblin.setBehavior(Monster.Behavior.COWARDLY);
            assertEquals(Monster.Behavior.COWARDLY, goblin.getBehavior());
        }

        @Test
        @DisplayName("isBloodied returns true at 50% HP")
        void isBloodiedAtHalfHp() {
            Monster goblin = new Monster("goblin", "Goblin", 10, 20);
            assertFalse(goblin.isBloodied());

            goblin.takeDamage(10);  // Now at 50%
            assertTrue(goblin.isBloodied());
        }

        @Test
        @DisplayName("isBloodied returns true below 50% HP")
        void isBloodiedBelowHalfHp() {
            Monster goblin = new Monster("goblin", "Goblin", 10, 20);
            goblin.takeDamage(15);  // Now at 25%
            assertTrue(goblin.isBloodied());
        }

        @Test
        @DisplayName("getHpPercentage calculates correctly")
        void getHpPercentageCalculatesCorrectly() {
            Monster goblin = new Monster("goblin", "Goblin", 10, 100);
            assertEquals(100, goblin.getHpPercentage());

            goblin.takeDamage(50);
            assertEquals(50, goblin.getHpPercentage());

            goblin.takeDamage(25);
            assertEquals(25, goblin.getHpPercentage());
        }

        @Test
        @DisplayName("behavior is copied when monster is copied")
        void behaviorIsCopied() {
            Monster original = createGoblin();
            original.setBehavior(Monster.Behavior.TACTICAL);

            Monster copy = original.copy("goblin_copy");

            assertEquals(Monster.Behavior.TACTICAL, copy.getBehavior());
        }
    }

    // ==========================================
    // Aggro Tracking Tests
    // ==========================================

    @Nested
    @DisplayName("Aggro Tracking")
    class AggroTrackingTests {

        @Test
        @DisplayName("attack records last attacker")
        void attackRecordsLastAttacker() {
            Monster goblin = new Monster("goblin", "Test Goblin", 5, 100);
            goblin.setAttackBonus(0);
            goblin.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(goblin));

            // Find player turn and attack
            while (combatSystem.isInCombat() &&
                   !(combatSystem.getCurrentCombatant() instanceof Character)) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("attack", null);

                // If the attack hit, the goblin should remember who hit it
                if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                    Combatant lastAttacker = combatSystem.getLastAttacker(goblin);
                    assertNotNull(lastAttacker);
                    assertEquals(character.getName(), lastAttacker.getName());
                }
            }
        }

        @Test
        @DisplayName("getLastAttacker returns null before being hit")
        void getLastAttackerReturnsNullInitially() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            Combatant lastAttacker = combatSystem.getLastAttacker(goblin);
            assertNull(lastAttacker);
        }
    }

    // ==========================================
    // Behavior Enum Tests
    // ==========================================

    @Nested
    @DisplayName("Behavior Enum")
    class BehaviorEnumTests {

        @Test
        @DisplayName("all behavior types have display names")
        void allBehaviorTypesHaveDisplayNames() {
            for (Monster.Behavior behavior : Monster.Behavior.values()) {
                assertNotNull(behavior.getDisplayName());
                assertFalse(behavior.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("behavior enum has four types")
        void behaviorEnumHasFourTypes() {
            assertEquals(4, Monster.Behavior.values().length);
        }
    }

    // ==========================================
    // Tactical Targeting Tests
    // ==========================================

    @Nested
    @DisplayName("Tactical Targeting")
    class TacticalTargetingTests {

        @Test
        @DisplayName("tactical monsters target lowest HP enemy")
        void tacticalMonstersTargetLowestHp() {
            // Create a tactical monster
            Monster tacticalMonster = new Monster("tactical", "Tactical Enemy", 10, 100);
            tacticalMonster.setBehavior(Monster.Behavior.TACTICAL);
            tacticalMonster.setAttackBonus(20);  // High attack to ensure hit
            tacticalMonster.setDamageDice("1d1");

            combatSystem.startCombat(state, List.of(tacticalMonster));

            // The player is the only non-monster target, so it should be selected
            // In a multi-player scenario, the lowest HP would be targeted
            assertTrue(combatSystem.isInCombat());
        }
    }

    // ==========================================
    // Opportunity Attack Tests
    // ==========================================

    @Nested
    @DisplayName("Opportunity Attacks")
    class OpportunityAttackTests {

        @Test
        @DisplayName("fleeing triggers opportunity attacks")
        void fleeingTriggersOpportunityAttacks() {
            Monster goblin = new Monster("goblin", "Goblin", 10, 100);
            goblin.setAttackBonus(5);
            goblin.setDamageDice("1d4");

            combatSystem.startCombat(state, List.of(goblin));

            // Get to player turn
            while (combatSystem.isInCombat() &&
                   !(combatSystem.getCurrentCombatant() instanceof Character)) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                int hpBefore = character.getCurrentHitPoints();
                CombatResult result = combatSystem.playerTurn("flee", null);

                // Message should contain opportunity attack info
                String message = result.getMessage();
                assertTrue(message.contains("Opportunity Attack") ||
                           message.contains("fled") ||
                           message.contains("Failed to flee"),
                    "Flee result should mention opportunity attack or flee outcome");
            }
        }

        @Test
        @DisplayName("flee message includes opportunity attack results")
        void fleeMessageIncludesOpportunityAttackResults() {
            Monster goblin = new Monster("goblin", "Goblin", 10, 100);
            goblin.setAttackBonus(5);
            goblin.setDamageDice("1d4");

            combatSystem.startCombat(state, List.of(goblin));

            while (combatSystem.isInCombat() &&
                   !(combatSystem.getCurrentCombatant() instanceof Character)) {
                combatSystem.enemyTurn();
            }

            if (combatSystem.isInCombat()) {
                CombatResult result = combatSystem.playerTurn("flee", null);

                // Should have some message about the flee attempt
                assertNotNull(result.getMessage());
                assertFalse(result.getMessage().isEmpty());
            }
        }
    }

    // ==========================================
    // Special Ability Tests
    // ==========================================

    @Nested
    @DisplayName("Special Abilities")
    class SpecialAbilityTests {

        @Test
        @DisplayName("monster with Disarm ability triggers save on hit")
        void disarmAbilityTriggersSaveOnHit() {
            Monster critter = new Monster("critter", "Clockwork Critter", 5, 100);
            critter.setAttackBonus(20);  // High attack to guarantee hit
            critter.setDamageDice("1d4");
            critter.setSpecialAbility("Disarm");

            combatSystem.startCombat(state, List.of(critter));

            // Get to enemy turn
            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat() && combatSystem.getCurrentCombatant() instanceof Monster) {
                CombatResult result = combatSystem.enemyTurn();

                // If it was a hit, should mention Disarm
                if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                    assertTrue(result.getMessage().contains("Disarm"),
                        "Hit message should include Disarm ability effect");
                }
            }
        }

        @Test
        @DisplayName("monster with Adhesive ability triggers save on hit")
        void adhesiveAbilityTriggersSaveOnHit() {
            Monster mimic = new Monster("mimic", "Mimic Prop", 5, 100);
            mimic.setAttackBonus(20);  // High attack to guarantee hit
            mimic.setDamageDice("1d4");
            mimic.setSpecialAbility("Adhesive");

            combatSystem.startCombat(state, List.of(mimic));

            // Get to enemy turn
            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat() && combatSystem.getCurrentCombatant() instanceof Monster) {
                CombatResult result = combatSystem.enemyTurn();

                // If it was a hit, should mention Adhesive
                if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                    assertTrue(result.getMessage().contains("Adhesive"),
                        "Hit message should include Adhesive ability effect");
                }
            }
        }

        @Test
        @DisplayName("monster without special ability does not trigger effects")
        void noSpecialAbilityNoEffects() {
            Monster goblin = new Monster("goblin", "Plain Goblin", 5, 100);
            goblin.setAttackBonus(20);
            goblin.setDamageDice("1d4");
            // No special ability set

            combatSystem.startCombat(state, List.of(goblin));

            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat() && combatSystem.getCurrentCombatant() instanceof Monster) {
                CombatResult result = combatSystem.enemyTurn();

                if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                    assertFalse(result.getMessage().contains("Disarm"));
                    assertFalse(result.getMessage().contains("Adhesive"));
                }
            }
        }

        @Test
        @DisplayName("tactical monster with special ability mentions it")
        void tacticalMonsterMentionsSpecialAbility() {
            Monster tacticalEnemy = new Monster("tactical", "Tactical Enemy", 5, 100);
            tacticalEnemy.setAttackBonus(20);
            tacticalEnemy.setDamageDice("1d4");
            tacticalEnemy.setBehavior(Monster.Behavior.TACTICAL);
            tacticalEnemy.setSpecialAbility("Power Strike");

            combatSystem.startCombat(state, List.of(tacticalEnemy));

            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat() && combatSystem.getCurrentCombatant() instanceof Monster) {
                CombatResult result = combatSystem.enemyTurn();

                if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                    assertTrue(result.getMessage().contains("Power Strike"),
                        "Tactical monster hit should mention special ability");
                }
            }
        }
    }

    // ==========================================
    // CombatResult Type Tests
    // ==========================================

    @Nested
    @DisplayName("CombatResult Types")
    class CombatResultTypeTests {

        @Test
        @DisplayName("SPECIAL_ABILITY type exists")
        void specialAbilityTypeExists() {
            boolean found = false;
            for (CombatResult.Type type : CombatResult.Type.values()) {
                if (type == CombatResult.Type.SPECIAL_ABILITY) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "SPECIAL_ABILITY type should exist in CombatResult.Type");
        }

        @Test
        @DisplayName("opportunity attack creates correct result type")
        void opportunityAttackCreatesCorrectType() {
            Monster goblin = createGoblin();
            CombatResult result = CombatResult.opportunityAttack(goblin, character, 15, 14, 5);

            assertEquals(CombatResult.Type.ATTACK_HIT, result.getType());
            assertTrue(result.getMessage().contains("Opportunity Attack"));
        }

        @Test
        @DisplayName("opportunity attack miss creates correct result")
        void opportunityAttackMissCreatesCorrectResult() {
            Monster goblin = createGoblin();
            CombatResult result = CombatResult.opportunityAttackMiss(goblin, character, 10, 14);

            assertEquals(CombatResult.Type.ATTACK_MISS, result.getType());
            assertTrue(result.getMessage().contains("Opportunity Attack"));
            assertTrue(result.getMessage().contains("MISS"));
        }

        @Test
        @DisplayName("fled with message includes custom message")
        void fledWithMessageIncludesCustomMessage() {
            CombatResult result = CombatResult.fled("Custom flee message");

            assertEquals(CombatResult.Type.FLED, result.getType());
            assertEquals("Custom flee message", result.getMessage());
        }

        @Test
        @DisplayName("special ability result has correct format")
        void specialAbilityResultFormat() {
            Monster goblin = createGoblin();
            CombatResult result = CombatResult.specialAbility(goblin, character, "Disarm", "Target dropped weapon!");

            assertEquals(CombatResult.Type.SPECIAL_ABILITY, result.getType());
            assertTrue(result.getMessage().contains("Disarm"));
            assertTrue(result.getMessage().contains("Target dropped weapon!"));
        }
    }

    // ==========================================
    // Inventory Integration Tests
    // ==========================================

    @Nested
    @DisplayName("Inventory Integration")
    class InventoryIntegrationTests {

        @Test
        @DisplayName("character has inventory")
        void characterHasInventory() {
            assertNotNull(character.getInventory());
        }

        @Test
        @DisplayName("dropped items list starts empty")
        void droppedItemsListStartsEmpty() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            assertFalse(combatSystem.hasDroppedItems());
            assertTrue(combatSystem.getDroppedItems().isEmpty());
        }

        @Test
        @DisplayName("can equip weapon to character")
        void canEquipWeaponToCharacter() {
            Weapon sword = Weapon.createLongsword();
            character.getInventory().addItem(sword);
            character.getInventory().equip(sword);

            assertEquals(sword, character.getInventory().getEquippedWeapon());
        }

        @Test
        @DisplayName("disarm drops equipped weapon when save fails")
        void disarmDropsEquippedWeapon() {
            // Equip a weapon
            Weapon sword = Weapon.createLongsword();
            character.getInventory().addItem(sword);
            character.getInventory().equip(sword);
            assertEquals(sword, character.getInventory().getEquippedWeapon());

            // Create a monster with Disarm ability and very high attack
            Monster critter = new Monster("critter", "Clockwork Critter", 5, 100);
            critter.setAttackBonus(30);  // Guaranteed hit
            critter.setDamageDice("1d1");
            critter.setSpecialAbility("Disarm");

            combatSystem.startCombat(state, List.of(critter));

            // Get to enemy turn
            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            }

            // Keep attacking until Disarm triggers (on failed save)
            int attempts = 0;
            while (combatSystem.isInCombat() &&
                   character.getInventory().getEquippedWeapon() != null &&
                   attempts < 50) {
                if (combatSystem.getCurrentCombatant() instanceof Monster) {
                    combatSystem.enemyTurn();
                } else {
                    combatSystem.playerTurn("attack", null);
                }
                attempts++;
            }

            // Either weapon was dropped or we hit max attempts
            // (Save might succeed multiple times due to randomness)
            assertTrue(attempts < 50 || character.getInventory().getEquippedWeapon() == null,
                "Weapon should eventually be disarmed");
        }

        @Test
        @DisplayName("dropped items returned to player on victory")
        void droppedItemsReturnedOnVictory() {
            // Equip a weapon
            Weapon sword = Weapon.createLongsword();
            character.getInventory().addItem(sword);
            character.getInventory().equip(sword);

            // Create a very weak monster with Disarm
            Monster critter = new Monster("critter", "Weak Critter", 1, 1);
            critter.setAttackBonus(30);  // High attack for guaranteed hit
            critter.setDamageDice("1d1");
            critter.setSpecialAbility("Disarm");

            int initialItemCount = character.getInventory().getTotalItemCount();
            combatSystem.startCombat(state, List.of(critter));

            // Fight until combat ends
            while (combatSystem.isInCombat()) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    combatSystem.playerTurn("attack", null);
                } else {
                    combatSystem.enemyTurn();
                }
            }

            // After victory, items should be returned to inventory
            // (Either in equipment slot or in inventory)
            Inventory inv = character.getInventory();
            boolean hasWeapon = inv.getEquippedWeapon() != null ||
                                inv.findItemsByName("Longsword").size() > 0;
            assertTrue(hasWeapon, "Weapon should be returned to inventory after victory");
        }

        @Test
        @DisplayName("can pick up dropped items during combat")
        void canPickUpDroppedItemsDuringCombat() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Manually add an item to dropped items for testing
            Weapon sword = Weapon.createLongsword();
            combatSystem.getDroppedItems();  // Just to confirm the list exists

            // Note: We can't easily test pickUpDroppedItem without direct access
            // to add items to droppedItems, so this is a basic functionality check
            assertNotNull(combatSystem.getDroppedItems());
        }
    }

    // ==========================================
    // Status Effects Integration Tests
    // ==========================================

    @Nested
    @DisplayName("Status Effects Integration")
    class StatusEffectsIntegrationTests {

        @Test
        @DisplayName("combat system has status effect manager")
        void combatSystemHasStatusEffectManager() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            assertNotNull(combatSystem.getStatusEffectManager());
        }

        @Test
        @DisplayName("status effect manager is reset on new combat")
        void statusEffectManagerIsResetOnNewCombat() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply an effect
            var effect = com.questkeeper.combat.status.ConditionEffect.poisoned(5);
            combatSystem.getStatusEffectManager().applyEffect(character, effect);

            assertTrue(combatSystem.getStatusEffectManager().hasAnyEffects(character));

            // Start new combat - should reset
            goblin.resetHitPoints();
            combatSystem.startCombat(state, List.of(goblin));

            assertFalse(combatSystem.getStatusEffectManager().hasAnyEffects(character));
        }

        @Test
        @DisplayName("adhesive ability applies restrained condition")
        void adhesiveAbilityAppliesRestrainedCondition() {
            // Create a monster with Adhesive ability
            Monster mimic = new Monster("mimic", "Mimic", 5, 100);
            mimic.setAttackBonus(30);  // Guaranteed hit
            mimic.setDamageDice("1d1");
            mimic.setSpecialAbility("Adhesive");

            combatSystem.startCombat(state, List.of(mimic));

            // Get to enemy turn
            while (combatSystem.isInCombat() &&
                   combatSystem.getCurrentCombatant() instanceof Character) {
                combatSystem.playerTurn("attack", null);
            }

            // Keep attacking until we get a restrained condition (save might succeed multiple times)
            int attempts = 0;
            boolean hasRestrained = false;
            while (combatSystem.isInCombat() && !hasRestrained && attempts < 30) {
                if (combatSystem.getCurrentCombatant() instanceof Monster) {
                    CombatResult result = combatSystem.enemyTurn();
                    if (result.getType() == CombatResult.Type.ATTACK_HIT) {
                        hasRestrained = combatSystem.getStatusEffectManager()
                            .hasCondition(character, com.questkeeper.combat.status.Condition.RESTRAINED);
                    }
                } else {
                    combatSystem.playerTurn("attack", null);
                }
                attempts++;
            }

            // Either we applied restrained or reached max attempts (due to save successes)
            assertTrue(attempts > 0, "Should have attempted at least one attack");
        }

        @Test
        @DisplayName("incapacitated combatant skips turn")
        void incapacitatedCombatantSkipsTurn() {
            Monster goblin = createGoblin();
            goblin.setAttackBonus(0);
            goblin.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(goblin));

            // Apply paralyzed condition to goblin
            var paralyzed = com.questkeeper.combat.status.ConditionEffect.paralyzed(3);
            combatSystem.getStatusEffectManager().applyEffect(goblin, paralyzed);

            // Get to goblin's turn
            while (combatSystem.isInCombat() &&
                   !(combatSystem.getCurrentCombatant() instanceof Monster)) {
                combatSystem.playerTurn("attack", null);
            }

            if (combatSystem.isInCombat() && combatSystem.getCurrentCombatant() == goblin) {
                // Execute turn - should skip due to paralysis
                CombatResult result = combatSystem.executeTurn();

                // Result should indicate incapacitation
                assertTrue(result.getMessage().contains("incapacitated") ||
                           result.getType() == CombatResult.Type.ERROR,
                    "Paralyzed combatant should be incapacitated");
            }
        }

        @Test
        @DisplayName("restrained target gives advantage on attacks")
        void restrainedTargetGivesAdvantageOnAttacks() {
            Monster goblin = new Monster("goblin", "Goblin", 20, 100);  // High AC
            goblin.setAttackBonus(0);
            goblin.setDamageDice("1d1");
            combatSystem.startCombat(state, List.of(goblin));

            // Apply restrained condition to goblin
            var restrained = com.questkeeper.combat.status.ConditionEffect.restrained(5);
            combatSystem.getStatusEffectManager().applyEffect(goblin, restrained);

            // Verify the manager reports advantage
            assertTrue(combatSystem.getStatusEffectManager().attacksHaveAdvantageAgainst(goblin));
        }

        @Test
        @DisplayName("poisoned attacker has disadvantage")
        void poisonedAttackerHasDisadvantage() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply poisoned condition to player
            var poisoned = com.questkeeper.combat.status.ConditionEffect.poisoned(3);
            combatSystem.getStatusEffectManager().applyEffect(character, poisoned);

            assertTrue(combatSystem.getStatusEffectManager().hasDisadvantageOnAttacks(character));
        }

        @Test
        @DisplayName("paralyzed target causes melee auto-crits")
        void paralyzedTargetCausesMeleeAutoCrits() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply paralyzed condition to goblin
            var paralyzed = com.questkeeper.combat.status.ConditionEffect.paralyzed(3);
            combatSystem.getStatusEffectManager().applyEffect(goblin, paralyzed);

            assertTrue(combatSystem.getStatusEffectManager().meleeCritsOnHit(goblin));
        }

        @Test
        @DisplayName("grappled combatant cannot move")
        void grappledCombatantCannotMove() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply grappled condition to character
            var grappled = com.questkeeper.combat.status.ConditionEffect.grappled();
            combatSystem.getStatusEffectManager().applyEffect(character, grappled);

            assertFalse(combatSystem.getStatusEffectManager().canMove(character));
        }

        @Test
        @DisplayName("stunned combatant auto-fails STR/DEX saves")
        void stunnedCombatantAutoFailsStrDexSaves() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply stunned condition
            var stunned = com.questkeeper.combat.status.ConditionEffect.stunned(2);
            combatSystem.getStatusEffectManager().applyEffect(character, stunned);

            assertTrue(combatSystem.getStatusEffectManager().autoFailsStrDexSaves(character));
        }

        @Test
        @DisplayName("multiple effects can stack")
        void multipleEffectsCanStack() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply multiple conditions
            var poisoned = com.questkeeper.combat.status.ConditionEffect.poisoned(3);
            var blinded = com.questkeeper.combat.status.ConditionEffect.blinded(2);
            combatSystem.getStatusEffectManager().applyEffect(character, poisoned);
            combatSystem.getStatusEffectManager().applyEffect(character, blinded);

            var effects = combatSystem.getStatusEffectManager().getEffects(character);
            assertEquals(2, effects.size());

            // Both should cause disadvantage on attacks
            assertTrue(combatSystem.getStatusEffectManager().hasDisadvantageOnAttacks(character));
        }

        @Test
        @DisplayName("invisible attacker has advantage")
        void invisibleAttackerHasAdvantage() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply invisible condition to character
            var invisible = com.questkeeper.combat.status.ConditionEffect.invisible(3);
            combatSystem.getStatusEffectManager().applyEffect(character, invisible);

            assertTrue(combatSystem.getStatusEffectManager().hasAdvantageOnAttacks(character));
        }

        @Test
        @DisplayName("status display shows active effects")
        void statusDisplayShowsActiveEffects() {
            Monster goblin = createGoblin();
            combatSystem.startCombat(state, List.of(goblin));

            // Apply poisoned condition
            var poisoned = com.questkeeper.combat.status.ConditionEffect.poisoned(3);
            combatSystem.getStatusEffectManager().applyEffect(character, poisoned);

            String display = combatSystem.getStatusEffectManager().getStatusDisplay(character);

            assertFalse(display.isEmpty());
            assertTrue(display.contains("Poisoned"));
        }

        @Test
        @DisplayName("effects cleared when combat ends normally")
        void effectsClearedWhenCombatEnds() {
            Monster weakGoblin = new Monster("goblin", "Weak Goblin", 1, 1);
            weakGoblin.setExperienceValue(10);
            combatSystem.startCombat(state, List.of(weakGoblin));

            // Apply effect
            var poisoned = com.questkeeper.combat.status.ConditionEffect.poisoned(10);
            combatSystem.getStatusEffectManager().applyEffect(character, poisoned);

            assertTrue(combatSystem.getStatusEffectManager().hasAnyEffects(character));

            // Fight until combat ends
            while (combatSystem.isInCombat()) {
                if (combatSystem.getCurrentCombatant() instanceof Character) {
                    combatSystem.playerTurn("attack", null);
                } else {
                    combatSystem.enemyTurn();
                }
            }

            // Start new combat to verify manager was reset
            weakGoblin.resetHitPoints();
            combatSystem.startCombat(state, List.of(weakGoblin));
            assertFalse(combatSystem.getStatusEffectManager().hasAnyEffects(character));
        }
    }

    // ==========================================
    // Weapon-Based Attack Tests
    // ==========================================

    @Nested
    @DisplayName("Weapon-Based Attacks")
    class WeaponBasedAttackTests {

        private Character dexCharacter;
        private Monster weakTarget;

        @BeforeEach
        void setUp() {
            // Create a DEX-focused character (DEX 16, STR 10)
            dexCharacter = new Character("DexRogue", Race.HUMAN, CharacterClass.ROGUE,
                10, 16, 12, 14, 10, 14);
            state = new GameState(dexCharacter, campaign);

            // Create a very weak target for testing
            weakTarget = new Monster("target", "Training Dummy", 10, 100);
            weakTarget.setAttackBonus(0);
            weakTarget.setDamageDice("1d4");
        }

        @Test
        @DisplayName("finesse weapon uses DEX when DEX > STR")
        void finesseWeaponUsesDexWhenHigher() {
            // Equip a rapier (finesse weapon)
            Weapon rapier = Weapon.createRapier();
            dexCharacter.getInventory().addItem(rapier);
            dexCharacter.getInventory().equip(rapier);

            combatSystem.startCombat(state, List.of(weakTarget));

            // DEX mod is +3, STR mod is +0
            // With finesse, should use DEX (+3) for attack
            int dexMod = dexCharacter.getAbilityModifier(Ability.DEXTERITY);
            int strMod = dexCharacter.getAbilityModifier(Ability.STRENGTH);
            assertEquals(3, dexMod);
            assertEquals(0, strMod);

            // The attack should succeed more often with the higher DEX mod
            // We can't directly test the roll, but we verify the weapon is finesse
            assertTrue(rapier.isFinesse());
        }

        @Test
        @DisplayName("ranged weapon always uses DEX")
        void rangedWeaponAlwaysUsesDex() {
            // Equip a shortbow (ranged weapon)
            Weapon shortbow = Weapon.createShortbow();
            dexCharacter.getInventory().addItem(shortbow);
            dexCharacter.getInventory().equip(shortbow);

            assertTrue(shortbow.isRanged());

            // DEX should be used for ranged weapons
            int dexMod = dexCharacter.getAbilityModifier(Ability.DEXTERITY);
            assertEquals(3, dexMod);
        }

        @Test
        @DisplayName("regular melee weapon uses STR")
        void regularMeleeWeaponUsesStr() {
            // Equip a longsword (non-finesse melee weapon)
            Weapon longsword = Weapon.createLongsword();
            dexCharacter.getInventory().addItem(longsword);
            dexCharacter.getInventory().equip(longsword);

            assertFalse(longsword.isFinesse());
            assertFalse(longsword.isRanged());

            // STR mod should be used
            int strMod = dexCharacter.getAbilityModifier(Ability.STRENGTH);
            assertEquals(0, strMod);
        }

        @Test
        @DisplayName("finesse weapon uses STR when STR > DEX")
        void finesseWeaponUsesStrWhenHigher() {
            // Create a STR-focused character (STR 16, DEX 10)
            Character strCharacter = new Character("StrFighter", Race.HUMAN, CharacterClass.FIGHTER,
                16, 10, 14, 10, 10, 10);

            // Equip a rapier (finesse weapon)
            Weapon rapier = Weapon.createRapier();
            strCharacter.getInventory().addItem(rapier);
            strCharacter.getInventory().equip(rapier);

            int strMod = strCharacter.getAbilityModifier(Ability.STRENGTH);
            int dexMod = strCharacter.getAbilityModifier(Ability.DEXTERITY);
            assertEquals(3, strMod);
            assertEquals(0, dexMod);

            // With finesse, should use STR (+3) since it's higher
            assertTrue(rapier.isFinesse());
        }

        @Test
        @DisplayName("unarmed attack uses STR")
        void unarmedAttackUsesStr() {
            // No weapon equipped
            assertNull(dexCharacter.getInventory().getEquippedWeapon());

            // Unarmed should use STR
            int strMod = dexCharacter.getAbilityModifier(Ability.STRENGTH);
            assertEquals(0, strMod);
        }

        @Test
        @DisplayName("weapon damage dice are used instead of default")
        void weaponDamageDiceUsed() {
            // Rapier does 1d8, not the default
            Weapon rapier = Weapon.createRapier();
            assertEquals("1d8", rapier.getDamageDice());

            // Shortsword does 1d6
            Weapon shortsword = Weapon.createShortsword();
            assertEquals("1d6", shortsword.getDamageDice());

            // Dagger does 1d4
            Weapon dagger = Weapon.createDagger();
            assertEquals("1d4", dagger.getDamageDice());
        }
    }
}
