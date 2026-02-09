package com.questkeeper.core.command;

import com.questkeeper.campaign.MiniGame;
import com.questkeeper.campaign.Trial;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.core.Dice;
import com.questkeeper.ui.Display;

import java.util.List;
import java.util.Set;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Handles trial-related commands: trial, attempt, solve, try.
 *
 * Trials are multi-challenge encounters that test the player's skills.
 * Each trial contains one or more mini-games that must be completed.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class TrialCommandHandler implements CommandHandler {

    private static final Set<String> HANDLED_VERBS = Set.of("trial", "attempt", "solve", "try");

    @Override
    public Set<String> getHandledVerbs() {
        return HANDLED_VERBS;
    }

    @Override
    public CommandResult handle(GameContext context, String verb, String noun, String fullInput) {
        return switch (verb) {
            case "trial" -> handleTrial(context);
            case "attempt", "solve", "try" -> handleAttempt(context, noun);
            default -> CommandResult.failure("Unknown trial command: " + verb);
        };
    }

    // ==========================================
    // Trial Command
    // ==========================================

    private CommandResult handleTrial(GameContext context) {
        String locationId = context.getCurrentLocation().getId();
        Trial trial = context.getCampaign().getTrialAtLocation(locationId);

        if (trial == null) {
            Display.showError("There's no trial at this location.");
            listAvailableTrials(context);
            return CommandResult.failure("No trial at location");
        }

        if (context.getGameState().hasCompletedTrial(trial.getId())) {
            Display.println("You've already completed this trial.");
            return CommandResult.success();
        }

        if (!checkTrialPrerequisites(context, trial)) {
            Display.showError("You haven't completed the prerequisites for this trial.");
            return CommandResult.failure("Prerequisites not met");
        }

        // Start or continue the trial
        if (!context.getGameState().hasStartedTrial(trial.getId())) {
            startTrial(context, trial);
        } else {
            displayTrialStatus(context, trial);
        }

        return CommandResult.success();
    }

    private boolean checkTrialPrerequisites(GameContext context, Trial trial) {
        if (!trial.hasPrerequisites()) {
            return true;
        }

        for (String prerequisite : trial.getPrerequisites()) {
            if (!context.getGameState().hasFlag(prerequisite)) {
                return false;
            }
        }
        return true;
    }

    private void listAvailableTrials(GameContext context) {
        var trials = context.getCampaign().getTrials();
        if (!trials.isEmpty()) {
            Display.println("Trials in this campaign:");
            for (Trial t : trials.values()) {
                String status = context.getGameState().hasCompletedTrial(t.getId()) ? "[COMPLETE]" :
                               context.getGameState().hasStartedTrial(t.getId()) ? "[IN PROGRESS]" : "[NOT STARTED]";
                Display.println("  - " + Display.colorize(t.getName(), MAGENTA) +
                    " at " + t.getLocation() + " " + status);
            }
        }
    }

    private void startTrial(GameContext context, Trial trial) {
        context.setActiveTrial(trial);

        Display.println();
        Display.printDivider('=', 60, MAGENTA);
        Display.showTrialHeader(trial.getName(), getTrialNumber(trial));
        Display.printDivider('=', 60, MAGENTA);
        Display.println();

        // Mark the trial as started in GameState
        context.getGameState().startTrial(trial.getId());

        // Show quest started notification
        Display.showQuestStarted(trial.getName());

        // Show entry narrative
        String narrative = trial.getEntryNarrative();
        Display.showNarrative(narrative);
        Display.println();

        Display.println("Press Enter to continue...");
        context.readInput();

        displayTrialStatus(context, trial);
    }

    private int getTrialNumber(Trial trial) {
        String id = trial.getId();
        if (id.contains("01")) return 1;
        if (id.contains("02")) return 2;
        if (id.contains("03")) return 3;
        return 0;
    }

    private void displayTrialStatus(GameContext context, Trial trial) {
        Display.println();
        Display.printBox(trial.getName(), 60, MAGENTA);
        Display.println();

        Display.println(Display.colorize("Challenges:", WHITE));
        Display.println();

        int i = 1;
        for (MiniGame game : trial.getMiniGames()) {
            String status = game.isCompleted() ?
                Display.colorize("[COMPLETE]", GREEN) :
                Display.colorize("[INCOMPLETE]", YELLOW);

            Display.println(String.format("  %d. %s %s", i++, game.getName(), status));

            if (!game.isCompleted()) {
                Display.println("     " + Display.colorize(game.getDescription().split("\n")[0], WHITE));
                Display.println("     Skills: " + Display.colorize(getSkillOptions(game), CYAN));
                Display.println("     DC: " + game.getDc());
            }
            Display.println();
        }

        Display.println("Use 'attempt <challenge name>' or 'attempt <number>' to try a challenge.");
        Display.println("Use 'attempt <challenge> with <skill>' to use a specific skill.");
        Display.println();
    }

    private String getSkillOptions(MiniGame game) {
        StringBuilder sb = new StringBuilder();
        if (game.getRequiredSkill() != null) {
            sb.append(game.getRequiredSkill().getDisplayName());
        }
        if (game.getAlternateSkill() != null) {
            sb.append(" or ").append(game.getAlternateSkill().getDisplayName());
        }
        return sb.toString();
    }

    // ==========================================
    // Attempt Command
    // ==========================================

    private CommandResult handleAttempt(GameContext context, String target) {
        String locationId = context.getCurrentLocation().getId();
        Trial trial = context.getCampaign().getTrialAtLocation(locationId);

        if (trial == null || !context.getGameState().hasStartedTrial(trial.getId())) {
            Display.showError("You're not in an active trial. Use 'trial' to start one.");
            return CommandResult.failure("Not in active trial");
        }

        if (target == null || target.isEmpty()) {
            Display.showError("Attempt what? Use 'attempt <challenge>' or 'attempt <number>'.");
            displayTrialStatus(context, trial);
            return CommandResult.failure("No target specified");
        }

        // Parse "attempt X with Y" syntax
        String challengeName = target;
        String skillName = null;

        if (target.toLowerCase().contains(" with ")) {
            String[] parts = target.toLowerCase().split(" with ");
            challengeName = parts[0].trim();
            skillName = parts.length > 1 ? parts[1].trim() : null;
        }

        // Find the mini-game
        MiniGame game = findMiniGame(trial, challengeName);
        if (game == null) {
            Display.showError("Couldn't find challenge '" + challengeName + "'.");
            displayTrialStatus(context, trial);
            return CommandResult.failure("Challenge not found");
        }

        if (game.isCompleted()) {
            Display.println("You've already completed '" + game.getName() + "'.");
            return CommandResult.success();
        }

        // Determine which skill to use
        Skill skill = determineSkill(game, skillName);
        if (skill == null) {
            Display.showError("Invalid skill. Use: " + getSkillOptions(game));
            return CommandResult.failure("Invalid skill");
        }

        // Attempt the challenge!
        attemptMiniGame(context, trial, game, skill);
        return CommandResult.success();
    }

    private MiniGame findMiniGame(Trial trial, String search) {
        List<MiniGame> games = trial.getMiniGames();

        // Try as number first
        try {
            int index = Integer.parseInt(search.trim()) - 1;
            if (index >= 0 && index < games.size()) {
                return games.get(index);
            }
        } catch (NumberFormatException ignored) {
            // Not a number, try name matching
        }

        // Search by name
        String searchLower = search.toLowerCase();
        for (MiniGame game : games) {
            if (game.getName().toLowerCase().contains(searchLower) ||
                game.getId().toLowerCase().contains(searchLower)) {
                return game;
            }
        }

        return null;
    }

    private Skill determineSkill(MiniGame game, String skillName) {
        if (skillName != null && !skillName.isEmpty()) {
            // Player specified a skill
            try {
                Skill skill = Skill.valueOf(skillName.toUpperCase().replace(" ", "_"));
                if (game.isValidApproach(skill)) {
                    return skill;
                }
                return null; // Invalid skill for this challenge
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // Default to required skill
        return game.getRequiredSkill();
    }

    private void attemptMiniGame(GameContext context, Trial trial, MiniGame game, Skill skill) {
        Display.println();
        Display.printBox(game.getName(), 60, CYAN);
        Display.println();

        // Show description
        Display.showNarrative(game.getDescription());
        Display.println();

        // Show hint
        if (!game.getHint().isEmpty()) {
            Display.println(Display.colorize("Hint: ", YELLOW) + game.getHint());
            Display.println();
        }

        Display.println(String.format("Attempting with %s (DC %d)...",
            Display.colorize(skill.getDisplayName(), CYAN), game.getDc()));
        Display.println();
        Display.println("Press Enter to roll...");
        context.readInput();

        // Perform the skill check
        try {
            MiniGame.EvaluationResult result = game.evaluate(context.getCharacter(), skill.name());
            displayMiniGameResult(result);

            // Handle rewards and save progress
            if (result.success()) {
                // Save mini-game completion to flags for persistence
                context.getGameState().setFlag("completed_minigame_" + game.getId());

                if (result.hasReward()) {
                    grantMiniGameReward(context, game);
                }
            }

            // Handle consequences
            if (!result.success() && result.hasConsequence()) {
                applyMiniGameConsequence(context, game, result);
            }

            // Check if trial is complete
            if (trial.checkComplete()) {
                completeTrial(context, trial);
            }

        } catch (IllegalArgumentException e) {
            Display.showError(e.getMessage());
        }
    }

    private void displayMiniGameResult(MiniGame.EvaluationResult result) {
        Display.println();
        Display.printDivider('-', 60, WHITE);

        // Show roll
        Display.showSkillCheck(
            result.rollDescription().split("\\+")[1].split("\\(")[0].trim(), // Extract skill name
            result.naturalRoll(),
            result.totalRoll() - result.naturalRoll(),
            extractDC(result.rollDescription()),
            result.success()
        );

        // Natural 20 or 1
        if (result.wasNatural20()) {
            Display.println(Display.colorize("*** NATURAL 20! ***", YELLOW));
        } else if (result.wasNatural1()) {
            Display.println(Display.colorize("*** NATURAL 1! ***", RED));
        }

        Display.println();

        // Show result message
        if (result.success()) {
            Display.println(Display.colorize("SUCCESS!", GREEN));
            Display.println();
            Display.showNarrative(result.message());
        } else {
            Display.println(Display.colorize("FAILURE!", RED));
            Display.println();
            Display.showNarrative(result.message());
        }

        Display.printDivider('-', 60, WHITE);
        Display.println();
    }

    private int extractDC(String rollDescription) {
        try {
            String[] parts = rollDescription.split("DC ");
            if (parts.length < 2) {
                return 10;
            }
            return Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 10;
        }
    }

    private void grantMiniGameReward(GameContext context, MiniGame game) {
        String rewardId = game.getReward();
        if (rewardId == null || rewardId.isEmpty()) {
            return;
        }

        var item = context.getCampaign().getItem(rewardId);
        if (item != null) {
            context.getCharacter().getInventory().addItem(item);
            Display.showItemGained(item.getName(), item.getDescription());
        } else {
            Display.println(Display.colorize("Reward: " + rewardId, YELLOW));
        }
    }

    private void applyMiniGameConsequence(GameContext context, MiniGame game, MiniGame.EvaluationResult result) {
        String consequence = result.consequence();
        Display.println(Display.colorize("Consequence: " + consequence, RED));

        String failDamage = game.getFailConsequence();
        if (failDamage != null && failDamage.contains("damage")) {
            int damage = 1;
            if (failDamage.contains("1d4")) {
                damage = Dice.parse("1d4");
            } else if (failDamage.contains("1d6")) {
                damage = Dice.parse("1d6");
            } else {
                damage = extractDamageNumber(failDamage);
            }

            context.getCharacter().takeDamage(damage);
            Display.println(Display.colorize("You take " + damage + " damage!", RED));
            Display.printHealthBar(
                context.getCharacter().getCurrentHitPoints(),
                context.getCharacter().getMaxHitPoints()
            );
        }
    }

    private int extractDamageNumber(String text) {
        String[] parts = text.split(" ");
        for (String part : parts) {
            try {
                return Integer.parseInt(part);
            } catch (NumberFormatException ignored) {
                // Not a number, continue
            }
        }
        return 1;
    }

    private void completeTrial(GameContext context, Trial trial) {
        Display.println();
        Display.printDivider('=', 60, GREEN);
        Display.println(Display.colorize("  TRIAL COMPLETE!", GREEN));
        Display.printDivider('=', 60, GREEN);
        Display.println();

        // Mark trial as completed in GameState
        context.getGameState().completeTrial(trial.getId());

        // Show clue gained notification
        Display.showClueGained("You've learned more about the mystery!");

        // Grant reward
        String rewardId = trial.getCompletionReward();
        if (rewardId != null && !rewardId.isEmpty()) {
            var item = context.getCampaign().getItem(rewardId);
            if (item != null) {
                context.getCharacter().getInventory().addItem(item);
                Display.showItemGained(item.getName(), item.getDescription());
            } else {
                Display.println(Display.colorize("You received: " + rewardId, YELLOW));
            }
        }

        // Show stinger (villain message)
        String stinger = trial.getStinger();
        if (stinger != null && !stinger.isEmpty()) {
            Display.println();
            Display.showVillainMessage(stinger);
        }

        // Set completion flags
        setTrialCompletionFlags(context, trial);

        Display.println("Press Enter to continue...");
        context.readInput();

        context.setActiveTrial(null);
    }

    private void setTrialCompletionFlags(GameContext context, Trial trial) {
        for (String flag : trial.getCompletionFlags()) {
            context.getGameState().setFlag(flag, true);

            // Check if this flag indicates a location unlock
            if (flag.endsWith("_unlocked")) {
                String locationId = flag.replace("_unlocked", "");
                var location = context.getCampaign().getLocation(locationId);

                if (location != null) {
                    context.getGameState().unlockLocation(locationId);
                    Display.println();
                    Display.println(Display.colorize(
                        "New area unlocked: " + location.getName() + "!", CYAN));
                }
            }

            // Check for campaign completion
            if (flag.equals("campaign_complete")) {
                Display.println();
                Display.printBox("CAMPAIGN COMPLETE!", 60, YELLOW);
                Display.println();
                Display.println(Display.colorize(
                    "Congratulations! You've completed the " + context.getCampaign().getName() + "!", GREEN));
                Display.println();
            }
        }
    }
}
