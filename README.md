# Terminal Sprite Lab

Een kleine Java-applicatie om **tien werkende, bestuurbare terminaldemo's** te vergelijken. Alles verschijnt in dezelfde terminal: ASCII, Unicode, RGB-blokken en PNG-sprites. Geen Swing/JavaFX-venster, Sixel of Kitty Graphics.

## Starten in PowerShell

Installeer een **JDK 17 of nieuwer** en **Maven 3.9+** en zorg dat `java`, `javac` en `mvn` op PATH staan. Open PowerShell in Windows Terminal in deze projectmap:

```powershell
.\run.cmd
```

Het script bouwt bij de eerste start en automatisch wanneer bestanden in `src` of `pom.xml` nieuwer zijn dan de gebouwde JAR. Bij een build worden ook de tests uitgevoerd. Zo worden wijzigingen aan bijvoorbeeld `ShowroomCatalog.java` en nieuwe versies na een git pull meegenomen. Het werkt ook wanneer PowerShell het uitvoeren van `.ps1`-bestanden blokkeert. Maven downloadt dependencies naar `.cache/maven` in dit project; daarna is geen internet nodig. Handmatig opnieuw bouwen kan ook:

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

Een eigen PNG wordt gebruikt in demo's 6–9. Demo's 4 en 5 zijn volledig tekstgebaseerde showrooms en gebruiken geen PNG. PNG's mogen maximaal 4096 pixels per zijde, 4.194.304 pixels totaal en 8 MiB zijn. De afbeelding wordt met nearest-neighbor passend gemaakt op het gekozen vierkante raster, met behoud van verhoudingen en transparante opvulling. Een onjuist bestand geeft een fout vóórdat de terminalinstellingen worden gewijzigd.

Een bestand toevoegen aan `src/main/resources/sprites` selecteert het niet automatisch. Geef het pad mee bij het starten, bijvoorbeeld:

```powershell
.\run.cmd --png '.\src\main\resources\sprites\poppetje.png' --size 32
```

Kies daarna **8** voor de PNG-demo met twee pixels per cel. Een al draaiende game moet eerst met **Q** worden afgesloten en opnieuw worden gestart. Kies een voldoende hoog venster voor 32×32, of druk op **-** om naar 16×16 te gaan. Een grote foto kan zo geladen worden, maar wordt in de huidige demo nog steeds naar maximaal 32×32 beeldpunten verkleind.

Er is ook een optioneel PowerShell-script: `.\run.ps1 -Png 'pad.png' -Size 16`, met `-Build` om opnieuw te bouwen. Als de bestaande execution policy dit blokkeert, gebruik dan `run.cmd` of het rechtstreekse Java-commando hierboven.

## Level 1: monsters, kist en wapens

Start met `.\run.cmd` en kies **1**. Dit level gebruikt alleen ASCII-tekens voor de modellen, met ANSI-kleuren voor herkenbaarheid. Het werkt in PowerShell binnen Windows Terminal of WezTerm. Er is geen afbeelding of afbeeldingsprotocol nodig.

Je vindt een **slijm**, **goblin** en **skelet**, een schatkist en twee wapens op de grond. Het bekende kleine `O /|\ / \`-poppetje blijft de speler. Een uitgerust wapen wordt ernaast getekend; het zwaard is vier regels hoog tegenover drie regels voor de speler.

| Toets | Actie in level 1 |
|---|---|
| WASD | Bewegen, zonder Enter |
| **E** | Een nabijgelegen wapen oppakken of de kist openen |
| **F** | Wisselen tussen verzamelde wapens |
| **Spatie** | Het dichtstbijzijnde monster binnen bereik aanvallen |
| B / P | Achtergrond / eenvoudige beenanimatie wisselen |
| **R** | Het level opnieuw beginnen, inclusief buit en monsters |
| M / Q | Hoofdmenu / afsluiten |

Het **zwaard** en de **bijl** verdwijnen van de grond zodra je ze oppakt en worden meteen uitgerust. De **kist** krijgt een open model en geeft eenmalig **5 goud en een dolk**. Eerder verzamelde wapens blijven beschikbaar via F. Onder het speelveld staan je huidige wapen, goud, aantal monsters en een melding over de laatste handeling.

| Wapen | Schade per aanval | Bereik buiten het spelerskader |
|---|---:|---:|
| Zwaard | 2 | 2 cellen |
| Bijl | 3 | 1 cel |
| Dolk | 1 | 1 cel |

De monsters hebben respectievelijk 2, 3 en 4 leven. Ze staan stil in deze proefopstelling. Een treffer licht kort rood op; verslagen monsters verdwijnen en geven 2 goud. Aanvallen heeft een korte wachttijd van 200 ms. De speler botst met muren, monsters en de kist. Het spelerskader reserveert steeds 7×4 cellen zodat ook een uitgerust wapen past; daardoor tellen enkele lege cellen mee voor botsingen.

Een venster van **80×24 tekencellen** is genoeg. Het level past zich bij het openen aan het beschikbare venster aan, vanaf 64×22 gebruikte cellen. Bij verkleinen blijft de voortgang bewaard en verschijnt zo nodig een melding; vergroot het venster weer of gebruik R om een nieuw level voor het kleinere venster te maken. `+` en `-` veranderen alleen spriteformaten in de andere demo's.

Een nieuw tekstmodel toevoegen kan in [`AsciiLevel.java`](src/main/java/nl/pinc/terminallab/AsciiLevel.java): voeg een `MonsterType` toe met naam, leven, kleur en een `AsciiModel` met tekstregels. Voeg daarna een exemplaar met een positie toe aan het level. [`AsciiModel.java`](src/main/java/nl/pinc/terminallab/AsciiModel.java) tekent die regels en laat spaties transparant, zodat dezelfde aanpak ook voor nieuwe kisten en wapens bruikbaar is.

## Demo 2: Free for All

Start `.\run.cmd` en kies **2**. Demo 2 is vervangen door een **lokale Free for All met vier bots**, in dezelfde ASCII-stijl als level 1. Er is geen server of netwerk nodig. De bots zijn zichtbaar als `[BOT] Rook`, `[BOT] Nyx`, `[BOT] Bram` en `[BOT] Iris`. Ze delen jouw wereld, zoeken gevechten, vallen ook elkaar aan, verdienen XP, kopen upgrades en respawnen.

De wereld is **240×100 tekencellen**. Een camera volgt jouw speler en toont hoogstens 100×28 cellen tegelijk, afhankelijk van de terminalgrootte. Je moet dus rondlopen om ruines, de oostelijke wildernis en de zuidelijke crypten te ontdekken. Vensterwijzigingen passen alleen het zicht aan: positie, monsters, XP en upgrades blijven behouden. Het minimum is 64×22 gebruikte terminalcellen; 100×32 is prettig.

| Toets | Actie in Free for All |
|---|---|
| **WASD** | Bewegen |
| **Pijl omhoog/omlaag/links/rechts** | Een zwaardaanval in die richting |
| **U**, daarna **1–4** | Upgradeoverzicht openen en een upgrade kopen |
| **U** | Upgradeoverzicht sluiten |
| **Tab** | Ranglijst van jou en de vier bots tonen/sluiten |
| **Enter** | Na een nederlaag opnieuw spawnen |
| B / P | Achtergrond / loopanimatie wisselen |
| **R** | Volledig nieuw spel; alle voortgang wissen |
| **M** / Esc / **Q** | Hoofdmenu / hoofdmenu / afsluiten |

In het upgrademenu betekenen 1–4 een aankoop; sluit het menu eerst met U als je met een cijfertoets naar een andere demo wilt. Gevechten lopen tijdens het upgrademenu en de ranglijst door. Je begint met een zichtbaar zwaard; het oppakken en wisselen van wapens blijft een onderdeel van level 1.

### Monsters en upgrades

Er zijn twintig monsters verspreid over de kaart. Ze achtervolgen nabije strijders en slaan terug. Monsters geven bij een nederlaag vaste XP en respawnen na ongeveer twaalf seconden.

| Monster | HP | Schade | XP |
|---|---:|---:|---:|
| Slijm | 10 | 2 | **10** |
| Goblin | 18 | 4 | 20 |
| Skelet | 28 | 6 | 35 |
| Ogre | 45 | 9 | 60 |

Je begint met 30 HP en 5 schade. Iedere upgrade heeft maximaal vijf niveaus. De eerste aankoop per eigenschap kost 10 XP, de tweede 20, vervolgens 30, 40 en 50 XP.

| Keuze in U-menu | Eigenschap | Effect per niveau |
|---|---|---|
| 1 | Maximale HP | +10 maximale en huidige HP |
| 2 | Genezing | Korter interval: 1 HP per `2 / (niveau + 1)` seconden |
| 3 | Bewegingssnelheid | 20 ms minder wachttijd per stap; van 140 tot minimaal 40 ms |
| 4 | Schade | +3 schade per treffer |

Genezing begint drie seconden na de laatste treffer. Simulatieticks ronden de genezingsmomenten iets af. Lopen werkt op losse toetsen en de toetsherhaling van het besturingssysteem; de werkelijke snelheid hangt ook van die herhaling af. Een zwaardaanval heeft een cooldown van 350 ms en raakt alleen een tegenstander in de gekozen richting, binnen vier cellen en zonder muur ertussen.

### XP-verdeling en respawn

De HUD toont **totaal XP** en **vrije XP**. Kopen verlaagt alleen vrije XP: uitgegeven XP blijft meetellen in het totaal voor PvP.

Bij het verslaan van een andere strijder gaat **70% van diens totale XP** naar de winnaar. De verliezer verliest alle gekochte upgrades en houdt **30% als volledig besteedbare startbonus**. Bijvoorbeeld: 100 totaal XP, waarvan 80 uitgegeven, levert nog steeds 70 XP voor de winnaar en 30 vrije XP bij de respawn op. Bij ondeelbare aantallen wordt de 70%-beloning naar beneden afgerond en gaat de rest naar de verliezer, zodat er geen XP bijkomt door afronding.

Na jouw nederlaag druk je **Enter** om op een verspreide vrije positie opnieuw te verschijnen, met 30 HP en drie seconden bescherming. Bots respawnen automatisch na vijf seconden. Als een monster jou verslaat, vervallen ook alle upgrades en blijft dezelfde 30%-startbonus over; de overige XP verdwijnt omdat er geen winnende speler is.

Dit is een proefopstelling met eenvoudige lokale bots. Er zijn geen andere menselijke spelers verbonden en er wordt niets opgeslagen na afsluiten of R. De FFA-simulatie staat in [`FreeForAll.java`](src/main/java/nl/pinc/terminallab/FreeForAll.java), de camera/HUD/menu's in [`FreeForAllView.java`](src/main/java/nl/pinc/terminallab/FreeForAllView.java). [`TerminalKeys.java`](src/main/java/nl/pinc/terminallab/TerminalKeys.java) voegt de escape-bytes van pijltjestoetsen samen, zodat een aanval niet per ongeluk als Esc of als WASD wordt verwerkt.

## Demo 4: monstershowroom

Sluit een al draaiende game met Q en start opnieuw met `.\run.cmd`. Kies **4** voor de monstershowroom of **5** voor de wapenshowroom. Als je nog het oude menu ziet, sluit dan ook die oude sessie: een draaiende Java-applicatie neemt broncodewijzigingen pas bij een nieuwe start over.

Kies **4** voor een begaanbare galerij met **15 verschillende ASCII-monsters**: slijm, goblin, skelet, vleermuis, spin, spook, ork, trol, wolf, mimic, golem, slang, duiveltje, kever en draak. Iedere soort heeft een eigen tekstmodel, kleur, naam en korte beschrijving. De monsters staan stil en vallen je niet aan.

Loop met **WASD** langs de drie rijen met telkens vijf modellen. De camera volgt je over de kaart van **124×44 cellen**; loop ook naar rechts en beneden om alle modellen te zien. Onder het veld staan de gegevens van het dichtstbijzijnde monster. De modellen zijn solide, maar je kunt langs ieder model lopen.

De herbruikbare modellen staan in [`ShowroomCatalog.java`](src/main/java/nl/pinc/terminallab/ShowroomCatalog.java), in `ShowroomCatalog.Monster`. Elk model bestaat uitsluitend uit afdrukbare ASCII-tekens, getekend met `AsciiModel`. De showroom is een catalogus voor toekomstige levels; de bestaande monsters in level 1 en de FFA worden er niet automatisch door vervangen.

## Demo 5: wapenshowroom en testveld

Kies **5** voor **15 oppakbare ASCII-wapens** in drie rijen rekken. Loop naar een wapen en druk **E**: het verdwijnt van de grond, komt in je inventaris en is zichtbaar naast je poppetje. **F** wisselt tussen je verzamelde wapens. Bij een rek zie je de eigenschappen van dat wapen, ook na het oppakken.

Onder de rekken staat op **(60,46)** een **onsterfelijke golem**. Deze beweegt niet en valt niet aan. Een treffer maakt hem 300 ms rood; de HUD toont de laatste schade, het aantal treffers en de totale schade. Er is geen munitielimiet, zodat je alle wapens onbeperkt kunt vergelijken.

| Toets | Actie in demo 5 |
|---|---|
| **WASD** | Lopen en kijkrichting veranderen |
| **E** | Dichtstbijzijnde wapen binnen twee cellen oppakken |
| **F** | Volgend verzameld wapen uitrusten |
| **Pijlen** | Meteen aanvallen in die richting |
| **Spatie** | Aanvallen in je laatste loop-/aanvalsrichting |
| **V** | Bereikvoorbeeld aan/uit: `.` is bereik, `*` is de aanval |
| **R** | Begin opnieuw, met lege inventaris en alle wapens terug in de rekken |
| **M / Esc / Q** | Hoofdmenu / hoofdmenu / afsluiten |

| Wapen | Schade | Bereik | Wachttijd | Aanvalsvorm |
|---|---:|---:|---:|---|
| Dolk | 3 | 1 | 180 ms | Smalle rechte lijn |
| Kortzwaard | 5 | 2 | 280 ms | Brede rechte baan |
| Langzwaard | 8 | 4 | 450 ms | Brede rechte baan |
| Slagzwaard | 13 | 5 | 850 ms | Waaier |
| Rapier | 6 | 6 | 330 ms | Smalle rechte lijn |
| Katana | 9 | 3 | 300 ms | Waaier |
| Bijl | 11 | 2 | 650 ms | Waaier |
| Oorlogshamer | 16 | 2 | 1100 ms | Rondom de speler |
| Speer | 7 | 8 | 550 ms | Smalle rechte lijn |
| Hellebaard | 12 | 6 | 800 ms | Waaier |
| Zweep | 4 | 9 | 400 ms | Brede rechte baan |
| Boog | 6 | 16 | 500 ms | Smalle rechte lijn |
| Kruisboog | 15 | 20 | 1200 ms | Smalle rechte lijn |
| Vuurstaf | 10 | 12 | 700 ms | Brede rechte baan |
| Bom | 20 | 4 | 1600 ms | Rondom de speler |

Bereik wordt in tekencellen gemeten vanaf het spelerskader van **10×5 cellen**, inclusief gedragen uitrusting. Rechte aanvallen en waaiers raken alleen in de gekozen richting; de hamer en bom raken rondom de speler. Een waaier wordt breder met de afstand. Het bereikvoorbeeld en de schadecontrole gebruiken exact hetzelfde gebied. Afstandsaanvallen raken direct, met een kort zichtbaar tekstspoor; de bom ontploft rondom jezelf. De wapens zijn voorbeelden zonder munitie- of werpsimulatie.

De wereld is **124×58 cellen** en de camera volgt je. Voor beide showrooms is **80×24** bruikbaar, met minimaal **64×22 gebruikte cellen**; een groter venster toont meer modellen tegelijk. Vensterwijzigingen bewaren je positie, inventaris en schadestatistieken. **+ / -** hebben hier geen effect, want alle modellen hebben een vast tekstformaat. **B** en **P** wisselen de achtergrond en loopanimatie.

De wapenmodellen en eigenschappen staan in `ShowroomCatalog.Weapon`; de showroom, camera en gevechtslogica in [`Showroom.java`](src/main/java/nl/pinc/terminallab/Showroom.java). Deze wapens worden nog niet aan level 1 of FFA toegevoegd.

## Bediening

| Toets | Actie |
|---|---|
| `1`–`9` | Open meteen een demo, ook vanuit een andere demo |
| `0` | Open de pedit5-variant: amber avonturier met zwaard en schild |
| `W A S D` | Beweeg omhoog, links, omlaag en rechts, zonder Enter |
| `E` / `F` / Spatie | In level 1: pakken/openen, wapen wisselen, aanvallen |
| Pijlen / `U` / Tab / Enter | In demo 2: gericht aanvallen, upgrades, ranglijst, respawn |
| `E` / `F` / Pijlen / Spatie / `V` | In demo 5: pakken, wisselen, aanvallen en bereikvoorbeeld |
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
| 1 | ASCII-level | Het kleine tekstpoppetje, drie monstertypes, een kist en zichtbare uitrusting |
| 2 | Free for All | Grote ASCII-wereld met vier bots, monsters, XP-upgrades en respawn |
| 3 | Unicode-art | `█`, `▀`, `▄`, `▐`, `▌` en kleine gezichtselementen |
| 4 | ASCII-monstershowroom | 15 benoemde, herbruikbare tekstmonsters in een begaanbare galerij |
| 5 | ASCII-wapenshowroom | 15 oppakbare wapens met verschillend bereik, schade en aanvalsvorm; onsterfelijke testgolem |
| 6 | `█` + voorgrondkleur | Eén bronpixel per terminalcel |
| 7 | Spatie + achtergrondkleur | Eén bronpixel per terminalcel |
| 8 | `▀` + voor- en achtergrondkleur | Twee verticale bronpixels per terminalcel |
| 9 | Vergelijking naast elkaar | Dezelfde sprite, achtergrond, invoer en positie met methodes 6–8 |
| 0 | Pedit5-stijl | Amberkleurig lijnpoppetje met zwaard en schild op zwart, getekend met `▀` |

Demo's 4 en 5 gebruiken uitsluitend ASCII-modellen met ANSI-kleuren. Demo's 6–9 vergelijken de pixelrenderers en kunnen een eigen PNG gebruiken: 6 toont voorgrondblokken, 7 achtergrondkleuren en 8 halfblokken. PNG is de invoerbron; de manier van tekenen is daarvan onafhankelijk.

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
| `AsciiLevel` | Level 1: monsters, kist, oppakken, uitrusten en aanvallen |
| `AsciiModel` | Herbruikbare modellen van alleen ASCII-tekstregels |
| `ShowroomCatalog` | Vijftien monster- en wapenmodellen met eigenschappen voor hergebruik |
| `Showroom` | Demo's 4 en 5: galerij, camera, inventaris, aanvalsmaskers en onsterfelijke testgolem |
| `FreeForAll` | Gedeelde lokale FFA-wereld, bots, gevechten, XP, upgrades en respawn |
| `FreeForAllView` | Camera, HUD, upgrademenu, ranglijst en respawnscherm |
| `TerminalKeys` | CSI/SS3-pijltjestoetsen en afzonderlijke Escape herkennen |

```powershell
mvn test
# Dezelfde projectlokale cache als de startscripts gebruiken:
mvn '-Dmaven.repo.local=.cache/maven' test
```

De automatische tests controleren de drie pixelstrategieën, transparantie (ook gedeeltelijk en bij oneven hoogtes), PNG-decoding, afmetingen/aspectratio, ongeldige invoer, clipping, obstakels, schermverschillen zonder wisactie en beweging in alle tien demo's op alle drie formaten. Voor het pedit5-personage controleren ze ook de amberkleur, transparante gaten, stabiele bovenlichaamcontouren en verschillende beenposes. Ze vereisen geen echte terminal. De interactieve Windows-controle moet daarnaast de native invoer en herstelinstellingen toetsen: start de JAR, kies een demo zonder Enter, beweeg met WASD, vergelijk 6/7/8/9, probeer 0, wissel formaat, keer terug naar het menu en sluit af met Q of Ctrl+C.

De leveltests lopen via echte routes naar wapens, kist en monsters op drie veldformaten. Ze controleren zichtbaar gedragen wapens, de eenmalige kistbeloning, wisselen van uitrusting, bereik en wachttijd van aanvallen, botsingen, ASCII-modeltekens en de koppeling met het hoofdmenu en resetten.

De FFA-tests controleren onder meer de 70/30-verdeling inclusief uitgegeven XP, verlies van upgrades, spawnbeveiliging, de effecten van beweging/genezing/schade/HP, richtingsaanvallen en muren, monsterrespawn, behoud van voortgang bij resizing, cameragrenzen en bereikbare routes over de kaart. Een langere simulatie controleert dat bots vechten, upgrades kopen en geldige posities en statistieken behouden. De invoertests dekken zowel CSI- als SS3-pijlen, modifiers en een losse Esc.

De showroomtests lopen echte routes naar alle 15 monsters en wapens, controleren zichtbare uitrusting en inventariswissels, gebruiken ieder wapen herhaaldelijk tegen de golem en toetsen richting, bereikgrenzen, aanvalsvormen, cooldowns, trefferkleur en herstel. Ze controleren ook ASCII-tekens, cameragrenzen, resizing, resetten en het negeren van spriteformaattoetsen.
