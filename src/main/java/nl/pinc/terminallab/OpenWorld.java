package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import nl.pinc.terminallab.OpenWorldCatalog.Biome;
import nl.pinc.terminallab.OpenWorldCatalog.Kind;
import nl.pinc.terminallab.OpenWorldCatalog.Resource;

/** Finite, persistent exploration sandbox. Only the visible window is rendered. */
public final class OpenWorld {
    public static final int WIDTH = 720, HEIGHT = 300, PLAYER_WIDTH = 5, PLAYER_HEIGHT = 5;
    private static final byte GROUND = 0, ROAD = 1, WATER = 2, LAVA = 3, WALL = 4, RUIN = 5;
    public record Camera(int x, int y, int width, int height) { }
    public static final class WorldObject {
        public final Kind kind;
        public final int x, y;
        public final Biome biome;
        public final boolean gateway;
        private int hp;
        private boolean opened;
        private long hitUntil;
        WorldObject(Kind kind, int x, int y, Biome biome, boolean gateway) {
            this.kind = kind; this.x = x; this.y = y; this.biome = biome; this.gateway = gateway;
            hp = kind.durability;
        }
        public int hp() { return hp; }
        public boolean opened() { return opened; }
        public boolean destroyed() { return kind.durability > 0 && hp == 0; }
        World.Rect bounds() { return new World.Rect(x, y, kind.model.width(), kind.model.height()); }
    }
    private final byte[] terrain = new byte[WIDTH * HEIGHT], biomes = new byte[WIDTH * HEIGHT];
    private final List<WorldObject> objects = new ArrayList<>();
    private final EnumSet<Biome> visited = EnumSet.of(Biome.MEADOW);
    private int x = 90, y = 58, steps, wood, stone, crystals, treasures;
    private boolean sprint, mapOpen;
    private long nextMove, lastMove, nextInteraction;
    private String message = "Welkom! E sloopt de stam op het pad. F sprint | Tab wereldkaart en snelreizen.";

    public OpenWorld() {
        generateTerrain();
        for (Biome b : Biome.values()) buildCampAndRuin(b);
        scatterScenery();
    }
    public int playerX() { return x; }
    public int playerY() { return y; }
    public boolean sprinting() { return sprint; }
    public boolean mapOpen() { return mapOpen; }
    public int wood() { return wood; }
    public int stone() { return stone; }
    public int crystals() { return crystals; }
    public int treasures() { return treasures; }
    public List<WorldObject> objects() { return List.copyOf(objects); }
    public EnumSet<Biome> visited() { return visited.clone(); }
    public Biome biome() { return biomeAt(x + 2, y + 2); }
    public Biome biomeAt(int px, int py) { return Biome.values()[biomes[py * WIDTH + px]]; }
    public String message() { return message; }

    private void generateTerrain() {
        for (int row = 0; row < HEIGHT; row++) for (int col = 0; col < WIDTH; col++) {
            int regionX = col < 240 + 14 * Math.sin(row / 22.0) ? 0 : col < 480 + 12 * Math.sin(row / 19.0) ? 1 : 2;
            int regionY = row < 150 + 12 * Math.sin(col / 38.0) ? 0 : 1;
            Biome b = Biome.values()[regionY * 3 + regionX];
            int index = row * WIDTH + col;
            biomes[index] = (byte) b.ordinal();
            // Small lakes and lava pools leave generous routes around their shores.
            int cx = (regionX * 240) + 165, cy = regionY * 150 + 105;
            double ellipse = Math.pow((col - cx) / 29.0, 2) + Math.pow((row - cy) / 13.0, 2);
            if (ellipse < 1 + 0.12 * Math.sin(col * .35)) terrain[index] = b == Biome.VOLCANO ? LAVA : WATER;
            if (Math.abs(row - 60) <= 5 || Math.abs(row - 210) <= 5
                || Math.abs(col - 100) <= 6 || Math.abs(col - 340) <= 6 || Math.abs(col - 580) <= 6)
                terrain[index] = ROAD;
            if (col < 2 || row < 2 || col >= WIDTH - 2 || row >= HEIGHT - 2) terrain[index] = WALL;
        }
    }
    private void fill(int px, int py, int w, int h, byte tile) {
        for (int row = py; row < py + h; row++) for (int col = px; col < px + w; col++) terrain[row * WIDTH + col] = tile;
    }
    private WorldObject add(Kind kind, int px, int py, Biome b, boolean gate) {
        WorldObject o = new WorldObject(kind, px, py, b, gate); objects.add(o); return o;
    }
    private void buildCampAndRuin(Biome b) {
        int cx = b.campX, cy = b.campY;
        fill(cx - 24, cy - 15, 54, 32, GROUND);
        fill(cx - 24, cy - 5, 54, 11, ROAD);
        fill(cx - 6, cy - 15, 13, 32, ROAD);
        add(Kind.TENT, cx - 21, cy - 13, b, false);
        add(Kind.WAYSTONE, cx - 17, cy + 9, b, false);
        add(Kind.SIGN, cx + 7, cy - 13, b, false);
        add(Kind.WELL, cx + 17, cy + 10, b, false);
        add(Kind.LOG, cx + 15, cy - 2, b, false);
        int rx = cx + 40, ry = cy + 7;
        fill(rx, ry, 42, 25, WALL);
        fill(rx + 2, ry + 3, 38, 20, RUIN);
        Kind gate = switch (b) {
            case MEADOW -> Kind.CRATES; case FOREST -> Kind.THORNS; case DESERT -> Kind.ROCK;
            case SWAMP -> Kind.LOG; case TUNDRA -> Kind.ICE; case VOLCANO -> Kind.BASALT;
        };
        fill(rx + 15, ry, gate.model.width(), 3, RUIN);
        add(gate, rx + 15, ry, b, true);
        add(Kind.CHEST, rx + 17, ry + 13, b, false);
        add(Kind.PILLAR, rx + 5, ry + 7, b, false);
        add(Kind.PILLAR, rx + 30, ry + 7, b, false);
    }
    private void scatterScenery() {
        Random random = new Random(0x50494e43L);
        for (Biome b : Biome.values()) {
            Kind[] choices = switch (b) {
                case MEADOW -> new Kind[]{Kind.TREE, Kind.FLOWERS, Kind.ROCK, Kind.FLOWERS};
                case FOREST -> new Kind[]{Kind.TREE, Kind.PINE, Kind.MUSHROOM, Kind.LOG};
                case DESERT -> new Kind[]{Kind.CACTUS, Kind.PALM, Kind.ROCK, Kind.PILLAR};
                case SWAMP -> new Kind[]{Kind.REED, Kind.MUSHROOM, Kind.LOG, Kind.TREE};
                case TUNDRA -> new Kind[]{Kind.PINE, Kind.CRYSTAL, Kind.ICE, Kind.ROCK};
                case VOLCANO -> new Kind[]{Kind.VENT, Kind.BASALT, Kind.CRYSTAL, Kind.PILLAR};
            };
            int placed = 0;
            for (int attempt = 0; attempt < 3000 && placed < 65; attempt++) {
                Kind kind = choices[random.nextInt(choices.length)];
                int px = 10 + random.nextInt(WIDTH - 34), py = 10 + random.nextInt(HEIGHT - 25);
                if (biomeAt(px, py) != b || reserved(px, py, kind.model.width(), kind.model.height())) continue;
                add(kind, px, py, b, false); placed++;
            }
        }
    }
    private boolean reserved(int px, int py, int w, int h) {
        for (Biome b : Biome.values()) {
            if (new World.Rect(b.campX - 30, b.campY - 20, 65, 42).overlaps(px, py, w, h)
                || new World.Rect(b.campX + 33, b.campY, 57, 41).overlaps(px, py, w, h)) return true;
        }
        for (int row = py - 4; row < py + h + 4; row++) for (int col = px - 5; col < px + w + 5; col++)
            if (terrain[row * WIDTH + col] != GROUND) return true;
        return objects.stream().anyMatch(o -> o.bounds().overlaps(px - 5, py - 4, w + 10, h + 8));
    }
    boolean canPlace(int px, int py) {
        if (px < 2 || py < 2 || px + PLAYER_WIDTH > WIDTH - 2 || py + PLAYER_HEIGHT > HEIGHT - 2) return false;
        for (int row = py; row < py + PLAYER_HEIGHT; row++) for (int col = px; col < px + PLAYER_WIDTH; col++) {
            byte t = terrain[row * WIDTH + col];
            if (t == WALL || t == WATER || t == LAVA) return false;
        }
        return objects.stream().noneMatch(o -> o.kind.solid() && !o.destroyed() && o.bounds().overlaps(px, py, PLAYER_WIDTH, PLAYER_HEIGHT));
    }
    boolean step(int dx, int dy, long now) {
        var step = Movement.resolve(x, y, dx, dy, this::canPlace);
        if (!step.moved()) return false;
        x += step.dx(); y += step.dy(); steps++; lastMove = now; visited.add(biome()); return true;
    }
    public boolean move(int dx, int dy, long now) {
        if (mapOpen || now < nextMove || Math.abs(dx) > 1 || Math.abs(dy) > 1 || dx == 0 && dy == 0) return false;
        nextMove = now + Movement.delay(sprint ? 40_000_000L : 60_000_000L, dx, dy);
        boolean moved = false;
        for (int i = 0; i < (sprint ? 4 : 2); i++) {
            if (!step(dx, dy, now)) break;
            moved = true;
        }
        return moved;
    }
    private int gap(WorldObject o) {
        return Math.max(0, Math.max(o.x - (x + PLAYER_WIDTH), x - (o.x + o.kind.model.width())))
            + Math.max(0, Math.max(o.y - (y + PLAYER_HEIGHT), y - (o.y + o.kind.model.height())));
    }
    public WorldObject nearby() {
        WorldObject nearest = null;
        for (WorldObject o : objects) if (!o.destroyed() && o.kind.interactive() && !(o.kind == Kind.CHEST && o.opened)
            && gap(o) <= 3 && (nearest == null || gap(o) < gap(nearest))) nearest = o;
        return nearest;
    }
    public void interact(long now) {
        if (mapOpen || now < nextInteraction) return;
        WorldObject o = nearby();
        if (o == null) { message = "Geen object binnen bereik. Loop naast een stam, rots, kist of wegwijzer en druk E."; return; }
        nextInteraction = now + 160_000_000L;
        if (o.kind.durability > 0) {
            o.hp--; o.hitUntil = now + 180_000_000L;
            if (o.hp > 0) message = o.kind.label + ": nog " + o.hp + " slag(en). E of Spatie om verder te slopen.";
            else {
                if (o.kind.resource == Resource.WOOD) wood += 2;
                else if (o.kind.resource == Resource.STONE) stone += 2;
                else if (o.kind.resource == Resource.CRYSTAL) crystals++;
                message = o.kind.label + " gesloopt! " + (o.gateway ? "De doorgang naar de ruïnekist is vrij." : "De weg is vrij; materiaal verzameld.");
            }
        } else switch (o.kind) {
            case CHEST -> { o.opened = true; treasures++; message = "Ruïnekist geopend! Relikwie " + treasures + "/6 gevonden in " + o.biome.label + "."; }
            case SIGN -> message = o.biome.label + ": ruïne ten zuidoosten. Sloop de ingang met E; verbindingspaden lopen door alle biomen.";
            case WAYSTONE -> { mapOpen = true; message = "Reissteen: kies 1-6 op de kaart. Snelreizen is vrij beschikbaar in deze demo."; }
            case WELL -> message = "Helder bronwater. Even rust in " + o.biome.label + ". F zet sprint aan of uit; geen uithoudingslimiet.";
            default -> { }
        }
    }
    public void travel(Biome biome) {
        x = biome.campX - 10; y = biome.campY - 2;
        mapOpen = false; nextMove = nextInteraction = 0; visited.add(biome);
        message = "Aangekomen in " + biome.label + ". E: object gebruiken/slopen | F: sprint | Tab: kaart.";
    }
    public boolean handle(int input, long now) {
        int key = Character.toLowerCase(input);
        if (key == '\t') { mapOpen = !mapOpen; nextMove = 0; return true; }
        if (key == 'r') return false;
        if (mapOpen) {
            if (key >= '1' && key <= '6') travel(Biome.values()[key - '1']);
            return true;
        }
        switch (key) {
            case 'w' -> move(0, -1, now); case 's' -> move(0, 1, now);
            case 'a' -> move(-1, 0, now); case 'd' -> move(1, 0, now);
            case 'e', ' ' -> interact(now);
            case 'f' -> { sprint = !sprint; nextMove = 0; message = "Sprint " + (sprint ? "AAN - 4 cellen per stap" : "UIT - 2 cellen per stap") + " | E: slopen | Tab: wereldkaart"; }
            default -> { return false; }
        }
        return true;
    }

    public Camera camera(int width, int height) {
        int w = Math.min(WIDTH, Math.max(1, width)), h = Math.min(HEIGHT, Math.max(1, height));
        return new Camera(Math.max(0, Math.min(WIDTH - w, x + 2 - w / 2)),
            Math.max(0, Math.min(HEIGHT - h, y + 2 - h / 2)), w, h);
    }
    private Cell ground(int px, int py, long now, boolean detail) {
        Biome b = biomeAt(px, py);
        int noise = Math.floorMod(px * 73856093 ^ py * 19349663, 97);
        int bg = ((px / 7 + py / 4) & 1) == 0 ? b.ground : b.lightGround;
        char glyph = detail && noise < 9 ? (noise % 2 == 0 ? '·' : ',') : ' ';
        int fg = b.ink;
        switch (terrain[py * WIDTH + px]) {
            case ROAD -> { bg = 0x685946; fg = 0xa5926c; glyph = detail && noise < 12 ? '·' : ' '; }
            case WATER -> { bg = 0x243f51; fg = 0x5c8c9b; glyph = (px + py + now / 500_000_000L) % 9 == 0 ? '≈' : ' '; }
            case LAVA -> { bg = 0x763b32; fg = 0xf5b26b; glyph = (px + py + now / 400_000_000L) % 7 == 0 ? '▒' : '░'; }
            case WALL -> { bg = 0x45434a; fg = 0x89818b; glyph = ((px + py) & 1) == 0 ? '▓' : '▒'; }
            case RUIN -> { bg = 0x3c4247; fg = 0x777f83; glyph = detail && noise < 15 ? '┄' : ' '; }
            default -> { }
        }
        return new Cell(glyph, fg, bg);
    }
    public Canvas frame(int width, int height, long now, boolean detail, boolean animate) {
        Canvas c = new Canvas(width, height);
        Camera cam = camera(width, height);
        for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
            int wx = cam.x + col, wy = cam.y + row;
            c.put(col, row, wx < WIDTH && wy < HEIGHT ? ground(wx, wy, now, detail) : new Cell('≈', 0x5c8c9b, 0x243f51));
        }
        for (WorldObject o : objects) {
            if (!o.bounds().overlaps(cam.x, cam.y, width, height)) continue;
            int sx = o.x - cam.x, sy = o.y - cam.y;
            if (o.destroyed()) { c.text(sx, sy + 1, "·  ·  ·", 0x929180); continue; }
            TextModel model = o.opened ? OpenWorldCatalog.OPEN_CHEST : o.kind.model;
            model.draw(c, sx, sy, o.kind.color);
            if (now < o.hitUntil) c.text(sx, sy - 1, "✦ " + o.hp + "HP", 0xffba92);
        }
        // Trim the player's transparent left padding: its visible and collision bounds are both 5x5.
        int pose = animate && now - lastMove < 180_000_000L ? steps / (sprint ? 4 : 2) % 2 : 0;
        (pose == 0 ? UnicodeMonsters.PLAYER : UnicodeMonsters.WALKING_PLAYER).draw(c, x - cam.x - 1, y - cam.y, 0xfff0d5);
        WorldObject close = nearby();
        if (close != null) {
            String hint = "E " + close.kind.label + (close.kind.durability > 0 ? " [" + close.hp + "]" : "");
            MenuStyle.text(c, Math.max(0, Math.min(width - hint.length(), close.x - cam.x)),
                Math.max(0, Math.min(height - 1, close.y - cam.y - 1)), width, hint, 0xffd78a);
        }
        if (mapOpen) drawMap(c);
        return c;
    }
    private void drawMap(Canvas c) {
        int panelW = Math.min(86, c.width()), panelH = Math.min(31, c.height());
        int left = (c.width() - panelW) / 2, top = (c.height() - panelH) / 2;
        MenuStyle.panel(c, left, top, panelW, panelH, "WERELDKAART / TAB SLUIT");
        int mw = Math.max(1, panelW - 4), mh = Math.max(1, panelH - 11);
        for (int row = 0; row < mh && top + 2 + row < c.height(); row++) for (int col = 0; col < mw; col++) {
            int wx = col * (WIDTH - 1) / mw, wy = row * (HEIGHT - 1) / mh;
            Cell tile = ground(wx, wy, 0, false);
            c.put(left + 2 + col, top + 2 + row, new Cell(' ', tile.foreground(), tile.background()));
        }
        for (Biome b : Biome.values()) c.text(left + 2 + b.campX * mw / WIDTH, top + 2 + b.campY * mh / HEIGHT,
            "" + (b.ordinal() + 1), 0xffd78a);
        c.text(left + 2 + x * mw / WIDTH, top + 2 + y * mh / HEIGHT, "@", 0xffffff);
        for (Biome b : Biome.values()) MenuStyle.text(c, left + 2, top + mh + 3 + b.ordinal(), panelW - 4,
            (b.ordinal() + 1) + " " + b.label + (visited.contains(b) ? "  [ontdekt]" : ""), b.ink);
        MenuStyle.text(c, left + 2, top + panelH - 1, panelW - 4, "1-6 snelreizen | @ jij | onbeperkt reizen in deze demo", MenuStyle.INK);
    }
    public void sidebar(Canvas c, int width, int height) {
        MenuStyle.panel(c, 0, 0, width, height, "VERKENNING");
        String[] lines = {biome().label, x + "," + y + " / 720x300", "Biomen " + visited.size() + "/6",
            sprint ? "SPRINT / snel" : "LOPEN / vlot", "", "Hout " + wood, "Steen " + stone, "Kristal " + crystals,
            "Relikwie " + treasures + "/6", "", "WASD lopen", "F sprint aan/uit", "E / Spatie slopen", "Tab wereldkaart", "1-6 op de kaart", "B gronddetail", "R nieuwe wereld"};
        for (int i = 0; i < lines.length && i + 2 < height; i++)
            MenuStyle.text(c, 1, i + 2, width - 2, lines[i], i < 4 ? MenuStyle.GREEN : MenuStyle.INK);
    }
}
