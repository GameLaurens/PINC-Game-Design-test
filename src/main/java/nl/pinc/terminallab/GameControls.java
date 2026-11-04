package nl.pinc.terminallab;

import java.util.Arrays;

/** Physical game-key state. Native input updates this; the game samples it once per frame. */
public final class GameControls {
    public record State(int dx, int dy, int attack, boolean space) { }
    private static final int[] KEYS = {'W', 'A', 'S', 'D', 37, 38, 39, 40, 32};
    private final boolean[] down = new boolean[KEYS.length], suppressed = new boolean[KEYS.length];
    private final boolean[] pending = new boolean[KEYS.length];
    private final long[] order = new long[KEYS.length];
    private long sequence;
    private boolean focused = true;

    public synchronized void keyEvent(int virtualKey, boolean pressed, int modifiers) {
        // Ctrl/Alt combinations belong to the terminal, menus or OS, never to movement.
        if ((modifiers & 15) != 0) suppressHeld();
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i] == virtualKey) {
            if (pressed && !down[i]) {
                order[i] = ++sequence;
                suppressed[i] = !focused || (modifiers & 15) != 0;
                pending[i] = !suppressed[i];
            }
            down[i] = pressed;
            if (!pressed) suppressed[i] = false;
            return;
        }
    }
    public synchronized void focus(boolean gained) {
        focused = gained;
        Arrays.fill(down, false); Arrays.fill(suppressed, false);
        Arrays.fill(pending, false);
    }
    public synchronized void suppressHeld() {
        for (int i = 0; i < down.length; i++) if (down[i]) suppressed[i] = true;
        Arrays.fill(pending, false);
    }
    private boolean active(int i) { return focused && (down[i] || pending[i]) && !suppressed[i]; }
    public synchronized State state() {
        int dx = (active(3) ? 1 : 0) - (active(1) ? 1 : 0);
        int dy = (active(2) ? 1 : 0) - (active(0) ? 1 : 0);
        int attack = 0;
        long newest = -1;
        for (int i = 4; i <= 7; i++) if (active(i) && order[i] > newest) {
            newest = order[i];
            attack = switch (KEYS[i]) {
                case 37 -> TerminalKeys.LEFT; case 38 -> TerminalKeys.UP;
                case 39 -> TerminalKeys.RIGHT; default -> TerminalKeys.DOWN;
            };
        }
        State result = new State(dx, dy, attack, active(8));
        // A quick tap must still act once even if press and release arrive between frames.
        Arrays.fill(pending, false);
        return result;
    }
    static boolean continuousKey(int key) {
        int lower = Character.toLowerCase(key);
        return lower == 'w' || lower == 'a' || lower == 's' || lower == 'd' || lower == ' '
            || key == TerminalKeys.LEFT || key == TerminalKeys.RIGHT || key == TerminalKeys.UP || key == TerminalKeys.DOWN;
    }
}
