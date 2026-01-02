package com.questkeeper.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.questkeeper.inventory.Item.ItemType;
import com.questkeeper.inventory.Item.Rarity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Item class.
 * 
 * Tests cover item creation, properties, stacking, copying,
 * and factory methods.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Item")
class ItemTest {

    private Item basicItem;
    private Item fullItem;

    @BeforeEach
    void setUp() {
        basicItem = new Item("Test Item", ItemType.MISCELLANEOUS);
        fullItem = new Item("Full Item", ItemType.TOOL, "A detailed description", 2.5, 50);
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructors")
    class ConstructorTests {

        @Test
        @DisplayName("Basic constructor sets name and type")
        void basicConstructorSetsNameAndType() {
            Item item = new Item("Simple Item", ItemType.TREASURE);
            
            assertEquals("Simple Item", item.getName());
            assertEquals(ItemType.TREASURE, item.getType());
        }

        @Test
        @DisplayName("Basic constructor sets defaults")
        void basicConstructorSetsDefaults() {
            Item item = new Item("Simple Item", ItemType.TREASURE);
            
            assertEquals("", item.getDescription());
            assertEquals(0.0, item.getWeight());
            assertEquals(0, item.getGoldValue());
            assertEquals(Rarity.COMMON, item.getRarity());
            assertFalse(item.isStackable());
            assertFalse(item.isQuestItem());
        }

        @Test
        @DisplayName("Standard constructor sets all properties")
        void standardConstructorSetsAllProperties() {
            Item item = new Item("Gold Ring", ItemType.TREASURE, "A shiny gold ring", 0.1, 25);
            
            assertEquals("Gold Ring", item.getName());
            assertEquals(ItemType.TREASURE, item.getType());
            assertEquals("A shiny gold ring", item.getDescription());
            assertEquals(0.1, item.getWeight(), 0.001);
            assertEquals(25, item.getGoldValue());
        }

        @Test
        @DisplayName("Full constructor sets rarity and stackable")
        void fullConstructorSetsRarityAndStackable() {
            Item item = new Item("Magic Gem", ItemType.TREASURE, "Glowing gem", 
                    0.5, 100, Rarity.RARE, true);
            
            assertEquals(Rarity.RARE, item.getRarity());
            assertTrue(item.isStackable());
            assertEquals(99, item.getMaxStackSize()); // Default stackable max
        }

        @Test
        @DisplayName("Negative weight becomes zero")
        void negativeWeightBecomesZero() {
            Item item = new Item("Weightless", ItemType.MISCELLANEOUS, "", -5.0, 10);
            
            assertEquals(0.0, item.getWeight());
        }

        @Test
        @DisplayName("Negative gold value becomes zero")
        void negativeGoldValueBecomesZero() {
            Item item = new Item("Worthless", ItemType.MISCELLANEOUS, "", 1.0, -100);
            
            assertEquals(0, item.getGoldValue());
        }

        @Test
        @DisplayName("Each item gets unique ID")
        void eachItemGetsUniqueId() {
            Item item1 = new Item("Sword", ItemType.WEAPON);
            Item item2 = new Item("Sword", ItemType.WEAPON);
            
            assertNotNull(item1.getId());
            assertNotNull(item2.getId());
            assertNotEquals(item1.getId(), item2.getId());
        }

        @Test
        @DisplayName("ID contains item name")
        void idContainsItemName() {
            Item item = new Item("Magic Wand", ItemType.MAGIC_ITEM);
            
            assertTrue(item.getId().toLowerCase().contains("magic_wand"));
        }
    }

    // ==================== Getter/Setter Tests ====================

    @Nested
    @DisplayName("Getters and Setters")
    class GetterSetterTests {

        @Test
        @DisplayName("Can set and get name")
        void canSetAndGetName() {
            basicItem.setName("New Name");
            assertEquals("New Name", basicItem.getName());
        }

        @Test
        @DisplayName("Can set and get description")
        void canSetAndGetDescription() {
            basicItem.setDescription("A new description");
            assertEquals("A new description", basicItem.getDescription());
        }

        @Test
        @DisplayName("Can set and get type")
        void canSetAndGetType() {
            basicItem.setType(ItemType.CONSUMABLE);
            assertEquals(ItemType.CONSUMABLE, basicItem.getType());
        }

        @Test
        @DisplayName("Can set and get rarity")
        void canSetAndGetRarity() {
            basicItem.setRarity(Rarity.LEGENDARY);
            assertEquals(Rarity.LEGENDARY, basicItem.getRarity());
        }

        @Test
        @DisplayName("Can set and get weight")
        void canSetAndGetWeight() {
            basicItem.setWeight(5.5);
            assertEquals(5.5, basicItem.getWeight(), 0.001);
        }

        @Test
        @DisplayName("Setting negative weight clamps to zero")
        void settingNegativeWeightClampsToZero() {
            basicItem.setWeight(-10.0);
            assertEquals(0.0, basicItem.getWeight());
        }

        @Test
        @DisplayName("Can set and get gold value")
        void canSetAndGetGoldValue() {
            basicItem.setGoldValue(100);
            assertEquals(100, basicItem.getGoldValue());
        }

        @Test
        @DisplayName("Setting negative gold value clamps to zero")
        void settingNegativeGoldValueClampsToZero() {
            basicItem.setGoldValue(-50);
            assertEquals(0, basicItem.getGoldValue());
        }
    }

    // ==================== Stacking Tests ====================

    @Nested
    @DisplayName("Stacking")
    class StackingTests {

        @Test
        @DisplayName("Items are not stackable by default")
        void itemsAreNotStackableByDefault() {
            assertFalse(basicItem.isStackable());
            assertEquals(1, basicItem.getMaxStackSize());
        }

        @Test
        @DisplayName("Can make item stackable")
        void canMakeItemStackable() {
            basicItem.setStackable(true);
            
            assertTrue(basicItem.isStackable());
            assertEquals(99, basicItem.getMaxStackSize());
        }

        @Test
        @DisplayName("Making item non-stackable resets max stack size")
        void makingItemNonStackableResetsMaxStackSize() {
            basicItem.setStackable(true);
            basicItem.setStackable(false);
            
            assertFalse(basicItem.isStackable());
            assertEquals(1, basicItem.getMaxStackSize());
        }

        @Test
        @DisplayName("Can set custom max stack size")
        void canSetCustomMaxStackSize() {
            basicItem.setStackable(true);
            basicItem.setMaxStackSize(20);
            
            assertEquals(20, basicItem.getMaxStackSize());
        }

        @Test
        @DisplayName("Max stack size minimum is 1")
        void maxStackSizeMinimumIsOne() {
            basicItem.setMaxStackSize(0);
            assertEquals(1, basicItem.getMaxStackSize());
            
            basicItem.setMaxStackSize(-5);
            assertEquals(1, basicItem.getMaxStackSize());
        }
    }

    // ==================== Quest Item Tests ====================

    @Nested
    @DisplayName("Quest Items")
    class QuestItemTests {

        @Test
        @DisplayName("Items are not quest items by default")
        void itemsAreNotQuestItemsByDefault() {
            assertFalse(basicItem.isQuestItem());
        }

        @Test
        @DisplayName("Can set item as quest item")
        void canSetItemAsQuestItem() {
            basicItem.setQuestItem(true);
            assertTrue(basicItem.isQuestItem());
        }

        @Test
        @DisplayName("Quest items cannot be dropped")
        void questItemsCannotBeDropped() {
            basicItem.setQuestItem(true);
            assertFalse(basicItem.isDroppable());
        }

        @Test
        @DisplayName("Quest items cannot be sold")
        void questItemsCannotBeSold() {
            basicItem.setQuestItem(true);
            basicItem.setGoldValue(100);
            assertFalse(basicItem.isSellable());
        }

        @Test
        @DisplayName("Non-quest items can be dropped")
        void nonQuestItemsCanBeDropped() {
            assertTrue(basicItem.isDroppable());
        }

        @Test
        @DisplayName("Items with value can be sold")
        void itemsWithValueCanBeSold() {
            basicItem.setGoldValue(50);
            assertTrue(basicItem.isSellable());
        }

        @Test
        @DisplayName("Items with zero value cannot be sold")
        void itemsWithZeroValueCannotBeSold() {
            basicItem.setGoldValue(0);
            assertFalse(basicItem.isSellable());
        }
    }

    // ==================== Equippable Tests ====================

    @Nested
    @DisplayName("Equippable")
    class EquippableTests {

        @Test
        @DisplayName("Weapon type is equippable")
        void weaponTypeIsEquippable() {
            Item item = new Item("Sword", ItemType.WEAPON);
            assertTrue(item.isEquippable());
        }

        @Test
        @DisplayName("Armor type is equippable")
        void armorTypeIsEquippable() {
            Item item = new Item("Chainmail", ItemType.ARMOR);
            assertTrue(item.isEquippable());
        }

        @Test
        @DisplayName("Shield type is equippable")
        void shieldTypeIsEquippable() {
            Item item = new Item("Buckler", ItemType.SHIELD);
            assertTrue(item.isEquippable());
        }

        @Test
        @DisplayName("Consumable type is not equippable")
        void consumableTypeIsNotEquippable() {
            Item item = new Item("Potion", ItemType.CONSUMABLE);
            assertFalse(item.isEquippable());
        }

        @Test
        @DisplayName("Tool type is not equippable")
        void toolTypeIsNotEquippable() {
            Item item = new Item("Hammer", ItemType.TOOL);
            assertFalse(item.isEquippable());
        }

        @Test
        @DisplayName("Miscellaneous type is not equippable")
        void miscellaneousTypeIsNotEquippable() {
            assertFalse(basicItem.isEquippable());
        }
    }

    // ==================== Copy Tests ====================

    @Nested
    @DisplayName("Copying")
    class CopyTests {

        @Test
        @DisplayName("Copy preserves ID")
        void copyPreservesId() {
            Item copy = fullItem.copy();
            assertEquals(fullItem.getId(), copy.getId());
        }

        @Test
        @DisplayName("Copy preserves all properties")
        void copyPreservesAllProperties() {
            fullItem.setRarity(Rarity.RARE);
            fullItem.setStackable(true);
            fullItem.setQuestItem(true);
            
            Item copy = fullItem.copy();
            
            assertEquals(fullItem.getName(), copy.getName());
            assertEquals(fullItem.getType(), copy.getType());
            assertEquals(fullItem.getDescription(), copy.getDescription());
            assertEquals(fullItem.getWeight(), copy.getWeight());
            assertEquals(fullItem.getGoldValue(), copy.getGoldValue());
            assertEquals(fullItem.getRarity(), copy.getRarity());
            assertEquals(fullItem.isStackable(), copy.isStackable());
            assertEquals(fullItem.isQuestItem(), copy.isQuestItem());
        }

        @Test
        @DisplayName("Copy is independent object")
        void copyIsIndependentObject() {
            Item copy = fullItem.copy();
            
            copy.setName("Modified Name");
            copy.setGoldValue(999);
            
            assertNotEquals(fullItem.getName(), copy.getName());
            assertNotEquals(fullItem.getGoldValue(), copy.getGoldValue());
        }

        @Test
        @DisplayName("CopyWithNewId creates new ID")
        void copyWithNewIdCreatesNewId() {
            Item copy = fullItem.copyWithNewId();
            
            assertNotEquals(fullItem.getId(), copy.getId());
        }

        @Test
        @DisplayName("CopyWithNewId preserves other properties")
        void copyWithNewIdPreservesOtherProperties() {
            fullItem.setRarity(Rarity.VERY_RARE);
            
            Item copy = fullItem.copyWithNewId();
            
            assertEquals(fullItem.getName(), copy.getName());
            assertEquals(fullItem.getType(), copy.getType());
            assertEquals(fullItem.getRarity(), copy.getRarity());
        }
    }

    // ==================== Display Tests ====================

    @Nested
    @DisplayName("Display Methods")
    class DisplayTests {

        @Test
        @DisplayName("GetDisplayName shows name for common items")
        void getDisplayNameShowsNameForCommonItems() {
            assertEquals("Test Item", basicItem.getDisplayName());
        }

        @Test
        @DisplayName("GetDisplayName includes rarity for non-common items")
        void getDisplayNameIncludesRarityForNonCommonItems() {
            basicItem.setRarity(Rarity.RARE);
            
            String displayName = basicItem.getDisplayName();
            
            assertTrue(displayName.contains("Test Item"));
            assertTrue(displayName.contains("Rare"));
        }

        @Test
        @DisplayName("GetDetailedInfo includes all relevant info")
        void getDetailedInfoIncludesAllRelevantInfo() {
            fullItem.setRarity(Rarity.UNCOMMON);
            
            String info = fullItem.getDetailedInfo();
            
            assertTrue(info.contains("Full Item"));
            assertTrue(info.contains("Uncommon"));
            assertTrue(info.contains("Tool"));
            assertTrue(info.contains("A detailed description"));
            assertTrue(info.contains("2.5"));
            assertTrue(info.contains("50"));
        }

        @Test
        @DisplayName("GetDetailedInfo shows quest item label")
        void getDetailedInfoShowsQuestItemLabel() {
            basicItem.setQuestItem(true);
            
            String info = basicItem.getDetailedInfo();
            
            assertTrue(info.contains("Quest Item"));
        }

        @Test
        @DisplayName("ToString provides summary")
        void toStringProvidesSummary() {
            String str = fullItem.toString();
            
            assertTrue(str.contains("Full Item"));
            assertTrue(str.contains("Tool"));
            assertTrue(str.contains("2.5"));
            assertTrue(str.contains("50"));
        }
    }

    // ==================== Equals/HashCode Tests ====================

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Same item equals itself")
        void sameItemEqualsItself() {
            assertEquals(basicItem, basicItem);
        }

        @Test
        @DisplayName("Items with same ID are equal")
        void itemsWithSameIdAreEqual() {
            Item copy = basicItem.copy();
            assertEquals(basicItem, copy);
        }

        @Test
        @DisplayName("Items with different IDs are not equal")
        void itemsWithDifferentIdsAreNotEqual() {
            Item other = new Item("Test Item", ItemType.MISCELLANEOUS);
            assertNotEquals(basicItem, other);
        }

        @Test
        @DisplayName("Item does not equal null")
        void itemDoesNotEqualNull() {
            assertNotEquals(null, basicItem);
        }

        @Test
        @DisplayName("Item does not equal different type")
        void itemDoesNotEqualDifferentType() {
            assertNotEquals("not an item", basicItem);
        }

        @Test
        @DisplayName("Equal items have same hash code")
        void equalItemsHaveSameHashCode() {
            Item copy = basicItem.copy();
            assertEquals(basicItem.hashCode(), copy.hashCode());
        }
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("CreateConsumable creates stackable consumable")
        void createConsumableCreatesStackableConsumable() {
            Item potion = Item.createConsumable("Healing Potion", "Restores HP", 0.5, 50);
            
            assertEquals("Healing Potion", potion.getName());
            assertEquals(ItemType.CONSUMABLE, potion.getType());
            assertEquals("Restores HP", potion.getDescription());
            assertEquals(0.5, potion.getWeight(), 0.001);
            assertEquals(50, potion.getGoldValue());
            assertTrue(potion.isStackable());
        }

        @Test
        @DisplayName("CreateTreasure creates treasure with rarity")
        void createTreasureCreatesTreasureWithRarity() {
            Item gem = Item.createTreasure("Ruby", "A red gem", 0.1, 500, Rarity.RARE);
            
            assertEquals("Ruby", gem.getName());
            assertEquals(ItemType.TREASURE, gem.getType());
            assertEquals(Rarity.RARE, gem.getRarity());
            assertEquals(500, gem.getGoldValue());
        }

        @Test
        @DisplayName("CreateQuestItem creates non-droppable quest item")
        void createQuestItemCreatesNonDroppableQuestItem() {
            Item key = Item.createQuestItem("Ancient Key", "Opens the sealed door");
            
            assertEquals("Ancient Key", key.getName());
            assertEquals(ItemType.QUEST_ITEM, key.getType());
            assertTrue(key.isQuestItem());
            assertFalse(key.isDroppable());
            assertFalse(key.isSellable());
            assertEquals(0, key.getGoldValue());
            assertEquals(0.0, key.getWeight());
        }

        @Test
        @DisplayName("CreateTool creates tool item")
        void createToolCreatesToolItem() {
            Item tool = Item.createTool("Thieves' Tools", "For picking locks", 1.0, 25);
            
            assertEquals("Thieves' Tools", tool.getName());
            assertEquals(ItemType.TOOL, tool.getType());
            assertEquals(1.0, tool.getWeight(), 0.001);
            assertEquals(25, tool.getGoldValue());
        }
    }

    // ==================== ItemType Enum Tests ====================

    @Nested
    @DisplayName("ItemType Enum")
    class ItemTypeEnumTests {

        @Test
        @DisplayName("All item types have display names")
        void allItemTypesHaveDisplayNames() {
            for (ItemType type : ItemType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("Item types have correct display names")
        void itemTypesHaveCorrectDisplayNames() {
            assertEquals("Weapon", ItemType.WEAPON.getDisplayName());
            assertEquals("Armor", ItemType.ARMOR.getDisplayName());
            assertEquals("Magic Item", ItemType.MAGIC_ITEM.getDisplayName());
            assertEquals("Quest Item", ItemType.QUEST_ITEM.getDisplayName());
        }
    }

    // ==================== Rarity Enum Tests ====================

    @Nested
    @DisplayName("Rarity Enum")
    class RarityEnumTests {

        @Test
        @DisplayName("All rarities have display names")
        void allRaritiesHaveDisplayNames() {
            for (Rarity rarity : Rarity.values()) {
                assertNotNull(rarity.getDisplayName());
                assertFalse(rarity.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("All rarities have colors")
        void allRaritiesHaveColors() {
            for (Rarity rarity : Rarity.values()) {
                assertNotNull(rarity.getColor());
                assertFalse(rarity.getColor().isEmpty());
            }
        }

        @Test
        @DisplayName("Rarities have correct display names")
        void raritiesHaveCorrectDisplayNames() {
            assertEquals("Common", Rarity.COMMON.getDisplayName());
            assertEquals("Uncommon", Rarity.UNCOMMON.getDisplayName());
            assertEquals("Rare", Rarity.RARE.getDisplayName());
            assertEquals("Very Rare", Rarity.VERY_RARE.getDisplayName());
            assertEquals("Legendary", Rarity.LEGENDARY.getDisplayName());
            assertEquals("Artifact", Rarity.ARTIFACT.getDisplayName());
        }

        @Test
        @DisplayName("Rarities have expected colors")
        void raritiesHaveExpectedColors() {
            assertEquals("gray", Rarity.COMMON.getColor());
            assertEquals("green", Rarity.UNCOMMON.getColor());
            assertEquals("blue", Rarity.RARE.getColor());
            assertEquals("purple", Rarity.VERY_RARE.getColor());
            assertEquals("orange", Rarity.LEGENDARY.getColor());
            assertEquals("red", Rarity.ARTIFACT.getColor());
        }
    }
}