package it.unibo.monopoly.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import it.unibo.monopoly.model.board.Board;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.RollResult;
import it.unibo.monopoly.model.player.Player;

/**
 * Il tabellone disegnato: le {@link Board#SIZE} caselle disposte sul perimetro con
 * le pedine dei giocatori al posto giusto.
 * <p>
 * Le caselle sono sistemate su una griglia 11x11 ({@link GridBagLayout}): i quattro
 * angoli e le dieci posizioni per lato riempiono esattamente il bordo, e il quadrato
 * centrale - una cella sola che ne occupa nove per lato - ospita, sul verde scuro che
 * stacca il tabellone dal tavolo, la scritta "MONOPOLY" e sotto di lei i dadi
 * dell'ultimo lancio ({@link DiceView}). Tutto attorno corre la cornice gialla
 * ({@link Theme#boardBorder()}). La conversione fra la posizione sul tabellone (0-39) e
 * la cella della griglia sta tutta in {@link #gridCellOf(int)}.
 * <p>
 * Di chi sia il turno lo dice la casella su cui si trova il giocatore di turno,
 * evidenziata da un anello giallo "acceso" ({@link Theme#paintHighlight(Graphics2D, int,
 * int, int, int)}), lo stesso della sua scheda nel {@link PlayerInfoPanel}. L'anello sta
 * appena fuori dalla casella, sul bordo delle vicine, cosi' non copre la banda del
 * gruppo, il nome ne' le pedine: per questo lo disegna il tabellone, sopra le caselle, e
 * non la casella, che non puo' dipingere fuori dai propri confini.
 * <p>
 * Il pannello costruisce un {@link TilePanel} per casella una volta sola, alla
 * creazione: muovere una pedina non ricostruisce nulla, {@link #refresh()} si limita
 * a ridistribuire i giocatori fra le caselle e a chiedere il ridisegno. E' il
 * {@link MainWindow} a chiamare {@code refresh()} quando il model segnala un
 * cambiamento.
 * <p>
 * Il {@link GameState} viene usato in sola lettura: da qui non parte nessuna
 * modifica alla partita.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class BoardPanel extends JPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Lato della griglia: 4 angoli + 9 caselle per lato = 40 caselle sul perimetro. */
    private static final int GRID_SIDE = 11;

    /** Indice dell'ultima riga e dell'ultima colonna della griglia. */
    private static final int LAST_CELL = GRID_SIDE - 1;

    /** Posizione fuori dal tabellone: nessuna casella da evidenziare. */
    private static final int NO_HIGHLIGHT = -1;

    private final transient GameState state;

    /** Un pannello per casella, all'indice corrispondente alla posizione sul tabellone. */
    private final transient List<TilePanel> tilePanels;

    /** La casella del giocatore di turno, oppure {@link #NO_HIGHLIGHT}. */
    private int highlightedPosition;

    /** I dadi al centro del tabellone, vuoti fino al primo lancio di questa partita. */
    private final DiceView diceView;

    /**
     * Costruisce il tabellone a partire dallo stato della partita.
     *
     * @param state lo stato della partita, usato in sola lettura
     * @throws IllegalArgumentException se lo stato e' null
     */
    public BoardPanel(final GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("Lo stato della partita non puo' essere null");
        }
        this.state = state;
        this.tilePanels = new ArrayList<>(Board.SIZE);
        this.highlightedPosition = NO_HIGHLIGHT;
        this.diceView = new DiceView();

        this.setLayout(new GridBagLayout());
        this.setBackground(Theme.DARK_GREEN);
        this.setBorder(Theme.boardBorder());
        this.createTiles();
        this.add(this.createCenter(), centerConstraints());
        this.refresh();
    }

    /**
     * Mostra sui dadi al centro i valori dell'ultimo lancio.
     * <p>
     * Non serve un metodo per svuotarli: quando la finestra passa a un'altra partita (per
     * esempio caricata da file) crea un tabellone nuovo, con i dadi vuoti.
     *
     * @param result il risultato comunicato dal motore
     */
    public void showRoll(final RollResult result) {
        this.diceView.setValues(result.firstDie(), result.secondDie());
    }

    /**
     * Riporta il disegno in linea con lo stato della partita: sposta le pedine sulle
     * caselle in cui si trovano ora ed evidenzia la casella del giocatore di turno.
     * <p>
     * A partita finita, o quando il giocatore corrente e' fallito, nessuna casella
     * viene evidenziata (vedi {@link ViewStyle#playerToHighlight(GameState)}).
     * <p>
     * E' l'unico metodo che il {@link MainWindow} deve chiamare dopo un evento del
     * model: il pannello ricava tutto il resto dal {@link GameState}.
     */
    public void refresh() {
        // Prima si raccoglie, per ogni casella, chi ci si trova: i falliti sono usciti
        // dalla partita e la loro pedina viene tolta dal tabellone.
        final Map<Integer, List<Player>> occupants = new HashMap<>();
        for (final Player player : this.state.getPlayers()) {
            if (!player.isBankrupt()) {
                occupants.computeIfAbsent(player.getPosition(), position -> new ArrayList<>()).add(player);
            }
        }
        for (int position = 0; position < this.tilePanels.size(); position++) {
            this.tilePanels.get(position).setOccupants(occupants.getOrDefault(position, List.of()));
        }
        this.highlightedPosition = ViewStyle.playerToHighlight(this.state)
                .map(Player::getPosition)
                .orElse(NO_HIGHLIGHT);
        // L'anello sta a cavallo fra piu' caselle: si ridisegna tutto il tabellone, che
        // toglie quello vecchio e disegna quello nuovo.
        this.repaint();
    }

    /**
     * Disegna le caselle e, sopra, l'evidenziazione della casella del giocatore di turno.
     * <p>
     * L'anello viene ritagliato dentro la cornice: sul lato esterno di una casella di
     * bordo a segnarla basta la cornice gialla, che le sta gia' a contatto.
     *
     * @param g il contesto grafico fornito da Swing
     */
    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (this.highlightedPosition == NO_HIGHLIGHT) {
            return;
        }
        final Rectangle tile = this.tilePanels.get(this.highlightedPosition).getBounds();
        final Insets frame = this.getInsets();
        final Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.clipRect(frame.left, frame.top,
                    this.getWidth() - frame.left - frame.right, this.getHeight() - frame.top - frame.bottom);
            final int ring = Theme.HIGHLIGHT_RING;
            Theme.paintHighlight(graphics, tile.x - ring, tile.y - ring,
                    tile.width + 2 * ring, tile.height + 2 * ring);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Fa ripartire dal tabellone ogni ridisegno chiesto da una casella.
     * <p>
     * L'anello di evidenziazione e' disegnato dal tabellone sopra le caselle: se una
     * casella si ridisegnasse da sola (per esempio quando cambia chi ci sta sopra)
     * cancellerebbe la parte di anello che le passa sopra. Partendo dal tabellone,
     * invece, l'anello viene ridisegnato ogni volta insieme alle caselle.
     *
     * @return sempre true
     */
    @Override
    protected boolean isPaintingOrigin() {
        return true;
    }

    /**
     * Il quadrato centrale: la scritta "MONOPOLY" e, centrati sotto di lei, i dadi. Una
     * colonna alla sua misura, tenuta al centro del quadrato.
     */
    private JPanel createCenter() {
        final JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.add(Theme.titleBanner());
        column.add(Box.createVerticalStrut(3 * Theme.GAP));
        column.add(this.diceView);

        // GridBagLayout con un solo componente: lo tiene al centro, alla sua misura.
        final JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(column);
        return center;
    }

    /** Crea un {@link TilePanel} per ogni casella e lo mette nella cella giusta della griglia. */
    private void createTiles() {
        final Board board = this.state.getBoard();
        for (int position = 0; position < board.getSize(); position++) {
            final TilePanel panel = new TilePanel(board.getTileAt(position));
            this.tilePanels.add(panel);
            this.add(panel, tileConstraints(position));
        }
    }

    /**
     * Traduce la posizione sul tabellone nella cella della griglia in cui disegnarla.
     * <p>
     * Si parte dall'angolo in basso a destra (il "Via", posizione 0) e si procede in
     * senso antiorario, come sul tabellone vero:
     * <ul>
     *   <li>0-10: lato inferiore, da destra verso sinistra;</li>
     *   <li>11-20: lato sinistro, dal basso verso l'alto;</li>
     *   <li>21-30: lato superiore, da sinistra verso destra;</li>
     *   <li>31-39: lato destro, dall'alto verso il basso.</li>
     * </ul>
     * Gli angoli appartengono a due lati: la formula li assegna al primo dei due, e
     * l'ordine dei controlli evita che vengano contati due volte.
     *
     * @param position la posizione sul tabellone, da 0 a {@link Board#SIZE} - 1
     * @return la cella della griglia, con {@code x} = colonna e {@code y} = riga
     */
    private static Point gridCellOf(final int position) {
        if (position <= LAST_CELL) {
            return new Point(LAST_CELL - position, LAST_CELL);
        }
        if (position <= 2 * LAST_CELL) {
            return new Point(0, 2 * LAST_CELL - position);
        }
        if (position <= 3 * LAST_CELL) {
            return new Point(position - 2 * LAST_CELL, 0);
        }
        return new Point(LAST_CELL, position - 3 * LAST_CELL);
    }

    /** Vincoli di una casella: una cella della griglia, che cresce insieme alla finestra. */
    private static GridBagConstraints tileConstraints(final int position) {
        final Point cell = gridCellOf(position);
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = cell.x;
        constraints.gridy = cell.y;
        constraints.fill = GridBagConstraints.BOTH;
        // Peso uguale per tutte le caselle: lo spazio in piu' viene diviso in parti uguali.
        constraints.weightx = 1;
        constraints.weighty = 1;
        return constraints;
    }

    /** Vincoli del quadrato centrale: una sola cella che ne occupa 9x9. */
    private static GridBagConstraints centerConstraints() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = GRID_SIDE - 2;
        constraints.gridheight = GRID_SIDE - 2;
        constraints.fill = GridBagConstraints.BOTH;
        return constraints;
    }
}
