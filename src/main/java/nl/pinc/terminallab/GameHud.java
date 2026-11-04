package nl.pinc.terminallab;

import java.util.Comparator;
import java.util.List;

/** Compact sidebar following the game's wireframe: health, XP/kills, skills, leaderboard. */
final class GameHud {
    static void draw(Canvas c, int w, int h, FreeForAll world, long otherKills) {
        MenuStyle.fill(c, 0, 0, w, h, MenuStyle.PANEL);
        FreeForAll.Actor p = world == null ? null : world.player();
        int inner = w - 2;
        MenuStyle.bar(c, 1, 1, inner, p == null ? 0 : p.hp(), p == null ? 0 : p.maxHp(),
            p == null ? "Health --" : "HP " + p.hp() + "/" + p.maxHp());
        String xp = p == null ? "--" : Long.toString(p.availableXp());
        String kills = p == null ? Long.toString(otherKills) : Integer.toString(p.kills());
        if (w >= 23) MenuStyle.text(c, 1, 3, inner, "XP: " + xp + "  Kills: " + kills, MenuStyle.INK);
        else {
            MenuStyle.text(c, 1, 3, inner, "XP: " + xp, MenuStyle.INK);
            MenuStyle.text(c, 1, 4, inner, "Kills: " + kills, MenuStyle.INK);
        }
        int upgrades = 0;
        if (p != null) for (var upgrade : FreeForAll.Upgrade.values()) upgrades += p.level(upgrade);
        MenuStyle.text(c, 1, 5, inner, (inner < 16 ? "Skills: " : "Skill upgrades: ") + (p == null ? "--" : upgrades), MenuStyle.MUTED);
        String[] labels = {"Max Health", "Health Regen", "Speed", "Strength"};
        int index = 0;
        for (var upgrade : FreeForAll.Upgrade.values()) {
            int row = 7 + index * 2, level = p == null ? 0 : p.level(upgrade);
            String cost = p == null ? "" : level == 5 ? " MAX" : " " + p.upgradeCost(upgrade) + "xp";
            MenuStyle.text(c, 1, row, inner, labels[index] + (inner >= labels[index].length() + cost.length() ? cost : ""), MenuStyle.INK);
            int barWidth = Math.max(5, w - 4);
            for (int slot = 0; slot < 5; slot++) {
                int start = slot * barWidth / 5, end = (slot + 1) * barWidth / 5;
                MenuStyle.text(c, 1 + start, row + 1, end - start,
                    (slot < level ? "\u2588" : "\u2591").repeat(end - start), slot < level ? MenuStyle.GREEN : MenuStyle.LINE);
                c.text(start + 1, row + 1, "|", MenuStyle.MUTED);
            }
            c.text(w - 2, row + 1, Integer.toString(++index), MenuStyle.AMBER);
        }
        if (p == null && h > 22) MenuStyle.text(c, 1, 16, inner, "Skills: FFA", MenuStyle.MUTED);
        int top = Math.max(17, h / 2 - 1);
        MenuStyle.text(c, 1, top, inner, "LEADERBOARD", MenuStyle.AMBER);
        List<FreeForAll.Actor> ranking = world == null ? List.of() : world.actors().stream()
            .filter(FreeForAll.Actor::fighter)
            .sorted(Comparator.comparingInt(FreeForAll.Actor::kills).reversed()
                .thenComparing(Comparator.comparingLong(FreeForAll.Actor::totalXp).reversed())).toList();
        int room = Math.max(0, h - top - (h >= 32 ? 6 : 2));
        for (int i = 0; i < Math.min(room, ranking.size()); i++) {
            var fighter = ranking.get(i);
            String score = fighter.kills() + " kills";
            MenuStyle.text(c, 1, top + 2 + i, Math.max(1, inner - score.length() - 1), fighter.name(),
                fighter == p ? MenuStyle.GREEN : MenuStyle.INK);
            MenuStyle.text(c, w - 1 - score.length(), top + 2 + i, score.length(), score, MenuStyle.MUTED);
        }
        if (world == null && room > 0) MenuStyle.text(c, 1, top + 2, inner, "JIJ / " + kills + " kills", MenuStyle.INK);
        if (h >= 32) {
            MenuStyle.text(c, 1, h - 4, inner, "T / Y  chat", MenuStyle.MUTED);
            MenuStyle.text(c, 1, h - 3, inner, "Esc    lobby", MenuStyle.MUTED);
            MenuStyle.text(c, 1, h - 2, inner, "M      menu", MenuStyle.MUTED);
        }
    }
    private GameHud() { }
}
