package nl.pinc.terminallab;

/** Eight-way movement and wall sliding; attack directions deliberately remain separate. */
final class Movement {
    @FunctionalInterface interface Free { boolean at(int x, int y); }
    record Step(int dx, int dy) { boolean moved() { return dx != 0 || dy != 0; } }
    private static final Step NONE = new Step(0, 0);
    static Step resolve(int x, int y, int dx, int dy, Free free) {
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || (dx == 0 && dy == 0)) return NONE;
        boolean horizontal = dx != 0 && free.at(x + dx, y);
        boolean vertical = dy != 0 && free.at(x, y + dy);
        if (dx != 0 && dy != 0 && horizontal && vertical && free.at(x + dx, y + dy)) return new Step(dx, dy);
        if (horizontal) return new Step(dx, 0);
        if (vertical) return new Step(0, dy);
        return NONE;
    }
    static long delay(long base, int dx, int dy) {
        return dx != 0 && dy != 0 ? Math.round(base * Math.sqrt(2)) : base;
    }
    private Movement() { }
}
