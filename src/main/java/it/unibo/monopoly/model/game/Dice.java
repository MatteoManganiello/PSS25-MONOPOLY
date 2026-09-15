package monopoly.model.game;

import java.util.Random;

/**
 * Coppia di dadi a sei facce usata per il movimento dei giocatori.
 * <p>
 * L'oggetto mantiene il risultato dell'ultimo lancio, cosi' il controller puo'
 * interrogarlo piu' volte (totale, singoli valori, doppio) senza dover rilanciare.
 * Il generatore casuale e' iniettabile tramite costruttore: nei test si passa un
 * {@link Random} con seme fisso e i risultati diventano deterministici.
 */
public class Dice {

    /** Numero di facce di ogni dado. */
    public static final int FACES = 6;

    /** Numero di dadi lanciati insieme. */
    public static final int NUMBER_OF_DICE = 2;

    /** Valore convenzionale dei dadi prima del primo lancio. */
    private static final int NOT_ROLLED = 0;

    private final Random random;
    private int firstValue;
    private int secondValue;

    /** Crea una coppia di dadi con un generatore casuale di default. */
    public Dice() {
        this(new Random());
    }

    /**
     * Crea una coppia di dadi con un generatore casuale specifico (utile nei test).
     *
     * @param random il generatore di numeri casuali da usare
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
     * Lancia entrambi i dadi e memorizza il risultato.
     *
     * @return la somma dei due dadi, cioe' il numero di caselle da percorrere
     */
    public int roll() {
        this.firstValue = this.random.nextInt(FACES) + 1;
        this.secondValue = this.random.nextInt(FACES) + 1;
        return this.getTotal();
    }

    /** @return la somma dell'ultimo lancio, oppure 0 se i dadi non sono ancora stati lanciati */
    public int getTotal() {
        return this.firstValue + this.secondValue;
    }

    /** @return il valore del primo dado nell'ultimo lancio */
    public int getFirstValue() {
        return this.firstValue;
    }

    /** @return il valore del secondo dado nell'ultimo lancio */
    public int getSecondValue() {
        return this.secondValue;
    }

    /**
     * Indica se l'ultimo lancio e' stato un "doppio" (due valori uguali).
     * Serve alle regole che daranno diritto a un turno extra o all'uscita di prigione.
     *
     * @return true se i due dadi hanno lo stesso valore e sono gia' stati lanciati
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
