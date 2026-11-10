package nl.pinc.terminallab;

/** A text sprite measured in terminal cells, with transparent spaces. */
public interface TextModel {
    int width();
    int height();
    void draw(Canvas canvas, int x, int y, int color);
}
