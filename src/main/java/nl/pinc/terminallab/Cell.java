package nl.pinc.terminallab;

/** One terminal column; colors are 0xRRGGBB. Only single-column glyphs are used. */
public record Cell(char glyph, int foreground, int background) {
    public int topColor() { return glyph == '▀' || glyph == '█' ? foreground : background; }
    public int bottomColor() { return glyph == '▄' || glyph == '█' ? foreground : background; }
}
