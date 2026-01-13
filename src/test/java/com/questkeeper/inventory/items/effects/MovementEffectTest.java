package com.questkeeper.inventory.items.effects;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MovementEffect class.
 *
 * @author Marc McGough
 */
@DisplayName("MovementEffect")
class MovementEffectTest {

    private Character testUser;

    @BeforeEach
    void setUp() {
        testUser = new Character("Test Hero", Race.HUMAN, CharacterClass.FIGHTER);
    }

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

    @Test
    @DisplayName("setMovementType updates type")
    void setMovementTypeUpdates() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_BONUS, 10);
        effect.setMovementType(MovementEffect.MovementType.FLYING);

        assertEquals(MovementEffect.MovementType.FLYING, effect.getMovementType());
    }

    @Test
    @DisplayName("setSpeedValue updates speed")
    void setSpeedValueUpdates() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_BONUS, 10);
        effect.setSpeedValue(20);

        assertEquals(20, effect.getSpeedValue());
    }

    @Test
    @DisplayName("setSpeedValue clamps negative values")
    void setSpeedValueClampsNegative() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_BONUS, 10);
        effect.setSpeedValue(-10);

        assertEquals(0, effect.getSpeedValue());
    }

    @Test
    @DisplayName("setDuration updates duration")
    void setDurationUpdates() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.FLYING, 30);
        effect.setDuration(60);

        assertEquals(60, effect.getDuration());
    }

    @Test
    @DisplayName("setDuration clamps negative values")
    void setDurationClampsNegative() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.FLYING, 30);
        effect.setDuration(-10);

        assertEquals(0, effect.getDuration());
    }

    @Test
    @DisplayName("setRequiresAttunement updates attunement")
    void setRequiresAttunementUpdates() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_BONUS, 10);
        assertFalse(effect.requiresAttunement());

        effect.setRequiresAttunement(true);
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("setHovering updates hovering")
    void setHoveringUpdates() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.FLYING, 30);
        assertFalse(effect.canHover());

        effect.setHovering(true);
        assertTrue(effect.canHover());
    }

    @Test
    @DisplayName("setActivationAction updates activation")
    void setActivationActionUpdates() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_BONUS, 10);
        assertEquals("none", effect.getActivationAction());

        effect.setActivationAction("bonus action");
        assertEquals("bonus action", effect.getActivationAction());
    }

    @Test
    @DisplayName("calculateModifiedSpeed for SPEED_SET")
    void calculateModifiedSpeedForSpeedSet() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_SET, 40);

        assertEquals(40, effect.calculateModifiedSpeed(30));
        assertEquals(40, effect.calculateModifiedSpeed(50)); // Sets regardless of base
    }

    @Test
    @DisplayName("getSpecialMovementSpeed for swimming")
    void getSpecialMovementSpeedSwimming() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SWIMMING, 60);

        assertEquals(60, effect.getSpecialMovementSpeed());
    }

    @Test
    @DisplayName("getSpecialMovementSpeed for climbing")
    void getSpecialMovementSpeedClimbing() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.CLIMBING, 30);

        assertEquals(30, effect.getSpecialMovementSpeed());
    }

    @Test
    @DisplayName("getSpecialMovementSpeed for burrowing")
    void getSpecialMovementSpeedBurrowing() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.BURROWING, 20);

        assertEquals(20, effect.getSpecialMovementSpeed());
    }

    @Test
    @DisplayName("getSpecialMovementSpeed returns 0 for non-movement types")
    void getSpecialMovementSpeedZeroForNonMovement() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.SPEED_BONUS, 10);

        assertEquals(0, effect.getSpecialMovementSpeed());
    }

    @Test
    @DisplayName("grantsMovementMode returns false for different types")
    void grantsMovementModeReturnsFalseForDifferent() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.FLYING, 30);

        assertTrue(effect.grantsMovementMode(MovementEffect.MovementType.FLYING));
        assertFalse(effect.grantsMovementMode(MovementEffect.MovementType.SWIMMING));
    }

    @Test
    @DisplayName("getDetailedInfo includes speed value")
    void getDetailedInfoIncludesSpeedValue() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.FLYING, 60);
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("60"));
        assertTrue(info.contains("ft"));
    }

    @Test
    @DisplayName("getDetailedInfo includes hovering for flying")
    void getDetailedInfoIncludesHovering() {
        MovementEffect effect = MovementEffect.createBroomOfFlying();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("hover"));
    }

    @Test
    @DisplayName("getDetailedInfo includes duration when set")
    void getDetailedInfoIncludesDuration() {
        MovementEffect effect = MovementEffect.createBootsOfSpeed();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Duration"));
        assertTrue(info.contains("10"));
    }

    @Test
    @DisplayName("getDetailedInfo includes activation action")
    void getDetailedInfoIncludesActivation() {
        MovementEffect effect = MovementEffect.createBootsOfSpeed();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Activation"));
        assertTrue(info.contains("bonus action"));
    }

    @Test
    @DisplayName("getDetailedInfo includes attunement")
    void getDetailedInfoIncludesAttunement() {
        MovementEffect effect = MovementEffect.createBootsOfSpeed();
        String info = effect.getDetailedInfo();

        assertTrue(info.contains("Requires attunement"));
    }

    @Test
    @DisplayName("MovementType enum has display names")
    void movementTypeHasDisplayNames() {
        for (MovementEffect.MovementType type : MovementEffect.MovementType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("createBroomOfFlying factory method")
    void createBroomOfFlyingFactory() {
        MovementEffect effect = MovementEffect.createBroomOfFlying();

        assertEquals("broom_flying_effect", effect.getId());
        assertEquals(MovementEffect.MovementType.FLYING, effect.getMovementType());
        assertEquals(50, effect.getSpeedValue());
        assertTrue(effect.canHover());
    }

    @Test
    @DisplayName("createRingOfWaterWalking factory method")
    void createRingOfWaterWalkingFactory() {
        MovementEffect effect = MovementEffect.createRingOfWaterWalking();

        assertEquals("ring_water_walking_effect", effect.getId());
        assertEquals(MovementEffect.MovementType.WATER_WALKING, effect.getMovementType());
    }

    @Test
    @DisplayName("createSlippersOfSpiderClimbing factory method")
    void createSlippersOfSpiderClimbingFactory() {
        MovementEffect effect = MovementEffect.createSlippersOfSpiderClimbing();

        assertEquals("slippers_spider_effect", effect.getId());
        assertEquals(MovementEffect.MovementType.SPIDER_CLIMB, effect.getMovementType());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createBootsOfStridingAndSpringing factory method")
    void createBootsOfStridingAndSpringingFactory() {
        MovementEffect effect = MovementEffect.createBootsOfStridingAndSpringing();

        assertEquals("boots_striding_effect", effect.getId());
        assertEquals(MovementEffect.MovementType.SPEED_SET, effect.getMovementType());
        assertEquals(30, effect.getSpeedValue());
        assertTrue(effect.requiresAttunement());
    }

    @Test
    @DisplayName("createCloakOfMantaRay factory method")
    void createCloakOfMantaRayFactory() {
        MovementEffect effect = MovementEffect.createCloakOfMantaRay();

        assertEquals("cloak_manta_effect", effect.getId());
        assertEquals(MovementEffect.MovementType.SWIMMING, effect.getMovementType());
        assertEquals(60, effect.getSpeedValue());
    }

    @Test
    @DisplayName("use returns appropriate message")
    void useReturnsMessage() {
        MovementEffect effect = MovementEffect.createWingedBoots();
        String result = effect.use(testUser);

        assertTrue(result.contains("Test Hero"));
    }

    @Test
    @DisplayName("constructor with duration sets values correctly")
    void constructorWithDuration() {
        MovementEffect effect = new MovementEffect("test", "Test",
                MovementEffect.MovementType.FLYING, 30,
                UsageType.DAILY, 1, 10);

        assertEquals(30, effect.getSpeedValue());
        assertEquals(UsageType.DAILY, effect.getUsageType());
        assertEquals(1, effect.getMaxCharges());
        assertEquals(10, effect.getDuration());
    }
}
