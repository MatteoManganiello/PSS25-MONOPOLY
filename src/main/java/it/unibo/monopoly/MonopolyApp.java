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
 * Punto di ingresso dell'applicazione.
 * <p>
 * Qui si "montano" i tre strati MVC: si creano i giocatori (model), il motore della
 * partita (controller) e le view che lo osservano. Dal GIORNO 4 la view principale e'
 * la finestra grafica {@link MainWindow}: la partita non viene piu' giocata in
 * automatico, la si gioca cliccando "Tira i dadi".
 * <p>
 * Insieme alla GUI viene registrata anche la vecchia view testuale
 * {@link ConsoleGameObserver}, che continua a raccontare la partita sul terminale.
 * Non e' un doppione inutile: e' la dimostrazione pratica del pattern Observer -
 * due view diverse, sugli stessi eventi, senza che il motore sappia nulla ne'
 * dell'una ne' dell'altra.
 * <p>
 * Con l'argomento {@code --console} si avvia invece la demo automatica del Giorno 3,
 * utile per vedere scorrere molti turni senza cliccare.
 */
public final class MonopolyApp {

    /** Turni giocati dalla demo testuale prima di fermarsi e stampare il riepilogo. */
    private static final int DEMO_TURNS = 20;

    /** Argomento che avvia la demo testuale al posto dell'interfaccia grafica. */
    private static final String CONSOLE_OPTION = "--console";

    /** Classe di utilita': non deve essere istanziata. */
    private MonopolyApp() {
    }

    /**
     * Avvia l'applicazione: la finestra di gioco oppure, con {@code --console}, la
     * demo testuale.
     *
     * @param args argomenti da riga di comando
     */
    public static void main(final String[] args) {
        if (args.length > 0 && CONSOLE_OPTION.equals(args[0])) {
            runConsoleDemo();
            return;
        }
        // I componenti Swing vanno creati e usati sull'Event Dispatch Thread.
        SwingUtilities.invokeLater(MonopolyApp::startGraphicalGame);
    }

    /** Prepara la partita di prova e apre la finestra di gioco. */
    private static void startGraphicalGame() {
        applySystemLookAndFeel();

        // 1. Model: i giocatori della partita di prova.
        final GameEngine engine = new GameEngine(createDemoPlayers());

        // 2. View: la finestra si registra da sola come osservatore del motore.
        final MainWindow window = new MainWindow(engine);
        // Seconda view sugli stessi eventi: la cronaca sul terminale.
        engine.addObserver(new ConsoleGameObserver());
        window.setVisible(true);

        // 3. Controller: da qui in poi comanda l'utente, un clic alla volta.
        engine.startGame();
    }

    /** Gioca da sola qualche turno e stampa il riepilogo: la demo del Giorno 3. */
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
     * @return i giocatori della partita di prova, con pedine di colori diversi
     *         (il colore e' una stringa: e' la view a tradurlo in un colore vero)
     */
    private static List<Player> createDemoPlayers() {
        return List.of(
                new Player("Alice", new Token("Car", "RED")),
                new Player("Bob", new Token("Dog", "BLUE")),
                new Player("Carol", new Token("Hat", "GREEN")));
    }

    /**
     * Prova a usare l'aspetto grafico del sistema operativo.
     * <p>
     * E' solo una rifinitura: se non e' disponibile si continua con l'aspetto di
     * default di Swing, senza disturbare l'utente con un errore.
     */
    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException ignored) {
            // Aspetto di default: la partita si gioca lo stesso.
        }
    }
}
