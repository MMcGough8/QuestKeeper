# QuestKeeper: Auto-DM

A CLI-based D&D 5e adventure game that automates the Dungeon Master role, bringing tabletop roleplaying to life through intelligent narrative generation and rules management.

## Overview

QuestKeeper is a Java-based application designed to serve as an automated Dungeon Master for D&D 5th Edition gameplay. The project emphasizes beginner-friendly mechanics while maintaining compatibility with core 5e rules, featuring natural language command parsing for enhanced player interaction.

## Features

- **Automated DM System** — Handles narrative progression, NPC interactions, combat encounters, and puzzle resolution
- **Character Creation** — Full D&D 5e character generation with 9 races, 12 classes, and 18 skills
- **Turn-Based Combat** — Initiative tracking, status effects, monster AI behaviors, and opportunity attacks
- **Trial System** — Multi-challenge puzzle encounters with skill checks and rewards
- **Dialogue System** — Flag-gated NPC conversations with branching topics
- **State Management** — Persistent game state with YAML-based save/load
- **Campaign Support** — Data-driven campaigns defined entirely in YAML

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Installation
```bash
git clone https://github.com/yourusername/questkeeper.git
cd questkeeper
mvn clean install
```

### Running the Game
```bash
# Interactive mode
mvn exec:java -Dexec.mainClass="com.questkeeper.Main" -q

# Demo mode (automated playthrough)
mvn exec:java -Dexec.mainClass="com.questkeeper.demo.AutoDemo" -q

# Quick build (skip tests)
mvn clean install -P quick
```

## Included Campaigns

### Muddlebrook: Harlequin Trials (Beginner)

A comedic, chaotic mystery campaign featuring puzzle rooms, mini-games, and a theatrical mastermind villain.

**Tone:** Scooby-Doo mystery meets Monty Python absurdity with dark theatrical traps (cartoon menace, never gory)

**Core Loop:**
1. Safe Hub Scene (tavern/shop/town square)
2. Lead (rumor, witness, strange event, villain taunt)
3. Trial Location (puzzle room with 3–6 mini-games)
4. Encounter (clockwork minions, time-ripple hazards)
5. Reward (magical item/boon)
6. Stinger (Machinist message, new location unlocked)

**Key Locations:** Drunken Dragon Inn, Town Hall, Clocktower Hill, Old Market Row, Whisperwood Edge

**Notable NPCs:** Norrin the Bard, Mara Ember, Darius, Elara, The Harlequin Machinist

---

### Eberron: Shards of the Fallen Sky (Intermediate)

A myth-heavy, puzzle-forward campaign exploring cosmic origins and ancient powers.

**Core Theme:** *The deeper you go, the darker it gets.*

**Campaign Start:** The Olympic Convergence Games — a competition that secretly scouts for heroes with dormant potential and shard resonance.

**Dragonshards (Reward System):**
| Shard | Passive Bonus | Active Ability |
|-------|---------------|----------------|
| Might | +1 Strength | +1d6 damage (1/long rest) |
| Insight | +1 Investigation/Perception | Ask one truthful hint (1/long rest) |
| Speed | +5 ft movement | Dash as bonus action (1/long rest) |
| Resolve | +1 to saving throws | Turn failed save to success (1/day) |

---

### Drowned God (Advanced)

Gothic nautical horror with multiple endings.

---

## Technical Architecture

### Tech Stack
- **Language:** Java 17
- **Build Tool:** Maven
- **Data Format:** SnakeYAML 2.2
- **Terminal UI:** Jansi 2.4.1 (ANSI colors)
- **Testing:** JUnit 5.10.1

### Project Structure
```
questkeeper/
├── src/main/java/com/questkeeper/
│   ├── core/              # GameEngine, CommandParser, Dice, RestSystem
│   │   └── command/       # Command handlers (7 handlers + router)
│   ├── campaign/          # Campaign, CampaignLoader, Trial, MiniGame
│   ├── character/         # Character (D&D 5e stats), NPC
│   ├── combat/            # CombatSystem, Monster, Combatant
│   │   └── status/        # 14 D&D 5e conditions, StatusEffectManager
│   ├── inventory/         # Item, Weapon, Armor, MagicItem, Inventory
│   │   └── items/effects/ # 12 item effect types (Template Method pattern)
│   ├── dialogue/          # DialogueSystem, DialogueResult
│   ├── state/             # GameState (flags, counters, variables)
│   ├── world/             # Location
│   ├── save/              # SaveState, CharacterData
│   └── ui/                # Display, CharacterCreator
├── src/main/resources/
│   └── campaigns/         # YAML campaign definitions
│       ├── muddlebrook/
│       ├── eberron/
│       └── drownedgod/
└── src/test/java/         # 43 test classes, 314 nested test classes
```

### Core Data Flow
```
YAML Files (campaigns/) → Campaign.loadFromYaml() → Campaign (immutable)
                                                          ↓
                              GameEngine creates GameState (mutable runtime)
                                                          ↓
                       Game Loop: input → CommandParser → CommandRouter → Handler → Display
```

## AI DM Guidelines

The Auto-DM system follows these principles:

1. **Always ask for rolls** on uncertain actions
2. **Keep scenes cinematic**, not mechanical dumps
3. **Use comedic consequences** over lethal outcomes early
4. **Reward curiosity** and creative problem-solving
5. Every trial can be **won without violence**
6. Every trial can be **failed without death** (unless reckless)

## Adding a New Campaign

1. Create `src/main/resources/campaigns/{campaign-id}/`
2. Add required `campaign.yaml`:
   ```yaml
   id: my_campaign
   name: "My Campaign"
   starting_location: tavern
   intro: |
     Opening narrative...
   author: Your Name
   version: "1.0"
   ```
3. Add optional files: `locations.yaml`, `npcs.yaml`, `monsters.yaml`, `items.yaml`, `trials.yaml`, `minigames.yaml`
4. Campaign auto-appears in selection menu

## Roadmap

### Completed
- [x] Character creation with full D&D 5e mechanics
- [x] Turn-based combat with initiative and status effects
- [x] Trial/puzzle system with skill checks
- [x] NPC dialogue with flag-gated topics
- [x] Save/load persistence
- [x] Natural language command parsing (100+ synonyms)
- [x] Command handler architecture
- [x] Three campaigns (Muddlebrook, Eberron, Drowned God)

### Planned
- [ ] Class features (Fighter abilities, Rogue sneak attack)
- [ ] Magic/spell system
- [ ] Death saving throws
- [ ] More combat options (dodge, help, ready)
- [ ] Quarkus web interface migration

## License

MIT License - see [LICENSE](LICENSE) for details.
