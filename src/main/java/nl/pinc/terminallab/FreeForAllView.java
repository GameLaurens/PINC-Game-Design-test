package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.List;

/** HUD and overlays for demo 2. Viewport resizing never replaces the simulation. */
public final class FreeForAllView {
    private final FreeForAll world;
    private boolean shop, scoreboard;
    public FreeForAllView(long now) { world = new FreeForAll(now); }
    FreeForAllView(FreeForAll world) { this.world = world; }
    public FreeForAll world() { return world; }
    public boolean handle(int key, long now) {
        FreeForAll.Direction attack = switch (key) {
            case TerminalKeys.UP -> FreeForAll.Direction.UP;
            case TerminalKeys.DOWN -> FreeForAll.Direction.DOWN;
            case TerminalKeys.LEFT -> FreeForAll.Direction.LEFT;
            case TerminalKeys.RIGHT -> FreeForAll.Direction.RIGHT;
            default -> null;
        };
        if (attack != null) { world.attack(world.player(), attack, now); return true; }
        if (key >= '1' && key <= '4') {
            world.buy(world.player(), FreeForAll.Upgrade.values()[key - '1']); return true;
        }
        switch (Character.toLowerCase(key)) {
            case 'w' -> world.move(world.player(), FreeForAll.Direction.UP, now);
            case 's' -> world.move(world.player(), FreeForAll.Direction.DOWN, now);
            case 'a' -> world.move(world.player(), FreeForAll.Direction.LEFT, now);
            case 'd' -> world.move(world.player(), FreeForAll.Direction.RIGHT, now);
            case 'u' -> { shop = !shop; scoreboard = false; }
            case '\t' -> { scoreboard = !scoreboard; shop = false; }
            case '\r', '\n' -> { world.respawnPlayer(now); shop = scoreboard = false; }
            default -> { return false; }
        }
        return true;
    }
    /** The simulation owns every cell of the central viewport. */
    Canvas arenaFrame(int width, int height, long now, boolean checker, boolean animate) {
        if (width < 12 || height < 6) return MenuStyle.canvas(width, height);
        world.tick(now);
        var camera = world.camera(width, height);
        Canvas c = world.draw(camera, now, checker, animate);
        MenuStyle.text(c, 1, 0, width - 2, "FREE FOR ALL / " + world.region(), MenuStyle.INK);
        if (!world.player().alive()) {
            box(c, camera, List.of("JE BENT VERSLAGEN", "Startbonus: " + world.player().totalXp() + " XP", "ENTER: respawn | Esc: lobby"));
        } else if (scoreboard) {
            List<String> lines = new ArrayList<>(); lines.add("RANGLIJST / XP / KILLS");
            for (var fighter : world.ranking()) lines.add(fighter.name() + (fighter.bot() ? " [BOT]" : " [JIJ]") + " / " + fighter.totalXp() + " XP / " + fighter.kills());
            lines.add("Tab: sluiten"); box(c, camera, lines);
        } else if (shop) {
            List<String> lines = new ArrayList<>(); lines.add("UPGRADES / 1-4 ook direct bruikbaar");
            int i = 1;
            for (var upgrade : FreeForAll.Upgrade.values()) lines.add(i++ + " " + upgrade.label + " / " + world.player().level(upgrade) + "/5");
            lines.add("U: sluiten"); box(c, camera, lines);
        }
        return c;
    }
    public Canvas frame(int width, int height, long now, boolean checker, boolean animate) {
        Canvas canvas = new Canvas(width, height);
        if (width >= 64 && height >= 22) world.tick(now);
        FreeForAll.Actor player = world.player();
        int filled = Math.max(0, player.hp() * 10 / player.maxHp());
        canvas.text(2, 0, "FREE FOR ALL | HP " + player.hp() + "/" + player.maxHp() + " [" + "#".repeat(filled) + ".".repeat(10 - filled) + "]", 0x71e6bb);
        canvas.text(2, 1, "WASD lopen | Pijlen aanvallen | U upgrades | Tab ranglijst");
        canvas.text(2, 2, "Enter respawn | R nieuw spel | M menu | Q stop");
        if (width < 64 || height < 22) {
            canvas.text(2, 5, "Vergroot de terminal naar minstens 64x22 cellen.");
            canvas.text(2, 7, "Je wereld en XP blijven bewaard.");
            return canvas;
        }
        FreeForAll.Camera camera = world.camera(width - 4, height - 10);
        canvas.text(2, 3, world.region() + " | kaart 240x100 | camera " + camera.x() + "," + camera.y(), 0x8ca6b4);
        canvas.blit(world.draw(camera, now, checker, animate), 2, 4);
        canvas.text(2, height - 5, "XP totaal: " + player.totalXp() + " | Vrij: " + player.availableXp() + " | U: uitgeven", 0xffd180);
        canvas.text(2, height - 4, "HP+" + player.level(FreeForAll.Upgrade.HP) + " Genezing+" + player.level(FreeForAll.Upgrade.HEAL)
            + " Snelheid+" + player.level(FreeForAll.Upgrade.SPEED) + " Schade+" + player.level(FreeForAll.Upgrade.DAMAGE));
        canvas.text(2, height - 3, "Positie " + player.x() + "," + player.y() + " | Kills " + player.kills() + " | Dood " + player.deaths()
            + (player.alive() && now < player.shieldUntil ? " | Bescherming " + ((player.shieldUntil - now + 999_999_999L) / 1_000_000_000L) + "s" : ""));
        canvas.text(2, height - 2, world.message(), 0xffd180);
        canvas.text(2, height - 1, "Lokale demo: jij + 4 bots | Geen netwerkverbinding nodig", 0x8ca6b4);
        if (!player.alive()) {
            box(canvas, camera, List.of("JE BENT VERSLAGEN", "Totaal voor nederlaag: " + player.lostTotal + " XP",
                "Naar tegenstander: " + player.transferredXp + " XP", "Startbonus: " + player.totalXp() + " XP",
                "Alle gekochte upgrades zijn vervallen.", "ENTER: respawn | R: volledig nieuw spel"));
        } else if (shop) {
            List<String> lines = new ArrayList<>();
            lines.add("UPGRADES | vrije XP: " + player.availableXp());
            int index = 1;
            for (FreeForAll.Upgrade upgrade : FreeForAll.Upgrade.values()) {
                lines.add(index++ + "  " + upgrade.label + " [" + player.level(upgrade) + "/5]  "
                    + (player.level(upgrade) == 5 ? "MAX" : player.upgradeCost(upgrade) + " XP"));
            }
            lines.add("HP " + player.maxHp() + " | schade " + player.damage() + " | stap " + player.moveDelay() / 1_000_000 + "ms");
            lines.add("Genezing: 1 HP per " + player.healDelay() / 1_000_000 + "ms");
            lines.add("U: sluiten. De wereld loopt door!");
            box(canvas, camera, lines);
        } else if (scoreboard) {
            List<String> lines = new ArrayList<>();
            lines.add("RANGLIJST - totaal XP / kills / dood");
            for (FreeForAll.Actor fighter : world.ranking()) lines.add((fighter == player ? "> " : "  ") + fighter.name()
                + (fighter.bot() ? " [BOT]" : "") + ": " + fighter.totalXp() + " XP / " + fighter.kills() + " / " + fighter.deaths()
                + (fighter.alive() ? "" : " (dood)"));
            lines.add("Tab: sluiten. De wereld loopt door!");
            box(canvas, camera, lines);
        }
        return canvas;
    }
    private void box(Canvas canvas, FreeForAll.Camera camera, List<String> lines) {
        int width = Math.min(58, canvas.width()), height = Math.min(lines.size() + 2, canvas.height());
        int left = Math.max(0, (canvas.width() - width) / 2);
        int top = Math.max(0, (canvas.height() - height) / 2);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            boolean edge = y == 0 || y == height - 1 || x == 0 || x == width - 1;
            char ch = !edge ? ' ' : (y == 0 || y == height - 1) ? (x == 0 || x == width - 1 ? '+' : '-') : '|';
            canvas.put(left + x, top + y, new Cell(ch, 0x71e6bb, 0x101923));
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            canvas.text(left + 2, top + i + 1, line.substring(0, Math.max(0, Math.min(line.length(), width - 4))));
        }
    }
}
