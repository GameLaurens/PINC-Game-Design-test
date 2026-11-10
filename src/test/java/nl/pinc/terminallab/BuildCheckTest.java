package nl.pinc.terminallab;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BuildCheckTest {
    @Test void launchCheckRendersEveryDemoWithoutOpeningATerminal() throws Exception {
        assertEquals(GameMenu.GAMES.length, BuildCheck.verify());
    }
}
