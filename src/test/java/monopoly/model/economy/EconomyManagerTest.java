package monopoly.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.game.GameContext;
import monopoly.model.player.Player;
import monopoly.model.player.Token;

/**
 * Test delle regole economiche: acquisti, affitti, tasse e fallimento.
 * <p>
 * Qui le proprieta' sono semplici {@link Property}: le regole sul denaro non
 * dipendono dal fatto che la proprieta' sia anche una casella del tabellone.
 */
class EconomyManagerTest {

    private static final int PRICE = 200;
    private static final int RENT = 50;

    private GameContext context;
    private EconomyManager economy;
    private Bank bank;
    private Player alice;
    private Player bob;
    private Property property;

    @BeforeEach
    void setUp() {
        context = new GameContext();
        economy = context.getEconomy();
        bank = context.getBank();
        alice = new Player("Alice", new Token("Car", "RED"));
        bob = new Player("Bob", new Token("Dog", "BLUE"));
        property = new Property("Viale di prova", 5, PRICE, RENT);
    }

    // ------------------------------------------------------------------
    // Acquisto e vendita
    // ------------------------------------------------------------------

    @Test
    void buyingChargesThePriceAndAssignsTheOwner() {
        final int bankBalanceBefore = bank.getBalance();

        assertTrue(economy.buyProperty(alice, property));

        assertEquals(Bank.STARTING_BALANCE - PRICE, alice.getMoney());
        assertEquals(bankBalanceBefore + PRICE, bank.getBalance());
        assertSame(alice, property.getOwner().orElseThrow());
        assertTrue(alice.getProperties().contains(property));
    }

    @Test
    void anAlreadyOwnedPropertyCannotBeBoughtAgain() {
        economy.buyProperty(alice, property);

        assertFalse(economy.buyProperty(bob, property));

        assertSame(alice, property.getOwner().orElseThrow());
        assertEquals(Bank.STARTING_BALANCE, bob.getMoney());
    }

    @Test
    void buyingFailsWithoutChargingWhenTheMoneyIsNotEnough() {
        final Player poor = new Player("Poor", new Token("Boot", "GREY"), PRICE - 1);

        assertFalse(economy.buyProperty(poor, property));

        assertEquals(PRICE - 1, poor.getMoney());
        assertTrue(property.isAvailable());
    }

    @Test
    void sellingReturnsThePriceAndPutsThePropertyBackOnSale() {
        economy.buyProperty(alice, property);

        assertEquals(PRICE, economy.sellPropertyToBank(alice, property));

        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
        assertTrue(property.isAvailable());
        assertTrue(alice.getProperties().isEmpty());
    }

    @Test
    void onlyTheOwnerCanSellAProperty() {
        economy.buyProperty(alice, property);

        assertEquals(0, economy.sellPropertyToBank(bob, property));

        assertSame(alice, property.getOwner().orElseThrow());
    }

    // ------------------------------------------------------------------
    // Affitti e tasse
    // ------------------------------------------------------------------

    @Test
    void rentMovesFromTheTenantToTheOwner() {
        economy.buyProperty(alice, property);
        final int bankBalanceBefore = bank.getBalance();

        assertTrue(economy.payRent(bob, property));

        assertEquals(Bank.STARTING_BALANCE - RENT, bob.getMoney());
        assertEquals(Bank.STARTING_BALANCE - PRICE + RENT, alice.getMoney());
        // L'affitto e' un giro tra giocatori: la cassa della banca non cambia.
        assertEquals(bankBalanceBefore, bank.getBalance());
    }

    @Test
    void taxesGoToTheBank() {
        final int bankBalanceBefore = bank.getBalance();

        assertTrue(economy.payToBank(alice, "Tassa patrimoniale", 200));

        assertEquals(Bank.STARTING_BALANCE - 200, alice.getMoney());
        assertEquals(bankBalanceBefore + 200, bank.getBalance());
    }

    @Test
    void theBankPaysTheSalary() {
        economy.receiveFromBank(alice, "stipendio del Via", Bank.GO_SALARY);

        assertEquals(Bank.STARTING_BALANCE + Bank.GO_SALARY, alice.getMoney());
    }

    // ------------------------------------------------------------------
    // Fallimento
    // ------------------------------------------------------------------

    @Test
    void aTenantWhoCannotPayGivesEverythingToTheOwnerAndIsOut() {
        final Player debtor = new Player("Debtor", new Token("Hat", "GREEN"), RENT - 1);
        final Property small = new Property("Vicolo di prova", 6, 10, 5);
        debtor.addProperty(small);
        economy.buyProperty(alice, property);

        assertFalse(economy.payRent(debtor, property));

        assertTrue(debtor.isBankrupt());
        assertEquals(0, debtor.getMoney());
        assertTrue(debtor.getProperties().isEmpty());
        // Il creditore incassa il contante rimasto e si prende le proprieta'.
        assertEquals(Bank.STARTING_BALANCE - PRICE + RENT - 1, alice.getMoney());
        assertSame(alice, small.getOwner().orElseThrow());
        assertTrue(alice.getProperties().contains(small));
    }

    @Test
    void aPlayerWhoCannotPayATaxFailsTowardsTheBank() {
        final Property owned = new Property("Vicolo di prova", 6, 10, 5);
        alice.addProperty(owned);

        assertFalse(economy.payToBank(alice, "Tassa patrimoniale", Bank.STARTING_BALANCE + 1));

        assertTrue(alice.isBankrupt());
        assertEquals(0, alice.getMoney());
        // Senza un creditore le proprieta' tornano in vendita.
        assertTrue(owned.isAvailable());
    }
}
