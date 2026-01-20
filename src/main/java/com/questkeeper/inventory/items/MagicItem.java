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

    /**
     * Private constructor for creating a MagicItem with an explicit ID.
     */
    private MagicItem(String id, String name, String description, double weight, int goldValue, Rarity rarity) {
        super(id, name, ItemType.MAGIC_ITEM, description, weight, goldValue);
        setRarity(rarity);
        this.effects = new ArrayList<>();
        this.requiresAttunement = false;
        this.attuned = false;
        this.attunedToId = null;
        this.attunedToName = null;
        this.attunementRequirement = null;
    }

    /**
     * Creates a MagicItem with an explicit ID (for YAML-loaded items).
     */
    public static MagicItem createWithId(String id, String name, String description,
                                          double weight, int goldValue, Rarity rarity) {
        return new MagicItem(id, name, description, weight, goldValue, rarity);
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

    public void addEffect(ItemEffect effect) {
        if (effect != null) {
            effects.add(effect);
        }
    }

    public boolean removeEffect(ItemEffect effect) {
        return effects.remove(effect);
    }

    public List<ItemEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public List<ItemEffect> getPassiveEffects() {
        return effects.stream()
                .filter(ItemEffect::isPassive)
                .collect(Collectors.toList());
    }

    public List<ItemEffect> getActiveEffects() {
        return effects.stream()
                .filter(e -> !e.isPassive())
                .collect(Collectors.toList());
    }

    public List<ItemEffect> getUsableEffects() {
        return effects.stream()
                .filter(e -> !e.isPassive() && e.isUsable())
                .collect(Collectors.toList());
    }

    public boolean hasEffects() {
        return !effects.isEmpty();
    }

     public int getEffectCount() {
        return effects.size();
    }

    public String use(Character user) {
        if (!canUse(user)) {
            throw new IllegalStateException(getCannotUseReason(user));
        }

        List<ItemEffect> usable = getUsableEffects();
        if (usable.isEmpty()) {
            return getName() + " has no usable effects right now.";
        }

        StringBuilder result = new StringBuilder();
        for (ItemEffect effect : usable) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(effect.use(user));
        }

        return result.toString();
    }

    public String useEffect(Character user, int effectIndex) {
        if (!canUse(user)) {
            throw new IllegalStateException(getCannotUseReason(user));
        }

        if (effectIndex < 0 || effectIndex >= effects.size()) {
            throw new IndexOutOfBoundsException("Invalid effect index: " + effectIndex);
        }

        ItemEffect effect = effects.get(effectIndex);
        if (effect.isPassive()) {
            return effect.getName() + " is a passive effect and doesn't need activation.";
        }
        if (!effect.isUsable()) {
            return effect.getName() + " cannot be used right now. " + effect.getChargeDisplay();
        }

        return effect.use(user);
    }

    public String useEffect(Character user, String effectName) {
        if (!canUse(user)) {
            throw new IllegalStateException(getCannotUseReason(user));
        }

        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).getName().equalsIgnoreCase(effectName)) {
                return useEffect(user, i);
            }
        }

        return "No effect named '" + effectName + "' found on " + getName() + ".";
    }

    public boolean canUse(Character user) {
        if (user == null) {
            return false;
        }
        if (requiresAttunement && !isAttunedTo(user)) {
            return false;
        }
        return true;
    }

    public String getCannotUseReason(Character user) {
        if (user == null) {
            return "No user specified.";
        }
        if (requiresAttunement && !attuned) {
            return getName() + " requires attunement before it can be used.";
        }
        if (requiresAttunement && !isAttunedTo(user)) {
            return getName() + " is attuned to " + attunedToName + ", not " + user.getName() + ".";
        }
        return "";
    }

    public boolean requiresAttunement() {
        return requiresAttunement;
    }

    public void setRequiresAttunement(boolean requiresAttunement) {
        this.requiresAttunement = requiresAttunement;
    }

    public boolean isAttuned() {
        return attuned;
    }

    public boolean isAttunedTo(Character character) {
        if (character == null || !attuned) {
            return false;
        }
        return character.getName().equals(attunedToName);
    }

    public String getAttunedToName() {
        return attunedToName;
    }

    public String getAttunedToId() {
        return attunedToId;
    }

    public String attune(Character character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }

        if (!requiresAttunement) {
            return getName() + " doesn't require attunement.";
        }

        if (attuned && !isAttunedTo(character)) {
            throw new IllegalStateException(getName() + " is already attuned to " + attunedToName + ". Unattune first.");
        }

        if (isAttunedTo(character)) {
            return getName() + " is already attuned to " + character.getName() + ".";
        }

        // Check attunement requirements 
        if (attunementRequirement != null && !meetsAttunementRequirement(character)) {
            throw new IllegalStateException(getName() + " requires attunement by a " + attunementRequirement + ".");
        }

        this.attuned = true;
        this.attunedToName = character.getName();

        return character.getName() + " attunes to " + getName() + ". " + "Its magic resonates with their soul.";
    }

    public String unattune() {
        if (!attuned) {
            return getName() + " is not attuned to anyone.";
        }

        String previousOwner = attunedToName;
        this.attuned = false;
        this.attunedToId = null;
        this.attunedToName = null;

        return getName() + " is no longer attuned to " + previousOwner + ".";
    }

    public String getAttunementRequirement() {
        return attunementRequirement;
    }

    public void setAttunementRequirement(String requirement) {
        this.attunementRequirement = requirement;
    }

    protected boolean meetsAttunementRequirement(Character character) {
        if (attunementRequirement == null || attunementRequirement.isEmpty()) {
            return true;
        }

        String charClass = character.getCharacterClass().name().toLowerCase();
        String requirement = attunementRequirement.toLowerCase();

        if (requirement.equals("spellcaster")) {
        
            return charClass.equals("wizard") || charClass.equals("sorcerer") ||
                   charClass.equals("warlock") || charClass.equals("bard") ||
                   charClass.equals("cleric") || charClass.equals("druid") ||
                   charClass.equals("paladin") || charClass.equals("ranger");
        }
        
        return charClass.contains(requirement);
    }
    
    public void resetEffectsOnLongRest() {
        for (ItemEffect effect : effects) {
            effect.resetOnLongRest();
        }
    }

    public void resetEffectsDaily() {
        for (ItemEffect effect : effects) {
            effect.resetDaily();
        }
    }

    public boolean isFullyConsumed() {
        if (effects.isEmpty()) {
            return false;
        }
        return effects.stream()
                .filter(e -> e.getUsageType() == UsageType.CONSUMABLE)
                .allMatch(e -> !e.isUsable());
    }

    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        if (getRarity() != Rarity.COMMON) {
            sb.append(" [").append(getRarity().getDisplayName()).append("]");
        }
        sb.append("\n");

        sb.append("Magic Item\n");

        if (requiresAttunement) {
            sb.append("Requires Attunement");
            if (attunementRequirement != null) {
                sb.append(" (").append(attunementRequirement).append(")");
            }
            if (attuned) {
                sb.append(" - Attuned to ").append(attunedToName);
            }
            sb.append("\n");
        }

        if (!getDescription().isEmpty()) {
            sb.append(getDescription()).append("\n");
        }

        if (!effects.isEmpty()) {
            sb.append("\nEffects:\n");
            for (ItemEffect effect : effects) {
                sb.append("  • ").append(effect.getName());
                sb.append(" (").append(effect.getChargeDisplay()).append(")");
                sb.append("\n");
                sb.append("    ").append(effect.getDescription()).append("\n");
            }
        }

        sb.append(String.format("\nWeight: %.1f lb | Value: %d gp", getWeight(), getGoldValue()));

        return sb.toString();
    }

    @Override
    public String toString() {
        String attuneStr = requiresAttunement ? (attuned ? ", attuned" : ", requires attunement") : "";
        return String.format("%s [%s, %d effect(s)%s]",
                getName(), getRarity().getDisplayName(), effects.size(), attuneStr);
    }

    @Override
    public Item copy() {
        MagicItem copy = new MagicItem(getName(), getDescription(), getWeight(),
                getGoldValue(), getRarity());
        copy.requiresAttunement = this.requiresAttunement;
        copy.attunementRequirement = this.attunementRequirement;
        // Effects are shared (not deep copied) - they're stateful
        copy.effects.addAll(this.effects);
        return copy;
    }

    /**
     * Creates a +1 Weapon effect package (can be added to any weapon).
     */
    public static List<ItemEffect> createPlusOneWeaponEffects() {
        List<ItemEffect> effects = new ArrayList<>();
        effects.add(StatBonusEffect.createPlusOneWeaponAttack());
        effects.add(StatBonusEffect.createPlusOneWeaponDamage());
        return effects;
    }

    /**
     * Creates a Flame Tongue sword.
     */
    public static MagicItem createFlameTongue() {
        List<ItemEffect> effects = new ArrayList<>();
        effects.add(ExtraDamageEffect.createFlameTongue());
        effects.add(new UtilityEffect("flame_tongue_light", "Blazing Light", 
                UtilityEffect.UtilityType.LIGHT, 40));

        MagicItem item = new MagicItem(
            "Flame Tongue Longsword",
            "This sword is wreathed in flames when drawn. " +
            "Speaking its command word causes fire to erupt from the blade.",
            3.0, 5000, Rarity.RARE, effects
        );
        item.setRequiresAttunement(true);
        return item;
    }

    /**
     * Creates a Ring of Protection (+1 AC and saves).
     */
    public static MagicItem createRingOfProtection() {
        List<ItemEffect> effects = new ArrayList<>();
        effects.add(StatBonusEffect.createRingOfProtectionAC());
        effects.add(StatBonusEffect.createRingOfProtectionSaves());

        MagicItem item = new MagicItem(
            "Ring of Protection",
            "A silver ring inscribed with protective runes.",
            0.02, 3500, Rarity.RARE, effects
        );
        item.setRequiresAttunement(true);
        return item;
    }

    /**
     * Creates Gauntlets of Ogre Power (STR 19).
     */
    public static MagicItem createGauntletsOfOgrePower() {
        MagicItem item = new MagicItem(
            "Gauntlets of Ogre Power",
            "Heavy iron gauntlets etched with runes of strength.",
            1.0, 8000, Rarity.UNCOMMON,
            AbilitySetEffect.createGauntletsOfOgrePower()
        );
        item.setRequiresAttunement(true);
        return item;
    }

    /**
     * Creates a Potion of Healing.
     */
    public static MagicItem createPotionOfHealing() {
        MagicItem item = new MagicItem(
            "Potion of Healing",
            "A vial of red liquid that glimmers when agitated.",
            0.5, 50, Rarity.COMMON,
            SpellEffect.createPotionOfHealing()
        );
        return item;
    }
}

