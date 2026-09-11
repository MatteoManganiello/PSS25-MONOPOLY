package monopoly.view;

import java.io.PrintStream;

import monopoly.controller.GameEngine;
import monopoly.controller.GameObserver;
import monopoly.model.board.Tile;
import monopoly.model.game.GameState;
import monopoly.model.game.RollOutcome;
import monopoly.model.game.RollResult;
import monopoly.model.player.Player;

/**
 * View testuale: stampa su console gli eventi della partita.
 * <p>
 * E' un esempio concreto di {@link GameObserver}: riceve le notifiche dal
 * {@link GameEngine} e si limita a mostrarle, senza conoscere le regole e senza
 * modificare il model. La GUI del Giorno 4 sara' semplicemente un altro
 * {@code GameObserver} che, invece di stampare, aggiorna i componenti grafici:
 * il motore non dovra' cambiare.
 * <p>
 * Non ridefinisce {@link GameObserver#onGameStateChanged}: le notifiche puntuali
 * le bastano, e il metodo di default (vuoto) viene ereditato dall'interfaccia.
 */
public class ConsoleGameObserver implements GameObserver {

    private final PrintStream out;

    /** Crea una view che scrive sullo standard output. */
    public ConsoleGameObserver() {
        this(System.out);
    }

    /**
     * Crea una view che scrive sullo stream indicato (utile per reindirizzare l'output).
     *
     * @param out lo stream su cui stampare
     * @throws IllegalArgumentException se lo stream e' null
     */
    public ConsoleGameObserver(final PrintStream out) {
        if (out == null) {
            throw new IllegalArgumentException("Lo stream di output non puo' essere null");
        }
        this.out = out;
    }

    @Override
    public void onGameStarted(final GameState state) {
        this.out.println("=== Partita iniziata con " + state.getPlayers().size() + " giocatori ===");
        for (final Player player : state.getPlayers()) {
            this.out.println("  " + player.getName() + " (pedina: " + player.getToken().getName() + ")");
        }
    }

    @Override
    public void onTurnStarted(final Player player) {
        this.out.println();
        this.out.println("--- Turno di " + player.getName() + (player.isInJail() ? " (in prigione)" : "") + " ---");
    }

    @Override
    public void onDiceRolled(final RollResult result) {
        this.out.println(result.player().getName() + " lancia i dadi: "
                + result.firstDie() + " + " + result.secondDie() + " = " + result.total()
                + describe(result.outcome()));
        if (result.passedGo()) {
            this.out.println("  ...e passa dal Via!");
        }
    }

    @Override
    public void onPlayerMoved(final Player player, final Tile from, final Tile to) {
        this.out.println("  " + player.getName() + " si sposta da \"" + from.getName()
                + "\" a \"" + to.getName() + "\"");
    }

    @Override
    public void onPlayerSentToJail(final Player player) {
        this.out.println("  " + player.getName() + " va in prigione!");
    }

    @Override
    public void onGameOver(final Player winner) {
        this.out.println();
        this.out.println("=== Partita finita: vince " + winner.getName() + " ===");
    }

    /**
     * Lo switch sull'enum e' esaustivo: se in futuro si aggiunge un nuovo esito,
     * il compilatore obbliga ad aggiornare anche questo metodo.
     */
    private static String describe(final RollOutcome outcome) {
        return switch (outcome) {
            case MOVED -> "";
            case ROLL_AGAIN -> " -> doppio, lancia di nuovo";
            case SENT_TO_JAIL -> " -> terzo doppio consecutivo";
            case RELEASED_FROM_JAIL -> " -> doppio, esce di prigione";
            case STAYED_IN_JAIL -> " -> niente doppio, resta in prigione";
        };
    }
}
