package it.unibo.monopoly.model.game;

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

    /**
     * Il gioco e' fermo in attesa che il giocatore decida se comprare la proprieta'
     * libera su cui si e' fermato.
     * <p>
     * E' la fase che permette al model di non bloccarsi aspettando l'utente: invece di
     * fermare il thread finche' non arriva una risposta, la partita registra una
     * decisione in sospeso e resta in questa fase. Il giocatore risponde quando vuole,
     * passando da {@link it.unibo.monopoly.controller.GameEngine#buyOfferedProperty()
     * buyOfferedProperty()} o
     * {@link it.unibo.monopoly.controller.GameEngine#declineOfferedProperty()
     * declineOfferedProperty()}; finche' non risponde non si puo' ne' tirare i dadi ne'
     * passare il turno.
     */
    AWAITING_PURCHASE_DECISION,

    /** Chiusura del turno e passaggio al giocatore successivo. */
    END_TURN
}
