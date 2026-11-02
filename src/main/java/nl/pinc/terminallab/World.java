package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.List;

public final class World {
    public record Rect(int x, int y, int width, int height) {
        boolean overlaps(int px, int py, int pw, int ph) {
            return px < x + width && px + pw > x && py < y + height && py + ph > y;
        }
    }
    private final int width, height;
    private final List<Rect> obstacles = new ArrayList<>();
    public World(int width, int height, int actorWidth, int actorHeight) {
        this.width = width; this.height = height;
        // Leave the spawn and a route around each obstacle free, even for 32x32 sprites.
        int ox = actorWidth + 5;
        if (ox + 5 < width - 1 && actorHeight + 4 < height - 1) obstacles.add(new Rect(ox, 3, 4, 2));
        if (width > actorWidth * 2 + 14 && height > actorHeight + 10)
            obstacles.add(new Rect(width - 8, height - 5, 5, 2));
    }
    public boolean canPlace(int x, int y, int w, int h) {
        return x >= 1 && y >= 1 && x + w < width && y + h < height
            && obstacles.stream().noneMatch(o -> o.overlaps(x, y, w, h));
    }
    public Canvas draw(boolean checker) {
        Canvas canvas = new Canvas(width, height);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            boolean edge = x == 0 || y == 0 || x == width - 1 || y == height - 1;
            int ground = checker && (x / 4 + y / 2) % 2 == 0 ? 0x263d40 : 0x1b3035;
            canvas.put(x, y, new Cell(edge ? '#' : ' ', edge ? 0x587d82 : ground, ground));
        }
        for (Rect o : obstacles) for (int y = o.y; y < o.y + o.height; y++) for (int x = o.x; x < o.x + o.width; x++)
            canvas.put(x, y, new Cell((x + y) % 2 == 0 ? '#' : '+', 0xd8b68a, 0x76573f));
        return canvas;
    }
}
