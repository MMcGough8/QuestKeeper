# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QuestKeeper is a CLI-based D&D 5e adventure game written in Java 17. It features YAML-driven campaign data, a modular item/effect system, and support for multiple campaigns.

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
| `campaign` | YAML campaign loader (`CampaignLoader`) |
| `world` | Location system |
| `ui` | Display and character creation UI |
| `save` | Game state persistence (`SaveState`, `CharacterData`) |

### Key Design Patterns

- **Template Method**: `AbstractItemEffect` defines effect application flow; concrete effects implement specifics
- **Strategy**: `ItemEffect` interface allows swappable effect implementations
- **Factory**: `CampaignLoader` creates entities from YAML templates
- **Combatant Interface**: Both `Character` and `Monster` implement `Combatant` for polymorphic combat

### Data Flow

Campaign data flows from YAML files (`src/main/resources/campaigns/`) through `CampaignLoader`, which returns unmodifiable collections. Monsters are loaded as templates and instantiated via `CampaignLoader.createMonster()`.

## Campaign YAML Structure

Campaign files live in `src/main/resources/campaigns/{campaign-name}/`:

- `campaign.yaml` - Metadata (id, name, starting location, DM notes)
- `npcs.yaml` - NPC definitions with dialogue trees
- `monsters.yaml` - Monster templates with D&D 5e stats
- `items.yaml` - Weapons, armor, and items with properties

## Testing

Tests use JUnit 5 with `@Nested` classes for organization and `@TempDir` for file-based tests. The `CampaignLoaderTest` demonstrates comprehensive YAML parsing tests with temporary file creation.

## Key Implementation Details

- Character ability scores use 1-20 scale with racial bonuses
- 18 skills map to 6 abilities via `Character.Skill` enum
- Item effects are composable - magic items can have multiple effects
- `Optional<T>` is used throughout for null safety
- `CampaignLoader` collects non-fatal errors allowing partial campaign loading
