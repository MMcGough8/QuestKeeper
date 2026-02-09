package com.questkeeper.core.command;

import com.questkeeper.inventory.Armor;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Item;
import com.questkeeper.inventory.Weapon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.questkeeper.core.command.CommandUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CommandUtils.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("CommandUtils")
class CommandUtilsTest {

    @Nested
    @DisplayName("matchesTarget")
    class MatchesTargetTests {

        @Test
        @DisplayName("exact match returns true")
        void exactMatchReturnsTrue() {
            assertTrue(matchesTarget("longsword", "longsword"));
        }

        @Test
        @DisplayName("case insensitive match returns true")
        void caseInsensitiveMatchReturnsTrue() {
            assertTrue(matchesTarget("Longsword", "longsword"));
            assertTrue(matchesTarget("longsword", "LONGSWORD"));
        }

        @Test
        @DisplayName("partial match returns true")
        void partialMatchReturnsTrue() {
            assertTrue(matchesTarget("longsword", "long"));
            assertTrue(matchesTarget("longsword", "sword"));
        }

        @Test
        @DisplayName("underscore to space match returns true")
        void underscoreToSpaceMatchReturnsTrue() {
            assertTrue(matchesTarget("healing_potion", "healing potion"));
        }

        @Test
        @DisplayName("no match returns false")
        void noMatchReturnsFalse() {
            assertFalse(matchesTarget("longsword", "dagger"));
        }

        @Test
        @DisplayName("null inputs return false")
        void nullInputsReturnFalse() {
            assertFalse(matchesTarget(null, "test"));
            assertFalse(matchesTarget("test", null));
            assertFalse(matchesTarget(null, null));
        }
    }

    @Nested
    @DisplayName("parseSlotName")
    class ParseSlotNameTests {

        @Test
        @DisplayName("parses weapon slot names")
        void parsesWeaponSlotNames() {
            assertEquals(EquipmentSlot.MAIN_HAND, parseSlotName("weapon"));
            assertEquals(EquipmentSlot.MAIN_HAND, parseSlotName("main hand"));
            assertEquals(EquipmentSlot.MAIN_HAND, parseSlotName("mainhand"));
            assertEquals(EquipmentSlot.MAIN_HAND, parseSlotName("main"));
        }

        @Test
        @DisplayName("parses offhand slot names")
        void parsesOffhandSlotNames() {
            assertEquals(EquipmentSlot.OFF_HAND, parseSlotName("offhand"));
            assertEquals(EquipmentSlot.OFF_HAND, parseSlotName("off hand"));
            assertEquals(EquipmentSlot.OFF_HAND, parseSlotName("shield"));
        }

        @Test
        @DisplayName("parses armor slot names")
        void parsesArmorSlotNames() {
            assertEquals(EquipmentSlot.ARMOR, parseSlotName("armor"));
            assertEquals(EquipmentSlot.ARMOR, parseSlotName("body"));
            assertEquals(EquipmentSlot.ARMOR, parseSlotName("chest"));
        }

        @Test
        @DisplayName("parses head slot names")
        void parsesHeadSlotNames() {
            assertEquals(EquipmentSlot.HEAD, parseSlotName("head"));
            assertEquals(EquipmentSlot.HEAD, parseSlotName("helmet"));
            assertEquals(EquipmentSlot.HEAD, parseSlotName("helm"));
        }

        @Test
        @DisplayName("parses neck slot names")
        void parsesNeckSlotNames() {
            assertEquals(EquipmentSlot.NECK, parseSlotName("neck"));
            assertEquals(EquipmentSlot.NECK, parseSlotName("necklace"));
            assertEquals(EquipmentSlot.NECK, parseSlotName("amulet"));
        }

        @Test
        @DisplayName("parses ring slot names")
        void parsesRingSlotNames() {
            assertEquals(EquipmentSlot.RING_LEFT, parseSlotName("ring"));
            assertEquals(EquipmentSlot.RING_LEFT, parseSlotName("left ring"));
            assertEquals(EquipmentSlot.RING_RIGHT, parseSlotName("right ring"));
        }

        @Test
        @DisplayName("returns null for unknown slot")
        void returnsNullForUnknownSlot() {
            assertNull(parseSlotName("unknown"));
            assertNull(parseSlotName(""));
            assertNull(parseSlotName(null));
        }

        @Test
        @DisplayName("is case insensitive")
        void isCaseInsensitive() {
            assertEquals(EquipmentSlot.MAIN_HAND, parseSlotName("WEAPON"));
            assertEquals(EquipmentSlot.ARMOR, parseSlotName("ARMOR"));
        }
    }

    @Nested
    @DisplayName("getSlotForItem")
    class GetSlotForItemTests {

        @Test
        @DisplayName("weapon gets main hand slot")
        void weaponGetsMainHandSlot() {
            Weapon sword = Weapon.createLongsword();
            assertEquals(EquipmentSlot.MAIN_HAND, getSlotForItem(sword));
        }

        @Test
        @DisplayName("armor gets armor slot")
        void armorGetsArmorSlot() {
            Armor chainmail = Armor.createChainMail();
            assertEquals(EquipmentSlot.ARMOR, getSlotForItem(chainmail));
        }

        @Test
        @DisplayName("shield gets offhand slot")
        void shieldGetsOffhandSlot() {
            Armor shield = Armor.createShield();
            assertEquals(EquipmentSlot.OFF_HAND, getSlotForItem(shield));
        }

        @Test
        @DisplayName("generic item returns null")
        void genericItemReturnsNull() {
            Item potion = Item.createConsumable("Healing Potion", "Heals wounds", 0.5, 50);
            assertNull(getSlotForItem(potion));
        }
    }

    @Nested
    @DisplayName("getSlotNameForItem")
    class GetSlotNameForItemTests {

        @Test
        @DisplayName("weapon returns Main Hand")
        void weaponReturnsMainHand() {
            Weapon sword = Weapon.createLongsword();
            assertEquals("Main Hand", getSlotNameForItem(sword));
        }

        @Test
        @DisplayName("armor returns Armor")
        void armorReturnsArmor() {
            Armor chainmail = Armor.createChainMail();
            assertEquals("Armor", getSlotNameForItem(chainmail));
        }

        @Test
        @DisplayName("shield returns Off Hand")
        void shieldReturnsOffHand() {
            Armor shield = Armor.createShield();
            assertEquals("Off Hand", getSlotNameForItem(shield));
        }

        @Test
        @DisplayName("generic item returns Equipment")
        void genericItemReturnsEquipment() {
            Item potion = Item.createConsumable("Healing Potion", "Heals wounds", 0.5, 50);
            assertEquals("Equipment", getSlotNameForItem(potion));
        }
    }

    @Nested
    @DisplayName("expandDirection")
    class ExpandDirectionTests {

        @Test
        @DisplayName("expands cardinal directions")
        void expandsCardinalDirections() {
            assertEquals("north", expandDirection("n"));
            assertEquals("south", expandDirection("s"));
            assertEquals("east", expandDirection("e"));
            assertEquals("west", expandDirection("w"));
        }

        @Test
        @DisplayName("expands diagonal directions")
        void expandsDiagonalDirections() {
            assertEquals("northeast", expandDirection("ne"));
            assertEquals("northwest", expandDirection("nw"));
            assertEquals("southeast", expandDirection("se"));
            assertEquals("southwest", expandDirection("sw"));
        }

        @Test
        @DisplayName("expands vertical directions")
        void expandsVerticalDirections() {
            assertEquals("up", expandDirection("u"));
            assertEquals("down", expandDirection("d"));
        }

        @Test
        @DisplayName("returns full direction unchanged")
        void returnsFullDirectionUnchanged() {
            assertEquals("north", expandDirection("north"));
            assertEquals("southeast", expandDirection("southeast"));
        }

        @Test
        @DisplayName("handles null")
        void handlesNull() {
            assertNull(expandDirection(null));
        }
    }

    @Nested
    @DisplayName("isDirection")
    class IsDirectionTests {

        @Test
        @DisplayName("recognizes cardinal directions")
        void recognizesCardinalDirections() {
            assertTrue(isDirection("north"));
            assertTrue(isDirection("south"));
            assertTrue(isDirection("east"));
            assertTrue(isDirection("west"));
        }

        @Test
        @DisplayName("recognizes shorthand directions")
        void recognizesShorthandDirections() {
            assertTrue(isDirection("n"));
            assertTrue(isDirection("s"));
            assertTrue(isDirection("e"));
            assertTrue(isDirection("w"));
        }

        @Test
        @DisplayName("recognizes diagonal directions")
        void recognizesDiagonalDirections() {
            assertTrue(isDirection("northeast"));
            assertTrue(isDirection("ne"));
            assertTrue(isDirection("northwest"));
            assertTrue(isDirection("nw"));
        }

        @Test
        @DisplayName("recognizes vertical directions")
        void recognizesVerticalDirections() {
            assertTrue(isDirection("up"));
            assertTrue(isDirection("u"));
            assertTrue(isDirection("down"));
            assertTrue(isDirection("d"));
        }

        @Test
        @DisplayName("rejects non-directions")
        void rejectsNonDirections() {
            assertFalse(isDirection("look"));
            assertFalse(isDirection("attack"));
            assertFalse(isDirection(""));
            assertFalse(isDirection(null));
        }

        @Test
        @DisplayName("is case insensitive")
        void isCaseInsensitive() {
            assertTrue(isDirection("NORTH"));
            assertTrue(isDirection("North"));
            assertTrue(isDirection("N"));
        }
    }
}
