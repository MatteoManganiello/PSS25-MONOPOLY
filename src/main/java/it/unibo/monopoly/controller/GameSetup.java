package it.unibo.monopoly.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;

/**
 * La configurazione con cui si sta per iniziare una partita: chi gioca e con che
 * pedina.
 * <p>
 * E' il pezzo che sta fra la schermata di setup e il {@link GameEngine}. La
 * schermata raccoglie i dati e li mette qui dentro come {@link PlayerSetup}; questa
 * classe risponde a due domande, e solo a quelle:
 * <ol>
 *   <li>«questi dati vanno bene?» ({@link #validate()});</li>
 *   <li>«allora costruiscimi i giocatori» ({@link #createPlayers()}).</li>
 * </ol>
 * <b>Perche' una classe a parte e non codice dentro la finestra.</b> Le regole
 * (quanti giocatori, nomi non vuoti, pedine tutte diverse) sono regole del gioco, non
 * dettagli grafici: scritte qui si possono provare con JUnit senza aprire nessuna
 * finestra, e domani una partita avviata da riga di comando o da un file di
 * configurazione userebbe le stesse identiche regole. La finestra, dal canto suo,
 * non decide piu' niente: chiede, e mostra la risposta.
 * <p>
 * I limiti sul numero di giocatori non sono riscritti qui: sono
 * {@link GameState#MIN_PLAYERS} e {@link GameState#MAX_PLAYERS}, cioe' gli stessi
 * che la partita fara' rispettare comunque. Questa classe li controlla <em>prima</em>
 * solo per poter avvisare l'utente con un messaggio invece che con un'eccezione.
 * <p>
 * L'oggetto non e' modificabile: una volta creato descrive una configurazione e basta.
 */
public final class GameSetup {

    private final List<PlayerSetup> players;

    /**
     * Crea una configurazione a partire dalle righe compilate dall'utente.
     * <p>
     * Qui non si controlla ancora niente sul <em>contenuto</em>: righe con il nome
     * vuoto o senza pedina sono ammesse, ed e' proprio il caso che {@link #validate()}
     * deve saper segnalare. Si rifiuta solo cio' che sarebbe un errore di
     * programmazione, cioe' una lista inesistente o con dei buchi.
     *
     * @param players le righe compilate, in ordine di turno
     * @throws IllegalArgumentException se la lista e' null o contiene elementi null
     */
    public GameSetup(final List<PlayerSetup> players) {
        if (players == null) {
            throw new IllegalArgumentException("L'elenco dei giocatori da configurare non puo' essere null");
        }
        // anyMatch e non contains(null): su una lista immutabile contains(null) esplode.
        if (players.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("L'elenco dei giocatori da configurare non puo' contenere null");
        }
        // Copia difensiva: da qui in poi la configurazione non cambia piu'.
        this.players = List.copyOf(players);
    }

    /** @return le righe della configurazione, in sola lettura */
    public List<PlayerSetup> getPlayers() {
        return this.players;
    }

    /**
     * Controlla la configurazione e dice tutto quello che non va.
     * <p>
     * Restituisce <em>tutti</em> i problemi e non solo il primo, cosi' chi ha
     * sbagliato due cose le vede tutte e due insieme invece di scoprirle una alla
     * volta. Ogni problema compare una volta sola anche se riguarda piu' righe (tre
     * nomi vuoti sono comunque un solo {@link SetupProblem#EMPTY_NAME}), e l'ordine e'
     * sempre lo stesso: e' un elenco da mostrare, non un insieme sparso.
     *
     * @return i problemi trovati, in una lista vuota se la configurazione va bene
     */
    public List<SetupProblem> validate() {
        // LinkedHashSet: niente doppioni, ma l'ordine in cui li abbiamo trovati resta.
        final Set<SetupProblem> problems = new LinkedHashSet<>();

        if (this.players.size() < GameState.MIN_PLAYERS) {
            problems.add(SetupProblem.TOO_FEW_PLAYERS);
        }
        if (this.players.size() > GameState.MAX_PLAYERS) {
            problems.add(SetupProblem.TOO_MANY_PLAYERS);
        }

        final Set<Token> alreadyChosen = new HashSet<>();
        for (final PlayerSetup player : this.players) {
            if (player.name() == null || player.name().isBlank()) {
                problems.add(SetupProblem.EMPTY_NAME);
            }
            if (player.token() == null) {
                problems.add(SetupProblem.MISSING_TOKEN);
            } else if (!alreadyChosen.add(player.token())) {
                // add() restituisce false se la pedina c'era gia': e' il doppione.
                problems.add(SetupProblem.DUPLICATE_TOKEN);
            }
        }
        return List.copyOf(problems);
    }

    /** @return true se con questa configurazione si puo' iniziare a giocare */
    public boolean isValid() {
        return this.validate().isEmpty();
    }

    /**
     * Costruisce i giocatori veri della partita, nell'ordine in cui sono stati
     * configurati (che diventa l'ordine dei turni).
     * <p>
     * Gli spazi in piu' attorno al nome vengono tolti: "  Marco " e "Marco" sono la
     * stessa persona, e nessuno vuole vedere il proprio nome disallineato sul
     * tabellone.
     *
     * @return i giocatori pronti da passare a {@link GameEngine}
     * @throws IllegalStateException se la configurazione non e' valida: chi chiama
     *                               deve prima passare da {@link #validate()} e
     *                               avvisare l'utente
     */
    public List<Player> createPlayers() {
        final List<SetupProblem> problems = this.validate();
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Non si puo' iniziare: " + problems);
        }
        final List<Player> created = new ArrayList<>(this.players.size());
        for (final PlayerSetup player : this.players) {
            created.add(new Player(player.name().trim(), player.token()));
        }
        return List.copyOf(created);
    }

    @Override
    public String toString() {
        return "GameSetup[players=" + this.players + "]";
    }
}
