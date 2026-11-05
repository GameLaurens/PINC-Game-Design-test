package nl.pinc.terminallab;

/** Shared retro palette and cell-based controls. */
final class MenuStyle {
    static final int BG = 0x191718, PANEL = 0x2b2728, INK = 0xe1d8c8, MUTED = 0xa39a90;
    static final int AMBER = 0xdb8671, GREEN = 0xb7bd91, LINE = 0x554a49;
    static Canvas canvas(int w, int h) {
        Canvas c = new Canvas(w, h);
        fill(c, 0, 0, w, h, BG);
        return c;
    }
    static void fill(Canvas c, int x, int y, int w, int h, int color) {
        for (int row = Math.max(0, y); row < Math.min(c.height(), y + h); row++)
            for (int col = Math.max(0, x); col < Math.min(c.width(), x + w); col++)
                c.put(col, row, new Cell(' ', INK, color));
    }
    static void text(Canvas c, int x, int y, int w, String s, int color) {
        if (w > 0) c.text(x, y, s.substring(0, Math.min(w, s.length())), color);
    }
    static void panel(Canvas c, int x, int y, int w, int h, String title) {
        fill(c, x, y, w, h, PANEL);
        text(c, x, y, w, "-".repeat(Math.max(0, w)), LINE);
        text(c, x + 1, y, w - 2, " " + title + " ", AMBER);
    }
    static void card(Canvas c, int x, int y, int w, int h, String title) {
        fill(c, x, y, w, h, PANEL);
        for (int col = 1; col < w - 1; col++) {
            c.put(x + col, y, new Cell('-', LINE, PANEL));
            c.put(x + col, y + h - 1, new Cell('-', LINE, PANEL));
        }
        for (int row = 1; row < h - 1; row++) {
            c.put(x, y + row, new Cell('|', LINE, PANEL));
            c.put(x + w - 1, y + row, new Cell('|', LINE, PANEL));
        }
        for (int col : new int[]{0, w - 1}) for (int row : new int[]{0, h - 1})
            c.put(x + col, y + row, new Cell('+', LINE, PANEL));
        text(c, x + 2, y, w - 4, " " + title + " ", AMBER);
    }
    static void choice(Canvas c, int x, int y, int w, String label, boolean selected) {
        choice(c, x, y, w, label, selected, true);
    }
    static void choice(Canvas c, int x, int y, int w, String label, boolean selected, boolean enabled) {
        if (selected) fill(c, x, y, w, 1, LINE);
        text(c, x, y, w, (selected ? "> " : "  ") + label, selected ? AMBER : enabled ? INK : MUTED);
    }
    static void center(Canvas c, int y, String s, int color) {
        text(c, Math.max(1, (c.width() - s.length()) / 2), y, c.width() - 2, s, color);
    }
    static void bar(Canvas c, int x, int y, int w, int value, int max, String label) {
        fill(c, x, y, w, 1, LINE);
        fill(c, x, y, max <= 0 ? 0 : (int) ((long) w * Math.max(0, value) / max), 1, 0x667451);
        text(c, x + Math.max(0, (w - label.length()) / 2), y, w, label, INK);
    }
    private MenuStyle() { }
}
