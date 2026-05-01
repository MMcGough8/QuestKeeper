# Divine Smite slot tracking out of sync

## Symptom
Paladin's Divine Smite is tracked using a separate field from the
Spellbook's spell slot field. The two never sync, which means a
Paladin can either Divine Smite indefinitely without consuming
spell slots, or run out of "smite slots" while still having spell
slots available — depending on which field is read.

## Discovered
April 2026, during class progression gap analysis. Documented in
docs/gap-analysis/class-progression.md.

## Impact
Real bug, not a gap. Will manifest at any level where a Paladin
has spell slots and uses Divine Smite. Demo characters at low
levels likely won't trigger it.

## Proposed fix
Refactor Paladin to use a single source of truth for spell slots
(probably the Spellbook). Remove the separate Divine Smite slot
field. Update Divine Smite logic to consume from Spellbook slots
directly, gated by spell level (1st-level slots and up, per RAW).

## Files involved
- src/main/java/com/questkeeper/character/features/PaladinFeatures.java
- src/main/java/com/questkeeper/character/Spellbook.java (likely;
  verify path)
