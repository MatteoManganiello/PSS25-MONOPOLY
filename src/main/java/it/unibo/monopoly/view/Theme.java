package it.unibo.monopoly.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.text.JTextComponent;

/**
 * Tema grafico della GUI: la tavolozza, i caratteri, le misure, i bordi e lo stile dei
 * pulsanti.
 * <p>
 * E' l'unico punto della view in cui compaiono valori grafici: tutti i colori e i
 * caratteri sono costanti di questa classe, e i pannelli si limitano a leggerle. Cosi'
 * la gerarchia visiva resta la stessa in tutte le finestre e un ritocco alla tavolozza
 * si fa in un posto solo. {@link ViewStyle} fa un lavoro diverso: traduce i dati del
 * model (la categoria di una casella, il colore di una pedina, lo stato di un
 * giocatore) in questi colori.
 * <p>
 * <b>Regole del tema.</b> Le superfici (caselle, schede, riquadri) sono crema e il testo
 * che ci sta sopra e' scuro; i verdi fanno solo da sfondo. I bordi sono sempre grigi,
 * con gli angoli netti. Il rosso e' riservato all'azione principale, il blu agli
 * importi e alle selezioni.
 * <p>
 * <b>Contrasto.</b> Tutte le coppie testo/sfondo della GUI rispettano il livello AA delle
 * linee guida WCAG (almeno 4,5:1): testo scuro su crema 11,9:1, testo scuro sul verde
 * salvia 5:1, blu su crema 7,3:1, crema su rosso 4,6:1. Il crema sui due verdi resta
 * sotto la soglia (2,4:1 e 4,2:1), per questo sui verdi non si scrive in chiaro: il
 * turno al centro del tabellone, per esempio, sta su una scheda crema.
 * <p>
 * Classe di sola utilita': tutti i membri sono statici e non e' istanziabile.
 */
public final class Theme {

    // ------------------------------------------------------------------
    // Tavolozza
    // ------------------------------------------------------------------

    /** Verde salvia del tavolo: sfondo principale delle finestre. */
    public static final Color TABLE_GREEN = new Color(0x6BA588);

    /** Verde scuro, solo dove serve profondita': il centro del tabellone. */
    public static final Color DARK_GREEN = new Color(0x3F7A5C);

    /** Crema avorio delle superfici: caselle, schede, riquadri, pulsanti secondari. */
    public static final Color CREAM = new Color(0xF4EAD5);

    /** Grigio medio dei bordi normali. */
    public static final Color BORDER = new Color(0x9AA0A6);

    /** Grigio scuro dei bordi marcati: caselle, pulsanti, giocatore di turno. */
    public static final Color BORDER_STRONG = new Color(0x6E6E6E);

    /** Rosso Monopoly: azione principale ed enfasi. */
    public static final Color MONOPOLY_RED = new Color(0xC0392B);

    /** Blu di accento secondario: importi, dettagli delle caselle, testo selezionato. */
    public static final Color ACCENT_BLUE = new Color(0x1F4E79);

    /** Testo scuro, da usare sulle superfici crema e sul verde salvia. */
    public static final Color TEXT_DARK = new Color(0x2B2B2B);

    /** Testo chiaro, da usare sul rosso. */
    public static final Color TEXT_LIGHT = CREAM;

    // ------------------------------------------------------------------
    // Bande dei gruppi di colore dei terreni (colori classici del gioco)
    // ------------------------------------------------------------------

    /** Gruppo marrone. */
    public static final Color GROUP_BROWN = new Color(0x955436);

    /** Gruppo azzurro. */
    public static final Color GROUP_LIGHT_BLUE = new Color(0xAAE0FA);

    /** Gruppo rosa. */
    public static final Color GROUP_PINK = new Color(0xD93A96);

    /** Gruppo arancione. */
    public static final Color GROUP_ORANGE = new Color(0xF7941D);

    /** Gruppo rosso. */
    public static final Color GROUP_RED = new Color(0xED1B24);

    /** Gruppo giallo. */
    public static final Color GROUP_YELLOW = new Color(0xFEF200);

    /** Gruppo verde. */
    public static final Color GROUP_GREEN = new Color(0x1FB25A);

    /** Gruppo blu. */
    public static final Color GROUP_BLUE = new Color(0x0072BB);

    // ------------------------------------------------------------------
    // Caselle speciali: un colore pieno, riconoscibile a colpo d'occhio
    // ------------------------------------------------------------------

    /** Imprevisti. */
    public static final Color CHANCE = new Color(0xF7941D);

    /** Probabilita'. */
    public static final Color COMMUNITY_CHEST = new Color(0x4A90D9);

    // ------------------------------------------------------------------
    // Tinte delle caselle: la tavolozza sciolta nel crema
    // ------------------------------------------------------------------

    /** Proprieta' (e ogni casella senza una tinta propria): crema pieno. */
    public static final Color TILE_PLAIN = CREAM;

    /** Il "Via": crema con un velo di verde. */
    public static final Color TILE_START = mix(CREAM, TABLE_GREEN, 0.25);

    /** Tasse: crema con un velo di rosso. */
    public static final Color TILE_TAX = mix(CREAM, MONOPOLY_RED, 0.16);

    /** Prigione: crema con un velo di grigio. */
    public static final Color TILE_JAIL = mix(CREAM, BORDER_STRONG, 0.18);

    /** "Vai in prigione": il rosso piu' deciso del tabellone, ma sempre chiaro. */
    public static final Color TILE_GO_TO_JAIL = mix(CREAM, MONOPOLY_RED, 0.30);

    /** Posteggio gratuito: crema con un velo di blu. */
    public static final Color TILE_FREE_PARKING = mix(CREAM, ACCENT_BLUE, 0.14);

    /** Banda delle proprieta' senza gruppo di colore (stazioni e societa'). */
    public static final Color NEUTRAL_BAND = mix(CREAM, BORDER_STRONG, 0.22);

    /**
     * Sfondo di cio' che riguarda il giocatore di turno (la sua scheda e la casella su
     * cui si trova): crema appena velato dal verde del tavolo.
     */
    public static final Color CURRENT_SURFACE = mix(CREAM, TABLE_GREEN, 0.22);

    // ------------------------------------------------------------------
    // Stati del giocatore: testo su crema, tutti sopra 4,5:1
    // ------------------------------------------------------------------

    /** Giocatore in partita: il verde scuro, scurito quanto basta per leggersi sul crema. */
    public static final Color STATUS_PLAYING = mix(DARK_GREEN, TEXT_DARK, 0.30);

    /** Giocatore in prigione: un marrone ambrato. */
    public static final Color STATUS_IN_JAIL = new Color(0x7A4E00);

    /** Giocatore fallito. */
    public static final Color STATUS_BANKRUPT = MONOPOLY_RED;

    // ------------------------------------------------------------------
    // Pedine
    // ------------------------------------------------------------------

    /** Pedina rossa. */
    public static final Color TOKEN_RED = new Color(0xD02727);

    /** Pedina blu. */
    public static final Color TOKEN_BLUE = new Color(0x1E5AC8);

    /** Pedina verde. */
    public static final Color TOKEN_GREEN = new Color(0x1E8C3A);

    /** Pedina gialla. */
    public static final Color TOKEN_YELLOW = new Color(0xE0B000);

    /** Pedina arancione. */
    public static final Color TOKEN_ORANGE = new Color(0xE87A14);

    /** Pedina viola. */
    public static final Color TOKEN_PURPLE = new Color(0x7B3FA8);

    /** Pedina magenta. */
    public static final Color TOKEN_MAGENTA = new Color(0xC02A8F);

    /** Pedina ciano. */
    public static final Color TOKEN_CYAN = new Color(0x119AA8);

    /** Pedina marrone. */
    public static final Color TOKEN_BROWN = new Color(0x8B5A2B);

    /** Pedina nera. */
    public static final Color TOKEN_BLACK = new Color(0x222222);

    /** Pedina grigia. */
    public static final Color TOKEN_GRAY = new Color(0x707070);

    /** Pedina rosa. */
    public static final Color TOKEN_PINK = new Color(0xE06A9C);

    /** Pedina con un nome di colore sconosciuto. */
    public static final Color TOKEN_UNKNOWN = new Color(0x606060);

    // ------------------------------------------------------------------
    // Caratteri: una sola famiglia sans, pochi pesi e poche misure
    // ------------------------------------------------------------------

    /** Titolo principale: la scritta "MONOPOLY". */
    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 28);

    /** Importo della cassa della banca: il numero piu' grande della schermata di gioco. */
    public static final Font AMOUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);

    /** Sottotitoli delle schermate, come la domanda "Chi gioca?" del setup. */
    public static final Font SUBTITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    /** Simbolo al centro delle caselle Imprevisti e Probabilita'. */
    public static final Font SYMBOL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    /** Intestazioni dei riquadri e giocatore di turno al centro del tabellone. */
    public static final Font HEADING_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    /** Nome del giocatore sulla sua scheda. */
    public static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    /** Etichette dei pulsanti. */
    public static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    /** Testo informativo. */
    public static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    /** Testo informativo da far risaltare (importi, stato, titoli dei riquadri). */
    public static final Font BODY_BOLD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    /** Iniziale del giocatore sulla pedina. */
    public static final Font TOKEN_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);

    /** Nome della casella. */
    public static final Font TILE_NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    /** Riga di dettaglio della casella (prezzo, importo, ...). */
    public static final Font TILE_DETAIL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    /** Carattere a spaziatura fissa del log, cosi' le colonne del riepilogo restano allineate. */
    public static final Font LOG_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    // ------------------------------------------------------------------
    // Misure
    // ------------------------------------------------------------------

    /**
     * Arrotondamento (diametro dell'arco) di schede, pulsanti e riquadri: zero, cioe'
     * angoli netti. E' una costante sola, cosi' l'aspetto resta uguale ovunque.
     */
    public static final int RADIUS = 0;

    /** Arrotondamento dei dadi, l'unico oggetto che resta smussato come quelli veri. */
    public static final int SMALL_RADIUS = 6;

    /** Spazio standard fra due elementi vicini. */
    public static final int GAP = 8;

    /** Margine interno standard di pannelli e schede. */
    public static final int PADDING = 10;

    /** Spessore del bordo che indica il giocatore di turno e la sua casella. */
    public static final int HIGHLIGHT_WIDTH = 3;

    // ------------------------------------------------------------------
    // Pulsanti
    // ------------------------------------------------------------------

    /*
     * Un pulsante disabilitato si ricava dalla superficie su cui poggia, spostata di poco
     * verso il testo che le si scrive sopra: sul crema diventa un crema appena piu' scuro
     * con il testo grigio. "Rientra" nello sfondo e si capisce che non e' attivo.
     */

    /** Quanto il riempimento di un pulsante disabilitato si stacca dalla superficie. */
    private static final double DISABLED_FILL_SHIFT = 0.08;

    /** Quanto il contorno di un pulsante disabilitato si stacca dalla superficie. */
    private static final double DISABLED_OUTLINE_SHIFT = 0.25;

    /** Quanto il testo di un pulsante disabilitato si stacca dalla superficie. */
    private static final double DISABLED_TEXT_SHIFT = 0.50;

    /** Azione principale: rosso pieno con il testo crema, piu' scuro al passaggio del mouse. */
    private static final ButtonColors PRIMARY = new ButtonColors(
            MONOPOLY_RED,
            mix(MONOPOLY_RED, Color.BLACK, 0.15),
            mix(MONOPOLY_RED, Color.BLACK, 0.30),
            TEXT_LIGHT,
            null);

    /** Azione secondaria: crema con il bordo grigio marcato, velata di grigio al passaggio del mouse. */
    private static final ButtonColors SECONDARY = new ButtonColors(
            CREAM,
            mix(CREAM, BORDER, 0.30),
            mix(CREAM, BORDER, 0.55),
            TEXT_DARK,
            BORDER_STRONG);

    /** Margine interno dei pulsanti, sopra e sotto il testo. */
    private static final int BUTTON_PADDING_V = 8;

    /** Margine interno dei pulsanti, ai lati del testo. */
    private static final int BUTTON_PADDING_H = 12;

    /** Spessore del contorno dei pulsanti. */
    private static final float BUTTON_OUTLINE = 1f;

    /** Distanza dal bordo dell'anello che indica il pulsante con il focus. */
    private static final int FOCUS_INSET = 3;

    // ------------------------------------------------------------------
    // Coefficienti del contrasto WCAG
    // ------------------------------------------------------------------

    private static final double RED_WEIGHT = 0.2126;
    private static final double GREEN_WEIGHT = 0.7152;
    private static final double BLUE_WEIGHT = 0.0722;
    private static final double LINEAR_THRESHOLD = 0.03928;
    private static final double LINEAR_DIVISOR = 12.92;
    private static final double GAMMA_OFFSET = 0.055;
    private static final double GAMMA_DIVISOR = 1.055;
    private static final double GAMMA = 2.4;
    private static final double FLARE = 0.05;
    private static final double MAX_CHANNEL = 255.0;

    /** Classe di utilita': non deve essere istanziata. */
    private Theme() {
    }

    // ------------------------------------------------------------------
    // Colori
    // ------------------------------------------------------------------

    /**
     * Mescola due colori.
     * <p>
     * E' il modo in cui il tema ricava le tinte (per esempio lo sfondo delle tasse e'
     * il crema con un po' di rosso) invece di aggiungere altri valori scritti a mano:
     * cosi' restano sempre in armonia con la tavolozza.
     *
     * @param base  il colore di partenza
     * @param other il colore da aggiungere
     * @param ratio quanto del secondo colore usare, da 0 (solo il primo) a 1 (solo il secondo)
     * @return il colore risultante
     */
    public static Color mix(final Color base, final Color other, final double ratio) {
        return new Color(
                blend(base.getRed(), other.getRed(), ratio),
                blend(base.getGreen(), other.getGreen(), ratio),
                blend(base.getBlue(), other.getBlue(), ratio));
    }

    /**
     * Colore del testo da scrivere sopra uno sfondo qualsiasi: scuro o chiaro, quello
     * dei due che contrasta di piu'.
     * <p>
     * Serve per gli sfondi che non sceglie il tema, come il colore di una pedina: la
     * sua iniziale resta leggibile sia sulla pedina gialla sia su quella nera.
     *
     * @param background lo sfondo
     * @return {@link #TEXT_DARK} oppure {@link #TEXT_LIGHT}
     */
    public static Color readableTextOn(final Color background) {
        return contrast(TEXT_DARK, background) >= contrast(TEXT_LIGHT, background) ? TEXT_DARK : TEXT_LIGHT;
    }

    // ------------------------------------------------------------------
    // Componenti
    // ------------------------------------------------------------------

    /**
     * Crea un'etichetta gia' con carattere e colore del tema.
     *
     * @param text  il testo
     * @param font  il carattere, preso fra le costanti di questa classe
     * @param color il colore del testo
     * @return l'etichetta
     */
    public static JLabel label(final String text, final Font font, final Color color) {
        final JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * Crea una scheda crema con il bordo grigio: la superficie standard della GUI.
     *
     * @param layout la disposizione dei componenti interni
     * @return la scheda, gia' con il margine interno standard
     */
    public static CardPanel card(final LayoutManager layout) {
        final CardPanel card = new CardPanel(layout, BORDER, 1);
        card.setBackground(CREAM);
        card.setBorder(padding(PADDING));
        return card;
    }

    /**
     * Crea la fascia con il nome del gioco: rossa con la scritta crema, come il marchio
     * stampato sui tabelloni veri. E' la stessa al centro del tabellone e in cima alla
     * schermata di setup, cosi' le due finestre si riconoscono come parti dello stesso
     * gioco.
     * <p>
     * La fascia resta delle dimensioni della scritta anche in un {@code BoxLayout}, che
     * altrimenti la allargherebbe a tutto lo spazio disponibile.
     *
     * @return la fascia, gia' centrata orizzontalmente nel suo contenitore
     */
    public static JPanel titleBanner() {
        final CardPanel banner = new CardPanel(new BorderLayout(), null, 0);
        banner.setBackground(MONOPOLY_RED);
        banner.setBorder(BorderFactory.createEmptyBorder(GAP, 3 * GAP, GAP, 3 * GAP));
        final JLabel title = label("MONOPOLY", TITLE_FONT, TEXT_LIGHT);
        title.setHorizontalAlignment(JLabel.CENTER);
        banner.add(title, BorderLayout.CENTER);
        banner.setAlignmentX(Component.CENTER_ALIGNMENT);
        banner.setMaximumSize(banner.getPreferredSize());
        return banner;
    }

    /**
     * Da' a un pulsante lo stile dell'azione principale: rosso pieno e testo crema.
     *
     * @param button il pulsante da stilizzare
     */
    public static void stylePrimary(final AbstractButton button) {
        styleButton(button, PRIMARY);
    }

    /**
     * Da' a un pulsante lo stile delle azioni secondarie: crema, testo scuro e bordo
     * grigio marcato.
     *
     * @param button il pulsante da stilizzare
     */
    public static void styleSecondary(final AbstractButton button) {
        styleButton(button, SECONDARY);
    }

    /**
     * Colori del testo selezionato in un campo o in un'area di testo: crema su blu,
     * come la selezione di un elenco.
     *
     * @param component il componente di testo
     */
    public static void styleSelection(final JTextComponent component) {
        component.setSelectionColor(ACCENT_BLUE);
        component.setSelectedTextColor(TEXT_LIGHT);
        component.setCaretColor(TEXT_DARK);
    }

    /**
     * Bordo di un riquadro con titolo, da usare sulle schede crema: linea grigia e
     * titolo scuro in grassetto.
     *
     * @param title il titolo del riquadro
     * @return il bordo, con il margine interno gia' compreso
     */
    public static Border sectionBorder(final String title) {
        final TitledBorder titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title,
                TitledBorder.LEADING, TitledBorder.TOP, BODY_BOLD_FONT, TEXT_DARK);
        return BorderFactory.createCompoundBorder(titled,
                BorderFactory.createEmptyBorder(GAP / 2, GAP, GAP, GAP));
    }

    /**
     * Cornice del tabellone: una linea grigia marcata, come il bordo stampato di un
     * tabellone vero.
     *
     * @return il bordo
     */
    public static Border boardBorder() {
        return BorderFactory.createLineBorder(BORDER_STRONG, HIGHLIGHT_WIDTH);
    }

    /**
     * Margine interno uguale sui quattro lati.
     *
     * @param size lo spessore, in pixel
     * @return il bordo vuoto
     */
    public static Border padding(final int size) {
        return BorderFactory.createEmptyBorder(size, size, size, size);
    }

    /**
     * Prepara un contesto grafico per disegnare forme e testo senza scalettature.
     *
     * @param graphics il contesto da configurare
     */
    public static void antialias(final Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Le linee vengono allineate ai pixel: i bordi netti non si sfocano.
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    // ------------------------------------------------------------------
    // Implementazione
    // ------------------------------------------------------------------

    /**
     * Applica a un pulsante l'aspetto del tema.
     * <p>
     * Il look and feel di sistema (su macOS, Aqua) ignora il colore di sfondo dei
     * pulsanti e li disegna sempre alla sua maniera: per avere i colori del tema serve
     * un disegnatore proprio ({@link ThemedButtonUI}). Il passaggio del mouse non
     * richiede ascoltatori aggiunti: con {@code rolloverEnabled} e' il
     * {@link ButtonModel} a registrare lo stato "mouse sopra", e il disegnatore di base
     * di Swing ridisegna il pulsante a ogni cambio di stato.
     * <p>
     * Tutto cio' che non e' aspetto - testo, azioni collegate, pulsante predefinito
     * della finestra, tasti - resta com'era.
     */
    private static void styleButton(final AbstractButton button, final ButtonColors colors) {
        button.setUI(new ThemedButtonUI(colors));
        button.setFont(BUTTON_FONT);
        button.setForeground(colors.text());
        button.setBorder(BorderFactory.createEmptyBorder(
                BUTTON_PADDING_V, BUTTON_PADDING_H, BUTTON_PADDING_V, BUTTON_PADDING_H));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(true);
        button.setRolloverEnabled(true);
    }

    /** Una componente di colore mescolata, arrotondata all'intero. */
    private static int blend(final int from, final int to, final double ratio) {
        return (int) Math.round(from + (to - from) * ratio);
    }

    /** Rapporto di contrasto WCAG fra due colori, da 1 (identici) a 21 (nero su bianco). */
    private static double contrast(final Color first, final Color second) {
        final double lighter = Math.max(luminance(first), luminance(second));
        final double darker = Math.min(luminance(first), luminance(second));
        return (lighter + FLARE) / (darker + FLARE);
    }

    /** Luminanza relativa di un colore, secondo la definizione WCAG. */
    private static double luminance(final Color color) {
        return RED_WEIGHT * linear(color.getRed())
                + GREEN_WEIGHT * linear(color.getGreen())
                + BLUE_WEIGHT * linear(color.getBlue());
    }

    /** Converte una componente di colore da scala sRGB a scala lineare. */
    private static double linear(final int channel) {
        final double value = channel / MAX_CHANNEL;
        return value <= LINEAR_THRESHOLD
                ? value / LINEAR_DIVISOR
                : Math.pow((value + GAMMA_OFFSET) / GAMMA_DIVISOR, GAMMA);
    }

    /**
     * I colori di un tipo di pulsante nei suoi stati.
     *
     * @param fill    riempimento normale
     * @param hover   riempimento con il mouse sopra
     * @param pressed riempimento mentre viene premuto
     * @param text    colore del testo
     * @param outline contorno, oppure {@code null} se il pulsante non ne ha
     */
    private record ButtonColors(Color fill, Color hover, Color pressed, Color text, Color outline) {
    }

    /**
     * Disegnatore dei pulsanti del tema: rettangolo colorato secondo lo stato, con il
     * contorno se previsto, testo spento se il pulsante e' disabilitato e un anello del
     * colore del testo quando il pulsante ha il focus della tastiera.
     * <p>
     * Estende {@link BasicButtonUI}, che resta responsabile di tutto il resto:
     * disposizione di testo e icona, gestione di mouse e tastiera, ridisegno a ogni
     * cambio di stato.
     */
    private static final class ThemedButtonUI extends BasicButtonUI {

        private final ButtonColors colors;

        ThemedButtonUI(final ButtonColors colors) {
            super();
            this.colors = colors;
        }

        @Override
        public void paint(final Graphics g, final JComponent component) {
            final ButtonModel model = ((AbstractButton) component).getModel();
            final Graphics2D graphics = (Graphics2D) g.create();
            try {
                antialias(graphics);
                final Color outline = model.isEnabled()
                        ? this.colors.outline()
                        : disabled(component, DISABLED_OUTLINE_SHIFT);
                final float inset = outline == null ? 0f : BUTTON_OUTLINE / 2;
                final RoundRectangle2D shape = new RoundRectangle2D.Float(inset, inset,
                        component.getWidth() - 2 * inset, component.getHeight() - 2 * inset,
                        RADIUS, RADIUS);
                graphics.setColor(model.isEnabled() ? this.fillFor(model) : disabled(component, DISABLED_FILL_SHIFT));
                graphics.fill(shape);
                if (outline != null) {
                    graphics.setColor(outline);
                    graphics.setStroke(new BasicStroke(BUTTON_OUTLINE));
                    graphics.draw(shape);
                }
            } finally {
                graphics.dispose();
            }
            super.paint(g, component);
        }

        @Override
        protected void paintText(final Graphics g, final JComponent component,
                                 final Rectangle textRect, final String text) {
            final AbstractButton button = (AbstractButton) component;
            g.setColor(button.isEnabled() ? button.getForeground() : disabled(component, DISABLED_TEXT_SHIFT));
            BasicGraphicsUtils.drawStringUnderlineCharAt(component, (Graphics2D) g, text,
                    button.getDisplayedMnemonicIndex(), textRect.x,
                    textRect.y + component.getFontMetrics(component.getFont()).getAscent());
        }

        @Override
        protected void paintFocus(final Graphics g, final AbstractButton button, final Rectangle viewRect,
                                  final Rectangle textRect, final Rectangle iconRect) {
            final Graphics2D graphics = (Graphics2D) g.create();
            try {
                antialias(graphics);
                graphics.setColor(button.getForeground());
                final int arc = Math.max(0, RADIUS - FOCUS_INSET);
                graphics.drawRoundRect(FOCUS_INSET, FOCUS_INSET,
                        button.getWidth() - 2 * FOCUS_INSET - 1, button.getHeight() - 2 * FOCUS_INSET - 1,
                        arc, arc);
            } finally {
                graphics.dispose();
            }
        }

        /** Riempimento di un pulsante attivo: premuto, sotto il mouse o normale. */
        private Color fillFor(final ButtonModel model) {
            if (model.isPressed() && model.isArmed()) {
                return this.colors.pressed();
            }
            return model.isRollover() ? this.colors.hover() : this.colors.fill();
        }

        /**
         * Colore di una parte del pulsante disabilitato: la superficie sottostante,
         * spostata verso il colore del testo leggibile su di essa.
         */
        private static Color disabled(final Component button, final double shift) {
            final Color surface = surfaceBehind(button);
            return mix(surface, readableTextOn(surface), shift);
        }

        /**
         * Il colore su cui poggia il pulsante: lo sfondo del primo contenitore che lo
         * dipinge davvero. Le righe trasparenti vanno saltate, e una {@link CardPanel}
         * conta anche se non e' opaca, perche' il suo sfondo lo disegna lei.
         */
        private static Color surfaceBehind(final Component button) {
            for (Container parent = button.getParent(); parent != null; parent = parent.getParent()) {
                if (parent.isOpaque() || parent instanceof CardPanel) {
                    return parent.getBackground();
                }
            }
            return CREAM;
        }
    }
}
