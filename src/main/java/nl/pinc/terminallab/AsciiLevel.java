package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.List;

/** Level 1 is entirely character based; models, equipment and game state are separate. */
public final class AsciiLevel {
    public enum Weapon {
        NONE("geen", 0, 0, new AsciiModel(" ")),
        SWORD("zwaard", 2, 2, new AsciiModel("   /", "  / ", "-+- ", " |  ")),
        AXE("bijl", 3, 1, new AsciiModel(" /\\ ", "|##)", " |  ", " |  ")),
        DAGGER("dolk", 1, 1, new AsciiModel("  /", "-+ ", " | "));
        private final String label;
        private final int damage, range;
        private final AsciiModel model;
        Weapon(String label, int damage, int range, AsciiModel model) {
            this.label = label; this.damage = damage; this.range = range; this.model = model;
        }
        public String label() { return label; }
        public AsciiModel model() { return model; }
    }
    public enum MonsterType {
        SLIME("slijm", 2, 0x8ddc79, new AsciiModel(" .----. ", "/ o  o \\", "\\__ww__/")),
        GOBLIN("goblin", 3, 0xf0916c, new AsciiModel("\\^___^/", " (o.o) ", " /|#|\\ ", "  / \\  ")),
        SKELETON("skelet", 4, 0xdce8ee, new AsciiModel(" .---. ", " |o o| ", "  \\-/  ", " /|+|\\ ", "  / \\  "));
        private final String label;
        private final int health, color;
        private final AsciiModel model;
        MonsterType(String label, int health, int color, AsciiModel model) {
            this.label = label; this.health = health; this.color = color; this.model = model;
        }
        public String label() { return label; }
        public AsciiModel model() { return model; }
    }
    public record MonsterView(MonsterType type, int x, int y, int health) { }
    public record Pickup(Weapon weapon, int x, int y) { }
    private static final class Monster {
        final MonsterType type;
        final int x, y;
        int health;
        long hitUntil;
        Monster(MonsterType type, int x, int y) { this.type = type; this.x = x; this.y = y; health = type.health; }
    }
    public static final int PLAYER_WIDTH = 7, PLAYER_HEIGHT = 4;
    private static final int GOLD = 0xffd180, STEEL = 0x87d7ed;
    private static final AsciiModel CLOSED_CHEST = new AsciiModel(" .----. ", "/____/| ", "|_[]_|/ ");
    private static final AsciiModel OPEN_CHEST = new AsciiModel(" .----. ", " /    / ", "/____/| ", "|____|/ ");
    private static final AsciiModel SWING = new AsciiModel("--->");
    private int width, height;
    private final int chestX = 3, chestY;
    private final List<World.Rect> walls;
    private final List<Monster> monsters = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Weapon> inventory = new ArrayList<>();
    private int x = 3, y = 3, steps, selected, gold;
    private long lastMove, swingUntil, lastAttack = Long.MIN_VALUE;
    private boolean chestOpened;
    private String message = "Loop naar het zwaard en druk E om het op te pakken.";

    public AsciiLevel(int width, int height) {
        if (width < 60 || height < 14) throw new IllegalArgumentException("Level 1 vereist minstens 60x14 cellen.");
        this.width = width; this.height = height; chestY = height - 5;
        walls = List.of(new World.Rect(18, 3, 2, 3), new World.Rect(width - 20, height - 6, 3, 3));
        monsters.add(new Monster(MonsterType.SLIME, width / 3, 3));
        monsters.add(new Monster(MonsterType.GOBLIN, width * 2 / 3, 3));
        monsters.add(new Monster(MonsterType.SKELETON, width - 12, height - 7));
        pickups.add(new Pickup(Weapon.SWORD, 12, 2));
        pickups.add(new Pickup(Weapon.AXE, width / 2, height - 5));
    }
    public int width() { return width; }
    public int height() { return height; }
    public int playerX() { return x; }
    public int playerY() { return y; }
    public int gold() { return gold; }
    public int chestX() { return chestX; }
    public int chestY() { return chestY; }
    public boolean chestOpened() { return chestOpened; }
    public String message() { return message; }
    public Weapon weapon() { return inventory.isEmpty() ? Weapon.NONE : inventory.get(selected); }
    public List<Weapon> inventory() { return List.copyOf(inventory); }
    public List<Pickup> pickups() { return List.copyOf(pickups); }
    public List<MonsterView> monsters() {
        return monsters.stream().map(m -> new MonsterView(m.type, m.x, m.y, m.health)).toList();
    }
    public long monstersRemaining() { return monsters.stream().filter(m -> m.health > 0).count(); }
    boolean canPlace(int px, int py) {
        return px >= 1 && py >= 1 && px + PLAYER_WIDTH < width && py + PLAYER_HEIGHT < height
            && walls.stream().noneMatch(w -> w.overlaps(px, py, PLAYER_WIDTH, PLAYER_HEIGHT))
            && !new World.Rect(chestX, chestY, OPEN_CHEST.width(), OPEN_CHEST.height()).overlaps(px, py, PLAYER_WIDTH, PLAYER_HEIGHT)
            && monsters.stream().noneMatch(m -> m.health > 0 && new World.Rect(m.x, m.y, m.type.model.width(), m.type.model.height())
                .overlaps(px, py, PLAYER_WIDTH, PLAYER_HEIGHT));
    }
    public boolean move(int dx, int dy, long now) {
        var step = Movement.resolve(x, y, dx, dy, this::canPlace);
        if (!step.moved()) return false;
        x += step.dx(); y += step.dy(); steps++; lastMove = now;
        return true;
    }
    private int distance(int ox, int oy, int ow, int oh) {
        return Math.max(0, Math.max(ox - (x + PLAYER_WIDTH), x - (ox + ow)))
            + Math.max(0, Math.max(oy - (y + PLAYER_HEIGHT), y - (oy + oh)));
    }
    public void interact() {
        Pickup nearest = null;
        int best = Integer.MAX_VALUE;
        for (Pickup pickup : pickups) {
            int distance = distance(pickup.x, pickup.y, pickup.weapon.model.width(), pickup.weapon.model.height());
            if (distance < best) { best = distance; nearest = pickup; }
        }
        int chestDistance = distance(chestX, chestY, OPEN_CHEST.width(), OPEN_CHEST.height());
        if (!chestOpened && chestDistance <= 2 && chestDistance < best) {
            chestOpened = true; gold += 5; equip(Weapon.DAGGER);
            message = "Kist geopend: 5 goud en een dolk! F wisselt je wapens.";
        } else if (nearest != null && best <= 2) {
            pickups.remove(nearest); equip(nearest.weapon);
            message = "Je draagt nu de " + weapon().label + ". Spatie: aanval | F: ander wapen.";
        } else message = "Ga naast een wapen of gesloten kist staan en druk E.";
    }
    private void equip(Weapon weapon) {
        if (!inventory.contains(weapon)) inventory.add(weapon);
        selected = inventory.indexOf(weapon);
    }
    public void nextWeapon() {
        if (inventory.isEmpty()) { message = "Nog geen wapens. Pak het zwaard op met E."; return; }
        selected = (selected + 1) % inventory.size();
        message = "Uitgerust: " + weapon().label + ".";
    }
    public void attack(long now) {
        if (weapon() == Weapon.NONE) { message = "Pak eerst een wapen op met E."; return; }
        if (lastAttack != Long.MIN_VALUE && now - lastAttack < 200_000_000L) return;
        lastAttack = now; swingUntil = now + 120_000_000L;
        Monster target = null;
        int best = Integer.MAX_VALUE;
        for (Monster monster : monsters) if (monster.health > 0) {
            int gap = distance(monster.x, monster.y, monster.type.model.width(), monster.type.model.height());
            if (gap < best) { best = gap; target = monster; }
        }
        if (target == null || best > weapon().range) { message = "Je zwaait de " + weapon().label + ". Kom dichter bij een monster."; return; }
        target.health = Math.max(0, target.health - weapon().damage); target.hitUntil = now + 150_000_000L;
        if (target.health == 0) {
            gold += 2;
            message = target.type.label + " verslagen! +2 goud. Nog " + monstersRemaining() + " monsters.";
        } else message = target.type.label + " geraakt: " + target.health + " leven over.";
    }
    public Canvas draw(long now, boolean checker, boolean animate) {
        Canvas canvas = new Canvas(width, height);
        for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
            boolean edge = col == 0 || row == 0 || col == width - 1 || row == height - 1;
            int ground = checker && (col / 6 + row / 3) % 2 == 0 ? 0x233438 : 0x192a2f;
            canvas.put(col, row, new Cell(edge ? '#' : ' ', edge ? 0x6c898d : ground, ground));
        }
        for (World.Rect wall : walls) for (int row = wall.y(); row < wall.y() + wall.height(); row++)
            for (int col = wall.x(); col < wall.x() + wall.width(); col++) canvas.put(col, row, new Cell('#', 0xad9270, 0x4c4033));
        for (Monster monster : monsters) if (monster.health > 0) {
            monster.type.model.draw(canvas, monster.x, monster.y, now < monster.hitUntil ? 0xff6565 : monster.type.color);
            canvas.text(monster.x, monster.y - 1, monster.type.label, monster.type.color);
        }
        for (Pickup pickup : pickups) {
            pickup.weapon.model.draw(canvas, pickup.x, pickup.y, STEEL);
            canvas.text(pickup.x, pickup.y - 1, pickup.weapon.label, STEEL);
        }
        (chestOpened ? OPEN_CHEST : CLOSED_CHEST).draw(canvas, chestX, chestY, GOLD);
        canvas.text(chestX, chestY - 1, chestOpened ? "leeg" : "kist", GOLD);
        boolean walking = animate && now - lastMove < 180_000_000L;
        TextArt.ASCII.draw(canvas, x, y + 1, walking ? (steps / 2) % 2 : 0);
        if (weapon() != Weapon.NONE) {
            // A four-row blade beside a three-row player: visible, without dwarfing the figure.
            if (now < swingUntil) SWING.draw(canvas, x + 3, y + 2, STEEL);
            else weapon().model.draw(canvas, x + 3, y, STEEL);
        }
        return canvas;
    }
    Canvas arenaFrame(int columns, int rows, long now, boolean checker, boolean animate) {
        // Growing the terminal extends the floor; shrinking only changes the camera.
        width = Math.max(width, columns); height = Math.max(height, rows);
        int cameraX = Math.max(0, Math.min(width - columns, x + PLAYER_WIDTH / 2 - columns / 2));
        int cameraY = Math.max(0, Math.min(height - rows, y + PLAYER_HEIGHT / 2 - rows / 2));
        return draw(now, checker, animate).window(cameraX, cameraY, columns, rows);
    }
}
