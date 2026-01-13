package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StatBonusEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("StatBonusEffect")
class StatBonusEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    @Test
    @DisplayName("creates with correct stat type")
    void createsWithStatType() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneArmor();

        assertEquals(StatBonusEffect.StatType.ARMOR_CLASS, effect.getStatType());
        assertEquals(1, effect.getBonusAmount());
    }

    @Test
    @DisplayName("applies to correct stat")
    void appliesToCorrectStat() {
        StatBonusEffect effect = new StatBonusEffect("test", "Test",
                StatBonusEffect.StatType.SAVING_THROWS, 1);

        assertTrue(effect.appliesTo(StatBonusEffect.StatType.SAVING_THROWS));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.STRENGTH_SAVE));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.WISDOM_SAVE));
        assertFalse(effect.appliesTo(StatBonusEffect.StatType.ARMOR_CLASS));
    }

    @Test
    @DisplayName("passive usage type")
    void passiveUsage() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneArmor();

        assertEquals(UsageType.PASSIVE, effect.getUsageType());
        assertTrue(effect.isPassive());
    }

    @Test
    @DisplayName("getBonus returns bonus amount")
    void getBonusReturnsBonusAmount() {
        StatBonusEffect effect = new StatBonusEffect("test", "Test",
                StatBonusEffect.StatType.ATTACK_ROLLS, 3);
        assertEquals(3, effect.getBonus());
    }

    @Test
    @DisplayName("setStatType updates stat type")
    void setStatTypeUpdates() {
        StatBonusEffect effect = new StatBonusEffect("test", "Test",
                StatBonusEffect.StatType.ARMOR_CLASS, 1);
        effect.setStatType(StatBonusEffect.StatType.INITIATIVE);

        assertEquals(StatBonusEffect.StatType.INITIATIVE, effect.getStatType());
        assertTrue(effect.getDescription().contains("Initiative"));
    }

    @Test
    @DisplayName("setBonusAmount updates bonus")
    void setBonusAmountUpdates() {
        StatBonusEffect effect = new StatBonusEffect("test", "Test",
                StatBonusEffect.StatType.ARMOR_CLASS, 1);
        effect.setBonusAmount(3);

        assertEquals(3, effect.getBonusAmount());
        assertTrue(effect.getDescription().contains("+3"));
    }

    @Test
    @DisplayName("requiresAttunement and setter")
    void attunementSetter() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneArmor();
        assertFalse(effect.requiresAttunement());

        effect.setRequiresAttunement(true);
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("stacksWithOther and setter")
    void stacksWithOtherSetter() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneArmor();
        assertFalse(effect.stacksWithOther());

        effect.setStacksWithOther(true);
        assertTrue(effect.stacksWithOther());
    }

    @Test
    @DisplayName("getDetailedInfo includes attunement")
    void getDetailedInfoIncludesAttunement() {
        StatBonusEffect effect = StatBonusEffect.createRingOfProtectionAC();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Requires attunement"));
    }

    @Test
    @DisplayName("createPlusTwoArmor factory method")
    void createPlusTwoArmorFactory() {
        StatBonusEffect effect = StatBonusEffect.createPlusTwoArmor();

        assertEquals(StatBonusEffect.StatType.ARMOR_CLASS, effect.getStatType());
        assertEquals(2, effect.getBonusAmount());
    }

    @Test
    @DisplayName("createPlusOneWeaponAttack factory method")
    void createPlusOneWeaponAttackFactory() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneWeaponAttack();

        assertEquals(StatBonusEffect.StatType.ATTACK_ROLLS, effect.getStatType());
        assertEquals(1, effect.getBonusAmount());
    }

    @Test
    @DisplayName("createPlusOneWeaponDamage factory method")
    void createPlusOneWeaponDamageFactory() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneWeaponDamage();

        assertEquals(StatBonusEffect.StatType.DAMAGE_ROLLS, effect.getStatType());
        assertEquals(1, effect.getBonusAmount());
    }

    @Test
    @DisplayName("createRingOfProtectionAC factory method")
    void createRingOfProtectionACFactory() {
        StatBonusEffect effect = StatBonusEffect.createRingOfProtectionAC();

        assertEquals(StatBonusEffect.StatType.ARMOR_CLASS, effect.getStatType());
        assertEquals(1, effect.getBonusAmount());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createRingOfProtectionSaves factory method")
    void createRingOfProtectionSavesFactory() {
        StatBonusEffect effect = StatBonusEffect.createRingOfProtectionSaves();

        assertEquals(StatBonusEffect.StatType.SAVING_THROWS, effect.getStatType());
        assertEquals(1, effect.getBonusAmount());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createCloakOfProtection factory method")
    void createCloakOfProtectionFactory() {
        StatBonusEffect effect = StatBonusEffect.createCloakOfProtection();

        assertTrue(effect.requiresAttunement());
        assertTrue(effect.getDescription().contains("saving throws"));
    }

    @Test
    @DisplayName("createInitiativeBonus factory method")
    void createInitiativeBonusFactory() {
        StatBonusEffect effect = StatBonusEffect.createInitiativeBonus(5);

        assertEquals(StatBonusEffect.StatType.INITIATIVE, effect.getStatType());
        assertEquals(5, effect.getBonusAmount());
    }

    @Test
    @DisplayName("createWandOfWarMage factory method")
    void createWandOfWarMageFactory() {
        StatBonusEffect effect = StatBonusEffect.createWandOfWarMage(2);

        assertEquals(StatBonusEffect.StatType.SPELL_ATTACK, effect.getStatType());
        assertEquals(2, effect.getBonusAmount());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("applies to all specific saves when SAVING_THROWS")
    void appliesToAllSpecificSaves() {
        StatBonusEffect effect = new StatBonusEffect("test", "Test",
                StatBonusEffect.StatType.SAVING_THROWS, 1);

        assertTrue(effect.appliesTo(StatBonusEffect.StatType.STRENGTH_SAVE));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.DEXTERITY_SAVE));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.CONSTITUTION_SAVE));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.INTELLIGENCE_SAVE));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.WISDOM_SAVE));
        assertTrue(effect.appliesTo(StatBonusEffect.StatType.CHARISMA_SAVE));
    }

    @Test
    @DisplayName("StatType has display names")
    void statTypeHasDisplayNames() {
        for (StatBonusEffect.StatType type : StatBonusEffect.StatType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("use returns appropriate message")
    void useReturnsMessage() {
        StatBonusEffect effect = StatBonusEffect.createPlusOneArmor();
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
    }

    @Test
    @DisplayName("specific save type does not apply to other saves")
    void specificSaveDoesNotApplyToOthers() {
        StatBonusEffect effect = new StatBonusEffect("test", "Test",
                StatBonusEffect.StatType.WISDOM_SAVE, 2);

        assertTrue(effect.appliesTo(StatBonusEffect.StatType.WISDOM_SAVE));
        assertFalse(effect.appliesTo(StatBonusEffect.StatType.STRENGTH_SAVE));
        assertFalse(effect.appliesTo(StatBonusEffect.StatType.DEXTERITY_SAVE));
    }
}
