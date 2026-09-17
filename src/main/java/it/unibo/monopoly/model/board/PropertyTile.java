package it.unibo.monopoly.model.board;

import it.unibo.monopoly.model.economy.EconomyManager;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.player.Player;

/**
 * Casella acquistabile: terreno, stazione o societa'.
 * <p>
 * Estende {@link Property} del Giorno 1 e ne aggiunge l'unica cosa che le mancava:
 * il comportamento quando un giocatore ci si ferma sopra. La divisione dei compiti
 * e' voluta:
 * <ul>
 *   <li>{@code Property} descrive <em>che cos'e'</em> una proprieta' (nome, posizione,
 *       prezzo, affitto, proprietario) e resta un oggetto passivo, senza collaboratori;</li>
 *   <li>{@code PropertyTile} descrive <em>come si comporta</em> nel gioco, e per farlo
 *       ha bisogno delle regole economiche ({@link EconomyManager}).</li>
 * </ul>
 * Cosi' la parte di dominio resta riusabile e testabile da sola, e tutto il denaro
 * continua a passare da un solo punto.
 * <p>
 * Regole applicate da {@link #onLand(Player)}:
 * <ol>
 *   <li>casella libera: il giocatore la compra, se ha abbastanza denaro;</li>
 *   <li>casella di un altro giocatore: paga l'affitto al proprietario;</li>
 *   <li>casella sua: nessun effetto.</li>
 * </ol>
 */
public class PropertyTile extends Property {

    private final GameContext context;
    private final EconomyManager economy;

    /**
     * Crea una casella acquistabile.
     * <p>
     * Riceve tutto il {@link GameContext} e non solo le regole economiche perche', per
     * offrire l'acquisto, deve poter registrare la decisione in sospeso nella partita e
     * annunciare l'offerta sul canale degli eventi.
     *
     * @param name     nome della proprieta' (es. "Vicolo Corto")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param rent     affitto dovuto da chi ci si ferma
     * @param context  banca, regole economiche ed eventi della partita
     * @throws IllegalArgumentException se i valori non sono validi o il contesto e' null
     */
    public PropertyTile(final String name, final int position, final int price,
                        final int rent, final GameContext context) {
        super(name, position, price, rent);
        if (context == null) {
            throw new IllegalArgumentException("Il contesto della partita non puo' essere null");
        }
        this.context = context;
        this.economy = context.getEconomy();
    }

    /**
     * Applica le regole della casella proprieta'.
     * <p>
     * L'acquisto non e' piu' automatico: e' una scelta del giocatore. Se la casella e'
     * libera e lui se la puo' permettere, qui non si compra niente, si registra solo
     * l'offerta nella partita e la si annuncia alle view. La partita non si blocca ad
     * aspettare la risposta: resta in attesa nella fase
     * {@link it.unibo.monopoly.model.game.GamePhase#AWAITING_PURCHASE_DECISION
     * AWAITING_PURCHASE_DECISION} finche' il giocatore non risponde passando dal
     * {@link it.unibo.monopoly.controller.GameEngine GameEngine}.
     * <p>
     * Affitto e tasse invece restano automatici, perche' non sono scelte: sono
     * obblighi.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        if (this.isAvailable()) {
            this.offerTo(player);
            return;
        }
        if (!this.isOwnedBy(player)) {
            this.economy.payRent(player, this);
        }
        // Se la casella e' gia' sua non succede nulla: non si paga l'affitto a se' stessi.
    }

    /**
     * Propone l'acquisto al giocatore, se se lo puo' permettere.
     * <p>
     * A chi non ha abbastanza soldi non si chiede niente: la proprieta' resta libera e
     * il turno va avanti come se la casella non avesse alcun effetto. Cosi' la GUI non
     * deve mostrare una domanda a cui il giocatore non potrebbe comunque rispondere di
     * si'.
     */
    private void offerTo(final Player player) {
        final int price = this.getPrice();
        if (!this.economy.getBank().canAfford(player, price)) {
            return;
        }
        this.context.getState().offerPurchase(player, this);
        this.context.getEvents().fire(listener -> listener.onPurchaseOffered(player, this, price));
    }
}
