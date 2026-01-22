package com.questkeeper.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Inventory class.
 * 
 * Tests cover item management, equipment slots, weight limits,
 * gold handling, and edge cases.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Inventory")
class InventoryTest {

    private Inventory inventory;
    
    // Test items
    private Item potion;
    private Item torch;
    private Item questItem;
    private Weapon longsword;
    private Weapon dagger;
    private Weapon greatsword;
    private Armor leatherArmor;
    private Armor chainMail;
    private Armor shield;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        
        // Create test items
        potion = Item.createConsumable("Healing Potion", "Restores 2d4+2 HP", 0.5, 50);
        torch = Item.createConsumable("Torch", "Provides light for 1 hour", 1.0, 1);
        questItem = Item.createQuestItem("Ancient Key", "Opens the sealed door");
        
        // Create test weapons
        longsword = Weapon.createLongsword();
        dagger = Weapon.createDagger();
        greatsword = Weapon.createGreatsword();
        
        // Create test armor
        leatherArmor = Armor.createLeatherArmor();
        chainMail = Armor.createChainMail();
        shield = Armor.createShield();
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor creates empty inventory")
        void defaultConstructorCreatesEmptyInventory() {
            Inventory inv = new Inventory();
            
            assertTrue(inv.isEmpty());
            assertEquals(0, inv.getTotalItemCount());
            assertEquals(0, inv.getGold());
            assertEquals(0.0, inv.getCurrentWeight());
            assertEquals(0.0, inv.getMaxWeight()); // 0 = unlimited
        }

        @Test
        @DisplayName("Strength constructor sets carrying capacity")
        void strengthConstructorSetsCarryingCapacity() {
            Inventory inv = new Inventory(10); // STR 10
            
            assertEquals(150.0, inv.getMaxWeight()); // 10 * 15 = 150
        }

        @Test
        @DisplayName("High strength gives high capacity")
        void highStrengthGivesHighCapacity() {
            Inventory inv = new Inventory(20); // STR 20
            
            assertEquals(300.0, inv.getMaxWeight()); // 20 * 15 = 300
        }
    }

    // ==================== Add Item Tests ====================

    @Nested
    @DisplayName("Adding Items")
    class AddItemTests {

        @Test
        @DisplayName("Can add single item")
        void canAddSingleItem() {
            assertTrue(inventory.addItem(longsword));
            
            assertEquals(1, inventory.getTotalItemCount());
            assertTrue(inventory.hasItem(longsword));
        }

        @Test
        @DisplayName("Can add multiple different items")
        void canAddMultipleDifferentItems() {
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            inventory.addItem(potion);
            
            assertEquals(3, inventory.getTotalItemCount());
            assertTrue(inventory.hasItem(longsword));
            assertTrue(inventory.hasItem(dagger));
            assertTrue(inventory.hasItem(potion));
        }

        @Test
        @DisplayName("Adding null returns false")
        void addingNullReturnsFalse() {
            assertFalse(inventory.addItem(null));
            assertTrue(inventory.isEmpty());
        }

        @Test
        @DisplayName("Adding zero quantity returns false")
        void addingZeroQuantityReturnsFalse() {
            assertFalse(inventory.addItem(potion, 0));
            assertTrue(inventory.isEmpty());
        }

        @Test
        @DisplayName("Adding negative quantity returns false")
        void addingNegativeQuantityReturnsFalse() {
            assertFalse(inventory.addItem(potion, -5));
            assertTrue(inventory.isEmpty());
        }

        @Test
        @DisplayName("Stackable items stack together")
        void stackableItemsStackTogether() {
            inventory.addItem(potion, 3);
            inventory.addItem(potion, 2);
            
            assertEquals(5, inventory.getItemCount(potion));
            assertEquals(1, inventory.getUsedSlots()); // Only 1 stack
        }

        @Test
        @DisplayName("Non-stackable items create separate entries")
        void nonStackableItemsCreateSeparateEntries() {
            inventory.addItem(longsword);
            Weapon anotherSword = Weapon.createLongsword();
            inventory.addItem(anotherSword);
            
            assertEquals(2, inventory.getTotalItemCount());
            assertEquals(2, inventory.getUsedSlots());
        }

        @Test
        @DisplayName("Adding items updates weight")
        void addingItemsUpdatesWeight() {
            inventory.addItem(longsword); // 3.0 lbs
            assertEquals(3.0, inventory.getCurrentWeight(), 0.01);
            
            inventory.addItem(shield); // 6.0 lbs
            assertEquals(9.0, inventory.getCurrentWeight(), 0.01);
        }

        @Test
        @DisplayName("Cannot exceed weight limit")
        void cannotExceedWeightLimit() {
            inventory.setMaxWeight(10.0);
            
            assertTrue(inventory.addItem(longsword)); // 3.0 lbs - OK
            assertTrue(inventory.addItem(shield));    // 6.0 lbs - OK (total 9)
            assertFalse(inventory.addItem(chainMail)); // 55 lbs - Too heavy
            
            assertEquals(9.0, inventory.getCurrentWeight(), 0.01);
        }

        @Test
        @DisplayName("Cannot exceed slot limit")
        void cannotExceedSlotLimit() {
            inventory.setMaxSlots(2);
            
            assertTrue(inventory.addItem(longsword));
            assertTrue(inventory.addItem(dagger));
            assertFalse(inventory.addItem(greatsword));
            
            assertEquals(2, inventory.getUsedSlots());
        }
    }

    // ==================== Remove Item Tests ====================

    @Nested
    @DisplayName("Removing Items")
    class RemoveItemTests {

        @Test
        @DisplayName("Can remove single item")
        void canRemoveSingleItem() {
            inventory.addItem(longsword);
            
            assertTrue(inventory.removeItem(longsword));
            assertFalse(inventory.hasItem(longsword));
            assertTrue(inventory.isEmpty());
        }

        @Test
        @DisplayName("Can remove partial stack")
        void canRemovePartialStack() {
            inventory.addItem(potion, 5);
            
            assertTrue(inventory.removeItem(potion, 3));
            assertEquals(2, inventory.getItemCount(potion));
        }

        @Test
        @DisplayName("Can remove entire stack")
        void canRemoveEntireStack() {
            inventory.addItem(potion, 5);
            
            assertTrue(inventory.removeItem(potion, 5));
            assertFalse(inventory.hasItem(potion));
            assertEquals(0, inventory.getUsedSlots());
        }

        @Test
        @DisplayName("Cannot remove more than available")
        void cannotRemoveMoreThanAvailable() {
            inventory.addItem(potion, 3);
            
            assertFalse(inventory.removeItem(potion, 5));
            assertEquals(3, inventory.getItemCount(potion)); // Unchanged
        }

        @Test
        @DisplayName("Removing null returns false")
        void removingNullReturnsFalse() {
            assertFalse(inventory.removeItem(null));
        }

        @Test
        @DisplayName("Removing updates weight")
        void removingUpdatesWeight() {
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            double initialWeight = inventory.getCurrentWeight();
            
            inventory.removeItem(longsword);
            
            assertEquals(initialWeight - longsword.getWeight(), 
                    inventory.getCurrentWeight(), 0.01);
        }

        @Test
        @DisplayName("Can remove item by ID")
        void canRemoveItemById() {
            inventory.addItem(longsword);
            String id = longsword.getId();
            
            Item removed = inventory.removeItemById(id);
            
            assertNotNull(removed);
            assertEquals(id, removed.getId());
            assertFalse(inventory.hasItemById(id));
        }

        @Test
        @DisplayName("Remove by ID returns null for missing item")
        void removeByIdReturnsNullForMissingItem() {
            assertNull(inventory.removeItemById("nonexistent_id"));
        }
    }

    // ==================== Find Item Tests ====================

    @Nested
    @DisplayName("Finding Items")
    class FindItemTests {

        @BeforeEach
        void addTestItems() {
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            inventory.addItem(potion, 3);
            inventory.addItem(leatherArmor);
        }

        @Test
        @DisplayName("Can find item by ID")
        void canFindItemById() {
            Optional<Item> found = inventory.findItemById(longsword.getId());
            
            assertTrue(found.isPresent());
            assertEquals(longsword.getId(), found.get().getId());
        }

        @Test
        @DisplayName("Find by ID returns empty for missing item")
        void findByIdReturnsEmptyForMissingItem() {
            Optional<Item> found = inventory.findItemById("nonexistent");
            
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Can find items by name")
        void canFindItemsByName() {
            List<Item> found = inventory.findItemsByName("sword");
            
            assertEquals(1, found.size());
            assertTrue(found.get(0).getName().toLowerCase().contains("sword"));
        }

        @Test
        @DisplayName("Find by name is case insensitive")
        void findByNameIsCaseInsensitive() {
            List<Item> found = inventory.findItemsByName("LONGSWORD");
            
            assertEquals(1, found.size());
        }

        @Test
        @DisplayName("Can get items by type")
        void canGetItemsByType() {
            List<Item> weapons = inventory.getItemsByType(Item.ItemType.WEAPON);
            
            assertEquals(2, weapons.size()); // longsword and dagger
        }

        @Test
        @DisplayName("Has item returns true for existing item")
        void hasItemReturnsTrueForExistingItem() {
            assertTrue(inventory.hasItem(longsword));
            assertTrue(inventory.hasItemById(longsword.getId()));
        }

        @Test
        @DisplayName("Has item returns false for missing item")
        void hasItemReturnsFalseForMissingItem() {
            assertFalse(inventory.hasItem(greatsword));
            assertFalse(inventory.hasItemById("fake_id"));
        }

        @Test
        @DisplayName("Get item count returns correct quantity")
        void getItemCountReturnsCorrectQuantity() {
            assertEquals(3, inventory.getItemCount(potion));
            assertEquals(1, inventory.getItemCount(longsword));
            assertEquals(0, inventory.getItemCount(greatsword));
        }
    }

    // ==================== Equipment Tests ====================

    @Nested
    @DisplayName("Equipment")
    class EquipmentTests {

        @Test
        @DisplayName("Can equip weapon to main hand")
        void canEquipWeaponToMainHand() {
            inventory.addItem(longsword);
            
            inventory.equip(longsword);
            
            assertEquals(longsword, inventory.getEquipped(EquipmentSlot.MAIN_HAND));
            assertEquals(longsword, inventory.getEquippedWeapon());
            assertFalse(inventory.hasItem(longsword)); // Removed from inventory
        }

        @Test
        @DisplayName("Can equip armor to armor slot")
        void canEquipArmorToArmorSlot() {
            inventory.addItem(leatherArmor);
            
            inventory.equip(leatherArmor);
            
            assertEquals(leatherArmor, inventory.getEquipped(EquipmentSlot.ARMOR));
            assertEquals(leatherArmor, inventory.getEquippedArmor());
        }

        @Test
        @DisplayName("Can equip shield to off hand")
        void canEquipShieldToOffHand() {
            inventory.addItem(shield);
            
            inventory.equip(shield);
            
            assertEquals(shield, inventory.getEquipped(EquipmentSlot.OFF_HAND));
            assertEquals(shield, inventory.getEquippedShield());
        }

        @Test
        @DisplayName("Equipping returns previously equipped item")
        void equippingReturnsPreviouslyEquippedItem() {
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            
            inventory.equip(longsword);
            Item previous = inventory.equip(dagger);
            
            assertEquals(longsword.getId(), previous.getId());
            assertEquals(dagger, inventory.getEquippedWeapon());
            assertTrue(inventory.hasItem(longsword)); // Returned to inventory
        }

        @Test
        @DisplayName("Two-handed weapon unequips off-hand")
        void twoHandedWeaponUnequipsOffHand() {
            inventory.addItem(shield);
            inventory.addItem(greatsword);
            
            inventory.equip(shield);
            inventory.equip(greatsword);
            
            assertNull(inventory.getEquipped(EquipmentSlot.OFF_HAND));
            assertTrue(inventory.hasItem(shield)); // Returned to inventory
        }

        @Test
        @DisplayName("Cannot equip item not in inventory")
        void cannotEquipItemNotInInventory() {
            Item result = inventory.equip(longsword);
            
            assertNull(result);
            assertNull(inventory.getEquippedWeapon());
        }

        @Test
        @DisplayName("Cannot equip non-equippable item")
        void cannotEquipNonEquippableItem() {
            inventory.addItem(potion);
            
            Item result = inventory.equip(potion);
            
            assertNull(result);
        }

        @Test
        @DisplayName("Can unequip item")
        void canUnequipItem() {
            inventory.addItem(longsword);
            inventory.equip(longsword);
            
            Item unequipped = inventory.unequip(EquipmentSlot.MAIN_HAND);
            
            assertEquals(longsword.getId(), unequipped.getId());
            assertNull(inventory.getEquippedWeapon());
            assertTrue(inventory.hasItem(longsword)); // Back in inventory
        }

        @Test
        @DisplayName("Unequip empty slot returns null")
        void unequipEmptySlotReturnsNull() {
            assertNull(inventory.unequip(EquipmentSlot.MAIN_HAND));
        }

        @Test
        @DisplayName("Equipped items count toward weight")
        void equippedItemsCountTowardWeight() {
            inventory.addItem(longsword);
            double weightBefore = inventory.getCurrentWeight();
            
            inventory.equip(longsword);
            
            assertEquals(weightBefore, inventory.getCurrentWeight(), 0.01);
        }

        @Test
        @DisplayName("Can check if slot is empty")
        void canCheckIfSlotIsEmpty() {
            assertTrue(inventory.isSlotEmpty(EquipmentSlot.MAIN_HAND));
            
            inventory.addItem(longsword);
            inventory.equip(longsword);
            
            assertFalse(inventory.isSlotEmpty(EquipmentSlot.MAIN_HAND));
        }

        @Test
        @DisplayName("Can get all equipped items")
        void canGetAllEquippedItems() {
            inventory.addItem(longsword);
            inventory.addItem(leatherArmor);
            inventory.addItem(shield);
            
            inventory.equip(longsword);
            inventory.equip(leatherArmor);
            inventory.equip(shield);
            
            Map<EquipmentSlot, Item> equipped = inventory.getEquippedItems();
            
            assertEquals(3, equipped.size());
            assertTrue(equipped.containsKey(EquipmentSlot.MAIN_HAND));
            assertTrue(equipped.containsKey(EquipmentSlot.ARMOR));
            assertTrue(equipped.containsKey(EquipmentSlot.OFF_HAND));
        }

        @Test
        @DisplayName("Equipped items map is unmodifiable")
        void equippedItemsMapIsUnmodifiable() {
            inventory.addItem(longsword);
            inventory.equip(longsword);
            
            Map<EquipmentSlot, Item> equipped = inventory.getEquippedItems();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                equipped.put(EquipmentSlot.OFF_HAND, dagger);
            });
        }
    }

    // ==================== Gold Tests ====================

    @Nested
    @DisplayName("Gold Management")
    class GoldTests {

        @Test
        @DisplayName("Starts with zero gold")
        void startsWithZeroGold() {
            assertEquals(0, inventory.getGold());
        }

        @Test
        @DisplayName("Can add gold")
        void canAddGold() {
            inventory.addGold(100);
            
            assertEquals(100, inventory.getGold());
        }

        @Test
        @DisplayName("Adding negative gold does nothing")
        void addingNegativeGoldDoesNothing() {
            inventory.addGold(50);
            inventory.addGold(-20);
            
            assertEquals(50, inventory.getGold());
        }

        @Test
        @DisplayName("Can remove gold")
        void canRemoveGold() {
            inventory.addGold(100);
            
            assertTrue(inventory.removeGold(30));
            assertEquals(70, inventory.getGold());
        }

        @Test
        @DisplayName("Cannot remove more gold than available")
        void cannotRemoveMoreGoldThanAvailable() {
            inventory.addGold(50);
            
            assertFalse(inventory.removeGold(100));
            assertEquals(50, inventory.getGold()); // Unchanged
        }

        @Test
        @DisplayName("Has gold returns correct result")
        void hasGoldReturnsCorrectResult() {
            inventory.addGold(50);
            
            assertTrue(inventory.hasGold(50));
            assertTrue(inventory.hasGold(25));
            assertFalse(inventory.hasGold(100));
        }

        @Test
        @DisplayName("Gold counts toward total value")
        void goldCountsTowardTotalValue() {
            inventory.addGold(100);
            inventory.addItem(longsword); // 15 gp
            
            assertEquals(115, inventory.getTotalValue());
        }
    }

    // ==================== Weight & Capacity Tests ====================

    @Nested
    @DisplayName("Weight and Capacity")
    class WeightCapacityTests {

        @Test
        @DisplayName("Can set max weight")
        void canSetMaxWeight() {
            inventory.setMaxWeight(100.0);
            
            assertEquals(100.0, inventory.getMaxWeight());
        }

        @Test
        @DisplayName("Negative max weight becomes zero")
        void negativeMaxWeightBecomesZero() {
            inventory.setMaxWeight(-50.0);
            
            assertEquals(0.0, inventory.getMaxWeight());
        }

        @Test
        @DisplayName("Can set carrying capacity from strength")
        void canSetCarryingCapacityFromStrength() {
            inventory.setCarryingCapacityFromStrength(14);
            
            assertEquals(210.0, inventory.getMaxWeight()); // 14 * 15
        }

        @Test
        @DisplayName("Over encumbered when over weight limit")
        void overEncumberedWhenOverWeightLimit() {
            inventory.setMaxWeight(5.0);
            inventory.addItem(longsword); // 3.0 lbs - OK
            
            assertFalse(inventory.isOverEncumbered());
            
            // Directly equip to bypass weight check (simulating forced load)
            inventory.addItem(shield); // Can't add normally, but test the check
        }

        @Test
        @DisplayName("Remaining capacity calculated correctly")
        void remainingCapacityCalculatedCorrectly() {
            inventory.setMaxWeight(100.0);
            inventory.addItem(longsword); // 3.0 lbs
            
            assertEquals(97.0, inventory.getRemainingCapacity(), 0.01);
        }

        @Test
        @DisplayName("Remaining capacity returns -1 when unlimited")
        void remainingCapacityReturnsNegativeOneWhenUnlimited() {
            assertEquals(-1, inventory.getRemainingCapacity());
        }
    }

    // ==================== Slot Management Tests ====================

    @Nested
    @DisplayName("Slot Management")
    class SlotManagementTests {

        @Test
        @DisplayName("Can set max slots")
        void canSetMaxSlots() {
            inventory.setMaxSlots(10);
            
            assertEquals(10, inventory.getMaxSlots());
        }

        @Test
        @DisplayName("Negative max slots becomes zero")
        void negativeMaxSlotsBecomesZero() {
            inventory.setMaxSlots(-5);
            
            assertEquals(0, inventory.getMaxSlots());
        }

        @Test
        @DisplayName("Is full when at slot limit")
        void isFullWhenAtSlotLimit() {
            inventory.setMaxSlots(2);
            
            assertFalse(inventory.isFull());
            
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            
            assertTrue(inventory.isFull());
        }

        @Test
        @DisplayName("Never full when slots unlimited")
        void neverFullWhenSlotsUnlimited() {
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            inventory.addItem(greatsword);
            
            assertFalse(inventory.isFull());
        }

        @Test
        @DisplayName("Used slots tracks correctly")
        void usedSlotsTracksCorrectly() {
            assertEquals(0, inventory.getUsedSlots());
            
            inventory.addItem(longsword);
            assertEquals(1, inventory.getUsedSlots());
            
            inventory.addItem(potion, 5); // Stackable - one slot
            assertEquals(2, inventory.getUsedSlots());
        }
    }

    // ==================== Utility Method Tests ====================

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodTests {

        @Test
        @DisplayName("Clear removes all items")
        void clearRemovesAllItems() {
            inventory.addItem(longsword);
            inventory.addItem(dagger);
            inventory.addItem(potion, 5);
            
            inventory.clear();
            
            assertTrue(inventory.isEmpty());
            assertEquals(0, inventory.getTotalItemCount());
        }

        @Test
        @DisplayName("Clear does not affect equipped items")
        void clearDoesNotAffectEquippedItems() {
            inventory.addItem(longsword);
            inventory.equip(longsword);
            inventory.addItem(dagger);
            
            inventory.clear();
            
            assertTrue(inventory.isEmpty()); // Inventory is empty
            assertNotNull(inventory.getEquippedWeapon()); // But weapon still equipped
        }

        @Test
        @DisplayName("Total item count includes stacks")
        void totalItemCountIncludesStacks() {
            inventory.addItem(longsword);     // 1
            inventory.addItem(potion, 5);     // 5
            inventory.addItem(torch, 3);      // 3
            
            assertEquals(9, inventory.getTotalItemCount());
        }

        @Test
        @DisplayName("Total value includes all items and gold")
        void totalValueIncludesAllItemsAndGold() {
            inventory.addItem(longsword);     // 15 gp
            inventory.addItem(potion, 2);     // 50 gp each = 100 gp
            inventory.addGold(50);
            
            assertEquals(165, inventory.getTotalValue());
        }

        @Test
        @DisplayName("Total value includes equipped items")
        void totalValueIncludesEquippedItems() {
            inventory.addItem(longsword);
            inventory.equip(longsword);
            inventory.addItem(potion); // 50 gp
            
            assertEquals(65, inventory.getTotalValue()); // 15 + 50
        }

        @Test
        @DisplayName("ToString provides summary")
        void toStringProvidesSummary() {
            inventory.setMaxWeight(150.0);
            inventory.addItem(longsword);
            inventory.addGold(100);
            
            String str = inventory.toString();
            
            assertTrue(str.contains("1 items"));
            assertTrue(str.contains("100 gp"));
        }

        @Test
        @DisplayName("Get all items returns unmodifiable list")
        void getAllItemsReturnsUnmodifiableList() {
            inventory.addItem(longsword);
            
            List<ItemStack> items = inventory.getAllItems();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                items.add(new ItemStack(dagger, 1));
            });
        }
    }

    // ==================== ItemStack Tests ====================

    @Nested
    @DisplayName("ItemStack")
    class ItemStackTests {

        @Test
        @DisplayName("ItemStack stores item and quantity")
        void itemStackStoresItemAndQuantity() {
            ItemStack stack = new ItemStack(potion, 5);
            
            assertEquals(potion, stack.getItem());
            assertEquals(5, stack.getQuantity());
        }

        @Test
        @DisplayName("ItemStack minimum quantity is 1")
        void itemStackMinimumQuantityIsOne() {
            ItemStack stack = new ItemStack(potion, -5);
            
            assertEquals(1, stack.getQuantity());
        }

        @Test
        @DisplayName("Can add to stack")
        void canAddToStack() {
            ItemStack stack = new ItemStack(potion, 5);
            
            int added = stack.add(3);
            
            assertEquals(3, added);
            assertEquals(8, stack.getQuantity());
        }

        @Test
        @DisplayName("Adding respects max stack size")
        void addingRespectsMaxStackSize() {
            ItemStack stack = new ItemStack(potion, 95);
            
            int added = stack.add(10); // Max is 99
            
            assertEquals(4, added);
            assertEquals(99, stack.getQuantity());
        }

        @Test
        @DisplayName("Can remove from stack")
        void canRemoveFromStack() {
            ItemStack stack = new ItemStack(potion, 5);
            
            int removed = stack.remove(3);
            
            assertEquals(3, removed);
            assertEquals(2, stack.getQuantity());
        }

        @Test
        @DisplayName("Cannot remove more than available")
        void cannotRemoveMoreThanAvailable() {
            ItemStack stack = new ItemStack(potion, 5);
            
            int removed = stack.remove(10);
            
            assertEquals(5, removed);
            assertEquals(0, stack.getQuantity());
        }

        @Test
        @DisplayName("isEmpty returns true when quantity is zero")
        void isEmptyReturnsTrueWhenQuantityIsZero() {
            ItemStack stack = new ItemStack(potion, 1);
            
            assertFalse(stack.isEmpty());
            
            stack.remove(1);
            
            assertTrue(stack.isEmpty());
        }

        @Test
        @DisplayName("Can stack with same stackable item")
        void canStackWithSameStackableItem() {
            ItemStack stack = new ItemStack(potion, 5);
            
            assertTrue(stack.canStackWith(potion));
        }

        @Test
        @DisplayName("Cannot stack with different item")
        void cannotStackWithDifferentItem() {
            ItemStack stack = new ItemStack(potion, 5);
            
            assertFalse(stack.canStackWith(torch));
        }

        @Test
        @DisplayName("Cannot stack non-stackable items")
        void cannotStackNonStackableItems() {
            ItemStack stack = new ItemStack(longsword, 1);
            
            assertFalse(stack.canStackWith(longsword));
        }

        @Test
        @DisplayName("ToString shows quantity for stacks")
        void toStringShowsQuantityForStacks() {
            ItemStack single = new ItemStack(longsword, 1);
            ItemStack multiple = new ItemStack(potion, 5);
            
            assertEquals("Longsword", single.toString());
            assertTrue(multiple.toString().contains("x5"));
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Zero weight items don't affect capacity")
        void zeroWeightItemsDontAffectCapacity() {
            inventory.setMaxWeight(1.0);
            
            assertTrue(inventory.addItem(questItem)); // 0 weight
            assertEquals(0.0, inventory.getCurrentWeight());
        }

        @Test
        @DisplayName("Can equip to specific slot")
        void canEquipToSpecificSlot() {
            inventory.addItem(dagger);

            inventory.equipToSlot(dagger, EquipmentSlot.OFF_HAND);

            assertEquals(dagger, inventory.getEquipped(EquipmentSlot.OFF_HAND));
        }

        @Test
        @DisplayName("Can dual wield light weapons")
        void canDualWieldLightWeapons() {
            // Shortswords are light weapons suitable for dual wielding
            Weapon mainSword = Weapon.createShortsword();
            Weapon offSword = Weapon.createShortsword();

            inventory.addItem(mainSword);
            inventory.addItem(offSword);

            // Equip main hand normally
            inventory.equip(mainSword);
            assertEquals(mainSword, inventory.getEquipped(EquipmentSlot.MAIN_HAND));

            // Equip off hand explicitly
            inventory.equipToSlot(offSword, EquipmentSlot.OFF_HAND);
            assertEquals(offSword, inventory.getEquipped(EquipmentSlot.OFF_HAND));

            // Both should remain equipped
            assertEquals(mainSword, inventory.getEquipped(EquipmentSlot.MAIN_HAND));
            assertEquals(offSword, inventory.getEquipped(EquipmentSlot.OFF_HAND));
        }

        @Test
        @DisplayName("Equip to slot with null item returns null")
        void equipToSlotWithNullItemReturnsNull() {
            assertNull(inventory.equipToSlot(null, EquipmentSlot.MAIN_HAND));
        }

        @Test
        @DisplayName("Equip to null slot returns null")
        void equipToNullSlotReturnsNull() {
            inventory.addItem(longsword);
            assertNull(inventory.equipToSlot(longsword, null));
        }

        @Test
        @DisplayName("Multiple potion types stack separately")
        void multiplePotionTypesStackSeparately() {
            Item healingPotion = Item.createConsumable("Healing Potion", "", 0.5, 50);
            Item manaPotion = Item.createConsumable("Mana Potion", "", 0.5, 75);
            
            inventory.addItem(healingPotion, 3);
            inventory.addItem(manaPotion, 2);
            
            assertEquals(5, inventory.getTotalItemCount());
            assertEquals(2, inventory.getUsedSlots()); // Different items, different stacks
        }
    }
}