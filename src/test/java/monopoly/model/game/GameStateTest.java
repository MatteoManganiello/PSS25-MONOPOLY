package monopoly.model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.board.Board;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;
import monopoly.model.player.Token;

/** Test sullo stato della partita e sul passaggio del turno. */
class GameStateTest {

    private Player alice;
    private Player bob;
    private Player carol;
    private GameState state;

    @BeforeEach
    void setUp() {
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
        carol = new Player("Carol", new Token("Hat", "GREEN"));
        state = new GameState(new Board(), List.of(alice, bob, carol), new Dice());
    }

    @Test
    void newGameStartsWithTheFirstPlayerInRollPhase() {
        assertSame(alice, state.getCurrentPlayer());
        assertEquals(GamePhase.ROLL, state.getPhase());
        assertEquals(0, state.getConsecutiveDoubles());
        assertFalse(state.isGameOver());
    }

    @Test
    void turnPassesInOrderAndWrapsAround() {
        assertSame(bob, state.advanceToNextPlayer());
        assertSame(carol, state.advanceToNextPlayer());
        // Dopo l'ultimo giocatore si ricomincia dal primo.
        assertSame(alice, state.advanceToNextPlayer());
    }

    @Test
    void bankruptPlayersAreSkipped() {
        bob.setStatus(PlayerStatus.BANKRUPT);
        assertSame(carol, state.advanceToNextPlayer());
        assertSame(alice, state.advanceToNextPlayer());
    }

    @Test
    void bankruptPlayersAreSkippedAcrossTheEndOfTheList() {
        carol.setStatus(PlayerStatus.BANKRUPT);
        state.setCurrentPlayerIndex(1);
        // Da Bob si salta Carol (fallita) e si torna ad Alice.
        assertSame(alice, state.advanceToNextPlayer());
    }

    @Test
    void lastActivePlayerKeepsTheTurn() {
        bob.setStatus(PlayerStatus.BANKRUPT);
        carol.setStatus(PlayerStatus.BANKRUPT);
        assertSame(alice, state.advanceToNextPlayer());
    }

    @Test
    void jailedPlayersAreNotSkipped() {
        // In prigione si resta in partita: il turno serve a tentare di uscire.
        bob.setStatus(PlayerStatus.IN_JAIL);
        assertSame(bob, state.advanceToNextPlayer());
    }

    @Test
    void advancingResetsTheTurnData() {
        state.setConsecutiveDoubles(2);
        state.setPhase(GamePhase.END_TURN);
        state.advanceToNextPlayer();
        assertEquals(0, state.getConsecutiveDoubles());
        assertEquals(GamePhase.ROLL, state.getPhase());
    }

    @Test
    void advancingFailsWhenEveryoneIsBankrupt() {
        alice.setStatus(PlayerStatus.BANKRUPT);
        bob.setStatus(PlayerStatus.BANKRUPT);
        carol.setStatus(PlayerStatus.BANKRUPT);
        assertThrows(IllegalStateException.class, state::advanceToNextPlayer);
    }

    @Test
    void activePlayersExcludeOnlyBankruptPlayers() {
        bob.setStatus(PlayerStatus.BANKRUPT);
        carol.setStatus(PlayerStatus.IN_JAIL);
        assertEquals(List.of(alice, carol), state.getActivePlayers());
    }

    @Test
    void invalidPlayerListsAreRejected() {
        final Board board = new Board();
        final Dice dice = new Dice();
        assertThrows(IllegalArgumentException.class, () -> new GameState(board, List.of(alice), dice));
        assertThrows(IllegalArgumentException.class, () -> new GameState(board, List.of(alice, alice), dice));
        assertThrows(IllegalArgumentException.class, () -> new GameState(board, null, dice));
    }
}
