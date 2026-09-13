package monopoly.model.game;

/**
 * Esito di un singolo lancio di dadi del giocatore di turno.
 * <p>
 * Descrive cosa e' successo secondo le regole del turno, cosi' chi riceve il
 * risultato (il controller e, tramite lui, la view) puo' reagire con uno
 * {@code switch} esaustivo invece di ricostruire la situazione guardando i dadi.
 * <p>
 * Attenzione a cosa <em>non</em> dice: l'esito riguarda il movimento del giocatore, non
 * le conseguenze economiche del turno. Un giocatore puo' risultare {@link #MOVED} ed
 * essere nel frattempo fallito, perche' la casella su cui si e' fermato gli ha chiesto
 * un affitto che non poteva pagare. Il fallimento si legge sempre nello stato del
 * giocatore ({@link monopoly.model.player.PlayerStatus#BANKRUPT BANKRUPT}) e
 * nell'evento {@code onPlayerBankrupt}, mai da qui.
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
    STAYED_IN_JAIL(false),

    /**
     * Il giocatore ha esaurito i tentativi di uscita: paga la cauzione ed e' libero,
     * ma per questo turno resta fermo sulla casella della prigione.
     * <p>
     * Vale anche quando e' proprio la cauzione a farlo fallire: la prigione comunque non
     * lo trattiene piu', ma e' uscito dalla partita. Come per {@link #MOVED}, e' lo stato
     * del giocatore a dire se e' ancora in gioco.
     */
    RELEASED_ON_BAIL(false);

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
