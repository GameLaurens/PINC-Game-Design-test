# Terminal Sprite Lab

Een kleine Java-applicatie om **tien werkende, bestuurbare terminaldemo's** te vergelijken. Alles verschijnt in dezelfde terminal: ASCII, Unicode, RGB-blokken en PNG-sprites. Geen Swing/JavaFX-venster, Sixel of Kitty Graphics.

## Starten in PowerShell

Installeer een **JDK 17 of nieuwer** en **Maven 3.9+** en zorg dat `java`, `javac` en `mvn` op PATH staan. Open PowerShell in Windows Terminal in deze projectmap:

```powershell
.\run.cmd
```

Het script bouwt bij de eerste start, voert de tests uit en start de uitvoerbare JAR. Het werkt ook wanneer PowerShell het uitvoeren van `.ps1`-bestanden blokkeert. Maven downloadt dependencies naar `.cache/maven` in dit project; daarna is geen internet nodig. Na broncodewijzigingen:

```powershell
mvn '-Dmaven.repo.local=.cache/maven' package
.\run.cmd
```

Of bouw en start rechtstreeks, zonder het script:

```powershell
mvn package
java --enable-native-access=ALL-UNNAMED -jar target/terminal-sprite-lab.jar
```

De JAR bevat de runtime-dependencies en voorbeeldsprites; op een andere machine is alleen Java 17+ nodig. `--enable-native-access` geeft JLine toestemming zijn native terminalbackend te gebruiken en voorkomt de bijbehorende waarschuwing op recente Java-versies. Er wordt geen software geïnstalleerd door de applicatie.

Gebruik een monospace-lettertype met bloktekens, bijvoorbeeld Cascadia Mono of Consolas. Voor alle demo's en de vergelijking met 32×32 is **140 kolommen × 50 rijen** prettig. Voor de vergelijking met 16×16 is 96×32 voldoende. Verklein eventueel de tekst met Ctrl + muiswiel in Windows Terminal. Een te klein venster geeft een melding; `-` kiest een kleinere sprite. De applicatie schaalt de terminalafmetingen dynamisch mee, tot 240×100 gebruikte cellen.

Een eigen PNG laden:

```powershell
.\run.cmd --png 'C:\sprites\mijn-held.png' --size 16
# Of gebruik de meegeleverde afbeelding expliciet als PNG-invoer:
.\run.cmd --png '.\src\main\resources\sprites\hero-16.png'
# Zonder script:
java --enable-native-access=ALL-UNNAMED -jar target/terminal-sprite-lab.jar --png 'C:\sprites\held.png' --size 32
```

Een eigen PNG wordt gebruikt in demo's 5–9; demo 4 houdt de ingebouwde gekleurde held. PNG's mogen maximaal 4096 pixels per zijde, 4.194.304 pixels totaal en 8 MiB zijn. De afbeelding wordt met nearest-neighbor passend gemaakt op het gekozen vierkante raster, met behoud van verhoudingen en transparante opvulling. Een onjuist bestand geeft een fout vóórdat de terminalinstellingen worden gewijzigd.

Een bestand toevoegen aan `src/main/resources/sprites` selecteert het niet automatisch. Geef het pad mee bij het starten, bijvoorbeeld:

```powershell
.\run.cmd --png '.\src\main\resources\sprites\poppetje.png' --size 32
```

Kies daarna **5** voor de PNG-demo. Een al draaiende game moet eerst met **Q** worden afgesloten en opnieuw worden gestart. Kies een voldoende hoog venster voor 32×32, of druk op **-** om naar 16×16 te gaan. Een grote foto kan zo geladen worden, maar wordt in de huidige demo nog steeds naar maximaal 32×32 beeldpunten verkleind.

Er is ook een optioneel PowerShell-script: `.\run.ps1 -Png 'pad.png' -Size 16`, met `-Build` om opnieuw te bouwen. Als de bestaande execution policy dit blokkeert, gebruik dan `run.cmd` of het rechtstreekse Java-commando hierboven.

## Bediening

| Toets | Actie |
|---|---|
| `1`–`9` | Open meteen een demo, ook vanuit een andere demo |
| `0` | Open de pedit5-variant: amber avonturier met zwaard en schild |
| `W A S D` | Beweeg omhoog, links, omlaag en rechts, zonder Enter |
| `+` / `-` | Kies 8×8, 16×16 of 32×32 pixels; positie wordt hersteld |
| `B` | Wissel tussen dambordachtergrond en egale grond |
| `P` | Schakel de eenvoudige loopanimatie in/uit |
| `R` | Herstel de beginpositie |
| `M` / `Esc` | Terug naar het hoofdmenu |
| `Q` / `Ctrl+C` / `Ctrl+D` | Sluit af en herstel de terminal |

Alle demo's hebben een achtergrond, muren en bij voldoende ruimte enkele bruine obstakels. Botsingen gebruiken het **hele rechthoekige kader** van de weergave, ook de transparante randen. In de vergelijking bewegen de drie exemplaren tegelijk en gebruiken ze hetzelfde grootste botsingskader. Daardoor blijven posities en obstakels gelijk, terwijl het verschil in hoogte zichtbaar blijft.

De ingebouwde held heeft twee beenposes tijdens beweging. De tekstpoppetjes hebben ook twee poses. Een eigen PNG blijft ongewijzigd; er is geen aanname over waar de benen zitten. Beweging gebeurt per ontvangen teken. Een ingedrukte toets gebruikt de herhaling van het besturingssysteem; het programma ontvangt geen betrouwbare key-up-events of gelijktijdige WASD-toestanden.

## De tien demo's

| Menu | Techniek | Wat je ziet |
|---|---|---|
| 1 | Eenvoudige ASCII | `O`, `/`, `|`, `\` in een figuurtje van 3×3 |
| 2 | Gedetailleerde ASCII | Gezicht, jas, armen en schoenen over meerdere regels |
| 3 | Unicode-art | `█`, `▀`, `▄`, `▐`, `▌` en kleine gezichtselementen |
| 4 | Gekleurde Unicode | RGB-held met haar, huid, turquoise jas, riem, broek en schoenen |
| 5 | PNG → terminal | Echte PNG-decoder, alpha-compositie en twee pixels per cel |
| 6 | `█` + voorgrondkleur | Eén bronpixel per terminalcel |
| 7 | Spatie + achtergrondkleur | Eén bronpixel per terminalcel |
| 8 | `▀` + voor- en achtergrondkleur | Twee verticale bronpixels per terminalcel |
| 9 | Vergelijking naast elkaar | Dezelfde sprite, achtergrond, invoer en positie met methodes 6–8 |
| 0 | Pedit5-stijl | Amberkleurig lijnpoppetje met zwaard en schild op zwart, getekend met `▀` |

Demo's 4 en 6 gebruiken bewust dezelfde blokrenderer: 4 toont de gekleurde voorbeeldheld, 6 is onderdeel van de rastervergelijking en kan een eigen PNG gebruiken. Demo's 5 en 8 delen de halfblokrenderer: PNG is de invoerbron; de manier van tekenen is daarvan onafhankelijk.

### Pedit5-poppetje proberen

Start met `.\run.cmd` en druk op **`0`**. Je ziet een eigen interpretatie van het kleine avonturiersfiguurtje uit pedit5: een hoofd, eenvoudige tuniek, benen, een lang zwaard links en een rond schild rechts. De amberkleurige lijnen op een zwarte achtergrond sluiten aan bij de [gameplayscreenshots van pedit5](https://howtomakeanrpg.com/r/l/g/the-dungeon/). Het is een nieuw getekende variant, geen exacte kopie of PLATO-emulator.

Beweeg met **WASD**, wissel met **+ / -** tussen 8×8, 16×16 en 32×32 en gebruik **P** voor de loopanimatie. Op 32×32 zijn de dunne lijnen en het schild het duidelijkst; op 8×8 vallen kleine details samen. **B** zet een subtiel amber dambord aan om de transparantie te bekijken. Het poppetje gebruikt de bestaande halfblokrenderer, botsingen en verschillenbuffer. Een met `--png` gekozen bestand vervangt dit personage niet.

De contouren staan in [`PeditCharacter.java`](src/main/java/nl/pinc/terminallab/PeditCharacter.java) als lijnsegmenten. Ze worden op ieder formaat opnieuw gerasterd met lijnen van één bronpixel breed, zodat het een fijn lijnfiguurtje blijft. De twee loopposes veranderen alleen de benen.

**Visueel verschil:** terminalcellen zijn doorgaans ongeveer twee keer zo hoog als breed. Zowel `█` als een gekleurde spatie vult één hele cel, waardoor de sprite verticaal uitgerekt is. Deze twee methodes horen bij een goed lettertype vrijwel hetzelfde te ogen; eventuele naden/verschillen komen uit de glyphweergave. `▀` verdeelt dezelfde cel in twee kleuren, waardoor bronpixels veel dichter bij vierkant komen. Het precieze resultaat hangt af van lettertype, regelafstand en terminal.

## Spriteformaten

De drie echte RGBA-bestanden staan in [`src/main/resources/sprites`](src/main/resources/sprites). Ze zijn oorspronkelijke, lokaal gemaakte pixel-art en mogen met dit project worden aangepast en gebruikt.

| Bronraster | `█` of achtergrond, cellen | `▀`, cellen | Praktische invloed |
|---|---|---|---|
| 8×8 | 8×8 | 8×4 | Compact en herkenbaar; weinig ruimte voor gezicht/kleding |
| 16×16 | 16×16 | 16×8 | Goede balans tussen details en speelveld |
| 32×32 | 32×32 | 32×16 | Meer details, maar viermaal zoveel bronpixels als 16×16 |

8×8 en 16×16 zijn apart ontworpen. De 32×32-variant bouwt voort op de 16×16-sprite met extra haar-, oog-, rits- en schoendetails en een deels transparante schaduw. De generatiesource staat in [`tools/generate_sprites.py`](tools/generate_sprites.py); opnieuw genereren kan met Python 3, zonder packages. Voor het starten is Python niet nodig. Bij eigen PNG's vergelijkt `+`/`-` de resampling van dezelfde bronafbeelding. Tekstpoppetjes hebben een vaste tekengrootte en veranderen niet mee.

## Transparantie en tekenpijplijn

1. `Sprite` leest PNG met de standaard Java ImageIO-decoder naar ARGB-pixels.
2. `World` tekent grond en obstakels in een nieuwe `Canvas`.
3. `TextArt` of een afzonderlijke `PixelRenderer` tekent daaroverheen.
4. `AnsiDisplay` vergelijkt met het vorige complete frame en schrijft alleen gewijzigde cellen, met absolute cursorposities en samengevoegde aangrenzende cellen.

Volledig transparante pixels laten de achtergrond staan. Deels transparante pixels worden per RGB-kanaal gemengd: `(bron × alpha + achtergrond × (255-alpha)) / 255`, afgerond. Bij `▀` gebeurt dat apart voor de bovenste en onderste helft. Bij een oneven bronhoogte blijft de ontbrekende onderste pixel transparant. ANSI zelf kent geen alpha: de applicatie berekent de uiteindelijke kleuren vooraf. Een deels bedekte cel behoudt de achtergrondkleur; een tekstteken onder zo'n cel kan niet tegelijk behouden blijven. Deze proefomgeving gebruikt daarom egale grondcellen.

RGB wordt uitgegeven met `ESC[38;2;R;G;Bm` voor de voorgrond en `ESC[48;2;R;G;Bm` voor de achtergrond. `▀` gebruikt de bovenste pixel als voorgrond en de onderste als achtergrond. Er zijn geen afbeeldingsprotocollen nodig.

Er is één initiële schermwisactie bij het openen. Beweging wist het scherm niet: opnieuw samengestelde achtergrondcellen herstellen vanzelf de oude spritepositie. Het alternatieve scherm houdt de oorspronkelijke terminalinhoud apart. De cursor is tijdens het spelen verborgen. De lus wacht maximaal ongeveer 33 ms op invoer en tekent normaal circa 30 keer per seconde; identieke frames leveren geen uitvoer op. Een statusregel toont eenmaal per seconde het aantal gegenereerde ANSI-bytes; dit is een praktische indicator, geen FPS- of netwerkbenchmark.

## Realtime invoer op Windows

`System.in` en `Scanner` werken in de normale console doorgaans regelgebaseerd. Alleen vaker `available()` aanroepen verandert dat niet. Daarom gebruikt dit project **JLine Terminal 3.30.17 met de JNI-backend**: `jline-terminal`, `jline-terminal-jni` en transitief `jline-native`. Er zijn geen andere runtime-libraries nodig, en er wordt geen shellproces gestart voor elke toets.

JLine schakelt de systeemterminal naar raw mode. De applicatie zet ook `ISIG` uit om Ctrl+C als invoerteken te verwerken, zodat afsluiten niet afhankelijk is van het doorgeven van een native consolesignaal. De `NonBlockingReader` leest tekens met een timeout, zodat toetsen, animatie en vensterwijzigingen in één lus verwerkt kunnen worden. De provider voert de platformafhankelijke consoleaanroepen uit. De Maven Shade-build neemt de serviceproviderregistratie en native resources mee in de uitvoerbare JAR.

Zie de officiële [JLine Terminal-documentatie](https://jline.org/versions/3.x/docs/terminal/), [raw-mode-interface](https://github.com/jline/jline3/blob/jline-3.30.17/terminal/src/main/java/org/jline/terminal/Terminal.java) en [JNI-module](https://github.com/jline/jline3/blob/jline-3.30.17/terminal-jni/pom.xml). Deze versie heeft een eigen JNI-provider; oudere voorbeelden die uitsluitend JNA of Jansi noemen zijn daarvoor niet volledig.

`TerminalSession` bewaart de originele attributen en herstelt die via `try-with-resources`, ook bij een fout. Een shutdown-hook probeert hetzelfde bij JVM-afsluiting. Kleuren worden gereset, de cursor wordt zichtbaar en het oorspronkelijke scherm keert terug. Een geforceerd beëindigd proces, stroomuitval of een gecrashte terminal kan geen Java-cleanup uitvoeren.

Gebruik een interactieve terminal, niet de outputconsole van een IDE of een pipe. `TERM=dumb` dwingt in JLine zelfs met `dumb(false)` een regelgebaseerde fallback af. De applicatie weigert die met een duidelijke melding. Mocht deze variabele verkeerd staan terwijl je echt Windows Terminal gebruikt, dan kun je voor die PowerShell-sessie instellen:

```powershell
$env:TERM = 'xterm-256color'
.\run.cmd
```

De naam `xterm-256color` beschrijft hier het terminaltype; de renderer blijft expliciete 24-bit RGB-codes gebruiken. Een terminal die geen True Color ondersteunt kan kleuren reduceren. Zonder Unicode-blokken blijft ASCII de bruikbare tekenvariant; ook de spelomgeving vereist ANSI-cursorbesturing.

## Voor- en nadelen

| Methode | Voordelen | Nadelen |
|---|---|---|
| ASCII | Breed beschikbare glyphs; weinig spritecellen; leesbaar bij beperkte kleurondersteuning | Weinig detail; symbolische vorm; verticale verhoudingen blijven beperkt |
| Unicode-art | Meer contouren en details bij een klein aantal cellen | Ontwerp per teken; font/glyphbreedte en bloknaden kunnen verschillen |
| True Color `█` | Eenvoudige rasterrenderer; veel kleuren | Hoge cellen rekken pixels uit; afhankelijk van een goed gevuld blokglyph |
| True Color achtergronden | Eenvoudig; hele cel zonder blokglyph | Zelfde verticale uitrekking; veel cellen voor grote sprites |
| PNG + `▀` | Gewone pixel-art-workflow; dubbele verticale resolutie; natuurlijke verhoudingen; alpha vooraf te mengen | Twee kleuren per cel; compositing/diff-rendering nodig; groter beeld vraagt meer terminalwerk |

## Advies voor een realtime multiplayer-terminalgame

Voor een game die zich op Windows Terminal en vergelijkbare moderne terminals richt, kies ik **16×16 PNG-sprites met True Color-halfblokken (`▀`)**, met ASCII als optionele weergavevariant voor beperkte clients. Een held gebruikt dan 16×8 terminalcellen en heeft genoeg ruimte voor herkenbare kleding en een gezicht. 8×8 is bruikbaar voor een groot speelveld; 32×32 is vooral geschikt voor portretten, een detailweergave of een zeer groot terminalvenster.

Dit is een ontwerpaanbeveling op basis van pixelverhoudingen, celgebruik en deze proefopstelling, geen gemeten multiplayerbenchmark. Halveren van het aantal cellen halveert niet automatisch het aantal bytes: halfblokken hebben vaak twee kleurwisselingen per cel, en compressie en het aantal gewijzigde cellen tellen mee. Vergelijk in demo 9 de vormen en test dezelfde bewegingen afzonderlijk in 6–8 met de byte-indicator. Terminal, hardware en verbinding bepalen de werkelijke grens.

Stuur in een multiplayergame logische speltoestand/posities over het netwerk en laat iedere client lokaal renderen. Gebruik een vaste simulatietick, interpolatie en een clientbuffer voor gewijzigde cellen. Vermijd volledige PNG's of complete ANSI-schermen per netwerktick. De huidige demo bevat geen netwerklaag en geen betrouwbare toets-los-events. Voor gelijktijdige richtingen en vloeiende key-down/key-up-besturing moet je een expliciet Windows-console-eventpad toevoegen of een invoermodel kiezen dat op losse toetsaanslagen is ontworpen.

## Opbouw en controle

| Klasse | Verantwoordelijkheid |
|---|---|
| `Main` | Menu, invoer, demo's en formaatwissels |
| `TerminalSession` | Raw mode, cursor, alternatief scherm en herstel |
| `Sprite` | PNG-decoder, schalen en ingebouwde beenpose |
| `PixelRenderer` | Drie los te testen pixelmethodes |
| `TextArt` | ASCII- en Unicode-poses |
| `PeditCharacter` | Pedit5-geïnspireerde amber lijnsprite en twee beenposes |
| `Canvas` / `Cell` | Volledig samengesteld frame met kleuren |
| `AnsiDisplay` | Verschillenbuffer en ANSI-uitvoer |
| `World` | Achtergrond, obstakels en botsingen |

```powershell
mvn test
# Dezelfde projectlokale cache als de startscripts gebruiken:
mvn '-Dmaven.repo.local=.cache/maven' test
```

De automatische tests controleren de drie pixelstrategieën, transparantie (ook gedeeltelijk en bij oneven hoogtes), PNG-decoding, afmetingen/aspectratio, ongeldige invoer, clipping, obstakels, schermverschillen zonder wisactie en beweging in alle tien demo's op alle drie formaten. Voor het pedit5-personage controleren ze ook de amberkleur, transparante gaten, stabiele bovenlichaamcontouren en verschillende beenposes. Ze vereisen geen echte terminal. De interactieve Windows-controle moet daarnaast de native invoer en herstelinstellingen toetsen: start de JAR, kies een demo zonder Enter, beweeg met WASD, vergelijk 6/7/8/9, probeer 0, wissel formaat, keer terug naar het menu en sluit af met Q of Ctrl+C.
