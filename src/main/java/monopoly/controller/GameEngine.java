package monopoly.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import monopoly.model.board.Board;
import monopoly.model.board.Tile;
import monopoly.model.game.Dice;
import monopoly.model.game.GamePhase;
import monopoly.model.game.GameState;
import monopoly.model.game.RollOutcome;
import monopoly.model.game.RollResult;
import monopoly.model.game.TurnManager;
import monopoly.model.player.Player;

/**
 * Motore della partita: coordinatore di alto livello e unico punto di ingresso
 * per chi comanda il gioco (oggi la demo testuale, dal Giorno 4 la GUI).
 * <p>
 * Nell'architettura MVC e' il controller: riceve i comandi ({@link #startGame()},
 * {@link #rollDice()}, {@link #endTurn()}), li fa eseguire al model (il
 * {@link TurnManager} applica le regole sul {@link GameState}) e poi informa le
 * view di cosa e' cambiato. Non contiene regole di gioco: si occupa solo del ciclo
 * di vita della partita (avvio e fine) e della notifica degli eventi.
 * <p>
 * E' anche il "subject" del pattern Observer: le view si registrano con
 * {@link #addObserver(GameObserver)} e ricevono gli eventi attraverso l'interfaccia
 * {@link GameObserver}, senza che il motore conosca le loro classi concrete.
 * <p>
 * Uso tipico:
 * <pre>
 *   GameEngine engine = new GameEngine(players);
 *   engine.addObserver(view);
 *   engine.startGame();
 *   engine.rollDice();   // di nuovo finche' canRollDice() e' true (doppi)
 *   engine.endTurn();
 * </pre>
 */
public class GameEngine {

    private final GameState state;
    private final TurnManager turnManager;
    private final List<GameObserver> observers;
    private boolean started;

    /**
     * Prepara una nuova partita con il tabellone standard e dadi casuali.
     *
     * @param players i giocatori nell'ordine di turno
     * @throws IllegalArgumentException se l'elenco dei giocatori non e' valido
     */
    public GameEngine(final List<Player> players) {
        this(new GameState(new Board(), players, new Dice()));
    }

    /**
     * Prepara una partita a partire da uno stato gia' pronto: per esempio con dadi
     * a seme fisso nei test o, in futuro, con una partita ricaricata da file.
     *
     * @param state lo stato della partita da gestire
     * @throws IllegalArgumentException se lo stato e' null
     */
    public GameEngine(final GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("Lo stato della partita non puo' essere null");
        }
        this.state = state;
        this.turnManager = new TurnManager(state);
        this.observers = new ArrayList<>();
        this.started = false;
    }

    // ------------------------------------------------------------------
    // Pattern Observer
    // ------------------------------------------------------------------

    /**
     * Registra un osservatore, che da questo momento ricevera' gli eventi della partita.
     * Registrare due volte lo stesso osservatore non ha effetto.
     *
     * @param observer l'osservatore da registrare
     * @throws IllegalArgumentException se l'osservatore e' null
     */
    public void addObserver(final GameObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("L'osservatore non puo' essere null");
        }
        if (!this.observers.contains(observer)) {
            this.observers.add(observer);
        }
    }

    /**
     * Rimuove un osservatore, che non ricevera' piu' eventi.
     *
     * @param observer l'osservatore da rimuovere
     */
    public void removeObserver(final GameObserver observer) {
        this.observers.remove(observer);
    }

    // ------------------------------------------------------------------
    // Comandi
    // ------------------------------------------------------------------

    /**
     * Avvia la partita e comunica agli osservatori lo stato iniziale e il primo turno.
     * <p>
     * Non viene fatto nel costruttore perche' gli osservatori si registrano dopo la
     * creazione del motore: cosi' ricevono anche l'evento di avvio.
     *
     * @throws IllegalStateException se la partita e' gia' stata avviata
     */
    public void startGame() {
        if (this.started) {
            throw new IllegalStateException("La partita e' gia' iniziata");
        }
        this.started = true;
        final Player firstPlayer = this.state.getCurrentPlayer();
        this.notifyObservers(observer -> observer.onGameStarted(this.state));
        this.notifyObservers(observer -> observer.onTurnStarted(firstPlayer));
    }

    /**
     * Fa lanciare i dadi al giocatore di turno e notifica cosa e' successo.
     * Dopo un doppio il turno resta allo stesso giocatore, che puo' lanciare di nuovo.
     *
     * @throws IllegalStateException se la partita non e' in corso o non e' il momento di lanciare
     */
    public void rollDice() {
        this.requireGameInProgress();
        final RollResult result = this.turnManager.rollDice();
        this.notifyObservers(observer -> observer.onDiceRolled(result));
        this.notifyMovement(result);
        this.notifyObservers(observer -> observer.onGameStateChanged(this.state));
        this.checkGameOver();
    }

    /**
     * Chiude il turno e passa la mano al giocatore successivo, saltando i falliti.
     *
     * @throws IllegalStateException se la partita non e' in corso o il turno non e' concluso
     */
    public void endTurn() {
        this.requireGameInProgress();
        final Player nextPlayer = this.turnManager.endTurn();
        this.notifyObservers(observer -> observer.onTurnStarted(nextPlayer));
        this.notifyObservers(observer -> observer.onGameStateChanged(this.state));
    }

    /**
     * Gioca automaticamente un turno intero: lancia finche' e' consentito (i doppi
     * fanno rilanciare) e poi passa la mano. Utile per la demo testuale e per i test;
     * la GUI usera' invece {@link #rollDice()} ed {@link #endTurn()} in risposta ai pulsanti.
     *
     * @throws IllegalStateException se la partita non e' in corso
     */
    public void playTurn() {
        this.requireGameInProgress();
        while (this.canRollDice()) {
            this.rollDice();
        }
        if (this.canEndTurn()) {
            this.endTurn();
        }
    }

    // ------------------------------------------------------------------
    // Interrogazioni per la view
    // ------------------------------------------------------------------

    /** @return true se ora il giocatore di turno puo' lanciare i dadi (es. per abilitare un pulsante) */
    public boolean canRollDice() {
        return this.isInProgress() && this.state.getPhase() == GamePhase.ROLL;
    }

    /** @return true se ora il giocatore di turno puo' (e deve) passare la mano */
    public boolean canEndTurn() {
        return this.isInProgress() && this.state.getPhase() == GamePhase.END_TURN;
    }

    /** @return true se la partita e' terminata */
    public boolean isGameOver() {
        return this.state.isGameOver();
    }

    /** @return il vincitore, oppure {@link Optional#empty()} se la partita e' ancora in corso */
    public Optional<Player> getWinner() {
        return this.state.isGameOver() ? this.state.getActivePlayers().stream().findFirst() : Optional.empty();
    }

    /** @return lo stato della partita, da usare in sola lettura */
    public GameState getState() {
        return this.state;
    }

    // ------------------------------------------------------------------
    // Metodi di supporto
    // ------------------------------------------------------------------

    /** Traduce l'esito del lancio nell'evento di spostamento corrispondente. */
    private void notifyMovement(final RollResult result) {
        final Player player = result.player();
        if (result.outcome().movesPlayer()) {
            final Board board = this.state.getBoard();
            final Tile from = board.getTileAt(result.fromPosition());
            final Tile to = board.getTileAt(result.toPosition());
            this.notifyObservers(observer -> observer.onPlayerMoved(player, from, to));
        } else if (result.outcome() == RollOutcome.SENT_TO_JAIL) {
            this.notifyObservers(observer -> observer.onPlayerSentToJail(player));
        }
    }

    /**
     * Chiude la partita quando resta un solo giocatore non fallito.
     * <p>
     * Oggi nessuno puo' ancora fallire (affitti e tasse arriveranno con le caselle
     * del Giorno 3), ma il controllo e' gia' al suo posto: bastera' che una casella
     * dichiari fallito un giocatore. I giocatori falliscono uno alla volta, quindi
     * ne resta sempre almeno uno.
     */
    private void checkGameOver() {
        final List<Player> activePlayers = this.state.getActivePlayers();
        if (activePlayers.size() == 1) {
            this.state.setGameOver(true);
            final Player winner = activePlayers.get(0);
            this.notifyObservers(observer -> observer.onGameOver(winner));
        }
    }

    /**
     * Invia un evento a tutti gli osservatori registrati. L'evento e' una lambda che
     * dice quale metodo chiamare su ciascun osservatore.
     * <p>
     * Si itera su una copia della lista, cosi' un osservatore puo' registrarsi o
     * rimuoversi anche mentre riceve una notifica.
     */
    private void notifyObservers(final Consumer<GameObserver> event) {
        List.copyOf(this.observers).forEach(event);
    }

    /** @return true se la partita e' stata avviata e non e' ancora finita */
    private boolean isInProgress() {
        return this.started && !this.state.isGameOver();
    }

    /** @throws IllegalStateException se la partita non e' stata avviata o e' gia' finita */
    private void requireGameInProgress() {
        if (!this.started) {
            throw new IllegalStateException("La partita non e' ancora iniziata: chiamare startGame()");
        }
        if (this.state.isGameOver()) {
            throw new IllegalStateException("La partita e' finita");
        }
    }
}
