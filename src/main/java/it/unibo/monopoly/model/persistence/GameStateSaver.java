package it.unibo.monopoly.model.persistence;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.JailManager;

/**
 * Salva su file lo stato di una partita.
 * <p>
 * Il lavoro e' diviso in due passi, ciascuno in un metodo:
 * <ol>
 *   <li>{@link #createSaveData(GameState)} fotografa la partita in un {@link SaveData},
 *       copiando i soli dati che servono per ricostruirla;</li>
 *   <li>{@link #save(GameState, Path)} scrive quella fotografia su disco nel formato
 *       descritto da {@link SaveFileFormat}.</li>
 * </ol>
 * <p>
 * <b>Errori.</b> I problemi di disco (cartella inesistente, permessi, disco pieno) non
 * diventano eccezioni: {@link #save} restituisce un {@link PersistenceResult} negativo,
 * con un messaggio che la view puo' mostrare cosi' com'e'. Restano eccezioni solo gli
 * errori di programmazione, come passare un file null.
 * <p>
 * <b>Robustezza.</b> Il file non viene mai scritto "sul posto": si scrive un file
 * temporaneo nella stessa cartella e solo alla fine lo si sposta al posto di quello
 * definitivo. Se la scrittura si interrompe, un salvataggio precedente con lo stesso
 * nome resta intatto, e non rimane mai un file scritto a meta'.
 * <p>
 * La classe non ha stato: la stessa istanza puo' salvare partite diverse.
 */
public class GameStateSaver {

    /** Commento scritto in cima al file, per chi lo apre con un editor. */
    private static final String FILE_HEADER =
            "Partita di Monopoly salvata - formato versione " + SaveFileFormat.VERSION;

    /** Prefisso dei messaggi di errore mostrati all'utente. */
    private static final String FAILURE_PREFIX = "Salvataggio non riuscito: ";

    /**
     * Salva la partita sul file indicato, sostituendolo se esiste gia'.
     * <p>
     * La partita va salvata fra un comando e l'altro: se e' a meta' di una mossa (fase
     * {@link GamePhase#MOVE} o {@link GamePhase#ACTION}) il salvataggio viene rifiutato,
     * perche' produrrebbe un file che il caricamento non potrebbe accettare.
     *
     * @param state la partita da salvare, letta e non modificata
     * @param file  il file su cui scrivere
     * @return l'esito del salvataggio, da mostrare all'utente
     * @throws IllegalArgumentException se la partita o il file sono null
     */
    public PersistenceResult save(final GameState state, final Path file) {
        if (state == null || file == null) {
            throw new IllegalArgumentException("La partita e il file di salvataggio non possono essere null");
        }
        if (state.getPhase() != GamePhase.ROLL && state.getPhase() != GamePhase.END_TURN) {
            return PersistenceResult.failure(FAILURE_PREFIX + "la partita e' a meta' di una mossa");
        }
        if (Files.isDirectory(file)) {
            return PersistenceResult.failure(FAILURE_PREFIX + file + " e' una cartella, non un file");
        }
        try {
            writeSafely(SaveFileFormat.toProperties(this.createSaveData(state)), file);
            return PersistenceResult.success("Partita salvata in " + file.getFileName());
        } catch (final IOException e) {
            return PersistenceResult.failure(FAILURE_PREFIX + describe(e, file));
        }
    }

    /**
     * Fotografa la partita: copia in un {@link SaveData} i dati necessari per ricostruirla.
     * <p>
     * Le proprieta' vengono ricordate con la loro posizione sul tabellone. I tentativi di
     * uscita dalla prigione si salvano solo per chi e' in prigione: per gli altri il
     * {@link JailManager} potrebbe conservare un valore rimasto da prima (per esempio
     * di chi e' fallito non riuscendo a pagare la cauzione), che non ha piu' significato.
     *
     * @param state la partita da fotografare, letta e non modificata
     * @return i dati da salvare
     * @throws IllegalArgumentException se la partita e' null
     */
    public SaveData createSaveData(final GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("La partita da salvare non puo' essere null");
        }
        final JailManager jail = state.getContext().getJail();
        final List<SaveData.PlayerData> players = state.getPlayers().stream()
                .map(player -> new SaveData.PlayerData(
                        player.getName(),
                        player.getToken().getName(),
                        player.getToken().getColor(),
                        player.getMoney(),
                        player.getPosition(),
                        player.getStatus(),
                        player.getProperties().stream().map(Property::getPosition).toList(),
                        player.isInJail() ? jail.getFailedAttempts(player) : 0))
                .toList();
        return new SaveData(
                state.getContext().getBank().getBalance(),
                state.getCurrentPlayerIndex(),
                state.getPhase(),
                state.getConsecutiveDoubles(),
                state.isGameOver(),
                players);
    }

    /**
     * Scrive le proprieta' in un file temporaneo e poi lo sposta al posto di quello
     * definitivo.
     * <p>
     * Lo spostamento avviene all'interno della stessa cartella, dove il sistema operativo
     * di norma lo esegue in un colpo solo ({@link StandardCopyOption#ATOMIC_MOVE}); dove
     * non e' possibile si ripiega su uno spostamento normale.
     */
    private static void writeSafely(final Properties properties, final Path file) throws IOException {
        final Path target = file.toAbsolutePath();
        final Path folder = target.getParent();
        if (folder == null || target.getFileName() == null) {
            throw new IOException("percorso non valido: " + file);
        }
        final Path temporary = Files.createTempFile(folder, target.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, FILE_HEADER);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Dopo uno spostamento riuscito il temporaneo non esiste piu'; se qualcosa e'
            // andato storto, invece, non deve restare sparso nella cartella.
            Files.deleteIfExists(temporary);
        }
    }

    /** Traduce un errore di I/O in una frase comprensibile per l'utente. */
    private static String describe(final IOException error, final Path file) {
        final Path folder = file.toAbsolutePath().getParent();
        if (error instanceof NoSuchFileException) {
            return "la cartella " + folder + " non esiste";
        }
        if (error instanceof AccessDeniedException) {
            return "non ci sono i permessi per scrivere in " + folder;
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
