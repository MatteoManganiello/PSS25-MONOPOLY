package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.model.game.JailManager;
import it.unibo.monopoly.model.game.RollResult;

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
 * <b>Aspetto.</b> Due schede crema appoggiate sul tavolo: sopra i comandi con i dadi,
 * sotto il racconto della partita. Le due azioni che fanno avanzare la partita, "Tira i
 * dadi" e "Compra", sono pulsanti principali (rossi); le altre sono secondarie (crema
 * con il bordo grigio). Di chi sia il turno lo dicono gia' il tabellone e le schede
 * dei giocatori, quindi qui non viene ripetuto.
 * <p>
 * <b>Racconto.</b> Le righe dell'ultimo evento - tutto cio' che ha prodotto l'ultimo
 * comando - sono in grassetto; appena ne arriva uno nuovo, quelle precedenti tornano
 * in carattere normale. Le righe lunghe vanno a capo invece di essere tagliate.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class ControlPanel extends JPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Righe visibili del racconto della partita. */
    private static final int LOG_ROWS = 6;

    /** Larghezza preferita del racconto: poca, cosi' a decidere la larghezza e' il tabellone. */
    private static final int LOG_PREFERRED_WIDTH = 400;

    /** Carattere delle righe gia' lette. */
    private static final SimpleAttributeSet OLDER_LINES = logStyle(false);

    /** Carattere delle righe dell'ultimo evento. */
    private static final SimpleAttributeSet LATEST_LINES = logStyle(true);

    private final transient GameEngine engine;
    private final JButton rollButton;
    private final JButton endTurnButton;
    private final JButton bailButton;
    private final JButton buyButton;
    private final JButton declineButton;
    private final DiceView diceView;
    private final JTextPane logPane;

    /**
     * True quando l'ultimo evento e' concluso: la prossima riga ne apre uno nuovo, e le
     * righe in grassetto tornano normali.
     */
    private boolean latestEventClosed;

    /**
     * Crea la barra dei comandi collegata al motore della partita.
     *
     * @param engine il motore a cui inoltrare i comandi dell'utente
     * @throws IllegalArgumentException se il motore e' null
     */
    public ControlPanel(final GameEngine engine) {
        super(new BorderLayout(0, Theme.GAP));
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
        this.logPane = new JTextPane();

        // Il pannello in se' e' trasparente: sul tavolo si vedono solo le sue due schede.
        this.setOpaque(false);
        this.add(this.createCommandsCard(), BorderLayout.NORTH);
        this.add(this.createLogCard(), BorderLayout.CENTER);
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
     * <p>
     * La riga fa parte dell'ultimo evento ed e' in grassetto. Se l'evento precedente era
     * gia' concluso, questa riga ne apre uno nuovo: prima tutto il testo gia' scritto
     * torna in carattere normale.
     *
     * @param line la riga da mostrare
     */
    public void appendLog(final String line) {
        final StyledDocument document = this.logPane.getStyledDocument();
        if (this.latestEventClosed) {
            document.setCharacterAttributes(0, document.getLength(), OLDER_LINES, true);
            this.latestEventClosed = false;
        }
        try {
            document.insertString(document.getLength(), line + "\n", LATEST_LINES);
        } catch (final BadLocationException e) {
            // Si scrive sempre in fondo al documento, quindi la posizione e' sempre valida.
            throw new IllegalStateException("Posizione non valida nel racconto della partita", e);
        }
        // Il cursore in fondo tiene visibile l'ultima riga scritta.
        this.logPane.setCaretPosition(document.getLength());
    }

    /**
     * Abilita i comandi consentiti in questo momento.
     * <p>
     * Non decide nulla da sola: chiede al motore cosa e' permesso. E' il motivo per
     * cui le regole restano una sola copia, nel model, anche se l'interfaccia deve
     * saperle mostrare.
     * <p>
     * Segna anche la fine dell'evento in corso nel racconto: il {@link MainWindow}
     * chiama questo metodo a ogni comando concluso, dopo che tutte le sue righe sono
     * state scritte.
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
        this.latestEventClosed = true;
    }

    /** La scheda dei comandi: pulsanti del turno, azioni contestuali e, a destra, i dadi. */
    private JPanel createCommandsCard() {
        Theme.stylePrimary(this.rollButton);
        Theme.styleSecondary(this.endTurnButton);
        Theme.styleSecondary(this.bailButton);
        Theme.stylePrimary(this.buyButton);
        Theme.styleSecondary(this.declineButton);

        final JPanel actions = transparentRow();
        actions.setBorder(Theme.sectionBorder("Azioni"));
        actions.add(this.bailButton);
        actions.add(this.buyButton);
        actions.add(this.declineButton);

        final JPanel buttons = transparentRow();
        buttons.add(this.rollButton);
        buttons.add(this.endTurnButton);
        buttons.add(actions);

        final CardPanel card = Theme.card(new BorderLayout(Theme.GAP, 0));
        card.add(buttons, BorderLayout.CENTER);
        card.add(this.diceView, BorderLayout.EAST);
        return card;
    }

    /**
     * La scheda del racconto: un titolo e, sotto, il testo in cui il {@link MainWindow}
     * riversa la cronaca della partita.
     */
    private JPanel createLogCard() {
        this.logPane.setEditable(false);
        this.logPane.setBackground(Theme.CREAM);
        this.logPane.setForeground(Theme.TEXT_DARK);
        this.logPane.setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP / 2, 0, Theme.GAP / 2));
        Theme.styleSelection(this.logPane);

        final JScrollPane scroll = new JScrollPane(this.logPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(Theme.CREAM);
        scroll.getViewport().setBackground(Theme.CREAM);
        final int lineHeight = this.logPane.getFontMetrics(Theme.LOG_FONT).getHeight();
        scroll.setPreferredSize(new Dimension(LOG_PREFERRED_WIDTH, LOG_ROWS * lineHeight));

        final CardPanel card = Theme.card(new BorderLayout(0, Theme.GAP / 2));
        card.add(Theme.label("Cosa e' successo", Theme.HEADING_FONT, Theme.TEXT_DARK), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** @return una riga trasparente di componenti affiancati, con lo spazio standard fra l'uno e l'altro */
    private static JPanel transparentRow() {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.GAP, 0));
        row.setOpaque(false);
        return row;
    }

    /**
     * @param bold true per le righe dell'ultimo evento
     * @return il carattere del racconto, normale o in grassetto
     */
    private static SimpleAttributeSet logStyle(final boolean bold) {
        final SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setFontFamily(style, Theme.LOG_FONT.getFamily());
        StyleConstants.setFontSize(style, Theme.LOG_FONT.getSize());
        StyleConstants.setBold(style, bold);
        StyleConstants.setForeground(style, Theme.TEXT_DARK);
        return style;
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
            this.setPreferredSize(new Dimension(2 * DIE_SIDE + GAP + 1, DIE_SIDE + 2));
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
                Theme.antialias(graphics);
                // I dadi stanno al centro dello spazio ricevuto, che puo' essere piu' alto di loro.
                final int y = Math.max(1, (this.getHeight() - DIE_SIDE) / 2);
                this.paintDie(graphics, 0, y, this.firstValue);
                this.paintDie(graphics, DIE_SIDE + GAP, y, this.secondValue);
            } finally {
                graphics.dispose();
            }
        }

        /** Disegna un dado crema con la faccia richiesta; con 0 il dado resta vuoto. */
        private void paintDie(final Graphics2D graphics, final int x, final int y, final int value) {
            graphics.setColor(Theme.CREAM);
            graphics.fillRoundRect(x, y, DIE_SIDE, DIE_SIDE, Theme.SMALL_RADIUS, Theme.SMALL_RADIUS);
            graphics.setColor(Theme.BORDER_STRONG);
            graphics.drawRoundRect(x, y, DIE_SIDE, DIE_SIDE, Theme.SMALL_RADIUS, Theme.SMALL_RADIUS);
            if (value < 1) {
                return;
            }
            graphics.setColor(Theme.TEXT_DARK);
            // Le facce dei dadi si compongono da tre posizioni orizzontali e tre verticali.
            final int low = x + DIE_SIDE / 4 - PIP / 2;
            final int middleX = x + DIE_SIDE / 2 - PIP / 2;
            final int high = x + 3 * DIE_SIDE / 4 - PIP / 2;
            final int top = y + DIE_SIDE / 4 - PIP / 2;
            final int middleY = y + DIE_SIDE / 2 - PIP / 2;
            final int bottom = y + 3 * DIE_SIDE / 4 - PIP / 2;

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
