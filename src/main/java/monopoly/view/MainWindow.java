package monopoly.view;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import monopoly.controller.GameEngine;
import monopoly.controller.GameObserver;
import monopoly.model.board.Tile;
import monopoly.model.game.GameState;
import monopoly.model.game.RollResult;
import monopoly.model.player.Player;

/**
 * Finestra principale della GUI: mette insieme i pannelli e fa da ponte fra il
 * model e l'interfaccia.
 * <p>
 * <b>Composizione.</b> Il layout segue la disposizione classica di un gioco da
 * tavolo: il {@link BoardPanel} al centro, il {@link PlayerInfoPanel} sul lato
 * destro e il {@link ControlPanel} in basso. Ogni pannello ha una sola
 * responsabilita' e non conosce gli altri: e' la finestra a comporli e a dire a
 * ciascuno quando aggiornarsi.
 * <p>
 * <b>Collegamento con il model.</b> La finestra implementa {@link GameObserver} e si
 * registra sul {@link GameEngine}: e' l'unico punto in cui gli eventi della partita
 * entrano nella GUI. Quando il motore annuncia qualcosa (un movimento, un cambio di
 * turno, un fallimento), la finestra chiede ai pannelli di rileggere il
 * {@link GameState} e ridisegnarsi. Il motore, dal canto suo, non sa che dall'altra
 * parte c'e' Swing: conosce solo l'interfaccia {@code GameObserver}.
 * <p>
 * <b>Due osservatori, non uno.</b> Oltre a se' stessa, la finestra registra un
 * secondo osservatore che scrive la cronaca testuale nell'area di log del
 * {@link ControlPanel}. E' una sottoclasse anonima di {@link TextGameObserver}, la
 * stessa classe base usata dalla view su console: cosi' la parte grafica si occupa
 * solo di disegnare e le frasi da mostrare restano scritte in un posto solo.
 * <p>
 * <b>Un solo verso.</b> Da qui non parte mai una modifica al model: i comandi
 * dell'utente passano dal {@link ControlPanel} al {@link GameEngine}, e tornano
 * indietro sotto forma di eventi. Gli oggetti del model che arrivano nei metodi
 * osservatore vengono solo letti.
 * <p>
 * <b>Thread.</b> Tutti i comandi partono da un pulsante, quindi dall'Event Dispatch
 * Thread di Swing; il motore notifica gli osservatori durante quella stessa
 * chiamata, quindi anche gli aggiornamenti grafici avvengono sull'EDT, come Swing
 * richiede. Per questo la finestra va creata dentro
 * {@link SwingUtilities#invokeLater(Runnable)} (lo fa {@link monopoly.MonopolyApp
 * MonopolyApp}) e non serve nessuna sincronizzazione.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class MainWindow extends JFrame implements GameObserver {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Titolo della finestra. */
    private static final String TITLE = "Monopoly - PSS25";

    private final transient GameEngine engine;
    private final BoardPanel boardPanel;
    private final PlayerInfoPanel playerInfoPanel;
    private final ControlPanel controlPanel;

    /** Cronaca testuale della partita, riversata nell'area di log dei comandi. */
    private final transient TextGameObserver logObserver;

    /**
     * Costruisce la finestra sulla partita gestita dal motore indicato e la registra
     * come osservatore.
     *
     * @param engine il motore della partita, unico canale verso il model
     * @throws IllegalArgumentException se il motore e' null
     */
    public MainWindow(final GameEngine engine) {
        super(TITLE);
        if (engine == null) {
            throw new IllegalArgumentException("Il motore della partita non puo' essere null");
        }
        this.engine = engine;
        final GameState state = engine.getState();
        this.boardPanel = new BoardPanel(state);
        this.playerInfoPanel = new PlayerInfoPanel(state);
        this.controlPanel = new ControlPanel(engine);
        // Sottoclasse anonima: riusa tutte le frasi di TextGameObserver e cambia solo
        // la destinazione, che qui e' l'area di testo dei comandi.
        this.logObserver = new TextGameObserver() {
            @Override
            protected void write(final String line) {
                MainWindow.this.controlPanel.appendLog(line);
            }
        };

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(6, 6));
        this.add(this.boardPanel, BorderLayout.CENTER);
        this.add(this.playerInfoPanel, BorderLayout.EAST);
        this.add(this.controlPanel, BorderLayout.SOUTH);
        this.pack();
        this.limitToScreen();
        this.setLocationRelativeTo(null);

        // Da questo momento la GUI riceve gli eventi della partita. Registrarsi nel
        // costruttore, prima di startGame(), fa arrivare anche l'evento di avvio.
        engine.addObserver(this);
        engine.addObserver(this.logObserver);
    }

    // ------------------------------------------------------------------
    // Eventi della partita (pattern Observer)
    // ------------------------------------------------------------------

    /**
     * Disegna la situazione iniziale.
     *
     * @param state lo stato iniziale della partita
     */
    @Override
    public void onGameStarted(final GameState state) {
        this.refreshAll();
    }

    /**
     * Aggiorna l'indicazione del giocatore di turno e i comandi disponibili.
     *
     * @param player il nuovo giocatore di turno
     */
    @Override
    public void onTurnStarted(final Player player) {
        this.refreshAll();
    }

    /**
     * Mostra i dadi appena lanciati.
     *
     * @param result il risultato del lancio
     */
    @Override
    public void onDiceRolled(final RollResult result) {
        this.controlPanel.showRoll(result);
    }

    /**
     * Sposta la pedina sul tabellone.
     * <p>
     * Il ridisegno completo arrivera' comunque con {@code onGameStateChanged}: qui la
     * pedina viene spostata subito perche' e' l'evento che riguarda direttamente il
     * tabellone, ed e' cosi' che i pannelli restano indipendenti fra loro.
     *
     * @param player il giocatore che si e' mosso
     * @param from   la casella di partenza
     * @param to     la casella di arrivo
     */
    @Override
    public void onPlayerMoved(final Player player, final Tile from, final Tile to) {
        this.boardPanel.refresh();
    }

    /**
     * Ridisegna tutto: e' l'evento generico inviato al termine di ogni comando, e
     * copre anche i cambiamenti che non hanno un evento dedicato.
     *
     * @param state lo stato aggiornato della partita
     */
    @Override
    public void onGameStateChanged(final GameState state) {
        this.refreshAll();
    }

    /**
     * Chiude la partita: aggiorna i pannelli (i comandi si disabilitano da soli,
     * perche' il motore non consente piu' nessuna azione) e annuncia il vincitore.
     *
     * @param winner il giocatore rimasto in partita
     */
    @Override
    public void onGameOver(final Player winner) {
        this.refreshAll();
        this.logObserver.printStandings(this.engine.getState());
        // La finestra di dialogo e' modale: mostrarla piu' tardi lascia prima finire
        // la notifica in corso, evitando di bloccare il motore a meta' di un comando.
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                "Vince " + winner.getName() + " con " + ViewStyle.formatMoney(winner.getMoney()) + " in cassa!",
                "Partita finita",
                JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Riduce la finestra se e' piu' grande dello spazio utile dello schermo.
     * <p>
     * Il tabellone e' costruito con pesi uguali su tutte le celle, quindi rimpicciolire
     * la finestra rimpicciolisce le caselle in modo uniforme invece di tagliarle: su un
     * portatile piccolo il tabellone resta tutto visibile.
     */
    private void limitToScreen() {
        final Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        this.setSize(Math.min(this.getWidth(), usable.width), Math.min(this.getHeight(), usable.height));
    }

    /** Chiede a tutti i pannelli di rileggere lo stato della partita e ridisegnarsi. */
    private void refreshAll() {
        this.boardPanel.refresh();
        this.playerInfoPanel.refresh();
        this.controlPanel.refresh();
    }
}
