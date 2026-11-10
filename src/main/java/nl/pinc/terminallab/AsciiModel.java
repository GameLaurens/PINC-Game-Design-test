package nl.pinc.terminallab;

import java.util.Arrays;

/** Reusable text sprite: add a model by writing its rows, using printable ASCII only. */
public final class AsciiModel implements TextModel {
    private final String[] rows;
    private final int width;
    public AsciiModel(String... rows) {
        if (rows.length == 0) throw new IllegalArgumentException("Een model heeft minstens een regel nodig.");
        this.rows = rows.clone();
        for (String row : rows) if (row.chars().anyMatch(c -> c < 32 || c > 126))
            throw new IllegalArgumentException("Gebruik alleen afdrukbare ASCII-tekens.");
        width = Arrays.stream(rows).mapToInt(String::length).max().orElseThrow();
    }
    public int width() { return width; }
    public int height() { return rows.length; }
    public void draw(Canvas canvas, int x, int y, int color) {
        for (int row = 0; row < rows.length; row++) for (int col = 0; col < rows[row].length(); col++) {
            char glyph = rows[row].charAt(col);
            if (glyph != ' ' && canvas.contains(x + col, y + row))
                canvas.put(x + col, y + row, new Cell(glyph, color, canvas.get(x + col, y + row).background()));
        }
    }
}
