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
            listAvailableMonsters(context);
            return CommandResult.failure("No target specified");
        }

        // Find monster to fight
        Monster monster = findMonsterByName(context, target);
        if (monster == null) {
            Display.showError("There's no '" + target + "' here to fight.");
            listAvailableMonsters(context);
            return CommandResult.failure("Monster not found");
        }

        // Start combat
        startCombat(context, monster);
        return CommandResult.combatStarted();
    }

    private void listAvailableMonsters(GameContext context) {
        Map<String, Monster> monsters = context.getCampaign().getMonsterTemplates();
        if (!monsters.isEmpty()) {
            Display.println("Available monsters in this campaign:");
            for (String id : monsters.keySet()) {
                Monster m = monsters.get(id);
                Display.println("  - " + Display.colorize(m.getName(), RED) +
                    " (CR " + m.getChallengeRating() + ")");
            }
        }
    }

    private Monster findMonsterByName(GameContext context, String name) {
        String searchTerm = name.trim().toLowerCase();
        Map<String, Monster> templates = context.getCampaign().getMonsterTemplates();

        for (String id : templates.keySet()) {
            Monster template = templates.get(id);
            if (template.getName().toLowerCase().contains(searchTerm) ||
                id.toLowerCase().contains(searchTerm)) {
                // Create a new instance from the template
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
