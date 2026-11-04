package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Local, deterministic FFA simulation. All fighters share this world; non-local players are bots. */
public final class FreeForAll {
    public static final int WIDTH = 240, HEIGHT = 100;
    private static final long SECOND = 1_000_000_000L, TICK = 100_000_000L;
    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);
        final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }
    public enum Upgrade {
        HP("Max HP"), HEAL("Genezing"), SPEED("Bewegen"), DAMAGE("Schade");
        final String label;
        Upgrade(String label) { this.label = label; }
    }
    public enum Kind {
        FIGHTER("speler", 30, 5, 0, 0x83c8ff, null),
        SLIME("slijm", 10, 2, 10, 0x8ddc79, AsciiLevel.MonsterType.SLIME.model()),
        GOBLIN("goblin", 18, 4, 20, 0xf0916c, AsciiLevel.MonsterType.GOBLIN.model()),
        SKELETON("skelet", 28, 6, 35, 0xdce8ee, AsciiLevel.MonsterType.SKELETON.model()),
        OGRE("ogre", 45, 9, 60, 0xc79fe6, new AsciiModel("  /^^^^\\  ", " ( O  O ) ", " /| WW |\\ ", "/ |####| \\", "  /    \\  ", " _|    |_ "));
        final String label;
        final int baseHp, baseDamage, xp, color;
        final AsciiModel model;
        Kind(String label, int hp, int damage, int xp, int color, AsciiModel model) {
            this.label = label; baseHp = hp; baseDamage = damage; this.xp = xp; this.color = color; this.model = model;
        }
        public int xp() { return xp; }
    }
    public static final class Actor {
        final Kind kind;
        final String name;
        final boolean bot;
        final int homeX, homeY;
        final int[] upgrades = new int[Upgrade.values().length];
        int x, y, hp, kills, deaths, steps, shoppingStep;
        long totalXp, spentXp, nextMove, nextAttack, nextHeal, lastHit, lastMove, hitUntil, swingUntil, shieldUntil, respawnAt, nextShop;
        long lostTotal, transferredXp;
        Direction facing = Direction.RIGHT;
        Actor(Kind kind, String name, boolean bot, int x, int y, long now) {
            this.kind = kind; this.name = name; this.bot = bot; this.x = homeX = x; this.y = homeY = y;
            hp = kind.baseHp; nextHeal = now + 2 * SECOND;
            nextMove = nextAttack = now; shieldUntil = kind == Kind.FIGHTER ? now + 3 * SECOND : now;
        }
        public Kind kind() { return kind; }
        public String name() { return name; }
        public boolean bot() { return bot; }
        public boolean alive() { return hp > 0; }
        public boolean fighter() { return kind == Kind.FIGHTER; }
        public int x() { return x; }
        public int y() { return y; }
        public int hp() { return hp; }
        public int maxHp() { return kind.baseHp + (fighter() ? 10 * level(Upgrade.HP) : 0); }
        public int damage() { return kind.baseDamage + (fighter() ? 3 * level(Upgrade.DAMAGE) : 0); }
        public int level(Upgrade upgrade) { return upgrades[upgrade.ordinal()]; }
        public long totalXp() { return totalXp; }
        public long availableXp() { return totalXp - spentXp; }
        public int kills() { return kills; }
        public int deaths() { return deaths; }
        public int width() { return fighter() ? 7 : kind.model.width(); }
        public int height() { return fighter() ? 4 : kind.model.height(); }
        public long moveDelay() { return fighter() ? (140L - 20L * level(Upgrade.SPEED)) * 1_000_000L : 300_000_000L; }
        public long healDelay() { return 2_000_000_000L / (1 + level(Upgrade.HEAL)); }
        public int upgradeCost(Upgrade upgrade) { return 10 * (level(upgrade) + 1); }
    }
    public record Camera(int x, int y, int width, int height) { }
    private final List<Actor> actors = new ArrayList<>();
    private final List<World.Rect> walls = List.of(
        new World.Rect(72, 10, 3, 12), new World.Rect(72, 34, 3, 14),
        new World.Rect(95, 10, 3, 14), new World.Rect(95, 38, 3, 10),
        new World.Rect(72, 10, 26, 2), new World.Rect(72, 46, 26, 2),
        new World.Rect(45, 43, 16, 3), new World.Rect(140, 22, 4, 16),
        new World.Rect(175, 60, 20, 3), new World.Rect(115, 77, 16, 3),
        new World.Rect(205, 22, 3, 16), new World.Rect(30, 77, 12, 3));
    private final Random random = new Random(4917);
    private final Actor player;
    private long nextTick;
    private String message = "Jaag op slijm (10 XP). Pijlen vallen aan; U opent upgrades.";

    public FreeForAll(long now) { this(now, true); }
    FreeForAll(long now, boolean populate) {
        player = addFighter("JIJ", false, 12, 12, 0, now);
        nextTick = now;
        if (populate) {
            for (int i = 0; i < 20; i++) {
                Kind kind = Kind.values()[1 + i % 4];
                int px = i == 0 ? 32 : 30 + (i % 5) * 43;
                addMonster(kind, px, 12 + (i / 5) * 22, now);
            }
            addFighter("Rook", true, 58, 14, 40, now);
            addFighter("Nyx", true, 120, 32, 75, now);
            addFighter("Bram", true, 180, 70, 100, now);
            addFighter("Iris", true, 30, 65, 25, now);
        }
    }
    Actor addFighter(String name, boolean bot, int x, int y, long xp, long now) {
        Actor actor = new Actor(Kind.FIGHTER, name, bot, x, y, now);
        actor.totalXp = xp; placeNear(actor, x, y); actors.add(actor); return actor;
    }
    Actor addMonster(Kind kind, int x, int y, long now) {
        if (kind == Kind.FIGHTER) throw new IllegalArgumentException("Gebruik addFighter.");
        Actor actor = new Actor(kind, kind.label, true, x, y, now);
        placeNear(actor, x, y); actors.add(actor); return actor;
    }
    public Actor player() { return player; }
    public List<Actor> actors() { return List.copyOf(actors); }
    public String message() { return message; }
    public String region() { return player.y < 50 ? (player.x < 110 ? "Mosveld / ruines" : "Oostelijke wildernis") : "Zuidelijke crypten"; }
    public List<Actor> ranking() {
        return actors.stream().filter(Actor::fighter).sorted(Comparator.comparingLong(Actor::totalXp).reversed()).toList();
    }
    boolean terrainFree(int x, int y, int w, int h) {
        return x >= 1 && y >= 1 && x + w < WIDTH && y + h < HEIGHT
            && walls.stream().noneMatch(wall -> wall.overlaps(x, y, w, h));
    }
    boolean canPlace(Actor actor, int x, int y) {
        return terrainFree(x, y, actor.width(), actor.height())
            && actors.stream().noneMatch(other -> other != actor && other.alive()
                && new World.Rect(other.x, other.y, other.width(), other.height()).overlaps(x, y, actor.width(), actor.height()));
    }
    public boolean move(Actor actor, Direction direction, long now) {
        if (!actor.alive() || now < actor.nextMove) return false;
        actor.facing = direction;
        if (!canPlace(actor, actor.x + direction.dx, actor.y + direction.dy)) return false;
        actor.x += direction.dx; actor.y += direction.dy; actor.nextMove = now + actor.moveDelay();
        actor.lastMove = now; actor.steps++;
        return true;
    }
    public boolean buy(Actor actor, Upgrade upgrade) {
        if (!actor.alive() || !actor.fighter()) return false;
        if (actor.level(upgrade) >= 5) { if (actor == player) message = "Deze upgrade is maximaal (niveau 5)."; return false; }
        int cost = actor.upgradeCost(upgrade);
        if (actor.availableXp() < cost) { if (actor == player) message = "Niet genoeg vrije XP: nodig " + cost + "."; return false; }
        actor.spentXp += cost; actor.upgrades[upgrade.ordinal()]++;
        if (upgrade == Upgrade.HP) actor.hp += 10;
        if (actor == player) message = upgrade.label + " gekocht voor " + cost + " XP. Totaal XP blijft meetellen voor PvP.";
        return true;
    }
    public void attack(Actor attacker, Direction direction, long now) {
        if (!attacker.alive() || now < attacker.nextAttack) return;
        attacker.facing = direction; attacker.nextAttack = now + (attacker.fighter() ? 350_000_000L : 950_000_000L);
        attacker.swingUntil = now + 150_000_000L;
        int reach = attacker.fighter() ? 4 : 2;
        World.Rect area = switch (direction) {
            case RIGHT -> new World.Rect(attacker.x + attacker.width(), attacker.y - 1, reach, attacker.height() + 2);
            case LEFT -> new World.Rect(attacker.x - reach, attacker.y - 1, reach, attacker.height() + 2);
            case UP -> new World.Rect(attacker.x - 1, attacker.y - reach, attacker.width() + 2, reach);
            case DOWN -> new World.Rect(attacker.x - 1, attacker.y + attacker.height(), attacker.width() + 2, reach);
        };
        Actor target = actors.stream().filter(a -> enemy(attacker, a) && now >= a.shieldUntil
            && area.overlaps(a.x, a.y, a.width(), a.height()) && clearLine(attacker, a))
            .min(Comparator.comparingInt(a -> distance(attacker, a))).orElse(null);
        if (target == null) return;
        target.hp = Math.max(0, target.hp - attacker.damage()); target.lastHit = now; target.hitUntil = now + 160_000_000L;
        target.nextHeal = now + 3 * SECOND;
        if (target.hp == 0) defeat(attacker, target, now);
        else if (attacker == player) message = target.name + " geraakt: " + target.hp + "/" + target.maxHp() + " HP.";
        else if (target == player) message = attacker.name + " raakt je voor " + attacker.damage() + " HP!";
    }
    private boolean enemy(Actor attacker, Actor other) {
        return other != attacker && other.alive() && (attacker.fighter() || other.fighter());
    }
    private int distance(Actor a, Actor b) {
        return Math.max(0, Math.max(b.x - a.x - a.width(), a.x - b.x - b.width()))
            + Math.max(0, Math.max(b.y - a.y - a.height(), a.y - b.y - b.height()));
    }
    private boolean clearLine(Actor a, Actor b) {
        int x = a.x + a.width() / 2, y = a.y + a.height() / 2;
        int endX = b.x + b.width() / 2, endY = b.y + b.height() / 2;
        int dx = Math.abs(endX - x), dy = -Math.abs(endY - y);
        int sx = x < endX ? 1 : -1, sy = y < endY ? 1 : -1, error = dx + dy;
        while (true) {
            if (!terrainFree(x, y, 1, 1)) return false;
            if (x == endX && y == endY) return true;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x += sx; }
            if (twice <= dx) { error += dx; y += sy; }
        }
    }
    private void defeat(Actor killer, Actor victim, long now) {
        killer.kills++; victim.deaths++;
        if (victim.fighter()) {
            long original = victim.totalXp;
            long share = (original / 10) * 7 + (original % 10) * 7 / 10;
            victim.lostTotal = original; victim.transferredXp = killer.fighter() ? share : 0;
            if (killer.fighter()) killer.totalXp += share;
            victim.totalXp = original - share; victim.spentXp = 0; Arrays.fill(victim.upgrades, 0);
            if (victim == player) message = "Verslagen door " + killer.name + ". Enter: respawn met " + victim.totalXp + " XP, zonder upgrades.";
            else if (killer == player) message = victim.name + " verslagen: +" + share + " XP (70% van " + original + ").";
        } else {
            if (killer.fighter()) killer.totalXp += victim.kind.xp;
            if (killer == player) message = victim.name + " verslagen! +" + victim.kind.xp + " XP. U: upgrades.";
        }
        victim.respawnAt = now + (victim.fighter() ? 5 : 12) * SECOND;
    }
    public void respawnPlayer(long now) {
        if (player.alive()) return;
        respawn(player, now); message = "Opnieuw in de arena: " + player.totalXp + " vrije XP en 3 seconden bescherming.";
    }
    private void respawn(Actor actor, long now) {
        int px = actor.homeX, py = actor.homeY;
        if (actor.fighter()) {
            // Try dispersed positions before falling back to the home position.
            for (int attempt = 0; attempt < 100; attempt++) {
                int cx = 4 + random.nextInt(WIDTH - 20), cy = 4 + random.nextInt(HEIGHT - 15);
                if (canPlace(actor, cx, cy) && actors.stream().noneMatch(a -> a != actor && a.alive()
                    && Math.abs(a.x - cx) + Math.abs(a.y - cy) < 18)) { px = cx; py = cy; break; }
            }
        }
        placeNear(actor, px, py); actor.hp = actor.maxHp();
        actor.nextMove = actor.nextAttack = now; actor.nextHeal = now + actor.healDelay();
        actor.shieldUntil = now + (actor.fighter() ? 3 * SECOND : SECOND); actor.lastHit = now; actor.swingUntil = 0;
    }
    private void placeNear(Actor actor, int x, int y) {
        if (canPlace(actor, x, y)) { actor.x = x; actor.y = y; return; }
        for (int radius = 1; radius < WIDTH; radius++) for (int dy = -radius; dy <= radius; dy++)
            for (int dx : new int[]{-radius, radius}) if (canPlace(actor, x + dx, y + dy)) {
                actor.x = x + dx; actor.y = y + dy; return;
            }
        throw new IllegalStateException("Geen vrije spawnpositie.");
    }
    public void tick(long now) {
        int catchup = 0;
        while (nextTick <= now && catchup++ < 5) { update(nextTick); nextTick += TICK; }
        if (nextTick <= now) nextTick = now + TICK;
    }
    private void update(long now) {
        for (Actor actor : actors) {
            if (!actor.alive()) {
                if (actor != player && now >= actor.respawnAt) respawn(actor, now);
                continue;
            }
            if (actor.fighter() && actor.hp < actor.maxHp() && now >= actor.nextHeal) {
                actor.hp++; actor.nextHeal = now + actor.healDelay();
            }
            if (actor == player) continue;
            if (actor.fighter() && now >= actor.nextShop) {
                buy(actor, Upgrade.values()[actor.shoppingStep++ % Upgrade.values().length]);
                actor.nextShop = now + 2 * SECOND;
            }
            Actor target = actors.stream().filter(a -> enemy(actor, a) && distance(actor, a) <= (actor.fighter() ? 24 : 12))
                .min(Comparator.comparingInt(a -> distance(actor, a))).orElse(null);
            if (target == null) {
                if (actor.fighter() && now >= actor.nextMove) {
                    move(actor, Direction.values()[random.nextInt(4)], now); actor.nextMove = now + 700_000_000L;
                }
                continue;
            }
            Direction horizontal = target.x + target.width() / 2 < actor.x + actor.width() / 2 ? Direction.LEFT : Direction.RIGHT;
            Direction vertical = target.y + target.height() / 2 < actor.y + actor.height() / 2 ? Direction.UP : Direction.DOWN;
            boolean preferHorizontal = Math.abs(target.x - actor.x) > 2 * Math.abs(target.y - actor.y);
            Direction first = preferHorizontal ? horizontal : vertical, second = preferHorizontal ? vertical : horizontal;
            attack(actor, first, now);
            if (!move(actor, first, now)) move(actor, second, now);
        }
    }
    public Camera camera(int columns, int rows) {
        int w = Math.max(1, columns), h = Math.max(1, rows);
        return new Camera(Math.max(0, Math.min(WIDTH - w, player.x + player.width() / 2 - w / 2)),
            Math.max(0, Math.min(HEIGHT - h, player.y + player.height() / 2 - h / 2)), w, h);
    }
    public Canvas draw(Camera camera, long now, boolean checker, boolean animate) {
        Canvas canvas = new Canvas(camera.width, camera.height);
        for (int row = 0; row < camera.height; row++) for (int col = 0; col < camera.width; col++) {
            int wx = camera.x + col, wy = camera.y + row;
            boolean solid = !terrainFree(wx, wy, 1, 1);
            int ground = wy >= 50 ? 0x282733 : wx >= 110 ? 0x302d23 : 0x192e2c;
            if (checker && (wx / 8 + wy / 4) % 2 == 0) ground += 0x060606;
            char glyph = solid ? '#' : (wx * 17 + wy * 31) % 47 == 0 ? '.' : ' ';
            canvas.put(col, row, new Cell(glyph, solid ? 0x8d9184 : 0x455851, ground));
        }
        for (Actor actor : actors) if (actor.alive()) {
            int ax = actor.x - camera.x, ay = actor.y - camera.y;
            if (ax + actor.width() < 0 || ay + actor.height() < 0 || ax >= camera.width || ay >= camera.height) continue;
            int color = now < actor.hitUntil ? 0xff6262 : actor == player ? 0xffffff : actor.kind.color;
            if (now < actor.shieldUntil && actor.fighter()) color = 0x77e8ff;
            if (actor.fighter()) {
                TextArt.ASCII.draw(canvas, ax, ay + 1, animate && now - actor.lastMove < 180_000_000L ? (actor.steps / 2) % 2 : 0);
                AsciiLevel.Weapon.SWORD.model().draw(canvas, ax + 3, ay, color);
            } else actor.kind.model.draw(canvas, ax, ay, color);
            canvas.text(ax, ay - 1, (actor.fighter() && actor.bot ? "[BOT] " : "") + actor.name + " " + actor.hp
                + (actor.fighter() ? "HP" : " /" + actor.kind.xp + "XP"), color);
            if (now < actor.swingUntil) {
                switch (actor.facing) {
                    case RIGHT -> canvas.text(ax + actor.width(), ay + 2, "--->", 0xffd180);
                    case LEFT -> canvas.text(ax - 4, ay + 2, "<---", 0xffd180);
                    case UP -> { for (int i = 1; i <= 3; i++) canvas.text(ax + 1, ay - i, i == 3 ? "^" : "|", 0xffd180); }
                    case DOWN -> { for (int i = 0; i < 3; i++) canvas.text(ax + 1, ay + actor.height() + i, i == 2 ? "v" : "|", 0xffd180); }
                }
            }
        }
        return canvas;
    }
}
