# QuestKeeper Demo Day Script

## Pre-Demo Checklist
- [ ] Terminal font size: 16pt+ (readable from back of room)
- [ ] Terminal width: 80+ columns
- [ ] Dark background, light text
- [ ] Close all other applications
- [ ] Silence phone/notifications
- [ ] Have backup video ready

---

## The Pitch (60 seconds)

> Speak while terminal is ready but not yet running

RPG fans want meaningful adventures they can play anywhere, anytime.

But mobile games are shallow, and tabletop requires coordination.

**[pause]**

QuestKeeper fills that gap.

A fantasy adventure with real RPG mechanics that runs offline and on any device.

Real choices. Real consequences. Real imagination.

**[pause]**

What makes QuestKeeper different?

Content creation requires zero code.

YAML-driven campaigns mean anyone can build worlds.

That's not just a feature. That's a foundation for something bigger.

Fully tested, clean architecture, built to last.

**[pause]**

The vision?

A community marketplace where creators publish campaigns and players find their next adventure.

QuestKeeper becomes the engine.

The community builds the content.

**[pause]**

I built this because I believe great adventure stories should be portable and on demand.

Let me show you what that looks like.

---

## The Demo (2-3 minutes)

### LAUNCH (10 seconds)
```bash
mvn exec:java -Dexec.mainClass="com.questkeeper.Main" -q
```

> Point to title screen

"A terminal-based fantasy adventure with real RPG mechanics that runs offline on any device."

### PRESS ENTER → Campaign Selection
> Select Muddlebrook (option 1)

"Three complete campaigns. Today I'll show you Muddlebrook - a comedic mystery."

### CHARACTER CREATION (30 seconds)
> Create quickly - don't dwell

```
Name: Demo Hero
Race: 2 (Human - "versatile, relatable")
Class: 3 (Rogue - "good for skill checks")
```

> Point to ability scores

"Real D&D-style character creation. These stats matter."

### CAMPAIGN INTRO (20 seconds)
> Let the intro text display

"Notice the narrative presentation - blockquote styling, theatrical pacing. This isn't just text dump, it's storytelling."

### FIRST LOCATION (30 seconds)
> Point to each UI element as you mention it

"Here's the game loop. Status panel shows HP, level, trial progress. The mini-map shows where you are. Narrative describes the scene. NPCs are listed. Exits show where you can go."

### TALK TO NPC (30 seconds)
```
talk norrin
```

> Show dialogue

"Dialogue system with personality. Each NPC has voice, topics, relationships."

```
ask about harlequin
```

> Show response

"NPCs remember context, reveal information based on trust."

```
bye
```

### EXPLORE (20 seconds)
```
go north
```

> Show new location with map updating

"Map updates to show your position. New location, new narrative."

### SKILL CHECK MOMENT (30 seconds)
```
go north
```
```
trial
```

> If you reach a trial location, start it

"Now the RPG mechanics shine. Real dice rolls, real consequences."

> Point to the [ROLL] display

"d20 plus modifier versus difficulty. Success or failure affects the story."

### THE VISION (20 seconds)
> Can show this while game is still visible

"What makes this different? Content creation requires zero code."

> If time, mention:

"YAML-driven campaigns mean anyone can build worlds. That's not a feature - that's a platform. The vision is a community marketplace where creators publish campaigns and players find their next adventure."

### CLOSE
"The code is built to last. Fully tested, clean architecture, designed for maintainability."

"Questions?"

---

## Recovery Commands

If something goes wrong:

| Problem | Solution |
|---------|----------|
| Stuck in dialogue | Type `bye` |
| Lost | Type `look` |
| Need to restart | `Ctrl+C`, rerun maven command |
| Game crashes | Switch to backup video |

---

## Backup Save File

Create a save at a good demo point:
```
save
```

Load it if you need to skip ahead:
```
load
```

---

## Key Points to Hit

1. **Visual Polish** - Title screen, status panel, maps
2. **Real Mechanics** - Dice rolls, skill checks, consequences
3. **Narrative Quality** - Blockquotes, NPC dialogue, atmosphere
4. **Technical Excellence** - 1,900 tests, clean architecture
5. **Platform Vision** - YAML content, community potential
