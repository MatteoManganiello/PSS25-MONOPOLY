package monopoly.model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.board.Board;
import monopoly.model.economy.Bank;
import monopoly.model.player.Player;
import monopoly.model.player.Token;

/** Test delle regole della prigione: ingresso, tentativi di uscita e cauzione. */
class JailManagerTest {

    private JailManager jail;
    private Player alice;

    @BeforeEach
    void setUp() {
        jail = new GameContext().getJail();
        alice = new Player("Alice", new Token("Car", "RED"));
    }

    @Test
    void sendingToJailMovesThePlayerAndChangesTheStatus() {
        alice.setPosition(25);

        jail.sendToJail(alice);

        assertEquals(Board.JAIL_POSITION, alice.getPosition());
        assertTrue(alice.isInJail());
        assertEquals(0, jail.getFailedAttempts(alice));
        // Lo spostamento e' diretto: non si passa dal Via e non si incassa nulla.
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
    }

    @Test
    void aDoubleReleasesThePlayerForFree() {
        jail.sendToJail(alice);

        jail.releaseWithDouble(alice);

        assertTrue(alice.isPlaying());
        assertEquals(Bank.STARTING_BALANCE, alice.getMoney());
    }

    @Test
    void payingTheBailReleasesThePlayer() {
        jail.sendToJail(alice);

        assertTrue(jail.payBailAndRelease(alice));

        assertTrue(alice.isPlaying());
        assertEquals(Bank.STARTING_BALANCE - JailManager.BAIL_AMOUNT, alice.getMoney());
    }

    @Test
    void failedAttemptsAreCountedUntilTheBailBecomesMandatory() {
        jail.sendToJail(alice);

        for (int attempt = 1; attempt < JailManager.MAX_ATTEMPTS; attempt++) {
            assertEquals(attempt, jail.registerFailedAttempt(alice));
            assertFalse(jail.hasUsedAllAttempts(alice));
        }
        assertEquals(JailManager.MAX_ATTEMPTS, jail.registerFailedAttempt(alice));
        assertTrue(jail.hasUsedAllAttempts(alice));
    }

    @Test
    void attemptsAreResetWhenThePlayerGoesBackToJail() {
        jail.sendToJail(alice);
        jail.registerFailedAttempt(alice);
        jail.releaseWithDouble(alice);

        jail.sendToJail(alice);

        assertEquals(0, jail.getFailedAttempts(alice));
    }

    @Test
    void aPlayerWhoCannotAffordTheBailGoesBankrupt() {
        final Player poor = new Player("Poor", new Token("Boot", "GREY"), JailManager.BAIL_AMOUNT - 1);
        jail.sendToJail(poor);

        assertFalse(jail.payBailAndRelease(poor));

        assertTrue(poor.isBankrupt());
        assertEquals(0, poor.getMoney());
    }

    @Test
    void theBailCanBeOfferedOnlyToAJailedPlayerWithEnoughMoney() {
        assertFalse(jail.canPayBail(alice), "Alice non e' in prigione");

        jail.sendToJail(alice);
        assertTrue(jail.canPayBail(alice));

        final Player poor = new Player("Poor", new Token("Boot", "GREY"), JailManager.BAIL_AMOUNT - 1);
        jail.sendToJail(poor);
        assertFalse(jail.canPayBail(poor));
    }
}
