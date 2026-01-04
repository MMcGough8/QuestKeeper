package com.questkeeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.questkeeper.items.effects.AbilitySetEffect;
import com.questkeeper.items.effects.BonusRollEffect;
import com.questkeeper.items.effects.DamageReductionEffect;
import com.questkeeper.items.effects.ExtraDamageEffect;
import com.questkeeper.items.effects.MovementEffect;
import com.questkeeper.items.effects.ResistanceEffect;
import com.questkeeper.items.effects.SkillBonusEffect;
import com.questkeeper.items.effects.SpellEffect;
import com.questkeeper.items.effects.StatBonusEffect;
import com.questkeeper.items.effects.TeleportEffect;
import com.questkeeper.items.effects.UsageType;
import com.questkeeper.items.effects.UtilityEffect;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ItemEffect interface and all implementations.
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
    }

    @Nested
    @DisplayName("TeleportEffect")
    class TeleportEffectTests {

        @Test
        @DisplayName("creates with correct distance")
        void createsWithDistance() {
            TeleportEffect effect = new TeleportEffect("blink", "Blink", 30);
            assertEquals(30, effect.getDistance());
        }

        @Test
        @DisplayName("Blinkstep Spark factory method")
        void blinkstepSparkFactory() {
            TeleportEffect effect = TeleportEffect.createBlinkstepSpark();
            
            assertEquals("blinkstep_spark_effect", effect.getId());
            assertEquals(10, effect.getDistance());
            assertEquals(UsageType.LONG_REST, effect.getUsageType());
            assertEquals(1, effect.getMaxCharges());
        }

        @Test
        @DisplayName("use returns description")
        void useReturnsDescription() {
            TeleportEffect effect = TeleportEffect.createBlinkstepSpark();
            String result = effect.use(testUser);
            
            assertTrue(result.contains("Test Hero"));
            assertTrue(result.contains("10"));
        }
    }

    @Nested
    @DisplayName("BonusRollEffect")
    class BonusRollEffectTests {

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
    }

    @Nested
    @DisplayName("DamageReductionEffect")
    class DamageReductionEffectTests {

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
    }

    @Nested
    @DisplayName("SpellEffect")
    class SpellEffectTests {

        @Test
        @DisplayName("creates spell with correct name")
        void createsWithSpellName() {
            SpellEffect effect = new SpellEffect("test", "Test", "Magic Missile");
            assertEquals("Magic Missile", effect.getSpellName());
        }

        @Test
        @DisplayName("Featherfall Bookmark factory method")
        void featherfallBookmark() {
            SpellEffect effect = SpellEffect.createFeatherfallBookmark();
            
            assertEquals("Feather Fall", effect.getSpellName());
            assertEquals(UsageType.DAILY, effect.getUsageType());
            assertTrue(effect.isSelfOnly());
        }

        @Test
        @DisplayName("Potion of Healing is consumable")
        void potionOfHealingConsumable() {
            SpellEffect effect = SpellEffect.createPotionOfHealing();
            
            assertEquals(UsageType.CONSUMABLE, effect.getUsageType());
            assertEquals("2d4+2", effect.getDamageOrHealing());
        }
    }

    @Nested
    @DisplayName("ExtraDamageEffect")
    class ExtraDamageEffectTests {

        @Test
        @DisplayName("always trigger works")
        void alwaysTrigger() {
            ExtraDamageEffect effect = ExtraDamageEffect.createFlameTongue();
            
            assertTrue(effect.shouldApply(false, false, null));
            assertTrue(effect.shouldApply(true, true, null));
        }

        @Test
        @DisplayName("on critical trigger works")
        void onCriticalTrigger() {
            ExtraDamageEffect effect = ExtraDamageEffect.createViciousWeapon();
            
            assertTrue(effect.shouldApply(true, false, null));
            assertFalse(effect.shouldApply(false, false, null));
        }

        @Test
        @DisplayName("target restriction works")
        void targetRestriction() {
            ExtraDamageEffect effect = ExtraDamageEffect.createHolyAvenger();
            
            assertTrue(effect.shouldApply(false, false, "fiend"));
            assertFalse(effect.shouldApply(false, false, "humanoid"));
        }

        @Test
        @DisplayName("rolls damage dice")
        void rollsDamage() {
            ExtraDamageEffect effect = ExtraDamageEffect.createFlameTongue();
            
            int damage = effect.rollExtraDamage();
            assertTrue(damage >= 2 && damage <= 12); // 2d6 range
        }
    }

    @Nested
    @DisplayName("StatBonusEffect")
    class StatBonusEffectTests {

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
    }

    @Nested
    @DisplayName("AbilitySetEffect")
    class AbilitySetEffectTests {

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
    }

    @Nested
    @DisplayName("ResistanceEffect")
    class ResistanceEffectTests {

        @Test
        @DisplayName("resistance halves damage")
        void resistanceHalvesDamage() {
            ResistanceEffect effect = ResistanceEffect.createRingOfFireResistance();
            
            assertEquals(10, effect.calculateModifiedDamage(20, false));
        }

        @Test
        @DisplayName("immunity negates damage")
        void immunityNegatesDamage() {
            ResistanceEffect effect = ResistanceEffect.createPeriaptOfProofAgainstPoison();
            
            assertEquals(0, effect.calculateModifiedDamage(20, false));
        }

        @Test
        @DisplayName("applies to correct damage type")
        void appliesToCorrectDamageType() {
            ResistanceEffect effect = ResistanceEffect.createRingOfFireResistance();
            
            assertTrue(effect.appliesTo(ResistanceEffect.DamageType.FIRE, false));
            assertFalse(effect.appliesTo(ResistanceEffect.DamageType.COLD, false));
        }
    }

    @Nested
    @DisplayName("MovementEffect")
    class MovementEffectTests {

        @Test
        @DisplayName("speed bonus adds to base")
        void speedBonusAddsToBase() {
            MovementEffect effect = new MovementEffect("test", "Test",
                    MovementEffect.MovementType.SPEED_BONUS, 10);
            
            assertEquals(40, effect.calculateModifiedSpeed(30));
        }

        @Test
        @DisplayName("speed double works")
        void speedDoubleWorks() {
            MovementEffect effect = MovementEffect.createBootsOfSpeed();
            
            assertEquals(60, effect.calculateModifiedSpeed(30));
        }

        @Test
        @DisplayName("flying grants special movement")
        void flyingGrantsSpecialMovement() {
            MovementEffect effect = MovementEffect.createWingedBoots();
            
            assertEquals(30, effect.getSpecialMovementSpeed());
            assertTrue(effect.grantsMovementMode(MovementEffect.MovementType.FLYING));
        }

        @Test
        @DisplayName("Hopper's Jump Band factory method")
        void hoppersJumpBand() {
            MovementEffect effect = MovementEffect.createHoppersJumpBand();
            
            assertEquals(MovementEffect.MovementType.JUMP_BONUS, effect.getMovementType());
            assertEquals(UsageType.LONG_REST, effect.getUsageType());
        }
    }

    @Nested
    @DisplayName("SkillBonusEffect")
    class SkillBonusEffectTests {

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
    }

    @Nested
    @DisplayName("UtilityEffect")
    class UtilityEffectTests {

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