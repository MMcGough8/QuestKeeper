# QuestKeeper - Project Specification Document

## Project Overview

**Project Name:** QuestKeeper  
**Version:** 1.0 (MVP)  
**Type:** Command-Line Interface (CLI) Text Adventure Game  
**Genre:** Dungeons & Dragons 5e Adventure Game  
**Primary Campaign:** Muddlebrook: Harlequin Trials  
**Author:** Marc McGough  
**Campaign Content:** Andrew Mariani  
**Target Platform:** Cross-platform (Windows, macOS, Linux)  
**Programming Language:** Java 17+  
**Build Tool:** Maven  
**Development Approach:** Test-Driven Development with Mini Sprints

---

## Executive Summary

QuestKeeper is a command-line D&D adventure game that automates the Dungeon Master role. Players create characters using D&D 5e rules, explore the town of Muddlebrook through text-based navigation, solve puzzle-based Trials, engage in turn-based combat, and progress through a theatrical mystery campaign featuring the Harlequin Machinist villain. The game handles all mechanics (dice rolls, ability checks, combat resolution, inventory management) while players make narrative choices through natural language commands.

---

## Project Goals

### Primary Objectives
1. **Requirement Fulfillment**
   - Demonstrate object-oriented programming mastery
   - Implement character abstraction and display classes
   - Create functioning game loop with state management
   - Include random encounters with dice mechanics
   - Implement data persistence (save/load)
   - Apply test-driven development methodology

2. **Gameplay Experience**
   - Make D&D accessible to solo players
   - Provide guided character creation
   - Deliver engaging story-driven gameplay
   - Teach D&D mechanics naturally through play
   - Create replayable content with meaningful choices

3. **Technical Achievement**
   - Build scalable, maintainable codebase
   - Design extensible data model for future campaigns
   - Implement natural language command parser
   - Create modular content system (easy to add Trials/NPCs/Items)

---

## Target Audience

### Primary Users
- **New D&D Players**: People interested in D&D but intimidated by complexity
- **Solo Players**: D&D fans without access to a group or DM
- **Reviewers**: Evaluating technical implementation

### User Personas

**Persona 1: "Curious Beginner"**
- Experience: Heard of D&D, never played
- Needs: Tutorial-style guidance, low barrier to entry
- Goals: Learn the basics, have fun without pressure

**Persona 2: "Solo Adventurer"**
- Experience: Has played D&D, can't find regular group
- Needs: Quality story content, solo-friendly mechanics
- Goals: Scratch the D&D itch between sessions

**Persona 3: "Evaluator"**
- Role: Instructor/Reviewer
- Needs: Clear code structure, proper documentation, working features
- Goals: Assess technical skills and design decisions

---

## Functional Requirements

### FR-1: Character Creation System
**Priority:** Critical  
**Description:** Guided 10-step process for creating a D&D 5e character

**Sub-Requirements:**
- FR-1.1: Player enters character name (string validation)
- FR-1.2: Player selects race from list (Human, Elf, Dwarf, Halfling, etc.)
- FR-1.3: Player selects class from list (Fighter, Rogue, Wizard, Cleric, etc.)
- FR-1.4: System generates or allows manual ability score assignment (STR, DEX, CON, INT, WIS, CHA)
- FR-1.5: Player selects skill proficiencies based on class
- FR-1.6: System calculates derived stats (HP, AC, ability modifiers)
- FR-1.7: Player receives starting equipment based on class
- FR-1.8: System displays character sheet for confirmation
- FR-1.9: Character data saved to file
- FR-1.10: System loads player into starting location (Drunken Dragon Inn)

**Acceptance Criteria:**
- Character creation completes in under 5 minutes
- All D&D 5e rules correctly applied
- Character data persists across sessions
- Invalid inputs handled gracefully with helpful prompts

---

### FR-2: Location Navigation System
**Priority:** Critical  
**Description:** Text-based exploration of Muddlebrook locations

**Sub-Requirements:**
- FR-2.1: Display current location name and description on entry
- FR-2.2: List available exits (north, south, east, west, up, down)
- FR-2.3: List NPCs present at location
- FR-2.4: List items on ground at location
- FR-2.5: Process "go [direction]" commands to move between locations
- FR-2.6: Process "look" command to redisplay location details
- FR-2.7: Update game state with visited locations
- FR-2.8: Handle invalid movement attempts with appropriate feedback

**Acceptance Criteria:**
- Player can navigate all 6 Muddlebrook districts
- Location descriptions display atmospheric text
- Connected locations properly linked
- Movement commands execute instantly

**Muddlebrook Locations (MVP):**
1. Drunken Dragon Inn (hub/starting point)
2. Town Hall (Trial #1 location)
3. Clocktower Hill (Trial #2 location)
4. Old Market Row (shopping/side quests)
5. Fogline Docks (optional exploration)
6. Whisperwood Edge (optional exploration)

---

### FR-3: NPC Interaction System
**Priority:** Critical  
**Description:** Dialogue system for interacting with non-player characters

**Sub-Requirements:**
- FR-3.1: Display NPC name and greeting on first interaction
- FR-3.2: Process "talk to [NPC]" commands
- FR-3.3: Process "ask [NPC] about [topic]" commands
- FR-3.4: Display dialogue responses based on topic
- FR-3.5: Check and set conversation flags
- FR-3.6: Grant items/quests when dialogue triggers require
- FR-3.7: Track which NPCs player has met
- FR-3.8: Update dialogue options based on story progress

**Acceptance Criteria:**
- All 4 core NPCs fully implemented (Norrin, Mara, Darius, Elara)
- Dialogue trees branch based on flags
- Conversations feel natural and informative
- Key story information conveyed through dialogue

**Core NPCs (MVP):**
1. **Norrin the Bard** - Rumors, foreshadowing, comedic relief
2. **Mara Ember** - Bartender, practical info, quest pointer
3. **Darius** - Dark recluse, warnings, mentor figure
4. **Elara** - Shopkeeper, items, back-room secrets

---

### FR-4: Trial & MiniGame System
**Priority:** Critical  
**Description:** Structured puzzle sequences that reward cleverness

**Sub-Requirements:**
- FR-4.1: Display Trial entry narrative when entering Trial location
- FR-4.2: Present MiniGame description without spoiling solution
- FR-4.3: Accept player commands to interact with puzzle
- FR-4.4: Determine required ability check (skill + DC)
- FR-4.5: Roll dice (d20 + modifier) for skill checks
- FR-4.6: Display roll results transparently
- FR-4.7: On success: grant magical item reward, display success text
- FR-4.8: On failure: apply consequence (damage, effect), display failure text
- FR-4.9: Set completion flags for each MiniGame
- FR-4.10: Track Trial completion status

**Acceptance Criteria:**
- Trial #1 (Mayor's Office) fully playable with 6 MiniGames
- Each MiniGame has clear success/failure outcomes
- Dice rolls visible to player (no hidden mechanics)
- Rewards match campaign documentation
- Multiple solution approaches accepted where logical

**Trial #1 MiniGames (MVP):**
1. Backwards Clock (Sleight of Hand/Investigation, DC 12)
2. Puzzle Box (Intelligence/Arcana, DC 13)
3. Stacked Bookshelf (Dexterity + Wisdom choice, DC 11)
4. Mechanical Frog Chase (Dexterity/Animal Handling, DC 12)
5. Three Locks (varies by approach)
6. Carpet Tile Puzzle (Dexterity/Investigation, DC 10)

---

### FR-5: Combat Encounter System
**Priority:** High  
**Description:** Turn-based combat using D&D 5e rules

**Sub-Requirements:**
- FR-5.1: Roll initiative for all combatants
- FR-5.2: Display turn order
- FR-5.3: On player turn, accept combat commands (attack, cast spell, use item, flee)
- FR-5.4: Calculate attack rolls (d20 + modifiers)
- FR-5.5: Calculate damage rolls
- FR-5.6: Apply damage to HP
- FR-5.7: Execute enemy AI turns
- FR-5.8: Check for combat end conditions (all enemies defeated, player defeated, fled)
- FR-5.9: Grant experience and loot on victory
- FR-5.10: Handle death/defeat appropriately

**Acceptance Criteria:**
- Combat follows D&D 5e action economy
- Attack rolls and damage clearly displayed
- Enemy AI makes tactical decisions
- Combat balancing appropriate for level 1-2 characters
- Death results in respawn at last safe location (with consequences)

**Enemies (MVP):**
1. Clockwork Critters (CR 1/4) - Tutorial combat
2. Confetti Ooze (CR 1/2) - Silly but challenging
3. Time Ripple Hazard (environmental) - Teaches tactical thinking

---

### FR-6: Inventory System
**Priority:** High  
**Description:** Item collection, management, and usage

**Sub-Requirements:**
- FR-6.1: Display inventory command shows all items
- FR-6.2: Process "take [item]" to pick up items from ground
- FR-6.3: Process "drop [item]" to place items on ground
- FR-6.4: Process "use [item]" to activate item effects
- FR-6.5: Track item quantities for stackable items
- FR-6.6: Display item descriptions when examined
- FR-6.7: Apply item effects (passive bonuses, active abilities)
- FR-6.8: Track usage limits (1/day, 1/long rest)
- FR-6.9: Handle item weight/encumbrance (optional for MVP)

**Acceptance Criteria:**
- All Trial rewards properly granted to inventory
- Item effects function as documented
- Inventory persists across save/load
- Items have clear descriptions and usage instructions

**Key Items (MVP):**
- Blinkstep Spark (teleport 10ft, 1/long rest)
- Jester's Lucky Coin (+1d4 bonus, 1/day)
- Featherfall Bookmark (cast Feather Fall, 1/day)
- Hopper's Jump Band (jump boost, 1/long rest)
- Health Potions (restore HP)

---

### FR-7: Save/Load System
**Priority:** Critical  
**Description:** Persistent game state across sessions

**Sub-Requirements:**
- FR-7.1: Auto-save game state after significant events
- FR-7.2: Manual save command available
- FR-7.3: Save file includes character data (stats, inventory, location)
- FR-7.4: Save file includes game state (flags, completed trials, active quests)
- FR-7.5: Load game from save file on startup
- FR-7.6: Multiple save slots supported
- FR-7.7: Display last save timestamp
- FR-7.8: Handle corrupted save files gracefully

**Acceptance Criteria:**
- Game state fully restored from save file
- No data loss between sessions
- Save files human-readable (YAML format)
- Load time under 2 seconds

**Save File Format:**
```yaml
character:
  name: "Shadows"
  race: "Half-Elf"
  class: "Rogue"
  level: 2
  stats: {...}
  inventory: [...]

game_state:
  current_location: "drunken_dragon"
  flags: ["met_norrin", "completed_trial_01", ...]
  completed_trials: ["trial_01"]
  discovered_locations: [...]
```

---

### FR-8: Command Parser
**Priority:** Critical  
**Description:** Natural language command interpretation

**Sub-Requirements:**
- FR-8.1: Accept text input from player
- FR-8.2: Parse commands into verb-noun pairs
- FR-8.3: Handle command variations (synonyms)
- FR-8.4: Provide "help" command with available actions
- FR-8.5: Suggest commands when input not recognized
- FR-8.6: Handle multi-word targets ("talk to norrin")
- FR-8.7: Case-insensitive parsing
- FR-8.8: Graceful error messages for invalid commands

**Acceptance Criteria:**
- Common command variations work (e.g., "go north" = "n" = "north")
- Parser understands context (nearby NPCs/items)
- Helpful feedback when commands fail
- Response time instant (<100ms)

**Core Commands:**
- **Movement:** go [direction], north, south, east, west, n, s, e, w
- **Observation:** look, look at [target], examine [target], inspect [target]
- **Interaction:** talk to [NPC], ask [NPC] about [topic]
- **Items:** take [item], drop [item], use [item], inventory, inv, i
- **Combat:** attack [target], cast [spell], use [item], flee
- **Meta:** help, save, load, quit, stats, status

---

### FR-9: Dice Rolling System
**Priority:** High  
**Description:** D&D dice mechanics with transparent rolls

**Sub-Requirements:**
- FR-9.1: Roll any die type (d4, d6, d8, d10, d12, d20, d100)
- FR-9.2: Support multiple dice (2d6, 3d8)
- FR-9.3: Apply modifiers to rolls
- FR-9.4: Display roll formula and result
- FR-9.5: Highlight critical successes (natural 20)
- FR-9.6: Highlight critical failures (natural 1)
- FR-9.7: Handle advantage/disadvantage
- FR-9.8: Track roll history for debugging

**Acceptance Criteria:**
- All rolls use true random number generation
- Roll results displayed clearly
- Modifiers correctly applied
- Critical hits/misses handled per D&D rules

**Example Output:**
```
[ROLL] Sleight of Hand Check: d20: 15 + 3 = 18 vs DC 12 - SUCCESS!
[ROLL] Damage: 2d6 + 2 = 9 piercing damage
[ROLL] Attack Roll: d20: 1 (CRITICAL MISS!)
```

---

### FR-10: Quest System
**Priority:** Medium  
**Description:** Track player objectives and progress

**Sub-Requirements:**
- FR-10.1: Display active quests command
- FR-10.2: Show quest objectives and completion status
- FR-10.3: Update quest progress based on flags
- FR-10.4: Grant rewards on quest completion
- FR-10.5: Display quest completion notification
- FR-10.6: Track main quest vs side quests

**Acceptance Criteria:**
- Main quest "The Harlequin Trials" fully implemented
- Quest objectives update dynamically
- Quest log accessible at any time
- Rewards granted automatically on completion

**Main Quest (MVP):**
- **The Harlequin Trials**
  - Objective 1: Investigate the missing mayor
  - Objective 2: Complete Trial #1 (Mayor's Office)
  - Objective 3: Discover the Harlequin's identity
  - Objective 4: Find clues at the Clocktower

---

## Non-Functional Requirements

### NFR-1: Performance
- Game startup: < 3 seconds
- Command response time: < 200ms
- Save file size: < 1MB
- Memory usage: < 100MB

### NFR-2: Usability
- Clear, beginner-friendly prompts
- Consistent command syntax
- Helpful error messages
- Tutorial tips during first session

### NFR-3: Reliability
- No crashes during normal gameplay
- Save files never corrupted
- Graceful handling of all invalid inputs
- 100% test coverage on core systems

### NFR-4: Maintainability
- Code follows Java naming conventions and style guidelines
- Comprehensive Javadoc documentation
- Modular architecture (easy to add content)
- Unit tests for all game systems using JUnit 5

### NFR-5: Portability
- Cross-platform compatible (Windows, macOS, Linux)
- Java 17+ required (platform-independent JVM)
- Maven build system for dependency management
- Installation via `mvn clean install`
- Executable JAR for easy distribution

### NFR-6: Scalability
- Easy to add new campaigns
- Easy to add new Trials/MiniGames
- Easy to add new items/NPCs
- Data-driven content (no code changes needed)

---

## Technical Architecture

### System Components

```
┌─────────────────────────────────────────────────────────┐
│                    QuestKeeper CLI                      │
│                   (Main Game Loop)                      │
└────────────┬────────────────────────────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
┌───▼────┐    ┌──────▼──────┐
│ Input  │    │   Output    │
│ Parser │    │   Display   │
└───┬────┘    └──────▲──────┘
    │                │
    │      ┌─────────┴─────────┐
    │      │                   │
┌───▼──────▼──────┐   ┌───────┴────────┐
│  Game Engine    │   │   Game State   │
│  (Logic Layer)  │◄──┤   (Data Layer) │
└─────┬───────────┘   └────────────────┘
      │
      │
┌─────▼─────────────────────────────────────┐
│         Game Systems                      │
├───────────────────────────────────────────┤
│ • Character Management                    │
│ • Location Navigation                     │
│ • NPC Interaction                         │
│ • Trial/MiniGame Engine                   │
│ • Combat Resolution                       │
│ • Inventory Management                    │
│ • Quest Tracking                          │
│ • Dice Rolling                            │
│ • Save/Load                               │
└───────────────┬───────────────────────────┘
                │
    ┌───────────┴───────────┐
    │                       │
┌───▼──────┐       ┌────────▼────────┐
│  YAML    │       │   Save Files    │
│  Data    │       │   (Character)   │
│  Files   │       │                 │
└──────────┘       └─────────────────┘
```

### File Structure

```
questkeeper/
├── README.md
├── pom.xml                     # Maven configuration
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── questkeeper/
│   │   │           ├── Main.java                  # Entry point
│   │   │           │
│   │   │           ├── core/
│   │   │           │   ├── Game.java             # Main game controller
│   │   │           │   ├── CommandParser.java    # Command parser
│   │   │           │   ├── Display.java          # Output formatting
│   │   │           │   └── Dice.java             # Dice rolling utilities
│   │   │           │
│   │   │           ├── character/
│   │   │           │   ├── Character.java        # Character class
│   │   │           │   ├── Abilities.java        # Ability scores & modifiers
│   │   │           │   └── Inventory.java        # Inventory management
│   │   │           │
│   │   │           ├── world/
│   │   │           │   ├── Location.java         # Location class
│   │   │           │   ├── NPC.java              # NPC class
│   │   │           │   └── Scene.java            # Scene triggers
│   │   │           │
│   │   │           ├── gameplay/
│   │   │           │   ├── Trial.java            # Trial class
│   │   │           │   ├── MiniGame.java         # MiniGame class
│   │   │           │   ├── Combat.java           # Combat system
│   │   │           │   └── Quest.java            # Quest tracking
│   │   │           │
│   │   │           ├── items/
│   │   │           │   ├── Item.java             # Item base class
│   │   │           │   └── ItemEffect.java       # Item effect handlers
│   │   │           │
│   │   │           └── state/
│   │   │               ├── GameState.java        # Game state management
│   │   │               └── SaveLoadManager.java  # Save/load handlers
│   │   │
│   │   └── resources/
│   │       └── data/
│   │           ├── campaigns/
│   │           │   └── muddlebrook/
│   │           │       ├── campaign.yaml
│   │           │       ├── locations.yaml
│   │           │       ├── npcs.yaml
│   │           │       ├── trials.yaml
│   │           │       ├── items.yaml
│   │           │       └── encounters.yaml
│   │           │
│   │           └── templates/
│   │               ├── races.yaml
│   │               └── classes.yaml
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── questkeeper/
│                   ├── CharacterTest.java
│                   ├── CommandParserTest.java
│                   ├── CombatTest.java
│                   ├── MiniGameTest.java
│                   └── SaveLoadTest.java
│
├── saves/
│   └── .gitkeep
│
└── docs/
    ├── data_model.md
    ├── command_reference.md
    ├── gameplay_guide.md
    └── developer_guide.md
```

---

## Development Phases

### Phase 1: Foundation (Week 1-2)
**Goal:** Core systems functional

**Deliverables:**
- [ ] Project structure setup
- [ ] Character class with D&D stats
- [ ] Command parser (basic commands)
- [ ] Location navigation system
- [ ] Dice rolling utilities
- [ ] Display/output formatting
- [ ] Unit tests for core systems

**Acceptance:** Can create character, move between rooms, roll dice

---

### Phase 2: Content Integration (Week 3-4)
**Goal:** Muddlebrook campaign playable

**Deliverables:**
- [ ] YAML data files for Muddlebrook
- [ ] NPC dialogue system
- [ ] Item system with magical effects
- [ ] Trial/MiniGame engine
- [ ] Save/load functionality
- [ ] Game state management

**Acceptance:** Can complete Trial #1 end-to-end

---

### Phase 3: Combat & Polish (Week 5-6)
**Goal:** Full gameplay loop

**Deliverables:**
- [ ] Turn-based combat system
- [ ] Enemy AI
- [ ] Quest tracking
- [ ] Tutorial tips
- [ ] Error handling polish
- [ ] Help system
- [ ] Full test coverage

**Acceptance:** Can play from character creation through Trial #1 and encounter

---

### Phase 4: Testing & Documentation (Week 7-8)
**Goal:** Production-ready

**Deliverables:**
- [ ] Full playthrough testing
- [ ] Bug fixes
- [ ] Performance optimization
- [ ] Documentation (README, guides)
- [ ] Code cleanup and refactoring
- [ ] Demo preparation

**Acceptance:** Stable, documented, ready for presentation

---

## Testing Strategy

### Unit Tests
- All game systems have 100% coverage
- Character stat calculations
- Dice roll mechanics
- Command parsing
- Item effects
- Combat resolution
- JUnit 5 test framework with assertions
- Maven Surefire plugin for test execution

### Integration Tests
- Character creation end-to-end
- Trial completion flow
- Save/load round-trip
- Combat encounter flow
- NPC interaction sequence

### Playthrough Tests
- Complete game from start to Trial #1 completion
- Test all command variations
- Verify all MiniGames functional
- Confirm all rewards granted
- Test save/load at various points

### Edge Case Tests
- Invalid command inputs
- Corrupted save files
- Impossible ability check DCs
- Combat with 0 HP
- Inventory overflow

---

## Success Metrics

### Technical Metrics
- [ ] 100% unit test coverage on core systems
- [ ] 0 critical bugs at launch
- [ ] < 200ms average command response time
- [ ] Successful save/load 100% of the time

### Gameplay Metrics
- [ ] Character creation completes in < 5 minutes
- [ ] Trial #1 playable end-to-end
- [ ] All 6 MiniGames functional
- [ ] All 4 NPCs have working dialogue
- [ ] Combat system follows D&D 5e rules

### Quality Metrics
- [ ] Code follows PEP 8 style guide
- [ ] All functions have docstrings
- [ ] README with clear setup instructions
- [ ] Data model documentation complete
- [ ] At least 3 successful playthroughs by testers

---

## Risk Assessment

### Technical Risks

**Risk:** Command parser too rigid, doesn't understand variations  
**Mitigation:** Build synonym mapping, implement fuzzy matching for common typos  
**Probability:** Medium | **Impact:** High

**Risk:** Save file corruption causing data loss  
**Mitigation:** Validate save files on load, create backups automatically  
**Probability:** Low | **Impact:** High

**Risk:** Combat system too complex to implement in timeframe  
**Mitigation:** Start with simplified combat, expand later if time permits  
**Probability:** Medium | **Impact:** Medium

**Risk:** Dice roll mechanics produce unfair results  
**Mitigation:** Extensive testing, balance tweaking, difficulty curve analysis  
**Probability:** Low | **Impact:** Medium

### Content Risks

**Risk:** Muddlebrook campaign too large to implement fully  
**Mitigation:** Focus on Trial #1 for MVP, Trial #2+ as stretch goals  
**Probability:** High | **Impact:** Low

**Risk:** NPC dialogue feels robotic or unnatural  
**Mitigation:** Use Andrew's voice lines directly, add personality to responses  
**Probability:** Medium | **Impact:** Medium

### Timeline Risks

**Risk:** Scope creep - trying to implement too many features  
**Mitigation:** Strict adherence to MVP feature list, defer non-critical items  
**Probability:** High | **Impact:** High

**Risk:** Testing phase insufficient  
**Mitigation:** Start testing early, continuous integration approach  
**Probability:** Medium | **Impact:** High

---

## Out of Scope (Future Phases)

The following features are **NOT** included in MVP but planned for future releases:

### Phase 2 Features (Web App Migration)
- 3-tier web architecture
- REST API backend
- Vue.js frontend
- Database storage (PostgreSQL)
- User accounts
- Multiplayer support

### Content Expansions
- Trial #2-10 for Muddlebrook
- Eberron campaign
- Character leveling system (beyond level 2)
- Full spell system
- Crafting system
- Merchant economy

### Advanced Features
- AI-generated narrative responses
- Procedural Trial generation
- Custom campaign editor
- Achievement system
- Leaderboards
- Voice narration

---

## Dependencies

### Required
- Java 17+ (JDK)
- Maven 3.8+
- SnakeYAML (YAML parsing)
- JUnit 5 (testing framework)

### Optional
- Jackson (JSON processing)
- Apache Commons CLI (command-line parsing)
- Jansi (colored console output)
- Picocli (enhanced CLI framework)

---

## Deliverables

### Code
- [ ] Complete source code on GitHub
- [ ] All classes implemented and tested
- [ ] YAML data files for Muddlebrook
- [ ] Unit tests with >90% coverage

### Documentation
- [ ] README.md with setup instructions
- [ ] Data model specification
- [ ] Command reference guide
- [ ] Developer guide for adding content
- [ ] Gameplay walkthrough

### Demonstration
- [ ] Live demo playthrough (Trial #1 completion)
- [ ] Presentation slides
- [ ] UML diagrams (class, sequence, state)
- [ ] Architecture diagram

### ZCW Requirements
- [ ] Project proposal document (this spec)
- [ ] Sprint planning documentation
- [ ] Test documentation
- [ ] Post-mortem analysis

---

## Acceptance Criteria (MVP Complete)

QuestKeeper MVP is considered complete when:

1. **Character Creation**: Player can create a D&D 5e character through guided process
2. **Navigation**: Player can explore all 6 Muddlebrook locations
3. **NPC Interaction**: Player can talk to Norrin, Mara, Darius, and Elara
4. **Trial #1**: All 6 MiniGames in Mayor's Office are playable
5. **Combat**: Player can complete at least one combat encounter
6. **Items**: Player receives and can use magical item rewards
7. **Save/Load**: Game state persists across sessions
8. **Commands**: Parser handles all core commands naturally
9. **Testing**: All core systems have passing unit tests
10. **Documentation**: README and guides are complete

**Definition of Done:** A playtester can create a character, play through Trial #1, defeat enemies, collect rewards, save their game, and resume later—all without bugs or confusion.

---

## Appendix

### Glossary

**CLI** - Command-Line Interface  
**D&D** - Dungeons & Dragons  
**5e** - Fifth Edition (of D&D rules)  
**DM** - Dungeon Master  
**NPC** - Non-Player Character  
**DC** - Difficulty Class (target number for skill checks)  
**HP** - Hit Points  
**AC** - Armor Class  
**STR/DEX/CON/INT/WIS/CHA** - Six D&D ability scores  
**MVP** - Minimum Viable Product  
**YAML** - Yet Another Markup Language (data format)

### References

- D&D 5e Basic Rules: https://www.dndbeyond.com/sources/basic-rules
- Muddlebrook Campaign Manual: `Muddlebrook_Story.pdf`
- Command-Line Game Design: Research document `cli_game_comparison.md`
- Data Model: `questkeeper_data_model.md`

---

**Document Version:** 1.0  
**Last Updated:** December 22, 2025  
**Author:** Marc McGough  
**Status:** Draft - Pending Review