package monopoly.model.board;

import monopoly.model.player.Player;

/**
 * Casella segnaposto senza alcun effetto.
 * <p>
 * Nata al Giorno 1 per riempire il tabellone prima che esistessero le caselle vere,
 * ha ancora un compito preciso: nel tabellone standard occupa le caselle
 * "Imprevisti" e "Probabilita'", che avranno un effetto solo quando ci sara' il
 * mazzo di carte. E' l'esempio piu' evidente del vantaggio del polimorfismo: il
 * tabellone e' completo e giocabile anche se una parte delle regole non c'e' ancora,
 * e quelle caselle potranno essere sostituite una alla volta senza toccare
 * {@link Board}, il {@link monopoly.model.game.TurnManager TurnManager} o la view.
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
