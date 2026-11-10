package nl.pinc.terminallab;

import java.util.Map;

/** Compact scenery for the open world; sizes stay close to the 5x5 explorer. */
public final class OpenWorldCatalog {
    private OpenWorldCatalog() { }
    public enum Resource { WOOD, STONE, CRYSTAL }
    public enum Biome {
        MEADOW("Zonweide", 0x283c30, 0x304838, 0x789764, 100, 60),
        FOREST("Fluisterbos", 0x192e2c, 0x203932, 0x57836e, 340, 60),
        DESERT("Amberduinen", 0x594635, 0x65503a, 0xb39a6a, 580, 60),
        SWAMP("Mistmoeras", 0x29353c, 0x303e43, 0x65878a, 100, 210),
        TUNDRA("Rijpvlakte", 0x404e5c, 0x485868, 0xa8c8d7, 340, 210),
        VOLCANO("Sintelland", 0x352c32, 0x402e32, 0x8f6467, 580, 210);
        public final String label;
        public final int ground, lightGround, ink, campX, campY;
        Biome(String label, int ground, int lightGround, int ink, int x, int y) {
            this.label = label; this.ground = ground; this.lightGround = lightGround; this.ink = ink;
            campX = x; campY = y;
        }
    }
    public enum Kind {
        TREE("Loofboom", 0x8baa76, 2, Resource.WOOD,
            "  ▄▄▄", "▄█▓▓▓█▄", "▀██▓██▀", "  ▐█▌", "  ▀▀▀"),
        PINE("Dennenboom", 0x719b85, 2, Resource.WOOD,
            "   ▄", "  ▄█▄", " ▄█▓█▄", "▄█▓▓▓█▄", "  ▐█▌"),
        LOG("Omgevallen stam", 0xc4a37a, 2, Resource.WOOD,
            "╭────────╮", "│▓▓╳▓▓╳▓▓│", "╰────────╯"),
        THORNS("Doornhaag", 0xa4af75, 2, Resource.WOOD,
            "╲╱╲╱╲╱╲╱╲╱", "▓╳▓╳▓╳▓╳▓╳", "╱╲╱╲╱╲╱╲╱╲"),
        ROCK("Bros rotsblok", 0xb9ac99, 3, Resource.STONE,
            " ▄██████▄ ", "▐▓▓╱▓▓▓╲▓▌", "▀████████▀"),
        CACTUS("Cactus", 0xa0b780, 1, Resource.WOOD,
            "  ▐█▌", "█ ▐▓▌ █", "▀█▓▓▓█▀", "  ▐▓▌", "  ▀▀▀"),
        PALM("Dadelpalm", 0xb1b77d, 2, Resource.WOOD,
            "▄▄ ╲╱ ▄▄", " ▀███▀", "  ╱█╲", "   █", "  ▄█▄"),
        MUSHROOM("Lichtzwam", 0xbe9fce, 1, Resource.CRYSTAL,
            " ▄◆◆▄", "▀████▀", "  ▐▌"),
        REED("Rietbundel", 0x9bac8c, 1, Resource.WOOD,
            "│ ╷ │", "╲│╱│╱", " ▐▓▌"),
        ICE("IJswand", 0x9ad5e5, 2, Resource.CRYSTAL,
            "◢█◣◢██◣◢█◣", "██╱██╲████", "▀████████▀"),
        CRYSTAL("Kristal", 0xb5b3ed, 2, Resource.CRYSTAL,
            "  ◢◆◣", "◢█▐█▌█◣", "▀█████▀"),
        BASALT("Basaltpuin", 0xa58f9f, 3, Resource.STONE,
            " ▄██▄▄██▄ ", "▐▓╲▓▓▓╱▓▓▌", "▀████████▀"),
        CRATES("Houten barricade", 0xc6a072, 2, Resource.WOOD,
            "╔═══╦════╗", "║╳▓╳║▓╳▓╳║", "╚═══╩════╝"),
        FLOWERS("Veldbloemen", 0xdfb1a2, 0, null,
            " ·◆· ◆", "  ╵ ╵"),
        VENT("Vulkaanschoorsteen", 0xe3a17b, 0, null,
            "  ░▒░", " ▄▓◆▓▄", "▀█████▀"),
        PILLAR("Oude zuil", 0xb4afa0, 0, null,
            " ╔══╗", " ▐▓▓▌", " ▐▓▓▌", "▄████▄"),
        TENT("Verkennerskamp", 0xc6b196, 0, null,
            "   ◢◣", "  ╱◆ ╲", " ╱▓▓▓▓╲", "▀▀▀  ▀▀▀"),
        WELL("Waterput", 0x9ebdc4, 0, null,
            "╭────╮", "│≈≈◆≈│", "╰────╯"),
        WAYSTONE("Reissteen", 0xb5c7e7, 0, null,
            " ▄██▄", " █◆◆█", " ▐▓▓▌", "▄████▄"),
        SIGN("Wegwijzer", 0xd5bd91, 0, null,
            "╞═════►", "│", "┴"),
        CHEST("Ruïnekist", 0xe3be78, 0, null,
            "╔═════╗", "║▓▓◆▓▓║", "╚═════╝");
        public final String label;
        public final int color, durability;
        public final Resource resource;
        public final UnicodeModel model;
        Kind(String label, int color, int durability, Resource resource, String... rows) {
            this.label = label; this.color = color; this.durability = durability; this.resource = resource;
            model = new UnicodeModel(Map.of('◆', 0xffd78a, '▓', darker(color)), rows);
        }
        public boolean solid() { return this != FLOWERS; }
        public boolean interactive() { return durability > 0 || this == CHEST || this == SIGN || this == WAYSTONE || this == WELL; }
    }
    static final UnicodeModel OPEN_CHEST = new UnicodeModel("╱═════╱", "╔═════╗", "╚═════╝");
    private static int darker(int c) { return ((c & 0xfefefe) >> 1) + ((c & 0xfcfcfc) >> 2); }
}
