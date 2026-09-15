package monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.economy.Bank;
import monopoly.model.economy.EconomyManager;
import monopoly.model.game.GameContext;
import monopoly.model.game.JailManager;
import monopoly.model.player.Player;
import monopoly.model.player.Token;

/**
 * Test degli effetti delle singole caselle, cioe' del polimorfismo di
 * {@link Tile#onLand(Player)}: la chiamata e' sempre la stessa, il risultato dipende
 * dal tipo concreto della casella.
 */
class TileEffectsTest {

    private static final int PRICE = 100;
    private static final int RENT = 25;
    private static final int TAX = 75;

    private GameContext context;
    private EconomyManager economy;
    private JailManager jail;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        context = new GameContext();
        economy = context.getEconomy();
        jail = context.getJail();
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
    }

    private PropertyTile newProperty(final int position) {
        return new PropertyTile("Vicolo di prova", position, PRICE, RENT, economy);
    }

    // ------------------------------------------------------------------
    // PropertyTile
    // ------------------------------------------------------------------

    @Test
    void landingOnAFreePropertyBuysIt() {
        final PropertyTile property = newProperty(5);

        property.onLand(alice);

        assertEquals(Bank.STARTING_BALANCE - PRICE, alice.getMoney());
        assertSame(alice, property.getOwner().orElseThrow());
        assertEquals(List.of(property), alice.getProperties());
    }

    @Test
    void aPropertyIsNotBoughtWhenTheMoneyIsNotEnough() {
        final Player poor = new Player("Poor", new Token("Boot", "GREY"), PRICE - 1);
        final PropertyTile property = newProperty(5);

        property.onLand(poor);

        // Non potersela permettere non e' un fallimento: la casella resta in vendita.
        assertTrue(property.isAvailable());
        assertEquals(PRICE - 1, poor.getMoney());
        assertTrue(poor.isPlaying());
    }

    @Test
    void landingOnSomeoneElsePropertyPaysTheRent() {
        final PropertyTile property = newProperty(5);
        property.onLand(alice); // Alice la compra

        property.onLand(bob);   // Bob ci si ferma e paga

        assertEquals(Bank.STARTING_BALANCE - RENT, bob.getMoney());
        assertEquals(Bank.STARTING_BALANCE - PRICE + RENT, alice.getMoney());
    }

    @Test
    void landingOnYourOwnPropertyHasNoEffect() {
        final PropertyTile property = newProperty(5);
        property.onLand(alice);
        final int moneyAfterPurchase = alice.getMoney();

        property.onLand(alice);

        assertEquals(moneyAfterPurchase, alice.getMoney());
    }

    // ------------------------------------------------------------------
    // TaxTile, StartTile
    // ------------------------------------------------------------------

    @Test
    void landingOnATaxTilePaysTheBank() {
        final int bankBalanceBefore = context.getBank().getBalance();
        final TaxTile tax = new TaxTile("Tassa di prova", 4, TAX, economy);

        tax.onLand(alice);

        assertEquals(Bank.STARTING_BALANCE - TAX, alice.getMoney());
        assertEquals(bankBalanceBefore + TAX, context.getBank().getBalance());
    }

    @Test
    void landingOnTheStartTilePaysTheSalary() {
        final StartTile start = new StartTile("Via", Board.START_POSITION, economy);

        start.onLand(alice);

        assertEquals(Bank.STARTING_BALANCE + Bank.GO_SALARY, alice.getMoney());
    }

    @Test
    void passingOverTheStartTileAlsoPaysTheSalary() {
        final StartTile start = new StartTile("Via", Board.START_POSITION, economy);

        start.onPass(alice);

        assertEquals(Bank.STARTING_BALANCE + Bank.GO_SALARY, alice.getMoney());
    }

    @Test
    void passingOverAnyOtherTileHasNoEffect() {
        final PropertyTile property = newProperty(5);

        property.onPass(alice);

        // onPass non e' ridefinito: vale l'implementazione vuota ereditata da Tile.
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
        assertTrue(property.isAvailable());
    }

    // ------------------------------------------------------------------
    // Caselle della prigione e posteggio
    // ------------------------------------------------------------------

    @Test
    void landingOnGoToJailSendsThePlayerToJail() {
        final GoToJailTile goToJail = new GoToJailTile("Vai in prigione", Board.GO_TO_JAIL_POSITION, jail);
        alice.setPosition(Board.GO_TO_JAIL_POSITION);

        goToJail.onLand(alice);

        assertEquals(Board.JAIL_POSITION, alice.getPosition());
        assertTrue(alice.isInJail());
        // Il trasferimento e' diretto: niente giro del tabellone, niente stipendio.
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
    }

    @Test
    void landingOnTheJailTileIsJustVisiting() {
        final JailTile jailTile = new JailTile("Prigione", Board.JAIL_POSITION);
        alice.setPosition(Board.JAIL_POSITION);

        jailTile.onLand(alice);

        assertTrue(alice.isPlaying());
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
    }

    @Test
    void freeParkingHasNoEffectWithTheOfficialRule() {
        final FreeParkingTile parking =
                new FreeParkingTile("Posteggio", Board.FREE_PARKING_POSITION, economy);
        parking.addToPot(500); // ignorato: la variante e' disattivata

        parking.onLand(alice);

        assertEquals(0, parking.getPot());
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
    }

    @Test
    void freeParkingPaysThePotWhenTheJackpotVariantIsEnabled() {
        final FreeParkingTile parking =
                new FreeParkingTile("Posteggio", Board.FREE_PARKING_POSITION, economy, true);
        parking.addToPot(500);

        parking.onLand(alice);

        assertEquals(Bank.STARTING_BALANCE + 500, alice.getMoney());
        assertEquals(0, parking.getPot(), "il montepremi va azzerato dopo la riscossione");
    }

    // ------------------------------------------------------------------
    // Polimorfismo
    // ------------------------------------------------------------------

    @Test
    void theSameCallProducesTheEffectOfEachTileType() {
        // Chi chiama vede solo il tipo astratto Tile e non sa che casella sia:
        // e' esattamente cio' che fa il TurnManager.
        final List<Tile> tiles = List.of(
                new StartTile("Via", Board.START_POSITION, economy),
                new TaxTile("Tassa", 4, TAX, economy),
                newProperty(5),
                new GoToJailTile("Vai in prigione", Board.GO_TO_JAIL_POSITION, jail));

        for (final Tile tile : tiles) {
            tile.onLand(alice);
        }

        // +200 di stipendio, -75 di tassa, -100 per l'acquisto, poi la prigione.
        assertEquals(Bank.STARTING_BALANCE + Bank.GO_SALARY - TAX - PRICE, alice.getMoney());
        assertEquals(1, alice.getProperties().size());
        assertTrue(alice.isInJail());
        assertEquals(Board.JAIL_POSITION, alice.getPosition());
    }
}
