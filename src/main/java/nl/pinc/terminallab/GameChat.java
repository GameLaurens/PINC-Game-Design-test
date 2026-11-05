package nl.pinc.terminallab;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Local chat UI. Text entry consumes keys before they can reach the game. */
final class GameChat {
    private record Message(boolean team, String text) { }
    private final Deque<Message> messages = new ArrayDeque<>();
    private final StringBuilder draft = new StringBuilder();
    private boolean editing, team;
    boolean editing() { return editing; }
    String draft() { return draft.toString(); }
    void close() { editing = false; draft.setLength(0); }
    boolean handle(int key) {
        if (!editing) {
            if (Character.toLowerCase(key) != 't' && Character.toLowerCase(key) != 'y') return false;
            editing = true; team = Character.toLowerCase(key) == 'y'; draft.setLength(0); return true;
        }
        if (key == 27) close();
        else if (key == '\r' || key == '\n') {
            if (!draft.toString().isBlank()) {
                messages.addLast(new Message(team, "JIJ: " + draft));
                while (messages.size() > 60) messages.removeFirst();
            }
            close();
        } else if (key == 127 || key == 8) {
            if (!draft.isEmpty()) draft.deleteCharAt(draft.length() - 1);
        } else if (key >= 32 && key <= 126 && draft.length() < 160) draft.append((char) key);
        return true;
    }
    void draw(Canvas c, int x, int y, int w, int h) {
        MenuStyle.panel(c, x, y, w, h, "CHAT / " + (team ? "TEAM" : "GLOBAL"));
        MenuStyle.text(c, x + 1, y + 2, w - 2, "T global / Y team", MenuStyle.MUTED);
        MenuStyle.text(c, x + 1, y + 3, w - 2, "Lokale chat", MenuStyle.MUTED);
        int lineWidth = Math.max(1, w - 2);
        List<String> lines = wrappedLines(lineWidth);
        int room = Math.max(0, h - 10), first = Math.max(0, lines.size() - room);
        for (int i = first; i < lines.size(); i++) MenuStyle.text(c, x + 1, y + 5 + i - first, w - 2, lines.get(i), MenuStyle.INK);
        if (editing) {
            MenuStyle.text(c, x + 1, y + h - 4, w - 2, "SCHRIJVEN / " + (team ? "TEAM" : "GLOBAL"), MenuStyle.AMBER);
            String visible = draft.substring(Math.max(0, draft.length() - Math.max(1, w - 5)));
            MenuStyle.text(c, x + 1, y + h - 3, w - 2, "> " + visible + "_", MenuStyle.INK);
            MenuStyle.text(c, x + 1, y + h - 2, w - 2, "Enter: zend", MenuStyle.MUTED);
            MenuStyle.text(c, x + 1, y + h - 1, w - 2, "Esc: sluit", MenuStyle.MUTED);
        } else {
            MenuStyle.text(c, x + 1, y + h - 3, w - 2, "[T] global", MenuStyle.AMBER);
            MenuStyle.text(c, x + 1, y + h - 2, w - 2, "[Y] team", MenuStyle.MUTED);
        }
    }
    /** Compact chat for the lobby, with a permanent input row and a separate history per channel. */
    void drawLobby(Canvas c, int x, int y, int w, int h) {
        MenuStyle.card(c, x, y, w, h, "LOBBYCHAT / " + (team ? "TEAM" : "GLOBAL"));
        MenuStyle.text(c, x + 2, y + 1, w - 4, editing
            ? "Enter: zend | Esc: annuleer" : "Lokale chat | T global / Y team", MenuStyle.MUTED);
        List<String> lines = wrappedLines(Math.max(1, w - 4));
        int room = Math.max(0, h - 5), first = Math.max(0, lines.size() - room);
        if (lines.isEmpty()) MenuStyle.text(c, x + 2, y + 2, w - 4, "Nog geen berichten.", MenuStyle.MUTED);
        for (int i = first; i < lines.size(); i++)
            MenuStyle.text(c, x + 2, y + 2 + i - first, w - 4, lines.get(i), MenuStyle.INK);
        if (editing) {
            MenuStyle.fill(c, x + 2, y + h - 2, w - 4, 1, MenuStyle.LINE);
            String visible = draft.substring(Math.max(0, draft.length() - Math.max(1, w - 7)));
            MenuStyle.text(c, x + 2, y + h - 2, w - 4, "> " + visible + "_", MenuStyle.INK);
        } else MenuStyle.text(c, x + 2, y + h - 2, w - 4, team ? "[Y] Typ een teambericht" : "[T] Typ een bericht", MenuStyle.AMBER);
    }
    private List<String> wrappedLines(int lineWidth) {
        List<String> lines = new ArrayList<>();
        for (Message message : messages) if (message.team == team) {
            for (int at = 0; at < message.text.length();) {
                int end = Math.min(message.text.length(), at + lineWidth);
                if (end < message.text.length()) {
                    int space = message.text.lastIndexOf(' ', end);
                    if (space > at) end = space;
                }
                lines.add(message.text.substring(at, end));
                at = end;
                while (at < message.text.length() && message.text.charAt(at) == ' ') at++;
            }
        }
        return lines;
    }
}
