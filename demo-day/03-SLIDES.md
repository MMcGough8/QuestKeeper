# QuestKeeper - Slide Deck Content

Use these as content for your presentation slides (Google Slides, Keynote, PowerPoint).
Recommended: 6-7 slides max, mostly visuals, you provide the narration.

---

## SLIDE 1: Title

**Visual:** QuestKeeper logo or terminal screenshot with title screen

```
QUESTKEEPER
A Terminal Fantasy Adventure

[Your Name]
Demo Day [Date]
```

---

## SLIDE 2: The Problem

**Visual:** Split screen - frustrated person coordinating calendars vs. shallow mobile game

**Text:**

```
THE GAP

RPG fans want meaningful adventures
they can play anywhere, anytime.

Mobile games are shallow.
Tabletop requires coordination.
```

---

## SLIDE 3: The Solution

**Visual:** Terminal screenshot showing game in action (status panel, map, narrative)

**Text:**

```
QUESTKEEPER FILLS THAT GAP

A fantasy adventure with real RPG mechanics
that runs offline and on any device.

Real choices.
Real consequences.
Real imagination.
```

---

## SLIDE 4: Live Demo

**Visual:** Just the word "DEMO" or a terminal icon

**Text:**

```
LET ME SHOW YOU
WHAT THAT LOOKS LIKE

[Switch to terminal]
```

*This is where you run the actual game or auto-demo*

---

## SLIDE 5: What Makes It Different

**Visual:** YAML code snippet next to a screenshot of that content in-game

**Text:**

```
ZERO-CODE CONTENT CREATION

locations.yaml → In-game world
npcs.yaml     → Characters with dialogue
items.yaml    → Weapons, armor, magic
trials.yaml   → Puzzle challenges

That's not just a feature.
That's a foundation for something bigger.
```

**YAML Example:**
```yaml
- id: drunken_dragon_inn
  name: The Drunken Dragon Inn
  description: |
    A warm, bustling tavern with
    creaky wooden floors...
  exits:
    north: town_square
  npcs:
    - norrin_bard
    - mara_bartender
```

---

## SLIDE 6: Technical Excellence

**Visual:** Architecture diagram (simplified) or code quality metrics

**Text:**

```
BUILT TO LAST

┌─────────────────────────────────┐
│         1,917 TESTS             │
├─────────────────────────────────┤
│  Fully tested                   │
│  Clean architecture             │
│  Built to last                  │
└─────────────────────────────────┘
```

---

## SLIDE 7: The Vision

**Visual:** Flowchart showing: Creators → Campaigns → Marketplace → Players

**Text:**

```
THE VISION

A community marketplace where creators
publish campaigns and players find
their next adventure.

QuestKeeper becomes the engine.
The community builds the content.
```

---

## SLIDE 8: Close / Questions

**Visual:** GitHub QR code + contact info

**Text:**

```
THANK YOU

github.com/[your-username]/questkeeper

"Great adventure stories should be
 portable and on demand."

QUESTIONS?
```

---

# Speaker Notes

## Slide 1 (5 seconds)
Just say your name and project title, move quickly.

## Slide 2 (15 seconds)
"RPG fans want meaningful adventures they can play anywhere, anytime. But mobile games are shallow, and tabletop requires coordination."

## Slide 3 (15 seconds)
"QuestKeeper fills that gap. A fantasy adventure with real RPG mechanics that runs offline and on any device. Real choices. Real consequences. Real imagination."

## Slide 4 (2-3 minutes)
"Let me show you what that looks like."
Run the demo. Follow the demo script.

## Slide 5 (20 seconds)
"What makes QuestKeeper different? Content creation requires zero code. YAML-driven campaigns mean anyone can build worlds. That's not just a feature. That's a foundation for something bigger."

## Slide 6 (10 seconds)
"Fully tested, clean architecture, built to last."

## Slide 7 (15 seconds)
"The vision? A community marketplace where creators publish campaigns and players find their next adventure. QuestKeeper becomes the engine. The community builds the content."

## Slide 8 (10 seconds)
"I built this because I believe great adventure stories should be portable and on demand. Questions?"

---

# Design Tips

1. **Dark theme** - Matches the terminal aesthetic
2. **Monospace font** - For code/YAML snippets (Fira Code, JetBrains Mono)
3. **Minimal text** - You narrate, slides support
4. **Screenshots** - Real terminal screenshots are powerful
5. **One idea per slide** - Don't overwhelm
6. **QR code on final slide** - Easy GitHub access

---

# Color Palette (matches game)

| Element | Hex | Use |
|---------|-----|-----|
| Background | #1a1a2e | Slide background |
| Yellow | #f4d03f | Titles, highlights |
| Cyan | #5dade2 | Subheadings, accents |
| Green | #58d68d | Success, positive |
| Red | #ec7063 | Warnings, emphasis |
| White | #ecf0f1 | Body text |
