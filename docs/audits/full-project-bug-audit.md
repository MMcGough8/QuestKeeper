# Full Project Bug Audit

Diagnosis-only audit conducted 2026-05-03 by three parallel research passes
across `combat/`, `magic/`, `character/`, `character/features/`, `save/`,
`state/`, `dialogue/`, `campaign/`, `core/`, `core/command/`, `world/`,
`inventory/`, `inventory/items/effects/`, and `ui/`. No fixes proposed; this
document supports prioritization only.

> **Status update 2026-05-04**: all 40 bugs in this audit have been fixed
> across Phase 1 (combat infra), Phase 2 (class abilities), Phase 3 (ASI +
> remaining classes), and the smoke-test polish round. The "post-pitch"
> deferred items below are kept here for historical context but are
> closed in the codebase. Further audits should start fresh.

## Executive summary

**40 bugs found.**

| Severity | Count | What it means |
|---|---|---|
| Demo-critical | 12 | Hits a path the planned demos walk; needs a fix or workaround before pitch |
| Demo-tangent | 19 | Real bug, but outside the demo's blast radius. Ship-after-pitch is fine |
| Post-pitch | 9 | Cosmetic / latent / unreachable in current campaigns |

The four bugs already fixed today (Spellbook.onLevelUp, SpellSlots.setCasterLevel,
DivineSmite parallel tracker, DivineSmite.setPaladinLevel) are excluded from
all counts.

The pitch is 2026-05-27. At ~30 min average per demo-critical fix, the
critical bucket is roughly **6-8 hours of focused work** — well within budget.

## Recurring patterns

Several bugs share root causes worth naming explicitly:

1. **Activated abilities consume resources but never fire.** Sacred Weapon,
   Patient Defense, Step of the Wind, Flurry of Blows, and Divine Smite all
   set a `boolean active` / increment a counter / spend a resource, but
   `processAttack` (and other read sites) never check the flag. The combat
   loop has no "ability dispatch" stage.

2. **Parallel state trackers across class-feature/spellbook boundaries.**
   We fixed one case (DivineSmite ↔ Spellbook). RangerSpellcasting has the
   same shape but inert. RestSystem has its own version of this with hit-die
   rolls (BUG-305). Hit-die / ki / rage uses also round-trip incorrectly
   across save/load (BUG-207).

3. **Save round-trip incompleteness.** Several Character fields (fighting
   style, expertise skills, Half-Elf bonus abilities, saving-throw
   proficiencies, activated-feature use counts) are never serialized.
   Demo-critical because the Lvl 3 Paladin's fighting style determines AC.

4. **Inputs that escape mode boundaries.** Combat doesn't accept system
   commands (quit/save/load); dialogue silently ends on direction inputs;
   character creator has its own scanner separate from the engine's. All
   create demo risk.

---

## Demo-critical bugs (12)

Suggested fix order (lowest-risk-highest-payoff first):

| # | ID | Bug | Effort | File:line |
|---|---|---|---|---|
| 1 | BUG-002 | Divine Smite `getSlotsStatus()` reads stale standalone slot array, not bound slots | ~5 min | `PaladinFeatures.java:464-476` |
| 2 | BUG-201 | `listSaves` non-recursive — can't see `saves/demo/*.yaml` | ~5 min | `SaveState.java:181` |
| 3 | BUG-303 | Direction-as-verb (`n`, `north`) silently ends active dialogue | ~15 min | `ExplorationCommandHandler.java:108-115` |
| 4 | BUG-205 | FightingStyle, expertiseSkills, halfElfBonusAbilities not persisted | ~30 min | `CharacterData.java`, `Character.java:235-236` |
| 5 | BUG-001 | Divine Smite never deals radiant damage / never expends slot | ~30 min | `CombatSystem.java:1453-1483, 455-702` |
| 6 | BUG-301 | Two competing `Scanner(System.in)` instances drop typed-ahead input | ~30 min | `CharacterCreator.java:31`, `GameEngine.java:65` |
| 7 | BUG-302 | `quit`/`save`/`load`/`rest` unreachable during combat | ~30 min | `GameEngine.java:755-802`, `CombatSystem.java:215-294` |
| 8 | BUG-304 | `MagicItem` is not equippable — passive items can never be worn | ~30 min | `Item.java:220-222`, `MagicItem.java:32-41`, `ItemCommandHandler.java:142-149` |
| 9 | BUG-305 | `RestSystem.useHitDie` displays a fake roll; actual heal uses a different roll | ~30 min | `RestSystem.java:115-145`, `Character.java:417-431` |
| 10 | BUG-206 | Dialogue mid-conversation cannot save/restore (only matters if demo #4 needs mid-dialogue saves) | ~30 min | `DialogueSystem.java:23-31` |
| 11 | BUG-202 | DivineSmite bind path is fragile to construction-time level changes | post-pitch unless triggered | `Character.java:271-274, 651-657` |
| 12 | BUG-005 | Initiative off-by-one when a combatant flees on its own turn (only triggers in multi-enemy combat) | ~30 min | `CombatSystem.java:363-377, 868-910` |

### Detail per bug

#### BUG-002 — Divine Smite slot status display reads stale array
- **File:** `PaladinFeatures.java:464-476`
- **Symptom:** After today's binding fix, `getSlotsStatus()` still iterates
  `currentSlots[i]` (the standalone array, never updated when bound) but
  pulls the max via `getMaxSlots()` (which does delegate). For a Lvl 3
  Paladin leveled from Lvl 2: standalone shows `2/3` even at full slots,
  and stays at `2/3` regardless of expenditure.
- **Reproduction:** Lvl 3 Paladin → `smite` → status reads `L1: 2/3`.
- **Impact:** Demo #2 visibly lies on screen. Easy fix (use the existing
  `readCurrentSlots()` helper).

#### BUG-201 — `listSaves` non-recursive
- **File:** `SaveState.java:181`
- **Symptom:** `Files.list(saveDir)` doesn't recurse into subdirectories, so
  `saves/demo/*.yaml` are invisible to the in-game Load menu.
- **Impact:** Demo saves placed under `saves/demo/` per the demo plan are
  unloadable through the UI without a code change or moving them flat.

#### BUG-303 — Direction-as-verb ends dialogue silently
- **File:** `ExplorationCommandHandler.java:108-115`
- **Symptom:** `CommandParser.parse` promotes any solitary direction word
  (`n`, `north`, etc.) to a `go` command. The `go` handler unconditionally
  calls `dialogueSystem.endDialogue()` on movement success.
- **Impact:** A typo or stray keypress mid-demo-#4 silently ends the
  conversation and moves the player. Pitch tank.

#### BUG-205 — Fighting style / expertise / Half-Elf abilities not persisted
- **Files:** `CharacterData.java` (entire file), `Character.java:235-236, 1125-1153`
- **Symptom:** Three Character fields (`fightingStyle`, `expertiseSkills`,
  `halfElfBonusAbilities`) are never serialized. On restore, `setLevel()`
  re-runs `initializeClassFeatures()` with `fightingStyle = null`, so
  `FightingStyleFeature` is not added.
- **Impact:** A Defense-style Lvl 3 Paladin loses +1 AC on save/load. If the
  demo character has any fighting style chosen, the demo number is wrong.

#### BUG-001 — Divine Smite never deals radiant damage
- **Files:** `CombatSystem.java:1453-1483` (handleDivineSmite),
  `:455-702` (processAttack)
- **Symptom:** `handleDivineSmite` sets `smiteReady = true` and prints a
  flashy message. `processAttack` never references `smiteReady` /
  `isSmiteReady()` / `smite.expendSlot(...)`, never adds 2d8+ radiant damage,
  and `advanceTurn:883` silently clears `smiteReady` at turn end.
  `isSmiteReady` and `clearSmiteReady` exist but have zero callers.
- **Impact:** Demo #2 prominently features Divine Smite. The "Divine Smite
  ready!" message is a lie — the next attack is vanilla. Slots also never
  decrement, so it doesn't even feel like a resource.

#### BUG-301 — Two competing Scanner instances on `System.in`
- **Files:** `CharacterCreator.java:31`, `GameEngine.java:65`
- **Symptom:** `CharacterCreator` declares `private static final Scanner
  scanner = new Scanner(System.in)`, separate from `GameEngine.scanner`.
  Both wrap the same stream. Bytes buffered in one are unrecoverable from
  the other.
- **Impact:** Type-ahead during character creation drops keystrokes after
  the creator exits.

#### BUG-302 — System commands unreachable during combat
- **Files:** `GameEngine.java:755-802` (combat loop),
  `CombatSystem.java:215-294` (action switch)
- **Symptom:** Combat input bypasses `processCommand` / `CommandRouter`
  entirely. Only `attack`, `flee`, and class actions are accepted; `quit`
  yields "Unknown action: quit. Try: attack, flee, or class abilities."
- **Impact:** If anything goes wrong during demo #2, no clean exit without
  dying or fleeing successfully.

#### BUG-304 — `MagicItem` is not equippable
- **Files:** `Item.java:220-222`, `MagicItem.java:32-41` (no override),
  `ItemCommandHandler.java:142-149`
- **Symptom:** `MagicItem` does not override `isEquippable()`. The base
  method only returns true for WEAPON/ARMOR/SHIELD. The equip handler
  filters by `Item::isEquippable` and rejects all magic items.
- **Impact:** Trial mini-game rewards (Ring of Protection,
  Gauntlets of Ogre Power, etc.) cannot be equipped. Demo #3 reward flow
  could land here.

#### BUG-305 — `RestSystem.useHitDie` displays a fake roll
- **Files:** `RestSystem.java:115-145`, `Character.java:417-431`
- **Symptom:** `Character.useHitDie()` rolls internally for the actual heal.
  `RestSystem.useHitDie` rolls a separate die "for display" then calls
  `Character.useHitDie()` which rolls a second die. Display says "Rolled
  d10: 7+2 = 8 HP restored" but the actual heal came from a die the player
  never sees.
- **Impact:** Any rest demo shows numbers that don't match the HP delta.
  Player perception of luck is decoupled from real state.

#### BUG-206 — Dialogue mode cannot be save/restored
- **File:** `DialogueSystem.java:23-31`
- **Symptom:** Dialogue state (`currentNpc`, `currentState`,
  `inConversation`) is runtime-only. Save mid-conversation lands in
  exploration on restore.
- **Impact:** Only demo-critical IF demo #4 wants to save mid-dialogue. The
  workaround is to design demo #4 to start in exploration with `talk <npc>`
  as the first command. If that workaround is OK, this drops to demo-tangent.

#### BUG-202 — DivineSmite bind path is order-fragile
- **Files:** `Character.java:271-274, 651-657`
- **Symptom:** `initializeClassFeatures()` runs before
  `spellbook.initializeForClass()` in the constructor. Currently safe
  because `STARTING_LEVEL = 1` and DivineSmite is Lvl 2+. If a future
  constructor accepts initial level >= 2, the bind grabs `getSpellSlots()`
  before slots exist.
- **Impact:** Latent. The binding does work for the standard
  `new Character(...) → setLevel(N)` flow.

#### BUG-005 — Initiative off-by-one on flee
- **Files:** `CombatSystem.java:363-377`, `868-910`
- **Symptom:** `attemptEnemyFlee` removes the fleer from `participants` and
  `initiative`, shifting subsequent indices down by one. `advanceTurn` then
  modulo-increments `currentTurn`, skipping the shifted combatant.
- **Impact:** Multi-monster combat with at least one cowardly enemy
  (Muddlebrook tags some monsters that way). Single-monster demo #2 won't
  trigger.

---

## Demo-tangent bugs (19)

Real bugs but outside the demo's blast radius.

| ID | Bug | Effort | File:line |
|---|---|---|---|
| BUG-003 | Sacred Weapon CHA-to-attack bonus never applied | ~10 min | `CombatSystem.java:1524-1563, 539-610` |
| BUG-004 | Patient Defense / Step of the Wind / Flurry of Blows / Cunning Action: Disengage are inert no-ops with resource cost | ~half day | `CombatSystem.java:1346-1424, 1183-1204` |
| BUG-006 | Ki / Rage uses refill to max on level-up instead of by delta | ~10 min | `ActivatedFeature.java:92-98` |
| BUG-007 | Rage never expires automatically (`processTurnEnd` unwired) | ~5 min | `BarbarianFeatures.java:185-194`, `CombatSystem.java:868-910` |
| BUG-203 | Mini-game completion state mutates shared Campaign instance during load | ~half day | `GameState.java:121-127`, `CampaignLoader.java:609` |
| BUG-204 | Saving throw proficiencies serialized but never restored | ~5 min | `CharacterData.java:102-142` |
| BUG-207 | Activated feature uses (Lay on Hands, Ki, Rage, Action Surge) reset to max on load | ~2 hr | `Character.java:579-696`, `CharacterData.java:102-142` |
| BUG-208 | Saved `current_location` pointing to a removed location silently snaps to start | ~5 min | `GameState.java:91-99` |
| BUG-209 | Inventory load drops items silently when carrying capacity is exceeded | ~5 min | `GameState.java:142-181`, `Inventory.java:117-129` |
| BUG-306 | `Inventory.equipToSlot` silently drops the previous item if `addItem` fails | ~30 min | `Inventory.java:286-308, 313` |
| BUG-307 | `Dice.parseDetailed` IndexOutOfBounds when history is at capacity | ~5 min | `Dice.java:312-317, 68-76` |
| BUG-310 | No EOF (Ctrl-D) handling on `scanner.nextLine()` — crashes with stack trace | ~30 min | `SystemCommandHandler.java:65`, `GameEngine.java:326,818,1014` |
| BUG-311 | `Display.printBox` throws on title longer than `width - 2` | ~5 min | `Display.java:379-386` |
| BUG-313 | Magic-item passive effects (StatBonus, Resistance, etc.) never apply when equipped | ~half day | 6 effect classes have no callers |
| BUG-315 | `attack <foo>` outside combat works for any monster from anywhere in the campaign | ~30 min | `CombatCommandHandler.java:77-90, 46-50` |
| BUG-318 | `handleAttempt` `target.split(" with ")` breaks names containing "with" | ~15 min | `TrialCommandHandler.java:200-204` |
| BUG-008 | `CureWounds` and `Shield` build display messages that are silently discarded | ~10 min | `CureWounds.java:60-71`, `Shield.java:36-42` |

---

## Post-pitch bugs (9)

Cosmetic, latent, or only reachable in code paths not exercised by current
campaigns or demos.

| ID | Bug | File:line |
|---|---|---|
| BUG-308 | `GameContext.running` is dead state, never read by GameEngine | `GameContext.java:35,47,114-120` |
| BUG-309 | `MagicItem.copy()` shares effect list — duplicated items share charges | `MagicItem.java:362-370`, `Inventory.java:154` |
| BUG-312 | `Display.showActionPrompt` ignores its `suggestions` parameter | `Display.java:701-704` |
| BUG-314 | `removeLeadingArticles` strips the word "about" from `ask` nouns (benign because `parseAskAbout` re-parses original) | `CommandParser.java:450-462` |
| BUG-316 | Save filename collision — different save names that map to the same slug silently overwrite | `GameEngine.java:443-465` |
| BUG-317 | `Dice.parseDetailed` non-synchronized read of `rollHistory.size()` | `Dice.java:313` |
| BUG-319 | Trial completion state inconsistent across save/load (parallel-tracker pattern) | `TrialCommandHandler.java:316`, `MiniGame.isCompleted()` |
| BUG-210 | `started_X` flag parsing collides with non-trial flags | `GameState.java:117-132` |
| BUG-211 | `setTemporaryHitPoints(0)` is no-op when temp HP > 0 (max-only setter) | `Character.java:1016-1018` |
| BUG-212 | `setLevel` does not grant the hit dice that `levelUp()` would | `Character.java:1076-1090` |
| BUG-213 | Flag case sensitivity asymmetric across direct-add vs save-restore | `GameState.java:107-110, 372-390` |

---

## Stub feature classification

Useful for "what to avoid demoing":

| Feature | Status |
|---|---|
| Sacred Weapon | **Actively broken** — Channel Divinity is consumed but +CHA to attack is never applied. (BUG-003) |
| Turn the Unholy | Inert no-op. No combat handler invokes it; player has no way to fire. |
| Flurry of Blows | **Actively broken** — costs 1 ki, sets `flurryAttacksRemaining=2`, but no caller of `useFlurryAttack()` exists. |
| Patient Defense | **Actively broken** — costs 1 ki, sets active flag, but `processAttack` never reads it for disadvantage. (BUG-004) |
| Step of the Wind | Inert no-op — costs 1 ki, sets `disengageActive`, but no opportunity-attack mechanic gates on it. |
| Open Hand Technique | Inert no-op — pure description; never gets a chance to fire because Flurry of Blows is broken. |
| Deflect Missiles | Inert no-op — has helpers but no caller; ranged damage never queries it. |
| Primeval Awareness | Inert no-op — pure description PassiveFeature. |
| Hunter's Prey (Colossus Slayer / Giant Killer / Horde Breaker) | Inert no-ops — `usedThisTurn` plumbing exists, but extra-damage triggers in `processAttack` are absent. Giant Killer's `resetRound()` never invoked. |
| Frenzy | Inert no-op — no bonus-action attack hookup. |
| Danger Sense | Inert no-op — no save-system gate. |
| Rage damage resistance | **Partially working** — physical-damage halving correctly applied. Caveat: blanket-applied to all monster damage incl. fire/etc., which is 5e-incorrect. |

**Demo guidance:** "actively broken" stubs are the ones to avoid. The "inert
no-op" ones print a description, charge nothing, and won't crash; audience
won't notice unless they specifically ask.

---

## Save/load round-trip pre-flight checklist

| Concern | Status |
|---|---|
| `DivineSmite.boundSlots` survives save/load | **YES** via re-binding on `setLevel()` |
| `Spellbook.charClass` field present in CharacterData | **NO** — but re-set during constructor via `initializeForClass`; round-trips by re-derivation |
| Spellbook prepared/known spells survive | **NO** — only class defaults at restored level. Acceptable for demos |
| Spell slots survive (current count) | **NO** — reset to max on restore |
| Equipped vs legacy list inconsistent on save | NO — round-trips cleanly |
| GameState flags / counters / strings round-trip | YES (with case-sensitivity caveat — BUG-213) |
| Trial mid-state representable | **YES** via `completed_minigame_<id>` flags. Demo #3 feasible |
| Character resources (HP / Lay on Hands / Ki / Action Surge / Rage) | **PARTIAL** — only HP / tempHP / hit dice persist. Others reset to max (BUG-207) |
| Dialogue mid-conversation | **NO** — runtime-only; load lands in exploration (BUG-206) |
| Cross-reference validation re-runs on load | **NO** — saved location IDs silently ignored if missing (BUG-208) |

---

## Recommended scope

The four 5-15 min demo-critical fixes (BUG-002, BUG-201, BUG-303, BUG-205)
close ~80% of the visible demo risk in roughly one hour. The five 30-min
demo-critical fixes (BUG-001, BUG-301, BUG-302, BUG-304, BUG-305) are a
second focused session. After those, demo work is unblocked and the
remaining 28 bugs can wait.

BUG-202 stays latent until a future code change exposes it; BUG-005 only
matters in multi-enemy combat. BUG-206 only matters if demo #4 needs
mid-dialogue saves, otherwise drops to demo-tangent.
