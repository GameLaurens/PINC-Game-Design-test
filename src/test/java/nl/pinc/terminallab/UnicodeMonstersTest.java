package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.jline.utils.WCWidth;
import org.junit.jupiter.api.Test;

class UnicodeMonstersTest {
    @Test void everyMonsterHasDistinctUnicodeArtRenderedInItsOwnColor() {
        Showroom room = Showroom.unicodeMonsters();
        Canvas full = room.draw(new Showroom.Camera(0, 0, Showroom.WIDTH, room.height()), 1, false, false);
        String gallery = rows(full);
        var silhouettes = new HashSet<String>();
        for (var e : room.exhibits()) {
            var monster = UnicodeMonsters.CREATURES.get(e.index());
            assertTrue(gallery.contains(monster.label()));
            assertTrue(gallery.contains(monster.technique()));
            UnicodeModel model = monster.model();
            assertTrue(model.width() <= 12, monster.label());
            assertTrue(model.height() <= 6, monster.label());
            assertTrue(model.width() * model.height() <= 3 * 5 * 5,
                "Maximaal driemaal het zichtbare spelerskader van 5x5: " + monster.label());
            Canvas expected = new Canvas(model.width(), model.height());
            model.draw(expected, 0, 0, monster.color());
            StringBuilder art = new StringBuilder();
            boolean unicode = false;
            var colors = new HashSet<Integer>();
            for (int y = 0; y < model.height(); y++) for (int x = 0; x < model.width(); x++) {
                Cell cell = expected.get(x, y);
                art.append(cell.glyph());
                assertEquals(1, WCWidth.wcwidth(cell.glyph()));
                unicode |= cell.glyph() > 127;
                if (cell.glyph() != ' ') {
                    Cell actual = full.get(e.x() + x, e.y() + y);
                    assertEquals(cell.glyph(), actual.glyph());
                    assertEquals(cell.foreground(), actual.foreground());
                    colors.add(actual.foreground());
                }
            }
            assertTrue(unicode, monster.label());
            assertTrue(colors.size() >= 3, "Schaduw en accenten: " + monster.label());
            assertTrue(silhouettes.add(art.toString()), monster.label());
        }
        assertEquals(15, silhouettes.size());
    }

    @Test void modelsRejectGlyphsThatWouldBreakTheCellGrid() {
        assertThrows(IllegalArgumentException.class, () -> new UnicodeModel());
        for (String bad : new String[]{"\n", "\t", "\033[31m", "界", "😀", "e\u0301", "\u200b"})
            assertThrows(IllegalArgumentException.class, () -> new UnicodeModel(bad), bad);
        assertThrows(IllegalArgumentException.class, () -> new AsciiModel("█"));
    }

    @Test void clippingAndTransparentSpacesPreserveTheUnderlyingCells() {
        Canvas canvas = new Canvas(3, 2);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 3; x++)
            canvas.put(x, y, new Cell('.', 0xffffff, 0x123456));
        new UnicodeModel("█ ▀", " ▄ ").draw(canvas, -1, 0, 0x00ff00);
        assertEquals(new Cell('.', 0xffffff, 0x123456), canvas.get(0, 0));
        assertEquals(new Cell('▀', 0x00ff00, 0x123456), canvas.get(1, 0));
        assertEquals(new Cell('▄', 0x00ff00, 0x123456), canvas.get(0, 1));
        assertDoesNotThrow(() -> UnicodeMonsters.PLAYER.draw(canvas, 100, 100, 0));
    }

    @Test void ansiOutputCarriesUtf8ArtAndIdenticalFramesEmitNothing() {
        var room = Showroom.unicodeMonsters();
        Canvas canvas = room.draw(new Showroom.Camera(0, 0, Showroom.WIDTH, room.height()), 1, false, false);
        var bytes = new ByteArrayOutputStream();
        var display = new AnsiDisplay(new PrintWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8)));
        display.render(canvas);
        String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Maanmot"));
        assertTrue(output.contains("Nachtvorst"));
        assertTrue(output.contains("█"));
        assertFalse(output.contains("�"));
        assertEquals(bytes.size(), display.totalBytes());
        int size = bytes.size();
        display.render(canvas);
        assertEquals(size, bytes.size());
        assertEquals(0, display.changedCells());
    }

    @Test void newDemoPreservesMovementThroughResizeAndSizeKeysAndResets() throws Exception {
        Main app = new Main(null, "test", 16);
        app.openDemo(12);
        assertTrue(rows(app.demo(100, 30, 1, "")).contains("15 Unicode-modellen"));
        app.handle('d');
        assertTrue(rows(app.demo(100, 30, 1, "")).contains("Jij 4,2"));
        app.handle('+'); app.handle('-');
        app.frame(320, 120, 1, "");
        app.demo(1, 1, 1, "");
        assertTrue(rows(app.demo(100, 30, 1, "")).contains("Jij 4,2"));
        app.handle('r');
        assertTrue(rows(app.demo(100, 30, 1, "")).contains("Jij 3,2"));
        app.handle('m');
        assertTrue(rows(app.frame(100, 30, 1, "")).contains("GAME AANMAKEN"));
    }

    @Test void heldDiagonalMovementAndChatUseTheExistingGameControls() throws Exception {
        Main app = new Main(null, "test", 16);
        GameControls keys = new GameControls(); app.useControls(keys); app.openDemo(12);
        app.frame(140, 40, 1, "");
        keys.keyEvent('W', true, 0); keys.keyEvent('D', true, 0);
        app.updateControls(1);
        assertTrue(rows(app.demo(100, 30, 1, "")).contains("Jij 4,1"));
        app.handle('t'); app.handle('d'); app.updateControls(1_000_000_000L);
        app.handle(27); app.updateControls(2_000_000_000L);
        assertTrue(rows(app.demo(100, 30, 1, "")).contains("Jij 4,1"));
    }

    private static String rows(Canvas canvas) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < canvas.height(); y++) {
            for (int x = 0; x < canvas.width(); x++) text.append(canvas.get(x, y).glyph());
            text.append('\n');
        }
        return text.toString();
    }
}
