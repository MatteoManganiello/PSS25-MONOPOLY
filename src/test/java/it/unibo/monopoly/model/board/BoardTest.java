package it.unibo.monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameContext;

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
        assertEquals(22, countOf(StreetTile.class), "i terreni del tabellone standard");
        assertEquals(4, countOf(StationTile.class), "le quattro stazioni");
        assertEquals(2, countOf(UtilityTile.class), "le due societa'");
        assertEquals(2, countOf(TaxTile.class));
        // Imprevisti e Probabilita': segnaposto senza effetto.
        assertEquals(6, countOf(PlaceholderTile.class));
    }

    @Test
    void everyPropertyStartsForSaleWithAPositivePrice() {
        for (final Tile tile : board.getTiles()) {
            if (tile instanceof Property property) {
                assertTrue(property.isAvailable(), property.getName() + " dovrebbe essere in vendita");
                assertTrue(property.getPrice() > 0, property.getName() + " dovrebbe avere un prezzo");
                if (tile instanceof UtilityTile) {
                    // Le societa' sono l'eccezione: non hanno un affitto fisso, si calcola sui dadi.
                    assertEquals(0, property.getRent(), property.getName());
                } else {
                    assertTrue(property.getRent() > 0, property.getName() + " dovrebbe avere un affitto");
                }
            }
        }
    }

    @Test
    void everyStreetBelongsToAColorGroupAndEveryGroupIsComplete() {
        final Map<ColorGroup, List<StreetTile>> byGroup = board.getTiles().stream()
                .filter(StreetTile.class::isInstance)
                .map(StreetTile.class::cast)
                .collect(Collectors.groupingBy(StreetTile::getGroup));

        // Tutti e otto i colori esistono sul tabellone: senza un gruppo completo il
        // monopolio non sarebbe mai raggiungibile.
        assertEquals(EnumSet.allOf(ColorGroup.class), EnumSet.copyOf(byGroup.keySet()));
        byGroup.forEach((group, streets) ->
                assertTrue(streets.size() >= 2, group + " dovrebbe avere almeno due terreni"));
    }

    /** @return quante caselle del tabellone sono del tipo indicato */
    private long countOf(final Class<? extends Tile> type) {
        return board.getTiles().stream().filter(type::isInstance).count();
    }
}
