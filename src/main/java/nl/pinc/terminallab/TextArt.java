package nl.pinc.terminallab;

public enum TextArt {
    ASCII(new String[]{" O ", "/|\\", "/ \\"}, new String[]{" O ", "\\|/", "| |"}),
    DETAIL(new String[]{"  .---.  ", "  |o o|  ", "  | - |  ", " /|===|\\ ", "/ | : | \\", "  /   \\  ", " _|   |_ "},
           new String[]{"  .---.  ", "  |o o|  ", "  | - |  ", " \\|===|/ ", "  | : |  ", "  |   |  ", "  |_  |_ "}),
    UNICODE(new String[]{" ▄██▄ ", " █▪▪█ ", " ▀▄▄▀ ", "▄█▀▀█▄", " ▐██▌ ", " ▐▌▐▌ ", " ▀  ▀ "},
            new String[]{" ▄██▄ ", " █▪▪█ ", " ▀▄▄▀ ", "▀█▀▀█▀", " ▐██▌ ", " ▐▌ ▐▌", " ▀   ▀"});
    private final String[][] frames;
    TextArt(String[] still, String[] walking) { frames = new String[][]{still, walking}; }
    public int width() { return java.util.Arrays.stream(frames).flatMap(java.util.Arrays::stream).mapToInt(String::length).max().orElse(0); }
    public int height() { return frames[0].length; }
    public void draw(Canvas canvas, int x, int y, int frame) {
        String[] lines = frames[frame % 2];
        for (int row = 0; row < lines.length; row++) for (int col = 0; col < lines[row].length(); col++) {
            char ch = lines[row].charAt(col);
            if (ch != ' ' && canvas.contains(x + col, y + row))
                canvas.put(x + col, y + row, new Cell(ch, Canvas.INK, canvas.get(x + col, y + row).background()));
        }
    }
}
