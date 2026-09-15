package monopoly.model.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import monopoly.model.player.Player;

/**
 * Casella finta per i test: ricorda chi ci si e' fermato sopra ed esegue un
 * effetto configurabile.
 * <p>
 * E' una sottoclasse di {@link Tile} come lo saranno le caselle vere: il
 * TurnManager la usa senza accorgersi della differenza, ed e' proprio questo che
 * i test vogliono verificare (polimorfismo di {@link Tile#onLand(Player)}).
 * L'effetto permette di simulare le caselle del Giorno 3 (prigione, fallimento, ...).
 */
public class RecordingTile extends Tile {

    private final Consumer<Player> effect;
    private final List<Player> visitors;

    /**
     * @param position posizione sul tabellone
     * @param effect   azione eseguita su chi si ferma sulla casella
     */
    public RecordingTile(final int position, final Consumer<Player> effect) {
        super("Test tile " + position, position);
        this.effect = effect;
        this.visitors = new ArrayList<>();
    }

    @Override
    public void onLand(final Player player) {
        this.visitors.add(player);
        this.effect.accept(player);
    }

    /** @return i giocatori che si sono fermati sulla casella, in ordine */
    public List<Player> getVisitors() {
        return Collections.unmodifiableList(this.visitors);
    }

    /**
     * @param effect azione eseguita da ogni casella su chi ci si ferma
     * @return un tabellone composto solo da RecordingTile con lo stesso effetto
     */
    public static Board createBoard(final Consumer<Player> effect) {
        final List<Tile> tiles = new ArrayList<>(Board.SIZE);
        for (int position = 0; position < Board.SIZE; position++) {
            tiles.add(new RecordingTile(position, effect));
        }
        return new Board(tiles);
    }

    /** @return un tabellone di RecordingTile senza alcun effetto */
    public static Board createBoard() {
        return createBoard(player -> { });
    }
}
