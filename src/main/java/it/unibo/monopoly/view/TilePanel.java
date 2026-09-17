package it.unibo.monopoly.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JPanel;

import it.unibo.monopoly.model.board.PropertyTile;
import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.player.Player;

/**
 * Componente grafico di una singola casella del tabellone.
 * <p>
 * Disegna sempre le stesse cose - sfondo, nome, riga di dettaglio, pedine presenti -
 * ma il risultato cambia da casella a casella perche' i dati arrivano da chiamate
 * polimorfiche: {@link Tile#getCategory()} decide il colore di sfondo e
 * {@link Tile#getDetail()} il testo della seconda riga. Il pannello non sa (e non
 * chiede) se la casella sia una tassa, il "Via" o la prigione: e' la casella a
 * rispondere, e ogni sottoclasse risponde a modo suo.
 * <p>
 * L'unica eccezione riguarda le informazioni che esistono solo per le caselle
 * acquistabili (proprietario e affitto): li' serve un controllo di tipo su
 * {@link PropertyTile}, ma si ferma a quella classe base, non e' una catena di
 * {@code instanceof} che elenca terreni, stazioni e societa'. Anche l'affitto
 * mostrato e' una chiamata polimorfica ({@link PropertyTile#getCurrentRent()}): il
 * pannello chiede l'importo alla casella senza sapere con che regola lo calcoli.
 * <p>
 * E' un componente passivo: non conosce il {@link it.unibo.monopoly.controller.GameEngine
 * GameEngine} e non modifica nulla. Riceve dal {@link BoardPanel} chi si trova sulla
 * casella ({@link #setOccupants(List)}) e se e' la casella del giocatore di turno
 * ({@link #setCurrent(boolean)}), e si limita a ridisegnarsi.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class TilePanel extends JPanel {

    /**
     * Richiesto perche' i componenti Swing sono serializzabili; il valore non ha
     * importanza in questo progetto, ma senza dichiararlo il compilatore segnala un
     * warning con {@code -Xlint:all}.
     */
    private static final long serialVersionUID = 1L;

    /** Lato preferito della casella, in pixel: 11 caselle per lato danno un tabellone di ~640px. */
    private static final int PREFERRED_SIDE = 58;

    /** Altezza della striscia colorata che indica il proprietario. */
    private static final int OWNER_BAND_HEIGHT = 8;

    /** Diametro della pedina disegnata sulla casella. */
    private static final int TOKEN_DIAMETER = 13;

    /** Margine interno della casella. */
    private static final int PADDING = 3;

    /** Numero massimo di righe su cui spezzare il nome della casella. */
    private static final int MAX_NAME_LINES = 3;

    private final transient Tile tile;

    /** Giocatori attualmente fermi su questa casella, in ordine di turno. */
    private final transient List<Player> occupants;

    /** True se qui si trova il giocatore di turno: la casella viene evidenziata. */
    private boolean current;

    /**
     * Crea il componente di una casella.
     *
     * @param tile la casella da rappresentare, usata in sola lettura
     * @throws IllegalArgumentException se la casella e' null
     */
    public TilePanel(final Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException("La casella da disegnare non puo' essere null");
        }
        this.tile = tile;
        this.occupants = new ArrayList<>();
        this.current = false;
        this.setPreferredSize(new Dimension(PREFERRED_SIDE, PREFERRED_SIDE));
        this.setOpaque(true);
        // Registra il componente presso il gestore dei tooltip; il testo vero e'
        // calcolato al volo da getToolTipText(), perche' cambia durante la partita.
        this.setToolTipText(tile.getName());
    }

    /** @return la casella rappresentata da questo componente */
    public Tile getTile() {
        return this.tile;
    }

    /**
     * Aggiorna l'elenco dei giocatori fermi su questa casella e ridisegna.
     *
     * @param players i giocatori presenti, eventualmente nessuno
     */
    public void setOccupants(final List<Player> players) {
        this.occupants.clear();
        if (players != null) {
            this.occupants.addAll(players);
        }
        this.repaint();
    }

    /**
     * Evidenzia (o smette di evidenziare) la casella del giocatore di turno.
     *
     * @param current true se qui si trova il giocatore di turno
     */
    public void setCurrent(final boolean current) {
        if (this.current != current) {
            this.current = current;
            this.repaint();
        }
    }

    /**
     * Descrizione completa mostrata passando il mouse sulla casella: le informazioni
     * che sul quadratino non ci starebbero.
     *
     * @return il testo del tooltip
     */
    @Override
    public String getToolTipText() {
        final StringBuilder text = new StringBuilder(this.tile.getName());
        if (!this.tile.getDetail().isEmpty()) {
            text.append(" - ").append(this.tile.getDetail());
        }
        if (this.tile instanceof PropertyTile property) {
            // getCurrentRent() e' l'affitto che si pagherebbe adesso: tiene gia' conto del
            // monopolio di colore, delle stazioni possedute e dell'ultimo lancio di dadi.
            text.append(property.getOwner()
                    .map(owner -> " - di " + owner.getName() + ", affitto " + property.getCurrentRent())
                    .orElse(" - in vendita"));
        }
        for (final Player player : this.occupants) {
            text.append(", ").append(player.getName());
        }
        return text.toString();
    }

    /**
     * Disegna la casella: sfondo della sua famiglia, striscia del proprietario, nome,
     * dettaglio e pedine presenti.
     *
     * @param g il contesto grafico fornito da Swing
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final int width = this.getWidth();
            final int height = this.getHeight();

            // Colore della casella: lo decide la categoria dichiarata dalla casella stessa.
            graphics.setColor(ViewStyle.colorOf(this.tile.getCategory()));
            graphics.fillRect(0, 0, width, height);

            final int textTop = this.paintOwnerBand(graphics, width);
            this.paintTexts(graphics, width, height, textTop);
            this.paintTokens(graphics, width, height);
            this.paintOutline(graphics, width, height);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Disegna la striscia superiore delle caselle acquistabili, colorata come la
     * pedina del proprietario (grigia se la casella e' ancora in vendita).
     *
     * @return l'altezza da cui puo' iniziare il testo
     */
    private int paintOwnerBand(final Graphics2D graphics, final int width) {
        if (!(this.tile instanceof PropertyTile property)) {
            return PADDING;
        }
        graphics.setColor(property.getOwner()
                .map(owner -> ViewStyle.colorOf(owner.getToken()))
                .orElse(new Color(0xC8, 0xC8, 0xC8)));
        graphics.fillRect(0, 0, width, OWNER_BAND_HEIGHT);
        return OWNER_BAND_HEIGHT + PADDING;
    }

    /**
     * Scrive il nome della casella (su piu' righe se serve) e la riga di dettaglio.
     * <p>
     * Lo spazio e' poco: il numero di righe del nome viene calcolato in base
     * all'altezza davvero disponibile, cioe' togliendo la riga di dettaglio e la fascia
     * in basso riservata alle pedine. Cosi' il dettaglio - prezzo o importo - resta
     * sempre leggibile e non finisce sotto le pedine anche sulle caselle dal nome
     * lungo, che vengono invece accorciate con i puntini (il nome intero e' nel tooltip).
     */
    private void paintTexts(final Graphics2D graphics, final int width, final int height, final int textTop) {
        graphics.setColor(ViewStyle.OUTLINE);
        graphics.setFont(ViewStyle.TILE_NAME_FONT);
        final FontMetrics nameMetrics = graphics.getFontMetrics();
        final int usableWidth = width - 2 * PADDING;
        final String detail = this.tile.getDetail();
        final int detailAscent = detail.isEmpty() ? 0
                : graphics.getFontMetrics(ViewStyle.TILE_DETAIL_FONT).getAscent();

        final int spaceForName = height - textTop - detailAscent - TOKEN_DIAMETER - PADDING;
        final int maxLines = Math.max(1, Math.min(MAX_NAME_LINES, spaceForName / nameMetrics.getHeight()));

        int baseline = textTop + nameMetrics.getAscent();
        for (final String line : wrap(this.tile.getName(), nameMetrics, usableWidth, maxLines)) {
            graphics.drawString(line, centeredX(line, nameMetrics, width), baseline);
            baseline += nameMetrics.getHeight();
        }

        if (!detail.isEmpty()) {
            graphics.setFont(ViewStyle.TILE_DETAIL_FONT);
            final FontMetrics detailMetrics = graphics.getFontMetrics();
            // Appena sopra le pedine se il nome lo consente, subito sotto al nome
            // altrimenti; mai oltre il bordo inferiore. Dopo il ciclo "baseline" e' gia'
            // la linea di base della riga libera successiva: la riga di dettaglio parte
            // dalla cima di quella riga, senza lasciare una riga vuota in mezzo.
            final int belowName = baseline - nameMetrics.getAscent() + detailMetrics.getAscent();
            final int detailBaseline = Math.min(height - PADDING - detailMetrics.getDescent(),
                    Math.max(belowName, height - TOKEN_DIAMETER - PADDING));
            graphics.drawString(detail, centeredX(detail, detailMetrics, width), detailBaseline);
        }
    }

    /**
     * Disegna una pallina per ogni giocatore presente, con l'iniziale del suo nome.
     * <p>
     * Le pedine stanno affiancate finche' c'e' posto. Quando sono troppe per la
     * larghezza della casella (da quattro giocatori in su) si sovrappongono in parte,
     * come fiches impilate, invece di uscire dal bordo: il colore resta visibile e i
     * nomi completi sono nel tooltip.
     */
    private void paintTokens(final Graphics2D graphics, final int width, final int height) {
        if (this.occupants.isEmpty()) {
            return;
        }
        final int count = this.occupants.size();
        // Spazio in cui possono iniziare le pedine successive alla prima.
        final int available = width - 2 * PADDING - TOKEN_DIAMETER;
        final int spacing = count == 1 ? 0
                : Math.max(1, Math.min(TOKEN_DIAMETER + 1, available / (count - 1)));
        final int totalWidth = (count - 1) * spacing + TOKEN_DIAMETER;
        int x = Math.max(PADDING, (width - totalWidth) / 2);
        final int y = height - TOKEN_DIAMETER - 2;

        for (final Player player : this.occupants) {
            graphics.setColor(ViewStyle.colorOf(player.getToken()));
            graphics.fillOval(x, y, TOKEN_DIAMETER, TOKEN_DIAMETER);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawOval(x, y, TOKEN_DIAMETER, TOKEN_DIAMETER);
            // Iniziale del nome: distingue le pedine anche a chi non ricorda i colori.
            graphics.setFont(ViewStyle.TILE_DETAIL_FONT);
            final String initial = player.getName().substring(0, 1).toUpperCase(Locale.ROOT);
            final FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(initial,
                    x + (TOKEN_DIAMETER - metrics.stringWidth(initial)) / 2,
                    y + (TOKEN_DIAMETER + metrics.getAscent()) / 2 - 1);
            x += spacing;
        }
    }

    /** Traccia la cornice: sottile e scura, spessa e arancione sulla casella di turno. */
    private void paintOutline(final Graphics2D graphics, final int width, final int height) {
        graphics.setColor(this.current ? ViewStyle.HIGHLIGHT : ViewStyle.OUTLINE);
        graphics.setStroke(new BasicStroke(this.current ? 3f : 1f));
        graphics.drawRect(1, 1, width - 3, height - 3);
    }

    /** @return la coordinata x che centra il testo nella casella */
    private static int centeredX(final String text, final FontMetrics metrics, final int width) {
        return Math.max(PADDING, (width - metrics.stringWidth(text)) / 2);
    }

    /**
     * Spezza un nome lungo su piu' righe, in modo che stia nella larghezza della
     * casella. Le righe eccedenti vengono scartate con dei puntini di sospensione:
     * il nome completo resta comunque leggibile nel tooltip.
     *
     * @param text     il testo da mandare a capo
     * @param metrics  le misure del carattere in uso
     * @param maxWidth la larghezza disponibile in pixel
     * @param maxLines quante righe ci stanno nell'altezza rimasta
     * @return le righe da disegnare
     */
    private static List<String> wrap(final String text, final FontMetrics metrics,
                                     final int maxWidth, final int maxLines) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.size() > maxLines) {
            final List<String> trimmed = new ArrayList<>(lines.subList(0, maxLines));
            trimmed.set(maxLines - 1, trimmed.get(maxLines - 1) + "...");
            return trimmed;
        }
        return lines;
    }
}
