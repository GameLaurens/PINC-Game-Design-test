package nl.pinc.terminallab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** ARGB raster; ImageIO only decodes pixels and never opens a window. */
public final class Sprite {
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 4L * 1024 * 1024;
    private final int width, height;
    private final int[] pixels;
    public Sprite(int width, int height, int[] pixels) {
        if (width < 1 || height < 1 || pixels.length != width * height) throw new IllegalArgumentException("Ongeldig raster");
        this.width = width; this.height = height; this.pixels = pixels.clone();
    }
    public int width() { return width; }
    public int height() { return height; }
    public int pixel(int x, int y) { return x < 0 || y < 0 || x >= width || y >= height ? 0 : pixels[y * width + x]; }
    public static Sprite load(Path path) throws IOException {
        if (Files.size(path) > 8 * 1024 * 1024) throw new IOException("PNG mag maximaal 8 MiB zijn.");
        try (InputStream stream = Files.newInputStream(path)) { return load(stream); }
    }
    public static Sprite bundled(int size) throws IOException {
        try (InputStream stream = Sprite.class.getResourceAsStream("/sprites/hero-" + size + ".png")) {
            if (stream == null) throw new IOException("Voorbeeldsprite ontbreekt.");
            return load(stream);
        }
    }
    private static Sprite load(InputStream stream) throws IOException {
        // No disk cache in the user's temp folder, including for bundled resources.
        try (ImageInputStream image = new javax.imageio.stream.MemoryCacheImageInputStream(stream)) {
            var readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext()) throw new IOException("Geen leesbare PNG-afbeelding.");
            ImageReader reader = readers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase("png")) throw new IOException("Alleen PNG wordt ondersteund.");
                reader.setInput(image);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > MAX_DIMENSION || h > MAX_DIMENSION || (long) w * h > MAX_PIXELS)
                    throw new IOException("PNG mag maximaal 4096 pixels per zijde en 4.194.304 pixels totaal bevatten.");
                var raster = reader.read(0);
                return new Sprite(w, h, raster.getRGB(0, 0, w, h, null, 0, w));
            } finally { reader.dispose(); }
        }
    }
    /** Nearest-neighbor, preserving aspect ratio with transparent padding. */
    public Sprite fit(int size) {
        int w = Math.max(1, (int) Math.round((double) width * size / Math.max(width, height)));
        int h = Math.max(1, (int) Math.round((double) height * size / Math.max(width, height)));
        int[] out = new int[size * size];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            out[(y + (size - h) / 2) * size + x + (size - w) / 2] = pixel(x * width / w, y * height / h);
        return new Sprite(size, size, out);
    }
    /** Deliberately tiny two-pose leg animation, for the bundled character only. */
    public Sprite walking(int pose) {
        if (pose == 0) return this;
        int[] out = pixels.clone();
        int start = height * 3 / 4;
        for (int y = start; y < height; y++) for (int x = 0; x < width; x++) {
            int shift = x < width / 2 ? -1 : 1;
            out[y * width + x] = pixel(x - shift, y);
        }
        return new Sprite(width, height, out);
    }
}
