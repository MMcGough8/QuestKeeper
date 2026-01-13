package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DamageReductionEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("DamageReductionEffect")
class DamageReductionEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

    @Test
    @DisplayName("flat reduction calculates correctly")
    void flatReduction() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);

        assertEquals(15, effect.calculateReducedDamage(20, "slashing", false));
        assertEquals(0, effect.calculateReducedDamage(3, "slashing", false));
    }

    @Test
    @DisplayName("halve reduction calculates correctly")
    void halveReduction() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test",
                DamageReductionEffect.ReductionType.HALVE, 0, UsageType.PASSIVE, -1);

        assertEquals(10, effect.calculateReducedDamage(20, "fire", false));
    }

    @Test
    @DisplayName("damage type restriction works")
    void damageTypeRestriction() {
        DamageReductionEffect effect = DamageReductionEffect.createBroochOfShielding();
        effect.setDamageTypeRestriction("force");

        // Force damage is reduced
        int reduced = effect.calculateReducedDamage(20, "force", false);
        assertTrue(reduced < 20);

        // Fire damage is not reduced
        assertEquals(20, effect.calculateReducedDamage(20, "fire", false));
    }

    @Test
    @DisplayName("Sigil Shard factory method")
    void sigilShard() {
        DamageReductionEffect effect = DamageReductionEffect.createSigilShard();

        assertEquals(2, effect.getReductionAmount());
        assertEquals(UsageType.DAILY, effect.getUsageType());
        assertTrue(effect.isReaction());
    }

    @Test
    @DisplayName("setReductionType updates type")
    void setReductionTypeUpdates() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        assertEquals(DamageReductionEffect.ReductionType.FLAT, effect.getReductionType());

        effect.setReductionType(DamageReductionEffect.ReductionType.HALVE);
        assertEquals(DamageReductionEffect.ReductionType.HALVE, effect.getReductionType());
    }

    @Test
    @DisplayName("setReductionAmount updates amount")
    void setReductionAmountUpdates() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        effect.setReductionAmount(10);

        assertEquals(10, effect.getReductionAmount());
    }

    @Test
    @DisplayName("setReductionAmount clamps negative values")
    void setReductionAmountClampsNegative() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        effect.setReductionAmount(-10);

        assertEquals(0, effect.getReductionAmount());
    }

    @Test
    @DisplayName("setDamageTypeRestriction updates restriction")
    void setDamageTypeRestrictionUpdates() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        assertNull(effect.getDamageTypeRestriction());

        effect.setDamageTypeRestriction("fire");
        assertEquals("fire", effect.getDamageTypeRestriction());
    }

    @Test
    @DisplayName("setReaction updates reaction flag")
    void setReactionUpdates() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        assertFalse(effect.isReaction());

        effect.setReaction(true);
        assertTrue(effect.isReaction());
    }

    @Test
    @DisplayName("percentage reduction calculates correctly")
    void percentageReduction() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test",
                DamageReductionEffect.ReductionType.PERCENTAGE, 25, UsageType.PASSIVE, -1);

        assertEquals(15, effect.calculateReducedDamage(20, "slashing", false)); // 20 - 25% = 15
    }

    @Test
    @DisplayName("negate crit halves critical damage")
    void negateCritHalvesCriticalDamage() {
        DamageReductionEffect effect = DamageReductionEffect.createAdamantineArmor();

        // Critical hit damage is halved (returning to normal damage)
        assertEquals(10, effect.calculateReducedDamage(20, "slashing", true));

        // Normal hit damage is unchanged
        assertEquals(20, effect.calculateReducedDamage(20, "slashing", false));
    }

    @Test
    @DisplayName("getDamageReduced returns amount reduced")
    void getDamageReducedReturnsAmount() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);

        assertEquals(5, effect.getDamageReduced(20, "slashing", false));
        assertEquals(3, effect.getDamageReduced(3, "slashing", false)); // Can't reduce more than damage
    }

    @Test
    @DisplayName("getDetailedInfo for flat reduction")
    void getDetailedInfoFlatReduction() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("5"));
        assertTrue(info.contains("Reduces damage"));
    }

    @Test
    @DisplayName("getDetailedInfo for percentage reduction")
    void getDetailedInfoPercentageReduction() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test",
                DamageReductionEffect.ReductionType.PERCENTAGE, 25, UsageType.PASSIVE, -1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("25%"));
    }

    @Test
    @DisplayName("getDetailedInfo for halve reduction")
    void getDetailedInfoHalveReduction() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test",
                DamageReductionEffect.ReductionType.HALVE, 0, UsageType.PASSIVE, -1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Halves"));
    }

    @Test
    @DisplayName("getDetailedInfo for negate crit")
    void getDetailedInfoNegateCrit() {
        DamageReductionEffect effect = DamageReductionEffect.createAdamantineArmor();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("critical"));
    }

    @Test
    @DisplayName("getDetailedInfo includes damage type restriction")
    void getDetailedInfoDamageTypeRestriction() {
        DamageReductionEffect effect = DamageReductionEffect.createBroochOfShielding();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("force"));
    }

    @Test
    @DisplayName("getDetailedInfo includes reaction")
    void getDetailedInfoReaction() {
        DamageReductionEffect effect = DamageReductionEffect.createSigilShard();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("reaction"));
    }

    @Test
    @DisplayName("createAdamantineArmor factory method")
    void createAdamantineArmorFactory() {
        DamageReductionEffect effect = DamageReductionEffect.createAdamantineArmor();

        assertEquals("adamantine_effect", effect.getId());
        assertEquals(DamageReductionEffect.ReductionType.NEGATE_CRIT, effect.getReductionType());
        assertEquals(UsageType.PASSIVE, effect.getUsageType());
    }

    @Test
    @DisplayName("createBroochOfShielding factory method")
    void createBroochOfShieldingFactory() {
        DamageReductionEffect effect = DamageReductionEffect.createBroochOfShielding();

        assertEquals("brooch_shielding_effect", effect.getId());
        assertEquals(DamageReductionEffect.ReductionType.HALVE, effect.getReductionType());
        assertEquals("force", effect.getDamageTypeRestriction());
    }

    @Test
    @DisplayName("damage type restriction is case-insensitive")
    void damageTypeRestrictionCaseInsensitive() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        effect.setDamageTypeRestriction("fire");

        // Should match regardless of case
        assertTrue(effect.calculateReducedDamage(20, "FIRE", false) < 20);
        assertTrue(effect.calculateReducedDamage(20, "Fire", false) < 20);
    }

    @Test
    @DisplayName("use returns appropriate message")
    void useReturnsMessage() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5);
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("5"));
    }

    @Test
    @DisplayName("ReductionType enum has all expected values")
    void reductionTypeEnumValues() {
        assertEquals(4, DamageReductionEffect.ReductionType.values().length);
        assertNotNull(DamageReductionEffect.ReductionType.FLAT);
        assertNotNull(DamageReductionEffect.ReductionType.PERCENTAGE);
        assertNotNull(DamageReductionEffect.ReductionType.HALVE);
        assertNotNull(DamageReductionEffect.ReductionType.NEGATE_CRIT);
    }

    @Test
    @DisplayName("constructor with usage limits")
    void constructorWithUsageLimits() {
        DamageReductionEffect effect = new DamageReductionEffect("test", "Test", 5,
                UsageType.DAILY, 3);

        assertEquals(5, effect.getReductionAmount());
        assertEquals(UsageType.DAILY, effect.getUsageType());
        assertEquals(3, effect.getMaxCharges());
    }
}
