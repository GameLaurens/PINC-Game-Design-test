package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class WeaponRangeLabTest {
    private static final long NOW = 20_000_000_000L;
    private static String rows(Canvas c) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < c.height(); y++) {
            for (int x = 0; x < c.width(); x++) text.append(c.get(x, y).glyph());
            text.append('\n');
        }
        return text.toString();
    }
    private void cursor(WeaponRangeLab lab, int x, int y) {
        while (lab.cursorX() != x) lab.handle(lab.cursorX() < x ? TerminalKeys.RIGHT : TerminalKeys.LEFT, NOW);
        while (lab.cursorY() != y) lab.handle(lab.cursorY() < y ? TerminalKeys.DOWN : TerminalKeys.UP, NOW);
    }
    @Test void enterTogglesIndependentDamageCellsWithoutMovingTheCharacter() {
        WeaponRangeLab lab = new WeaponRangeLab();
        assertTrue(lab.damageCells().isEmpty());
        lab.handle('\r', NOW);
        assertTrue(lab.damages(2, 0));
        lab.handle(TerminalKeys.UP, NOW); lab.handle('\n', NOW);
        assertTrue(lab.damages(2, -1));
        cursor(lab, -3, 2); lab.handle('\r', NOW);
        assertEquals(3, lab.damageCells().size());
        lab.handle('\r', NOW);
        assertFalse(lab.damages(-3, 2));
        assertTrue(lab.damages(2, 0)); assertTrue(lab.damages(2, -1));
    }
    @Test void allSurroundingCellsCanBeSelectedButTheThreeByThreeBodyIsProtected() {
        WeaponRangeLab lab = new WeaponRangeLab();
        int r = WeaponRangeLab.RADIUS, all = WeaponRangeLab.SIZE * WeaponRangeLab.SIZE - 9;
        assertEquals(15, r);
        for (int y = -r; y <= r; y++) for (int x = -r; x <= r; x++) {
            cursor(lab, x, y); lab.handle('\r', NOW);
            assertEquals(Math.abs(x) > 1 || Math.abs(y) > 1, lab.damages(x, y));
        }
        assertEquals(all, lab.damageCells().size());
        for (int key : new int[]{TerminalKeys.LEFT, TerminalKeys.RIGHT, TerminalKeys.UP, TerminalKeys.DOWN}) {
            for (int i = 0; i < 60; i++) lab.handle(key, NOW);
            assertTrue(Math.abs(lab.cursorX()) <= r && Math.abs(lab.cursorY()) <= r);
        }
        var copy = new HashSet<>(lab.damageCells()); copy.clear();
        assertEquals(all, lab.damageCells().size());
    }
    @Test void clearAndResetDoNotConfuseTheCursorWithDamageSelection() {
        WeaponRangeLab lab = new WeaponRangeLab();
        lab.handle('\r', NOW); cursor(lab, 4, -3);
        lab.handle('c', NOW);
        assertTrue(lab.damageCells().isEmpty()); assertEquals(4, lab.cursorX()); assertEquals(-3, lab.cursorY());
        lab.handle('\r', NOW); lab.handle('r', NOW);
        assertTrue(lab.damageCells().isEmpty()); assertEquals(2, lab.cursorX()); assertEquals(0, lab.cursorY());
    }
    @Test void previewHighlightsOnlySelectedCellsAndExpiresWithoutChangingTheMask() {
        WeaponRangeLab lab = new WeaponRangeLab(); lab.handle('\r', NOW);
        var grid = lab.grid(100, 39);
        Canvas before = lab.frame(100, 39, NOW);
        lab.handle(' ', NOW);
        Canvas attack = lab.frame(100, 39, NOW + 1);
        assertEquals(WeaponRangeLab.DAMAGE, before.get(grid.x(2), grid.y(0)).background());
        assertEquals(WeaponRangeLab.FLASH, attack.get(grid.x(2), grid.y(0)).background());
        assertEquals(before.get(grid.x(3), grid.y(0)), attack.get(grid.x(3), grid.y(0)));
        assertEquals(WeaponRangeLab.DAMAGE, lab.frame(100, 39, NOW + 400_000_000L).get(grid.x(2), grid.y(0)).background());
        assertEquals(1, lab.damageCells().size());
    }
    @Test void compactAndRoomyGridsShowTheCursorAndPreserveTheSameOneThirdScale() {
        int[][] sizes = {{52, 23}, {64, 19}, {100, 39}, {220, 80}};
        WeaponRangeLab lab = new WeaponRangeLab();
        for (int[] size : sizes) {
            var grid = lab.grid(size[0], size[1]); Canvas c = lab.frame(size[0], size[1], NOW);
            assertTrue(grid.visible(0, 0), "Het raster begint rond de speler");
            assertEquals('O', c.get(grid.x(0) + (grid.roomy() ? 1 : 0), grid.y(-1)).glyph());
            assertEquals(3 * grid.cellWidth(), grid.x(2) - grid.x(-1));
            assertEquals(3 * grid.cellHeight(), grid.y(2) - grid.y(-1));
        }
        cursor(lab, -15, -15); lab.handle('\r', NOW);
        for (int[] size : sizes) {
            var grid = lab.grid(size[0], size[1]); Canvas c = lab.frame(size[0], size[1], NOW);
            assertTrue(grid.visible(-15, -15), "Het zicht volgt de cursor");
            assertTrue(c.contains(grid.x(grid.lastX()), grid.y(grid.lastY())));
            assertTrue(grid.y(grid.lastY()) < size[1] - 3, "Het raster mag de bediening niet overlappen");
            assertEquals(WeaponRangeLab.DAMAGE, c.get(grid.x(-15), grid.y(-15)).background());
            assertEquals(1, lab.damageCells().size());
        }
        var full = lab.grid(220, 80);
        assertTrue(full.complete() && full.roomy() && full.visible(15, 15));
        cursor(lab, 15, 15);
        var small = lab.grid(52, 23);
        assertTrue(small.visible(15, 15)); assertFalse(small.complete());
        assertTrue(rows(lab.frame(52, 23, NOW)).contains("Zicht"));
        assertTrue(rows(lab.frame(1, 1, NOW)).length() > 0);
        assertTrue(rows(lab.frame(30, 12, NOW)).contains("Vergroot"));
    }
    @Test void menuCanCreateTheNewModeAndMainRoutesArrowsEnterAndResetToIt() throws Exception {
        GameMenu menu = new GameMenu(); menu.home(); menu.handle('\r');
        while (menu.game() != 11) menu.handle(TerminalKeys.RIGHT);
        assertTrue(rows(menu.frame(100, 30, NOW)).contains("Weapon Range Editor"));
        menu.handle(TerminalKeys.DOWN); menu.handle(TerminalKeys.DOWN); menu.handle('\r'); menu.handle('\r');
        assertEquals(11, menu.takeStartRequest());
        Main app = new Main(null, "test", 16); app.openDemo(11);
        app.handle('\r'); app.handle(TerminalKeys.UP); app.handle('\r');
        String frame = rows(app.frame(140, 40, NOW, ""));
        assertTrue(frame.contains("Cursor (2,-1) | Schadevakjes: 2"));
        assertTrue(frame.contains("WAPENPATROON"));
        assertTrue(rows(app.frame(64, 22, NOW, "")).contains("Schadevakjes: 2"));
        app.handle('+'); app.handle('-'); app.handle('w'); app.handle('d');
        assertTrue(rows(app.demo(90, 32, NOW, "")).contains("Cursor (2,-1) | Schadevakjes: 2"));
        app.handle('r');
        assertTrue(rows(app.demo(90, 32, NOW, "")).contains("Cursor (2,0) | Schadevakjes: 0"));
        app.handle(27);
        assertTrue(rows(app.frame(100, 30, NOW, "")).contains("LOBBY"));
    }
    @Test void windowsHeldInputCannotDuplicateArrowPressesOrMoveTheAvatarAndChatConsumesEnter() throws Exception {
        Main app = new Main(null, "test", 16); GameControls keys = new GameControls();
        app.useControls(keys); app.openDemo(11);
        keys.keyEvent(38, true, 0); app.handle(TerminalKeys.UP);
        for (int i = 0; i < 10; i++) app.updateControls(NOW + i * 100_000_000L);
        assertTrue(rows(app.demo(90, 32, NOW, "")).contains("Cursor (2,-1)"));
        app.handle('\r'); app.handle('t'); app.handle(TerminalKeys.DOWN); app.handle('\r');
        assertTrue(rows(app.demo(90, 32, NOW, "")).contains("Cursor (2,-1) | Schadevakjes: 1"));
        app.handle(TerminalKeys.DOWN);
        assertTrue(rows(app.demo(90, 32, NOW, "")).contains("Cursor (2,0) | Schadevakjes: 1"));
    }
    @Test void cursorUpdatesUseTheExistingDifferenceRendererWithoutClearingTheScreen() {
        WeaponRangeLab lab = new WeaponRangeLab(); StringWriter output = new StringWriter();
        AnsiDisplay display = new AnsiDisplay(new PrintWriter(output));
        display.render(lab.frame(100, 39, NOW)); output.getBuffer().setLength(0);
        lab.handle(TerminalKeys.RIGHT, NOW); display.render(lab.frame(100, 39, NOW));
        assertFalse(output.toString().contains("\033[2J"));
        assertTrue(display.changedCells() < 50, "Alleen cursor en status hoeven opnieuw getekend te worden");
    }
}
