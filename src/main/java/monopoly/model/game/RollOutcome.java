package monopoly.model.game;

/**
 * Esito di un singolo lancio di dadi del giocatore di turno.
 * <p>
 * Descrive cosa e' successo secondo le regole del turno, cosi' chi riceve il
 * risultato (il controller e, tramite lui, la view) puo' reagire con uno
 * {@code switch} esaustivo invece di ricostruire la situazione guardando i dadi.
 */
public enum RollOutcome {

    /** Il giocatore si e' mosso e il suo turno e' concluso. */
    MOVED(true),

    /** Il giocatore ha fatto doppio, si e' mosso e deve lanciare di nuovo. */
    ROLL_AGAIN(true),

    /** Terzo doppio consecutivo: il giocatore va dritto in prigione senza muoversi. */
    SENT_TO_JAIL(false),

    /** Il giocatore era in prigione e ha fatto doppio: esce e si muove, ma non rilancia. */
    RELEASED_FROM_JAIL(true),

    /** Il giocatore era in prigione e non ha fatto doppio: resta dov'e'. */
    STAYED_IN_JAIL(false);

    private final boolean movesPlayer;

    RollOutcome(final boolean movesPlayer) {
        this.movesPlayer = movesPlayer;
    }

    /**
     * @return true se con questo esito la pedina percorre il tabellone del numero
     *         di caselle indicato dai dadi (l'andata in prigione e' uno spostamento
     *         diretto, non un movimento lungo il tabellone)
     */
    public boolean movesPlayer() {
        return this.movesPlayer;
    }
}
