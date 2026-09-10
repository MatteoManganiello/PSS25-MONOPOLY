# PSS25-MONOPOLY

Implementazione in Java del gioco da tavolo **Monopoly** (progetto individuale, corso PSS).
Architettura **MVC**, build con **Gradle (Kotlin DSL)**.

## Requisiti

- JDK 21 o superiore
- Nessuna installazione di Gradle: il progetto include il **Gradle wrapper**

## Struttura del progetto

```
src/main/java/monopoly/
├── MonopolyApp.java          # punto di ingresso (demo testuale del modello)
├── model/
│   ├── board/                # Tile (astratta), PlaceholderTile, Board
│   ├── player/               # Player, PlayerStatus, Token
│   ├── economy/              # Bank, Property
│   └── game/                 # Dice, GamePhase
├── controller/               # vuoto (coordinamento model-view)
└── view/                     # vuoto (interfaccia grafica)

src/test/java/monopoly/model/  # test JUnit 5 (board, economy, game)
```

## Comandi Gradle

```bash
./gradlew build        # compila, esegue i test e produce il jar
./gradlew compileJava  # solo compilazione
./gradlew test         # solo test JUnit 5
./gradlew run          # avvia l'applicazione
./gradlew jar          # crea il jar eseguibile in build/libs/
java -jar build/libs/monopoly-1.0.0.jar
./gradlew clean        # ripulisce la cartella build/
```