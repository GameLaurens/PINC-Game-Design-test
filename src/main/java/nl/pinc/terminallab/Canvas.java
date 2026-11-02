package nl.pinc.terminallab;

import java.util.Arrays;

public final class Canvas {
    public static final int INK = 0xdce8ee;
    private final int width, height;
    private final Cell[] cells;

    public Canvas(int width, int height) {
        this.width = width; this.height = height;
        cells = new Cell[width * height];
        Arrays.fill(cells, new Cell(' ', INK, 0x101923));
    }
    public int width() { return width; }
    public int height() { return height; }
    public boolean contains(int x, int y) { return x >= 0 && y >= 0 && x < width && y < height; }
    public Cell get(int x, int y) { return cells[y * width + x]; }
    public void put(int x, int y, Cell cell) { if (contains(x, y)) cells[y * width + x] = cell; }
    public void text(int x, int y, String text, int color) {
        for (int i = 0; i < text.length(); i++) {
            if (contains(x + i, y)) put(x + i, y, new Cell(text.charAt(i), color, get(x + i, y).background()));
        }
    }
    public void text(int x, int y, String text) { text(x, y, text, INK); }
    public void blit(Canvas other, int x, int y) {
        for (int row = 0; row < other.height; row++)
            for (int col = 0; col < other.width; col++) put(x + col, y + row, other.get(col, row));
    }
}
