package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnemyIndicatorsTest {
    private static final long NOW = 10_000_000_000L;
    private EnemyIndicators.Target monster(int x, int y) {
        return new EnemyIndicators.Target(x, y, 1, 1, false);
    }
    private boolean marker(Cell cell) {
        char glyph = cell.glyph();
        return (glyph == '\u25cf' || glyph == '\u25cb' || glyph == '+' || Character.isDigit(glyph))
            && (cell.foreground() == EnemyIndicators.MONSTER_COLOR
                || cell.foreground() == EnemyIndicators.PLAYER_COLOR || cell.foreground() == EnemyIndicators.MIXED_COLOR);
    }
    private int represented(Canvas c) {
        int total = 0;
        for (int y = 0; y < c.height(); y++) for (int x = 0; x < c.width(); x++) if (
            (x == 0 || x == c.width() - 1 || y == 0 || y == c.height() - 1) && marker(c.get(x, y))) {
            char glyph = c.get(x, y).glyph();
            total += Character.isDigit(glyph) ? glyph - '0' : 1;
        }
        return total;
    }

    @Test void cardinalAndDiagonalDirectionsLandOnTheCorrectBorder() {
        Canvas c = new Canvas(11, 9);
        EnemyIndicators.draw(c, 0, 0, 5, 4, List.of(
            monster(-20, 4), monster(30, 4), monster(5, -20), monster(5, 30), monster(25, 20)));
        for (int[] point : new int[][]{{0, 4}, {10, 4}, {5, 0}, {5, 8}, {10, 8}})
            assertEquals('\u25cf', c.get(point[0], point[1]).glyph());
        assertEquals(5, represented(c));
    }

    @Test void projectionUsesWorldCoordinatesAndTheActualPlayerPositionAtMapEdges() {
        Canvas c = new Canvas(21, 11);
        EnemyIndicators.draw(c, 100, 80, 102, 82, List.of(monster(200, 92)));
        assertEquals('\u25cf', c.get(20, 4).glyph(), "De speler staat hier linksboven, niet midden in het beeld");
        assertEquals(1, represented(c));
    }

    @Test void visibleAndPartiallyVisibleModelsDoNotReceiveDuplicateMarkers() {
        Canvas c = new Canvas(11, 9);
        EnemyIndicators.draw(c, 0, 0, 5, 4, List.of(monster(5, 4), monster(10, 8),
            new EnemyIndicators.Target(-2, 3, 3, 2, false), new EnemyIndicators.Target(3, -1, 3, 2, false)));
        assertEquals(0, represented(c));
        EnemyIndicators.draw(c, 0, 0, 5, 4, List.of(new EnemyIndicators.Target(-3, 3, 3, 2, false)));
        assertEquals(1, represented(c), "Een volledig verdwenen model krijgt wel een marker");
    }

    @Test void overlappingDirectionsCountAllOpponentsAndDistinguishMixedGroups() {
        Canvas c = new Canvas(11, 9);
        EnemyIndicators.draw(c, 0, 0, 5, 4, List.of(monster(20, 4), monster(30, 4)));
        assertEquals(new Cell('2', EnemyIndicators.MONSTER_COLOR, 0x101923), c.get(10, 4));
        EnemyIndicators.draw(c, 0, 0, 5, 4, List.of(monster(20, 4), new EnemyIndicators.Target(30, 4, 1, 1, true)));
        assertEquals(new Cell('2', EnemyIndicators.MIXED_COLOR, 0x101923), c.get(10, 4));
        List<EnemyIndicators.Target> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) many.add(monster(20 + i, 4));
        EnemyIndicators.draw(c, 0, 0, 5, 4, many);
        assertEquals('+', c.get(10, 4).glyph());
    }

    @Test void tinyViewportsAndEmptyTargetListsRemainSafeAndPreserveTheGround() {
        for (int[] size : new int[][]{{1, 1}, {1, 9}, {11, 1}, {0, 0}}) {
            Canvas c = new Canvas(size[0], size[1]);
            assertDoesNotThrow(() -> EnemyIndicators.draw(c, 0, 0, 0, 0,
                List.of(monster(-20, -20), monster(20, 20))));
        }
        Canvas c = new Canvas(11, 9);
        c.put(10, 4, new Cell('#', 0xffffff, 0x234567));
        EnemyIndicators.draw(c, 0, 0, 5, 4, List.of(monster(30, 4)));
        assertEquals(0x234567, c.get(10, 4).background());
        Canvas empty = new Canvas(11, 9);
        EnemyIndicators.draw(empty, 0, 0, 5, 4, List.of());
        assertEquals(new Cell(' ', Canvas.INK, 0x101923), empty.get(10, 4));
    }

    @Test void freeForAllTracksLivingMonstersAndRivalsButNeverThePlayerOrDeadActors() {
        FreeForAll world = new FreeForAll(NOW, false);
        world.player().x = 40; world.player().y = 30;
        var slime = world.addMonster(FreeForAll.Kind.SLIME, 100, 30, NOW);
        var bot = world.addFighter("Rivaal", true, 40, 2, 0, NOW);
        var camera = world.camera(21, 11);
        Canvas c = world.draw(camera, NOW, false, false);
        assertEquals(2, represented(c));
        assertEquals('\u25cb', c.get(10, 0).glyph());
        assertEquals(EnemyIndicators.PLAYER_COLOR, c.get(10, 0).foreground());
        slime.hp = 0;
        assertEquals(1, represented(world.draw(camera, NOW, false, false)));
        bot.hp = 0;
        assertEquals(0, represented(world.draw(camera, NOW, false, false)));
        slime.respawnAt = Long.MAX_VALUE;
        bot.respawnAt = NOW;
        world.tick(NOW);
        assertTrue(bot.alive());
        world.player().x = bot.x() < 120 ? 200 : 5;
        assertEquals(1, represented(world.draw(world.camera(21, 11), NOW, false, false)),
            "Een gerespawnde bot buiten beeld wordt weer gemarkeerd");
    }

    @Test void cameraMovementResizeAndReappearanceUpdateTheMarkersWithoutScreenClears() {
        FreeForAll world = new FreeForAll(NOW, false);
        world.player().x = 40; world.player().y = 30;
        var slime = world.addMonster(FreeForAll.Kind.SLIME, 100, 30, NOW);
        Canvas before = world.draw(world.camera(21, 11), NOW, false, false);
        slime.x = 5;
        Canvas after = world.draw(world.camera(21, 11), NOW, false, false);
        assertTrue(marker(before.get(20, 5))); assertFalse(marker(after.get(20, 5)));
        assertTrue(marker(after.get(0, 5)));
        StringWriter output = new StringWriter();
        AnsiDisplay display = new AnsiDisplay(new PrintWriter(output));
        display.render(before); output.getBuffer().setLength(0); display.render(after);
        assertFalse(output.toString().contains("\033[2J"));
        world.player().x = 2;
        assertEquals(0, represented(world.draw(world.camera(21, 11), NOW, false, false)), "In beeld verdwijnt de marker");
        slime.x = 100;
        assertEquals(1, represented(world.draw(world.camera(21, 11), NOW, false, false)));
        assertEquals(0, represented(world.draw(world.camera(140, 60), NOW, false, false)), "Groter beeld toont het model");
    }

    @Test void freeForAllTitleDoesNotEraseTopEdgeMarkers() {
        FreeForAll world = new FreeForAll(NOW, false);
        world.player().x = 40; world.player().y = 30;
        var bot = world.addFighter("Rivaal", true, 40, 2, 0, NOW);
        bot.nextMove = Long.MAX_VALUE; bot.nextAttack = Long.MAX_VALUE;
        Canvas c = new FreeForAllView(world).arenaFrame(21, 11, NOW, false, false);
        assertEquals('\u25cb', c.get(10, 0).glyph());
    }

    @Test void adventureAndBothShowroomsTrackMonstersButNotWeaponRacksOrChests() {
        AsciiLevel level = new AsciiLevel(60, 24);
        assertEquals(3, represented(level.arenaFrame(16, 12, NOW, false, false)));
        assertEquals(0, represented(level.arenaFrame(80, 30, NOW, false, false)));
        Showroom gallery = new Showroom(false);
        assertEquals(14, represented(gallery.arenaFrame(20, 12, NOW, false, false)));
        assertEquals(0, represented(gallery.arenaFrame(140, 60, NOW, false, false)));
        Showroom weapons = new Showroom(true);
        assertEquals(1, represented(weapons.arenaFrame(20, 12, NOW, false, false)));
        assertEquals(0, represented(weapons.arenaFrame(140, 60, NOW, false, false)));
    }
}
