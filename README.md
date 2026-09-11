# PSS25-MONOPOLY

Implementazione in Java del gioco da tavolo **Monopoly** (progetto individuale, corso PSS).
Architettura **MVC**, build con **Gradle (Kotlin DSL)**.

## Requisiti

- JDK 21 o superiore
- Nessuna installazione di Gradle: il progetto include il **Gradle wrapper**

## Struttura del progetto

```
src/main/java/monopoly/
├── MonopolyApp.java          # punto di ingresso (demo testuale di alcuni turni)
├── model/
│   ├── board/                # Tile (astratta), PlaceholderTile, Board
│   ├── player/               # Player, PlayerStatus, Token
│   ├── economy/              # Bank, Property
│   └── game/                 # Dice, GamePhase, GameState, TurnManager, RollResult, RollOutcome
├── controller/               # GameEngine (punto di ingresso), GameObserver (pattern Observer)
└── view/                     # ConsoleGameObserver (view testuale di esempio)

src/test/java/monopoly/       # test JUnit 5 (model e controller)
```

## Comandi Gradle

```bash
./gradlew build        # compila, esegue i test e produce il jar
./gradlew compileJava  # solo compilazione
./gradlew test         # solo test JUnit 5
./gradlew test --tests TurnManagerTest   # una sola classe di test
./gradlew run          # avvia l'applicazione
./gradlew jar          # crea il jar eseguibile in build/libs/
java -jar build/libs/monopoly-1.0.0.jar
./gradlew clean        # ripulisce la cartella build/
```