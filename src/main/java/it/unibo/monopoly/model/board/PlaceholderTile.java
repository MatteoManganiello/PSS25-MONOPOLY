package it.unibo.monopoly.model.board;

import it.unibo.monopoly.model.player.Player;

/**
 * Casella segnaposto senza alcun effetto.
 * <p>
 * Nel tabellone standard occupa le caselle "Imprevisti" e "Probabilita'", che in
 * questa versione non hanno alcun effetto. E' l'esempio piu' evidente del vantaggio
 * del polimorfismo: il tabellone e' completo e giocabile anche senza quelle regole,
 * e quelle caselle potrebbero essere sostituite una alla volta senza toccare
 * {@link Board}, il {@link it.unibo.monopoly.model.game.TurnManager TurnManager} o la view.
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
     * @return {@link TileCategory#CARD}: nel tabellone standard il segnaposto occupa
     *         proprio le caselle "Imprevisti" e "Probabilita'"
     */
    @Override
    public TileCategory getCategory() {
        return TileCategory.CARD;
    }

    /** @return un punto interrogativo: la casella non ha un effetto da mostrare */
    @Override
    public String getDetail() {
        return "?";
    }

    /**
     * Non produce alcun effetto: il giocatore si ferma e basta.
     *
     * @param player il giocatore che si e' fermato sulla casella (ignorato)
     */
    @Override
    public void onLand(final Player player) {
        // Nessun effetto: e' un segnaposto.
    }
}
