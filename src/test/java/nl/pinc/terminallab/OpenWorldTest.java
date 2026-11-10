package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import nl.pinc.terminallab.OpenWorld.WorldObject;
import nl.pinc.terminallab.OpenWorldCatalog.Biome;
import nl.pinc.terminallab.OpenWorldCatalog.Kind;
import org.junit.jupiter.api.Test;

class OpenWorldTest {
    private static final long NOW = 20_000_000_000L;
    private record Point(int x, int y) { }
    private static final int[][] DIRECTIONS = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    @Test void worldHasSixDistinctBiomesAndReproducibleCompactScenery() {
        OpenWorld world = new OpenWorld(), again = new OpenWorld();
        assertTrue(OpenWorld.WIDTH >= 600 && OpenWorld.HEIGHT >= 250);
        assertTrue(world.objects().size() >= 400);
        assertEquals(world.objects().stream().map(o -> o.kind + ":" + o.x + "," + o.y).toList(),
            again.objects().stream().map(o -> o.kind + ":" + o.x + "," + o.y).toList());
        assertEquals(Kind.values().length, world.objects().stream().map(o -> o.kind).distinct().count());
        assertEquals(6, Arrays.stream(Biome.values()).map(b -> b.ground).distinct().count());
        for (Kind kind : Kind.values()) {
            assertTrue(kind.model.width() <= 12, kind.label);
            assertTrue(kind.model.height() <= 6, kind.label);
        }
        for (Biome biome : Biome.values()) {
            world.travel(biome);
            assertEquals(biome, world.biome());
            assertTrue(world.canPlace(world.playerX(), world.playerY()));
        }
        assertEquals(6, world.visited().size());
        assertFalse(world.canPlace(164, 104), "Water blokkeert beweging");
        assertFalse(world.canPlace(644, 254), "Lava blokkeert beweging");
        assertFalse(world.canPlace(-1, 60));
    }

    @Test void connectedHighwaysReachAllBiomesOnFootWithoutFastTravel() {
        OpenWorld world = new OpenWorld();
        walkStraight(world, 94, 58); walkStraight(world, 94, 52);
        for (int road : new int[]{94, 334, 574}) {
            walkStraight(world, road, 52);
            walkStraight(world, road, 202);
            walkStraight(world, road, 52);
        }
        assertEquals(6, world.visited().size());
    }

    @Test void everyRuinRequiresDemolitionAndItsTreasureCanOnlyBeLootedOnce() {
        OpenWorld world = new OpenWorld();
        long now = NOW;
        int found = 0;
        for (Biome b : Biome.values()) {
            world.travel(b);
            WorldObject gate = world.objects().stream().filter(o -> o.biome == b && o.gateway).findFirst().orElseThrow();
            WorldObject chest = world.objects().stream().filter(o -> o.biome == b && o.kind == Kind.CHEST).findFirst().orElseThrow();
            World.Rect search = new World.Rect(b.campX - 27, b.campY - 18, 115, 60);
            Point entrance = new Point(gate.x + 2, gate.y - 5);
            walkTo(world, p -> p.equals(entrance), search);
            assertSame(gate, world.nearby());
            assertFalse(world.step(0, 1, now), "Ingang is nog geblokkeerd");
            Predicate<Point> nextToChest = p -> p.x == chest.x + 1 && p.y == chest.y - 5;
            assertNull(route(world, nextToChest, search), "Je mag niet via een kier om de ingang heen");
            int hp = gate.hp();
            world.interact(now); world.interact(now + 1);
            assertEquals(hp - 1, gate.hp(), "Sloophamer heeft een eigen cooldown");
            while (!gate.destroyed()) { now += 200_000_000L; world.interact(now); }
            assertTrue(world.canPlace(gate.x + 2, gate.y));
            walkTo(world, nextToChest, search);
            assertSame(chest, world.nearby());
            now += 300_000_000L; world.interact(now);
            assertTrue(chest.opened()); assertEquals(++found, world.treasures());
            world.interact(now + 300_000_000L);
            assertEquals(found, world.treasures());
            now += 1_000_000_000L;
        }
        assertTrue(world.wood() > 0 && world.stone() > 0 && world.crystals() > 0);
    }

    @Test void walkingIsFastSprintIsFasterAndMultiCellStepsCannotTunnelThroughObjects() {
        OpenWorld walk = new OpenWorld(), sprint = new OpenWorld();
        sprint.handle('f', NOW);
        for (int i = 0; i < 6; i++) {
            walk.move(0, -1, NOW + i * 100_000_000L);
            sprint.move(0, -1, NOW + i * 100_000_000L);
        }
        assertEquals(12, 58 - walk.playerY()); assertEquals(24, 58 - sprint.playerY());
        OpenWorld blocked = new OpenWorld();
        walkStraight(blocked, 110, 58);
        WorldObject log = blocked.nearby(); assertEquals(Kind.LOG, log.kind);
        blocked.handle('f', NOW);
        assertFalse(blocked.move(1, 0, NOW)); assertEquals(110, blocked.playerX());
        blocked.interact(NOW); blocked.interact(NOW + 200_000_000L);
        assertTrue(log.destroyed()); assertEquals(2, blocked.wood());
        assertTrue(blocked.move(1, 0, NOW + 300_000_000L)); assertEquals(114, blocked.playerX());
        assertTrue(blocked.canPlace(blocked.playerX(), blocked.playerY()));
        OpenWorld diagonal = new OpenWorld();
        assertTrue(diagonal.move(-1, -1, NOW));
        assertFalse(diagonal.move(-1, -1, NOW + 60_000_000L));
        assertTrue(diagonal.move(-1, -1, NOW + 90_000_000L));
    }

    @Test void resizingPreservesDestroyedObjectsInventoryAndWorldAndMapSurvivesTinyViewports() {
        OpenWorld world = new OpenWorld(); walkStraight(world, 110, 58);
        WorldObject log = world.nearby();
        world.interact(NOW); world.interact(NOW + 200_000_000L);
        for (int[] size : new int[][]{{1,1}, {20,8}, {64,22}, {140,40}, {320,120}}) {
            var camera = world.camera(size[0], size[1]);
            assertTrue(camera.x() >= 0 && camera.x() + camera.width() <= OpenWorld.WIDTH);
            assertTrue(camera.y() >= 0 && camera.y() + camera.height() <= OpenWorld.HEIGHT);
            assertDoesNotThrow(() -> world.frame(size[0], size[1], NOW, true, true));
            world.handle('\t', NOW);
            assertDoesNotThrow(() -> world.frame(size[0], size[1], NOW, true, true));
            world.handle('\t', NOW);
            assertEquals(110, world.playerX()); assertEquals(58, world.playerY());
            assertTrue(log.destroyed()); assertEquals(2, world.wood());
        }
    }

    @Test void menuNativeMovementChatMapAndResetAllRouteToTheNewDemo() throws Exception {
        GameMenu menu = new GameMenu(); menu.home(); menu.handle('\r');
        while (menu.game() != 13) menu.handle(TerminalKeys.RIGHT);
        menu.handle('s'); menu.handle('s'); menu.handle('\r'); menu.handle('\r');
        assertEquals(13, menu.takeStartRequest());
        Main app = new Main(null, "test", 16); GameControls keys = new GameControls();
        app.useControls(keys); app.openDemo(13); app.frame(140, 40, NOW, "");
        OpenWorld world = app.openWorld();
        keys.keyEvent('D', true, 0); app.updateControls(NOW);
        assertEquals(92, world.playerX());
        app.handle('f'); app.updateControls(NOW + 100_000_000L);
        assertTrue(world.sprinting()); assertEquals(96, world.playerX());
        app.handle('t'); app.handle('f'); app.handle('e'); app.handle('\t'); app.handle('6');
        app.updateControls(NOW + 500_000_000L); app.handle(27);
        app.updateControls(NOW + 600_000_000L);
        assertEquals(96, world.playerX()); assertTrue(world.sprinting()); assertFalse(world.mapOpen());
        app.handle('\t'); assertTrue(world.mapOpen());
        app.updateControls(NOW + 700_000_000L);
        app.handle('6'); assertEquals(Biome.VOLCANO, world.biome());
        int before = world.playerX(); app.updateControls(NOW + 800_000_000L);
        assertEquals(before, world.playerX(), "Na kaart/chat eerst de looptoets loslaten");
        keys.keyEvent('D', false, 0); keys.keyEvent('D', true, 0); app.updateControls(NOW + 900_000_000L);
        assertEquals(before + 4, world.playerX());
        app.handle('+'); app.handle('-');
        assertSame(world, app.openWorld());
        assertTrue(rows(app.frame(140, 40, NOW, "")).contains("VERKENNING"));
        app.handle('\t'); app.handle('r');
        assertNotSame(world, app.openWorld()); assertEquals(Biome.MEADOW, app.openWorld().biome());
        app.handle(27); assertTrue(rows(app.frame(100, 30, NOW, "")).contains("LOBBY"));
    }

    private static void walkStraight(OpenWorld world, int x, int y) {
        while (world.playerX() != x || world.playerY() != y) {
            int dx = Integer.signum(x - world.playerX()), dy = dx == 0 ? Integer.signum(y - world.playerY()) : 0;
            assertTrue(world.step(dx, dy, NOW), "Blokkade bij " + world.playerX() + "," + world.playerY());
        }
    }
    private static List<Point> route(OpenWorld world, Predicate<Point> destination, World.Rect bounds) {
        Point start = new Point(world.playerX(), world.playerY());
        ArrayDeque<Point> queue = new ArrayDeque<>(); queue.add(start);
        Map<Point, Point> previous = new HashMap<>(); previous.put(start, start);
        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();
            if (destination.test(p)) {
                ArrayDeque<Point> route = new ArrayDeque<>();
                for (Point at = p; !at.equals(start); at = previous.get(at)) route.addFirst(at);
                return List.copyOf(route);
            }
            for (int[] d : DIRECTIONS) {
                Point n = new Point(p.x + d[0], p.y + d[1]);
                if (n.x < bounds.x() || n.y < bounds.y() || n.x >= bounds.x() + bounds.width() || n.y >= bounds.y() + bounds.height()) continue;
                if (!previous.containsKey(n) && world.canPlace(n.x, n.y)) { previous.put(n, p); queue.add(n); }
            }
        }
        return null;
    }
    private static void walkTo(OpenWorld world, Predicate<Point> destination, World.Rect bounds) {
        List<Point> route = route(world, destination, bounds);
        assertNotNull(route, "Looproute naar doel vereist");
        for (Point p : route) assertTrue(world.step(p.x - world.playerX(), p.y - world.playerY(), NOW));
    }
    private static String rows(Canvas c) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < c.height(); row++) {
            for (int col = 0; col < c.width(); col++) result.append(c.get(col, row).glyph());
            result.append('\n');
        }
        return result.toString();
    }
}
