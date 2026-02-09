package com.questkeeper.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the YamlUtils utility class.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("YamlUtils")
class YamlUtilsTest {

    // ==================== getString Tests ====================

    @Nested
    @DisplayName("getString")
    class GetStringTests {

        @Test
        @DisplayName("returns value when present")
        void returnsValueWhenPresent() {
            Map<String, Object> data = Map.of("name", "Longsword");

            assertEquals("Longsword", YamlUtils.getString(data, "name", "default"));
        }

        @Test
        @DisplayName("returns default when key missing")
        void returnsDefaultWhenKeyMissing() {
            Map<String, Object> data = Map.of("name", "Longsword");

            assertEquals("default", YamlUtils.getString(data, "missing", "default"));
        }

        @Test
        @DisplayName("returns default when value is null")
        void returnsDefaultWhenValueNull() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", null);

            assertEquals("default", YamlUtils.getString(data, "name", "default"));
        }

        @Test
        @DisplayName("returns default when map is null")
        void returnsDefaultWhenMapNull() {
            assertEquals("default", YamlUtils.getString(null, "name", "default"));
        }

        @Test
        @DisplayName("converts non-string values to string")
        void convertsNonStringValues() {
            Map<String, Object> data = Map.of("count", 42, "flag", true);

            assertEquals("42", YamlUtils.getString(data, "count", ""));
            assertEquals("true", YamlUtils.getString(data, "flag", ""));
        }

        @Test
        @DisplayName("no-default version returns empty string")
        void noDefaultReturnsEmptyString() {
            Map<String, Object> data = Map.of("name", "Sword");

            assertEquals("Sword", YamlUtils.getString(data, "name"));
            assertEquals("", YamlUtils.getString(data, "missing"));
        }
    }

    // ==================== getInt Tests ====================

    @Nested
    @DisplayName("getInt")
    class GetIntTests {

        @Test
        @DisplayName("returns int value when present")
        void returnsIntValueWhenPresent() {
            Map<String, Object> data = Map.of("damage", 8);

            assertEquals(8, YamlUtils.getInt(data, "damage", 0));
        }

        @Test
        @DisplayName("handles double values")
        void handlesDoubleValues() {
            Map<String, Object> data = Map.of("weight", 3.5);

            assertEquals(3, YamlUtils.getInt(data, "weight", 0));
        }

        @Test
        @DisplayName("returns default when key missing")
        void returnsDefaultWhenKeyMissing() {
            Map<String, Object> data = Map.of("damage", 8);

            assertEquals(10, YamlUtils.getInt(data, "missing", 10));
        }

        @Test
        @DisplayName("returns default when value is not a number")
        void returnsDefaultWhenNotNumber() {
            Map<String, Object> data = Map.of("damage", "eight");

            assertEquals(0, YamlUtils.getInt(data, "damage", 0));
        }

        @Test
        @DisplayName("returns default when map is null")
        void returnsDefaultWhenMapNull() {
            assertEquals(5, YamlUtils.getInt(null, "damage", 5));
        }
    }

    // ==================== getDouble Tests ====================

    @Nested
    @DisplayName("getDouble")
    class GetDoubleTests {

        @Test
        @DisplayName("returns double value when present")
        void returnsDoubleValueWhenPresent() {
            Map<String, Object> data = Map.of("weight", 3.5);

            assertEquals(3.5, YamlUtils.getDouble(data, "weight", 0.0), 0.001);
        }

        @Test
        @DisplayName("handles int values")
        void handlesIntValues() {
            Map<String, Object> data = Map.of("weight", 3);

            assertEquals(3.0, YamlUtils.getDouble(data, "weight", 0.0), 0.001);
        }

        @Test
        @DisplayName("returns default when key missing")
        void returnsDefaultWhenKeyMissing() {
            Map<String, Object> data = Map.of("weight", 3.5);

            assertEquals(1.0, YamlUtils.getDouble(data, "missing", 1.0), 0.001);
        }

        @Test
        @DisplayName("returns default when value is not a number")
        void returnsDefaultWhenNotNumber() {
            Map<String, Object> data = Map.of("weight", "heavy");

            assertEquals(0.0, YamlUtils.getDouble(data, "weight", 0.0), 0.001);
        }

        @Test
        @DisplayName("returns default when map is null")
        void returnsDefaultWhenMapNull() {
            assertEquals(2.5, YamlUtils.getDouble(null, "weight", 2.5), 0.001);
        }
    }

    // ==================== getBoolean Tests ====================

    @Nested
    @DisplayName("getBoolean")
    class GetBooleanTests {

        @Test
        @DisplayName("returns true when present")
        void returnsTrueWhenPresent() {
            Map<String, Object> data = Map.of("enabled", true);

            assertTrue(YamlUtils.getBoolean(data, "enabled", false));
        }

        @Test
        @DisplayName("returns false when present")
        void returnsFalseWhenPresent() {
            Map<String, Object> data = Map.of("enabled", false);

            assertFalse(YamlUtils.getBoolean(data, "enabled", true));
        }

        @Test
        @DisplayName("returns default when key missing")
        void returnsDefaultWhenKeyMissing() {
            Map<String, Object> data = Map.of("enabled", true);

            assertTrue(YamlUtils.getBoolean(data, "missing", true));
            assertFalse(YamlUtils.getBoolean(data, "missing", false));
        }

        @Test
        @DisplayName("returns default when value is not boolean")
        void returnsDefaultWhenNotBoolean() {
            Map<String, Object> data = Map.of("enabled", "yes");

            assertTrue(YamlUtils.getBoolean(data, "enabled", true));
        }

        @Test
        @DisplayName("returns default when map is null")
        void returnsDefaultWhenMapNull() {
            assertTrue(YamlUtils.getBoolean(null, "enabled", true));
        }
    }

    // ==================== getListOfMaps Tests ====================

    @Nested
    @DisplayName("getListOfMaps")
    class GetListOfMapsTests {

        @Test
        @DisplayName("returns list of maps when present")
        void returnsListOfMapsWhenPresent() {
            Map<String, Object> item1 = Map.of("id", "sword", "damage", 8);
            Map<String, Object> item2 = Map.of("id", "dagger", "damage", 4);
            Map<String, Object> data = Map.of("weapons", List.of(item1, item2));

            List<Map<String, Object>> result = YamlUtils.getListOfMaps(data, "weapons");

            assertEquals(2, result.size());
            assertEquals("sword", result.get(0).get("id"));
            assertEquals("dagger", result.get(1).get("id"));
        }

        @Test
        @DisplayName("returns empty list when key missing")
        void returnsEmptyListWhenKeyMissing() {
            Map<String, Object> data = Map.of("weapons", List.of());

            List<Map<String, Object>> result = YamlUtils.getListOfMaps(data, "missing");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list when value is not a list")
        void returnsEmptyListWhenNotList() {
            Map<String, Object> data = Map.of("weapons", "not a list");

            List<Map<String, Object>> result = YamlUtils.getListOfMaps(data, "weapons");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list when map is null")
        void returnsEmptyListWhenMapNull() {
            List<Map<String, Object>> result = YamlUtils.getListOfMaps(null, "weapons");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("skips non-map items in list")
        void skipsNonMapItems() {
            Map<String, Object> item = Map.of("id", "sword");
            Map<String, Object> data = Map.of("weapons", List.of(item, "not a map", 123));

            List<Map<String, Object>> result = YamlUtils.getListOfMaps(data, "weapons");

            assertEquals(1, result.size());
            assertEquals("sword", result.get(0).get("id"));
        }
    }

    // ==================== getStringList Tests ====================

    @Nested
    @DisplayName("getStringList")
    class GetStringListTests {

        @Test
        @DisplayName("returns list of strings when present")
        void returnsListOfStringsWhenPresent() {
            Map<String, Object> data = Map.of("properties", List.of("FINESSE", "LIGHT", "THROWN"));

            List<String> result = YamlUtils.getStringList(data, "properties");

            assertEquals(3, result.size());
            assertTrue(result.contains("FINESSE"));
            assertTrue(result.contains("LIGHT"));
            assertTrue(result.contains("THROWN"));
        }

        @Test
        @DisplayName("converts non-string items to strings")
        void convertsNonStringItems() {
            Map<String, Object> data = Map.of("values", List.of(1, 2, 3));

            List<String> result = YamlUtils.getStringList(data, "values");

            assertEquals(3, result.size());
            assertEquals("1", result.get(0));
            assertEquals("2", result.get(1));
            assertEquals("3", result.get(2));
        }

        @Test
        @DisplayName("returns empty list when key missing")
        void returnsEmptyListWhenKeyMissing() {
            Map<String, Object> data = Map.of("other", "value");

            List<String> result = YamlUtils.getStringList(data, "properties");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list when value is not a list")
        void returnsEmptyListWhenNotList() {
            Map<String, Object> data = Map.of("properties", "FINESSE");

            List<String> result = YamlUtils.getStringList(data, "properties");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list when map is null")
        void returnsEmptyListWhenMapNull() {
            List<String> result = YamlUtils.getStringList(null, "properties");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("skips null items in list")
        void skipsNullItems() {
            List<Object> listWithNull = new java.util.ArrayList<>();
            listWithNull.add("FINESSE");
            listWithNull.add(null);
            listWithNull.add("LIGHT");
            Map<String, Object> data = Map.of("properties", listWithNull);

            List<String> result = YamlUtils.getStringList(data, "properties");

            assertEquals(2, result.size());
            assertEquals("FINESSE", result.get(0));
            assertEquals("LIGHT", result.get(1));
        }
    }

    // ==================== hasKey Tests ====================

    @Nested
    @DisplayName("hasKey")
    class HasKeyTests {

        @Test
        @DisplayName("returns true when key exists with non-null value")
        void returnsTrueWhenKeyExists() {
            Map<String, Object> data = Map.of("name", "Sword");

            assertTrue(YamlUtils.hasKey(data, "name"));
        }

        @Test
        @DisplayName("returns false when key missing")
        void returnsFalseWhenKeyMissing() {
            Map<String, Object> data = Map.of("name", "Sword");

            assertFalse(YamlUtils.hasKey(data, "missing"));
        }

        @Test
        @DisplayName("returns false when value is null")
        void returnsFalseWhenValueNull() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", null);

            assertFalse(YamlUtils.hasKey(data, "name"));
        }

        @Test
        @DisplayName("returns false when map is null")
        void returnsFalseWhenMapNull() {
            assertFalse(YamlUtils.hasKey(null, "name"));
        }
    }
}
