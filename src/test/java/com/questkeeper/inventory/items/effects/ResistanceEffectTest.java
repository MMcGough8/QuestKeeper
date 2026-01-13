package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ResistanceEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("ResistanceEffect")
class ResistanceEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

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

    @Test
    @DisplayName("vulnerability doubles damage")
    void vulnerabilityDoublesDamage() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.FIRE, ResistanceEffect.ResistanceLevel.VULNERABILITY);

        assertEquals(40, effect.calculateModifiedDamage(20, false));
    }

    @Test
    @DisplayName("normal level passes damage unchanged")
    void normalPassesDamageUnchanged() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.FIRE, ResistanceEffect.ResistanceLevel.NORMAL);

        assertEquals(20, effect.calculateModifiedDamage(20, false));
    }

    @Test
    @DisplayName("setResistanceLevel updates level and description")
    void setResistanceLevelUpdates() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.FIRE, ResistanceEffect.ResistanceLevel.RESISTANCE);

        effect.setResistanceLevel(ResistanceEffect.ResistanceLevel.IMMUNITY);

        assertEquals(ResistanceEffect.ResistanceLevel.IMMUNITY, effect.getResistanceLevel());
        assertTrue(effect.getDescription().contains("immune"));
    }

    @Test
    @DisplayName("setDamageType updates type and description")
    void setDamageTypeUpdates() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.FIRE, ResistanceEffect.ResistanceLevel.RESISTANCE);

        effect.setDamageType(ResistanceEffect.DamageType.COLD);

        assertEquals(ResistanceEffect.DamageType.COLD, effect.getDamageType());
        assertTrue(effect.getDescription().contains("Cold"));
    }

    @Test
    @DisplayName("requiresAttunement and setter")
    void attunementSetter() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.FIRE, ResistanceEffect.ResistanceLevel.RESISTANCE);
        assertFalse(effect.requiresAttunement());

        effect.setRequiresAttunement(true);
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("onlyNonmagical and setter")
    void onlyNonmagicalSetter() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.SLASHING, ResistanceEffect.ResistanceLevel.RESISTANCE);
        assertFalse(effect.isOnlyNonmagical());

        effect.setOnlyNonmagical(true);
        assertTrue(effect.isOnlyNonmagical());
    }

    @Test
    @DisplayName("onlyNonmagical ignores magical damage")
    void onlyNonmagicalIgnoresMagical() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.SLASHING, ResistanceEffect.ResistanceLevel.RESISTANCE);
        effect.setOnlyNonmagical(true);

        // Nonmagical damage is halved
        assertEquals(10, effect.calculateModifiedDamage(20, false));
        // Magical damage passes through
        assertEquals(20, effect.calculateModifiedDamage(20, true));
    }

    @Test
    @DisplayName("ALL damage type applies to everything")
    void allDamageTypeAppliesToEverything() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.ALL, ResistanceEffect.ResistanceLevel.RESISTANCE);

        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.FIRE, false));
        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.COLD, false));
        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.SLASHING, false));
        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.PSYCHIC, true));
    }

    @Test
    @DisplayName("NONMAGICAL_PHYSICAL applies to physical types when not magical")
    void nonmagicalPhysicalApplies() {
        ResistanceEffect effect = new ResistanceEffect("test", "Test",
                ResistanceEffect.DamageType.NONMAGICAL_PHYSICAL, ResistanceEffect.ResistanceLevel.RESISTANCE);

        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.BLUDGEONING, false));
        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.PIERCING, false));
        assertTrue(effect.appliesTo(ResistanceEffect.DamageType.SLASHING, false));

        assertFalse(effect.appliesTo(ResistanceEffect.DamageType.BLUDGEONING, true)); // Magical
        assertFalse(effect.appliesTo(ResistanceEffect.DamageType.FIRE, false)); // Not physical
    }

    @Test
    @DisplayName("getDetailedInfo includes all properties")
    void getDetailedInfoIncludesProperties() {
        ResistanceEffect effect = ResistanceEffect.createArmorOfInvulnerability();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Requires attunement"));
        assertTrue(info.contains("nonmagical only"));
    }

    @Test
    @DisplayName("createRingOfColdResistance factory method")
    void createRingOfColdResistanceFactory() {
        ResistanceEffect effect = ResistanceEffect.createRingOfColdResistance();

        assertEquals(ResistanceEffect.DamageType.COLD, effect.getDamageType());
        assertEquals(ResistanceEffect.ResistanceLevel.RESISTANCE, effect.getResistanceLevel());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createRingOfLightningResistance factory method")
    void createRingOfLightningResistanceFactory() {
        ResistanceEffect effect = ResistanceEffect.createRingOfLightningResistance();

        assertEquals(ResistanceEffect.DamageType.LIGHTNING, effect.getDamageType());
        assertEquals(ResistanceEffect.ResistanceLevel.RESISTANCE, effect.getResistanceLevel());
    }

    @Test
    @DisplayName("createArmorOfInvulnerability factory method")
    void createArmorOfInvulnerabilityFactory() {
        ResistanceEffect effect = ResistanceEffect.createArmorOfInvulnerability();

        assertEquals(ResistanceEffect.DamageType.NONMAGICAL_PHYSICAL, effect.getDamageType());
        assertEquals(ResistanceEffect.ResistanceLevel.IMMUNITY, effect.getResistanceLevel());
        assertTrue(effect.requiresAttunement());
        assertTrue(effect.isOnlyNonmagical());
    }

    @Test
    @DisplayName("createBroochOfShieldingForce factory method")
    void createBroochOfShieldingForceFactory() {
        ResistanceEffect effect = ResistanceEffect.createBroochOfShieldingForce();

        assertEquals(ResistanceEffect.DamageType.FORCE, effect.getDamageType());
        assertEquals(ResistanceEffect.ResistanceLevel.RESISTANCE, effect.getResistanceLevel());
    }

    @Test
    @DisplayName("DamageType has display names")
    void damageTypeHasDisplayNames() {
        for (ResistanceEffect.DamageType type : ResistanceEffect.DamageType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("ResistanceLevel has correct multipliers")
    void resistanceLevelHasMultipliers() {
        assertEquals(2.0, ResistanceEffect.ResistanceLevel.VULNERABILITY.getDamageMultiplier());
        assertEquals(1.0, ResistanceEffect.ResistanceLevel.NORMAL.getDamageMultiplier());
        assertEquals(0.5, ResistanceEffect.ResistanceLevel.RESISTANCE.getDamageMultiplier());
        assertEquals(0.0, ResistanceEffect.ResistanceLevel.IMMUNITY.getDamageMultiplier());
    }

    @Test
    @DisplayName("use returns appropriate message")
    void useReturnsMessage() {
        ResistanceEffect effect = ResistanceEffect.createRingOfFireResistance();
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
    }

    @Test
    @DisplayName("is passive usage type")
    void isPassiveUsageType() {
        ResistanceEffect effect = ResistanceEffect.createRingOfFireResistance();

        assertEquals(UsageType.PASSIVE, effect.getUsageType());
        assertTrue(effect.isPassive());
    }
}
