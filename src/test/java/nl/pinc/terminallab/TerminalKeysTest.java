package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalKeysTest {
    private List<Integer> decode(String sequence) {
        TerminalKeys keys = new TerminalKeys(); List<Integer> out = new ArrayList<>();
        for (int i = 0; i < sequence.length(); i++) out.addAll(keys.accept(sequence.charAt(i), i * 1_000_000L));
        return out;
    }
    @Test void windowsCsiAndSs3ArrowsAndModifiersBecomeOneAttackKey() {
        assertEquals(List.of(TerminalKeys.UP), decode("\033[A"));
        assertEquals(List.of(TerminalKeys.DOWN), decode("\033OB"));
        assertEquals(List.of(TerminalKeys.RIGHT), decode("\033[1;5C"));
        assertEquals(List.of(TerminalKeys.LEFT), decode("\033[D"));
        assertEquals(List.of((int)'w', TerminalKeys.LEFT, (int)'d'), decode("w\033[Dd"));
    }
    @Test void onlyStandaloneEscapeReturnsToMenuAndUnknownSequencesDoNotMovePlayer() {
        TerminalKeys keys = new TerminalKeys();
        assertTrue(keys.accept(27, 0).isEmpty());
        assertTrue(keys.expire(40_000_000L).isEmpty());
        assertEquals(List.of(27), keys.expire(90_000_000L));
        assertTrue(decode("\033[3~").isEmpty());
        assertTrue(decode("\033[H").isEmpty());
        assertEquals(List.of(27, (int)'m'), decode("\033m"));
    }
}
