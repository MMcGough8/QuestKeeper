package com.questkeeper.inventory;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.inventory.Item.Rarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the MagicItem class.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("MagicItem")
class MagicItemTest {

    private MagicItem magicItem;
    private Character testCharacter;
    private Character otherCharacter;

    @BeforeEach
    void setUp() {
        magicItem = new MagicItem("Test Amulet", "A glowing amulet.", 
                0.5, 100, Rarity.UNCOMMON);
        testCharacter = new Character("Hero", Race.HUMAN, CharacterClass.FIGHTER);
        otherCharacter = new Character("Villain", Race.ELF, CharacterClass.ROGUE);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("creates magic item with basic properties")
        void createsWithBasicProperties() {
            assertEquals("Test Amulet", magicItem.getName());
            assertEquals("A glowing amulet.", magicItem.getDescription());
            assertEquals(0.5, magicItem.getWeight());
            assertEquals(100, magicItem.getGoldValue());
            assertEquals(Rarity.UNCOMMON, magicItem.getRarity());
        }

        @Test
        @DisplayName("creates with single effect")
        void createsWithSingleEffect() {
            ItemEffect effect = TeleportEffect.createBlinkstepSpark();
            MagicItem item = new MagicItem("Blink Ring", "A shimmering ring.",
                    0.1, 200, Rarity.RARE, effect);

            assertEquals(1, item.getEffectCount());
            assertTrue(item.hasEffects());
        }

        @Test
        @DisplayName("creates with multiple effects")
        void createsWithMultipleEffects() {
            List<ItemEffect> effects = MagicItem.createPlusOneWeaponEffects();
            MagicItem item = new MagicItem("+1 Sword", "A magical blade.",
                    3.0, 1000, Rarity.UNCOMMON, effects);

            assertEquals(2, item.getEffectCount());
        }

        @Test
        @DisplayName("starts with no attunement required")
        void startsNoAttunement() {
            assertFalse(magicItem.requiresAttunement());
            assertFalse(magicItem.isAttuned());
        }

        @Test
        @DisplayName("starts with no effects")
        void startsNoEffects() {
            assertFalse(magicItem.hasEffects());
            assertEquals(0, magicItem.getEffectCount());
        }
    }

    @Nested
    @DisplayName("Effect Management")
    class EffectManagementTests {

        @Test
        @DisplayName("adds effect successfully")
        void addsEffect() {
            ItemEffect effect = TeleportEffect.createBlinkstepSpark();
            magicItem.addEffect(effect);

            assertEquals(1, magicItem.getEffectCount());
            assertTrue(magicItem.getEffects().contains(effect));
        }

        @Test
        @DisplayName("adds multiple effects")
        void addsMultipleEffects() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            magicItem.addEffect(StatBonusEffect.createPlusOneArmor());
            magicItem.addEffect(ResistanceEffect.createRingOfFireResistance());

            assertEquals(3, magicItem.getEffectCount());
        }

        @Test
        @DisplayName("removes effect successfully")
        void removesEffect() {
            ItemEffect effect = TeleportEffect.createBlinkstepSpark();
            magicItem.addEffect(effect);
            
            boolean removed = magicItem.removeEffect(effect);
            
            assertTrue(removed);
            assertEquals(0, magicItem.getEffectCount());
        }

        @Test
        @DisplayName("ignores null effect")
        void ignoresNullEffect() {
            magicItem.addEffect(null);
            assertEquals(0, magicItem.getEffectCount());
        }

        @Test
        @DisplayName("separates passive and active effects")
        void separatesPassiveAndActive() {
            // Passive effect
            magicItem.addEffect(StatBonusEffect.createPlusOneArmor());
            // Active effect
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());

            assertEquals(1, magicItem.getPassiveEffects().size());
            assertEquals(1, magicItem.getActiveEffects().size());
        }

        @Test
        @DisplayName("gets usable effects only")
        void getsUsableEffects() {
            TeleportEffect effect = TeleportEffect.createBlinkstepSpark();
            magicItem.addEffect(effect);

            // Before use - should be usable
            assertEquals(1, magicItem.getUsableEffects().size());

            // Use it
            effect.use(testCharacter);

            // After use - should not be usable
            assertEquals(0, magicItem.getUsableEffects().size());
        }

        @Test
        @DisplayName("effects list is unmodifiable")
        void effectsListUnmodifiable() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());

            assertThrows(UnsupportedOperationException.class, 
                    () -> magicItem.getEffects().add(StatBonusEffect.createPlusOneArmor()));
        }
    }

    @Nested
    @DisplayName("Using the Item")
    class UseTests {

        @Test
        @DisplayName("uses all active effects")
        void usesAllActiveEffects() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            
            String result = magicItem.use(testCharacter);
            
            assertNotNull(result);
            assertTrue(result.contains("Hero"));
        }

        @Test
        @DisplayName("returns message when no usable effects")
        void messageWhenNoUsableEffects() {
            // Only passive effect
            magicItem.addEffect(StatBonusEffect.createPlusOneArmor());
            
            String result = magicItem.use(testCharacter);
            
            assertTrue(result.contains("no usable effects"));
        }

        @Test
        @DisplayName("uses specific effect by index")
        void usesEffectByIndex() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            magicItem.addEffect(SpellEffect.createFeatherfallBookmark());

            String result = magicItem.useEffect(testCharacter, 0);
            
            assertTrue(result.contains("Hero"));
        }

        @Test
        @DisplayName("throws for invalid effect index")
        void throwsForInvalidIndex() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());

            assertThrows(IndexOutOfBoundsException.class,
                    () -> magicItem.useEffect(testCharacter, 5));
        }

        @Test
        @DisplayName("uses specific effect by name")
        void usesEffectByName() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());

            String result = magicItem.useEffect(testCharacter, "Blinkstep");
            
            assertTrue(result.contains("Hero"));
        }

        @Test
        @DisplayName("returns message for unknown effect name")
        void messageForUnknownEffectName() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());

            String result = magicItem.useEffect(testCharacter, "Fireball");
            
            assertTrue(result.contains("No effect named"));
        }

        @Test
        @DisplayName("cannot use without required attunement")
        void cannotUseWithoutAttunement() {
            magicItem.setRequiresAttunement(true);
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());

            assertFalse(magicItem.canUse(testCharacter));
            assertThrows(IllegalStateException.class, 
                    () -> magicItem.use(testCharacter));
        }

        @Test
        @DisplayName("can use after attunement")
        void canUseAfterAttunement() {
            magicItem.setRequiresAttunement(true);
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            magicItem.attune(testCharacter);

            assertTrue(magicItem.canUse(testCharacter));
            assertDoesNotThrow(() -> magicItem.use(testCharacter));
        }

        @Test
        @DisplayName("only attuned character can use")
        void onlyAttunedCanUse() {
            magicItem.setRequiresAttunement(true);
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            magicItem.attune(testCharacter);

            assertTrue(magicItem.canUse(testCharacter));
            assertFalse(magicItem.canUse(otherCharacter));
        }
    }

    @Nested
    @DisplayName("Attunement")
    class AttunementTests {

        @Test
        @DisplayName("attunes successfully")
        void attunesSuccessfully() {
            magicItem.setRequiresAttunement(true);
            
            String result = magicItem.attune(testCharacter);
            
            assertTrue(magicItem.isAttuned());
            assertTrue(magicItem.isAttunedTo(testCharacter));
            assertEquals("Hero", magicItem.getAttunedToName());
            assertTrue(result.contains("attunes"));
        }

        @Test
        @DisplayName("returns message if attunement not required")
        void messageIfNotRequired() {
            String result = magicItem.attune(testCharacter);
            
            assertTrue(result.contains("doesn't require attunement"));
            assertFalse(magicItem.isAttuned());
        }

        @Test
        @DisplayName("returns message if already attuned to same character")
        void messageIfAlreadyAttuned() {
            magicItem.setRequiresAttunement(true);
            magicItem.attune(testCharacter);

            String result = magicItem.attune(testCharacter);
            
            assertTrue(result.contains("already attuned"));
        }

        @Test
        @DisplayName("throws if attuned to different character")
        void throwsIfAttunedToOther() {
            magicItem.setRequiresAttunement(true);
            magicItem.attune(testCharacter);

            assertThrows(IllegalStateException.class, 
                    () -> magicItem.attune(otherCharacter));
        }

        @Test
        @DisplayName("unattunes successfully")
        void unattunesSuccessfully() {
            magicItem.setRequiresAttunement(true);
            magicItem.attune(testCharacter);

            String result = magicItem.unattune();

            assertFalse(magicItem.isAttuned());
            assertNull(magicItem.getAttunedToName());
            assertTrue(result.contains("no longer attuned"));
        }

        @Test
        @DisplayName("returns message if not attuned when unattuning")
        void messageIfNotAttunedWhenUnattuning() {
            String result = magicItem.unattune();
            
            assertTrue(result.contains("not attuned"));
        }

        @Test
        @DisplayName("throws for null character")
        void throwsForNullCharacter() {
            magicItem.setRequiresAttunement(true);

            assertThrows(IllegalArgumentException.class, 
                    () -> magicItem.attune(null));
        }

        @Test
        @DisplayName("enforces attunement requirement")
        void enforcesAttunementRequirement() {
            magicItem.setRequiresAttunement(true);
            magicItem.setAttunementRequirement("wizard");

            // Fighter shouldn't be able to attune
            assertThrows(IllegalStateException.class, 
                    () -> magicItem.attune(testCharacter));
        }

        @Test
        @DisplayName("spellcaster requirement works")
        void spellcasterRequirementWorks() {
            magicItem.setRequiresAttunement(true);
            magicItem.setAttunementRequirement("spellcaster");

            Character wizard = new Character("Merlin", Race.HUMAN, CharacterClass.WIZARD);
            
            assertDoesNotThrow(() -> magicItem.attune(wizard));
            assertTrue(magicItem.isAttuned());
        }
    }

    @Nested
    @DisplayName("Rest and Reset")
    class RestResetTests {

        @Test
        @DisplayName("resets long rest effects")
        void resetsLongRestEffects() {
            TeleportEffect effect = TeleportEffect.createBlinkstepSpark();
            magicItem.addEffect(effect);
            
            // Use the effect
            effect.use(testCharacter);
            assertEquals(0, effect.getCurrentCharges());

            // Long rest
            magicItem.resetEffectsOnLongRest();
            
            assertEquals(1, effect.getCurrentCharges());
        }

        @Test
        @DisplayName("resets daily effects")
        void resetsDailyEffects() {
            SpellEffect effect = SpellEffect.createFeatherfallBookmark();
            magicItem.addEffect(effect);
            
            // Use the effect
            effect.use(testCharacter);
            assertEquals(0, effect.getCurrentCharges());

            // Daily reset
            magicItem.resetEffectsDaily();
            
            assertEquals(1, effect.getCurrentCharges());
        }

        @Test
        @DisplayName("checks if fully consumed")
        void checksFullyConsumed() {
            SpellEffect effect = SpellEffect.createPotionOfHealing();
            magicItem.addEffect(effect);

            assertFalse(magicItem.isFullyConsumed());

            effect.use(testCharacter);

            assertTrue(magicItem.isFullyConsumed());
        }
    }

    @Nested
    @DisplayName("Factory Methods - Muddlebrook")
    class MuddlebrookFactoryTests {

        @Test
        @DisplayName("creates Blinkstep Spark")
        void createsBlinkstepSpark() {
            MagicItem item = MagicItem.createBlinkstepSpark();
            
            assertEquals("Blinkstep Spark", item.getName());
            assertEquals(Rarity.UNCOMMON, item.getRarity());
            assertEquals(1, item.getEffectCount());
            assertFalse(item.requiresAttunement());
        }

        @Test
        @DisplayName("creates Featherfall Bookmark")
        void createsFeatherfallBookmark() {
            MagicItem item = MagicItem.createFeatherfallBookmark();
            
            assertEquals("Featherfall Bookmark", item.getName());
            assertEquals(1, item.getEffectCount());
        }

        @Test
        @DisplayName("creates Jester's Lucky Coin")
        void createsJestersLuckyCoin() {
            MagicItem item = MagicItem.createJestersLuckyCoin();
            
            assertEquals("Jester's Lucky Coin", item.getName());
            assertEquals(1, item.getEffectCount());
        }

        @Test
        @DisplayName("creates Sigil Shard")
        void createsSigilShard() {
            MagicItem item = MagicItem.createSigilShard();
            
            assertEquals("Sigil Shard", item.getName());
            assertEquals(1, item.getEffectCount());
        }

        @Test
        @DisplayName("creates Whispering Stone")
        void createsWhisperingStone() {
            MagicItem item = MagicItem.createWhisperingStone();
            
            assertEquals("Whispering Stone", item.getName());
        }

        @Test
        @DisplayName("creates Flash Powder Orb as consumable")
        void createsFlashPowderOrb() {
            MagicItem item = MagicItem.createFlashPowderOrb();
            
            assertEquals("Flash Powder Orb", item.getName());
            assertEquals(Rarity.COMMON, item.getRarity());
        }

        @Test
        @DisplayName("creates all Muddlebrook items successfully")
        void createsAllMuddlebrookItems() {
            assertDoesNotThrow(MagicItem::createBlinkstepSpark);
            assertDoesNotThrow(MagicItem::createFeatherfallBookmark);
            assertDoesNotThrow(MagicItem::createJestersLuckyCoin);
            assertDoesNotThrow(MagicItem::createSigilShard);
            assertDoesNotThrow(MagicItem::createWhisperingStone);
            assertDoesNotThrow(MagicItem::createFlashPowderOrb);
            assertDoesNotThrow(MagicItem::createCloakPinOfMinorDisguise);
            assertDoesNotThrow(MagicItem::createGearbrakersKit);
            assertDoesNotThrow(MagicItem::createHoppersJumpBand);
            assertDoesNotThrow(MagicItem::createHarlequinsFavor);
        }
    }

    @Nested
    @DisplayName("Factory Methods - Standard D&D")
    class StandardDnDFactoryTests {

        @Test
        @DisplayName("creates Flame Tongue with multiple effects")
        void createsFlameTongue() {
            MagicItem item = MagicItem.createFlameTongue();
            
            assertEquals("Flame Tongue Longsword", item.getName());
            assertEquals(Rarity.RARE, item.getRarity());
            assertEquals(2, item.getEffectCount()); // Extra damage + light
            assertTrue(item.requiresAttunement());
        }

        @Test
        @DisplayName("creates Ring of Protection with multiple effects")
        void createsRingOfProtection() {
            MagicItem item = MagicItem.createRingOfProtection();
            
            assertEquals("Ring of Protection", item.getName());
            assertEquals(2, item.getEffectCount()); // AC + saves
            assertTrue(item.requiresAttunement());
        }

        @Test
        @DisplayName("creates Gauntlets of Ogre Power")
        void createsGauntletsOfOgrePower() {
            MagicItem item = MagicItem.createGauntletsOfOgrePower();
            
            assertEquals("Gauntlets of Ogre Power", item.getName());
            assertTrue(item.requiresAttunement());
        }

        @Test
        @DisplayName("creates Potion of Healing as consumable")
        void createsPotionOfHealing() {
            MagicItem item = MagicItem.createPotionOfHealing();
            
            assertEquals("Potion of Healing", item.getName());
            assertEquals(Rarity.COMMON, item.getRarity());
            assertFalse(item.requiresAttunement());
        }

        @Test
        @DisplayName("creates +1 weapon effects")
        void createsPlusOneWeaponEffects() {
            List<ItemEffect> effects = MagicItem.createPlusOneWeaponEffects();
            
            assertEquals(2, effects.size());
        }
    }

    @Nested
    @DisplayName("Display and Utility")
    class DisplayTests {

        @Test
        @DisplayName("generates detailed info")
        void generatesDetailedInfo() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            
            String info = magicItem.getDetailedInfo();
            
            assertTrue(info.contains("Test Amulet"));
            assertTrue(info.contains("UNCOMMON"));
            assertTrue(info.contains("Effects:"));
        }

        @Test
        @DisplayName("shows attunement info")
        void showsAttunementInfo() {
            magicItem.setRequiresAttunement(true);
            magicItem.attune(testCharacter);
            
            String info = magicItem.getDetailedInfo();
            
            assertTrue(info.contains("Requires Attunement"));
            assertTrue(info.contains("Attuned to Hero"));
        }

        @Test
        @DisplayName("toString includes effect count")
        void toStringIncludesEffectCount() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            magicItem.addEffect(StatBonusEffect.createPlusOneArmor());
            
            String str = magicItem.toString();
            
            assertTrue(str.contains("2 effect(s)"));
        }

        @Test
        @DisplayName("toString shows attunement status")
        void toStringShowsAttunement() {
            magicItem.setRequiresAttunement(true);
            
            String str = magicItem.toString();
            assertTrue(str.contains("requires attunement"));
            
            magicItem.attune(testCharacter);
            str = magicItem.toString();
            assertTrue(str.contains("attuned"));
        }

        @Test
        @DisplayName("copy creates duplicate")
        void copyCreatesDuplicate() {
            magicItem.addEffect(TeleportEffect.createBlinkstepSpark());
            magicItem.setRequiresAttunement(true);
            
            MagicItem copy = (MagicItem) magicItem.copy();
            
            assertEquals(magicItem.getName(), copy.getName());
            assertEquals(magicItem.getEffectCount(), copy.getEffectCount());
            assertEquals(magicItem.requiresAttunement(), copy.requiresAttunement());
            // Copy should not be attuned even if original was
            assertFalse(copy.isAttuned());
        }
    }
}