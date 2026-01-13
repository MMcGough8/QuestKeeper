package com.questkeeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.questkeeper.inventory.items.effects.BonusRollEffect;
import com.questkeeper.inventory.items.effects.DamageReductionEffect;
import com.questkeeper.inventory.items.effects.MovementEffect;
import com.questkeeper.inventory.items.effects.SkillBonusEffect;
import com.questkeeper.inventory.items.effects.SpellEffect;
import com.questkeeper.inventory.items.effects.StatBonusEffect;
import com.questkeeper.inventory.items.effects.TeleportEffect;
import com.questkeeper.inventory.items.effects.UsageType;
import com.questkeeper.inventory.items.effects.UtilityEffect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ItemEffect interface and shared effect functionality.
 * Individual effect type tests are in separate test files in the effects package.
 *
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Item Effects")
class ItemEffectTest {

    // Mock character for testing - in real tests, use actual Character class
    private TestCharacter testUser;

    @BeforeEach
    void setUp() {
        testUser = new TestCharacter("Test Hero", 50, 50);
    }

    // Simple test character class for testing effects
    static class TestCharacter extends com.questkeeper.character.Character {
    public TestCharacter(String name, int currentHp, int maxHp) {
        super(name, Race.HUMAN, CharacterClass.FIGHTER);
        // HP is set by constructor based on class/CON
    }
}

    @Nested
    @DisplayName("UsageType")
    class UsageTypeTests {

        @Test
        @DisplayName("all usage types have display names")
        void allHaveDisplayNames() {
            for (UsageType type : UsageType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("all usage types have descriptions")
        void allHaveDescriptions() {
            for (UsageType type : UsageType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("AbstractItemEffect")
    class AbstractItemEffectTests {

        @Test
        @DisplayName("throws exception for null ID")
        void throwsForNullId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TeleportEffect(null, "Test", 10));
        }

        @Test
        @DisplayName("throws exception for empty name")
        void throwsForEmptyName() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TeleportEffect("id", "", 10));
        }

        @Test
        @DisplayName("throws exception for whitespace-only ID")
        void throwsForWhitespaceId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TeleportEffect("   ", "Test", 10));
        }

        @Test
        @DisplayName("throws exception for null user on use")
        void throwsForNullUser() {
            TeleportEffect effect = new TeleportEffect("test", "Test", 10);
            assertThrows(IllegalArgumentException.class, () -> effect.use(null));
        }

        @Test
        @DisplayName("unlimited effects are always usable")
        void unlimitedAlwaysUsable() {
            StatBonusEffect effect = StatBonusEffect.createPlusOneArmor();
            assertTrue(effect.isUsable());
            assertEquals(-1, effect.getCurrentCharges());
        }

        @Test
        @DisplayName("passive effects are always usable")
        void passiveAlwaysUsable() {
            StatBonusEffect effect = new StatBonusEffect("test", "Test",
                    StatBonusEffect.StatType.ARMOR_CLASS, 1);
            assertTrue(effect.isPassive());
            assertTrue(effect.isUsable());
        }

        @Test
        @DisplayName("charge-based effects track charges correctly")
        void chargeTracking() {
            SpellEffect effect = new SpellEffect("test", "Test", "Fireball",
                    UsageType.CHARGES, 3);

            assertEquals(3, effect.getMaxCharges());
            assertEquals(3, effect.getCurrentCharges());

            effect.use(testUser);
            assertEquals(2, effect.getCurrentCharges());

            effect.use(testUser);
            effect.use(testUser);
            assertEquals(0, effect.getCurrentCharges());
            assertFalse(effect.isUsable());
        }

        @Test
        @DisplayName("throws when using depleted effect")
        void throwsWhenDepleted() {
            SpellEffect effect = new SpellEffect("test", "Test", "Spell",
                    UsageType.DAILY, 1);
            effect.use(testUser);

            assertThrows(IllegalStateException.class, () -> effect.use(testUser));
        }

        @Test
        @DisplayName("daily effects reset on resetDaily")
        void dailyReset() {
            SpellEffect effect = new SpellEffect("test", "Test", "Spell",
                    UsageType.DAILY, 1);
            effect.use(testUser);
            assertEquals(0, effect.getCurrentCharges());

            effect.resetDaily();
            assertEquals(1, effect.getCurrentCharges());
            assertTrue(effect.isUsable());
        }

        @Test
        @DisplayName("long rest effects reset on resetOnLongRest")
        void longRestReset() {
            TeleportEffect effect = TeleportEffect.createBlinkstepSpark();
            effect.use(testUser);
            assertEquals(0, effect.getCurrentCharges());

            effect.resetOnLongRest();
            assertEquals(1, effect.getCurrentCharges());
        }

        @Test
        @DisplayName("consumables are marked consumed after use")
        void consumablesConsumed() {
            SpellEffect effect = SpellEffect.createPotionOfHealing();
            assertFalse(effect.isConsumed());

            effect.use(testUser);
            assertTrue(effect.isConsumed());
            assertFalse(effect.isUsable());
        }

        @Test
        @DisplayName("consumables don't recharge")
        void consumablesNoRecharge() {
            SpellEffect effect = SpellEffect.createPotionOfHealing();
            effect.use(testUser);

            effect.resetDaily();
            effect.resetOnLongRest();

            assertTrue(effect.isConsumed());
            assertFalse(effect.isUsable());
        }

        @Test
        @DisplayName("equality based on ID")
        void equalityById() {
            TeleportEffect e1 = new TeleportEffect("same_id", "Name1", 10);
            TeleportEffect e2 = new TeleportEffect("same_id", "Name2", 20);

            assertEquals(e1, e2);
            assertEquals(e1.hashCode(), e2.hashCode());
        }

        @Test
        @DisplayName("setName updates name")
        void setNameUpdatesName() {
            TeleportEffect effect = new TeleportEffect("test", "Original", 10);
            effect.setName("Updated");
            assertEquals("Updated", effect.getName());
        }

        @Test
        @DisplayName("setName ignores null or empty")
        void setNameIgnoresInvalid() {
            TeleportEffect effect = new TeleportEffect("test", "Original", 10);
            effect.setName(null);
            assertEquals("Original", effect.getName());
            effect.setName("   ");
            assertEquals("Original", effect.getName());
        }

        @Test
        @DisplayName("setDescription updates description")
        void setDescriptionUpdates() {
            TeleportEffect effect = new TeleportEffect("test", "Test", 10);
            effect.setDescription("New description");
            assertEquals("New description", effect.getDescription());
        }

        @Test
        @DisplayName("setDescription handles null")
        void setDescriptionHandlesNull() {
            TeleportEffect effect = new TeleportEffect("test", "Test", 10);
            effect.setDescription(null);
            assertEquals("", effect.getDescription());
        }

        @Test
        @DisplayName("setMaxCharges clamps current charges")
        void setMaxChargesClampsCurrentCharges() {
            SpellEffect effect = new SpellEffect("test", "Test", "Spell", UsageType.CHARGES, 5);
            assertEquals(5, effect.getCurrentCharges());

            effect.setMaxCharges(3);
            assertEquals(3, effect.getMaxCharges());
            assertEquals(3, effect.getCurrentCharges());
        }

        @Test
        @DisplayName("addCharges respects max")
        void addChargesRespectsMax() {
            SpellEffect effect = new SpellEffect("test", "Test", "Spell", UsageType.CHARGES, 3);
            effect.use(testUser);
            effect.use(testUser);
            assertEquals(1, effect.getCurrentCharges());

            effect.addCharges(5);
            assertEquals(3, effect.getCurrentCharges()); // Clamped to max
        }

        @Test
        @DisplayName("setCurrentCharges clamps to valid range")
        void setCurrentChargesClampsToRange() {
            SpellEffect effect = new SpellEffect("test", "Test", "Spell", UsageType.CHARGES, 3);

            effect.setCurrentCharges(10);
            assertEquals(3, effect.getCurrentCharges()); // Clamped to max

            effect.setCurrentCharges(-5);
            assertEquals(0, effect.getCurrentCharges()); // Clamped to 0
        }

        @Test
        @DisplayName("partial recharge with setRechargeAmount")
        void partialRecharge() {
            SpellEffect effect = new SpellEffect("test", "Test", "Spell", UsageType.LONG_REST, 5);
            effect.setRechargeAmount(2);
            assertEquals(2, effect.getRechargeAmount());

            // Use all charges
            effect.use(testUser);
            effect.use(testUser);
            effect.use(testUser);
            effect.use(testUser);
            effect.use(testUser);
            assertEquals(0, effect.getCurrentCharges());

            // Partial recharge
            effect.resetOnLongRest();
            assertEquals(2, effect.getCurrentCharges());

            // Another partial recharge
            effect.resetOnLongRest();
            assertEquals(4, effect.getCurrentCharges());
        }

        @Test
        @DisplayName("getDetailedInfo returns formatted string")
        void getDetailedInfoReturnsFormatted() {
            TeleportEffect effect = TeleportEffect.createBlinkstepSpark();
            String info = effect.getDetailedInfo();

            assertNotNull(info);
            assertTrue(info.contains("Blinkstep"));
            assertTrue(info.contains("Usage:"));
        }

        @Test
        @DisplayName("toString returns meaningful representation")
        void toStringReturnsRepresentation() {
            TeleportEffect effect = new TeleportEffect("test_id", "Test Effect", 30);
            String str = effect.toString();

            assertTrue(str.contains("TeleportEffect"));
            assertTrue(str.contains("test_id"));
        }

        @Test
        @DisplayName("inequality for different IDs")
        void inequalityForDifferentIds() {
            TeleportEffect e1 = new TeleportEffect("id_one", "Same Name", 10);
            TeleportEffect e2 = new TeleportEffect("id_two", "Same Name", 10);

            assertNotEquals(e1, e2);
        }

        @Test
        @DisplayName("not equal to null or different type")
        void notEqualToNullOrDifferentType() {
            TeleportEffect effect = new TeleportEffect("test", "Test", 10);

            assertNotEquals(null, effect);
            assertNotEquals("not an effect", effect);
        }
    }

    @Nested
    @DisplayName("Muddlebrook Items Integration")
    class MuddlebrookItemsTests {

        @Test
        @DisplayName("all Muddlebrook effects can be created")
        void allMuddlebrookEffectsCreated() {
            assertDoesNotThrow(TeleportEffect::createBlinkstepSpark);
            assertDoesNotThrow(SpellEffect::createFeatherfallBookmark);
            assertDoesNotThrow(BonusRollEffect::createJestersLuckyCoin);
            assertDoesNotThrow(DamageReductionEffect::createSigilShard);
            assertDoesNotThrow(UtilityEffect::createWhisperingStone);
            assertDoesNotThrow(UtilityEffect::createFlashPowderOrb);
            assertDoesNotThrow(SkillBonusEffect::createCloakPinOfMinorDisguise);
            assertDoesNotThrow(SkillBonusEffect::createGearbrakersKit);
            assertDoesNotThrow(MovementEffect::createHoppersJumpBand);
            assertDoesNotThrow(BonusRollEffect::createHarlequinsFavor);
        }

        @Test
        @DisplayName("Muddlebrook effects have correct usage types")
        void muddlebrookUsageTypes() {
            assertEquals(UsageType.LONG_REST, TeleportEffect.createBlinkstepSpark().getUsageType());
            assertEquals(UsageType.DAILY, SpellEffect.createFeatherfallBookmark().getUsageType());
            assertEquals(UsageType.DAILY, BonusRollEffect.createJestersLuckyCoin().getUsageType());
            assertEquals(UsageType.DAILY, DamageReductionEffect.createSigilShard().getUsageType());
            assertEquals(UsageType.DAILY, UtilityEffect.createWhisperingStone().getUsageType());
            assertEquals(UsageType.CONSUMABLE, UtilityEffect.createFlashPowderOrb().getUsageType());
            assertEquals(UsageType.CHARGES, SkillBonusEffect.createGearbrakersKit().getUsageType());
            assertEquals(UsageType.LONG_REST, MovementEffect.createHoppersJumpBand().getUsageType());
            assertEquals(UsageType.CONSUMABLE, BonusRollEffect.createHarlequinsFavor().getUsageType());
        }
    }
}
