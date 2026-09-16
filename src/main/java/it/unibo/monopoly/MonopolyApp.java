package it.unibo.monopoly;

import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;
import it.unibo.monopoly.view.ConsoleGameObserver;
import it.unibo.monopoly.view.MainWindow;

/**
 * Da qui parte tutto il programma.
 * <p>
 * Qui si montano insieme i tre pezzi dell'MVC: i giocatori (model), il motore della
 * partita (controller) e le view che lo guardano. La view principale e' la finestra
 * grafica {@link MainWindow}: si gioca cliccando "Tira i dadi".
 * <p>
 * Insieme alla finestra registriamo anche la vecchia view testuale
 * {@link ConsoleGameObserver}, che racconta la partita sul terminale. Non e' un
 * doppione inutile: fa vedere il pattern Observer all'opera, cioe' due view diverse
 * sugli stessi eventi senza che il motore sappia niente ne' dell'una ne' dell'altra.
 * <p>
 * Con l'argomento {@code --console} parte invece la demo automatica, comoda per veder
 * scorrere tanti turni senza cliccare.
 */
public final class MonopolyApp {

    /** Quanti turni gioca la demo testuale prima di fermarsi e stampare il riepilogo. */
    private static final int DEMO_TURNS = 20;

    /** L'argomento che fa partire la demo testuale invece della finestra. */
    private static final String CONSOLE_OPTION = "--console";

    /** Costruttore privato: questa classe ha solo metodi statici, non va creata. */
    private MonopolyApp() {
    }

    /**
     * Fa partire il programma: la finestra di gioco, oppure la demo testuale se si passa
     * {@code --console}.
     *
     * @param args gli argomenti scritti da riga di comando
     */
    public static void main(final String[] args) {
        if (args.length > 0 && CONSOLE_OPTION.equals(args[0])) {
            runConsoleDemo();
            return;
        }
        // Swing vuole che le finestre si creino e si usino sull'Event Dispatch Thread.
        SwingUtilities.invokeLater(MonopolyApp::startGraphicalGame);
    }

    /** Prepara la partita di prova e apre la finestra. */
    private static void startGraphicalGame() {
        applySystemLookAndFeel();

        // 1. Model: i giocatori di prova.
        final GameEngine engine = new GameEngine(createDemoPlayers());

        // 2. View: la finestra si registra da sola come osservatrice del motore.
        final MainWindow window = new MainWindow(engine);
        // Seconda view sugli stessi eventi: il racconto sul terminale.
        engine.addObserver(new ConsoleGameObserver());
        window.setVisible(true);

        // 3. Controller: da qui in avanti comanda l'utente, un clic per volta.
        engine.startGame();
    }

    /** La demo automatica: gioca da sola qualche turno e stampa il riepilogo. */
    private static void runConsoleDemo() {
        final GameEngine engine = new GameEngine(createDemoPlayers());
        final ConsoleGameObserver view = new ConsoleGameObserver();
        engine.addObserver(view);
        engine.startGame();

        for (int turn = 0; turn < DEMO_TURNS && !engine.isGameOver(); turn++) {
            engine.playTurn();
        }
        view.printStandings(engine.getState());
    }

    /**
     * @return i giocatori di prova, con pedine di colori diversi. Il colore e' scritto
     *         come stringa: sara' la view a trasformarlo in un colore vero
     */
    private static List<Player> createDemoPlayers() {
        return List.of(
                new Player("Alice", new Token("Car", "RED")),
                new Player("Bob", new Token("Dog", "BLUE")),
                new Player("Carol", new Token("Hat", "GREEN")));
    }

    /**
     * Prova a far sembrare la finestra un programma del sistema operativo.
     * <p>
     * E' solo un abbellimento: se non funziona teniamo l'aspetto normale di Swing senza
     * far vedere nessun errore all'utente.
     */
    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException ignored) {
            // Pazienza, resta l'aspetto normale: si gioca lo stesso.
        }
    }
}
