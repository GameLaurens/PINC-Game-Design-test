package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenderingTest {
    private Canvas background() {
        Canvas canvas = new Canvas(6, 6);
        for (int y = 0; y < 6; y++) for (int x = 0; x < 6; x++) canvas.put(x, y, new Cell('.', 0xffffff, 0x123456));
        return canvas;
    }
    @Test void fullBlockAndBackgroundHaveSameVisibleColorButDifferentGlyphs() {
        Sprite s = new Sprite(1, 1, new int[]{0xffabcdef});
        Canvas full = background(), bg = background();
        PixelRenderer.FULL_BLOCK.draw(full, s, 1, 1);
        PixelRenderer.BACKGROUND.draw(bg, s, 1, 1);
        assertEquals(new Cell('█', 0xabcdef, 0x123456), full.get(1, 1));
        assertEquals(new Cell(' ', 0xabcdef, 0xabcdef), bg.get(1, 1));
    }
    @Test void halfBlocksPackTwoDifferentColorsAndPreserveOddLastRowBackground() {
        Sprite s = new Sprite(1, 3, new int[]{0xffff0000, 0xff00ff00, 0xff0000ff});
        Canvas c = background();
        PixelRenderer.HALF_BLOCK.draw(c, s, 1, 1);
        assertEquals(2, PixelRenderer.HALF_BLOCK.rows(s));
        assertEquals(new Cell('▀', 0xff0000, 0x00ff00), c.get(1, 1));
        assertEquals(new Cell('▀', 0x0000ff, 0x123456), c.get(1, 2));
    }
    @Test void transparentPixelsPreserveTheWorldInEveryStrategy() {
        Sprite s = new Sprite(1, 2, new int[]{0x00112233, 0});
        for (PixelRenderer method : PixelRenderer.values()) {
            Canvas c = background(); method.draw(c, s, 1, 1);
            assertEquals(new Cell('.', 0xffffff, 0x123456), c.get(1, 1));
        }
    }
    @Test void halfTransparentPairCompositesEachHalfOnItsOwnBackground() {
        Canvas c = background();
        c.put(1, 1, new Cell('▀', 0x000000, 0xffffff));
        PixelRenderer.HALF_BLOCK.draw(c, new Sprite(1, 2, new int[]{0x80ff0000, 0x00000000}), 1, 1);
        assertEquals(new Cell('▀', 0x800000, 0xffffff), c.get(1, 1));
        assertEquals(0x7f7f7f, PixelRenderer.blend(0x80000000, 0xffffff));
    }
    @Test void drawingOutsideCanvasClipsWithoutExceptions() {
        for (PixelRenderer method : PixelRenderer.values()) {
            assertDoesNotThrow(() -> method.draw(background(), new Sprite(2, 2, new int[]{-1, -1, -1, -1}), -1, -1));
        }
    }
    @Test void diffWriterEmitsNothingForAnIdenticalFrameAndRestoresOldPlayerCells() {
        StringWriter output = new StringWriter();
        AnsiDisplay display = new AnsiDisplay(new PrintWriter(output));
        Canvas first = background(); first.put(1, 1, new Cell('O', 0xffffff, 0x123456));
        display.render(first);
        assertTrue(output.toString().contains("38;2;255;255;255m"));
        assertTrue(output.toString().contains("48;2;18;52;86m"));
        output.getBuffer().setLength(0);
        display.render(first);
        assertEquals("", output.toString());
        Canvas moved = background(); moved.put(2, 1, new Cell('O', 0xffffff, 0x123456));
        display.render(moved);
        assertEquals(2, display.changedCells());
        assertTrue(output.toString().contains(".O"));
        assertFalse(output.toString().contains("\033[2J"));
    }
    @Test void pngFixturesAreActualArgbImagesAtAllThreeResolutions() throws Exception {
        for (int size : new int[]{8, 16, 32}) {
            Sprite s = Sprite.bundled(size);
            assertEquals(size, s.width()); assertEquals(size, s.height());
            assertEquals(0, s.pixel(0, 0) >>> 24);
            assertNotEquals(0, s.pixel(size / 2, size / 2) >>> 24);
        }
        assertEquals(65, Sprite.bundled(32).pixel(5, 31) >>> 24);
    }
    @Test void rectangularPngKeepsAspectRatioAndAlpha(@TempDir Path dir) throws Exception {
        var image = new java.awt.image.BufferedImage(2, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80ff0000);
        Path path = dir.resolve("test.png");
        javax.imageio.ImageIO.write(image, "PNG", path.toFile());
        Sprite s = Sprite.load(path).fit(8);
        assertEquals(0, s.pixel(0, 0));
        assertEquals(0x80ff0000, s.pixel(0, 2));
        assertEquals(0, s.pixel(7, 2));
    }
    @Test void invalidAndExcessivelyLargePngAreRejectedBeforeAllocation(@TempDir Path dir) throws Exception {
        Path invalid = dir.resolve("invalid.png"); Files.writeString(invalid, "geen PNG");
        assertThrows(java.io.IOException.class, () -> Sprite.load(invalid));
        Path big = dir.resolve("big.png");
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(513, 1, 2), "PNG", big.toFile());
        assertThrows(java.io.IOException.class, () -> Sprite.load(big));
    }
    @Test void worldRejectsWallsAndObstaclesButAllowsSpawn() {
        World w = new World(80, 30, 8, 8);
        assertTrue(w.canPlace(2, 2, 8, 8));
        assertFalse(w.canPlace(0, 2, 8, 8));
        assertFalse(w.canPlace(75, 2, 8, 8));
        assertFalse(w.canPlace(13, 3, 8, 8));
    }
    @Test void allNineDemosRenderAndRespondToMovementAtEverySize() throws Exception {
        for (int size : new int[]{8, 16, 32}) for (int mode = 1; mode <= 9; mode++) {
            Main app = new Main(null, "test", size);
            app.handle('0' + mode);
            Canvas before = app.demo(160, 55, System.nanoTime(), "");
            app.handle('d');
            Canvas after = app.demo(160, 55, System.nanoTime(), "");
            int changed = 0;
            for (int y = 4; y < 51; y++) for (int x = 0; x < 160; x++)
                if (!before.get(x, y).equals(after.get(x, y))) changed++;
            assertTrue(changed > 0, "Demo " + mode + ", size " + size + " moet bewegen");
        }
    }
    @Test void resizingToTinyTerminalAndBackDoesNotBreakAnyDemo() throws Exception {
        Main app = new Main(null, "test", 32);
        for (int mode = 1; mode <= 9; mode++) {
            app.handle('0' + mode);
            assertDoesNotThrow(() -> app.demo(1, 1, System.nanoTime(), ""));
            assertDoesNotThrow(() -> app.demo(160, 55, System.nanoTime(), ""));
        }
    }
    @Test void textTechniquesContainRequiredGlyphs() {
        Canvas c = background(); TextArt.ASCII.draw(c, 0, 0, 0);
        assertEquals('O', c.get(1, 0).glyph());
        assertEquals('/', c.get(0, 1).glyph());
        assertEquals('\\', c.get(2, 1).glyph());
    }
}
