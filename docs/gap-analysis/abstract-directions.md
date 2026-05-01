# Abstract directional language not supported

## Summary
Parser handles concrete directions (`north`, `door`) but fails on abstract
directional concepts: `away`, `back`, `out`, `up`, `toward [thing]`,
`after [npc]`, `leave`, `escape`, `enter [thing]`.

## Example
`walk away` → parses to `(go, "away")` → ExplorationCommandHandler tries
to find an exit named "away" → fails with "You can't go away from here."

The parse succeeded. The exit matcher failed. That's the right error
message but a missing capability.

## Why it matters
Synonym expansion (April 2026) made the parser feel intelligent, which
raises player expectations. When players type natural English directional
phrases that *should* work, the failure feels like the system regressed.

## What a fix would require
1. Movement history per character (`back` = previous exit)
2. Abstract-direction lexicon resolved at runtime, not parse time:
   - `away` → any exit
   - `back` → previous exit (requires #1)
   - `out` → exit semantically marked as outward
   - `toward [name]` → exit leading toward a named entity
3. Clarification UX when abstract direction is ambiguous:
   "You could leave through the door or head north. Which?"
4. Possibly: `Location.outwardExit()` field, NPC position tracking
   for `follow` and `after`.

## Estimate
1-2 weeks of design + implementation, not a small change.
Out of scope for May 2026 pitch.

## Related
- Surfaced during Norrin dialogue/exploration testing, April 30, 2026
- Synonym work that exposed it: commit 406d7e0
