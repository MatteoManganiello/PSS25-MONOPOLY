package monopoly.controller;

import monopoly.model.board.Tile;
import monopoly.model.game.GameEventListener;
import monopoly.model.game.GameState;
import monopoly.model.game.RollResult;
import monopoly.model.player.Player;

/**
 * Osservatore degli eventi di una partita (ruolo "Observer" dell'omonimo pattern).
 * <p>
 * Chi vuole essere informato di cio' che accade in partita (la view testuale di
 * oggi, la GUI del Giorno 4, un eventuale log) implementa questa interfaccia e si
 * registra con {@link GameEngine#addObserver(GameObserver)}. Il {@link GameEngine}
 * conosce solo questa interfaccia e ne invoca i metodi in modo polimorfico, senza
 * sapere quale classe concreta ci sia dall'altra parte.
 * <p>
 * Tutti i metodi hanno un'implementazione vuota di default, cosi' ogni osservatore
 * ridefinisce solo gli eventi che gli interessano: per esempio il pannello dei dadi
 * solo {@link #onDiceRolled}, il tabellone solo {@link #onPlayerMoved}, mentre una
 * view semplice puo' limitarsi a {@link #onGameStateChanged} e ridisegnare tutto.
 * <p>
 * Estende {@link GameEventListener}, che dichiara gli eventi prodotti dal model
 * (acquisti, affitti, tasse, prigione, fallimenti): qui si aggiungono gli eventi che
 * riguardano lo svolgimento della partita (avvio, turni, dadi, movimento, fine).
 * La divisione segue i livelli dell'MVC - il model non conosce il controller e puo'
 * quindi dichiarare solo la prima meta' - ma chi implementa {@code GameObserver} li
 * riceve tutti, registrandosi una volta sola con {@link GameEngine#addObserver}.
 * <p>
 * Gli oggetti del model ricevuti come parametro vanno usati in sola lettura: ogni
 * modifica alla partita passa dai comandi del {@link GameEngine}.
 */
public interface GameObserver extends GameEventListener {

    /**
     * La partita e' iniziata (o, in futuro, e' stata ricaricata).
     *
     * @param state lo stato iniziale da mostrare
     */
    default void onGameStarted(final GameState state) {
    }

    /**
     * Inizia il turno di un giocatore.
     *
     * @param player il nuovo giocatore di turno
     */
    default void onTurnStarted(final Player player) {
    }

    /**
     * Il giocatore di turno ha lanciato i dadi.
     *
     * @param result valori dei dadi ed esito del lancio (doppio, prigione, ...)
     */
    default void onDiceRolled(final RollResult result) {
    }

    /**
     * Un giocatore si e' mosso lungo il tabellone.
     *
     * @param player il giocatore che si e' mosso
     * @param from   la casella di partenza
     * @param to     la casella di arrivo
     */
    default void onPlayerMoved(final Player player, final Tile from, final Tile to) {
    }

    /**
     * Evento generico, inviato dopo ogni comando che modifica la partita.
     * Una view che preferisce ridisegnarsi da zero puo' ascoltare solo questo.
     *
     * @param state lo stato aggiornato della partita
     */
    default void onGameStateChanged(final GameState state) {
    }

    /**
     * La partita e' finita: e' rimasto un solo giocatore non fallito.
     *
     * @param winner il vincitore
     */
    default void onGameOver(final Player winner) {
    }
}
