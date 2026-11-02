package nl.pinc.terminallab;

/** Original line-art adventurer inspired by pedit5's amber sword-and-shield figure. */
public final class PeditCharacter {
    public static final int AMBER = 0xffa52c;
    private final int size;
    private final int[] pixels;

    private PeditCharacter(int size) {
        this.size = size;
        pixels = new int[size * size];
    }

    public static Sprite create(int size, int pose) {
        PeditCharacter art = new PeditCharacter(size);
        // Coordinates describe a 32x32 drawing, rasterized afresh at the selected size.
        art.path(14, 2, 17, 1, 20, 3, 20, 6, 17, 8, 14, 6, 14, 2); // head
        art.line(17, 8, 17, 11); // neck
        art.path(13, 11, 20, 11, 19, 21, 13, 21, 13, 11); // tunic
        art.line(15, 12, 15, 20);
        art.line(17, 12, 17, 20);
        art.path(13, 11, 10, 17, 5, 16); // arm holding sword
        art.line(5, 1, 5, 22); // blade and grip
        art.line(2, 15, 8, 15); // guard
        art.line(20, 12, 23, 14); // shield arm
        art.path(25, 11, 29, 13, 29, 18, 25, 21, 22, 18, 22, 13, 25, 11);
        art.line(25, 13, 25, 18); // shield boss
        if (pose % 2 == 0) {
            art.path(15, 21, 13, 29, 10, 30);
            art.path(18, 21, 21, 29, 24, 30);
        } else {
            art.path(15, 21, 11, 26, 14, 30, 17, 30);
            art.path(18, 21, 21, 25, 20, 29, 23, 29);
        }
        return new Sprite(size, size, art.pixels);
    }

    private void path(int... points) {
        for (int i = 2; i < points.length; i += 2) line(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }

    /** Integer Bresenham lines keep the phosphor-like strokes one source pixel wide. */
    private void line(int x0, int y0, int x1, int y1) {
        x0 = x0 * (size - 1) / 31; y0 = y0 * (size - 1) / 31;
        x1 = x1 * (size - 1) / 31; y1 = y1 * (size - 1) / 31;
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, error = dx + dy;
        while (true) {
            pixels[y0 * size + x0] = 0xff000000 | AMBER;
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }
}
