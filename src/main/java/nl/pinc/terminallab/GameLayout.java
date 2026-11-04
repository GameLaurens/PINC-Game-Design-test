package nl.pinc.terminallab;

/** Three edge-to-edge columns. The central viewport gets all remaining cells. */
record GameLayout(int width, int height, int left, int right) {
    static GameLayout of(int width, int height) {
        boolean sidebars = width >= 80 && height >= 22;
        int side = sidebars ? Math.max(14, Math.min(30, width / 7)) : 0;
        return new GameLayout(width, height, side, side);
    }
    int fieldWidth() { return width - left - right; }
    int fieldHeight() { return Math.max(1, height - (left == 0 ? 3 : 1)); }
}
