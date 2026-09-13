package monopoly.model.board;

import monopoly.model.economy.EconomyManager;
import monopoly.model.economy.Property;
import monopoly.model.player.Player;

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

    private final EconomyManager economy;

    /**
     * Crea una casella acquistabile.
     *
     * @param name     nome della proprieta' (es. "Vicolo Corto")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param rent     affitto dovuto da chi ci si ferma
     * @param economy  le regole economiche della partita
     * @throws IllegalArgumentException se i valori non sono validi o l'economia e' null
     */
    public PropertyTile(final String name, final int position, final int price,
                        final int rent, final EconomyManager economy) {
        super(name, position, price, rent);
        if (economy == null) {
            throw new IllegalArgumentException("Le regole economiche non possono essere null");
        }
        this.economy = economy;
    }

    /**
     * Applica le tre regole della casella proprieta'.
     * <p>
     * L'acquisto e' automatico quando il giocatore puo' permetterselo: finche' non
     * c'e' un'interfaccia con cui rispondere "compro / non compro" non ha senso
     * chiedere. Quando al Giorno 4 arrivera' la GUI bastera' sostituire la chiamata
     * a {@code buyProperty} con la richiesta di una decisione al giocatore: le
     * regole economiche resteranno le stesse.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        if (this.isAvailable()) {
            this.economy.buyProperty(player, this);
        } else if (!this.isOwnedBy(player)) {
            this.economy.payRent(player, this);
        }
        // Se la casella e' gia' sua non succede nulla: non si paga l'affitto a se' stessi.
    }
}
