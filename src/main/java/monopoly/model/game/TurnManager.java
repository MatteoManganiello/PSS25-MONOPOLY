package monopoly.model.game;

import monopoly.model.board.Board;
import monopoly.model.board.Tile;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;

/**
 * Applica le regole di svolgimento di un turno allo stato della partita.
 * <p>
 * Il turno e' una piccola macchina a stati guidata da {@link GamePhase}:
 * <ol>
 *   <li>{@link GamePhase#ROLL}: il giocatore di turno lancia i dadi ({@link #rollDice()});</li>
 *   <li>{@link GamePhase#MOVE}: la pedina avanza sul tabellone, in modo circolare;</li>
 *   <li>{@link GamePhase#ACTION}: si applica l'effetto della casella di arrivo;</li>
 *   <li>si torna a {@code ROLL} se il giocatore ha fatto doppio, altrimenti si passa a
 *       {@link GamePhase#END_TURN}, da cui {@link #endTurn()} passa la mano.</li>
 * </ol>
 * Regole dei doppi: chi fa doppio lancia di nuovo, ma al terzo doppio consecutivo
 * va direttamente in prigione. Chi e' in prigione esce solo facendo doppio, e in
 * quel caso si muove ma non rilancia.
 * <p>
 * La classe non ha uno stato proprio: fase, doppi consecutivi e giocatore di turno
 * stanno tutti nel {@link GameState}, che resta cosi' l'unica descrizione completa
 * della partita (facile da mostrare, testare e, in futuro, salvare).
 * <p>
 * Sta nel model, e non nel controller, perche' contiene regole del gioco: valgono
 * allo stesso modo qualunque sia l'interfaccia che comanda la partita.
 */
public class TurnManager {

    /** Numero di doppi consecutivi che manda il giocatore in prigione. */
    public static final int MAX_CONSECUTIVE_DOUBLES = 3;

    private final GameState state;

    /**
     * Crea il gestore dei turni per una partita.
     *
     * @param state lo stato della partita su cui applicare le regole
     * @throws IllegalArgumentException se lo stato e' null
     */
    public TurnManager(final GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("Lo stato della partita non puo' essere null");
        }
        this.state = state;
    }

    /**
     * Esegue un lancio di dadi del giocatore di turno e ne applica le conseguenze:
     * movimento, effetto della casella di arrivo e regola dei doppi.
     *
     * @return il risultato del lancio
     * @throws IllegalStateException se la partita e' finita o se non e' il momento di lanciare
     */
    public RollResult rollDice() {
        this.requirePhase(GamePhase.ROLL);
        final Player player = this.state.getCurrentPlayer();
        final Dice dice = this.state.getDice();
        dice.roll();
        // Le regole del lancio cambiano a seconda che il giocatore sia libero o in prigione.
        return player.isInJail() ? this.rollFromJail(player, dice) : this.rollAndMove(player, dice);
    }

    /**
     * Chiude il turno corrente e passa la mano al giocatore successivo, saltando i falliti.
     *
     * @return il nuovo giocatore di turno
     * @throws IllegalStateException se la partita e' finita o se il turno non e' ancora concluso
     */
    public Player endTurn() {
        this.requirePhase(GamePhase.END_TURN);
        return this.state.advanceToNextPlayer();
    }

    /** Lancio di un giocatore libero: regola dei doppi, movimento ed effetto della casella. */
    private RollResult rollAndMove(final Player player, final Dice dice) {
        final int from = player.getPosition();
        if (dice.isDouble()
                && this.state.incrementConsecutiveDoubles() >= MAX_CONSECUTIVE_DOUBLES) {
            // Terzo doppio di fila: si va in prigione senza muoversi con questo lancio.
            this.sendToJail(player);
            this.state.setPhase(GamePhase.END_TURN);
            return this.createResult(player, dice, from, Board.JAIL_POSITION, RollOutcome.SENT_TO_JAIL);
        }

        final int destination = this.moveAndLand(player, dice.getTotal());

        // Il doppio fa rilanciare solo se il giocatore e' ancora libero: se la casella
        // lo ha mandato in prigione (o, dal Giorno 3, lo ha fatto fallire) il turno finisce.
        if (dice.isDouble() && player.isPlaying()) {
            this.state.setPhase(GamePhase.ROLL);
            return this.createResult(player, dice, from, destination, RollOutcome.ROLL_AGAIN);
        }
        this.state.setPhase(GamePhase.END_TURN);
        return this.createResult(player, dice, from, destination, RollOutcome.MOVED);
    }

    /** Lancio di un giocatore in prigione: esce solo con un doppio, che pero' non fa rilanciare. */
    private RollResult rollFromJail(final Player player, final Dice dice) {
        final int from = player.getPosition();
        if (!dice.isDouble()) {
            this.state.setPhase(GamePhase.END_TURN);
            return this.createResult(player, dice, from, from, RollOutcome.STAYED_IN_JAIL);
        }
        player.setStatus(PlayerStatus.PLAYING);
        final int destination = this.moveAndLand(player, dice.getTotal());
        this.state.setPhase(GamePhase.END_TURN);
        return this.createResult(player, dice, from, destination, RollOutcome.RELEASED_FROM_JAIL);
    }

    /**
     * Fa avanzare il giocatore e applica l'effetto della casella su cui si ferma.
     *
     * @param player il giocatore da muovere
     * @param steps  numero di caselle da percorrere
     * @return la posizione della casella di arrivo
     */
    private int moveAndLand(final Player player, final int steps) {
        final Board board = this.state.getBoard();

        this.state.setPhase(GamePhase.MOVE);
        // Il tabellone e' circolare: il modulo riporta all'inizio chi supera l'ultima casella.
        final int destination = (player.getPosition() + steps) % board.getSize();
        player.setPosition(destination);

        this.state.setPhase(GamePhase.ACTION);
        // Polimorfismo: non serve sapere di che casella si tratti, ogni sottoclasse di
        // Tile sa da se' cosa fare. Le caselle vere arriveranno al Giorno 3.
        final Tile landedTile = board.getTileAt(destination);
        landedTile.onLand(player);
        return destination;
    }

    /** Sposta il giocatore sulla casella della prigione e ne aggiorna lo stato. */
    private void sendToJail(final Player player) {
        player.setPosition(Board.JAIL_POSITION);
        player.setStatus(PlayerStatus.IN_JAIL);
    }

    /** Crea il risultato del lancio copiando i valori attuali dei dadi. */
    private RollResult createResult(final Player player, final Dice dice,
                                    final int from, final int to, final RollOutcome outcome) {
        return new RollResult(player, dice.getFirstValue(), dice.getSecondValue(), from, to, outcome);
    }

    /**
     * Verifica che l'azione richiesta sia consentita in questo momento.
     *
     * @param expected la fase in cui l'azione e' ammessa
     * @throws IllegalStateException se la partita e' finita o la fase non corrisponde
     */
    private void requirePhase(final GamePhase expected) {
        if (this.state.isGameOver()) {
            throw new IllegalStateException("La partita e' finita");
        }
        if (this.state.getPhase() != expected) {
            throw new IllegalStateException("Azione non consentita nella fase "
                    + this.state.getPhase() + " (richiesta la fase " + expected + ")");
        }
    }
}
