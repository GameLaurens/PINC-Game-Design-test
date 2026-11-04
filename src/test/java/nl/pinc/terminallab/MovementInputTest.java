package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MovementInputTest {
    private static final long NOW = 20_000_000_000L;

    @Test void twoMovementKeysAndAnAttackRemainHeldTogetherUntilTheirOwnRelease() {
        GameControls keys = new GameControls();
        keys.keyEvent('W', true, 0); keys.keyEvent('D', true, 0); keys.keyEvent(39, true, 0);
        assertEquals(new GameControls.State(1, -1, TerminalKeys.RIGHT, false), keys.state());
        assertEquals(new GameControls.State(1, -1, TerminalKeys.RIGHT, false), keys.state());
        keys.keyEvent('D', false, 0);
        assertEquals(new GameControls.State(0, -1, TerminalKeys.RIGHT, false), keys.state());
        keys.keyEvent(39, false, 0);
        assertEquals(new GameControls.State(0, -1, 0, false), keys.state());
        keys.keyEvent('W', false, 0);
        assertEquals(new GameControls.State(0, 0, 0, false), keys.state());
    }
    @Test void allFourDiagonalCombinationsWorkAndOppositeMovementKeysCancel() {
        for (int[] pair : new int[][]{{'W','A',-1,-1}, {'W','D',1,-1}, {'S','A',-1,1}, {'S','D',1,1}}) {
            GameControls keys = new GameControls(); keys.keyEvent(pair[0], true, 0); keys.keyEvent(pair[1], true, 0);
            assertEquals(pair[2], keys.state().dx()); assertEquals(pair[3], keys.state().dy());
        }
        GameControls keys = new GameControls();
        for (char key : "WASD".toCharArray()) keys.keyEvent(key, true, 0);
        assertEquals(new GameControls.State(0, 0, 0, false), keys.state());
    }
    @Test void attacksAlwaysChooseOneCardinalDirectionAndIgnoreOsRepeatPriority() {
        GameControls keys = new GameControls();
        keys.keyEvent(38, true, 0); keys.keyEvent(39, true, 0);
        assertEquals(TerminalKeys.RIGHT, keys.state().attack());
        keys.keyEvent(38, true, 0); // repeat of older arrow must not change aim
        assertEquals(TerminalKeys.RIGHT, keys.state().attack());
        keys.keyEvent(39, false, 0);
        assertEquals(TerminalKeys.UP, keys.state().attack());
        assertEquals(4, FreeForAll.Direction.values().length);
    }
    @Test void shortTapsAreNotLostBetweenFramesButNeverStickAfterRelease() {
        GameControls keys = new GameControls();
        keys.keyEvent('W', true, 0); keys.keyEvent('W', false, 0);
        keys.keyEvent(37, true, 0); keys.keyEvent(37, false, 0);
        assertEquals(new GameControls.State(0, -1, TerminalKeys.LEFT, false), keys.state());
        assertEquals(new GameControls.State(0, 0, 0, false), keys.state());
    }
    @Test void menusChatFocusLossAndModifierKeysCannotLeaveMovementStuck() {
        GameControls keys = new GameControls();
        keys.keyEvent('W', true, 0); keys.suppressHeld(); keys.keyEvent('W', true, 0);
        assertEquals(0, keys.state().dy(), "Autorepeat mag een onderdrukte toets niet opnieuw activeren");
        keys.keyEvent('W', false, 0); keys.keyEvent('W', true, 0); assertEquals(-1, keys.state().dy());
        keys.focus(false); assertEquals(0, keys.state().dy());
        keys.keyEvent('W', true, 0); assertEquals(0, keys.state().dy());
        keys.focus(true); assertEquals(0, keys.state().dy());
        keys.keyEvent('D', true, 4); assertEquals(0, keys.state().dx(), "Ctrl+D mag geen loopactie zijn");
    }
    @Test void diagonalMovementDoesNotCutCornersAndSlidesAlongFreeWalls() {
        var slide = Movement.resolve(1, 1, 1, 1, (x, y) -> !(x == 2 && y == 1));
        assertEquals(new Movement.Step(0, 1), slide);
        var blocked = Movement.resolve(1, 1, 1, 1, (x, y) -> x == 2 && y == 2);
        assertFalse(blocked.moved(), "Een vrije diagonale cel geeft geen doorgang door twee muren");
        assertFalse(Movement.resolve(1, 1, 2, 0, (x, y) -> true).moved());
    }
    @Test void diagonalMovementAndCardinalAttacksUseIndependentCooldowns() {
        FreeForAll world = new FreeForAll(NOW, false);
        var p = world.player();
        var right = world.addMonster(FreeForAll.Kind.SLIME, 21, 11, NOW);
        var above = world.addMonster(FreeForAll.Kind.SLIME, 13, 7, NOW);
        assertTrue(world.move(p, 1, -1, NOW));
        assertEquals(13, p.x()); assertEquals(11, p.y());
        assertEquals(NOW + Math.round(p.moveDelay() * Math.sqrt(2)), p.nextMove);
        world.attack(p, FreeForAll.Direction.RIGHT, NOW);
        assertEquals(5, right.hp()); assertEquals(10, above.hp());
        assertFalse(world.move(p, 1, 1, NOW + 150_000_000L), "Diagonaal lopen mag niet sneller zijn");
        assertTrue(world.move(p, 1, 1, NOW + 200_000_000L), "Een aanval mag beweging niet blokkeren");
        world.attack(p, FreeForAll.Direction.RIGHT, NOW + 200_000_000L);
        assertEquals(5, right.hp(), "Bewegen mag de aanvalscooldown niet omzeilen");
        world.attack(p, FreeForAll.Direction.RIGHT, NOW + 400_000_000L);
        assertEquals(0, right.hp()); assertEquals(10, above.hp());
    }
    @Test void allTenDemosUseTheSameHeldDiagonalMovement() throws Exception {
        for (int mode = 1; mode <= 10; mode++) {
            Main app = new Main(null, "test", 16); GameControls keys = new GameControls();
            app.useControls(keys); app.openDemo(mode);
            Canvas before = app.frame(140, 40, NOW, "");
            keys.keyEvent('W', true, 0); keys.keyEvent('D', true, 0); keys.keyEvent(39, true, 0);
            app.updateControls(NOW + 1);
            Canvas after = app.frame(140, 40, NOW + 1, "");
            int changed = 0;
            GameLayout layout = GameLayout.of(140, 40);
            for (int y = 1; y < layout.fieldHeight(); y++)
                for (int x = layout.left(); x < 140 - layout.right(); x++)
                    if (!before.get(x, y).equals(after.get(x, y))) changed++;
            assertTrue(changed > 0, "Demo " + mode + " moet bewegen met ingedrukte toetsen");
        }
    }
    @Test void chatCapturesHeldKeysAndRequiresAFreshPressAfterClosing() throws Exception {
        Main app = new Main(null, "test", 16); GameControls keys = new GameControls();
        app.useControls(keys); app.openDemo(2); app.frame(140, 40, NOW, "");
        keys.keyEvent('D', true, 0); app.updateControls(NOW + 1);
        int before = playerLabel(app.frame(140, 40, NOW + 1, ""));
        app.handle('t'); app.handle('d'); keys.keyEvent('D', true, 0);
        app.updateControls(NOW + 500_000_000L);
        app.handle(27); app.updateControls(NOW + 1_000_000_000L);
        assertEquals(before, playerLabel(app.frame(140, 40, NOW + 1, "")));
        keys.keyEvent('D', false, 0); keys.keyEvent('D', true, 0);
        app.updateControls(NOW + 2_000_000_000L);
        assertEquals(before + 1, playerLabel(app.frame(140, 40, NOW + 1, "")));
    }
    private int playerLabel(Canvas c) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < c.height(); y++) for (int x = 0; x < c.width(); x++) text.append(c.get(x, y).glyph());
        int index = text.indexOf("JIJ 30HP"); assertTrue(index >= 0); return index;
    }
}
