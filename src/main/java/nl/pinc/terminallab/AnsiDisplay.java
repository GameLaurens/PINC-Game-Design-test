package nl.pinc.terminallab;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** Diff the composed frame: no clear-screen between movement frames. */
public final class AnsiDisplay {
    public static final String ESC = "\033[";
    private final PrintWriter out;
    private Canvas previous;
    private long totalBytes;
    private int changedCells;
    public AnsiDisplay(PrintWriter out) { this.out = out; }
    public long totalBytes() { return totalBytes; }
    public int changedCells() { return changedCells; }
    public void invalidate() { previous = null; }
    public void render(Canvas next) {
        if (previous != null && (previous.width() != next.width() || previous.height() != next.height())) previous = null;
        StringBuilder buffer = new StringBuilder();
        int fg = -1, bg = -1;
        changedCells = 0;
        for (int y = 0; y < next.height(); y++) {
            int lastX = -2;
            for (int x = 0; x < next.width(); x++) {
                Cell cell = next.get(x, y);
                if (previous != null && cell.equals(previous.get(x, y))) continue;
                if (x != lastX + 1) buffer.append(ESC).append(y + 1).append(';').append(x + 1).append('H');
                if (fg != cell.foreground()) { color(buffer, 38, cell.foreground()); fg = cell.foreground(); }
                if (bg != cell.background()) { color(buffer, 48, cell.background()); bg = cell.background(); }
                buffer.append(cell.glyph()); lastX = x; changedCells++;
            }
        }
        if (!buffer.isEmpty()) {
            buffer.append(ESC).append("0m");
            String output = buffer.toString();
            totalBytes += output.getBytes(StandardCharsets.UTF_8).length;
            out.print(output); out.flush();
        }
        previous = next;
    }
    private static void color(StringBuilder buffer, int plane, int rgb) {
        buffer.append(ESC).append(plane).append(";2;").append((rgb >> 16) & 255).append(';')
              .append((rgb >> 8) & 255).append(';').append(rgb & 255).append('m');
    }
}
