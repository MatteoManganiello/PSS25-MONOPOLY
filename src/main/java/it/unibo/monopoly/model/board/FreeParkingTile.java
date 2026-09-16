package it.unibo.monopoly.model.board;

import it.unibo.monopoly.model.economy.EconomyManager;
import it.unibo.monopoly.model.player.Player;

/**
 * Posteggio gratuito: per il regolamento ufficiale e' una casella di sosta, senza
 * alcun effetto. Chi ci si ferma non paga e non incassa nulla.
 */
public class FreeParkingTile extends Tile {

    /**
     * Crea un posteggio gratuito senza effetto (regola ufficiale).
     *
     * @param name     nome della casella (es. "Posteggio gratuito")
     * @param position posizione sul tabellone, normalmente {@link Board#FREE_PARKING_POSITION}
     * @param economy  le regole economiche della partita
     * @throws IllegalArgumentException se le regole economiche sono null
     */
    public FreeParkingTile(final String name, final int position, final EconomyManager economy) {
        super(name, position);
        if (economy == null) {
            throw new IllegalArgumentException("Le regole economiche non possono essere null");
        }
    }

    /** @return {@link TileCategory#FREE_PARKING}: e' il posteggio gratuito */
    @Override
    public TileCategory getCategory() {
        return TileCategory.FREE_PARKING;
    }

    /** @return sempre "Sosta libera": la casella non ha nulla da mostrare */
    @Override
    public String getDetail() {
        return "Sosta libera";
    }

    /**
     * Non fa nulla: con la regola ufficiale il posteggio gratuito e' una semplice
     * sosta, quindi non c'e' nessun effetto da applicare.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        // Nessun effetto per il regolamento ufficiale.
    }
}
