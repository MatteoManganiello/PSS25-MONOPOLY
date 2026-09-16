package it.unibo.monopoly.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import it.unibo.monopoly.model.board.Board;
import it.unibo.monopoly.model.board.BoardFactory;
import it.unibo.monopoly.model.player.Player;

/**
 * La fotografia della partita in un certo momento.
 * <p>
 * Mette insieme tutto quello che serve per descrivere (e per salvare e ricaricare)
 * una partita: tabellone, giocatori, dadi, di chi e' il turno, la fase del turno, i
 * doppi consecutivi e se la partita e' finita.
 * <p>
 * Fa parte della partita anche il {@link GameContext}, cioe' la banca e le regole con
 * cui sono state costruite le caselle. Tenerlo qui serve a essere sicuri che
 * {@link TurnManager}, caselle e controller usino tutti gli stessi oggetti.
 * <p>
 * Questa classe tiene i dati in ordine ma non applica le regole del turno: quelle
 * sono compito del {@link TurnManager}. Tabellone, giocatori e dadi sono {@code final}
 * perche' non cambiano piu' dopo la creazione; hanno un setter solo i dati che
 * cambiano turno dopo turno.
 */
public class GameState {

    /** Quanti giocatori servono almeno per giocare. */
    public static final int MIN_PLAYERS = 2;

    /** Quanti giocatori al massimo possono giocare. */
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
     * Crea una partita nuova: inizia il primo giocatore della lista, che deve ancora
     * tirare i dadi.
     *
     * @param board   il tabellone
     * @param players i giocatori in ordine di turno, da {@value #MIN_PLAYERS} a {@value #MAX_PLAYERS}
     * @param dice    i dadi della partita
     * @throws IllegalArgumentException se un parametro e' null o la lista dei giocatori non va bene
     */
    public GameState(final Board board, final List<Player> players, final Dice dice) {
        this(board, players, dice, new GameContext());
    }

    /**
     * Crea una partita nuova dicendo anche quale contesto usare.
     * <p>
     * E' meglio usare questo quando il tabellone e' stato costruito con un certo
     * {@link GameContext}: passando qui lo stesso contesto, caselle e regole del turno
     * usano la stessa banca e la stessa prigione. Ci pensa gia'
     * {@link #createStandardGame(List, Dice)}.
     *
     * @param board   il tabellone
     * @param players i giocatori in ordine di turno
     * @param dice    i dadi della partita
     * @param context banca, regole sui soldi, prigione e canale degli eventi
     * @throws IllegalArgumentException se un parametro e' null o la lista dei giocatori non va bene
     */
    public GameState(final Board board, final List<Player> players, final Dice dice,
                     final GameContext context) {
        if (board == null || dice == null || context == null) {
            throw new IllegalArgumentException("Tabellone, dadi e contesto non possono essere null");
        }
        checkPlayers(players);
        this.board = board;
        // Copiamo la lista, cosi' nessuno da fuori puo' cambiare l'ordine dei giocatori.
        this.players = new ArrayList<>(players);
        this.dice = dice;
        this.context = context;
        this.currentPlayerIndex = 0;
        this.phase = GamePhase.ROLL;
        this.consecutiveDoubles = 0;
        this.gameOver = false;
    }

    /**
     * Prepara una partita completa: crea il contesto, costruisce il tabellone standard
     * collegato a quel contesto e restituisce lo stato iniziale.
     * <p>
     * E' qui che i pezzi vengono montati nell'ordine giusto, cosi' non puo' capitare
     * di avere un tabellone collegato a una banca diversa da quella della partita.
     *
     * @param players i giocatori in ordine di turno
     * @param dice    i dadi da usare (nei test, con seme fisso)
     * @return la partita appena iniziata sul tabellone standard
     * @throws IllegalArgumentException se i parametri non vanno bene
     */
    public static GameState createStandardGame(final List<Player> players, final Dice dice) {
        final GameContext context = new GameContext();
        return new GameState(BoardFactory.createStandardBoard(context), players, dice, context);
    }

    public Board getBoard() {
        return this.board;
    }

    /** @return tutti i giocatori in ordine di turno, anche i falliti, in sola lettura */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(this.players);
    }

    /** @return il contesto della partita: banca, economia, prigione ed eventi */
    public GameContext getContext() {
        return this.context;
    }

    public Dice getDice() {
        return this.dice;
    }

    /** @return in che posizione della lista sta il giocatore di turno */
    public int getCurrentPlayerIndex() {
        return this.currentPlayerIndex;
    }

    /**
     * Decide direttamente di chi e' il turno: serve per ricaricare una partita e nei
     * test. Durante il gioco normale si usa {@link #advanceToNextPlayer()}.
     *
     * @param currentPlayerIndex la posizione del giocatore di turno
     * @throws IllegalArgumentException se quella posizione non esiste
     */
    public void setCurrentPlayerIndex(final int currentPlayerIndex) {
        if (currentPlayerIndex < 0 || currentPlayerIndex >= this.players.size()) {
            throw new IllegalArgumentException("Indice del giocatore non valido: " + currentPlayerIndex);
        }
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public Player getCurrentPlayer() {
        return this.players.get(this.currentPlayerIndex);
    }

    /** @return a che punto e' il turno adesso */
    public GamePhase getPhase() {
        return this.phase;
    }

    /**
     * @param phase la nuova fase del turno
     * @throws IllegalArgumentException se la fase e' null
     */
    public void setPhase(final GamePhase phase) {
        if (phase == null) {
            throw new IllegalArgumentException("La fase del turno non puo' essere null");
        }
        this.phase = phase;
    }

    /** @return quanti doppi di fila ha fatto il giocatore di turno */
    public int getConsecutiveDoubles() {
        return this.consecutiveDoubles;
    }

    /**
     * @param consecutiveDoubles quanti doppi di fila, non puo' essere negativo
     * @throws IllegalArgumentException se il valore e' negativo
     */
    public void setConsecutiveDoubles(final int consecutiveDoubles) {
        if (consecutiveDoubles < 0) {
            throw new IllegalArgumentException("I doppi consecutivi non possono essere negativi");
        }
        this.consecutiveDoubles = consecutiveDoubles;
    }

    /**
     * Segna che il giocatore di turno ha fatto un altro doppio.
     *
     * @return quanti doppi di fila ha fatto adesso
     */
    public int incrementConsecutiveDoubles() {
        this.consecutiveDoubles++;
        return this.consecutiveDoubles;
    }

    /** @return true se la partita e' finita */
    public boolean isGameOver() {
        return this.gameOver;
    }

    /** @param gameOver true per dire che la partita e' finita */
    public void setGameOver(final boolean gameOver) {
        this.gameOver = gameOver;
    }

    /**
     * Da' i giocatori ancora in partita, cioe' quelli che non sono falliti.
     * Chi e' in prigione conta come in gioco: e' fermo, ma non e' fuori.
     *
     * @return i giocatori non falliti, in ordine di turno
     */
    public List<Player> getActivePlayers() {
        return this.players.stream()
                .filter(player -> !player.isBankrupt())
                .toList();
    }

    /**
     * Passa il turno al giocatore dopo e glielo prepara: azzera i doppi consecutivi e
     * rimette la fase a {@link GamePhase#ROLL}.
     * <p>
     * Si gira in tondo (dopo l'ultimo si torna al primo) e i falliti si saltano. Quelli
     * in prigione invece non si saltano: il loro turno serve proprio per provare a
     * uscire. Se e' rimasto un solo giocatore, il turno torna a lui.
     *
     * @return il nuovo giocatore di turno
     * @throws IllegalStateException se sono falliti tutti
     */
    public Player advanceToNextPlayer() {
        if (this.getActivePlayers().isEmpty()) {
            throw new IllegalStateException("Nessun giocatore e' ancora in partita");
        }
        int next = this.currentPlayerIndex;
        // Il ciclo prima o poi finisce, perche' almeno un giocatore non e' fallito.
        do {
            next = (next + 1) % this.players.size();
        } while (this.players.get(next).isBankrupt());

        this.currentPlayerIndex = next;
        this.consecutiveDoubles = 0;
        this.phase = GamePhase.ROLL;
        return this.getCurrentPlayer();
    }

    /** Controlla che con questa lista di giocatori si possa davvero giocare. */
    private static void checkPlayers(final List<Player> players) {
        if (players == null || players.size() < MIN_PLAYERS || players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Servono da " + MIN_PLAYERS + " a " + MAX_PLAYERS + " giocatori");
        }
        if (players.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("L'elenco dei giocatori non puo' contenere null");
        }
        // Player non riscrive equals, quindi il Set scarta solo lo stesso identico oggetto messo due volte.
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
