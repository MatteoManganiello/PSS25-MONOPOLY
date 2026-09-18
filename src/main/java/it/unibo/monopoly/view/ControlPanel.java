package it.unibo.monopoly.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.model.game.JailManager;

/**
 * I comandi con cui si gioca: il pulsante dei dadi, il passaggio del turno e le
 * azioni contestuali. I dadi dell'ultimo lancio sono al centro del tabellone
 * ({@link BoardPanel}).
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
 * <b>Aspetto.</b> Una scheda crema appoggiata sul tavolo, con i pulsanti in fila. Le due
 * azioni che fanno avanzare la partita, "Tira i dadi" e "Compra", sono pulsanti
 * principali (rossi); le altre sono secondarie (crema con il bordo grigio). Di chi sia il
 * turno lo dicono gia' il tabellone e le schede dei giocatori, e cosa e' successo lo
 * mostrano pedine, saldi e dadi: qui non viene ripetuto.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi
 * (layout, dimensioni, bordi) senza il rischio di chiamare metodi ridefiniti da una
 * sottoclasse non ancora inizializzata.
 */
public final class ControlPanel extends JPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    private final transient GameEngine engine;
    private final JButton rollButton;
    private final JButton endTurnButton;
    private final JButton bailButton;
    private final JButton buyButton;
    private final JButton declineButton;

    /**
     * Crea la barra dei comandi collegata al motore della partita.
     *
     * @param engine il motore a cui inoltrare i comandi dell'utente
     * @throws IllegalArgumentException se il motore e' null
     */
    public ControlPanel(final GameEngine engine) {
        super(new BorderLayout());
        if (engine == null) {
            throw new IllegalArgumentException("Il motore della partita non puo' essere null");
        }
        this.engine = engine;
        this.rollButton = new JButton("Tira i dadi");
        this.endTurnButton = new JButton("Passa il turno");
        this.bailButton = new JButton("Paga la cauzione (" + ViewStyle.formatMoney(JailManager.BAIL_AMOUNT) + ")");
        this.buyButton = new JButton("Compra");
        this.declineButton = new JButton("Non comprare");

        // Il pannello in se' e' trasparente: sul tavolo si vede solo la sua scheda.
        this.setOpaque(false);
        this.add(this.createCommandsCard(), BorderLayout.CENTER);
        this.connectButtons();
        this.refresh();
    }

    /**
     * Abilita i comandi consentiti in questo momento.
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
    }

    /** La scheda dei comandi: pulsanti del turno e azioni contestuali. */
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

        final CardPanel card = Theme.card(new BorderLayout());
        card.add(buttons, BorderLayout.CENTER);
        return card;
    }

    /** @return una riga trasparente di componenti affiancati, con lo spazio standard fra l'uno e l'altro */
    private static JPanel transparentRow() {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.GAP, 0));
        row.setOpaque(false);
        return row;
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
}
