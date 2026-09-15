package monopoly.model.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Test di base sui dadi. */
class DiceTest {

    private static final int ROLLS = 1000;

    @Test
    void diceAreNotRolledAtCreation() {
        final Dice dice = new Dice();
        assertFalse(dice.hasBeenRolled());
        assertFalse(dice.isDouble());
        assertEquals(0, dice.getTotal());
    }

    @Test
    void rollAlwaysReturnsValidValues() {
        final Dice dice = new Dice(new Random(42));
        for (int i = 0; i < ROLLS; i++) {
            final int total = dice.roll();
            assertTrue(dice.getFirstValue() >= 1 && dice.getFirstValue() <= Dice.FACES);
            assertTrue(dice.getSecondValue() >= 1 && dice.getSecondValue() <= Dice.FACES);
            assertEquals(dice.getFirstValue() + dice.getSecondValue(), total);
        }
    }

    @Test
    void doubleIsDetectedWhenValuesAreEqual() {
        final Dice dice = new Dice(new Random(42));
        dice.roll();
        assertEquals(dice.getFirstValue() == dice.getSecondValue(), dice.isDouble());
    }
}
