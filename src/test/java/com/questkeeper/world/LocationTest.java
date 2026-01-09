package com.questkeeper.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the Location class.
 * 
 * @author Marc McGough
 * @version 1.0
 */
@DisplayName("Location")
class LocationTest {

    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location("test_location", "Test Room", 
                "A plain test room.", "You enter the test room for the first time.");
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("creates location with all properties")
        void createsWithAllProperties() {
            Location loc = new Location("tavern", "The Tavern", 
                    "A cozy tavern.", "Welcome to the tavern!");
            
            assertEquals("tavern", loc.getId());
            assertEquals("The Tavern", loc.getName());
            assertEquals("A cozy tavern.", loc.getDescription());
            assertEquals("Welcome to the tavern!", loc.getReadAloudText());
        }

        @Test
        @DisplayName("creates location with minimal properties")
        void createsWithMinimalProperties() {
            Location loc = new Location("room", "Room");
            
            assertEquals("room", loc.getId());
            assertEquals("Room", loc.getName());
            assertEquals("", loc.getDescription());
            assertEquals("", loc.getReadAloudText());
        }

        @Test
        @DisplayName("throws exception for null ID")
        void throwsForNullId() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new Location(null, "Name"));
        }

        @Test
        @DisplayName("throws exception for empty ID")
        void throwsForEmptyId() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new Location("", "Name"));
        }

        @Test
        @DisplayName("throws exception for null name")
        void throwsForNullName() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new Location("id", null));
        }

        @Test
        @DisplayName("throws exception for empty name")
        void throwsForEmptyName() {
            assertThrows(IllegalArgumentException.class, 
                    () -> new Location("id", ""));
        }

        @Test
        @DisplayName("handles null description gracefully")
        void handlesNullDescription() {
            Location loc = new Location("id", "Name", null, null);
            assertEquals("", loc.getDescription());
            assertEquals("", loc.getReadAloudText());
        }

        @Test
        @DisplayName("location starts unlocked by default")
        void startsUnlocked() {
            assertTrue(location.isUnlocked());
        }

        @Test
        @DisplayName("location starts not visited")
        void startsNotVisited() {
            assertFalse(location.hasBeenVisited());
        }
    }

    @Nested
    @DisplayName("Exit Management")
    class ExitTests {

        @Test
        @DisplayName("adds exit successfully")
        void addsExit() {
            location.addExit("north", "other_room");
            
            assertTrue(location.hasExit("north"));
            assertEquals("other_room", location.getExit("north"));
        }

        @Test
        @DisplayName("exits are case-insensitive")
        void exitsAreCaseInsensitive() {
            location.addExit("NORTH", "other_room");
            
            assertTrue(location.hasExit("north"));
            assertTrue(location.hasExit("North"));
            assertTrue(location.hasExit("NORTH"));
            assertEquals("other_room", location.getExit("north"));
        }

        @Test
        @DisplayName("returns all exits")
        void returnsAllExits() {
            location.addExit("north", "room1");
            location.addExit("south", "room2");
            location.addExit("door", "room3");
            
            assertEquals(3, location.getExitCount());
            assertTrue(location.getExits().contains("north"));
            assertTrue(location.getExits().contains("south"));
            assertTrue(location.getExits().contains("door"));
        }

        @Test
        @DisplayName("returns null for nonexistent exit")
        void returnsNullForNoExit() {
            assertNull(location.getExit("north"));
            assertNull(location.getExit(null));
        }

        @Test
        @DisplayName("removes exit")
        void removesExit() {
            location.addExit("north", "room1");
            location.removeExit("north");
            
            assertFalse(location.hasExit("north"));
            assertEquals(0, location.getExitCount());
        }

        @Test
        @DisplayName("ignores null or empty exit additions")
        void ignoresInvalidExits() {
            location.addExit(null, "room");
            location.addExit("", "room");
            location.addExit("north", null);
            location.addExit("north", "");
            
            assertEquals(0, location.getExitCount());
        }

        @Test
        @DisplayName("getExitsDisplay formats correctly with no exits")
        void displaysNoExits() {
            assertEquals("There are no obvious exits.", location.getExitsDisplay());
        }

        @Test
        @DisplayName("getExitsDisplay formats correctly with exits")
        void displaysExits() {
            location.addExit("north", "room1");
            location.addExit("south", "room2");
            
            String display = location.getExitsDisplay();
            assertTrue(display.startsWith("Exits:"));
            assertTrue(display.contains("north"));
            assertTrue(display.contains("south"));
        }
    }

    @Nested
    @DisplayName("NPC Management")
    class NpcTests {

        @Test
        @DisplayName("adds NPC successfully")
        void addsNpc() {
            location.addNpc("norrin_bard");
            
            assertTrue(location.hasNpc("norrin_bard"));
            assertEquals(1, location.getNpcCount());
        }

        @Test
        @DisplayName("does not add duplicate NPCs")
        void noDuplicateNpcs() {
            location.addNpc("norrin_bard");
            location.addNpc("norrin_bard");
            
            assertEquals(1, location.getNpcCount());
        }

        @Test
        @DisplayName("removes NPC successfully")
        void removesNpc() {
            location.addNpc("norrin_bard");
            boolean removed = location.removeNpc("norrin_bard");
            
            assertTrue(removed);
            assertFalse(location.hasNpc("norrin_bard"));
            assertEquals(0, location.getNpcCount());
        }

        @Test
        @DisplayName("returns false when removing nonexistent NPC")
        void returnsFalseForNonexistentNpc() {
            assertFalse(location.removeNpc("nobody"));
        }

        @Test
        @DisplayName("returns all NPCs")
        void returnsAllNpcs() {
            location.addNpc("norrin_bard");
            location.addNpc("mara_bartender");
            
            assertEquals(2, location.getNpcCount());
            assertTrue(location.getNpcs().contains("norrin_bard"));
            assertTrue(location.getNpcs().contains("mara_bartender"));
        }

        @Test
        @DisplayName("hasNpc returns false for null")
        void hasNpcReturnsFalseForNull() {
            assertFalse(location.hasNpc(null));
        }

        @Test
        @DisplayName("ignores null or empty NPC additions")
        void ignoresInvalidNpcs() {
            location.addNpc(null);
            location.addNpc("");
            location.addNpc("   ");
            
            assertEquals(0, location.getNpcCount());
        }
    }

    @Nested
    @DisplayName("Item Management")
    class ItemTests {

        @Test
        @DisplayName("adds item successfully")
        void addsItem() {
            location.addItem("gold_coin");
            
            assertTrue(location.hasItem("gold_coin"));
            assertEquals(1, location.getItemCount());
        }

        @Test
        @DisplayName("allows duplicate items")
        void allowsDuplicateItems() {
            location.addItem("gold_coin");
            location.addItem("gold_coin");
            
            assertEquals(2, location.getItemCount());
        }

        @Test
        @DisplayName("removes item successfully")
        void removesItem() {
            location.addItem("gold_coin");
            boolean removed = location.removeItem("gold_coin");
            
            assertTrue(removed);
            assertFalse(location.hasItem("gold_coin"));
        }

        @Test
        @DisplayName("removes only one instance of duplicate items")
        void removesOnlyOneInstance() {
            location.addItem("gold_coin");
            location.addItem("gold_coin");
            location.removeItem("gold_coin");
            
            assertEquals(1, location.getItemCount());
            assertTrue(location.hasItem("gold_coin"));
        }

        @Test
        @DisplayName("returns false when removing nonexistent item")
        void returnsFalseForNonexistentItem() {
            assertFalse(location.removeItem("nothing"));
        }

        @Test
        @DisplayName("hasItem returns false for null")
        void hasItemReturnsFalseForNull() {
            assertFalse(location.hasItem(null));
        }
    }

    @Nested
    @DisplayName("Flag Management")
    class FlagTests {

        @Test
        @DisplayName("marks location as visited")
        void marksVisited() {
            assertFalse(location.hasBeenVisited());
            
            location.markVisited();
            
            assertTrue(location.hasBeenVisited());
        }

        @Test
        @DisplayName("locks and unlocks location")
        void locksAndUnlocks() {
            assertTrue(location.isUnlocked());
            
            location.lock();
            assertFalse(location.isUnlocked());
            
            location.unlock();
            assertTrue(location.isUnlocked());
        }

        @Test
        @DisplayName("sets custom flag")
        void setsCustomFlag() {
            location.setFlag("trial_complete");
            
            assertTrue(location.hasFlag("trial_complete"));
        }

        @Test
        @DisplayName("custom flags are case-insensitive")
        void flagsCaseInsensitive() {
            location.setFlag("TRIAL_COMPLETE");
            
            assertTrue(location.hasFlag("trial_complete"));
            assertTrue(location.hasFlag("TRIAL_COMPLETE"));
        }

        @Test
        @DisplayName("removes custom flag")
        void removesFlag() {
            location.setFlag("trial_complete");
            location.removeFlag("trial_complete");
            
            assertFalse(location.hasFlag("trial_complete"));
        }

        @Test
        @DisplayName("returns all flags")
        void returnsAllFlags() {
            location.setFlag("custom_flag");
            
            assertTrue(location.getFlags().contains("unlocked"));
            assertTrue(location.getFlags().contains("custom_flag"));
        }

        @Test
        @DisplayName("hasFlag returns false for null")
        void hasFlagReturnsFalseForNull() {
            assertFalse(location.hasFlag(null));
        }
    }

    @Nested
    @DisplayName("Display Description")
    class DisplayDescriptionTests {

        @Test
        @DisplayName("returns readAloudText on first visit")
        void returnsReadAloudOnFirstVisit() {
            String display = location.getDisplayDescription();
            
            assertEquals("You enter the test room for the first time.", display);
        }

        @Test
        @DisplayName("returns description after visited")
        void returnsDescriptionAfterVisit() {
            location.markVisited();
            String display = location.getDisplayDescription();
            
            assertEquals("A plain test room.", display);
        }

        @Test
        @DisplayName("returns description when no readAloudText")
        void returnsDescriptionWhenNoReadAloud() {
            Location loc = new Location("id", "Name", "Description", "");
            
            assertEquals("Description", loc.getDisplayDescription());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("locations with same ID are equal")
        void sameIdEquals() {
            Location loc1 = new Location("room", "Room 1");
            Location loc2 = new Location("room", "Room 2");
            
            assertEquals(loc1, loc2);
            assertEquals(loc1.hashCode(), loc2.hashCode());
        }

        @Test
        @DisplayName("locations with different IDs are not equal")
        void differentIdNotEquals() {
            Location loc1 = new Location("room1", "Room");
            Location loc2 = new Location("room2", "Room");
            
            assertNotEquals(loc1, loc2);
        }

        @Test
        @DisplayName("location not equal to null")
        void notEqualToNull() {
            assertNotEquals(null, location);
        }

        @Test
        @DisplayName("location not equal to other types")
        void notEqualToOtherType() {
            assertNotEquals("test_location", location);
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("creates Drunken Dragon Inn correctly")
        void createsDrunkenDragonInn() {
            Location inn = Location.createDrunkenDragonInn();
            
            assertEquals("drunken_dragon_inn", inn.getId());
            assertEquals("The Drunken Dragon Inn", inn.getName());
            assertTrue(inn.hasExit("north"));
            assertTrue(inn.hasExit("door"));
            assertTrue(inn.hasNpc("norrin_bard"));
            assertTrue(inn.hasNpc("mara_bartender"));
        }

        @Test
        @DisplayName("creates Town Square with correct exits")
        void createsTownSquare() {
            Location square = Location.createTownSquare();
            
            assertEquals("town_square", square.getId());
            assertTrue(square.hasExit("north"));
            assertTrue(square.hasExit("south"));
            assertTrue(square.hasExit("east"));
            assertTrue(square.hasExit("west"));
            assertEquals("drunken_dragon_inn", square.getExit("south"));
        }

        @Test
        @DisplayName("creates Mayor's Office as trial location")
        void createsMayorsOffice() {
            Location office = Location.createMayorsOffice();
            
            assertEquals("mayors_office", office.getId());
            assertTrue(office.hasFlag("trial_location"));
            assertTrue(office.hasExit("downstairs"));
        }

        @Test
        @DisplayName("creates Clocktower Hill as locked")
        void createsClocktowerHillLocked() {
            Location hill = Location.createClocktowerHill();
            
            assertEquals("clocktower_hill", hill.getId());
            assertFalse(hill.isUnlocked());
        }

        @Test
        @DisplayName("creates Clockwork Curios with Elara")
        void createsClockworkCurios() {
            Location shop = Location.createClockworkCurios();
            
            assertEquals("clockwork_curios", shop.getId());
            assertTrue(shop.hasNpc("elara_shopkeeper"));
            assertTrue(shop.hasExit("out"));
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("formats toString correctly")
        void formatsCorrectly() {
            location.addExit("north", "room1");
            location.addNpc("npc1");
            location.addItem("item1");
            location.addItem("item2");
            
            String str = location.toString();
            
            assertTrue(str.contains("test_location"));
            assertTrue(str.contains("Test Room"));
            assertTrue(str.contains("exits=1"));
            assertTrue(str.contains("npcs=1"));
            assertTrue(str.contains("items=2"));
        }
    }
}