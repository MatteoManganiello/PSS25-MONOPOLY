package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;

/**
 * Colonna laterale con la cassa della banca e la situazione di ogni giocatore: nome e
 * pedina, denaro, proprieta' possedute e stato (in gioco, in prigione, fallito).
 * <p>
 * Ogni giocatore ha la sua "scheda" ({@link PlayerCard}), creata una volta sola alla
 * costruzione del pannello: {@link #refresh()} non ricostruisce nulla, aggiorna solo
 * il testo delle etichette. La scheda del giocatore di turno viene evidenziata con un
 * bordo grigio spesso, uno sfondo appena velato di verde e un triangolino accanto al
 * nome, cosi' di chi sia il turno si capisce a colpo d'occhio, anche senza distinguere
 * i colori.
 * <p>
 * <b>Aspetto.</b> La colonna e' trasparente: sul tavolo si vedono solo le schede crema.
 * In cima c'e' quella della banca, con il bordo marcato e l'importo nel numero piu'
 * grande della schermata, perche' deve saltare all'occhio. Il nome dei giocatori e'
 * sempre scuro: il colore della pedina e' nel pallino accanto, perche' scritto in
 * giallo o in ciano sul crema non si leggerebbe.
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

    /** Larghezza preferita della colonna laterale, in pixel. */
    private static final int PREFERRED_WIDTH = 260;

    /**
     * Larghezza a cui mandare a capo l'elenco delle proprieta', in pixel: la colonna
     * meno i margini interni della scheda e il suo bordo.
     */
    private static final int TEXT_WIDTH = PREFERRED_WIDTH - 2 * Theme.PADDING - 20;

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
        super();
        if (state == null) {
            throw new IllegalArgumentException("Lo stato della partita non puo' essere null");
        }
        this.state = state;
        this.cards = new ArrayList<>();
        this.bankAmount = Theme.label("", Theme.AMOUNT_FONT, Theme.ACCENT_BLUE);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));

        this.add(this.createBankCard());
        this.add(Box.createVerticalStrut(2 * Theme.GAP));
        // Il titolo sta direttamente sul tavolo: il testo scuro sul verde salvia si legge bene.
        final JLabel title = Theme.label("Giocatori", Theme.HEADING_FONT, Theme.TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(title);
        this.add(Box.createVerticalStrut(Theme.GAP));
        for (final Player player : state.getPlayers()) {
            final PlayerCard card = new PlayerCard(player);
            this.cards.add(card);
            this.add(card);
            this.add(Box.createVerticalStrut(Theme.GAP));
        }
        this.add(Box.createVerticalGlue());
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
        this.bankAmount.setText(ViewStyle.formatMoney(this.state.getContext().getBank().getBalance()));
    }

    /**
     * La scheda della banca: un'etichetta chiara e, sotto, l'importo in cassa in grande.
     * Il bordo e' quello marcato, piu' spesso di quello dei giocatori, perche' e' la
     * prima cosa che la colonna deve mostrare.
     */
    private CardPanel createBankCard() {
        final CardPanel card = Theme.card(new BorderLayout(0, 2));
        card.setOutline(Theme.BORDER_STRONG, 2);
        card.add(Theme.label("Cassa della banca", Theme.BODY_BOLD_FONT, Theme.TEXT_DARK), BorderLayout.NORTH);
        card.add(this.bankAmount, BorderLayout.CENTER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Nel BoxLayout la scheda si allargherebbe anche in altezza: resta alta quanto il suo contenuto.
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
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

        private final transient Player player;
        private final JLabel nameLabel;
        private final JLabel moneyLabel;
        private final JLabel statusLabel;
        private final JLabel propertiesLabel;

        PlayerCard(final Player player) {
            super(null, Theme.BORDER, 1);
            this.player = player;
            this.nameLabel = new JLabel();
            this.moneyLabel = new JLabel();
            this.statusLabel = new JLabel();
            this.propertiesLabel = new JLabel();

            // BoxLayout e non GridLayout: l'elenco delle proprieta' cresce di riga in
            // riga durante la partita, e ogni etichetta deve poter prendere l'altezza
            // che le serve invece di riceverne una uguale per tutte.
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setAlignmentX(Component.LEFT_ALIGNMENT);
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
            this.propertiesLabel.setFont(Theme.BODY_FONT);
            this.propertiesLabel.setForeground(Theme.TEXT_DARK);
            for (final JLabel label : List.of(this.nameLabel, this.moneyLabel,
                    this.statusLabel, this.propertiesLabel)) {
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                this.add(label);
                this.add(Box.createVerticalStrut(2));
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
         *
         * @param current true se e' il suo turno: la scheda viene evidenziata
         */
        void refresh(final boolean current) {
            this.nameLabel.setText((current ? "▶ " : "") + this.player.getName()
                    + "  (" + this.player.getToken().getName() + ")");
            this.moneyLabel.setText("Denaro: " + ViewStyle.formatMoney(this.player.getMoney()));
            this.statusLabel.setText("Stato: " + ViewStyle.describe(this.player.getStatus()));
            this.statusLabel.setForeground(ViewStyle.colorOf(this.player.getStatus()));
            this.propertiesLabel.setText(describeProperties(this.player));

            // Il turno si legge dal bordo grigio spesso e dallo sfondo velato di verde.
            this.setBackground(current ? Theme.CURRENT_SURFACE : Theme.CREAM);
            this.setOutline(current ? Theme.BORDER_STRONG : Theme.BORDER, current ? Theme.HIGHLIGHT_WIDTH : 1);
        }

        /**
         * Elenco delle proprieta' possedute, in un'etichetta HTML: e' il modo piu'
         * semplice per far andare a capo un testo lungo dentro un {@link JLabel}.
         * <p>
         * La larghezza e' imposta con l'attributo {@code width} di una tabella e non
         * con il CSS {@code style="width:..."}: il renderer HTML di Swing ignora il
         * secondo e il testo finirebbe tagliato oltre il bordo della scheda.
         */
        private static String describeProperties(final Player player) {
            final List<Property> properties = player.getProperties();
            if (properties.isEmpty()) {
                return "Proprieta': nessuna";
            }
            final String names = properties.stream()
                    .map(Property::getName)
                    .collect(Collectors.joining(", "));
            return "<html><table width='" + TEXT_WIDTH + "'><tr><td>Proprieta' ("
                    + properties.size() + "): " + names + "</td></tr></table></html>";
        }
    }
}
