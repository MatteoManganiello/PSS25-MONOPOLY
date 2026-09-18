package it.unibo.monopoly;

import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.TokenCatalog;
import it.unibo.monopoly.view.ConsoleGameObserver;
import it.unibo.monopoly.view.MainWindow;
import it.unibo.monopoly.view.SetupWindow;

/**
 * Da qui parte tutto il programma.
 * <p>
 * Qui si montano insieme i tre pezzi dell'MVC: i giocatori (model), il motore della
 * partita (controller) e le view che lo guardano. La view principale e' la finestra
 * grafica {@link MainWindow}: si gioca cliccando "Tira i dadi".
 * <p>
 * Il gioco non parte pero' subito: prima si apre la {@link SetupWindow}, dove si
 * decide chi gioca e con che pedina. Il percorso e' sempre lo stesso -
 * <b>setup, conferma, partita, e di nuovo setup</b> - e lo comanda questa classe: la
 * schermata di setup si limita a consegnare i giocatori scelti, senza sapere cosa ne
 * verra' fatto, ed e' qui che nascono il motore e la finestra di gioco; la finestra di
 * gioco, a partita finita, si limita a chiudersi e ad avvisare, ed e' qui che si
 * decide di riaprire la schermata iniziale. Nessuna delle due finestre conosce
 * l'altra: il collegamento lo fanno i metodi di questa classe.
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

    /** Prepara l'aspetto delle finestre e apre la prima schermata di setup. */
    private static void startGraphicalGame() {
        applySystemLookAndFeel();
        showSetup();
    }

    /**
     * Apre la schermata di setup: da qui parte ogni partita, la prima e quelle dopo. La
     * partita vera partira' solo quando l'utente avra' confermato i giocatori: a quel
     * punto la schermata chiama {@link #startGameWith(List)}.
     */
    private static void showSetup() {
        new SetupWindow(MonopolyApp::startGameWith).setVisible(true);
    }

    /**
     * Avvia la partita con i giocatori scelti nella schermata di setup e apre la
     * finestra di gioco.
     *
     * @param players i giocatori configurati dall'utente, in ordine di turno
     */
    private static void startGameWith(final List<Player> players) {
        // 1. Controller: il motore costruisce da se' il tabellone standard.
        final GameEngine engine = new GameEngine(players);

        // 2. View: la finestra si registra da sola come osservatrice del motore. A
        //    partita finita si chiude e si torna alla schermata di setup.
        final MainWindow window = new MainWindow(engine, MonopolyApp::showSetup);
        // Seconda view sugli stessi eventi: il racconto sul terminale.
        engine.addObserver(new ConsoleGameObserver());
        window.setVisible(true);

        // 3. Da qui in avanti comanda l'utente, un clic per volta.
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
     * I giocatori della demo testuale, che non ha una schermata di setup da cui
     * prenderli. Le pedine sono le prime del {@link TokenCatalog}, cioe' le stesse
     * proposte dalla schermata: cosi' l'elenco delle pedine del gioco resta scritto
     * in un posto solo.
     *
     * @return tre giocatori con nomi e pedine fissi
     */
    private static List<Player> createDemoPlayers() {
        return List.of(
                new Player("Alice", TokenCatalog.defaultTokenFor(0)),
                new Player("Bob", TokenCatalog.defaultTokenFor(1)),
                new Player("Carol", TokenCatalog.defaultTokenFor(2)));
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
