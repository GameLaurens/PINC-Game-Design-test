package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class AsciiLevelTest {
    private record Point(int x, int y) { }
    private int gap(Point p, int x, int y, int width, int height) {
        return Math.max(0, Math.max(x - p.x - AsciiLevel.PLAYER_WIDTH, p.x - x - width))
            + Math.max(0, Math.max(y - p.y - AsciiLevel.PLAYER_HEIGHT, p.y - y - height));
    }
    /** Find a route through actual walls and entities rather than teleporting the player. */
    private void walkTo(AsciiLevel level, Predicate<Point> destination) {
        Point start = new Point(level.playerX(), level.playerY());
        ArrayDeque<Point> queue = new ArrayDeque<>(); queue.add(start);
        Map<Point, Point> previous = new HashMap<>(); previous.put(start, start);
        Point found = null;
        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();
            if (destination.test(p)) { found = p; break; }
            for (Point next : new Point[]{new Point(p.x + 1, p.y), new Point(p.x - 1, p.y), new Point(p.x, p.y + 1), new Point(p.x, p.y - 1)})
                if (!previous.containsKey(next) && level.canPlace(next.x, next.y)) { previous.put(next, p); queue.add(next); }
        }
        assertNotNull(found, "Het doel moet bereikbaar zijn in " + level.width() + "x" + level.height());
        ArrayDeque<Point> path = new ArrayDeque<>();
        for (Point p = found; !p.equals(start); p = previous.get(p)) path.addFirst(p);
        for (Point p : path) assertTrue(level.move(p.x - level.playerX(), p.y - level.playerY(), System.nanoTime()));
    }
    private void collect(AsciiLevel level, AsciiLevel.Weapon weapon) {
        var item = level.pickups().stream().filter(p -> p.weapon() == weapon).findFirst().orElseThrow();
        walkTo(level, p -> gap(p, item.x(), item.y(), weapon.model().width(), weapon.model().height()) == 0);
        level.interact();
        assertEquals(weapon, level.weapon());
    }
    @Test void swordIsPickedUpRemovedFromGroundAndDrawnInThePlayersHand() {
        AsciiLevel level = new AsciiLevel(75, 15);
        assertEquals(AsciiLevel.Weapon.NONE, level.weapon());
        collect(level, AsciiLevel.Weapon.SWORD);
        assertEquals(1, level.pickups().size());
        Canvas frame = level.draw(1_000_000_000L, false, false);
        assertEquals('/', frame.get(level.playerX() + 6, level.playerY()).glyph());
        assertTrue(level.message().contains("zwaard"));
    }
    @Test void chestOpensOnceAndWeaponsCanBeSwitchedWithoutDiscardingThem() {
        AsciiLevel level = new AsciiLevel(75, 15);
        collect(level, AsciiLevel.Weapon.SWORD);
        walkTo(level, p -> gap(p, level.chestX(), level.chestY(), 8, 4) == 0);
        level.interact();
        assertTrue(level.chestOpened()); assertEquals(5, level.gold());
        assertEquals(AsciiLevel.Weapon.DAGGER, level.weapon());
        Canvas open = level.draw(1_000_000_000L, false, false);
        assertEquals('/', open.get(level.chestX() + 1, level.chestY() + 1).glyph());
        level.interact(); assertEquals(5, level.gold());
        level.nextWeapon(); assertEquals(AsciiLevel.Weapon.SWORD, level.weapon());
        collect(level, AsciiLevel.Weapon.AXE);
        assertEquals(3, level.inventory().size());
        Canvas frame = level.draw(1_000_000_000L, false, false);
        assertEquals('#', frame.get(level.playerX() + 4, level.playerY() + 1).glyph());
    }
    @Test void allLootAndMonstersCanBeReachedOnSmallAndLargeMaps() {
        for (int[] size : new int[][]{{60, 14}, {75, 15}, {110, 28}}) {
            AsciiLevel level = new AsciiLevel(size[0], size[1]);
            collect(level, AsciiLevel.Weapon.SWORD);
            collect(level, AsciiLevel.Weapon.AXE);
            long tick = 10_000_000_000L;
            for (var target : level.monsters()) {
                walkTo(level, p -> gap(p, target.x(), target.y(), target.type().model().width(), target.type().model().height()) == 0);
                for (int strike = 0; strike < 4; strike++) { level.attack(tick); tick += 300_000_000L; }
            }
            assertEquals(0, level.monstersRemaining()); assertEquals(6, level.gold());
            walkTo(level, p -> gap(p, level.chestX(), level.chestY(), 8, 4) == 0);
            level.interact();
            assertTrue(level.chestOpened()); assertEquals(11, level.gold());
        }
    }
    @Test void attackingRequiresAWeaponAndHasLimitedRangeAndACooldown() {
        AsciiLevel level = new AsciiLevel(75, 15);
        level.attack(1_000_000_000L);
        assertTrue(level.message().contains("eerst een wapen"));
        collect(level, AsciiLevel.Weapon.SWORD);
        level.attack(2_000_000_000L);
        assertEquals(3, level.monstersRemaining());
        var target = level.monsters().stream().filter(m -> m.type() == AsciiLevel.MonsterType.GOBLIN).findFirst().orElseThrow();
        walkTo(level, p -> gap(p, target.x(), target.y(), target.type().model().width(), target.type().model().height()) == 0);
        level.attack(3_000_000_000L);
        assertEquals(1, level.monsters().get(1).health());
        level.attack(3_000_000_001L);
        assertEquals(1, level.monsters().get(1).health());
        level.attack(3_300_000_000L);
        assertEquals(0, level.monsters().get(1).health());
    }
    @Test void wallsAndSolidEntitiesBlockMovementAndAllMapGlyphsAreAscii() {
        AsciiLevel level = new AsciiLevel(60, 14);
        assertFalse(level.canPlace(0, 3));
        assertFalse(level.canPlace(18, 3));
        assertFalse(level.canPlace(level.chestX(), level.chestY()));
        for (var m : level.monsters()) assertFalse(level.canPlace(m.x(), m.y()));
        Canvas frame = level.draw(1_000_000_000L, true, true);
        for (int y = 0; y < frame.height(); y++) for (int x = 0; x < frame.width(); x++) {
            char glyph = frame.get(x, y).glyph();
            assertTrue(glyph >= 32 && glyph <= 126);
        }
    }
    @Test void mainMenuSelectionPickupAndResetAreWiredToLevelOne() throws Exception {
        Main app = new Main(null, "test", 16); app.handle('1');
        Canvas before = app.demo(79, 23, System.nanoTime(), "");
        assertTrue(row(before, 20).contains("Wapen: geen"));
        app.handle('e');
        Canvas equipped = app.demo(79, 23, System.nanoTime(), "");
        assertTrue(row(equipped, 20).contains("Wapen: zwaard"));
        app.handle('+');
        assertTrue(row(app.demo(79, 23, System.nanoTime(), ""), 20).contains("Wapen: zwaard"));
        app.demo(1, 1, System.nanoTime(), "");
        assertTrue(row(app.demo(79, 23, System.nanoTime(), ""), 20).contains("Wapen: zwaard"));
        app.handle('r');
        assertTrue(row(app.demo(79, 23, System.nanoTime(), ""), 20).contains("Wapen: geen"));
    }
    private String row(Canvas canvas, int row) {
        StringBuilder text = new StringBuilder();
        for (int x = 0; x < canvas.width(); x++) text.append(canvas.get(x, row).glyph());
        return text.toString();
    }
}
