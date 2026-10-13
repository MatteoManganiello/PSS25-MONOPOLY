package it.unibo.monopoly.model.board;

import java.util.List;
import java.util.Random;

import it.unibo.monopoly.model.economy.EconomyManager;
import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.game.GameEventSupport;
import it.unibo.monopoly.model.player.Player;

/**
 * Casella "Imprevisti" o "Probabilita'": chi ci si ferma pesca una carta dal mazzo e ne
 * subisce l'effetto.
 * <p>
 * La carta viene scelta a caso a ogni pesca, senza togliere nulla dal mazzo: e' come
 * rimettere ogni carta in fondo e rimescolare. Cosi' la casella non ha stato da
 * ricordare, e il salvataggio della partita non deve conservare l'ordine dei mazzi.
 * Le tre caselle dello stesso tipo ricevono lo stesso elenco di carte.
 * <p>
 * Prima dell'effetto viene annunciata la carta pescata
 * ({@link it.unibo.monopoly.model.game.GameEventListener#onCardDrawn onCardDrawn}), cosi'
 * la view puo' mostrarne il testo; poi il denaro passa per l'{@link EconomyManager},
 * come per le tasse: chi non riesce a pagare fallisce verso la banca.
 * <p>
 * Il generatore casuale arriva dal costruttore, come per i
 * {@link it.unibo.monopoly.model.game.Dice Dice}: nei test si usa un seme fisso.
 */
public class CardTile extends Tile {

    private final List<Card> deck;
    private final EconomyManager economy;
    private final GameEventSupport events;
    private final Random random;

    /**
     * Crea una casella che pesca dal mazzo indicato.
     *
     * @param name     nome della casella, che e' anche il nome del mazzo (es. "Imprevisti")
     * @param position posizione sul tabellone
     * @param deck     le carte del mazzo, almeno una
     * @param context  i servizi della partita (regole economiche e canale degli eventi)
     * @param random   il generatore con cui scegliere la carta
     * @throws IllegalArgumentException se il mazzo e' vuoto o un parametro e' null
     */
    public CardTile(final String name, final int position, final List<Card> deck,
                    final GameContext context, final Random random) {
        super(name, position);
        if (deck == null || deck.isEmpty()) {
            throw new IllegalArgumentException("Il mazzo deve contenere almeno una carta");
        }
        if (context == null || random == null) {
            throw new IllegalArgumentException("Il contesto e il generatore casuale non possono essere null");
        }
        this.deck = List.copyOf(deck);
        this.economy = context.getEconomy();
        this.events = context.getEvents();
        this.random = random;
    }

    /** @return {@link TileCategory#CARD}: la casella fa pescare una carta */
    @Override
    public TileCategory getCategory() {
        return TileCategory.CARD;
    }

    /** @return un punto interrogativo: la carta che uscira' non si conosce in anticipo */
    @Override
    public String getDetail() {
        return "?";
    }

    /** @return le carte del mazzo, in sola lettura */
    public List<Card> getDeck() {
        return this.deck;
    }

    /**
     * Fa pescare una carta al giocatore e ne applica l'effetto.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        final Card card = this.deck.get(this.random.nextInt(this.deck.size()));
        this.events.fire(listener -> listener.onCardDrawn(player, this.getName(), card));
        if (card.isGain()) {
            this.economy.receiveFromBank(player, this.getName(), card.amount());
        } else {
            this.economy.payToBank(player, this.getName(), -card.amount());
        }
    }
}
