package com.questkeeper.ui;

import com.questkeeper.campaign.Campaign;
import com.questkeeper.world.Location;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BFS-based coordinate assignment for the world map render.
 *
 * <p>Given a campaign and the player's visited rooms, builds:
 * <ul>
 *   <li>a {@link Coord} per placed room (visited and one-hop peek-ahead)</li>
 *   <li>a list of orphan rooms that couldn't be placed on the cardinal grid</li>
 *   <li>a list of conflicts where the campaign's exit graph is not 2D-embeddable</li>
 *   <li>vertical stacks (groups of rooms connected via up/down exits)</li>
 * </ul>
 *
 * <p>This is data only; rendering lives in {@code MapRenderer}.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>BFS from the campaign's starting location, anchored at (0, 0).</li>
 *   <li>For each cardinal exit, place the target at neighbor's coord + delta,
 *       or record a conflict if a different placement already exists.</li>
 *   <li>For each up/down exit, link the target into a vertical stack at the
 *       same coord (stacks don't grow the grid).</li>
 *   <li>Non-cardinal, non-vertical exits (e.g., {@code door}, {@code out})
 *       are skipped during placement.</li>
 *   <li>Visited rooms not reachable through cardinal/vertical exits become orphans.</li>
 * </ol>
 */
public final class MapLayout {

    /** A 2D grid coordinate. */
    public record Coord(int x, int y) { }

    /** A placement conflict: an exit that violates the cardinal-grid embedding. */
    public record Conflict(String fromRoom, String direction, String targetRoom, String reason) { }

    private static final Map<String, int[]> CARDINAL_DELTAS = Map.of(
        "north", new int[]{ 0, -1 },
        "south", new int[]{ 0,  1 },
        "east",  new int[]{ 1,  0 },
        "west",  new int[]{-1,  0 }
    );
    private static final Set<String> VERTICAL_DIRECTIONS = Set.of("up", "down");

    private final Map<String, Coord> coords;
    private final Set<String> visited;
    private final Set<String> peekAhead;
    private final List<String> orphans;
    private final List<Conflict> conflicts;
    private final List<List<String>> verticalStacks;

    private MapLayout(Map<String, Coord> coords,
                      Set<String> visited,
                      Set<String> peekAhead,
                      List<String> orphans,
                      List<Conflict> conflicts,
                      List<List<String>> verticalStacks) {
        this.coords = Collections.unmodifiableMap(coords);
        this.visited = Collections.unmodifiableSet(visited);
        this.peekAhead = Collections.unmodifiableSet(peekAhead);
        this.orphans = Collections.unmodifiableList(orphans);
        this.conflicts = Collections.unmodifiableList(conflicts);
        this.verticalStacks = Collections.unmodifiableList(verticalStacks);
    }

    /** Builds a layout for the given campaign and visited room set. */
    public static MapLayout compute(Campaign campaign, Set<String> visitedRooms) {
        Set<String> visited = visitedRooms == null ? Set.of() : Set.copyOf(visitedRooms);
        if (visited.isEmpty()) {
            return empty(visited);
        }

        String anchor = campaign.getStartingLocationId();
        if (anchor == null || !visited.contains(anchor)) {
            // Player hasn't reached the canonical start yet — anchor on
            // any visited room so the layout still draws something.
            anchor = visited.iterator().next();
        }

        Map<String, Coord> coords = new LinkedHashMap<>();
        Map<Coord, String> cellOccupants = new HashMap<>();
        Set<String> peekAhead = new LinkedHashSet<>();
        List<Conflict> conflicts = new ArrayList<>();
        Map<String, String> upTo = new HashMap<>();
        Map<String, String> downTo = new HashMap<>();

        coords.put(anchor, new Coord(0, 0));
        cellOccupants.put(new Coord(0, 0), anchor);

        Deque<String> queue = new ArrayDeque<>();
        Set<String> queued = new HashSet<>();
        queue.add(anchor);
        queued.add(anchor);

        while (!queue.isEmpty()) {
            String roomId = queue.poll();
            Location room = campaign.getLocation(roomId);
            if (room == null) continue;
            Coord here = coords.get(roomId);
            if (here == null) continue;

            for (String dir : room.getExits()) {
                String targetId = room.getExit(dir);
                if (targetId == null) continue;

                int[] delta = CARDINAL_DELTAS.get(dir);
                if (delta != null) {
                    handleCardinal(roomId, dir, targetId, here, delta,
                        visited, coords, cellOccupants, peekAhead,
                        conflicts, queue, queued);
                } else if (VERTICAL_DIRECTIONS.contains(dir)) {
                    if (visited.contains(targetId)) {
                        handleVertical(dir, roomId, targetId, here,
                            coords, upTo, downTo, queue, queued);
                    }
                    // Peek-ahead via vertical: not surfaced in v1.
                }
                // Non-cardinal, non-vertical: ignored for placement.
            }
        }

        // Visited rooms we never managed to place become orphans.
        List<String> orphans = new ArrayList<>();
        for (String v : visited) {
            if (!coords.containsKey(v)) {
                orphans.add(v);
            }
        }

        List<List<String>> stacks = buildStacks(upTo, downTo, visited);

        return new MapLayout(coords, visited, peekAhead, orphans, conflicts, stacks);
    }

    private static MapLayout empty(Set<String> visited) {
        return new MapLayout(
            new LinkedHashMap<>(),
            visited,
            new LinkedHashSet<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }

    private static void handleCardinal(String roomId, String dir, String targetId,
                                       Coord here, int[] delta,
                                       Set<String> visited,
                                       Map<String, Coord> coords,
                                       Map<Coord, String> cellOccupants,
                                       Set<String> peekAhead,
                                       List<Conflict> conflicts,
                                       Deque<String> queue,
                                       Set<String> queued) {
        Coord target = new Coord(here.x() + delta[0], here.y() + delta[1]);
        Coord existing = coords.get(targetId);

        if (existing != null) {
            if (!existing.equals(target)) {
                conflicts.add(new Conflict(roomId, dir, targetId,
                    "target already at " + existing + ", expected " + target));
            }
            return;
        }

        String occupant = cellOccupants.get(target);
        if (occupant != null) {
            conflicts.add(new Conflict(roomId, dir, targetId,
                "cell " + target + " already occupied by " + occupant));
            return;
        }

        coords.put(targetId, target);
        cellOccupants.put(target, targetId);

        if (visited.contains(targetId)) {
            if (!queued.contains(targetId)) {
                queue.add(targetId);
                queued.add(targetId);
            }
        } else {
            // Unvisited but cardinally reachable from a visited room: peek-ahead.
            peekAhead.add(targetId);
        }
    }

    private static void handleVertical(String dir, String roomId, String targetId,
                                       Coord here,
                                       Map<String, Coord> coords,
                                       Map<String, String> upTo,
                                       Map<String, String> downTo,
                                       Deque<String> queue,
                                       Set<String> queued) {
        // Record directed edges, inferring the inverse so asymmetric
        // YAML (only one side declares the link) still produces a stack.
        if ("up".equals(dir)) {
            upTo.putIfAbsent(roomId, targetId);
            downTo.putIfAbsent(targetId, roomId);
        } else { // down
            downTo.putIfAbsent(roomId, targetId);
            upTo.putIfAbsent(targetId, roomId);
        }

        // Stack members share the same grid coord.
        coords.putIfAbsent(targetId, here);

        if (!queued.contains(targetId)) {
            queue.add(targetId);
            queued.add(targetId);
        }
    }

    private static List<List<String>> buildStacks(Map<String, String> upTo,
                                                  Map<String, String> downTo,
                                                  Set<String> visited) {
        // Build undirected adjacency over visited stack members.
        Map<String, Set<String>> adj = new HashMap<>();
        for (var e : upTo.entrySet()) {
            String a = e.getKey();
            String b = e.getValue();
            if (!visited.contains(a) || !visited.contains(b)) continue;
            adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        }
        for (var e : downTo.entrySet()) {
            String a = e.getKey();
            String b = e.getValue();
            if (!visited.contains(a) || !visited.contains(b)) continue;
            adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        }

        List<List<String>> stacks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String root : adj.keySet()) {
            if (seen.contains(root)) continue;
            List<String> component = new ArrayList<>();
            Deque<String> q = new ArrayDeque<>();
            q.add(root);
            seen.add(root);
            while (!q.isEmpty()) {
                String r = q.poll();
                component.add(r);
                for (String n : adj.getOrDefault(r, Set.of())) {
                    if (!seen.contains(n)) {
                        seen.add(n);
                        q.add(n);
                    }
                }
            }
            stacks.add(orderTopToBottom(component, upTo, downTo));
        }
        return stacks;
    }

    private static List<String> orderTopToBottom(List<String> component,
                                                 Map<String, String> upTo,
                                                 Map<String, String> downTo) {
        Set<String> componentSet = new HashSet<>(component);

        // Topmost: a member whose upTo target is null or outside the component.
        String top = null;
        for (String m : component) {
            String above = upTo.get(m);
            if (above == null || !componentSet.contains(above)) {
                top = m;
                break;
            }
        }
        if (top == null) {
            // Pathological cycle; fall back to insertion order.
            return List.copyOf(component);
        }

        List<String> ordered = new ArrayList<>();
        Set<String> walked = new HashSet<>();
        String cur = top;
        while (cur != null && !walked.contains(cur)) {
            ordered.add(cur);
            walked.add(cur);
            String below = downTo.get(cur);
            cur = (below != null && componentSet.contains(below)) ? below : null;
        }

        // Catch any component members not reached by the top->down walk
        // (could happen with a Y-shaped stack); append in insertion order.
        for (String m : component) {
            if (!walked.contains(m)) {
                ordered.add(m);
            }
        }
        return ordered;
    }

    // ==========================================
    // Accessors
    // ==========================================

    public Map<String, Coord> getCoords() {
        return coords;
    }

    public Coord getCoord(String roomId) {
        return coords.get(roomId);
    }

    public Set<String> getVisited() {
        return visited;
    }

    public Set<String> getPeekAhead() {
        return peekAhead;
    }

    public boolean isVisited(String roomId) {
        return visited.contains(roomId);
    }

    public boolean isPeekAhead(String roomId) {
        return peekAhead.contains(roomId);
    }

    public List<String> getOrphans() {
        return orphans;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }

    public List<List<String>> getVerticalStacks() {
        return verticalStacks;
    }
}
