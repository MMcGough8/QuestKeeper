package com.questkeeper.core.command;

import com.questkeeper.ui.Display;
import com.questkeeper.ui.MapRenderer;

import java.util.Set;

/**
 * Handles the {@code map} command — renders an ASCII world map of
 * the player's discovered locations.
 */
public class MapCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of("map");

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        Display.println(MapRenderer.render(context.getCampaign(), context.getGameState()));
        return CommandResult.success();
    }
}
