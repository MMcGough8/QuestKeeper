package com.questkeeper.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.questkeeper.inventory.Item.ItemType;
import com.questkeeper.inventory.Item.Rarity;
import com.questkeeper.inventory.Weapon.DamageType;
import com.questkeeper.inventory.Weapon.WeaponCategory;
import com.questkeeper.inventory.Weapon.WeaponProperty;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Weapon class.
 * 
 * Tests cover weapon creation, damage calculations, properties,
 * categories, and factory methods for standard D&D weapons.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Weapon")
class WeaponTest {

    private Weapon basicWeapon;
    private Weapon rangedWeapon;

    @BeforeEach
    void setUp() {
        basicWeapon = new Weapon("Test Sword", 1, 8, DamageType.SLASHING,
                WeaponCategory.MARTIAL_MELEE, 3.0, 15);
        rangedWeapon = new Weapon("Test Bow", 1, 6, DamageType.PIERCING,
                WeaponCategory.SIMPLE_RANGED, 80, 320, 2.0, 25);
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructors")
    class ConstructorTests {

        @Test
        @DisplayName("Basic constructor sets weapon properties")
        void basicConstructorSetsWeaponProperties() {
            Weapon weapon = new Weapon("Longsword", 1, 8, DamageType.SLASHING,
                    WeaponCategory.MARTIAL_MELEE, 3.0, 15);
            
            assertEquals("Longsword", weapon.getName());
            assertEquals(1, weapon.getDamageDiceCount());
            assertEquals(8, weapon.getDamageDieSize());
            assertEquals(DamageType.SLASHING, weapon.getDamageType());
            assertEquals(WeaponCategory.MARTIAL_MELEE, weapon.getCategory());
            assertEquals(3.0, weapon.getWeight(), 0.001);
            assertEquals(15, weapon.getGoldValue());
        }

        @Test
        @DisplayName("Basic constructor sets item type to WEAPON")
        void basicConstructorSetsItemTypeToWeapon() {
            assertEquals(ItemType.WEAPON, basicWeapon.getType());
        }

        @Test
        @DisplayName("Basic constructor initializes range to zero")
        void basicConstructorInitializesRangeToZero() {
            assertEquals(0, basicWeapon.getNormalRange());
            assertEquals(0, basicWeapon.getLongRange());
        }

        @Test
        @DisplayName("Ranged constructor sets range values")
        void rangedConstructorSetsRangeValues() {
            assertEquals(80, rangedWeapon.getNormalRange());
            assertEquals(320, rangedWeapon.getLongRange());
        }

        @Test
        @DisplayName("Minimum damage dice count is 1")
        void minimumDamageDiceCountIsOne() {
            Weapon weapon = new Weapon("Weak", 0, 6, DamageType.BLUDGEONING,
                    WeaponCategory.SIMPLE_MELEE, 1.0, 1);
            
            assertEquals(1, weapon.getDamageDiceCount());
        }

        @Test
        @DisplayName("Minimum damage die size is 1")
        void minimumDamageDieSizeIsOne() {
            Weapon weapon = new Weapon("Weak", 1, 0, DamageType.BLUDGEONING,
                    WeaponCategory.SIMPLE_MELEE, 1.0, 1);
            
            assertEquals(1, weapon.getDamageDieSize());
        }

        @Test
        @DisplayName("Long range cannot be less than normal range")
        void longRangeCannotBeLessThanNormalRange() {
            Weapon weapon = new Weapon("Javelin", 1, 6, DamageType.PIERCING,
                    WeaponCategory.SIMPLE_MELEE, 30, 20, 2.0, 5);
            
            assertEquals(30, weapon.getNormalRange());
            assertEquals(30, weapon.getLongRange()); // Should be clamped to normal
        }

        @Test
        @DisplayName("Each weapon gets unique ID")
        void eachWeaponGetsUniqueId() {
            Weapon weapon1 = Weapon.createLongsword();
            Weapon weapon2 = Weapon.createLongsword();
            
            assertNotEquals(weapon1.getId(), weapon2.getId());
        }
    }

    // ==================== Property Tests ====================

    @Nested
    @DisplayName("Weapon Properties")
    class PropertyTests {

        @Test
        @DisplayName("Weapons start with no properties")
        void weaponsStartWithNoProperties() {
            assertTrue(basicWeapon.getProperties().isEmpty());
        }

        @Test
        @DisplayName("Can add property")
        void canAddProperty() {
            basicWeapon.addProperty(WeaponProperty.FINESSE);
            
            assertTrue(basicWeapon.hasProperty(WeaponProperty.FINESSE));
        }

        @Test
        @DisplayName("Can add multiple properties")
        void canAddMultipleProperties() {
            basicWeapon.addProperty(WeaponProperty.FINESSE);
            basicWeapon.addProperty(WeaponProperty.LIGHT);
            basicWeapon.addProperty(WeaponProperty.THROWN);
            
            Set<WeaponProperty> props = basicWeapon.getProperties();
            
            assertEquals(3, props.size());
            assertTrue(props.contains(WeaponProperty.FINESSE));
            assertTrue(props.contains(WeaponProperty.LIGHT));
            assertTrue(props.contains(WeaponProperty.THROWN));
        }

        @Test
        @DisplayName("Can remove property")
        void canRemoveProperty() {
            basicWeapon.addProperty(WeaponProperty.FINESSE);
            basicWeapon.removeProperty(WeaponProperty.FINESSE);
            
            assertFalse(basicWeapon.hasProperty(WeaponProperty.FINESSE));
        }

        @Test
        @DisplayName("GetProperties returns copy")
        void getPropertiesReturnsCopy() {
            basicWeapon.addProperty(WeaponProperty.LIGHT);
            
            Set<WeaponProperty> props = basicWeapon.getProperties();
            props.clear(); // Modify the returned set
            
            assertTrue(basicWeapon.hasProperty(WeaponProperty.LIGHT)); // Original unchanged
        }
    }

    // ==================== Convenience Property Check Tests ====================

    @Nested
    @DisplayName("Property Convenience Methods")
    class PropertyConvenienceTests {

        @Test
        @DisplayName("isTwoHanded returns true when has TWO_HANDED property")
        void isTwoHandedReturnsTrueWhenHasTwoHandedProperty() {
            assertFalse(basicWeapon.isTwoHanded());
            
            basicWeapon.addProperty(WeaponProperty.TWO_HANDED);
            
            assertTrue(basicWeapon.isTwoHanded());
        }

        @Test
        @DisplayName("isFinesse returns true when has FINESSE property")
        void isFinesseReturnsTrueWhenHasFinesseProperty() {
            assertFalse(basicWeapon.isFinesse());
            
            basicWeapon.addProperty(WeaponProperty.FINESSE);
            
            assertTrue(basicWeapon.isFinesse());
        }

        @Test
        @DisplayName("isLight returns true when has LIGHT property")
        void isLightReturnsTrueWhenHasLightProperty() {
            assertFalse(basicWeapon.isLight());
            
            basicWeapon.addProperty(WeaponProperty.LIGHT);
            
            assertTrue(basicWeapon.isLight());
        }

        @Test
        @DisplayName("isVersatile returns true when has VERSATILE property")
        void isVersatileReturnsTrueWhenHasVersatileProperty() {
            assertFalse(basicWeapon.isVersatile());
            
            basicWeapon.addProperty(WeaponProperty.VERSATILE);
            
            assertTrue(basicWeapon.isVersatile());
        }

        @Test
        @DisplayName("isThrown returns true when has THROWN property")
        void isThrownReturnsTrueWhenHasThrownProperty() {
            assertFalse(basicWeapon.isThrown());
            
            basicWeapon.addProperty(WeaponProperty.THROWN);
            
            assertTrue(basicWeapon.isThrown());
        }

        @Test
        @DisplayName("hasReach returns true when has REACH property")
        void hasReachReturnsTrueWhenHasReachProperty() {
            assertFalse(basicWeapon.hasReach());
            
            basicWeapon.addProperty(WeaponProperty.REACH);
            
            assertTrue(basicWeapon.hasReach());
        }

        @Test
        @DisplayName("isMagical returns true when has MAGICAL property")
        void isMagicalReturnsTrueWhenHasMagicalProperty() {
            assertFalse(basicWeapon.isMagical());
            
            basicWeapon.addProperty(WeaponProperty.MAGICAL);
            
            assertTrue(basicWeapon.isMagical());
        }

        @Test
        @DisplayName("isMagical returns true when has attack bonus")
        void isMagicalReturnsTrueWhenHasAttackBonus() {
            basicWeapon.setAttackBonus(1);
            
            assertTrue(basicWeapon.isMagical());
        }

        @Test
        @DisplayName("isMagical returns true when has damage bonus")
        void isMagicalReturnsTrueWhenHasDamageBonus() {
            basicWeapon.setDamageBonus(1);
            
            assertTrue(basicWeapon.isMagical());
        }
    }

    // ==================== Category Tests ====================

    @Nested
    @DisplayName("Weapon Categories")
    class CategoryTests {

        @Test
        @DisplayName("isMelee returns true for melee weapons")
        void isMeleeReturnsTrueForMeleeWeapons() {
            assertTrue(basicWeapon.isMelee());
            assertFalse(rangedWeapon.isMelee());
        }

        @Test
        @DisplayName("isRanged returns true for ranged weapons")
        void isRangedReturnsTrueForRangedWeapons() {
            assertFalse(basicWeapon.isRanged());
            assertTrue(rangedWeapon.isRanged());
        }

        @Test
        @DisplayName("Category isSimple works correctly")
        void categoryIsSimpleWorksCorrectly() {
            assertTrue(WeaponCategory.SIMPLE_MELEE.isSimple());
            assertTrue(WeaponCategory.SIMPLE_RANGED.isSimple());
            assertFalse(WeaponCategory.MARTIAL_MELEE.isSimple());
            assertFalse(WeaponCategory.MARTIAL_RANGED.isSimple());
        }

        @Test
        @DisplayName("Category isMartial works correctly")
        void categoryIsMartialWorksCorrectly() {
            assertFalse(WeaponCategory.SIMPLE_MELEE.isMartial());
            assertFalse(WeaponCategory.SIMPLE_RANGED.isMartial());
            assertTrue(WeaponCategory.MARTIAL_MELEE.isMartial());
            assertTrue(WeaponCategory.MARTIAL_RANGED.isMartial());
        }

        @Test
        @DisplayName("Category isMelee works correctly")
        void categoryIsMeleeWorksCorrectly() {
            assertTrue(WeaponCategory.SIMPLE_MELEE.isMelee());
            assertFalse(WeaponCategory.SIMPLE_RANGED.isMelee());
            assertTrue(WeaponCategory.MARTIAL_MELEE.isMelee());
            assertFalse(WeaponCategory.MARTIAL_RANGED.isMelee());
        }

        @Test
        @DisplayName("Category isRanged works correctly")
        void categoryIsRangedWorksCorrectly() {
            assertFalse(WeaponCategory.SIMPLE_MELEE.isRanged());
            assertTrue(WeaponCategory.SIMPLE_RANGED.isRanged());
            assertFalse(WeaponCategory.MARTIAL_MELEE.isRanged());
            assertTrue(WeaponCategory.MARTIAL_RANGED.isRanged());
        }
    }

    // ==================== Damage Tests ====================

    @Nested
    @DisplayName("Damage Calculations")
    class DamageTests {

        @Test
        @DisplayName("GetDamageDice returns correct notation")
        void getDamageDiceReturnsCorrectNotation() {
            assertEquals("1d8", basicWeapon.getDamageDice());
        }

        @Test
        @DisplayName("GetDamageDice includes positive damage bonus")
        void getDamageDiceIncludesPositiveDamageBonus() {
            basicWeapon.setDamageBonus(2);
            assertEquals("1d8+2", basicWeapon.getDamageDice());
        }

        @Test
        @DisplayName("GetDamageDice includes negative damage bonus")
        void getDamageDiceIncludesNegativeDamageBonus() {
            basicWeapon.setDamageBonus(-1);
            assertEquals("1d8-1", basicWeapon.getDamageDice());
        }

        @Test
        @DisplayName("Multiple dice shown correctly")
        void multipleDiceShownCorrectly() {
            Weapon greatsword = new Weapon("Greatsword", 2, 6, DamageType.SLASHING,
                    WeaponCategory.MARTIAL_MELEE, 6.0, 50);
            
            assertEquals("2d6", greatsword.getDamageDice());
        }

        @Test
        @DisplayName("GetVersatileDamageDice returns versatile damage when set")
        void getVersatileDamageDiceReturnsVersatileDamageWhenSet() {
            basicWeapon.addProperty(WeaponProperty.VERSATILE);
            basicWeapon.setVersatileDieSize(10);
            
            assertEquals("1d10", basicWeapon.getVersatileDamageDice());
        }

        @Test
        @DisplayName("GetVersatileDamageDice returns normal damage when not versatile")
        void getVersatileDamageDiceReturnsNormalDamageWhenNotVersatile() {
            assertEquals("1d8", basicWeapon.getVersatileDamageDice());
        }

        @Test
        @DisplayName("GetAverageDamage calculates correctly")
        void getAverageDamageCalculatesCorrectly() {
            // 1d8 average = (8+1)/2 = 4.5
            assertEquals(4.5, basicWeapon.getAverageDamage(), 0.001);
        }

        @Test
        @DisplayName("GetAverageDamage includes damage bonus")
        void getAverageDamageIncludesDamageBonus() {
            basicWeapon.setDamageBonus(3);
            // 1d8 + 3 average = 4.5 + 3 = 7.5
            assertEquals(7.5, basicWeapon.getAverageDamage(), 0.001);
        }

        @Test
        @DisplayName("GetAverageDamage for multiple dice")
        void getAverageDamageForMultipleDice() {
            Weapon greatsword = new Weapon("Greatsword", 2, 6, DamageType.SLASHING,
                    WeaponCategory.MARTIAL_MELEE, 6.0, 50);
            // 2d6 average = 2 * (6+1)/2 = 7
            assertEquals(7.0, greatsword.getAverageDamage(), 0.001);
        }
    }

    // ==================== Range Tests ====================

    @Nested
    @DisplayName("Range")
    class RangeTests {

        @Test
        @DisplayName("Can set range values")
        void canSetRangeValues() {
            basicWeapon.setRange(30, 120);
            
            assertEquals(30, basicWeapon.getNormalRange());
            assertEquals(120, basicWeapon.getLongRange());
        }

        @Test
        @DisplayName("Negative normal range becomes zero")
        void negativeNormalRangeBecomesZero() {
            basicWeapon.setNormalRange(-10);
            assertEquals(0, basicWeapon.getNormalRange());
        }

        @Test
        @DisplayName("Long range auto-adjusts to at least normal range")
        void longRangeAutoAdjustsToAtLeastNormalRange() {
            basicWeapon.setRange(60, 30);
            assertEquals(60, basicWeapon.getLongRange());
        }

        @Test
        @DisplayName("Can set individual range values")
        void canSetIndividualRangeValues() {
            basicWeapon.setNormalRange(20);
            basicWeapon.setLongRange(60);
            
            assertEquals(20, basicWeapon.getNormalRange());
            assertEquals(60, basicWeapon.getLongRange());
        }
    }

    // ==================== Magic Bonus Tests ====================

    @Nested
    @DisplayName("Magic Bonuses")
    class MagicBonusTests {

        @Test
        @DisplayName("Can set attack bonus")
        void canSetAttackBonus() {
            basicWeapon.setAttackBonus(2);
            assertEquals(2, basicWeapon.getAttackBonus());
        }

        @Test
        @DisplayName("Can set damage bonus")
        void canSetDamageBonus() {
            basicWeapon.setDamageBonus(3);
            assertEquals(3, basicWeapon.getDamageBonus());
        }

        @Test
        @DisplayName("SetMagicBonus sets both attack and damage")
        void setMagicBonusSetsBothAttackAndDamage() {
            basicWeapon.setMagicBonus(2);
            
            assertEquals(2, basicWeapon.getAttackBonus());
            assertEquals(2, basicWeapon.getDamageBonus());
        }

        @Test
        @DisplayName("SetMagicBonus adds MAGICAL property")
        void setMagicBonusAddsMagicalProperty() {
            basicWeapon.setMagicBonus(1);
            
            assertTrue(basicWeapon.hasProperty(WeaponProperty.MAGICAL));
        }

        @Test
        @DisplayName("Zero magic bonus does not add MAGICAL property")
        void zeroMagicBonusDoesNotAddMagicalProperty() {
            basicWeapon.setMagicBonus(0);
            
            assertFalse(basicWeapon.hasProperty(WeaponProperty.MAGICAL));
        }

        @Test
        @DisplayName("Negative bonuses are allowed")
        void negativeBonusesAreAllowed() {
            basicWeapon.setAttackBonus(-2);
            basicWeapon.setDamageBonus(-1);
            
            assertEquals(-2, basicWeapon.getAttackBonus());
            assertEquals(-1, basicWeapon.getDamageBonus());
        }
    }

    // ==================== Copy Tests ====================

    @Nested
    @DisplayName("Copying")
    class CopyTests {

        @Test
        @DisplayName("Copy preserves ID")
        void copyPreservesId() {
            Weapon copy = (Weapon) basicWeapon.copy();
            assertEquals(basicWeapon.getId(), copy.getId());
        }

        @Test
        @DisplayName("Copy preserves all weapon properties")
        void copyPreservesAllWeaponProperties() {
            basicWeapon.addProperty(WeaponProperty.FINESSE);
            basicWeapon.addProperty(WeaponProperty.LIGHT);
            basicWeapon.setMagicBonus(2);
            basicWeapon.setVersatileDieSize(10);
            basicWeapon.setRarity(Rarity.RARE);
            
            Weapon copy = (Weapon) basicWeapon.copy();
            
            assertEquals(basicWeapon.getDamageDiceCount(), copy.getDamageDiceCount());
            assertEquals(basicWeapon.getDamageDieSize(), copy.getDamageDieSize());
            assertEquals(basicWeapon.getDamageType(), copy.getDamageType());
            assertEquals(basicWeapon.getCategory(), copy.getCategory());
            assertEquals(basicWeapon.getAttackBonus(), copy.getAttackBonus());
            assertEquals(basicWeapon.getDamageBonus(), copy.getDamageBonus());
            assertEquals(basicWeapon.getVersatileDieSize(), copy.getVersatileDieSize());
            assertEquals(basicWeapon.getRarity(), copy.getRarity());
            assertTrue(copy.hasProperty(WeaponProperty.FINESSE));
            assertTrue(copy.hasProperty(WeaponProperty.LIGHT));
        }

        @Test
        @DisplayName("Copy preserves range")
        void copyPreservesRange() {
            Weapon copy = (Weapon) rangedWeapon.copy();
            
            assertEquals(rangedWeapon.getNormalRange(), copy.getNormalRange());
            assertEquals(rangedWeapon.getLongRange(), copy.getLongRange());
        }

        @Test
        @DisplayName("CopyWithNewId creates new ID")
        void copyWithNewIdCreatesNewId() {
            Weapon copy = basicWeapon.copyWithNewId();
            
            assertNotEquals(basicWeapon.getId(), copy.getId());
        }

        @Test
        @DisplayName("Modifying copy does not affect original")
        void modifyingCopyDoesNotAffectOriginal() {
            Weapon copy = (Weapon) basicWeapon.copy();
            
            copy.setMagicBonus(5);
            copy.addProperty(WeaponProperty.SILVERED);
            
            assertEquals(0, basicWeapon.getAttackBonus());
            assertFalse(basicWeapon.hasProperty(WeaponProperty.SILVERED));
        }
    }

    // ==================== Equippable Tests ====================

    @Nested
    @DisplayName("Equippable")
    class EquippableTests {

        @Test
        @DisplayName("Weapons are always equippable")
        void weaponsAreAlwaysEquippable() {
            assertTrue(basicWeapon.isEquippable());
            assertTrue(rangedWeapon.isEquippable());
        }
    }

    // ==================== Display Tests ====================

    @Nested
    @DisplayName("Display Methods")
    class DisplayTests {

        @Test
        @DisplayName("ToString includes name and damage")
        void toStringIncludesNameAndDamage() {
            String str = basicWeapon.toString();
            
            assertTrue(str.contains("Test Sword"));
            assertTrue(str.contains("1d8"));
            assertTrue(str.contains("Slashing"));
        }

        @Test
        @DisplayName("ToString includes range for ranged weapons")
        void toStringIncludesRangeForRangedWeapons() {
            String str = rangedWeapon.toString();
            
            assertTrue(str.contains("80/320"));
        }

        @Test
        @DisplayName("ToString includes magic bonus")
        void toStringIncludesMagicBonus() {
            basicWeapon.setMagicBonus(2);
            String str = basicWeapon.toString();
            
            assertTrue(str.contains("+2"));
        }

        @Test
        @DisplayName("GetDetailedInfo includes all relevant info")
        void getDetailedInfoIncludesAllRelevantInfo() {
            basicWeapon.addProperty(WeaponProperty.VERSATILE);
            basicWeapon.setVersatileDieSize(10);
            basicWeapon.setRarity(Rarity.RARE);
            
            String info = basicWeapon.getDetailedInfo();
            
            assertTrue(info.contains("Test Sword"));
            assertTrue(info.contains("Rare"));
            assertTrue(info.contains("Martial Melee"));
            assertTrue(info.contains("1d8"));
            assertTrue(info.contains("Slashing"));
            assertTrue(info.contains("1d10"));
            assertTrue(info.contains("Versatile"));
        }

        @Test
        @DisplayName("GetDetailedInfo includes range for ranged weapons")
        void getDetailedInfoIncludesRangeForRangedWeapons() {
            String info = rangedWeapon.getDetailedInfo();
            
            assertTrue(info.contains("Range:"));
            assertTrue(info.contains("80"));
            assertTrue(info.contains("320"));
        }
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("Simple Melee Weapons")
    class SimpleMeleeWeaponTests {

        @Test
        @DisplayName("Club is correct")
        void clubIsCorrect() {
            Weapon club = Weapon.createClub();
            
            assertEquals("Club", club.getName());
            assertEquals(1, club.getDamageDiceCount());
            assertEquals(4, club.getDamageDieSize());
            assertEquals(DamageType.BLUDGEONING, club.getDamageType());
            assertEquals(WeaponCategory.SIMPLE_MELEE, club.getCategory());
            assertTrue(club.hasProperty(WeaponProperty.LIGHT));
        }

        @Test
        @DisplayName("Dagger is correct")
        void daggerIsCorrect() {
            Weapon dagger = Weapon.createDagger();
            
            assertEquals("Dagger", dagger.getName());
            assertEquals(1, dagger.getDamageDiceCount());
            assertEquals(4, dagger.getDamageDieSize());
            assertEquals(DamageType.PIERCING, dagger.getDamageType());
            assertTrue(dagger.hasProperty(WeaponProperty.FINESSE));
            assertTrue(dagger.hasProperty(WeaponProperty.LIGHT));
            assertTrue(dagger.hasProperty(WeaponProperty.THROWN));
            assertEquals(20, dagger.getNormalRange());
            assertEquals(60, dagger.getLongRange());
        }

        @Test
        @DisplayName("Quarterstaff is versatile")
        void quarterstaffIsVersatile() {
            Weapon quarterstaff = Weapon.createQuarterstaff();
            
            assertEquals("Quarterstaff", quarterstaff.getName());
            assertEquals("1d6", quarterstaff.getDamageDice());
            assertTrue(quarterstaff.hasProperty(WeaponProperty.VERSATILE));
            assertEquals(8, quarterstaff.getVersatileDieSize());
            assertEquals("1d8", quarterstaff.getVersatileDamageDice());
        }

        @Test
        @DisplayName("Greatclub is two-handed")
        void greatclubIsTwoHanded() {
            Weapon greatclub = Weapon.createGreatclub();
            
            assertTrue(greatclub.isTwoHanded());
        }
    }

    @Nested
    @DisplayName("Simple Ranged Weapons")
    class SimpleRangedWeaponTests {

        @Test
        @DisplayName("Shortbow is correct")
        void shortbowIsCorrect() {
            Weapon shortbow = Weapon.createShortbow();
            
            assertEquals("Shortbow", shortbow.getName());
            assertEquals(1, shortbow.getDamageDiceCount());
            assertEquals(6, shortbow.getDamageDieSize());
            assertEquals(DamageType.PIERCING, shortbow.getDamageType());
            assertEquals(WeaponCategory.SIMPLE_RANGED, shortbow.getCategory());
            assertTrue(shortbow.hasProperty(WeaponProperty.AMMUNITION));
            assertTrue(shortbow.hasProperty(WeaponProperty.TWO_HANDED));
            assertEquals(80, shortbow.getNormalRange());
            assertEquals(320, shortbow.getLongRange());
        }

        @Test
        @DisplayName("Light crossbow has loading property")
        void lightCrossbowHasLoadingProperty() {
            Weapon crossbow = Weapon.createLightCrossbow();
            
            assertTrue(crossbow.hasProperty(WeaponProperty.LOADING));
            assertTrue(crossbow.hasProperty(WeaponProperty.AMMUNITION));
        }
    }

    @Nested
    @DisplayName("Martial Melee Weapons")
    class MartialMeleeWeaponTests {

        @Test
        @DisplayName("Longsword is versatile")
        void longswordIsVersatile() {
            Weapon longsword = Weapon.createLongsword();
            
            assertEquals("Longsword", longsword.getName());
            assertEquals(1, longsword.getDamageDiceCount());
            assertEquals(8, longsword.getDamageDieSize());
            assertEquals(DamageType.SLASHING, longsword.getDamageType());
            assertEquals(WeaponCategory.MARTIAL_MELEE, longsword.getCategory());
            assertTrue(longsword.hasProperty(WeaponProperty.VERSATILE));
            assertEquals(10, longsword.getVersatileDieSize());
        }

        @Test
        @DisplayName("Greatsword is two-handed and heavy")
        void greatswordIsTwoHandedAndHeavy() {
            Weapon greatsword = Weapon.createGreatsword();
            
            assertEquals("Greatsword", greatsword.getName());
            assertEquals(2, greatsword.getDamageDiceCount());
            assertEquals(6, greatsword.getDamageDieSize());
            assertTrue(greatsword.hasProperty(WeaponProperty.TWO_HANDED));
            assertTrue(greatsword.hasProperty(WeaponProperty.HEAVY));
        }

        @Test
        @DisplayName("Rapier is finesse")
        void rapierIsFinesse() {
            Weapon rapier = Weapon.createRapier();
            
            assertEquals("Rapier", rapier.getName());
            assertEquals(8, rapier.getDamageDieSize());
            assertTrue(rapier.hasProperty(WeaponProperty.FINESSE));
            assertFalse(rapier.hasProperty(WeaponProperty.LIGHT)); // Rapier is not light
        }

        @Test
        @DisplayName("Shortsword is finesse and light")
        void shortswordIsFinesseAndLight() {
            Weapon shortsword = Weapon.createShortsword();
            
            assertTrue(shortsword.hasProperty(WeaponProperty.FINESSE));
            assertTrue(shortsword.hasProperty(WeaponProperty.LIGHT));
        }

        @Test
        @DisplayName("Scimitar is finesse and light")
        void scimitarIsFinesseAndLight() {
            Weapon scimitar = Weapon.createScimitar();
            
            assertTrue(scimitar.hasProperty(WeaponProperty.FINESSE));
            assertTrue(scimitar.hasProperty(WeaponProperty.LIGHT));
            assertEquals(DamageType.SLASHING, scimitar.getDamageType());
        }
    }

    @Nested
    @DisplayName("Martial Ranged Weapons")
    class MartialRangedWeaponTests {

        @Test
        @DisplayName("Longbow is correct")
        void longbowIsCorrect() {
            Weapon longbow = Weapon.createLongbow();
            
            assertEquals("Longbow", longbow.getName());
            assertEquals(1, longbow.getDamageDiceCount());
            assertEquals(8, longbow.getDamageDieSize());
            assertEquals(DamageType.PIERCING, longbow.getDamageType());
            assertEquals(WeaponCategory.MARTIAL_RANGED, longbow.getCategory());
            assertTrue(longbow.hasProperty(WeaponProperty.HEAVY));
            assertTrue(longbow.hasProperty(WeaponProperty.TWO_HANDED));
            assertTrue(longbow.hasProperty(WeaponProperty.AMMUNITION));
            assertEquals(150, longbow.getNormalRange());
            assertEquals(600, longbow.getLongRange());
        }

        @Test
        @DisplayName("Hand crossbow is light")
        void handCrossbowIsLight() {
            Weapon handCrossbow = Weapon.createHandCrossbow();
            
            assertEquals("Hand Crossbow", handCrossbow.getName());
            assertTrue(handCrossbow.hasProperty(WeaponProperty.LIGHT));
            assertTrue(handCrossbow.hasProperty(WeaponProperty.LOADING));
            assertFalse(handCrossbow.hasProperty(WeaponProperty.TWO_HANDED));
        }

        @Test
        @DisplayName("Heavy crossbow is correct")
        void heavyCrossbowIsCorrect() {
            Weapon heavyCrossbow = Weapon.createHeavyCrossbow();
            
            assertEquals(10, heavyCrossbow.getDamageDieSize());
            assertTrue(heavyCrossbow.hasProperty(WeaponProperty.HEAVY));
            assertTrue(heavyCrossbow.hasProperty(WeaponProperty.TWO_HANDED));
            assertTrue(heavyCrossbow.hasProperty(WeaponProperty.LOADING));
        }
    }

    // ==================== Enum Tests ====================

    @Nested
    @DisplayName("Enums")
    class EnumTests {

        @ParameterizedTest
        @EnumSource(DamageType.class)
        @DisplayName("All damage types have display names")
        void allDamageTypesHaveDisplayNames(DamageType type) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }

        @ParameterizedTest
        @EnumSource(WeaponCategory.class)
        @DisplayName("All weapon categories have display names")
        void allWeaponCategoriesHaveDisplayNames(WeaponCategory category) {
            assertNotNull(category.getDisplayName());
            assertFalse(category.getDisplayName().isEmpty());
        }

        @ParameterizedTest
        @EnumSource(WeaponProperty.class)
        @DisplayName("All weapon properties have display names")
        void allWeaponPropertiesHaveDisplayNames(WeaponProperty property) {
            assertNotNull(property.getDisplayName());
            assertFalse(property.getDisplayName().isEmpty());
        }
    }
}