package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;

/**
 * Colonna laterale con la cassa della banca e la situazione di ogni giocatore: nome e
 * pedina, denaro, stato (in gioco, in prigione, fallito) e proprieta' possedute.
 * <p>
 * Ogni giocatore ha la sua "scheda" ({@link PlayerCard}), creata una volta sola alla
 * costruzione del pannello: {@link #refresh()} aggiorna le etichette e ricostruisce
 * soltanto l'elenco delle proprieta', e solo quando cambia. La scheda del giocatore di
 * turno viene evidenziata con l'anello giallo acceso del turno attivo - lo stesso che il
 * tabellone disegna attorno alla casella su cui si trova - e con un triangolino accanto
 * al nome, cosi' di chi sia il turno si capisce a colpo d'occhio, anche senza
 * distinguere i colori.
 * <p>
 * <b>Scorrimento.</b> La cassa della banca e il titolo "Giocatori" restano sempre in
 * vista; sotto, le schede scorrono ({@link JScrollPane}) quando sono troppe per l'altezza
 * della finestra, per esempio con molti giocatori o con tante proprieta'. Quando passa il
 * turno l'elenco scorre da solo fino alla scheda del nuovo giocatore di turno.
 * <p>
 * <b>Aspetto.</b> La colonna e' trasparente: sul tavolo si vedono solo le schede crema
 * con il bordo grigio. In cima c'e' quella della banca, che deve saltare all'occhio:
 * un'intestazione blu con la scritta crema, come il cartiglio di un contratto, e sotto
 * l'importo in cassa nel numero piu' grande della schermata. Il nome dei giocatori e'
 * sempre scuro: il colore della pedina e' nel pallino accanto, perche' scritto in
 * giallo o in ciano sul crema non si leggerebbe. Ogni proprieta' e' un piccolo
 * contratto con la striscia del suo colore, lo stesso della banda sul tabellone, e le
 * proprieta' sono in ordine di tabellone, cosi' quelle dello stesso gruppo stanno vicine.
 * <p>
 * Come gli altri pannelli legge il {@link GameState} e non lo modifica mai.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class PlayerInfoPanel extends JPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Larghezza preferita della colonna laterale, in pixel: c'e' posto anche per la barra di scorrimento. */
    private static final int PREFERRED_WIDTH = 280;

    /** Spessore del contorno grigio della scheda della banca: piu' marcato di quello dei giocatori. */
    private static final int BANK_OUTLINE = 2;

    /** Titolo della scheda della banca. */
    private static final String BANK_TITLE = "CASSA DELLA BANCA";

    private final transient GameState state;
    private final transient List<PlayerCard> cards;

    /** L'importo in cassa, scritto in grande sulla scheda della banca. */
    private final JLabel bankAmount;

    /**
     * Crea il pannello con una scheda per giocatore.
     *
     * @param state lo stato della partita, usato in sola lettura
     * @throws IllegalArgumentException se lo stato e' null
     */
    public PlayerInfoPanel(final GameState state) {
        super(new BorderLayout(0, Theme.GAP));
        if (state == null) {
            throw new IllegalArgumentException("Lo stato della partita non puo' essere null");
        }
        this.state = state;
        this.cards = new ArrayList<>();
        // L'importo c'e' gia' dall'inizio: la scheda della banca prende l'altezza del suo
        // contenuto, e un'etichetta vuota sarebbe alta zero.
        this.bankAmount = Theme.label(bankBalance(state), Theme.AMOUNT_FONT, Theme.ACCENT_BLUE);

        this.setOpaque(false);
        this.setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));
        this.add(this.createHeader(), BorderLayout.NORTH);
        this.add(this.createPlayersList(), BorderLayout.CENTER);
        this.refresh();
    }

    /**
     * Aggiorna denaro, proprieta', stato ed evidenziazione del turno di tutti i
     * giocatori. Chiamato dal {@link MainWindow} a ogni evento del model.
     * <p>
     * A partita finita, o quando il giocatore corrente e' fallito, nessuna scheda viene
     * evidenziata: la regola e' la stessa del tabellone
     * ({@link ViewStyle#playerToHighlight(GameState)}).
     */
    public void refresh() {
        final Optional<Player> highlighted = ViewStyle.playerToHighlight(this.state);
        for (final PlayerCard card : this.cards) {
            card.refresh(highlighted.filter(card::isFor).isPresent());
        }
        this.bankAmount.setText(bankBalance(this.state));
    }

    /** @return il denaro in cassa alla banca, gia' formattato in euro */
    private static String bankBalance(final GameState state) {
        return ViewStyle.formatMoney(state.getContext().getBank().getBalance());
    }

    /** La parte fissa in cima alla colonna: la scheda della banca e il titolo "Giocatori". */
    private JPanel createHeader() {
        final JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(this.createBankCard());
        header.add(Box.createVerticalStrut(2 * Theme.GAP));
        // Il titolo sta direttamente sul tavolo: il testo scuro sul verde salvia si legge bene.
        final JLabel title = Theme.label("Giocatori", Theme.HEADING_FONT, Theme.TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        return header;
    }

    /**
     * L'elenco scorrevole delle schede dei giocatori. Scorre solo in verticale: le schede
     * sono sempre larghe quanto la colonna, e la barra compare solo quando serve.
     */
    private JScrollPane createPlayersList() {
        final CardsColumn column = new CardsColumn();
        for (final Player player : this.state.getPlayers()) {
            if (!this.cards.isEmpty()) {
                column.add(Box.createVerticalStrut(Theme.GAP));
            }
            final PlayerCard card = new PlayerCard(player);
            this.cards.add(card);
            column.add(card);
        }
        final JScrollPane scroller = new JScrollPane(column,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Trasparente e senza cornice: sotto le schede si vede il verde del tavolo.
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        Theme.styleScrollBar(scroller.getVerticalScrollBar());
        return scroller;
    }

    /**
     * La scheda della banca: un'intestazione blu con il titolo in crema (7,3:1) e, sotto,
     * l'importo in cassa in grande, blu su crema. Il contorno e' quello marcato, piu'
     * spesso di quello dei giocatori, perche' e' la prima cosa che la colonna deve mostrare.
     */
    private CardPanel createBankCard() {
        final JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.ACCENT_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(Theme.GAP / 2, Theme.PADDING, Theme.GAP / 2, Theme.PADDING));
        header.add(Theme.label(BANK_TITLE, Theme.BODY_BOLD_FONT, Theme.TEXT_LIGHT), BorderLayout.CENTER);

        this.bankAmount.setBorder(BorderFactory.createEmptyBorder(
                Theme.GAP / 2, Theme.PADDING, Theme.GAP / 2, Theme.PADDING));

        final CardPanel card = new CardPanel(new BorderLayout(), Theme.BORDER_STRONG, BANK_OUTLINE);
        card.setBackground(Theme.CREAM);
        // Un margine pari al contorno tiene l'intestazione dentro il filo grigio.
        card.setBorder(Theme.padding(BANK_OUTLINE));
        card.add(header, BorderLayout.NORTH);
        card.add(this.bankAmount, BorderLayout.CENTER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Nel BoxLayout la scheda si allargherebbe anche in altezza: resta alta quanto il suo contenuto.
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    /**
     * La colonna delle schede dentro l'area scorrevole.
     * <p>
     * Implementa {@link Scrollable} per due ragioni: resta sempre larga quanto l'area
     * visibile (le schede non escono mai di lato e non serve scorrere in orizzontale), e
     * la rotellina la fa scorrere di un passo comodo invece che di un pixel alla volta.
     */
    private static final class CardsColumn extends JPanel implements Scrollable {

        /** Vedi {@link TilePanel#serialVersionUID}. */
        private static final long serialVersionUID = 1L;

        /** Di quanto scorre la colonna a ogni scatto della rotellina, in pixel. */
        private static final int SCROLL_STEP = 24;

        CardsColumn() {
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return this.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation,
                                              final int direction) {
            return SCROLL_STEP;
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation,
                                               final int direction) {
            return Math.max(SCROLL_STEP, visibleRect.height - SCROLL_STEP);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Scheda di un singolo giocatore.
     * <p>
     * E' una classe annidata privata perche' ha senso solo dentro questo pannello:
     * tenerla qui evita di aggiungere un file alla view per un dettaglio di layout,
     * ma le da' comunque uno stato e dei metodi propri invece di sparpagliare decine
     * di etichette nel pannello padre. E' {@code static} perche' le basta il proprio
     * giocatore: non ha bisogno del pannello che la contiene.
     */
    private static final class PlayerCard extends CardPanel {

        /** Vedi {@link TilePanel#serialVersionUID}. */
        private static final long serialVersionUID = 1L;

        /** Spazio fra due contratti dell'elenco delle proprieta'. */
        private static final int DEED_GAP = 3;

        private final transient Player player;
        private final JLabel nameLabel;
        private final JLabel moneyLabel;
        private final JLabel statusLabel;
        private final JLabel propertiesTitle;

        /** L'elenco delle proprieta', un contratto per riga. */
        private final JPanel propertiesList;

        /** Le proprieta' mostrate ora, per ricostruire l'elenco solo quando cambiano. */
        private transient List<Property> shownProperties;

        /** True se la scheda e' quella del giocatore di turno. */
        private boolean current;

        PlayerCard(final Player player) {
            super(null, Theme.BORDER, 1);
            this.player = player;
            this.nameLabel = new JLabel();
            this.moneyLabel = new JLabel();
            this.statusLabel = new JLabel();
            this.propertiesTitle = Theme.label("", Theme.BODY_BOLD_FONT, Theme.TEXT_DARK);
            this.propertiesList = new JPanel();
            this.shownProperties = List.of();
            this.current = false;

            // BoxLayout e non GridLayout: l'elenco delle proprieta' cresce di riga in
            // riga durante la partita, e ogni riga deve poter prendere l'altezza che le
            // serve invece di riceverne una uguale per tutte.
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Margini interni piu' ampi dell'anello del turno con il suo alone: quando la
            // scheda e' evidenziata il testo non ci finisce sotto.
            this.setBorder(BorderFactory.createEmptyBorder(Theme.GAP, Theme.PADDING, Theme.GAP, Theme.PADDING));
            this.setBackground(Theme.CREAM);
            this.nameLabel.setFont(Theme.NAME_FONT);
            this.nameLabel.setForeground(Theme.TEXT_DARK);
            // Il pallino della pedina segue il suo nome: "Alice (Cappello) ●".
            this.nameLabel.setIcon(ViewStyle.iconOf(player.getToken()));
            this.nameLabel.setHorizontalTextPosition(SwingConstants.LEADING);
            this.moneyLabel.setFont(Theme.BODY_BOLD_FONT);
            this.moneyLabel.setForeground(Theme.ACCENT_BLUE);
            this.statusLabel.setFont(Theme.BODY_BOLD_FONT);
            this.propertiesList.setLayout(new BoxLayout(this.propertiesList, BoxLayout.Y_AXIS));
            this.propertiesList.setOpaque(false);
            for (final Row row : List.of(
                    new Row(this.nameLabel, 2),
                    new Row(this.moneyLabel, 2),
                    new Row(this.statusLabel, Theme.GAP),
                    new Row(this.propertiesTitle, DEED_GAP + 1),
                    new Row(this.propertiesList, 0))) {
                row.component().setAlignmentX(Component.LEFT_ALIGNMENT);
                this.add(row.component());
                this.add(Box.createVerticalStrut(row.spaceBelow()));
            }
        }

        /**
         * @param other il giocatore da confrontare
         * @return true se questa scheda descrive quel giocatore
         */
        boolean isFor(final Player other) {
            // Player non ridefinisce equals: il confronto e' volutamente per identita'.
            return this.player == other;
        }

        /**
         * Riscrive le etichette con i valori attuali del giocatore.
         * <p>
         * Quando la scheda diventa quella di turno, l'elenco scorre fino a mostrarla. Lo
         * scorrimento aspetta che Swing abbia rifatto il layout, perche' la scheda potrebbe
         * essere appena cresciuta di un contratto.
         *
         * @param current true se e' il suo turno: la scheda viene evidenziata
         */
        void refresh(final boolean current) {
            this.nameLabel.setText((current ? "▶ " : "") + this.player.getName()
                    + "  (" + this.player.getToken().getName() + ")");
            this.moneyLabel.setText("Denaro: " + ViewStyle.formatMoney(this.player.getMoney()));
            this.statusLabel.setText("Stato: " + ViewStyle.describe(this.player.getStatus()));
            this.statusLabel.setForeground(ViewStyle.colorOf(this.player.getStatus()));
            this.refreshProperties();

            // Il turno si legge dall'anello giallo, lo stesso della casella di turno.
            this.setHighlighted(current);
            if (current && !this.current) {
                SwingUtilities.invokeLater(() -> this.scrollRectToVisible(
                        new Rectangle(0, 0, this.getWidth(), this.getHeight())));
            }
            this.current = current;
        }

        /**
         * Aggiorna il titolo dell'elenco delle proprieta' e, se le proprieta' sono
         * cambiate, ricostruisce i contratti. In ordine di tabellone: quelle dello stesso
         * gruppo di colore finiscono vicine.
         */
        private void refreshProperties() {
            final List<Property> owned = this.player.getProperties().stream()
                    .sorted(Comparator.comparingInt(Tile::getPosition))
                    .toList();
            this.propertiesTitle.setText(owned.isEmpty()
                    ? "Proprieta': nessuna"
                    : "Proprieta' (" + owned.size() + ")");
            if (owned.equals(this.shownProperties)) {
                return;
            }
            this.propertiesList.removeAll();
            for (final Property property : owned) {
                if (this.propertiesList.getComponentCount() > 0) {
                    this.propertiesList.add(Box.createVerticalStrut(DEED_GAP));
                }
                this.propertiesList.add(deedOf(property));
            }
            this.shownProperties = owned;
            this.revalidate();
            this.repaint();
        }

        /**
         * Una proprieta' nell'elenco: un piccolo contratto su carta chiara, con la
         * striscia del colore della proprieta' e il nome, largo quanto la scheda.
         */
        private static JLabel deedOf(final Property property) {
            final JLabel deed = Theme.label(property.getName(), Theme.BODY_FONT, Theme.TEXT_DARK);
            deed.setOpaque(true);
            deed.setBackground(Theme.DEED_SURFACE);
            deed.setBorder(Theme.deedBorder(ViewStyle.bandColorOf(property)));
            deed.setAlignmentX(Component.LEFT_ALIGNMENT);
            deed.setMaximumSize(new Dimension(Integer.MAX_VALUE, deed.getPreferredSize().height));
            return deed;
        }

        /**
         * Una riga della scheda e lo spazio da lasciare sotto di lei.
         *
         * @param component  il contenuto della riga
         * @param spaceBelow lo spazio sotto, in pixel
         */
        private record Row(JComponent component, int spaceBelow) {
        }
    }
}
