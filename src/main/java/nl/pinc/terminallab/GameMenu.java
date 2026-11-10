package nl.pinc.terminallab;

import java.util.ArrayList;
import java.util.List;

/** Local UI prototype: room discovery and code execution are not network/runtime services. */
final class GameMenu {
    enum Screen { TITLE, HOME, CREATE, SAVES, LOBBY, AGENT, CUSTOM, PLAY }
    enum LobbyRole { HOST, GUEST }
    enum GameStatus {
        IN_LOBBY("In Lobby"), IN_GAME("In Game");
        final String label;
        GameStatus(String label) { this.label = label; }
    }
    static final int HOME_ACTIONS = 5, AGENT_ACTION = 3, CUSTOM_ACTION = 4;
    static final String[] GAMES = {"ASCII Adventure", "Free for All",
        "Unicode Playground", "Monster Gallery", "Weapon Lab",
        "Foreground Pixels", "Background Pixels", "Halfblock Pixels",
        "Renderer Comparison", "Amber Adventure", "Weapon Range Editor", "Unicode Monster Gallery", "Open World Explorer",
        "Small ASCII Gallery"};
    private record Player(String name, boolean bot) { }
    private record Room(String name, int game, boolean open, int capacity, GameStatus status, String host, List<Player> players) {
        boolean joinable() { return open && players.size() < capacity; }
    }
    private static final List<Room> ROOMS = List.of(
        new Room("Open Arena", 2, true, 8, GameStatus.IN_LOBBY, "Rook", bots()),
        new Room("Adventure Room", 1, true, 4, GameStatus.IN_GAME, "Nora", List.of(new Player("Nora", false), new Player("Teun", false))),
        new Room("Weapon Sandbox", 5, true, 2, GameStatus.IN_GAME, "Bo", List.of(new Player("Bo", false), new Player("Liv", false))));
    private Screen screen = Screen.TITLE;
    private int selected, game = 2, strategy, startRequest, joinRequest;
    private boolean openGame = true, currentOpen = true, quit;
    private LobbyRole lobbyRole = LobbyRole.HOST;
    private GameStatus gameStatus = GameStatus.IN_LOBBY;
    private List<Player> players = List.of(new Player("JIJ", false));
    private String hostName = "JIJ";
    private GameChat lobbyChat = new GameChat();
    private final GameDraft draft = new GameDraft();
    private String roomName = "Nieuwe sessie";
    private String notice = "";
    Screen screen() { return screen; }
    int game() { return game; }
    boolean quitRequested() { return quit; }
    String roomName() { return roomName; }
    boolean isHost() { return lobbyRole == LobbyRole.HOST; }
    void playing(int mode) { lobbyChat.close(); game = mode; gameStatus = GameStatus.IN_GAME; screen = Screen.PLAY; }
    void home() { lobbyChat.close(); screen = Screen.HOME; selected = 0; startRequest = joinRequest = 0; notice = ""; }
    void lobby() { lobbyChat.close(); screen = Screen.LOBBY; selected = 0; startRequest = joinRequest = 0; }
    private static List<Player> bots() {
        return List.of(new Player("Rook", true), new Player("Nyx", true), new Player("Bram", true), new Player("Iris", true));
    }
    private void joinLobby(Room room, LobbyRole role) {
        lobbyRole = role; hostName = room.host; roomName = room.name; game = room.game; gameStatus = room.status; currentOpen = room.open;
        players = new ArrayList<>(room.players);
        if (players.stream().noneMatch(p -> p.name.equals("JIJ"))) players.add(0, new Player("JIJ", false));
        players = List.copyOf(players);
        lobbyChat = new GameChat(); screen = Screen.LOBBY; selected = 0; startRequest = joinRequest = 0;
    }
    int takeStartRequest() { int request = startRequest; startRequest = 0; return request; }
    int takeJoinRequest() { int request = joinRequest; joinRequest = 0; return request; }
    void handle(int input) {
        if (screen == Screen.TITLE) { home(); return; }
        if (screen == Screen.LOBBY && lobbyChat.handle(input)) return;
        if (screen == Screen.CUSTOM && draft.handle(input)) return;
        int key = Character.toLowerCase(input);
        if (screen == Screen.HOME && key == 'q') { quit = true; return; }
        if (key == 27 || key == 'm') {
            if (screen == Screen.AGENT || screen == Screen.CUSTOM) {
                int action = screen == Screen.AGENT ? AGENT_ACTION : CUSTOM_ACTION;
                home(); selected = action;
            }
            else home();
            return;
        }
        if (screen == Screen.PLAY) return;
        if (screen == Screen.CREATE && selected < 2 && (key == 'a' || key == 'd' || key == TerminalKeys.LEFT || key == TerminalKeys.RIGHT)) {
            changeSetting(key == 'a' || key == TerminalKeys.LEFT ? -1 : 1); return;
        }
        int count = switch (screen) {
            case HOME -> HOME_ACTIONS + ROOMS.size(); case LOBBY -> 3;
            case CREATE, AGENT, CUSTOM -> 4; default -> 1;
        };
        notice = "";
        if (key == 'w' || key == 'a' || key == TerminalKeys.UP || key == TerminalKeys.LEFT)
            selected = Math.floorMod(selected - 1, count);
        else if (key == 's' || key == 'd' || key == TerminalKeys.DOWN || key == TerminalKeys.RIGHT || key == '\t')
            selected = (selected + 1) % count;
        else if (key == '\r' || key == '\n') activate();
    }
    private void changeSetting(int direction) {
        if (selected == 0) game = Math.floorMod(game - 1 + direction, GAMES.length) + 1;
        if (selected == 1) openGame = !openGame;
    }
    private void activate() {
        switch (screen) {
            case HOME -> {
                if (selected == 0) { screen = Screen.CREATE; selected = 0; }
                else if (selected == 1) { screen = Screen.SAVES; selected = 0; }
                else if (selected == 2) selected = HOME_ACTIONS;
                else if (selected == AGENT_ACTION) { screen = Screen.AGENT; selected = strategy; }
                else if (selected == CUSTOM_ACTION) { screen = Screen.CUSTOM; selected = 0; }
                else {
                    Room room = ROOMS.get(selected - HOME_ACTIONS);
                    if (!room.joinable()) { notice = "Niet toegankelijk: " + room.name + " is vol of gesloten."; return; }
                    joinLobby(room, LobbyRole.GUEST);
                }
            }
            case CREATE -> {
                if (selected < 2) changeSetting(1);
                else if (selected == 3) home();
                else {
                    List<Player> participants = new ArrayList<>(); participants.add(new Player("JIJ", false));
                    if (game == 2) participants.addAll(bots());
                    joinLobby(new Room("Mijn game", game, openGame, 8, GameStatus.IN_LOBBY, "JIJ", List.copyOf(participants)), LobbyRole.HOST);
                }
            }
            case SAVES -> home();
            case LOBBY -> {
                if (selected == 0 && isHost()) { startRequest = game; gameStatus = GameStatus.IN_GAME; screen = Screen.PLAY; }
                else if (selected == 0 && gameStatus == GameStatus.IN_GAME) { joinRequest = game; screen = Screen.PLAY; }
                else if (selected == 1) lobbyChat.handle('t');
                else if (selected == 2) home();
            }
            case AGENT -> {
                if (selected < 3) strategy = selected;
                else { home(); selected = AGENT_ACTION; }
            }
            case CUSTOM -> {
                if (selected == 0) draft.edit();
                else if (selected == 1) draft.save();
                else if (selected == 2) draft.reset();
                else { home(); selected = CUSTOM_ACTION; }
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
            case HOME -> "GAME BROWSER"; case CREATE -> "GAME AANMAKEN"; case SAVES -> "GAME SAVE LADEN";
            case LOBBY -> "LOBBY / " + roomName; case AGENT -> "AGENT CONFIGUREREN";
            case CUSTOM -> "ZELF GAME MAKEN"; default -> "";
        }, MenuStyle.INK);
        if (w >= 100) MenuStyle.text(c, w - 23, 1, 21, "TERMINAL SANDBOX  0.2", MenuStyle.AMBER);
        switch (screen) {
            case HOME -> homeFrame(c);
            case CREATE -> createFrame(c);
            case SAVES -> savesFrame(c);
            case LOBBY -> lobbyFrame(c);
            case AGENT -> agentFrame(c);
            case CUSTOM -> customFrame(c);
            default -> { }
        }
        MenuStyle.fill(c, 0, h - 1, w, 1, MenuStyle.LINE);
        String hint = screen == Screen.CUSTOM && draft.editing()
            ? "CODE | Ctrl+S: bewaren | Esc: klaar | Pijlen: cursor"
            : screen == Screen.LOBBY
            ? lobbyChat.editing() ? "CHAT ACTIEF | Enter: versturen | Esc: annuleren"
                : "WASD/pijlen kiezen | Enter bevestigen | T/Y chat | Esc terug"
            : screen == Screen.HOME ? "WASD/pijlen: kiezen | Enter: openen | Q: afsluiten"
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
        if (w >= 100) {
            int side = 34, right = side + 3, roomWidth = w - right - 1;
            MenuStyle.card(c, 1, 4, side, 16, "SPELEN");
            MenuStyle.choice(c, 3, 6, side - 4, "GAME AANMAKEN", selected == 0);
            MenuStyle.text(c, 5, 7, side - 6, "Voorgemaakte gamemodes", MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 8, side - 4, "GAME SAVE LADEN", selected == 1);
            MenuStyle.text(c, 5, 9, side - 6, "Opgeslagen voortgang", MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 10, side - 4, "DEELNEMEN AAN BESTAANDE GAME", selected == 2);
            MenuStyle.text(c, 5, 11, side - 6, "Kies rechts een actieve game", MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 12, side - 4, "AGENT CONFIGUREREN", selected == AGENT_ACTION);
            MenuStyle.text(c, 5, 13, side - 6, "Profiel: " + profile(), MenuStyle.MUTED);
            MenuStyle.choice(c, 3, 14, side - 4, "ZELF GAME MAKEN", selected == CUSTOM_ACTION);
            MenuStyle.text(c, 5, 15, side - 6, "Bewerk je eigen gameconcept", MenuStyle.MUTED);
            MenuStyle.text(c, 3, 18, side - 4, "Q: afsluiten", MenuStyle.MUTED);
            MenuStyle.card(c, right, 4, roomWidth, 16, "ACTIEVE GAMES");
            MenuStyle.text(c, right + 2, 6, roomWidth - 4, "Lokale voorbeelden / joinstatus staat los van spelstatus", MenuStyle.MUTED);
            for (int i = 0; i < ROOMS.size(); i++) room(c, right + 2, 8 + i * 3, roomWidth - 4, i);
            MenuStyle.text(c, right + 2, 18, roomWidth - 4, "Groen: toegankelijk | Rood: niet toegankelijk", MenuStyle.MUTED);
        } else {
            int column = (w - 6) / 2;
            MenuStyle.card(c, 1, 4, w - 2, 7, "SPELEN");
            MenuStyle.choice(c, 3, 5, column, "GAME AANMAKEN", selected == 0);
            MenuStyle.choice(c, 4 + column, 5, w - column - 7, "GAME SAVE LADEN", selected == 1);
            MenuStyle.choice(c, 3, 7, column, "DEELNEMEN AAN GAME", selected == 2);
            MenuStyle.choice(c, 4 + column, 7, w - column - 7, "AGENT CONFIGUREREN", selected == AGENT_ACTION);
            MenuStyle.choice(c, 3, 9, column, "ZELF GAME MAKEN", selected == CUSTOM_ACTION);
            MenuStyle.text(c, 4 + column, 9, w - column - 7, "Q: afsluiten", MenuStyle.MUTED);
            MenuStyle.card(c, 1, 11, w - 2, 9, "ACTIEVE GAMES / LOKALE VOORBEELDEN");
            for (int i = 0; i < ROOMS.size(); i++) room(c, 3, 13 + i * 2, w - 6, i);
        }
        MenuStyle.text(c, 2, 20, w - 4, notice.isEmpty()
            ? "Groene cirkel: je kunt ook deelnemen als de game In Game is." : notice,
            notice.isEmpty() ? MenuStyle.MUTED : MenuStyle.AMBER);
    }
    private void room(Canvas c, int x, int y, int w, int i) {
        Room room = ROOMS.get(i);
        MenuStyle.choice(c, x, y, w, "\u25cf " + room.name, selected == i + HOME_ACTIONS, room.joinable());
        c.put(x + 2, y, new Cell('\u25cf', room.joinable() ? MenuStyle.AVAILABLE : MenuStyle.UNAVAILABLE, c.get(x + 2, y).background()));
        MenuStyle.text(c, x + 2, y + 1, w - 2, GAMES[room.game - 1] + " | Spelers: "
            + room.players.size() + "/" + room.capacity + " | " + room.status.label, MenuStyle.MUTED);
    }
    private void createFrame(Canvas c) {
        int w = c.width(), h = c.height();
        MenuStyle.panel(c, 0, 4, w, h - 6, "GAME INSTELLEN");
        MenuStyle.text(c, 3, 6, w - 6, "A/D of links/rechts: wijzigen. Enter wisselt ook.", MenuStyle.MUTED);
        int gameRow = 8, modeRow = Math.max(10, h / 2 - 2);
        MenuStyle.choice(c, 2, gameRow, w - 4, "GAME   < " + String.format("%02d", game) + " / " + GAMES[game - 1] + " >", selected == 0);
        MenuStyle.text(c, 4, gameRow + 1, w - 8, GAMES.length + " speelbare demo's / terminalrenderers en editors", MenuStyle.MUTED);
        MenuStyle.choice(c, 2, modeRow, w - 4, "TOEGANG  < " + (openGame ? "Open" : "Gesloten") + " >", selected == 1);
        if (h > 25) MenuStyle.text(c, 4, modeRow + 2, w - 8, "Lokale UI-demo. De maker wordt host; Free for All heeft vier bots.", MenuStyle.MUTED);
        MenuStyle.choice(c, 2, h - 6, w - 4, "AANMAKEN EN NAAR LOBBY >>", selected == 2);
        MenuStyle.choice(c, 2, h - 4, w - 4, "TERUG", selected == 3);
    }
    private void savesFrame(Canvas c) {
        int w = c.width(), h = c.height();
        MenuStyle.card(c, 1, 4, w - 2, h - 6, "GAME SAVE LADEN");
        MenuStyle.text(c, 3, 7, w - 6, "Geen opgeslagen games gevonden.", MenuStyle.INK);
        MenuStyle.text(c, 3, 10, w - 6, "Echte voortgang opslaan en laden is nog niet aangesloten.", MenuStyle.MUTED);
        MenuStyle.text(c, 3, 12, w - 6, "Een nieuwe game start je via Game aanmaken.", MenuStyle.MUTED);
        MenuStyle.choice(c, 3, h - 4, w - 6, "TERUG NAAR HOMEPAGE", true);
    }
    private void lobbyFrame(Canvas c) {
        int w = c.width(), side = Math.max(24, Math.min(34, w / 3));
        MenuStyle.card(c, 1, 4, side, 9, "SPELERS / " + players.size());
        for (int i = 0; i < Math.min(players.size(), 6); i++) {
            Player player = players.get(i);
            String label = player.name.equals("JIJ") ? "JIJ / " + (isHost() ? "HOST" : "SPELER")
                : player.name + (player.bot ? " [BOT]" : "") + (player.name.equals(hostName) ? " [HOST]" : "");
            MenuStyle.text(c, 3, 6 + i, side - 4, label, player.name.equals(hostName) ? MenuStyle.GREEN : MenuStyle.INK);
        }
        MenuStyle.text(c, 3, 13, side - 4, "Agent: " + profile(), MenuStyle.MUTED);
        MenuStyle.card(c, 1, 14, side, 6, "ACTIES");
        boolean selecting = !lobbyChat.editing();
        boolean runningGame = gameStatus == GameStatus.IN_GAME;
        String action = isHost() ? "GAME STARTEN >>" : runningGame ? "GAME BINNENGAAN" : "WACHT OP HOST";
        MenuStyle.choice(c, 3, 15, side - 4, action, selecting && selected == 0, isHost() || runningGame);
        MenuStyle.choice(c, 3, 16, side - 4, "CHAT OPENEN", selecting && selected == 1);
        MenuStyle.choice(c, 3, 17, side - 4, "LOBBY VERLATEN", selecting && selected == 2);
        MenuStyle.text(c, 3, 18, side - 4, "Alleen de host start", MenuStyle.MUTED);
        int x = side + 3, cw = w - x - 1;
        MenuStyle.card(c, x, 4, cw, 5, "SESSIE / " + GAMES[game - 1]);
        MenuStyle.text(c, x + 2, 6, cw - 4, "Host: " + hostName + " | " + (currentOpen ? "Open" : "Gesloten"), MenuStyle.MUTED);
        MenuStyle.text(c, x + 2, 7, cw - 4, "Spelstatus: " + gameStatus.label, MenuStyle.MUTED);
        lobbyChat.drawLobby(c, x, 10, cw, 10);
    }
    private String profile() { return new String[]{"Verkenner", "Verdediger", "Jager"}[strategy]; }
    private void customFrame(Canvas c) {
        int w = c.width(), h = c.height(), side = Math.max(24, Math.min(30, w / 3));
        MenuStyle.card(c, 1, 4, side, h - 6, "ZELF GAME MAKEN");
        MenuStyle.choice(c, 3, 6, side - 4, "CODE BEWERKEN", !draft.editing() && selected == 0);
        MenuStyle.choice(c, 3, 9, side - 4, "CONCEPT BEWAREN", !draft.editing() && selected == 1);
        MenuStyle.choice(c, 3, 12, side - 4, "CODE RESETTEN", !draft.editing() && selected == 2);
        MenuStyle.text(c, 3, 14, side - 4, draft.saved() ? "Concept bewaard" : "Niet bewaard", MenuStyle.MUTED);
        MenuStyle.choice(c, 3, h - 4, side - 4, "TERUG", !draft.editing() && selected == 3);
        draft.draw(c, side + 3, 4, w - side - 4, h - 6);
        MenuStyle.text(c, 2, h - 2, w - 4, "Concept blijft in deze sessie; code wordt niet uitgevoerd.", MenuStyle.MUTED);
    }
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
