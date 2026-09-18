package it.unibo.monopoly.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;

import it.unibo.monopoly.controller.PlayerSetup;
import it.unibo.monopoly.model.player.Token;

/**
 * La riga di un giocatore nella schermata di setup: il suo numero, la casella per il
 * nome, la tendina per la pedina e il pulsante per toglierlo dal tavolo.
 * <p>
 * E' un componente e basta: non decide niente e non sa nemmeno quanti giocatori ci
 * sono. Sa fare due cose, che e' quello che serve al {@link SetupWindow}:
 * <ul>
 *   <li>dire cosa ha scritto l'utente, con {@link #toPlayerSetup()};</li>
 *   <li>mostrare solo le pedine ancora libere, quando la finestra glielo chiede con
 *       {@link #showFreeTokens(List, List)}.</li>
 * </ul>
 * Quando l'utente cambia pedina o chiede di togliere la riga, la riga non fa niente
 * da sola: avvisa la finestra con le due azioni ricevute nel costruttore, e sara' la
 * finestra a decidere. E' lo stesso modo di lavorare del {@link ControlPanel} con il
 * motore: il componente chiede, qualcun altro decide.
 * <p>
 * Ha la forma di una scheda crema ({@link CardPanel}), come quelle dei giocatori nella
 * finestra di gioco: e' lo stesso giocatore, prima e durante la partita.
 * <p>
 * La classe e' {@code final} per lo stesso motivo degli altri pannelli: e' un
 * componente concreto e il costruttore puo' configurarsi senza rischiare di chiamare
 * metodi ridefiniti da una sottoclasse non ancora pronta.
 */
final class PlayerSetupRow extends CardPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Quanti caratteri larga e' la casella del nome. */
    private static final int NAME_COLUMNS = 14;

    /** Larghezza fissa della tendina, cosi' le righe restano incolonnate. */
    private static final Dimension CHOOSER_SIZE = new Dimension(140, 26);

    private final JLabel numberLabel;
    private final JTextField nameField;
    private final JComboBox<Token> tokenChooser;
    private final JButton removeButton;

    /**
     * L'ultimo nome proposto da questa riga ("Giocatore 3").
     * <p>
     * Serve a capire se il nome che si legge nella casella l'ha scritto l'utente o e'
     * ancora quello proposto: nel secondo caso si puo' aggiornare senza cancellare il
     * lavoro di nessuno.
     */
    private String proposedName;

    /**
     * Vero mentre e' la finestra a rifare l'elenco delle pedine.
     * <p>
     * Serve a non girare in tondo: rifare l'elenco fa scattare l'evento "pedina
     * cambiata", che avviserebbe la finestra, che rifarebbe l'elenco... Con questa
     * bandierina l'avviso parte solo quando a cambiare pedina e' davvero l'utente.
     */
    private boolean rebuildingChoices;

    /**
     * Crea la riga di un giocatore.
     *
     * @param initialName      il nome gia' scritto nella casella, che l'utente puo' cambiare
     * @param initialToken     la pedina proposta
     * @param onTokenChosen    cosa fare quando l'utente sceglie un'altra pedina
     * @param onRemoveRequested cosa fare quando l'utente chiede di togliere questa riga
     */
    PlayerSetupRow(final String initialName, final Token initialToken,
                   final Runnable onTokenChosen, final Consumer<PlayerSetupRow> onRemoveRequested) {
        super(new FlowLayout(FlowLayout.LEFT, Theme.GAP, Theme.GAP / 2), null, 0);
        this.setBackground(Theme.CREAM);
        this.setBorder(BorderFactory.createEmptyBorder(Theme.GAP / 2, Theme.PADDING, Theme.GAP / 2, Theme.PADDING));

        this.numberLabel = Theme.label("", Theme.NAME_FONT, Theme.TEXT_DARK);
        this.numberLabel.setPreferredSize(new Dimension(24, 20));

        this.proposedName = initialName;
        this.nameField = new JTextField(initialName, NAME_COLUMNS);
        this.nameField.setFont(Theme.BODY_FONT);
        Theme.styleSelection(this.nameField);

        this.tokenChooser = new JComboBox<>(new DefaultComboBoxModel<>(new Token[] {initialToken}));
        this.tokenChooser.setSelectedItem(initialToken);
        this.tokenChooser.setFont(Theme.BODY_FONT);
        this.tokenChooser.setPreferredSize(CHOOSER_SIZE);
        this.tokenChooser.setRenderer(new TokenRenderer());
        this.tokenChooser.addActionListener(event -> {
            if (!this.rebuildingChoices) {
                onTokenChosen.run();
            }
        });

        this.removeButton = new JButton("Togli");
        Theme.styleSecondary(this.removeButton);
        this.removeButton.setToolTipText("Toglie questo giocatore dalla partita");
        this.removeButton.addActionListener(event -> onRemoveRequested.accept(this));

        this.add(this.numberLabel);
        this.add(Theme.label("Nome:", Theme.BODY_BOLD_FONT, Theme.TEXT_DARK));
        this.add(this.nameField);
        this.add(Theme.label("Pedina:", Theme.BODY_BOLD_FONT, Theme.TEXT_DARK));
        this.add(this.tokenChooser);
        this.add(this.removeButton);
    }

    /**
     * @return nome e pedina cosi' come sono adesso nella riga. Il nome puo' essere
     *         vuoto: a dire se va bene ci pensa
     *         {@link it.unibo.monopoly.controller.GameSetup GameSetup}, non questo pannello
     */
    PlayerSetup toPlayerSetup() {
        return new PlayerSetup(this.nameField.getText(), this.getSelectedToken());
    }

    /** @return la pedina scelta in questa riga, o {@code null} se la tendina e' vuota */
    Token getSelectedToken() {
        return (Token) this.tokenChooser.getSelectedItem();
    }

    /**
     * Rifa' l'elenco delle pedine mostrate, tenendo quella gia' scelta qui e togliendo
     * quelle prese dagli altri giocatori. E' cosi' che si garantisce che ogni pedina
     * sia di uno solo: le pedine occupate non compaiono proprio.
     *
     * @param allTokens   tutte le pedine del gioco
     * @param takenTokens le pedine scelte dai giocatori, compresa quella di questa riga
     */
    void showFreeTokens(final List<Token> allTokens, final List<Token> takenTokens) {
        final Token mine = this.getSelectedToken();
        final List<Token> selectable = allTokens.stream()
                .filter(token -> token.equals(mine) || !takenTokens.contains(token))
                .toList();
        this.rebuildingChoices = true;
        try {
            this.tokenChooser.setModel(new DefaultComboBoxModel<>(selectable.toArray(new Token[0])));
            this.tokenChooser.setSelectedItem(mine);
        } finally {
            this.rebuildingChoices = false;
        }
    }

    /**
     * Aggiorna il posto occupato dalla riga: il numero mostrato davanti, il nome
     * proposto e il pulsante "Togli" (che si spegne quando i giocatori sono gia' il
     * minimo consentito).
     * <p>
     * Il nome viene riscritto <em>solo</em> se nella casella c'e' ancora quello
     * proposto: togliendo un giocatore gli altri si rinumerano, e sarebbe strano
     * vedere "3." davanti a "Giocatore 4". Se invece l'utente ha scritto il proprio
     * nome, quello non si tocca.
     *
     * @param number       il numero del giocatore, a partire da 1
     * @param defaultName  il nome da proporre a chi sta in questa posizione
     * @param removable    true se questo giocatore si puo' togliere
     */
    void updatePosition(final int number, final String defaultName, final boolean removable) {
        this.numberLabel.setText(number + ".");
        if (this.proposedName.equals(this.nameField.getText())) {
            this.nameField.setText(defaultName);
        }
        this.proposedName = defaultName;
        this.removeButton.setEnabled(removable);
    }

    /** Porta il cursore nella casella del nome, cosi' si puo' scrivere subito. */
    void focusName() {
        this.nameField.requestFocusInWindow();
    }

    /**
     * Disegna una pedina nella tendina: il pallino del suo colore e il suo nome.
     * <p>
     * Senza questo, Swing mostrerebbe il {@code toString()} di {@link Token}, che e'
     * fatto per il debug e non per l'utente.
     */
    private static final class TokenRenderer extends DefaultListCellRenderer {

        /** Vedi {@link TilePanel#serialVersionUID}. */
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                      final int index, final boolean selected,
                                                      final boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof Token token) {
                this.setText(token.getName());
                this.setIcon(ViewStyle.iconOf(token));
            }
            return this;
        }
    }
}
