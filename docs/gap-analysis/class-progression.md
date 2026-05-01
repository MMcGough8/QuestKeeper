# Class Progression Gap Analysis (vs PHB Levels 1-20)

Diagnosis-only audit of QuestKeeper's six implemented classes. No fixes
proposed; this document just describes what is and is not in the code as of
the latest commit on `main`.

## TL;DR

- **Hard level cap:** 20 (`Character.java:1073` clamps `setLevel`).
- **Effective mechanical cap:** 3 for every class. After level 3, only
  scaling-numeric features keep updating (Sneak Attack dice, Martial Arts
  die, Ki points, Rage uses/damage, spell slots). No new features unlock at
  levels 4-20 in any class.
- **No Ability Score Improvement (ASI) mechanism exists** anywhere in the
  codebase. Every class is missing all 5-7 ASIs PHB grants from level 4
  onward.
- **No Extra Attack mechanism exists.** The combat loop is single-attack;
  Fighter/Paladin/Ranger/Monk/Barbarian all miss this.
- **Subclass selection is hardcoded** to one option per class
  (Champion / Devotion / Thief / Open Hand / Hunter / Berserker), with no
  player choice, and most subclass features beyond level 3 are absent.
- **Several level-2/3 features are stubs**: descriptive text and tracked
  state exist, but the combat loop never reads them (Sacred Weapon, Turn
  the Unholy, Flurry of Blows, Patient Defense, Step of the Wind, Open Hand
  Technique, Deflect Missiles, Primeval Awareness, Hunter's Prey variants,
  Frenzy, Danger Sense, Rage damage resistance).
- **Spellcasting:** slot tables are correct through level 20, but the spell
  registry contains only 5 spells (all 1st-level), spells are hardcoded
  Java classes, and the level-up flow does not add or prompt for new spells.

## Level cap summary

| Class | Hard cap | Last feature unlock | Effective max |
|---|---|---|---|
| Fighter | 20 (`Character.java:1073`) | Lvl 3 — Improved Critical | Lvl 3 mechanically; Lvl 20 stat-only |
| Paladin | 20 | Lvl 3 — Channel Divinity / Sacred Weapon / Turn the Unholy / Divine Health | Lvl 3 mechanically; spell slots scale to 20 |
| Rogue | 20 | Lvl 3 — Fast Hands, Second-Story Work | Lvl 3 features + Sneak Attack dice scale to 20 |
| Monk | 20 | Lvl 3 — Deflect Missiles, Open Hand Technique | Lvl 3 features + Martial Arts die / Ki scale to 20 |
| Ranger | 20 | Lvl 3 — Primeval Awareness, Hunter's Prey | Lvl 3 features + spell slots scale to 20 |
| Barbarian | 20 | Lvl 3 — Frenzy | Lvl 3 features + Rage uses / damage scale to 20 |

XP-to-level table covers all 1-20 (`Character.java:204-207`).
`Character.addExperience()` loops through levels (`Character.java:1041-1053`).
`levelUp()` increments level, recalculates HP, calls
`initializeClassFeatures()` and `spellbook.onLevelUp(level)`
(`Character.java:1055-1069`).

---

## Fighter

### Implemented
- **Lvl 1: Fighting Style** — `FighterFeatures.java:43-48`, registered in `Character.java:580-588`. Chosen via `Character.setFightingStyle()`.
- **Lvl 1: Second Wind** (1d10 + level, 1/short rest) — `FighterFeatures.java:102-132`.
- **Lvl 2: Action Surge** (1 use, short rest) — `FighterFeatures.java:138-183`. Max-uses field never bumped to 2 at Lvl 17.
- **Lvl 3: Improved Critical** (Champion subclass, hardcoded) — `FighterFeatures.java:210-228`; consumed by `Character.getCriticalThreshold()` (`Character.java:809-811`).

### Missing PHB features (through level 20)
- Lvl 4, 6, 8, 12, 14, 16, 19: Ability Score Improvement (7 ASIs total)
- Lvl 5, 11, 20: Extra Attack (2 / 3 / 4 attacks)
- Lvl 7: Remarkable Athlete (Champion)
- Lvl 9: Indomitable (1 use)
- Lvl 10: Additional Fighting Style (Champion)
- Lvl 13: Indomitable (2 uses)
- Lvl 15: Superior Critical (Champion - crit on 18-20)
- Lvl 17: Action Surge (2 uses) — *stub: max-uses field exists but never updated*
- Lvl 17: Indomitable (3 uses)
- Lvl 18: Survivor (Champion)
- Subclass: Battle Master and Eldritch Knight absent

### Notes
- Subclass forced at Lvl 3 (Champion only).
- `FighterFeatures` Javadoc explicitly limits scope to Lvl 1-3
  (`FighterFeatures.java:12-15`).

---

## Paladin

### Implemented
- **Lvl 1: Divine Sense** (1 + CHA mod uses, long rest) — `PaladinFeatures.java:149-183`; uses recalculated on level-up `Character.java:669-672`.
- **Lvl 1: Lay on Hands** (5 × level pool) — `PaladinFeatures.java:189-293`.
- **Lvl 2: Fighting Style** (if set) — `PaladinFeatures.java:299-316`.
- **Lvl 2: Spellcasting / spell slots** (full half-caster table 1-20) — `PaladinFeatures.java:75-89` and `SpellSlots.getHalfCasterSlots()` (`SpellSlots.java:83-97`).
- **Lvl 2: Divine Smite** (2d8 base, +1d8 per slot level above 1, max 5d8, +1d8 vs undead/fiend) — `PaladinFeatures.java:323-471`.
- **Lvl 3: Divine Health** (immunity to disease, passive flag) — `PaladinFeatures.java:476-486`.
- **Lvl 3: Channel Divinity** (1/short rest) — `PaladinFeatures.java:491-512`.
- **Lvl 3: Sacred Weapon** (Devotion option) — `PaladinFeatures.java:517-571`. *Stub: timer logic exists but not wired to attack rolls.*
- **Lvl 3: Turn the Unholy** (Devotion option) — `PaladinFeatures.java:576-596`. *Stub: DC calc exists but no application to enemies.*

### Missing PHB features (through level 20)
- Lvl 4, 8, 12, 16, 19: Ability Score Improvement
- Lvl 5: Extra Attack
- Lvl 6: Aura of Protection (+CHA to saves for nearby allies)
- Lvl 7: Aura of Devotion (immunity to charm)
- Lvl 10: Aura of Courage (immunity to frightened)
- Lvl 11: Improved Divine Smite (+1d8 radiant on every melee hit)
- Lvl 14: Cleansing Touch
- Lvl 15: Purity of Spirit (Devotion - permanent Protection from Evil and Good)
- Lvl 18: Aura range increases to 30 ft
- Lvl 20: Holy Nimbus (Devotion capstone)
- Oath spells not implemented at any level
- Subclass: Ancients and Vengeance absent

### Notes
- Channel Divinity resets on short rest correctly.
- `SacredWeapon` and `TurnTheUnholy` are passive flag classes containing
  helper methods; combat does not reference them.

---

## Rogue

### Implemented
- **Lvl 1: Sneak Attack** — dice formula `(level+1)/2` covers 1d6 → 10d6 across Lvl 1-20 (`RogueFeatures.java:142-144`); auto-updated on level-up (`Character.java:596-602`).
- **Lvl 1: Expertise** (set via `Character.setExpertiseSkills()`) — `RogueFeatures.java:168-196`. Only one slot; not re-prompted at Lvl 6.
- **Lvl 1: Thieves' Cant** — `RogueFeatures.java:201-213`.
- **Lvl 2: Cunning Action** (Dash / Disengage / Hide) — `RogueFeatures.java:218-264`.
- **Lvl 3: Fast Hands** (Thief) — `RogueFeatures.java:269-281`.
- **Lvl 3: Second-Story Work** (Thief) — `RogueFeatures.java:286-306`.

### Missing PHB features (through level 20)
- Lvl 4, 8, 10, 12, 16, 19: Ability Score Improvement
- Lvl 5: Uncanny Dodge (halve damage from one attacker, reaction)
- Lvl 6: Expertise (second pair) — no upgrade hook in `setExpertiseSkills`
- Lvl 7: Evasion (DEX save halve → no damage)
- Lvl 9: Supreme Sneak (Thief - advantage on Stealth at half speed)
- Lvl 11: Reliable Talent
- Lvl 13: Use Magic Device (Thief)
- Lvl 14: Blindsense
- Lvl 15: Slippery Mind (WIS save proficiency)
- Lvl 17: Thief's Reflexes (Thief - extra turn round 1)
- Lvl 18: Elusive
- Lvl 20: Stroke of Luck
- Subclass: Assassin and Arcane Trickster absent

### Notes
- `RogueFeatures` Javadoc states scope is Lvl 1-3 (`RogueFeatures.java:14-19`).

---

## Monk

### Implemented
- **Lvl 1: Unarmored Defense** (10 + DEX + WIS, no shield) — `MonkFeatures.java:139-159`; integrated in `Character.getArmorClass()` (`Character.java:316-325`).
- **Lvl 1: Martial Arts** — die scales d4 / d6 / d8 / d10 across full Lvl 1-20 (`MonkFeatures.java:72-77`); auto-updated (`Character.java:626-631`).
- **Lvl 2: Ki** (points = monk level, scales 1-20) — `MonkFeatures.java:215-275`; updated on level-up (`Character.java:632-637`).
- **Lvl 2: Flurry of Blows** (passive description) — `MonkFeatures.java:280-291`. *Stub: no combat hook spending Ki.*
- **Lvl 2: Patient Defense** (passive description) — `MonkFeatures.java:296-308`. *Stub.*
- **Lvl 2: Step of the Wind** (passive description) — `MonkFeatures.java:313-324`. *Stub.*
- **Lvl 3: Deflect Missiles** — reduction formula scales with level (`MonkFeatures.java:329-372`); auto-updated (`Character.java:638-644`). *Stub: no reaction hook in combat.*
- **Lvl 3: Open Hand Technique** (passive description) — `MonkFeatures.java:377-391`. *Stub.*

### Missing PHB features (through level 20)
- Lvl 2: Unarmored Movement (+10 ft scaling to +30 ft)
- Lvl 4, 8, 12, 16, 19: Ability Score Improvement
- Lvl 4: Slow Fall
- Lvl 5: Extra Attack
- Lvl 5: Stunning Strike
- Lvl 6: Ki-Empowered Strikes (unarmed = magical for resistance bypass)
- Lvl 6: Wholeness of Body (Open Hand)
- Lvl 7: Evasion
- Lvl 7: Stillness of Mind
- Lvl 9: Unarmored Movement Improvement (vertical/liquid)
- Lvl 10: Purity of Body
- Lvl 11: Tranquility (Open Hand)
- Lvl 13: Tongue of the Sun and Moon
- Lvl 14: Diamond Soul
- Lvl 15: Timeless Body
- Lvl 17: Quivering Palm (Open Hand)
- Lvl 18: Empty Body
- Lvl 20: Perfect Self
- Subclass: Shadow and Four Elements absent

---

## Ranger

### Implemented
- **Lvl 1: Favored Enemy** (selectable enum; null until set) — `RangerFeatures.java:246-275`.
- **Lvl 1: Natural Explorer** (selectable terrain enum; null until set) — `RangerFeatures.java:280-304`.
- **Lvl 2: Fighting Style** — `RangerFeatures.java:310-327`.
- **Lvl 2: Spellcasting / spell slots** (1-20 half-caster) — `RangerFeatures.java:141-155, 332-418`.
- **Lvl 3: Primeval Awareness** — `RangerFeatures.java:423-437`. *Stub (description only).*
- **Lvl 3: Hunter's Prey** (Colossus Slayer / Giant Killer / Horde Breaker) — `RangerFeatures.java:442-564`. Each variant tracks turn state but has no combat-loop integration.

### Missing PHB features (through level 20)
- Lvl 4, 8, 12, 16, 19: Ability Score Improvement
- Lvl 5: Extra Attack
- Lvl 6: Favored Enemy improvement
- Lvl 6: Natural Explorer improvement
- Lvl 7: Defensive Tactics (Hunter)
- Lvl 8: Land's Stride
- Lvl 10: Hide in Plain Sight
- Lvl 11: Multiattack (Hunter - Volley / Whirlwind)
- Lvl 14: Vanish
- Lvl 14: Favored Enemy improvement (third)
- Lvl 14: Natural Explorer improvement (third)
- Lvl 15: Superior Hunter's Defense (Hunter)
- Lvl 18: Feral Senses
- Lvl 20: Foe Slayer
- Subclass: Beast Master absent

### Notes
- `Character.initializeClassFeatures()` always passes `null` for Favored Enemy
  and Natural Explorer (`Character.java:675-676`); no setter exists on
  `Character` to choose them. A freshly-leveled Ranger therefore has zero of
  these features wired in. Structural gap, not just unimplemented levels.

---

## Barbarian

### Implemented
- **Lvl 1: Rage** (uses scale 2/3/4/5/6/unlimited per `getRagesPerLongRest()`; damage 2/3/4 per `getRageDamageBonus()`) — `BarbarianFeatures.java:64-81, 126-221`. Uses + damage updated on level-up (`Character.java:611-617`).
- **Lvl 1: Unarmored Defense** (10 + DEX + CON) — `BarbarianFeatures.java:226-247`; integrated in `Character.java:312-316`.
- **Lvl 2: Reckless Attack** — `BarbarianFeatures.java:253-301`.
- **Lvl 2: Danger Sense** — `BarbarianFeatures.java:306-318`. *Stub (description only).*
- **Lvl 3: Frenzy** (Berserker) — `BarbarianFeatures.java:324-337`. *Stub.*

### Missing PHB features (through level 20)
- Lvl 4, 8, 12, 16, 19: Ability Score Improvement
- Lvl 5: Extra Attack
- Lvl 5: Fast Movement (+10 ft when unarmored)
- Lvl 6: Mindless Rage (Berserker - immune to charm/fright while raging)
- Lvl 7: Feral Instinct
- Lvl 9: Brutal Critical (1 die)
- Lvl 10: Intimidating Presence (Berserker)
- Lvl 11: Relentless Rage
- Lvl 13: Brutal Critical (2 dice)
- Lvl 14: Retaliation (Berserker)
- Lvl 15: Persistent Rage
- Lvl 17: Brutal Critical (3 dice)
- Lvl 18: Indomitable Might
- Lvl 20: Primal Champion (+4 STR/CON, caps raised to 24)

### Notes
- Rage damage resistance to physical damage is described in the activate
  string but not wired into damage application (no code in
  `Character.takeDamage()` checks `isRaging()`). Stub.
- Lvl 20 Primal Champion is structurally blocked because
  `MAX_ABILITY_SCORE = 20` is hardcoded (`Character.java:198`); raising
  ability caps to 24 cannot be expressed in the current Character model.

---

## Spellcasting (Paladin and Ranger)

### Spell slot progression

Both classes use `SpellSlots.getHalfCasterSlots()`
(`SpellSlots.java:83-97`), a complete 1-20 half-caster table matching the
PHB exactly. Slots start at level 2 and max out at 4 / 3 / 3 / 3 / 2 (1st
through 5th level slots) by class level 19-20.

The Paladin and Ranger feature classes each duplicate this table internally
(`PaladinFeatures.java:75-89`, `RangerFeatures.java:141-155`) inside
`DivineSmite` and `RangerSpellcasting`. **Two separate slot trackers** exist
per Paladin: `Spellbook.spellSlots` and `DivineSmite.currentSlots`. They are
not synchronized — casting via `Spellbook.cast()` does not decrement Divine
Smite's pool; expending a slot via `DivineSmite.expendSlot()` does not
decrement Spellbook's. This is a real bug.

### Maximum spell level supported

`SpellSlots` is sized for 9 spell levels; full casters reach 9th-level slots
at Lvl 17 (`SpellSlots.java:71`). Half-casters reach 5th level (correct).

The spell registry contains only **5 spells total** (`SpellRegistry.java:18-25`):
Cure Wounds, Magic Missile, Shield, Fire Bolt, Sacred Flame. The highest
spell level present in the registry is 1st level. **No 2nd-level or higher
spells exist anywhere in the code.** Slots scale, but there is nothing to
cast in slots above 1st.

### Spell loading mechanism

Spells are **hardcoded as Java classes** in
`src/main/java/com/questkeeper/magic/spells/` (5 classes:
`CureWounds.java`, `FireBolt.java`, `MagicMissile.java`, `SacredFlame.java`,
`Shield.java`). They are constructed and registered in a static block
(`SpellRegistry.java:18-25`). No YAML or JSON spell files exist in
`src/main/resources/` — only campaign content YAMLs. Adding a new spell
requires writing a Java class extending `Spell` / `AbstractSpell` and a
`register(...)` call.

### Level-up spell selection flow

On level-up, `Character.levelUp()` calls `spellbook.onLevelUp(level)`
(`Character.java:1068`), which calls `spellSlots.setCasterLevel(level)`
(`Spellbook.java:390-394`). This **only resizes the slot table.** No spell
is added, prompted, or chosen.

The only place spells get into a Spellbook is the constructor path:
`Character` constructor → `spellbook.initializeForClass()`
(`Character.java:274`) → `Spellbook.addDefaultSpells()`
(`Spellbook.java:88-136`), which hardcodes a single spell per class:

- Paladin Lvl 2+: `cure_wounds` only
- Ranger Lvl 2+: `cure_wounds` only

`addDefaultSpells` is gated by `if (level >= 2)` but is only called from the
constructor; if a character is created at level 1 and levels up to 2, the
call is not re-invoked. A leveled-up Paladin or Ranger therefore never
gains a spell automatically.

There is **no UI prompt or selection mechanism** for spell learning anywhere.
`CharacterCreator.java` makes no reference to spells (zero matches for
"spell" or "Spell"). `GameEngine.java` has no `levelUp` or `onLevelUp`
references.

End result: slots scale correctly through Lvl 20, but the spell list is
effectively frozen at one spell (`cure_wounds`) for both half-caster classes
regardless of level, and there is no selection UX.
