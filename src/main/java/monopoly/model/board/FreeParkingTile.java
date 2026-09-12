package monopoly.model.board;

import monopoly.model.economy.EconomyManager;
import monopoly.model.player.Player;

/**
 * Posteggio gratuito: per il regolamento ufficiale e' una casella di sosta, senza
 * alcun effetto.
 * <p>
 * E' pero' predisposta per la variante casalinga piu' diffusa, il <em>jackpot</em>:
 * tasse e multe finiscono in un montepremi che viene incassato da chi si ferma qui.
 * La variante e' <b>disattivata di default</b>; per usarla basta costruire la casella
 * con {@code jackpotEnabled = true} e alimentare il montepremi con
 * {@link #addToPot(int)} (per esempio da un ascoltatore degli eventi che intercetti
 * i pagamenti alla banca). Finche' resta disattivata la casella si comporta come una
 * normale casella neutra.
 */
public class FreeParkingTile extends Tile {

    private final EconomyManager economy;
    private final boolean jackpotEnabled;
    private int pot;

    /**
     * Crea un posteggio gratuito senza effetto (regola ufficiale).
     *
     * @param name     nome della casella (es. "Posteggio gratuito")
     * @param position posizione sul tabellone, normalmente {@link Board#FREE_PARKING_POSITION}
     * @param economy  le regole economiche della partita
     */
    public FreeParkingTile(final String name, final int position, final EconomyManager economy) {
        this(name, position, economy, false);
    }

    /**
     * Crea un posteggio gratuito scegliendo se attivare la variante del jackpot.
     *
     * @param name           nome della casella
     * @param position       posizione sul tabellone
     * @param economy        le regole economiche della partita
     * @param jackpotEnabled true per attivare il montepremi
     * @throws IllegalArgumentException se le regole economiche sono null
     */
    public FreeParkingTile(final String name, final int position,
                           final EconomyManager economy, final boolean jackpotEnabled) {
        super(name, position);
        if (economy == null) {
            throw new IllegalArgumentException("Le regole economiche non possono essere null");
        }
        this.economy = economy;
        this.jackpotEnabled = jackpotEnabled;
        this.pot = 0;
    }

    /** @return true se la variante del montepremi e' attiva */
    public boolean isJackpotEnabled() {
        return this.jackpotEnabled;
    }

    /** @return il montepremi attualmente accumulato */
    public int getPot() {
        return this.pot;
    }

    /**
     * Aggiunge denaro al montepremi. Non ha effetto se la variante e' disattivata.
     *
     * @param amount importo da aggiungere, non negativo
     * @throws IllegalArgumentException se l'importo e' negativo
     */
    public void addToPot(final int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Il montepremi non puo' essere alimentato con importi negativi");
        }
        if (this.jackpotEnabled) {
            this.pot += amount;
        }
    }

    /**
     * Con la regola ufficiale non fa nulla; con la variante attiva consegna il
     * montepremi al giocatore e lo azzera.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        if (!this.jackpotEnabled || this.pot == 0) {
            return;
        }
        final int prize = this.pot;
        this.pot = 0;
        this.economy.receiveFromBank(player, "montepremi del posteggio", prize);
    }
}
