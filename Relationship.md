# QuestKeeper - Entity Relationship Diagram

## Core Entities and Relationships

This data model follows standard database design conventions with proper entity relationships, primary keys (underlined), and foreign keys.

---

## Entity Definitions

### **character**
- <u>character_id</u>
- name
- race
- class
- level
- max_hp
- current_hp
- armor_class
- strength
- dexterity
- constitution
- intelligence
- wisdom
- charisma
- created_at

### **game_state**
- <u>state_id</u>
- character_id (FK → character)
- campaign_id (FK → campaign)
- current_location_id (FK → location)
- last_save_time
- total_play_time

### **character_flags**
- <u>flag_id</u>
- character_id (FK → character)
- flag_name
- set_at

### **campaign**
- <u>campaign_id</u>
- name
- description
- tone
- recommended_level
- author

### **location**
- <u>location_id</u>
- campaign_id (FK → campaign)
- name
- description
- type
- is_safe_zone

### **connects_to**
- <u>connection_id</u>
- from_location_id (FK → location)
- to_location_id (FK → location)
- direction

### **npc**
- <u>npc_id</u>
- campaign_id (FK → campaign)
- name
- role
- voice_description
- behavior_description
- greeting_text

### **located_at**
- <u>presence_id</u>
- npc_id (FK → npc)
- location_id (FK → location)

### **dialogue_option**
- <u>dialogue_id</u>
- npc_id (FK → npc)
- topic
- response_text
- required_flag
- sets_flag

### **trial**
- <u>trial_id</u>
- campaign_id (FK → campaign)
- location_id (FK → location)
- name
- description
- difficulty

### **minigame**
- <u>minigame_id</u>
- trial_id (FK → trial)
- name
- description
- type
- required_skill
- difficulty_class
- allow_retry
- success_text
- failure_text
- failure_damage

### **item**
- <u>item_id</u>
- campaign_id (FK → campaign)
- name
- description
- type
- rarity
- is_consumable
- gold_value
- weight

### **rewards**
- <u>reward_id</u>
- minigame_id (FK → minigame)
- item_id (FK → item)

### **inventory**
- <u>inventory_id</u>
- character_id (FK → character)
- item_id (FK → item)
- quantity
- is_equipped
- acquired_at

### **item_effect**
- <u>effect_id</u>
- item_id (FK → item)
- effect_type
- effect_description
- uses_per_rest

### **encounter**
- <u>encounter_id</u>
- trial_id (FK → trial)
- name
- type
- difficulty
- experience_reward
- gold_reward

### **monster**
- <u>monster_id</u>
- campaign_id (FK → campaign)
- name
- type
- size
- challenge_rating
- armor_class
- max_hp
- hp_dice

### **appears_in**
- <u>appearance_id</u>
- encounter_id (FK → encounter)
- monster_id (FK → monster)
- count

### **quest**
- <u>quest_id</u>
- campaign_id (FK → campaign)
- name
- description
- type
- experience_reward
- gold_reward

### **quest_objective**
- <u>objective_id</u>
- quest_id (FK → quest)
- description
- required_flag
- sequence_order

### **active_quest**
- <u>active_id</u>
- character_id (FK → character)
- quest_id (FK → quest)
- started_at

### **completed_trial**
- <u>completion_id</u>
- character_id (FK → character)
- trial_id (FK → trial)
- completed_at

---

## Relationship Summary

### One-to-Many Relationships

**campaign → location**
- One campaign has many locations
- Each location belongs to one campaign

**campaign → npc**
- One campaign has many NPCs
- Each NPC belongs to one campaign

**campaign → trial**
- One campaign has many trials
- Each trial belongs to one campaign

**campaign → item**
- One campaign has many items
- Each item belongs to one campaign

**campaign → monster**
- One campaign has many monsters
- Each monster belongs to one campaign

**campaign → quest**
- One campaign has many quests
- Each quest belongs to one campaign

**character → game_state**
- One character has one game state
- Each game state belongs to one character

**character → character_flags**
- One character has many flags
- Each flag belongs to one character

**character → inventory**
- One character has many inventory items
- Each inventory entry belongs to one character

**character → active_quest**
- One character has many active quests
- Each active quest belongs to one character

**character → completed_trial**
- One character has many completed trials
- Each completion belongs to one character

**location → trial**
- One location may have one trial
- Each trial is at one location

**npc → dialogue_option**
- One NPC has many dialogue options
- Each dialogue option belongs to one NPC

**trial → minigame**
- One trial has many minigames
- Each minigame belongs to one trial

**trial → encounter**
- One trial may have one encounter
- Each encounter belongs to one trial

**item → item_effect**
- One item has many effects
- Each effect belongs to one item

**quest → quest_objective**
- One quest has many objectives
- Each objective belongs to one quest

### Many-to-Many Relationships (with join tables)

**location ↔ location (connects_to)**
- Locations connect to other locations via directional connections
- Join table: connects_to

**npc ↔ location (located_at)**
- NPCs can be present at multiple locations (at different times)
- Locations can have multiple NPCs
- Join table: located_at

**minigame ↔ item (rewards)**
- Minigames can reward multiple items
- Items can be rewarded by multiple minigames
- Join table: rewards

**character ↔ item (inventory)**
- Characters can have multiple items
- Items can be owned by multiple characters (different instances)
- Join table: inventory

**encounter ↔ monster (appears_in)**
- Encounters can have multiple monsters
- Monsters can appear in multiple encounters
- Join table: appears_in

**character ↔ quest (active_quest)**
- Characters can have multiple active quests
- Quests can be active for multiple characters
- Join table: active_quest

---

## Primary Keys and Foreign Keys Legend

- <u>Underlined attributes</u> = Primary Key
- (FK → table) = Foreign Key referencing another table
- All IDs are string UUIDs for flexibility

---

## Cardinality Notation

```
campaign (1) ──< (M) location
campaign (1) ──< (M) npc
campaign (1) ──< (M) trial
campaign (1) ──< (M) item
campaign (1) ──< (M) monster
campaign (1) ──< (M) quest

character (1) ──< (1) game_state
character (1) ──< (M) character_flags
character (1) ──< (M) inventory
character (1) ──< (M) active_quest
character (1) ──< (M) completed_trial

location (M) ──< connects_to >──< (M) location
location (1) ──< (0..1) trial

npc (1) ──< (M) dialogue_option
npc (M) ──< located_at >──< (M) location

trial (1) ──< (M) minigame
trial (1) ──< (0..1) encounter

minigame (M) ──< rewards >──< (M) item

item (1) ──< (M) item_effect
item (M) ──< inventory >──< (M) character

encounter (M) ──< appears_in >──< (M) monster

quest (1) ──< (M) quest_objective
quest (M) ──< active_quest >──< (M) character
```

---
