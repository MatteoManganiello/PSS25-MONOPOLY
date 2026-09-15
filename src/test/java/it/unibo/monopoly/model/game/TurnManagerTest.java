package monopoly.model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.board.Board;
import monopoly.model.board.RecordingTile;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;
import monopoly.model.player.Token;

/**
 * Test delle regole del turno: movimento circolare, onLand, doppi e prigione.
 * <p>
 * I dadi usano un {@link Random} con seme fisso. L'algoritmo di java.util.Random
 * e' definito dalle specifiche Java, quindi a parita' di seme i lanci sono gli
 * stessi su qualunque JVM: i valori indicati accanto a ogni seme sono garantiti.
 */
class TurnManagerTest {

    /** Lanci: (4+5), (2+4), ... nessun doppio. */
    private static final long SEED_NO_DOUBLE = 1;

    /** Lanci: (3+3), (1+2), ... un doppio seguito da un lancio normale. */
    private static final long SEED_DOUBLE_THEN_NORMAL = 3;

    /** Lanci: (2+2), (2+2), (1+1), ... tre doppi consecutivi. */
    private static final long SEED_THREE_DOUBLES = 284;

    private Board board;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        board = RecordingTile.createBoard();
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
    }

    private GameState newState(final long seed) {
        return new GameState(board, List.of(alice, bob), new Dice(new Random(seed)));
    }

    private List<Player> visitorsOf(final int position) {
        return ((RecordingTile) board.getTileAt(position)).getVisitors();
    }

    // ------------------------------------------------------------------
    // Movimento
    // ------------------------------------------------------------------

    @Test
    void rollMovesThePlayerForwardByTheDiceTotal() {
        final RollResult result = new TurnManager(newState(SEED_NO_DOUBLE)).rollDice();

        assertEquals(4, result.firstDie());
        assertEquals(5, result.secondDie());
        assertEquals(RollOutcome.MOVED, result.outcome());
        assertEquals(0, result.fromPosition());
        assertEquals(9, result.toPosition());
        assertEquals(9, alice.getPosition());
        assertFalse(result.passedGo());
    }

    @Test
    void movementWrapsAroundTheBoard() {
        alice.setPosition(38);
        final RollResult result = new TurnManager(newState(SEED_NO_DOUBLE)).rollDice();

        // 38 + 9 = 47, e 47 modulo 40 = 7: il giocatore ha fatto il giro del tabellone.
        assertEquals(7, alice.getPosition());
        assertTrue(result.passedGo());
    }

    @Test
    void onLandIsCalledOnlyOnTheDestinationTile() {
        new TurnManager(newState(SEED_NO_DOUBLE)).rollDice();

        assertEquals(List.of(alice), visitorsOf(9));
        // Le caselle attraversate durante il movimento non vengono attivate.
        for (int position = 0; position < Board.SIZE; position++) {
            if (position != 9) {
                assertTrue(visitorsOf(position).isEmpty(), "onLand inatteso sulla casella " + position);
            }
        }
    }

    @Test
    void normalRollEndsTheTurnAndPassesToTheNextPlayer() {
        final GameState state = newState(SEED_NO_DOUBLE);
        final TurnManager turnManager = new TurnManager(state);

        // Prima di lanciare non si puo' chiudere il turno.
        assertThrows(IllegalStateException.class, turnManager::endTurn);
        turnManager.rollDice();
        assertEquals(GamePhase.END_TURN, state.getPhase());
        // Senza doppio non si lancia una seconda volta.
        assertThrows(IllegalStateException.class, turnManager::rollDice);

        assertSame(bob, turnManager.endTurn());
        assertSame(bob, state.getCurrentPlayer());
        assertEquals(GamePhase.ROLL, state.getPhase());
    }

    // ------------------------------------------------------------------
    // Regola dei doppi
    // ------------------------------------------------------------------

    @Test
    void doubleLetsThePlayerRollAgain() {
        final GameState state = newState(SEED_DOUBLE_THEN_NORMAL);
        final TurnManager turnManager = new TurnManager(state);

        final RollResult first = turnManager.rollDice(); // 3 + 3
        assertTrue(first.isDouble());
        assertEquals(RollOutcome.ROLL_AGAIN, first.outcome());
        assertEquals(6, alice.getPosition());
        assertEquals(1, state.getConsecutiveDoubles());
        assertEquals(GamePhase.ROLL, state.getPhase());
        assertSame(alice, state.getCurrentPlayer());
        // Dopo un doppio bisogna rilanciare: non si puo' passare la mano.
        assertThrows(IllegalStateException.class, turnManager::endTurn);

        final RollResult second = turnManager.rollDice(); // 1 + 2
        assertEquals(RollOutcome.MOVED, second.outcome());
        assertEquals(9, alice.getPosition());
        assertEquals(GamePhase.END_TURN, state.getPhase());
    }

    @Test
    void doublesCounterIsResetForTheNextPlayer() {
        final GameState state = newState(SEED_DOUBLE_THEN_NORMAL);
        final TurnManager turnManager = new TurnManager(state);

        turnManager.rollDice();
        turnManager.rollDice();
        turnManager.endTurn();

        assertSame(bob, state.getCurrentPlayer());
        assertEquals(0, state.getConsecutiveDoubles());
    }

    @Test
    void threeConsecutiveDoublesSendThePlayerToJail() {
        final GameState state = newState(SEED_THREE_DOUBLES);
        final TurnManager turnManager = new TurnManager(state);

        assertEquals(RollOutcome.ROLL_AGAIN, turnManager.rollDice().outcome()); // 2 + 2 -> casella 4
        assertEquals(RollOutcome.ROLL_AGAIN, turnManager.rollDice().outcome()); // 2 + 2 -> casella 8
        final RollResult third = turnManager.rollDice();                        // 1 + 1 -> prigione

        assertEquals(RollOutcome.SENT_TO_JAIL, third.outcome());
        assertEquals(Board.JAIL_POSITION, alice.getPosition());
        assertTrue(alice.isInJail());
        assertEquals(GamePhase.END_TURN, state.getPhase());
        // In prigione si va direttamente: la casella non riceve alcun onLand.
        assertTrue(visitorsOf(Board.JAIL_POSITION).isEmpty());
    }

    @Test
    void doubleDoesNotGrantAnotherRollIfTheTileSendsThePlayerToJail() {
        // Simula la futura casella "Vai in prigione" del Giorno 3.
        board = RecordingTile.createBoard(player -> {
            player.setPosition(Board.JAIL_POSITION);
            player.setStatus(PlayerStatus.IN_JAIL);
        });
        final GameState state = newState(SEED_DOUBLE_THEN_NORMAL);

        final RollResult result = new TurnManager(state).rollDice(); // 3 + 3

        assertTrue(result.isDouble());
        assertEquals(RollOutcome.MOVED, result.outcome());
        assertTrue(alice.isInJail());
        assertEquals(GamePhase.END_TURN, state.getPhase());
    }

    // ------------------------------------------------------------------
    // Prigione
    // ------------------------------------------------------------------

    @Test
    void jailedPlayerIsReleasedByADoubleButDoesNotRollAgain() {
        alice.setPosition(Board.JAIL_POSITION);
        alice.setStatus(PlayerStatus.IN_JAIL);
        final GameState state = newState(SEED_DOUBLE_THEN_NORMAL);

        final RollResult result = new TurnManager(state).rollDice(); // 3 + 3

        assertEquals(RollOutcome.RELEASED_FROM_JAIL, result.outcome());
        assertTrue(alice.isPlaying());
        assertEquals(Board.JAIL_POSITION + 6, alice.getPosition());
        assertEquals(GamePhase.END_TURN, state.getPhase());
    }

    @Test
    void jailedPlayerWithoutADoubleStaysInJail() {
        alice.setPosition(Board.JAIL_POSITION);
        alice.setStatus(PlayerStatus.IN_JAIL);
        final GameState state = newState(SEED_NO_DOUBLE);

        final RollResult result = new TurnManager(state).rollDice(); // 4 + 5

        assertEquals(RollOutcome.STAYED_IN_JAIL, result.outcome());
        assertTrue(alice.isInJail());
        assertEquals(Board.JAIL_POSITION, alice.getPosition());
        assertEquals(GamePhase.END_TURN, state.getPhase());
    }
}
