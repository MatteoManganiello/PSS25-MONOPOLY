package it.unibo.monopoly.controller;

/**
 * I motivi per cui una configurazione di inizio partita non va bene.
 * <p>
 * E' un enum e non un messaggio gia' scritto perche' il controller non deve decidere
 * come si parla all'utente: qui c'e' <em>qual e'</em> il problema, la frase da
 * mostrare la sceglie la view
 * ({@link it.unibo.monopoly.view.ViewStyle#describe(SetupProblem) ViewStyle.describe}).
 * Cosi' la GUI puo' scrivere una cosa e un'eventuale interfaccia testuale un'altra,
 * e i test possono controllare il problema senza dipendere dal testo esatto.
 *
 * @see GameSetup#validate()
 */
public enum SetupProblem {

    /** I giocatori sono meno del minimo previsto dal regolamento. */
    TOO_FEW_PLAYERS,

    /** I giocatori sono piu' del massimo previsto dal regolamento. */
    TOO_MANY_PLAYERS,

    /** Almeno un giocatore non ha un nome, o ha scritto solo spazi. */
    EMPTY_NAME,

    /** Almeno un giocatore non ha scelto la pedina. */
    MISSING_TOKEN,

    /** Due o piu' giocatori hanno scelto la stessa pedina. */
    DUPLICATE_TOKEN
}
