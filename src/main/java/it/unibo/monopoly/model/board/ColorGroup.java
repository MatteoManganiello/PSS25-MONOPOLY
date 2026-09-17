package it.unibo.monopoly.model.board;

/**
 * Gruppo di colore a cui appartiene un terreno del tabellone.
 * <p>
 * Serve a una regola sola, ma importante: chi possiede <em>tutti</em> i terreni di
 * un gruppo ha il monopolio su quel colore e incassa un affitto doppio. Il gruppo e'
 * un dato fisso del tabellone (lo assegna {@link BoardFactory} alla creazione della
 * casella e non cambia mai), quindi non entra nel salvataggio della partita: per
 * ricostruire la situazione basta sapere di chi e' ogni casella.
 * <p>
 * E' un enum e non una semplice stringa perche' i gruppi sono pochi e noti in
 * anticipo: cosi' il confronto e' sicuro e non si possono scrivere colori inesistenti.
 * <p>
 * Non va confuso con {@link TileCategory}, che dice di che <em>famiglia</em> e' una
 * casella (proprieta', tassa, prigione, ...): il colore riguarda solo i terreni e
 * serve al calcolo dell'affitto, non alla view.
 */
public enum ColorGroup {

    /** Marrone: Vicolo Corto, Vicolo Stretto. */
    BROWN,

    /** Azzurro: Bastioni Gran Sasso, Viale Monterosa, Viale Vesuvio. */
    LIGHT_BLUE,

    /** Rosa: Via Accademia, Corso Ateneo, Piazza Universita'. */
    PINK,

    /** Arancione: Via Verdi, Corso Raffaello, Piazza Dante. */
    ORANGE,

    /** Rosso: Via Marco Polo, Corso Magellano, Largo Colombo. */
    RED,

    /** Giallo: Viale Costantino, Viale Traiano, Piazza Giulio Cesare. */
    YELLOW,

    /** Verde: Via Roma, Corso Impero, Largo Augusto. */
    GREEN,

    /** Blu: Viale dei Giardini, Parco della Vittoria. */
    BLUE
}
