package nl.pinc.terminallab;

import java.io.IOException;
import java.nio.file.Path;
import org.jline.utils.NonBlockingReader;

public final class Main {
    private static final String[] MODES = {
        "ASCII - level 1: monsters, kist en wapens", "Free for All - grote ASCII-wereld met bots", "Unicode - vormen en blokken",
        "Monstershowroom - 15 ASCII-monsters", "Wapenshowroom - oppakken en testen",
        "ANSI - een pixel per voorgrondblok", "ANSI - een pixel per achtergrond",
        "ANSI - twee verticale pixels per teken", "Vergelijk de drie ANSI-methodes",
        "Pedit5-stijl - amber avonturier met zwaard en schild", "Weapon Range Editor - teken je eigen schadegebied",
        "Unicode-monstershowroom - 15 monsters in UTF-8",
        "Open World Explorer - zes biomen en sloopbare objecten",
        "Small ASCII Gallery - monsters van 1/3 tot 5/3 van de speler"
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
    private FreeForAllView freeForAll;
    private Showroom showroom;
    private WeaponRangeLab weaponRange;
    private OpenWorld openWorld;
    private final GameMenu menus = new GameMenu();
    private GameChat chat = new GameChat();
    private GameControls realtimeControls;
    private long nextControlMove;

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
                    case "--check-build" -> {
                        System.out.println("Buildcontrole geslaagd: " + BuildCheck.verify() + " demo's, inclusief Monster Gallery 4.");
                        return;
                    }
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
            System.err.println("GLYPH Engine: " + e.getMessage());
            System.err.println("Start in een interactieve terminal (Windows Terminal/PowerShell), niet in een IDE-uitvoervenster.");
            System.exit(1);
        }
    }
    private static void help() {
        System.out.println("GLYPH ENGINE | Terminal Sprite Lab | Java 25+ | ANSI + UTF-8");
        System.out.println("java --enable-native-access=ALL-UNNAMED -jar target/terminal-sprite-lab.jar [--png pad.png] [--size 8|16|32]");
        System.out.println("--check-build: controleer alle demo's zonder een interactieve terminal te openen");
        System.out.println("Startscherm: elke toets | Menu's: WASD/pijlen + Enter | Esc: terug");
        System.out.println("Hoofdmenu: Agent configureren | Lobby: T/Y chat of Chat openen | Enter versturen | Esc annuleren");
        System.out.println("Homepage: Game aanmaken | Game save laden | Deelnemen | Agent configureren | Zelf game maken");
        System.out.println("Alleen de host start een game. Gasten kunnen een toegankelijke game met status In Game binnengaan.");
        System.out.println("Spel: T global chat | Y teamchat | Esc lobby | M hoofdmenu | Q stop | Ctrl+C/D altijd afsluiten");
        System.out.println("WASD vasthouden/combineren: ook diagonaal lopen | Pijlen: tegelijk recht aanvallen (Windows Terminal)");
        System.out.println("+/-: formaat | B: achtergrond | P: animatie | R: opnieuw");
        System.out.println("Level 1: E pakken/kist openen | F wapen wisselen | Spatie aanvallen | R level opnieuw");
        System.out.println("Demo 2 FFA: WASD lopen | pijlen aanvallen | 1-4 direct upgraden | U overzicht | Tab ranglijst | Enter respawn");
        System.out.println("Lokale UI-demo: games/chat zijn lokaal, saves zijn nog niet aangesloten, conceptcode wordt niet uitgevoerd.");
        System.out.println("Demo 4: 15 ASCII-monsters | Demo 5: E wapens pakken, F wisselen, pijlen/Spatie aanvallen, V bereik");
        System.out.println("Demo 11 Weapon Range Editor: pijlen cursor | Enter vakje aan/uit | Spatie test | C wis | R reset");
        System.out.println("Demo 12 Unicode Monster Gallery: 15 Unicode-monsters | WASD lopen | B grond | P animatie | R reset");
        System.out.println("Demo 13 Open World Explorer: WASD vlot lopen | F sprint | E/Spatie gebruiken en slopen | Tab kaart | 1-6 snelreizen op kaart");
        System.out.println("Demo 14 Small ASCII Gallery: speler 3x3 | 15 ASCII-monsters van 1x1 t/m 5x5 | WASD lopen | R reset");
    }
    private void run() throws IOException {
        try (TerminalSession session = new TerminalSession()) {
            useControls(session.simultaneousInput() ? session.controls() : null);
            var terminal = session.terminal();
            AnsiDisplay display = new AnsiDisplay(terminal.writer());
            long statsAt = System.nanoTime(), bytesAt = 0;
            String stats = "";
            TerminalKeys keys = new TerminalKeys();
            while (running && !session.interrupted()) {
                for (int expired : keys.expire(System.nanoTime())) handle(expired);
                // Leave the final column and row unused: never trigger terminal autowrap/scroll.
                int width = Math.max(1, terminal.getWidth() - 1);
                int height = Math.max(1, terminal.getHeight() - 1);
                long now = System.nanoTime();
                if (now - statsAt >= 1_000_000_000L) {
                    stats = "ANSI UTF-8 bytes/s: " + (long) ((display.totalBytes() - bytesAt) * 1_000_000_000.0 / (now - statsAt));
                    statsAt = now; bytesAt = display.totalBytes();
                }
                updateControls(now);
                display.render(frame(width, height, now, stats));
                int key = terminal.reader().read(33);
                if (key == -1) break;
                if (key != NonBlockingReader.READ_EXPIRED) for (int decoded : keys.accept(key, System.nanoTime())) handle(decoded);
                // Bound work per frame, even when a terminal pastes a large amount of text.
                for (int count = 0; count < 16 && running; count++) {
                    int extra = terminal.reader().read(1);
                    if (extra == NonBlockingReader.READ_EXPIRED) break;
                    if (extra == -1) { running = false; break; }
                    for (int decoded : keys.accept(extra, System.nanoTime())) handle(decoded);
                }
            }
        }
    }
    Canvas frame(int width, int height, long now, String stats) {
        if (menus.screen() != GameMenu.Screen.PLAY) return menus.frame(width, height, now);
        GameLayout layout = GameLayout.of(width, height);
        Canvas c = MenuStyle.canvas(width, height);
        c.blit(playfield(layout.fieldWidth(), layout.fieldHeight(), now), layout.left(), 0);
        if (layout.left() > 0) {
            if (mode == 11) weaponRange().sidebar(c, layout.left(), height);
            else if (mode == 13) openWorld().sidebar(c, layout.left(), height);
            else GameHud.draw(c, layout.left(), height, mode == 2 && freeForAll != null ? freeForAll.world() : null,
                mode == 1 && asciiLevel != null ? 3 - asciiLevel.monstersRemaining() : 0);
            chat.draw(c, width - layout.right(), 0, layout.right(), height);
        } else {
            var p = mode == 2 && freeForAll != null ? freeForAll.world().player() : null;
            String status = p == null ? "HP -- | XP -- | Kills --" : "HP " + p.hp() + "/" + p.maxHp()
                + " | XP " + p.availableXp() + " | Kills " + p.kills();
            if (mode == 13) status = openWorld().biome().label + " | Biomen " + openWorld().visited().size() + "/6 | Relikwie " + openWorld().treasures() + "/6";
            MenuStyle.text(c, 0, height - 2, width, mode == 11 ? "Wapenpatroon / " + weaponRange().damageCells().size() + " schadevakjes" : status, MenuStyle.GREEN);
            MenuStyle.text(c, 0, height - 1, width, mode == 13 ? "WASD | F sprint | E slopen | Tab kaart | T/Y chat"
                : mode == 11 ? "T/Y chat | Esc lobby | M menu"
                : "T/Y chat | Esc lobby | M menu | 1-4 skills", MenuStyle.MUTED);
        }
        String message = mode == 2 && freeForAll != null ? freeForAll.world().message()
            : mode == 1 && asciiLevel != null ? asciiLevel.message()
            : isShowroom() && showroom != null ? showroom.message()
            : mode == 11 ? weaponRange().message()
            : mode == 13 ? openWorld().message()
            : "WASD bewegen | +/- formaat | B grond | P animatie | R reset";
        String radarLegend = mode == 2 ? "\u25cf monster \u25cb speler | getal=groep" : "\u25cf monster | getal=groep";
        boolean radar = (mode == 1 || mode == 2 || isShowroom() && mode != 14) && layout.fieldWidth() >= 64;
        int messageWidth = layout.fieldWidth() - (radar ? radarLegend.length() + 3 : 0);
        MenuStyle.text(c, layout.left(), layout.fieldHeight(), messageWidth, message, MenuStyle.MUTED);
        if (radar) {
            int legendX = layout.left() + layout.fieldWidth() - radarLegend.length();
            MenuStyle.text(c, legendX, layout.fieldHeight(), radarLegend.length(), radarLegend, MenuStyle.MUTED);
            c.text(legendX, layout.fieldHeight(), "\u25cf", EnemyIndicators.MONSTER_COLOR);
            if (mode == 2) c.text(legendX + 10, layout.fieldHeight(), "\u25cb", EnemyIndicators.PLAYER_COLOR);
        }
        if (layout.left() == 0 && chat.editing()) chat.draw(c, 0, Math.max(0, height - 11), width, Math.min(height, 11));
        return c;
    }
    /** Render scenes directly into the central viewport, without the old demo headers or padding. */
    private Canvas playfield(int width, int height, long now) {
        if (mode == 13) return openWorld().frame(width, height, now, checker, animate);
        if (mode == 11) return weaponRange().frame(width, height, now);
        if (mode == 2) {
            if (freeForAll == null) freeForAll = new FreeForAllView(now);
            return freeForAll.arenaFrame(width, height, now, checker, animate);
        }
        if (mode == 1) {
            if (asciiLevel == null) asciiLevel = new AsciiLevel(Math.max(60, width), Math.max(14, height));
            return asciiLevel.arenaFrame(width, height, now, checker, animate);
        }
        if (isShowroom()) {
            return showroom().arenaFrame(width, height, now, checker, animate);
        }
        Canvas c = MenuStyle.canvas(width, height);
        TextArt art = art();
        Sprite sprite = currentSprite();
        int panels = mode == 9 ? 3 : 1;
        int sw = (width + panels - 1) / panels, sh = height;
        int aw = art == null ? sprite.width() : art.width();
        int ah = art != null ? art.height() : mode == 9 ? sprite.height() : renderer().rows(sprite);
        if (sw < aw + 4 || sh < ah + 4) {
            world = null;
            MenuStyle.text(c, 1, Math.max(0, height / 2 - 1), width - 2, "Vergroot het venster of kies - voor kleinere sprites.", MenuStyle.AMBER);
            return c;
        }
        if (world == null || sw != sceneWidth || sh != sceneHeight || aw != actorWidth || ah != actorHeight) {
            world = new World(sw, sh, aw, ah);
            sceneWidth = sw; sceneHeight = sh; actorWidth = aw; actorHeight = ah;
            if (!world.canPlace(x, y, aw, ah)) { x = 2; y = 2; }
        }
        int pose = animate && now - lastMovement < 180_000_000L ? (steps / 2) % 2 : 0;
        if (mode == 10) sprite = PeditCharacter.create(SIZES[sizeIndex], pose);
        else if (pose == 1 && !(custom != null && mode >= 6)) sprite = sprite.walking(pose);
        for (int i = 0; i < panels; i++) {
            Canvas scene = world.draw(checker, mode == 10);
            PixelRenderer method = panels == 3 ? PixelRenderer.values()[i] : renderer();
            if (art != null) art.draw(scene, x, y, pose);
            else method.draw(scene, sprite, x, y);
            if (panels == 3) MenuStyle.text(scene, 1, 0, sw - 2, method.label(), MenuStyle.AMBER);
            c.blit(scene, i * sw, 0);
        }
        return c;
    }
    private TextArt art() { return switch (mode) { case 1 -> TextArt.ASCII; case 3 -> TextArt.UNICODE; default -> null; }; }
    private PixelRenderer renderer() {
        return switch (mode) {
            case 6 -> PixelRenderer.FULL_BLOCK;
            case 7 -> PixelRenderer.BACKGROUND;
            default -> PixelRenderer.HALF_BLOCK;
        };
    }
    private Sprite currentSprite() {
        if (mode == 10) return PeditCharacter.create(SIZES[sizeIndex], 0);
        return custom != null && mode >= 6 ? custom.fit(SIZES[sizeIndex]) : examples[sizeIndex];
    }
    Canvas demo(int width, int height, long now, String stats) {
        if (mode == 13) return openWorld().frame(width, height, now, checker, animate);
        if (mode == 11) return weaponRange().frame(width, height, now);
        if (mode == 1) return asciiLevel(width, height, now, stats);
        if (mode == 2) {
            if (freeForAll == null) freeForAll = new FreeForAllView(now);
            return freeForAll.frame(width, height, now, checker, animate);
        }
        if (isShowroom()) {
            return showroom().frame(width, height, now, checker, animate);
        }
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
        else if (pose == 1 && !(custom != null && mode >= 6)) sprite = sprite.walking(pose);
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
            : mode >= 6 ? "PNG: " + source + " | transparantie op achtergrond" : "# en + zijn obstakels; het hele kader van het poppetje botst.");
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
        boolean wasChat = chat.editing();
        boolean wasMap = mode == 13 && openWorld != null && openWorld.mapOpen();
        try { handleKey(input); }
        finally {
            if (realtimeControls != null && (wasChat || chat.editing() || wasMap
                || mode == 13 && openWorld != null && openWorld.mapOpen() || menus.screen() != GameMenu.Screen.PLAY)) {
                realtimeControls.suppressHeld(); nextControlMove = 0;
            }
        }
    }
    private void handleKey(int input) {
        int key = Character.toLowerCase(input);
        if (key == 3 || key == 4) { running = false; return; }
        if (menus.screen() != GameMenu.Screen.PLAY) {
            menus.handle(input);
            if (menus.quitRequested()) running = false;
            int start = menus.takeStartRequest();
            if (start == 0) start = menus.takeJoinRequest();
            if (start != 0) openDemo(start);
            return;
        }
        if (chat.handle(input)) return;
        if (mode != 11 && realtimeControls != null && GameControls.continuousKey(input)) return;
        if (key == 'q') { running = false; return; }
        if (key == 27) { menus.lobby(); return; }
        if (key == 'm') { menus.home(); return; }
        if (mode == 11) { weaponRange().handle(input, System.nanoTime()); return; }
        if (mode == 13 && openWorld().handle(input, System.nanoTime())) return;
        if (mode == 2 && freeForAll != null && freeForAll.handle(input, System.nanoTime())) return;
        if (isShowroom() && showroom != null && showroom.handle(input, System.nanoTime())) return;
        switch (key) {
            case 'e' -> { if (mode == 1 && asciiLevel != null) asciiLevel.interact(); }
            case 'f' -> { if (mode == 1 && asciiLevel != null) asciiLevel.nextWeapon(); }
            case ' ' -> { if (mode == 1 && asciiLevel != null) asciiLevel.attack(System.nanoTime()); }
            case '+', '=' -> { if (mode != 1 && mode != 2 && mode != 13 && !isShowroom()) { sizeIndex = Math.min(2, sizeIndex + 1); reset(); } }
            case '-', '_' -> { if (mode != 1 && mode != 2 && mode != 13 && !isShowroom()) { sizeIndex = Math.max(0, sizeIndex - 1); reset(); } }
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
    void useControls(GameControls controls) {
        realtimeControls = controls; nextControlMove = 0;
        if (controls != null) controls.suppressHeld();
    }
    void updateControls(long now) {
        if (realtimeControls == null) return;
        // The editor uses the same translated key presses/repeat as menus, on every OS.
        if (menus.screen() != GameMenu.Screen.PLAY || chat.editing() || mode == 11 || mode == 13 && openWorld().mapOpen()) {
            realtimeControls.suppressHeld(); nextControlMove = 0; return;
        }
        var held = realtimeControls.state();
        if (held.dx() != 0 || held.dy() != 0) {
            if (mode == 2 || mode == 13 || now >= nextControlMove) {
                move(held.dx(), held.dy(), now);
                nextControlMove = now + Movement.delay(140_000_000L, held.dx(), held.dy());
            }
        } else nextControlMove = 0;
        // Attack has its own cooldown and never consumes or changes the movement keys.
        if (held.attack() != 0) {
            if (mode == 2 && freeForAll != null) freeForAll.handle(held.attack(), now);
            else if (mode == 5 && showroom != null) showroom.handle(held.attack(), now);
        } else if (held.space()) {
            if (mode == 1 && asciiLevel != null) asciiLevel.attack(now);
            else if (mode == 5 && showroom != null) showroom.handle(' ', now);
            else if (mode == 13) openWorld().interact(now);
        }
    }
    void openDemo(int selectedMode) {
        if (selectedMode < 1 || selectedMode > MODES.length) throw new IllegalArgumentException("Onbekende demo");
        mode = selectedMode; menus.playing(mode); chat = new GameChat();
        if (mode == 10) checker = false;
        reset();
    }
    private void reset() {
        x = 2; y = 2; steps = 0; lastMovement = 0; nextControlMove = 0;
        world = null; asciiLevel = null; freeForAll = null; showroom = null; weaponRange = null; openWorld = null;
        if (realtimeControls != null) realtimeControls.suppressHeld();
    }
    private WeaponRangeLab weaponRange() {
        if (weaponRange == null) weaponRange = new WeaponRangeLab();
        return weaponRange;
    }
    private boolean isShowroom() { return mode == 4 || mode == 5 || mode == 12 || mode == 14; }
    private Showroom showroom() {
        if (showroom == null) showroom = switch (mode) {
            case 12 -> Showroom.unicodeMonsters();
            case 14 -> Showroom.smallAsciiMonsters();
            default -> new Showroom(mode == 5);
        };
        return showroom;
    }
    OpenWorld openWorld() {
        if (openWorld == null) openWorld = new OpenWorld();
        return openWorld;
    }
    private void move(int dx, int dy) {
        move(dx, dy, System.nanoTime());
    }
    private void move(int dx, int dy, long now) {
        if (mode == 13) { openWorld().move(dx, dy, now); return; }
        if (mode == 1) {
            if (asciiLevel != null) asciiLevel.move(dx, dy, now);
            return;
        }
        if (mode == 2) {
            if (freeForAll != null) freeForAll.world().move(freeForAll.world().player(), dx, dy, now);
            return;
        }
        if (isShowroom()) {
            if (showroom != null) showroom.move(dx, dy, now);
            return;
        }
        if (mode == 0 || world == null) return;
        var step = Movement.resolve(x, y, dx, dy, (px, py) -> world.canPlace(px, py, actorWidth, actorHeight));
        if (!step.moved()) return;
        x += step.dx(); y += step.dy(); steps++; lastMovement = now;
    }
}
