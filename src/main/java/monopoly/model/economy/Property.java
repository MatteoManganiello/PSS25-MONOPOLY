package monopoly.model.economy;

import java.util.Optional;

import monopoly.model.board.Tile;
import monopoly.model.player.Player;

/**
 * Casella acquistabile del tabellone (terreno, stazione o societa').
 * <p>
 * Estende {@link Tile} perche' una proprieta' <em>e'</em> una casella: ha nome e
 * posizione come tutte le altre, ma in piu' ha un prezzo, un affitto e un
 * eventuale proprietario. Sara' a sua volta la classe base delle proprieta'
 * specializzate (terreni con case/alberghi, stazioni, societa'), che
 * ridefiniranno il calcolo dell'affitto.
 */
public class Property extends Tile {

    private final int price;
    private final int rent;

    /** Proprietario attuale; {@code null} significa "in vendita dalla banca". */
    private Player owner;

    /**
     * Crea una proprieta' non ancora acquistata da nessuno.
     *
     * @param name     nome della proprieta' (es. "Vicolo Corto")
     * @param position posizione sul tabellone
     * @param price    prezzo di acquisto dalla banca
     * @param rent     affitto base dovuto da chi si ferma sulla casella
     * @throws IllegalArgumentException se prezzo o affitto sono negativi
     */
    public Property(final String name, final int position, final int price, final int rent) {
        super(name, position);
        if (price < 0) {
            throw new IllegalArgumentException("Il prezzo non puo' essere negativo");
        }
        if (rent < 0) {
            throw new IllegalArgumentException("L'affitto non puo' essere negativo");
        }
        this.price = price;
        this.rent = rent;
        this.owner = null;
    }

    /** @return il prezzo di acquisto */
    public int getPrice() {
        return this.price;
    }

    /**
     * Affitto dovuto da chi si ferma sulla casella.
     * <p>
     * E' un metodo (e non un semplice accesso al campo) proprio per poter essere
     * ridefinito dalle sottoclassi: un terreno con le case, una stazione o una
     * societa' calcolano l'affitto in modi diversi.
     *
     * @return l'importo dell'affitto
     */
    public int getRent() {
        return this.rent;
    }

    /**
     * @return il proprietario attuale, oppure {@link Optional#empty()} se la
     *         proprieta' e' ancora della banca. L'Optional rende esplicita
     *         l'assenza di proprietario ed evita NullPointerException.
     */
    public Optional<Player> getOwner() {
        return Optional.ofNullable(this.owner);
    }

    /**
     * Assegna la proprieta' a un giocatore.
     * Nota: si limita ad aggiornare il modello, non muove denaro (se ne occupa {@link Bank}).
     *
     * @param owner il nuovo proprietario, non nullo
     */
    public void setOwner(final Player owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Il proprietario non puo' essere null: usare releaseOwner()");
        }
        this.owner = owner;
    }

    /** Riporta la proprieta' alla banca (es. dopo il fallimento del proprietario). */
    public void releaseOwner() {
        this.owner = null;
    }

    /** @return true se la proprieta' non ha ancora un proprietario ed e' quindi acquistabile */
    public boolean isAvailable() {
        return this.owner == null;
    }

    /**
     * @param player il giocatore da controllare
     * @return true se la proprieta' appartiene al giocatore indicato
     */
    public boolean isOwnedBy(final Player player) {
        return this.owner != null && this.owner.equals(player);
    }

    /**
     * Effetto della casella proprieta'.
     * <p>
     * GIORNO 1: implementazione volutamente vuota. Le regole (proporre l'acquisto
     * se la casella e' libera, far pagare l'affitto al proprietario se e' occupata)
     * verranno aggiunte quando ci sara' la gestione dei turni, perche' richiedono
     * un'interazione con il giocatore e con la {@link Bank}.
     *
     * @param player il giocatore che si e' fermato sulla proprieta'
     */
    @Override
    public void onLand(final Player player) {
        // TODO (gestione turni): se isAvailable() -> proporre l'acquisto;
        // altrimenti, se !isOwnedBy(player) -> pagare getRent() al proprietario.
    }

    @Override
    public String toString() {
        return "Property[position=" + this.getPosition()
                + ", name=" + this.getName()
                + ", price=" + this.price
                + ", rent=" + this.rent
                + ", owner=" + this.getOwner().map(Player::getName).orElse("bank")
                + "]";
    }
}
