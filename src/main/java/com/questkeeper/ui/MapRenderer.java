package com.questkeeper.ui;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.state.GameState;
import com.questkeeper.ui.MapLayout.Coord;
import com.questkeeper.world.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Renders a {@link MapLayout} as a multi-line ASCII string.
 *
 * <p>Pure function — no state, no I/O, no terminal calls. The
 * {@code render(Campaign, GameState)} convenience overload pulls
 * visited rooms, current location, and lock state out of GameState.
 *
 * <p>Layout grid:
 * <ul>
 *   <li>{@code CELL_W = 14} chars wide (box including borders)</li>
 *   <li>{@code CELL_H = 3} lines tall (top border, label, bottom border)</li>
 *   <li>{@code H_GAP = 2} between cells horizontally (room for {@code ──})</li>
 *   <li>{@code V_GAP = 1} between cells vertically (room for {@code │})</li>
 * </ul>
 */
public final class MapRenderer {

    private static final int CELL_W = 16;
    private static final int CELL_H = 3;
    private static final int H_GAP = 2;
    private static final int V_GAP = 1;
    private static final int LABEL_WIDTH = CELL_W - 2; // 14 chars between │ │
    private static final int FRAME_WIDTH = 60;

    private MapRenderer() {}

    /** Convenience overload for game-loop callers. */
    public static String render(Campaign campaign, GameState state) {
        Set<String> visited = state.getVisitedLocations();
        MapLayout layout = MapLayout.compute(campaign, visited);
        String currentId = state.getCurrentLocationId();
        Predicate<String> isUnlocked = state::isLocationUnlocked;
        return render(campaign, layout, currentId, isUnlocked);
    }

    /**
     * Test-friendly variant: caller supplies the layout, current room,
     * and a predicate for lock state. Returns the rendered string.
     */
    public static String render(Campaign campaign, MapLayout layout,
                                String currentRoomId,
                                Predicate<String> isUnlocked) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb);

        if (layout.getCoords().isEmpty() && layout.getOrphans().isEmpty()) {
            sb.append("\n  You haven't been anywhere yet. Try `look` and `go <direction>`.\n\n");
            return sb.toString();
        }

        appendGrid(sb, campaign, layout, currentRoomId, isUnlocked);
        appendVerticalStacks(sb, campaign, layout, isUnlocked);
        appendOrphans(sb, campaign, layout);
        appendConflicts(sb, campaign, layout);
        appendCounts(sb, campaign, layout, currentRoomId);
        appendVisitedListing(sb, campaign, layout, currentRoomId, isUnlocked);
        appendLegend(sb);
        return colorizeOutput(sb.toString());
    }

    // ==========================================
    // Header / framing
    // ==========================================

    private static void appendHeader(StringBuilder sb) {
        String border = "═".repeat(FRAME_WIDTH - 2);
        sb.append("╔").append(border).append("╗\n");
        sb.append("║").append(center("WORLD MAP", FRAME_WIDTH - 2)).append("║\n");
        sb.append("╚").append(border).append("╝\n");
    }

    // ==========================================
    // Main grid
    // ==========================================

    private static void appendGrid(StringBuilder sb, Campaign campaign,
                                   MapLayout layout, String currentRoomId,
                                   Predicate<String> isUnlocked) {
        Map<String, Coord> coords = layout.getCoords();
        if (coords.isEmpty()) return;

        int minX = coords.values().stream().mapToInt(Coord::x).min().orElse(0);
        int maxX = coords.values().stream().mapToInt(Coord::x).max().orElse(0);
        int minY = coords.values().stream().mapToInt(Coord::y).min().orElse(0);
        int maxY = coords.values().stream().mapToInt(Coord::y).max().orElse(0);

        int nx = maxX - minX + 1;
        int ny = maxY - minY + 1;
        int width = nx * CELL_W + (nx - 1) * H_GAP;
        int height = ny * CELL_H + (ny - 1) * V_GAP;

        char[][] grid = new char[height][width];
        for (char[] row : grid) java.util.Arrays.fill(row, ' ');

        // Place each room's box. Multi-room stacks share a coord; only
        // the first encountered wins the cell — additional stack members
        // are surfaced in the vertical inset below.
        for (var entry : coords.entrySet()) {
            String roomId = entry.getKey();
            Coord c = entry.getValue();
            int gx = (c.x() - minX) * (CELL_W + H_GAP);
            int gy = (c.y() - minY) * (CELL_H + V_GAP);
            String label = labelFor(roomId, currentRoomId, layout, campaign, isUnlocked);
            paintBox(grid, gx, gy, label, /* skip= */ cellHasBox(grid, gx, gy));
        }

        // Connectors between cardinally-adjacent placed rooms.
        Set<Coord> placedSet = new HashSet<>(coords.values());
        for (var entry : coords.entrySet()) {
            Coord c = entry.getValue();
            int gx = (c.x() - minX) * (CELL_W + H_GAP);
            int gy = (c.y() - minY) * (CELL_H + V_GAP);
            if (placedSet.contains(new Coord(c.x() + 1, c.y()))) {
                int connRow = gy + 1;
                int connColStart = gx + CELL_W;
                for (int i = 0; i < H_GAP; i++) {
                    grid[connRow][connColStart + i] = '─';
                }
            }
            if (placedSet.contains(new Coord(c.x(), c.y() + 1))) {
                int connCol = gx + CELL_W / 2;
                int connRowStart = gy + CELL_H;
                for (int i = 0; i < V_GAP; i++) {
                    grid[connRowStart + i][connCol] = '│';
                }
            }
        }

        for (char[] row : grid) {
            sb.append("\n").append(new String(row).replaceAll("\\s+$", ""));
        }
        sb.append("\n");
    }

    private static boolean cellHasBox(char[][] grid, int gx, int gy) {
        if (gy >= grid.length || gx >= grid[0].length) return false;
        return grid[gy][gx] == '┌';
    }

    private static void paintBox(char[][] grid, int gx, int gy, String label, boolean skip) {
        if (skip) return;
        if (gy + CELL_H > grid.length || gx + CELL_W > grid[0].length) return;

        grid[gy][gx] = '┌';
        for (int i = 1; i < CELL_W - 1; i++) grid[gy][gx + i] = '─';
        grid[gy][gx + CELL_W - 1] = '┐';

        grid[gy + 1][gx] = '│';
        String fitted = fitLabel(label, LABEL_WIDTH);
        for (int i = 0; i < LABEL_WIDTH; i++) {
            grid[gy + 1][gx + 1 + i] = i < fitted.length() ? fitted.charAt(i) : ' ';
        }
        grid[gy + 1][gx + CELL_W - 1] = '│';

        grid[gy + 2][gx] = '└';
        for (int i = 1; i < CELL_W - 1; i++) grid[gy + 2][gx + i] = '─';
        grid[gy + 2][gx + CELL_W - 1] = '┘';
    }

    /**
     * Builds the in-cell label for a room: name (or `?`), with `[*]`
     * prefix if it's the current room. Falls back to the room id if
     * no display name is available. Strips a leading "The " from long
     * names so they fit better in the 14-char label slot.
     */
    private static String labelFor(String roomId, String currentRoomId,
                                   MapLayout layout, Campaign campaign,
                                   Predicate<String> isUnlocked) {
        if (layout.isPeekAhead(roomId)) {
            return "?";
        }
        Location loc = campaign.getLocation(roomId);
        String name = (loc != null && loc.getName() != null && !loc.getName().isEmpty())
            ? loc.getName()
            : roomId;
        String compact = compactForCell(name);
        String marker = roomId.equals(currentRoomId) ? "[*]" : "";
        return marker + compact;
    }

    /**
     * Squeezes a room display name to fit the cell label slot:
     * drops a leading "The " when the original wouldn't fit, since
     * that's almost always filler. Idempotent on already-short names.
     */
    private static String compactForCell(String name) {
        if (name.length() <= LABEL_WIDTH) return name;
        if (name.startsWith("The ")) {
            String stripped = name.substring(4);
            if (stripped.length() <= LABEL_WIDTH) return stripped;
            return stripped;
        }
        return name;
    }

    private static String fitLabel(String s, int width) {
        if (s == null) s = "";
        if (s.length() <= width) {
            int padTotal = width - s.length();
            int padLeft = padTotal / 2;
            int padRight = padTotal - padLeft;
            return " ".repeat(padLeft) + s + " ".repeat(padRight);
        }
        return s.substring(0, width - 1) + "…";
    }

    private static String center(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        int pad = width - s.length();
        int left = pad / 2;
        int right = pad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    // ==========================================
    // Vertical stacks inset
    // ==========================================

    private static void appendVerticalStacks(StringBuilder sb, Campaign campaign,
                                             MapLayout layout,
                                             Predicate<String> isUnlocked) {
        List<List<String>> stacks = layout.getVerticalStacks();
        if (stacks.isEmpty()) return;

        sb.append("\n").append(Display.info("Vertical paths:")).append("\n");
        for (List<String> stack : stacks) {
            sb.append("  ");
            for (int i = 0; i < stack.size(); i++) {
                String id = stack.get(i);
                Location loc = campaign.getLocation(id);
                String name = loc != null ? loc.getName() : id;
                sb.append(name);
                if (!isUnlocked.test(id)) sb.append(" (locked)");
                if (i < stack.size() - 1) sb.append(" ⇕ ");
            }
            sb.append("\n");
        }
    }

    // ==========================================
    // Orphans footer
    // ==========================================

    private static void appendOrphans(StringBuilder sb, Campaign campaign, MapLayout layout) {
        List<String> orphans = layout.getOrphans();
        if (orphans.isEmpty()) return;

        sb.append("\n").append(Display.warn("Other locations discovered:")).append("\n");
        List<String> sorted = new ArrayList<>(orphans);
        sorted.sort(Comparator.comparing(id -> {
            Location loc = campaign.getLocation(id);
            return loc != null ? loc.getName() : id;
        }, String.CASE_INSENSITIVE_ORDER));
        for (String id : sorted) {
            Location loc = campaign.getLocation(id);
            String name = loc != null ? loc.getName() : id;
            sb.append("  - ").append(name).append("\n");
        }
    }

    // ==========================================
    // Conflicts footer
    // ==========================================

    private static void appendConflicts(StringBuilder sb, Campaign campaign, MapLayout layout) {
        List<MapLayout.Conflict> conflicts = layout.getConflicts();
        if (conflicts.isEmpty()) return;

        sb.append("\n").append(Display.warn(
            "Layout warnings (" + conflicts.size() + "):")).append("\n");
        int shown = Math.min(conflicts.size(), 3);
        for (int i = 0; i < shown; i++) {
            var c = conflicts.get(i);
            sb.append("  - ").append(displayName(campaign, c.fromRoom()))
              .append(" → ").append(c.direction()).append(" → ")
              .append(displayName(campaign, c.targetRoom()))
              .append("\n");
        }
        if (conflicts.size() > shown) {
            sb.append("  ... and ").append(conflicts.size() - shown).append(" more\n");
        }
    }

    private static String displayName(Campaign campaign, String roomId) {
        Location loc = campaign.getLocation(roomId);
        return (loc != null && loc.getName() != null && !loc.getName().isEmpty())
            ? loc.getName()
            : roomId;
    }

    // ==========================================
    // Counts + legend
    // ==========================================

    private static void appendCounts(StringBuilder sb, Campaign campaign,
                                     MapLayout layout, String currentRoomId) {
        int discovered = layout.getVisited().size();
        int total = campaign.getLocations().size();
        sb.append("\nLocations discovered: ")
          .append(discovered).append(" / ").append(total).append("\n");
        if (currentRoomId != null) {
            Location cur = campaign.getLocation(currentRoomId);
            sb.append("Current: ")
              .append(cur != null ? cur.getName() : currentRoomId)
              .append("\n");
        }
    }

    /**
     * Lists every visited room by display name. Cells truncate long
     * names to fit the box, but the listing always shows the full name
     * so tests and players can find every place they've been.
     */
    private static void appendVisitedListing(StringBuilder sb, Campaign campaign,
                                             MapLayout layout, String currentRoomId,
                                             Predicate<String> isUnlocked) {
        Set<String> visited = layout.getVisited();
        if (visited.isEmpty()) return;

        List<String> sorted = new ArrayList<>(visited);
        sorted.sort(Comparator.comparing(id -> {
            Location loc = campaign.getLocation(id);
            return loc != null ? loc.getName() : id;
        }, String.CASE_INSENSITIVE_ORDER));

        sb.append("\n").append(Display.info("Visited:")).append("\n");
        for (String id : sorted) {
            Location loc = campaign.getLocation(id);
            String name = loc != null ? loc.getName() : id;
            String marker = id.equals(currentRoomId) ? "[*] " : "    ";
            String lockTag = isUnlocked.test(id) ? "" : " (locked)";
            sb.append("  ").append(marker).append(name).append(lockTag).append("\n");
        }
    }

    private static void appendLegend(StringBuilder sb) {
        sb.append("\n").append(Display.info("Legend:")).append("\n");
        sb.append("  [*] You are here    ?  Unvisited (you can see it from here)\n");
        sb.append("  ⇕ Vertical path     (locked) Currently inaccessible\n");
    }

    /**
     * Post-processes the assembled output to add color highlights.
     * Operates on simple token substitutions so the grid's width math
     * (computed before this step in plain ASCII) isn't disturbed.
     * When colors are disabled, Display.colorize is a no-op and the
     * output is unchanged.
     */
    private static String colorizeOutput(String raw) {
        String coloredStar = Display.success("[*]");
        String coloredLocked = Display.danger("(locked)");
        return raw
            .replace("[*]", coloredStar)
            .replace("(locked)", coloredLocked);
    }
}
