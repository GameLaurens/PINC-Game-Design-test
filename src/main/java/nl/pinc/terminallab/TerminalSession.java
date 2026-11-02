package nl.pinc.terminallab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** Restores raw input, colors, cursor and the original screen on all normal exit paths. */
public final class TerminalSession implements AutoCloseable {
    private final Terminal terminal;
    private final Attributes original;
    private final Thread shutdownHook;
    private boolean closed;
    private volatile boolean interrupted;
    public TerminalSession() throws IOException {
        terminal = TerminalBuilder.builder().system(true).dumb(false).encoding(StandardCharsets.UTF_8).build();
        // TERM=dumb explicitly forces JLine's line-based fallback, even with dumb(false).
        if (terminal.getType().startsWith("dumb") || terminal.getWidth() < 1 || terminal.getHeight() < 1) {
            terminal.close();
            throw new IOException("Geen interactieve ANSI-terminal gevonden. Controleer ook of TERM niet op 'dumb' staat.");
        }
        original = new Attributes(terminal.getAttributes());
        shutdownHook = new Thread(this::restore, "terminal-herstel");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            terminal.handle(Terminal.Signal.INT, signal -> interrupted = true);
            terminal.enterRawMode();
            // Read Ctrl+C as character 3 as well; ConPTY need not forward a native signal.
            Attributes raw = terminal.getAttributes();
            raw.setLocalFlag(Attributes.LocalFlag.ISIG, false);
            terminal.setAttributes(raw);
            terminal.writer().print("\033[?1049h\033[0m\033[?25l\033[2J\033[H");
            terminal.writer().flush();
        } catch (RuntimeException e) { close(); throw e; }
    }
    public Terminal terminal() { return terminal; }
    public boolean interrupted() { return interrupted; }
    private synchronized void restore() {
        if (closed) return;
        closed = true;
        try {
            terminal.writer().print("\033[0m\033[?25h\033[?1049l");
            terminal.writer().flush();
        } finally {
            try { terminal.setAttributes(original); }
            finally { try { terminal.close(); } catch (IOException ignored) { /* best effort at shutdown */ } }
        }
    }
    @Override public void close() {
        try { restore(); }
        finally {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); }
            catch (IllegalStateException ignored) { /* JVM shutdown is already in progress */ }
        }
    }
}
