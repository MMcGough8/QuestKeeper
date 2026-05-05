package com.questkeeper.ui;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.ui.MapLayout.Coord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MapLayout — the BFS-based coordinate assignment that
 * powers the world map render. Pure data, no rendering.
 *
 * Each test builds a tiny synthetic campaign on disk via @TempDir,
 * then asserts that placed coords / orphans / conflicts / vertical
 * stacks match expectations. Real-campaign smoke tests at the bottom
 * confirm Muddlebrook, Eberron, and DrownedGod all lay out cleanly.
 */
@DisplayName("MapLayout")
class MapLayoutTest {

    @TempDir
    Path tempDir;

    private Path campaignDir;

    @BeforeEach
    void setUp() throws IOException {
        campaignDir = tempDir.resolve("test_campaign");
        Files.createDirectories(campaignDir);
    }

    private Campaign loadCampaign(String startingId, String locationsYaml) throws IOException {
        Files.writeString(campaignDir.resolve("campaign.yaml"), """
            id: test_campaign
            name: Test Campaign
            author: Test
            version: "1.0"
            starting_location: %s
            """.formatted(startingId));
        Files.writeString(campaignDir.resolve("locations.yaml"), locationsYaml);
        return Campaign.loadFromYaml(campaignDir);
    }

    // ==========================================
    // Single-room and trivial cases
    // ==========================================

    @Nested
    @DisplayName("Trivial layouts")
    class TrivialLayoutTests {

        @Test
        @DisplayName("single visited room places at (0,0)")
        void singleRoomAtOrigin() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: Just a room.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("hub"));

            assertEquals(new Coord(0, 0), layout.getCoord("hub"));
            assertTrue(layout.getOrphans().isEmpty());
            assertTrue(layout.getConflicts().isEmpty());
            assertTrue(layout.getVerticalStacks().isEmpty());
        }

        @Test
        @DisplayName("empty visited set yields empty layout")
        void emptyVisitedYieldsEmpty() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: Just a room.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of());

            assertTrue(layout.getCoords().isEmpty());
            assertTrue(layout.getPeekAhead().isEmpty());
        }

        @Test
        @DisplayName("starting room placed even if exits go nowhere")
        void startingRoomPlacedAlone() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: A room.
                    exits:
                      mystery_door: nowhere
                  - id: nowhere
                    name: Nowhere
                    description: Nada.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("hub"));

            assertEquals(new Coord(0, 0), layout.getCoord("hub"));
        }
    }

    // ==========================================
    // Cardinal placement
    // ==========================================

    @Nested
    @DisplayName("Cardinal BFS placement")
    class CardinalPlacementTests {

        @Test
        @DisplayName("east chain: A->B->C lays out at x=0,1,2")
        void eastChain() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      east: b
                  - id: b
                    name: B
                    description: B.
                    exits:
                      west: a
                      east: c
                  - id: c
                    name: C
                    description: C.
                    exits:
                      west: b
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b", "c"));

            assertEquals(new Coord(0, 0), layout.getCoord("a"));
            assertEquals(new Coord(1, 0), layout.getCoord("b"));
            assertEquals(new Coord(2, 0), layout.getCoord("c"));
        }

        @Test
        @DisplayName("T-shape: hub with N/S/E/W neighbors")
        void tShape() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: Center.
                    exits:
                      north: n
                      south: s
                      east: e
                      west: w
                  - id: n
                    name: N
                    description: north.
                  - id: s
                    name: S
                    description: south.
                  - id: e
                    name: E
                    description: east.
                  - id: w
                    name: W
                    description: west.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("hub", "n", "s", "e", "w"));

            assertEquals(new Coord(0, 0), layout.getCoord("hub"));
            assertEquals(new Coord(0, -1), layout.getCoord("n"));
            assertEquals(new Coord(0, 1), layout.getCoord("s"));
            assertEquals(new Coord(1, 0), layout.getCoord("e"));
            assertEquals(new Coord(-1, 0), layout.getCoord("w"));
        }

        @Test
        @DisplayName("asymmetric exit: A->south->B with no B->north->A still places B")
        void asymmetricExitsPlace() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      south: b
                  - id: b
                    name: B
                    description: B (no return exit).
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b"));

            assertEquals(new Coord(0, 0), layout.getCoord("a"));
            assertEquals(new Coord(0, 1), layout.getCoord("b"));
        }
    }

    // ==========================================
    // Peek-ahead
    // ==========================================

    @Nested
    @DisplayName("Peek-ahead (unvisited adjacent rooms)")
    class PeekAheadTests {

        @Test
        @DisplayName("unvisited room reachable via cardinal exit is peek-ahead")
        void unvisitedNeighborIsPeekAhead() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      east: b
                  - id: b
                    name: B
                    description: B.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));

            assertTrue(layout.isVisited("a"));
            assertFalse(layout.isVisited("b"));
            assertTrue(layout.isPeekAhead("b"));
            assertEquals(new Coord(1, 0), layout.getCoord("b"));
        }

        @Test
        @DisplayName("unvisited room reachable only via non-cardinal exit is NOT peek-ahead")
        void nonCardinalNeighborNotPeekAhead() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      door: b
                  - id: b
                    name: B
                    description: B.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));

            assertFalse(layout.isPeekAhead("b"));
            assertNull(layout.getCoord("b"));
        }
    }

    // ==========================================
    // Vertical stacks
    // ==========================================

    @Nested
    @DisplayName("Vertical stacks (up/down)")
    class VerticalStackTests {

        @Test
        @DisplayName("up/down chain becomes a single stack, no x/y growth")
        void verticalChainBecomesStack() throws IOException {
            Campaign c = loadCampaign("ground", """
                locations:
                  - id: ground
                    name: Ground
                    description: Floor 1.
                    exits:
                      up: middle
                  - id: middle
                    name: Middle
                    description: Floor 2.
                    exits:
                      up: top
                      down: ground
                  - id: top
                    name: Top
                    description: Floor 3.
                    exits:
                      down: middle
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("ground", "middle", "top"));

            // All three rooms share the same (x,y) — stacks don't grow the grid.
            assertEquals(new Coord(0, 0), layout.getCoord("ground"));
            assertEquals(new Coord(0, 0), layout.getCoord("middle"));
            assertEquals(new Coord(0, 0), layout.getCoord("top"));

            List<List<String>> stacks = layout.getVerticalStacks();
            assertEquals(1, stacks.size());
            // Top-to-bottom ordering.
            assertEquals(List.of("top", "middle", "ground"), stacks.get(0));
        }

        @Test
        @DisplayName("two unrelated vertical chains produce two stacks")
        void twoUnrelatedStacks() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      east: c
                      up: b
                  - id: b
                    name: B
                    description: B above A.
                    exits:
                      down: a
                  - id: c
                    name: C
                    description: C.
                    exits:
                      west: a
                      up: d
                  - id: d
                    name: D
                    description: D above C.
                    exits:
                      down: c
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b", "c", "d"));

            List<List<String>> stacks = layout.getVerticalStacks();
            assertEquals(2, stacks.size());
        }
    }

    // ==========================================
    // Orphans
    // ==========================================

    @Nested
    @DisplayName("Orphan rooms")
    class OrphanTests {

        @Test
        @DisplayName("visited room with no cardinal path lands in orphans")
        void unreachableVisitedRoomIsOrphan() throws IOException {
            // mayors_office is visited (via trial-teleport) but has no exits
            // and no cardinal exit points to it from anywhere.
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                  - id: mayors_office
                    name: Mayor's Office
                    description: An office.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "mayors_office"));

            assertEquals(new Coord(0, 0), layout.getCoord("a"));
            assertNull(layout.getCoord("mayors_office"));
            assertTrue(layout.getOrphans().contains("mayors_office"));
        }

        @Test
        @DisplayName("non-cardinal-only neighbor of visited room is orphan when visited")
        void nonCardinalReachableVisitedIsOrphan() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      door: b
                  - id: b
                    name: B
                    description: B.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b"));

            // 'door' is non-cardinal so b can't be placed on the grid.
            assertNull(layout.getCoord("b"));
            assertTrue(layout.getOrphans().contains("b"));
        }
    }

    // ==========================================
    // Conflicts
    // ==========================================

    @Nested
    @DisplayName("Cardinal conflicts")
    class ConflictTests {

        @Test
        @DisplayName("two paths to the same room at different coords records a conflict")
        void differentCoordsRecordsConflict() throws IOException {
            // a -> east -> b   (b at (1,0))
            // a -> south -> c  (c at (0,1))
            // c -> east -> b   (would put b at (1,1), conflict with (1,0))
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      east: b
                      south: c
                  - id: b
                    name: B
                    description: B.
                    exits:
                      west: a
                  - id: c
                    name: C
                    description: C.
                    exits:
                      north: a
                      east: b
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b", "c"));

            // First placement (BFS reaches b via a-east before c-east) wins.
            assertEquals(new Coord(1, 0), layout.getCoord("b"));
            assertFalse(layout.getConflicts().isEmpty(),
                "expected at least one conflict from c->east->b mismatch");
        }

        @Test
        @DisplayName("two different rooms claiming the same cell records a conflict")
        void sameCellTwoRoomsRecordsConflict() throws IOException {
            // a -> east -> b -> south -> d   (d at (1,1))
            // a -> south -> c -> east -> d2  (d2 also wants (1,1))
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: A.
                    exits:
                      east: b
                      south: c
                  - id: b
                    name: B
                    description: B.
                    exits:
                      south: d
                  - id: c
                    name: C
                    description: C.
                    exits:
                      east: d2
                  - id: d
                    name: D
                    description: D.
                  - id: d2
                    name: D2
                    description: D2.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b", "c", "d", "d2"));

            // Whichever wins, the other must be a conflict.
            int placedCount = (layout.getCoord("d") != null ? 1 : 0)
                + (layout.getCoord("d2") != null ? 1 : 0);
            assertEquals(1, placedCount,
                "exactly one of d/d2 should be placed; the other should be a conflict");
            assertFalse(layout.getConflicts().isEmpty());
        }
    }

    // ==========================================
    // Real-campaign smoke tests
    // ==========================================

    @Nested
    @DisplayName("Real campaign smoke tests")
    class RealCampaignTests {

        @Test
        @DisplayName("Muddlebrook: town_square neighbors place at expected cardinals")
        void muddlebrookSmoke() throws Exception {
            Campaign c = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/muddlebrook"));
            Set<String> visited = Set.of(
                "drunken_dragon_inn",
                "town_square",
                "town_hall",
                "market_row",
                "clocktower_hill"
            );
            MapLayout layout = MapLayout.compute(c, visited);

            Coord square = layout.getCoord("town_square");
            Coord hall = layout.getCoord("town_hall");
            Coord inn = layout.getCoord("drunken_dragon_inn");
            Coord market = layout.getCoord("market_row");
            Coord clockHill = layout.getCoord("clocktower_hill");

            assertNotNull(square, "town_square should be placed");
            assertNotNull(hall, "town_hall should be placed");
            assertNotNull(inn, "inn should be placed");
            assertNotNull(market, "market_row should be placed");
            assertNotNull(clockHill, "clocktower_hill should be placed");

            assertEquals(square.x(), hall.x());
            assertEquals(square.y() - 1, hall.y());
            assertEquals(square.x(), inn.x());
            assertEquals(square.y() + 1, inn.y());
            assertEquals(square.x() + 1, market.x());
            assertEquals(square.y(), market.y());
            assertEquals(square.x() - 1, clockHill.x());
            assertEquals(square.y(), clockHill.y());
        }

        @Test
        @DisplayName("Eberron: convergence_plaza neighbors place at expected cardinals")
        void eberronSmoke() throws Exception {
            Campaign c = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/eberron"));
            Set<String> visited = Set.of(
                "convergence_plaza",
                "registration_hall",
                "competitor_quarters",
                "merchant_row",
                "grand_arena"
            );
            MapLayout layout = MapLayout.compute(c, visited);

            Coord plaza = layout.getCoord("convergence_plaza");
            Coord registration = layout.getCoord("registration_hall");
            Coord quarters = layout.getCoord("competitor_quarters");
            Coord market = layout.getCoord("merchant_row");
            Coord arena = layout.getCoord("grand_arena");

            assertNotNull(plaza);
            assertEquals(plaza.x(), registration.x());
            assertEquals(plaza.y() - 1, registration.y());
            assertEquals(plaza.x() + 1, quarters.x());
            assertEquals(plaza.y(), quarters.y());
            assertEquals(plaza.x() - 1, market.x());
            assertEquals(plaza.y(), market.y());
            assertEquals(plaza.x(), arena.x());
            assertEquals(plaza.y() + 1, arena.y());
        }

        @Test
        @DisplayName("DrownedGod: vertical chain forms a stack")
        void drownedgodVerticalChain() throws Exception {
            Campaign c = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/drownedgod"));
            Set<String> visited = Set.of(
                "harbor_district",
                "flooded_underdock",
                "sunken_ruins_entrance",
                "the_drowning_heart"
            );
            MapLayout layout = MapLayout.compute(c, visited);

            List<List<String>> stacks = layout.getVerticalStacks();
            boolean foundDeepStack = stacks.stream()
                .anyMatch(s -> s.size() == 4
                    && s.contains("harbor_district")
                    && s.contains("flooded_underdock")
                    && s.contains("sunken_ruins_entrance")
                    && s.contains("the_drowning_heart"));
            assertTrue(foundDeepStack,
                "expected a 4-room vertical stack; got " + stacks);
        }
    }
}
