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
mvn test -Dtest=CampaignTest

# Run a specific test method
mvn test -Dtest=CampaignTest#testMethodName

# Run the application
mvn exec:java -Dexec.mainClass="com.questkeeper.Main"
```

## Playing the Game

```bash
# Run the game (interactive mode)
mvn exec:java -Dexec.mainClass="com.questkeeper.Main" -q

# Run automated demo (for Demo Day presentations)
mvn exec:java -Dexec.mainClass="com.questkeeper.demo.AutoDemo" -q
```

**Basic Commands:**
- `look` - Examine current location
- `go <direction>` - Move (north, south, east, door, upstairs, etc.)
- `n`, `s`, `e`, `w`, `ne`, `nw`, `se`, `sw`, `u`, `d` - Shorthand directions
- `leave` / `exit` - Leave current location (finds best exit automatically)
- `talk <npc>` - Start conversation
- `ask about <topic>` - Ask NPC about something
- `buy <item>` - Purchase item from merchant NPC (must be in conversation)
- `bye` - End conversation
- `inventory` / `i` - View items
- `equip <item>` - Equip weapon/armor
- `trial` - Start trial at current location
- `save` / `load` - Save/load game
- `help` - Show all commands

## Architecture

### Package Structure

| Package | Purpose |
|---------|---------|
| `core` | Game engine (`GameEngine`), dice rolling (`Dice`), command parsing (`CommandParser`) |
| `character` | Player characters (`Character`) and NPCs (`NPC`) |
| `combat` | Combat system (`CombatSystem`), monster definitions, `Combatant` interface, `CombatResult` |
| `combat.status` | Status effects system: conditions, durations, effect management |
| `inventory` | Item hierarchy (`Item` → `Weapon`, `Armor`, `MagicItem`), `Inventory` with equipment slots and weight limits, `StandardEquipment` singleton for D&D 5e items |
| `inventory.items.effects` | Item effect system using Template Method pattern |
| `campaign` | Campaign facade (`Campaign`), YAML loader (`CampaignLoader` - internal), trials (`Trial`), mini-games (`MiniGame`) |
| `dialogue` | NPC conversation system (`DialogueSystem`, `DialogueResult`) |
| `state` | Runtime game state tracking (`GameState`) |
| `world` | Location system |
| `ui` | Display and character creation UI |
| `save` | Game state persistence (`SaveState`, `CharacterData`) |
| `demo` | Automated demo for presentations (`AutoDemo`) |

### Key Design Patterns

- **Template Method**: `AbstractItemEffect` defines effect application flow; concrete effects (`StatBonusEffect`, `ResistanceEffect`, `SpellEffect`, `MovementEffect`, `ExtraDamageEffect`, `SkillBonusEffect`, `BonusRollEffect`, `AbilitySetEffect`, `DamageReductionEffect`, `TeleportEffect`, `UtilityEffect`, `DescriptionEffect`) implement specifics
- **Strategy**: `ItemEffect` interface allows swappable effect implementations
- **Facade**: `Campaign` provides clean public API; `CampaignLoader` is package-private implementation
- **Factory**: `Campaign.loadFromYaml()` creates campaigns from YAML directories
- **Combatant Interface**: Both `Character` and `Monster` implement `Combatant` for polymorphic combat

### Data Flow

Campaign data flows from YAML files (`src/main/resources/campaigns/`) through `Campaign.loadFromYaml()`, which internally uses `CampaignLoader` and returns unmodifiable collections. Cross-references between entities (location exits, NPC locations, trial mini-games) are validated on load. Monsters are loaded as templates and instantiated via `Campaign.createMonster()`. Trials reference mini-games by ID.

### Multi-Campaign Support

The system supports multiple campaigns through data-driven design:

- **Campaign Discovery**: `Campaign.listAvailable()` scans `campaigns/` directory for subdirectories with `campaign.yaml`
- **Campaign Selection**: When multiple campaigns exist, GameEngine displays a selection menu
- **State Isolation**: Each `GameState` instance tracks its own location/trial state independently
- **Data-Driven Unlocks**: Location unlocks use `*_unlocked` flag convention (e.g., `clocktower_hill_unlocked` unlocks `clocktower_hill`)
- **State Tracking**: `GameState` tracks visited locations, unlocked locations, started/completed trials (not on Campaign objects)

### Inventory System

The `Inventory` class manages item storage with D&D 5e mechanics:

- **Equipment Slots**: MAIN_HAND, OFF_HAND, ARMOR, HEAD, NECK, RING_LEFT, RING_RIGHT
- **Weight Limits**: Carrying capacity = STR × 15 lbs
- **Item Stacking**: Stackable items (consumables) use `ItemStack` with max stack sizes
- **Two-Handed Weapons**: Auto-unequips off-hand when equipping
- **Standard Equipment**: `StandardEquipment` singleton loads D&D 5e weapons/armor from `src/main/resources/items/standard_equipment.yaml`

### Combat System

The `CombatSystem` class manages turn-based D&D 5e combat:

- **Turn Order**: Initiative-based with d20 + DEX modifier rolls
- **Actions**: Attack, defend, use item, flee, special abilities
- **Attack Resolution**: d20 + attack bonus vs AC, with advantage/disadvantage support
- **Opportunity Attacks**: Triggered when fleeing combat
- **CombatResult**: Immutable result objects with factory methods (`attackHit`, `attackMiss`, `enemyDefeated`, `victory`, `fled`, etc.)

### Game Engine (`GameEngine`)

The `GameEngine` class orchestrates the main game loop:

- **Game Flow**: Title screen → Campaign loading → Character creation → Intro scene → Game loop
- **Campaign Intro**: Displays dramatic opening scene from `campaign.yaml` intro field
- **Location Display**: Shows read-aloud text on first visit, short description on subsequent visits
- **NPC Display**: Shows NPCs with role descriptors (e.g., "Norrin (a bard)")
- **Exit Display**: Shows exits with destinations (e.g., "Exits: north (Town Square), south (Inn)")
- **Navigation**: Shorthand directions (n/s/e/w/ne/nw/se/sw/u/d), `leave`/`exit` finds best exit automatically
- **Commands**: look, go, leave, talk, ask, buy, bye, attack, trial, attempt, inventory, equip, unequip, save, load, quit
- **Dialogue System**: `talk <npc>` starts conversation, `ask about <topic>` queries, `buy <item>` purchases from merchants, `bye` ends conversation
- **Location Unlocking**: Completing trials unlocks new locations (e.g., trial_01 → clocktower_hill)

### Status Effects System (`combat.status` package)

D&D 5e-compliant status effects using composition (not modifying Combatant interface):

| Class | Purpose |
|-------|---------|
| `Condition` | Enum of 14 D&D 5e conditions with mechanical queries |
| `DurationType` | Enum for duration tracking (ROUNDS, UNTIL_SAVE, PERMANENT, etc.) |
| `StatusEffect` | Interface defining status effect contract |
| `AbstractStatusEffect` | Base class with duration/save handling |
| `ConditionEffect` | Concrete implementation with factory methods |
| `StatusEffectManager` | Tracks effects on combatants, processes turns |

**Condition Mechanics:**
- `grantsAdvantageOnAttacksAgainst()` - Restrained, Paralyzed, Stunned, Prone, Unconscious
- `causesDisadvantageOnAttacks()` - Blinded, Frightened, Poisoned, Prone, Restrained
- `preventsMovement()` - Grappled, Paralyzed, Petrified, Restrained, Stunned, Unconscious
- `autoFailsStrDexSaves()` - Paralyzed, Petrified, Stunned, Unconscious
- `meleeCritsOnHit()` - Paralyzed, Unconscious

**Factory Methods:**
```java
ConditionEffect.poisoned(int rounds)
ConditionEffect.restrained(Ability saveAbility, int dc)
ConditionEffect.paralyzed(Ability saveAbility, int dc)
ConditionEffect.stunned(int rounds)
ConditionEffect.blinded(int rounds)
ConditionEffect.grappled()
ConditionEffect.prone()
ConditionEffect.invisible(int rounds)
```

## Campaign YAML Structure

Campaign files live in `src/main/resources/campaigns/{campaign-name}/`:

| File | Purpose |
|------|---------|
| `campaign.yaml` | Metadata (id, name, intro scene, starting location, DM notes) |
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

Tests use JUnit 5 with `@Nested` classes for organization and `@TempDir` for file-based tests. Run `mvn test` for all tests.

**Test organization by package:**
- `campaign/` - `CampaignTest`, `CampaignLoaderTest`, `TrialTest`, `TrialIntegrationTest`, `MiniGameTest`
- `character/` - `CharacterTest`, `NPCTest`
- `combat/` - `CombatSystemTest`, `CombatResultTest`, `MonsterTest`
- `combat/status/` - `ConditionTest`, `ConditionEffectTest`, `StatusEffectManagerTest`, `DurationTypeTest`, `AbstractStatusEffectTest`
- `core/` - `GameEngineTest`, `CommandParserTest`, `DiceTest`, `RestSystemTest`
- `dialogue/` - `DialogueSystemTest`
- `inventory/` - `InventoryTest`, `WeaponTest`, `ArmorTest`, `ItemTest`, `MagicItemTest`
- `inventory/items/effects/` - Tests for each effect type (`StatBonusEffectTest`, `ResistanceEffectTest`, etc.)
- `save/` - `SaveStateTest`
- `state/` - `GameStateTest`
- `ui/` - `DisplayTest`, `CharacterCreatorTest`
- `world/` - `LocationTest`
- Root - `EdgeCaseTest`

## Key Implementation Details

- Character ability scores use 1-20 scale with racial bonuses
- 18 skills map to 6 abilities via `Character.Skill` enum
- Item effects are composable - magic items can have multiple effects
- `Optional<T>` is used throughout for null safety
- `Campaign` validates cross-references on load and exposes validation errors via `getValidationErrors()`
- Mini-game `evaluate()` method rolls d20 + skill modifier vs DC
- Standard D&D equipment (weapons, armor) has factory methods; campaign-specific content is YAML-only

## Auto-DM Design Principles

1. Always ask for rolls on uncertain actions
2. Keep scenes cinematic, not mechanical dumps
3. Don't reveal hidden structure to players
4. Use comedic consequences over lethal outcomes early
5. Reward curiosity and creative problem-solving
6. Every trial can be won without violence
7. Every trial can be failed without death (unless reckless)

## Available Campaigns

Three campaigns exist at varying difficulty levels:
- **muddlebrook** - Beginner comedic mystery (DCs 10-14)
- **eberron** - Intermediate Olympic competition (DCs 12-16)
- **drownedgod** - Advanced nautical horror (DCs 15-19)

Campaign details are in each campaign's `campaign.yaml` file.

## Adding a New Campaign

Create a new directory under `src/main/resources/campaigns/` with these YAML files:
- `campaign.yaml` (required): id, name, starting_location, intro, description, author, version
- `locations.yaml`: locations with exits, NPCs, items, flags (`locked` flag for initially locked locations, `locked_message` for custom messages)
- `npcs.yaml`: NPC definitions with dialogue trees
- `monsters.yaml`: monster templates with D&D 5e stats
- `items.yaml`: weapons, armor, consumables, magic items
- `trials.yaml`: trial definitions with `prerequisites` and `completion_flags` lists
- `minigames.yaml`: mini-game definitions with skill checks

The campaign selection menu appears automatically when multiple campaigns exist.

## Dependencies

- **SnakeYAML 2.2** - YAML parsing for campaign data
- **Jansi 2.4.1** - Terminal colors for CLI output
- **JUnit 5.10.1** - Testing framework
