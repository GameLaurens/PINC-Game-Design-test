package nl.pinc.terminallab;

import java.util.List;

/** Prototype front end: rooms and saves are explicitly sample data, never network state. */
final class GameMenu {
    enum Screen { TITLE, HOME, CREATE, SAVES, LOBBY, AGENT, PLAY }
    enum LobbyRole { HOST, GUEST }
    static final String[] GAMES = {"ASCII Adventure", "Free for All",
        "Unicode Playground", "Monster Gallery", "Weapon Lab",
        "Foreground Pixels", "Background Pixels", "Halfblock Pixels",
        "Renderer Comparison", "Amber Adventure"};
    private record Room(String name, int game, String detail, String host) { }
    private static final List<Room> ROOMS = List.of(
        new Room("Open Arena", 2, "1 speler + 4 bots | Free for All", "Rook"),
        new Room("Adventure Room", 1, "2 voorbeeldspelers | ASCII-avontuur", "Nora"),
        new Room("Weapon Sandbox", 5, "2 voorbeeldspelers | Wapens testen", "Bo"));
    private Screen screen = Screen.TITLE;
    private int selected, game = 2, strategy, startRequest;
    private boolean multiplayer, quit;
    private LobbyRole lobbyRole = LobbyRole.HOST;
    private String hostName = "JIJ";
    private GameChat lobbyChat = new GameChat();
    private String roomName = "Nieuwe sessie", origin = "Nieuwe sessie";
    Screen screen() { return screen; }
    int game() { return game; }
    boolean quitRequested() { return quit; }
    String roomName() { return roomName; }
    boolean isHost() { return lobbyRole == LobbyRole.HOST; }
    void playing(int mode) { lobbyChat.close(); game = mode; screen = Screen.PLAY; }
    void home() { lobbyChat.close(); screen = Screen.HOME; selected = 0; startRequest = 0; }
    void lobby() { lobbyChat.close(); screen = Screen.LOBBY; selected = 0; startRequest = 0; }
    private void joinLobby(LobbyRole role, String host) {
        lobbyRole = role; hostName = host;
        lobbyChat = new GameChat(); screen = Screen.LOBBY; selected = 0; startRequest = 0;
    }
    int takeStartRequest() { int request = startRequest; startRequest = 0; return request; }
    void handle(int input) {
        if (screen == Screen.TITLE) { home(); return; }
        if (screen == Screen.LOBBY && lobbyChat.handle(input)) return;
        int key = Character.toLowerCase(input);
        if (key == 27 || key == 'm') {
            if (screen == Screen.AGENT) { home(); selected = 5; }
            else home();
            return;
        }
        if (screen == Screen.PLAY) return;
        if (screen == Screen.CREATE && selected < 2 && (key == 'a' || key == 'd' || key == TerminalKeys.LEFT || key == TerminalKeys.RIGHT)) {
            changeSetting(key == 'a' || key == TerminalKeys.LEFT ? -1 : 1); return;
        }
        int count = switch (screen) { case HOME -> 7; case LOBBY -> 3; case CREATE, SAVES, AGENT -> 4; default -> 1; };
        if (key == 'w' || key == 'a' || key == TerminalKeys.UP || key == TerminalKeys.LEFT)
            selected = Math.floorMod(selected - 1, count);
        else if (key == 's' || key == 'd' || key == TerminalKeys.DOWN || key == TerminalKeys.RIGHT || key == '\t')
            selected = (selected + 1) % count;
        else if (key == '\r' || key == '\n') activate();
    }
    private void changeSetting(int direction) {
        if (selected == 0) game = Math.floorMod(game - 1 + direction, GAMES.length) + 1;
        if (selected == 1) multiplayer = !multiplayer;
    }
    private void activate() {
        switch (screen) {
            case HOME -> {
                if (selected == 0) { screen = Screen.CREATE; selected = 0; }
                else if (selected == 1) { screen = Screen.SAVES; selected = 0; }
                else if (selected == 5) { screen = Screen.AGENT; selected = strategy; }
                else if (selected == 6) quit = true;
                else {
                    Room room = ROOMS.get(selected - 2); game = room.game; roomName = room.name;
                    multiplayer = true; origin = "Voorbeeldkamer / lokaal"; joinLobby(LobbyRole.GUEST, room.host);
                }
            }
            case CREATE -> {
                if (selected < 2) changeSetting(1);
                else if (selected == 3) home();
                else { roomName = "Mijn game"; origin = "Nieuwe sessie"; joinLobby(LobbyRole.HOST, "JIJ"); }
            }
            case SAVES -> {
                if (selected == 3) home();
                else {
                    game = new int[]{2, 1, 5}[selected]; multiplayer = false;
                    roomName = new String[]{"Save: Arena", "Save: Adventure", "Save: Weapon Lab"}[selected];
                    origin = "Voorbeeldsave / start een verse demo"; joinLobby(LobbyRole.HOST, "JIJ");
                }
            }
            case LOBBY -> {
                if (selected == 0 && isHost()) { startRequest = game; screen = Screen.PLAY; }
                else if (selected == 1) lobbyChat.handle('t');
                else if (selected == 2) home();
            }
            case AGENT -> {
                if (selected < 3) strategy = selected;
                else { home(); selected = 5; }
            }
            default -> { }
        }
    }
    private long titleStarted = Long.MIN_VALUE;
    Canvas frame(int w, int h) { return frame(w, h, System.nanoTime()); }
    Canvas frame(int w, int h, long now) {
        Canvas c = MenuStyle.canvas(w, h);
        if (w < 64 || h < 22) {
            MenuStyle.text(c, 1, 1, w - 2, "GLYPH ENGINE | Minimaal 64x22", MenuStyle.AMBER); return c;
        }
        if (screen == Screen.TITLE) { title(c, now); return c; }
        // Keep controls together even in a maximized terminal. Extra space stays outside the page.
        int pageWidth = Math.min(w, 112);
        int pageHeight = Math.min(h, screen == Screen.HOME || screen == Screen.LOBBY ? 22 : 26);
        Canvas page = MenuStyle.canvas(pageWidth, pageHeight);
        menuFrame(page);
        c.blit(page, (w - pageWidth) / 2, Math.min(2, h - pageHeight));
        return c;
    }
    private void menuFrame(Canvas c) {
        int w = c.width(), h = c.height();
        MenuStyle.fill(c, 0, 0, w, 3, MenuStyle.PANEL);
        MenuStyle.text(c, 2, 1, w >= 100 ? w - 28 : w - 4, "GLYPH ENGINE / " + switch (screen) {
            case HOME -> "GAME BROWSER"; case CREATE -> "NIEUWE GAME"; case SAVES -> "SAVES";
            case LOBBY -> "LOBBY / " + roomName; case AGENT -> "AGENT EDITOR"; default -> "";
        }, MenuStyle.INK);
        if (w >= 100) MenuStyle.text(c, w - 23, 1, 21, "TERMINAL SANDBOX  0.2", MenuStyle.AMBER);
        switch (screen) {
            case HOME -> homeFrame(c);
            case CREATE -> createFrame(c);
            case SAVES -> savesFrame(c);
            case LOBBY -> lobbyFrame(c);
            case AGENT -> agentFrame(c);
            default -> { }
        }
        MenuStyle.fill(c, 0, h - 1, w, 1, MenuStyle.LINE);
        String hint = screen == Screen.LOBBY
            ? lobbyChat.editing() ? "CHAT ACTIEF | Enter: versturen | Esc: annuleren"
                : "WASD/pijlen kiezen | Enter bevestigen | T/Y chat | Esc terug"
            : "WASD / pijlen: kiezen    ENTER: bevestigen    ESC: terug";
        MenuStyle.text(c, 2, h - 1, w - 4, hint, MenuStyle.INK);
    }
    private void title(Canvas c, long now) {
        if (titleStarted == Long.MIN_VALUE) titleStarted = now;
        int scale = PixelTitle.scale(c.width(), c.height());
        int amplitude = Math.max(1, Math.min(3, c.height() / 18));
        int bob = (int) Math.round(Math.sin((now - titleStarted) / 1_000_000_000.0 * Math.PI / 2) * amplitude);
        int top = Math.max(3, (c.height() - 7 * scale) / 2 - 3) + bob;
        // Quiet horizontal bands frame the moving original pixel lettering.
        MenuStyle.fill(c, 0, 0, c.width(), 2, MenuStyle.PANEL);
        MenuStyle.fill(c, 0, c.height() - 3, c.width(), 3, MenuStyle.PANEL);
        PixelTitle.draw(c, top, scale);
        MenuStyle.center(c, top + 7 * scale + 2, "E N G I N E", MenuStyle.AMBER);
        MenuStyle.center(c, c.height() - 6, "[ DRUK OP EEN TOETS ]", MenuStyle.INK);
        MenuStyle.center(c, c.height() - 2, "GLYPH ENGINE / TERMINAL SANDBOX", MenuStyle.MUTED);
    }
    private void homeFrame(Canvas c) {
        int w = c.width();
        if (w >= 96) {
            int side = 32, right = side + 3, roomWidth = w - right - 1;
            MenuStyle.card(c, 1, 4, side, 16, "SPELEN");
            MenuStyle.text(c, 3, 6, side - 4, "Jouw terminal. Jouw wereld.", MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 8, side - 4, "NIEUWE GAME AANMAKEN", selected == 0);
            MenuStyle.text(c, 5, 9, side - 6, "Stel een sessie in", MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 11, side - 4, "OUDE SAVE LADEN", selected == 1);
            MenuStyle.text(c, 5, 12, side - 6, "Open een voorbeeldsave", MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 14, side - 4, "AGENT CONFIGUREREN", selected == 5);
            MenuStyle.text(c, 5, 15, side - 6, "Profiel: " + profile(), MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 17, side - 4, "AFSLUITEN", selected == 6);
            MenuStyle.card(c, right, 4, roomWidth, 16, "ACTIEVE GAMES / VOORBEELDSERVERS");
            MenuStyle.text(c, right + 2, 6, roomWidth - 4, "Lokale kamers / kies een game en druk Enter", MenuStyle.MUTED);
            for (int i = 0; i < ROOMS.size(); i++) room(c, right + 2, 8 + i * 3, roomWidth - 4, i);
            MenuStyle.text(c, right + 2, 17, roomWidth - 4, "Je start steeds een verse demo.", MenuStyle.MUTED);
        } else {
            int column = (w - 6) / 2;
            MenuStyle.card(c, 1, 4, w - 2, 6, "SPELEN");
            MenuStyle.choice(c, 3, 6, column, "NIEUWE GAME AANMAKEN", selected == 0);
            MenuStyle.choice(c, 4 + column, 6, w - column - 7, "OUDE SAVE LADEN", selected == 1);
            MenuStyle.choice(c, 3, 8, column, "AGENT CONFIGUREREN", selected == 5);
            MenuStyle.text(c, 4 + column, 8, w - column - 7, "Profiel: " + profile(), MenuStyle.MUTED);
            MenuStyle.card(c, 1, 10, w - 2, 9, "ACTIEVE GAMES / VOORBEELDSERVERS");
            for (int i = 0; i < ROOMS.size(); i++) room(c, 3, 12 + i * 2, w - 6, i);
            MenuStyle.choice(c, 2, 19, w - 4, "AFSLUITEN", selected == 6);
        }
    }
    private void room(Canvas c, int x, int y, int w, int i) {
        Room room = ROOMS.get(i);
        MenuStyle.choice(c, x, y, w, "[OPEN] " + room.name, selected == i + 2);
        MenuStyle.text(c, x + 2, y + 1, w - 2, room.detail, MenuStyle.MUTED);
    }
    private void createFrame(Canvas c) {
        int w = c.width(), h = c.height();
        MenuStyle.panel(c, 0, 4, w, h - 6, "GAME INSTELLEN");
        MenuStyle.text(c, 3, 6, w - 6, "A/D of links/rechts: wijzigen. Enter wisselt ook.", MenuStyle.MUTED);
        int gameRow = 8, modeRow = Math.max(10, h / 2 - 2);
        MenuStyle.choice(c, 2, gameRow, w - 4, "GAME   < " + String.format("%02d", game) + " / " + GAMES[game - 1] + " >", selected == 0);
        MenuStyle.text(c, 4, gameRow + 1, w - 8, "10 speelbare demo's / originele terminalrenderers", MenuStyle.MUTED);
        MenuStyle.choice(c, 2, modeRow, w - 4, "MODUS  < " + (multiplayer ? "Multiplayer (prototype)" : "Singleplayer") + " >", selected == 1);
        if (h > 25) MenuStyle.text(c, 4, modeRow + 2, w - 8, "Free for All: jij + vier bots. Multiplayer is lokaal gesimuleerd.", MenuStyle.MUTED);
        MenuStyle.choice(c, 2, h - 6, w - 4, "AANMAKEN EN NAAR LOBBY >>", selected == 2);
        MenuStyle.choice(c, 2, h - 4, w - 4, "TERUG", selected == 3);
    }
    private void savesFrame(Canvas c) {
        int w = c.width(), h = c.height();
        MenuStyle.panel(c, 0, 4, w, h - 6, "SAVE LADEN");
        MenuStyle.text(c, 3, 6, w - 6, "Voorbeeldsaves: open een verse demo via de lobby.", MenuStyle.MUTED);
        String[] slots = {"01 / Free for All", "02 / ASCII Adventure", "03 / Weapon Lab"};
        for (int i = 0; i < 3; i++) {
            int top = 8 + i * Math.max(3, (h - 14) / 3);
            MenuStyle.choice(c, 2, top, w - 4, slots[i], selected == i);
            MenuStyle.text(c, 4, top + 1, w - 8, "[DEMO SAVE]  Enter: openen", MenuStyle.MUTED);
        }
        MenuStyle.choice(c, 2, h - 4, w - 4, "TERUG", selected == 3);
    }
    private void lobbyFrame(Canvas c) {
        int w = c.width(), side = Math.max(24, Math.min(34, w / 3));
        MenuStyle.card(c, 1, 4, side, 9, "SPELERS / " + (game == 2 ? "5" : isHost() ? "1" : "2"));
        MenuStyle.text(c, 3, 6, side - 4, isHost() ? "JIJ / HOST" : "JIJ / SPELER", MenuStyle.INK);
        MenuStyle.text(c, 3, 7, side - 4, isHost() ? "Jij kunt starten" : "Host: " + hostName, MenuStyle.GREEN);
        if (game == 2) {
            String[] bots = {"Rook", "Nyx", "Bram", "Iris"};
            for (int i = 0; i < bots.length; i++)
                MenuStyle.text(c, 3, 8 + i, side - 4, "[BOT] " + bots[i]
                    + (!isHost() && bots[i].equals(hostName) ? " [HOST]" : ""), MenuStyle.INK);
        } else if (!isHost()) {
            MenuStyle.text(c, 3, 9, side - 4, hostName + " [HOST]", MenuStyle.INK);
        } else {
            MenuStyle.text(c, 3, 10, side - 4, "Geen bots in demo", MenuStyle.MUTED);
        }
        MenuStyle.text(c, 3, 13, side - 4, "Agent: " + profile(), MenuStyle.MUTED);
        MenuStyle.card(c, 1, 14, side, 6, "ACTIES");
        boolean selecting = !lobbyChat.editing();
        MenuStyle.choice(c, 3, 15, side - 4, isHost() ? "GAME STARTEN >>" : "WACHT OP HOST", selecting && selected == 0, isHost());
        MenuStyle.choice(c, 3, 16, side - 4, "CHAT OPENEN", selecting && selected == 1);
        MenuStyle.choice(c, 3, 17, side - 4, "LOBBY VERLATEN", selecting && selected == 2);
        MenuStyle.text(c, 3, 18, side - 4, "Alleen de host start", MenuStyle.MUTED);
        int x = side + 3, cw = w - x - 1;
        MenuStyle.card(c, x, 4, cw, 5, "SESSIE / " + GAMES[game - 1]);
        MenuStyle.text(c, x + 2, 6, cw - 4, multiplayer ? "Multiplayer-UI / lokaal" : "Singleplayer", MenuStyle.MUTED);
        MenuStyle.text(c, x + 2, 7, cw - 4, origin.startsWith("Voorbeeldsave") ? "Voorbeeldsave / verse demo" : origin, MenuStyle.MUTED);
        lobbyChat.drawLobby(c, x, 10, cw, 10);
    }
    private String profile() { return new String[]{"Verkenner", "Verdediger", "Jager"}[strategy]; }
    private void agentFrame(Canvas c) {
        int w = c.width(), h = c.height(), side = Math.max(22, w / 3);
        MenuStyle.panel(c, 0, 4, side, h - 6, "PROFIEL");
        String[] names = {"Verkenner", "Verdediger", "Jager"};
        for (int i = 0; i < 3; i++) MenuStyle.choice(c, 1, 7 + i * 3, side - 2,
            (strategy == i ? "[X] " : "[ ] ") + names[i], selected == i);
        MenuStyle.choice(c, 1, h - 4, side - 2, "OPSLAAN / TERUG", selected == 3);
        MenuStyle.panel(c, side + 1, 4, w - side - 1, h - 6, "AGENT SCRIPT / VOORBEELD");
        int x = side + 3, cw = w - x - 2;
        MenuStyle.text(c, x, 7, cw, "01  agent " + names[strategy].toLowerCase() + " {", MenuStyle.AMBER);
        MenuStyle.text(c, x, 9, cw, "02    on_tick() {", MenuStyle.INK);
        MenuStyle.text(c, x, 11, cw, "03      " + new String[]{"verken();", "bewaak_positie();", "zoek_doelwit();"}[strategy], MenuStyle.INK);
        MenuStyle.text(c, x, 12, cw, "04      " + new String[]{"verzamel_xp();", "bescherm_team();", "val_aan();"}[strategy], MenuStyle.INK);
        MenuStyle.text(c, x, 14, cw, "05    }", MenuStyle.INK);
        MenuStyle.text(c, x, 15, cw, "06  }", MenuStyle.AMBER);
        MenuStyle.text(c, x, h - 5, cw, "UI-prototype: code wordt niet uitgevoerd.", MenuStyle.MUTED);
    }
}
