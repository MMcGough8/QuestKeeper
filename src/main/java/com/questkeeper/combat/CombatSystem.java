package com.questkeeper.combat;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.core.Dice;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import com.questkeeper.state.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages turn-based combat encounters.
 *
 * Handles initiative rolling, turn order, attack resolution,
 * and combat end conditions. All rolls are displayed transparently.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class CombatSystem {

    private static final String DEFAULT_PLAYER_WEAPON = "1d8";
    private static final int FLEE_DC = 10;
    private static final int ENEMY_FLEE_DC = 12;

    private List<Combatant> participants;
    private List<Combatant> initiative;
    private Map<Combatant, Integer> initiativeRolls;
    private Map<Combatant, Combatant> lastAttacker;  // Tracks who hit each combatant last
    private List<Item> droppedItems;  // Items dropped during combat (e.g., from Disarm)
    private int currentTurn;
    private GameState currentState;
    private boolean inCombat;
    private boolean playerFled;

    public CombatSystem() {
        this.participants = new ArrayList<>();
        this.initiative = new ArrayList<>();
        this.initiativeRolls = new HashMap<>();
        this.lastAttacker = new HashMap<>();
        this.droppedItems = new ArrayList<>();
        this.currentTurn = 0;
        this.currentState = null;
        this.inCombat = false;
        this.playerFled = false;
    }

    // ==========================================
    // Combat Lifecycle
    // ==========================================

    /**
     * Starts combat with the given enemies.
     */
    public CombatResult startCombat(GameState state, List<Monster> enemies) {
        if (state == null) {
            return CombatResult.error("No active game state.");
        }

        if (enemies == null || enemies.isEmpty()) {
            return CombatResult.error("No enemies to fight.");
        }

        this.currentState = state;
        this.participants = new ArrayList<>();
        this.initiative = new ArrayList<>();
        this.initiativeRolls = new HashMap<>();
        this.lastAttacker = new HashMap<>();
        this.droppedItems = new ArrayList<>();
        this.currentTurn = 0;
        this.playerFled = false;

        // Add player
        participants.add(state.getCharacter());

        // Add enemies
        for (Monster enemy : enemies) {
            enemy.resetHitPoints();
            participants.add(enemy);
        }

        // Roll and sort initiative
        rollInitiative();

        this.inCombat = true;

        // Build initiative message
        StringBuilder sb = new StringBuilder();
        sb.append("Combat begins!\n\nInitiative Rolls:");
        for (Combatant c : initiative) {
            sb.append(String.format("\n  %s: %d", c.getName(), initiativeRolls.get(c)));
        }

        return CombatResult.combatStart(new ArrayList<>(initiative), sb.toString());
    }

    /**
     * Executes the current combatant's turn.
     * For enemies, automatically performs AI action.
     * For player, returns turn start notification.
     */
    public CombatResult executeTurn() {
        if (!inCombat) {
            return CombatResult.error("Not in combat.");
        }

        Combatant current = getCurrentCombatant();
        if (current == null) {
            return CombatResult.error("No current combatant.");
        }

        // Skip dead combatants
        while (!current.isAlive()) {
            advanceTurn();
            current = getCurrentCombatant();
            if (current == null) {
                return checkEndConditions();
            }
        }

        // If it's an enemy, execute AI turn
        if (isEnemy(current)) {
            return enemyTurn();
        }

        // Player turn - return notification
        return CombatResult.turnStart(current);
    }

    /**
     * Processes the player's turn action.
     */
    public CombatResult playerTurn(String action, String target) {
        if (!inCombat) {
            return CombatResult.error("Not in combat.");
        }

        Combatant current = getCurrentCombatant();
        if (current == null || isEnemy(current)) {
            return CombatResult.error("It's not your turn.");
        }

        if (action == null || action.trim().isEmpty()) {
            return CombatResult.error("What do you want to do? (attack, flee)");
        }

        String normalizedAction = action.trim().toLowerCase();

        switch (normalizedAction) {
            case "attack":
            case "hit":
            case "strike":
                return handlePlayerAttack(target);

            case "flee":
            case "run":
            case "escape":
                return handleFlee();

            default:
                return CombatResult.error(
                    String.format("Unknown action: %s. Try: attack, flee", action));
        }
    }

    /**
     * Executes an enemy's turn using behavior-based AI.
     */
    public CombatResult enemyTurn() {
        if (!inCombat) {
            return CombatResult.error("Not in combat.");
        }

        Combatant current = getCurrentCombatant();
        if (current == null || !isEnemy(current)) {
            return CombatResult.error("It's not an enemy's turn.");
        }

        Monster monster = (Monster) current;
        Monster.Behavior behavior = monster.getBehavior();

        // Check for flee behavior based on HP
        if (shouldEnemyFlee(monster, behavior)) {
            CombatResult fleeResult = attemptEnemyFlee(monster);
            if (fleeResult != null) {
                advanceTurn();
                return fleeResult;
            }
            // Failed to flee, continue with attack
        }

        // Determine target based on aggro
        Combatant target = selectTarget(monster);
        if (target == null || !target.isAlive()) {
            return checkEndConditions();
        }

        CombatResult attackResult = processAttack(monster, target);

        // Check if target was defeated
        if (!target.isAlive() && target == getPlayer()) {
            advanceTurn();
            return CombatResult.playerDefeated(target);
        }

        advanceTurn();
        return attackResult;
    }

    /**
     * Determines if an enemy should try to flee based on behavior.
     */
    private boolean shouldEnemyFlee(Monster monster, Monster.Behavior behavior) {
        switch (behavior) {
            case COWARDLY:
                return monster.isBloodied();  // Flee at 50% HP
            case DEFENSIVE:
                return monster.getHpPercentage() <= 25;  // Flee at 25% HP
            case AGGRESSIVE:
            case TACTICAL:
            default:
                return false;  // Never flee
        }
    }

    /**
     * Attempts enemy flee with DEX check.
     */
    private CombatResult attemptEnemyFlee(Monster monster) {
        int dexMod = monster.getDexterityMod();
        boolean success = Dice.checkAgainstDC(dexMod, ENEMY_FLEE_DC);

        if (success) {
            // Remove monster from combat
            participants.remove(monster);
            initiative.remove(monster);

            String message = String.format("%s flees from combat! [DEX check vs DC %d - SUCCESS]",
                monster.getName(), ENEMY_FLEE_DC);
            return CombatResult.error(message);  // Use error type for info message
        }
        return null;  // Failed to flee
    }

    /**
     * Selects target based on aggro and behavior.
     */
    private Combatant selectTarget(Monster monster) {
        // Check if someone hit this monster - target them first (aggro)
        Combatant attacker = lastAttacker.get(monster);
        if (attacker != null && attacker.isAlive()) {
            return attacker;
        }

        // Tactical behavior: target lowest HP enemy
        if (monster.getBehavior() == Monster.Behavior.TACTICAL) {
            Combatant lowestHpTarget = findLowestHpTarget();
            if (lowestHpTarget != null) {
                return lowestHpTarget;
            }
        }

        // Default: attack the player
        return getPlayer();
    }

    /**
     * Finds the living non-monster combatant with the lowest HP.
     */
    private Combatant findLowestHpTarget() {
        Combatant lowestHpTarget = null;
        int lowestHp = Integer.MAX_VALUE;

        for (Combatant c : participants) {
            if (!(c instanceof Monster) && c.isAlive()) {
                if (c.getCurrentHitPoints() < lowestHp) {
                    lowestHp = c.getCurrentHitPoints();
                    lowestHpTarget = c;
                }
            }
        }
        return lowestHpTarget;
    }

    /**
     * Ends combat and returns final result.
     */
    public CombatResult endCombat() {
        if (!inCombat) {
            return CombatResult.error("Not in combat.");
        }

        inCombat = false;
        int xp = calculateXpReward();
        participants.clear();
        initiative.clear();
        initiativeRolls.clear();
        currentTurn = 0;
        currentState = null;

        return CombatResult.victory(xp);
    }

    // ==========================================
    // Combat Actions
    // ==========================================

    /**
     * Processes an attack from attacker to target.
     */
    public CombatResult processAttack(Combatant attacker, Combatant target) {
        if (attacker == null || target == null) {
            return CombatResult.error("Invalid attacker or target.");
        }

        if (!attacker.isAlive()) {
            return CombatResult.error(attacker.getName() + " cannot attack while unconscious.");
        }

        if (!target.isAlive()) {
            return CombatResult.error(target.getName() + " is already defeated.");
        }

        int attackRoll;
        int damage;
        int targetAC = target.getArmorClass();

        if (attacker instanceof Monster monster) {
            // Monster attack
            attackRoll = monster.rollAttack();
            if (attackRoll >= targetAC) {
                damage = monster.rollDamage();
                target.takeDamage(damage);

                // Track aggro - target remembers who hit them
                lastAttacker.put(target, attacker);

                // Check for special ability on hit
                String specialEffect = processSpecialAbilityOnHit(monster, target);

                return CombatResult.attackHit(attacker, target, attackRoll, targetAC, damage, specialEffect);
            } else {
                return CombatResult.attackMiss(attacker, target, attackRoll, targetAC);
            }
        } else if (attacker instanceof Character character) {
            // Player attack: d20 + STR mod + proficiency
            int strMod = character.getAbilityModifier(Ability.STRENGTH);
            int profBonus = character.getProficiencyBonus();
            attackRoll = Dice.rollD20() + strMod + profBonus;

            if (attackRoll >= targetAC) {
                // Damage: 1d8 + STR mod (simulating longsword)
                damage = Dice.parse(DEFAULT_PLAYER_WEAPON) + strMod;
                damage = Math.max(1, damage); // Minimum 1 damage
                target.takeDamage(damage);

                // Track aggro - target remembers who hit them
                lastAttacker.put(target, attacker);

                return CombatResult.attackHit(attacker, target, attackRoll, targetAC, damage);
            } else {
                return CombatResult.attackMiss(attacker, target, attackRoll, targetAC);
            }
        }

        return CombatResult.error("Unknown combatant type.");
    }

    // ==========================================
    // State Accessors
    // ==========================================

    /**
     * Checks if currently in combat.
     */
    public boolean isInCombat() {
        return inCombat;
    }

    /**
     * Gets the combatant whose turn it is.
     */
    public Combatant getCurrentCombatant() {
        if (initiative.isEmpty() || currentTurn < 0 || currentTurn >= initiative.size()) {
            return null;
        }
        return initiative.get(currentTurn);
    }

    /**
     * Gets all combat participants.
     */
    public List<Combatant> getParticipants() {
        return new ArrayList<>(participants);
    }

    /**
     * Gets the initiative order.
     */
    public List<Combatant> getInitiativeOrder() {
        return new ArrayList<>(initiative);
    }

    /**
     * Gets all enemies in combat.
     */
    public List<Combatant> getEnemies() {
        List<Combatant> enemies = new ArrayList<>();
        for (Combatant c : participants) {
            if (c instanceof Monster) {
                enemies.add(c);
            }
        }
        return enemies;
    }

    /**
     * Gets all living enemies.
     */
    public List<Combatant> getLivingEnemies() {
        List<Combatant> living = new ArrayList<>();
        for (Combatant c : getEnemies()) {
            if (c.isAlive()) {
                living.add(c);
            }
        }
        return living;
    }

    /**
     * Gets the player combatant.
     */
    public Combatant getPlayer() {
        for (Combatant c : participants) {
            if (c instanceof Character) {
                return c;
            }
        }
        return null;
    }

    /**
     * Gets the current turn index.
     */
    public int getCurrentTurnIndex() {
        return currentTurn;
    }

    /**
     * Gets the initiative roll for a combatant.
     */
    public int getInitiativeRoll(Combatant combatant) {
        return initiativeRolls.getOrDefault(combatant, 0);
    }

    /**
     * Gets the last attacker of a combatant (for aggro tracking).
     */
    public Combatant getLastAttacker(Combatant target) {
        return lastAttacker.get(target);
    }

    /**
     * Gets items dropped during combat (e.g., from Disarm ability).
     */
    public List<Item> getDroppedItems() {
        return new ArrayList<>(droppedItems);
    }

    /**
     * Checks if there are any dropped items in combat.
     */
    public boolean hasDroppedItems() {
        return !droppedItems.isEmpty();
    }

    /**
     * Allows a combatant to pick up a dropped item during combat.
     * Returns true if successful.
     */
    public boolean pickUpDroppedItem(Combatant combatant, Item item) {
        if (!droppedItems.contains(item)) {
            return false;
        }

        if (combatant instanceof Character character) {
            if (character.getInventory().addItem(item)) {
                droppedItems.remove(item);
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    /**
     * Rolls initiative for all participants and sorts by roll.
     */
    private void rollInitiative() {
        initiativeRolls.clear();

        for (Combatant c : participants) {
            int roll = c.rollInitiative();
            initiativeRolls.put(c, roll);
        }

        // Sort descending by initiative roll
        initiative = new ArrayList<>(participants);
        initiative.sort((a, b) -> {
            int rollDiff = initiativeRolls.get(b) - initiativeRolls.get(a);
            if (rollDiff != 0) return rollDiff;
            // Tie-breaker: higher DEX modifier goes first
            return b.getInitiativeModifier() - a.getInitiativeModifier();
        });

        currentTurn = 0;
    }

    /**
     * Advances to the next combatant's turn.
     */
    private void advanceTurn() {
        currentTurn = (currentTurn + 1) % initiative.size();
    }

    /**
     * Checks if a combatant is an enemy.
     */
    private boolean isEnemy(Combatant c) {
        return c instanceof Monster;
    }

    /**
     * Checks if combat should end.
     */
    private CombatResult checkEndConditions() {
        if (playerFled) {
            inCombat = false;
            return CombatResult.fled();
        }

        // Check if all enemies are dead
        boolean allEnemiesDead = getEnemies().stream().noneMatch(Combatant::isAlive);
        if (allEnemiesDead) {
            int xp = calculateXpReward();
            inCombat = false;

            // Award XP to player and return dropped items
            Combatant player = getPlayer();
            if (player instanceof Character character) {
                character.addExperience(xp);

                // Return any dropped items to player's inventory
                for (Item item : droppedItems) {
                    character.getInventory().addItem(item);
                }
                droppedItems.clear();
            }

            return CombatResult.victory(xp);
        }

        // Check if player is dead
        Combatant player = getPlayer();
        if (player == null || !player.isAlive()) {
            inCombat = false;
            return CombatResult.playerDefeated(player);
        }

        return null; // Combat continues
    }

    /**
     * Calculates total XP reward from defeated enemies.
     */
    private int calculateXpReward() {
        int totalXp = 0;
        for (Combatant c : getEnemies()) {
            if (c instanceof Monster monster && !monster.isAlive()) {
                totalXp += monster.getExperienceValue();
            }
        }
        return totalXp;
    }

    /**
     * Handles player attack action.
     */
    private CombatResult handlePlayerAttack(String target) {
        List<Combatant> livingEnemies = getLivingEnemies();
        if (livingEnemies.isEmpty()) {
            return checkEndConditions();
        }

        Combatant targetEnemy;
        if (target == null || target.trim().isEmpty()) {
            // Default: attack first living enemy
            targetEnemy = livingEnemies.get(0);
        } else {
            // Find enemy by name
            Optional<Combatant> found = findEnemyByName(target);
            if (found.isEmpty()) {
                return CombatResult.error(
                    String.format("No enemy named '%s'. Enemies: %s",
                        target, getEnemyNames()));
            }
            targetEnemy = found.get();
        }

        CombatResult attackResult = processAttack(getPlayer(), targetEnemy);

        // Check if enemy was defeated
        if (!targetEnemy.isAlive()) {
            advanceTurn();

            // Check for victory
            CombatResult endResult = checkEndConditions();
            if (endResult != null) {
                return endResult;
            }

            return CombatResult.enemyDefeated((Monster) targetEnemy);
        }

        advanceTurn();
        return attackResult;
    }

    /**
     * Handles flee action with opportunity attacks.
     */
    private CombatResult handleFlee() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player to flee.");
        }

        StringBuilder fleeMessage = new StringBuilder();

        // Opportunity attacks from all living enemies
        List<Combatant> enemies = getLivingEnemies();
        for (Combatant enemy : enemies) {
            if (enemy instanceof Monster monster) {
                CombatResult oppAttack = processOpportunityAttack(monster, player);
                if (oppAttack != null) {
                    fleeMessage.append(oppAttack.getMessage()).append("\n");
                }

                // Check if player died from opportunity attack
                if (!player.isAlive()) {
                    inCombat = false;
                    return CombatResult.playerDefeated(player);
                }
            }
        }

        // Flee check: DEX check vs DC 10
        int dexMod = player.getAbilityModifier(Ability.DEXTERITY);
        boolean success = Dice.checkAgainstDC(dexMod, FLEE_DC);

        if (success) {
            playerFled = true;
            inCombat = false;
            fleeMessage.append("You fled from combat!");
            return CombatResult.fled(fleeMessage.toString().trim());
        } else {
            advanceTurn();
            fleeMessage.append(String.format("Failed to flee! [DEX check vs DC %d]", FLEE_DC));
            return CombatResult.error(fleeMessage.toString().trim());
        }
    }

    /**
     * Processes an opportunity attack (triggered by fleeing).
     */
    private CombatResult processOpportunityAttack(Monster monster, Combatant target) {
        int attackRoll = monster.rollAttack();
        int targetAC = target.getArmorClass();

        if (attackRoll >= targetAC) {
            int damage = monster.rollDamage();
            target.takeDamage(damage);
            return CombatResult.opportunityAttack(monster, target, attackRoll, targetAC, damage);
        } else {
            return CombatResult.opportunityAttackMiss(monster, target, attackRoll, targetAC);
        }
    }

    /**
     * Finds an enemy by name (case-insensitive partial match).
     */
    private Optional<Combatant> findEnemyByName(String name) {
        String searchTerm = name.trim().toLowerCase();
        for (Combatant c : getLivingEnemies()) {
            if (c.getName().toLowerCase().equals(searchTerm) ||
                c.getName().toLowerCase().contains(searchTerm)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets comma-separated list of enemy names.
     */
    private String getEnemyNames() {
        List<String> names = new ArrayList<>();
        for (Combatant c : getLivingEnemies()) {
            names.add(c.getName());
        }
        return String.join(", ", names);
    }

    // ==========================================
    // Special Ability Handling
    // ==========================================

    /**
     * Processes special ability effects when a monster hits a target.
     * Returns a description of the effect, or null if no ability triggers.
     */
    private String processSpecialAbilityOnHit(Monster monster, Combatant target) {
        if (!monster.hasSpecialAbility()) {
            return null;
        }

        String ability = monster.getSpecialAbility().toLowerCase();

        // Disarm ability (Clockwork Critter)
        if (ability.contains("disarm")) {
            return processDisarmAbility(monster, target);
        }

        // Adhesive ability (Mimic Prop)
        if (ability.contains("adhesive")) {
            return processAdhesiveAbility(monster, target);
        }

        // Generic special ability - just mention it triggered
        if (monster.getBehavior() == Monster.Behavior.TACTICAL) {
            return String.format("[%s triggered!]", monster.getSpecialAbility());
        }

        return null;
    }

    /**
     * Processes the Disarm ability - target must make DEX save or drop their weapon.
     */
    private String processDisarmAbility(Monster monster, Combatant target) {
        int dc = 11;  // Default DC for Disarm
        int dexMod = 0;

        if (target instanceof Character character) {
            dexMod = character.getAbilityModifier(Ability.DEXTERITY);
        }

        boolean saved = Dice.checkAgainstDC(dexMod, dc);

        if (saved) {
            return String.format("[Disarm: DEX save DC %d - SAVED!]", dc);
        } else {
            // Actually drop the weapon from inventory
            if (target instanceof Character character) {
                Inventory inventory = character.getInventory();
                Weapon weapon = inventory.getEquippedWeapon();

                if (weapon != null) {
                    // Unequip the weapon (puts it in inventory)
                    inventory.unequip(Inventory.EquipmentSlot.MAIN_HAND);
                    // Remove from inventory and add to dropped items
                    inventory.removeItem(weapon);
                    droppedItems.add(weapon);

                    return String.format("[Disarm: DEX save DC %d - FAILED! %s drops %s!]",
                        dc, target.getName(), weapon.getName());
                } else {
                    return String.format("[Disarm: DEX save DC %d - FAILED! (no weapon equipped)]", dc);
                }
            }
            return String.format("[Disarm: DEX save DC %d - FAILED! %s drops a held item!]",
                dc, target.getName());
        }
    }

    /**
     * Processes the Adhesive ability - target is stuck unless they pass STR save.
     */
    private String processAdhesiveAbility(Monster monster, Combatant target) {
        int dc = 13;  // Default DC for Adhesive
        int strMod = 0;

        if (target instanceof Character character) {
            strMod = character.getAbilityModifier(Ability.STRENGTH);
        }

        boolean saved = Dice.checkAgainstDC(strMod, dc);

        if (saved) {
            return String.format("[Adhesive: STR save DC %d - SAVED!]", dc);
        } else {
            // TODO: Apply restrained condition when status effects are implemented
            return String.format("[Adhesive: STR save DC %d - FAILED! %s is stuck!]",
                dc, target.getName());
        }
    }
}
