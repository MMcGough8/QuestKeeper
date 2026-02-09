package com.questkeeper.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the StandardEquipment singleton class.
 *
 * Tests cover singleton behavior, weapon loading, armor loading,
 * item retrieval, and copy semantics.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("StandardEquipment")
class StandardEquipmentTest {

    private StandardEquipment equipment;

    @BeforeEach
    void setUp() {
        equipment = StandardEquipment.getInstance();
    }

    // ==================== Singleton Tests ====================

    @Nested
    @DisplayName("Singleton Behavior")
    class SingletonTests {

        @Test
        @DisplayName("getInstance returns non-null instance")
        void getInstanceReturnsNonNull() {
            assertNotNull(StandardEquipment.getInstance());
        }

        @Test
        @DisplayName("getInstance returns same instance")
        void getInstanceReturnsSameInstance() {
            StandardEquipment first = StandardEquipment.getInstance();
            StandardEquipment second = StandardEquipment.getInstance();

            assertSame(first, second);
        }
    }

    // ==================== Weapon Loading Tests ====================

    @Nested
    @DisplayName("Weapon Loading")
    class WeaponLoadingTests {

        @Test
        @DisplayName("loads simple melee weapons")
        void loadsSimpleMeleeWeapons() {
            assertTrue(equipment.hasWeapon("club"));
            assertTrue(equipment.hasWeapon("dagger"));
            assertTrue(equipment.hasWeapon("quarterstaff"));
            assertTrue(equipment.hasWeapon("spear"));
        }

        @Test
        @DisplayName("loads martial melee weapons")
        void loadsMartialMeleeWeapons() {
            assertTrue(equipment.hasWeapon("longsword"));
            assertTrue(equipment.hasWeapon("greatsword"));
            assertTrue(equipment.hasWeapon("rapier"));
            assertTrue(equipment.hasWeapon("battleaxe"));
        }

        @Test
        @DisplayName("loads ranged weapons")
        void loadsRangedWeapons() {
            assertTrue(equipment.hasWeapon("shortbow"));
            assertTrue(equipment.hasWeapon("longbow"));
            assertTrue(equipment.hasWeapon("light_crossbow"));
            assertTrue(equipment.hasWeapon("heavy_crossbow"));
        }

        @Test
        @DisplayName("weapon lookup is case insensitive")
        void weaponLookupCaseInsensitive() {
            assertTrue(equipment.hasWeapon("LONGSWORD"));
            assertTrue(equipment.hasWeapon("Longsword"));
            assertTrue(equipment.hasWeapon("longsword"));
        }
    }

    // ==================== Armor Loading Tests ====================

    @Nested
    @DisplayName("Armor Loading")
    class ArmorLoadingTests {

        @Test
        @DisplayName("loads light armor")
        void loadsLightArmor() {
            assertTrue(equipment.hasArmor("leather_armor"));
            assertTrue(equipment.hasArmor("studded_leather"));
            assertTrue(equipment.hasArmor("padded_armor"));
        }

        @Test
        @DisplayName("loads medium armor")
        void loadsMediumArmor() {
            assertTrue(equipment.hasArmor("chain_shirt"));
            assertTrue(equipment.hasArmor("scale_mail"));
            assertTrue(equipment.hasArmor("breastplate"));
            assertTrue(equipment.hasArmor("half_plate"));
        }

        @Test
        @DisplayName("loads heavy armor")
        void loadsHeavyArmor() {
            assertTrue(equipment.hasArmor("chain_mail"));
            assertTrue(equipment.hasArmor("plate_armor"));
            assertTrue(equipment.hasArmor("splint_armor"));
        }

        @Test
        @DisplayName("loads shields")
        void loadsShields() {
            assertTrue(equipment.hasArmor("shield"));
        }

        @Test
        @DisplayName("armor lookup is case insensitive")
        void armorLookupCaseInsensitive() {
            assertTrue(equipment.hasArmor("LEATHER_ARMOR"));
            assertTrue(equipment.hasArmor("Leather_Armor"));
            assertTrue(equipment.hasArmor("leather_armor"));
        }
    }

    // ==================== Weapon Retrieval Tests ====================

    @Nested
    @DisplayName("Weapon Retrieval")
    class WeaponRetrievalTests {

        @Test
        @DisplayName("getWeapon returns correct weapon")
        void getWeaponReturnsCorrectWeapon() {
            Weapon longsword = equipment.getWeapon("longsword");

            assertNotNull(longsword);
            assertEquals("Longsword", longsword.getName());
        }

        @Test
        @DisplayName("weapon has correct damage")
        void weaponHasCorrectDamage() {
            Weapon longsword = equipment.getWeapon("longsword");

            assertEquals(1, longsword.getDamageDiceCount());
            assertEquals(8, longsword.getDamageDieSize());
            assertEquals(Weapon.DamageType.SLASHING, longsword.getDamageType());
        }

        @Test
        @DisplayName("weapon has correct category")
        void weaponHasCorrectCategory() {
            Weapon longsword = equipment.getWeapon("longsword");
            Weapon dagger = equipment.getWeapon("dagger");

            assertEquals(Weapon.WeaponCategory.MARTIAL_MELEE, longsword.getCategory());
            assertEquals(Weapon.WeaponCategory.SIMPLE_MELEE, dagger.getCategory());
        }

        @Test
        @DisplayName("weapon has correct properties")
        void weaponHasCorrectProperties() {
            Weapon longsword = equipment.getWeapon("longsword");
            Weapon dagger = equipment.getWeapon("dagger");

            assertTrue(longsword.hasProperty(Weapon.WeaponProperty.VERSATILE));
            assertTrue(dagger.hasProperty(Weapon.WeaponProperty.FINESSE));
            assertTrue(dagger.hasProperty(Weapon.WeaponProperty.LIGHT));
            assertTrue(dagger.hasProperty(Weapon.WeaponProperty.THROWN));
        }

        @Test
        @DisplayName("weapon has correct range for thrown/ranged")
        void weaponHasCorrectRange() {
            Weapon dagger = equipment.getWeapon("dagger");
            Weapon longbow = equipment.getWeapon("longbow");

            assertEquals(20, dagger.getNormalRange());
            assertEquals(60, dagger.getLongRange());
            assertEquals(150, longbow.getNormalRange());
            assertEquals(600, longbow.getLongRange());
        }

        @Test
        @DisplayName("versatile weapon has correct versatile die")
        void versatileWeaponHasCorrectVersatileDie() {
            Weapon longsword = equipment.getWeapon("longsword");
            Weapon quarterstaff = equipment.getWeapon("quarterstaff");

            assertEquals(10, longsword.getVersatileDieSize());
            assertEquals(8, quarterstaff.getVersatileDieSize());
        }

        @Test
        @DisplayName("weapon has correct weight and value")
        void weaponHasCorrectWeightAndValue() {
            Weapon longsword = equipment.getWeapon("longsword");

            assertEquals(3.0, longsword.getWeight(), 0.001);
            assertEquals(15, longsword.getGoldValue());
        }

        @Test
        @DisplayName("returns null for unknown weapon")
        void returnsNullForUnknownWeapon() {
            assertNull(equipment.getWeapon("lightsaber"));
            assertNull(equipment.getWeapon(""));
            assertNull(equipment.getWeapon("nonexistent"));
        }

        @Test
        @DisplayName("returns copy not original")
        void returnsCopyNotOriginal() {
            Weapon first = equipment.getWeapon("longsword");
            Weapon second = equipment.getWeapon("longsword");

            assertNotSame(first, second);
            assertEquals(first.getName(), second.getName());
        }

        @Test
        @DisplayName("modifying copy does not affect cache")
        void modifyingCopyDoesNotAffectCache() {
            Weapon weapon = equipment.getWeapon("longsword");
            weapon.setName("Modified Sword");

            Weapon fresh = equipment.getWeapon("longsword");
            assertEquals("Longsword", fresh.getName());
        }
    }

    // ==================== Armor Retrieval Tests ====================

    @Nested
    @DisplayName("Armor Retrieval")
    class ArmorRetrievalTests {

        @Test
        @DisplayName("getArmor returns correct armor")
        void getArmorReturnsCorrectArmor() {
            Armor chainmail = equipment.getArmor("chain_mail");

            assertNotNull(chainmail);
            assertEquals("Chain Mail", chainmail.getName());
        }

        @Test
        @DisplayName("armor has correct AC")
        void armorHasCorrectAC() {
            Armor leather = equipment.getArmor("leather_armor");
            Armor chainmail = equipment.getArmor("chain_mail");
            Armor plate = equipment.getArmor("plate_armor");

            assertEquals(11, leather.getBaseAC());
            assertEquals(16, chainmail.getBaseAC());
            assertEquals(18, plate.getBaseAC());
        }

        @Test
        @DisplayName("armor has correct category")
        void armorHasCorrectCategory() {
            Armor leather = equipment.getArmor("leather_armor");
            Armor chainmail = equipment.getArmor("chain_mail");
            Armor shield = equipment.getArmor("shield");

            assertEquals(Armor.ArmorCategory.LIGHT, leather.getCategory());
            assertEquals(Armor.ArmorCategory.HEAVY, chainmail.getCategory());
            assertEquals(Armor.ArmorCategory.SHIELD, shield.getCategory());
        }

        @Test
        @DisplayName("armor has correct strength requirement")
        void armorHasCorrectStrengthRequirement() {
            Armor chainmail = equipment.getArmor("chain_mail");
            Armor plate = equipment.getArmor("plate_armor");
            Armor leather = equipment.getArmor("leather_armor");

            assertEquals(13, chainmail.getStrengthRequirement());
            assertEquals(15, plate.getStrengthRequirement());
            assertEquals(0, leather.getStrengthRequirement());
        }

        @Test
        @DisplayName("armor has correct stealth disadvantage")
        void armorHasCorrectStealthDisadvantage() {
            Armor chainmail = equipment.getArmor("chain_mail");
            Armor leather = equipment.getArmor("leather_armor");
            Armor paddedArmor = equipment.getArmor("padded_armor");

            assertTrue(chainmail.hasStealthDisadvantage());
            assertFalse(leather.hasStealthDisadvantage());
            assertTrue(paddedArmor.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("armor has correct weight and value")
        void armorHasCorrectWeightAndValue() {
            Armor plate = equipment.getArmor("plate_armor");

            assertEquals(65.0, plate.getWeight(), 0.001);
            assertEquals(1500, plate.getGoldValue());
        }

        @Test
        @DisplayName("returns null for unknown armor")
        void returnsNullForUnknownArmor() {
            assertNull(equipment.getArmor("mithril_armor"));
            assertNull(equipment.getArmor(""));
            assertNull(equipment.getArmor("nonexistent"));
        }

        @Test
        @DisplayName("returns copy not original")
        void returnsCopyNotOriginal() {
            Armor first = equipment.getArmor("chain_mail");
            Armor second = equipment.getArmor("chain_mail");

            assertNotSame(first, second);
            assertEquals(first.getName(), second.getName());
        }

        @Test
        @DisplayName("modifying copy does not affect cache")
        void modifyingCopyDoesNotAffectCache() {
            Armor armor = equipment.getArmor("chain_mail");
            armor.setName("Modified Armor");

            Armor fresh = equipment.getArmor("chain_mail");
            assertEquals("Chain Mail", fresh.getName());
        }
    }

    // ==================== Generic Item Retrieval Tests ====================

    @Nested
    @DisplayName("Generic Item Retrieval")
    class GenericItemRetrievalTests {

        @Test
        @DisplayName("getItem returns weapon for weapon ID")
        void getItemReturnsWeaponForWeaponId() {
            Item item = equipment.getItem("longsword");

            assertNotNull(item);
            assertInstanceOf(Weapon.class, item);
            assertEquals("Longsword", item.getName());
        }

        @Test
        @DisplayName("getItem returns armor for armor ID")
        void getItemReturnsArmorForArmorId() {
            Item item = equipment.getItem("chain_mail");

            assertNotNull(item);
            assertInstanceOf(Armor.class, item);
            assertEquals("Chain Mail", item.getName());
        }

        @Test
        @DisplayName("getItem is case insensitive")
        void getItemIsCaseInsensitive() {
            assertNotNull(equipment.getItem("LONGSWORD"));
            assertNotNull(equipment.getItem("CHAIN_MAIL"));
        }

        @Test
        @DisplayName("getItem returns null for unknown ID")
        void getItemReturnsNullForUnknownId() {
            assertNull(equipment.getItem("unknown_item"));
        }
    }

    // ==================== Specific Weapon Tests (D&D 5e Accuracy) ====================

    @Nested
    @DisplayName("D&D 5e Weapon Accuracy")
    class DnD5eWeaponAccuracyTests {

        @Test
        @DisplayName("greatsword has correct 2d6 damage")
        void greatswordHasCorrect2d6Damage() {
            Weapon greatsword = equipment.getWeapon("greatsword");

            assertEquals(2, greatsword.getDamageDiceCount());
            assertEquals(6, greatsword.getDamageDieSize());
            assertTrue(greatsword.hasProperty(Weapon.WeaponProperty.TWO_HANDED));
            assertTrue(greatsword.hasProperty(Weapon.WeaponProperty.HEAVY));
        }

        @Test
        @DisplayName("rapier is finesse martial weapon")
        void rapierIsFinesseMaritalWeapon() {
            Weapon rapier = equipment.getWeapon("rapier");

            assertEquals(1, rapier.getDamageDiceCount());
            assertEquals(8, rapier.getDamageDieSize());
            assertEquals(Weapon.WeaponCategory.MARTIAL_MELEE, rapier.getCategory());
            assertTrue(rapier.hasProperty(Weapon.WeaponProperty.FINESSE));
        }

        @Test
        @DisplayName("glaive has reach property")
        void glaiveHasReachProperty() {
            Weapon glaive = equipment.getWeapon("glaive");

            assertTrue(glaive.hasProperty(Weapon.WeaponProperty.REACH));
            assertTrue(glaive.hasProperty(Weapon.WeaponProperty.HEAVY));
            assertTrue(glaive.hasProperty(Weapon.WeaponProperty.TWO_HANDED));
        }

        @Test
        @DisplayName("hand crossbow is light one-handed ranged")
        void handCrossbowIsLightOneHandedRanged() {
            Weapon handCrossbow = equipment.getWeapon("hand_crossbow");

            assertTrue(handCrossbow.hasProperty(Weapon.WeaponProperty.LIGHT));
            assertTrue(handCrossbow.hasProperty(Weapon.WeaponProperty.LOADING));
            assertFalse(handCrossbow.hasProperty(Weapon.WeaponProperty.TWO_HANDED));
        }
    }

    // ==================== Specific Armor Tests (D&D 5e Accuracy) ====================

    @Nested
    @DisplayName("D&D 5e Armor Accuracy")
    class DnD5eArmorAccuracyTests {

        @Test
        @DisplayName("shield provides +2 AC bonus")
        void shieldProvidesPlusTwoAC() {
            Armor shield = equipment.getArmor("shield");

            assertEquals(2, shield.getBaseAC());
            assertEquals(Armor.ArmorCategory.SHIELD, shield.getCategory());
        }

        @Test
        @DisplayName("studded leather is best light armor")
        void studdedLeatherIsBestLightArmor() {
            Armor studded = equipment.getArmor("studded_leather");

            assertEquals(12, studded.getBaseAC());
            assertEquals(Armor.ArmorCategory.LIGHT, studded.getCategory());
            assertFalse(studded.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("half plate is best medium armor")
        void halfPlateIsBestMediumArmor() {
            Armor halfPlate = equipment.getArmor("half_plate");

            assertEquals(15, halfPlate.getBaseAC());
            assertEquals(Armor.ArmorCategory.MEDIUM, halfPlate.getCategory());
            assertTrue(halfPlate.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("plate armor is best heavy armor")
        void plateArmorIsBestHeavyArmor() {
            Armor plate = equipment.getArmor("plate_armor");

            assertEquals(18, plate.getBaseAC());
            assertEquals(Armor.ArmorCategory.HEAVY, plate.getCategory());
            assertEquals(15, plate.getStrengthRequirement());
        }
    }
}
