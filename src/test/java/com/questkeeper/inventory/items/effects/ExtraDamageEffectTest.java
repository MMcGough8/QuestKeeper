package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExtraDamageEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("ExtraDamageEffect")
class ExtraDamageEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

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

    @Test
    @DisplayName("setDamageDice updates damage dice")
    void setDamageDiceUpdates() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "fire");
        effect.setDamageDice("3d8");

        assertEquals("3d8", effect.getDamageDice());
    }

    @Test
    @DisplayName("setDamageType updates damage type")
    void setDamageTypeUpdates() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "fire");
        effect.setDamageType("cold");

        assertEquals("cold", effect.getDamageType());
    }

    @Test
    @DisplayName("setTrigger updates trigger")
    void setTriggerUpdates() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "fire");
        assertEquals(ExtraDamageEffect.Trigger.ALWAYS, effect.getTrigger());

        effect.setTrigger(ExtraDamageEffect.Trigger.ON_CRITICAL);
        assertEquals(ExtraDamageEffect.Trigger.ON_CRITICAL, effect.getTrigger());
    }

    @Test
    @DisplayName("setConditionDescription updates condition")
    void setConditionDescriptionUpdates() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "fire",
                ExtraDamageEffect.Trigger.CONDITIONAL);
        effect.setConditionDescription("when target is below half health");

        assertEquals("when target is below half health", effect.getConditionDescription());
    }

    @Test
    @DisplayName("setTargetRestriction updates restriction")
    void setTargetRestrictionUpdates() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "radiant");
        assertNull(effect.getTargetRestriction());

        effect.setTargetRestriction("undead");
        assertEquals("undead", effect.getTargetRestriction());
    }

    @Test
    @DisplayName("setRequiresAttunement updates attunement")
    void setRequiresAttunementUpdates() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "fire");
        assertFalse(effect.requiresAttunement());

        effect.setRequiresAttunement(true);
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("ONCE_PER_TURN trigger only applies on first hit")
    void oncePerTurnTrigger() {
        ExtraDamageEffect effect = ExtraDamageEffect.createSneakAttackStyle(3);

        assertTrue(effect.shouldApply(false, true, null)); // First hit this turn
        assertFalse(effect.shouldApply(false, false, null)); // Not first hit
        assertTrue(effect.shouldApply(true, true, null)); // First hit (crit doesn't matter)
    }

    @Test
    @DisplayName("target restriction is case-insensitive")
    void targetRestrictionCaseInsensitive() {
        ExtraDamageEffect effect = ExtraDamageEffect.createHolyAvenger();

        assertTrue(effect.shouldApply(false, false, "FIEND"));
        assertTrue(effect.shouldApply(false, false, "Fiend"));
        assertTrue(effect.shouldApply(false, false, "fiend"));
    }

    @Test
    @DisplayName("target restriction with null target type always applies")
    void targetRestrictionWithNullTarget() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "1d6", "radiant");
        effect.setTargetRestriction("fiend");

        // When we don't know the target type, still applies if trigger is met
        assertTrue(effect.shouldApply(false, false, null));
    }

    @Test
    @DisplayName("getDetailedInfo includes all properties")
    void getDetailedInfoIncludesProperties() {
        ExtraDamageEffect effect = ExtraDamageEffect.createHolyAvenger();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Holy Avenger"));
        assertTrue(info.contains("2d10"));
        assertTrue(info.contains("radiant"));
        assertTrue(info.contains("fiend"));
        assertTrue(info.contains("Requires attunement"));
    }

    @Test
    @DisplayName("getDetailedInfo shows trigger type")
    void getDetailedInfoShowsTrigger() {
        ExtraDamageEffect critEffect = ExtraDamageEffect.createViciousWeapon();
        assertTrue(critEffect.getDetailedInfo().contains("critical"));

        ExtraDamageEffect onceEffect = ExtraDamageEffect.createSneakAttackStyle(2);
        assertTrue(onceEffect.getDetailedInfo().contains("once per turn"));
    }

    @Test
    @DisplayName("createFlameTongue factory method")
    void createFlameTongueFactory() {
        ExtraDamageEffect effect = ExtraDamageEffect.createFlameTongue();

        assertEquals("flame_tongue_effect", effect.getId());
        assertEquals("2d6", effect.getDamageDice());
        assertEquals("fire", effect.getDamageType());
        assertEquals(ExtraDamageEffect.Trigger.ALWAYS, effect.getTrigger());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createViciousWeapon factory method")
    void createViciousWeaponFactory() {
        ExtraDamageEffect effect = ExtraDamageEffect.createViciousWeapon();

        assertEquals("vicious_weapon_effect", effect.getId());
        assertEquals("2d6", effect.getDamageDice());
        assertEquals(ExtraDamageEffect.Trigger.ON_CRITICAL, effect.getTrigger());
        assertFalse(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createSneakAttackStyle factory method")
    void createSneakAttackStyleFactory() {
        ExtraDamageEffect effect = ExtraDamageEffect.createSneakAttackStyle(5);

        assertEquals("sneak_attack_effect", effect.getId());
        assertEquals("5d6", effect.getDamageDice());
        assertEquals("weapon", effect.getDamageType());
        assertEquals(ExtraDamageEffect.Trigger.ONCE_PER_TURN, effect.getTrigger());
        assertNotNull(effect.getConditionDescription());
    }

    @Test
    @DisplayName("createHolyAvenger factory method")
    void createHolyAvengerFactory() {
        ExtraDamageEffect effect = ExtraDamageEffect.createHolyAvenger();

        assertEquals("holy_avenger_effect", effect.getId());
        assertEquals("2d10", effect.getDamageDice());
        assertEquals("radiant", effect.getDamageType());
        assertEquals(ExtraDamageEffect.Trigger.CONDITIONAL, effect.getTrigger());
        assertEquals("fiend", effect.getTargetRestriction());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("use returns damage message")
    void useReturnsDamageMessage() {
        ExtraDamageEffect effect = ExtraDamageEffect.createFlameTongue();
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("fire"));
        assertTrue(result.contains("damage"));
    }

    @Test
    @DisplayName("is passive usage type")
    void isPassiveUsageType() {
        ExtraDamageEffect effect = ExtraDamageEffect.createFlameTongue();

        assertEquals(UsageType.PASSIVE, effect.getUsageType());
        assertTrue(effect.isPassive());
    }

    @Test
    @DisplayName("Trigger enum has all expected values")
    void triggerEnumValues() {
        assertEquals(4, ExtraDamageEffect.Trigger.values().length);
        assertNotNull(ExtraDamageEffect.Trigger.ALWAYS);
        assertNotNull(ExtraDamageEffect.Trigger.ON_CRITICAL);
        assertNotNull(ExtraDamageEffect.Trigger.ONCE_PER_TURN);
        assertNotNull(ExtraDamageEffect.Trigger.CONDITIONAL);
    }

    @Test
    @DisplayName("constructor with trigger sets correct values")
    void constructorWithTrigger() {
        ExtraDamageEffect effect = new ExtraDamageEffect("test", "Test", "2d8", "lightning",
                ExtraDamageEffect.Trigger.ON_CRITICAL);

        assertEquals("2d8", effect.getDamageDice());
        assertEquals("lightning", effect.getDamageType());
        assertEquals(ExtraDamageEffect.Trigger.ON_CRITICAL, effect.getTrigger());
    }
}
