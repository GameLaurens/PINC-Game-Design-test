package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import nl.pinc.terminallab.Showroom.Direction;
import org.junit.jupiter.api.Test;

class SmallAsciiMonstersTest {
    private static final long NOW = 20_000_000_000L;
    private record Point(int x, int y) { }

    @Test void fifteenDistinctAsciiModelsStayBetweenOneAndFiveThirdsOfTheVisiblePlayer() {
        assertEquals(3, SmallAsciiMonsters.PLAYER_WIDTH);
        assertEquals(3, SmallAsciiMonsters.PLAYER_HEIGHT);
        Showroom room = Showroom.smallAsciiMonsters();
        Canvas scene = room.draw(new Showroom.Camera(0, 0, room.width(), room.height()), NOW, false, false);
        var silhouettes = new HashSet<String>();
        var sizes = new HashSet<String>();
        for (var exhibit : room.exhibits()) {
            var monster = SmallAsciiMonsters.CREATURES.get(exhibit.index());
            var model = monster.model();
            assertTrue(model.width() * 3 >= SmallAsciiMonsters.PLAYER_WIDTH);
            assertTrue(model.width() * 3 <= SmallAsciiMonsters.PLAYER_WIDTH * 5);
            assertTrue(model.height() * 3 >= SmallAsciiMonsters.PLAYER_HEIGHT);
            assertTrue(model.height() * 3 <= SmallAsciiMonsters.PLAYER_HEIGHT * 5);
            Canvas expected = new Canvas(model.width(), model.height());
            model.draw(expected, 0, 0, monster.color());
            assertTrue(silhouettes.add(rows(expected)), "Eigen silhouet voor " + monster.label());
            sizes.add(monster.dimensions());
            for (int y = 0; y < model.height(); y++) for (int x = 0; x < model.width(); x++) {
                Cell cell = expected.get(x, y);
                if (cell.glyph() != ' ') {
                    assertEquals(cell.glyph(), scene.get(exhibit.x() + x, exhibit.y() + y).glyph());
                    assertEquals(monster.color(), scene.get(exhibit.x() + x, exhibit.y() + y).foreground());
                }
            }
        }
        assertEquals(15, silhouettes.size());
        assertTrue(sizes.contains("1x1")); assertTrue(sizes.contains("3x3")); assertTrue(sizes.contains("5x5"));
        for (int y = 0; y < scene.height(); y++) for (int x = 0; x < scene.width(); x++)
            assertTrue(scene.get(x, y).glyph() >= 32 && scene.get(x, y).glyph() <= 126,
                "Ook de galerij en schaalinformatie gebruiken alleen ASCII");
    }

    @Test void playerUsesHisVisibleThreeByThreeBoundsAndCanReachEveryMonster() {
        Showroom room = Showroom.smallAsciiMonsters();
        assertEquals(3, room.playerWidth()); assertEquals(3, room.playerHeight());
        for (var e : room.exhibits()) {
            assertTrue(room.canPlace(e.x() - 3, e.y()), "Speler past precies naast het monster");
            assertFalse(room.canPlace(e.x() - 2, e.y()), "Zichtbaar lijf botst met het monster");
            Point target = new Point(e.x() - 3, e.y());
            Point start = new Point(room.playerX(), room.playerY());
            var queue = new ArrayDeque<Point>(); queue.add(start);
            Map<Point, Point> previous = new HashMap<>(); previous.put(start, start);
            while (!queue.isEmpty() && !previous.containsKey(target)) {
                Point p = queue.removeFirst();
                for (Direction d : Direction.values()) {
                    Point next = new Point(p.x + d.dx, p.y + d.dy);
                    if (!previous.containsKey(next) && room.canPlace(next.x, next.y)) { previous.put(next, p); queue.add(next); }
                }
            }
            assertTrue(previous.containsKey(target), "Looproute naar ieder model");
            var path = new ArrayDeque<Point>();
            for (Point p = target; !p.equals(start); p = previous.get(p)) path.addFirst(p);
            for (Point p : path) assertTrue(room.move(p.x - room.playerX(), p.y - room.playerY(), NOW));
            var monster = SmallAsciiMonsters.CREATURES.get(e.index());
            assertTrue(room.message().contains(monster.label()));
            assertTrue(room.message().contains(monster.scale()));
        }
    }

    @Test void menuNativeInputChatResizingSizeKeysAndResetWorkForTheNewGallery() throws Exception {
        GameMenu menu = new GameMenu(); menu.home(); menu.handle('\r');
        while (menu.game() != 14) menu.handle(TerminalKeys.RIGHT);
        menu.handle('s'); menu.handle('s'); menu.handle('\r'); menu.handle('\r');
        assertEquals(14, menu.takeStartRequest());
        Main app = new Main(null, "test", 16); GameControls keys = new GameControls();
        app.useControls(keys); app.openDemo(14); app.frame(140, 40, NOW, "");
        keys.keyEvent('W', true, 0); keys.keyEvent('D', true, 0); app.updateControls(NOW);
        assertTrue(rows(app.demo(100, 30, NOW, "")).contains("Jij 4,1"));
        app.handle('t'); app.handle('d'); app.updateControls(NOW + 500_000_000L); app.handle(27);
        app.updateControls(NOW + 1_000_000_000L);
        app.handle('+'); app.handle('-');
        app.frame(320, 120, NOW, ""); app.demo(1, 1, NOW, "");
        assertTrue(rows(app.demo(100, 30, NOW, "")).contains("Jij 4,1"));
        keys.keyEvent('W', false, 0); keys.keyEvent('D', false, 0); keys.keyEvent('D', true, 0);
        app.updateControls(NOW + 2_000_000_000L);
        assertTrue(rows(app.demo(100, 30, NOW, "")).contains("Jij 5,1"));
        app.handle('r');
        String reset = rows(app.demo(100, 30, NOW, ""));
        assertTrue(reset.contains("14 SMALL ASCII GALLERY")); assertTrue(reset.contains("Jij 3,2"));
    }

    private static String rows(Canvas c) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < c.height(); y++) {
            for (int x = 0; x < c.width(); x++) text.append(c.get(x, y).glyph());
            text.append('\n');
        }
        return text.toString();
    }
}
