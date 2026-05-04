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
import com.questkeeper.magic.Spell;
import com.questkeeper.magic.Spellbook;
import com.questkeeper.magic.SpellRegistry;
import com.questkeeper.magic.SpellResult;
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
    private int mainActionAttacksRemaining; // Track remaining main-action attacks (Extra Attack)
    private boolean reactionUsed;         // Track if reaction was used this turn
    private boolean sacredWeaponActive;   // Track if Sacred Weapon is active (+CHA to attacks)
    private boolean smiteReady;           // Track if Divine Smite will be used on next hit
    private boolean stunningStrikeReady;  // Monk: next melee hit forces a CON save vs Ki DC

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
        this.mainActionAttacksRemaining = 0;
        this.reactionUsed = false;
        this.sacredWeaponActive = false;
        this.smiteReady = false;
        this.stunningStrikeReady = false;
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
        this.mainActionAttacksRemaining = 0;
        this.reactionUsed = false;
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

        // Player turn-start: Patient Defense expires now (it lasted through
        // enemy turns since the monk activated it last round).
        patientDefenseActive = false;

        // Reset Extra Attack budget for the new turn (martial classes get
        // 2+ attacks per main action at Lvl 5+).
        if (current instanceof Character c) {
            mainActionAttacksRemaining = c.getAttacksPerTurn();
        }

        // Reaction refreshes at the start of each player turn (5e: one
        // reaction per round, refreshed at start of your turn).
        reactionUsed = false;

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

            case "help":
            case "?":
                return CombatResult.info(buildCombatHelp());

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

            case "frenzy":
                return handleFrenzy();

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

            case "stun":
            case "stunningstrike":
            case "stunning strike":
                return handleStunningStrike();

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

            case "turn":
            case "turntheunholy":
            case "turn the unholy":
                return handleTurnTheUnholy();

            // Spellcasting
            case "cast":
                return handleCastSpell(target);

            default:
                return CombatResult.error(
                    String.format("Unknown action: %s. Type 'help' for available actions.", action));
        }
    }

    /**
     * Builds an in-combat help string listing the verbs the player's
     * character has available right now, based on their class features.
     */
    private String buildCombatHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMBAT ACTIONS ===\n");
        sb.append("  attack <target>  - make a weapon attack\n");
        sb.append("  flee             - try to escape (DEX check)\n");
        sb.append("  cast <spell>     - cast a spell (uses a slot)\n");
        sb.append("  help             - this menu\n");

        Combatant p = getPlayer();
        if (!(p instanceof Character ch)) return sb.toString();

        StringBuilder cls = new StringBuilder();
        if (ch.getFeature(FighterFeatures.SECOND_WIND_ID).isPresent()) {
            cls.append("  secondwind       - Fighter: bonus-action self-heal\n");
        }
        if (ch.getFeature(FighterFeatures.ACTION_SURGE_ID).isPresent()) {
            cls.append("  actionsurge      - Fighter: another action this turn\n");
        }
        if (ch.getFeature(PaladinFeatures.DIVINE_SMITE_ID).isPresent()) {
            cls.append("  smite            - Paladin: prime Divine Smite\n");
        }
        if (ch.getFeature(PaladinFeatures.LAY_ON_HANDS_ID).isPresent()) {
            cls.append("  layonhands       - Paladin: heal from your divine pool\n");
        }
        if (ch.getFeature(PaladinFeatures.SACRED_WEAPON_ID).isPresent()) {
            cls.append("  sacredweapon     - Paladin: Channel Divinity buff\n");
        }
        if (ch.getFeature(PaladinFeatures.TURN_THE_UNHOLY_ID).isPresent()
            || ch.getFeature(com.questkeeper.character.features.ClericFeatures.TURN_UNDEAD_ID).isPresent()) {
            cls.append("  turn             - frighten undead/fiends (Channel Divinity)\n");
        }
        if (ch.getFeature(BarbarianFeatures.RAGE_ID).isPresent()) {
            cls.append("  rage             - Barbarian: enter rage\n");
            cls.append("  reckless         - Barbarian: advantage on attacks (and on you)\n");
        }
        if (ch.getFeature(BarbarianFeatures.FRENZY_ID).isPresent()) {
            cls.append("  frenzy           - Berserker: bonus melee attack while raging\n");
        }
        if (ch.getFeature(MonkFeatures.FLURRY_OF_BLOWS_ID).isPresent()) {
            cls.append("  flurry           - Monk: 2 bonus attacks (1 ki)\n");
            cls.append("  patient          - Monk: defensive (1 ki)\n");
            cls.append("  step             - Monk: dash/disengage (1 ki)\n");
        }
        if (ch.getFeature(MonkFeatures.STUNNING_STRIKE_ID).isPresent()) {
            cls.append("  stun             - Monk L5: prime Stunning Strike\n");
        }
        if (ch.getFeature(RogueFeatures.CUNNING_ACTION_ID).isPresent()) {
            cls.append("  dash / disengage / hide - Rogue: Cunning Action\n");
        }

        if (cls.length() > 0) {
            sb.append("=== CLASS ACTIONS ===\n").append(cls);
        }
        return sb.toString();
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
            // Remove monster from combat. If the fleer's index in `initiative`
            // was at or before currentTurn, every later index shifts down by
            // one — adjust currentTurn so advanceTurn doesn't skip the
            // combatant that took the fleer's place.
            participants.remove(monster);
            int fledIdx = initiative.indexOf(monster);
            initiative.remove(monster);
            if (fledIdx >= 0 && fledIdx <= currentTurn && currentTurn > 0) {
                currentTurn--;
            }

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

        // Patient Defense (Monk): attacks against the protected target have
        // disadvantage until the start of the monk's next turn.
        if (target instanceof Character && patientDefenseActive) {
            hasDisadvantage = true;
        }

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

                // Deflect Missiles (Lvl 3 Monk): reaction reduces incoming
                // ranged damage by 1d10 + DEX + level. The monk uses it
                // automatically when available.
                int deflectReduction = 0;
                boolean deflected = false;
                if (target instanceof Character monkTarget
                    && monster.isRangedAttack()
                    && reactionAvailable()) {
                    var dmFeature = monkTarget.getFeature(MonkFeatures.DEFLECT_MISSILES_ID);
                    if (dmFeature.isPresent()
                        && dmFeature.get() instanceof MonkFeatures.DeflectMissiles dm) {
                        int roll = Dice.roll(10);
                        deflectReduction = dm.calculateReduction(monkTarget, roll);
                        damage = Math.max(0, damage - deflectReduction);
                        deflected = true;
                        consumeReaction();
                    }
                }

                // Uncanny Dodge (Lvl 5 Rogue): if the rogue still has a
                // reaction this round and Deflect Missiles didn't already
                // claim it, halve the incoming damage. Auto-fires when
                // available.
                boolean uncannyDodge = false;
                if (target instanceof Character rogueTarget
                    && reactionAvailable()
                    && rogueTarget.getFeature(
                        com.questkeeper.character.features.RogueFeatures.UNCANNY_DODGE_ID
                    ).isPresent()
                    && damage > 0) {
                    damage = damage / 2;
                    uncannyDodge = true;
                    consumeReaction();
                }

                target.takeDamage(damage);

                // Track aggro - target remembers who hit them
                lastAttacker.put(target, attacker);

                // Check for special ability on hit
                String specialEffect = processSpecialAbilityOnHit(monster, target);
                if (rageResisted) {
                    specialEffect = (specialEffect != null ? specialEffect + " " : "") + "[RAGE RESISTED!]";
                }
                if (deflected) {
                    String tag = damage == 0
                        ? String.format("[DEFLECT MISSILES: -%d, fully deflected!]", deflectReduction)
                        : String.format("[DEFLECT MISSILES: -%d damage]", deflectReduction);
                    specialEffect = (specialEffect != null ? specialEffect + " " : "") + tag;
                }
                if (uncannyDodge) {
                    specialEffect = (specialEffect != null ? specialEffect + " " : "")
                        + "[UNCANNY DODGE: damage halved!]";
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

            // Sacred Weapon (Paladin): +CHA mod (min +1) to attack rolls while active
            int sacredWeaponBonus = 0;
            if (sacredWeaponActive && weapon != null) {
                sacredWeaponBonus = Math.max(1, character.getAbilityModifier(Ability.CHARISMA));
                totalMod += sacredWeaponBonus;
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

                // Colossus Slayer (Ranger Hunter): +1d8 once per turn on a hit
                // against a creature already below max HP.
                int colossusSlayerDamage = 0;
                var colossusSlayerOpt = character.getFeature(
                    RangerFeatures.COLOSSUS_SLAYER_ID);
                if (colossusSlayerOpt.isPresent()
                    && colossusSlayerOpt.get() instanceof
                        RangerFeatures.ColossusSlayer colossus
                    && colossus.canUse()
                    && weapon != null
                    && target.getCurrentHitPoints() < target.getMaxHitPoints()) {
                    colossusSlayerDamage = Dice.rollMultiple(1, 8);
                    if (isCrit) {
                        colossusSlayerDamage += Dice.rollMultiple(1, 8);
                    }
                    damage += colossusSlayerDamage;
                    colossus.use();
                }

                // Horde Breaker (Ranger Hunter): grant one extra weapon attack
                // once per turn, drawing from the bonus-attack budget. Requires
                // a "different target" — only fires when 2+ enemies are alive.
                var hordeBreakerOpt = character.getFeature(
                    RangerFeatures.HORDE_BREAKER_ID);
                if (hordeBreakerOpt.isPresent()
                    && hordeBreakerOpt.get() instanceof
                        RangerFeatures.HordeBreaker horde
                    && horde.canUse()
                    && weapon != null
                    && getLivingEnemies().size() >= 2) {
                    flurryAttacksRemaining += 1;
                    horde.use();
                }

                // Divine Smite (Paladin): expend a spell slot for radiant damage on a melee hit
                int smiteDamage = 0;
                if (smiteReady && weapon != null && !isRangedAttack) {
                    var smiteFeature = character.getFeature(PaladinFeatures.DIVINE_SMITE_ID);
                    if (smiteFeature.isPresent() &&
                        smiteFeature.get() instanceof PaladinFeatures.DivineSmite divineSmite) {
                        int slotLevel = divineSmite.getLowestAvailableSlot();
                        if (slotLevel > 0) {
                            int diceCount = divineSmite.getSmiteDice(slotLevel, false);
                            smiteDamage = Dice.rollMultiple(diceCount, 8);
                            if (isCrit) {
                                smiteDamage += Dice.rollMultiple(diceCount, 8);
                            }
                            damage += smiteDamage;
                            divineSmite.expendSlot(slotLevel);
                            smiteReady = false;
                        }
                    }
                }

                damage = Math.max(1, damage); // Minimum 1 damage

                target.takeDamage(damage);

                // Track aggro - target remembers who hit them
                lastAttacker.put(target, attacker);

                // Stunning Strike (Monk L5+): primed by playerTurn("stun");
                // expends 1 ki on the next melee hit and forces a CON save.
                // Fires on unarmed (null weapon) or any non-ranged weapon.
                String stunningStrikeTag = null;
                if (stunningStrikeReady && !isRangedAttack
                    && target.isAlive() && target instanceof Monster mTarget) {
                    stunningStrikeReady = false;
                    var kiOpt = character.getFeature(MonkFeatures.KI_ID);
                    if (kiOpt.isPresent()
                        && kiOpt.get() instanceof MonkFeatures.Ki ki
                        && ki.getKiPoints() >= 1) {
                        ki.spendKi(1);
                        int dc = MonkFeatures.getKiSaveDC(character);
                        int roll = Dice.rollWithModifier(20, mTarget.getConstitutionMod());
                        if (roll < dc) {
                            statusEffectManager.applyEffect(mTarget,
                                com.questkeeper.combat.status.ConditionEffect.stunned(1));
                            stunningStrikeTag = String.format(
                                "[STUNNING STRIKE: %s STUNNED! (CON %d vs DC %d)]",
                                mTarget.getName(), roll, dc);
                        } else {
                            stunningStrikeTag = String.format(
                                "[STUNNING STRIKE: %s saves (CON %d vs DC %d)]",
                                mTarget.getName(), roll, dc);
                        }
                    }
                }

                StringBuilder specialEffects = new StringBuilder();
                if (rageDamage > 0) {
                    specialEffects.append(String.format("[RAGE +%d!]", rageDamage));
                }
                if (sneakAttackDamage > 0) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append(String.format("[SNEAK ATTACK +%d!]", sneakAttackDamage));
                }
                if (smiteDamage > 0) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append(String.format("[DIVINE SMITE +%d radiant!]", smiteDamage));
                }
                if (stunningStrikeTag != null) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append(stunningStrikeTag);
                }
                if (colossusSlayerDamage > 0) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append(String.format(
                        "[COLOSSUS SLAYER +%d!]", colossusSlayerDamage));
                }
                if (sacredWeaponBonus > 0) {
                    if (specialEffects.length() > 0) specialEffects.append(" ");
                    specialEffects.append(String.format("[SACRED WEAPON +%d attack!]", sacredWeaponBonus));
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

        // Reset per-turn flags for player. Note that patientDefenseActive
        // is *not* cleared here — Patient Defense is supposed to last
        // until the start of the monk's next turn, so it persists
        // through intervening enemy turns and is cleared in executeTurn
        // when the player's next turn begins.
        if (current instanceof Character character) {
            bonusActionUsed = false;
            actionSurgeActive = false;
            sneakAttackUsed = false;
            disengageActive = false;
            flurryAttacksRemaining = 0;
            smiteReady = false;  // Smite expires if not used
            stunningStrikeReady = false;  // Stunning Strike expires unused
            character.resetRecklessAttack();

            // Reset Hunter's Prey once-per-turn riders.
            character.getFeature(RangerFeatures.COLOSSUS_SLAYER_ID)
                .filter(f -> f instanceof RangerFeatures.ColossusSlayer)
                .map(f -> (RangerFeatures.ColossusSlayer) f)
                .ifPresent(RangerFeatures.ColossusSlayer::resetTurn);
            character.getFeature(RangerFeatures.HORDE_BREAKER_ID)
                .filter(f -> f instanceof RangerFeatures.HordeBreaker)
                .map(f -> (RangerFeatures.HordeBreaker) f)
                .ifPresent(RangerFeatures.HordeBreaker::resetTurn);

            // Process Sacred Weapon duration
            character.getFeature(PaladinFeatures.SACRED_WEAPON_ID)
                .filter(f -> f instanceof PaladinFeatures.SacredWeapon)
                .map(f -> (PaladinFeatures.SacredWeapon) f)
                .ifPresent(sw -> {
                    if (!sw.processTurnEnd()) {
                        sacredWeaponActive = false;
                    }
                });

            // Process Rage duration (decrements roundsRemaining; auto-ends at 0)
            character.getFeature(BarbarianFeatures.RAGE_ID)
                .filter(f -> f instanceof BarbarianFeatures.Rage)
                .map(f -> (BarbarianFeatures.Rage) f)
                .ifPresent(BarbarianFeatures.Rage::processTurnEnd);

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

        boolean wasFlurryAttack =
            mainActionAttacksRemaining == 0 && flurryAttacksRemaining > 0;

        CombatResult attackResult = processAttack(getPlayer(), targetEnemy);

        // Consume one attack from whichever budget has charges. Main-action
        // budget (Extra Attack) drains first; bonus-action attack pools
        // (Flurry of Blows, eventually Frenzy) drain after.
        if (mainActionAttacksRemaining > 0) {
            mainActionAttacksRemaining--;
        } else if (flurryAttacksRemaining > 0) {
            flurryAttacksRemaining--;
        }

        // Open Hand Technique (Way of the Open Hand, Lvl 3 Monk): a
        // landing Flurry attack imposes a DEX save vs the monk's Ki DC;
        // failure knocks the target prone.
        if (wasFlurryAttack
            && attackResult.getType() == CombatResult.Type.ATTACK_HIT
            && targetEnemy.isAlive()
            && getPlayer() instanceof Character monk
            && monk.getFeature(MonkFeatures.OPEN_HAND_TECHNIQUE_ID).isPresent()
            && targetEnemy instanceof Monster m) {
            int dc = MonkFeatures.getKiSaveDC(monk);
            int roll = Dice.rollWithModifier(20, m.getDexterityMod());
            if (roll < dc) {
                statusEffectManager.applyEffect(m,
                    com.questkeeper.combat.status.ConditionEffect.prone());
                attackResult = CombatResult.info(attackResult.getMessage()
                    + String.format(
                        "\n[OPEN HAND: %s knocked PRONE! (DEX %d vs DC %d)]",
                        m.getName(), roll, dc));
            }
        }

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

            // Action Surge: refill the attack budget for another full action.
            if (actionSurgeActive) {
                actionSurgeActive = false;
                if (getPlayer() instanceof Character ch) {
                    mainActionAttacksRemaining = ch.getAttacksPerTurn();
                }
                String message = attackResult.getMessage() + "\n" +
                    targetEnemy.getName() + " is defeated!\n" +
                    "[Action Surge: You can take another action!]";
                return CombatResult.info(message);
            }

            // Extra Attack / Flurry: still have attacks in any budget — keep turn open.
            int remaining = mainActionAttacksRemaining + flurryAttacksRemaining;
            if (remaining > 0) {
                String message = attackResult.getMessage() + "\n" +
                    targetEnemy.getName() + " is defeated!\n" +
                    "[" + remaining + " attack(s) remaining.]";
                return CombatResult.info(message);
            }

            advanceTurn();
            return CombatResult.enemyDefeated((Monster) targetEnemy);
        }

        // Action Surge: refill the attack budget for another full action.
        if (actionSurgeActive) {
            actionSurgeActive = false;
            if (getPlayer() instanceof Character ch) {
                mainActionAttacksRemaining = ch.getAttacksPerTurn();
            }
            String message = attackResult.getMessage() + "\n[Action Surge: You can take another action!]";
            return CombatResult.info(message);
        }

        // Extra Attack / Flurry: still have attacks in any budget — keep turn open.
        int remaining = mainActionAttacksRemaining + flurryAttacksRemaining;
        if (remaining > 0) {
            String message = attackResult.getMessage() + "\n" +
                "[" + remaining + " attack(s) remaining.]";
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

        // Disengage (Cunning Action / Step of the Wind): movement doesn't
        // provoke opportunity attacks. Skip the OA loop entirely.
        if (disengageActive) {
            fleeMessage.append("You disengage cleanly — no opportunity attacks!\n");
        } else {
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
     * Handles Frenzy (Barbarian Berserker, Lvl 3). Bonus action while
     * raging; grants one bonus melee attack on this turn. The post-rage
     * exhaustion penalty is not modeled (post-pitch concern).
     */
    private CombatResult handleFrenzy() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }
        var feature = player.getFeature(BarbarianFeatures.FRENZY_ID);
        if (feature.isEmpty()) {
            return CombatResult.error("You don't have the Frenzy ability.");
        }
        if (!player.isRaging()) {
            return CombatResult.error("You can only enter a Frenzy while raging.");
        }
        CombatResult bonusCheck = requireBonusAction();
        if (bonusCheck != null) return bonusCheck;

        // Grant one bonus melee attack via the shared bonus-attack budget
        // (drains in handlePlayerAttack after main action is exhausted).
        flurryAttacksRemaining = 1;
        consumeBonusAction();

        return CombatResult.info(
            player.getName() + " enters a Frenzy! "
            + "[1 bonus melee attack available this turn.]");
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
     * Stunning Strike (Monk L5+) — primes the next melee hit to spend 1 ki
     * and force a CON save vs the monk's Ki DC. On fail, target gains the
     * STUNNED condition until end of monk's next turn.
     */
    private CombatResult handleStunningStrike() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }
        if (player.getFeature(MonkFeatures.STUNNING_STRIKE_ID).isEmpty()) {
            return CombatResult.error(
                "You don't have Stunning Strike yet (requires Monk level 5).");
        }
        var kiOpt = player.getFeature(MonkFeatures.KI_ID);
        if (kiOpt.isEmpty() || ((MonkFeatures.Ki) kiOpt.get()).getKiPoints() < 1) {
            return CombatResult.error("Not enough ki to ready Stunning Strike.");
        }
        if (stunningStrikeReady) {
            return CombatResult.error("Stunning Strike is already primed.");
        }
        stunningStrikeReady = true;
        return CombatResult.info(
            player.getName() + " readies a Stunning Strike. "
            + "[Next melee hit forces a CON save or the target is stunned.]");
    }

    public boolean isStunningStrikeReady() {
        return stunningStrikeReady;
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
    public int getMainActionAttacksRemaining() {
        return mainActionAttacksRemaining;
    }

    public boolean isBonusActionUsed() {
        return bonusActionUsed;
    }

    public boolean isReactionUsed() {
        return reactionUsed;
    }

    /**
     * Helper for bonus-action handlers. Returns a CombatResult error if the
     * player has already spent their bonus action this turn; null otherwise.
     * Pattern: `CombatResult err = requireBonusAction(); if (err != null) return err;`
     */
    private CombatResult requireBonusAction() {
        if (bonusActionUsed) {
            return CombatResult.error("You've already used your bonus action this turn.");
        }
        return null;
    }

    /**
     * Marks the bonus action as consumed for this turn. Call after the
     * handler has confirmed the action will succeed.
     */
    private void consumeBonusAction() {
        bonusActionUsed = true;
    }

    /**
     * True if the player has a reaction available this turn. Reactions
     * fire automatically (e.g., Deflect Missiles when hit by ranged) so
     * callers check this guard, then call consumeReaction() if they fire.
     * Refreshed at the start of each player turn.
     */
    boolean reactionAvailable() {
        return !reactionUsed;
    }

    /**
     * Marks the reaction as spent for this turn. Reactions are once per
     * round (5e); the next refresh is at the start of the player's next
     * turn. Phase 1.3 establishes the API; specific reactions (Deflect
     * Missiles, Uncanny Dodge) wire in during Phase 2.
     */
    void consumeReaction() {
        reactionUsed = true;
    }

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
     * Turn the Unholy (Channel Divinity, Lvl 3 Paladin). Each living
     * fiend or undead enemy makes a WIS save vs the paladin's CD; failure
     * applies FRIGHTENED for 3 rounds. Spends one Channel Divinity use.
     */
    private CombatResult handleTurnTheUnholy() {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }
        var channelFeature = player.getFeature(PaladinFeatures.CHANNEL_DIVINITY_ID);
        if (channelFeature.isEmpty()) {
            return CombatResult.error("You don't have Channel Divinity yet (requires level 3).");
        }
        PaladinFeatures.ChannelDivinity channel =
            (PaladinFeatures.ChannelDivinity) channelFeature.get();
        if (!channel.canUse()) {
            return CombatResult.error(
                "You've already used Channel Divinity. Take a short rest to restore it.");
        }
        var ttuFeature = player.getFeature(PaladinFeatures.TURN_THE_UNHOLY_ID);
        if (ttuFeature.isEmpty()) {
            return CombatResult.error("You don't have Turn the Unholy.");
        }
        PaladinFeatures.TurnTheUnholy ttu = (PaladinFeatures.TurnTheUnholy) ttuFeature.get();
        int dc = ttu.getSaveDC(player);

        StringBuilder log = new StringBuilder();
        log.append(String.format(
            "%s presents a holy symbol! [Turn the Unholy: WIS save DC %d]\n",
            player.getName(), dc));

        int affected = 0;
        for (Combatant target : getLivingEnemies()) {
            if (!(target instanceof Monster m)) continue;
            if (m.getType() != Monster.MonsterType.UNDEAD
                && m.getType() != Monster.MonsterType.FIEND) {
                continue;
            }
            int roll = Dice.rollWithModifier(20, m.getWisdomMod());
            if (roll >= dc) {
                log.append(String.format("  %s saves (rolled %d).\n", m.getName(), roll));
            } else {
                statusEffectManager.applyEffect(m,
                    com.questkeeper.combat.status.ConditionEffect.frightened(3));
                affected++;
                log.append(String.format("  %s is TURNED! (rolled %d)\n",
                    m.getName(), roll));
            }
        }
        if (affected == 0 && log.indexOf("\n  ") < 0) {
            log.append("  No fiends or undead in sight.\n");
        }

        channel.use(player);
        return CombatResult.info(log.toString().trim());
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
     * Handles casting a spell in combat.
     * Format: "cast <spell name> [on <target>]"
     */
    private CombatResult handleCastSpell(String spellInput) {
        Character player = (Character) getPlayer();
        if (player == null) {
            return CombatResult.error("No player character found.");
        }

        Spellbook spellbook = player.getSpellbook();
        if (!spellbook.canCastSpells()) {
            return CombatResult.error("You don't have spellcasting ability.");
        }

        if (spellInput == null || spellInput.trim().isEmpty()) {
            // Show available spells
            return showAvailableSpells(spellbook);
        }

        // Parse spell name and optional target
        String spellName;
        String targetName = null;
        String input = spellInput.trim().toLowerCase();

        // Check for "on <target>" suffix
        int onIndex = input.lastIndexOf(" on ");
        if (onIndex > 0) {
            spellName = input.substring(0, onIndex).trim();
            targetName = input.substring(onIndex + 4).trim();
        } else {
            spellName = input;
        }

        // Find the spell
        Spell spell = SpellRegistry.getSpellByName(spellName);
        if (spell == null) {
            return CombatResult.error("Unknown spell: " + spellName +
                "\nTry: cast fire bolt, cast magic missile, cast cure wounds");
        }

        // Check if we can cast it
        if (!spellbook.canCast(spell.getId())) {
            if (spell.getLevel() == 0) {
                return CombatResult.error("You don't know the " + spell.getName() + " cantrip.");
            } else {
                return CombatResult.error("You can't cast " + spell.getName() +
                    ". Either it's not prepared or you have no spell slots.");
            }
        }

        // Determine target
        Combatant target = null;
        if (spell.canTargetEnemy()) {
            if (targetName != null) {
                Optional<Combatant> found = findEnemyByName(targetName);
                if (found.isEmpty()) {
                    return CombatResult.error("Can't find enemy: " + targetName);
                }
                target = found.get();
            } else {
                // Default to first living enemy for offensive spells
                List<Combatant> enemies = getLivingEnemies();
                if (!enemies.isEmpty()) {
                    target = enemies.get(0);
                }
            }
        } else if (spell.canTargetAlly()) {
            // Healing/buff spells default to self
            target = player;
        }

        // Cast the spell
        SpellResult result = spellbook.cast(spell.getId(), player, target);

        if (result.getType() == SpellResult.Type.ERROR) {
            return CombatResult.error(result.getMessage());
        }

        // Convert SpellResult to CombatResult
        StringBuilder message = new StringBuilder(result.getMessage());

        // Check if target died (combat end is handled by advanceTurn)
        if (target != null && !target.isAlive() && isEnemy(target)) {
            message.append("\n").append(target.getName()).append(" is defeated!");
        }

        // End turn after casting (unless it's a cantrip with leftover action from Action Surge)
        if (!actionSurgeActive) {
            advanceTurn();
        } else {
            actionSurgeActive = false;
            message.append("\n(Action Surge allows another action this turn)");
        }

        return CombatResult.info(message.toString());
    }

    /**
     * Shows available spells when no spell name is given.
     */
    private CombatResult showAvailableSpells(Spellbook spellbook) {
        StringBuilder sb = new StringBuilder("Available spells:\n");

        var cantrips = spellbook.getKnownCantrips();
        if (!cantrips.isEmpty()) {
            sb.append("Cantrips: ");
            sb.append(String.join(", ", cantrips.stream().map(Spell::getName).toList()));
            sb.append("\n");
        }

        var prepared = spellbook.getPreparedSpells();
        if (!prepared.isEmpty()) {
            sb.append("Prepared: ");
            sb.append(String.join(", ", prepared.stream().map(Spell::getName).toList()));
            sb.append("\n");
        }

        if (spellbook.getSpellSlots() != null) {
            sb.append(spellbook.getSpellSlots().getStatus());
        }

        sb.append("\nUsage: cast <spell name> [on <target>]");
        return CombatResult.info(sb.toString());
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
