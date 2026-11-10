package nl.pinc.terminallab;

import java.util.List;
import java.util.Map;

/** Independent bestiary designed around Unicode silhouettes, shading and negative space. */
public final class UnicodeMonsters {
    private UnicodeMonsters() { }
    public static final int MAX_WIDTH = 12, MAX_HEIGHT = 6;
    public record Creature(String label, int color, String description, String technique, UnicodeModel model) { }

    public static final UnicodeModel PLAYER = new UnicodeModel(
        "  ▄▄▄", "  █▪█", " ▄█▀█▄", "  ▐█▌", "  ▀ ▀");
    public static final UnicodeModel WALKING_PLAYER = new UnicodeModel(
        "  ▄▄▄", "  █▪█", " ▀█▀█▀", "  ▐█▌", " ▀   ▀");

    public static final List<Creature> CREATURES = List.of(
        creature("Maanmot", 0xb6a1ef, 0xffdfa6, "Drinkt het licht van slapende sterren", "Halfblokken / spiegeling",
            "▄    ╲╱    ▄",
            "▐██▄ ▐▌ ▄██▌",
            " ▀▓◆████◆▓▀",
            " ▄█▓▀▐▌▀▓█▄",
            " ▀▀  ╱╲  ▀▀"),
        creature("Lantaarneter", 0x72c9b2, 0xffd17c, "Lokt reizigers met een gestolen vlam", "Negatieve ruimte / licht",
            "    ╭──╮",
            " ▄██╯  │",
            "▐█●▓█▌╭┴╮",
            " █▾▾█ │◆│",
            " ▐▓▓▌ ╰─╯",
            " ▀  ▀"),
        creature("Kristalkrab", 0x7ebee9, 0xf1d8ff, "Bewaakt een hart van bevroren licht", "Facetten / brede houding",
            "◢█◣     ◢█◣",
            " ▀█ ◢◆◣ █▀",
            "  ╲▐● ●▌╱",
            " ▄██▓◆▓██▄",
            "╱ ╱▀███▀╲ ╲"),
        creature("Mosrug", 0x91b774, 0xffcf9a, "Een wandelend eiland vol paddenstoelen", "Textuur / organische rug",
            "  ▄◆▄ ▄▄",
            " ▄▐█▌▄▐▌▄",
            "▐█▓▒▓▒▓▓██▄",
            " █▓▓▓▓▓█ ●█",
            " ▀█▓██▓█▀▀",
            "  ▀▀ ▀▀"),
        creature("Droomkwal", 0xc09be6, 0x91ebed, "Bewaart dromen onder een glazen koepel", "Ronde koepel / fijne draden",
            "  ▄▄▄▄▄▄",
            "▄█░◇◆◇░█▄",
            "▀█▄▄▄▄▄▄█▀",
            " ╰╮│╭╯│╭╯",
            "  │╰╮ ╰╮",
            " ╭╯ ╰╯ ╰"),
        creature("Inktorakel", 0x9b9ee9, 0xffb8d2, "Ziet de toekomst door één open oog", "Concentrische vormen",
            "  ▄████▄",
            " ▐▓╭──╮▓▌",
            " █▓│◉ │▓█",
            " ▀█╰──╯█▀",
            "╭╮▄▓██▓▄╭╮",
            "╰╯╰╮╰╯╭╯╰╯"),
        creature("Sterrenrog", 0x79c9d6, 0xffe9ad, "Zweeft geruisloos door de nachtlucht", "Diagonalen / breed silhouet",
            "     ◢◣",
            "◢▄ ▄██▄ ▄◣",
            "▐◆██●●██◆▌",
            " ▀██▓▓██▀",
            "    ▐▌",
            "     ╰─◇"),
        creature("Sintelkroon", 0xf29973, 0xffe3a1, "Een zwevend masker in een kroon van vuur", "Vlamritme / open masker",
            " ▄ ◢◣ ▄",
            " ▀█◆◆█▀",
            " ▐▓●●▓▌",
            "  █▾▾█",
            "  ▀▄▄▀"),
        creature("Diepzeetand", 0x83a7cd, 0x9ff2dd, "Een levende bek uit de donkerste trog", "Tanden / diepe uitsparing",
            " ▄████▄",
            "▐▓●▄▄●▓▌",
            "█▓▾▾▾▾▓█",
            "█▓▴▴▴▴▓█",
            "▀█▓◆◆▓█▀"),
        creature("Klokwerkuil", 0xd2b485, 0xa3e7ed, "Telt elke hartslag in het verlaten bos", "Dubbele oogringen",
            "◢◣    ◢◣",
            "█╭─╮╭─╮█",
            "▐│◉││◉│▌",
            "█╰─╯╰─╯█",
            " ▀█▓◆▓█▀",
            "  ┴┴ ┴┴"),
        creature("Wortelhart", 0xb89c7a, 0xc8ed9b, "Een oude stronk met een nieuwe ziel", "Vertakkingen / kernlicht",
            " ╲╱ ╲╱",
            "  █▄█",
            " ▐▓╭─╮▓▌",
            "  █│◆│█",
            " ▄▓╰─╯▓▄",
            "╱ ╱   ╲ ╲"),
        creature("IJsbloem", 0x9dd9ee, 0xe3c5ff, "Bloeit alleen wanneer de tijd bevriest", "Radiale vorm / kristallen",
            "   ◢◆◣",
            " ◢▄▐█▌▄◣",
            "▀█▓╭─╮▓█▀",
            "▄█▓│◉│▓█▄",
            " ▀▀▐█▌▀▀",
            "    ▀"),
        creature("Fluweelwurm", 0xd694bd, 0xffdea6, "Draagt een hele sterrenhemel op zijn rug", "Asymmetrie / segmenten",
            "╲╱",
            "█●█▄",
            "▐◆▓██▄ ▄◆▄",
            " ▀██▓◆██▓█▌",
            "   ▀▀██▀ ▀▀"),
        creature("Leegteoog", 0xa49be0, 0x9ef4e0, "Kijkt terug vanuit een scheur in de wereld", "Losse fragmenten / iris",
            " ▄▄    ▄▄",
            "  ▄█▀▀█▄",
            "◢▐▓│◉ │▓▌◣",
            "  ▀█▄▄█▀",
            " ▀▀    ▀▀"),
        creature("Nachtvorst", 0x909bd3, 0xf6d7a2, "De laatste koning van een vergeten hemel", "Kroon / gelaagde mantel",
            "  ▄◢◣▄",
            "  ▀◆◆▀",
            "  ▐●●▌",
            " ▄█╲◆╱█▄",
            "▐█▓██▓█▌",
            "▀▀▀  ▀▀▀")
    );

    private static Creature creature(String label, int body, int accent, String description, String technique, String... rows) {
        int shadow = shade(body, 0.65), light = shade(body, 1.22);
        Map<Character, Integer> palette = Map.ofEntries(
            Map.entry('▓', shadow), Map.entry('▒', shade(body, 0.48)), Map.entry('░', light),
            Map.entry('▄', light), Map.entry('▀', light), Map.entry('◆', accent), Map.entry('◇', accent),
            Map.entry('◉', accent), Map.entry('●', 0xfff1d5), Map.entry('▾', 0xfff1d5), Map.entry('▴', 0xfff1d5));
        UnicodeModel model = new UnicodeModel(palette, rows);
        if (model.width() > MAX_WIDTH || model.height() > MAX_HEIGHT)
            throw new IllegalArgumentException(label + " is groter dan het compacte monsterformaat van 12x6 cellen.");
        return new Creature(label, body, description, technique, model);
    }

    private static int shade(int color, double factor) {
        int r = Math.min(255, (int) (((color >> 16) & 255) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((color & 255) * factor));
        return (r << 16) | (g << 8) | b;
    }
}
