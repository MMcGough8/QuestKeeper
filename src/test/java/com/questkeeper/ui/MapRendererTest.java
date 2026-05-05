package com.questkeeper.ui;

import com.questkeeper.campaign.Campaign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MapRenderer — the ASCII string output for the world map.
 *
 * Tests assert that the rendered output contains expected substrings
 * (room names, markers, footer sections), not exact glyph layouts —
 * the latter would be too brittle.
 */
@DisplayName("MapRenderer")
class MapRendererTest {

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

    private static final Predicate<String> ALL_UNLOCKED = id -> true;

    // ==========================================
    // Empty / trivial
    // ==========================================

    @Nested
    @DisplayName("Empty / trivial layouts")
    class TrivialTests {

        @Test
        @DisplayName("empty visited set renders a friendly hint")
        void emptyVisitedHint() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: A room.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of());
            String out = MapRenderer.render(c, layout, null, ALL_UNLOCKED);

            assertTrue(out.toLowerCase().contains("haven"),
                "expected a 'haven't been anywhere' hint; got:\n" + out);
        }

        @Test
        @DisplayName("single visited room shows its name and the [*] marker")
        void singleRoomShowsCurrentMarker() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: The Hub
                    description: A room.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("hub"));
            String out = MapRenderer.render(c, layout, "hub", ALL_UNLOCKED);

            assertTrue(out.contains("The Hub"), "room name should appear; got:\n" + out);
            assertTrue(out.contains("[*]"), "current marker should appear; got:\n" + out);
        }
    }

    // ==========================================
    // Cardinal grid
    // ==========================================

    @Nested
    @DisplayName("Cardinal grid")
    class CardinalGridTests {

        @Test
        @DisplayName("T-shape: all five rooms appear by name")
        void tShapeAllRoomsAppear() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: c.
                    exits:
                      north: north_room
                      south: south_room
                      east: east_room
                      west: west_room
                  - id: north_room
                    name: NorthRoom
                    description: n.
                  - id: south_room
                    name: SouthRoom
                    description: s.
                  - id: east_room
                    name: EastRoom
                    description: e.
                  - id: west_room
                    name: WestRoom
                    description: w.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("hub", "north_room", "south_room", "east_room", "west_room"));
            String out = MapRenderer.render(c, layout, "hub", ALL_UNLOCKED);

            assertTrue(out.contains("Hub"));
            assertTrue(out.contains("NorthRoom"));
            assertTrue(out.contains("SouthRoom"));
            assertTrue(out.contains("EastRoom"));
            assertTrue(out.contains("WestRoom"));
        }
    }

    // ==========================================
    // Peek-ahead
    // ==========================================

    @Nested
    @DisplayName("Peek-ahead")
    class PeekAheadTests {

        @Test
        @DisplayName("peek-ahead room renders as ? not by name")
        void peekAheadShowsQuestionMark() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                    exits:
                      east: secret_room
                  - id: secret_room
                    name: SecretRoom
                    description: s.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED);

            assertTrue(out.contains("?"), "peek-ahead marker should appear; got:\n" + out);
            assertFalse(out.contains("SecretRoom"),
                "peek-ahead room name should be hidden until visited; got:\n" + out);
        }
    }

    // ==========================================
    // Locked rooms
    // ==========================================

    @Nested
    @DisplayName("Locked rooms")
    class LockedRoomTests {

        @Test
        @DisplayName("locked visited room shows a locked marker")
        void lockedVisitedRoomShowsMarker() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                    exits:
                      east: vault
                  - id: vault
                    name: Vault
                    description: v.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "vault"));
            // vault is locked
            Predicate<String> isUnlocked = id -> !id.equals("vault");
            String out = MapRenderer.render(c, layout, "a", isUnlocked);

            assertTrue(out.contains("Vault"));
            assertTrue(out.toLowerCase().contains("locked"),
                "locked marker should appear; got:\n" + out);
        }
    }

    // ==========================================
    // Vertical stacks
    // ==========================================

    @Nested
    @DisplayName("Vertical stacks")
    class VerticalStackTests {

        @Test
        @DisplayName("vertical stack appears in inset section")
        void verticalStackInsetSection() throws IOException {
            Campaign c = loadCampaign("ground", """
                locations:
                  - id: ground
                    name: Ground
                    description: floor 1.
                    exits:
                      up: middle
                  - id: middle
                    name: Middle
                    description: floor 2.
                    exits:
                      up: top
                      down: ground
                  - id: top
                    name: Top
                    description: floor 3.
                    exits:
                      down: middle
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("ground", "middle", "top"));
            String out = MapRenderer.render(c, layout, "ground", ALL_UNLOCKED);

            int insetStart = out.toLowerCase().indexOf("vertical paths");
            assertTrue(insetStart >= 0, "vertical paths section should appear; got:\n" + out);
            // The inset is one line after the header; clip to that line so
            // the cell-painted "[*]Ground" doesn't shift indexes.
            int lineEnd = out.indexOf('\n', out.indexOf('\n', insetStart) + 1);
            String inset = out.substring(insetStart, lineEnd > 0 ? lineEnd : out.length());
            int topIdx = inset.indexOf("Top");
            int midIdx = inset.indexOf("Middle");
            int gndIdx = inset.indexOf("Ground");
            assertTrue(topIdx >= 0 && midIdx >= 0 && gndIdx >= 0,
                "inset missing one of top/middle/ground; got:\n" + inset);
            assertTrue(topIdx < midIdx && midIdx < gndIdx,
                "inset should list rooms top->middle->ground; got idx " + topIdx + "," + midIdx + "," + gndIdx);
        }
    }

    // ==========================================
    // Orphans
    // ==========================================

    @Nested
    @DisplayName("Orphan rooms")
    class OrphanRoomTests {

        @Test
        @DisplayName("orphans listed in 'Other locations' footer")
        void orphansFooterSection() throws IOException {
            Campaign c = loadCampaign("hub", """
                locations:
                  - id: hub
                    name: Hub
                    description: h.
                  - id: secret_office
                    name: SecretOffice
                    description: a hidden office.
                """);
            // secret_office is visited but has no cardinal route -> orphan.
            MapLayout layout = MapLayout.compute(c, Set.of("hub", "secret_office"));
            String out = MapRenderer.render(c, layout, "hub", ALL_UNLOCKED);

            assertTrue(out.toLowerCase().contains("other locations"),
                "footer section should appear; got:\n" + out);
            assertTrue(out.contains("SecretOffice"),
                "orphan room should be named in footer; got:\n" + out);
        }
    }

    // ==========================================
    // Discovered count + legend
    // ==========================================

    @Nested
    @DisplayName("Footer details")
    class FooterTests {

        @Test
        @DisplayName("renders 'discovered: N / total' line")
        void rendersDiscoveredCount() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                    exits:
                      east: b
                  - id: b
                    name: B
                    description: b.
                    exits:
                      west: a
                  - id: c
                    name: C
                    description: c.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED);

            assertTrue(out.contains("2"), "discovered count should be 2; got:\n" + out);
            assertTrue(out.contains("3"), "total count should be 3; got:\n" + out);
        }

        @Test
        @DisplayName("includes a legend that explains the [*] and ? markers")
        void includesLegend() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED);

            assertTrue(out.toLowerCase().contains("legend"),
                "legend section should appear; got:\n" + out);
        }
    }

    // ==========================================
    // Real-campaign smoke tests
    // ==========================================

    @Nested
    @DisplayName("Real campaign smoke tests")
    class RealCampaignTests {

        @Test
        @DisplayName("Muddlebrook mid-game contains all visited names")
        void muddlebrookMidGame() throws Exception {
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
            String out = MapRenderer.render(c, layout, "town_square", ALL_UNLOCKED);

            assertTrue(out.contains("Town Square"));
            assertTrue(out.contains("Town Hall"));
            assertTrue(out.contains("Market Row"));
            assertTrue(out.contains("[*]"), "current marker should appear");
        }

        @Test
        @DisplayName("Eberron with plaza visited renders plaza + cardinals")
        void eberronAtPlaza() throws Exception {
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
            String out = MapRenderer.render(c, layout, "convergence_plaza", ALL_UNLOCKED);

            assertTrue(out.contains("Convergence Plaza"));
            assertTrue(out.contains("Registration Hall"));
            assertTrue(out.contains("Merchant Row"));
            assertTrue(out.contains("Grand Arena"));
        }

        @Test
        @DisplayName("DrownedGod deep dive shows the vertical inset")
        void drownedgodDeepDive() throws Exception {
            Campaign c = Campaign.loadFromYaml(
                Path.of("src/main/resources/campaigns/drownedgod"));
            Set<String> visited = Set.of(
                "harbor_district",
                "flooded_underdock",
                "sunken_ruins_entrance",
                "the_drowning_heart"
            );
            MapLayout layout = MapLayout.compute(c, visited);
            String out = MapRenderer.render(c, layout, "harbor_district", ALL_UNLOCKED);

            assertTrue(out.toLowerCase().contains("vertical"),
                "vertical paths inset should appear");
            assertTrue(out.contains("Harbor District"));
            assertTrue(out.contains("Drowning Heart") || out.contains("The Drowning Heart"),
                "deepest room should appear in the inset");
        }
    }
}
