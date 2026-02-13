package com.questkeeper.combat;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.features.ActivatedFeature;
import com.questkeeper.character.features.FighterFeatures;
import com.questkeeper.character.features.FightingStyle;
import com.questkeeper.character.features.RogueFeatures;
import com.questkeeper.character.features.BarbarianFeatures;
import com.questkeeper.character.features.MonkFeatures;
import com.questkeeper.character.features.PaladinFeatures;
import com.questkeeper.character.features.RangerFeatures;
import com.questkeeper.combat.status.Condition;
import com.questkeeper.combat.status.ConditionEffect;
import com.questkeeper.combat.status.StatusEffectManager;
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
    private StatusEffectManager statusEffectManager;  // Tracks status effects on combatants
    private int currentTurn;
    private GameState currentState;
    private boolean inCombat;
    private boolean playerFled;
    private boolean bonusActionUsed;      // Track if bonus action was used this turn
    private boolean actionSurgeActive;    // Track if Action Surge grants an extra action
    private boolean sneakAttackUsed;      // Track if Sneak Attack was used this turn
    private boolean disengageActive;      // Track if Disengage is active (no opportunity attacks)
    private boolean patientDefenseActive; // Track if Patient Defense is active (attacks have disadvantage)
    private int flurryAttacksRemaining;   // Track remaining Flurry of Blows attacks
    private boolean sacredWeaponActive;   // Track if Sacred Weapon is active (+CHA to attacks)
    private boolean smiteReady;           // Track if Divine Smite will be used on next hit

    public CombatSystem() {
        this.participants = new ArrayList<>();
        this.initiative = new ArrayList<>();
        this.initiativeRolls = new HashMap<>();
        this.lastAttacker = new HashMap<>();
        this.droppedItems = new ArrayList<>();
        this.statusEffectManager = new StatusEffectManager();
        this.currentTurn = 0;
        this.currentState = null;
        this.inCombat = false;
        this.playerFled = false;
        this.bonusActionUsed = false;
        this.actionSurgeActive = false;
        this.sneakAttackUsed = false;
        this.disengageActive = false;
        this.patientDefenseActive = false;
        this.flurryAttacksRemaining = 0;
        this.sacredWeaponActive = false;
        this.smiteReady = false;
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
        this.statusEffectManager = new StatusEffectManager();
        this.currentTurn = 0;
        this.playerFled = false;
        this.bonusActionUsed = false;
        this.actionSurgeActive = false;
        this.sneakAttackUsed = false;
        this.disengageActive = false;
        this.patientDefenseActive = false;
        this.flurryAttacksRemaining = 0;
        this.sacredWeaponActive = false;
        this.smiteReady = false;

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

        // Process turn start effects (may expire conditions, deal damage, etc.)
        List<String> turnStartMessages = statusEffectManager.processTurnStart(current);

        // Check if combatant is incapacitated and can't act
        if (!statusEffectManager.canTakeActions(current)) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("%s is incapacitated and cannot act!", current.getName()));
            if (!turnStartMessages.isEmpty()) {
                message.append("\n").append(String.join("\n", turnStartMessages));
            }
            advanceTurn();
            return CombatResult.error(message.toString());
        }

        // If it's an enemy, execute AI turn
        if (isEnemy(current)) {
            return enemyTurn();
        }

        // Player turn - return notification (include any turn start messages)
        CombatResult turnStart = CombatResult.turnStart(current);
        if (!turnStartMessages.isEmpty()) {
            // The turn start messages are logged separately but we return the basic turn start
        }
        return turnStart;
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
            return CombatResult.error("What do you want to do? (attack, flee, secondwind, surge)");
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

            case "secondwind":
            case "second wind":
            case "second_wind":
                return handleSecondWind();

            case "surge":
            case "action surge":
            case "action_surge":
            case "actionsurge":
                return handleActionSurge();

            // Cunning Action options (Rogue)
            case "dash":
                return handleCunningDash();

            case "disengage":
                return handleCunningDisengage();

            case "hide":
                return handleCunningHide();

            // Barbarian actions
            case "rage":
                return handleRage();

            case "reckless":
            case "reckless attack":
            case "recklessattack":
                return handleRecklessAttack();

            // Monk actions
            case "flurry":
            case "flurryofblows":
            case "flurry of blows":
                return handleFlurryOfBlows();

            case "patient":
            case "patientdefense":
            case "patient defense":
                return handlePatientDefense();

            case "step":
            case "stepofthewind":
            case "step of the wind":
                return handleStepOfTheWind();

            // Paladin actions
            case "smite":
            case "divinesmite":
            case "divine smite":
                return handleDivineSmite();

            case "layonhands":
            case "lay on hands":
            case "loh":
                return handleLayOnHands(target);

            case "sacredweapon":
            case "sacred weapon":
                return handleSacredWeapon();

            default:
                return CombatResult.error(
                    String.format("Unknown action: %s. Try: attack, flee, or class abilities", action));
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

        // Defensive check with pattern matching - isEnemy() should guarantee this
        if (!(current instanceof Monster monster)) {
            return CombatResult.error("Current combatant is not a valid enemy type.");
        }
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
            return CombatResult.info(message);
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
        lastAttacker.clear();
        currentTurn = 0;
        currentState = null;

        return CombatResult.victory(xp);
    }

    /**
     * Removes dead combatants from tracking maps to prevent memory leaks during long combats.
     */
    private void cleanupDeadCombatants() {
        lastAttacker.entrySet().removeIf(entry ->
            !entry.getKey().isAlive() || !entry.getValue().isAlive());
    }

    // ==========================================
    // Combat Actions
    // ==========================================

    /**
     * Processes an attack from attacker to target.
     * Takes into account status effects for advantage/disadvantage and auto-crits.
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

        // Determine advantage/disadvantage from status effects
        boolean hasAdvantage = statusEffectManager.hasAdvantageOnAttacks(attacker) ||
                               statusEffectManager.attacksHaveAdvantageAgainst(target);
        boolean hasDisadvantage = statusEffectManager.hasDisadvantageOnAttacks(attacker);

        // Reckless Attack: enemies have advantage on attacks against a reckless barbarian
        if (target instanceof Character targetChar && targetChar.isRecklessAttackActive()) {
            hasAdvantage = true;
        }

        // Check if melee attacks auto-crit (paralyzed/unconscious targets)
        boolean autoCrit = statusEffectManager.meleeCritsOnHit(target);

        int attackRoll;
        int damage;
        int targetAC = target.getArmorClass();

        if (attacker instanceof Monster monster) {
            // Monster attack - apply advantage/disadvantage
            int attackBonus = monster.getAttackBonus();
            boolean isNaturalCrit = false;

            if (hasAdvantage && !hasDisadvantage) {
                attackRoll = Dice.rollWithAdvantage(attackBonus);
                isNaturalCrit = Dice.wasNatural20();
            } else if (hasDisadvantage && !hasAdvantage) {
                attackRoll = Dice.rollWithDisadvantage(attackBonus);
                isNaturalCrit = Dice.wasNatural20();
            } else {
                attackRoll = monster.rollAttack();
                isNaturalCrit = Dice.wasNatural20();
            }

            // Critical hit on natural 20 or auto-crit conditions
            boolean isCrit = isNaturalCrit || autoCrit;

            if (attackRoll >= targetAC || isNaturalCrit) {
                // Roll damage dice (roll twice on crit per D&D 5e rules)
                damage = monster.rollDamage();
                if (isCrit) {
                    damage += monster.rollDamage(); // Double the dice, not the total
                }

                // Rage resistance: halve physical damage while raging
                boolean rageResisted = false;
                if (target instanceof Character targetChar && targetChar.isRaging()) {
                    // Rage grants resistance to bludgeoning, piercing, slashing
                    // For simplicity, assume all monster attacks are physical
                    damage = damage / 2;
                    rageResisted = true;
                }

                target.takeDamage(damage);

                // Track aggro - target remembers who hit them
                lastAttacker.put(target, attacker);

                // Check for special ability on hit
                String specialEffect = processSpecialAbilityOnHit(monster, target);
                if (rageResisted) {
                    specialEffect = (specialEffect != null ? specialEffect + " " : "") + "[RAGE RESISTED!]";
                }
                if (isCrit) {
                    String critType = isNaturalCrit ? "[CRITICAL HIT!]" : "[AUTO-CRIT!]";
                    specialEffect = (specialEffect != null ? specialEffect + " " : "") + critType;
                }

                return CombatResult.attackHit(attacker, target, attackRoll, targetAC, damage, specialEffect);
            } else {
                return CombatResult.attackMiss(attacker, target, attackRoll, targetAC);
            }
        } else if (attacker instanceof Character character) {
            // Get equipped weapon and determine ability modifier to use
            Weapon weapon = character.getInventory().getEquippedWeapon();
            String damageDice = DEFAULT_PLAYER_WEAPON;

            int strMod = character.getAbilityModifier(Ability.STRENGTH);
            int dexMod = character.getAbilityModifier(Ability.DEXTERITY);
            int profBonus = character.getProficiencyBonus();

            // Get Fighting Style for bonuses
            FightingStyle fightingStyle = character.getFightingStyle();
            boolean isRangedAttack = weapon != null && weapon.isRanged();
            boolean hasOffHand = character.getInventory().getEquipped(Inventory.EquipmentSlot.OFF_HAND) != null;
            boolean isTwoHanded = weapon != null && weapon.isTwoHanded();

            // Determine which ability modifier to use for attack and damage
            int abilityMod;
            if (weapon != null) {
                damageDice = weapon.getDamageDice();
                if (weapon.isRanged()) {
                    // Ranged weapons always use DEX
                    abilityMod = dexMod;
                } else if (weapon.isFinesse()) {
                    // Finesse weapons use the higher of STR or DEX
                    abilityMod = Math.max(strMod, dexMod);
                } else {
                    // Melee weapons use STR
                    abilityMod = strMod;
                }
            } else {
                // Unarmed: use STR, damage is 1 + STR mod
                abilityMod = strMod;
                damageDice = "1";
            }

            int totalMod = abilityMod + profBonus;

            // Archery Fighting Style: +2 to ranged attack rolls
            if (fightingStyle == FightingStyle.ARCHERY && isRangedAttack) {
                totalMod += 2;
            }

            // Reckless Attack (Barbarian): grants advantage on melee STR attacks
            boolean isReckless = character.isRecklessAttackActive();
            boolean isMeleeStrAttack = !isRangedAttack && abilityMod == strMod;
            if (isReckless && isMeleeStrAttack) {
                hasAdvantage = true;
            }

            boolean isNaturalCrit = false;
            int naturalRoll;

            if (hasAdvantage && !hasDisadvantage) {
                attackRoll = Dice.rollWithAdvantage(totalMod);
                isNaturalCrit = Dice.wasNatural20();
                naturalRoll = attackRoll - totalMod;  // Approximate the natural roll
            } else if (hasDisadvantage && !hasAdvantage) {
                attackRoll = Dice.rollWithDisadvantage(totalMod);
                isNaturalCrit = Dice.wasNatural20();
                naturalRoll = attackRoll - totalMod;
            } else {
                naturalRoll = Dice.rollD20();
                attackRoll = naturalRoll + totalMod;
                isNaturalCrit = naturalRoll == 20;
            }

            // Check for Improved Critical (Champion Fighter) - crit on 19-20
            int critThreshold = character.getCriticalThreshold();
            boolean isImprovedCrit = naturalRoll >= critThreshold && naturalRoll < 20;
            boolean isCrit = isNaturalCrit || isImprovedCrit || autoCrit;

            if (attackRoll >= targetAC || isNaturalCrit || isImprovedCrit) {
                // Damage: weapon dice + ability modifier (roll dice twice on crit)
                // Unarmed attacks deal 1 flat damage (no dice), weapons use dice notation
                if (weapon != null) {
                    damage = Dice.parse(damageDice);
                    if (isCrit) {
                        damage += Dice.parse(damageDice); // Double the dice, not the modifier
                    }
                } else {
                    // Unarmed: 1 damage (doubled to 2 on crit)
                    damage = isCrit ? 2 : 1;
                }
                damage += abilityMod;

                // Dueling Fighting Style: +2 damage with one-handed melee weapon and no off-hand
                if (fightingStyle == FightingStyle.DUELING && weapon != null &&
                    !isRangedAttack && !isTwoHanded && !hasOffHand) {
                    damage += 2;
                }

                // Rage (Barbarian): bonus damage on melee STR attacks while raging
                int rageDamage = 0;
                if (character.isRaging() && isMeleeStrAttack) {
                    rageDamage = character.getRageDamageBonus();
                    damage += rageDamage;
                }

                // Sneak Attack (Rogue): extra damage with finesse/ranged when having advantage
                // or when an ally is adjacent to the target
                int sneakAttackDamage = 0;
                boolean canSneakAttack = !sneakAttackUsed &&
                    weapon != null &&
                    (weapon.isFinesse() || weapon.isRanged());

                if (canSneakAttack) {
                    // Check for Sneak Attack conditions:
                    // 1. Have advantage on the attack, OR
                    // 2. Another enemy of the target is within 5 feet (simplified: always true in combat)
                    boolean hasSneakCondition = hasAdvantage || hasAllyAdjacentToTarget(target);

                    if (hasSneakCondition) {
                        var sneakAttackFeature = character.getFeature(RogueFeatures.SNEAK_ATTACK_ID);
                        if (sneakAttackFeature.isPresent() &&
                            sneakAttackFeature.get() instanceof RogueFeatures.SneakAttack sneakAttack) {
                            int sneakDice = sneakAttack.getSneakAttackDice();
                            sneakAttackDamage = Dice.rollMultiple(sneakDice, 6);
                            if (isCrit) {
                                sneakAttackDamage += Dice.rollMultiple(sneakDice, 6); // Double on crit
                            }
                            damage += sneakAttackDamage;
                            sneakAttackUsed = true;
                        }
                    }
                }

                damage = Math.max(1, damage); // Minimum 1 damage

                target.takeDamage(damage);

                // Track aggro - target remembers who hit them
                lastAttacker.put(target, attacker);

                StringBuilder specialEffects = new StringBuilder();
                if (rageDamage > 0) {
                    specialEffects.append(String.format("[RAGE +%d!]", rageDamage));
                }
                if (sneakAttackDamage > 0) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append(String.format("[SNEAK ATTACK +%d!]", sneakAttackDamage));
                }
                if (isReckless) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append("[RECKLESS]");
                }
                if (isCrit) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    if (isImprovedCrit && !isNaturalCrit) {
                        specialEffects.append("[IMPROVED CRITICAL!]");
                    } else if (isNaturalCrit) {
                        specialEffects.append("[CRITICAL HIT!]");
                    } else {
                        specialEffects.append("[AUTO-CRIT!]");
                    }
                }
                String specialEffect = specialEffects.length() > 0 ? specialEffects.toString() : null;
                return CombatResult.attackHit(attacker, target, attackRoll, targetAC, damage, specialEffect);
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
     * Gets the status effect manager for this combat.
     */
    public StatusEffectManager getStatusEffectManager() {
        return statusEffectManager;
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
     * Processes turn-end effects for the current combatant before advancing.
     */
    private void advanceTurn() {
        // Process turn end effects for current combatant (saves, duration decrement)
        Combatant current = getCurrentCombatant();
        if (current != null && current.isAlive()) {
            statusEffectManager.processTurnEnd(current);
        }

        // Reset per-turn flags for player
        if (current instanceof Character character) {
            bonusActionUsed = false;
            actionSurgeActive = false;
            sneakAttackUsed = false;
            disengageActive = false;
            patientDefenseActive = false;
            flurryAttacksRemaining = 0;
            smiteReady = false;  // Smite expires if not used
            character.resetRecklessAttack();

            // Process Sacred Weapon duration
            character.getFeature(PaladinFeatures.SACRED_WEAPON_ID)
                .filter(f -> f instanceof PaladinFeatures.SacredWeapon)
                .map(f -> (PaladinFeatures.SacredWeapon) f)
                .ifPresent(sw -> {
                    if (!sw.processTurnEnd()) {
                        sacredWeaponActive = false;
                    }
                });

            // Reset Colossus Slayer for Ranger
            character.getFeature(RangerFeatures.COLOSSUS_SLAYER_ID)
                .filter(f -> f instanceof RangerFeatures.ColossusSlayer)
                .map(f -> (RangerFeatures.ColossusSlayer) f)
                .ifPresent(RangerFeatures.ColossusSlayer::resetTurn);

            // Reset Horde Breaker for Ranger
            character.getFeature(RangerFeatures.HORDE_BREAKER_ID)
                .filter(f -> f instanceof RangerFeatures.HordeBreaker)
                .map(f -> (RangerFeatures.HordeBreaker) f)
                .ifPresent(RangerFeatures.HordeBreaker::resetTurn);
        }

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
        // Clean up tracking maps for dead combatants
        cleanupDeadCombatants();

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
            // Check for victory - include killing blow details
            if (getLivingEnemies().isEmpty()) {
                int xp = calculateXpReward();
                inCombat = false;

                // Award XP to player
                Combatant player = getPlayer();
                if (player instanceof Character character) {
                    character.addExperience(xp);
                    for (Item item : droppedItems) {
                        character.getInventory().addItem(item);
                    }
                    droppedItems.clear();
                }

                advanceTurn();
                return CombatResult.victoryWithKillingBlow(xp, attackResult.getMessage());
            }

            // Check if Action Surge is active - allow another attack
            if (actionSurgeActive) {
                actionSurgeActive = false;  // Consume the surge
                String message = attackResult.getMessage() + "\n" +
                    targetEnemy.getName() + " is defeated!\n" +
                    "[Action Surge: You can take another action!]";
                return CombatResult.info(message);
            }

            advanceTurn();
            return CombatResult.enemyDefeated((Monster) targetEnemy);
        }

        // Check if Action Surge is active - allow another attack
        if (actionSurgeActive) {
            actionSurgeActive = false;  // Consume the surge
            String message = attackResult.getMessage() + "\n[Action Surge: You can take another action!]";
            return CombatResult.info(message);
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
     * Handles Second Wind action (Fighter feature).
     * Uses bonus action to heal 1d10 + Fighter level.
     */
    private CombatResult handleSecondWind() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if bonus action already used this turn
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Check if player has Second Wind and can use it
        if (!player.canUseFeature(FighterFeatures.SECOND_WIND_ID)) {
            var feature = player.getFeature(FighterFeatures.SECOND_WIND_ID);
            if (feature.isEmpty()) {
                return CombatResult.error("You don't have the Second Wind ability.");
            }
            return CombatResult.error("You have no uses of Second Wind remaining. Take a short rest to recover it.");
        }

        // Use the feature
        String result = player.useFeature(FighterFeatures.SECOND_WIND_ID);
        bonusActionUsed = true;

        // Return info result (doesn't end turn - player can still attack)
        return CombatResult.info(result + "\n(You can still take your action this turn.)");
    }

    /**
     * Handles Action Surge action (Fighter feature).
     * Grants an additional action this turn.
     */
    private CombatResult handleActionSurge() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if Action Surge is already active
        if (actionSurgeActive) {
            return CombatResult.error("Action Surge is already active this turn.");
        }

        // Check if player has Action Surge and can use it
        if (!player.canUseFeature(FighterFeatures.ACTION_SURGE_ID)) {
            var feature = player.getFeature(FighterFeatures.ACTION_SURGE_ID);
            if (feature.isEmpty()) {
                return CombatResult.error("You don't have the Action Surge ability.");
            }
            return CombatResult.error("You have no uses of Action Surge remaining. Take a short rest to recover it.");
        }

        // Use the feature
        String result = player.useFeature(FighterFeatures.ACTION_SURGE_ID);
        actionSurgeActive = true;

        return CombatResult.info(result);
    }

    /**
     * Handles Cunning Action: Dash (Rogue feature).
     * Uses bonus action to double movement speed this turn.
     */
    private CombatResult handleCunningDash() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Check if player has Cunning Action
        if (player.getFeature(RogueFeatures.CUNNING_ACTION_ID).isEmpty()) {
            return CombatResult.error("You don't have the Cunning Action ability.");
        }

        bonusActionUsed = true;
        return CombatResult.info(String.format(
            "%s uses Cunning Action to Dash! Movement speed doubled this turn.\n" +
            "(You can still take your action this turn.)", player.getName()));
    }

    /**
     * Handles Cunning Action: Disengage (Rogue feature).
     * Uses bonus action to avoid opportunity attacks this turn.
     */
    private CombatResult handleCunningDisengage() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Check if player has Cunning Action
        if (player.getFeature(RogueFeatures.CUNNING_ACTION_ID).isEmpty()) {
            return CombatResult.error("You don't have the Cunning Action ability.");
        }

        bonusActionUsed = true;
        disengageActive = true;
        return CombatResult.info(String.format(
            "%s uses Cunning Action to Disengage! Movement won't provoke opportunity attacks.\n" +
            "(You can still take your action this turn.)", player.getName()));
    }

    /**
     * Handles Cunning Action: Hide (Rogue feature).
     * Uses bonus action to attempt to hide.
     */
    private CombatResult handleCunningHide() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Check if player has Cunning Action
        if (player.getFeature(RogueFeatures.CUNNING_ACTION_ID).isEmpty()) {
            return CombatResult.error("You don't have the Cunning Action ability.");
        }

        bonusActionUsed = true;

        // Roll stealth check
        int stealthMod = player.getSkillModifier(Character.Skill.STEALTH);
        int stealthRoll = Dice.rollWithModifier(20, stealthMod);

        return CombatResult.info(String.format(
            "%s uses Cunning Action to Hide! Stealth check: %d (d20 + %d)\n" +
            "If successful, you have advantage on your next attack.\n" +
            "(You can still take your action this turn.)",
            player.getName(), stealthRoll, stealthMod));
    }

    /**
     * Handles Rage action (Barbarian feature).
     * Uses bonus action to enter rage.
     */
    private CombatResult handleRage() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Check if already raging
        if (player.isRaging()) {
            return CombatResult.error("You are already raging!");
        }

        // Check if player has Rage and can use it
        if (!player.canUseFeature(BarbarianFeatures.RAGE_ID)) {
            var feature = player.getFeature(BarbarianFeatures.RAGE_ID);
            if (feature.isEmpty()) {
                return CombatResult.error("You don't have the Rage ability.");
            }
            return CombatResult.error("You have no uses of Rage remaining. Take a long rest to recover.");
        }

        // Use the feature
        String result = player.useFeature(BarbarianFeatures.RAGE_ID);
        bonusActionUsed = true;

        return CombatResult.info(result + "\n(You can still take your action this turn.)");
    }

    /**
     * Handles Reckless Attack action (Barbarian feature).
     * Gives advantage on attacks but enemies have advantage on you.
     */
    private CombatResult handleRecklessAttack() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Reckless Attack
        var feature = player.getFeature(BarbarianFeatures.RECKLESS_ATTACK_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Reckless Attack ability.");
        }

        // Check if already used this turn
        if (player.isRecklessAttackActive()) {
            return CombatResult.error("You are already attacking recklessly this turn.");
        }

        // Activate Reckless Attack
        String result = player.useFeature(BarbarianFeatures.RECKLESS_ATTACK_ID);

        return CombatResult.info(result + "\n(Now make your attack!)");
    }

    // ==========================================
    // Monk Combat Actions
    // ==========================================

    private CombatResult handleFlurryOfBlows() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Flurry of Blows
        var feature = player.getFeature(MonkFeatures.FLURRY_OF_BLOWS_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Flurry of Blows ability.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Get Ki feature and check for points
        var kiFeature = player.getFeature(MonkFeatures.KI_ID);
        if (kiFeature.isEmpty()) {
            return CombatResult.error("You don't have Ki points yet.");
        }

        MonkFeatures.Ki ki = (MonkFeatures.Ki) kiFeature.get();
        if (ki.getKiPoints() < 1) {
            return CombatResult.error("You don't have enough ki points. (0/" + ki.getMaxKiPoints() + ")");
        }

        // Spend 1 ki point
        ki.spendKi(1);
        bonusActionUsed = true;
        flurryAttacksRemaining = 2;

        return CombatResult.info(String.format(
            "%s spends 1 ki point for Flurry of Blows!\n" +
            "Ki: %d/%d remaining\n" +
            "(Make 2 unarmed strikes with 'attack')",
            player.getName(), ki.getKiPoints(), ki.getMaxKiPoints()));
    }

    private CombatResult handlePatientDefense() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Patient Defense
        var feature = player.getFeature(MonkFeatures.PATIENT_DEFENSE_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Patient Defense ability.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Get Ki feature and check for points
        var kiFeature = player.getFeature(MonkFeatures.KI_ID);
        if (kiFeature.isEmpty()) {
            return CombatResult.error("You don't have Ki points yet.");
        }

        MonkFeatures.Ki ki = (MonkFeatures.Ki) kiFeature.get();
        if (ki.getKiPoints() < 1) {
            return CombatResult.error("You don't have enough ki points. (0/" + ki.getMaxKiPoints() + ")");
        }

        // Spend 1 ki point
        ki.spendKi(1);
        bonusActionUsed = true;
        patientDefenseActive = true;

        return CombatResult.info(String.format(
            "%s spends 1 ki point for Patient Defense!\n" +
            "Ki: %d/%d remaining\n" +
            "All attacks against you have disadvantage until your next turn.",
            player.getName(), ki.getKiPoints(), ki.getMaxKiPoints()));
    }

    private CombatResult handleStepOfTheWind() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Step of the Wind
        var feature = player.getFeature(MonkFeatures.STEP_OF_THE_WIND_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Step of the Wind ability.");
        }

        // Check if bonus action already used
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }

        // Get Ki feature and check for points
        var kiFeature = player.getFeature(MonkFeatures.KI_ID);
        if (kiFeature.isEmpty()) {
            return CombatResult.error("You don't have Ki points yet.");
        }

        MonkFeatures.Ki ki = (MonkFeatures.Ki) kiFeature.get();
        if (ki.getKiPoints() < 1) {
            return CombatResult.error("You don't have enough ki points. (0/" + ki.getMaxKiPoints() + ")");
        }

        // Spend 1 ki point
        ki.spendKi(1);
        bonusActionUsed = true;
        disengageActive = true;  // Can disengage as part of Step of the Wind

        return CombatResult.info(String.format(
            "%s spends 1 ki point for Step of the Wind!\n" +
            "Ki: %d/%d remaining\n" +
            "You can Dash or Disengage. Movement doesn't provoke opportunity attacks.",
            player.getName(), ki.getKiPoints(), ki.getMaxKiPoints()));
    }

    /**
     * Gets remaining Flurry of Blows attacks this turn.
     */
    public int getFlurryAttacksRemaining() {
        return flurryAttacksRemaining;
    }

    /**
     * Uses one Flurry of Blows attack.
     */
    public void useFlurryAttack() {
        if (flurryAttacksRemaining > 0) {
            flurryAttacksRemaining--;
        }
    }

    /**
     * Checks if Patient Defense is active (attacks have disadvantage).
     */
    public boolean isPatientDefenseActive() {
        return patientDefenseActive;
    }

    // ==========================================
    // Paladin Combat Actions
    // ==========================================

    private CombatResult handleDivineSmite() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Divine Smite
        var feature = player.getFeature(PaladinFeatures.DIVINE_SMITE_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Divine Smite ability.");
        }

        PaladinFeatures.DivineSmite smite = (PaladinFeatures.DivineSmite) feature.get();

        // Check for available spell slots
        int lowestSlot = smite.getLowestAvailableSlot();
        if (lowestSlot == 0) {
            return CombatResult.error("You have no spell slots remaining.\n" + smite.getSlotsStatus());
        }

        // Ready smite for next attack
        smiteReady = true;

        return CombatResult.info(String.format(
            "Divine Smite ready! Your next melee attack will deal extra radiant damage.\n" +
            "%s\n" +
            "(Lowest slot: Level %d = %s extra damage)",
            smite.getSlotsStatus(),
            lowestSlot,
            smite.getSmiteDamageNotation(lowestSlot, false)));
    }

    private CombatResult handleLayOnHands(String target) {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Lay on Hands
        var feature = player.getFeature(PaladinFeatures.LAY_ON_HANDS_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Lay on Hands ability.");
        }

        PaladinFeatures.LayOnHands loh = (PaladinFeatures.LayOnHands) feature.get();

        if (loh.getPoolRemaining() <= 0) {
            return CombatResult.error("Your Lay on Hands pool is empty. Take a long rest to restore it.");
        }

        // For now, heal self for max available or amount needed
        int missingHp = player.getMaxHitPoints() - player.getCurrentHitPoints();
        if (missingHp <= 0) {
            return CombatResult.error("You are already at full health.");
        }

        int healAmount = Math.min(missingHp, loh.getPoolRemaining());
        int actualHeal = loh.heal(player, healAmount);

        return CombatResult.info(String.format(
            "%s uses Lay on Hands!\n" +
            "Healed %d HP. (Now %d/%d HP)\n" +
            "Pool remaining: %d/%d",
            player.getName(),
            actualHeal,
            player.getCurrentHitPoints(),
            player.getMaxHitPoints(),
            loh.getPoolRemaining(),
            loh.getMaxPool()));
    }

    private CombatResult handleSacredWeapon() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        // Check if player has Channel Divinity
        var channelFeature = player.getFeature(PaladinFeatures.CHANNEL_DIVINITY_ID);
        if (channelFeature.isEmpty()) {
            return CombatResult.error("You don't have Channel Divinity yet (requires level 3).");
        }

        // Check if Channel Divinity can be used
        PaladinFeatures.ChannelDivinity channel = (PaladinFeatures.ChannelDivinity) channelFeature.get();
        if (!channel.canUse()) {
            return CombatResult.error("You've already used Channel Divinity. Take a short rest to restore it.");
        }

        // Check for Sacred Weapon feature
        var swFeature = player.getFeature(PaladinFeatures.SACRED_WEAPON_ID);
        if (swFeature.isEmpty()) {
            return CombatResult.error("You don't have Sacred Weapon.");
        }

        PaladinFeatures.SacredWeapon sw = (PaladinFeatures.SacredWeapon) swFeature.get();

        if (sw.isActive()) {
            return CombatResult.error("Sacred Weapon is already active.");
        }

        // Use Channel Divinity
        channel.use(player);

        // Activate Sacred Weapon
        int chaMod = player.getAbilityModifier(Character.Ability.CHARISMA);
        String result = sw.activate(chaMod);
        sacredWeaponActive = true;

        return CombatResult.info(result);
    }

    /**
     * Checks if Divine Smite is ready for the next attack.
     */
    public boolean isSmiteReady() {
        return smiteReady;
    }

    /**
     * Clears smite ready status (after attack resolves).
     */
    public void clearSmiteReady() {
        smiteReady = false;
    }

    /**
     * Checks if Sacred Weapon is active.
     */
    public boolean isSacredWeaponActive() {
        return sacredWeaponActive;
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
     * Checks if an ally (non-enemy combatant) is adjacent to the target.
     * For Sneak Attack purposes, this means another hostile creature is within 5 feet.
     * In this simplified implementation, returns true if there are multiple enemies
     * engaged in combat (allowing Sneak Attack when fighting alongside monsters).
     * Since this is typically a solo player game, this usually returns false,
     * making Rogues rely on advantage for Sneak Attack.
     */
    private boolean hasAllyAdjacentToTarget(Combatant target) {
        // In a solo player game, we consider there's an "ally" (another threat to the enemy)
        // if there are multiple living enemies - this represents chaotic combat where
        // the Rogue can exploit openings. This is a simplification.
        // In practice, Rogues should try to gain advantage (e.g., via Hide) for Sneak Attack.
        return getLivingEnemies().size() > 1;
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
     * On failure, applies the RESTRAINED condition with ongoing STR saves to escape.
     */
    private String processAdhesiveAbility(Monster monster, Combatant target) {
        int dc = 13;  // Default DC for Adhesive
        int strMod = 0;

        if (target instanceof Character character) {
            strMod = character.getAbilityModifier(Ability.STRENGTH);
        } else if (target instanceof Monster targetMonster) {
            strMod = targetMonster.getStrengthMod();
        }

        boolean saved = Dice.checkAgainstDC(strMod, dc);

        if (saved) {
            return String.format("[Adhesive: STR save DC %d - SAVED!]", dc);
        } else {
            // Apply RESTRAINED condition with STR save to escape each turn
            ConditionEffect restrained = ConditionEffect.restrainedWithSave(Ability.STRENGTH, dc);
            restrained.setSource(monster);
            statusEffectManager.applyEffect(target, restrained);

            return String.format("[Adhesive: STR save DC %d - FAILED! %s is RESTRAINED!]",
                dc, target.getName());
        }
    }
}
