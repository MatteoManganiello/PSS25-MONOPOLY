package monopoly.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.board.PropertyTile;
import monopoly.model.economy.Bank;
import monopoly.model.economy.Property;
import monopoly.model.game.Dice;
import monopoly.model.game.GameState;
import monopoly.model.game.RollResult;
import monopoly.model.player.Player;
import monopoly.model.player.Token;

/**
 * Test del collegamento fra effetti delle caselle e osservatori: cio' che succede
 * nel model (acquisti, affitti, fallimenti) deve arrivare alla view, e nell'ordine
 * giusto rispetto a dadi e movimento.
 */
class GameEngineEconomyTest {

    /** Lanci: (4+5), (2+4), ... nessun doppio. */
    private static final long SEED_NO_DOUBLE = 1;

    private static final int VESUVIO = 9;

    /** Osservatore di test: registra come testo gli eventi ricevuti, nell'ordine di arrivo. */
    private static final class RecordingObserver implements GameObserver {

        private final List<String> events = new ArrayList<>();

        @Override
        public void onDiceRolled(final RollResult result) {
            events.add("dice:" + result.firstDie() + "+" + result.secondDie());
        }

        @Override
        public void onPlayerMoved(final Player player, final monopoly.model.board.Tile from,
                                  final monopoly.model.board.Tile to) {
            events.add("moved:" + player.getName() + ":" + from.getPosition() + "->" + to.getPosition());
        }

        @Override
        public void onPropertyBought(final Player buyer, final Property property, final int price) {
            events.add("bought:" + buyer.getName() + ":" + property.getName() + ":" + price);
        }

        @Override
        public void onRentPaid(final Player tenant, final Player owner,
                               final Property property, final int amount) {
            events.add("rent:" + tenant.getName() + "->" + owner.getName() + ":" + amount);
        }

        @Override
        public void onPlayerBankrupt(final Player player, final Optional<Player> creditor) {
            events.add("bankrupt:" + player.getName() + ":" + creditor.map(Player::getName).orElse("bank"));
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

    private Player bob;
    private RecordingObserver observer;

    @BeforeEach
    void setUp() {
        bob = new Player("Bob", new Token("Dog", "BLUE"));
        observer = new RecordingObserver();
    }

    private GameEngine newEngine(final Player firstPlayer) {
        final GameState state = GameState.createStandardGame(
                List.of(firstPlayer, bob), new Dice(new Random(SEED_NO_DOUBLE)));
        final GameEngine engine = new GameEngine(state);
        engine.addObserver(observer);
        return engine;
    }

    @Test
    void thePurchaseIsAnnouncedAfterTheMovementAndBeforeTheStateChange() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = newEngine(alice);
        engine.startGame();
        observer.events.clear();

        engine.rollDice(); // 4 + 5 -> casella 9, "Viale Vesuvio", libera

        assertEquals(List.of("dice:4+5", "moved:Alice:0->9", "bought:Alice:Viale Vesuvio:120", "changed"),
                observer.events);
    }

    @Test
    void rentAndBankruptcyAreAnnouncedAndEndTheGame() {
        // Alice non puo' pagare l'affitto di Viale Vesuvio, che e' di Bob.
        final Player alice = new Player("Alice", new Token("Car", "RED"), 1);
        final GameEngine engine = newEngine(alice);
        final PropertyTile vesuvio = (PropertyTile) engine.getState().getBoard().getTileAt(VESUVIO);
        engine.getState().getContext().getEconomy().buyProperty(bob, vesuvio);
        engine.startGame();
        observer.events.clear();

        engine.rollDice();

        assertEquals(List.of("dice:4+5", "moved:Alice:0->9", "bankrupt:Alice:Bob", "changed", "over:Bob"),
                observer.events);
        assertTrue(engine.isGameOver());
        assertEquals(Optional.of(bob), engine.getWinner());
    }

    @Test
    void aRemovedObserverNoLongerReceivesEconomyEvents() {
        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final GameEngine engine = newEngine(alice);
        engine.startGame();
        engine.removeObserver(observer);
        observer.events.clear();

        engine.rollDice();

        assertTrue(observer.events.isEmpty());
        // L'acquisto e' comunque avvenuto: gli eventi servono solo a raccontarlo.
        assertEquals(Bank.STARTING_BALANCE - 120, alice.getMoney());
    }
}
