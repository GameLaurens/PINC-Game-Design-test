package nl.pinc.terminallab;

/** Reusable, printable ASCII models and weapon rules for future levels. */
public final class ShowroomCatalog {
    private ShowroomCatalog() { }

    public enum Monster {
        SLIME("Slijm", 0x8ddc79, "Traag moerasmonster", new AsciiModel(
            " .----.",
            "/ o  o \\",
            "\\__ww__/"
        )),

        GOBLIN("Goblin", 0xf0916c, "Kleine schatdief", new AsciiModel(
            "\\^___^/",
            " (o.o)",
            " /|#|\\",
            "  / \\"
        )),

        SKELETON("Skelet", 0xdce8ee, "Ondode bewaker", new AsciiModel(
            " .---.",
            " |o o|",
            "  \\-/",
            " /|+|\\",
            "  / \\"
        )),

        BAT("Vleermuis", 0xb59deb, "Vliegende grotbewoner", new AsciiModel(
            " /\\     /\\",
            "/  \\_o_/  \\",
            "\\__/ V \\__/"
        )),

        SPIDER("Spin", 0xccab83, "Achtpotige hinderlaag", new AsciiModel(
            "\\ \\ | / /",
            " \\(o o)/",
            " /(___)\\",
            "/ / | \\ \\"
        )),

        GHOST("Spook", 0x9de7e7, "Zwevende geest", new AsciiModel(
            " .---.",
            "/ o o \\",
            "|  O  |",
            "|     |",
            "'v-v-v'"
        )),

        ORC("Ork", 0x82b568, "Gepantserde krijger", new AsciiModel(
            "  /^^\\",
            " [o  o]",
            "  |vv|",
            " /[##]\\",
            "  /  \\"
        )),

        TROLL("Trol", 0xa0b47b, "Grote brugbewaker", new AsciiModel(
            "  .----.",
            " / o  o \\",
            " |  __  |",
            "/|######|\\",
            "  /|  |\\"
        )),

        WOLF("Wolf", 0xaebdcb, "Snelle roedeljager", new AsciiModel(
            " /\\_/\\",
            "( o.o )___",
            " > ^     _)",
            "  / / / /"
        )),

        MIMIC("Mimic", 0xdba361, "Schatkist met tanden", new AsciiModel(
            " .------.",
            "/__o__o_/|",
            "| VVVVV ||",
            "|_^_^_^_|/"
        )),

        GOLEM("Golem", 0xb6a395, "Levende steenreus", new AsciiModel(
            "  [----]",
            "  [o__o]",
            " /[####]\\",
            "[_|####|_]",
            "  []  []"
        )),

        SERPENT("Slang", 0xd1cb69, "Giftige kronkelaar", new AsciiModel(
            "  __",
            " /oo\\__",
            " \\__/  )",
            "   / /",
            "  ( (___",
            "   '----'"
        )),

        IMP("Duiveltje", 0xed8075, "Kleine vuurduivel", new AsciiModel(
            " ^   ^",
            "(o   o) /",
            " \\_v_/ /",
            " /|*|\\/",
            "  / \\"
        )),

        BEETLE("Kever", 0x78bca7, "Hard gepantserd insect", new AsciiModel(
            "  \\ /",
            " /o|o\\",
            "<(#|#)>",
            " /#|#\\",
            "  / \\"
        )),

        DRAGON("Draak", 0xf3b56d, "Gevleugelde eindbaas", new AsciiModel(
            " /\\   /\\",
            "/  \\^/  \\",
            "\\  (oo) /",
            " \\/|vv|/",
            "   /##\\__>",
            "  / / \\"
        ));


        public final String label, description;
        public final int color;
        public final AsciiModel model;
        Monster(String label, int color, String description, AsciiModel model) {
            this.label = label; this.color = color; this.description = description; this.model = model;
        }
    }

    public enum Shape { LINE, FAN, BURST }

    public enum Weapon {
        DAGGER("Dolk", 3, 1, 180, 0, Shape.LINE, new AsciiModel(
            " /",
            "-+",
            " |"
        )),

        SHORTSWORD("Kortzwaard", 5, 2, 280, 1, Shape.LINE, new AsciiModel(
            "  /",
            " /",
            "-+-",
            " |"
        )),

        LONGSWORD("Langzwaard", 8, 4, 450, 1, Shape.LINE, new AsciiModel(
            "   /",
            "  /",
            " /",
            "-+-",
            " |"
        )),

        GREATSWORD("Slagzwaard", 13, 5, 850, 2, Shape.FAN, new AsciiModel(
            "  /\\",
            "  ||",
            "  ||",
            "--++--",
            "  ||"
        )),

        RAPIER("Rapier", 6, 6, 330, 0, Shape.LINE, new AsciiModel(
            "    /",
            "   /",
            "  /",
            "-(+)-",
            "  |"
        )),

        KATANA("Katana", 9, 3, 300, 2, Shape.FAN, new AsciiModel(
            "   /",
            "  /",
            " /",
            "=+",
            " |"
        )),

        AXE("Bijl", 11, 2, 650, 2, Shape.FAN, new AsciiModel(
            " /\\",
            "|##)",
            " |",
            " |"
        )),

        HAMMER("Oorlogshamer", 16, 2, 1100, 0, Shape.BURST, new AsciiModel(
            "[####]",
            "  ||",
            "  ||",
            "  ||"
        )),

        SPEAR("Speer", 7, 8, 550, 0, Shape.LINE, new AsciiModel(
            "  ^",
            " /|\\",
            "  |",
            "  |",
            "  |"
        )),

        HALBERD("Hellebaard", 12, 6, 800, 2, Shape.FAN, new AsciiModel(
            "  ^",
            " /|#)",
            "  |",
            "  |",
            "  |"
        )),

        WHIP("Zweep", 4, 9, 400, 2, Shape.LINE, new AsciiModel(
            " .--.",
            "(  /",
            " \\/",
            " /",
            " |"
        )),

        BOW("Boog", 6, 16, 500, 0, Shape.LINE, new AsciiModel(
            " )\\",
            " )-=>",
            " )/"
        )),

        CROSSBOW("Kruisboog", 15, 20, 1200, 0, Shape.LINE, new AsciiModel(
            " \\|/",
            "--+-->",
            " /|\\",
            "  |"
        )),

        STAFF("Vuurstaf", 10, 12, 700, 1, Shape.LINE, new AsciiModel(
            " (*)",
            "  |",
            "  |",
            "  |"
        )),

        BOMB("Bom", 20, 4, 1600, 0, Shape.BURST, new AsciiModel(
            "  *",
            " /",
            "(##)",
            " \\__/"
        ));


        public final String label;
        public final int damage, range, cooldownMs, halfWidth;
        public final Shape shape;
        public final AsciiModel model;
        Weapon(String label, int damage, int range, int cooldownMs, int halfWidth, Shape shape, AsciiModel model) {
            this.label = label; this.damage = damage; this.range = range; this.cooldownMs = cooldownMs;
            this.halfWidth = halfWidth; this.shape = shape; this.model = model;
        }
        public String stats() { return damage + " dmg | " + range + " bereik | " + cooldownMs + "ms"; }
        public String style() {
            return switch (shape) {
                case LINE -> halfWidth == 0 ? "rechte lijn" : "brede baan";
                case FAN -> "waaier";
                case BURST -> "rondom jezelf";
            };
        }
    }
}
