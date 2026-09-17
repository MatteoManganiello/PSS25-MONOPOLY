package it.unibo.monopoly.model.board;

import it.unibo.monopoly.model.economy.EconomyManager;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.game.GameState;
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
 *   <li>casella libera: al giocatore viene offerto l'acquisto, se se lo puo' permettere;</li>
 *   <li>casella di un altro giocatore: paga l'affitto al proprietario;</li>
 *   <li>casella sua: nessun effetto.</li>
 * </ol>
 * <p>
 * <b>Affitto</b>: {@link #computeRent(Board, Dice)} e' il punto di estensione
 * polimorfico delle tre proprieta' vere del tabellone ({@link StreetTile},
 * {@link StationTile}, {@link UtilityTile}), che lo calcolano ognuna a modo suo.
 * Qui l'implementazione di base restituisce l'affitto fisso scritto sulla casella,
 * cosi' una proprieta' senza regole particolari resta utilizzabile com'e'.
 */
public class PropertyTile extends Property {

    private final GameContext context;
    private final EconomyManager economy;

    /**
     * Crea una casella acquistabile.
     * <p>
     * Riceve tutto il {@link GameContext} e non solo le regole economiche perche', per
     * offrire l'acquisto, deve poter registrare la decisione in sospeso nella partita e
     * annunciare l'offerta sul canale degli eventi; per calcolare l'affitto gli servono
     * poi il tabellone e i dadi, che vivono anch'essi nella partita.
     *
     * @param name     nome della proprieta' (es. "Vicolo Corto")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param rent     affitto base dovuto da chi ci si ferma
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
     * L'acquisto non e' automatico: e' una scelta del giocatore. Se la casella e'
     * libera e lui se la puo' permettere, qui non si compra niente, si registra solo
     * l'offerta nella partita e la si annuncia alle view. La partita non si blocca ad
     * aspettare la risposta: resta in attesa nella fase
     * {@link it.unibo.monopoly.model.game.GamePhase#AWAITING_PURCHASE_DECISION
     * AWAITING_PURCHASE_DECISION} finche' il giocatore non risponde passando dal
     * {@link it.unibo.monopoly.controller.GameEngine GameEngine}.
     * <p>
     * Affitto e tasse invece restano automatici, perche' non sono scelte: sono
     * obblighi. L'importo pero' non e' piu' fisso: lo decide {@link #getCurrentRent()},
     * cioe' la sottoclasse concreta della casella.
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
            this.economy.payRent(player, this, this.getCurrentRent());
        }
        // Se la casella e' gia' sua non succede nulla: non si paga l'affitto a se' stessi.
    }

    /**
     * Affitto che si pagherebbe adesso su questa casella.
     * <p>
     * E' il metodo comodo per chi ha in mano solo la casella (la casella stessa in
     * {@link #onLand(Player)}, la view per il tooltip): prende tabellone e dadi dalla
     * partita e chiama {@link #computeRent(Board, Dice)}.
     *
     * @return l'importo dovuto da chi si ferma qui, nella situazione attuale
     * @throws IllegalStateException se la casella non appartiene a nessuna partita
     */
    public int getCurrentRent() {
        final GameState game = this.context.getState();
        return this.computeRent(game.getBoard(), game.getDice());
    }

    /**
     * Calcola l'affitto dovuto da chi si ferma sulla casella.
     * <p>
     * E' <em>il</em> metodo polimorfico dell'affitto: chi lo chiama non sa se la
     * casella sia un terreno, una stazione o una societa', e non deve saperlo. Le
     * informazioni che servono alle varie regole arrivano come parametri invece che
     * da campi nascosti, cosi' il calcolo si puo' provare nei test senza far girare
     * una partita:
     * <ul>
     *   <li>il {@link Board} serve a contare le caselle dello stesso tipo o dello
     *       stesso colore possedute dal proprietario (monopolio, stazioni);</li>
     *   <li>i {@link Dice} servono alle societa', il cui affitto dipende dal lancio
     *       appena fatto.</li>
     * </ul>
     * L'implementazione di base restituisce l'affitto fisso della casella; sono le
     * sottoclassi a ridefinirla.
     *
     * @param board il tabellone su cui si sta giocando
     * @param dice  i dadi della partita, con il valore dell'ultimo lancio
     * @return l'importo dell'affitto
     */
    public int computeRent(final Board board, final Dice dice) {
        return this.getRent();
    }

    /** @return il contesto della partita, a disposizione delle sottoclassi */
    protected GameContext getContext() {
        return this.context;
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
