package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import it.unibo.monopoly.controller.GameSetup;
import it.unibo.monopoly.controller.PlayerSetup;
import it.unibo.monopoly.controller.SetupProblem;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.Token;
import it.unibo.monopoly.model.player.TokenCatalog;

/**
 * La schermata che si apre prima della partita: qui si decide chi gioca e con che
 * pedina.
 * <p>
 * <b>Cosa fa.</b> Mostra una riga per giocatore ({@link PlayerSetupRow}), da un
 * minimo di {@link GameState#MIN_PLAYERS} a un massimo di
 * {@link GameState#MAX_PLAYERS}, e i pulsanti per aggiungerne e toglierne. Le pedine
 * gia' scelte spariscono dalle tendine degli altri, quindi due giocatori non possono
 * ritrovarsi con la stessa; il controllo vero resta comunque in
 * {@link GameSetup}, perche' una regola del gioco non puo' dipendere dal fatto che
 * una tendina sia fatta bene.
 * <p>
 * <b>Cosa non fa.</b> Non crea la partita e non conosce il motore. Alla conferma
 * chiede a {@link GameSetup} se i dati vanno bene: se non vanno, mostra il perche' e
 * resta aperta; se vanno, fa costruire i {@link Player} e li consegna a chi le ha
 * passato l'azione {@code onConfirm}, cioe' a
 * {@link it.unibo.monopoly.MonopolyApp MonopolyApp}, che da li' in poi avvia il gioco.
 * Cosi' la finestra resta una view: raccoglie dati e li passa, senza decidere niente.
 * <p>
 * La classe e' {@code final} come le altre finestre e pannelli del progetto.
 */
public final class SetupWindow extends JFrame {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Titolo della finestra. */
    private static final String TITLE = "Monopoly - Nuova partita";

    /** Come si chiama un giocatore finche' non ci mette il suo nome. */
    private static final String DEFAULT_NAME = "Giocatore ";

    /** Quanti giocatori sono gia' pronti quando la schermata si apre. */
    private static final int INITIAL_PLAYERS = GameState.MIN_PLAYERS;

    /**
     * Dimensioni di partenza dell'area scorrevole con le righe dei giocatori.
     * <p>
     * Si danno a lei e non alla finestra: cosi' {@code pack()} calcola il resto e
     * titolo e pulsanti ci stanno sempre, comunque siano larghe le righe.
     */
    private static final Dimension ROWS_AREA_SIZE = new Dimension(560, 210);

    /** Le righe dei giocatori, nello stesso ordine in cui sono mostrate. */
    private final transient List<PlayerSetupRow> rows = new ArrayList<>();

    /** Il contenitore verticale che tiene le righe. */
    private final JPanel rowsPanel = new JPanel();

    private final JButton addButton = new JButton("Aggiungi giocatore");
    private final JButton startButton = new JButton("Inizia partita");

    /** Cosa fare con i giocatori configurati, quando l'utente conferma. */
    private final transient Consumer<List<Player>> onConfirm;

    /**
     * Crea la schermata di setup.
     *
     * @param onConfirm l'azione a cui consegnare i giocatori quando la configurazione
     *                  e' valida e l'utente conferma; e' l'unico modo che ha questa
     *                  finestra di far partire qualcosa
     * @throws IllegalArgumentException se l'azione e' null
     */
    public SetupWindow(final Consumer<List<Player>> onConfirm) {
        super(TITLE);
        if (onConfirm == null) {
            throw new IllegalArgumentException("Serve un'azione da eseguire alla conferma");
        }
        this.onConfirm = onConfirm;

        this.rowsPanel.setLayout(new BoxLayout(this.rowsPanel, BoxLayout.Y_AXIS));
        this.rowsPanel.setBackground(ViewStyle.PANEL_BACKGROUND);

        this.addButton.addActionListener(event -> this.addPlayerRow());
        this.startButton.addActionListener(event -> this.confirm());

        this.setLayout(new BorderLayout());
        this.add(this.createHeader(), BorderLayout.NORTH);
        this.add(this.createRowsArea(), BorderLayout.CENTER);
        this.add(this.createButtons(), BorderLayout.SOUTH);

        for (int player = 0; player < INITIAL_PLAYERS; player++) {
            this.addPlayerRow();
        }

        // Chiudendo questa finestra si rinuncia a giocare: non c'e' nient'altro aperto.
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getRootPane().setDefaultButton(this.startButton);
        this.pack();
        this.setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Composizione della finestra
    // ------------------------------------------------------------------

    /** Il titolo e la riga di istruzioni in cima alla finestra. */
    private JPanel createHeader() {
        final JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ViewStyle.PANEL_BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        final JLabel title = new JLabel("Chi gioca?");
        title.setFont(ViewStyle.TITLE_FONT);
        title.setForeground(ViewStyle.TEXT);

        final JLabel hint = new JLabel("Da " + GameState.MIN_PLAYERS + " a " + GameState.MAX_PLAYERS
                + " giocatori. Ogni pedina puo' essere scelta da un giocatore solo.");
        hint.setFont(ViewStyle.PLAYER_INFO_FONT);
        hint.setForeground(ViewStyle.TEXT);

        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.SOUTH);
        return header;
    }

    /**
     * L'area scorrevole con le righe dei giocatori.
     * <p>
     * Le righe stanno in un {@link BorderLayout#NORTH}: senza, con pochi giocatori si
     * allargherebbero in altezza per riempire lo spazio vuoto.
     */
    private JScrollPane createRowsArea() {
        final JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(ViewStyle.PANEL_BACKGROUND);
        holder.add(this.rowsPanel, BorderLayout.NORTH);

        final JScrollPane scroller = new JScrollPane(holder);
        scroller.setPreferredSize(ROWS_AREA_SIZE);
        scroller.getViewport().setBackground(ViewStyle.PANEL_BACKGROUND);
        scroller.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        return scroller;
    }

    /** La barra in basso con i due pulsanti. */
    private JPanel createButtons() {
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBackground(ViewStyle.PANEL_BACKGROUND);
        buttons.add(this.addButton);
        buttons.add(Box.createHorizontalStrut(16));
        buttons.add(this.startButton);
        return buttons;
    }

    // ------------------------------------------------------------------
    // Aggiunta e rimozione dei giocatori
    // ------------------------------------------------------------------

    /**
     * Aggiunge una riga con un nome gia' pronto ("Giocatore 3") e la prima pedina
     * ancora libera, cosi' si puo' iniziare subito senza compilare niente.
     */
    private void addPlayerRow() {
        final Optional<Token> free = this.firstFreeToken();
        if (this.rows.size() >= GameState.MAX_PLAYERS || free.isEmpty()) {
            // I pulsanti non lo permetterebbero: e' solo una difesa.
            return;
        }
        final PlayerSetupRow row = new PlayerSetupRow(
                DEFAULT_NAME + (this.rows.size() + 1),
                free.orElseThrow(),
                this::refreshTokenChoices,
                this::removePlayerRow);
        this.rows.add(row);
        this.rowsPanel.add(row);
        this.refresh();
        row.focusName();
    }

    /**
     * Toglie una riga dal tavolo. Sotto il minimo consentito non si scende: il
     * pulsante e' gia' disabilitato, qui si ricontrolla e basta.
     */
    private void removePlayerRow(final PlayerSetupRow row) {
        if (this.rows.size() <= GameState.MIN_PLAYERS) {
            return;
        }
        this.rows.remove(row);
        this.rowsPanel.remove(row);
        this.refresh();
    }

    /**
     * Rimette in ordine tutta la schermata dopo ogni cambiamento: numeri delle righe,
     * pedine ancora libere, pulsanti attivi o spenti.
     */
    private void refresh() {
        final boolean removable = this.rows.size() > GameState.MIN_PLAYERS;
        for (int index = 0; index < this.rows.size(); index++) {
            final int number = index + 1;
            this.rows.get(index).updatePosition(number, DEFAULT_NAME + number, removable);
        }
        this.refreshTokenChoices();
        this.addButton.setEnabled(this.rows.size() < GameState.MAX_PLAYERS && this.firstFreeToken().isPresent());
        this.rowsPanel.revalidate();
        this.rowsPanel.repaint();
    }

    /** Rifa' le tendine di tutte le righe, togliendo da ognuna le pedine degli altri. */
    private void refreshTokenChoices() {
        final List<Token> taken = this.chosenTokens();
        for (final PlayerSetupRow row : this.rows) {
            row.showFreeTokens(TokenCatalog.availableTokens(), taken);
        }
    }

    /** @return le pedine scelte in questo momento, senza i posti ancora vuoti */
    private List<Token> chosenTokens() {
        return this.rows.stream()
                .map(PlayerSetupRow::getSelectedToken)
                .filter(Objects::nonNull)
                .toList();
    }

    /** @return la prima pedina che non ha ancora preso nessuno */
    private Optional<Token> firstFreeToken() {
        final List<Token> taken = this.chosenTokens();
        return TokenCatalog.availableTokens().stream()
                .filter(token -> !taken.contains(token))
                .findFirst();
    }

    // ------------------------------------------------------------------
    // Conferma
    // ------------------------------------------------------------------

    /**
     * Risponde al pulsante "Inizia partita".
     * <p>
     * Non controlla niente da sola: passa quello che ha raccolto a {@link GameSetup} e
     * si comporta di conseguenza. Se ci sono problemi li mostra tutti insieme e resta
     * aperta; altrimenti si chiude e consegna i giocatori a chi deve avviare la partita.
     */
    private void confirm() {
        final List<PlayerSetup> configured = this.rows.stream()
                .map(PlayerSetupRow::toPlayerSetup)
                .toList();
        final GameSetup setup = new GameSetup(configured);
        final List<SetupProblem> problems = setup.validate();
        if (!problems.isEmpty()) {
            this.showProblems(problems);
            return;
        }
        final List<Player> players = setup.createPlayers();
        this.dispose();
        this.onConfirm.accept(players);
    }

    /** Mostra in una finestrella tutti i motivi per cui non si puo' ancora giocare. */
    private void showProblems(final List<SetupProblem> problems) {
        final StringBuilder message = new StringBuilder("Non si puo' ancora iniziare:\n");
        for (final SetupProblem problem : problems) {
            message.append("\n - ").append(ViewStyle.describe(problem));
        }
        JOptionPane.showMessageDialog(this, message.toString(),
                "Configurazione da correggere", JOptionPane.WARNING_MESSAGE);
    }
}
