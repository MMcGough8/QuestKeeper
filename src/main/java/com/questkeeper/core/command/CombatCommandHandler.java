package com.questkeeper.core.command;

import com.questkeeper.combat.CombatResult;
import com.questkeeper.combat.Monster;
import com.questkeeper.ui.Display;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles combat-related commands: attack.
 *
 * This handler initiates combat when the player attacks a monster.
 * The actual combat loop is run by GameEngine after receiving COMBAT_STARTED.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class CombatCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of("attack");

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        return handleAttack(context, noun);
    }

    private CommandResult handleAttack(GameContext context, String target) {
        // If already in combat, let the combat loop handle it
        if (context.isInCombat()) {
            // This shouldn't normally happen since the combat loop handles attacks
            // But just in case, delegate to combat system
            CombatResult result = context.getCombatSystem().playerTurn("attack", target);
            return CommandResult.success(result.getMessage());
        }

        // Not in combat - start combat with a monster
        if (target == null || target.isEmpty()) {
            Display.showError("Attack what? Use 'attack <monster>' to start combat.");
            return CommandResult.failure("No target specified");
        }

        // Find monster to fight
        Monster monster = findMonsterByName(context, target);
        if (monster == null) {
            // If the target is actually an NPC at this location, redirect
            // toward `talk` rather than dumping the whole monster catalog.
            for (String npcId : context.getCurrentLocation().getNpcs()) {
                var npc = context.getCampaign().getNPC(npcId);
                if (npc != null && npc.getName().toLowerCase()
                        .contains(target.toLowerCase())) {
                    Display.showError("You don't want to attack "
                        + npc.getName() + ". Try `talk " + target + "` instead.");
                    return CommandResult.failure("Target is an NPC");
                }
            }
            Display.showError("There's no '" + target + "' here to fight.");
            return CommandResult.failure("Monster not found");
        }

        // Start combat
        startCombat(context, monster);
        return CommandResult.combatStarted();
    }

    private Monster findMonsterByName(GameContext context, String name) {
        String searchTerm = name.trim().toLowerCase();
        Map<String, Monster> templates = context.getCampaign().getMonsterTemplates();

        // Prefer exact id or full-name match so accidental typos like
        // `attack i` don't summon a random monster from anywhere in the
        // campaign. Falls back to a partial match only if no exact match
        // and the term is at least three characters.
        for (String id : templates.keySet()) {
            Monster template = templates.get(id);
            if (id.equalsIgnoreCase(searchTerm) ||
                template.getName().equalsIgnoreCase(searchTerm)) {
                return context.getCampaign().createMonster(id);
            }
        }
        if (searchTerm.length() < 3) {
            return null;
        }
        for (String id : templates.keySet()) {
            Monster template = templates.get(id);
            if (template.getName().toLowerCase().contains(searchTerm) ||
                id.toLowerCase().contains(searchTerm)) {
                return context.getCampaign().createMonster(id);
            }
        }
        return null;
    }

    private void startCombat(GameContext context, Monster monster) {
        Display.println();
        Display.printDivider('=', 60, RED);
        Display.println(Display.colorize("  COMBAT ENCOUNTER!", RED));
        Display.printDivider('=', 60, RED);
        Display.println();

        CombatResult startResult = context.getCombatSystem().startCombat(
            context.getGameState(), List.of(monster));
        displayCombatResult(startResult, context);
    }

    private void displayCombatResult(CombatResult result, GameContext context) {
        if (result == null) {
            return;
        }

        Display.println();

        if (result.getType() == CombatResult.Type.COMBAT_START) {
            Display.println(result.getMessage());
            Display.println();
            Display.println(Display.colorize("Initiative Order:", CYAN));
            int i = 1;
            for (var c : result.getTurnOrder()) {
                Display.println(String.format("  %d. %s", i++, c.getCombatStatus()));
            }
        } else if (result.isError()) {
            Display.showError(result.getMessage());
        } else {
            Display.println(result.getMessage());
        }
    }
}
