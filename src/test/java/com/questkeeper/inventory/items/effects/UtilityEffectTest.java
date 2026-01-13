package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UtilityEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("UtilityEffect")
class UtilityEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    @Test
    @DisplayName("darkvision with range")
    void darkvisionWithRange() {
        UtilityEffect effect = UtilityEffect.createGogglesOfNight();

        assertEquals(UtilityEffect.UtilityType.DARKVISION, effect.getUtilityType());
        assertEquals(60, effect.getVisionRange());
    }

    @Test
    @DisplayName("Whispering Stone factory method")
    void whisperingStone() {
        UtilityEffect effect = UtilityEffect.createWhisperingStone();

        assertEquals(UtilityEffect.UtilityType.PARTY_COMMUNICATION, effect.getUtilityType());
        assertEquals(UsageType.DAILY, effect.getUsageType());
        assertEquals(100, effect.getRange());
    }

    @Test
    @DisplayName("Flash Powder Orb is consumable")
    void flashPowderConsumable() {
        UtilityEffect effect = UtilityEffect.createFlashPowderOrb();

        assertEquals(UsageType.CONSUMABLE, effect.getUsageType());
    }

    @Test
    @DisplayName("grants vision type correctly")
    void grantsVisionType() {
        UtilityEffect effect = UtilityEffect.createGogglesOfNight();

        assertTrue(effect.grantsVision(UtilityEffect.UtilityType.DARKVISION));
        assertFalse(effect.grantsVision(UtilityEffect.UtilityType.TRUESIGHT));
    }

    @Test
    @DisplayName("setUtilityType updates type")
    void setUtilityTypeUpdates() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.LIGHT);
        effect.setUtilityType(UtilityEffect.UtilityType.DARKVISION);

        assertEquals(UtilityEffect.UtilityType.DARKVISION, effect.getUtilityType());
    }

    @Test
    @DisplayName("setRange updates range")
    void setRangeUpdates() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.DARKVISION);
        effect.setRange(120);

        assertEquals(120, effect.getRange());
    }

    @Test
    @DisplayName("setRange clamps negative values to zero")
    void setRangeClampsNegative() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.DARKVISION);
        effect.setRange(-50);

        assertEquals(0, effect.getRange());
    }

    @Test
    @DisplayName("setDuration updates duration")
    void setDurationUpdates() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.TRUESIGHT);
        effect.setDuration(10);

        assertEquals(10, effect.getDuration());
    }

    @Test
    @DisplayName("setDuration clamps negative values to zero")
    void setDurationClampsNegative() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.TRUESIGHT);
        effect.setDuration(-10);

        assertEquals(0, effect.getDuration());
    }

    @Test
    @DisplayName("setSpecificValue updates specific value")
    void setSpecificValueUpdates() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.PRODUCE_ITEM);
        effect.setSpecificValue("gold coins");

        assertEquals("gold coins", effect.getSpecificValue());
    }

    @Test
    @DisplayName("setRequiresAttunement updates attunement")
    void setRequiresAttunementUpdates() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.DARKVISION);
        assertFalse(effect.requiresAttunement());

        effect.setRequiresAttunement(true);
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("getVisionRange returns range for vision types")
    void getVisionRangeForVisionTypes() {
        UtilityEffect darkvision = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.DARKVISION, 90);
        assertEquals(90, darkvision.getVisionRange());

        UtilityEffect truesight = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.TRUESIGHT, 30);
        assertEquals(30, truesight.getVisionRange());

        UtilityEffect blindsight = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.BLINDSIGHT, 60);
        assertEquals(60, blindsight.getVisionRange());

        UtilityEffect tremorsense = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.TREMORSENSE, 30);
        assertEquals(30, tremorsense.getVisionRange());
    }

    @Test
    @DisplayName("getVisionRange returns 0 for non-vision types")
    void getVisionRangeZeroForNonVision() {
        UtilityEffect effect = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.COMPREHEND_LANGUAGES);
        assertEquals(0, effect.getVisionRange());

        UtilityEffect light = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.LIGHT, 30);
        assertEquals(0, light.getVisionRange());
    }

    @Test
    @DisplayName("getDetailedInfo includes all properties")
    void getDetailedInfoIncludesProperties() {
        UtilityEffect effect = UtilityEffect.createGemOfSeeing();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("True Seeing"));
        assertTrue(info.contains("Truesight"));
        assertTrue(info.contains("120"));
        assertTrue(info.contains("Requires attunement"));
    }

    @Test
    @DisplayName("getDetailedInfo includes duration when set")
    void getDetailedInfoIncludesDuration() {
        UtilityEffect effect = UtilityEffect.createGemOfSeeing();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Duration"));
        assertTrue(info.contains("10"));
    }

    @Test
    @DisplayName("getDetailedInfo includes specific value when set")
    void getDetailedInfoIncludesSpecificValue() {
        UtilityEffect effect = UtilityEffect.createBagOfHolding();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Specific"));
        assertTrue(info.contains("500"));
    }

    @Test
    @DisplayName("UtilityType enum has display names")
    void utilityTypeHasDisplayNames() {
        for (UtilityEffect.UtilityType type : UtilityEffect.UtilityType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("createGogglesOfNight factory method")
    void createGogglesOfNightFactory() {
        UtilityEffect effect = UtilityEffect.createGogglesOfNight();

        assertEquals("goggles_night_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.DARKVISION, effect.getUtilityType());
        assertEquals(60, effect.getRange());
        assertEquals(UsageType.PASSIVE, effect.getUsageType());
    }

    @Test
    @DisplayName("createHelmOfComprehendingLanguages factory method")
    void createHelmOfComprehendingLanguagesFactory() {
        UtilityEffect effect = UtilityEffect.createHelmOfComprehendingLanguages();

        assertEquals("helm_languages_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.COMPREHEND_LANGUAGES, effect.getUtilityType());
    }

    @Test
    @DisplayName("createGemOfSeeing factory method")
    void createGemOfSeeingFactory() {
        UtilityEffect effect = UtilityEffect.createGemOfSeeing();

        assertEquals("gem_seeing_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.TRUESIGHT, effect.getUtilityType());
        assertEquals(120, effect.getRange());
        assertEquals(UsageType.CHARGES, effect.getUsageType());
        assertEquals(3, effect.getMaxCharges());
        assertEquals(10, effect.getDuration());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createLanternOfRevealing factory method")
    void createLanternOfRevealingFactory() {
        UtilityEffect effect = UtilityEffect.createLanternOfRevealing();

        assertEquals("lantern_revealing_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.SEE_INVISIBILITY, effect.getUtilityType());
        assertEquals(30, effect.getRange());
    }

    @Test
    @DisplayName("createBagOfHolding factory method")
    void createBagOfHoldingFactory() {
        UtilityEffect effect = UtilityEffect.createBagOfHolding();

        assertEquals("bag_holding_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.STORAGE, effect.getUtilityType());
        assertNotNull(effect.getSpecificValue());
        assertTrue(effect.getSpecificValue().contains("500"));
    }

    @Test
    @DisplayName("createDecanterOfEndlessWater factory method")
    void createDecanterOfEndlessWaterFactory() {
        UtilityEffect effect = UtilityEffect.createDecanterOfEndlessWater();

        assertEquals("decanter_water_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.PRODUCE_ITEM, effect.getUtilityType());
        assertEquals("water", effect.getSpecificValue());
    }

    @Test
    @DisplayName("createRingOfMindShielding factory method")
    void createRingOfMindShieldingFactory() {
        UtilityEffect effect = UtilityEffect.createRingOfMindShielding();

        assertEquals("ring_mind_shield_effect", effect.getId());
        assertEquals(UtilityEffect.UtilityType.TELEPATHY, effect.getUtilityType());
        assertEquals(0, effect.getRange());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("use returns appropriate message for different types")
    void useReturnsAppropriateMessage() {
        UtilityEffect darkvision = UtilityEffect.createGogglesOfNight();
        String result = darkvision.use(testUser);
        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("dark") || result.contains("see"));

        UtilityEffect languages = UtilityEffect.createHelmOfComprehendingLanguages();
        String langResult = languages.use(testUser);
        assertTrue(langResult.contains("languages"));
    }

    @Test
    @DisplayName("constructor with three parameters sets defaults")
    void constructorThreeParams() {
        UtilityEffect effect = new UtilityEffect("test", "Test", UtilityEffect.UtilityType.LIGHT);

        assertEquals(60, effect.getRange()); // Default range
        assertEquals(0, effect.getDuration());
        assertNull(effect.getSpecificValue());
        assertFalse(effect.requiresAttunement());
    }

    @Test
    @DisplayName("constructor with four parameters sets range")
    void constructorFourParams() {
        UtilityEffect effect = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.DARKVISION, 120);

        assertEquals(120, effect.getRange());
        assertEquals(UsageType.PASSIVE, effect.getUsageType());
    }

    @Test
    @DisplayName("constructor with six parameters sets all values")
    void constructorSixParams() {
        UtilityEffect effect = new UtilityEffect("test", "Test",
                UtilityEffect.UtilityType.TRUESIGHT, 30,
                UsageType.DAILY, 3);

        assertEquals(30, effect.getRange());
        assertEquals(UsageType.DAILY, effect.getUsageType());
        assertEquals(3, effect.getMaxCharges());
    }
}
