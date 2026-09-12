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
├── MonopolyApp.java          # punto di ingresso (demo testuale di alcuni turni)
├── model/
│   ├── board/                # Tile (astratta) e le sue sottoclassi concrete:
│   │                         #   PropertyTile, TaxTile, StartTile, JailTile,
│   │                         #   GoToJailTile, FreeParkingTile, PlaceholderTile;
│   │                         #   Board + BoardFactory (tabellone standard da 40 caselle)
│   ├── player/               # Player, PlayerStatus, Token
│   ├── economy/              # Bank (cassa), EconomyManager (regole sul denaro), Property
│   └── game/                 # Dice, GamePhase, GameState, TurnManager, RollResult,
│                             #   RollOutcome, JailManager, GameContext,
│                             #   GameEventListener + GameEventSupport (eventi del model)
├── controller/               # GameEngine (punto di ingresso), GameObserver (pattern Observer)
└── view/                     # ConsoleGameObserver (view testuale di esempio)

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

## Comandi Gradle

```bash
./gradlew build        # compila, esegue i test e produce il jar
./gradlew compileJava  # solo compilazione
./gradlew test         # solo test JUnit 5
./gradlew test --tests TurnManagerTest   # una sola classe di test
./gradlew test --tests '*Economy*'       # solo i test dell'economia
./gradlew run          # avvia l'applicazione
./gradlew jar          # crea il jar eseguibile in build/libs/
java -jar build/libs/monopoly-1.0.0.jar
./gradlew clean        # ripulisce la cartella build/
```