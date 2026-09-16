package it.unibo.monopoly.model.persistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import it.unibo.monopoly.model.board.Board;
import it.unibo.monopoly.model.board.BoardFactory;
import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.economy.Bank;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.JailManager;
import it.unibo.monopoly.model.game.TurnManager;
import it.unibo.monopoly.model.persistence.SaveFileException.Problem;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * Carica una partita da file: legge il salvataggio e ne ricostruisce un
 * {@link GameState} completo e coerente.
 * <p>
 * Anche qui il lavoro e' diviso in due passi:
 * <ol>
 *   <li>{@link #readSaveData(Path)} legge il file e lo traduce in un {@link SaveData},
 *       controllando che sia un salvataggio compatibile e che ogni dato sia presente e
 *       del tipo giusto;</li>
 *   <li>{@link #createGameState(SaveData, Dice)} ricostruisce la partita da quei dati e
 *       ne verifica la coerenza.</li>
 * </ol>
 * {@link #load(Path, Dice)} esegue entrambi i passi.
 * <p>
 * <b>Tutto o niente.</b> La partita viene costruita da zero - banca, tabellone,
 * giocatori - e restituita solo alla fine, quando ogni controllo e' passato. Se il file
 * manca, non si legge, e' danneggiato o incompatibile si ottiene una
 * {@link SaveFileException} con il motivo, e nessuno stato parziale: la partita in
 * corso non viene toccata, perche' il caricatore non la conosce nemmeno.
 * <p>
 * <b>Riuso delle regole.</b> Per ricostruire la partita non si scrivono i campi a mano:
 * si usano gli stessi metodi del model usati durante il gioco. Il tabellone e' quello
 * di {@link BoardFactory}, collegato alla banca ricostruita; le proprieta' vengono
 * assegnate con {@link Player#addProperty(Property)}, che aggiorna anche il proprietario
 * della casella; i tentativi di uscita di prigione vengono registrati sul
 * {@link JailManager}. I costruttori e i setter del model rifiutano gia' i valori
 * impossibili (denaro negativo, posizione fuori dal tabellone, nome vuoto...), e i loro
 * errori diventano un salvataggio {@link Problem#CORRUPTED}.
 * <p>
 * La classe non ha stato, quindi la stessa istanza puo' essere usata da piu' thread:
 * leggere un file non tocca nulla di condiviso.
 */
public class GameStateLoader {

    /**
     * Legge il file e ricostruisce la partita.
     *
     * @param file il file di salvataggio
     * @param dice i dadi con cui continuare la partita (non vengono salvati: vedi {@link SaveData})
     * @return la partita ricostruita
     * @throws SaveFileException        se il file manca, non si legge, e' danneggiato o incompatibile
     * @throws IllegalArgumentException se il file o i dadi sono null
     */
    public GameState load(final Path file, final Dice dice) throws SaveFileException {
        return this.createGameState(this.readSaveData(file), dice);
    }

    /**
     * Legge il file e lo traduce nei dati del salvataggio, senza ancora ricostruire la
     * partita.
     *
     * @param file il file di salvataggio
     * @return i dati letti
     * @throws SaveFileException        se il file manca, non si legge, e' danneggiato o incompatibile
     * @throws IllegalArgumentException se il file e' null
     */
    public SaveData readSaveData(final Path file) throws SaveFileException {
        if (file == null) {
            throw new IllegalArgumentException("Il file da caricare non puo' essere null");
        }
        if (Files.notExists(file)) {
            throw new SaveFileException(Problem.FILE_NOT_FOUND, file.toString());
        }
        if (!Files.isRegularFile(file)) {
            throw new SaveFileException(Problem.UNREADABLE, file + " non e' un file");
        }
        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (final NoSuchFileException e) {
            // Il file e' sparito fra il controllo e l'apertura.
            throw new SaveFileException(Problem.FILE_NOT_FOUND, file.toString(), e);
        } catch (final CharacterCodingException e) {
            throw new SaveFileException(Problem.CORRUPTED, "il file contiene byte che non sono testo valido", e);
        } catch (final IOException e) {
            throw new SaveFileException(Problem.UNREADABLE, String.valueOf(e.getMessage()), e);
        } catch (final IllegalArgumentException e) {
            // Properties.load rifiuta cosi' le sequenze di escape Unicode malformate.
            throw new SaveFileException(Problem.CORRUPTED, "testo non valido (" + e.getMessage() + ")", e);
        }
        return SaveFileFormat.fromProperties(properties);
    }

    /**
     * Ricostruisce una partita dai dati di un salvataggio, verificandone la coerenza.
     *
     * @param data i dati del salvataggio
     * @param dice i dadi con cui continuare la partita
     * @return la partita ricostruita, pronta per essere messa in gioco
     * @throws SaveFileException        se i dati non descrivono una partita possibile
     * @throws IllegalArgumentException se i dati o i dadi sono null
     */
    public GameState createGameState(final SaveData data, final Dice dice) throws SaveFileException {
        if (data == null || dice == null) {
            throw new IllegalArgumentException("I dati del salvataggio e i dadi non possono essere null");
        }
        try {
            return rebuild(data, dice);
        } catch (final IllegalArgumentException e) {
            throw new SaveFileException(Problem.CORRUPTED, e.getMessage(), e);
        }
    }

    /**
     * Costruisce la partita un pezzo alla volta. Ogni incoerenza e' segnalata con una
     * {@link IllegalArgumentException}, come fanno i costruttori e i setter del model:
     * {@link #createGameState} le traduce tutte nello stesso modo.
     */
    private static GameState rebuild(final SaveData data, final Dice dice) {
        // Stesso ordine di GameState.createStandardGame: prima i servizi, poi il tabellone
        // collegato a quei servizi, cosi' caselle e regole usano la stessa banca.
        final GameContext context = new GameContext(new Bank(data.bankBalance()));
        final Board board = BoardFactory.createStandardBoard(context);

        final List<Player> players = new ArrayList<>(data.players().size());
        for (final SaveData.PlayerData saved : data.players()) {
            players.add(createPlayer(saved, board, context.getJail()));
        }

        final GameState state = new GameState(board, players, dice, context);
        state.setCurrentPlayerIndex(data.currentPlayerIndex());
        state.setPhase(data.phase());
        state.setConsecutiveDoubles(data.consecutiveDoubles());
        state.setGameOver(data.gameOver());
        checkTurn(state);
        return state;
    }

    /** Ricrea un giocatore, la sua situazione in prigione e le proprieta' che possiede. */
    private static Player createPlayer(final SaveData.PlayerData saved, final Board board, final JailManager jail) {
        final Player player = new Player(saved.name(), new Token(saved.tokenName(), saved.tokenColor()), saved.money());
        player.setPosition(saved.position());
        player.setStatus(saved.status());
        restoreJail(player, saved.failedJailAttempts(), jail);

        if (player.isBankrupt()) {
            // Il fallimento cede al creditore tutto il contante e tutte le proprieta'.
            require(player.getMoney() == 0 && saved.propertyPositions().isEmpty(),
                    player.getName() + " e' fallito ma possiede ancora denaro o proprieta'");
        }
        for (final int position : saved.propertyPositions()) {
            final Tile tile = board.getTileAt(position);
            if (!(tile instanceof Property property)) {
                throw new IllegalArgumentException("la casella " + position + " (" + tile.getName()
                        + ") di " + player.getName() + " non e' una proprieta'");
            }
            require(property.isAvailable(), "la proprieta' " + property.getName() + " risulta di due giocatori");
            player.addProperty(property);
        }
        return player;
    }

    /**
     * Rimette i tentativi di uscita dalla prigione.
     * <p>
     * In prigione un giocatore sta sulla casella della prigione e ha fallito meno di
     * {@link JailManager#MAX_ATTEMPTS} tentativi: al tentativo che esaurisce il limite
     * la cauzione diventa obbligatoria e il giocatore esce (o fallisce). I tentativi si
     * registrano con lo stesso metodo usato durante il gioco.
     */
    private static void restoreJail(final Player player, final int failedAttempts, final JailManager jail) {
        if (!player.isInJail()) {
            require(failedAttempts == 0, player.getName() + " non e' in prigione ma ha tentativi di uscita registrati");
            return;
        }
        require(player.getPosition() == Board.JAIL_POSITION,
                player.getName() + " e' in prigione ma non si trova sulla casella della prigione");
        require(failedAttempts >= 0 && failedAttempts < JailManager.MAX_ATTEMPTS,
                "tentativi di uscita di prigione non validi per " + player.getName() + " (" + failedAttempts + ")");
        for (int attempt = 0; attempt < failedAttempts; attempt++) {
            jail.registerFailedAttempt(player);
        }
    }

    /**
     * Verifica che turno e fine partita siano una situazione raggiungibile giocando.
     * <p>
     * Il salvataggio avviene sempre fra un comando e l'altro, quindi la fase e'
     * {@link GamePhase#ROLL} o {@link GamePhase#END_TURN}. Una partita in corso ha almeno
     * due giocatori non falliti, una partita finita esattamente uno. Il giocatore di turno
     * puo' essere fallito solo se deve passare la mano o se la partita e' finita.
     */
    private static void checkTurn(final GameState state) {
        require(state.getPhase() == GamePhase.ROLL || state.getPhase() == GamePhase.END_TURN,
                "fase del turno non valida per una partita salvata (" + state.getPhase() + ")");
        require(state.getConsecutiveDoubles() < TurnManager.MAX_CONSECUTIVE_DOUBLES,
                "numero di doppi consecutivi non valido (" + state.getConsecutiveDoubles() + ")");

        final int activePlayers = state.getActivePlayers().size();
        if (state.isGameOver()) {
            require(activePlayers == 1, "la partita risulta finita ma i giocatori in gioco sono " + activePlayers);
        } else {
            require(activePlayers >= GameState.MIN_PLAYERS,
                    "la partita risulta in corso ma i giocatori in gioco sono " + activePlayers);
            require(!state.getCurrentPlayer().isBankrupt() || state.getPhase() == GamePhase.END_TURN,
                    "il giocatore di turno e' fallito ma deve ancora lanciare i dadi");
        }
    }

    /** Segnala un'incoerenza nei dati se la condizione non e' rispettata. */
    private static void require(final boolean condition, final String problem) {
        if (!condition) {
            throw new IllegalArgumentException(problem);
        }
    }
}
