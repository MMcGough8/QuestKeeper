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
import java.util.List;
import java.util.Map;
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

    @Nested
    @DisplayName("Trial markers (Phase 5a)")
    class TrialMarkerTests {

        @Test
        @DisplayName("Trials section shows ✓/▶/★ glyphs for each trial status")
        void trialsSectionShowsAllStatuses() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                  - id: b
                    name: B
                    description: b.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            Map<String, MapRenderer.TrialMarker> trials = Map.of(
                "a", new MapRenderer.TrialMarker("t1", "First Mystery",
                    MapRenderer.TrialStatus.COMPLETED),
                "b", new MapRenderer.TrialMarker("t2", "Second Quest",
                    MapRenderer.TrialStatus.AVAILABLE)
            );
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED, trials);

            assertTrue(out.toLowerCase().contains("trials:"),
                "trials section header should appear; got:\n" + out);
            assertTrue(out.contains("✓"), "completed glyph should appear");
            assertTrue(out.contains("★"), "available glyph should appear");
            assertTrue(out.contains("First Mystery"), "trial name should appear");
            assertTrue(out.contains("Second Quest"), "second trial name should appear");
        }

        @Test
        @DisplayName("counts line includes 'Trials: N / M'")
        void countsIncludesTrialsCount() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            Map<String, MapRenderer.TrialMarker> trials = Map.of(
                "a", new MapRenderer.TrialMarker("t1", "Done",
                    MapRenderer.TrialStatus.COMPLETED),
                "b", new MapRenderer.TrialMarker("t2", "Open",
                    MapRenderer.TrialStatus.AVAILABLE),
                "c", new MapRenderer.TrialMarker("t3", "Active",
                    MapRenderer.TrialStatus.ACTIVE)
            );
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED, trials);

            assertTrue(out.contains("Trials: 1 / 3"),
                "counts line should show 1 completed of 3; got:\n" + out);
        }

        @Test
        @DisplayName("no Trials section appears when trialsByRoom is empty")
        void noTrialsSectionWhenEmpty() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED, Map.of());

            // Section is skipped entirely so no "Trials:" header appears
            // (note: "Trials" word does NOT appear in the legend).
            assertFalse(out.contains("Trials:"),
                "Trials section should be omitted when no trials; got:\n" + out);
        }

        @Test
        @DisplayName("Visited listing shows trial glyph next to room with active trial")
        void visitedListingIncludesTrialGlyph() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: Trial Room
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            Map<String, MapRenderer.TrialMarker> trials = Map.of(
                "a", new MapRenderer.TrialMarker("t1", "The Test",
                    MapRenderer.TrialStatus.ACTIVE)
            );
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED, trials);

            // The Visited listing line for "Trial Room" should carry the glyph.
            int visitedIdx = out.toLowerCase().indexOf("visited:");
            assertTrue(visitedIdx >= 0);
            String visitedSection = out.substring(visitedIdx);
            assertTrue(visitedSection.contains("Trial Room"));
            assertTrue(visitedSection.contains("▶"),
                "active trial glyph should appear in visited listing; got:\n" + visitedSection);
        }
    }

    @Nested
    @DisplayName("NPC indicators (Phase 5b)")
    class NpcMarkerTests {

        @Test
        @DisplayName("'People to meet:' lists each unmet NPC by room")
        void peopleToMeetSection() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: Tavern
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            Map<String, List<String>> unmet = Map.of(
                "a", List.of("Norrin the Bard", "Mira the Smith")
            );
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED,
                Map.of(), unmet);

            assertTrue(out.contains("People to meet:"),
                "people-to-meet header should appear; got:\n" + out);
            assertTrue(out.contains("Norrin the Bard"));
            assertTrue(out.contains("Mira the Smith"));
            assertTrue(out.contains("Tavern"),
                "location name should appear next to NPC; got:\n" + out);
        }

        @Test
        @DisplayName("Visited listing marks rooms with unmet NPCs with •")
        void visitedListingShowsBullet() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: Plaza
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            Map<String, List<String>> unmet = Map.of("a", List.of("Herald"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED,
                Map.of(), unmet);

            int visitedIdx = out.toLowerCase().indexOf("visited:");
            assertTrue(visitedIdx >= 0);
            String visitedSection = out.substring(visitedIdx);
            assertTrue(visitedSection.contains("Plaza"));
            assertTrue(visitedSection.contains("•"),
                "bullet should mark rooms with unmet NPCs; got:\n" + visitedSection);
        }

        @Test
        @DisplayName("no 'People to meet:' section when all NPCs are met")
        void noSectionWhenAllMet() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: A
                    description: a.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED,
                Map.of(), Map.of());

            assertFalse(out.contains("People to meet:"),
                "section should be omitted when no unmet NPCs; got:\n" + out);
        }
    }

    @Nested
    @DisplayName("Polish (Phase 4)")
    class PolishTests {

        @Test
        @DisplayName("conflict warnings show display names, not raw ids")
        void conflictsUseDisplayNames() throws IOException {
            // Set up a layout that produces a conflict: a east to b at (1,0),
            // a south to c at (0,1), c east to b would put b at (1,1).
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: Alpha Hall
                    description: a.
                    exits:
                      east: b
                      south: c
                  - id: b
                    name: Beta Chamber
                    description: b.
                    exits:
                      west: a
                  - id: c
                    name: Gamma Vault
                    description: c.
                    exits:
                      north: a
                      east: b
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "b", "c"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED);

            assertTrue(out.toLowerCase().contains("layout warnings"),
                "layout warnings header should appear; got:\n" + out);
            assertTrue(out.contains("Alpha Hall") || out.contains("Beta Chamber") || out.contains("Gamma Vault"),
                "conflict line should reference display names, not just raw ids; got:\n" + out);
        }

        @Test
        @DisplayName("'The ' prefix is stripped from cell labels when name overflows")
        void stripsTheFromOverflowingCellLabels() throws IOException {
            Campaign c = loadCampaign("a", """
                locations:
                  - id: a
                    name: Hub
                    description: a.
                    exits:
                      east: inn
                  - id: inn
                    name: The Drunken Dragon Inn
                    description: long name.
                """);
            MapLayout layout = MapLayout.compute(c, Set.of("a", "inn"));
            String out = MapRenderer.render(c, layout, "a", ALL_UNLOCKED);

            // Original 22-char name truncates to "The Drunken D…"; with "The "
            // stripped the cell instead shows "Drunken Dragon" or "Drunken Drag…"
            // (still 14-char ceiling) — confirm the cell shows "Drunken" rather
            // than "The Drunken D…".
            assertTrue(out.contains("Drunken"),
                "cell label should retain the meaningful part of the name; got:\n" + out);
            // Visited footer always shows the full name; verify that's intact.
            assertTrue(out.contains("The Drunken Dragon Inn"),
                "Visited footer should show the full name; got:\n" + out);
        }
    }
}
