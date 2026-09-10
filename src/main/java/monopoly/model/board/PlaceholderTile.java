package monopoly.model.board;

import monopoly.model.player.Player;

/**
 * Casella segnaposto senza alcun effetto.
 * <p>
 * Serve a riempire il tabellone finche' le caselle definitive (proprieta',
 * imprevisti, probabilita', tasse, prigione, ...) non saranno implementate:
 * in questo modo {@link Board} e' gia' completa e utilizzabile, e le singole
 * caselle potranno essere sostituite una alla volta senza toccare il resto del codice.
 */
public class PlaceholderTile extends Tile {

    /**
     * Crea una casella segnaposto.
     *
     * @param name     nome della casella
     * @param position posizione sul tabellone
     */
    public PlaceholderTile(final String name, final int position) {
        super(name, position);
    }

    /**
     * Non produce alcun effetto: il giocatore si ferma e basta.
     *
     * @param player il giocatore che si e' fermato sulla casella (ignorato)
     */
    @Override
    public void onLand(final Player player) {
        // Nessun effetto: sara' la casella definitiva a implementare la regola.
    }
}
