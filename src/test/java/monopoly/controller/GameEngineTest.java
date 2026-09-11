package monopoly.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.board.Board;
import monopoly.model.board.RecordingTile;
import monopoly.model.board.Tile;
import monopoly.model.game.Dice;
import monopoly.model.game.GameState;
import monopoly.model.game.RollResult;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;
import monopoly.model.player.Token;

/** Test del motore di gioco: ciclo di vita della partita e notifiche agli osservatori. */
class GameEngineTest {

    /** Lanci: (4+5), (2+4), ... nessun doppio (vedi TurnManagerTest). */
    private static final long SEED_NO_DOUBLE = 1;

    /** Lanci: (3+3), (1+2), ... un doppio seguito da un lancio normale. */
    private static final long SEED_DOUBLE_THEN_NORMAL = 3;

    /** Osservatore di test: registra come testo gli eventi ricevuti, nell'ordine di arrivo. */
    private static final class RecordingObserver implements GameObserver {

        private final List<String> events = new ArrayList<>();

        @Override
        public void onGameStarted(final GameState state) {
            events.add("started");
        }

        @Override
        public void onTurnStarted(final Player player) {
            events.add("turn:" + player.getName());
        }

        @Override
        public void onDiceRolled(final RollResult result) {
            events.add("dice:" + result.firstDie() + "+" + result.secondDie());
        }

        @Override
        public void onPlayerMoved(final Player player, final Tile from, final Tile to) {
            events.add("moved:" + player.getName() + ":" + from.getPosition() + "->" + to.getPosition());
        }

        @Override
        public void onPlayerSentToJail(final Player player) {
            events.add("jail:" + player.getName());
        }

        @Override
        public void onGameStateChanged(final GameState state) {
            events.add("changed");
        }

        @Override
        public void onGameOver(final Player winner) {
            events.add("over:" + winner.getName());
        }
    }

    private Player alice;
    private Player bob;
    private RecordingObserver observer;

    @BeforeEach
    void setUp() {
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
        observer = new RecordingObserver();
    }

    private GameEngine newEngine(final Board board, final long seed) {
        final GameState state = new GameState(board, List.of(alice, bob), new Dice(new Random(seed)));
        final GameEngine engine = new GameEngine(state);
        engine.addObserver(observer);
        return engine;
    }

    @Test
    void startGameNotifiesTheInitialStateAndTheFirstTurn() {
        newEngine(new Board(), SEED_NO_DOUBLE).startGame();
        assertEquals(List.of("started", "turn:Alice"), observer.events);
    }

    @Test
    void commandsAreRejectedBeforeTheGameStarts() {
        final GameEngine engine = newEngine(new Board(), SEED_NO_DOUBLE);
        assertThrows(IllegalStateException.class, engine::rollDice);
        assertThrows(IllegalStateException.class, engine::endTurn);
        engine.startGame();
        assertThrows(IllegalStateException.class, engine::startGame);
    }

    @Test
    void rollDiceNotifiesDiceMovementAndStateChange() {
        final GameEngine engine = newEngine(new Board(), SEED_NO_DOUBLE);
        engine.startGame();
        observer.events.clear();

        engine.rollDice(); // 4 + 5

        assertEquals(List.of("dice:4+5", "moved:Alice:0->9", "changed"), observer.events);
    }

    @Test
    void availableCommandsFollowTheTurnPhase() {
        final GameEngine engine = newEngine(new Board(), SEED_NO_DOUBLE);
        assertFalse(engine.canRollDice());

        engine.startGame();
        assertTrue(engine.canRollDice());
        assertFalse(engine.canEndTurn());

        engine.rollDice();
        assertFalse(engine.canRollDice());
        assertTrue(engine.canEndTurn());
    }

    @Test
    void endTurnPassesToTheNextPlayer() {
        final GameEngine engine = newEngine(new Board(), SEED_NO_DOUBLE);
        engine.startGame();
        engine.rollDice();
        observer.events.clear();

        engine.endTurn();

        assertSame(bob, engine.getState().getCurrentPlayer());
        assertEquals(List.of("turn:Bob", "changed"), observer.events);
    }

    @Test
    void playTurnRollsAgainAfterADoubleAndThenPassesTheTurn() {
        final GameEngine engine = newEngine(new Board(), SEED_DOUBLE_THEN_NORMAL);
        engine.startGame();
        observer.events.clear();

        engine.playTurn();

        assertEquals(List.of(
                "dice:3+3", "moved:Alice:0->6", "changed",
                "dice:1+2", "moved:Alice:6->9", "changed",
                "turn:Bob", "changed"), observer.events);
    }

    @Test
    void gameEndsWhenOnlyOnePlayerIsLeft() {
        // Simula il fallimento che dal Giorno 3 potra' causare una casella (affitto, tassa).
        final Board ruinousBoard = RecordingTile.createBoard(player -> player.setStatus(PlayerStatus.BANKRUPT));
        final GameEngine engine = newEngine(ruinousBoard, SEED_NO_DOUBLE);
        engine.startGame();

        engine.rollDice();

        assertTrue(engine.isGameOver());
        assertEquals(Optional.of(bob), engine.getWinner());
        assertEquals("over:Bob", observer.events.get(observer.events.size() - 1));
        assertFalse(engine.canRollDice());
        assertThrows(IllegalStateException.class, engine::endTurn);
    }

    @Test
    void removedObserversAreNoLongerNotified() {
        final GameEngine engine = newEngine(new Board(), SEED_NO_DOUBLE);
        engine.removeObserver(observer);
        engine.startGame();
        engine.rollDice();
        assertTrue(observer.events.isEmpty());
    }
}
