package monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import monopoly.model.economy.Property;
import monopoly.model.game.GameContext;

/** Test sul tabellone e sulla composizione del tabellone standard. */
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        // Le caselle vere hanno bisogno dei servizi della partita: li fornisce il contesto.
        board = BoardFactory.createStandardBoard(new GameContext());
    }

    @Test
    void boardHasFortyTiles() {
        assertEquals(Board.SIZE, board.getSize());
        assertEquals(Board.SIZE, board.getTiles().size());
    }

    @Test
    void eachTileKnowsItsOwnPosition() {
        for (int position = 0; position < Board.SIZE; position++) {
            assertEquals(position, board.getTileAt(position).getPosition());
        }
    }

    @Test
    void invalidPositionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> board.getTileAt(-1));
        assertThrows(IllegalArgumentException.class, () -> board.getTileAt(Board.SIZE));
    }

    @Test
    void theFourCornersHaveTheRightTileType() {
        assertInstanceOf(StartTile.class, board.getTileAt(Board.START_POSITION));
        assertInstanceOf(JailTile.class, board.getTileAt(Board.JAIL_POSITION));
        assertInstanceOf(FreeParkingTile.class, board.getTileAt(Board.FREE_PARKING_POSITION));
        assertInstanceOf(GoToJailTile.class, board.getTileAt(Board.GO_TO_JAIL_POSITION));
    }

    @Test
    void everyTileTypeIsRepresented() {
        assertTrue(countOf(PropertyTile.class) >= 20, "servono abbastanza proprieta' acquistabili");
        assertEquals(2, countOf(TaxTile.class));
        // Imprevisti e Probabilita': ancora segnaposto, in attesa del mazzo di carte.
        assertEquals(6, countOf(PlaceholderTile.class));
    }

    @Test
    void everyPropertyStartsForSaleWithAPositivePrice() {
        for (final Tile tile : board.getTiles()) {
            if (tile instanceof Property property) {
                assertTrue(property.isAvailable(), property.getName() + " dovrebbe essere in vendita");
                assertTrue(property.getPrice() > 0, property.getName() + " dovrebbe avere un prezzo");
                assertTrue(property.getRent() > 0, property.getName() + " dovrebbe avere un affitto");
            }
        }
    }

    @Test
    void freeParkingJackpotIsDisabledByDefault() {
        final FreeParkingTile parking = (FreeParkingTile) board.getTileAt(Board.FREE_PARKING_POSITION);
        assertFalse(parking.isJackpotEnabled());
    }

    /** @return quante caselle del tabellone sono del tipo indicato */
    private long countOf(final Class<? extends Tile> type) {
        return board.getTiles().stream().filter(type::isInstance).count();
    }
}
