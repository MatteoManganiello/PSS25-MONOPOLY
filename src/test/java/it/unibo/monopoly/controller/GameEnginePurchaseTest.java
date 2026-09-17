package it.unibo.monopoly.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.board.PropertyTile;
import it.unibo.monopoly.model.economy.Bank;
import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * Test dell'acquisto come scelta del giocatore.
 * <p>
 * Fermarsi su una proprieta' libera non fa piu' comprare in automatico: apre un'offerta
 * e mette il turno in pausa finche' il giocatore non risponde con
 * {@link GameEngine#buyOfferedProperty()} o {@link GameEngine#declineOfferedProperty()}.
 * <p>
 * I dadi hanno un seme fisso, cosi' il primo tiro e' sempre 4 + 5: Alice parte dal Via e
 * finisce sulla casella {@value #VESUVIO}, "Viale Vesuvio", che e' libera e costa
 * {@value #VESUVIO_PRICE}.
 */
class GameEnginePurchaseTest {

    /** Seme che fa uscire 4 + 5 al primo tiro: nessun doppio, quindi un tiro solo. */
    private static final long SEED_NO_DOUBLE = 1;

    /** Casella su cui si arriva con 4 + 5 partendo dal Via. */
    private static final int VESUVIO = 9;

    /** Prezzo di "Viale Vesuvio". */
    private static final int VESUVIO_PRICE = 120;

    /** Seme che fa uscire 3 + 3 al primo tiro, cioe' un doppio, e 1 + 2 al secondo. */
    private static final long SEED_DOUBLE = 3;

    /** Casella su cui si arriva con il doppio 3 + 3: "Bastioni Gran Sasso". */
    private static final int GRAN_SASSO = 6;

    /** Prezzo di "Bastioni Gran Sasso". */
    private static final int GRAN_SASSO_PRICE = 100;

    @Test
    void landingOnAnAffordableFreePropertyOffersItWithoutCharging() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = startedGame(alice);

        engine.rollDice();

        assertEquals(VESUVIO, alice.getPosition());
        // Nessun addebito e nessun proprietario: c'e' solo una domanda in sospeso.
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
        assertTrue(vesuvio(engine).isAvailable());
        assertTrue(alice.getProperties().isEmpty());

        assertEquals(GamePhase.AWAITING_PURCHASE_DECISION, engine.getState().getPhase());
        assertTrue(engine.canBuyOfferedProperty());
        assertTrue(engine.canDeclineOfferedProperty());
        assertSame(vesuvio(engine), engine.getOfferedProperty().orElseThrow());
    }

    @Test
    void buyingTheOfferedPropertyChargesThePriceAndAssignsTheOwner() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = startedGame(alice);
        engine.rollDice();
        final int bankBefore = engine.getState().getContext().getBank().getBalance();

        assertTrue(engine.buyOfferedProperty());

        assertEquals(Bank.STARTING_BALANCE - VESUVIO_PRICE, alice.getMoney());
        assertSame(alice, vesuvio(engine).getOwner().orElseThrow());
        assertEquals(List.of(vesuvio(engine)), alice.getProperties());
        assertEquals(bankBefore + VESUVIO_PRICE, engine.getState().getContext().getBank().getBalance());

        // Risolta l'offerta, il turno riprende da dove si era fermato.
        assertFalse(engine.getState().hasPendingPurchase());
        assertTrue(engine.getOfferedProperty().isEmpty());
        assertEquals(GamePhase.END_TURN, engine.getState().getPhase());
        assertTrue(engine.canEndTurn());
    }

    @Test
    void refusingTheOfferLeavesThePropertyFreeAndWithoutAnOwner() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = startedGame(alice);
        engine.rollDice();

        engine.declineOfferedProperty();

        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
        assertTrue(vesuvio(engine).isAvailable());
        assertTrue(vesuvio(engine).getOwner().isEmpty());
        assertTrue(alice.getProperties().isEmpty());

        assertFalse(engine.getState().hasPendingPurchase());
        assertEquals(GamePhase.END_TURN, engine.getState().getPhase());
        assertTrue(engine.canEndTurn());
    }

    @Test
    void theTurnCannotEndWhileThereIsAPurchaseToDecide() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = startedGame(alice);
        engine.rollDice();

        assertFalse(engine.canEndTurn());
        assertThrows(IllegalStateException.class, engine::endTurn);
        // Nemmeno tirare di nuovo: prima si risponde.
        assertFalse(engine.canRollDice());
        assertThrows(IllegalStateException.class, engine::rollDice);

        // Dopo la risposta il turno si chiude normalmente.
        engine.declineOfferedProperty();
        assertSame(alice, engine.getState().getCurrentPlayer());
        assertTrue(engine.canEndTurn());
    }

    @Test
    void aPlayerWhoCannotAffordThePropertyGetsNoOffer() {
        final Player poor = new Player("Poor", new Token("Boot", "GREY"), VESUVIO_PRICE - 1);
        final GameEngine engine = startedGame(poor);

        engine.rollDice();

        assertEquals(VESUVIO, poor.getPosition());
        // Niente offerta: la proprieta' resta libera e il turno va avanti come sempre.
        assertFalse(engine.getState().hasPendingPurchase());
        assertFalse(engine.canBuyOfferedProperty());
        assertFalse(engine.canDeclineOfferedProperty());
        assertTrue(engine.getOfferedProperty().isEmpty());
        assertTrue(vesuvio(engine).isAvailable());
        assertEquals(VESUVIO_PRICE - 1, poor.getMoney());
        assertEquals(GamePhase.END_TURN, engine.getState().getPhase());
        assertTrue(engine.canEndTurn());
    }

    @Test
    void afterADoubleTheChoiceComesBeforeTheNextRoll() {
        // Con questo seme il primo tiro e' 3 + 3: un doppio, che porta sulla casella 6
        // ("Bastioni Gran Sasso", libera). Il secondo tiro e' 1 + 2, quindi niente doppio.
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameState state = GameState.createStandardGame(
                List.of(alice, new Player("Bob", new Token("Dog", "BLUE"))),
                new Dice(new Random(SEED_DOUBLE)));
        final GameEngine engine = new GameEngine(state);
        engine.startGame();

        engine.rollDice();

        // Prima si decide: il doppio non fa saltare la scelta.
        assertEquals(GRAN_SASSO, alice.getPosition());
        assertEquals(GamePhase.AWAITING_PURCHASE_DECISION, state.getPhase());
        assertFalse(engine.canRollDice());

        assertTrue(engine.buyOfferedProperty());

        // Risposto, il doppio si fa valere: Alice tira di nuovo.
        assertEquals(Bank.STARTING_BALANCE - GRAN_SASSO_PRICE, alice.getMoney());
        assertEquals(GamePhase.ROLL, state.getPhase());
        assertTrue(engine.canRollDice());
        assertFalse(engine.canEndTurn());

        engine.rollDice(); // 1 + 2 -> casella 9, "Viale Vesuvio", libera
        engine.declineOfferedProperty();

        // Senza doppio il turno finisce qui.
        assertEquals(GamePhase.END_TURN, state.getPhase());
        assertTrue(engine.canEndTurn());
    }

    @Test
    void thereIsNothingToAnswerWhenNoOfferIsOpen() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = startedGame(alice);

        assertFalse(engine.canBuyOfferedProperty());
        assertFalse(engine.canDeclineOfferedProperty());
        assertThrows(IllegalStateException.class, engine::buyOfferedProperty);
        assertThrows(IllegalStateException.class, engine::declineOfferedProperty);
    }

    /** @return una partita gia' avviata, con dadi a seme fisso e il giocatore indicato di turno */
    private static GameEngine startedGame(final Player firstPlayer) {
        final GameState state = GameState.createStandardGame(
                List.of(firstPlayer, new Player("Bob", new Token("Dog", "BLUE"))),
                new Dice(new Random(SEED_NO_DOUBLE)));
        final GameEngine engine = new GameEngine(state);
        engine.startGame();
        return engine;
    }

    /** @return la casella "Viale Vesuvio" del tabellone di questa partita */
    private static PropertyTile vesuvio(final GameEngine engine) {
        return (PropertyTile) engine.getState().getBoard().getTileAt(VESUVIO);
    }
}
