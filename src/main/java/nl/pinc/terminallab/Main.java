package nl.pinc.terminallab;

import java.io.IOException;
import java.nio.file.Path;
import org.jline.utils.NonBlockingReader;

public final class Main {
    private static final String[] MODES = {
        "ASCII - level 1: monsters, kist en wapens", "ASCII - gedetailleerd", "Unicode - vormen en blokken",
        "ANSI True Color - gekleurd poppetje", "PNG - transparante terminalpixels",
        "ANSI - een pixel per voorgrondblok", "ANSI - een pixel per achtergrond",
        "ANSI - twee verticale pixels per teken", "Vergelijk de drie ANSI-methodes",
        "Pedit5-stijl - amber avonturier met zwaard en schild"
    };
    private static final int[] SIZES = {8, 16, 32};
    private final Sprite[] examples = new Sprite[3];
    private final Sprite custom;
    private final String source;
    private int sizeIndex, mode, x = 2, y = 2, steps;
    private boolean checker = true, animate = true, running = true;
    private long lastMovement;
    private int sceneWidth, sceneHeight, actorWidth, actorHeight;
    private World world;
    private AsciiLevel asciiLevel;

    Main(Sprite custom, String source, int size) throws IOException {
        this.custom = custom; this.source = source;
        for (int i = 0; i < SIZES.length; i++) {
            examples[i] = Sprite.bundled(SIZES[i]);
            if (SIZES[i] == size) sizeIndex = i;
        }
    }

    public static void main(String[] args) {
        try {
            Path path = null; int size = 16;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> { help(); return; }
                    case "--png" -> {
                        if (++i == args.length) throw new IllegalArgumentException("Pad na --png ontbreekt.");
                        path = Path.of(args[i]);
                    }
                    case "--size" -> {
                        if (++i == args.length) throw new IllegalArgumentException("Formaat na --size ontbreekt.");
                        size = Integer.parseInt(args[i]);
                        if (size != 8 && size != 16 && size != 32) throw new IllegalArgumentException("Kies 8, 16 of 32.");
                    }
                    default -> throw new IllegalArgumentException("Onbekende optie: " + args[i]);
                }
            }
            Sprite sprite = path == null ? null : Sprite.load(path);
            new Main(sprite, path == null ? "meegeleverde held" : "eigen PNG (" + sprite.width() + "x" + sprite.height() + ")", size).run();
        } catch (Exception e) {
            System.err.println("Terminal Sprite Lab: " + e.getMessage());
            System.err.println("Start in een interactieve terminal (Windows Terminal/PowerShell), niet in een IDE-uitvoervenster.");
            System.exit(1);
        }
    }
    private static void help() {
        System.out.println("Terminal Sprite Lab | Java 17+ | ANSI + UTF-8");
        System.out.println("java --enable-native-access=ALL-UNNAMED -jar target/terminal-sprite-lab.jar [--png pad.png] [--size 8|16|32]");
        System.out.println("1-9: demo | 0: pedit5-stijl | WASD: bewegen | +/-: formaat | B: achtergrond | P: animatie | R: positie | M/Esc: menu | Q: stop");
        System.out.println("Level 1: E pakken/kist openen | F wapen wisselen | Spatie aanvallen | R level opnieuw");
    }
    private void run() throws IOException {
        try (TerminalSession session = new TerminalSession()) {
            var terminal = session.terminal();
            AnsiDisplay display = new AnsiDisplay(terminal.writer());
            long statsAt = System.nanoTime(), bytesAt = 0;
            String stats = "";
            while (running && !session.interrupted()) {
                // Leave the final column and row unused: never trigger terminal autowrap/scroll.
                int width = Math.max(1, Math.min(terminal.getWidth() - 1, 240));
                int height = Math.max(1, Math.min(terminal.getHeight() - 1, 100));
                long now = System.nanoTime();
                if (now - statsAt >= 1_000_000_000L) {
                    stats = "ANSI UTF-8 bytes/s: " + (long) ((display.totalBytes() - bytesAt) * 1_000_000_000.0 / (now - statsAt));
                    statsAt = now; bytesAt = display.totalBytes();
                }
                display.render(mode == 0 ? menu(width, height) : demo(width, height, now, stats));
                int key = terminal.reader().read(33);
                if (key == -1) break;
                if (key != NonBlockingReader.READ_EXPIRED) handle(key);
                // Bound work per frame, even when a terminal pastes a large amount of text.
                for (int count = 0; count < 16 && running; count++) {
                    int extra = terminal.reader().read(1);
                    if (extra == NonBlockingReader.READ_EXPIRED) break;
                    if (extra == -1) { running = false; break; }
                    handle(extra);
                }
            }
        }
    }
    private Canvas menu(int width, int height) {
        Canvas c = new Canvas(width, height);
        c.text(2, 1, "TERMINAL SPRITE LAB", 0x71e6bb);
        c.text(2, 2, "Tien speelbare demo's - geen grafisch venster");
        for (int i = 0; i < MODES.length; i++) c.text(2, 4 + i, (i == 9 ? "0" : Integer.toString(i + 1)) + "  " + MODES[i]);
        c.text(2, 14, "Kies 1-9 of 0 (pedit5), zonder Enter. Q sluit af.");
        c.text(2, 16, "WASD bewegen | +/- 8,16,32 px | B achtergrond");
        c.text(2, 17, "P animatie | R beginpositie | M/Esc menu | Q stop");
        c.text(2, 19, "PNG-bron: " + source);
        c.text(2, 20, "Formaat: " + SIZES[sizeIndex] + "x" + SIZES[sizeIndex] + " | Windows Terminal + monospace aanbevolen");
        c.text(2, 22, "Vergelijking 16px: 96x32 aanbevolen; 32px: 132x48.");
        return c;
    }
    private TextArt art() { return switch (mode) { case 1 -> TextArt.ASCII; case 2 -> TextArt.DETAIL; case 3 -> TextArt.UNICODE; default -> null; }; }
    private PixelRenderer renderer() {
        return switch (mode) {
            case 4, 6 -> PixelRenderer.FULL_BLOCK;
            case 7 -> PixelRenderer.BACKGROUND;
            default -> PixelRenderer.HALF_BLOCK;
        };
    }
    private Sprite currentSprite() {
        if (mode == 10) return PeditCharacter.create(SIZES[sizeIndex], 0);
        return custom != null && mode >= 5 ? custom.fit(SIZES[sizeIndex]) : examples[sizeIndex];
    }
    Canvas demo(int width, int height, long now, String stats) {
        if (mode == 1) return asciiLevel(width, height, now, stats);
        Canvas c = new Canvas(width, height);
        TextArt art = art();
        Sprite sprite = currentSprite();
        int panels = mode == 9 ? 3 : 1;
        int panelWidth = (width - 4) / panels;
        int sw = panelWidth - 1, sh = height - 8;
        int aw = art != null ? art.width() : sprite.width();
        int ah = art != null ? art.height() : mode == 9 ? sprite.height() : renderer().rows(sprite);
        c.text(2, 0, (mode == 10 ? 0 : mode) + "  " + MODES[mode - 1], mode == 10 ? PeditCharacter.AMBER : 0x71e6bb);
        c.text(2, 1, "WASD | +/- formaat | B grond | P animatie | R reset | M menu | Q stop");
        if (width < 64 || sw < aw + 8 || sh < ah + 5) {
            world = null;
            c.text(2, 4, "Vergroot de terminal of druk - voor een kleinere sprite.", 0xffcc77);
            c.text(2, 6, "Nodig: " + Math.max(64, (aw + 9) * panels + 4) + " kolommen x " + (ah + 14) + " rijen.");
            c.text(2, 8, "M: hoofdmenu | Q: afsluiten | formaat: " + SIZES[sizeIndex]);
            return c;
        }
        if (world == null || sw != sceneWidth || sh != sceneHeight || aw != actorWidth || ah != actorHeight) {
            world = new World(sw, sh, aw, ah);
            sceneWidth = sw; sceneHeight = sh; actorWidth = aw; actorHeight = ah;
            if (!world.canPlace(x, y, aw, ah)) { x = 2; y = 2; }
        }
        boolean moving = animate && now - lastMovement < 180_000_000L;
        int pose = moving ? (steps / 2) % 2 : 0;
        // An arbitrary external image is kept intact; only our own hero has leg poses.
        if (mode == 10) sprite = PeditCharacter.create(SIZES[sizeIndex], pose);
        else if (pose == 1 && !(custom != null && mode >= 5)) sprite = sprite.walking(pose);
        for (int i = 0; i < panels; i++) {
            PixelRenderer method = panels == 3 ? PixelRenderer.values()[i] : renderer();
            Canvas scene = world.draw(checker, mode == 10);
            if (art != null) art.draw(scene, x, y, pose);
            else method.draw(scene, sprite, x, y);
            int left = 2 + i * panelWidth;
            String label = art != null ? "Tekens, " + aw + "x" + ah + " cellen"
                : method.label() + " " + sprite.width() + "x" + method.rows(sprite) + " cellen";
            c.text(left, 3, label.substring(0, Math.min(label.length(), panelWidth - 1)));
            c.blit(scene, left, 4);
        }
        c.text(2, height - 3, "Sprite: " + (art != null ? "tekst (vast formaat)" : SIZES[sizeIndex] + "x" + SIZES[sizeIndex] + " px")
            + " | positie " + x + "," + y + " | animatie " + (animate ? "aan" : "uit"));
        c.text(2, height - 2, mode == 10 ? "Pedit5-stijl | amber lijnfiguur met zwaard en schild | B: grond"
            : mode >= 5 ? "PNG: " + source + " | transparantie op achtergrond" : "# en + zijn obstakels; het hele kader van het poppetje botst.");
        c.text(2, height - 1, stats + " | verschillenbuffer, maximaal circa 30 fps", 0x8ca6b4);
        return c;
    }
    private Canvas asciiLevel(int width, int height, long now, String stats) {
        Canvas c = new Canvas(width, height);
        c.text(2, 0, "LEVEL 1 - ASCII monsters en buit", 0x71e6bb);
        c.text(2, 1, "WASD bewegen | E pakken/openen | Spatie aanvallen");
        c.text(2, 2, "F wapen | B grond | P animatie | R reset | M menu | Q stop");
        if (asciiLevel == null && width >= 64 && height >= 22)
            asciiLevel = new AsciiLevel(Math.min(110, width - 4), Math.min(28, height - 8));
        if (asciiLevel == null || width < asciiLevel.width() + 4 || height < asciiLevel.height() + 8) {
            int neededWidth = asciiLevel == null ? 64 : asciiLevel.width() + 4;
            int neededHeight = asciiLevel == null ? 22 : asciiLevel.height() + 8;
            c.text(2, 5, "Vergroot het venster: dit level vereist " + neededWidth + "x" + neededHeight + " cellen.");
            if (asciiLevel != null) c.text(2, 7, "Voortgang bewaard. R maakt een nieuw level voor dit venster.");
            return c;
        }
        c.text(2, 3, "Slijm / goblin / skelet | Zwaard + bijl | Kist met buit", 0x8ca6b4);
        c.blit(asciiLevel.draw(now, checker, animate), 2, 4);
        c.text(2, height - 3, "Wapen: " + asciiLevel.weapon().label() + " | Goud: " + asciiLevel.gold()
            + " | Monsters: " + asciiLevel.monstersRemaining() + " | Wapens: " + asciiLevel.inventory().size());
        c.text(2, height - 2, asciiLevel.message(), 0xffd180);
        c.text(2, height - 1, stats + " | ASCII-tekens + ANSI-kleuren", 0x8ca6b4);
        return c;
    }
    void handle(int input) {
        int key = Character.toLowerCase(input);
        if (key == 'q' || key == 3 || key == 4) { running = false; return; }
        if (key >= '1' && key <= '9') { mode = key - '0'; reset(); return; }
        switch (key) {
            case 'e' -> { if (mode == 1 && asciiLevel != null) asciiLevel.interact(); }
            case 'f' -> { if (mode == 1 && asciiLevel != null) asciiLevel.nextWeapon(); }
            case ' ' -> { if (mode == 1 && asciiLevel != null) asciiLevel.attack(System.nanoTime()); }
            case '0' -> { mode = 10; checker = false; reset(); }
            case 'm', 27 -> { mode = 0; world = null; }
            case '+', '=' -> { if (mode != 1) { sizeIndex = Math.min(2, sizeIndex + 1); reset(); } }
            case '-', '_' -> { if (mode != 1) { sizeIndex = Math.max(0, sizeIndex - 1); reset(); } }
            case 'b' -> checker = !checker;
            case 'p' -> animate = !animate;
            case 'r' -> reset();
            case 'w' -> move(0, -1);
            case 'a' -> move(-1, 0);
            case 's' -> move(0, 1);
            case 'd' -> move(1, 0);
            default -> { }
        }
    }
    private void reset() { x = 2; y = 2; steps = 0; lastMovement = 0; world = null; asciiLevel = null; }
    private void move(int dx, int dy) {
        if (mode == 1) {
            if (asciiLevel != null) asciiLevel.move(dx, dy, System.nanoTime());
            return;
        }
        if (mode == 0 || world == null || !world.canPlace(x + dx, y + dy, actorWidth, actorHeight)) return;
        x += dx; y += dy; steps++; lastMovement = System.nanoTime();
    }
}
