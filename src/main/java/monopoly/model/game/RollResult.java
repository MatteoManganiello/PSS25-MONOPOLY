package monopoly.model.game;

import monopoly.model.player.Player;

/**
 * Risultato immutabile di un singolo lancio: chi ha lanciato, cosa e' uscito,
 * da dove a dove si e' mosso e con quale esito.
 * <p>
 * I valori dei dadi vengono copiati qui perche' l'oggetto {@link Dice} viene
 * sovrascritto a ogni lancio: il risultato resta invece una "fotografia" valida
 * anche dopo. E' un {@code record}: immutabile, con costruttore, accessor,
 * {@code equals}, {@code hashCode} e {@code toString} generati dal compilatore.
 *
 * @param player       il giocatore che ha lanciato i dadi
 * @param firstDie     valore del primo dado
 * @param secondDie    valore del secondo dado
 * @param fromPosition posizione del giocatore prima del lancio
 * @param toPosition   casella raggiunta con il lancio: coincide con {@code fromPosition}
 *                     se il giocatore e' rimasto in prigione, ed e'
 *                     {@link monopoly.model.board.Board#JAIL_POSITION} se ci e' stato mandato
 * @param outcome      esito del lancio secondo le regole del turno
 */
public record RollResult(Player player, int firstDie, int secondDie,
                         int fromPosition, int toPosition, RollOutcome outcome) {

    /**
     * Controlla i campi obbligatori (costruttore compatto del record).
     *
     * @throws IllegalArgumentException se giocatore o esito sono null
     */
    public RollResult {
        if (player == null || outcome == null) {
            throw new IllegalArgumentException("Giocatore ed esito del lancio non possono essere null");
        }
    }

    /** @return la somma dei due dadi */
    public int total() {
        return this.firstDie + this.secondDie;
    }

    /** @return true se i due dadi hanno lo stesso valore */
    public boolean isDouble() {
        return this.firstDie == this.secondDie;
    }

    /**
     * Indica se il movimento ha completato un giro del tabellone passando dal "Via".
     * <p>
     * Un lancio vale da 2 a 12 caselle, quindi il giro c'e' esattamente quando la
     * destinazione, calcolata modulo 40, ha un indice minore della partenza.
     *
     * @return true se il giocatore e' passato dal "Via" durante il movimento
     */
    public boolean passedGo() {
        return this.outcome.movesPlayer() && this.toPosition < this.fromPosition;
    }
}
