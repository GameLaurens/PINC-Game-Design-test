package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FullscreenTest {
    private static final long NOW = 20_000_000_000L;
    @Test void pixelTitleMovesUpAndDownWhileThePromptStaysInPlace() {
        GameMenu menu = new GameMenu();
        Canvas center = menu.frame(140, 40, NOW);
        Canvas down = menu.frame(140, 40, NOW + 1_000_000_000L);
        Canvas up = menu.frame(140, 40, NOW + 3_000_000_000L);
        assertTrue(firstPixel(down) > firstPixel(center));
        assertTrue(firstPixel(up) < firstPixel(center));
        for (int x = 0; x < center.width(); x++)
            assertEquals(center.get(x, 34), down.get(x, 34), "De startprompt mag niet met het logo verspringen");
    }
    private int firstPixel(Canvas c) {
        for (int y = 0; y < c.height(); y++) for (int x = 0; x < c.width(); x++)
            if (c.get(x, y).glyph() == '\u2588') return y;
        fail("Pixellettering ontbreekt"); return -1;
    }
    @Test void everyDemoFillsTheCentralAreaAtLargeTerminalSizes() throws Exception {
        Main app = new Main(null, "test", 16);
        for (int mode = 1; mode <= 10; mode++) {
            app.openDemo(mode);
            for (int[] size : new int[][]{{140, 40}, {240, 80}, {320, 120}}) {
                Canvas c = app.frame(size[0], size[1], NOW, "");
                GameLayout layout = GameLayout.of(size[0], size[1]);
                assertTrue(layout.fieldWidth() >= size[0] * 0.7, "Het spel moet het grootste deel van het scherm krijgen");
                int vacant = 0;
                for (int y = 0; y < layout.fieldHeight(); y++)
                    for (int x = layout.left(); x < size[0] - layout.right(); x++)
                        if (c.get(x, y).background() == 0x101923 || c.get(x, y).background() == MenuStyle.BG) vacant++;
                assertEquals(0, vacant, "Demo " + mode + " laat ongebruikte randen binnen het speelveld achter");
                assertEquals(MenuStyle.PANEL, c.get(0, size[1] - 1).background());
                assertEquals(MenuStyle.PANEL, c.get(size[0] - 1, size[1] - 1).background());
            }
        }
    }
    @Test void wideningTheTerminalActuallyShowsMoreWorldRatherThanOnlyLargerMargins() {
        FreeForAllView view = new FreeForAllView(new FreeForAll(NOW, false));
        Canvas small = view.arenaFrame(90, 28, NOW, false, false);
        Canvas large = view.arenaFrame(180, 75, NOW, false, false);
        assertEquals(180, large.width()); assertEquals(75, large.height());
        assertNotEquals(0x101923, large.get(179, 74).background());
        assertNotEquals(small.get(89, 27).background(), large.get(179, 74).background(), "Het grotere beeld moet de zuidelijke kaartregio tonen");
    }
    @Test void expandedShowroomsAndAdventureRemainWalkableAndPreserveProgressWhenResized() {
        Showroom showroom = new Showroom(true); showroom.interact();
        showroom.arenaFrame(220, 90, NOW, false, false);
        assertNotNull(showroom.weapon());
        assertTrue(showroom.canPlace(200, 75));
        showroom.arenaFrame(52, 21, NOW, false, false);
        assertNotNull(showroom.weapon());
        AsciiLevel level = new AsciiLevel(60, 14); level.interact();
        int px = level.playerX(), py = level.playerY();
        level.arenaFrame(220, 90, NOW, false, false);
        assertEquals(220, level.width()); assertEquals(90, level.height());
        level.arenaFrame(52, 21, NOW, false, false);
        assertEquals(px, level.playerX()); assertEquals(py, level.playerY());
    }
    @Test void leaderboardUsesActualKillsAndUpgradeBarsReflectPurchasedLevels() {
        FreeForAll world = new FreeForAll(NOW, false);
        world.player().totalXp = 100;
        world.buy(world.player(), FreeForAll.Upgrade.HP);
        var bot = world.addFighter("Rivaal", true, 30, 30, 10, NOW); bot.kills = 8;
        Canvas c = MenuStyle.canvas(28, 50);
        GameHud.draw(c, 28, 50, world, 0);
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < c.height(); y++) {
            for (int x = 0; x < c.width(); x++) text.append(c.get(x, y).glyph());
            text.append('\n');
        }
        assertTrue(text.toString().contains("Skill upgrades: 1"));
        assertTrue(text.toString().contains("8 kills"));
        assertTrue(text.indexOf("Rivaal") < text.indexOf("JIJ"));
    }
}
