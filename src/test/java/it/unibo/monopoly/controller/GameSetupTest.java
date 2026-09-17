package it.unibo.monopoly.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.economy.Bank;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;
import it.unibo.monopoly.model.player.TokenCatalog;

/**
 * Test della configurazione di inizio partita.
 * <p>
 * Sono i controlli che la schermata di setup fa prima di lasciar partire il gioco, ma
 * qui nessuna finestra viene aperta: e' proprio il motivo per cui le regole stanno in
 * {@link GameSetup} e non dentro la view.
 */
class GameSetupTest {

    /** Nome usato quando il nome non e' l'oggetto del test. */
    private static final String ANY_NAME = "Giocatore";

    // ------------------------------------------------------------------
    // Numero di giocatori
    // ------------------------------------------------------------------

    @Test
    void aValidConfigurationHasNoProblems() {
        final GameSetup setup = setupOf(ANY_NAME, "Marco", "Giulia");

        assertTrue(setup.validate().isEmpty());
        assertTrue(setup.isValid());
    }

    @Test
    void oneSinglePlayerIsNotEnough() {
        final GameSetup setup = setupOf("Marco");

        assertEquals(List.of(SetupProblem.TOO_FEW_PLAYERS), setup.validate());
        assertFalse(setup.isValid());
    }

    @Test
    void anEmptyConfigurationIsRejected() {
        final GameSetup setup = new GameSetup(List.of());

        assertEquals(List.of(SetupProblem.TOO_FEW_PLAYERS), setup.validate());
    }

    @Test
    void theMinimumAndTheMaximumNumberOfPlayersAreBothAccepted() {
        assertTrue(withPlayers(GameState.MIN_PLAYERS).isValid(), "il minimo deve bastare");
        assertTrue(withPlayers(GameState.MAX_PLAYERS).isValid(), "il massimo deve essere ammesso");
    }

    @Test
    void tooManyPlayersAreRejected() {
        // Una pedina a testa non basterebbe: serve un doppione per arrivare a nove.
        final List<PlayerSetup> tooMany = new ArrayList<>(
                withPlayers(GameState.MAX_PLAYERS).getPlayers());
        tooMany.add(new PlayerSetup("Uno di troppo", TokenCatalog.defaultTokenFor(0)));

        final List<SetupProblem> problems = new GameSetup(tooMany).validate();

        assertTrue(problems.contains(SetupProblem.TOO_MANY_PLAYERS));
    }

    // ------------------------------------------------------------------
    // Nomi
    // ------------------------------------------------------------------

    @Test
    void anEmptyNameIsRejected() {
        final GameSetup setup = setupOf("Marco", "");

        assertEquals(List.of(SetupProblem.EMPTY_NAME), setup.validate());
    }

    @Test
    void aNameMadeOnlyOfSpacesIsRejected() {
        final GameSetup setup = setupOf("Marco", "   ");

        assertEquals(List.of(SetupProblem.EMPTY_NAME), setup.validate());
    }

    @Test
    void aMissingNameIsRejected() {
        final GameSetup setup = setupOf("Marco", null);

        assertEquals(List.of(SetupProblem.EMPTY_NAME), setup.validate());
    }

    @Test
    void severalEmptyNamesAreReportedOnce() {
        final GameSetup setup = setupOf("", " ", "Marco");

        // Un elenco da mostrare all'utente: lo stesso problema non si ripete tre volte.
        assertEquals(List.of(SetupProblem.EMPTY_NAME), setup.validate());
    }

    // ------------------------------------------------------------------
    // Pedine
    // ------------------------------------------------------------------

    @Test
    void twoPlayersCannotShareTheSameToken() {
        final Token shared = TokenCatalog.defaultTokenFor(0);
        final GameSetup setup = new GameSetup(List.of(
                new PlayerSetup("Marco", shared),
                new PlayerSetup("Giulia", shared)));

        assertEquals(List.of(SetupProblem.DUPLICATE_TOKEN), setup.validate());
    }

    @Test
    void twoEqualTokensAreDuplicatesEvenIfTheyAreNotTheSameObject() {
        // Token confronta nome e colore, non l'identita': due copie sono la stessa pedina.
        final GameSetup setup = new GameSetup(List.of(
                new PlayerSetup("Marco", new Token("Cappello", "RED")),
                new PlayerSetup("Giulia", new Token("Cappello", "RED"))));

        assertEquals(List.of(SetupProblem.DUPLICATE_TOKEN), setup.validate());
    }

    @Test
    void aPlayerWithoutATokenIsRejected() {
        final GameSetup setup = new GameSetup(List.of(
                new PlayerSetup("Marco", TokenCatalog.defaultTokenFor(0)),
                new PlayerSetup("Giulia", null)));

        assertEquals(List.of(SetupProblem.MISSING_TOKEN), setup.validate());
    }

    @Test
    void allTheProblemsAreReportedTogether() {
        final Token shared = TokenCatalog.defaultTokenFor(0);
        final GameSetup setup = new GameSetup(List.of(
                new PlayerSetup("", shared),
                new PlayerSetup("Giulia", shared)));

        // Chi ha sbagliato due cose le vede tutte e due in una volta sola.
        assertEquals(List.of(SetupProblem.EMPTY_NAME, SetupProblem.DUPLICATE_TOKEN), setup.validate());
    }

    // ------------------------------------------------------------------
    // Creazione dei giocatori
    // ------------------------------------------------------------------

    @Test
    void aValidConfigurationBecomesPlayersInTheSameOrder() {
        final GameSetup setup = new GameSetup(List.of(
                new PlayerSetup("Marco", TokenCatalog.defaultTokenFor(2)),
                new PlayerSetup("Giulia", TokenCatalog.defaultTokenFor(0))));

        final List<Player> players = setup.createPlayers();

        assertEquals(List.of("Marco", "Giulia"), players.stream().map(Player::getName).toList());
        assertEquals(TokenCatalog.defaultTokenFor(2), players.get(0).getToken());
        assertEquals(TokenCatalog.defaultTokenFor(0), players.get(1).getToken());
        // Giocatori nuovi di zecca: soldi iniziali e casella di partenza.
        assertTrue(players.stream().allMatch(player -> player.getMoney() == Bank.STARTING_BALANCE));
        assertTrue(players.stream().allMatch(Player::isPlaying));
    }

    @Test
    void theSpacesAroundTheNameAreRemoved() {
        final GameSetup setup = setupOf("  Marco  ", "Giulia");

        assertEquals("Marco", setup.createPlayers().get(0).getName());
    }

    @Test
    void anInvalidConfigurationCannotBecomeAGame() {
        final GameSetup setup = setupOf("Marco");

        assertThrows(IllegalStateException.class, setup::createPlayers);
    }

    @Test
    void thePlayersOfAValidSetupAreAcceptedByTheGame() {
        // La prova finale: i giocatori costruiti qui devono andare bene alla partita
        // vera, che ha gli stessi limiti su quanti possono essere.
        final List<Player> players = withPlayers(GameState.MAX_PLAYERS).createPlayers();

        assertEquals(GameState.MAX_PLAYERS, new GameEngine(players).getState().getPlayers().size());
    }

    // ------------------------------------------------------------------
    // Controlli sull'elenco ricevuto
    // ------------------------------------------------------------------

    @Test
    void aMissingOrBrokenListIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> new GameSetup(null));
        assertThrows(IllegalArgumentException.class, () -> new GameSetup(Arrays.asList(
                new PlayerSetup("Marco", TokenCatalog.defaultTokenFor(0)), null)));
    }

    // ------------------------------------------------------------------
    // Metodi di supporto
    // ------------------------------------------------------------------

    /** Una configurazione con i nomi indicati e una pedina diversa per ciascuno. */
    private static GameSetup setupOf(final String... names) {
        final List<PlayerSetup> players = new ArrayList<>(names.length);
        for (int index = 0; index < names.length; index++) {
            players.add(new PlayerSetup(names[index], TokenCatalog.defaultTokenFor(index)));
        }
        return new GameSetup(players);
    }

    /** Una configurazione valida con il numero di giocatori indicato. */
    private static GameSetup withPlayers(final int howMany) {
        final String[] names = new String[howMany];
        Arrays.setAll(names, index -> ANY_NAME + " " + (index + 1));
        return setupOf(names);
    }
}
