package it.unibo.monopoly.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * Pannello a forma di scheda: sfondo pieno con gli angoli arrotondati ed eventuale
 * contorno.
 * <p>
 * E' la superficie comune della GUI - i pannelli laterali, le schede dei giocatori, le
 * righe della schermata di setup, la scritta al centro del tabellone - cosi' tutte
 * hanno lo stesso arrotondamento ({@link Theme#RADIUS}) e lo stesso modo di disegnare
 * il contorno. Il colore di riempimento e' lo sfondo del pannello
 * ({@link #setBackground(Color)}), quindi si imposta come per qualunque altro
 * componente Swing.
 * <p>
 * Non e' {@code final} perche' e' pensata come classe base dei pannelli che hanno
 * questa forma. Proprio per questo il costruttore si limita a memorizzare i valori,
 * senza chiamare metodi che una sottoclasse potrebbe ridefinire: e' la ragione per cui
 * la trasparenza degli angoli si ottiene ridefinendo {@link #isOpaque()} e non con
 * {@code setOpaque(false)}.
 */
public class CardPanel extends JPanel {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Contorno della scheda; {@code null} significa "nessun contorno". */
    private Color outline;

    /** Spessore del contorno, in pixel. */
    private int outlineWidth;

    /**
     * Crea una scheda.
     *
     * @param layout       la disposizione dei componenti interni
     * @param outline      il colore del contorno, oppure {@code null} per non disegnarlo
     * @param outlineWidth lo spessore del contorno, in pixel
     */
    public CardPanel(final LayoutManager layout, final Color outline, final int outlineWidth) {
        super(layout);
        this.outline = outline;
        this.outlineWidth = outlineWidth;
    }

    /**
     * Cambia il contorno, per esempio per evidenziare la scheda del giocatore di turno.
     *
     * @param color il nuovo colore, oppure {@code null} per toglierlo
     * @param width il nuovo spessore, in pixel
     */
    public void setOutline(final Color color, final int width) {
        this.outline = color;
        this.outlineWidth = width;
        this.repaint();
    }

    /**
     * @return sempre false: gli angoli arrotondati lasciano vedere cio' che sta sotto,
     *         e Swing deve saperlo per ridisegnare anche il contenitore
     */
    @Override
    public boolean isOpaque() {
        return false;
    }

    /**
     * Disegna lo sfondo arrotondato e il contorno; i componenti interni vengono
     * disegnati sopra da Swing, come in qualunque pannello.
     *
     * @param g il contesto grafico fornito da Swing
     */
    @Override
    protected void paintComponent(final Graphics g) {
        final Graphics2D graphics = (Graphics2D) g.create();
        try {
            Theme.antialias(graphics);
            final boolean outlined = this.outline != null && this.outlineWidth > 0;
            final float inset = outlined ? this.outlineWidth / 2f : 0f;
            final RoundRectangle2D shape = new RoundRectangle2D.Float(inset, inset,
                    this.getWidth() - 2 * inset, this.getHeight() - 2 * inset, Theme.RADIUS, Theme.RADIUS);
            graphics.setColor(this.getBackground());
            graphics.fill(shape);
            if (outlined) {
                graphics.setColor(this.outline);
                graphics.setStroke(new BasicStroke(this.outlineWidth));
                graphics.draw(shape);
            }
        } finally {
            graphics.dispose();
        }
    }
}
