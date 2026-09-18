package it.unibo.monopoly.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameEventSupport;
import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.RollResult;
import it.unibo.monopoly.model.persistence.GameStateSaver;
import it.unibo.monopoly.model.persistence.PersistenceResult;
import it.unibo.monopoly.model.persistence.SaveData;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * Test di salvataggio e caricamento attraverso il {@link GameEngine}, cioe' la strada
 * usata dalla GUI.
 * <p>
 * Oltre ai dati si verifica cio' su cui la GUI conta: le notifiche dopo un caricamento,
 * osservatori mai duplicati, la partita in corso intatta se il file non e' valido, e una
 * partita caricata che prosegue esattamente come avrebbe fatto quella originale.
 */
class GameEnginePersistenceTest {

    private static final long SEED = 7;

    /** Turni giocati prima di salvare. */
    private static final int TURNS = 10;

    @TempDir
    Path folder;

    private Path saveFile;
    private GameStateSaver saver;

    /** Osservatore di test: registra come testo gli eventi ricevuti, nell'ordine di arrivo. */
    private static final class RecordingObserver implements GameObserver {

        private final List<String> events = new ArrayList<>();

        @Override
        public void onGameLoaded(final GameState state) {
            events.add("loaded");
        }

        @Override
        public void onTurnStarted(final Player player) {
            events.add("turn:" + player.getName());
        }

        @Override
        public void onDiceRolled(final RollResult result) {
            events.add("dice");
        }

        @Override
        public void onPlayerSentToJail(final Player player) {
            events.add("jail:" + player.getName());
        }

        @Override
        public void onGameStateChanged(final GameState state) {
            events.add("changed");
        }

        /** @return quante volte e' arrivato l'evento indicato */
        long count(final String event) {
            return events.stream().filter(event::equals).count();
        }
    }

    @BeforeEach
    void setUp() {
        saveFile = folder.resolve("partita.properties");
        saver = new GameStateSaver();
    }

    @Test
    void loadingReplacesTheGameAndNotifiesTheObserversOnce() {
        final GameEngine original = playedEngine(new RecordingObserver());
        assertTrue(original.saveGame(saveFile).successful());
        final GameEngine engine = newEngine(newDice());
        final RecordingObserver observer = new RecordingObserver();
        engine.addObserver(observer);
        engine.startGame();
        final GameState before = engine.getState();

        observer.events.clear();
        final PersistenceResult result = engine.loadGame(saveFile);

        assertTrue(result.successful(), result.message());
        assertNotSame(before, engine.getState());
        assertEquals(saver.createSaveData(original.getState()), saver.createSaveData(engine.getState()));
        final String currentPlayer = engine.getState().getCurrentPlayer().getName();
        assertEquals(List.of("loaded", "turn:" + currentPlayer, "changed"), observer.events);
    }

    @Test
    void aLoadIsAnnouncedAsAStartToObserversThatDoNotDistinguishIt() {
        assertTrue(playedEngine(new RecordingObserver()).saveGame(saveFile).successful());
        final GameEngine engine = newEngine(newDice());
        final List<String> events = new ArrayList<>();
        // Ridefinisce solo onGameStarted: il default di onGameLoaded deve ricadere li'.
        engine.addObserver(new GameObserver() {
            @Override
            public void onGameStarted(final GameState state) {
                events.add("started");
            }
        });

        engine.loadGame(saveFile);

        assertEquals(List.of("started"), events);
    }

    @Test
    void observersAreMovedToTheLoadedGameWithoutDuplicates() {
        assertTrue(playedEngine(new RecordingObserver()).saveGame(saveFile).successful());
        final GameEngine engine = newEngine(newDice());
        final RecordingObserver observer = new RecordingObserver();
        engine.addObserver(observer);
        engine.startGame();
        final GameEventSupport initialEvents = engine.getState().getContext().getEvents();
        engine.loadGame(saveFile);
        final GameEventSupport firstLoadEvents = engine.getState().getContext().getEvents();
        engine.loadGame(saveFile);
        observer.events.clear();

        // Un evento del model della partita attuale arriva una volta sola...
        final Player someone = engine.getState().getCurrentPlayer();
        final GameEventSupport currentEvents = engine.getState().getContext().getEvents();
        currentEvents.fire(listener -> listener.onPlayerSentToJail(someone));
        currentEvents.publishPending();
        assertEquals(List.of("jail:" + someone.getName()), observer.events);

        // ...e le partite abbandonate non hanno piu' ascoltatori: un evento non viene nemmeno accodato.
        initialEvents.fire(listener -> listener.onPlayerSentToJail(someone));
        firstLoadEvents.fire(listener -> listener.onPlayerSentToJail(someone));
        assertFalse(initialEvents.hasPendingEvents());
        assertFalse(firstLoadEvents.hasPendingEvents());
    }

    @Test
    void aFailedLoadLeavesTheCurrentGameUntouched() throws IOException {
        final RecordingObserver observer = new RecordingObserver();
        final GameEngine engine = playedEngine(observer);
        final GameState stateBefore = engine.getState();
        final SaveData dataBefore = saver.createSaveData(stateBefore);
        final Path corrupted = folder.resolve("rovinato.properties");
        Files.writeString(corrupted, "format.id=monopoly-pss25\nformat.version=1\ngame.playerCount=tre\n");
        observer.events.clear();

        final PersistenceResult missing = engine.loadGame(folder.resolve("assente.properties"));
        final PersistenceResult damaged = engine.loadGame(corrupted);

        assertFalse(missing.successful());
        assertTrue(missing.message().contains("file non trovato"), missing.message());
        assertFalse(damaged.successful());
        assertTrue(damaged.message().contains("salvataggio danneggiato"), damaged.message());
        assertSame(stateBefore, engine.getState());
        assertEquals(dataBefore, saver.createSaveData(engine.getState()));
        assertTrue(observer.events.isEmpty());
        assertTrue(engine.canRollDice(), "la partita in corso deve poter continuare");
    }

    /**
     * La prova piu' forte di continuita': la partita originale e quella caricata giocano
     * lo stesso turno successivo con dadi nella stessa condizione, e devono arrivare alla
     * stessa identica situazione.
     */
    @Test
    void aLoadedGameContinuesExactlyAsTheOriginalWould() {
        final RecordingObserver originalObserver = new RecordingObserver();
        final GameEngine original = playedEngine(originalObserver);
        assertFalse(original.isGameOver());
        assertTrue(original.saveGame(saveFile).successful());

        // Dadi con lo stesso seme, fatti avanzare dei lanci gia' giocati: il prossimo lancio
        // sara' lo stesso che fara' la partita originale.
        final Dice syncedDice = newDice();
        for (long roll = 0; roll < originalObserver.count("dice"); roll++) {
            syncedDice.roll();
        }
        final GameEngine loaded = newEngine(syncedDice);
        final RecordingObserver loadedObserver = new RecordingObserver();
        loaded.addObserver(loadedObserver);
        assertTrue(loaded.loadGame(saveFile).successful());
        final Player playerBefore = loaded.getState().getCurrentPlayer();
        loadedObserver.events.clear();

        original.playTurn();
        loaded.playTurn();

        assertTrue(loadedObserver.count("dice") > 0, "dopo il caricamento si deve poter lanciare");
        assertNotEquals(playerBefore.getName(), loaded.getState().getCurrentPlayer().getName());
        assertEquals(GamePhase.ROLL, loaded.getState().getPhase());
        assertEquals(saver.createSaveData(original.getState()), saver.createSaveData(loaded.getState()));
    }

    @Test
    void savingReportsItsOutcomeWithoutNotifyingTheObservers() {
        final RecordingObserver observer = new RecordingObserver();
        final GameEngine engine = playedEngine(observer);
        observer.events.clear();

        final PersistenceResult saved = engine.saveGame(saveFile);
        final PersistenceResult failed = engine.saveGame(folder.resolve("manca").resolve("partita.properties"));

        assertTrue(saved.successful(), saved.message());
        assertTrue(Files.exists(saveFile));
        assertFalse(failed.successful());
        assertTrue(observer.events.isEmpty());
        assertTrue(engine.canRollDice(), "un salvataggio fallito non deve fermare la partita");
    }

    // ------------------------------------------------------------------
    // Metodi di supporto
    // ------------------------------------------------------------------

    /** Motore con dadi a seme fisso, gia' avviato e giocato per qualche turno. */
    private static GameEngine playedEngine(final GameObserver observer) {
        final GameEngine engine = newEngine(newDice());
        engine.addObserver(observer);
        engine.startGame();
        for (int turn = 0; turn < TURNS; turn++) {
            engine.playTurn();
        }
        return engine;
    }

    /** Motore su una nuova partita standard con i dadi indicati. */
    private static GameEngine newEngine(final Dice dice) {
        return new GameEngine(GameState.createStandardGame(List.of(
                new Player("Alice", new Token("Car", "RED")),
                new Player("Bob", new Token("Dog", "BLUE")),
                new Player("Carol", new Token("Hat", "GREEN"))), dice));
    }

    private static Dice newDice() {
        return new Dice(new Random(SEED));
    }
}
