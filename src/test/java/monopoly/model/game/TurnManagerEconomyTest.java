package monopoly.model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.board.Board;
import monopoly.model.board.PropertyTile;
import monopoly.model.economy.Bank;
import monopoly.model.economy.EconomyManager;
import monopoly.model.player.Player;
import monopoly.model.player.Token;

/**
 * Test di integrazione fra turni, tabellone standard ed economia: il
 * {@link TurnManager} chiama {@code onLand} senza sapere che casella sia, e gli
 * effetti concreti devono comunque arrivare fino al denaro dei giocatori.
 * <p>
 * I dadi usano un {@link Random} con seme fisso, quindi i lanci sono sempre gli
 * stessi su qualunque JVM. Con il seme {@value #SEED_NO_DOUBLE} escono, nell'ordine:
 * 4+5 (9), 2+4 (6), 3+5 (8) - nessun doppio.
 * <p>
 * Caselle del tabellone standard usate qui: 9 "Viale Vesuvio" (prezzo 120, affitto 8),
 * 38 "Tassa di lusso" (100), 30 "Vai in prigione".
 */
class TurnManagerEconomyTest {

    /** Lanci: (4+5), (2+4), (3+5), ... nessun doppio. */
    private static final long SEED_NO_DOUBLE = 1;

    private static final int VESUVIO = 9;
    private static final int VESUVIO_PRICE = 120;
    private static final int VESUVIO_RENT = 8;
    private static final int LUXURY_TAX_TILE = 38;
    private static final int LUXURY_TAX = 100;

    private Player alice;
    private Player bob;
    private Player carol;

    @BeforeEach
    void setUp() {
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
        carol = new Player("Carol", new Token("Hat", "GREEN"));
    }

    /** Crea una partita sul tabellone standard con i giocatori indicati e dadi prevedibili. */
    private GameState newGame(final List<Player> players) {
        return GameState.createStandardGame(players, new Dice(new Random(SEED_NO_DOUBLE)));
    }

    @Test
    void landingOnAFreePropertyBuysItThroughTheBank() {
        final GameState state = newGame(List.of(alice, bob));

        new TurnManager(state).rollDice(); // 4 + 5 -> casella 9

        final PropertyTile vesuvio = (PropertyTile) state.getBoard().getTileAt(VESUVIO);
        assertEquals(VESUVIO, alice.getPosition());
        assertEquals(Bank.STARTING_BALANCE - VESUVIO_PRICE, alice.getMoney());
        assertSame(alice, vesuvio.getOwner().orElseThrow());
    }

    @Test
    void landingOnAnOwnedPropertyPaysTheRentToTheOwner() {
        final GameState state = newGame(List.of(alice, bob));
        final EconomyManager economy = state.getContext().getEconomy();
        final PropertyTile vesuvio = (PropertyTile) state.getBoard().getTileAt(VESUVIO);
        economy.buyProperty(bob, vesuvio);

        new TurnManager(state).rollDice(); // Alice: 4 + 5 -> casella 9, gia' di Bob

        assertEquals(Bank.STARTING_BALANCE - VESUVIO_RENT, alice.getMoney());
        assertEquals(Bank.STARTING_BALANCE - VESUVIO_PRICE + VESUVIO_RENT, bob.getMoney());
    }

    @Test
    void landingOnATaxTilePaysTheTax() {
        final GameState state = newGame(List.of(alice, bob));
        alice.setPosition(LUXURY_TAX_TILE - 9);

        new TurnManager(state).rollDice(); // 4 + 5 -> casella 38

        assertEquals(LUXURY_TAX_TILE, alice.getPosition());
        assertEquals(Bank.STARTING_BALANCE - LUXURY_TAX, alice.getMoney());
    }

    @Test
    void passingOverGoPaysTheSalary() {
        final GameState state = newGame(List.of(alice, bob));
        // 33 + 9 = 42, cioe' casella 2: il giro passa dal Via senza fermarcisi.
        alice.setPosition(33);

        final RollResult result = new TurnManager(state).rollDice();

        assertTrue(result.passedGo());
        assertEquals(2, alice.getPosition());
        assertEquals(Bank.STARTING_BALANCE + Bank.GO_SALARY, alice.getMoney());
    }

    @Test
    void landingOnGoToJailTileSendsThePlayerToJail() {
        final GameState state = newGame(List.of(alice, bob));
        alice.setPosition(Board.GO_TO_JAIL_POSITION - 9);

        new TurnManager(state).rollDice(); // 4 + 5 -> casella 30

        assertTrue(alice.isInJail());
        assertEquals(Board.JAIL_POSITION, alice.getPosition());
        assertEquals(GamePhase.END_TURN, state.getPhase());
    }

    @Test
    void aJailedPlayerPaysTheBailAfterTheLastFailedAttempt() {
        final GameState state = newGame(List.of(alice, bob));
        final JailManager jail = state.getContext().getJail();
        jail.sendToJail(alice);
        // Due tentativi gia' falliti: il prossimo lancio senza doppio e' l'ultimo.
        jail.registerFailedAttempt(alice);
        jail.registerFailedAttempt(alice);

        final RollResult result = new TurnManager(state).rollDice(); // 4 + 5, niente doppio

        assertEquals(RollOutcome.RELEASED_ON_BAIL, result.outcome());
        assertTrue(alice.isPlaying());
        assertEquals(Board.JAIL_POSITION, alice.getPosition(), "esce, ma questo turno resta ferma");
        assertEquals(Bank.STARTING_BALANCE - JailManager.BAIL_AMOUNT, alice.getMoney());
    }

    @Test
    void aPlayerWhoCannotPayTheRentGoesBankruptAndIsSkippedAfterwards() {
        // Alice ha meno dell'affitto di Viale Vesuvio, che appartiene a Bob.
        alice = new Player("Alice", new Token("Car", "RED"), VESUVIO_RENT - 1);
        final GameState state = newGame(List.of(alice, bob, carol));
        final PropertyTile vesuvio = (PropertyTile) state.getBoard().getTileAt(VESUVIO);
        state.getContext().getEconomy().buyProperty(bob, vesuvio);
        final TurnManager turnManager = new TurnManager(state);

        turnManager.rollDice(); // Alice: 4 + 5 -> casella 9, affitto non pagabile

        assertTrue(alice.isBankrupt());
        assertEquals(0, alice.getMoney());
        // Il contante rimasto va al creditore.
        assertEquals(Bank.STARTING_BALANCE - VESUVIO_PRICE + VESUVIO_RENT - 1, bob.getMoney());

        // Da qui in avanti Alice non riceve piu' il turno.
        assertSame(bob, turnManager.endTurn());
        turnManager.rollDice();
        assertSame(carol, turnManager.endTurn());
        turnManager.rollDice();
        assertSame(bob, turnManager.endTurn(), "il turno salta Alice e torna a Bob");
        assertFalse(state.getActivePlayers().contains(alice));
    }
}
