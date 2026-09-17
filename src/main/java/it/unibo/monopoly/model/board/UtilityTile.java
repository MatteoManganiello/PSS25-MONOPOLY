package it.unibo.monopoly.model.board;

import java.util.List;

import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameContext;

/**
 * Societa' (elettrica e acqua potabile).
 * <p>
 * E' l'unica proprieta' il cui affitto non e' un importo scritto sulla casella: si
 * calcola sul lancio di dadi che ha portato il giocatore fin li', moltiplicato per
 * {@value #SINGLE_MULTIPLIER} se il proprietario ha una sola societa' o per
 * {@value #BOTH_MULTIPLIER} se le ha tutte e due.
 * <p>
 * Per questo l'affitto base di una societa' e' {@value #NO_FIXED_RENT}: non esiste
 * un importo fisso da mostrare o da pagare, e {@link #computeRent(Board, Dice)} non
 * lo usa mai. E' anche il motivo per cui il calcolo dell'affitto riceve i
 * {@link Dice}: senza il valore dell'ultimo lancio, qui, non si puo' rispondere.
 */
public class UtilityTile extends PropertyTile {

    /** Moltiplicatore dei dadi quando il proprietario ha una sola societa'. */
    public static final int SINGLE_MULTIPLIER = 4;

    /** Moltiplicatore dei dadi quando il proprietario le ha tutte e due. */
    public static final int BOTH_MULTIPLIER = 10;

    /** Le societa' non hanno un affitto fisso: dipende sempre dai dadi. */
    private static final int NO_FIXED_RENT = 0;

    /**
     * Crea una societa'.
     *
     * @param name     nome della societa' (es. "Societa' Elettrica")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param context  banca, regole economiche ed eventi della partita
     * @throws IllegalArgumentException se i valori non sono validi o il contesto e' null
     */
    public UtilityTile(final String name, final int position, final int price, final GameContext context) {
        super(name, position, price, NO_FIXED_RENT, context);
    }

    /**
     * Affitto della societa': il valore dei dadi appena tirati moltiplicato per 4
     * (una sola societa') o per 10 (tutte e due).
     *
     * @param board il tabellone, che serve a vedere se il proprietario ha l'altra societa'
     * @param dice  i dadi, il cui ultimo lancio e' la base del calcolo
     * @return l'affitto dovuto
     */
    @Override
    public int computeRent(final Board board, final Dice dice) {
        final int multiplier = this.ownerHasAllUtilities(board) ? BOTH_MULTIPLIER : SINGLE_MULTIPLIER;
        return dice.getTotal() * multiplier;
    }

    /** @return true se tutte le societa' del tabellone sono del proprietario di questa */
    private boolean ownerHasAllUtilities(final Board board) {
        final List<UtilityTile> utilities = board.getTiles().stream()
                .filter(UtilityTile.class::isInstance)
                .map(UtilityTile.class::cast)
                .toList();
        // Stesso controllo di StreetTile: se la casella non e' su questo tabellone non
        // si puo' parlare di "tutte le societa'".
        if (!utilities.contains(this)) {
            return false;
        }
        return this.getOwner()
                .map(owner -> utilities.stream().allMatch(utility -> utility.isOwnedBy(owner)))
                .orElse(false);
    }

    @Override
    public String toString() {
        return "UtilityTile[position=" + this.getPosition() + ", name=" + this.getName() + "]";
    }
}
