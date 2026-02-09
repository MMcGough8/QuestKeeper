package com.questkeeper.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class for safely extracting values from YAML-parsed maps.
 *
 * These methods handle null values and type mismatches gracefully,
 * returning sensible defaults when data is missing or malformed.
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class YamlUtils {

    private YamlUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts a String value from a YAML map.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @param defaultValue the value to return if key is missing or null
     * @return the String value, or defaultValue if not found
     */
    public static String getString(Map<String, Object> data, String key, String defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Extracts a String value from a YAML map, defaulting to empty string.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @return the String value, or empty string if not found
     */
    public static String getString(Map<String, Object> data, String key) {
        return getString(data, key, "");
    }

    /**
     * Extracts an int value from a YAML map.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @param defaultValue the value to return if key is missing or not a number
     * @return the int value, or defaultValue if not found or not a number
     */
    public static int getInt(Map<String, Object> data, String key, int defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Extracts a double value from a YAML map.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @param defaultValue the value to return if key is missing or not a number
     * @return the double value, or defaultValue if not found or not a number
     */
    public static double getDouble(Map<String, Object> data, String key, double defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Extracts a boolean value from a YAML map.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @param defaultValue the value to return if key is missing or not a boolean
     * @return the boolean value, or defaultValue if not found or not a boolean
     */
    public static boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Extracts a List of Maps from a YAML map.
     * This is commonly used for arrays of objects in YAML.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @return the list of maps, or empty list if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getListOfMaps(Map<String, Object> data, String key) {
        if (data == null) return Collections.emptyList();
        Object value = data.get(key);
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    // Verify all keys are strings
                    boolean allStrings = map.keySet().stream().allMatch(k -> k instanceof String);
                    if (allStrings) {
                        result.add((Map<String, Object>) map);
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Extracts a List of Strings from a YAML map.
     *
     * @param data the parsed YAML map
     * @param key the key to look up
     * @return the list of strings, or empty list if not found or wrong type
     */
    public static List<String> getStringList(Map<String, Object> data, String key) {
        if (data == null) return Collections.emptyList();
        Object value = data.get(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Checks if a key exists and has a non-null value in the map.
     *
     * @param data the parsed YAML map
     * @param key the key to check
     * @return true if the key exists and has a non-null value
     */
    public static boolean hasKey(Map<String, Object> data, String key) {
        return data != null && data.containsKey(key) && data.get(key) != null;
    }
}
