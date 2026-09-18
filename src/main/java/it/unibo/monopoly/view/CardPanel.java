package it.unibo.monopoly.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * Pannello a forma di scheda: sfondo pieno ed eventuale contorno.
 * <p>
 * E' la superficie comune della GUI - le schede dei giocatori e della banca, il riquadro
 * dei comandi, le righe e il titolo della schermata di setup - cosi' tutte hanno gli
 * stessi angoli ({@link Theme#RADIUS}, oggi netti) e lo stesso modo di disegnare il
 * contorno. Il colore di riempimento e' lo sfondo del pannello
 * ({@link #setBackground(Color)}), quindi si imposta come per qualunque altro componente
 * Swing.
 * <p>
 * Una scheda puo' essere evidenziata ({@link #setHighlighted(boolean)}): al posto del
 * contorno compare l'anello giallo del turno attivo, lo stesso che il tabellone disegna
 * attorno alla casella del giocatore di turno.
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

    /** True se la scheda e' evidenziata come turno attivo: l'anello giallo sostituisce il contorno. */
    private boolean highlighted;

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
        this.highlighted = false;
    }

    /**
     * Accende o spegne l'evidenziazione del turno attivo.
     * <p>
     * L'anello e' disegnato dentro la scheda, quindi non ne cambia le misure: la colonna
     * dei giocatori non si sposta quando passa il turno. Serve pero' un margine interno
     * almeno pari all'anello con il suo alone, perche' il contenuto non ci finisca sotto.
     *
     * @param highlighted true per evidenziare la scheda
     */
    public void setHighlighted(final boolean highlighted) {
        if (this.highlighted != highlighted) {
            this.highlighted = highlighted;
            this.repaint();
        }
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
     * @return sempre false: se gli angoli sono arrotondati lasciano vedere cio' che sta
     *         sotto, e Swing deve saperlo per ridisegnare anche il contenitore
     */
    @Override
    public boolean isOpaque() {
        return false;
    }

    /**
     * Disegna lo sfondo e il contorno, oppure l'anello del turno attivo; i componenti
     * interni vengono disegnati sopra da Swing, come in qualunque pannello.
     *
     * @param g il contesto grafico fornito da Swing
     */
    @Override
    protected void paintComponent(final Graphics g) {
        final Graphics2D graphics = (Graphics2D) g.create();
        try {
            Theme.antialias(graphics);
            if (this.highlighted) {
                graphics.setColor(this.getBackground());
                graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
                Theme.paintHighlight(graphics, 0, 0, this.getWidth(), this.getHeight());
                return;
            }
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
