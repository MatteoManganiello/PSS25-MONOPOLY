package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.controller.GameObserver;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.RollResult;
import it.unibo.monopoly.model.player.Player;

/**
 * Finestra principale della GUI: mette insieme i pannelli e fa da ponte fra il
 * model e l'interfaccia.
 * <p>
 * <b>Composizione.</b> Il layout segue la disposizione classica di un gioco da
 * tavolo: il {@link BoardPanel} al centro, il {@link PlayerInfoPanel} sul lato
 * destro e il {@link ControlPanel} in basso. Il {@link GameMenuBar} con salvataggio
 * e caricamento non occupa spazio nella finestra: su macOS sta nella barra dei menu del
 * sistema (lo decide {@link it.unibo.monopoly.MonopolyApp MonopolyApp} all'avvio), sugli
 * altri sistemi resta la solita barra in cima. Ogni pannello ha una sola
 * responsabilita' e non conosce gli altri: e' la finestra a comporli e a dire a
 * ciascuno quando aggiornarsi. Sotto i
 * pannelli c'e' il verde del tavolo ({@link Theme#TABLE_GREEN}), con gli stessi
 * margini su tutti i lati.
 * <p>
 * <b>Partite caricate.</b> Quando il motore mette in gioco una partita caricata da
 * file, la finestra riceve l'evento di avvio con uno stato nuovo. Non viene ricreata e
 * non si registra di nuovo: sostituisce soltanto il tabellone e le schede dei
 * giocatori, che erano costruiti sulla partita precedente, e poi si ridisegna. Il
 * pannello dei comandi resta, perche' interroga sempre il motore, e con lui resta il log.
 * <p>
 * <b>Fine della partita.</b> Quando resta un solo giocatore la finestra annuncia il
 * vincitore e, appena l'utente chiude l'annuncio, si chiude a sua volta ed esegue
 * l'azione {@code onGameFinished} ricevuta nel costruttore. La finestra non sa cosa
 * succede dopo: e' {@link it.unibo.monopoly.MonopolyApp MonopolyApp} a decidere di
 * riaprire la schermata iniziale, esattamente come e' lei a decidere cosa fare dei
 * giocatori scelti nella {@link SetupWindow}.
 * <p>
 * <b>Collegamento con il model.</b> La finestra implementa {@link GameObserver} e si
 * registra sul {@link GameEngine}: e' l'unico punto in cui gli eventi della partita
 * entrano nella GUI. Non ascolta uno per uno gli eventi di dettaglio (movimento,
 * cambio di turno, acquisto, fallimento...): il motore chiude ogni comando con
 * {@link #onGameStateChanged(GameState)}, e a quel punto la finestra chiede ai
 * pannelli di rileggere il {@link GameState} e ridisegnarsi. Cosi' ogni comando
 * produce un solo ridisegno, e nessun cambiamento resta fuori. Il motore, dal canto
 * suo, non sa che dall'altra parte c'e' Swing: conosce solo l'interfaccia
 * {@code GameObserver}.
 * <p>
 * <b>Due osservatori, non uno.</b> Oltre a se' stessa, la finestra registra un
 * secondo osservatore che scrive la cronaca testuale nell'area di log del
 * {@link ControlPanel} e, a fine partita, il riepilogo finale. E' una sottoclasse
 * anonima di {@link TextGameObserver}, la stessa classe base usata dalla view su
 * console: cosi' la parte grafica si occupa solo di disegnare e le frasi da mostrare
 * restano scritte in un posto solo. Entrambi gli osservatori vengono rimossi dal
 * motore in {@link #dispose()}.
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
 * {@link SwingUtilities#invokeLater(Runnable)} (lo fa {@link it.unibo.monopoly.MonopolyApp
 * MonopolyApp}) e non serve nessuna sincronizzazione. Chi comanda il motore senza
 * passare da un pulsante - per esempio al termine di un caricamento da file in un
 * thread in background - deve inviare i comandi con {@code SwingUtilities.invokeLater}:
 * la finestra lo verifica a ogni aggiornamento e, se la regola non e' rispettata, si
 * ferma subito con un'eccezione invece di corrompere l'interfaccia.
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

    /** Cosa fare quando la partita e' finita e la finestra si e' chiusa. */
    private final transient Runnable onGameFinished;

    /** Tabellone della partita mostrata: cambia quando viene caricata un'altra partita. */
    private BoardPanel boardPanel;

    /** Schede dei giocatori della partita mostrata: cambiano insieme al tabellone. */
    private PlayerInfoPanel playerInfoPanel;

    private final ControlPanel controlPanel;

    /** La partita su cui sono costruiti tabellone e schede dei giocatori. */
    private transient GameState displayedState;

    /** Cronaca testuale della partita, riversata nell'area di log dei comandi. */
    private final transient TextGameObserver logObserver;

    /**
     * Costruisce la finestra sulla partita gestita dal motore indicato e la registra
     * come osservatore.
     *
     * @param engine         il motore della partita, unico canale verso il model
     * @param onGameFinished l'azione da eseguire a partita finita, dopo che l'utente ha
     *                       letto chi ha vinto e la finestra si e' chiusa
     * @throws IllegalArgumentException se il motore o l'azione sono null
     */
    public MainWindow(final GameEngine engine, final Runnable onGameFinished) {
        super(TITLE);
        if (engine == null) {
            throw new IllegalArgumentException("Il motore della partita non puo' essere null");
        }
        if (onGameFinished == null) {
            throw new IllegalArgumentException("Serve un'azione da eseguire a fine partita");
        }
        this.engine = engine;
        this.onGameFinished = onGameFinished;
        final GameState state = engine.getState();
        this.displayedState = state;
        this.boardPanel = new BoardPanel(state);
        this.playerInfoPanel = new PlayerInfoPanel(state);
        this.controlPanel = new ControlPanel(engine);
        // Sottoclasse anonima: riusa tutte le frasi di TextGameObserver e cambia solo
        // la destinazione, che qui e' l'area di testo dei comandi.
        this.logObserver = new TextGameObserver() {
            @Override
            protected void write(final String line) {
                // La scrittura nel log non passa da refreshAll(): il controllo del
                // thread va fatto anche qui, prima di toccare l'area di testo.
                requireEdt();
                MainWindow.this.controlPanel.appendLog(line);
            }

            @Override
            public void onGameOver(final Player winner) {
                // Prima l'annuncio del vincitore, poi il riepilogo: e' l'ordine in cui
                // ha senso leggerli nel log.
                super.onGameOver(winner);
                this.printStandings(MainWindow.this.engine.getState());
            }
        };

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setJMenuBar(new GameMenuBar(engine, this, this.controlPanel::appendLog));
        this.setContentPane(createTable());
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
     * <p>
     * Arriva anche quando il motore mette in gioco una partita caricata da file: la
     * finestra non ridefinisce {@link GameObserver#onGameLoaded(GameState)}, che ricade
     * qui. In quel caso lo stato e' un oggetto nuovo, e prima di ridisegnare vengono
     * sostituiti i pannelli costruiti sulla partita precedente.
     *
     * @param state lo stato iniziale della partita
     */
    @Override
    public void onGameStarted(final GameState state) {
        this.showState(state);
        this.refreshAll();
    }

    /**
     * Mostra i dadi appena lanciati.
     * <p>
     * Come la scrittura nel log, non passa da {@code refreshAll()}: il controllo del
     * thread va fatto anche qui. E' il primo evento di ogni lancio, quindi e' anche il
     * primo punto in cui un comando inviato dal thread sbagliato verrebbe scoperto.
     *
     * @param result il risultato del lancio
     */
    @Override
    public void onDiceRolled(final RollResult result) {
        requireEdt();
        this.controlPanel.showRoll(result);
    }

    /**
     * Ridisegna tutto.
     * <p>
     * E' l'evento generico con cui il motore chiude ogni comando (lancio, fine turno,
     * cauzione): per questo e' l'unico a cui la finestra affida l'aggiornamento dei
     * pannelli, e copre anche i cambiamenti che non hanno un evento dedicato (turno,
     * movimento, denaro, proprieta', prigione, fallimento).
     *
     * @param state lo stato aggiornato della partita
     */
    @Override
    public void onGameStateChanged(final GameState state) {
        this.refreshAll();
    }

    /**
     * Al giocatore viene chiesto se vuole comprare la proprieta' su cui si e' fermato.
     * <p>
     * La finestra non risponde e non blocca niente: si limita a ridisegnare i pannelli,
     * cosi' si accendono "Compra" e "Non comprare" mentre "Tira i dadi" e "Passa il
     * turno" restano spenti. La partita aspetta il clic, ma il model non e' fermo ad
     * aspettare nessuno: e' solo in una fase in cui le altre mosse non sono permesse.
     *
     * @param player   il giocatore a cui tocca decidere
     * @param property la proprieta' che puo' comprare
     * @param price    quanto costa
     */
    @Override
    public void onPurchaseOffered(final Player player, final Property property, final int price) {
        this.refreshAll();
    }

    /**
     * Il giocatore ha risposto: la domanda sparisce e il turno riprende.
     *
     * @param player   il giocatore che ha risposto
     * @param property la proprieta' che gli era stata offerta
     * @param bought   true se l'ha comprata
     */
    @Override
    public void onPurchaseResolved(final Player player, final Property property, final boolean bought) {
        this.refreshAll();
    }

    /**
     * Chiude la partita: aggiorna i pannelli (i comandi si disabilitano da soli,
     * perche' il motore non consente piu' nessuna azione), annuncia il vincitore e,
     * quando l'utente chiude l'annuncio, lascia il posto alla schermata iniziale.
     * Il riepilogo finale lo scrive il log, subito dopo la propria riga di fine partita.
     *
     * @param winner il giocatore rimasto in partita
     */
    @Override
    public void onGameOver(final Player winner) {
        this.refreshAll();
        // La finestra di dialogo e' modale: mostrarla piu' tardi lascia prima finire
        // la notifica in corso, evitando di bloccare il motore a meta' di un comando.
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Vince " + winner.getName() + " con " + ViewStyle.formatMoney(winner.getMoney())
                            + " in cassa!\nSi torna alla schermata iniziale per una nuova partita.",
                    "Partita finita",
                    JOptionPane.INFORMATION_MESSAGE);
            this.leaveFinishedGame();
        });
    }

    /**
     * Chiude la finestra e la scollega dal motore.
     * <p>
     * Senza questo passaggio il {@link GameEngine} continuerebbe a tenere un
     * riferimento alla finestra e al suo log e a notificarli a ogni comando: una
     * finestra gia' chiusa resterebbe in memoria e continuerebbe a ridisegnarsi. La
     * chiusura dalla "X" termina l'intera applicazione, ma a fine partita la finestra si
     * chiude da sola e il programma continua con la schermata iniziale: li' questo
     * passaggio e' indispensabile. Caricare una partita, invece, non passa da qui: la
     * finestra resta la stessa e il motore sposta da solo le registrazioni sulla nuova
     * partita.
     * <p>
     * Chiamarlo piu' volte non e' un problema: rimuovere un osservatore gia' rimosso
     * non ha effetto.
     */
    @Override
    public void dispose() {
        this.engine.removeObserver(this);
        this.engine.removeObserver(this.logObserver);
        super.dispose();
    }

    /**
     * Passa la mano a chi deve continuare dopo la partita, poi chiude la finestra.
     * <p>
     * L'ordine conta: se si chiudesse prima questa, per un istante non resterebbe
     * nessuna finestra aperta, ed e' proprio la condizione in cui Java puo' decidere di
     * terminare il programma. Chiudendo la finestra, {@link #dispose()} la scollega
     * anche dal motore.
     */
    private void leaveFinishedGame() {
        this.onGameFinished.run();
        this.dispose();
    }

    /** @return il fondo della finestra: il verde del tavolo, su cui poggiano i pannelli */
    private static JPanel createTable() {
        final JPanel table = new JPanel(new BorderLayout(Theme.PADDING, Theme.PADDING));
        table.setBackground(Theme.TABLE_GREEN);
        table.setBorder(Theme.padding(Theme.PADDING));
        return table;
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

    /**
     * Mette in finestra i pannelli della partita indicata, se non sono gia' quelli.
     * <p>
     * Tabellone e schede dei giocatori leggono un {@link GameState} preciso, ricevuto
     * alla creazione (con le sue caselle e i suoi giocatori): quando la partita cambia si
     * creano pannelli nuovi al posto dei vecchi. Questi pannelli non sono osservatori,
     * quindi sostituirli non lascia registrazioni appese sul motore. Anche i dadi
     * disegnati appartenevano alla partita precedente, e vengono svuotati.
     */
    private void showState(final GameState state) {
        requireEdt();
        if (state == this.displayedState) {
            return;
        }
        this.remove(this.boardPanel);
        this.remove(this.playerInfoPanel);
        this.boardPanel = new BoardPanel(state);
        this.playerInfoPanel = new PlayerInfoPanel(state);
        this.add(this.boardPanel, BorderLayout.CENTER);
        this.add(this.playerInfoPanel, BorderLayout.EAST);
        this.displayedState = state;
        this.controlPanel.clearRoll();
        this.revalidate();
        this.repaint();
    }

    /** Chiede a tutti i pannelli di rileggere lo stato della partita e ridisegnarsi. */
    private void refreshAll() {
        requireEdt();
        this.boardPanel.refresh();
        this.playerInfoPanel.refresh();
        this.controlPanel.refresh();
    }

    /**
     * Verifica che l'aggiornamento della GUI avvenga sull'Event Dispatch Thread.
     * <p>
     * Swing non e' thread-safe: i componenti vanno toccati solo dall'EDT. I pulsanti lo
     * garantiscono da soli, ma chi comanda il {@link GameEngine} da un altro thread deve
     * inviare i comandi con {@link SwingUtilities#invokeLater(Runnable)}. Se se ne
     * dimentica, e' meglio fermarsi subito con un errore chiaro che lasciare
     * un'interfaccia corrotta in modo casuale e difficile da riprodurre.
     *
     * @throws IllegalStateException se il thread corrente non e' l'Event Dispatch Thread
     */
    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("La GUI va aggiornata sull'Event Dispatch Thread: "
                    + "inviare i comandi al GameEngine con SwingUtilities.invokeLater");
        }
    }
}
