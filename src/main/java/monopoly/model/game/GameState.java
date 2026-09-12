package monopoly.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import monopoly.model.board.Board;
import monopoly.model.board.BoardFactory;
import monopoly.model.player.Player;

/**
 * Fotografia completa dello stato di una partita in un dato istante.
 * <p>
 * Raccoglie in un unico oggetto tutto cio' che serve per descrivere (e in futuro
 * salvare e ricaricare) una partita: tabellone, giocatori, dadi, giocatore di
 * turno, fase del turno, doppi consecutivi e fine della partita.
 * <p>
 * Dal Giorno 3 fa parte della partita anche il {@link GameContext}: la banca e le
 * regole (economia e prigione) con cui sono state costruite le caselle del tabellone.
 * Tenerlo qui garantisce che {@link TurnManager}, caselle e controller lavorino tutti
 * sugli stessi oggetti.
 * <p>
 * La classe custodisce i dati e ne garantisce la coerenza, ma non applica le
 * regole del turno: quello e' compito di {@link TurnManager}. Tabellone, giocatori
 * e dadi sono {@code final} perche' la "struttura" della partita non cambia dopo
 * la creazione; hanno un setter solo i dati che evolvono turno dopo turno.
 */
public class GameState {

    /** Numero minimo di giocatori per iniziare una partita. */
    public static final int MIN_PLAYERS = 2;

    /** Numero massimo di giocatori ammessi dal regolamento. */
    public static final int MAX_PLAYERS = 8;

    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private final GameContext context;

    private int currentPlayerIndex;
    private GamePhase phase;
    private int consecutiveDoubles;
    private boolean gameOver;

    /**
     * Crea lo stato iniziale di una nuova partita: tocca al primo giocatore della
     * lista, che deve ancora lanciare i dadi.
     *
     * @param board   il tabellone
     * @param players i giocatori nell'ordine di turno, da {@value #MIN_PLAYERS} a {@value #MAX_PLAYERS}
     * @param dice    i dadi della partita
     * @throws IllegalArgumentException se un parametro e' null o l'elenco dei giocatori non e' valido
     */
    public GameState(final Board board, final List<Player> players, final Dice dice) {
        this(board, players, dice, new GameContext());
    }

    /**
     * Crea lo stato iniziale di una partita indicando anche i servizi da usare.
     * <p>
     * E' il costruttore da preferire quando il tabellone e' stato costruito con un
     * certo {@link GameContext}: passando qui lo stesso contesto, le caselle e le
     * regole del turno useranno la stessa banca e la stessa prigione. Ci pensa gia'
     * {@link #createStandardGame(List, Dice)}.
     *
     * @param board   il tabellone
     * @param players i giocatori nell'ordine di turno
     * @param dice    i dadi della partita
     * @param context banca, regole economiche, prigione e canale degli eventi
     * @throws IllegalArgumentException se un parametro e' null o l'elenco dei giocatori non e' valido
     */
    public GameState(final Board board, final List<Player> players, final Dice dice,
                     final GameContext context) {
        if (board == null || dice == null || context == null) {
            throw new IllegalArgumentException("Tabellone, dadi e contesto non possono essere null");
        }
        checkPlayers(players);
        this.board = board;
        // Copia difensiva: l'ordine dei giocatori non puo' essere alterato da fuori.
        this.players = new ArrayList<>(players);
        this.dice = dice;
        this.context = context;
        this.currentPlayerIndex = 0;
        this.phase = GamePhase.ROLL;
        this.consecutiveDoubles = 0;
        this.gameOver = false;
    }

    /**
     * Prepara una partita completa: crea i servizi condivisi, costruisce il tabellone
     * standard collegato a quei servizi e restituisce lo stato iniziale.
     * <p>
     * E' il punto in cui i pezzi vengono montati nell'ordine giusto, cosi' nessuno
     * puo' ritrovarsi con un tabellone collegato a una banca diversa da quella della
     * partita.
     *
     * @param players i giocatori nell'ordine di turno
     * @param dice    i dadi da usare (nei test, con seme fisso)
     * @return lo stato iniziale di una nuova partita sul tabellone standard
     * @throws IllegalArgumentException se i parametri non sono validi
     */
    public static GameState createStandardGame(final List<Player> players, final Dice dice) {
        final GameContext context = new GameContext();
        return new GameState(BoardFactory.createStandardBoard(context), players, dice, context);
    }

    /** @return il tabellone della partita */
    public Board getBoard() {
        return this.board;
    }

    /** @return tutti i giocatori in ordine di turno (falliti compresi), in sola lettura */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(this.players);
    }

    /** @return i servizi condivisi della partita: banca, economia, prigione, eventi */
    public GameContext getContext() {
        return this.context;
    }

    /** @return i dadi della partita */
    public Dice getDice() {
        return this.dice;
    }

    /** @return l'indice, nella lista dei giocatori, di chi deve giocare */
    public int getCurrentPlayerIndex() {
        return this.currentPlayerIndex;
    }

    /**
     * Imposta direttamente il giocatore di turno (per ricaricare una partita o nei test).
     * Per il normale avanzamento del gioco si usa {@link #advanceToNextPlayer()}.
     *
     * @param currentPlayerIndex indice del giocatore di turno
     * @throws IllegalArgumentException se l'indice non corrisponde a nessun giocatore
     */
    public void setCurrentPlayerIndex(final int currentPlayerIndex) {
        if (currentPlayerIndex < 0 || currentPlayerIndex >= this.players.size()) {
            throw new IllegalArgumentException("Indice del giocatore non valido: " + currentPlayerIndex);
        }
        this.currentPlayerIndex = currentPlayerIndex;
    }

    /** @return il giocatore di turno */
    public Player getCurrentPlayer() {
        return this.players.get(this.currentPlayerIndex);
    }

    /** @return la fase in cui si trova il turno corrente */
    public GamePhase getPhase() {
        return this.phase;
    }

    /**
     * @param phase la nuova fase del turno, non nulla
     * @throws IllegalArgumentException se la fase e' null
     */
    public void setPhase(final GamePhase phase) {
        if (phase == null) {
            throw new IllegalArgumentException("La fase del turno non puo' essere null");
        }
        this.phase = phase;
    }

    /** @return quanti doppi di fila ha fatto il giocatore di turno in questo turno */
    public int getConsecutiveDoubles() {
        return this.consecutiveDoubles;
    }

    /**
     * @param consecutiveDoubles numero di doppi consecutivi, non negativo
     * @throws IllegalArgumentException se il valore e' negativo
     */
    public void setConsecutiveDoubles(final int consecutiveDoubles) {
        if (consecutiveDoubles < 0) {
            throw new IllegalArgumentException("I doppi consecutivi non possono essere negativi");
        }
        this.consecutiveDoubles = consecutiveDoubles;
    }

    /**
     * Registra un nuovo doppio del giocatore di turno.
     *
     * @return il numero aggiornato di doppi consecutivi
     */
    public int incrementConsecutiveDoubles() {
        this.consecutiveDoubles++;
        return this.consecutiveDoubles;
    }

    /** @return true se la partita e' terminata */
    public boolean isGameOver() {
        return this.gameOver;
    }

    /** @param gameOver true per segnare la partita come terminata */
    public void setGameOver(final boolean gameOver) {
        this.gameOver = gameOver;
    }

    /**
     * Restituisce i giocatori ancora in partita, cioe' quelli non falliti.
     * Chi e' in prigione conta come ancora in gioco: e' fermo, ma non eliminato.
     *
     * @return i giocatori non falliti, in ordine di turno
     */
    public List<Player> getActivePlayers() {
        return this.players.stream()
                .filter(player -> !player.isBankrupt())
                .toList();
    }

    /**
     * Passa il turno al giocatore successivo e prepara il suo turno: azzera i
     * doppi consecutivi e riporta la fase a {@link GamePhase#ROLL}.
     * <p>
     * L'ordine e' circolare (dopo l'ultimo giocatore si torna al primo) e i giocatori
     * falliti vengono saltati. Quelli in prigione invece NON vengono saltati: il loro
     * turno serve proprio a tentare di uscire. Se e' rimasto un solo giocatore attivo,
     * il turno torna a lui.
     *
     * @return il nuovo giocatore di turno
     * @throws IllegalStateException se tutti i giocatori sono falliti
     */
    public Player advanceToNextPlayer() {
        if (this.getActivePlayers().isEmpty()) {
            throw new IllegalStateException("Nessun giocatore e' ancora in partita");
        }
        int next = this.currentPlayerIndex;
        // Il ciclo termina sicuramente: almeno un giocatore non e' fallito.
        do {
            next = (next + 1) % this.players.size();
        } while (this.players.get(next).isBankrupt());

        this.currentPlayerIndex = next;
        this.consecutiveDoubles = 0;
        this.phase = GamePhase.ROLL;
        return this.getCurrentPlayer();
    }

    /** Controlla che l'elenco dei giocatori sia utilizzabile per una partita. */
    private static void checkPlayers(final List<Player> players) {
        if (players == null || players.size() < MIN_PLAYERS || players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Servono da " + MIN_PLAYERS + " a " + MAX_PLAYERS + " giocatori");
        }
        if (players.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("L'elenco dei giocatori non puo' contenere null");
        }
        // Player non ridefinisce equals: il Set scarta solo lo stesso oggetto inserito due volte.
        if (new HashSet<>(players).size() != players.size()) {
            throw new IllegalArgumentException("Lo stesso giocatore non puo' partecipare due volte");
        }
    }

    @Override
    public String toString() {
        return "GameState[currentPlayer=" + this.getCurrentPlayer().getName()
                + ", phase=" + this.phase
                + ", consecutiveDoubles=" + this.consecutiveDoubles
                + ", gameOver=" + this.gameOver
                + ", players=" + this.players
                + "]";
    }
}
