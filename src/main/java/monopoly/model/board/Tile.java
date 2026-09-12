package monopoly.model.board;

import monopoly.model.player.Player;

/**
 * Casella generica del tabellone: classe base astratta di tutte le caselle.
 * <p>
 * Ogni casella ha due caratteristiche comuni (nome e posizione) che sono
 * implementate qui una volta sola, e un comportamento che invece cambia da
 * casella a casella: cosa succede quando un giocatore ci si ferma sopra.
 * Quel comportamento e' dichiarato dal metodo astratto {@link #onLand(Player)},
 * che ogni sottoclasse concreta e' obbligata a definire; {@link #onPass(Player)}
 * descrive invece il (raro) effetto del semplice passaggio sulla casella.
 * <p>
 * La classe e' astratta proprio perche' una "casella generica" non esiste sul
 * tabellone reale: esistono solo caselle specifiche (proprieta', tasse, imprevisti,
 * prigione, ...). Rendendola astratta si impedisce di istanziarla per errore e si
 * garantisce che ogni casella sappia rispondere a {@code onLand}.
 */
public abstract class Tile {

    private final String name;
    private final int position;

    /**
     * Costruttore riservato alle sottoclassi (protected: nessuno puo' creare
     * direttamente una Tile "senza tipo").
     *
     * @param name     nome della casella, mostrato dalla view
     * @param position indice della casella sul tabellone, da 0 a {@link Board#SIZE} - 1
     * @throws IllegalArgumentException se il nome e' vuoto o la posizione e' negativa
     */
    protected Tile(final String name, final int position) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Il nome della casella non puo' essere vuoto");
        }
        if (position < 0) {
            throw new IllegalArgumentException("La posizione della casella non puo' essere negativa");
        }
        this.name = name;
        this.position = position;
    }

    /**
     * Effetto della casella sul giocatore che vi si ferma sopra.
     * <p>
     * E' il punto di estensione polimorfico del modello: il controller chiamera'
     * sempre e solo {@code tile.onLand(player)} senza sapere di che casella si tratti,
     * e sara' la sottoclasse concreta a decidere cosa fare (far pagare l'affitto,
     * proporre l'acquisto, mandare in prigione, pescare una carta, ...).
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    public abstract void onLand(Player player);

    /**
     * Effetto della casella sul giocatore che la <em>attraversa</em> senza fermarsi.
     * <p>
     * A differenza di {@link #onLand(Player)} non e' astratto ma ha gia' un corpo,
     * vuoto: quasi nessuna casella fa qualcosa quando ci si passa sopra, quindi
     * obbligare tutte le sottoclassi a ridefinirlo sarebbe solo un fastidio. Chi ha
     * bisogno di reagire al passaggio lo ridefinisce: e' il caso di
     * {@link StartTile}, che accredita lo stipendio anche a chi passa dal "Via".
     * <p>
     * E' il {@link monopoly.model.game.TurnManager TurnManager} a chiamarlo, una volta per ogni casella attraversata
     * durante il movimento.
     *
     * @param player il giocatore che sta attraversando la casella
     */
    public void onPass(final Player player) {
        // Nessun effetto: passare su una casella, di norma, non comporta nulla.
    }

    /** @return il nome della casella */
    public String getName() {
        return this.name;
    }

    /** @return la posizione della casella sul tabellone */
    public int getPosition() {
        return this.position;
    }

    @Override
    public String toString() {
        // getClass().getSimpleName() mostra il tipo concreto della sottoclasse
        return this.getClass().getSimpleName() + "[position=" + this.position + ", name=" + this.name + "]";
    }
}
