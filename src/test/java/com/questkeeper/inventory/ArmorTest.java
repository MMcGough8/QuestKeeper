package com.questkeeper.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.questkeeper.inventory.Item.ItemType;
import com.questkeeper.inventory.Item.Rarity;
import com.questkeeper.inventory.Armor.ArmorCategory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Armor class.
 * 
 * Tests cover armor creation, AC calculations, categories,
 * requirements, and factory methods for standard D&D armor.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Armor")
class ArmorTest {

    private Armor lightArmor;
    private Armor mediumArmor;
    private Armor heavyArmor;
    private Armor shield;

    @BeforeEach
    void setUp() {
        lightArmor = new Armor("Test Light", ArmorCategory.LIGHT, 11, 10.0, 10);
        mediumArmor = new Armor("Test Medium", ArmorCategory.MEDIUM, 14, 20.0, 50);
        heavyArmor = new Armor("Test Heavy", ArmorCategory.HEAVY, 16, 13, true, 55.0, 75);
        shield = new Armor("Test Shield", ArmorCategory.SHIELD, 2, 6.0, 10);
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructors")
    class ConstructorTests {

        @Test
        @DisplayName("Basic constructor sets armor properties")
        void basicConstructorSetsArmorProperties() {
            Armor armor = new Armor("Leather", ArmorCategory.LIGHT, 11, 10.0, 10);
            
            assertEquals("Leather", armor.getName());
            assertEquals(ArmorCategory.LIGHT, armor.getCategory());
            assertEquals(11, armor.getBaseAC());
            assertEquals(10.0, armor.getWeight(), 0.001);
            assertEquals(10, armor.getGoldValue());
        }

        @Test
        @DisplayName("Basic constructor sets item type based on category")
        void basicConstructorSetsItemTypeBasedOnCategory() {
            assertEquals(ItemType.ARMOR, lightArmor.getType());
            assertEquals(ItemType.ARMOR, mediumArmor.getType());
            assertEquals(ItemType.ARMOR, heavyArmor.getType());
            assertEquals(ItemType.SHIELD, shield.getType());
        }

        @Test
        @DisplayName("Basic constructor defaults no strength requirement")
        void basicConstructorDefaultsNoStrengthRequirement() {
            assertEquals(0, lightArmor.getStrengthRequirement());
        }

        @Test
        @DisplayName("Basic constructor defaults no stealth disadvantage")
        void basicConstructorDefaultsNoStealthDisadvantage() {
            assertFalse(lightArmor.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Full constructor sets strength requirement")
        void fullConstructorSetsStrengthRequirement() {
            assertEquals(13, heavyArmor.getStrengthRequirement());
        }

        @Test
        @DisplayName("Full constructor sets stealth disadvantage")
        void fullConstructorSetsStealthDisadvantage() {
            assertTrue(heavyArmor.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Negative base AC becomes zero")
        void negativeBaseAcBecomesZero() {
            Armor armor = new Armor("Broken", ArmorCategory.LIGHT, -5, 1.0, 0);
            assertEquals(0, armor.getBaseAC());
        }

        @Test
        @DisplayName("Negative strength requirement becomes zero")
        void negativeStrengthRequirementBecomesZero() {
            Armor armor = new Armor("Test", ArmorCategory.HEAVY, 16, -5, false, 50.0, 100);
            assertEquals(0, armor.getStrengthRequirement());
        }

        @Test
        @DisplayName("Each armor gets unique ID")
        void eachArmorGetsUniqueId() {
            Armor armor1 = Armor.createLeatherArmor();
            Armor armor2 = Armor.createLeatherArmor();
            
            assertNotEquals(armor1.getId(), armor2.getId());
        }
    }

    // ==================== AC Calculation Tests ====================

    @Nested
    @DisplayName("AC Calculations")
    class AcCalculationTests {

        @Test
        @DisplayName("GetTotalBaseAC returns base AC")
        void getTotalBaseAcReturnsBaseAc() {
            assertEquals(11, lightArmor.getTotalBaseAC());
            assertEquals(14, mediumArmor.getTotalBaseAC());
            assertEquals(16, heavyArmor.getTotalBaseAC());
        }

        @Test
        @DisplayName("GetTotalBaseAC includes magic bonus")
        void getTotalBaseAcIncludesMagicBonus() {
            lightArmor.setAcBonus(2);
            assertEquals(13, lightArmor.getTotalBaseAC());
        }

        @Test
        @DisplayName("Light armor adds full DEX modifier")
        void lightArmorAddsFullDexModifier() {
            assertEquals(11 + 0, lightArmor.calculateAC(0));
            assertEquals(11 + 2, lightArmor.calculateAC(2));
            assertEquals(11 + 5, lightArmor.calculateAC(5));
            assertEquals(11 - 1, lightArmor.calculateAC(-1));
        }

        @Test
        @DisplayName("Medium armor caps DEX modifier at +2")
        void mediumArmorCapsDexModifierAtTwo() {
            assertEquals(14 + 0, mediumArmor.calculateAC(0));
            assertEquals(14 + 1, mediumArmor.calculateAC(1));
            assertEquals(14 + 2, mediumArmor.calculateAC(2));
            assertEquals(14 + 2, mediumArmor.calculateAC(3)); // Capped
            assertEquals(14 + 2, mediumArmor.calculateAC(5)); // Capped
        }

        @Test
        @DisplayName("Medium armor allows negative DEX modifier")
        void mediumArmorAllowsNegativeDexModifier() {
            assertEquals(14 - 1, mediumArmor.calculateAC(-1));
            assertEquals(14 - 2, mediumArmor.calculateAC(-2));
        }

        @Test
        @DisplayName("Heavy armor ignores DEX modifier")
        void heavyArmorIgnoresDexModifier() {
            assertEquals(16, heavyArmor.calculateAC(0));
            assertEquals(16, heavyArmor.calculateAC(2));
            assertEquals(16, heavyArmor.calculateAC(5));
            assertEquals(16, heavyArmor.calculateAC(-2));
        }

        @Test
        @DisplayName("Shield ignores DEX modifier")
        void shieldIgnoresDexModifier() {
            assertEquals(2, shield.calculateAC(0));
            assertEquals(2, shield.calculateAC(3));
        }

        @Test
        @DisplayName("Magic bonus applies to AC calculation")
        void magicBonusAppliesToAcCalculation() {
            lightArmor.setAcBonus(1);
            assertEquals(12 + 3, lightArmor.calculateAC(3));
            
            heavyArmor.setAcBonus(2);
            assertEquals(18, heavyArmor.calculateAC(5)); // DEX ignored for heavy
        }

        @Test
        @DisplayName("GetShieldBonus returns AC for shields")
        void getShieldBonusReturnsAcForShields() {
            assertEquals(2, shield.getShieldBonus());
        }

        @Test
        @DisplayName("GetShieldBonus returns zero for non-shields")
        void getShieldBonusReturnsZeroForNonShields() {
            assertEquals(0, lightArmor.getShieldBonus());
            assertEquals(0, mediumArmor.getShieldBonus());
            assertEquals(0, heavyArmor.getShieldBonus());
        }

        @Test
        @DisplayName("GetShieldBonus includes magic bonus")
        void getShieldBonusIncludesMagicBonus() {
            shield.setAcBonus(1);
            assertEquals(3, shield.getShieldBonus());
        }
    }

    // ==================== Category Tests ====================

    @Nested
    @DisplayName("Armor Categories")
    class CategoryTests {

        @Test
        @DisplayName("isShield returns true only for shields")
        void isShieldReturnsTrueOnlyForShields() {
            assertFalse(lightArmor.isShield());
            assertFalse(mediumArmor.isShield());
            assertFalse(heavyArmor.isShield());
            assertTrue(shield.isShield());
        }

        @Test
        @DisplayName("isLightArmor returns true only for light armor")
        void isLightArmorReturnsTrueOnlyForLightArmor() {
            assertTrue(lightArmor.isLightArmor());
            assertFalse(mediumArmor.isLightArmor());
            assertFalse(heavyArmor.isLightArmor());
            assertFalse(shield.isLightArmor());
        }

        @Test
        @DisplayName("isMediumArmor returns true only for medium armor")
        void isMediumArmorReturnsTrueOnlyForMediumArmor() {
            assertFalse(lightArmor.isMediumArmor());
            assertTrue(mediumArmor.isMediumArmor());
            assertFalse(heavyArmor.isMediumArmor());
            assertFalse(shield.isMediumArmor());
        }

        @Test
        @DisplayName("isHeavyArmor returns true only for heavy armor")
        void isHeavyArmorReturnsTrueOnlyForHeavyArmor() {
            assertFalse(lightArmor.isHeavyArmor());
            assertFalse(mediumArmor.isHeavyArmor());
            assertTrue(heavyArmor.isHeavyArmor());
            assertFalse(shield.isHeavyArmor());
        }

        @Test
        @DisplayName("Category allows DEX bonus correctly")
        void categoryAllowsDexBonusCorrectly() {
            assertTrue(ArmorCategory.LIGHT.allowsDexBonus());
            assertTrue(ArmorCategory.MEDIUM.allowsDexBonus());
            assertFalse(ArmorCategory.HEAVY.allowsDexBonus());
            assertFalse(ArmorCategory.SHIELD.allowsDexBonus());
        }

        @Test
        @DisplayName("Category max DEX bonus is correct")
        void categoryMaxDexBonusIsCorrect() {
            assertEquals(-1, ArmorCategory.LIGHT.getMaxDexBonus()); // Unlimited
            assertEquals(2, ArmorCategory.MEDIUM.getMaxDexBonus());
            assertEquals(0, ArmorCategory.HEAVY.getMaxDexBonus());
            assertEquals(0, ArmorCategory.SHIELD.getMaxDexBonus());
        }

        @Test
        @DisplayName("Can change category")
        void canChangeCategory() {
            lightArmor.setCategory(ArmorCategory.MEDIUM);
            
            assertEquals(ArmorCategory.MEDIUM, lightArmor.getCategory());
            assertTrue(lightArmor.isMediumArmor());
        }

        @Test
        @DisplayName("Changing to shield updates item type")
        void changingToShieldUpdatesItemType() {
            lightArmor.setCategory(ArmorCategory.SHIELD);
            
            assertEquals(ItemType.SHIELD, lightArmor.getType());
        }
    }

    // ==================== Strength Requirement Tests ====================

    @Nested
    @DisplayName("Strength Requirements")
    class StrengthRequirementTests {

        @Test
        @DisplayName("MeetsStrengthRequirement returns true when no requirement")
        void meetsStrengthRequirementReturnsTrueWhenNoRequirement() {
            assertTrue(lightArmor.meetsStrengthRequirement(8));
            assertTrue(lightArmor.meetsStrengthRequirement(20));
        }

        @Test
        @DisplayName("MeetsStrengthRequirement checks correctly")
        void meetsStrengthRequirementChecksCorrectly() {
            assertTrue(heavyArmor.meetsStrengthRequirement(13));
            assertTrue(heavyArmor.meetsStrengthRequirement(15));
            assertFalse(heavyArmor.meetsStrengthRequirement(12));
            assertFalse(heavyArmor.meetsStrengthRequirement(10));
        }

        @Test
        @DisplayName("GetSpeedPenalty returns zero when no requirement")
        void getSpeedPenaltyReturnsZeroWhenNoRequirement() {
            assertEquals(0, lightArmor.getSpeedPenalty(8));
        }

        @Test
        @DisplayName("GetSpeedPenalty returns zero when requirement met")
        void getSpeedPenaltyReturnsZeroWhenRequirementMet() {
            assertEquals(0, heavyArmor.getSpeedPenalty(13));
            assertEquals(0, heavyArmor.getSpeedPenalty(20));
        }

        @Test
        @DisplayName("GetSpeedPenalty returns 10 when requirement not met")
        void getSpeedPenaltyReturnsTenWhenRequirementNotMet() {
            assertEquals(10, heavyArmor.getSpeedPenalty(12));
            assertEquals(10, heavyArmor.getSpeedPenalty(8));
        }

        @Test
        @DisplayName("Can set strength requirement")
        void canSetStrengthRequirement() {
            lightArmor.setStrengthRequirement(15);
            assertEquals(15, lightArmor.getStrengthRequirement());
        }
    }

    // ==================== Stealth Tests ====================

    @Nested
    @DisplayName("Stealth Disadvantage")
    class StealthTests {

        @Test
        @DisplayName("Can set stealth disadvantage")
        void canSetStealthDisadvantage() {
            lightArmor.setStealthDisadvantage(true);
            assertTrue(lightArmor.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Can remove stealth disadvantage")
        void canRemoveStealthDisadvantage() {
            heavyArmor.setStealthDisadvantage(false);
            assertFalse(heavyArmor.hasStealthDisadvantage());
        }
    }

    // ==================== Magic Bonus Tests ====================

    @Nested
    @DisplayName("Magic Bonuses")
    class MagicBonusTests {

        @Test
        @DisplayName("Armor starts with no magic bonus")
        void armorStartsWithNoMagicBonus() {
            assertEquals(0, lightArmor.getAcBonus());
        }

        @Test
        @DisplayName("Can set AC bonus")
        void canSetAcBonus() {
            lightArmor.setAcBonus(2);
            assertEquals(2, lightArmor.getAcBonus());
        }

        @Test
        @DisplayName("SetMagicBonus sets AC bonus")
        void setMagicBonusSetsAcBonus() {
            lightArmor.setMagicBonus(3);
            assertEquals(3, lightArmor.getAcBonus());
        }

        @Test
        @DisplayName("isMagical returns true when has AC bonus")
        void isMagicalReturnsTrueWhenHasAcBonus() {
            assertFalse(lightArmor.isMagical());
            
            lightArmor.setAcBonus(1);
            
            assertTrue(lightArmor.isMagical());
        }

        @Test
        @DisplayName("Negative AC bonus is allowed")
        void negativeAcBonusIsAllowed() {
            lightArmor.setAcBonus(-1);
            assertEquals(-1, lightArmor.getAcBonus());
            assertEquals(10, lightArmor.getTotalBaseAC());
        }
    }

    // ==================== Copy Tests ====================

    @Nested
    @DisplayName("Copying")
    class CopyTests {

        @Test
        @DisplayName("Copy preserves ID")
        void copyPreservesId() {
            Armor copy = (Armor) heavyArmor.copy();
            assertEquals(heavyArmor.getId(), copy.getId());
        }

        @Test
        @DisplayName("Copy preserves all armor properties")
        void copyPreservesAllArmorProperties() {
            heavyArmor.setAcBonus(2);
            heavyArmor.setRarity(Rarity.RARE);
            heavyArmor.setDescription("Magical plate");
            
            Armor copy = (Armor) heavyArmor.copy();
            
            assertEquals(heavyArmor.getCategory(), copy.getCategory());
            assertEquals(heavyArmor.getBaseAC(), copy.getBaseAC());
            assertEquals(heavyArmor.getStrengthRequirement(), copy.getStrengthRequirement());
            assertEquals(heavyArmor.hasStealthDisadvantage(), copy.hasStealthDisadvantage());
            assertEquals(heavyArmor.getAcBonus(), copy.getAcBonus());
            assertEquals(heavyArmor.getRarity(), copy.getRarity());
            assertEquals(heavyArmor.getDescription(), copy.getDescription());
        }

        @Test
        @DisplayName("CopyWithNewId creates new ID")
        void copyWithNewIdCreatesNewId() {
            Armor copy = heavyArmor.copyWithNewId();
            
            assertNotEquals(heavyArmor.getId(), copy.getId());
        }

        @Test
        @DisplayName("Modifying copy does not affect original")
        void modifyingCopyDoesNotAffectOriginal() {
            Armor copy = (Armor) lightArmor.copy();
            
            copy.setAcBonus(5);
            copy.setStealthDisadvantage(true);
            
            assertEquals(0, lightArmor.getAcBonus());
            assertFalse(lightArmor.hasStealthDisadvantage());
        }
    }

    // ==================== Equippable Tests ====================

    @Nested
    @DisplayName("Equippable")
    class EquippableTests {

        @Test
        @DisplayName("All armor types are equippable")
        void allArmorTypesAreEquippable() {
            assertTrue(lightArmor.isEquippable());
            assertTrue(mediumArmor.isEquippable());
            assertTrue(heavyArmor.isEquippable());
            assertTrue(shield.isEquippable());
        }
    }

    // ==================== Display Tests ====================

    @Nested
    @DisplayName("Display Methods")
    class DisplayTests {

        @Test
        @DisplayName("ToString includes name and AC")
        void toStringIncludesNameAndAc() {
            String str = lightArmor.toString();
            
            assertTrue(str.contains("Test Light"));
            assertTrue(str.contains("11") || str.contains("AC"));
        }

        @Test
        @DisplayName("ToString shows shield format for shields")
        void toStringShowsShieldFormatForShields() {
            String str = shield.toString();
            
            assertTrue(str.contains("Shield"));
            assertTrue(str.contains("+2"));
        }

        @Test
        @DisplayName("ToString includes magic bonus")
        void toStringIncludesMagicBonus() {
            lightArmor.setAcBonus(1);
            String str = lightArmor.toString();
            
            assertTrue(str.contains("+1"));
        }

        @Test
        @DisplayName("GetDetailedInfo includes all relevant info")
        void getDetailedInfoIncludesAllRelevantInfo() {
            heavyArmor.setRarity(Rarity.UNCOMMON);
            heavyArmor.setDescription("Well-crafted armor");
            
            String info = heavyArmor.getDetailedInfo();
            
            assertTrue(info.contains("Test Heavy"));
            assertTrue(info.contains("Uncommon"));
            assertTrue(info.contains("Heavy Armor"));
            assertTrue(info.contains("16"));
            assertTrue(info.contains("13")); // Strength requirement
            assertTrue(info.contains("Stealth") || info.contains("Disadvantage"));
            assertTrue(info.contains("Well-crafted armor"));
        }

        @Test
        @DisplayName("GetDetailedInfo shows DEX info for light armor")
        void getDetailedInfoShowsDexInfoForLightArmor() {
            String info = lightArmor.getDetailedInfo();
            
            assertTrue(info.contains("DEX"));
        }

        @Test
        @DisplayName("GetDetailedInfo shows DEX cap for medium armor")
        void getDetailedInfoShowsDexCapForMediumArmor() {
            String info = mediumArmor.getDetailedInfo();
            
            assertTrue(info.contains("DEX"));
            assertTrue(info.contains("max") || info.contains("2"));
        }

        @Test
        @DisplayName("Shield detailed info shows bonus format")
        void shieldDetailedInfoShowsBonusFormat() {
            String info = shield.getDetailedInfo();
            
            assertTrue(info.contains("+2") || info.contains("AC: +"));
        }
    }

    // ==================== Factory Method Tests - Light Armor ====================

    @Nested
    @DisplayName("Light Armor Factory Methods")
    class LightArmorFactoryTests {

        @Test
        @DisplayName("Padded armor is correct")
        void paddedArmorIsCorrect() {
            Armor padded = Armor.createPaddedArmor();
            
            assertEquals("Padded Armor", padded.getName());
            assertEquals(ArmorCategory.LIGHT, padded.getCategory());
            assertEquals(11, padded.getBaseAC());
            assertTrue(padded.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Leather armor is correct")
        void leatherArmorIsCorrect() {
            Armor leather = Armor.createLeatherArmor();
            
            assertEquals("Leather Armor", leather.getName());
            assertEquals(ArmorCategory.LIGHT, leather.getCategory());
            assertEquals(11, leather.getBaseAC());
            assertFalse(leather.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Studded leather is correct")
        void studdedLeatherIsCorrect() {
            Armor studded = Armor.createStuddedLeather();
            
            assertEquals("Studded Leather", studded.getName());
            assertEquals(ArmorCategory.LIGHT, studded.getCategory());
            assertEquals(12, studded.getBaseAC());
            assertFalse(studded.hasStealthDisadvantage());
        }
    }

    // ==================== Factory Method Tests - Medium Armor ====================

    @Nested
    @DisplayName("Medium Armor Factory Methods")
    class MediumArmorFactoryTests {

        @Test
        @DisplayName("Hide armor is correct")
        void hideArmorIsCorrect() {
            Armor hide = Armor.createHide();
            
            assertEquals("Hide Armor", hide.getName());
            assertEquals(ArmorCategory.MEDIUM, hide.getCategory());
            assertEquals(12, hide.getBaseAC());
        }

        @Test
        @DisplayName("Chain shirt is correct")
        void chainShirtIsCorrect() {
            Armor chainShirt = Armor.createChainShirt();
            
            assertEquals("Chain Shirt", chainShirt.getName());
            assertEquals(ArmorCategory.MEDIUM, chainShirt.getCategory());
            assertEquals(13, chainShirt.getBaseAC());
            assertFalse(chainShirt.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Scale mail has stealth disadvantage")
        void scaleMailHasStealthDisadvantage() {
            Armor scaleMail = Armor.createScaleMail();
            
            assertEquals("Scale Mail", scaleMail.getName());
            assertEquals(14, scaleMail.getBaseAC());
            assertTrue(scaleMail.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Breastplate is correct")
        void breastplateIsCorrect() {
            Armor breastplate = Armor.createBreastplate();
            
            assertEquals("Breastplate", breastplate.getName());
            assertEquals(14, breastplate.getBaseAC());
            assertFalse(breastplate.hasStealthDisadvantage());
            assertEquals(400, breastplate.getGoldValue());
        }

        @Test
        @DisplayName("Half plate has stealth disadvantage")
        void halfPlateHasStealthDisadvantage() {
            Armor halfPlate = Armor.createHalfPlate();
            
            assertEquals("Half Plate", halfPlate.getName());
            assertEquals(15, halfPlate.getBaseAC());
            assertTrue(halfPlate.hasStealthDisadvantage());
        }
    }

    // ==================== Factory Method Tests - Heavy Armor ====================

    @Nested
    @DisplayName("Heavy Armor Factory Methods")
    class HeavyArmorFactoryTests {

        @Test
        @DisplayName("Ring mail is correct")
        void ringMailIsCorrect() {
            Armor ringMail = Armor.createRingMail();
            
            assertEquals("Ring Mail", ringMail.getName());
            assertEquals(ArmorCategory.HEAVY, ringMail.getCategory());
            assertEquals(14, ringMail.getBaseAC());
            assertTrue(ringMail.hasStealthDisadvantage());
            assertEquals(0, ringMail.getStrengthRequirement()); // Ring mail has no STR req
        }

        @Test
        @DisplayName("Chain mail has strength requirement")
        void chainMailHasStrengthRequirement() {
            Armor chainMail = Armor.createChainMail();
            
            assertEquals("Chain Mail", chainMail.getName());
            assertEquals(16, chainMail.getBaseAC());
            assertEquals(13, chainMail.getStrengthRequirement());
            assertTrue(chainMail.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Splint armor is correct")
        void splintArmorIsCorrect() {
            Armor splint = Armor.createSplint();
            
            assertEquals("Splint Armor", splint.getName());
            assertEquals(17, splint.getBaseAC());
            assertEquals(15, splint.getStrengthRequirement());
            assertTrue(splint.hasStealthDisadvantage());
        }

        @Test
        @DisplayName("Plate armor is correct")
        void plateArmorIsCorrect() {
            Armor plate = Armor.createPlate();
            
            assertEquals("Plate Armor", plate.getName());
            assertEquals(ArmorCategory.HEAVY, plate.getCategory());
            assertEquals(18, plate.getBaseAC());
            assertEquals(15, plate.getStrengthRequirement());
            assertTrue(plate.hasStealthDisadvantage());
            assertEquals(1500, plate.getGoldValue());
        }
    }

    // ==================== Factory Method Tests - Shields ====================

    @Nested
    @DisplayName("Shield Factory Methods")
    class ShieldFactoryTests {

        @Test
        @DisplayName("Basic shield is correct")
        void basicShieldIsCorrect() {
            Armor shield = Armor.createShield();
            
            assertEquals("Shield", shield.getName());
            assertEquals(ArmorCategory.SHIELD, shield.getCategory());
            assertEquals(2, shield.getBaseAC());
            assertEquals(2, shield.getShieldBonus());
            assertEquals(6.0, shield.getWeight(), 0.001);
            assertEquals(10, shield.getGoldValue());
        }

        @Test
        @DisplayName("Named shield factory works")
        void namedShieldFactoryWorks() {
            Armor namedShield = Armor.createShield("Tower Shield", 15.0, 50);
            
            assertEquals("Tower Shield", namedShield.getName());
            assertEquals(ArmorCategory.SHIELD, namedShield.getCategory());
            assertEquals(2, namedShield.getShieldBonus());
            assertEquals(15.0, namedShield.getWeight(), 0.001);
            assertEquals(50, namedShield.getGoldValue());
        }
    }

    // ==================== Enum Tests ====================

    @Nested
    @DisplayName("ArmorCategory Enum")
    class EnumTests {

        @ParameterizedTest
        @EnumSource(ArmorCategory.class)
        @DisplayName("All armor categories have display names")
        void allArmorCategoriesHaveDisplayNames(ArmorCategory category) {
            assertNotNull(category.getDisplayName());
            assertFalse(category.getDisplayName().isEmpty());
        }

        @Test
        @DisplayName("Armor categories have correct display names")
        void armorCategoriesHaveCorrectDisplayNames() {
            assertEquals("Light Armor", ArmorCategory.LIGHT.getDisplayName());
            assertEquals("Medium Armor", ArmorCategory.MEDIUM.getDisplayName());
            assertEquals("Heavy Armor", ArmorCategory.HEAVY.getDisplayName());
            assertEquals("Shield", ArmorCategory.SHIELD.getDisplayName());
        }
    }

    // ==================== AC Calculation Parameterized Tests ====================

    @Nested
    @DisplayName("Parameterized AC Tests")
    class ParameterizedAcTests {

        @ParameterizedTest
        @CsvSource({
            "LIGHT, 11, -2, 9",
            "LIGHT, 11, 0, 11",
            "LIGHT, 11, 3, 14",
            "LIGHT, 11, 5, 16",
            "MEDIUM, 14, -1, 13",
            "MEDIUM, 14, 0, 14",
            "MEDIUM, 14, 2, 16",
            "MEDIUM, 14, 4, 16",  // Capped at +2
            "HEAVY, 18, -2, 18",  // DEX ignored
            "HEAVY, 18, 0, 18",
            "HEAVY, 18, 5, 18",
            "SHIELD, 2, 3, 2"     // DEX ignored
        })
        @DisplayName("AC calculation is correct for various scenarios")
        void acCalculationIsCorrect(ArmorCategory category, int baseAC, int dexMod, int expectedAC) {
            Armor armor = new Armor("Test", category, baseAC, 10.0, 50);
            assertEquals(expectedAC, armor.calculateAC(dexMod));
        }
    }
}