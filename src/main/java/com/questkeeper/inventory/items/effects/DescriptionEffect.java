package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;

/**
 * A simple effect that displays a description when activated.
 * Used for YAML-defined items with effect_description fields.
 *
 * @author Marc McGough
 * @version 1.0
 */
public class DescriptionEffect extends AbstractItemEffect {

    private final String activationMessage;

    /**
     * Creates a description-based effect.
     *
     * @param id Unique effect ID
     * @param name Display name of the effect
     * @param description What the effect does
     * @param usageType How often it can be used
     * @param maxCharges Number of charges (-1 for unlimited)
     */
    public DescriptionEffect(String id, String name, String description,
                             UsageType usageType, int maxCharges) {
        super(id, name, description, usageType, maxCharges);
        this.activationMessage = description;
    }

    /**
     * Creates a description-based effect with a custom activation message.
     */
    public DescriptionEffect(String id, String name, String description,
                             UsageType usageType, int maxCharges, String activationMessage) {
        super(id, name, description, usageType, maxCharges);
        this.activationMessage = activationMessage != null ? activationMessage : description;
    }

    @Override
    protected String applyEffect(Character user) {
        return activationMessage;
    }

    /**
     * Creates a DescriptionEffect from YAML fields.
     *
     * @param id Item ID to use as effect ID base
     * @param name Item name
     * @param effectDescription The effect_description from YAML
     * @param usageTypeStr The usage_type from YAML (LONG_REST, DAILY, etc.)
     * @param charges Number of charges from YAML
     * @return A configured DescriptionEffect
     */
    public static DescriptionEffect fromYaml(String id, String name, String effectDescription,
                                              String usageTypeStr, int charges) {
        UsageType usageType = parseUsageType(usageTypeStr);
        int maxCharges = (usageType == UsageType.UNLIMITED || usageType == UsageType.PASSIVE)
                         ? -1 : Math.max(1, charges);

        return new DescriptionEffect(
            id + "_effect",
            name,
            effectDescription,
            usageType,
            maxCharges
        );
    }

    private static UsageType parseUsageType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UsageType.DAILY;
        }

        return switch (typeStr.toUpperCase()) {
            case "LONG_REST" -> UsageType.LONG_REST;
            case "DAILY" -> UsageType.DAILY;
            case "CONSUMABLE" -> UsageType.CONSUMABLE;
            case "CHARGES" -> UsageType.CHARGES;
            case "PASSIVE" -> UsageType.PASSIVE;
            case "UNLIMITED" -> UsageType.UNLIMITED;
            default -> UsageType.DAILY;
        };
    }
}
