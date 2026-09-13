package monopoly;

import java.util.List;

import monopoly.controller.GameEngine;
import monopoly.model.player.Player;
import monopoly.model.player.Token;
import monopoly.view.ConsoleGameObserver;

/**
 * Punto di ingresso dell'applicazione.
 * <p>
 * Qui si "montano" i tre strati MVC: si creano i giocatori (model), il motore
 * della partita (controller) e una view che lo osserva. GIORNO 3: la view e' ancora
 * testuale ({@link ConsoleGameObserver}) e la partita viene giocata in automatico per
 * qualche turno, ma ora sul tabellone vero: si vedono acquisti, affitti, tasse,
 * stipendi del "Via" e, se qualcuno resta senza soldi, i fallimenti.
 */
public final class MonopolyApp {

    /** Turni giocati dalla demo prima di fermarsi e stampare il riepilogo. */
    private static final int DEMO_TURNS = 20;

    /** Classe di utilita': non deve essere istanziata. */
    private MonopolyApp() {
    }

    /**
     * Avvia una dimostrazione testuale di alcuni turni di gioco.
     *
     * @param args argomenti da riga di comando (non utilizzati)
     */
    public static void main(final String[] args) {
        final List<Player> players = List.of(
                new Player("Alice", new Token("Car", "RED")),
                new Player("Bob", new Token("Dog", "BLUE")),
                new Player("Carol", new Token("Hat", "GREEN")));

        // Il motore prepara da solo tabellone standard, banca e regole (vedi
        // GameState.createStandardGame): all'applicazione basta dire chi gioca.
        final GameEngine engine = new GameEngine(players);
        final ConsoleGameObserver view = new ConsoleGameObserver();
        // La view si registra come osservatore: il motore non sa che si tratta di una console.
        engine.addObserver(view);
        engine.startGame();

        for (int turn = 0; turn < DEMO_TURNS && !engine.isGameOver(); turn++) {
            engine.playTurn();
        }
        view.printStandings(engine.getState());
    }
}
