package com.questkeeper.combat.status;

import com.questkeeper.character.Character;
import com.questkeeper.combat.Combatant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages status effects on combatants during combat.
 *
 * Tracks active effects per combatant, processes turn start/end effects,
 * and provides queries for the combat system to check how effects modify
 * attacks, movement, and other combat mechanics.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class StatusEffectManager {

    // Maps each combatant to their list of active effects
    private final Map<Combatant, List<StatusEffect>> effectsByTarget;

    public StatusEffectManager() {
        this.effectsByTarget = new HashMap<>();
    }

    // ==========================================
    // Apply / Remove Effects
    // ==========================================

    /**
     * Applies a status effect to a target combatant. If the effect prevents
     * actions (incapacitating conditions: paralyzed, stunned, unconscious,
     * petrified, incapacitated) and the target is a Character, also drops
     * any active concentration spell per 5e RAW.
     *
     * @param target the combatant to apply the effect to
     * @param effect the effect to apply
     */
    public void applyEffect(Combatant target, StatusEffect effect) {
        effectsByTarget.computeIfAbsent(target, k -> new ArrayList<>()).add(effect);
        if (effect.preventsActions() && target instanceof Character character) {
            character.endConcentration();
        }
    }

    /**
     * Removes a specific status effect from a target.
     *
     * @param target the combatant to remove the effect from
     * @param effect the effect to remove
     * @return true if the effect was removed
     */
    public boolean removeEffect(Combatant target, StatusEffect effect) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects != null) {
            return effects.remove(effect);
        }
        return false;
    }

    /**
     * Removes all effects with a specific condition from a target.
     *
     * @param target the combatant to remove effects from
     * @param condition the condition to remove
     * @return the number of effects removed
     */
    public int removeCondition(Combatant target, Condition condition) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects == null) {
            return 0;
        }

        int removed = 0;
        Iterator<StatusEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();
            if (effect.getCondition() == condition) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Removes all effects from a specific source.
     *
     * @param target the combatant to remove effects from
     * @param source the source of effects to remove
     * @return the number of effects removed
     */
    public int removeEffectsFromSource(Combatant target, Combatant source) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects == null) {
            return 0;
        }

        int removed = 0;
        Iterator<StatusEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();
            if (effect.getSource() == source) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Removes all effects from a target.
     *
     * @param target the combatant to clear effects from
     */
    public void clearEffects(Combatant target) {
        effectsByTarget.remove(target);
    }

    /**
     * Clears all effects from all combatants.
     */
    public void clearAllEffects() {
        effectsByTarget.clear();
    }

    // ==========================================
    // Query Effects
    // ==========================================

    /**
     * Gets all active effects on a combatant.
     *
     * @param target the combatant to query
     * @return unmodifiable list of active effects
     */
    public List<StatusEffect> getEffects(Combatant target) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects == null || effects.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(effects));
    }

    /**
     * Checks if a combatant has a specific condition.
     *
     * @param target the combatant to check
     * @param condition the condition to look for
     * @return true if the combatant has the condition
     */
    public boolean hasCondition(Combatant target, Condition condition) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects == null) {
            return false;
        }
        return effects.stream().anyMatch(e -> e.getCondition() == condition);
    }

    /**
     * Checks if a combatant has any active effects.
     *
     * @param target the combatant to check
     * @return true if the combatant has any effects
     */
    public boolean hasAnyEffects(Combatant target) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        return effects != null && !effects.isEmpty();
    }

    // ==========================================
    // Turn Processing
    // ==========================================

    /**
     * Processes the start of a combatant's turn.
     * Checks for effects that expire at turn start and applies ongoing effects.
     *
     * @param combatant the combatant whose turn is starting
     * @return list of messages describing what happened
     */
    public List<String> processTurnStart(Combatant combatant) {
        List<String> messages = new ArrayList<>();
        List<StatusEffect> effects = effectsByTarget.get(combatant);

        if (effects == null || effects.isEmpty()) {
            return messages;
        }

        // Process each effect and collect messages
        for (StatusEffect effect : new ArrayList<>(effects)) { // Copy to avoid concurrent modification
            String message = effect.onTurnStart(combatant);
            if (message != null && !message.isEmpty()) {
                messages.add(message);
            }
        }

        // Remove expired effects
        removeExpiredEffects(combatant);

        return messages;
    }

    /**
     * Processes the end of a combatant's turn.
     * Handles saving throws, duration decrements, and expiration.
     *
     * @param combatant the combatant whose turn is ending
     * @return list of messages describing what happened
     */
    public List<String> processTurnEnd(Combatant combatant) {
        List<String> messages = new ArrayList<>();
        List<StatusEffect> effects = effectsByTarget.get(combatant);

        if (effects == null || effects.isEmpty()) {
            return messages;
        }

        // Process each effect and collect messages
        for (StatusEffect effect : new ArrayList<>(effects)) { // Copy to avoid concurrent modification
            String message = effect.onTurnEnd(combatant);
            if (message != null && !message.isEmpty()) {
                messages.add(message);
            }
        }

        // Remove expired effects
        removeExpiredEffects(combatant);

        return messages;
    }

    /**
     * Removes all expired effects from a combatant.
     */
    private void removeExpiredEffects(Combatant combatant) {
        List<StatusEffect> effects = effectsByTarget.get(combatant);
        if (effects != null) {
            effects.removeIf(StatusEffect::isExpired);
        }
    }

    // ==========================================
    // Combat Queries - Attack Modifiers
    // ==========================================

    /**
     * Checks if a combatant has advantage on attacks due to effects.
     *
     * @param attacker the attacking combatant
     * @return true if the attacker has advantage
     */
    public boolean hasAdvantageOnAttacks(Combatant attacker) {
        List<StatusEffect> effects = effectsByTarget.get(attacker);
        if (effects == null) {
            return false;
        }
        return effects.stream().anyMatch(StatusEffect::grantsAdvantageOnAttacks);
    }

    /**
     * Checks if a combatant has disadvantage on attacks due to effects.
     *
     * @param attacker the attacking combatant
     * @return true if the attacker has disadvantage
     */
    public boolean hasDisadvantageOnAttacks(Combatant attacker) {
        List<StatusEffect> effects = effectsByTarget.get(attacker);
        if (effects == null) {
            return false;
        }
        return effects.stream().anyMatch(StatusEffect::causesDisadvantageOnAttacks);
    }

    /**
     * Checks if attacks against a target have advantage due to the target's effects.
     *
     * @param target the target being attacked
     * @return true if attacks against the target have advantage
     */
    public boolean attacksHaveAdvantageAgainst(Combatant target) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects == null) {
            return false;
        }
        return effects.stream().anyMatch(StatusEffect::grantsAdvantageAgainst);
    }

    // ==========================================
    // Combat Queries - Action Restrictions
    // ==========================================

    /**
     * Checks if a combatant can take actions.
     *
     * @param combatant the combatant to check
     * @return true if the combatant can take actions
     */
    public boolean canTakeActions(Combatant combatant) {
        List<StatusEffect> effects = effectsByTarget.get(combatant);
        if (effects == null) {
            return true;
        }
        return effects.stream().noneMatch(StatusEffect::preventsActions);
    }

    /**
     * Checks if a combatant can move.
     *
     * @param combatant the combatant to check
     * @return true if the combatant can move
     */
    public boolean canMove(Combatant combatant) {
        List<StatusEffect> effects = effectsByTarget.get(combatant);
        if (effects == null) {
            return true;
        }
        return effects.stream().noneMatch(StatusEffect::preventsMovement);
    }

    // ==========================================
    // Combat Queries - Critical Hits
    // ==========================================

    /**
     * Checks if melee attacks that hit a target are automatic critical hits.
     *
     * @param target the target being attacked
     * @return true if melee hits are automatic crits
     */
    public boolean meleeCritsOnHit(Combatant target) {
        List<StatusEffect> effects = effectsByTarget.get(target);
        if (effects == null) {
            return false;
        }
        return effects.stream().anyMatch(StatusEffect::meleeCritsOnHit);
    }

    /**
     * Checks if a combatant automatically fails STR and DEX saves.
     *
     * @param combatant the combatant to check
     * @return true if the combatant auto-fails STR/DEX saves
     */
    public boolean autoFailsStrDexSaves(Combatant combatant) {
        List<StatusEffect> effects = effectsByTarget.get(combatant);
        if (effects == null) {
            return false;
        }
        return effects.stream().anyMatch(StatusEffect::autoFailsStrDexSaves);
    }

    // ==========================================
    // Display
    // ==========================================

    /**
     * Gets a display string showing all active effects on a combatant.
     *
     * @param combatant the combatant to display effects for
     * @return formatted string of effects, or empty string if none
     */
    public String getStatusDisplay(Combatant combatant) {
        List<StatusEffect> effects = effectsByTarget.get(combatant);
        if (effects == null || effects.isEmpty()) {
            return "";
        }

        return effects.stream()
            .map(StatusEffect::getName)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Gets detailed status information for a combatant.
     *
     * @param combatant the combatant to display effects for
     * @return formatted string with full effect details
     */
    public String getDetailedStatus(Combatant combatant) {
        List<StatusEffect> effects = effectsByTarget.get(combatant);
        if (effects == null || effects.isEmpty()) {
            return combatant.getName() + " has no active status effects.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(combatant.getName()).append(" status effects:\n");
        for (StatusEffect effect : effects) {
            sb.append("  - ").append(effect.toString()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Gets a count of combatants with active effects.
     *
     * @return number of combatants being tracked
     */
    public int getTrackedCombatantCount() {
        return (int) effectsByTarget.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .count();
    }

    /**
     * Gets total number of active effects across all combatants.
     *
     * @return total effect count
     */
    public int getTotalEffectCount() {
        return effectsByTarget.values().stream()
            .mapToInt(List::size)
            .sum();
    }
}
