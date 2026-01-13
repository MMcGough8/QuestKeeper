package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbilitySetEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("AbilitySetEffect")
class AbilitySetEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    @Test
    @DisplayName("sets ability score correctly")
    void setsAbilityScore() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();

        assertEquals(AbilitySetEffect.Ability.STRENGTH, effect.getAbility());
        assertEquals(19, effect.getSetValue());
    }

    @Test
    @DisplayName("effective score calculation")
    void effectiveScore() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();

        assertEquals(19, effect.getEffectiveScore(10)); // Lower score improved
        assertEquals(20, effect.getEffectiveScore(20)); // Higher score unchanged
    }

    @Test
    @DisplayName("would improve score check")
    void wouldImproveScore() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();

        assertTrue(effect.wouldImproveScore(10));
        assertTrue(effect.wouldImproveScore(18));
        assertFalse(effect.wouldImproveScore(19));
        assertFalse(effect.wouldImproveScore(20));
    }

    @Test
    @DisplayName("modifier calculation")
    void modifierCalculation() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();
        assertEquals(4, effect.getSetModifier()); // 19 STR = +4 mod
    }

    @Test
    @DisplayName("setAbility updates ability and description")
    void setAbilityUpdates() {
        AbilitySetEffect effect = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 19);
        effect.setAbility(AbilitySetEffect.Ability.DEXTERITY);

        assertEquals(AbilitySetEffect.Ability.DEXTERITY, effect.getAbility());
        assertTrue(effect.getDescription().contains("Dexterity"));
    }

    @Test
    @DisplayName("setSetValue updates value and description")
    void setSetValueUpdates() {
        AbilitySetEffect effect = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 19);
        effect.setSetValue(21);

        assertEquals(21, effect.getSetValue());
        assertTrue(effect.getDescription().contains("21"));
    }

    @Test
    @DisplayName("setSetValue clamps to valid range (1-30)")
    void setSetValueClampsRange() {
        AbilitySetEffect effect = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 19);

        effect.setSetValue(0);
        assertEquals(1, effect.getSetValue());

        effect.setSetValue(35);
        assertEquals(30, effect.getSetValue());
    }

    @Test
    @DisplayName("constructor clamps value to valid range")
    void constructorClampsRange() {
        AbilitySetEffect lowEffect = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, -5);
        assertEquals(1, lowEffect.getSetValue());

        AbilitySetEffect highEffect = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 50);
        assertEquals(30, highEffect.getSetValue());
    }

    @Test
    @DisplayName("setRequiresAttunement updates attunement")
    void setRequiresAttunementUpdates() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();
        assertTrue(effect.requiresAttunement()); // Default is true

        effect.setRequiresAttunement(false);
        assertFalse(effect.requiresAttunement());
    }

    @Test
    @DisplayName("getDetailedInfo includes all properties")
    void getDetailedInfoIncludesProperties() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Ogre Power"));
        assertTrue(info.contains("Strength"));
        assertTrue(info.contains("19"));
        assertTrue(info.contains("+4")); // modifier
        assertTrue(info.contains("Requires attunement"));
    }

    @Test
    @DisplayName("modifier calculation for various scores")
    void modifierCalculationVarious() {
        // STR 10 = +0
        AbilitySetEffect effect10 = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 10);
        assertEquals(0, effect10.getSetModifier());

        // STR 8 = -1
        AbilitySetEffect effect8 = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 8);
        assertEquals(-1, effect8.getSetModifier());

        // STR 20 = +5
        AbilitySetEffect effect20 = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 20);
        assertEquals(5, effect20.getSetModifier());

        // STR 30 = +10
        AbilitySetEffect effect30 = new AbilitySetEffect("test", "Test",
                AbilitySetEffect.Ability.STRENGTH, 30);
        assertEquals(10, effect30.getSetModifier());
    }

    @Test
    @DisplayName("Ability enum has display names")
    void abilityEnumHasDisplayNames() {
        for (AbilitySetEffect.Ability ability : AbilitySetEffect.Ability.values()) {
            assertNotNull(ability.getDisplayName());
            assertFalse(ability.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("createHeadbandOfIntellect factory method")
    void createHeadbandOfIntellectFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createHeadbandOfIntellect();

        assertEquals("headband_intellect_effect", effect.getId());
        assertEquals(AbilitySetEffect.Ability.INTELLIGENCE, effect.getAbility());
        assertEquals(19, effect.getSetValue());
    }

    @Test
    @DisplayName("createAmuletOfHealth factory method")
    void createAmuletOfHealthFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createAmuletOfHealth();

        assertEquals("amulet_health_effect", effect.getId());
        assertEquals(AbilitySetEffect.Ability.CONSTITUTION, effect.getAbility());
        assertEquals(19, effect.getSetValue());
    }

    @Test
    @DisplayName("createBeltOfHillGiantStrength factory method")
    void createBeltOfHillGiantStrengthFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createBeltOfHillGiantStrength();

        assertEquals(AbilitySetEffect.Ability.STRENGTH, effect.getAbility());
        assertEquals(21, effect.getSetValue());
    }

    @Test
    @DisplayName("createBeltOfStoneGiantStrength factory method")
    void createBeltOfStoneGiantStrengthFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createBeltOfStoneGiantStrength();

        assertEquals(AbilitySetEffect.Ability.STRENGTH, effect.getAbility());
        assertEquals(23, effect.getSetValue());
    }

    @Test
    @DisplayName("createBeltOfFireGiantStrength factory method")
    void createBeltOfFireGiantStrengthFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createBeltOfFireGiantStrength();

        assertEquals(AbilitySetEffect.Ability.STRENGTH, effect.getAbility());
        assertEquals(25, effect.getSetValue());
    }

    @Test
    @DisplayName("createBeltOfCloudGiantStrength factory method")
    void createBeltOfCloudGiantStrengthFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createBeltOfCloudGiantStrength();

        assertEquals(AbilitySetEffect.Ability.STRENGTH, effect.getAbility());
        assertEquals(27, effect.getSetValue());
    }

    @Test
    @DisplayName("createBeltOfStormGiantStrength factory method")
    void createBeltOfStormGiantStrengthFactory() {
        AbilitySetEffect effect = AbilitySetEffect.createBeltOfStormGiantStrength();

        assertEquals(AbilitySetEffect.Ability.STRENGTH, effect.getAbility());
        assertEquals(29, effect.getSetValue());
    }

    @Test
    @DisplayName("use returns ability set message")
    void useReturnsMessage() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("Strength"));
        assertTrue(result.contains("19"));
    }

    @Test
    @DisplayName("is passive usage type")
    void isPassiveUsageType() {
        AbilitySetEffect effect = AbilitySetEffect.createGauntletsOfOgrePower();

        assertEquals(UsageType.PASSIVE, effect.getUsageType());
        assertTrue(effect.isPassive());
    }
}
