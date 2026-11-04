package nl.pinc.terminallab;

/** Original 5x7 lettering. Pixels use two terminal columns to keep square proportions. */
final class PixelTitle {
    private static final String[][] LETTERS = {
        {"01111", "11000", "11000", "11011", "11001", "11001", "01111"},
        {"11000", "11000", "11000", "11000", "11000", "11000", "11111"},
        {"11001", "11001", "01110", "00100", "00100", "00100", "00100"},
        {"11110", "11001", "11001", "11110", "11000", "11000", "11000"},
        {"11001", "11001", "11001", "11111", "11001", "11001", "11001"}
    };
    static int scale(int width, int height) {
        int amplitude = Math.max(1, Math.min(3, height / 18));
        return Math.max(1, Math.min((width - 6) / 58, (height - 14 - 2 * amplitude) / 7));
    }
    static void draw(Canvas c, int top, int scale) {
        int left = (c.width() - 58 * scale) / 2;
        drawAt(c, left + scale, top + 1, scale, MenuStyle.LINE);
        drawAt(c, left, top, scale, MenuStyle.INK);
    }
    private static void drawAt(Canvas c, int left, int top, int scale, int color) {
        for (int letter = 0; letter < LETTERS.length; letter++)
            for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) {
                if (LETTERS[letter][y].charAt(x) == '0') continue;
                for (int row = 0; row < scale; row++)
                    MenuStyle.text(c, left + (letter * 6 + x) * scale * 2, top + y * scale + row,
                        scale * 2, "\u2588".repeat(scale * 2), color);
            }
    }
    private PixelTitle() { }
}
