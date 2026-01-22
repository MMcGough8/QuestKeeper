# QuestKeeper High-Level Explanation

## Overview

QuestKeeper is a CLI-based D&D 5e adventure game written in Java 17. It features YAML-driven campaign data, turn-based combat with full D&D 5e mechanics, and support for multiple campaigns.

---

## Game Flow

```
Main.java
    ↓
GameEngine.start()
    ├─ Display title screen
    ├─ Load campaign (select if multiple exist)
    ├─ Character creation (or load saved game)
    ├─ Initialize GameState
    └─ Main game loop
        ├─ Display location
        ├─ Process player commands
        ├─ Handle combat/dialogue/trials
        └─ Repeat until quit
```

### Key Components

| Component | Purpose |
|-----------|---------|
| `GameEngine` | Main orchestrator - handles game loop, commands, state |
| `Campaign` | YAML-loaded campaign data (locations, NPCs, monsters, items) |
| `GameState` | Tracks player location, visited places, completed trials |
| `Character` | Player character with D&D 5e stats and abilities |
| `CombatSystem` | Turn-based combat orchestration |
| `DialogueSystem` | NPC conversation management |

---

## Architecture Diagram

```
GameEngine (orchestrator)
  ├─ Campaign (YAML-loaded data)
  │   ├─ Locations
  │   ├─ NPCs
  │   ├─ Monsters (templates)
  │   ├─ Items
  │   ├─ Trials
  │   └─ Mini-games
  │
  ├─ GameState (runtime state)
  │   ├─ Current location
  │   ├─ Visited locations
  │   ├─ Unlocked locations
  │   └─ Completed trials
  │
  ├─ Character (player)
  │   ├─ Ability scores (STR, DEX, CON, INT, WIS, CHA)
  │   ├─ HP, AC, Level
  │   ├─ Skills & proficiencies
  │   └─ Inventory & equipment
  │
  ├─ CombatSystem
  │   ├─ Initiative tracking
  │   ├─ Attack resolution
  │   ├─ Status effects
  │   └─ AI behavior
  │
  └─ DialogueSystem
      └─ NPC conversations
```

---

## Combat System

### The Combatant Interface

Both `Character` (player) and `Monster` implement the `Combatant` interface, allowing polymorphic combat:

```java
public interface Combatant {
    String getName();
    int getCurrentHitPoints();
    int getMaxHitPoints();
    int getArmorClass();
    int takeDamage(int amount);
    int heal(int amount);
    int getInitiativeModifier();
    int rollInitiative();
    boolean isAlive();
}
```

### Combat Flow

1. **Start Combat** - Roll initiative for all participants
2. **Turn Order** - Sort by initiative (highest first), DEX breaks ties
3. **Execute Turns** - Each combatant acts in order
4. **End Combat** - Victory (all enemies dead) or defeat (player dead)

---

## How Attacks Get Calculated

### Attack Resolution Formula

```
Attack Roll = d20 + Attack Modifier

For Player:
  Attack Modifier = STR modifier + Proficiency Bonus

For Monster:
  Attack Modifier = Monster's attack_bonus (fixed value from YAML)

Hit Condition: Attack Roll >= Target's AC
```

### Step-by-Step Attack Process

Located in `CombatSystem.processAttack()`:

```
1. VALIDATE
   - Check both combatants are alive
   - Validate target is in combat

2. CHECK STATUS EFFECTS
   - Advantage: Status effects grant advantage OR target has vulnerable condition
   - Disadvantage: Status effects impose disadvantage on attacker
   - Auto-Crit: Target is Paralyzed or Unconscious

3. ROLL ATTACK
   ┌─────────────────────────────────────────────────────────┐
   │ If advantage (and no disadvantage):                     │
   │   Roll 2d20, take HIGHER + modifier                     │
   │                                                         │
   │ If disadvantage (and no advantage):                     │
   │   Roll 2d20, take LOWER + modifier                      │
   │                                                         │
   │ Otherwise:                                              │
   │   Roll 1d20 + modifier                                  │
   └─────────────────────────────────────────────────────────┘

4. COMPARE VS AC
   - If attackRoll >= targetAC → HIT
   - If attackRoll < targetAC → MISS (return miss result)

5. CALCULATE DAMAGE (on hit)
   ┌─────────────────────────────────────────────────────────┐
   │ Player Damage:                                          │
   │   1d8 + STR modifier (minimum 1 damage)                 │
   │                                                         │
   │ Monster Damage:                                         │
   │   Rolls monster's damage_dice from YAML (e.g., "1d6+2") │
   │                                                         │
   │ If Auto-Crit:                                           │
   │   Damage × 2                                            │
   └─────────────────────────────────────────────────────────┘

6. APPLY DAMAGE
   - target.takeDamage(damage)
   - Track aggro (monster remembers who hit it)
   - Process special abilities (Disarm, Adhesive, etc.)

7. RETURN RESULT
   - CombatResult with all roll details for display
```

### Example Attack Calculation

```
Player (Level 3 Fighter) attacks Goblin

Player Stats:
  - STR: 16 (modifier: +3)
  - Proficiency Bonus: +2
  - Total Attack Modifier: +5

Goblin Stats:
  - AC: 15
  - HP: 14

Attack Roll:
  d20 = 12
  Total = 12 + 5 = 17

Compare: 17 >= 15? YES → HIT!

Damage Roll:
  1d8 = 6
  + STR modifier = 6 + 3 = 9 damage

Result: Goblin takes 9 damage (now at 5/14 HP)
```

### Modifier Calculations

**Ability Modifier** (D&D 5e standard):
```
modifier = (ability_score - 10) / 2

Examples:
  STR 16 → (16-10)/2 = +3
  DEX 14 → (14-10)/2 = +2
  CON 10 → (10-10)/2 = +0
  INT 8  → (8-10)/2  = -1
```

**Proficiency Bonus** (based on level):
```
proficiency = (level - 1) / 4 + 2

Level 1-4:  +2
Level 5-8:  +3
Level 9-12: +4
Level 13-16: +5
Level 17-20: +6
```

**Armor Class** (for players):
```
AC = 10 + DEX modifier + armor bonus + shield bonus
```

---

## Status Effects & Combat Modifiers

### Conditions That Grant Advantage to Attackers
- Restrained
- Paralyzed
- Stunned
- Prone
- Unconscious

### Conditions That Impose Disadvantage on Attacks
- Blinded
- Frightened
- Poisoned
- Prone
- Restrained

### Conditions That Auto-Crit on Melee Hits
- Paralyzed
- Unconscious

---

## Monster AI Behavior

Monsters have behavior patterns that affect combat decisions:

| Behavior | Description |
|----------|-------------|
| `AGGRESSIVE` | Always attacks, never flees |
| `COWARDLY` | Flees when bloodied (≤50% HP) |
| `TACTICAL` | Uses special abilities, targets strategically |
| `DEFENSIVE` | May flee when below 25% HP |

**Target Selection Priority:**
1. Last attacker (aggro system)
2. Behavior-based selection (tactical may target lowest HP)
3. Default to player

---

## Key Files

| File | Location | Purpose |
|------|----------|---------|
| `CombatSystem.java` | `combat/` | Attack resolution, turn management |
| `Combatant.java` | `combat/` | Interface for combat participants |
| `Character.java` | `character/` | Player character implementation |
| `Monster.java` | `combat/` | Enemy creature implementation |
| `Dice.java` | `core/` | All dice rolling logic |
| `GameEngine.java` | `core/` | Main game loop and commands |

---

## Campaign Data (YAML-Driven)

All game content is loaded from YAML files in `src/main/resources/campaigns/{name}/`:

- `campaign.yaml` - Metadata, starting location, intro
- `locations.yaml` - Maps, exits, NPCs present
- `npcs.yaml` - Dialogue trees, personalities
- `monsters.yaml` - Stats, abilities, behavior
- `items.yaml` - Weapons, armor, consumables
- `trials.yaml` - Puzzle rooms
- `minigames.yaml` - Skill challenges

---

## Available Campaigns

| Campaign | Difficulty | Theme |
|----------|------------|-------|
| Muddlebrook | Beginner (DC 10-14) | Comedic mystery, Scooby-Doo meets Monty Python |
| Eberron | Intermediate (DC 12-16) | Olympic competition with cosmic stakes |
| Drowned God | Advanced (DC 15-19) | Gothic horror, Lovecraftian nautical |
