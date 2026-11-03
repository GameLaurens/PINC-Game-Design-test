package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

class FreeForAllTest {
    private static final long NOW = 10_000_000_000L, SECOND = 1_000_000_000L;
    private void advance(FreeForAll world, long from, long duration) {
        for (long at = from; at <= from + duration; at += 100_000_000L) world.tick(at);
    }
    @Test void standardMonsterGivesTenXpAndSpentXpStillCountsTowardsTotal() {
        FreeForAll world = new FreeForAll(NOW, false);
        var player = world.player();
        var slime = world.addMonster(FreeForAll.Kind.SLIME, 20, 12, NOW);
        world.attack(player, FreeForAll.Direction.RIGHT, NOW);
        assertEquals(5, slime.hp());
        world.attack(player, FreeForAll.Direction.RIGHT, NOW + 400_000_000L);
        assertFalse(slime.alive()); assertEquals(10, player.totalXp());
        assertTrue(world.buy(player, FreeForAll.Upgrade.HP));
        assertEquals(40, player.maxHp()); assertEquals(40, player.hp());
        assertEquals(10, player.totalXp()); assertEquals(0, player.availableXp());
        assertFalse(world.buy(player, FreeForAll.Upgrade.DAMAGE));
        assertEquals(5, player.damage());
        assertEquals(20, FreeForAll.Kind.GOBLIN.xp());
        assertEquals(35, FreeForAll.Kind.SKELETON.xp());
        assertEquals(60, FreeForAll.Kind.OGRE.xp());
    }
    @Test void pvpSplitsTotalIncludingSpentXpAndRespawnRemovesAllUpgrades() {
        FreeForAll world = new FreeForAll(NOW, false);
        var player = world.player(); player.totalXp = 101;
        world.buy(player, FreeForAll.Upgrade.HP);
        world.buy(player, FreeForAll.Upgrade.DAMAGE);
        world.buy(player, FreeForAll.Upgrade.SPEED);
        assertEquals(71, player.availableXp());
        var killer = world.addFighter("Rivaal", true, 3, 12, 17, NOW);
        player.hp = 5; player.shieldUntil = 0;
        world.attack(killer, FreeForAll.Direction.RIGHT, NOW);
        assertFalse(player.alive()); assertEquals(31, player.totalXp());
        assertEquals(87, killer.totalXp()); assertEquals(118, killer.totalXp() + player.totalXp());
        assertEquals(31, player.availableXp());
        for (var upgrade : FreeForAll.Upgrade.values()) assertEquals(0, player.level(upgrade));
        world.respawnPlayer(NOW + SECOND);
        assertTrue(player.alive()); assertEquals(30, player.hp()); assertEquals(30, player.maxHp());
        assertEquals(31, player.availableXp()); assertEquals(1, player.deaths()); assertEquals(1, killer.kills());
        assertEquals(NOW + 4 * SECOND, player.shieldUntil);
        assertTrue(world.canPlace(player, player.x(), player.y()));
    }
    @Test void killingBotGivesTheHumanSeventyPercentAndSpawnProtectionBlocksDamage() {
        FreeForAll world = new FreeForAll(NOW, false);
        var player = world.player();
        var bot = world.addFighter("Doelwit", true, 20, 12, 100, NOW);
        bot.hp = 5;
        world.attack(player, FreeForAll.Direction.RIGHT, NOW);
        assertEquals(5, bot.hp()); assertEquals(0, player.totalXp());
        world.attack(player, FreeForAll.Direction.RIGHT, NOW + 4 * SECOND);
        assertFalse(bot.alive()); assertEquals(70, player.totalXp()); assertEquals(30, bot.totalXp());
    }
    @Test void arrowsDamageOnlyTheirDirectionAndWallsBlockMelee() {
        FreeForAll world = new FreeForAll(NOW, false);
        var above = world.addMonster(FreeForAll.Kind.SLIME, 12, 8, NOW);
        var right = world.addMonster(FreeForAll.Kind.SLIME, 20, 12, NOW);
        world.attack(world.player(), FreeForAll.Direction.UP, NOW);
        assertEquals(5, above.hp()); assertEquals(10, right.hp());
        world.attack(world.player(), FreeForAll.Direction.RIGHT, NOW + 1);
        assertEquals(10, right.hp(), "Een aanval heeft een cooldown");
        world.attack(world.player(), FreeForAll.Direction.RIGHT, NOW + SECOND);
        assertEquals(5, right.hp());
        FreeForAll ruins = new FreeForAll(NOW, false);
        ruins.player().x = 65; ruins.player().y = 18;
        var behindWall = ruins.addMonster(FreeForAll.Kind.SLIME, 75, 18, NOW);
        ruins.attack(ruins.player(), FreeForAll.Direction.RIGHT, NOW);
        assertEquals(10, behindWall.hp());
    }
    @Test void movementAndHealingUpgradesHaveAnActualSimulationEffectAndAreCapped() {
        FreeForAll normal = new FreeForAll(NOW, false), upgraded = new FreeForAll(NOW, false);
        upgraded.player().totalXp = 200;
        for (int i = 0; i < 5; i++) assertTrue(upgraded.buy(upgraded.player(), FreeForAll.Upgrade.SPEED));
        assertFalse(upgraded.buy(upgraded.player(), FreeForAll.Upgrade.SPEED));
        assertEquals(40_000_000L, upgraded.player().moveDelay());
        assertTrue(normal.move(normal.player(), FreeForAll.Direction.RIGHT, NOW));
        assertTrue(upgraded.move(upgraded.player(), FreeForAll.Direction.RIGHT, NOW));
        assertFalse(normal.move(normal.player(), FreeForAll.Direction.RIGHT, NOW + 50_000_000L));
        assertTrue(upgraded.move(upgraded.player(), FreeForAll.Direction.RIGHT, NOW + 50_000_000L));
        normal.player().hp = upgraded.player().hp = 10;
        assertTrue(upgraded.buy(upgraded.player(), FreeForAll.Upgrade.HEAL));
        assertTrue(upgraded.buy(upgraded.player(), FreeForAll.Upgrade.DAMAGE));
        assertEquals(8, upgraded.player().damage());
        advance(normal, NOW, 3 * SECOND); advance(upgraded, NOW, 3 * SECOND);
        assertEquals(11, normal.player().hp()); assertEquals(12, upgraded.player().hp());
    }
    @Test void deadMonstersRespawnAndDeadPlayerDoesNotHealBackToLife() {
        FreeForAll world = new FreeForAll(NOW, false);
        var monster = world.addMonster(FreeForAll.Kind.SLIME, 20, 12, NOW);
        monster.hp = 5;
        world.attack(world.player(), FreeForAll.Direction.RIGHT, NOW);
        assertFalse(monster.alive());
        world.player().hp = 0; world.player().respawnAt = NOW;
        advance(world, NOW, 13 * SECOND);
        assertTrue(monster.alive()); assertEquals(10, monster.hp());
        assertFalse(world.player().alive());
    }
    @Test void cameraScrollsClampsAndNeverShowsTheWholeMapEvenOnLargeScreens() {
        FreeForAll world = new FreeForAll(NOW, false);
        var start = world.camera(75, 15);
        world.player().x = 120; world.player().y = 55;
        var moved = world.camera(75, 15);
        assertTrue(moved.x() > start.x()); assertTrue(moved.y() > start.y());
        assertTrue(moved.x() <= 120 && moved.x() + moved.width() > 127);
        world.player().x = 232; world.player().y = 95;
        var edge = world.camera(1000, 1000);
        assertEquals(100, edge.width()); assertEquals(28, edge.height());
        assertEquals(FreeForAll.WIDTH, edge.x() + edge.width());
        assertEquals(FreeForAll.HEIGHT, edge.y() + edge.height());
    }
    @Test void terrainHasRoutesAcrossTheWorldAndWorldModelsUseOnlyAscii() {
        FreeForAll world = new FreeForAll(NOW, false);
        boolean[][] seen = new boolean[FreeForAll.HEIGHT][FreeForAll.WIDTH];
        ArrayDeque<int[]> queue = new ArrayDeque<>(); queue.add(new int[]{12, 12}); seen[12][12] = true;
        while (!queue.isEmpty()) {
            int[] p = queue.removeFirst();
            for (var direction : FreeForAll.Direction.values()) {
                int x = p[0] + direction.dx, y = p[1] + direction.dy;
                if (x >= 0 && y >= 0 && x < FreeForAll.WIDTH && y < FreeForAll.HEIGHT && !seen[y][x] && world.terrainFree(x, y, 7, 4)) {
                    seen[y][x] = true; queue.add(new int[]{x, y});
                }
            }
        }
        assertTrue(seen[1][1]); assertTrue(seen[95][232]); assertTrue(seen[95][1]); assertTrue(seen[1][232]);
        Canvas image = world.draw(world.camera(75, 15), NOW, true, true);
        for (int y = 0; y < image.height(); y++) for (int x = 0; x < image.width(); x++)
            assertTrue(image.get(x, y).glyph() >= 32 && image.get(x, y).glyph() <= 126);
    }
    @Test void botsFightBuyUpgradesAndSimulationKeepsValidStatsDuringLongRun() {
        FreeForAll world = new FreeForAll(NOW);
        for (long at = NOW; at <= NOW + 90 * SECOND; at += 100_000_000L) {
            world.tick(at);
            for (var actor : world.actors()) {
                assertTrue(actor.hp() >= 0 && actor.hp() <= actor.maxHp());
                assertTrue(actor.availableXp() >= 0);
                if (actor.alive()) assertTrue(world.canPlace(actor, actor.x(), actor.y()), actor.name());
            }
        }
        assertTrue(world.actors().stream().filter(a -> a.fighter() && a.bot()).mapToInt(FreeForAll.Actor::kills).sum() > 0);
        assertTrue(world.actors().stream().filter(a -> a.fighter() && a.bot()).anyMatch(a -> a.spentXp > 0));
    }
    @Test void upgradeMenuKeysDoNotChangeDemoAndResizePreservesProgress() {
        FreeForAll world = new FreeForAll(NOW, false); world.player().totalXp = 40;
        FreeForAllView view = new FreeForAllView(world);
        assertTrue(view.handle('u', NOW)); assertTrue(view.handle('1', NOW));
        assertEquals(1, world.player().level(FreeForAll.Upgrade.HP));
        view.frame(1, 1, NOW, false, false);
        Canvas frame = view.frame(79, 23, NOW, false, false);
        assertTrue(rows(frame).contains("UPGRADES")); assertEquals(30, world.player().availableXp());
        view.handle('u', NOW); view.handle('\t', NOW);
        assertTrue(rows(view.frame(79, 23, NOW, false, false)).contains("RANGLIJST"));
        assertTrue(view.handle(TerminalKeys.LEFT, NOW));
        Main app;
        try { app = new Main(null, "test", 16); } catch (Exception e) { throw new AssertionError(e); }
        app.handle('2'); app.demo(79, 23, NOW, ""); app.handle('u'); app.handle('1');
        assertTrue(rows(app.demo(79, 23, NOW, "")).contains("FREE FOR ALL"));
    }
    private String rows(Canvas canvas) {
        StringBuilder out = new StringBuilder();
        for (int y = 0; y < canvas.height(); y++) for (int x = 0; x < canvas.width(); x++) out.append(canvas.get(x, y).glyph());
        return out.toString();
    }
}
