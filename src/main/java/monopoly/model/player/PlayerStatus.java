package monopoly.model.player;

/**
 * Stato in cui si puo' trovare un giocatore durante la partita.
 * <p>
 * E' modellato come enum (e non come stringa o flag booleani) perche' gli stati
 * sono un insieme chiuso e mutuamente esclusivo: cosi' il compilatore stesso
 * impedisce valori non validi e si potra' usare uno {@code switch} esaustivo
 * nella logica dei turni.
 */
public enum PlayerStatus {

    /** Il giocatore e' regolarmente in partita e puo' muoversi. */
    PLAYING,

    /** Il giocatore e' in prigione: resta sul tabellone ma non si muove liberamente. */
    IN_JAIL,

    /** Il giocatore e' fallito ed e' fuori dalla partita. */
    BANKRUPT
}
