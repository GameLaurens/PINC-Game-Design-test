package org.jline.terminal.impl.jni.win;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nl.pinc.terminallab.GameControls;
import org.jline.nativ.Kernel32;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.jni.JniTerminalProvider;
import org.jline.terminal.spi.SystemStream;

/**
 * JLine 3.30.17 adapter preserving native press/release events before character translation.
 * This package is intentional: the pinned JNI terminal exposes its constructor package-locally.
 * Only the normal console input queue is read; no global keyboard hook or polling is used.
 */
public final class GameWindowsTerminal extends NativeWinSysTerminal {
    private final GameControls controls;
    private GameWindowsTerminal(GameControls controls, long in, int inMode, long out, int outMode) throws IOException {
        super(new JniTerminalProvider(), SystemStream.Output, new NativeWinConsoleWriter(out),
            "GLYPH Engine", TYPE_WINDOWS_VTP, StandardCharsets.UTF_8, false, Terminal.SignalHandler.SIG_DFL,
            in, inMode, out, outMode);
        this.controls = controls;
    }
    public static GameWindowsTerminal open(GameControls controls) throws IOException {
        long in = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
        long out = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        int[] inMode = new int[1], outMode = new int[1];
        if (Kernel32.GetConsoleMode(in, inMode) == 0 || Kernel32.GetConsoleMode(out, outMode) == 0) return null;
        if (Kernel32.SetConsoleMode(out, outMode[0] | ENABLE_VIRTUAL_TERMINAL_PROCESSING) == 0) return null;
        try {
            // The constructor starts paused. TerminalSession resumes after raw mode is configured.
            return new GameWindowsTerminal(controls, in, inMode[0], out, outMode[0]);
        } catch (IOException | RuntimeException | LinkageError e) {
            Kernel32.SetConsoleMode(in, inMode[0]); Kernel32.SetConsoleMode(out, outMode[0]);
            throw e;
        }
    }
    @Override protected void processKeyEvent(boolean pressed, short virtualKey, char ch, int modifiers) throws IOException {
        controls.keyEvent(virtualKey, pressed, modifiers);
        super.processKeyEvent(pressed, virtualKey, ch, modifiers);
    }
    @Override protected boolean processConsoleInput() throws IOException {
        if (Kernel32.WaitForSingleObject(inConsole, 100) != 0) return false;
        boolean flush = false;
        for (var event : Kernel32.readConsoleInputHelper(inConsole, 1, false)) {
            if (event.eventType == Kernel32.INPUT_RECORD.KEY_EVENT) {
                var key = event.keyEvent;
                processKeyEvent(key.keyDown, key.keyCode, key.uchar, key.controlKeyState);
                flush = true;
            } else if (event.eventType == Kernel32.INPUT_RECORD.FOCUS_EVENT) {
                controls.focus(event.focusEvent.setFocus);
            } else if (event.eventType == Kernel32.INPUT_RECORD.WINDOW_BUFFER_SIZE_EVENT) raise(Terminal.Signal.WINCH);
        }
        return flush;
    }
}
