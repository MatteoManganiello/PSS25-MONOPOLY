package monopoly.model.board;

import monopoly.model.economy.EconomyManager;
import monopoly.model.player.Player;

/**
 * Casella delle tasse: chi ci si ferma versa un importo alla banca.
 * <p>
 * L'importo e' fisso (scelta piu' semplice e piu' facile da spiegare della
 * percentuale sul patrimonio), ma non e' letto direttamente dal campo: passa da
 * {@link #computeAmount(Player)}. Cosi' una futura casella a percentuale dovrebbe
 * solo estendere questa classe e ridefinire quel metodo, senza toccare
 * {@link #onLand(Player)} ne' le regole economiche.
 * <p>
 * Se il giocatore non riesce a pagare, l'{@link EconomyManager} lo dichiara fallito
 * verso la banca.
 */
public class TaxTile extends Tile {

    private final int amount;
    private final EconomyManager economy;

    /**
     * Crea una casella delle tasse.
     *
     * @param name     nome della tassa (es. "Tassa patrimoniale")
     * @param position posizione sul tabellone
     * @param amount   importo dovuto, strettamente positivo
     * @param economy  le regole economiche della partita
     * @throws IllegalArgumentException se l'importo non e' positivo o l'economia e' null
     */
    public TaxTile(final String name, final int position, final int amount, final EconomyManager economy) {
        super(name, position);
        if (amount <= 0) {
            throw new IllegalArgumentException("L'importo di una tassa deve essere positivo");
        }
        if (economy == null) {
            throw new IllegalArgumentException("Le regole economiche non possono essere null");
        }
        this.amount = amount;
        this.economy = economy;
    }

    /** @return l'importo base della tassa */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Calcola quanto deve pagare il giocatore indicato.
     * <p>
     * Qui restituisce sempre l'importo fisso; e' il punto da ridefinire per una
     * tassa proporzionale al patrimonio.
     *
     * @param player il giocatore che deve pagare
     * @return l'importo dovuto
     */
    protected int computeAmount(final Player player) {
        return this.amount;
    }

    /**
     * Fa pagare la tassa alla banca.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        this.economy.payToBank(player, this.getName(), this.computeAmount(player));
    }
}
