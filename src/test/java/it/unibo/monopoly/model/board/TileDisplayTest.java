package it.unibo.monopoly.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.economy.EconomyManager;
import it.unibo.monopoly.model.game.GameContext;
import it.unibo.monopoly.model.game.JailManager;

/**
 * Test delle informazioni che ogni casella espone alla view: la categoria
 * ({@link Tile#getCategory()}) e la riga di dettaglio ({@link Tile#getDetail()}).
 * <p>
 * Sono le due chiamate polimorfiche su cui si basa il disegno del tabellone:
 * grazie a queste la GUI non ha bisogno di sapere di che tipo sia una casella. I test verificano proprio questo, cioe' che ogni sottoclasse risponda
 * per conto proprio senza che nessuno debba interrogarne il tipo.
 */
class TileDisplayTest {

    private GameContext context;
    private EconomyManager economy;
    private JailManager jail;

    @BeforeEach
    void setUp() {
        context = new GameContext();
        economy = context.getEconomy();
        jail = context.getJail();
    }

    @Test
    void eachTileTypeDeclaresItsOwnCategory() {
        assertEquals(TileCategory.START, new StartTile("Via", 0, economy).getCategory());
        assertEquals(TileCategory.PROPERTY,
                new FixedRentPropertyTile("Vicolo Corto", 1, 60, 2, context).getCategory());
        assertEquals(TileCategory.TAX, new TaxTile("Tassa", 4, 200, economy).getCategory());
        assertEquals(TileCategory.JAIL, new JailTile("Prigione", 10).getCategory());
        assertEquals(TileCategory.GO_TO_JAIL,
                new GoToJailTile("Vai in prigione", 30, jail).getCategory());
        assertEquals(TileCategory.FREE_PARKING,
                new FreeParkingTile("Posteggio", 20).getCategory());
        assertEquals(TileCategory.CARD, new PlaceholderTile("Imprevisti", 7).getCategory());
    }

    @Test
    void propertiesShowTheirPrice() {
        assertEquals("60", new FixedRentPropertyTile("Vicolo Corto", 1, 60, 2, context).getDetail());
    }

    @Test
    void taxesAndSalaryShowTheirAmount() {
        assertEquals("Paga 200", new TaxTile("Tassa patrimoniale", 4, 200, economy).getDetail());
        assertEquals("Ritira 200", new StartTile("Via", 0, 200, economy).getDetail());
    }

    @Test
    void everyTileOfTheStandardBoardIsReadyToBeDrawn() {
        final Board board = BoardFactory.createStandardBoard(new GameContext());
        for (int position = 0; position < Board.SIZE; position++) {
            final Tile tile = board.getTileAt(position);
            // Nessuna casella del tabellone standard resta "senza famiglia": la view
            // trova sempre un colore da usare.
            assertFalse(tile.getCategory() == TileCategory.OTHER,
                    "la casella " + tile.getName() + " non dichiara una categoria");
            assertTrue(tile.getDetail() != null, "il dettaglio non puo' essere null");
        }
    }
}
