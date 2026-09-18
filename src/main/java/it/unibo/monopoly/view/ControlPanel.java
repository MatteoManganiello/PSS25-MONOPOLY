package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.model.game.JailManager;
import it.unibo.monopoly.model.game.RollResult;
import it.unibo.monopoly.model.player.Player;

/**
 * I comandi con cui si gioca: il pulsante dei dadi, il passaggio del turno, le
 * azioni contestuali, il disegno dell'ultimo lancio e il racconto di cosa e'
 * successo.
 * <p>
 * E' l'unico pannello che parla con il {@link GameEngine}, e ci parla solo in un
 * verso: i gestori dei pulsanti chiamano un comando del motore
 * ({@link GameEngine#rollDice()}, {@link GameEngine#endTurn()},
 * {@link GameEngine#payBail()}) e si fermano li'. Non aggiornano l'interfaccia:
 * l'aggiornamento arriva dopo, quando il motore notifica gli osservatori e il
 * {@link MainWindow} chiama {@link #refresh()}. E' la regola dell'MVC - la view
 * chiede, il model decide, la view si ridisegna su cio' che e' successo - ed e'
 * anche il motivo per cui un comando "impossibile" non e' un problema: i pulsanti
 * si abilitano solo quando il motore dichiara che l'azione e' consentita
 * ({@link GameEngine#canRollDice()} e simili).
 * <p>
 * Il riquadro "Azioni" raccoglie le azioni contestuali, attive solo quando servono:
 * il pagamento della cauzione e la decisione di acquisto ("Compra" / "Non comprare").
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class ControlPanel extends JPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Righe visibili dell'area di log. */
    private static final int LOG_ROWS = 6;

    private final transient GameEngine engine;
    private final JButton rollButton;
    private final JButton endTurnButton;
    private final JButton bailButton;
    private final JButton buyButton;
    private final JButton declineButton;
    private final DiceView diceView;
    private final JLabel statusLabel;
    private final JTextArea logArea;

    /**
     * Crea la barra dei comandi collegata al motore della partita.
     *
     * @param engine il motore a cui inoltrare i comandi dell'utente
     * @throws IllegalArgumentException se il motore e' null
     */
    public ControlPanel(final GameEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Il motore della partita non puo' essere null");
        }
        this.engine = engine;
        this.rollButton = new JButton("Tira i dadi");
        this.endTurnButton = new JButton("Passa il turno");
        this.bailButton = new JButton("Paga la cauzione (" + JailManager.BAIL_AMOUNT + ")");
        this.buyButton = new JButton("Compra");
        this.declineButton = new JButton("Non comprare");
        this.diceView = new DiceView();
        this.statusLabel = new JLabel();
        this.logArea = new JTextArea(LOG_ROWS, 40);

        this.setLayout(new BorderLayout(0, 4));
        this.setBackground(ViewStyle.PANEL_BACKGROUND);
        this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.add(this.createCommandsRow(), BorderLayout.NORTH);
        this.add(this.createLogArea(), BorderLayout.CENTER);
        this.connectButtons();
        this.refresh();
    }

    /**
     * Mostra i valori dell'ultimo lancio.
     *
     * @param result il risultato comunicato dal motore
     */
    public void showRoll(final RollResult result) {
        this.diceView.setValues(result.firstDie(), result.secondDie());
    }

    /**
     * Svuota il disegno dei dadi.
     * <p>
     * Serve quando la finestra passa a un'altra partita, per esempio caricata da file:
     * l'ultimo lancio mostrato apparteneva alla partita precedente.
     */
    public void clearRoll() {
        this.diceView.setValues(DiceView.NOT_ROLLED, DiceView.NOT_ROLLED);
    }

    /**
     * Aggiunge una riga al racconto della partita e vi scorre sopra.
     *
     * @param line la riga da mostrare
     */
    public void appendLog(final String line) {
        this.logArea.append(line + System.lineSeparator());
        // Il cursore in fondo tiene visibile l'ultima riga scritta.
        this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
    }

    /**
     * Abilita i comandi consentiti in questo momento e aggiorna la riga di stato.
     * <p>
     * Non decide nulla da sola: chiede al motore cosa e' permesso. E' il motivo per
     * cui le regole restano una sola copia, nel model, anche se l'interfaccia deve
     * saperle mostrare.
     */
    public void refresh() {
        // Mentre c'e' una proprieta' da decidere, canRollDice() e canEndTurn() sono gia'
        // false: i pulsanti del turno si spengono da soli e restano accesi solo Compra e
        // Non comprare.
        this.rollButton.setEnabled(this.engine.canRollDice());
        this.endTurnButton.setEnabled(this.engine.canEndTurn());
        this.bailButton.setEnabled(this.engine.canPayBail());
        this.buyButton.setEnabled(this.engine.canBuyOfferedProperty());
        this.declineButton.setEnabled(this.engine.canDeclineOfferedProperty());
        this.statusLabel.setText(this.describeSituation());
    }

    /** Riga di stato: a chi tocca e cosa ci si aspetta che faccia. */
    private String describeSituation() {
        if (this.engine.isGameOver()) {
            return this.engine.getWinner()
                    .map(winner -> "Partita finita: vince " + winner.getName())
                    .orElse("Partita finita");
        }
        final Player current = this.engine.getState().getCurrentPlayer();
        final String action;
        final var offered = this.engine.getOfferedProperty();
        if (offered.isPresent()) {
            action = "compri \"" + offered.get().getName() + "\" per " + offered.get().getPrice() + "?";
        } else if (this.engine.canRollDice()) {
            action = current.isInJail() ? "tenta l'uscita di prigione" : "lancia i dadi";
        } else if (this.engine.canEndTurn()) {
            action = "passa il turno";
        } else {
            action = "attendi";
        }
        return "Tocca a " + current.getName() + ": " + action;
    }

    /** La riga superiore: pulsanti, azioni contestuali, dadi e stato. */
    private JPanel createCommandsRow() {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(ViewStyle.PANEL_BACKGROUND);

        final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setBackground(ViewStyle.PANEL_BACKGROUND);
        actions.setBorder(BorderFactory.createTitledBorder("Azioni"));
        actions.add(this.bailButton);
        actions.add(this.buyButton);
        actions.add(this.declineButton);

        this.statusLabel.setFont(ViewStyle.PLAYER_NAME_FONT);
        this.statusLabel.setForeground(ViewStyle.TEXT);

        row.add(this.rollButton);
        row.add(this.endTurnButton);
        row.add(actions);
        row.add(this.diceView);
        row.add(this.statusLabel);
        return row;
    }

    /** L'area di testo in cui il {@link MainWindow} riversa la cronaca della partita. */
    private JScrollPane createLogArea() {
        this.logArea.setEditable(false);
        this.logArea.setFont(ViewStyle.LOG_FONT);
        this.logArea.setLineWrap(true);
        this.logArea.setWrapStyleWord(true);
        final JScrollPane scroll = new JScrollPane(this.logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Cosa e' successo"));
        return scroll;
    }

    /**
     * Collega i pulsanti ai comandi del motore.
     * <p>
     * I gestori sono volutamente di una riga: la view non aggiunge logica, inoltra
     * l'intenzione dell'utente e lascia che sia il model a stabilire cosa comporta.
     */
    private void connectButtons() {
        this.rollButton.addActionListener(event -> this.engine.rollDice());
        this.endTurnButton.addActionListener(event -> this.engine.endTurn());
        this.bailButton.addActionListener(event -> this.engine.payBail());
        this.buyButton.addActionListener(event -> this.engine.buyOfferedProperty());
        this.declineButton.addActionListener(event -> this.engine.declineOfferedProperty());
    }

    /**
     * Disegno dei due dadi dell'ultimo lancio.
     * <p>
     * Un componente minuscolo, con l'unico scopo di rendere il risultato leggibile a
     * colpo d'occhio invece che come numero in mezzo al testo. Non conosce il model:
     * riceve due valori e li disegna.
     */
    private static final class DiceView extends JComponent {

        /** Vedi {@link TilePanel#serialVersionUID}. */
        private static final long serialVersionUID = 1L;

        /** Lato di un dado, in pixel. */
        private static final int DIE_SIDE = 30;

        /** Spazio fra i due dadi. */
        private static final int GAP = 6;

        /** Diametro di un pallino. */
        private static final int PIP = 6;

        /** Valore convenzionale prima del primo lancio. */
        private static final int NOT_ROLLED = 0;

        private int firstValue;
        private int secondValue;

        DiceView() {
            this.firstValue = NOT_ROLLED;
            this.secondValue = NOT_ROLLED;
            this.setPreferredSize(new Dimension(2 * DIE_SIDE + GAP, DIE_SIDE + 2));
        }

        /**
         * Mostra un nuovo lancio.
         *
         * @param first  valore del primo dado
         * @param second valore del secondo dado
         */
        void setValues(final int first, final int second) {
            this.firstValue = first;
            this.secondValue = second;
            this.repaint();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D graphics = (Graphics2D) g.create();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                this.paintDie(graphics, 0, this.firstValue);
                this.paintDie(graphics, DIE_SIDE + GAP, this.secondValue);
            } finally {
                graphics.dispose();
            }
        }

        /** Disegna un dado con la faccia richiesta; con 0 il dado resta vuoto. */
        private void paintDie(final Graphics2D graphics, final int x, final int value) {
            graphics.setColor(Color.WHITE);
            graphics.fillRoundRect(x, 1, DIE_SIDE, DIE_SIDE, 8, 8);
            graphics.setColor(ViewStyle.OUTLINE);
            graphics.drawRoundRect(x, 1, DIE_SIDE, DIE_SIDE, 8, 8);
            if (value < 1) {
                return;
            }
            // Le facce dei dadi si compongono da tre posizioni orizzontali e tre verticali.
            final int low = x + DIE_SIDE / 4 - PIP / 2;
            final int middleX = x + DIE_SIDE / 2 - PIP / 2;
            final int high = x + 3 * DIE_SIDE / 4 - PIP / 2;
            final int top = 1 + DIE_SIDE / 4 - PIP / 2;
            final int middleY = 1 + DIE_SIDE / 2 - PIP / 2;
            final int bottom = 1 + 3 * DIE_SIDE / 4 - PIP / 2;

            if (value % 2 == 1) {
                // 1, 3 e 5 hanno il pallino centrale.
                graphics.fillOval(middleX, middleY, PIP, PIP);
            }
            if (value >= 2) {
                graphics.fillOval(low, top, PIP, PIP);
                graphics.fillOval(high, bottom, PIP, PIP);
            }
            if (value >= 4) {
                graphics.fillOval(high, top, PIP, PIP);
                graphics.fillOval(low, bottom, PIP, PIP);
            }
            if (value == 6) {
                graphics.fillOval(low, middleY, PIP, PIP);
                graphics.fillOval(high, middleY, PIP, PIP);
            }
        }
    }
}
