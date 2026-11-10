package nl.pinc.terminallab;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.jar.JarFile;

/** Smoke-test the actual launch archive, without opening or changing the terminal. */
final class BuildCheck {
    private BuildCheck() { }

    static int verify() throws IOException, ReflectiveOperationException, URISyntaxException {
        Path location = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (location.toString().endsWith(".jar")) {
            try (JarFile jar = new JarFile(location.toFile())) {
                for (var entry : jar.stream().filter(e -> e.getName().startsWith("nl/pinc/terminallab/")
                    && e.getName().endsWith(".class")).toList()) {
                    String name = entry.getName().replace('/', '.').replaceFirst("\\.class$", "");
                    Class.forName(name, false, Main.class.getClassLoader()).getDeclaredClasses();
                }
            }
        }
        Main app = new Main(null, "buildcontrole", 16);
        long now = 20_000_000_000L;
        for (int mode = 1; mode <= GameMenu.GAMES.length; mode++) {
            app.openDemo(mode);
            app.frame(140, 40, now, "");
            app.handle('e');
            app.handle(TerminalKeys.RIGHT);
            app.demo(100, 30, now, "");
            app.frame(64, 22, now, "");
        }
        return GameMenu.GAMES.length;
    }
}
