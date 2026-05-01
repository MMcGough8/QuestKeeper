# Demo Save State Plan (handoff for next session)

Goal: produce 4 demo save states for a 5-minute pitch, dropped into
`saves/demo/`, each demonstrating a specific subsystem. Format-checked but
not yet generated. Pick this up where it stands.

## Confirmed: save format

Saves are **YAML-only** (via SnakeYAML), not JSON:

- `SaveState.java:9-10` imports `org.yaml.snakeyaml.{DumperOptions, Yaml}`.
- `SaveState.save()` uses `yaml.dump(...)` and atomic write
  (`SaveState.java:99-127`).
- `SaveState.load()` calls `yaml.load(reader)` and explicitly throws
  `IOException` on `ScannerException` (`SaveState.java:154-155`).
- `SaveState.quickSave()` writes `.yaml` extension (`SaveState.java:165`).
- `SaveState.listSaves()` filters `.endsWith(".yaml")`
  (`SaveState.java:182`) — **a `.json` file would not be discovered by the
  in-game save menu** even if it parsed.

User's original spec said `.json`; was a typo. **Use `.yaml` extensions.**

## Four targets (corrected filenames)

| File | What it demonstrates |
|---|---|
| `saves/demo/01-parser-showcase.yaml` | Level 3 Fighter, Muddlebrook town square, Norrin nearby, 2-3 visible exits. Demo synonym variety: talk/chat/greet, go/walk/dash, look/peek/scrutinize. |
| `saves/demo/02-combat-ready.yaml` | Level 3 Paladin equipped with longsword + chain mail, full HP, full Lay on Hands pool, in a room with a CR ~1 monster (goblin or ghoul). Demo turn-based combat, status effects, possible Divine Smite. |
| `saves/demo/03-trial-midway.yaml` | Mid-trial in Muddlebrook (`gear_alignment` or `backwards_clock` minigame), 3 of 6 minigames completed. Demo trial/minigame system, progress state, campaign-flavored synonyms (wind, align, decipher). |
| `saves/demo/04-dialogue-branching.yaml` | In a dialogue with an NPC who has conditional dialogue based on a flag. Set the flag so the conditional path is available. Demo dialogue depth. |

Plus `saves/demo/README.md` with one line per save: what to type first to
make the demo land.

Constraint: no custom load command. Use existing save/load infrastructure.
Run `mvn test` after creation to confirm nothing broke.

## Save schema (from `SaveState.toMap`, lines 199-227)

Top-level YAML keys, in order:

```yaml
save_version: "1.0"
timestamp: "2026-04-30T22:00:00Z"   # Instant.toString (ISO-8601)
campaign_id: muddlebrook
save_name: "Demo - Parser Showcase"
character:                           # CharacterData.toMap (read this next)
  ...
current_location: town_square        # location id from campaign yaml
visited_locations: [...]             # list of location ids
flags:                               # Map<String, Boolean>
  met_norrin: true
counters:                            # Map<String, Integer>
  enemies_defeated: 0
strings:                             # Map<String, String>
  villain_name: "The Machinist"
inventory: [longsword, chain_mail]   # list of item ids
equipped: [longsword, chain_mail]    # legacy list
equipped_slots:                      # new format, slot -> item id
  MAIN_HAND: longsword
  ARMOR: chain_mail
gold: 0
play_time_seconds: 0
save_count: 0
```

## What still needs investigation

I did NOT read these files this session — next session must:

1. **`src/main/java/com/questkeeper/save/CharacterData.java`** — full
   character serialization schema (ability scores, hit points, hit dice,
   class, race, level, fighting style, expertise, etc.). The `character:`
   sub-map keys live here.
2. **Muddlebrook campaign YAMLs** under
   `src/main/resources/campaigns/muddlebrook/`:
   - `locations.yaml` — confirm "town_square" id, exits, NPC list there
   - `npcs.yaml` — Norrin's id, his location, **find an NPC with
     `required_flags` on a dialogue topic** (this is the dialogue-branching
     candidate). Watch for `required_flags:` keys.
   - `monsters.yaml` — pick CR ~1 monster (look for goblin, ghoul, or
     similar). Capture id, AC, HP, attack bonus.
   - `trials.yaml` — check `trial_01` or similar; identify which trial
     contains `gear_alignment` and `backwards_clock`.
   - `minigames.yaml` — map of minigame ids to skill checks. Confirm which
     trial owns which minigames so we know which 3 of 6 to mark complete.
3. **Trial progression flags** — check existing `Trial` / `MiniGame`
   classes for what flag pattern marks a minigame as complete.
   `started_[trial]` and `completed_[trial]` are documented in CLAUDE.md;
   per-minigame flags are the open question.
4. **For combat-ready (#2)**: the in-room monster needs to be loaded on
   load. Confirm the load path actually instantiates monsters in the
   current location, or whether the player must trigger combat via
   `attack <monster>`.

## Practical pitch starter (suggested in README.md)

| Save | First command(s) to type |
|---|---|
| 01 | `chat norrin` (or any synonym) — show the parser handles natural verbs |
| 02 | `attack goblin` — initiate combat, then `cast smite` or `attack` to show options |
| 03 | `attempt athletics` (or whichever skill the next minigame needs) — show roll + outcome |
| 04 | `talk <npc>` then `ask about <gated-topic>` — show the conditional branch |

## Risks / things to verify before pitch

- The dialogue-branching save (#4) requires picking the right NPC. If no
  Muddlebrook NPC has `required_flags` on any topic, we'd need to use
  Eberron or DrownedGod. Check all three campaigns' npcs.yaml.
- Combat save (#2) may need the engine to be in a particular state when
  loaded — verify how `load` re-enters game loop and whether monsters are
  bound to locations or spawned at combat-init time.
- The level-up flow in `Character` (per the gap-analysis doc) doesn't
  re-call `addDefaultSpells`, so a created-at-3 Paladin in #2 must be
  constructed at level 3 directly so `cure_wounds` lands. Verify by
  reading `CharacterCreator` and the character constructor before
  hand-writing the YAML, or generate the save by playing in-game and
  copying the resulting file.

## Easiest path forward (suggested)

Two strategies, pick at the start of next session:

A. **Hand-write YAML**: read `CharacterData.toMap`, copy the schema into
   each demo save, and tune values. Fast to iterate, but easy to mis-spell
   field names.

B. **Generate by playing**: start the game, get into the desired state for
   each demo, `save`, then copy `saves/<name>.yaml` to
   `saves/demo/0X-...yaml`. Slower per save but guaranteed
   schema-correct.

Recommend B for #2 (combat-ready) and #4 (dialogue) where character /
flag state is fiddly. Hand-write A for #1 and #3 if the schema is
straightforward.
