package nl.pinc.terminallab;

/** Small in-memory concept editor. Source is editable text and is never executed. */
final class GameDraft {
    private static final String TEMPLATE = "game \"Mijn arena\" {\n"
        + "    gamemode = \"Free for All\";\n"
        + "    max_players = 8;\n"
        + "    monster \"slime\" {\n"
        + "        hp = 10;\n"
        + "        xp = 10;\n"
        + "    }\n"
        + "}";
    private final StringBuilder source = new StringBuilder(TEMPLATE);
    private String saved;
    private int cursor;
    private int preferredColumn = -1;
    private boolean editing;
    String source() { return source.toString(); }
    boolean editing() { return editing; }
    boolean saved() { return source.toString().equals(saved); }
    void edit() { editing = true; cursor = source.length(); preferredColumn = -1; }
    void save() { saved = source.toString(); editing = false; }
    void reset() { source.setLength(0); source.append(TEMPLATE); cursor = 0; editing = false; }
    boolean handle(int key) {
        if (!editing) return false;
        if (key != TerminalKeys.UP && key != TerminalKeys.DOWN) preferredColumn = -1;
        switch (key) {
            case 27 -> editing = false;
            case 19 -> save(); // Ctrl+S
            case TerminalKeys.LEFT -> cursor = Math.max(0, cursor - 1);
            case TerminalKeys.RIGHT -> cursor = Math.min(source.length(), cursor + 1);
            case TerminalKeys.UP -> vertical(-1);
            case TerminalKeys.DOWN -> vertical(1);
            case 8, 127 -> { if (cursor > 0) source.deleteCharAt(--cursor); }
            case '\r', '\n' -> insert("\n");
            case '\t' -> insert("    ");
            default -> { if (key >= 32 && key <= 126) insert(Character.toString(key)); }
        }
        return true;
    }
    private void insert(String text) {
        if (source.length() + text.length() > 4000) return;
        source.insert(cursor, text); cursor += text.length();
    }
    private void vertical(int direction) {
        int start = source.lastIndexOf("\n", cursor - 1) + 1;
        int column = preferredColumn < 0 ? cursor - start : preferredColumn;
        preferredColumn = column;
        if (direction < 0 && start > 0) {
            int previous = source.lastIndexOf("\n", start - 2) + 1;
            cursor = Math.min(previous + column, start - 1);
        } else if (direction > 0) {
            int next = source.indexOf("\n", cursor);
            if (next < 0) return;
            int end = source.indexOf("\n", next + 1);
            cursor = Math.min(next + 1 + column, end < 0 ? source.length() : end);
        }
    }
    void draw(Canvas c, int x, int y, int w, int h) {
        MenuStyle.card(c, x, y, w, h, "GAMECODE / CONCEPT");
        String[] lines = source.toString().split("\n", -1);
        String before = source.substring(0, cursor);
        int row = (int) before.chars().filter(ch -> ch == '\n').count();
        int column = cursor - (before.lastIndexOf('\n') + 1);
        int visible = Math.max(1, h - 4), first = editing ? Math.max(0, row - visible + 1) : 0;
        int textWidth = Math.max(1, w - 8), offset = editing ? Math.max(0, column - textWidth + 1) : 0;
        for (int i = first; i < Math.min(lines.length, first + visible); i++) {
            int lineY = y + 2 + i - first;
            MenuStyle.text(c, x + 2, lineY, 3, String.format("%02d", i + 1), MenuStyle.MUTED);
            String line = lines[i].substring(Math.min(offset, lines[i].length()));
            MenuStyle.text(c, x + 6, lineY, textWidth, line, MenuStyle.INK);
        }
        if (editing) {
            int cx = x + 6 + column - offset, cy = y + 2 + row - first;
            char glyph = cursor < source.length() && source.charAt(cursor) != '\n' ? source.charAt(cursor) : '_';
            c.put(cx, cy, new Cell(glyph, MenuStyle.AMBER, MenuStyle.LINE));
        }
    }
}
