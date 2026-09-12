package monopoly.model.board;

import java.util.ArrayList;
import java.util.List;

import monopoly.model.economy.EconomyManager;
import monopoly.model.game.GameContext;
import monopoly.model.game.JailManager;

/**
 * Costruisce il tabellone standard da {@link Board#SIZE} caselle.
 * <p>
 * E' una classe a parte, e non un metodo di {@link Board}, per una questione di
 * responsabilita': il tabellone deve sapere <em>come si usa</em> una sequenza di
 * caselle (trovarne una, scorrerle), non <em>quali</em> caselle contiene una
 * partita di Monopoly. Tenendo qui i dati, il giorno in cui il tabellone verra'
 * caricato da un file bastera' aggiungere una seconda fabbrica senza toccare
 * {@code Board} ne' le caselle.
 * <p>
 * I nomi delle caselle sono quelli dell'edizione italiana del gioco; gli identificatori
 * del codice restano invece in inglese, come nel resto del progetto.
 * <p>
 * Le caselle "Imprevisti" e "Probabilita'" sono ancora {@link PlaceholderTile}:
 * il mazzo di carte non fa parte del Giorno 3, e una casella segnaposto senza effetto
 * permette di avere gia' il tabellone completo e giocabile.
 */
public final class BoardFactory {

    /** Prezzo e affitto uguali per tutte e quattro le stazioni. */
    private static final int STATION_PRICE = 200;
    private static final int STATION_RENT = 25;

    /** Prezzo e affitto uguali per le due societa'. */
    private static final int UTILITY_PRICE = 150;
    private static final int UTILITY_RENT = 20;

    /** Classe di utilita': non deve essere istanziata. */
    private BoardFactory() {
    }

    /**
     * Crea il tabellone standard, con le caselle gia' collegate ai servizi della partita.
     *
     * @param context i servizi condivisi (banca, economia, prigione, eventi)
     * @return un tabellone di {@link Board#SIZE} caselle
     * @throws IllegalArgumentException se il contesto e' null
     */
    public static Board createStandardBoard(final GameContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Il contesto della partita non puo' essere null");
        }
        return new Board(createStandardTiles(context));
    }

    /**
     * Crea le {@link Board#SIZE} caselle standard, in ordine di posizione.
     *
     * @param context i servizi condivisi della partita
     * @return l'elenco ordinato delle caselle
     */
    public static List<Tile> createStandardTiles(final GameContext context) {
        final EconomyManager economy = context.getEconomy();
        final JailManager jail = context.getJail();
        final List<Tile> tiles = new ArrayList<>(Board.SIZE);

        // --- Primo lato: dal Via alla prigione ---
        tiles.add(new StartTile("Via", Board.START_POSITION, economy));
        tiles.add(street("Vicolo Corto", 1, 60, 2, economy));
        tiles.add(card("Probabilita'", 2));
        tiles.add(street("Vicolo Stretto", 3, 60, 4, economy));
        tiles.add(new TaxTile("Tassa patrimoniale", 4, 200, economy));
        tiles.add(station("Stazione Sud", 5, economy));
        tiles.add(street("Bastioni Gran Sasso", 6, 100, 6, economy));
        tiles.add(card("Imprevisti", 7));
        tiles.add(street("Viale Monterosa", 8, 100, 6, economy));
        tiles.add(street("Viale Vesuvio", 9, 120, 8, economy));

        // --- Secondo lato: dalla prigione al posteggio ---
        tiles.add(new JailTile("Prigione", Board.JAIL_POSITION));
        tiles.add(street("Via Accademia", 11, 140, 10, economy));
        tiles.add(utility("Societa' Elettrica", 12, economy));
        tiles.add(street("Corso Ateneo", 13, 140, 10, economy));
        tiles.add(street("Piazza Universita'", 14, 160, 12, economy));
        tiles.add(station("Stazione Ovest", 15, economy));
        tiles.add(street("Via Verdi", 16, 180, 14, economy));
        tiles.add(card("Probabilita'", 17));
        tiles.add(street("Corso Raffaello", 18, 180, 14, economy));
        tiles.add(street("Piazza Dante", 19, 200, 16, economy));

        // --- Terzo lato: dal posteggio al "Vai in prigione" ---
        tiles.add(new FreeParkingTile("Posteggio gratuito", Board.FREE_PARKING_POSITION, economy));
        tiles.add(street("Via Marco Polo", 21, 220, 18, economy));
        tiles.add(card("Imprevisti", 22));
        tiles.add(street("Corso Magellano", 23, 220, 18, economy));
        tiles.add(street("Largo Colombo", 24, 240, 20, economy));
        tiles.add(station("Stazione Nord", 25, economy));
        tiles.add(street("Viale Costantino", 26, 260, 22, economy));
        tiles.add(street("Viale Traiano", 27, 260, 22, economy));
        tiles.add(utility("Societa' Acqua Potabile", 28, economy));
        tiles.add(street("Piazza Giulio Cesare", 29, 280, 24, economy));

        // --- Quarto lato: dal "Vai in prigione" al Via ---
        tiles.add(new GoToJailTile("Vai in prigione", Board.GO_TO_JAIL_POSITION, jail));
        tiles.add(street("Via Roma", 31, 300, 26, economy));
        tiles.add(street("Corso Impero", 32, 300, 26, economy));
        tiles.add(card("Probabilita'", 33));
        tiles.add(street("Largo Augusto", 34, 320, 28, economy));
        tiles.add(station("Stazione Est", 35, economy));
        tiles.add(card("Imprevisti", 36));
        tiles.add(street("Viale dei Giardini", 37, 350, 35, economy));
        tiles.add(new TaxTile("Tassa di lusso", 38, 100, economy));
        tiles.add(street("Parco della Vittoria", 39, 400, 50, economy));

        return tiles;
    }

    /** Terreno acquistabile con prezzo e affitto propri. */
    private static PropertyTile street(final String name, final int position, final int price,
                                       final int rent, final EconomyManager economy) {
        return new PropertyTile(name, position, price, rent, economy);
    }

    /** Stazione: una proprieta' come le altre, con prezzo e affitto uguali per tutte. */
    private static PropertyTile station(final String name, final int position, final EconomyManager economy) {
        return new PropertyTile(name, position, STATION_PRICE, STATION_RENT, economy);
    }

    /** Societa': anch'essa una proprieta', con valori propri. */
    private static PropertyTile utility(final String name, final int position, final EconomyManager economy) {
        return new PropertyTile(name, position, UTILITY_PRICE, UTILITY_RENT, economy);
    }

    /** Casella "Imprevisti" o "Probabilita'": segnaposto in attesa del mazzo di carte. */
    private static PlaceholderTile card(final String name, final int position) {
        return new PlaceholderTile(name, position);
    }
}
