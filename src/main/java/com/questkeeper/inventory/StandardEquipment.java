package com.questkeeper.inventory;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and provides access to standard D&D 5e equipment from YAML.
 * This is a singleton that caches all standard weapons and armor.
 */
public class StandardEquipment {

    private static StandardEquipment instance;
    private final Map<String, Weapon> weapons = new HashMap<>();
    private final Map<String, Armor> armors = new HashMap<>();
    private boolean loaded = false;

    private StandardEquipment() {
        // Private constructor for singleton
    }

    public static synchronized StandardEquipment getInstance() {
        if (instance == null) {
            instance = new StandardEquipment();
            instance.load();
        }
        return instance;
    }

    private void load() {
        if (loaded) return;

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("items/standard_equipment.yaml")) {
            if (input == null) {
                System.err.println("Warning: standard_equipment.yaml not found");
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            loadWeapons(data);
            loadArmor(data);

            loaded = true;
        } catch (Exception e) {
            System.err.println("Error loading standard equipment: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadWeapons(Map<String, Object> data) {
        List<Map<String, Object>> weaponList = (List<Map<String, Object>>) data.get("weapons");
        if (weaponList == null) return;

        for (Map<String, Object> weaponData : weaponList) {
            try {
                String yamlId = getString(weaponData, "id");
                Weapon weapon = parseWeapon(weaponData);
                weapons.put(yamlId.toLowerCase(), weapon);
            } catch (Exception e) {
                System.err.println("Error parsing weapon: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadArmor(Map<String, Object> data) {
        List<Map<String, Object>> armorList = (List<Map<String, Object>>) data.get("armor");
        if (armorList == null) return;

        for (Map<String, Object> armorData : armorList) {
            try {
                String yamlId = getString(armorData, "id");
                Armor armor = parseArmor(armorData);
                armors.put(yamlId.toLowerCase(), armor);
            } catch (Exception e) {
                System.err.println("Error parsing armor: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Weapon parseWeapon(Map<String, Object> data) {
        String id = getString(data, "id");
        String name = getString(data, "name");
        int diceCount = getInt(data, "damage_dice_count", 1);
        int dieSize = getInt(data, "damage_die_size", 4);
        String damageTypeStr = getString(data, "damage_type", "BLUDGEONING");
        String categoryStr = getString(data, "category", "SIMPLE_MELEE");
        double weight = getDouble(data, "weight", 1.0);
        int value = getInt(data, "value", 0);

        Weapon.DamageType damageType = Weapon.DamageType.valueOf(damageTypeStr);
        Weapon.WeaponCategory category = Weapon.WeaponCategory.valueOf(categoryStr);

        // Use YAML ID so items can be saved/loaded correctly
        Weapon weapon = Weapon.createWithId(id, name, diceCount, dieSize, damageType, category, weight, value);

        // Set ranges if present
        if (data.containsKey("normal_range")) {
            weapon.setRange(getInt(data, "normal_range", 0), getInt(data, "long_range", 0));
        }

        // Set versatile die size if present
        if (data.containsKey("versatile_die_size")) {
            weapon.setVersatileDieSize(getInt(data, "versatile_die_size", 0));
        }

        // Add properties
        List<String> properties = (List<String>) data.get("properties");
        if (properties != null) {
            for (String prop : properties) {
                try {
                    weapon.addProperty(Weapon.WeaponProperty.valueOf(prop));
                } catch (IllegalArgumentException e) {
                    // Skip unknown properties
                }
            }
        }

        return weapon;
    }

    private Armor parseArmor(Map<String, Object> data) {
        String id = getString(data, "id");
        String name = getString(data, "name");
        int armorClass = getInt(data, "armor_class", 10);
        String armorTypeStr = getString(data, "armor_type", "LIGHT");
        double weight = getDouble(data, "weight", 0.0);
        int value = getInt(data, "value", 0);
        int strengthReq = getInt(data, "strength_requirement", 0);
        boolean stealthDisadv = getBoolean(data, "stealth_disadvantage", false);

        Armor.ArmorCategory category = Armor.ArmorCategory.valueOf(armorTypeStr);

        // Use YAML ID so items can be saved/loaded correctly
        Armor armor = Armor.createWithId(id, name, category, armorClass, weight, value);
        armor.setStrengthRequirement(strengthReq);
        armor.setStealthDisadvantage(stealthDisadv);
        return armor;
    }

    // ==========================================
    // Public Accessors
    // ==========================================

    public Weapon getWeapon(String id) {
        Weapon original = weapons.get(id.toLowerCase());
        if (original == null) return null;
        // Return a copy to prevent modification of cached items
        return (Weapon) original.copy();
    }

    public Armor getArmor(String id) {
        Armor original = armors.get(id.toLowerCase());
        if (original == null) return null;
        // Return a copy to prevent modification of cached items
        return (Armor) original.copy();
    }

    public Item getItem(String id) {
        String lowerId = id.toLowerCase();
        if (weapons.containsKey(lowerId)) {
            return getWeapon(lowerId);
        }
        if (armors.containsKey(lowerId)) {
            return getArmor(lowerId);
        }
        return null;
    }

    public boolean hasWeapon(String id) {
        return weapons.containsKey(id.toLowerCase());
    }

    public boolean hasArmor(String id) {
        return armors.containsKey(id.toLowerCase());
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private String getString(Map<String, Object> data, String key) {
        return getString(data, key, "");
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
