package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.List;
import nl.pinc.terminallab.ShowroomCatalog.Monster;
import nl.pinc.terminallab.ShowroomCatalog.Weapon;

/** Walkable ASCII galleries. The fixed world survives terminal resizing. */
public final class Showroom {
    public static final int WIDTH = 124, PLAYER_WIDTH = 10, PLAYER_HEIGHT = 5;
    public static final int TARGET_X = 60, TARGET_Y = 46, HIT_COLOR = 0xff6565;
    public enum Direction {
        UP(0, -1, "omhoog"), DOWN(0, 1, "omlaag"), LEFT(-1, 0, "links"), RIGHT(1, 0, "rechts");
        final int dx, dy;
        final String label;
        Direction(int dx, int dy, String label) { this.dx = dx; this.dy = dy; this.label = label; }
    }
    public record Exhibit(int index, int x, int y) { }
    public record Camera(int x, int y, int width, int height) { }
    private record Strike(Weapon weapon, Direction direction, int x, int y, long until) { }

    private final boolean weapons;
    private int worldWidth = WIDTH, worldHeight;
    private final List<Exhibit> exhibits = new ArrayList<>();
    private final List<Weapon> inventory = new ArrayList<>();
    private int x = 3, y = 2, selected, steps, hits, lastDamage;
    private long totalDamage, lastMove, lastAttack = Long.MIN_VALUE, hitUntil;
    private Direction facing = Direction.RIGHT;
    private Strike strike;
    private boolean showRange = true;
    private String message;

    public Showroom(boolean weapons) {
        this.weapons = weapons;
        worldHeight = weapons ? 58 : 44;
        for (int i = 0; i < 15; i++) exhibits.add(new Exhibit(i, 6 + (i % 5) * 24, 7 + (i / 5) * 12));
        message = weapons ? "Loop naar een wapen en druk E. Testmonster: (60,46), onder de rekken."
            : "15 ASCII-monsters: loop langs de modellen. De camera volgt je.";
    }
    public int height() { return worldHeight; }
    public int playerX() { return x; }
    public int playerY() { return y; }
    public int hits() { return hits; }
    public long totalDamage() { return totalDamage; }
    public int lastDamage() { return lastDamage; }
    public String message() { return message; }
    public Weapon weapon() { return inventory.isEmpty() ? null : inventory.get(selected); }
    public List<Weapon> inventory() { return List.copyOf(inventory); }
    public List<Exhibit> exhibits() { return List.copyOf(exhibits); }

    boolean canPlace(int px, int py) {
        if (px < 1 || py < 1 || px + PLAYER_WIDTH >= worldWidth || py + PLAYER_HEIGHT >= height()) return false;
        if (weapons) return !new World.Rect(TARGET_X, TARGET_Y, Monster.GOLEM.model.width(), Monster.GOLEM.model.height())
            .overlaps(px, py, PLAYER_WIDTH, PLAYER_HEIGHT);
        return exhibits.stream().noneMatch(e -> new World.Rect(e.x, e.y,
            Monster.values()[e.index].model.width(), Monster.values()[e.index].model.height())
            .overlaps(px, py, PLAYER_WIDTH, PLAYER_HEIGHT));
    }
    public boolean move(Direction direction, long now) {
        return move(direction.dx, direction.dy, now);
    }
    public boolean move(int dx, int dy, long now) {
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || (dx == 0 && dy == 0)) return false;
        facing = dx != 0 ? (dx < 0 ? Direction.LEFT : Direction.RIGHT) : (dy < 0 ? Direction.UP : Direction.DOWN);
        var step = Movement.resolve(x, y, dx, dy, this::canPlace);
        if (!step.moved()) return false;
        x += step.dx(); y += step.dy(); steps++; lastMove = now;
        return true;
    }
    private int gap(Exhibit e) {
        AsciiModel model = weapons ? Weapon.values()[e.index].model : Monster.values()[e.index].model;
        return Math.max(0, Math.max(e.x - (x + PLAYER_WIDTH), x - (e.x + model.width())))
            + Math.max(0, Math.max(e.y - (y + PLAYER_HEIGHT), y - (e.y + model.height())));
    }
    private Exhibit nearest(boolean uncollectedOnly) {
        Exhibit best = null;
        for (Exhibit e : exhibits) {
            if (uncollectedOnly && inventory.contains(Weapon.values()[e.index])) continue;
            if (best == null || gap(e) < gap(best)) best = e;
        }
        return best;
    }
    public void interact() {
        if (!weapons) return;
        Exhibit e = nearest(true);
        if (e == null || gap(e) > 2) { message = "Ga naast een wapen staan en druk E. F wisselt je verzamelde wapens."; return; }
        Weapon weapon = Weapon.values()[e.index];
        inventory.add(weapon); selected = inventory.size() - 1;
        message = "Opgepakt: " + weapon.label + ". Pijlen: gericht aanvallen | F: wisselen.";
    }
    public void nextWeapon() {
        if (inventory.isEmpty()) { message = "Pak eerst een wapen op met E."; return; }
        selected = (selected + 1) % inventory.size();
        message = "Uitgerust: " + weapon().label + " | " + weapon().style();
    }
    public void attack(Direction direction, long now) {
        if (!weapons) return;
        facing = direction;
        Weapon weapon = weapon();
        if (weapon == null) { message = "Pak eerst een wapen op met E."; return; }
        if (lastAttack != Long.MIN_VALUE && now - lastAttack < weapon.cooldownMs * 1_000_000L) return;
        lastAttack = now;
        strike = new Strike(weapon, direction, x, y, now + 220_000_000L);
        boolean hit = false;
        for (int row = TARGET_Y; row < TARGET_Y + Monster.GOLEM.model.height(); row++)
            for (int col = TARGET_X; col < TARGET_X + Monster.GOLEM.model.width(); col++)
                if (inAttack(weapon, direction, x, y, col, row)) hit = true;
        if (hit) {
            hits++; lastDamage = weapon.damage; totalDamage += weapon.damage; hitUntil = now + 300_000_000L;
            message = "RAAK! " + weapon.label + ": " + weapon.damage + " schade. De golem is onsterfelijk.";
        } else message = "MIS | " + weapon.label + " | " + weapon.range + " bereik | richt " + direction.label + " naar de golem.";
    }
    /** Cell-accurate attack mask, shared by damage detection and the visible range preview. */
    static boolean inAttack(Weapon weapon, Direction direction, int px, int py, int cx, int cy) {
        int gapX = cx < px ? px - cx : Math.max(0, cx - (px + PLAYER_WIDTH) + 1);
        int gapY = cy < py ? py - cy : Math.max(0, cy - (py + PLAYER_HEIGHT) + 1);
        if (weapon.shape == ShowroomCatalog.Shape.BURST) return gapX + gapY > 0 && gapX + gapY <= weapon.range;
        int forward = switch (direction) {
            case RIGHT -> cx - (px + PLAYER_WIDTH) + 1;
            case LEFT -> px - cx;
            case DOWN -> cy - (py + PLAYER_HEIGHT) + 1;
            case UP -> py - cy;
        };
        int side = direction.dx == 0 ? Math.abs(cx - (px + PLAYER_WIDTH / 2)) : Math.abs(cy - (py + PLAYER_HEIGHT / 2));
        int spread = weapon.halfWidth + (weapon.shape == ShowroomCatalog.Shape.FAN ? Math.max(0, forward) / 3 : 0);
        return forward >= 1 && forward <= weapon.range && side <= spread;
    }
    public boolean handle(int input, long now) {
        Direction direction = switch (input) {
            case TerminalKeys.UP -> Direction.UP;
            case TerminalKeys.DOWN -> Direction.DOWN;
            case TerminalKeys.LEFT -> Direction.LEFT;
            case TerminalKeys.RIGHT -> Direction.RIGHT;
            default -> null;
        };
        if (direction != null) { if (weapons) attack(direction, now); return true; }
        switch (Character.toLowerCase(input)) {
            case 'w' -> move(Direction.UP, now);
            case 's' -> move(Direction.DOWN, now);
            case 'a' -> move(Direction.LEFT, now);
            case 'd' -> move(Direction.RIGHT, now);
            case 'e' -> interact();
            case 'f' -> { if (weapons) nextWeapon(); }
            case ' ' -> attack(facing, now);
            case 'v' -> showRange = !showRange;
            default -> { return false; }
        }
        return true;
    }
    public Camera camera(int width, int height) {
        int w = Math.min(worldWidth, Math.max(1, width)), h = Math.min(height(), Math.max(1, height));
        return new Camera(Math.max(0, Math.min(worldWidth - w, x + PLAYER_WIDTH / 2 - w / 2)),
            Math.max(0, Math.min(height() - h, y + PLAYER_HEIGHT / 2 - h / 2)), w, h);
    }
    public Canvas frame(int width, int height, long now, boolean checker, boolean animate) {
        Canvas canvas = new Canvas(width, height);
        canvas.text(2, 0, weapons ? "5  WAPENSHOWROOM - 15 wapens + onsterfelijke golem" : "4  MONSTERSHOWROOM - 15 ASCII-modellen", 0x71e6bb);
        canvas.text(2, 1, weapons ? "WASD lopen | E pakken | F wisselen | Pijlen aanval" : "WASD lopen | Bekijk alle 15 monsters in drie rijen");
        canvas.text(2, 2, weapons ? "Spatie: kijkrichting | V bereik | R reset | M menu | Q stop" : "B grond | P loopanimatie | R reset | M menu | Q stop");
        if (width < 64 || height < 22) {
            canvas.text(2, 5, "Vergroot de terminal naar minstens 64x22 cellen.");
            canvas.text(2, 7, "Positie en verzamelde wapens blijven bewaard.");
            return canvas;
        }
        Camera camera = camera(width - 4, height - 10);
        canvas.text(2, 3, "Jij " + x + "," + y + " | kaart " + WIDTH + "x" + height()
            + (weapons ? " | Golem 60,46 (onder de rekken)" : " | Loop ook naar rechts en beneden"), 0x8ca6b4);
        canvas.blit(draw(camera, now, checker, animate), 2, 4);
        Exhibit nearby = nearest(false);
        if (weapons) {
            Weapon weapon = weapon();
            canvas.text(2, height - 5, "Wapen: " + (weapon == null ? "geen | E: oppakken" : weapon.label + " | " + weapon.stats()), 0xffd180);
            long remaining = weapon == null || lastAttack == Long.MIN_VALUE ? 0 : Math.max(0, weapon.cooldownMs - (now - lastAttack) / 1_000_000L);
            canvas.text(2, height - 4, "Treffers " + hits + " | Laatste " + lastDamage + " | Totaal " + totalDamage
                + " | " + (remaining == 0 ? "KLAAR" : remaining + "ms wachten"));
            canvas.text(2, height - 3, "Verzameld " + inventory.size() + "/15 | " + (weapon == null ? "Pak een wapen bij een rek."
                : weapon.style() + " | " + facing.label + " | . bereik, * aanval"), 0x8ca6b4);
        } else {
            Monster monster = Monster.values()[nearby.index];
            canvas.text(2, height - 5, "Dichtstbij: " + (nearby.index + 1) + "/15 " + monster.label, monster.color);
            canvas.text(2, height - 4, monster.description + " | Model " + monster.model.width() + "x" + monster.model.height() + " tekens");
            canvas.text(2, height - 3, "Rij " + (nearby.index / 5 + 1) + "/3 | Vak " + (nearby.index % 5 + 1)
                + "/5 | Model op " + nearby.x + "," + nearby.y, 0x8ca6b4);
        }
        canvas.text(2, height - 2, message, 0xffd180);
        if (weapons && gap(nearby) <= 2) {
            Weapon item = Weapon.values()[nearby.index];
            canvas.text(2, height - 1, "Rek: " + item.label + " | " + item.stats() + " | " + item.style(), 0x8ca6b4);
        } else canvas.text(2, height - 1, "Alle modellen en effecten: ASCII-tekst + ANSI-kleuren.", 0x8ca6b4);
        return canvas;
    }
    public Canvas draw(Camera camera, long now, boolean checker, boolean animate) {
        Canvas scene = new Canvas(worldWidth, height());
        for (int row = 0; row < height(); row++) for (int col = 0; col < worldWidth; col++) {
            boolean edge = col == 0 || row == 0 || col == worldWidth - 1 || row == height() - 1;
            int ground = checker && (col / 6 + row / 3) % 2 == 0 ? 0x233438 : 0x192a2f;
            scene.put(col, row, new Cell(edge ? '#' : ' ', edge ? 0x6c898d : ground, ground));
        }
        if (weapons && weapon() != null && showRange) drawMask(scene, weapon(), facing, x, y, '.', 0x577a7c);
        if (strike != null && now < strike.until) drawMask(scene, strike.weapon, strike.direction, strike.x, strike.y, '*', 0xffd180);
        for (Exhibit e : exhibits) {
            if (weapons) {
                Weapon weapon = Weapon.values()[e.index];
                scene.text(e.x, e.y - 1, String.format("%02d %s", e.index + 1, weapon.label), 0x87d7ed);
                if (!inventory.contains(weapon)) weapon.model.draw(scene, e.x + 2, e.y, 0x87d7ed);
                else scene.text(e.x + 2, e.y + 1, "[opgepakt]", 0x577a7c);
                scene.text(e.x, e.y + 6, weapon.damage + " dmg / " + weapon.range + " bereik", 0xa5b6bb);
            } else {
                Monster monster = Monster.values()[e.index];
                scene.text(e.x, e.y - 1, String.format("%02d %s", e.index + 1, monster.label), monster.color);
                monster.model.draw(scene, e.x, e.y, monster.color);
            }
        }
        if (weapons) {
            scene.text(44, 41, "TESTVELD - pijlen richten, . toont bereik", 0x71e6bb);
            scene.text(44, 42, "Golem blijft leven. Geen munitielimiet.", 0x8ca6b4);
            scene.text(TARGET_X - 1, TARGET_Y - 1, "GOLEM [oneindig HP]", 0x71e6bb);
            Monster.GOLEM.model.draw(scene, TARGET_X, TARGET_Y, now < hitUntil ? HIT_COLOR : Monster.GOLEM.color);
            scene.text(TARGET_X - 1, TARGET_Y + 6, "Totaal: " + totalDamage, 0xffd180);
        }
        boolean walking = animate && now - lastMove < 180_000_000L;
        TextArt.ASCII.draw(scene, x, y + 1, walking ? (steps / 2) % 2 : 0);
        if (weapons && weapon() != null) weapon().model.draw(scene, x + 4, y, 0x87d7ed);
        Canvas viewport = new Canvas(camera.width, camera.height);
        for (int row = 0; row < camera.height; row++) for (int col = 0; col < camera.width; col++)
            viewport.put(col, row, scene.get(camera.x + col, camera.y + row));
        return viewport;
    }
    Canvas arenaFrame(int width, int height, long now, boolean checker, boolean animate) {
        worldWidth = Math.max(worldWidth, width); worldHeight = Math.max(worldHeight, height);
        return draw(camera(width, height), now, checker, animate);
    }
    private void drawMask(Canvas scene, Weapon weapon, Direction direction, int px, int py, char glyph, int color) {
        for (int row = Math.max(1, py - weapon.range); row < Math.min(height() - 1, py + PLAYER_HEIGHT + weapon.range); row++)
            for (int col = Math.max(1, px - weapon.range); col < Math.min(WIDTH - 1, px + PLAYER_WIDTH + weapon.range); col++)
                if (inAttack(weapon, direction, px, py, col, row))
                    scene.put(col, row, new Cell(glyph, color, scene.get(col, row).background()));
    }
}
