package monopoly.model.game;

/**
 * Fasi in cui si articola il turno di un giocatore.
 * <p>
 * Modellare le fasi come enum permette di descrivere il turno come una piccola
 * macchina a stati: il controller sapra' sempre "a che punto siamo" e potra'
 * abilitare o disabilitare i comandi della view di conseguenza (per esempio il
 * pulsante "lancia i dadi" e' attivo solo nella fase {@link #ROLL}).
 * <p>
 * La logica che fa avanzare le fasi si trova in {@link TurnManager}.
 */
public enum GamePhase {

    /** Attesa del lancio dei dadi da parte del giocatore di turno. */
    ROLL,

    /** Spostamento della pedina sul tabellone. */
    MOVE,

    /** Applicazione dell'effetto della casella su cui il giocatore si e' fermato. */
    ACTION,

    /** Chiusura del turno e passaggio al giocatore successivo. */
    END_TURN
}
