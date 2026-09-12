package monopoly.model.game;

import monopoly.model.board.Board;
import monopoly.model.board.Tile;
import monopoly.model.player.Player;

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
 * va direttamente in prigione. Chi e' in prigione esce facendo doppio (e in quel caso
 * si muove ma non rilancia), oppure pagando la cauzione con {@link #payBail()}; dopo
 * {@link JailManager#MAX_ATTEMPTS} tentativi falliti la cauzione diventa obbligatoria.
 * <p>
 * Le regole della prigione non sono scritte qui ma nel {@link JailManager}, che e' lo
 * stesso oggetto usato dalla casella "Vai in prigione": il {@code TurnManager} decide
 * <em>quando</em> si va o si esce di prigione, il {@code JailManager} sa <em>cosa</em>
 * comporta. Allo stesso modo, gli effetti delle caselle (affitti, tasse, stipendi)
 * restano nelle caselle: qui si chiama solo {@code onLand}, senza sapere cosa fara'.
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
    private final JailManager jail;

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
        this.jail = state.getContext().getJail();
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
     * Fa pagare al giocatore di turno la cauzione per uscire subito di prigione,
     * prima di lanciare i dadi.
     * <p>
     * Dopo il pagamento il giocatore e' libero e gioca un turno normale: lancia i
     * dadi e, se fa doppio, rilancia. Se non riesce a pagare la cauzione fallisce e
     * il suo turno finisce subito.
     *
     * @return true se la cauzione e' stata pagata e il giocatore e' uscito di prigione
     * @throws IllegalStateException se la partita e' finita, se non e' il momento di
     *                               lanciare o se il giocatore di turno non e' in prigione
     */
    public boolean payBail() {
        this.requirePhase(GamePhase.ROLL);
        final Player player = this.state.getCurrentPlayer();
        if (!player.isInJail()) {
            throw new IllegalStateException("Il giocatore di turno non e' in prigione");
        }
        if (this.jail.payBailAndRelease(player)) {
            return true;
        }
        // Non aveva abbastanza denaro: e' fallito, quindi non puo' lanciare.
        this.state.setPhase(GamePhase.END_TURN);
        return false;
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
        // lo ha mandato in prigione o lo ha fatto fallire, il turno finisce qui.
        if (dice.isDouble() && player.isPlaying()) {
            this.state.setPhase(GamePhase.ROLL);
            return this.createResult(player, dice, from, destination, RollOutcome.ROLL_AGAIN);
        }
        this.state.setPhase(GamePhase.END_TURN);
        return this.createResult(player, dice, from, destination, RollOutcome.MOVED);
    }

    /**
     * Lancio di un giocatore in prigione: esce con un doppio, che pero' non fa
     * rilanciare; altrimenti resta dentro e consuma un tentativo. Esauriti i
     * tentativi paga la cauzione ed esce, restando pero' fermo per questo turno.
     */
    private RollResult rollFromJail(final Player player, final Dice dice) {
        final int from = player.getPosition();
        if (dice.isDouble()) {
            this.jail.releaseWithDouble(player);
            final int destination = this.moveAndLand(player, dice.getTotal());
            this.state.setPhase(GamePhase.END_TURN);
            return this.createResult(player, dice, from, destination, RollOutcome.RELEASED_FROM_JAIL);
        }

        this.state.setPhase(GamePhase.END_TURN);
        this.jail.registerFailedAttempt(player);
        if (this.jail.hasUsedAllAttempts(player)) {
            // Ultimo tentativo fallito: la cauzione diventa obbligatoria. Chi non riesce a
            // pagarla fallisce ed esce dalla partita; in entrambi i casi il turno finisce qui
            // e lo stato del giocatore (PLAYING o BANKRUPT) dice com'e' andata.
            this.jail.payBailAndRelease(player);
            return this.createResult(player, dice, from, from, RollOutcome.RELEASED_ON_BAIL);
        }
        return this.createResult(player, dice, from, from, RollOutcome.STAYED_IN_JAIL);
    }

    /**
     * Fa avanzare il giocatore lungo il tabellone, avvisa le caselle attraversate e
     * applica l'effetto della casella su cui si ferma.
     *
     * @param player il giocatore da muovere
     * @param steps  numero di caselle da percorrere
     * @return la posizione della casella di arrivo
     */
    private int moveAndLand(final Player player, final int steps) {
        final Board board = this.state.getBoard();
        final int from = player.getPosition();

        this.state.setPhase(GamePhase.MOVE);
        // Il tabellone e' circolare: il modulo riporta all'inizio chi supera l'ultima casella.
        final int destination = (from + steps) % board.getSize();
        // Caselle attraversate (escluse partenza e arrivo): ognuna decide da se' se il
        // passaggio comporta qualcosa. Di fatto reagisce solo il "Via", che paga lo
        // stipendio a chi completa il giro; per tutte le altre onPass non fa nulla.
        for (int step = 1; step < steps; step++) {
            board.getTileAt((from + step) % board.getSize()).onPass(player);
        }
        player.setPosition(destination);

        this.state.setPhase(GamePhase.ACTION);
        // Polimorfismo: non serve sapere di che casella si tratti, ogni sottoclasse di
        // Tile sa da se' cosa fare (comprare, far pagare l'affitto, mandare in prigione...).
        final Tile landedTile = board.getTileAt(destination);
        landedTile.onLand(player);
        return destination;
    }

    /**
     * Manda il giocatore in prigione applicando la stessa regola della casella
     * "Vai in prigione": l'unica differenza e' il motivo per cui ci finisce.
     */
    private void sendToJail(final Player player) {
        this.jail.sendToJail(player);
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
