package com.questkeeper.inventory.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.questkeeper.character.Character;
import com.questkeeper.inventory.items.effects.*;
import com.questkeeper.inventory.Item;

/**
 * Represents a magic item with one or more magical effects.
 * 
 * Magic items can require attunement and may have multiple effects
 * that activate on use or provide passive bonuses.
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class MagicItem extends Item {

    private static final int MAX_ATTUNEMENT_SLOTS = 3; // D&D 5e standard

    private final List<ItemEffect> effects;
    private boolean requiresAttunement;
    private boolean attuned;
    private String attunedToId;         // Character ID who attuned
    private String attunedToName;       // Character name for display
    private String attunementRequirement; // e.g., "spellcaster", "cleric", null for any

    public MagicItem(String name, String description, double weight, int goldValue, Rarity rarity) {
        super(name, ItemType.MAGIC_ITEM, description, weight, goldValue);
        setRarity(rarity);
        this.effects = new ArrayList<>();
        this.requiresAttunement = false;
        this.attuned = false;
        this.attunedToId = null;
        this.attunedToName = null;
        this.attunementRequirement = null;
    }

    public MagicItem(String name, String description, double weight, int goldValue, Rarity rarity, ItemEffect effect) {
        this(name, description, weight, goldValue, rarity);
        if (effect != null) {
            this.effects.add(effect);
        }
    }

    public MagicItem(String name, String description, double weight, int goldValue, Rarity rarity, List<ItemEffect> effects) {
        this(name, description, weight, goldValue, rarity);
        if (effects != null) {
            this.effects.addAll(effects);
        }
    }

    