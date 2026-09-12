package monopoly.model.board;

import monopoly.model.game.JailManager;
import monopoly.model.player.Player;

/**
 * Casella "Vai in prigione": chi ci si ferma viene spedito direttamente in prigione.
 * <p>
 * Non applica la regola da sola: delega al {@link JailManager}, che e' lo stesso
 * oggetto usato dal {@link monopoly.model.game.TurnManager TurnManager} per i tre
 * doppi consecutivi. Le due strade che portano in prigione producono quindi
 * esattamente lo stesso effetto (spostamento sulla casella della prigione, stato
 * {@code IN_JAIL}, tentativi di uscita azzerati, evento per la view).
 * <p>
 * Il giocatore viene spostato, non "mosso": attraversando il tabellone all'indietro
 * fino alla prigione non passa dal "Via" e quindi non prende lo stipendio.
 */
public class GoToJailTile extends Tile {

    private final JailManager jail;

    /**
     * Crea la casella "Vai in prigione".
     *
     * @param name     nome della casella (es. "Vai in prigione")
     * @param position posizione sul tabellone, normalmente {@link Board#GO_TO_JAIL_POSITION}
     * @param jail     le regole della prigione della partita
     * @throws IllegalArgumentException se le regole della prigione sono null
     */
    public GoToJailTile(final String name, final int position, final JailManager jail) {
        super(name, position);
        if (jail == null) {
            throw new IllegalArgumentException("Le regole della prigione non possono essere null");
        }
        this.jail = jail;
    }

    /**
     * Manda il giocatore in prigione.
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        this.jail.sendToJail(player);
    }
}
