package nl.pinc.terminallab;

import java.util.List;

/** Assemble CSI/SS3 keys across reads; an arrow must never be mistaken for Escape + WASD. */
public final class TerminalKeys {
    public static final int UP = 0x110001, DOWN = 0x110002, RIGHT = 0x110003, LEFT = 0x110004;
    private final StringBuilder escape = new StringBuilder();
    private long started;
    public List<Integer> accept(int key, long now) {
        if (escape.isEmpty()) {
            if (key != 27) return List.of(key);
            escape.append((char) key); started = now; return List.of();
        }
        escape.append((char) key);
        if (escape.length() == 2) {
            if (key == '[' || key == 'O') return List.of();
            escape.setLength(0);
            if (key == 27) { escape.append((char) key); started = now; return List.of(27); }
            return List.of(27, key);
        }
        if (escape.length() > 32) { escape.setLength(0); return List.of(); }
        if (key >= 0x40 && key <= 0x7e) {
            escape.setLength(0);
            return switch (key) {
                case 'A' -> List.of(UP);
                case 'B' -> List.of(DOWN);
                case 'C' -> List.of(RIGHT);
                case 'D' -> List.of(LEFT);
                default -> List.of();
            };
        }
        return List.of();
    }
    public List<Integer> expire(long now) {
        if (escape.isEmpty() || now - started < 80_000_000L) return List.of();
        boolean loneEscape = escape.length() == 1;
        escape.setLength(0);
        return loneEscape ? List.of(27) : List.of();
    }
}
