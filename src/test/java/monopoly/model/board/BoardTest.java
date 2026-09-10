package monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Test di base sul tabellone. */
class BoardTest {

    @Test
    void boardHasFortyTiles() {
        final Board board = new Board();
        assertEquals(Board.SIZE, board.getSize());
        assertEquals(Board.SIZE, board.getTiles().size());
    }

    @Test
    void eachTileKnowsItsOwnPosition() {
        final Board board = new Board();
        for (int position = 0; position < Board.SIZE; position++) {
            assertEquals(position, board.getTileAt(position).getPosition());
        }
    }

    @Test
    void invalidPositionsAreRejected() {
        final Board board = new Board();
        assertThrows(IllegalArgumentException.class, () -> board.getTileAt(-1));
        assertThrows(IllegalArgumentException.class, () -> board.getTileAt(Board.SIZE));
    }
}
