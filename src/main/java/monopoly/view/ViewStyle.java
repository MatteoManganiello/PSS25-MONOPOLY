package monopoly.view;

import java.awt.Color;
import java.awt.Font;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import monopoly.model.board.TileCategory;
import monopoly.model.game.GameState;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;
import monopoly.model.player.Token;

/**
 * Aspetto condiviso della GUI: colori, caratteri, formattazione dei numeri e le poche
 * regole di presentazione che piu' pannelli devono applicare allo stesso modo.
 * <p>
 * Tutte le scelte "estetiche" stanno qui invece di essere sparse nei pannelli, per
 * due motivi. Il primo e' pratico: cambiare la tinta di un tipo di casella o il
 * carattere delle etichette si fa in un punto solo. Il secondo riguarda l'MVC: e'
 * qui che i dati del model vengono tradotti in qualcosa di grafico (la stringa
 * "RED" della pedina diventa un {@link Color}, la {@link TileCategory} di una
 * casella diventa uno sfondo), cosi' il model non ha bisogno di conoscere Swing e
 * la view non ha bisogno di sapere di che classe sia una casella.
 * <p>
 * Classe di sola utilita': tutti i membri sono statici e non e' istanziabile.
 */
public final class ViewStyle {

    /** Verde del panno su cui e' appoggiato il tabellone. */
    public static final Color BOARD_BACKGROUND = new Color(0xC8, 0xE0, 0xC8);

    /** Sfondo neutro dei pannelli laterali e dei comandi. */
    public static final Color PANEL_BACKGROUND = new Color(0xF2, 0xF2, 0xEE);

    /** Colore delle cornici e del testo delle caselle. */
    public static final Color OUTLINE = new Color(0x33, 0x33, 0x33);

    /** Evidenziazione della casella su cui si trova il giocatore di turno. */
    public static final Color HIGHLIGHT = new Color(0xF5, 0xA6, 0x23);

    /** Colore del testo delle etichette informative. */
    public static final Color TEXT = new Color(0x22, 0x22, 0x22);

    /** Carattere del nome della casella. */
    public static final Font TILE_NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    /** Carattere della riga di dettaglio della casella. */
    public static final Font TILE_DETAIL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

    /** Carattere del nome dei giocatori nel pannello laterale. */
    public static final Font PLAYER_NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    /** Carattere delle informazioni secondarie (denaro, stato, proprieta'). */
    public static final Font PLAYER_INFO_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    /** Carattere del titolo al centro del tabellone. */
    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 28);

    /** Carattere a spaziatura fissa del log, cosi' le colonne del riepilogo restano allineate. */
    public static final Font LOG_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /** Sfondo di ogni famiglia di caselle. */
    private static final Map<TileCategory, Color> CATEGORY_COLORS = createCategoryColors();

    /** Traduzione dei nomi di colore usati dalle pedine ({@link Token#getColor()}). */
    private static final Map<String, Color> TOKEN_COLORS = createTokenColors();

    /** Colore usato quando il nome indicato da una pedina non e' riconosciuto. */
    private static final Color UNKNOWN_TOKEN_COLOR = new Color(0x60, 0x60, 0x60);

    /** Classe di utilita': non deve essere istanziata. */
    private ViewStyle() {
    }

    /**
     * Colore di sfondo di una casella.
     *
     * @param category la famiglia dichiarata dalla casella
     * @return il colore con cui disegnarla
     */
    public static Color colorOf(final TileCategory category) {
        return CATEGORY_COLORS.getOrDefault(category, Color.WHITE);
    }

    /**
     * Colore di una pedina.
     * <p>
     * La pedina descrive il proprio colore con una stringa ("RED", "BLUE", ...):
     * il model resta cosi' indipendente da Swing ed e' la view a fare la traduzione.
     *
     * @param token la pedina da disegnare
     * @return il colore corrispondente, un grigio scuro se il nome non e' riconosciuto
     */
    public static Color colorOf(final Token token) {
        if (token == null) {
            return UNKNOWN_TOKEN_COLOR;
        }
        return TOKEN_COLORS.getOrDefault(token.getColor().trim().toUpperCase(Locale.ROOT), UNKNOWN_TOKEN_COLOR);
    }

    /**
     * Colore con cui scrivere lo stato di un giocatore: verde se gioca, arancione se
     * e' in prigione, rosso se e' fallito.
     *
     * @param status lo stato del giocatore
     * @return il colore del testo
     */
    public static Color colorOf(final PlayerStatus status) {
        // Lo switch su enum e' esaustivo: aggiungendo uno stato il compilatore avvisa.
        return switch (status) {
            case PLAYING -> new Color(0x1B, 0x7F, 0x3B);
            case IN_JAIL -> new Color(0xC2, 0x6A, 0x00);
            case BANKRUPT -> new Color(0xB0, 0x1C, 0x1C);
        };
    }

    /**
     * Nome leggibile dello stato di un giocatore.
     * <p>
     * La traduzione in italiano sta nella view e non nell'enum del model: e' un
     * problema di presentazione, e un'altra interfaccia potrebbe volerla diversa.
     *
     * @param status lo stato del giocatore
     * @return la descrizione da mostrare
     */
    public static String describe(final PlayerStatus status) {
        return switch (status) {
            case PLAYING -> "in gioco";
            case IN_JAIL -> "in prigione";
            case BANKRUPT -> "fallito";
        };
    }

    /**
     * Formatta un importo con il separatore delle migliaia (1500 diventa "1.500").
     *
     * @param amount l'importo da mostrare
     * @return l'importo formattato
     */
    public static String formatMoney(final int amount) {
        return String.format(Locale.ITALY, "%,d", amount);
    }

    /**
     * Giocatore da indicare come "di turno" sul tabellone e nel pannello dei giocatori.
     * <p>
     * Di norma e' il giocatore corrente, ma {@link GameState#getCurrentPlayer()} non
     * avanza dopo un fallimento, e in due casi indicarlo sarebbe fuorviante:
     * <ul>
     *   <li>a partita finita, quando il "giocatore corrente" e' chi ha appena perso;</li>
     *   <li>quando il giocatore corrente e' fallito e deve solo passare la mano: la sua
     *       pedina e' gia' stata tolta dal tabellone.</li>
     * </ul>
     * In quei casi non si evidenzia nessuno. La regola sta qui, e non nei singoli
     * pannelli, perche' {@link BoardPanel} e {@link PlayerInfoPanel} devono dare la
     * stessa risposta.
     *
     * @param state lo stato della partita, usato in sola lettura
     * @return il giocatore da evidenziare, oppure {@link Optional#empty()} se non va
     *         evidenziato nessuno
     */
    public static Optional<Player> playerToHighlight(final GameState state) {
        final Player current = state.getCurrentPlayer();
        if (state.isGameOver() || current.isBankrupt()) {
            return Optional.empty();
        }
        return Optional.of(current);
    }

    /** Tabella "famiglia di casella - colore", alternativa a una catena di if nella view. */
    private static Map<TileCategory, Color> createCategoryColors() {
        final Map<TileCategory, Color> colors = new EnumMap<>(TileCategory.class);
        colors.put(TileCategory.START, new Color(0xB6, 0xE3, 0xB6));
        colors.put(TileCategory.PROPERTY, new Color(0xFA, 0xFA, 0xF5));
        colors.put(TileCategory.TAX, new Color(0xF3, 0xC7, 0xC7));
        colors.put(TileCategory.JAIL, new Color(0xD8, 0xC8, 0xB0));
        colors.put(TileCategory.GO_TO_JAIL, new Color(0xE8, 0xB0, 0x90));
        colors.put(TileCategory.FREE_PARKING, new Color(0xBF, 0xD8, 0xEE));
        colors.put(TileCategory.CARD, new Color(0xF7, 0xE9, 0xB0));
        colors.put(TileCategory.OTHER, Color.WHITE);
        return colors;
    }

    /** Nomi di colore accettati per le pedine. */
    private static Map<String, Color> createTokenColors() {
        final Map<String, Color> colors = new HashMap<>();
        colors.put("RED", new Color(0xD0, 0x27, 0x27));
        colors.put("BLUE", new Color(0x1E, 0x5A, 0xC8));
        colors.put("GREEN", new Color(0x1E, 0x8C, 0x3A));
        colors.put("YELLOW", new Color(0xE0, 0xB0, 0x00));
        colors.put("ORANGE", new Color(0xE8, 0x7A, 0x14));
        colors.put("PURPLE", new Color(0x7B, 0x3F, 0xA8));
        colors.put("MAGENTA", new Color(0xC0, 0x2A, 0x8F));
        colors.put("CYAN", new Color(0x11, 0x9A, 0xA8));
        colors.put("BROWN", new Color(0x8B, 0x5A, 0x2B));
        colors.put("BLACK", new Color(0x22, 0x22, 0x22));
        colors.put("GRAY", new Color(0x70, 0x70, 0x70));
        colors.put("PINK", new Color(0xE0, 0x6A, 0x9C));
        return colors;
    }
}
