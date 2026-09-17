package it.unibo.monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.economy.Bank;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.TurnManager;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * Test delle regole di calcolo dell'affitto, cioe' del polimorfismo di
 * {@link PropertyTile#computeRent(Board, Dice)}: la chiamata e' sempre la stessa, il
 * risultato dipende dal tipo di proprieta' e da come sta messo il tabellone.
 * <p>
 * I dadi hanno un seme fisso, quindi i lanci sono sempre gli stessi su qualunque JVM:
 * con il seme {@value #SEED} il primo lancio e' {@value #FIRST_ROLL} (4 + 5).
 */
class RentRulesTest {

    /** Seme dei dadi: il primo lancio e' 4 + 5, senza doppio. */
    private static final long SEED = 1;

    /** Quanto fa il primo lancio con quel seme. */
    private static final int FIRST_ROLL = 9;

    // Gruppo marrone, completo con due soli terreni: il monopolio piu' facile da fare.
    private static final int VICOLO_CORTO = 1;
    private static final int VICOLO_CORTO_RENT = 2;
    private static final int VICOLO_STRETTO = 3;
    private static final int VICOLO_STRETTO_RENT = 4;

    // Gruppo azzurro, di tre terreni: serve a controllare che due su tre non bastino.
    private static final int GRAN_SASSO = 6;
    private static final int MONTEROSA = 8;
    private static final int VESUVIO = 9;
    private static final int VESUVIO_RENT = 8;

    /** Le quattro stazioni, in ordine di acquisto nei test. */
    private static final int[] STATIONS = {5, 15, 25, 35};

    /** Affitto di una stazione con 1, 2, 3 e 4 stazioni possedute. */
    private static final int[] STATION_RENTS = {25, 50, 100, 200};

    private static final int ELECTRIC_COMPANY = 12;
    private static final int WATER_COMPANY = 28;

    private Player alice;
    private Player bob;
    private Dice dice;
    private GameState state;
    private Board board;

    @BeforeEach
    void setUp() {
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
        dice = new Dice(new Random(SEED));
        state = GameState.createStandardGame(List.of(alice, bob), dice);
        board = state.getBoard();
    }

    // ------------------------------------------------------------------
    // Terreni: monopolio di colore
    // ------------------------------------------------------------------

    @Test
    void aStreetAsksItsBaseRentWhenTheGroupIsIncomplete() {
        giveTo(bob, VICOLO_CORTO); // manca Vicolo Stretto

        assertEquals(VICOLO_CORTO_RENT, rentOf(VICOLO_CORTO));
    }

    @Test
    void aStreetAsksTwiceTheRentWhenTheOwnerHasTheWholeGroup() {
        giveTo(bob, VICOLO_CORTO, VICOLO_STRETTO);

        // Ogni terreno raddoppia il proprio affitto, non quello del gruppo.
        assertEquals(VICOLO_CORTO_RENT * 2, rentOf(VICOLO_CORTO));
        assertEquals(VICOLO_STRETTO_RENT * 2, rentOf(VICOLO_STRETTO));
    }

    @Test
    void aGroupSplitBetweenTwoPlayersIsNotAMonopoly() {
        giveTo(bob, VICOLO_CORTO);
        giveTo(alice, VICOLO_STRETTO);

        assertEquals(VICOLO_CORTO_RENT, rentOf(VICOLO_CORTO));
        assertEquals(VICOLO_STRETTO_RENT, rentOf(VICOLO_STRETTO));
    }

    @Test
    void almostAllTheGroupIsStillNotAMonopoly() {
        // Gruppo azzurro: due terreni su tre non bastano, il terzo lo completa.
        giveTo(bob, GRAN_SASSO, VESUVIO);
        assertEquals(VESUVIO_RENT, rentOf(VESUVIO));

        giveTo(bob, MONTEROSA);
        assertEquals(VESUVIO_RENT * 2, rentOf(VESUVIO));
    }

    // ------------------------------------------------------------------
    // Stazioni: 25, 50, 100, 200
    // ------------------------------------------------------------------

    @Test
    void stationRentDoublesWithEveryExtraStation() {
        for (int owned = 1; owned <= STATIONS.length; owned++) {
            giveTo(bob, STATIONS[owned - 1]);
            final int expected = STATION_RENTS[owned - 1];
            // Tutte le stazioni possedute chiedono lo stesso affitto.
            for (int index = 0; index < owned; index++) {
                assertEquals(expected, rentOf(STATIONS[index]),
                        "con " + owned + " stazioni");
            }
        }
    }

    @Test
    void stationsOfDifferentOwnersAreCountedSeparately() {
        giveTo(bob, STATIONS[0], STATIONS[1], STATIONS[2]);
        giveTo(alice, STATIONS[3]);

        assertEquals(STATION_RENTS[2], rentOf(STATIONS[0]), "Bob ne ha tre");
        assertEquals(STATION_RENTS[0], rentOf(STATIONS[3]), "Alice ne ha una sola");
    }

    // ------------------------------------------------------------------
    // Societa': dadi per 4 o per 10
    // ------------------------------------------------------------------

    @Test
    void oneUtilityAsksFourTimesTheDice() {
        giveTo(bob, ELECTRIC_COMPANY);
        dice.roll(); // 4 + 5

        assertEquals(FIRST_ROLL * UtilityTile.SINGLE_MULTIPLIER, rentOf(ELECTRIC_COMPANY));
    }

    @Test
    void bothUtilitiesAskTenTimesTheDice() {
        giveTo(bob, ELECTRIC_COMPANY, WATER_COMPANY);
        dice.roll(); // 4 + 5

        assertEquals(FIRST_ROLL * UtilityTile.BOTH_MULTIPLIER, rentOf(ELECTRIC_COMPANY));
        assertEquals(FIRST_ROLL * UtilityTile.BOTH_MULTIPLIER, rentOf(WATER_COMPANY));
    }

    @Test
    void theUtilityRentFollowsTheLastRoll() {
        giveTo(bob, ELECTRIC_COMPANY);

        dice.roll(); // 4 + 5 = 9
        final int firstRent = rentOf(ELECTRIC_COMPANY);
        dice.roll(); // 2 + 4 = 6
        final int secondRent = rentOf(ELECTRIC_COMPANY);

        assertEquals(dice.getTotal() * UtilityTile.SINGLE_MULTIPLIER, secondRent);
        assertEquals(FIRST_ROLL * UtilityTile.SINGLE_MULTIPLIER, firstRent);
    }

    // ------------------------------------------------------------------
    // Dall'atterraggio al pagamento: l'importo giusto arriva fino al denaro
    // ------------------------------------------------------------------

    @Test
    void landingOnAMonopolisedStreetPaysTwiceTheRent() {
        // Bob ha tutto il gruppo azzurro, Alice si ferma su Viale Vesuvio.
        giveTo(bob, GRAN_SASSO, MONTEROSA, VESUVIO);

        new TurnManager(state).rollDice(); // Alice: 4 + 5 -> casella 9

        final int paid = VESUVIO_RENT * 2;
        assertEquals(VESUVIO, alice.getPosition());
        assertEquals(Bank.STARTING_BALANCE - paid, alice.getMoney());
        assertEquals(Bank.STARTING_BALANCE + paid, bob.getMoney());
    }

    @Test
    void landingOnAStationPaysTheRentOfTheStationsOwned() {
        // Bob ha due stazioni: chi ci si ferma paga 50, non 25.
        giveTo(bob, STATIONS[0], STATIONS[1]);
        // Da 36 con un 9 si arriva alla casella 5 passando dal Via, che paga lo stipendio.
        alice.setPosition(36);

        new TurnManager(state).rollDice(); // 4 + 5 -> casella 5, "Stazione Sud"

        assertEquals(STATIONS[0], alice.getPosition());
        assertEquals(Bank.STARTING_BALANCE + Bank.GO_SALARY - STATION_RENTS[1], alice.getMoney());
        assertEquals(Bank.STARTING_BALANCE + STATION_RENTS[1], bob.getMoney());
    }

    @Test
    void landingOnAUtilityPaysTheDiceJustRolled() {
        giveTo(bob, ELECTRIC_COMPANY);
        alice.setPosition(3);

        new TurnManager(state).rollDice(); // 4 + 5 -> casella 12, "Societa' Elettrica"

        final int paid = FIRST_ROLL * UtilityTile.SINGLE_MULTIPLIER;
        assertEquals(ELECTRIC_COMPANY, alice.getPosition());
        assertEquals(Bank.STARTING_BALANCE - paid, alice.getMoney());
        assertEquals(Bank.STARTING_BALANCE + paid, bob.getMoney());
    }

    // ------------------------------------------------------------------
    // Metodi di supporto
    // ------------------------------------------------------------------

    /** @return l'affitto che si pagherebbe adesso sulla casella indicata */
    private int rentOf(final int position) {
        return ((PropertyTile) board.getTileAt(position)).computeRent(board, dice);
    }

    /**
     * Assegna le caselle indicate al giocatore senza farle pagare: qui interessa solo
     * il calcolo dell'affitto, non l'acquisto.
     */
    private void giveTo(final Player owner, final int... positions) {
        for (final int position : positions) {
            owner.addProperty((Property) board.getTileAt(position));
        }
    }
}
