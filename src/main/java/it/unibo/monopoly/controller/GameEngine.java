package it.unibo.monopoly.controller;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import it.unibo.monopoly.model.board.Board;
import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameEventSupport;
import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.RollResult;
import it.unibo.monopoly.model.game.TurnManager;
import it.unibo.monopoly.model.persistence.GameStateLoader;
import it.unibo.monopoly.model.persistence.GameStateSaver;
import it.unibo.monopoly.model.persistence.PersistenceResult;
import it.unibo.monopoly.model.persistence.SaveFileException;
import it.unibo.monopoly.model.player.Player;

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
 * Gli eventi arrivano da due sorgenti. Quelli sullo svolgimento della partita (avvio,
 * dadi, movimento, turni) li produce direttamente il motore; quelli sugli effetti
 * (acquisti, affitti, tasse, prigione, fallimenti) li produce il model mentre applica
 * le regole, e il motore si limita a pubblicarli con
 * {@link GameEventSupport#publishPending()} subito dopo il proprio evento, cosi' i
 * messaggi arrivano alla view nell'ordine in cui sono accaduti.
 * <p>
 * <b>Salvataggio e caricamento.</b> Anche la persistenza passa da qui
 * ({@link #saveGame(Path)}, {@link #loadGame(Path)}), cosi' la view non deve conoscere
 * come e' fatto un file di salvataggio. Il lavoro sui file lo fanno le classi di
 * {@code model.persistence}; il motore decide quando una partita caricata prende il
 * posto di quella in corso e lo annuncia agli osservatori. Per questo stato, turni ed
 * eventi del model non sono {@code final}: sono l'unica parte del motore che cambia
 * con un caricamento, mentre osservatori e dadi restano gli stessi.
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

    private GameState state;
    private TurnManager turnManager;
    private GameEventSupport modelEvents;
    private final List<GameObserver> observers;

    /**
     * I dadi del tavolo. Non fanno parte di un salvataggio, quindi una partita caricata
     * continua con questi: nei test, con dadi a seme fisso, anche il seguito di una
     * partita caricata e' prevedibile.
     */
    private final Dice dice;

    private final GameStateSaver saver;
    private final GameStateLoader loader;
    private boolean started;

    /**
     * Prepara una nuova partita con il tabellone standard e dadi casuali.
     *
     * @param players i giocatori nell'ordine di turno
     * @throws IllegalArgumentException se l'elenco dei giocatori non e' valido
     */
    public GameEngine(final List<Player> players) {
        this(GameState.createStandardGame(players, new Dice()));
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
        this.modelEvents = state.getContext().getEvents();
        this.observers = new ArrayList<>();
        this.dice = state.getDice();
        this.saver = new GameStateSaver();
        this.loader = new GameStateLoader();
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
            // Un GameObserver e' anche un GameEventListener: con una sola registrazione
            // riceve sia gli eventi del motore sia quelli prodotti dal model.
            this.modelEvents.addListener(observer);
        }
    }

    /**
     * Rimuove un osservatore, che non ricevera' piu' eventi.
     *
     * @param observer l'osservatore da rimuovere
     */
    public void removeObserver(final GameObserver observer) {
        this.observers.remove(observer);
        this.modelEvents.removeListener(observer);
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
        // Ogni comando lascia la coda del model vuota: eventuali eventi prodotti durante
        // la preparazione della partita vengono raccontati subito, non al primo lancio.
        this.modelEvents.publishPending();
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
        // Effetti della casella (acquisto, affitto, tassa, prigione, fallimento):
        // sono gia' avvenuti dentro rollDice, qui vengono raccontati alla view.
        this.modelEvents.publishPending();
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
        this.modelEvents.publishPending();
        this.notifyObservers(observer -> observer.onTurnStarted(nextPlayer));
        this.notifyObservers(observer -> observer.onGameStateChanged(this.state));
    }

    /**
     * Fa pagare al giocatore di turno la cauzione per uscire subito di prigione.
     * Dopo il pagamento il giocatore puo' lanciare i dadi normalmente.
     *
     * @return true se la cauzione e' stata pagata e il giocatore e' libero
     * @throws IllegalStateException se la partita non e' in corso, se non e' il momento
     *                               di lanciare o se il giocatore di turno non e' in prigione
     */
    public boolean payBail() {
        this.requireGameInProgress();
        final boolean released = this.turnManager.payBail();
        this.modelEvents.publishPending();
        this.notifyObservers(observer -> observer.onGameStateChanged(this.state));
        this.checkGameOver();
        return released;
    }

    /**
     * Il giocatore di turno accetta l'offerta e compra la proprieta' su cui si e'
     * fermato: paga il prezzo, diventa il proprietario e il turno riparte da dove era
     * rimasto.
     * <p>
     * Se il giocatore nel frattempo non se la potesse piu' permettere l'acquisto non
     * viene fatto, ma l'offerta si chiude comunque: la proprieta' resta libera.
     *
     * @return true se l'acquisto e' andato a buon fine
     * @throws IllegalStateException se non c'e' nessuna offerta a cui rispondere
     */
    public boolean buyOfferedProperty() {
        final Property offered = this.requireOfferedProperty();
        final Player buyer = this.state.getPendingPurchasePlayer().orElseThrow();
        final boolean bought = this.state.getContext().getEconomy().buyProperty(buyer, offered);
        this.state.resolvePendingPurchase();
        this.modelEvents.publishPending();
        this.notifyObservers(observer -> observer.onPurchaseResolved(buyer, offered, bought));
        this.notifyObservers(observer -> observer.onGameStateChanged(this.state));
        return bought;
    }

    /**
     * Il giocatore di turno rifiuta l'offerta: non paga niente e la proprieta' resta
     * libera, cosi' potra' comprarla qualcun altro piu' avanti. Il turno riparte da dove
     * era rimasto.
     * <p>
     * Nel gioco vero a questo punto partirebbe un'asta fra gli altri giocatori: qui
     * l'abbiamo lasciata fuori, e' una possibile aggiunta futura.
     *
     * @throws IllegalStateException se non c'e' nessuna offerta a cui rispondere
     */
    public void declineOfferedProperty() {
        final Property offered = this.requireOfferedProperty();
        final Player player = this.state.getPendingPurchasePlayer().orElseThrow();
        this.state.resolvePendingPurchase();
        this.modelEvents.publishPending();
        this.notifyObservers(observer -> observer.onPurchaseResolved(player, offered, false));
        this.notifyObservers(observer -> observer.onGameStateChanged(this.state));
    }

    /**
     * Gioca automaticamente un turno intero: lancia finche' e' consentito (i doppi
     * fanno rilanciare) e poi passa la mano. Utile per la demo testuale e per i test;
     * la GUI usera' invece {@link #rollDice()} ed {@link #endTurn()} in risposta ai pulsanti.
     * <p>
     * Qui non c'e' nessuno a cui chiedere se comprare, quindi le offerte vengono
     * accettate in automatico: la demo si comporta come prima che l'acquisto diventasse
     * una scelta.
     *
     * @throws IllegalStateException se la partita non e' in corso
     */
    public void playTurn() {
        this.requireGameInProgress();
        while (this.canRollDice() || this.canBuyOfferedProperty()) {
            if (this.canBuyOfferedProperty()) {
                this.buyOfferedProperty();
            } else {
                this.rollDice();
            }
        }
        if (this.canEndTurn()) {
            this.endTurn();
        }
    }

    /**
     * @return la proprieta' offerta in questo momento
     * @throws IllegalStateException se non c'e' nessuna offerta aperta
     */
    private Property requireOfferedProperty() {
        this.requireGameInProgress();
        if (this.state.getPhase() != GamePhase.AWAITING_PURCHASE_DECISION) {
            throw new IllegalStateException("Non c'e' nessuna proprieta' da decidere");
        }
        return this.state.getPendingPurchaseProperty()
                .orElseThrow(() -> new IllegalStateException("Non c'e' nessuna proprieta' da decidere"));
    }

    // ------------------------------------------------------------------
    // Salvataggio e caricamento
    // ------------------------------------------------------------------

    /**
     * Salva la partita in corso sul file indicato, sostituendolo se esiste gia'.
     * <p>
     * Va chiamato dal thread che esegue i comandi (per la GUI, l'EDT): la fotografia
     * della partita deve essere presa fra un comando e l'altro, mai mentre un lancio la
     * sta modificando. Il file e' di poche righe, quindi scriverlo non blocca
     * l'interfaccia in modo percettibile.
     * <p>
     * Non notifica gli osservatori: salvare non cambia la partita.
     *
     * @param file il file su cui salvare
     * @return l'esito da mostrare all'utente; i problemi di disco non lanciano eccezioni
     * @throws IllegalArgumentException se il file e' null
     */
    public PersistenceResult saveGame(final Path file) {
        return this.saver.save(this.state, file);
    }

    /**
     * Carica una partita da file e la mette al posto di quella in corso, notificando gli
     * osservatori.
     * <p>
     * E' la forma sincrona del caricamento, comoda nei test e per chi non ha
     * un'interfaccia da tenere reattiva: equivale a {@link #readSavedGame(Path)} seguito
     * da {@link #resumeGame(GameState)}. Se il file non e' valido la partita in corso
     * resta com'era e nessun osservatore viene avvisato.
     *
     * @param file il file da caricare
     * @return l'esito da mostrare all'utente; un file mancante o danneggiato non lancia eccezioni
     * @throws IllegalArgumentException se il file e' null
     */
    public PersistenceResult loadGame(final Path file) {
        final GameState loaded;
        try {
            loaded = this.readSavedGame(file);
        } catch (final SaveFileException e) {
            return PersistenceResult.failure("Caricamento non riuscito: " + e.getMessage());
        }
        this.resumeGame(loaded);
        return PersistenceResult.success("Partita caricata da " + file.getFileName());
    }

    /**
     * Legge un salvataggio e ricostruisce la partita, <em>senza</em> metterla in gioco.
     * <p>
     * E' la parte del caricamento che lavora sul disco, ed e' separata apposta da
     * {@link #resumeGame(GameState)}: non modifica il motore, non notifica nessuno e usa
     * solo campi che non cambiano mai (il lettore e i dadi). Per questo, a differenza
     * degli altri comandi, puo' essere eseguita in un thread in background; la partita
     * restituita e' nuova e non ancora condivisa con nessuno.
     *
     * @param file il file da leggere
     * @return la partita ricostruita, completa e coerente
     * @throws SaveFileException        se il file manca, non si legge, e' danneggiato o incompatibile
     * @throws IllegalArgumentException se il file e' null
     */
    public GameState readSavedGame(final Path file) throws SaveFileException {
        return this.loader.load(file, this.dice);
    }

    /**
     * Mette in gioco una partita gia' pronta - di norma letta con
     * {@link #readSavedGame(Path)} - al posto di quella in corso, e lo annuncia agli
     * osservatori.
     * <p>
     * <b>Osservatori.</b> Restano gli stessi, registrati una volta sola. Gli eventi del
     * model viaggiano pero' sul canale della partita ({@link GameEventSupport}), quindi le
     * registrazioni vengono spostate dal canale della partita abbandonata a quello della
     * nuova: nessuno riceve gli eventi due volte, e la partita abbandonata non trattiene
     * riferimenti alle view. Non serve ricreare le view ne' registrarle di nuovo.
     * <p>
     * <b>Notifiche.</b> Le stesse di un avvio, nello stesso ordine:
     * {@link GameObserver#onGameLoaded(GameState)} (che per chi non lo ridefinisce vale
     * come {@link GameObserver#onGameStarted(GameState)}), il turno corrente se la partita
     * non e' finita, e infine {@link GameObserver#onGameStateChanged(GameState)}, l'evento
     * con cui si chiude ogni comando.
     * <p>
     * Come ogni comando, va chiamato sul thread degli osservatori: per la GUI, l'EDT.
     *
     * @param loaded la partita da mettere in gioco
     * @throws IllegalArgumentException se la partita e' null
     */
    public void resumeGame(final GameState loaded) {
        if (loaded == null) {
            throw new IllegalArgumentException("La partita da riprendere non puo' essere null");
        }
        this.observers.forEach(this.modelEvents::removeListener);
        this.state = loaded;
        this.turnManager = new TurnManager(loaded);
        this.modelEvents = loaded.getContext().getEvents();
        this.observers.forEach(this.modelEvents::addListener);
        this.started = true;

        this.notifyObservers(observer -> observer.onGameLoaded(loaded));
        if (!loaded.isGameOver()) {
            final Player current = loaded.getCurrentPlayer();
            this.notifyObservers(observer -> observer.onTurnStarted(current));
        }
        this.modelEvents.publishPending();
        this.notifyObservers(observer -> observer.onGameStateChanged(loaded));
    }

    // ------------------------------------------------------------------
    // Interrogazioni per la view
    // ------------------------------------------------------------------

    /** @return true se ora il giocatore di turno puo' lanciare i dadi (es. per abilitare un pulsante) */
    public boolean canRollDice() {
        return this.isInProgress() && this.state.getPhase() == GamePhase.ROLL;
    }

    /**
     * @return true se il giocatore di turno e' in prigione e puo' permettersi la
     *         cauzione (per abilitare il pulsante "paga la cauzione")
     */
    public boolean canPayBail() {
        return this.isInProgress()
                && this.state.getPhase() == GamePhase.ROLL
                && this.state.getContext().getJail().canPayBail(this.state.getCurrentPlayer());
    }

    /** @return true se ora il giocatore di turno puo' (e deve) passare la mano */
    public boolean canEndTurn() {
        return this.isInProgress() && this.state.getPhase() == GamePhase.END_TURN;
    }

    /**
     * @return true se c'e' un'offerta aperta e il giocatore puo' ancora permettersi la
     *         proprieta' (per abilitare il pulsante "Compra")
     */
    public boolean canBuyOfferedProperty() {
        return this.isAwaitingPurchaseDecision()
                && this.state.getPendingPurchaseProperty()
                        .map(property -> this.state.getContext().getBank()
                                .canAfford(this.state.getCurrentPlayer(), property.getPrice()))
                        .orElse(false);
    }

    /**
     * @return true se c'e' un'offerta aperta da rifiutare (per abilitare il pulsante
     *         "Non comprare"). Rifiutare si puo' sempre, anche quando i soldi non
     *         bastassero piu'
     */
    public boolean canDeclineOfferedProperty() {
        return this.isAwaitingPurchaseDecision();
    }

    /** @return la proprieta' offerta in acquisto, se c'e' un'offerta aperta */
    public Optional<Property> getOfferedProperty() {
        return this.isAwaitingPurchaseDecision() ? this.state.getPendingPurchaseProperty() : Optional.empty();
    }

    /** @return true se la partita e' ferma in attesa di una decisione di acquisto */
    private boolean isAwaitingPurchaseDecision() {
        return this.isInProgress()
                && this.state.getPhase() == GamePhase.AWAITING_PURCHASE_DECISION
                && this.state.hasPendingPurchase();
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

    /**
     * Traduce l'esito del lancio nell'evento di spostamento corrispondente.
     * <p>
     * Gli esiti che non muovono la pedina (prigione, cauzione) non producono un evento
     * di movimento: li annuncia gia' il model tramite
     * {@link it.unibo.monopoly.model.game.JailManager JailManager}.
     */
    private void notifyMovement(final RollResult result) {
        if (!result.outcome().movesPlayer()) {
            return;
        }
        final Player player = result.player();
        final Board board = this.state.getBoard();
        final Tile from = board.getTileAt(result.fromPosition());
        final Tile to = board.getTileAt(result.toPosition());
        this.notifyObservers(observer -> observer.onPlayerMoved(player, from, to));
    }

    /**
     * Chiude la partita quando resta un solo giocatore non fallito.
     * <p>
     * Dal Giorno 3 il fallimento e' reale: chi non riesce a pagare un affitto o una
     * tassa esce dalla partita. I giocatori falliscono uno alla volta, quindi ne resta
     * sempre almeno uno.
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
