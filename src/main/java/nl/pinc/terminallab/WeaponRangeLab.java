package nl.pinc.terminallab;

import java.util.LinkedHashSet;
import java.util.Set;

/** Editable damage mask in thirds of a character, independent of terminal cell size. */
final class WeaponRangeLab {
    static final int RADIUS = 5, SIZE = RADIUS * 2 + 1;
    static final int FLOOR = 0x192a2f, TILE = 0x23383d, BODY = 0x304a52;
    static final int DAMAGE = 0x783e43, CURSOR = 0xffd180, FLASH = 0xc77848;
    record Offset(int x, int y) { }
    /** Roomy tiles have ASCII borders; compact tiles use alternating backgrounds. */
    record Grid(int left, int top, int cellWidth, int cellHeight, boolean roomy) {
        int x(int offset) { return left + 1 + (offset + RADIUS) * cellWidth; }
        int y(int offset) { return top + 1 + (offset + RADIUS) * cellHeight; }
    }
    private final Set<Offset> damage = new LinkedHashSet<>();
    private int cursorX = 2, cursorY;
    private long previewUntil;
    private String message = "Pijlen: kies een vakje. Enter: schade aan/uit.";

    int cursorX() { return cursorX; }
    int cursorY() { return cursorY; }
    Set<Offset> damageCells() { return Set.copyOf(damage); }
    boolean damages(int x, int y) { return damage.contains(new Offset(x, y)); }
    String message() { return message; }
    private boolean body(int x, int y) { return Math.abs(x) <= 1 && Math.abs(y) <= 1; }

    boolean handle(int key, long now) {
        switch (key) {
            case TerminalKeys.LEFT -> cursorX = Math.max(-RADIUS, cursorX - 1);
            case TerminalKeys.RIGHT -> cursorX = Math.min(RADIUS, cursorX + 1);
            case TerminalKeys.UP -> cursorY = Math.max(-RADIUS, cursorY - 1);
            case TerminalKeys.DOWN -> cursorY = Math.min(RADIUS, cursorY + 1);
            case '\r', '\n' -> {
                if (body(cursorX, cursorY)) {
                    message = "Hier staat de speler. Kies een vakje eromheen.";
                    return true;
                }
                Offset cell = new Offset(cursorX, cursorY);
                boolean removed = damage.remove(cell);
                if (!removed) damage.add(cell);
                message = "Vak (" + cursorX + "," + cursorY + "): schade " + (removed ? "uit" : "aan") + ".";
                previewUntil = 0;
            }
            case 'c', 'C' -> {
                damage.clear(); previewUntil = 0;
                message = "Alle schadevakjes gewist.";
            }
            case 'r', 'R' -> {
                damage.clear(); cursorX = 2; cursorY = 0; previewUntil = 0;
                message = "Nieuw wapenpatroon. Pijlen + Enter om te tekenen.";
            }
            case ' ' -> {
                previewUntil = now + 350_000_000L;
                message = damage.isEmpty() ? "Selecteer eerst schadevakjes met Enter."
                    : "Aanvals-preview: " + damage.size() + " vakjes doen schade.";
            }
            default -> { return false; }
        }
        return true;
    }

    Grid grid(int width, int height) {
        boolean roomy = width >= SIZE * 4 + 3 && height >= SIZE * 2 + 9;
        int cw = roomy ? 4 : 2, ch = roomy ? 2 : 1;
        int gw = SIZE * cw + (roomy ? 1 : 2), gh = SIZE * ch + (roomy ? 1 : 2);
        int top = roomy ? 4 : 3;
        return new Grid((width - gw) / 2, top + Math.max(0, (height - top - 3 - gh) / 2), cw, ch, roomy);
    }

    Canvas frame(int width, int height, long now) {
        Canvas c = new Canvas(width, height);
        MenuStyle.fill(c, 0, 0, width, height, FLOOR);
        MenuStyle.text(c, 1, 0, width - 2, "weapon_range\\wapen_instellen", MenuStyle.AMBER);
        if (width < 26 || height < 19) {
            MenuStyle.text(c, 1, 2, width - 2, "Vergroot het speelveld naar 26x19.", CURSOR);
            MenuStyle.text(c, 1, 4, width - 2, "Je patroon blijft bewaard.", MenuStyle.INK);
            return c;
        }
        MenuStyle.text(c, 1, 1, width - 2, "Pijlen: cursor | Enter: schade aan/uit", MenuStyle.INK);
        MenuStyle.text(c, 1, 2, width - 2, "1 vak = 1/3 speler | speler = 3x3 vakken", MenuStyle.MUTED);
        Grid grid = grid(width, height);
        int gw = SIZE * grid.cellWidth + (grid.roomy ? 1 : 2);
        int gh = SIZE * grid.cellHeight + (grid.roomy ? 1 : 2);
        for (int row = 0; row < gh; row++) for (int col = 0; col < gw; col++) {
            boolean line = grid.roomy ? row % grid.cellHeight == 0 || col % grid.cellWidth == 0
                : row == 0 || col == 0 || row == gh - 1 || col == gw - 1;
            if (line) {
                char glyph = grid.roomy && row % grid.cellHeight == 0 ? (col % grid.cellWidth == 0 ? '+' : '-')
                    : row == 0 || row == gh - 1 ? '-' : '|';
                c.put(grid.left + col, grid.top + row, new Cell(glyph, 0x4c676a, FLOOR));
            }
        }
        boolean preview = now < previewUntil;
        for (int y = -RADIUS; y <= RADIUS; y++) for (int x = -RADIUS; x <= RADIUS; x++) {
            boolean selected = damages(x, y), cursor = x == cursorX && y == cursorY;
            int bg = body(x, y) ? BODY : selected ? preview ? FLASH : DAMAGE : (x + y) % 2 == 0 ? TILE : FLOOR;
            int tileWidth = grid.cellWidth - (grid.roomy ? 1 : 0);
            MenuStyle.fill(c, grid.x(x), grid.y(y), tileWidth, 1, bg);
            if (!body(x, y)) {
                String glyph = cursor ? grid.roomy ? selected ? "[x]" : "[ ]" : "[]"
                    : grid.roomy ? selected ? "###" : " . " : selected ? "##" : ". ";
                c.text(grid.x(x), grid.y(y), glyph, cursor ? CURSOR : selected ? 0xffaaa0 : 0x66828a);
            }
        }
        // A continuous 3x3-cell silhouette makes the one-third scale explicit.
        int bodyLeft = grid.x(-1), bodyTop = grid.y(-1);
        MenuStyle.fill(c, bodyLeft, bodyTop, grid.cellWidth * 3 - (grid.roomy ? 1 : 0),
            grid.cellHeight * 3 - (grid.roomy ? 1 : 0), BODY);
        if (body(cursorX, cursorY)) MenuStyle.fill(c, grid.x(cursorX), grid.y(cursorY),
            grid.cellWidth - (grid.roomy ? 1 : 0), 1, 0x635a3f);
        String[] avatar = {" O ", "/|\\", "/ \\"};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            if (avatar[row].charAt(col) != ' ') c.text(grid.x(col - 1) + (grid.roomy ? 1 : 0), grid.y(row - 1),
                Character.toString(avatar[row].charAt(col)), MenuStyle.INK);
        c.text(grid.x(1) + (grid.roomy ? 2 : 1), grid.y(0), "/", 0x87d7ed);
        MenuStyle.text(c, 1, height - 3, width - 2, "Cursor (" + cursorX + "," + cursorY + ") | Schadevakjes: " + damage.size(), CURSOR);
        MenuStyle.text(c, 1, height - 2, width - 2, "Spatie: test aanval | C: wis | R: reset", MenuStyle.INK);
        MenuStyle.text(c, 1, height - 1, width - 2, "Rood = schade | [ ] = cursor | midden = speler", MenuStyle.MUTED);
        return c;
    }

    void sidebar(Canvas c, int width, int height) {
        MenuStyle.fill(c, 0, 0, width, height, MenuStyle.PANEL);
        String[] lines = {"WAPENPATROON", "", damage.size() + " vakjes", "Raster 11x11", "Speler 3x3", "", "Pijlen: kies",
            "Enter: kies", "Spatie: test", "C: leeg", "R: reset", "", "Rood: schade", "Geel: cursor", "Blauw: jij"};
        for (int i = 0; i < lines.length; i++) MenuStyle.text(c, 1, i + 1, width - 2, lines[i], i == 0 ? MenuStyle.AMBER : MenuStyle.INK);
        MenuStyle.text(c, 1, height - 3, width - 2, "Esc: lobby", MenuStyle.MUTED);
        MenuStyle.text(c, 1, height - 2, width - 2, "M: hoofdmenu", MenuStyle.MUTED);
    }
}
