package it.unibo.monopoly.model.board;

import java.util.List;

import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameContext;

/**
 * Terreno del tabellone: la proprieta' con un colore.
 * <p>
 * Aggiunge a {@link PropertyTile} l'unica regola che distingue un terreno dalle
 * altre proprieta': se il proprietario possiede <em>tutti</em> i terreni del suo
 * {@link ColorGroup} ha il monopolio su quel colore e l'affitto raddoppia.
 * <p>
 * Il gruppo non basta saperlo: per sapere se e' completo bisogna guardare il
 * tabellone, ed e' per questo che {@link #computeRent(Board, Dice)} lo riceve come
 * parametro. Il conteggio si fa qui e non in {@link Board} di proposito: il
 * tabellone conosce solo {@link Tile} e non deve imparare cos'e' un colore.
 */
public class StreetTile extends PropertyTile {

    /** Di quanto si moltiplica l'affitto quando il gruppo e' tutto dello stesso giocatore. */
    private static final int MONOPOLY_MULTIPLIER = 2;

    private final ColorGroup group;

    /**
     * Crea un terreno.
     *
     * @param name     nome del terreno (es. "Vicolo Corto")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param rent     affitto base, quello che si paga senza il monopolio del colore
     * @param group    il gruppo di colore a cui appartiene
     * @param context  banca, regole economiche ed eventi della partita
     * @throws IllegalArgumentException se i valori non sono validi o il gruppo e' null
     */
    public StreetTile(final String name, final int position, final int price, final int rent,
                      final ColorGroup group, final GameContext context) {
        super(name, position, price, rent, context);
        if (group == null) {
            throw new IllegalArgumentException("Il gruppo di colore non puo' essere null");
        }
        this.group = group;
    }

    /** @return il gruppo di colore del terreno */
    public ColorGroup getGroup() {
        return this.group;
    }

    /**
     * Affitto del terreno: quello base, raddoppiato se il proprietario ha tutto il
     * gruppo di colore.
     *
     * @param board il tabellone, che serve a trovare gli altri terreni dello stesso colore
     * @param dice  i dadi, che per un terreno non contano
     * @return l'affitto dovuto
     */
    @Override
    public int computeRent(final Board board, final Dice dice) {
        return this.ownerHasWholeGroup(board) ? this.getRent() * MONOPOLY_MULTIPLIER : this.getRent();
    }

    /** @return true se tutti i terreni del gruppo appartengono al proprietario di questo */
    private boolean ownerHasWholeGroup(final Board board) {
        final List<StreetTile> sameColor = board.getTiles().stream()
                .filter(StreetTile.class::isInstance)
                .map(StreetTile.class::cast)
                .filter(street -> street.group == this.group)
                .toList();
        // Se questa casella non sta su questo tabellone il confronto non ha senso:
        // senza il controllo un gruppo "vuoto" risulterebbe completo per chiunque.
        if (!sameColor.contains(this)) {
            return false;
        }
        return this.getOwner()
                .map(owner -> sameColor.stream().allMatch(street -> street.isOwnedBy(owner)))
                .orElse(false);
    }

    @Override
    public String toString() {
        return "StreetTile[position=" + this.getPosition()
                + ", name=" + this.getName()
                + ", group=" + this.group
                + ", rent=" + this.getRent()
                + "]";
    }
}
