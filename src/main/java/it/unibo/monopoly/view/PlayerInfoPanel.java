package it.unibo.monopoly.view;

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
import javax.swing.SwingConstants;

import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.player.Player;

/**
 * Pannello laterale con la situazione di ogni giocatore: nome e pedina, denaro,
 * proprieta' possedute e stato (in gioco, in prigione, fallito).
 * <p>
 * Ogni giocatore ha la sua "scheda" ({@link PlayerCard}), creata una volta sola alla
 * costruzione del pannello: {@link #refresh()} non ricostruisce nulla, aggiorna solo
 * il testo delle etichette. La scheda del giocatore di turno viene evidenziata con
 * una cornice oro, uno sfondo appena piu' caldo e un triangolino accanto al nome, cosi'
 * di chi sia il turno si capisce a colpo d'occhio, anche senza distinguere i colori.
 * <p>
 * Il pannello e' una scheda verde scuro con il titolo in oro; le schede dei giocatori
 * sono crema con il testo scuro. Il nome e' sempre scuro: il colore della pedina e'
 * nel pallino accanto, perche' scritto in giallo o in ciano sul crema non si
 * leggerebbe.
 * <p>
 * Come gli altri pannelli legge il {@link GameState} e non lo modifica mai.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class PlayerInfoPanel extends CardPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Larghezza preferita della colonna laterale, in pixel. */
    private static final int PREFERRED_WIDTH = 260;

    /**
     * Larghezza a cui mandare a capo l'elenco delle proprieta', in pixel: la colonna
     * meno i margini interni del pannello e della scheda.
     */
    private static final int TEXT_WIDTH = PREFERRED_WIDTH - 4 * Theme.PADDING - 30;

    private final transient GameState state;
    private final transient List<PlayerCard> cards;
    private final JLabel bankLabel;

    /**
     * Crea il pannello con una scheda per giocatore.
     *
     * @param state lo stato della partita, usato in sola lettura
     * @throws IllegalArgumentException se lo stato e' null
     */
    public PlayerInfoPanel(final GameState state) {
        super(null, Theme.GOLD, 1);
        if (state == null) {
            throw new IllegalArgumentException("Lo stato della partita non puo' essere null");
        }
        this.state = state;
        this.cards = new ArrayList<>();
        this.bankLabel = new JLabel();

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(Theme.DARK_GREEN);
        this.setBorder(Theme.padding(Theme.PADDING));
        this.setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));

        final JLabel title = Theme.label("Giocatori", Theme.HEADING_FONT, Theme.GOLD);
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
        this.bankLabel.setFont(Theme.BODY_FONT);
        this.bankLabel.setForeground(Theme.TEXT_LIGHT);
        this.bankLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(this.bankLabel);
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
        this.bankLabel.setText("Cassa della banca: "
                + ViewStyle.formatMoney(this.state.getContext().getBank().getBalance()));
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
            super(null, null, 0);
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

            // Il turno si legge dalla cornice oro e dallo sfondo appena piu' caldo.
            this.setBackground(current ? Theme.CURRENT_CARD : Theme.CREAM);
            this.setOutline(current ? Theme.GOLD : null, current ? Theme.HIGHLIGHT_WIDTH : 0);
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
