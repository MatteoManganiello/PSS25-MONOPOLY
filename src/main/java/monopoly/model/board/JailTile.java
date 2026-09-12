package monopoly.model.board;

import monopoly.model.player.Player;

/**
 * Casella della prigione.
 * <p>
 * Ha due significati a seconda di come ci si arriva, ed e' proprio per questo che
 * {@link #onLand(Player)} non fa nulla:
 * <ul>
 *   <li>chi ci arriva muovendo i dadi e' "di passaggio" ({@code just visiting}): si
 *       ferma sulla casella come su una qualsiasi altra e al turno dopo riparte;</li>
 *   <li>chi ci viene mandato (tre doppi consecutivi o casella "Vai in prigione") e'
 *       davvero in prigione, ma in quel caso non c'e' stato nessun atterraggio: ci
 *       ha pensato il {@link monopoly.model.game.JailManager JailManager} a
 *       spostarlo e a cambiargli stato.</li>
 * </ul>
 * La distinzione non e' quindi nella casella ma nello stato del giocatore
 * ({@link Player#isInJail()}): la casella e' solo un luogo.
 */
public class JailTile extends Tile {

    /**
     * Crea la casella della prigione.
     *
     * @param name     nome della casella (es. "Prigione")
     * @param position posizione sul tabellone, normalmente {@link Board#JAIL_POSITION}
     */
    public JailTile(final String name, final int position) {
        super(name, position);
    }

    /**
     * Non produce alcun effetto: chi ci si ferma con i dadi e' solo di passaggio.
     *
     * @param player il giocatore che si e' fermato sulla casella (ignorato)
     */
    @Override
    public void onLand(final Player player) {
        // "Just visiting": nessun effetto. Chi e' davvero in prigione ci e' stato mandato.
    }
}
