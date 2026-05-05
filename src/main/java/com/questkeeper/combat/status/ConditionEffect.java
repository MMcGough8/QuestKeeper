package com.questkeeper.combat.status;

import com.questkeeper.character.Character.Ability;
import com.questkeeper.combat.Combatant;

/**
 * Concrete implementation of StatusEffect for D&D 5e standard conditions.
 *
 * Each ConditionEffect wraps a Condition enum value and delegates mechanical
 * queries to the condition. Factory methods provide convenient creation of
 * common effect configurations.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class ConditionEffect extends AbstractStatusEffect {

    private final Condition condition;

    /**
     * Creates a condition effect with a fixed duration in rounds.
     */
    private ConditionEffect(Condition condition, int durationRounds) {
        super(
            condition.name().toLowerCase() + "_" + System.currentTimeMillis(),
            condition.getDisplayName(),
            condition.getDescription(),
            durationRounds
        );
        this.condition = condition;
    }

    /**
     * Creates a condition effect that lasts until a saving throw succeeds.
     */
    private ConditionEffect(Condition condition, Ability savingThrowAbility, int saveDC) {
        super(
            condition.name().toLowerCase() + "_" + System.currentTimeMillis(),
            condition.getDisplayName(),
            condition.getDescription(),
            savingThrowAbility,
            saveDC
        );
        this.condition = condition;
    }

    /**
     * Creates a condition effect with a specific duration type.
     */
    private ConditionEffect(Condition condition, DurationType durationType) {
        super(
            condition.name().toLowerCase() + "_" + System.currentTimeMillis(),
            condition.getDisplayName(),
            condition.getDescription(),
            durationType
        );
        this.condition = condition;
    }

    // ==========================================
    // Factory Methods - Standard Conditions
    // ==========================================

    /**
     * Creates a BLINDED condition lasting a number of rounds.
     * Attacks against have advantage, attacks made have disadvantage.
     */
    public static ConditionEffect blinded(int rounds) {
        return new ConditionEffect(Condition.BLINDED, rounds);
    }

    /**
     * Creates a BLINDED condition until a save succeeds.
     */
    public static ConditionEffect blindedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.BLINDED, saveAbility, dc);
    }

    /**
     * Creates a CHARMED condition lasting a number of rounds.
     * Can't attack the charmer.
     */
    public static ConditionEffect charmed(int rounds) {
        return new ConditionEffect(Condition.CHARMED, rounds);
    }

    /**
     * Creates a CHARMED condition until a save succeeds.
     */
    public static ConditionEffect charmedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.CHARMED, saveAbility, dc);
    }

    /**
     * Creates a DEAFENED condition lasting a number of rounds.
     */
    public static ConditionEffect deafened(int rounds) {
        return new ConditionEffect(Condition.DEAFENED, rounds);
    }

    /**
     * Creates a FRIGHTENED condition lasting a number of rounds.
     * Disadvantage on attacks and ability checks while source is visible.
     */
    public static ConditionEffect frightened(int rounds) {
        return new ConditionEffect(Condition.FRIGHTENED, rounds);
    }

    /**
     * Creates a FRIGHTENED condition until a save succeeds.
     */
    public static ConditionEffect frightenedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.FRIGHTENED, saveAbility, dc);
    }

    /**
     * Creates a GRAPPLED condition that lasts indefinitely.
     * Speed is 0. Typically ended by escaping or grappler being incapacitated.
     */
    public static ConditionEffect grappled() {
        return new ConditionEffect(Condition.GRAPPLED, DurationType.INDEFINITE);
    }

    /**
     * Creates a GRAPPLED condition with a save to escape.
     */
    public static ConditionEffect grappledWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.GRAPPLED, saveAbility, dc);
    }

    /**
     * Creates an INCAPACITATED condition lasting a number of rounds.
     * Can't take actions or reactions.
     */
    public static ConditionEffect incapacitated(int rounds) {
        return new ConditionEffect(Condition.INCAPACITATED, rounds);
    }

    /**
     * Creates an INVISIBLE condition lasting a number of rounds.
     * Attacks against have disadvantage, attacks made have advantage.
     */
    public static ConditionEffect invisible(int rounds) {
        return new ConditionEffect(Condition.INVISIBLE, rounds);
    }

    /**
     * Creates a PARALYZED condition lasting a number of rounds.
     * Incapacitated, can't move, auto-fails STR/DEX saves, melee crits.
     */
    public static ConditionEffect paralyzed(int rounds) {
        return new ConditionEffect(Condition.PARALYZED, rounds);
    }

    /**
     * Creates a PARALYZED condition until a save succeeds.
     */
    public static ConditionEffect paralyzedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.PARALYZED, saveAbility, dc);
    }

    /**
     * Creates a PETRIFIED condition (permanent until dispelled).
     * Turned to stone, incapacitated, auto-fails STR/DEX saves.
     */
    public static ConditionEffect petrified() {
        return new ConditionEffect(Condition.PETRIFIED, DurationType.PERMANENT);
    }

    /**
     * Creates a POISONED condition lasting a number of rounds.
     * Disadvantage on attack rolls and ability checks.
     */
    public static ConditionEffect poisoned(int rounds) {
        return new ConditionEffect(Condition.POISONED, rounds);
    }

    /**
     * Creates a POISONED condition until a save succeeds.
     */
    public static ConditionEffect poisonedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.POISONED, saveAbility, dc);
    }

    /**
     * Creates a PRONE condition. RAW says prone lasts until the creature
     * uses half its movement to stand. This sim doesn't model movement
     * costs, so prone auto-recovers at the end of the prone target's
     * next turn — equivalent to standing up at the start of that turn.
     */
    public static ConditionEffect prone() {
        return new ConditionEffect(Condition.PRONE, 1);
    }

    /**
     * Creates a RESTRAINED condition lasting a number of rounds.
     * Speed 0, disadvantage on attacks and DEX saves, attacks against have advantage.
     */
    public static ConditionEffect restrained(int rounds) {
        return new ConditionEffect(Condition.RESTRAINED, rounds);
    }

    /**
     * Creates a RESTRAINED condition until a save succeeds.
     */
    public static ConditionEffect restrainedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.RESTRAINED, saveAbility, dc);
    }

    /**
     * Creates a STUNNED condition lasting a number of rounds.
     * Incapacitated, can't move, auto-fails STR/DEX saves.
     */
    public static ConditionEffect stunned(int rounds) {
        return new ConditionEffect(Condition.STUNNED, rounds);
    }

    /**
     * Creates a STUNNED condition until a save succeeds.
     */
    public static ConditionEffect stunnedWithSave(Ability saveAbility, int dc) {
        return new ConditionEffect(Condition.STUNNED, saveAbility, dc);
    }

    /**
     * Creates an UNCONSCIOUS condition (indefinite until healed).
     * Incapacitated, can't move or speak, drops items, falls prone, melee crits.
     */
    public static ConditionEffect unconscious() {
        return new ConditionEffect(Condition.UNCONSCIOUS, DurationType.INDEFINITE);
    }

    // ==========================================
    // Condition Query
    // ==========================================

    @Override
    public Condition getCondition() {
        return condition;
    }

    // ==========================================
    // Mechanical Effects - Delegated to Condition
    // ==========================================

    @Override
    public boolean grantsAdvantageOnAttacks() {
        return condition.grantsAdvantageOnAttacks();
    }

    @Override
    public boolean causesDisadvantageOnAttacks() {
        return condition.causesDisadvantageOnAttacks();
    }

    @Override
    public boolean grantsAdvantageAgainst() {
        return condition.grantsAdvantageOnAttacksAgainst();
    }

    @Override
    public boolean preventsActions() {
        return condition.causesIncapacitated();
    }

    @Override
    public boolean preventsMovement() {
        return condition.preventsMovement();
    }

    @Override
    public boolean meleeCritsOnHit() {
        return condition.meleeCritsOnHit();
    }

    @Override
    public boolean autoFailsStrDexSaves() {
        return condition.autoFailsStrDexSaves();
    }

    // ==========================================
    // Turn Processing Override
    // ==========================================

    @Override
    public String onTurnStart(Combatant affected) {
        // Check if creature dies from being unconscious at 0 HP
        // (this would normally involve death saving throws, simplified here)
        return super.onTurnStart(affected);
    }

    @Override
    public String onTurnEnd(Combatant affected) {
        return super.onTurnEnd(affected);
    }

    // ==========================================
    // Display
    // ==========================================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(condition.getDisplayName());

        if (getDurationType() == DurationType.ROUNDS && getRemainingDuration() > 0) {
            sb.append(String.format(" (%d rounds)", getRemainingDuration()));
        } else if (getDurationType() == DurationType.UNTIL_SAVE && allowsSavingThrow()) {
            sb.append(String.format(" (DC %d %s save)",
                getSaveDC(), getSavingThrowAbility().getAbbreviation()));
        }

        return sb.toString();
    }
}
