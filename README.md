# PSS25-MONOPOLY

Implementazione in Java del gioco da tavolo **Monopoly** (progetto individuale, corso PSS).
Architettura **MVC**, build con **Gradle (Kotlin DSL)**.

## Requisiti

- JDK 21: e' la versione richiesta dalla toolchain in `build.gradle.kts`, che Gradle
  cerca sulla macchina (con un JDK diverso la build si ferma con
  "Cannot find a Java installation ... matching languageVersion=21")
- Nessuna installazione di Gradle: il progetto include il **Gradle wrapper**

## Struttura del progetto

```
src/main/java/monopoly/
├── MonopolyApp.java          # punto di ingresso: apre la finestra di gioco
├── model/
│   ├── board/                # Tile (astratta) e le sue sottoclassi concrete:
│   │                         #   PropertyTile, TaxTile, StartTile, JailTile,
│   │                         #   GoToJailTile, FreeParkingTile, PlaceholderTile;
│   │                         #   TileCategory (famiglia della casella, usata dalla view);
│   │                         #   Board + BoardFactory (tabellone standard da 40 caselle)
│   ├── player/               # Player, PlayerStatus, Token
│   ├── economy/              # Bank (cassa), EconomyManager (regole sul denaro), Property
│   └── game/                 # Dice, GamePhase, GameState, TurnManager, RollResult,
│                             #   RollOutcome, JailManager, GameContext,
│                             #   GameEventListener + GameEventSupport (eventi del model)
├── controller/               # GameEngine (punto di ingresso), GameObserver (pattern Observer)
└── view/                     # GUI Swing: MainWindow (finestra e osservatore),
                              #   BoardPanel + TilePanel (tabellone e caselle),
                              #   PlayerInfoPanel (giocatori), ControlPanel (comandi e log),
                              #   ViewStyle (colori e formati);
                              # view testuale: TextGameObserver (cronaca condivisa)
                              #   + ConsoleGameObserver (la stampa su terminale)

src/test/java/monopoly/       # test JUnit 5 (model e controller)
```

## Regole implementate (Giorno 3)

- **Caselle**: ogni tipo di casella decide da se' cosa succede a chi ci si ferma
  (`Tile.onLand`) o ci passa sopra (`Tile.onPass`, ridefinito solo dal "Via").
- **Proprieta'**: se libera viene comprata da chi ci si ferma (se puo' permettersela),
  se e' di un altro si paga l'affitto al proprietario, se e' propria non succede nulla.
- **Tasse**: importo fisso versato alla banca (`Tassa patrimoniale` 200, `Tassa di lusso` 100).
- **Via**: la banca versa 200 sia a chi ci passa sopra sia a chi ci si ferma.
- **Fallimento**: chi deve pagare piu' di quanto possiede cede tutto il contante e tutte
  le proprieta' al creditore (all'altro giocatore, oppure alla banca: in quel caso le
  proprieta' tornano in vendita), passa nello stato `BANKRUPT` e viene saltato dai turni
  successivi.
- **Prigione**: ci si finisce con tre doppi consecutivi o con la casella "Vai in prigione".
  Si esce facendo doppio, pagando la cauzione di 50 prima di lanciare, oppure - dopo tre
  tentativi falliti - pagando obbligatoriamente la cauzione.

## Interfaccia grafica (Giorno 4)

L'applicazione si apre su una finestra Swing con il tabellone al centro, la situazione
dei giocatori sul lato destro e i comandi in basso.

- **Tabellone**: le 40 caselle sul perimetro, ognuna con nome, dettaglio (prezzo,
  importo della tassa, ...) e una striscia del colore del proprietario. Le pedine sono
  i pallini con l'iniziale del giocatore; la casella del giocatore di turno e'
  evidenziata in arancione. Il nome completo, l'affitto e il proprietario compaiono
  passando il mouse sulla casella.
- **Giocatori**: denaro, proprieta' possedute e stato (in gioco / in prigione /
  fallito). La scheda di chi deve giocare e' evidenziata con il colore della sua pedina.
- **Comandi**: "Tira i dadi" e "Passa il turno" (attivi solo quando la mossa e'
  consentita), il riquadro "Azioni" con il pagamento della cauzione, il disegno
  dell'ultimo lancio e il log di tutto cio' che e' successo nel turno.

La GUI parla con la partita solo attraverso il `GameEngine` e si aggiorna tramite il
pattern Observer: la stessa partita viene raccontata in parallelo anche sul terminale
dalla view testuale del Giorno 2.

## Comandi Gradle

```bash
./gradlew build        # compila, esegue i test e produce il jar
./gradlew compileJava  # solo compilazione
./gradlew test         # solo test JUnit 5
./gradlew test --tests TurnManagerTest   # una sola classe di test
./gradlew test --tests '*Economy*'       # solo i test dell'economia
./gradlew run          # avvia l'interfaccia grafica
./gradlew run --args="--console"   # demo testuale automatica di 20 turni
./gradlew jar          # crea il jar eseguibile in build/libs/
java -jar build/libs/monopoly-1.0.0.jar
./gradlew clean        # ripulisce la cartella build/
```