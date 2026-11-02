package nl.pinc.terminallab;

/** Independent ANSI raster strategies, operating on terminal cells, not escape strings. */
public enum PixelRenderer {
    FULL_BLOCK("Voorgrond: █", 1), BACKGROUND("Achtergrond: spatie", 1), HALF_BLOCK("Twee pixels: ▀", 2);
    private final String label;
    private final int verticalPixels;
    PixelRenderer(String label, int verticalPixels) { this.label = label; this.verticalPixels = verticalPixels; }
    public String label() { return label; }
    public int rows(Sprite sprite) { return (sprite.height() + verticalPixels - 1) / verticalPixels; }
    public void draw(Canvas target, Sprite sprite, int left, int top) {
        for (int y = 0; y < rows(sprite); y++) for (int x = 0; x < sprite.width(); x++) {
            int tx = left + x, ty = top + y;
            if (!target.contains(tx, ty)) continue;
            Cell under = target.get(tx, ty);
            int upper = sprite.pixel(x, y * verticalPixels);
            if (this == HALF_BLOCK) {
                int lower = sprite.pixel(x, y * 2 + 1);
                if ((upper >>> 24) == 0 && (lower >>> 24) == 0) continue;
                target.put(tx, ty, new Cell('▀', blend(upper, under.topColor()), blend(lower, under.bottomColor())));
            } else if ((upper >>> 24) != 0) {
                int color = blend(upper, under.background());
                target.put(tx, ty, this == FULL_BLOCK ? new Cell('█', color, under.background()) : new Cell(' ', color, color));
            }
        }
    }
    public static int blend(int argb, int background) {
        int alpha = argb >>> 24;
        int result = 0;
        for (int shift : new int[]{16, 8, 0}) {
            int value = (((argb >>> shift) & 255) * alpha + ((background >>> shift) & 255) * (255 - alpha) + 127) / 255;
            result |= value << shift;
        }
        return result;
    }
}
