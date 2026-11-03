package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import nl.pinc.terminallab.Showroom.Direction;
import nl.pinc.terminallab.ShowroomCatalog.Monster;
import nl.pinc.terminallab.ShowroomCatalog.Weapon;
import org.junit.jupiter.api.Test;

class ShowroomTest {
    private record Point(int x, int y) { }
    private void walkTo(Showroom room, Predicate<Point> destination) {
        Point start = new Point(room.playerX(), room.playerY());
        ArrayDeque<Point> queue = new ArrayDeque<>(); queue.add(start);
        Map<Point, Point> previous = new HashMap<>(); previous.put(start, start);
        Point found = null;
        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();
            if (destination.test(p)) { found = p; break; }
            for (Direction direction : Direction.values()) {
                Point next = new Point(p.x + direction.dx, p.y + direction.dy);
                if (!previous.containsKey(next) && room.canPlace(next.x, next.y)) {
                    previous.put(next, p); queue.add(next);
                }
            }
        }
        assertNotNull(found, "Doel moet via echte looproute bereikbaar zijn");
        ArrayDeque<Point> route = new ArrayDeque<>();
        for (Point p = found; !p.equals(start); p = previous.get(p)) route.addFirst(p);
        for (Point p : route) {
            Direction direction = java.util.Arrays.stream(Direction.values())
                .filter(d -> room.playerX() + d.dx == p.x && room.playerY() + d.dy == p.y).findFirst().orElseThrow();
            assertTrue(room.move(direction, 1));
        }
    }
    private int gap(Point p, Showroom.Exhibit e, AsciiModel model) {
        return Math.max(0, Math.max(e.x() - p.x - Showroom.PLAYER_WIDTH, p.x - e.x() - model.width()))
            + Math.max(0, Math.max(e.y() - p.y - Showroom.PLAYER_HEIGHT, p.y - e.y() - model.height()));
    }
    private void collect(Showroom room, Weapon weapon) {
        var e = room.exhibits().get(weapon.ordinal());
        walkTo(room, p -> gap(p, e, weapon.model) == 0);
        room.interact();
        assertEquals(weapon, room.weapon());
    }
    private boolean hitsFrom(Weapon weapon, Direction direction, Point p) {
        for (int y = Showroom.TARGET_Y; y < Showroom.TARGET_Y + Monster.GOLEM.model.height(); y++)
            for (int x = Showroom.TARGET_X; x < Showroom.TARGET_X + Monster.GOLEM.model.width(); x++)
                if (Showroom.inAttack(weapon, direction, p.x, p.y, x, y)) return true;
        return false;
    }
    @Test void allFifteenMonstersAreDistinctAsciiModelsAndReachable() {
        Showroom room = new Showroom(false);
        assertEquals(15, Monster.values().length);
        assertEquals(15, room.exhibits().size());
        Canvas full = room.draw(new Showroom.Camera(0, 0, Showroom.WIDTH, room.height()), 1, false, false);
        java.util.Set<String> models = new java.util.HashSet<>();
        for (var e : room.exhibits()) {
            Monster monster = Monster.values()[e.index()];
            walkTo(room, p -> gap(p, e, monster.model) == 0);
            assertFalse(room.canPlace(e.x(), e.y()), "Monsters zijn solide");
            StringBuilder model = new StringBuilder();
            for (int y = 0; y < monster.model.height(); y++) for (int x = 0; x < monster.model.width(); x++)
                model.append(full.get(e.x() + x, e.y() + y).glyph());
            assertTrue(models.add(model.toString()), "Ieder monster heeft een eigen silhouet");
        }
        assertAscii(full);
    }
    @Test void allWeaponsCanBeCollectedEquippedAndUsedRepeatedlyAgainstImmortalGolem() {
        Showroom room = new Showroom(true);
        long now = 10_000_000_000L;
        long expected = 0;
        for (Weapon weapon : Weapon.values()) {
            collect(room, weapon);
            Canvas equipped = room.draw(new Showroom.Camera(0, 0, Showroom.WIDTH, room.height()), now, false, false);
            Canvas model = new Canvas(weapon.model.width(), weapon.model.height());
            weapon.model.draw(model, 0, 0, 0x87d7ed);
            for (int y = 0; y < model.height(); y++) for (int x = 0; x < model.width(); x++)
                if (model.get(x, y).glyph() != ' ')
                    assertEquals(model.get(x, y).glyph(), equipped.get(room.playerX() + 4 + x, room.playerY() + y).glyph());
            walkTo(room, p -> hitsFrom(weapon, Direction.RIGHT, p));
            for (int i = 0; i < 3; i++) {
                room.attack(Direction.RIGHT, now);
                expected += weapon.damage;
                assertEquals(expected, room.totalDamage());
                assertEquals(weapon.damage, room.lastDamage());
                room.attack(Direction.RIGHT, now + 1);
                assertEquals(expected, room.totalDamage(), "Cooldown voorkomt dubbele schade");
                now += 2_000_000_000L;
            }
        }
        assertEquals(15, room.inventory().size());
        assertEquals(45, room.hits());
        room.nextWeapon(); assertEquals(Weapon.DAGGER, room.weapon());
        for (int i = 0; i < 15; i++) room.nextWeapon();
        assertEquals(Weapon.DAGGER, room.weapon());
        Canvas full = room.draw(new Showroom.Camera(0, 0, Showroom.WIDTH, room.height()), now, false, false);
        assertTrue(row(full, 8).contains("[opgepakt]"));
        assertAscii(full);
    }
    @Test void directionalAttacksMissBehindPlayerAndAtLongRangeAndFlashOnImpact() {
        Showroom room = new Showroom(true);
        room.attack(Direction.RIGHT, 1);
        assertEquals(0, room.hits()); assertTrue(room.message().contains("eerst"));
        collect(room, Weapon.LONGSWORD);
        room.attack(Direction.RIGHT, 1_000_000_000L);
        assertEquals(0, room.hits());
        walkTo(room, p -> p.x < Showroom.TARGET_X && hitsFrom(Weapon.LONGSWORD, Direction.RIGHT, p));
        room.attack(Direction.LEFT, 2_000_000_000L);
        assertEquals(0, room.hits());
        room.attack(Direction.RIGHT, 3_000_000_000L);
        assertEquals(1, room.hits());
        var camera = new Showroom.Camera(0, 0, Showroom.WIDTH, room.height());
        Canvas hit = room.draw(camera, 3_100_000_000L, false, false);
        Canvas recovered = room.draw(camera, 3_400_000_000L, false, false);
        assertEquals(Showroom.HIT_COLOR, hit.get(Showroom.TARGET_X + 2, Showroom.TARGET_Y).foreground());
        assertEquals(Monster.GOLEM.color, recovered.get(Showroom.TARGET_X + 2, Showroom.TARGET_Y).foreground());
        assertFalse(room.canPlace(Showroom.TARGET_X, Showroom.TARGET_Y));
    }
    @Test void actualAttackMasksHaveDistinctRangesWidthsAndRadialBehavior() {
        for (Weapon weapon : Weapon.values()) for (Direction direction : Direction.values()) {
            int px = 40, py = 20;
            int edgeX = direction == Direction.RIGHT ? px + Showroom.PLAYER_WIDTH - 1 : direction == Direction.LEFT ? px : px + Showroom.PLAYER_WIDTH / 2;
            int edgeY = direction == Direction.DOWN ? py + Showroom.PLAYER_HEIGHT - 1 : direction == Direction.UP ? py : py + Showroom.PLAYER_HEIGHT / 2;
            assertTrue(Showroom.inAttack(weapon, direction, px, py, edgeX + direction.dx * weapon.range, edgeY + direction.dy * weapon.range));
            assertFalse(Showroom.inAttack(weapon, direction, px, py, edgeX + direction.dx * (weapon.range + 1), edgeY + direction.dy * (weapon.range + 1)));
        }
        int edge = 40 + Showroom.PLAYER_WIDTH;
        assertFalse(Showroom.inAttack(Weapon.RAPIER, Direction.RIGHT, 40, 20, edge, 23));
        assertTrue(Showroom.inAttack(Weapon.KATANA, Direction.RIGHT, 40, 20, edge, 23));
        assertFalse(Showroom.inAttack(Weapon.DAGGER, Direction.RIGHT, 40, 20, 39, 22));
        assertTrue(Showroom.inAttack(Weapon.HAMMER, Direction.RIGHT, 40, 20, 39, 22));
        assertTrue(Weapon.DAGGER.cooldownMs < Weapon.GREATSWORD.cooldownMs);
    }
    @Test void resizingAndSizeKeysPreserveInventoryAndResetClearsIt() throws Exception {
        Main app = new Main(new Sprite(1, 1, new int[]{0xffff00ff}), "test", 32);
        app.handle('5');
        app.demo(79, 23, 1, ""); app.handle('e');
        assertTrue(row(app.demo(79, 23, 1, ""), 18).contains("Wapen: Dolk"));
        app.handle('+'); app.handle('-');
        app.demo(1, 1, 1, "");
        Canvas frame = app.demo(79, 23, 1, "");
        assertTrue(row(frame, 18).contains("Wapen: Dolk"));
        assertAscii(frame);
        app.handle('r');
        assertTrue(row(app.demo(79, 23, 1, ""), 18).contains("Wapen: geen"));
        app.handle('4');
        assertTrue(row(app.demo(79, 23, 1, ""), 0).contains("MONSTERSHOWROOM"));
    }
    @Test void cameraFollowsPlayerAcrossEntireGalleryAndStaysInsideWorld() {
        Showroom room = new Showroom(true);
        walkTo(room, p -> p.x == Showroom.WIDTH - Showroom.PLAYER_WIDTH - 1 && p.y == room.height() - Showroom.PLAYER_HEIGHT - 1);
        for (int[] size : new int[][]{{60, 12}, {100, 28}, {236, 90}}) {
            var camera = room.camera(size[0], size[1]);
            assertTrue(camera.x() >= 0 && camera.y() >= 0);
            assertTrue(camera.x() + camera.width() <= Showroom.WIDTH);
            assertTrue(camera.y() + camera.height() <= room.height());
            assertTrue(room.playerX() >= camera.x() && room.playerX() + Showroom.PLAYER_WIDTH <= camera.x() + camera.width());
            assertDoesNotThrow(() -> room.draw(camera, 1, true, true));
        }
    }
    private void assertAscii(Canvas canvas) {
        for (int y = 0; y < canvas.height(); y++) for (int x = 0; x < canvas.width(); x++)
            assertTrue(canvas.get(x, y).glyph() >= 32 && canvas.get(x, y).glyph() <= 126);
    }
    private String row(Canvas canvas, int y) {
        StringBuilder text = new StringBuilder();
        for (int x = 0; x < canvas.width(); x++) text.append(canvas.get(x, y).glyph());
        return text.toString();
    }
}
