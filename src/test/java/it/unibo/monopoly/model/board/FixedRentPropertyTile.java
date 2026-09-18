package it.unibo.monopoly.model.board;

import it.unibo.monopoly.model.game.GameContext;

/**
 * Proprieta' finta per i test: una {@link PropertyTile} senza regole particolari,
 * che fa pagare sempre l'affitto fisso scritto sulla casella.
 * <p>
 * Serve perche' {@code PropertyTile} e' astratta: i test sulle regole comuni a tutte
 * le proprieta' (offerta di acquisto, pagamento dell'affitto, categoria e prezzo
 * mostrati dalla view) non devono dipendere da quelle di terreni, stazioni o societa'.
 */
class FixedRentPropertyTile extends PropertyTile {

    /**
     * @param name     nome della proprieta'
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param rent     affitto fisso dovuto da chi ci si ferma
     * @param context  banca, regole economiche ed eventi della partita
     */
    FixedRentPropertyTile(final String name, final int position, final int price,
                          final int rent, final GameContext context) {
        super(name, position, price, rent, context);
    }
}
