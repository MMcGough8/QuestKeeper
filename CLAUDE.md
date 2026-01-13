# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QuestKeeper is a CLI-based D&D 5e adventure game written in Java 17. It features YAML-driven campaign data, a modular item/effect system, D&D skill check mechanics, and support for multiple campaigns. All campaign-specific content (monsters, NPCs, locations, items, trials, mini-games) is loaded from YAML files.

## Build Commands

```bash
# Build and run all tests
mvn clean install

# Quick build (skip tests)
mvn clean install -P quick

# Run tests only
mvn test

# Run a specific test class
mvn test -Dtest=CampaignLoaderTest

# Run the application
mvn exec:java -Dexec.mainClass="com.questkeeper.Main"

# Generate Javadoc
mvn javadoc:javadoc
```

## Architecture

### Package Structure

| Package | Purpose |
|---------|---------|
| `core` | Dice rolling (`Dice`) and command parsing (`CommandParser`) |
| `character` | Player characters (`Character`) and NPCs (`NPC`) |
| `combat` | Monster definitions and `Combatant` interface for unified combat |
| `inventory` | Item hierarchy: `Item` → `Weapon`, `Armor`, `MagicItem` |
| `inventory.items.effects` | Item effect system using Template Method pattern |
| `campaign` | YAML campaign loader (`CampaignLoader`), trials (`Trial`), mini-games (`MiniGame`) |
| `world` | Location system |
| `ui` | Display and character creation UI |
| `save` | Game state persistence (`SaveState`, `CharacterData`) |

### Key Design Patterns

- **Template Method**: `AbstractItemEffect` defines effect application flow; concrete effects implement specifics
- **Strategy**: `ItemEffect` interface allows swappable effect implementations
- **Factory**: `CampaignLoader` creates entities from YAML templates
- **Combatant Interface**: Both `Character` and `Monster` implement `Combatant` for polymorphic combat

### Data Flow

Campaign data flows from YAML files (`src/main/resources/campaigns/`) through `CampaignLoader`, which returns unmodifiable collections. Monsters are loaded as templates and instantiated via `CampaignLoader.createMonster()`. Trials reference mini-games by ID.

## Campaign YAML Structure

Campaign files live in `src/main/resources/campaigns/{campaign-name}/`:

| File | Purpose |
|------|---------|
| `campaign.yaml` | Metadata (id, name, starting location, DM notes) |
| `npcs.yaml` | NPC definitions with dialogue trees, voice, personality |
| `monsters.yaml` | Monster templates with D&D 5e stats (AC, HP, abilities, attacks) |
| `items.yaml` | Weapons, armor, consumables, quest items, and magic items |
| `locations.yaml` | Location definitions with exits, NPCs, items, and flags |
| `trials.yaml` | Trial (puzzle room) definitions with mini-game references |
| `minigames.yaml` | Mini-game definitions with D&D skill checks, DCs, rewards |

### Trial System

Trials are theatrical puzzle rooms containing mini-games. Each trial has:
- Entry narrative (read-aloud text)
- List of mini-games (referenced by ID)
- Completion reward and stinger message
- Prerequisites (flags from previous trials)

### Mini-Game System

Mini-games use D&D 5e skill checks:
- `required_skill` and `alternate_skill` options
- Difficulty Class (DC) for the check
- Success/failure text and consequences
- Reward items on success

## Testing

Tests use JUnit 5 with `@Nested` classes for organization and `@TempDir` for file-based tests. The `CampaignLoaderTest` demonstrates comprehensive YAML parsing tests with temporary file creation.

## Key Implementation Details

- Character ability scores use 1-20 scale with racial bonuses
- 18 skills map to 6 abilities via `Character.Skill` enum
- Item effects are composable - magic items can have multiple effects
- `Optional<T>` is used throughout for null safety
- `CampaignLoader` collects non-fatal errors allowing partial campaign loading
- Mini-game `evaluate()` method rolls d20 + skill modifier vs DC
- Standard D&D equipment (weapons, armor) has factory methods; campaign-specific content is YAML-only

## Current Campaign: Muddlebrook

The "Muddlebrook: Harlequin Trials" campaign includes:
- 3 trials (Mayor's Office, Clocktower, Harlequin's Stage)
- 14 mini-games with skill checks
- 11 magic item rewards (Tier 1)
- 6 NPCs (Norrin, Mara, Darius, Elara, Captain Thorne, Mayor Alderwick)
- 3 monster types (Clockwork Critter, Confetti Ooze, Harlequin Machinist)
