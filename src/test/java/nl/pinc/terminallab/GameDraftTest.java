package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GameDraftTest {
    @Test void sourceCanBeEditedSavedAndResetWithoutExecution() {
        GameDraft draft = new GameDraft();
        String original = draft.source();
        assertFalse(draft.handle('x'));
        draft.edit(); draft.handle('\n'); draft.handle('q'); draft.handle('m');
        assertTrue(draft.source().endsWith("\nqm"));
        draft.handle(127); assertTrue(draft.source().endsWith("\nq"));
        draft.handle(19);
        assertFalse(draft.editing()); assertTrue(draft.saved());
        draft.edit(); draft.handle('x'); draft.handle(27);
        assertFalse(draft.saved()); assertFalse(draft.editing());
        draft.reset(); assertEquals(original, draft.source());
        assertFalse(draft.saved());
    }
    @Test void cursorMovementAndTabsEditTheExpectedLocationAndSourceIsBounded() {
        GameDraft draft = new GameDraft(); draft.edit();
        draft.handle(TerminalKeys.LEFT); draft.handle('x');
        assertTrue(draft.source().endsWith("x}"));
        draft.handle(TerminalKeys.RIGHT); draft.handle('\n'); draft.handle('\t'); draft.handle('y');
        draft.handle(TerminalKeys.UP); draft.handle(TerminalKeys.DOWN); draft.handle('z');
        assertTrue(draft.source().endsWith("    yz"));
        for (int i = 0; i < 6000; i++) draft.handle('a');
        assertEquals(4000, draft.source().length());
        assertDoesNotThrow(() -> draft.draw(new Canvas(64, 22), 27, 4, 36, 16));
    }
}
