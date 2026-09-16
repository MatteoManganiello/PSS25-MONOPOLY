package it.unibo.monopoly.model.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Il tabellone: le 40 caselle in cerchio.
 * <p>
 * Qui dentro si vede solo il tipo {@link Tile}, mai le caselle vere. Cosi' si
 * possono aggiungere nuovi tipi di casella senza toccare questa classe.
 * <p>
 * Le caselle arrivano gia' pronte dal costruttore: di solito le crea
 * {@link BoardFactory#createStandardBoard}, mentre il costruttore pubblico serve
 * ai test e al caricamento da file.
 */
public class Board {

    /** Numero di caselle del tabellone. */
    public static final int SIZE = 40;

    /** Posizione della casella di partenza ("Via"). */
    public static final int START_POSITION = 0;

    /** Posizione della prigione. */
    public static final int JAIL_POSITION = 10;

    /** Posizione del posteggio gratuito. */
    public static final int FREE_PARKING_POSITION = 20;

    /** Posizione della casella "Vai in prigione". */
    public static final int GO_TO_JAIL_POSITION = 30;

    private final List<Tile> tiles;

    /**
     * Crea un tabellone da una lista di caselle gia' pronta.
     * Per la partita vera si usa {@link BoardFactory#createStandardBoard}.
     *
     * @param tiles le {@link Board#SIZE} caselle, in ordine di posizione
     * @throws IllegalArgumentException se le caselle non sono esattamente {@link Board#SIZE}
     */
    public Board(final List<Tile> tiles) {
        if (tiles == null || tiles.size() != SIZE) {
            throw new IllegalArgumentException("Il tabellone deve avere esattamente " + SIZE + " caselle");
        }
        // Copiamo la lista, cosi' nessuno da fuori puo' cambiarla.
        this.tiles = new ArrayList<>(tiles);
    }

    /**
     * Da' la casella che si trova in una certa posizione.
     *
     * @param position la posizione, tra 0 e {@link Board#SIZE} - 1
     * @return la casella che sta li'
     * @throws IllegalArgumentException se la posizione non esiste sul tabellone
     */
    public Tile getTileAt(final int position) {
        if (position < 0 || position >= SIZE) {
            throw new IllegalArgumentException("Posizione non valida sul tabellone: " + position);
        }
        return this.tiles.get(position);
    }

    /** @return il numero di caselle del tabellone */
    public int getSize() {
        return SIZE;
    }

    /** @return tutte le caselle, in sola lettura */
    public List<Tile> getTiles() {
        return Collections.unmodifiableList(this.tiles);
    }

    @Override
    public String toString() {
        return "Board[size=" + SIZE + ", tiles=" + this.tiles + "]";
    }
}
