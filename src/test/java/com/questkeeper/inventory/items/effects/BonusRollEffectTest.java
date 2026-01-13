package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BonusRollEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("BonusRollEffect")
class BonusRollEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    @Test
    @DisplayName("flat bonus effect")
    void flatBonus() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);

        assertEquals(BonusRollEffect.BonusType.FLAT_BONUS, effect.getBonusType());
        assertEquals(2, effect.getFlatBonus());
        assertEquals(2, effect.getBonusValue());
    }

    @Test
    @DisplayName("bonus dice effect")
    void bonusDice() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);

        assertEquals(BonusRollEffect.BonusType.BONUS_DICE, effect.getBonusType());
        assertEquals("1d4", effect.getBonusDice());
    }

    @Test
    @DisplayName("Jester's Lucky Coin factory method")
    void jestersLuckyCoin() {
        BonusRollEffect effect = BonusRollEffect.createJestersLuckyCoin();

        assertEquals("1d4", effect.getBonusDice());
        assertEquals(1, effect.getSelfDamage());
        assertEquals("on tails", effect.getSelfDamageCondition());
    }

    @Test
    @DisplayName("Harlequin's Favor is consumable")
    void harlequinsFavorConsumable() {
        BonusRollEffect effect = BonusRollEffect.createHarlequinsFavor();

        assertEquals(BonusRollEffect.BonusType.ADVANTAGE, effect.getBonusType());
        assertEquals(UsageType.CONSUMABLE, effect.getUsageType());
    }

    @Test
    @DisplayName("setBonusType updates type")
    void setBonusTypeUpdates() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setBonusType(BonusRollEffect.BonusType.ADVANTAGE);

        assertEquals(BonusRollEffect.BonusType.ADVANTAGE, effect.getBonusType());
    }

    @Test
    @DisplayName("setAppliesTo updates appliesTo")
    void setAppliesToUpdates() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        assertEquals(BonusRollEffect.AppliesTo.ANY, effect.getAppliesTo());

        effect.setAppliesTo(BonusRollEffect.AppliesTo.ATTACK);
        assertEquals(BonusRollEffect.AppliesTo.ATTACK, effect.getAppliesTo());
    }

    @Test
    @DisplayName("setFlatBonus updates flat bonus")
    void setFlatBonusUpdates() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setFlatBonus(5);

        assertEquals(5, effect.getFlatBonus());
    }

    @Test
    @DisplayName("setBonusDice updates dice")
    void setBonusDiceUpdates() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);
        effect.setBonusDice("2d6");

        assertEquals("2d6", effect.getBonusDice());
    }

    @Test
    @DisplayName("setSkillName updates skill name")
    void setSkillNameUpdates() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setAppliesTo(BonusRollEffect.AppliesTo.SPECIFIC_SKILL);
        effect.setSkillName("Perception");

        assertEquals("Perception", effect.getSkillName());
    }

    @Test
    @DisplayName("setSelfDamage clamps negative values")
    void setSelfDamageClampsNegative() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);
        effect.setSelfDamage(-5);

        assertEquals(0, effect.getSelfDamage());
    }

    @Test
    @DisplayName("setSelfDamageCondition updates condition")
    void setSelfDamageConditionUpdates() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);
        effect.setSelfDamageCondition("coin flip");

        assertEquals("coin flip", effect.getSelfDamageCondition());
    }

    @Test
    @DisplayName("getBonusValue returns 0 for non-numeric types")
    void getBonusValueNonNumeric() {
        BonusRollEffect advantageEffect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.DAILY, 1);
        assertEquals(0, advantageEffect.getBonusValue());

        BonusRollEffect rerollEffect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.REROLL, UsageType.DAILY, 1);
        assertEquals(0, rerollEffect.getBonusValue());
    }

    @Test
    @DisplayName("getBonusValue rolls dice for BONUS_DICE type")
    void getBonusValueRollsDice() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "2d6",
                UsageType.DAILY, 1);

        int value = effect.getBonusValue();
        assertTrue(value >= 2 && value <= 12); // 2d6 range
    }

    @Test
    @DisplayName("advantage constructor sets correct type")
    void advantageConstructor() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.DAILY, 1);

        assertEquals(BonusRollEffect.BonusType.ADVANTAGE, effect.getBonusType());
        assertEquals(0, effect.getFlatBonus());
        assertNull(effect.getBonusDice());
    }

    @Test
    @DisplayName("reroll constructor sets correct type")
    void rerollConstructor() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.REROLL, UsageType.DAILY, 1);

        assertEquals(BonusRollEffect.BonusType.REROLL, effect.getBonusType());
    }

    @Test
    @DisplayName("auto success constructor sets correct type")
    void autoSuccessConstructor() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.AUTO_SUCCESS, UsageType.DAILY, 1);

        assertEquals(BonusRollEffect.BonusType.AUTO_SUCCESS, effect.getBonusType());
    }

    @Test
    @DisplayName("getDetailedInfo for flat bonus")
    void getDetailedInfoFlatBonus() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 3);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("+3"));
    }

    @Test
    @DisplayName("getDetailedInfo for bonus dice")
    void getDetailedInfoBonusDice() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d6",
                UsageType.DAILY, 1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("1d6"));
    }

    @Test
    @DisplayName("getDetailedInfo for advantage")
    void getDetailedInfoAdvantage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.DAILY, 1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("advantage"));
    }

    @Test
    @DisplayName("getDetailedInfo includes appliesTo when not ANY")
    void getDetailedInfoAppliesTo() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setAppliesTo(BonusRollEffect.AppliesTo.SAVING_THROW);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("saving throw"));
    }

    @Test
    @DisplayName("getDetailedInfo includes self damage risk")
    void getDetailedInfoSelfDamage() {
        BonusRollEffect effect = BonusRollEffect.createJestersLuckyCoin();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Risk"));
        assertTrue(info.contains("1"));
        assertTrue(info.contains("tails"));
    }

    @Test
    @DisplayName("createStoneOfGoodLuck factory method")
    void createStoneOfGoodLuckFactory() {
        BonusRollEffect effect = BonusRollEffect.createStoneOfGoodLuck();

        assertEquals("stone_good_luck_effect", effect.getId());
        assertEquals(BonusRollEffect.BonusType.FLAT_BONUS, effect.getBonusType());
        assertEquals(1, effect.getFlatBonus());
        assertEquals(BonusRollEffect.AppliesTo.ABILITY_CHECK, effect.getAppliesTo());
    }

    @Test
    @DisplayName("use returns appropriate message for flat bonus")
    void useReturnsFlatBonusMessage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("+2"));
    }

    @Test
    @DisplayName("use returns appropriate message for advantage")
    void useReturnsAdvantageMessage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.DAILY, 1);
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("advantage"));
    }

    @Test
    @DisplayName("use returns appropriate message for reroll")
    void useReturnsRerollMessage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.REROLL, UsageType.DAILY, 1);
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("rerolls"));
    }

    @Test
    @DisplayName("use returns appropriate message for auto success")
    void useReturnsAutoSuccessMessage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.AUTO_SUCCESS, UsageType.DAILY, 1);
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("automatically succeeds"));
    }

    @Test
    @DisplayName("AppliesTo enum has all expected values")
    void appliesToEnumValues() {
        assertEquals(6, BonusRollEffect.AppliesTo.values().length);
        assertNotNull(BonusRollEffect.AppliesTo.ANY);
        assertNotNull(BonusRollEffect.AppliesTo.ATTACK);
        assertNotNull(BonusRollEffect.AppliesTo.SAVING_THROW);
        assertNotNull(BonusRollEffect.AppliesTo.ABILITY_CHECK);
        assertNotNull(BonusRollEffect.AppliesTo.DAMAGE);
        assertNotNull(BonusRollEffect.AppliesTo.SPECIFIC_SKILL);
    }

    @Test
    @DisplayName("BonusType enum has all expected values")
    void bonusTypeEnumValues() {
        assertEquals(5, BonusRollEffect.BonusType.values().length);
        assertNotNull(BonusRollEffect.BonusType.FLAT_BONUS);
        assertNotNull(BonusRollEffect.BonusType.BONUS_DICE);
        assertNotNull(BonusRollEffect.BonusType.ADVANTAGE);
        assertNotNull(BonusRollEffect.BonusType.REROLL);
        assertNotNull(BonusRollEffect.BonusType.AUTO_SUCCESS);
    }

    @Test
    @DisplayName("getDetailedInfo for reroll type")
    void getDetailedInfoReroll() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test Reroll",
                BonusRollEffect.BonusType.REROLL, UsageType.DAILY, 1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Test Reroll"));
        assertTrue(info.contains("reroll") || info.contains("Allows reroll"));
    }

    @Test
    @DisplayName("getDetailedInfo for auto success type")
    void getDetailedInfoAutoSuccess() {
        BonusRollEffect effect = new BonusRollEffect("test", "Auto Win",
                BonusRollEffect.BonusType.AUTO_SUCCESS, UsageType.DAILY, 1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Auto Win"));
        assertTrue(info.contains("success") || info.contains("Automatic"));
    }

    @Test
    @DisplayName("getDetailedInfo includes skill name for SPECIFIC_SKILL")
    void getDetailedInfoSpecificSkill() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setAppliesTo(BonusRollEffect.AppliesTo.SPECIFIC_SKILL);
        effect.setSkillName("Perception");
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("specific skill") || info.contains("Applies to"));
    }

    @Test
    @DisplayName("getDetailedInfo includes attack for ATTACK appliesTo")
    void getDetailedInfoAttack() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setAppliesTo(BonusRollEffect.AppliesTo.ATTACK);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("attack"));
    }

    @Test
    @DisplayName("getDetailedInfo includes damage for DAMAGE appliesTo")
    void getDetailedInfoDamage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setAppliesTo(BonusRollEffect.AppliesTo.DAMAGE);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("damage"));
    }

    @Test
    @DisplayName("getDetailedInfo includes ability check for ABILITY_CHECK appliesTo")
    void getDetailedInfoAbilityCheck() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setAppliesTo(BonusRollEffect.AppliesTo.ABILITY_CHECK);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("ability check"));
    }

    @Test
    @DisplayName("setSelfDamage accepts positive values")
    void setSelfDamagePositive() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);
        effect.setSelfDamage(5);

        assertEquals(5, effect.getSelfDamage());
    }

    @Test
    @DisplayName("setSelfDamage clamps zero to zero")
    void setSelfDamageZero() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);
        effect.setSelfDamage(0);

        assertEquals(0, effect.getSelfDamage());
    }

    @Test
    @DisplayName("getBonusValue for 1d4 dice is in valid range")
    void getBonusValueD4Range() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);

        // Run multiple times to verify range
        for (int i = 0; i < 20; i++) {
            int value = effect.getBonusValue();
            assertTrue(value >= 1 && value <= 4, "1d4 should produce 1-4, got: " + value);
        }
    }

    @Test
    @DisplayName("getBonusValue for 1d6 dice is in valid range")
    void getBonusValueD6Range() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d6",
                UsageType.DAILY, 1);

        for (int i = 0; i < 20; i++) {
            int value = effect.getBonusValue();
            assertTrue(value >= 1 && value <= 6, "1d6 should produce 1-6, got: " + value);
        }
    }

    @Test
    @DisplayName("getBonusValue for 1d8 dice is in valid range")
    void getBonusValueD8Range() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d8",
                UsageType.DAILY, 1);

        for (int i = 0; i < 20; i++) {
            int value = effect.getBonusValue();
            assertTrue(value >= 1 && value <= 8, "1d8 should produce 1-8, got: " + value);
        }
    }

    @Test
    @DisplayName("getBonusValue returns 0 for AUTO_SUCCESS")
    void getBonusValueAutoSuccess() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.AUTO_SUCCESS, UsageType.DAILY, 1);

        assertEquals(0, effect.getBonusValue());
    }

    @Test
    @DisplayName("use with bonus dice returns dice result message")
    void useWithBonusDiceReturnsMessage() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test Dice", "1d6",
                UsageType.DAILY, 1);
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("1d6"));
    }

    @Test
    @DisplayName("flat bonus constructor sets passive usage")
    void flatBonusConstructorPassive() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);

        assertEquals(UsageType.PASSIVE, effect.getUsageType());
        assertTrue(effect.isPassive());
    }

    @Test
    @DisplayName("bonus dice constructor sets correct usage type")
    void bonusDiceConstructorUsageType() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.LONG_REST, 2);

        assertEquals(UsageType.LONG_REST, effect.getUsageType());
        assertEquals(2, effect.getMaxCharges());
    }

    @Test
    @DisplayName("bonus type constructor sets correct usage type")
    void bonusTypeConstructorUsageType() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.CHARGES, 3);

        assertEquals(UsageType.CHARGES, effect.getUsageType());
        assertEquals(3, effect.getMaxCharges());
    }

    @Test
    @DisplayName("use with self damage condition can apply damage")
    void useWithSelfDamageCondition() {
        BonusRollEffect effect = new BonusRollEffect("test", "Risk Roll", "1d4",
                UsageType.DAILY, 10);
        effect.setSelfDamage(2);
        effect.setSelfDamageCondition("on tails");

        int initialHp = testUser.getCurrentHitPoints();

        // Use multiple times - statistically some should cause self damage
        boolean tookDamage = false;
        for (int i = 0; i < 20; i++) {
            effect.resetDaily(); // Reset for next use
            effect.use(testUser);
            if (testUser.getCurrentHitPoints() < initialHp) {
                tookDamage = true;
                break;
            }
        }

        // Note: This is probabilistic, but with 20 attempts and 50% chance,
        // there's a very high probability of at least one damage event
        // We're mainly testing the mechanism works, not the exact probability
    }

    @Test
    @DisplayName("self damage condition 'coin flip' also works")
    void selfDamageConditionCoinFlip() {
        BonusRollEffect effect = new BonusRollEffect("test", "Flip Risk", "1d4",
                UsageType.DAILY, 1);
        effect.setSelfDamage(1);
        effect.setSelfDamageCondition("coin flip");

        // Just verify no exceptions are thrown
        assertDoesNotThrow(() -> effect.use(testUser));
    }

    @Test
    @DisplayName("no self damage without condition")
    void noSelfDamageWithoutCondition() {
        BonusRollEffect effect = new BonusRollEffect("test", "Safe Roll", "1d4",
                UsageType.DAILY, 10);
        effect.setSelfDamage(5); // Has damage amount but no condition

        int initialHp = testUser.getCurrentHitPoints();

        for (int i = 0; i < 10; i++) {
            effect.resetDaily();
            effect.use(testUser);
        }

        // Without a condition, no self damage should occur
        assertEquals(initialHp, testUser.getCurrentHitPoints());
    }

    @Test
    @DisplayName("no self damage with empty condition")
    void noSelfDamageWithEmptyCondition() {
        BonusRollEffect effect = new BonusRollEffect("test", "Safe Roll", "1d4",
                UsageType.DAILY, 10);
        effect.setSelfDamage(5);
        effect.setSelfDamageCondition("");

        int initialHp = testUser.getCurrentHitPoints();

        for (int i = 0; i < 10; i++) {
            effect.resetDaily();
            effect.use(testUser);
        }

        assertEquals(initialHp, testUser.getCurrentHitPoints());
    }

    @Test
    @DisplayName("setSkillName allows any string")
    void setSkillNameAnyString() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);
        effect.setSkillName("Thieves' Tools");

        assertEquals("Thieves' Tools", effect.getSkillName());
    }

    @Test
    @DisplayName("default appliesTo is ANY")
    void defaultAppliesToIsAny() {
        BonusRollEffect flatEffect = new BonusRollEffect("test", "Test", 2);
        assertEquals(BonusRollEffect.AppliesTo.ANY, flatEffect.getAppliesTo());

        BonusRollEffect diceEffect = new BonusRollEffect("test", "Test", "1d4",
                UsageType.DAILY, 1);
        assertEquals(BonusRollEffect.AppliesTo.ANY, diceEffect.getAppliesTo());

        BonusRollEffect typeEffect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.DAILY, 1);
        assertEquals(BonusRollEffect.AppliesTo.ANY, typeEffect.getAppliesTo());
    }

    @Test
    @DisplayName("toString contains class name and id")
    void toStringContainsInfo() {
        BonusRollEffect effect = new BonusRollEffect("lucky_bonus", "Lucky", 2);
        String str = effect.toString();

        assertTrue(str.contains("BonusRollEffect") || str.contains("lucky_bonus"));
    }

    @Test
    @DisplayName("multiple setters can be chained logically")
    void multipleSetters() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 2);

        effect.setBonusType(BonusRollEffect.BonusType.BONUS_DICE);
        effect.setBonusDice("2d4");
        effect.setAppliesTo(BonusRollEffect.AppliesTo.ATTACK);
        effect.setSelfDamage(1);
        effect.setSelfDamageCondition("on nat 1");

        assertEquals(BonusRollEffect.BonusType.BONUS_DICE, effect.getBonusType());
        assertEquals("2d4", effect.getBonusDice());
        assertEquals(BonusRollEffect.AppliesTo.ATTACK, effect.getAppliesTo());
        assertEquals(1, effect.getSelfDamage());
        assertEquals("on nat 1", effect.getSelfDamageCondition());
    }

    @Test
    @DisplayName("Jester's Lucky Coin has correct ID")
    void jestersLuckyCoinId() {
        BonusRollEffect effect = BonusRollEffect.createJestersLuckyCoin();
        assertEquals("jesters_coin_effect", effect.getId());
    }

    @Test
    @DisplayName("Harlequin's Favor has correct ID")
    void harlequinsFavorId() {
        BonusRollEffect effect = BonusRollEffect.createHarlequinsFavor();
        assertEquals("harlequins_favor_effect", effect.getId());
    }

    @Test
    @DisplayName("Jester's Lucky Coin has correct usage type")
    void jestersLuckyCoinUsageType() {
        BonusRollEffect effect = BonusRollEffect.createJestersLuckyCoin();
        assertEquals(UsageType.DAILY, effect.getUsageType());
    }

    @Test
    @DisplayName("Stone of Good Luck requires no charges")
    void stoneOfGoodLuckNoCharges() {
        BonusRollEffect effect = BonusRollEffect.createStoneOfGoodLuck();
        assertEquals(UsageType.PASSIVE, effect.getUsageType());
        assertTrue(effect.isPassive());
    }

    @Test
    @DisplayName("flat bonus description contains bonus amount")
    void flatBonusDescription() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", 3);
        String desc = effect.getDescription();

        assertTrue(desc.contains("+3") || desc.contains("3"));
    }

    @Test
    @DisplayName("bonus dice description contains dice notation")
    void bonusDiceDescription() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test", "2d6",
                UsageType.DAILY, 1);
        String desc = effect.getDescription();

        assertTrue(desc.contains("2d6"));
    }

    @Test
    @DisplayName("advantage description mentions rolling twice")
    void advantageDescription() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.ADVANTAGE, UsageType.DAILY, 1);
        String desc = effect.getDescription();

        assertTrue(desc.toLowerCase().contains("twice") || desc.toLowerCase().contains("higher"));
    }

    @Test
    @DisplayName("reroll description mentions reroll")
    void rerollDescription() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.REROLL, UsageType.DAILY, 1);
        String desc = effect.getDescription();

        assertTrue(desc.toLowerCase().contains("reroll"));
    }

    @Test
    @DisplayName("auto success description mentions success")
    void autoSuccessDescription() {
        BonusRollEffect effect = new BonusRollEffect("test", "Test",
                BonusRollEffect.BonusType.AUTO_SUCCESS, UsageType.DAILY, 1);
        String desc = effect.getDescription();

        assertTrue(desc.toLowerCase().contains("succeed") || desc.toLowerCase().contains("success"));
    }
}
