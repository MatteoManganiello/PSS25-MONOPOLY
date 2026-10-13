package it.unibo.monopoly.model.board;

/**
 * Famiglia a cui appartiene una casella del tabellone.
 * <p>
 * Nasce da un'esigenza della view: per disegnare il tabellone servono un colore e
 * un aspetto diversi per ogni tipo di casella, ma chiederlo con una catena di
 * {@code instanceof} significherebbe elencare nella GUI tutte le sottoclassi di
 * {@link Tile} e doverla modificare a ogni casella nuova. Con questa categoria e'
 * la casella stessa a dichiarare di che famiglia e' ({@link Tile#getCategory()}) e
 * alla view basta una tabella "categoria - colore".
 * <p>
 * E' un'informazione puramente descrittiva: nessuna regola del gioco dipende da
 * questo valore, gli effetti restano nel metodo polimorfico
 * {@link Tile#onLand(it.unibo.monopoly.model.player.Player) onLand}.
 */
public enum TileCategory {

    /** Casella di partenza, il "Via". */
    START,

    /** Casella acquistabile: terreno, stazione o societa'. */
    PROPERTY,

    /** Casella che impone un pagamento alla banca. */
    TAX,

    /** Casella della prigione (comprende la semplice visita). */
    JAIL,

    /** Casella che manda in prigione. */
    GO_TO_JAIL,

    /** Posteggio gratuito. */
    FREE_PARKING,

    /** Casella "Imprevisti" o "Probabilita'", che fa pescare una carta. */
    CARD,

    /** Categoria di ripiego per le caselle che non ne dichiarano una. */
    OTHER
}
