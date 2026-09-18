package it.unibo.monopoly.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JPanel;

import it.unibo.monopoly.model.board.PropertyTile;
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
 * rispondere, e ogni sottoclasse risponde a modo suo. Gli importi del dettaglio
 * ricevono il simbolo dell'euro da {@link ViewStyle#detailOf(Tile)}.
 * <p>
 * Le eccezioni riguardano le informazioni che esistono solo per alcune caselle. Il
 * proprietario e l'affitto esistono solo per le caselle acquistabili: li' serve un
 * controllo di tipo su {@link PropertyTile}, ma si ferma a quella classe base. Anche
 * l'affitto mostrato e' una chiamata polimorfica ({@link PropertyTile#getCurrentRent()}):
 * il pannello chiede l'importo alla casella senza sapere con che regola lo calcoli. Il
 * colore della banda in alto - il gruppo per i terreni, un colore per le stazioni e uno
 * per le societa' - lo decide {@link ViewStyle#bandColorOf(it.unibo.monopoly.model.economy.Property)}: il pannello
 * non distingue da se' le famiglie di proprieta'.
 * <p>
 * Il colore del testo segue lo sfondo ({@link Theme#textOn(Color)},
 * {@link Theme#accentOn(Color)}): scuro, e con gli importi in blu, sulle caselle chiare;
 * piu' scuro ancora sulle tinte piene come le tasse, dove il blu non si leggerebbe.
 * <p>
 * Imprevisti e Probabilita' ({@link TileCategory#CARD}) sono invece interamente del
 * colore del loro mazzo, con il punto interrogativo e il nome piccoli al centro: si
 * riconoscono a colpo d'occhio fra tante caselle crema, e a dirlo basta il colore.
 * <p>
 * E' un componente passivo: non conosce il {@link it.unibo.monopoly.controller.GameEngine
 * GameEngine} e non modifica nulla. Riceve dal {@link BoardPanel} chi si trova sulla
 * casella ({@link #setOccupants(List)}) e si limita a ridisegnarsi. L'evidenziazione
 * della casella del giocatore di turno la disegna il {@link BoardPanel}, perche'
 * l'anello giallo sta appena fuori dalla casella.
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
     * ~800px. E' quanto basta perche', con i margini interni e le pedine grandi, i nomi
     * stiano su due righe.
     */
    private static final int PREFERRED_SIDE = 72;

    /**
     * Altezza della banda in alto sulle caselle acquistabili. Insieme a
     * {@link #BAND_GAP} lascia al nome le sue due righe anche sulle caselle piu' piccole.
     */
    private static final int BAND_HEIGHT = 10;

    /** Spazio fra la banda e la prima riga del nome. */
    private static final int BAND_GAP = 4;

    /** Diametro del segnalino del proprietario, disegnato dentro la banda. */
    private static final int OWNER_BADGE = 8;

    /** Diametro della pedina disegnata sulla casella: grande abbastanza da riconoscerla e leggerne l'iniziale. */
    private static final int TOKEN_DIAMETER = 19;

    /** Spessore dell'anello crema che stacca ogni pedina dallo sfondo e da quella accanto. */
    private static final int TOKEN_HALO = 1;

    /** Spazio fra due pedine affiancate, quando ci stanno senza sovrapporsi. */
    private static final int TOKEN_GAP = 2;

    /** Margine interno della casella: distanza di testo e segnalini dai bordi. */
    private static final int PADDING = 5;

    /**
     * Distanza delle pedine dal bordo inferiore: appena piu' dell'anello con cui il
     * tabellone evidenzia la casella di turno, che sborda anche sulle caselle vicine.
     * Cosi' le pedine, l'informazione piu' importante, non finiscono mai sotto l'anello.
     */
    private static final int TOKEN_BOTTOM_MARGIN = Theme.HIGHLIGHT_RING + 1;

    /** Misura minima a cui si puo' rimpicciolire una parola troppo lunga per la casella. */
    private static final float MIN_FONT_SIZE = 7f;

    /** Numero massimo di righe su cui spezzare il nome della casella. */
    private static final int MAX_NAME_LINES = 3;

    private final transient Tile tile;

    /** Giocatori attualmente fermi su questa casella, in ordine di turno. */
    private final transient List<Player> occupants;

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
     * Descrizione completa mostrata passando il mouse sulla casella: le informazioni
     * che sul quadratino non ci starebbero.
     *
     * @return il testo del tooltip
     */
    @Override
    public String getToolTipText() {
        final StringBuilder text = new StringBuilder(this.tile.getName());
        final String detail = ViewStyle.detailOf(this.tile);
        if (!detail.isEmpty()) {
            text.append(" - ").append(detail);
        }
        if (this.tile instanceof PropertyTile property) {
            // getCurrentRent() e' l'affitto che si pagherebbe adesso: tiene gia' conto del
            // monopolio di colore, delle stazioni possedute e dell'ultimo lancio di dadi.
            text.append(property.getOwner()
                    .map(owner -> " - di " + owner.getName()
                            + ", affitto " + ViewStyle.formatMoney(property.getCurrentRent()))
                    .orElse(" - in vendita"));
        }
        for (final Player player : this.occupants) {
            text.append(", ").append(player.getName());
        }
        return text.toString();
    }

    /**
     * Disegna la casella: sfondo, banda del gruppo con il segnalino del proprietario,
     * nome, dettaglio e pedine presenti; oppure, per Imprevisti e Probabilita', la
     * faccia colorata del mazzo.
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

            // Colore della casella: lo decide la categoria dichiarata dalla casella stessa
            // (e, per Imprevisti e Probabilita', il mazzo a cui appartiene).
            final Color background = ViewStyle.colorOf(this.tile);
            graphics.setColor(background);
            graphics.fillRect(0, 0, width, height);

            if (this.tile.getCategory() == TileCategory.CARD) {
                this.paintCardFace(graphics, width, height, background);
            } else {
                this.paintTexts(graphics, width, height, this.paintBand(graphics, width), background);
            }
            // La cornice prima delle pedine: le pedine, che sono l'informazione piu'
            // importante, devono restarle sopra.
            paintOutline(graphics, width, height);
            this.paintTokens(graphics, width, height);
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Disegna la banda superiore delle caselle acquistabili: il colore del gruppo per i
     * terreni, un colore proprio per le stazioni e uno per le societa'. Se la casella ha
     * un proprietario, dentro la banda compare il suo segnalino.
     *
     * @return l'altezza da cui puo' iniziare il testo
     */
    private int paintBand(final Graphics2D graphics, final int width) {
        if (!(this.tile instanceof PropertyTile property)) {
            return PADDING;
        }
        graphics.setColor(ViewStyle.bandColorOf(property));
        graphics.fillRect(0, 0, width, BAND_HEIGHT);
        graphics.setColor(Theme.BORDER_STRONG);
        graphics.drawLine(0, BAND_HEIGHT, width, BAND_HEIGHT);
        property.getOwner().ifPresent(owner -> paintOwnerBadge(graphics, width, ViewStyle.colorOf(owner.getToken())));
        return BAND_HEIGHT + BAND_GAP;
    }

    /**
     * Faccia di Imprevisti e Probabilita': sullo sfondo del colore del mazzo, il simbolo
     * della casella ({@link Tile#getDetail()}, il punto interrogativo) e sotto il nome,
     * entrambi piccoli e centrati nello spazio sopra le pedine. Il colore della scritta
     * lo sceglie {@link Theme#textOn(Color)}: scuro sull'arancione, inchiostro
     * sull'azzurro, dove il testo scuro normale non basterebbe.
     */
    private void paintCardFace(final Graphics2D graphics, final int width, final int height,
                               final Color background) {
        final String symbol = this.tile.getDetail();
        final FontMetrics symbolMetrics = graphics.getFontMetrics(Theme.SYMBOL_FONT);
        final FontMetrics nameMetrics = graphics.getFontMetrics(Theme.CARD_NAME_FONT);
        final int usableWidth = width - 2 * PADDING;
        final int bottom = tokenTop(height);
        final int symbolHeight = symbol.isEmpty() ? 0 : symbolMetrics.getHeight();
        final int maxLines = Math.max(1, Math.min(MAX_NAME_LINES,
                (bottom - PADDING - symbolHeight) / nameMetrics.getHeight()));
        final List<String> lines = wrap(this.tile.getName(), nameMetrics, usableWidth, maxLines);

        // Simbolo e nome formano un blocco solo, centrato in verticale sopra le pedine.
        final int blockHeight = symbolHeight + lines.size() * nameMetrics.getHeight();
        int top = PADDING + Math.max(0, (bottom - PADDING - blockHeight) / 2);
        graphics.setColor(Theme.textOn(background));
        if (!symbol.isEmpty()) {
            graphics.setFont(Theme.SYMBOL_FONT);
            graphics.drawString(symbol, centeredX(symbol, symbolMetrics, width), top + symbolMetrics.getAscent());
            top += symbolHeight;
        }
        for (final String line : lines) {
            graphics.setFont(fitting(Theme.CARD_NAME_FONT, line, usableWidth, graphics));
            graphics.drawString(line, centeredX(line, graphics.getFontMetrics(), width), top + nameMetrics.getAscent());
            top += nameMetrics.getHeight();
        }
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
    private void paintTexts(final Graphics2D graphics, final int width, final int height, final int textTop,
                            final Color background) {
        graphics.setColor(Theme.textOn(background));
        graphics.setFont(Theme.TILE_NAME_FONT);
        final FontMetrics nameMetrics = graphics.getFontMetrics();
        final int usableWidth = width - 2 * PADDING;
        final String detail = ViewStyle.detailOf(this.tile);
        final int detailAscent = detail.isEmpty() ? 0
                : graphics.getFontMetrics(Theme.TILE_DETAIL_FONT).getAscent();

        final int spaceForName = tokenTop(height) - textTop - detailAscent;
        final int maxLines = Math.max(1, Math.min(MAX_NAME_LINES, spaceForName / nameMetrics.getHeight()));

        int baseline = textTop + nameMetrics.getAscent();
        for (final String line : wrap(this.tile.getName(), nameMetrics, usableWidth, maxLines)) {
            graphics.setFont(fitting(Theme.TILE_NAME_FONT, line, usableWidth, graphics));
            graphics.drawString(line, centeredX(line, graphics.getFontMetrics(), width), baseline);
            baseline += nameMetrics.getHeight();
        }

        if (!detail.isEmpty()) {
            // Il dettaglio (prezzo, importo, ...) e' in blu dove si legge: si distingue dal
            // nome a colpo d'occhio senza bisogno di un carattere piu' grande, che qui non
            // ci starebbe. Sulle tinte piene prende invece il colore del nome.
            graphics.setColor(Theme.accentOn(background));
            graphics.setFont(fitting(Theme.TILE_DETAIL_FONT, detail, usableWidth, graphics));
            final FontMetrics detailMetrics = graphics.getFontMetrics();
            // Appena sopra le pedine se il nome lo consente, subito sotto al nome
            // altrimenti; mai oltre il bordo inferiore. Dopo il ciclo "baseline" e' gia'
            // la linea di base della riga libera successiva: la riga di dettaglio parte
            // dalla cima di quella riga, senza lasciare una riga vuota in mezzo.
            final int belowName = baseline - nameMetrics.getAscent() + detailMetrics.getAscent();
            final int detailBaseline = Math.min(height - PADDING - detailMetrics.getDescent(),
                    Math.max(belowName, tokenTop(height) - 1));
            graphics.drawString(detail, centeredX(detail, detailMetrics, width), detailBaseline);
        }
    }

    /**
     * Disegna una pedina per ogni giocatore presente, con l'iniziale del suo nome.
     * <p>
     * Le pedine sono centrate nella casella e stanno affiancate finche' c'e' posto.
     * Quando sono troppe per la larghezza della casella (da quattro giocatori in su) si
     * sovrappongono in parte, come fiches impilate, invece di uscire dal bordo: il
     * colore resta visibile e i nomi completi sono nel tooltip.
     */
    private void paintTokens(final Graphics2D graphics, final int width, final int height) {
        if (this.occupants.isEmpty()) {
            return;
        }
        final int count = this.occupants.size();
        // Spazio in cui possono iniziare le pedine successive alla prima.
        final int available = width - 2 * PADDING - TOKEN_DIAMETER;
        final int spacing = count == 1 ? 0
                : Math.max(1, Math.min(TOKEN_DIAMETER + TOKEN_GAP, available / (count - 1)));
        final int totalWidth = (count - 1) * spacing + TOKEN_DIAMETER;
        int x = Math.max(PADDING, (width - totalWidth) / 2);
        final int y = tokenTop(height);

        // I cerchi non vengono allineati ai pixel: restano tondi e con il bordo netto.
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setStroke(new BasicStroke(1f));
        graphics.setFont(Theme.TOKEN_FONT);
        final FontMetrics metrics = graphics.getFontMetrics();
        for (final Player player : this.occupants) {
            final Color tokenColor = ViewStyle.colorOf(player.getToken());
            // L'anello crema stacca la pedina da qualunque sfondo: dall'arancione di
            // Imprevisti, da una banda del suo stesso colore, dalla pedina accanto.
            graphics.setColor(Theme.CREAM);
            graphics.fill(new Ellipse2D.Float(x - TOKEN_HALO, y - TOKEN_HALO,
                    TOKEN_DIAMETER + 2 * TOKEN_HALO, TOKEN_DIAMETER + 2 * TOKEN_HALO));
            graphics.setColor(tokenColor);
            graphics.fill(new Ellipse2D.Float(x, y, TOKEN_DIAMETER, TOKEN_DIAMETER));
            // Il contorno sta dentro il disco, non a cavallo del suo bordo: niente sbavature.
            graphics.setColor(Theme.BORDER_STRONG);
            graphics.draw(new Ellipse2D.Float(x + 0.5f, y + 0.5f, TOKEN_DIAMETER - 1, TOKEN_DIAMETER - 1));
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
     * Traccia il filo grigio che separa la casella dalle vicine. L'evidenziazione della
     * casella di turno non e' qui: la disegna il {@link BoardPanel}, appena fuori dal filo.
     */
    private static void paintOutline(final Graphics2D graphics, final int width, final int height) {
        graphics.setColor(Theme.BORDER_STRONG);
        // Rettangoli pieni e non tratti: con gli angoli netti restano precisi al pixel.
        graphics.fillRect(0, 0, width, 1);
        graphics.fillRect(0, height - 1, width, 1);
        graphics.fillRect(0, 0, 1, height);
        graphics.fillRect(width - 1, 0, 1, height);
    }

    /** @return l'ordinata a cui iniziano le pedine: sopra, lo spazio per i testi */
    private static int tokenTop(final int height) {
        return height - TOKEN_DIAMETER - TOKEN_BOTTOM_MARGIN;
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
