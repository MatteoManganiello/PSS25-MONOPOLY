package it.unibo.monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.game.GameEventListener;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * Test delle caselle "Imprevisti" e "Probabilita'": pesca della carta, effetto sul
 * denaro ed evento per la view.
 */
class CardTileTest {

    private static final int MONEY = 500;
    private static final long SEED = 42L;

    private GameContext context;
    private Player alice;

    @BeforeEach
    void setUp() {
        context = new GameContext();
        alice = new Player("Alice", new Token("Car", "RED"), MONEY);
    }

    /** Una casella con un mazzo di una sola carta: la pesca e' sempre quella. */
    private CardTile tileWith(final Card card) {
        return new CardTile("Imprevisti", 7, List.of(card), context, new Random(SEED));
    }

    @Test
    void aGainCardPaysThePlayerFromTheBank() {
        final int bankBefore = context.getBank().getBalance();

        tileWith(new Card("Errore della banca a vostro favore", 200)).onLand(alice);

        assertEquals(MONEY + 200, alice.getMoney());
        assertEquals(bankBefore - 200, context.getBank().getBalance());
    }

    @Test
    void aPaymentCardChargesThePlayer() {
        final int bankBefore = context.getBank().getBalance();

        tileWith(new Card("Multa", -50)).onLand(alice);

        assertEquals(MONEY - 50, alice.getMoney());
        assertEquals(bankBefore + 50, context.getBank().getBalance());
    }

    @Test
    void whoCannotPayTheCardGoesBankrupt() {
        final Player poor = new Player("Poor", new Token("Boot", "GREY"), 10);

        tileWith(new Card("Retta scolastica", -150)).onLand(poor);

        assertTrue(poor.isBankrupt());
    }

    @Test
    void theDrawnCardIsAnnouncedBeforeItsEffect() {
        final Card card = new Card("Dividendo", 50);
        final List<String> events = new ArrayList<>();
        context.getEvents().addListener(new GameEventListener() {
            @Override
            public void onCardDrawn(final Player player, final String deck, final Card drawn) {
                events.add("carta " + deck + ": " + drawn.text() + " a " + player.getName());
            }

            @Override
            public void onMoneyReceivedFromBank(final Player player, final String reason, final int amount) {
                events.add("incasso " + amount + " (" + reason + ")");
            }
        });

        tileWith(card).onLand(alice);
        context.getEvents().publishPending();

        assertEquals(List.of("carta Imprevisti: Dividendo a Alice", "incasso 50 (Imprevisti)"), events);
    }

    @Test
    void everyDrawComesFromTheTileDeck() {
        final CardTile tile = new CardTile("Probabilita'", 2, BoardFactory.COMMUNITY_CHEST_CARDS,
                context, new Random(SEED));
        final List<Card> drawn = new ArrayList<>();
        context.getEvents().addListener(new GameEventListener() {
            @Override
            public void onCardDrawn(final Player player, final String deck, final Card card) {
                drawn.add(card);
            }
        });

        for (int i = 0; i < 20; i++) {
            // Denaro abbondante: nessuna carta deve far fallire il giocatore durante il test.
            tile.onLand(new Player("Ricco", new Token("Hat", "BLACK"), 10_000));
        }
        context.getEvents().publishPending();

        assertEquals(20, drawn.size());
        assertTrue(BoardFactory.COMMUNITY_CHEST_CARDS.containsAll(drawn));
    }

    @Test
    void theStandardBoardDealsTheRightDeckToEachCardTile() {
        final Board board = BoardFactory.createStandardBoard(context);
        for (final Tile tile : board.getTiles()) {
            if (tile instanceof CardTile cardTile) {
                final List<Card> expected = "Imprevisti".equals(tile.getName())
                        ? BoardFactory.CHANCE_CARDS
                        : BoardFactory.COMMUNITY_CHEST_CARDS;
                assertEquals(expected, cardTile.getDeck(), tile.getName() + " pesca dal mazzo sbagliato");
            }
        }
    }

    @Test
    void bothDecksHaveGainsAndPayments() {
        for (final List<Card> deck : List.of(BoardFactory.CHANCE_CARDS, BoardFactory.COMMUNITY_CHEST_CARDS)) {
            assertTrue(deck.stream().anyMatch(Card::isGain));
            assertFalse(deck.stream().allMatch(Card::isGain));
        }
    }

    @Test
    void invalidCardsAndDecksAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Card("Niente", 0));
        assertThrows(IllegalArgumentException.class, () -> new Card(" ", 10));
        assertThrows(IllegalArgumentException.class,
                () -> new CardTile("Imprevisti", 7, List.of(), context, new Random()));
    }
}
