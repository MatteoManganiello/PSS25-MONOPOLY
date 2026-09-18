package it.unibo.monopoly.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JPanel;

import it.unibo.monopoly.model.board.PropertyTile;
import it.unibo.monopoly.model.board.StreetTile;
import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.board.TileCategory;
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
 * Le eccezioni riguardano le informazioni che esistono solo per alcune caselle. Il
 * proprietario e l'affitto esistono solo per le caselle acquistabili: li' serve un
 * controllo di tipo su {@link PropertyTile}, ma si ferma a quella classe base. Anche
 * l'affitto mostrato e' una chiamata polimorfica ({@link PropertyTile#getCurrentRent()}):
 * il pannello chiede l'importo alla casella senza sapere con che regola lo calcoli. Il
 * gruppo di colore, invece, esiste solo per i terreni ({@link StreetTile}), ed e' l'unico
 * motivo per cui il pannello li riconosce: serve a colorare la banda in alto, come sul
 * tabellone vero. Non e' una catena di {@code instanceof}: stazioni e societa' non
 * vengono mai distinte fra loro.
 * <p>
 * Imprevisti e Probabilita' ({@link TileCategory#CARD}) hanno invece una banda alta, del
 * colore del loro mazzo, con il punto interrogativo grande al centro: devono
 * riconoscersi a colpo d'occhio anche fra tante caselle crema.
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

    /**
     * Lato preferito della casella, in pixel: 11 caselle per lato danno un tabellone di
     * ~730px. E' quanto basta perche', con i margini interni e le pedine grandi, i nomi
     * stiano su due righe.
     */
    private static final int PREFERRED_SIDE = 66;

    /**
     * Altezza della banda in alto sulle caselle acquistabili. Insieme a
     * {@link #BAND_GAP} lascia al nome le sue due righe anche sulle caselle piu' piccole.
     */
    private static final int BAND_HEIGHT = 10;

    /** Spazio fra la banda e la prima riga del nome. */
    private static final int BAND_GAP = 4;

    /** Altezza della banda colorata di Imprevisti e Probabilita', che ospita il simbolo. */
    private static final int CARD_BAND_HEIGHT = 24;

    /** Diametro del segnalino del proprietario, disegnato dentro la banda. */
    private static final int OWNER_BADGE = 8;

    /**
     * Quanto la casella di turno si vela del verde del tavolo: e' la stessa tinta della
     * scheda del suo giocatore ({@link Theme#CURRENT_SURFACE}).
     */
    private static final double CURRENT_TINT = 0.22;

    /** Diametro della pedina disegnata sulla casella: grande abbastanza da leggerne l'iniziale. */
    private static final int TOKEN_DIAMETER = 15;

    /** Margine interno della casella: distanza di testo e segnalini dai bordi. */
    private static final int PADDING = 5;

    /** Distanza delle pedine dal bordo inferiore: un po' meno del margine, dove lo spazio e' piu' stretto. */
    private static final int TOKEN_BOTTOM_MARGIN = 3;

    /** Misura minima a cui si puo' rimpicciolire una parola troppo lunga per la casella. */
    private static final float MIN_FONT_SIZE = 7f;

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
     * Disegna la casella: sfondo della sua famiglia, banda del gruppo con il segnalino
     * del proprietario, nome, dettaglio e pedine presenti.
     *
     * @param g il contesto grafico fornito da Swing
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D graphics = (Graphics2D) g.create();
        try {
            Theme.antialias(graphics);
            final int width = this.getWidth();
            final int height = this.getHeight();

            // Colore della casella: lo decide la categoria dichiarata dalla casella stessa.
            // Quella del giocatore di turno si vela di verde, come la sua scheda.
            final Color background = ViewStyle.colorOf(this.tile.getCategory());
            graphics.setColor(this.current ? Theme.mix(background, Theme.TABLE_GREEN, CURRENT_TINT) : background);
            graphics.fillRect(0, 0, width, height);

            final int textTop = this.paintBand(graphics, width);
            this.paintTexts(graphics, width, height, textTop);
            // La cornice prima delle pedine: sulla casella di turno e' spessa, e le
            // pedine, che sono l'informazione piu' importante, devono restarle sopra.
            this.paintOutline(graphics, width, height);
            this.paintTokens(graphics, width, height);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Disegna la banda superiore delle caselle acquistabili: il colore del gruppo per i
     * terreni, una tinta neutra per stazioni e societa'. Se la casella ha un
     * proprietario, dentro la banda compare il suo segnalino. Imprevisti e Probabilita'
     * hanno la loro banda, piu' alta ({@link #paintCardBand(Graphics2D, int)}).
     *
     * @return l'altezza da cui puo' iniziare il testo
     */
    private int paintBand(final Graphics2D graphics, final int width) {
        if (this.tile.getCategory() == TileCategory.CARD) {
            return this.paintCardBand(graphics, width);
        }
        if (!(this.tile instanceof PropertyTile property)) {
            return PADDING;
        }
        graphics.setColor(this.tile instanceof StreetTile street
                ? ViewStyle.colorOf(street.getGroup())
                : Theme.NEUTRAL_BAND);
        graphics.fillRect(0, 0, width, BAND_HEIGHT);
        graphics.setColor(Theme.BORDER_STRONG);
        graphics.drawLine(0, BAND_HEIGHT, width, BAND_HEIGHT);
        property.getOwner().ifPresent(owner -> paintOwnerBadge(graphics, width, ViewStyle.colorOf(owner.getToken())));
        return BAND_HEIGHT + BAND_GAP;
    }

    /**
     * Banda di Imprevisti e Probabilita': alta, del colore del mazzo, con il simbolo
     * della casella ({@link Tile#getDetail()}, il punto interrogativo) grande al centro.
     * Il simbolo e' scuro: sull'arancione e sull'azzurro si legge meglio del crema.
     *
     * @return l'altezza da cui puo' iniziare il nome
     */
    private int paintCardBand(final Graphics2D graphics, final int width) {
        final Color color = ViewStyle.colorOfCardTile(this.tile);
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, CARD_BAND_HEIGHT);
        graphics.setColor(Theme.BORDER_STRONG);
        graphics.drawLine(0, CARD_BAND_HEIGHT, width, CARD_BAND_HEIGHT);
        final String symbol = this.tile.getDetail();
        if (!symbol.isEmpty()) {
            graphics.setColor(Theme.readableTextOn(color));
            graphics.setFont(Theme.SYMBOL_FONT);
            final FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(symbol, centeredX(symbol, metrics, width),
                    (CARD_BAND_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2);
        }
        return CARD_BAND_HEIGHT + BAND_GAP;
    }

    /**
     * Segnalino del proprietario: un pallino del colore della sua pedina, nell'angolo
     * destro della banda. L'anello grigio scuro lo stacca da qualunque colore di gruppo,
     * anche quando e' simile a quello della pedina.
     */
    private static void paintOwnerBadge(final Graphics2D graphics, final int width, final Color ownerColor) {
        final int x = width - OWNER_BADGE - PADDING;
        final int y = (BAND_HEIGHT - OWNER_BADGE) / 2;
        graphics.setColor(Theme.BORDER_STRONG);
        graphics.fillOval(x, y, OWNER_BADGE, OWNER_BADGE);
        graphics.setColor(ownerColor);
        graphics.fillOval(x + 1, y + 1, OWNER_BADGE - 2, OWNER_BADGE - 2);
    }

    /**
     * Scrive il nome della casella (su piu' righe se serve) e la riga di dettaglio.
     * <p>
     * Lo spazio e' poco: il numero di righe del nome viene calcolato in base
     * all'altezza davvero disponibile, cioe' togliendo la riga di dettaglio e la fascia
     * in basso riservata alle pedine. Cosi' il dettaglio - prezzo o importo - resta
     * sempre leggibile e non finisce sotto le pedine anche sulle caselle dal nome
     * lungo, che vengono invece accorciate con i puntini (il nome intero e' nel tooltip).
     * <p>
     * Una parola sola piu' larga della casella ("patrimoniale") non si puo' mandare a
     * capo: quella riga viene scritta appena piu' piccola, cosi' resta dentro i margini
     * invece di finire sul bordo.
     */
    private void paintTexts(final Graphics2D graphics, final int width, final int height, final int textTop) {
        graphics.setColor(Theme.TEXT_DARK);
        graphics.setFont(Theme.TILE_NAME_FONT);
        final FontMetrics nameMetrics = graphics.getFontMetrics();
        final int usableWidth = width - 2 * PADDING;
        // Su Imprevisti e Probabilita' il dettaglio e' gia' il simbolo nella banda.
        final String detail = this.tile.getCategory() == TileCategory.CARD ? "" : this.tile.getDetail();
        final int detailAscent = detail.isEmpty() ? 0
                : graphics.getFontMetrics(Theme.TILE_DETAIL_FONT).getAscent();

        final int spaceForName = height - textTop - detailAscent - TOKEN_DIAMETER - PADDING;
        final int maxLines = Math.max(1, Math.min(MAX_NAME_LINES, spaceForName / nameMetrics.getHeight()));

        int baseline = textTop + nameMetrics.getAscent();
        for (final String line : wrap(this.tile.getName(), nameMetrics, usableWidth, maxLines)) {
            graphics.setFont(fitting(Theme.TILE_NAME_FONT, line, usableWidth, graphics));
            graphics.drawString(line, centeredX(line, graphics.getFontMetrics(), width), baseline);
            baseline += nameMetrics.getHeight();
        }

        if (!detail.isEmpty()) {
            // Il dettaglio (prezzo, importo, ...) e' in blu: si distingue dal nome a colpo
            // d'occhio senza bisogno di un carattere piu' grande, che qui non ci starebbe.
            graphics.setColor(Theme.ACCENT_BLUE);
            graphics.setFont(fitting(Theme.TILE_DETAIL_FONT, detail, usableWidth, graphics));
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
        final int y = height - TOKEN_DIAMETER - TOKEN_BOTTOM_MARGIN;

        graphics.setFont(Theme.TOKEN_FONT);
        final FontMetrics metrics = graphics.getFontMetrics();
        for (final Player player : this.occupants) {
            final Color tokenColor = ViewStyle.colorOf(player.getToken());
            graphics.setColor(tokenColor);
            graphics.fillOval(x, y, TOKEN_DIAMETER, TOKEN_DIAMETER);
            graphics.setColor(Theme.BORDER_STRONG);
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawOval(x, y, TOKEN_DIAMETER, TOKEN_DIAMETER);
            // Iniziale del nome: distingue le pedine anche a chi non ricorda i colori. E'
            // scura sulle pedine chiare e chiara su quelle scure, cosi' si legge sempre,
            // ed e' centrata sul disco in entrambe le direzioni.
            graphics.setColor(Theme.readableTextOn(tokenColor));
            final String initial = player.getName().substring(0, 1).toUpperCase(Locale.ROOT);
            final float center = TOKEN_DIAMETER / 2f;
            graphics.drawString(initial,
                    x + center - metrics.stringWidth(initial) / 2f,
                    y + center + (metrics.getAscent() - metrics.getDescent()) / 2f);
            x += spacing;
        }
    }

    /**
     * Traccia la cornice grigia: un filo sottile, che sulla casella del giocatore di turno
     * diventa una cornice spessa. Insieme alla tinta verde dello sfondo e' lo stesso
     * segno che distingue la scheda di quel giocatore.
     */
    private void paintOutline(final Graphics2D graphics, final int width, final int height) {
        final int thickness = this.current ? Theme.HIGHLIGHT_WIDTH : 1;
        graphics.setColor(Theme.BORDER_STRONG);
        // Rettangoli pieni e non tratti: con gli angoli netti restano precisi al pixel.
        graphics.fillRect(0, 0, width, thickness);
        graphics.fillRect(0, height - thickness, width, thickness);
        graphics.fillRect(0, 0, thickness, height);
        graphics.fillRect(width - thickness, 0, thickness, height);
    }

    /**
     * @return il carattere con cui il testo sta nella larghezza indicata: quello dato se
     *         ci sta gia', altrimenti una sua versione piu' piccola, fino a
     *         {@link #MIN_FONT_SIZE}
     */
    private static Font fitting(final Font font, final String text, final int maxWidth, final Graphics2D graphics) {
        final int textWidth = graphics.getFontMetrics(font).stringWidth(text);
        if (textWidth <= maxWidth) {
            return font;
        }
        return font.deriveFont(Math.max(MIN_FONT_SIZE, font.getSize2D() * maxWidth / textWidth));
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
