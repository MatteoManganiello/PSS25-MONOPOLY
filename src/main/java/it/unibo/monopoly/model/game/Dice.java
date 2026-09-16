package it.unibo.monopoly.model.game;

import java.util.Random;

/**
 * I due dadi a sei facce con cui si muovono i giocatori.
 * <p>
 * Si ricorda l'ultimo lancio, cosi' lo si puo' interrogare piu' volte (totale, valori
 * singoli, doppio) senza rilanciare. Il generatore casuale si passa dal costruttore:
 * nei test gli diamo un {@link Random} con un seme fisso, cosi' esce sempre lo stesso
 * risultato e i test sono ripetibili.
 */
public class Dice {

    /** Quante facce ha ogni dado. */
    public static final int FACES = 6;

    /** Quanti dadi si lanciano insieme. */
    public static final int NUMBER_OF_DICE = 2;

    /** Il valore che hanno i dadi prima di essere lanciati la prima volta. */
    private static final int NOT_ROLLED = 0;

    private final Random random;
    private int firstValue;
    private int secondValue;

    /** Crea i dadi con un generatore casuale normale. */
    public Dice() {
        this(new Random());
    }

    /**
     * Crea i dadi scegliendo il generatore casuale: serve soprattutto nei test.
     *
     * @param random il generatore di numeri casuali
     */
    public Dice(final Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Il generatore casuale non puo' essere null");
        }
        this.random = random;
        this.firstValue = NOT_ROLLED;
        this.secondValue = NOT_ROLLED;
    }

    /**
     * Lancia tutti e due i dadi e si segna il risultato.
     *
     * @return la somma dei due dadi, cioe' di quante caselle ci si muove
     */
    public int roll() {
        this.firstValue = this.random.nextInt(FACES) + 1;
        this.secondValue = this.random.nextInt(FACES) + 1;
        return this.getTotal();
    }

    /** @return la somma dell'ultimo lancio, o 0 se non sono ancora stati lanciati */
    public int getTotal() {
        return this.firstValue + this.secondValue;
    }

    /** @return quanto ha fatto il primo dado nell'ultimo lancio */
    public int getFirstValue() {
        return this.firstValue;
    }

    /** @return quanto ha fatto il secondo dado nell'ultimo lancio */
    public int getSecondValue() {
        return this.secondValue;
    }

    /**
     * Dice se l'ultimo lancio era un doppio, cioe' due valori uguali.
     * Serve per il tiro extra e per uscire di prigione.
     *
     * @return true se i due dadi sono uguali e sono gia' stati lanciati
     */
    public boolean isDouble() {
        return this.hasBeenRolled() && this.firstValue == this.secondValue;
    }

    /** @return true se i dadi sono gia' stati lanciati almeno una volta */
    public boolean hasBeenRolled() {
        return this.firstValue != NOT_ROLLED && this.secondValue != NOT_ROLLED;
    }

    @Override
    public String toString() {
        return "Dice[" + this.firstValue + " + " + this.secondValue
                + " = " + this.getTotal() + (this.isDouble() ? ", double" : "") + "]";
    }
}
