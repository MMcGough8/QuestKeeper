# Map Feature — Design Doc

**Status:** Approved — Phase 1 starting.
**Date:** 2026-05-04
**Owner:** Marc McGough
**Goal:** Add a `map` command that renders an ASCII map of the player's
discovered world. Fill-it-in-as-you-explore is the pitch beat.

**Decisions ratified 2026-05-04:**
- Render style: boxed cells (`┌──┐`, `└──┘`).
- Gating: ungated — `map` works from turn 1.
- Per-campaign coord overrides: deferred to v2.
- Coverage: all three shipped campaigns must look correct before merge.
- Footer grouping: orphan rooms grouped under a subhead if >3.

---

## 1. Goal & Scope

### In scope
- New verb: `map`. Routes through a new `MapCommandHandler`.
- Renders **only visited locations** plus their immediately adjacent
  but unvisited neighbors as `?`, to hint where exploration leads.
- Auto-laid-out via BFS from the player's starting room.
- Marks current location with `[*]`, visited with name, locked rooms
  with `(locked)` suffix.
- Respects the existing 60-column display width used by `Display.printBox`.

### Out of scope (v1)
- Room interiors, item icons, NPC markers.
- True dungeon-style room rectangles. We're drawing a **node-and-edge
  graph**, not a tile map.
- Editing layouts. Layouts are derived, never authored.
- Curved or diagonal connectors. Only `─` (horizontal), `│` (vertical),
  `⇕` (up/down stack marker).
- Animated reveals.

---

## 2. Data We Have

`Location` already exposes:
```java
String getId();
String getName();
Map<String, String> getExits();   // direction -> targetLocationId
boolean isLocked();
```

`GameState` exposes:
```java
Set<String> getVisitedLocations();
String getCurrentLocationId();
```

`Campaign` exposes:
```java
Location getLocation(String id);
String getStartingLocationId();
```

That's everything the renderer needs. No new fields, no YAML changes.

---

## 3. Algorithm

### 3a. Coordinate assignment (BFS)

Start at `campaign.getStartingLocationId()`. Place it at `(0, 0)`.
BFS outward. For each neighbor reached via direction `d`:

| Direction | dx | dy |
|-----------|----|----|
| `north` | 0 | -1 |
| `south` | 0 | +1 |
| `east` | +1 | 0 |
| `west` | -1 | 0 |
| `up` | 0 | 0 (special — see §3c) |
| `down` | 0 | 0 (special — see §3c) |
| anything else | skipped (see §3d) |

If a target location is already placed at a different `(x, y)`, we
detect a **conflict**: the campaign's exit graph is not embeddable in
2D under cardinal-only constraints. Two choices:

1. **Keep first placement** (BFS-order wins) and mark the conflicting
   edge with a "(see footnote)" marker in the rendered map.
2. **Drop the room into the orphans list** if no consistent placement
   exists at all.

V1 picks option 1: first-placement wins, conflicting edges become
footnotes. This keeps the main map readable and is honest about
non-Euclidean geometry rather than silently lying.

### 3b. Placement algorithm

```
function placeRooms(start, campaign, visited):
    coords = {}
    coords[start] = (0, 0)
    queue = [start]
    while queue:
        roomId = queue.pop_front()
        room = campaign.getLocation(roomId)
        for direction, targetId in room.getExits():
            (dx, dy) = CARDINAL_DELTAS.get(direction, null)
            if dx is null: continue        # not cardinal, skip
            target = (coords[roomId].x + dx, coords[roomId].y + dy)
            if targetId in coords:
                if coords[targetId] != target:
                    record conflict (roomId, direction, targetId)
                continue
            if target in coords.values():
                # spot already taken by a different room -> conflict
                record conflict; skip placement
                continue
            coords[targetId] = target
            queue.push_back(targetId)
    return (coords, conflicts)
```

### 3c. Vertical exits (up/down)

Cardinal-only placement loses the clocktower stack
(`clocktower_hill -> up -> clocktower_base -> up -> clocktower_mechanism
-> up -> harlequin_lair`). v1 handles this with a **stack annotation**:
if a placed room has an `up` or `down` exit to another visited room,
draw a `⇕` symbol next to its label. Render the vertical chain as a
small inset list below the main map:

```
Vertical paths:
  Clocktower Hill ⇕ Clocktower Base ⇕ Clocktower Mechanism ⇕ Harlequin Lair
```

This is a known compromise; full 3D layout is out of scope.

### 3d. Non-cardinal / themed exits

Exits like `out`, `exit`, `to_the_market`, `door` are skipped during
placement. They appear under the map in a **footnote section**:

```
Other paths from here:
  - market (Market Row)
  - theater_door (Theater Foyer)
```

### 3e. Rendering

Once `coords` is built:
1. Compute bounding box: `(minX, minY)` to `(maxX, maxY)`.
2. Allocate a character grid of width `(maxX - minX + 1) * CELL_W` and
   height `(maxY - minY + 1) * CELL_H` where `CELL_W = 18` (room
   label slot) and `CELL_H = 3` (label + connector + spacer).
3. Paint each placed room as a centered label inside its cell. Truncate
   names to 14 chars with an ellipsis.
4. Paint connectors between adjacent placed rooms in cells where the
   placement is consistent. Use `─` and `│`.
5. Mark the current room with `[*]` prefix; locked rooms with
   `(locked)` suffix; first-time-seen-but-not-yet-visited rooms (the
   `?`-mode peek-ahead) with `?` instead of name.
6. Frame the map with the same `printBox` style used elsewhere.

---

## 4. Sample Output (Muddlebrook, mid-game)

Player has visited: `drunken_dragon_inn`, `town_square`, `town_hall`,
`market_row`, `clocktower_hill`. Currently at `town_square`. Adjacent
unvisited (peek-ahead): `cemetery`, `clocktower_base`.

```
╔════════════════════════════════════════════════════════════╗
║                       WORLD MAP                            ║
╚════════════════════════════════════════════════════════════╝

                    ┌──────────────┐
                    │  Town Hall   │
                    └──────┬───────┘
                           │
   ┌──────────────┐ ┌──────┴───────┐ ┌──────────────┐
   │Clocktower H ⇕│─│[*]Town Square│─│  Market Row  │
   └──────────────┘ └──────┬───────┘ └──────────────┘
                           │
                    ┌──────┴───────┐
                    │  Cemetery ?  │
                    └──────────────┘

Vertical paths:
  Clocktower Hill ⇕ Clocktower Base ?

Locations discovered: 5 / 18
Current: Town Square

Legend:
  [*] You are here    ?  Unvisited (you can see it from here)
  ⇕ Vertical path     (locked) Currently inaccessible
```

(Box widths in the sketch above are illustrative; the real renderer
sizes per `CELL_W=18`.)

---

## 5. Edge Cases & Fallbacks

| Case | Handling |
|------|----------|
| Room only reachable via non-cardinal exit (e.g., `mayors_office`) | Listed under "Other locations discovered" footer, not on the grid. |
| Two rooms collide on the same `(x, y)` | First placed wins; second is added to footer with note "Connection from <room> via <direction> not drawn." |
| Asymmetric exits (cemetery -> south town_square but no north -> cemetery) | Cemetery still placed using reverse-direction inference (south from cemetery to town_square implies cemetery is north of town_square). Edge drawn. |
| Large map (>40 rooms across) | Truncate to a window centered on current room (10 cells out in each direction). Show "-> more" markers at edges. |
| All exits non-cardinal | Map shows a single `[*]` cell with all exits in the footer. |
| Empty visited set | "You haven't been anywhere yet. Try `look` and `go <direction>`." |

---

## 6. Command Surface

**New verb:** `map`. Aliases: `m`. (Single-letter `m` is currently
unused per `CommandParser.SYNONYM_MAP`.)

**New handler:** `MapCommandHandler implements CommandHandler` in
`com.questkeeper.core.command`.

```java
public class MapCommandHandler implements CommandHandler {
    private static final Set<String> HANDLED = Set.of("map", "m");

    public Set<String> getHandledVerbs() { return HANDLED; }

    public CommandResult handle(GameContext ctx, String verb, String noun, String input) {
        Display.println(MapRenderer.render(ctx.getCampaign(), ctx.getGameState()));
        return CommandResult.success();
    }
}
```

**New rendering class:** `com.questkeeper.ui.MapRenderer` —
pure-function `render(Campaign, GameState) -> String`. No state, no
side effects. Easy to unit-test.

**Wiring:** add `new MapCommandHandler()` to
`CommandRouter.createDefault()`.

---

## 7. Implementation Phases

Each phase ships green tests before the next starts.

1. **Phase 1 — Layout engine** (no rendering, no UI).
   `MapLayout` class + tests:
   - BFS coordinate assignment from a starting node.
   - Conflict detection.
   - Vertical-stack extraction.
   - Orphan list.
   ~150 lines + ~200 lines of tests using muddlebrook & eberron fixtures.

2. **Phase 2 — Renderer.**
   `MapRenderer.render(Campaign, GameState) -> String`:
   - Take a `MapLayout`, paint a char grid, return the framed string.
   - Snapshot tests against muddlebrook with various visited subsets.
   ~200 lines + ~150 lines of tests.

3. **Phase 3 — Command wiring.**
   - `MapCommandHandler` (~30 lines).
   - Register in `CommandRouter`.
   - Add `map` to `SYNONYM_MAP` (alias `m`).
   - Add to `help` text.
   - End-to-end smoke test via `GameEngineTest`.
   ~50 lines code + ~50 lines tests.

4. **Phase 4 — Polish.**
   - Color (visited cyan, current green, locked dim, peek `?` yellow).
   - Window-around-current truncation for large maps.
   - "Vertical paths" inset.
   - Update help text and demo run.

Total estimate: **~600 lines of code + ~400 lines of tests**, ~1-2 dev
days. Phase 1+2 alone is enough for a credible pitch demo if Phase 3+4
slip.

---

## 8. Risks / Known Limitations

- **Topology lies.** A campaign that exits south from A to B *and*
  north from B to C will place C above A, even if the author meant C
  somewhere else. We accept this; map is a navigation aid, not a
  cartographer.
- **Stack rendering for >2 vertical levels** gets cramped. Falls back
  to footer list cleanly.
- **Localization** of direction names (e.g., `Norte`, `Sur`) is not
  handled. Cardinal detection is hardcoded English. Acceptable today.
- **YAML one-way exits.** Some campaigns define A -> B but not B -> A.
  We draw what's authored; we do *not* synthesize phantom return edges.

---

## 9. Open Questions

1. Should the map be **ungated** (always visible via `map`) or **gated**
   on having visited at least 3 locations? Voting for ungated — even
   one room is a useful demo.
2. Do we want **per-campaign overrides**? E.g., a campaign author
   could ship a `map.yaml` with hand-placed coordinates that override
   BFS layout. Probably yes eventually, but not v1.
3. How do trial / mini-game locations show up? Treat as normal rooms
   for now — they appear when entered and disappear when not visited.
4. Should locked-but-discovered rooms render? E.g., `clocktower_base`
   is visible from `clocktower_hill` even before unlock. Vote yes,
   render with `(locked)` suffix per §3e.

---

## 10. Acceptance Criteria

- [ ] `map` command works in Muddlebrook, Eberron, DrownedGod.
- [ ] Map updates as the player visits new rooms (no caching bugs).
- [ ] Current location is clearly marked.
- [ ] Locked rooms are visually distinct.
- [ ] Non-cardinal exits never silently disappear; always footnoted.
- [ ] Renders in <= 60 columns when total map fits; truncates gracefully
      otherwise.
- [ ] Full test suite stays green; map-specific tests cover layout,
      conflict, vertical, orphan, and render paths.
