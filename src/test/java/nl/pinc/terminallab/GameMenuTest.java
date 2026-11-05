package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GameMenuTest {
    private static final long NOW = 10_000_000_000L;
    private static void enter(GameMenu menu) { menu.handle('\r'); }
    private static GameMenu hostLobby() {
        GameMenu menu = new GameMenu(); menu.home(); enter(menu);
        menu.handle('s'); menu.handle('s'); enter(menu);
        return menu;
    }
    private static String rows(Canvas c) {
        StringBuilder s = new StringBuilder();
        for (int y = 0; y < c.height(); y++) {
            for (int x = 0; x < c.width(); x++) s.append(c.get(x, y).glyph());
            s.append('\n');
        }
        return s.toString();
    }
    @Test void anyTitleKeyOnlyDismissesTitleAndEveryDemoCanBeCreatedAndStarted() {
        for (int mode = 1; mode <= 10; mode++) {
            GameMenu menu = new GameMenu();
            assertTrue(rows(menu.frame(100, 30)).contains("GLYPH ENGINE"));
            menu.handle('q'); assertEquals(GameMenu.Screen.HOME, menu.screen());
            assertFalse(menu.quitRequested());
            enter(menu); assertEquals(GameMenu.Screen.CREATE, menu.screen());
            while (menu.game() != mode) menu.handle(TerminalKeys.RIGHT);
            menu.handle('s'); menu.handle('d'); // select multiplayer
            menu.handle(TerminalKeys.DOWN); enter(menu);
            assertEquals(GameMenu.Screen.LOBBY, menu.screen());
            assertTrue(rows(menu.frame(100, 30)).contains("Multiplayer-UI"));
            assertTrue(menu.isHost());
            assertFalse(rows(menu.frame(64, 22)).contains("GEREED"));
            enter(menu); // host starts immediately
            assertEquals(GameMenu.Screen.PLAY, menu.screen());
            assertEquals(mode, menu.takeStartRequest()); assertEquals(0, menu.takeStartRequest());
        }
    }
    @Test void savesAndExampleRoomsOpenTheirOwnLobbyAndAgentSelectionIsRemembered() {
        for (int index = 0; index < 3; index++) {
            GameMenu menu = new GameMenu(); menu.handle(' ');
            for (int i = 0; i < 5; i++) menu.handle('s');
            enter(menu); assertEquals(GameMenu.Screen.AGENT, menu.screen());
            menu.handle('s'); enter(menu); // defender
            menu.handle('s'); menu.handle('s'); enter(menu); // save profile
            assertEquals(GameMenu.Screen.HOME, menu.screen());
            assertTrue(rows(menu.frame(100, 30)).contains("Profiel: Verdediger"));
            menu.home();
            menu.handle('s'); enter(menu); // saves
            for (int i = 0; i < index; i++) menu.handle('s');
            enter(menu);
            assertEquals(new int[]{2, 1, 5}[index], menu.game());
            assertTrue(rows(menu.frame(100, 30)).contains("Voorbeeldsave"));
            assertTrue(rows(menu.frame(100, 30)).contains("Agent: Verdediger"));
            assertFalse(rows(menu.frame(100, 30)).contains("EIGEN AGENT"));
            assertTrue(menu.isHost());
            enter(menu);
            assertEquals(new int[]{2, 1, 5}[index], menu.takeStartRequest());
            menu.lobby();
            menu.handle(27); // home
            for (int i = 0; i < index + 2; i++) menu.handle(TerminalKeys.DOWN);
            enter(menu);
            assertEquals(GameMenu.Screen.LOBBY, menu.screen());
            assertEquals(new int[]{2, 1, 5}[index], menu.game());
            assertFalse(menu.isHost());
            enter(menu);
            assertEquals(0, menu.takeStartRequest());
            assertEquals(GameMenu.Screen.LOBBY, menu.screen());
        }
    }
    @Test void guestsCannotStartThroughEnterNavigationOrReturningToLobbyAndCreatingMakesThemHost() {
        for (int room = 0; room < 3; room++) {
            GameMenu menu = new GameMenu(); menu.home();
            for (int i = 0; i < room + 2; i++) menu.handle('s');
            enter(menu);
            String lobby = rows(menu.frame(64, 22));
            assertFalse(menu.isHost());
            assertTrue(lobby.contains("JIJ / SPELER"));
            assertTrue(lobby.contains("Host: " + new String[]{"Rook", "Nora", "Bo"}[room]));
            assertTrue(lobby.contains("WACHT OP HOST"));
            assertFalse(lobby.contains("GAME STARTEN"));
            assertFalse(lobby.contains("GEREED"));
            for (int attempt = 0; attempt < 5; attempt++) {
                enter(menu);
                assertEquals(0, menu.takeStartRequest());
                assertEquals(GameMenu.Screen.LOBBY, menu.screen());
                menu.handle('\t'); menu.handle('\t'); menu.handle('\t');
            }
            menu.lobby(); enter(menu);
            assertFalse(menu.isHost());
            assertEquals(0, menu.takeStartRequest());
            // Leaving and creating a new room grants host rights for that new room only.
            menu.handle('w'); enter(menu); // wrapped to leave
            assertEquals(GameMenu.Screen.HOME, menu.screen());
            enter(menu); menu.handle('s'); menu.handle('s'); enter(menu);
            assertTrue(menu.isHost());
            int game = menu.game();
            enter(menu);
            assertEquals(game, menu.takeStartRequest());
            assertEquals(GameMenu.Screen.PLAY, menu.screen());
        }
        GameMenu menu = hostLobby(); enter(menu); // leave an unconsumed host command
        menu.home(); menu.handle('s'); menu.handle('s'); enter(menu); // join as guest
        assertFalse(menu.isHost());
        assertEquals(0, menu.takeStartRequest(), "Een startopdracht mag niet naar een andere lobby lekken");
    }
    @Test void menuNavigationWrapsAndEscapeReturnsToTheParent() {
        GameMenu menu = new GameMenu(); menu.handle(' ');
        menu.handle(TerminalKeys.UP); enter(menu); assertTrue(menu.quitRequested());
        menu = new GameMenu(); menu.handle(' '); enter(menu);
        menu.handle('a'); assertEquals(1, menu.game());
        menu.handle('a'); assertEquals(10, menu.game());
        menu.handle(27); assertEquals(GameMenu.Screen.HOME, menu.screen());
    }
    @Test void agentEditorReturnsToHomeAndRemembersTheChosenProfileWhenReopened() {
        GameMenu menu = new GameMenu(); menu.home();
        for (int i = 0; i < 5; i++) menu.handle('s');
        enter(menu);
        menu.handle('s'); menu.handle('s'); enter(menu); // hunter
        menu.handle(27);
        assertEquals(GameMenu.Screen.HOME, menu.screen());
        assertTrue(rows(menu.frame(64, 22)).contains("Profiel: Jager"));
        enter(menu); // focus returned to the agent button
        assertEquals(GameMenu.Screen.AGENT, menu.screen());
        assertTrue(rows(menu.frame(100, 30)).contains("[X] Jager"));
        menu.handle('s'); enter(menu); // save / back
        assertEquals(GameMenu.Screen.HOME, menu.screen());
    }
    @Test void lobbyChatButtonCapturesNavigationAndEscapeOnlyClosesTheDraft() {
        GameMenu menu = new GameMenu(); menu.home();
        menu.handle('s'); menu.handle('s'); enter(menu); // Open Arena
        menu.handle('s'); enter(menu); // chat button
        assertTrue(rows(menu.frame(64, 22)).contains("CHAT ACTIEF"));
        for (char key : "wasd qrm 123".toCharArray()) menu.handle(key);
        assertEquals(GameMenu.Screen.LOBBY, menu.screen());
        assertFalse(menu.quitRequested());
        assertTrue(rows(menu.frame(64, 22)).contains("> wasd qrm 123_"));
        enter(menu);
        assertEquals(GameMenu.Screen.LOBBY, menu.screen());
        assertEquals(0, menu.takeStartRequest());
        assertTrue(rows(menu.frame(64, 22)).contains("JIJ: wasd qrm 123"));
        assertTrue(rows(menu.frame(64, 22)).contains("WACHT OP HOST"));
        menu.handle('t'); menu.handle('x'); menu.handle(27);
        assertEquals(GameMenu.Screen.LOBBY, menu.screen());
        assertFalse(rows(menu.frame(64, 22)).contains("CHAT ACTIEF"));
        menu.handle(27);
        assertEquals(GameMenu.Screen.HOME, menu.screen());
    }
    @Test void submittingChatDoesNotStartHostGameAndChannelsAndRoomsHaveSeparateHistory() {
        GameMenu menu = hostLobby(); // start selected
        menu.handle('t');
        for (char key : "Hallo lobby".toCharArray()) menu.handle(key);
        enter(menu);
        assertEquals(0, menu.takeStartRequest());
        assertEquals(GameMenu.Screen.LOBBY, menu.screen());
        assertTrue(rows(menu.frame(64, 22)).contains("JIJ: Hallo lobby"));
        menu.handle('y');
        assertFalse(rows(menu.frame(64, 22)).contains("JIJ: Hallo lobby"));
        for (char key : "Teambericht".toCharArray()) menu.handle(key);
        enter(menu);
        assertTrue(rows(menu.frame(64, 22)).contains("JIJ: Teambericht"));
        menu.handle('t'); menu.handle(27);
        assertTrue(rows(menu.frame(64, 22)).contains("JIJ: Hallo lobby"));
        assertFalse(rows(menu.frame(64, 22)).contains("JIJ: Teambericht"));
        enter(menu); // now start explicitly
        assertEquals(2, menu.takeStartRequest());
        menu.lobby();
        assertTrue(rows(menu.frame(64, 22)).contains("JIJ: Hallo lobby"));
        menu.handle(27); // home
        menu.handle('s'); menu.handle('s'); menu.handle('s'); enter(menu); // different room
        assertFalse(rows(menu.frame(64, 22)).contains("JIJ: Hallo lobby"));
    }
    @Test void applicationLobbyChatIsSeparateFromGameChatAndCannotStartGameWhileTyping() throws Exception {
        Main app = new Main(null, "test", 16);
        app.handle(' '); app.handle('\r'); // create a room as host
        app.handle('s'); app.handle('s'); app.handle('\r'); // start selected
        app.handle('t');
        for (char key : "Hallo qrm wasd123".toCharArray()) app.handle(key);
        app.handle('\r');
        String lobby = rows(app.frame(80, 24, NOW, ""));
        assertTrue(lobby.contains("LOBBYCHAT / GLOBAL"));
        assertTrue(lobby.contains("JIJ: Hallo qrm wasd123"));
        app.handle('\r');
        String game = rows(app.frame(140, 38, NOW, ""));
        assertTrue(game.contains("FREE FOR ALL"));
        assertFalse(game.contains("Hallo qrm wasd123"));
        app.handle(27);
        assertTrue(rows(app.frame(80, 24, NOW, "")).contains("JIJ: Hallo qrm wasd123"));
    }
    @Test void fullApplicationRouteEntersGameAndChatCapturesMenuAndQuitKeys() throws Exception {
        Main app = new Main(null, "test", 16);
        app.handle(' '); app.handle('\r'); // home -> create, default FFA
        app.handle('s'); app.handle('s'); app.handle('\r'); // lobby
        app.handle('\r'); // host starts
        String field = rows(app.frame(140, 38, NOW, ""));
        assertTrue(field.contains("FREE FOR ALL")); assertTrue(field.contains("Skill upgrades:"));
        assertTrue(field.contains("LEADERBOARD")); assertTrue(field.contains("Health Regen"));
        assertTrue(field.contains("CHAT / GLOBAL"));
        app.handle('t');
        for (char ch : "wasdq123m hello".toCharArray()) app.handle(ch);
        app.handle('\r');
        String chat = rows(app.frame(140, 38, NOW, ""));
        assertTrue(chat.contains("JIJ: wasdq123m")); assertTrue(chat.contains("hello"));
        app.handle('y'); app.handle(27); // cancel chat only
        assertTrue(rows(app.frame(140, 38, NOW, "")).contains("FREE FOR ALL"));
        app.handle(27); assertTrue(rows(app.frame(100, 30, NOW, "")).contains("LOBBY /"));
        app.handle(27); assertTrue(rows(app.frame(100, 30, NOW, "")).contains("GAME BROWSER"));
    }
    @Test void allScreensAndGameShellsSurviveResizeIncludingOpenChat() throws Exception {
        Main app = new Main(null, "test", 32);
        int[][] sizes = {{1, 1}, {63, 21}, {64, 22}, {80, 24}, {114, 28}, {140, 50}, {240, 100}};
        for (int[] size : sizes) assertDoesNotThrow(() -> app.frame(size[0], size[1], NOW, ""));
        for (int mode = 1; mode <= 10; mode++) {
            app.openDemo(mode);
            for (int[] size : sizes) {
                assertDoesNotThrow(() -> app.frame(size[0], size[1], NOW, ""));
                app.handle('t'); assertDoesNotThrow(() -> app.frame(size[0], size[1], NOW, "")); app.handle(27);
            }
        }
        GameMenu menu = new GameMenu(); menu.handle(' ');
        for (int target = 0; target < 6; target++) {
            menu.home();
            for (int i = 0; i < target; i++) menu.handle('s');
            enter(menu);
            for (int[] size : sizes) assertDoesNotThrow(() -> menu.frame(size[0], size[1]));
            if (menu.screen() == GameMenu.Screen.LOBBY) {
                menu.handle('t');
                for (int[] size : sizes) assertDoesNotThrow(() -> menu.frame(size[0], size[1]));
            }
        }
    }
    @Test void upgradesCanBePurchasedWithoutOpeningTheShop() {
        FreeForAll world = new FreeForAll(NOW, false); world.player().totalXp = 40;
        FreeForAllView view = new FreeForAllView(world);
        for (int key = '1'; key <= '4'; key++) assertTrue(view.handle(key, NOW));
        assertEquals(0, world.player().availableXp());
        for (var upgrade : FreeForAll.Upgrade.values()) assertEquals(1, world.player().level(upgrade));
    }
    @Test void chatSeparatesChannelsEditsCancelsAndLimitsItsDraft() {
        GameChat chat = new GameChat(); assertFalse(chat.handle('w'));
        chat.handle('t'); chat.handle('a'); chat.handle('b'); chat.handle(127); chat.handle('c');
        assertEquals("ac", chat.draft()); chat.handle('\r');
        Canvas c = new Canvas(30, 20); chat.draw(c, 0, 0, 30, 20);
        assertTrue(rows(c).contains("JIJ: ac"));
        chat.handle('y');
        for (int i = 0; i < 200; i++) chat.handle('z');
        assertEquals(160, chat.draft().length());
        chat.handle(27); assertFalse(chat.editing()); assertEquals("", chat.draft());
        c = new Canvas(30, 20); chat.draw(c, 0, 0, 30, 20);
        assertFalse(rows(c).contains("JIJ: ac"));
    }
}
