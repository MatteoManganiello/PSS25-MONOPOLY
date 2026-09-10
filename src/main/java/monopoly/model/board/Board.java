package monopoly.model.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabellone di gioco: sequenza circolare delle 40 caselle.
 * <p>
 * Il tabellone conosce solo il tipo astratto {@link Tile}: non sa (e non deve
 * sapere) se una casella e' una proprieta', una tassa o un imprevisto. Questo
 * permette di sostituire o aggiungere tipi di casella senza modificare
 * una sola riga di questa classe.
 * <p>
 * GIORNO 1: le caselle sono tutte segnaposto ({@link PlaceholderTile}); verranno
 * sostituite dalle caselle definitive nei prossimi giorni.
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

    /** Crea un tabellone standard riempito con caselle segnaposto. */
    public Board() {
        this(createPlaceholderTiles());
    }

    /**
     * Crea un tabellone a partire da un elenco di caselle gia' pronto.
     * Utile per i test e, in futuro, per caricare la configurazione da file.
     *
     * @param tiles le {@link Board#SIZE} caselle del tabellone, in ordine di posizione
     * @throws IllegalArgumentException se le caselle non sono esattamente {@link Board#SIZE}
     */
    public Board(final List<Tile> tiles) {
        if (tiles == null || tiles.size() != SIZE) {
            throw new IllegalArgumentException("Il tabellone deve avere esattamente " + SIZE + " caselle");
        }
        // Copia difensiva: la lista interna non puo' essere modificata da fuori.
        this.tiles = new ArrayList<>(tiles);
    }

    /**
     * Restituisce la casella in una data posizione.
     *
     * @param position indice della casella, tra 0 e {@link Board#SIZE} - 1
     * @return la casella corrispondente
     * @throws IllegalArgumentException se la posizione non appartiene al tabellone
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

    /** @return tutte le caselle in sola lettura */
    public List<Tile> getTiles() {
        return Collections.unmodifiableList(this.tiles);
    }

    /**
     * Costruisce le caselle segnaposto del tabellone: i quattro angoli ricevono
     * gia' il nome definitivo, le altre un nome provvisorio.
     *
     * @return la lista delle 40 caselle segnaposto
     */
    private static List<Tile> createPlaceholderTiles() {
        final List<Tile> placeholders = new ArrayList<>(SIZE);
        for (int position = 0; position < SIZE; position++) {
            placeholders.add(new PlaceholderTile(placeholderName(position), position));
        }
        return placeholders;
    }

    /** @return il nome provvisorio della casella nella posizione indicata */
    private static String placeholderName(final int position) {
        return switch (position) {
            case START_POSITION -> "GO";
            case JAIL_POSITION -> "Jail";
            case FREE_PARKING_POSITION -> "Free Parking";
            case GO_TO_JAIL_POSITION -> "Go To Jail";
            default -> "Tile " + position;
        };
    }

    @Override
    public String toString() {
        return "Board[size=" + SIZE + ", tiles=" + this.tiles + "]";
    }
}
