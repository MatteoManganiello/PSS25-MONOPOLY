package monopoly.model.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import monopoly.model.player.Player;
import monopoly.model.player.Token;

/** Test di base sui movimenti di denaro. */
class BankTest {

    private static final int AMOUNT = 200;

    private Player newPlayer() {
        return new Player("Alice", new Token("Car", "RED"));
    }

    @Test
    void payIncreasesPlayerMoney() {
        final Bank bank = new Bank();
        final Player player = newPlayer();
        bank.pay(player, AMOUNT);
        assertEquals(Bank.STARTING_BALANCE + AMOUNT, player.getMoney());
    }

    @Test
    void chargeDecreasesPlayerMoney() {
        final Bank bank = new Bank();
        final Player player = newPlayer();
        assertTrue(bank.charge(player, AMOUNT));
        assertEquals(Bank.STARTING_BALANCE - AMOUNT, player.getMoney());
    }

    @Test
    void chargeFailsWhenPlayerCannotAfford() {
        final Bank bank = new Bank();
        final Player player = newPlayer();
        assertFalse(bank.charge(player, Bank.STARTING_BALANCE + 1));
        assertEquals(Bank.STARTING_BALANCE, player.getMoney());
    }
}
