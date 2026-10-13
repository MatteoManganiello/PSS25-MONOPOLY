package it.unibo.monopoly.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.swing.Icon;

import it.unibo.monopoly.controller.SetupProblem;
import it.unibo.monopoly.model.board.ColorGroup;
import it.unibo.monopoly.model.board.StationTile;
import it.unibo.monopoly.model.board.StreetTile;
import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.board.TileCategory;
import it.unibo.monopoly.model.board.UtilityTile;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;
import it.unibo.monopoly.model.player.PlayerStatus;
import it.unibo.monopoly.model.player.Token;

/**
 * Presentazione dei dati del model: come si mostra una casella, una pedina, lo stato
 * di un giocatore, un importo, e le poche regole di presentazione che piu' pannelli
 * devono applicare allo stesso modo.
 * <p>
 * I valori grafici veri e propri (tavolozza, caratteri, bordi) stanno in {@link Theme};
 * questa classe fa da ponte fra il model e il tema, ed e' il motivo per cui esiste
 * nell'MVC: qui i dati del model vengono tradotti in qualcosa di grafico (la stringa
 * "RED" della pedina diventa un {@link Color}, la {@link TileCategory} di una casella
 * diventa uno sfondo, il {@link ColorGroup} di un terreno il colore della sua banda),
 * cosi' il model non ha bisogno di conoscere Swing e la view non ha bisogno di sapere
 * di che classe sia una casella.
 * <p>
 * Classe di sola utilita': tutti i membri sono statici e non e' istanziabile.
 */
public final class ViewStyle {

    /** Inizio del nome delle caselle "Imprevisti", in minuscolo. */
    private static final String CHANCE_NAME = "imprevist";

    /** Simbolo della valuta, scritto dopo ogni importo. */
    private static final String CURRENCY = "€";

    /** Sfondo di ogni famiglia di caselle. */
    private static final Map<TileCategory, Color> CATEGORY_COLORS = createCategoryColors();

    /** Traduzione dei nomi di colore usati dalle pedine ({@link Token#getColor()}). */
    private static final Map<String, Color> TOKEN_COLORS = createTokenColors();

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
        return CATEGORY_COLORS.getOrDefault(category, Theme.TILE_PLAIN);
    }

    /**
     * Colore di sfondo di una casella precisa: quello della sua famiglia
     * ({@link #colorOf(TileCategory)}), tranne per Imprevisti e Probabilita', che sono
     * interamente del colore del loro mazzo - arancione la prima, azzurro la seconda,
     * come nel gioco vero.
     * <p>
     * Nel model le due caselle sono la stessa classe
     * ({@link it.unibo.monopoly.model.board.CardTile CardTile}), della categoria
     * {@link TileCategory#CARD}: l'unica differenza e' il nome stampato sopra, ed e' da
     * quello che la view le distingue. Una casella di quella categoria con un nome
     * diverso prende il colore di Probabilita'.
     *
     * @param tile la casella da disegnare
     * @return il colore con cui riempirla
     */
    public static Color colorOf(final Tile tile) {
        if (tile.getCategory() != TileCategory.CARD) {
            return colorOf(tile.getCategory());
        }
        return tile.getName().toLowerCase(Locale.ROOT).startsWith(CHANCE_NAME)
                ? Theme.CHANCE
                : Theme.COMMUNITY_CHEST;
    }

    /**
     * Riga di dettaglio di una casella, pronta da mostrare: se finisce con un importo
     * ("60", "Paga 200") gli aggiunge il simbolo dell'euro, nello stesso formato di
     * {@link #formatMoney(int)}.
     * <p>
     * Il testo arriva dal model ({@link Tile#getDetail()}), che non conosce la valuta: e'
     * una scelta di presentazione, quindi la si aggiunge qui. Le scritte che non finiscono
     * con una cifra ("Solo visita", "?") restano come sono.
     *
     * @param tile la casella
     * @return il dettaglio, eventualmente con il simbolo della valuta
     */
    public static String detailOf(final Tile tile) {
        final String detail = tile.getDetail();
        return !detail.isEmpty() && Character.isDigit(detail.charAt(detail.length() - 1))
                ? detail + " " + CURRENCY
                : detail;
    }

    /**
     * Colore di una proprieta': quello della banda in alto sulla sua casella - il gruppo
     * per i terreni, come sul tabellone vero; un colore proprio per le stazioni e uno
     * diverso per le societa', cosi' le due famiglie si distinguono a colpo d'occhio - e
     * della striscia del suo contratto nella scheda del proprietario.
     * <p>
     * E' l'unico punto della view che guarda la classe di una proprieta', e lo fa in un
     * solo {@code switch}: aggiungere una famiglia di caselle significa aggiungere un caso
     * qui, e nessun pannello va toccato.
     *
     * @param property la proprieta'
     * @return il colore della sua banda
     */
    public static Color bandColorOf(final Property property) {
        return switch (property) {
            case StreetTile street -> colorOf(street.getGroup());
            case StationTile _ -> Theme.STATION_BAND;
            case UtilityTile _ -> Theme.UTILITY_BAND;
            default -> Theme.NEUTRAL_BAND;
        };
    }

    /**
     * Colore della banda di un terreno: quello del suo gruppo, come sul tabellone vero.
     *
     * @param group il gruppo di colore del terreno
     * @return il colore della banda
     */
    public static Color colorOf(final ColorGroup group) {
        // Lo switch su enum e' esaustivo: aggiungendo un gruppo il compilatore avvisa.
        return switch (group) {
            case BROWN -> Theme.GROUP_BROWN;
            case LIGHT_BLUE -> Theme.GROUP_LIGHT_BLUE;
            case PINK -> Theme.GROUP_PINK;
            case ORANGE -> Theme.GROUP_ORANGE;
            case RED -> Theme.GROUP_RED;
            case YELLOW -> Theme.GROUP_YELLOW;
            case GREEN -> Theme.GROUP_GREEN;
            case BLUE -> Theme.GROUP_BLUE;
        };
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
            return Theme.TOKEN_UNKNOWN;
        }
        return TOKEN_COLORS.getOrDefault(token.getColor().trim().toUpperCase(Locale.ROOT), Theme.TOKEN_UNKNOWN);
    }

    /**
     * Pallino colorato con cui mostrare una pedina in un elenco, per esempio nella
     * tendina della schermata di setup.
     * <p>
     * E' disegnato come le pedine sul tabellone (un cerchio pieno con il bordo grigio),
     * cosi' chi sceglie la pedina vede gia' come apparira' in partita.
     *
     * @param token la pedina da rappresentare
     * @return un'icona quadrata con il colore della pedina
     */
    public static Icon iconOf(final Token token) {
        return new TokenIcon(colorOf(token), TokenIcon.DOT_SIDE, "");
    }

    /**
     * Distintivo tondo di un giocatore: il colore della sua pedina con una scritta al
     * centro, per esempio il suo numero nella schermata di setup.
     * <p>
     * E' disegnato come le pedine sul tabellone, che portano l'iniziale del giocatore:
     * la scritta e' scura sulle pedine chiare e chiara su quelle scure.
     *
     * @param token la pedina del giocatore, oppure null se non l'ha ancora scelta
     * @param text  la scritta da mettere al centro
     * @return un'icona tonda, piu' grande di quella di {@link #iconOf(Token)}
     */
    public static Icon badgeOf(final Token token, final String text) {
        return new TokenIcon(colorOf(token), TokenIcon.BADGE_SIDE, text);
    }

    /**
     * Frase da mostrare all'utente quando la configurazione di inizio partita non va
     * bene.
     * <p>
     * La traduzione sta nella view e non nell'enum del controller per lo stesso motivo
     * di {@link #describe(PlayerStatus)}: e' un problema di presentazione, e
     * un'interfaccia diversa potrebbe volerla dire in un altro modo.
     *
     * @param problem il problema trovato dalla validazione
     * @return la spiegazione da mostrare
     */
    public static String describe(final SetupProblem problem) {
        return switch (problem) {
            case TOO_FEW_PLAYERS -> "Servono almeno " + GameState.MIN_PLAYERS + " giocatori.";
            case TOO_MANY_PLAYERS -> "Si puo' giocare al massimo in " + GameState.MAX_PLAYERS + ".";
            case EMPTY_NAME -> "Ogni giocatore deve avere un nome.";
            case MISSING_TOKEN -> "Ogni giocatore deve scegliere una pedina.";
            case DUPLICATE_TOKEN -> "Due giocatori hanno scelto la stessa pedina: devono essere tutte diverse.";
        };
    }

    /**
     * Colore con cui scrivere lo stato di un giocatore: verde se gioca, ambra se e' in
     * prigione, rosso se e' fallito. Sono tutti leggibili sul crema delle schede.
     *
     * @param status lo stato del giocatore
     * @return il colore del testo
     */
    public static Color colorOf(final PlayerStatus status) {
        // Lo switch su enum e' esaustivo: aggiungendo uno stato il compilatore avvisa.
        return switch (status) {
            case PLAYING -> Theme.STATUS_PLAYING;
            case IN_JAIL -> Theme.STATUS_IN_JAIL;
            case BANKRUPT -> Theme.STATUS_BANKRUPT;
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
     * Formatta un importo in euro: 1500 diventa "1500 €".
     * <p>
     * E' l'unico formato degli importi nella view - cassa della banca, saldi, prezzi,
     * affitti, cauzione - cosi' si leggono tutti allo stesso modo.
     *
     * @param amount l'importo da mostrare
     * @return l'importo formattato, con il simbolo della valuta
     */
    public static String formatMoney(final int amount) {
        return amount + " " + CURRENCY;
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
        colors.put(TileCategory.START, Theme.TILE_START);
        colors.put(TileCategory.PROPERTY, Theme.TILE_PLAIN);
        colors.put(TileCategory.TAX, Theme.TILE_TAX);
        colors.put(TileCategory.JAIL, Theme.TILE_JAIL);
        colors.put(TileCategory.GO_TO_JAIL, Theme.TILE_GO_TO_JAIL);
        colors.put(TileCategory.FREE_PARKING, Theme.TILE_FREE_PARKING);
        // Imprevisti e Probabilita' prendono il colore del loro mazzo, che dipende dal
        // nome e non dalla categoria (vedi colorOf(Tile)): questo e' solo il ripiego.
        colors.put(TileCategory.CARD, Theme.TILE_PLAIN);
        colors.put(TileCategory.OTHER, Theme.TILE_PLAIN);
        return colors;
    }

    /**
     * Il pallino colorato restituito da {@link #iconOf(Token)} e il distintivo di
     * {@link #badgeOf(Token, String)}: sono la stessa figura, cambiano solo la misura e
     * la scritta.
     * <p>
     * E' una classe annidata privata perche' non serve a nessun altro: e' solo il modo
     * di dare a Swing qualcosa da disegnare accanto al nome della pedina.
     */
    private static final class TokenIcon implements Icon {

        /** Lato del pallino semplice, in pixel. */
        private static final int DOT_SIDE = 12;

        /** Lato del distintivo con la scritta, in pixel. */
        private static final int BADGE_SIDE = 26;

        private final Color color;
        private final int side;
        private final String text;

        private TokenIcon(final Color color, final int side, final String text) {
            this.color = color;
            this.side = side;
            this.text = text;
        }

        @Override
        public void paintIcon(final Component component, final Graphics g, final int x, final int y) {
            final Graphics2D graphics = (Graphics2D) g.create();
            try {
                Theme.antialias(graphics);
                graphics.setColor(this.color);
                graphics.fillOval(x, y, this.side, this.side);
                graphics.setColor(Theme.BORDER_STRONG);
                graphics.drawOval(x, y, this.side, this.side);
                if (!this.text.isEmpty()) {
                    graphics.setColor(Theme.readableTextOn(this.color));
                    graphics.setFont(Theme.NAME_FONT);
                    final FontMetrics metrics = graphics.getFontMetrics();
                    graphics.drawString(this.text,
                            x + (this.side - metrics.stringWidth(this.text)) / 2,
                            y + (this.side - metrics.getHeight()) / 2 + metrics.getAscent());
                }
            } finally {
                graphics.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return this.side + 1;
        }

        @Override
        public int getIconHeight() {
            return this.side + 1;
        }
    }

    /** Nomi di colore accettati per le pedine. */
    private static Map<String, Color> createTokenColors() {
        final Map<String, Color> colors = new HashMap<>();
        colors.put("RED", Theme.TOKEN_RED);
        colors.put("BLUE", Theme.TOKEN_BLUE);
        colors.put("GREEN", Theme.TOKEN_GREEN);
        colors.put("YELLOW", Theme.TOKEN_YELLOW);
        colors.put("ORANGE", Theme.TOKEN_ORANGE);
        colors.put("PURPLE", Theme.TOKEN_PURPLE);
        colors.put("MAGENTA", Theme.TOKEN_MAGENTA);
        colors.put("CYAN", Theme.TOKEN_CYAN);
        colors.put("BROWN", Theme.TOKEN_BROWN);
        colors.put("BLACK", Theme.TOKEN_BLACK);
        colors.put("GRAY", Theme.TOKEN_GRAY);
        colors.put("PINK", Theme.TOKEN_PINK);
        return colors;
    }
}
