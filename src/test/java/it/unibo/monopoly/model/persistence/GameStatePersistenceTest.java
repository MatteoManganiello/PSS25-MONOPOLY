package it.unibo.monopoly.model.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.Dice;
import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.TurnManager;
import it.unibo.monopoly.model.persistence.SaveFileException.Problem;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * Test della persistenza nel model: dalla partita al file e ritorno, senza motore e
 * senza GUI.
 * <p>
 * I dadi hanno un seme fisso, cosi' la partita giocata prima di salvare e' sempre la
 * stessa. I file vengono scritti in una cartella temporanea che JUnit crea per ogni
 * test ({@link TempDir}) e cancella alla fine.
 */
class GameStatePersistenceTest {

    private static final long SEED = 42;

    /** Turni giocati prima di salvare: abbastanza per avere proprieta', affitti e tasse. */
    private static final int TURNS = 15;

    private static final int VICOLO_CORTO = 1;
    private static final int STAZIONE_SUD = 5;
    private static final int BASTIONI_GRAN_SASSO = 6;
    private static final int VIALE_MONTEROSA = 8;

    @TempDir
    Path folder;

    private GameStateSaver saver;
    private GameStateLoader loader;

    @BeforeEach
    void setUp() {
        saver = new GameStateSaver();
        loader = new GameStateLoader();
    }

    // ------------------------------------------------------------------
    // Salvataggio e ricaricamento
    // ------------------------------------------------------------------

    @Test
    void aPlayedGameIsTheSameAfterSaveAndLoad() throws SaveFileException {
        final GameState original = playedGame();
        assertFalse(ownedProperties(original).isEmpty(), "la partita di prova deve avere proprieta' comprate");

        final GameState loaded = saveAndLoad(original);

        assertSameGame(original, loaded);
        // Anche le due fotografie coincidono: il record SaveData confronta campo per campo.
        assertEquals(saver.createSaveData(original), saver.createSaveData(loaded));
    }

    @Test
    void jailBankruptcyAndTurnDetailsSurviveSaveAndLoad() throws SaveFileException {
        final GameState original = detailedGame();

        final GameState loaded = saveAndLoad(original);

        assertSameGame(original, loaded);
        final Player alice = loaded.getPlayers().get(0);
        assertTrue(alice.isInJail());
        assertEquals(2, loaded.getContext().getJail().getFailedAttempts(alice));
        assertTrue(loaded.getPlayers().get(1).isBankrupt());
        assertEquals(2, loaded.getCurrentPlayerIndex());
        assertEquals(GamePhase.END_TURN, loaded.getPhase());
        assertEquals(1, loaded.getConsecutiveDoubles());
    }

    @Test
    void namesWithSpecialCharactersSurviveSaveAndLoad() throws SaveFileException {
        final String trickyName = "  Zoë = #1 \\ ok!";
        final GameState original = GameState.createStandardGame(List.of(
                new Player(trickyName, new Token("Ferro: da stiro", "RED")),
                new Player("Bob", new Token("Dog", "BLUE"))), newDice());

        final GameState loaded = saveAndLoad(original);

        assertEquals(trickyName, loaded.getPlayers().get(0).getName());
        assertEquals("Ferro: da stiro", loaded.getPlayers().get(0).getToken().getName());
    }

    @Test
    void savingAgainReplacesTheFileWithoutLeavingTemporaryFiles() throws IOException, SaveFileException {
        final Path file = save(playedGame());

        save(detailedGame());

        assertSameGame(detailedGame(), loader.load(file, newDice()));
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    // ------------------------------------------------------------------
    // Proprieta' e proprietari
    // ------------------------------------------------------------------

    @Test
    void propertiesAreReassignedToTheirOwnersOnTheNewBoard() throws SaveFileException {
        final GameState original = detailedGame();

        final GameState loaded = saveAndLoad(original);

        for (final Tile tile : loaded.getBoard().getTiles()) {
            if (tile instanceof Property property) {
                final Property before = (Property) original.getBoard().getTileAt(property.getPosition());
                assertEquals(before.getOwner().map(Player::getName), property.getOwner().map(Player::getName),
                        property.getName());
                property.getOwner().ifPresent(owner -> {
                    // Il proprietario e' un giocatore della partita caricata, non di quella salvata,
                    // e i due lati della relazione sono allineati.
                    assertTrue(loaded.getPlayers().stream().anyMatch(player -> player == owner));
                    assertTrue(owner.getProperties().contains(property));
                });
            }
        }
        assertEquals(List.of("Vicolo Corto", "Stazione Sud", "Bastioni Gran Sasso"),
                names(loaded.getPlayers().get(2).getProperties()));
    }

    @Test
    void theLoadedBoardUsesTheBankOfTheLoadedGame() throws SaveFileException {
        final GameState original = detailedGame();
        final GameState loaded = saveAndLoad(original);
        final int originalBalance = original.getContext().getBank().getBalance();
        final int loadedBalance = loaded.getContext().getBank().getBalance();
        final Player carol = loaded.getPlayers().get(2);

        propertyAt(loaded, VIALE_MONTEROSA).onLand(carol);

        assertSame(carol, propertyAt(loaded, VIALE_MONTEROSA).getOwner().orElseThrow());
        assertEquals(loadedBalance + propertyAt(loaded, VIALE_MONTEROSA).getPrice(),
                loaded.getContext().getBank().getBalance());
        assertEquals(originalBalance, original.getContext().getBank().getBalance());
    }

    // ------------------------------------------------------------------
    // File mancanti, danneggiati o incompatibili
    // ------------------------------------------------------------------

    @Test
    void aMissingFileIsReportedAsFileNotFound() {
        assertProblem(Problem.FILE_NOT_FOUND, folder.resolve("assente.properties"));
    }

    @Test
    void aTruncatedFileIsReportedAsCorrupted() throws IOException {
        final Path file = save(detailedGame());
        final List<String> lines = Files.readAllLines(file);

        Files.write(file, lines.subList(0, lines.size() / 2));

        assertProblem(Problem.CORRUPTED, file);
    }

    @Test
    void bytesThatAreNotTextAreReportedAsCorrupted() throws IOException {
        final Path file = folder.resolve("binario.properties");

        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28, (byte) 0xFF});

        assertProblem(Problem.CORRUPTED, file);
    }

    @Test
    void aFileThatIsNotASaveIsReportedAsIncompatible() throws IOException {
        final Path file = folder.resolve("appunti.properties");

        Files.writeString(file, "spesa = pane, latte, uova");

        assertProblem(Problem.INCOMPATIBLE, file);
    }

    /**
     * Ogni riga altera un solo dato di un salvataggio valido: un valore del tipo sbagliato,
     * un valore impossibile o una situazione incoerente con le regole.
     */
    @ParameterizedTest(name = "{0} = {1} -> {2}")
    @CsvSource(delimiter = '|', textBlock = """
            format.version          | 99      | INCOMPATIBLE
            format.id               | scacchi | INCOMPATIBLE
            player.0.money          | tanti   | CORRUPTED
            game.over               | forse   | CORRUPTED
            game.phase              | VOLO    | CORRUPTED
            game.phase              | MOVE    | CORRUPTED
            game.playerCount        | 12      | CORRUPTED
            game.currentPlayerIndex | 7       | CORRUPTED
            player.2.position       | 40      | CORRUPTED
            player.0.position       | 5       | CORRUPTED
            player.0.jailAttempts   | 3       | CORRUPTED
            player.2.properties     | 1,7     | CORRUPTED
            player.0.properties     | 1       | CORRUPTED
            player.1.money          | 100     | CORRUPTED
            """)
    void invalidOrInconsistentDataIsRejected(final String key, final String value, final Problem expected)
            throws IOException {
        final Path file = save(detailedGame());

        replaceValue(file, key, value);

        assertProblem(expected, file);
    }

    @Test
    void savingIntoAMissingFolderReturnsAFailureInsteadOfThrowing() {
        final PersistenceResult result = saver.save(playedGame(), folder.resolve("manca").resolve("partita.properties"));

        assertFalse(result.successful());
        assertTrue(result.message().contains("non esiste"), result.message());
    }

    // ------------------------------------------------------------------
    // Partite di prova
    // ------------------------------------------------------------------

    /** Partita standard giocata per qualche turno con le regole vere e dadi a seme fisso. */
    private static GameState playedGame() {
        final GameState state = GameState.createStandardGame(newPlayers(), newDice());
        final TurnManager turns = new TurnManager(state);
        for (int turn = 0; turn < TURNS; turn++) {
            while (state.getPhase() == GamePhase.ROLL) {
                turns.rollDice();
            }
            turns.endTurn();
        }
        return state;
    }

    /**
     * Partita preparata a mano con i casi che una partita breve potrebbe non toccare:
     * Alice in prigione con due tentativi falliti, Bob fallito verso Carol (che eredita
     * le sue proprieta'), Carol di turno dopo aver gia' lanciato, con un doppio all'attivo.
     */
    private static GameState detailedGame() {
        final List<Player> players = newPlayers();
        final GameState state = GameState.createStandardGame(players, newDice());
        final GameContext context = state.getContext();
        final Player alice = players.get(0);
        final Player bob = players.get(1);
        final Player carol = players.get(2);

        context.getJail().sendToJail(alice);
        context.getJail().registerFailedAttempt(alice);
        context.getJail().registerFailedAttempt(alice);
        context.getEconomy().buyProperty(carol, propertyAt(state, VICOLO_CORTO));
        context.getEconomy().buyProperty(carol, propertyAt(state, STAZIONE_SUD));
        context.getEconomy().buyProperty(bob, propertyAt(state, BASTIONI_GRAN_SASSO));
        context.getEconomy().declareBankruptcy(bob, carol);
        carol.setPosition(STAZIONE_SUD);
        state.setCurrentPlayerIndex(2);
        state.setPhase(GamePhase.END_TURN);
        state.setConsecutiveDoubles(1);
        return state;
    }

    private static List<Player> newPlayers() {
        return List.of(
                new Player("Alice", new Token("Car", "RED")),
                new Player("Bob", new Token("Dog", "BLUE")),
                new Player("Carol", new Token("Hat", "GREEN")));
    }

    private static Dice newDice() {
        return new Dice(new Random(SEED));
    }

    // ------------------------------------------------------------------
    // Metodi di supporto
    // ------------------------------------------------------------------

    /** Salva la partita nella cartella del test e controlla che l'esito sia positivo. */
    private Path save(final GameState state) {
        final Path file = folder.resolve("partita.properties");
        final PersistenceResult result = saver.save(state, file);
        assertTrue(result.successful(), result.message());
        return file;
    }

    private GameState saveAndLoad(final GameState state) throws SaveFileException {
        return loader.load(save(state), newDice());
    }

    /** Il caricamento deve fallire con un errore gestito del tipo atteso, non con un'altra eccezione. */
    private void assertProblem(final Problem expected, final Path file) {
        final SaveFileException error = assertThrows(SaveFileException.class, () -> loader.load(file, newDice()));
        assertEquals(expected, error.getProblem(), error.getMessage());
    }

    /** Sostituisce il valore di una chiave nel file, controllando che la chiave ci sia davvero. */
    private static void replaceValue(final Path file, final String key, final String value) throws IOException {
        final String text = Files.readString(file);
        final Pattern line = Pattern.compile("^" + Pattern.quote(key) + "=.*$", Pattern.MULTILINE);
        assertTrue(line.matcher(text).find(), "il salvataggio di prova deve contenere la chiave " + key);
        Files.writeString(file, line.matcher(text).replaceFirst(Matcher.quoteReplacement(key + "=" + value)));
    }

    /** Confronta due partite dato per dato, senza passare dal formato di salvataggio. */
    private static void assertSameGame(final GameState expected, final GameState actual) {
        assertEquals(expected.getContext().getBank().getBalance(), actual.getContext().getBank().getBalance());
        assertEquals(expected.getCurrentPlayerIndex(), actual.getCurrentPlayerIndex());
        assertEquals(expected.getPhase(), actual.getPhase());
        assertEquals(expected.getConsecutiveDoubles(), actual.getConsecutiveDoubles());
        assertEquals(expected.isGameOver(), actual.isGameOver());
        assertEquals(expected.getPlayers().size(), actual.getPlayers().size());
        for (int index = 0; index < expected.getPlayers().size(); index++) {
            final Player before = expected.getPlayers().get(index);
            final Player after = actual.getPlayers().get(index);
            assertEquals(before.getName(), after.getName());
            assertEquals(before.getToken(), after.getToken());
            assertEquals(before.getMoney(), after.getMoney(), before.getName());
            assertEquals(before.getPosition(), after.getPosition(), before.getName());
            assertEquals(before.getStatus(), after.getStatus(), before.getName());
            assertEquals(names(before.getProperties()), names(after.getProperties()), before.getName());
            if (before.isInJail()) {
                assertEquals(expected.getContext().getJail().getFailedAttempts(before),
                        actual.getContext().getJail().getFailedAttempts(after), before.getName());
            }
        }
        for (int position = 0; position < expected.getBoard().getSize(); position++) {
            if (expected.getBoard().getTileAt(position) instanceof Property property) {
                assertEquals(property.getOwner().map(Player::getName),
                        propertyAt(actual, position).getOwner().map(Player::getName), property.getName());
            }
        }
    }

    private static List<Property> ownedProperties(final GameState state) {
        return state.getPlayers().stream().flatMap(player -> player.getProperties().stream()).toList();
    }

    private static List<String> names(final List<Property> properties) {
        return properties.stream().map(Property::getName).toList();
    }

    private static Property propertyAt(final GameState state, final int position) {
        return (Property) state.getBoard().getTileAt(position);
    }
}
