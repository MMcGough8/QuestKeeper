package com.questkeeper.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import com.questkeeper.inventory.Armor.ArmorType;
import com.questkeeper.inventory.Item.ItemType;
import com.questkeeper.inventory.Item.Rarity;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.Weapon.DamageType;
import com.questkeeper.inventory.Weapon.WeaponProperty;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test suite for the Inventory system (Item, Weapon, Armor, Inventory).
 * 
 * @author Marc McGough
 */
class InventoryTest {
    
    private Inventory inventory;
    private Item potion;
    private Item gold;
    private Weapon sword;
    private Weapon dagger;
    private Armor chainmail;
    private Armor shield;
    
    @BeforeEach
    void setUp() {
        inventory = new Inventory(10); // STR 10 = 150 lb capacity
        
        // Basic items
        potion = new Item("potion_healing", "Healing Potion", 
                "Restores 2d4+2 HP", ItemType.CONSUMABLE, Rarity.COMMON, 0.5, 50);
        potion.setStackable(true, 10);
        
        gold = new Item("gold_coin", "Gold Coin", ItemType.TREASURE);
        gold.setStackable(true, 9999);
        
        // Weapons
        sword = new Weapon("longsword", "Longsword", "A versatile blade",
                Rarity.COMMON, 3.0, 15, 1, 8, DamageType.SLASHING);
        sword.setVersatileDamage(1, 10);
        sword.addProperty(WeaponProperty.MARTIAL);
        
        dagger = new Weapon("dagger", "Dagger", 1, 4, DamageType.PIERCING);
        dagger.addProperty(WeaponProperty.FINESSE)
              .addProperty(WeaponProperty.LIGHT)
              .addProperty(WeaponProperty.THROWN)
              .setRange(20, 60);
        dagger.setWeight(1.0);
        dagger.setGoldValue(2);
        
        // Armor
        chainmail = new Armor("chainmail", "Chain Mail", "Heavy armor made of interlocking rings",
                Rarity.COMMON, 55.0, 75, ArmorType.HEAVY, 16);
        chainmail.setStrengthRequirement(13).setStealthDisadvantage(true);
        
        shield = new Armor("shield", "Shield", ArmorType.SHIELD, 2);
        shield.setWeight(6.0);
        shield.setGoldValue(10);
    }
    
    // ========================================================================
    // ITEM TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Item Tests")
    class ItemTests {
        
        @Test
        @DisplayName("Item is created with correct properties")
        void itemCreatedCorrectly() {
            assertEquals("potion_healing", potion.getId());
            assertEquals("Healing Potion", potion.getName());
            assertEquals(ItemType.CONSUMABLE, potion.getType());
            assertEquals(Rarity.COMMON, potion.getRarity());
            assertEquals(0.5, potion.getWeight());
            assertEquals(50, potion.getGoldValue());
        }
        
        @Test
        @DisplayName("Item type checking methods work")
        void itemTypeChecking() {
            assertTrue(potion.isConsumable());
            assertFalse(potion.isWeapon());
            assertFalse(potion.isArmor());
            assertFalse(potion.isEquippable());
            
            assertTrue(sword.isWeapon());
            assertTrue(sword.isEquippable());
            
            assertTrue(chainmail.isArmor());
            assertTrue(chainmail.isEquippable());
            
            assertTrue(shield.isShield());
        }
        
        @Test
        @DisplayName("Stackable items have correct max stack size")
        void stackableItems() {
            assertTrue(potion.isStackable());
            assertEquals(10, potion.getMaxStackSize());
            
            assertFalse(sword.isStackable());
            assertEquals(1, sword.getMaxStackSize());
        }
        
        @Test
        @DisplayName("Item copy creates independent copy")
        void itemCopyWorks() {
            Item copy = potion.copy();
            
            assertEquals(potion.getId(), copy.getId());
            assertEquals(potion.getName(), copy.getName());
            
            copy.setName("Modified Potion");
            assertNotEquals(potion.getName(), copy.getName());
        }
        
        @Test
        @DisplayName("Item equality based on ID")
        void itemEquality() {
            Item same = new Item("potion_healing", "Different Name", ItemType.CONSUMABLE);
            Item different = new Item("potion_mana", "Healing Potion", ItemType.CONSUMABLE);
            
            assertEquals(potion, same);
            assertNotEquals(potion, different);
        }
    }
    
    // ========================================================================
    // WEAPON TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Weapon Tests")
    class WeaponTests {
        
        @Test
        @DisplayName("Weapon damage dice formatted correctly")
        void damageDiceFormat() {
            assertEquals("1d8", sword.getDamageDice());
            assertEquals("1d4", dagger.getDamageDice());
            
            sword.setDamageBonus(2);
            assertEquals("1d8+2", sword.getDamageDice());
        }
        
        @Test
        @DisplayName("Weapon damage range calculated correctly")
        void damageRange() {
            // 1d8: min 1, max 8, avg 4.5
            assertEquals(1, sword.getMinDamage());
            assertEquals(8, sword.getMaxDamage());
            assertEquals(4.5, sword.getAverageDamage());
        }
        
        @Test
        @DisplayName("Weapon properties work correctly")
        void weaponProperties() {
            assertTrue(sword.isVersatile());
            assertTrue(sword.isMartial());
            assertFalse(sword.isFinesse());
            
            assertTrue(dagger.isFinesse());
            assertTrue(dagger.isLight());
            assertTrue(dagger.isThrown());
            assertTrue(dagger.isRanged());
        }
        
        @Test
        @DisplayName("Versatile damage set correctly")
        void versatileDamage() {
            assertEquals("1d10", sword.getVersatileDamageDice());
            assertNull(dagger.getVersatileDamageDice());
        }
        
        @Test
        @DisplayName("Weapon range set correctly")
        void weaponRange() {
            assertEquals(20, dagger.getRangeNormal());
            assertEquals(60, dagger.getRangeLong());
            assertEquals("20/60", dagger.getRangeString());
            
            assertNull(sword.getRangeString());
        }
        
        @Test
        @DisplayName("Magic weapon bonus works")
        void magicWeaponBonus() {
            sword.setAttackBonus(1);
            sword.setDamageBonus(1);
            
            assertEquals(1, sword.getAttackBonus());
            assertEquals("1d8+1", sword.getDamageDice());
        }
    }
    
    // ========================================================================
    // ARMOR TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Armor Tests")
    class ArmorTests {
        
        @Test
        @DisplayName("Armor created with correct properties")
        void armorCreatedCorrectly() {
            assertEquals(ArmorType.HEAVY, chainmail.getArmorType());
            assertEquals(16, chainmail.getBaseAC());
            assertEquals(13, chainmail.getStrengthRequirement());
            assertTrue(chainmail.hasStealthDisadvantage());
        }
        
        @Test
        @DisplayName("Shield created correctly")
        void shieldCreatedCorrectly() {
            assertEquals(ArmorType.SHIELD, shield.getArmorType());
            assertEquals(2, shield.getBaseAC());
            assertTrue(shield.isShield());
        }
        
        @Test
        @DisplayName("Heavy armor ignores DEX")
        void heavyArmorIgnoresDex() {
            assertEquals(16, chainmail.calculateAC(0));
            assertEquals(16, chainmail.calculateAC(2));
            assertEquals(16, chainmail.calculateAC(-1));
        }
        
        @Test
        @DisplayName("Medium armor caps DEX at +2")
        void mediumArmorCapsDex() {
            Armor breastplate = new Armor("breastplate", "Breastplate", 
                    ArmorType.MEDIUM, 14);
            
            assertEquals(14, breastplate.calculateAC(0));
            assertEquals(16, breastplate.calculateAC(2));
            assertEquals(16, breastplate.calculateAC(4)); // Capped at +2
            assertEquals(13, breastplate.calculateAC(-1));
        }
        
        @Test
        @DisplayName("Light armor uses full DEX")
        void lightArmorFullDex() {
            Armor leather = new Armor("leather", "Leather Armor", 
                    ArmorType.LIGHT, 11);
            
            assertEquals(11, leather.calculateAC(0));
            assertEquals(13, leather.calculateAC(2));
            assertEquals(15, leather.calculateAC(4));
        }
        
        @Test
        @DisplayName("Magic armor bonus works")
        void magicArmorBonus() {
            chainmail.setMagicBonus(1);
            
            assertEquals(1, chainmail.getMagicBonus());
            assertEquals(17, chainmail.getTotalAC());
            assertTrue(chainmail.isMagical());
            assertEquals(Rarity.UNCOMMON, chainmail.getRarity());
        }
        
        @Test
        @DisplayName("Strength requirement check works")
        void strengthRequirementCheck() {
            assertTrue(chainmail.meetsStrengthRequirement(13));
            assertTrue(chainmail.meetsStrengthRequirement(16));
            assertFalse(chainmail.meetsStrengthRequirement(10));
        }
    }
    
    // ========================================================================
    // INVENTORY BASIC TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Inventory Basic Tests")
    class InventoryBasicTests {
        
        @Test
        @DisplayName("Inventory starts empty")
        void inventoryStartsEmpty() {
            assertTrue(inventory.isEmpty());
            assertEquals(0, inventory.getTotalItemCount());
            assertEquals(0, inventory.getGold());
        }
        
        @Test
        @DisplayName("Carrying capacity set from strength")
        void carryingCapacityFromStrength() {
            assertEquals(150.0, inventory.getMaxWeight()); // STR 10 * 15
        }
        
        @Test
        @DisplayName("Adding item works")
        void addingItemWorks() {
            assertTrue(inventory.addItem(sword));
            
            assertFalse(inventory.isEmpty());
            assertEquals(1, inventory.getTotalItemCount());
            assertTrue(inventory.hasItem(sword));
        }
        
        @Test
        @DisplayName("Removing item works")
        void removingItemWorks() {
            inventory.addItem(sword);
            assertTrue(inventory.removeItem(sword));
            
            assertTrue(inventory.isEmpty());
            assertFalse(inventory.hasItem(sword));
        }
        
        @Test
        @DisplayName("Cannot remove item not in inventory")
        void cannotRemoveNonexistentItem() {
            assertFalse(inventory.removeItem(sword));
        }
        
        @Test
        @DisplayName("Find item by ID works")
        void findItemByIdWorks() {
            inventory.addItem(sword);
            
            assertTrue(inventory.findItemById("longsword").isPresent());
            assertFalse(inventory.findItemById("nonexistent").isPresent());
        }
        
        @Test
        @DisplayName("Find items by name works")
        void findItemsByNameWorks() {
            inventory.addItem(sword);
            inventory.addItem(dagger);
            
            List<Item> found = inventory.findItemsByName("sword");
            assertEquals(1, found.size());
            assertEquals("Longsword", found.get(0).getName());
        }
        
        @Test
        @DisplayName("Get items by type works")
        void getItemsByTypeWorks() {
            inventory.addItem(sword);
            inventory.addItem(dagger);
            inventory.addItem(potion);
            
            List<Item> weapons = inventory.getItemsByType(ItemType.WEAPON);
            assertEquals(2, weapons.size());
            
            List<Item> consumables = inventory.getItemsByType(ItemType.CONSUMABLE);
            assertEquals(1, consumables.size());
        }
    }
    
    // ========================================================================
    // INVENTORY STACKING TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Inventory Stacking Tests")
    class InventoryStackingTests {
        
        @Test
        @DisplayName("Stackable items stack together")
        void stackableItemsStack() {
            inventory.addItem(potion, 3);
            inventory.addItem(potion, 2);
            
            assertEquals(1, inventory.getUsedSlots()); // Single stack
            assertEquals(5, inventory.getItemCount(potion));
        }
        
        @Test
        @DisplayName("Non-stackable items don't stack")
        void nonStackableItemsDontStack() {
            inventory.addItem(sword);
            inventory.addItem(sword.copy());
            
            assertEquals(2, inventory.getUsedSlots());
        }
        
        @Test
        @DisplayName("Stack respects max stack size")
        void stackRespectsMaxSize() {
            inventory.addItem(potion, 15); // Max stack is 10
            
            assertEquals(2, inventory.getUsedSlots()); // Split into two stacks
            assertEquals(15, inventory.getTotalItemCount());
        }
        
        @Test
        @DisplayName("Removing from stack reduces quantity")
        void removingFromStackWorks() {
            inventory.addItem(potion, 5);
            inventory.removeItem(potion, 2);
            
            assertEquals(3, inventory.getItemCount(potion));
            assertEquals(1, inventory.getUsedSlots());
        }
        
        @Test
        @DisplayName("Removing entire stack removes slot")
        void removingEntireStackRemovesSlot() {
            inventory.addItem(potion, 5);
            inventory.removeItem(potion, 5);
            
            assertTrue(inventory.isEmpty());
            assertEquals(0, inventory.getUsedSlots());
        }
    }
    
    // ========================================================================
    // INVENTORY WEIGHT TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Inventory Weight Tests")
    class InventoryWeightTests {
        
        @Test
        @DisplayName("Weight calculated correctly")
        void weightCalculatedCorrectly() {
            inventory.addItem(sword); // 3.0 lb
            inventory.addItem(dagger); // 1.0 lb
            
            assertEquals(4.0, inventory.getCurrentWeight());
        }
        
        @Test
        @DisplayName("Stacked items weight multiplied")
        void stackedItemsWeightMultiplied() {
            inventory.addItem(potion, 4); // 0.5 lb each
            
            assertEquals(2.0, inventory.getCurrentWeight());
        }
        
        @Test
        @DisplayName("Cannot add item exceeding capacity")
        void cannotExceedCapacity() {
            inventory.setMaxWeight(50.0);
            
            assertTrue(inventory.addItem(sword)); // 3.0 lb
            assertFalse(inventory.addItem(chainmail)); // 55.0 lb - too heavy
        }
        
        @Test
        @DisplayName("Over-encumbered check works")
        void overEncumberedCheck() {
            inventory.setMaxWeight(2.0);
            inventory.addItem(dagger); // 1.0 lb
            
            assertFalse(inventory.isOverEncumbered());
            
            inventory.addItem(potion, 3); // 1.5 lb more
            assertTrue(inventory.isOverEncumbered());
        }
        
        @Test
        @DisplayName("Remaining capacity calculated correctly")
        void remainingCapacityCorrect() {
            inventory.setMaxWeight(100.0);
            inventory.addItem(sword); // 3.0 lb
            
            assertEquals(97.0, inventory.getRemainingCapacity());
        }
    }
    
    // ========================================================================
    // INVENTORY EQUIPMENT TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Inventory Equipment Tests")
    class InventoryEquipmentTests {
        
        @Test
        @DisplayName("Can equip weapon")
        void canEquipWeapon() {
            inventory.addItem(sword);
            inventory.equip(sword);
            
            assertEquals(sword, inventory.getEquippedWeapon());
            assertFalse(inventory.hasItem(sword)); // Removed from inventory
        }
        
        @Test
        @DisplayName("Can equip armor")
        void canEquipArmor() {
            inventory.addItem(chainmail);
            inventory.equip(chainmail);
            
            assertEquals(chainmail, inventory.getEquippedArmor());
        }
        
        @Test
        @DisplayName("Can equip shield")
        void canEquipShield() {
            inventory.addItem(shield);
            inventory.equip(shield);
            
            assertEquals(shield, inventory.getEquippedShield());
        }
        
        @Test
        @DisplayName("Equipping replaces previous item")
        void equippingReplacesItem() {
            inventory.addItem(sword);
            inventory.addItem(dagger);
            
            inventory.equip(sword);
            Item replaced = inventory.equip(dagger);
            
            assertEquals(sword, replaced);
            assertEquals(dagger, inventory.getEquippedWeapon());
            assertTrue(inventory.hasItem(sword)); // Returned to inventory
        }
        
        @Test
        @DisplayName("Unequipping returns item to inventory")
        void unequippingReturnsItem() {
            inventory.addItem(sword);
            inventory.equip(sword);
            
            Item unequipped = inventory.unequip(EquipmentSlot.MAIN_HAND);
            
            assertEquals(sword, unequipped);
            assertNull(inventory.getEquippedWeapon());
            assertTrue(inventory.hasItem(sword));
        }
        
        @Test
        @DisplayName("Cannot equip item not in inventory")
        void cannotEquipNonexistentItem() {
            assertNull(inventory.equip(sword));
            assertNull(inventory.getEquippedWeapon());
        }
        
        @Test
        @DisplayName("Equipped items count toward weight")
        void equippedItemsCountWeight() {
            inventory.addItem(sword);
            double beforeEquip = inventory.getCurrentWeight();
            
            inventory.equip(sword);
            
            assertEquals(beforeEquip, inventory.getCurrentWeight());
        }
    }
    
    // ========================================================================
    // INVENTORY GOLD TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Inventory Gold Tests")
    class InventoryGoldTests {
        
        @Test
        @DisplayName("Adding gold works")
        void addingGoldWorks() {
            inventory.addGold(100);
            assertEquals(100, inventory.getGold());
            
            inventory.addGold(50);
            assertEquals(150, inventory.getGold());
        }
        
        @Test
        @DisplayName("Removing gold works")
        void removingGoldWorks() {
            inventory.addGold(100);
            
            assertTrue(inventory.removeGold(30));
            assertEquals(70, inventory.getGold());
        }
        
        @Test
        @DisplayName("Cannot remove more gold than available")
        void cannotRemoveExcessGold() {
            inventory.addGold(50);
            
            assertFalse(inventory.removeGold(100));
            assertEquals(50, inventory.getGold()); // Unchanged
        }
        
        @Test
        @DisplayName("Has gold check works")
        void hasGoldCheckWorks() {
            inventory.addGold(100);
            
            assertTrue(inventory.hasGold(50));
            assertTrue(inventory.hasGold(100));
            assertFalse(inventory.hasGold(150));
        }
        
        @Test
        @DisplayName("Total value includes gold")
        void totalValueIncludesGold() {
            inventory.addGold(100);
            inventory.addItem(sword); // 15 gp
            
            assertEquals(115, inventory.getTotalValue());
        }
    }
    
    // ========================================================================
    // INVENTORY SLOT TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("Inventory Slot Tests")
    class InventorySlotTests {
        
        @Test
        @DisplayName("Slot limit enforced")
        void slotLimitEnforced() {
            inventory.setMaxSlots(2);
            
            assertTrue(inventory.addItem(sword));
            assertTrue(inventory.addItem(dagger));
            assertFalse(inventory.addItem(chainmail)); // 3rd item blocked
        }
        
        @Test
        @DisplayName("Is full check works")
        void isFullCheckWorks() {
            inventory.setMaxSlots(1);
            
            assertFalse(inventory.isFull());
            inventory.addItem(sword);
            assertTrue(inventory.isFull());
        }
        
        @Test
        @DisplayName("Stacking doesn't use extra slots")
        void stackingDoesntUseSlots() {
            inventory.setMaxSlots(1);
            
            inventory.addItem(potion, 5);
            assertEquals(1, inventory.getUsedSlots());
            
            inventory.addItem(potion, 3);
            assertEquals(1, inventory.getUsedSlots());
            assertEquals(8, inventory.getTotalItemCount());
        }
    }
}