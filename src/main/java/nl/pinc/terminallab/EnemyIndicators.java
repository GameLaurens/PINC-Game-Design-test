package nl.pinc.terminallab;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Projects offscreen opponents onto the viewport border, relative to the player. */
final class EnemyIndicators {
    static final int MONSTER_COLOR = 0xff7777, PLAYER_COLOR = 0xffd180, MIXED_COLOR = 0x87d7ed;
    record Target(int x, int y, int width, int height, boolean player) { }
    private record Point(int x, int y) { }
    private record Group(int count, int kinds) { }

    static void draw(Canvas canvas, int cameraX, int cameraY, int playerX, int playerY, List<Target> targets) {
        int width = canvas.width(), height = canvas.height();
        if (width < 1 || height < 1) return;
        double px = Math.max(0, Math.min(width - 1, playerX - cameraX));
        double py = Math.max(0, Math.min(height - 1, playerY - cameraY));
        Map<Point, Group> groups = new LinkedHashMap<>();
        for (Target target : targets) {
            int x = target.x - cameraX, y = target.y - cameraY;
            // A partially visible model already shows the opponent's location.
            if (x < width && y < height && x + target.width > 0 && y + target.height > 0) continue;
            double dx = x + (target.width - 1) / 2.0 - px;
            double dy = y + (target.height - 1) / 2.0 - py;
            double horizontal = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? width - 1 - px : -px) / dx;
            double vertical = dy == 0 ? Double.POSITIVE_INFINITY : (dy > 0 ? height - 1 - py : -py) / dy;
            double scale = Math.min(horizontal, vertical);
            int edgeX = Math.max(0, Math.min(width - 1, (int) Math.round(px + dx * scale)));
            int edgeY = Math.max(0, Math.min(height - 1, (int) Math.round(py + dy * scale)));
            int kind = target.player ? 2 : 1;
            groups.merge(new Point(edgeX, edgeY), new Group(1, kind),
                (a, b) -> new Group(a.count + b.count, a.kinds | b.kinds));
        }
        groups.forEach((point, group) -> {
            char glyph = group.count > 9 ? '+' : group.count > 1 ? (char) ('0' + group.count)
                : group.kinds == 2 ? '\u25cb' : '\u25cf';
            int color = group.kinds == 3 ? MIXED_COLOR : group.kinds == 2 ? PLAYER_COLOR : MONSTER_COLOR;
            canvas.put(point.x, point.y, new Cell(glyph, color, canvas.get(point.x, point.y).background()));
        });
    }

    private EnemyIndicators() { }
}
