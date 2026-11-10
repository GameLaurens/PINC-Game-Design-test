package nl.pinc.terminallab;

import java.util.Arrays;
import java.util.Map;
import org.jline.utils.WCWidth;

/** Unicode art restricted to one-column BMP glyphs supported by the cell renderer. */
public final class UnicodeModel implements TextModel {
    private final String[] rows;
    private final int width;
    private final Map<Character, Integer> palette;

    public UnicodeModel(String... rows) {
        this(Map.of(), rows);
    }

    public UnicodeModel(Map<Character, Integer> palette, String... rows) {
        if (rows.length == 0) throw new IllegalArgumentException("Een model heeft minstens een regel nodig.");
        this.rows = rows.clone();
        this.palette = Map.copyOf(palette);
        for (String row : rows) for (char glyph : row.toCharArray())
            if (Character.isSurrogate(glyph) || Character.isISOControl(glyph) || WCWidth.wcwidth(glyph) != 1)
                throw new IllegalArgumentException("Gebruik alleen Unicode-tekens van één terminalkolom breed.");
        width = Arrays.stream(rows).mapToInt(String::length).max().orElseThrow();
    }

    public int width() { return width; }
    public int height() { return rows.length; }
    public void draw(Canvas canvas, int x, int y, int color) {
        for (int row = 0; row < rows.length; row++) for (int col = 0; col < rows[row].length(); col++) {
            char glyph = rows[row].charAt(col);
            if (glyph != ' ' && canvas.contains(x + col, y + row))
                canvas.put(x + col, y + row, new Cell(glyph, palette.getOrDefault(glyph, color), canvas.get(x + col, y + row).background()));
        }
    }
}
