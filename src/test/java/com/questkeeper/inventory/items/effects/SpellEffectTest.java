package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpellEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("SpellEffect")
class SpellEffectTest {

    private Character testUser;
    private Character testTarget;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
        testTarget = new Character("Target", Race.HUMAN, CharacterClass.FIGHTER);
    }

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

    @Test
    @DisplayName("setSpellName updates name and description")
    void setSpellNameUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setSpellName("Ice Storm");

        assertEquals("Ice Storm", effect.getSpellName());
        assertTrue(effect.getDescription().contains("Ice Storm"));
    }

    @Test
    @DisplayName("setSpellLevel updates level")
    void setSpellLevelUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setSpellLevel(5);

        assertEquals(5, effect.getSpellLevel());
    }

    @Test
    @DisplayName("setSpellLevel clamps to valid range (0-9)")
    void setSpellLevelClampsRange() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");

        effect.setSpellLevel(-1);
        assertEquals(0, effect.getSpellLevel());

        effect.setSpellLevel(15);
        assertEquals(9, effect.getSpellLevel());
    }

    @Test
    @DisplayName("setSavingThrow updates saving throw")
    void setSavingThrowUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setSavingThrow("DEX");

        assertEquals("DEX", effect.getSavingThrow());
    }

    @Test
    @DisplayName("setSaveDC updates save DC")
    void setSaveDCUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setSaveDC(15);

        assertEquals(15, effect.getSaveDC());
    }

    @Test
    @DisplayName("setSaveDC clamps negative values")
    void setSaveDCClampsNegative() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setSaveDC(-5);

        assertEquals(0, effect.getSaveDC());
    }

    @Test
    @DisplayName("setDamageOrHealing updates damage/healing")
    void setDamageOrHealingUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setDamageOrHealing("8d6");

        assertEquals("8d6", effect.getDamageOrHealing());
    }

    @Test
    @DisplayName("setDamageType updates damage type")
    void setDamageTypeUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setDamageType("fire");

        assertEquals("fire", effect.getDamageType());
    }

    @Test
    @DisplayName("setDuration updates duration")
    void setDurationUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Hold Person");
        effect.setDuration(10);

        assertEquals(10, effect.getDuration());
    }

    @Test
    @DisplayName("setDuration clamps negative values")
    void setDurationClampsNegative() {
        SpellEffect effect = new SpellEffect("test", "Test", "Hold Person");
        effect.setDuration(-5);

        assertEquals(0, effect.getDuration());
    }

    @Test
    @DisplayName("setConcentration updates concentration")
    void setConcentrationUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Hold Person");
        assertFalse(effect.requiresConcentration());

        effect.setConcentration(true);
        assertTrue(effect.requiresConcentration());
    }

    @Test
    @DisplayName("setSelfOnly updates self-only flag")
    void setSelfOnlyUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        assertTrue(effect.isSelfOnly()); // Default is true

        effect.setSelfOnly(false);
        assertFalse(effect.isSelfOnly());
    }

    @Test
    @DisplayName("setRange updates range")
    void setRangeUpdates() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fireball");
        effect.setRange(150);

        assertEquals(150, effect.getRange());
    }

    @Test
    @DisplayName("use on target with damage")
    void useOnTargetWithDamage() {
        SpellEffect effect = SpellEffect.createWandOfFireballs();
        int initialHp = testTarget.getCurrentHitPoints();

        String result = effect.use(testUser, testTarget);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("Fireball"));
        assertTrue(result.contains("Target"));
        assertTrue(testTarget.getCurrentHitPoints() < initialHp); // Took damage
    }

    @Test
    @DisplayName("use on target with healing")
    void useOnTargetWithHealing() {
        SpellEffect effect = SpellEffect.createPotionOfHealing();
        testUser.takeDamage(10);
        int damagedHp = testUser.getCurrentHitPoints();

        String result = effect.use(testUser, testUser);

        assertTrue(result.contains("healing"));
        assertTrue(testUser.getCurrentHitPoints() > damagedHp); // Was healed
    }

    @Test
    @DisplayName("self-only spell throws on different target")
    void selfOnlyThrowsOnDifferentTarget() {
        SpellEffect effect = SpellEffect.createFeatherfallBookmark();

        assertThrows(IllegalArgumentException.class, () ->
                effect.use(testUser, testTarget));
    }

    @Test
    @DisplayName("getDetailedInfo for damage spell")
    void getDetailedInfoDamageSpell() {
        SpellEffect effect = SpellEffect.createWandOfFireballs();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Fireball"));
        assertTrue(info.contains("3rd")); // 3rd level
        assertTrue(info.contains("150")); // range
        assertTrue(info.contains("DEX")); // save
        assertTrue(info.contains("15")); // DC
        assertTrue(info.contains("8d6")); // damage
        assertTrue(info.contains("fire")); // damage type
    }

    @Test
    @DisplayName("getDetailedInfo for healing spell")
    void getDetailedInfoHealingSpell() {
        SpellEffect effect = SpellEffect.createPotionOfHealing();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Healing"));
        assertTrue(info.contains("2d4+2"));
    }

    @Test
    @DisplayName("getDetailedInfo for cantrip")
    void getDetailedInfoCantrip() {
        SpellEffect effect = new SpellEffect("test", "Test", "Fire Bolt");
        effect.setSpellLevel(0);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("cantrip"));
    }

    @Test
    @DisplayName("getDetailedInfo for concentration spell")
    void getDetailedInfoConcentration() {
        SpellEffect effect = new SpellEffect("test", "Test", "Hold Person");
        effect.setDuration(10);
        effect.setConcentration(true);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Duration"));
        assertTrue(info.contains("concentration"));
    }

    @Test
    @DisplayName("getDetailedInfo range self")
    void getDetailedInfoRangeSelf() {
        SpellEffect effect = new SpellEffect("test", "Test", "Shield");
        effect.setRange(0);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Self"));
    }

    @Test
    @DisplayName("getDetailedInfo range touch")
    void getDetailedInfoRangeTouch() {
        SpellEffect effect = new SpellEffect("test", "Test", "Cure Wounds");
        effect.setRange(-1);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Touch"));
    }

    @Test
    @DisplayName("createWandOfFireballs factory method")
    void createWandOfFireballsFactory() {
        SpellEffect effect = SpellEffect.createWandOfFireballs();

        assertEquals("wand_fireballs_effect", effect.getId());
        assertEquals("Fireball", effect.getSpellName());
        assertEquals(3, effect.getSpellLevel());
        assertEquals(UsageType.CHARGES, effect.getUsageType());
        assertEquals(7, effect.getMaxCharges());
        assertEquals("DEX", effect.getSavingThrow());
        assertEquals(15, effect.getSaveDC());
        assertEquals("8d6", effect.getDamageOrHealing());
        assertEquals("fire", effect.getDamageType());
    }

    @Test
    @DisplayName("createRingOfInvisibility factory method")
    void createRingOfInvisibilityFactory() {
        SpellEffect effect = SpellEffect.createRingOfInvisibility();

        assertEquals("ring_invisibility_effect", effect.getId());
        assertEquals("Invisibility", effect.getSpellName());
        assertEquals(2, effect.getSpellLevel());
        assertEquals(UsageType.UNLIMITED, effect.getUsageType());
        assertTrue(effect.isSelfOnly());
    }

    @Test
    @DisplayName("simple constructor sets defaults")
    void simpleConstructorDefaults() {
        SpellEffect effect = new SpellEffect("test", "Test", "Magic Missile");

        assertEquals(1, effect.getSpellLevel());
        assertNull(effect.getSavingThrow());
        assertEquals(0, effect.getSaveDC());
        assertNull(effect.getDamageOrHealing());
        assertNull(effect.getDamageType());
        assertEquals(0, effect.getDuration());
        assertFalse(effect.requiresConcentration());
        assertTrue(effect.isSelfOnly());
        assertEquals(0, effect.getRange());
        assertEquals(UsageType.DAILY, effect.getUsageType());
        assertEquals(1, effect.getMaxCharges());
    }

    @Test
    @DisplayName("use consumes charges")
    void useConsumesCharges() {
        SpellEffect effect = SpellEffect.createWandOfFireballs();
        assertEquals(7, effect.getCurrentCharges());

        effect.use(testUser, testTarget);
        assertEquals(6, effect.getCurrentCharges());
    }

    @Test
    @DisplayName("use returns spell cast message")
    void useReturnsSpellMessage() {
        SpellEffect effect = new SpellEffect("test", "Test", "Shield");
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
        assertTrue(result.contains("casts"));
        assertTrue(result.contains("Shield"));
    }

    @Test
    @DisplayName("ordinal suffix for various spell levels")
    void ordinalSuffixVarious() {
        // Test by examining getDetailedInfo output
        SpellEffect effect1 = new SpellEffect("test", "Test", "Spell");
        effect1.setSpellLevel(1);
        assertTrue(effect1.getDetailedInfo().contains("1st"));

        SpellEffect effect2 = new SpellEffect("test", "Test", "Spell");
        effect2.setSpellLevel(2);
        assertTrue(effect2.getDetailedInfo().contains("2nd"));

        SpellEffect effect3 = new SpellEffect("test", "Test", "Spell");
        effect3.setSpellLevel(3);
        assertTrue(effect3.getDetailedInfo().contains("3rd"));

        SpellEffect effect4 = new SpellEffect("test", "Test", "Spell");
        effect4.setSpellLevel(4);
        assertTrue(effect4.getDetailedInfo().contains("4th"));

        // Test special cases: 11th, 12th, 13th (if spell levels went that high)
        // Since spell levels are capped at 9, we test what we can
    }
}
