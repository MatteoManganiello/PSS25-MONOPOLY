package monopoly.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import monopoly.model.board.Board;
import monopoly.model.game.GameState;
import monopoly.model.player.Player;

/**
 * Il tabellone disegnato: le {@link Board#SIZE} caselle disposte sul perimetro con
 * le pedine dei giocatori al posto giusto.
 * <p>
 * Le caselle sono sistemate su una griglia 11x11 ({@link GridBagLayout}): i quattro
 * angoli e le dieci posizioni per lato riempiono esattamente il bordo, e il quadrato
 * centrale 9x9 - una cella sola che ne occupa nove per lato - ospita il titolo e
 * l'indicazione del giocatore di turno. La conversione fra la posizione sul
 * tabellone (0-39) e la cella della griglia sta tutta in {@link #gridCellOf(int)}.
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

    /** Etichetta centrale con il giocatore di turno. */
    private final JLabel turnLabel;

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
        this.turnLabel = new JLabel("", JLabel.CENTER);

        this.setLayout(new GridBagLayout());
        this.setBackground(ViewStyle.BOARD_BACKGROUND);
        this.setBorder(BorderFactory.createLineBorder(ViewStyle.OUTLINE, 2));
        this.createTiles();
        this.add(this.createCenter(), centerConstraints());
        this.refresh();
    }

    /**
     * Riporta il disegno in linea con lo stato della partita: sposta le pedine sulle
     * caselle in cui si trovano ora, evidenzia la casella del giocatore di turno e
     * aggiorna la scritta centrale.
     * <p>
     * A partita finita, o quando il giocatore corrente e' fallito, nessuna casella
     * viene evidenziata (vedi {@link ViewStyle#playerToHighlight(GameState)}); a partita
     * finita la scritta centrale diventa "Partita finita".
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
        final Optional<Player> highlighted = ViewStyle.playerToHighlight(this.state);
        final int highlightedPosition = highlighted.map(Player::getPosition).orElse(NO_HIGHLIGHT);
        for (int position = 0; position < this.tilePanels.size(); position++) {
            final TilePanel panel = this.tilePanels.get(position);
            panel.setOccupants(occupants.getOrDefault(position, List.of()));
            panel.setCurrent(position == highlightedPosition);
        }
        this.turnLabel.setText(this.state.isGameOver()
                ? "Partita finita"
                : "Turno di " + this.state.getCurrentPlayer().getName());
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

    /** Il quadrato centrale: titolo del gioco e giocatore di turno. */
    private JPanel createCenter() {
        final JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(ViewStyle.BOARD_BACKGROUND);

        final JLabel title = new JLabel("MONOPOLY", JLabel.CENTER);
        title.setFont(ViewStyle.TITLE_FONT);
        title.setForeground(ViewStyle.OUTLINE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        this.turnLabel.setFont(ViewStyle.PLAYER_NAME_FONT);
        this.turnLabel.setForeground(ViewStyle.TEXT);
        this.turnLabel.setAlignmentX(CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(title);
        center.add(Box.createVerticalStrut(8));
        center.add(this.turnLabel);
        center.add(Box.createVerticalGlue());
        return center;
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
