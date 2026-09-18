# PSS25-MONOPOLY

Implementazione in Java del gioco da tavolo **Monopoly** (progetto individuale, corso PSS).
Architettura **MVC**, build con **Gradle (Kotlin DSL)**.

Autore: Matteo Manganiello

## Funzionalità

- Schermata di setup: scelta dei giocatori e delle pedine
- Turni con i doppi: chi fa doppio rilancia, al terzo doppio consecutivo va in prigione
- Acquisto delle proprietà deciso dal giocatore
- Affitti proporzionali: monopolio di colore ×2, stazioni 25/50/100/200, società ×4/×10 dei dadi
- Prigione
- Fallimento
- Salvataggio e caricamento della partita

## Requisiti

- JDK 25
- Nessuna installazione di Gradle: il progetto include il **Gradle wrapper**

## Comandi Gradle

```bash
./gradlew build        # compila, esegue i test e produce il jar
./gradlew compileJava  # solo compilazione
./gradlew test         # solo test JUnit 5
./gradlew run          # avvia l'interfaccia grafica
./gradlew run --args="--console"   # demo testuale automatica
./gradlew jar          # crea il jar eseguibile in build/libs/
java -jar build/libs/monopoly-1.0.0.jar
./gradlew clean        # ripulisce la cartella build/
```
