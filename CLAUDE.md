# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QuestKeeper is a CLI-based D&D 5e adventure game written in Java 17. It features a data-driven architecture where all campaign content (locations, NPCs, monsters, items, trials, mini-games) is defined in YAML files. The game supports multiple campaigns, implements core D&D 5e mechanics (ability scores, skills, combat, conditions), and uses a modular effect system for magic items.

## Build Commands

```bash
mvn clean install              # Build and run all tests
mvn clean install -P quick     # Quick build (skip tests)
mvn test                       # Run tests only
mvn test -Dtest=CampaignTest   # Run specific test class
mvn test -Dtest=CampaignTest#testMethodName  # Run specific test method
```

## Running the Game

```bash
mvn exec:java -Dexec.mainClass="com.questkeeper.Main" -q           # Interactive mode
mvn exec:java -Dexec.mainClass="com.questkeeper.demo.AutoDemo" -q  # Demo mode
java -jar target/questkeeper-1.0.0-SNAPSHOT.jar                    # Run fat JAR (after mvn package)
```

## Architecture Overview

### Core Data Flow

```
YAML Files (campaigns/)
    ↓
Campaign.loadFromYaml() → CampaignLoader (package-private)
    ↓
Campaign (immutable facade) ← validates cross-references
    ↓
GameEngine creates GameState (mutable runtime)
    ↓
Game Loop: input → CommandParser → handler → GameState update → Display
```

**Key Separation**: `Campaign` holds immutable game content (locations, NPCs, items). `GameState` tracks mutable runtime state (current location, flags, visited locations, trial progress).

### Package Structure

| Package | Key Classes | Purpose |
|---------|-------------|---------|
| `core` | `GameEngine` (2,460 lines), `CommandParser`, `Dice`, `RestSystem` | Main orchestration, input parsing, dice mechanics |
| `campaign` | `Campaign`, `CampaignLoader`, `Trial`, `MiniGame` | YAML loading, trial/puzzle system |
| `character` | `Character`, `NPC` | Player (D&D 5e stats) and NPCs (dialogue trees) |
| `combat` | `CombatSystem`, `Monster`, `Combatant`, `CombatResult` | Turn-based combat with initiative |
| `combat.status` | `Condition`, `StatusEffect`, `StatusEffectManager` | 14 D&D 5e conditions, duration tracking |
| `inventory` | `Item`, `Weapon`, `Armor`, `MagicItem`, `Inventory` | Item hierarchy, equipment slots, weight |
| `inventory.items.effects` | `AbstractItemEffect`, 12 concrete effects | Template Method pattern for item effects |
| `dialogue` | `DialogueSystem`, `DialogueResult` | NPC conversations with flag-gated topics |
| `state` | `GameState` | Runtime tracking: flags, counters, variables |
| `world` | `Location` | Location graph with exits, NPCs, items |
| `save` | `SaveState`, `CharacterData` | YAML persistence with atomic writes |
| `ui` | `Display`, `CharacterCreator` | Terminal output (Jansi), character creation |

## GameEngine - The Orchestrator

`GameEngine.java` (~2,460 lines) is the central coordinator managing all game systems.

### Game Flow

```
Main.main() → new GameEngine().start()
    ├── Display.init()
    ├── showTitleScreen()
    ├── loadCampaign()         // Campaign.listAvailable() → selection → load
    ├── createOrLoadCharacter() // CharacterCreator or quick-start
    ├── initializeGameState()
    └── runGameLoop()
        ├── displayCampaignIntro()
        └── while (running):
            ├── generateSuggestions()
            ├── scanner.nextLine()      // blocking input
            └── processCommand()        // dispatch to 25+ handlers
```

### Command Processing

Input flows through `CommandParser` which normalizes 100+ synonyms to 25 canonical verbs:

```
User Input: "grab the sword"
    ↓
CommandParser.parse()
    ├── cleanInput()           // normalize whitespace
    ├── extractVerb("grab")    // first word
    ├── SYNONYM_MAP lookup     // grab → "take"
    └── extractNoun("sword")   // remove articles (the, a, an)
    ↓
Command(verb="take", noun="sword")
    ↓
GameEngine.processCommand() → handleTake("sword")
```

**Synonym Categories:**
- Movement: walk, move, travel, head, run → `go`
- Look: examine, inspect, check, view, observe, search → `look`
- Take: get, grab, pick, pickup, collect → `take`
- Attack: hit, strike, fight, kill, slay → `attack`
- Directions: n/s/e/w/ne/nw/se/sw/u/d → expanded forms

**Core Commands:** `go`, `look`, `take`, `drop`, `talk`, `ask`, `buy`, `bye`, `attack`, `flee`, `use`, `inventory`, `equip`, `unequip`, `character`, `trial`, `attempt`, `rest`, `save`, `load`, `help`, `quit`

### Game Modes

The engine operates in different modes affecting command availability:

| Mode | Context | Available Commands |
|------|---------|-------------------|
| **Exploration** | Normal gameplay | All commands |
| **Dialogue** | `talk <npc>` active | ask, buy, bye, look |
| **Combat** | `CombatSystem.isInCombat()` | attack, flee, use, inventory |
| **Trial** | `activeTrial != null` | attempt, look, inventory |

## Character System

### D&D 5e Implementation

`Character.java` implements full D&D 5e mechanics:

- **6 Ability Scores**: STR, DEX, CON, INT, WIS, CHA (1-20 scale, modifier = (score-10)/2)
- **18 Skills**: Each maps to an ability (e.g., Athletics→STR, Stealth→DEX, Arcana→INT)
- **9 Races**: Human (+1 all), Elf (+2 DEX), Dwarf (+2 CON), Halfling (+2 DEX), etc.
- **12 Classes**: Fighter (d10), Wizard (d6), Rogue (d8), Cleric (d8), etc.
- **Proficiency Bonus**: `(level - 1) / 4 + 2`

### Hit Points

```
Max HP = hit_die + CON_mod + (level-1) * (hit_die/2 + 1 + CON_mod)
```

- **Hit Dice**: Class-based (d6-d12), spent on short rests for `d[die] + CON_mod` healing
- **Long Rest**: Restores all HP, recovers half of max hit dice
- **Temporary HP**: Absorbed before regular damage

### Combatant Interface

Both `Character` and `Monster` implement `Combatant` for polymorphic combat:

```java
interface Combatant {
    String getName();
    int getCurrentHitPoints(), getMaxHitPoints(), getArmorClass();
    int takeDamage(int amount), heal(int amount);
    int rollInitiative();  // d20 + DEX modifier
    boolean isAlive(), isUnconscious(), isBloodied();  // HP <= 50%
}
```

## Combat System

### Turn-Based Flow

1. **Initiative**: All combatants roll d20 + DEX mod, sorted descending
2. **Turn Processing**: `StatusEffectManager.processTurnStart()` → action → `processTurnEnd()`
3. **Attack Resolution**: `d20 + attack_bonus` vs AC; hit → roll damage
4. **End Conditions**: All enemies dead (victory), player dead (defeat), or fled

### Monster AI Behaviors

```java
enum Behavior {
    AGGRESSIVE,  // Never flees
    COWARDLY,    // Flees when bloodied (50% HP)
    TACTICAL,    // Uses abilities, targets weakest
    DEFENSIVE    // Flees at 25% HP
}
```

### Flee Mechanics

1. All living enemies get opportunity attacks (full attack resolution)
2. Player makes DEX check vs DC 10
3. Success: escape combat (may have taken opportunity attack damage)
4. Failure: remain in combat, enemy turn begins

### Status Effects

`StatusEffectManager` tracks effects separately from combatants (composition pattern):

```java
// Application
statusEffectManager.applyEffect(target, ConditionEffect.poisoned(3));

// Combat queries
statusEffectManager.hasAdvantageOnAttacks(attacker);
statusEffectManager.attacksHaveAdvantageAgainst(target);
statusEffectManager.canTakeActions(combatant);
statusEffectManager.meleeCritsOnHit(target);  // Paralyzed, Unconscious
```

**14 D&D 5e Conditions**: Blinded, Charmed, Deafened, Frightened, Grappled, Incapacitated, Invisible, Paralyzed, Petrified, Poisoned, Prone, Restrained, Stunned, Unconscious

## Inventory & Items

### Item Hierarchy

```
Item (base)
 ├── Weapon (damage dice, properties, range)
 ├── Armor (AC, category, STR requirement)
 └── MagicItem (effects[], attunement)
```

### Equipment Slots

`Inventory` manages 7 slots: MAIN_HAND, OFF_HAND, ARMOR, HEAD, NECK, RING_LEFT, RING_RIGHT

- **Weight Limit**: STR × 15 lbs
- **Two-Handed**: Auto-unequips off-hand
- **Stacking**: Consumables stack via `ItemStack` (max 99)

### Item Effects (Template Method Pattern)

`AbstractItemEffect` defines the flow; 12 concrete implementations:

| Effect | Purpose | Example |
|--------|---------|---------|
| `StatBonusEffect` | +X to AC, attacks, saves | Ring of Protection |
| `ResistanceEffect` | Damage resistance/immunity | Ring of Fire Resistance |
| `SpellEffect` | Cast spells with charges | Wand of Fireballs |
| `MovementEffect` | Speed bonuses, flying | Boots of Speed |
| `ExtraDamageEffect` | Bonus damage on attacks | Flame Tongue |
| `SkillBonusEffect` | Skill check bonuses | Eyes of the Eagle |
| `BonusRollEffect` | Rerolls, luck effects | Stone of Good Luck |
| `AbilitySetEffect` | Set ability to value | Gauntlets of Ogre Power (STR 19) |
| `DamageReductionEffect` | Reduce incoming damage | Adamantine Armor |
| `TeleportEffect` | Teleportation | Blinkstep Spark |
| `UtilityEffect` | Darkvision, storage, etc. | Goggles of Night |
| `DescriptionEffect` | YAML-defined text effects | Custom campaign items |

### Usage Types

```java
enum UsageType {
    PASSIVE,     // Always active when equipped
    UNLIMITED,   // No restrictions
    DAILY,       // Resets at dawn
    LONG_REST,   // Resets after long rest
    CHARGES,     // Limited charges (may recharge)
    CONSUMABLE   // Single use, then destroyed
}
```

## Campaign System

### YAML Structure

Campaigns live in `src/main/resources/campaigns/{campaign-id}/`:

| File | Required | Content |
|------|----------|---------|
| `campaign.yaml` | Yes | id, name, starting_location, intro, author, version |
| `locations.yaml` | No | Locations with exits, NPCs, items, flags |
| `npcs.yaml` | No | NPCs with dialogue trees, shop items |
| `monsters.yaml` | No | Monster templates (instantiated via `Campaign.createMonster()`) |
| `items.yaml` | No | Weapons, armor, items, magic_items sections |
| `trials.yaml` | No | Trials with mini-game lists, prerequisites |
| `minigames.yaml` | No | Skill checks with DC, rewards, consequences |

### Cross-Reference Validation

`Campaign.validateCrossReferences()` checks 8 reference types on load:

1. Starting location exists
2. Location exits point to valid locations
3. Location NPCs exist
4. Location items exist
5. NPC location references valid
6. Trial location references valid
7. Trial mini-games exist
8. Mini-game reward items exist

Errors accessible via `campaign.getValidationErrors()`.

### Trial & Mini-Game Flow

```
trial command → check prerequisites → show entry narrative → mark started
    ↓
attempt <skill> → MiniGame.evaluate(character, skill)
    ↓
Roll: d20 + skill_modifier vs DC
    ↓
Success: grant reward, show success_text
Failure: apply consequence, show fail_text
    ↓
All mini-games complete → trial.complete() → set completion_flags
```

## GameState - Runtime Tracking

`GameState` maintains all mutable game progress:

### State Categories

```java
// Flags (boolean markers)
gameState.setFlag("met_norrin", true);
gameState.hasFlag("clocktower_hill_unlocked");

// Counters (numeric tracking)
gameState.incrementCounter("enemies_defeated");
gameState.getCounter("clues_found");

// Variables (string storage)
gameState.setVariable("villain_name", "The Machinist");
```

### Flag Conventions

| Pattern | Purpose | Example |
|---------|---------|---------|
| `met_[npc_id]` | NPC first meeting tracked | `met_norrin` |
| `[location_id]_unlocked` | Location accessibility | `clocktower_hill_unlocked` |
| `started_[trial_id]` | Trial begun | `started_trial_01` |
| `completed_[trial_id]` | Trial finished | `completed_trial_01` |
| `campaign_complete` | Campaign finished | Ends game |

### Location Unlocking

Locations with `locked: true` flag require matching unlock flag:

```yaml
# In locations.yaml
- id: clocktower_hill
  flags: [locked]
  locked_message: "The path is blocked by rubble."

# In trials.yaml - completion_flags
completion_flags:
  - completed_trial_01
  - clocktower_hill_unlocked  # Unlocks the location
```

## Dialogue System

### Conversation Flow

```
talk <npc> → DialogueSystem.startDialogue()
    ├── Find NPC (case-insensitive, partial match)
    ├── Set flag: met_[npc_id]
    ├── Return greeting (initial or return based on met flag)
    └── Return available topics (filtered by flags)
        ↓
ask about <topic> → DialogueSystem.askAbout()
    ├── Validate topic available (flag-gated)
    └── Return NPC response + updated topics
        ↓
bye → DialogueSystem.endDialogue()
```

### Flag-Gated Dialogue

NPCs can have topics that only appear when certain flags are set:

```yaml
# In npcs.yaml
dialogues:
  rumors:
    text: "I heard strange noises from the clocktower..."
  secret:
    text: "Now that you've proven yourself..."
    required_flags: [completed_trial_01]
```

## Save System

### Atomic Persistence

`SaveState` uses atomic writes to prevent corruption:

1. Write to temporary file
2. Atomic rename to target path
3. YAML format (human-readable, version-controlled)

### Save Contents

```yaml
save_version: "1.0"
timestamp: "2026-01-27T14:30:00Z"
campaign_id: "muddlebrook"
character:
  name: "Aelar"
  race: "HUMAN"
  class: "FIGHTER"
  level: 1
  ability_scores: {STRENGTH: 16, DEXTERITY: 14, ...}
  current_hp: 11
  max_hp: 11
current_location: "drunken_dragon_inn"
visited_locations: [drunken_dragon_inn, town_square]
flags: {met_norrin: true, completed_trial_01: true}
counters: {enemies_defeated: 3}
inventory: [longsword, scale_mail]
equipped_slots: {MAIN_HAND: longsword, ARMOR: scale_mail}
gold: 150
play_time_seconds: 1425
```

## Dice System

`Dice.java` provides thread-safe D&D dice mechanics:

```java
// Basic rolls
Dice.roll(20);                    // 1d20
Dice.rollMultiple(2, 6);          // 2d6, returns sum

// D&D mechanics
Dice.rollWithAdvantage();         // Roll 2d20, take highest
Dice.rollWithDisadvantage();      // Roll 2d20, take lowest
Dice.checkAgainstDC(modifier, dc); // d20 + mod >= DC

// Notation parsing
Dice.parse("2d6+3");              // Returns rolled total
Dice.parseDetailed("1d20+5");     // Returns RollResult with breakdown

// History (synchronized, max 1000 entries)
Dice.getLastRoll();               // "d20: 15 + 3 = 18"
Dice.wasNatural20();              // Check for crit
Dice.wasNatural1();               // Check for fumble
```

## Testing

### Organization

43 test classes with 314 `@Nested` classes for hierarchical organization:

```
@DisplayName("Campaign")
class CampaignTest {
    @Nested @DisplayName("loadFromYaml")
    class LoadFromYamlTests { }

    @Nested @DisplayName("Cross-Reference Validation")
    class ValidationTests { }
}
```

### Patterns

- **No mocks**: Uses real objects and fixtures
- **`@TempDir`**: 8 test classes use temporary directories for YAML tests
- **Factory fixtures**: `Weapon.createLongsword()`, `Item.createConsumable()`
- **`EdgeCaseTest.java`**: Root-level boundary condition tests

### Running Tests

```bash
mvn test                                    # All tests
mvn test -Dtest=CampaignTest               # Single class
mvn test -Dtest=InventoryTest#canAddItem   # Single method
```

## Available Campaigns

| Campaign | Difficulty | DC Range | Theme |
|----------|------------|----------|-------|
| `muddlebrook` | Beginner | 10-14 | Comedic mystery, theatrical villain |
| `eberron` | Intermediate | 12-16 | Olympic competition, dragonshards |
| `drownedgod` | Advanced | 15-19 | Nautical horror |

### Campaign Design Philosophy

Trials follow a consistent structure across campaigns:
1. **Safe Hub Scene** — Tavern/shop/town square for regrouping
2. **Lead** — Rumor, witness, strange event, or villain taunt
3. **Trial Location** — Puzzle room with 3-6 mini-games
4. **Encounter** — Combat or hazard appropriate to theme
5. **Reward** — Magical item or boon
6. **Stinger** — Villain message or new location unlocked

Design principles:
- Every trial can be **won without violence**
- Every trial can be **failed without death** (unless reckless)
- Use **comedic consequences** over lethal outcomes early
- **Reward curiosity** and creative problem-solving

### Difficulty Scaling

| Factor | Adjustment |
|--------|------------|
| Boss stats | +2 AC / +15 HP per tier |
| Minions | +1-2 extra per additional player |
| DCs | 10-12 (early) → 13-15 (mid) → 16-18 (late) |
| Failures | Add conditions (slowed, blinded) instead of raw damage |

## Adding a New Campaign

1. Create `src/main/resources/campaigns/{campaign-id}/`
2. Add `campaign.yaml` (required):
   ```yaml
   id: my_campaign
   name: "My Campaign"
   starting_location: tavern
   intro: |
     Opening narrative shown at game start...
   author: Your Name
   version: "1.0"
   ```
3. Add optional YAML files: locations, npcs, monsters, items, trials, minigames
4. Campaign auto-appears in selection menu

## Dependencies

- **SnakeYAML 2.2** - YAML parsing
- **Jansi 2.4.1** - Terminal colors (ANSI)
- **JUnit 5.10.1** - Testing framework

## Key Design Decisions

- **No mocking in tests**: Tests use real objects and YAML fixtures with `@TempDir`
- **Immutable campaign data**: `Campaign` is read-only; all mutations go through `GameState`
- **Package-private loaders**: `CampaignLoader` hidden behind `Campaign` facade
- **Composition over inheritance**: `StatusEffectManager` tracks effects externally from `Combatant`
- **Factory methods for fixtures**: `Weapon.createLongsword()`, `Item.createConsumable()` for tests
