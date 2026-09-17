package it.unibo.monopoly.model.board;

import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameContext;

/**
 * Stazione ferroviaria.
 * <p>
 * Le quattro stazioni non hanno un colore e non si possono migliorare: quello che
 * conta e' <em>quante</em> ne ha lo stesso proprietario. L'affitto parte da
 * {@value #BASE_RENT} e raddoppia per ogni stazione in piu', cioe' 25, 50, 100, 200.
 * <p>
 * Il conteggio si fa sul {@link Board} ricevuto da {@link #computeRent(Board, Dice)},
 * perche' le altre stazioni sono caselle del tabellone e non qualcosa che la singola
 * stazione possa sapere da sola.
 */
public class StationTile extends PropertyTile {

    /** Affitto con una sola stazione; raddoppia a ogni stazione in piu'. */
    public static final int BASE_RENT = 25;

    /**
     * Crea una stazione.
     *
     * @param name     nome della stazione (es. "Stazione Sud")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param context  banca, regole economiche ed eventi della partita
     * @throws IllegalArgumentException se i valori non sono validi o il contesto e' null
     */
    public StationTile(final String name, final int position, final int price, final GameContext context) {
        super(name, position, price, BASE_RENT, context);
    }

    /**
     * Affitto della stazione: {@value #BASE_RENT} raddoppiato per ogni altra stazione
     * dello stesso proprietario, cioe' 25, 50, 100 o 200.
     *
     * @param board il tabellone, che serve a contare le stazioni del proprietario
     * @param dice  i dadi, che per una stazione non contano
     * @return l'affitto dovuto
     */
    @Override
    public int computeRent(final Board board, final Dice dice) {
        // Almeno una: l'affitto lo si paga solo su una stazione che ha un proprietario.
        final int owned = Math.max(1, this.countStationsOfTheOwner(board));
        // 25 * 2^(n-1): lo spostamento a sinistra di n-1 posizioni e' proprio "* 2^(n-1)".
        return BASE_RENT * (1 << (owned - 1));
    }

    /** @return quante stazioni del tabellone appartengono al proprietario di questa */
    private int countStationsOfTheOwner(final Board board) {
        return this.getOwner()
                .map(owner -> (int) board.getTiles().stream()
                        .filter(StationTile.class::isInstance)
                        .map(StationTile.class::cast)
                        .filter(station -> station.isOwnedBy(owner))
                        .count())
                .orElse(0);
    }

    @Override
    public String toString() {
        return "StationTile[position=" + this.getPosition() + ", name=" + this.getName() + "]";
    }
}
