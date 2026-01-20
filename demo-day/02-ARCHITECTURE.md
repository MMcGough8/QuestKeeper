# QuestKeeper Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              QUESTKEEPER                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │    USER     │    │   DISPLAY   │    │ GAME ENGINE │    │  CAMPAIGN   │  │
│  │  INTERFACE  │───▶│   (UI/UX)   │◀──▶│   (CORE)    │◀──▶│   (DATA)    │  │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘  │
│         │                                     │                   │         │
│         │                                     ▼                   │         │
│         │                            ┌─────────────┐              │         │
│         │                            │  GAME STATE │              │         │
│         │                            │  (RUNTIME)  │              │         │
│         │                            └─────────────┘              │         │
│         │                                     │                   │         │
│         │                                     ▼                   │         │
│         │          ┌──────────────────────────────────────────┐   │         │
│         │          │              SUBSYSTEMS                  │   │         │
│         │          ├──────────┬──────────┬──────────┬────────┤   │         │
│         │          │ COMBAT   │ DIALOGUE │INVENTORY │  SAVE  │   │         │
│         │          │ SYSTEM   │  SYSTEM  │  SYSTEM  │ SYSTEM │   │         │
│         │          └──────────┴──────────┴──────────┴────────┘   │         │
│         │                                                         │         │
│         └─────────────────────────────────────────────────────────┘         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           YAML DATA LAYER                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   campaigns/                                                                │
│   ├── muddlebrook/          ├── eberron/            ├── drownedgod/        │
│   │   ├── campaign.yaml     │   ├── campaign.yaml   │   ├── campaign.yaml  │
│   │   ├── locations.yaml    │   ├── locations.yaml  │   ├── locations.yaml │
│   │   ├── npcs.yaml         │   ├── npcs.yaml       │   ├── npcs.yaml      │
│   │   ├── monsters.yaml     │   ├── monsters.yaml   │   ├── monsters.yaml  │
│   │   ├── items.yaml        │   ├── items.yaml      │   ├── items.yaml     │
│   │   ├── trials.yaml       │   ├── trials.yaml     │   ├── trials.yaml    │
│   │   └── minigames.yaml    │   └── minigames.yaml  │   └── minigames.yaml │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.questkeeper/
├── core/                 # Game Engine, Command Parser, Dice
│   ├── GameEngine        # Main game loop, orchestration
│   ├── CommandParser     # Input parsing
│   └── Dice              # D&D dice rolling
│
├── character/            # Player & NPC
│   ├── Character         # Player with stats, skills, inventory
│   └── NPC               # Non-player characters with dialogue
│
├── combat/               # Combat System
│   ├── CombatSystem      # Turn-based D&D combat
│   ├── Monster           # Enemy definitions
│   ├── Combatant         # Interface for combat participants
│   └── status/           # Status effects (poisoned, stunned, etc.)
│
├── inventory/            # Item System
│   ├── Inventory         # Storage, equipment slots, weight
│   ├── items/            # Item hierarchy (Weapon, Armor, etc.)
│   └── items/effects/    # Item effect system
│
├── campaign/             # Campaign Data
│   ├── Campaign          # Facade for campaign data
│   ├── CampaignLoader    # YAML parsing (internal)
│   ├── Trial             # Puzzle room definitions
│   └── MiniGame          # Skill check challenges
│
├── dialogue/             # Conversation System
│   ├── DialogueSystem    # NPC conversation management
│   └── DialogueResult    # Conversation outcomes
│
├── world/                # Location System
│   └── Location          # Places with exits, NPCs, items
│
├── state/                # Runtime State
│   └── GameState         # Current game state tracking
│
├── save/                 # Persistence
│   ├── SaveState         # Serializable game state
│   └── CharacterData     # Character serialization
│
└── ui/                   # Display
    ├── Display           # All terminal output
    └── CharacterCreator  # Character creation wizard
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Facade** | `Campaign` class | Clean public API hiding YAML loading complexity |
| **Template Method** | `AbstractItemEffect` | Define effect flow, subclasses implement specifics |
| **Strategy** | `ItemEffect` interface | Swappable effect implementations |
| **Factory** | `Campaign.loadFromYaml()` | Create campaigns from YAML directories |
| **Command** | `CommandParser` | Parse and execute user commands |
| **State** | `GameState` | Track runtime state changes |

---

## Data Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│   YAML   │────▶│ Campaign │────▶│GameState │────▶│ Display  │
│  Files   │     │  Loader  │     │          │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                      │                 ▲
                      │                 │
                      ▼                 │
                ┌──────────┐      ┌──────────┐
                │Validation│      │  User    │
                │  Errors  │      │  Input   │
                └──────────┘      └──────────┘
```

1. **Load**: YAML files parsed into Campaign objects
2. **Validate**: Cross-references checked (NPCs exist, locations connect)
3. **Initialize**: GameState created with starting location
4. **Loop**: User input → Command → State change → Display update

---

## Key Technical Decisions

### Why Terminal-Based?
- Runs anywhere (laptop, server, phone via SSH)
- No graphics dependencies
- Imagination fills in the visuals
- Portable and lightweight

### Why YAML for Content?
- Human-readable, easy to edit
- No code required for new campaigns
- Version control friendly
- Enables community content creation

### Why Java 17?
- Modern language features (records, pattern matching)
- Strong typing catches errors at compile time
- Excellent test tooling (JUnit 5)
- Cross-platform compatibility

---

## Test Coverage

```
┌─────────────────────────────────────────┐
│           1,917 TESTS                   │
├─────────────────────────────────────────┤
│                                         │
│  Unit Tests         ████████████  85%   │
│  Integration Tests  ████████░░░░  65%   │
│  Campaign Validation████████████  100%  │
│                                         │
│  Key Test Classes:                      │
│  • CampaignTest (YAML loading)          │
│  • CombatSystemTest (D&D mechanics)     │
│  • InventoryTest (equipment/weight)     │
│  • DialogueSystemTest (conversations)   │
│  • DisplayTest (UI rendering)           │
│  • GameStateTest (state management)     │
│                                         │
└─────────────────────────────────────────┘
```

---

## Scalability Path

```
CURRENT                    FUTURE
───────                    ──────
Local CLI          ──▶     Web version (same engine)
3 campaigns        ──▶     Community marketplace
Single player      ──▶     Shared campaigns/saves
Manual install     ──▶     Package managers (brew, apt)
```
