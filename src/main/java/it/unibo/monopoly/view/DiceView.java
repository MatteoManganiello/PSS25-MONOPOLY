package it.unibo.monopoly.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

/**
 * Disegno dei due dadi dell'ultimo lancio, al centro del tabellone sotto la scritta
 * "MONOPOLY", come dadi appena tirati sul tavolo.
 * <p>
 * Un componente minuscolo, con l'unico scopo di rendere il risultato leggibile a colpo
 * d'occhio invece che come numero in mezzo al testo. Non conosce il model: riceve due
 * valori e li disegna. Prima del primo lancio i dadi restano bianchi e vuoti.
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi senza
 * il rischio di chiamare metodi ridefiniti da una sottoclasse non ancora inizializzata.
 */
final class DiceView extends JComponent {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Lato di un dado, in pixel: grande, perche' il lancio si legga da lontano. */
    private static final int DIE_SIDE = 56;

    /** Spazio fra i due dadi. */
    private static final int GAP = 14;

    /** Diametro di un pallino. */
    private static final int PIP = 11;

    /** Valore convenzionale prima del primo lancio: il dado resta vuoto. */
    private static final int NOT_ROLLED = 0;

    private int firstValue;
    private int secondValue;

    /** Crea i dadi, vuoti fino al primo lancio. */
    DiceView() {
        this.firstValue = NOT_ROLLED;
        this.secondValue = NOT_ROLLED;
        final Dimension size = new Dimension(2 * DIE_SIDE + GAP + 1, DIE_SIDE + 2);
        this.setPreferredSize(size);
        // In un BoxLayout un componente senza misura massima verrebbe allargato: i dadi
        // restano della loro misura, centrati.
        this.setMaximumSize(size);
        this.setAlignmentX(CENTER_ALIGNMENT);
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
            // I dadi stanno al centro dello spazio ricevuto, che puo' essere piu' grande di loro.
            final int x = Math.max(0, (this.getWidth() - (2 * DIE_SIDE + GAP)) / 2);
            final int y = Math.max(1, (this.getHeight() - DIE_SIDE) / 2);
            paintDie(graphics, x, y, this.firstValue);
            paintDie(graphics, x + DIE_SIDE + GAP, y, this.secondValue);
        } finally {
            graphics.dispose();
        }
    }

    /** Disegna un dado bianco con la faccia richiesta; con 0 il dado resta vuoto. */
    private static void paintDie(final Graphics2D graphics, final int x, final int y, final int value) {
        graphics.setColor(Theme.DIE_FACE);
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
