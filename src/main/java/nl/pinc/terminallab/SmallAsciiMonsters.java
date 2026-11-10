package nl.pinc.terminallab;

import java.util.List;

/** Tiny ASCII creatures measured against the visible 3x3 stick figure, without collision padding. */
public final class SmallAsciiMonsters {
    private SmallAsciiMonsters() { }
    public static final int PLAYER_WIDTH = TextArt.ASCII.width(), PLAYER_HEIGHT = TextArt.ASCII.height();
    public record Creature(String label, String description, int color, AsciiModel model) {
        public String dimensions() { return model.width() + "x" + model.height(); }
        public String scale() { return model.width() + "/3 breed, " + model.height() + "/3 hoog"; }
    }

    public static final List<Creature> CREATURES = List.of(
        creature("Druppel", "Een levend druppeltje met een rond oog", 0x9dd4c0, "o"),
        creature("Wervel", "Een opgerolde geest in een enkel teken", 0xc3afe5, "@"),
        creature("Knabbel", "Twee kaken, verder niets", 0xe4b184, "><"),
        creature("Slijmoog", "Een klein oog in een zacht lijf", 0xb9ce8a, "(o)"),
        creature("Vlerkje", "Een vliegend puntje met scherpe vleugels", 0xc5afdf, "^v^"),
        creature("Stekel", "Een stekelig zaadje op korte pootjes", 0xaec68c, "\\|/", "/o\\"),
        creature("Oogtor", "Een oplettend insect met drie poten", 0x99c8cb, "o_o", "/|\\"),
        creature("Miniimp", "Kleine hoorns en een grote nieuwsgierigheid", 0xe4a493, "\\^/", "(o)", "/ \\"),
        creature("Nachtmot", "Brede vleugels rond een slaperig gezicht", 0xbcafd9, "\\ ^ /", "(o_o)", "/ v \\"),
        creature("Waas", "Een zwevend spookje met rafelige voeten", 0xafdae0, " .-.", "(oo)", " vv"),
        creature("Duinling", "Een kleine woestijnbewaker", 0xd7bc90, " ^ ^", "(o.o)", "/|#|\\", " / \\"),
        creature("Kever", "Een stevig schild op dunne pootjes", 0xa4c7b0, " /\\", "(oo)", "/##\\", " ||"),
        creature("Schaduw", "Een lange schim met een getande glimlach", 0xb1b7d3, " /\\", "/oo\\", "\\vv/", "/||\\", " ||"),
        creature("Hoornwacht", "Een gehoornde wachter met een borstplaat", 0xc9ad96, "^   ^", "(o o)", " \\v/", "/|#|\\", " / \\"),
        creature("Brulbek", "De grootste bewoner van deze kleine galerij", 0xddad91, " .-.", "|o o|", "| v |", "/|#|\\", " / \\")
    );

    private static Creature creature(String label, String description, int color, String... rows) {
        AsciiModel model = new AsciiModel(rows);
        if (3 * model.width() < PLAYER_WIDTH || 3 * model.width() > 5 * PLAYER_WIDTH
            || 3 * model.height() < PLAYER_HEIGHT || 3 * model.height() > 5 * PLAYER_HEIGHT)
            throw new IllegalArgumentException(label + " valt buiten 1/3 tot 5/3 van de zichtbare speler.");
        return new Creature(label, description, color, model);
    }
}
