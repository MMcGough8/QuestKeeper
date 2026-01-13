package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SkillBonusEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("SkillBonusEffect")
class SkillBonusEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    @Test
    @DisplayName("flat bonus to skill")
    void flatBonusToSkill() {
        SkillBonusEffect effect = SkillBonusEffect.createEyesOfTheEagle();

        assertEquals(SkillBonusEffect.Skill.PERCEPTION, effect.getSkill());
        assertEquals(5, effect.getBonusAmount());
    }

    @Test
    @DisplayName("advantage on skill")
    void advantageOnSkill() {
        SkillBonusEffect effect = SkillBonusEffect.createCloakOfElvenkind();

        assertTrue(effect.grantsAdvantage());
        assertEquals(SkillBonusEffect.Skill.STEALTH, effect.getSkill());
    }

    @Test
    @DisplayName("Gearbreaker's Kit factory method")
    void gearbrakersKit() {
        SkillBonusEffect effect = SkillBonusEffect.createGearbrakersKit();

        assertEquals(2, effect.getBonusAmount());
        assertEquals(3, effect.getMaxCharges());
        assertEquals("mechanical traps only", effect.getSpecialCondition());
    }

    @Test
    @DisplayName("applies to correct skill")
    void appliesToCorrectSkill() {
        SkillBonusEffect effect = SkillBonusEffect.createEyesOfTheEagle();

        assertTrue(effect.appliesTo(SkillBonusEffect.Skill.PERCEPTION));
        assertFalse(effect.appliesTo(SkillBonusEffect.Skill.STEALTH));
    }

    @Test
    @DisplayName("setSkill updates skill")
    void setSkillUpdates() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 2);
        effect.setSkill(SkillBonusEffect.Skill.STEALTH);

        assertEquals(SkillBonusEffect.Skill.STEALTH, effect.getSkill());
    }

    @Test
    @DisplayName("setBonusType updates type")
    void setBonusTypeUpdates() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 2);
        effect.setBonusType(SkillBonusEffect.BonusType.ADVANTAGE);

        assertEquals(SkillBonusEffect.BonusType.ADVANTAGE, effect.getBonusType());
    }

    @Test
    @DisplayName("setBonusAmount updates amount")
    void setBonusAmountUpdates() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 2);
        effect.setBonusAmount(5);

        assertEquals(5, effect.getBonusAmount());
    }

    @Test
    @DisplayName("setRequiresAttunement updates attunement")
    void setRequiresAttunementUpdates() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 2);
        assertFalse(effect.requiresAttunement());

        effect.setRequiresAttunement(true);
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("setSpecialCondition updates condition")
    void setSpecialConditionUpdates() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 2);
        effect.setSpecialCondition("in dim light only");

        assertEquals("in dim light only", effect.getSpecialCondition());
    }

    @Test
    @DisplayName("calculateBonus for flat bonus")
    void calculateBonusFlatBonus() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 5);

        assertEquals(5, effect.calculateBonus(2, false));
        assertEquals(5, effect.calculateBonus(3, true));
    }

    @Test
    @DisplayName("calculateBonus for proficiency")
    void calculateBonusProficiency() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, SkillBonusEffect.BonusType.PROFICIENCY);

        // If not proficient, grants proficiency bonus
        assertEquals(3, effect.calculateBonus(3, false));
        // If already proficient, no additional bonus
        assertEquals(0, effect.calculateBonus(3, true));
    }

    @Test
    @DisplayName("calculateBonus for expertise")
    void calculateBonusExpertise() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, SkillBonusEffect.BonusType.EXPERTISE);

        // If proficient, doubles proficiency (adds proficiency again)
        assertEquals(3, effect.calculateBonus(3, true));
        // If not proficient, gives double proficiency
        assertEquals(6, effect.calculateBonus(3, false));
    }

    @Test
    @DisplayName("grantsAdvantage returns true only for ADVANTAGE type")
    void grantsAdvantageOnlyForAdvantage() {
        SkillBonusEffect advantageEffect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.STEALTH, SkillBonusEffect.BonusType.ADVANTAGE);
        assertTrue(advantageEffect.grantsAdvantage());

        SkillBonusEffect flatEffect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.STEALTH, 5);
        assertFalse(flatEffect.grantsAdvantage());
    }

    @Test
    @DisplayName("ALL_SKILLS applies to any skill")
    void allSkillsAppliesToAny() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.ALL_SKILLS, 1);

        assertTrue(effect.appliesTo(SkillBonusEffect.Skill.PERCEPTION));
        assertTrue(effect.appliesTo(SkillBonusEffect.Skill.STEALTH));
        assertTrue(effect.appliesTo(SkillBonusEffect.Skill.ATHLETICS));
        assertTrue(effect.appliesTo(SkillBonusEffect.Skill.ARCANA));
    }

    @Test
    @DisplayName("getDetailedInfo for flat bonus")
    void getDetailedInfoFlatBonus() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 5);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Perception"));
        assertTrue(info.contains("+5"));
    }

    @Test
    @DisplayName("getDetailedInfo for advantage")
    void getDetailedInfoAdvantage() {
        SkillBonusEffect effect = SkillBonusEffect.createCloakOfElvenkind();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Stealth"));
        assertTrue(info.contains("Advantage"));
    }

    @Test
    @DisplayName("getDetailedInfo includes special condition")
    void getDetailedInfoSpecialCondition() {
        SkillBonusEffect effect = SkillBonusEffect.createGearbrakersKit();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Condition"));
        assertTrue(info.contains("mechanical traps"));
    }

    @Test
    @DisplayName("getDetailedInfo includes attunement")
    void getDetailedInfoAttunement() {
        SkillBonusEffect effect = SkillBonusEffect.createEyesOfTheEagle();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Requires attunement"));
    }

    @Test
    @DisplayName("Skill enum has display names")
    void skillEnumHasDisplayNames() {
        for (SkillBonusEffect.Skill skill : SkillBonusEffect.Skill.values()) {
            assertNotNull(skill.getDisplayName());
            assertFalse(skill.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("Skill enum has ability short")
    void skillEnumHasAbilityShort() {
        assertEquals("STR", SkillBonusEffect.Skill.ATHLETICS.getAbilityShort());
        assertEquals("DEX", SkillBonusEffect.Skill.STEALTH.getAbilityShort());
        assertEquals("INT", SkillBonusEffect.Skill.ARCANA.getAbilityShort());
        assertEquals("WIS", SkillBonusEffect.Skill.PERCEPTION.getAbilityShort());
        assertEquals("CHA", SkillBonusEffect.Skill.PERSUASION.getAbilityShort());
    }

    @Test
    @DisplayName("BonusType enum has all expected values")
    void bonusTypeEnumValues() {
        assertEquals(5, SkillBonusEffect.BonusType.values().length);
        assertNotNull(SkillBonusEffect.BonusType.FLAT_BONUS);
        assertNotNull(SkillBonusEffect.BonusType.ADVANTAGE);
        assertNotNull(SkillBonusEffect.BonusType.PROFICIENCY);
        assertNotNull(SkillBonusEffect.BonusType.EXPERTISE);
        assertNotNull(SkillBonusEffect.BonusType.AUTO_SUCCESS);
    }

    @Test
    @DisplayName("createBootsOfElvenkind factory method")
    void createBootsOfElvenkindFactory() {
        SkillBonusEffect effect = SkillBonusEffect.createBootsOfElvenkind();

        assertEquals("boots_elvenkind_effect", effect.getId());
        assertEquals(SkillBonusEffect.Skill.STEALTH, effect.getSkill());
        assertEquals(SkillBonusEffect.BonusType.ADVANTAGE, effect.getBonusType());
        assertNotNull(effect.getSpecialCondition());
    }

    @Test
    @DisplayName("createGlovesOfThievery factory method")
    void createGlovesOfThieveryFactory() {
        SkillBonusEffect effect = SkillBonusEffect.createGlovesOfThievery();

        assertEquals("gloves_thievery_effect", effect.getId());
        assertEquals(SkillBonusEffect.Skill.SLEIGHT_OF_HAND, effect.getSkill());
        assertEquals(5, effect.getBonusAmount());
    }

    @Test
    @DisplayName("createToolProficiency factory method")
    void createToolProficiencyFactory() {
        SkillBonusEffect effect = SkillBonusEffect.createToolProficiency("Thieves Tools",
                SkillBonusEffect.Skill.THIEVES_TOOLS);

        assertEquals(SkillBonusEffect.Skill.THIEVES_TOOLS, effect.getSkill());
        assertEquals(SkillBonusEffect.BonusType.PROFICIENCY, effect.getBonusType());
    }

    @Test
    @DisplayName("createCloakPinOfMinorDisguise factory method")
    void createCloakPinOfMinorDisguiseFactory() {
        SkillBonusEffect effect = SkillBonusEffect.createCloakPinOfMinorDisguise();

        assertEquals("cloak_pin_disguise_effect", effect.getId());
        assertEquals(SkillBonusEffect.Skill.DECEPTION, effect.getSkill());
        assertEquals(SkillBonusEffect.BonusType.ADVANTAGE, effect.getBonusType());
        assertEquals(UsageType.DAILY, effect.getUsageType());
    }

    @Test
    @DisplayName("use returns appropriate message")
    void useReturnsMessage() {
        SkillBonusEffect effect = SkillBonusEffect.createEyesOfTheEagle();
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("Perception"));
    }

    @Test
    @DisplayName("constructor with charges sets usage type")
    void constructorWithCharges() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.PERCEPTION, 2, 3);

        assertEquals(UsageType.CHARGES, effect.getUsageType());
        assertEquals(3, effect.getMaxCharges());
    }

    @Test
    @DisplayName("constructor with bonus type sets type correctly")
    void constructorWithBonusType() {
        SkillBonusEffect effect = new SkillBonusEffect("test", "Test",
                SkillBonusEffect.Skill.STEALTH, SkillBonusEffect.BonusType.EXPERTISE);

        assertEquals(SkillBonusEffect.BonusType.EXPERTISE, effect.getBonusType());
        assertEquals(0, effect.getBonusAmount());
    }
}
